package com.nobicode.editor.rk.exec.terminal;

import android.content.Context;

import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.shell.command.environment.TermuxShellEnvironment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

/**
 * v29: NobiTerminal engine. The old Alpine/PRoot sandbox is gone — one-shot
 * and long-running commands now run in the merged app's own NobiTerminal
 * environment ({@code $PREFIX/bin/sh}, HOME = terminal home, the canonical
 * Termux shell environment). There is no sandbox to enter, no axs binary,
 * no init scripts.
 *
 * <p>The {@code useAlpine} parameter is kept only so callers
 * (TerminalService, BackgroundExecutor) do not change; it is ignored.
 */
public class ProcessManager {

    private final Context context;

    public ProcessManager(Context context) {
        this.context = context;
    }

    /** The shell every command runs under: the NobiTerminal shell. */
    public static String getShellPath() {
        return TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/bin/sh";
    }

    /** NobiTerminal home — the working directory for commands and PTY sessions. */
    public static String getHomePath() {
        return TermuxConstants.TERMUX_HOME_DIR_PATH;
    }

    /**
     * The environment a NobiTerminal shell runs with: the canonical Termux
     * shell environment (HOME, PREFIX, PATH, TMPDIR, ...) plus ANDROID_TZ.
     * Shared by one-shot commands (here) and PTY sessions
     * (NobiTerminalServer) so both behave identically.
     */
    public static Map<String, String> buildNobiTerminalEnvironment(Context context) {
        HashMap<String, String> env =
                new TermuxShellEnvironment().getEnvironment(context, false);
        env.put("ANDROID_TZ", TimeZone.getDefault().getID());
        return env;
    }

    /**
     * Creates a ProcessBuilder that runs {@code cmd} under the NobiTerminal
     * shell, in the terminal home, with the NobiTerminal environment.
     * {@code useAlpine} is ignored (v29: no Alpine sandbox anymore).
     */
    public ProcessBuilder createProcessBuilder(String cmd, boolean useAlpine) {
        ProcessBuilder builder = new ProcessBuilder(getShellPath(), "-c", cmd);
        builder.directory(new File(getHomePath()));
        Map<String, String> env = builder.environment();
        env.clear();
        env.putAll(buildNobiTerminalEnvironment(context));
        return builder;
    }

    /**
     * Reads all output from a stream
     */
    public static String readStream(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        return output.toString();
    }

    /**
     * Executes a command and returns the result
     */
    public ExecResult executeCommand(String cmd, boolean useAlpine) throws Exception {
        ProcessBuilder builder = createProcessBuilder(cmd, useAlpine);
        Process process = builder.start();

        String stdout = readStream(process.getInputStream());
        String stderr = readStream(process.getErrorStream());
        int exitCode = process.waitFor();

        return new ExecResult(exitCode, stdout.trim(), stderr.trim());
    }

    /**
     * Result container for command execution
     */
    public static class ExecResult {
        public final int exitCode;
        public final String stdout;
        public final String stderr;

        public ExecResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }

        public String getErrorMessage() {
            if (!stderr.isEmpty()) {
                return stderr;
            }
            return "Command exited with code: " + exitCode;
        }
    }
}
