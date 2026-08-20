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

package com.github.zly2006.zhihu.ui.components

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.FeedDisplayItem
import com.github.zly2006.zhihu.data.navDestination
import com.github.zly2006.zhihu.data.officialBadge
import com.github.zly2006.zhihu.data.sourceLabel
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Navigator
import com.github.zly2006.zhihu.navigation.withReadingQueueSource
import com.github.zly2006.zhihu.platform.UserMessageDuration
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.subscreens.PREF_FONT_SIZE
import com.github.zly2006.zhihu.ui.subscreens.PREF_LINE_HEIGHT
import com.github.zly2006.zhihu.util.parseEmphasizedHtmlTextWithTheme
import com.github.zly2006.zhihu.viewmodel.QUALITY_FILTER_MODE_PREFERENCE_KEY

/**
 * 信息流卡片的 Material 3 实现。
 *
 * 卡片负责展示标题、摘要、作者、徽章、缩略图和更多菜单，并根据设置支持卡片/分割线两种外观、Duo3 排版和缩略图开关。
 * 默认点击会解析 [FeedDisplayItem] 的导航目标并进入详情页；页面可以通过 [menuItems] 直接声明自己的业务菜单项。
 *
 * 修改这个组件时要同步复核 `showFeedThumbnail`、`feedCardStyle`、`duo3_card_appearance`、
 * `duo3_card_layout` 和 `duo3_card_large_title` 对各信息流入口的影响。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FeedCard(
    item: FeedDisplayItem,
    modifier: Modifier = Modifier,
    readingQueueSourceId: String? = null,
    maxHeight: Dp = 240.dp,
    thumbnailUrl: String? = null,
    horizontalPadding: Dp = 16.dp,
    menuItems: @Composable ColumnScope.(dismissMenu: () -> Unit) -> Unit = { _ -> },
    showSourceLabel: Boolean = false,
    /**
     * 默认点击行为：优先跳转到信息流条目的详情页；如果只能识别为外链则打开外链，否则提示暂不支持。
     */
    onClick: ((item: FeedDisplayItem, destination: NavDestination?) -> Unit)? = null,
) {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    val userMessages = rememberUserMessageSink()
    val settings = rememberSettingsStore()
    var showMenu by remember { mutableStateOf(false) }
    val showFeedThumbnail = remember {
        settings.getBoolean("showFeedThumbnail", true)
    }
    val feedCardStyle = remember {
        settings.getString("feedCardStyle", "divider")
    }
    val duo3CardAppearance = remember { settings.getBoolean("duo3_card_appearance", false) }
    val duo3CardLayout = remember { settings.getBoolean("duo3_card_layout", false) }
    val duo3CardLargeTitle = remember { settings.getBoolean("duo3_card_large_title", true) }
    val pinImages = (item.feed?.target as? Feed.PinTarget)
        ?.content
        ?.filterIsInstance<DataHolder.Pin.ContentImage>()
        .orEmpty()
    val showPinImages = showFeedThumbnail && pinImages.isNotEmpty() && !item.isFiltered
    val performClick: (FeedDisplayItem) -> Unit = { clickedItem ->
        val destination = clickedItem.navDestination?.withReadingQueueSource(readingQueueSourceId)
        if (onClick != null) {
            onClick(clickedItem, destination)
        } else {
            destination?.let(navigator.onNavigate) ?: run {
                if (clickedItem.content?.startsWith("http") == true) {
                    uriHandler.openUri(clickedItem.content)
                } else {
                    userMessages.showMessage("暂不支持打开该内容", UserMessageDuration.Short)
                }
            }
        }
    }
    if (feedCardStyle == "divider") {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .then(if (showPinImages) Modifier else Modifier.heightIn(max = maxHeight)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { performClick(item) }
                    .padding(horizontal = horizontalPadding, vertical = 12.dp),
            ) {
                FeedCardContent(
                    item = item,
                    showFeedThumbnail = showFeedThumbnail,
                    thumbnailUrl = thumbnailUrl,
                    pinImages = pinImages,
                    showMenu = showMenu,
                    onShowMenuChange = { showMenu = it },
                    menuItems = menuItems,
                    duo3CardLayout = duo3CardLayout,
                    duo3CardLargeTitle = duo3CardLargeTitle,
                    showSourceLabel = showSourceLabel,
                )
            }
            HorizontalDivider(thickness = 0.3.dp)
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .then(if (showPinImages) Modifier else Modifier.heightIn(max = maxHeight))
                .padding(horizontal = horizontalPadding, vertical = 8.dp),
        ) {
            Card(
                colors = if (duo3CardAppearance) {
                    CardDefaults.cardColors().copy(
                        containerColor = MaterialTheme.colorScheme.surfaceBright,
                    )
                } else {
                    CardDefaults.cardColors()
                },
                shape = if (duo3CardAppearance) RoundedCornerShape(24.dp) else CardDefaults.shape,
                modifier = Modifier
                    .fillMaxWidth()
                    .let { if (duo3CardAppearance) it.clip(RoundedCornerShape(24.dp)) else it }
                    .clickable { performClick(item) },
                elevation = if (duo3CardAppearance) {
                    CardDefaults.cardElevation()
                } else {
                    CardDefaults.cardElevation(defaultElevation = 2.dp)
                },
            ) {
                Column(
                    modifier = if (duo3CardAppearance) {
                        Modifier.padding(16.dp, 12.dp, 16.dp, 16.dp)
                    } else {
                        Modifier.padding(8.dp)
                    },
                ) {
                    FeedCardContent(
                        item = item,
                        showFeedThumbnail = showFeedThumbnail,
                        thumbnailUrl = thumbnailUrl,
                        pinImages = pinImages,
                        showMenu = showMenu,
                        onShowMenuChange = { showMenu = it },
                        menuItems = menuItems,
                        duo3CardLayout = duo3CardLayout,
                        duo3CardLargeTitle = duo3CardLargeTitle,
                        showSourceLabel = showSourceLabel,
                    )
                }
            }
        }
    }
}

