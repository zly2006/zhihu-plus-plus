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

package com.chloemlla.zhplus

import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import com.chloemlla.zhplus.test.InstrumentedTestEnvironment

class ZhihuInstrumentedTestRunner : AndroidJUnitRunner() {
    override fun onCreate(arguments: Bundle) {
        if (!arguments.containsKey(TEST_TIMEOUT_ARGUMENT)) {
            arguments.putString(TEST_TIMEOUT_ARGUMENT, DEFAULT_TEST_TIMEOUT_MILLIS.toString())
        }

        val timingListenerName = InstrumentedTestTimingListener::class.java.name
        val listenerNames = arguments
            .getString(TEST_LISTENER_ARGUMENT)
            .orEmpty()
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toMutableSet()
        listenerNames += timingListenerName
        arguments.putString(TEST_LISTENER_ARGUMENT, listenerNames.joinToString(","))

        InstrumentedTestEnvironment.configureFromArguments(arguments)
        super.onCreate(arguments)
    }

    override fun onStart() {
        InstrumentedTestEnvironment.reseed(targetContext)
        super.onStart()
    }

    private companion object {
        const val TEST_TIMEOUT_ARGUMENT = "timeout_msec"
        const val TEST_LISTENER_ARGUMENT = "listener"
        const val DEFAULT_TEST_TIMEOUT_MILLIS = 30_000L
    }
}
