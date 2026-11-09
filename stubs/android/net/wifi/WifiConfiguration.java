package android.net.wifi; public class WifiConfiguration { public String SSID; public String preSharedKey; public boolean hiddenSSID;
 public int priority; public int networkId; public boolean requirePmf; public java.util.BitSet allowedKeyManagement = new java.util.BitSet();
 public void setSecurityParams(int t){} }
