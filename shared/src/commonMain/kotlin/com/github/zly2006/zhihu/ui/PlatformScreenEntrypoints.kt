package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable

typealias BlocklistSettingsNlpContent = @Composable (onNavigateBack: () -> Unit) -> Unit

@Composable
expect fun AccountSettingScreen(innerPadding: PaddingValues)

@Composable
expect fun BlocklistSettingsScreen(nlpContent: BlocklistSettingsNlpContent? = null)
