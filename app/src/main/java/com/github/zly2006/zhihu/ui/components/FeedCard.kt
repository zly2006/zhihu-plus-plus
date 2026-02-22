@file:Suppress("FunctionName")

package com.github.zly2006.zhihu.ui.components

import android.content.Context.MODE_PRIVATE
import androidx.compose.animation.ExperimentalSharedTransitionApi
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.Account
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.util.parseHtmlTextWithTheme
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun FeedCard(
    item: BaseFeedViewModel.FeedDisplayItem,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 240.dp,
    thumbnailUrl: String? = null,
    horizontalPadding: Dp = 16.dp,
    onLike: ((BaseFeedViewModel.FeedDisplayItem) -> Unit)? = null,
    onDislike: ((BaseFeedViewModel.FeedDisplayItem) -> Unit)? = null,
    onBlockUser: ((BaseFeedViewModel.FeedDisplayItem) -> Unit)? = null,
    onBlockByKeywords: ((BaseFeedViewModel.FeedDisplayItem) -> Unit)? = null,
    onBlockTopic: ((topicId: String, topicName: String) -> Unit)? = null,
    onClick: BaseFeedViewModel.FeedDisplayItem.() -> Unit,
) {
    val navigator = LocalNavigator.current
    val density = LocalDensity.current
    val context = LocalContext.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    var currentY by remember { mutableFloatStateOf(0f) } // 当前手指Y位置
    var startY by remember { mutableFloatStateOf(0f) } // 开始滑动时的Y位置
    var isDragging by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val preferences = remember { context.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE) }
    val enableSwipeReaction = remember {
        preferences.getBoolean("enableSwipeReaction", false)
    } &&
        onLike != null &&
        onDislike != null
    val showFeedThumbnail = remember {
        preferences.getBoolean("showFeedThumbnail", true)
    }

    // 动画偏移量
    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isDragging) offsetX else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "offsetAnimation",
    )

    // 操作区域的透明度动画
    val actionAlpha by animateFloatAsState(
        targetValue = if (abs(animatedOffsetX) > 50f) (abs(animatedOffsetX) - 50f) / 100f else 0f,
        animationSpec = tween(150),
        label = "actionAlpha",
    )

    // 根据横向滑动距离和纵向位置确定当前操作类型
    val currentAction = when {
        abs(animatedOffsetX) < 75f -> "none" // 横向滑动不够，无操作
        currentY - startY < -30f -> "like" // 手指向上，喜欢
        currentY - startY > 30f -> "dislike" // 手指向下，不喜欢
        else -> "neutral" // 中间位置，待定
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .padding(horizontal = horizontalPadding, vertical = 8.dp),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(1 - min(actionAlpha, 0.5f))
                .offset(x = with(density) { animatedOffsetX.toDp() })
                .clickable {
                    if (!isDragging && abs(animatedOffsetX) < 10f) {
                        onClick(item)
                    }
                }.let {
                    if (enableSwipeReaction) {
                        it.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    startY = offset.y // 记录开始位置
                                    currentY = offset.y
                                },
                                onDragEnd = {
                                    isDragging = false

                                    // 根据最终位置执行操作
                                    when {
                                        abs(offsetX) >= 75f && currentY - startY < -30f && onLike != null -> {
                                            onLike(item) // 横向滑动足够 + 向上 = 喜欢
                                        }

                                        abs(offsetX) >= 75f && currentY - startY > 30f && onDislike != null -> {
                                            onDislike(item) // 横向滑动足够 + 向下 = 不喜欢
                                        }
                                    }

                                    // 重置位置
                                    coroutineScope.launch {
                                        offsetX = 0f
                                        currentY = 0f
                                        startY = 0f
                                    }
                                },
                            ) { change, dragAmount ->
                                // 更新当前手指位置
                                currentY = change.position.y

                                // 只允许向左滑动，并限制最大偏移量
                                val newOffset = offsetX + dragAmount
                                offsetX = max(newOffset, -250f).coerceAtMost(0f)
                            }
                        }
                    } else {
                        it
                    }
                },
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isDragging) 8.dp else 2.dp,
            ),
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
            ) {
                if (!item.isFiltered) {
                    if (!item.title.isEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = parseHtmlTextWithTheme(item.title),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (item.avatarSrc != null && item.authorName != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                // Click on author - could navigate to author page
                            },
                        ) {
                            item.avatarSrc.let {
                                AsyncImage(
                                    model = it,
                                    contentDescription = "Avatar",
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .size(20.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                text = item.authorName,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                Row {
                    Column(
                        modifier = Modifier.weight(2f),
                    ) {
                        Text(
                            text = parseHtmlTextWithTheme(item.summary ?: ""),
                            fontSize = 14.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(
                                top = if (item.isFiltered) 0.dp else 3.dp,
                            ),
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
                                Box {
                                    IconButton(
                                        onClick = { showMenu = true },
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
                                        onDismissRequest = { showMenu = false },
                                    ) {
                                        @Suppress("SimplifyBooleanWithConstants", "KotlinConstantConditions")
                                        if (onBlockByKeywords != null && !BuildConfig.IS_LITE) {
                                            DropdownMenuItem(
                                                text = { Text("按关键词屏蔽") },
                                                onClick = {
                                                    showMenu = false
                                                    onBlockByKeywords(item)
                                                },
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("屏蔽用户") },
                                            onClick = {
                                                showMenu = false
                                                onBlockUser?.invoke(item)
                                            },
                                        )
                                        // 添加主题屏蔽选项
                                        if (onBlockTopic != null && item.raw != null) {
                                            val topics = when (val raw = item.raw) {
                                                is com.github.zly2006.zhihu.data.DataHolder.Answer -> raw.question.topics
                                                is com.github.zly2006.zhihu.data.DataHolder.Question -> raw.topics
                                                is com.github.zly2006.zhihu.data.DataHolder.Article -> raw.topics ?: emptyList()
                                                else -> emptyList()
                                            }
                                            topics.forEach { topic ->
                                                DropdownMenuItem(
                                                    text = { Text("屏蔽「${topic.name}」") },
                                                    onClick = {
                                                        showMenu = false
                                                        onBlockTopic(topic.id, topic.name)
                                                    },
                                                )
                                            }
                                        }
                                        DropdownMenuItem(
                                            text = { Text("外观设置") },
                                            onClick = {
                                                showMenu = false
                                                navigator.onNavigate(Account.AppearanceSettings())
                                            },
                                        )
                                        if (item.isFiltered) {
                                            DropdownMenuItem(
                                                text = { Text("不再屏蔽低赞内容") },
                                                onClick = {
                                                    showMenu = false
                                                    navigator.onNavigate(Account.RecommendSettings("enableQualityFilter"))
                                                },
                                            )
                                        }
                                    }
                                }
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

        if (actionAlpha > 0f && enableSwipeReaction) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    when (currentAction) {
                        "like" -> {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "喜欢",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier
                                    .size(32.dp)
                                    .scale(1f + actionAlpha * 0.3f),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "向上滑动 - 喜欢",
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.scale(1f + actionAlpha * 0.2f),
                            )
                        }
                        "dislike" -> {
                            Icon(
                                imageVector = Icons.Default.ThumbDown,
                                contentDescription = "不喜欢",
                                tint = Color(0xFFFF5722),
                                modifier = Modifier
                                    .size(32.dp)
                                    .scale(1f + actionAlpha * 0.3f),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "向下滑动 - 不喜欢",
                                color = Color(0xFFFF5722),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.scale(1f + actionAlpha * 0.2f),
                            )
                        }
                        "neutral" -> {
                            Text(
                                text = "上下滑动选择",
                                color = Color(0xFF9E9E9E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.scale(1f + actionAlpha * 0.2f),
                            )
                        }
                    }
                }
            }
        }
    }
}
