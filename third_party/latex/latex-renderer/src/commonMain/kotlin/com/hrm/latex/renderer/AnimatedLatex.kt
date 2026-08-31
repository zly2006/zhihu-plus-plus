/*
 * Copyright (c) 2026 huarangmeng
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.hrm.latex.renderer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hrm.latex.renderer.model.LatexConfig

/**
 * 动画过渡类型
 */
enum class LatexTransition {
    /** 淡入淡出 */
    CROSSFADE,
    /** 从下方滑入 */
    SLIDE_UP,
    /** 从上方滑入 */
    SLIDE_DOWN,
    /** 淡入淡出 + 从下方滑入 */
    FADE_SLIDE
}

/**
 * 动画配置
 *
 * @param transition 过渡动画类型
 * @param durationMillis 动画持续时间（毫秒）
 */
data class LatexAnimationConfig(
    val transition: LatexTransition = LatexTransition.CROSSFADE,
    val durationMillis: Int = 300
)

/**
 * 带动画过渡的 LaTeX 渲染组件
 *
 * 当 [latex] 内容发生变化时，新旧公式之间以动画过渡效果切换。
 * 支持多种过渡类型：淡入淡出、滑动、组合效果。
 *
 * @param latex LaTeX 字符串
 * @param modifier 修饰符
 * @param config 渲染配置
 * @param animationConfig 动画配置
 * @param isDarkTheme 当前环境是否为深色模式。
 * 仅在 `config.theme = LatexTheme.auto(...)` 时用于选择 light/dark 色板。
 */
@Composable
fun AnimatedLatex(
    latex: String,
    modifier: Modifier = Modifier,
    config: LatexConfig = LatexConfig(),
    animationConfig: LatexAnimationConfig = LatexAnimationConfig(),
    isDarkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme()
) {
    AnimatedContent(
        targetState = latex,
        modifier = modifier,
        transitionSpec = { buildTransition(animationConfig) },
        label = "AnimatedLatex"
    ) { targetLatex ->
        Latex(
            latex = targetLatex,
            config = config,
            isDarkTheme = isDarkTheme
        )
    }
}

/**
 * 根据配置构建过渡动画
 */
private fun buildTransition(config: LatexAnimationConfig): ContentTransform {
    val duration = config.durationMillis
    return when (config.transition) {
        LatexTransition.CROSSFADE ->
            fadeIn(tween(duration)) togetherWith fadeOut(tween(duration))

        LatexTransition.SLIDE_UP ->
            slideInVertically(tween(duration)) { it } togetherWith
                slideOutVertically(tween(duration)) { -it }

        LatexTransition.SLIDE_DOWN ->
            slideInVertically(tween(duration)) { -it } togetherWith
                slideOutVertically(tween(duration)) { it }

        LatexTransition.FADE_SLIDE ->
            (fadeIn(tween(duration)) + slideInVertically(tween(duration)) { it / 3 }) togetherWith
                (fadeOut(tween(duration)) + slideOutVertically(tween(duration)) { -it / 3 })
    }
}
