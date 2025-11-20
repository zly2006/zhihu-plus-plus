package com.github.zly2006.zhihu.viewmodel.filter

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BlockedUserDao {
    @Query("SELECT * FROM ${BlockedUser.TABLE_NAME} ORDER BY createdTime DESC")
    suspend fun getAllUsers(): List<BlockedUser>

    @Query("SELECT * FROM ${BlockedUser.TABLE_NAME} WHERE userId = :userId")
    suspend fun getUserById(userId: String): BlockedUser?

    @Query("SELECT userId FROM ${BlockedUser.TABLE_NAME}")
    suspend fun getAllUserIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: BlockedUser)

    @Delete
    suspend fun deleteUser(user: BlockedUser)

    @Query("DELETE FROM ${BlockedUser.TABLE_NAME} WHERE userId = :userId")
    suspend fun deleteUserById(userId: String)

    @Query("DELETE FROM ${BlockedUser.TABLE_NAME}")
    suspend fun clearAllUsers()

    @Query("SELECT COUNT(*) FROM ${BlockedUser.TABLE_NAME}")
    suspend fun getUserCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM ${BlockedUser.TABLE_NAME} WHERE userId = :userId)")
    suspend fun isUserBlocked(userId: String): Boolean
}
