package ua.energykits.app;
final class CompatibilityMath {
 private CompatibilityMath() {}
 static boolean positiveFinite(double v){return v>0&&!Double.isInfinite(v);}
 static boolean validRange(double low,double high){return positiveFinite(low)&&positiveFinite(high)&&high>=low;}
 static boolean fitsLimit(double required,double limit){
 return positiveFinite(required)&&positiveFinite(limit)&&required<=limit;
 }
 static boolean exceedsLimit(double required,double limit){
 return positiveFinite(required)&&positiveFinite(limit)&&required>limit;
 }
 static double requiredCurrent(double kw,double minimumVoltage,double nominalVoltage){
 double v=positiveFinite(minimumVoltage)?minimumVoltage:nominalVoltage;
 return positiveFinite(v)&&positiveFinite(kw)?kw*1000/(v*0.9*0.9):Double.NaN;
 }
 static boolean voltageConflict(double nominal,double battery,double min,double max,double low,double high){
 if(validRange(min,max)&&validRange(low,high))return low<min||high>max;
 return positiveFinite(nominal)&&positiveFinite(battery)&&Math.abs(nominal-battery)>nominal*0.1;
 }
}
