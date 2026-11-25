package ua.energykits.app;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.io.InputStream;
import static org.junit.Assert.*;

public class SixKwCatalogRegressionTest {
    private JSONArray resource(String name) throws Exception {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(name)) {
            assertNotNull(name, input);
            return OfferSource.read(input);
        }
    }

    private int countUniquePairs(JSONArray offers, CompatibilityCatalog catalog, double kw) {
        java.util.Set<String> pairs = new java.util.HashSet<>();
        for (JSONObject inverter : OfferSource.matches(offers, "inverter", 0, kw)) {
            for (JSONObject battery : OfferSource.matches(offers, "battery", 0, Double.MIN_VALUE)) {
                if (catalog.canRecommend(inverter, battery, kw)) {
                    pairs.add(inverter.optString("model") + "\n" + battery.optString("model"));
                }
            }
        }
        return pairs.size();
    }

    @Test public void alternativesForMultiplePowerLevels() throws Exception {
        JSONArray offers = resource("offers.json");
        CompatibilityCatalog catalog = new CompatibilityCatalog(offers, resource("bms-compatibility.json"));
        assertTrue("3 kW alternatives", countUniquePairs(offers, catalog, 3) >= 30);
        assertTrue("5 kW alternatives", countUniquePairs(offers, catalog, 5) >= 10);
        assertTrue("6 kW alternatives", countUniquePairs(offers, catalog, 6) >= 10);
        assertTrue("8 kW alternatives", countUniquePairs(offers, catalog, 8) >= 5);
    }

    @Test public void sixKwWithoutVoltageOrCapacityHasVerifiedPair() throws Exception {
        JSONArray offers = resource("offers.json");
        CompatibilityCatalog catalog = new CompatibilityCatalog(offers, resource("bms-compatibility.json"));
        boolean found = false;
        for (JSONObject inverter : OfferSource.matches(offers, "inverter", 0, 6)) {
            for (JSONObject battery : OfferSource.matches(offers, "battery", 0, Double.MIN_VALUE)) {
                if (catalog.canRecommend(inverter, battery, 6)) {
                    found = true;
                    break;
                }
            }
        }
        assertTrue("No confirmed pair for a 6 kW load", found);
    }
}