/**
 * 信息流卡片右上角的更多菜单。
 *
 * 卡片只负责菜单的展开、收起和通用设置项；页面业务动作由 [menuItems] 直接提供。
 */
@Composable
private fun FeedCardMenuBox(
    item: FeedDisplayItem,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    menuItems: @Composable ColumnScope.(dismissMenu: () -> Unit) -> Unit,
    navigator: Navigator,
) {
    Box {
        IconButton(
            onClick = { onShowMenuChange(true) },
            modifier = Modifier.size(24.dp),
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "更多选项",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { onShowMenuChange(false) },
        ) {
            menuItems { onShowMenuChange(false) }
            DropdownMenuItem(
                text = { Text("外观设置") },
                onClick = {
                    onShowMenuChange(false)
                    navigator.onNavigate(Account.AppearanceSettings())
                },
            )
            if (item.isQualityFiltered) {
                DropdownMenuItem(
                    text = { Text("调整质量屏蔽") },
                    onClick = {
                        onShowMenuChange(false)
                        navigator.onNavigate(Account.RecommendSettings(QUALITY_FILTER_MODE_PREFERENCE_KEY))
                    },
                )
            }
        }
    }
}

/**
 * 信息流卡片正文内容。
 *
 * 这里决定标题、摘要、缩略图、作者信息和操作菜单在卡片内的排列方式。标准排版强调兼容既有 Material 3 卡片，
 * Duo3 排版则把作者移到底部、调整图片和摘要结构，并可使用更大的标题字号。
 */
