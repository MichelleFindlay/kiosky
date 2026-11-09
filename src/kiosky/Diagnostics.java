package kiosky;

import android.content.Context;
import android.os.Build;
import android.util.Base64;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

/**
 * The diagnostics command (PROTOCOL.md §7): a gzipped text file with versions, the
 * status report, recent page console errors, the last crash and Kiosky's own log.
 * No page content, cookies or device token.
 */
final class Diagnostics {

    private static final int MAX_CONSOLE = 200;
    private final ArrayDeque<String> console = new ArrayDeque<String>();

    /** Keeps page console warnings and errors (from onConsoleMessage). */
    synchronized void console(String level, String message, String source, int line) {
        if (console.size() >= MAX_CONSOLE) console.removeFirst();
        String m = message == null ? "" : message;
        if (m.length() > 500) m = m.substring(0, 500) + "…";
        console.addLast(new Date() + " " + level + " " + m + " (" + source + ":" + line + ")");
    }

    /** Base64 of the gzipped report. Main thread; reading the log takes a moment. */
    String bundle(Context c, JSONObject status, String policyVersion, int lines) throws Exception {
        StringBuilder b = new StringBuilder();
        b.append("Kiosky diagnostics, ").append(new Date()).append('\n')
         .append("Kiosky ").append(Updater.currentVersion(c)).append(" (").append(Updater.currentVersionCode(c)).append(")\n")
         .append("Device ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
         .append(", Android ").append(Build.VERSION.RELEASE).append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n")
         .append("Policy version ").append(policyVersion).append("\n\n");

        b.append("== Status ==\n").append(status == null ? "{}" : status.toString(2)).append("\n\n");

        b.append("== Page console (warnings and errors, newest last) ==\n");
        synchronized (this) {
            for (String s : console) b.append(s).append('\n');
        }
        b.append('\n');

        String crash = Crashes.last(c);
        b.append("== Last crash ==\n").append(crash == null ? "none recorded" : new JSONObject(crash).toString(2)).append("\n\n");

        b.append("== Log (last ").append(lines).append(" lines) ==\n").append(log(lines));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gz = new GZIPOutputStream(out);
        gz.write(b.toString().getBytes("UTF-8"));
        gz.close();
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
    }

    /** Kiosky's own logcat lines; apps can only read their own log anyway. */
    private static String log(int lines) {
        try {
            String[] cmd = Build.VERSION.SDK_INT >= 24
                    ? new String[] {"logcat", "-d", "-v", "time", "-t", String.valueOf(lines), "--pid=" + android.os.Process.myPid()}
                    : new String[] {"logcat", "-d", "-v", "time", "-t", String.valueOf(lines)};
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null && sb.length() < 8 * 1024 * 1024) sb.append(line).append('\n');
            r.close();
            p.destroy();
            return sb.toString();
        } catch (Exception e) {
            return "couldn't read the log: " + e + "\n";
        }
    }
}
