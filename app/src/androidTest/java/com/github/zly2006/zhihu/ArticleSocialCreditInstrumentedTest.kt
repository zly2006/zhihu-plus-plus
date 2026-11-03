package com.github.zly2006.zhihu

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.ArticleScreen
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import com.github.zly2006.zhihu.viewmodel.sharedArticleAnswerSwitchState
import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ArticleSocialCreditInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/797
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/802
     * 缓存正文已显示时加载赞同人数，再加载较长的赞同说明，正文起始位置都应保持不变。
     * 通过真实 ArticleScreen 的布局坐标检测异步信息插入造成的阅读位置跳动。
     */
    @Test
    fun delayedVotersDoNotMoveAnswerBody() {
        sharedArticleAnswerSwitchState.reset()
        composeRule.resetAppPreferences()
        composeRule.activity
            .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("pinAnswerDate", false)
            .commit()
        val article = Article(id = 123456L, type = ArticleType.Answer)
        val model = ArticleViewModel(article, null)
        composeRule.runOnIdle {
            model.title = "回答切换后的正文位置"
            model.authorName = "离线测试作者"
            model.content = "<p>赞同信息加载前后，这段正文应保持原位。</p><p>测试通过实际回答页面检查正文的位置。</p>"
            model.createdAt = 1710000000L
            model.updatedAt = model.createdAt
        }
        composeRule.setScreenContent { ArticleScreen(article, model) }
        val before = composeRule
            .onNodeWithTag("article_content")
            .fetchSemanticsNode()
            .boundsInRoot.top
        val environment = object : ZhihuApiEnvironment {
            override fun httpClient(): HttpClient = error("No network needed")

            override fun authenticatedCookies(): Map<String, String> = emptyMap()

            override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit

            override suspend fun fetchJson(url: String, include: String): JsonObject =
                Json
                    .parseToJsonElement(
                        if (url.contains("upvoters")) {
                            """{"paging":{"is_end":true,"totals":42,"next":""},"data":[]}"""
                        } else {
                            """{"text":"${"测试用户、".repeat(15)}等 42 人赞同了该回答"}"""
                        },
                    ).jsonObject
        }
        composeRule.runOnIdle { model.loadMoreVoters(environment, reset = true) }
        composeRule.waitUntil(5_000) { model.votersTotal == 42 }
        composeRule.onNodeWithText("42 人赞同了该回答").assertIsDisplayed()
        val after = composeRule
            .onNodeWithTag("article_content")
            .fetchSemanticsNode()
            .boundsInRoot.top
        assertEquals("异步赞同信息不应推动正文", before, after, 0.5f)
        composeRule.runOnIdle {
            model.loadAnswerRelationshipEndorsement(environment)
        }
        composeRule.waitUntil(5_000) { model.votersSocialText.isNotBlank() }
        assertEquals(
            before,
            composeRule
                .onNodeWithTag("article_content")
                .fetchSemanticsNode()
                .boundsInRoot.top,
            0.5f,
        )
    }
}
