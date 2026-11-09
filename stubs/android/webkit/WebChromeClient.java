package android.webkit; public class WebChromeClient {
 public void onPermissionRequest(PermissionRequest r){} public void onPermissionRequestCanceled(PermissionRequest r){}
 public boolean onJsAlert(WebView v,String u,String m,JsResult r){return false;}
 public boolean onJsConfirm(WebView v,String u,String m,JsResult r){return false;}
 public boolean onJsPrompt(WebView v,String u,String m,String d,JsPromptResult r){return false;}
 public boolean onJsBeforeUnload(WebView v,String u,String m,JsResult r){return false;}
 public boolean onConsoleMessage(ConsoleMessage m){return false;}
 public boolean onShowFileChooser(WebView v,ValueCallback<android.net.Uri[]> cb,FileChooserParams p){return false;}
 public abstract static class FileChooserParams { public abstract android.content.Intent createIntent();
  public static android.net.Uri[] parseResult(int code,android.content.Intent data){return null;} } }
