package ua.energykits.app;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class AvailableKitCatalogTest {
    private JSONArray resource(String name) throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(name)) {
            assertNotNull(name, input);
            return OfferSource.read(input);
        }
    }

    private Set<String> combinations(JSONArray offers, CompatibilityCatalog catalog, double kw) throws Exception {
        Set<String> result = new HashSet<>();
        for (JSONObject inverter : OfferSource.matches(offers, "inverter", 0, kw)) {
            for (JSONObject battery : OfferSource.matches(offers, "battery", 0, Double.MIN_VALUE)) {
                int count = catalog.minimumModules(inverter, battery, kw, 0);
                if (count < 1) continue;
                JSONObject bank = new JSONObject(battery.toString())
                        .put("_kit_modules", count)
                        .put("price", battery.optDouble("price") * count)
                        .put("ah", battery.optDouble("ah") * count);
                if (catalog.canRecommend(inverter, bank, kw)) {
                    result.add(inverter.optString("model") + "\n" + battery.optString("model"));
                }
            }
        }
        return result;
    }

    @Test public void catalogSupportsHundredDistinctCombinationsAndHvAlternatives() throws Exception {
        JSONArray offers = resource("offers.json");
        CompatibilityCatalog catalog = new CompatibilityCatalog(offers, resource("bms-compatibility.json"));
        assertTrue("At least 100 distinct eligible model pairs", combinations(offers, catalog, 1).size() >= 100);
        assertTrue("At least 15 alternatives at 15 kW", combinations(offers, catalog, 15).size() >= 15);
        assertTrue("At least 3 alternatives at 20 kW", combinations(offers, catalog, 20).size() >= 3);
        for (String pair : combinations(offers, catalog, 20)) {
            assertFalse(pair.contains("Deye SUN-20K-SG01HP3-EU-AM2"));
        }
        for (String pair : combinations(offers, catalog, 1)) {
            assertFalse(pair.startsWith("SRNE ASF4880S180-H\n"));
            assertFalse(pair.startsWith("MUST PV18-5248 PRO\n"));
        }
    }
}
