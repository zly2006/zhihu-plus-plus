package com.github.zly2006.zhihu.harmonyprobe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hrm.markdown.renderer.Markdown
import kotlinx.coroutines.CancellationException

@Composable
fun ProbeApp() {
    P1Theme {
        Surface(Modifier.fillMaxSize()) {
            if (P1ShellState.p2SliceOpen) {
                P2Slice()
            } else {
                P1Shell()
            }
        }
    }
}

@Composable
private fun P2Slice() {
    val homeScroll = rememberLazyListState()
    LaunchedEffect(Unit) {
        if (usesNativeNetwork) {
            try {
                loadNativeDaily()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                P2State.applyError("Ktor 日报加载失败：${error::class.simpleName}")
            }
        }
    }
    if (P2State.showArticle || P2State.showStress) {
        val detail = P2State.detail
        val stress = P2State.showStress
        val markdown = remember(detail?.body, stress) {
            if (stress) p2StressMarkdown() else dailyHtmlToMarkdown(detail?.body.orEmpty())
        }
        Column(Modifier.fillMaxSize()) {
            Button(
                onClick = { P2State.showArticle = false; P2State.showStress = false },
                modifier = Modifier.padding(horizontal = 18.dp),
            ) { Text("← 返回日报首页") }
            Markdown(
                markdown = markdown,
                modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                enableSelection = false,
                header = {
                    Column {
                        Text(
                            if (stress) "P2 阅读器压力样本" else detail?.title.orEmpty(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        if (!stress && detail != null && detail.image.isNotBlank()) {
                            P2Cover(detail.image, detail.title, Modifier.fillMaxWidth().height(230.dp))
                        }
                        Text(
                            if (stress) "80 公式 · 300 列表项 · 200 表格行 · Kotlin 高亮"
                            else "安卓同源 Markdown / LaTeX / 代码高亮",
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                imageContent = { image, modifier ->
                    P2Cover(image.url, image.altText, modifier.fillMaxWidth().height(220.dp))
                },
                footer = {
                    Text(
                        if (stress) "— P2 压力样本结束 —" else "— 文章结束 —",
                        Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        }
    } else {
        LazyColumn(
            state = homeScroll,
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(Modifier.height(18.dp))
                Text("P2 知乎日报访客切片", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(P2State.sessionStatus, style = MaterialTheme.typography.bodySmall)
                Text(
                    if (usesNativeNetwork) "arm64：CPF Ktor CIO + Coil"
                    else "x64：NetworkKit + Skia 图片适配",
                    style = MaterialTheme.typography.bodySmall,
                )
                P2State.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = { P2State.showStress = true }) { Text("运行阅读器压力样本") }
                Button(onClick = { P1ShellState.p2SliceOpen = false }) { Text("← 返回 P1 主壳") }
            }
            val home = P2State.home
            if (home == null) {
                item { CircularProgressIndicator() }
            } else {
                items(home.stories, key = { it.id }) { story ->
                    val ready = story.id == P2State.detail?.id
                    Column(
                        Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                            .clickable(enabled = ready) { P2State.showArticle = true }
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(story.title, style = MaterialTheme.typography.titleMedium)
                        Text(story.hint, style = MaterialTheme.typography.bodySmall)
                        Text(
                            if (ready) "已预载 · 点击阅读" else "此切片只预载阅读时长最长的一篇",
                            color = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
                item { Text("知乎日报公开 API · ${home.date}", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

