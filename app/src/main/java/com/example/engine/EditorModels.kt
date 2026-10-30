package com.example.engine

import java.util.UUID

enum class ShapeType(val labelBn: String) {
    LINE("লাইন"),
    ARROW("অ্যারো"),
    CIRCLE("সার্কেল"),
    OVAL("ওভাল"),
    RECTANGLE("রেকট্যাঙ্গেল")
}

enum class EditorTool(val labelBn: String) {
    SELECT("সিলেক্ট/মুভ"),
    EDIT_TEXT("লেখা পরিবর্তন"),
    ADD_TEXT("টেক্সট যোগ"),
    ADD_IMAGE("ইমেজ যোগ"),
    SQUARE_ERASE("স্কয়ার ইরেজ"),
    HIGHLIGHTER("হাইলাইটার"),
    SHAPE("শেপ ও সাইন"),
    SIGNATURE("সিগনেচার")
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
        val isBold: Boolean = false
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
        val normWidth: Float = 0.34f,
        val normHeight: Float = 0.22f,
        val imagePath: String
    ) : PageEditItem()

    data class SignatureAddition(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        val normX: Float,
        val normY: Float,
        val normWidth: Float = 0.32f,
        val normHeight: Float = 0.12f,
        val signaturePath: String
    ) : PageEditItem()

    data class SquareErase(
        override val id: String = UUID.randomUUID().toString(),
        override val pageIndex: Int,
        val normLeft: Float,
        val normTop: Float,
        val normRight: Float,
        val normBottom: Float,
        val fillColorArgb: Int = 0xFFFFFFFF.toInt()
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
        val isFilled: Boolean = false
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

data class OcrTextSegment(
    val id: String = UUID.randomUUID().toString(),
    val pageNumber: Int,
    val text: String
)

data class OcrPageResult(
    val pageNumber: Int,
    val imagePath: String,
    val segments: List<OcrTextSegment>,
    val fullText: String
)
