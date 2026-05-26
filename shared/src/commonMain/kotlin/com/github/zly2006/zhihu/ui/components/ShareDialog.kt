package com.github.zly2006.zhihu.ui.components

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.shared.platform.SettingsStore

data class ShareDialogRuntime(
    val share: (NavDestination, String) -> Unit,
    val directShare: (NavDestination, String) -> Unit,
    val copyLink: (NavDestination, String) -> Unit,
)

@Composable
expect fun rememberShareDialogRuntime(): ShareDialogRuntime

@Composable
fun ShareDialog(
    content: NavDestination,
    shareText: String,
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
) {
    val navigator = LocalNavigator.current
    val runtime = rememberShareDialogRuntime()

    ShareDialogContent(
        showDialog = showDialog,
        onDismissRequest = onDismissRequest,
        onShareClick = {
            onDismissRequest()
            runtime.share(content, shareText)
        },
        onCopyClick = {
            onDismissRequest()
            runtime.copyLink(content, shareText)
        },
        onSettingsClick = {
            onDismissRequest()
            navigator.onNavigate(Account.AppearanceSettings(setting = "shareAction"))
        },
    )
}

fun handleShareAction(
    content: NavDestination,
    settings: SettingsStore,
    runtime: ShareDialogRuntime,
    onShowDialog: () -> Unit,
) {
    val shareText = getShareText(content) ?: return
    when (settings.getString("shareActionMode", "ask")) {
        "copy" -> runtime.copyLink(content, shareText)
        "share" -> runtime.directShare(content, shareText)
        else -> onShowDialog()
    }
}
