package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.engine.ConvertedPageImage
import com.example.ui.components.NokshiIcons
import com.example.ui.i18n.tr
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToImageScreen(
    pdfTitle: String,
    isConverting: Boolean,
    images: List<ConvertedPageImage>,
    onBack: () -> Unit,
    onReconvert: (highQuality: Boolean, asPng: Boolean) -> Unit,
    onSaveSingleToGallery: (ConvertedPageImage) -> Unit,
    onSaveAllToGallery: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var highQuality by remember { mutableStateOf(true) }
    var asPng by remember { mutableStateOf(true) }
    val shareChooserTitle = tr("Share Image", "ইমেজ শেয়ার করুন")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(tr("PDF to Image Converter", "PDF থেকে ইমেজ কনভার্টার"), style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = tr(
                                "$pdfTitle • ${images.size} page images (Saves to Pictures/NokshiPDF)",
                                "$pdfTitle • মোট ${images.size}টি পেজ ইমেজ (Pictures/NokshiPDF ফোল্ডারে সেভ হয়)"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("pdf_to_img_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Back", "ফিরে যান"))
                    }
                }
            )
        },
        bottomBar = {
            if (!isConverting && images.isNotEmpty()) {
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Button(
                        onClick = onSaveAllToGallery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("save_all_images_button")
                    ) {
                        Icon(NokshiIcons.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            tr(
                                "Save All to Gallery — Pictures/NokshiPDF (${images.size})",
                                "সবগুলো পেজ গ্যালারিতে সেভ করুন (${images.size}টি)"
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = asPng,
                            onClick = {
                                asPng = true
                                onReconvert(highQuality, true)
                            },
                            label = { Text("PNG") }
                        )
                        FilterChip(
                            selected = !asPng,
                            onClick = {
                                asPng = false
                                onReconvert(highQuality, false)
                            },
                            label = { Text("JPG") }
                        )
                        FilterChip(
                            selected = highQuality,
                            onClick = {
                                highQuality = !highQuality
                                onReconvert(highQuality, asPng)
                            },
                            label = { Text(if (highQuality) tr("HD Quality", "HD কোয়ালিটি") else tr("Standard", "স্ট্যান্ডার্ড")) }
                        )
                    }
                    IconButton(onClick = { onReconvert(highQuality, asPng) }) {
                        Icon(Icons.Default.Refresh, contentDescription = tr("Re-render", "পুনরায় রেন্ডার করুন"))
                    }
                }
            }

            if (isConverting) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            tr("Converting PDF pages to images...", "PDF পেজগুলো ইমেজে কনভার্ট করা হচ্ছে..."),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 165.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(images, key = { it.pageNumber }) { img ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.72f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                ) {
                                    AsyncImage(
                                        model = File(img.filePath),
                                        contentDescription = "Page ${img.pageNumber}",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = tr("Page ${img.pageNumber} • ${img.formatLabel}", "পেজ ${img.pageNumber} • ${img.formatLabel}"),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "${img.width} × ${img.height} px",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(
                                        onClick = { onSaveSingleToGallery(img) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(NokshiIcons.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(tr("Save", "সেভ"))
                                    }
                                    IconButton(
                                        onClick = {
                                            runCatching {
                                                val file = File(img.filePath)
                                                val uri = FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    file
                                                )
                                                val share = Intent(Intent.ACTION_SEND).apply {
                                                    type = "image/*"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(share, shareChooserTitle))
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = tr("Share", "শেয়ার"))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
