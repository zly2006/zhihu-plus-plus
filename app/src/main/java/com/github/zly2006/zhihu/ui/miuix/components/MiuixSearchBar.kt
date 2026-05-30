/*
 * Based on KernelSU's SuperSearchBar.kt (GPL-3.0-only)
 *   https://github.com/tiann/KernelSU
 * Adapted for zhihu-plus-plus under AGPL-3.0-only.
 *
 * 改动：
 *  - navigationevent 依赖换成 androidx.activity.compose.BackHandler
 *  - SearchBarFake 加 onClick + trailingContent + 按下缩放（pressScale，效果 A）
 *  - 展开过渡动画（topPadding 位移 + surfaceAlpha 渐显 + 取消按钮滑入，效果 B）原样保留
 */

package com.github.zly2006.zhihu.ui.miuix.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/* ============================================================
 * 效果 A：按下缩放 modifier（假搜索框点击反馈）
 * spring 参数抄自 KernelSU DampedDragAnimation 的 scaleX 弹簧
 * ============================================================ */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.98f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 250f),
        label = "pressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/* ============================================================
 * SearchBox：折叠态显示 content（feed），展开态隐藏
 * ============================================================ */
@Composable
fun SearchStatus.SearchBox(content: @Composable () -> Unit) {
    if (shouldCollapsed()) content()
}

/* ============================================================
 * SearchPager：展开态全屏浮层（效果 B 的展开过渡都在这）
 * ============================================================ */
@Composable
fun SearchStatus.SearchPager(
    onSearchStatusChange: (SearchStatus) -> Unit,
    defaultResult: @Composable () -> Unit,
    expandBar: @Composable (SearchStatus, (SearchStatus) -> Unit, Dp) -> Unit = { s, onChange, padding ->
        SearchBar(s, onChange, padding)
    },
    searchBarTopPadding: Dp = 12.dp,
    result: @Composable () -> Unit,
) {
    val searchStatus = this
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

    // 效果 B-1：搜索框上移到顶的位移动画，动画完成时推进状态机
    val topPadding by animateDpAsState(
        targetValue = if (searchStatus.shouldExpand()) systemBarsPadding + 5.dp
                      else searchStatus.offsetY.coerceAtLeast(0.dp),
        animationSpec = tween(300, easing = LinearOutSlowInEasing),
        label = "SearchPagerTopPadding",
        finishedListener = { onSearchStatusChange(searchStatus.onAnimationComplete()) },
    )
    // 效果 B-2：白色背景渐显
    val surfaceAlpha by animateFloatAsState(
        if (searchStatus.shouldExpand()) 1f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "SearchPagerSurfaceAlpha",
    )
    val surfaceColor = colorScheme.surface

    // 展开时按返回先收起搜索（替代 KernelSU 的 navigationevent）
    BackHandler(enabled = searchStatus.shouldExpand()) {
        onSearchStatusChange(searchStatus.copy(searchText = "", current = SearchStatus.Status.COLLAPSING))
    }

    // 完全折叠时不渲染任何东西，避免全屏 Column 盖在 feed 上形成遮罩 / 拦截触摸
    if (searchStatus.isCollapsed()) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(5f)
            .drawBehind { drawRect(surfaceColor.copy(alpha = surfaceAlpha)) }
            .semantics { onClick { false } }
            .then(if (!searchStatus.isCollapsed()) Modifier.pointerInput(Unit) {} else Modifier),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = topPadding)
                .then(if (!searchStatus.isCollapsed()) Modifier.background(colorScheme.surface) else Modifier),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!searchStatus.isCollapsed()) {
                Box(modifier = Modifier.weight(1f).background(colorScheme.surface)) {
                    expandBar(searchStatus, onSearchStatusChange, searchBarTopPadding)
                }
            }
            // 效果 B-3：取消按钮从右侧滑入
            AnimatedVisibility(
                visible = searchStatus.isExpand() || searchStatus.isAnimatingExpand(),
                enter = expandHorizontally() + slideInHorizontally(initialOffsetX = { it }),
                exit = shrinkHorizontally() + slideOutHorizontally(targetOffsetX = { it }),
            ) {
                Text(
                    text = "取消",
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 4.dp, end = 16.dp, top = searchBarTopPadding, bottom = 6.dp)
                        .clickable(
                            interactionSource = null,
                            enabled = searchStatus.isExpand(),
                            indication = null,
                        ) {
                            onSearchStatusChange(
                                searchStatus.copy(searchText = "", current = SearchStatus.Status.COLLAPSING)
                            )
                        },
                )
            }
        }
        // 效果 B-4：结果区淡入淡出
        AnimatedVisibility(
            visible = searchStatus.isExpand(),
            modifier = Modifier.fillMaxSize().zIndex(1f),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            when (searchStatus.resultStatus) {
                SearchStatus.ResultStatus.DEFAULT -> defaultResult()
                SearchStatus.ResultStatus.EMPTY -> {}
                SearchStatus.ResultStatus.LOAD -> {}
                SearchStatus.ResultStatus.SHOW -> result()
            }
        }
    }
}

