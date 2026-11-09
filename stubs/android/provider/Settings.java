package android.provider; public final class Settings { public static boolean canDrawOverlays(android.content.Context c){return false;}
 public static final class Secure { public static String getString(android.content.ContentResolver r,String n){return null;} }
 public static final class System { public static int getInt(android.content.ContentResolver r,String n,int d){return 0;} }
 public static final class Global { public static int getInt(android.content.ContentResolver r,String n,int d){return 0;}
  public static boolean putInt(android.content.ContentResolver r,String n,int v){return false;} } }
