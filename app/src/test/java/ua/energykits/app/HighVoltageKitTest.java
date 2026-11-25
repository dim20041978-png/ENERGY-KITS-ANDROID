package ua.energykits.app;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.io.InputStream;
import static org.junit.Assert.*;

public class HighVoltageKitTest {
    private JSONArray resource(String name) throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(name)) {
            assertNotNull(name, input);
            return OfferSource.read(input);
        }
    }
    @Test public void matrixHasAtLeastOneHundredUniqueAutomaticRules() throws Exception {
        JSONArray rules = resource("bms-compatibility.json");
        java.util.Map<String, Boolean> unique = new java.util.HashMap<>();
        for (int n = 0; n < rules.length(); n++) {
            JSONObject rule = rules.getJSONObject(n);
            JSONArray inverters = rule.getJSONArray("inverters");
            JSONArray batteries = rule.getJSONArray("batteries");
            for (int i = 0; i < inverters.length(); i++)
                for (int b = 0; b < batteries.length(); b++)
                    unique.put(inverters.getString(i) + "\n" + batteries.getString(b),
                            rule.optBoolean("automatic") && rule.optInt("min_modules") >= 1);
        }
        int count = 0;
        for (boolean enabled : unique.values()) if (enabled) count++;
        assertTrue("Unique automatic BMS rules: " + count, count >= 100);
    }

    @Test public void completeStackSupportsTwentyKwWithoutMixingVoltageClasses() throws Exception {
        JSONArray offers = resource("offers.json");
        CompatibilityCatalog catalog = new CompatibilityCatalog(offers, resource("bms-compatibility.json"));
        JSONObject inverter = null, battery = null, low = null;
        for (int n = 0; n < offers.length(); n++) {
            JSONObject item = offers.getJSONObject(n);
            if ("Solis S6-EH3P20K-H".equals(item.optString("model"))) inverter = item;
            if ("Dyness STACK100-12".equals(item.optString("model"))) battery = item;
            if ("Deye SE-G5.1 Pro-B".equals(item.optString("model"))) low = item;
        }
        assertNotNull(inverter); assertNotNull(battery); assertNotNull(low);
        assertEquals(1, catalog.minimumModules(inverter, battery, 20, 0));
        assertTrue(catalog.canRecommend(inverter, battery, 15));
        assertTrue(catalog.canRecommend(inverter, battery, 20));
        assertEquals(0, catalog.minimumModules(inverter, battery, 20, 200));
        assertEquals(0, catalog.minimumModules(inverter, low, 20, 0));
        JSONObject invalid = new JSONObject(battery.toString()).put("_kit_modules", 2);
        assertFalse(catalog.canRecommend(inverter, invalid, 20));
    }
}
