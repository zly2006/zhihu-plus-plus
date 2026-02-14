package com.github.zly2006.zhihu.viewmodel.filter

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 屏蔽主题实体
 */
@Entity(tableName = "blocked_topics")
data class BlockedTopic(
    @PrimaryKey
    val topicId: String,
    val topicName: String,
    val addedTime: Long = System.currentTimeMillis(),
)
