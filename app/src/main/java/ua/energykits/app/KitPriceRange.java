package ua.energykits.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.json.JSONObject;

/** Inclusive price filtering in kopecks, matching the displayed two-decimal total. */
final class KitPriceRange {
    private KitPriceRange() {}
    static long total(JSONObject[] pair) {
        double a = pair[0].optDouble("price"), b = pair[1].optDouble("price");
        if (!CompatibilityMath.positiveFinite(a) || !CompatibilityMath.positiveFinite(b)) return -1;
        try { return BigDecimal.valueOf(a+b).movePointRight(2).setScale(0,RoundingMode.HALF_UP).longValueExact(); }
        catch (RuntimeException ex) { return -1; }
    }
    static long parse(String text) {
        try {
            String clean=text.trim().replace(',', '.');
            if (!clean.matches("[0-9]+(\\.[0-9]{1,2})?")) return -1;
            return new BigDecimal(clean).movePointRight(2).longValueExact();
        } catch (RuntimeException ex) { return -1; }
    }
    static String format(long cents) {
        return BigDecimal.valueOf(cents,2).stripTrailingZeros().toPlainString().replace('.',',');
    }
    static boolean includes(long price,long lower,long upper) {
        return price>=0 && lower>=0 && upper>=lower && price>=lower && price<=upper;
    }
    static long position(long min,long max,float fraction) {
        if (fraction<=0) return min;
        if (fraction>=1) return max;
        if (max<=min) return min;
        return min+Math.round((max-min)*(double)fraction);
    }
}
