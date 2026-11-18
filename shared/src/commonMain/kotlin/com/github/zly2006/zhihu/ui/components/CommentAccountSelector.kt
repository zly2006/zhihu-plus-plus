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

package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.account.ZhihuIdentityAccount
import com.github.zly2006.zhihu.account.ZhihuIdentityAccountListResponse
import com.github.zly2006.zhihu.account.ZhihuSavedAccount
import com.github.zly2006.zhihu.account.applyIdentityHeaders
import com.github.zly2006.zhihu.account.createAccountHttpClient
import com.github.zly2006.zhihu.account.identitySuccessBody
import com.github.zly2006.zhihu.account.rememberZhihuAccountStore
import com.github.zly2006.zhihu.data.ZhihuJson
import io.ktor.client.request.get
import kotlinx.coroutines.CancellationException

internal data class CommentAccountSelection(
    val sourceAccountId: String,
    val identity: ZhihuIdentityAccount,
)

private data class CommentAccountOption(
    val source: ZhihuSavedAccount,
    val identity: ZhihuIdentityAccount,
)

/** 选择当前评论的发送身份，独立于应用当前登录身份与文字草稿。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CommentAccountSelector(
    selectedAccount: CommentAccountSelection?,
    enabled: Boolean,
    onSelected: (CommentAccountSelection?) -> Unit,
) {
    val accountStore = rememberZhihuAccountStore()
    val savedAccounts by accountStore.accountsState.collectAsState()
    val currentSession = savedAccounts.session
    if (!currentSession.login) return

    val selectedId = selectedAccount?.identity?.id ?: currentSession.profile?.id
    val selectedName = selectedAccount?.identity?.name ?: currentSession.profile?.name.orEmpty()
    val selectedAvatar = if (selectedAccount == null) currentSession.profile?.avatarUrl else selectedAccount.identity.avatarUrl
    var expanded by remember { mutableStateOf(false) }
    var remoteOptions by remember { mutableStateOf<List<CommentAccountOption>>(emptyList()) }
    var attemptedSources by remember { mutableStateOf<Set<String>>(emptySet()) }
    var failedSources by remember { mutableStateOf<Set<String>>(emptySet()) }
    var retryToken by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    val options = (
        remoteOptions.sortedBy {
            if (it.identity.id == it.source.session.profile
                    ?.id
            ) {
                0
            } else {
                1
            }
        } +
            savedAccounts.accounts.mapNotNull { account ->
                account.session.profile?.takeIf { it.name.isNotBlank() }?.let { profile ->
                    CommentAccountOption(
                        source = account,
                        identity = ZhihuIdentityAccount(
                            id = profile.id,
                            name = profile.name,
                            avatarUrl = profile.avatarUrl,
                        ),
                    )
                }
            }
    ).distinctBy { it.identity.id }

    LaunchedEffect(expanded, retryToken) {
        if (!expanded) return@LaunchedEffect
        loading = true
        try {
            for (source in savedAccounts.accounts) {
                if (source.id in attemptedSources) continue
                try {
                    accountStore.restorePendingCommentIdentities()
                    val session = accountStore.accounts.first { it.id == source.id }.session
                    val accounts = createAccountHttpClient(session.cookies.toMutableMap(), session.userAgent).use { client ->
                        val response = client.get("https://api.zhihu.com/people/account/list") {
                            applyIdentityHeaders(session)
                        }
                        ZhihuJson
                            .decodeJson<ZhihuIdentityAccountListResponse>(
                                ZhihuJson.json.parseToJsonElement(response.identitySuccessBody("加载评论账号")),
                            ).data
                    }
                    remoteOptions = remoteOptions.filterNot { it.source.id == source.id } +
                        accounts.map { CommentAccountOption(source, it) }
                    failedSources = failedSources - source.id
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    failedSources = failedSources + source.id
                }
                attemptedSources = attemptedSources + source.id
            }
        } finally {
            loading = false
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
    ) {
        Row(
            modifier = Modifier
                .widthIn(min = 48.dp)
                .heightIn(min = 48.dp)
                .clip(MaterialTheme.shapes.small)
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = enabled)
                .semantics {
                    contentDescription = if (selectedName.isBlank()) "选择发表评论的账号" else "发表评论的账号：$selectedName，选择账号"
                }.testTag("comment_account_selector")
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CommentAccountAvatar(selectedAvatar)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            matchAnchorWidth = false,
            modifier = Modifier.widthIn(min = 240.dp, max = 320.dp).testTag("comment_account_menu"),
        ) {
            Text(
                text = "选择发表评论的账号",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).semantics { heading() },
            )
            HorizontalDivider()
            options.forEach { option ->
                val identity = option.identity
                DropdownMenuItem(
                    text = { Text(identity.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = { CommentAccountAvatar(identity.avatarUrl) },
                    trailingIcon = {
                        if (identity.id == selectedId) Icon(Icons.Default.Check, contentDescription = "当前发送账号")
                    },
                    enabled = enabled,
                    modifier = Modifier.testTag("comment_account_${identity.id}"),
                    onClick = {
                        onSelected(
                            CommentAccountSelection(option.source.id, identity)
                                .takeUnless { identity.id == accountStore.session.profile?.id },
                        )
                        expanded = false
                    },
                )
            }
            if (loading) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("正在加载马甲号…", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (failedSources.isNotEmpty() && !loading) {
                DropdownMenuItem(
                    text = { Text("部分账号的马甲号加载失败，点击重试", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.testTag("comment_account_retry"),
                    onClick = {
                        attemptedSources = attemptedSources - failedSources
                        retryToken += 1
                    },
                )
            }
        }
    }
}

@Composable
private fun CommentAccountAvatar(avatarUrl: String?) {
    Box(
        modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarUrl.isNullOrBlank()) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
        } else {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
