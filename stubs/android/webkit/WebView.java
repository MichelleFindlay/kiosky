package android.webkit; import android.os.Bundle;
public class WebView extends android.widget.AbsoluteLayout {
 public WebView(android.content.Context c){super(c);} public WebSettings getSettings(){return null;} public void loadUrl(String u){}
 public void loadDataWithBaseURL(String b,String d,String m,String e,String h){}
 public String getUrl(){return null;} public String getTitle(){return null;} public void stopLoading(){}
 public void clearCache(boolean b){} public void clearHistory(){}
 public void reload(){} public boolean canGoBack(){return false;} public void goBack(){} public void onResume(){} public void onPause(){}
 public void destroy(){} public void setWebViewClient(WebViewClient c){} public void setWebChromeClient(WebChromeClient c){}
 public void evaluateJavascript(String s,ValueCallback<String> cb){} public void addJavascriptInterface(Object o,String n){}
 public WebBackForwardList saveState(Bundle b){return null;} public WebBackForwardList restoreState(Bundle b){return null;} }
