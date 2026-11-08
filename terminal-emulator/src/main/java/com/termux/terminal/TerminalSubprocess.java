package com.termux.terminal;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;

/**
 * Public, minimal wrapper around the package-private {@link JNI} PTY API
 * ({@code createSubprocess} / {@code setPtyWindowSize} / {@code waitFor} /
 * {@code close}), for use by modules that cannot see JNI — e.g. the Acode
 * terminal plugin's NobiTerminalServer (v29), which runs the NobiTerminal
 * engine behind the editor's terminal tabs.
 *
 * <p>Additive only: {@link TerminalSession} and JNI are untouched; this
 * class just exposes the same calls (and the same FileDescriptor-wrapping
 * trick TerminalSession uses) through a public API.
 */
public final class TerminalSubprocess {

    private TerminalSubprocess() {
    }

    /** A running PTY subprocess. */
    public static final class Session {
        /** Linux pid of the shell process. */
        public final int pid;
        /** Master PTY file descriptor (raw int, for {@link #setWindowSize} / {@link #close}). */
        public final int fd;
        /** Stream reading the shell's output (PTY master side). */
        public final InputStream in;
        /** Stream writing to the shell's input (PTY master side). */
        public final OutputStream out;

        private Session(int pid, int fd, InputStream in, OutputStream out) {
            this.pid = pid;
            this.fd = fd;
            this.in = in;
            this.out = out;
        }
    }

    /**
     * Start {@code cmd} with {@code args} on a new pseudoterminal.
     *
     * @param cmd     absolute path of the executable (e.g. $PREFIX/bin/bash)
     * @param cwd     working directory for the child
     * @param args    arguments (excluding argv[0]); may be empty, not null
     * @param envVars environment as "VAR=value" strings; may be empty, not null
     * @param rows    initial terminal rows
     * @param columns initial terminal columns
     * @return the live session (pid + fd + streams)
     * @throws IOException if the subprocess could not be created
     */
    public static Session create(String cmd, String cwd, String[] args, String[] envVars,
                                 int rows, int columns) throws IOException {
        int[] processId = new int[1];
        int fd = JNI.createSubprocess(cmd, cwd, args, envVars, processId,
                rows, columns, 0, 0);
        if (fd < 0) {
            throw new IOException("createSubprocess failed for " + cmd + " (fd=" + fd + ")");
        }
        FileDescriptor wrapped = wrapFileDescriptor(fd);
        return new Session(processId[0], fd,
                new FileInputStream(wrapped), new FileOutputStream(wrapped));
    }

    /** Resize the PTY window (rows/columns); cell size is unused (0). */
    public static void setWindowSize(int fd, int rows, int columns) {
        JNI.setPtyWindowSize(fd, rows, columns, 0, 0);
    }

    /** Block until the process {@code pid} exits; returns its exit status (or negated signal). */
    public static int waitFor(int pid) {
        return JNI.waitFor(pid);
    }

    /** Close a PTY master file descriptor. */
    public static void close(int fd) {
        JNI.close(fd);
    }

    /**
     * Same trick as TerminalSession.wrapFileDescriptor: stuff the raw int fd
     * into a java.io.FileDescriptor via reflection so FileInputStream /
     * FileOutputStream can use it.
     */
    private static FileDescriptor wrapFileDescriptor(int fileDescriptor) throws IOException {
        FileDescriptor result = new FileDescriptor();
        try {
            Field descriptorField;
            try {
                descriptorField = FileDescriptor.class.getDeclaredField("descriptor");
            } catch (NoSuchFieldException e) {
                // For desktop java:
                descriptorField = FileDescriptor.class.getDeclaredField("fd");
            }
            descriptorField.setAccessible(true);
            descriptorField.set(result, fileDescriptor);
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException e) {
            throw new IOException("Cannot wrap PTY fd " + fileDescriptor, e);
        }
        return result;
    }
}
