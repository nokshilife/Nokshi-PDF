package com.example.engine

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import com.tom_roush.pdfbox.rendering.ImageType
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.File
import java.io.FileOutputStream
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PdfInspectResult(
    val pageCount: Int,
    val isEncrypted: Boolean,
    val requiresPassword: Boolean
)

class PasswordRequiredPdfException : Exception("এই PDF ফাইলটি পাসওয়ার্ড দিয়ে সুরক্ষিত।")

object PdfEditAndSecurityEngine {

    @Volatile
    private var pdfBoxInitialized = false

    fun ensureInitialized(context: Context) {
        if (!pdfBoxInitialized) {
            synchronized(this) {
                if (!pdfBoxInitialized) {
                    PDFBoxResourceLoader.init(context.applicationContext)
                    pdfBoxInitialized = true
                }
            }
        }
    }

    suspend fun inspectPdf(
        context: Context,
        file: File,
        password: String = ""
    ): PdfInspectResult = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        try {
            PDDocument.load(file, password).use { doc ->
                PdfInspectResult(
                    pageCount = doc.numberOfPages,
                    isEncrypted = doc.isEncrypted,
                    requiresPassword = false
                )
            }
        } catch (_: InvalidPasswordException) {
            PdfInspectResult(
                pageCount = 1,
                isEncrypted = true,
                requiresPassword = true
            )
        }
    }

    suspend fun renderPageBitmap(
        context: Context,
        file: File,
        pageIndex: Int,
        password: String = "",
        scale: Float = 2.0f
    ): Bitmap = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        try {
            PDDocument.load(file, password).use { doc ->
                val safeIdx = pageIndex.coerceIn(0, (doc.numberOfPages - 1).coerceAtLeast(0))
                val renderer = PDFRenderer(doc)
                renderer.renderImage(safeIdx, scale, ImageType.RGB)
            }
        } catch (_: InvalidPasswordException) {
            throw PasswordRequiredPdfException()
        }
    }

    suspend fun extractPageTextLines(
        context: Context,
        file: File,
        pageIndex: Int,
        password: String = ""
    ): List<DetectedPdfTextLine> = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        PDDocument.load(file, password).use { doc ->
            if (pageIndex !in 0 until doc.numberOfPages) return@use emptyList()
            val page = doc.getPage(pageIndex)
            val cropBox = page.cropBox ?: page.mediaBox ?: PDRectangle.A4
            val pageW = cropBox.width.coerceAtLeast(1f)
            val pageH = cropBox.height.coerceAtLeast(1f)

            val rawLines = mutableListOf<DetectedPdfTextLine>()
            val stripper = object : PDFTextStripper() {
                override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
                    val str = text?.trim() ?: return
                    val positions = textPositions?.filter { !it.unicode.isNullOrBlank() } ?: return
                    if (str.isEmpty() || positions.isEmpty()) return

                    val minX = positions.minOf { it.xDirAdj }
                    val maxX = positions.maxOf { it.xDirAdj + it.widthDirAdj }
                    val minTop = positions.minOf { it.yDirAdj - it.heightDir.coerceAtLeast(it.fontSizeInPt * 0.85f) }
                    val maxBottom = positions.maxOf { it.yDirAdj + it.heightDir * 0.22f }
                    val avgPt = positions.map { it.fontSizeInPt.takeIf { s -> s > 4f } ?: 12f }.average().toFloat()

                    rawLines.add(
                        DetectedPdfTextLine(
                            pageIndex = pageIndex,
                            text = str,
                            normLeft = (minX / pageW).coerceIn(0f, 0.98f),
                            normTop = (minTop / pageH).coerceIn(0f, 0.98f),
                            normRight = ((maxX + 4f) / pageW).coerceIn(0.02f, 1f),
                            normBottom = ((maxBottom + 2f) / pageH).coerceIn(0.02f, 1f),
                            fontSizePt = avgPt.coerceIn(8f, 36f)
                        )
                    )
                }
            }
            stripper.startPage = pageIndex + 1
            stripper.endPage = pageIndex + 1
            stripper.getText(doc)
            rawLines
        }
    }

    suspend fun addBlankPageToPdf(
        context: Context,
        file: File,
        password: String = ""
    ): Int = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        val tmp = File(file.parentFile, "tmp_blank_${System.currentTimeMillis()}.pdf")
        val count = PDDocument.load(file, password).use { doc ->
            doc.isAllSecurityToBeRemoved = password.isEmpty()
            doc.addPage(PDPage(PDRectangle.A4))
            if (password.isNotEmpty()) {
                val spp = StandardProtectionPolicy(password, password, AccessPermission()).apply {
                    encryptionKeyLength = 128
                }
                doc.protect(spp)
            }
            doc.save(tmp)
            doc.numberOfPages
        }
        tmp.copyTo(file, overwrite = true)
        tmp.delete()
        count
    }

    suspend fun saveEditsToPdf(
        context: Context,
        sourceFile: File,
        edits: List<PageEditItem>,
        password: String = "",
        saveAsNewCopy: Boolean = false
    ): File = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        val targetFile = if (saveAsNewCopy) {
            File(sourceFile.parentFile, "${sourceFile.nameWithoutExtension}_edited_${System.currentTimeMillis()}.pdf")
        } else {
            File(sourceFile.parentFile, "tmp_save_${System.currentTimeMillis()}.pdf")
        }

        val regularTf = runCatching { ResourcesCompat.getFont(context, R.font.hind_siliguri) }.getOrNull() ?: Typeface.SANS_SERIF
        val boldTf = Typeface.create(regularTf, Typeface.BOLD)

        PDDocument.load(sourceFile, password).use { doc ->
            doc.isAllSecurityToBeRemoved = true
            val editsByPage = edits.groupBy { it.pageIndex }

            for ((pageIndex, pageEdits) in editsByPage) {
                if (pageIndex !in 0 until doc.numberOfPages) continue
                val page = doc.getPage(pageIndex)
                val box = page.cropBox ?: page.mediaBox ?: PDRectangle.A4
                val pw = box.width
                val ph = box.height

                PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true).use { cs ->
                    pageEdits.forEach { item ->
                        when (item) {
                            is PageEditItem.SquareErase -> {
                                val left = min(item.normLeft, item.normRight) * pw
                                val right = max(item.normLeft, item.normRight) * pw
                                val topNorm = min(item.normTop, item.normBottom)
                                val bottomNorm = max(item.normTop, item.normBottom)
                                val pdfY = ph * (1f - bottomNorm)
                                val rectH = (bottomNorm - topNorm) * ph
                                val rectW = right - left

                                setNonStrokingArgb(cs, item.fillColorArgb)
                                cs.addRect(box.lowerLeftX + left, box.lowerLeftY + pdfY, rectW, rectH)
                                cs.fill()
                            }

                            is PageEditItem.TextReplacement -> {
                                val left = min(item.normLeft, item.normRight) * pw
                                val right = max(item.normLeft, item.normRight) * pw
                                val topNorm = min(item.normTop, item.normBottom)
                                val bottomNorm = max(item.normTop, item.normBottom)
                                val eraseW = (right - left).coerceAtLeast(24f)
                                val eraseH = ((bottomNorm - topNorm) * ph).coerceAtLeast(item.fontSizePt * 1.35f)
                                val erasePdfY = ph * (1f - bottomNorm)

                                // Erase original text area cleanly
                                setNonStrokingArgb(cs, item.bgColorArgb)
                                cs.addRect(box.lowerLeftX + left - 2f, box.lowerLeftY + erasePdfY - 2f, eraseW + 4f, eraseH + 4f)
                                cs.fill()

                                // Render replacement text with full Bengali + English shaping
                                if (item.newText.isNotBlank()) {
                                    val textBmp = renderCrispTextBitmap(
                                        text = item.newText,
                                        fontSizePt = item.fontSizePt,
                                        colorArgb = item.colorArgb,
                                        typeface = regularTf,
                                        maxWidthPt = (pw - left - 16f).coerceAtLeast(80f)
                                    )
                                    val pdImg = LosslessFactory.createFromImage(doc, textBmp)
                                    val drawW = textBmp.width / 3f
                                    val drawH = textBmp.height / 3f
                                    val topPdfY = ph * (1f - topNorm) - drawH
                                    cs.drawImage(pdImg, box.lowerLeftX + left, box.lowerLeftY + topPdfY, drawW, drawH)
                                    textBmp.recycle()
                                }
                            }

                            is PageEditItem.HighlighterStroke -> {
                                if (item.points.size >= 2) {
                                    cs.saveGraphicsState()
                                    val gs = PDExtendedGraphicsState().apply {
                                        strokingAlphaConstant = 0.38f
                                        nonStrokingAlphaConstant = 0.38f
                                    }
                                    cs.setGraphicsStateParameters(gs)
                                    setStrokingArgb(cs, item.colorArgb)
                                    cs.setLineWidth((item.strokeWidthNorm * pw).coerceAtLeast(8f))
                                    cs.setLineCapStyle(1)
                                    cs.setLineJoinStyle(1)

                                    val first = item.points.first()
                                    cs.moveTo(
                                        box.lowerLeftX + first.x * pw,
                                        box.lowerLeftY + (1f - first.y) * ph
                                    )
                                    item.points.drop(1).forEach { pt ->
                                        cs.lineTo(
                                            box.lowerLeftX + pt.x * pw,
                                            box.lowerLeftY + (1f - pt.y) * ph
                                        )
                                    }
                                    cs.stroke()
                                    cs.restoreGraphicsState()
                                }
                            }

                            is PageEditItem.ShapeItem -> {
                                cs.saveGraphicsState()
                                setStrokingArgb(cs, item.colorArgb)
                                setNonStrokingArgb(cs, item.colorArgb)
                                cs.setLineWidth((item.strokeWidthNorm * pw).coerceAtLeast(1.5f))
                                cs.setLineCapStyle(1)
                                cs.setLineJoinStyle(1)

                                val x1 = box.lowerLeftX + item.startX * pw
                                val y1 = box.lowerLeftY + (1f - item.startY) * ph
                                val x2 = box.lowerLeftX + item.endX * pw
                                val y2 = box.lowerLeftY + (1f - item.endY) * ph

                                when (item.shapeType) {
                                    ShapeType.LINE -> {
                                        cs.moveTo(x1, y1)
                                        cs.lineTo(x2, y2)
                                        cs.stroke()
                                    }
                                    ShapeType.ARROW -> {
                                        cs.moveTo(x1, y1)
                                        cs.lineTo(x2, y2)
                                        cs.stroke()
                                        val angle = atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
                                        val headLen = (item.strokeWidthNorm * pw * 4.5f).coerceIn(10f, 24f)
                                        val wing = Math.toRadians(28.0)
                                        val ax1 = (x2 - headLen * cos(angle - wing)).toFloat()
                                        val ay1 = (y2 - headLen * sin(angle - wing)).toFloat()
                                        val ax2 = (x2 - headLen * cos(angle + wing)).toFloat()
                                        val ay2 = (y2 - headLen * sin(angle + wing)).toFloat()
                                        cs.moveTo(ax1, ay1)
                                        cs.lineTo(x2, y2)
                                        cs.lineTo(ax2, ay2)
                                        cs.stroke()
                                    }
                                    ShapeType.RECTANGLE -> {
                                        val rx = min(x1, x2)
                                        val ry = min(y1, y2)
                                        val rw = kotlin.math.abs(x2 - x1).coerceAtLeast(4f)
                                        val rh = kotlin.math.abs(y2 - y1).coerceAtLeast(4f)
                                        cs.addRect(rx, ry, rw, rh)
                                        if (item.isFilled) cs.fill() else cs.stroke()
                                    }
                                    ShapeType.CIRCLE -> {
                                        val cx = (x1 + x2) / 2f
                                        val cy = (y1 + y2) / 2f
                                        val r = (hypot(x2 - x1, y2 - y1) / 2f).coerceAtLeast(4f)
                                        drawPdfEllipse(cs, cx, cy, r, r)
                                        if (item.isFilled) cs.fill() else cs.stroke()
                                    }
                                    ShapeType.OVAL -> {
                                        val cx = (x1 + x2) / 2f
                                        val cy = (y1 + y2) / 2f
                                        val rx = (kotlin.math.abs(x2 - x1) / 2f).coerceAtLeast(4f)
                                        val ry = (kotlin.math.abs(y2 - y1) / 2f).coerceAtLeast(4f)
                                        drawPdfEllipse(cs, cx, cy, rx, ry)
                                        if (item.isFilled) cs.fill() else cs.stroke()
                                    }
                                }
                                cs.restoreGraphicsState()
                            }

                            is PageEditItem.TextAddition -> {
                                if (item.text.isNotBlank()) {
                                    val tf = if (item.isBold) boldTf else regularTf
                                    val bmp = renderCrispTextBitmap(
                                        text = item.text,
                                        fontSizePt = item.fontSizePt,
                                        colorArgb = item.colorArgb,
                                        typeface = tf,
                                        maxWidthPt = (pw * (1f - item.normX)).coerceAtLeast(90f)
                                    )
                                    val pdImg = LosslessFactory.createFromImage(doc, bmp)
                                    val drawW = bmp.width / 3f
                                    val drawH = bmp.height / 3f
                                    val pdfX = box.lowerLeftX + item.normX * pw
                                    val pdfY = box.lowerLeftY + (1f - item.normY) * ph - drawH
                                    cs.drawImage(pdImg, pdfX, pdfY, drawW, drawH)
                                    bmp.recycle()
                                }
                            }

                            is PageEditItem.ImageAddition -> {
                                val imgFile = File(item.imagePath)
                                if (imgFile.exists()) {
                                    BitmapFactory.decodeFile(imgFile.absolutePath)?.let { bmp ->
                                        val pdImg = LosslessFactory.createFromImage(doc, bmp)
                                        val drawW = item.normWidth * pw
                                        val drawH = item.normHeight * ph
                                        val pdfX = box.lowerLeftX + item.normX * pw
                                        val pdfY = box.lowerLeftY + (1f - item.normY - item.normHeight) * ph
                                        cs.drawImage(pdImg, pdfX, pdfY, drawW, drawH)
                                        bmp.recycle()
                                    }
                                }
                            }

                            is PageEditItem.SignatureAddition -> {
                                val sigFile = File(item.signaturePath)
                                if (sigFile.exists()) {
                                    BitmapFactory.decodeFile(sigFile.absolutePath)?.let { bmp ->
                                        val pdImg = LosslessFactory.createFromImage(doc, bmp)
                                        val drawW = item.normWidth * pw
                                        val drawH = item.normHeight * ph
                                        val pdfX = box.lowerLeftX + item.normX * pw
                                        val pdfY = box.lowerLeftY + (1f - item.normY - item.normHeight) * ph
                                        cs.drawImage(pdImg, pdfX, pdfY, drawW, drawH)
                                        bmp.recycle()
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (password.isNotEmpty()) {
                val spp = StandardProtectionPolicy(password, password, AccessPermission()).apply {
                    encryptionKeyLength = 128
                }
                doc.protect(spp)
            }
            doc.save(targetFile)
        }

        if (!saveAsNewCopy) {
            targetFile.copyTo(sourceFile, overwrite = true)
            targetFile.delete()
            sourceFile
        } else {
            targetFile
        }
    }

    suspend fun mergePdfs(
        context: Context,
        filesToMerge: List<File>,
        outputTitle: String,
        passwords: Map<String, String> = emptyMap()
    ): ConversionOutput = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        require(filesToMerge.size >= 2) { "মার্জ করার জন্য কমপক্ষে ২টি PDF ফাইল নির্বাচন করুন।" }

        val cleanTitle = outputTitle.trim().ifBlank { "Merged_Nokshi_PDF" }
        val outDir = File(context.filesDir, "workspace_pdfs").apply { mkdirs() }
        val outFile = File(outDir, "${cleanTitle}_${System.currentTimeMillis()}.pdf")

        val mergedDoc = PDDocument()
        val openedDocs = mutableListOf<PDDocument>()
        try {
            val merger = PDFMergerUtility()
            for (src in filesToMerge) {
                val pwd = passwords[src.absolutePath] ?: ""
                val srcDoc = PDDocument.load(src, pwd)
                srcDoc.isAllSecurityToBeRemoved = true
                openedDocs.add(srcDoc)
                merger.appendDocument(mergedDoc, srcDoc)
            }
            mergedDoc.save(outFile)
            val totalPages = mergedDoc.numberOfPages
            ConversionOutput(outFile, cleanTitle, totalPages, "MERGED")
        } finally {
            openedDocs.forEach { runCatching { it.close() } }
            runCatching { mergedDoc.close() }
        }
    }

    suspend fun protectPdfWithPassword(
        context: Context,
        sourceFile: File,
        currentPassword: String = "",
        newPassword: String
    ): File = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        require(newPassword.isNotBlank()) { "একটি পাসওয়ার্ড লিখুন।" }
        val outFile = File(
            sourceFile.parentFile,
            "${sourceFile.nameWithoutExtension}_locked_${System.currentTimeMillis()}.pdf"
        )
        PDDocument.load(sourceFile, currentPassword).use { doc ->
            doc.isAllSecurityToBeRemoved = true
            val ap = AccessPermission().apply {
                setCanPrint(true)
                setCanModify(false)
            }
            val spp = StandardProtectionPolicy(newPassword, newPassword, ap).apply {
                encryptionKeyLength = 128
            }
            doc.protect(spp)
            doc.save(outFile)
        }
        outFile
    }

    suspend fun removePdfPassword(
        context: Context,
        sourceFile: File,
        currentPassword: String
    ): File = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        val outFile = File(
            sourceFile.parentFile,
            "${sourceFile.nameWithoutExtension}_unlocked_${System.currentTimeMillis()}.pdf"
        )
        PDDocument.load(sourceFile, currentPassword).use { doc ->
            doc.isAllSecurityToBeRemoved = true
            doc.save(outFile)
        }
        outFile
    }

    suspend fun convertPdfToImages(
        context: Context,
        file: File,
        password: String = "",
        highQuality: Boolean = true,
        asPng: Boolean = true
    ): List<ConvertedPageImage> = withContext(Dispatchers.IO) {
        ensureInitialized(context)
        val outDir = File(context.cacheDir, "exported_pdf_images_${System.currentTimeMillis()}").apply { mkdirs() }
        val scale = if (highQuality) 2.5f else 1.5f
        val results = mutableListOf<ConvertedPageImage>()

        PDDocument.load(file, password).use { doc ->
            val renderer = PDFRenderer(doc)
            for (i in 0 until doc.numberOfPages) {
                val bmp = renderer.renderImage(i, scale, ImageType.RGB)
                val ext = if (asPng) "png" else "jpg"
                val imgFile = File(outDir, "${file.nameWithoutExtension}_page_${i + 1}.$ext")
                FileOutputStream(imgFile).use { fos ->
                    if (asPng) {
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    } else {
                        bmp.compress(Bitmap.CompressFormat.JPEG, 92, fos)
                    }
                }
                results.add(
                    ConvertedPageImage(
                        pageNumber = i + 1,
                        filePath = imgFile.absolutePath,
                        width = bmp.width,
                        height = bmp.height,
                        formatLabel = ext.uppercase()
                    )
                )
                bmp.recycle()
            }
        }
        results
    }

    suspend fun saveImageFileToGallery(context: Context, imageFile: File): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val isPng = imageFile.extension.equals("png", true)
            val mime = if (isPng) "image/png" else "image/jpeg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, imageFile.name)
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/NokshiPDF")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@runCatching false
            resolver.openOutputStream(uri)?.use { out ->
                imageFile.inputStream().use { it.copyTo(out) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        }.getOrDefault(false)
    }

    private fun renderCrispTextBitmap(
        text: String,
        fontSizePt: Float,
        colorArgb: Int,
        typeface: Typeface,
        maxWidthPt: Float
    ): Bitmap {
        val densityScale = 3f
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorArgb
            textSize = fontSizePt * densityScale
            this.typeface = typeface
        }
        val maxPx = (maxWidthPt * densityScale).toInt().coerceAtLeast(180)
        val desiredPx = StaticLayout.getDesiredWidth(text, tp).toInt() + 12
        val layoutWidth = min(maxPx, desiredPx.coerceAtLeast(48))

        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, tp, layoutWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(2f, 1.1f)
            .build()

        val bmpW = layout.width.coerceAtLeast(12)
        val bmpH = (layout.height + 6).coerceAtLeast(12)
        val bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.TRANSPARENT)
        layout.draw(canvas)
        return bmp
    }

    private fun setStrokingArgb(cs: PDPageContentStream, argb: Int) {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        cs.setStrokingColor(r, g, b)
    }

    private fun setNonStrokingArgb(cs: PDPageContentStream, argb: Int) {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        cs.setNonStrokingColor(r, g, b)
    }

    private fun drawPdfEllipse(
        cs: PDPageContentStream,
        cx: Float,
        cy: Float,
        rx: Float,
        ry: Float
    ) {
        val k = 0.55228475f
        val ox = rx * k
        val oy = ry * k
        cs.moveTo(cx - rx, cy)
        cs.curveTo(cx - rx, cy + oy, cx - ox, cy + ry, cx, cy + ry)
        cs.curveTo(cx + ox, cy + ry, cx + rx, cy + oy, cx + rx, cy)
        cs.curveTo(cx + rx, cy - oy, cx + ox, cy - ry, cx, cy - ry)
        cs.curveTo(cx - ox, cy - ry, cx - rx, cy - oy, cx - rx, cy)
        cs.closePath()
    }
}
