package com.filmstudio.nex6;

import android.hardware.Camera;
import android.os.Handler;
import android.view.SurfaceHolder;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Camera access through Sony's CameraEx when it exists, with a plain
 * android.hardware.Camera fallback. All calls stay on the main thread, the
 * same threading model Recipe Lab uses.
 */
final class Cam {
    interface FocusListener {
        void onFocusDone(boolean success);
    }

    private static final String[] KEYS = {
            "color-mode", "saturation", "contrast", "sharpness",
            "whitebalance", "color-temperture-white-balance",
            "light-balance-for-white-balance", "color-compensation-for-white-balance",
            "dro-mode", "dro-level", "storage-fmt", "jpeg-quality", "picture-effect"
    };

    /** Runtime names a mono Creative Style may carry across Sony firmwares. */
    private static final String[] MONO_ALIASES = {
            "mono", "bw", "blackwhite", "black-white", "monochrome", "monotone"
    };

    /** Picture Effect spellings of Sony's two monochrome effects. The effect
     *  drives the image independently of Creative Style, so it is the second
     *  path to a grayscale frame on a body that drops color-mode=mono. */
    private static final String[] RICHTONE_ALIASES = {
            "richtone-mono", "rich-tone-monochrome", "rich-tone-mono", "richtone-monochrome"
    };
    private static final String[] ROUGH_ALIASES = {
            "rough-mono", "high-contrast-mono", "highcontrast-mono", "rough-monochrome", "hc-mono"
    };

    private final Handler handler;
    private SurfaceHolder holder;
    private Object cameraEx;
    private Camera camera;
    private final Map<String, String> original = new LinkedHashMap<String, String>();
    private boolean originalCaptured;
    private boolean previewing;
    private boolean sizeLogged;
    private int retry;
    private String lastError = "";
    private String note = "";
    private Runnable onPreviewStarted;
    private FocusListener focusListener;
    private int focusGeneration;

    private final Runnable startTick = new Runnable() {
        public void run() {
            attemptStart();
        }
    };

    private final Runnable resumeTick = new Runnable() {
        public void run() {
            attemptResume();
        }
    };

    Cam(Handler handler) {
        this.handler = handler;
    }

    void setHolder(SurfaceHolder h) {
        holder = h;
    }

    boolean isOpen() {
        return camera != null;
    }

    String lastError() {
        return lastError;
    }

    /** What the camera actually kept after the last apply, for the on-screen
     *  corner readout: the SCorner cannot reach the card log reliably. */
    String lastNote() {
        return note;
    }

    boolean open() {
        release();
        lastError = "";
        note = "";
        try {
            Class<?> cx = Class.forName("com.sony.scalar.hardware.CameraEx");
            Class<?> opts = Class.forName("com.sony.scalar.hardware.CameraEx$OpenOptions");
            Object ex = cx.getMethod("open", int.class, opts).invoke(null, Integer.valueOf(0), null);
            camera = (Camera) cx.getMethod("getNormalCamera").invoke(ex);
            cameraEx = ex;
            Log8.event("OPEN", "CameraEx");
        } catch (Throwable t) {
            Log8.failure("OPEN-CAMERAEX", t);
            try {
                camera = Camera.open();
                cameraEx = null;
                Log8.event("OPEN", "android.hardware.Camera");
            } catch (Throwable t2) {
                lastError = "camera open failed: " + t2;
                Log8.failure("OPEN-FAIL", t2);
                return false;
            }
        }
        originalCaptured = false;
        captureOriginal();
        logCapabilities();
        return true;
    }

    private void logCapabilities() {
        try {
            Camera.Parameters p = camera.getParameters();
            Log8.event("CAP", "color-mode=" + p.get("color-mode-values"));
            Log8.event("CAP", "picture-effect=" + p.get("picture-effect-values"));
            Log8.event("CAP", "whitebalance=" + p.get("whitebalance-values"));
            Log8.event("CAP", "rgb-matrix-mode=" + p.get("rgb-matrix-mode"));
        } catch (Throwable t) {
            Log8.failure("CAP-FAIL", t);
        }
    }

