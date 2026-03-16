package buildlogic

import java.io.File

private const val FALLBACK_HASH = "unknown"

fun gitHash(projectRoot: File): String {
    return runGit(projectRoot, "rev-parse", "--short", "HEAD") ?: FALLBACK_HASH
}

private fun runGit(projectRoot: File, vararg args: String): String? {
    val command = listOf("git") + args
    return runCatching {
        val process = ProcessBuilder(command)
            .directory(projectRoot)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText().trim() }
        val exitCode = process.waitFor()
        if (exitCode == 0 && output.isNotEmpty()) output else null
    }.getOrNull()
}
