/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.subscreens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Daily
import com.github.zly2006.zhihu.navigation.Follow
import com.github.zly2006.zhihu.navigation.Home
import com.github.zly2006.zhihu.navigation.HotList
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.MyCollections
import com.github.zly2006.zhihu.navigation.OnlineHistory
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.theme.ThemeMode
import com.github.zly2006.zhihu.shared.ui.ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY
import com.github.zly2006.zhihu.shared.ui.AnswerDoubleTapAction
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.ThemeStyle
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.ARTICLE_USE_WEBVIEW_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.miuix.components.MiuixColorPickerSheet
import com.github.zly2006.zhihu.ui.miuix.components.MiuixExpandableArrowPreference
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import com.github.zly2006.zhihu.ui.subscreens.BOTTOM_BAR_ITEMS_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.BOTTOM_BAR_ITEM_ORDER_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.DUO3_CARD_LARGE_TITLE_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.PREF_FONT_SIZE
import com.github.zly2006.zhihu.ui.subscreens.PREF_LINE_HEIGHT
import com.github.zly2006.zhihu.ui.subscreens.START_DESTINATION_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.WebViewCustomFontSettings
import com.github.zly2006.zhihu.ui.subscreens.bottomBarItemOrderFromPreference
import com.github.zly2006.zhihu.ui.subscreens.bottomBarItemOrderPreferenceValue
import com.github.zly2006.zhihu.ui.subscreens.defaultBottomBarSelectionKeys
import com.github.zly2006.zhihu.ui.subscreens.normalizeBottomBarItemOrder
import com.github.zly2006.zhihu.ui.subscreens.normalizeBottomBarSelection
import com.github.zly2006.zhihu.ui.subscreens.rememberThemeSettingsRuntime
import com.github.zly2006.zhihu.ui.subscreens.resolveValidStartDestinationKey
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

private val bottomBarItemLabels = mapOf(
    Home.name to "主页",
    Follow.name to "关注",
    HotList.name to "热榜",
    Daily.name to "日报",
    OnlineHistory.name to "历史",
    MyCollections.name to "收藏夹",
    Account.name to "账号设置",
)

private val bottomBarItemKeys = bottomBarItemLabels.keys.toList()

