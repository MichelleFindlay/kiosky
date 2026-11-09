package android.app; public class ActivityManager { public int getLockTaskModeState(){return 0;} public boolean isInLockTaskMode(){return false;}
 public void getMemoryInfo(MemoryInfo o){}
 public static class MemoryInfo { public long availMem; public long totalMem; public boolean lowMemory; public MemoryInfo(){} } }
