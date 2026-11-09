package com.filmstudio.nex6;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

/**
 * Best-effort log on the camera card. The Sony FUSE layer accepts DOS 8.3 names
 * only, so the directory (FSNEX6) and files (FSLOG.TXT, REPORT.TXT) are short.
 *
 * FSLOG.TXT is an append journal: on Sony FUSE a file kept open for append reads
 * back as all-NUL while the process is alive (the layer rejects simultaneous
 * read/write). REPORT.TXT is the readable copy: a fresh file written once and
 * closed, never reopened, produced on exit/crash. The same pattern is verified
 * on A6300 by LUT Camera. An in-memory ring keeps the lines even if the journal
 * write fails.
 */
final class Log8 {
    private static final String TAG = "FS-NEX6";
    private static final String DIR = "FSNEX6";
    private static final String NAME = "FSLOG.TXT";
    private static final String REPORT = "REPORT.TXT";
    static final int MAX_LINES = 600;
    private static final LinkedList<String> BUFFER = new LinkedList<String>();
    private static String path;
    private static boolean gaveUp;
    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm:ss", Locale.US);

    private Log8() {}

    static void event(String kind, String msg) {
        record(TIME.format(new Date()) + " " + kind + " " + clean(msg));
    }

    private static void record(String line) {
        Log.i(TAG, line);
        synchronized (BUFFER) {
            BUFFER.add(line);
            while (BUFFER.size() > MAX_LINES) BUFFER.removeFirst();
        }
        write(line);
    }

    /** Writes the buffered lines to a fresh, closed REPORT.TXT that the Sony
     *  FUSE layer will actually return on read. Returns false if it could not. */
    static boolean export() {
        String dir = journalDir();
        if (dir == null) return false;
        File f = new File(dir, REPORT);
        // Record the marker first so the snapshot is self-describing; the append
        // journal also keeps it as residue even if the snapshot write fails.
        record(TIME.format(new Date()) + " EXPORT " + f.getAbsolutePath());
        String snapshot;
        int lines;
        synchronized (BUFFER) {
            StringBuilder sb = new StringBuilder();
            sb.append("== ").append(f.getAbsolutePath()).append(" ==\n");
            for (int i = 0; i < BUFFER.size(); i++) {
                sb.append(BUFFER.get(i)).append('\n');
            }
            snapshot = sb.toString();
            lines = BUFFER.size();
        }
        FileOutputStream out = null;
        try {
            if (f.exists()) f.delete();
            out = new FileOutputStream(f, false);
            out.write(snapshot.getBytes("UTF-8"));
            out.flush();
            out.getFD().sync();
            out.close();
            out = null;
            Log.i(TAG, "report written: " + f.getAbsolutePath() + " lines=" + lines);
            // Read it back at once: Sony FUSE can keep the size and return only
            // NUL, and this is the one moment the verdict is known on the body.
            event("EXPORT-VERIFY", scan(f));
            return true;
        } catch (Throwable t) {
            Log.w(TAG, "report export failed: " + t);
            return false;
        } finally {
            if (out != null) {
                try { out.close(); } catch (Throwable ignored) {}
            }
        }
    }

    private static String journalDir() {
        if (path == null) path = findPath();
        if (path == null) return null;
        File parent = new File(path).getParentFile();
        return parent == null ? null : parent.getAbsolutePath();
    }

    /** Reads the two files of the previous session back with the same File API
     *  they were written with, so the body itself reports whether Sony keeps
     *  what the app writes. Called once at startup; the result is a log line. */
    static void probe() {
        String dir = journalDir();
        if (dir == null) {
            event("PROBE", "no writable directory");
            return;
        }
        probeOne(dir, REPORT);
        probeOne(dir, NAME);
    }

    private static void probeOne(String dir, String name) {
        File f = new File(dir, name);
        if (!f.exists()) {
            event("PROBE", name + " missing in " + dir);
            return;
        }
        event("PROBE", name + " " + scan(f));
    }

    /** Byte census of the first 4 KiB: a file Sony failed to flush reports the
     *  right size with every byte NUL instead of failing the read. */
    private static String scan(File f) {
        long size = f.length();
        byte[] b = new byte[(int) Math.min(size, 4096)];
        FileInputStream in = null;
        int n = 0;
        try {
            in = new FileInputStream(f);
            n = in.read(b);
        } catch (Throwable t) {
            return "size=" + size + " read_failed=" + t.getClass().getSimpleName();
        } finally {
            if (in != null) {
                try { in.close(); } catch (Throwable ignored) {}
            }
        }
        int zero = 0, text = 0, other = 0;
        for (int i = 0; i < n; i++) {
            int c = b[i] & 0xFF;
            if (c == 0) zero++;
            else if (c >= 32 && c < 127) text++;
            else other++;
        }
        return "size=" + size + " head=" + n + " zero=" + zero + " text=" + text + " other=" + other;
    }

    /** The most recent buffered lines, oldest first, for the on-screen overlay. */
    static String[] tail(int max) {
        synchronized (BUFFER) {
            int n = Math.min(max, BUFFER.size());
            String[] out = new String[n];
            int from = BUFFER.size() - n;
            for (int i = 0; i < n; i++) out[i] = BUFFER.get(from + i);
            return out;
        }
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
        if (path == null) {
            path = findPath();
            if (path != null) {
                // Which mount accepted the file is part of the diagnosis:
                // first-generation firmware may not share /sdcard and /mnt/sdcard.
                record(TIME.format(new Date()) + " PATH " + path);
            }
        }
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
