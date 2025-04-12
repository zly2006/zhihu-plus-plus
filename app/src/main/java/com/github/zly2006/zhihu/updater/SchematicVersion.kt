package com.github.zly2006.zhihu.updater

class SchematicVersion(
    val allComponents: List<Int>,
    val preRelease: String,
    val build: String,
) {
    companion object {
        private val REGEX = Regex("""[vV]?(?<components>[\d.]+)(-(?<pre>[\w._-]+))?(\+(?<build>.+))?""")
        fun fromString(version: String): SchematicVersion {
            val match = REGEX.matchEntire(version) ?: throw IllegalArgumentException("Invalid version string")
            val components = match.groups["components"]!!.value.split(".").map { it.toInt() }
            require(components.isNotEmpty()) { "Version must have at least one component" }
            val preRelease = match.groups["pre"]?.value ?: ""
            val build = match.groups["build"]?.value ?: ""
            return SchematicVersion(components, preRelease, build)
        }
    }

    val major: Int get() = allComponents.getOrNull(0) ?: 0
    val minor: Int get() = allComponents.getOrNull(1) ?: 0
    val patch: Int get() = allComponents.getOrNull(2) ?: 0

    override fun toString() = buildString {
        append(allComponents.joinToString("."))
        if (preRelease.isNotEmpty()) append("-$preRelease")
        if (build.isNotEmpty()) append("+$build")
    }

    operator fun compareTo(other: SchematicVersion): Int {
        for (i in 0 until maxOf(allComponents.size, other.allComponents.size)) {
            val a = allComponents.getOrNull(i) ?: 0
            val b = other.allComponents.getOrNull(i) ?: 0
            if (a != b) return a - b
        }
        if (preRelease.isNotEmpty() && other.preRelease.isNotEmpty()) {
            val a = preRelease.split(".")
            val b = other.preRelease.split(".")
            for (i in 0 until maxOf(a.size, b.size)) {
                val aPart = a.getOrNull(i) ?: ""
                val bPart = b.getOrNull(i) ?: ""
                if (aPart != bPart) {
                    return when {
                        aPart.all { it.isDigit() } && bPart.all { it.isDigit() } -> aPart.toInt() - bPart.toInt()
                        else -> aPart.compareTo(bPart)
                    }
                }
            }
            return 0
        } else if (preRelease.isNotEmpty()) {
            return -1
        } else if (other.preRelease.isNotEmpty()) {
            return 1
        }
        return 0
    }

    operator fun compareTo(other: String): Int {
        return compareTo(fromString(other))
    }
}