@Composable
fun MiuixAppearanceSettingsScreen(
    @Suppress("UNUSED_PARAMETER") setting: String = "",
    onExit: () -> Unit = {},
) {
    val settings = rememberSettingsStore()
    val userMessages = rememberUserMessageSink()
    val runtime = rememberThemeSettingsRuntime()
    val navigator = LocalNavigator.current
    val blurEnabled = remember { mutableStateOf(settings.getBoolean("blurEnabled", true)) }
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled.value)
    val scrollBehavior = MiuixScrollBehavior()

    // Theme state
    val themeStyle = ThemeManager.getThemeStyle()
    val themeMode = ThemeManager.getThemeMode()
    val useDynamicColor = ThemeManager.getUseDynamicColor()
    val customColor = ThemeManager.getCustomColor()
    val isDark = ThemeManager.isDarkTheme()
    val bgColor = ThemeManager.getBackgroundColor()
    val luotianYiColor = remember { mutableStateOf(Color(settings.getInt("luotianyi_color", 0xff_66CCFF.toInt()))) }

    // 阅读
    var fontSize by remember { mutableIntStateOf(settings.getInt(PREF_FONT_SIZE, 100)) }
    var lineHeight by remember { mutableIntStateOf(settings.getInt(PREF_LINE_HEIGHT, 160)) }
    var showFontSlider by remember { mutableStateOf(false) }
    var showLineSlider by remember { mutableStateOf(false) }
    val showWebViewFontSettings = remember { mutableStateOf(false) }

    // 信息流
    val showFeedThumbnail = remember { mutableStateOf(settings.getBoolean("showFeedThumbnail", true)) }
    val showRefreshFab = remember { mutableStateOf(settings.getBoolean("showRefreshFab", true)) }
    val feedCardStyle = remember { mutableStateOf(settings.getString("feedCardStyle", "card")) }

    // 回答页
    val articleUseWebview = remember { mutableStateOf(settings.getBoolean(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, false)) }
    val customWebViewFontName = remember { mutableStateOf(settings.getStringOrNull("webviewCustomFontName")) }
    val webViewHardwareAcceleration = remember { mutableStateOf(settings.getBoolean("webviewHardwareAcceleration", true)) }
    val titleAutoHide = remember { mutableStateOf(settings.getBoolean("titleAutoHide", false)) }
    val autoHideBottomBar = remember { mutableStateOf(settings.getBoolean("autoHideArticleBottomBar", false)) }
    val buttonSkipAnswer = remember { mutableStateOf(settings.getBoolean("buttonSkipAnswer", true)) }
    val autoHideSkipBtn = remember { mutableStateOf(settings.getBoolean("autoHideSkipAnswerButton", true)) }
    val pinAnswerDate = remember { mutableStateOf(settings.getBoolean("pinAnswerDate", false)) }
    val answerSwitchMode = remember { mutableStateOf(settings.getString("answerSwitchMode", "vertical")) }
    val answerDoubleTap = remember {
        mutableStateOf(
            AnswerDoubleTapAction.fromPreference(
                settings.getString(
                    ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY,
                    AnswerDoubleTapAction.Ask.preferenceValue,
                ),
            ),
        )
    }

    // 底部栏
    val selectedBottomBarKeys = remember {
        val selectedKeys = normalizeBottomBarSelection(
            settings.getStringSet(
                BOTTOM_BAR_ITEMS_PREFERENCE_KEY,
                defaultBottomBarSelectionKeys(settings.getBoolean("duo3_home_account", false)),
            ),
            settings.getBoolean("duo3_home_account", false),
            enforceMinimumSelection = true,
        )
        mutableStateOf(
            bottomBarItemOrderFromPreference(
                settings.getStringOrNull(BOTTOM_BAR_ITEM_ORDER_PREFERENCE_KEY),
                selectedKeys,
            ),
        )
    }
    val startDestinationKey = remember {
        mutableStateOf(
            resolveValidStartDestinationKey(
                settings.getString(START_DESTINATION_PREFERENCE_KEY, Home.name),
                selectedBottomBarKeys.value,
            ),
        )
    }
    val tapToRefresh = remember { mutableStateOf(settings.getBoolean("bottomBarTapScrollToTop", true)) }
    val autoHideTopBar = remember { mutableStateOf(settings.getBoolean("autoHideTopBar", false)) }
    val autoHideNavBar = remember { mutableStateOf(settings.getBoolean("autoHideBottomBar", false)) }
    val shareActionMode = remember { mutableStateOf(settings.getString("shareActionMode", "ask")) }

    // 搜索
    val showHotSearch = remember { mutableStateOf(settings.getBoolean("showSearchHotSearch", true)) }
    val showSearchHistory = remember { mutableStateOf(settings.getBoolean("showSearchHistory", true)) }

    // 导航
    val useCustomNavHost = remember { mutableStateOf(settings.getBoolean("use_custom_nav_host", true)) }
    val enablePredictiveBack = remember { mutableStateOf(settings.getBoolean("enable_predictive_back", true)) }

    // 123Duo3
    val duo3All = remember { mutableStateOf(settings.getBoolean("duo3_all", false)) }
    val duo3HomeAccount = remember { mutableStateOf(settings.getBoolean("duo3_home_account", false)) }
    val duo3NavStyle = remember { mutableStateOf(settings.getBoolean("duo3_nav_style", false)) }
    val duo3CardAppearance = remember { mutableStateOf(settings.getBoolean("duo3_card_appearance", false)) }
    val duo3CardLayout = remember { mutableStateOf(settings.getBoolean("duo3_card_layout", false)) }
    val duo3CardLargeTitle = remember { mutableStateOf(settings.getBoolean(DUO3_CARD_LARGE_TITLE_PREFERENCE_KEY, true)) }
    val duo3ArticleBar = remember { mutableStateOf(settings.getBoolean("duo3_article_bar", false)) }
    val duo3ArticleActions = remember { mutableStateOf(settings.getBoolean("duo3_article_actions", false)) }

    // Color picker state (MutableState ref for WindowBottomSheet pattern)
    val showColorPicker = remember { mutableStateOf(false) }
    val showLuotianYiColorPicker = remember { mutableStateOf(false) }
    val showBgPicker = remember { mutableStateOf(false) }

    // 底栏选择规范化（对齐 M3 persistBottomBarSelection）：
    // duo3_home_account 关闭时强制保留「账号」入口，否则账号设置无处可进。
    fun persistBottomBar(currentOrderKeys: List<String>, duo3Enabled: Boolean) {
        val normalized = normalizeBottomBarSelection(currentOrderKeys, duo3Enabled, enforceMinimumSelection = true)
        val normalizedOrder = normalizeBottomBarItemOrder(currentOrderKeys, normalized)
        val resolvedStart = resolveValidStartDestinationKey(startDestinationKey.value, normalizedOrder)
        selectedBottomBarKeys.value = normalizedOrder
        startDestinationKey.value = resolvedStart
        settings.putStringSet(BOTTOM_BAR_ITEMS_PREFERENCE_KEY, normalized)
        settings.putString(BOTTOM_BAR_ITEM_ORDER_PREFERENCE_KEY, bottomBarItemOrderPreferenceValue(normalizedOrder))
        settings.putString(START_DESTINATION_PREFERENCE_KEY, resolvedStart)
    }

    fun moveBottomBarItem(key: String, offset: Int) {
        val currentOrder = selectedBottomBarKeys.value
        val fromIndex = currentOrder.indexOf(key)
        val toIndex = fromIndex + offset
        if (fromIndex < 0 || toIndex !in currentOrder.indices) {
            return
        }
        val reordered = currentOrder.toMutableList()
        reordered.removeAt(fromIndex)
        reordered.add(toIndex, key)
        persistBottomBar(reordered, duo3HomeAccount.value)
    }

    DisposableEffect(Unit) {
        onDispose {
            onExit()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(backdrop),
                color = backdrop.getMiuixAppBarColor(),
                title = "外观",
                navigationIcon = {
                    IconButton(onClick = {
                        navigator.onNavigateBack()
                    }) {
                        Icon(MiuixIconsEmbedded.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
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
                    UiEngineSpinner(themeStyle) { runtime.setThemeStyle(it) }
                    SwitchPreference(
                        checked = blurEnabled.value,
                        onCheckedChange = {
                            blurEnabled.value = it
                            settings.putBoolean("blurEnabled", it)
                        },
                        title = "毛玻璃效果",
                        summary = "顶栏和底栏使用半透明模糊背景（Android 12+ 可用）",
                    )
                }
            }

            // ===== 主题 =====
            item { SmallTitle(text = "主题") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    ThemeModeSpinner(themeMode) { runtime.setThemeMode(it) }
                    SwitchPreference(
                        checked = useDynamicColor,
                        onCheckedChange = { runtime.setUseDynamicColor(it) },
                        title = "动态颜色",
                        summary = "跟随系统取色（Android 12 及以上）",
                    )
                    if (!useDynamicColor) {
                        ArrowPreference(
                            title = "自定义主题色",
                            summary = "点击选择您喜欢的主题颜色",
                            onClick = { showColorPicker.value = true },
                            endActions = { ColorCircle(customColor) },
                        )
                    }
                    ArrowPreference(
                        title = "唤起浏览器主题色",
                        summary = "应用内浏览器的工具栏颜色",
                        onClick = { showLuotianYiColorPicker.value = true },
                        endActions = { ColorCircle(luotianYiColor.value) },
                    )
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
                        title = "字号",
                        summary = "调整内容文字大小 ($fontSize%)",
                        expanded = showFontSlider,
                        onExpandedChange = { showFontSlider = !showFontSlider },
                    ) {
                        SliderRow(fontSize.toFloat(), 50f..200f, 14) {
                            fontSize = it
                            settings.putInt(PREF_FONT_SIZE, it)
                        }
                    }
                    MiuixExpandableArrowPreference(
                        title = "行高",
                        summary = "调整内容行间距 (${lineHeight / 100f})",
                        expanded = showLineSlider,
                        onExpandedChange = { showLineSlider = !showLineSlider },
                    ) {
                        SliderRow(lineHeight.toFloat(), 100f..300f, 19) {
                            lineHeight = it
                            settings.putInt(PREF_LINE_HEIGHT, it)
                        }
                    }
                }
            }

            // ===== 信息流 =====
            item { SmallTitle(text = "信息流") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    SwitchPreference(
                        checked = showFeedThumbnail.value,
                        onCheckedChange = {
                            showFeedThumbnail.value = it
                            settings.putBoolean("showFeedThumbnail", it)
                        },
                        title = "显示卡片缩略图",
                        summary = "在信息流卡片中显示文章缩略图",
                    )
                    SwitchPreference(
                        checked = showRefreshFab.value,
                        onCheckedChange = {
                            showRefreshFab.value = it
                            settings.putBoolean("showRefreshFab", it)
                        },
                        title = "显示刷新 FAB 按钮",
                        summary = "在页面上显示可拖动的刷新按钮",
                    )
                    FeedCardStyleSpinner(feedCardStyle.value) {
                        feedCardStyle.value = it
                        settings.putString("feedCardStyle", it)
                    }
                }
            }

            // ===== 回答页 =====
            item { SmallTitle(text = "回答页") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    SwitchPreference(
                        checked = articleUseWebview.value,
                        onCheckedChange = {
                            articleUseWebview.value = it
                            settings.putBoolean(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, it)
                        },
                        title = "使用 WebView 显示文章",
                        summary = "关闭后使用 Compose 渲染，支持代码高亮",
                    )
                    if (articleUseWebview.value) {
                        MiuixExpandableArrowPreference(
                            title = "WebView 自定义字体",
                            summary = customWebViewFontName.value ?: "未设置",
                            expanded = showWebViewFontSettings.value,
                            onExpandedChange = { showWebViewFontSettings.value = !showWebViewFontSettings.value },
                        ) {
                            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                WebViewCustomFontSettings(
                                    customFontName = customWebViewFontName.value,
                                    onCustomFontNameChange = { name ->
                                        if (name == null) {
                                            settings.remove("webviewCustomFontName")
                                        } else {
                                            settings.putString("webviewCustomFontName", name)
                                        }
                                        customWebViewFontName.value = name
                                    },
                                )
                            }
                        }
                        SwitchPreference(
                            checked = webViewHardwareAcceleration.value,
                            onCheckedChange = {
                                webViewHardwareAcceleration.value = it
                                settings.putBoolean("webviewHardwareAcceleration", it)
                            },
                            title = "WebView 硬件加速",
                            summary = "提高渲染性能，可能导致兼容性问题",
                        )
                    }
                    SwitchPreference(
                        checked = titleAutoHide.value,
                        onCheckedChange = {
                            titleAutoHide.value = it
                            settings.putBoolean("titleAutoHide", it)
                        },
                        title = "自动隐藏回答标题",
                        summary = "滚动时自动隐藏回答标题栏",
                    )
                    SwitchPreference(
                        checked = autoHideBottomBar.value,
                        onCheckedChange = {
                            autoHideBottomBar.value = it
                            settings.putBoolean("autoHideArticleBottomBar", it)
                        },
                        title = "自动隐藏回答底部按钮",
                        summary = "上划时隐藏，下划时重新显示",
                    )
                    SwitchPreference(
                        checked = buttonSkipAnswer.value,
                        onCheckedChange = {
                            buttonSkipAnswer.value = it
                            settings.putBoolean("buttonSkipAnswer", it)
                        },
                        title = "显示跳转下一个回答按钮",
                        summary = "在回答页显示可拖动快速跳转按钮",
                    )
                    if (buttonSkipAnswer.value) {
                        SwitchPreference(
                            checked = autoHideSkipBtn.value,
                            onCheckedChange = {
                                autoHideSkipBtn.value = it
                                settings.putBoolean("autoHideSkipAnswerButton", it)
                            },
                            title = "滚动时自动隐藏跳转按钮",
                            summary = "上划时淡出，下划时淡入",
                        )
                    }
                    SwitchPreference(
                        checked = pinAnswerDate.value,
                        onCheckedChange = {
                            pinAnswerDate.value = it
                            settings.putBoolean("pinAnswerDate", it)
                        },
                        title = "置顶回答日期",
                        summary = "将回答日期移到内容最前面显示",
                    )
                    AnswerSwitchModeSpinner(answerSwitchMode.value) {
                        answerSwitchMode.value = it
                        settings.putString("answerSwitchMode", it)
                    }
                    AnswerDoubleTapSpinner(answerDoubleTap.value) { action ->
                        answerDoubleTap.value = action
                        settings.putString(ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY, action.preferenceValue)
                    }
                }
            }

            // ===== 底部导航栏 =====
            item { SmallTitle(text = "底部导航栏") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    StartDestinationSpinner(startDestinationKey.value, selectedBottomBarKeys.value) {
                        startDestinationKey.value = it
                        settings.putString(START_DESTINATION_PREFERENCE_KEY, it)
                        userMessages.showShortMessage("重启后生效")
                    }
                    BottomBarOrderPreference(
                        title = "选择显示的页面",
                        selectedKeys = selectedBottomBarKeys.value,
                        onToggle = { key ->
                            val isChecked = key in selectedBottomBarKeys.value
                            val nextOrder = if (isChecked) {
                                selectedBottomBarKeys.value.filter { it != key }
                            } else {
                                selectedBottomBarKeys.value + key
                            }
                            when {
                                isChecked && selectedBottomBarKeys.value.size <= 3 -> {
                                    userMessages.showShortMessage("至少保留3项")
                                }
                                !isChecked && selectedBottomBarKeys.value.size >= 5 -> {
                                    userMessages.showShortMessage("最多选择5项")
                                }
                                else -> persistBottomBar(nextOrder, duo3HomeAccount.value)
                            }
                        },
                        onMove = ::moveBottomBarItem,
                    )
                    SwitchPreference(
                        checked = tapToRefresh.value,
                        onCheckedChange = {
                            tapToRefresh.value = it
                            settings.putBoolean("bottomBarTapScrollToTop", it)
                        },
                        title = "点击导航栏回到顶部/刷新",
                        summary = "点击当前页按钮回到顶部，已在顶部则刷新",
                    )
                    SwitchPreference(
                        checked = autoHideTopBar.value,
                        onCheckedChange = {
                            autoHideTopBar.value = it
                            settings.putBoolean("autoHideTopBar", it)
                        },
                        title = "滚动时自动隐藏顶栏",
                        summary = "浏览首页/关注/热榜/日报/历史时上划隐藏，下划重新显示",
                    )
                    SwitchPreference(
                        checked = autoHideNavBar.value,
                        onCheckedChange = {
                            autoHideNavBar.value = it
                            settings.putBoolean("autoHideBottomBar", it)
                        },
                        title = "滚动时自动隐藏底部导航栏",
                        summary = "上划时隐藏，下划时重新显示",
                    )
                }
            }

            // ===== 交互 =====
            item { SmallTitle(text = "交互") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    ShareActionSpinner(shareActionMode.value) {
                        shareActionMode.value = it
                        settings.putString("shareActionMode", it)
                    }
                }
            }

            // ===== 搜索 =====
            item { SmallTitle(text = "搜索") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    SwitchPreference(
                        checked = showHotSearch.value,
                        onCheckedChange = {
                            showHotSearch.value = it
                            settings.putBoolean("showSearchHotSearch", it)
                        },
                        title = "显示热搜",
                        summary = "在搜索界面空白时显示知乎热搜关键词",
                    )
                    SwitchPreference(
                        checked = showSearchHistory.value,
                        onCheckedChange = {
                            showSearchHistory.value = it
                            settings.putBoolean("showSearchHistory", it)
                        },
                        title = "记录并显示搜索历史",
                        summary = "关闭后不再记录新的搜索",
                    )
                }
            }

            // ===== 导航 =====
            item { SmallTitle(text = "导航") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    SwitchPreference(
                        checked = useCustomNavHost.value,
                        onCheckedChange = {
                            useCustomNavHost.value = it
                            settings.putBoolean("use_custom_nav_host", it)
                            userMessages.showShortMessage("需要重启应用生效")
                        },
                        title = "使用自定义导航",
                        summary = "替代系统默认导航组件，可能改善部分国产手机上的操作手感",
                    )
                    SwitchPreference(
                        checked = enablePredictiveBack.value,
                        onCheckedChange = {
                            enablePredictiveBack.value = it
                            settings.putBoolean("enable_predictive_back", it)
                        },
                        title = "启用预测性返回",
                        summary = "Android 14+ 手势动画",
                    )
                }
            }

            // ===== 123Duo3 =====
            item { SmallTitle(text = "123Duo3 UI/UX 改进 (beta)") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    SwitchPreference(
                        checked = duo3All.value,
                        onCheckedChange = { all ->
                            duo3All.value = all
                            settings.putBoolean("duo3_all", all)
                            listOf(duo3HomeAccount, duo3NavStyle, duo3CardAppearance, duo3CardLayout, duo3ArticleBar, duo3ArticleActions).forEach { it.value = all }
                            settings.putBoolean("duo3_home_account", all)
                            settings.putBoolean("duo3_nav_style", all)
                            settings.putBoolean("duo3_card_appearance", all)
                            settings.putBoolean("duo3_card_layout", all)
                            settings.putBoolean("duo3_article_bar", all)
                            settings.putBoolean("duo3_article_actions", all)
                            if (all) {
                                showRefreshFab.value = false
                                buttonSkipAnswer.value = false
                                settings.putBoolean("showRefreshFab", false)
                                settings.putBoolean("buttonSkipAnswer", false)
                            }
                            val updated = if (all && Home.name !in selectedBottomBarKeys.value) {
                                selectedBottomBarKeys.value + Account.name
                            } else {
                                selectedBottomBarKeys.value
                            }
                            persistBottomBar(updated, all)
                        },
                        title = "启用所有修改并关闭浮动按钮",
                        summary = "一键开关所有 123Duo3 改进",
                    )
                    SwitchPreference(
                        checked = duo3HomeAccount.value,
                        onCheckedChange = {
                            duo3HomeAccount.value = it
                            settings.putBoolean("duo3_home_account", it)
                            val updated = if (it && Home.name !in selectedBottomBarKeys.value) {
                                selectedBottomBarKeys.value + Account.name
                            } else {
                                selectedBottomBarKeys.value
                            }
                            persistBottomBar(updated, it)
                        },
                        title = "主页：账号入口迁移至顶部头像",
                        summary = "搜索栏样式变更，点击头像弹出账号与设置",
                    )
                    SwitchPreference(checked = duo3NavStyle.value, onCheckedChange = {
                        duo3NavStyle.value = it
                        settings.putBoolean("duo3_nav_style", it)
                    }, title = "底部导航栏：改为 Material 样式", summary = "移除自定义样式，更改关注图标")
                    SwitchPreference(checked = duo3CardAppearance.value, onCheckedChange = {
                        duo3CardAppearance.value = it
                        settings.putBoolean("duo3_card_appearance", it)
                    }, title = "信息流卡片：外观更改", summary = "圆角增大，移除阴影")
                    SwitchPreference(checked = duo3CardLayout.value, onCheckedChange = {
                        duo3CardLayout.value = it
                        settings.putBoolean("duo3_card_layout", it)
                    }, title = "信息流卡片：更改内容排版", summary = "作者移至底部，摘要最多4行")
                    if (duo3CardLayout.value) {
                        SwitchPreference(checked = duo3CardLargeTitle.value, onCheckedChange = {
                            duo3CardLargeTitle.value = it
                            settings.putBoolean(DUO3_CARD_LARGE_TITLE_PREFERENCE_KEY, it)
                        }, title = "信息流卡片：使用更大的标题字体", summary = "默认启用；关闭后标题会缩小一档")
                    }
                    SwitchPreference(checked = duo3ArticleBar.value, onCheckedChange = {
                        duo3ArticleBar.value = it
                        settings.putBoolean("duo3_article_bar", it)
                    }, title = "文章阅读页：更改顶/底栏框架", summary = "标题栏样式变更，优化隐藏逻辑")
                    if (duo3ArticleBar.value) {
                        SwitchPreference(checked = duo3ArticleActions.value, onCheckedChange = {
                            duo3ArticleActions.value = it
                            settings.putBoolean("duo3_article_actions", it)
                        }, title = "文章阅读页：更改操作栏样式", summary = "底栏操作用药丸包裹")
                    }
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
            runtime.setCustomColor(it)
            showColorPicker.value = false
        },
    )
    MiuixColorPickerSheet(
        show = showLuotianYiColorPicker,
        title = "选择浏览器主题色",
        initialColor = luotianYiColor.value,
        onConfirm = {
            luotianYiColor.value = it
            settings.putInt("luotianyi_color", it.toArgb())
            showLuotianYiColorPicker.value = false
        },
    )
    MiuixColorPickerSheet(
        show = showBgPicker,
        title = "选择背景颜色",
        initialColor = bgColor,
        onConfirm = {
            runtime.setBackgroundColor(it, isDark)
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
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MiuixTheme.colorScheme.onSurfaceVariantSummary, CircleShape),
    )
}

