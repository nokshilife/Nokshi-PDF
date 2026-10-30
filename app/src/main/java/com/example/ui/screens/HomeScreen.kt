package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.WorkspacePdfEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    workspacePdfs: List<WorkspacePdfEntity>,
    onOpenExistingPdfPicker: () -> Unit,
    onConvertDocumentPicker: () -> Unit,
    onConvertImagePicker: () -> Unit,
    onConvertExcelPicker: () -> Unit,
    onConvertPptPicker: () -> Unit,
    onOpenWriteDocDialog: () -> Unit,
    onCreateSamplePdf: () -> Unit,
    onNavigateToMergeTab: () -> Unit,
    onOpenPdfInEditor: (WorkspacePdfEntity) -> Unit,
    onRunOcrOnPdf: (WorkspacePdfEntity) -> Unit,
    onConvertPdfToImages: (WorkspacePdfEntity) -> Unit,
    onManagePassword: (WorkspacePdfEntity) -> Unit,
    onDeletePdf: (WorkspacePdfEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Nokshi Kantha Stitched Hero Banner
        item {
            val primary = MaterialTheme.colorScheme.primary
            val secondary = MaterialTheme.colorScheme.secondary
            val stitchColor = Color.White.copy(alpha = 0.45f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(primary, secondary)
                        )
                    )
                    .drawBehind {
                        val inset = 10.dp.toPx()
                        drawRoundRect(
                            color = stitchColor,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - inset * 2, size.height - inset * 2),
                            cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                            style = Stroke(
                                width = 1.6.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                            )
                        )
                    }
                    .padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_nokshi_logo),
                            contentDescription = "Nokshi PDF লোগো",
                            modifier = Modifier
                                .size(62.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Nokshi PDF",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ডকুমেন্ট, ইমেজ, এক্সেল ও প্রেজেন্টেশন কনভার্টার • স্মার্ট এডিটর • সিগনেচার ও OCR",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.90f)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onOpenExistingPdfPicker,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = primary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hero_open_pdf_button")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("PDF ওপেন করুন", fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = onOpenWriteDocDialog,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("hero_write_pdf_button")
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ডকুমেন্ট লিখুন")
                        }
                    }
                }
            }
        }

        // Section 1: File-to-PDF Converters (Requirement 1)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "ফাইল থেকে PDF ওপেন বা কনভার্ট করুন",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ConvertFormatCard(
                        title = "ডকুমেন্ট",
                        subtitle = "DOCX, TXT, RTF",
                        icon = Icons.Default.Description,
                        accent = MaterialTheme.colorScheme.primary,
                        onClick = onConvertDocumentPicker,
                        modifier = Modifier.weight(1f),
                        tag = "home_convert_doc"
                    )
                    ConvertFormatCard(
                        title = "ইমেজ",
                        subtitle = "JPG, PNG → PDF",
                        icon = Icons.Default.Image,
                        accent = MaterialTheme.colorScheme.secondary,
                        onClick = onConvertImagePicker,
                        modifier = Modifier.weight(1f),
                        tag = "home_convert_image"
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ConvertFormatCard(
                        title = "এক্সেল শীট",
                        subtitle = "XLSX, CSV টেবিল",
                        icon = Icons.Default.TableChart,
                        accent = Color(0xFF0F6E56),
                        onClick = onConvertExcelPicker,
                        modifier = Modifier.weight(1f),
                        tag = "home_convert_excel"
                    )
                    ConvertFormatCard(
                        title = "পাওয়ারপয়েন্ট",
                        subtitle = "PPTX স্লাইড",
                        icon = Icons.Default.Slideshow,
                        accent = Color(0xFFB45309),
                        onClick = onConvertPptPicker,
                        modifier = Modifier.weight(1f),
                        tag = "home_convert_ppt"
                    )
                }
            }
        }

        // Section 2: Quick Power Actions Strip
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("দ্রুত প্রো টুলস", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        AssistChip(
                            onClick = onNavigateToMergeTab,
                            label = { Text("PDF মার্জ করুন") },
                            leadingIcon = {
                                Icon(Icons.Default.CallMerge, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.testTag("quick_merge_chip")
                        )
                    }
                    item {
                        AssistChip(
                            onClick = onCreateSamplePdf,
                            label = { Text("নমুনা PDF তৈরি করুন") },
                            leadingIcon = {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.testTag("quick_sample_pdf_chip")
                        )
                    }
                }
            }
        }

        // Section 3: Workspace & Recent PDFs
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("আপনার ওয়ার্কস্পেস PDF সমূহ", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${workspacePdfs.size}টি ফাইল",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (workspacePdfs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "এখনও কোনো PDF যোগ করা হয়নি",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "ডিভাইস থেকে ডকুমেন্ট, ছবি, এক্সেল বা পাওয়ারপয়েন্ট কনভার্ট করুন অথবা এক ট্যাপে নমুনা PDF তৈরি করে সব এডিটিং ও OCR টুল পরীক্ষা করুন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilledTonalButton(
                            onClick = onCreateSamplePdf,
                            modifier = Modifier.testTag("empty_state_sample_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("নমুনা নকশি PDF তৈরি করুন")
                        }
                    }
                }
            }
        } else {
            items(workspacePdfs, key = { it.id }) { pdf ->
                WorkspacePdfCard(
                    pdf = pdf,
                    onEdit = { onOpenPdfInEditor(pdf) },
                    onOcr = { onRunOcrOnPdf(pdf) },
                    onPdfToImages = { onConvertPdfToImages(pdf) },
                    onPassword = { onManagePassword(pdf) },
                    onShare = {
                        runCatching {
                            val f = File(pdf.filePath)
                            if (f.exists()) {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    f
                                )
                                val send = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(send, "PDF শেয়ার করুন"))
                            }
                        }
                    },
                    onDelete = { onDeletePdf(pdf) }
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ConvertFormatCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.14f))
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WorkspacePdfCard(
    pdf: WorkspacePdfEntity,
    onEdit: () -> Unit,
    onOcr: () -> Unit,
    onPdfToImages: () -> Unit,
    onPassword: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val kb = (pdf.fileSizeBytes / 1024L).coerceAtLeast(1L)
    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(pdf.updatedAt))

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .testTag("workspace_pdf_card_${pdf.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = if (pdf.isPasswordProtected) Icons.Default.Lock else Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pdf.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${pdf.pageCount} পেজ • ${kb} KB • $dateStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = pdf.sourceCategory,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Quick Actions Row for Each PDF
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(
                        onClick = onEdit,
                        label = { Text("এডিট") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    AssistChip(
                        onClick = onOcr,
                        label = { Text("OCR") },
                        leadingIcon = { Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    AssistChip(
                        onClick = onPdfToImages,
                        label = { Text("ইমেজ") },
                        leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                Row {
                    IconButton(onClick = onPassword, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Default.Lock, contentDescription = "পাসওয়ার্ড লক", modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "শেয়ার", modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "মুছে ফেলুন",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}
