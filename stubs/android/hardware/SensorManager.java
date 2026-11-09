package android.hardware; public abstract class SensorManager { public Sensor getDefaultSensor(int t){return null;}
 public boolean registerListener(SensorEventListener l,Sensor s,int r){return false;} public void unregisterListener(SensorEventListener l){} }
