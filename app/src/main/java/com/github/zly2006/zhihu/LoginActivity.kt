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
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import com.github.zly2006.zhihu.util.LocaleManager
import com.github.zly2006.zhihu.util.enableEdgeToEdgeCompat
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import com.github.zly2006.zhihu.util.telemetry
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

private const val LOGIN_MODE_WEB = 0
private const val LOGIN_MODE_QR = 1

private const val HOME_URL = "https://www.zhihu.com/"
private const val SIGNIN_URL = "https://www.zhihu.com/signin?next=%2F"
private const val SIGNIN_REFERER_URL = "https://www.zhihu.com/signin"
private const val UDID_URL = "https://www.zhihu.com/udid"
private const val CAPTCHA_V2_URL = "https://www.zhihu.com/api/v3/oauth/captcha/v2?type=captcha_sign_in"
private const val QRCODE_URL = "https://www.zhihu.com/api/v3/account/api/login/qrcode"
private const val ME_URL = "https://www.zhihu.com/api/v4/me"
private const val RISK_CONTROL_URL = "https://www.zhihu.com/account/risk_control/"

private const val DESKTOP_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36"
private const val DESKTOP_SEC_CH_UA = "\"Not:A-Brand\";v=\"99\", \"Google Chrome\";v=\"145\", \"Chromium\";v=\"145\""
private const val DESKTOP_SEC_CH_UA_MOBILE = "?0"
private const val DESKTOP_SEC_CH_UA_PLATFORM = "\"Windows\""

class LoginActivity : ComponentActivity() {
    override fun attachBaseContext(base: android.content.Context?) {
        super.attachBaseContext(base?.let { LocaleManager.wrapContext(it) })
    }

    private var isCompletingLogin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdgeCompat()

