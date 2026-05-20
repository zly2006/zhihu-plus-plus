package com.github.zly2006.zhihu.shared.data

import kotlinx.serialization.Serializable

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
}

fun DataHolder.BadgeV2?.officialBadge(): OfficialBadge? {
    this ?: return null
    val details = officialBadgeDetails()
    val primary = details.firstOrNull { it.type != "identity" && it.iconUrl.isNotBlank() }
        ?: details.firstOrNull { it.iconUrl.isNotBlank() }
        ?: return null

    return primary
        .copy(
            iconUrl = icon.ifBlank { primary.iconUrl },
            nightIconUrl = nightIcon.ifBlank { primary.nightIconUrl },
        )
}

fun DataHolder.BadgeV2?.officialBadgeDetails(): List<OfficialBadge> {
    this ?: return emptyList()
    val details = detailBadges
        ?.mapNotNull(DataHolder.BadgeV2.Badge::asOfficialBadge)
        .orEmpty()
    if (details.isNotEmpty()) return details
    return mergedBadges
        ?.mapNotNull(DataHolder.BadgeV2.Badge::asOfficialBadge)
        .orEmpty()
}

private fun DataHolder.BadgeV2.Badge.asOfficialBadge(): OfficialBadge? {
    if (badgeStatus != null && badgeStatus != "passed") return null
    val title = title.takeIf { it.isNotBlank() } ?: return null
    val description = description.takeIf { it.isNotBlank() } ?: title
    return OfficialBadge(
        title = title,
        description = description,
        iconUrl = icon,
        nightIconUrl = nightIcon,
        url = url,
        type = type,
        detailType = detailType,
    )
}
