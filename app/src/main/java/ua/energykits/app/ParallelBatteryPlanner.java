package ua.energykits.app;

import org.json.JSONObject;

/** Conservative DC screening; the installation still requires a qualified system design. */
final class ParallelBatteryPlanner {
    private ParallelBatteryPlanner() {}

    static int minimumModules(JSONObject inverter, JSONObject battery, JSONObject rule,
                              double loadKw, double minimumAh) {
        if (inverter == null || battery == null || rule == null ||
                !rule.optBoolean("automatic") || !rule.optString("source").startsWith("https://") ||
                !CompatibilityMath.positiveFinite(loadKw) ||
                Double.isNaN(minimumAh) || Double.isInfinite(minimumAh) || minimumAh < 0 ||
                inverter.optDouble("kw") < loadKw ||
                !CompatibilityMath.positiveFinite(inverter.optDouble("kw")) ||
                CompatibilityMath.exceedsLimit(loadKw, inverter.optDouble("max_battery_discharge_kw")) ||
                battery.optBoolean("requires_hv_system") ||
                !CompatibilityMath.validRange(inverter.optDouble("dc_min"), inverter.optDouble("dc_max")) ||
                !CompatibilityMath.validRange(battery.optDouble("voltage_min"), battery.optDouble("voltage_max")) ||
                CompatibilityMath.voltageConflict(inverter.optDouble("voltage"), battery.optDouble("voltage"),
                        inverter.optDouble("dc_min"), inverter.optDouble("dc_max"),
                        battery.optDouble("voltage_min"), battery.optDouble("voltage_max"))) return 0;
        double required = CompatibilityMath.requiredCurrent(loadKw, battery.optDouble("voltage_min"),
                battery.optDouble("voltage"));
        double discharge = battery.optDouble("max_discharge_a");
        double ah = battery.optDouble("ah");
        if (!CompatibilityMath.fitsLimit(required, inverter.optDouble("max_discharge_a")) ||
                !CompatibilityMath.positiveFinite(discharge) ||
                !CompatibilityMath.positiveFinite(ah) ||
                !CompatibilityMath.positiveFinite(CompatibilityCatalog.chargeLimit(inverter, battery))) return 0;
        int min = rule.optInt("min_modules");
        if (min < 1) return 0;
        int max = rule.optString("parallel_source").startsWith("https://")
                ? Math.min(64, rule.optInt("max_parallel_modules", 1)) : 1;
        // The existing current safety margin applies to the whole battery bank.
        double n = Math.max(min, Math.max(Math.ceil(required / discharge), Math.ceil(minimumAh / ah)));
        return n <= max ? (int) n : 0;
    }
}
