/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.subscreens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Memory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.shared.platform.rememberExternalUrlOpener
import com.github.zly2006.zhihu.shared.platform.rememberSettingBoolean
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import com.github.zly2006.zhihu.ui.subscreens.rememberOpenSourceLicensesLibraries
import com.github.zly2006.zhihu.ui.subscreens.rememberShowFullVariantLicenses
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

private data class ManualLicenseEntry(
    val name: String,
    val license: String,
    val summary: String,
    val url: String,
    val icon: @Composable () -> Unit,
)

private val fullVariantManualLibraries = listOf(
    ManualLicenseEntry(
        "Sentence-Embeddings-Android",
        "Apache-2.0",
        "用于执行句子嵌入。",
        "https://github.com/shubham0204/Sentence-Embeddings-Android",
        { Icon(Icons.Default.Memory, null) },
    ),
    ManualLicenseEntry(
        "huggingface/tokenizers 0.22.2",
        "Apache-2.0",
        "huggingface tokenizer",
        "https://github.com/huggingface/tokenizers",
        { Icon(Icons.Default.DataObject, null) },
    ),
    ManualLicenseEntry(
        "jni 0.21.1",
        "MIT/Apache-2.0",
        "Rust JNI 绑定。",
        "https://github.com/jni-rs/jni-rs",
        { Icon(Icons.Default.DataObject, null) },
    ),
    ManualLicenseEntry(
        "bytes 1.11.1",
        "MIT",
        "Rust bytes 缓冲区库。",
        "https://github.com/tokio-rs/bytes",
        { Icon(Icons.Default.DataObject, null) },
    ),
    ManualLicenseEntry(
        "serde 1.0.228",
        "MIT OR Apache-2.0",
        "Rust 序列化框架。",
        "https://github.com/serde-rs/serde",
        { Icon(Icons.Default.DataObject, null) },
    ),
    ManualLicenseEntry(
        "serde_json 1.0.149",
        "MIT OR Apache-2.0",
        "Rust JSON 序列化实现。",
        "https://github.com/serde-rs/json",
        { Icon(Icons.Default.DataObject, null) },
    ),
)

@Composable
fun MiuixOpenSourceLicensesScreen() {
    val navigator = LocalNavigator.current
    val openUrl = rememberExternalUrlOpener()
    val settings = rememberSettingsStore()
    val libraries = rememberOpenSourceLicensesLibraries()
    val manualLibraries = if (rememberShowFullVariantLicenses()) fullVariantManualLibraries else emptyList()
    val blurEnabled = rememberSettingBoolean("blurEnabled", true, settings)
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()
    val body2FontSize = MiuixTheme.textStyles.body2.fontSize
    val summaryColor = MiuixTheme.colorScheme.onSurfaceVariantActions

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(backdrop),
                color = backdrop.getMiuixAppBarColor(),
                title = "开源许可",
                navigationIcon = {
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(MiuixIconsEmbedded.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
            ),
        ) {
            item { Spacer(Modifier.height(12.dp)) }

            // Auto-generated libraries — each as Card{ArrowPreference} (no M3 LibrariesContainer)
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    libraries.libraries.forEach { lib ->
                        val licenseNames = lib.licenses.joinToString(", ") { it.name }
                        ArrowPreference(
                            title = lib.name,
                            summary = "${lib.artifactVersion}, $licenseNames",
                            onClick = { lib.website?.let { url -> openUrl(url) } },
                        )
                    }
                }
            }

            // Manual license entries (full variant only)
            if (manualLibraries.isNotEmpty()) {
                item { Spacer(Modifier.height(12.dp)) }
                item { SmallTitle(text = "Full 版本特有组件") }
                item {
                    Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                        manualLibraries.forEach { entry ->
                            ArrowPreference(
                                title = entry.name,
                                summary = "${entry.license} · ${entry.summary}",
                                startAction = entry.icon,
                                onClick = { openUrl(entry.url) },
                            )
                        }
                    }
                }
            }
        }
    }
}
