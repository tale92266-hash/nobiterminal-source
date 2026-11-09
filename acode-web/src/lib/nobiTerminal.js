// v30: cross-over helpers — editor ⇄ terminal.
// The editor's terminal tab is xterm.js over the NobiTerminal engine
// (NobiTerminalServer on 127.0.0.1:8767); a new tab's shell always starts
// in the terminal home, so a cross-over jump writes `cd <dir>` (and the
// run command) into the tab once it is connected — the same pattern
// openFolder.js openInTerminal() already uses. No server change needed.
import toast from "components/toast";
import Url from "utils/Url";
import { convertToProotPath, getTerminalPaths, isTerminalAccessiblePath } from "./openFolder";

/** Shell single-quote escaping: 'foo'bar' -> 'foo'\''bar'. */
export function shellQuote(value) {
	return `'${String(value).replace(/'/g, `'\\''`)}'`;
}

/** Extension -> interpreter for "Run in Terminal". */
const RUNNERS = {
	".sh": "bash",
	".py": "python3",
	".js": "node",
};

/** Extensions that need compiling first (no direct run). */
const COMPILE_FIRST = [".kt", ".java", ".c", ".cpp", ".rs", ".go"];

export function isRunnableFile(filePath) {
	const ext = Url.extname(String(filePath || "")).toLowerCase();
	return ext in RUNNERS;
}

export function needsCompileFirst(filePath) {
	const ext = Url.extname(String(filePath || "")).toLowerCase();
	return COMPILE_FIRST.includes(ext);
}

/**
 * Full shell command to run a file, or null when it cannot run directly.
 * The file argument is single-quoted, so spaces in the path are safe.
 */
export function runCommandForFile(filePath) {
	const ext = Url.extname(String(filePath || "")).toLowerCase();
	const interpreter = RUNNERS[ext];
	if (!interpreter) return null;
	return `${interpreter} ${shellQuote(filePath)}`;
}

/** Real device path for a tree/file URL (SAF URIs resolved the same way). */
export function fileUrlToPath(url) {
	return convertToProotPath(url || "");
}

function dirOfPath(filePath) {
	const slash = String(filePath).lastIndexOf("/");
	if (slash <= 0) return "/";
	return String(filePath).slice(0, slash);
}

async function waitForTerminalConnection(component, timeoutMs = 5000) {
	const startTime = Date.now();
	for (;;) {
		if (component.isConnected) return;
		if (Date.now() - startTime > timeoutMs) {
			throw new Error("Terminal connection timeout");
		}
		await new Promise((resolve) => setTimeout(resolve, 50));
	}
}

async function getTerminalManager() {
	const { TerminalManager } = await import(
		/* webpackChunkName: "terminal" */ "components/terminal"
	);
	return TerminalManager;
}

/**
 * Open a terminal tab in [cwd] and, when given, run [command] there.
 * The tab stays open afterwards so the output can be read.
 */
export async function openTerminalWithCommand({ name, cwd, command }) {
	const TerminalManager = await getTerminalManager();
	const terminal = await TerminalManager.createTerminal({
		name,
		render: true,
	});
	if (!terminal?.component) return null;
	await waitForTerminalConnection(terminal.component);
	const dir = cwd || getTerminalPaths().nobiHome;
	const cd = `cd ${JSON.stringify(dir)}`;
	terminal.component.write(command ? `${cd} && ${command}\n` : `${cd}\n`);
	return terminal;
}

/** The terminal instance behind the active tab, else the most recent one. */
export async function getActiveTerminalInstance() {
	const TerminalManager = await getTerminalManager();
	const all = TerminalManager.getAllTerminals();
	if (!all || all.size <= 0) return null;
	const activeFile = editorManager?.activeFile;
	if (activeFile?.type === "terminal") {
		for (const instance of all.values()) {
			if (instance.file?.id === activeFile.id) return instance;
		}
	}
	const instances = [...all.values()];
	return instances[instances.length - 1];
}

function activeFilePath() {
	const file = editorManager?.activeFile;
	if (!file || file.type === "terminal" || !file.uri) return null;
	return { file, path: fileUrlToPath(file.uri), dir: fileUrlToPath(file.location) };
}

/**
 * Run the given file (tree URL) — or the active file — in a terminal tab.
 * .sh/.py/.js run with their interpreter; .kt/.java (and other compiled
 * languages) open the terminal in the file's folder with a compile hint.
 */
export async function runFileInTerminal(fileUrl, fileName) {
	try {
		let path;
		let name = fileName;
		if (fileUrl) {
			path = fileUrlToPath(fileUrl);
			if (!name) {
				const slash = path.lastIndexOf("/");
				name = slash >= 0 ? path.slice(slash + 1) : path;
			}
		} else {
			const active = activeFilePath();
			if (!active) {
				toast(strings["open a file first"] || "Open a file first");
				return false;
			}
			path = active.path;
			name = active.file.filename || name;
		}
		if (!isTerminalAccessiblePath(fileUrl || path)) {
			toast(strings["cannot run this file"] || "Cannot run this file here");
			return false;
		}
		const dir = dirOfPath(path);
		if (needsCompileFirst(path)) {
			await openTerminalWithCommand({ name: `Run: ${name}`, cwd: dir });
			toast(
				strings["compile first"] ||
					"Compile this file first — run kotlinc, javac or your build command in the terminal",
			);
			return true;
		}
		const command = runCommandForFile(path);
		if (!command) {
			toast(
				strings["only scripts run"] ||
					"Only .sh, .py and .js files can run in the terminal",
			);
			return false;
		}
		await openTerminalWithCommand({
			name: `Run: ${name}`,
			cwd: dir,
			command,
		});
		return true;
	} catch (error) {
		console.error("Failed to run file in terminal:", error);
		toast(strings["run failed"] || "Could not run in terminal");
		return false;
	}
}

/** Open a terminal tab in the active file's folder (or the given folder URL). */
export async function openTerminalHere(folderUrl) {
	try {
		let dir;
		let name;
		if (folderUrl) {
			dir = fileUrlToPath(folderUrl);
		} else {
			const active = activeFilePath();
			dir = active?.dir || getTerminalPaths().nobiHome;
		}
		if (!dir) {
			toast(strings["open a file first"] || "Open a file first");
			return false;
		}
		const slash = dir.replace(/\/$/, "").lastIndexOf("/");
		name = slash >= 0 ? dir.replace(/\/$/, "").slice(slash + 1) : dir;
		await openTerminalWithCommand({ name: `Terminal - ${name}`, cwd: dir });
		return true;
	} catch (error) {
		console.error("Failed to open terminal here:", error);
		toast(strings["terminal failed"] || "Could not open terminal");
		return false;
	}
}

function getEditorSelectedText() {
	try {
		const editor = editorManager?.editor;
		if (!editor) return "";
		if (typeof editor.getSelectedText === "function") {
			return editor.getSelectedText() ?? "";
		}
		if (
			typeof editor.session?.getTextRange === "function" &&
			typeof editor.getSelectionRange === "function"
		) {
			return editor.session.getTextRange(editor.getSelectionRange()) ?? "";
		}
		return "";
	} catch {
		return "";
	}
}

function currentLineText() {
	try {
		const editor = editorManager?.editor;
		if (!editor || typeof editor.getCursorPosition !== "function") return "";
		const row = editor.getCursorPosition().row;
		if (typeof editor.session?.getLine === "function") {
			return editor.session.getLine(row) ?? "";
		}
		return "";
	} catch {
		return "";
	}
}

/**
 * Send the selected text (or the current line, when nothing is selected)
 * to the terminal: the active terminal tab if there is one, else the most
 * recent one, else a new tab. The text is typed followed by Enter.
 */
export async function sendSelectionToTerminal() {
	try {
		let text = getEditorSelectedText();
		if (!text.trim()) text = currentLineText();
		if (!text.trim()) {
			toast(strings["select text first"] || "Select text to send to the terminal");
			return false;
		}
		let instance = await getActiveTerminalInstance();
		if (!instance) {
			const active = activeFilePath();
			instance = await openTerminalWithCommand({
				name: "Terminal",
				cwd: active?.dir || undefined,
			});
			if (!instance) return false;
		} else if (instance.file?.makeActive) {
			instance.file.makeActive();
			await waitForTerminalConnection(instance.component);
		}
		if (!text.endsWith("\n")) text += "\n";
		instance.component.write(text);
		return true;
	} catch (error) {
		console.error("Failed to send selection to terminal:", error);
		toast(strings["send failed"] || "Could not send to terminal");
		return false;
	}
}

export default {
	runFileInTerminal,
	sendSelectionToTerminal,
	openTerminalHere,
	runCommandForFile,
	isRunnableFile,
};
