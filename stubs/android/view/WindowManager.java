package android.view; public interface WindowManager { void addView(View v,ViewGroup.LayoutParams p); void removeView(View v);
 class LayoutParams extends ViewGroup.LayoutParams { public int gravity; public float screenBrightness;
  public LayoutParams(int w,int h,int t,int f,int fmt){super(w,h);} } }
