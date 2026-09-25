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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MirrorSelectorTest {
    private val officialUrl = "https://github.com/zly2006/zhihu-plus-plus/releases/download/0.30/zhihu%2B%2B-lite.apk"

    @Test
    fun prefixMirrorKeepsOfficialUrlIntactBehindPrefix() {
        val candidate = MirrorCandidate("https://ghfast.top/", MirrorUrlForm.Prefix)
        assertEquals("https://ghfast.top/$officialUrl", candidate.rewrite(officialUrl))
    }

    @Test
    fun domainReplaceMirrorOnlySwapsHost() {
        val candidate = MirrorCandidate("https://dgithub.xyz", MirrorUrlForm.DomainReplace)
        assertEquals(
            "https://dgithub.xyz/zly2006/zhihu-plus-plus/releases/download/0.30/zhihu%2B%2B-lite.apk",
            candidate.rewrite(officialUrl),
        )
    }

    @Test
    fun candidateNameIsMirrorHost() {
        assertEquals("ghfast.top", MirrorCandidate("https://ghfast.top/", MirrorUrlForm.Prefix).name)
        assertEquals("hub.gitmirror.com", MirrorCandidate("https://hub.gitmirror.com", MirrorUrlForm.DomainReplace).name)
    }

    @Test
    fun candidateChainAppendsOfficialDirectDownloadAtTail() {
        val chain = MirrorSelector().candidateChain(officialUrl, mirrorEnabled = true)

        assertEquals(DEFAULT_MIRROR_CANDIDATES.size + 1, chain.size)
        assertEquals(DEFAULT_MIRROR_CANDIDATES.map { it.name }, chain.dropLast(1).map { it.name })
        assertEquals(OFFICIAL_DOWNLOAD_SOURCE_NAME, chain.last().name)
        assertEquals(officialUrl, chain.last().url)
    }

    @Test
    fun disabledMirrorsLeaveOnlyOfficialSource() {
        val chain = MirrorSelector().candidateChain(officialUrl, mirrorEnabled = false)
        assertEquals(listOf(DownloadSource(OFFICIAL_DOWNLOAD_SOURCE_NAME, officialUrl)), chain)
    }

    @Test
    fun nonGithubUrlIsNeverRewritten() {
        val quarkUrl = "https://pan.quark.cn/s/abc123"
        assertEquals(
            listOf(DownloadSource(OFFICIAL_DOWNLOAD_SOURCE_NAME, quarkUrl)),
            MirrorSelector().candidateChain(quarkUrl, mirrorEnabled = true),
        )
    }

    @Test
    fun candidateMarkedDisabledNeverEntersChain() {
        val selector = MirrorSelector(
            listOf(MirrorCandidate("https://ghfast.top", MirrorUrlForm.Prefix, enabled = false)),
        )
        assertEquals(1, selector.candidateChain(officialUrl, mirrorEnabled = true).size)
    }

    @Test
    fun rankedMirrorsDropsUnavailableAndOrdersByMeasuredBandwidth() {
        val fastest = probe("fast.example.com", bytesPerSecond = 800_000, ttfbMillis = 40)
        val slow = probe("slow.example.com", bytesPerSecond = 90_000, ttfbMillis = 10)
        val sizeMismatch = probe("wrong.example.com", bytesPerSecond = 9_000_000, sizeMatches = false)
        val unreachable = probe("dead.example.com", bytesPerSecond = 0, failureReason = "探测超时")

        assertEquals(
            listOf(fastest, slow),
            MirrorSelector(emptyList()).rankedMirrors(listOf(slow, sizeMismatch, unreachable, fastest)),
        )
    }

    @Test
    fun mirrorWithoutRangeSupportIsDownrankedBelowSlowerMirror() {
        val rangeSupported = probe("range.example.com", bytesPerSecond = 200_000, supportsRange = true)
        val noRange = probe("norange.example.com", bytesPerSecond = 300_000, supportsRange = false)

        assertEquals(
            listOf(rangeSupported, noRange),
            MirrorSelector(emptyList()).rankedMirrors(listOf(noRange, rangeSupported)),
        )
    }

    @Test
    fun ttfbBreaksTiesBetweenEquallyFastMirrors() {
        val quick = probe("quick.example.com", bytesPerSecond = 500_000, ttfbMillis = 30)
        val sluggish = probe("sluggish.example.com", bytesPerSecond = 500_000, ttfbMillis = 900)

        assertEquals(
            listOf(quick, sluggish),
            MirrorSelector(emptyList()).rankedMirrors(listOf(sluggish, quick)),
        )
    }

    @Test
    fun sizeMismatchOrProbeFailureMakesMirrorUnavailable() {
        assertFalse(probe("wrong.example.com", sizeMatches = false).available)
        assertFalse(probe("dead.example.com", failureReason = "探测超时").available)
        assertTrue(probe("healthy.example.com").available)
    }

    @Test
    fun consecutiveFailuresDownrankAnUnstableMirror() {
        val unstable = probe("flaky.example.com", bytesPerSecond = 1_500_000)

        // 每次连续失败扣 1MB/s：两次失败后已经低于一个稳定但仍可用的一般镜像。
        assertEquals(1_500_000, unstable.score(0))
        assertTrue(unstable.score(2) < probe("stable.example.com", bytesPerSecond = 400_000).score(0))
    }
}

private fun probe(
    name: String,
    bytesPerSecond: Long = 500_000,
    ttfbMillis: Long = 100,
    supportsRange: Boolean = true,
    sizeMatches: Boolean = true,
    failureReason: String? = null,
) = MirrorProbe(
    source = DownloadSource(name, "https://$name/path"),
    sizeMatches = sizeMatches,
    supportsRange = supportsRange,
    ttfbMillis = ttfbMillis,
    bytesPerSecond = bytesPerSecond,
    failureReason = failureReason,
)
