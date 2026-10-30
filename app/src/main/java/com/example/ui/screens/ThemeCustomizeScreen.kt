package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.UserThemeSettings
import com.example.ui.components.ColorPickerDialog
import com.example.ui.components.NokshiIcons
import com.example.ui.theme.AppColorTheme
import com.example.ui.theme.AppFontStyle
import com.example.ui.theme.ThemeMode

@Composable
fun ThemeCustomizeScreen(
    settings: UserThemeSettings,
    onSelectColorTheme: (AppColorTheme) -> Unit,
    onSelectCustomColor: (Int) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectFontStyle: (AppFontStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomColorPicker by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // Live Preview Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "কালার থিম ও ফন্ট কাস্টমাইজেশন",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "আপনার নির্বাচিত রঙ এবং ফন্ট স্টাইল পুরো Nokshi PDF অ্যাপে তাৎক্ষণিকভাবে প্রযোজ্য হবে।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Theme Mode (System / Light / Dark)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ডিসপ্লে মোড", style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = settings.themeMode == mode,
                            onClick = { onSelectThemeMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                            modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
                        ) {
                            Text(mode.labelBn)
                        }
                    }
                }
            }
        }

        // Color Theme Section
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(NokshiIcons.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("অ্যাপের কালার থিম", style = MaterialTheme.typography.titleMedium)
                }
                Button(
                    onClick = { showCustomColorPicker = true },
                    modifier = Modifier.testTag("pick_custom_theme_color_button")
                ) {
                    Icon(NokshiIcons.Colorize, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("কাস্টম রঙ")
                }
            }
        }

        items(AppColorTheme.entries) { themeOption ->
            val isSelected = settings.colorTheme == themeOption
            val primarySwatch = if (themeOption == AppColorTheme.CUSTOM) {
                Color(settings.customPrimaryArgb)
            } else {
                themeOption.previewPrimary
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (themeOption == AppColorTheme.CUSTOM) {
                            showCustomColorPicker = true
                        } else {
                            onSelectColorTheme(themeOption)
                        }
                    }
                    .testTag("color_theme_${themeOption.name.lowercase()}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(primarySwatch)
                                .border(2.dp, Color.White, CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(themeOption.previewSecondary)
                                .border(2.dp, Color.White, CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(themeOption.previewTertiary)
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(themeOption.titleBn, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = themeOption.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "সক্রিয় থিম",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Font Style Section
        item {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(NokshiIcons.FontDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("অ্যাপের ফন্ট স্টাইল", style = MaterialTheme.typography.titleMedium)
            }
        }

        items(AppFontStyle.entries) { fontOption ->
            val isSelected = settings.fontStyle == fontOption
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectFontStyle(fontOption) }
                    .testTag("font_style_${fontOption.name.lowercase()}")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fontOption.titleBn,
                            fontFamily = fontOption.displayFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = fontOption.sampleText,
                            fontFamily = fontOption.bodyFamily,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "সক্রিয় ফন্ট",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showCustomColorPicker) {
        ColorPickerDialog(
            title = "অ্যাপের কাস্টম থিম রঙ নির্বাচন",
            initialColorArgb = settings.customPrimaryArgb,
            onDismiss = { showCustomColorPicker = false },
            onColorSelected = { argb ->
                onSelectCustomColor(argb)
                showCustomColorPicker = false
            }
        )
    }
}
