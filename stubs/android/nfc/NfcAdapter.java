package android.nfc; public final class NfcAdapter {
 public static NfcAdapter getDefaultAdapter(android.content.Context c){return null;} public boolean isEnabled(){return false;}
 public void enableReaderMode(android.app.Activity a,ReaderCallback cb,int flags,android.os.Bundle extras){} public void disableReaderMode(android.app.Activity a){}
 public interface ReaderCallback { void onTagDiscovered(Tag tag); } }
