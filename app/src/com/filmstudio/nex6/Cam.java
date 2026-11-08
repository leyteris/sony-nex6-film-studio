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

    boolean open() {
        release();
        lastError = "";
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
        String style = supportedStyle(scaled.style);
        if (!style.equals(scaled.style)) {
            wanted.put("color-mode", style);
            Log8.event("STYLE", scaled.style + " -> " + style);
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
        verify(wanted);
        Log8.event("APPLY", scaled.tag + " " + strength + "% " + style);
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
        p.put("picture-effect", "off");
        p.put("storage-fmt", "jpeg");
        p.put("jpeg-quality", "50");
        if ("kelvin".equals(l.wb)) {
            p.put("whitebalance", "color-temp");
            p.put("color-temperture-white-balance", String.valueOf(l.kelvin * 100));
        } else {
            p.put("whitebalance", "auto");
        }
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

    private String supportedStyle(String want) {
        try {
            Camera.Parameters p = camera.getParameters();
            String values = p.get("color-mode-values");
            if (values == null) return want;
            String[] list = values.split(",");
            for (int i = 0; i < list.length; i++) {
                if (want.equals(list[i].trim())) return want;
            }
            Log8.event("STYLE", want + " not in " + values);
            return "standard";
        } catch (Throwable t) {
            return want;
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
    }
}
