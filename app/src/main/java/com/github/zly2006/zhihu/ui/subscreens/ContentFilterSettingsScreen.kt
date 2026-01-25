package com.github.zly2006.zhihu.ui.subscreens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.RecommendationMode
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.components.SwitchSettingItem
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterManager
import com.github.zly2006.zhihu.viewmodel.filter.FilterStats
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentFilterSettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigate: (NavDestination) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val preferences = remember {
        context.getSharedPreferences(
            PREFERENCE_NAME,
            Context.MODE_PRIVATE,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("推荐系统与内容过滤") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                windowInsets = WindowInsets(0),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // Rec Mode
            val currentRecommendationMode = remember {
                mutableStateOf(
                    RecommendationMode.entries.find {
                        it.key == preferences.getString("recommendationMode", RecommendationMode.MIXED.key)
                    } ?: RecommendationMode.MIXED,
                )
            }
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                OutlinedTextField(
                    value = currentRecommendationMode.value.displayName,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("推荐算法") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    RecommendationMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(mode.displayName)
                                    Text(mode.description, style = MaterialTheme.typography.bodySmall)
                                }
                            },
                            onClick = {
                                currentRecommendationMode.value = mode
                                preferences.edit { putString("recommendationMode", mode.key) }
                                expanded = false
                            },
                        )
                    }
                }
            }

            val isLoginForRecommendation = remember {
                mutableStateOf(preferences.getBoolean("loginForRecommendation", true))
            }
            SwitchSettingItem(
                title = "推荐内容时登录",
                description = "获取推荐内容时携带登录凭证",
                checked = isLoginForRecommendation.value,
                onCheckedChange = { checked ->
                    isLoginForRecommendation.value = checked
                    preferences.edit { putBoolean("loginForRecommendation", checked) }
                },
            )

            val enableContentFilter = remember { mutableStateOf(preferences.getBoolean("enableContentFilter", true)) }
            SwitchSettingItem(
                title = "启用智能内容过滤",
                description = "自动过滤首页展示超过2次但用户未点击的内容，减少重复推荐",
                checked = enableContentFilter.value,
                onCheckedChange = {
                    enableContentFilter.value = it
                    preferences.edit { putBoolean("enableContentFilter", it) }
                },
            )

            val filterFollowedUserContent = remember { mutableStateOf(preferences.getBoolean("filterFollowedUserContent", false)) }
            SwitchSettingItem(
                title = "过滤已关注用户内容",
                description = "是否对已关注用户的内容也应用过滤规则。关闭此选项可确保关注用户的内容始终显示",
                checked = filterFollowedUserContent.value,
                onCheckedChange = {
                    filterFollowedUserContent.value = it
                    preferences.edit { putBoolean("filterFollowedUserContent", it) }
                },
                enabled = enableContentFilter.value,
            )

            val enableKeywordBlocking = remember { mutableStateOf(preferences.getBoolean("enableKeywordBlocking", true)) }
            SwitchSettingItem(
                title = "启用关键词屏蔽",
                description = "屏蔽包含特定关键词的内容",
                checked = enableKeywordBlocking.value,
                onCheckedChange = {
                    enableKeywordBlocking.value = it
                    preferences.edit { putBoolean("enableKeywordBlocking", it) }
                },
            )

            val enableUserBlocking = remember { mutableStateOf(preferences.getBoolean("enableUserBlocking", true)) }
            SwitchSettingItem(
                title = "启用用户屏蔽",
                description = "屏蔽特定用户发布的内容",
                checked = enableUserBlocking.value,
                onCheckedChange = {
                    enableUserBlocking.value = it
                    preferences.edit { putBoolean("enableUserBlocking", it) }
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate(com.github.zly2006.zhihu.Blocklist) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("管理屏蔽列表", style = MaterialTheme.typography.bodyLarge)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Filter Stats (Simplified)
            var filterStats by remember { mutableStateOf<FilterStats?>(null) }
            var showStatsDialog by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                try {
                    val contentFilterManager = ContentFilterManager.getInstance(context)
                    filterStats = contentFilterManager.getFilterStats()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            AnimatedVisibility(visible = enableContentFilter.value && filterStats != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStatsDialog = true }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("过滤统计", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "已累计过滤 ${filterStats?.filteredCount ?: 0} 条内容，点击查看详情",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (showStatsDialog && filterStats != null) {
                AlertDialog(
                    onDismissRequest = { showStatsDialog = false },
                    title = { Text("过滤统计详情") },
                    text = {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("总记录数: ${filterStats?.totalRecords}")
                                Text("过滤率: %.1f%%".format((filterStats?.filterRate ?: 0f) * 100))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            val contentFilterManager = ContentFilterManager.getInstance(context)
                                            contentFilterManager.cleanupOldData()
                                            filterStats = contentFilterManager.getFilterStats()
                                            Toast.makeText(context, "已清理过期数据", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("清理过期数据")
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        try {
                                            val contentFilterManager = ContentFilterManager.getInstance(context)
                                            contentFilterManager.clearAllData()
                                            filterStats = contentFilterManager.getFilterStats()
                                            Toast.makeText(context, "已重置所有数据", Toast.LENGTH_SHORT).show()
                                            showStatsDialog = false
                                        } catch (e: Exception) {
                                            // ignore
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            ) {
                                Text("重置所有数据")
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showStatsDialog = false }) {
                            Text("关闭")
                        }
                    },
                )
            }
        }
    }
}
