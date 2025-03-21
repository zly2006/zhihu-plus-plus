package com.github.zly2006.zhihu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.createGraph
import androidx.navigation.findNavController
import androidx.navigation.fragment.fragment
import androidx.navigation.get
import androidx.navigation.ui.setupWithNavController
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.HistoryStorage
import com.github.zly2006.zhihu.data.HistoryStorage.Companion.navigate
import com.github.zly2006.zhihu.databinding.ActivityMainBinding
import com.github.zly2006.zhihu.legacy.placeholder.PlaceholderItem
import com.github.zly2006.zhihu.legacy.ui.SettingsFragment
import com.github.zly2006.zhihu.legacy.ui.dashboard.DashboardFragment
import com.github.zly2006.zhihu.legacy.ui.home.HomeFragment
import com.github.zly2006.zhihu.legacy.ui.home.ReadArticleFragment
import com.github.zly2006.zhihu.legacy.ui.home.question.QuestionDetailsFragment
import com.github.zly2006.zhihu.legacy.ui.home.setupUpWebview
import com.github.zly2006.zhihu.legacy.ui.notifications.NotificationsFragment
import io.ktor.client.request.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.lang.Thread.UncaughtExceptionHandler
import java.security.MessageDigest
import kotlin.math.min

class LegacyMainActivity : AppCompatActivity() {
    lateinit var history: HistoryStorage
    private lateinit var binding: ActivityMainBinding

    class MainActivityViewModel : ViewModel() {
        val list = mutableListOf<PlaceholderItem>()
        val toast = MutableLiveData<String>()
    }

    private val gViewModel: MainActivityViewModel by viewModels()

    val exceptionHandler = UncaughtExceptionHandler { t, e ->
        Log.e("UncaughtException", "Uncaught exception", e)
        runOnUiThread {
            AlertDialog.Builder(this).apply {
                setTitle("Application crashed")
                setMessage(e.toString().substring(0, min(e.toString().length, 100)))
                setNeutralButton("Report") { _, _ ->
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Crash report", e.stackTraceToString())
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this@LegacyMainActivity, "Copied bug details to clipboard", Toast.LENGTH_LONG).show()
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://github.com/zly2006/zhihu-plus-plus/issues/new")
                    startActivity(intent)
                }
                setPositiveButton("OK") { _, _ ->
                }
            }.create().show()
        }
    }

    lateinit var webview: WebView

    suspend fun signRequest96(url: String): String {
        val dc0 = AccountData.getData().cookies["d_c0"] ?: ""
        val zse93 = "101_3_3.0"
        val pathname = "/" + url.substringAfter("//").substringAfter('/')
        val signSource = "$zse93+$pathname+$dc0"
        val md5 = MessageDigest.getInstance("MD5").digest(signSource.toByteArray()).joinToString("") {
            "%02x".format(it)
        }
        val future = CompletableDeferred<String>()
        webview.evaluateJavascript("exports.encrypt('$md5')") {
            Log.i("MainActivity", "Sign request: $url")
            Log.i("MainActivity", "Sign source: $signSource")
            Log.i("MainActivity", "Sign result: $it")
            future.complete(it.trim('"'))
        }
        return "2.0_" + future.await()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler)
        Thread.currentThread().uncaughtExceptionHandler = exceptionHandler

        val data = AccountData.getData(this)
        webview = WebView(this)
        Log.i("MainActivity", "Webview created")
        setupUpWebview(webview, this)
        webview.settings.javaScriptEnabled = true
        webview.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                Log.i("MainActivity", "Console message: ${consoleMessage?.message()}")
                return true
            }
        }
        webview.loadUrl("https://zhihu-plus.internal/assets/zse.html")
        history = HistoryStorage(this)
        if (!data.login) {
            val myIntent = Intent(this, LoginActivity::class.java)
            startActivity(myIntent)
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        window.statusBarColor = Color.TRANSPARENT
        setContentView(binding.root)

        gViewModel.toast.distinctUntilChanged().observe(this) {
            if (it.isNotEmpty()) {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
            }
        }
        val navView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)


        navController.graph = navController.createGraph(
            startDestination = Home,
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
        Log.i("MainActivity", "Intent data: $uri")
        if (uri != null) {
            val destination = resolveContent(uri)
            if (destination != null) {
                navigate(destination)
            } else {
                AlertDialog.Builder(this).apply {
                    setTitle("Unsupported URL")
                    setMessage("Unknown URL: $uri")
                    setPositiveButton("OK") { _, _ ->
                    }
                }.create().show()
            }
        }
    }

}

fun <T> FragmentActivity.catching(action: () -> T): T? = if (this is LegacyMainActivity) {
    try {
        action()
    } catch (e: Exception) {
        exceptionHandler.uncaughtException(Thread.currentThread(), e)
        null
    }
} else {
    action()
}

suspend fun HttpRequestBuilder.signFetchRequest(context: Context) {
    val url = url.buildString()
    withContext(context.mainExecutor.asCoroutineDispatcher()) {
        header("x-zse-93", "101_3_3.0")
        header("x-zse-96",
            (context as? LegacyMainActivity)?.signRequest96(url) ?:
            (context as? MainActivity)?.signRequest96(url)
        )
        header("x-requested-with", "fetch")
    }
}
