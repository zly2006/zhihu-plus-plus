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

package com.github.zly2006.zhihu.viewmodel.filter

import com.github.zly2006.zhihu.shared.data.OfficialBadge

data class McnAuthorProfile(
    val mcnCompany: String? = null,
    val officialBadge: OfficialBadge? = null,
)

fun interface McnAndBadgeProvider {
    suspend fun getAuthorProfile(urlToken: String): McnAuthorProfile
}

val NoopMcnAndBadgeProvider = McnAndBadgeProvider { McnAuthorProfile() }

internal fun String?.normalizeMcnCompany(): String? {
    val value = this?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return value.takeUnless { it.equals("false", ignoreCase = true) || it.equals("true", ignoreCase = true) }
}

internal fun String.matchesMcnOrganization(other: String): Boolean {
    val left = normalizeMcnCompanyForMatch()
    val right = other.normalizeMcnCompanyForMatch()
    if (left.isBlank() || right.isBlank()) return false
    return left == right || left.contains(right) || right.contains(left)
}

private fun String.normalizeMcnCompanyForMatch(): String = normalizeMcnCompany()
    ?.filterNot { it.isWhitespace() || it == '（' || it == '）' || it == '(' || it == ')' }
    .orEmpty()
