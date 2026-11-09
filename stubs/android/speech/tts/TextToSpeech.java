package android.speech.tts; public class TextToSpeech { public TextToSpeech(android.content.Context c,OnInitListener l){}
 public int speak(CharSequence t,int q,android.os.Bundle p,String id){return 0;} public int setLanguage(java.util.Locale l){return 0;} public void shutdown(){}
 public interface OnInitListener { void onInit(int status); } }
