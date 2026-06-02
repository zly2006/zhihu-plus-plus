/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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
