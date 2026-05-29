package com.github.zly2006.zhihu.ui.components

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question

fun getShareText(content: NavDestination, title: String = "", authorName: String = ""): String? = when (content) {
    is Article -> {
        when (content.type) {
            ArticleType.Answer -> {
                "https://www.zhihu.com/answer/${content.id}\n【$title - $authorName 的回答】"
            }
            ArticleType.Article -> {
                "https://zhuanlan.zhihu.com/p/${content.id}\n【$title - $authorName 的文章】"
            }
        }
    }
    is Question -> {
        "https://www.zhihu.com/question/${content.questionId}\n【${content.title}】"
    }
    is Pin -> {
        "https://www.zhihu.com/pin/${content.id}"
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
