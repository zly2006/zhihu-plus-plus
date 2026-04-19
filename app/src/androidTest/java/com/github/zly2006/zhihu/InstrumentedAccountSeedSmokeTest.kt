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

package com.github.zly2006.zhihu

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.test.InstrumentedTestEnvironment
import com.github.zly2006.zhihu.test.ZhihuMockApi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstrumentedAccountSeedSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun runnerSeedsExpectedAccountModeBeforeActivityInteractions() {
        val seeded = AccountData.data
        if (InstrumentedTestEnvironment.isMockMode()) {
            assertTrue(ZhihuMockApi.isEnabled())
            assertTrue(seeded.login)
            assertTrue(seeded.cookies["z_c0"] == "android-test-zc0")
        } else {
            assertFalse(ZhihuMockApi.isEnabled())
            assertTrue(seeded.login)
            assertTrue(seeded.cookies.isNotEmpty())
            assertTrue(seeded.cookies["z_c0"]?.isNotBlank() == true)
        }
    }
}
