package com.termux.app.nobi;

import android.content.Context;

import com.termux.shared.termux.TermuxConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * v31.4 (auto-fix patch, Sam's order): closes the three gaps the v27
 * rewrite still left. (1) The motd early-return skipped the name check,
 * so a motd that was already spaced but still said Nobita never got
 * fixed — the name is now checked independently of the form. (2) A
 * login.sh with no _nobi_greeting block at all (pre-v20 installs) got
 * nothing — the greeting block is now installed (with "Sam"). (3) A
 * stock Termux motd (no Nobi content at all) was left alone — it now
 * gets the reference Nobi welcome. User-customised motds (anything
 * that is neither a known Nobi form nor the stock Termux text) are
 * still left alone, and user shell config is never overwritten.
 * Runs at app start, so existing installs see it too.
 *
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

    /** Reference Nobi welcome: the seed motd + v27 spacing, name Sam. */
    private static final String REFERENCE_MOTD =
            "\n" +
            " \u001b[1;38;5;27m█    █  ████  █████  █████ █████  ████\u001b[0m\n" +
            " \u001b[1;38;5;33m██   █ █    █ █    █   █     █   █    █\u001b[0m\n" +
            " \u001b[1;38;5;39m█ ██ █ █    █ █████    █     █   ██████\u001b[0m\n" +
            " \u001b[1;38;5;45m█  ███ █    █ █    █   █     █   █    █\u001b[0m\n" +
            " \u001b[1;38;5;51m█    █  ████  █████  █████   █   █    █\u001b[0m\n" +
            "\u001b[1;38;5;141m   Welcome back, Sam.\u001b[0m\n" +
            "\u001b[38;5;51m   Your terminal. Your rules.\u001b[0m\n";

    /** Time-aware greeting block, as the seed writes it, name Sam. */
    private static final String LOGIN_GREETING_BLOCK =
            "# NobiTerminal: time-aware greeting, printed by the login script before the shell starts.\n" +
            "_nobi_hour=\"$(date +%H 2>/dev/null)\"\n" +
            "case \"$_nobi_hour\" in\n" +
            "0[0-9]|1[01]) _nobi_greeting=\"Good morning\" ;;\n" +
            "12|1[3-6]) _nobi_greeting=\"Good afternoon\" ;;\n" +
            "*) _nobi_greeting=\"Good evening\" ;;\n" +
            "esac\n" +
            "printf '\\033[1;38;5;141m%s, Sam.\\033[0m\\n' \"$_nobi_greeting\"\n" +
            "unset _nobi_hour _nobi_greeting\n";

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
        // v31.4: stock Termux motd (no Nobi content at all) — install the
        // reference Nobi welcome wholesale. User-customised motds never
        // match this marker, so they are still left alone.
        if (!text.contains("\u2588")
            && !text.contains("Welcome back, Nobita.")
            && !text.contains("Welcome back, Sam.")
            && !text.contains("\u001b[1;38;5;51mNOBITA\u001b[0m")
            && text.contains("Welcome to Termux")) {
            writeAtomically(motd, REFERENCE_MOTD.getBytes(StandardCharsets.UTF_8));
            return;
        }
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
        // Reference name (only the exact greeting line). Checked
        // independently of the form (v31.4): there is no early return
        // above, so a motd that is already spaced but still says
        // Nobita gets fixed too. An already-correct motd falls out
        // unchanged at the equals check below.
        updated = updated.replace("Welcome back, Nobita.", "Welcome back, Sam.");
        if (updated.equals(text)) return;
        writeAtomically(motd, updated.getBytes(StandardCharsets.UTF_8));
    }

    private static void restoreLoginGreetingName() throws Exception {
        File login = new File(TermuxConstants.TERMUX_PREFIX_DIR_PATH, "etc/termux-login.sh");
        if (!login.isFile() || !login.canRead() || !login.canWrite()) return;
        String text = new String(readAll(login), StandardCharsets.UTF_8);
        // v31.4: no greeting block at all (pre-v20 login.sh) — install it
        // at the end, with the reference name. The block is idempotent:
        // it is only appended when _nobi_greeting is absent.
        if (!text.contains("_nobi_greeting")) {
            String updated = text;
            if (!updated.endsWith("\n")) updated += "\n";
            updated += LOGIN_GREETING_BLOCK;
            writeAtomically(login, updated.getBytes(StandardCharsets.UTF_8));
            return;
        }
        // Only the exact Nobi greeting line; the time-aware case logic
        // above it is untouched.
        if (!text.contains(", Nobita.")) return;
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
