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
        if (!ParallelBatteryPlanner.systemMatches(i, b, bmsRule(inverter, battery)) ||
                CompatibilityMath.voltageConflict(i.optDouble("voltage"), b.optDouble("voltage"),
                        i.optDouble("dc_min"), i.optDouble("dc_max"),
                        b.optDouble("voltage_min"), b.optDouble("voltage_max"))) {
            return "Несумісна напруга обладнання";
        }
        JSONObject rule = bmsRule(inverter, battery);
        if (rule != null && rule.optInt("min_modules") > modules(battery)) {
            return "Для цієї пари виробник вимагає щонайменше " +
                    rule.optInt("min_modules") + " модулі АКБ";
        }
        if ((modules(battery) > 1 || b.optBoolean("requires_hv_system")) && !canUseBank(inverter, battery, loadKw)) {
            return "Кількість модулів або паспортні параметри банку АКБ не підтверджені";
        }
        if (batterySpec != null) {
            double required = CompatibilityMath.requiredCurrent(loadKw, b.optDouble("voltage_min"),
                    b.optDouble("voltage"));
            if (CompatibilityMath.exceedsLimit(required, b.optDouble("max_discharge_a") * modules(battery))) {
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
        if (i == null || b == null || rule == null || rule.optInt("min_modules") < 1 ||
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
                && CompatibilityMath.fitsLimit(required, b.optDouble("max_discharge_a") * modules(battery))
                && CompatibilityMath.positiveFinite(chargeLimit(i, b))
                && CompatibilityMath.positiveFinite(inverter.optDouble("price"))
                && CompatibilityMath.positiveFinite(battery.optDouble("price"))
                && CompatibilityMath.positiveFinite(inverter.optDouble("price") + battery.optDouble("price"));
    }

    static int modules(JSONObject battery) { return battery.optInt("_kit_modules", 1); }

    int minimumModules(JSONObject inverter, JSONObject battery, double loadKw, double minAh) {
        return ParallelBatteryPlanner.minimumModules(specification(inverter), specification(battery),
                bmsRule(inverter, battery), loadKw, minAh);
    }

    boolean canUseBank(JSONObject inverter, JSONObject battery, double loadKw) {
        int n = modules(battery);
        JSONObject rule = bmsRule(inverter, battery);
        int minimum = minimumModules(inverter, battery, loadKw, 0);
        return n >= 1 && minimum >= 1 && n >= minimum && rule != null &&
                (n == 1 || (!"HV".equals(rule.optString("system")) && rule.optString("parallel_source").startsWith("https://") &&
                 n <= Math.min(64, rule.optInt("max_parallel_modules", 1))));
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
