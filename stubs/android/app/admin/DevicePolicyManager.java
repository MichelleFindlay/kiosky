package android.app.admin; public class DevicePolicyManager {
 public boolean isDeviceOwnerApp(String p){return false;} public boolean isLockTaskPermitted(String p){return false;}
 public void setLockTaskPackages(android.content.ComponentName a,String[] p){}
 public void setLockTaskFeatures(android.content.ComponentName a,int f){} public void clearDeviceOwnerApp(String p){}
 public boolean setStatusBarDisabled(android.content.ComponentName a,boolean d){return false;}
 public void addPersistentPreferredActivity(android.content.ComponentName a,android.content.IntentFilter f,android.content.ComponentName c){}
 public void clearPackagePersistentPreferredActivities(android.content.ComponentName a,String p){} }
