package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question

@Composable
actual fun HomeScreen(scrollToTopTrigger: Int, innerPadding: PaddingValues) = UnsupportedDesktopScreen()

@Composable
actual fun AccountSettingScreen(innerPadding: PaddingValues) = UnsupportedDesktopScreen()

@Composable
actual fun QuestionScreen(question: Question) = UnsupportedDesktopScreen()

@Composable
actual fun PeopleScreen(person: Person) = UnsupportedDesktopScreen()

@Composable
actual fun PinScreen(pin: Pin) = UnsupportedDesktopScreen()

@Composable
actual fun BlocklistSettingsScreen(nlpContent: BlocklistSettingsNlpContent?) = UnsupportedDesktopScreen()

@Composable
private fun UnsupportedDesktopScreen() {
    Text("Desktop screen implementation is pending migration.")
}
