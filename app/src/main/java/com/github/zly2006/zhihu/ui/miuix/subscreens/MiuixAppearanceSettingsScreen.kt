/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.subscreens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.ThemeMode
import com.github.zly2006.zhihu.theme.ThemeStyle
import com.github.zly2006.zhihu.ui.miuix.components.MiuixColorPickerSheet
import com.github.zly2006.zhihu.ui.miuix.components.MiuixExpandableArrowPreference
import com.github.zly2006.zhihu.ui.miuix.components.MiuixMultiSelectExpandable
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.subscreens.normalizeBottomBarSelection
import com.github.zly2006.zhihu.ui.subscreens.resolveValidStartDestinationKey
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

// Constants shared with M3 version
private const val BOTTOM_BAR_ITEMS_PREFERENCE_KEY = "bottom_bar_items"
private const val START_DESTINATION_PREFERENCE_KEY = "startDestination"
private const val PREF_FONT_SIZE = "contentFontSize"
private const val PREF_LINE_HEIGHT = "contentLineHeight"

@Composable
fun MiuixAppearanceSettingsScreen(
    @Suppress("UNUSED_PARAMETER") setting: String = "",
    onExit: () -> Unit = {},
) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE) }
    val navigator = LocalNavigator.current
    val blurEnabled = remember { mutableStateOf(preferences.getBoolean("blurEnabled", true)) }
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled.value)
    val scrollBehavior = MiuixScrollBehavior()

    // Theme state
    val themeStyle = ThemeManager.getThemeStyle()
    val themeMode = ThemeManager.getThemeMode()
    val useDynamicColor = ThemeManager.getUseDynamicColor()
    val customColor = ThemeManager.getCustomColor()
    val isDark = ThemeManager.isDarkTheme()
    val bgColor = ThemeManager.getBackgroundColor()

    // 阅读
    var fontSize by remember { mutableIntStateOf(preferences.getInt(PREF_FONT_SIZE, 100)) }
    var lineHeight by remember { mutableIntStateOf(preferences.getInt(PREF_LINE_HEIGHT, 160)) }
    var showFontSlider by remember { mutableStateOf(false) }
    var showLineSlider by remember { mutableStateOf(false) }

    // 信息流
    val showFeedThumbnail = remember { mutableStateOf(preferences.getBoolean("showFeedThumbnail", true)) }
    val showRefreshFab = remember { mutableStateOf(preferences.getBoolean("showRefreshFab", true)) }
    val feedCardStyle = remember { mutableStateOf(preferences.getString("feedCardStyle", "card") ?: "card") }

    // 回答页
    val articleUseWebview = remember { mutableStateOf(preferences.getBoolean("articleUseWebview", false)) }
    val titleAutoHide = remember { mutableStateOf(preferences.getBoolean("titleAutoHide", false)) }
    val autoHideBottomBar = remember { mutableStateOf(preferences.getBoolean("autoHideArticleBottomBar", false)) }
    val buttonSkipAnswer = remember { mutableStateOf(preferences.getBoolean("buttonSkipAnswer", true)) }
    val autoHideSkipBtn = remember { mutableStateOf(preferences.getBoolean("autoHideSkipAnswerButton", true)) }
    val pinAnswerDate = remember { mutableStateOf(preferences.getBoolean("pinAnswerDate", false)) }
    val answerSwitchMode = remember { mutableStateOf(preferences.getString("answerSwitchMode", "vertical") ?: "vertical") }
    val answerDoubleTap = remember { mutableStateOf(preferences.getString("answerDoubleTapAction", "ask") ?: "ask") }

    // 底部栏
    val startDestinationKey = remember { mutableStateOf(preferences.getString(START_DESTINATION_PREFERENCE_KEY, "Home") ?: "Home") }
    val selectedBottomBarKeys = remember {
        mutableStateOf(
            preferences.getStringSet(BOTTOM_BAR_ITEMS_PREFERENCE_KEY, null)
                ?.toSet() ?: setOf("Home", "Follow", "Daily", "OnlineHistory", "Account"),
        )
    }
    val tapToRefresh = remember { mutableStateOf(preferences.getBoolean("bottomBarTapScrollToTop", true)) }
    val autoHideNavBar = remember { mutableStateOf(preferences.getBoolean("autoHideBottomBar", false)) }

    // 搜索
    val showHotSearch = remember { mutableStateOf(preferences.getBoolean("showSearchHotSearch", true)) }
    val showSearchHistory = remember { mutableStateOf(preferences.getBoolean("showSearchHistory", true)) }

    // 导航
    val useCustomNav = remember { mutableStateOf(preferences.getBoolean("use_custom_nav_host", true)) }
    val enablePredictiveBack = remember { mutableStateOf(preferences.getBoolean("enable_predictive_back", true)) }

    // 123Duo3
    val duo3All = remember { mutableStateOf(preferences.getBoolean("duo3_all", false)) }
    val duo3HomeAccount = remember { mutableStateOf(preferences.getBoolean("duo3_home_account", false)) }
    val duo3NavStyle = remember { mutableStateOf(preferences.getBoolean("duo3_nav_style", false)) }
    val duo3CardAppearance = remember { mutableStateOf(preferences.getBoolean("duo3_card_appearance", false)) }
    val duo3CardLayout = remember { mutableStateOf(preferences.getBoolean("duo3_card_layout", false)) }
    val duo3ArticleBar = remember { mutableStateOf(preferences.getBoolean("duo3_article_bar", false)) }
    val duo3ArticleActions = remember { mutableStateOf(preferences.getBoolean("duo3_article_actions", false)) }

    // Color picker state (MutableState ref for WindowBottomSheet pattern)
    val showColorPicker = remember { mutableStateOf(false) }
    val showBgPicker = remember { mutableStateOf(false) }

    // 底栏选择规范化（对齐 M3 persistBottomBarSelection）：
    // duo3_home_account 关闭时强制保留「账号」入口，否则账号设置无处可进。
    fun persistBottomBar(currentSet: Set<String>, duo3Enabled: Boolean) {
        val normalized = normalizeBottomBarSelection(currentSet, duo3Enabled, enforceMinimumSelection = true)
        val available = listOf("Home", "Follow", "HotList", "Daily", "OnlineHistory", "Account").filter { it in normalized }
        val resolvedStart = resolveValidStartDestinationKey(startDestinationKey.value, available)
        selectedBottomBarKeys.value = normalized
        startDestinationKey.value = resolvedStart
        preferences.edit {
            putStringSet(BOTTOM_BAR_ITEMS_PREFERENCE_KEY, normalized)
            putString(START_DESTINATION_PREFERENCE_KEY, resolvedStart)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(backdrop),
                color = backdrop.getMiuixAppBarColor(),
                title = "外观",
                navigationIcon = {
                    IconButton(onClick = { onExit(); navigator.onNavigateBack() }) {
                        Icon(MiuixIconsEmbedded.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = innerPadding,
        ) {
            item { Spacer(Modifier.size(12.dp)) }

            // ===== 界面风格 =====
            item { SmallTitle(text = "界面风格") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    UiEngineSpinner(themeStyle) { ThemeManager.setThemeStyle(context, it) }
                    SwitchPreference(
                        checked = blurEnabled.value,
                        onCheckedChange = { blurEnabled.value = it; preferences.edit { putBoolean("blurEnabled", it) } },
                        title = "毛玻璃效果",
                        summary = "顶栏和底栏使用半透明模糊背景（Android 12+ 可用）",
                    )
                }
            }

            // ===== 主题 =====
            item { SmallTitle(text = "主题") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    ThemeModeSpinner(themeMode) { ThemeManager.setThemeMode(context, it) }
                    SwitchPreference(
                        checked = useDynamicColor, onCheckedChange = { ThemeManager.setUseDynamicColor(context, it) },
                        title = "动态颜色", summary = "跟随系统取色（Android 12 及以上）",
                    )
                    if (!useDynamicColor) {
                        ArrowPreference(
                            title = "自定义主题色", summary = "点击选择您喜欢的主题颜色",
                            onClick = { showColorPicker.value = true },
                            endActions = { ColorCircle(customColor) },
                        )
                    }
                    ArrowPreference(
                        title = "自定义背景颜色",
                        summary = if (isDark) "深色模式背景色" else "浅色模式背景色",
                        onClick = { showBgPicker.value = true },
                        endActions = { ColorCircle(bgColor) },
                    )
                }
            }

            // ===== 阅读 =====
            item { SmallTitle(text = "阅读") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    MiuixExpandableArrowPreference(
                        title = "字号", summary = "调整内容文字大小 ($fontSize%)",
                        expanded = showFontSlider,
                        onExpandedChange = { showFontSlider = !showFontSlider },
                    ) {
                        SliderRow(fontSize.toFloat(), 50f..200f, 14) { fontSize = it; preferences.edit { putInt(PREF_FONT_SIZE, it) } }
                    }
                    MiuixExpandableArrowPreference(
                        title = "行高", summary = "调整内容行间距 (${lineHeight / 100f})",
                        expanded = showLineSlider,
                        onExpandedChange = { showLineSlider = !showLineSlider },
                    ) {
                        SliderRow(lineHeight.toFloat(), 100f..300f, 19) { lineHeight = it; preferences.edit { putInt(PREF_LINE_HEIGHT, it) } }
                    }
                }
            }

            // ===== 信息流 =====
            item { SmallTitle(text = "信息流") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    SwitchPreference(
                        checked = showFeedThumbnail.value, onCheckedChange = { showFeedThumbnail.value = it; preferences.edit { putBoolean("showFeedThumbnail", it) } },
                        title = "显示卡片缩略图", summary = "在信息流卡片中显示文章缩略图",
                    )
                    SwitchPreference(
                        checked = showRefreshFab.value, onCheckedChange = { showRefreshFab.value = it; preferences.edit { putBoolean("showRefreshFab", it) } },
                        title = "显示刷新 FAB 按钮", summary = "在页面上显示可拖动的刷新按钮",
                    )
                    FeedCardStyleSpinner(feedCardStyle.value) { feedCardStyle.value = it; preferences.edit { putString("feedCardStyle", it) } }
                }
            }

            // ===== 回答页 =====
            item { SmallTitle(text = "回答页") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    SwitchPreference(
                        checked = articleUseWebview.value, onCheckedChange = { articleUseWebview.value = it; preferences.edit { putBoolean("articleUseWebview", it) } },
                        title = "使用 WebView 显示文章", summary = "关闭后使用 Compose 渲染，支持代码高亮",
                    )
                    SwitchPreference(
                        checked = titleAutoHide.value, onCheckedChange = { titleAutoHide.value = it; preferences.edit { putBoolean("titleAutoHide", it) } },
                        title = "自动隐藏回答标题", summary = "滚动时自动隐藏回答标题栏",
                    )
                    SwitchPreference(
                        checked = autoHideBottomBar.value, onCheckedChange = { autoHideBottomBar.value = it; preferences.edit { putBoolean("autoHideArticleBottomBar", it) } },
                        title = "自动隐藏回答底部按钮", summary = "上划时隐藏，下划时重新显示",
                    )
                    SwitchPreference(
                        checked = buttonSkipAnswer.value, onCheckedChange = { buttonSkipAnswer.value = it; preferences.edit { putBoolean("buttonSkipAnswer", it) } },
                        title = "显示跳转下一个回答按钮", summary = "在回答页显示可拖动快速跳转按钮",
                    )
                    if (buttonSkipAnswer.value) {
                        SwitchPreference(
                            checked = autoHideSkipBtn.value, onCheckedChange = { autoHideSkipBtn.value = it; preferences.edit { putBoolean("autoHideSkipAnswerButton", it) } },
                            title = "滚动时自动隐藏跳转按钮", summary = "上划时淡出，下划时淡入",
                        )
                    }
                    SwitchPreference(
                        checked = pinAnswerDate.value, onCheckedChange = { pinAnswerDate.value = it; preferences.edit { putBoolean("pinAnswerDate", it) } },
                        title = "置顶回答日期", summary = "将回答日期移到内容最前面显示",
                    )
                    AnswerSwitchModeSpinner(answerSwitchMode.value) { answerSwitchMode.value = it; preferences.edit { putString("answerSwitchMode", it) } }
                    AnswerDoubleTapSpinner(answerDoubleTap.value) { answerDoubleTap.value = it; preferences.edit { putString("answerDoubleTapAction", it) } }
                }
            }

            // ===== 底部导航栏 =====
            item { SmallTitle(text = "底部导航栏") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    StartDestinationSpinner(startDestinationKey.value, selectedBottomBarKeys.value) {
                        startDestinationKey.value = it; preferences.edit { putString(START_DESTINATION_PREFERENCE_KEY, it) }
                        Toast.makeText(context, "重启后生效", Toast.LENGTH_SHORT).show()
                    }
                    MiuixMultiSelectExpandable(
                        title = "选择显示的页面",
                        options = listOf("Home", "Follow", "HotList", "Daily", "OnlineHistory", "Account"),
                        optionLabel = { mapOf("Home" to "主页", "Follow" to "关注", "HotList" to "热榜", "Daily" to "日报", "OnlineHistory" to "历史", "Account" to "账号设置")[it] ?: it },
                        selectedOptions = selectedBottomBarKeys.value,
                        onSelectionChange = { persistBottomBar(it, duo3HomeAccount.value) },
                    )
                    SwitchPreference(
                        checked = tapToRefresh.value, onCheckedChange = { tapToRefresh.value = it; preferences.edit { putBoolean("bottomBarTapScrollToTop", it) } },
                        title = "点击导航栏回到顶部/刷新", summary = "点击当前页按钮回到顶部，已在顶部则刷新",
                    )
                    SwitchPreference(
                        checked = autoHideNavBar.value, onCheckedChange = { autoHideNavBar.value = it; preferences.edit { putBoolean("autoHideBottomBar", it) } },
                        title = "滚动时自动隐藏底部导航栏", summary = "上划时隐藏，下划时重新显示",
                    )
                }
            }

            // ===== 搜索 =====
            item { SmallTitle(text = "搜索") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    SwitchPreference(
                        checked = showHotSearch.value, onCheckedChange = { showHotSearch.value = it; preferences.edit { putBoolean("showSearchHotSearch", it) } },
                        title = "显示热搜", summary = "在搜索界面空白时显示知乎热搜关键词",
                    )
                    SwitchPreference(
                        checked = showSearchHistory.value, onCheckedChange = { showSearchHistory.value = it; preferences.edit { putBoolean("showSearchHistory", it) } },
                        title = "记录并显示搜索历史", summary = "关闭后不再记录新的搜索",
                    )
                }
            }

            // ===== 导航 =====
            item { SmallTitle(text = "导航") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    SwitchPreference(
                        checked = useCustomNav.value, onCheckedChange = { useCustomNav.value = it; preferences.edit { putBoolean("use_custom_nav_host", it) } },
                        title = "使用自定义导航", summary = "替代系统默认导航，需要重启生效",
                    )
                    SwitchPreference(
                        checked = enablePredictiveBack.value, onCheckedChange = { enablePredictiveBack.value = it; preferences.edit { putBoolean("enable_predictive_back", it) } },
                        title = "启用预测性返回", summary = "Android 14+ 手势动画",
                    )
                }
            }

            // ===== 123Duo3 =====
            item { SmallTitle(text = "123Duo3 UI/UX 改进 (beta)") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    SwitchPreference(
                        checked = duo3All.value, onCheckedChange = { all ->
                            duo3All.value = all; preferences.edit { putBoolean("duo3_all", all) }
                            listOf(duo3HomeAccount, duo3NavStyle, duo3CardAppearance, duo3CardLayout, duo3ArticleBar, duo3ArticleActions).forEach { it.value = all }
                            preferences.edit { putBoolean("duo3_home_account", all); putBoolean("duo3_nav_style", all); putBoolean("duo3_card_appearance", all); putBoolean("duo3_card_layout", all); putBoolean("duo3_article_bar", all); putBoolean("duo3_article_actions", all) }
                            val updated = if (all && "Home" !in selectedBottomBarKeys.value) selectedBottomBarKeys.value + "Account" else selectedBottomBarKeys.value
                            persistBottomBar(updated, all)
                        },
                        title = "启用所有修改并关闭浮动按钮", summary = "一键开关所有 123Duo3 改进",
                    )
                    SwitchPreference(
                        checked = duo3HomeAccount.value,
                        onCheckedChange = {
                            duo3HomeAccount.value = it
                            preferences.edit { putBoolean("duo3_home_account", it) }
                            val updated = if (it && "Home" !in selectedBottomBarKeys.value) selectedBottomBarKeys.value + "Account" else selectedBottomBarKeys.value
                            persistBottomBar(updated, it)
                        },
                        title = "主页：账号入口迁移至顶部头像", summary = "搜索栏样式变更，点击头像弹出账号与设置",
                    )
                    SwitchPreference(checked = duo3NavStyle.value, onCheckedChange = { duo3NavStyle.value = it; preferences.edit { putBoolean("duo3_nav_style", it) } }, title = "底部导航栏：改为 Material 样式", summary = "移除自定义样式，更改关注图标")
                    SwitchPreference(checked = duo3CardAppearance.value, onCheckedChange = { duo3CardAppearance.value = it; preferences.edit { putBoolean("duo3_card_appearance", it) } }, title = "信息流卡片：外观更改", summary = "圆角增大，移除阴影")
                    SwitchPreference(checked = duo3CardLayout.value, onCheckedChange = { duo3CardLayout.value = it; preferences.edit { putBoolean("duo3_card_layout", it) } }, title = "信息流卡片：更改内容排版", summary = "作者移至底部，摘要最多4行")
                    SwitchPreference(checked = duo3ArticleBar.value, onCheckedChange = { duo3ArticleBar.value = it; preferences.edit { putBoolean("duo3_article_bar", it) } }, title = "文章阅读页：更改顶/底栏框架", summary = "标题栏样式变更，优化隐藏逻辑")
                    SwitchPreference(checked = duo3ArticleActions.value, onCheckedChange = { duo3ArticleActions.value = it; preferences.edit { putBoolean("duo3_article_actions", it) } }, title = "文章阅读页：更改操作栏样式", summary = "底栏操作用药丸包裹")
                }
            }
        }
    }

    // Color picker sheets (always in tree, driven by show state)
    MiuixColorPickerSheet(
        show = showColorPicker,
        title = "选择主题色",
        initialColor = customColor,
        onConfirm = {
            ThemeManager.setCustomColor(context, it)
            showColorPicker.value = false
        },
    )
    MiuixColorPickerSheet(
        show = showBgPicker,
        title = "选择背景颜色",
        initialColor = bgColor,
        onConfirm = {
            ThemeManager.setBackgroundColor(context, it, isDark)
            showBgPicker.value = false
        },
    )
}

