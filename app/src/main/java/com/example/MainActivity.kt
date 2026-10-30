package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainTab
import com.example.ui.MainViewModel
import com.example.ui.PendingProtectedAction
import com.example.ui.SubRoute
import com.example.ui.components.NokshiIcons
import com.example.ui.components.SignatureLibraryContent
import com.example.ui.i18n.tr
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PasswordVaultDialog
import com.example.ui.screens.PdfEditorScreen
import com.example.ui.screens.PdfToImageScreen
import com.example.ui.screens.ThemeCustomizeScreen
import com.example.ui.screens.ToolsAndMergeScreen
import com.example.ui.screens.UnlockPdfPasswordDialog
import com.example.ui.screens.WriteDocumentToPdfDialog
import com.example.ui.theme.NokshiPdfTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialPdfUri = if (intent?.action == Intent.ACTION_VIEW) intent.data else null

        setContent {
            val vm: MainViewModel = viewModel()
            val themeSettings by vm.themeSettings.collectAsStateWithLifecycle()

            LaunchedEffect(initialPdfUri) {
                if (initialPdfUri != null) {
                    vm.importPdfFromUri(initialPdfUri, PendingProtectedAction.OPEN_EDITOR, this@MainActivity)
                }
            }

            NokshiPdfTheme(
                colorTheme = themeSettings.colorTheme,
                themeMode = themeSettings.themeMode,
                customPrimaryArgb = themeSettings.customPrimaryArgb,
                fontStyle = themeSettings.fontStyle,
                language = themeSettings.language
            ) {
                NokshiPdfAppRoot(vm = vm)
            }
        }
    }
}

