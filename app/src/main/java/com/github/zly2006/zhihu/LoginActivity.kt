/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.shared.login.SharedQrLoginPane
import com.github.zly2006.zhihu.shared.login.ZHIHU_DESKTOP_USER_AGENT
import com.github.zly2006.zhihu.shared.login.ZHIHU_HOME_URL
import com.github.zly2006.zhihu.shared.login.ZHIHU_SIGNIN_URL
import com.github.zly2006.zhihu.shared.login.parseCookieAssignments
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.ThemeStyle
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.components.WebviewComp
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import com.github.zly2006.zhihu.util.enableEdgeToEdgeCompat
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import com.github.zly2006.zhihu.util.telemetry
import kotlinx.coroutines.launch

private const val LOGIN_MODE_WEB = 0
private const val LOGIN_MODE_QR = 1

class LoginActivity : ComponentActivity() {
    private var isCompletingLogin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeCompat()

        setContent {
            ZhihuTheme {
                var currentNoticeStep by rememberSaveable { mutableIntStateOf(0) }
                var loginMode by rememberSaveable { mutableIntStateOf(LOGIN_MODE_WEB) }

                val useMiuix = ThemeManager.getThemeStyle() == ThemeStyle.Miuix
                if (currentNoticeStep >= 3) {
                    if (useMiuix) {
                        MiuixLoginModeScreen(
                            activity = this@LoginActivity,
                            loginMode = loginMode,
                            onModeChanged = { loginMode = it },
                        )
                    } else {
                        Surface(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                            LoginModeScreen(
                                activity = this@LoginActivity,
                                loginMode = loginMode,
                                onModeChanged = { loginMode = it },
                            )
                        }
                    }
                } else if (useMiuix) {
                    when (currentNoticeStep) {
                        0 -> MiuixLoginNotice(
                            "我清楚，本应用由开源社区开发和维护，不由知乎官方开发并运营，也不受到知乎官方的承认或支持，使用本应用的一切后果由我本人承担。我可以在 https://www.zhihu.com/app/ 下载官方应用。",
                            step = "1/3", onConfirm = { currentNoticeStep = 1 })
                        1 -> MiuixLoginNotice(
                            "在使用本应用的过程中，我承诺遵守知乎使用协议 https://www.zhihu.com/term/zhihu-terms 。我保证在使用过程中不侵犯知乎及其他作者的著作权，使用本应用产生的一切输出仅用于个人浏览和备份，不会进行传播等其他影响作者著作权的行为。",
                            step = "2/3", onConfirm = { currentNoticeStep = 2 })
                        else -> MiuixLoginNotice(
                            "我知晓，本应用可能会收集部分匿名化的使用信息来确定使用人数，我可以在设置中随时关闭此项遥测。",
                            step = "3/3", onConfirm = { currentNoticeStep = 3 })
                    }
                } else {
                    Surface(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
                        when (currentNoticeStep) {
                            0 -> LoginNoticeScreen(stepTag = "login_notice_step_1",
                                message = "我清楚，本应用由开源社区开发和维护，不由知乎官方开发并运营，也不受到知乎官方的承认或支持，使用本应用的一切后果由我本人承担。我可以在 https://www.zhihu.com/app/ 下载官方应用。",
                                secondaryButtonText = "下载官方App",
                                onSecondaryAction = { luoTianYiUrlLauncher(this@LoginActivity, "https://www.zhihu.com/app/".toUri()) },
                                onConfirm = { currentNoticeStep = 1 })
                            1 -> LoginNoticeScreen(stepTag = "login_notice_step_2",
                                message = "在使用本应用的过程中，我承诺遵守知乎使用协议 https://www.zhihu.com/term/zhihu-terms 。我保证在使用过程中不侵犯知乎及其他作者的著作权，使用本应用产生的一切输出仅用于个人浏览和备份，不会进行传播等其他影响作者著作权的行为。",
                                secondaryButtonText = "查看协议",
                                onSecondaryAction = { luoTianYiUrlLauncher(this@LoginActivity, "https://www.zhihu.com/term/zhihu-terms".toUri()) },
                                onConfirm = { currentNoticeStep = 2 })
                            else -> LoginNoticeScreen(stepTag = "login_notice_step_3",
                                message = "我知晓，本应用可能会收集部分匿名化的使用信息来确定使用人数，我可以在设置中随时关闭此项遥测。",
                                secondaryButtonText = "查看设置",
                                onSecondaryAction = { startActivity(Intent(this@LoginActivity, MainActivity::class.java)) },
                                onConfirm = { currentNoticeStep = 3 })
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun configureWebLogin(webView: WebView) {
        webView.setupUpWebviewClient()
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest,
            ): Boolean {
                if (request.url.toString() == ZHIHU_HOME_URL) {
                    webView.settings.userAgentString = AccountData.ANDROID_USER_AGENT
                }
                return request.url?.scheme == "zhihu"
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url == ZHIHU_HOME_URL) {
                    val cookies = parseCookieAssignments(
                        CookieManager.getInstance().getCookie(ZHIHU_HOME_URL).orEmpty(),
                    )
                    lifecycleScope.launch {
                        finalizeLoginFromCookies(cookies)
                    }
                }
            }
        }
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        if (webView.url.isNullOrEmpty()) {
            CookieManager.getInstance().removeAllCookies { }
            webView.loadUrl(ZHIHU_SIGNIN_URL)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun configureRiskControlWebView(
        webView: WebView,
        url: String,
        cookies: Map<String, String>,
        onCookiesChanged: (Map<String, String>) -> Unit,
    ) {
        webView.setupUpWebviewClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.userAgentString = ZHIHU_DESKTOP_USER_AGENT
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        cookies.forEach { (name, value) ->
            cookieManager.setCookie(ZHIHU_HOME_URL, "$name=$value; Domain=.zhihu.com; Path=/")
        }
        cookieManager.flush()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest,
            ): Boolean = request.url?.scheme == "zhihu"

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                onCookiesChanged(
                    readWebViewCookies(url),
                )
            }
        }
        if (webView.url != url) {
            webView.loadUrl(url)
        }
    }

    suspend fun finalizeLoginFromCookies(cookies: Map<String, String>): Boolean {
        if (isCompletingLogin) {
            return false
        }
        isCompletingLogin = true
        return try {
            if (AccountData.verifyLogin(this, cookies)) {
                val data = AccountData.loadData(this)
                AlertDialog
                    .Builder(this)
                    .apply {
                        setTitle("登录成功")
                        setMessage("欢迎回来，${data.username}")
                        setPositiveButton("OK") { _, _ -> }
                        setOnDismissListener {
                            finish()
                        }
                    }.create()
                    .show()
                telemetry(this, "login")
                true
            } else {
                AlertDialog
                    .Builder(this)
                    .apply {
                        setTitle("登录失败")
                        setMessage("请重试")
                        setPositiveButton("OK") { _, _ -> }
                    }.create()
                    .show()
                false
            }
        } finally {
            isCompletingLogin = false
        }
    }
}

@Composable
private fun MiuixLoginModeScreen(
    activity: LoginActivity,
    loginMode: Int,
    onModeChanged: (Int) -> Unit,
) {
    MiuixScaffold(
        topBar = {
            MiuixTopAppBar(
                title = "登录知乎",
                navigationIcon = {
                    MiuixIconButton(onClick = { activity.finish() }) {
                        MiuixIcon(MiuixIconsEmbedded.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                tabs = listOf("网页登录", "扫码登录"),
                selectedTabIndex = loginMode,
                onTabSelected = onModeChanged,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            )
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp)) {
                if (loginMode == LOGIN_MODE_WEB) {
                    WebviewComp(
                        modifier = Modifier.fillMaxSize(),
                        onLoad = { webView -> activity.configureWebLogin(webView) },
                    )
                } else {
                    QrLoginPane(activity = activity)
                }
            }
        }
    }
}

@Composable
private fun LoginModeScreen(
    activity: LoginActivity,
    loginMode: Int,
    onModeChanged: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LoginModeButton(
                text = "网页登录",
                selected = loginMode == LOGIN_MODE_WEB,
                tag = "login_mode_web",
                modifier = Modifier.weight(1f),
                onClick = { onModeChanged(LOGIN_MODE_WEB) },
            )
            LoginModeButton(
                text = "扫码登录",
                selected = loginMode == LOGIN_MODE_QR,
                tag = "login_mode_qr",
                modifier = Modifier.weight(1f),
                onClick = { onModeChanged(LOGIN_MODE_QR) },
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (loginMode == LOGIN_MODE_WEB) {
                WebviewComp(
                    modifier = Modifier.fillMaxSize(),
                    onLoad = { webView ->
                        activity.configureWebLogin(webView)
                    },
                )
            } else {
                QrLoginPane(activity = activity)
            }
        }
    }
}

@Composable
private fun LoginModeButton(
    text: String,
    selected: Boolean,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.testTag(tag),
        ) {
            Text(text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.testTag(tag),
        ) {
            Text(text)
        }
    }
}

