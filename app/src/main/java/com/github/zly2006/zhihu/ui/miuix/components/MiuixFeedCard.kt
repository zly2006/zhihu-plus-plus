/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.components

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ThumbDown
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.data.officialBadge
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Navigator
import com.github.zly2006.zhihu.theme.AppTokens
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.components.AuthorBadge
import com.github.zly2006.zhihu.ui.subscreens.DUO3_CARD_LARGE_TITLE_PREFERENCE_KEY
import com.github.zly2006.zhihu.util.parseHtmlTextWithTheme
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.menu.WindowDropdownMenu

@Composable
fun MiuixFeedCard(
    item: BaseFeedViewModel.FeedDisplayItem,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 240.dp,
    thumbnailUrl: String? = null,
    horizontalPadding: Dp = 12.dp,
    onLike: ((BaseFeedViewModel.FeedDisplayItem) -> Unit)? = null,
    onDislike: ((BaseFeedViewModel.FeedDisplayItem) -> Unit)? = null,
    onBlockUser: ((BaseFeedViewModel.FeedDisplayItem) -> Unit)? = null,
    onBlockByKeywords: ((BaseFeedViewModel.FeedDisplayItem) -> Unit)? = null,
    onBlockTopic: ((topicId: String, topicName: String) -> Unit)? = null,
    onClick: (BaseFeedViewModel.FeedDisplayItem.() -> Unit)? = null,
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    var currentY by remember { mutableFloatStateOf(0f) }
    var startY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val coroutineScope = remember { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main) }
    val preferences = remember { context.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE) }
    val enableSwipeReaction = remember { preferences.getBoolean("enableSwipeReaction", false) } && onLike != null && onDislike != null
    val showFeedThumbnail = remember { preferences.getBoolean("showFeedThumbnail", true) }
    val feedCardStyle = remember { preferences.getString("feedCardStyle", "card") }
    val duo3CardAppearance = remember { preferences.getBoolean("duo3_card_appearance", false) }
    val duo3CardLayout = remember { preferences.getBoolean("duo3_card_layout", false) }
    val duo3CardLargeTitle = remember { preferences.getBoolean(DUO3_CARD_LARGE_TITLE_PREFERENCE_KEY, true) }

    val resolvedOnClick: BaseFeedViewModel.FeedDisplayItem.() -> Unit = onClick ?: {
        val self = this
        self.navDestination?.let { navigator.onNavigate(it) }
            ?: if (self.content?.startsWith("http") == true) {
                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse(self.content)
                })
            } else { Toast.makeText(context, "暂不支持打开该内容", Toast.LENGTH_SHORT).show() }
    }

    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isDragging) offsetX else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "offset",
    )
    val actionAlpha by animateFloatAsState(
        targetValue = if (abs(animatedOffsetX) > 50f) (abs(animatedOffsetX) - 50f) / 100f else 0f,
        animationSpec = tween(150), label = "actionAlpha",
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
                modifier = Modifier.fillMaxWidth()
                    .clickable { resolvedOnClick(item) }
                    .padding(horizontal = horizontalPadding, vertical = 12.dp),
            ) {
                MiuixFeedCardContent(item, showFeedThumbnail, thumbnailUrl, duo3CardLayout, duo3CardLargeTitle)
            }
        }
    } else {
        Box(
            modifier = modifier.fillMaxWidth().heightIn(max = maxHeight)
                .padding(horizontal = horizontalPadding, vertical = 8.dp),
        ) {
            // miuix Card — NO clickable, NO internal padding (caller controls both)
            Card(
                modifier = Modifier.fillMaxWidth()
                    .alpha(1 - kotlin.math.min(actionAlpha, 0.5f))
                    .offset(x = with(density) { animatedOffsetX.toDp() })
                    .let { mod ->
                        if (enableSwipeReaction) {
                            mod.pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragStart = { offset -> isDragging = true; startY = offset.y; currentY = offset.y },
                                    onDragEnd = {
                                        isDragging = false
                                        when {
                                            abs(offsetX) >= 75f && currentY - startY < -30f -> onLike?.invoke(item)
                                            abs(offsetX) >= 75f && currentY - startY > 30f -> onDislike?.invoke(item)
                                        }
                                        kotlinx.coroutines.runBlocking { offsetX = 0f; currentY = 0f; startY = 0f }
                                    },
                                ) { change, dragAmount ->
                                    currentY = change.position.y
                                    offsetX = kotlin.math.max(offsetX + dragAmount, -250f).coerceAtMost(0f)
                                }
                            }
                        } else mod
                    },
            ) {
                Column(modifier = Modifier.padding(if (duo3CardAppearance) 16.dp else 12.dp)) {
                    MiuixFeedCardContent(item, showFeedThumbnail, thumbnailUrl, duo3CardLayout, duo3CardLargeTitle)
                    // menu button row
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                        MiuixFeedCardMenuBox(item, showMenu, { showMenu = it }, onBlockUser, onBlockByKeywords, onBlockTopic, navigator)
                    }
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
                        }, shape = RoundedCornerShape(12.dp),
                    ), contentAlignment = when (currentAction) {
                        "like" -> Alignment.TopStart; "dislike" -> Alignment.BottomStart; else -> Alignment.CenterStart
                    },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        when (currentAction) {
                            "like" -> {
                                Icon(Icons.Default.Favorite, "喜欢", tint = Color(0xFF4CAF50), modifier = Modifier.size(32.dp).scale(1f + actionAlpha * 0.3f))
                                Spacer(Modifier.width(12.dp))
                                Text("向上滑动 - 喜欢", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.scale(1f + actionAlpha * 0.2f))
                            }
                            "dislike" -> {
                                Icon(Icons.Default.ThumbDown, "不喜欢", tint = Color(0xFFFF5722), modifier = Modifier.size(32.dp).scale(1f + actionAlpha * 0.3f))
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
    item: BaseFeedViewModel.FeedDisplayItem,
    showFeedThumbnail: Boolean,
    thumbnailUrl: String?,
    duo3CardLayout: Boolean,
    duo3CardLargeTitle: Boolean,
) {
    val colors = AppTokens.colors
    val text = AppTokens.text

    if (duo3CardLayout) {
        if (!item.title.isEmpty()) {
            Text(
                text = parseHtmlTextWithTheme(item.title),
                style = if (duo3CardLargeTitle) text.titleLarge else text.titleMedium,
                maxLines = 2, color = colors.onSurface, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Column {
            Row {
                Text(
                    text = parseHtmlTextWithTheme(item.summary ?: ""),
                    style = text.bodyMedium, color = colors.onSurfaceVariant,
                    maxLines = 4, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (!thumbnailUrl.isNullOrEmpty() && showFeedThumbnail && !item.isFiltered) {
                    Spacer(Modifier.width(8.dp))
                    AsyncImage(thumbnailUrl, "缩略图",
                        modifier = Modifier.padding(top = 8.dp).sizeIn(maxHeight = 80.dp, maxWidth = 128.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.FillHeight)
                }
            }
            if (item.details.isNotEmpty() || (item.avatarSrc != null && item.authorName != null)) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (item.avatarSrc != null && item.authorName != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                            AsyncImage(item.avatarSrc, "头像", modifier = Modifier.clip(CircleShape).size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(item.authorName, style = text.labelMedium, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            val badge = item.authorBadgeV2.officialBadge()
                            if (badge?.isUsefulInList == true) { Spacer(Modifier.width(4.dp)); AuthorBadge(badge, compact = true) }
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    if (item.details.isNotEmpty()) {
                        Text(item.details, style = text.labelMedium, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    } else {
        if (!item.title.isEmpty() && !item.isFiltered) {
            Text(parseHtmlTextWithTheme(item.title), fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (item.avatarSrc != null && item.authorName != null) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(item.avatarSrc, "头像", modifier = Modifier.clip(CircleShape).size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(item.authorName, fontSize = 14.sp, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                val badge = item.authorBadgeV2.officialBadge()
                if (badge?.isUsefulInList == true) { Spacer(Modifier.width(4.dp)); AuthorBadge(badge, compact = true) }
            }
        }
        Row {
            Column(Modifier.weight(2f)) {
                Text(parseHtmlTextWithTheme(item.summary ?: ""), fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = if (item.isFiltered) 0.dp else 3.dp))
            }
            if (!thumbnailUrl.isNullOrEmpty() && showFeedThumbnail) {
                Spacer(Modifier.width(8.dp))
                AsyncImage(thumbnailUrl, "缩略图", modifier = Modifier.weight(1f).sizeIn(maxWidth = 60.dp).clip(RoundedCornerShape(8.dp)))
            }
        }
    }
}

@Composable
private fun MiuixFeedCardMenuBox(
    item: BaseFeedViewModel.FeedDisplayItem,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    onBlockUser: ((BaseFeedViewModel.FeedDisplayItem) -> Unit)?,
    onBlockByKeywords: ((BaseFeedViewModel.FeedDisplayItem) -> Unit)?,
    onBlockTopic: ((topicId: String, topicName: String) -> Unit)?,
    navigator: Navigator,
) {
    val context = LocalContext.current
    val entries = buildList {
        if (onBlockByKeywords != null && !BuildConfig.IS_LITE) {
            add(DropdownItem(title = "按关键词屏蔽"))
        }
        add(DropdownItem(title = "屏蔽用户"))
        if (onBlockTopic != null && item.raw != null) {
            val topics = when (val raw = item.raw) {
                is com.github.zly2006.zhihu.data.DataHolder.Answer -> raw.question.topics
                is com.github.zly2006.zhihu.data.DataHolder.Question -> raw.topics
                is com.github.zly2006.zhihu.data.DataHolder.Article -> raw.topics ?: emptyList()
                else -> emptyList()
            }
            topics.forEach { topic -> add(DropdownItem(title = "屏蔽「${topic.name}」")) }
        }
        add(DropdownItem(title = "外观设置"))
        if (item.isFiltered) add(DropdownItem(title = "不再屏蔽低赞内容"))
    }

    Box {
        IconButton(onClick = { onShowMenuChange(true) }, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.MoreVert, "更多选项", tint = AppTokens.colors.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
        // More 菜单暂未实现，端口时参考 InstallerX WindowListPopup / WindowDropdownMenu
    }
}
