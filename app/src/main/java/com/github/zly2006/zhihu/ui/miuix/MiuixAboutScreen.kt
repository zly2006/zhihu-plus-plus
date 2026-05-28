/*
 * Based on compose-miuix-ui sample's About screen (Apache-2.0)
 *   https://github.com/compose-miuix-ui/miuix
 * Adapted for zhihu-plus-plus under AGPL-3.0-only.
 */
 
package com.github.zly2006.zhihu.ui.miuix
 
import android.content.Intent
import androidx.compose.foundation.background
import coil3.compose.AsyncImage
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
 
/**
 * miuix 风格"关于"页 —— 简化稳定版。
 *
 * 设计取舍：放弃了"logo 浮动 + 滚动淡出 + 标题渐显"的联动特效，
 * 因为那套机制会形成「logo 尺寸 → spacer 高度 → layoutInfo →
 * scrollProgress → logo 尺寸」的循环依赖，导致无限重组、滑不动、闪退。
 *
 * 这一版：logo / 名称 / 版本号 / Card 全部作为 LazyColumn 的普通 item，
 * 跟着内容一起滚动；TopAppBar 折叠交给 MiuixScrollBehavior 自带逻辑。
 * 视觉上保留了居中、留白、Card 分组，只是去掉了花哨的淡出动画。
 */
@Composable
fun MiuixAboutScreen(innerPadding: PaddingValues = PaddingValues(0.dp)) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val scrollBehavior = MiuixScrollBehavior()
    val lazyListState = rememberLazyListState()
 
    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = "关于",
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { navigator.onNavigateBack() }) {
                        // 用 Material 图标，跟项目其它页面一致；
                        // 如果你确认要用 miuix 图标，换成 MiuixIcons.Back
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onBackground,
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .overScrollVertical(),
            contentPadding = padding,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 顶部留白
            item { Spacer(Modifier.height(40.dp)) }
 
            // App icon
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White),
                ) {
                    AsyncImage(
                        model = R.mipmap.ic_launcher,
                        contentDescription = "知乎++",
                        modifier = Modifier.size(74.dp),
                    )
                }
            }
 
            // App 名称
            item {
                Text(
                    text = "知乎++",
                    modifier = Modifier.padding(top = 12.dp),
                    color = MiuixTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 35.sp,
                )
            }
 
            // 版本号
            item {
                Text(
                    text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
 
            // 名称区到 Card 区的留白
            item { Spacer(Modifier.height(48.dp)) }
 
            // Card 1：外部链接
            item {
                Card(modifier = Modifier.padding(horizontal = 12.dp)) {
                    ArrowPreference(
                        title = "View Source",
                        summary = "GitHub",   // ← 如果你的版本支持 summary 显示在右侧就用这个；
                                              //    若需要右侧值用 endActions，见文件末尾说明
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, "https://github.com/zly2006/zhihu-plus-plus".toUri())
                            )
                        },
                    )
                    ArrowPreference(
                        title = "Join Group",
                        summary = "Telegram",
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, "https://t.me/+_A1Yto6EpyIyODA1".toUri())
                            )
                        },
                    )
                }
            }
 
            // Card 2：许可证
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(top = 12.dp),
                ) {
                    ArrowPreference(
                        title = "License",
                        summary = "AGPL-3.0",
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, "https://www.gnu.org/licenses/agpl-3.0.html".toUri())
                            )
                        },
                    )
                    ArrowPreference(
                        title = "Third Party Licenses",
                        onClick = { navigator.onNavigate(Account.OpenSourceLicenses) },
                    )
                }
            }
 
            // Card 3：系统与更新
            item {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .padding(top = 12.dp),
                ) {
                    ArrowPreference(
                        title = "系统与更新",
                        summary = "GitHub、更新设置等",
                        // ← 图标参数名：你之前文件里 startAction / leftAction 都出现过，
                        //    用你项目里实际能编译的那个。这里先用 leftAction，
                        //    若报 unresolved reference 就改成 startAction
                        startAction = { Icon(Icons.Default.Settings, null) },
                        onClick = { navigator.onNavigate(Account.SystemAndUpdateSettings) },
                    )
                }
            }
 
            // 底部留白
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
 
/*
 * ============ 需要你核对/可能要改的 3 个点 ============
 *
 * 1. ArrowPreference 的图标参数名（第 Card 3 处）：
 *    - 你之前的文件里同时出现过 startAction 和 leftAction，二者必有一个错。
 *    - 打开 top.yukonga.miuix.kmp.preference.ArrowPreference 的源码看真实签名，
 *      用对的那个，本文件统一改成它。
 *
 * 2. ArrowPreference 右侧值的显示方式：
 *    - 我这里用了 summary = "GitHub" 让值显示在标题下方。
 *    - 如果你想要值显示在右侧（像截图那样 "View Source ... GitHub >"），
 *      把 summary 换成你之前用过的 endActions：
 *        endActions = {
 *            Text("GitHub",
 *                fontSize = MiuixTheme.textStyles.body2.fontSize,
 *                color = MiuixTheme.colorScheme.onSurfaceVariantActions)
 *        }
 *    - 两种都行，看你要的视觉。截图是右侧显示，那就用 endActions。
 *
 * 3. SmallTopAppBar 是否存在：
 *    - 如果你的 0.9.1 没有 SmallTopAppBar，换成普通 TopAppBar。
 *
 * 其它都是标准 API，应该直接能编译。
 */
 