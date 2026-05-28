/*
 * Based on compose-miuix-ui sample's About screen (Apache-2.0)
 *   https://github.com/compose-miuix-ui/miuix
 * Adapted for zhihu-plus-plus under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

@Composable
fun MiuixAboutScreen(innerPadding: PaddingValues = PaddingValues(0.dp)) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val scrollBehavior = MiuixScrollBehavior()
    val lazyListState = rememberLazyListState()

    var logoHeightDp by remember { mutableStateOf(0.dp) }

    val scrollProgress by remember {
        derivedStateOf {
            when {
                lazyListState.firstVisibleItemIndex > 0 -> 1f
                else -> {
                    val spacer = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "logoSpacer" }
                    if (spacer != null && spacer.size > 0) {
                        (lazyListState.firstVisibleItemScrollOffset.toFloat() / spacer.size).coerceIn(0f, 1f)
                    } else 0f
                }
            }
        }
    }

    val barColor = if (scrollProgress == 1f) MiuixTheme.colorScheme.surface else Color.Transparent
    val iconProgress = ((scrollProgress - 0.35f) / 0.15f).coerceIn(0f, 1f)
    val nameProgress = ((scrollProgress - 0.20f) / 0.15f).coerceIn(0f, 1f)
    val versionProgress = ((scrollProgress - 0.05f) / 0.15f).coerceIn(0f, 1f)

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "关于",
                scrollBehavior = scrollBehavior,
                color = barColor,
                titleColor = MiuixTheme.colorScheme.onSurface.copy(
                    alpha = ((scrollProgress - 0.35f) / 0.65f).coerceIn(0f, 1f),
                ),
                navigationIcon = {
                    IconButton(onClick = { navigator.onNavigateBack() }) {
                        Icon(MiuixIcons.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Logo area — floats above LazyColumn
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(top = 52.dp + padding.calculateTopPadding())
                    .onSizeChanged { size -> with(density) { logoHeightDp = size.height.toDp() } },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(88.dp)
                        .graphicsLayer {
                            clip = true
                            shape = RoundedCornerShape(24.dp)
                            alpha = 1 - iconProgress
                            scaleX = 1 - (iconProgress * 0.05f)
                            scaleY = 1 - (iconProgress * 0.05f)
                        }
                        .background(Color.White),
                ) {
                    Image(
                        modifier = Modifier.size(74.dp),
                        painter = painterResource(R.mipmap.ic_launcher),
                        contentDescription = null,
                    )
                }
                Text(
                    modifier = Modifier.padding(top = 12.dp).graphicsLayer {
                        alpha = 1 - nameProgress
                        scaleX = 1 - (nameProgress * 0.05f)
                        scaleY = 1 - (nameProgress * 0.05f)
                    },
                    text = "知乎++",
                    color = MiuixTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 35.sp,
                )
                Text(
                    modifier = Modifier.fillMaxWidth().graphicsLayer {
                        alpha = 1 - versionProgress
                    },
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }

            // Scrollable content
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize().scrollEndHaptic().overScrollVertical(),
            ) {
                // Spacer matching logo height so content starts below logo
                item(key = "logoSpacer") {
                    Box(
                        Modifier.fillMaxWidth().height(logoHeightDp + 52.dp + padding.calculateTopPadding() + 80.dp),
                    )
                }

                // Card 1: External links
                item {
                    Column(modifier = Modifier.fillParentMaxHeight().padding(bottom = 12.dp)) {
                        Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                            ArrowPreference(
                                title = "View Source",
                                endActions = {
                                    Text(
                                        text = "GitHub",
                                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                    )
                                },
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/zly2006/zhihu-plus-plus".toUri())) },
                            )
                            ArrowPreference(
                                title = "Join Group",
                                endActions = {
                                    Text(
                                        text = "Telegram",
                                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                    )
                                },
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://t.me/+_A1Yto6EpyIyODA1".toUri())) },
                            )
                        }
                        Card(
                            modifier = Modifier.padding(horizontal = 12.dp).padding(top = 12.dp),
                        ) {
                            ArrowPreference(
                                title = "License",
                                endActions = {
                                    Text(
                                        text = "AGPL-3.0",
                                        fontSize = MiuixTheme.textStyles.body2.fontSize,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                    )
                                },
                                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://www.gnu.org/licenses/agpl-3.0.html".toUri())) },
                            )
                            ArrowPreference(
                                title = "Third Party Licenses",
                                onClick = { navigator.onNavigate(Account.OpenSourceLicenses) },
                            )
                        }
                        Card(
                            modifier = Modifier.padding(horizontal = 12.dp).padding(top = 12.dp),
                        ) {
                            ArrowPreference(
                                title = "系统与更新",
                                summary = "GitHub、更新设置等",
                                startAction = { Icon(Icons.Default.Settings, null) },
                                onClick = { navigator.onNavigate(Account.SystemAndUpdateSettings) },
                            )
                        }
                    }
                }
            }
        }
    }
}
