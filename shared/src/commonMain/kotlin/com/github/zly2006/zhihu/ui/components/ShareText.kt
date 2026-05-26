package com.github.zly2006.zhihu.ui.components

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.zhihuAnswerUrl
import com.github.zly2006.zhihu.navigation.zhihuArticleUrl
import com.github.zly2006.zhihu.navigation.zhihuPinUrl
import com.github.zly2006.zhihu.navigation.zhihuQuestionUrl

fun getShareText(content: NavDestination, title: String = "", authorName: String = ""): String? = when (content) {
    is Article -> {
        when (content.type) {
            ArticleType.Answer -> {
                "${zhihuAnswerUrl(content.id)}\n【$title - $authorName 的回答】"
            }
            ArticleType.Article -> {
                "${zhihuArticleUrl(content.id)}\n【$title - $authorName 的文章】"
            }
        }
    }
    is Question -> {
        "${zhihuQuestionUrl(content.questionId)}\n【${content.title}】"
    }
    is Pin -> {
        zhihuPinUrl(content.id)
    }
    else -> null
}

fun getShareTitle(content: NavDestination): String = when (content) {
    is Article -> content.title + when (content.type) {
        ArticleType.Answer -> " - ${content.authorName} 的回答"
        ArticleType.Article -> " - ${content.authorName} 的文章"
    }
    is Question -> content.title
    else -> "分享内容"
}