// ────────────────────────────────────────────────────────────────────
// Reusable sub-composables
// ────────────────────────────────────────────────────────────────────

@Composable
private fun ColorCircle(color: Color) {
    Box(
        Modifier.size(32.dp).clip(CircleShape).background(color)
            .border(1.dp, MiuixTheme.colorScheme.onSurfaceVariantSummary, CircleShape),
    )
}

@Composable
private fun SliderRow(value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onValueChange: (Int) -> Unit) {
    var sliderValue by remember(value) { mutableStateOf(value) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it; onValueChange(it.toInt()) },
            valueRange = range,
            modifier = Modifier.weight(1f),
        )
    }
}

// ────────────────────────────────────────────────────────────────────
// Spinner widgets
// ────────────────────────────────────────────────────────────────────

@Composable
private fun UiEngineSpinner(currentStyle: ThemeStyle, onStyleChange: (ThemeStyle) -> Unit) {
    val options = remember { mapOf(ThemeStyle.Material3 to "Material 3", ThemeStyle.Miuix to "Miuix") }
    val items = remember { options.values.map { DropdownItem(title = it) } }
    val idx = remember(currentStyle, options) { options.keys.indexOf(currentStyle).coerceAtLeast(0) }
    WindowSpinnerPreference(
        title = "UI 引擎", summary = "切换应用整体的视觉风格",
        items = items, selectedIndex = idx,
        onSelectedIndexChange = { val s = options.keys.elementAt(it); if (s != currentStyle) onStyleChange(s) },
    )
}

