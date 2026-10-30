package com.example.engine

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LensAndReadAloudHelper {

    const val READ_ALOUD_URL = "https://bcs24admin.org.bd/read.html"

    /**
     * Converts the target PDF page into a crisp PNG image in the background cache
     * ready to be passed directly to Google Lens.
     */
    suspend fun renderPdfPageToLensImage(
        context: Context,
        pdfFile: File,
        pageIndex: Int = 0,
        password: String = ""
    ): File = withContext(Dispatchers.IO) {
        val lensDir = File(context.cacheDir, "lens_bridge").apply { mkdirs() }
        // Clean old temporary Lens images older than 15 minutes
        val cutoff = System.currentTimeMillis() - 15 * 60 * 1000L
        lensDir.listFiles()?.forEach { if (it.lastModified() < cutoff) runCatching { it.delete() } }

        val bmp = PdfEditAndSecurityEngine.renderPageBitmap(
            context = context,
            file = pdfFile,
            pageIndex = pageIndex,
            password = password,
            scale = 2.4f
        )
        val outFile = File(lensDir, "nokshi_lens_p${pageIndex + 1}_${System.currentTimeMillis()}.png")
        FileOutputStream(outFile).use { fos ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
        }
        bmp.recycle()
        outFile
    }

    /**
     * Opens the converted PDF page image directly in the Google Lens app
     * (or Google Search's Lens share target, with standard chooser fallback).
     */
    fun launchGoogleLens(context: Context, imageFile: File): Boolean {
        if (!imageFile.exists()) return false
        return runCatching {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
            val pm = context.packageManager
            val clip = ClipData.newRawUri("Nokshi PDF Page", uri)

            // 1. Standalone Google Lens app (com.google.ar.lens)
            val lensIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = clip
                setPackage("com.google.ar.lens")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (lensIntent.resolveActivity(pm) != null) {
                context.startActivity(lensIntent)
                return true
            }

            // 2. Google App's built-in Google Lens receiver (preinstalled on almost all Android phones)
            val googleLensShare = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = clip
                setClassName(
                    "com.google.android.googlequicksearchbox",
                    "com.google.android.apps.search.lens.LensShareEntryPointActivity"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (googleLensShare.resolveActivity(pm) != null) {
                context.startActivity(googleLensShare)
                return true
            }

            val googlePkgIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = clip
                setPackage("com.google.android.googlequicksearchbox")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            if (googlePkgIntent.resolveActivity(pm) != null) {
                context.startActivity(googlePkgIntent)
                return true
            }

            // 3. Fallback share chooser
            val chooser = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = clip
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Open in Google Lens"
            )
            context.startActivity(chooser)
            true
        }.getOrDefault(false)
    }

    /**
     * Opens https://bcs24admin.org.bd/read.html in Microsoft Edge browser (`com.microsoft.emmx`),
     * with automatic fallback to the user's default browser if Microsoft Edge is not installed.
     */
    fun openReadAloudSiteInEdge(context: Context): Boolean {
        return runCatching {
            val targetUri = Uri.parse(READ_ALOUD_URL)
            val pm = context.packageManager
            val edgePackages = listOf(
                "com.microsoft.emmx",
                "com.microsoft.emmx.beta",
                "com.microsoft.emmx.dev",
                "com.microsoft.emmx.canary"
            )

            for (pkg in edgePackages) {
                val edgeIntent = Intent(Intent.ACTION_VIEW, targetUri).apply {
                    setPackage(pkg)
                    addCategory(Intent.CATEGORY_BROWSABLE)
                }
                if (edgeIntent.resolveActivity(pm) != null) {
                    context.startActivity(edgeIntent)
                    return true
                }
            }

            // Fallback to default browser if Edge is not installed on the device
            val fallbackBrowser = Intent(Intent.ACTION_VIEW, targetUri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            context.startActivity(fallbackBrowser)
            true
        }.getOrDefault(false)
    }
}
