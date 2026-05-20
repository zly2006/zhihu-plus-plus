package com.github.zly2006.zhihu.viewmodel.filter

import kotlinx.coroutines.test.runTest
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class ContentFilterDatabaseTest {
    @Test
    fun createsJvmRoomDatabaseAndStoresKeyword() = runTest {
        val database = getContentFilterDatabase(
            createTempDirectory("content-filter-room").resolve("content-filter.db").toFile(),
        )

        val keywordId = database.blockedKeywordDao().insertKeyword(
            BlockedKeyword(keyword = "noise"),
        )

        val keywords = database.blockedKeywordDao().getAllKeywords()
        assertEquals(1L, keywordId)
        assertEquals(listOf("noise"), keywords.map { it.keyword })
        database.close()
    }
}
