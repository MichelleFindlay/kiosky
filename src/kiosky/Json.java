package kiosky;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** org.json helpers that don't throw checked exceptions. */
final class Json {

    private Json() {}

    /** Builds an object from key/value pairs: {@code Json.obj("a", 1, "b", "x")}. */
    static JSONObject obj(Object... kv) {
        JSONObject o = new JSONObject();
        for (int i = 0; i + 1 < kv.length; i += 2) put(o, (String) kv[i], kv[i + 1]);
        return o;
    }

    static JSONObject put(JSONObject o, String key, Object value) {
        try {
            o.put(key, value == null ? JSONObject.NULL : value);
        } catch (JSONException e) {
            // only thrown for NaN/infinite numbers, which we never send
        }
        return o;
    }

    static JSONObject parse(String s) {
        if (s == null || s.length() == 0) return null;
        try {
            return new JSONObject(s);
        } catch (JSONException e) {
            return null;
        }
    }

    static JSONArray parseArray(String s) {
        if (s != null && s.length() > 0) {
            try {
                return new JSONArray(s);
            } catch (JSONException e) {
                // fall through to an empty list
            }
        }
        return new JSONArray();
    }

    /** Like optString, but returns {@code def} for JSON null instead of "null". */
    static String str(JSONObject o, String key, String def) {
        if (o == null || !o.has(key) || o.isNull(key)) return def;
        return o.optString(key, def);
    }

    static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
