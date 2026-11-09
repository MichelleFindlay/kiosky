package kiosky;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.InputStream;
import java.text.DateFormat;
import java.util.Date;

/**
 * Covers the page while it can't be reached, instead of Chromium's error page or a
 * server's 502 page: the logo, the time, "Reconnecting…" and what went wrong. It's a
 * native view above the WebView, so the retries underneath don't add history entries
 * and the toolbar and corner taps still work.
 */
final class OfflineScreen {

    final LinearLayout view;
    private final TextView clock, detail;

    OfflineScreen(Context c, final Runnable tryNow) {
        float d = c.getResources().getDisplayMetrics().density;
        int pad = (int) (24 * d);

        view = new LinearLayout(c);
        view.setOrientation(1 /* VERTICAL */);
        view.setGravity(17 /* CENTER */);
        view.setBackgroundColor(0xFF1C1C1E);
        view.setPadding(pad, pad, pad, pad);
        view.setVisibility(8 /* GONE */);
        view.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // swallow taps so they don't reach the error page underneath
            }
        });

        try {
            InputStream in = c.getAssets().open("logo.png");
            try {
                ImageView logo = new ImageView(c);
                logo.setImageBitmap(BitmapFactory.decodeStream(in));
                int size = (int) (96 * d);
                view.addView(logo, new LinearLayout.LayoutParams(size, size));
            } finally {
                in.close();
            }
        } catch (Exception ignored) {
            // no logo, no problem
        }

        clock = text(c, 64, 0xFFFFFFFF);
        TextView title = text(c, 28, 0xFFFFFFFF);
        title.setText("Reconnecting…");
        detail = text(c, 18, 0xFFA1A1A6);
        detail.setPadding(0, pad / 2, 0, pad);

        Button retry = new Button(c);
        retry.setText("Try now");
        retry.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tryNow.run();
            }
        });
        view.addView(retry, new LinearLayout.LayoutParams(-2, -2));
    }

    private TextView text(Context c, float size, int color) {
        TextView t = new TextView(c);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(17 /* CENTER */);
        view.addView(t, new LinearLayout.LayoutParams(-1, -2));
        return t;
    }

    void show(String host, String problem, int retrySeconds) {
        showText((host != null ? "Can't reach " + host : "The page can't be reached")
                + (problem != null ? " (" + problem + ")" : "")
                + ".\nTrying again every " + retrySeconds + " s.");
    }

    void showText(String text) {
        detail.setText(text);
        tick();
        view.setVisibility(0 /* VISIBLE */);
    }

    void hide() {
        view.setVisibility(8 /* GONE */);
    }

    boolean showing() {
        return view.getVisibility() == 0;
    }

    /** Keeps the clock current while showing. */
    void tick() {
        clock.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date()));
    }
}