@Composable
private fun ThemeModeSpinner(currentThemeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    val options = remember { mapOf(ThemeMode.LIGHT to "浅色主题", ThemeMode.DARK to "深色主题", ThemeMode.SYSTEM to "跟随系统主题") }
    val items = remember { options.values.map { DropdownItem(title = it) } }
    val idx = remember(currentThemeMode, options) { options.keys.indexOf(currentThemeMode).coerceAtLeast(0) }
    WindowSpinnerPreference(
        title = "主题模式", items = items, selectedIndex = idx,
        onSelectedIndexChange = { val m = options.keys.elementAt(it); if (m != currentThemeMode) onThemeModeChange(m) },
    )
}

@Composable
private fun FeedCardStyleSpinner(current: String, onChange: (String) -> Unit) {
    val opts = remember { mapOf("card" to "卡片样式", "divider" to "分割线样式") }
    val items = remember { opts.values.map { DropdownItem(title = it) } }
    val idx = remember(current, opts) { opts.keys.indexOf(current).coerceAtLeast(0) }
    WindowSpinnerPreference(
        title = "信息流样式", summary = "卡片样式用圆角卡片，分割线用细线分隔",
        items = items, selectedIndex = idx,
        onSelectedIndexChange = { val s = opts.keys.elementAt(it); if (s != current) onChange(s) },
    )
}

