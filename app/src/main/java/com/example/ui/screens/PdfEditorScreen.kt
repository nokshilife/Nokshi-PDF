package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SignatureEntity
import com.example.data.WorkspacePdfEntity
import com.example.engine.DetectedPdfTextLine
import com.example.engine.EditorTool
import com.example.engine.NormPoint
import com.example.engine.PageEditItem
import com.example.engine.ShapeType
import com.example.ui.components.ColorPickerDialog
import com.example.ui.components.SignatureLibraryContent
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfEditorScreen(
    pdfEntity: WorkspacePdfEntity,
    pageBitmap: Bitmap?,
    currentPageIndex: Int,
    totalPages: Int,
    detectedTextLines: List<DetectedPdfTextLine>,
    edits: List<PageEditItem>,
    signatures: List<SignatureEntity>,
    isBusy: Boolean,
    onBack: () -> Unit,
    onSelectPage: (Int) -> Unit,
    onAddBlankPage: () -> Unit,
    onAddEditItem: (PageEditItem) -> Unit,
    onUpdateEditItem: (PageEditItem) -> Unit,
    onDeleteEditItem: (String) -> Unit,
    onUndoLastEdit: () -> Unit,
    onSaveEdits: () -> Unit,
    onCreateSignature: (String, File, Int) -> Unit,
    onDeleteSignature: (SignatureEntity) -> Unit,
    onRequestPasswordDialog: () -> Unit,
    onRunOcrOnPdf: () -> Unit,
    onOpenPdfToImage: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val textMeasurer = rememberTextMeasurer()

    var activeTool by remember { mutableStateOf(EditorTool.SELECT) }
    var activeShapeType by remember { mutableStateOf(ShapeType.RECTANGLE) }
    var shapeFilled by remember { mutableStateOf(false) }

    var activeColorArgb by remember { mutableIntStateOf(0xFF8E1D2A.toInt()) }
    var highlighterColorArgb by remember { mutableIntStateOf(0xFFFFEB3B.toInt()) }
    var eraseColorArgb by remember { mutableIntStateOf(0xFFFFFFFF.toInt()) }

    var strokeThicknessNorm by remember { mutableFloatStateOf(0.006f) }
    var highlighterThicknessNorm by remember { mutableFloatStateOf(0.028f) }
    var zoomMultiplier by remember { mutableFloatStateOf(1f) }

    var selectedEditId by remember { mutableStateOf<String?>(null) }
    var showColorPickerFor by remember { mutableStateOf<String?>(null) } // "MAIN", "HIGHLIGHT", "ERASE"
    var showSignatureSheet by remember { mutableStateOf(false) }

    // Add / Edit Text Dialog State
    var pendingAddTextPos by remember { mutableStateOf<NormPoint?>(null) }
    var editingDetectedLine by remember { mutableStateOf<DetectedPdfTextLine?>(null) }

    // Live Drag State
    var dragStartNorm by remember { mutableStateOf<NormPoint?>(null) }
    var dragCurrentNorm by remember { mutableStateOf<NormPoint?>(null) }
    val liveHighlightPoints = remember { mutableStateListOf<NormPoint>() }
    var canvasPixelSize by remember { mutableStateOf(IntSize(1, 1)) }

    // Cache loaded stamp bitmaps for fast Canvas preview
    val currentPageEdits = remember(edits, currentPageIndex) {
        edits.filter { it.pageIndex == currentPageIndex }
    }
    val stampBitmaps = remember(currentPageEdits) {
        val map = mutableMapOf<String, Bitmap>()
        currentPageEdits.forEach { item ->
            val path = when (item) {
                is PageEditItem.ImageAddition -> item.imagePath
                is PageEditItem.SignatureAddition -> item.signaturePath
                else -> null
            }
            if (path != null && !map.containsKey(path)) {
                BitmapFactory.decodeFile(path)?.let { map[path] = it }
            }
        }
        map
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val cacheDir = File(context.cacheDir, "editor_stamps").apply { mkdirs() }
            val outFile = File(cacheDir, "img_${System.currentTimeMillis()}.png")
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bmp = BitmapFactory.decodeStream(input)
                    if (bmp != null) {
                        FileOutputStream(outFile).use { fos ->
                            bmp.compress(Bitmap.CompressFormat.PNG, 95, fos)
                        }
                        val ratio = (bmp.height.toFloat() / bmp.width.coerceAtLeast(1)).coerceIn(0.3f, 1.8f)
                        bmp.recycle()
                        val item = PageEditItem.ImageAddition(
                            pageIndex = currentPageIndex,
                            normX = 0.28f,
                            normY = 0.30f,
                            normWidth = 0.38f,
                            normHeight = (0.38f * ratio).coerceAtMost(0.55f),
                            imagePath = outFile.absolutePath
                        )
                        onAddEditItem(item)
                        selectedEditId = item.id
                        activeTool = EditorTool.SELECT
                    }
                }
            }
        }
    }

    LaunchedEffect(currentPageIndex) {
        selectedEditId = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = pdfEntity.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "পেজ ${currentPageIndex + 1}/$totalPages • পরিবর্তিত আইটেম: ${edits.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("editor_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onUndoLastEdit,
                        enabled = edits.isNotEmpty(),
                        modifier = Modifier.testTag("editor_undo_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "আনডু")
                    }
                    IconButton(
                        onClick = onRunOcrOnPdf,
                        modifier = Modifier.testTag("editor_ocr_button")
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = "OCR এক্সট্রাক্ট")
                    }
                    IconButton(
                        onClick = onOpenPdfToImage,
                        modifier = Modifier.testTag("editor_pdf_to_img_button")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = "PDF থেকে ইমেজ")
                    }
                    IconButton(
                        onClick = onRequestPasswordDialog,
                        modifier = Modifier.testTag("editor_lock_button")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "পাসওয়ার্ড সুরক্ষা")
                    }
                    Button(
                        onClick = onSaveEdits,
                        enabled = !isBusy,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("editor_save_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("সেভ")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            EditorBottomToolDock(
                activeTool = activeTool,
                activeShapeType = activeShapeType,
                shapeFilled = shapeFilled,
                activeColorArgb = activeColorArgb,
                highlighterColorArgb = highlighterColorArgb,
                eraseColorArgb = eraseColorArgb,
                strokeThicknessNorm = strokeThicknessNorm,
                highlighterThicknessNorm = highlighterThicknessNorm,
                selectedItem = currentPageEdits.find { it.id == selectedEditId },
                detectedCount = detectedTextLines.size,
                onSelectTool = { tool ->
                    activeTool = tool
                    when (tool) {
                        EditorTool.ADD_IMAGE -> {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        EditorTool.SIGNATURE -> {
                            showSignatureSheet = true
                        }
                        else -> Unit
                    }
                },
                onSelectShapeType = { activeShapeType = it },
                onToggleShapeFilled = { shapeFilled = it },
                onStrokeThicknessChange = { strokeThicknessNorm = it },
                onHighlighterThicknessChange = { highlighterThicknessNorm = it },
                onQuickHighlightColor = { highlighterColorArgb = it },
                onOpenColorPicker = { target -> showColorPickerFor = target },
                onResizeSelected = { scaleDelta ->
                    val sel = currentPageEdits.find { it.id == selectedEditId } ?: return@EditorBottomToolDock
                    when (sel) {
                        is PageEditItem.ImageAddition -> onUpdateEditItem(
                            sel.copy(
                                normWidth = (sel.normWidth + scaleDelta).coerceIn(0.10f, 0.90f),
                                normHeight = (sel.normHeight + scaleDelta * 0.75f).coerceIn(0.08f, 0.90f)
                            )
                        )
                        is PageEditItem.SignatureAddition -> onUpdateEditItem(
                            sel.copy(
                                normWidth = (sel.normWidth + scaleDelta).coerceIn(0.10f, 0.80f),
                                normHeight = (sel.normHeight + scaleDelta * 0.45f).coerceIn(0.05f, 0.50f)
                            )
                        )
                        is PageEditItem.TextAddition -> onUpdateEditItem(
                            sel.copy(fontSizePt = (sel.fontSizePt + scaleDelta * 40f).coerceIn(9f, 48f))
                        )
                        else -> Unit
                    }
                },
                onDeleteSelected = {
                    selectedEditId?.let {
                        onDeleteEditItem(it)
                        selectedEditId = null
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Page Navigation + Zoom Strip
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (currentPageIndex > 0) onSelectPage(currentPageIndex - 1) },
                            enabled = currentPageIndex > 0
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "আগের পেজ")
                        }
                        Text(
                            text = "পেজ ${currentPageIndex + 1} / $totalPages",
                            style = MaterialTheme.typography.labelLarge
                        )
                        IconButton(
                            onClick = { if (currentPageIndex < totalPages - 1) onSelectPage(currentPageIndex + 1) },
                            enabled = currentPageIndex < totalPages - 1
                        ) {
                            Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "পরের পেজ")
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                zoomMultiplier = when (zoomMultiplier) {
                                    1f -> 1.4f
                                    1.4f -> 1.8f
                                    else -> 1f
                                }
                            },
                            label = { Text("${(zoomMultiplier * 100).roundToInt()}%") },
                            leadingIcon = {
                                Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                        AssistChip(
                            onClick = onAddBlankPage,
                            label = { Text("+ নতুন পেজ") },
                            leadingIcon = {
                                Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }

            // Contextual Hint Banner
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                val hint = when (activeTool) {
                    EditorTool.SELECT -> "যেকোনো যোগ করা টেক্সট, ছবি, শেপ বা সিগনেচারে ট্যাপ করে ড্র্যাগ করে সরান।"
                    EditorTool.EDIT_TEXT -> if (detectedTextLines.isNotEmpty())
                        "নীল ড্যাশ বক্স যুক্ত যেকোনো লেখায় ট্যাপ করে লেখা পরিবর্তন করুন (${detectedTextLines.size}টি লাইন পাওয়া গেছে)।"
                    else
                        "এই পেজে আলাদা টেক্সট লেয়ার নেই—‘স্কয়ার ইরেজ’ দিয়ে মুছে ‘টেক্সট যোগ’ ব্যবহার করুন।"
                    EditorTool.ADD_TEXT -> "নতুন বাংলা বা ইংরেজি টেক্সট বসাতে পেজের যেকোনো জায়গায় ট্যাপ করুন।"
                    EditorTool.ADD_IMAGE -> "গ্যালারি থেকে ছবি বেছে নিয়ে পেজে বসান ও ড্র্যাগ করে সাজান।"
                    EditorTool.SQUARE_ERASE -> "যে অংশটুকু মুছে ফেলতে চান তার ওপর আঙুল টেনে চারকোণা বক্স আঁকুন।"
                    EditorTool.HIGHLIGHTER -> "গুরুত্বপূর্ণ লেখার ওপর আঙুল টেনে হাইলাইট করুন।"
                    EditorTool.SHAPE -> "পেজের ওপর আঙুল টেনে ${activeShapeType.labelBn} আঁকুন।"
                    EditorTool.SIGNATURE -> "নিজের সিগনেচার লাইব্রেরী থেকে স্বাক্ষর বেছে নিয়ে পেজে বসান।"
                }
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            // PDF Interactive Page Canvas Viewport
            BoxWithConstraints(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (pageBitmap == null || isBusy) {
                    CircularProgressIndicator()
                } else {
                    val aspect = pageBitmap.width.toFloat() / pageBitmap.height.coerceAtLeast(1).toFloat()
                    val baseWidth = maxWidth.coerceAtMost(560.dp) * zoomMultiplier

                    Box(
                        modifier = Modifier
                            .width(baseWidth)
                            .aspectRatio(aspect)
                            .shadow(6.dp, RoundedCornerShape(6.dp))
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White)
                            .onSizeChanged { canvasPixelSize = it }
                            .pointerInput(
                                activeTool,
                                activeShapeType,
                                shapeFilled,
                                activeColorArgb,
                                highlighterColorArgb,
                                eraseColorArgb,
                                strokeThicknessNorm,
                                highlighterThicknessNorm,
                                currentPageIndex,
                                currentPageEdits,
                                detectedTextLines
                            ) {
                                detectTapGestures { offset ->
                                    val w = size.width.coerceAtLeast(1).toFloat()
                                    val h = size.height.coerceAtLeast(1).toFloat()
                                    val nx = (offset.x / w).coerceIn(0f, 1f)
                                    val ny = (offset.y / h).coerceIn(0f, 1f)

                                    when (activeTool) {
                                        EditorTool.ADD_TEXT -> {
                                            pendingAddTextPos = NormPoint(nx, ny)
                                        }
                                        EditorTool.EDIT_TEXT -> {
                                            val hitLine = detectedTextLines.firstOrNull { line ->
                                                nx in (line.normLeft - 0.02f)..(line.normRight + 0.02f) &&
                                                    ny in (line.normTop - 0.015f)..(line.normBottom + 0.015f)
                                            }
                                            if (hitLine != null) {
                                                editingDetectedLine = hitLine
                                            }
                                        }
                                        EditorTool.SIGNATURE -> {
                                            showSignatureSheet = true
                                        }
                                        else -> {
                                            val hit = hitTestEditItem(currentPageEdits, nx, ny)
                                            selectedEditId = hit?.id
                                        }
                                    }
                                }
                            }
                            .pointerInput(
                                activeTool,
                                activeShapeType,
                                shapeFilled,
                                activeColorArgb,
                                highlighterColorArgb,
                                eraseColorArgb,
                                strokeThicknessNorm,
                                highlighterThicknessNorm,
                                currentPageIndex,
                                selectedEditId,
                                currentPageEdits
                            ) {
                                detectDragGestures(
                                    onDragStart = { startOffset ->
                                        val w = size.width.coerceAtLeast(1).toFloat()
                                        val h = size.height.coerceAtLeast(1).toFloat()
                                        val nx = (startOffset.x / w).coerceIn(0f, 1f)
                                        val ny = (startOffset.y / h).coerceIn(0f, 1f)

                                        when (activeTool) {
                                            EditorTool.SELECT -> {
                                                val hit = hitTestEditItem(currentPageEdits, nx, ny)
                                                selectedEditId = hit?.id
                                            }
                                            EditorTool.HIGHLIGHTER -> {
                                                liveHighlightPoints.clear()
                                                liveHighlightPoints.add(NormPoint(nx, ny))
                                            }
                                            EditorTool.SQUARE_ERASE, EditorTool.SHAPE -> {
                                                dragStartNorm = NormPoint(nx, ny)
                                                dragCurrentNorm = NormPoint(nx, ny)
                                            }
                                            else -> Unit
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        val w = size.width.coerceAtLeast(1).toFloat()
                                        val h = size.height.coerceAtLeast(1).toFloat()
                                        val nx = (change.position.x / w).coerceIn(0f, 1f)
                                        val ny = (change.position.y / h).coerceIn(0f, 1f)
                                        val dx = dragAmount.x / w
                                        val dy = dragAmount.y / h

                                        when (activeTool) {
                                            EditorTool.SELECT -> {
                                                val target = currentPageEdits.find { it.id == selectedEditId }
                                                if (target != null) {
                                                    change.consume()
                                                    onUpdateEditItem(moveEditItem(target, dx, dy))
                                                }
                                            }
                                            EditorTool.HIGHLIGHTER -> {
                                                change.consume()
                                                liveHighlightPoints.add(NormPoint(nx, ny))
                                            }
                                            EditorTool.SQUARE_ERASE, EditorTool.SHAPE -> {
                                                change.consume()
                                                dragCurrentNorm = NormPoint(nx, ny)
                                            }
                                            else -> Unit
                                        }
                                    },
                                    onDragEnd = {
                                        when (activeTool) {
                                            EditorTool.HIGHLIGHTER -> {
                                                if (liveHighlightPoints.size >= 2) {
                                                    onAddEditItem(
                                                        PageEditItem.HighlighterStroke(
                                                            pageIndex = currentPageIndex,
                                                            points = liveHighlightPoints.toList(),
                                                            colorArgb = highlighterColorArgb,
                                                            strokeWidthNorm = highlighterThicknessNorm
                                                        )
                                                    )
                                                }
                                                liveHighlightPoints.clear()
                                            }
                                            EditorTool.SQUARE_ERASE -> {
                                                val s = dragStartNorm
                                                val e = dragCurrentNorm
                                                if (s != null && e != null &&
                                                    abs(e.x - s.x) > 0.01f && abs(e.y - s.y) > 0.01f
                                                ) {
                                                    val erase = PageEditItem.SquareErase(
                                                        pageIndex = currentPageIndex,
                                                        normLeft = min(s.x, e.x),
                                                        normTop = min(s.y, e.y),
                                                        normRight = max(s.x, e.x),
                                                        normBottom = max(s.y, e.y),
                                                        fillColorArgb = eraseColorArgb
                                                    )
                                                    onAddEditItem(erase)
                                                    selectedEditId = erase.id
                                                }
                                                dragStartNorm = null
                                                dragCurrentNorm = null
                                            }
                                            EditorTool.SHAPE -> {
                                                val s = dragStartNorm
                                                val e = dragCurrentNorm
                                                if (s != null && e != null && hypot(e.x - s.x, e.y - s.y) > 0.015f) {
                                                    val shape = PageEditItem.ShapeItem(
                                                        pageIndex = currentPageIndex,
                                                        shapeType = activeShapeType,
                                                        startX = s.x,
                                                        startY = s.y,
                                                        endX = e.x,
                                                        endY = e.y,
                                                        colorArgb = activeColorArgb,
                                                        strokeWidthNorm = strokeThicknessNorm,
                                                        isFilled = shapeFilled
                                                    )
                                                    onAddEditItem(shape)
                                                    selectedEditId = shape.id
                                                }
                                                dragStartNorm = null
                                                dragCurrentNorm = null
                                            }
                                            else -> Unit
                                        }
                                    }
                                )
                            }
                            .testTag("pdf_editor_canvas")
                    ) {
                        Image(
                            bitmap = pageBitmap.asImageBitmap(),
                            contentDescription = "PDF Page ${currentPageIndex + 1}",
                            modifier = Modifier.fillMaxSize()
                        )

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cw = size.width
                            val ch = size.height

                            // Show existing text boxes when EDIT_TEXT tool is active
                            if (activeTool == EditorTool.EDIT_TEXT) {
                                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                                detectedTextLines.forEach { line ->
                                    val lx = line.normLeft * cw
                                    val ly = line.normTop * ch
                                    val lw = ((line.normRight - line.normLeft) * cw).coerceAtLeast(16f)
                                    val lh = ((line.normBottom - line.normTop) * ch).coerceAtLeast(14f)
                                    drawRect(
                                        color = Color(0x221F3A60),
                                        topLeft = Offset(lx, ly),
                                        size = Size(lw, lh)
                                    )
                                    drawRect(
                                        color = Color(0xFF1F3A60),
                                        topLeft = Offset(lx, ly),
                                        size = Size(lw, lh),
                                        style = Stroke(width = 1.6f, pathEffect = dash)
                                    )
                                }
                            }

                            // Render committed edits on current page
                            currentPageEdits.forEach { item ->
                                val isSelected = item.id == selectedEditId
                                when (item) {
                                    is PageEditItem.SquareErase -> {
                                        val left = min(item.normLeft, item.normRight) * cw
                                        val top = min(item.normTop, item.normBottom) * ch
                                        val rw = abs(item.normRight - item.normLeft) * cw
                                        val rh = abs(item.normBottom - item.normTop) * ch
                                        drawRect(
                                            color = Color(item.fillColorArgb),
                                            topLeft = Offset(left, top),
                                            size = Size(rw, rh)
                                        )
                                        if (isSelected) {
                                            drawSelectionBox(left, top, rw, rh)
                                        }
                                    }

                                    is PageEditItem.TextReplacement -> {
                                        val left = min(item.normLeft, item.normRight) * cw
                                        val top = min(item.normTop, item.normBottom) * ch
                                        val rw = (abs(item.normRight - item.normLeft) * cw).coerceAtLeast(40f)
                                        val rh = (abs(item.normBottom - item.normTop) * ch).coerceAtLeast(18f)
                                        drawRect(
                                            color = Color(item.bgColorArgb),
                                            topLeft = Offset(left - 2f, top - 2f),
                                            size = Size(rw + 4f, rh + 4f)
                                        )
                                        if (item.newText.isNotBlank()) {
                                            val scaledSp = ((item.fontSizePt / 595f) * cw / density).coerceIn(9f, 34f).sp
                                            val measured = textMeasurer.measure(
                                                text = item.newText,
                                                style = TextStyle(
                                                    color = Color(item.colorArgb),
                                                    fontSize = scaledSp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                            drawText(measured, topLeft = Offset(left, top))
                                        }
                                        if (isSelected) {
                                            drawSelectionBox(left, top, rw, rh)
                                        }
                                    }

                                    is PageEditItem.HighlighterStroke -> {
                                        if (item.points.size >= 2) {
                                            val p = Path().apply {
                                                moveTo(item.points.first().x * cw, item.points.first().y * ch)
                                                item.points.drop(1).forEach { lineTo(it.x * cw, it.y * ch) }
                                            }
                                            drawPath(
                                                path = p,
                                                color = Color(item.colorArgb).copy(alpha = 0.38f),
                                                style = Stroke(
                                                    width = (item.strokeWidthNorm * cw).coerceAtLeast(10f),
                                                    cap = StrokeCap.Round,
                                                    join = StrokeJoin.Round
                                                )
                                            )
                                        }
                                    }

                                    is PageEditItem.ShapeItem -> {
                                        drawShapePreview(
                                            shapeType = item.shapeType,
                                            x1 = item.startX * cw,
                                            y1 = item.startY * ch,
                                            x2 = item.endX * cw,
                                            y2 = item.endY * ch,
                                            color = Color(item.colorArgb),
                                            strokePx = (item.strokeWidthNorm * cw).coerceAtLeast(2.5f),
                                            filled = item.isFilled
                                        )
                                        if (isSelected) {
                                            val lx = min(item.startX, item.endX) * cw
                                            val ly = min(item.startY, item.endY) * ch
                                            drawSelectionBox(
                                                lx - 6f,
                                                ly - 6f,
                                                abs(item.endX - item.startX) * cw + 12f,
                                                abs(item.endY - item.startY) * ch + 12f
                                            )
                                        }
                                    }

                                    is PageEditItem.TextAddition -> {
                                        val scaledSp = ((item.fontSizePt / 595f) * cw / density).coerceIn(10f, 44f).sp
                                        val measured = textMeasurer.measure(
                                            text = item.text,
                                            style = TextStyle(
                                                color = Color(item.colorArgb),
                                                fontSize = scaledSp,
                                                fontWeight = if (item.isBold) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                        val tx = item.normX * cw
                                        val ty = item.normY * ch
                                        drawText(measured, topLeft = Offset(tx, ty))
                                        if (isSelected) {
                                            drawSelectionBox(
                                                tx - 4f,
                                                ty - 2f,
                                                measured.size.width + 8f,
                                                measured.size.height + 4f
                                            )
                                        }
                                    }

                                    is PageEditItem.ImageAddition -> {
                                        val bmp = stampBitmaps[item.imagePath]
                                        val ix = item.normX * cw
                                        val iy = item.normY * ch
                                        val iw = item.normWidth * cw
                                        val ih = item.normHeight * ch
                                        if (bmp != null) {
                                            drawImage(
                                                image = bmp.asImageBitmap(),
                                                dstOffset = IntOffset(ix.roundToInt(), iy.roundToInt()),
                                                dstSize = IntSize(iw.roundToInt().coerceAtLeast(10), ih.roundToInt().coerceAtLeast(10))
                                            )
                                        }
                                        if (isSelected) drawSelectionBox(ix, iy, iw, ih)
                                    }

                                    is PageEditItem.SignatureAddition -> {
                                        val bmp = stampBitmaps[item.signaturePath]
                                        val sx = item.normX * cw
                                        val sy = item.normY * ch
                                        val sw = item.normWidth * cw
                                        val sh = item.normHeight * ch
                                        if (bmp != null) {
                                            drawImage(
                                                image = bmp.asImageBitmap(),
                                                dstOffset = IntOffset(sx.roundToInt(), sy.roundToInt()),
                                                dstSize = IntSize(sw.roundToInt().coerceAtLeast(10), sh.roundToInt().coerceAtLeast(10))
                                            )
                                        }
                                        if (isSelected) drawSelectionBox(sx, sy, sw, sh)
                                    }
                                }
                            }

                            // Live gesture overlays
                            if (liveHighlightPoints.size >= 2) {
                                val lp = Path().apply {
                                    moveTo(liveHighlightPoints.first().x * cw, liveHighlightPoints.first().y * ch)
                                    liveHighlightPoints.drop(1).forEach { lineTo(it.x * cw, it.y * ch) }
                                }
                                drawPath(
                                    path = lp,
                                    color = Color(highlighterColorArgb).copy(alpha = 0.38f),
                                    style = Stroke(
                                        width = (highlighterThicknessNorm * cw).coerceAtLeast(10f),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }

                            val ds = dragStartNorm
                            val de = dragCurrentNorm
                            if (ds != null && de != null) {
                                if (activeTool == EditorTool.SQUARE_ERASE) {
                                    val lx = min(ds.x, de.x) * cw
                                    val ly = min(ds.y, de.y) * ch
                                    val lw = abs(de.x - ds.x) * cw
                                    val lh = abs(de.y - ds.y) * ch
                                    drawRect(
                                        color = Color(eraseColorArgb).copy(alpha = 0.85f),
                                        topLeft = Offset(lx, ly),
                                        size = Size(lw, lh)
                                    )
                                    drawRect(
                                        color = Color(0xFF8E1D2A),
                                        topLeft = Offset(lx, ly),
                                        size = Size(lw, lh),
                                        style = Stroke(
                                            width = 2.5f,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                                        )
                                    )
                                } else if (activeTool == EditorTool.SHAPE) {
                                    drawShapePreview(
                                        shapeType = activeShapeType,
                                        x1 = ds.x * cw,
                                        y1 = ds.y * ch,
                                        x2 = de.x * cw,
                                        y2 = de.y * ch,
                                        color = Color(activeColorArgb),
                                        strokePx = (strokeThicknessNorm * cw).coerceAtLeast(2.5f),
                                        filled = shapeFilled
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add New Text Dialog
    pendingAddTextPos?.let { pos ->
        AddOrEditTextDialog(
            title = "নতুন টেক্সট যোগ করুন",
            initialText = "",
            initialFontSize = 16f,
            initialColorArgb = activeColorArgb,
            showBoldToggle = true,
            onDismiss = { pendingAddTextPos = null },
            onConfirm = { newText, sizePt, chosenColor, bold ->
                activeColorArgb = chosenColor
                if (newText.isNotBlank()) {
                    val added = PageEditItem.TextAddition(
                        pageIndex = currentPageIndex,
                        normX = pos.x.coerceIn(0.03f, 0.85f),
                        normY = pos.y.coerceIn(0.03f, 0.92f),
                        text = newText,
                        fontSizePt = sizePt,
                        colorArgb = chosenColor,
                        isBold = bold
                    )
                    onAddEditItem(added)
                    selectedEditId = added.id
                    activeTool = EditorTool.SELECT
                }
                pendingAddTextPos = null
            }
        )
    }

    // Replace Existing PDF Text Line Dialog
    editingDetectedLine?.let { line ->
        AddOrEditTextDialog(
            title = "বিদ্যমান লেখা পরিবর্তন করুন",
            initialText = line.text,
            initialFontSize = line.fontSizePt,
            initialColorArgb = 0xFF1A1A1A.toInt(),
            showBoldToggle = false,
            subtitle = "মূল লেখা: \"${line.text}\" (ফাঁকা রাখলে মূল লেখাটি মুছে যাবে)",
            onDismiss = { editingDetectedLine = null },
            onConfirm = { updatedText, sizePt, chosenColor, _ ->
                val rep = PageEditItem.TextReplacement(
                    pageIndex = currentPageIndex,
                    normLeft = line.normLeft,
                    normTop = line.normTop,
                    normRight = line.normRight,
                    normBottom = line.normBottom,
                    originalText = line.text,
                    newText = updatedText,
                    colorArgb = chosenColor,
                    bgColorArgb = 0xFFFFFFFF.toInt(),
                    fontSizePt = sizePt
                )
                onAddEditItem(rep)
                selectedEditId = rep.id
                editingDetectedLine = null
            }
        )
    }

    // Color Picker Dialog
    showColorPickerFor?.let { target ->
        val startArgb = when (target) {
            "HIGHLIGHT" -> highlighterColorArgb
            "ERASE" -> eraseColorArgb
            else -> activeColorArgb
        }
        ColorPickerDialog(
            title = when (target) {
                "HIGHLIGHT" -> "হাইলাইটারের রঙ নির্বাচন"
                "ERASE" -> "ইরেজ বক্সের ব্যাকগ্রাউন্ড রঙ"
                else -> "টেক্সট ও শেপের রঙ নির্বাচন"
            },
            initialColorArgb = startArgb,
            onDismiss = { showColorPickerFor = null },
            onColorSelected = { chosen ->
                when (target) {
                    "HIGHLIGHT" -> highlighterColorArgb = chosen
                    "ERASE" -> eraseColorArgb = chosen
                    else -> {
                        activeColorArgb = chosen
                        val sel = currentPageEdits.find { it.id == selectedEditId }
                        if (sel is PageEditItem.ShapeItem) {
                            onUpdateEditItem(sel.copy(colorArgb = chosen))
                        } else if (sel is PageEditItem.TextAddition) {
                            onUpdateEditItem(sel.copy(colorArgb = chosen))
                        }
                    }
                }
                showColorPickerFor = null
            }
        )
    }

    // Signature Library Bottom Sheet
    if (showSignatureSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSignatureSheet = false }
        ) {
            SignatureLibraryContent(
                signatures = signatures,
                selectionModeLabel = "যেকোনো সিগনেচারে ট্যাপ করলেই বর্তমান PDF পেজে বসে যাবে:",
                onSelectSignature = { sig ->
                    val stamp = PageEditItem.SignatureAddition(
                        pageIndex = currentPageIndex,
                        normX = 0.52f,
                        normY = 0.74f,
                        normWidth = 0.32f,
                        normHeight = 0.11f,
                        signaturePath = sig.imagePath
                    )
                    onAddEditItem(stamp)
                    selectedEditId = stamp.id
                    activeTool = EditorTool.SELECT
                    showSignatureSheet = false
                },
                onCreateSignature = onCreateSignature,
                onDeleteSignature = onDeleteSignature,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }
}

@Composable
private fun EditorBottomToolDock(
    activeTool: EditorTool,
    activeShapeType: ShapeType,
    shapeFilled: Boolean,
    activeColorArgb: Int,
    highlighterColorArgb: Int,
    eraseColorArgb: Int,
    strokeThicknessNorm: Float,
    highlighterThicknessNorm: Float,
    selectedItem: PageEditItem?,
    detectedCount: Int,
    onSelectTool: (EditorTool) -> Unit,
    onSelectShapeType: (ShapeType) -> Unit,
    onToggleShapeFilled: (Boolean) -> Unit,
    onStrokeThicknessChange: (Float) -> Unit,
    onHighlighterThicknessChange: (Float) -> Unit,
    onQuickHighlightColor: (Int) -> Unit,
    onOpenColorPicker: (String) -> Unit,
    onResizeSelected: (Float) -> Unit,
    onDeleteSelected: () -> Unit
) {
    Surface(
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Secondary Contextual Controls
            when {
                activeTool == EditorTool.SELECT && selectedItem != null -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Text("নির্বাচিত আইটেম:", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(onClick = { onResizeSelected(-0.04f) }, label = { Text("ছোট -") })
                            AssistChip(onClick = { onResizeSelected(0.04f) }, label = { Text("বড় +") })
                            IconButton(onClick = { onOpenColorPicker("MAIN") }) {
                                Icon(Icons.Default.Palette, contentDescription = "রঙ পরিবর্তন")
                            }
                            IconButton(onClick = onDeleteSelected) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "মুছে ফেলুন",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                activeTool == EditorTool.SHAPE -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ShapeType.entries) { st ->
                                FilterChip(
                                    selected = activeShapeType == st,
                                    onClick = { onSelectShapeType(st) },
                                    label = { Text(st.labelBn) },
                                    modifier = Modifier.testTag("shape_chip_${st.name.lowercase()}")
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(activeColorArgb))
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    .clickable { onOpenColorPicker("MAIN") }
                            )
                            TextButton(onClick = { onOpenColorPicker("MAIN") }) {
                                Text("কালার পিকার")
                            }
                            Slider(
                                value = strokeThicknessNorm,
                                onValueChange = onStrokeThicknessChange,
                                valueRange = 0.002f..0.020f,
                                modifier = Modifier.weight(1f)
                            )
                            if (activeShapeType == ShapeType.CIRCLE ||
                                activeShapeType == ShapeType.OVAL ||
                                activeShapeType == ShapeType.RECTANGLE
                            ) {
                                Text("ভরাট", style = MaterialTheme.typography.labelSmall)
                                Switch(checked = shapeFilled, onCheckedChange = onToggleShapeFilled)
                            }
                        }
                    }
                }

                activeTool == EditorTool.HIGHLIGHTER -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        val quickSwatches = listOf(
                            0xFFFFEB3B.toInt(),
                            0xFF69F0AE.toInt(),
                            0xFF40C4FF.toInt(),
                            0xFFFF80AB.toInt(),
                            0xFFFFAB40.toInt()
                        )
                        quickSwatches.forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(c))
                                    .border(
                                        width = if (highlighterColorArgb == c) 2.5.dp else 1.dp,
                                        color = if (highlighterColorArgb == c) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = CircleShape
                                    )
                                    .clickable { onQuickHighlightColor(c) }
                            )
                        }
                        IconButton(onClick = { onOpenColorPicker("HIGHLIGHT") }) {
                            Icon(Icons.Default.Palette, contentDescription = "কালার পিকার")
                        }
                        Slider(
                            value = highlighterThicknessNorm,
                            onValueChange = onHighlighterThicknessChange,
                            valueRange = 0.015f..0.055f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                activeTool == EditorTool.SQUARE_ERASE -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = "স্কয়ার মার্কিং ইরেজার সক্রিয়",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(eraseColorArgb))
                                    .border(1.dp, Color.Gray, RoundedCornerShape(6.dp))
                                    .clickable { onOpenColorPicker("ERASE") }
                            )
                            TextButton(onClick = { onOpenColorPicker("ERASE") }) {
                                Text("ইরেজ রঙ পরিবর্তন")
                            }
                        }
                    }
                }
            }

            // Primary Scrollable Tool Strip
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(EditorTool.entries) { tool ->
                    val icon = when (tool) {
                        EditorTool.SELECT -> Icons.Default.OpenWith
                        EditorTool.EDIT_TEXT -> Icons.Default.EditNote
                        EditorTool.ADD_TEXT -> Icons.Default.TextFields
                        EditorTool.ADD_IMAGE -> Icons.Default.AddPhotoAlternate
                        EditorTool.SQUARE_ERASE -> Icons.Default.CropFree
                        EditorTool.HIGHLIGHTER -> Icons.Default.BorderColor
                        EditorTool.SHAPE -> Icons.Default.Category
                        EditorTool.SIGNATURE -> Icons.Default.Draw
                    }
                    val label = if (tool == EditorTool.EDIT_TEXT && detectedCount > 0) {
                        "${tool.labelBn} ($detectedCount)"
                    } else {
                        tool.labelBn
                    }
                    FilterChip(
                        selected = activeTool == tool,
                        onClick = { onSelectTool(tool) },
                        label = { Text(label) },
                        leadingIcon = {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("tool_chip_${tool.name.lowercase()}")
                    )
                }
            }
        }
    }
}

@Composable
private fun AddOrEditTextDialog(
    title: String,
    initialText: String,
    initialFontSize: Float,
    initialColorArgb: Int,
    showBoldToggle: Boolean,
    subtitle: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (text: String, fontSizePt: Float, colorArgb: Int, isBold: Boolean) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var fontSize by remember { mutableFloatStateOf(initialFontSize) }
    var colorArgb by remember { mutableIntStateOf(initialColorArgb) }
    var isBold by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("বাংলা বা ইংরেজি টেক্সট লিখুন") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_text_dialog_input")
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ফন্ট সাইজ: ${fontSize.roundToInt()} pt", style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(colorArgb))
                                .border(1.dp, Color.Gray, CircleShape)
                                .clickable { showPicker = true }
                        )
                        TextButton(onClick = { showPicker = true }) {
                            Text("কালার পিকার")
                        }
                    }
                }
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 9f..42f
                )
                if (showBoldToggle) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("বোল্ড (গাঢ়) ফন্ট", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = isBold, onCheckedChange = { isBold = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text, fontSize, colorArgb, isBold) },
                modifier = Modifier.testTag("confirm_pdf_text_button")
            ) {
                Text("যুক্ত করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )

    if (showPicker) {
        ColorPickerDialog(
            title = "লেখার রঙ নির্বাচন করুন",
            initialColorArgb = colorArgb,
            onDismiss = { showPicker = false },
            onColorSelected = {
                colorArgb = it
                showPicker = false
            }
        )
    }
}

private fun DrawScope.drawSelectionBox(x: Float, y: Float, w: Float, h: Float) {
    drawRect(
        color = Color(0xFF1E88E5),
        topLeft = Offset(x, y),
        size = Size(w.coerceAtLeast(12f), h.coerceAtLeast(12f)),
        style = Stroke(
            width = 2.2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )
    )
}

private fun DrawScope.drawShapePreview(
    shapeType: ShapeType,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    color: Color,
    strokePx: Float,
    filled: Boolean
) {
    val style = if (filled) androidx.compose.ui.graphics.drawscope.Fill else Stroke(
        width = strokePx,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )
    when (shapeType) {
        ShapeType.LINE -> {
            drawLine(color = color, start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = strokePx, cap = StrokeCap.Round)
        }
        ShapeType.ARROW -> {
            drawLine(color = color, start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = strokePx, cap = StrokeCap.Round)
            val angle = atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())
            val headLen = (strokePx * 4.5f).coerceIn(14f, 34f)
            val wing = Math.toRadians(28.0)
            val ax1 = (x2 - headLen * cos(angle - wing)).toFloat()
            val ay1 = (y2 - headLen * sin(angle - wing)).toFloat()
            val ax2 = (x2 - headLen * cos(angle + wing)).toFloat()
            val ay2 = (y2 - headLen * sin(angle + wing)).toFloat()
            val headPath = Path().apply {
                moveTo(ax1, ay1)
                lineTo(x2, y2)
                lineTo(ax2, ay2)
            }
            drawPath(
                path = headPath,
                color = color,
                style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        ShapeType.RECTANGLE -> {
            drawRect(
                color = color,
                topLeft = Offset(min(x1, x2), min(y1, y2)),
                size = Size(abs(x2 - x1).coerceAtLeast(4f), abs(y2 - y1).coerceAtLeast(4f)),
                style = style
            )
        }
        ShapeType.CIRCLE -> {
            val cx = (x1 + x2) / 2f
            val cy = (y1 + y2) / 2f
            val r = (hypot(x2 - x1, y2 - y1) / 2f).coerceAtLeast(4f)
            drawCircle(
                color = color,
                radius = r,
                center = Offset(cx, cy),
                style = style
            )
        }
        ShapeType.OVAL -> {
            drawOval(
                color = color,
                topLeft = Offset(min(x1, x2), min(y1, y2)),
                size = Size(abs(x2 - x1).coerceAtLeast(4f), abs(y2 - y1).coerceAtLeast(4f)),
                style = style
            )
        }
    }
}

private fun hitTestEditItem(items: List<PageEditItem>, nx: Float, ny: Float): PageEditItem? {
    return items.asReversed().firstOrNull { item ->
        when (item) {
            is PageEditItem.ImageAddition ->
                nx in item.normX..(item.normX + item.normWidth) &&
                    ny in item.normY..(item.normY + item.normHeight)

            is PageEditItem.SignatureAddition ->
                nx in item.normX..(item.normX + item.normWidth) &&
                    ny in item.normY..(item.normY + item.normHeight)

            is PageEditItem.TextAddition ->
                nx in (item.normX - 0.03f)..(item.normX + 0.35f) &&
                    ny in (item.normY - 0.03f)..(item.normY + 0.08f)

            is PageEditItem.SquareErase ->
                nx in min(item.normLeft, item.normRight)..max(item.normLeft, item.normRight) &&
                    ny in min(item.normTop, item.normBottom)..max(item.normTop, item.normBottom)

            is PageEditItem.TextReplacement ->
                nx in min(item.normLeft, item.normRight)..max(item.normLeft, item.normRight) &&
                    ny in min(item.normTop, item.normBottom)..max(item.normTop, item.normBottom)

            is PageEditItem.ShapeItem -> {
                val left = min(item.startX, item.endX) - 0.03f
                val right = max(item.startX, item.endX) + 0.03f
                val top = min(item.startY, item.endY) - 0.03f
                val bottom = max(item.startY, item.endY) + 0.03f
                nx in left..right && ny in top..bottom
            }

            is PageEditItem.HighlighterStroke -> false
        }
    }
}

private fun moveEditItem(item: PageEditItem, dx: Float, dy: Float): PageEditItem {
    return when (item) {
        is PageEditItem.TextAddition -> item.copy(
            normX = (item.normX + dx).coerceIn(0.01f, 0.94f),
            normY = (item.normY + dy).coerceIn(0.01f, 0.96f)
        )
        is PageEditItem.ImageAddition -> item.copy(
            normX = (item.normX + dx).coerceIn(0f, (1f - item.normWidth).coerceAtLeast(0.05f)),
            normY = (item.normY + dy).coerceIn(0f, (1f - item.normHeight).coerceAtLeast(0.05f))
        )
        is PageEditItem.SignatureAddition -> item.copy(
            normX = (item.normX + dx).coerceIn(0f, (1f - item.normWidth).coerceAtLeast(0.05f)),
            normY = (item.normY + dy).coerceIn(0f, (1f - item.normHeight).coerceAtLeast(0.05f))
        )
        is PageEditItem.SquareErase -> item.copy(
            normLeft = (item.normLeft + dx).coerceIn(0f, 0.98f),
            normRight = (item.normRight + dx).coerceIn(0.02f, 1f),
            normTop = (item.normTop + dy).coerceIn(0f, 0.98f),
            normBottom = (item.normBottom + dy).coerceIn(0.02f, 1f)
        )
        is PageEditItem.ShapeItem -> item.copy(
            startX = (item.startX + dx).coerceIn(0f, 1f),
            endX = (item.endX + dx).coerceIn(0f, 1f),
            startY = (item.startY + dy).coerceIn(0f, 1f),
            endY = (item.endY + dy).coerceIn(0f, 1f)
        )
        is PageEditItem.TextReplacement -> item.copy(
            normLeft = (item.normLeft + dx).coerceIn(0f, 0.95f),
            normRight = (item.normRight + dx).coerceIn(0.05f, 1f),
            normTop = (item.normTop + dy).coerceIn(0f, 0.95f),
            normBottom = (item.normBottom + dy).coerceIn(0.05f, 1f)
        )
        is PageEditItem.HighlighterStroke -> item
    }
}
