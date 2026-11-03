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

package com.github.zly2006.zhihu.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.github.zly2006.zhihu.platform.SettingsStore
import com.github.zly2006.zhihu.platform.rememberAppPrivateDirectory
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.hrm.latex.renderer.font.MathFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

const val CUSTOM_MATH_FONTS_KEY = "customMathFonts"
const val SELECTED_MATH_FONT_KEY = "selectedMathFont"
const val MAX_MATH_FONT_BYTES = 20 * 1024 * 1024

@Serializable
data class ImportedMathFont(
    val id: String,
    val name: String,
)

/** Owns imported bytes; the original document permission is never needed after import. */
class CustomMathFonts(
    private val directory: Path,
    private val settings: SettingsStore,
) {
    fun list(): List<ImportedMathFont> = runCatching {
        Json.decodeFromString(ListSerializer(ImportedMathFont.serializer()), settings.getString(CUSTOM_MATH_FONTS_KEY, "[]"))
    }.getOrDefault(emptyList()).filter { it.id.matches(Regex("[0-9a-f-]{36}")) }

    fun selected(): ImportedMathFont? = list().find { it.id == settings.getStringOrNull(SELECTED_MATH_FONT_KEY) }

    fun select(font: ImportedMathFont?) {
        require(font == null || font in list())
        if (font == null) settings.remove(SELECTED_MATH_FONT_KEY) else settings.putString(SELECTED_MATH_FONT_KEY, font.id)
    }

    @OptIn(ExperimentalUuidApi::class)
    fun import(name: String, bytes: ByteArray): ImportedMathFont {
        validateMathFont(bytes)
        val font = ImportedMathFont(
            Uuid.random().toString(),
            name
                .substringAfterLast('/')
                .substringAfterLast('\\')
                .take(160)
                .ifBlank { "数学字体" },
        )
        SystemFileSystem.createDirectories(directory)
        val file = Path(directory, font.id)
        SystemFileSystem.sink(file).buffered().use { it.write(bytes) }
        try {
            // Resolve the platform face before changing saved metadata or selection.
            createImportedMathFont(bytes, font.id, file)
            settings.putString(CUSTOM_MATH_FONTS_KEY, Json.encodeToString(ListSerializer(ImportedMathFont.serializer()), list() + font))
        } catch (error: Exception) {
            SystemFileSystem.delete(file)
            throw error
        }
        select(font)
        return font
    }

    fun load(font: ImportedMathFont): MathFont.OTF {
        require(font in list())
        val file = Path(directory, font.id)
        require((SystemFileSystem.metadataOrNull(file)?.size ?: 0) in 1..MAX_MATH_FONT_BYTES.toLong()) { "字体文件大小无效" }
        val bytes = SystemFileSystem.source(file).buffered().use { it.readByteArray() }
        validateMathFont(bytes)
        return createImportedMathFont(bytes, font.id, file)
    }
}

/** Validate the SFNT directory before accepting a font, including bounds of every table. */
internal fun validateMathFont(bytes: ByteArray) {
    require(bytes.size in 12..MAX_MATH_FONT_BYTES) { "请选择不超过 20 MB 的 OpenType 数学字体" }

    fun u16(offset: Int) = ((bytes[offset].toInt() and 255) shl 8) or (bytes[offset + 1].toInt() and 255)

    fun u32(offset: Int): Long = (0..3).fold(0L) { value, i -> (value shl 8) or (bytes[offset + i].toLong() and 255) }
    require(u32(0) == 0x4f54544fL || u32(0) == 0x00010000L) { "不支持此字体格式，请选择 OTF 或 TTF 文件" }
    val count = u16(4)
    require(count > 0 && 12L + count * 16L <= bytes.size) { "字体文件已损坏" }
    var math = false
    repeat(count) { index ->
        val record = 12 + index * 16
        val offset = u32(record + 8)
        val length = u32(record + 12)
        require(offset + length <= bytes.size) { "字体文件已损坏" }
        if (u32(record) == 0x4d415448L) {
            require(length >= 10 && u16(offset.toInt()) == 1) { "数学字体的 MATH 表无效" }
            math = true
        }
    }
    require(math) { "此字体不含 OpenType MATH 表，请选择 Latin Modern Math 等数学字体" }
}

internal expect fun createImportedMathFont(bytes: ByteArray, identity: String, file: Path): MathFont.OTF

@Composable
fun rememberCustomMathFonts(): CustomMathFonts {
    val directory = rememberAppPrivateDirectory()
    val settings = rememberSettingsStore()
    return remember(directory, settings) { CustomMathFonts(Path(directory, "custom-math-fonts"), settings) }
}

@Composable
fun rememberSelectedMathFont(): ImportedMathFont? {
    val settings = rememberSettingsStore()
    val fonts = rememberCustomMathFonts()
    var selected by remember(fonts) { mutableStateOf(fonts.selected()) }
    DisposableEffect(settings, fonts) {
        val subscription = settings.observeKeyChanges { key ->
            if (key == SELECTED_MATH_FONT_KEY || key == CUSTOM_MATH_FONTS_KEY) selected = fonts.selected()
        }
        onDispose { subscription.close() }
    }
    return selected
}

@Composable
fun rememberImportedMathFont(): MathFont.OTF? {
    val fonts = rememberCustomMathFonts()
    val selected = rememberSelectedMathFont()
    val messages = rememberUserMessageSink()
    var loaded by remember(selected) { mutableStateOf<MathFont.OTF?>(null) }
    LaunchedEffect(fonts, selected) {
        loaded = selected?.let {
            val result = withContext(Dispatchers.Default) { runCatching { fonts.load(it) } }
            if (result.isFailure) messages.showLongMessage("无法读取所选公式字体，已暂用默认字体；请重新导入。")
            result.getOrNull()
        }
    }
    return loaded
}
