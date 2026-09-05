package com.github.zly2006.zhihu.harmonyprobe

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.Typography

/**
 * P1 主题：复用真实 ThemeManager 状态（主题模式、种子色、背景色）与真实 Typography。
 * material-kolor 动态取色未进探针，按可行性文档的推荐先用静态 seed 方案替代动态色路径。
 */
@Composable
fun P1Theme(content: @Composable () -> Unit) {
    val darkTheme = ThemeManager.isDarkTheme()
    val seed = ThemeManager.getCustomColor()
    val background = ThemeManager.getBackgroundColor()
    val colorScheme = if (darkTheme) {
        darkColorScheme(primary = seed, background = background, surface = background)
    } else {
        lightColorScheme(primary = seed, background = background, surface = background)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
