/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fleeksoft.ksoup.Ksoup
import com.github.zly2006.zhihu.account.rememberZhihuAccountStore
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.navigation.AnswerNavigator
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.TopLevelDestination
import com.github.zly2006.zhihu.platform.SettingsStore
import com.github.zly2006.zhihu.platform.UserMessageSink
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.ui.subscreens.DUO3_TIQIAN_MARKDOWN_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.isIdentityManagementSupported
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel.CachedAnswerContent
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import com.github.zly2006.zhihu.viewmodel.getOrFetchContentDetail
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

data class PinLikeResult(
    val isLiked: Boolean,
    val likeCount: Int,
)

internal suspend fun fetchPinLinkCardPreview(
    linkCard: DataHolder.Pin.ContentLinkCard,
    env: ZhihuApiEnvironment,
): PinLinkCardPreview? {
    val destination = resolveLinkCardDestination(linkCard) ?: return null
    return when (destination) {
        is Article -> {
            when (val detail = env.getOrFetchContentDetail(destination)) {
                is DataHolder.Article -> PinLinkCardPreview(
                    title = compactTitle(detail.title),
                    preview = compactPreview(detail.excerpt.ifBlank { detail.content }),
                )
                is DataHolder.Answer -> PinLinkCardPreview(
                    title = compactTitle(detail.question.title),
                    preview = compactPreview(detail.excerpt.ifBlank { detail.content }),
                )
                else -> null
            }
        }
        is Question -> {
            (env.getOrFetchContentDetail(destination) as? DataHolder.Question)?.let { detail ->
                PinLinkCardPreview(
                    title = compactTitle(detail.title),
                    preview = compactPreview(detail.detail),
                )
            }
        }
        is Pin -> {
            (env.getOrFetchContentDetail(destination) as? DataHolder.Pin)?.let { detail ->
                PinLinkCardPreview(
                    title = "${detail.author.name} 的想法",
                    preview = compactPreview(detail.contentHtml),
                )
            }
        }
        else -> null
    }
}

internal fun JsonObject?.booleanCompat(vararg keys: String): Boolean {
    if (this == null) return false
    return keys.firstNotNullOfOrNull { key ->
        get(key)?.jsonPrimitive?.booleanOrNull
    } ?: false
}

/**
 * 想法正文的 HTML 渲染入口。
 *
 * 根据当前 WebView 设置选择平台 WebView 或 Compose Markdown 渲染。这样想法页、问题详情和文章页可以共享同一条“正文渲染模式”
 * 语义，避免用户打开 WebView 后只有部分内容类型生效；提椠 Markdown 开关同理，对这些内容类型一并生效。
 */
@Composable
fun PinHtmlContent(html: String) {
    val settings = rememberSettingsStore()
    if (settings.getBoolean(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, false) &&
        isLegacyWebViewSupported
    ) {
        ZhihuHtmlWebViewContent(html)
    } else {
        Spacer(Modifier.height(10.dp))
        RenderMarkdown(
            html = html,
            modifier = Modifier.questionSelectionWorkaround(),
            selectable = true,
            enableScroll = false,
            useTiqianRenderer = settings.getBoolean(DUO3_TIQIAN_MARKDOWN_PREFERENCE_KEY, false),
        )
    }
}

expect val isLegacyWebViewSupported: Boolean

@Composable
expect fun ZhihuHtmlWebViewContent(html: String)

@Composable
internal fun <T> rememberObservedSetting(
    settings: SettingsStore,
    key: String,
    read: SettingsStore.() -> T,
): MutableState<T> {
    val state = remember(settings, key) { mutableStateOf(settings.read()) }
    DisposableEffect(settings, key, state) {
        val subscription = settings.observeKeyChanges { changedKey ->
            if (changedKey == key) state.value = settings.read()
        }
        onDispose(subscription::close)
    }
    return state
}

@Composable
expect fun consumePendingCommentId(content: NavDestination): String?

@Composable
expect fun ArticleWebViewContent(
    article: Article,
    html: String,
    title: String,
    scrollState: ScrollState,
    rememberedScrollY: Int,
    rememberedScrollYSync: Boolean,
    onRememberedScrollYSyncChange: (Boolean) -> Unit,
    onImageLoadFailed: () -> Unit,
    onDoubleTap: () -> Unit,
)

/** 过滤部分设备文本选择菜单中的非预期系统项。 */
expect fun Modifier.articleMarkdownSelectionWorkaround(): Modifier

/**
 * 问题描述正文的渲染入口。
 *
 * 与文章和想法一致，优先遵循用户选择的 WebView/Markdown 渲染模式；当前平台不支持问题详情 WebView 时回落到 Compose Markdown。
 */
