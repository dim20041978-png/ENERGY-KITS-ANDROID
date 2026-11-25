package ua.energykits.app;

/** Estimates, not a certification of starting current or appliance compatibility. */
final class ApplianceUsage {
    static final String[] NAMES = {"Холодильник","Телевізор","Wi-Fi роутер","Освітлення LED",
            "Газовий котел","Ноутбук","Пральна машина","Мікрохвильова піч","Насосна станція",
            "Електрочайник","Морозильна камера","Комп’ютер","Кондиціонер","Бойлер","Праска"};
    static final double[] WATTS = {150,100,15,100,150,65,2000,1200,750,2000,150,300,1000,2000,2000};
    static final double[] HOURS = {8,4,24,5,8,4,1,0.25,1,0.25,8,4,4,2,0.5};
    static final boolean[] START_CHECK = {true,false,false,false,true,false,true,true,true,false,true,false,true,false,false};
    static final double USABLE = 0.9, EFFICIENCY = 0.9, IDLE_W = 30;
    private ApplianceUsage() {}

    static double parse(String value, boolean hours) {
        try {
            double n = Double.parseDouble(value.trim().replace(',', '.'));
            if (Double.isNaN(n) || Double.isInfinite(n) || n < 0 ||
                    (hours ? n > 24 : n <= 0 || n > 100000)) return Double.NaN;
            return n;
        } catch (RuntimeException e) { return Double.NaN; }
    }

    static double dailyWh(double watts, double hours) {
        return watts * hours;
    }

    static double runtimeHours(double nominalWh, double watts) {
        if (!CompatibilityMath.positiveFinite(nominalWh) || !CompatibilityMath.positiveFinite(watts))
            return Double.NaN;
        return nominalWh * USABLE / (watts / EFFICIENCY + IDLE_W);
    }

    static double dailyBatteryWh(double acWh) {
        return acWh / EFFICIENCY + IDLE_W * 24;
    }
}
