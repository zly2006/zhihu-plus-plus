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
import com.fleeksoft.ksoup.Ksoup
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.markdown.RenderVideoBox
import com.github.zly2006.zhihu.navigation.AnswerNavigator
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.TopLevelDestination
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.RecommendationMode
import com.github.zly2006.zhihu.shared.data.officialBadge
import com.github.zly2006.zhihu.shared.data.officialBadgeDetails
import com.github.zly2006.zhihu.shared.filter.ContentOpenFrom
import com.github.zly2006.zhihu.shared.platform.SettingsStore
import com.github.zly2006.zhihu.shared.platform.UserMessageSink
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.ui.ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY
import com.github.zly2006.zhihu.shared.ui.AnswerDoubleTapAction
import com.github.zly2006.zhihu.ui.components.ShareDialogRuntime
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel.CachedAnswerContent
import com.github.zly2006.zhihu.viewmodel.HistoryEnvironment
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import io.ktor.client.HttpClient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import top.yukonga.miuix.kmp.nav.core.NavController

data class PinLikeResult(
    val isLiked: Boolean,
    val likeCount: Int,
)

data class PinScreenRuntime(
    val fetchLinkCardPreview: suspend (DataHolder.Pin.ContentLinkCard) -> PinLinkCardPreview?,
)

