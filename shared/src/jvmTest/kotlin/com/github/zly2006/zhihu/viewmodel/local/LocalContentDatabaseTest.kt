package com.github.zly2006.zhihu.viewmodel.local

import kotlinx.coroutines.test.runTest
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalContentDatabaseTest {
    @Test
    fun createsJvmRoomDatabaseAndStoresTask() = runTest {
        val database = getLocalContentDatabase(
            createTempDirectory("local-content-room").resolve("local-content.db").toFile(),
        )

        val taskId = database.contentDao().insertTask(
            CrawlingTask(
                url = "https://www.zhihu.com/question/1",
                reason = CrawlingReason.Trending,
            ),
        )

        val tasks = database.contentDao().getTasksByStatus(CrawlingStatus.NotStarted)
        assertEquals(1L, taskId)
        assertEquals(listOf("https://www.zhihu.com/question/1"), tasks.map { it.url })
        database.close()
    }
}
