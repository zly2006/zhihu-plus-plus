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
import androidx.navigation.NavHostController
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
import com.github.zly2006.zhihu.shared.data.mcnCompany
import com.github.zly2006.zhihu.shared.data.officialBadge
import com.github.zly2006.zhihu.shared.data.officialBadgeDetails
import com.github.zly2006.zhihu.shared.filter.ContentOpenFrom
import com.github.zly2006.zhihu.shared.platform.SettingsStore
import com.github.zly2006.zhihu.shared.platform.UserMessageSink
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.ui.ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY
import com.github.zly2006.zhihu.shared.ui.AnswerDoubleTapAction
import com.github.zly2006.zhihu.ui.PeopleProfileUiState
import com.github.zly2006.zhihu.ui.PinLinkCardPreview
import com.github.zly2006.zhihu.ui.QuestionScreenUiState
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import com.github.zly2006.zhihu.ui.components.ShareDialogRuntime
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel.CachedAnswerContent
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.filter.normalizeMcnCompany
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import io.ktor.client.HttpClient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

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

@Composable
fun PinCommentsSheet(
    showComments: Boolean,
    onDismiss: () -> Unit,
    content: Pin,
) {
    CommentScreenComponent(
        showComments = showComments,
        onDismiss = onDismiss,
        content = content,
    )
}

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

@Composable
fun ArticleMarkdownContent(
    html: String,
    modifier: Modifier,
    header: @Composable () -> Unit,
    footer: @Composable () -> Unit,
) {
    RenderMarkdown(
        html = html,
        modifier = modifier,
        selectable = true,
        enableScroll = false,
        header = header,
        footer = footer,
    )
}

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
fun QuestionCommentsSheet(
    showComments: Boolean,
    onDismiss: () -> Unit,
    content: Question,
) {
    CommentScreenComponent(
        showComments = showComments,
        onDismiss = onDismiss,
        content = content,
    )
}

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

data class ZhihuMainPreferenceSnapshot(
    val duo3HomeAccount: Boolean,
    val duo3NavStyle: Boolean,
    val tapToScrollToTopEnabled: Boolean,
    val autoHideBottomBar: Boolean,
    val selectedBottomBarItemKeys: Set<String>,
    val startDestination: TopLevelDestination,
)

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
        selectedBottomBarItemKeys = snapshot.selectedBottomBarItemKeys
        startDestination = snapshot.startDestination
    }
}

@Composable
fun rememberZhihuMainPreferenceState(
    readSnapshot: () -> ZhihuMainPreferenceSnapshot,
): ZhihuMainPreferenceState = remember { ZhihuMainPreferenceState(readSnapshot) }

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
        mcnCompany = loadedPerson.badgeV2.mcnCompany().normalizeMcnCompany(),
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
    val environment = rememberPaginationEnvironment(false)
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
