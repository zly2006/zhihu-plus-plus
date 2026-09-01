/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

package com.github.zly2006.zhihu.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.platform.rememberExternalUrlOpener
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.ThemeStyle
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.Scaffold as MiuixScaffold
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton
import top.yukonga.miuix.kmp.basic.TopAppBar as MiuixTopAppBar

enum class LoginMethod(
    val label: String,
    val tag: String,
) {
    Phone("手机号登录", "login_mode_phone"),
    Qr("扫码登录", "login_mode_qr"),
    Web("备用网页登录", "login_mode_web"),
}

expect val supportedLoginMethods: List<LoginMethod>

@Composable
expect fun QrLoginPane(onLoginSuccess: (String) -> Unit)

@Composable
expect fun WebLoginPane(onLoginSuccess: (String) -> Unit)

@Composable
fun LoginScreen(
    onLoginComplete: () -> Unit,
    onOpenTelemetrySettings: () -> Unit,
) {
    val navigator = LocalNavigator.current
    val openExternalUrl = rememberExternalUrlOpener()
    val useMiuix = ThemeManager.getThemeStyle() == ThemeStyle.Miuix
    var noticeStep by rememberSaveable {
        mutableIntStateOf(0)
    }
    var selectedMethod by rememberSaveable(supportedLoginMethods) {
        mutableStateOf(supportedLoginMethods.first())
    }
    var loggedInUsername by remember { mutableStateOf<String?>(null) }
    val onLoginSuccess: (String) -> Unit = { username -> loggedInUsername = username }
    val onSecondaryNoticeAction: () -> Unit = {
        when (noticeStep) {
            0 -> openExternalUrl("https://www.zhihu.com/app/")
            1 -> openExternalUrl("https://www.zhihu.com/term/zhihu-terms")
            else -> onOpenTelemetrySettings()
        }
    }
    val methodPane: @Composable (LoginMethod) -> Unit = { method ->
        when (method) {
            LoginMethod.Phone -> PhoneLoginPane(onLoginSuccess)
            LoginMethod.Qr -> QrLoginPane(onLoginSuccess)
            LoginMethod.Web -> WebLoginPane(onLoginSuccess)
        }
    }

    if (noticeStep < LOGIN_NOTICE_COUNT) {
        val notice = loginNotices[noticeStep]
        val stepTag = "login_notice_step_${noticeStep + 1}"
        if (useMiuix) {
            MiuixLoginNoticeScreen(
                stepTag = stepTag,
                stepLabel = "${noticeStep + 1}/$LOGIN_NOTICE_COUNT",
                message = notice.message,
                secondaryButtonText = notice.secondaryButtonText,
                onSecondaryAction = onSecondaryNoticeAction,
                onConfirm = { noticeStep++ },
                // 第一条须知再往回退就是离开登录页，交回导航栈。
                onBack = { if (noticeStep > 0) noticeStep-- else navigator.onNavigateBack() },
            )
        } else {
            LoginNoticeScreen(
                stepTag = stepTag,
                message = notice.message,
                secondaryButtonText = notice.secondaryButtonText,
                onSecondaryAction = onSecondaryNoticeAction,
                onConfirm = { noticeStep++ },
            )
        }
    } else if (useMiuix) {
        MiuixLoginMethodScreen(
            selectedMethod = selectedMethod,
            onMethodSelected = { selectedMethod = it },
            onBack = navigator.onNavigateBack,
            pane = methodPane,
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                supportedLoginMethods.forEach { method ->
                    LoginMethodButton(
                        method = method,
                        selected = method == selectedMethod,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedMethod = method },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                methodPane(selectedMethod)
            }
        }
    }

    loggedInUsername?.let { username ->
        if (useMiuix) {
            WindowDialog(
                show = true,
                title = "登录成功",
                summary = "欢迎回来，$username",
                onDismissRequest = onLoginComplete,
            ) {
                MiuixButton(
                    onClick = onLoginComplete,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = MiuixButtonDefaults.buttonColorsPrimary(),
                ) {
                    MiuixText("确定")
                }
            }
        } else {
            AlertDialog(
                onDismissRequest = onLoginComplete,
                title = { Text("登录成功") },
                text = { Text("欢迎回来，$username") },
                confirmButton = {
                    TextButton(onClick = onLoginComplete) {
                        Text("确定")
                    }
                },
            )
        }
    }
}

