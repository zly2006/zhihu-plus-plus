package com.github.zly2006.zhihu.shared.comment

import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray

fun decodeZhihuCommentData(
    json: JsonObject,
    limit: Int,
): List<DataHolder.Comment> =
    json["data"]
        ?.jsonArray
        ?.mapNotNull { element ->
            runCatching {
                ZhihuJson.decodeJson<DataHolder.Comment>(element)
            }.getOrNull()
        }?.take(limit)
        .orEmpty()
