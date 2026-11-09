package android.os; public final class PowerManager { public boolean isInteractive(){return false;}
 public WakeLock newWakeLock(int f,String t){return null;}
 public final class WakeLock { public void acquire(long ms){} public void release(){} public boolean isHeld(){return false;} } }
