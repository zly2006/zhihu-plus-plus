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

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.fleeksoft.ksoup.Ksoup
import com.github.zly2006.zhihu.data.decodePinContentDetail
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.resolveContent
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.officialBadge
import com.github.zly2006.zhihu.shared.platform.rememberExternalUrlOpener
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.util.formatCompactCount
import com.github.zly2006.zhihu.shared.util.twoDigitString
import com.github.zly2006.zhihu.ui.components.AuthorBadge
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import com.github.zly2006.zhihu.ui.components.ShareDialog
import com.github.zly2006.zhihu.ui.components.VotersSheet
import com.github.zly2006.zhihu.ui.components.getShareText
import com.github.zly2006.zhihu.ui.components.handleShareAction
import com.github.zly2006.zhihu.ui.components.rememberShareDialogRuntime
import com.github.zly2006.zhihu.viewmodel.ContentLoadEnvironment
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import com.github.zly2006.zhihu.viewmodel.deleteSigned
import com.github.zly2006.zhihu.viewmodel.loadVotersPage
import com.github.zly2006.zhihu.viewmodel.nextUrlOrNull
import com.github.zly2006.zhihu.viewmodel.postSigned
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.replaceOrAppendUniqueVoters
import io.ktor.client.call.body
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import androidx.compose.material.icons.outlined.ThumbUp as OutlinedThumbUp

const val PIN_SCREEN_BACK_BUTTON_TAG = "pin_screen_back_button"
const val PIN_SCREEN_SHARE_BUTTON_TAG = "pin_screen_share_button"
const val PIN_SCREEN_LOADING_TAG = "pin_screen_loading"
const val PIN_SCREEN_ERROR_TAG = "pin_screen_error"
const val PIN_SCREEN_SCROLL_TAG = "pin_screen_scroll"

private suspend fun togglePinLike(
    environment: ZhihuApiEnvironment,
    pin: Pin,
    isLiked: Boolean,
): PinLikeResult {
    val endpoint = "https://www.zhihu.com/api/v4/pins/${pin.id}/voters/up"
    val jojo = if (isLiked) {
        environment.deleteSigned(endpoint).body<JsonObject>()
    } else {
        environment.postSigned(endpoint).body<JsonObject>()
    }
    return PinLikeResult(
        isLiked = !isLiked,
        likeCount = jojo["liked_count"]?.jsonPrimitive?.intOrNull ?: -1,
    )
}

private suspend fun loadPinDetail(
    environment: ContentLoadEnvironment,
    pin: Pin,
): PinScreenUiState {
    environment.addReadHistory(pin.id.toString(), "pin")
    val jsonObject = environment.fetchJson("https://www.zhihu.com/api/v4/pins/${pin.id}", "")
        ?: error("想法详情为空")
    val content = decodePinContentDetail(jsonObject)
    environment.postHistoryDestination(pin)
    environment.recordContentOpenEvent(destination = pin)
    return PinScreenUiState(
        isLoading = false,
        pinContent = content,
        isLiked = content.virtuals.booleanCompat("isLiked", "is_liked"),
        likeCount = content.likeCount,
    )
}

const val PIN_SCREEN_AUTHOR_TAG = "pin_screen_author"
const val PIN_SCREEN_LINK_CARD_TAG = "pin_screen_link_card"
const val PIN_SCREEN_LIKE_BUTTON_TAG = "pin_screen_like_button"
const val PIN_SCREEN_COMMENT_BUTTON_TAG = "pin_screen_comment_button"

data class PinScreenTestOverrides(
    val state: PinScreenUiState,
    val onLikeClick: (() -> Unit)? = null,
    val onShareAction: ((showShareDialog: () -> Unit) -> Unit)? = null,
    val linkCardPreview: PinLinkCardPreview? = null,
    val commentScreenContent: (@Composable (showComments: Boolean, onDismiss: () -> Unit, content: Pin) -> Unit)? = null,
    val shareDialogContent: (
        @Composable (
            showDialog: Boolean,
            onDismissRequest: () -> Unit,
            content: Pin,
            shareText: String,
        ) -> Unit
    )? = null,
)

