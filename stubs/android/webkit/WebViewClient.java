package android.webkit; public class WebViewClient {
 public boolean shouldOverrideUrlLoading(WebView v,String u){return false;} public void onPageFinished(WebView v,String u){}
 public void onPageStarted(WebView v,String u,android.graphics.Bitmap f){}
 public void onReceivedError(WebView v,int c,String d,String u){}
 public void onReceivedHttpError(WebView v,WebResourceRequest r,WebResourceResponse p){}
 public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail d){return false;}
 public void doUpdateVisitedHistory(WebView v,String u,boolean r){}
 public void onReceivedHttpAuthRequest(WebView v,HttpAuthHandler h,String host,String realm){}
 public void onReceivedSslError(WebView v,SslErrorHandler h,android.net.http.SslError e){} }
