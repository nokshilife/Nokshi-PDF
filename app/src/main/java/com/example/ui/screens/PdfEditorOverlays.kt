package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DetectedPdfTextLine
import com.example.engine.EditorTool
import com.example.engine.PageEditItem
import com.example.engine.ShapeType
import com.example.ui.components.NokshiIcons
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.tr
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Local 60fps mutable state for an actively dragged / resized / rotated item on a PDF page.
 * Avoids mutating ViewModel lists during finger movement so pointerInput never restarts mid-drag!
 */
@Stable
class ActiveDraftState(
    val sourceItem: PageEditItem,
    val isExistingEdit: Boolean
) {
    val id: String = sourceItem.id
    val pageIndex: Int = sourceItem.pageIndex

    var normX by mutableFloatStateOf(
        when (sourceItem) {
            is PageEditItem.TextAddition -> sourceItem.normX
            is PageEditItem.ImageAddition -> sourceItem.normX
            is PageEditItem.SignatureAddition -> sourceItem.normX
            is PageEditItem.SquareErase -> min(sourceItem.normLeft, sourceItem.normRight)
            is PageEditItem.ShapeItem -> min(sourceItem.startX, sourceItem.endX)
            else -> 0.25f
        }
    )

    var normY by mutableFloatStateOf(
        when (sourceItem) {
            is PageEditItem.TextAddition -> sourceItem.normY
            is PageEditItem.ImageAddition -> sourceItem.normY
            is PageEditItem.SignatureAddition -> sourceItem.normY
            is PageEditItem.SquareErase -> min(sourceItem.normTop, sourceItem.normBottom)
            is PageEditItem.ShapeItem -> min(sourceItem.startY, sourceItem.endY)
            else -> 0.30f
        }
    )

    var normWidth by mutableFloatStateOf(
        when (sourceItem) {
            is PageEditItem.TextAddition -> (sourceItem.text.length.coerceIn(4, 28) * sourceItem.fontSizePt * 0.58f / 595f).coerceIn(0.14f, 0.78f)
            is PageEditItem.ImageAddition -> sourceItem.normWidth
            is PageEditItem.SignatureAddition -> sourceItem.normWidth
            is PageEditItem.SquareErase -> abs(sourceItem.normRight - sourceItem.normLeft).coerceAtLeast(0.06f)
            is PageEditItem.ShapeItem -> abs(sourceItem.endX - sourceItem.startX).coerceAtLeast(0.06f)
            else -> 0.30f
        }
    )

    var normHeight by mutableFloatStateOf(
        when (sourceItem) {
            is PageEditItem.TextAddition -> ((sourceItem.fontSizePt * 1.65f) / 842f).coerceIn(0.04f, 0.30f)
            is PageEditItem.ImageAddition -> sourceItem.normHeight
            is PageEditItem.SignatureAddition -> sourceItem.normHeight
            is PageEditItem.SquareErase -> abs(sourceItem.normBottom - sourceItem.normTop).coerceAtLeast(0.04f)
            is PageEditItem.ShapeItem -> abs(sourceItem.endY - sourceItem.startY).coerceAtLeast(0.04f)
            else -> 0.14f
        }
    )

    var fontSizePt by mutableFloatStateOf(
        if (sourceItem is PageEditItem.TextAddition) sourceItem.fontSizePt else 18f
    )

    var rotationDeg by mutableFloatStateOf(
        when (sourceItem) {
            is PageEditItem.TextAddition -> sourceItem.rotationDeg
            is PageEditItem.ImageAddition -> sourceItem.rotationDeg
            is PageEditItem.SignatureAddition -> sourceItem.rotationDeg
            is PageEditItem.SquareErase -> sourceItem.rotationDeg
            is PageEditItem.ShapeItem -> sourceItem.rotationDeg
            else -> 0f
        }
    )

    // Preserve directional signs for LINE / ARROW shapes
    private val shapeInvertX: Boolean =
        sourceItem is PageEditItem.ShapeItem && sourceItem.endX < sourceItem.startX
    private val shapeInvertY: Boolean =
        sourceItem is PageEditItem.ShapeItem && sourceItem.endY < sourceItem.startY

    fun toPageEditItem(): PageEditItem {
        return when (val s = sourceItem) {
            is PageEditItem.TextAddition -> s.copy(
                normX = normX.coerceIn(0.01f, 0.94f),
                normY = normY.coerceIn(0.01f, 0.95f),
                fontSizePt = fontSizePt.coerceIn(8f, 64f),
                rotationDeg = normalizeAngle(rotationDeg)
            )
            is PageEditItem.ImageAddition -> s.copy(
                normX = normX.coerceIn(-0.15f, 0.95f),
                normY = normY.coerceIn(-0.15f, 0.95f),
                normWidth = normWidth.coerceIn(0.06f, 0.96f),
                normHeight = normHeight.coerceIn(0.04f, 0.96f),
                rotationDeg = normalizeAngle(rotationDeg)
            )
            is PageEditItem.SignatureAddition -> s.copy(
                normX = normX.coerceIn(-0.15f, 0.95f),
                normY = normY.coerceIn(-0.15f, 0.95f),
                normWidth = normWidth.coerceIn(0.08f, 0.90f),
                normHeight = normHeight.coerceIn(0.03f, 0.65f),
                rotationDeg = normalizeAngle(rotationDeg)
            )
            is PageEditItem.SquareErase -> s.copy(
                normLeft = normX.coerceIn(0f, 0.98f),
                normTop = normY.coerceIn(0f, 0.98f),
                normRight = (normX + normWidth).coerceIn(0.02f, 1f),
                normBottom = (normY + normHeight).coerceIn(0.02f, 1f),
                rotationDeg = normalizeAngle(rotationDeg)
            )
            is PageEditItem.ShapeItem -> {
                val x1 = if (shapeInvertX) normX + normWidth else normX
                val x2 = if (shapeInvertX) normX else normX + normWidth
                val y1 = if (shapeInvertY) normY + normHeight else normY
                val y2 = if (shapeInvertY) normY else normY + normHeight
                s.copy(
                    startX = x1.coerceIn(0f, 1f),
                    startY = y1.coerceIn(0f, 1f),
                    endX = x2.coerceIn(0f, 1f),
                    endY = y2.coerceIn(0f, 1f),
                    rotationDeg = normalizeAngle(rotationDeg)
                )
            }
            else -> s
        }
    }

    private fun normalizeAngle(deg: Float): Float {
        var a = deg % 360f
        if (a < 0f) a += 360f
        return a
    }
}

