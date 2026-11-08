package android.webkit; import android.os.Bundle;
public class WebView extends android.widget.AbsoluteLayout {
 public WebView(android.content.Context c){super(c);} public WebSettings getSettings(){return null;} public void loadUrl(String u){}
 public void reload(){} public boolean canGoBack(){return false;} public void goBack(){} public void onResume(){} public void onPause(){}
 public void destroy(){} public void setWebViewClient(WebViewClient c){} public void setWebChromeClient(WebChromeClient c){}
 public WebBackForwardList saveState(Bundle b){return null;} public WebBackForwardList restoreState(Bundle b){return null;} }
