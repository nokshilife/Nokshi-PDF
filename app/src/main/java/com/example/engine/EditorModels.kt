package com.example.engine

import com.example.ui.i18n.AppLanguage
import java.util.UUID

enum class ShapeType(val labelEn: String, val labelBn: String) {
    LINE("Line", "লাইন"),
    ARROW("Arrow", "অ্যারো"),
    CIRCLE("Circle", "সার্কেল"),
    OVAL("Oval", "ওভাল"),
    RECTANGLE("Rectangle", "রেকট্যাঙ্গেল");

    fun label(lang: AppLanguage): String = lang.pick(labelEn, labelBn)
}

enum class EditorTool(val labelEn: String, val labelBn: String) {
    VIEW("View", "ভিউ"),
    SELECT("Move / Rotate", "সিলেক্ট ও ঘোরান"),
    FREEHAND("Freehand Pen", "হ্যান্ডস ফ্রি দাগ"),
    HIGHLIGHTER("Highlighter", "হাইলাইটার"),
    ADD_TEXT("Add Text", "টেক্সট যোগ"),
    EDIT_TEXT("Edit Text", "লেখা পরিবর্তন"),
    ADD_IMAGE("Add Image", "ইমেজ যোগ"),
    SIGNATURE("Signature", "সিগনেচার"),
    SQUARE_ERASE("Square Erase", "স্কয়ার ইরেজ"),
    SHAPE("Shapes", "শেপ ও সাইন");

    fun label(lang: AppLanguage): String = lang.pick(labelEn, labelBn)
}

data class NormPoint(
    val x: Float,
    val y: Float
)

sealed class PageEditItem {
    abstract val id: String
    abstract val pageIndex: Int

    data class TextAddition(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        val normX: Float,
        val normY: Float,
        val text: String,
        val fontSizePt: Float = 16f,
        val colorArgb: Int = 0xFF1A1A1A.toInt(),
        val isBold: Boolean = false,
        val rotationDeg: Float = 0f
    ) : PageEditItem()

    data class TextReplacement(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        val normLeft: Float,
        val normTop: Float,
        val normRight: Float,
        val normBottom: Float,
        val originalText: String,
        val newText: String,
        val colorArgb: Int = 0xFF1A1A1A.toInt(),
        val bgColorArgb: Int = 0xFFFFFFFF.toInt(),
        val fontSizePt: Float = 13f
    ) : PageEditItem()

    data class ImageAddition(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        val normX: Float,
        val normY: Float,
        val normWidth: Float = 0.36f,
        val normHeight: Float = 0.24f,
        val imagePath: String,
        val rotationDeg: Float = 0f
    ) : PageEditItem()

    data class SignatureAddition(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        val normX: Float,
        val normY: Float,
        val normWidth: Float = 0.34f,
        val normHeight: Float = 0.13f,
        val signaturePath: String,
        val rotationDeg: Float = 0f
    ) : PageEditItem()

    data class SquareErase(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        val normLeft: Float,
        val normTop: Float,
        val normRight: Float,
        val normBottom: Float,
        val fillColorArgb: Int = 0xFFFFFFFF.toInt(),
        val rotationDeg: Float = 0f
    ) : PageEditItem()

    data class FreehandStroke(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        val points: List<NormPoint>,
        val colorArgb: Int = 0xFF8E1D2A.toInt(),
        val strokeWidthNorm: Float = 0.006f
    ) : PageEditItem()

    data class HighlighterStroke(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        val points: List<NormPoint>,
        val colorArgb: Int = 0xFFFFEB3B.toInt(),
        val strokeWidthNorm: Float = 0.026f
    ) : PageEditItem()

    data class ShapeItem(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        val shapeType: ShapeType,
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val colorArgb: Int = 0xFF8E1D2A.toInt(),
        val strokeWidthNorm: Float = 0.005f,
        val isFilled: Boolean = false,
        val rotationDeg: Float = 0f
    ) : PageEditItem()
}

data class DetectedPdfTextLine(
    val id: String = UUID.randomUUID().toString(),
    val pageIndex: Int,
    val text: String,
    val normLeft: Float,
    val normTop: Float,
    val normRight: Float,
    val normBottom: Float,
    val fontSizePt: Float
)

data class ConvertedPageImage(
    val pageNumber: Int,
    val filePath: String,
    val width: Int,
    val height: Int,
    val formatLabel: String
)

data class PageOperationSpec(
    val id: String = UUID.randomUUID().toString(),
    val originalPageIndex: Int,
    val additionalRotationDeg: Int = 0
)