@Composable
private fun SliderRow(value: Float, range: ClosedFloatingPointRange<Float>, steps: Int, onValueChange: (Int) -> Unit) {
    var sliderValue by remember(value) { mutableStateOf(value) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Slider(
            value = sliderValue,
            onValueChange = {
                sliderValue = it
                onValueChange(it.toInt())
            },
            valueRange = range,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BottomBarOrderPreference(
    title: String,
    selectedKeys: List<String>,
    onToggle: (String) -> Unit,
    onMove: (String, Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedKeySet = selectedKeys.toSet()
    val orderedKeys = selectedKeys + bottomBarItemKeys.filter { it !in selectedKeySet }
    val summary = selectedKeys.joinToString("、") { bottomBarItemLabels[it] ?: it }.ifBlank { "未选择" }

    MiuixExpandableArrowPreference(
        title = title,
        summary = "$summary；可用箭头调整显示和滑动顺序",
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        Column {
            orderedKeys.forEach { key ->
                val label = bottomBarItemLabels[key] ?: key
                val isChecked = key in selectedKeySet
                val selectedIndex = selectedKeys.indexOf(key)
                val isEnabled = key != Account.name

                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isEnabled) { onToggle(key) }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        state = if (isChecked) ToggleableState.On else ToggleableState.Off,
                        onClick = { if (isEnabled) onToggle(key) },
                        enabled = isEnabled,
                    )
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        style = MiuixTheme.textStyles.body1,
                        color = if (isEnabled) {
                            MiuixTheme.colorScheme.onBackground
                        } else {
                            MiuixTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                        },
                    )
                    if (isChecked) {
                        IconButton(
                            onClick = { onMove(key, -1) },
                            enabled = selectedIndex > 0,
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上移$label")
                        }
                        IconButton(
                            onClick = { onMove(key, 1) },
                            enabled = selectedIndex in 0 until selectedKeys.lastIndex,
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下移$label")
                        }
                    }
                }
            }
        }
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
        title = "UI 引擎",
        summary = "切换应用整体的视觉风格",
        items = items,
        selectedIndex = idx,
        onSelectedIndexChange = {
            val s = options.keys.elementAt(it)
            if (s != currentStyle) onStyleChange(s)
        },
    )
}

