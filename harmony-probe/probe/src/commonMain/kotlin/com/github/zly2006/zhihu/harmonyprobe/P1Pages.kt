package com.github.zly2006.zhihu.harmonyprobe

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Search
import com.github.zly2006.zhihu.shared.data.DailyStory
import com.github.zly2006.zhihu.shared.theme.ThemeMode
import com.github.zly2006.zhihu.theme.ThemeManager

/**
 * P1 主壳的内存数据页面。数据为固定内存样本，仅用于验证主壳结构、typed Navigation 与主题管线；
 * 接入真实网络与页面实现属于 P2/后续阶段。
 */

private data class P1FeedItem(
    val id: Long,
    val type: ArticleType,
    val title: String,
    val excerpt: String,
)

private val P1_FEED = listOf(
    P1FeedItem(624949300, ArticleType.Answer, "如何看待 Compose Multiplatform 登陆 HarmonyOS？", "typed route、返回栈与状态恢复在真机上的表现。"),
    P1FeedItem(721055100, ArticleType.Article, "Kotlin/Native 双架构编译实战", "ohosArm64 与 ohosX64 的构建链路。"),
    P1FeedItem(721055101, ArticleType.Article, "Material 3 在鸿蒙上的第一屏", "真实 ThemeManager 状态驱动的主题管线。"),
)

private val P1_DAILY_STORIES = listOf(
    DailyStory(id = 9712001, title = "内存样本：日报列表第一篇", url = "", hint = "3 分钟阅读", images = emptyList(), type = 0),
    DailyStory(id = 9712002, title = "内存样本：日报列表第二篇", url = "", hint = "2 分钟阅读", images = emptyList(), type = 0),
)

