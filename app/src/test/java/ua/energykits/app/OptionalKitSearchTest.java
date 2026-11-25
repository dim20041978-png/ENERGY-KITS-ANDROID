package ua.energykits.app;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class OptionalKitSearchTest {
    @Test public void powerOnlyAndPartialInputs() {
        assertEquals(5.0, MainActivity.searchNumber("5", false), 0);
        assertEquals(0.0, MainActivity.searchNumber("", true), 0);
        assertEquals(0.0, MainActivity.searchNumber("   ", true), 0);
        assertEquals(51.2, MainActivity.searchNumber("51,2", true), 0);
        assertEquals(100.0, MainActivity.searchNumber("100", true), 0);
    }

    @Test public void invalidNumbersDoNotBecomeAutomatic() {
        for (String value : new String[]{"0", "-1", "NaN", "Infinity", "abc"}) {
            try {
                MainActivity.searchNumber(value, true);
                fail("Accepted invalid input: " + value);
            } catch (IllegalArgumentException expected) { }
        }
        try {
            MainActivity.searchNumber("", false);
            fail("Accepted missing power");
        } catch (IllegalArgumentException expected) { }
    }

    private JSONObject battery(String model, double voltage, double ah) throws Exception {
        return new JSONObject().put("kind", "battery").put("model", model)
                .put("voltage", voltage).put("ah", ah).put("price", 1000)
                .put("currency", "UAH").put("seller", "test")
                .put("updated_at", "2026-11-25").put("url", "https://example.org/item");
    }

    @Test public void optionalFiltersAndExplicitConstraints() throws Exception {
        JSONArray data = new JSONArray().put(battery("24V", 25.6, 100))
                .put(battery("48V", 51.2, 100)).put(battery("48V large", 51.2, 200))
                .put(battery("invalid capacity", 51.2, 0));
        double anyCapacity = Math.max(MainActivity.searchNumber("", true), Double.MIN_VALUE);
        assertEquals(3, OfferSource.matches(data, "battery", 0, anyCapacity).size());
        assertEquals(2, OfferSource.matches(data, "battery", 48, anyCapacity).size());
        assertEquals(1, OfferSource.matches(data, "battery", 0, 150).size());
        assertEquals(1, OfferSource.matches(data, "battery", 48, 150).size());
        assertEquals("48V large", OfferSource.matches(data, "battery", 48, 150).get(0).getString("model"));
    }
}
