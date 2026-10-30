package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NokshiDatabase
import com.example.data.NokshiRepository
import com.example.data.SignatureEntity
import com.example.data.ThemePreferencesRepository
import com.example.data.UserThemeSettings
import com.example.data.WorkspacePdfEntity
import com.example.engine.ConvertedPageImage
import com.example.engine.DetectedPdfTextLine
import com.example.engine.FileToPdfConverter
import com.example.engine.OcrLensEngine
import com.example.engine.OcrPageResult
import com.example.engine.OcrStepProgress
import com.example.engine.PageEditItem
import com.example.engine.PasswordRequiredPdfException
import com.example.engine.PdfEditAndSecurityEngine
import com.example.ui.theme.AppColorTheme
import com.example.ui.theme.AppFontStyle
import com.example.ui.theme.ThemeMode
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainTab {
    HOME,
    TOOLS,
    SIGNATURES,
    CUSTOMIZE
}

enum class SubRoute {
    NONE,
    EDITOR,
    OCR_WINDOW,
    PDF_TO_IMAGE
}

enum class PendingProtectedAction {
    OPEN_EDITOR,
    RUN_OCR,
    EXPORT_IMAGES
}

data class NokshiUiState(
    val activeTab: MainTab = MainTab.HOME,
    val subRoute: SubRoute = SubRoute.NONE,
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val snackbarMessage: String? = null,

    // Active Editor State
    val activePdf: WorkspacePdfEntity? = null,
    val currentPageIndex: Int = 0,
    val totalPages: Int = 1,
    val currentPageBitmap: Bitmap? = null,
    val detectedTextLines: List<DetectedPdfTextLine> = emptyList(),
    val pendingEdits: List<PageEditItem> = emptyList(),

    // OCR Window State
    val ocrPdfTitle: String = "",
    val isOcrRunning: Boolean = false,
    val ocrProgress: OcrStepProgress? = null,
    val ocrResults: List<OcrPageResult> = emptyList(),

    // PDF-to-Image State
    val imageExportPdfTitle: String = "",
    val isExportingImages: Boolean = false,
    val convertedImages: List<ConvertedPageImage> = emptyList(),

    // Password Dialog States
    val passwordVaultTarget: WorkspacePdfEntity? = null,
    val unlockPromptTarget: WorkspacePdfEntity? = null,
    val pendingProtectedAction: PendingProtectedAction? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context get() = getApplication<Application>().applicationContext
    private val db = NokshiDatabase.getInstance(context)
    private val repository = NokshiRepository(db.workspacePdfDao(), db.signatureDao())
    private val themePrefs = ThemePreferencesRepository(context)

    // Session cache of unlocked PDF passwords keyed by filePath
    private val unlockedPasswords = mutableMapOf<String, String>()

    val workspacePdfs: StateFlow<List<WorkspacePdfEntity>> = repository.workspacePdfs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val signatures: StateFlow<List<SignatureEntity>> = repository.signatures
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val themeSettings: StateFlow<UserThemeSettings> = themePrefs.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserThemeSettings())

    private val _uiState = MutableStateFlow(NokshiUiState())
    val uiState: StateFlow<NokshiUiState> = _uiState.asStateFlow()

    init {
        PdfEditAndSecurityEngine.ensureInitialized(context)
    }

    fun selectTab(tab: MainTab) {
        _uiState.update { it.copy(activeTab = tab, subRoute = SubRoute.NONE) }
    }

    fun closeSubRoute() {
        _uiState.update { it.copy(subRoute = SubRoute.NONE) }
    }

    fun showMessage(msg: String) {
        _uiState.update { it.copy(snackbarMessage = msg) }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    // --- Theme & Font Customization ---
    fun setColorTheme(theme: AppColorTheme) {
        viewModelScope.launch { themePrefs.updateColorTheme(theme) }
    }

    fun setCustomThemeColor(argb: Int) {
        viewModelScope.launch { themePrefs.updateCustomColor(argb) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themePrefs.updateThemeMode(mode) }
    }

    fun setFontStyle(style: AppFontStyle) {
        viewModelScope.launch { themePrefs.updateFontStyle(style) }
    }

    // --- Signatures ---
    fun saveSignature(label: String, file: File, strokeColorArgb: Int) {
        viewModelScope.launch {
            repository.saveSignature(label, file, strokeColorArgb)
            showMessage("স্বাক্ষরটি লাইব্রেরীতে সংরক্ষিত হয়েছে!")
        }
    }

    fun deleteSignature(entity: SignatureEntity) {
        viewModelScope.launch {
            repository.deleteSignature(entity)
            showMessage("স্বাক্ষর মুছে ফেলা হয়েছে।")
        }
    }

    // --- File Converters & Openers ---
    fun importPdfFromUri(uri: Uri, afterAction: PendingProtectedAction = PendingProtectedAction.OPEN_EDITOR) {
        viewModelScope.launch {
            setBusy(true, "PDF ওপেন করা হচ্ছে...")
            runCatching {
                val out = FileToPdfConverter.importExistingPdf(context, uri)
                val inspect = PdfEditAndSecurityEngine.inspectPdf(context, out.file)
                val saved = repository.upsertWorkspacePdf(
                    title = out.title,
                    file = out.file,
                    pageCount = inspect.pageCount,
                    sourceCategory = "PDF",
                    isPasswordProtected = inspect.isEncrypted
                )
                setBusy(false)
                when (afterAction) {
                    PendingProtectedAction.OPEN_EDITOR -> openPdfInEditor(saved)
                    PendingProtectedAction.RUN_OCR -> startOcrForPdf(saved)
                    PendingProtectedAction.EXPORT_IMAGES -> openPdfToImages(saved)
                }
            }.onFailure { err ->
                setBusy(false)
                showMessage("সমস্যা হয়েছে: ${err.localizedMessage ?: "PDF পড়া যায়নি"}")
            }
        }
    }

    fun importMultiplePdfsToWorkspace(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            setBusy(true, "${uris.size}টি PDF ওয়ার্কস্পেসে আনা হচ্ছে...")
            var imported = 0
            uris.forEach { uri ->
                runCatching {
                    val out = FileToPdfConverter.importExistingPdf(context, uri)
                    val inspect = PdfEditAndSecurityEngine.inspectPdf(context, out.file)
                    repository.upsertWorkspacePdf(
                        title = out.title,
                        file = out.file,
                        pageCount = inspect.pageCount,
                        sourceCategory = "PDF",
                        isPasswordProtected = inspect.isEncrypted
                    )
                    imported++
                }
            }
            setBusy(false)
            showMessage("$imported টি PDF মার্জ স্টুডিওতে যুক্ত হয়েছে!")
        }
    }

    fun convertDocumentUri(uri: Uri) {
        viewModelScope.launch {
            setBusy(true, "ডকুমেন্ট ফাইল PDF-এ কনভার্ট হচ্ছে...")
            runCatching {
                val out = FileToPdfConverter.convertDocumentToPdf(context, uri)
                val inspect = PdfEditAndSecurityEngine.inspectPdf(context, out.file)
                val entity = repository.upsertWorkspacePdf(
                    title = out.title,
                    file = out.file,
                    pageCount = out.pageCount,
                    sourceCategory = out.category,
                    isPasswordProtected = inspect.isEncrypted
                )
                setBusy(false)
                showMessage("ডকুমেন্ট সফলভাবে PDF-এ রূপান্তরিত হয়েছে!")
                openPdfInEditor(entity)
            }.onFailure { err ->
                setBusy(false)
                showMessage("ডকুমেন্ট কনভার্ট ব্যর্থ: ${err.localizedMessage}")
            }
        }
    }

    fun convertImagesToPdf(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            setBusy(true, "${uris.size}টি ইমেজ থেকে PDF তৈরি হচ্ছে...")
            runCatching {
                val out = FileToPdfConverter.convertImagesToPdf(context, uris)
                val entity = repository.upsertWorkspacePdf(
                    title = out.title,
                    file = out.file,
                    pageCount = out.pageCount,
                    sourceCategory = "IMAGE"
                )
                setBusy(false)
                showMessage("ইমেজ থেকে ${out.pageCount} পেজের PDF তৈরি হয়েছে!")
                openPdfInEditor(entity)
            }.onFailure { err ->
                setBusy(false)
                showMessage("ইমেজ কনভার্ট ব্যর্থ: ${err.localizedMessage}")
            }
        }
    }

    fun convertExcelUri(uri: Uri) {
        viewModelScope.launch {
            setBusy(true, "এক্সেল শীট PDF-এ কনভার্ট হচ্ছে...")
            runCatching {
                val out = FileToPdfConverter.convertExcelToPdf(context, uri)
                val entity = repository.upsertWorkspacePdf(
                    title = out.title,
                    file = out.file,
                    pageCount = out.pageCount,
                    sourceCategory = "EXCEL"
                )
                setBusy(false)
                showMessage("এক্সেল ফাইল সফলভাবে PDF-এ রূপান্তরিত হয়েছে!")
                openPdfInEditor(entity)
            }.onFailure { err ->
                setBusy(false)
                showMessage("এক্সেল কনভার্ট ব্যর্থ: ${err.localizedMessage}")
            }
        }
    }

    fun convertPresentationUri(uri: Uri) {
        viewModelScope.launch {
            setBusy(true, "প্রেজেন্টেশন স্লাইড PDF-এ কনভার্ট হচ্ছে...")
            runCatching {
                val out = FileToPdfConverter.convertPresentationToPdf(context, uri)
                val entity = repository.upsertWorkspacePdf(
                    title = out.title,
                    file = out.file,
                    pageCount = out.pageCount,
                    sourceCategory = "POWERPOINT"
                )
                setBusy(false)
                showMessage("পাওয়ারপয়েন্ট প্রেজেন্টেশন PDF-এ রূপান্তরিত হয়েছে!")
                openPdfInEditor(entity)
            }.onFailure { err ->
                setBusy(false)
                showMessage("প্রেজেন্টেশন কনভার্ট ব্যর্থ: ${err.localizedMessage}")
            }
        }
    }

    fun createPdfFromText(title: String, body: String) {
        viewModelScope.launch {
            setBusy(true, "নতুন ডকুমেন্ট PDF তৈরি হচ্ছে...")
            runCatching {
                val out = FileToPdfConverter.createPdfFromWrittenText(context, title, body)
                val entity = repository.upsertWorkspacePdf(
                    title = out.title,
                    file = out.file,
                    pageCount = out.pageCount,
                    sourceCategory = "DOCUMENT"
                )
                setBusy(false)
                showMessage("নতুন PDF তৈরি সম্পন্ন!")
                openPdfInEditor(entity)
            }.onFailure { err ->
                setBusy(false)
                showMessage("ত্রুটি: ${err.localizedMessage}")
            }
        }
    }

    fun createStarterSamplePdf() {
        viewModelScope.launch {
            setBusy(true, "নমুনা নকশি PDF তৈরি হচ্ছে...")
            runCatching {
                val out = FileToPdfConverter.createSampleNokshiPdf(context)
                val entity = repository.upsertWorkspacePdf(
                    title = out.title,
                    file = out.file,
                    pageCount = out.pageCount,
                    sourceCategory = "SAMPLE"
                )
                setBusy(false)
                showMessage("২ পেজের নমুনা নকশি PDF প্রস্তুত!")
                openPdfInEditor(entity)
            }.onFailure { err ->
                setBusy(false)
                showMessage("নমুনা তৈরি ব্যর্থ: ${err.localizedMessage}")
            }
        }
    }

    fun deleteWorkspacePdf(entity: WorkspacePdfEntity) {
        viewModelScope.launch {
            repository.deleteWorkspacePdf(entity)
            showMessage("PDF ফাইলটি মুছে ফেলা হয়েছে।")
        }
    }

    // --- Merge PDFs ---
    fun mergeSelectedWorkspacePdfs(entities: List<WorkspacePdfEntity>, mergedTitle: String) {
        viewModelScope.launch {
            setBusy(true, "${entities.size}টি PDF একত্রে মার্জ করা হচ্ছে...")
            runCatching {
                val files = entities.map { File(it.filePath) }
                val out = PdfEditAndSecurityEngine.mergePdfs(
                    context = context,
                    filesToMerge = files,
                    outputTitle = mergedTitle,
                    passwords = unlockedPasswords
                )
                val mergedEntity = repository.upsertWorkspacePdf(
                    title = out.title,
                    file = out.file,
                    pageCount = out.pageCount,
                    sourceCategory = "MERGED"
                )
                setBusy(false)
                showMessage("সফলভাবে মার্জ হয়ে ${out.pageCount} পেজের PDF তৈরি হয়েছে!")
                openPdfInEditor(mergedEntity)
            }.onFailure { err ->
                setBusy(false)
                showMessage("মার্জ ব্যর্থ (সুরক্ষিত ফাইল হলে আগে আনলক করুন): ${err.localizedMessage}")
            }
        }
    }

    // --- Password Vault & Unlocking ---
    fun requestPasswordVault(entity: WorkspacePdfEntity) {
        _uiState.update { it.copy(passwordVaultTarget = entity) }
    }

    fun dismissPasswordVault() {
        _uiState.update { it.copy(passwordVaultTarget = null) }
    }

    fun applyPasswordProtection(entity: WorkspacePdfEntity, currentPwd: String, newPwd: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(passwordVaultTarget = null) }
            setBusy(true, "PDF ফাইলে পাসওয়ার্ড এনক্রিপশন যুক্ত হচ্ছে...")
            runCatching {
                val srcFile = File(entity.filePath)
                val effectiveOldPwd = currentPwd.ifBlank { unlockedPasswords[entity.filePath] ?: "" }
                val lockedFile = PdfEditAndSecurityEngine.protectPdfWithPassword(
                    context = context,
                    sourceFile = srcFile,
                    currentPassword = effectiveOldPwd,
                    newPassword = newPwd
                )
                unlockedPasswords[lockedFile.absolutePath] = newPwd
                val lockedEntity = repository.upsertWorkspacePdf(
                    title = "${entity.title} (Locked)",
                    file = lockedFile,
                    pageCount = entity.pageCount,
                    sourceCategory = entity.sourceCategory,
                    isPasswordProtected = true
                )
                if (_uiState.value.activePdf?.id == entity.id) {
                    _uiState.update { it.copy(activePdf = lockedEntity) }
                }
                setBusy(false)
                showMessage("পাসওয়ার্ড দিয়ে সুরক্ষিত নতুন PDF সংরক্ষিত হয়েছে!")
            }.onFailure { err ->
                setBusy(false)
                showMessage("পাসওয়ার্ড সেট ব্যর্থ: ${err.localizedMessage}")
            }
        }
    }

    fun removePasswordFromPdf(entity: WorkspacePdfEntity, currentPwd: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(passwordVaultTarget = null) }
            setBusy(true, "পাসওয়ার্ড সরানো হচ্ছে...")
            runCatching {
                val unlockedFile = PdfEditAndSecurityEngine.removePdfPassword(
                    context = context,
                    sourceFile = File(entity.filePath),
                    currentPassword = currentPwd
                )
                repository.upsertWorkspacePdf(
                    title = "${entity.title.removeSuffix(" (Locked)")} (Unlocked)",
                    file = unlockedFile,
                    pageCount = entity.pageCount,
                    sourceCategory = entity.sourceCategory,
                    isPasswordProtected = false
                )
                setBusy(false)
                showMessage("পাসওয়ার্ড মুক্ত আনলক PDF তৈরি হয়েছে!")
            }.onFailure {
                setBusy(false)
                showMessage("সঠিক পাসওয়ার্ড দিন।")
            }
        }
    }

    fun dismissUnlockPrompt() {
        _uiState.update { it.copy(unlockPromptTarget = null, pendingProtectedAction = null) }
    }

    fun submitUnlockPassword(password: String) {
        val target = _uiState.value.unlockPromptTarget ?: return
        val action = _uiState.value.pendingProtectedAction ?: PendingProtectedAction.OPEN_EDITOR
        viewModelScope.launch {
            val inspect = PdfEditAndSecurityEngine.inspectPdf(context, File(target.filePath), password)
            if (inspect.requiresPassword) {
                showMessage("ভুল পাসওয়ার্ড! আবার চেষ্টা করুন।")
            } else {
                unlockedPasswords[target.filePath] = password
                _uiState.update { it.copy(unlockPromptTarget = null, pendingProtectedAction = null) }
                when (action) {
                    PendingProtectedAction.OPEN_EDITOR -> openPdfInEditor(target)
                    PendingProtectedAction.RUN_OCR -> startOcrForPdf(target)
                    PendingProtectedAction.EXPORT_IMAGES -> openPdfToImages(target)
                }
            }
        }
    }

    // --- PDF Editor Operations ---
    fun openPdfInEditor(entity: WorkspacePdfEntity) {
        viewModelScope.launch {
            val file = File(entity.filePath)
            if (!file.exists()) {
                showMessage("ফাইলটি পাওয়া যায়নি।")
                return@launch
            }
            val pwd = unlockedPasswords[entity.filePath] ?: ""
            val inspect = PdfEditAndSecurityEngine.inspectPdf(context, file, pwd)
            if (inspect.requiresPassword) {
                _uiState.update {
                    it.copy(
                        unlockPromptTarget = entity,
                        pendingProtectedAction = PendingProtectedAction.OPEN_EDITOR
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    activePdf = entity,
                    currentPageIndex = 0,
                    totalPages = inspect.pageCount.coerceAtLeast(1),
                    pendingEdits = emptyList(),
                    subRoute = SubRoute.EDITOR
                )
            }
            loadEditorPage(entity, 0, pwd)
        }
    }

    fun selectEditorPage(pageIndex: Int) {
        val pdf = _uiState.value.activePdf ?: return
        val pwd = unlockedPasswords[pdf.filePath] ?: ""
        viewModelScope.launch {
            loadEditorPage(pdf, pageIndex, pwd)
        }
    }

    private suspend fun loadEditorPage(entity: WorkspacePdfEntity, pageIndex: Int, pwd: String) {
        setBusy(true)
        runCatching {
            val file = File(entity.filePath)
            val bmp = PdfEditAndSecurityEngine.renderPageBitmap(context, file, pageIndex, pwd, scale = 2.0f)
            val lines = PdfEditAndSecurityEngine.extractPageTextLines(context, file, pageIndex, pwd)
            _uiState.update {
                it.copy(
                    currentPageIndex = pageIndex,
                    currentPageBitmap = bmp,
                    detectedTextLines = lines,
                    isLoading = false
                )
            }
        }.onFailure { err ->
            setBusy(false)
            if (err is PasswordRequiredPdfException) {
                _uiState.update {
                    it.copy(
                        unlockPromptTarget = entity,
                        pendingProtectedAction = PendingProtectedAction.OPEN_EDITOR
                    )
                }
            } else {
                showMessage("পেজ লোড করতে সমস্যা হয়েছে: ${err.localizedMessage}")
            }
        }
    }

    fun addEditorItem(item: PageEditItem) {
        _uiState.update { it.copy(pendingEdits = it.pendingEdits + item) }
    }

    fun updateEditorItem(updated: PageEditItem) {
        _uiState.update { state ->
            state.copy(
                pendingEdits = state.pendingEdits.map { if (it.id == updated.id) updated else it }
            )
        }
    }

    fun deleteEditorItem(id: String) {
        _uiState.update { state ->
            state.copy(pendingEdits = state.pendingEdits.filterNot { it.id == id })
        }
    }

    fun undoLastEditorItem() {
        _uiState.update { state ->
            if (state.pendingEdits.isNotEmpty()) {
                state.copy(pendingEdits = state.pendingEdits.dropLast(1))
            } else state
        }
    }

    fun addBlankPageInEditor() {
        val pdf = _uiState.value.activePdf ?: return
        val pwd = unlockedPasswords[pdf.filePath] ?: ""
        viewModelScope.launch {
            setBusy(true, "নতুন সাদা পেজ যোগ হচ্ছে...")
            runCatching {
                if (_uiState.value.pendingEdits.isNotEmpty()) {
                    PdfEditAndSecurityEngine.saveEditsToPdf(
                        context = context,
                        sourceFile = File(pdf.filePath),
                        edits = _uiState.value.pendingEdits,
                        password = pwd
                    )
                }
                val newTotal = PdfEditAndSecurityEngine.addBlankPageToPdf(context, File(pdf.filePath), pwd)
                val updated = repository.upsertWorkspacePdf(
                    title = pdf.title,
                    file = File(pdf.filePath),
                    pageCount = newTotal,
                    sourceCategory = pdf.sourceCategory,
                    isPasswordProtected = pdf.isPasswordProtected
                )
                _uiState.update {
                    it.copy(
                        activePdf = updated,
                        totalPages = newTotal,
                        pendingEdits = emptyList()
                    )
                }
                loadEditorPage(updated, newTotal - 1, pwd)
                showMessage("নতুন পেজ যুক্ত হয়েছে!")
            }.onFailure { err ->
                setBusy(false)
                showMessage("ত্রুটি: ${err.localizedMessage}")
            }
        }
    }

    fun saveEditorChanges() {
        val state = _uiState.value
        val pdf = state.activePdf ?: return
        if (state.pendingEdits.isEmpty()) {
            showMessage("কোনো নতুন পরিবর্তন নেই—ফাইলটি ইতিমধ্যে আপ-টু-ডেট।")
            return
        }
        val pwd = unlockedPasswords[pdf.filePath] ?: ""
        viewModelScope.launch {
            setBusy(true, "পরিবর্তনগুলো PDF ফাইলে সেভ হচ্ছে...")
            runCatching {
                val savedFile = PdfEditAndSecurityEngine.saveEditsToPdf(
                    context = context,
                    sourceFile = File(pdf.filePath),
                    edits = state.pendingEdits,
                    password = pwd,
                    saveAsNewCopy = false
                )
                val inspect = PdfEditAndSecurityEngine.inspectPdf(context, savedFile, pwd)
                val updatedEntity = repository.upsertWorkspacePdf(
                    title = pdf.title,
                    file = savedFile,
                    pageCount = inspect.pageCount,
                    sourceCategory = pdf.sourceCategory,
                    isPasswordProtected = pdf.isPasswordProtected
                )
                _uiState.update {
                    it.copy(
                        activePdf = updatedEntity,
                        pendingEdits = emptyList()
                    )
                }
                loadEditorPage(updatedEntity, state.currentPageIndex, pwd)
                showMessage("সব এডিট সফলভাবে PDF ফাইলে সেভ হয়েছে!")
            }.onFailure { err ->
                setBusy(false)
                showMessage("সেভ ব্যর্থ: ${err.localizedMessage}")
            }
        }
    }

    // --- Requirement 6: Background PDF-to-Image + Google Lens / ML Kit OCR ---
    fun startOcrForPdf(entity: WorkspacePdfEntity) {
        viewModelScope.launch {
            val file = File(entity.filePath)
            val pwd = unlockedPasswords[entity.filePath] ?: ""
            val inspect = PdfEditAndSecurityEngine.inspectPdf(context, file, pwd)
            if (inspect.requiresPassword) {
                _uiState.update {
                    it.copy(
                        unlockPromptTarget = entity,
                        pendingProtectedAction = PendingProtectedAction.RUN_OCR
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    subRoute = SubRoute.OCR_WINDOW,
                    ocrPdfTitle = entity.title,
                    isOcrRunning = true,
                    ocrProgress = null,
                    ocrResults = emptyList()
                )
            }

            runCatching {
                val results = OcrLensEngine.runBackgroundPdfToImageAndLensOcr(
                    context = context,
                    pdfFile = file,
                    password = pwd,
                    onProgress = { prog ->
                        _uiState.update { it.copy(ocrProgress = prog) }
                    }
                )
                _uiState.update {
                    it.copy(
                        isOcrRunning = false,
                        ocrResults = results
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isOcrRunning = false) }
                showMessage("OCR ব্যর্থ: ${err.localizedMessage}")
            }
        }
    }

    // --- Requirement 5: PDF to Image Converter ---
    fun openPdfToImages(entity: WorkspacePdfEntity, highQuality: Boolean = true, asPng: Boolean = true) {
        viewModelScope.launch {
            val file = File(entity.filePath)
            val pwd = unlockedPasswords[entity.filePath] ?: ""
            val inspect = PdfEditAndSecurityEngine.inspectPdf(context, file, pwd)
            if (inspect.requiresPassword) {
                _uiState.update {
                    it.copy(
                        unlockPromptTarget = entity,
                        pendingProtectedAction = PendingProtectedAction.EXPORT_IMAGES
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    activePdf = entity,
                    subRoute = SubRoute.PDF_TO_IMAGE,
                    imageExportPdfTitle = entity.title,
                    isExportingImages = true,
                    convertedImages = emptyList()
                )
            }

            runCatching {
                val imgs = PdfEditAndSecurityEngine.convertPdfToImages(
                    context = context,
                    file = file,
                    password = pwd,
                    highQuality = highQuality,
                    asPng = asPng
                )
                _uiState.update {
                    it.copy(
                        isExportingImages = false,
                        convertedImages = imgs
                    )
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isExportingImages = false) }
                showMessage("PDF থেকে ইমেজ কনভার্ট ব্যর্থ: ${err.localizedMessage}")
            }
        }
    }

    fun reconvertCurrentPdfImages(highQuality: Boolean, asPng: Boolean) {
        val pdf = _uiState.value.activePdf ?: return
        openPdfToImages(pdf, highQuality, asPng)
    }

    fun saveSingleImageToGallery(img: ConvertedPageImage) {
        viewModelScope.launch {
            val ok = PdfEditAndSecurityEngine.saveImageFileToGallery(context, File(img.filePath))
            showMessage(if (ok) "পেজ ${img.pageNumber} গ্যালারির Pictures/NokshiPDF ফোল্ডারে সেভ হয়েছে!" else "গ্যালারিতে সেভ করা যায়নি।")
        }
    }

    fun saveAllImagesToGallery() {
        val list = _uiState.value.convertedImages
        if (list.isEmpty()) return
        viewModelScope.launch {
            var count = 0
            list.forEach { img ->
                if (PdfEditAndSecurityEngine.saveImageFileToGallery(context, File(img.filePath))) {
                    count++
                }
            }
            showMessage("$count টি ইমেজ গ্যালারির Pictures/NokshiPDF ফোল্ডারে সংরক্ষিত হয়েছে!")
        }
    }

    private fun setBusy(busy: Boolean, msg: String = "") {
        _uiState.update { it.copy(isLoading = busy, loadingMessage = msg) }
    }
}
