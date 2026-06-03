/*
 * Based on compose-miuix-ui example AboutPage.kt (Apache-2.0)
 *   https://github.com/miuix-kotlin-multiplatform/miuix
 * Adapted for zhihu-plus-plus under AGPL-3.0-only.
 *
 * 照抄官方三个效果（用普通 Compose 实现，不依赖官方那套 RuntimeShader）：
 *   1. 卡片上移限制：LazyColumn contentPadding=top(topbar高) + 卡片 fillParentMaxHeight，
 *      第一个卡片最高只能滚到 topbar 底部，不会超过。
 *   2. topbar 透明→白模糊：barColor 在 scrollProgress==1f 时变 surface（官方是突变）；
 *      标题 alpha 随 scrollProgress 渐显。
 *   3. 背景跟着过渡：渐变背景 alpha = 1 - scrollProgress，滚到顶时背景透明，露出纯白 surface。
 *   分阶段错峰淡出：版本号(0.05)→名称(0.20)→图标(0.35)，照抄官方阈值。
 */

package com.github.zly2006.zhihu.ui.miuix.subscreens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Navigator
import com.github.zly2006.zhihu.shared.platform.rememberExternalUrlOpener
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import com.github.zly2006.zhihu.ui.rememberAccountSettingsPlatformRuntime
import org.jetbrains.compose.resources.painterResource
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import zhihu.shared.generated.resources.Res
import zhihu.shared.generated.resources.ic_launcher_foreground

private val LightFlowColors = listOf(Color(0xFFE8D5F5), Color(0xFFF5D8E8), Color(0xFFD5E5F5))
private val DarkFlowColors = listOf(Color(0xFF3D2A52), Color(0xFF2A3A5C), Color(0xFF1E3A3A))

@Composable
fun MiuixAboutScreen(innerPadding: PaddingValues = PaddingValues(0.dp)) {
    val navigator = LocalNavigator.current
    val openUrl = rememberExternalUrlOpener()
    val runtime = rememberAccountSettingsPlatformRuntime()
    val versionInfo = remember(runtime) { runtime.appVersionInfo() }
    val density = LocalDensity.current
    val darkTheme = ThemeManager.isDarkTheme()
    val lazyListState = rememberLazyListState()
    val scrollBehavior = MiuixScrollBehavior()
    val backdrop = rememberMiuixBlurBackdrop(true)

    val scrollProgress by remember {
        derivedStateOf {
            when {
                lazyListState.firstVisibleItemIndex > 0 -> 1f
                else -> {
                    val spacer = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "logoSpacer" }
                    if (spacer != null && spacer.size > 0) {
                        (lazyListState.firstVisibleItemScrollOffset.toFloat() / spacer.size).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                }
            }
        }
    }

    val versionCodeProgress = ((scrollProgress - 0.05f) / 0.15f).coerceIn(0f, 1f)
    val projectNameProgress = ((scrollProgress - 0.20f) / 0.15f).coerceIn(0f, 1f)
    val iconProgress = ((scrollProgress - 0.35f) / 0.15f).coerceIn(0f, 1f)

    // 效果2：topbar 滚到顶才变白（官方突变逻辑）。
    // 模糊开启时，到顶用透明（露出模糊），否则到顶用 surface 白。
    val blurActive = backdrop != null && scrollProgress == 1f
    val barColor = when {
        blurActive -> Color.Transparent
        scrollProgress == 1f -> MiuixTheme.colorScheme.surface
        else -> Color.Transparent
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(backdrop, enabled = blurActive),
                title = "关于",
                scrollBehavior = scrollBehavior,
                color = barColor,
                titleColor = MiuixTheme.colorScheme.onSurface.copy(
                    alpha = ((scrollProgress - 0.35f) / 0.65f).coerceIn(0f, 1f),
                ),
                navigationIcon = {
                    IconButton(onClick = { navigator.onNavigateBack() }) {
                        Icon(
                            imageVector = MiuixIconsEmbedded.Back,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                },
            )
        },
    ) { padding ->
        AboutContent(
            padding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding(),
            ),
            lazyListState = lazyListState,
            scrollProgress = scrollProgress,
            iconProgress = iconProgress,
            projectNameProgress = projectNameProgress,
            versionCodeProgress = versionCodeProgress,
            versionInfo = versionInfo,
            darkTheme = darkTheme,
            navigator = navigator,
            openUrl = openUrl,
            density = density,
            backdrop = backdrop,
        )
    }
}

