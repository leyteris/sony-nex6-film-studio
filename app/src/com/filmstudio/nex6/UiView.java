package com.filmstudio.nex6;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.View;

/** Canvas overlay on top of the live view: no XML, no framework resources.
 *  Three stages, like Recipe Lab: the full panel, a small pill, nothing. */
final class UiView extends View {
    static final int MODE_FULL = 0, MODE_PILL = 1, MODE_HIDDEN = 2;
    static final int FOCUS_OFF = 0, FOCUS_WORKING = 1, FOCUS_LOCKED = 2, FOCUS_FAILED = 3;

    private static final int PANEL = 0xB4000000;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int DIM = 0xFFB8B8B8;
    private static final int ACCENT = 0xFFE8C87A;
    private static final int WARN = 0xFFFFB060;
    private static final int CARD = 0x33FFFFFF;
    private static final int CARD_EDGE = 0x66FFFFFF;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Rect glyphBounds = new Rect();
    private int index;
    private int strengthIndex = 3;
    private int mode = MODE_FULL;
    private int focusState = FOCUS_OFF;
    private String status = "";
    private String flash = "";
    private long flashUntil;

    UiView(Context context) {
        super(context);
        setFocusable(false);
        paint.setTypeface(Lang.typeface(context));
    }

    void setState(int lookIndex, int strength, String status) {
        index = lookIndex;
        strengthIndex = strength;
        this.status = status == null ? "" : status;
        invalidate();
    }

    void setMode(int m) {
        if (mode == m) return;
        mode = m;
        invalidate();
    }

    void setFocus(int state) {
        if (focusState == state) return;
        focusState = state;
        invalidate();
    }

    void showFlash(String text) {
        flash = text;
        flashUntil = SystemClock.uptimeMillis() + 1400;
        postInvalidate();
        postInvalidateDelayed(1500);
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        if (mode == MODE_PILL) drawPill(c, w, h, Looks.ALL[index]);
        if (mode == MODE_FULL) {
        int title = size(h, 0.036f);
        int body = size(h, 0.027f);
        int small = size(h, 0.022f);

        int topH = size(h, 0.17f);
        rect.set(0, 0, w, topH);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(PANEL);
        c.drawRect(rect, paint);

        Looks.Look look = Looks.ALL[index];

        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(WHITE);
        paint.setTextSize(title);
        c.drawText("FILM STUDIO NEX-6", size(w, 0.022f), topH * 0.30f, paint);

        paint.setColor(ACCENT);
        paint.setTextSize(body);
        c.drawText(Lang.lookName(look), size(w, 0.022f), topH * 0.60f, paint);

        paint.setColor(DIM);
        paint.setTextSize(small);
        c.drawText(look.paramLine(), size(w, 0.022f), topH * 0.86f, paint);

        if (status.length() > 0) {
            paint.setColor(WARN);
            paint.setTextAlign(Paint.Align.RIGHT);
            c.drawText(clip(Lang.status(status), 52), w - size(w, 0.022f), topH * 0.30f, paint);
            paint.setTextAlign(Paint.Align.LEFT);
        }

        int panelH = size(h, 0.29f);
        int panelTop = h - panelH;
        rect.set(0, panelTop, w, h);
        paint.setColor(PANEL);
        c.drawRect(rect, paint);

        int pad = size(w, 0.018f);
        int chipTop = panelTop + size(h, 0.008f);
        int chipH = size(h, 0.042f);
        int chipW = size(w, 0.105f);
        paint.setTextSize(small);
        paint.setColor(DIM);
        paint.setTextAlign(Paint.Align.LEFT);
        c.drawText(Lang.strength(), pad, chipTop + chipH * 0.68f, paint);

        int chipX = pad + (int) paint.measureText(Lang.strength()) + size(w, 0.012f);
        for (int i = 0; i < Looks.STRENGTH.length; i++) {
            boolean on = i == strengthIndex;
            rect.set(chipX, chipTop, chipX + chipW, chipTop + chipH);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(on ? ACCENT : 0x22FFFFFF);
            c.drawRoundRect(rect, size(h, 0.008f), size(h, 0.008f), paint);
            if (!on) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(CARD_EDGE);
                c.drawRoundRect(rect, size(h, 0.008f), size(h, 0.008f), paint);
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(on ? 0xFF16110A : WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            c.drawText(Looks.STRENGTH[i] + "%", rect.centerX(), rect.centerY() + small * 0.36f, paint);
            chipX += chipW + size(w, 0.008f);
        }

        int cardTop = panelTop + size(h, 0.086f);
        int cardGap = cardTop - (chipTop + chipH);
        int hintH = size(h, 0.076f);
        int cardH = panelTop + panelH - hintH - cardTop;
        paint.setTextSize(size(h, 0.021f));
        paint.getTextBounds("H", 0, 1, glyphBounds);
        int hintBaseline = cardTop + cardH + cardGap - glyphBounds.top;
        int gap = size(w, 0.008f);
        int cardW = (w - pad * 2 - gap * 4) / 5;
        int start = index - 2;
        if (start < 0) start = 0;
        if (start > Looks.ALL.length - 5) start = Looks.ALL.length - 5;

        for (int i = 0; i < 5; i++) {
            int at = start + i;
            Looks.Look l = Looks.ALL[at];
            int x = pad + i * (cardW + gap);
            rect.set(x, cardTop, x + cardW, cardTop + cardH);
            boolean on = at == index;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(on ? ACCENT : CARD);
            c.drawRoundRect(rect, size(h, 0.010f), size(h, 0.010f), paint);
            if (!on) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(CARD_EDGE);
                c.drawRoundRect(rect, size(h, 0.010f), size(h, 0.010f), paint);
            }
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(l.accent);
            rect.set(x + gap, cardTop + gap, x + cardW - gap, cardTop + gap + size(h, 0.008f));
            c.drawRect(rect, paint);

            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(on ? 0xFF16110A : WHITE);
            paint.setTextSize(size(h, 0.028f));
            c.drawText(l.tag, x + cardW / 2f, cardTop + cardH * 0.54f, paint);
            paint.setColor(on ? 0xFF4A3E24 : DIM);
            paint.setTextSize(size(h, 0.018f));
            String shortName = Lang.lookShort(l.tag);
            c.drawText(shortName != null ? shortName : two(at + 1),
                    x + cardW / 2f, cardTop + cardH * 0.82f, paint);
        }

        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(DIM);
        paint.setTextSize(size(h, 0.021f));
        c.drawText(Lang.hints(), pad, hintBaseline, paint);
        }

        drawFocus(c, w, h);

        if (SystemClock.uptimeMillis() < flashUntil) {
            int fs = size(h, 0.040f);
            paint.setTextSize(fs);
            float tw = paint.measureText(flash);
            rect.set(w / 2f - tw / 2f - size(w, 0.03f), h * 0.20f,
                    w / 2f + tw / 2f + size(w, 0.03f), h * 0.20f + fs * 1.8f);
            paint.setColor(0xE0000000);
            c.drawRoundRect(rect, size(h, 0.012f), size(h, 0.012f), paint);
            paint.setColor(ACCENT);
            paint.setTextAlign(Paint.Align.CENTER);
            c.drawText(flash, w / 2f, rect.centerY() + fs * 0.36f, paint);
        }
    }

