package com.github.zly2006.zhihu.viewmodel.filter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
actual fun rememberBlockedFeedRecordDao(): BlockedFeedRecordDao = remember {
    object : BlockedFeedRecordDao {
        override fun observeAll(): Flow<List<BlockedFeedRecord>> = emptyFlow() // TODO: iOS 观察所有记录

        override suspend fun getRecent(limit: Int): List<BlockedFeedRecord> = emptyList() // TODO: iOS 获取最近记录

        override suspend fun insert(record: BlockedFeedRecord): Long = 0L // TODO: iOS 插入记录

        override suspend fun deleteById(id: Long) = Unit // TODO: iOS 删除记录

        override suspend fun clearAll() = Unit // TODO: iOS 清空所有

        override suspend fun maintainLimit() = Unit // TODO: iOS 维护限制
    }
} // TODO: iOS 内容过滤数据库完整实现