@Composable
fun QuestionDetailContent(
    questionId: Long,
    html: String,
) {
    val settings = rememberSettingsStore()
    if (settings.getBoolean(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, false) &&
        isLegacyWebViewSupported
    ) {
        QuestionDetailWebViewContent(
            questionId = questionId,
            html = html,
        )
    } else {
        RenderMarkdown(
            html = html,
            modifier = Modifier.questionSelectionWorkaround(),
            selectable = true,
            enableScroll = false,
            useTiqianRenderer = settings.getBoolean(DUO3_TIQIAN_MARKDOWN_PREFERENCE_KEY, false),
        )
    }
}

@Composable
expect fun QuestionDetailWebViewContent(
    questionId: Long,
    html: String,
)

@Composable
expect fun rememberArticleTtsState(): TtsState

interface ArticleSpeechToggler {
    operator fun invoke(title: String, content: String)
}

@Composable
expect fun rememberArticleSpeechToggler(): ArticleSpeechToggler

interface ArticleBrowserOpener {
    operator fun invoke(article: Article)
}

@Composable
expect fun rememberArticleBrowserOpener(): ArticleBrowserOpener

fun articleActionText(
    article: Article,
    questionId: Long,
    title: String,
    authorName: String,
): String =
    when (article.type) {
        ArticleType.Answer -> {
            "https://www.zhihu.com/question/$questionId/answer/${article.id}\n【$title - $authorName 的回答】"
        }
        ArticleType.Article -> {
            "https://zhuanlan.zhihu.com/p/${article.id}\n【$title - $authorName 的文章】"
        }
    }

fun articleWebUrl(article: Article): String =
    when (article.type) {
        ArticleType.Answer -> "https://www.zhihu.com/answer/${article.id}"
        ArticleType.Article -> "https://zhuanlan.zhihu.com/p/${article.id}"
    }

fun articleSpeechText(
    title: String,
    content: String,
    maxContentLength: Int = 50_000,
): String =
    buildString {
        append(title)
        append("。")
        if (content.isNotEmpty()) {
            val contentToProcess =
                if (content.length > maxContentLength) {
                    content.substring(0, maxContentLength) + "..."
                } else {
                    content
                }
            append(Ksoup.parse(contentToProcess).text())
        }
    }

/**
 * 同一问题下不同回答之间导航时使用的共享状态。
 *
 * 手势处理器会在导航前更新这里的状态，让平台适配层选择正确的入场/出场转场方向，并避免 route 切换时丢失待交接的
 * navigator 或内容。它不能放在单个文章 composable 内，因为离开页和进入页都需要通过它协调。
 */
interface ArticleAnswerSwitchState {
    var navigator: AnswerNavigator?
    var pendingNavigator: AnswerNavigator?
    var pendingInitialContent: CachedAnswerContent?
    var navigatingFromAnswerSwitch: Boolean
    var answerSwitchDisposeInProgress: Boolean
    var answerTransitionDirection: ArticleAnswerTransitionDirection
    var isImmersiveMode: Boolean

    fun reset()

    fun promoteForNavigation(direction: ArticleAnswerTransitionDirection)
}

enum class ArticleAnswerTransitionDirection {
    DEFAULT,
    VERTICAL_NEXT,
    VERTICAL_PREVIOUS,
    HORIZONTAL_NEXT,
    HORIZONTAL_PREVIOUS,
}

enum class TtsState(
    val isSpeaking: Boolean = false,
) {
    Uninitialized,
    Initializing,
    Ready,
    Error,
    LoadingText,
    Speaking(true),
    Paused,
    SwitchingChunk(true),
}

/**
 * 影响应用主壳形态的不可变设置快照。
 *
 * 这些值决定底部栏有哪些入口、主 pager 从哪个页面开始、重选 tab 是否回到顶部/刷新，以及顶栏/底栏是否自动隐藏。
 * [ZhihuMain] 按快照读取它们，避免把更新到一半的导航设置应用到主界面。
 */
data class ZhihuMainPreferenceSnapshot(
    val duo3HomeAccount: Boolean,
    val tapToScrollToTopEnabled: Boolean,
    val autoHideBottomBar: Boolean,
    val collectionDirectBrowseEnabled: Boolean,
    val selectedBottomBarItemKeys: List<String>,
    val startDestination: TopLevelDestination,
)

/**
 * 长生命周期主壳使用的 [ZhihuMainPreferenceSnapshot] 可变持有者。
 *
 * 用户每次修改外观设置时不应该重建 NavHost。设置页退出时调用 [reload] 即可；主壳会原地更新底部栏和 pager 状态，
 * 同时保持已加载 tab、返回栈和滚动位置稳定。
 */
