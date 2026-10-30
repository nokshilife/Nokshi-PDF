package com.example.data

import java.io.File
import kotlinx.coroutines.flow.Flow

class NokshiRepository(
    private val pdfDao: WorkspacePdfDao,
    private val signatureDao: SignatureDao
) {
    val workspacePdfs: Flow<List<WorkspacePdfEntity>> = pdfDao.observeAllPdfs()
    val signatures: Flow<List<SignatureEntity>> = signatureDao.observeAllSignatures()

    suspend fun upsertWorkspacePdf(
        title: String,
        file: File,
        pageCount: Int,
        sourceCategory: String,
        isPasswordProtected: Boolean = false,
        originalUriString: String? = null,
        downloadUriString: String? = null,
        savedLocationLabel: String = ""
    ): WorkspacePdfEntity {
        val existing = pdfDao.findByPath(file.absolutePath)
        return if (existing != null) {
            val updated = existing.copy(
                title = title,
                pageCount = pageCount,
                fileSizeBytes = file.length(),
                isPasswordProtected = isPasswordProtected,
                originalUriString = originalUriString ?: existing.originalUriString,
                downloadUriString = downloadUriString ?: existing.downloadUriString,
                savedLocationLabel = savedLocationLabel.ifBlank { existing.savedLocationLabel },
                updatedAt = System.currentTimeMillis()
            )
            pdfDao.updatePdf(updated)
            updated
        } else {
            val entity = WorkspacePdfEntity(
                title = title,
                filePath = file.absolutePath,
                pageCount = pageCount,
                fileSizeBytes = file.length(),
                sourceCategory = sourceCategory,
                isPasswordProtected = isPasswordProtected,
                originalUriString = originalUriString,
                downloadUriString = downloadUriString,
                savedLocationLabel = savedLocationLabel,
                updatedAt = System.currentTimeMillis()
            )
            val id = pdfDao.insertPdf(entity)
            entity.copy(id = id)
        }
    }

    suspend fun deleteWorkspacePdf(entity: WorkspacePdfEntity) {
        runCatching { File(entity.filePath).delete() }
        pdfDao.deleteById(entity.id)
    }

    suspend fun saveSignature(label: String, imageFile: File, strokeColorArgb: Int): SignatureEntity {
        val entity = SignatureEntity(
            label = label,
            imagePath = imageFile.absolutePath,
            strokeColorArgb = strokeColorArgb,
            createdAt = System.currentTimeMillis()
        )
        val id = signatureDao.insertSignature(entity)
        return entity.copy(id = id)
    }

    suspend fun deleteSignature(entity: SignatureEntity) {
        runCatching { File(entity.imagePath).delete() }
        signatureDao.deleteSignatureById(entity.id)
    }
}
