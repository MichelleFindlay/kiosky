package android.content.pm; public abstract class PackageManager {
 public abstract PackageInfo getPackageInfo(String p,int f) throws NameNotFoundException;
 public abstract boolean canRequestPackageInstalls(); public abstract PackageInstaller getPackageInstaller();
 public static class NameNotFoundException extends Exception {} }