/**
 * 登录方式选择页的 miuix 版本。
 *
 * 登录方式由平台的 [supportedLoginMethods] 决定，这里只把它投影成 TabRow；
 * 各方式的面板本身仍是共享实现，miuix 主题下由 `ZhihuMiuixTheme` 的 M3 兜底配色着色。
 */
@Composable
private fun MiuixLoginMethodScreen(
    selectedMethod: LoginMethod,
    onMethodSelected: (LoginMethod) -> Unit,
    onBack: () -> Unit,
    pane: @Composable (LoginMethod) -> Unit,
) {
    MiuixScaffold(
        topBar = {
            MiuixTopAppBar(
                title = "登录知乎",
                navigationIcon = {
                    MiuixIconButton(onClick = onBack) {
                        MiuixIcon(MiuixIconsEmbedded.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRowWithContour(
                tabs = supportedLoginMethods.map(LoginMethod::label),
                selectedTabIndex = supportedLoginMethods.indexOf(selectedMethod).coerceAtLeast(0),
                onTabSelected = { onMethodSelected(supportedLoginMethods[it]) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            )
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp)) {
                pane(selectedMethod)
            }
        }
    }
}

@Composable
private fun MiuixLoginNoticeScreen(
    stepTag: String,
    stepLabel: String,
    message: String,
    secondaryButtonText: String,
    onSecondaryAction: () -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    MiuixScaffold(
        topBar = {
            MiuixTopAppBar(
                title = "登录须知",
                navigationIcon = {
                    MiuixIconButton(onClick = onBack) {
                        MiuixIcon(MiuixIconsEmbedded.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .testTag(stepTag),
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
                    MiuixText(
                        text = stepLabel,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        style = MiuixTheme.textStyles.body2,
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MiuixTextButton(
                    text = secondaryButtonText,
                    onClick = onSecondaryAction,
                    modifier = Modifier.fillMaxWidth().testTag("login_notice_secondary_action"),
                )
                MiuixButton(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth().testTag("login_notice_confirm"),
                    colors = MiuixButtonDefaults.buttonColorsPrimary(),
                ) {
                    MiuixText("确认并继续")
                }
            }
        }
    }
}

@Composable
private fun LoginMethodButton(
    method: LoginMethod,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.testTag(method.tag),
        ) {
            Text(method.label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.testTag(method.tag),
        ) {
            Text(method.label)
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
            // 不透明背景兜底：NavDisplay 会保留被覆盖层，登录页不自己铺底就会透出下面的首页信息流。
            // miuix 分支由 MiuixScaffold 铺底，M3 分支是裸 Column，必须显式补上（同 ArticleAnswerSlot）。
            .background(MaterialTheme.colorScheme.background)
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

private data class LoginNotice(
    val message: String,
    val secondaryButtonText: String,
)

private const val LOGIN_NOTICE_COUNT = 3

private val loginNotices = listOf(
    LoginNotice(
        message = "我清楚，本应用由开源社区开发和维护，不由知乎官方开发并运营，也不受到知乎官方的承认或支持，使用本应用的一切后果由我本人承担。我可以在 https://www.zhihu.com/app/ 下载官方应用。",
        secondaryButtonText = "下载官方 App",
    ),
    LoginNotice(
        message = "在使用本应用的过程中，我承诺遵守知乎使用协议 https://www.zhihu.com/term/zhihu-terms 。我保证在使用过程中不侵犯知乎及其他作者的著作权，使用本应用产生的一切输出仅用于个人浏览和备份，不会进行传播等其他影响作者著作权的行为。",
        secondaryButtonText = "查看协议",
    ),
    LoginNotice(
        message = "我知晓，本应用可能会收集部分匿名化的使用信息来确定使用人数，我可以在设置中随时关闭此项遥测。",
        secondaryButtonText = "查看设置",
    ),
)
