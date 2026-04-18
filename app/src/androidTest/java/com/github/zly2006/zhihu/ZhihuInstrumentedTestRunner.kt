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

import androidx.test.runner.AndroidJUnitRunner
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Person
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME

class ZhihuInstrumentedTestRunner : AndroidJUnitRunner() {
    override fun onStart() {
        seedStableInstrumentedTestState()
        super.onStart()
    }

    private fun seedStableInstrumentedTestState() {
        val context = targetContext
        context
            .getSharedPreferences(PREFERENCE_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean("allowTelemetry", false)
            .putLong("last_main_launch_timestamp", System.currentTimeMillis())
            .commit()

        AccountData.saveData(
            context,
            AccountData.Data(
                login = true,
                username = "AndroidTestUser",
                cookies = mutableMapOf(
                    "z_c0" to "android-test-zc0",
                    "d_c0" to "android-test-dc0",
                    "_xsrf" to "android-test-xsrf",
                ),
                userAgent = AccountData.ANDROID_USER_AGENT,
                self = Person(
                    id = "android-test-user-id",
                    url = "https://www.zhihu.com/people/android-test-user",
                    userType = "people",
                    urlToken = "android-test-user",
                    name = "AndroidTestUser",
                    headline = "androidTest seeded login state",
                    avatarUrl = "",
                ),
            ),
        )
    }
}