@Composable
private fun QrLoginPane(activity: LoginActivity) {
    val context = LocalContext.current
    SharedQrLoginPane(
        createClient = { cookies -> AccountData.httpClient(context, cookies) },
        onLoginSuccess = { cookies -> activity.finalizeLoginFromCookies(cookies) },
        readRiskControlCookies = ::readWebViewCookies,
        riskControlContent = { url, cookies, onCookiesChanged ->
            WebviewComp(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("qr_risk_control_webview"),
                onLoad = { webView ->
                    activity.configureRiskControlWebView(
                        webView = webView,
                        url = url,
                        cookies = cookies,
                        onCookiesChanged = { updatedCookies ->
                            onCookiesChanged(cookies + updatedCookies)
                        },
                    )
                },
            )
        },
    )
}

@Composable
private fun MiuixLoginNotice(
    message: String,
    step: String,
    onConfirm: () -> Unit,
) {
    MiuixScaffold(
        topBar = {
            MiuixTopAppBar(
                title = "登录须知",
                navigationIcon = {
                    MiuixIconButton(onClick = { onConfirm() }) {
                        MiuixIcon(MiuixIconsEmbedded.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MiuixText(
                        text = message,
                        style = MiuixTheme.textStyles.body1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    MiuixText(step, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, style = MiuixTheme.textStyles.body2)
                }
            }
            MiuixButton(onClick = onConfirm, modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                colors = MiuixButtonDefaults.buttonColorsPrimary()) {
                MiuixText("确认并继续")
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

private fun readWebViewCookies(url: String?): Map<String, String> {
    val cookieManager = CookieManager.getInstance()
    val cookies = mutableMapOf<String, String>()
    cookies.putAll(parseCookieAssignments(cookieManager.getCookie(ZHIHU_HOME_URL).orEmpty()))
    if (!url.isNullOrBlank()) {
        cookies.putAll(parseCookieAssignments(cookieManager.getCookie(url).orEmpty()))
    }
    return cookies
}