@Composable
private fun AboutContent(
    padding: PaddingValues,
    lazyListState: LazyListState,
    scrollProgress: Float,
    iconProgress: Float,
    projectNameProgress: Float,
    versionCodeProgress: Float,
    versionInfo: String,
    darkTheme: Boolean,
    navigator: Navigator,
    openUrl: (String) -> Unit,
    density: Density,
    backdrop: top.yukonga.miuix.kmp.blur.LayerBackdrop?,
) {
    var logoHeightDp by remember { mutableStateOf(300.dp) }
    val logoBoxColor = if (darkTheme) MiuixTheme.colorScheme.surface else Color.White

    // 效果3：背景 = 底层纯 surface（白），上面盖一层渐变流光，
    // 渐变 alpha = 1 - scrollProgress，滚到顶时渐变透明 → 露出纯白
    val infinite = rememberInfiniteTransition(label = "flow")
    val flow by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "flowOffset",
    )
    val flowColors = if (darkTheme) DarkFlowColors else LightFlowColors
    val flowBrush = Brush.linearGradient(
        colors = flowColors,
        start = Offset(flow * 600f, 0f),
        end = Offset(flow * 600f + 900f, 1400f),
    )

    // layerBackdrop 标在整个内容 Box 上，作为 topbar 模糊的采样源
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier),
    ) {
        // 底层：纯 surface（滚到顶后看到的白）
        Box(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.surface))
        // 上层：流光渐变，随滚动淡出
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 1f - scrollProgress }
                .background(flowBrush),
        )

        // 浮动 logo（下层），onSizeChanged 测高度
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = padding.calculateTopPadding() + 52.dp)
                .onSizeChanged { size -> with(density) { logoHeightDp = size.height.toDp() } },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(88.dp)
                    .graphicsLayer {
                        alpha = 1f - iconProgress
                        scaleX = 1f - iconProgress * 0.05f
                        scaleY = 1f - iconProgress * 0.05f
                    }.background(logoBoxColor, RoundedCornerShape(24.dp)),
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_launcher_foreground),
                    contentDescription = "知乎++",
                    modifier = Modifier.size(74.dp),
                )
            }
            // 知乎++ 流光：照抄 miuix 官方 AboutPage 的 HyperOS shimmer 扫光（drawWithCache + drawText + 横向渐变平移）
            val nameShimmer by infinite.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
                label = "nameShimmer",
            )
            val nameStops = remember {
                arrayOf(
                    0.0f to Color(0xFF4286F4),
                    0.25f to Color(0xFF8E54E9),
                    0.5f to Color(0xFFFF6B6B),
                    0.75f to Color(0xFF8E54E9),
                    1.0f to Color(0xFF4286F4),
                )
            }
            val nameMeasurer = rememberTextMeasurer()
            Text(
                text = "知乎++",
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 5.dp)
                    .graphicsLayer {
                        alpha = 1f - projectNameProgress
                        scaleX = 1f - projectNameProgress * 0.05f
                        scaleY = 1f - projectNameProgress * 0.05f
                    }.drawWithCache {
                        val layout = nameMeasurer.measure("知乎++", TextStyle(fontSize = 35.sp, fontWeight = FontWeight.Bold))
                        val tw = layout.size.width.toFloat()
                        val th = layout.size.height.toFloat()
                        onDrawWithContent {
                            val gw = tw * 2f
                            val startX = -gw + nameShimmer * (gw + tw + gw)
                            drawText(
                                textLayoutResult = layout,
                                brush = Brush.horizontalGradient(colorStops = nameStops, startX = startX, endX = startX + gw),
                                topLeft = Offset((size.width - tw) / 2f, (size.height - th) / 2f),
                            )
                        }
                    },
                color = Color.Transparent,
                fontWeight = FontWeight.Bold,
                fontSize = 35.sp,
            )
            Text(
                text = versionInfo,
                modifier = Modifier.fillMaxWidth().graphicsLayer {
                    alpha = 1f - versionCodeProgress
                    scaleX = 1f - versionCodeProgress * 0.05f
                    scaleY = 1f - versionCodeProgress * 0.05f
                },
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
        }

        // 上层 LazyColumn 接触摸。
        // 效果1：contentPadding.top = topbar 高度 → 第一个卡片最高只能滚到 topbar 底部，不会超过
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding()),
        ) {
            item(key = "logoSpacer") {
                Box(Modifier.fillMaxWidth().height(logoHeightDp + 52.dp + 126.dp))
            }
            item(key = "about") {
                Column(modifier = Modifier.fillParentMaxHeight().padding(bottom = padding.calculateBottomPadding())) {
                    Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                        ArrowPreference(
                            title = "View Source",
                            summary = "GitHub",
                            onClick = { openUrl("https://github.com/zly2006/zhihu-plus-plus") },
                        )
                        ArrowPreference(
                            title = "Join Group",
                            summary = "Telegram",
                            onClick = { openUrl("https://t.me/+_A1Yto6EpyIyODA1") },
                        )
                    }
                    Card(modifier = Modifier.padding(horizontal = 12.dp).padding(top = 12.dp)) {
                        ArrowPreference(
                            title = "License",
                            summary = "AGPL-3.0",
                            onClick = { openUrl("https://www.gnu.org/licenses/agpl-3.0.html") },
                        )
                        ArrowPreference(
                            title = "Third Party Licenses",
                            onClick = { navigator.onNavigate(Account.OpenSourceLicenses) },
                        )
                    }
                    Card(modifier = Modifier.padding(horizontal = 12.dp).padding(top = 12.dp)) {
                        ArrowPreference(
                            title = "系统与更新",
                            summary = "GitHub、更新设置等",
                            startAction = { Icon(Icons.Filled.Settings, null) },
                            onClick = { navigator.onNavigate(Account.SystemAndUpdateSettings) },
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}
