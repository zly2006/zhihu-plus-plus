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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val aboutGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFE8D5F5), Color(0xFFF5D5E8), Color(0xFFD5E5F5)),
)

@Composable
fun MiuixAboutScreen(innerPadding: PaddingValues = PaddingValues(0.dp)) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()

    Box(
        modifier = Modifier.fillMaxSize()
            .then(if (darkTheme) Modifier.background(MiuixTheme.colorScheme.background) else Modifier.background(aboutGradient))
            .padding(innerPadding),
    ) {
        // 左上角返回箭头（无标题栏）
        IconButton(
            onClick = { navigator.onNavigateBack() },
            modifier = Modifier.padding(top = 8.dp, start = 8.dp).align(Alignment.TopStart),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = MiuixTheme.colorScheme.onBackground)
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 顶部到 icon 之间约占 30% 高度
            Spacer(Modifier.fillMaxWidth().weight(0.30f))

            // App icon（110.dp，白色圆角方底）
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(110.dp).clip(RoundedCornerShape(24.dp)).background(Color.White),
            ) {
                val appIcon = remember {
                    context.packageManager.getApplicationIcon(context.packageName)
                        .toBitmap(width = 200, height = 200).asImageBitmap()
                }
                Image(bitmap = appIcon, contentDescription = "知乎++", modifier = Modifier.size(82.dp))
            }

            Spacer(Modifier.height(16.dp))

            // App 名称
            Text(
                "知乎++",
                color = MiuixTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
            )

            Spacer(Modifier.height(8.dp))

            // 版本号
            Text(
                "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
            )

            // 版本号到 Card 之间约占 25% 高度
            Spacer(Modifier.fillMaxWidth().weight(0.25f))

            // Card 1：外部链接
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                ArrowPreference(
                    title = "View Source",
                    endActions = {
                        Text("GitHub", fontSize = MiuixTheme.textStyles.body2.fontSize,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions)
                    },
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/zly2006/zhihu-plus-plus".toUri())) },
                )
                ArrowPreference(
                    title = "Join Group",
                    endActions = {
                        Text("Telegram", fontSize = MiuixTheme.textStyles.body2.fontSize,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions)
                    },
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://t.me/+_A1Yto6EpyIyODA1".toUri())) },
                )
            }

            Spacer(Modifier.height(12.dp))

            // Card 2：许可证
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                ArrowPreference(
                    title = "License",
                    endActions = {
                        Text("AGPL-3.0", fontSize = MiuixTheme.textStyles.body2.fontSize,
                            color = MiuixTheme.colorScheme.onSurfaceVariantActions)
                    },
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://www.gnu.org/licenses/agpl-3.0.html".toUri())) },
                )
                ArrowPreference(
                    title = "Third Party Licenses",
                    onClick = { navigator.onNavigate(Account.OpenSourceLicenses) },
                )
            }

            Spacer(Modifier.height(12.dp))

            // Card 3：系统与更新
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                ArrowPreference(
                    title = "系统与更新",
                    summary = "GitHub、更新设置等",
                    startAction = { Icon(Icons.Default.Settings, null) },
                    onClick = { navigator.onNavigate(Account.SystemAndUpdateSettings) },
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
