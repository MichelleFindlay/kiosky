package kiosky;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.speech.tts.TextToSpeech;

import java.util.ArrayList;
import java.util.List;

/** Reads announcements aloud (the announce command), with Android's text-to-speech. */
final class Speaker implements TextToSpeech.OnInitListener {

    private final Context context;
    private final Handler handler = new Handler();
    private TextToSpeech tts;
    private boolean ready, failed;
    private final List<String> queue = new ArrayList<String>();
    private int restoreVolume = -1;

    private final Runnable restore = new Runnable() {
        public void run() {
            if (restoreVolume >= 0) Volume.setIndex(context, restoreVolume);
            restoreVolume = -1;
        }
    };

    Speaker(Context c) {
        this.context = c;
    }

    /** False if this tablet has no text-to-speech engine. {@code volume}: percent, or -1 to leave it. */
    boolean say(String text, int volume) {
        if (failed) return false;
        if (volume >= 0) {
            if (restoreVolume < 0) restoreVolume = Volume.index(context);
            Volume.setPercent(context, volume);
            handler.removeCallbacks(restore);
            handler.postDelayed(restore, 3000L + text.length() * 90L); // about how long it takes to say
        }
        if (tts == null) tts = new TextToSpeech(context, this);
        if (ready) speak(text);
        else queue.add(text);
        return true;
    }

    private void speak(String text) {
        tts.speak(text, 1 /* QUEUE_ADD */, null, "kiosky-" + System.currentTimeMillis());
    }

    @Override
    public void onInit(int status) {
        if (status != 0 /* SUCCESS */) {
            failed = true;
            queue.clear();
            return;
        }
        ready = true;
        for (String t : queue) speak(t);
        queue.clear();
    }

    void shutdown() {
        handler.removeCallbacks(restore);
        restore.run();
        if (tts != null) tts.shutdown();
        tts = null;
        ready = false;
    }

    /** STREAM_MUSIC volume, which pages and text-to-speech play on. */
    static final class Volume {
        private static final int MUSIC = 3;

        static int index(Context c) {
            try {
                return ((AudioManager) c.getSystemService("audio")).getStreamVolume(MUSIC);
            } catch (Exception e) {
                return -1;
            }
        }

        static void setIndex(Context c, int index) {
            try {
                ((AudioManager) c.getSystemService("audio")).setStreamVolume(MUSIC, index, 0);
            } catch (Exception ignored) {
                // Do Not Disturb or the device refuses
            }
        }

        static void setPercent(Context c, int percent) {
            try {
                AudioManager am = (AudioManager) c.getSystemService("audio");
                int max = am.getStreamMaxVolume(MUSIC);
                am.setStreamVolume(MUSIC, Math.round(max * Json.clamp(percent, 0, 100) / 100f), 0);
            } catch (Exception ignored) {
                // Do Not Disturb or the device refuses
            }
        }
    }
}
