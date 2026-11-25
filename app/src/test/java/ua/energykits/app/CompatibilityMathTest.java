package ua.energykits.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class CompatibilityMathTest {
 @Test public void nominal48Accepts51(){assertFalse(CompatibilityMath.voltageConflict(48,51.2,0,0,0,0));}
 @Test public void highVoltageRejected(){assertTrue(CompatibilityMath.voltageConflict(48,204.8,0,0,0,0));}
 @Test public void rangeOutsideRejected(){assertTrue(CompatibilityMath.voltageConflict(48,51.2,42,58,40,57));}
 @Test public void rangeInsideAccepted(){assertFalse(CompatibilityMath.voltageConflict(48,51.2,40,60,42,58));}
 @Test public void currentUsesMinimumVoltage(){assertEquals(6000/(42*0.81),CompatibilityMath.requiredCurrent(6,42,51.2),0.001);}
 @Test public void missingVoltageIsUnknown(){assertTrue(Double.isNaN(CompatibilityMath.requiredCurrent(6,0,0)));}
 @Test public void infinitePowerIsUnknown(){assertTrue(Double.isNaN(CompatibilityMath.requiredCurrent(Double.POSITIVE_INFINITY,42,48)));}
 @Test public void missingElectricalLimitsCannotPass(){
 assertFalse(CompatibilityMath.validRange(40,Double.NaN));
 assertFalse(CompatibilityMath.fitsLimit(100,Double.NaN));
 assertFalse(CompatibilityMath.fitsLimit(100,Double.POSITIVE_INFINITY));
 }
 @Test public void continuousCurrentMustCoverTheRequestedLoad(){
 double needed=CompatibilityMath.requiredCurrent(4,44.8,51.2);
 assertFalse(CompatibilityMath.fitsLimit(needed,100));
 assertTrue(CompatibilityMath.exceedsLimit(needed,100));
 assertTrue(CompatibilityMath.fitsLimit(needed,120));
 assertTrue(CompatibilityMath.fitsLimit(needed,needed));
 }
}
