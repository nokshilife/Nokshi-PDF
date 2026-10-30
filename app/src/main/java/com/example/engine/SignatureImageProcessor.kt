package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

object SignatureImageProcessor {

    /**
     * Decodes a user-selected image URI at a clean resolution suitable for signature processing
     * (max 1100px longest side) in ARGB_8888 format.
     */
    fun decodeSignatureBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val maxDim = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
            var sampleSize = 1
            while (maxDim / sampleSize > 1100) {
                sampleSize *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        }.getOrNull()
    }

    /**
     * Removes paper/white/light background from an external signature image, producing a crisp
     * transparent ARGB_8888 Bitmap with smooth anti-aliased ink edges and tight bounding-box crop.
     *
     * @param source Original decoded bitmap
     * @param removeBackground If false, preserves original background
     * @param tolerance Threshold (0.10f..0.70f) controlling how aggressively paper background is made transparent
     * @param inkOverrideArgb Optional ink color override (null = preserve original ink RGB)
     */
    fun processSignatureBitmap(
        source: Bitmap,
        removeBackground: Boolean = true,
        tolerance: Float = 0.32f,
        inkOverrideArgb: Int? = null
    ): Bitmap {
        val w = source.width.coerceAtLeast(1)
        val h = source.height.coerceAtLeast(1)
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)

        if (!removeBackground) {
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            out.setPixels(pixels, 0, w, 0, 0, w, h)
            return out
        }

        // Estimate background RGB by sampling border pixels around all 4 edges
        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var count = 0
        val stepX = (w / 24).coerceAtLeast(1)
        val stepY = (h / 24).coerceAtLeast(1)

        fun samplePixel(px: Int) {
            if (Color.alpha(px) > 180) {
                sumR += Color.red(px)
                sumG += Color.green(px)
                sumB += Color.blue(px)
                count++
            }
        }

        for (x in 0 until w step stepX) {
            samplePixel(pixels[x])
            samplePixel(pixels[(h - 1) * w + x])
        }
        for (y in 0 until h step stepY) {
            samplePixel(pixels[y * w])
            samplePixel(pixels[y * w + (w - 1)])
        }

        val bgR = if (count > 0) (sumR / count).toInt() else 248
        val bgG = if (count > 0) (sumG / count).toInt() else 248
        val bgB = if (count > 0) (sumB / count).toInt() else 248
        val bgLum = 0.299f * bgR + 0.587f * bgG + 0.114f * bgB

        // Max Euclidean RGB distance is ~441.67
        val hardCutDist = (tolerance.coerceIn(0.08f, 0.75f) * 310f)
        val softBand = 52f

        val outR = inkOverrideArgb?.let { Color.red(it) }
        val outG = inkOverrideArgb?.let { Color.green(it) }
        val outB = inkOverrideArgb?.let { Color.blue(it) }

        var minX = w
        var minY = h
        var maxX = -1
        var maxY = -1

        for (y in 0 until h) {
            val rowOffset = y * w
            for (x in 0 until w) {
                val idx = rowOffset + x
                val c = pixels[idx]
                val origA = Color.alpha(c)
                if (origA < 16) {
                    pixels[idx] = Color.TRANSPARENT
                    continue
                }

                val r = Color.red(c)
                val g = Color.green(c)
                val b = Color.blue(c)

                val dr = (r - bgR).toFloat()
                val dg = (g - bgG).toFloat()
                val db = (b - bgB).toFloat()
                val rgbDist = sqrt(dr * dr + dg * dg + db * db)

                val lum = 0.299f * r + 0.587f * g + 0.114f * b
                val lumDiff = abs(bgLum - lum)

                // Combine RGB distance + luminance contrast so shadowy white paper vanishes cleanly
                val effectiveDist = max(rgbDist, lumDiff * 1.35f)

                val newAlpha = when {
                    effectiveDist <= hardCutDist -> 0
                    effectiveDist >= hardCutDist + softBand -> origA
                    else -> {
                        val t = (effectiveDist - hardCutDist) / softBand
                        (t * origA).roundToInt().coerceIn(0, 255)
                    }
                }

                if (newAlpha <= 8) {
                    pixels[idx] = Color.TRANSPARENT
                } else {
                    val finalR = outR ?: r
                    val finalG = outG ?: g
                    val finalB = outB ?: b
                    pixels[idx] = Color.argb(newAlpha, finalR, finalG, finalB)
                    if (newAlpha > 32) {
                        if (x < minX) minX = x
                        if (y < minY) minY = y
                        if (x > maxX) maxX = x
                        if (y > maxY) maxY = y
                    }
                }
            }
        }

        val fullProcessed = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        fullProcessed.setPixels(pixels, 0, w, 0, 0, w, h)

        // Tightly crop transparent padding around ink strokes (with a small 12px margin)
        if (maxX >= minX && maxY >= minY) {
            val pad = 12
            val cx = (minX - pad).coerceAtLeast(0)
            val cy = (minY - pad).coerceAtLeast(0)
            val cw = (maxX - minX + 1 + pad * 2).coerceAtMost(w - cx).coerceAtLeast(16)
            val ch = (maxY - minY + 1 + pad * 2).coerceAtMost(h - cy).coerceAtLeast(16)
            if (cw < w || ch < h) {
                val cropped = Bitmap.createBitmap(fullProcessed, cx, cy, cw, ch)
                if (cropped !== fullProcessed) fullProcessed.recycle()
                return cropped
            }
        }
        return fullProcessed
    }

    fun saveProcessedSignatureToFile(context: Context, bitmap: Bitmap): File {
        val dir = File(context.filesDir, "signatures").apply { mkdirs() }
        val outFile = File(dir, "sig_${System.currentTimeMillis()}.png")
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        return outFile
    }
}
