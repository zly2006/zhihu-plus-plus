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

package com.ml.shubham0204.sentence_embeddings

import org.json.JSONObject

class HFTokenizer(
    tokenizerBytes: ByteArray,
) {
    data class Result(
        val ids: LongArray = longArrayOf(),
        val attentionMask: LongArray = longArrayOf(),
        val tokenTypeIds: LongArray = longArrayOf(),
    )

    private val tokenizerPtr: Long = createTokenizer(tokenizerBytes)

    fun tokenize(text: String): Result {
        val output = tokenize(tokenizerPtr, text)
        val jsonObject = JSONObject(output)
        val idsArray = jsonObject.getJSONArray("ids")
        val ids = LongArray(idsArray.length())
        for (i in 0 until idsArray.length()) {
            ids[i] = (idsArray.get(i) as Int).toLong()
        }
        val attentionMaskArray = jsonObject.getJSONArray("attention_mask")
        val attentionMask = LongArray(attentionMaskArray.length())
        for (i in 0 until attentionMaskArray.length()) {
            attentionMask[i] = (attentionMaskArray.get(i) as Int).toLong()
        }
        val tokenTypeIdsArray = jsonObject.getJSONArray("token_type_ids")
        val tokenTypeIds = LongArray(tokenTypeIdsArray.length())
        for (i in 0 until tokenTypeIdsArray.length()) {
            tokenTypeIds[i] = (tokenTypeIdsArray.get(i) as Int).toLong()
        }
        return Result(ids, attentionMask, tokenTypeIds)
    }

    fun close() {
        deleteTokenizer(tokenizerPtr)
    }

    private external fun createTokenizer(tokenizerBytes: ByteArray): Long

    private external fun tokenize(
        tokenizerPtr: Long,
        text: String,
    ): String

    private external fun deleteTokenizer(tokenizerPtr: Long)

    companion object {
        init {
            try {
                System.loadLibrary("hftokenizer")
            } catch (e: UnsatisfiedLinkError) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
