/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.data.decodePinContentDetail
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.resolveContent
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.platform.rememberExternalUrlOpener
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.util.formatCompactCount
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.PIN_SCREEN_AUTHOR_TAG
import com.github.zly2006.zhihu.ui.PIN_SCREEN_COMMENT_BUTTON_TAG
import com.github.zly2006.zhihu.ui.PIN_SCREEN_ERROR_TAG
import com.github.zly2006.zhihu.ui.PIN_SCREEN_LIKE_BUTTON_TAG
import com.github.zly2006.zhihu.ui.PIN_SCREEN_LINK_CARD_TAG
import com.github.zly2006.zhihu.ui.PIN_SCREEN_LOADING_TAG
import com.github.zly2006.zhihu.ui.PIN_SCREEN_SCROLL_TAG
import com.github.zly2006.zhihu.ui.PinHtmlContent
import com.github.zly2006.zhihu.ui.PinLikeResult
import com.github.zly2006.zhihu.ui.PinLinkCardPreview
import com.github.zly2006.zhihu.ui.PinScreenTestOverrides
import com.github.zly2006.zhihu.ui.PinScreenUiState
import com.github.zly2006.zhihu.ui.booleanCompat
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import com.github.zly2006.zhihu.ui.components.ShareDialog
import com.github.zly2006.zhihu.ui.components.VotersSheet
import com.github.zly2006.zhihu.ui.components.getShareText
import com.github.zly2006.zhihu.ui.components.handleShareAction
import com.github.zly2006.zhihu.ui.components.rememberShareDialogRuntime
import com.github.zly2006.zhihu.ui.linkCardTypeLabel
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import com.github.zly2006.zhihu.ui.rememberPinScreenRuntime
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
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

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

