/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.shared.aigc.AIGC_MARKING_ENABLED_PREFERENCE_KEY
import com.github.zly2006.zhihu.shared.notification.NotificationType
import com.github.zly2006.zhihu.shared.ui.ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.ARTICLE_USE_WEBVIEW_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.components.SettingItem
import com.github.zly2006.zhihu.ui.components.SettingItemGroup

const val SETTINGS_SEARCH_INPUT_TAG = "settingsSearch.input"
const val SETTINGS_SEARCH_RESULTS_TAG = "settingsSearch.results"

private data class SettingsSearchEntry(
    val id: String,
    val title: String,
    val section: String,
    val description: String,
    val destination: NavDestination,
    val keywords: List<String> = emptyList(),
) {
    fun matches(query: String): Boolean {
        val normalizedTerms = query
            .trim()
            .lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
        if (normalizedTerms.isEmpty()) return true
        val searchableText = buildString {
            append(title)
            append('\n')
            append(section)
            append('\n')
            append(description)
            if (keywords.isNotEmpty()) {
                append('\n')
                append(keywords.joinToString(" "))
            }
        }.lowercase()
        return normalizedTerms.all(searchableText::contains)
    }
}

private fun appearanceEntry(
    id: String,
    title: String,
    description: String,
    settingKey: String,
    keywords: List<String> = emptyList(),
): SettingsSearchEntry = SettingsSearchEntry(
    id = id,
    title = title,
    section = "外观与阅读体验",
    description = description,
    destination = Account.AppearanceSettings(setting = settingKey),
    keywords = keywords,
)

private fun recommendEntry(
    id: String,
    title: String,
    description: String,
    settingKey: String,
    keywords: List<String> = emptyList(),
): SettingsSearchEntry = SettingsSearchEntry(
    id = id,
    title = title,
    section = "推荐系统与内容过滤",
    description = description,
    destination = Account.RecommendSettings(setting = settingKey),
    keywords = keywords,
)

private fun systemEntry(
    id: String,
    title: String,
    description: String,
    settingKey: String,
    keywords: List<String> = emptyList(),
): SettingsSearchEntry = SettingsSearchEntry(
    id = id,
    title = title,
    section = "系统与更新",
    description = description,
    destination = Account.SystemAndUpdateSettings(setting = settingKey),
    keywords = keywords,
)

private fun notificationEntry(
    id: String,
    title: String,
    description: String,
    settingKey: String,
    keywords: List<String> = emptyList(),
): SettingsSearchEntry = SettingsSearchEntry(
    id = id,
    title = title,
    section = "通知设置",
    description = description,
    destination = Notification.NotificationSettings(setting = settingKey),
    keywords = keywords,
)

