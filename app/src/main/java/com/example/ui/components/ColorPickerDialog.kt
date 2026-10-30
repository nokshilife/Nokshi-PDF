package com.example.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

val NokshiPresetColors = listOf(
    0xFF8E1D2A.toInt(), // Nokshi Maroon
    0xFF1F3A60.toInt(), // Royal Indigo
    0xFFD49B35.toInt(), // Marigold Gold
    0xFF0F6E56.toInt(), // Bengal Emerald
    0xFFFFEB3B.toInt(), // Classic Yellow Highlighter
    0xFF69F0AE.toInt(), // Mint Highlighter
    0xFF40C4FF.toInt(), // Sky Cyan Highlighter
    0xFFFF80AB.toInt(), // Pink Rose Highlighter
    0xFFE53935.toInt(), // Crimson Red
    0xFF1E88E5.toInt(), // Signature Blue
    0xFF6A1B9A.toInt(), // Deep Violet
    0xFFFB8C00.toInt(), // Sunset Orange
    0xFF1A1A1A.toInt(), // Ink Black
    0xFFFFFFFF.toInt()  // Paper White
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(
    title: String = "কালার পিকার (রঙ নির্বাচন করুন)",
    initialColorArgb: Int,
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit
) {
    val hsvInit = remember(initialColorArgb) {
        FloatArray(3).also { AndroidColor.colorToHSV(initialColorArgb, it) }
    }
    var hue by remember { mutableFloatStateOf(hsvInit[0]) }
    var saturation by remember { mutableFloatStateOf(hsvInit[1]) }
    var value by remember { mutableFloatStateOf(hsvInit[2]) }

    val currentArgb = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))
    val currentColor = Color(currentArgb)
    var hexInput by remember(currentArgb) {
        mutableStateOf(String.format("#%06X", 0xFFFFFF and currentArgb))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Preview + Hex Field
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(currentColor)
                            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { raw ->
                            hexInput = raw
                            val cleaned = raw.trim().removePrefix("#")
                            if (cleaned.length == 6) {
                                runCatching {
                                    val parsed = AndroidColor.parseColor("#$cleaned")
                                    val hsv = FloatArray(3)
                                    AndroidColor.colorToHSV(parsed, hsv)
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    value = hsv[2]
                                }
                            }
                        },
                        label = { Text("Hex কোড (#RRGGBB)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("color_hex_input")
                    )
                }

                // Hue Rainbow Bar + Slider
                Text("হিউ (রঙের ধারা)", style = MaterialTheme.typography.labelMedium)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Red,
                                    Color.Yellow,
                                    Color.Green,
                                    Color.Cyan,
                                    Color.Blue,
                                    Color.Magenta,
                                    Color.Red
                                )
                            )
                        )
                )
                Slider(
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f,
                    modifier = Modifier.testTag("hue_slider")
                )

                Text("গাঢ়ত্ব (Saturation)", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = saturation,
                    onValueChange = { saturation = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.testTag("saturation_slider")
                )

                Text("উজ্জ্বলতা (Brightness)", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.testTag("brightness_slider")
                )

                Text("দ্রুত রঙ নির্বাচন (Swatches)", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NokshiPresetColors.forEach { presetArgb ->
                        val c = Color(presetArgb)
                        val selected = (presetArgb and 0xFFFFFF) == (currentArgb and 0xFFFFFF)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (selected) 2.5.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = CircleShape
                                )
                                .clickable {
                                    val hsv = FloatArray(3)
                                    AndroidColor.colorToHSV(presetArgb, hsv)
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    value = hsv[2]
                                }
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "নির্বাচিত রঙ",
                                    tint = if (c.luminance() > 0.5f) Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onColorSelected(currentColor.toArgb()) },
                modifier = Modifier.testTag("confirm_color_button")
            ) {
                Text("রঙ নির্বাচন করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
