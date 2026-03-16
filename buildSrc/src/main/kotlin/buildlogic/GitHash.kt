package buildlogic

import java.io.File

private const val FALLBACK_HASH = "unknown"
private const val SHORT_HASH_LENGTH = 7

fun gitHash(projectRoot: File): String {
    val repoRoot = findRepositoryRoot(projectRoot.absoluteFile) ?: return FALLBACK_HASH
    val gitDir = resolveGitDir(repoRoot) ?: return FALLBACK_HASH
    val fullHash = readHeadHash(gitDir) ?: return FALLBACK_HASH
    return fullHash.take(SHORT_HASH_LENGTH)
}

private fun findRepositoryRoot(start: File): File? {
    var current: File? = start
    while (current != null) {
        val gitPath = File(current, ".git")
        if (gitPath.exists()) return current
        current = current.parentFile
    }
    return null
}

private fun resolveGitDir(repositoryRoot: File): File? {
    val gitPath = File(repositoryRoot, ".git")
    return when {
        gitPath.isDirectory -> gitPath
        gitPath.isFile -> {
            val gitDirLine = gitPath
                .readText()
                .lineSequence()
                .firstOrNull { it.startsWith("gitdir:") }
                ?.substringAfter("gitdir:")
                ?.trim()
                ?: return null
            resolvePath(repositoryRoot, gitDirLine)
        }
        else -> null
    }
}

private fun readHeadHash(gitDir: File): String? {
    val headFile = File(gitDir, "HEAD")
    if (!headFile.isFile) return null
    val headContent = headFile.readText().trim()

    return if (headContent.startsWith("ref:")) {
        val refPath = headContent.substringAfter("ref:").trim()
        readRefHash(gitDir, refPath)
    } else {
        headContent.takeIf(::isCommitHash)
    }
}

private fun readRefHash(gitDir: File, refPath: String): String? {
    val commonDir = resolveCommonGitDir(gitDir)
    val candidateDirs = listOf(gitDir, commonDir).distinctBy { it.absolutePath }

    candidateDirs.forEach { dir ->
        val refFile = File(dir, refPath)
        if (refFile.isFile) {
            val hash = refFile.readText().trim()
            if (isCommitHash(hash)) return hash
        }
    }

    for (dir in candidateDirs) {
        val packedRefs = File(dir, "packed-refs")
        if (!packedRefs.isFile) continue
        for (line in packedRefs.readLines()) {
            if (line.isBlank() || line.startsWith("#") || line.startsWith("^")) continue
            val sep = line.indexOf(' ')
            if (sep <= 0) continue
            val hash = line.substring(0, sep)
            val packedRef = line.substring(sep + 1)
            if (packedRef == refPath && isCommitHash(hash)) return hash
        }
    }

    return null
}

private fun resolveCommonGitDir(gitDir: File): File {
    val commonDirFile = File(gitDir, "commondir")
    if (!commonDirFile.isFile) return gitDir
    val commonDirPath = commonDirFile.readText().trim()
    if (commonDirPath.isEmpty()) return gitDir
    return resolvePath(gitDir, commonDirPath)
}

private fun resolvePath(baseDir: File, path: String): File {
    val raw = File(path)
    return if (raw.isAbsolute) raw else File(baseDir, path)
}

private fun isCommitHash(value: String): Boolean {
    return value.length == 40 && value.all { it in "0123456789abcdefABCDEF" }
}
