package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
actual fun AccountSettingScreen(innerPadding: PaddingValues) = UnsupportedDesktopScreen()

@Composable
actual fun BlocklistSettingsScreen(nlpContent: BlocklistSettingsNlpContent?) = UnsupportedDesktopScreen()

@Composable
private fun UnsupportedDesktopScreen() {
    Text("Desktop screen implementation is pending migration.")
}
