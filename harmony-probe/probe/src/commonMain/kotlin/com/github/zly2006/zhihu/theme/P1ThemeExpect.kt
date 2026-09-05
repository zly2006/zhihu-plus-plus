package com.github.zly2006.zhihu.theme

import androidx.compose.runtime.Composable

/**
 * 探针只编译共享 ThemeManager，不编译完整 Theme.kt（其 material-kolor 动态取色尚未进探针）。
 * 这里为 ThemeManager.isDarkTheme() 补上它依赖的 expect 声明；OHOS actual 由 ArkTS 壳推送系统深色状态。
 */
@Composable
expect fun currentSystemInDarkTheme(): Boolean
