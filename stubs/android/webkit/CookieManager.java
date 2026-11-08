package android.webkit; public abstract class CookieManager {
 public static CookieManager getInstance(){return null;} public abstract void setAcceptCookie(boolean b);
 public abstract void setAcceptThirdPartyCookies(WebView w,boolean b); public abstract void flush(); }