        setContent {
            ZhihuTheme {
                var currentNoticeStep by rememberSaveable { mutableIntStateOf(0) }
                var loginMode by rememberSaveable { mutableIntStateOf(LOGIN_MODE_WEB) }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding(),
                ) {
                    if (currentNoticeStep >= 3) {
                        LoginModeScreen(
                            activity = this@LoginActivity,
                            loginMode = loginMode,
                            onModeChanged = { loginMode = it },
                        )
                    } else {
                        when (currentNoticeStep) {
                            0 ->
                                LoginNoticeScreen(
                                    stepTag = "login_notice_step_1",
                                    message = getString(R.string.login_notice_community),
                                    secondaryButtonText = getString(R.string.download_official_app),
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
                                    message = getString(R.string.login_notice_terms),
                                    secondaryButtonText = getString(R.string.view_terms),
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
                                    message = getString(R.string.login_notice_telemetry),
                                    secondaryButtonText = getString(R.string.view_settings),
                                    onSecondaryAction = {
                                        startActivity(
                                            Intent(this@LoginActivity, MainActivity::class.java),
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

    @SuppressLint("SetJavaScriptEnabled")
    fun configureWebLogin(webView: WebView) {
        webView.setupUpWebviewClient()
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest,
            ): Boolean {
                if (request.url.toString() == HOME_URL) {
                    webView.settings.userAgentString = AccountData.ANDROID_USER_AGENT
                }
                return request.url?.scheme == "zhihu"
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url == HOME_URL) {
                    val cookies = parseCookieAssignments(
                        CookieManager.getInstance().getCookie(HOME_URL).orEmpty(),
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
            webView.loadUrl(SIGNIN_URL)
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
        webView.settings.userAgentString = DESKTOP_USER_AGENT
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        cookies.forEach { (name, value) ->
            cookieManager.setCookie(HOME_URL, "$name=$value; Domain=.zhihu.com; Path=/")
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
                        setTitle(getString(R.string.login_success))
                        setMessage(getString(R.string.welcome_back, data.username))
                        setPositiveButton(getString(R.string.ok)) { _, _ -> }
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
                        setTitle(getString(R.string.login_failed))
                        setMessage(getString(R.string.please_retry))
                        setPositiveButton(getString(R.string.ok)) { _, _ -> }
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
private fun LoginModeScreen(
    activity: LoginActivity,
    loginMode: Int,
    onModeChanged: (Int) -> Unit,
) {
    val context = LocalContext.current
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
                text = context.getString(R.string.web_login),
                selected = loginMode == LOGIN_MODE_WEB,
                tag = "login_mode_web",
                modifier = Modifier.weight(1f),
                onClick = { onModeChanged(LOGIN_MODE_WEB) },
            )
            LoginModeButton(
                text = context.getString(R.string.qr_scan_login),
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
    var refreshKey by rememberSaveable { mutableIntStateOf(0) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var statusText by remember(context) { mutableStateOf(context.getString(R.string.qr_fetching)) }
    var sessionCookies by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var riskControlUrl by remember { mutableStateOf<String?>(null) }
    var riskControlMessage by remember { mutableStateOf<String?>(null) }
    var isWorking by remember { mutableStateOf(true) }

    LaunchedEffect(refreshKey) {
        val cookies = sessionCookies.toMutableMap()
        val client = AccountData.httpClient(context, cookies)
        qrBitmap = null
        statusText = context.getString(R.string.qr_fetching)
        isWorking = true

        try {
            prefetchQrLoginContext(client, cookies)
            val qrCode = requestQrCode(context, client, cookies)
            sessionCookies = cookies.toMap()
            val qrLink = qrCode.link ?: throw IllegalStateException(context.getString(R.string.qr_missing_link))
            val qrToken = qrCode.token ?: qrCode.qrcodeToken ?: throw IllegalStateException(context.getString(R.string.qr_missing_token))
            qrBitmap = generateQrBitmap(qrLink)
            statusText = context.getString(R.string.qr_open_zhihu_app_scan)

            val deadline = normalizeDeadline(qrCode.expiresAt)
            val success = pollQrCodeLogin(
                client = client,
                cookies = cookies,
                token = qrToken,
                deadline = deadline,
                onScanned = {
                    statusText = context.getString(R.string.qr_confirm_on_zhihu_app)
                },
                onRiskControl = { message, redirectUrl ->
                    sessionCookies = cookies.toMap()
                    riskControlMessage = message ?: context.getString(R.string.zhihu_risk_control_required)
                    riskControlUrl = redirectUrl ?: RISK_CONTROL_URL
                    statusText = riskControlMessage ?: context.getString(R.string.zhihu_risk_control_required)
                },
            )

            if (success) {
                statusText = context.getString(R.string.qr_verifying_login)
                isWorking = false
                activity.finalizeLoginFromCookies(cookies)
            } else if (!riskControlUrl.isNullOrBlank()) {
                isWorking = false
            } else {
                statusText = context.getString(R.string.qr_expired_retry)
                isWorking = false
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            statusText = e.message ?: context.getString(R.string.qr_fetch_failed_retry)
            isWorking = false
        } finally {
            client.close()
        }
    }

    if (!riskControlUrl.isNullOrBlank()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("qr_risk_control_content"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = riskControlMessage ?: context.getString(R.string.zhihu_risk_control_required),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = {
                    sessionCookies = sessionCookies + readWebViewCookies(riskControlUrl)
                    riskControlUrl = null
                    riskControlMessage = null
                    refreshKey += 1
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("qr_risk_control_continue"),
            ) {
                Text(context.getString(R.string.qr_complete_risk_continue))
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                WebviewComp(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("qr_risk_control_webview"),
                    onLoad = { webView ->
                        activity.configureRiskControlWebView(
                            webView = webView,
                            url = riskControlUrl.orEmpty(),
                            cookies = sessionCookies,
                            onCookiesChanged = { updatedCookies ->
                                sessionCookies = sessionCookies + updatedCookies
                            },
                        )
                    },
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("qr_login_content"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (qrBitmap != null) {
            Image(
                bitmap = qrBitmap!!.asImageBitmap(),
                contentDescription = context.getString(R.string.zhihu_login_qr),
                modifier = Modifier
                    .size(260.dp)
                    .testTag("qr_login_image"),
            )
            Spacer(modifier = Modifier.size(16.dp))
        } else if (isWorking) {
            CircularProgressIndicator(
                modifier = Modifier.testTag("qr_login_loading"),
            )
            Spacer(modifier = Modifier.size(16.dp))
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("qr_login_status"),
        )

        if (!riskControlMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = riskControlMessage.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.size(20.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = {
                    riskControlUrl = null
                    riskControlMessage = null
                    refreshKey += 1
                },
                modifier = Modifier.testTag("qr_login_retry"),
            ) {
                Text(context.getString(R.string.refresh_qr_code))
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
                    Text(LocalContext.current.getString(R.string.confirm))
                }
            }
        }
    }
}

@Serializable
private data class ZhihuQrCodeResponse(
    val expiresAt: Long? = null,
    val link: String? = null,
    val token: String? = null,
    val qrcodeToken: String? = null,
)

@Serializable
private data class ZhihuQrScanError(
    val needLogin: Boolean? = null,
    val redirect: String? = null,
    val code: Int? = null,
    val message: String? = null,
)

@Serializable
private data class ZhihuQrScanInfo(
    val status: Int? = null,
    val cookie: String? = null,
    val cookies: String? = null,
    val zC0: String? = null,
    val userId: String? = null,
    val accessToken: String? = null,
    val success: Boolean? = null,
    val loggedIn: Boolean? = null,
    val loginStatus: String? = null,
    val error: ZhihuQrScanError? = null,
)

private suspend fun prefetchQrLoginContext(
    client: HttpClient,
    cookies: Map<String, String>,
) {
    client.get(SIGNIN_URL) {
        createDesktopHeaders(HOME_URL).forEach { (key, value) ->
            header(key, value)
        }
    }

    runCatching {
        client.post(UDID_URL) {
            createZhihuLoginHeaders(cookies, SIGNIN_REFERER_URL).forEach { (key, value) ->
                header(key, value)
            }
            setBody("{}")
        }
    }

    runCatching {
        client.get(CAPTCHA_V2_URL) {
            createZhihuLoginHeaders(cookies, SIGNIN_REFERER_URL).forEach { (key, value) ->
                header(key, value)
            }
        }
    }
}

private suspend fun requestQrCode(
    context: android.content.Context,
    client: HttpClient,
    cookies: Map<String, String>,
): ZhihuQrCodeResponse {
    val response = client.post(QRCODE_URL) {
        createZhihuLoginHeaders(cookies, SIGNIN_REFERER_URL).forEach { (key, value) ->
            header(key, value)
        }
        setBody("{}")
    }
    val body = response.bodyAsText()
    val result = AccountData.decodeJson<ZhihuQrCodeResponse>(
        AccountData.json.parseToJsonElement(body),
    )
    val token = result.token ?: result.qrcodeToken
    if (response.status.value >= 400 || token.isNullOrBlank() || result.link.isNullOrBlank()) {
        throw IllegalStateException(context.getString(R.string.qr_fetch_failed_status, response.status.value))
    }
    return result.copy(token = token)
}

private suspend fun pollQrCodeLogin(
    client: HttpClient,
    cookies: MutableMap<String, String>,
    token: String,
    deadline: Long,
    onScanned: () -> Unit,
    onRiskControl: (String?, String?) -> Unit,
): Boolean {
    var hasPromptedConfirm = false
    var hasPromptedRiskControl = false

    while (System.currentTimeMillis() <= deadline) {
        currentCoroutineContext().ensureActive()

        try {
            val response = client.get("$QRCODE_URL/$token/scan_info") {
                createZhihuLoginHeaders(cookies, SIGNIN_URL, isPolling = true).forEach { (key, value) ->
                    header(key, value)
                }
            }
            if (response.status == HttpStatusCode.Forbidden) {
                onRiskControl(
                    null,
                    RISK_CONTROL_URL,
                )
                return false
            }
            val body = response.bodyAsText()
            val scanInfo = runCatching {
                AccountData.decodeJson<ZhihuQrScanInfo>(
                    AccountData.json.parseToJsonElement(body),
                )
            }.getOrDefault(ZhihuQrScanInfo())

            syncCookiesFromScanInfo(cookies, scanInfo)

            if (isRiskControlResponse(response.status.value, scanInfo)) {
                if (!hasPromptedRiskControl) {
                    hasPromptedRiskControl = true
                    onRiskControl(scanInfo.error?.message, scanInfo.error?.redirect)
                }
                return false
            }

            if (scanInfo.status == 1 && !hasPromptedConfirm) {
                hasPromptedConfirm = true
                onScanned()
            }

            val loginSucceeded = isQrLoginSuccessful(scanInfo)
            if (loginSucceeded || cookies.containsKey("z_c0")) {
                if (!cookies.containsKey("z_c0")) {
                    runCatching {
                        client.get(ME_URL) {
                            createZhihuLoginHeaders(cookies, SIGNIN_URL, isPolling = true).forEach { (key, value) ->
                                header(key, value)
                            }
                        }
                    }
                }
                if (loginSucceeded || cookies.containsKey("z_c0")) {
                    return true
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // 临时网络抖动时继续轮询
        }

        delay(500)
    }

    return false
}

private fun createDesktopHeaders(referer: String? = null): MutableMap<String, String> {
    val headers = mutableMapOf(
        "accept-encoding" to "gzip",
        "Accept" to "application/json, text/plain, */*",
        "Accept-Language" to "en-US,en;q=0.9",
        "User-Agent" to DESKTOP_USER_AGENT,
        "sec-ch-ua" to DESKTOP_SEC_CH_UA,
        "sec-ch-ua-mobile" to DESKTOP_SEC_CH_UA_MOBILE,
        "sec-ch-ua-platform" to DESKTOP_SEC_CH_UA_PLATFORM,
    )
    if (!referer.isNullOrBlank()) {
        headers["Referer"] = referer
    }
    return headers
}

private fun createZhihuLoginHeaders(
    cookies: Map<String, String>,
    referer: String,
    isPolling: Boolean = false,
): Map<String, String> {
    val headers = createDesktopHeaders(referer)
    headers["Origin"] = HOME_URL.removeSuffix("/")
    headers["x-requested-with"] = "fetch"
    headers["content-type"] = "application/json;charset=UTF-8"
    if (isPolling) {
        headers["Accept"] = "*/*"
        headers["sec-fetch-dest"] = "empty"
        headers["sec-fetch-mode"] = "cors"
        headers["sec-fetch-site"] = "same-origin"
        headers["x-zse-93"] = "101_3_3.0"
    }
    getXsrf(cookies)?.let {
        headers["x-xsrftoken"] = it
    }
    return headers
}

private fun syncCookiesFromScanInfo(
    cookies: MutableMap<String, String>,
    scanInfo: ZhihuQrScanInfo,
) {
    val rawCookie = listOf(scanInfo.cookie, scanInfo.cookies)
        .filterIsInstance<String>()
        .joinToString(";")
    cookies.putAll(parseCookieAssignments(rawCookie))
    scanInfo.zC0
        ?.takeIf { it.isNotBlank() }
        ?.let { cookies["z_c0"] = it }
}

private fun parseCookieAssignments(rawCookie: String): Map<String, String> {
    val skipCookieAttributes = setOf("Domain", "Path", "Expires", "Max-Age", "HttpOnly", "Secure", "SameSite")
    return rawCookie
        .split(";")
        .mapNotNull { item ->
            val trimmed = item.trim()
            if (!trimmed.contains("=")) {
                return@mapNotNull null
            }
            val name = trimmed.substringBefore("=").trim()
            if (name.isBlank() || skipCookieAttributes.contains(name)) {
                return@mapNotNull null
            }
            val value = trimmed.substringAfter("=").trim()
            if (value.isBlank()) {
                return@mapNotNull null
            }
            name to value
        }.toMap()
}

private fun isQrLoginSuccessful(scanInfo: ZhihuQrScanInfo): Boolean {
    if (scanInfo.userId != null || !scanInfo.accessToken.isNullOrBlank() || scanInfo.success == true || scanInfo.loggedIn == true) {
        return true
    }
    val loginStatus = scanInfo.loginStatus.orEmpty().uppercase()
    return loginStatus in setOf("CONFIRMED", "LOGIN_SUCCESS", "SUCCESS", "OK", "LOGGED_IN")
}

private fun isRiskControlResponse(
    statusCode: Int,
    scanInfo: ZhihuQrScanInfo,
): Boolean {
    val errorBody = scanInfo.error ?: return false
    return statusCode == 403 && (errorBody.code == 40352 || errorBody.needLogin == true)
}

private fun getXsrf(cookies: Map<String, String>): String? = cookies["_xsrf"]

private fun normalizeDeadline(expiresAt: Long?): Long {
    if (expiresAt == null || expiresAt <= 0) {
        return System.currentTimeMillis() + 120_000
    }
    return if (expiresAt < 10_000_000_000L) {
        expiresAt * 1000
    } else {
        expiresAt
    }
}

private fun generateQrBitmap(content: String): Bitmap {
    val size = 960
    val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
        }
    }
    return bitmap
}

private fun readWebViewCookies(url: String?): Map<String, String> {
    val cookieManager = CookieManager.getInstance()
    val cookies = mutableMapOf<String, String>()
    cookies.putAll(parseCookieAssignments(cookieManager.getCookie(HOME_URL).orEmpty()))
    if (!url.isNullOrBlank()) {
        cookies.putAll(parseCookieAssignments(cookieManager.getCookie(url).orEmpty()))
    }
    return cookies
}
