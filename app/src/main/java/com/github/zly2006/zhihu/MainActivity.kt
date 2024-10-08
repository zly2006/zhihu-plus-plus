package com.github.zly2006.zhihu

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavDestination

@Serializable
data object Home: NavDestination

@Serializable
data object Dashboard: NavDestination

@Serializable
data object Notifications: NavDestination

@Serializable
data object Settings: NavDestination

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
    var authorName: String,
    var authorBio: String,
    var content: String? = null,
    var avatarSrc: String? = null
): NavDestination {
    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is Article && other.id == id
    }
}

@Serializable
data class Question(
    val questionId: Long,
    val title: String
): NavDestination {
    override fun hashCode(): Int {
        return questionId.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is Question && other.questionId == questionId
    }
}

class MainActivity : AppCompatActivity() {
//    val history = HistoryStorage(this)
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

        val navView = binding.navView

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

        val uri = intent.data
        if (uri?.host == "zhihu.com" || uri?.host == "www.zhihu.com") {
            if (uri.pathSegments.size == 4
                && uri.pathSegments[0] == "question"
                && uri.pathSegments[2] == "answer"
            ) {
                val questionId = uri.pathSegments[1].toLong()
                val answerId = uri.pathSegments[3].toLong()
                navController.navigate(
                    Article(
                        "loading...",
                        "answer",
                        answerId,
                        "loading...",
                        "loading...",
                        null,
                        null
                    )
                )
            } else if (uri.pathSegments.size == 2
                && uri.pathSegments[0] == "answer"
            ) {
                val answerId = uri.pathSegments[1].toLong()
                navController.navigate(
                    Article(
                        "loading...",
                        "answer",
                        answerId,
                        "loading...",
                        "loading...",
                        null,
                        null
                    )
                )
            } else if (uri.pathSegments.size == 2
                && uri.pathSegments[0] == "question"
            ) {
                val questionId = uri.pathSegments[1].toLong()
                navController.navigate(
                    Question(
                        questionId,
                        "loading...",
                    )
                )
            } else {
                Toast.makeText(this, "Invalid URL (not question or answer)", Toast.LENGTH_LONG).show()
            }
        }
    }
}
