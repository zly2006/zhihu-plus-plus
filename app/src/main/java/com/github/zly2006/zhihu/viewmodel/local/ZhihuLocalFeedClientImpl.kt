package com.github.zly2006.zhihu.viewmodel.local

import com.github.zly2006.zhihu.Person
import com.github.zly2006.zhihu.Question
import kotlin.random.Random

class ZhihuLocalFeedClientImpl : ZhihuLocalFeedClient {

    override fun pickAnswers(question: Question, offset: Int): List<CrawlingTask> {
        // 生成问题回答相关的爬虫任务
        return generateTasksForReason(
            count = 10,
            reason = CrawlingReason.UpvotedQuestion,
            baseUrl = "https://www.zhihu.com/question/${question.questionId}/answer/"
        )
    }

    override fun pickAuthorWorks(author: Person, offset: Int): List<CrawlingTask> {
        // 生成作者作品相关的爬虫任务
        return generateTasksForReason(
            count = 15,
            reason = CrawlingReason.Following,
            baseUrl = "https://www.zhihu.com/people/${author.urlToken}/posts/"
        )
    }

    override fun pickColumnArticles(column: Person, offset: Int): List<CrawlingTask> {
        // 生成专栏文章相关的爬虫任务
        return generateTasksForReason(
            count = 12,
            reason = CrawlingReason.Trending,
            baseUrl = "https://zhuanlan.zhihu.com/p/"
        )
    }

    override fun pickAuthorFollowing(author: Person, offset: Int): List<CrawlingTask> {
        // 生成关注用户相关的爬虫任务
        return generateTasksForReason(
            count = 8,
            reason = CrawlingReason.CollaborativeFiltering,
            baseUrl = "https://www.zhihu.com/people/${author.urlToken}/following/"
        )
    }

    private fun generateTasksForReason(
        count: Int,
        reason: CrawlingReason,
        baseUrl: String
    ): List<CrawlingTask> {
        return (1..count).map { i ->
            CrawlingTask(
                id = Random.nextInt(100000, 999999),
                url = "$baseUrl${Random.nextInt(1000000, 9999999)}",
                status = CrawlingStatus.NotStarted,
                reason = reason,
                lastCrawled = 0L
            )
        }
    }
}
