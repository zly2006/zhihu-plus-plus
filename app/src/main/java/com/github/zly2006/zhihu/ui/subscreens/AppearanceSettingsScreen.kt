package com.github.zly2006.zhihu.ui.subscreens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.components.ColorPickerDialog
import com.github.zly2006.zhihu.ui.components.SwitchSettingItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences(
            PREFERENCE_NAME,
            Context.MODE_PRIVATE,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("外观与阅读体验") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                windowInsets = WindowInsets(0),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            val useDynamicColor = ThemeManager.getUseDynamicColor()
            SwitchSettingItem(
                title = "使用 Material You 动态取色",
                description = "根据系统壁纸自动提取主题色（Android 12+ 可用）\n关闭后可以自己设定主题颜色。",
                checked = useDynamicColor,
                onCheckedChange = {
                    ThemeManager.setUseDynamicColor(context, it)
                    Toast.makeText(context, "已${if (it) "启用" else "禁用"}动态取色", Toast.LENGTH_SHORT).show()
                },
            )

            var showColorPicker by remember { mutableStateOf(false) }
            val customColor = ThemeManager.getCustomColor()

            AnimatedVisibility(visible = !useDynamicColor) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showColorPicker = true }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "自定义主题色",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                "点击选择您喜欢的主题颜色",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(customColor)
                                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        )
                    }
                }
            }
            if (showColorPicker) {
                ColorPickerDialog(
                    title = "选择主题色",
                    initialColor = customColor,
                    onDismiss = { showColorPicker = false },
                    onColorSelected = { color ->
                        ThemeManager.setCustomColor(context, color)
                        Toast.makeText(context, "主题色已保存", Toast.LENGTH_SHORT).show()
                        showColorPicker = false
                    },
                )
            }

            val articleUseWebview = remember { mutableStateOf(preferences.getBoolean("articleUseWebview", true)) }
            SwitchSettingItem(
                title = "使用 WebView 显示文章",
                description = "关闭后使用 Compose 渲染，文本选择更好但格式支持较少",
                checked = articleUseWebview.value,
                onCheckedChange = {
                    articleUseWebview.value = it
                    preferences.edit { putBoolean("articleUseWebview", it) }
                },
            )

            val useWebview = remember { mutableStateOf(preferences.getBoolean("commentsUseWebview1", false)) }
            SwitchSettingItem(
                title = "使用 WebView 显示评论",
                description = "支持显示图片和富文本链接",
                checked = useWebview.value,
                onCheckedChange = {
                    useWebview.value = it
                    preferences.edit { putBoolean("commentsUseWebview1", it) }
                },
            )

            val pinWebview = remember { mutableStateOf(preferences.getBoolean("commentsPinWebview1", false)) }
            SwitchSettingItem(
                title = "评论区 WebView 对象常驻",
                description = "提高滚动流畅度，但占用更多内存",
                checked = pinWebview.value,
                onCheckedChange = {
                    pinWebview.value = it
                    preferences.edit { putBoolean("commentsPinWebview1", it) }
                },
            )

            val useHardwareAcceleration = remember { mutableStateOf(preferences.getBoolean("webviewHardwareAcceleration", true)) }
            SwitchSettingItem(
                title = "WebView 硬件加速",
                description = "提高渲染性能，可能导致兼容性问题",
                checked = useHardwareAcceleration.value,
                onCheckedChange = {
                    useHardwareAcceleration.value = it
                    preferences.edit { putBoolean("webviewHardwareAcceleration", it) }
                },
            )

            val isTitleAutoHide = remember { mutableStateOf(preferences.getBoolean("titleAutoHide", false)) }
            SwitchSettingItem(
                title = "标题栏自动隐藏",
                description = "滚动时自动隐藏标题栏",
                checked = isTitleAutoHide.value,
                onCheckedChange = {
                    isTitleAutoHide.value = it
                    preferences.edit { putBoolean("titleAutoHide", it) }
                },
            )

            val buttonSkipAnswer = remember { mutableStateOf(preferences.getBoolean("buttonSkipAnswer", true)) }
            SwitchSettingItem(
                title = "显示跳转下一个回答按钮",
                description = "在回答页面显示快速跳转按钮",
                checked = buttonSkipAnswer.value,
                onCheckedChange = {
                    buttonSkipAnswer.value = it
                    preferences.edit { putBoolean("buttonSkipAnswer", it) }
                },
            )

            Text(
                "底部栏设置",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            )

            val defaultKeys = setOf("Home", "Follow", "Daily", "OnlineHistory", "Account")
            val selectedKeys = remember { mutableStateOf(preferences.getStringSet("bottom_bar_items", defaultKeys) ?: defaultKeys) }
            val allItems = listOf(
                "Home" to "主页",
                "Follow" to "关注",
                "HotList" to "热榜",
                "Daily" to "日报",
                "OnlineHistory" to "历史",
                "Account" to "账号设置",
            )

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "选择要在底部栏显示的页面（3-5项）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                allItems.forEach { (key, label) ->
                    val isChecked = selectedKeys.value.contains(key)
                    val isEnabled = key != "Account"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isEnabled) {
                                val currentSet = selectedKeys.value.toMutableSet()
                                if (isChecked) {
                                    if (currentSet.size > 3) {
                                        currentSet.remove(key)
                                        selectedKeys.value = currentSet
                                        preferences.edit { putStringSet("bottom_bar_items", currentSet) }
                                    } else {
                                        Toast.makeText(context, "至少保留3项", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    if (currentSet.size < 5) {
                                        currentSet.add(key)
                                        selectedKeys.value = currentSet
                                        preferences.edit { putStringSet("bottom_bar_items", currentSet) }
                                    } else {
                                        Toast.makeText(context, "最多选择5项", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        )
                        androidx.compose.material3.Checkbox(
                            checked = isChecked,
                            onCheckedChange = null, // Handled by Row click
                            enabled = isEnabled,
                        )
                    }
                }
            }

            Text(
                "阅读设置",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
            )

            val fontSize = remember { mutableStateOf(preferences.getInt("webviewFontSize", 100)) }
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("字号", style = MaterialTheme.typography.bodyLarge)
                    Text("${fontSize.value}%", style = MaterialTheme.typography.bodyMedium)
                }
                Slider(
                    value = fontSize.value.toFloat(),
                    onValueChange = {
                        fontSize.value = it.toInt()
                        preferences.edit { putInt("webviewFontSize", it.toInt()) }
                    },
                    valueRange = 50f..200f,
                    steps = 14,
                )
            }

            val lineHeight = remember { mutableStateOf(preferences.getInt("webviewLineHeight", 160)) }
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("行高", style = MaterialTheme.typography.bodyLarge)
                    Text("${lineHeight.value / 100f}", style = MaterialTheme.typography.bodyMedium)
                }
                Slider(
                    value = lineHeight.value.toFloat(),
                    onValueChange = {
                        lineHeight.value = it.toInt()
                        preferences.edit { putInt("webviewLineHeight", it.toInt()) }
                    },
                    valueRange = 100f..300f,
                    steps = 19,
                )
            }
        }
    }
}
