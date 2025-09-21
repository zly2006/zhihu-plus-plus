package com.github.zly2006.zhihu.desktop

import com.github.zly2006.zhihu.local.engine.LocalRecommendationEngine
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
import java.util.Scanner

class ZhihuDesktopApp {
    private val recommendationEngine: LocalRecommendationEngine by lazy {
        LocalRecommendationEngine(databasePath)
    }
    private val scanner = Scanner(System.`in`)
    private lateinit var databasePath: String

    suspend fun run(args: Array<String>) {
        val parser = ArgParser("zhihu-local")
        val databasePathOption by parser
            .option(
                ArgType.String,
                shortName = "d",
                description = "Database file path",
            ).default("./zhihu_local.db")

        val mode by parser
            .option(
                ArgType.Choice<AppMode>(),
                shortName = "m",
                description = "Application mode",
            ).default(AppMode.INTERACTIVE)

        parser.parse(args)

        this.databasePath = databasePathOption

        // 初始化推荐引擎
        println("🚀 初始化知乎本地推荐引擎...")
        recommendationEngine.initialize()

        println("✅ 初始化完成!")
        println("📊 数据库路径: $databasePath")
        println()

        when (mode) {
            AppMode.INTERACTIVE -> runInteractiveMode()
            AppMode.DAEMON -> runDaemonMode()
            AppMode.GENERATE_ONCE -> generateOnce()
        }
    }

    private suspend fun runInteractiveMode() {
        println("=== 知乎本地推荐系统 - 交互模式 ===")
        println()

        while (true) {
            showMenu()
            print("请选择操作 (1-6): ")

            when (scanner.nextLine().trim()) {
                "1" -> generateRecommendations()
                "2" -> showStatistics()
                "3" -> recordFeedback()
                "4" -> showConfiguration()
                "5" -> runCleanup()
                "6" -> {
                    println("👋 感谢使用，再见!")
                    break
                }
                else -> println("❌ 无效选择，请重试")
            }
            println()
        }

        recommendationEngine.cleanup()
    }

    private suspend fun runDaemonMode() {
        println("🔄 启动守护进程模式...")

        // 启动推荐流
        val job = CoroutineScope(Dispatchers.Default).launch {
            recommendationEngine.getRecommendationStream().collect { feeds ->
                println("\n📅 ${Date()}: 生成了 ${feeds.size} 条推荐")
                feeds.take(3).forEach { feed ->
                    println("  📖 ${feed.title}")
                }
            }
        }

        println("✅ 守护进程已启动，按回车键停止...")
        scanner.nextLine()

        job.cancel()
        recommendationEngine.cleanup()
        println("⏹️ 守护进程已停止")
    }

    private suspend fun generateOnce() {
        println("🎯 生成一次性推荐...")
        val feeds = recommendationEngine.generateRecommendations(10)

        if (feeds.isEmpty()) {
            println("😔 暂无推荐内容，可能需要等待数据收集完成")
        } else {
            println("📋 为您生成了 ${feeds.size} 条推荐:")
            println("=" * 50)

            feeds.forEachIndexed { index, feed ->
                println("${index + 1}. ${feed.title}")
                println("   📝 ${feed.summary}")
                println("   🏷️ ${feed.reasonDisplay}")
                if (feed.navDestination != null) {
                    println("   🔗 ${feed.navDestination}")
                }
                println()
            }
        }

        recommendationEngine.cleanup()
    }

    private fun showMenu() {
        println("=" * 40)
        println("📋 主菜单")
        println("=" * 40)
        println("1. 📖 生成推荐内容")
        println("2. 📊 查看统计信息")
        println("3. 👍 记录用户反馈")
        println("4. ⚙️ 查看配置信息")
        println("5. 🧹 清理旧数据")
        println("6. 🚪 退出程序")
        println("=" * 40)
    }

    private suspend fun generateRecommendations() {
        print("请输入推荐数量 (默认20): ")
        val input = scanner.nextLine().trim()
        val limit = if (input.isEmpty()) 20 else input.toIntOrNull() ?: 20

        println("⏳ 正在生成推荐...")
        val feeds = recommendationEngine.generateRecommendations(limit)

        if (feeds.isEmpty()) {
            println("😔 暂无推荐内容")
            println("💡 提示: 系统可能正在收集数据，请稍后再试")
        } else {
            println("✅ 为您生成了 ${feeds.size} 条推荐:")
            println()

            feeds.forEachIndexed { index, feed ->
                println("${index + 1}. ${feed.title}")
                println("   📝 ${feed.summary}")
                println("   🏷️ ${feed.reasonDisplay}")
                println("   ⭐ 用户反馈: ${String.format(Locale.getDefault(), "%.1f", feed.userFeedback)}")
                if (feed.navDestination != null) {
                    println("   🔗 ${feed.navDestination}")
                }
                println()
            }
        }
    }

    private suspend fun showStatistics() {
        println("📊 系统统计信息")
        println("-" * 30)

        // 这里可以添加更多统计信息
        try {
            val recentFeeds = recommendationEngine.generateRecommendations(1)
            if (recentFeeds.isNotEmpty()) {
                println("✅ 系统正常运行")
                println("📈 最近推荐生成时间: ${Date(recentFeeds.first().createdAt)}")
            } else {
                println("⚠️ 暂无推荐数据")
            }
        } catch (e: Exception) {
            println("❌ 获取统计信息失败: ${e.message}")
        }
    }

    private suspend fun recordFeedback() {
        print("请输入内容ID: ")
        val feedId = scanner.nextLine().trim()

        if (feedId.isEmpty()) {
            println("❌ 内容ID不能为空")
            return
        }

        print("请输入反馈分数 (-1.0 到 1.0, 负数表示不喜欢，正数表示喜欢): ")
        val feedback = scanner.nextLine().trim().toDoubleOrNull()

        if (feedback == null || feedback < -1.0 || feedback > 1.0) {
            println("❌ 反馈分数必须在-1.0到1.0之间")
            return
        }

        try {
            recommendationEngine.recordUserFeedback(feedId, feedback)
            val emoji = when {
                feedback > 0.5 -> "👍"
                feedback < -0.5 -> "👎"
                else -> "😐"
            }
            println("✅ 反馈已记录 $emoji")
        } catch (e: Exception) {
            println("❌ 记录反馈失败: ${e.message}")
        }
    }

    private fun showConfiguration() {
        println("⚙️ 系统配置信息")
        println("-" * 30)
        println("📂 数据库路径: $databasePath")
        println("🔧 运行模式: 交互模式")
        println("💾 JVM 内存: ${Runtime.getRuntime().totalMemory() / 1024 / 1024} MB")
        println("🖥️ 系统: ${System.getProperty("os.name")}")
    }

    private suspend fun runCleanup() {
        print("确认要清理旧数据吗? (y/N): ")
        val confirm = scanner.nextLine().trim().lowercase()

        if (confirm == "y" || confirm == "yes") {
            println("🧹 正在清理旧数据...")
            try {
                recommendationEngine.cleanup()
                println("✅ 清理完成")
            } catch (e: Exception) {
                println("❌ 清理失败: ${e.message}")
            }
        } else {
            println("❌ 已取消清理操作")
        }
    }
}

enum class AppMode {
    INTERACTIVE,
    DAEMON,
    GENERATE_ONCE,
}

suspend fun main(args: Array<String>) {
    try {
        val app = ZhihuDesktopApp()
        app.run(args)
    } catch (e: Exception) {
        println("❌ 应用程序错误: ${e.message}")
        e.printStackTrace()
    }
}

private operator fun String.times(n: Int): String = repeat(n)
