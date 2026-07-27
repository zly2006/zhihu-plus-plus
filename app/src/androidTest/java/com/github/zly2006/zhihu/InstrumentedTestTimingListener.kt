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

package com.github.zly2006.zhihu

import android.os.SystemClock
import android.util.Log
import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunListener
import java.util.concurrent.ConcurrentHashMap

class InstrumentedTestTimingListener : RunListener() {
    private val startedAtMillis = ConcurrentHashMap<Description, Long>()
    private val failedTests = ConcurrentHashMap.newKeySet<Description>()

    override fun testStarted(description: Description) {
        startedAtMillis[description] = SystemClock.elapsedRealtime()
        Log.i(TEST_LOG_TAG, "START ${description.displayName}")
    }

    override fun testFailure(failure: Failure) {
        failedTests += failure.description
    }

    override fun testFinished(description: Description) {
        val elapsedMillis = SystemClock.elapsedRealtime() - (startedAtMillis.remove(description) ?: return)
        val result = if (failedTests.remove(description)) "FAIL" else "PASS"
        Log.i(TEST_LOG_TAG, "$result ${description.displayName} elapsed=${elapsedMillis}ms")
    }

    override fun testIgnored(description: Description) {
        Log.i(TEST_LOG_TAG, "SKIP ${description.displayName}")
    }

    private companion object {
        const val TEST_LOG_TAG = "ZHPP_TEST"
    }
}
