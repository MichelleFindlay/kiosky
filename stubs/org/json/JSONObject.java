package org.json; public class JSONObject { public static final Object NULL = new Object();
 public JSONObject(){} public JSONObject(String s) throws JSONException {}
 public JSONObject put(String n,Object v) throws JSONException {return this;} public boolean has(String n){return false;} public boolean isNull(String n){return false;}
 public String getString(String n) throws JSONException {return null;} public String optString(String n){return null;} public String optString(String n,String d){return null;}
 public int optInt(String n,int d){return 0;} public long optLong(String n,long d){return 0;} public boolean optBoolean(String n,boolean d){return false;}
 public JSONArray optJSONArray(String n){return null;} public JSONObject optJSONObject(String n){return null;}  public String toString(int indent) throws JSONException {return null;}
}
