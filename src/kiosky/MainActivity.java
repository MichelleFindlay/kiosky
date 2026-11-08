package kiosky;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

public class MainActivity extends Activity {

    static final String PREFS = "kiosk"; // also read by BootReceiver
    private static final String KEY_URL = "url";
    static final String KEY_BOOT = "start_on_boot";
    static final String KEY_LOCKDOWN = "lockdown";
    private static final String DEFAULT_URL = Pages.HELP_URL; // instructions until an address is set
    private static final long RETRY_MS = 10000L;
    private static final long BAR_TIMEOUT_MS = 15000L;
    private static final long FIRST_CHECK_MS = 60 * 1000L;          // let Wi-Fi settle after launch
    private static final long CHECK_EVERY_MS = 12 * 60 * 60 * 1000L;
    private static final long HOUR_MS = 60 * 60 * 1000L;
    private static final int REQ_SCAN = 42;
    private static final int MAX_PIN_TRIES = 5;
    private static final long PIN_LOCKOUT_MS = 5 * 60 * 1000L;

    private final Handler handler = new Handler();

    private FrameLayout root;
    private WebView web;
    private String defaultUserAgent;

    // Toolbar
    private LinearLayout urlBar;
    private EditText urlField;
    private TextView mgmtStatus;
    private Button goButton, updateButton, bootToggle, enrollButton, infoButton, overlayButton, installButton;
    private Button lockToggle, ownerButton;

    // Update bar
    private Updater updater;
    private LinearLayout updateBar;
    private TextView updateText;
    private Button updateInstall;
    private String updateVersion, updateUrl, updateAuth, updateSha, dismissedVersion;
    private long updateSize;
    private int updateCode, failedCode;
    private long failedAt;

    // Management
    private ManagedState managed;
    private Shepherd shepherd;
    private Policy policy;            // null while unmanaged
    private boolean unlocked;         // technician PIN entered; until the toolbar hides
    private String tempHost;          // host of an admin-sent or technician address, exempt from allowed_hosts
    private boolean onTemporaryPage;  // a typed or sent address that isn't the start page
    private View corner;
    private final long[] cornerTaps = new long[5];
    private int pinFails;
    private long pinLockedUntil;
    private View overlay;

    // Page state, for status reports and events
    private String pageError;
    private boolean errorThisLoad;
    private String lastLoadedUrl, lastErrorKey;

    private final Runnable autoCheck = new Runnable() {
        public void run() {
            long next = CHECK_EVERY_MS;
            if (policy == null) {
                updater.check(false);
            } else if ("github".equals(policy.updateSource)) {
                updater.check(false);
                next = policy.checkHours * HOUR_MS;
            } else {
                next = HOUR_MS; // look again later in case the policy switches to github
            }
            handler.postDelayed(autoCheck, next);
        }
    };

    private final Runnable retry = new Runnable() {
        public void run() {
            if (web != null) web.reload();
        }
    };

    private final Runnable periodicReload = new Runnable() {
        public void run() {
            if (web == null || policy == null || policy.reloadMinutes <= 0) return;
            web.reload();
            handler.postDelayed(periodicReload, policy.reloadMinutes * 60000L);
        }
    };

    private final Runnable autoHide = new Runnable() {
        public void run() {
            // Leave it alone while someone is typing in it
            if (urlField != null && urlField.hasFocus()) {
                handler.postDelayed(autoHide, barTimeoutMs());
            } else {
                dismissToolbar();
            }
        }
    };

    // Notification shade blocking
    private boolean resumed;
    private int ownDialogs; // our dialogs take focus too; don't mistake them for the shade

    private final Runnable closeShade = new Runnable() {
        public void run() {
            if (!resumed || hasWindowFocus() || ownDialogs > 0) return;
            Lockdown.collapseShade(MainActivity.this);
        }
    };

