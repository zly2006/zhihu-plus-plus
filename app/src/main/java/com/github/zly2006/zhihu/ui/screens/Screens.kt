package com.github.zly2006.zhihu.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.github.zly2006.zhihu.Account
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.Blocklist
import com.github.zly2006.zhihu.CollectionContent
import com.github.zly2006.zhihu.Collections
import com.github.zly2006.zhihu.CommentHolder
import com.github.zly2006.zhihu.Follow
import com.github.zly2006.zhihu.History
import com.github.zly2006.zhihu.Home
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.Person
import com.github.zly2006.zhihu.Question
import com.github.zly2006.zhihu.Search
import com.github.zly2006.zhihu.Video
import com.github.zly2006.zhihu.ui.AccountSettingScreen
import com.github.zly2006.zhihu.ui.ArticleScreen
import com.github.zly2006.zhihu.ui.BlocklistSettingsScreen
import com.github.zly2006.zhihu.ui.CollectionContentScreen
import com.github.zly2006.zhihu.ui.CollectionScreen
import com.github.zly2006.zhihu.ui.FollowScreen
import com.github.zly2006.zhihu.ui.HistoryScreen
import com.github.zly2006.zhihu.ui.HomeScreen
import com.github.zly2006.zhihu.ui.PeopleScreen
import com.github.zly2006.zhihu.ui.QuestionScreen
import com.github.zly2006.zhihu.ui.SearchScreen
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel

/**
 * Voyager Screen wrappers for existing screens to make navigation KMP-compatible
 */

data object HomeScreenVoyager : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val activity = androidx.activity.compose.LocalActivity.current as? MainActivity
        activity?.navigator = navigator
        HomeScreen { destination ->
            navigator.push(destination.toScreen())
        }
    }
}

data class QuestionScreenVoyager(val question: Question) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        QuestionScreen(question) { destination ->
            navigator.push(destination.toScreen())
        }
    }
}

data class ArticleScreenVoyager(val article: Article) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val activity = androidx.activity.compose.LocalActivity.current as? MainActivity
        if (activity != null) {
            val articleViewModel = viewModel<ArticleViewModel> {
                ArticleViewModel(article, activity.httpClient, null)
            }
            ArticleScreen(article, articleViewModel) { destination ->
                navigator.push(destination.toScreen())
            }
        }
    }
}

data object FollowScreenVoyager : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        FollowScreen { destination ->
            navigator.push(destination.toScreen())
        }
    }
}

data object HistoryScreenVoyager : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        HistoryScreen { destination ->
            navigator.push(destination.toScreen())
        }
    }
}

data object AccountScreenVoyager : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        AccountSettingScreen(PaddingValues(0.dp)) { destination ->
            navigator.push(destination.toScreen())
        }
    }
}

data class SearchScreenVoyager(val search: Search) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        SearchScreen(
            search,
            { destination ->
                navigator.push(destination.toScreen())
            },
        ) {
            navigator.pop()
        }
    }
}

data class CollectionsScreenVoyager(val userToken: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        CollectionScreen(userToken) { destination ->
            navigator.push(destination.toScreen())
        }
    }
}

data class CollectionContentScreenVoyager(val collectionId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        CollectionContentScreen(collectionId) { destination ->
            navigator.push(destination.toScreen())
        }
    }
}

data class PersonScreenVoyager(val person: Person) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        PeopleScreen(person) { destination ->
            navigator.push(destination.toScreen())
        }
    }
}

data object BlocklistScreenVoyager : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        BlocklistSettingsScreen(
            innerPadding = PaddingValues(0.dp),
            onNavigateBack = { navigator.pop() },
        ) { destination ->
            navigator.push(destination.toScreen())
        }
    }
}

/**
 * Extension function to convert NavDestination to Voyager Screen
 */
fun NavDestination.toScreen(): Screen = when (this) {
    is Home -> HomeScreenVoyager
    is Follow -> FollowScreenVoyager
    is History -> HistoryScreenVoyager
    is Account -> AccountScreenVoyager
    is Blocklist -> BlocklistScreenVoyager
    is Search -> SearchScreenVoyager(this)
    is Collections -> CollectionsScreenVoyager(this.userToken)
    is CollectionContent -> CollectionContentScreenVoyager(this.collectionId)
    is Article -> ArticleScreenVoyager(this)
    is Question -> QuestionScreenVoyager(this)
    is Person -> PersonScreenVoyager(this)
    is Video -> HomeScreenVoyager // fallback to home for unsupported types
    is CommentHolder -> HomeScreenVoyager // fallback to home for unsupported types
}
