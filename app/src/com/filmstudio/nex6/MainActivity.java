package com.filmstudio.nex6;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.io.File;
import java.util.HashSet;
import java.util.Locale;

/**
 * The whole app: a SurfaceView for the live view, one Canvas overlay, and
 * Sony parameters driven through reflection. The structure follows the app
 * pattern that is known to run on NEX-6 firmware, rather than the a5100
 * app's camera pipeline, because that pipeline needs APIs this body lacks.
 */
public class MainActivity extends Activity implements SurfaceHolder.Callback {
    private static final int K_UP = 103, K_DOWN = 108, K_LEFT = 105, K_RIGHT = 106,
            K_ENTER = 232, K_MENU = 514, K_SK1 = 229, K_SK2 = 513, K_DELETE = 595,
            K_PLAY = 207, K_DISP = 608, K_MOVIE = 515, K_S1 = 516, K_S2 = 518,
            K_WHEEL_CW = 522, K_WHEEL_CCW = 523, K_DIAL_CW = 525, K_DIAL_CCW = 526,
            K_DIAL2_CW = 528, K_DIAL2_CCW = 529, K_DIAL3_CW = 635, K_DIAL3_CCW = 634;

    private static final long SHOT_PREVIEW_MS = 1200;
    private static final long SHOT_BUSY_MS = 2500;

    private final Handler handler = new Handler();
    private final Cam cam = new Cam(handler);
    private UiView ui;
    private SurfaceView surface;
    private SharedPreferences prefs;

    private int lookIndex;
    private int strengthIndex = Looks.STRENGTH.length - 1;
    private int uiMode = UiView.MODE_FULL;
    private boolean resumed;
    private boolean surfaceReady;
    private boolean captureBusy;
    private boolean exiting;
    private String status = "";
    private long startedAt;
    private final HashSet<String> shotBefore = new HashSet<String>();
    private int savedTries;
    private final HashSet<Integer> seenScans = new HashSet<Integer>();

    private final Runnable alive = new Runnable() {
        public void run() {
            if (!resumed) return;
            Log8.event("ALIVE", (System.currentTimeMillis() - startedAt) / 1000 + "s");
            handler.postDelayed(this, 60000);
        }
    };

    private final Runnable previewReady = new Runnable() {
        public void run() {
            applyLook();
        }
    };

    private final Cam.FocusListener focusResult = new Cam.FocusListener() {
        public void onFocusDone(boolean success) {
            ui.setFocus(success ? UiView.FOCUS_LOCKED : UiView.FOCUS_FAILED);
            Log8.event("FOCUS", success ? "locked" : "failed");
        }
    };

    private final Runnable afterShot = new Runnable() {
        public void run() {
            if (!resumed || !cam.isOpen()) return;
            cam.resumePreview(new Runnable() {
                public void run() {
                    applyLook();
                    ui.showFlash(Lang.captured());
                }
            });
        }
    };

    private final Runnable clearBusy = new Runnable() {
        public void run() {
            captureBusy = false;
        }
    };

    /** After a shot, says in the card log whether a new JPEG appeared under DCIM:
     *  on NEX-6 this is the only way to know the native save actually happened.
     *  Matches by file name, not timestamp, because the FUSE layer's mtimes are
     *  not something this app can rely on. Retried because the camera may file
     *  the picture several seconds later. */
    private final Runnable checkSaved = new Runnable() {
        public void run() {
            if (!resumed) return;
            String after = newShot(shotBefore);
            if (after != null) {
                Log8.event("SAVED", after);
            } else if (++savedTries < 3) {
                handler.postDelayed(this, 3500);
            } else {
                Log8.event("SAVED", "no new JPEG (known " + shotBefore.size() + ")");
            }
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        startedAt = System.currentTimeMillis();
        Log8.event("START", "FilmStudio NEX-6 " + versionName() + " ui=" + Lang.code());
        Log8.event("BODY", bodyLine());

        prefs = getPreferences(MODE_PRIVATE);
        lookIndex = clamp(prefs.getInt("look", 0), 0, Looks.ALL.length - 1);
        strengthIndex = clamp(prefs.getInt("strength", strengthIndex), 0, Looks.STRENGTH.length - 1);

        surface = new SurfaceView(this);
        surface.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surface.getHolder().addCallback(this);

        ui = new UiView(this);
        ui.setMode(uiMode);

        FrameLayout root = new FrameLayout(this);
        root.addView(surface, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        root.addView(ui, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        final Thread.UncaughtExceptionHandler was = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread th, Throwable t) {
                Log8.event("CRASH", Log8.stack(t));
                if (was != null) was.uncaughtException(th, t);
            }
        });
        updateUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        Log8.event("RESUME", "open=" + cam.isOpen() + " surface=" + surfaceReady);
        handler.removeCallbacks(alive);
        handler.postDelayed(alive, 60000);
        if (!cam.isOpen()) {
            if (cam.open()) {
                cam.setHolder(surface.getHolder());
                applyLook();
                if (surfaceReady) cam.startPreview(previewReady);
            } else {
                status = cam.lastError();
            }
        }
        updateUi();
    }

