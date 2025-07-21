package com.github.zly2006.zhihu.viewmodel.local

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/**
 * 爬虫执行器，负责执行爬虫任务并生成结果
 */
class CrawlingExecutor(private val context: Context) {
    private val database by lazy { LocalContentDatabase.getDatabase(context) }
    private val dao by lazy { database.contentDao() }

    /**
     * 执行爬虫任务
     */
    suspend fun executeTask(task: CrawlingTask): CrawlingResult? {
        return withContext(Dispatchers.IO) {
            try {
                // 更新任务状态为进行中
                dao.updateTask(task.copy(status = CrawlingStatus.InProgress, lastCrawled = System.currentTimeMillis()))

                // 模拟爬虫执行过程
                val result = simulateCrawling(task)

                // 保存爬虫结果
                dao.insertResult(result)

                // 更新任务状态为完成
                dao.updateTask(task.copy(status = CrawlingStatus.Completed))

                result
            } catch (e: Exception) {
                // 更新任务状态为失败
                dao.updateTask(task.copy(status = CrawlingStatus.Failed, errorMessage = e.message))
                null
            }
        }
    }

    /**
     * 模拟爬虫过程，根据URL和原因生成相应的内容
     */
    private fun simulateCrawling(task: CrawlingTask): CrawlingResult {
        val contentTemplates = getContentTemplatesByReason(task.reason)
        val randomTemplate = contentTemplates.random()

        return CrawlingResult(
            id = Random.nextInt(100000, 999999),
            taskId = task.id,
            title = randomTemplate.title,
            content = randomTemplate.content,
            summary = randomTemplate.summary,
            authorName = generateRandomAuthor(),
            url = task.url
        )
    }

    private fun getContentTemplatesByReason(reason: CrawlingReason?): List<ContentTemplate> {
        return when (reason) {
            CrawlingReason.Following -> listOf(
                ContentTemplate(
                    "技术分享：Android性能优化实战经验",
                    "本文详细介绍了Android应用性能优化的各个方面，包括内存管理、布局优化、网络请求优化等。通过实际案例分析，展示了如何识别性能瓶颈并进行针对性优化。",
                    "从内存泄漏检测到布局层级优化，从网络缓存策略到代码混淆，全面覆盖Android性能优化的方方面面。"
                ),
                ContentTemplate(
                    "职场成长：从初级到高级开发者的进阶之路",
                    "分享个人从初级开发者成长为技术专家的经历，包括技能提升、项目经验积累、团队协作等方面的心得体会。",
                    "技术成长不仅仅是代码能力的提升，更需要培养系统性思维、沟通协调能力和持续学习的习惯。"
                ),
                ContentTemplate(
                    "开发工具：提升编程效率的神器推荐",
                    "推荐一系列能显著提升开发效率的工具和插件，包括IDE配置、版本控制、自动化部署等方面的最佳实践。",
                    "工欲善其事，必先利其器。合适的工具能让开发工作事半功倍，大幅提升编程体验和效率。"
                )
            )
            CrawlingReason.Trending -> listOf(
                ContentTemplate(
                    "AI发展趋势：大模型技术的现状与未来",
                    "深度分析当前AI大模型的技术发展现状，探讨GPT、BERT等模型的应用场景，以及AI技术对各行业的影响。",
                    "人工智能正在重塑我们的工作和生活方式，了解AI发展趋势有助于把握未来机遇。"
                ),
                ContentTemplate(
                    "远程办公：数字化时代的工作新模式",
                    "分析远程办公模式的优势与挑战，分享高效远程协作的工具和方法，探讨未来工作模式的发展趋势。",
                    "疫情加速了远程办公的普及，如何在分布式团队中保持高效协作成为重要课题。"
                ),
                ContentTemplate(
                    "新能源革命：可持续发展的技术创新",
                    "探讨太阳能、风能、电池技术等新能源领域的最新进展，分析清洁能源对环境和经济的积极影响。",
                    "能源转型是应对气候变化的关键，新能源技术的快速发展为可持续未来带来希望。"
                )
            )
            CrawlingReason.FollowingUpvote -> listOf(
                ContentTemplate(
                    "读书心得：《深度工作》的实践应用",
                    "分享阅读《深度工作》后的实践心得，介绍如何在信息爆炸的时代保持专注，提升工作和学习效率。",
                    "深度工作能力是知识工作者的核心竞争力，需要刻意练习和持续培养。"
                ),
                ContentTemplate(
                    "健康生活：程序员的身心健康指南",
                    "针对程序员群体的健康问题，提供科学的运动建议、营养搭配和心理调节方法，帮助维护身心健康。",
                    "长期的久坐工作对身体健康造成不小挑战，合理的运动和作息安排至关重要。"
                ),
                ContentTemplate(
                    "投资理财：年轻人的财富管理入门",
                    "为初入职场的年轻人提供基础的理财知识，包括预算规划、投资组合、风险管理等方面的实用建议。",
                    "理财越早开始越好，复利的力量需要时间来显现，年轻时的理财规划影响一生。"
                )
            )
            CrawlingReason.UpvotedQuestion -> listOf(
                ContentTemplate(
                    "技术选型：如何选择合适的开发框架",
                    "详细分析不同开发框架的特点和适用场景，提供技术选型的决策框架和评估标准。",
                    "技术选型需要综合考虑项目需求、团队能力、维护成本等多个因素，没有银弹解决方案。"
                ),
                ContentTemplate(
                    "架构设计：微服务架构的实践经验",
                    "分享微服务架构的设计原则和实践经验，包括服务拆分、数据一致性、监控告警等关键问题的解决方案。",
                    "微服务架构能提升系统的可扩展性和可维护性，但也带来了分布式系统的复杂性挑战。"
                ),
                ContentTemplate(
                    "代码质量：编写可维护代码的最佳实践",
                    "总结编写高质量代码的经验和技巧，包括命名规范、函数设计、注释编写等方面的最佳实践。",
                    "代码的可读性和可维护性比性能优化更重要，好的代码是给人看的，顺便能在机器上运行。"
                )
            )
            CrawlingReason.CollaborativeFiltering -> listOf(
                ContentTemplate(
                    "学习方法：如何高效学习新技术",
                    "分享个人学习新技术的方法论，包括信息筛选、实践项目、知识体系构建等方面的经验。",
                    "技术更新换代很快，掌握高效的学习方法比学会具体技术更重要。"
                ),
                ContentTemplate(
                    "团队协作：敏捷开发的实践心得",
                    "分享在敏捷开发实践中的经验教训，包括需求管理、迭代计划、团队沟通等方面的最佳实践。",
                    "敏捷不仅仅是一套流程，更是一种思维方式，需要团队成员的深度参与和持续改进。"
                )
            )
            else -> listOf(
                ContentTemplate(
                    "知识分享：持续学习的重要性",
                    "在快速变化的时代，持续学习已成为个人发展的必备能力。本文探讨如何建立有效的学习体系。",
                    "终身学习不是口号，而是现代人必须具备的基本素养。"
                )
            )
        }
    }

    private fun generateRandomAuthor(): String {
        val authors = listOf(
            "技术小白", "代码达人", "架构师老王", "产品经理小李", "设计师小美",
            "算法工程师", "前端开发者", "后端专家", "移动开发", "全栈工程师",
            "数据分析师", "运维工程师", "测试工程师", "技术总监", "创业者"
        )
        return authors.random()
    }

    private data class ContentTemplate(
        val title: String,
        val content: String,
        val summary: String
    )
}
