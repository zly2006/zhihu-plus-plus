/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.data.Collection
import com.github.zly2006.zhihu.navigation.CollectionContent
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.platform.rememberSettingBoolean
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.miuix.components.MiuixConfirmDialog
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import com.github.zly2006.zhihu.ui.miuix.components.MiuixListLoadingIndicator
import com.github.zly2006.zhihu.ui.miuix.components.MiuixSheetInsideMargin
import com.github.zly2006.zhihu.ui.miuix.components.miuixSheetBottomInsets
import com.github.zly2006.zhihu.ui.miuix.components.miuixSheetCornerRadius
import com.github.zly2006.zhihu.viewmodel.CollectionsViewModel
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.window.WindowBottomSheet

@Composable
fun MiuixCollectionScreen(
    urlToken: String?,
    testCollections: List<Collection>? = null,
    showBackButton: Boolean = true,
    innerPadding: PaddingValues = PaddingValues(0.dp),
) {
    val navigator = LocalNavigator.current
    val environment = rememberPaginationEnvironment(allowGuestAccess = false)
    val viewModel = viewModel(key = urlToken) { CollectionsViewModel(urlToken.orEmpty()) }
    val listState = rememberLazyListState()
    val userMessages = rememberUserMessageSink()
    val coroutineScope = rememberCoroutineScope()
    var showCreateSheet by remember { mutableStateOf(false) }
    var newCollectionTitle by remember { mutableStateOf("") }
    var newCollectionPublic by remember { mutableStateOf(false) }
    var collectionPendingDeletion by remember { mutableStateOf<Collection?>(null) }
    val useTestCollections = testCollections != null || urlToken == null
    val collections = testCollections ?: viewModel.allData
    val settings = rememberSettingsStore()
    val blurEnabled = rememberSettingBoolean("blurEnabled", true, settings)
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()

    LaunchedEffect(useTestCollections) {
        if (!useTestCollections && viewModel.allData.isEmpty()) {
            viewModel.refresh(environment)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(backdrop),
                color = backdrop.getMiuixAppBarColor(),
                title = "我的收藏夹",
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = { navigator.onNavigateBack() }) {
                            Icon(MiuixIconsEmbedded.Back, "返回", tint = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onBackground)
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (!viewModel.isCreatingCollection && viewModel.deletingCollectionId == null) {
                                viewModel.clearMutationErrors()
                                showCreateSheet = true
                            }
                        },
                    ) {
                        Icon(Icons.Default.Add, "新建收藏夹", tint = MiuixTheme.colorScheme.onBackground)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + innerPadding.calculateBottomPadding(),
                ),
            ) {
                item { Spacer(Modifier.height(12.dp)) }
                items(collections, key = { it.id }) { collection ->
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                        ArrowPreference(
                            title = collection.title,
                            onClick = { navigator.onNavigate(CollectionContent(collection.id)) },
                            endActions = {
                                // 默认收藏夹（“我的收藏”）不能删，与 M3 CollectionScreen 一致。
                                if (!collection.isDefault) {
                                    IconButton(
                                        onClick = {
                                            viewModel.clearMutationErrors()
                                            collectionPendingDeletion = collection
                                        },
                                        enabled = !viewModel.isCreatingCollection && viewModel.deletingCollectionId == null,
                                    ) {
                                        Icon(Icons.Default.Delete, "删除${collection.title}", tint = MiuixTheme.colorScheme.primary)
                                    }
                                }
                            },
                        )
                    }
                }
                if (!useTestCollections && !viewModel.isEnd) {
                    item {
                        LaunchedEffect(Unit) { viewModel.loadMore(environment) }
                    }
                }
            }
            MiuixListLoadingIndicator(
                isLoading = viewModel.isLoading,
                isEmpty = collections.isEmpty(),
            )
        }
    }

    WindowBottomSheet(
        cornerRadius = miuixSheetCornerRadius(),
        show = showCreateSheet,
        title = "新建收藏夹",
        insideMargin = MiuixSheetInsideMargin,
        onDismissRequest = { showCreateSheet = false },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().miuixSheetBottomInsets(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextField(
                newCollectionTitle,
                { newCollectionTitle = it },
                modifier = Modifier.fillMaxWidth(),
                label = "收藏夹名称",
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("公开", color = MiuixTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Switch(checked = newCollectionPublic, onCheckedChange = { newCollectionPublic = it })
            }
            viewModel.createCollectionError?.let {
                Text(it, color = MiuixTheme.colorScheme.primary)
            }
            TextButton(
                text = if (viewModel.isCreatingCollection) "创建中…" else "创建",
                enabled = !viewModel.isCreatingCollection && newCollectionTitle.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val title = newCollectionTitle.trim()
                    if (title.isEmpty()) return@TextButton
                    coroutineScope.launch {
                        if (viewModel.createCollection(environment, title, "", newCollectionPublic)) {
                            viewModel.refresh(environment)
                            newCollectionTitle = ""
                            newCollectionPublic = false
                            showCreateSheet = false
                            userMessages.showShortMessage("收藏夹已创建")
                        }
                    }
                },
            )
        }
    }

    collectionPendingDeletion?.let { collection ->
        val isDeleting = viewModel.deletingCollectionId != null
        MiuixConfirmDialog(
            show = true,
            title = "删除收藏夹",
            summary = viewModel.deleteCollectionError
                ?: "删除后无法恢复，确认删除收藏夹「${collection.title}」吗？",
            confirmText = if (isDeleting) "删除中…" else "删除",
            onConfirm = {
                coroutineScope.launch {
                    if (viewModel.deleteCollection(environment, collection)) {
                        viewModel.refresh(environment)
                        collectionPendingDeletion = null
                        userMessages.showShortMessage("收藏夹已删除")
                    }
                }
            },
            onDismiss = { if (!isDeleting) collectionPendingDeletion = null },
        )
    }
}
