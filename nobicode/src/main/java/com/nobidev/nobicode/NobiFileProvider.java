package com.nobidev.nobicode;

import androidx.core.content.FileProvider;

/**
 * v29: Distinct FileProvider subclass for the native editor.
 * The manifest merger keys &lt;provider&gt; elements by android:name (the class).
 * :acode declares three androidx.core.content.FileProvider entries and this
 * module declares one more with the same class but a different authority, so
 * the merger fails ("Attribute provider#...@authorities ... is also present").
 * A trivial subclass gives this provider a distinct name while keeping the
 * same authority (com.nobiterminal.fileprovider) and paths (@xml/file_paths),
 * so existing content URIs keep working (getUriForFile looks up by authority).
 */
public class NobiFileProvider extends FileProvider {
}
