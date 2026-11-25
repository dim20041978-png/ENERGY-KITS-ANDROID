package ua.energykits.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class ApplianceUsageTest {
    @Test public void acceptsDecimalHoursAndWatts() {
        assertEquals(750, ApplianceUsage.parse("750", false), 0);
        assertEquals(0.25, ApplianceUsage.parse("0,25", true), 0);
        assertEquals(24, ApplianceUsage.parse("24", true), 0);
        assertEquals(0, ApplianceUsage.parse("0", true), 0);
    }

    @Test public void rejectsInvalidInputs() {
        assertTrue(Double.isNaN(ApplianceUsage.parse("25", true)));
        assertTrue(Double.isNaN(ApplianceUsage.parse("-1", true)));
        assertTrue(Double.isNaN(ApplianceUsage.parse("0", false)));
        assertTrue(Double.isNaN(ApplianceUsage.parse("NaN", false)));
        assertTrue(Double.isNaN(ApplianceUsage.parse("Infinity", false)));
        assertTrue(Double.isNaN(ApplianceUsage.parse("", true)));
    }

    @Test public void computesDailyConsumptionAndContinuousRuntime() {
        assertEquals(1500, ApplianceUsage.dailyWh(750, 2), 0.001);
        assertEquals(3883.3333333333, ApplianceUsage.dailyBatteryWh(2847), 0.001);
        assertEquals(5120 * .9 / (515 / .9 + 30), ApplianceUsage.runtimeHours(5120,515),0.001);
        assertTrue(Double.isNaN(ApplianceUsage.runtimeHours(5120,0)));
        assertTrue(Double.isNaN(ApplianceUsage.runtimeHours(0,515)));
    }

    @Test public void screenshotExampleHasDailyDeficit() {
        assertEquals(9216, ApplianceUsage.usableWh(10240), 0.001);
        assertEquals(10064.4444444, ApplianceUsage.dailyBatteryWh(8410), 0.001);
        assertEquals(-848.4444444, ApplianceUsage.dailyBalanceWh(10240,8410), 0.001);
        assertEquals("2 год 31 хв", ApplianceUsage.runtimeText(ApplianceUsage.runtimeHours(10240,3265)));
    }

    @Test public void dailyBalanceSupportsSurplusAndUnknownCapacity() {
        assertEquals(2880, ApplianceUsage.dailyBalanceWh(4000,0), 0.001);
        assertEquals(0, ApplianceUsage.dailyBalanceWh(800,0), 0.001);
        assertTrue(ApplianceUsage.dailyBalanceWh(10240,1000)>0);
        assertTrue(Double.isNaN(ApplianceUsage.dailyBalanceWh(0,1000)));
        assertTrue(Double.isNaN(ApplianceUsage.dailyBalanceWh(10240,-1)));
        assertEquals("1 год 0 хв", ApplianceUsage.runtimeText(0.999));
        assertEquals("—", ApplianceUsage.runtimeText(Double.NaN));
    }

    @Test public void everyApplianceHasDefaultValues() {
        assertEquals(ApplianceUsage.NAMES.length, ApplianceUsage.WATTS.length);
        assertEquals(ApplianceUsage.NAMES.length, ApplianceUsage.HOURS.length);
        assertEquals(ApplianceUsage.NAMES.length, ApplianceUsage.START_CHECK.length);
    }
}
