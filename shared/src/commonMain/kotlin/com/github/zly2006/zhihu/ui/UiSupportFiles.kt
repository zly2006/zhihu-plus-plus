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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.fleeksoft.ksoup.Ksoup
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.navigation.AnswerNavigator
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.TopLevelDestination
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.filter.ContentOpenFrom
import com.github.zly2006.zhihu.shared.platform.SettingsStore
import com.github.zly2006.zhihu.shared.platform.UserMessageSink
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.ui.ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY
import com.github.zly2006.zhihu.shared.ui.AnswerDoubleTapAction
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel.CachedAnswerContent
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import com.github.zly2006.zhihu.viewmodel.getOrFetchContentDetail
import io.ktor.client.HttpClient
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
 * 语义，避免用户打开 WebView 后只有部分内容类型生效。
 */
@Composable
fun PinHtmlContent(html: String) {
    if (rememberSettingsStore().getBoolean(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, false) &&
        supportsZhihuHtmlWebView()
    ) {
        ZhihuHtmlWebViewContent(html)
    } else {
        Spacer(Modifier.height(10.dp))
        RenderMarkdown(
            html = html,
            modifier = Modifier.questionSelectionWorkaround(),
            selectable = true,
            enableScroll = false,
        )
    }
}

expect fun supportsZhihuHtmlWebView(): Boolean

@Composable
expect fun ZhihuHtmlWebViewContent(html: String)

/**
 * 文章页 Compose UI 使用的运行时设置视图。
 *
 * 这些值保存在可变 state 中，是因为大多数阅读设置都应该在用户已经打开文章时即时生效：标题/底栏自动隐藏、回答切换、
 * WebView 渲染和双击正文动作都不应要求重建页面。持久化仍由 [SettingsStore] 负责；这个类只镜像会影响当前 UI 的值，
 * 并暴露文章页内弹窗会用到的显式保存入口。
 */
class ArticleScreenSettingsState(
    isTitleAutoHide: Boolean,
    autoHideArticleBottomBar: Boolean,
    answerSwitchMode: String,
    pinAnswerDate: Boolean,
    useDuo3ArticleActions: Boolean,
    buttonSkipAnswer: Boolean,
    autoHideSkipAnswerButton: Boolean,
    answerDoubleTapAction: AnswerDoubleTapAction,
    useWebView: Boolean,
    private val saveAnswerDoubleTapActionPreference: (AnswerDoubleTapAction) -> Unit,
) {
    var isTitleAutoHide by mutableStateOf(isTitleAutoHide)
    var autoHideArticleBottomBar by mutableStateOf(autoHideArticleBottomBar)
    var answerSwitchMode by mutableStateOf(answerSwitchMode)
    var pinAnswerDate by mutableStateOf(pinAnswerDate)
    var useDuo3ArticleActions by mutableStateOf(useDuo3ArticleActions)
    var buttonSkipAnswer by mutableStateOf(buttonSkipAnswer)
    var autoHideSkipAnswerButton by mutableStateOf(autoHideSkipAnswerButton)
    var answerDoubleTapAction by mutableStateOf(answerDoubleTapAction)
    var useWebView by mutableStateOf(useWebView)

    fun saveAnswerDoubleTapAction(action: AnswerDoubleTapAction) {
        answerDoubleTapAction = action
        saveAnswerDoubleTapActionPreference(action)
    }
}

/**
 * 订阅会改变文章页可见阅读行为的设置项。
 *
 * 设置页和文章页内弹窗可能修改同一批 key。这里通过监听这些 key 并原地更新 [ArticleScreenSettingsState]，
 * 让文章 UI 在保留滚动位置、已加载内容和 ViewModel 状态的同时应用新设置。
 */