@Composable
private fun ThemeModeSpinner(currentThemeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit) {
    val options = remember { mapOf(ThemeMode.LIGHT to "浅色主题", ThemeMode.DARK to "深色主题", ThemeMode.SYSTEM to "跟随系统主题") }
    val items = remember { options.values.map { DropdownItem(title = it) } }
    val idx = remember(currentThemeMode, options) { options.keys.indexOf(currentThemeMode).coerceAtLeast(0) }
    WindowSpinnerPreference(
        title = "主题模式",
        items = items,
        selectedIndex = idx,
        onSelectedIndexChange = {
            val m = options.keys.elementAt(it)
            if (m != currentThemeMode) onThemeModeChange(m)
        },
    )
}

@Composable
private fun FeedCardStyleSpinner(current: String, onChange: (String) -> Unit) {
    val opts = remember { mapOf("card" to "卡片样式", "divider" to "分割线样式") }
    val items = remember { opts.values.map { DropdownItem(title = it) } }
    val idx = remember(current, opts) { opts.keys.indexOf(current).coerceAtLeast(0) }
    WindowSpinnerPreference(
        title = "信息流样式",
        summary = "卡片样式用圆角卡片，分割线用细线分隔",
        items = items,
        selectedIndex = idx,
        onSelectedIndexChange = {
            val s = opts.keys.elementAt(it)
            if (s != current) onChange(s)
        },
    )
}

