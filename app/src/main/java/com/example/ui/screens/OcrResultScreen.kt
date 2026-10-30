package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.engine.OcrLensEngine
import com.example.engine.OcrPageResult
import com.example.engine.OcrStepProgress
import com.example.ui.components.NokshiIcons
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrResultScreen(
    pdfTitle: String,
    isProcessing: Boolean,
    progress: OcrStepProgress?,
    pageResults: List<OcrPageResult>,
    onBack: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var selectedPageFilter by remember { mutableIntStateOf(0) } // 0 = All pages, 1..N = page
    var tapSelectMode by remember { mutableStateOf(true) }
    val selectedSegmentIds = remember { mutableStateListOf<String>() }

    val visiblePages = remember(pageResults, selectedPageFilter) {
        if (selectedPageFilter == 0) pageResults
        else pageResults.filter { it.pageNumber == selectedPageFilter }
    }
    val visibleSegments = remember(visiblePages) {
        visiblePages.flatMap { it.segments }
    }
    val fullCombinedText = remember(visiblePages) {
        visiblePages.joinToString("\n\n") { page ->
            if (visiblePages.size > 1) "--- পেজ ${page.pageNumber} ---\n${page.fullText}"
            else page.fullText
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("OCR টেক্সট এক্সট্রাক্ট উইন্ডো", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = pdfTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("ocr_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                actions = {
                    if (!isProcessing && visibleSegments.isNotEmpty() && tapSelectMode) {
                        IconButton(
                            onClick = {
                                val allSelected = visibleSegments.all { it.id in selectedSegmentIds }
                                if (allSelected) {
                                    selectedSegmentIds.clear()
                                } else {
                                    visibleSegments.forEach { seg ->
                                        if (seg.id !in selectedSegmentIds) selectedSegmentIds.add(seg.id)
                                    }
                                }
                            },
                            modifier = Modifier.testTag("ocr_select_all_button")
                        ) {
                            Icon(NokshiIcons.SelectAll, contentDescription = "সব সিলেক্ট")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!isProcessing && pageResults.isNotEmpty()) {
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                copyToClipboard(context, fullCombinedText)
                                onShowMessage("সম্পূর্ণ এক্সট্রাক্ট করা টেক্সট কপি হয়েছে!")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ocr_copy_all_button")
                        ) {
                            Icon(NokshiIcons.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("সব কপি করুন")
                        }

                        if (tapSelectMode) {
                            Button(
                                enabled = selectedSegmentIds.isNotEmpty(),
                                onClick = {
                                    val chosenText = pageResults
                                        .flatMap { it.segments }
                                        .filter { it.id in selectedSegmentIds }
                                        .joinToString("\n") { it.text }
                                    copyToClipboard(context, chosenText)
                                    onShowMessage("সিলেক্ট করা ${selectedSegmentIds.size}টি অংশ কপি হয়েছে!")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ocr_copy_selected_button")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("সিলেক্ট কপি (${selectedSegmentIds.size})")
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (isProcessing) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp)
                    ) {
                        CircularProgressIndicator()
                        Icon(
                            imageVector = NokshiIcons.DocumentScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "ব্যাকএন্ডে ইমেজ কনভার্ট ও OCR চলছে...",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = progress?.stepTitleBn ?: "PDF পেজগুলো ইমেজে রূপান্তর করে Google Lens / ML Kit ইঞ্জিনে টেক্সট পড়া হচ্ছে...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Page Filter Chips
                if (pageResults.size > 1) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedPageFilter == 0,
                                    onClick = { selectedPageFilter = 0 },
                                    label = { Text("সব পেজ (${pageResults.size})") }
                                )
                            }
                            items(pageResults) { pr ->
                                FilterChip(
                                    selected = selectedPageFilter == pr.pageNumber,
                                    onClick = { selectedPageFilter = pr.pageNumber },
                                    label = { Text("পেজ ${pr.pageNumber}") }
                                )
                            }
                        }
                    }
                }

                // Converted Image + Open in Google Lens Card
                val activePreviewPage = visiblePages.firstOrNull()
                if (activePreviewPage != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                AsyncImage(
                                    model = File(activePreviewPage.imagePath),
                                    contentDescription = "কনভার্টকৃত পেজ ইমেজ",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(width = 64.dp, height = 84.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "ব্যাকএন্ডে কনভার্টকৃত পেজ ইমেজ (#${activePreviewPage.pageNumber})",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Google ML Kit / Lens ইঞ্জিনে টেক্সট এক্সট্রাক্ট সম্পন্ন। চাইলে সরাসরি Google Lens অ্যাপেও ইমেজটি খুলতে পারেন।",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    FilledTonalButton(
                                        onClick = {
                                            val ok = OcrLensEngine.openConvertedImageInGoogleLens(
                                                context,
                                                activePreviewPage.imagePath
                                            )
                                            if (!ok) {
                                                onShowMessage("ইমেজটি ওপেন করার অ্যাপ পাওয়া যায়নি।")
                                            }
                                        },
                                        modifier = Modifier.testTag("open_google_lens_button")
                                    ) {
                                        Icon(NokshiIcons.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Google Lens-এ ইমেজ খুলুন")
                                    }
                                }
                            }
                        }
                    }
                }

                // Selection Mode Switcher
                item {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = tapSelectMode,
                            onClick = { tapSelectMode = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("ট্যাপ করে লাইন সিলেক্ট")
                        }
                        SegmentedButton(
                            selected = !tapSelectMode,
                            onClick = { tapSelectMode = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("যেকোনো শব্দ সিলেক্ট")
                        }
                    }
                }

                if (tapSelectMode) {
                    item {
                        Text(
                            text = "নিচের যেকোনো লাইনে ট্যাপ করে সিলেক্ট করুন এবং নিচে ‘সিলেক্ট কপি’ বাটন চাপুন:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(visibleSegments, key = { it.id }) { segment ->
                        val isChosen = segment.id in selectedSegmentIds
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChosen) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isChosen) 3.dp else 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChosen) selectedSegmentIds.remove(segment.id)
                                    else selectedSegmentIds.add(segment.id)
                                }
                                .testTag("ocr_segment_${segment.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = if (isChosen) Icons.Default.CheckCircle else NokshiIcons.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isChosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    if (visiblePages.size > 1) {
                                        Text(
                                            text = "পেজ ${segment.pageNumber}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = segment.text,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        copyToClipboard(context, segment.text)
                                        onShowMessage("লাইনটি কপি হয়েছে!")
                                    }
                                ) {
                                    Icon(
                                        NokshiIcons.ContentCopy,
                                        contentDescription = "কপি করুন",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "টেক্সটের ওপর চেপে ধরে (Long-press) যেকোনো শব্দ বা অংশ সিলেক্ট করে কপি করুন:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(10.dp))
                                SelectionContainer {
                                    Text(
                                        text = fullCombinedText.ifBlank { "কোনো টেক্সট পাওয়া যায়নি।" },
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Nokshi OCR Text", text))
}
