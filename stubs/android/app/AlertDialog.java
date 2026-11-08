package android.app; import android.content.DialogInterface;
public class AlertDialog implements DialogInterface {
 public void dismiss(){}
 public static class Builder { public Builder(android.content.Context c){}
  public Builder setTitle(CharSequence t){return this;} public Builder setMessage(CharSequence m){return this;}
  public Builder setView(android.view.View v){return this;}
  public Builder setPositiveButton(CharSequence t,DialogInterface.OnClickListener l){return this;}
  public Builder setNegativeButton(CharSequence t,DialogInterface.OnClickListener l){return this;}
  public Builder setNeutralButton(CharSequence t,DialogInterface.OnClickListener l){return this;}
  public Builder setOnDismissListener(DialogInterface.OnDismissListener l){return this;}
  public AlertDialog show(){return null;} } }
