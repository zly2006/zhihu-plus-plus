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

@file:OptIn(kotlinx.cinterop.BetaInteropApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package com.github.zly2006.zhihu.platform

import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding

private const val STRING_SET_SEPARATOR = '\u001F'

internal actual fun nativeSettingsStore(relativePath: String): SettingsStore {
    val propertiesFile = MacosPropertiesFile("${nativeAppPrivateDirectoryPath()}/$relativePath")

    return SettingsStore(
        getBoolean = { key, defaultValue ->
            propertiesFile[key]?.toBooleanStrictOrNull() ?: defaultValue
        },
        putBoolean = { key, value -> propertiesFile[key] = value.toString() },
        getString = { key, defaultValue -> propertiesFile[key] ?: defaultValue },
        putString = { key, value -> propertiesFile[key] = value },
        getStringOrNull = propertiesFile::get,
        putStringSet = { key, value -> propertiesFile[key] = value.joinToString(STRING_SET_SEPARATOR.toString()) },
        getStringSet = { key, defaultValue ->
            propertiesFile[key]
                ?.split(STRING_SET_SEPARATOR)
                ?.filter(String::isNotEmpty)
                ?.toSet() ?: defaultValue
        },
        getInt = { key, defaultValue -> propertiesFile[key]?.toIntOrNull() ?: defaultValue },
        putInt = { key, value -> propertiesFile[key] = value.toString() },
        getLong = { key, defaultValue -> propertiesFile[key]?.toLongOrNull() ?: defaultValue },
        putLong = { key, value -> propertiesFile[key] = value.toString() },
        getFloat = { key, defaultValue -> propertiesFile[key]?.toFloatOrNull() ?: defaultValue },
        putFloat = { key, value -> propertiesFile[key] = value.toString() },
        remove = propertiesFile::remove,
    )
}

private class MacosPropertiesFile(
    private val filePath: String,
) {
    private val values = linkedMapOf<String, String>()

    init {
        val fileManager = NSFileManager.defaultManager
        if (fileManager.fileExistsAtPath(filePath)) {
            val data = fileManager.contentsAtPath(filePath)
            val text = data?.let { NSString.create(data = it, encoding = NSUTF8StringEncoding)?.toString() }
            if (text != null) {
                read(text)
            }
        }
    }

    operator fun get(key: String): String? = values[key]

    operator fun set(key: String, value: String) {
        values[key] = value
        save()
    }

    fun remove(key: String) {
        if (values.remove(key) != null) {
            save()
        }
    }

    private fun read(text: String) {
        val logicalLines = mutableListOf<String>()
        var currentLine = ""
        text.lineSequence().forEach { physicalLine ->
            currentLine += physicalLine
            var trailingBackslashes = 0
            for (index in currentLine.lastIndex downTo 0) {
                if (currentLine[index] != '\\') break
                trailingBackslashes += 1
            }
            if (trailingBackslashes % 2 == 1) {
                currentLine = currentLine.dropLast(1)
            } else {
                logicalLines += currentLine
                currentLine = ""
            }
        }
        if (currentLine.isNotEmpty()) logicalLines += currentLine

        logicalLines.forEach { line ->
            val trimmedStart = line.trimStart()
            if (trimmedStart.isEmpty() || trimmedStart.startsWith('#') || trimmedStart.startsWith('!')) return@forEach
            var escaped = false
            var separatorIndex = -1
            for (index in trimmedStart.indices) {
                val character = trimmedStart[index]
                if (!escaped && (character == '=' || character == ':' || character.isWhitespace())) {
                    separatorIndex = index
                    break
                }
                escaped = !escaped && character == '\\'
                if (character != '\\') escaped = false
            }
            val rawKey = if (separatorIndex >= 0) trimmedStart.substring(0, separatorIndex) else trimmedStart
            var valueStart = if (separatorIndex >= 0) separatorIndex else trimmedStart.length
            while (valueStart < trimmedStart.length && trimmedStart[valueStart].isWhitespace()) valueStart += 1
            if (valueStart < trimmedStart.length && (trimmedStart[valueStart] == '=' || trimmedStart[valueStart] == ':')) {
                valueStart += 1
            }
            while (valueStart < trimmedStart.length && trimmedStart[valueStart].isWhitespace()) valueStart += 1
            values[unescape(rawKey)] = unescape(trimmedStart.substring(valueStart))
        }
    }

    private fun save() {
        val parentDirectory = filePath.substringBeforeLast('/')
        val fileManager = NSFileManager.defaultManager
        if (!fileManager.fileExistsAtPath(parentDirectory)) {
            fileManager.createDirectoryAtPath(
                parentDirectory,
                withIntermediateDirectories = true,
                attributes = null,
                error = null,
            )
        }
        val text = buildString {
            append("#Zhihu++ desktop settings\n")
            values.forEach { (key, value) ->
                append(escape(key))
                append('=')
                append(escape(value))
                append('\n')
            }
        }
        val data = NSString.create(string = text).dataUsingEncoding(NSUTF8StringEncoding)
        fileManager.createFileAtPath(filePath, contents = data, attributes = null)
    }

    private fun unescape(value: String): String = buildString {
        var index = 0
        while (index < value.length) {
            val character = value[index]
            if (character != '\\' || index == value.lastIndex) {
                append(character)
                index += 1
                continue
            }
            index += 1
            when (val escaped = value[index]) {
                't' -> append('\t')
                'n' -> append('\n')
                'r' -> append('\r')
                'f' -> append('\u000C')
                'u' -> {
                    val endIndex = (index + 5).coerceAtMost(value.length)
                    val codePoint = value.substring(index + 1, endIndex).toIntOrNull(16)
                    if (codePoint != null && endIndex == index + 5) {
                        append(codePoint.toChar())
                        index += 4
                    } else {
                        append(escaped)
                    }
                }
                else -> append(escaped)
            }
            index += 1
        }
    }

    private fun escape(value: String): String = buildString {
        value.forEachIndexed { index, character ->
            when {
                character == ' ' && index == 0 -> append("\\ ")
                character == '\\' -> append("\\\\")
                character == '\t' -> append("\\t")
                character == '\n' -> append("\\n")
                character == '\r' -> append("\\r")
                character == '\u000C' -> append("\\f")
                character == '=' || character == ':' || character == '#' || character == '!' -> {
                    append('\\')
                    append(character)
                }
                character.code !in 0x20..0x7E -> append("\\u${character.code.toString(16).uppercase().padStart(4, '0')}")
                else -> append(character)
            }
        }
    }
}
