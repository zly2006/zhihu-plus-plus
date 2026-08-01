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

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.chloemlla.zhplus.navigation.Article
import com.chloemlla.zhplus.navigation.ArticleType
import com.chloemlla.zhplus.navigation.CommentHolder
import com.chloemlla.zhplus.navigation.Notification
import com.chloemlla.zhplus.navigation.Pin
import com.chloemlla.zhplus.navigation.Question
import com.chloemlla.zhplus.navigation.resolveContent
import com.chloemlla.zhplus.shared.data.MobileNotificationContent
import com.chloemlla.zhplus.shared.data.MobileNotificationTimelineItem
import com.chloemlla.zhplus.test.MainActivityComposeRule
import com.chloemlla.zhplus.test.RecordingNavigator
import com.chloemlla.zhplus.test.resetAppPreferences
import com.chloemlla.zhplus.test.setScreenContent
import com.chloemlla.zhplus.ui.NotificationScreen
import com.chloemlla.zhplus.viewmodel.MobileNotificationCategory
import com.chloemlla.zhplus.viewmodel.NotificationViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
    }

    @Test
    fun notificationScreen_showsStableToolbarActionsWithoutLiveData() {
        /*
         * Expected behavior:
         * 1. The test preloads one local notification before composing the screen so NotificationScreen
         *    does not need to fetch live notification data just to render its scaffold.
         * 2. The toolbar should always show the page title plus clickable back and settings actions.
         * 3. The "mark all as read" action should stay hidden while unreadCount remains at its default zero.
         */
        setNotificationScreenContent()

        composeRule.onNodeWithText("消息").assertIsDisplayed()
        MobileNotificationCategory.entries.forEach { category ->
            composeRule
                .onNodeWithTag("notification_category_${category.entryName}")
                .assertExists()
                .assertHasClickAction()
        }
        composeRule.onNodeWithContentDescription("返回").assertExists().assertHasClickAction()
        composeRule.onNodeWithContentDescription("设置").assertExists().assertHasClickAction()
        composeRule.onNodeWithContentDescription("已读").assertDoesNotExist()
    }

    @Test
    fun notificationScreen_backButton_delegatesToNavigatorBackCallback() {
        /*
         * Expected behavior:
         * 1. Pressing the toolbar back button should invoke the injected navigator back callback exactly once.
         * 2. The screen should not record any forward navigation destination when the user only requests back.
         * 3. This interaction must remain deterministic even when the notification list itself is seeded locally.
         */
        val recordingNavigator = setNotificationScreenContent()

        composeRule.onNodeWithContentDescription("返回").performClick()

        assertEquals(1, recordingNavigator.backCount)
        assertTrue(recordingNavigator.destinations.isEmpty())
    }

    @Test
    fun notificationScreen_settingsButton_navigatesToNotificationSettings() {
        /*
         * Expected behavior:
         * 1. Pressing the toolbar settings button should navigate to Notification.NotificationSettings.
         * 2. This action should not trigger a back event because it is a forward navigation path.
         * 3. The recorded destination list should contain exactly the settings destination after one click.
         */
        val recordingNavigator = setNotificationScreenContent()

        composeRule.onNodeWithContentDescription("设置").performClick()

        assertEquals(0, recordingNavigator.backCount)
        assertEquals(listOf(Notification.NotificationSettings), recordingNavigator.destinations)
    }

    @Test
    fun notificationScreen_showsCategoryUnreadCountBadge() {
        /*
         * Expected behavior:
         * 1. The test preloads per-category unread counts into the screen ViewModel.
         * 2. The top category row should render that count as a visible badge on the matching category.
         * 3. The badge should be part of the category button, not a separate toolbar count.
         */
        composeRule.seedNotificationViewModel(
            unreadCounts = mapOf(MobileNotificationCategory.Like to 2),
        )
        composeRule.setScreenContent {
            NotificationScreen()
        }

        composeRule.onNodeWithText("2").assertIsDisplayed()
    }

    @Test
    fun notificationScreen_fourObservedCommentActionsNavigateWithCommentAnchors() {
        val notifications = listOf(
            notificationFixture(
                id = "comment-content",
                title = "别人评论我的内容",
                subTitle = "评论了你的回答",
                targetLink = "zhihu://comment/list/answer/2?anchor_comment_id=3&is_child=false",
            ),
            notificationFixture(
                id = "reply-comment",
                title = "别人回复我的评论",
                subTitle = "回复了想法下你的评论",
                targetLink = "zhihu://comment/list/pin/4?anchor_comment_id=5&is_child=true",
            ),
            notificationFixture(
                id = "like-root-comment",
                title = "别人点赞我的根评论",
                subTitle = "喜欢了你的评论",
                targetLink = "zhihu://comment/list/article/6?anchor_comment_id=7&is_child=false",
            ),
            notificationFixture(
                id = "like-child-comment",
                title = "别人点赞我的楼中楼评论",
                subTitle = "喜欢了你的评论",
                targetLink = "zhihu://comment/list/pin/8?anchor_comment_id=9&is_child=false",
            ),
        )
        val recordingNavigator = setNotificationScreenContent(notifications)

        notifications.forEach { notification ->
            composeRule.onNodeWithText(notification.content!!.title).performClick()
        }

        assertEquals(4, recordingNavigator.destinations.size)
        val commentedAnswerHolder = recordingNavigator.destinations[0] as CommentHolder
        assertEquals("3", commentedAnswerHolder.commentId)
        val commentedAnswer = commentedAnswerHolder.article as Article
        assertEquals(ArticleType.Answer, commentedAnswer.type)
        assertEquals(2L, commentedAnswer.id)
        val repliedPinHolder = recordingNavigator.destinations[1] as CommentHolder
        assertEquals("5", repliedPinHolder.commentId)
        val repliedPin = repliedPinHolder.article as Pin
        assertEquals(4L, repliedPin.id)
        val likedArticleCommentHolder = recordingNavigator.destinations[2] as CommentHolder
        assertEquals("7", likedArticleCommentHolder.commentId)
        val likedArticleComment = likedArticleCommentHolder.article as Article
        assertEquals(ArticleType.Article, likedArticleComment.type)
        assertEquals(6L, likedArticleComment.id)
        val likedChildPinCommentHolder = recordingNavigator.destinations[3] as CommentHolder
        assertEquals("9", likedChildPinCommentHolder.commentId)
        val likedChildPinComment = likedChildPinCommentHolder.article as Pin
        assertEquals(8L, likedChildPinComment.id)
    }

    @Test
    fun realAccountCommentLinkSnapshot_all212OccurrencesResolveToTheirAnchors() {
        // 2026-07-30 从当前账号 comment/like 通知各取两页后，保留结构与数量，ID 全部替换为测试值。
        val fixtures = buildList {
            val groups = listOf(
                ObservedCommentLinkGroup("pin", occurrences = 163, uniqueLinks = 163),
                ObservedCommentLinkGroup("answer", occurrences = 22, uniqueLinks = 22),
                ObservedCommentLinkGroup("article", occurrences = 4, uniqueLinks = 4),
                ObservedCommentLinkGroup("pin", occurrences = 1, uniqueLinks = 1, isChild = true),
                ObservedCommentLinkGroup("question", occurrences = 1, uniqueLinks = 1),
                // like 分区有 20 次跳转，聚合到 7 条唯一链接：3 条根评论、4 条楼中楼评论。
                ObservedCommentLinkGroup("answer", occurrences = 5, uniqueLinks = 3),
                ObservedCommentLinkGroup("article", occurrences = 1, uniqueLinks = 1),
                ObservedCommentLinkGroup("pin", occurrences = 14, uniqueLinks = 3),
            )
            groups.forEachIndexed { groupIndex, group ->
                repeat(group.occurrences) { occurrence ->
                    val uniqueIndex = occurrence % group.uniqueLinks
                    val contentId = 100_000L + groupIndex * 10_000L + uniqueIndex
                    val anchorId = (1_000_000L + groupIndex * 10_000L + uniqueIndex).toString()
                    add(
                        ObservedCommentLink(
                            url = "zhihu://comment/list/${group.contentType}/$contentId?anchor_comment_id=$anchorId&is_child=${group.isChild}",
                            contentType = group.contentType,
                            contentId = contentId,
                            anchorId = anchorId,
                        ),
                    )
                }
            }
            add(
                ObservedCommentLink(
                    url = "zhihu://comment/list/answer/900000?anchor_comment_id=1900000&list_height_ratio=0.66&dragIconVisible=true&segment=%7B%22id%22%3A1%7D",
                    contentType = "answer",
                    contentId = 900_000L,
                    anchorId = "1900000",
                ),
            )
        }

        assertEquals(212, fixtures.size)
        assertEquals(199, fixtures.map { it.url }.distinct().size)
        fixtures.forEach { fixture ->
            val holder = resolveContent(fixture.url) as? CommentHolder
                ?: throw AssertionError("无法解析真实评论跳转结构：${fixture.url}")
            assertEquals(fixture.anchorId, holder.commentId)
            when (val destination = holder.article) {
                is Article -> {
                    assertEquals(fixture.contentType, destination.type.toString())
                    assertEquals(fixture.contentId, destination.id)
                }

                is Pin -> {
                    assertEquals("pin", fixture.contentType)
                    assertEquals(fixture.contentId, destination.id)
                }

                is Question -> {
                    assertEquals("question", fixture.contentType)
                    assertEquals(fixture.contentId, destination.questionId)
                }

                else -> throw AssertionError("无法解析真实评论跳转结构：${fixture.url}")
            }
        }
    }

    private fun setNotificationScreenContent(
        notifications: List<MobileNotificationTimelineItem> = listOf(notificationFixture()),
    ): RecordingNavigator {
        composeRule.seedNotificationViewModel(notifications = notifications)
        return composeRule.setScreenContent {
            NotificationScreen()
        }
    }

    private fun notificationFixture(
        id: String = "local-notification",
        title: String = "测试用户 回复了回答下你的评论",
        subTitle: String = "评论和回复",
        targetLink: String = "zhihu://comment/list/answer/2?anchor_comment_id=3&is_child=false",
    ) = MobileNotificationTimelineItem(
        id = id,
        type = "aggregate_notification",
        isRead = true,
        created = 1_713_420_000L,
        content = MobileNotificationContent(
            title = title,
            subTitle = subTitle,
            targetLink = targetLink,
        ),
    )

    private fun MainActivityComposeRule.seedNotificationViewModel(
        unreadCounts: Map<MobileNotificationCategory, Int> = emptyMap(),
        notifications: List<MobileNotificationTimelineItem> = listOf(notificationFixture()),
    ) {
        activity.runOnUiThread {
            val viewModel = ViewModelProvider(activity)[NotificationViewModel::class.java]
            viewModel.allData.clear()
            viewModel.allData += notifications
            if (unreadCounts.isNotEmpty()) {
                viewModel.categoryUnreadCounts.putAll(unreadCounts)
            }
        }
        waitForIdle()
    }

    private data class ObservedCommentLink(
        val url: String,
        val contentType: String,
        val contentId: Long,
        val anchorId: String,
    )

    private data class ObservedCommentLinkGroup(
        val contentType: String,
        val occurrences: Int,
        val uniqueLinks: Int,
        val isChild: Boolean = false,
    )
}
