package android.content;
public abstract class Context {
 public abstract SharedPreferences getSharedPreferences(String n,int m);
 public abstract android.content.res.Resources getResources();
 public abstract Object getSystemService(String n);
 public abstract android.content.res.AssetManager getAssets();
 public android.content.pm.PackageManager getPackageManager(){return null;} public String getPackageName(){return null;}
 public ContentResolver getContentResolver(){return null;} public java.io.File getCacheDir(){return null;} public java.io.File getFilesDir(){return null;}
 public Context getApplicationContext(){return null;} public int checkSelfPermission(String p){return 0;}
 public void startActivity(Intent i){} public void sendBroadcast(Intent i){} public Intent registerReceiver(BroadcastReceiver r,IntentFilter f){return null;}
 public void unregisterReceiver(BroadcastReceiver r){} }
