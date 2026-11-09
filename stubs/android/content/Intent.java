package android.content;
public class Intent implements android.os.Parcelable {
 public Intent(){} public Intent(String a){} public Intent(String a,android.net.Uri u){} public Intent(Context c,Class<?> k){}
 public Intent setPackage(String p){return this;} public Intent addCategory(String c){return this;} public Intent addFlags(int f){return this;}
 public Intent putExtra(String n,String v){return this;} public Intent putExtra(String n,boolean v){return this;} public Intent putExtra(String n,int v){return this;} public int getIntExtra(String n,int d){return 0;}
 public boolean getBooleanExtra(String n,boolean d){return false;} public String getAction(){return null;} public android.net.Uri getData(){return null;}
 public String getStringExtra(String n){return null;} public <T extends android.os.Parcelable> T getParcelableExtra(String n){return null;} }