/**
 * 想法详情页。
 *
 * 页面展示想法正文、链接卡片、图片、评论入口、点赞和分享操作。正文渲染会受 WebView/Markdown 设置影响，图片长按菜单和评论弹窗
 * 也在这里串联，因此改动内容渲染或图片交互时要同时验证想法页。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinScreen(
    pin: Pin,
    testOverrides: PinScreenTestOverrides? = null,
) {
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()
    val paginationEnvironment = rememberPaginationEnvironment(allowGuestAccess = false)

    val settings = rememberSettingsStore()
    val shareRuntime = rememberShareDialogRuntime()
    var screenState by remember(pin.id, testOverrides) {
        mutableStateOf(
            testOverrides?.state ?: PinScreenUiState(isLoading = true),
        )
    }

    LaunchedEffect(pin.id, testOverrides) {
        if (testOverrides == null) {
            screenState = PinScreenUiState(isLoading = true)
            screenState = try {
                loadPinDetail(paginationEnvironment, pin)
            } catch (e: Exception) {
                PinScreenUiState(isLoading = false, errorMessage = e.message ?: "未知错误")
            }
        }
    }

    var showShareDialog by remember { mutableStateOf(false) }
    var showComments by rememberSaveable(pin.id) { mutableStateOf(false) }
    var showVoters by rememberSaveable(pin.id) { mutableStateOf(false) }
    var votersNextUrl by rememberSaveable(pin.id) { mutableStateOf<String?>(null) }
    var votersLoading by rememberSaveable(pin.id) { mutableStateOf(false) }
    var votersError by rememberSaveable(pin.id) { mutableStateOf<String?>(null) }
    val voters = remember(pin.id) { mutableStateListOf<DataHolder.Author>() }

    fun loadMoreVoters(reset: Boolean = false) {
        if (votersLoading) return
        coroutineScope.launch {
            votersLoading = true
            votersError = null
            try {
                val page = loadVotersPage(
                    environment = paginationEnvironment,
                    initialUrl = "https://www.zhihu.com/api/v4/pins/${pin.id}/upvoters?limit=10&offset=0",
                    nextUrl = votersNextUrl,
                    reset = reset,
                )
                voters.replaceOrAppendUniqueVoters(page.data, reset)
                val total = page.paging.totals.takeIf { it > 0 } ?: screenState.likeCount
                screenState = screenState.copy(likeCount = total)
                votersNextUrl = page.nextUrlOrNull()
            } catch (e: Exception) {
                votersError = e.message ?: "加载赞同者失败"
            } finally {
                votersLoading = false
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    val titleAuthor = screenState.pinContent?.author?.name
                    Text(
                        buildString {
                            if (titleAuthor != null) {
                                append(titleAuthor)
                                append("的")
                            }
                            append("想法")
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                navigationIcon = {
                    IconButton(
                        onClick = navigator.onNavigateBack,
                        modifier = Modifier.testTag(PIN_SCREEN_BACK_BUTTON_TAG),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val shareText = getShareText(pin)
                            if (shareText != null) {
                                testOverrides?.onShareAction?.invoke { showShareDialog = true }
                                    ?: handleShareAction(pin, settings, shareRuntime) {
                                        showShareDialog = true
                                    }
                            }
                        },
                        modifier = Modifier.testTag(PIN_SCREEN_SHARE_BUTTON_TAG),
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "分享")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                screenState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(PIN_SCREEN_LOADING_TAG),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                screenState.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "加载失败: ${screenState.errorMessage}",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.testTag(PIN_SCREEN_ERROR_TAG),
                        )
                    }
                }

                screenState.pinContent != null -> {
                    val pinContent = screenState.pinContent ?: return@Box
                    PinContent(
                        pin = pinContent,
                        isLiked = screenState.isLiked,
                        likeCount = screenState.likeCount,
                        onLikeClick = {
                            testOverrides?.onLikeClick?.invoke() ?: coroutineScope.launch {
                                val result = togglePinLike(paginationEnvironment, pin, screenState.isLiked)
                                screenState = screenState.copy(
                                    isLiked = result.isLiked,
                                    likeCount = result.likeCount,
                                )
                            }
                        },
                        onCommentClick = {
                            showComments = true
                        },
                        onSocialCreditClick = {
                            showVoters = true
                            if (voters.isEmpty()) {
                                loadMoreVoters(reset = true)
                            }
                        },
                        linkCardPreviewOverride = testOverrides?.linkCardPreview,
                    )

                    if (testOverrides?.commentScreenContent != null) {
                        testOverrides.commentScreenContent.invoke(
                            showComments,
                            { showComments = false },
                            pin,
                        )
                    } else if (showComments) {
                        CommentScreenComponent(
                            showComments = showComments,
                            onDismiss = { showComments = false },
                            content = pin,
                        )
                    }

                    val shareText = getShareText(pin)
                    if (shareText != null) {
                        if (testOverrides?.shareDialogContent != null) {
                            testOverrides.shareDialogContent.invoke(
                                showShareDialog,
                                { showShareDialog = false },
                                pin,
                                shareText,
                            )
                        } else {
                            ShareDialog(
                                content = pin,
                                shareText = shareText,
                                showDialog = showShareDialog,
                                onDismissRequest = { showShareDialog = false },
                            )
                        }
                    }

                    VotersSheet(
                        show = showVoters,
                        title = "${formatCompactCount(screenState.likeCount)} 人赞同了该想法",
                        voters = voters,
                        isLoading = votersLoading,
                        errorMessage = votersError,
                        canLoadMore = votersNextUrl != null,
                        onDismissRequest = { showVoters = false },
                        onLoadMore = { loadMoreVoters() },
                        onRetry = { loadMoreVoters(reset = voters.isEmpty()) },
                        onNavigate = { person ->
                            showVoters = false
                            navigator.onNavigate(person)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PinContent(
    pin: DataHolder.Pin,
    isLiked: Boolean,
    likeCount: Int,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onSocialCreditClick: () -> Unit,
    linkCardPreviewOverride: PinLinkCardPreview? = null,
) {
    val navigator = LocalNavigator.current
    val runtime = rememberPinScreenRuntime()
    val openExternalUrl = rememberExternalUrlOpener()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag(PIN_SCREEN_SCROLL_TAG)
            .padding(16.dp),
    ) {
        // 作者信息。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(PIN_SCREEN_AUTHOR_TAG)
                .clickable {
                    navigator.onNavigate(
                        Person(
                            id = pin.author.id,
                            urlToken = pin.author.urlToken,
                            name = pin.author.name,
                        ),
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = pin.author.avatarUrl,
                    contentDescription = "头像",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            pin.author.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        val authorBadge = pin.author.badgeV2.officialBadge()
                        if (authorBadge != null) {
                            Spacer(modifier = Modifier.width(4.dp))
                            AuthorBadge(authorBadge)
                        }
                    }
                    if (pin.author.headline.isNotEmpty()) {
                        Text(
                            pin.author.headline,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (!pin.selfCreate) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (pin.author.isFollowing) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                ) {
                    Text(
                        text = if (pin.author.isFollowing) "已关注" else "关注",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (pin.author.isFollowing) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            buildString {
                append("发布于")
                append(Instant.fromEpochSeconds(pin.created).toLocalDateTime(TimeZone.currentSystemDefault()).run { "$year-${(month.ordinal + 1).twoDigitString()}-${day.twoDigitString()} ${hour.twoDigitString()}:${minute.twoDigitString()}" })
                if (pin.updated > pin.created) {
                    append(" · 编辑于")
                    append(Instant.fromEpochSeconds(pin.updated).toLocalDateTime(TimeZone.currentSystemDefault()).run { "$year-${(month.ordinal + 1).twoDigitString()}-${day.twoDigitString()} ${hour.twoDigitString()}:${minute.twoDigitString()}" })
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (likeCount > 0) {
            // 社交证明。
            Spacer(modifier = Modifier.height(8.dp))
            val firstLiker = pin.likers.firstOrNull()
            Text(
                text = if (firstLiker != null) {
                    "${firstLiker.name} 等 ${formatCompactCount(likeCount)} 人赞同了该想法"
                } else {
                    "${formatCompactCount(likeCount)} 人赞同了该想法"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = onSocialCreditClick),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 想法正文。
        PinHtmlContent(pin.contentHtml)

        Spacer(modifier = Modifier.height(24.dp))

        val linkCard = pin.content.firstOrNull {
            it is DataHolder.Pin.ContentLinkCard
        } as? DataHolder.Pin.ContentLinkCard
        if (linkCard != null) {
            var relatedTitle by remember(linkCard.dataContentType, linkCard.dataContentId, linkCard.url, linkCardPreviewOverride) {
                mutableStateOf(linkCardPreviewOverride?.title)
            }
            var relatedPreview by remember(linkCard.dataContentType, linkCard.dataContentId, linkCard.url, linkCardPreviewOverride) {
                mutableStateOf(linkCardPreviewOverride?.preview)
            }
            var isRelatedLoading by remember(linkCard.dataContentType, linkCard.dataContentId, linkCard.url, linkCardPreviewOverride) {
                mutableStateOf(linkCardPreviewOverride == null)
            }

            LaunchedEffect(linkCard.dataContentType, linkCard.dataContentId, linkCard.url, linkCardPreviewOverride) {
                if (linkCardPreviewOverride != null) {
                    isRelatedLoading = false
                    return@LaunchedEffect
                }
                isRelatedLoading = true
                val preview = runtime.fetchLinkCardPreview(linkCard)
                relatedTitle = preview?.title
                relatedPreview = preview?.preview
                isRelatedLoading = false
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(PIN_SCREEN_LINK_CARD_TAG)
                    .clickable {
                        val targetUrl = linkCard.url.takeIf { it.isNotBlank() }
                        val destination = targetUrl?.let(::resolveContent)

                        if (destination != null) {
                            navigator.onNavigate(destination)
                        } else {
                            targetUrl?.let {
                                openExternalUrl(it)
                            }
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                ) {
                    Text(
                        "关联内容",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isRelatedLoading) {
                        Text(
                            text = "正在加载关联内容...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (!relatedTitle.isNullOrBlank()) {
                        Text(
                            text = relatedTitle!!,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!relatedPreview.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = relatedPreview!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else {
                        Text(
                            text = "${linkCardTypeLabel(linkCard.dataContentType)} · ${linkCard.dataContentId}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (relatedTitle.isNullOrBlank() && linkCard.url.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = linkCard.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 统计与操作区。
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            FilledTonalButton(
                onClick = onLikeClick,
                modifier = Modifier.testTag(PIN_SCREEN_LIKE_BUTTON_TAG),
            ) {
                Icon(
                    if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.OutlinedThumbUp,
                    contentDescription = "赞",
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "$likeCount",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            FilledTonalButton(
                onClick = onCommentClick,
                modifier = Modifier.testTag(PIN_SCREEN_COMMENT_BUTTON_TAG),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Comment,
                    contentDescription = "评论",
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "${pin.commentCount}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        // 话题列表。
        val topics = pin.topics
        if (topics?.isNotEmpty() == true) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "话题",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            pin.topics!!.forEach { topic ->
                Text(
                    "# ${topic.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}

private fun linkCardTypeLabel(dataContentType: String): String = when (dataContentType.lowercase()) {
    "answer" -> "回答"
    "article" -> "文章"
    "question" -> "问题"
    "pin" -> "想法"
    "people" -> "用户"
    "video", "zvideo" -> "视频"
    else -> dataContentType
}

private suspend fun fetchLinkCardPreview(
    fetchDetail: suspend (NavDestination) -> DataHolder.Content?,
    linkCard: DataHolder.Pin.ContentLinkCard,
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

internal fun resolveLinkCardDestination(linkCard: DataHolder.Pin.ContentLinkCard): NavDestination? {
    val byUrl = linkCard.url
        .takeIf { it.isNotBlank() }
        ?.let(::resolveContent)
    if (byUrl != null) return byUrl

    val contentId = linkCard.dataContentId
    return when (linkCard.dataContentType.lowercase()) {
        "answer" -> contentId.toLongOrNull()?.let { Article(type = ArticleType.Answer, id = it) }
        "article" -> contentId.toLongOrNull()?.let { Article(type = ArticleType.Article, id = it) }
        "question" -> contentId.toLongOrNull()?.let { Question(questionId = it) }
        "pin" -> contentId.toLongOrNull()?.let { Pin(id = it) }
        else -> null
    }
}

internal fun compactPreview(raw: String, maxLength: Int = 120): String {
    val plainText = Ksoup
        .parse(raw)
        .text()
        .replace(Regex("\\s+"), " ")
        .trim()
    if (plainText.length <= maxLength) return plainText
    return plainText.take(maxLength).trimEnd() + "..."
}

internal fun compactTitle(raw: String, maxLength: Int = 56): String = compactPreview(raw, maxLength)

data class PinScreenUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val pinContent: DataHolder.Pin? = null,
    val isLiked: Boolean = false,
    val likeCount: Int = 0,
)

data class PinLinkCardPreview(
    val title: String,
    val preview: String,
)