@Composable
private fun AnswerSwitchModeSpinner(current: String, onChange: (String) -> Unit) {
    val opts = remember { mapOf("off" to "关闭", "vertical" to "上下滑动", "horizontal" to "左右滑动") }
    val items = remember { opts.values.map { DropdownItem(title = it) } }
    val idx = remember(current, opts) { opts.keys.indexOf(current).coerceAtLeast(0) }
    WindowSpinnerPreference(
        title = "回答切换手势",
        summary = "通过手势切换同一问题下的其他回答",
        items = items,
        selectedIndex = idx,
        onSelectedIndexChange = {
            val s = opts.keys.elementAt(it)
            if (s != current) onChange(s)
        },
    )
}

@Composable
private fun AnswerDoubleTapSpinner(current: AnswerDoubleTapAction, onChange: (AnswerDoubleTapAction) -> Unit) {
    val actions = remember { AnswerDoubleTapAction.entries }
    val items = remember(actions) { actions.map { DropdownItem(title = it.label) } }
    val idx = remember(current, actions) { actions.indexOf(current).coerceAtLeast(0) }
    WindowSpinnerPreference(
        title = "双击回答动作",
        summary = "双击回答正文时执行的动作",
        items = items,
        selectedIndex = idx,
        onSelectedIndexChange = {
            val action = actions.elementAt(it)
            if (action != current) onChange(action)
        },
    )
}

