package com.github.zly2006.zhihu.shared.data

import com.github.zly2006.zhihu.navigation.Search
import kotlin.test.Test
import kotlin.test.assertEquals

class FeedNavigationTest {
    @Test
    fun feedDisplayItemRestoresExplicitNavigationJson() {
        val destination = Search(query = "fixture")
        val item = FeedDisplayItem(
            title = "标题",
            summary = "摘要",
            details = "详情",
            feed = null,
            navDestinationJson = destination.toFeedDisplayItemNavDestinationJson(),
        )

        assertEquals(destination, item.navDestination)
        assertEquals(destination.toFeedDisplayItemNavDestinationJson(), item.stableKey)
    }
}