    private void captureOriginal() {
        original.clear();
        if (camera == null) return;
        try {
            Camera.Parameters p = camera.getParameters();
            for (int i = 0; i < KEYS.length; i++) {
                String k = KEYS[i];
                try {
                    original.put(k, p.get(k));
                } catch (Throwable ignored) {
                }
            }
            originalCaptured = true;
            Log8.event("ORIGINAL", "captured " + original.size());
        } catch (Throwable t) {
            Log8.failure("ORIGINAL-FAIL", t);
        }
    }

    void startPreview() {
        startPreview(null);
    }

    /** Runs on the main thread once live view is actually up, so parameter
     *  writes after a capture never race a stopped preview. */
    void startPreview(Runnable onStarted) {
        if (camera == null) return;
        onPreviewStarted = onStarted;
        handler.removeCallbacks(startTick);
        handler.removeCallbacks(resumeTick);
        retry = 0;
        attemptStart();
    }

    /** After a capture Recipe Lab calls startPreview() only: the display is
     *  already bound, and setting it again can fail on a body that has already
     *  restarted the preview by itself. */
    void resumePreview(Runnable onStarted) {
        if (camera == null) return;
        onPreviewStarted = onStarted;
        handler.removeCallbacks(startTick);
        handler.removeCallbacks(resumeTick);
        retry = 0;
        attemptResume();
    }

    private void attemptResume() {
        if (camera == null) return;
        try {
            camera.startPreview();
            previewing = true;
            if (retry > 0) Log8.event("PREVIEW", "resumed after " + retry * 250 + " ms");
            previewStarted();
        } catch (Throwable t) {
            retry++;
            if (retry < 24) {
                handler.postDelayed(resumeTick, 250);
            } else {
                previewing = false;
                onPreviewStarted = null;
                lastError = "live view: " + t;
                Log8.failure("PREVIEW-FAIL", t);
            }
        }
    }

    private void attemptStart() {
        if (camera == null) return;
        try {
            if (holder != null) camera.setPreviewDisplay(holder);
            camera.startPreview();
            previewing = true;
            if (!sizeLogged) {
                try {
                    Camera.Size s = camera.getParameters().getPreviewSize();
                    Log8.event("PREVIEW", "size=" + s.width + "x" + s.height);
                    sizeLogged = true;
                } catch (Throwable ignored) {
                }
            }
            if (retry > 0) Log8.event("PREVIEW", "ok after " + retry * 250 + " ms");
            previewStarted();
        } catch (Throwable t) {
            retry++;
            if (retry < 24) {
                handler.postDelayed(startTick, 250);
            } else {
                previewing = false;
                onPreviewStarted = null;
                lastError = "live view: " + t;
                Log8.failure("PREVIEW-FAIL", t);
            }
        }
    }

    private void previewStarted() {
        Runnable started = onPreviewStarted;
        onPreviewStarted = null;
        if (started != null) {
            try {
                started.run();
            } catch (Throwable t) {
                Log8.failure("PREVIEW-CB", t);
            }
        }
    }

