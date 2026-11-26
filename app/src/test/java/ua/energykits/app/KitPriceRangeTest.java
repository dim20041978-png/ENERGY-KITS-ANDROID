package ua.energykits.app;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class KitPriceRangeTest {
    @Test public void includesAllBatteryModulesAndExactLimits() throws Exception {
        JSONObject[] pair={new JSONObject().put("price",58900),new JSONObject().put("price",68000).put("_kit_modules",2)};
        long price=KitPriceRange.total(pair);
        assertEquals(12690000L,price);
        assertTrue(KitPriceRange.includes(price,price,price));
        assertTrue(KitPriceRange.includes(price,10000000,15000000));
        assertFalse(KitPriceRange.includes(price,0,price-1));
        assertFalse(KitPriceRange.includes(price,price+1,15000000));
    }
    @Test public void parsesExactCurrencyAndRejectsInvalidRanges() {
        assertEquals(12690025,KitPriceRange.parse("126900,25"));
        assertEquals("126900,25",KitPriceRange.format(12690025));
        assertEquals("0",KitPriceRange.format(0));
        assertEquals(-1,KitPriceRange.parse(""));
        assertEquals(-1,KitPriceRange.parse("-1"));
        assertEquals(-1,KitPriceRange.parse("1.234"));
        assertEquals(-1,KitPriceRange.parse("NaN"));
        assertFalse(KitPriceRange.includes(100,-1,200));
        assertFalse(KitPriceRange.includes(100,200,100));
    }
    @Test public void sliderClampsToActualPriceBounds() {
        assertEquals(100,KitPriceRange.position(100,200,-1));
        assertEquals(150,KitPriceRange.position(100,200,.5f));
        assertEquals(200,KitPriceRange.position(100,200,2));
        assertEquals(100,KitPriceRange.position(100,100,.5f));
    }
    @Test public void invalidPriceCannotMatch() throws Exception {
        JSONObject[] pair={new JSONObject().put("price",1000),new JSONObject()};
        assertEquals(-1,KitPriceRange.total(pair));
        assertFalse(KitPriceRange.includes(-1,0,100000));
    }
}
