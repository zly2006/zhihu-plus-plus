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

package com.github.zly2006.zhihu.ui.subscreens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.ui.components.SettingItem
import com.github.zly2006.zhihu.ui.components.SettingItemGroup
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer

private data class ManualLicenseEntry(
    val name: String,
    val license: String,
    @StringRes val summaryResId: Int,
    val url: String,
    val icon: @Composable () -> Unit,
)

private val fullVariantManualLibraries = listOf(
    ManualLicenseEntry(
        name = "Sentence-Embeddings-Android",
        license = "Apache-2.0",
        summaryResId = R.string.license_summary_sentence_embeddings_android,
        url = "https://github.com/shubham0204/Sentence-Embeddings-Android",
        icon = { Icon(Icons.Default.Memory, contentDescription = null) },
    ),
    ManualLicenseEntry(
        name = "huggingface/tokenizers 0.22.2",
        license = "Apache-2.0",
        summaryResId = R.string.license_summary_huggingface_tokenizers,
        url = "https://github.com/huggingface/tokenizers",
        icon = { Icon(Icons.Default.DataObject, contentDescription = null) },
    ),
    ManualLicenseEntry(
        name = "jni 0.21.1",
        license = "MIT/Apache-2.0",
        summaryResId = R.string.license_summary_jni,
        url = "https://github.com/jni-rs/jni-rs",
        icon = { Icon(Icons.Default.DataObject, contentDescription = null) },
    ),
    ManualLicenseEntry(
        name = "bytes 1.11.1",
        license = "MIT",
        summaryResId = R.string.license_summary_bytes,
        url = "https://github.com/tokio-rs/bytes",
        icon = { Icon(Icons.Default.DataObject, contentDescription = null) },
    ),
    ManualLicenseEntry(
        name = "serde 1.0.228",
        license = "MIT OR Apache-2.0",
        summaryResId = R.string.license_summary_serde,
        url = "https://github.com/serde-rs/serde",
        icon = { Icon(Icons.Default.DataObject, contentDescription = null) },
    ),
    ManualLicenseEntry(
        name = "serde_json 1.0.149",
        license = "MIT OR Apache-2.0",
        summaryResId = R.string.license_summary_serde_json,
        url = "https://github.com/serde-rs/json",
        icon = { Icon(Icons.Default.DataObject, contentDescription = null) },
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceLicensesScreen() {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val libraries by produceLibraries(R.raw.aboutlibraries)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val manualLibraries = if (BuildConfig.IS_LITE) emptyList() else fullVariantManualLibraries

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.open_source_licenses)) },
                navigationIcon = {
                    IconButton(
                        onClick = navigator.onNavigateBack,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )
        },
    ) { innerPadding ->
        LibrariesContainer(
            libraries = libraries,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            showDescription = false,
            header = {
                if (manualLibraries.isNotEmpty()) {
                    item {
                        SettingItemGroup(title = stringResource(R.string.full_variant_components)) {
                            manualLibraries.forEach { entry ->
                                SettingItem(
                                    title = { Text(entry.name) },
                                    description = { Text("${entry.license} · ${stringResource(entry.summaryResId)}") },
                                    icon = entry.icon,
                                    onClick = {
                                        luoTianYiUrlLauncher(context, entry.url.toUri())
                                    },
                                )
                            }
                        }
                    }
                }
            },
        )
    }
}
