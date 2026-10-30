package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NokshiDatabase
import com.example.data.NokshiRepository
import com.example.data.SignatureEntity
import com.example.data.ThemePreferencesRepository
import com.example.data.UserThemeSettings
import com.example.data.WorkspacePdfEntity
import com.example.engine.ConvertedPageImage
import com.example.engine.FileToPdfConverter
import com.example.engine.LensAndReadAloudHelper
import com.example.engine.PageEditItem
import com.example.engine.PageOperationSpec
import com.example.engine.PdfEditAndSecurityEngine
import com.example.engine.StorageExportHelper
import com.example.ui.i18n.AppLanguage
import com.example.ui.theme.AppColorTheme
import com.example.ui.theme.AppFontStyle
import com.example.ui.theme.ThemeMode
import java.io.File
import kotlinx.coroutines.delay
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
    PDF_TO_IMAGE
}

enum class PendingProtectedAction {
    OPEN_EDITOR,
    OPEN_IN_LENS,
    READ_ALOUD,
    EXPORT_IMAGES
}

enum class ReadAloudStage {
    IDLE,
    LAUNCHING_LENS,
    IN_LENS
}

data class NokshiUiState(
    val activeTab: MainTab = MainTab.HOME,
    val subRoute: SubRoute = SubRoute.NONE,
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val snackbarMessage: String? = null,

    // Active Editor State
    val activePdf: WorkspacePdfEntity? = null,
    val activeUnlockedPassword: String = "",
    val pendingEdits: List<PageEditItem> = emptyList(),

    // Read Aloud Splash & Lifecycle State
    val showReadAloudSplash: Boolean = false,
    val readAloudStage: ReadAloudStage = ReadAloudStage.IDLE,

    // PDF-to-Image State
    val imageExportPdfTitle: String = "",
    val isExportingImages: Boolean = false,
    val convertedImages: List<ConvertedPageImage> = emptyList(),

    // Password Dialog States
    val passwordVaultTarget: WorkspacePdfEntity? = null,
    val unlockPromptTarget: WorkspacePdfEntity? = null,
    val pendingProtectedAction: PendingProtectedAction? = null,
    val pendingTargetPageIndex: Int = 0
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

    private fun pickMsg(en: String, bn: String): String {
        return themeSettings.value.language.pick(en, bn)
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

    // --- Language, Theme & Font Customization ---
    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { themePrefs.updateLanguage(language) }
    }

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
            showMessage(pickMsg("Signature saved to library!", "স্বাক্ষরটি লাইব্রেরীতে সংরক্ষিত হয়েছে!"))
        }
    }

    fun deleteSignature(entity: SignatureEntity) {
        viewModelScope.launch {
            repository.deleteSignature(entity)
            showMessage(pickMsg("Signature deleted.", "স্বাক্ষর মুছে ফেলা হয়েছে।"))
        }
    }

    // --- File Converters & Openers ---
    fun importPdfFromUri(
        uri: Uri,
        afterAction: PendingProtectedAction = PendingProtectedAction.OPEN_EDITOR,
        activityContext: Context? = null
    ) {
        viewModelScope.launch {
            setBusy(true, pickMsg("Opening PDF...", "PDF ওপেন করা হচ্ছে..."))
            runCatching {
                // Take persistable read+write permissions when available so edits save directly back to this original file!
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }.onFailure {
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }
                }

                val out = FileToPdfConverter.importExistingPdf(context, uri)
                val inspect = PdfEditAndSecurityEngine.inspectPdf(context, out.file)
                val origDisplayName = FileToPdfConverter.resolveDisplayName(context, uri).ifBlank { "${out.title}.pdf" }
                val saved = repository.upsertWorkspacePdf(
                    title = out.title,
                    file = out.file,
                    pageCount = inspect.pageCount,
                    sourceCategory = "PDF",
                    isPasswordProtected = inspect.isEncrypted,
                    originalUriString = uri.toString(),
                    savedLocationLabel = "Original: $origDisplayName"
                )
                setBusy(false)
                when (afterAction) {
                    PendingProtectedAction.OPEN_EDITOR -> openPdfInEditor(saved)
                    PendingProtectedAction.OPEN_IN_LENS -> openPdfInGoogleLens(saved, 0, activityContext ?: context)
                    PendingProtectedAction.READ_ALOUD -> startReadAloudForPdf(saved, 0, activityContext ?: context)
                    PendingProtectedAction.EXPORT_IMAGES -> openPdfToImages(saved)
                }
            }.onFailure { err ->
                setBusy(false)
                showMessage(
                    pickMsg(
                        "Could not open PDF: ${err.localizedMessage ?: "Unknown error"}",
                        "সমস্যা হয়েছে: ${err.localizedMessage ?: "PDF পড়া যায়নি"}"
                    )
                )
            }
        }
    }

    fun importMultiplePdfsToWorkspace(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            setBusy(true, pickMsg("Importing ${uris.size} PDFs...", "${uris.size}টি PDF ওয়ার্কস্পেসে আনা হচ্ছে..."))
            var imported = 0
            uris.forEach { uri ->
                runCatching {
                    runCatching {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    }
                    val out = FileToPdfConverter.importExistingPdf(context, uri)
                    val inspect = PdfEditAndSecurityEngine.inspectPdf(context, out.file)
                    val origName = FileToPdfConverter.resolveDisplayName(context, uri).ifBlank { "${out.title}.pdf" }
                    repository.upsertWorkspacePdf(
                        title = out.title,
                        file = out.file,
                        pageCount = inspect.pageCount,
                        sourceCategory = "PDF",
                        isPasswordProtected = inspect.isEncrypted,
                        originalUriString = uri.toString(),
                        savedLocationLabel = "Original: $origName"
                    )
                    imported++
                }
            }
            setBusy(false)
            showMessage(
                pickMsg(
                    "$imported PDFs added to Merge Studio!",
                    "$imported টি PDF মার্জ স্টুডিওতে যুক্ত হয়েছে!"
                )
            )
        }
    }

    private suspend fun saveNewGeneratedPdfAndRegister(
        title: String,
        file: File,
        pageCount: Int,
        category: String,
        isPasswordProtected: Boolean = false
    ): WorkspacePdfEntity {
        val syncOutcome = StorageExportHelper.syncPdfToUserStorage(
            context = context,
            localPdfFile = file,
            displayTitle = title,
            originalUriString = null,
            existingDownloadUriString = null
        )
        val entity = repository.upsertWorkspacePdf(
            title = title,
            file = file,
            pageCount = pageCount,
            sourceCategory = category,
            isPasswordProtected = isPasswordProtected,
            originalUriString = null,
            downloadUriString = syncOutcome.downloadUriString,
            savedLocationLabel = syncOutcome.locationBadge
        )
        showMessage(pickMsg(syncOutcome.messageEn, syncOutcome.messageBn))
        return entity
    }

    fun convertDocumentUri(uri: Uri) {
        viewModelScope.launch {
            setBusy(true, pickMsg("Converting Document to PDF...", "ডকুমেন্ট ফাইল PDF-এ কনভার্ট হচ্ছে..."))
            runCatching {
                val out = FileToPdfConverter.convertDocumentToPdf(context, uri)
                val inspect = PdfEditAndSecurityEngine.inspectPdf(context, out.file)
                val entity = saveNewGeneratedPdfAndRegister(
                    title = out.title,
                    file = out.file,
                    pageCount = out.pageCount,
                    category = out.category,
                    isPasswordProtected = inspect.isEncrypted
                )
                setBusy(false)
                openPdfInEditor(entity)
            }.onFailure { err ->
                setBusy(false)
                showMessage(pickMsg("Conversion failed: ${err.localizedMessage}", "ডকুমেন্ট কনভার্ট ব্যর্থ: ${err.localizedMessage}"))
            }
        }
    }

    fun convertImagesToPdf(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            setBusy(true, pickMsg("Converting ${uris.size} images to PDF...", "${uris.size}টি ইমেজ থেকে PDF তৈরি হচ্ছে..."))
            runCatching {
                val out = FileToPdfConverter.convertImagesToPdf(context, uris)
                val entity = saveNewGeneratedPdfAndRegister(
                    title = out.title,
                    file = out.file,
                    pageCount = out.pageCount,
                    category = "IMAGE"
                )
                setBusy(false)
                openPdfInEditor(entity)
            }.onFailure { err ->
                setBusy(false)
                showMessage(pickMsg("Image conversion failed: ${err.localizedMessage}", "ইমেজ কনভার্ট ব্যর্থ: ${err.localizedMessage}"))
            }
        }
    }

    fun convertExcelUri(uri: Uri) {
        viewModelScope.launch {
            setBusy(true, pickMsg("Converting Excel sheet to PDF...", "এক্সেল শীট PDF-এ কনভার্ট হচ্ছে..."))
            runCatching {
                val out = FileToPdfConverter.convertExcelToPdf(context, uri)
                val entity = saveNewGeneratedPdfAndRegister(
                    title = out.title,
                    file = out.file,
                    pageCount = out.pageCount,
                    category = "EXCEL"
                )
                setBusy(false)
                openPdfInEditor(entity)
            }.onFailure { err ->
                setBusy(false)
                showMessage(pickMsg("Excel conversion failed: ${err.localizedMessage}", "এক্সেল কনভার্ট ব্যর্থ: ${err.localizedMessage}"))
            }
        }
    }

    fun convertPresentationUri(uri: Uri) {
        viewModelScope.launch {
            setBusy(true, pickMsg("Converting PowerPoint slides to PDF...", "প্রেজেন্টেশন স্লাইড PDF-এ কনভার্ট হচ্ছে..."))
            runCatching {
                val out = FileToPdfConverter.convertPresentationToPdf(context, uri)
                val entity = saveNewGeneratedPdfAndRegister(
                    title = out.title,
                    file = out.file,
                    pageCount = out.pageCount,
                    category = "POWERPOINT"
                )
                setBusy(false)
                openPdfInEditor(entity)
            }.onFailure { err ->
                setBusy(false)
                showMessage(pickMsg("Presentation conversion failed: ${err.localizedMessage}", "প্রেজেন্টেশন কনভার্ট ব্যর্থ: ${err.localizedMessage}"))
            }
        }
    }

    fun createPdfFromText(title: String, body: String) {
        viewModelScope.launch {
            setBusy(true, pickMsg("Creating new PDF in Download folder...", "নতুন ডকুমেন্ট PDF তৈরি হচ্ছে..."))
            runCatching {
                val out = FileToPdfConverter.createPdfFromWrittenText(context, title, body)
                val entity = saveNewGeneratedPdfAndRegister(
                    title = out.title,
                    file = out.file,
                    pageCount = out.pageCount,
                    category = "DOCUMENT"
                )
                setBusy(false)
                openPdfInEditor(entity)
            }.onFailure { err ->
                setBusy(false)
                showMessage(pickMsg("Error: ${err.localizedMessage}", "ত্রুটি: ${err.localizedMessage}"))
            }
        }
    }

    fun createStarterSamplePdf() {
        viewModelScope.launch {
            setBusy(true, pickMsg("Generating sample Nokshi PDF...", "নমুনা নকশি PDF তৈরি হচ্ছে..."))
            runCatching {
                val out = FileToPdfConverter.createSampleNokshiPdf(context)
                val entity = saveNewGeneratedPdfAndRegister(
                    title = out.title,
                    file = out.file,
                    pageCount = out.pageCount,
                    category = "SAMPLE"
                )
                setBusy(false)
                openPdfInEditor(entity)
            }.onFailure { err ->
                setBusy(false)
                showMessage(pickMsg("Sample creation failed: ${err.localizedMessage}", "নমুনা তৈরি ব্যর্থ: ${err.localizedMessage}"))
            }
        }
    }

    fun deleteWorkspacePdf(entity: WorkspacePdfEntity) {
        viewModelScope.launch {
            repository.deleteWorkspacePdf(entity)
            showMessage(pickMsg("Removed from workspace.", "PDF ফাইলটি তালিকা থেকে সরানো হয়েছে।"))
        }
    }

    // --- Merge PDFs (Saved into Download/ folder) ---
    fun mergeSelectedWorkspacePdfs(entities: List<WorkspacePdfEntity>, mergedTitle: String) {
        viewModelScope.launch {
            setBusy(true, pickMsg("Merging ${entities.size} PDFs...", "${entities.size}টি PDF একত্রে মার্জ করা হচ্ছে..."))
            runCatching {
                val files = entities.map { File(it.filePath) }
                val out = PdfEditAndSecurityEngine.mergePdfs(
                    context = context,
                    filesToMerge = files,
                    outputTitle = mergedTitle,
                    passwords = unlockedPasswords
                )
                val mergedEntity = saveNewGeneratedPdfAndRegister(
                    title = out.title,
                    file = out.file,
                    pageCount = out.pageCount,
                    category = "MERGED"
                )
                setBusy(false)
                openPdfInEditor(mergedEntity)
            }.onFailure { err ->
                setBusy(false)
                showMessage(
                    pickMsg(
                        "Merge failed (unlock protected PDFs first): ${err.localizedMessage}",
                        "মার্জ ব্যর্থ (সুরক্ষিত ফাইল হলে আগে আনলক করুন): ${err.localizedMessage}"
                    )
                )
            }
        }
    }

    // --- Password Vault & Unlocking (Saved into Download/ folder) ---
    fun requestPasswordVault(entity: WorkspacePdfEntity) {
        _uiState.update { it.copy(passwordVaultTarget = entity) }
    }

    fun dismissPasswordVault() {
        _uiState.update { it.copy(passwordVaultTarget = null) }
    }

    fun applyPasswordProtection(entity: WorkspacePdfEntity, currentPwd: String, newPwd: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(passwordVaultTarget = null) }
            setBusy(true, pickMsg("Encrypting PDF & saving to Download/...", "PDF ফাইলে পাসওয়ার্ড যুক্ত হচ্ছে..."))
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
                val lockedTitle = "${entity.title}_Locked"
                val lockedEntity = saveNewGeneratedPdfAndRegister(
                    title = lockedTitle,
                    file = lockedFile,
                    pageCount = entity.pageCount,
                    category = entity.sourceCategory,
                    isPasswordProtected = true
                )
                if (_uiState.value.activePdf?.id == entity.id) {
                    _uiState.update {
                        it.copy(
                            activePdf = lockedEntity,
                            activeUnlockedPassword = newPwd
                        )
                    }
                }
                setBusy(false)
            }.onFailure { err ->
                setBusy(false)
                showMessage(pickMsg("Password setup failed: ${err.localizedMessage}", "পাসওয়ার্ড সেট ব্যর্থ: ${err.localizedMessage}"))
            }
        }
    }

    fun removePasswordFromPdf(entity: WorkspacePdfEntity, currentPwd: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(passwordVaultTarget = null) }
            setBusy(true, pickMsg("Removing password & saving to Download/...", "পাসওয়ার্ড সরানো হচ্ছে..."))
            runCatching {
                val unlockedFile = PdfEditAndSecurityEngine.removePdfPassword(
                    context = context,
                    sourceFile = File(entity.filePath),
                    currentPassword = currentPwd
                )
                val cleanTitle = "${entity.title.removeSuffix("_Locked").removeSuffix(" (Locked)")}_Unlocked"
                saveNewGeneratedPdfAndRegister(
                    title = cleanTitle,
                    file = unlockedFile,
                    pageCount = entity.pageCount,
                    category = entity.sourceCategory,
                    isPasswordProtected = false
                )
                setBusy(false)
            }.onFailure {
                setBusy(false)
                showMessage(pickMsg("Incorrect password.", "সঠিক পাসওয়ার্ড দিন।"))
            }
        }
    }

    fun dismissUnlockPrompt() {
        _uiState.update { it.copy(unlockPromptTarget = null, pendingProtectedAction = null) }
    }

    fun submitUnlockPassword(password: String, activityContext: Context? = null) {
        val target = _uiState.value.unlockPromptTarget ?: return
        val action = _uiState.value.pendingProtectedAction ?: PendingProtectedAction.OPEN_EDITOR
        val pageIdx = _uiState.value.pendingTargetPageIndex
        viewModelScope.launch {
            val inspect = PdfEditAndSecurityEngine.inspectPdf(context, File(target.filePath), password)
            if (inspect.requiresPassword) {
                showMessage(pickMsg("Wrong password! Please try again.", "ভুল পাসওয়ার্ড! আবার চেষ্টা করুন।"))
            } else {
                unlockedPasswords[target.filePath] = password
                _uiState.update { it.copy(unlockPromptTarget = null, pendingProtectedAction = null) }
                when (action) {
                    PendingProtectedAction.OPEN_EDITOR -> openPdfInEditor(target)
                    PendingProtectedAction.OPEN_IN_LENS -> openPdfInGoogleLens(target, pageIdx, activityContext ?: context)
                    PendingProtectedAction.READ_ALOUD -> startReadAloudForPdf(target, pageIdx, activityContext ?: context)
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
                showMessage(pickMsg("File not found.", "ফাইলটি পাওয়া যায়নি।"))
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
                    activePdf = entity.copy(pageCount = inspect.pageCount.coerceAtLeast(1)),
                    activeUnlockedPassword = pwd,
                    pendingEdits = emptyList(),
                    subRoute = SubRoute.EDITOR
                )
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

    fun addBlankPageInEditor(allCurrentEdits: List<PageEditItem>) {
        val pdf = _uiState.value.activePdf ?: return
        val pwd = unlockedPasswords[pdf.filePath] ?: ""
        viewModelScope.launch {
            setBusy(true, pickMsg("Adding new blank page...", "নতুন সাদা পেজ যোগ হচ্ছে..."))
            runCatching {
                val localFile = File(pdf.filePath)
                if (allCurrentEdits.isNotEmpty()) {
                    PdfEditAndSecurityEngine.saveEditsToPdf(
                        context = context,
                        sourceFile = localFile,
                        edits = allCurrentEdits,
                        password = pwd
                    )
                }
                val newTotal = PdfEditAndSecurityEngine.addBlankPageToPdf(context, localFile, pwd)
                val syncOutcome = StorageExportHelper.syncPdfToUserStorage(
                    context = context,
                    localPdfFile = localFile,
                    displayTitle = pdf.title,
                    originalUriString = pdf.originalUriString,
                    existingDownloadUriString = pdf.downloadUriString
                )
                val updated = repository.upsertWorkspacePdf(
                    title = pdf.title,
                    file = localFile,
                    pageCount = newTotal,
                    sourceCategory = pdf.sourceCategory,
                    isPasswordProtected = pdf.isPasswordProtected,
                    originalUriString = syncOutcome.originalUriString,
                    downloadUriString = syncOutcome.downloadUriString,
                    savedLocationLabel = syncOutcome.locationBadge
                )
                _uiState.update {
                    it.copy(
                        activePdf = updated,
                        pendingEdits = emptyList(),
                        isLoading = false,
                        loadingMessage = ""
                    )
                }
                showMessage(pickMsg("Blank page added • ${syncOutcome.messageEn}", "নতুন পেজ যুক্ত হয়েছে • ${syncOutcome.messageBn}"))
            }.onFailure { err ->
                setBusy(false)
                showMessage(pickMsg("Error: ${err.localizedMessage}", "ত্রুটি: ${err.localizedMessage}"))
            }
        }
    }

    fun applyPageOperationsInEditor(
        pageSpecs: List<PageOperationSpec>,
        allCurrentEdits: List<PageEditItem>
    ) {
        val pdf = _uiState.value.activePdf ?: return
        if (pageSpecs.isEmpty()) {
            showMessage(pickMsg("At least 1 page must remain.", "অন্তত ১টি পেজ রাখতে হবে।"))
            return
        }
        val pwd = unlockedPasswords[pdf.filePath] ?: ""
        viewModelScope.launch {
            setBusy(true, pickMsg("Updating pages (reorder / rotate / delete)...", "পেজ সাজানো ও আপডেট হচ্ছে..."))
            runCatching {
                val localFile = File(pdf.filePath)
                if (allCurrentEdits.isNotEmpty()) {
                    PdfEditAndSecurityEngine.saveEditsToPdf(
                        context = context,
                        sourceFile = localFile,
                        edits = allCurrentEdits,
                        password = pwd
                    )
                }
                val newTotal = PdfEditAndSecurityEngine.applyPageOperations(
                    context = context,
                    sourceFile = localFile,
                    pageSpecs = pageSpecs,
                    password = pwd
                )
                val syncOutcome = StorageExportHelper.syncPdfToUserStorage(
                    context = context,
                    localPdfFile = localFile,
                    displayTitle = pdf.title,
                    originalUriString = pdf.originalUriString,
                    existingDownloadUriString = pdf.downloadUriString
                )
                val updated = repository.upsertWorkspacePdf(
                    title = pdf.title,
                    file = localFile,
                    pageCount = newTotal,
                    sourceCategory = pdf.sourceCategory,
                    isPasswordProtected = pdf.isPasswordProtected,
                    originalUriString = syncOutcome.originalUriString,
                    downloadUriString = syncOutcome.downloadUriString,
                    savedLocationLabel = syncOutcome.locationBadge
                )
                _uiState.update {
                    it.copy(
                        activePdf = updated,
                        pendingEdits = emptyList(),
                        isLoading = false,
                        loadingMessage = ""
                    )
                }
                showMessage(
                    pickMsg(
                        "Pages updated ($newTotal pages) • ${syncOutcome.messageEn}",
                        "পেজ আপডেট হয়েছে ($newTotal পেজ) • ${syncOutcome.messageBn}"
                    )
                )
            }.onFailure { err ->
                setBusy(false)
                showMessage(pickMsg("Page update failed: ${err.localizedMessage}", "পেজ আপডেট ব্যর্থ: ${err.localizedMessage}"))
            }
        }
    }

    fun saveEditorChanges(allEditsToSave: List<PageEditItem>) {
        val pdf = _uiState.value.activePdf ?: return
        val pwd = unlockedPasswords[pdf.filePath] ?: ""
        viewModelScope.launch {
            setBusy(true, pickMsg("Saving PDF...", "পরিবর্তনগুলো PDF ফাইলে সেভ হচ্ছে..."))
            runCatching {
                val savedFile = if (allEditsToSave.isNotEmpty()) {
                    PdfEditAndSecurityEngine.saveEditsToPdf(
                        context = context,
                        sourceFile = File(pdf.filePath),
                        edits = allEditsToSave,
                        password = pwd,
                        saveAsNewCopy = false
                    )
                } else {
                    File(pdf.filePath)
                }
                val inspect = PdfEditAndSecurityEngine.inspectPdf(context, savedFile, pwd)

                // Requirement ছ: Overwrite original file in-place if opened from device, or update Download/ file!
                val syncOutcome = StorageExportHelper.syncPdfToUserStorage(
                    context = context,
                    localPdfFile = savedFile,
                    displayTitle = pdf.title,
                    originalUriString = pdf.originalUriString,
                    existingDownloadUriString = pdf.downloadUriString
                )

                val updatedEntity = repository.upsertWorkspacePdf(
                    title = pdf.title,
                    file = savedFile,
                    pageCount = inspect.pageCount,
                    sourceCategory = pdf.sourceCategory,
                    isPasswordProtected = pdf.isPasswordProtected,
                    originalUriString = syncOutcome.originalUriString,
                    downloadUriString = syncOutcome.downloadUriString,
                    savedLocationLabel = syncOutcome.locationBadge
                )
                _uiState.update {
                    it.copy(
                        activePdf = updatedEntity,
                        pendingEdits = emptyList(),
                        isLoading = false,
                        loadingMessage = ""
                    )
                }
                showMessage(pickMsg(syncOutcome.messageEn, syncOutcome.messageBn))
            }.onFailure { err ->
                setBusy(false)
                showMessage(pickMsg("Save failed: ${err.localizedMessage}", "সেভ ব্যর্থ: ${err.localizedMessage}"))
            }
        }
    }

    // --- Requirement চ: Open in Lens & Read Aloud (Edge Browser Return Flow) ---
    fun openPdfInGoogleLens(
        entity: WorkspacePdfEntity,
        pageIndex: Int = 0,
        activityContext: Context,
        unsavedPageEdits: List<PageEditItem> = emptyList()
    ) {
        viewModelScope.launch {
            val file = File(entity.filePath)
            val pwd = unlockedPasswords[entity.filePath] ?: ""
            val inspect = PdfEditAndSecurityEngine.inspectPdf(context, file, pwd)
            if (inspect.requiresPassword) {
                _uiState.update {
                    it.copy(
                        unlockPromptTarget = entity,
                        pendingProtectedAction = PendingProtectedAction.OPEN_IN_LENS,
                        pendingTargetPageIndex = pageIndex
                    )
                }
                return@launch
            }

            setBusy(
                true,
                pickMsg(
                    "Preparing Page ${pageIndex + 1} for Google Lens...",
                    "Google Lens-এর জন্য পেজ ${pageIndex + 1} প্রস্তুত হচ্ছে..."
                )
            )
            runCatching {
                val pageEditsOnly = unsavedPageEdits.filter { it.pageIndex == pageIndex }
                val sourceForLens = if (pageEditsOnly.isNotEmpty()) {
                    PdfEditAndSecurityEngine.saveEditsToPdf(
                        context = context,
                        sourceFile = file,
                        edits = pageEditsOnly,
                        password = pwd,
                        saveAsNewCopy = true
                    )
                } else {
                    file
                }
                val imgFile = try {
                    LensAndReadAloudHelper.renderPdfPageToLensImage(
                        context = context,
                        pdfFile = sourceForLens,
                        pageIndex = pageIndex,
                        password = pwd
                    )
                } finally {
                    if (sourceForLens != file) runCatching { sourceForLens.delete() }
                }
                setBusy(false)
                val launched = LensAndReadAloudHelper.launchGoogleLens(activityContext, imgFile)
                if (!launched) {
                    showMessage(pickMsg("Could not launch Google Lens.", "Google Lens ওপেন করা যায়নি।"))
                }
            }.onFailure { err ->
                setBusy(false)
                showMessage(pickMsg("Lens error: ${err.localizedMessage}", "ত্রুটি: ${err.localizedMessage}"))
            }
        }
    }

    fun startReadAloudForPdf(
        entity: WorkspacePdfEntity,
        pageIndex: Int = 0,
        activityContext: Context,
        unsavedPageEdits: List<PageEditItem> = emptyList()
    ) {
        viewModelScope.launch {
            val file = File(entity.filePath)
            val pwd = unlockedPasswords[entity.filePath] ?: ""
            val inspect = PdfEditAndSecurityEngine.inspectPdf(context, file, pwd)
            if (inspect.requiresPassword) {
                _uiState.update {
                    it.copy(
                        unlockPromptTarget = entity,
                        pendingProtectedAction = PendingProtectedAction.READ_ALOUD,
                        pendingTargetPageIndex = pageIndex
                    )
                }
                return@launch
            }

            // Show splash message "টেক্সট কপি করুন" clearly to the user!
            _uiState.update {
                it.copy(
                    showReadAloudSplash = true,
                    readAloudStage = ReadAloudStage.LAUNCHING_LENS
                )
            }
            Toast.makeText(
                context,
                "টেক্সট কপি করুন (পেজ ${pageIndex + 1}) • Copy text in Google Lens, then press Back",
                Toast.LENGTH_LONG
            ).show()

            runCatching {
                val pageEditsOnly = unsavedPageEdits.filter { it.pageIndex == pageIndex }
                val sourceForLens = if (pageEditsOnly.isNotEmpty()) {
                    PdfEditAndSecurityEngine.saveEditsToPdf(
                        context = context,
                        sourceFile = file,
                        edits = pageEditsOnly,
                        password = pwd,
                        saveAsNewCopy = true
                    )
                } else {
                    file
                }
                val imgFile = try {
                    LensAndReadAloudHelper.renderPdfPageToLensImage(
                        context = context,
                        pdfFile = sourceForLens,
                        pageIndex = pageIndex,
                        password = pwd
                    )
                } finally {
                    if (sourceForLens != file) runCatching { sourceForLens.delete() }
                }
                // Keep splash visible briefly so the user sees "টেক্সট কপি করুন" before Google Lens opens
                delay(900)
                _uiState.update { it.copy(showReadAloudSplash = false) }
                val launched = LensAndReadAloudHelper.launchGoogleLens(activityContext, imgFile)
                if (!launched) {
                    _uiState.update { it.copy(readAloudStage = ReadAloudStage.IDLE) }
                    showMessage(pickMsg("Could not launch Google Lens.", "Google Lens ওপেন করা যায়নি।"))
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(showReadAloudSplash = false, readAloudStage = ReadAloudStage.IDLE)
                }
                showMessage(pickMsg("Read Aloud error: ${err.localizedMessage}", "ত্রুটি: ${err.localizedMessage}"))
            }
        }
    }

    fun notifyAppPausedForExternalActivity() {
        if (_uiState.value.readAloudStage == ReadAloudStage.LAUNCHING_LENS) {
            _uiState.update { it.copy(readAloudStage = ReadAloudStage.IN_LENS, showReadAloudSplash = false) }
        }
    }

    fun notifyAppResumed(activityContext: Context) {
        if (_uiState.value.readAloudStage == ReadAloudStage.IN_LENS) {
            _uiState.update { it.copy(readAloudStage = ReadAloudStage.IDLE, showReadAloudSplash = false) }
            LensAndReadAloudHelper.openReadAloudSiteInEdge(activityContext)
        }
    }

    // --- PDF to Image Converter ---
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
                showMessage(pickMsg("PDF to Image failed: ${err.localizedMessage}", "PDF থেকে ইমেজ কনভার্ট ব্যর্থ: ${err.localizedMessage}"))
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
            showMessage(
                if (ok) pickMsg(
                    "Saved Page ${img.pageNumber} to Pictures/NokshiPDF!",
                    "পেজ ${img.pageNumber} গ্যালারির Pictures/NokshiPDF ফোল্ডারে সেভ হয়েছে!"
                ) else pickMsg("Failed to save image.", "গ্যালারিতে সেভ করা যায়নি।")
            )
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
            showMessage(
                pickMsg(
                    "$count images saved to Pictures/NokshiPDF!",
                    "$count টি ইমেজ গ্যালারির Pictures/NokshiPDF ফোল্ডারে সংরক্ষিত হয়েছে!"
                )
            )
        }
    }

    private fun setBusy(busy: Boolean, msg: String = "") {
        _uiState.update { it.copy(isLoading = busy, loadingMessage = msg) }
    }
}
