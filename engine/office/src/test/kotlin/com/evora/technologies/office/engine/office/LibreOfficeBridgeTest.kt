package com.evora.technologies.office.engine.office

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.libreoffice.kit.DirectBufferAllocator
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LibreOfficeBridgeTest {

    @Test
    fun `native bridge and asset helper degrade safely on host JVM without crashing`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val adapter = LibreOfficeEngineAdapter(context)

        // On Robolectric JVM (host arch), ARM64 .so libs cannot load; bridge must return false cleanly.
        assertFalse(LibreOfficeNativeBridge.ensureLoaded())
        assertFalse(adapter.isNativeEngineAvailable())
        assertFalse(LibreOfficeAssetHelper.hasBundledRuntimeAssets(context))
        assertFalse(LibreOfficeNativeBridge.ensureInitialized(context))
        assertFalse(adapter.convertToPdfIfAvailable("file:///nonexistent.doc", File(context.cacheDir, "out.pdf").absolutePath))
    }

    @Test
    fun `direct buffer allocator allocates and guards invalid sizes`() {
        val direct = DirectBufferAllocator.allocate(64)
        assertNotNull(direct)
        assertFalse(DirectBufferAllocator.free(direct) != null)
        assertFalse(DirectBufferAllocator.guardedAllocate(0) != null)
    }

    @Test
    fun `copyUriToTempFile safely copies file uri into cache`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source = File(context.filesDir, "sample report.docx").apply {
            writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04))
        }
        val copied = LibreOfficeAssetHelper.copyUriToTempFile(
            context = context,
            uriString = source.toURI().toString(),
            fallbackExt = "docx",
        )
        assertNotNull(copied)
        try {
            assertFalse(copied!!.name.isBlank())
            assertFalse(copied.length() == 0L)
        } finally {
            copied?.delete()
            source.delete()
        }
    }
}
