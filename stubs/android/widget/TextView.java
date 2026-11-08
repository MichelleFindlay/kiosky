package android.widget;
public class TextView extends android.view.View {
 public TextView(android.content.Context c){super(c);}
 public final void setText(CharSequence t){} public CharSequence getText(){return null;} public void setTextColor(int c){} public void setTextSize(float s){} public void setGravity(int g){}
 public final void setHintTextColor(int c){} public final void setHint(CharSequence h){} public void setSingleLine(boolean b){}
 public void setInputType(int t){} public void setImeOptions(int o){} public void setSelectAllOnFocus(boolean b){}
 public void setOnEditorActionListener(OnEditorActionListener l){}
 public interface OnEditorActionListener { boolean onEditorAction(TextView v,int id,android.view.KeyEvent e); } }
