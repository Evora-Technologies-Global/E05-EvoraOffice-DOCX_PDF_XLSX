package com.evora.technologies.office.engine.pdf

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.evora.technologies.office.core.domain.port.PdfEnginePort
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.legere.pdfiumandroid.PdfiumCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfiumPdfEngine @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : PdfEnginePort {

    private val pdfiumCore by lazy { PdfiumCore(context) }

    override suspend fun getPageCount(uriString: String, password: String?): Int =
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(uriString)
            val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                ?: error("Unable to open PDF descriptor: $uriString")
            pfd.use { descriptor ->
                pdfiumCore.newDocument(descriptor, password).use { document ->
                    document.getPageCount()
                }
            }
        }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PdfEngineModule {
    @Binds
    @Singleton
    abstract fun bindPdfEnginePort(impl: PdfiumPdfEngine): PdfEnginePort
}
