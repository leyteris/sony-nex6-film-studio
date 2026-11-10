package com.filmstudio.nex6;

import java.util.Locale;

/**
 * The 17 Film Studio looks, expressed only with parameters the NEX-6 camera
 * layer actually accepts. NEX-6 firmware has no RGB matrix and no extended
 * gamma table, so this is a parameter-level approximation, not a LUT port.
 */
final class Looks {

    static final class Look {
        final String tag;
        final String name;
        final int accent;
        final String style;
        final int sat;
        final int con;
        final int sharp;
        final String wb;       // "auto" or "kelvin"
        final int kelvin;
        final int ab;          // amber positive, -7..+7
        final int gm;          // green positive, -7..+7
        final String dro;      // "auto" or "off"
        final String effect;   // mono Picture Effect, or null for "off"

        Look(String tag, String name, int accent, String style,
             int sat, int con, int sharp, String wb, int kelvin, int ab, int gm, String dro) {
            this(tag, name, accent, style, sat, con, sharp, wb, kelvin, ab, gm, dro, null);
        }

        Look(String tag, String name, int accent, String style,
             int sat, int con, int sharp, String wb, int kelvin, int ab, int gm, String dro,
             String effect) {
            this.tag = tag;
            this.name = name;
            this.accent = accent;
            this.style = style;
            this.sat = sat;
            this.con = con;
            this.sharp = sharp;
            this.wb = wb;
            this.kelvin = kelvin;
            this.ab = ab;
            this.gm = gm;
            this.dro = dro;
            this.effect = effect;
        }

        String wbLabel() {
            if ("kelvin".equals(wb)) {
                String s = kelvin + "K";
                if (ab != 0) s += (ab > 0 ? " A+" : " B-") + Math.abs(ab);
                if (gm != 0) s += (gm > 0 ? " G+" : " M-") + Math.abs(gm);
                return s;
            }
            return "AWB";
        }

        String paramLine() {
            return style.toUpperCase(Locale.US) + "  S" + signed(sat) + " C" + signed(con)
                    + " R" + signed(sharp) + "  " + wbLabel() + "  DRO " + dro;
        }
    }

    private static String signed(int v) {
        return v >= 0 ? "+" + v : String.valueOf(v);
    }

    static final Look[] ALL = new Look[] {
            new Look("PROVIA", "Fuji PROVIA Standard", 0xFFB8C4B0, "standard", +1, 0, 0, "auto", 0, 0, 0, "auto"),
            new Look("VELVIA", "Fuji Velvia Vivid", 0xFFC94F3D, "vivid", +2, +1, +1, "auto", 0, 0, 0, "auto"),
            new Look("ASTIA", "Fuji ASTIA Soft", 0xFFD8A87A, "portrait", 0, -1, -1, "auto", 0, 0, 0, "auto"),
            new Look("CHROME", "Fuji Classic Chrome", 0xFF8A9AA8, "neutral", -1, +1, 0, "kelvin", 5600, +1, 0, "auto"),
            new Look("REALA", "Fuji REALA ACE", 0xFFE0B64A, "standard", +1, +1, 0, "auto", 0, 0, 0, "auto"),
            new Look("PRONEG", "Fuji PRO Neg. Std", 0xFF9FAE9A, "neutral", -1, -1, 0, "auto", 0, 0, 0, "auto"),
            new Look("CNEG", "Fuji Classic Negative", 0xFF7A6A58, "neutral", -1, +1, 0, "kelvin", 5400, +2, 0, "auto"),
            new Look("ETERNA", "Fuji ETERNA Cinema", 0xFF6E7F86, "neutral", -2, -1, -1, "auto", 0, 0, 0, "auto"),
            new Look("BLEACH", "Fuji ETERNA Bleach", 0xFFB9B4A6, "neutral", -3, +2, +1, "auto", 0, 0, 0, "off"),
            new Look("ACROS", "Fuji ACROS Mono", 0xFFBFBFBF, "mono", 0, +1, +1, "auto", 0, 0, 0, "auto", "richtone-mono"),
            new Look("GR+", "Ricoh GR Positive", 0xFF7FA36B, "vivid", +1, +1, +1, "auto", 0, 0, 0, "auto"),
            new Look("GR-", "Ricoh Negative Film", 0xFF8C7A63, "neutral", -1, +1, 0, "kelvin", 5600, +1, 0, "auto"),
            new Look("HCBW", "Ricoh Hi-Contrast B&W", 0xFF9E9E9E, "mono", 0, +3, +2, "auto", 0, 0, 0, "off", "rough-mono"),
            new Look("MORI", "Ricoh Moriyama Mono", 0xFF6B6B6B, "mono", 0, +3, +3, "auto", 0, 0, 0, "off", "rough-mono"),
            new Look("CROSS", "Ricoh Cross Process", 0xFFB07A9E, "vivid", +3, +2, 0, "kelvin", 5000, +2, 0, "off"),
            new Look("LEICA-C", "Leica Look Classic", 0xFFB08A6A, "neutral", -1, -1, 0, "auto", 0, 0, 0, "auto"),
            new Look("LEICA-N", "Leica Look Natural", 0xFF9AA58E, "standard", 0, 0, 0, "auto", 0, 0, 0, "auto")
    };

    static final int[] STRENGTH = new int[] { 30, 50, 70, 100 };

    private Looks() {}

    static Look atStrength(Look look, int pct) {
        if (pct >= 100) return look;
        return new Look(look.tag, look.name, look.accent, look.style,
                scale(look.sat, pct), scale(look.con, pct), scale(look.sharp, pct),
                look.wb, look.kelvin, scale(look.ab, pct), scale(look.gm, pct), look.dro, look.effect);
    }

    private static int scale(int value, int pct) {
        if (value == 0) return 0;
        return Math.round(value * pct / 100.0f);
    }
}