@Composable
fun MiuixPinScreen(
    pin: Pin,
    testOverrides: PinScreenTestOverrides? = null,
) {
    val navigator = LocalNavigator.current
    val paginationEnvironment = rememberPaginationEnvironment(allowGuestAccess = false)
    val settings = rememberSettingsStore()
    val shareRuntime = rememberShareDialogRuntime()
    val coroutineScope = rememberCoroutineScope()
    val blurEnabled = remember { mutableStateOf(settings.getBoolean("blurEnabled", true)) }
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled.value)
    val scrollBehavior = MiuixScrollBehavior()

    var screenState by remember(pin.id, testOverrides) {
        mutableStateOf(testOverrides?.state ?: PinScreenUiState(isLoading = true))
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
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(backdrop),
                color = backdrop.getMiuixAppBarColor(),
                title = buildString {
                    screenState.pinContent?.author?.let { append(it.name).append("的") }
                    append("想法")
                },
                navigationIcon = {
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(MiuixIconsEmbedded.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareText = getShareText(pin)
                        if (shareText != null) {
                            testOverrides?.onShareAction?.invoke { showShareDialog = true }
                                ?: handleShareAction(pin, settings, shareRuntime) { showShareDialog = true }
                        }
                    }) {
                        Icon(Icons.Default.Share, "分享", tint = MiuixTheme.colorScheme.onBackground)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        ) {
            when {
                screenState.isLoading -> Box(Modifier.fillMaxSize().testTag(PIN_SCREEN_LOADING_TAG), Alignment.Center) {
                    CircularProgressIndicator()
                }

                screenState.errorMessage != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("加载失败: ${screenState.errorMessage}", color = MiuixTheme.colorScheme.onBackground, modifier = Modifier.testTag(PIN_SCREEN_ERROR_TAG))
                }

                screenState.pinContent != null -> MiuixPinContent(
                    pin = screenState.pinContent ?: return@Box,
                    isLiked = screenState.isLiked,
                    likeCount = screenState.likeCount,
                    onLikeClick = {
                        testOverrides?.onLikeClick?.invoke() ?: coroutineScope.launch {
                            val result = togglePinLike(paginationEnvironment, pin, screenState.isLiked)
                            screenState = screenState.copy(isLiked = result.isLiked, likeCount = result.likeCount)
                        }
                    },
                    onCommentClick = { showComments = true },
                    onSocialCreditClick = {
                        showVoters = true
                        if (voters.isEmpty()) {
                            loadMoreVoters(reset = true)
                        }
                    },
                    linkCardPreviewOverride = testOverrides?.linkCardPreview,
                )
            }
        }
    }

    if (screenState.pinContent != null) {
        // TODO: 评论区尚未 miuix 化，暂复用 M3 CommentScreenComponent（与问题页一致）
        testOverrides?.commentScreenContent?.invoke(showComments, { showComments = false }, pin)
            ?: CommentScreenComponent(showComments = showComments, onDismiss = { showComments = false }, content = pin)

        val shareText = getShareText(pin)
        if (shareText != null) {
            testOverrides?.shareDialogContent?.invoke(showShareDialog, { showShareDialog = false }, pin, shareText)
                ?: ShareDialog(
                    content = pin,
                    shareText = shareText,
                    showDialog = showShareDialog,
                    onDismissRequest = { showShareDialog = false },
                )
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

@Composable
private fun MiuixPinContent(
    pin: DataHolder.Pin,
    isLiked: Boolean,
    likeCount: Int,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onSocialCreditClick: () -> Unit,
    linkCardPreviewOverride: PinLinkCardPreview? = null,
) {
    val navigator = LocalNavigator.current
    val formattedDate = remember(pin.created) {
        Instant.fromEpochSeconds(pin.created).toLocalDateTime(TimeZone.currentSystemDefault()).run {
            "$year-${(month.ordinal + 1).toString().padStart(2, '0')}-${day.toString().padStart(2, '0')} " +
                "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .testTag(PIN_SCREEN_SCROLL_TAG)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 作者信息
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(PIN_SCREEN_AUTHOR_TAG)
                        .clickable {
                            navigator.onNavigate(Person(id = pin.author.id, urlToken = pin.author.urlToken, name = pin.author.name))
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(pin.author.avatarUrl, "头像", modifier = Modifier.size(48.dp).clip(CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(pin.author.name, style = MiuixTheme.textStyles.title4, fontWeight = FontWeight.Bold)
                        if (pin.author.headline.isNotEmpty()) {
                            Text(pin.author.headline, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(formattedDate, style = MiuixTheme.textStyles.footnote1, color = MiuixTheme.colorScheme.onSurfaceSecondary)
                if (likeCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    val firstLiker = pin.likers.firstOrNull()
                    Text(
                        text = if (firstLiker != null) {
                            "${firstLiker.name} 等 ${formatCompactCount(likeCount)} 人赞同了该想法"
                        } else {
                            "${formatCompactCount(likeCount)} 人赞同了该想法"
                        },
                        style = MiuixTheme.textStyles.footnote1,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        modifier = Modifier.clickable(onClick = onSocialCreditClick),
                    )
                }
                Spacer(Modifier.height(12.dp))

                // 正文
                PinHtmlContent(pin.contentHtml)
            }
        }

        // 关联内容卡片
        val linkCard = pin.content.firstOrNull { it is DataHolder.Pin.ContentLinkCard } as? DataHolder.Pin.ContentLinkCard
        if (linkCard != null) {
            Spacer(Modifier.height(12.dp))
            MiuixPinLinkCard(linkCard, linkCardPreviewOverride)
        }

        // 操作栏
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(onClick = onLikeClick, modifier = Modifier.weight(1f).testTag(PIN_SCREEN_LIKE_BUTTON_TAG), colors = ButtonDefaults.buttonColorsPrimary()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        "赞",
                        modifier = Modifier.size(18.dp),
                        tint = MiuixTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("$likeCount", color = MiuixTheme.colorScheme.onPrimary)
                }
            }
            Button(onClick = onCommentClick, modifier = Modifier.weight(1f).testTag(PIN_SCREEN_COMMENT_BUTTON_TAG), colors = ButtonDefaults.buttonColorsPrimary()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.AutoMirrored.Filled.Comment, "评论", modifier = Modifier.size(18.dp), tint = MiuixTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text("${pin.commentCount}", color = MiuixTheme.colorScheme.onPrimary)
                }
            }
        }

        // 话题
        if (pin.topics?.isNotEmpty() == true) {
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("话题", style = MiuixTheme.textStyles.title4, fontWeight = FontWeight.Bold)
                    pin.topics.forEach { topic ->
                        Text(
                            "# ${topic.name}",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MiuixPinLinkCard(
    linkCard: DataHolder.Pin.ContentLinkCard,
    linkCardPreviewOverride: PinLinkCardPreview?,
) {
    val navigator = LocalNavigator.current
    val runtime = rememberPinScreenRuntime()
    val openExternalUrl = rememberExternalUrlOpener()
    var relatedTitle by remember(linkCard, linkCardPreviewOverride) { mutableStateOf(linkCardPreviewOverride?.title) }
    var relatedPreview by remember(linkCard, linkCardPreviewOverride) { mutableStateOf(linkCardPreviewOverride?.preview) }
    var isRelatedLoading by remember(linkCard, linkCardPreviewOverride) { mutableStateOf(linkCardPreviewOverride == null) }

    LaunchedEffect(linkCard, linkCardPreviewOverride) {
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
                    targetUrl?.let { openExternalUrl(it) }
                }
            },
        colors = CardDefaults.defaultColors(color = MiuixTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("关联内容", style = MiuixTheme.textStyles.subtitle, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            when {
                isRelatedLoading -> Text("正在加载关联内容...", style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceSecondary)
                !relatedTitle.isNullOrBlank() -> {
                    Text(relatedTitle!!, style = MiuixTheme.textStyles.title4, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (!relatedPreview.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(relatedPreview!!, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurfaceSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }

                else -> Text("${linkCardTypeLabel(linkCard.dataContentType)} · ${linkCard.dataContentId}", style = MiuixTheme.textStyles.body2)
            }
            if (relatedTitle.isNullOrBlank() && linkCard.url.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(linkCard.url, fontSize = 12.sp, color = MiuixTheme.colorScheme.primary)
            }
        }
    }
}
