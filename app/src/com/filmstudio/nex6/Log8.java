package com.filmstudio.nex6;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Best-effort log on the camera card. The Sony FUSE layer accepts DOS 8.3 names
 * only, so the directory (FSNEX6) and file (FSLOG.TXT) are both short names;
 * the first writable candidate wins and the file starts with its own path.
 */
final class Log8 {
    private static final String TAG = "FS-NEX6";
    private static final String DIR = "FSNEX6";
    private static final String NAME = "FSLOG.TXT";
    private static String path;
    private static boolean gaveUp;
    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm:ss", Locale.US);

    private Log8() {}

    static void event(String kind, String msg) {
        String line = TIME.format(new Date()) + " " + kind + " " + clean(msg);
        Log.i(TAG, line);
        write(line);
    }

    static void failure(String kind, Throwable t) {
        String s = t == null ? "null" : t.getClass().getName() + ": " + t.getMessage();
        event(kind, s);
    }

    static String stack(Throwable t) {
        String s = Log.getStackTraceString(t);
        return s.length() > 1200 ? s.substring(0, 1200) : s;
    }

    private static String clean(String s) {
        if (s == null) return "";
        s = s.replace('\n', '|').replace('\r', '|');
        return s.length() > 900 ? s.substring(0, 900) : s;
    }

    private static synchronized void write(String line) {
        if (gaveUp) return;
        if (path == null) path = findPath();
        if (path == null) return;
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path, true);
            out.write((line + "\n").getBytes("UTF-8"));
            out.flush();
        } catch (Throwable t) {
            path = null;
            Log.w(TAG, "log write failed: " + t);
        } finally {
            if (out != null) {
                try { out.close(); } catch (Throwable ignored) {}
            }
        }
    }

    private static String findPath() {
        String[] candidates = candidates();
        for (int i = 0; i < candidates.length; i++) {
            String p = candidates[i];
            if (p == null) continue;
            try {
                File f = new File(p);
                File parent = f.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                FileOutputStream out = new FileOutputStream(f, true);
                out.write(("== " + f.getAbsolutePath() + " ==\n").getBytes("UTF-8"));
                out.close();
                return p;
            } catch (Throwable ignored) {
            }
        }
        gaveUp = true;
        return null;
    }

    private static String[] candidates() {
        String root = null;
        try {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                File d = Environment.getExternalStorageDirectory();
                root = d == null ? null : d.getAbsolutePath();
            }
        } catch (Throwable ignored) {
        }
        String[] fixed = new String[] {
                "/sdcard/" + DIR + "/" + NAME,
                "/mnt/sdcard/" + DIR + "/" + NAME,
                "/android/mnt/sdcard/" + DIR + "/" + NAME,
                "/storage/sdcard0/" + DIR + "/" + NAME,
                "/sdcard/DCIM/" + DIR + "/" + NAME,
                "/sdcard/" + NAME
        };
        String[] all = new String[fixed.length + 1];
        all[0] = root == null ? null : root + "/" + DIR + "/" + NAME;
        System.arraycopy(fixed, 0, all, 1, fixed.length);
        return all;
    }
}