    boolean apply(Looks.Look look, int strength) {
        if (camera == null) return false;
        lastError = "";
        note = "";
        Looks.Look scaled = Looks.atStrength(look, strength);
        Map<String, String> wanted = wanted(scaled);
        // Recipe Lab clears the Picture Profile matrix on every body it writes to;
        // here only when the camera exposes the key, so an unknown key can never
        // make the whole parameter set fail.
        try {
            if (camera.getParameters().get("rgb-matrix-mode") != null) {
                wanted.put("rgb-matrix-mode", "false");
            }
        } catch (Throwable ignored) {
        }
        String style = resolveStyle(scaled.style);
        String effect = resolveEffect(scaled.effect);
        wanted.put("color-mode", style);
        wanted.put("picture-effect", effect);
        if ("neutral".equals(scaled.style) && "standard".equals(style)) {
            int sat = neutralFallbackSat(scaled.sat, strength);
            wanted.put("saturation", String.valueOf(sat));
            Log8.event("STYLE", "neutral fallback standard sat=" + sat);
        }
        try {
            setAll(wanted);
        } catch (Throwable t) {
            Log8.failure("APPLY-FULL", t);
            try {
                setAll(core(wanted));
            } catch (Throwable t2) {
                lastError = "parameters rejected: " + t2;
                Log8.failure("APPLY-FAIL", t2);
                return false;
            }
        }
        String got = readStyle();
        String gotFx = readEffect();
        boolean mono = scaled.effect != null;
        if (mono && !isMonoToken(got) && !isMonoToken(gotFx)) {
            // Neither path stuck: Sony accepts setParameters and may still keep
            // the old values, and a body may spell mono differently. Retry the
            // style alone and then every spelling, then the effect alone.
            got = retryMonoStyle(style);
            gotFx = readEffect();
            if (!isMonoToken(got) && !isMonoToken(gotFx)) {
                gotFx = retryMonoEffect(effect);
            }
            Log8.event("MONO", "retry cm=" + got + " fx=" + gotFx);
            if (isMonoToken(got)) wanted.put("color-mode", got);
            if (isMonoToken(gotFx)) wanted.put("picture-effect", gotFx);
        }
        note = mono ? "CM " + got + " FX " + gotFx : "CM " + got;
        verify(wanted);
        Log8.event("APPLY", scaled.tag + " " + strength + "% " + style + " cm=" + got
                + (mono ? " fx=" + gotFx : ""));
        return true;
    }

    private void setAll(Map<String, String> wanted) {
        Camera.Parameters p = camera.getParameters();
        for (Map.Entry<String, String> e : wanted.entrySet()) {
            try {
                p.set(e.getKey(), e.getValue());
            } catch (Throwable t) {
                Log8.failure("SET-" + e.getKey(), t);
            }
        }
        camera.setParameters(p);
    }

    private void verify(Map<String, String> wanted) {
        try {
            Camera.Parameters back = camera.getParameters();
            StringBuilder dropped = new StringBuilder();
            for (Map.Entry<String, String> e : wanted.entrySet()) {
                String got = back.get(e.getKey());
                if (!equals(e.getValue(), got)) {
                    dropped.append(e.getKey()).append('=').append(got).append(' ');
                }
            }
            if (dropped.length() > 0) Log8.event("DROPPED", dropped.toString());
        } catch (Throwable t) {
            Log8.failure("VERIFY-FAIL", t);
        }
    }

