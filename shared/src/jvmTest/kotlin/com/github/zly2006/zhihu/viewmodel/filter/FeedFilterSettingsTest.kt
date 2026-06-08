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

package com.github.zly2006.zhihu.viewmodel.filter

import com.github.zly2006.zhihu.shared.platform.SettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedFilterSettingsTest {
    @Test
    fun readsFeedFilterSettingsFromSettingsStore() {
        val settings = mapBackedSettingsStore(
            "enableContentFilter" to false,
            "reverseBlock" to true,
            "filterFollowedUserContent" to true,
            "enableKeywordBlocking" to false,
            "enableNLPBlocking" to false,
            "nlpSimilarityThreshold" to 0.65f,
            "enableUserBlocking" to false,
            "enableTopicBlocking" to false,
            "topicBlockingThreshold" to 3,
            "blockZhihuAdPlatform" to false,
            "blockZhihuSchool" to false,
            "blockWeChatOfficialAccount" to false,
            "blockPaidContent" to false,
        ).toFeedFilterSettings()

        assertEquals(false, settings.enableContentFilter)
        assertEquals(true, settings.reverseBlock)
        assertEquals(true, settings.filterFollowedUserContent)
        assertEquals(false, settings.enableKeywordBlocking)
        assertEquals(false, settings.enableNlpBlocking)
        assertEquals(0.65, settings.nlpSimilarityThreshold, 0.0001)
        assertEquals(false, settings.enableUserBlocking)
        assertEquals(false, settings.enableTopicBlocking)
        assertEquals(3, settings.topicBlockingThreshold)
        assertEquals(false, settings.adBlockSettings.blockZhihuAdPlatform)
        assertEquals(false, settings.adBlockSettings.blockZhihuSchool)
        assertEquals(false, settings.adBlockSettings.blockWeChatOfficialAccount)
        assertEquals(false, settings.adBlockSettings.blockPaidContent)
    }

    private fun mapBackedSettingsStore(vararg values: Pair<String, Any>): SettingsStore {
        val map = values.toMap()
        return SettingsStore(
            getBoolean = { key, default -> map[key] as? Boolean ?: default },
            putBoolean = { _, _ -> },
            getString = { key, default -> map[key] as? String ?: default },
            putString = { _, _ -> },
            getStringOrNull = { key -> map[key] as? String },
            putStringSet = { _, _ -> },
            getStringSet = { key, default ->
                (map[key] as? Iterable<*>)?.filterIsInstance<String>()?.toSet() ?: default
            },
            getInt = { key, default -> map[key] as? Int ?: default },
            putInt = { _, _ -> },
            getLong = { key, default -> map[key] as? Long ?: default },
            putLong = { _, _ -> },
            getFloat = { key, default -> map[key] as? Float ?: default },
            putFloat = { _, _ -> },
            remove = { _ -> },
        )
    }
}
