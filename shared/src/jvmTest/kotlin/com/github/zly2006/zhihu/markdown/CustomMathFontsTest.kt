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

import com.github.zly2006.zhihu.platform.MapSettingsStore
import kotlinx.io.files.Path
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CustomMathFontsTest {
    @Test
    fun selectingDefaultKeepsImportedFontsAndPersistsAcrossInstances() {
        val directory = Files.createTempDirectory("math-font-test").toFile()
        try {
            val settings = MapSettingsStore()
            val id = "01234567-0123-0123-0123-0123456789ab"
            settings.putString(CUSTOM_MATH_FONTS_KEY, """[{"id":"$id","name":"数学字体.otf"}]""")
            val fonts = CustomMathFonts(Path(directory.absolutePath), settings)
            val font = fonts.list().single()
            fonts.select(font)
            assertEquals(font, CustomMathFonts(Path(directory.absolutePath), settings).selected())
            fonts.select(null)
            assertNull(fonts.selected())
            assertEquals(listOf(font), fonts.list())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun invalidImportPreservesSelectionAndDoesNotWriteFiles() {
        val directory = Files.createTempDirectory("math-font-test").toFile()
        try {
            val settings = MapSettingsStore()
            val fonts = CustomMathFonts(Path(directory.absolutePath), settings)
            assertFailsWith<IllegalArgumentException> { fonts.import("fake.otf", "not a font".encodeToByteArray()) }
            assertTrue(fonts.list().isEmpty())
            assertNull(fonts.selected())
            assertTrue(directory.listFiles().orEmpty().isEmpty())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun rejectsNonMathFontsAndOutOfBoundsMathTables() {
        // SFNT header + one table directory entry + the ten-byte MATH header.
        val font = ByteArray(38)
        font[0] = 0x4f
        font[1] = 0x54
        font[2] = 0x54
        font[3] = 0x4f
        font[5] = 1
        "name".encodeToByteArray().copyInto(font, 12)
        font[23] = 28
        font[27] = 10
        assertFailsWith<IllegalArgumentException> { validateMathFont(font) }
        "MATH".encodeToByteArray().copyInto(font, 12)
        font[29] = 1
        validateMathFont(font)
        font[27] = 11
        assertFailsWith<IllegalArgumentException> { validateMathFont(font) }
        font[27] = 10
        font[29] = 2
        assertFailsWith<IllegalArgumentException> { validateMathFont(font) }
    }

    @Test
    fun ignoresCorruptMetadataAndRejectsUnknownSelection() {
        val settings = MapSettingsStore()
        val fonts = CustomMathFonts(Path("unused"), settings)
        settings.putString(CUSTOM_MATH_FONTS_KEY, "broken")
        assertTrue(fonts.list().isEmpty())
        settings.putString(CUSTOM_MATH_FONTS_KEY, """[{"id":"../../outside","name":"bad.otf"}]""")
        assertTrue(fonts.list().isEmpty())
        assertFailsWith<IllegalArgumentException> { fonts.select(ImportedMathFont("unknown", "unknown.otf")) }
    }
}
