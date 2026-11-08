package android.webkit; public class WebViewClient {
 public boolean shouldOverrideUrlLoading(WebView v,String u){return false;} public void onPageFinished(WebView v,String u){}
 public void onPageStarted(WebView v,String u,android.graphics.Bitmap f){}
 public void onReceivedError(WebView v,int c,String d,String u){} }