@Composable
private fun StartDestinationSpinner(current: String, availableKeys: List<String>, onChange: (String) -> Unit) {
    val options = remember(availableKeys) {
        availableKeys.mapNotNull { key -> bottomBarItemLabels[key]?.let { label -> key to label } }
    }
    val items = remember(options) { options.map { DropdownItem(title = it.second) } }
    val keys = remember(options) { options.map { it.first } }
    val idx = remember(current, keys) { keys.indexOf(current).coerceAtLeast(0) }
    WindowSpinnerPreference(
        title = "启动默认页面",
        summary = "仅可选择已在底部导航栏中显示的页面",
        items = items,
        selectedIndex = idx,
        onSelectedIndexChange = {
            val k = keys.elementAt(it)
            if (k != current) onChange(k)
        },
    )
}

@Composable
private fun ShareActionSpinner(current: String, onChange: (String) -> Unit) {
    val opts = remember { mapOf("ask" to "询问", "copy" to "复制链接", "share" to "Android分享") }
    val items = remember { opts.values.map { DropdownItem(title = it) } }
    val idx = remember(current, opts) { opts.keys.indexOf(current).coerceAtLeast(0) }
    WindowSpinnerPreference(
        title = "分享操作",
        summary = "点击分享按钮时的默认行为",
        items = items,
        selectedIndex = idx,
        onSelectedIndexChange = {
            val mode = opts.keys.elementAt(it)
            if (mode != current) onChange(mode)
        },
    )
}
