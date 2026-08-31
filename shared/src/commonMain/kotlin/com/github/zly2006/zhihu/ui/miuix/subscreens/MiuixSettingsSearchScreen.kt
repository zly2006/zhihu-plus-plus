/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.subscreens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.platform.rememberSettingBoolean
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import com.github.zly2006.zhihu.ui.subscreens.SETTINGS_SEARCH_INPUT_TAG
import com.github.zly2006.zhihu.ui.subscreens.SETTINGS_SEARCH_RESULTS_TAG
import com.github.zly2006.zhihu.ui.subscreens.settingsSearchEntries
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

/**
 * 设置搜索页的 miuix 版本，对标 M3 [com.github.zly2006.zhihu.ui.subscreens.SettingsSearchScreen]。
 *
 * 索引数据（`settingsSearchEntries`）与主题无关，两套外观共用同一份；这里只换渲染层。
 */
@Composable
fun MiuixSettingsSearchScreen() {
    val navigator = LocalNavigator.current
    val settings = rememberSettingsStore()
    val blurEnabled = rememberSettingBoolean("blurEnabled", true, settings)
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()

    var query by rememberSaveable { mutableStateOf("") }
    var developerModeEnabled by remember { mutableStateOf(settings.getBoolean("developer", false)) }
    DisposableEffect(settings) {
        val subscription = settings.observeKeyChanges { key ->
            if (key == "developer") {
                developerModeEnabled = settings.getBoolean("developer", false)
            }
        }
        onDispose(subscription::close)
    }
    val results = remember(query, developerModeEnabled) {
        settingsSearchEntries
            .filter { it.id != "developer.page" || developerModeEnabled }
            .filter { it.matches(query) }
    }
    val resultSections = remember(results) { results.groupBy { it.section }.toList() }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(backdrop),
                color = backdrop.getMiuixAppBarColor(),
                title = "搜索设置项",
                navigationIcon = {
                    IconButton(onClick = { navigator.onNavigateBack() }) {
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
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .testTag(SETTINGS_SEARCH_RESULTS_TAG),
            contentPadding = innerPadding,
        ) {
            item { Spacer(Modifier.size(12.dp)) }
            item {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    label = "搜索设置名称或关键词",
                    useLabelAsPlaceholder = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                        .testTag(SETTINGS_SEARCH_INPUT_TAG),
                )
            }

            if (results.isEmpty()) {
                item { SmallTitle(text = "没有找到相关设置") }
            } else {
                // miuix 的 ArrowPreference 只接受纯文本标题/摘要，放不下 M3 那行彩色分区标签，
                // 改成按分区分组，用 SmallTitle 承载分区名。
                resultSections.forEach { (section, entries) ->
                    item(key = "section.$section") { SmallTitle(text = section) }
                    item(key = "card.$section") {
                        Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                            entries.forEach { entry ->
                                ArrowPreference(
                                    modifier = Modifier.testTag("settingsSearch.result.${entry.id}"),
                                    title = entry.title,
                                    summary = entry.description,
                                    onClick = { navigator.onNavigate(entry.destination) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
