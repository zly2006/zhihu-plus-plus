package com.github.zly2006.zhihu.viewmodel.filter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

// TODO: iOS 内容过滤数据库完整实现
@Composable
actual fun rememberBlockedFeedRecordDao(): BlockedFeedRecordDao = remember {
    object : BlockedFeedRecordDao {
        override fun observeAll(): Flow<List<BlockedFeedRecord>> = emptyFlow()

        override suspend fun getRecent(limit: Int): List<BlockedFeedRecord> = emptyList()

        override suspend fun insert(record: BlockedFeedRecord): Long = 0L

        override suspend fun deleteById(id: Long) = Unit

        override suspend fun clearAll() = Unit

        override suspend fun maintainLimit() = Unit
    }
}
