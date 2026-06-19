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

package com.github.zly2006.zhihu.viewmodel.filter

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BlockedMcnOrganizationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrganization(organization: BlockedMcnOrganization)

    @Query("DELETE FROM ${BlockedMcnOrganization.TABLE_NAME} WHERE organizationName = :organizationName")
    suspend fun deleteOrganizationByName(organizationName: String)

    @Query("SELECT * FROM ${BlockedMcnOrganization.TABLE_NAME} ORDER BY addedTime DESC")
    suspend fun getAllOrganizations(): List<BlockedMcnOrganization>

    @Query("SELECT COUNT(*) FROM ${BlockedMcnOrganization.TABLE_NAME}")
    suspend fun getOrganizationCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM ${BlockedMcnOrganization.TABLE_NAME})")
    suspend fun hasOrganizations(): Boolean

    @Query("DELETE FROM ${BlockedMcnOrganization.TABLE_NAME}")
    suspend fun clearAllOrganizations()
}
