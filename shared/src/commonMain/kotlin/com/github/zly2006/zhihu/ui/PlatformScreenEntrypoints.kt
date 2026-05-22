package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question

typealias BlocklistSettingsNlpContent = @Composable (onNavigateBack: () -> Unit) -> Unit

@Composable
expect fun AccountSettingScreen(innerPadding: PaddingValues)

@Composable
expect fun QuestionScreen(question: Question)

@Composable
expect fun PeopleScreen(person: Person)

@Composable
expect fun PinScreen(pin: Pin)

@Composable
expect fun BlocklistSettingsScreen(nlpContent: BlocklistSettingsNlpContent? = null)
