package com.github.zly2006.zhihu.theme

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.harmonyprobe.P1ShellState

/**
 * OHOS actual：ArkTS 壳在 onConfigurationUpdate / onWindowStageCreate 中推送系统深浅色，
 * 由 P1ShellState 缓存。完整 SystemColorMode 监听属于后续平台能力工作。
 */
@Composable
actual fun currentSystemInDarkTheme(): Boolean = P1ShellState.systemDarkMode
