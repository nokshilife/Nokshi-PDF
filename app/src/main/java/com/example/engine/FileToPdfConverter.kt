package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.tom_roush.pdfbox.pdmodel.graphics.state.RenderingMode
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class ConversionOutput(
    val file: File,
    val title: String,
    val pageCount: Int,
    val category: String
)

private data class SearchableLineEntry(
    val text: String,
    val xPt: Float,
    val topYPt: Float,
    val fontSizePt: Float
)

object FileToPdfConverter {

    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842
    private const val SLIDE_WIDTH = 842
    private const val SLIDE_HEIGHT = 474
    private const val RENDER_SCALE = 2f

    fun resolveDisplayName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                val raw = cursor.getString(idx)
                if (!raw.isNullOrBlank()) return raw
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "Nokshi_Document"
    }

    private fun workspaceDir(context: Context): File {
        return File(context.filesDir, "workspace_pdfs").apply { mkdirs() }
    }

    private fun loadBengaliCapableTypeface(context: Context, bold: Boolean = false): Typeface {
        val base = runCatching {
            ResourcesCompat.getFont(context, R.font.hind_siliguri)
        }.getOrNull() ?: Typeface.SANS_SERIF
        return Typeface.create(base, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private class HighResPageCanvas(val widthPt: Int, val heightPt: Int) {
        val bitmap: Bitmap = Bitmap.createBitmap(
            (widthPt * RENDER_SCALE).toInt(),
            (heightPt * RENDER_SCALE).toInt(),
            Bitmap.Config.ARGB_8888
        )
        val canvas: Canvas = Canvas(bitmap).apply {
            scale(RENDER_SCALE, RENDER_SCALE)
            drawColor(Color.WHITE)
        }
        val searchableLines = mutableListOf<SearchableLineEntry>()
    }

    private fun appendCanvasPageToPdDocument(
        context: Context,
        pdDoc: PDDocument,
        pageCanvas: HighResPageCanvas,
        unicodeFont: PDType0Font?
    ) {
        val pw = pageCanvas.widthPt.toFloat()
        val ph = pageCanvas.heightPt.toFloat()
        val pdPage = PDPage(PDRectangle(pw, ph))
        pdDoc.addPage(pdPage)

        val pdImg = JPEGFactory.createFromImage(pdDoc, pageCanvas.bitmap, 0.92f)
        PDPageContentStream(pdDoc, pdPage).use { cs ->
            cs.drawImage(pdImg, 0f, 0f, pw, ph)

            // Embed invisible searchable text layer so Edit Text & Stripper detect exact line positions
            for (entry in pageCanvas.searchableLines) {
                val clean = entry.text.replace('\n', ' ').trim()
                if (clean.isEmpty()) continue
                val pdfY = (ph - entry.topYPt - entry.fontSizePt).coerceIn(8f, ph - 8f)
                val pdfX = entry.xPt.coerceIn(4f, pw - 20f)

                runCatching {
                    cs.beginText()
                    cs.setRenderingMode(RenderingMode.NEITHER)
                    if (unicodeFont != null) {
                        cs.setFont(unicodeFont, entry.fontSizePt)
                        cs.newLineAtOffset(pdfX, pdfY)
                        cs.showText(clean)
                    } else {
                        val asciiOnly = clean.filter { it.code in 32..126 }
                        if (asciiOnly.isNotEmpty()) {
                            cs.setFont(PDType1Font.HELVETICA, entry.fontSizePt)
                            cs.newLineAtOffset(pdfX, pdfY)
                            cs.showText(asciiOnly)
                        }
                    }
                    cs.endText()
                }
            }
        }
        pageCanvas.bitmap.recycle()
    }

    private fun loadPdUnicodeFont(context: Context, pdDoc: PDDocument): PDType0Font? {
        return runCatching {
            context.resources.openRawResource(R.font.hind_siliguri).use { stream ->
                PDType0Font.load(pdDoc, stream, true)
            }
        }.getOrNull()
    }

    suspend fun importExistingPdf(context: Context, uri: Uri): ConversionOutput = withContext(Dispatchers.IO) {
        val rawName = resolveDisplayName(context, uri)
        val cleanTitle = rawName.substringBeforeLast('.').ifBlank { "Imported_PDF" }
        val target = File(workspaceDir(context), "${cleanTitle}_${System.currentTimeMillis()}.pdf")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(target).use { output -> input.copyTo(output) }
        } ?: error("ফাইলটি ওপেন করা যায়নি।")

        val info = PdfEditAndSecurityEngine.inspectPdf(context, target)
        ConversionOutput(
            file = target,
            title = cleanTitle,
            pageCount = info.pageCount.coerceAtLeast(1),
            category = "PDF"
        )
    }

    suspend fun convertImagesToPdf(
        context: Context,
        uris: List<Uri>,
        customTitle: String? = null
    ): ConversionOutput = withContext(Dispatchers.IO) {
        PdfEditAndSecurityEngine.ensureInitialized(context)
        require(uris.isNotEmpty()) { "কমপক্ষে একটি ইমেজ নির্বাচন করুন।" }
        val firstLabel = customTitle?.takeIf { it.isNotBlank() }
            ?: resolveDisplayName(context, uris.first()).substringBeforeLast('.')
        val outFile = File(workspaceDir(context), "${firstLabel}_img_${System.currentTimeMillis()}.pdf")

        PDDocument().use { pdDoc ->
            var createdPages = 0
            uris.forEach { uri ->
                val bmp = decodeSampledBitmap(context, uri, 1800) ?: return@forEach
                val page = PDPage(PDRectangle.A4)
                pdDoc.addPage(page)
                val pw = PDRectangle.A4.width
                val ph = PDRectangle.A4.height
                val margin = 28f
                val scale = minOf((pw - margin * 2) / bmp.width, (ph - margin * 2) / bmp.height)
                val dw = bmp.width * scale
                val dh = bmp.height * scale
                val dx = (pw - dw) / 2f
                val dy = (ph - dh) / 2f

                val pdImg = LosslessFactory.createFromImage(pdDoc, bmp)
                PDPageContentStream(pdDoc, page).use { cs ->
                    cs.drawImage(pdImg, dx, dy, dw, dh)
                }
                bmp.recycle()
                createdPages++
            }
            require(createdPages > 0) { "ইমেজগুলো পড়া সম্ভব হয়নি।" }
            pdDoc.save(outFile)
            ConversionOutput(outFile, firstLabel, createdPages, "IMAGE")
        }
    }

    suspend fun convertDocumentToPdf(context: Context, uri: Uri): ConversionOutput = withContext(Dispatchers.IO) {
        val rawName = resolveDisplayName(context, uri)
        val ext = rawName.substringAfterLast('.', "").lowercase()
        val title = rawName.substringBeforeLast('.').ifBlank { "Document" }

        if (ext == "pdf") return@withContext importExistingPdf(context, uri)

        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("ডকুমেন্ট ফাইলটি পড়া যায়নি।")

        val extracted = when {
            ext == "docx" || isZipArchive(bytes) -> parseDocxContent(bytes)
            ext == "html" || ext == "htm" -> DocParsedContent(
                paragraphs = stripHtmlTags(String(bytes, Charsets.UTF_8)).lines(),
                images = emptyList()
            )
            ext == "rtf" -> DocParsedContent(
                paragraphs = stripRtfControlWords(String(bytes, Charsets.UTF_8)).lines(),
                images = emptyList()
            )
            else -> DocParsedContent(
                paragraphs = extractReadableText(bytes).lines(),
                images = emptyList()
            )
        }

        renderDocumentContentToPdf(context, title, extracted, "DOCUMENT")
    }

    suspend fun createPdfFromWrittenText(
        context: Context,
        title: String,
        body: String
    ): ConversionOutput = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim().ifBlank { "নতুন ডকুমেন্ট" }
        val content = DocParsedContent(
            paragraphs = body.lines().ifEmpty { listOf("") },
            images = emptyList()
        )
        renderDocumentContentToPdf(context, cleanTitle, content, "DOCUMENT")
    }

    suspend fun convertExcelToPdf(context: Context, uri: Uri): ConversionOutput = withContext(Dispatchers.IO) {
        val rawName = resolveDisplayName(context, uri)
        val ext = rawName.substringAfterLast('.', "").lowercase()
        val title = rawName.substringBeforeLast('.').ifBlank { "Spreadsheet" }
        if (ext == "pdf") return@withContext importExistingPdf(context, uri)

        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("এক্সেল ফাইলটি পড়া যায়নি।")

        val sheets: List<ParsedSheet> = if (ext == "xlsx" || isZipArchive(bytes)) {
            parseXlsxSheets(bytes)
        } else {
            parseCsvOrTextSheet(title, bytes)
        }

        renderSheetsToPdf(context, title, sheets)
    }

    suspend fun convertPresentationToPdf(context: Context, uri: Uri): ConversionOutput = withContext(Dispatchers.IO) {
        val rawName = resolveDisplayName(context, uri)
        val ext = rawName.substringAfterLast('.', "").lowercase()
        val title = rawName.substringBeforeLast('.').ifBlank { "Presentation" }
        if (ext == "pdf") return@withContext importExistingPdf(context, uri)

        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("প্রেজেন্টেশন ফাইলটি পড়া যায়নি।")

        val slides: List<ParsedSlide> = if (ext == "pptx" || isZipArchive(bytes)) {
            parsePptxSlides(bytes)
        } else {
            val lines = extractReadableText(bytes).lines().filter { it.isNotBlank() }
            lines.chunked(6).mapIndexed { idx, chunk ->
                ParsedSlide(
                    slideNumber = idx + 1,
                    heading = chunk.firstOrNull() ?: "$title - Slide ${idx + 1}",
                    bullets = chunk.drop(1),
                    imageBytes = null
                )
            }.ifEmpty {
                listOf(ParsedSlide(1, title, listOf("প্রেজেন্টেশন স্লাইড কনভার্ট করা হয়েছে।"), null))
            }
        }

        renderSlidesToPdf(context, title, slides)
    }

    suspend fun createSampleNokshiPdf(context: Context): ConversionOutput = withContext(Dispatchers.IO) {
        PdfEditAndSecurityEngine.ensureInitialized(context)
        val title = "নকশি পিডিএফ গাইড ও নমুনা"
        val outFile = File(workspaceDir(context), "Nokshi_Sample_${System.currentTimeMillis()}.pdf")
        val regularTf = loadBengaliCapableTypeface(context, false)
        val boldTf = loadBengaliCapableTypeface(context, true)

        PDDocument().use { pdDoc ->
            val unicodeFont = loadPdUnicodeFont(context, pdDoc)

            // Page 1
            val p1 = HighResPageCanvas(A4_WIDTH, A4_HEIGHT)
            val c1 = p1.canvas
            c1.drawColor(Color.parseColor("#FFFDF9"))

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#8E1D2A")
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
            }
            c1.drawRect(24f, 24f, A4_WIDTH - 24f, A4_HEIGHT - 24f, borderPaint)

            val headerFill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#8E1D2A") }
            c1.drawRoundRect(RectF(40f, 42f, A4_WIDTH - 40f, 118f), 12f, 12f, headerFill)

            val headerTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 22f
                typeface = boldTf
            }
            c1.drawText("Nokshi PDF • নকশি পিডিএফ স্টুডিও", 58f, 76f, headerTitlePaint)
            p1.searchableLines.add(SearchableLineEntry("Nokshi PDF • নকশি পিডিএফ স্টুডিও", 58f, 56f, 20f))

            headerTitlePaint.textSize = 13f
            headerTitlePaint.typeface = regularTf
            c1.drawText("ডকুমেন্ট কনভার্টার, স্মার্ট এডিটর, সিগনেচার ও Google Lens OCR", 58f, 100f, headerTitlePaint)

            val bodyLines = listOf(
                "স্বাগতম! এটি একটি নমুনা PDF ফাইল যা দিয়ে আপনি সব ফিচার পরীক্ষা করতে পারবেন:",
                "১. লেখা পরিবর্তন: যেকোনো লাইনে ট্যাপ করে লেখা পরিবর্তন করুন।",
                "২. নতুন টেক্সট ও ইমেজ যোগ: পছন্দের রঙ ও সাইজে বাংলা বা ইংরেজি টেক্সট বসান।",
                "৩. স্কয়ার ইরেজার: যেকোনো অংশে চারকোণা মার্ক করে মুছে ফেলুন।",
                "৪. শেপ ও সিগনেচার: লাইন, অ্যারো, সার্কেল, ওভাল, রেকট্যাঙ্গেল ও স্বাক্ষর যুক্ত করুন।",
                "৫. হাইলাইটার ও কালার পিকার: গুরুত্বপূর্ণ লাইনগুলো রঙিন হাইলাইটার দিয়ে চিহ্নিত করুন।",
                "",
                "Sample English Section for Instant OCR & Editing:",
                "Invoice Reference: NK-2026-0894",
                "Client Name: Nokshi Craft Studio Dhaka",
                "Project Summary: Complete Digital Document Transformation.",
                "Total Payable Amount: BDT 24,500.00 (Verified)"
            )

            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#22191A")
                textSize = 14f
                typeface = regularTf
            }
            var yPos = 150f
            bodyLines.forEach { line ->
                if (line.isBlank()) {
                    yPos += 14f
                } else {
                    val layout = StaticLayout.Builder
                        .obtain(line, 0, line.length, textPaint, A4_WIDTH - 96)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(4f, 1.15f)
                        .build()
                    c1.save()
                    c1.translate(48f, yPos)
                    layout.draw(c1)
                    c1.restore()
                    p1.searchableLines.add(SearchableLineEntry(line, 48f, yPos, 13.5f))
                    yPos += layout.height + 10f
                }
            }

            val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1F3A60")
                style = Paint.Style.STROKE
                strokeWidth = 1.4f
            }
            c1.drawRoundRect(RectF(360f, 700f, 540f, 775f), 8f, 8f, boxPaint)
            val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1F3A60")
                textSize = 12f
                typeface = boldTf
            }
            c1.drawText("এখানে সিগনেচার যুক্ত করুন", 382f, 795f, labelPaint)
            appendCanvasPageToPdDocument(context, pdDoc, p1, unicodeFont)

            // Page 2
            val p2 = HighResPageCanvas(A4_WIDTH, A4_HEIGHT)
            val c2 = p2.canvas
            c2.drawRect(24f, 24f, A4_WIDTH - 24f, A4_HEIGHT - 24f, borderPaint)
            val p2Head = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#8E1D2A")
                textSize = 18f
                typeface = boldTf
            }
            c2.drawText("বাজেট ও কার্যবিবরণী টেবিল (Page 2)", 48f, 68f, p2Head)
            p2.searchableLines.add(SearchableLineEntry("বাজেট ও কার্যবিবরণী টেবিল (Page 2)", 48f, 50f, 17f))

            val rows = listOf(
                listOf("ক্রমিক", "বিবরণ (Item)", "পরিমাণ", "মূল্য (BDT)"),
                listOf("01", "Nokshi Kantha Cover Design", "10 Pcs", "8,500"),
                listOf("02", "Annual Report Formatting", "04 Files", "6,000"),
                listOf("03", "OCR Archive Digitization", "120 Pages", "10,000")
            )
            var tableY = 100f
            val colWidths = floatArrayOf(70f, 235f, 95f, 100f)
            rows.forEachIndexed { rIdx, row ->
                var colX = 48f
                val rowH = 36f
                val bgPaint = Paint().apply {
                    color = when {
                        rIdx == 0 -> Color.parseColor("#1F3A60")
                        rIdx % 2 == 1 -> Color.parseColor("#F8F4EC")
                        else -> Color.WHITE
                    }
                }
                c2.drawRect(48f, tableY, 548f, tableY + rowH, bgPaint)
                val cellBorder = Paint().apply {
                    color = Color.parseColor("#CBD5E1")
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }
                val cellText = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (rIdx == 0) Color.WHITE else Color.parseColor("#1E293B")
                    textSize = 12.5f
                    typeface = if (rIdx == 0) boldTf else regularTf
                }
                row.forEachIndexed { cIdx, cell ->
                    val w = colWidths[cIdx]
                    c2.drawRect(colX, tableY, colX + w, tableY + rowH, cellBorder)
                    c2.drawText(cell, colX + 10f, tableY + 23f, cellText)
                    colX += w
                }
                p2.searchableLines.add(SearchableLineEntry(row.joinToString(" | "), 54f, tableY + 8f, 12f))
                tableY += rowH
            }
            appendCanvasPageToPdDocument(context, pdDoc, p2, unicodeFont)
            pdDoc.save(outFile)
        }

        ConversionOutput(outFile, title, 2, "SAMPLE")
    }

    private data class DocParsedContent(
        val paragraphs: List<String>,
        val images: List<ByteArray>
    )

    private data class ParsedSheet(
        val name: String,
        val rows: List<List<String>>
    )

    private data class ParsedSlide(
        val slideNumber: Int,
        val heading: String,
        val bullets: List<String>,
        val imageBytes: ByteArray?
    )

    private fun isZipArchive(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte() &&
            bytes[2] == 0x03.toByte() &&
            bytes[3] == 0x04.toByte()
    }

    private fun readZipEntries(bytes: ByteArray): Map<String, ByteArray> {
        val entries = LinkedHashMap<String, ByteArray>()
        runCatching {
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && entry.size < 15_000_000L) {
                        entries[entry.name] = zis.readBytes()
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
        return entries
    }

    private fun parseDocxContent(bytes: ByteArray): DocParsedContent {
        val entries = readZipEntries(bytes)
        val docXml = entries["word/document.xml"]
        val paragraphs = mutableListOf<String>()
        if (docXml != null) {
            runCatching {
                val parser = XmlPullParserFactory.newInstance().newPullParser()
                parser.setInput(ByteArrayInputStream(docXml), "UTF-8")
                var event = parser.eventType
                val currentPara = StringBuilder()
                while (event != XmlPullParser.END_DOCUMENT) {
                    val name = parser.name?.substringAfterLast(':') ?: ""
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            if (name == "tab") currentPara.append("    ")
                            if (name == "br") currentPara.append("\n")
                            if (name == "tc" && currentPara.isNotEmpty()) currentPara.append("  |  ")
                        }
                        XmlPullParser.TEXT -> currentPara.append(parser.text ?: "")
                        XmlPullParser.END_TAG -> {
                            if (name == "p") {
                                paragraphs.add(currentPara.toString().trimEnd())
                                currentPara.clear()
                            }
                        }
                    }
                    event = parser.next()
                }
                if (currentPara.isNotEmpty()) paragraphs.add(currentPara.toString())
            }
        }
        val media = entries.filterKeys {
            it.startsWith("word/media/") &&
                (it.endsWith(".png", true) || it.endsWith(".jpg", true) || it.endsWith(".jpeg", true))
        }.values.take(8)

        if (paragraphs.all { it.isBlank() } && media.isEmpty()) {
            return DocParsedContent(listOf("ডকুমেন্ট কনভার্ট সম্পন্ন হয়েছে।"), emptyList())
        }
        return DocParsedContent(paragraphs, media)
    }

    private fun parseXlsxSheets(bytes: ByteArray): List<ParsedSheet> {
        val entries = readZipEntries(bytes)
        val sharedStrings = mutableListOf<String>()
        entries["xl/sharedStrings.xml"]?.let { ssXml ->
            runCatching {
                val parser = XmlPullParserFactory.newInstance().newPullParser()
                parser.setInput(ByteArrayInputStream(ssXml), "UTF-8")
                var event = parser.eventType
                var inSi = false
                val sb = StringBuilder()
                while (event != XmlPullParser.END_DOCUMENT) {
                    val tag = parser.name?.substringAfterLast(':') ?: ""
                    when (event) {
                        XmlPullParser.START_TAG -> if (tag == "si") {
                            inSi = true
                            sb.clear()
                        }
                        XmlPullParser.TEXT -> if (inSi) sb.append(parser.text ?: "")
                        XmlPullParser.END_TAG -> if (tag == "si") {
                            sharedStrings.add(sb.toString())
                            inSi = false
                        }
                    }
                    event = parser.next()
                }
            }
        }

        val sheetEntries = entries.keys.filter { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }.sorted()
        val parsedSheets = sheetEntries.mapIndexedNotNull { idx, key ->
            val xml = entries[key] ?: return@mapIndexedNotNull null
            val rows = mutableListOf<List<String>>()
            runCatching {
                val parser = XmlPullParserFactory.newInstance().newPullParser()
                parser.setInput(ByteArrayInputStream(xml), "UTF-8")
                var event = parser.eventType
                var currentRow = mutableMapOf<Int, String>()
                var cellType = ""
                var cellCol = 0
                var inValue = false
                var inInline = false
                val valBuf = StringBuilder()

                while (event != XmlPullParser.END_DOCUMENT) {
                    val tag = parser.name?.substringAfterLast(':') ?: ""
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            when (tag) {
                                "row" -> currentRow = mutableMapOf()
                                "c" -> {
                                    cellType = parser.getAttributeValue(null, "t") ?: ""
                                    val ref = parser.getAttributeValue(null, "r") ?: ""
                                    cellCol = colRefToIndex(ref)
                                    valBuf.clear()
                                }
                                "v" -> inValue = true
                                "is" -> inInline = true
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inValue || inInline) valBuf.append(parser.text ?: "")
                        }
                        XmlPullParser.END_TAG -> {
                            when (tag) {
                                "v" -> inValue = false
                                "is" -> inInline = false
                                "c" -> {
                                    val raw = valBuf.toString().trim()
                                    val resolved = if (cellType == "s") {
                                        raw.toIntOrNull()?.let { sharedStrings.getOrNull(it) } ?: raw
                                    } else raw
                                    val targetCol = if (cellCol >= 0) cellCol else currentRow.size
                                    currentRow[targetCol] = resolved
                                }
                                "row" -> {
                                    if (currentRow.isNotEmpty()) {
                                        val colLimit = (currentRow.keys.maxOrNull() ?: 0) + 1
                                        val list = (0 until colLimit.coerceAtMost(12)).map { currentRow[it] ?: "" }
                                        if (list.any { it.isNotBlank() }) rows.add(list)
                                    }
                                }
                            }
                        }
                    }
                    event = parser.next()
                }
            }
            if (rows.isEmpty()) null else ParsedSheet("Sheet ${idx + 1}", rows)
        }

        return parsedSheets.ifEmpty {
            listOf(ParsedSheet("Sheet 1", listOf(listOf("এক্সেল শীটে কোনো দৃশ্যমান ডাটা পাওয়া যায়নি।"))))
        }
    }

    private fun colRefToIndex(ref: String): Int {
        val letters = ref.takeWhile { it.isLetter() }.uppercase()
        if (letters.isEmpty()) return -1
        var result = 0
        for (ch in letters) {
            result = result * 26 + (ch - 'A' + 1)
        }
        return result - 1
    }

    private fun parseCsvOrTextSheet(title: String, bytes: ByteArray): List<ParsedSheet> {
        val text = extractReadableText(bytes)
        val delimiter = if (text.contains('\t')) '\t' else ','
        val rows = BufferedReader(InputStreamReader(ByteArrayInputStream(text.toByteArray())))
            .readLines()
            .filter { it.isNotBlank() }
            .map { line -> line.split(delimiter).map { it.trim().trim('"') } }
            .ifEmpty { listOf(listOf(title)) }
        return listOf(ParsedSheet(title, rows))
    }

    private fun parsePptxSlides(bytes: ByteArray): List<ParsedSlide> {
        val entries = readZipEntries(bytes)
        val slideKeys = entries.keys
            .filter { it.startsWith("ppt/slides/slide") && it.endsWith(".xml") }
            .sortedBy { key ->
                key.removePrefix("ppt/slides/slide").removeSuffix(".xml").toIntOrNull() ?: 999
            }
        val mediaImages = entries.entries
            .filter {
                it.key.startsWith("ppt/media/") &&
                    (it.key.endsWith(".png", true) || it.key.endsWith(".jpg", true) || it.key.endsWith(".jpeg", true))
            }
            .map { it.value }

        val slides = slideKeys.mapIndexed { idx, key ->
            val xml = entries[key] ?: ByteArray(0)
            val paragraphs = mutableListOf<String>()
            runCatching {
                val parser = XmlPullParserFactory.newInstance().newPullParser()
                parser.setInput(ByteArrayInputStream(xml), "UTF-8")
                var event = parser.eventType
                val pBuf = StringBuilder()
                var inText = false
                while (event != XmlPullParser.END_DOCUMENT) {
                    val tag = parser.name?.substringAfterLast(':') ?: ""
                    when (event) {
                        XmlPullParser.START_TAG -> if (tag == "t") inText = true
                        XmlPullParser.TEXT -> if (inText) pBuf.append(parser.text ?: "")
                        XmlPullParser.END_TAG -> {
                            if (tag == "t") inText = false
                            if (tag == "p") {
                                val line = pBuf.toString().trim()
                                if (line.isNotEmpty()) paragraphs.add(line)
                                pBuf.clear()
                            }
                        }
                    }
                    event = parser.next()
                }
            }
            ParsedSlide(
                slideNumber = idx + 1,
                heading = paragraphs.firstOrNull() ?: "Slide ${idx + 1}",
                bullets = paragraphs.drop(1),
                imageBytes = mediaImages.getOrNull(idx)
            )
        }

        return slides.ifEmpty {
            listOf(ParsedSlide(1, "Presentation", listOf("স্লাইড ফাইলটি PDF হিসেবে কনভার্ট হয়েছে।"), null))
        }
    }

    private fun renderDocumentContentToPdf(
        context: Context,
        title: String,
        content: DocParsedContent,
        category: String
    ): ConversionOutput {
        PdfEditAndSecurityEngine.ensureInitialized(context)
        val outFile = File(workspaceDir(context), "${title}_doc_${System.currentTimeMillis()}.pdf")
        val regularTf = loadBengaliCapableTypeface(context, false)
        val boldTf = loadBengaliCapableTypeface(context, true)

        val margin = 46f
        val contentWidth = (A4_WIDTH - margin * 2).toInt()
        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E1E1E")
            textSize = 13.5f
            typeface = regularTf
        }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8E1D2A")
            textSize = 18f
            typeface = boldTf
        }

        PDDocument().use { pdDoc ->
            val unicodeFont = loadPdUnicodeFont(context, pdDoc)
            var pageNum = 1
            var pageCanvas = HighResPageCanvas(A4_WIDTH, A4_HEIGHT)

            val headerLayout = StaticLayout.Builder
                .obtain(title, 0, title.length, titlePaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
            pageCanvas.canvas.save()
            pageCanvas.canvas.translate(margin, margin)
            headerLayout.draw(pageCanvas.canvas)
            pageCanvas.canvas.restore()
            pageCanvas.searchableLines.add(SearchableLineEntry(title, margin, margin, 18f))

            var cursorY = margin + headerLayout.height + 18f
            val dividerPaint = Paint().apply {
                color = Color.parseColor("#E2D6CE")
                strokeWidth = 1.2f
            }
            pageCanvas.canvas.drawLine(margin, cursorY - 8f, A4_WIDTH - margin, cursorY - 8f, dividerPaint)

            for (para in content.paragraphs) {
                if (para.isBlank()) {
                    cursorY += 10f
                    continue
                }
                val layout = StaticLayout.Builder
                    .obtain(para, 0, para.length, bodyPaint, contentWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(3f, 1.15f)
                    .build()

                if (cursorY + layout.height > A4_HEIGHT - margin) {
                    appendCanvasPageToPdDocument(context, pdDoc, pageCanvas, unicodeFont)
                    pageNum++
                    pageCanvas = HighResPageCanvas(A4_WIDTH, A4_HEIGHT)
                    cursorY = margin
                }
                pageCanvas.canvas.save()
                pageCanvas.canvas.translate(margin, cursorY)
                layout.draw(pageCanvas.canvas)
                pageCanvas.canvas.restore()
                pageCanvas.searchableLines.add(SearchableLineEntry(para, margin, cursorY, 13.5f))
                cursorY += layout.height + 8f
            }

            for (imgBytes in content.images) {
                val bmp = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size) ?: continue
                val maxH = 260f
                val scale = minOf(contentWidth / bmp.width.toFloat(), maxH / bmp.height.toFloat(), 1f)
                val drawW = bmp.width * scale
                val drawH = bmp.height * scale
                if (cursorY + drawH > A4_HEIGHT - margin) {
                    appendCanvasPageToPdDocument(context, pdDoc, pageCanvas, unicodeFont)
                    pageNum++
                    pageCanvas = HighResPageCanvas(A4_WIDTH, A4_HEIGHT)
                    cursorY = margin
                }
                pageCanvas.canvas.drawBitmap(
                    bmp,
                    null,
                    RectF(margin, cursorY, margin + drawW, cursorY + drawH),
                    Paint(Paint.FILTER_BITMAP_FLAG)
                )
                bmp.recycle()
                cursorY += drawH + 14f
            }

            appendCanvasPageToPdDocument(context, pdDoc, pageCanvas, unicodeFont)
            pdDoc.save(outFile)
            return ConversionOutput(outFile, title, pageNum, category)
        }
    }

    private fun renderSheetsToPdf(
        context: Context,
        title: String,
        sheets: List<ParsedSheet>
    ): ConversionOutput {
        PdfEditAndSecurityEngine.ensureInitialized(context)
        val outFile = File(workspaceDir(context), "${title}_xls_${System.currentTimeMillis()}.pdf")
        val regularTf = loadBengaliCapableTypeface(context, false)
        val boldTf = loadBengaliCapableTypeface(context, true)
        val pageW = A4_HEIGHT
        val pageH = A4_WIDTH
        val margin = 34f
        var pageNum = 0

        PDDocument().use { pdDoc ->
            val unicodeFont = loadPdUnicodeFont(context, pdDoc)
            sheets.forEach { sheet ->
                val colCount = (sheet.rows.maxOfOrNull { it.size } ?: 1).coerceIn(1, 10)
                val tableW = pageW - margin * 2
                val colW = tableW / colCount

                var rowIndex = 0
                while (rowIndex < sheet.rows.size || rowIndex == 0) {
                    pageNum++
                    val pc = HighResPageCanvas(pageW, pageH)
                    val canvas = pc.canvas

                    val headPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.parseColor("#8E1D2A")
                        textSize = 15f
                        typeface = boldTf
                    }
                    val headTitle = "$title — ${sheet.name} (Page $pageNum)"
                    canvas.drawText(headTitle, margin, margin + 12f, headPaint)
                    pc.searchableLines.add(SearchableLineEntry(headTitle, margin, margin, 14f))

                    var y = margin + 28f
                    while (rowIndex < sheet.rows.size) {
                        val row = sheet.rows[rowIndex]
                        val cellLayouts = (0 until colCount).map { c ->
                            val txt = row.getOrNull(c) ?: ""
                            val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = if (rowIndex == 0) Color.WHITE else Color.parseColor("#1E293B")
                                textSize = 10.5f
                                typeface = if (rowIndex == 0) boldTf else regularTf
                            }
                            StaticLayout.Builder
                                .obtain(txt, 0, txt.length, tp, (colW - 10f).toInt().coerceAtLeast(20))
                                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                                .build()
                        }
                        val rowH = (cellLayouts.maxOfOrNull { it.height } ?: 16) + 12f
                        if (y + rowH > pageH - margin && y > margin + 40f) {
                            break
                        }

                        val bgPaint = Paint().apply {
                            color = when {
                                rowIndex == 0 -> Color.parseColor("#1F3A60")
                                rowIndex % 2 == 1 -> Color.parseColor("#F9F6F0")
                                else -> Color.WHITE
                            }
                        }
                        canvas.drawRect(margin, y, margin + tableW, y + rowH, bgPaint)

                        val gridPaint = Paint().apply {
                            color = Color.parseColor("#CBD5E1")
                            style = Paint.Style.STROKE
                            strokeWidth = 0.8f
                        }

                        for (c in 0 until colCount) {
                            val cellX = margin + c * colW
                            canvas.drawRect(cellX, y, cellX + colW, y + rowH, gridPaint)
                            canvas.save()
                            canvas.translate(cellX + 5f, y + 6f)
                            cellLayouts[c].draw(canvas)
                            canvas.restore()
                        }
                        pc.searchableLines.add(SearchableLineEntry(row.joinToString(" | "), margin + 5f, y + 6f, 10.5f))
                        y += rowH
                        rowIndex++
                    }
                    appendCanvasPageToPdDocument(context, pdDoc, pc, unicodeFont)
                    if (sheet.rows.isEmpty()) break
                }
            }
            pdDoc.save(outFile)
        }

        return ConversionOutput(outFile, title, pageNum.coerceAtLeast(1), "EXCEL")
    }

    private fun renderSlidesToPdf(
        context: Context,
        title: String,
        slides: List<ParsedSlide>
    ): ConversionOutput {
        PdfEditAndSecurityEngine.ensureInitialized(context)
        val outFile = File(workspaceDir(context), "${title}_ppt_${System.currentTimeMillis()}.pdf")
        val regularTf = loadBengaliCapableTypeface(context, false)
        val boldTf = loadBengaliCapableTypeface(context, true)

        PDDocument().use { pdDoc ->
            val unicodeFont = loadPdUnicodeFont(context, pdDoc)
            slides.forEach { slide ->
                val pc = HighResPageCanvas(SLIDE_WIDTH, SLIDE_HEIGHT)
                val canvas = pc.canvas
                canvas.drawColor(Color.parseColor("#FFFDF9"))

                val accentBar = Paint().apply { color = Color.parseColor("#8E1D2A") }
                canvas.drawRect(0f, 0f, SLIDE_WIDTH.toFloat(), 14f, accentBar)

                val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#E5DDD3")
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f
                }
                canvas.drawRoundRect(RectF(24f, 28f, SLIDE_WIDTH - 24f, SLIDE_HEIGHT - 24f), 14f, 14f, border)

                val headingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#1F3A60")
                    textSize = 24f
                    typeface = boldTf
                }
                val hasImg = slide.imageBytes != null
                val textColumnWidth = if (hasImg) (SLIDE_WIDTH * 0.54f).toInt() else SLIDE_WIDTH - 96

                val headLayout = StaticLayout.Builder
                    .obtain(slide.heading, 0, slide.heading.length, headingPaint, textColumnWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .build()
                canvas.save()
                canvas.translate(48f, 52f)
                headLayout.draw(canvas)
                canvas.restore()
                pc.searchableLines.add(SearchableLineEntry(slide.heading, 48f, 52f, 22f))

                var y = 52f + headLayout.height + 20f
                val bulletPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#262626")
                    textSize = 15f
                    typeface = regularTf
                }
                for (b in slide.bullets) {
                    if (y > SLIDE_HEIGHT - 54f) break
                    val line = "•  $b"
                    val bl = StaticLayout.Builder
                        .obtain(line, 0, line.length, bulletPaint, textColumnWidth)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setLineSpacing(3f, 1.15f)
                        .build()
                    canvas.save()
                    canvas.translate(52f, y)
                    bl.draw(canvas)
                    canvas.restore()
                    pc.searchableLines.add(SearchableLineEntry(b, 52f, y, 14f))
                    y += bl.height + 10f
                }

                slide.imageBytes?.let { bytes ->
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { bmp ->
                        val boxLeft = SLIDE_WIDTH * 0.60f
                        val boxTop = 64f
                        val maxW = SLIDE_WIDTH * 0.34f
                        val maxH = SLIDE_HEIGHT - 120f
                        val scale = minOf(maxW / bmp.width, maxH / bmp.height)
                        val dw = bmp.width * scale
                        val dh = bmp.height * scale
                        canvas.drawBitmap(
                            bmp,
                            null,
                            RectF(boxLeft, boxTop, boxLeft + dw, boxTop + dh),
                            Paint(Paint.FILTER_BITMAP_FLAG)
                        )
                        bmp.recycle()
                    }
                }

                val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#78716C")
                    textSize = 11f
                    typeface = regularTf
                }
                canvas.drawText("$title • Slide ${slide.slideNumber}/${slides.size}", 48f, SLIDE_HEIGHT - 36f, footerPaint)
                appendCanvasPageToPdDocument(context, pdDoc, pc, unicodeFont)
            }
            pdDoc.save(outFile)
        }

        return ConversionOutput(outFile, title, slides.size, "POWERPOINT")
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    private fun stripHtmlTags(html: String): String {
        return html
            .replace(Regex("(?is)<style.*?>.*?</style>"), "")
            .replace(Regex("(?is)<script.*?>.*?</script>"), "")
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>|</div>|</li>|</tr>|<h[1-6]>"), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    private fun stripRtfControlWords(rtf: String): String {
        return rtf
            .replace("\\par", "\n")
            .replace(Regex("\\\\[a-zA-Z]+-?\\d* ?"), "")
            .replace("{", "")
            .replace("}", "")
    }

    private fun extractReadableText(bytes: ByteArray): String {
        val utf8 = String(bytes, Charsets.UTF_8)
        val printableRatio = utf8.count { it == '\n' || it == '\r' || it == '\t' || it.code >= 32 } /
            utf8.length.coerceAtLeast(1).toFloat()
        return if (printableRatio > 0.82f) {
            utf8
        } else {
            val sb = StringBuilder()
            val run = StringBuilder()
            for (b in bytes) {
                val c = b.toInt() and 0xFF
                if (c in 32..126 || c == 10 || c == 13) {
                    run.append(c.toChar())
                } else {
                    if (run.length >= 4) {
                        sb.append(run).append('\n')
                    }
                    run.clear()
                }
            }
            if (run.length >= 4) sb.append(run)
            sb.toString().ifBlank { "কনভার্টকৃত বাইনারি ডকুমেন্ট।" }
        }
    }
}
