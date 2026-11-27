/* -*- Mode: Java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*- */
/*
 * This file is part of the LibreOffice project.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.libreoffice.kit;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Context-safe JNI entry point matching native symbols compiled in liblo-native-code.so.
 * Native libraries are loaded lazily via [loadLibraries] instead of a static initializer so
 * JVM unit tests and non-ARM64 environments degrade gracefully without ExceptionInInitializerError.
 */
public final class LibreOfficeKit {
    private static final String LOGTAG = "LibreOfficeKit";
    private static volatile boolean librariesAttempted = false;
    private static volatile boolean librariesLoaded = false;
    private static volatile boolean initializeDone = false;

    private LibreOfficeKit() {
    }

    private static native boolean initializeNative(
        String dataDir,
        String cacheDir,
        String apkFile,
        AssetManager mgr
    );

    public static native ByteBuffer getLibreOfficeKitHandle();

    public static native void putenv(String string);

    public static native void redirectStdio(boolean state);

    public static synchronized boolean loadLibraries() {
        if (librariesAttempted) {
            return librariesLoaded;
        }
        librariesAttempted = true;
        try {
            String[] orderedLibs = {
                "nspr4",
                "plds4",
                "plc4",
                "nssutil3",
                "freebl3",
                "sqlite3",
                "softokn3",
                "nss3",
                "nssckbi",
                "nssdbm3",
                "smime3",
                "ssl3",
                "c++_shared",
                "lo-native-code"
            };
            for (String lib : orderedLibs) {
                System.loadLibrary(lib);
            }
            librariesLoaded = true;
        } catch (Throwable t) {
            Log.w(LOGTAG, "LibreOfficeKit native libraries unavailable on this host/ABI: " + t.getMessage());
            librariesLoaded = false;
        }
        return librariesLoaded;
    }

    public static synchronized boolean init(Context context) {
        if (initializeDone) {
            return true;
        }
        if (!loadLibraries()) {
            return false;
        }
        Context appContext = context.getApplicationContext();
        AssetManager mgr = appContext.getAssets();
        ApplicationInfo info = appContext.getApplicationInfo();
        String dataDir = info.dataDir;
        String cacheDir = appContext.getCacheDir().getAbsolutePath();
        String apkFile = appContext.getPackageResourcePath();

        try {
            boolean isDebuggable = (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
            redirectStdio(isDebuggable);
            putenv("SAL_LOK_OPTIONS=compact_fonts");
            String userRoot = "file://" + dataDir + "/user";
            putenv("BUNDLED_EXTENSIONS=file:///assets/share/extensions");
            putenv("BUNDLED_EXTENSIONS_USER=" + userRoot + "/extensions/bundled");
            putenv("SHARED_EXTENSIONS_USER=" + userRoot + "/extensions/shared");
            putenv("UNO_SHARED_PACKAGES_CACHE=" + userRoot + "/uno_packages/cache");
            putenv("UNO_USER_PACKAGES_CACHE=" + userRoot + "/uno_packages/cache");
            putenv("TMP_EXTENSIONS=" + userRoot + "/extensions/tmp");
            putenv("BAK_EXTENSIONS=" + userRoot + "/extensions/bak");
            if (!initializeNative(dataDir, cacheDir, apkFile, mgr)) {
                Log.e(LOGTAG, "LibreOfficeKit.initializeNative returned false");
                return false;
            }
            initializeDone = true;
            return true;
        } catch (Throwable t) {
            Log.e(LOGTAG, "LibreOfficeKit initialization failed: " + t.getMessage(), t);
            return false;
        }
    }

    public static boolean isInitialized() {
        return initializeDone;
    }
}
