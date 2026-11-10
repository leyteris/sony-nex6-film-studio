package com.filmstudio.nex6;

import android.content.Context;
import android.graphics.Typeface;

import java.util.Locale;

/**
 * UI language. The firmware font has no CJK glyphs, so Chinese is drawn with the
 * subset Noto Sans fonts in assets/fonts; every other language keeps the camera's
 * own font, as Recipe Lab does.
 */
final class Lang {
    private static final int EN = 0, SC = 1, TC = 2;

    private static final String[] FONT = {
            null,
            "fonts/NotoSansSC-UI.ttf",
            "fonts/NotoSansTC-UI.ttf",
    };
    private static final Typeface[] LOADED = new Typeface[3];
    private static final int SCRIPT = detect();

    private Lang() {}

    static String code() {
        return SCRIPT == SC ? "zh-CN" : SCRIPT == TC ? "zh-TW" : "en";
    }

    private static int detect() {
        try {
            Locale l = Locale.getDefault();
            if (l == null) return EN;
            if ("zh".equals(l.getLanguage())) {
                String c = l.getCountry();
                if ("TW".equals(c) || "HK".equals(c) || "MO".equals(c)) return TC;
                return SC;
            }
        } catch (Throwable ignored) {
        }
        return EN;
    }

    static Typeface typeface(Context c) {
        if (SCRIPT == EN) return Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
        int i = SCRIPT;
        if (LOADED[i] == null) {
            try {
                LOADED[i] = Typeface.createFromAsset(c.getAssets(), FONT[i]);
                Log8.event("FONT", FONT[i] + " ok script=" + code());
            } catch (Throwable t) {
                LOADED[i] = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
                Log8.event("FONT-FAIL", FONT[i] + " " + Log8.stack(t));
            }
        }
        return LOADED[i];
    }

    static String strength() {
        if (SCRIPT == SC) return "强度";
        if (SCRIPT == TC) return "強度";
        return "STRENGTH";
    }

    static String hints() {
        if (SCRIPT == SC) return "左右:风格  上下:强度  中央:拍摄  DEL:界面  MENU:退出";
        if (SCRIPT == TC) return "左右:風格  上下:強度  中央:拍攝  DEL:介面  MENU:離開";
        return "LEFT/RIGHT: look  UP/DOWN: strength  CENTER: shoot  DEL: UI  MENU: exit";
    }

    /** The dial stays on P/A/S/M: iAuto and SCN hand the look back to the
     *  camera, which drops Creative Style and Picture Effect. */
    static String modeHint() {
        if (SCRIPT == SC) return "P/A/S/M:风格";
        if (SCRIPT == TC) return "P/A/S/M:風格";
        return "P/A/S/M: STYLE";
    }

    static String captured() {
        if (SCRIPT == SC) return "已拍摄";
        if (SCRIPT == TC) return "已拍攝";
        return "CAPTURED";
    }

    static String logHint() {
        if (SCRIPT == SC) return "DISP:关闭  上下:翻页";
        if (SCRIPT == TC) return "DISP:關閉  上下:翻頁";
        return "DISP: close   UP/DOWN: scroll";
    }

    /** Full look name for the top bar. English keeps the existing tag + name. */
    static String lookName(Looks.Look look) {
        if (SCRIPT == EN) return look.tag + "  " + look.name;
        String n = lookName(look.tag);
        return n == null ? look.name : n;
    }

