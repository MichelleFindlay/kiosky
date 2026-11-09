package kiosky;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

public class JsonTest {

    @Test
    public void objBuildsFromPairs() {
        JSONObject o = Json.obj("a", 1, "b", "x", "c", true);
        assertEquals(1, o.optInt("a", 0));
        assertEquals("x", o.optString("b"));
        assertTrue(o.optBoolean("c", false));
    }

    @Test
    public void nullBecomesJsonNull() {
        JSONObject o = Json.obj("gone", null);
        assertTrue(o.has("gone"));
        assertTrue(o.isNull("gone"));
        assertEquals("{\"gone\":null}", o.toString());
    }

    @Test
    public void putIgnoresNumbersJsonCantHold() {
        JSONObject o = new JSONObject();
        Json.put(o, "nan", Double.NaN); // org.json refuses NaN; Json.put swallows that instead of throwing
        Json.put(o, "after", 1);
        assertEquals(1, o.optInt("after", 0));
    }

    @Test
    public void parseReturnsNullForBadInput() {
        assertNull(Json.parse(null));
        assertNull(Json.parse(""));
        assertNull(Json.parse("{not json"));
        assertNull(Json.parse("[1, 2]"));
        assertEquals(2, Json.parse("{\"a\": 2}").optInt("a", 0));
    }

    @Test
    public void parseArrayFallsBackToEmpty() {
        assertEquals(0, Json.parseArray(null).length());
        assertEquals(0, Json.parseArray("nope").length());
        JSONArray a = Json.parseArray("[1, \"two\"]");
        assertEquals(2, a.length());
        assertEquals("two", a.optString(1, ""));
    }

    @Test
    public void strTreatsMissingAndNullAlike() {
        JSONObject o = Json.parse("{\"set\": \"v\", \"empty\": \"\", \"nothing\": null}");
        assertEquals("v", Json.str(o, "set", "d"));
        assertEquals("", Json.str(o, "empty", "d"));
        assertEquals("d", Json.str(o, "nothing", "d")); // not the text "null"
        assertEquals("d", Json.str(o, "missing", "d"));
        assertEquals("d", Json.str(null, "set", "d"));
    }

    @Test
    public void clamp() {
        assertEquals(5, Json.clamp(1, 5, 10));
        assertEquals(10, Json.clamp(99, 5, 10));
        assertEquals(7, Json.clamp(7, 5, 10));
        assertEquals(-1, Json.clamp(-1, -1, 100));
    }
}
