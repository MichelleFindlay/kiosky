package android.content;
public class Intent implements android.os.Parcelable {
 public Intent(String a){} public Intent(String a,android.net.Uri u){} public Intent(Context c,Class<?> k){}
 public Intent setPackage(String p){return this;} public Intent addFlags(int f){return this;}
 public Intent putExtra(String n,String v){return this;} public int getIntExtra(String n,int d){return 0;}
 public String getStringExtra(String n){return null;} public <T extends android.os.Parcelable> T getParcelableExtra(String n){return null;} }
