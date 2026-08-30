/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under the GNU Affero General Public License version 3.
 */

package com.github.zly2006.zhihu.account

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.desktop.defaultDesktopAccountStore

@Composable
actual fun rememberZhihuAccountStore(): ZhihuAccountStore = defaultDesktopAccountStore
