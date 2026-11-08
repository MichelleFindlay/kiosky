package uk.lp0.ha;

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

public class MainActivity extends Activity {

    private static final String DEFAULT_URL = "https://ha.lp0.uk";
    private static final String PREFS = "kiosk";
    private static final String KEY_URL = "url";
    private static final long RETRY_MS = 10000L;
    private static final long BAR_TIMEOUT_MS = 15000L;

    private WebView web;
    private LinearLayout urlBar;
    private EditText urlField;
    private final Handler handler = new Handler();

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
        s.setDomStorageEnabled(true);   // HA keeps its auth tokens in localStorage
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
                // Server unreachable (Wi-Fi waking, HA restarting): try again shortly
                handler.removeCallbacks(retry);
                handler.postDelayed(retry, RETRY_MS);
            }
        });
        root.addView(web, new FrameLayout.LayoutParams(-1, -1)); // MATCH_PARENT

        buildUrlBar(root);

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
            web.loadUrl(savedUrl());
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

        Button reset = new Button(this);
        reset.setText("Default");
        reset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                applyUrl(DEFAULT_URL);
            }
        });
        urlBar.addView(reset, new LinearLayout.LayoutParams(-2, -2));

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

        web.loadUrl(url);
        goImmersive();
    }

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
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
