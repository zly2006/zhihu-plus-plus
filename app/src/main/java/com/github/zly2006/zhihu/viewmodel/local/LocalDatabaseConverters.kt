package com.github.zly2006.zhihu.viewmodel.local

import androidx.room.TypeConverter

class LocalDatabaseConverters {
    @TypeConverter
    fun fromCrawlingReason(reason: CrawlingReason): String {
        return reason.name
    }

    @TypeConverter
    fun toCrawlingReason(reasonString: String): CrawlingReason {
        return CrawlingReason.valueOf(reasonString)
    }

    @TypeConverter
    fun fromCrawlingStatus(status: CrawlingStatus): String {
        return status.name
    }

    @TypeConverter
    fun toCrawlingStatus(statusString: String): CrawlingStatus {
        return CrawlingStatus.valueOf(statusString)
    }
}
