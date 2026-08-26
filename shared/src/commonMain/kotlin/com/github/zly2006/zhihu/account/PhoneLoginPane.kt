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

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
expect fun rememberLoginHttpClient(cookies: MutableMap<String, String>): HttpClient

expect fun decodePhoneLoginCaptchaImage(content: String): ImageBitmap?

@Composable
fun PhoneLoginPane(onLoginSuccess: (String) -> Unit) {
    val accountStore = rememberZhihuAccountStore()
    val loginClient = remember { ZhihuPhoneLoginClient() }
    DisposableEffect(loginClient) {
        onDispose(loginClient::close)
    }

    val scope = rememberCoroutineScope()
    var phoneNumber by remember { mutableStateOf("") }
    var digits by remember { mutableStateOf("") }
    var captchaInput by remember { mutableStateOf("") }
    var captchaImageBase64 by remember { mutableStateOf<String?>(null) }
    var captchaRequired by remember { mutableStateOf(false) }
    var agreementAccepted by remember { mutableStateOf(false) }
    var hasRequestedDigits by remember { mutableStateOf(false) }
    var isSendingDigits by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var resendSeconds by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(resendSeconds) {
        if (resendSeconds > 0) {
            delay(1_000)
            resendSeconds--
        }
    }

    val sendDigits: suspend () -> Unit = {
        errorMessage = null
        isSendingDigits = true
        try {
            when (val result = loginClient.requestDigits(phoneNumber)) {
                ZhihuPhoneDigitsResult.Sent -> {
                    hasRequestedDigits = true
                    captchaRequired = false
                    captchaImageBase64 = null
                    captchaInput = ""
                    resendSeconds = 60
                }

                is ZhihuPhoneDigitsResult.CaptchaRequired -> {
                    captchaRequired = true
                    captchaImageBase64 = result.imageBase64
                    if (result.imageBase64 == null) {
                        errorMessage = "服务器未返回图形验证码，请换一张重试"
                    }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            errorMessage = error.message ?: "发送验证码失败"
        } finally {
            isSendingDigits = false
        }
    }
    val verifyCaptchaAndSend: suspend () -> Unit = {
        isSendingDigits = true
        errorMessage = null
        try {
            if (loginClient.verifyCaptcha(captchaInput)) {
                isSendingDigits = false
                sendDigits()
            } else {
                errorMessage = "图形验证码不正确"
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            errorMessage = error.message ?: "验证图形验证码失败"
        } finally {
            isSendingDigits = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "手机号登录",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "使用知乎官方 Android 登录协议。是否需要图形验证码由知乎风控实时决定。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { value ->
                val nextPhoneNumber = value.filter(Char::isDigit).take(11)
                if (phoneNumber != nextPhoneNumber) {
                    phoneNumber = nextPhoneNumber
                    digits = ""
                    hasRequestedDigits = false
                    captchaRequired = false
                    captchaImageBase64 = null
                    captchaInput = ""
                }
            },
            label = { Text("手机号") },
            leadingIcon = { Text("+86") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("phone_login_phone"),
        )

        if (captchaRequired) {
            val captchaBitmap = remember(captchaImageBase64) {
                captchaImageBase64?.let(::decodePhoneLoginCaptchaImage)
            }
            captchaBitmap?.let { image ->
                Image(
                    bitmap = image,
                    contentDescription = "图形验证码",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .testTag("phone_login_captcha_image"),
                )
            }
            OutlinedTextField(
                value = captchaInput,
                onValueChange = { captchaInput = it },
                label = { Text("图形验证码") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (captchaInput.isNotBlank() && !isSendingDigits) {
                            scope.launch { verifyCaptchaAndSend() }
                        }
                    },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phone_login_captcha_input"),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isSendingDigits = true
                            errorMessage = null
                            try {
                                captchaImageBase64 = loginClient.refreshCaptcha()
                                if (captchaImageBase64 == null) {
                                    errorMessage = "服务器未返回图形验证码，请稍后重试"
                                }
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                errorMessage = error.message ?: "刷新图形验证码失败"
                            } finally {
                                isSendingDigits = false
                            }
                        }
                    },
                    enabled = !isSendingDigits,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("换一张")
                }
                Button(
                    onClick = {
                        scope.launch { verifyCaptchaAndSend() }
                    },
                    enabled = captchaInput.isNotBlank() && !isSendingDigits,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("phone_login_verify_captcha"),
                ) {
                    Text("验证并发送")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = digits,
                onValueChange = { value ->
                    digits = value.filter(Char::isDigit).take(6)
                },
                label = { Text("短信验证码") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("phone_login_digits"),
            )
            Button(
                onClick = { scope.launch { sendDigits() } },
                enabled = agreementAccepted &&
                    phoneNumber.length == 11 &&
                    resendSeconds == 0 &&
                    !isSendingDigits &&
                    !isLoggingIn,
                modifier = Modifier.testTag("phone_login_send_digits"),
            ) {
                if (isSendingDigits) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        if (resendSeconds > 0) {
                            "${resendSeconds}s"
                        } else {
                            "发送验证码"
                        },
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { agreementAccepted = !agreementAccepted }
                .testTag("phone_login_agreement"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = agreementAccepted,
                onCheckedChange = { agreementAccepted = it },
            )
            Text(
                text = "我已阅读并同意《知乎协议》《个人信息保护指引》",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
        }

        Button(
            onClick = {
                scope.launch {
                    isLoggingIn = true
                    errorMessage = null
                    try {
                        val token = loginClient.signIn(phoneNumber, digits)
                        if (accountStore.login(token)) {
                            onLoginSuccess(accountStore.session.username)
                        } else {
                            errorMessage = "登录凭证验证失败，请重试"
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        errorMessage = error.message ?: "登录失败"
                    } finally {
                        isLoggingIn = false
                    }
                }
            },
            enabled = agreementAccepted &&
                hasRequestedDigits &&
                phoneNumber.length == 11 &&
                digits.length == 6 &&
                !isSendingDigits &&
                !isLoggingIn,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("phone_login_submit"),
        ) {
            if (isLoggingIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text("登录")
            }
        }

        errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("phone_login_error"),
            )
        }
    }
}
