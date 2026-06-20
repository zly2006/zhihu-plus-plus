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

package com.github.zly2006.zhihu.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.shared.util.HttpStatusException

@Composable
internal fun WriteOperationErrorDialog(
    message: String?,
    onDismissRequest: () -> Unit,
    onCopy: (String) -> Unit,
) {
    if (message == null) return

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("操作失败") },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
            )
        },
        confirmButton = {
            Button(onClick = onDismissRequest) {
                Text("确定")
            }
        },
        dismissButton = {
            FilledTonalButton(onClick = { onCopy(message) }) {
                Text("复制")
            }
        },
    )
}

internal fun buildWriteOperationErrorMessage(
    title: String,
    throwable: Throwable,
): String {
    var httpStatusException: HttpStatusException? = null
    val chain = buildList {
        var current: Throwable? = throwable
        while (current != null) {
            if (httpStatusException == null && current is HttpStatusException) {
                httpStatusException = current
            }
            add("${current::class.qualifiedName}: ${current.message.orEmpty()}")
            current = current.cause
        }
    }
    return buildString {
        append(title).append('\n')
        append('\n')
        chain.forEachIndexed { index, item ->
            append("#")
                .append(index + 1)
                .append(' ')
                .append(item)
                .append('\n')
        }
        if (httpStatusException != null) {
            append('\n')
            append("HTTP 状态: ")
                .append(httpStatusException.status.value)
                .append(' ')
                .append(httpStatusException.status.description)
                .append('\n')
            append("请求地址: ")
                .append(httpStatusException.requestUrl)
                .append('\n')
            if (httpStatusException.bodyText.isNotBlank()) {
                append('\n')
                append("响应体:\n")
                append(httpStatusException.bodyText.trim())
                    .append('\n')
            }
            httpStatusException.dumpedCurlRequest
                ?.takeIf { it.isNotBlank() }
                ?.let { curl ->
                    append('\n')
                    append("请求复现:\n")
                    append(curl.trim())
                        .append('\n')
                }
        }
    }.trim()
}
