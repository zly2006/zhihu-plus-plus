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
