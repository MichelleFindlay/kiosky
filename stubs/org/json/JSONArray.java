package org.json; public class JSONArray { public JSONArray(){} public JSONArray(String s) throws JSONException {}
 public JSONArray put(Object v){return this;} public Object opt(int i){return null;} public String optString(int i,String d){return null;}
 public int length(){return 0;} public JSONObject optJSONObject(int i){return null;} }
