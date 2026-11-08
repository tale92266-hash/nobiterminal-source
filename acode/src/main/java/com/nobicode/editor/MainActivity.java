/*
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
*/

package com.nobicode.editor;

import android.os.Build;
import android.os.Bundle;

import org.apache.cordova.*;
import android.content.Context;
import java.lang.ref.WeakReference;

public class MainActivity extends CordovaActivity
{

    private static WeakReference<Context> weakContext;

    public static Context getContext() {
        return weakContext != null ? weakContext.get() : null;
    }

    private static boolean hasBrokenRealmeSplashTransfer() {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU
            && ("realme".equalsIgnoreCase(Build.MANUFACTURER)
                || "realme".equalsIgnoreCase(Build.BRAND));
    }

    @Override
    protected void loadConfig() {
        super.loadConfig();

        // Some realme Android 13 builds pass a null SurfaceControl while handing the
        // splash view to an app-provided exit animation. Avoid only that handoff.
        if (hasBrokenRealmeSplashTransfer()) {
            preferences.set("FadeSplashScreen", false);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        weakContext = new WeakReference<>(this);

        // enable Cordova apps to be started in the background
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.getBoolean("cdvStartInBackground", false)) {
            moveTaskToBack(true);
        }

        // Set by <content src="index.html" /> in config.xml
        loadUrl(launchUrl);
    }
}
