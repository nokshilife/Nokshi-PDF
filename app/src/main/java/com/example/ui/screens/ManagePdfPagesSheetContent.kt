package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.engine.LazyPdfSession
import com.example.engine.PageOperationSpec
import com.example.ui.components.NokshiIcons
import com.example.ui.i18n.tr

@Composable
fun ManagePdfPagesSheetContent(
    session: LazyPdfSession,
    initialFocusPageIndex: Int,
    onDismiss: () -> Unit,
    onApplyChanges: (List<PageOperationSpec>) -> Unit
) {
    val totalOriginalPages = session.pageCount.coerceAtLeast(1)
    val pageSpecs = remember(totalOriginalPages) {
        mutableStateListOf<PageOperationSpec>().apply {
            for (idx in 0 until totalOriginalPages) {
                add(PageOperationSpec(originalPageIndex = idx, additionalRotationDeg = 0))
            }
        }
    }

    val hasModifications = pageSpecs.size != totalOriginalPages ||
        pageSpecs.withIndex().any { (i, spec) ->
            spec.originalPageIndex != i || (spec.additionalRotationDeg % 360) != 0
        }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr("Manage Pages (Reorder • Rotate • Delete)", "পেজ ম্যানেজার (আগে-পিছে • রোটেশন • ডিলিট)"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tr(
                        "${pageSpecs.size} of $totalOriginalPages pages kept • Active Page: ${initialFocusPageIndex + 1}",
                        "মোট $totalOriginalPages পেজের মধ্যে ${pageSpecs.size}টি পেজ রাখা হয়েছে • নির্বাচিত পেজ: ${initialFocusPageIndex + 1}"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("manage_pages_close_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = tr("Close", "বন্ধ করুন"))
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 440.dp)
                .testTag("manage_pages_list")
        ) {
            itemsIndexed(pageSpecs, key = { _, spec -> spec.id }) { currentIndex, spec ->
                val isInitialFocus = spec.originalPageIndex == initialFocusPageIndex
                PageOrganizerItemCard(
                    session = session,
                    currentNumber = currentIndex + 1,
                    spec = spec,
                    isHighlighted = isInitialFocus,
                    canMoveEarlier = currentIndex > 0,
                    canMoveLater = currentIndex < pageSpecs.lastIndex,
                    canDelete = pageSpecs.size > 1,
                    onMoveEarlier = {
                        if (currentIndex > 0) {
                            val prev = pageSpecs[currentIndex - 1]
                            pageSpecs[currentIndex - 1] = spec
                            pageSpecs[currentIndex] = prev
                        }
                    },
                    onMoveLater = {
                        if (currentIndex < pageSpecs.lastIndex) {
                            val next = pageSpecs[currentIndex + 1]
                            pageSpecs[currentIndex + 1] = spec
                            pageSpecs[currentIndex] = next
                        }
                    },
                    onRotate90 = {
                        val nextRot = (spec.additionalRotationDeg + 90) % 360
                        pageSpecs[currentIndex] = spec.copy(additionalRotationDeg = nextRot)
                    },
                    onDelete = {
                        if (pageSpecs.size > 1) {
                            pageSpecs.removeAt(currentIndex)
                        }
                    }
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    pageSpecs.clear()
                    for (idx in 0 until totalOriginalPages) {
                        pageSpecs.add(PageOperationSpec(originalPageIndex = idx, additionalRotationDeg = 0))
                    }
                },
                enabled = hasModifications,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("manage_pages_reset_button")
            ) {
                Icon(NokshiIcons.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(tr("Reset", "রিসেট"))
            }

            Button(
                onClick = { onApplyChanges(pageSpecs.toList()) },
                enabled = hasModifications && pageSpecs.isNotEmpty(),
                modifier = Modifier
                    .weight(1.4f)
                    .height(48.dp)
                    .testTag("manage_pages_apply_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(tr("Apply & Save PDF", "সেভ ও অ্যাপ্লাই করুন"))
            }
        }
    }
}

@Composable
private fun PageOrganizerItemCard(
    session: LazyPdfSession,
    currentNumber: Int,
    spec: PageOperationSpec,
    isHighlighted: Boolean,
    canMoveEarlier: Boolean,
    canMoveLater: Boolean,
    canDelete: Boolean,
    onMoveEarlier: () -> Unit,
    onMoveLater: () -> Unit,
    onRotate90: () -> Unit,
    onDelete: () -> Unit
) {
    var thumbBitmap by remember(session, spec.originalPageIndex) {
        mutableStateOf(session.getCachedBitmap(spec.originalPageIndex))
    }
    var ratio by remember(session, spec.originalPageIndex) {
        mutableFloatStateOf(session.getCachedAspectRatio(spec.originalPageIndex))
    }

    LaunchedEffect(session, spec.originalPageIndex) {
        ratio = session.getOrLoadAspectRatio(spec.originalPageIndex)
        if (thumbBitmap == null) {
            thumbBitmap = session.renderPage(spec.originalPageIndex, 720)
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isHighlighted) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                } else Modifier
            )
            .testTag("organizer_page_card_${spec.originalPageIndex}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(60.dp)
                    .aspectRatio(ratio.coerceIn(0.45f, 1.8f))
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            ) {
                val bmp = thumbBitmap
                if (bmp == null) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Image(
                        bitmap = remember(bmp) { bmp.asImageBitmap() },
                        contentDescription = tr("Page ${spec.originalPageIndex + 1} preview", "পেজ ${spec.originalPageIndex + 1} প্রিভিউ"),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                rotationZ = spec.additionalRotationDeg.toFloat()
                            }
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tr("Page #$currentNumber", "পেজ #$currentNumber"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (spec.originalPageIndex + 1 != currentNumber) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = tr("Orig ${spec.originalPageIndex + 1}", "মূল ${spec.originalPageIndex + 1}"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (spec.additionalRotationDeg != 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "+${spec.additionalRotationDeg}°",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = if (isHighlighted) {
                        tr("Double-tapped target page", "ডাবল ট্যাপ করা পেজ")
                    } else {
                        tr("Use arrows to move, rotate 90°, or delete", "আগে-পিছে নিন, ৯০° ঘোরান বা ডিলিট করুন")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action Buttons (each >= 48dp touch target via IconButton defaults)
            IconButton(
                onClick = onMoveEarlier,
                enabled = canMoveEarlier,
                modifier = Modifier.testTag("page_move_up_${spec.originalPageIndex}")
            ) {
                Icon(
                    imageVector = NokshiIcons.ArrowUpward,
                    contentDescription = tr("Move page earlier", "পেজ আগে নিন")
                )
            }
            IconButton(
                onClick = onMoveLater,
                enabled = canMoveLater,
                modifier = Modifier.testTag("page_move_down_${spec.originalPageIndex}")
            ) {
                Icon(
                    imageVector = NokshiIcons.ArrowDownward,
                    contentDescription = tr("Move page later", "পেজ পরে নিন")
                )
            }
            IconButton(
                onClick = onRotate90,
                modifier = Modifier.testTag("page_rotate_${spec.originalPageIndex}")
            ) {
                Icon(
                    imageVector = NokshiIcons.RotateRight,
                    contentDescription = tr("Rotate page 90 degrees", "পেজ ৯০ ডিগ্রি ঘোরান"),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = onDelete,
                enabled = canDelete,
                modifier = Modifier.testTag("page_delete_${spec.originalPageIndex}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = tr("Delete page", "পেজ মুছুন"),
                    tint = if (canDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
