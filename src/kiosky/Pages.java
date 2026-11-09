package kiosky;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/** The built-in pages: help (from app/assets/help.md) and the managed "waiting" screen. */
final class Pages {

    static final String HELP_URL = "file:///android_asset/help.md";
    static final String WAITING_URL = "file:///android_asset/waiting";

    private Pages() {}

    /** {@code start}: shown as the start page, with the arrow pointing at the swipe edge. */
    static String help(Context c, boolean start) {
        String body;
        try {
            body = Markdown.toHtml(readAsset(c, "help.md").replace("{{version}}", Updater.currentVersion(c)));
        } catch (IOException e) {
            body = "<h1>Kiosky</h1><p>The help page couldn't be loaded. Swipe in from the top "
                    + "or bottom edge to open the toolbar and enter an address.</p>";
        }
        return page("Kiosky help", start ? HELP_ARROW + "<main>" + body + "</main>"
                : "<main class=\"plain\">" + body + "</main>");
    }

    /** Shown on a managed tablet whose policy has no start page yet. */
    static String waiting(String organisation, String deviceName) {
        return page("Kiosky", "<main class=\"center\"><p><img src=\"logo.png\" alt=\"\"></p>"
                + "<h1>Waiting for configuration</h1>"
                + "<p>This tablet is managed by <strong>" + esc(organisation) + "</strong>. "
                + "It will show its page as soon as an administrator sets one.</p>"
                + (deviceName == null || deviceName.length() == 0 ? ""
                        : "<p class=\"muted\">Device name: " + esc(deviceName) + "</p>")
                + "</main>");
    }

    private static String page(String title, String body) {
        return "<!doctype html><html><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>" + esc(title) + "</title><style>" + HELP_CSS + "</style></head>"
                + "<body>" + body + "</body></html>";
    }

    static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String readAsset(Context c, String name) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(c.getAssets().open(name), "UTF-8"));
        try {
            StringBuilder b = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = r.read(buf)) > 0) b.append(buf, 0, n);
            return b.toString();
        } finally {
            r.close();
        }
    }

    // Points at the top edge, where a swipe opens the toolbar
    private static final String HELP_ARROW =
            "<div class=\"edge\" aria-hidden=\"true\"><svg viewBox=\"0 0 24 24\" width=\"44\" height=\"44\">"
            + "<path d=\"M12 21V4M5 11l7-7 7 7\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2.5\" "
            + "stroke-linecap=\"round\" stroke-linejoin=\"round\"/></svg>"
            + "<span>Swipe down from this edge to open the menu</span></div>";

    private static final String HELP_CSS =
            ":root{color-scheme:light dark;--bg:#f5f5f7;--fg:#1c1c1e;--muted:#6e6e73;--line:#d2d2d7;"
            + "--card:#fff;--accent:#0a66d8}"
            + "@media (prefers-color-scheme:dark){:root{--bg:#1c1c1e;--fg:#f2f2f7;--muted:#a1a1a6;"
            + "--line:#3a3a3c;--card:#2c2c2e;--accent:#4ea1ff}}"
            + "body{margin:0;background:var(--bg);color:var(--fg);"
            + "font:18px/1.6 system-ui,-apple-system,Roboto,sans-serif}"
            + "main{max-width:44rem;margin:0 auto;padding:8rem 1.5rem 4rem}"
            + ".edge{position:fixed;top:0;left:0;right:0;z-index:1;pointer-events:none;display:flex;"
            + "flex-direction:column;align-items:center;gap:.2rem;padding:.6rem 1rem 1.6rem;text-align:center;"
            + "color:var(--accent);font-weight:600;font-size:1rem;background:linear-gradient(var(--bg) 65%,transparent)}"
            + ".edge::before{content:\"\";position:absolute;top:0;left:0;right:0;height:4px;background:var(--accent)}"
            + ".edge svg{animation:nudge 1.6s ease-in-out infinite}"
            + "@keyframes nudge{0%,100%{transform:translateY(0)}50%{transform:translateY(-8px)}}"
            + "@media (prefers-reduced-motion:reduce){.edge svg{animation:none}}"
            + "img{max-width:100%}main>p:first-child img{display:block;width:8rem;height:8rem;margin:0 0 1.25rem;"
            + "filter:drop-shadow(0 4px 10px rgba(0,0,0,.15))}"
            + "h1{font-size:2.2rem;line-height:1.2;margin:0 0 1rem}"
            + "h2{font-size:1.35rem;margin:2.5rem 0 .75rem;padding-top:1.5rem;border-top:1px solid var(--line)}"
            + "a{color:var(--accent)}li{margin:.4rem 0}"
            + "code{font:.9em ui-monospace,monospace;background:var(--card);border:1px solid var(--line);"
            + "border-radius:4px;padding:.1em .35em}"
            + "pre{background:var(--card);border:1px solid var(--line);border-radius:8px;padding:1rem;overflow-x:auto}"
            + "pre code{border:0;padding:0}"
            + "blockquote{margin:1.25rem 0;padding:.75rem 1rem;background:var(--card);"
            + "border-left:4px solid var(--accent);border-radius:4px}blockquote p{margin:0}"
            + ".table{overflow-x:auto}table{border-collapse:collapse;width:100%;background:var(--card);"
            + "border-radius:8px;overflow:hidden}"
            + "th,td{text-align:left;padding:.6rem .9rem;border-bottom:1px solid var(--line);vertical-align:top}"
            + "th{color:var(--muted);font-size:.85em;text-transform:uppercase;letter-spacing:.04em}"
            + "tr:last-child td{border-bottom:0}hr{border:0;border-top:1px solid var(--line);margin:2rem 0}"
            + ".center{text-align:center;padding-top:20vh}.center img{margin:0 auto 1.25rem!important}"
            + ".muted{color:var(--muted)}main.plain{padding-top:2rem}";
}