@Composable
private fun AnswerSwitchModeSpinner(current: String, onChange: (String) -> Unit) {
    val opts = remember { mapOf("off" to "关闭", "vertical" to "上下滑动", "horizontal" to "左右滑动") }
    val items = remember { opts.values.map { DropdownItem(title = it) } }
    val idx = remember(current, opts) { opts.keys.indexOf(current).coerceAtLeast(0) }
    WindowSpinnerPreference(
        title = "回答切换手势", summary = "通过手势切换同一问题下的其他回答",
        items = items, selectedIndex = idx,
        onSelectedIndexChange = { val s = opts.keys.elementAt(it); if (s != current) onChange(s) },
    )
}

@Composable
private fun AnswerDoubleTapSpinner(current: String, onChange: (String) -> Unit) {
    val opts = remember { mapOf("ask" to "询问", "copy" to "复制链接", "upvote" to "赞同", "downvote" to "反对") }
    val items = remember { opts.values.map { DropdownItem(title = it) } }
    val idx = remember(current, opts) { opts.keys.indexOf(current).coerceAtLeast(0) }
    WindowSpinnerPreference(
        title = "双击回答动作", summary = "双击回答正文时执行的动作",
        items = items, selectedIndex = idx,
        onSelectedIndexChange = { val s = opts.keys.elementAt(it); if (s != current) onChange(s) },
    )
}

@Composable
private fun StartDestinationSpinner(current: String, availableKeys: Set<String>, onChange: (String) -> Unit) {
    val allLabels = mapOf("Home" to "主页", "Follow" to "关注", "HotList" to "热榜", "Daily" to "日报", "OnlineHistory" to "历史", "Account" to "账号设置")
    val filtered = allLabels.filterKeys { it in availableKeys }
    val items = remember(filtered) { filtered.values.map { DropdownItem(title = it) } }
    val keys = remember(filtered) { filtered.keys.toList() }
    val idx = remember(current, keys) { keys.indexOf(current).coerceAtLeast(0) }
    WindowSpinnerPreference(
        title = "启动默认页面", summary = "仅可选择已在底部导航栏中显示的页面",
        items = items, selectedIndex = idx,
        onSelectedIndexChange = { val k = keys.elementAt(it); if (k != current) onChange(k) },
    )
}

