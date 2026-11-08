package android.view;
public abstract class ViewGroup extends View {
 public ViewGroup(android.content.Context c){super(c);}
 public void addView(View v, LayoutParams p){}
 public static class LayoutParams { public LayoutParams(int w,int h){} }
 public static class MarginLayoutParams extends LayoutParams { public MarginLayoutParams(int w,int h){super(w,h);} } }
