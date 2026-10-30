package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.WorkspacePdfEntity
import com.example.ui.components.NokshiIcons
import com.example.ui.i18n.tr
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
    onOpenInLens: (WorkspacePdfEntity) -> Unit,
    onReadAloud: (WorkspacePdfEntity) -> Unit,
    onConvertPdfToImages: (WorkspacePdfEntity) -> Unit,
    onManagePassword: (WorkspacePdfEntity) -> Unit,
    onDeletePdf: (WorkspacePdfEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shareTitle = tr("Share PDF", "PDF শেয়ার করুন")

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("home_lazy_column")
    ) {
        // Nokshi Kantha Stitched Hero Banner with High-Contrast Primary CTA (Requirement ঙ)
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
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_nokshi_logo),
                            contentDescription = "Nokshi PDF Logo",
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
                                text = tr(
                                    "Full-Width PDF Viewer • Smart Editor • Signature • Lens & Read Aloud",
                                    "ফুল-উইডথ PDF ভিউয়ার • স্মার্ট এডিটর • সিগনেচার • Lens ও Read Aloud"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.94f)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Ultra High-Contrast Golden-Amber CTA Button (Requirement ঙ)
                        Button(
                            onClick = onOpenExistingPdfPicker,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFCA28),
                                contentColor = Color(0xFF1A1100)
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 6.dp,
                                pressedElevation = 10.dp
                            ),
                            border = BorderStroke(1.5.dp, Color.White),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .height(50.dp)
                                .testTag("hero_open_pdf_button")
                        ) {
                            Icon(NokshiIcons.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = tr("Open PDF", "PDF ওপেন করুন"),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                        }
                        OutlinedButton(
                            onClick = onOpenWriteDocDialog,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White.copy(alpha = 0.16f),
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.85f)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("hero_write_pdf_button")
                        ) {
                            Icon(NokshiIcons.EditNote, contentDescription = null, modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = tr("Write PDF", "ডকুমেন্ট লিখুন"),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Section 1: File-to-PDF Converters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = tr("Open or Convert Files to PDF", "ফাইল থেকে PDF ওপেন বা কনভার্ট করুন"),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ConvertFormatCard(
                        title = tr("Document", "ডকুমেন্ট"),
                        subtitle = "DOCX, TXT, RTF",
                        icon = NokshiIcons.Description,
                        accent = MaterialTheme.colorScheme.primary,
                        onClick = onConvertDocumentPicker,
                        modifier = Modifier.weight(1f),
                        tag = "home_convert_doc"
                    )
                    ConvertFormatCard(
                        title = tr("Images", "ইমেজ"),
                        subtitle = "JPG, PNG → PDF",
                        icon = NokshiIcons.Image,
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
                        title = tr("Excel Sheet", "এক্সেল শীট"),
                        subtitle = tr("XLSX, CSV Table", "XLSX, CSV টেবিল"),
                        icon = NokshiIcons.TableChart,
                        accent = Color(0xFF0F6E56),
                        onClick = onConvertExcelPicker,
                        modifier = Modifier.weight(1f),
                        tag = "home_convert_excel"
                    )
                    ConvertFormatCard(
                        title = tr("PowerPoint", "পাওয়ারপয়েন্ট"),
                        subtitle = tr("PPTX Slides", "PPTX স্লাইড"),
                        icon = NokshiIcons.Slideshow,
                        accent = Color(0xFFB45309),
                        onClick = onConvertPptPicker,
                        modifier = Modifier.weight(1f),
                        tag = "home_convert_ppt"
                    )
                }
            }
        }

        // Section 2: Quick Power Actions Strip + Storage Info Note
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("Quick Pro Tools", "দ্রুত প্রো টুলস"), style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        AssistChip(
                            onClick = onNavigateToMergeTab,
                            label = { Text(tr("Merge PDFs", "PDF মার্জ করুন")) },
                            leadingIcon = {
                                Icon(NokshiIcons.CallMerge, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.testTag("quick_merge_chip")
                        )
                    }
                    item {
                        AssistChip(
                            onClick = onCreateSamplePdf,
                            label = { Text(tr("Create Sample PDF", "নমুনা PDF তৈরি করুন")) },
                            leadingIcon = {
                                Icon(NokshiIcons.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            modifier = Modifier.testTag("quick_sample_pdf_chip")
                        )
                    }
                }
                Text(
                    text = tr(
                        "📁 Save Rule: Opened PDFs save directly to the original file; new/converted PDFs save to your Download folder.",
                        "📁 সেভ নিয়ম: ওপেন করা পুরনো PDF মূল ফাইলেই সেভ হয়; নতুন বা কনভার্ট করা PDF সরাসরি Download ফোল্ডারে সেভ হয়।"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section 3: Workspace & Recent PDFs
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(tr("Your PDF Documents", "আপনার ওয়ার্কস্পেস PDF সমূহ"), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = tr("${workspacePdfs.size} files", "${workspacePdfs.size}টি ফাইল"),
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
                            imageVector = NokshiIcons.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = tr("No PDFs opened yet", "এখনও কোনো PDF যোগ করা হয়নি"),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = tr(
                                "Open any PDF or convert Document, Image, Excel, or PowerPoint files — or tap below to generate a starter PDF.",
                                "ডিভাইস থেকে PDF ওপেন করুন অথবা ডকুমেন্ট, ছবি, এক্সেল বা পাওয়ারপয়েন্ট কনভার্ট করুন।"
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilledTonalButton(
                            onClick = onCreateSamplePdf,
                            modifier = Modifier.testTag("empty_state_sample_button")
                        ) {
                            Icon(NokshiIcons.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(tr("Create Sample Nokshi PDF", "নমুনা নকশি PDF তৈরি করুন"))
                        }
                    }
                }
            }
        } else {
            items(workspacePdfs, key = { it.id }) { pdf ->
                WorkspacePdfCard(
                    pdf = pdf,
                    onEdit = { onOpenPdfInEditor(pdf) },
                    onOpenInLens = { onOpenInLens(pdf) },
                    onReadAloud = { onReadAloud(pdf) },
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
                                context.startActivity(Intent.createChooser(send, shareTitle))
                            }
                        }
                    },
                    onDelete = { onDeletePdf(pdf) }
                )
            }
        }

        // Requirements ৫ & ৬: Developer Credit Footer at the very bottom of Home Screen
        item {
            DeveloperCreditFooterCard()
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun DeveloperCreditFooterCard() {
    val context = LocalContext.current
    val websiteUrl = "https://jalil.bcs24admin.org.bd/"
    val mobileNumber = "+880 1717 501021"
    val copiedToastText = tr("Mobile number copied: +880 1717 501021", "মোবাইল নাম্বার কপি হয়েছে: +880 1717 501021")

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("developer_credit_footer")
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Developed by: Md A Jalil (15703)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "24th BCS Administration Cadre",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Clickable URL Row (min 48dp touch target) -> Opens in browser
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        runCatching {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(websiteUrl)).apply {
                                addCategory(Intent.CATEGORY_BROWSABLE)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(browserIntent)
                        }
                    }
                    .padding(horizontal = 10.dp)
                    .testTag("developer_website_link")
            ) {
                Text(
                    text = websiteUrl,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = NokshiIcons.OpenInNew,
                    contentDescription = tr("Open website in browser", "ওয়েবসাইট ব্রাউজারে ওপেন করুন"),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Mobile Number + Copy Icon Button beside it (48dp touch target)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Mobile: $mobileNumber",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("Developer Mobile", mobileNumber))
                        Toast.makeText(context, copiedToastText, Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("copy_developer_mobile_button")
                ) {
                    Icon(
                        imageVector = NokshiIcons.ContentCopy,
                        contentDescription = tr("Copy mobile number", "মোবাইল নাম্বার কপি করুন"),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
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
    onOpenInLens: () -> Unit,
    onReadAloud: () -> Unit,
    onPdfToImages: () -> Unit,
    onPassword: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val kb = (pdf.fileSizeBytes / 1024L).coerceAtLeast(1L)
    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(pdf.updatedAt))
    val locationLabel = pdf.savedLocationLabel.ifBlank {
        if (!pdf.originalUriString.isNullOrBlank()) tr("Original Device File", "মূল ডিভাইস ফাইল")
        else "Download/${pdf.title}.pdf"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .testTag("workspace_pdf_card_${pdf.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        imageVector = if (pdf.isPasswordProtected) Icons.Default.Lock else NokshiIcons.PictureAsPdf,
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
                        text = tr(
                            "${pdf.pageCount} pages • ${kb} KB • $dateStr",
                            "${pdf.pageCount} পেজ • ${kb} KB • $dateStr"
                        ),
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

            // Explicit Save Location Badge (Requirement ছ)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = NokshiIcons.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = locationLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Side-by-Side Open in Lens + Read Aloud + Edit + Image Actions (Requirement চ)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                AssistChip(
                    onClick = onEdit,
                    label = { Text(tr("View / Edit", "ভিউ / এডিট")) },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp)) }
                )
                AssistChip(
                    onClick = onOpenInLens,
                    label = { Text("Open in Lens") },
                    leadingIcon = { Icon(NokshiIcons.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(15.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                    )
                )
                AssistChip(
                    onClick = onReadAloud,
                    label = { Text("Read Aloud") },
                    leadingIcon = { Icon(NokshiIcons.VolumeUp, contentDescription = null, modifier = Modifier.size(15.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f)
                    )
                )
                AssistChip(
                    onClick = onPdfToImages,
                    label = { Text(tr("Images", "ইমেজ")) },
                    leadingIcon = { Icon(NokshiIcons.Image, contentDescription = null, modifier = Modifier.size(15.dp)) }
                )
            }

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onPassword, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = tr("Password Lock", "পাসওয়ার্ড লক"), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Share, contentDescription = tr("Share", "শেয়ার"), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = tr("Delete", "মুছে ফেলুন"),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
