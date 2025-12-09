package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.ajalt.colormath.model.RGB
import kotlin.math.roundToInt

@Composable
fun ColorPickerDialog(
    title: String = "选择颜色",
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
) {
    val rgb = RGB(
        initialColor.red,
        initialColor.green,
        initialColor.blue,
    )

    var red by remember { mutableFloatStateOf(rgb.r) }
    var green by remember { mutableFloatStateOf(rgb.g) }
    var blue by remember { mutableFloatStateOf(rgb.b) }

    val currentColor = Color(red, green, blue)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(currentColor)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                )

                Spacer(modifier = Modifier.height(16.dp))

                ColorSlider(
                    label = "红色",
                    value = red,
                    color = Color.Red,
                    onValueChange = { red = it },
                )

                ColorSlider(
                    label = "绿色",
                    value = green,
                    color = Color.Green,
                    onValueChange = { green = it },
                )

                ColorSlider(
                    label = "蓝色",
                    value = blue,
                    color = Color.Blue,
                    onValueChange = { blue = it },
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "RGB: (${(red * 255).roundToInt()}, ${(green * 255).roundToInt()}, ${(blue * 255).roundToInt()})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("预设颜色", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    PresetColor(Color(0xFF2196F3)) {
                        // Blue
                        red = it.red
                        green = it.green
                        blue = it.blue
                    }
                    PresetColor(Color(0xFF4CAF50)) {
                        // Green
                        red = it.red
                        green = it.green
                        blue = it.blue
                    }
                    PresetColor(Color(0xFFF44336)) {
                        // Red
                        red = it.red
                        green = it.green
                        blue = it.blue
                    }
                    PresetColor(Color(0xFFFF9800)) {
                        // Orange
                        red = it.red
                        green = it.green
                        blue = it.blue
                    }
                    PresetColor(Color(0xFF9C27B0)) {
                        // Purple
                        red = it.red
                        green = it.green
                        blue = it.blue
                    }
                    PresetColor(Color(0xFF607D8B)) {
                        // Grey
                        red = it.red
                        green = it.green
                        blue = it.blue
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(currentColor) }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            label,
            modifier = Modifier.width(50.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
        )
        Text(
            "${(value * 255).roundToInt()}",
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun PresetColor(
    color: Color,
    onSelect: (Color) -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .clickable { onSelect(color) },
    )
}
