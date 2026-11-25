package ua.energykits.app;
final class CompatibilityMath {
 private CompatibilityMath() {}
 static boolean positiveFinite(double v){return v>0&&!Double.isInfinite(v);}
 static double requiredCurrent(double kw,double minimumVoltage,double nominalVoltage){
 double v=positiveFinite(minimumVoltage)?minimumVoltage:nominalVoltage;
 return positiveFinite(v)&&positiveFinite(kw)?kw*1000/(v*0.9*0.9):Double.NaN;
 }
 static boolean voltageConflict(double nominal,double battery,double min,double max,double low,double high){
 if(positiveFinite(min)&&positiveFinite(max)&&max>=min&&positiveFinite(low)&&positiveFinite(high)&&high>=low)return low<min||high>max;
 return positiveFinite(nominal)&&positiveFinite(battery)&&Math.abs(nominal-battery)>nominal*0.1;
 }
}
