package android.content.pm; public class PackageInstaller {
 public int createSession(SessionParams p) throws java.io.IOException {return 0;}
 public Session openSession(int id) throws java.io.IOException {return null;}
 public static class SessionParams { public SessionParams(int mode){} }
 public static class Session implements java.io.Closeable {
  public java.io.OutputStream openWrite(String n,long off,long len) throws java.io.IOException {return null;}
  public void fsync(java.io.OutputStream o) throws java.io.IOException {}
  public void commit(android.content.IntentSender s){} public void abandon(){} public void close(){} } }
