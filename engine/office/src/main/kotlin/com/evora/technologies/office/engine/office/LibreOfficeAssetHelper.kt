package com.evora.technologies.office.engine.office

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Verifies and unpacks LibreOfficeKit runtime configuration assets (`assets/unpack/`)
 * into [Context.getApplicationInfo().dataDir] so UNO (`fundamentalrc`, `unorc`,
 * `udkapi.rdb`, `offapi.rdb`, `fonts.conf`) can bootstrap cleanly when packaged.
 */
internal object LibreOfficeAssetHelper {
    private const val TAG = "LibreOfficeAssetHelper"
    private const val MARKER_FILE = ".evora_lok_assets_version"
    private val REQUIRED_APK_ASSETS = arrayOf(
        "program/fundamentalrc",
        "program/services.rdb",
        "unpack/program/udkapi.rdb",
    )

    fun hasBundledRuntimeAssets(context: Context): Boolean {
        val am = context.applicationContext.assets
        return REQUIRED_APK_ASSETS.all { assetPath ->
            runCatching {
                am.open(assetPath).use { true }
            }.getOrDefault(false)
        }
    }

    fun prepareRuntime(context: Context): Boolean {
        val appContext = context.applicationContext
        if (!hasBundledRuntimeAssets(appContext)) {
            return false
        }

        val dataDir = File(appContext.applicationInfo.dataDir)
        val cacheDir = appContext.cacheDir
        val versionCode = resolveVersionCode(appContext)
        val marker = File(dataDir, MARKER_FILE)

        val existingVersion = runCatching {
            if (marker.exists()) marker.readText().trim().toLongOrNull() else null
        }.getOrNull()

        if (existingVersion == versionCode && File(dataDir, "program/udkapi.rdb").exists()) {
            return true
        }

        return runCatching {
            dataDir.mkdirs()
            cacheDir.mkdirs()
            File(dataDir, "fontconfig").mkdirs()
            copyAssetTree(
                assetManager = appContext.assets,
                assetDir = "unpack",
                targetDir = dataDir,
                packageName = appContext.packageName,
            )
            marker.writeText(versionCode.toString())
            true
        }.getOrElse { error ->
            Log.e(TAG, "Failed preparing LibreOfficeKit runtime assets: ${error.message}", error)
            false
        }
    }

    private fun copyAssetTree(
        assetManager: AssetManager,
        assetDir: String,
        targetDir: File,
        packageName: String,
    ) {
        val children = assetManager.list(assetDir).orEmpty()
        if (children.isEmpty()) {
            return
        }
        val canonicalRoot = targetDir.canonicalFile
        for (child in children) {
            val childAssetPath = "$assetDir/$child"
            val subEntries = assetManager.list(childAssetPath).orEmpty()
            val outFile = File(targetDir, child)
            if (!outFile.canonicalPath.startsWith(canonicalRoot.canonicalPath + File.separator) &&
                outFile.canonicalFile != canonicalRoot
            ) {
                throw SecurityException("Blocked unsafe asset path traversal: $childAssetPath")
            }
            if (subEntries.isNotEmpty()) {
                outFile.mkdirs()
                copyAssetTree(assetManager, childAssetPath, outFile, packageName)
            } else {
                outFile.parentFile?.mkdirs()
                if (childAssetPath.endsWith("etc/fonts/fonts.conf")) {
                    val text = assetManager.open(childAssetPath).bufferedReader().use { it.readText() }
                    outFile.writeText(text.replace("@@APPLICATION_ID@@", packageName))
                } else {
                    assetManager.open(childAssetPath).use { input ->
                        FileOutputStream(outFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveVersionCode(context: Context): Long {
        val pkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0L),
            )
        } else {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo.longVersionCode
        } else {
            pkgInfo.versionCode.toLong()
        }
    }

    fun copyUriToTempFile(context: Context, uriString: String, fallbackExt: String): File? {
        val uri = android.net.Uri.parse(uriString)
        val rawName = uri.lastPathSegment?.substringAfterLast('/').orEmpty()
        val ext = rawName.substringAfterLast('.', "").lowercase()
            .takeIf { it.matches(Regex("[a-z0-9]{1,8}")) }
            ?: fallbackExt.lowercase()
        val tempFile = File.createTempFile("lok_src_", ".$ext", context.cacheDir)
        val stream = if (uri.scheme == "file" && uri.path != null) {
            runCatching { java.io.FileInputStream(File(uri.path!!)) }.getOrNull()
        } else {
            context.contentResolver.openInputStream(uri)
                ?: runCatching { java.io.FileInputStream(File(uriString)) }.getOrNull()
        }
        if (stream == null) {
            tempFile.delete()
            return null
        }
        return runCatching {
            stream.use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            }
            tempFile
        }.getOrElse {
            tempFile.delete()
            null
        }
    }
}
