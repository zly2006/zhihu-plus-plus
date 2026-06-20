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

package com.github.zly2006.zhihu.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

inline fun <reified VM : ViewModel> MainActivityComposeRule.seedViewModel(
    noinline create: () -> VM,
): VM = seedViewModel(key = null, create = create)

inline fun <reified VM : ViewModel> MainActivityComposeRule.seedViewModel(
    key: String?,
    noinline create: () -> VM,
): VM {
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras,
        ): T {
            if (modelClass.isAssignableFrom(VM::class.java)) {
                return create() as T
            }
            error("Unexpected ViewModel class $modelClass")
        }
    }
    val provider = ViewModelProvider(activity, factory)
    return if (key == null) {
        provider[VM::class.java]
    } else {
        provider[key, VM::class.java]
    }
}
