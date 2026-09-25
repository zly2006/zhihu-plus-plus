/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.updater

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = SemanticVersion.Companion::class)
class SemanticVersion(
    val allComponents: List<Int>,
    val preRelease: String,
    val build: String,
) : Comparable<SemanticVersion> {
    companion object : KSerializer<SemanticVersion> {
        override val descriptor =
            PrimitiveSerialDescriptor(SemanticVersion::class.simpleName!!, PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: SemanticVersion) = encoder.encodeString(value.toString())

        override fun deserialize(decoder: Decoder) = fromString(decoder.decodeString())

        private val REGEX = Regex("""[vV]?(?<components>\d+(?:\.\d+)*)(?:-(?<pre>[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?(?:\+(?<build>[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?""")

        fun fromString(version: String): SemanticVersion {
            val match = REGEX.matchEntire(version) ?: throw IllegalArgumentException("Invalid version string")
            val components = match.groups["components"]!!
                .value
                .split(".")
                .map { it.toInt() }
            require(components.isNotEmpty()) { "Version must have at least one component" }
            val preRelease = match.groups["pre"]?.value ?: ""
            val build = match.groups["build"]?.value ?: ""
            return SemanticVersion(components, preRelease, build)
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

    override operator fun compareTo(other: SemanticVersion): Int {
        for (i in 0 until maxOf(allComponents.size, other.allComponents.size)) {
            val a = allComponents.getOrNull(i) ?: 0
            val b = other.allComponents.getOrNull(i) ?: 0
            if (a != b) return a.compareTo(b)
        }
        if (preRelease.isNotEmpty() && other.preRelease.isNotEmpty()) {
            val a = preRelease.split(".")
            val b = other.preRelease.split(".")
            for (i in 0 until minOf(a.size, b.size)) {
                val aPart = a[i]
                val bPart = b[i]
                if (aPart == bPart) continue
                val aNumeric = aPart.all(Char::isDigit)
                val bNumeric = bPart.all(Char::isDigit)
                val result = when {
                    aNumeric && bNumeric ->
                        aPart
                            .trimStart('0')
                            .length
                            .compareTo(bPart.trimStart('0').length)
                            .takeIf { it != 0 } ?: aPart.trimStart('0').compareTo(bPart.trimStart('0'))
                    aNumeric -> -1
                    bNumeric -> 1
                    else -> aPart.compareTo(bPart)
                }
                if (result != 0) return result
            }
            return a.size.compareTo(b.size)
        } else if (preRelease.isNotEmpty()) {
            return -1
        } else if (other.preRelease.isNotEmpty()) {
            return 1
        }
        return 0
    }

    operator fun compareTo(other: String): Int = compareTo(fromString(other))
}
