package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.SignatureEntity
import com.example.engine.SignatureImageProcessor
import com.example.ui.i18n.tr
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

private data class DrawnStroke(
    val points: List<Offset>,
    val colorArgb: Int,
    val widthPx: Float
)

@Composable
fun SignatureLibraryContent(
    signatures: List<SignatureEntity>,
    selectionModeLabel: String? = null,
    onSelectSignature: ((SignatureEntity) -> Unit)? = null,
    onCreateSignature: (label: String, pngFile: File, colorArgb: Int) -> Unit,
    onDeleteSignature: (SignatureEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCreatePad by remember { mutableStateOf(false) }
    var importedRawBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val externalSigPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val decoded = SignatureImageProcessor.decodeSignatureBitmapFromUri(context, uri)
            if (decoded != null) {
                importedRawBitmap = decoded
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = tr("Signature Library", "নিজস্ব সিগনেচার লাইব্রেরী"),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = selectionModeLabel ?: tr(
                    "Draw on pad or import an external signature image with automatic transparent background.",
                    "প্যাডে স্বাক্ষর আঁকুন অথবা এক্সটার্নাল সিগনেচার ইমেজ (ট্রান্সপারেন্ট ব্যাকগ্রাউন্ডসহ) যোগ করুন।"
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Action Buttons Row: Draw on Pad + Import External Image
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { showCreatePad = true },
                modifier = Modifier
                    .weight(1f)
                    .testTag("add_signature_button")
            ) {
                Icon(NokshiIcons.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(tr("Draw Pad", "প্যাডে আঁকুন"), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            FilledTonalButton(
                onClick = {
                    externalSigPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("import_signature_image_button")
            ) {
                Icon(NokshiIcons.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(tr("Import Image", "ইমেজ যোগ করুন"), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        if (signatures.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Icon(
                        imageVector = NokshiIcons.Draw,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = tr("No signatures saved yet", "আপনার লাইব্রেরীতে এখনও কোনো সিগনেচার নেই"),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = tr(
                            "Draw your signature on the pad or import a photo of your signature — we automatically make its background transparent.",
                            "প্যাডে স্বাক্ষর আঁকুন অথবা কাগজের স্বাক্ষরের ছবি ইমপোর্ট করুন — ব্যাকগ্রাউন্ড স্বয়ংক্রিয়ভাবে ট্রান্সপারেন্ট হয়ে যাবে।"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 152.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 460.dp)
            ) {
                items(signatures, key = { it.id }) { sig ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = onSelectSignature != null) {
                                onSelectSignature?.invoke(sig)
                            }
                            .testTag("signature_card_${sig.id}")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(88.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .checkerboardBackground()
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                    .padding(8.dp)
                            ) {
                                AsyncImage(
                                    model = File(sig.imagePath),
                                    contentDescription = sig.label,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = sig.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onDeleteSignature(sig) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = tr("Delete signature", "সিগনেচার মুছুন"),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                            if (onSelectSignature != null) {
                                Button(
                                    onClick = { onSelectSignature(sig) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("place_signature_${sig.id}")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(tr("Place on Page", "পেজে বসান"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePad) {
        SignaturePadDialog(
            onDismiss = { showCreatePad = false },
            onSaved = { label, file, color ->
                onCreateSignature(label, file, color)
                showCreatePad = false
                // If opened from PDF Editor, immediately place newly drawn signature onto the page!
                onSelectSignature?.invoke(
                    SignatureEntity(
                        id = -1L,
                        label = label,
                        imagePath = file.absolutePath,
                        strokeColorArgb = color
                    )
                )
            }
        )
    }

    importedRawBitmap?.let { rawBmp ->
        ExternalSignatureImportDialog(
            sourceBitmap = rawBmp,
            onDismiss = {
                rawBmp.recycle()
                importedRawBitmap = null
            },
            onSaved = { label, file, color ->
                onCreateSignature(label, file, color)
                rawBmp.recycle()
                importedRawBitmap = null
                // If opened from PDF Editor, immediately place newly imported signature onto the page!
                onSelectSignature?.invoke(
                    SignatureEntity(
                        id = -1L,
                        label = label,
                        imagePath = file.absolutePath,
                        strokeColorArgb = color
                    )
                )
            }
        )
    }
}

@Composable
private fun ExternalSignatureImportDialog(
    sourceBitmap: Bitmap,
    onDismiss: () -> Unit,
    onSaved: (label: String, pngFile: File, inkColorArgb: Int) -> Unit
) {
    val context = LocalContext.current
    var label by remember { mutableStateOf("") }
    var defaultLabel = tr("Imported Signature", "ইমপোর্ট করা স্বাক্ষর")
    var removeBackground by remember { mutableStateOf(true) }
    var tolerance by remember { mutableFloatStateOf(0.32f) }
    var inkOverrideArgb by remember { mutableStateOf<Int?>(null) }

    val previewBitmap = remember(sourceBitmap, removeBackground, tolerance, inkOverrideArgb) {
        SignatureImageProcessor.processSignatureBitmap(
            source = sourceBitmap,
            removeBackground = removeBackground,
            tolerance = tolerance,
            inkOverrideArgb = inkOverrideArgb
        )
    }

    DisposableEffect(previewBitmap) {
        onDispose {
            // Keep previewBitmap alive while displayed; GC reclaims old previews safely
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = tr("Import Signature Image", "সিগনেচার ইমেজ যোগ করুন"),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    placeholder = { Text(defaultLabel) },
                    label = { Text(tr("Signature Name", "স্বাক্ষরের নাম")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Checkerboard live preview showing transparency
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .checkerboardBackground()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = tr("Transparent Signature Preview", "স্বাক্ষর প্রিভিউ"),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr("Transparent Background", "ব্যাকগ্রাউন্ড ট্রান্সপারেন্ট করুন"),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Switch(
                        checked = removeBackground,
                        onCheckedChange = { removeBackground = it },
                        modifier = Modifier.testTag("sig_transparent_bg_switch")
                    )
                }

                if (removeBackground) {
                    Column {
                        Text(
                            text = tr("Background Removal Strength", "ব্যাকগ্রাউন্ড পরিষ্কারের মাত্রা"),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(
                            value = tolerance,
                            onValueChange = { tolerance = it },
                            valueRange = 0.12f..0.65f
                        )
                    }
                }

                Text(
                    text = tr("Ink Color", "কালির রঙ"),
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = inkOverrideArgb == null,
                        onClick = { inkOverrideArgb = null },
                        label = { Text(tr("Original", "আসল")) }
                    )
                    val presetInks = listOf(
                        0xFF1A1A1A.toInt(),
                        0xFF102A56.toInt(),
                        0xFF8E1D2A.toInt()
                    )
                    presetInks.forEach { argb ->
                        val selected = inkOverrideArgb == argb
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { inkOverrideArgb = argb }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val outFile = SignatureImageProcessor.saveProcessedSignatureToFile(context, previewBitmap)
                    onSaved(
                        label.trim().ifBlank { defaultLabel },
                        outFile,
                        inkOverrideArgb ?: 0xFF102A56.toInt()
                    )
                },
                modifier = Modifier.testTag("confirm_import_signature_button")
            ) {
                Text(tr("Save Signature", "লাইব্রেরীতে সেভ করুন"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancel", "বাতিল"))
            }
        }
    )
}

@Composable
fun SignaturePadDialog(
    onDismiss: () -> Unit,
    onSaved: (label: String, pngFile: File, strokeColorArgb: Int) -> Unit
) {
    val context = LocalContext.current
    val defaultName = tr("My Signature", "আমার স্বাক্ষর")
    var label by remember { mutableStateOf("") }
    var inkColorArgb by remember { mutableIntStateOf(0xFF102A56.toInt()) }
    var strokeWidth by remember { mutableFloatStateOf(7f) }
    var showColorPicker by remember { mutableStateOf(false) }

    val completedStrokes = remember { mutableStateListOf<DrawnStroke>() }
    val activePoints = remember { mutableStateListOf<Offset>() }
    var padSize by remember { mutableStateOf(IntSize(600, 300)) }

    val quickInkColors = listOf(
        0xFF1A1A1A.toInt(),
        0xFF102A56.toInt(),
        0xFF8E1D2A.toInt(),
        0xFF0F6E56.toInt()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = tr("Draw Signature (Transparent PNG)", "নতুন সিগনেচার আঁকুন"),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    placeholder = { Text(defaultName) },
                    label = { Text(tr("Signature Name", "স্বাক্ষরের নাম")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signature_label_input")
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(tr("Ink:", "কালির রঙ:"), style = MaterialTheme.typography.labelLarge)
                    quickInkColors.forEach { argb ->
                        val selected = argb == inkColorArgb
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color.LightGray,
                                    shape = CircleShape
                                )
                                .clickable { inkColorArgb = argb }
                        )
                    }
                    IconButton(onClick = { showColorPicker = true }) {
                        Icon(NokshiIcons.Palette, contentDescription = tr("Color Picker", "কালার পিকার"))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(tr("Thickness:", "কলমের পুরুত্ব:"), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(8.dp))
                    Slider(
                        value = strokeWidth,
                        onValueChange = { strokeWidth = it },
                        valueRange = 3f..16f,
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .checkerboardBackground()
                        .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                        .onSizeChanged { padSize = it }
                        .pointerInput(inkColorArgb, strokeWidth) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    activePoints.clear()
                                    activePoints.add(offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    activePoints.add(change.position)
                                },
                                onDragEnd = {
                                    if (activePoints.size >= 2) {
                                        completedStrokes.add(
                                            DrawnStroke(
                                                points = activePoints.toList(),
                                                colorArgb = inkColorArgb,
                                                widthPx = strokeWidth
                                            )
                                        )
                                    }
                                    activePoints.clear()
                                }
                            )
                        }
                        .testTag("signature_canvas")
                ) {
                    if (completedStrokes.isEmpty() && activePoints.isEmpty()) {
                        Text(
                            text = tr("Sign here with finger or stylus", "এখানে আঙুল দিয়ে স্বাক্ষর আঁকুন"),
                            color = Color.DarkGray,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawLine(
                            color = Color(0xFFCBD5E1),
                            start = Offset(24f, size.height * 0.76f),
                            end = Offset(size.width - 24f, size.height * 0.76f),
                            strokeWidth = 2f
                        )
                        completedStrokes.forEach { stroke ->
                            val path = Path().apply {
                                moveTo(stroke.points.first().x, stroke.points.first().y)
                                stroke.points.drop(1).forEach { lineTo(it.x, it.y) }
                            }
                            drawPath(
                                path = path,
                                color = Color(stroke.colorArgb),
                                style = Stroke(
                                    width = stroke.widthPx,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                        if (activePoints.size >= 2) {
                            val livePath = Path().apply {
                                moveTo(activePoints.first().x, activePoints.first().y)
                                activePoints.drop(1).forEach { lineTo(it.x, it.y) }
                            }
                            drawPath(
                                path = livePath,
                                color = Color(inkColorArgb),
                                style = Stroke(
                                    width = strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        completedStrokes.clear()
                        activePoints.clear()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(tr("Clear Pad", "মুছে আবার আঁকুন"))
                }
            }
        },
        confirmButton = {
            Button(
                enabled = completedStrokes.isNotEmpty(),
                onClick = {
                    val out = exportSignaturePng(context, completedStrokes, padSize)
                    onSaved(label.trim().ifBlank { defaultName }, out, inkColorArgb)
                },
                modifier = Modifier.testTag("save_signature_button")
            ) {
                Text(tr("Save Signature", "লাইব্রেরীতে সেভ করুন"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancel", "বাতিল"))
            }
        }
    )

    if (showColorPicker) {
        ColorPickerDialog(
            title = tr("Signature Ink Color", "স্বাক্ষরের কালির রঙ"),
            initialColorArgb = inkColorArgb,
            onDismiss = { showColorPicker = false },
            onColorSelected = {
                inkColorArgb = it
                showColorPicker = false
            }
        )
    }
}

private fun Modifier.checkerboardBackground(): Modifier = this.drawBehind {
    val cell = 10.dp.toPx().coerceAtLeast(8f)
    val light = Color(0xFFFFFFFF)
    val gray = Color(0xFFF1F5F9)
    drawRect(light)
    val cols = (size.width / cell).toInt() + 1
    val rows = (size.height / cell).toInt() + 1
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if ((r + c) % 2 == 1) {
                drawRect(
                    color = gray,
                    topLeft = Offset(c * cell, r * cell),
                    size = Size(cell, cell)
                )
            }
        }
    }
}

private fun exportSignaturePng(
    context: Context,
    strokes: List<DrawnStroke>,
    padSize: IntSize
): File {
    val allPts = strokes.flatMap { it.points }
    val pad = 20f
    val minX = (allPts.minOfOrNull { it.x } ?: 0f) - pad
    val minY = (allPts.minOfOrNull { it.y } ?: 0f) - pad
    val maxX = (allPts.maxOfOrNull { it.x } ?: padSize.width.toFloat()) + pad
    val maxY = (allPts.maxOfOrNull { it.y } ?: padSize.height.toFloat()) + pad

    val cropW = max(120, (maxX - minX).toInt())
    val cropH = max(70, (maxY - minY).toInt())

    val bmp = Bitmap.createBitmap(cropW, cropH, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    canvas.drawColor(android.graphics.Color.TRANSPARENT)

    strokes.forEach { s ->
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = s.colorArgb
            style = Paint.Style.STROKE
            strokeWidth = s.widthPx
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = AndroidPath()
        val first = s.points.first()
        path.moveTo(first.x - minX, first.y - minY)
        s.points.drop(1).forEach { pt ->
            path.lineTo(pt.x - minX, pt.y - minY)
        }
        canvas.drawPath(path, p)
    }

    val dir = File(context.filesDir, "signatures").apply { mkdirs() }
    val outFile = File(dir, "sig_${System.currentTimeMillis()}.png")
    FileOutputStream(outFile).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    bmp.recycle()
    return outFile
}