private val settingsSearchEntries = buildList {
    add(appearanceEntry("appearance.nightMode", "主题模式", "切换浅色、深色或跟随系统。", "nightMode", listOf("夜间模式", "深色模式")))
    add(appearanceEntry("appearance.dynamicColor", "使用 Material You 动态取色", "Android 12+ 根据系统壁纸取色。", "dynamicColor", listOf("动态颜色")))
    add(appearanceEntry("appearance.fontScale", "字号与行高", "调整正文阅读字号和行距。", "fontScale", listOf("字体大小", "内容字体")))
    add(appearanceEntry("appearance.showFeedThumbnail", "显示 Feed 卡片缩略图", "控制信息流卡片图片显示。", "showFeedThumbnail", listOf("图片", "封面")))
    add(appearanceEntry("appearance.showRefreshFab", "显示刷新 FAB 按钮", "控制首页和列表的浮动刷新按钮。", "showRefreshFab", listOf("刷新按钮", "浮动按钮")))
    add(appearanceEntry("appearance.feedCardStyle", "信息流样式", "切换卡片或分割线样式。", "feedCardStyle", listOf("Feed", "列表样式")))
    add(appearanceEntry("appearance.webviewRender", "使用 WebView 显示文章", "切换文章、回答、想法正文渲染方式。", ARTICLE_USE_WEBVIEW_PREFERENCE_KEY))
    add(appearanceEntry("appearance.titleAutoHide", "自动隐藏回答标题", "阅读时自动收起顶部标题。", "titleAutoHide", listOf("标题栏")))
    add(appearanceEntry("appearance.autoHideArticleBottomBar", "自动隐藏回答底部按钮", "滚动阅读时自动隐藏底部操作栏。", "autoHideArticleBottomBar"))
    add(appearanceEntry("appearance.buttonSkipAnswer", "显示跳转下一个回答按钮", "在回答页显示快速跳转按钮。", "buttonSkipAnswer", listOf("下一个回答")))
    add(appearanceEntry("appearance.pinAnswerDate", "置顶回答日期", "调整回答日期在正文中的位置。", "pinAnswerDate"))
    add(appearanceEntry("appearance.answerSwitchMode", "回答切换手势", "设置回答之间的上下或左右切换。", "answerSwitchMode", listOf("手势")))
    add(appearanceEntry("appearance.answerDoubleTapAction", "双击回答动作", "设置双击正文后的默认动作。", ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY, listOf("双击")))
    add(appearanceEntry("appearance.bottomBar", "底部导航栏", "启动页、底栏显示页面和底栏行为。", APPEARANCE_SETTINGS_BOTTOM_BAR_SECTION_KEY, listOf("启动默认页面", "底部栏")))
    add(appearanceEntry("appearance.shareAction", "分享操作", "设置分享按钮默认复制、分享或询问。", "shareAction"))
    add(appearanceEntry("appearance.showSearchHotSearch", "搜索界面显示热搜", "控制搜索页空查询时是否展示热搜。", "showSearchHotSearch", listOf("热搜")))
    add(appearanceEntry("appearance.showSearchHistory", "记录并显示搜索历史", "控制搜索历史记录和展示。", "showSearchHistory", listOf("搜索记录")))
    add(appearanceEntry("appearance.customNavHost", "使用自定义导航", "切换实验性的导航实现。", "use_custom_nav_host"))
    add(appearanceEntry("appearance.predictiveBack", "启用预测性返回", "控制 Android 预测性返回动画。", "enable_predictive_back", listOf("返回手势")))
    add(appearanceEntry("appearance.duo3", "123Duo3 新 UI", "集中管理 Duo3 视觉和交互开关。", "123Duo3", listOf("新UI", "Duo3")))

    add(recommendEntry("recommend.recommendationMode", "推荐算法", "选择 Web、Android、本地或混合推荐。", "recommendationMode", listOf("推荐来源")))
    add(recommendEntry("recommend.loginForRecommendation", "推荐内容时登录", "获取推荐内容时是否带登录凭证。", "loginForRecommendation"))
    add(recommendEntry("recommend.enableQualityFilter", "启用质量过滤规则", "按赞同数、关注数等指标过滤内容。", "enableQualityFilter", listOf("低质量")))
    add(recommendEntry("recommend.enableContentFilter", "启用智能内容过滤", "过滤重复出现但未点击内容。", "enableContentFilter"))
    add(recommendEntry("recommend.filterFollowedUserContent", "过滤已关注用户内容", "控制是否过滤已关注用户的内容。", "filterFollowedUserContent"))
    add(recommendEntry("recommend.enableKeywordBlocking", "启用关键词屏蔽", "按关键词过滤内容。", "enableKeywordBlocking", listOf("关键词")))
    add(recommendEntry("recommend.enableUserBlocking", "启用用户屏蔽", "按用户过滤内容。", "enableUserBlocking", listOf("作者屏蔽")))
    add(recommendEntry("recommend.enableTopicBlocking", "启用主题屏蔽", "按命中主题过滤内容。", "enableTopicBlocking"))
    add(recommendEntry("recommend.topicBlockingThreshold", "主题屏蔽阈值", "设置命中多少个被屏蔽主题后才过滤内容。", "topicBlockingThreshold", listOf("主题阈值")))
    add(recommendEntry("recommend.blockZhihuAdPlatform", "屏蔽知乎广告平台内容", "过滤知乎广告平台推广内容。", "blockZhihuAdPlatform", listOf("广告")))
    add(recommendEntry("recommend.blockZhihuSchool", "屏蔽知乎学堂内容", "过滤知乎学堂和教育推广内容。", "blockZhihuSchool", listOf("学堂")))
    add(recommendEntry("recommend.blockWeChatOfficialAccount", "屏蔽微信公众号文章", "过滤微信公众号外链内容。", "blockWeChatOfficialAccount", listOf("微信")))
    add(recommendEntry("recommend.blockPaidContent", "屏蔽知乎盐选付费内容", "过滤会员付费内容。", "blockPaidContent", listOf("盐选", "付费")))
    add(recommendEntry("recommend.reverseBlock", "反向屏蔽", "只保留广告和付费内容的调试模式。", "reverseBlock"))
    add(
        SettingsSearchEntry(
            id = "recommend.blocklist",
            title = "管理屏蔽列表",
            section = "推荐系统与内容过滤",
            description = "管理关键词、用户和主题屏蔽。",
            destination = Account.RecommendSettings.Blocklist,
            keywords = listOf("黑名单"),
        ),
    )
    add(
        SettingsSearchEntry(
            id = "recommend.blockedHistory",
            title = "屏蔽记录",
            section = "推荐系统与内容过滤",
            description = "查看最近被过滤的内容。",
            destination = Account.RecommendSettings.BlockedFeedHistory,
            keywords = listOf("过滤记录"),
        ),
    )

    add(systemEntry("system.githubToken", "GitHub Token", "配置更新检查时使用的 GitHub API 令牌。", "githubToken", listOf("限速", "更新检查")))
    add(systemEntry("system.autoCheckUpdates", "自动检查更新", "应用启动后后台检查新版本。", "autoCheckUpdates", listOf("更新提醒")))
    add(systemEntry("system.checkNightlyUpdates", "检查 Nightly 版本更新", "检查每日构建版本。", "checkNightlyUpdates", listOf("每日构建")))
    add(systemEntry("system.allowTelemetry", "允许发送遥测统计数据", "控制匿名使用统计。", "allowTelemetry", listOf("统计", "隐私")))
    add(systemEntry("system.aigcMarking", "启用 AIGC 标记", "开启后可查看其他用户对内容是否疑似 AIGC 的标记。", AIGC_MARKING_ENABLED_PREFERENCE_KEY, listOf("AI", "AIGC")))
    add(systemEntry("system.reminder", "防沉迷提醒", "设置连续使用提醒的间隔。", CONTINUOUS_USAGE_REMINDER_INTERVAL_MINUTES_KEY, listOf("连续使用", "休息提醒")))

    add(notificationEntry("notification.autoMarkAsRead", "打开通知自动已读", "进入通知页后自动标记当前批次为已读。", "autoMarkAsRead", listOf("已读")))
    add(notificationEntry("notification.unreadBadge", "显示未读红点", "控制首页和账号入口的未读角标。", "unreadBadge", listOf("角标", "红点")))
    add(notificationEntry("notification.systemNotifications", "系统通知", "控制是否向系统发送各类通知。", "systemNotifications", NotificationType.entries.map { it.displayName }))
    add(notificationEntry("notification.displayInAppNotifications", "应用内显示", "控制通知页展示哪些通知类型。", "displayInAppNotifications", NotificationType.entries.map { it.displayName }))

    add(
        SettingsSearchEntry(
            id = "developer.page",
            title = "开发者选项",
            section = "开发者",
            description = "调试、签名、Cookie 和实验入口。",
            destination = Account.DeveloperSettings,
            keywords = listOf("Cookie", "调试", "签名请求", "验证登录", "刷新Token"),
        ),
    )
    add(
        SettingsSearchEntry(
            id = "about.licenses",
            title = "开源许可",
            section = "关于",
            description = "查看第三方组件许可证。",
            destination = Account.OpenSourceLicenses,
            keywords = listOf("许可证"),
        ),
    )
}

