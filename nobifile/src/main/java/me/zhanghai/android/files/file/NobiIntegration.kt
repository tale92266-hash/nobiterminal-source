/*
 * Copyright (c) 2026 NobiFile contributors
 *
 * Opening files and folders in the merged NobiTerminal app's siblings:
 * the terminal (TermuxActivity, in-app) and the code editor
 * (com.nobicode.editor.MainActivity — Acode, in-app).
 *
 * v29: the old standalone-app route is gone — no RUN_COMMAND service intent,
 * no com.nobiterminal.permission.RUN_COMMAND permission, no
 * allow-external-apps setting. "Open in terminal" now just brings the app's
 * own launcher activity to the front with nobi_terminal_cwd (the same extra
 * the editor's openTerminalHere() uses); "Open in Nobi Code" starts the
 * in-app Acode editor with ACTION_VIEW + a file:// URI (the intent its
 * handlers/intent.js opens). Both are always available in the merged app,
 * so no installed-app checks are needed for them.
 */

package me.zhanghai.android.files.file

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.nobifile.manager.R
import java8.nio.file.Path
import me.zhanghai.android.files.provider.linux.isLinuxPath
import me.zhanghai.android.files.util.showToast

object NobiIntegration {

    const val NOBI_TERMINAL_PACKAGE = "com.nobiterminal"
    const val NOBI_CODE_PACKAGE = "com.nobicode.editor"

    fun isInstalled(packageManager: PackageManager, packageName: String): Boolean =
        try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

    fun launchIntent(
        packageManager: PackageManager,
        packageName: String
    ): Intent? = packageManager.getLaunchIntentForPackage(packageName)

    /**
     * Brings the app's own terminal to the front, opening a session in
     * [directory] (via the nobi_terminal_cwd extra TermuxActivity reads).
     */
    fun openInNobiTerminal(context: Context, file: FileItem) {
        val directory: Path = if (file.attributes.isDirectory) {
            file.path
        } else {
            file.path.parent ?: return
        }
        if (!directory.isLinuxPath) {
            context.showToast(R.string.nobi_terminal_open_failed)
            return
        }
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        intent.putExtra("nobi_terminal_cwd", directory.toString())
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            context.showToast(R.string.nobi_terminal_open_failed)
        }
    }

    /**
     * v30: run [file] in the terminal — TermuxActivity gets the nobi_run_file
     * extra and runs it in the file's folder session (tab stays open so the
     * output can be read). Only script files (.sh/.py/.js) can run; the
     * terminal shows a toast for anything else.
     */
    fun runInNobiTerminal(context: Context, file: FileItem) {
        if (!file.path.isLinuxPath) {
            context.showToast(R.string.nobi_terminal_open_failed)
            return
        }
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        intent.putExtra("nobi_run_file", file.path.toString())
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            context.showToast(R.string.nobi_terminal_open_failed)
        }
    }

    /** v30: script files that can run in the terminal (.sh/.py/.js). */
    fun isRunnableScript(file: FileItem): Boolean {
        if (file.attributes.isDirectory) return false
        val name = file.name.lowercase()
        return name.endsWith(".sh") || name.endsWith(".py") || name.endsWith(".js")
    }

    /**
     * Opens [file] in the in-app code editor (Acode,
     * com.nobicode.editor.MainActivity) via ACTION_VIEW with a file:// URI —
     * the intent the editor's handlers/intent.js opens (VIEW). v29: the old
     * native-editor route (com.nobidev.nobicode.MainActivity +
     * nobi_open_file_path) is gone.
     */
    fun openInNobiCode(context: Context, file: FileItem) {
        if (file.attributes.isDirectory) {
            return
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setClassName(context.packageName, "com.nobicode.editor.MainActivity")
            setDataAndType(Uri.parse("file://" + file.path.toString()), "*/*")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, R.string.nobi_code_not_installed, Toast.LENGTH_SHORT).show()
        }
    }
}
