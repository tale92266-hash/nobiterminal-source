package com.termux.app.nobi;

import android.content.Context;

import com.termux.shared.termux.TermuxConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * v27: reference welcome spacing (spec section 6) WITHOUT rewriting the
 * motd wholesale. v26 replaced the whole motd with a fixed REFERENCE_MOTD
 * — which would have stripped the icon lines the bootstrap motd carries
 * (person / &gt;_ / calendar glyphs before the three welcome lines, as in
 * Sam's own reference screenshot). v27 instead does a SURGICAL transform
 * of whatever Nobi motd is already there, preserving every line
 * byte-for-byte except: (a) one blank line prepended, so the NOBITA
 * wordmark no longer touches the tabs bar (the reference has a clear gap
 * under the tabs), and (b) one leading space on each of the 5 art lines,
 * so the wordmark no longer touches the terminal's left edge. The
 * greeting name is still normalised to Sam (only the exact greeting
 * line). Guarded as before: unknown motd content is left alone, and
 * user shell config is never overwritten. Runs at app start, so existing
 * installs see it too.
 */
public final class NobiWelcome {

    private NobiWelcome() {
    }

    public static void restoreReferenceMotdIfNeeded(Context context) {
        try {
            new Thread(() -> {
                try {
                    restoreMotd();
                    restoreLoginGreetingName();
                } catch (Exception ignored) {
                }
            }).start();
        } catch (Exception ignored) {
        }
    }

    private static void restoreMotd() throws Exception {
        File motd = new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH, "etc/motd");
        if (!motd.isFile() || !motd.canRead() || !motd.canWrite()) return;
        String text = new String(readAll(motd), StandardCharsets.UTF_8);
        // Already the v27 form: leading blank line + space-indented art.
        if (text.startsWith("\n") && text.contains(" \u001b[1;38;5;27m\u2588")) return;
        // Only touch known Nobi forms: the block art (any name/spacing),
        // the v24-compacted single-line mark, or the Nobita greeting.
        if (!text.contains("\u2588")
            && !text.contains("Welcome back, Nobita.")
            && !text.contains("\u001b[1;38;5;51mNOBITA\u001b[0m")) return;
        String updated = text;
        // (a) One blank line on top — the reference gap under the tabs.
        if (!updated.startsWith("\n")) updated = "\n" + updated;
        // (b) Indent the 5 art lines by one column — the reference left
        // margin. Every other line (icons, greetings, tagline) is kept
        // byte-for-byte.
        String[] lines = updated.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.contains("\u2588") && !line.startsWith(" ")) line = " " + line;
            if (i > 0) sb.append('\n');
            sb.append(line);
        }
        updated = sb.toString();
        // Reference name (only the exact greeting line).
        updated = updated.replace("Welcome back, Nobita.", "Welcome back, Sam.");
        if (updated.equals(text)) return;
        writeAtomically(motd, updated.getBytes(StandardCharsets.UTF_8));
    }

    private static void restoreLoginGreetingName() throws Exception {
        File login = new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH, "etc/termux-login.sh");
        if (!login.isFile() || !login.canRead() || !login.canWrite()) return;
        String text = new String(readAll(login), StandardCharsets.UTF_8);
        // Only the exact Nobi greeting line; the time-aware case logic
        // above it is untouched.
        if (!text.contains("_nobi_greeting") || !text.contains(", Nobita.")) return;
        String updated = text.replace(", Nobita.", ", Sam.");
        if (updated.equals(text)) return;
        writeAtomically(login, updated.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeAtomically(File target, byte[] bytes) throws Exception {
        File tmp = new File(target.getParentFile(), target.getName() + ".nobi-tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(bytes);
        }
        if (!tmp.renameTo(target)) tmp.delete();
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
