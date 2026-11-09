package kiosky;

import android.app.Activity;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Handler;
import android.util.Base64;
import android.view.KeyEvent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.Locale;

/**
 * Scans for check-in, attendance and stock-taking pages:
 *
 * - NFC tags, read with Android's reader mode while Kiosky is in front (which also
 *   stops a tag opening another app): the tag's id and any NDEF text, URI or data.
 * - Barcode scanners that act as a keyboard (USB or Bluetooth): a burst of keys much
 *   faster than anyone types, ending in Enter or Tab. The keys still reach the page
 *   as usual, so a focused text box fills in just as before.
 *
 * Each scan goes to the {@link Listener} on the main thread, as JSON.
 */
final class ScanInput {

    interface Listener {
        /** {@code type}: "nfc" or "barcode". */
        void onScan(String type, JSONObject scan);
    }

    private static final long MAX_GAP_MS = 60;    // between a scanner's keys; people take 100 ms or more
    private static final int MIN_LENGTH = 3;
    private static final int READER_FLAGS = 0x1 | 0x2 | 0x4 | 0x8; // NFC A, B, F and V

    private final Activity activity;
    private final Listener listener;
    private final Handler main = new Handler();
    private NfcAdapter nfc;
    private boolean reading;

    private final StringBuilder keys = new StringBuilder();
    private long firstKeyAt, lastKeyAt;

    ScanInput(Activity activity, Listener listener) {
        this.activity = activity;
        this.listener = listener;
        try {
            nfc = NfcAdapter.getDefaultAdapter(activity);
        } catch (Exception e) {
            nfc = null;
        }
    }

    /** The tablet has NFC (it may be switched off in Android's settings). */
    boolean nfcPresent() {
        return nfc != null;
    }

    boolean nfcOn() {
        try {
            return nfc != null && nfc.isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    /** Starts or stops NFC reader mode: on while {@code wanted} and Kiosky is in front. */
    void setNfc(boolean wanted) {
        if (nfc == null || wanted == reading) return;
        try {
            if (wanted) {
                nfc.enableReaderMode(activity, new NfcAdapter.ReaderCallback() {
                    public void onTagDiscovered(Tag tag) {
                        final JSONObject scan = describe(tag); // on a binder thread: read it here
                        main.post(new Runnable() {
                            public void run() {
                                listener.onScan("nfc", scan);
                            }
                        });
                    }
                }, READER_FLAGS, null);
            } else {
                nfc.disableReaderMode(activity);
            }
            reading = wanted;
        } catch (Exception e) {
            reading = false; // NFC switched off, or not in front any more
        }
    }

    /** Every key event, before the page gets it. Never consumes it. */
    void key(KeyEvent e) {
        if (e.getAction() != 0 /* DOWN */ || e.getRepeatCount() > 0) return;
        long t = e.getEventTime();
        int code = e.getKeyCode();
        if (code == 66 /* ENTER */ || code == 160 /* NUMPAD_ENTER */ || code == 61 /* TAB */) {
            long span = lastKeyAt - firstKeyAt;
            boolean fast = keys.length() >= MIN_LENGTH && t - lastKeyAt <= MAX_GAP_MS * 2
                    && span <= (keys.length() - 1) * MAX_GAP_MS;
            if (fast) {
                JSONObject scan = Json.obj("code", keys.toString());
                listener.onScan("barcode", scan);
            }
            keys.setLength(0);
            return;
        }
        int ch = e.getUnicodeChar();
        if (ch <= 0) return; // shift and other modifiers come between a scanner's characters
        if (keys.length() == 0 || t - lastKeyAt > MAX_GAP_MS) {
            keys.setLength(0);
            firstKeyAt = t;
        }
        if (keys.length() < 512) keys.append((char) ch);
        lastKeyAt = t;
    }

    /** {id, techs, records: [{tnf, type, text | uri | mime + data}]} */
    static JSONObject describe(Tag tag) {
        JSONObject out = Json.obj("id", hex(tag.getId()));
        JSONArray techs = new JSONArray();
        String[] list = tag.getTechList();
        for (int i = 0; list != null && i < list.length; i++) {
            String t = list[i];
            techs.put(t.substring(t.lastIndexOf('.') + 1));
        }
        Json.put(out, "techs", techs);
        JSONArray records = new JSONArray();
        try {
            Ndef ndef = Ndef.get(tag);
            NdefMessage msg = ndef == null ? null : ndef.getCachedNdefMessage();
            NdefRecord[] recs = msg == null ? null : msg.getRecords();
            for (int i = 0; recs != null && i < recs.length && i < 16; i++) records.put(record(recs[i]));
        } catch (Exception ignored) {
            // just the id, then
        }
        Json.put(out, "records", records);
        return out;
    }

    private static JSONObject record(NdefRecord r) {
        short tnf = r.getTnf();
        String type = r.getType() == null ? "" : new String(r.getType(), Charset.forName("US-ASCII"));
        byte[] p = r.getPayload() == null ? new byte[0] : r.getPayload();
        JSONObject o = Json.obj("tnf", (int) tnf, "type", type);
        if (tnf == 1 /* WELL_KNOWN */ && "T".equals(type) && p.length > 0) {
            int langLen = p[0] & 0x3F;
            boolean utf16 = (p[0] & 0x80) != 0;
            if (1 + langLen <= p.length) {
                Json.put(o, "lang", new String(p, 1, langLen, Charset.forName("US-ASCII")));
                Json.put(o, "text", new String(p, 1 + langLen, p.length - 1 - langLen,
                        Charset.forName(utf16 ? "UTF-16" : "UTF-8")));
            }
        } else if ((tnf == 1 && "U".equals(type)) || tnf == 3 /* ABSOLUTE_URI */) {
            Json.put(o, "uri", String.valueOf(r.toUri()));
        } else if (tnf == 2 /* MIME_MEDIA */) {
            Json.put(o, "mime", type.toLowerCase(Locale.ROOT));
            Json.put(o, "data", Base64.encodeToString(p, Base64.NO_WRAP));
        } else {
            Json.put(o, "data", Base64.encodeToString(p, Base64.NO_WRAP));
        }
        return o;
    }

    static String hex(byte[] b) {
        if (b == null) return "";
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format(Locale.ROOT, "%02x", x & 0xff));
        return sb.toString();
    }
}
