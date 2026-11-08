package com.nobicode.editor.rk.exec.terminal;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.termux.shared.termux.TermuxConstants;
import com.termux.terminal.TerminalSubprocess;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * v29 §7 — the NobiTerminal engine behind Acode's xterm.js terminal tabs.
 *
 * <p>Replaces the old {@code axs} Go server + Alpine/PRoot sandbox. This is a
 * small raw-socket HTTP + WebSocket server on 127.0.0.1:8767 (no new
 * dependencies — NanoHTTPD cannot do WebSocket, Java-WebSocket cannot easily
 * serve plain HTTP on the same port). It speaks the exact wire contract the
 * editor's terminal tab already expects from axs, so the JS side
 * (terminal.js / Terminal.js) needs no protocol changes:
 *
 * <ul>
 *   <li>{@code GET /status} → {@code 200 "OK"}</li>
 *   <li>{@code POST /terminals} with JSON {@code {cols, rows}} → {@code 200 "<pid>"}
 *       (plain-text shell pid, the same string axs returned)</li>
 *   <li>{@code GET /terminals/<pid>} with {@code Upgrade: websocket} → raw
 *       byte pump: PTY output is sent as binary frames, terminal input
 *       arrives as text or binary frames (AttachAddon sends both)</li>
 *   <li>{@code POST /terminals/<pid>/resize} with JSON {@code {cols, rows}} →
 *       {@code 200 "OK"}</li>
 *   <li>{@code POST /terminals/<pid>/terminate} → {@code 200 "OK"}</li>
 * </ul>
 *
 * <p>Each session is a real NobiTerminal interactive bash
 * ({@code $PREFIX/bin/bash -i}) on a PTY created through
 * {@link TerminalSubprocess}, in the terminal home, with the canonical
 * Termux shell environment — the same shell the main terminal screen runs.
 * Closing the WebSocket terminates the session, exactly as axs did.
 *
 * <p>Started idempotently from {@link TerminalService#onCreate()} and
 * stopped in {@link TerminalService#onDestroy()}. If the NobiTerminal
 * bootstrap is not installed yet (no bash), session creation fails with a
 * clear 500 message instead of crashing.
 */
public final class NobiTerminalServer {

    private static final String TAG = "NobiTerminalServer";

    /** The port the editor's terminal tab talks to (same as axs used). */
    public static final int PORT = 8767;

    private static final String WS_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final int MAX_HEADER_BYTES = 64 * 1024;
    private static final int MAX_FRAME_BYTES = 16 * 1024 * 1024;

    private static NobiTerminalServer INSTANCE;

    /** Start the server (idempotent — a second call is a no-op). */
    public static synchronized NobiTerminalServer start(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new NobiTerminalServer(context.getApplicationContext());
        }
        INSTANCE.ensureRunning();
        return INSTANCE;
    }

    /** Stop the server and terminate all sessions. */
    public static synchronized void stop() {
        if (INSTANCE != null) {
            INSTANCE.shutdown();
            INSTANCE = null;
        }
    }

    /** True if the server socket is listening. */
    public static synchronized boolean isRunning() {
        return INSTANCE != null && INSTANCE.running;
    }

    private final Context appContext;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final ExecutorService pool = Executors.newCachedThreadPool();

    private ServerSocket serverSocket;
    private volatile boolean running;

    private NobiTerminalServer(Context appContext) {
        this.appContext = appContext;
    }

    private synchronized void ensureRunning() {
        if (running) return;
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress("127.0.0.1", PORT));
            running = true;
            Thread acceptThread = new Thread(this::acceptLoop, "NobiTerminalServer-accept");
            acceptThread.setDaemon(true);
            acceptThread.start();
            Log.i(TAG, "NobiTerminal server listening on 127.0.0.1:" + PORT);
        } catch (IOException e) {
            running = false;
            Log.e(TAG, "Failed to start NobiTerminal server on port " + PORT + ": " + e.getMessage());
            try {
                if (serverSocket != null) serverSocket.close();
            } catch (Exception ignored) {
                // Already closing.
            }
            serverSocket = null;
        }
    }

    private synchronized void shutdown() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception ignored) {
            // Already closing.
        }
        serverSocket = null;
        for (String pid : new ArrayList<>(sessions.keySet())) {
            closeSession(pid);
        }
        pool.shutdownNow();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                socket.setSoTimeout(30_000);
                pool.execute(() -> handleConnection(socket));
            } catch (IOException e) {
                if (running) Log.w(TAG, "accept failed: " + e.getMessage());
            }
        }
    }

    // ---------------------------------------------------------------- HTTP

    private static final class HttpRequest {
        String method;
        String path;
        final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        byte[] body = new byte[0];
    }

    private void handleConnection(Socket socket) {
        boolean isWebSocket = false;
        try {
            BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
            HttpRequest request = readHttpRequest(in);
            if (request == null) return;

            String upgrade = request.headers.get("Upgrade");
            String wsKey = request.headers.get("Sec-WebSocket-Key");
            if ("GET".equalsIgnoreCase(request.method)
                    && request.path.startsWith("/terminals/")
                    && "websocket".equalsIgnoreCase(upgrade)
                    && wsKey != null) {
                String pid = request.path.substring("/terminals/".length());
                Session session = sessions.get(pid);
                if (session == null || session.closed.get()) {
                    sendHttp(socket, 404, "Not Found", "no such terminal: " + pid);
                    return;
                }
                isWebSocket = true;
                socket.setSoTimeout(0); // terminal tabs idle for hours
                sendWsHandshake(socket, wsKey);
                handleWsConnection(socket, in, session);
                return;
            }

            routeHttp(socket, request);
        } catch (Exception e) {
            Log.w(TAG, "HTTP connection failed: " + e.getMessage());
            try {
                sendHttp(socket, 500, "Internal Server Error", e.getMessage() == null ? "error" : e.getMessage());
            } catch (Exception ignored) {
                // Socket already gone.
            }
        } finally {
            if (!isWebSocket) {
                try {
                    socket.close();
                } catch (Exception ignored) {
                    // Already closing.
                }
            }
        }
    }

    private void routeHttp(Socket socket, HttpRequest request) throws IOException {
        if ("GET".equalsIgnoreCase(request.method) && "/status".equals(request.path)) {
            sendHttp(socket, 200, "OK", "OK");
            return;
        }
        if ("POST".equalsIgnoreCase(request.method) && "/terminals".equals(request.path)) {
            String body = new String(request.body, StandardCharsets.UTF_8);
            int cols = parseIntField(body, "cols", 80);
            int rows = parseIntField(body, "rows", 24);
            try {
                Session session = createSession(cols, rows);
                sendHttp(socket, 200, "OK", session.pid);
            } catch (IOException e) {
                Log.w(TAG, "createSession failed: " + e.getMessage());
                sendHttp(socket, 500, "Internal Server Error", e.getMessage());
            }
            return;
        }
        if ("POST".equalsIgnoreCase(request.method)
                && request.path.startsWith("/terminals/")
                && request.path.endsWith("/resize")) {
            String pid = request.path.substring("/terminals/".length(),
                    request.path.length() - "/resize".length());
            Session session = sessions.get(pid);
            if (session == null || session.closed.get()) {
                sendHttp(socket, 404, "Not Found", "no such terminal: " + pid);
                return;
            }
            String body = new String(request.body, StandardCharsets.UTF_8);
            int cols = parseIntField(body, "cols", 80);
            int rows = parseIntField(body, "rows", 24);
            TerminalSubprocess.setWindowSize(session.pty.fd,
                    Math.max(rows, 1), Math.max(cols, 1));
            sendHttp(socket, 200, "OK", "OK");
            return;
        }
        if ("POST".equalsIgnoreCase(request.method)
                && request.path.startsWith("/terminals/")
                && request.path.endsWith("/terminate")) {
            String pid = request.path.substring("/terminals/".length(),
                    request.path.length() - "/terminate".length());
            closeSession(pid);
            sendHttp(socket, 200, "OK", "OK");
            return;
        }
        sendHttp(socket, 404, "Not Found", "not found: " + request.path);
    }

    private static HttpRequest readHttpRequest(BufferedInputStream in) throws IOException {
        ByteArrayOutputStream head = new ByteArrayOutputStream();
        int b;
        int matched = 0; // bytes of "\r\n\r\n" matched so far
        byte[] terminator = {'\r', '\n', '\r', '\n'};
        while ((b = in.read()) != -1) {
            head.write(b);
            if (head.size() > MAX_HEADER_BYTES) {
                throw new IOException("HTTP headers too large");
            }
            if (b == terminator[matched]) {
                matched++;
                if (matched == 4) break;
            } else {
                matched = (b == '\r') ? 1 : 0;
            }
        }
        if (head.size() == 0) return null; // client connected and closed
        if (matched != 4) throw new IOException("Incomplete HTTP request");

        String headText = head.toString(StandardCharsets.US_ASCII.name());
        String[] lines = headText.split("\r\n");
        if (lines.length == 0) throw new IOException("Empty HTTP request");
        String[] requestLine = lines[0].split(" ");
        if (requestLine.length < 2) throw new IOException("Bad request line: " + lines[0]);

        HttpRequest request = new HttpRequest();
        request.method = requestLine[0];
        request.path = requestLine[1];
        for (int i = 1; i < lines.length; i++) {
            int colon = lines[i].indexOf(':');
            if (colon > 0) {
                request.headers.put(lines[i].substring(0, colon).trim(),
                        lines[i].substring(colon + 1).trim());
            }
        }
        int contentLength = 0;
        try {
            contentLength = Integer.parseInt(
                    request.headers.getOrDefault("Content-Length", "0"));
        } catch (NumberFormatException ignored) {
            // No body.
        }
        if (contentLength > 0) {
            if (contentLength > MAX_FRAME_BYTES) {
                throw new IOException("HTTP body too large: " + contentLength);
            }
            request.body = new byte[contentLength];
            readFully(in, request.body, 0, contentLength);
        }
        return request;
    }

    private static void sendHttp(Socket socket, int status, String statusText, String body)
            throws IOException {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String head = "HTTP/1.1 " + status + " " + statusText + "\r\n"
                + "Content-Type: text/plain; charset=utf-8\r\n"
                + "Content-Length: " + bodyBytes.length + "\r\n"
                + "Connection: close\r\n\r\n";
        OutputStream out = socket.getOutputStream();
        out.write(head.getBytes(StandardCharsets.US_ASCII));
        out.write(bodyBytes);
        out.flush();
    }

    /** Minimal JSON integer scan: finds "field": <int>, tolerating key order and whitespace. */
    private static int parseIntField(String json, String field, int defaultValue) {
        int idx = json.indexOf('"' + field + '"');
        if (idx < 0) return defaultValue;
        int colon = json.indexOf(':', idx);
        if (colon < 0) return defaultValue;
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        int start = i;
        if (i < json.length() && json.charAt(i) == '-') i++;
        while (i < json.length() && Character.isDigit(json.charAt(i))) i++;
        if (i == start) return defaultValue;
        try {
            return Integer.parseInt(json.substring(start, i));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // ------------------------------------------------------------- WebSocket

    private static final class WsFrame {
        final int opcode;
        final byte[] payload;

        WsFrame(int opcode, byte[] payload) {
            this.opcode = opcode;
            this.payload = payload;
        }
    }

    private static void sendWsHandshake(Socket socket, String wsKey) throws Exception {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        sha1.update((wsKey + WS_GUID).getBytes(StandardCharsets.US_ASCII));
        String accept = Base64.encodeToString(sha1.digest(), Base64.NO_WRAP);
        String response = "HTTP/1.1 101 Switching Protocols\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: Upgrade\r\n"
                + "Sec-WebSocket-Accept: " + accept + "\r\n\r\n";
        OutputStream out = socket.getOutputStream();
        out.write(response.getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }

    private static void writeWsFrame(OutputStream out, int opcode, byte[] payload) throws IOException {
        out.write(0x80 | opcode);
        int len = payload.length;
        if (len < 126) {
            out.write(len);
        } else if (len <= 0xFFFF) {
            out.write(126);
            out.write((len >> 8) & 0xFF);
            out.write(len & 0xFF);
        } else {
            out.write(127);
            for (int i = 7; i >= 0; i--) out.write((len >> (8 * i)) & 0xFF);
        }
        out.write(payload);
        out.flush();
    }

    private static int readByte(InputStream in) throws IOException {
        int b = in.read();
        if (b == -1) throw new IOException("Unexpected end of WebSocket frame");
        return b;
    }

    private static void readFully(InputStream in, byte[] buf, int off, int len) throws IOException {
        while (len > 0) {
            int n = in.read(buf, off, len);
            if (n == -1) throw new IOException("Unexpected end of stream");
            off += n;
            len -= n;
        }
    }

    /**
     * Reads one complete WebSocket message, reassembling fragmented data
     * frames. Control frames (close/ping/pong) are returned immediately.
     * Returns null on clean EOF.
     */
    private static WsFrame readWsMessage(InputStream in) throws IOException {
        ByteArrayOutputStream acc = null;
        int messageOpcode = -1;
        while (true) {
            int b0 = in.read();
            if (b0 == -1) return null;
            int b1 = in.read();
            if (b1 == -1) return null;
            boolean fin = (b0 & 0x80) != 0;
            int opcode = b0 & 0x0F;
            boolean masked = (b1 & 0x80) != 0;
            long len = b1 & 0x7F;
            if (len == 126) {
                len = ((long) readByte(in) << 8) | readByte(in);
            } else if (len == 127) {
                len = 0;
                for (int i = 0; i < 8; i++) len = (len << 8) | readByte(in);
            }
            if (len > MAX_FRAME_BYTES) {
                throw new IOException("WebSocket frame too large: " + len);
            }
            byte[] mask = null;
            if (masked) {
                mask = new byte[4];
                readFully(in, mask, 0, 4);
            }
            byte[] payload = new byte[(int) len];
            readFully(in, payload, 0, payload.length);
            if (masked) {
                for (int i = 0; i < payload.length; i++) payload[i] ^= mask[i % 4];
            }
            if (opcode == 0x8 || opcode == 0x9 || opcode == 0xA) {
                return new WsFrame(opcode, payload); // control frames are never fragmented
            }
            if (opcode == 0x0) {
                if (acc == null) throw new IOException("Continuation frame without a start");
                acc.write(payload);
                if (fin) return new WsFrame(messageOpcode, acc.toByteArray());
            } else if (opcode == 0x1 || opcode == 0x2) {
                if (!fin) {
                    messageOpcode = opcode;
                    acc = new ByteArrayOutputStream();
                    acc.write(payload);
                } else {
                    return new WsFrame(opcode, payload);
                }
            }
            // Unknown opcodes are ignored.
        }
    }

    private static final class WsConnection {
        private final Socket socket;
        private final OutputStream out;

        WsConnection(Socket socket) throws IOException {
            this.socket = socket;
            this.out = socket.getOutputStream();
        }

        /** PTY output goes out as binary frames (AttachAddon handles them; raw bytes stay intact). */
        synchronized void sendBinary(byte[] payload) {
            try {
                writeWsFrame(out, 0x2, payload);
            } catch (IOException ignored) {
                // Socket gone; the read loop will clean up.
            }
        }

        synchronized void sendFrame(int opcode, byte[] payload) {
            try {
                writeWsFrame(out, opcode, payload);
            } catch (IOException ignored) {
                // Socket gone; the read loop will clean up.
            }
        }

        synchronized void sendClose() {
            sendFrame(0x8, new byte[0]);
        }

        void closeQuietly() {
            try {
                socket.close();
            } catch (Exception ignored) {
                // Already closing.
            }
        }
    }

    private void handleWsConnection(Socket socket, InputStream in, Session session) {
        WsConnection conn;
        try {
            conn = new WsConnection(socket);
        } catch (IOException e) {
            Log.w(TAG, "Cannot open WS connection: " + e.getMessage());
            closeSession(session.pid);
            return;
        }
        session.clients.add(conn);
        try {
            while (!session.closed.get() && !socket.isClosed()) {
                WsFrame frame = readWsMessage(in);
                if (frame == null) break; // EOF — the tab went away
                switch (frame.opcode) {
                    case 0x1: // text input
                    case 0x2: // binary input — raw bytes straight into the PTY
                        try {
                            session.pty.out.write(frame.payload);
                            session.pty.out.flush();
                        } catch (IOException e) {
                            return; // PTY is gone; cleanup below
                        }
                        break;
                    case 0x8: // close — echo it, then terminate the session
                        conn.sendClose();
                        return;
                    case 0x9: // ping — pong back with the same payload
                        conn.sendFrame(0xA, frame.payload);
                        break;
                    case 0xA: // pong — ignore
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            Log.d(TAG, "WS connection ended: " + e.getMessage());
        } finally {
            session.clients.remove(conn);
            conn.closeQuietly();
            // The tab went away — terminate its shell, exactly as axs did.
            closeSession(session.pid);
        }
    }

    // -------------------------------------------------------------- Sessions

    private static final class Session {
        final String pid; // shell pid as a string (the axs contract)
        final int shellPid;
        final TerminalSubprocess.Session pty;
        final Set<WsConnection> clients = ConcurrentHashMap.newKeySet();
        final AtomicBoolean closed = new AtomicBoolean(false);

        Session(TerminalSubprocess.Session pty) {
            this.pty = pty;
            this.shellPid = pty.pid;
            this.pid = String.valueOf(pty.pid);
        }
    }

    private Session createSession(int cols, int rows) throws IOException {
        String shell = TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/bin/bash";
        if (!new File(shell).canExecute()) {
            throw new IOException("NobiTerminal bash is not installed yet — open the "
                    + "terminal once to finish setup (" + shell + " missing)");
        }
        installNobicodeCliIfMissing();

        Map<String, String> env = ProcessManager.buildNobiTerminalEnvironment(appContext);
        String[] envArr = new String[env.size()];
        int i = 0;
        for (Map.Entry<String, String> e : env.entrySet()) {
            envArr[i++] = e.getKey() + "=" + e.getValue();
        }
        String home = TermuxConstants.TERMUX_HOME_DIR_PATH;
        TerminalSubprocess.Session pty = TerminalSubprocess.create(
                shell, home, new String[]{"-i"}, envArr,
                Math.max(rows, 1), Math.max(cols, 1));
        Session session = new Session(pty);
        sessions.put(session.pid, session);
        startPtyReader(session);
        startWaiter(session);
        Log.i(TAG, "Created terminal session pid=" + session.pid
                + " (" + cols + "x" + rows + ")");
        return session;
    }

    private void startPtyReader(Session session) {
        Thread reader = new Thread(() -> {
            byte[] buf = new byte[8192];
            try {
                int n;
                while ((n = session.pty.in.read(buf)) != -1) {
                    byte[] chunk = Arrays.copyOf(buf, n);
                    for (WsConnection c : session.clients) {
                        c.sendBinary(chunk);
                    }
                }
            } catch (IOException e) {
                // PTY closed — the session is ending.
            } finally {
                closeSession(session.pid);
            }
        }, "NobiTerminalServer-reader[" + session.pid + "]");
        reader.setDaemon(true);
        reader.start();
    }

    private void startWaiter(Session session) {
        Thread waiter = new Thread(() -> {
            TerminalSubprocess.waitFor(session.shellPid);
            closeSession(session.pid);
        }, "NobiTerminalServer-waiter[" + session.pid + "]");
        waiter.setDaemon(true);
        waiter.start();
    }

    /** Idempotent: safe to call from the reader, the waiter and the WS loop. */
    private void closeSession(String pid) {
        Session session = sessions.remove(pid);
        if (session == null) return;
        if (!session.closed.compareAndSet(false, true)) return;
        for (WsConnection c : session.clients) {
            c.closeQuietly();
        }
        session.clients.clear();
        killProcessGroup(session.shellPid);
        // Closing the streams closes the PTY master fd (the FileDescriptor
        // is shared by both streams), which SIGHUPs the session's
        // foreground group — the reliable half of the terminate.
        try {
            session.pty.in.close();
        } catch (Exception ignored) {
            // Already closing.
        }
        try {
            session.pty.out.close();
        } catch (Exception ignored) {
            // Already closing.
        }
        Log.i(TAG, "Closed terminal session pid=" + pid);
    }

    /** Best-effort SIGKILL to the whole process group (the shell is a session leader). */
    private void killProcessGroup(int shellPid) {
        String[] killBinaries = {
                TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/bin/kill",
                "/system/bin/kill",
        };
        for (String kill : killBinaries) {
            try {
                new ProcessBuilder(kill, "-9", "-" + shellPid)
                        .redirectErrorStream(true)
                        .start()
                        .waitFor();
                return;
            } catch (Exception ignored) {
                // Try the next kill binary; the PTY close still SIGHUPs.
            }
        }
    }

    /**
     * Installs the {@code nobicode} CLI (open a file/folder in the editor
     * from the terminal, via the OSC 7777 escape sequence — the same
     * protocol the Alpine build used, now with real NobiTerminal paths).
     * Only written if missing, so a user's own version is never clobbered.
     */
    private void installNobicodeCliIfMissing() {
        try {
            File target = new File(
                    TermuxConstants.TERMUX_PREFIX_DIR_PATH + "/bin/nobicode");
            if (target.exists()) return;
            File binDir = target.getParentFile();
            if (binDir == null || !binDir.isDirectory()) return; // bootstrap not installed
            try (InputStream in = appContext.getAssets().open("nobi-terminal-nobicode-cli");
                 OutputStream out = new FileOutputStream(target)) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            }
            target.setExecutable(true, false);
        } catch (Exception e) {
            Log.w(TAG, "Could not install nobicode CLI: " + e.getMessage());
        }
    }
}
