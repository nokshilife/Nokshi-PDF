package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer as NativePdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.ImageType
import com.tom_roush.pdfbox.rendering.PDFRenderer as PdfBoxRenderer
import java.io.Closeable
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Lazy, memory-bounded multi-page PDF rendering session capable of smoothly scrolling
 * through 1,000+ page PDFs without freezing or exhausting RAM.
 *
 * Uses Android's hardware-backed C++ PdfRenderer for unencrypted PDFs and automatically
 * falls back to an open PDFBox PDDocument for password-protected PDFs or JVM tests.
 */
class LazyPdfSession private constructor(
    private val pfd: ParcelFileDescriptor?,
    private val nativeRenderer: NativePdfRenderer?,
    private val pdfBoxDoc: PDDocument?,
    private val pdfBoxRenderer: PdfBoxRenderer?,
    val pageCount: Int
) : Closeable {

    private val renderMutex = Mutex()
    private val aspectRatios = ConcurrentHashMap<Int, Float>()

    @Volatile
    private var closed = false

    // Allocate ~1/6th of max JVM heap (in KB) for active rendered pages
    private val maxCacheKb: Int = ((Runtime.getRuntime().maxMemory() / 1024L) / 6L)
        .toInt()
        .coerceAtLeast(16 * 1024)

    private val bitmapCache = object : LruCache<Int, Bitmap>(maxCacheKb) {
        override fun sizeOf(key: Int, value: Bitmap): Int {
            return (value.byteCount / 1024).coerceAtLeast(1)
        }
    }

    fun getCachedBitmap(pageIndex: Int): Bitmap? = bitmapCache.get(pageIndex)

    fun getCachedAspectRatio(pageIndex: Int): Float = aspectRatios[pageIndex] ?: 0.7071f

    suspend fun getOrLoadAspectRatio(pageIndex: Int): Float {
        aspectRatios[pageIndex]?.let { return it }
        if (closed || pageIndex !in 0 until pageCount) return 0.7071f
        return withContext(Dispatchers.IO) {
            renderMutex.withLock {
                if (closed) return@withLock 0.7071f
                aspectRatios[pageIndex]?.let { return@withLock it }
                val ratio = when {
                    nativeRenderer != null -> {
                        nativeRenderer.openPage(pageIndex).use { page ->
                            val w = page.width.coerceAtLeast(1).toFloat()
                            val h = page.height.coerceAtLeast(1).toFloat()
                            (w / h).coerceIn(0.25f, 3.5f)
                        }
                    }
                    pdfBoxDoc != null -> {
                        val page = pdfBoxDoc.getPage(pageIndex)
                        val box = page.cropBox ?: page.mediaBox
                        val rawW = (box?.width ?: 595f).coerceAtLeast(1f)
                        val rawH = (box?.height ?: 842f).coerceAtLeast(1f)
                        val rot = ((page.rotation % 360) + 360) % 360
                        val swapped = rot == 90 || rot == 270
                        val w = if (swapped) rawH else rawW
                        val h = if (swapped) rawW else rawH
                        (w / h).coerceIn(0.25f, 3.5f)
                    }
                    else -> 0.7071f
                }
                aspectRatios[pageIndex] = ratio
                ratio
            }
        }
    }

    suspend fun renderPage(pageIndex: Int, targetWidthPx: Int = 1440): Bitmap? {
        if (closed || pageIndex !in 0 until pageCount) return null
        bitmapCache.get(pageIndex)?.let { return it }

        return withContext(Dispatchers.IO) {
            renderMutex.withLock {
                if (closed) return@withLock null
                bitmapCache.get(pageIndex)?.let { return@withLock it }

                val rendered: Bitmap? = runCatching {
                    when {
                        nativeRenderer != null -> {
                            nativeRenderer.openPage(pageIndex).use { page ->
                                val pw = page.width.coerceAtLeast(1)
                                val ph = page.height.coerceAtLeast(1)
                                val ratio = (pw.toFloat() / ph.toFloat()).coerceIn(0.25f, 3.5f)
                                aspectRatios[pageIndex] = ratio

                                val outW = targetWidthPx.coerceIn(720, 2048)
                                val outH = (outW / ratio).roundToInt().coerceIn(400, 3200)
                                val bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
                                bmp.eraseColor(Color.WHITE)
                                page.render(bmp, null, null, NativePdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bmp
                            }
                        }
                        pdfBoxDoc != null && pdfBoxRenderer != null -> {
                            val page = pdfBoxDoc.getPage(pageIndex)
                            val box = page.cropBox ?: page.mediaBox
                            val rawW = (box?.width ?: 595f).coerceAtLeast(1f)
                            val rawH = (box?.height ?: 842f).coerceAtLeast(1f)
                            val rot = ((page.rotation % 360) + 360) % 360
                            val swapped = rot == 90 || rot == 270
                            val pw = if (swapped) rawH else rawW
                            val ph = if (swapped) rawW else rawH
                            aspectRatios[pageIndex] = (pw / ph).coerceIn(0.25f, 3.5f)

                            val scale = (targetWidthPx.coerceIn(720, 1920) / pw).coerceIn(1.2f, 2.5f)
                            pdfBoxRenderer.renderImage(pageIndex, scale, ImageType.RGB)
                        }
                        else -> null
                    }
                }.getOrNull()

                if (rendered != null) {
                    bitmapCache.put(pageIndex, rendered)
                }
                rendered
            }
        }
    }

    override fun close() {
        closed = true
        bitmapCache.evictAll()
        runCatching { nativeRenderer?.close() }
        runCatching { pfd?.close() }
        runCatching { pdfBoxDoc?.close() }
    }

    companion object {
        suspend fun open(
            context: Context,
            file: File,
            password: String = ""
        ): LazyPdfSession = withContext(Dispatchers.IO) {
            PdfEditAndSecurityEngine.ensureInitialized(context)

            if (password.isEmpty()) {
                val nativeAttempt = runCatching {
                    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = NativePdfRenderer(descriptor)
                    val count = renderer.pageCount.coerceAtLeast(1)
                    // Probe page 0 dimensions to ensure native PdfRenderer is operational (not JVM stub)
                    if (renderer.pageCount > 0) {
                        renderer.openPage(0).use { p ->
                            check(p.width > 0 && p.height > 0)
                        }
                    }
                    LazyPdfSession(
                        pfd = descriptor,
                        nativeRenderer = renderer,
                        pdfBoxDoc = null,
                        pdfBoxRenderer = null,
                        pageCount = count
                    )
                }.getOrNull()

                if (nativeAttempt != null) {
                    return@withContext nativeAttempt
                }
            }

            // Password-protected PDF or Robolectric JVM fallback
            val doc = PDDocument.load(file, password)
            val renderer = PdfBoxRenderer(doc)
            LazyPdfSession(
                pfd = null,
                nativeRenderer = null,
                pdfBoxDoc = doc,
                pdfBoxRenderer = renderer,
                pageCount = doc.numberOfPages.coerceAtLeast(1)
            )
        }
    }
}
