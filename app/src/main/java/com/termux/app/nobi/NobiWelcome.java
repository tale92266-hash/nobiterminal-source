package com.termux.app.nobi;

import android.content.Context;

import com.termux.shared.termux.TermuxConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * v26: restore the REFERENCE welcome (spec Phase 4) instead of compacting
 * it (v24's job). The cached bootstrap motd already has the 5-line block
 * NOBITA wordmark (ANSI-256 blue-to-cyan gradient 27-51) + "Welcome back,
 * Nobita." + "Your terminal. Your rules.", and termux-login.sh prints the
 * time-aware "Good morning/afternoon/evening, Nobita." — but the v26
 * reference screenshot shows the name as Sam, and v24's compaction had
 * already shrunk the wordmark to one line on existing installs. So on app
 * start (background thread, never throws) this rewrites the motd to the
 * reference form — the same 5 block lines byte-for-byte, "Welcome back,
 * Sam.", tagline unchanged — and the login.sh greeting name with it
 * (time-aware logic untouched). Guarded: only touches files that contain
 * the known Nobi forms; user shell config is never overwritten (a motd
 * without the block art / compact mark / Nobita greeting is left alone,
 * and login.sh is only touched when it contains the exact Nobi greeting
 * line). Runs at app start, so — unlike a bootstrap change — existing
 * installs see it too.
 *
 * NAME NOTE (v26 decision): v20 renamed the greeting Sam -&gt; Nobita.
 * Sam's own v26 reference screenshot shows "Welcome back, Sam." /
 * "Good evening, Sam.", so v26 follows the reference. If Sam wants
 * Nobita back, it is a one-word revert here.
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

    /** Reference motd: 5 block lines (cached bootstrap art, byte-exact). */
    private static final String REFERENCE_MOTD =
            "\u001b[1;38;5;27m█    █  ████  █████  █████ █████  ████\u001b[0m\n" +
            "\u001b[1;38;5;33m██   █ █    █ █    █   █     █   █    █\u001b[0m\n" +
            "\u001b[1;38;5;39m█ ██ █ █    █ █████    █     █   ██████\u001b[0m\n" +
            "\u001b[1;38;5;45m█  ███ █    █ █    █   █     █   █    █\u001b[0m\n" +
            "\u001b[1;38;5;51m█    █  ████  █████  █████   █   █    █\u001b[0m\n" +
            "\u001b[1;38;5;141m   Welcome back, Sam.\u001b[0m\n" +
            "\u001b[38;5;51m   Your terminal. Your rules.\u001b[0m\n";

    private static void restoreMotd() throws Exception {
        File motd = new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH, "etc/motd");
        if (!motd.isFile() || !motd.canRead() || !motd.canWrite()) return;
        String text = new String(readAll(motd), StandardCharsets.UTF_8);
        // Already the reference form: block art + Sam greeting + tagline.
        if (text.contains("\u2588") && text.contains("Welcome back, Sam.")
            && text.contains("Your terminal. Your rules.")) return;
        // Only rewrite known Nobi forms: the block art (any name), the
        // v24-compacted single-line mark, or the Nobita greeting.
        if (!text.contains("\u2588")
            && !text.contains("Welcome back, Nobita.")
            && !text.contains("\u001b[1;38;5;51mNOBITA\u001b[0m")) return;
        writeAtomically(motd, REFERENCE_MOTD.getBytes(StandardCharsets.UTF_8));
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
