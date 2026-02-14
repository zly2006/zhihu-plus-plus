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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.github.zly2006.zhihu.Account
import com.github.zly2006.zhihu.Daily
import com.github.zly2006.zhihu.Follow
import com.github.zly2006.zhihu.Home
import com.github.zly2006.zhihu.HotList
import com.github.zly2006.zhihu.OnlineHistory
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.ThemeMode
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.components.ColorPickerDialog
import com.github.zly2006.zhihu.ui.components.SwitchSettingItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    setting: String = "",
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences(
            PREFERENCE_NAME,
            Context.MODE_PRIVATE,
        )
    }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // 用于存储各个设置项的位置
    var shareActionPosition by remember { mutableIntStateOf(0) }

    // 当 setting 参数不为空时，滚动到指定位置
    LaunchedEffect(setting, shareActionPosition) {
        if (setting.isNotEmpty() && shareActionPosition > 0) {
            when (setting) {
                "shareAction" -> {
                    kotlinx.coroutines.delay(100) // 等待布局完成
                    scrollState.animateScrollTo(shareActionPosition)
                }
            }
        }
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
                .verticalScroll(scrollState),
        ) {
            val useDynamicColor = ThemeManager.getUseDynamicColor()
            val currentThemeMode = ThemeManager.getThemeMode()

            Text(
                "主题设置",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )

            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    "主题模式",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val themeModes = listOf(
                        ThemeMode.SYSTEM to "跟随系统",
                        ThemeMode.LIGHT to "亮色",
                        ThemeMode.DARK to "暗色",
                    )
                    themeModes.forEach { (mode, label) ->
                        OutlinedButton(
                            onClick = {
                                ThemeManager.setThemeMode(context, mode)
                                Toast.makeText(context, "已切换到$label", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (currentThemeMode == mode) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                            ),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(label)
                        }
                    }
                }
            }

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

            var showLuotianYiColorPicker by remember { mutableStateOf(false) }
            val luotianYiColor = remember {
                Color(preferences.getInt("luotianyi_color", 0xff_66CCFF.toInt()))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLuotianYiColorPicker = true }
                    .padding(vertical = 12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "唤起浏览器主题色",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        "在应用内浏览器的工具栏颜色",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(luotianYiColor)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                )
            }

            if (showLuotianYiColorPicker) {
                ColorPickerDialog(
                    title = "选择浏览器主题色",
                    initialColor = luotianYiColor,
                    presetColors = listOf(
                        Color(0xFF66CCFF),
                        Color(0xFF2196F3),
                        Color(0xFF4CAF50),
                        Color(0xFFF44336),
                        Color(0xFFFF9800),
                        Color(0xFF9C27B0),
                    ),
                    onDismiss = { showLuotianYiColorPicker = false },
                    onColorSelected = { color ->
                        val argbColor = android.graphics.Color.argb(
                            (color.alpha * 255).toInt(),
                            (color.red * 255).toInt(),
                            (color.green * 255).toInt(),
                            (color.blue * 255).toInt(),
                        )
                        preferences.edit { putInt("luotianyi_color", argbColor) }
                        Toast.makeText(context, "浏览器主题色已保存", Toast.LENGTH_SHORT).show()
                        showLuotianYiColorPicker = false
                    },
                )
            }

            val currentIsDarkTheme = ThemeManager.isDarkTheme()
            val defaultBackgroundColor = if (currentIsDarkTheme) 0xFF121212.toInt() else 0xFFFFFFFF.toInt()
            var showBackgroundColorPicker by remember { mutableStateOf(false) }
            val backgroundColor = ThemeManager.getBackgroundColor()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showBackgroundColorPicker = true }
                    .padding(vertical = 12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "背景颜色",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        if (currentIsDarkTheme) "深色模式背景色" else "浅色模式背景色",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(backgroundColor)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                )
            }

            if (showBackgroundColorPicker) {
                ColorPickerDialog(
                    title = "选择背景颜色",
                    initialColor = backgroundColor,
                    presetColors = listOf(
                        Color(defaultBackgroundColor),
                    ),
                    onDismiss = { showBackgroundColorPicker = false },
                    onColorSelected = { color ->
                        ThemeManager.setBackgroundColor(context, color, currentIsDarkTheme)
                        Toast.makeText(context, "背景颜色已保存", Toast.LENGTH_SHORT).show()
                        showBackgroundColorPicker = false
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

            val showFeedThumbnail = remember { mutableStateOf(preferences.getBoolean("showFeedThumbnail", true)) }
            SwitchSettingItem(
                title = "显示 Feed 卡片缩略图",
                description = "在信息流卡片中显示文章缩略图",
                checked = showFeedThumbnail.value,
                onCheckedChange = {
                    showFeedThumbnail.value = it
                    preferences.edit { putBoolean("showFeedThumbnail", it) }
                },
            )

            // 分享操作设置
            var shareActionExpanded by remember { mutableStateOf(false) }
            val shareActionMode = remember {
                mutableStateOf(preferences.getString("shareActionMode", "ask") ?: "ask")
            }
            val shareActionOptions = listOf(
                "ask" to "询问",
                "copy" to "复制链接",
                "share" to "Android分享",
            )

            Column(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .onGloballyPositioned { coordinates ->
                        // 存储设置项的 Y 位置
                        shareActionPosition = coordinates.size.height
                    },
            ) {
                Text(
                    "分享操作",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    "点击分享按钮时的默认行为",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                ExposedDropdownMenuBox(
                    expanded = shareActionExpanded,
                    onExpandedChange = { shareActionExpanded = it },
                ) {
                    OutlinedTextField(
                        value = shareActionOptions.find { it.first == shareActionMode.value }?.second ?: "询问",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shareActionExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = shareActionExpanded,
                        onDismissRequest = { shareActionExpanded = false },
                    ) {
                        shareActionOptions.forEach { (mode, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    shareActionMode.value = mode
                                    preferences.edit { putString("shareActionMode", mode) }
                                    shareActionExpanded = false
                                    Toast.makeText(context, "已设置为：$label", Toast.LENGTH_SHORT).show()
                                },
                            )
                        }
                    }
                }
            }

            Text(
                "底部栏设置",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )

            val defaultKeys = setOf(Home.name, Follow.name, Daily.name, OnlineHistory.name, Account.name)
            val selectedKeys = remember { mutableStateOf(preferences.getStringSet("bottom_bar_items", defaultKeys) ?: defaultKeys) }
            val allItems = listOf(
                Home.name to "主页",
                Follow.name to "关注",
                HotList.name to "热榜",
                Daily.name to "日报",
                OnlineHistory.name to "历史",
                Account.name to "账号设置",
            )

            Column {
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
                            }.padding(vertical = 12.dp, horizontal = 16.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        )
                        Checkbox(
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
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )

            var fontSize by remember { mutableIntStateOf(preferences.getInt("webviewFontSize", 100)) }
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("字号", style = MaterialTheme.typography.bodyLarge)
                    Text("$fontSize%", style = MaterialTheme.typography.bodyMedium)
                }
                Slider(
                    value = fontSize.toFloat(),
                    onValueChange = {
                        fontSize = it.toInt()
                        preferences.edit { putInt("webviewFontSize", it.toInt()) }
                    },
                    valueRange = 50f..200f,
                    steps = 14,
                )
            }

            var lineHeight by remember { mutableIntStateOf(preferences.getInt("webviewLineHeight", 160)) }
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("行高", style = MaterialTheme.typography.bodyLarge)
                    Text("${lineHeight / 100f}", style = MaterialTheme.typography.bodyMedium)
                }
                Slider(
                    value = lineHeight.toFloat(),
                    onValueChange = {
                        lineHeight = it.toInt()
                        preferences.edit { putInt("webviewLineHeight", it.toInt()) }
                    },
                    valueRange = 100f..300f,
                    steps = 19,
                )
            }

            Text(
                "导航设置",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )

            val useCustomNavHost = remember { mutableStateOf(preferences.getBoolean("use_custom_nav_host", true)) }
            SwitchSettingItem(
                title = "使用自定义导航",
                description = "使用自定义导航替代系统默认的导航组件，可能部分提升国产手机上的操作手感，请视情况开启。",
                checked = useCustomNavHost.value,
                onCheckedChange = {
                    useCustomNavHost.value = it
                    preferences.edit { putBoolean("use_custom_nav_host", it) }
                    Toast.makeText(context, "需要重启应用生效", Toast.LENGTH_SHORT).show()
                },
            )

            val enablePredictiveBack = remember { mutableStateOf(preferences.getBoolean("enable_predictive_back", true)) }
            SwitchSettingItem(
                title = "启用预测性返回",
                description = "开启 Android 14+ 的预测性返回手势动画",
                checked = enablePredictiveBack.value,
                onCheckedChange = {
                    enablePredictiveBack.value = it
                    preferences.edit { putBoolean("enable_predictive_back", it) }
                },
            )
        }
    }
}
