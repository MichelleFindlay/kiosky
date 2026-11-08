package android.app;
import android.os.Bundle; import android.view.*;
public class Activity extends android.content.Context {
 protected void onCreate(Bundle b){} protected void onResume(){} protected void onPause(){} protected void onDestroy(){}
 protected void onSaveInstanceState(Bundle b){} public void onBackPressed(){} public void onWindowFocusChanged(boolean f){}
 public Window getWindow(){return null;} public void setContentView(View v){}
 public android.content.SharedPreferences getSharedPreferences(String n,int m){return null;}
 public android.content.res.Resources getResources(){return null;} public Object getSystemService(String n){return null;} }