@Composable
fun NokshiPdfAppRoot(vm: MainViewModel) {
    val activityContext = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val workspacePdfs by vm.workspacePdfs.collectAsStateWithLifecycle()
    val signatures by vm.signatures.collectAsStateWithLifecycle()
    val themeSettings by vm.themeSettings.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showWriteDocDialog by remember { mutableStateOf(false) }

    // Observe Activity pause/resume so returning from Google Lens during Read Aloud opens Edge reader!
    DisposableEffect(lifecycleOwner, activityContext) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    vm.notifyAppPausedForExternalActivity()
                }
                Lifecycle.Event.ON_RESUME -> {
                    vm.notifyAppResumed(activityContext)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            vm.consumeMessage()
        }
    }

    // SAF & Photo Picker Launchers
    val openPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.importPdfFromUri(uri, PendingProtectedAction.OPEN_EDITOR, activityContext) }

    val openPdfForLensLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.importPdfFromUri(uri, PendingProtectedAction.OPEN_IN_LENS, activityContext) }

    val openPdfForReadAloudLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.importPdfFromUri(uri, PendingProtectedAction.READ_ALOUD, activityContext) }

    val openPdfForImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.importPdfFromUri(uri, PendingProtectedAction.EXPORT_IMAGES, activityContext) }

    val openMultiplePdfsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> if (uris.isNotEmpty()) vm.importMultiplePdfsToWorkspace(uris) }

    val openDocConverterLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.convertDocumentUri(uri) }

    val pickImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris -> if (uris.isNotEmpty()) vm.convertImagesToPdf(uris) }

    val openExcelConverterLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.convertExcelUri(uri) }

    val openPptConverterLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.convertPresentationUri(uri) }

    when (uiState.subRoute) {
        SubRoute.EDITOR -> {
            val activePdf = uiState.activePdf
            if (activePdf != null) {
                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { _ ->
                    PdfEditorScreen(
                        pdfEntity = activePdf,
                        unlockedPassword = uiState.activeUnlockedPassword,
                        edits = uiState.pendingEdits,
                        signatures = signatures,
                        isBusy = uiState.isLoading,
                        onBack = { vm.closeSubRoute() },
                        onAddBlankPage = { allEdits -> vm.addBlankPageInEditor(allEdits) },
                        onApplyPageOperations = { specs, allEdits ->
                            vm.applyPageOperationsInEditor(specs, allEdits)
                        },
                        onAddEditItem = { vm.addEditorItem(it) },
                        onUpdateEditItem = { vm.updateEditorItem(it) },
                        onDeleteEditItem = { vm.deleteEditorItem(it) },
                        onUndoLastEdit = { vm.undoLastEditorItem() },
                        onSaveEdits = { allEdits -> vm.saveEditorChanges(allEdits) },
                        onCreateSignature = { lbl, file, col -> vm.saveSignature(lbl, file, col) },
                        onDeleteSignature = { vm.deleteSignature(it) },
                        onRequestPasswordDialog = { vm.requestPasswordVault(activePdf) },
                        onOpenInLens = { pageIdx, allEdits ->
                            vm.openPdfInGoogleLens(activePdf, pageIdx, activityContext, allEdits)
                        },
                        onReadAloud = { pageIdx, allEdits ->
                            vm.startReadAloudForPdf(activePdf, pageIdx, activityContext, allEdits)
                        },
                        onOpenPdfToImage = { vm.openPdfToImages(activePdf) }
                    )
                }
            }
        }

        SubRoute.PDF_TO_IMAGE -> {
            PdfToImageScreen(
                pdfTitle = uiState.imageExportPdfTitle,
                isConverting = uiState.isExportingImages,
                images = uiState.convertedImages,
                onBack = { vm.closeSubRoute() },
                onReconvert = { hq, png -> vm.reconvertCurrentPdfImages(hq, png) },
                onSaveSingleToGallery = { vm.saveSingleImageToGallery(it) },
                onSaveAllToGallery = { vm.saveAllImagesToGallery() }
            )
        }

        SubRoute.NONE -> {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavigationRail = maxWidth >= 600.dp

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (!useNavigationRail) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = uiState.activeTab == MainTab.HOME,
                                    onClick = { vm.selectTab(MainTab.HOME) },
                                    icon = {
                                        Icon(
                                            if (uiState.activeTab == MainTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                            contentDescription = tr("Home", "হোম")
                                        )
                                    },
                                    label = { Text(tr("Home", "হোম")) },
                                    modifier = Modifier.testTag("nav_tab_home")
                                )
                                NavigationBarItem(
                                    selected = uiState.activeTab == MainTab.TOOLS,
                                    onClick = { vm.selectTab(MainTab.TOOLS) },
                                    icon = {
                                        Icon(
                                            if (uiState.activeTab == MainTab.TOOLS) Icons.Filled.Build else Icons.Outlined.Build,
                                            contentDescription = tr("Tools", "টুলস")
                                        )
                                    },
                                    label = { Text(tr("Tools", "টুলস")) },
                                    modifier = Modifier.testTag("nav_tab_tools")
                                )
                                NavigationBarItem(
                                    selected = uiState.activeTab == MainTab.SIGNATURES,
                                    onClick = { vm.selectTab(MainTab.SIGNATURES) },
                                    icon = {
                                        Icon(
                                            NokshiIcons.Draw,
                                            contentDescription = tr("Signatures", "সিগনেচার")
                                        )
                                    },
                                    label = { Text(tr("Signatures", "সিগনেচার")) },
                                    modifier = Modifier.testTag("nav_tab_signatures")
                                )
                                NavigationBarItem(
                                    selected = uiState.activeTab == MainTab.CUSTOMIZE,
                                    onClick = { vm.selectTab(MainTab.CUSTOMIZE) },
                                    icon = {
                                        Icon(
                                            NokshiIcons.Palette,
                                            contentDescription = tr("Settings", "সেটিংস")
                                        )
                                    },
                                    label = { Text(tr("Settings", "সেটিংস")) },
                                    modifier = Modifier.testTag("nav_tab_customize")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useNavigationRail) {
                            NavigationRail(modifier = Modifier.fillMaxHeight()) {
                                NavigationRailItem(
                                    selected = uiState.activeTab == MainTab.HOME,
                                    onClick = { vm.selectTab(MainTab.HOME) },
                                    icon = { Icon(Icons.Filled.Home, contentDescription = tr("Home", "হোম")) },
                                    label = { Text(tr("Home", "হোম")) }
                                )
                                NavigationRailItem(
                                    selected = uiState.activeTab == MainTab.TOOLS,
                                    onClick = { vm.selectTab(MainTab.TOOLS) },
                                    icon = { Icon(Icons.Filled.Build, contentDescription = tr("Tools", "টুলস")) },
                                    label = { Text(tr("Tools", "টুলস")) }
                                )
                                NavigationRailItem(
                                    selected = uiState.activeTab == MainTab.SIGNATURES,
                                    onClick = { vm.selectTab(MainTab.SIGNATURES) },
                                    icon = { Icon(NokshiIcons.Draw, contentDescription = tr("Signatures", "সিগনেচার")) },
                                    label = { Text(tr("Signatures", "সিগনেচার")) }
                                )
                                NavigationRailItem(
                                    selected = uiState.activeTab == MainTab.CUSTOMIZE,
                                    onClick = { vm.selectTab(MainTab.CUSTOMIZE) },
                                    icon = { Icon(NokshiIcons.Palette, contentDescription = tr("Settings", "সেটিংস")) },
                                    label = { Text(tr("Settings", "সেটিংস")) }
                                )
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            when (uiState.activeTab) {
                                MainTab.HOME -> HomeScreen(
                                    workspacePdfs = workspacePdfs,
                                    onOpenExistingPdfPicker = {
                                        openPdfLauncher.launch(arrayOf("application/pdf"))
                                    },
                                    onConvertDocumentPicker = {
                                        openDocConverterLauncher.launch(
                                            arrayOf(
                                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                                "application/msword",
                                                "text/plain",
                                                "text/html",
                                                "application/rtf",
                                                "text/*"
                                            )
                                        )
                                    },
                                    onConvertImagePicker = {
                                        pickImagesLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onConvertExcelPicker = {
                                        openExcelConverterLauncher.launch(
                                            arrayOf(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                "application/vnd.ms-excel",
                                                "text/csv",
                                                "text/comma-separated-values"
                                            )
                                        )
                                    },
                                    onConvertPptPicker = {
                                        openPptConverterLauncher.launch(
                                            arrayOf(
                                                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                                "application/vnd.ms-powerpoint"
                                            )
                                        )
                                    },
                                    onOpenWriteDocDialog = { showWriteDocDialog = true },
                                    onCreateSamplePdf = { vm.createStarterSamplePdf() },
                                    onNavigateToMergeTab = { vm.selectTab(MainTab.TOOLS) },
                                    onOpenPdfInEditor = { vm.openPdfInEditor(it) },
                                    onOpenInLens = { vm.openPdfInGoogleLens(it, 0, activityContext) },
                                    onReadAloud = { vm.startReadAloudForPdf(it, 0, activityContext) },
                                    onConvertPdfToImages = { vm.openPdfToImages(it) },
                                    onManagePassword = { vm.requestPasswordVault(it) },
                                    onDeletePdf = { vm.deleteWorkspacePdf(it) }
                                )

                                MainTab.TOOLS -> ToolsAndMergeScreen(
                                    workspacePdfs = workspacePdfs,
                                    onPickDocumentToPdf = {
                                        openDocConverterLauncher.launch(
                                            arrayOf(
                                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                                "application/msword",
                                                "text/plain",
                                                "text/html",
                                                "application/rtf",
                                                "text/*"
                                            )
                                        )
                                    },
                                    onPickImagesToPdf = {
                                        pickImagesLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onPickExcelToPdf = {
                                        openExcelConverterLauncher.launch(
                                            arrayOf(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                "application/vnd.ms-excel",
                                                "text/csv"
                                            )
                                        )
                                    },
                                    onPickPptToPdf = {
                                        openPptConverterLauncher.launch(
                                            arrayOf(
                                                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                                                "application/vnd.ms-powerpoint"
                                            )
                                        )
                                    },
                                    onImportMultiplePdfsForMerge = {
                                        openMultiplePdfsLauncher.launch(arrayOf("application/pdf"))
                                    },
                                    onMergeSelectedPdfs = { list, name ->
                                        vm.mergeSelectedWorkspacePdfs(list, name)
                                    },
                                    onPickPdfForLens = {
                                        openPdfForLensLauncher.launch(arrayOf("application/pdf"))
                                    },
                                    onPickPdfForReadAloud = {
                                        openPdfForReadAloudLauncher.launch(arrayOf("application/pdf"))
                                    },
                                    onPickPdfForImages = {
                                        openPdfForImagesLauncher.launch(arrayOf("application/pdf"))
                                    },
                                    onPickPdfForPassword = {
                                        val first = workspacePdfs.firstOrNull()
                                        if (first != null) vm.requestPasswordVault(first)
                                        else openPdfLauncher.launch(arrayOf("application/pdf"))
                                    }
                                )

                                MainTab.SIGNATURES -> SignatureLibraryContent(
                                    signatures = signatures,
                                    onCreateSignature = { lbl, file, col -> vm.saveSignature(lbl, file, col) },
                                    onDeleteSignature = { vm.deleteSignature(it) },
                                    modifier = Modifier.fillMaxSize()
                                )

                                MainTab.CUSTOMIZE -> ThemeCustomizeScreen(
                                    settings = themeSettings,
                                    onSelectLanguage = { vm.setLanguage(it) },
                                    onSelectColorTheme = { vm.setColorTheme(it) },
                                    onSelectCustomColor = { vm.setCustomThemeColor(it) },
                                    onSelectThemeMode = { vm.setThemeMode(it) },
                                    onSelectFontStyle = { vm.setFontStyle(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Shared Modal Dialogs
    uiState.passwordVaultTarget?.let { target ->
        PasswordVaultDialog(
            pdfEntity = target,
            onDismiss = { vm.dismissPasswordVault() },
            onSetPassword = { oldPwd, newPwd -> vm.applyPasswordProtection(target, oldPwd, newPwd) },
            onRemovePassword = { oldPwd -> vm.removePasswordFromPdf(target, oldPwd) }
        )
    }

    uiState.unlockPromptTarget?.let { lockedPdf ->
        UnlockPdfPasswordDialog(
            pdfTitle = lockedPdf.title,
            onDismiss = { vm.dismissUnlockPrompt() },
            onSubmitPassword = { vm.submitUnlockPassword(it, activityContext) }
        )
    }

    if (showWriteDocDialog) {
        WriteDocumentToPdfDialog(
            onDismiss = { showWriteDocDialog = false },
            onCreatePdf = { title, content ->
                showWriteDocDialog = false
                vm.createPdfFromText(title, content)
            }
        )
    }

    // Requirement চ: Prominent Splash Message "টেক্সট কপি করুন" when Read Aloud opens Google Lens
    if (uiState.showReadAloudSplash) {
        Dialog(onDismissRequest = {}) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F6E56)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp)
                ) {
                    Icon(
                        imageVector = NokshiIcons.VolumeUp,
                        contentDescription = null,
                        tint = Color(0xFFFFCA28),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "টেক্সট কপি করুন",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = tr(
                            "Copy text in Google Lens, then press Back to open Read Aloud in Edge",
                            "Google Lens থেকে টেক্সট কপি করে ব্যাক বাটনে চাপুন — অটোমেটিক Edge ব্রাউজারে ওপেন হবে"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    CircularProgressIndicator(color = Color(0xFFFFCA28), modifier = Modifier.size(28.dp))
                }
            }
        }
    }

    if (uiState.isLoading && uiState.loadingMessage.isNotBlank()) {
        Dialog(onDismissRequest = {}) {
            Card(shape = RoundedCornerShape(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(22.dp)
                ) {
                    CircularProgressIndicator()
                    Column {
                        Text(tr("Please wait...", "অনুগ্রহ করে অপেক্ষা করুন"), style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = uiState.loadingMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}
