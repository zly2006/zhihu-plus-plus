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

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.filter.ContentOpenEvent
import com.github.zly2006.zhihu.viewmodel.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.viewmodel.filter.ContentOpenFrom
import com.github.zly2006.zhihu.viewmodel.filter.ContentType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContentOpenEventSupportInstrumentedTest {
    @Test
    fun getAlreadyOpenedContentIds_recognizesOpenedContentEvents() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = ContentFilterDatabase.getDatabase(context)
        val contentId = "opened-${System.currentTimeMillis()}"

        database.contentOpenEventDao().insert(
            ContentOpenEvent(
                contentType = ContentType.ANSWER,
                contentId = contentId,
                questionId = 42L,
                openFrom = ContentOpenFrom.HOME_FEED,
            ),
        )

        val viewedIds = ContentOpenEventSupport.getAlreadyOpenedContentIds(
            context = context,
            content = listOf(ContentType.ANSWER to contentId),
        )

        assertTrue(
            viewedIds.contains(ContentOpenEventSupport.buildContentKey(ContentType.ANSWER, contentId)),
        )
    }
}
