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
import android.os.SystemClock;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.net.http.SslError;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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
    private static final int REQ_MEDIA = 43;
    private static final int REQ_FILE = 44;
    private static final String KEY_LAST_REBOOT = "last_reboot_day";
    private static final String KEY_PIN = "settings_pin";          // local settings PIN (unmanaged)
    private static final String KEY_MEDIA_HOSTS = "media_hosts";   // hosts allowed the camera/microphone
    private static final long TICK_MS = 15000L;
    private static final long MINUTE_MS = 60 * 1000L;
    private static final int JS_DIALOG_SECONDS = 30;
    private static final String RES_VIDEO = "android.webkit.resource.VIDEO_CAPTURE";
    private static final String RES_AUDIO = "android.webkit.resource.AUDIO_CAPTURE";
    private static final String PERM_CAMERA = "android.permission.CAMERA";
    private static final String PERM_MIC = "android.permission.RECORD_AUDIO";
    private static final int WAKE_FLAGS = 0x80000 | 0x200000 | 0x400000; // SHOW_WHEN_LOCKED | TURN_SCREEN_ON | DISMISS_KEYGUARD

    private final Handler handler = new Handler();

    private FrameLayout root;
    private WebView web;
    private String defaultUserAgent;

    // Toolbar
    private LinearLayout urlBar;
    private EditText urlField;
    private TextView mgmtStatus;
    private Button goButton, updateButton, bootToggle, enrollButton, infoButton, overlayButton, installButton;
    private Button lockToggle, ownerButton, settingsButton;

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
    private boolean pinPadOpen;
    private Runnable afterUnlock;     // what the local PIN was asked for, if not the toolbar
    private Options localOptions;     // unmanaged; managed tablets use policy.options

    // Page state, for status reports and events
    private String pageError;
    private boolean errorThisLoad;
    private String lastLoadedUrl, lastErrorKey;
    private String errorUrl;          // the failing address and when, to tie an error to its load
    private long errorAt;
    private OfflineScreen offline;
    private final long[] rendererGone = new long[3];

    // Watchdog: a JavaScript heartbeat that a frozen page can't answer
    private boolean heartbeatPending, watchdogReloaded;
    private long heartbeatSentAt;
    private int heartbeatGen;

    // Idle, screen schedule and dimming
    private long lastTouch, lastActivity, wakeUntil;   // SystemClock.elapsedRealtime()
    private boolean swallowGesture, idleDimmed, scheduledOff;
    private boolean manualOff;                        // window.kiosky.screenOff(): until a touch or wake
    private float pageBrightness = -1f;               // window.kiosky.setBrightness(); -1: the tablet's own
    private volatile String pageUrl;                  // for the page bridge, which runs on its own thread
    private final Battery battery = new Battery();
    private String batteryProtect = "off";
    private int ticks;
    /** restart_app: start clean instead of restoring the saved page. */
    private static boolean freshStart;

    private final Diagnostics diagnostics = new Diagnostics();
    private Speaker speaker;
    private ScreenSaver saver;
    private ValueCallback<Uri[]> fileCallback;   // a page's file picker, while it's open
    private int playlistIndex = -1;              // -1: the start page
    private long liveUntil;                       // live_view running until (elapsedRealtime)
    private int liveInterval, liveWidth, liveQuality;
    private boolean liveSending;
    private String authHost;                      // last HTTP sign-in we answered, to spot a refusal
    private long authAt;
    private boolean certDialogOpen;
    private int appliedVolume = -2;               // -2: not applied yet
    private int enrollTries;
    private final long[] offTaps = new long[5];       // five quick taps always wake a switched-off screen
    private int screenState = Options.NORMAL;
    private View blackout;
    private MotionWake motion;
    private boolean landingHome, clearHistoryOnLoad;
    private long landingUntil;
    private String homeLanded;        // where the start page actually ended up (redirects, SPA routes)

    // Camera and microphone, while Android's permission prompt is open
    private PermissionRequest pendingMedia;
    private String[] pendingGrant;

    // Help, shown over the page so the page itself stays loaded
    private LinearLayout helpBox;
    private WebView helpView;

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

    /** Every 15 s: screen schedule and dimming, going home when idle, the watchdog, the offline clock. */
    private final Runnable tick = new Runnable() {
        public void run() {
            handler.removeCallbacks(tick);
            updateScreen();
            checkIdleHome();
            watchdog();
            if (offline.showing()) offline.tick();
            if (ticks++ % 4 == 0) checkBattery(); // once a minute
            saver.tick();
            applyVolume();
            checkReboot();
            handler.postDelayed(tick, TICK_MS);
        }
    };

    private final Runnable playlistNext = new Runnable() {
        public void run() {
            advancePlaylist();
        }
    };

    private final Runnable liveFrame = new Runnable() {
        public void run() {
            sendLiveFrame();
        }
    };

    private final Runnable pendingEnroll = new Runnable() {
        public void run() {
            tryPendingEnroll();
        }
    };

    private final Runnable rebootNow = new Runnable() {
        public void run() {
            try {
                Lockdown.dpm(MainActivity.this).reboot(admin());
            } catch (Exception e) {
                managed.addEvent("reboot_failed", Json.obj("message", String.valueOf(e)));
            }
        }
    };

    private final Runnable loadHomeLater = new Runnable() {
        public void run() {
            loadHome();
        }
    };

    private final Runnable clearWakeFlags = new Runnable() {
        public void run() {
            if (screenState != Options.OFF) getWindow().clearFlags(WAKE_FLAGS);
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
            if (!i.getBooleanExtra("noConnectivity", false) && shepherd != null) {
                shepherd.onNetworkBack();
                if (Provisioning.pending(c) != null) {
                    handler.removeCallbacks(pendingEnroll);
                    handler.postDelayed(pendingEnroll, 3000);
                }
            }
        }
    };

    // ======================================================================
    // Lifecycle
    // ======================================================================

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Crashes.install(this);
        managed = new ManagedState(this);
        JSONObject crash = Crashes.takePending(this);
        if (crash != null) managed.addEvent("crash", crash);
        policy = managed.isManaged() ? Policy.parse(managed.policyJson()) : null;
        if (managed.isManaged() && policy == null) policy = new Policy(new JSONObject()); // locked defaults
        localOptions = Options.local(this);
        lastTouch = lastActivity = SystemClock.elapsedRealtime();
        motion = new MotionWake(this, new MotionWake.Listener() {
            public void onMotion(String source) {
                if (options().wakeOnMotion) wakeScreen(options().wakeMinutes);
            }
        });

        root = new FrameLayout(this);
        buildWebView();
        offline = new OfflineScreen(this, new Runnable() {
            public void run() {
                handler.removeCallbacks(retry);
                if (web != null) web.reload();
            }
        });
        root.addView(offline.view, new FrameLayout.LayoutParams(-1, -1));
        saver = new ScreenSaver(this, root);
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
        handler.postDelayed(pendingEnroll, 3000); // after QR provisioning: enroll once the network is up

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

        if (state != null && !freshStart) {
            web.restoreState(state);
        } else {
            loadHome();
            handleIntent(getIntent());
        }

        freshStart = false;

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
        if (intent != null && intent.getBooleanExtra(ScheduleReceiver.EXTRA_WAKE, false)) updateScreen();
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
        heartbeatPending = false; // time spent paused isn't a frozen page
        tick.run();
    }

    @Override
    protected void onPause() {
        resumed = false;
        setStatusBarBlocked(false); // Settings or an install prompt may need it
        statusBarBlocker.hide(this);
        motion.stop();
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
        if (saver.showing() || screenState == Options.OFF) {
            wakeScreen(options().wakeMinutes);
        } else if (urlBar != null && urlBar.getVisibility() == 0) {
            dismissToolbar();
        } else if (helpBox != null) {
            closeHelp();
        } else if (web != null && web.canGoBack()) {
            web.goBack();
        } else if ((policy != null && !policy.allowExit) || localPinSet() || lockdownWanted()) {
            // Not allowed to close, protected by the settings PIN, or locked down (closing
            // would end screen pinning and leave the tablet unlocked): stay put
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == REQ_FILE) {
            ValueCallback<Uri[]> cb = fileCallback;
            fileCallback = null;
            if (cb != null) cb.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result, data));
            return;
        }
        if (request != REQ_SCAN || result != -1 /* RESULT_OK */ || data == null) return;
        String text = data.getStringExtra("SCAN_RESULT");
        if (text != null && text.startsWith("kiosky://enroll")) {
            handleEnrollUri(Uri.parse(text));
        } else {
            toast("That QR code isn't a Kiosky enrollment code");
        }
    }

    /**
     * Every touch: counts as activity for idle dimming and going home, and wakes the
     * screen. A touch that wakes a dimmed or switched-off screen doesn't reach the page.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        if (action == 0 /* DOWN */) {
            long now = SystemClock.elapsedRealtime();
            boolean asleep = screenState == Options.OFF || idleDimmed || saver.showing();
            lastTouch = lastActivity = now;
            manualOff = false;
            Options o = options();
            if (scheduledOff && o.wakeOnTouch) wakeUntil = now + o.wakeMinutes * MINUTE_MS;
            if (screenState == Options.OFF && !o.wakeOnTouch) {
                // Escape hatch when touch isn't meant to wake it: five quick taps still do
                System.arraycopy(offTaps, 1, offTaps, 0, offTaps.length - 1);
                offTaps[offTaps.length - 1] = now;
                if (offTaps[0] != 0 && now - offTaps[0] <= 3000L) {
                    for (int i = 0; i < offTaps.length; i++) offTaps[i] = 0;
                    wakeUntil = now + o.wakeMinutes * MINUTE_MS;
                }
            }
            swallowGesture = asleep;
            if (asleep) updateScreen();
        }
        if (swallowGesture) {
            if (action == 1 /* UP */ || action == 3 /* CANCEL */) swallowGesture = false;
            return true;
        }
        return super.dispatchTouchEvent(e);
    }

    @Override
    public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        if (request != REQ_MEDIA || pendingMedia == null) return;
        PermissionRequest r = pendingMedia;
        String[] wanted = pendingGrant;
        pendingMedia = null;
        pendingGrant = null;
        List<String> grant = new ArrayList<String>();
        for (String res : wanted) {
            if (hasPermission(RES_VIDEO.equals(res) ? PERM_CAMERA : PERM_MIC)) grant.add(res);
        }
        if (grant.size() < wanted.length) {
            managed.addEvent("media_denied", Json.obj("origin", String.valueOf(r.getOrigin()),
                    "reason", "android_permission"));
        }
        if (grant.isEmpty()) r.deny();
        else r.grant(grant.toArray(new String[0]));
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(retry);
        handler.removeCallbacks(tick);
        handler.removeCallbacks(loadHomeLater);
        handler.removeCallbacks(clearWakeFlags);
        handler.removeCallbacks(playlistNext);
        handler.removeCallbacks(liveFrame);
        handler.removeCallbacks(pendingEnroll);
        handler.removeCallbacks(rebootNow);
        motion.stop();
        closeHelp();
        saver.hide();
        if (speaker != null) speaker.shutdown();
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        if (isFinishing()) ScheduleReceiver.cancel(this); // closed on purpose: don't reopen at on_at
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
        web.addJavascriptInterface(new Bridge(this), "kiosky");

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest r) {
                mediaRequested(r);
            }

            @Override
            public void onPermissionRequestCanceled(PermissionRequest r) {
                if (r == pendingMedia) {
                    pendingMedia = null;
                    pendingGrant = null;
                }
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                jsDialog("alert", message, null, result, null);
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                jsDialog("confirm", message, null, result, null);
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String def, JsPromptResult result) {
                jsDialog("prompt", message, def, result, result);
                return true;
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage m) {
                ConsoleMessage.MessageLevel level = m == null ? null : m.messageLevel();
                if (level == ConsoleMessage.MessageLevel.ERROR || level == ConsoleMessage.MessageLevel.WARNING) {
                    diagnostics.console(level.name(), m.message(), m.sourceId(), m.lineNumber());
                }
                return false;
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> cb, FileChooserParams params) {
                return chooseFile(cb, params);
            }

            @Override
            public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
                result.confirm(); // "Leave this page?" would block going home, reloads and the watchdog
                return true;
            }
        });
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
                // An HTTP error arrives just before its page starts; a network error just after
                boolean sameLoad = url != null && url.equals(errorUrl)
                        && SystemClock.elapsedRealtime() - errorAt < 2000;
                if (!sameLoad) errorThisLoad = false;
                if (!allowed(url)) { // catches redirects that skip shouldOverrideUrlLoading
                    view.stopLoading();
                    blocked(url);
                    if (view.canGoBack()) view.goBack(); else loadHome();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                CookieManager.getInstance().flush();
                if (clearHistoryOnLoad) { // back from the start page shouldn't lead anywhere
                    clearHistoryOnLoad = false;
                    view.clearHistory();
                }
                if (errorThisLoad) return;
                pageUrl = url;
                handler.removeCallbacks(retry);
                offline.hide();
                pageError = null;
                lastErrorKey = null;
                if (landingHome) {
                    landingHome = false;
                    homeLanded = url;
                    landingUntil = SystemClock.elapsedRealtime() + 10000; // in-page redirects (SPA routes)
                }
                if (url != null && !url.equals(lastLoadedUrl) && !isInternal(url)) {
                    lastLoadedUrl = url;
                    managed.addEvent("page_loaded", Json.obj("url", url, "title", view.getTitle()));
                }
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                pageUrl = url;
                if (url != null && SystemClock.elapsedRealtime() < landingUntil) homeLanded = url;
            }

            @Override
            public void onReceivedError(WebView view, int code, String desc, String url) {
                // Server unreachable (Wi-Fi waking, server restarting): try again shortly
                pageFailed(code, desc + " (" + code + ")", desc, url, true);
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest req, WebResourceResponse resp) {
                if (req == null || resp == null || !req.isForMainFrame()) return;
                int status = resp.getStatusCode();
                String reason = resp.getReasonPhrase();
                String text = "HTTP " + status + (reason != null && reason.length() > 0 ? " " + reason : "");
                // 5xx: the server is restarting or overloaded (Home Assistant answers 502 while
                // it starts). 408/429: try later. Other 4xx are real answers: leave them showing.
                boolean retryable = status >= 500 || status == 408 || status == 429;
                pageFailed(status, text, text, String.valueOf(req.getUrl()), retryable);
            }

            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler h, String host, String realm) {
                httpAuth(h, host, realm);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler h, SslError error) {
                sslError(h, error);
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                // Without this, Android 8+ kills the whole app when the page's renderer dies
                if (view != web) {
                    destroyQuietly(view);
                    return true;
                }
                rendererGone(detail != null && detail.didCrash());
                return true;
            }
        });
        root.addView(web, 0, new FrameLayout.LayoutParams(-1, -1)); // MATCH_PARENT, under everything else
    }

    /**
     * The page couldn't load. {@code retryable}: cover it with the offline screen and
     * keep trying; otherwise leave the server's answer showing.
     */
    private void pageFailed(int code, String status, String problem, String url, boolean retryable) {
        errorThisLoad = true;
        errorUrl = url;
        errorAt = SystemClock.elapsedRealtime();
        pageError = status;
        String key = code + " " + url;
        if (!key.equals(lastErrorKey)) { // once per failure, not on every retry
            lastErrorKey = key;
            lastLoadedUrl = null;
            managed.addEvent("page_error", Json.obj("code", code, "description", status, "url", url));
        }
        if (!retryable) { // a real answer (404 and the like): show it, stop retrying
            handler.removeCallbacks(retry);
            offline.hide();
            return;
        }
        int seconds = retrySeconds();
        offline.show(Policy.host(url), problem, seconds);
        handler.removeCallbacks(retry);
        handler.postDelayed(retry, seconds * 1000L);
    }

    private int retrySeconds() {
        return policy != null ? policy.retrySeconds : (int) (RETRY_MS / 1000);
    }

    /** The renderer crashed or was killed for memory: start again with a fresh WebView. */
    private void rendererGone(boolean crashed) {
        long now = SystemClock.elapsedRealtime();
        System.arraycopy(rendererGone, 1, rendererGone, 0, rendererGone.length - 1);
        rendererGone[rendererGone.length - 1] = now;
        // Three times in five minutes: the page itself is probably the cause, so slow down
        boolean looping = rendererGone[0] != 0 && now - rendererGone[0] < 5 * MINUTE_MS;
        managed.addEvent("renderer_crashed", Json.obj("crashed", crashed, "url", lastLoadedUrl, "looping", looping));
        rebuildWebView(looping ? MINUTE_MS : 0);
    }

    /** Replaces the WebView with a new one and loads the start page, after {@code delayMs}. */
    private void rebuildWebView(long delayMs) {
        WebView old = web;
        web = null;
        destroyQuietly(old);
        buildWebView();
        applyWebSettings();
        heartbeatPending = false;
        watchdogReloaded = false;
        lastLoadedUrl = null;
        handler.removeCallbacks(retry);
        handler.removeCallbacks(loadHomeLater);
        if (delayMs <= 0) {
            loadHome();
        } else {
            offline.showText("The page keeps stopping. Kiosky will open it again in "
                    + (delayMs / 1000) + " s.");
            handler.postDelayed(loadHomeLater, delayMs);
        }
        web.requestFocus();
    }

    private void destroyQuietly(WebView v) {
        if (v == null) return;
        try {
            root.removeView(v);
            v.destroy();
        } catch (Exception ignored) {
            // already gone
        }
    }

    /** {@code window.kiosky} for pages: lets a dashboard wake the screen (motion sensor automations). */
    public static final class Bridge {
        private final MainActivity a;

        Bridge(MainActivity a) {
            this.a = a;
        }

        @JavascriptInterface
        public void wake() {
            a.handler.post(new Runnable() {
                public void run() {
                    a.wakeScreen(a.options().wakeMinutes);
                }
            });
        }

        @JavascriptInterface
        public String version() {
            return Updater.currentVersion(a);
        }

        /** Whether the screen is off (schedule or screenOff()). */
        @JavascriptInterface
        public boolean isScreenOff() {
            return a.screenState == Options.OFF;
        }

        // The rest change the tablet, so only the kiosk's own trusted page may use them

        @JavascriptInterface
        public boolean reload() {
            if (!a.trustedPage()) return false;
            a.handler.post(new Runnable() {
                public void run() {
                    if (a.web != null) a.web.reload();
                }
            });
            return true;
        }

        /** 1–100 percent until the page sets another value; 0 or less: the tablet's own brightness. */
        @JavascriptInterface
        public boolean setBrightness(final int percent) {
            if (!a.trustedPage()) return false;
            a.handler.post(new Runnable() {
                public void run() {
                    a.pageBrightness = percent <= 0 ? -1f : Json.clamp(percent, 1, 100) / 100f;
                    a.updateScreen();
                }
            });
            return true;
        }

        /** Black screen until the next touch, wake() or motion (if motion wakes it). */
        @JavascriptInterface
        public boolean screenOff() {
            if (!a.trustedPage()) return false;
            a.handler.post(new Runnable() {
                public void run() {
                    a.manualOff = true;
                    a.updateScreen();
                }
            });
            return true;
        }

        /** The name given in Shepherd, or "" when unmanaged or untrusted. */
        @JavascriptInterface
        public String deviceName() {
            return a.trustedPage() && a.managed.isManaged() ? a.managed.name() : "";
        }
    }

    /**
     * The page now showing is the kiosk's own: managed, the start page's host, an
     * allowed_hosts entry or an admin-sent page; unmanaged, the saved address's host.
     * Any thread. Frames inside the page share window.kiosky, so this trusts the
     * page's address, not the frame that calls.
     */
    boolean trustedPage() {
        String url = pageUrl;
        String host = Policy.host(url);
        if (host == null || !url.regionMatches(true, 0, "https://", 0, 8)) return false;
        Policy p = policy;
        if (p != null) return p.trusted(url) || host.equals(tempHost);
        return host.equals(Policy.host(savedUrl()));
    }

    // ======================================================================
    // JavaScript dialogs
    // ======================================================================

    /**
     * Shows a page's alert(), confirm() or prompt() and closes it by itself after
     * 30 seconds (alert: OK, confirm and prompt: Cancel), so one stray dialog can't
     * leave an unattended panel stuck.
     */
    private void jsDialog(final String type, String message, String def, final JsResult result,
                          final JsPromptResult prompt) {
        String text = message == null ? "" : message;
        managed.addEvent("js_dialog", Json.obj("type", type,
                "message", text.length() > 200 ? text.substring(0, 200) : text));
        final boolean[] done = {false};
        final AlertDialog[] shown = new AlertDialog[1];
        final Runnable timeout = new Runnable() {
            public void run() {
                if (shown[0] != null) shown[0].dismiss();
            }
        };
        final EditText field = prompt != null ? new EditText(this) : null;
        if (field != null && def != null) field.setText(def);

        AlertDialog.Builder b = dialog(new Runnable() {
            public void run() {
                handler.removeCallbacks(timeout);
                if (done[0]) return;
                done[0] = true;
                if ("alert".equals(type)) result.confirm(); else result.cancel();
            }
        }).setTitle("The page says").setMessage(text);
        if (field != null) b.setView(field);
        b.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, int w) {
                if (done[0]) return;
                done[0] = true;
                if (prompt != null) prompt.confirm(field.getText().toString()); else result.confirm();
            }
        });
        if (!"alert".equals(type)) {
            b.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    if (done[0]) return;
                    done[0] = true;
                    result.cancel();
                }
            });
        }
        shown[0] = b.show();
        handler.postDelayed(timeout, JS_DIALOG_SECONDS * 1000L);
    }

    // ======================================================================
    // Camera and microphone
    // ======================================================================

    /**
     * A page asked for the camera or microphone (WebRTC doorbells, intercoms). Granted
     * when the options allow it and the page is trusted: managed, the start page's host
     * or an allowed_hosts entry; unmanaged, the start page's host or a host allowed once.
     */
    private void mediaRequested(final PermissionRequest r) {
        Options o = options();
        final String origin = String.valueOf(r.getOrigin());
        final List<String> grant = new ArrayList<String>();
        boolean media = false;
        String[] res = r.getResources();
        for (int i = 0; res != null && i < res.length; i++) {
            if (RES_VIDEO.equals(res[i])) {
                media = true;
                if (o.camera) grant.add(res[i]);
            } else if (RES_AUDIO.equals(res[i])) {
                media = true;
                if (o.microphone) grant.add(res[i]);
            }
        }
        if (grant.isEmpty()) {
            r.deny();
            if (media) managed.addEvent("media_denied", Json.obj("origin", origin, "reason", "turned_off"));
            return;
        }
        final String host = Policy.host(origin);
        boolean https = origin.regionMatches(true, 0, "https://", 0, 8);
        if (host == null || !https) {
            r.deny();
            return;
        }
        if (policy != null) {
            if (policy.trusted(origin) || host.equals(tempHost)) {
                grantMedia(r, grant);
            } else {
                r.deny();
                managed.addEvent("media_denied", Json.obj("origin", origin, "reason", "host_not_allowed"));
            }
            return;
        }
        if (host.equals(Policy.host(savedUrl())) || mediaHosts().contains("," + host + ",")) {
            grantMedia(r, grant);
            return;
        }
        // Unmanaged and not the start page: ask once, and remember a yes
        final boolean[] answered = {false};
        String what = grant.size() == 2 ? "camera and microphone" : (RES_VIDEO.equals(grant.get(0)) ? "camera" : "microphone");
        dialog(new Runnable() {
            public void run() {
                if (!answered[0]) r.deny();
            }
        })
                .setTitle("Allow the " + what + "?")
                .setMessage(host + " wants to use this tablet's " + what + ".")
                .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        answered[0] = true;
                        getSharedPreferences(PREFS, 0).edit().putString(KEY_MEDIA_HOSTS, mediaHosts() + host + ",").apply();
                        grantMedia(r, grant);
                    }
                })
                .setNegativeButton("Block", null)
                .show();
    }

    private String mediaHosts() {
        String s = getSharedPreferences(PREFS, 0).getString(KEY_MEDIA_HOSTS, ",");
        return s.startsWith(",") ? s : "," + s;
    }

    /** Grants once Android's own camera/microphone permissions are in place. */
    private void grantMedia(PermissionRequest r, List<String> grant) {
        List<String> missing = missingPermissions(grant);
        if (!missing.isEmpty() && isDeviceOwner() && Build.VERSION.SDK_INT >= 23) {
            for (String p : missing) {
                try {
                    Lockdown.dpm(this).setPermissionGrantState(admin(), getPackageName(), p, 1 /* GRANTED */);
                } catch (Exception ignored) {
                    // falls back to asking
                }
            }
            missing = missingPermissions(grant);
        }
        if (missing.isEmpty()) {
            r.grant(grant.toArray(new String[0]));
            return;
        }
        if (pendingMedia != null) pendingMedia.deny();
        pendingMedia = r;
        pendingGrant = grant.toArray(new String[0]);
        // Android's prompt is another app's window: don't treat it as leaving Kiosky
        Lockdown.allowLeave(60 * 1000L);
        Lockdown.quietUntil = System.currentTimeMillis() + 60 * 1000L;
        requestPermissions(missing.toArray(new String[0]), REQ_MEDIA);
    }

    private List<String> missingPermissions(List<String> resources) {
        List<String> missing = new ArrayList<String>();
        for (String res : resources) {
            String p = RES_VIDEO.equals(res) ? PERM_CAMERA : PERM_MIC;
            if (!hasPermission(p) && !missing.contains(p)) missing.add(p);
        }
        return missing;
    }

    private boolean hasPermission(String p) {
        return Build.VERSION.SDK_INT < 23 || checkSelfPermission(p) == 0 /* GRANTED */;
    }

    /**
     * Five quick taps in the top-left corner open the technician PIN pad (§5.3), or,
     * on an unmanaged tablet in lockdown, Kiosky's menu (after the settings PIN, if
     * set). A fallback in case an edge swipe can't reveal the system bars while locked down.
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
                    if (policy == null) showUrlBar(); // unmanaged: the menu (after the settings PIN, if set)
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
                showHelpOverlay(); // over the page, which stays loaded underneath
                dismissToolbar();
            }
        });
        settingsButton = button("Settings", new Runnable() {
            public void run() {
                showSettingsForm(null);
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
        landingHome = true;
        clearHistoryOnLoad = true;
        homeLanded = null;
        handler.removeCallbacks(loadHomeLater);
        playlistIndex = -1;
        schedulePlaylist();
        open(homeUrl());
    }

    private void open(String url) {
        if (Pages.HELP_URL.equals(url)) {
            // No address set yet: the help page is the start page
            web.loadDataWithBaseURL(Pages.HELP_URL, Pages.help(this, true), "text/html", "utf-8", Pages.HELP_URL);
        } else if (Pages.WAITING_URL.equals(url)) {
            String org = policy != null ? policy.organisation : "your organisation";
            web.loadDataWithBaseURL(Pages.WAITING_URL, Pages.waiting(org, managed.name()),
                    "text/html", "utf-8", Pages.WAITING_URL);
        } else {
            web.loadUrl(url);
        }
    }

    /**
     * Shows the help page over the kiosk page in its own WebView, so opening help
     * never navigates away from the page that was set. Close or Back returns to it.
     */
    private void showHelpOverlay() {
        if (helpBox != null) return;
        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (8 * d);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(1 /* VERTICAL */);
        box.setBackgroundColor(0xFF1C1C1E);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(0 /* HORIZONTAL */);
        bar.setGravity(16 /* CENTER_VERTICAL */);
        bar.setPadding(pad * 2, pad, pad, pad);
        TextView title = new TextView(this);
        title.setText("Kiosky help");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18);
        bar.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));
        Button close = new Button(this);
        close.setText("Close");
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closeHelp();
            }
        });
        bar.addView(close, new LinearLayout.LayoutParams(-2, -2));
        box.addView(bar, new LinearLayout.LayoutParams(-1, -2));

        WebView hv = new WebView(this);
        hv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && url.startsWith(Pages.HELP_URL + "#")) return false;
                toast("Open " + url + " on another device"); // help never leads off the tablet's page
                return true;
            }

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                closeHelp(); // shares the renderer with the page, which rebuilds itself
                return true;
            }
        });
        // WebView can't display Markdown, so it's converted to HTML first
        hv.loadDataWithBaseURL(Pages.HELP_URL, Pages.help(this, false), "text/html", "utf-8", null);
        box.addView(hv, new LinearLayout.LayoutParams(-1, 0, 1f));

        helpBox = box;
        helpView = hv;
        root.addView(box, new FrameLayout.LayoutParams(-1, -1));
    }

    private void closeHelp() {
        if (helpBox == null) return;
        LinearLayout box = helpBox;
        WebView hv = helpView;
        helpBox = null;
        helpView = null;
        root.removeView(box);
        try {
            hv.destroy();
        } catch (Exception ignored) {
            // already gone
        }
        if (web != null) web.requestFocus();
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
        show(settingsButton, !isManaged); // managed: these come from the server
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
        if (screenState == Options.OFF) return;
        if (localPinSet() && !unlocked) {
            showPinPad();
            return;
        }
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
        applyWebSettings();
        applyScreenOn();
        setRequestedOrientation(p != null ? p.orientationCode() : -1);

        if (!toolbarAllowed()) hideUrlBar();
        refreshToolbar();

        handler.removeCallbacks(periodicReload);
        if (p != null && p.reloadMinutes > 0) handler.postDelayed(periodicReload, p.reloadMinutes * 60000L);
        updateScreen();
        if (managed != null) checkBattery();
        appliedVolume = -2;
        applyVolume();
        schedulePlaylist();
    }

    /** The WebView's own settings; re-applied when the WebView is rebuilt. */
    private void applyWebSettings() {
        Policy p = policy;
        WebSettings s = web.getSettings();
        s.setTextZoom(p != null ? p.textZoom : 100);
        s.setUserAgentString(p != null && p.userAgentSuffix.length() > 0
                ? defaultUserAgent + " " + p.userAgentSuffix : defaultUserAgent);
        s.setMediaPlaybackRequiresUserGesture(p != null && !p.autoplayMedia);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, p == null || p.thirdPartyCookies);
    }

    private Options options() {
        return policy != null ? policy.options : localOptions;
    }

    private void applyScreenOn() {
        // Fully off for the night (nothing can wake it): let Android switch the screen off
        boolean asleep = screenState == Options.OFF && !options().wakeable() && !manualOff;
        boolean on = (policy == null || policy.keepScreenOn) && !asleep;
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
            Options o = options();
            if (o.hasSchedule()) Json.put(s, "screen_schedule", scheduledOff ? "off" : "on");
            Json.put(s, "screen_state", screenState == Options.OFF ? "off" : (screenState == Options.DIM ? "dim" : "normal"));
            Json.put(s, "camera_permission", hasPermission(PERM_CAMERA));
            Json.put(s, "microphone_permission", hasPermission(PERM_MIC));
            Json.put(s, "offline_screen", offline.showing());
            Json.put(s, "battery_full_hours", battery.fullHours());
            Json.put(s, "battery_protect", batteryProtect);
            Json.put(s, "screensaver", saver.showing());
            Json.put(s, "reboot_supported", canReboot());
            Json.put(s, "volume_percent", volumePercent());
            if (options().playlistUrls.length > 0) Json.put(s, "playlist_index", playlistIndex);
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
            freshStart = true; // a restart should clear a stuck page, not bring it back
            recreate();
        }

        public void onRetired(Policy last) {
            String org = last != null ? last.organisation : "your organisation";
            // Keep the screen as it is: the managed start page becomes the local one
            SharedPreferences.Editor ed = getSharedPreferences(PREFS, 0).edit();
            if (last != null && last.url.length() > 0) ed.putString(KEY_URL, last.url);
            if (last != null) ed.putBoolean(KEY_BOOT, last.startOnBoot);
            if (last != null) ed.putString(Options.PREF, last.options.toJson().toString());
            ed.putBoolean(KEY_LOCKDOWN, last != null && last.lockdown); // stays as it was
            ed.apply();
            boolean waiting = Pages.WAITING_URL.equals(web.getUrl()) || (last != null && last.url.length() == 0);
            policy = null;
            unlocked = false;
            tempHost = null;
            localOptions = last != null ? last.options : Options.local(MainActivity.this);
            applyPolicySettings();
            if (waiting) loadHome();
            showOverlay("This tablet is no longer managed by " + org, 8, false);
        }

        public void onEnrolled(Policy p, String name) {
            Provisioning.clear(MainActivity.this);
            handler.removeCallbacks(pendingEnroll);
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
            closeHelp();
            loadHome();
        } else if ("live_view".equals(type)) {
            int seconds = Json.clamp(params.optInt("seconds", 120), 0, 600);
            handler.removeCallbacks(liveFrame);
            if (seconds == 0) {
                liveUntil = 0;
            } else {
                liveUntil = SystemClock.elapsedRealtime() + seconds * 1000L;
                liveInterval = Json.clamp(params.optInt("interval", 2), 1, 10);
                liveWidth = Json.clamp(params.optInt("max_width", 960), 64, 1920);
                liveQuality = Json.clamp(params.optInt("quality", 60), 10, 100);
                handler.postDelayed(liveFrame, 500); // after this result is on its way
            }
        } else if ("diagnostics".equals(type)) {
            try {
                String bundle = diagnostics.bundle(this, shepherdHost.status(), managed.appliedVersion(),
                        Json.clamp(params.optInt("lines", 2000), 100, 10000));
                if (bundle.length() > 13 * 1024 * 1024) return failed("the diagnostics bundle is over 10 MB");
                return Json.obj("status", "done", "bundle", bundle);
            } catch (Exception e) {
                return failed("couldn't collect diagnostics: " + e);
            }
        } else if ("announce".equals(type)) {
            String text = Json.str(params, "text", "").trim();
            if (text.length() == 0) return failed("no text");
            if (speaker == null) speaker = new Speaker(this);
            if (!speaker.say(text, params.has("volume") ? Json.clamp(params.optInt("volume", -1), 0, 100) : -1)) {
                return failed("this tablet has no text-to-speech engine");
            }
        } else if ("reboot".equals(type)) {
            if (!canReboot()) return failed("rebooting needs Kiosky to be the device owner, on Android 7 or later");
            handler.postDelayed(rebootNow, 8000); // time for this result to reach the server
        } else if ("wake_screen".equals(type)) {
            int minutes = params == null ? 0 : params.optInt("minutes", 0);
            wakeScreen(minutes > 0 ? Json.clamp(minutes, 1, 240) : options().wakeMinutes);
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

    /** What's on screen: the page plus anything over it (offline screen, help, screensaver). */
    private JSONObject screenshot(int maxWidth, int quality) {
        int w = root.getWidth(), h = root.getHeight();
        if (w <= 0 || h <= 0) return failed("the page isn't on screen");
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bmp));
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
    // Playlist, live view, volume, reboots and provisioning
    // ======================================================================

    private void schedulePlaylist() {
        handler.removeCallbacks(playlistNext);
        Options o = options();
        if (o.playlistUrls.length > 0) handler.postDelayed(playlistNext, o.playlistSeconds * 1000L);
    }

    /** Next page of the playlist: the start page, then each playlist page in turn. */
    private void advancePlaylist() {
        Options o = options();
        String[] urls = o.playlistUrls;
        if (urls.length == 0 || web == null) return;
        long sinceTouch = SystemClock.elapsedRealtime() - lastTouch;
        long pause = o.playlistPauseSeconds * 1000L;
        if (pause > 0 && sinceTouch < pause) { // someone is using it: carry on later from here
            handler.removeCallbacks(playlistNext);
            handler.postDelayed(playlistNext, pause - sinceTouch + 500);
            return;
        }
        if (offline.showing() || screenState == Options.OFF || helpBox != null || onTemporaryPage) {
            schedulePlaylist();
            return;
        }
        playlistIndex = playlistIndex + 1 >= urls.length ? -1 : playlistIndex + 1;
        clearHistoryOnLoad = true;
        if (playlistIndex < 0) open(homeUrl());
        else web.loadUrl(urls[playlistIndex]);
        schedulePlaylist();
    }

    private void sendLiveFrame() {
        if (policy == null || SystemClock.elapsedRealtime() > liveUntil) return;
        handler.postDelayed(liveFrame, liveInterval * 1000L);
        if (liveSending) return; // the last frame is still on its way: skip one
        JSONObject shot = screenshot(liveWidth, liveQuality);
        String b64 = Json.str(shot, "screenshot", null);
        if (b64 == null) return;
        liveSending = true;
        shepherd.sendLiveFrame(b64, new Shepherd.FrameSent() {
            public void onSent(int code) {
                liveSending = false;
                if (code >= 400 && code < 500) { // the server wants no more (§7)
                    liveUntil = 0;
                    handler.removeCallbacks(liveFrame);
                }
            }
        });
    }

    /** audio.volume and quiet hours: only when the wanted level changes, so a hand-set level holds until then. */
    private void applyVolume() {
        int want = options().volumeNow(Calendar.getInstance());
        if (want == appliedVolume) return;
        appliedVolume = want;
        if (want >= 0) Speaker.Volume.setPercent(this, want);
    }

    private int volumePercent() {
        try {
            android.media.AudioManager am = (android.media.AudioManager) getSystemService("audio");
            int max = am.getStreamMaxVolume(3);
            return max > 0 ? Math.round(am.getStreamVolume(3) * 100f / max) : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    private boolean canReboot() {
        return isDeviceOwner() && Build.VERSION.SDK_INT >= 24;
    }

    /** reboot_at: once a day, a full reboot as device owner, otherwise a fresh restart of Kiosky. */
    private void checkReboot() {
        Options o = options();
        if (o.rebootAt < 0) return;
        Calendar now = Calendar.getInstance();
        if (now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE) != o.rebootAt) return;
        String today = now.get(Calendar.YEAR) + "-" + now.get(Calendar.DAY_OF_YEAR);
        SharedPreferences prefs = getSharedPreferences(PREFS, 0);
        if (today.equals(prefs.getString(KEY_LAST_REBOOT, null))) return;
        if (SystemClock.elapsedRealtime() < 30 * MINUTE_MS) return; // just started: that's fresh enough
        prefs.edit().putString(KEY_LAST_REBOOT, today).commit();
        if (canReboot()) {
            managed.addEvent("rebooting", Json.obj("reason", "reboot_at"));
            shepherd.checkinSoon(); // deliver the event before going down
            handler.postDelayed(rebootNow, 10000);
        } else {
            managed.addEvent("restarting", Json.obj("reason", "reboot_at"));
            freshStart = true;
            recreate();
        }
    }

    /** After QR provisioning: enroll with the server and code it carried, no questions asked. */
    private void tryPendingEnroll() {
        handler.removeCallbacks(pendingEnroll);
        JSONObject p = Provisioning.pending(this);
        if (p == null) return;
        if (managed.isManaged()) {
            Provisioning.clear(this);
            return;
        }
        if (enrollTries++ >= 60) return; // a code that never works; the next start tries again
        shepherd.enroll(Json.str(p, "server", ""), Json.str(p, "token", ""));
        handler.postDelayed(pendingEnroll, MINUTE_MS); // cancelled by onEnrolled
    }

    // ======================================================================
    // File uploads, HTTP sign-in and certificates
    // ======================================================================

    private boolean chooseFile(ValueCallback<Uri[]> cb, WebChromeClient.FileChooserParams params) {
        if (!options().fileUploads) {
            cb.onReceiveValue(null);
            toast("File uploads are turned off on this tablet");
            return true;
        }
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        fileCallback = cb;
        try {
            // The picker is another app: don't pull Kiosky back over it, but don't unlock either
            Lockdown.allowLeave(2 * MINUTE_MS);
            startActivityForResult(params.createIntent(), REQ_FILE);
        } catch (Exception e) {
            fileCallback = null;
            cb.onReceiveValue(null);
            toast("No app on this tablet can pick files");
        }
        return true;
    }

    /** A page wants a username and password (HTTP basic auth). */
    private void httpAuth(final HttpAuthHandler h, final String host, String realm) {
        long now = SystemClock.elapsedRealtime();
        boolean refused = host != null && host.equals(authHost) && now - authAt < 15000; // asked again straight away
        authHost = host;
        authAt = now;
        if (host == null) {
            h.cancel();
            return;
        }
        if (policy != null) {
            String want = policy.httpAuthHost.length() > 0 ? policy.httpAuthHost : Policy.host(policy.url);
            if (!refused && host.equalsIgnoreCase(want) && policy.httpAuthUser.length() > 0) {
                h.proceed(policy.httpAuthUser, policy.httpAuthPassword);
                return;
            }
            h.cancel();
            pageError = refused ? "HTTP sign-in refused for " + host : "HTTP sign-in needed for " + host;
            managed.addEvent("page_error", Json.obj("code", 401, "description", pageError, "url", host));
            return;
        }
        JSONObject saved = Trust.localAuth(this, host);
        if (saved != null && !refused) {
            h.proceed(Json.str(saved, "username", ""), Json.str(saved, "password", ""));
            return;
        }
        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (20 * d);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(1 /* VERTICAL */);
        form.setPadding(pad, pad / 2, pad, 0);
        final EditText user = new EditText(this);
        user.setSingleLine(true);
        user.setHint("Username");
        if (saved != null) user.setText(Json.str(saved, "username", ""));
        form.addView(user, new LinearLayout.LayoutParams(-1, -2));
        final EditText pass = new EditText(this);
        pass.setSingleLine(true);
        pass.setInputType(0x81 /* TEXT | TEXT_VARIATION_PASSWORD */);
        pass.setHint("Password");
        form.addView(pass, new LinearLayout.LayoutParams(-1, -2));
        final CheckBox remember = check(form, "Remember on this tablet", true);
        final boolean[] answered = {false};
        dialog(new Runnable() {
            public void run() {
                if (!answered[0]) h.cancel();
            }
        })
                .setTitle("Sign in to " + host)
                .setMessage((refused ? "That didn't work. " : "") + (realm != null && realm.length() > 0 ? realm : ""))
                .setView(form)
                .setPositiveButton("Sign in", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int which) {
                        answered[0] = true;
                        if (remember.isChecked()) Trust.saveLocalAuth(MainActivity.this, host, text(user), text(pass));
                        h.proceed(text(user), text(pass));
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * A certificate Android doesn't trust (self-signed, private CA, expired…). Allowed
     * only by its SHA-256 fingerprint: managed, listed in trusted_certificates and on a
     * trusted host; unmanaged, confirmed once for this host.
     */
    private void sslError(final SslErrorHandler h, SslError e) {
        final String url = e == null ? null : e.getUrl();
        final String host = Policy.host(url);
        final String fp = e == null ? null : Trust.fingerprint(e.getCertificate());
        if (host == null || fp == null) {
            h.cancel();
            return;
        }
        if (policy != null) {
            if (policy.trustsCertificate(fp) && (policy.trusted(url) || host.equals(tempHost))) {
                h.proceed();
                return;
            }
            h.cancel();
            untrustedCertificate(url, host, fp, e.getPrimaryError());
            return;
        }
        if (Trust.localCert(this, host, fp)) {
            h.proceed();
            return;
        }
        if (certDialogOpen) { // one question at a time; the rest of the page's requests wait for a reload
            h.cancel();
            return;
        }
        certDialogOpen = true;
        final boolean[] answered = {false};
        dialog(new Runnable() {
            public void run() {
                certDialogOpen = false;
                if (!answered[0]) {
                    h.cancel();
                    untrustedCertificate(url, host, fp, -1);
                }
            }
        })
                .setTitle("Trust " + host + "?")
                .setMessage(host + "'s certificate isn't trusted by Android. That's normal for a server on your own "
                        + "network with a self-signed certificate (Home Assistant, a NAS…), and a warning sign anywhere else.\n\n"
                        + "SHA-256 fingerprint:\n" + Trust.pretty(fp) + "\n\nOnly trust it if this is your own server.")
                .setPositiveButton("Trust", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int which) {
                        answered[0] = true;
                        Trust.addLocalCert(MainActivity.this, host, fp);
                        h.proceed();
                    }
                })
                .setNegativeButton("Don't trust", null)
                .show();
    }

    private void untrustedCertificate(String url, String host, String fp, int error) {
        // Sub-resources (an image from another site) don't take over the screen; the page's own site does
        boolean page = host.equals(Policy.host(homeUrl())) || host.equals(Policy.host(web.getUrl()));
        if (!page) {
            managed.addEvent("certificate_untrusted", Json.obj("url", url, "host", host,
                    "fingerprint", Trust.pretty(fp), "error", error, "subresource", true));
            return;
        }
        errorThisLoad = true;
        errorUrl = url;
        errorAt = SystemClock.elapsedRealtime();
        pageError = "untrusted certificate for " + host;
        String key = "cert " + host + fp;
        if (!key.equals(lastErrorKey)) {
            lastErrorKey = key;
            // The fingerprint goes to the server so an admin can copy it into trusted_certificates
            managed.addEvent("certificate_untrusted", Json.obj("url", url, "host", host,
                    "fingerprint", Trust.pretty(fp), "error", error));
        }
        offline.showText(host + "'s certificate isn't trusted.\nSHA-256 " + Trust.pretty(fp)
                + (policy != null ? "\nAdd it to Trusted certificates in Kiosky Shepherd." : ""));
    }

    // ======================================================================
    // Screen schedule, dimming and waking (policy "screen", or local Settings)
    // ======================================================================

    /**
     * Works out what the screen should be doing now and applies it. Off: a black
     * overlay at zero brightness, kept on so a touch or motion can wake it; or, when
     * nothing may wake it, actually off (Android's timeout, or lockNow as device owner)
     * with an alarm to switch it back on at {@code on_at}.
     */
    private void updateScreen() {
        if (root == null || motion == null) return;
        Options o = options();
        long now = SystemClock.elapsedRealtime();
        int sched = o.scheduled(Calendar.getInstance());
        boolean off = sched == Options.OFF;
        if (scheduledOff && !off) { // the off hours just ended
            wakeUntil = 0;
            lastTouch = lastActivity = now; // don't dim or go home straight away
            managed.addEvent("screen_schedule", Json.obj("state", "on"));
            if (web != null && !offline.showing()) web.reload(); // fresh after a night's sleep
        } else if (!scheduledOff && off) {
            managed.addEvent("screen_schedule", Json.obj("state", "off"));
        }
        scheduledOff = off;

        boolean idle = o.idleDimMinutes > 0 && now - lastActivity >= o.idleDimMinutes * MINUTE_MS;
        idleDimmed = idle && sched == Options.NORMAL;
        int state = manualOff || (off && now >= wakeUntil) ? Options.OFF
                : (sched == Options.DIM || idleDimmed ? Options.DIM : Options.NORMAL);
        boolean changed = state != screenState;
        screenState = state;

        setBrightness(state == Options.OFF ? 0f : (state == Options.DIM ? o.dimBrightness / 100f : pageBrightness));
        if (state == Options.OFF && blackout == null) {
            blackout = new View(this);
            blackout.setBackgroundColor(0xFF000000);
            root.addView(blackout, new FrameLayout.LayoutParams(-1, -1));
        } else if (state != Options.OFF && blackout != null) {
            root.removeView(blackout);
            blackout = null;
        }
        applyScreenOn();

        boolean saverWanted = o.screensaverMinutes > 0 && state != Options.OFF && helpBox == null
                && !offline.showing() && now - lastActivity >= o.screensaverMinutes * MINUTE_MS;
        if (saverWanted && !saver.showing()) {
            saver.show(o.screensaverUrl);
            managed.addEvent("screensaver", Json.obj("state", "on"));
        } else if (!saverWanted && saver.showing()) {
            saver.hide();
        }

        if (o.wakeOnMotion && resumed && (o.hasSchedule() || o.idleDimMinutes > 0 || o.screensaverMinutes > 0)) motion.start();
        else motion.stop();

        if (!changed) return;
        if (state == Options.OFF) {
            dismissToolbar();
            if (!o.wakeable() && !manualOff) {
                getWindow().addFlags(WAKE_FLAGS); // so the alarm can bring it back over the lock screen
                handler.removeCallbacks(clearWakeFlags);
                ScheduleReceiver.set(this, o.nextOnAt(Calendar.getInstance()));
                if (isDeviceOwner()) {
                    try {
                        Lockdown.dpm(this).lockNow();
                    } catch (Exception ignored) {
                        // Android's screen timeout switches it off instead
                    }
                }
            }
        } else {
            ScheduleReceiver.cancel(this);
            handler.removeCallbacks(clearWakeFlags);
            handler.postDelayed(clearWakeFlags, MINUTE_MS);
        }
    }

    /** Once a minute: battery temperature events and the charging limit. */
    private void checkBattery() {
        Intent b = Battery.read(this);
        String change = battery.check(b);
        if (change != null) {
            managed.addEvent(change.equals("hot") ? "battery_hot" : "battery_cool",
                    Json.obj("temperature", Battery.temperature(b), "level", Battery.level(b),
                            "charging", Battery.charging(b)));
        }
        String was = batteryProtect;
        batteryProtect = Battery.applyProtect(this, options().batteryProtect);
        if (!batteryProtect.equals(was) && !"off".equals(batteryProtect)) {
            managed.addEvent("battery_protect", Json.obj("state", batteryProtect));
        }
    }

    private void setBrightness(float value) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        if (lp == null || lp.screenBrightness == value) return;
        lp.screenBrightness = value; // -1: the tablet's own brightness
        getWindow().setAttributes(lp);
    }

    /** Someone is there (motion, a page, a server command): wake for a while if it's off. */
    private void wakeScreen(int minutes) {
        long now = SystemClock.elapsedRealtime();
        lastActivity = now;
        manualOff = false;
        if (scheduledOff) wakeUntil = now + minutes * MINUTE_MS;
        updateScreen();
    }

    // ======================================================================
    // Going home when idle, and the watchdog
    // ======================================================================

    /** After idle_home_minutes without a touch, back to the start page (once per idle spell). */
    private void checkIdleHome() {
        Options o = options();
        if (o.idleHomeMinutes <= 0 || web == null) return;
        long now = SystemClock.elapsedRealtime();
        if (now - lastTouch < o.idleHomeMinutes * MINUTE_MS) return;
        lastTouch = now;
        closeHelp();
        if (offline.showing() || atHome()) return;
        managed.addEvent("idle_home", Json.obj("url", web.getUrl()));
        loadHome();
    }

    private boolean atHome() {
        if (onTemporaryPage) return false;
        String url = web.getUrl();
        if (sameUrl(url, homeUrl()) || sameUrl(url, homeLanded)) return true;
        for (String u : options().playlistUrls) if (sameUrl(url, u)) return true;
        return false;
    }

    private static boolean sameUrl(String a, String b) {
        return a != null && b != null && trimUrl(a).equals(trimUrl(b));
    }

    private static String trimUrl(String u) {
        int hash = u.indexOf('#');
        if (hash >= 0) u = u.substring(0, hash);
        while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        return u;
    }

    /**
     * Asks the page for a trivial bit of JavaScript. A page that doesn't answer within
     * watchdog_minutes is frozen: reload it, and if that doesn't help, rebuild the WebView.
     */
    private void watchdog() {
        Options o = options();
        if (o.watchdogMinutes <= 0 || web == null || !resumed) {
            heartbeatPending = false;
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (heartbeatPending) {
            if (now - heartbeatSentAt < o.watchdogMinutes * MINUTE_MS) return;
            heartbeatPending = false;
            heartbeatGen++;
            if (!watchdogReloaded) {
                watchdogReloaded = true;
                managed.addEvent("watchdog_reload", Json.obj("url", web.getUrl()));
                web.reload();
            } else {
                managed.addEvent("watchdog_rebuild", Json.obj("url", lastLoadedUrl));
                rebuildWebView(0);
            }
            return;
        }
        heartbeatPending = true;
        heartbeatSentAt = now;
        final int gen = ++heartbeatGen;
        try {
            web.evaluateJavascript("1", new ValueCallback<String>() {
                public void onReceiveValue(String value) {
                    if (gen != heartbeatGen) return;
                    heartbeatPending = false;
                    watchdogReloaded = false;
                }
            });
        } catch (Exception e) {
            heartbeatPending = false;
        }
    }

    // ======================================================================
    // Local settings (unmanaged tablets)
    // ======================================================================

    /** {@code draft}: values to show again after a mistake, or null for the saved ones. */
    private void showSettingsForm(JSONObject draft) {
        JSONObject cur = draft != null ? draft : localOptions.toJson();
        JSONObject scr = cur.optJSONObject("screen");
        JSONObject media = cur.optJSONObject("media");
        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (20 * d);

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(1 /* VERTICAL */);
        form.setPadding(pad, pad / 2, pad, pad / 2);

        heading(form, "Screen off at night");
        final EditText offAt = field(form, "Off at (HH:MM, empty: never)", Json.str(scr, "off_at", ""), false);
        final EditText onAt = field(form, "On again at (HH:MM)", Json.str(scr, "on_at", ""), false);
        final CheckBox wakeTouch = check(form, "A touch wakes it", scr == null || scr.optBoolean("wake_on_touch", true));
        boolean sensors = motion.available();
        final CheckBox wakeMotion = check(form, sensors ? "Someone close by wakes it (proximity or light sensor)"
                : "Someone close by wakes it (this tablet has no suitable sensor)",
                scr != null && scr.optBoolean("wake_on_motion", false));
        wakeMotion.setEnabled(sensors);
        final EditText wakeMin = field(form, "Stays awake for (minutes)", Json.str(scr, "wake_minutes", "5"), true);

        heading(form, "Dimming");
        final EditText dimAt = field(form, "Dim from (HH:MM, empty: never)", Json.str(scr, "dim_at", ""), false);
        final EditText dimUntil = field(form, "Until (HH:MM)", Json.str(scr, "dim_until", ""), false);
        final EditText idleDim = field(form, "Also dim after this many minutes untouched (0: never)",
                Json.str(scr, "idle_dim_minutes", "0"), true);
        final EditText dimPct = field(form, "Dimmed brightness (%)", Json.str(scr, "dim_brightness", "15"), true);

        heading(form, "Page");
        final EditText idleHome = field(form, "Back to the start page after this many minutes untouched (0: never)",
                Json.str(cur, "idle_home_minutes", "0"), true);
        final EditText watchdogMin = field(form, "Reload a frozen page after this many minutes (0: never)",
                Json.str(cur, "watchdog_minutes", "2"), true);
        final CheckBox camera = check(form, "Pages may use the camera", media == null || media.optBoolean("camera", true));
        final CheckBox mic = check(form, "Pages may use the microphone", media == null || media.optBoolean("microphone", true));

        heading(form, "Playlist");
        JSONObject pl = cur.optJSONObject("playlist");
        StringBuilder plText = new StringBuilder();
        org.json.JSONArray plUrls = pl == null ? null : pl.optJSONArray("urls");
        for (int i = 0; plUrls != null && i < plUrls.length(); i++) plText.append(plUrls.optString(i, "")).append('\n');
        final EditText playlist = field(form, "Pages to rotate through after the start page (https://, one per line)",
                plText.toString().trim(), false);
        playlist.setSingleLine(false);
        playlist.setMinLines(2);
        playlist.setInputType(0x1 | 0x10 | 0x20000 /* TEXT | URI | MULTI_LINE */);
        final EditText plSeconds = field(form, "Show each page for (seconds)", Json.str(pl, "seconds", "60"), true);
        final EditText plPause = field(form, "After a touch, pause the rotation for (seconds, 0: never)",
                Json.str(pl, "pause_on_touch_seconds", "120"), true);

        heading(form, "Screensaver");
        JSONObject ss = cur.optJSONObject("screensaver");
        final EditText ssMin = field(form, "Show a clock after this many minutes untouched (0: never)",
                Json.str(ss, "minutes", "0"), true);
        final EditText ssUrl = field(form, "Or show this page instead of the clock (https://, optional)", Json.str(ss, "url", ""), false);
        ssUrl.setInputType(0x1 | 0x10 /* TEXT | URI */);

        heading(form, "Sound");
        JSONObject au = cur.optJSONObject("audio");
        final EditText vol = field(form, "Volume (%, empty: leave it as it is)",
                au == null || au.optInt("volume", -1) < 0 ? "" : Json.str(au, "volume", ""), true);
        final EditText quietAt = field(form, "Quiet from (HH:MM, empty: never)", Json.str(au, "quiet_at", ""), false);
        final EditText quietUntil = field(form, "Until (HH:MM)", Json.str(au, "quiet_until", ""), false);
        final EditText quietVol = field(form, "Volume during quiet hours (%)", Json.str(au, "quiet_volume", "0"), true);

        heading(form, "Tablet");
        final EditText rebootAt = field(form, "Restart every day at (HH:MM, empty: never; a full reboot when device owner)",
                Json.str(cur, "reboot_at", ""), false);
        final CheckBox uploads = check(form, "Pages may upload files", cur.optBoolean("file_uploads", true));
        final CheckBox forget = check(form, "Forget saved sign-ins and trusted certificates", false);

        heading(form, "Battery");
        JSONObject bat = cur.optJSONObject("battery");
        final CheckBox protect = check(form, "Limit charging to protect the battery (Samsung, experimental; see Help)",
                bat != null && bat.optBoolean("protect", false));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form, new FrameLayout.LayoutParams(-1, -2));

        dialog()
                .setTitle("Settings")
                .setView(scroll)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int which) {
                        JSONObject o = Json.obj(
                                "idle_home_minutes", number(idleHome, 0),
                                "watchdog_minutes", number(watchdogMin, 2),
                                "media", Json.obj("camera", camera.isChecked(), "microphone", mic.isChecked()),
                                "screen", Json.obj(
                                        "off_at", text(offAt), "on_at", text(onAt),
                                        "wake_on_touch", wakeTouch.isChecked(),
                                        "wake_on_motion", wakeMotion.isChecked(),
                                        "wake_minutes", number(wakeMin, 5),
                                        "dim_at", text(dimAt), "dim_until", text(dimUntil),
                                        "dim_brightness", number(dimPct, 15),
                                        "idle_dim_minutes", number(idleDim, 0)),
                                "battery", Json.obj("protect", protect.isChecked()),
                                "playlist", Json.obj("urls", lines(playlist), "seconds", number(plSeconds, 60),
                                        "pause_on_touch_seconds", number(plPause, 120)),
                                "screensaver", Json.obj("minutes", number(ssMin, 0), "url", text(ssUrl)),
                                "audio", Json.obj("volume", text(vol).length() == 0 ? -1 : number(vol, -1),
                                        "quiet_at", text(quietAt), "quiet_until", text(quietUntil),
                                        "quiet_volume", number(quietVol, 0)),
                                "file_uploads", uploads.isChecked(),
                                "reboot_at", text(rebootAt));
                        String problem = timesProblem(text(offAt), text(onAt), "off")
                                + timesProblem(text(dimAt), text(dimUntil), "dimming")
                                + timesProblem(text(quietAt), text(quietUntil), "quiet hours");
                        if (text(rebootAt).length() > 0 && Options.time(text(rebootAt)) < 0) {
                            problem += "\"" + text(rebootAt) + "\" isn't a time. Use HH:MM, like 04:00. ";
                        }
                        org.json.JSONArray urls = lines(playlist);
                        for (int i = 0; i < urls.length(); i++) {
                            if (!urls.optString(i, "").startsWith("https://")) {
                                problem += "Playlist pages must start with https://. ";
                                break;
                            }
                        }
                        if (text(ssUrl).length() > 0 && !text(ssUrl).startsWith("https://")) {
                            problem += "The screensaver page must start with https://. ";
                        }
                        if (problem.length() > 0) {
                            toast(problem.trim());
                            showSettingsForm(o);
                            return;
                        }
                        Options.saveLocal(MainActivity.this, o);
                        if (forget.isChecked()) Trust.forgetLocal(MainActivity.this);
                        localOptions = Options.local(MainActivity.this);
                        appliedVolume = -2;
                        applyVolume();
                        schedulePlaylist();
                        lastTouch = lastActivity = SystemClock.elapsedRealtime();
                        updateScreen();
                        checkBattery();
                        if ("no_permission".equals(batteryProtect)) {
                            toast("Settings saved. Limiting charging needs a one-time adb command (see Help)");
                        } else if ("unsupported".equals(batteryProtect)) {
                            toast("Settings saved. This tablet has no charging limit Kiosky can use");
                        } else {
                            toast("Settings saved");
                        }
                    }
                })
                .setNeutralButton(localPin() == null ? "Set a PIN…" : "PIN…", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int which) {
                        pinTapped();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Empty, or a sentence about what's wrong with a pair of times. */
    private static String timesProblem(String from, String to, String what) {
        if (from.length() == 0 && to.length() == 0) return "";
        if (from.length() == 0 || to.length() == 0) return "Set both times for " + what + ", or neither. ";
        if (Options.time(from) < 0) return "\"" + from + "\" isn't a time. Use HH:MM, like 22:30. ";
        if (Options.time(to) < 0) return "\"" + to + "\" isn't a time. Use HH:MM, like 07:00. ";
        if (Options.time(from) == Options.time(to)) return "The " + what + " times can't be the same. ";
        return "";
    }

    private void heading(LinearLayout form, String label) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(18);
        t.setPadding(0, (int) (16 * getResources().getDisplayMetrics().density), 0, 0);
        form.addView(t, new LinearLayout.LayoutParams(-1, -2));
    }

    private EditText field(LinearLayout form, String label, String value, boolean numeric) {
        TextView t = new TextView(this);
        t.setText(label);
        form.addView(t, new LinearLayout.LayoutParams(-1, -2));
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setInputType(numeric ? 0x2 /* TYPE_CLASS_NUMBER */ : 0x4 | 0x20 /* TYPE_CLASS_DATETIME | TIME */);
        e.setText(value);
        form.addView(e, new LinearLayout.LayoutParams(-1, -2));
        return e;
    }

    private CheckBox check(LinearLayout form, String label, boolean on) {
        CheckBox c = new CheckBox(this);
        c.setText(label);
        c.setChecked(on);
        form.addView(c, new LinearLayout.LayoutParams(-1, -2));
        return c;
    }

    private static String text(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private static org.json.JSONArray lines(EditText e) {
        org.json.JSONArray a = new org.json.JSONArray();
        for (String l : text(e).split("[\\s]+")) if (l.length() > 0) a.put(l);
        return a;
    }

    private static int number(EditText e, int def) {
        try {
            return Integer.parseInt(text(e));
        } catch (NumberFormatException x) {
            return def;
        }
    }

    // ---- Settings PIN ------------------------------------------------------

    private JSONObject localPin() {
        return Json.parse(getSharedPreferences(PREFS, 0).getString(KEY_PIN, null));
    }

    /** An unmanaged tablet whose toolbar asks for a PIN. */
    private boolean localPinSet() {
        return policy == null && localPin() != null;
    }

    private void pinTapped() {
        if (localPin() == null) {
            askNewPin();
            return;
        }
        dialog()
                .setTitle("Settings PIN")
                .setMessage("Kiosky's toolbar asks for this PIN before it opens.")
                .setPositiveButton("Change PIN", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        askNewPin();
                    }
                })
                .setNeutralButton("Remove PIN", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        getSharedPreferences(PREFS, 0).edit().remove(KEY_PIN).apply();
                        refreshToolbar();
                        toast("The toolbar opens without a PIN again");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void askNewPin() {
        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (20 * d);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(1 /* VERTICAL */);
        form.setPadding(pad, pad / 2, pad, 0);
        final EditText first = new EditText(this);
        first.setInputType(0x12 /* NUMBER | NUMBER_VARIATION_PASSWORD */);
        first.setHint("New PIN (4 to 12 digits)");
        form.addView(first, new LinearLayout.LayoutParams(-1, -2));
        final EditText second = new EditText(this);
        second.setInputType(0x12);
        second.setHint("Same PIN again");
        form.addView(second, new LinearLayout.LayoutParams(-1, -2));

        dialog()
                .setTitle("Set a settings PIN")
                .setMessage("Opening Kiosky's toolbar will need this PIN. If it's forgotten, the only way back "
                        + "is clearing Kiosky's storage in Android's settings, which also forgets the address and sign-ins.")
                .setView(form)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int which) {
                        final String pin = text(first);
                        if (!pin.matches("[0-9]{4,12}")) {
                            toast("Use 4 to 12 digits");
                            return;
                        }
                        if (!pin.equals(text(second))) {
                            toast("The two PINs don't match");
                            return;
                        }
                        toast("Saving the PIN…");
                        new Thread(new Runnable() {
                            public void run() {
                                final JSONObject spec = Pin.create(pin); // slow by design
                                handler.post(new Runnable() {
                                    public void run() {
                                        if (spec == null) {
                                            toast("Couldn't save the PIN");
                                            return;
                                        }
                                        getSharedPreferences(PREFS, 0).edit().putString(KEY_PIN, spec.toString()).apply();
                                        refreshToolbar();
                                        toast("PIN set. Kiosky's toolbar will ask for it");
                                    }
                                });
                            }
                        }).start();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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
                showHelpOverlay();
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
        return dialog(null);
    }

    /** {@code onDismiss} runs however the dialog closes (a button, Back, or a timeout). */
    private AlertDialog.Builder dialog(final Runnable onDismiss) {
        ownDialogs++;
        return new AlertDialog.Builder(this).setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface d) {
                if (ownDialogs > 0) ownDialogs--;
                if (onDismiss != null) onDismiss.run();
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

    /** Managed: the technician PIN (§5.3). Unmanaged: the local settings PIN. */
    private void showPinPad() {
        final JSONObject spec = policy != null ? policy.pin : localPin();
        if (spec == null || pinPadOpen) return;
        long wait = pinLockedUntil - System.currentTimeMillis();
        if (wait > 0) {
            toast("Too many wrong PINs. Try again in " + ((wait + 59999) / 60000) + " min");
            return;
        }
        pinPadOpen = true;
        final EditText field = new EditText(this);
        field.setInputType(0x12 /* TYPE_CLASS_NUMBER | TYPE_NUMBER_VARIATION_PASSWORD */);
        field.setHint("PIN");
        dialog(new Runnable() {
            public void run() {
                pinPadOpen = false;
            }
        })
                .setTitle(policy != null ? "Technician PIN" : "Settings PIN")
                .setView(field)
                .setPositiveButton("Unlock", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int which) {
                        checkPin(field.getText().toString(), spec);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int which) {
                        afterUnlock = null;
                    }
                })
                .show();
    }

    private void checkPin(final String entered, final JSONObject spec) {
        new Thread(new Runnable() {
            public void run() {
                final boolean ok = Pin.verify(entered, spec); // slow by design
                handler.post(new Runnable() {
                    public void run() {
                        if (ok) {
                            pinFails = 0;
                            unlocked = true;
                            Runnable then = afterUnlock;
                            afterUnlock = null;
                            if (then != null) {
                                unlocked = false; // only for this action, not the toolbar
                                then.run();
                            } else {
                                showUrlBar();
                            }
                            if (policy != null && shepherd.lastError != null) toast(shepherd.lastError);
                        } else if (++pinFails >= MAX_PIN_TRIES) {
                            pinFails = 0;
                            afterUnlock = null;
                            pinLockedUntil = System.currentTimeMillis() + PIN_LOCKOUT_MS;
                            managed.addEvent("pin_failed", Json.obj("attempts", MAX_PIN_TRIES));
                            toast("Too many wrong PINs. PIN entry is locked for 5 minutes");
                        } else {
                            afterUnlock = null;
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

    private void handleEnrollUri(final Uri u) {
        if (policy != null) {
            // Never re-enroll a managed tablet: that would be a way out of lockdown
            toast("This tablet is already managed by " + policy.organisation);
            return;
        }
        if (localPinSet() && !unlocked) { // handing the tablet over needs the settings PIN
            afterUnlock = new Runnable() {
                public void run() {
                    confirmEnroll(u.getQueryParameter("server"), u.getQueryParameter("token"));
                }
            };
            showPinPad();
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
