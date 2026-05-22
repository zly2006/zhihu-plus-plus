package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
actual fun SystemAndUpdateSettingsScreen() = UnsupportedDesktopSubscreen()

@Composable
private fun UnsupportedDesktopSubscreen() {
    Text("Desktop settings implementation is pending migration.")
}
