package com.github.zly2006.zhihu.shared.data

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

suspend fun fetchHighestQualityZhihuVideoUrl(
    httpClient: HttpClient,
    videoId: String,
    contentId: String,
    contentType: String = "answer",
    xsrfToken: String? = null,
    configureRequest: HttpRequestBuilder.() -> Unit = {},
): String? {
    val response = httpClient.post("https://www.zhihu.com/api/v4/video/play_info?r=$videoId") {
        contentType(ContentType.Application.Json)
        xsrfToken?.let { header("x-xsrftoken", it) }
        header("x-app-za", "OS=webplayer")
        header("x-referer", "")
        setBody(
            """{"content_id":"$contentId","content_type_str":"$contentType","video_id":"$videoId","scene_code":"answer_detail_web","is_only_video":true}""",
        )
        configureRequest()
    }

    return selectHighestQualityZhihuVideoUrl(
        ZhihuJson.json
            .parseToJsonElement(response.bodyAsText())
            .jsonObject,
    )
}

fun selectHighestQualityZhihuVideoUrl(jsonResponse: JsonObject): String? {
    val mp4List = jsonResponse["video_play"]
        ?.jsonObject
        ?.get("playlist")
        ?.jsonObject
        ?.get("mp4")
        ?.jsonArray

    var bestVideo: JsonObject? = null
    var maxBitrate = -1

    mp4List?.forEach { videoElement ->
        val video = videoElement.jsonObject
        val bitrate = video["bitrate"]?.jsonPrimitive?.intOrNull ?: 0
        if (bitrate > maxBitrate) {
            maxBitrate = bitrate
            bestVideo = video
        }
    }

    return bestVideo
        ?.get("url")
        ?.jsonArray
        ?.firstOrNull()
        ?.jsonPrimitive
        ?.content
}
