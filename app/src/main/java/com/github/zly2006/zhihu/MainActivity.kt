package com.github.zly2006.zhihu

import android.content.ClipData
import android.content.ClipboardManager
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
import com.github.zly2006.zhihu.databinding.ActivityMainBinding
import com.github.zly2006.zhihu.placeholder.PlaceholderItem
import com.github.zly2006.zhihu.ui.SettingsFragment
import com.github.zly2006.zhihu.ui.dashboard.DashboardFragment
import com.github.zly2006.zhihu.ui.home.HomeFragment
import com.github.zly2006.zhihu.ui.home.ReadArticleFragment
import com.github.zly2006.zhihu.ui.home.question.QuestionDetailsFragment
import com.github.zly2006.zhihu.ui.home.setupUpWebview
import com.github.zly2006.zhihu.ui.notifications.NotificationsFragment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.lang.Thread.UncaughtExceptionHandler
import java.security.MessageDigest
import kotlin.math.min

@Serializable
sealed interface NavDestination

@Serializable
data object Home : NavDestination

@Serializable
data object Dashboard : NavDestination

@Serializable
data object Notifications : NavDestination

@Serializable
data object Settings : NavDestination

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
    var title: String,
    @SerialName("article_type_1")
    val type: String,
    val id: Long,
    var authorName: String,
    var authorBio: String,
    var avatarSrc: String? = null,
    var excerpt: String? = null,
) : NavDestination {
    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is Article && other.id == id
    }
}

@Serializable
data class CommentHolder(
    val commentId: String,
    val article: Article,
) : NavDestination

@Serializable
data class Question(
    val questionId: Long,
    val title: String
) : NavDestination {
    override fun hashCode(): Int {
        return questionId.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is Question && other.questionId == questionId
    }
}

class MainActivity : AppCompatActivity() {
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
                    Toast.makeText(this@MainActivity, "Copied bug details to clipboard", Toast.LENGTH_LONG).show()
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
        Log.i("MainActivity", "Intent data: $uri")
        if (uri?.scheme == "http" || uri?.scheme == "https") {
            if (uri.host == "zhihu.com" || uri.host == "www.zhihu.com") {
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
            else if (uri.host == "zhuanlan.zhihu.com") {
                if (uri.pathSegments.size == 2
                    && uri.pathSegments[0] == "p"
                ) {
                    val articleId = uri.pathSegments[1].toLong()
                    navController.navigate(
                        Article(
                            "loading...",
                            "article",
                            articleId,
                            "loading...",
                            "loading...",
                            null,
                            null
                        )
                    )
                }
                else {
                    AlertDialog.Builder(this).apply {
                        setTitle("Unsupported URL")
                        setMessage("Unknown URL: $uri")
                        setPositiveButton("OK") { _, _ ->
                        }
                    }.create().show()
                }
            }
            else {
                AlertDialog.Builder(this).apply {
                    setTitle("Unsupported URL")
                    setMessage("Unknown URL: $uri")
                    setPositiveButton("OK") { _, _ ->
                    }
                }.create().show()
            }
        }
        if (uri?.scheme == "zhihu") {
            if (uri.host == "answers") {
                val answerId = uri.pathSegments[0].toLong()
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
            } else if (uri.host == "questions") {
                val questionId = uri.pathSegments[0].toLong()
                navController.navigate(
                    Question(
                        questionId,
                        "loading...",
                    )
                )
            }
            else if (uri.host == "feed") {
                navController.navigate(Home)
            } else {
                AlertDialog.Builder(this).apply {
                    setTitle("Invalid URL")
                    setMessage("Unknown zhihu URL: $uri\nPlease report to the developer")
                    setPositiveButton("OK") { _, _ ->
                    }
                }.create().show()
            }
        }
    }
}

fun <T> FragmentActivity.catching(action: () -> T): T? = if (this is MainActivity) {
    try {
        action()
    } catch (e: Exception) {
        exceptionHandler.uncaughtException(Thread.currentThread(), e)
        null
    }
} else {
    action()
}

suspend fun <T> FragmentActivity.catchingS(action: suspend () -> T): T? = if (this is MainActivity) {
    try {
        action()
    } catch (e: Exception) {
        exceptionHandler.uncaughtException(Thread.currentThread(), e)
        null
    }
} else {
    action()
}
