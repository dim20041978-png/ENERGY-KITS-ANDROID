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
