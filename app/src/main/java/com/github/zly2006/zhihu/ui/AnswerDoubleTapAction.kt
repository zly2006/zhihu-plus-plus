package com.github.zly2006.zhihu.ui

const val ANSWER_DOUBLE_TAP_ACTION_PREFERENCE_KEY = "answerDoubleTapAction"

enum class AnswerDoubleTapAction(
    val preferenceValue: String,
    val label: String,
) {
    None(
        preferenceValue = "none",
        label = "无操作",
    ),
    Ask(
        preferenceValue = "ask",
        label = "弹窗询问",
    ),
    VoteUp(
        preferenceValue = "voteUp",
        label = "点赞",
    ),
    OpenComments(
        preferenceValue = "openComments",
        label = "打开评论区",
    ),
    ;

    companion object {
        fun fromPreference(value: String?): AnswerDoubleTapAction = entries.firstOrNull {
            it.preferenceValue == value
        } ?: Ask
    }
}
