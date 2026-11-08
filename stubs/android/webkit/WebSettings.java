package android.webkit; public abstract class WebSettings {
 public abstract void setJavaScriptEnabled(boolean b); public abstract void setDomStorageEnabled(boolean b);
 public abstract void setDatabaseEnabled(boolean b); public abstract void setMediaPlaybackRequiresUserGesture(boolean b);
 public abstract void setCacheMode(int m); public abstract void setTextZoom(int z);
 public abstract String getUserAgentString(); public abstract void setUserAgentString(String ua); }
