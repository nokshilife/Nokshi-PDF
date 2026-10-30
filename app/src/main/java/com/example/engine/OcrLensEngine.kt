package com.example.engine

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.ImageType
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class OcrStepProgress(
    val stepTitleBn: String,
    val currentPage: Int,
    val totalPages: Int
)

object OcrLensEngine {

    /**
     * Requirement 6:
     * 1. Converts PDF pages into high-resolution PNG images in background.
     * 2. Runs Google's on-device ML Kit Text Recognizer (Google Lens OCR technology)
     *    on each rendered page image to extract visual text blocks & lines, supplemented
     *    with Unicode stream extraction for Indic/Bengali glyphs.
     * 3. Produces structured OcrPageResult items for the interactive Tap-to-Select & Copy Window,
     *    plus ready-to-open converted page images for external Google Lens app launch.
     */
    suspend fun runBackgroundPdfToImageAndLensOcr(
        context: Context,
        pdfFile: File,
        password: String = "",
        onProgress: (OcrStepProgress) -> Unit = {}
    ): List<OcrPageResult> = withContext(Dispatchers.IO) {
        PdfEditAndSecurityEngine.ensureInitialized(context)
        val cacheDir = File(context.cacheDir, "ocr_lens_pages_${System.currentTimeMillis()}").apply { mkdirs() }
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val pageResults = mutableListOf<OcrPageResult>()

        try {
            PDDocument.load(pdfFile, password).use { doc ->
                val total = doc.numberOfPages.coerceAtLeast(1)
                val renderer = PDFRenderer(doc)
                val stripper = PDFTextStripper()

                for (i in 0 until doc.numberOfPages) {
                    val pageNo = i + 1
                    onProgress(
                        OcrStepProgress(
                            stepTitleBn = "ব্যাকএন্ডে পেজ $pageNo/$total ইমেজে কনভার্ট করা হচ্ছে...",
                            currentPage = pageNo,
                            totalPages = total
                        )
                    )

                    // Step 1: Convert PDF page to high-res image in background
                    val pageBitmap = renderer.renderImage(i, 2.2f, ImageType.RGB)
                    val imgFile = File(cacheDir, "lens_ocr_page_$pageNo.png")
                    FileOutputStream(imgFile).use { fos ->
                        pageBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    }

                    // Step 2: Process converted image through Google ML Kit / Lens OCR engine
                    onProgress(
                        OcrStepProgress(
                            stepTitleBn = "Google Lens / ML Kit ইঞ্জিনে পেজ $pageNo থেকে টেক্সট এক্সট্রাক্ট হচ্ছে...",
                            currentPage = pageNo,
                            totalPages = total
                        )
                    )

                    val visionLines = mutableListOf<String>()
                    runCatching {
                        val inputImage = InputImage.fromBitmap(pageBitmap, 0)
                        val visionText = recognizer.process(inputImage).await()
                        visionText.textBlocks.forEach { block ->
                            block.lines.forEach { line ->
                                val clean = line.text.trim()
                                if (clean.isNotEmpty()) visionLines.add(clean)
                            }
                        }
                    }
                    pageBitmap.recycle()

                    // Supplement with embedded Unicode lines (e.g. বাংলা যুক্তবর্ণ) if present
                    val unicodeLines = runCatching {
                        stripper.startPage = pageNo
                        stripper.endPage = pageNo
                        stripper.getText(doc)
                            .lines()
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                    }.getOrDefault(emptyList())

                    val combinedLines = mergeExtractedLines(visionLines, unicodeLines)
                    val segments = combinedLines.map { line ->
                        OcrTextSegment(pageNumber = pageNo, text = line)
                    }
                    val fullPageText = combinedLines.joinToString("\n")

                    pageResults.add(
                        OcrPageResult(
                            pageNumber = pageNo,
                            imagePath = imgFile.absolutePath,
                            segments = segments,
                            fullText = fullPageText
                        )
                    )
                }
            }
        } finally {
            runCatching { recognizer.close() }
        }

        pageResults
    }

    private fun mergeExtractedLines(
        ocrLines: List<String>,
        unicodeLines: List<String>
    ): List<String> {
        val hasIndicUnicode = unicodeLines.any { line ->
            line.any { ch -> ch.code in 0x0980..0x09FF } // Bengali Unicode block
        }
        return when {
            hasIndicUnicode -> {
                val merged = LinkedHashSet<String>()
                unicodeLines.forEach { merged.add(it) }
                ocrLines.forEach { line ->
                    if (merged.none { existing -> existing.contains(line, ignoreCase = true) }) {
                        merged.add(line)
                    }
                }
                merged.toList()
            }
            ocrLines.isNotEmpty() -> ocrLines
            unicodeLines.isNotEmpty() -> unicodeLines
            else -> listOf("(এই পেজে কোনো পাঠযোগ্য টেক্সট পাওয়া যায়নি)")
        }
    }

    /**
     * Opens the backend-converted PDF page image directly in the Google Lens app
     * (or Google Search Lens receiver, with standard image chooser fallback).
     */
    fun openConvertedImageInGoogleLens(context: Context, imagePath: String): Boolean {
        val file = File(imagePath)
        if (!file.exists()) return false
        return runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val pm = context.packageManager

            val lensDirect = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                setPackage("com.google.ar.lens")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (lensDirect.resolveActivity(pm) != null) {
                context.startActivity(lensDirect)
                return true
            }

            val googleAppLens = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                setPackage("com.google.android.googlequicksearchbox")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (googleAppLens.resolveActivity(pm) != null) {
                context.startActivity(googleAppLens)
                return true
            }

            val fallbackChooser = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Google Lens দিয়ে খুলুন"
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallbackChooser)
            true
        }.getOrDefault(false)
    }
}
