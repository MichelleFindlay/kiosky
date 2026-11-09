package kiosky;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Notices someone at the tablet without a touch: the proximity sensor reporting
 * "near" (a hand or face close to the top edge), or the light sensor seeing a clear
 * jump in brightness (a room light switched on, someone stepping in front of a lamp).
 * Both sensors are low-power and are only listened to while wanted.
 */
final class MotionWake implements SensorEventListener {

    interface Listener {
        void onMotion(String source);
    }

    private static final int TYPE_LIGHT = 5, TYPE_PROXIMITY = 8;
    private static final int RATE = 3; // SENSOR_DELAY_NORMAL

    private final SensorManager sm;
    private final Listener listener;
    private boolean running;
    private float baseline = -1; // slowly follows the room's light level

    MotionWake(Context c, Listener listener) {
        this.sm = (SensorManager) c.getSystemService("sensor");
        this.listener = listener;
    }

    /** Whether this tablet has any sensor that can wake it. */
    boolean available() {
        return sm != null && (sm.getDefaultSensor(TYPE_PROXIMITY) != null || sm.getDefaultSensor(TYPE_LIGHT) != null);
    }

    void start() {
        if (running || sm == null) return;
        try {
            Sensor prox = sm.getDefaultSensor(TYPE_PROXIMITY);
            Sensor light = sm.getDefaultSensor(TYPE_LIGHT);
            if (prox != null) sm.registerListener(this, prox, RATE);
            if (light != null) sm.registerListener(this, light, RATE);
            running = prox != null || light != null;
            baseline = -1;
        } catch (Exception ignored) {
            // sensors unavailable
        }
    }

    void stop() {
        if (!running) return;
        try {
            sm.unregisterListener(this);
        } catch (Exception ignored) {
            // already gone
        }
        running = false;
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        if (e == null || e.sensor == null || e.values == null || e.values.length == 0) return;
        float v = e.values[0];
        int type = e.sensor.getType();
        if (type == TYPE_PROXIMITY) {
            // Many sensors only report 0 (near) or their maximum range (far)
            if (v < e.sensor.getMaximumRange() && v < 5f) listener.onMotion("proximity");
        } else if (type == TYPE_LIGHT) {
            if (baseline < 0) {
                baseline = v;
                return;
            }
            boolean jump = v - baseline >= 10f && v >= baseline * 1.5f;
            baseline = baseline * 0.9f + v * 0.1f;
            if (jump) {
                baseline = v; // don't fire again for the same change
                listener.onMotion("light");
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor s, int accuracy) {
        // not needed
    }
}