    /** The body's own AF point is not exposed to PMCA apps, so this is an
     *  app-drawn S1 indicator centered on the frame, not the camera's area. */
    private void drawFocus(Canvas c, int w, int h) {
        if (focusState == FOCUS_OFF) return;
        int color = focusState == FOCUS_LOCKED ? 0xFF74E08A
                : focusState == FOCUS_FAILED ? 0xFFFF6A5A : WHITE;
        float fw = w * 0.22f;
        float fh = fw * 0.68f;
        float left = (w - fw) / 2f;
        float top = (h - fh) / 2f;
        float right = left + fw;
        float bottom = top + fh;
        float arm = fw * 0.26f;
        float stroke = Math.max(2f, h * 0.0045f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke + 2f);
        paint.setColor(0x66000000);
        brackets(c, left, top, right, bottom, arm);
        paint.setStrokeWidth(stroke);
        paint.setColor(color);
        brackets(c, left, top, right, bottom, arm);
        paint.setStyle(Paint.Style.FILL);
    }

    private void brackets(Canvas c, float left, float top, float right, float bottom, float arm) {
        c.drawLine(left, top, left + arm, top, paint);
        c.drawLine(left, top, left, top + arm, paint);
        c.drawLine(right, top, right - arm, top, paint);
        c.drawLine(right, top, right, top + arm, paint);
        c.drawLine(left, bottom, left + arm, bottom, paint);
        c.drawLine(left, bottom, left, bottom - arm, paint);
        c.drawLine(right, bottom, right - arm, bottom, paint);
        c.drawLine(right, bottom, right, bottom - arm, paint);
    }

    private void drawPill(Canvas c, int w, int h, Looks.Look look) {
        int small = size(h, 0.021f);
        int fs = size(h, 0.027f);
        String text = Lang.compactName(look) + "  " + Looks.STRENGTH[strengthIndex] + "%";
        paint.setTextSize(fs);
        paint.setTextAlign(Paint.Align.LEFT);
        float tw = paint.measureText(text);
        float pad = size(w, 0.016f);
        float pillH = size(h, 0.062f);
        float bottom = h - size(h, 0.025f);
        float left = size(w, 0.016f);
        rect.set(left, bottom - pillH, left + tw + pad * 2, bottom);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(PANEL);
        c.drawRoundRect(rect, pillH / 2f, pillH / 2f, paint);
        paint.setColor(ACCENT);
        c.drawText(text, rect.left + pad, rect.centerY() + fs * 0.36f, paint);

        if (status.length() > 0) {
            paint.setColor(WARN);
            paint.setTextSize(small);
            paint.setTextAlign(Paint.Align.LEFT);
            c.drawText(clip(Lang.status(status), 52), rect.left + pad, rect.top - small * 0.6f, paint);
        }
    }

    private static int size(int total, float fraction) {
        int v = (int) (total * fraction);
        return v < 9 ? 9 : v;
    }

    private static String two(int n) {
        return n < 10 ? "0" + n : String.valueOf(n);
    }

    private static String clip(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "~";
    }
}
