package android.view; public abstract class Window { public void addFlags(int f){} public void clearFlags(int f){} public abstract View getDecorView();
 public final WindowManager.LayoutParams getAttributes(){return null;} public void setAttributes(WindowManager.LayoutParams a){} }