@Composable
private fun P1PageHeader(title: String, subtitle: String? = null) {
    Column(Modifier.padding(top = 24.dp, bottom = 8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun P1Row(title: String, subtitle: String? = null, tag: String? = null, onClick: (() -> Unit)? = null) {
    Column(
        Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .let { if (tag != null) it.testTag(tag) else it }
            .padding(vertical = 12.dp, horizontal = 16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun P1HomePage(innerPadding: PaddingValues) {
    val navigator = LocalNavigator.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            P1PageHeader(
                "P1 共享主壳",
                "主页 · 内存数据 · 真实 typed Navigation",
            )
            Surface(
                Modifier.fillMaxWidth().clickable {
                    navigator.onNavigate(Search(query = "鸿蒙 typed route"))
                },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("🔍 搜索", style = MaterialTheme.typography.titleMedium)
                    Text("跳转 Search(query) —— 验证带字符串参数的 typed route", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        items(P1_FEED.size) { index ->
            val item = P1_FEED[index]
            Surface(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        navigator.onNavigate(
                            Article(
                                title = item.title,
                                type = item.type,
                                id = item.id,
                                authorName = "P1 内存样本",
                                excerpt = item.excerpt,
                            ),
                        )
                    }
                    .testTag("p1_feed_item_${item.id}"),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(item.excerpt, style = MaterialTheme.typography.bodySmall)
                    Text(item.type.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Text(
                "内存样本不含真实网络数据；P2 日报切片在「账号」页打开。",
                Modifier.padding(vertical = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun P1DailyPage(innerPadding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { P1PageHeader("日报", "复用共享 DailyStory 模型的内存列表") }
        items(P1_DAILY_STORIES.size) { index ->
            val story = P1_DAILY_STORIES[index]
            Surface(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(story.title, style = MaterialTheme.typography.titleMedium)
                    Text(story.hint, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
internal fun P1AccountPage(innerPadding: PaddingValues) {
    val navigator = LocalNavigator.current
    val initialThemeMode = ThemeManager.getThemeMode()
    var followSystem by remember { mutableStateOf(initialThemeMode == ThemeMode.SYSTEM) }
    Column(
        Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        P1PageHeader("账号", "真实 Account.* typed routes · 内存设置项")

        P1Row("外观设置", "Account.AppearanceSettings(setting) — 带参数 typed route") {
            navigator.onNavigate(Account.AppearanceSettings(setting = "from=account"))
        }
        HorizontalDivider()
        P1Row("推荐与屏蔽设置", "Account.RecommendSettings(setting)") {
            navigator.onNavigate(Account.RecommendSettings(setting = "from=account"))
        }
        HorizontalDivider()
        P1Row("系统与更新", "Account.SystemAndUpdateSettings") {
            navigator.onNavigate(Account.SystemAndUpdateSettings)
        }
        HorizontalDivider()
        P1Row("开源许可证", "Account.OpenSourceLicenses") {
            navigator.onNavigate(Account.OpenSourceLicenses)
        }
        HorizontalDivider()
        P1Row("开发者选项", "Account.DeveloperSettings → ColorScheme 二级嵌套 route") {
            navigator.onNavigate(Account.DeveloperSettings)
        }
        HorizontalDivider()

        Spacer(Modifier.height(16.dp))
        Text("主题（真实 ThemeManager 状态）", style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("跟随系统深色模式（当前系统：${if (P1ShellState.systemDarkMode) "深色" else "浅色"}）")
            Switch(
                checked = followSystem,
                onCheckedChange = {
                    followSystem = it
                    ThemeManager.setThemeMode(if (it) ThemeMode.SYSTEM else ThemeMode.LIGHT)
                },
                modifier = Modifier.testTag("p1_follow_system_switch"),
            )
        }
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { ThemeManager.setThemeMode(ThemeMode.LIGHT) }, modifier = Modifier.testTag("p1_theme_light")) {
                Text("浅色")
            }
            Button(onClick = { ThemeManager.setThemeMode(ThemeMode.DARK) }, modifier = Modifier.testTag("p1_theme_dark")) {
                Text("深色")
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("已验证切片", style = MaterialTheme.typography.titleMedium)
        P1Row("P2 知乎日报访客切片", "打开 P2 阶段的日报阅读验证内容") {
            P1ShellState.p2SliceOpen = true
        }
        Text(
            "返回键、深色模式推送与状态恢复由宿主壳提供；详见 harmony-probe/README.md。",
            Modifier.padding(vertical = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun P1ArticlePage(article: Article, innerPadding: PaddingValues) {
    val navigator = LocalNavigator.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        P1PageHeader(article.title, "${article.type} · id=${article.id}")
        Text("作者：${article.authorName}", style = MaterialTheme.typography.bodySmall)
        article.excerpt?.let {
            Text(it, Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            "正文为内存占位；typed route 携带 ArticleType 自定义 NavType（ArticleTypeNavType）完成序列化与恢复。" +
                "点击系统返回键应回到原 tab 并保留滚动位置。",
            Modifier.padding(vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = { navigator.onNavigateBack() },
            modifier = Modifier.padding(vertical = 12.dp).testTag("p1_article_back"),
        ) {
            Text("返回（LocalNavigator.onNavigateBack）")
        }
    }
}

@Composable
internal fun P1SearchPage(search: Search, innerPadding: PaddingValues) {
    val navigator = LocalNavigator.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 18.dp),
    ) {
        P1PageHeader("搜索", "Search(query=\"${search.query}\")")
        Text("typed route 的字符串参数成功序列化并恢复。")
        Button(
            onClick = { navigator.onNavigateBack() },
            modifier = Modifier.padding(vertical = 12.dp).testTag("p1_search_back"),
        ) {
            Text("返回")
        }
    }
}

@Composable
internal fun P1SettingsPage(title: String, subtitle: String, innerPadding: PaddingValues) {
    val navigator = LocalNavigator.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 18.dp),
    ) {
        P1PageHeader(title, subtitle)
        Text("内存设置页面：验证共享 typed route 在 OHOS 上的压栈与返回。")
        Button(
            onClick = { navigator.onNavigateBack() },
            modifier = Modifier.padding(vertical = 12.dp).testTag("p1_settings_back"),
        ) {
            Text("返回")
        }
    }
}

@Composable
internal fun P1LicensePage(innerPadding: PaddingValues) {
    P1SettingsPage(
        title = "开源许可证",
        subtitle = "AGPL-3.0-only · 探针第三方组件见 THIRD_PARTY_NOTICES.md",
        innerPadding = innerPadding,
    )
}

@Composable
internal fun P1DeveloperSettingsPage(innerPadding: PaddingValues) {
    val navigator = LocalNavigator.current
    Column(
        Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 18.dp),
    ) {
        P1PageHeader("开发者选项", "二级嵌套 route")
        P1Row("颜色方案测试", "Account.DeveloperSettings.ColorScheme") {
            navigator.onNavigate(Account.DeveloperSettings.ColorScheme)
        }
        HorizontalDivider()
        Button(
            onClick = { navigator.onNavigateBack() },
            modifier = Modifier.padding(vertical = 12.dp).testTag("p1_dev_back"),
        ) {
            Text("返回")
        }
    }
}

@Composable
internal fun P1ColorSchemePage(innerPadding: PaddingValues) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        P1PageHeader("颜色方案", "Account.DeveloperSettings.ColorScheme")
        Text("primary: ${MaterialTheme.colorScheme.primary}", color = MaterialTheme.colorScheme.primary)
        Text("background: ${MaterialTheme.colorScheme.background}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("当前主题模式：${ThemeManager.getThemeMode().name}")
        Text("系统深色推送：${if (P1ShellState.systemDarkMode) "深色" else "浅色"}")
    }
}
