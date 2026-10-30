package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PdfSaveOutcome(
    val originalUriString: String?,
    val downloadUriString: String?,
    val locationBadge: String,
    val messageEn: String,
    val messageBn: String,
    val updatedOriginalInPlace: Boolean
)

object StorageExportHelper {

    /**
     * Requirement ছ:
     * - If the PDF was opened from an existing device file (`originalUriString != null`),
     *   write the modified PDF directly back to that original file URI in-place.
     * - If the PDF is newly created / converted / merged / locked (`originalUriString == null`),
     *   save (or update) it inside the public `Download` folder so the user can always find it immediately.
     */
    suspend fun syncPdfToUserStorage(
        context: Context,
        localPdfFile: File,
        displayTitle: String,
        originalUriString: String?,
        existingDownloadUriString: String?
    ): PdfSaveOutcome = withContext(Dispatchers.IO) {
        val safeBaseName = sanitizeFileName(displayTitle)
        val pdfFileName = if (safeBaseName.endsWith(".pdf", ignoreCase = true)) {
            safeBaseName
        } else {
            "$safeBaseName.pdf"
        }

        // Case 1: Existing file opened by user -> overwrite original URI in-place
        if (!originalUriString.isNullOrBlank()) {
            val targetUri = Uri.parse(originalUriString)
            val wroteInPlace = writeFileToContentUri(context, localPdfFile, targetUri)
            if (wroteInPlace) {
                val origName = FileToPdfConverter.resolveDisplayName(context, targetUri).ifBlank { pdfFileName }
                return@withContext PdfSaveOutcome(
                    originalUriString = originalUriString,
                    downloadUriString = existingDownloadUriString,
                    locationBadge = "Original: $origName",
                    messageEn = "Saved directly to original file ($origName)",
                    messageBn = "মূল ফাইলেই পরিবর্তিত অবস্থায় সেভ হয়েছে ($origName)",
                    updatedOriginalInPlace = true
                )
            }
        }

        // Case 2: New / Converted / Merged / Password-protected file (or read-only external provider fallback)
        // Update existing Download URI if we already created one in this session, otherwise insert into Download/
        if (!existingDownloadUriString.isNullOrBlank()) {
            val existingDlUri = Uri.parse(existingDownloadUriString)
            if (writeFileToContentUri(context, localPdfFile, existingDlUri)) {
                return@withContext PdfSaveOutcome(
                    originalUriString = originalUriString,
                    downloadUriString = existingDownloadUriString,
                    locationBadge = "Download/$pdfFileName",
                    messageEn = "Saved to Download/$pdfFileName",
                    messageBn = "Download ফোল্ডারে সেভ হয়েছে: Download/$pdfFileName",
                    updatedOriginalInPlace = false
                )
            }
        }

        val savedDl = writeNewPdfToDownloadsFolder(context, localPdfFile, pdfFileName)
        PdfSaveOutcome(
            originalUriString = originalUriString,
            downloadUriString = savedDl.first,
            locationBadge = savedDl.second,
            messageEn = "Saved to ${savedDl.second}",
            messageBn = "Download ফোল্ডারে সেভ হয়েছে: ${savedDl.second}",
            updatedOriginalInPlace = false
        )
    }

    private fun writeFileToContentUri(context: Context, srcFile: File, targetUri: Uri): Boolean {
        val resolver = context.contentResolver
        val modes = listOf("wt", "rwt", "w")
        for (mode in modes) {
            val ok = runCatching {
                resolver.openOutputStream(targetUri, mode)?.use { out ->
                    srcFile.inputStream().use { input -> input.copyTo(out) }
                    out.flush()
                    true
                } ?: false
            }.getOrDefault(false)
            if (ok) return true
        }
        return false
    }

    private fun writeNewPdfToDownloadsFolder(
        context: Context,
        srcFile: File,
        pdfFileName: String
    ): Pair<String?, String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val inserted = runCatching {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, pdfFileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        srcFile.inputStream().use { input -> input.copyTo(out) }
                        out.flush()
                    }
                    val doneValues = ContentValues().apply {
                        put(MediaStore.Downloads.IS_PENDING, 0)
                    }
                    resolver.update(uri, doneValues, null, null)
                    val actualName = FileToPdfConverter.resolveDisplayName(context, uri).ifBlank { pdfFileName }
                    Pair(uri.toString(), "Download/$actualName")
                } else null
            }.getOrNull()
            if (inserted != null) return inserted
        }

        // Fallback for API 24-28 or local unit-test environments
        val dlDir = runCatching {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.apply { mkdirs() }
        }.getOrNull() ?: File(context.filesDir, "Download").apply { mkdirs() }

        var target = File(dlDir, pdfFileName)
        if (target.exists()) {
            val stem = pdfFileName.removeSuffix(".pdf")
            target = File(dlDir, "${stem}_${System.currentTimeMillis() % 10000}.pdf")
        }
        runCatching {
            FileOutputStream(target).use { out ->
                srcFile.inputStream().use { input -> input.copyTo(out) }
            }
        }
        return Pair(Uri.fromFile(target).toString(), "Download/${target.name}")
    }

    private fun sanitizeFileName(raw: String): String {
        val cleaned = raw.trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), "_")
        return cleaned.ifBlank { "Nokshi_Document" }
    }
}
