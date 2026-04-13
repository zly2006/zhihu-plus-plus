package com.github.zly2006.zhihu

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import com.github.zly2006.zhihu.util.enableEdgeToEdgeCompat
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import com.github.zly2006.zhihu.util.telemetry
import kotlinx.coroutines.runBlocking

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeCompat()

        setContent {
            ZhihuTheme {
                var currentNoticeStep by rememberSaveable { mutableIntStateOf(0) }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                ) {
                    if (currentNoticeStep >= 3) {
                        WebviewComp(
                            modifier = Modifier.fillMaxSize(),
                            onLoad = { webView ->
                                webView.setupUpWebviewClient()
                                @SuppressLint("SetJavaScriptEnabled")
                                webView.settings.javaScriptEnabled = true
                                CookieManager.getInstance().removeAllCookies { }
                                webView.webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest,
                                    ): Boolean {
                                        if (request.url.toString() == "https://www.zhihu.com/") {
                                            webView.settings.userAgentString = AccountData.ANDROID_USER_AGENT
                                        }
                                        if (request.url?.scheme == "zhihu") {
                                            return true
                                        }
                                        return false
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        if (url == "https://www.zhihu.com/") {
                                            val cookies =
                                                CookieManager
                                                    .getInstance()
                                                    .getCookie("https://www.zhihu.com/")
                                                    .orEmpty()
                                                    .split(";")
                                                    .associate {
                                                        it.substringBefore("=").trim() to it.substringAfter("=")
                                                    }
                                            runBlocking {
                                                if (AccountData.verifyLogin(this@LoginActivity, cookies)) {
                                                    val data = AccountData.loadData(this@LoginActivity)

                                                    val preferences = this@LoginActivity.getSharedPreferences(
                                                        PREFERENCE_NAME,
                                                        MODE_PRIVATE,
                                                    )
                                                    print(preferences.toString())

                                                    AlertDialog
                                                        .Builder(this@LoginActivity)
                                                        .apply {
                                                            setTitle("登录成功")
                                                            setMessage("欢迎回来，${data.username}")
                                                            setPositiveButton("OK") { _, _ ->
                                                            }
                                                            setOnDismissListener {
                                                                // back to the main activity
                                                                this@LoginActivity.finish()
                                                            }
                                                        }.create()
                                                        .show()
                                                    AccountData.saveData(this@LoginActivity, data)
                                                    telemetry(this@LoginActivity, "login")
                                                    return@runBlocking true
                                                } else {
                                                    AlertDialog
                                                        .Builder(this@LoginActivity)
                                                        .apply {
                                                            setTitle("登录失败")
                                                            setMessage("请检查用户名和密码")
                                                            setPositiveButton("OK") { _, _ ->
                                                            }
                                                        }.create()
                                                        .show()
                                                    return@runBlocking false
                                                }
                                            }
                                        }
                                    }
                                }
                                CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
                                webView.loadUrl("https://www.zhihu.com/signin")
                            },
                        )
                    } else {
                        when (currentNoticeStep) {
                            0 ->
                                LoginNoticeScreen(
                                    stepTag = "login_notice_step_1",
                                    message = "我清楚，本应用由开源社区开发和维护，不由知乎官方开发并运营，也不受到知乎官方的承认或支持，使用本应用的一切后果由我本人承担。我可以在 https://www.zhihu.com/app/ 下载官方应用。",
                                    secondaryButtonText = "下载官方App",
                                    onSecondaryAction = {
                                        luoTianYiUrlLauncher(
                                            this@LoginActivity,
                                            "https://www.zhihu.com/app/".toUri(),
                                        )
                                    },
                                    onConfirm = {
                                        currentNoticeStep = 1
                                    },
                                )
                            1 ->
                                LoginNoticeScreen(
                                    stepTag = "login_notice_step_2",
                                    message = "在使用本应用的过程中，我承诺遵守知乎使用协议 https://www.zhihu.com/term/zhihu-terms 。我保证在使用过程中不侵犯知乎及其他作者的著作权，使用本应用产生的一切输出仅用于个人浏览和备份，不会进行传播等其他影响作者著作权的行为。",
                                    secondaryButtonText = "查看协议",
                                    onSecondaryAction = {
                                        luoTianYiUrlLauncher(
                                            this@LoginActivity,
                                            "https://www.zhihu.com/term/zhihu-terms".toUri(),
                                        )
                                    },
                                    onConfirm = {
                                        currentNoticeStep = 2
                                    },
                                )
                            else ->
                                LoginNoticeScreen(
                                    stepTag = "login_notice_step_3",
                                    message = "我知晓，本应用可能会收集部分匿名化的使用信息来确定使用人数，我可以在设置中随时关闭此项遥测。",
                                    secondaryButtonText = "查看设置",
                                    onSecondaryAction = {
                                        startActivity(
                                            Intent(this@LoginActivity, MainActivity::class.java).apply {
                                            },
                                        )
                                    },
                                    onConfirm = {
                                        currentNoticeStep = 3
                                    },
                                )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginNoticeScreen(
    stepTag: String,
    message: String,
    secondaryButtonText: String,
    onConfirm: () -> Unit,
    onSecondaryAction: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(stepTag),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onSecondaryAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_notice_secondary_action"),
                ) {
                    Text(secondaryButtonText)
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_notice_confirm"),
                ) {
                    Text("确认")
                }
            }
        }
    }
}
