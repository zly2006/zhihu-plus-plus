package com.github.zly2006.zhihu.viewmodel.local

import androidx.room.TypeConverter

class LocalDatabaseConverters {
    @TypeConverter
    fun fromCrawlingReason(reason: CrawlingReason): String = reason.name

    @TypeConverter
    fun toCrawlingReason(reasonString: String): CrawlingReason = CrawlingReason.valueOf(reasonString)

    @TypeConverter
    fun fromCrawlingStatus(status: CrawlingStatus): String = status.name

    @TypeConverter
    fun toCrawlingStatus(statusString: String): CrawlingStatus = CrawlingStatus.valueOf(statusString)
}
