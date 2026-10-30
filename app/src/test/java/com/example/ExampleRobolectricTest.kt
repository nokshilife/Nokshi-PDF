package com.example

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.test.core.app.ApplicationProvider
import com.example.data.UserThemeSettings
import com.example.engine.FileToPdfConverter
import com.example.engine.LazyPdfSession
import com.example.engine.LensAndReadAloudHelper
import com.example.engine.NormPoint
import com.example.engine.PageEditItem
import com.example.engine.PageOperationSpec
import com.example.engine.PdfEditAndSecurityEngine
import com.example.engine.ShapeType
import com.example.engine.SignatureImageProcessor
import com.example.engine.StorageExportHelper
import com.example.ui.i18n.AppLanguage
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.NokshiPdfTheme
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `app name matches Nokshi PDF and default language is English`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Nokshi PDF", appName)
        assertEquals(AppLanguage.ENGLISH, UserThemeSettings().language)
        assertEquals("https://bcs24admin.org.bd/read.html", LensAndReadAloudHelper.READ_ALOUD_URL)
    }

    @Test
    fun `lazy pdf session rotated edits freehand strokes and storage export work end to end`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val doc1 = FileToPdfConverter.createPdfFromWrittenText(
            context = context,
            title = "Test Doc 1",
            body = "Hello Nokshi PDF Studio\nনকশি পিডিএফ টেস্ট"
        )
        assertTrue(doc1.file.exists())
        assertEquals(1, doc1.pageCount)

        // Verify LazyPdfSession renders pages cleanly
        LazyPdfSession.open(context, doc1.file).use { session ->
            assertEquals(1, session.pageCount)
            val bmp = session.renderPage(0, 1080)
            assertNotNull(bmp)
            assertTrue(bmp!!.width > 0 && bmp.height > 0)
        }

        // Verify rotated edits + freehand stroke save cleanly
        val edited = PdfEditAndSecurityEngine.saveEditsToPdf(
            context = context,
            sourceFile = doc1.file,
            edits = listOf(
                PageEditItem.SquareErase(
                    pageIndex = 0,
                    normLeft = 0.1f,
                    normTop = 0.1f,
                    normRight = 0.3f,
                    normBottom = 0.2f,
                    rotationDeg = 25f
                ),
                PageEditItem.FreehandStroke(
                    pageIndex = 0,
                    points = listOf(NormPoint(0.15f, 0.25f), NormPoint(0.45f, 0.28f), NormPoint(0.65f, 0.32f)),
                    colorArgb = 0xFF8E1D2A.toInt()
                ),
                PageEditItem.ShapeItem(
                    pageIndex = 0,
                    shapeType = ShapeType.ARROW,
                    startX = 0.2f,
                    startY = 0.4f,
                    endX = 0.6f,
                    endY = 0.5f,
                    rotationDeg = 45f
                ),
                PageEditItem.TextAddition(
                    pageIndex = 0,
                    normX = 0.2f,
                    normY = 0.6f,
                    text = "Rotated Signature Note • নকশি",
                    rotationDeg = 15f
                )
            )
        )
        assertTrue(edited.exists())

        // Verify StorageExportHelper saves new files to Download/ and overwrites existing Uri files in-place
        val newOutcome = StorageExportHelper.syncPdfToUserStorage(
            context = context,
            localPdfFile = edited,
            displayTitle = "Converted_Test",
            originalUriString = null,
            existingDownloadUriString = null
        )
        assertTrue(newOutcome.locationBadge.startsWith("Download/"))

        val originalTargetFile = File(context.cacheDir, "user_original_file.pdf")
        doc1.file.copyTo(originalTargetFile, overwrite = true)
        val inPlaceOutcome = StorageExportHelper.syncPdfToUserStorage(
            context = context,
            localPdfFile = edited,
            displayTitle = "user_original_file",
            originalUriString = Uri.fromFile(originalTargetFile).toString(),
            existingDownloadUriString = null
        )
        assertTrue(inPlaceOutcome.updatedOriginalInPlace)
        assertEquals(edited.length(), originalTargetFile.length())
    }

    @Test
    fun `pdf page operations delete reorder rotate and lens page scoping work accurately`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val p1 = FileToPdfConverter.createPdfFromWrittenText(context, "P1", "Page One Content")
        val p2 = FileToPdfConverter.createPdfFromWrittenText(context, "P2", "Page Two Content")
        val p3 = FileToPdfConverter.createPdfFromWrittenText(context, "P3", "Page Three Content")
        val merged = PdfEditAndSecurityEngine.mergePdfs(
            context = context,
            filesToMerge = listOf(p1.file, p2.file, p3.file),
            outputTitle = "ThreePageTest"
        )
        assertEquals(3, merged.pageCount)

        // Verify Lens renders the requested target page (e.g., pageIndex = 2 -> p3 in filename)
        val lensPage3 = LensAndReadAloudHelper.renderPdfPageToLensImage(
            context = context,
            pdfFile = merged.file,
            pageIndex = 2
        )
        assertTrue(lensPage3.exists())
        assertTrue(lensPage3.name.contains("nokshi_lens_p3_"))

        // Reorder [Page 3 rotated 90°, Page 1 rotated 180°], deleting Page 2!
        val newCount = PdfEditAndSecurityEngine.applyPageOperations(
            context = context,
            sourceFile = merged.file,
            pageSpecs = listOf(
                PageOperationSpec(originalPageIndex = 2, additionalRotationDeg = 90),
                PageOperationSpec(originalPageIndex = 0, additionalRotationDeg = 180)
            )
        )
        assertEquals(2, newCount)

        PDDocument.load(merged.file).use { doc ->
            assertEquals(2, doc.numberOfPages)
            assertEquals(90, doc.getPage(0).rotation)
            assertEquals(180, doc.getPage(1).rotation)
        }
    }

    @Test
    fun `home screen footer displays developer info copies mobile and opens url in browser`() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        composeTestRule.setContent {
            NokshiPdfTheme {
                HomeScreen(
                    workspacePdfs = emptyList(),
                    onOpenExistingPdfPicker = {},
                    onConvertDocumentPicker = {},
                    onConvertImagePicker = {},
                    onConvertExcelPicker = {},
                    onConvertPptPicker = {},
                    onOpenWriteDocDialog = {},
                    onCreateSamplePdf = {},
                    onNavigateToMergeTab = {},
                    onOpenPdfInEditor = {},
                    onOpenInLens = {},
                    onReadAloud = {},
                    onConvertPdfToImages = {},
                    onManagePassword = {},
                    onDeletePdf = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Scroll Home screen LazyColumn to the very bottom developer credit card
        composeTestRule.onNodeWithTag("home_convert_doc").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nokshi PDF").assertIsDisplayed()

        composeTestRule.onNodeWithTag("home_lazy_column")
            .performScrollToNode(hasTestTag("developer_credit_footer"))
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Developed by: Md A Jalil (15703)").assertIsDisplayed()
        composeTestRule.onNodeWithText("24th BCS Administration Cadre").assertIsDisplayed()
        composeTestRule.onNodeWithText("https://jalil.bcs24admin.org.bd/").assertIsDisplayed()
        composeTestRule.onNodeWithText("Mobile: +880 1717 501021").assertIsDisplayed()

        // Click Copy button -> Verify clipboard contains +880 1717 501021
        composeTestRule.onNodeWithTag("copy_developer_mobile_button").performClick()
        composeTestRule.waitForIdle()
        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals("+880 1717 501021", clipboard.primaryClip?.getItemAt(0)?.text?.toString())

        // Click URL -> Verify ACTION_VIEW intent launched with https://jalil.bcs24admin.org.bd/
        composeTestRule.onNodeWithTag("developer_website_link").performClick()
        composeTestRule.waitForIdle()
        val nextIntent = shadowOf(ApplicationProvider.getApplicationContext<android.app.Application>()).nextStartedActivity
        assertNotNull(nextIntent)
        assertEquals("https://jalil.bcs24admin.org.bd/", nextIntent.dataString)
    }

    @Test
    fun `signature image processor removes white paper background cleanly`() {
        val raw = Bitmap.createBitmap(200, 100, Bitmap.Config.ARGB_8888)
        raw.eraseColor(Color.WHITE)
        val inkColor = Color.argb(255, 16, 42, 86)
        for (y in 46..54) {
            for (x in 30..170) {
                raw.setPixel(x, y, inkColor)
            }
        }

        val processed = SignatureImageProcessor.processSignatureBitmap(
            source = raw,
            removeBackground = true,
            tolerance = 0.32f
        )
        // Cropped tighter than original 200x100
        assertTrue(processed.width < 200 && processed.height < 100)
        // Corner pixel of cropped transparent signature must be completely transparent
        assertEquals(0, Color.alpha(processed.getPixel(0, 0)))
        // Center pixel on the ink line must remain opaque
        val midAlpha = Color.alpha(processed.getPixel(processed.width / 2, processed.height / 2))
        assertTrue("Expected opaque signature ink in center, got alpha=$midAlpha", midAlpha > 180)
    }

    @Test
    fun `main activity launches and renders home and all icons without crashing`() {
        Robolectric.buildActivity(MainActivity::class.java).use { controller ->
            controller.setup()
            val activity = controller.get()
            assertNotNull(activity)

            val icons = listOf(
                com.example.ui.components.NokshiIcons.Palette,
                com.example.ui.components.NokshiIcons.Draw,
                com.example.ui.components.NokshiIcons.Visibility,
                com.example.ui.components.NokshiIcons.Brush,
                com.example.ui.components.NokshiIcons.RotateRight,
                com.example.ui.components.NokshiIcons.OpenInFull,
                com.example.ui.components.NokshiIcons.CenterFocusStrong,
                com.example.ui.components.NokshiIcons.VolumeUp,
                com.example.ui.components.NokshiIcons.Language,
                com.example.ui.components.NokshiIcons.Folder,
                com.example.ui.components.NokshiIcons.GridView
            )
            assertEquals(11, icons.size)
        }
    }
}
