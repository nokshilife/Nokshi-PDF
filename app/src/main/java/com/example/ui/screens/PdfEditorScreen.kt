package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.example.engine.LazyPdfSession
import com.example.engine.NormPoint
import com.example.engine.PageEditItem
import com.example.engine.PageOperationSpec
import com.example.engine.PdfEditAndSecurityEngine
import com.example.engine.ShapeType
import com.example.ui.components.ColorPickerDialog
import com.example.ui.components.NokshiIcons
import com.example.ui.components.SignatureLibraryContent
import com.example.ui.i18n.tr
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfEditorScreen(
    pdfEntity: WorkspacePdfEntity,
    unlockedPassword: String,
    edits: List<PageEditItem>,
    signatures: List<SignatureEntity>,
    isBusy: Boolean,
    onBack: () -> Unit,
    onAddBlankPage: (List<PageEditItem>) -> Unit,
    onApplyPageOperations: (List<PageOperationSpec>, List<PageEditItem>) -> Unit,
    onAddEditItem: (PageEditItem) -> Unit,
    onUpdateEditItem: (PageEditItem) -> Unit,
    onDeleteEditItem: (String) -> Unit,
    onUndoLastEdit: () -> Unit,
    onSaveEdits: (List<PageEditItem>) -> Unit,
    onCreateSignature: (String, File, Int) -> Unit,
    onDeleteSignature: (SignatureEntity) -> Unit,
    onRequestPasswordDialog: () -> Unit,
    onOpenInLens: (pageIndex: Int, allEdits: List<PageEditItem>) -> Unit,
    onReadAloud: (pageIndex: Int, allEdits: List<PageEditItem>) -> Unit,
    onOpenPdfToImage: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = remember(configuration.screenWidthDp, density) {
        with(density) { configuration.screenWidthDp.dp.roundToPx() }.coerceIn(900, 1800)
    }

    // Lazy multi-page session (supports 1,000+ pages without freezing)
    var pdfSession by remember { mutableStateOf<LazyPdfSession?>(null) }
    LaunchedEffect(pdfEntity.filePath, pdfEntity.updatedAt, unlockedPassword) {
        val old = pdfSession
        val opened = runCatching {
            LazyPdfSession.open(context, File(pdfEntity.filePath), unlockedPassword)
        }.getOrNull()
        pdfSession = opened
        old?.close()
    }
    DisposableEffect(Unit) {
        onDispose { pdfSession?.close() }
    }

    val totalPages = (pdfSession?.pageCount ?: pdfEntity.pageCount).coerceAtLeast(1)
    val lazyListState = rememberLazyListState()

    // Scroll indicator page index (uses viewport center rather than top edge!)
    val centerVisiblePageIndex by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val items = layoutInfo.visibleItemsInfo
            if (items.isEmpty()) {
                0
            } else {
                val viewportMid = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                val closest = items.minByOrNull { info ->
                    val itemMid = info.offset + info.size / 2
                    abs(itemMid - viewportMid)
                }
                (closest?.index ?: lazyListState.firstVisibleItemIndex)
                    .coerceIn(0, (totalPages - 1).coerceAtLeast(0))
            }
        }
    }

    // STRICT TARGET PAGE INDEX (Requirement ৩):
    // Locked to the exact page where the user double-tapped! All editing, Add Text, Add Image,
    // Signature, Lens, and Read Aloud operations strictly use targetPageIndex!
    var targetPageIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(totalPages) {
        if (targetPageIndex >= totalPages) {
            targetPageIndex = (totalPages - 1).coerceAtLeast(0)
        }
    }

    // Two-finger Pinch-to-Zoom + Pan state (Requirement ২)
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(IntSize(1, 1)) }
    val isZoomedIn = zoomScale > 1.02f

    fun resetZoom() {
        zoomScale = 1f
        panOffset = Offset.Zero
    }

    // Requirement খ: Controls hidden initially; revealed on Double-Tap; hidden when scrolling starts or View is clicked
    var areControlsVisible by remember { mutableStateOf(false) }
    var showInitialDoubleTapHint by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(2400)
        showInitialDoubleTapHint = false
    }

    var activeTool by remember { mutableStateOf(EditorTool.VIEW) }
    var activeShapeType by remember { mutableStateOf(ShapeType.RECTANGLE) }
    var shapeFilled by remember { mutableStateOf(false) }

    var activeColorArgb by remember { mutableIntStateOf(0xFF8E1D2A.toInt()) }
    var highlighterColorArgb by remember { mutableIntStateOf(0xFFFFEB3B.toInt()) }
    var eraseColorArgb by remember { mutableIntStateOf(0xFFFFFFFF.toInt()) }

    var strokeThicknessNorm by remember { mutableFloatStateOf(0.0055f) }
    var highlighterThicknessNorm by remember { mutableFloatStateOf(0.028f) }

    // Active floating item for smooth 60fps Drag + Resize + Rotate
    var activeDraft by remember { mutableStateOf<ActiveDraftState?>(null) }

    fun collectAllEditsIncludingDraft(): List<PageEditItem> {
        val pendingDraftItem = activeDraft?.toPageEditItem()
        return if (pendingDraftItem != null) {
            if (activeDraft?.isExistingEdit == true) {
                edits.map { if (it.id == pendingDraftItem.id) pendingDraftItem else it }
            } else {
                edits + pendingDraftItem
            }
        } else {
            edits
        }
    }

    fun commitActiveDraft() {
        val draft = activeDraft ?: return
        val committed = draft.toPageEditItem()
        if (draft.isExistingEdit) {
            onUpdateEditItem(committed)
        } else {
            onAddEditItem(committed)
        }
        activeDraft = null
    }

    BackHandler {
        when {
            activeDraft != null -> commitActiveDraft()
            isZoomedIn -> resetZoom()
            else -> onBack()
        }
    }

    // Hide controls automatically as soon as continuous scrolling starts
    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress && areControlsVisible) {
            areControlsVisible = false
        }
    }

    // Persistent decoded stamp ImageBitmap cache (never re-decoded during drag!)
    val stampBitmaps = remember { mutableStateMapOf<String, ImageBitmap>() }
    LaunchedEffect(edits, activeDraft?.id) {
        val paths = buildSet {
            edits.forEach {
                when (it) {
                    is PageEditItem.ImageAddition -> add(it.imagePath)
                    is PageEditItem.SignatureAddition -> add(it.signaturePath)
                    else -> Unit
                }
            }
            when (val src = activeDraft?.sourceItem) {
                is PageEditItem.ImageAddition -> add(src.imagePath)
                is PageEditItem.SignatureAddition -> add(src.signaturePath)
                else -> Unit
            }
        }
        val missing = paths.filterNot { stampBitmaps.containsKey(it) }
        if (missing.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                missing.forEach { p ->
                    runCatching { BitmapFactory.decodeFile(p, opts) }.getOrNull()?.let { bmp ->
                        withContext(Dispatchers.Main) {
                            stampBitmaps[p] = bmp.asImageBitmap()
                        }
                    }
                }
            }
        }
    }

    // Extracted text lines cache per page (loaded lazily only when EDIT_TEXT tool is active)
    val detectedLinesByPage = remember(pdfEntity.updatedAt) {
        mutableStateMapOf<Int, List<DetectedPdfTextLine>>()
    }

    var showColorPickerFor by remember { mutableStateOf<String?>(null) }
    var showSignatureSheet by remember { mutableStateOf(false) }
    var showManagePagesSheet by remember { mutableStateOf(false) }
    var lockedTargetPageForPicker by remember { mutableIntStateOf(0) }
    var pendingAddTextPos by remember { mutableStateOf<NormPoint?>(null) }
    var editingDetectedLine by remember { mutableStateOf<DetectedPdfTextLine?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            commitActiveDraft()
            val destinationPage = lockedTargetPageForPicker.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
            val cacheDir = File(context.cacheDir, "editor_stamps").apply { mkdirs() }
            val outFile = File(cacheDir, "img_${System.currentTimeMillis()}.png")
            runCatching {
                val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val bmp = BitmapFactory.decodeStream(input, null, opts)
                    if (bmp != null) {
                        FileOutputStream(outFile).use { fos ->
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
                        }
                        stampBitmaps[outFile.absolutePath] = bmp.asImageBitmap()
                        val ratio = (bmp.height.toFloat() / bmp.width.coerceAtLeast(1)).coerceIn(0.25f, 1.8f)
                        val item = PageEditItem.ImageAddition(
                            pageIndex = destinationPage,
                            normX = 0.28f,
                            normY = 0.28f,
                            normWidth = 0.42f,
                            normHeight = (0.42f * ratio).coerceAtMost(0.55f),
                            imagePath = outFile.absolutePath
                        )
                        targetPageIndex = destinationPage
                        activeDraft = ActiveDraftState(item, isExistingEdit = false)
                        activeTool = EditorTool.SELECT
                    }
                }
            }
        }
    }

    // Helper to place a signature strictly onto the locked double-tapped target page!
    fun placeSignatureOnTargetPage(sig: SignatureEntity) {
        commitActiveDraft()
        val sigFile = File(sig.imagePath)
        if (!sigFile.exists()) return
        val destinationPage = lockedTargetPageForPicker.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val bmp = BitmapFactory.decodeFile(sigFile.absolutePath, opts)
        val aspect = if (bmp != null) {
            stampBitmaps[sigFile.absolutePath] = bmp.asImageBitmap()
            (bmp.height.toFloat() / bmp.width.coerceAtLeast(1)).coerceIn(0.18f, 1.1f)
        } else {
            0.38f
        }
        val sigItem = PageEditItem.SignatureAddition(
            pageIndex = destinationPage,
            normX = 0.28f,
            normY = 0.34f,
            normWidth = 0.38f,
            normHeight = (0.38f * aspect).coerceIn(0.07f, 0.32f),
            signaturePath = sig.imagePath
        )
        targetPageIndex = destinationPage
        activeDraft = ActiveDraftState(sigItem, isExistingEdit = false)
        activeTool = EditorTool.SELECT
        showSignatureSheet = false
    }

    val isDrawingGestureTool = activeTool == EditorTool.FREEHAND ||
        activeTool == EditorTool.HIGHLIGHTER ||
        activeTool == EditorTool.SQUARE_ERASE ||
        activeTool == EditorTool.SHAPE

    val scrollEnabled = !isDrawingGestureTool && activeDraft == null && !isZoomedIn
    val isEditingScoped = areControlsVisible || activeTool != EditorTool.VIEW || activeDraft != null

    val currentActiveTool by rememberUpdatedState(activeTool)
    val currentHasDraft by rememberUpdatedState(activeDraft != null)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1F2227))
    ) {
        // FULL-WIDTH CONTINUOUS LAZY PDF VIEWPORT + TWO-FINGER PINCH ZOOM + SINGLE-PAGE CENTERING
        if (pdfSession == null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewportSize = it }
                    .pointerInput(Unit) {
                        // Smooth two-finger pinch-to-zoom (1x..4x) + 1-finger pan when zoomed in (Requirement ২)
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                val pressedCount = event.changes.count { it.pressed }
                                val canOneFingerPan = zoomScale > 1.02f &&
                                    currentActiveTool == EditorTool.VIEW &&
                                    !currentHasDraft

                                if (pressedCount >= 2 || (pressedCount == 1 && canOneFingerPan)) {
                                    val zoomChange = if (pressedCount >= 2) event.calculateZoom() else 1f
                                    val panChange = event.calculatePan()
                                    val centroid = event.calculateCentroid(useCurrent = false)

                                    val oldScale = zoomScale
                                    val newScale = (oldScale * zoomChange).coerceIn(1f, 4f)
                                    val vw = viewportSize.width.coerceAtLeast(1).toFloat()
                                    val vh = viewportSize.height.coerceAtLeast(1).toFloat()

                                    if (newScale <= 1.01f) {
                                        zoomScale = 1f
                                        panOffset = Offset.Zero
                                    } else {
                                        val effectiveZoom = newScale / oldScale
                                        val rawOffset = if (centroid != Offset.Unspecified && pressedCount >= 2) {
                                            (panOffset - centroid) * effectiveZoom + centroid + panChange
                                        } else {
                                            panOffset + panChange
                                        }
                                        val maxPanX = 0f
                                        val minPanX = -(vw * (newScale - 1f))
                                        val maxPanY = 0f
                                        val minPanY = -(vh * (newScale - 1f))
                                        zoomScale = newScale
                                        panOffset = Offset(
                                            x = rawOffset.x.coerceIn(minPanX, maxPanX),
                                            y = rawOffset.y.coerceIn(minPanY, maxPanY)
                                        )
                                    }

                                    if (pressedCount >= 2 || canOneFingerPan) {
                                        event.changes.forEach { ch ->
                                            if (ch.positionChanged()) ch.consume()
                                        }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }
            ) {
                LazyColumn(
                    state = lazyListState,
                    userScrollEnabled = scrollEnabled,
                    contentPadding = PaddingValues(0.dp),
                    // Requirement ১: Center 1-page PDFs vertically instead of sticking to the very top!
                    verticalArrangement = if (totalPages == 1) {
                        Arrangement.Center
                    } else {
                        Arrangement.spacedBy(4.dp)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0f, 0f)
                            scaleX = zoomScale
                            scaleY = zoomScale
                            translationX = panOffset.x
                            translationY = panOffset.y
                        }
                        .testTag("lazy_pdf_viewer")
                ) {
                    items(
                        count = totalPages,
                        key = { pageIdx -> pageIdx }
                    ) { pageIdx ->
                        val pageEdits = remember(edits, pageIdx, activeDraft?.id) {
                            edits.filter { it.pageIndex == pageIdx && it.id != activeDraft?.id }
                        }
                        val draftOnThisPage = activeDraft?.takeIf { it.pageIndex == pageIdx }
                        // Requirement ৩: Editing operations are strictly scoped to the double-tapped targetPageIndex!
                        val isTargetEditablePage = pageIdx == targetPageIndex

                        // Lazy-load text lines only when EDIT_TEXT is active on the target page
                        LaunchedEffect(activeTool, pageIdx, targetPageIndex, pdfEntity.updatedAt) {
                            if (activeTool == EditorTool.EDIT_TEXT &&
                                isTargetEditablePage &&
                                !detectedLinesByPage.containsKey(pageIdx)
                            ) {
                                val lines = runCatching {
                                    PdfEditAndSecurityEngine.extractPageTextLines(
                                        context = context,
                                        file = File(pdfEntity.filePath),
                                        pageIndex = pageIdx,
                                        password = unlockedPassword
                                    )
                                }.getOrDefault(emptyList())
                                detectedLinesByPage[pageIdx] = lines
                            }
                        }

                        FullWidthLazyPdfPageItem(
                            session = pdfSession!!,
                            pageIndex = pageIdx,
                            isTargetEditablePage = isTargetEditablePage,
                            showTargetBorder = isEditingScoped && totalPages > 1 && isTargetEditablePage,
                            targetRenderWidthPx = screenWidthPx,
                            activeTool = if (isTargetEditablePage) activeTool else EditorTool.VIEW,
                            activeShapeType = activeShapeType,
                            shapeFilled = shapeFilled,
                            activeColorArgb = activeColorArgb,
                            highlighterColorArgb = highlighterColorArgb,
                            eraseColorArgb = eraseColorArgb,
                            strokeThicknessNorm = strokeThicknessNorm,
                            highlighterThicknessNorm = highlighterThicknessNorm,
                            committedPageEdits = pageEdits,
                            detectedLines = if (isTargetEditablePage) detectedLinesByPage[pageIdx].orEmpty() else emptyList(),
                            activeDraftOnPage = draftOnThisPage,
                            hasAnyActiveDraft = activeDraft != null,
                            stampBitmaps = stampBitmaps,
                            onDoubleTapPage = {
                                if (activeDraft != null) {
                                    // Requirement গ: Double-tap outside applies the floating item!
                                    commitActiveDraft()
                                    if (pageIdx != targetPageIndex) {
                                        targetPageIndex = pageIdx
                                        lockedTargetPageForPicker = pageIdx
                                    }
                                } else {
                                    // Requirement খ & ৩: Lock targetPageIndex strictly to THIS double-tapped page!
                                    if (!areControlsVisible) {
                                        targetPageIndex = pageIdx
                                        lockedTargetPageForPicker = pageIdx
                                        areControlsVisible = true
                                    } else if (targetPageIndex != pageIdx) {
                                        // Double-tapping a different page while tools are open switches focus strictly to that page
                                        targetPageIndex = pageIdx
                                        lockedTargetPageForPicker = pageIdx
                                    } else {
                                        activeTool = EditorTool.VIEW
                                        areControlsVisible = false
                                    }
                                }
                            },
                            onRequestAddTextAt = { pt ->
                                if (isTargetEditablePage) {
                                    commitActiveDraft()
                                    lockedTargetPageForPicker = pageIdx
                                    pendingAddTextPos = pt
                                }
                            },
                            onRequestEditDetectedLine = { line ->
                                if (isTargetEditablePage) {
                                    commitActiveDraft()
                                    editingDetectedLine = line
                                }
                            },
                            onRequestOpenSignatureSheet = {
                                if (isTargetEditablePage) {
                                    lockedTargetPageForPicker = pageIdx
                                    showSignatureSheet = true
                                }
                            },
                            onSelectExistingEditForTransform = { hitItem ->
                                if (isTargetEditablePage || !isEditingScoped) {
                                    commitActiveDraft()
                                    targetPageIndex = pageIdx
                                    lockedTargetPageForPicker = pageIdx
                                    activeDraft = ActiveDraftState(hitItem, isExistingEdit = true)
                                    activeTool = EditorTool.SELECT
                                }
                            },
                            onStartNewDraft = { newItem ->
                                if (isTargetEditablePage) {
                                    commitActiveDraft()
                                    activeDraft = ActiveDraftState(newItem, isExistingEdit = false)
                                }
                            },
                            onCommitStrokeEdit = { strokeItem ->
                                if (isTargetEditablePage) {
                                    onAddEditItem(strokeItem)
                                }
                            },
                            onApplyActiveDraft = { commitActiveDraft() },
                            onDeleteActiveDraft = {
                                val draft = activeDraft
                                if (draft != null) {
                                    if (draft.isExistingEdit) {
                                        onDeleteEditItem(draft.id)
                                    }
                                    activeDraft = null
                                }
                            }
                        )
                    }
                }
            }
        }

        // Subtle Page Counter Pill + Zoom Reset Chip + Return-to-View Chip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 12.dp,
                    bottom = if (areControlsVisible) 142.dp else 18.dp
                )
        ) {
            if (isZoomedIn) {
                AssistChip(
                    onClick = { resetZoom() },
                    label = { Text("${(zoomScale * 100).roundToInt()}% • " + tr("Reset", "১০০%")) },
                    leadingIcon = {
                        Icon(NokshiIcons.ZoomIn, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xEE1A1C1E),
                        labelColor = Color.White,
                        leadingIconContentColor = Color(0xFFFFCA28)
                    ),
                    modifier = Modifier.testTag("reset_zoom_chip")
                )
            }
            if (!areControlsVisible && isDrawingGestureTool) {
                AssistChip(
                    onClick = {
                        commitActiveDraft()
                        activeTool = EditorTool.VIEW
                    },
                    label = { Text(tr("View / Scroll", "ভিউ / স্ক্রোল")) },
                    leadingIcon = {
                        Icon(NokshiIcons.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xEE1A1C1E),
                        labelColor = Color.White,
                        leadingIconContentColor = Color(0xFFFFCA28)
                    )
                )
            }
            val shownPageIdx = if (isEditingScoped) targetPageIndex else centerVisiblePageIndex
            Surface(
                color = if (isEditingScoped) Color(0xEE8E1D2A) else Color(0xBB141619),
                contentColor = Color.White,
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = if (isEditingScoped) {
                        tr("Target Page ${shownPageIdx + 1} / $totalPages", "পেজ ${shownPageIdx + 1} / $totalPages (এডিট)")
                    } else {
                        "${shownPageIdx + 1} / $totalPages"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }

        // Initial non-blocking Double-Tap & Pinch-Zoom Hint Pill
        AnimatedVisibility(
            visible = showInitialDoubleTapHint && !areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
        ) {
            Surface(
                color = Color(0xDD1A1C1E),
                contentColor = Color.White,
                shape = RoundedCornerShape(999.dp),
                shadowElevation = 6.dp
            ) {
                Text(
                    text = tr(
                        "Pinch with 2 fingers to Zoom • Double-tap a page for Tools",
                        "দুই আঙুলে জুম করুন • নির্দিষ্ট পেজে ডাবল ট্যাপ করলে টুলস আসবে"
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // FLOATING TOP CONTROLS BAR (Revealed on Double-Tap, Scoped strictly to targetPageIndex!)
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 10.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(bottom = 6.dp)
                ) {
                    // Row 1: Back, Title + Active Target Page Badge, Undo, View (Hide), Save
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        IconButton(
                            onClick = {
                                commitActiveDraft()
                                onBack()
                            },
                            modifier = Modifier.testTag("editor_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Back", "ফিরে যান"))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pdfEntity.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tr(
                                    "🎯 Active Page ${targetPageIndex + 1} of $totalPages",
                                    "🎯 ডাবল ট্যাপ করা পেজ: ${targetPageIndex + 1} / $totalPages"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = onUndoLastEdit,
                            enabled = edits.isNotEmpty(),
                            modifier = Modifier.testTag("editor_undo_button")
                        ) {
                            Icon(NokshiIcons.Undo, contentDescription = tr("Undo", "আনডু"))
                        }
                        IconButton(
                            onClick = {
                                commitActiveDraft()
                                activeTool = EditorTool.VIEW
                                areControlsVisible = false
                            },
                            modifier = Modifier.testTag("editor_view_hide_button")
                        ) {
                            Icon(NokshiIcons.Visibility, contentDescription = tr("View Mode (Hide Tools)", "ভিউ মোড"))
                        }
                        Button(
                            onClick = {
                                val allEdits = collectAllEditsIncludingDraft()
                                activeDraft = null
                                onSaveEdits(allEdits)
                            },
                            enabled = !isBusy,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("editor_save_button")
                        ) {
                            Icon(NokshiIcons.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(tr("Save", "সেভ"))
                        }
                    }

                    // Row 2: Side-by-Side "Open in Lens" + "Read Aloud" (for targetPageIndex!) + Manage Pages + Add Page + Image + Lock
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        Button(
                            onClick = {
                                val allEdits = collectAllEditsIncludingDraft()
                                onOpenInLens(targetPageIndex, allEdits)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("editor_open_in_lens_button")
                        ) {
                            Icon(NokshiIcons.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = tr("Lens (P.${targetPageIndex + 1})", "Lens (পেজ ${targetPageIndex + 1})"),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = {
                                val allEdits = collectAllEditsIncludingDraft()
                                onReadAloud(targetPageIndex, allEdits)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F6E56),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("editor_read_aloud_button")
                        ) {
                            Icon(NokshiIcons.VolumeUp, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = tr("Read (P.${targetPageIndex + 1})", "Read (পেজ ${targetPageIndex + 1})"),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Requirement ৪: Manage Pages (Delete, Reorder, Rotate Specific Pages)
                        IconButton(
                            onClick = {
                                commitActiveDraft()
                                showManagePagesSheet = true
                            },
                            modifier = Modifier.testTag("editor_manage_pages_button")
                        ) {
                            Icon(
                                NokshiIcons.GridView,
                                contentDescription = tr("Manage Pages (Delete, Reorder, Rotate)", "পেজ ম্যানেজার (ডিলিট, আগে-পিছে, রোটেশন)")
                            )
                        }
                        IconButton(
                            onClick = {
                                val allEdits = collectAllEditsIncludingDraft()
                                activeDraft = null
                                onAddBlankPage(allEdits)
                            },
                            modifier = Modifier.testTag("editor_add_blank_page_button")
                        ) {
                            Icon(NokshiIcons.NoteAdd, contentDescription = tr("Add Blank Page", "নতুন সাদা পেজ"))
                        }
                        IconButton(
                            onClick = onOpenPdfToImage,
                            modifier = Modifier.testTag("editor_pdf_to_image_button")
                        ) {
                            Icon(NokshiIcons.Image, contentDescription = tr("PDF to Image", "PDF থেকে ইমেজ"))
                        }
                        IconButton(
                            onClick = onRequestPasswordDialog,
                            modifier = Modifier.testTag("editor_password_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = tr("Password Lock", "পাসওয়ার্ড"))
                        }
                    }
                }
            }
        }

        // FLOATING BOTTOM TOOL DOCK (Revealed on Double-Tap, Scoped strictly to targetPageIndex!)
        AnimatedVisibility(
            visible = areControlsVisible,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            EditorBottomToolDock(
                activeTool = activeTool,
                activeShapeType = activeShapeType,
                shapeFilled = shapeFilled,
                activeColorArgb = activeColorArgb,
                highlighterColorArgb = highlighterColorArgb,
                eraseColorArgb = eraseColorArgb,
                strokeThicknessNorm = strokeThicknessNorm,
                highlighterThicknessNorm = highlighterThicknessNorm,
                activeDraft = activeDraft,
                onSelectTool = { tool ->
                    if (tool != EditorTool.SELECT) {
                        commitActiveDraft()
                    }
                    activeTool = tool
                    // Lock the target page to exact double-tapped targetPageIndex!
                    lockedTargetPageForPicker = targetPageIndex
                    when (tool) {
                        EditorTool.ADD_IMAGE -> {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        EditorTool.SIGNATURE -> {
                            showSignatureSheet = true
                        }
                        EditorTool.ADD_TEXT -> {
                            pendingAddTextPos = NormPoint(0.24f, 0.34f)
                        }
                        else -> Unit
                    }
                },
                onHideControlsForViewing = {
                    commitActiveDraft()
                    activeTool = EditorTool.VIEW
                    areControlsVisible = false
                },
                onSelectShapeType = { activeShapeType = it },
                onToggleShapeFilled = { shapeFilled = it },
                onStrokeThicknessChange = { strokeThicknessNorm = it },
                onHighlighterThicknessChange = { highlighterThicknessNorm = it },
                onQuickFreehandColor = { activeColorArgb = it },
                onQuickHighlightColor = { highlighterColorArgb = it },
                onOpenColorPicker = { target -> showColorPickerFor = target },
                onApplyActiveDraft = { commitActiveDraft() },
                onDeleteActiveDraft = {
                    val d = activeDraft
                    if (d != null) {
                        if (d.isExistingEdit) onDeleteEditItem(d.id)
                        activeDraft = null
                    }
                }
            )
        }
    }

    // Color Picker Dialog
    showColorPickerFor?.let { target ->
        val initCol = when (target) {
            "HIGHLIGHT" -> highlighterColorArgb
            "ERASE" -> eraseColorArgb
            else -> activeColorArgb
        }
        ColorPickerDialog(
            initialColorArgb = initCol,
            onDismiss = { showColorPickerFor = null },
            onColorSelected = { selected ->
                when (target) {
                    "HIGHLIGHT" -> highlighterColorArgb = selected
                    "ERASE" -> eraseColorArgb = selected
                    else -> activeColorArgb = selected
                }
                showColorPickerFor = null
            }
        )
    }

    // Add New Text Dialog -> Places strictly onto lockedTargetPageForPicker (the double-tapped page!)
    pendingAddTextPos?.let { pos ->
        val destinationPage = lockedTargetPageForPicker.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
        AddTextToPdfDialog(
            initialColorArgb = activeColorArgb,
            onDismiss = { pendingAddTextPos = null },
            onPickColor = { showColorPickerFor = "MAIN" },
            onConfirm = { text, fontSizePt, isBold ->
                val item = PageEditItem.TextAddition(
                    pageIndex = destinationPage,
                    normX = pos.x.coerceIn(0.05f, 0.78f),
                    normY = pos.y.coerceIn(0.05f, 0.88f),
                    text = text,
                    fontSizePt = fontSizePt,
                    colorArgb = activeColorArgb,
                    isBold = isBold
                )
                targetPageIndex = destinationPage
                activeDraft = ActiveDraftState(item, isExistingEdit = false)
                activeTool = EditorTool.SELECT
                pendingAddTextPos = null
            }
        )
    }

    // Edit Existing Detected Text Dialog
    editingDetectedLine?.let { detected ->
        EditDetectedTextDialog(
            line = detected,
            colorArgb = activeColorArgb,
            onDismiss = { editingDetectedLine = null },
            onPickColor = { showColorPickerFor = "MAIN" },
            onConfirm = { newText ->
                onAddEditItem(
                    PageEditItem.TextReplacement(
                        pageIndex = detected.pageIndex,
                        normLeft = detected.normLeft,
                        normTop = detected.normTop,
                        normRight = detected.normRight,
                        normBottom = detected.normBottom,
                        originalText = detected.text,
                        newText = newText,
                        colorArgb = activeColorArgb,
                        fontSizePt = detected.fontSizePt
                    )
                )
                editingDetectedLine = null
            }
        )
    }

    // Signature Library Bottom Sheet (Scoped strictly to lockedTargetPageForPicker!)
    if (showSignatureSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val destinationPage = lockedTargetPageForPicker.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { showSignatureSheet = false }
        ) {
            SignatureLibraryContent(
                signatures = signatures,
                selectionModeLabel = tr(
                    "Tap any signature (or draw/import a new one) to place, drag & rotate it strictly on Page ${destinationPage + 1}.",
                    "যেকোনো স্বাক্ষরে ট্যাপ করুন (অথবা নতুন আঁকুন/ইমেজ আনুন) — সরাসরি ডাবল ট্যাপ করা পেজ ${destinationPage + 1}-এ বসবে।"
                ),
                onSelectSignature = { sig -> placeSignatureOnTargetPage(sig) },
                onCreateSignature = onCreateSignature,
                onDeleteSignature = onDeleteSignature
            )
        }
    }

    // Requirement ৪: Manage Pages Bottom Sheet (Delete, Reorder, Rotate Specific Pages)
    if (showManagePagesSheet && pdfSession != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { showManagePagesSheet = false }
        ) {
            ManagePdfPagesSheetContent(
                session = pdfSession!!,
                initialFocusPageIndex = targetPageIndex,
                onDismiss = { showManagePagesSheet = false },
                onApplyChanges = { updatedSpecs ->
                    showManagePagesSheet = false
                    val allEdits = collectAllEditsIncludingDraft()
                    activeDraft = null
                    resetZoom()
                    onApplyPageOperations(updatedSpecs, allEdits)
                }
            )
        }
    }
}

@Composable
private fun FullWidthLazyPdfPageItem(
    session: LazyPdfSession,
    pageIndex: Int,
    isTargetEditablePage: Boolean,
    showTargetBorder: Boolean,
    targetRenderWidthPx: Int,
    activeTool: EditorTool,
    activeShapeType: ShapeType,
    shapeFilled: Boolean,
    activeColorArgb: Int,
    highlighterColorArgb: Int,
    eraseColorArgb: Int,
    strokeThicknessNorm: Float,
    highlighterThicknessNorm: Float,
    committedPageEdits: List<PageEditItem>,
    detectedLines: List<DetectedPdfTextLine>,
    activeDraftOnPage: ActiveDraftState?,
    hasAnyActiveDraft: Boolean,
    stampBitmaps: Map<String, ImageBitmap>,
    onDoubleTapPage: () -> Unit,
    onRequestAddTextAt: (NormPoint) -> Unit,
    onRequestEditDetectedLine: (DetectedPdfTextLine) -> Unit,
    onRequestOpenSignatureSheet: () -> Unit,
    onSelectExistingEditForTransform: (PageEditItem) -> Unit,
    onStartNewDraft: (PageEditItem) -> Unit,
    onCommitStrokeEdit: (PageEditItem) -> Unit,
    onApplyActiveDraft: () -> Unit,
    onDeleteActiveDraft: () -> Unit
) {
    val textMeasurer = rememberTextMeasurer()

    var aspectRatio by remember(session, pageIndex) {
        mutableFloatStateOf(session.getCachedAspectRatio(pageIndex))
    }
    var pageBitmap by remember(session, pageIndex) {
        mutableStateOf(session.getCachedBitmap(pageIndex))
    }
    var pagePixelSize by remember { mutableStateOf(IntSize(1, 1)) }

    LaunchedEffect(session, pageIndex, targetRenderWidthPx) {
        aspectRatio = session.getOrLoadAspectRatio(pageIndex)
        val rendered = session.renderPage(pageIndex, targetRenderWidthPx)
        if (rendered != null) {
            aspectRatio = session.getCachedAspectRatio(pageIndex)
            pageBitmap = rendered
        }
    }

    // Live finger drawing state for Freehand Pen, Highlighter, Square Erase, and Shapes
    val livePoints = remember { mutableStateListOf<NormPoint>() }
    var dragStartNorm by remember { mutableStateOf<NormPoint?>(null) }
    var dragCurrentNorm by remember { mutableStateOf<NormPoint?>(null) }

    val currentTool by rememberUpdatedState(activeTool)
    val currentIsTargetEditable by rememberUpdatedState(isTargetEditablePage)
    val currentShape by rememberUpdatedState(activeShapeType)
    val currentFilled by rememberUpdatedState(shapeFilled)
    val currentColor by rememberUpdatedState(activeColorArgb)
    val currentHiColor by rememberUpdatedState(highlighterColorArgb)
    val currentEraseColor by rememberUpdatedState(eraseColorArgb)
    val currentStrokeW by rememberUpdatedState(strokeThicknessNorm)
    val currentHiW by rememberUpdatedState(highlighterThicknessNorm)
    val currentEdits by rememberUpdatedState(committedPageEdits)
    val currentDetected by rememberUpdatedState(detectedLines)
    val currentHasDraft by rememberUpdatedState(hasAnyActiveDraft)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTapPage)

    val isStrokeTool = isTargetEditablePage && (
        activeTool == EditorTool.FREEHAND ||
            activeTool == EditorTool.HIGHLIGHTER ||
            activeTool == EditorTool.SQUARE_ERASE ||
            activeTool == EditorTool.SHAPE
        )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio.coerceIn(0.25f, 3.5f))
            .background(Color.White)
            .then(
                if (showTargetBorder) {
                    Modifier.border(2.5.dp, Color(0xFFFFCA28))
                } else Modifier
            )
            .onSizeChanged { pagePixelSize = it }
            .pointerInput(pageIndex) {
                detectTapGestures(
                    onDoubleTap = {
                        currentOnDoubleTap()
                    },
                    onTap = { tapOffset ->
                        if (currentHasDraft || !currentIsTargetEditable) return@detectTapGestures
                        val w = size.width.coerceAtLeast(1).toFloat()
                        val h = size.height.coerceAtLeast(1).toFloat()
                        val nx = (tapOffset.x / w).coerceIn(0f, 1f)
                        val ny = (tapOffset.y / h).coerceIn(0f, 1f)

                        when (currentTool) {
                            EditorTool.ADD_TEXT -> onRequestAddTextAt(NormPoint(nx, ny))
                            EditorTool.EDIT_TEXT -> {
                                val hit = currentDetected.firstOrNull { line ->
                                    nx in (line.normLeft - 0.02f)..(line.normRight + 0.02f) &&
                                        ny in (line.normTop - 0.015f)..(line.normBottom + 0.015f)
                                }
                                if (hit != null) onRequestEditDetectedLine(hit)
                            }
                            EditorTool.SIGNATURE -> onRequestOpenSignatureSheet()
                            else -> {
                                val hitEdit = hitTestCommittedItem(currentEdits, nx, ny)
                                if (hitEdit != null) {
                                    onSelectExistingEditForTransform(hitEdit)
                                }
                            }
                        }
                    }
                )
            }
            .then(
                if (isStrokeTool && activeDraftOnPage == null) {
                    Modifier.pointerInput(pageIndex, activeTool) {
                        detectDragGestures(
                            onDragStart = { start ->
                                val w = size.width.coerceAtLeast(1).toFloat()
                                val h = size.height.coerceAtLeast(1).toFloat()
                                val pt = NormPoint((start.x / w).coerceIn(0f, 1f), (start.y / h).coerceIn(0f, 1f))
                                when (currentTool) {
                                    EditorTool.FREEHAND, EditorTool.HIGHLIGHTER -> {
                                        livePoints.clear()
                                        livePoints.add(pt)
                                    }
                                    EditorTool.SQUARE_ERASE, EditorTool.SHAPE -> {
                                        dragStartNorm = pt
                                        dragCurrentNorm = pt
                                    }
                                    else -> Unit
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val w = size.width.coerceAtLeast(1).toFloat()
                                val h = size.height.coerceAtLeast(1).toFloat()
                                val pt = NormPoint(
                                    (change.position.x / w).coerceIn(0f, 1f),
                                    (change.position.y / h).coerceIn(0f, 1f)
                                )
                                when (currentTool) {
                                    EditorTool.FREEHAND, EditorTool.HIGHLIGHTER -> livePoints.add(pt)
                                    EditorTool.SQUARE_ERASE, EditorTool.SHAPE -> dragCurrentNorm = pt
                                    else -> Unit
                                }
                            },
                            onDragEnd = {
                                when (currentTool) {
                                    EditorTool.FREEHAND -> {
                                        if (livePoints.size >= 2) {
                                            onCommitStrokeEdit(
                                                PageEditItem.FreehandStroke(
                                                    pageIndex = pageIndex,
                                                    points = livePoints.toList(),
                                                    colorArgb = currentColor,
                                                    strokeWidthNorm = currentStrokeW
                                                )
                                            )
                                        }
                                        livePoints.clear()
                                    }
                                    EditorTool.HIGHLIGHTER -> {
                                        if (livePoints.size >= 2) {
                                            onCommitStrokeEdit(
                                                PageEditItem.HighlighterStroke(
                                                    pageIndex = pageIndex,
                                                    points = livePoints.toList(),
                                                    colorArgb = currentHiColor,
                                                    strokeWidthNorm = currentHiW
                                                )
                                            )
                                        }
                                        livePoints.clear()
                                    }
                                    EditorTool.SQUARE_ERASE -> {
                                        val s = dragStartNorm
                                        val e = dragCurrentNorm
                                        if (s != null && e != null &&
                                            abs(e.x - s.x) > 0.015f && abs(e.y - s.y) > 0.012f
                                        ) {
                                            onStartNewDraft(
                                                PageEditItem.SquareErase(
                                                    pageIndex = pageIndex,
                                                    normLeft = min(s.x, e.x),
                                                    normTop = min(s.y, e.y),
                                                    normRight = max(s.x, e.x),
                                                    normBottom = max(s.y, e.y),
                                                    fillColorArgb = currentEraseColor
                                                )
                                            )
                                        }
                                        dragStartNorm = null
                                        dragCurrentNorm = null
                                    }
                                    EditorTool.SHAPE -> {
                                        val s = dragStartNorm
                                        val e = dragCurrentNorm
                                        if (s != null && e != null && hypot(e.x - s.x, e.y - s.y) > 0.018f) {
                                            onStartNewDraft(
                                                PageEditItem.ShapeItem(
                                                    pageIndex = pageIndex,
                                                    shapeType = currentShape,
                                                    startX = s.x,
                                                    startY = s.y,
                                                    endX = e.x,
                                                    endY = e.y,
                                                    colorArgb = currentColor,
                                                    strokeWidthNorm = currentStrokeW,
                                                    isFilled = currentFilled
                                                )
                                            )
                                        }
                                        dragStartNorm = null
                                        dragCurrentNorm = null
                                    }
                                    else -> Unit
                                }
                            }
                        )
                    }
                } else Modifier
            )
            .testTag("pdf_page_$pageIndex")
    ) {
        val bmp = pageBitmap
        if (bmp == null) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        } else {
            Image(
                bitmap = remember(bmp) { bmp.asImageBitmap() },
                contentDescription = "Page ${pageIndex + 1}",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Committed Edits & Live Drawing Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cw = size.width
            val ch = size.height

            if (activeTool == EditorTool.EDIT_TEXT && isTargetEditablePage) {
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                detectedLines.forEach { line ->
                    val lx = line.normLeft * cw
                    val ly = line.normTop * ch
                    val lw = ((line.normRight - line.normLeft) * cw).coerceAtLeast(16f)
                    val lh = ((line.normBottom - line.normTop) * ch).coerceAtLeast(14f)
                    drawRect(color = Color(0x221F3A60), topLeft = Offset(lx, ly), size = Size(lw, lh))
                    drawRect(
                        color = Color(0xFF1F3A60),
                        topLeft = Offset(lx, ly),
                        size = Size(lw, lh),
                        style = Stroke(width = 1.5f, pathEffect = dash)
                    )
                }
            }

            committedPageEdits.forEach { item ->
                when (item) {
                    is PageEditItem.SquareErase -> {
                        val left = min(item.normLeft, item.normRight) * cw
                        val top = min(item.normTop, item.normBottom) * ch
                        val rw = abs(item.normRight - item.normLeft) * cw
                        val rh = abs(item.normBottom - item.normTop) * ch
                        withTransform({
                            rotate(item.rotationDeg, Offset(left + rw / 2f, top + rh / 2f))
                        }) {
                            drawRect(color = Color(item.fillColorArgb), topLeft = Offset(left, top), size = Size(rw, rh))
                        }
                    }

                    is PageEditItem.TextReplacement -> {
                        val left = min(item.normLeft, item.normRight) * cw
                        val top = min(item.normTop, item.normBottom) * ch
                        val rw = (abs(item.normRight - item.normLeft) * cw).coerceAtLeast(40f)
                        val rh = (abs(item.normBottom - item.normTop) * ch).coerceAtLeast(18f)
                        drawRect(color = Color(item.bgColorArgb), topLeft = Offset(left - 2f, top - 2f), size = Size(rw + 4f, rh + 4f))
                        if (item.newText.isNotBlank()) {
                            val scaledSp = ((item.fontSizePt / 595f) * cw / density).coerceIn(9f, 34f).sp
                            val measured = textMeasurer.measure(
                                text = item.newText,
                                style = TextStyle(color = Color(item.colorArgb), fontSize = scaledSp, fontWeight = FontWeight.Medium)
                            )
                            drawText(measured, topLeft = Offset(left, top))
                        }
                    }

                    is PageEditItem.FreehandStroke -> {
                        if (item.points.size >= 2) {
                            val p = Path().apply {
                                moveTo(item.points.first().x * cw, item.points.first().y * ch)
                                item.points.drop(1).forEach { lineTo(it.x * cw, it.y * ch) }
                            }
                            drawPath(
                                path = p,
                                color = Color(item.colorArgb),
                                style = Stroke(
                                    width = (item.strokeWidthNorm * cw).coerceAtLeast(2.2f),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
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
                        val x1 = item.startX * cw
                        val y1 = item.startY * ch
                        val x2 = item.endX * cw
                        val y2 = item.endY * ch
                        val cx = (x1 + x2) / 2f
                        val cy = (y1 + y2) / 2f
                        withTransform({
                            rotate(item.rotationDeg, Offset(cx, cy))
                        }) {
                            drawShapePreview(
                                shapeType = item.shapeType,
                                x1 = x1,
                                y1 = y1,
                                x2 = x2,
                                y2 = y2,
                                color = Color(item.colorArgb),
                                strokePx = (item.strokeWidthNorm * cw).coerceAtLeast(2.5f),
                                filled = item.isFilled
                            )
                        }
                    }

                    is PageEditItem.TextAddition -> {
                        val scaledSp = ((item.fontSizePt / 595f) * cw / density).coerceIn(10f, 52f).sp
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
                        withTransform({
                            rotate(item.rotationDeg, Offset(tx + measured.size.width / 2f, ty + measured.size.height / 2f))
                        }) {
                            drawText(measured, topLeft = Offset(tx, ty))
                        }
                    }

                    is PageEditItem.ImageAddition -> {
                        val img = stampBitmaps[item.imagePath]
                        if (img != null) {
                            val ix = item.normX * cw
                            val iy = item.normY * ch
                            val iw = item.normWidth * cw
                            val ih = item.normHeight * ch
                            withTransform({
                                rotate(item.rotationDeg, Offset(ix + iw / 2f, iy + ih / 2f))
                            }) {
                                drawImage(
                                    image = img,
                                    dstOffset = IntOffset(ix.roundToInt(), iy.roundToInt()),
                                    dstSize = IntSize(iw.roundToInt().coerceAtLeast(10), ih.roundToInt().coerceAtLeast(10))
                                )
                            }
                        }
                    }

                    is PageEditItem.SignatureAddition -> {
                        val sig = stampBitmaps[item.signaturePath]
                        if (sig != null) {
                            val sx = item.normX * cw
                            val sy = item.normY * ch
                            val sw = item.normWidth * cw
                            val sh = item.normHeight * ch
                            withTransform({
                                rotate(item.rotationDeg, Offset(sx + sw / 2f, sy + sh / 2f))
                            }) {
                                drawImage(
                                    image = sig,
                                    dstOffset = IntOffset(sx.roundToInt(), sy.roundToInt()),
                                    dstSize = IntSize(sw.roundToInt().coerceAtLeast(10), sh.roundToInt().coerceAtLeast(10))
                                )
                            }
                        }
                    }
                }
            }

            // Live Freehand / Highlighter Stroke Preview
            if (livePoints.size >= 2) {
                val p = Path().apply {
                    moveTo(livePoints.first().x * cw, livePoints.first().y * ch)
                    livePoints.drop(1).forEach { lineTo(it.x * cw, it.y * ch) }
                }
                if (activeTool == EditorTool.FREEHAND) {
                    drawPath(
                        path = p,
                        color = Color(activeColorArgb),
                        style = Stroke(width = (strokeThicknessNorm * cw).coerceAtLeast(2.2f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                } else if (activeTool == EditorTool.HIGHLIGHTER) {
                    drawPath(
                        path = p,
                        color = Color(highlighterColorArgb).copy(alpha = 0.40f),
                        style = Stroke(width = (highlighterThicknessNorm * cw).coerceAtLeast(10f), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            // Live Square Erase / Shape Drag Preview
            val s = dragStartNorm
            val e = dragCurrentNorm
            if (s != null && e != null) {
                if (activeTool == EditorTool.SQUARE_ERASE) {
                    val lx = min(s.x, e.x) * cw
                    val ly = min(s.y, e.y) * ch
                    val rw = abs(e.x - s.x) * cw
                    val rh = abs(e.y - s.y) * ch
                    drawRect(color = Color(eraseColorArgb), topLeft = Offset(lx, ly), size = Size(rw, rh))
                    drawRect(color = Color(0xFFE53935), topLeft = Offset(lx, ly), size = Size(rw, rh), style = Stroke(width = 2f))
                } else if (activeTool == EditorTool.SHAPE) {
                    drawShapePreview(
                        shapeType = activeShapeType,
                        x1 = s.x * cw,
                        y1 = s.y * ch,
                        x2 = e.x * cw,
                        y2 = e.y * ch,
                        color = Color(activeColorArgb),
                        strokePx = (strokeThicknessNorm * cw).coerceAtLeast(2.5f),
                        filled = shapeFilled
                    )
                }
            }
        }

        // Smooth 60fps Active Transform Box Overlay on this page
        if (activeDraftOnPage != null) {
            ActiveTransformOverlay(
                draft = activeDraftOnPage,
                pagePixelSize = pagePixelSize,
                stampBitmaps = stampBitmaps,
                onApply = onApplyActiveDraft,
                onDelete = onDeleteActiveDraft
            )
        }
    }
}

private fun hitTestCommittedItem(
    items: List<PageEditItem>,
    nx: Float,
    ny: Float
): PageEditItem? {
    return items.asReversed().firstOrNull { item ->
        when (item) {
            is PageEditItem.ImageAddition -> {
                nx in (item.normX - 0.02f)..(item.normX + item.normWidth + 0.02f) &&
                    ny in (item.normY - 0.02f)..(item.normY + item.normHeight + 0.02f)
            }
            is PageEditItem.SignatureAddition -> {
                nx in (item.normX - 0.02f)..(item.normX + item.normWidth + 0.02f) &&
                    ny in (item.normY - 0.02f)..(item.normY + item.normHeight + 0.02f)
            }
            is PageEditItem.TextAddition -> {
                val estW = (item.text.length.coerceAtLeast(4) * item.fontSizePt * 0.58f / 595f).coerceIn(0.12f, 0.80f)
                val estH = ((item.fontSizePt * 1.8f) / 842f).coerceAtLeast(0.045f)
                nx in (item.normX - 0.02f)..(item.normX + estW + 0.02f) &&
                    ny in (item.normY - 0.02f)..(item.normY + estH + 0.02f)
            }
            is PageEditItem.SquareErase -> {
                nx in min(item.normLeft, item.normRight)..max(item.normLeft, item.normRight) &&
                    ny in min(item.normTop, item.normBottom)..max(item.normTop, item.normBottom)
            }
            is PageEditItem.ShapeItem -> {
                nx in (min(item.startX, item.endX) - 0.03f)..(max(item.startX, item.endX) + 0.03f) &&
                    ny in (min(item.startY, item.endY) - 0.03f)..(max(item.startY, item.endY) + 0.03f)
            }
            else -> false
        }
    }
}