/**
 * 设置搜索页。
 *
 * 这里维护的是“可搜索标签 -> 已有设置页 route”的轻量索引，不复制设置项实现。新增设置如果已经支持 `settingKey`
 * 高亮，应补一条索引；不支持高亮的页面级入口只跳到对应页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSearchScreen() {
    val navigator = LocalNavigator.current
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query) {
        settingsSearchEntries.filter { it.matches(query) }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = { Text("搜索设置项") },
                navigationIcon = {
                    IconButton(
                        onClick = navigator.onNavigateBack,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(SETTINGS_SEARCH_RESULTS_TAG)
                .padding(innerPadding),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 16.dp)
                        .testTag(SETTINGS_SEARCH_INPUT_TAG),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("搜索设置名称或关键词") },
                    singleLine = true,
                )
            }

            if (results.isEmpty()) {
                item {
                    Text(
                        text = "没有找到相关设置",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(
                    items = results,
                    key = { it.id },
                ) { entry ->
                    SettingItemGroup {
                        SettingItem(
                            modifier = Modifier.testTag("settingsSearch.result.${entry.id}"),
                            title = {
                                Column {
                                    Text(entry.title)
                                    Text(
                                        text = entry.section,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                            description = {
                                Text(entry.description)
                            },
                            onClick = { navigator.onNavigate(entry.destination) },
                            endAction = {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
