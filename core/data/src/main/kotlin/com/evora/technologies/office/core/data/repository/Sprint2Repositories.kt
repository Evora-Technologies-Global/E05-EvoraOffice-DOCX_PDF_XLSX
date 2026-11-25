package com.evora.technologies.office.core.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Environment
import com.evora.technologies.office.core.data.di.IoDispatcher
import com.evora.technologies.office.core.database.dao.DocumentMetaDao
import com.evora.technologies.office.core.database.dao.DraftRecoveryDao
import com.evora.technologies.office.core.database.dao.SignatureDao
import com.evora.technologies.office.core.database.entity.DocumentMetaEntity
import com.evora.technologies.office.core.database.entity.DraftRecoveryEntity
import com.evora.technologies.office.core.database.entity.SignatureEntity
import com.evora.technologies.office.core.domain.port.DraftRecoveryRepository
import com.evora.technologies.office.core.domain.port.SignatureRepository
import com.evora.technologies.office.core.domain.port.TemplateRepository
import com.evora.technologies.office.core.model.DocFormat
import com.evora.technologies.office.core.model.DocTemplatePreset
import com.evora.technologies.office.core.model.DocumentItem
import com.evora.technologies.office.core.model.DraftCheckpoint
import com.evora.technologies.office.core.model.OfficeKind
import com.evora.technologies.office.core.model.SignatureItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignatureRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val signatureDao: SignatureDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SignatureRepository {

    override fun observeSignatures(): Flow<List<SignatureItem>> = signatureDao.observeAll()
        .map { entities ->
            entities.map { entity ->
                SignatureItem(
                    id = entity.id,
                    assetPath = entity.assetPath,
                    createdAtEpochMs = entity.createdAtEpochMs,
                )
            }
        }
        .flowOn(ioDispatcher)

    override suspend fun saveSignaturePng(
        width: Int,
        height: Int,
        strokes: List<List<Pair<Float, Float>>>,
        strokeColorArgb: Long,
        strokeWidthPx: Float,
    ): Result<SignatureItem> = withContext(ioDispatcher) {
        try {
            val safeW = width.coerceIn(120, 1200)
            val safeH = height.coerceIn(80, 600)
            val nonEmpty = strokes.filter { it.isNotEmpty() }
            if (nonEmpty.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Draw a signature before saving"))
            }

            val bitmap = Bitmap.createBitmap(safeW, safeH, Bitmap.Config.ARGB_8888)
            try {
                val canvas = Canvas(bitmap)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = strokeColorArgb.toInt()
                    style = Paint.Style.STROKE
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                    strokeWidth = strokeWidthPx.coerceIn(2f, 24f)
                }
                for (stroke in nonEmpty) {
                    val path = Path()
                    val first = stroke.first()
                    path.moveTo(first.first, first.second)
                    if (stroke.size == 1) {
                        path.lineTo(first.first + 0.5f, first.second + 0.5f)
                    } else {
                        for (i in 1 until stroke.size) {
                            val prev = stroke[i - 1]
                            val curr = stroke[i]
                            path.quadTo(prev.first, prev.second, (prev.first + curr.first) / 2f, (prev.second + curr.second) / 2f)
                        }
                    }
                    canvas.drawPath(path, paint)
                }

                val dir = File(context.filesDir, "signatures").apply { mkdirs() }
                val id = UUID.randomUUID().toString()
                val outFile = File(dir, "sig_$id.png")
                FileOutputStream(outFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }

                val now = System.currentTimeMillis()
                signatureDao.upsert(
                    SignatureEntity(
                        id = id,
                        assetPath = outFile.absolutePath,
                        createdAtEpochMs = now,
                    ),
                )
                Result.success(SignatureItem(id = id, assetPath = outFile.absolutePath, createdAtEpochMs = now))
            } finally {
                bitmap.recycle()
            }
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun deleteSignature(id: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val dir = File(context.filesDir, "signatures")
            val candidate = File(dir, "sig_$id.png")
            if (candidate.exists()) candidate.delete()
            signatureDao.deleteById(id)
            Result.success(Unit)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}

@Singleton
class DraftRecoveryRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val draftRecoveryDao: DraftRecoveryDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DraftRecoveryRepository {

    override fun observePendingDrafts(): Flow<List<DraftCheckpoint>> = draftRecoveryDao.observePending()
        .map { list -> list.map { it.toModel() } }
        .flowOn(ioDispatcher)

    override suspend fun getPendingDraft(targetUriString: String): DraftCheckpoint? = withContext(ioDispatcher) {
        val entity = draftRecoveryDao.getByTargetUri(targetUriString) ?: return@withContext null
        if (!entity.hasPendingRecovery) return@withContext null
        val file = File(entity.draftCachePath)
        if (!file.exists()) {
            draftRecoveryDao.clearPending(targetUriString)
            return@withContext null
        }
        entity.toModel()
    }

    override suspend fun writeAtomicCheckpoint(
        targetUriString: String,
        displayName: String,
        serializedPayload: ByteArray,
    ): Result<DraftCheckpoint> = withContext(ioDispatcher) {
        try {
            val draftsDir = File(context.filesDir, "drafts").apply { mkdirs() }
            val digest = sha256Hex(targetUriString)
            val tempFile = File(draftsDir, "$digest.tmp")
            val finalFile = File(draftsDir, "$digest.draft")

            // Atomic write: temp -> fsync -> rename (07_TECH_SPEC.md §7.1)
            FileOutputStream(tempFile).use { fos ->
                fos.write(serializedPayload)
                fos.flush()
                runCatching { fos.fd.sync() }
            }
            if (finalFile.exists()) finalFile.delete()
            if (!tempFile.renameTo(finalFile)) {
                tempFile.copyTo(finalFile, overwrite = true)
                tempFile.delete()
            }

            val now = System.currentTimeMillis()
            val entity = DraftRecoveryEntity(
                targetUriString = targetUriString,
                draftCachePath = finalFile.absolutePath,
                displayName = displayName,
                updatedAtEpochMs = now,
                hasPendingRecovery = true,
            )
            draftRecoveryDao.upsert(entity)
            Result.success(entity.toModel())
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun readDraftBytes(draftCachePath: String): ByteArray? = withContext(ioDispatcher) {
        val file = File(draftCachePath)
        if (file.exists()) file.readBytes() else null
    }

    override suspend fun discardDraft(targetUriString: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val existing = draftRecoveryDao.getByTargetUri(targetUriString)
            if (existing != null) {
                File(existing.draftCachePath).delete()
                draftRecoveryDao.deleteByTargetUri(targetUriString)
            }
            Result.success(Unit)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private fun DraftRecoveryEntity.toModel() = DraftCheckpoint(
        targetUriString = targetUriString,
        draftCachePath = draftCachePath,
        displayName = displayName,
        updatedAtEpochMs = updatedAtEpochMs,
        hasPendingRecovery = hasPendingRecovery,
    )

    private fun sha256Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

@Singleton
class TemplateRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val documentMetaDao: DocumentMetaDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TemplateRepository {

    private val presets = listOf(
        DocTemplatePreset("tpl_blank_word", "Blank Word", "Clean A4 document", OfficeKind.WORD, "blank_word.docx", "Untitled_Document.docx"),
        DocTemplatePreset("tpl_resume_cv", "Resume / CV", "Structured career layout", OfficeKind.WORD, "resume_cv.docx", "Resume_CV.docx"),
        DocTemplatePreset("tpl_meeting_notes", "Meeting Notes", "Agenda & action items", OfficeKind.WORD, "meeting_notes.docx", "Meeting_Notes.docx"),
        DocTemplatePreset("tpl_blank_excel", "Blank Sheet", "Clean grid & formulas", OfficeKind.EXCEL, "blank_excel.xlsx", "Untitled_Sheet.xlsx"),
        DocTemplatePreset("tpl_monthly_budget", "Monthly Budget", "Income & expense SUM", OfficeKind.EXCEL, "monthly_budget.xlsx", "Monthly_Budget.xlsx"),
        DocTemplatePreset("tpl_invoice_sheet", "Invoice Sheet", "Client billing calculator", OfficeKind.EXCEL, "invoice_sheet.xlsx", "Invoice_Sheet.xlsx"),
        DocTemplatePreset("tpl_blank_slide", "Blank Slides", "16:9 presentation deck", OfficeKind.PPT, "blank_slide.pptx", "Untitled_Deck.pptx"),
        DocTemplatePreset("tpl_pitch_deck", "Pitch Deck", "5-slide startup deck", OfficeKind.PPT, "pitch_deck.pptx", "Pitch_Deck.pptx"),
    )

    override fun getAvailableTemplates(): List<DocTemplatePreset> = presets

    override suspend fun createDocumentFromTemplate(
        preset: DocTemplatePreset,
        customBaseName: String?,
    ): Result<DocumentItem> = withContext(ioDispatcher) {
        try {
            val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: File(context.filesDir, "documents")
            val docsDir = File(baseDir, "EvoraDocuments").apply { mkdirs() }

            val ext = preset.defaultFileName.substringAfterLast('.', "docx")
            val safeBase = customBaseName?.trim()?.takeIf { it.isNotEmpty() }
                ?.replace(Regex("[/\\\\:*?\"<>|\\x00-\\x1F]"), "_")
                ?: preset.defaultFileName.substringBeforeLast('.')
            val targetName = "$safeBase.$ext"
            var outFile = File(docsDir, targetName)
            var idx = 1
            while (outFile.exists()) {
                outFile = File(docsDir, "${safeBase}_$idx.$ext")
                idx++
            }

            context.assets.open("templates/${preset.assetFileName}").use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }

            val format = when (preset.kind) {
                OfficeKind.WORD -> DocFormat.WORD
                OfficeKind.EXCEL -> DocFormat.EXCEL
                OfficeKind.PPT -> DocFormat.PPT
            }
            val now = System.currentTimeMillis()
            val uriString = outFile.toURI().toString()
            val item = DocumentItem(
                id = uriString,
                uriString = uriString,
                displayName = outFile.name,
                extension = ext,
                format = format,
                sizeBytes = outFile.length(),
                modifiedAtEpochMs = now,
                parentFolderName = docsDir.name,
                lastOpenedAtEpochMs = now,
            )
            documentMetaDao.upsert(
                DocumentMetaEntity(
                    uriString = uriString,
                    displayName = outFile.name,
                    format = format.name,
                    sizeBytes = outFile.length(),
                    modifiedAtEpochMs = now,
                    lastOpenedAtEpochMs = now,
                ),
            )
            Result.success(item)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