    @Override
    protected void onPause() {
        super.onPause();
        resumed = false;
        handler.removeCallbacks(alive);
        handler.removeCallbacks(afterShot);
        handler.removeCallbacks(clearBusy);
        handler.removeCallbacks(checkSaved);
        captureBusy = false;
        ui.setFocus(UiView.FOCUS_OFF);
        cam.cancelFocus();
        cam.restore();
        cam.release();
        Log8.event("PAUSE", "released finishing=" + isFinishing());
    }

    @Override
    protected void onDestroy() {
        Log8.event("END", "app");
        super.onDestroy();
    }

    public void surfaceCreated(SurfaceHolder h) {
        surfaceReady = true;
        cam.setHolder(h);
        if (!resumed) return;
        if (!cam.isOpen() && !cam.open()) {
            status = cam.lastError();
            updateUi();
            return;
        }
        cam.startPreview(previewReady);
    }

    public void surfaceChanged(SurfaceHolder h, int format, int w, int hh) {
    }

    public void surfaceDestroyed(SurfaceHolder h) {
        surfaceReady = false;
        cam.setHolder(null);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getAction() == KeyEvent.ACTION_DOWN) return onKeyDown(e.getKeyCode(), e);
        if (e.getAction() == KeyEvent.ACTION_UP) return onKeyUp(e.getKeyCode(), e);
        return super.dispatchKeyEvent(e);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {
        int sc = e.getScanCode();
        if (sc == 0) sc = fallback(keyCode);
        boolean repeat = e.getRepeatCount() > 0;
        logScan(sc, keyCode, repeat, true);

        if (sc == K_MENU || sc == K_SK1) {
            return true;                       // exit waits for the release, as the app framework expects
        }
        if (sc == K_ENTER || sc == K_S2) {
            if (!repeat) shoot();
            return true;
        }
        if (sc == K_S1) {
            if (!repeat && cam.focus(focusResult)) {
                ui.setFocus(UiView.FOCUS_WORKING);
            }
            return true;
        }
        if (sc == K_UP) {
            moveStrength(+1);
            return true;
        }
        if (sc == K_DOWN) {
            moveStrength(-1);
            return true;
        }
        if (sc == K_LEFT) {
            cycleLook(-1);
            return true;
        }
        if (sc == K_RIGHT) {
            cycleLook(+1);
            return true;
        }
        if (sc == K_WHEEL_CCW || sc == K_DIAL_CCW || sc == K_DIAL2_CCW || sc == K_DIAL3_CCW) {
            cycleLook(-1);
            return true;
        }
        if (sc == K_WHEEL_CW || sc == K_DIAL_CW || sc == K_DIAL2_CW || sc == K_DIAL3_CW) {
            cycleLook(+1);
            return true;
        }
        if (sc == K_PLAY || sc == K_DISP || sc == K_MOVIE || sc == K_DELETE || sc == K_SK2) {
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exitApp();
            return true;
        }
        return super.onKeyDown(keyCode, e);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent e) {
        int sc = e.getScanCode();
        if (sc == 0) sc = fallback(keyCode);
        logScan(sc, keyCode, false, false);
        if (sc == K_MENU || sc == K_SK1) {
            exitApp();
            return true;
        }
        if (sc == K_DELETE || sc == K_SK2) {
            cycleUi();
            return true;
        }
        if (sc == K_S1) {
            cam.cancelFocus();
            ui.setFocus(UiView.FOCUS_OFF);
            return true;
        }
        if (sc == K_S2) {
            cam.cancelShoot();
            return true;
        }
        return true;
    }

    /** Identifies the body and firmware in the card log without a serial or IP. */
    private static String bodyLine() {
        String model = "?", fw = "?", platform = "?";
        try {
            Class<?> props = Class.forName("com.sony.scalar.sysutil.ScalarProperties");
            model = String.valueOf(props.getMethod("getString", String.class).invoke(null, "model.name"));
            platform = String.valueOf(props.getMethod("getString", String.class).invoke(null, "version.platform"));
            fw = String.valueOf(props.getMethod("getFirmwareVersion").invoke(null));
        } catch (Throwable ignored) {
        }
        return "model=" + model + " fw=" + fw + " platform=" + platform
                + " android=" + android.os.Build.VERSION.RELEASE
                + " sdk=" + android.os.Build.VERSION.SDK_INT;
    }

    /** First press/release of each code goes to the card log: on hardware this
     *  is how the real Sony scan codes are confirmed without a debugger. */
    private void logScan(int sc, int keyCode, boolean repeat, boolean down) {
        int key = (sc != 0 ? sc : 10000 + keyCode) * (down ? 1 : -1);
        if (!seenScans.add(Integer.valueOf(key))) return;
        Log8.event(down ? "KEY" : "KEYUP",
                "scan=" + sc + " keyCode=" + keyCode + (repeat ? " repeat" : ""));
    }

