package kiosky;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * A small MQTT 3.1.1 client, so a home server (Home Assistant, Node-RED) can watch
 * and control the tablet without Shepherd.
 *
 * Kiosky publishes, all retained, under its base topic ({@code kiosky/<device>}):
 * {@code availability} (online/offline, offline also as the last will), {@code state}
 * (JSON, when something changes) and {@code motion} (ON/OFF). It listens on
 * {@code <base>/cmd/<name>}; the activity decides what each command does. With
 * discovery on, it also describes its entities to Home Assistant and repeats that
 * whenever Home Assistant announces it has restarted.
 *
 * Everything is QoS 0. One thread connects and reads; writes go through a second,
 * so the main thread never touches the network.
 */
final class Mqtt {

    /** What the activity provides. Called on the main thread. */
    interface Listener {
        /** Connected (or Home Assistant restarted): publish the state now. */
        void onConnected();
        void onCommand(String name, String payload);
    }

    private static final int KEEPALIVE_SECONDS = 60;
    private static final int READ_TIMEOUT_MS = 30000;   // a ping after this long without traffic
    private static final int MAX_PACKET = 256 * 1024;
    private static final long FIRST_BACKOFF_MS = 5000L, MAX_BACKOFF_MS = 5 * 60 * 1000L;

    private final Context context;
    private final Listener listener;
    private final Handler main = new Handler();
    private final ExecutorService writer = Executors.newSingleThreadExecutor();
    private final Random random = new Random();
    private final Options o;

    final String base;          // kiosky/<device>
    private final String nodeId; // kiosky_<android id>, for Home Assistant unique ids
    private final String clientId;

    private volatile boolean stopped;
    private Thread thread;
    private volatile Socket socket;
    private OutputStream out;    // guarded by writeLock
    private final Object writeLock = new Object();
    private volatile long lastSent;

    /** "connecting", "connected" or "error: …", for status reports and Device info. */
    volatile String status = "connecting";

    Mqtt(Context context, Options o, Listener listener) {
        this.context = context.getApplicationContext();
        this.o = o;
        this.listener = listener;
        String id = deviceId(context);
        nodeId = "kiosky_" + id;
        clientId = ("kiosky-" + id).length() > 23 ? ("kiosky-" + id).substring(0, 23) : "kiosky-" + id;
        base = o.mqttTopic.length() > 0 ? o.mqttTopic : "kiosky/" + (id.length() > 8 ? id.substring(0, 8) : id);
    }

    /** android_id in lower-case hex, or a random id kept for next time. */
    static String deviceId(Context c) {
        String id = null;
        try {
            id = Settings.Secure.getString(c.getContentResolver(), "android_id");
        } catch (Exception ignored) {
            // fall through
        }
        if (id != null) id = id.toLowerCase(Locale.ROOT).replaceAll("[^0-9a-z]", "");
        if (id == null || id.length() == 0) {
            android.content.SharedPreferences p = c.getSharedPreferences(MainActivity.PREFS, 0);
            id = p.getString("mqtt_id", null);
            if (id == null) {
                id = Long.toHexString(new Random().nextLong());
                p.edit().putString("mqtt_id", id).apply();
            }
        }
        return id;
    }

    /** A topic without wildcards, empty levels at the ends or stray spaces. */
    static String cleanTopic(String t) {
        if (t == null) return "";
        t = t.trim().replace("+", "").replace("#", "");
        while (t.startsWith("/")) t = t.substring(1);
        while (t.endsWith("/")) t = t.substring(0, t.length() - 1);
        return t;
    }

