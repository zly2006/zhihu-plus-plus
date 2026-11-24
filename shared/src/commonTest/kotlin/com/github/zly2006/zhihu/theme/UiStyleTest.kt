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

package com.github.zly2006.zhihu.theme

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UiStyleTest {
    @AfterTest
    fun tearDown() {
        ThemeManager.load(ThemeSnapshot())
    }

    @Test
    fun fromValueOrDefault_acceptsPrefAndEnumNames() {
        assertEquals(UiStyle.Material3, UiStyle.fromValueOrDefault(null))
        assertEquals(UiStyle.Material3, UiStyle.fromValueOrDefault(""))
        assertEquals(UiStyle.Material3, UiStyle.fromValueOrDefault("unknown"))
        assertEquals(UiStyle.Material3, UiStyle.fromValueOrDefault("material"))
        assertEquals(UiStyle.Material3, UiStyle.fromValueOrDefault("Material3"))
        assertEquals(UiStyle.Miuix, UiStyle.fromValueOrDefault("miuix"))
        assertEquals(UiStyle.Miuix, UiStyle.fromValueOrDefault("Miuix"))
        assertEquals(UiStyle.LiquidGlass, UiStyle.fromValueOrDefault("liquid_glass"))
        assertEquals(UiStyle.LiquidGlass, UiStyle.fromValueOrDefault("LiquidGlass"))
    }

    @Test
    fun resolveEffective_fallsBackWhenUnsupported() {
        assertEquals(UiStyle.Material3, UiStyle.Material3.resolveEffective())
        assertEquals(UiStyle.Miuix, UiStyle.Miuix.resolveEffective())
        val expectedLiquid = if (isLiquidGlassSupported) UiStyle.LiquidGlass else UiStyle.Material3
        assertEquals(expectedLiquid, UiStyle.LiquidGlass.resolveEffective())
    }

    @Test
    fun themeManager_persistsUiStyleInSnapshot() {
        ThemeManager.load(ThemeSnapshot(uiStyle = UiStyle.Miuix))
        assertEquals(UiStyle.Miuix, ThemeManager.snapshot().uiStyle)

        ThemeManager.setUiStyle(UiStyle.LiquidGlass)
        assertEquals(UiStyle.LiquidGlass, ThemeManager.snapshot().uiStyle)
        assertEquals(UiStyle.LiquidGlass, ThemeManager.currentUiStyle())
    }
}
