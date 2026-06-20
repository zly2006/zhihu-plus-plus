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

import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import io.ktor.http.HttpMethod
import kotlinx.serialization.encodeToString

fun mockRootComments(
    urlPrefix: String,
    commentId: String = "offline-root-comment",
) {
    val comment = DataHolder.Comment(
        id = commentId,
        type = "comment",
        resourceType = "answer",
        url = "https://www.zhihu.com/comment/$commentId",
        content = "<p>离线评论内容</p>",
        createdTime = 1_713_500_000L,
        isDelete = false,
        collapsed = false,
        reviewing = false,
        liked = false,
        likeCount = 0,
        isAuthor = false,
        author = DataHolder.Comment.Author(
            id = "offline-comment-author",
            urlToken = "offline-comment-author-token",
            name = "离线评论作者",
            avatarUrl = "https://example.invalid/avatar.png",
            avatarUrlTemplate = "",
            isOrg = false,
            type = "people",
            url = "https://www.zhihu.com/people/offline-comment-author-token",
            userType = "people",
            headline = "离线评论作者签名",
            gender = 0,
            isAdvertiser = false,
        ),
        childCommentCount = 0,
        childComments = emptyList(),
    )
    ZhihuMockApi.mockJsonPrefix(
        method = HttpMethod.Get,
        urlPrefix = urlPrefix,
        body =
            """
            {
              "data": [${ZhihuJson.json.encodeToString(comment)}],
              "paging": {
                "is_end": true,
                "is_start": true,
                "totals": 1,
                "next": ""
              }
            }
            """.trimIndent(),
    )
}
