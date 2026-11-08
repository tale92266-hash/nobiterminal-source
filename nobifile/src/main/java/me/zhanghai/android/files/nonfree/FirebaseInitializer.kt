/*
 * Copyright (c) 2019 Hai Zhang <dreaming.in.code.zh@gmail.com>
 * All Rights Reserved.
 */

// v29: STUB — the merged app is the FREE build; Firebase / Google Play
// Services are stripped (no google-services.json, no Firebase dependencies).
// Upstream keeps this file only in the NONFREE (paid) build via its
// //#ifdef NONFREE preprocessor. Here it is a no-op so AppInitializers'
// initializeFirebase() call still resolves and the module compiles.
package me.zhanghai.android.files.nonfree

object FirebaseInitializer {
    fun initialize() {
        // No-op: no Firebase in the merged app.
    }
}
