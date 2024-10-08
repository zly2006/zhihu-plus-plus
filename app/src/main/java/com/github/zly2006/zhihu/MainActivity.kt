package com.github.zly2006.zhihu

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.createGraph
import androidx.navigation.findNavController
import androidx.navigation.fragment.fragment
import androidx.navigation.get
import androidx.navigation.ui.setupWithNavController
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.databinding.ActivityMainBinding
import com.github.zly2006.zhihu.placeholder.PlaceholderItem
import com.github.zly2006.zhihu.ui.SettingsFragment
import com.github.zly2006.zhihu.ui.dashboard.DashboardFragment
import com.github.zly2006.zhihu.ui.home.HomeFragment
import com.github.zly2006.zhihu.ui.home.ReadArticleFragment
import com.github.zly2006.zhihu.ui.home.question.QuestionDetailsFragment
import com.github.zly2006.zhihu.ui.notifications.NotificationsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data object Home

@Serializable
data object Dashboard

@Serializable
data object Notifications

@Serializable
data object Settings

@Serializable
enum class ArticleType {
    @SerialName("article")
    Article,
    @SerialName("answer")
    Answer,
    ;

    override fun toString(): String {
        return name.lowercase()
    }
}

@Serializable
data class Article(
    val title: String,
    val type: String,
    val id: Long,
    val authorName: String,
    val authorBio: String,
    val content: String? = null,
    val avatarSrc: String? = null
)

@Serializable
data class Question(
    val questionId: Long,
    val title: String
)

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    class MainActivityViewModel : ViewModel() {
        val list = mutableListOf<PlaceholderItem>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = AccountData.getData(this)
        if (!data.login) {
            val myIntent = Intent(this, LoginActivity::class.java)
            startActivity(myIntent)
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)


        navController.graph = navController.createGraph(
            startDestination = Home::class,
        ) {
            fragment<HomeFragment, Home> {
                label = "Home"
            }
            fragment<DashboardFragment, Dashboard> {
                label = "Dashboard"
            }
            fragment<NotificationsFragment, Notifications> {
                label = "Notifications"
            }
            fragment<ReadArticleFragment, Article>(
            )
            fragment<SettingsFragment, Settings>(
            )
            fragment<QuestionDetailsFragment, Question>()
        }
        navView.menu.add(0, navController.graph[Home].id, 0, "主页").apply {
            icon = getDrawable(R.drawable.ic_home_black_24dp)
        }
        navView.menu.add(0, navController.graph[Dashboard].id, 0, "状态").apply {
            icon = getDrawable(R.drawable.ic_dashboard_black_24dp)
        }
        navView.menu.add(0, navController.graph[Settings].id, 0, "设置").apply {
            icon = getDrawable(R.drawable.ic_notifications_black_24dp)
        }
        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.hasRoute<Home>() ||
                destination.hasRoute<Dashboard>() ||
                destination.hasRoute<Settings>()
            ) {
                navView.visibility = View.VISIBLE
            } else {
                navView.visibility = View.GONE
            }
        }
    }
}
