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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val aboutGradient = Brush.radialGradient(
    colors = listOf(Color(0xFFE8D5F5), Color(0xFFD5E8F5), Color(0xFFFFFFFF)),
)

@Composable
fun MiuixAboutScreen(innerPadding: PaddingValues = PaddingValues(0.dp)) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()

    Box(
        modifier = Modifier.fillMaxSize()
            .then(if (!darkTheme) Modifier.background(aboutGradient) else Modifier.background(MiuixTheme.colorScheme.background)),
    ) {
        // 返回按钮（左上角悬浮）
        IconButton(
            onClick = { navigator.onNavigateBack() },
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding() + 8.dp, start = 8.dp).align(Alignment.TopStart),
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = MiuixTheme.colorScheme.onBackground)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 顶部留白，让内容垂直居中
            item { Spacer(Modifier.height(80.dp)) }

            // App icon
            item {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher),
                    contentDescription = "知乎++",
                    modifier = Modifier.size(96.dp).clip(RoundedCornerShape(22.dp)),
                )
            }

            item { Spacer(Modifier.height(16.dp)) }

            // App 名称
            item {
                Text(
                    "知乎++",
                    style = MiuixTheme.textStyles.title1,
                    fontWeight = FontWeight.Bold,
                )
            }

            item { Spacer(Modifier.height(8.dp)) }

            // 版本号
            item {
                Text(
                    "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MiuixTheme.textStyles.subtitle,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }

            item { Spacer(Modifier.height(32.dp)) }

            // Card 1：外部链接
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                    ArrowPreference(
                        title = "View Source",
                        summary = "GitHub",
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/zly2006/zhihu-plus-plus".toUri())) },
                    )
                    ArrowPreference(
                        title = "Join Group",
                        summary = "Telegram",
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://t.me/+_A1Yto6EpyIyODA1".toUri())) },
                    )
                }
            }

            // Card 2：许可证
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                    ArrowPreference(
                        title = "License",
                        summary = "AGPL-3.0",
                        onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, "https://www.gnu.org/licenses/agpl-3.0.html".toUri())) },
                    )
                    ArrowPreference(
                        title = "Third Party Licenses",
                        onClick = { navigator.onNavigate(Account.OpenSourceLicenses) },
                    )
                }
            }

            // Card 3：系统与更新
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                    ArrowPreference(
                        title = "系统与更新",
                        summary = "GitHub、更新设置等",
                        startAction = { Icon(Icons.Default.Settings, null) },
                        onClick = { navigator.onNavigate(Account.SystemAndUpdateSettings) },
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
