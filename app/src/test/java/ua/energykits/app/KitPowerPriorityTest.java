package ua.energykits.app;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class KitPowerPriorityTest {
    private JSONObject[] pair(double kw, double inverterPrice, double batteryPrice) throws Exception {
        return new JSONObject[]{new JSONObject().put("kw",kw).put("price",inverterPrice),
                new JSONObject().put("price",batteryPrice)};
    }

    @Test public void exactPowerWinsEvenWhenLargerKitIsCheaper() throws Exception {
        JSONObject[] exact = pair(3,50000,30000), bigger = pair(5,20000,30000);
        assertTrue(MainActivity.compareKitPriority(exact,bigger,3) < 0);
        assertFalse(MainActivity.isHigherPower(3,3));
        assertTrue(MainActivity.isHigherPower(5,3));
    }

    @Test public void closestLargerPowerThenLowestTotalPrice() throws Exception {
        List<JSONObject[]> pairs = new ArrayList<>();
        JSONObject[] far = pair(8,10000,10000), nearExpensive = pair(5,40000,40000),
                nearCheap = pair(5,30000,30000);
        pairs.add(far); pairs.add(nearExpensive); pairs.add(nearCheap);
        pairs.sort((p,q)->MainActivity.compareKitPriority(p,q,3));
        assertSame(nearCheap,pairs.get(0));
        assertSame(nearExpensive,pairs.get(1));
        assertSame(far,pairs.get(2));
    }

    @Test public void missingRequestedPowerDoesNotHighlightAlternative() {
        assertFalse(MainActivity.isHigherPower(5,Double.NaN));
        assertFalse(MainActivity.isHigherPower(3.0000001,3));
        assertEquals("3,6",MainActivity.powerText(3.6));
    }
}