/* ============================================================
 * SearchBar：展开态真输入框（带清除按钮）
 * ============================================================ */
@Composable
fun SearchBar(
    searchStatus: SearchStatus,
    onSearchStatusChange: (SearchStatus) -> Unit,
    searchBarTopPadding: Dp = 12.dp,
) {
    val focusRequester = remember { FocusRequester() }
    var textFieldValue by remember { mutableStateOf(TextFieldValue(searchStatus.searchText)) }

    LaunchedEffect(searchStatus.searchText) {
        if (textFieldValue.text != searchStatus.searchText) {
            textFieldValue = TextFieldValue(searchStatus.searchText)
        }
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = {
            textFieldValue = it
            onSearchStatusChange(searchStatus.copy(searchText = it.text))
        },
        singleLine = true,
        textStyle = TextStyle(fontWeight = FontWeight.Medium, fontSize = 17.sp, color = colorScheme.onSurface),
        cursorBrush = SolidColor(colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = searchBarTopPadding, bottom = 6.dp)
            .heightIn(min = 45.dp)
            .background(colorScheme.surfaceContainerHigh, CircleShape)
            .focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = MiuixIcons.Search,
                    contentDescription = "search",
                    modifier = Modifier.size(44.dp).padding(start = 16.dp, end = 8.dp),
                    tint = colorScheme.onSurfaceContainerHigh,
                )
                Box(modifier = Modifier.weight(1f)) { innerTextField() }
                AnimatedVisibility(
                    searchStatus.searchText.isNotEmpty(),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    Icon(
                        imageVector = MiuixIcons.Close,
                        tint = colorScheme.onSurface,
                        contentDescription = "Clean",
                        modifier = Modifier
                            .size(44.dp)
                            .padding(start = 8.dp, end = 16.dp)
                            .clickable(interactionSource = null, indication = null) {
                                textFieldValue = TextFieldValue("")
                                onSearchStatusChange(searchStatus.copy(searchText = ""))
                            },
                    )
                }
            }
        },
    )

    LaunchedEffect(Unit) {
        if (searchStatus.isAnimatingExpand()) focusRequester.requestFocus()
    }
}

/* ============================================================
 * SearchBarFake：折叠态假搜索框
 *  - onClick：点击触发展开（调用方设 EXPANDING）
 *  - trailingContent：右侧插槽（放头像入口）
 *  - 效果 A：按下时整体缩放回弹（pressScale）
 * ============================================================ */
@Composable
fun SearchBarFake(
    label: String,
    onClick: () -> Unit,
    searchBarTopPadding: Dp = 12.dp,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InputField(
            query = "",
            onQueryChange = {},
            label = label,
            leadingIcon = {
                Icon(
                    imageVector = MiuixIcons.Search,
                    contentDescription = "search",
                    modifier = Modifier.size(44.dp).padding(start = 16.dp, end = 8.dp),
                    tint = colorScheme.onSurfaceContainerHigh,
                )
            },
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = if (trailingContent != null) 4.dp else 12.dp)
                .padding(top = searchBarTopPadding, bottom = 6.dp)
                .pressScale(interactionSource)                       // 效果 A：按下缩放
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
            onSearch = {},
            enabled = false,
            expanded = false,
            onExpandedChange = {},
        )
        trailingContent?.let {
            Box(modifier = Modifier.padding(end = 12.dp, top = searchBarTopPadding, bottom = 6.dp)) {
                it()
            }
        }
    }
}
