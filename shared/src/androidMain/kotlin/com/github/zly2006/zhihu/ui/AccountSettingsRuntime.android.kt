package com.github.zly2006.zhihu.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.navigation.TopLevelDestination
import com.github.zly2006.zhihu.shared.data.ZHIHU_ME_URL
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.util.clipboardManager
import kotlinx.coroutines.flow.MutableStateFlow

private const val LOGIN_ACTIVITY_CLASS = "com.github.zly2006.zhihu.LoginActivity"
private const val QR_CODE_SCAN_ACTIVITY_CLASS = "com.github.zly2006.zhihu.QRCodeScanActivity"
private const val WEBVIEW_ACTIVITY_CLASS = "com.github.zly2006.zhihu.WebviewActivity"
private const val QR_SCAN_RESULT_EXTRA = "scan_result"

@Composable
actual fun rememberAccountSettingsPlatformRuntime(): AccountSettingsRuntime {
    val context = LocalContext.current
    val accountDataState = AccountData.asState()
    val scanActivityLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) scan@{ result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scanResult = result.data?.getStringExtra(QR_SCAN_RESULT_EXTRA) ?: return@scan
            context.startActivity(context.webviewActivityIntent(scanResult))
        }
    }
    val accountState = remember(accountDataState.value) {
        androidx.compose.runtime.derivedStateOf {
            accountDataState.value.toAccountSettingsAccountState()
        }
    }
    val settings = rememberSettingsStore()
    val userMessages = rememberUserMessageSink()
    return AccountSettingsRuntime(
        accountState = accountState,
        settings = settings,
        userMessages = userMessages,
        refreshProfile = {
            val data = AccountData.data
            if (data.login) {
                val response = AccountData.signedFetchGet(context, ZHIHU_ME_URL)!!
                val self = ZhihuJson.decodeJson<com.github.zly2006.zhihu.shared.data.Person>(response)
                AccountData.saveData(context, data.copy(self = self))
            }
        },
        requestLogin = { context.startActivity(context.loginActivityIntent()) },
        requestQrLoginScan = { scanActivityLauncher.launch(context.qrCodeScanActivityIntent()) },
        logout = { AccountData.delete(context) },
        appVersionInfo = { context.zhihuVersionInfo() },
        copyText = { label, text ->
            val clip = android.content.ClipData.newPlainText(label, text)
            context.clipboardManager.setPrimaryClip(clip)
        },
        openExternalUrl = { url ->
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        },
        selectMainTab = { destination -> context.navigateMainTab(destination) },
        updateState = MutableStateFlow(com.github.zly2006.zhihu.ui.subscreens.SystemUpdateState.NoUpdate),
    )
}

fun AccountData.Data.toAccountSettingsAccountState(): AccountSettingsAccountState = AccountSettingsAccountState(
    login = login,
    username = username,
    avatarUrl = self?.avatarUrl,
    id = self?.id ?: "",
    urlToken = self?.urlToken,
)

private fun Context.zhihuVersionInfo(): String {
    val versionName = runCatching {
        packageManager.getPackageInfo(packageName, 0).versionName
    }.getOrNull() ?: "unknown"
    val appInfo = runCatching {
        packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
    }.getOrNull()
    val metaData = appInfo?.metaData
    val buildType = metaData?.getString("com.github.zly2006.zhihu.BUILD_TYPE")
        ?: if ((applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0) "debug" else "release"
    val gitHash = metaData?.getString("com.github.zly2006.zhihu.GIT_HASH") ?: "unknown"
    return "$versionName $buildType, $gitHash"
}

private fun Context.loginActivityIntent(): Intent = Intent().setClassName(packageName, LOGIN_ACTIVITY_CLASS)

private fun Context.qrCodeScanActivityIntent(): Intent = Intent().setClassName(packageName, QR_CODE_SCAN_ACTIVITY_CLASS)

private fun Context.webviewActivityIntent(url: String): Intent = Intent().apply {
    setClassName(packageName, WEBVIEW_ACTIVITY_CLASS)
    data = url.toUri()
}

private fun Context.navigateMainTab(destination: TopLevelDestination) {
    val activity = findActivity() ?: return
    activity
        .javaClass
        .methods
        .firstOrNull { method ->
            method.name == "navigateMainTab" &&
                method.parameterTypes.size == 1 &&
                method.parameterTypes.first().isAssignableFrom(destination::class.java)
        }?.invoke(activity, destination)
}

private fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
