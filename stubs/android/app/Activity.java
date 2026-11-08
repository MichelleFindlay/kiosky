package android.app;
import android.os.Bundle; import android.view.*;
public class Activity extends android.content.Context {
 protected void onCreate(Bundle b){} protected void onResume(){} protected void onPause(){} protected void onDestroy(){}
 protected void onSaveInstanceState(Bundle b){} public void onBackPressed(){} public void onWindowFocusChanged(boolean f){}
 protected void onNewIntent(android.content.Intent i){} protected void onActivityResult(int r,int c,android.content.Intent d){}
 public void setIntent(android.content.Intent i){} public android.content.Intent getIntent(){return null;}
 public void startActivityForResult(android.content.Intent i,int r){} public void setRequestedOrientation(int o){} public void recreate(){} protected void onStop(){} public boolean isFinishing(){return false;} public void startLockTask(){} public boolean hasWindowFocus(){return false;} public void stopLockTask(){}
 public Window getWindow(){return null;} public void setContentView(View v){}
 public android.content.SharedPreferences getSharedPreferences(String n,int m){return null;}
 public android.content.res.Resources getResources(){return null;} public Object getSystemService(String n){return null;}
 public android.content.res.AssetManager getAssets(){return null;} }
