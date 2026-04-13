package com.github.zly2006.zhihu.markdown

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.zly2006.zhihu.DummyLocalNavigator
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MdAstInlineMathTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    data class MathInfo(
        val math: String,
        val rect: Rect,
    )

    @Test
    fun adjacentInlineMathBounds_doNotOverlapEachOther() {
        val recordedBounds = mutableMapOf<String, MathInfo>()

        composeTestRule.setContent {
            DummyLocalNavigator {
                MaterialTheme {
                    val html = InstrumentationRegistry
                        .getInstrumentation()
                        .context.assets
                        .open("answer_237992320.html")
                        .bufferedReader()
                        .use { it.readText() }
                    Box(
                        modifier = Modifier
                            .background(Color.White)
                            .padding(24.dp),
                    ) {
                        CompositionLocalProvider(
                            LocalMarkdownOnInlineMathPositioned provides { math, rect ->
                                recordedBounds[math.math] = MathInfo(
                                    math = math.math,
                                    rect = rect,
                                )
                            },
                        ) {
                            RenderMarkdown(
                                html = html,
                            )
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        val overlappingPairs = mutableListOf<Pair<MathInfo, MathInfo>>()
        val bounds = recordedBounds.values.toList()
        for (i in bounds.indices) {
            for (j in i + 1 until bounds.size) {
                if (bounds[i].rect.overlaps(bounds[j].rect)) {
                    overlappingPairs += bounds[i] to bounds[j]
                }
            }
        }

        assertFalse(
            "Expected inline math bounds parsed from answer_237992320 to stay separate, but found overlaps: $overlappingPairs ; bounds=$bounds ; raw=$recordedBounds",
            overlappingPairs.isNotEmpty(),
        )
    }
}
