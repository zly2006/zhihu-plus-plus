package com.github.zly2006.zhihu.viewmodel.local

import androidx.room.TypeConverter

class LocalDatabaseConverters {
    @TypeConverter
    fun fromCrawlingStatus(status: CrawlingStatus): String = status.name

    @TypeConverter
    fun toCrawlingStatus(status: String): CrawlingStatus = CrawlingStatus.valueOf(status)

    @TypeConverter
    fun fromCrawlingReason(reason: CrawlingReason?): String? = reason?.name

    @TypeConverter
    fun toCrawlingReason(reason: String?): CrawlingReason? = reason?.let { CrawlingReason.valueOf(it) }

    @TypeConverter
    fun fromUserActionType(actionType: UserActionType): String = actionType.name

    @TypeConverter
    fun toUserActionType(actionType: String): UserActionType = UserActionType.valueOf(actionType)
}