internal suspend fun fetchPinLinkCardPreview(
    linkCard: DataHolder.Pin.ContentLinkCard,
    fetchDetail: suspend (NavDestination) -> DataHolder.Content?,
): PinLinkCardPreview? {
    val destination = resolveLinkCardDestination(linkCard) ?: return null
    return when (destination) {
        is Article -> {
            when (val detail = fetchDetail(destination)) {
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
            (fetchDetail(destination) as? DataHolder.Question)?.let { detail ->
                PinLinkCardPreview(
                    title = compactTitle(detail.title),
                    preview = compactPreview(detail.detail),
                )
            }
        }
        is Pin -> {
            (fetchDetail(destination) as? DataHolder.Pin)?.let { detail ->
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

@Composable
expect fun rememberPinScreenRuntime(): PinScreenRuntime

/**
 * 想法正文的 HTML 渲染入口。
 *
 * 根据当前 WebView 设置选择平台 WebView 或 Compose Markdown 渲染。这样想法页、问题详情和文章页可以共享同一条“正文渲染模式”
 * 语义，避免用户打开 WebView 后只有部分内容类型生效。
 */
@Composable
fun PinHtmlContent(html: String) {
    if (rememberSettingsStore().getBoolean(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, false) &&
        supportsPinHtmlWebView()
    ) {
        PinHtmlWebViewContent(html)
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

expect fun supportsPinHtmlWebView(): Boolean

@Composable
expect fun PinHtmlWebViewContent(html: String)

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

/**
 * 共享文章页需要的平台服务。
 *
 * 文章页的布局、操作区和渲染路径由 common UI 负责；host 桥接和预加载器由平台提供，因为它们依赖 Android 的
 * Activity/NavController 所有权，或 Desktop 的窗口和运行时服务。
 */
interface ArticleScreenRuntime {
    val articleHost: ArticleHost?
    val previewPreloader: ArticlePreviewPreloader
}

fun interface ArticlePreviewPreloader {
    fun preloadPreview(
        cached: CachedAnswerContent,
        isNext: Boolean,
        title: String,
        onImageLoadFailed: () -> Unit,
    )
}

@Composable
expect fun rememberArticleScreenRuntime(): ArticleScreenRuntime

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

/**
 * 文章附件中的视频入口渲染。
 *
 * 只处理知乎接口里 `attachment.type=video` 的情况，将视频 ID 和缩略图交给统一的视频卡片。普通正文视频仍由 Markdown/WebView 路径处理。
 */
@Composable
fun ArticleVideoAttachmentContent(attachment: JsonElement?) {
    if (attachment
            ?.jsonObject
            ?.get("type")
            ?.jsonPrimitive
            ?.content == "video"
    ) {
        val videoId = attachment
            .jsonObject["attachmentId"]
            ?.jsonPrimitive
            ?.content
            ?.toLongOrNull()
        if (videoId != null) {
            val thumbnail = attachment
                .jsonObject["video"]!!
                .jsonObject["videoInfo"]!!
                .jsonObject["thumbnail"]!!
                .jsonPrimitive.content
            RenderVideoBox(
                videoId = videoId,
                thumbnailUrl = thumbnail,
            )
        }
    }
}

/** 过滤部分设备文本选择菜单中的非预期系统项。 */
expect fun Modifier.articleMarkdownSelectionWorkaround(): Modifier

data class LoadedQuestionScreenData(
    val uiState: QuestionScreenUiState,
    val historyDestination: Question,
)

fun questionDetailPreview(html: String): String = Ksoup.parse(html).text().trim()

internal fun loadedQuestionScreenData(
    question: Question,
    questionData: DataHolder.Question,
): LoadedQuestionScreenData {
    val historyDestination = Question(question.questionId, questionData.title)
    return LoadedQuestionScreenData(
        uiState = QuestionScreenUiState(
            questionContent = questionData.detail,
            answerCount = questionData.answerCount,
            visitCount = questionData.visitCount,
            commentCount = questionData.commentCount,
            followerCount = questionData.followerCount,
            title = questionData.title,
            isFollowing = questionData.relationship.isFollowing,
        ),
        historyDestination = historyDestination,
    )
}

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

/**
 * 文章页底部操作区使用的平台桥接。
 *
 * 可见按钮由 common UI 统一绘制，但语音朗读、系统分享、剪贴板和在浏览器打开知乎原文都需要平台实现。
 * 把契约放在这里，可以让操作按钮作为 UI 被测试，同时把副作用留在 shared composable 之外。
 */
interface ArticleActionsRuntime {
    val ttsState: TtsState
    val shareRuntime: ShareDialogRuntime

    fun toggleSpeech(
        title: String,
        content: String,
    )

    fun shareArticle(
        article: Article,
        questionId: Long,
        title: String,
        authorName: String,
    ) {
        shareRuntime.share(article, articleActionText(article, questionId, title, authorName))
    }

    fun copyArticleLink(
        article: Article,
        questionId: Long,
        title: String,
        authorName: String,
    ) {
        shareRuntime.copyLink(article, articleActionText(article, questionId, title, authorName))
    }

    fun openArticleInBrowser(article: Article)
}

@Composable
expect fun rememberArticleActionsRuntime(): ArticleActionsRuntime

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
    val articleNavController: NavController<NavDestination>
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
    var answerTransitionDirection: ArticleAnswerTransitionDirection

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
    val autoHideTopBar: Boolean,
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
    private val initialSnapshot = readSnapshot()

    var duo3HomeAccount by mutableStateOf(initialSnapshot.duo3HomeAccount)
        private set
    var duo3NavStyle by mutableStateOf(initialSnapshot.duo3NavStyle)
        private set
    var tapToScrollToTopEnabled by mutableStateOf(initialSnapshot.tapToScrollToTopEnabled)
        private set
    var autoHideBottomBar by mutableStateOf(initialSnapshot.autoHideBottomBar)
        private set
    var autoHideTopBar by mutableStateOf(initialSnapshot.autoHideTopBar)
        private set
    var selectedBottomBarItemKeys by mutableStateOf(initialSnapshot.selectedBottomBarItemKeys)
        private set
    var startDestination by mutableStateOf(initialSnapshot.startDestination)
        private set

    fun reload() {
        val snapshot = readSnapshot()
        duo3HomeAccount = snapshot.duo3HomeAccount
        duo3NavStyle = snapshot.duo3NavStyle
        tapToScrollToTopEnabled = snapshot.tapToScrollToTopEnabled
        autoHideBottomBar = snapshot.autoHideBottomBar
        autoHideTopBar = snapshot.autoHideTopBar
        selectedBottomBarItemKeys = snapshot.selectedBottomBarItemKeys
        startDestination = snapshot.startDestination
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

/**
 * 账号设置页消费的平台与账号服务。
 *
 * composable 自己负责视觉层级：资料头部、快捷入口、设置入口和关于/许可证区域。登录、扫码、退出、版本信息和主 tab 选择仍由平台注入，
 * 这样同一套账号 UI 可以运行在 Android、Desktop、预览和测试中。
 */
data class AccountSettingsRuntime(
    val accountState: State<AccountSettingsAccountState>,
    val refreshProfile: suspend () -> Unit,
    val requestLogin: () -> Unit,
    val requestQrLoginScan: () -> Unit,
    val logout: () -> Unit,
    val appVersionInfo: () -> String,
    val selectMainTab: (TopLevelDestination) -> Unit,
)

@Composable
expect fun rememberAccountSettingsPlatformRuntime(): AccountSettingsRuntime

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

data class PeopleProfileLoadResult(
    val profile: PeopleProfileUiState,
    val urlToken: String?,
)

internal const val PEOPLE_PROFILE_INCLUDE_PATH =
    "allow_message,is_followed,is_following,is_org,is_blocking,badge_v2,answer_count,follower_count,following_count,articles_count,question_count,pins_count"

internal fun toPeopleProfileLoadResult(
    loadedPerson: DataHolder.People,
    isBlockedInRecommendations: Boolean,
): PeopleProfileLoadResult = PeopleProfileLoadResult(
    profile = PeopleProfileUiState(
        avatar = loadedPerson.avatarUrl,
        name = loadedPerson.name,
        headline = loadedPerson.headline,
        officialBadge = loadedPerson.badgeV2.officialBadge(),
        officialBadgeDetails = loadedPerson.badgeV2.officialBadgeDetails(),
        followerCount = loadedPerson.followerCount,
        followingCount = loadedPerson.followingCount,
        answerCount = loadedPerson.answerCount,
        articleCount = loadedPerson.articlesCount,
        isFollowing = loadedPerson.isFollowing,
        isBlocking = loadedPerson.isBlocking,
        isBlockedInRecommendations = isBlockedInRecommendations,
    ),
    urlToken = loadedPerson.urlToken,
)

data class HomeAccountState(
    val isLoggedIn: Boolean,
    val avatarUrl: String?,
)

data class HomeUpdateAnnouncement(
    val version: String,
    val isNightly: Boolean,
)

/**
 * 首页信息流界面的运行时依赖集合。
 *
 * 首页同时组合推荐数据、账号入口、更新横幅、未读通知和可选账号面板行为。把这些依赖集中在一起后，页面本身可以专注布局：
 * 顶部操作区、信息流列表、刷新入口和临时公告。
 */
data class HomeScreenRuntime(
    val account: HomeAccountState,
    val updateAnnouncement: HomeUpdateAnnouncement?,
    val installedAtLeastThreeHours: Boolean,
    val isDebuggable: Boolean,
    val viewModel: BaseFeedViewModel,
    val requestLogin: () -> Unit,
    val recordLocalItemOpened: (FeedDisplayItem) -> Unit,
    val recordLocalItemFeedback: (FeedDisplayItem, Double) -> Boolean,
)

@Composable
expect fun rememberHomeScreenRuntime(recommendationMode: RecommendationMode): HomeScreenRuntime

fun interface ArticleReadHistoryRecorder {
    suspend fun addReadHistory(article: Article)
}

@Composable
fun rememberArticleReadHistoryRecorder(): ArticleReadHistoryRecorder {
    val environment: HistoryEnvironment = rememberPaginationEnvironment(false)
    return remember(environment) {
        ArticleReadHistoryRecorder { article ->
            environment.addReadHistory(
                contentToken = article.id.toString(),
                contentTypeName = article.type.name.lowercase(),
            )
        }
    }
}

interface CommentScreenRuntime {
    fun saveImage(imageUrl: String)

    fun shareImage(imageUrl: String)
}

@Composable
expect fun rememberCommentScreenRuntime(): CommentScreenRuntime

@Composable
expect fun rememberCommentEmojiInlineContent(emojiKeys: Set<String>): Map<String, InlineTextContent>

expect fun commentEmojiInlineKey(placeholder: String): String?

expect fun Modifier.commentSelectionWorkaround(): Modifier

data class BlocklistSettingsRuntime(
    val requestImport: (((String) -> Unit) -> Unit),
    val exportRules: suspend () -> String,
)

@Composable
expect fun rememberBlocklistSettingsPlatformRuntime(
    userMessages: UserMessageSink,
): BlocklistSettingsRuntime

@Composable
expect fun rememberZhihuHttpClient(): HttpClient

expect fun Modifier.questionSelectionWorkaround(): Modifier
