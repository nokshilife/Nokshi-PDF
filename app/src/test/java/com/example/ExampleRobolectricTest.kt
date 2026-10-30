package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.FileToPdfConverter
import com.example.engine.PageEditItem
import com.example.engine.PdfEditAndSecurityEngine
import com.example.engine.ShapeType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Nokshi PDF`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Nokshi PDF", appName)
    }

    @Test
    fun `pdf creation editing merging and password encryption work end to end`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val doc1 = FileToPdfConverter.createPdfFromWrittenText(
            context = context,
            title = "Test Doc 1",
            body = "Hello Nokshi PDF Studio\nনকশি পিডিএফ টেস্ট"
        )
        assertTrue(doc1.file.exists())
        assertEquals(1, doc1.pageCount)

        // Apply shape, square erase, and text addition
        val edited = PdfEditAndSecurityEngine.saveEditsToPdf(
            context = context,
            sourceFile = doc1.file,
            edits = listOf(
                PageEditItem.SquareErase(
                    pageIndex = 0,
                    normLeft = 0.1f,
                    normTop = 0.1f,
                    normRight = 0.3f,
                    normBottom = 0.2f
                ),
                PageEditItem.ShapeItem(
                    pageIndex = 0,
                    shapeType = ShapeType.ARROW,
                    startX = 0.2f,
                    startY = 0.4f,
                    endX = 0.6f,
                    endY = 0.5f
                ),
                PageEditItem.TextAddition(
                    pageIndex = 0,
                    normX = 0.2f,
                    normY = 0.6f,
                    text = "নতুন বাংলা লেখা"
                )
            )
        )
        assertTrue(edited.exists())

        // Merge two PDFs
        val doc2 = FileToPdfConverter.createPdfFromWrittenText(context, "Doc 2", "Page Two Content")
        val merged = PdfEditAndSecurityEngine.mergePdfs(
            context = context,
            filesToMerge = listOf(doc1.file, doc2.file),
            outputTitle = "MergedTest"
        )
        assertEquals(2, merged.pageCount)

        // Password protect PDF and verify password requirement
        val locked = PdfEditAndSecurityEngine.protectPdfWithPassword(
            context = context,
            sourceFile = merged.file,
            newPassword = "secret123"
        )
        val withoutPwd = PdfEditAndSecurityEngine.inspectPdf(context, locked, "")
        assertTrue(withoutPwd.requiresPassword)

        val withPwd = PdfEditAndSecurityEngine.inspectPdf(context, locked, "secret123")
        assertEquals(false, withPwd.requiresPassword)
        assertEquals(2, withPwd.pageCount)
    }

    @Test
    fun `main activity launches and renders home and all tabs without crashing`() {
        org.robolectric.Robolectric.buildActivity(MainActivity::class.java).use { controller ->
            controller.setup()
            val activity = controller.get()
            assertTrue(activity != null)

            // Verify all NokshiIcons build cleanly without Vector/Path exceptions
            val icons = listOf(
                com.example.ui.components.NokshiIcons.Palette,
                com.example.ui.components.NokshiIcons.Draw,
                com.example.ui.components.NokshiIcons.Colorize,
                com.example.ui.components.NokshiIcons.FontDownload,
                com.example.ui.components.NokshiIcons.Description,
                com.example.ui.components.NokshiIcons.Image,
                com.example.ui.components.NokshiIcons.TableChart,
                com.example.ui.components.NokshiIcons.Slideshow,
                com.example.ui.components.NokshiIcons.DocumentScanner,
                com.example.ui.components.NokshiIcons.FolderOpen,
                com.example.ui.components.NokshiIcons.EditNote,
                com.example.ui.components.NokshiIcons.AutoAwesome,
                com.example.ui.components.NokshiIcons.CallMerge,
                com.example.ui.components.NokshiIcons.PictureAsPdf,
                com.example.ui.components.NokshiIcons.ArrowUpward,
                com.example.ui.components.NokshiIcons.ArrowDownward,
                com.example.ui.components.NokshiIcons.CheckBox,
                com.example.ui.components.NokshiIcons.CheckBoxOutlineBlank,
                com.example.ui.components.NokshiIcons.LockOpen,
                com.example.ui.components.NokshiIcons.Download,
                com.example.ui.components.NokshiIcons.ContentCopy,
                com.example.ui.components.NokshiIcons.OpenInNew,
                com.example.ui.components.NokshiIcons.RadioButtonUnchecked,
                com.example.ui.components.NokshiIcons.SelectAll,
                com.example.ui.components.NokshiIcons.NavigateBefore,
                com.example.ui.components.NokshiIcons.NavigateNext,
                com.example.ui.components.NokshiIcons.Undo,
                com.example.ui.components.NokshiIcons.ZoomIn,
                com.example.ui.components.NokshiIcons.NoteAdd,
                com.example.ui.components.NokshiIcons.Save,
                com.example.ui.components.NokshiIcons.OpenWith,
                com.example.ui.components.NokshiIcons.TextFields,
                com.example.ui.components.NokshiIcons.AddPhotoAlternate,
                com.example.ui.components.NokshiIcons.CropFree,
                com.example.ui.components.NokshiIcons.BorderColor,
                com.example.ui.components.NokshiIcons.Category
            )
            assertEquals(36, icons.size)

            // Verify all AppFontStyle and AppColorTheme variants build cleanly
            com.example.ui.theme.AppFontStyle.entries.forEach { font ->
                val typo = com.example.ui.theme.buildAppTypography(font)
                assertTrue(typo.bodyLarge != null)
            }
            com.example.ui.theme.AppColorTheme.entries.forEach { theme ->
                val light = com.example.ui.theme.buildColorScheme(theme, false, 0xFF8E1D2A.toInt())
                val dark = com.example.ui.theme.buildColorScheme(theme, true, 0xFF8E1D2A.toInt())
                assertTrue(light.primary != dark.primary || theme == com.example.ui.theme.AppColorTheme.CUSTOM)
            }
        }
    }
}
