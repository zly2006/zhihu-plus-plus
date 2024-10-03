package com.github.zly2006.zhihu

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.createGraph
import androidx.navigation.findNavController
import androidx.navigation.fragment.fragment
import androidx.navigation.ui.setupWithNavController
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.databinding.ActivityMainBinding
import com.github.zly2006.zhihu.ui.home.HomeFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.serialization.Serializable

@Serializable
data object Home

@Serializable
data object Dashboard

@Serializable
data object Notifications

enum class ArticleType {
    Article,
    Answer,
}

@Serializable
data class Article(
    val title: String,
    val type: ArticleType,
    val id: Long,
    val author: String,
    val authorBio: String,
)



class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

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
        navView.setupWithNavController(navController)

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
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            navView.visibility = when (destination.id) {
                R.id.navigation_home -> View.VISIBLE
                R.id.navigation_dashboard -> View.VISIBLE
                R.id.navigation_notifications -> View.VISIBLE
                else -> View.GONE
            }
        }
    }
}
