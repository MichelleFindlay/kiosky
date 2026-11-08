package android.graphics; public final class Bitmap {
 public enum Config { ARGB_8888 } public enum CompressFormat { JPEG, PNG }
 public static Bitmap createBitmap(int w,int h,Config c){return null;} public static Bitmap createScaledBitmap(Bitmap b,int w,int h,boolean f){return null;}
 public boolean compress(CompressFormat f,int q,java.io.OutputStream o){return false;} public void recycle(){}
 public int getWidth(){return 0;} public int getHeight(){return 0;} }
