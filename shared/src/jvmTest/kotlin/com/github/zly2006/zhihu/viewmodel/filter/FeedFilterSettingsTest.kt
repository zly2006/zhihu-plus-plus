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
