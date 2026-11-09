package kiosky;

import android.content.Context;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;
import java.util.Random;

/**
 * Shown after screensaver.minutes without a touch: a large clock and date that drift
 * around the screen (so nothing burns in), or a page of its own (a photo frame, say).
 * The page view keeps the kiosk page loaded underneath; a touch closes it.
 */
final class ScreenSaver {

    private final Context context;
    private final FrameLayout root;
    private final Random random = new Random();
    private FrameLayout view;
    private LinearLayout clockBox;
    private TextView clock, date;
    private WebView page;
    private boolean placed; // the clock has a spot (the view's size is known)

    ScreenSaver(Context c, FrameLayout root) {
        this.context = c;
        this.root = root;
    }

    boolean showing() {
        return view != null;
    }

    /** {@code url}: an https page to show instead of the clock, or "". */
    void show(String url) {
        if (view != null) return;
        view = new FrameLayout(context);
        view.setBackgroundColor(0xFF000000);
        if (url != null && url.startsWith("https://")) {
            final String host = Policy.host(url);
            page = new WebView(context);
            page.getSettings().setJavaScriptEnabled(true);
            page.getSettings().setDomStorageEnabled(true);
            page.getSettings().setMediaPlaybackRequiresUserGesture(false);
            page.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView v, String u) {
                    return host == null || !host.equals(Policy.host(u)); // stays on its own site
                }

                @Override
                public boolean onRenderProcessGone(WebView v, RenderProcessGoneDetail d) {
                    dropPage(); // shares the renderer with the kiosk page; fall back to the clock
                    return true;
                }
            });
            page.loadUrl(url);
            view.addView(page, new FrameLayout.LayoutParams(-1, -1));
        } else {
            addClock();
        }
        root.addView(view, new FrameLayout.LayoutParams(-1, -1));
        tick();
    }

    private void addClock() {
        clockBox = new LinearLayout(context);
        clockBox.setOrientation(1 /* VERTICAL */);
        clock = new TextView(context);
        clock.setTextSize(96);
        clock.setTextColor(0xFFB0B0B0); // not full white: less glare and wear
        clockBox.addView(clock, new LinearLayout.LayoutParams(-2, -2));
        date = new TextView(context);
        date.setTextSize(28);
        date.setTextColor(0xFF808080);
        clockBox.addView(date, new LinearLayout.LayoutParams(-2, -2));
        view.addView(clockBox, new FrameLayout.LayoutParams(-2, -2));
    }

    private void dropPage() {
        if (page == null) return;
        try {
            view.removeView(page);
            page.destroy();
        } catch (Exception ignored) {
            // already gone
        }
        page = null;
        addClock();
        tick();
    }

    /** Every 15 s: the time, and every few minutes a new spot for the clock. */
    void tick() {
        if (clock == null) return;
        Date now = new Date();
        clock.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(now));
        date.setText(DateFormat.getDateInstance(DateFormat.FULL).format(now));
        if (!placed || random.nextInt(8) == 0) move();
    }

    private void move() {
        int w = Math.max(0, view.getWidth() - (int) (420 * density()));
        int h = Math.max(0, view.getHeight() - (int) (200 * density()));
        clockBox.setPadding(w > 0 ? random.nextInt(w) : 0, h > 0 ? random.nextInt(h) : 0, 0, 0);
        placed = view.getWidth() > 0;
    }

    private float density() {
        return context.getResources().getDisplayMetrics().density;
    }

    void hide() {
        if (view == null) return;
        if (page != null) {
            try {
                page.destroy();
            } catch (Exception ignored) {
                // already gone
            }
        }
        root.removeView(view);
        view = null;
        page = null;
        clock = null;
        date = null;
        clockBox = null;
        placed = false;
    }
}
