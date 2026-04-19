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

package com.github.zly2006.zhihu.test

import android.content.Context
import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Person
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME

object InstrumentedTestEnvironment {
    const val DATA_MODE_ARG = "zhpp_data_mode"
    private const val SECRET_ACCOUNT_ASSET_PATH = "secret/account.json"

    enum class DataMode {
        MOCK,
        REAL,
    }

    private var currentDataMode: DataMode = DataMode.MOCK

    fun configureFromArguments(arguments: Bundle?) {
        currentDataMode = when (arguments?.getString(DATA_MODE_ARG)?.trim()?.lowercase()) {
            "real" -> DataMode.REAL
            else -> DataMode.MOCK
        }
        ZhihuMockApi.install(enabled = currentDataMode == DataMode.MOCK)
    }

    fun isMockMode(): Boolean = currentDataMode == DataMode.MOCK

    fun reseed(context: Context, assetContext: Context = InstrumentationRegistry.getInstrumentation().context) {
        context
            .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .putBoolean("allowTelemetry", false)
            .putLong("last_main_launch_timestamp", System.currentTimeMillis())
            .commit()

        when (currentDataMode) {
            DataMode.MOCK -> {
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
                ZhihuMockApi.reset()
            }

            DataMode.REAL -> {
                val secretAccountData = loadRealAccountData(assetContext)
                AccountData.saveData(
                    context,
                    secretAccountData.copy(
                        login = secretAccountData.login || secretAccountData.cookies.isNotEmpty(),
                        userAgent = secretAccountData.userAgent.ifBlank { AccountData.ANDROID_USER_AGENT },
                    ),
                )
                ZhihuMockApi.reset()
            }
        }
    }

    private fun loadRealAccountData(context: Context): AccountData.Data = try {
        val json = context.assets
            .open(SECRET_ACCOUNT_ASSET_PATH)
            .bufferedReader()
            .use { it.readText() }
        AccountData.json.decodeFromString(AccountData.Data.serializer(), json)
    } catch (e: Exception) {
        throw IllegalStateException(
            "Real-data mode requires project-root .secret/account.json packaged into androidTest assets as $SECRET_ACCOUNT_ASSET_PATH",
            e,
        )
    }
}
