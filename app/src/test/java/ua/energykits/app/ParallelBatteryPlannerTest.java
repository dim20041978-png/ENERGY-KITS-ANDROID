package ua.energykits.app;

import org.json.JSONObject;
import org.json.JSONArray;
import org.junit.Test;
import java.io.InputStream;
import static org.junit.Assert.*;

public class ParallelBatteryPlannerTest {
    private JSONArray resource(String name) throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(name)) {
            assertNotNull(name, input);
            return OfferSource.read(input);
        }
    }

    @Test public void modulesRespectInverterAndBatteryLimits() throws Exception {
        JSONArray offers = resource("offers.json");
        CompatibilityCatalog catalog = new CompatibilityCatalog(offers, resource("bms-compatibility.json"));
        JSONObject inverter = new JSONObject().put("kind","inverter")
                .put("model","Deye SUN-20K-SG05LP3-EU-SM2");
        JSONObject battery = new JSONObject().put("kind","battery")
                .put("model","Deye SE-G5.1 Pro-B");
        assertEquals(3, catalog.minimumModules(inverter,battery,10,0));
        assertEquals(4, catalog.minimumModules(inverter,battery,12,0));
        assertEquals(5, catalog.minimumModules(inverter,battery,10,450));
        assertEquals(0, catalog.minimumModules(inverter,battery,20,0));
        assertEquals(0, catalog.minimumModules(inverter,battery,10,3300));
        JSONObject bank = new JSONObject(battery.toString()).put("_kit_modules",3);
        assertTrue(catalog.canUseBank(inverter,bank,10));
        bank.put("_kit_modules",2);
        assertFalse(catalog.canUseBank(inverter,bank,10));
        bank.put("_kit_modules",33);
        assertFalse(catalog.canUseBank(inverter,bank,10));
    }
}
