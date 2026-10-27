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

package com.github.zly2006.zhihu.platform

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopSettingsStoreTest {
    @Test
    fun fontSelectionIsVisibleAcrossStoresAndOtherSettingsDoNotOverwriteIt() {
        val previousHome = System.getProperty("user.home")
        val homeDirectory = Files.createTempDirectory("settings-store-test").toFile()
        try {
            System.setProperty("user.home", homeDirectory.absolutePath)
            val readingSettings = desktopSettingsStore()
            val fontSettings = desktopSettingsStore()
            val changes = mutableListOf<String>()
            val subscription = readingSettings.observeKeyChanges(changes::add)
            try {
                fontSettings.putString("selectedMathFont", "imported-font")
                assertEquals("imported-font", readingSettings.getStringOrNull("selectedMathFont"))
                assertEquals(listOf("selectedMathFont"), changes)
                readingSettings.putInt("contentFontSize", 120)
                assertEquals("imported-font", desktopSettingsStore().getStringOrNull("selectedMathFont"))
                val persisted = java.util.Properties().apply {
                    homeDirectory.resolve(".zhihu-plus/settings.properties").inputStream().use(::load)
                }
                assertEquals("imported-font", persisted.getProperty("selectedMathFont"))
                fontSettings.remove("selectedMathFont")
                assertEquals(listOf("selectedMathFont", "contentFontSize", "selectedMathFont"), changes)
            } finally {
                subscription.close()
            }
        } finally {
            System.setProperty("user.home", previousHome)
            homeDirectory.deleteRecursively()
        }
    }
}
