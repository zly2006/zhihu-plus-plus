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

import com.github.zly2006.zhihu.data.ZhihuJson
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SemanticVersionTest {
    @Test
    fun parsesVersionComponentsPreReleaseAndBuild() {
        val version = SemanticVersion.fromString("v1.2.3-beta.4+build.5")

        assertEquals(listOf(1, 2, 3), version.allComponents)
        assertEquals(1, version.major)
        assertEquals(2, version.minor)
        assertEquals(3, version.patch)
        assertEquals("beta.4", version.preRelease)
        assertEquals("build.5", version.build)
        assertEquals("1.2.3-beta.4+build.5", version.toString())
    }

    @Test
    fun comparesReleaseAndPreReleaseVersions() {
        assertTrue(SemanticVersion.fromString("1.2.1") > SemanticVersion.fromString("1.2.0"))
        assertEquals(0, SemanticVersion.fromString("1.2.0").compareTo(SemanticVersion.fromString("1.2")))
        assertTrue(SemanticVersion.fromString("1.2.0") > SemanticVersion.fromString("1.2.0-beta.1"))
        assertTrue(SemanticVersion.fromString("1.2.0-beta.2") > SemanticVersion.fromString("1.2.0-beta.1"))
        assertTrue(SemanticVersion.fromString("1.2.0-beta.11") > SemanticVersion.fromString("1.2.0-beta.1"))
        assertTrue(SemanticVersion.fromString("1.2.0-beta.11") > SemanticVersion.fromString("1.2.0-beta.2"))
    }

    @Test
    fun comparesPreReleaseIdentifiers() {
        val versions = listOf("1.0-pre.1", "1.0-pre.2", "1.0-pre.10", "1.0-pre.alpha", "1.0-pre.alpha.1", "1.0")
            .map(SemanticVersion::fromString)
        versions.zipWithNext().forEach { (earlier, later) -> assertTrue(earlier < later, "$earlier should precede $later") }
        assertTrue(SemanticVersion.fromString("1.0-1") < SemanticVersion.fromString("1.0-alpha"))
        assertEquals(0, SemanticVersion.fromString("1.0-pre.1+first").compareTo("1.0-pre.1+second"))
        assertTrue(SemanticVersion.fromString("2147483647.0") > SemanticVersion.fromString("0.0"))
        assertTrue(SemanticVersion.fromString("1.0-pre.999999999999999999999") > SemanticVersion.fromString("1.0-pre.2"))
    }

    @Test
    fun serializesAsVersionString() {
        val holder = VersionHolder(SemanticVersion.fromString("2.0.0-nightly"))

        val encoded = ZhihuJson.json.encodeToString(holder)
        val decoded = ZhihuJson.json.decodeFromString<VersionHolder>(encoded)

        assertEquals("""{"version":"2.0.0-nightly"}""", encoded)
        assertEquals("2.0.0-nightly", decoded.version.toString())
    }

    @Test
    fun rejectsInvalidVersionString() {
        listOf("nightly", "1..0", "1.0-", "1.0-pre..1", "1.0-pre_1", "1.0+").forEach { invalid ->
            assertFailsWith<IllegalArgumentException>(invalid) {
                SemanticVersion.fromString(invalid)
            }
        }
    }

    @Serializable
    private data class VersionHolder(
        val version: SemanticVersion,
    )
}
