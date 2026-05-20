package com.github.zly2006.zhihu.shared.filter

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
            topics = listOf(TopicBackup(topicId = "topic-1", topicName = "主题")),
        )

        val encoded = json.encodeToString(BlocklistBackup.serializer(), backup)
        val decoded = json.decodeFromString(BlocklistBackup.serializer(), encoded)
        val element = json.parseToJsonElement(encoded).jsonObject

        assertTrue("keywords" in element)
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
