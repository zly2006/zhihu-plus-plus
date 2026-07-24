/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.chloemlla.zhplus.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.chloemlla.zhplus.shared.platform.rememberSettingsStore

/**
 * 账号页等入口请求重开开源说明时调用。
 * [AppGateHost] 挂载时注册，卸载时清空。
 */
object OssNoticeReopenBus {
    @Volatile
    var request: (() -> Unit)? = null
}

/**
 * 主壳门控：开源声明 → 用户须知 → 本次更新说明 → 内容。
 *
 * 首装 OSS / 引导 / whats-new 以全屏替换内容，避免底栏闪现；
 * 账号页 reopen 则叠在内容之上。
 */
@Composable
fun AppGateHost(content: @Composable () -> Unit) {
    val settings = rememberSettingsStore()
    val identity = rememberBuildIdentity()
    var reopenOss by remember { mutableStateOf(false) }
    var revision by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        val reopen: () -> Unit = { reopenOss = true }
        OssNoticeReopenBus.request = reopen
        onDispose {
            if (OssNoticeReopenBus.request === reopen) {
                OssNoticeReopenBus.request = null
            }
        }
    }

    val gate =
        remember(settings, identity, reopenOss, revision) {
            settings.resolveAppGate(identity = identity, reopenOss = reopenOss)
        }

    when {
        gate.kind == AppGateKind.OpenSourceNotice && !gate.reopenOss -> {
            OpenSourceNoticeScreen(
                onContinue = {
                    settings.markOssNoticeCompleted()
                    revision += 1
                },
            )
        }
        gate.kind == AppGateKind.ProductOnboarding -> {
            ProductOnboardingScreen(
                onComplete = {
                    settings.markOnboardingCompleted(identity)
                    revision += 1
                },
            )
        }
        gate.kind == AppGateKind.WhatsNew && gate.whatsNew != null -> {
            WhatsNewScreen(
                entry = gate.whatsNew,
                identity = identity,
                onContinue = {
                    settings.markWhatsNewSeen(identity)
                    revision += 1
                },
            )
        }
        else -> {
            Box(Modifier.fillMaxSize()) {
                content()
                if (gate.kind == AppGateKind.OpenSourceNotice && gate.reopenOss) {
                    OpenSourceNoticeScreen(
                        onContinue = {
                            reopenOss = false
                            revision += 1
                        },
                        onDismiss = {
                            reopenOss = false
                            revision += 1
                        },
                    )
                }
            }
        }
    }
}