class ZhihuMainPreferenceState(
    private val readSnapshot: () -> ZhihuMainPreferenceSnapshot,
) {
    private var snapshot by mutableStateOf(readSnapshot())

    val duo3HomeAccount: Boolean get() = snapshot.duo3HomeAccount
    val tapToScrollToTopEnabled: Boolean get() = snapshot.tapToScrollToTopEnabled
    val autoHideBottomBar: Boolean get() = snapshot.autoHideBottomBar
    val collectionDirectBrowseEnabled: Boolean get() = snapshot.collectionDirectBrowseEnabled
    val selectedBottomBarItemKeys: List<String> get() = snapshot.selectedBottomBarItemKeys
    val startDestination: TopLevelDestination get() = snapshot.startDestination

    fun reload() {
        snapshot = readSnapshot()
    }
}

@Composable
fun rememberZhihuMainPreferenceState(
    readSnapshot: () -> ZhihuMainPreferenceSnapshot,
): ZhihuMainPreferenceState = remember { ZhihuMainPreferenceState(readSnapshot) }

data class AccountSettingsAccountState(
    val login: Boolean = false,
    val hasRequiredCookie: Boolean = true,
    val username: String = "",
    val avatarUrl: String? = null,
    val id: String = "",
    val urlToken: String? = null,
    val identityManagementSupported: Boolean = false,
)

@Composable
fun rememberAccountSettingsAccountState(): State<AccountSettingsAccountState> {
    val accountStore = rememberZhihuAccountStore()
    val accounts = accountStore.accountsState.collectAsState()
    return remember(accounts) {
        derivedStateOf {
            val session = accounts.value.session
            AccountSettingsAccountState(
                login = session.login,
                hasRequiredCookie = session.cookies["d_c0"]
                    .isNullOrBlank()
                    .not(),
                username = session.username,
                avatarUrl = session.profile?.avatarUrl,
                id = session.profile
                    ?.id
                    .orEmpty(),
                urlToken = session.profile?.urlToken,
                identityManagementSupported = isIdentityManagementSupported,
            )
        }
    }
}

@Composable
expect fun rememberAppVersionInfo(): String

fun noopSettingsStore(): SettingsStore = object : SettingsStore {
    override fun getBoolean(key: String, defaultValue: Boolean) = defaultValue

    override fun putBoolean(key: String, value: Boolean) = Unit

    override fun getString(key: String, defaultValue: String) = defaultValue

    override fun putString(key: String, value: String) = Unit

    override fun getStringOrNull(key: String): String? = null

    override fun putStringSet(key: String, value: Set<String>) = Unit

    override fun getStringSet(key: String, defaultValue: Set<String>) = defaultValue

    override fun getInt(key: String, defaultValue: Int) = defaultValue

    override fun putInt(key: String, value: Int) = Unit

    override fun getLong(key: String, defaultValue: Long) = defaultValue

    override fun putLong(key: String, value: Long) = Unit

    override fun getFloat(key: String, defaultValue: Float) = defaultValue

    override fun putFloat(key: String, value: Float) = Unit

    override fun remove(key: String) = Unit
}

internal const val PEOPLE_PROFILE_INCLUDE_PATH =
    "allow_message,is_followed,is_following,is_org,is_blocking,badge_v2,answer_count,follower_count,following_count,articles_count,question_count,pins_count"

@Composable
expect fun rememberHomeIsDebuggable(): Boolean

@Composable
expect fun rememberCommentEmojiInlineContent(emojiKeys: Set<String>): Map<String, InlineTextContent>

data class CommentEmoji(
    val placeholder: String,
    val inlineKey: String,
)

@Composable
expect fun rememberCommentEmojis(): List<CommentEmoji>

expect fun commentEmojiInlineKey(placeholder: String): String?

expect fun Modifier.commentSelectionWorkaround(): Modifier

@Composable
expect fun rememberBlocklistRuleImporter(
    userMessages: UserMessageSink,
    onImported: (String) -> Unit,
): BlocklistRuleImporter

interface BlocklistRuleImporter {
    operator fun invoke()
}

interface BlocklistRuleExporter {
    suspend operator fun invoke(): String
}

@Composable
expect fun rememberBlocklistRuleExporter(): BlocklistRuleExporter

/**
 * 沉浸式阅读时控制系统栏（状态栏/导航栏）的显隐。
 * Android 会隐藏状态栏并允许滑动唤出；Desktop/iOS 为空操作。
 */
@Composable
expect fun ArticleImmersiveModeEffect(immersive: Boolean)

/**
 * 离开沉浸式阅读时恢复系统状态栏。
 * 调用时机：导航目的地从 Article 切换到非 Article 时。
 * Android 会显示状态栏；Desktop/iOS 为空操作。
 */
@Composable
expect fun LeaveImmersiveModeCleanup()

expect fun Modifier.questionSelectionWorkaround(): Modifier
