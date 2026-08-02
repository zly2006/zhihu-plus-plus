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

package com.github.zly2006.zhihu.data

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertTrue

class CommentDeletePermissionTest {
    @Test
    fun decodesDeletePermissionFromCommentV5Response() {
        val comment = ZhihuJson.decodeJson<DataHolder.Comment>(
            Json.parseToJsonElement(
                """
                {
                  "id": "11542410327",
                  "type": "comment",
                  "resource_type": "segment",
                  "url": "https://www.zhihu.com/api/v4/comment_v5/comment/11542410327",
                  "content": "<p>测试评论</p>",
                  "created_time": 1750000000,
                  "is_delete": true,
                  "collapsed": false,
                  "reviewing": false,
                  "can_delete": true,
                  "author": {
                    "id": "author-id",
                    "url_token": "author-token",
                    "name": "作者",
                    "avatar_url": "https://example.invalid/avatar.png",
                    "avatar_url_template": "",
                    "is_org": false,
                    "type": "people",
                    "url": "https://www.zhihu.com/people/author-token",
                    "user_type": "people",
                    "headline": "",
                    "gender": 0,
                    "is_advertiser": false
                  }
                }
                """.trimIndent(),
            ),
        )

        assertTrue(comment.canDelete)
    }
}
