package android.accessibilityservice; public abstract class AccessibilityService extends android.content.Context {
 protected void onServiceConnected(){} public void onDestroy(){} public final boolean performGlobalAction(int a){return false;}
 public abstract void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent e); public abstract void onInterrupt();
 public android.content.SharedPreferences getSharedPreferences(String n,int m){return null;} public android.content.res.Resources getResources(){return null;}
 public Object getSystemService(String n){return null;} public android.content.res.AssetManager getAssets(){return null;} }
