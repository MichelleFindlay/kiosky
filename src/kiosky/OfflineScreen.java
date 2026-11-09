package kiosky;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;

/**
 * Covers the page while it can't be reached, instead of Chromium's error page or a
 * server's 502 page. It's a native view above the WebView, so the retries underneath
 * don't add history entries and the toolbar and corner taps still work.
 *
 * Two looks: with a picture of the last page that loaded properly ({@link #show} with
 * a snapshot), that picture greyed out under a slim "Reconnecting…" banner, so a
 * dashboard that's briefly offline still looks like itself; otherwise the logo, the
 * time, "Reconnecting…" and what went wrong, full screen.
 */
final class OfflineScreen {

    final FrameLayout view;
    private final ImageView shot;
    private final LinearLayout card;
    private final View logo;
    private final TextView clock, title, detail;
    private final int pad;

    OfflineScreen(Context c, final Runnable tryNow) {
        float d = c.getResources().getDisplayMetrics().density;
        pad = (int) (24 * d);

        view = new FrameLayout(c);
        view.setBackgroundColor(0xFF1C1C1E);
        view.setVisibility(8 /* GONE */);
        view.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // swallow taps so they don't reach the error page underneath
            }
        });

        shot = new ImageView(c);
        shot.setScaleType(ImageView.ScaleType.FIT_XY); // taken at this screen's size
        ColorMatrix grey = new ColorMatrix();
        grey.setSaturation(0f);
        shot.setColorFilter(new ColorMatrixColorFilter(grey));
        shot.setAlpha(0.45f); // over the dark background: greyed out and dimmed
        shot.setVisibility(8);
        view.addView(shot, new FrameLayout.LayoutParams(-1, -1));

        card = new LinearLayout(c);
        card.setOrientation(1 /* VERTICAL */);
        card.setGravity(17 /* CENTER */);
        card.setPadding(pad, pad, pad, pad);
        view.addView(card, new FrameLayout.LayoutParams(-1, -1));

        View logoView = null;
        try {
            InputStream in = c.getAssets().open("logo.png");
            try {
                ImageView img = new ImageView(c);
                img.setImageBitmap(BitmapFactory.decodeStream(in));
                int size = (int) (96 * d);
                card.addView(img, new LinearLayout.LayoutParams(size, size));
                logoView = img;
            } finally {
                in.close();
            }
        } catch (Exception ignored) {
            // no logo, no problem
        }
        logo = logoView;

        clock = text(c, 64, 0xFFFFFFFF);
        title = text(c, 28, 0xFFFFFFFF);
        title.setText("Reconnecting…");
        detail = text(c, 18, 0xFFA1A1A6);

        Button retry = new Button(c);
        retry.setText("Try now");
        retry.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tryNow.run();
            }
        });
        card.addView(retry, new LinearLayout.LayoutParams(-2, -2));
    }

    private TextView text(Context c, float size, int color) {
        TextView t = new TextView(c);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(17 /* CENTER */);
        card.addView(t, new LinearLayout.LayoutParams(-1, -2));
        return t;
    }

    /** {@code snapshot}: the last good page to show greyed out, or null for the full screen. */
    void show(String host, String problem, int retrySeconds, Bitmap snapshot) {
        String what = (host != null ? "Can't reach " + host : "The page can't be reached")
                + (problem != null ? " (" + problem + ")" : "");
        if (snapshot != null) {
            showText(what + ". Trying again every " + retrySeconds + " s.", snapshot);
        } else {
            showText(what + ".\nTrying again every " + retrySeconds + " s.", null);
        }
    }

    void showText(String text) {
        showText(text, null);
    }

    private void showText(String text, Bitmap snapshot) {
        boolean banner = snapshot != null;
        if (banner) shot.setImageBitmap(snapshot);
        shot.setVisibility(banner ? 0 : 8);
        // Banner: one compact strip along the top, so the page underneath stays recognisable
        card.setBackgroundColor(banner ? 0xE61C1C1E : 0x00000000);
        card.setPadding(pad, banner ? pad / 3 : pad, pad, banner ? pad / 3 : pad);
        card.setLayoutParams(new FrameLayout.LayoutParams(-1, banner ? -2 : -1, 48 /* TOP */));
        if (logo != null) logo.setVisibility(banner ? 8 : 0);
        clock.setVisibility(banner ? 8 : 0);
        title.setTextSize(banner ? 20 : 28);
        title.setText(banner ? "Reconnecting… " + time() : "Reconnecting…");
        detail.setTextSize(banner ? 14 : 18);
        detail.setPadding(0, banner ? 0 : pad / 2, 0, banner ? pad / 4 : pad);
        detail.setText(text);
        bannerMode = banner;
        tick();
        view.setVisibility(0 /* VISIBLE */);
    }

    private boolean bannerMode;

    void hide() {
        view.setVisibility(8 /* GONE */);
        shot.setImageBitmap(null);
    }

    boolean showing() {
        return view.getVisibility() == 0;
    }

    /** Keeps the clock current while showing. */
    void tick() {
        if (bannerMode) title.setText("Reconnecting… " + time());
        else clock.setText(time());
    }

    private static String time() {
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date());
    }
}
