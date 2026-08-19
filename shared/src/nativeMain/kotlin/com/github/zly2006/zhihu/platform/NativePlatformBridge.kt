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

package com.github.zly2006.zhihu.platform

import kotlinx.coroutines.flow.StateFlow

internal expect val nativePlatformName: String

internal expect val nativeIsDesktop: Boolean

internal expect val nativeAppVersionName: String

internal expect val nativeQrLoginRequestVersion: StateFlow<Int>

internal expect fun openNativeExternalUrl(url: String)

internal expect fun copyNativePlainText(text: String)

internal expect fun requestNativeQrLogin()

internal expect fun nativeAccountFilePath(): String

internal expect fun nativeAppPrivateDirectoryPath(): String

internal expect fun nativeDownloadsDirectoryPath(): String

internal expect fun nativeBundledResourcePath(relativePath: String): String?

internal expect fun nativeSettingsStore(relativePath: String): SettingsStore

internal expect fun nativeSystemInDarkTheme(): Boolean

internal expect fun showNativeUserMessage(message: String)
