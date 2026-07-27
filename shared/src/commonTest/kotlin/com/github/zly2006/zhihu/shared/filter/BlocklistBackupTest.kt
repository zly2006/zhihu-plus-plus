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

package com.github.zly2006.zhihu.shared.filter

import com.github.zly2006.zhihu.viewmodel.filter.BlocklistBackup
import com.github.zly2006.zhihu.viewmodel.filter.KeywordBackup
import com.github.zly2006.zhihu.viewmodel.filter.NlpKeywordBackup
import com.github.zly2006.zhihu.viewmodel.filter.TopicBackup
import com.github.zly2006.zhihu.viewmodel.filter.UserBackup
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

class BlocklistBackupTest {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @Test
    fun serializesBlocklistBackupWithAllSections() {
        val backup = BlocklistBackup(
            exportTime = 1234,
            keywords = listOf(
                KeywordBackup(
                    keyword = "广告",
                    caseSensitive = true,
                    isRegex = false,
                ),
            ),
            nlpKeywords = listOf(NlpKeywordBackup("低质内容")),
            users = listOf(
                UserBackup(
                    userId = "user-1",
                    userName = "用户",
                    urlToken = "token",
                    avatarUrl = "https://example.com/avatar.png",
                ),
            ),
            questionAuthors = listOf(
                UserBackup(
                    userId = "asker-1",
                    userName = "提问者",
                    urlToken = "asker-token",
                    avatarUrl = "https://example.com/asker.png",
                ),
            ),
            topics = listOf(TopicBackup(topicId = "topic-1", topicName = "主题")),
        )

        val encoded = json.encodeToString(BlocklistBackup.serializer(), backup)
        val decoded = json.decodeFromString(BlocklistBackup.serializer(), encoded)
        val element = json.parseToJsonElement(encoded).jsonObject

        assertTrue("keywords" in element)
        assertTrue("questionAuthors" in element)
        assertEquals(backup, decoded)
    }

    @Test
    fun defaultExportTimeUsesCurrentClock() {
        val before = Clock.System.now().toEpochMilliseconds()
        val backup = BlocklistBackup()
        val after = Clock.System.now().toEpochMilliseconds()

        assertTrue(backup.exportTime in before..after)
    }
}