    private static boolean equals(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static Map<String, String> wanted(Looks.Look l) {
        Map<String, String> p = new LinkedHashMap<String, String>();
        p.put("color-mode", l.style);
        p.put("saturation", String.valueOf(l.sat));
        p.put("contrast", String.valueOf(l.con));
        p.put("sharpness", String.valueOf(l.sharp));
        p.put("picture-effect", l.effect == null ? "off" : l.effect);
        p.put("storage-fmt", "jpeg");
        p.put("jpeg-quality", "50");
        p.put("whitebalance", "auto");
        p.put("light-balance-for-white-balance", String.valueOf(l.ab));
        // The camera counts magenta positive; the look table counts green positive.
        p.put("color-compensation-for-white-balance", String.valueOf(-l.gm));
        if ("off".equals(l.dro)) {
            p.put("dro-mode", "off");
        } else {
            p.put("dro-mode", "auto");
        }
        return p;
    }

    private static Map<String, String> core(Map<String, String> wanted) {
        Map<String, String> p = new LinkedHashMap<String, String>();
        p.put("color-mode", wanted.get("color-mode"));
        p.put("saturation", wanted.get("saturation"));
        p.put("contrast", wanted.get("contrast"));
        p.put("sharpness", wanted.get("sharpness"));
        p.put("picture-effect", wanted.get("picture-effect"));
        p.put("storage-fmt", wanted.get("storage-fmt"));
        p.put("jpeg-quality", wanted.get("jpeg-quality"));
        return p;
    }

    /** The wanted style, or the runtime spelling of it when the body advertises
     *  a different name (mono/bw/monochrome). Other unlisted values are still
     *  sent: Sony ignores an unknown color-mode rather than rejecting the set,
     *  and the read-back below is what decides the outcome. Neutral is the
     *  exception: see styleOrFallback. */
    private String resolveStyle(String want) {
        try {
            Camera.Parameters p = camera.getParameters();
            String values = p.get("color-mode-values");
            if (values == null) return want;
            String[] list = values.split(",");
            String[] candidates = "mono".equals(want) ? MONO_ALIASES : new String[] { want };
            for (int c = 0; c < candidates.length; c++) {
                for (int i = 0; i < list.length; i++) {
                    if (candidates[c].equals(list[i].trim())) {
                        if (!candidates[c].equals(want)) {
                            Log8.event("STYLE", want + " as " + candidates[c]);
                        }
                        return candidates[c];
                    }
                }
            }
            Log8.event("STYLE", want + " not in " + values);
            return styleOrFallback(values, want);
        } catch (Throwable t) {
            return want;
        }
    }

    /** NEX-6-generation bodies have no Neutral Creative Style and silently keep
     *  the previous style when it is written, so Neutral falls back to Standard
     *  there. Other unlisted values are still sent: the read-back decides. */
    static String styleOrFallback(String values, String want) {
        if ("neutral".equals(want) && values != null) {
            String[] list = values.split(",");
            for (int i = 0; i < list.length; i++) {
                if ("neutral".equals(list[i].trim())) return want;
            }
            return "standard";
        }
        return want;
    }

    /** Standard minus saturation approximates Neutral's flat base; the cut
     *  scales with strength like the look sliders and clamps at -3. */
    static int neutralFallbackSat(int sat, int strength) {
        return Math.max(-3, sat - Math.round(2 * strength / 100.0f));
    }

    private String readStyle() {
        try {
            String s = camera.getParameters().get("color-mode");
            return s == null ? "?" : s;
        } catch (Throwable t) {
            return "?";
        }
    }

    /** The wanted Picture Effect, or the runtime spelling the body advertises. */
    private String resolveEffect(String want) {
        if (want == null) return "off";
        try {
            Camera.Parameters p = camera.getParameters();
            String values = p.get("picture-effect-values");
            if (values == null) return want;
            String[] list = values.split(",");
            String[] candidates = effectAliases(want);
            for (int c = 0; c < candidates.length; c++) {
                for (int i = 0; i < list.length; i++) {
                    if (candidates[c].equals(list[i].trim())) {
                        if (!candidates[c].equals(want)) {
                            Log8.event("EFFECT", want + " as " + candidates[c]);
                        }
                        return candidates[c];
                    }
                }
            }
            Log8.event("EFFECT", want + " not in " + values);
            return want;
        } catch (Throwable t) {
            return want;
        }
    }

    private static String[] effectAliases(String effect) {
        if ("richtone-mono".equals(effect)) return RICHTONE_ALIASES;
        if ("rough-mono".equals(effect)) return ROUGH_ALIASES;
        return new String[] { effect };
    }

    /** Whether a runtime style or effect token means monochrome. */
    private static boolean isMonoToken(String v) {
        if (v == null) return false;
        for (int i = 0; i < MONO_ALIASES.length; i++) {
            if (MONO_ALIASES[i].equals(v)) return true;
        }
        return v.indexOf("mono") >= 0;
    }

    /** Writes color-mode alone, then each mono spelling; returns the last read-back. */
    private String retryMonoStyle(String style) {
        String got = readStyle();
        if (!writeStyle(style)) return got;
        got = readStyle();
        if (isMonoToken(got)) return got;
        for (int i = 0; i < MONO_ALIASES.length; i++) {
            String alias = MONO_ALIASES[i];
            if (alias.equals(style)) continue;
            if (!writeStyle(alias)) return got;
            got = readStyle();
            if (isMonoToken(got)) return got;
        }
        return got;
    }

    /** Writes the mono Picture Effect alone, then each spelling. */
    private String retryMonoEffect(String effect) {
        String got = readEffect();
        String[] aliases = effectAliases(effect);
        for (int i = 0; i < aliases.length; i++) {
            if (!writeEffect(aliases[i])) return got;
            got = readEffect();
            if (isMonoToken(got)) return got;
        }
        return got;
    }

    private boolean writeStyle(String value) {
        return writeParam("color-mode", value, "STYLE-RETRY");
    }

    private boolean writeEffect(String value) {
        return writeParam("picture-effect", value, "EFFECT-RETRY");
    }

    private boolean writeParam(String key, String value, String logKind) {
        try {
            Camera.Parameters p = camera.getParameters();
            p.set(key, value);
            camera.setParameters(p);
            return true;
        } catch (Throwable t) {
            Log8.failure(logKind, t);
            return false;
        }
    }

    private String readEffect() {
        try {
            String s = camera.getParameters().get("picture-effect");
            return s == null ? "?" : s;
        } catch (Throwable t) {
            return "?";
        }
    }

    void restore() {
        if (camera == null || !originalCaptured) return;
        try {
            Camera.Parameters p = camera.getParameters();
            for (int i = 0; i < KEYS.length; i++) {
                String k = KEYS[i];
                String v = original.get(k);
                if (v != null) p.set(k, v);
            }
            camera.setParameters(p);
            Log8.event("RESTORE", "ok");
        } catch (Throwable t) {
            Log8.failure("RESTORE-FAIL", t);
        }
        originalCaptured = false;
    }

    boolean shoot() {
        if (camera == null) {
            lastError = "camera not ready";
            return false;
        }
        try {
            // Written before the call: Recipe Lab records bodies that reboot on
            // the shutter, and then this is the line that survives.
            Log8.event("SHOOT", "requested");
            camera.takePicture(null, null, null);
            previewing = false;
            return true;
        } catch (Throwable t) {
            lastError = "shutter failed: " + t;
            Log8.failure("SHOOT-FAIL", t);
            return false;
        }
    }

    /** Recipe Lab pairs the shutter press with this on release; on bodies that
     *  capture synchronously it is already too late to cancel and a no-op. */
    void cancelShoot() {
        if (cameraEx == null) return;
        try {
            cameraEx.getClass().getMethod("cancelTakePicture").invoke(cameraEx);
        } catch (Throwable ignored) {
        }
    }

    /** Starts an S1 autofocus pass. The callback is always delivered through
     *  the main-thread handler, so the view can be updated without locks. */
    boolean focus(final FocusListener listener) {
        if (camera == null) return false;
        focusListener = listener;
        final int generation = ++focusGeneration;
        Log8.event("FOCUS", "requested");
        try {
            camera.autoFocus(new Camera.AutoFocusCallback() {
                public void onAutoFocus(final boolean success, Camera c) {
                    handler.post(new Runnable() {
                        public void run() {
                            if (generation != focusGeneration) return;
                            FocusListener l = focusListener;
                            focusListener = null;
                            if (l != null) l.onFocusDone(success);
                        }
                    });
                }
            });
        } catch (Throwable t) {
            Log8.failure("FOCUS-FAIL", t);
            handler.post(new Runnable() {
                public void run() {
                    if (generation != focusGeneration) return;
                    FocusListener l = focusListener;
                    focusListener = null;
                    if (l != null) l.onFocusDone(false);
                }
            });
        }
        return true;
    }

    void cancelFocus() {
        focusGeneration++;
        focusListener = null;
        if (camera == null) return;
        try {
            camera.cancelAutoFocus();
        } catch (Throwable ignored) {
        }
    }

    void release() {
        handler.removeCallbacks(startTick);
        handler.removeCallbacks(resumeTick);
        onPreviewStarted = null;
        if (camera != null) {
            try { camera.stopPreview(); } catch (Throwable ignored) {}
        }
        if (cameraEx != null) {
            try {
                Method m = cameraEx.getClass().getMethod("release");
                m.invoke(cameraEx);
            } catch (Throwable t) {
                Log8.failure("RELEASE-CAMERAEX", t);
            }
        } else if (camera != null) {
            try { camera.release(); } catch (Throwable ignored) {}
        }
        camera = null;
        cameraEx = null;
        previewing = false;
        note = "";
    }
}
