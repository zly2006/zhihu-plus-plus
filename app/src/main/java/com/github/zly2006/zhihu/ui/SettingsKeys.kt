/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui

/**
 * 设置项 key + 默认值单一信息源。
 * M3 端和 miuix 端都从这里读 key 和默认值，避免硬编码导致状态不同步。
 */
object SettingsKeys {
    // ── 主题 ──
    const val THEME_MODE = "themeMode"
    const val THEME_STYLE = "themeStyle"
    const val USE_DYNAMIC_COLOR = "useDynamicColor"
    const val CUSTOM_THEME_COLOR = "customThemeColor"
    const val BACKGROUND_COLOR_LIGHT = "backgroundColorLight"
    const val BACKGROUND_COLOR_DARK = "backgroundColorDark"
    const val BLUR_ENABLED = "blurEnabled"

    // ── 阅读 ──
    const val FONT_SIZE = "contentFontSize"
    const val LINE_HEIGHT = "contentLineHeight"
    val FONT_SIZE_DEFAULT = 100
    val LINE_HEIGHT_DEFAULT = 160

    // ── 信息流 ──
    const val SHOW_FEED_THUMBNAIL = "showFeedThumbnail"
    const val SHOW_REFRESH_FAB = "showRefreshFab"
    const val FEED_CARD_STYLE = "feedCardStyle"
    val SHOW_FEED_THUMBNAIL_DEFAULT = true
    val SHOW_REFRESH_FAB_DEFAULT = true
    val FEED_CARD_STYLE_DEFAULT = "card"

    // ── 回答页 ──
    const val ARTICLE_USE_WEBVIEW = "articleUseWebview"
    const val TITLE_AUTO_HIDE = "titleAutoHide"
    const val AUTO_HIDE_ARTICLE_BOTTOM_BAR = "autoHideArticleBottomBar"
    const val BUTTON_SKIP_ANSWER = "buttonSkipAnswer"
    const val AUTO_HIDE_SKIP_ANSWER_BUTTON = "autoHideSkipAnswerButton"
    const val PIN_ANSWER_DATE = "pinAnswerDate"
    const val ANSWER_SWITCH_MODE = "answerSwitchMode"
    const val ANSWER_DOUBLE_TAP_ACTION = "answerDoubleTapAction"
    val ANSWER_SWITCH_MODE_DEFAULT = "vertical"
    val ANSWER_DOUBLE_TAP_ACTION_DEFAULT = "ask"

    // ── 底部导航栏 ──
    const val START_DESTINATION = "startDestination"
    const val BOTTOM_BAR_ITEMS = "bottom_bar_items"
    const val BOTTOM_BAR_TAP_SCROLL_TO_TOP = "bottomBarTapScrollToTop"
    const val AUTO_HIDE_TOP_BAR = "autoHideTopBar"
    const val AUTO_HIDE_BOTTOM_BAR = "autoHideBottomBar"
    val BOTTOM_BAR_ITEMS_DEFAULT = setOf("Home", "Follow", "Daily", "OnlineHistory", "Account")
    val START_DESTINATION_DEFAULT = "Home"
    val BOTTOM_BAR_TAP_SCROLL_TO_TOP_DEFAULT = true

    // ── 搜索 ──
    const val SHOW_SEARCH_HOT_SEARCH = "showSearchHotSearch"
    const val SHOW_SEARCH_HISTORY = "showSearchHistory"
    val SHOW_SEARCH_HOT_SEARCH_DEFAULT = true
    val SHOW_SEARCH_HISTORY_DEFAULT = true

    // ── 导航 ──
    const val USE_CUSTOM_NAV_HOST = "use_custom_nav_host"
    const val ENABLE_PREDICTIVE_BACK = "enable_predictive_back"
    val USE_CUSTOM_NAV_HOST_DEFAULT = true
    val ENABLE_PREDICTIVE_BACK_DEFAULT = true

    // ── Duo3 ──
    const val DUO3_ALL = "duo3_all"
    const val DUO3_HOME_ACCOUNT = "duo3_home_account"
    const val DUO3_NAV_STYLE = "duo3_nav_style"
    const val DUO3_CARD_APPEARANCE = "duo3_card_appearance"
    const val DUO3_CARD_LAYOUT = "duo3_card_layout"
    const val DUO3_CARD_LARGE_TITLE = "duo3_card_large_title"
    const val DUO3_ARTICLE_BAR = "duo3_article_bar"
    const val DUO3_ARTICLE_ACTIONS = "duo3_article_actions"

    // ── 推荐/过滤 ──
    const val ENABLE_NLP_FILTER = "enableNlpFilter"
    const val ENABLE_LOW_QUALITY_FILTER = "enableQualityFilter"
    const val ENABLE_SWIPE_REACTION = "enableSwipeReaction"
}