    private static int fallback(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP: return K_UP;
            case KeyEvent.KEYCODE_DPAD_DOWN: return K_DOWN;
            case KeyEvent.KEYCODE_DPAD_LEFT: return K_LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return K_RIGHT;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER: return K_ENTER;
            case KeyEvent.KEYCODE_MENU: return K_MENU;
            case KeyEvent.KEYCODE_CAMERA: return K_S2;
            case KeyEvent.KEYCODE_FOCUS: return K_S1;
            case KeyEvent.KEYCODE_DEL: return K_DELETE;
            case 112: return K_DELETE;         // KEYCODE_FORWARD_DEL, API 11+, not in the API 10 stub
            default: return 0;
        }
    }

    /** Three stages like Recipe Lab: full panel, a small look/strength pill, and
     *  nothing but the live view. DELETE or SK2 advances one stage. */
    private void cycleUi() {
        uiMode = (uiMode + 1) % 3;
        ui.setMode(uiMode);
        Log8.event("UI", uiMode == UiView.MODE_FULL ? "full"
                : uiMode == UiView.MODE_PILL ? "pill" : "hidden");
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Throwable t) {
            return "?";
        }
    }

    private void cycleLook(int dir) {
        int n = Looks.ALL.length;
        lookIndex = (lookIndex + dir + n) % n;
        prefs.edit().putInt("look", lookIndex).commit();
        applyLook();
        ui.showFlash(Lang.compactName(Looks.ALL[lookIndex]));
    }

    private void moveStrength(int dir) {
        int next = clamp(strengthIndex + dir, 0, Looks.STRENGTH.length - 1);
        if (next == strengthIndex) return;
        strengthIndex = next;
        prefs.edit().putInt("strength", strengthIndex).commit();
        applyLook();
        if (uiMode == UiView.MODE_HIDDEN) {
            ui.showFlash(Lang.strength() + " " + Looks.STRENGTH[strengthIndex] + "%");
        }
    }

    private void applyLook() {
        if (!cam.isOpen()) {
            status = cam.lastError();
            updateUi();
            return;
        }
        cam.apply(Looks.ALL[lookIndex], Looks.STRENGTH[strengthIndex]);
        status = cam.lastError();
        updateUi();
    }

    private void shoot() {
        if (captureBusy) return;
        if (!cam.isOpen()) {
            status = "camera not ready";
            updateUi();
            return;
        }
        captureBusy = true;
        handler.removeCallbacks(checkSaved);
        savedTries = 0;
        shotBefore.clear();
        collectJpegs(dcimDir(), shotBefore);
        if (cam.shoot()) {
            status = "";
            handler.postDelayed(afterShot, SHOT_PREVIEW_MS);
            handler.postDelayed(clearBusy, SHOT_BUSY_MS);
            handler.postDelayed(checkSaved, SHOT_BUSY_MS);
        } else {
            captureBusy = false;
            status = cam.lastError();
            ui.showFlash(Lang.shutterFailed());
        }
        updateUi();
    }

    private void exitApp() {
        if (exiting) return;
        exiting = true;
        Log8.event("EXIT", "menu");
        handler.removeCallbacks(alive);
        handler.removeCallbacks(checkSaved);
        cam.restore();
        cam.release();
        finish();
    }

    private void updateUi() {
        if (ui != null) ui.setState(lookIndex, strengthIndex, status);
    }

    /** A JPEG that was not on the card before the shutter, as "name size", or
     *  null. Only directory entries and sizes are read, never the pictures. */
    private static String newShot(HashSet<String> before) {
        File dcim = dcimDir();
        File best = newJpeg(dcim, before, null);
        File[] subs = dcim.listFiles();
        if (subs != null) {
            for (int i = 0; i < subs.length; i++) {
                if (subs[i].isDirectory()) best = newJpeg(subs[i], before, best);
            }
        }
        return best == null ? null : best.getName() + " " + best.length();
    }

    private static File newJpeg(File dir, HashSet<String> before, File best) {
        File[] files = dir.listFiles();
        if (files == null) return best;
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory() || !isJpeg(f) || f.length() <= 0) continue;
            if (before.contains(f.getAbsolutePath())) continue;
            if (best == null || f.lastModified() > best.lastModified()) best = f;
        }
        return best;
    }

    private static void collectJpegs(File dir, HashSet<String> into) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                if (into.size() < 10000) collectJpegs(f, into);
            } else if (isJpeg(f)) {
                into.add(f.getAbsolutePath());
            }
        }
    }

    private static boolean isJpeg(File f) {
        String n = f.getName().toUpperCase(Locale.US);
        return n.endsWith(".JPG") || n.endsWith(".JPEG");
    }

    private static File dcimDir() {
        try {
            File ext = Environment.getExternalStorageDirectory();
            if (ext != null) return new File(ext, "DCIM");
        } catch (Throwable ignored) {
        }
        return new File("/sdcard/DCIM");
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