@Composable
fun rememberArticleScreenSettingsState(): ArticleScreenSettingsState {
    val settings = rememberSettingsStore()
    val state = remember(settings) {
        ArticleScreenSettingsState(
            isTitleAutoHide = settings.getBoolean("titleAutoHide", false),
            autoHideArticleBottomBar = settings.getBoolean("autoHideArticleBottomBar", false),
            answerSwitchMode = settings.getString("answerSwitchMode", "vertical"),
            pinAnswerDate = settings.getBoolean("pinAnswerDate", false),
            useDuo3ArticleActions = settings.getBoolean("duo3_article_actions", false),
            buttonSkipAnswer = settings.getBoolean("buttonSkipAnswer", true),
            autoHideSkipAnswerButton = settings.getBoolean("autoHideSkipAnswerButton", true),
            answerDoubleTapAction = settings.answerDoubleTapAction(),
            useWebView = settings.getBoolean(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, false),
            saveAnswerDoubleTapActionPreference = { action ->
                settings.putString(
                    ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY,
                    action.preferenceValue,
                )
            },
        )
    }

    DisposableEffect(settings, state) {
        val unregister = settings.observeKeyChanges { key ->
            when (key) {
                "titleAutoHide" -> state.isTitleAutoHide = settings.getBoolean(key, false)
                "autoHideArticleBottomBar" -> {
                    state.autoHideArticleBottomBar = settings.getBoolean(key, false)
                }

                "buttonSkipAnswer" -> state.buttonSkipAnswer = settings.getBoolean(key, true)
                "autoHideSkipAnswerButton" -> state.autoHideSkipAnswerButton = settings.getBoolean(key, true)
                "answerSwitchMode" -> {
                    state.answerSwitchMode = settings.getString(key, "vertical")
                }

                "pinAnswerDate" -> state.pinAnswerDate = settings.getBoolean(key, false)
                "duo3_article_actions" -> state.useDuo3ArticleActions = settings.getBoolean(key, false)
                ARTICLE_USE_WEBVIEW_PREFERENCE_KEY -> state.useWebView = settings.getBoolean(key, false)
                ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY -> {
                    state.answerDoubleTapAction = settings.answerDoubleTapAction()
                }
            }
        }
        onDispose(unregister)
    }

    return state
}

private fun SettingsStore.answerDoubleTapAction(): AnswerDoubleTapAction =
    AnswerDoubleTapAction.fromPreference(
        getString(
            ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY,
            AnswerDoubleTapAction.Ask.preferenceValue,
        ),
    )

@Composable
expect fun rememberArticleHost(): ArticleHost?

@Composable
expect fun ArticlePreviewPreloadEffect(
    cached: CachedAnswerContent?,
    isNext: Boolean,
    title: String,
    onImageLoadFailed: () -> Unit,
)

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
    if (rememberSettingsStore().getBoolean(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, false) &&
        supportsQuestionDetailWebView()
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
        )
    }
}

expect fun supportsQuestionDetailWebView(): Boolean

@Composable
expect fun QuestionDetailWebViewContent(
    questionId: Long,
    html: String,
)

@Composable
expect fun rememberArticleTtsState(): TtsState

@Composable
expect fun rememberArticleSpeechToggler(): (title: String, content: String) -> Unit

@Composable
expect fun rememberArticleBrowserOpener(): (Article) -> Unit

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
 * 文章页需要从外围应用获取的宿主级服务。
 *
 * 文章会参与历史记录、回答间导航、内容打开来源归因、TTS、剪贴板和 deep link 交接。这个接口刻意比 Activity 窄，
 * 让 common 文章 UI 能同时运行在 Android、Desktop 和测试环境里，而不依赖平台类。
 */
interface ArticleHost {
    val articleNavController: NavHostController
    val articleAnswerSwitchState: ArticleAnswerSwitchState
    val articleTtsState: TtsState
    var clipboardDestination: NavDestination?

    fun postHistoryDestination(destination: NavDestination)

    fun consumePendingContentOpenFrom(destination: NavDestination): String = ContentOpenFrom.UNKNOWN

    fun speakArticleText(
        text: String,
        title: String,
    )

