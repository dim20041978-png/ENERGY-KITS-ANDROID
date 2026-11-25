package ua.energykits.app;

import android.content.res.AssetManager;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

final class CompatibilityCatalog {
    private final Map<String, JSONObject> equipment = new HashMap<>();
    private final Map<String, JSONObject> pairs = new HashMap<>();

    CompatibilityCatalog(AssetManager assets) {
        this(readAsset(assets, "offers.json"), readAsset(assets, "bms-compatibility.json"));
    }

    private static JSONArray readAsset(AssetManager assets, String name) {
        try (java.io.InputStream input = assets.open(name)) {
            return OfferSource.read(input);
        } catch (IOException | JSONException error) {
            Log.w("CompatibilityCatalog", "Could not read " + name, error);
            return new JSONArray();
        }
    }

    CompatibilityCatalog(JSONArray offers, JSONArray rules) {
        try {
            for (int i = 0; i < offers.length(); i++) {
                JSONObject offer = offers.getJSONObject(i);
                String status = offer.optString("spec_status");
                if ("manufacturer_verified".equals(status) || "manufacturer_partial".equals(status)) {
                    equipment.put(key(offer.optString("kind"), offer.optString("model")), offer);
                }
            }
        } catch (JSONException error) {
            equipment.clear();
            Log.w("CompatibilityCatalog", "Equipment data unavailable", error);
        }

        try {
            for (int i = 0; i < rules.length(); i++) {
                JSONObject rule = rules.getJSONObject(i);
                JSONArray inverters = rule.getJSONArray("inverters");
                JSONArray batteries = rule.getJSONArray("batteries");
                for (int j = 0; j < inverters.length(); j++) {
                    for (int k = 0; k < batteries.length(); k++) {
                        pairs.put(key(inverters.getString(j), batteries.getString(k)), rule);
                    }
                }
            }
        } catch (JSONException error) {
            pairs.clear();
            Log.w("CompatibilityCatalog", "BMS data unavailable", error);
        }
    }

    private static String key(String first, String second) {
        return first + "\n" + second;
    }

    JSONObject specification(JSONObject offer) {
        return equipment.get(key(offer.optString("kind"), offer.optString("model")));
    }

    JSONObject bmsRule(JSONObject inverter, JSONObject battery) {
        return pairs.get(key(inverter.optString("model"), battery.optString("model")));
    }

    String conflict(JSONObject inverter, JSONObject battery, double loadKw) {
        JSONObject inverterSpec = specification(inverter);
        JSONObject batterySpec = specification(battery);
        JSONObject i = inverterSpec == null ? inverter : inverterSpec;
        JSONObject b = batterySpec == null ? battery : batterySpec;
        if (b.optBoolean("requires_hv_system") ||
                CompatibilityMath.voltageConflict(i.optDouble("voltage"), b.optDouble("voltage"),
                        i.optDouble("dc_min"), i.optDouble("dc_max"),
                        b.optDouble("voltage_min"), b.optDouble("voltage_max"))) {
            return "Несумісна напруга обладнання";
        }
        JSONObject rule = bmsRule(inverter, battery);
        if (rule != null && rule.optInt("min_modules") > 1) {
            return "Для цієї пари виробник вимагає щонайменше " +
                    rule.optInt("min_modules") + " модулі АКБ";
        }
        if (batterySpec != null) {
            double required = CompatibilityMath.requiredCurrent(loadKw, b.optDouble("voltage_min"),
                    b.optDouble("voltage"));
            if (CompatibilityMath.exceedsLimit(required, b.optDouble("max_discharge_a"))) {
                return "Струм розряду АКБ недостатній для заданої потужності";
            }
            if (inverterSpec != null &&
                    CompatibilityMath.exceedsLimit(required, i.optDouble("max_discharge_a"))) {
                return "Інвертор обмежує струм розряду для заданої потужності";
            }
        }
        return "";
    }

    boolean canRecommend(JSONObject inverter, JSONObject battery, double loadKw) {
        JSONObject i = specification(inverter);
        JSONObject b = specification(battery);
        JSONObject rule = bmsRule(inverter, battery);
        if (i == null || b == null || rule == null || rule.optInt("min_modules") != 1 ||
                !rule.optString("source").startsWith("https://") ||
                !rule.optBoolean("automatic") || !conflict(inverter, battery, loadKw).isEmpty()) {
            return false;
        }
        double required = CompatibilityMath.requiredCurrent(loadKw, b.optDouble("voltage_min"),
                b.optDouble("voltage"));
        return i.optDouble("kw") >= loadKw
                && !CompatibilityMath.exceedsLimit(loadKw, i.optDouble("max_battery_discharge_kw"))
                && CompatibilityMath.validRange(i.optDouble("dc_min"), i.optDouble("dc_max"))
                && CompatibilityMath.validRange(b.optDouble("voltage_min"), b.optDouble("voltage_max"))
                && CompatibilityMath.fitsLimit(required, i.optDouble("max_discharge_a"))
                && CompatibilityMath.fitsLimit(required, b.optDouble("max_discharge_a"))
                && CompatibilityMath.positiveFinite(chargeLimit(i, b))
                && CompatibilityMath.positiveFinite(inverter.optDouble("price"))
                && CompatibilityMath.positiveFinite(battery.optDouble("price"))
                && CompatibilityMath.positiveFinite(inverter.optDouble("price") + battery.optDouble("price"));
    }

    static double chargeLimit(JSONObject inverter, JSONObject battery) {
        if (inverter == null || battery == null) return Double.NaN;
        double inverterLimit = inverter.optDouble("max_charge_a");
        double batteryLimit = battery.optDouble("max_charge_a");
        if (!CompatibilityMath.positiveFinite(inverterLimit) ||
                !CompatibilityMath.positiveFinite(batteryLimit)) return Double.NaN;
        return Math.min(inverterLimit, batteryLimit);
    }
}
