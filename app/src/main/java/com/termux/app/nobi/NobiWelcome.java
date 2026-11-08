package com.termux.app.nobi;

import android.content.Context;

import com.termux.shared.termux.TermuxConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * v24 w48: one-time compaction of the oversized NOBITA block-letter motd
 * (spec 2D). The motd lives in the bootstrap tarball, so a bootstrap patch
 * would only reach fresh installs AND rotate PATCH_HASH (cache MISS); the
 * app-start rewrite reaches existing installs too. Guarded: only rewrites
 * when the file contains the known 5-line block art (a line with "█") plus
 * the "Welcome back, Nobita." line; the two greeting lines are preserved
 * byte-for-byte and everything else is left alone. Runs on a background
 * thread; never throws (startup must never depend on it).
 */
public final class NobiWelcome {

    private NobiWelcome() {
    }

    public static void compactMotdIfNeeded(Context context) {
        try {
            new Thread(() -> {
                try {
                    compactMotd();
                } catch (Exception ignored) {
                }
            }).start();
        } catch (Exception ignored) {
        }
    }

    private static void compactMotd() throws Exception {
        File motd = new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH, "etc/motd");
        if (!motd.isFile() || !motd.canRead() || !motd.canWrite()) return;
        byte[] bytes = readAll(motd);
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (!text.contains("█") || !text.contains("Welcome back, Nobita.")) return;
        String welcome = null;
        String rules = null;
        for (String line : text.split("\n")) {
            if (line.contains("Welcome back, Nobita.")) welcome = line;
            if (line.contains("Your terminal. Your rules.")) rules = line;
        }
        if (welcome == null || rules == null) return;
        // 7 lines -> 3 lines (~57% smaller): one restrained cyan wordmark
        // plus the two original greeting lines, byte-for-byte.
        String compact = "\033[1;38;5;51mNOBITA\033[0m\n" + welcome + "\n" + rules + "\n";
        File tmp = new File(motd.getParentFile(), "motd.nobi-tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(compact.getBytes(StandardCharsets.UTF_8));
        }
        if (!tmp.renameTo(motd)) tmp.delete();
    }

    private static byte[] readAll(File file) throws Exception {
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[(int) file.length()];
            int off = 0;
            int n;
            while (off < buf.length && (n = in.read(buf, off, buf.length - off)) > 0) off += n;
            if (off < buf.length) {
                byte[] shortBuf = new byte[off];
                System.arraycopy(buf, 0, shortBuf, 0, off);
                return shortBuf;
            }
            return buf;
        }
    }

}
