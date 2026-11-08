package android.content.pm; public abstract class PackageManager {
 public abstract PackageInfo getPackageInfo(String p,int f) throws NameNotFoundException;
 public abstract boolean canRequestPackageInstalls(); public abstract PackageInstaller getPackageInstaller();
 public abstract ResolveInfo resolveActivity(android.content.Intent i,int f);
 public abstract void setComponentEnabledSetting(android.content.ComponentName c,int s,int f);
 public static class NameNotFoundException extends Exception {} }