@Composable
private fun FeedCardContent(
    item: FeedDisplayItem,
    showFeedThumbnail: Boolean,
    thumbnailUrl: String?,
    pinImages: List<DataHolder.Pin.ContentImage>,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    menuItems: @Composable ColumnScope.(dismissMenu: () -> Unit) -> Unit,
    duo3CardLayout: Boolean,
    duo3CardLargeTitle: Boolean,
    showSourceLabel: Boolean,
) {
    val settings = rememberSettingsStore()
    val fontSizePercent = remember { settings.getInt(PREF_FONT_SIZE, 100) }
    val lineHeightPercent = remember { settings.getInt(PREF_LINE_HEIGHT, 160) }
    val navigator = LocalNavigator.current
    val visiblePinImages = pinImages.takeIf { showFeedThumbnail && !item.isFiltered }.orEmpty()
    val sourceLabel = item.feed?.sourceLabel.takeUnless { item.isFiltered }
    if (duo3CardLayout) {
        // ── 新排版（duo3）────────────────────────────────────────────────────
        if (showSourceLabel) {
            FeedCardSourceLabel(sourceLabel)
        }
        if (!item.title.isEmpty()) {
            val titleStyle = if (duo3CardLargeTitle) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.titleMedium
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = parseEmphasizedHtmlTextWithTheme(item.title),
                    style = titleStyle.copy(
                        fontSize = titleStyle.fontSize * fontSizePercent / 100,
                        lineHeight = titleStyle.lineHeight * fontSizePercent / 100,
                    ),
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurface,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }

        Column {
            Row {
                Text(
                    text = parseEmphasizedHtmlTextWithTheme(item.summary ?: ""),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp * fontSizePercent / 100,
                        lineHeight = 14.sp * fontSizePercent / 100 * lineHeightPercent / 100,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (!thumbnailUrl.isNullOrEmpty() && showFeedThumbnail && !item.isFiltered) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = "Thumbnail",
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .sizeIn(maxHeight = 80.dp, maxWidth = 128.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.FillHeight,
                    )
                }
            }
            PinFeedImages(
                images = visiblePinImages,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (item.details.isNotEmpty() || (item.avatarSrc != null && item.authorName != null)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val avatarSrc = item.avatarSrc
                        val authorName = item.authorName
                        if (avatarSrc != null && authorName != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .clickable {},
                            ) {
                                AsyncImage(
                                    model = avatarSrc,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .size(24.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = authorName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                val authorBadge = item.authorBadgeV2.officialBadge()
                                if (authorBadge?.isUsefulInList == true) {
                                    Spacer(Modifier.width(4.dp))
                                    AuthorBadge(authorBadge, compact = true)
                                }
                            }
                            Spacer(Modifier.width(6.dp))
                        }
                        if (item.details.isNotEmpty()) {
                            Text(
                                text = item.details,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    if (item.details.isNotEmpty()) {
                        FeedCardMenuBox(item, showMenu, onShowMenuChange, menuItems, navigator)
                    }
                }
            }
        }
    } else {
        // ── 原始排版（master）────────────────────────────────────────────────
        if (showSourceLabel) {
            FeedCardSourceLabel(sourceLabel)
        }
        if (!item.title.isEmpty() && !item.isFiltered) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = parseEmphasizedHtmlTextWithTheme(item.title),
                    fontSize = 16.sp * fontSizePercent / 100,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        val avatarSrc = item.avatarSrc
        val authorName = item.authorName
        if (avatarSrc != null && authorName != null) {
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {},
            ) {
                AsyncImage(
                    model = avatarSrc,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = authorName,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                val authorBadge = item.authorBadgeV2.officialBadge()
                if (authorBadge?.isUsefulInList == true) {
                    Spacer(Modifier.width(4.dp))
                    AuthorBadge(authorBadge, compact = true)
                }
            }
        }
        Row {
            Column(modifier = Modifier.weight(2f)) {
                Text(
                    text = parseEmphasizedHtmlTextWithTheme(item.summary ?: ""),
                    fontSize = 14.sp * fontSizePercent / 100,
                    lineHeight = 14.sp * fontSizePercent / 100 * lineHeightPercent / 100,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = if (item.isFiltered) 0.dp else 3.dp),
                )
                PinFeedImages(
                    images = visiblePinImages,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (item.details.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = item.details,
                            fontSize = 12.sp,
                            lineHeight = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        FeedCardMenuBox(item, showMenu, onShowMenuChange, menuItems, navigator)
                    }
                }
            }
            if (!thumbnailUrl.isNullOrEmpty() && showFeedThumbnail) {
                Spacer(modifier = Modifier.width(8.dp))
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = "Thumbnail",
                    modifier = Modifier
                        .weight(1f)
                        .sizeIn(maxWidth = 60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            }
        }
    }
}

@Composable
private fun PinFeedImages(
    images: List<DataHolder.Pin.ContentImage>,
    modifier: Modifier = Modifier,
) {
    when (images.size) {
        0 -> return
        1 -> {
            val image = images.single()
            AsyncImage(
                model = image.feedThumbnailUrl,
                contentDescription = "想法图片 1/1",
                modifier = modifier
                    .fillMaxWidth(1f / 3f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .testTag("pin_feed_image_0"),
                contentScale = ContentScale.Crop,
            )
        }
        in 2..4 -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("pin_feed_images"),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                images.forEachIndexed { index, image ->
                    PinFeedImage(
                        image = image,
                        index = index,
                        totalCount = images.size,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                    )
                }
                repeat((3 - images.size).coerceAtLeast(0)) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                    )
                }
            }
        }
        else -> {
            val visibleImages = images.take(9)
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .testTag("pin_feed_images"),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                visibleImages.chunked(3).forEachIndexed { rowIndex, rowImages ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        rowImages.forEachIndexed { columnIndex, image ->
                            val index = rowIndex * 3 + columnIndex
                            PinFeedImage(
                                image = image,
                                index = index,
                                totalCount = images.size,
                                remainingCount = (images.size - 9).takeIf { index == 8 && it > 0 },
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                            )
                        }
                        repeat(3 - rowImages.size) {
                            Spacer(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinFeedImage(
    image: DataHolder.Pin.ContentImage,
    index: Int,
    totalCount: Int,
    remainingCount: Int? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .testTag("pin_feed_image_$index"),
    ) {
        AsyncImage(
            model = image.feedThumbnailUrl,
            contentDescription = "想法图片 ${index + 1}/$totalCount",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        if (remainingCount != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+$remainingCount",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

internal val DataHolder.Pin.ContentImage.feedThumbnailUrl: String
    get() = thumbnail.ifBlank { url }

@Composable
private fun FeedCardSourceLabel(sourceLabel: String?) {
    val label = sourceLabel?.takeIf { it.isNotBlank() } ?: return
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}
