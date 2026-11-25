package ua.energykits.app;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CompatibilityCatalogTest {
    private JSONArray offers;
    private CompatibilityCatalog catalog;
    private JSONObject inverter;
    private JSONObject battery;

    @Before
    public void readCatalog() throws Exception {
        try (InputStream offerInput = getClass().getResourceAsStream("/offers.json");
             InputStream bmsInput = getClass().getResourceAsStream("/bms-compatibility.json")) {
            assertNotNull(offerInput);
            assertNotNull(bmsInput);
            offers = OfferSource.read(offerInput);
            catalog = new CompatibilityCatalog(offers, OfferSource.read(bmsInput));
        }
        inverter = model("Deye SUN-5K-SG03LP1-EU");
        battery = model("Dyness PowerBrick Plus");
    }

    @Test
    public void manufacturerRuleAllowsPairAtRequestedLoad() {
        assertTrue(catalog.canRecommend(inverter, battery, 3));
        assertEquals(120, CompatibilityCatalog.chargeLimit(
                catalog.specification(inverter), catalog.specification(battery)), 0.001);
    }

    @Test
    public void inverterDischargeLimitCanRuleOutNominalAcPower() {
        assertFalse(catalog.canRecommend(inverter, battery, 5));
        assertFalse(catalog.conflict(inverter, battery, 5).isEmpty());
    }

    @Test
    public void matchingVoltageAndInterfacesDoNotEstablishBmsCompatibility() throws Exception {
        JSONObject unknown = new JSONObject(battery.toString()).put("model", "Unlisted battery");
        assertFalse(catalog.canRecommend(inverter, unknown, 3));
    }

    @Test
    public void systemVoltageBlocksHighVoltageEquipment() {
        JSONObject highVoltage = model("LogicPower LP LiFePO4 256V 315Ah (40589)");
        assertFalse(catalog.canRecommend(inverter, highVoltage, 3));
        assertFalse(catalog.conflict(inverter, highVoltage, 3).isEmpty());
    }

    @Test
    public void aFeedCannotOverrideTheBundledElectricalLimit() throws Exception {
        JSONObject sellerData = new JSONObject(inverter.toString()).put("max_discharge_a", 500);
        assertFalse(catalog.canRecommend(sellerData, battery, 5));
    }

    @Test
    public void aFamilyOnlyMatchRequiresManualReview() {
        JSONObject other = model("SRNE ASF4880S180-H");
        assertNotNull(catalog.bmsRule(other, battery));
        assertFalse(catalog.bmsRule(other, battery).optBoolean("automatic"));
        assertFalse(catalog.canRecommend(other, battery, 3));
    }

    @Test
    public void newlyApprovedDeyeRevisionSupportsRequestedLoad() {
        JSONObject other = model("Deye SUN-12K-SG05LP3-EU-SM2");
        assertNotNull(catalog.bmsRule(other, battery));
        assertTrue(catalog.bmsRule(other, battery).optBoolean("automatic"));
        assertTrue(catalog.canRecommend(other, battery, 3));
        assertFalse(catalog.canRecommend(other, battery, 12));
    }

    @Test
    public void outOfStockOfferIsExcludedWhileVerifiedAlternativeIsAvailable() {
        assertTrue(OfferSource.matches(offers, "inverter", 48, 3).stream().anyMatch(
                offer -> "Deye SUN-5K-SG03LP1-EU".equals(offer.optString("model"))
                        && "Chastotnik".equals(offer.optString("seller"))));
        assertFalse(OfferSource.matches(offers, "inverter", 48, 3).stream().anyMatch(
                offer -> "Deye SUN-5K-SG03LP1-EU".equals(offer.optString("model"))
                        && "Eagle".equals(offer.optString("seller"))));
    }

    private JSONObject model(String name) {
        for (int index = 0; index < offers.length(); index++) {
            JSONObject offer = offers.optJSONObject(index);
            if (name.equals(offer.optString("model"))) return offer;
        }
        throw new AssertionError("Model missing from catalog: " + name);
    }
}
