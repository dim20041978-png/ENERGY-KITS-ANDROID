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

    @Test public void everyApplianceHasDefaultValues() {
        assertEquals(ApplianceUsage.NAMES.length, ApplianceUsage.WATTS.length);
        assertEquals(ApplianceUsage.NAMES.length, ApplianceUsage.HOURS.length);
        assertEquals(ApplianceUsage.NAMES.length, ApplianceUsage.START_CHECK.length);
    }
}
