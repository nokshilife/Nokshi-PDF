package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

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
    var showCreatePad by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "নিজস্ব সিগনেচার লাইব্রেরী",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = selectionModeLabel ?: "একাধিক স্বাক্ষর তৈরি করে সংরক্ষণ করুন এবং যেকোনো PDF-এ ব্যবহার করুন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { showCreatePad = true },
                modifier = Modifier.testTag("add_signature_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("নতুন স্বাক্ষর")
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
                        .padding(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Draw,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(46.dp)
                    )
                    Text(
                        text = "আপনার লাইব্রেরীতে এখনও কোনো সিগনেচার নেই",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "‘নতুন স্বাক্ষর’ বাটনে ট্যাপ করে আঙুল বা স্টাইলাস দিয়ে আপনার স্বাক্ষর এঁকে সংরক্ষণ করুন।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilledTonalButton(onClick = { showCreatePad = true }) {
                        Icon(Icons.Default.Draw, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("এখনই সিগনেচার আঁকুন")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f, fill = false)
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
                                    .height(92.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
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
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "সিগনেচার মুছুন",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            if (onSelectSignature != null) {
                                FilledTonalButton(
                                    onClick = { onSelectSignature(sig) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("PDF-এ বসান")
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
            }
        )
    }
}

@Composable
fun SignaturePadDialog(
    onDismiss: () -> Unit,
    onSaved: (label: String, pngFile: File, strokeColorArgb: Int) -> Unit
) {
    val context = LocalContext.current
    var label by remember { mutableStateOf("আমার স্বাক্ষর") }
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
        title = { Text("নতুন সিগনেচার আঁকুন", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("স্বাক্ষরের নাম") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signature_label_input")
                )

                // Ink Color Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("কালির রঙ:", style = MaterialTheme.typography.labelLarge)
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
                        Icon(Icons.Default.Palette, contentDescription = "কালার পিকার")
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("কলমের পুরুত্ব:", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(8.dp))
                    Slider(
                        value = strokeWidth,
                        onValueChange = { strokeWidth = it },
                        valueRange = 3f..16f,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Drawing Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(175.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
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
                            text = "এখানে আঙুল দিয়ে স্বাক্ষর আঁকুন",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Alignment.Center.let { Modifier.align(it) }
                        )
                    }
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Subtle baseline
                        drawLine(
                            color = Color(0xFFE2E8F0),
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
                    Text("মুছে আবার আঁকুন")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = completedStrokes.isNotEmpty(),
                onClick = {
                    val out = exportSignaturePng(context, completedStrokes, padSize)
                    onSaved(label.trim().ifBlank { "স্বাক্ষর" }, out, inkColorArgb)
                },
                modifier = Modifier.testTag("save_signature_button")
            ) {
                Text("লাইব্রেরীতে সেভ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )

    if (showColorPicker) {
        ColorPickerDialog(
            title = "স্বাক্ষরের কালির রঙ",
            initialColorArgb = inkColorArgb,
            onDismiss = { showColorPicker = false },
            onColorSelected = {
                inkColorArgb = it
                showColorPicker = false
            }
        )
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
