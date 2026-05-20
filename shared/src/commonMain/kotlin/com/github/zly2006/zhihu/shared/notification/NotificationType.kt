package com.github.zly2006.zhihu.shared.notification

enum class NotificationType(
    val displayName: String,
    val defaultValue: Boolean,
    val regex: Regex,
) {
    LIKE_ANSWER("喜欢了你的回答", true, Regex("喜欢了你的回答")),
    LIKE_COMMENT("喜欢了你的评论", true, Regex("喜欢了.*你的评论")),
    REPLY_COMMENT("回复了你的评论", true, Regex("回复了.*你的评论")),
    INVITE_ANSWER("邀请你回答问题", false, Regex("\\s?(邀请你回答问题|的提问等你来答|邀请你回答)")),
}

fun matchNotificationType(verb: String): NotificationType? =
    NotificationType.entries.find { it.regex.matches(verb) }