@Composable
fun ActiveTransformOverlay(
    draft: ActiveDraftState,
    pagePixelSize: IntSize,
    stampBitmaps: Map<String, ImageBitmap>,
    onApply: () -> Unit,
    onDelete: () -> Unit
) {
    if (pagePixelSize.width <= 1 || pagePixelSize.height <= 1) return

    val density = LocalDensity.current
    val pw = pagePixelSize.width.toFloat()
    val ph = pagePixelSize.height.toFloat()
    val currentPw by rememberUpdatedState(pw)
    val currentPh by rememberUpdatedState(ph)

    val boxLeftPx = draft.normX * pw
    val boxTopPx = draft.normY * ph
    val boxWidthPx = (draft.normWidth * pw).coerceAtLeast(with(density) { 48.dp.toPx() })
    val boxHeightPx = (draft.normHeight * ph).coerceAtLeast(with(density) { 36.dp.toPx() })

    val boxWidthDp = with(density) { boxWidthPx.toDp() }
    val boxHeightDp = with(density) { boxHeightPx.toDp() }
    val handleSize = 32.dp
    val handleHalfPx = with(density) { 16.dp.toPx() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Floating status pill above or below item showing rotation & quick Apply
        val pillTopPx = (boxTopPx - with(density) { 46.dp.toPx() }).coerceAtLeast(8f)
        Surface(
            color = Color(0xDD1A1C1E),
            contentColor = Color.White,
            shape = RoundedCornerShape(999.dp),
            shadowElevation = 4.dp,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = boxLeftPx.roundToInt().coerceIn(8, (pw - 200f).roundToInt().coerceAtLeast(8)),
                        y = pillTopPx.roundToInt()
                    )
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${draft.rotationDeg.roundToInt()}° • " + tr("Double-tap outside to apply", "বাইরে ডাবল ট্যাপে অ্যাপ্লাই"),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
                Surface(
                    color = Color(0xFFFFCA28),
                    contentColor = Color(0xFF1A1100),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .clickable(onClick = onApply)
                        .testTag("apply_active_draft_chip")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(tr("Apply", "অ্যাপ্লাই"), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Main Interactive Draggable + Rotatable Item Box
        Box(
            modifier = Modifier
                .offset { IntOffset(boxLeftPx.roundToInt(), boxTopPx.roundToInt()) }
                .size(boxWidthDp, boxHeightDp)
                .graphicsLayer {
                    rotationZ = draft.rotationDeg
                }
                .border(2.dp, Color(0xFF1E88E5), RoundedCornerShape(4.dp))
                .pointerInput(draft.id) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // DragAmount in rotated local coordinates -> convert to parent page coordinates!
                        val rad = Math.toRadians(draft.rotationDeg.toDouble())
                        val cosA = cos(rad).toFloat()
                        val sinA = sin(rad).toFloat()
                        val worldDx = dragAmount.x * cosA - dragAmount.y * sinA
                        val worldDy = dragAmount.x * sinA + dragAmount.y * cosA

                        draft.normX = (draft.normX + worldDx / currentPw).coerceIn(-0.20f, 0.96f)
                        draft.normY = (draft.normY + worldDy / currentPh).coerceIn(-0.20f, 0.96f)
                    }
                }
                .testTag("active_transform_box")
        ) {
            when (val item = draft.sourceItem) {
                is PageEditItem.TextAddition -> {
                    val scaledSp = ((draft.fontSizePt / 595f) * pw / density.density).coerceIn(10f, 56f).sp
                    Text(
                        text = item.text,
                        color = Color(item.colorArgb),
                        fontSize = scaledSp,
                        fontWeight = if (item.isBold) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(2.dp)
                    )
                }
                is PageEditItem.ImageAddition -> {
                    val img = stampBitmaps[item.imagePath]
                    if (img != null) {
                        Image(
                            bitmap = img,
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                is PageEditItem.SignatureAddition -> {
                    val sig = stampBitmaps[item.signaturePath]
                    if (sig != null) {
                        Image(
                            bitmap = sig,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                is PageEditItem.SquareErase -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(item.fillColorArgb))
                    )
                }
                is PageEditItem.ShapeItem -> {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val x1 = if (item.endX < item.startX) size.width else 0f
                        val x2 = if (item.endX < item.startX) 0f else size.width
                        val y1 = if (item.endY < item.startY) size.height else 0f
                        val y2 = if (item.endY < item.startY) 0f else size.height
                        drawShapePreview(
                            shapeType = item.shapeType,
                            x1 = x1,
                            y1 = y1,
                            x2 = x2,
                            y2 = y2,
                            color = Color(item.colorArgb),
                            strokePx = (item.strokeWidthNorm * pw).coerceAtLeast(2.5f),
                            filled = item.isFilled
                        )
                    }
                }
                else -> Unit
            }
        }

        // Compute rotated corner positions so handles track rotation smoothly!
        val cx = boxLeftPx + boxWidthPx / 2f
        val cy = boxTopPx + boxHeightPx / 2f
        val rad = Math.toRadians(draft.rotationDeg.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()

        fun rotatedOffset(localDx: Float, localDy: Float): IntOffset {
            val rx = cx + localDx * cosA - localDy * sinA - handleHalfPx
            val ry = cy + localDx * sinA + localDy * cosA - handleHalfPx
            return IntOffset(rx.roundToInt(), ry.roundToInt())
        }

        // 1. Top-Center ROTATE Handle
        val rotateOffset = rotatedOffset(0f, -boxHeightPx / 2f - with(density) { 22.dp.toPx() })
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset { rotateOffset }
                .size(handleSize)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF1F3A60))
                .border(1.5.dp, Color.White, CircleShape)
                .pointerInput(draft.id) {
                    var pointerPageX = 0f
                    var pointerPageY = 0f
                    detectDragGestures(
                        onDragStart = { startLocal ->
                            val curCx = draft.normX * currentPw + (draft.normWidth * currentPw) / 2f
                            val curCy = draft.normY * currentPh + (draft.normHeight * currentPh) / 2f
                            val curRad = Math.toRadians(draft.rotationDeg.toDouble())
                            val hDist = -(draft.normHeight * currentPh).coerceAtLeast(48f) / 2f - 40f
                            pointerPageX = curCx - hDist * sin(curRad).toFloat() + (startLocal.x - handleHalfPx)
                            pointerPageY = curCy + hDist * cos(curRad).toFloat() + (startLocal.y - handleHalfPx)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            pointerPageX += dragAmount.x
                            pointerPageY += dragAmount.y
                            val curCx = draft.normX * currentPw + (draft.normWidth * currentPw) / 2f
                            val curCy = draft.normY * currentPh + (draft.normHeight * currentPh) / 2f
                            val angle = Math.toDegrees(atan2((pointerPageY - curCy).toDouble(), (pointerPageX - curCx).toDouble())).toFloat() + 90f
                            draft.rotationDeg = ((angle % 360f) + 360f) % 360f
                        }
                    )
                }
                .testTag("handle_rotate_item")
        ) {
            Icon(
                imageVector = NokshiIcons.RotateRight,
                contentDescription = tr("Rotate", "ঘোরান"),
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        // 2. Bottom-Right RESIZE Handle
        val resizeOffset = rotatedOffset(boxWidthPx / 2f, boxHeightPx / 2f)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset { resizeOffset }
                .size(handleSize)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFF1E88E5))
                .border(1.5.dp, Color.White, CircleShape)
                .pointerInput(draft.id) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val curRad = Math.toRadians(draft.rotationDeg.toDouble())
                        val c = cos(curRad).toFloat()
                        val s = sin(curRad).toFloat()
                        // Project world drag delta onto item's local width/height axes
                        val localDx = dragAmount.x * c + dragAmount.y * s
                        val localDy = -dragAmount.x * s + dragAmount.y * c

                        if (draft.sourceItem is PageEditItem.TextAddition) {
                            val scaleFactor = (1f + localDx / (draft.normWidth * currentPw).coerceAtLeast(60f)).coerceIn(0.85f, 1.18f)
                            draft.fontSizePt = (draft.fontSizePt * scaleFactor).coerceIn(8f, 64f)
                            draft.normWidth = (draft.normWidth * scaleFactor).coerceIn(0.10f, 0.92f)
                            draft.normHeight = (draft.normHeight * scaleFactor).coerceIn(0.03f, 0.45f)
                        } else {
                            draft.normWidth = (draft.normWidth + localDx / currentPw).coerceIn(0.06f, 0.95f)
                            draft.normHeight = (draft.normHeight + localDy / currentPh).coerceIn(0.03f, 0.95f)
                        }
                    }
                }
                .testTag("handle_resize_item")
        ) {
            Icon(
                imageVector = NokshiIcons.OpenInFull,
                contentDescription = tr("Resize", "বড়-ছোট করুন"),
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        // 3. Top-Left DELETE Handle
        val deleteOffset = rotatedOffset(-boxWidthPx / 2f, -boxHeightPx / 2f)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset { deleteOffset }
                .size(handleSize)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(Color(0xFFD32F2F))
                .border(1.5.dp, Color.White, CircleShape)
                .clickable(onClick = onDelete)
                .testTag("handle_delete_item")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = tr("Remove", "মুছুন"),
                tint = Color.White,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

fun DrawScope.drawShapePreview(
    shapeType: ShapeType,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    color: Color,
    strokePx: Float,
    filled: Boolean
) {
    val left = min(x1, x2)
    val top = min(y1, y2)
    val w = abs(x2 - x1).coerceAtLeast(4f)
    val h = abs(y2 - y1).coerceAtLeast(4f)

    when (shapeType) {
        ShapeType.LINE -> {
            drawLine(
                color = color,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
        }
        ShapeType.ARROW -> {
            drawLine(
                color = color,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
            val angle = atan2(y2 - y1, x2 - x1)
            val headLen = (strokePx * 4.2f).coerceAtLeast(16f)
            val spread = Math.toRadians(28.0).toFloat()
            val ax1 = x2 - headLen * cos(angle - spread)
            val ay1 = y2 - headLen * sin(angle - spread)
            val ax2 = x2 - headLen * cos(angle + spread)
            val ay2 = y2 - headLen * sin(angle + spread)
            val arrowHead = Path().apply {
                moveTo(x2, y2)
                lineTo(ax1, ay1)
                lineTo(ax2, ay2)
                close()
            }
            drawPath(arrowHead, color = color)
        }
        ShapeType.RECTANGLE -> {
            if (filled) {
                drawRect(color = color.copy(alpha = 0.25f), topLeft = Offset(left, top), size = Size(w, h))
            }
            drawRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(w, h),
                style = Stroke(width = strokePx)
            )
        }
        ShapeType.OVAL -> {
            if (filled) {
                drawOval(color = color.copy(alpha = 0.25f), topLeft = Offset(left, top), size = Size(w, h))
            }
            drawOval(
                color = color,
                topLeft = Offset(left, top),
                size = Size(w, h),
                style = Stroke(width = strokePx)
            )
        }
        ShapeType.CIRCLE -> {
            val d = max(w, h)
            val cx = (x1 + x2) / 2f
            val cy = (y1 + y2) / 2f
            if (filled) {
                drawCircle(color = color.copy(alpha = 0.25f), radius = d / 2f, center = Offset(cx, cy))
            }
            drawCircle(
                color = color,
                radius = d / 2f,
                center = Offset(cx, cy),
                style = Stroke(width = strokePx)
            )
        }
    }
}

@Composable
fun EditorBottomToolDock(
    activeTool: EditorTool,
    activeShapeType: ShapeType,
    shapeFilled: Boolean,
    activeColorArgb: Int,
    highlighterColorArgb: Int,
    eraseColorArgb: Int,
    strokeThicknessNorm: Float,
    highlighterThicknessNorm: Float,
    activeDraft: ActiveDraftState?,
    onSelectTool: (EditorTool) -> Unit,
    onHideControlsForViewing: () -> Unit,
    onSelectShapeType: (ShapeType) -> Unit,
    onToggleShapeFilled: (Boolean) -> Unit,
    onStrokeThicknessChange: (Float) -> Unit,
    onHighlighterThicknessChange: (Float) -> Unit,
    onQuickFreehandColor: (Int) -> Unit,
    onQuickHighlightColor: (Int) -> Unit,
    onOpenColorPicker: (String) -> Unit,
    onApplyActiveDraft: () -> Unit,
    onDeleteActiveDraft: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val highlightPresets = listOf(
        0xFFFFEB3B.toInt(),
        0xFF69F0AE.toInt(),
        0xFF40C4FF.toInt(),
        0xFFFF80AB.toInt(),
        0xFFFFAB40.toInt()
    )
    val inkPresets = listOf(
        0xFF8E1D2A.toInt(),
        0xFF1A1A1A.toInt(),
        0xFF1E88E5.toInt(),
        0xFF0F6E56.toInt(),
        0xFFD32F2F.toInt()
    )

    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Active Draft Rotation Slider & Apply Bar
            if (activeDraft != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    Icon(NokshiIcons.RotateRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Text("${activeDraft.rotationDeg.roundToInt()}°", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = activeDraft.rotationDeg,
                        onValueChange = { activeDraft.rotationDeg = it },
                        valueRange = 0f..360f,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = onDeleteActiveDraft,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                    Button(
                        onClick = onApplyActiveDraft,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0F6E56),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(tr("Apply", "অ্যাপ্লাই"))
                    }
                }
            }

            // Secondary Options Row for Freehand / Highlighter / Shapes / Erase
            when (activeTool) {
                EditorTool.FREEHAND -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Text(tr("Pen:", "কলম:"), style = MaterialTheme.typography.labelMedium)
                        inkPresets.forEach { argb ->
                            val sel = argb == activeColorArgb
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(Color(argb))
                                    .border(
                                        width = if (sel) 2.5.dp else 1.dp,
                                        color = if (sel) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { onQuickFreehandColor(argb) }
                            )
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable { onOpenColorPicker("MAIN") }
                        ) {
                            Icon(NokshiIcons.Palette, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Slider(
                            value = strokeThicknessNorm,
                            onValueChange = onStrokeThicknessChange,
                            valueRange = 0.002f..0.018f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                EditorTool.SHAPE -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ShapeType.entries) { st ->
                                FilterChip(
                                    selected = activeShapeType == st,
                                    onClick = { onSelectShapeType(st) },
                                    label = { Text(st.label(lang)) }
                                )
                            }
                            if (activeShapeType != ShapeType.LINE && activeShapeType != ShapeType.ARROW) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 6.dp)
                                    ) {
                                        Text(tr("Fill", "ভরাট"), style = MaterialTheme.typography.labelMedium)
                                        Spacer(Modifier.width(4.dp))
                                        Switch(checked = shapeFilled, onCheckedChange = onToggleShapeFilled)
                                    }
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(activeColorArgb))
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .clickable { onOpenColorPicker("MAIN") }
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(tr("Stroke:", "দাগ:"), style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = strokeThicknessNorm,
                                onValueChange = onStrokeThicknessChange,
                                valueRange = 0.003f..0.018f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                EditorTool.HIGHLIGHTER -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        highlightPresets.forEach { cArgb ->
                            val sel = highlighterColorArgb == cArgb
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(cArgb))
                                    .border(
                                        width = if (sel) 2.5.dp else 1.dp,
                                        color = if (sel) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { onQuickHighlightColor(cArgb) }
                            )
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable { onOpenColorPicker("HIGHLIGHT") }
                        ) {
                            Icon(NokshiIcons.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Slider(
                            value = highlighterThicknessNorm,
                            onValueChange = onHighlighterThicknessChange,
                            valueRange = 0.015f..0.055f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                EditorTool.SQUARE_ERASE -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 14.dp)
                    ) {
                        Text(tr("Erase Cover Color:", "ইরেজার রঙ:"), style = MaterialTheme.typography.labelMedium)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(eraseColorArgb))
                                .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { onOpenColorPicker("ERASE") }
                        )
                        TextButton(onClick = { onOpenColorPicker("ERASE") }) {
                            Text(tr("Match Paper Color", "পেজের রঙ মেলান"))
                        }
                    }
                }

                else -> Unit
            }

            // Primary Horizontal Tool Strip (Starts with View Button!)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(EditorTool.entries) { tool ->
                    val selected = activeTool == tool
                    val icon = when (tool) {
                        EditorTool.VIEW -> NokshiIcons.Visibility
                        EditorTool.SELECT -> NokshiIcons.OpenWith
                        EditorTool.FREEHAND -> NokshiIcons.Brush
                        EditorTool.HIGHLIGHTER -> NokshiIcons.BorderColor
                        EditorTool.ADD_TEXT -> NokshiIcons.TextFields
                        EditorTool.EDIT_TEXT -> NokshiIcons.EditNote
                        EditorTool.ADD_IMAGE -> NokshiIcons.AddPhotoAlternate
                        EditorTool.SIGNATURE -> NokshiIcons.Draw
                        EditorTool.SQUARE_ERASE -> NokshiIcons.CropFree
                        EditorTool.SHAPE -> NokshiIcons.Category
                    }
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (tool == EditorTool.VIEW) {
                                onHideControlsForViewing()
                            } else {
                                onSelectTool(tool)
                            }
                        },
                        label = { Text(tool.label(lang)) },
                        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("editor_tool_${tool.name.lowercase()}")
                    )
                }
            }
        }
    }
}

