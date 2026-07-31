/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

package com.chloemlla.zhplus.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.chloemlla.zhplus.ui.Collection
import io.ktor.client.call.body
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.reflect.typeOf

class CollectionsViewModel(
    val urlToken: String,
) : PaginationViewModel<Collection>(typeOf<Collection>()) {
    var isCreatingCollection by mutableStateOf(false)
        private set
    var createCollectionError by mutableStateOf<String?>(null)
        private set
    var deletingCollectionId by mutableStateOf<String?>(null)
        private set
    var deleteCollectionError by mutableStateOf<String?>(null)
        private set

    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/people/$urlToken/collections"

    suspend fun createCollection(
        environment: ZhihuApiEnvironment,
        title: String,
        description: String,
        isPublic: Boolean,
    ): Boolean {
        if (isCreatingCollection) return false
        isCreatingCollection = true
        createCollectionError = null
        return try {
            val response = environment.postSigned("https://www.zhihu.com/api/v4/collections") {
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("title", title)
                        put("description", description)
                        put("is_public", isPublic)
                    },
                )
            }
            if (!response.status.isSuccess()) {
                error("创建收藏夹失败：${response.status}")
            }
            val responseBody = response.body<JsonObject>()
            val collectionId = (responseBody["collection"] as? JsonObject)
                ?.get("id")
                ?.jsonPrimitive
                ?.contentOrNull
            if (responseBody["status"]?.jsonPrimitive?.intOrNull != 100 || collectionId.isNullOrBlank()) {
                error(
                    responseBody["message"]
                        ?.jsonPrimitive
                        ?.contentOrNull
                        ?.takeIf(String::isNotBlank)
                        ?: "创建收藏夹失败：响应无效",
                )
            }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            createCollectionError = e.message ?: "创建收藏夹失败"
            false
        } finally {
            isCreatingCollection = false
        }
    }

    suspend fun deleteCollection(
        environment: ZhihuApiEnvironment,
        collection: Collection,
    ): Boolean {
        if (deletingCollectionId != null) return false
        if (collection.isDefault) {
            deleteCollectionError = "默认收藏夹不能删除"
            return false
        }
        deletingCollectionId = collection.id
        deleteCollectionError = null
        return try {
            val response = environment.deleteSigned(
                "https://www.zhihu.com/api/v4/collections/${collection.id}",
            )
            if (!response.status.isSuccess()) {
                error("删除收藏夹失败：${response.status}")
            }
            val responseBody = response.body<JsonObject>()
            if (responseBody["success"]?.jsonPrimitive?.booleanOrNull != true) {
                error(
                    responseBody["message"]
                        ?.jsonPrimitive
                        ?.contentOrNull
                        ?.takeIf(String::isNotBlank)
                        ?: "删除收藏夹失败：响应无效",
                )
            }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            deleteCollectionError = e.message ?: "删除收藏夹失败"
            false
        } finally {
            deletingCollectionId = null
        }
    }

    fun clearMutationErrors() {
        createCollectionError = null
        deleteCollectionError = null
    }
}
