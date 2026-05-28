/*
 * Based on compose-miuix-ui sample's About screen (Apache-2.0)
 *   https://github.com/compose-miuix-ui/miuix
 * Adapted for zhihu-plus-plus under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MiuixAboutScreen(innerPadding: PaddingValues = PaddingValues(0.dp)) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()

    val gradient = Brush.linearGradient(listOf(Color(0xFFD8B4E2), Color(0xFFE8C4D8), Color(0xFFB4D4E8)))

    if (darkTheme) {
        Box(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.background).padding(innerPadding)) {
            AboutContent(navigator, context)
        }
    } else {
        Box(Modifier.fillMaxSize().background(Color.Magenta).padding(innerPadding)) {
            AboutContent(navigator, context)
        }
    }
}

@Composable
private fun AboutContent(navigator: com.github.zly2006.zhihu.navigation.Navigator, context: android.content.Context) {
    // 返回箭头
    IconButton(
        onClick = { navigator.onNavigateBack() },
        modifier = Modifier.padding(top = 8.dp, start = 8.dp),
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = MiuixTheme.colorScheme.onBackground)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 顶部留白
        item(key = "topSpacer") { Spacer(Modifier.height(80.dp)) }

            // App icon
            item(key = "icon") {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(88.dp).clip(RoundedCornerShape(24.dp)).background(Color.White),
                ) {
                    val appIcon = remember {
                        context.packageManager.getApplicationIcon(context.packageName)
                            .toBitmap(width = 200, height = 200).asImageBitmap()
                    }
                    Image(bitmap = appIcon, contentDescription = "知乎++", modifier = Modifier.size(74.dp))
                }
            }

            item(key = "nameGap") { Spacer(Modifier.height(16.dp)) }

            // App 名称
            item(key = "name") {
                Text("知乎++", color = MiuixTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold, fontSize = 36.sp)
            }

            item(key = "verGap") { Spacer(Modifier.height(8.dp)) }

            // 版本号
            item(key = "version") {
                Text("v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 20.sp, textAlign = TextAlign.Center)
            }

            // 到卡片的留白
            item(key = "midSpacer") { Spacer(Modifier.height(64.dp)) }

            // Card 1：外部链接
            item(key = "card1") {
                Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.defaultColors(color = Color.Transparent),
            ) {
                    ArrowPreference(
                        title = "View Source",
                        endActions = { Text("GitHub", fontSize = MiuixTheme.textStyles.body2.fontSize, color = MiuixTheme.colorScheme.onSurfaceVariantActions) },
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/zly2006/zhihu-plus-plus".toUri())) },
                    )
                    ArrowPreference(
                        title = "Join Group",
                        endActions = { Text("Telegram", fontSize = MiuixTheme.textStyles.body2.fontSize, color = MiuixTheme.colorScheme.onSurfaceVariantActions) },
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://t.me/+_A1Yto6EpyIyODA1".toUri())) },
                    )
                }
            }

            item(key = "gap1") { Spacer(Modifier.height(12.dp)) }

            // Card 2：许可证
            item(key = "card2") {
                Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.defaultColors(color = Color.Transparent),
            ) {
                    ArrowPreference(
                        title = "License",
                        endActions = { Text("AGPL-3.0", fontSize = MiuixTheme.textStyles.body2.fontSize, color = MiuixTheme.colorScheme.onSurfaceVariantActions) },
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://www.gnu.org/licenses/agpl-3.0.html".toUri())) },
                    )
                    ArrowPreference(
                        title = "Third Party Licenses",
                        onClick = { navigator.onNavigate(Account.OpenSourceLicenses) },
                    )
                }
            }

            item(key = "gap2") { Spacer(Modifier.height(12.dp)) }

            // Card 3：系统与更新
            item(key = "card3") {
                Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.defaultColors(color = Color.Transparent),
            ) {
                    ArrowPreference(
                        title = "系统与更新",
                        summary = "GitHub、更新设置等",
                    onClick = { navigator.onNavigate(Account.SystemAndUpdateSettings) },
                    )
                }
            }

            item(key = "bottomSpacer") { Spacer(Modifier.height(32.dp)) }
        }
}