@Composable
fun AddTextToPdfDialog(
    initialColorArgb: Int,
    onDismiss: () -> Unit,
    onPickColor: () -> Unit,
    onConfirm: (text: String, fontSizePt: Float, isBold: Boolean) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var fontSize by remember { mutableFloatStateOf(18f) }
    var isBold by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Add Text to Page", "পেজে নতুন টেক্সট যোগ করুন")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(tr("Type English or Bengali text", "বাংলা বা ইংরেজি টেক্সট লিখুন")) },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_pdf_text_input")
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Color:", "রঙ:"))
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(initialColorArgb))
                                .border(1.dp, Color.Gray, CircleShape)
                                .clickable { onPickColor() }
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tr("Bold", "বোল্ড"))
                        Spacer(Modifier.width(6.dp))
                        Switch(checked = isBold, onCheckedChange = { isBold = it })
                    }
                }
                Text(
                    tr("Font Size: ${fontSize.roundToInt()} pt", "ফন্ট সাইজ: ${fontSize.roundToInt()} pt"),
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 10f..44f
                )
            }
        },
        confirmButton = {
            Button(
                enabled = text.isNotBlank(),
                onClick = { onConfirm(text.trim(), fontSize, isBold) },
                modifier = Modifier.testTag("confirm_add_pdf_text_button")
            ) {
                Text(tr("Place & Position", "পেজে বসান"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Cancel", "বাতিল")) }
        }
    )
}

@Composable
fun EditDetectedTextDialog(
    line: DetectedPdfTextLine,
    colorArgb: Int,
    onDismiss: () -> Unit,
    onPickColor: () -> Unit,
    onConfirm: (newText: String) -> Unit
) {
    var updatedText by remember(line) { mutableStateOf(line.text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Edit PDF Text", "PDF-এর বিদ্যমান লেখা পরিবর্তন")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = tr("Original: ${line.text}", "মূল লেখা: ${line.text}"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = updatedText,
                    onValueChange = { updatedText = it },
                    label = { Text(tr("Updated Text (Leave blank to erase)", "পরিবর্তিত নতুন লেখা (মুছতে খালি রাখুন)")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_detected_pdf_text_input")
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Text Color:", "নতুন লেখার রঙ:"))
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(colorArgb))
                            .border(1.dp, Color.Gray, CircleShape)
                            .clickable { onPickColor() }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(updatedText.trim()) },
                modifier = Modifier.testTag("confirm_replace_pdf_text_button")
            ) {
                Text(tr("Update", "পরিবর্তন করুন"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Cancel", "বাতিল")) }
        }
    )
}
