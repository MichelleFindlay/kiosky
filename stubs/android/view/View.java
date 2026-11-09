package android.view;
public class View {
 public View(android.content.Context c){}
 public void setSystemUiVisibility(int v){} public void setOnSystemUiVisibilityChangeListener(OnSystemUiVisibilityChangeListener l){}
 public void setVisibility(int v){} public void setEnabled(boolean e){} public void setKeepScreenOn(boolean k){} public final int getWidth(){return 0;} public final int getHeight(){return 0;} public void draw(android.graphics.Canvas c){} public int getVisibility(){return 0;} public void setBackgroundColor(int c){}
 public void setPadding(int a,int b,int c,int d){} public void setOnClickListener(OnClickListener l){}
 public boolean hasFocus(){return false;} public void clearFocus(){} public final boolean requestFocus(){return false;}
 public android.os.IBinder getWindowToken(){return null;}
 public void setLayoutParams(ViewGroup.LayoutParams p){} public void setTranslationX(float x){} public void setTranslationY(float y){} public void setAlpha(float a){}
 public void setOnLongClickListener(OnLongClickListener l){} public void setLongClickable(boolean b){} public void setHapticFeedbackEnabled(boolean b){}
 public interface OnLongClickListener { boolean onLongClick(View v); }
 public interface OnClickListener { void onClick(View v); }
 public interface OnSystemUiVisibilityChangeListener { void onSystemUiVisibilityChange(int v); } }
