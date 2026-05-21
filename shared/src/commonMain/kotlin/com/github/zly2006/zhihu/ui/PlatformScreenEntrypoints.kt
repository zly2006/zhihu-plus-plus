package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.Search

@Composable
expect fun HomeScreen(scrollToTopTrigger: Int = 0, innerPadding: PaddingValues)

@Composable
expect fun FollowTopLevelPage(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    scrollToTopTrigger: Int,
    innerPadding: PaddingValues,
    isActive: Boolean,
)

@Composable
expect fun FollowScreen(scrollToTopTrigger: Int = 0, innerPadding: PaddingValues)

@Composable
expect fun HotListScreen(innerPadding: PaddingValues)

@Composable
expect fun LegacyLocalHistoryScreen(innerPadding: PaddingValues)

@Composable
expect fun OnlineHistoryScreen()

@Composable
expect fun AccountSettingScreen(innerPadding: PaddingValues)

@Composable
expect fun QuestionScreen(question: Question)

@Composable
expect fun SearchScreen(search: Search)

@Composable
expect fun CollectionContentScreen(collectionId: String)

@Composable
expect fun PeopleScreen(person: Person)

@Composable
expect fun PinScreen(pin: Pin)

@Composable
expect fun BlocklistSettingsScreen()
