package com.github.zly2006.zhihu.viewmodel.filter

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BlockedKeywordDao {
    @Query("SELECT * FROM ${BlockedKeyword.TABLE_NAME} ORDER BY createdTime DESC")
    suspend fun getAllKeywords(): List<BlockedKeyword>

    @Query("SELECT * FROM ${BlockedKeyword.TABLE_NAME} WHERE id = :id")
    suspend fun getKeywordById(id: Long): BlockedKeyword?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeyword(keyword: BlockedKeyword): Long

    @Delete
    suspend fun deleteKeyword(keyword: BlockedKeyword)

    @Query("DELETE FROM ${BlockedKeyword.TABLE_NAME} WHERE id = :id")
    suspend fun deleteKeywordById(id: Long)

    @Query("DELETE FROM ${BlockedKeyword.TABLE_NAME}")
    suspend fun clearAllKeywords()

    @Query("SELECT COUNT(*) FROM ${BlockedKeyword.TABLE_NAME}")
    suspend fun getKeywordCount(): Int
}
