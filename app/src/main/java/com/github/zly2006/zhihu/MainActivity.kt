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
import androidx.navigation.ui.setupWithNavController
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.databinding.ActivityMainBinding
import com.github.zly2006.zhihu.placeholder.PlaceholderItem
import com.github.zly2006.zhihu.ui.home.HomeFragment
import com.github.zly2006.zhihu.ui.home.ReadArticleFragment
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



class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    class MainActivityViewModel: ViewModel() {
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
            fragment<HomeFragment, Dashboard> {
                label = "Dashboard"
            }
            fragment<HomeFragment, Notifications> {
                label = "Notifications"
            }
            fragment<ReadArticleFragment, Article>(
            )
        }
        navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.hasRoute<Home>() ||
                destination.hasRoute<Dashboard>() ||
                destination.hasRoute<Notifications>()
                )
            {
                navView.visibility = View.VISIBLE
            }
            else {
                navView.visibility = View.GONE
            }
        }
    }
}
