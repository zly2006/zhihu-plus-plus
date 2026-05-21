package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.runtime.Composable

@Composable
expect fun AppearanceSettingsScreen(setting: String? = null, onExit: () -> Unit = {})

@Composable
expect fun ContentFilterSettingsScreen(setting: String? = null)

@Composable
expect fun DeveloperSettingsScreen()

@Composable
expect fun SystemAndUpdateSettingsScreen()