    private static String lookName(String tag) {
        if ("PROVIA".equals(tag)) return t("富士 PROVIA 标准", "富士 PROVIA 標準");
        if ("VELVIA".equals(tag)) return t("富士 Velvia 鲜艳", "富士 Velvia 鮮豔");
        if ("ASTIA".equals(tag)) return t("富士 ASTIA 柔和", "富士 ASTIA 柔和");
        if ("CHROME".equals(tag)) return t("富士 经典正片", "富士 經典正片");
        if ("REALA".equals(tag)) return "富士 REALA ACE";
        if ("PRONEG".equals(tag)) return "富士 PRO Neg. Std";
        if ("CNEG".equals(tag)) return t("富士 经典负片", "富士 經典負片");
        if ("ETERNA".equals(tag)) return t("富士 ETERNA 电影", "富士 ETERNA 電影");
        if ("BLEACH".equals(tag)) return t("富士 ETERNA 漂白", "富士 ETERNA 漂白");
        if ("ACROS".equals(tag)) return t("富士 ACROS 黑白", "富士 ACROS 黑白");
        if ("GR+".equals(tag)) return t("理光 正片", "理光 正片");
        if ("GR-".equals(tag)) return t("理光 负片", "理光 負片");
        if ("HCBW".equals(tag)) return t("理光 高反差黑白", "理光 高反差黑白");
        if ("MORI".equals(tag)) return t("理光 森山黑白", "理光 森山黑白");
        if ("CROSS".equals(tag)) return t("理光 正负逆冲", "理光 正負逆沖");
        if ("LEICA-C".equals(tag)) return t("徕卡 经典", "徠卡 經典");
        if ("LEICA-N".equals(tag)) return t("徕卡 自然", "徠卡 自然");
        return null;
    }

    /** Compact look label for the pill and the look-change flash: English keeps
     *  the tag, Chinese adds the short name so the minimal stages stay readable. */
    static String compactName(Looks.Look look) {
        if (SCRIPT == EN) return look.tag;
        String s = lookShort(look.tag);
        if (s == null || s.equals(look.tag)) return look.tag;
        return look.tag + " " + s;
    }

    /** Short label under the card tag; null means "keep the index number". */
    static String lookShort(String tag) {
        if (SCRIPT == EN) return null;
        if ("PROVIA".equals(tag)) return t("标准", "標準");
        if ("VELVIA".equals(tag)) return t("鲜艳", "鮮豔");
        if ("ASTIA".equals(tag)) return t("柔和", "柔和");
        if ("CHROME".equals(tag)) return t("经典正片", "經典正片");
        if ("REALA".equals(tag)) return "REALA";
        if ("PRONEG".equals(tag)) return "Neg.Std";
        if ("CNEG".equals(tag)) return t("经典负片", "經典負片");
        if ("ETERNA".equals(tag)) return t("电影", "電影");
        if ("BLEACH".equals(tag)) return t("漂白", "漂白");
        if ("ACROS".equals(tag)) return t("黑白", "黑白");
        if ("GR+".equals(tag)) return t("正片", "正片");
        if ("GR-".equals(tag)) return t("负片", "負片");
        if ("HCBW".equals(tag)) return t("高反差", "高反差");
        if ("MORI".equals(tag)) return t("森山", "森山");
        if ("CROSS".equals(tag)) return t("逆冲", "逆沖");
        if ("LEICA-C".equals(tag)) return t("经典", "經典");
        if ("LEICA-N".equals(tag)) return t("自然", "自然");
        return null;
    }

    static String shutterFailed() {
        if (SCRIPT == SC) return "快门失败";
        if (SCRIPT == TC) return "快門失敗";
        return "SHUTTER FAILED";
    }

    /** The camera layer reports in English; only the label is translated so the
     *  exception text stays intact for diagnosis. */
    static String status(String s) {
        if (s == null || s.length() == 0 || SCRIPT == EN) return s;
        if (s.equals("camera not ready")) return t("相机未就绪", "相機未就緒");
        if (s.startsWith("camera open failed")) return t("相机打开失败", "相機開啟失敗") + detail(s);
        if (s.startsWith("live view")) return t("实时取景", "即時取景") + detail(s);
        if (s.startsWith("parameters rejected")) return t("参数被拒绝", "參數被拒絕") + detail(s);
        if (s.startsWith("shutter failed")) return t("快门失败", "快門失敗") + detail(s);
        return s;
    }

    private static String t(String sc, String tc) {
        return SCRIPT == SC ? sc : tc;
    }

    private static String detail(String s) {
        int i = s.indexOf(": ");
        return i < 0 ? "" : s.substring(i);
    }
}