    private final BroadcastReceiver network = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            if (!i.getBooleanExtra("noConnectivity", false) && shepherd != null) shepherd.onNetworkBack();
        }
    };

    // ======================================================================
    // Lifecycle
    // ======================================================================

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        managed = new ManagedState(this);
        policy = managed.isManaged() ? Policy.parse(managed.policyJson()) : null;
        if (managed.isManaged() && policy == null) policy = new Policy(new JSONObject()); // locked defaults

        root = new FrameLayout(this);
        buildWebView();
        buildCorner();
        buildUrlBar();
        buildUpdateBar();
        setContentView(root);

        updater = new Updater(this, new Updater.Listener() {
            public void onUpdateAvailable(String version, String apkUrl, long size) {
                offerUpdate(version, 0, apkUrl, size, null, null);
            }

            public void onInstallFailed(String reason) {
                updateFailed(reason);
            }
        });
        handler.postDelayed(autoCheck, FIRST_CHECK_MS);

        shepherd = new Shepherd(this, shepherdHost, managed);
        registerReceiver(network, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

        // Edge swipe reveals the system bars; show the toolbar alongside them
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
                    public void onSystemUiVisibilityChange(int vis) {
                        if ((vis & 0x4 /* SYSTEM_UI_FLAG_FULLSCREEN */) == 0) {
                            showUrlBar();
                        } else if (!unlocked) {
                            hideUrlBar();
                        }
                    }
                });

        applyPolicySettings();

        if (state != null) {
            web.restoreState(state);
        } else {
            loadHome();
            handleIntent(getIntent());
        }

        if (policy != null) {
            managed.addEvent("app_started", null);
            // The cached policy was applied above; report it as applied
            if (policy.version.length() > 0) managed.setApplied(policy.version);
            shepherd.start();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && (urlBar == null || urlBar.getVisibility() != 0)) goImmersive();
        // Losing focus while still in front, with none of our own dialogs open, means
        // the notification shade (or another system panel) was pulled over Kiosky
        if (!hasFocus && resumed && ownDialogs == 0) {
            for (long delay : new long[] {0, 100, 250, 500, 1000, 2000}) handler.postDelayed(closeShade, delay);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        Lockdown.allowLeaveUntil = 0; // back in Kiosky: any planned trip away is over
        handler.removeCallbacks(returnToKiosky);
        setStatusBarBlocked(true);
        applyScreenOn(); // re-apply after returning from Settings or the install screen
        refreshToolbar(); // permissions may have just been granted
        goImmersive();
        if (web != null) web.onResume();
        handler.post(enforceLockdown); // once the activity is fully in front
    }

    @Override
    protected void onPause() {
        resumed = false;
        setStatusBarBlocked(false); // Settings or an install prompt may need it
        statusBarBlocker.hide(this);
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
            dismissToolbar();
        } else if (web != null && web.canGoBack()) {
            web.goBack();
        } else if (policy != null && !policy.allowExit) {
            // Managed and not allowed to close: stay put
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != REQ_SCAN || result != -1 /* RESULT_OK */ || data == null) return;
        String text = data.getStringExtra("SCAN_RESULT");
        if (text != null && text.startsWith("kiosky://enroll")) {
            handleEnrollUri(Uri.parse(text));
        } else {
            toast("That QR code isn't a Kiosky enrollment code");
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(retry);
        handler.removeCallbacks(autoHide);
        handler.removeCallbacks(autoCheck);
        handler.removeCallbacks(periodicReload);
        handler.removeCallbacks(enforceLockdown);
        handler.removeCallbacks(closeShade);
        handler.removeCallbacks(returnToKiosky);
        statusBarBlocker.hide(this);
        try {
            unregisterReceiver(network);
        } catch (Exception ignored) {
            // not registered
        }
        if (shepherd != null) shepherd.stop();
        if (updater != null) updater.destroy();
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    // ======================================================================
    // Building the screen
    // ======================================================================

    private void buildWebView() {
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);   // many dashboards keep sign-in tokens in localStorage
        s.setDatabaseEnabled(true);
        s.setCacheMode(-1 /* LOAD_DEFAULT */);
        defaultUserAgent = s.getUserAgentString();

        CookieManager.getInstance().setAcceptCookie(true);

        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (!allowed(url)) {
                    blocked(url);
                    return true;
                }
                return false; // keep every allowed link, including login redirects, inside the app
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                errorThisLoad = false;
                if (!allowed(url)) { // catches redirects that skip shouldOverrideUrlLoading
                    view.stopLoading();
                    blocked(url);
                    if (view.canGoBack()) view.goBack(); else loadHome();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                CookieManager.getInstance().flush();
                if (errorThisLoad) return;
                pageError = null;
                lastErrorKey = null;
                if (url != null && !url.equals(lastLoadedUrl) && !isInternal(url)) {
                    lastLoadedUrl = url;
                    managed.addEvent("page_loaded", Json.obj("url", url, "title", view.getTitle()));
                }
            }

            @Override
            public void onReceivedError(WebView view, int code, String desc, String url) {
                errorThisLoad = true;
                pageError = desc + " (" + code + ")";
                String key = code + " " + url;
                if (!key.equals(lastErrorKey)) { // once per failure, not on every retry
                    lastErrorKey = key;
                    lastLoadedUrl = null;
                    managed.addEvent("page_error", Json.obj("code", code, "description", desc, "url", url));
                }
                // Server unreachable (Wi-Fi waking, server restarting): try again shortly
                handler.removeCallbacks(retry);
                handler.postDelayed(retry, policy != null ? policy.retrySeconds * 1000L : RETRY_MS);
            }
        });
        root.addView(web, new FrameLayout.LayoutParams(-1, -1)); // MATCH_PARENT
    }

    /**
     * Five quick taps in the top-left corner open the technician PIN pad (§5.3), or,
     * on an unmanaged tablet in lockdown, Kiosky's menu. A fallback in case an edge
     * swipe can't reveal the system bars while the tablet is locked down.
     */
    private void buildCorner() {
        int size = (int) (48 * getResources().getDisplayMetrics().density);
        corner = new View(this);
        corner.setVisibility(8 /* GONE */);
        corner.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                long now = System.currentTimeMillis();
                System.arraycopy(cornerTaps, 1, cornerTaps, 0, cornerTaps.length - 1);
                cornerTaps[cornerTaps.length - 1] = now;
                if (now - cornerTaps[0] <= 3000L) {
                    for (int i = 0; i < cornerTaps.length; i++) cornerTaps[i] = 0;
                    if (policy == null) showUrlBar(); // unmanaged lockdown: straight to the menu
                    else showPinPad();
                }
            }
        });
        root.addView(corner, new FrameLayout.LayoutParams(size, size, 48 | 3 /* TOP | LEFT */));
    }

    private void buildUrlBar() {
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

        mgmtStatus = new TextView(this);
        mgmtStatus.setTextColor(0xFFFFB4A9);
        mgmtStatus.setPadding(0, 0, pad, 0);
        urlBar.addView(mgmtStatus, new LinearLayout.LayoutParams(-2, -2));

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

        goButton = button("Go", new Runnable() {
            public void run() {
                applyUrl(urlField.getText().toString());
            }
        });
        button("Reload", new Runnable() {
            public void run() {
                web.reload();
                dismissToolbar();
            }
        });
        button("Help", new Runnable() {
            public void run() {
                showHelp(); // view only; the saved address stays as it is
                dismissToolbar();
            }
        });
        updateButton = button("Update", new Runnable() {
            public void run() {
                dismissedVersion = null; // a manual check always shows what it finds
                updater.check(true);
            }
        });
        bootToggle = button("", new Runnable() {
            public void run() {
                setStartOnBoot(!startOnBoot(MainActivity.this));
            }
        });
        enrollButton = button("Enroll", new Runnable() {
            public void run() {
                showEnrollForm(null, null);
            }
        });
        lockToggle = button("", new Runnable() {
            public void run() {
                lockdownTapped();
            }
        });
        ownerButton = button("Remove device owner", new Runnable() {
            public void run() {
                confirmRemoveOwner();
            }
        });
        infoButton = button("Device info", new Runnable() {
            public void run() {
                showDeviceInfo();
            }
        });
        overlayButton = button("Allow start on boot", new Runnable() {
            public void run() {
                openOverlaySettings();
            }
        });
        installButton = button("Allow updates", new Runnable() {
            public void run() {
                openSettings("android.settings.MANAGE_UNKNOWN_APP_SOURCES");
            }
        });
        button("✕", new Runnable() {
            public void run() {
                dismissToolbar();
            }
        });

        root.addView(urlBar, new FrameLayout.LayoutParams(-1, -2, 48 /* Gravity.TOP */));
    }

    private Button button(String label, final Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                handler.removeCallbacks(autoHide);
                handler.postDelayed(autoHide, barTimeoutMs());
                action.run();
            }
        });
        urlBar.addView(b, new LinearLayout.LayoutParams(-2, -2));
        return b;
    }

    private void buildUpdateBar() {
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
                startInstall();
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

    // ======================================================================
    // Pages and navigation
    // ======================================================================

    private String savedUrl() {
        return getSharedPreferences(PREFS, 0).getString(KEY_URL, DEFAULT_URL);
    }

    /** The start page: the policy's when managed, the saved address otherwise. */
    private String homeUrl() {
        if (policy == null) return savedUrl();
        return policy.url.length() == 0 ? Pages.WAITING_URL : policy.url;
    }

    private void loadHome() {
        tempHost = null;
        onTemporaryPage = false;
        open(homeUrl());
    }

    private void open(String url) {
        if (Pages.HELP_URL.equals(url)) {
            showHelp();
        } else if (Pages.WAITING_URL.equals(url)) {
            String org = policy != null ? policy.organisation : "your organisation";
            web.loadDataWithBaseURL(Pages.WAITING_URL, Pages.waiting(org, managed.name()),
                    "text/html", "utf-8", Pages.WAITING_URL);
        } else {
            web.loadUrl(url);
        }
    }

    private void showHelp() {
        // WebView can't display Markdown, so it's converted to HTML first
        web.loadDataWithBaseURL(Pages.HELP_URL, Pages.help(this), "text/html", "utf-8", Pages.HELP_URL);
    }

    private static boolean isInternal(String url) {
        return url == null || url.startsWith("file:///android_asset/") || url.startsWith("about:")
                || url.startsWith("data:");
    }

    /** allowed_hosts enforcement (§5.2). Unmanaged tablets can go anywhere. */
    private boolean allowed(String url) {
        if (policy == null || isInternal(url) || url.equals(policy.url)) return true;
        if (tempHost != null && tempHost.equals(Policy.host(url))) return true;
        return policy.allows(url);
    }

    private void blocked(String url) {
        managed.addEvent("navigation_blocked", Json.obj("url", url));
        toast("This site isn't allowed on this tablet");
    }

    /** The address box. Unmanaged: saved. Managed (full toolbar or unlocked): temporary. */
    private void applyUrl(String raw) {
        String url = raw == null ? "" : raw.trim();
        if (policy != null) {
            if (url.length() == 0) {
                loadHome();
            } else {
                if (url.indexOf("://") < 0) url = "https://" + url;
                if (unlocked) tempHost = Policy.host(url); // a technician may go anywhere
                onTemporaryPage = true;
                web.loadUrl(url); // never saved; restart, go_home or a policy change returns home
            }
            dismissToolbar();
            return;
        }

        if (url.length() == 0) url = DEFAULT_URL;
        if (url.indexOf("://") < 0) url = "https://" + url;
        SharedPreferences.Editor ed = getSharedPreferences(PREFS, 0).edit();
        ed.putString(KEY_URL, url);
        ed.apply();
        open(url);
        dismissToolbar();
    }

    // ======================================================================
    // Toolbar
    // ======================================================================

    private long barTimeoutMs() {
        return policy != null ? policy.toolbarTimeoutSeconds * 1000L : BAR_TIMEOUT_MS;
    }

    private boolean toolbarAllowed() {
        return policy == null || unlocked || !"hidden".equals(policy.toolbarMode);
    }

    /** Shows exactly the controls this state allows (§5.2 toolbar.mode). */
    private void refreshToolbar() {
        boolean isManaged = policy != null;
        boolean addressBox = !isManaged || unlocked || "full".equals(policy.toolbarMode);
        show(urlField, addressBox);
        show(goButton, addressBox);
        show(updateButton, !isManaged);
        show(bootToggle, !isManaged);
        show(enrollButton, !isManaged);
        show(infoButton, isManaged && unlocked);
        boolean owner = isDeviceOwner();
        // Managed: the policy decides; after the technician PIN the checklist can still be opened
        show(lockToggle, !isManaged || (unlocked && policy.lockdown));
        String mode = Lockdown.mode(this);
        lockToggle.setText(!lockdownWanted() ? "Lockdown: Off"
                : ("owner".equals(mode) ? "Lockdown: On" : ("mdm".equals(mode) ? "Lockdown: On (MDM)" : "Lockdown: On (partial)")));
        show(ownerButton, owner && !lockdownWanted() && (!isManaged || unlocked));
        show(corner, isManaged ? policy.pin != null : lockdownWanted());
        show(overlayButton, isManaged && unlocked && policy.startOnBoot && !DeviceInfo.overlayPermission(this));
        show(installButton, isManaged && unlocked && !DeviceInfo.installPermission(this));
        String err = shepherd != null ? shepherd.lastError : null;
        mgmtStatus.setText(err != null ? err : "");
        show(mgmtStatus, isManaged && unlocked && err != null);

        if (!isManaged) {
            if (!startOnBoot(this)) {
                bootToggle.setText("Start on boot: Off");
            } else if (!DeviceInfo.overlayPermission(this)) {
                bootToggle.setText("Start on boot: Needs permission");
            } else {
                bootToggle.setText("Start on boot: On");
            }
        }
    }

    private static void show(View v, boolean visible) {
        if (v != null) v.setVisibility(visible ? 0 : 8);
    }

    private void showUrlBar() {
        if (!toolbarAllowed()) return; // managed "hidden": the system bars show, the toolbar doesn't
        refreshToolbar();
        if (policy == null) {
            urlField.setText(savedUrl());
        } else {
            String current = web.getUrl();
            urlField.setText(current == null || isInternal(current) ? policy.url : current);
        }
        urlBar.setVisibility(0 /* VISIBLE */);
        handler.removeCallbacks(autoHide);
        handler.postDelayed(autoHide, barTimeoutMs());
    }

    private void hideUrlBar() {
        handler.removeCallbacks(autoHide);
        if (urlBar == null) return;
        InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
        if (imm != null) imm.hideSoftInputFromWindow(urlField.getWindowToken(), 0);
        urlField.clearFocus();
        urlBar.setVisibility(8 /* GONE */);
        unlocked = false; // the technician unlock lasts until the toolbar hides
        web.requestFocus();
    }

    private void dismissToolbar() {
        hideUrlBar();
        goImmersive();
    }

    private void goImmersive() {
        // LAYOUT_STABLE | LAYOUT_HIDE_NAV | LAYOUT_FULLSCREEN | HIDE_NAV | FULLSCREEN | IMMERSIVE
        // Non-sticky immersive, so an edge swipe tells the app the bars are showing
        getWindow().getDecorView().setSystemUiVisibility(
                0x100 | 0x200 | 0x400 | 0x2 | 0x4 | 0x800);
    }

    // ======================================================================
    // Policy (§5)
    // ======================================================================

    /** Applies every setting that doesn't involve navigating. */
    private void applyPolicySettings() {
        Policy p = policy;
        WebSettings s = web.getSettings();
        s.setTextZoom(p != null ? p.textZoom : 100);
        s.setUserAgentString(p != null && p.userAgentSuffix.length() > 0
                ? defaultUserAgent + " " + p.userAgentSuffix : defaultUserAgent);
        s.setMediaPlaybackRequiresUserGesture(p != null && !p.autoplayMedia);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, p == null || p.thirdPartyCookies);
        applyScreenOn();
        setRequestedOrientation(p != null ? p.orientationCode() : -1);

        if (!toolbarAllowed()) hideUrlBar();
        refreshToolbar();

        handler.removeCallbacks(periodicReload);
        if (p != null && p.reloadMinutes > 0) handler.postDelayed(periodicReload, p.reloadMinutes * 60000L);
    }

    private void applyScreenOn() {
        boolean on = policy == null || policy.keepScreenOn;
        // Window flag plus view flag, so it holds even if one of them is replaced
        if (on) getWindow().addFlags(128 /* FLAG_KEEP_SCREEN_ON */);
        else getWindow().clearFlags(128);
        if (root != null) root.setKeepScreenOn(on);
    }

    private final Shepherd.Host shepherdHost = new Shepherd.Host() {
        public JSONObject status() {
            JSONObject s = DeviceInfo.status(MainActivity.this, web.getUrl(), web.getTitle(), pageError,
                    web.getSettings().getUserAgentString());
            Json.put(s, "device_owner", isDeviceOwner());
            Json.put(s, "lockdown", lockdownWanted());
            Json.put(s, "lock_task", inLockTask());
            Json.put(s, "lockdown_mode", Lockdown.mode(MainActivity.this));
            Json.put(s, "accessibility_helper", KioskAccessibility.running());
            Json.put(s, "default_home", Lockdown.isDefaultHome(MainActivity.this));
            return s;
        }

        public void onPolicy(Policy p) {
            String oldUrl = policy != null ? policy.url : null;
            boolean wasLocked = lockdownWanted();
            policy = p;
            applyPolicySettings();
            if (p.lockdown != wasLocked) applyLockdown(p.lockdown, true);
            if (oldUrl == null || !oldUrl.equals(p.url) || onTemporaryPage) loadHome();
            managed.setApplied(p.version);
            managed.addEvent("policy_applied", Json.obj("version", p.version));
        }

        public void onUpdateOffer(JSONObject u) {
            int code = u.optInt("version_code", 0);
            if (code <= Updater.currentVersionCode(MainActivity.this)) return; // never downgrade
            offerUpdate(Json.str(u, "version", String.valueOf(code)), code, Json.str(u, "url", null),
                    u.optLong("size", -1), "Bearer " + managed.token(), Json.str(u, "sha256", null));
        }

        public JSONObject runCommand(String type, JSONObject params) {
            return command(type, params);
        }

        public void restartApp() {
            recreate();
        }

        public void onRetired(Policy last) {
            String org = last != null ? last.organisation : "your organisation";
            // Keep the screen as it is: the managed start page becomes the local one
            SharedPreferences.Editor ed = getSharedPreferences(PREFS, 0).edit();
            if (last != null && last.url.length() > 0) ed.putString(KEY_URL, last.url);
            if (last != null) ed.putBoolean(KEY_BOOT, last.startOnBoot);
            ed.putBoolean(KEY_LOCKDOWN, last != null && last.lockdown); // stays as it was
            ed.apply();
            boolean waiting = Pages.WAITING_URL.equals(web.getUrl()) || (last != null && last.url.length() == 0);
            policy = null;
            unlocked = false;
            tempHost = null;
            applyPolicySettings();
            if (waiting) loadHome();
            showOverlay("This tablet is no longer managed by " + org, 8, false);
        }

        public void onEnrolled(Policy p, String name) {
            boolean wasLocked = lockdownWanted();
            policy = p;
            applyPolicySettings();
            if (p.lockdown != wasLocked) applyLockdown(p.lockdown, true);
            loadHome();
            dismissToolbar();
            managed.setApplied(p.version);
            managed.addEvent("policy_applied", Json.obj("version", p.version));
            toast("This tablet is now managed by " + p.organisation + (name.length() > 0 ? " as " + name : ""));
        }

        public void onEnrollFailed(String message) {
            toast(message);
        }
    };

    // ======================================================================
    // Commands (§7)
    // ======================================================================

    private JSONObject command(String type, JSONObject params) {
        if ("reload".equals(type)) {
            web.reload();
        } else if ("go_home".equals(type)) {
            loadHome();
        } else if ("navigate".equals(type)) {
            String url = Json.str(params, "url", "");
            if (!url.startsWith("https://")) return failed("url must start with https://");
            tempHost = Policy.host(url); // admin-sent addresses skip allowed_hosts
            onTemporaryPage = true;
            web.loadUrl(url);
        } else if ("clear_cache".equals(type)) {
            web.clearCache(true);
            web.reload();
        } else if ("clear_web_data".equals(type)) {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
            WebStorage.getInstance().deleteAllData();
            web.clearCache(true);
            web.clearHistory();
            loadHome();
        } else if ("check_update".equals(type)) {
            if ("off".equals(policy.updateSource)) return failed("updates are turned off in the policy");
            if ("github".equals(policy.updateSource)) updater.check(false); // server: the next check-in brings it
        } else if ("show_message".equals(type)) {
            String text = Json.str(params, "text", "");
            if (text.length() == 0) return failed("no text");
            showOverlay(text, Json.clamp(params.optInt("seconds", 30), 1, 86400), false);
        } else if ("identify".equals(type)) {
            showOverlay(managed.name() + "\n" + managed.deviceId(),
                    Json.clamp(params.optInt("seconds", 30), 1, 3600), true);
        } else if ("screenshot".equals(type)) {
            return screenshot(Json.clamp(params.optInt("max_width", 1280), 64, 4096),
                    Json.clamp(params.optInt("quality", 70), 10, 100));
        } else {
            return failed("unsupported command");
        }
        return Json.obj("status", "done");
    }

    private static JSONObject failed(String message) {
        return Json.obj("status", "failed", "message", message);
    }

    private JSONObject screenshot(int maxWidth, int quality) {
        int w = web.getWidth(), h = web.getHeight();
        if (w <= 0 || h <= 0) return failed("the page isn't on screen");
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        web.draw(new Canvas(bmp));
        if (w > maxWidth) {
            Bitmap scaled = Bitmap.createScaledBitmap(bmp, maxWidth, Math.max(1, h * maxWidth / w), true);
            bmp.recycle();
            bmp = scaled;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, out);
        JSONObject r = Json.obj("status", "done",
                "data", Json.obj("width", bmp.getWidth(), "height", bmp.getHeight()),
                "screenshot", Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP));
        bmp.recycle();
        return r;
    }

    /** Large centred text over the page, with a dismiss button (show_message, identify). */
    private void showOverlay(String text, int seconds, boolean huge) {
        if (overlay != null) root.removeView(overlay);
        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (24 * d);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(1 /* VERTICAL */);
        box.setGravity(17 /* CENTER */);
        box.setBackgroundColor(huge ? 0xFF0A66D8 : 0xEE1C1C1E);
        box.setPadding(pad, pad, pad, pad);
        box.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // swallow taps so they don't reach the page underneath
            }
        });

        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(huge ? 56 : 32);
        t.setGravity(17 /* CENTER */);
        box.addView(t, new LinearLayout.LayoutParams(-1, -2));

        Button dismiss = new Button(this);
        dismiss.setText("Dismiss");
        final View shown = box;
        final Runnable remove = new Runnable() {
            public void run() {
                if (overlay == shown) {
                    root.removeView(shown);
                    overlay = null;
                }
            }
        };
        dismiss.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                remove.run();
            }
        });
        box.addView(dismiss, new LinearLayout.LayoutParams(-2, -2));

        overlay = box;
        root.addView(box, new FrameLayout.LayoutParams(-1, -1));
        handler.postDelayed(remove, seconds * 1000L);
    }

    // ======================================================================
    // Updates (§6)
    // ======================================================================

    /** {@code code} is 0 for GitHub releases, whose version code isn't known up front. */
    private void offerUpdate(String version, int code, String url, long size, String auth, String sha) {
        if (url == null || updater.isBusy()) return;
        if (policy != null && "off".equals(policy.updateSource)) return;
        if (code != 0 && code == failedCode && System.currentTimeMillis() - failedAt < HOUR_MS) return;
        updateVersion = version;
        updateCode = code;
        updateUrl = url;
        updateSize = size;
        updateAuth = auth;
        updateSha = sha;
        if (policy != null && policy.autoInstall) {
            startInstall();
        } else if (!version.equals(dismissedVersion)) { // "Later" was tapped for this one
            updateText.setText("Kiosky " + version + " is available (you have " + updater.version() + ")");
            updateInstall.setEnabled(true);
            updateBar.setVisibility(0 /* VISIBLE */);
        }
    }

    private void startInstall() {
        if (updateUrl == null || updater.isBusy()) return;
        updateInstall.setEnabled(false);
        updateText.setText("Downloading Kiosky " + updateVersion + "…");
        managed.addEvent("update_started", Json.obj("version", updateVersion, "version_code", updateCode));
        // A device owner installs silently; a pinned tablet must unpin so Android's prompt can show
        if (!isDeviceOwner()) leaveLockForExternalScreen();
        else Lockdown.allowLeave(10 * 60 * 1000L);
        updater.install(updateUrl, updateSize, updateAuth, updateSha);
    }

    private void updateFailed(String reason) {
        managed.addEvent("update_failed", Json.obj("version", updateVersion, "version_code", updateCode,
                "reason", reason));
        failedCode = updateCode;
        failedAt = System.currentTimeMillis();
        if (updateVersion == null) return;
        updateText.setText("Kiosky " + updateVersion + " is available");
        updateInstall.setEnabled(true);
    }

    // ======================================================================
    // Start on boot
    // ======================================================================

    /** Managed: the policy decides. Unmanaged: the local toggle. */
    static boolean startOnBoot(Context c) {
        if (ManagedState.isManaged(c)) {
            Policy p = ManagedState.cachedPolicy(c);
            return p == null || p.startOnBoot;
        }
        return c.getSharedPreferences(PREFS, 0).getBoolean(KEY_BOOT, false);
    }

    private void setStartOnBoot(boolean on) {
        SharedPreferences.Editor ed = getSharedPreferences(PREFS, 0).edit();
        ed.putBoolean(KEY_BOOT, on);
        ed.apply();
        refreshToolbar();

        if (on && !DeviceInfo.overlayPermission(this)) {
            toast("Turn on \"Display over other apps\" for Kiosky so it can open when the tablet starts, then go back");
            openOverlaySettings();
        } else {
            toast(on ? "Kiosky will open when the tablet starts" : "Kiosky won't open on its own at start-up");
        }
    }

    private void openOverlaySettings() {
        openSettings("android.settings.action.MANAGE_OVERLAY_PERMISSION");
    }

    private void openSettings(String action) {
        leaveLockForExternalScreen();
        try {
            startActivity(new Intent(action, Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            toast("Open Settings › Apps › Kiosky to change this");
        }
    }

    // ======================================================================
    // Lockdown (see Lockdown.java for the layers and where each one works)
    // ======================================================================

    private final Lockdown.StatusBarBlocker statusBarBlocker = new Lockdown.StatusBarBlocker();

    private final Runnable enforceLockdown = new Runnable() {
        public void run() {
            if (!lockdownWanted() || !resumed) return;
            statusBarBlocker.show(MainActivity.this);
            if (inLockTask()) return;
            try {
                if (Lockdown.isOwner(MainActivity.this)) {
                    DevicePolicyManager dpm = Lockdown.dpm(MainActivity.this);
                    dpm.setLockTaskPackages(admin(), new String[] {getPackageName()});
                    if (Build.VERSION.SDK_INT >= 28) dpm.setLockTaskFeatures(admin(), 0 /* LOCK_TASK_FEATURE_NONE */);
                }
                if (!Lockdown.fullLockPossible(MainActivity.this)) {
                    Lockdown.quietUntil = System.currentTimeMillis() + 5000; // let the pin prompt show
                }
                startLockTask(); // full lock task if allowed, screen pinning otherwise
            } catch (Exception e) {
                // Not in front yet, or refused; the next resume tries again
            }
        }
    };

    /** Brings Kiosky back after it was left without going through its menu. */
    private final Runnable returnToKiosky = new Runnable() {
        public void run() {
            if (!resumed && lockdownWanted() && !Lockdown.leaveAllowed() && !isFinishing()) {
                Lockdown.bringToFront(MainActivity.this);
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        if (lockdownWanted() && !Lockdown.leaveAllowed() && !Lockdown.fullLockPossible(this)) {
            handler.postDelayed(returnToKiosky, 300);
        }
    }

    private boolean lockdownWanted() {
        return policy != null ? policy.lockdown : Lockdown.localSetting(this);
    }

    private ComponentName admin() {
        return Lockdown.admin(this);
    }

    private boolean isDeviceOwner() {
        return Lockdown.isOwner(this);
    }

    private boolean inLockTask() {
        try {
            ActivityManager am = (ActivityManager) getSystemService("activity");
            if (Build.VERSION.SDK_INT >= 23) return am.getLockTaskModeState() != 0 /* NONE */;
            return am.isInLockTaskMode();
        } catch (Exception e) {
            return false;
        }
    }

    /** The toolbar's Lockdown button. */
    private void lockdownTapped() {
        if (policy != null) { // managed: set by the server, so only the checklist
            showLockdownChecklist();
            return;
        }
        if (!lockdownWanted()) {
            setLockdown(true);
            if (Lockdown.fullLockPossible(this)) {
                dismissToolbar();
                toast("Locked down. Swipe in from an edge, or tap the top-left corner five times, to open Kiosky's menu");
            } else {
                showLockdownChecklist();
            }
        } else {
            showLockdownChecklist();
        }
    }

    /** The local toggle (unmanaged tablets only). */
    private void setLockdown(boolean on) {
        Lockdown.setLocalSetting(this, on);
        applyLockdown(on, false);
        if (!on) toast("Lockdown is off. The system menus and buttons work again");
    }

    /** Turns lockdown on or off. {@code fromPolicy}: the Shepherd policy changed it. */
    private void applyLockdown(boolean on, boolean fromPolicy) {
        JSONObject data = Json.obj("mode", Lockdown.mode(this));
        if (fromPolicy) Json.put(data, "source", "policy");
        managed.addEvent(on ? "lockdown_on" : "lockdown_off", data);
        Lockdown.setHome(this, on);
        if (on) {
            handler.post(enforceLockdown);
        } else {
            handler.removeCallbacks(returnToKiosky);
            statusBarBlocker.hide(this);
            stopLock();
        }
        refreshToolbar();
    }

    /**
     * Shows how strong the lockdown is and lets each missing layer be switched on.
     * Tapping an item opens the right settings screen; Lockdown resumes on return.
     */
    private void showLockdownChecklist() {
        final boolean on = lockdownWanted();
        String mode = Lockdown.mode(this);
        final boolean local = policy == null; // managed tablets follow the server
        if (!"partial".equals(mode)) {
            AlertDialog.Builder b = dialog()
                    .setTitle("owner".equals(mode) ? "Full lockdown (device owner)" : "Full lockdown (MDM)")
                    .setMessage("The notification shade, Home, Recents and the power menu are blocked. "
                            + "Kiosky's menu is the only way out."
                            + (local ? "" : "\n\nLockdown is set by " + policy.organisation + "."))
                    .setNegativeButton("Close", null);
            if (local) {
                b.setPositiveButton(on ? "Turn off lockdown" : "Turn on lockdown", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        setLockdown(!on);
                    }
                });
            }
            b.show();
            return;
        }

        final java.util.List<String> labels = new java.util.ArrayList<String>();
        final java.util.List<Runnable> actions = new java.util.ArrayList<Runnable>();
        int sdk = Build.VERSION.SDK_INT;

        boolean helper = KioskAccessibility.running();
        labels.add((helper ? "✓ " : "✗ ") + "Accessibility helper: returns to Kiosky if it's left"
                + (sdk >= 31 ? " and closes the notification shade" : "") + (helper ? "" : " (tap to turn on)"));
        actions.add(new Runnable() {
            public void run() {
                toast("Turn on Kiosky under Installed apps or Downloaded apps, then go back");
                openScreen(new Intent("android.settings.ACCESSIBILITY_SETTINGS"));
            }
        });
        if (sdk >= 33 && !helper) {
            labels.add("   If Kiosky is greyed out there: App info › ⋮ › Allow restricted settings (tap)");
            actions.add(new Runnable() {
                public void run() {
                    openSettings("android.settings.APPLICATION_DETAILS_SETTINGS");
                }
            });
        }
        if (sdk >= 23) {
            boolean overlay = android.provider.Settings.canDrawOverlays(this);
            String what = sdk < 26 ? "blocks the notification shade" : (sdk >= 29 ? "lets Kiosky return when it's left" : "start on boot");
            labels.add((overlay ? "✓ " : "✗ ") + "Display over other apps: " + what + (overlay ? "" : " (tap to allow)"));
            actions.add(new Runnable() {
                public void run() {
                    openSettings("android.settings.action.MANAGE_OVERLAY_PERMISSION");
                }
            });
        }
        boolean home = Lockdown.isDefaultHome(this);
        labels.add((home ? "✓ " : "✗ ") + "Kiosky is the Home app: the Home button comes back here" + (home ? "" : " (tap to choose)"));
        actions.add(new Runnable() {
            public void run() {
                toast("Choose Kiosky as the Home app, then go back");
                if (!openScreen(new Intent("android.settings.HOME_SETTINGS"))) {
                    openScreen(new Intent("android.intent.action.MAIN").addCategory("android.intent.category.HOME"));
                }
            }
        });
        labels.add("• Screen pinning: blocks Home and Recents. Turn on \"Ask for PIN before unpinning\" (tap)");
        actions.add(new Runnable() {
            public void run() {
                toast("Find App pinning (or Screen pinning) and turn on asking for a PIN to unpin");
                openScreen(new Intent("android.settings.SECURITY_SETTINGS"));
            }
        });
        labels.add("• For full lockdown on any Android version, make Kiosky the device owner (tap for help)");
        actions.add(new Runnable() {
            public void run() {
                showHelp();
                dismissToolbar();
            }
        });

        AlertDialog.Builder b = dialog()
                .setTitle(local ? (on ? "Lockdown is on (partial)" : "Lockdown")
                        : "Partial lockdown, set by " + policy.organisation)
                .setItems(labels.toArray(new CharSequence[0]), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        actions.get(which).run();
                    }
                })
                .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        if (lockdownWanted()) dismissToolbar();
                    }
                });
        if (local) {
            b.setPositiveButton(on ? "Turn off lockdown" : "Turn on lockdown", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    setLockdown(!on);
                }
            });
        }
        b.show();
    }

    // ======================================================================
    // Notification shade
    // ======================================================================

    /**
     * As device owner, turns the notification shade off while Kiosky is in front
     * (Android 6+). This is the only fully reliable way.
     */
    private void setStatusBarBlocked(boolean blocked) {
        if (Build.VERSION.SDK_INT < 23 || !isDeviceOwner()) return;
        try {
            Lockdown.dpm(this).setStatusBarDisabled(admin(), blocked);
        } catch (Exception ignored) {
            // not permitted on this device
        }
    }

    /** All of Kiosky's dialogs go through here, so they aren't mistaken for the shade. */
    private AlertDialog.Builder dialog() {
        ownDialogs++;
        return new AlertDialog.Builder(this).setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface d) {
                if (ownDialogs > 0) ownDialogs--;
            }
        });
    }

    private void stopLock() {
        try {
            if (inLockTask()) stopLockTask();
        } catch (Exception ignored) {
            // wasn't locked
        }
    }

    /**
     * Before opening Settings, a scanner or an install prompt on purpose: lets Kiosky
     * be left for a while, and unlocks the task so the screen can open. Resume re-locks.
     */
    private void leaveLockForExternalScreen() {
        Lockdown.allowLeave(10 * 60 * 1000L);
        if (lockdownWanted()) stopLock();
    }

    /** Opens a system screen without a package address. Returns false if there's none. */
    private boolean openScreen(Intent i) {
        leaveLockForExternalScreen();
        try {
            startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void confirmRemoveOwner() {
        dialog()
                .setTitle("Remove device owner?")
                .setMessage("Full lockdown won't be possible until Kiosky is made device owner again with adb. "
                        + "Do this before uninstalling Kiosky: Android won't uninstall a device owner.")
                .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int which) {
                        stopLock();
                        try {
                            setStatusBarBlocked(false);
                            Lockdown.setHome(MainActivity.this, false);
                            Lockdown.dpm(MainActivity.this).clearDeviceOwnerApp(getPackageName());
                            toast("Kiosky is no longer the device owner");
                        } catch (Exception e) {
                            toast("Couldn't remove device owner: " + e.getMessage());
                        }
                        refreshToolbar();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ======================================================================
    // Technician PIN (§5.3)
    // ======================================================================

    private void showPinPad() {
        if (policy == null || policy.pin == null) return;
        long wait = pinLockedUntil - System.currentTimeMillis();
        if (wait > 0) {
            toast("Too many wrong PINs. Try again in " + ((wait + 59999) / 60000) + " min");
            return;
        }
        final EditText field = new EditText(this);
        field.setInputType(0x12 /* TYPE_CLASS_NUMBER | TYPE_NUMBER_VARIATION_PASSWORD */);
        field.setHint("PIN");
        dialog()
                .setTitle("Technician PIN")
                .setView(field)
                .setPositiveButton("Unlock", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int which) {
                        checkPin(field.getText().toString());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkPin(final String entered) {
        final JSONObject spec = policy.pin;
        new Thread(new Runnable() {
            public void run() {
                final boolean ok = Pin.verify(entered, spec); // slow by design
                handler.post(new Runnable() {
                    public void run() {
                        if (ok) {
                            pinFails = 0;
                            unlocked = true;
                            showUrlBar();
                            if (shepherd.lastError != null) toast(shepherd.lastError);
                        } else if (++pinFails >= MAX_PIN_TRIES) {
                            pinFails = 0;
                            pinLockedUntil = System.currentTimeMillis() + PIN_LOCKOUT_MS;
                            managed.addEvent("pin_failed", Json.obj("attempts", MAX_PIN_TRIES));
                            toast("Too many wrong PINs. PIN entry is locked for 5 minutes");
                        } else {
                            toast("Wrong PIN");
                        }
                    }
                });
            }
        }).start();
    }

    private void showDeviceInfo() {
        StringBuilder b = new StringBuilder();
        b.append("Managed by: ").append(policy.organisation)
         .append("\nServer: ").append(managed.server())
         .append("\nDevice: ").append(managed.name()).append(" (").append(managed.deviceId()).append(')')
         .append("\nKiosky: ").append(Updater.currentVersion(this))
         .append("\nPolicy version: ").append(managed.appliedVersion());
        long last = shepherd.lastCheckinAt;
        b.append("\nLast check-in: ").append(last == 0 ? "not yet since start"
                : ((System.currentTimeMillis() - last) / 1000) + " s ago");
        b.append("\nStatus: ").append(shepherd.lastError != null ? shepherd.lastError : "OK");
        b.append("\nStart on boot permission: ").append(DeviceInfo.overlayPermission(this) ? "granted" : "missing");
        b.append("\nInstall permission: ").append(DeviceInfo.installPermission(this) ? "granted" : "missing");
        dialog()
                .setTitle("Device info")
                .setMessage(b.toString())
                .setPositiveButton("OK", null)
                .setNeutralButton("Check in now", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int which) {
                        shepherd.checkinNow();
                    }
                })
                .show();
    }

    // ======================================================================
    // Enrollment (§3)
    // ======================================================================

    private void handleIntent(Intent i) {
        if (i == null || !"android.intent.action.VIEW".equals(i.getAction()) || i.getData() == null) return;
        Uri u = i.getData();
        if ("kiosky".equals(u.getScheme()) && "enroll".equals(u.getHost())) handleEnrollUri(u);
    }

    private void handleEnrollUri(Uri u) {
        if (policy != null) {
            // Never re-enroll a managed tablet: that would be a way out of lockdown
            toast("This tablet is already managed by " + policy.organisation);
            return;
        }
        confirmEnroll(u.getQueryParameter("server"), u.getQueryParameter("token"));
    }

    private void showEnrollForm(String server, String code) {
        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (20 * d);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(1 /* VERTICAL */);
        form.setPadding(pad, pad / 2, pad, 0);

        final EditText serverField = new EditText(this);
        serverField.setSingleLine(true);
        serverField.setInputType(0x11 /* URI */);
        serverField.setHint("Server, e.g. kiosks.example.com");
        if (server != null) serverField.setText(server);
        form.addView(serverField, new LinearLayout.LayoutParams(-1, -2));

        final EditText codeField = new EditText(this);
        codeField.setSingleLine(true);
        codeField.setInputType(0x1001 /* TEXT | CAP_CHARACTERS */);
        codeField.setHint("Code, e.g. ABCD-2EFG-H3JK");
        if (code != null) codeField.setText(code);
        form.addView(codeField, new LinearLayout.LayoutParams(-1, -2));

        dialog()
                .setTitle("Enroll with a Shepherd server")
                .setMessage("Enter the server and code from your administrator, or scan its QR code.")
                .setView(form)
                .setPositiveButton("Enroll", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int which) {
                        confirmEnroll(serverField.getText().toString(), codeField.getText().toString());
                    }
                })
                .setNeutralButton("Scan QR", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int which) {
                        scanQr();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void scanQr() {
        leaveLockForExternalScreen();
        try {
            Intent scan = new Intent("com.google.zxing.client.android.SCAN");
            scan.putExtra("SCAN_MODE", "QR_CODE_MODE");
            startActivityForResult(scan, REQ_SCAN);
        } catch (Exception e) {
            toast("No QR scanner app found. Scan the code with the camera app, or type the server and code");
        }
    }

    private void confirmEnroll(String rawServer, final String rawCode) {
        String s = rawServer == null ? "" : rawServer.trim();
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        final String code = rawCode == null ? "" : rawCode.trim();
        if (s.length() == 0 || code.length() == 0) {
            toast("Enter both the server and the code");
            showEnrollForm(s, code);
            return;
        }
        if (s.regionMatches(true, 0, "http://", 0, 7)) {
            toast("The server must use https://");
            return;
        }
        if (s.indexOf("://") < 0) s = "https://" + s;
        final String server = s;
        String host = Policy.host(server);
        if (host == null || !server.regionMatches(true, 0, "https://", 0, 8)) {
            toast("That server address doesn't look right");
            return;
        }

        dialog()
                .setTitle("Hand over this tablet?")
                .setMessage("Let " + host + " manage this tablet? Its settings will be locked, and only "
                        + "that server can release it.")
                .setPositiveButton("Enroll", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int which) {
                        toast("Enrolling…");
                        shepherd.enroll(server, code);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toast(String text) {
        Toast.makeText(this, text, 1 /* LENGTH_LONG */).show();
    }
}
