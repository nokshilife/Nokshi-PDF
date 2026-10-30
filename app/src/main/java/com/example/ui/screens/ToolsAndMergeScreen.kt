package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.WorkspacePdfEntity
import com.example.ui.components.NokshiIcons
import com.example.ui.i18n.tr

@Composable
fun ToolsAndMergeScreen(
    workspacePdfs: List<WorkspacePdfEntity>,
    onPickDocumentToPdf: () -> Unit,
    onPickImagesToPdf: () -> Unit,
    onPickExcelToPdf: () -> Unit,
    onPickPptToPdf: () -> Unit,
    onImportMultiplePdfsForMerge: () -> Unit,
    onMergeSelectedPdfs: (List<WorkspacePdfEntity>, String) -> Unit,
    onPickPdfForLens: () -> Unit,
    onPickPdfForReadAloud: () -> Unit,
    onPickPdfForImages: () -> Unit,
    onPickPdfForPassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedForMerge = remember { mutableStateListOf<Long>() }
    var mergedName by remember { mutableStateOf("Merged_Nokshi_Document") }

    val orderedSelectedEntities = selectedForMerge.mapNotNull { id ->
        workspacePdfs.find { it.id == id }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = tr("All-in-One PDF Toolbox & Merge Studio", "অল-ইন-ওয়ান PDF টুলবক্স ও মার্জ স্টুডিও"),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = tr(
                    "Convert files to PDF (saved in Download/), merge multiple PDFs, protect with passwords, or launch Google Lens & Read Aloud.",
                    "যেকোনো ফরম্যাট থেকে PDF কনভার্ট (Download ফোল্ডারে সেভ হয়), একাধিক PDF মার্জ, পাসওয়ার্ড লক এবং Google Lens ও Read Aloud।"
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Quick Converters & Lens/Read Aloud Row Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ToolActionTile(
                        title = tr("Document → PDF", "ডকুমেন্ট → PDF"),
                        subtitle = "DOCX, TXT, HTML",
                        icon = NokshiIcons.Description,
                        onClick = onPickDocumentToPdf,
                        modifier = Modifier.weight(1f),
                        tag = "tools_doc_to_pdf"
                    )
                    ToolActionTile(
                        title = tr("Images → PDF", "ইমেজ → PDF"),
                        subtitle = "JPG, PNG, WEBP",
                        icon = NokshiIcons.Image,
                        onClick = onPickImagesToPdf,
                        modifier = Modifier.weight(1f),
                        tag = "tools_img_to_pdf"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ToolActionTile(
                        title = tr("Excel → PDF", "এক্সেল → PDF"),
                        subtitle = tr("XLSX, CSV Sheet", "XLSX, CSV শীট"),
                        icon = NokshiIcons.TableChart,
                        onClick = onPickExcelToPdf,
                        modifier = Modifier.weight(1f),
                        tag = "tools_excel_to_pdf"
                    )
                    ToolActionTile(
                        title = tr("Presentation → PDF", "প্রেজেন্টেশন → PDF"),
                        subtitle = tr("PPTX Slides", "PPTX স্লাইড"),
                        icon = NokshiIcons.Slideshow,
                        onClick = onPickPptToPdf,
                        modifier = Modifier.weight(1f),
                        tag = "tools_ppt_to_pdf"
                    )
                }
                // Requirement চ: Side-by-Side Open in Lens + Read Aloud Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ToolActionTile(
                        title = "Open in Lens",
                        subtitle = tr("PDF → Image → Google Lens", "PDF → ইমেজ → Google Lens"),
                        icon = NokshiIcons.CenterFocusStrong,
                        onClick = onPickPdfForLens,
                        modifier = Modifier.weight(1f),
                        tag = "tools_open_in_lens"
                    )
                    ToolActionTile(
                        title = "Read Aloud",
                        subtitle = tr("Lens Copy → Edge Reader", "Lens কপি → Edge ব্রাউজার"),
                        icon = NokshiIcons.VolumeUp,
                        onClick = onPickPdfForReadAloud,
                        modifier = Modifier.weight(1f),
                        tag = "tools_read_aloud"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onPickPdfForImages,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tools_pdf_to_img_button")
                    ) {
                        Icon(NokshiIcons.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("PDF → Images", "PDF → ইমেজ"), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onPickPdfForPassword,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tools_lock_pdf")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Password Lock", "পাসওয়ার্ড লক"), maxLines = 1)
                    }
                }
            }
        }

        // Interactive PDF Merge Studio
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(NokshiIcons.CallMerge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(tr("PDF Merge Studio", "PDF মার্জ স্টুডিও"), style = MaterialTheme.typography.titleLarge)
                        }
                        FilledTonalButton(
                            onClick = onImportMultiplePdfsForMerge,
                            modifier = Modifier.testTag("import_pdfs_for_merge_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(tr("Add Files", "ফাইল আনুন"))
                        }
                    }

                    Text(
                        text = tr(
                            "Select 2 or more PDFs below, reorder if needed, and merge into Download/ folder:",
                            "নিচের তালিকা থেকে ২ বা ততোধিক PDF নির্বাচন করে একটি ফাইলে মার্জ করুন (Download ফোল্ডারে সেভ হবে):"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = mergedName,
                        onValueChange = { mergedName = it },
                        label = { Text(tr("Merged PDF File Name", "মার্জ করা নতুন PDF-এর নাম")) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("merge_output_name_input")
                    )

                    Button(
                        enabled = orderedSelectedEntities.size >= 2,
                        onClick = {
                            onMergeSelectedPdfs(orderedSelectedEntities, mergedName)
                            selectedForMerge.clear()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("execute_merge_button")
                    ) {
                        Icon(NokshiIcons.CallMerge, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            tr(
                                "Merge PDFs (${orderedSelectedEntities.size} selected)",
                                "মার্জ করুন (${orderedSelectedEntities.size}টি ফাইল নির্বাচিত)"
                            )
                        )
                    }
                }
            }
        }

        if (workspacePdfs.isEmpty()) {
            item {
                Text(
                    text = tr(
                        "Tap 'Add Files' above to import multiple PDFs from your device for merging.",
                        "মার্জ করার জন্য ওপরে ‘ফাইল আনুন’ বাটনে ট্যাপ করে স্টোরেজ থেকে একাধিক PDF যোগ করুন।"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            itemsIndexed(workspacePdfs, key = { _, item -> item.id }) { _, pdf ->
                val orderIdx = selectedForMerge.indexOf(pdf.id)
                val isSelected = orderIdx >= 0
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isSelected) selectedForMerge.remove(pdf.id)
                            else selectedForMerge.add(pdf.id)
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = if (isSelected) NokshiIcons.CheckBox else NokshiIcons.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isSelected) "#${orderIdx + 1}  ${pdf.title}" else pdf.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = tr(
                                    "${pdf.pageCount} pages • ${pdf.sourceCategory}",
                                    "${pdf.pageCount} পেজ • ${pdf.sourceCategory}"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected && selectedForMerge.size > 1) {
                            IconButton(
                                enabled = orderIdx > 0,
                                onClick = {
                                    val prev = selectedForMerge[orderIdx - 1]
                                    selectedForMerge[orderIdx - 1] = pdf.id
                                    selectedForMerge[orderIdx] = prev
                                }
                            ) {
                                Icon(NokshiIcons.ArrowUpward, contentDescription = tr("Move up", "ওপরে নিন"), modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                enabled = orderIdx < selectedForMerge.lastIndex,
                                onClick = {
                                    val next = selectedForMerge[orderIdx + 1]
                                    selectedForMerge[orderIdx + 1] = pdf.id
                                    selectedForMerge[orderIdx] = next
                                }
                            ) {
                                Icon(NokshiIcons.ArrowDownward, contentDescription = tr("Move down", "নিচে নিন"), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ToolActionTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PasswordVaultDialog(
    pdfEntity: WorkspacePdfEntity,
    onDismiss: () -> Unit,
    onSetPassword: (currentPwd: String, newPwd: String) -> Unit,
    onRemovePassword: (currentPwd: String) -> Unit
) {
    var currentPwd by remember { mutableStateOf("") }
    var newPwd by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (pdfEntity.isPasswordProtected) NokshiIcons.LockOpen else Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(tr("PDF Password Security", "PDF পাসওয়ার্ড সুরক্ষা"))
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = tr("File: ${pdfEntity.title}", "ফাইল: ${pdfEntity.title}"),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = tr(
                        "Saved into Download/ folder as a protected PDF.",
                        "সুরক্ষিত নতুন PDF সরাসরি Download ফোল্ডারে সেভ হবে।"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (pdfEntity.isPasswordProtected) {
                    OutlinedTextField(
                        value = currentPwd,
                        onValueChange = { currentPwd = it },
                        label = { Text(tr("Current Password", "বর্তমান পাসওয়ার্ড")) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = newPwd,
                    onValueChange = { newPwd = it },
                    label = { Text(tr("New Password (128-bit Encryption)", "নতুন পাসওয়ার্ড দিন (128-bit Encryption)")) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_new_password_input")
                )
                if (pdfEntity.isPasswordProtected && currentPwd.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onRemovePassword(currentPwd) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(tr("Remove Password & Save Unlocked Copy", "পাসওয়ার্ড সরিয়ে আনলক কপি তৈরি করুন"))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = newPwd.isNotBlank(),
                onClick = { onSetPassword(currentPwd, newPwd) },
                modifier = Modifier.testTag("confirm_password_lock_button")
            ) {
                Text(tr("Set Password", "পাসওয়ার্ড সেট করুন"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Cancel", "বাতিল")) }
        }
    )
}

@Composable
fun UnlockPdfPasswordDialog(
    pdfTitle: String,
    onDismiss: () -> Unit,
    onSubmitPassword: (String) -> Unit
) {
    var pwd by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Unlock Protected PDF", "সুরক্ষিত PDF খুলুন")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    tr(
                        "“$pdfTitle” is password-protected. Enter password to open:",
                        "“$pdfTitle” ফাইলটি পাসওয়ার্ড দ্বারা সুরক্ষিত। ওপেন করতে পাসওয়ার্ড লিখুন:"
                    )
                )
                OutlinedTextField(
                    value = pwd,
                    onValueChange = { pwd = it },
                    label = { Text(tr("Password", "পাসওয়ার্ড")) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("unlock_password_input")
                )
            }
        },
        confirmButton = {
            Button(
                enabled = pwd.isNotBlank(),
                onClick = { onSubmitPassword(pwd) }
            ) {
                Text(tr("Unlock", "খুলুন"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Cancel", "বাতিল")) }
        }
    )
}

@Composable
fun WriteDocumentToPdfDialog(
    onDismiss: () -> Unit,
    onCreatePdf: (title: String, content: String) -> Unit
) {
    val defaultTitle = tr("My Nokshi Document", "আমার নকশি ডকুমেন্ট")
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Write New Document to PDF", "নতুন ডকুমেন্ট লিখে PDF তৈরি করুন")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text(defaultTitle) },
                    label = { Text(tr("Document Title", "ডকুমেন্টের শিরোনাম")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("write_doc_title_input")
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text(tr("Write English or Bengali paragraphs...", "বাংলা বা ইংরেজি অনুচ্ছেদ লিখুন...")) },
                    minLines = 5,
                    maxLines = 9,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("write_doc_body_input")
                )
                Text(
                    text = tr(
                        "📁 Will be saved automatically in your Download folder.",
                        "📁 নতুন PDF ফাইলটি সরাসরি Download ফোল্ডারে সেভ হবে।"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                enabled = body.isNotBlank() || title.isNotBlank(),
                onClick = { onCreatePdf(title.trim().ifBlank { defaultTitle }, body) },
                modifier = Modifier.testTag("confirm_write_doc_pdf_button")
            ) {
                Text(tr("Create PDF", "PDF তৈরি করুন"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Cancel", "বাতিল")) }
        }
    )
}
