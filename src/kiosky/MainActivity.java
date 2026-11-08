package kiosky;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends Activity {

    // Bundled instructions (app/assets/help.md), shown until an address is set
    private static final String HELP_URL = "file:///android_asset/help.md";
    private static final String DEFAULT_URL = HELP_URL;
    private static final String PREFS = "kiosk";
    private static final String KEY_URL = "url";
    private static final long RETRY_MS = 10000L;
    private static final long BAR_TIMEOUT_MS = 15000L;
    private static final long FIRST_CHECK_MS = 60 * 1000L;          // let Wi-Fi settle after launch
    private static final long CHECK_EVERY_MS = 12 * 60 * 60 * 1000L;

    private WebView web;
    private LinearLayout urlBar;
    private EditText urlField;
    private final Handler handler = new Handler();

    private Updater updater;
    private LinearLayout updateBar;
    private TextView updateText;
    private Button updateInstall;
    private String updateVersion, updateUrl, dismissedVersion;
    private long updateSize;

    private final Runnable autoCheck = new Runnable() {
        public void run() {
            updater.check(false);
            handler.postDelayed(autoCheck, CHECK_EVERY_MS);
        }
    };

    private final Runnable retry = new Runnable() {
        public void run() {
            if (web != null) web.reload();
        }
    };

    private final Runnable autoHide = new Runnable() {
        public void run() {
            // Leave it alone while someone is typing in it
            if (urlField != null && urlField.hasFocus()) {
                handler.postDelayed(autoHide, BAR_TIMEOUT_MS);
            } else {
                goImmersive();
            }
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        // Keep the screen on (wall-panel use)
        getWindow().addFlags(128 /* FLAG_KEEP_SCREEN_ON */);

        FrameLayout root = new FrameLayout(this);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);   // many dashboards keep sign-in tokens in localStorage
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(-1 /* LOAD_DEFAULT */);

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(web, true);

        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false; // keep every link, including login redirects, inside the app
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                CookieManager.getInstance().flush();
            }

            @Override
            public void onReceivedError(WebView view, int code, String desc, String url) {
                // Server unreachable (Wi-Fi waking, server restarting): try again shortly
                handler.removeCallbacks(retry);
                handler.postDelayed(retry, RETRY_MS);
            }
        });
        root.addView(web, new FrameLayout.LayoutParams(-1, -1)); // MATCH_PARENT

        buildUrlBar(root);
        buildUpdateBar(root);

        updater = new Updater(this, new Updater.Listener() {
            public void onUpdateAvailable(String version, String apkUrl, long size) {
                if (version.equals(dismissedVersion)) return; // "Later" was tapped for this one
                updateVersion = version;
                updateUrl = apkUrl;
                updateSize = size;
                updateText.setText("Kiosky " + version + " is available (you have " + updater.version() + ")");
                updateInstall.setEnabled(true);
                updateBar.setVisibility(0 /* VISIBLE */);
            }

            public void onInstallFailed() {
                if (updateVersion == null) return;
                updateText.setText("Kiosky " + updateVersion + " is available");
                updateInstall.setEnabled(true);
            }
        });
        handler.postDelayed(autoCheck, FIRST_CHECK_MS);

        // Edge swipe reveals the system bars; show the URL bar alongside them
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
                    public void onSystemUiVisibilityChange(int vis) {
                        if ((vis & 0x4 /* SYSTEM_UI_FLAG_FULLSCREEN */) == 0) {
                            showUrlBar();
                        } else {
                            hideUrlBar();
                        }
                    }
                });

        setContentView(root);

        if (state != null) {
            web.restoreState(state);
        } else {
            open(savedUrl());
        }
    }

    private void buildUrlBar(FrameLayout root) {
        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (8 * d);

        // Sit below the status bar, which overlaps the top of the screen
        int statusBar = 0;
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (id > 0) statusBar = getResources().getDimensionPixelSize(id);

        urlBar = new LinearLayout(this);
        urlBar.setOrientation(0 /* HORIZONTAL */);
        urlBar.setGravity(16 /* CENTER_VERTICAL */);
        urlBar.setBackgroundColor(0xEE1C1C1E);
        urlBar.setPadding(pad, statusBar + pad, pad, pad);
        urlBar.setVisibility(8 /* GONE */);

        urlField = new EditText(this);
        urlField.setSingleLine(true);
        urlField.setInputType(0x11 /* TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_URI */);
        urlField.setImeOptions(2 /* IME_ACTION_GO */);
        urlField.setSelectAllOnFocus(true);
        urlField.setTextColor(0xFFFFFFFF);
        urlField.setHintTextColor(0xFF8E8E93);
        urlField.setHint("https://...");
        urlField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent e) {
                boolean enter = e != null && e.getKeyCode() == 66 && e.getAction() == 0;
                if (actionId == 2 || enter) {
                    applyUrl(urlField.getText().toString());
                    return true;
                }
                return false;
            }
        });
        urlBar.addView(urlField, new LinearLayout.LayoutParams(0, -2, 1f));

        Button go = new Button(this);
        go.setText("Go");
        go.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                applyUrl(urlField.getText().toString());
            }
        });
        urlBar.addView(go, new LinearLayout.LayoutParams(-2, -2));

        Button reload = new Button(this);
        reload.setText("Reload");
        reload.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                web.reload();
                goImmersive();
            }
        });
        urlBar.addView(reload, new LinearLayout.LayoutParams(-2, -2));

        Button help = new Button(this);
        help.setText("Help");
        help.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showHelp(); // view only; the saved address stays as it is
                goImmersive();
            }
        });
        urlBar.addView(help, new LinearLayout.LayoutParams(-2, -2));

        Button update = new Button(this);
        update.setText("Update");
        update.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismissedVersion = null; // a manual check always shows what it finds
                updater.check(true);
            }
        });
        urlBar.addView(update, new LinearLayout.LayoutParams(-2, -2));

        Button close = new Button(this);
        close.setText("✕");
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                goImmersive();
            }
        });
        urlBar.addView(close, new LinearLayout.LayoutParams(-2, -2));

        root.addView(urlBar, new FrameLayout.LayoutParams(-1, -2, 48 /* Gravity.TOP */));
    }

    private void buildUpdateBar(FrameLayout root) {
        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (8 * d);

        updateBar = new LinearLayout(this);
        updateBar.setOrientation(0 /* HORIZONTAL */);
        updateBar.setGravity(16 /* CENTER_VERTICAL */);
        updateBar.setBackgroundColor(0xEE1C1C1E);
        updateBar.setPadding(pad * 2, pad, pad, pad);
        updateBar.setVisibility(8 /* GONE */);

        updateText = new TextView(this);
        updateText.setTextColor(0xFFFFFFFF);
        updateBar.addView(updateText, new LinearLayout.LayoutParams(0, -2, 1f));

        updateInstall = new Button(this);
        updateInstall.setText("Install");
        updateInstall.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                updateInstall.setEnabled(false);
                updateText.setText("Downloading Kiosky " + updateVersion + "…");
                updater.install(updateUrl, updateSize);
            }
        });
        updateBar.addView(updateInstall, new LinearLayout.LayoutParams(-2, -2));

        Button later = new Button(this);
        later.setText("Later");
        later.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dismissedVersion = updateVersion;
                updateBar.setVisibility(8 /* GONE */);
            }
        });
        updateBar.addView(later, new LinearLayout.LayoutParams(-2, -2));

        root.addView(updateBar, new FrameLayout.LayoutParams(-1, -2, 80 /* Gravity.BOTTOM */));
    }

    private String savedUrl() {
        return getSharedPreferences(PREFS, 0).getString(KEY_URL, DEFAULT_URL);
    }

    private void applyUrl(String raw) {
        String url = raw == null ? "" : raw.trim();
        if (url.length() == 0) url = DEFAULT_URL;
        if (url.indexOf("://") < 0) url = "https://" + url;

        SharedPreferences.Editor ed = getSharedPreferences(PREFS, 0).edit();
        ed.putString(KEY_URL, url);
        ed.apply();

        open(url);
        goImmersive();
    }

    private void open(String url) {
        if (HELP_URL.equals(url)) {
            showHelp();
        } else {
            web.loadUrl(url);
        }
    }

    private void showHelp() {
        // WebView can't display Markdown, so convert it to HTML first
        web.loadDataWithBaseURL(HELP_URL, helpHtml(), "text/html", "utf-8", HELP_URL);
    }

    private String helpHtml() {
        String body;
        try {
            body = Markdown.toHtml(readAsset("help.md").replace("{{version}}", Updater.currentVersion(this)));
        } catch (IOException e) {
            body = "<h1>Kiosky</h1><p>The help page couldn't be loaded. Swipe in from the top "
                    + "or bottom edge to open the toolbar and enter an address.</p>";
        }
        return "<!doctype html><html><head><meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>Kiosky help</title><style>" + HELP_CSS + "</style></head>"
                + "<body>" + HELP_ARROW + "<main>" + body + "</main></body></html>";
    }

    private String readAsset(String name) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(getAssets().open(name), "UTF-8"));
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
            + "tr:last-child td{border-bottom:0}hr{border:0;border-top:1px solid var(--line);margin:2rem 0}";

    private void showUrlBar() {
        urlField.setText(savedUrl());
        urlBar.setVisibility(0 /* VISIBLE */);
        handler.removeCallbacks(autoHide);
        handler.postDelayed(autoHide, BAR_TIMEOUT_MS);
    }

    private void hideUrlBar() {
        handler.removeCallbacks(autoHide);
        if (urlBar == null) return;
        InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
        if (imm != null) imm.hideSoftInputFromWindow(urlField.getWindowToken(), 0);
        urlField.clearFocus();
        urlBar.setVisibility(8 /* GONE */);
        web.requestFocus();
    }

    private void goImmersive() {
        // LAYOUT_STABLE | LAYOUT_HIDE_NAV | LAYOUT_FULLSCREEN | HIDE_NAV | FULLSCREEN | IMMERSIVE
        // Non-sticky immersive, so an edge swipe tells the app the bars are showing
        getWindow().getDecorView().setSystemUiVisibility(
                0x100 | 0x200 | 0x400 | 0x2 | 0x4 | 0x800);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && (urlBar == null || urlBar.getVisibility() != 0)) goImmersive();
    }

    @Override
    protected void onResume() {
        super.onResume();
        goImmersive();
        if (web != null) web.onResume();
    }

    @Override
    protected void onPause() {
        if (web != null) web.onPause();
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (web != null) web.saveState(out);
    }

    @Override
    public void onBackPressed() {
        if (urlBar != null && urlBar.getVisibility() == 0) {
            goImmersive();
        } else if (web != null && web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(retry);
        handler.removeCallbacks(autoHide);
        handler.removeCallbacks(autoCheck);
        if (updater != null) updater.destroy();
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
