/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Navigator
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.navDestination
import com.github.zly2006.zhihu.shared.data.officialBadge
import com.github.zly2006.zhihu.shared.platform.rememberExternalUrlOpener
import com.github.zly2006.zhihu.shared.platform.rememberIsLiteVariant
import com.github.zly2006.zhihu.shared.platform.rememberSettingBoolean
import com.github.zly2006.zhihu.shared.platform.rememberSettingInt
import com.github.zly2006.zhihu.shared.platform.rememberSettingString
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.theme.AppTokens
import com.github.zly2006.zhihu.ui.components.AuthorBadge
import com.github.zly2006.zhihu.ui.subscreens.DUO3_CARD_LARGE_TITLE_PREFERENCE_KEY
import com.github.zly2006.zhihu.util.parseHtmlTextWithTheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.LocalDismissState
import top.yukonga.miuix.kmp.window.WindowListPopup
import kotlin.math.abs

@Composable
fun MiuixFeedCard(
    item: FeedDisplayItem,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 240.dp,
    thumbnailUrl: String? = null,
    // 与 miuix 设置页 Card 的 horizontal=12.dp 对齐，让信息流卡片左右边缘与设置卡片一致。
    horizontalPadding: Dp = 12.dp,
    // 标题最大行数。热榜等标题即正文的场景可调大，避免长标题被截断。
    titleMaxLines: Int = 2,
    onLike: ((FeedDisplayItem) -> Unit)? = null,
    onDislike: ((FeedDisplayItem) -> Unit)? = null,
    onBlockUser: ((FeedDisplayItem) -> Unit)? = null,
    onBlockByKeywords: ((FeedDisplayItem) -> Unit)? = null,
    onBlockTopic: ((topicId: String, topicName: String) -> Unit)? = null,
    showSourceLabel: Boolean = false,
    onClick: (FeedDisplayItem.() -> Unit)? = null,
) {
    val density = LocalDensity.current
    val navigator = LocalNavigator.current
    val settings = rememberSettingsStore()
    val openUrl = rememberExternalUrlOpener()
    val userMessages = rememberUserMessageSink()
    var offsetX by remember { mutableFloatStateOf(0f) }
    var currentY by remember { mutableFloatStateOf(0f) }
    var startY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val coroutineScope = remember { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main) }
    // 直读（不 remember）：设置项改动后返回信息流即重组生效，避免被 remember 缓存住。
    val enableSwipeReaction = rememberSettingBoolean("enableSwipeReaction", false, settings) && onLike != null && onDislike != null
    val showFeedThumbnail = rememberSettingBoolean("showFeedThumbnail", true, settings)
    val feedCardStyle = rememberSettingString("feedCardStyle", "card", settings)
    val duo3CardAppearance = rememberSettingBoolean("duo3_card_appearance", false, settings)
    val duo3CardLayout = rememberSettingBoolean("duo3_card_layout", false, settings)
    val duo3CardLargeTitle = rememberSettingBoolean(DUO3_CARD_LARGE_TITLE_PREFERENCE_KEY, true, settings)
    val fontSizePercent = rememberSettingInt("contentFontSize", 100, settings)
    val titleFontSize = (15.sp * fontSizePercent / 100)

    val resolvedOnClick: FeedDisplayItem.() -> Unit = onClick ?: {
        val self = this
        val content = self.content
        self.navDestination?.let { navigator.onNavigate(it) }
            ?: if (content?.startsWith("http") == true) {
                openUrl(content)
            } else {
                userMessages.showShortMessage("暂不支持打开该内容")
            }
    }

    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isDragging) offsetX else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "offset",
    )
    val actionAlpha by animateFloatAsState(
        targetValue = if (abs(animatedOffsetX) > 50f) (abs(animatedOffsetX) - 50f) / 100f else 0f,
        animationSpec = tween(150),
        label = "actionAlpha",
    )
    val currentAction = when {
        abs(animatedOffsetX) < 75f -> "none"
        currentY - startY < -30f -> "like"
        currentY - startY > 30f -> "dislike"
        else -> "neutral"
    }

    if (feedCardStyle == "divider") {
        Column(modifier = modifier.fillMaxWidth().heightIn(max = maxHeight)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { resolvedOnClick(item) }
                    .padding(horizontal = horizontalPadding, vertical = 8.dp),
            ) {
                MiuixFeedCardContent(
                    item,
                    showFeedThumbnail,
                    thumbnailUrl,
                    duo3CardLayout,
                    duo3CardLargeTitle,
                    titleFontSize,
                    titleMaxLines,
                    showMenu,
                    { showMenu = it },
                    onBlockUser,
                    onBlockByKeywords,
                    onBlockTopic,
                    showSourceLabel,
                    navigator,
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .padding(horizontal = horizontalPadding, vertical = 6.dp),
        ) {
            // miuix Card — 点击进入详情；swipe 反应手势与点击共存
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { resolvedOnClick(item) }
                    .alpha(1 - kotlin.math.min(actionAlpha, 0.5f))
                    .offset(x = with(density) { animatedOffsetX.toDp() })
                    .let { mod ->
                        if (enableSwipeReaction) {
                            mod.pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragStart = { offset ->
                                        isDragging = true
                                        startY = offset.y
                                        currentY = offset.y
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        when {
                                            abs(offsetX) >= 75f && currentY - startY < -30f -> onLike(item)
                                            abs(offsetX) >= 75f && currentY - startY > 30f -> onDislike(item)
                                        }
                                        kotlinx.coroutines.runBlocking {
                                            offsetX = 0f
                                            currentY = 0f
                                            startY = 0f
                                        }
                                    },
                                ) { change, dragAmount ->
                                    currentY = change.position.y
                                    offsetX = kotlin.math.max(offsetX + dragAmount, -250f).coerceAtMost(0f)
                                }
                            }
                        } else {
                            mod
                        }
                    },
            ) {
                Column(modifier = Modifier.padding(if (duo3CardAppearance) 16.dp else 12.dp)) {
                    MiuixFeedCardContent(
                        item,
                        showFeedThumbnail,
                        thumbnailUrl,
                        duo3CardLayout,
                        duo3CardLargeTitle,
                        titleFontSize,
                        titleMaxLines,
                        showMenu,
                        { showMenu = it },
                        onBlockUser,
                        onBlockByKeywords,
                        onBlockTopic,
                        showSourceLabel,
                        navigator,
                    )
                }
            }

            if (actionAlpha > 0f && enableSwipeReaction) {
                Box(
                    modifier = Modifier.matchParentSize().background(
                        color = when (currentAction) {
                            "like" -> Color(0xFF4CAF50).copy(alpha = actionAlpha * 0.2f)
                            "dislike" -> Color(0xFFFF5722).copy(alpha = actionAlpha * 0.2f)
                            "neutral" -> Color(0xFF9E9E9E).copy(alpha = actionAlpha * 0.1f)
                            else -> Color.Transparent
                        },
                        shape = RoundedCornerShape(12.dp),
                    ),
                    contentAlignment = when (currentAction) {
                        "like" -> Alignment.TopStart
                        "dislike" -> Alignment.BottomStart
                        else -> Alignment.CenterStart
                    },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        when (currentAction) {
                            "like" -> {
                                Icon(MiuixIconsEmbedded.FavoritesFill, "喜欢", tint = Color(0xFF4CAF50), modifier = Modifier.size(32.dp).scale(1f + actionAlpha * 0.3f))
                                Spacer(Modifier.width(12.dp))
                                Text("向上滑动 - 喜欢", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.scale(1f + actionAlpha * 0.2f))
                            }
                            "dislike" -> {
                                Icon(MiuixIconsEmbedded.Report, "不喜欢", tint = Color(0xFFFF5722), modifier = Modifier.size(32.dp).scale(1f + actionAlpha * 0.3f))
                                Spacer(Modifier.width(12.dp))
                                Text("向下滑动 - 不喜欢", color = Color(0xFFFF5722), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.scale(1f + actionAlpha * 0.2f))
                            }
                            "neutral" -> {
                                Text("上下滑动选择", color = Color(0xFF9E9E9E), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.scale(1f + actionAlpha * 0.2f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiuixFeedCardContent(
    item: FeedDisplayItem,
    showFeedThumbnail: Boolean,
    thumbnailUrl: String?,
    duo3CardLayout: Boolean,
    duo3CardLargeTitle: Boolean,
    titleFontSize: androidx.compose.ui.unit.TextUnit,
    titleMaxLines: Int,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    onBlockUser: ((FeedDisplayItem) -> Unit)?,
    onBlockByKeywords: ((FeedDisplayItem) -> Unit)?,
    onBlockTopic: ((topicId: String, topicName: String) -> Unit)?,
    showSourceLabel: Boolean,
    navigator: Navigator,
) {
    val colors = AppTokens.colors
    val text = AppTokens.text

    // 赞同数（details）+ 「更多」菜单按钮内联一行，对齐 M3 FeedCard。
    // details 允许换行（不设 maxLines），避免长文案被截断。
    @Composable
    fun DetailsWithMenuRow() {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (item.details.isNotEmpty()) {
                Text(
                    item.details,
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            MiuixFeedCardMenuBox(item, showMenu, onShowMenuChange, onBlockUser, onBlockByKeywords, onBlockTopic, navigator)
        }
    }

    if (duo3CardLayout) {
        if (showSourceLabel) {
            MiuixFeedCardSourceLabel(item.sourceLabel)
        }
        if (!item.title.isEmpty()) {
            Text(
                text = parseHtmlTextWithTheme(item.title),
                fontSize = titleFontSize,
                fontWeight = FontWeight.Bold,
                maxLines = titleMaxLines,
                color = colors.onSurface,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Column {
            Row {
                Text(
                    text = parseHtmlTextWithTheme(item.summary ?: ""),
                    style = text.bodyMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (!thumbnailUrl.isNullOrEmpty() && showFeedThumbnail && !item.isFiltered) {
                    Spacer(Modifier.width(8.dp))
                    AsyncImage(
                        thumbnailUrl,
                        "缩略图",
                        modifier = Modifier.padding(top = 8.dp).sizeIn(maxHeight = 80.dp, maxWidth = 128.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.FillHeight,
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (item.avatarSrc != null && item.authorName != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                        AsyncImage(item.avatarSrc, "头像", modifier = Modifier.clip(CircleShape).size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(item.authorName, style = text.labelMedium, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        val badge = item.authorBadgeV2.officialBadge()
                        if (badge?.isUsefulInList == true) {
                            Spacer(Modifier.width(4.dp))
                            AuthorBadge(badge, compact = true)
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                }
                if (item.details.isNotEmpty()) {
                    Text(item.details, style = text.labelMedium, color = colors.onSurfaceVariant, maxLines = 2, modifier = Modifier.weight(1f))
                } else {
                    Spacer(Modifier.weight(1f))
                }
                MiuixFeedCardMenuBox(item, showMenu, onShowMenuChange, onBlockUser, onBlockByKeywords, onBlockTopic, navigator)
            }
        }
    } else {
        if (showSourceLabel) {
            MiuixFeedCardSourceLabel(item.sourceLabel)
        }
        if (!item.title.isEmpty() && !item.isFiltered) {
            Text(parseHtmlTextWithTheme(item.title), fontSize = titleFontSize, fontWeight = FontWeight.Bold, maxLines = titleMaxLines, overflow = TextOverflow.Ellipsis)
        }
        if (item.avatarSrc != null && item.authorName != null) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(item.avatarSrc, "头像", modifier = Modifier.clip(CircleShape).size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(item.authorName, fontSize = 13.sp, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                val badge = item.authorBadgeV2.officialBadge()
                if (badge?.isUsefulInList == true) {
                    Spacer(Modifier.width(4.dp))
                    AuthorBadge(badge, compact = true)
                }
            }
        }
        Row {
            Column(Modifier.weight(2f)) {
                Text(parseHtmlTextWithTheme(item.summary ?: ""), fontSize = 13.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = if (item.isFiltered) 0.dp else 3.dp))
                DetailsWithMenuRow()
            }
            if (!thumbnailUrl.isNullOrEmpty() && showFeedThumbnail) {
                Spacer(Modifier.width(8.dp))
                AsyncImage(thumbnailUrl, "缩略图", modifier = Modifier.weight(1f).sizeIn(maxWidth = 60.dp).clip(RoundedCornerShape(8.dp)))
            }
        }
    }
}

@Composable
private fun MiuixFeedCardSourceLabel(sourceLabel: String?) {
    val label = sourceLabel?.takeIf { it.isNotBlank() } ?: return
    Text(
        parseHtmlTextWithTheme(label),
        style = AppTokens.text.labelMedium,
        color = AppTokens.colors.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun MiuixFeedCardMenuBox(
    item: FeedDisplayItem,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    onBlockUser: ((FeedDisplayItem) -> Unit)?,
    onBlockByKeywords: ((FeedDisplayItem) -> Unit)?,
    onBlockTopic: ((topicId: String, topicName: String) -> Unit)?,
    navigator: Navigator,
) {
    // 菜单项 (标题, 动作)，与 M3 FeedCardMenuBox 完全对齐
    val isLiteVariant = rememberIsLiteVariant()
    val topics = if (onBlockTopic != null && item.raw != null) {
        when (val raw = item.raw) {
            is DataHolder.Answer -> raw.question.topics
            is DataHolder.Question -> raw.topics
            is DataHolder.Article -> raw.topics ?: emptyList()
            else -> emptyList()
        }
    } else {
        emptyList()
    }
    val entries = buildList<Pair<String, () -> Unit>> {
        if (onBlockByKeywords != null && !isLiteVariant) {
            add("按关键词屏蔽" to { onBlockByKeywords(item) })
        }
        add("屏蔽用户" to { onBlockUser?.invoke(item) })
        topics.forEach { topic -> add("屏蔽「${topic.name}」" to { onBlockTopic!!(topic.id, topic.name) }) }
        add("外观设置" to { navigator.onNavigate(Account.AppearanceSettings()) })
        if (item.isFiltered) {
            add("不再屏蔽低赞内容" to { navigator.onNavigate(Account.RecommendSettings("enableQualityFilter")) })
        }
    }

    Box {
        IconButton(onClick = { onShowMenuChange(true) }, modifier = Modifier.size(24.dp)) {
            Icon(MiuixIconsEmbedded.More, "更多选项", tint = AppTokens.colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
        WindowListPopup(
            show = showMenu,
            popupPositionProvider = ListPopupDefaults.MenuPositionProvider,
            alignment = PopupPositionProvider.Align.TopEnd,
            onDismissRequest = { onShowMenuChange(false) },
        ) {
            val dismissState = LocalDismissState.current
            ListPopupColumn {
                entries.forEach { (title, action) ->
                    Text(
                        text = title,
                        modifier = Modifier
                            .clickable {
                                dismissState?.invoke()
                                onShowMenuChange(false)
                                action()
                            }.padding(horizontal = 16.dp, vertical = 14.dp),
                        color = AppTokens.colors.onSurface,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}
