package android.content; public interface DialogInterface { void dismiss();
 interface OnClickListener { void onClick(DialogInterface d,int w); } interface OnDismissListener { void onDismiss(DialogInterface d); } }
