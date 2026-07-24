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

package com.github.zly2006.zhihu.ui.subscreens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.data.AccountData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@Composable
actual fun rememberIdentityManagementRuntime(): IdentityManagementRuntime {
    val context = LocalContext.current
    val accountDataState = AccountData.asState()
    val identityClient = remember(context.applicationContext) {
        AccountData.identityClient(context.applicationContext)
    }
    val state = remember {
        MutableStateFlow(
            IdentityManagementState(
                currentAccountId = accountDataState.value.self
                    ?.id
                    .orEmpty(),
                loading = true,
            ),
        )
    }

    LaunchedEffect(accountDataState.value.self?.id) {
        state.update {
            it.copy(
                currentAccountId = accountDataState.value.self
                    ?.id
                    .orEmpty(),
            )
        }
    }

    return remember(context, identityClient, state) {
        IdentityManagementRuntime(
            state = state,
            refresh = refresh@{
                if (state.value.switchingToAccountId != null || state.value.creating) return@refresh
                state.update { it.copy(loading = true, errorMessage = null) }
                try {
                    val accounts = identityClient.listAccounts()
                    state.update {
                        it.copy(
                            accounts = accounts,
                            currentAccountId = AccountData.data.self
                                ?.id
                                .orEmpty(),
                            loading = false,
                            errorMessage = null,
                        )
                    }
                } catch (e: Exception) {
                    state.update {
                        it.copy(
                            loading = false,
                            errorMessage = e.message ?: "获取身份列表失败",
                        )
                    }
                }
            },
            switchAccount = switch@{ targetAccountId ->
                if (state.value.busy) throw IllegalStateException("另一个账号操作正在进行")
                if (targetAccountId == state.value.currentAccountId) return@switch
                state.update {
                    it.copy(
                        switchingToAccountId = targetAccountId,
                        errorMessage = null,
                    )
                }
                try {
                    val result = identityClient.switchAccount(targetAccountId)
                    state.update {
                        it.copy(
                            currentAccountId = result.account.id,
                            switchingToAccountId = null,
                            errorMessage = null,
                        )
                    }
                } catch (e: Exception) {
                    state.update {
                        it.copy(
                            switchingToAccountId = null,
                            errorMessage = e.message ?: "切换账号失败",
                        )
                    }
                    throw e
                }
            },
            createSubAccount = {
                if (state.value.busy) throw IllegalStateException("另一个账号操作正在进行")
                check(state.value.canCreateSubAccount) { "当前账号暂不能创建新账号" }
                state.update {
                    it.copy(
                        creating = true,
                        errorMessage = null,
                    )
                }
                try {
                    val result = identityClient.createSubAccount()
                    state.update {
                        it.copy(
                            accounts = it.accounts + result.account,
                            currentAccountId = result.account.id,
                            creating = false,
                            errorMessage = null,
                        )
                    }
                } catch (e: Exception) {
                    state.update {
                        it.copy(
                            creating = false,
                            errorMessage = e.message ?: "创建新账号失败",
                        )
                    }
                    throw e
                }
            },
            reloadApplication = {
                context.findActivity()?.recreate()
            },
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
