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

package com.github.zly2006.zhihu.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberZhihuAnswerPublisher(): ZhihuAnswerPublisher = remember {
    object : ZhihuAnswerPublisher {
        override val isSupported: Boolean = false

        override suspend fun findMyAnswerId(questionId: Long): Long? = null

        override suspend fun fetchAnswerForEditing(answerId: Long): ExistingAnswerForEditing? = null

        override suspend fun uploadImage(bytes: ByteArray, mimeType: String?, fileName: String?): UploadedZhihuImage = throw UnsupportedOperationException("当前平台暂不支持上传图片")

        override suspend fun uploadImageFromUrl(url: String): UploadedZhihuImage = throw UnsupportedOperationException("当前平台暂不支持上传图片")

        override suspend fun patchDraft(
            questionId: Long,
            answerId: Long?,
            html: String,
            tocEnabled: Boolean,
        ): Unit = throw UnsupportedOperationException("当前平台暂不支持发布/编辑知乎回答")

        override suspend fun publishAnswer(
            questionId: Long,
            answerId: Long?,
            html: String,
            tocEnabled: Boolean,
        ): Long = throw UnsupportedOperationException("当前平台暂不支持发布/编辑知乎回答")
    }
}
