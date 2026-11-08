const Executor = require("./Executor");

/**
 * v29: NobiTerminal engine. There is no Alpine sandbox and no axs binary
 * anymore — the terminal tab runs the merged app's own NobiTerminal bash
 * through the NobiTerminalServer (127.0.0.1:8767), started by the
 * TerminalService. The wire contract is unchanged (GET /status,
 * POST /terminals, WS /terminals/<pid>, POST .../resize, POST
 * .../terminate), so terminal.js needs no protocol changes.
 */
const Terminal = {
    /**
     * Makes sure the NobiTerminal server is up. Binding the
     * TerminalService starts it (TerminalService.onCreate starts the
     * server). Installing is a no-op — the terminal is part of the app,
     * there is nothing to download or extract.
     * @param {boolean} [installing=false] - Ignored; nothing to install.
     * @param {Function} [logger=console.log] - Function to log standard output.
     * @param {Function} [err_logger=console.error] - Function to log errors.
     * @returns {Promise<boolean>} - Returns true when the server is up.
     */
    async startAxs(installing = false, logger = console.log, err_logger = console.error, failsafe = false) {
        if (installing) return true;

        try {
            // A no-op command that forces the TerminalService to start —
            // and with it the NobiTerminal server. The server is listening
            // by the time this resolves (onCreate runs before the command).
            await Executor.execute("true");
        } catch (error) {
            const message = `Failed to start the NobiTerminal server: ${formatError(error)}`;
            err_logger(message);
            throw new Error(message);
        }
        return true;
    },

    /**
     * The server has no standalone process to kill — it stops with the
     * TerminalService. Kept so existing callers keep working.
     * @returns {Promise<void>}
     */
    async stopAxs() {
        // No-op.
    },

    /**
     * True when the NobiTerminal server answers GET /status with "OK".
     * @returns {Promise<boolean>} - `true` if the server is running, `false` otherwise.
     */
    async isAxsRunning() {
        try {
            const response = await new Promise((resolve, reject) => {
                cordova.plugin.http.sendRequest(
                    "http://127.0.0.1:8767/status",
                    { method: "GET", responseType: "text" },
                    (res) => resolve(res),
                    (err) => reject(new Error(err.error || "HTTP error")),
                );
            });
            return response.status === 200 && String(response.data).trim() === "OK";
        } catch (e) {
            return false;
        }
    },

    /**
     * Nothing to install — the terminal ships inside the app.
     * @param {Function} [logger=console.log] - Function to log standard output.
     * @param {Function} [err_logger=console.error] - Function to log errors.
     * @returns {Promise<boolean>} - Always true.
     */
    async install(logger = console.log, err_logger = console.error) {
        return true;
    },

    /**
     * The terminal is built into the app — there is no separate
     * installation to check for.
     * @returns {Promise<boolean>} - Always true.
     */
    async isInstalled() {
        return true;
    },

    /**
     * Checks if the current device architecture is supported.
     * @returns {Promise<boolean>} - `true` if architecture is supported, otherwise `false`.
     */
    isSupported() {
        return new Promise((resolve, reject) => {
            system.getArch((arch) => {
                resolve(["arm64-v8a", "armeabi-v7a", "x86_64"].includes(arch));
            }, reject);
        });
    },

    /**
     * No backups exist in the merged app: the terminal's files live in
     * the NobiTerminal home, not in a sandbox image. Rejects so the
     * settings UI shows a clear message instead of pretending a backup
     * was made.
     * @returns {Promise<string>} - Always rejects with a clear message.
     */
    backup() {
        return Promise.reject(
            "Terminal backup is not available in the merged app — your files live in the NobiTerminal home.",
        );
    },

    /**
     * No backups exist to restore from (see backup()).
     * @returns {Promise<string>} - Always rejects with a clear message.
     */
    restore() {
        return Promise.reject(
            "There is no terminal backup to restore in the merged app.",
        );
    },

    /**
     * The terminal is built into the app and cannot be uninstalled.
     * @returns {Promise<string>} - Always rejects with a clear message.
     */
    uninstall() {
        return Promise.reject(
            "The terminal is built into the app and cannot be uninstalled.",
        );
    },

    /**
     * Nothing to migrate: the terminal home IS the NobiTerminal home.
     * Kept as a no-op — fileBrowser.js calls it (guarded) on some flows.
     * @returns {Promise<void>}
     */
    async migrateLegacyHome() {
        // No-op.
    },

    formatError
};

function formatError(error) {
    if (error == null) return "Unknown error";
    if (error instanceof Error) return error.message || String(error);
    if (typeof error === "string") return error || "Unknown error";
    if (typeof error === "object") {
        const parts = [];
        if (error.status != null) parts.push(`status ${error.status}`);
        if (error.error) parts.push(String(error.error));
        if (error.message) parts.push(String(error.message));
        if (error.exception) parts.push(String(error.exception));
        if (error.url) parts.push(`URL: ${error.url}`);
        if (parts.length) return parts.join(" - ");

        try {
            return JSON.stringify(error);
        } catch (jsonError) {
            return String(error);
        }
    }

    return String(error);
}

module.exports = Terminal;
