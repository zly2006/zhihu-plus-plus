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

package com.github.zly2006.zhihu.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class OfficialBadge(
    val title: String,
    val description: String,
    val iconUrl: String = "",
    val nightIconUrl: String = "",
    val url: String = "",
    val type: String = "",
    val detailType: String = "",
) {
    val isGenericCertification: Boolean
        get() = title == "认证"

    val isUsefulInList: Boolean
        get() = !isGenericCertification

    fun displayTitle(expandGenericCertification: Boolean = false): String = if (expandGenericCertification && isGenericCertification) {
        description
    } else {
        title
    }
}

fun DataHolder.BadgeV2?.officialBadge(): OfficialBadge? {
    this ?: return null
    val details = officialBadgeDetails()
    val primary = details.firstOrNull { it.type != "identity" && it.iconUrl.isNotBlank() }
        ?: details.firstOrNull { it.iconUrl.isNotBlank() }
        ?: listOfNotNull(mergedBadges)
            .flatten()
            .mapNotNull(JsonElement::asOfficialBadge)
            .firstOrNull()

    return primary
        ?.copy(
            iconUrl = icon.ifBlank { primary.iconUrl },
            nightIconUrl = nightIcon.ifBlank { primary.nightIconUrl },
        )
        ?: title.takeIf { it.isNotBlank() }?.let {
            OfficialBadge(
                title = it,
                description = it,
                iconUrl = icon,
                nightIconUrl = nightIcon,
            )
        }
}

fun DataHolder.BadgeV2?.officialBadgeDetails(): List<OfficialBadge> {
    this ?: return emptyList()
    val details = detailBadges
        ?.mapNotNull(JsonElement::asOfficialBadge)
        .orEmpty()
    if (details.isNotEmpty()) return details
    return mergedBadges
        ?.mapNotNull(JsonElement::asOfficialBadge)
        .orEmpty()
}

private fun JsonElement.asOfficialBadge(): OfficialBadge? {
    val badge = this as? JsonObject ?: return null
    val status = badge.stringValue("badgeStatus") ?: badge.stringValue("badge_status")
    if (status != null && status != "passed") return null
    val title = badge.stringValue("title")?.takeIf { it.isNotBlank() } ?: return null
    val description = badge.stringValue("description")?.takeIf { it.isNotBlank() } ?: title
    return OfficialBadge(
        title = title,
        description = description,
        iconUrl = badge.stringValue("icon").orEmpty(),
        nightIconUrl = badge.stringValue("nightIcon")?.takeIf { it.isNotBlank() }
            ?: badge.stringValue("night_icon").orEmpty(),
        url = badge.stringValue("url").orEmpty(),
        type = badge.stringValue("type").orEmpty(),
        detailType = badge.stringValue("detailType")?.takeIf { it.isNotBlank() }
            ?: badge.stringValue("detail_type").orEmpty(),
    )
}

private fun JsonObject.stringValue(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
