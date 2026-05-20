package com.github.zly2006.zhihu.shared.question

data class QuestionScreenUiState(
    val questionContent: String = "",
    val answerCount: Int = 0,
    val visitCount: Int = 0,
    val commentCount: Int = 0,
    val followerCount: Int = 0,
    val title: String = "",
    val isFollowing: Boolean = false,
    val isQuestionDetailExpanded: Boolean = true,
)