    void start() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                loop();
            }
        }, "kiosky-mqtt");
        t.setDaemon(true);
        thread = t;
        t.start();
    }

    /** Says goodbye (offline, then DISCONNECT) and closes the connection. */
    void stop() {
        if (stopped) return;
        stopped = true;
        writer.execute(new Runnable() {
            public void run() {
                try {
                    if (socket != null) {
                        sendPublish(base + "/availability", "offline", true);
                        send(0xE0, new byte[0]); // DISCONNECT: no last will
                    }
                } catch (Exception ignored) {
                    // going away anyway
                }
                closeSocket();
            }
        });
        writer.shutdown();
        if (thread != null) thread.interrupt(); // in case it's waiting to reconnect
    }

    boolean connected() {
        return "connected".equals(status);
    }

    // ---- Publishing (any thread) -------------------------------------------------

    void publish(final String subtopic, final String payload, final boolean retain) {
        if (stopped || !connected()) return;
        try {
            writer.execute(new Runnable() {
                public void run() {
                    try {
                        sendPublish(base + "/" + subtopic, payload, retain);
                    } catch (IOException e) {
                        closeSocket(); // the reader notices and reconnects
                    }
                }
            });
        } catch (Exception ignored) {
            // shut down
        }
    }

    /**
     * Home Assistant MQTT discovery: one retained config message per entity, all on
     * one device. {@code name}, {@code model} and {@code version} describe the device.
     */
    void publishDiscovery(String name, String model, String version) {
        if (!o.mqttDiscovery || stopped || !connected()) return;
        JSONObject device = Json.obj("identifiers", new JSONArray().put(nodeId), "name", name,
                "manufacturer", "Kiosky", "model", model, "sw_version", version);
        final String p = o.mqttDiscoveryPrefix + "/";
        final java.util.List<String[]> configs = new java.util.ArrayList<String[]>();

        configs.add(entity(p, "switch", "screen", "Screen", device,
                "command_topic", base + "/cmd/screen", "state_topic", base + "/state",
                "value_template", "{{ 'OFF' if value_json.screen == 'off' else 'ON' }}",
                "icon", "mdi:tablet"));
        configs.add(entity(p, "number", "brightness", "Brightness", device,
                "command_topic", base + "/cmd/brightness", "state_topic", base + "/state",
                "value_template", "{{ value_json.brightness }}", "min", 1, "max", 100, "step", 1,
                "unit_of_measurement", "%", "mode", "slider", "icon", "mdi:brightness-6"));
        configs.add(entity(p, "number", "volume", "Volume", device,
                "command_topic", base + "/cmd/volume", "state_topic", base + "/state",
                "value_template", "{{ value_json.volume }}", "min", 0, "max", 100, "step", 1,
                "unit_of_measurement", "%", "mode", "slider", "icon", "mdi:volume-high"));
        configs.add(entity(p, "text", "page", "Page", device,
                "command_topic", base + "/cmd/set_url", "state_topic", base + "/state",
                "value_template", "{{ (value_json.url or '')[:255] }}", "max", 255, "icon", "mdi:web"));
        configs.add(entity(p, "button", "reload", "Reload", device,
                "command_topic", base + "/cmd/reload", "icon", "mdi:refresh"));
        configs.add(entity(p, "button", "go_home", "Start page", device,
                "command_topic", base + "/cmd/go_home", "icon", "mdi:home"));
        configs.add(entity(p, "notify", "announce", "Announce", device,
                "command_topic", base + "/cmd/announce", "icon", "mdi:bullhorn"));
        configs.add(entity(p, "binary_sensor", "motion", "Motion", device,
                "state_topic", base + "/motion", "device_class", "motion"));
        configs.add(entity(p, "sensor", "battery", "Battery", device,
                "state_topic", base + "/state", "value_template", "{{ value_json.battery_level }}",
                "device_class", "battery", "unit_of_measurement", "%", "state_class", "measurement"));
        configs.add(entity(p, "binary_sensor", "charging", "Charging", device,
                "state_topic", base + "/state",
                "value_template", "{{ 'ON' if value_json.charging else 'OFF' }}",
                "device_class", "battery_charging"));
        configs.add(entity(p, "sensor", "battery_temperature", "Battery temperature", device,
                "state_topic", base + "/state", "value_template", "{{ value_json.battery_temperature }}",
                "device_class", "temperature", "unit_of_measurement", "°C", "state_class", "measurement",
                "entity_category", "diagnostic"));
        configs.add(entity(p, "sensor", "wifi_signal", "Wi-Fi signal", device,
                "state_topic", base + "/state", "value_template", "{{ value_json.wifi_rssi }}",
                "device_class", "signal_strength", "unit_of_measurement", "dBm", "state_class", "measurement",
                "entity_category", "diagnostic"));
        configs.add(entity(p, "sensor", "page_title", "Page title", device,
                "state_topic", base + "/state", "value_template", "{{ (value_json.title or '')[:255] }}",
                "icon", "mdi:card-text-outline", "entity_category", "diagnostic"));

        // Scans arrive as Home Assistant events; an empty config removes a scanner that's off
        String[] nfc = entity(p, "event", "nfc", "NFC tag", device, "state_topic", base + "/nfc",
                "event_types", new JSONArray().put("scanned"), "icon", "mdi:nfc-variant");
        String[] barcode = entity(p, "event", "barcode", "Barcode", device, "state_topic", base + "/barcode",
                "event_types", new JSONArray().put("scanned"), "icon", "mdi:barcode-scan");
        configs.add(o.nfc ? nfc : new String[] {nfc[0], ""});
        configs.add(o.barcode ? barcode : new String[] {barcode[0], ""});

        try {
            writer.execute(new Runnable() {
                public void run() {
                    try {
                        for (String[] c : configs) sendPublish(c[0], c[1], true);
                    } catch (IOException e) {
                        closeSocket();
                    }
                }
            });
        } catch (Exception ignored) {
            // shut down
        }
    }

    /** {topic, payload} for one entity's discovery message. */
    private String[] entity(String prefix, String component, String object, String name, JSONObject device,
                            Object... kv) {
        JSONObject c = Json.obj(kv);
        Json.put(c, "name", name);
        Json.put(c, "unique_id", nodeId + "_" + object);
        Json.put(c, "availability_topic", base + "/availability");
        Json.put(c, "device", device);
        return new String[] {prefix + component + "/" + nodeId + "/" + object + "/config", c.toString()};
    }

    // ---- Connection --------------------------------------------------------------

    private void loop() {
        long backoff = FIRST_BACKOFF_MS;
        while (!stopped) {
            boolean refused = false;
            try {
                DataInputStream in = connect();
                backoff = FIRST_BACKOFF_MS;
                read(in);
            } catch (Refused e) {
                status = "error: " + e.getMessage();
                refused = true;
            } catch (Exception e) {
                if (!stopped) status = "error: " + (e.getMessage() != null ? e.getMessage() : e.toString());
            } finally {
                closeSocket();
            }
            if (stopped) break;
            long wait = refused ? MAX_BACKOFF_MS : backoff;
            backoff = Math.min(backoff * 2, MAX_BACKOFF_MS);
            try {
                Thread.sleep((long) (wait * (0.8 + 0.4 * random.nextDouble())));
            } catch (InterruptedException e) {
                return;
            }
            if (!stopped) status = "connecting";
        }
    }

    /** The broker answered CONNECT with a refusal: wrong details, so don't hammer it. */
    private static final class Refused extends IOException {
        Refused(String m) {
            super(m);
        }
    }

    private DataInputStream connect() throws IOException {
        Socket s;
        if (o.mqttTls) {
            SSLSocket ss = (SSLSocket) sslContext().getSocketFactory().createSocket();
            socket = ss;
            ss.connect(new InetSocketAddress(o.mqttHost, o.mqttPort), 15000);
            ss.setSoTimeout(15000);
            ss.startHandshake();
            // A pinned certificate is checked by its fingerprint; otherwise the name must match
            if (o.mqttFingerprint.length() == 0
                    && !HttpsURLConnection.getDefaultHostnameVerifier().verify(o.mqttHost, ss.getSession())) {
                throw new IOException("the broker's certificate isn't for " + o.mqttHost);
            }
            s = ss;
        } else {
            s = new Socket();
            socket = s;
            s.connect(new InetSocketAddress(o.mqttHost, o.mqttPort), 15000);
        }
        s.setTcpNoDelay(true);
        s.setSoTimeout(15000);
        DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
        synchronized (writeLock) {
            out = new BufferedOutputStream(s.getOutputStream());
        }

        // CONNECT: clean session, last will "offline" (retained) on the availability topic
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        string(b, "MQTT");
        b.write(4); // protocol level 3.1.1
        boolean user = o.mqttUsername.length() > 0;
        boolean pass = user && o.mqttPassword.length() > 0;
        int flags = 0x02 | 0x04 | 0x20; // clean session, will, will retain (will QoS 0)
        if (user) flags |= 0x80;
        if (pass) flags |= 0x40;
        b.write(flags);
        b.write(KEEPALIVE_SECONDS >> 8);
        b.write(KEEPALIVE_SECONDS & 0xFF);
        string(b, clientId);
        string(b, base + "/availability");
        string(b, "offline");
        if (user) string(b, o.mqttUsername);
        if (pass) string(b, o.mqttPassword);
        send(0x10, b.toByteArray());

        int h = in.read();
        if (h < 0) throw new IOException("the broker closed the connection");
        byte[] ack = body(in);
        if ((h >> 4) != 2 || ack.length < 2) throw new IOException("the broker didn't answer like an MQTT server");
        int rc = ack[1] & 0xFF;
        if (rc != 0) {
            String[] why = {"", "unsupported protocol version", "client id refused", "broker unavailable",
                    "wrong username or password", "not authorised"};
            throw new Refused(rc < why.length ? why[rc] : "connection refused (" + rc + ")");
        }
        s.setSoTimeout(READ_TIMEOUT_MS);

        // SUBSCRIBE (QoS 0) to our commands, and to Home Assistant's restart announcements
        ByteArrayOutputStream sub = new ByteArrayOutputStream();
        sub.write(0);
        sub.write(1); // packet id 1
        string(sub, base + "/cmd/+");
        sub.write(0);
        if (o.mqttDiscovery) {
            string(sub, o.mqttDiscoveryPrefix + "/status");
            sub.write(0);
        }
        send(0x82, sub.toByteArray());
        sendPublish(base + "/availability", "online", true);

        status = "connected";
        main.post(new Runnable() {
            public void run() {
                if (!stopped) listener.onConnected();
            }
        });
        return in;
    }

    private void read(DataInputStream in) throws IOException {
        boolean pinged = false;
        while (!stopped) {
            int h;
            try {
                h = in.read();
            } catch (SocketTimeoutException e) {
                if (pinged) throw new IOException("the broker stopped answering");
                ping();
                pinged = true;
                continue;
            }
            if (h < 0) throw new IOException("the broker closed the connection");
            byte[] body = body(in);
            pinged = false;
            int type = h >> 4;
            if (type == 3) received(h, body);
            // SUBACK (9) and PINGRESP (13) need nothing
            if (System.currentTimeMillis() - lastSent > KEEPALIVE_SECONDS * 500L) ping(); // busy inbox: still ping
        }
    }

    private void received(int header, byte[] b) throws IOException {
        if (b.length < 2) return;
        int len = ((b[0] & 0xFF) << 8) | (b[1] & 0xFF);
        if (2 + len > b.length) return;
        String topic = new String(b, 2, len, "UTF-8");
        int pos = 2 + len;
        int qos = (header >> 1) & 3;
        if (qos > 0) {
            if (pos + 2 > b.length) return;
            if (qos == 1) send(0x40, new byte[] {b[pos], b[pos + 1]}); // PUBACK
            pos += 2;
        }
        final String payload = new String(b, pos, b.length - pos, "UTF-8");
        if (topic.equals(o.mqttDiscoveryPrefix + "/status")) {
            if ("online".equalsIgnoreCase(payload.trim())) {
                main.post(new Runnable() {
                    public void run() {
                        if (!stopped) listener.onConnected(); // Home Assistant restarted: describe ourselves again
                    }
                });
            }
            return;
        }
        String prefix = base + "/cmd/";
        if (!topic.startsWith(prefix)) return;
        final String name = topic.substring(prefix.length());
        main.post(new Runnable() {
            public void run() {
                if (!stopped) listener.onCommand(name, payload);
            }
        });
    }

    private void ping() throws IOException {
        send(0xC0, new byte[0]);
    }

    private void sendPublish(String topic, String payload, boolean retain) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        string(b, topic);
        byte[] p = payload.getBytes("UTF-8");
        b.write(p, 0, p.length);
        send(retain ? 0x31 : 0x30, b.toByteArray());
    }

    private void send(int header, byte[] body) throws IOException {
        synchronized (writeLock) {
            if (out == null) throw new IOException("not connected");
            out.write(header);
            int n = body.length;
            do { // remaining length: 7 bits per byte, high bit for "more"
                int digit = n % 128;
                n /= 128;
                out.write(n > 0 ? digit | 0x80 : digit);
            } while (n > 0);
            out.write(body);
            out.flush();
            lastSent = System.currentTimeMillis();
        }
    }

    private static byte[] body(DataInputStream in) throws IOException {
        int len = 0, mult = 1, digit;
        int bytes = 0;
        do {
            digit = in.read();
            if (digit < 0) throw new IOException("the broker closed the connection");
            len += (digit & 0x7F) * mult;
            mult *= 128;
            if (++bytes > 4) throw new IOException("malformed packet");
        } while ((digit & 0x80) != 0);
        if (len > MAX_PACKET) throw new IOException("a message from the broker was too big");
        byte[] b = new byte[len];
        in.readFully(b);
        return b;
    }

    private static void string(ByteArrayOutputStream b, String s) throws IOException {
        byte[] u = s.getBytes("UTF-8");
        b.write(u.length >> 8);
        b.write(u.length & 0xFF);
        b.write(u, 0, u.length);
    }

    private void closeSocket() {
        Socket s = socket;
        socket = null;
        synchronized (writeLock) {
            out = null;
        }
        if (s != null) {
            try {
                s.close();
            } catch (IOException ignored) {
                // already closed
            }
        }
    }

    /** The system's trusted certificates, or exactly the pinned one when a fingerprint is set. */
    private SSLContext sslContext() throws IOException {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            if (o.mqttFingerprint.length() == 0) {
                ctx.init(null, null, null);
            } else {
                final String want = o.mqttFingerprint;
                ctx.init(null, new TrustManager[] {new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String auth) throws CertificateException {
                        throw new CertificateException("not a server");
                    }

                    public void checkServerTrusted(X509Certificate[] chain, String auth) throws CertificateException {
                        if (chain == null || chain.length == 0) throw new CertificateException("no certificate");
                        try {
                            byte[] h = MessageDigest.getInstance("SHA-256").digest(chain[0].getEncoded());
                            StringBuilder sb = new StringBuilder();
                            for (byte x : h) sb.append(String.format(Locale.ROOT, "%02x", x & 0xff));
                            if (!want.equals(sb.toString())) {
                                throw new CertificateException("the broker's certificate doesn't match the fingerprint");
                            }
                        } catch (java.security.NoSuchAlgorithmException e) {
                            throw new CertificateException(e.toString());
                        }
                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }}, null);
            }
            return ctx;
        } catch (Exception e) {
            throw new IOException("TLS isn't available: " + e);
        }
    }

    /** The device name shown in Home Assistant. */
    static String deviceName(Options o, String managedName) {
        if (o.mqttName.length() > 0) return o.mqttName;
        if (managedName != null && managedName.length() > 0) return managedName;
        return "Kiosky " + (Build.MODEL != null ? Build.MODEL : "tablet");
    }
}