    fun stopArticleSpeaking()
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
    val duo3NavStyle: Boolean,
    val tapToScrollToTopEnabled: Boolean,
    val autoHideBottomBar: Boolean,
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
    val duo3NavStyle: Boolean get() = snapshot.duo3NavStyle
    val tapToScrollToTopEnabled: Boolean get() = snapshot.tapToScrollToTopEnabled
    val autoHideBottomBar: Boolean get() = snapshot.autoHideBottomBar
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

/**
 * 当前平台注入 [ZhihuMain] 的导航回调。
 *
 * common UI 的所有点击都通过这个对象发起导航。平台代码负责把旧的顶层目的地映射到
 * [com.github.zly2006.zhihu.navigation.MainTabs]、记录内容打开来源，并处理视频这类平台专用目标。
 */
data class ZhihuMainNavigationState(
    val mainTabNavigationTarget: TopLevelDestination?,
    val navigate: (NavDestination) -> Unit,
    val setCurrentMainTabOpenFrom: (String?) -> Unit,
    val consumeMainTabNavigationTarget: (TopLevelDestination) -> Unit,
)

data class AccountSettingsAccountState(
    val login: Boolean = false,
    val username: String = "",
    val avatarUrl: String? = null,
    val id: String = "",
    val urlToken: String? = null,
)

@Composable
expect fun rememberAccountSettingsAccountState(): State<AccountSettingsAccountState>

@Composable
expect fun rememberAccountProfileRefresher(): suspend () -> Unit

@Composable
expect fun rememberAccountLoginRequester(): () -> Unit

@Composable
expect fun rememberAccountQrLoginRequester(): () -> Unit

@Composable
expect fun rememberAccountLogoutAction(): () -> Unit

@Composable
expect fun rememberAppVersionInfo(): String

@Composable
expect fun rememberMainTabSelector(): (TopLevelDestination) -> Unit

fun noopSettingsStore(): SettingsStore = SettingsStore(
    getBoolean = { _, defaultValue -> defaultValue },
    putBoolean = { _, _ -> },
    getString = { _, defaultValue -> defaultValue },
    putString = { _, _ -> },
    getStringOrNull = { _ -> null },
    putStringSet = { _, _ -> },
    getStringSet = { _, defaultValue -> defaultValue },
    getInt = { _, defaultValue -> defaultValue },
    putInt = { _, _ -> },
    getLong = { _, defaultValue -> defaultValue },
    putLong = { _, _ -> },
    getFloat = { _, defaultValue -> defaultValue },
    putFloat = { _, _ -> },
    remove = { _ -> },
)

internal const val PEOPLE_PROFILE_INCLUDE_PATH =
    "allow_message,is_followed,is_following,is_org,is_blocking,badge_v2,answer_count,follower_count,following_count,articles_count,question_count,pins_count"

data class HomeAccountState(
    val isLoggedIn: Boolean,
    val avatarUrl: String?,
)

data class HomeUpdateAnnouncement(
    val version: String,
    val isNightly: Boolean,
)

@Composable
expect fun rememberHomeAccountState(): HomeAccountState

@Composable
expect fun rememberHomeUpdateAnnouncement(): HomeUpdateAnnouncement?

@Composable
expect fun rememberHomeInstalledAtLeastThreeHours(): Boolean

@Composable
expect fun rememberHomeIsDebuggable(): Boolean

@Composable
expect fun rememberHomeLoginRequester(): () -> Unit

@Composable
expect fun rememberCommentEmojiInlineContent(emojiKeys: Set<String>): Map<String, InlineTextContent>

expect fun commentEmojiInlineKey(placeholder: String): String?

expect fun Modifier.commentSelectionWorkaround(): Modifier

@Composable
expect fun rememberBlocklistRuleImporter(
    userMessages: UserMessageSink,
): (((String) -> Unit) -> Unit)

@Composable
expect fun rememberBlocklistRuleExporter(): suspend () -> String

@Composable
expect fun rememberZhihuHttpClient(): HttpClient

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
