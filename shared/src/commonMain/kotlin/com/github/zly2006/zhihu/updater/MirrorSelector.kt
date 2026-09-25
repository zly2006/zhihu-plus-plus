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

import io.ktor.client.HttpClient
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/** 设置键：应用内下载是否启用国内镜像加速。 */
const val MIRROR_ACCELERATION_ENABLED_PREFERENCE_KEY = "mirrorAccelerationEnabled"

/** 官方直连的来源名，也是候选链尾部永远保留的兜底来源。 */
const val OFFICIAL_DOWNLOAD_SOURCE_NAME = "GitHub 官方"

/** 官方下载地址的域名。只有该域名的地址才允许镜像改写，网盘外链等地址只能官方直连。 */
private const val OFFICIAL_DOWNLOAD_HOST = "github.com"

/** 单候选探测预算：HEAD 与首分片测速各自不超过该时长。 */
private const val PROBE_TIMEOUT_MILLIS = 5_000L

/** 测速采样大小：首个 512KB 分片。 */
private const val SPEED_SAMPLE_BYTES = 512L * 1024

/**
 * 速度评估周期，同时作为单次读取的停滞上限。
 *
 * 每周期结算已下载字节以判断是否跌破低速阈值；同一时长内没有任何数据到达说明连接已经停滞，
 * 同样需要切换候选源，因此两个判断共用一个周期。
 */
private const val SPEED_EVALUATION_INTERVAL_MILLIS = 10_000L

/** 低速阈值：评估周期内的平均速度低于该值时切换候选源。 */
private const val MIN_DOWNLOAD_BYTES_PER_SECOND = 32L * 1024

/** 不支持 Range 的源会失去断点续传能力，按该带宽量降权。 */
private const val NO_RANGE_PENALTY_BYTES_PER_SECOND = 256L * 1024

/** 每多一次连续失败追加的带宽量惩罚，避免反复选中不稳定源。 */
private const val FAILURE_PENALTY_BYTES_PER_SECOND = 1024L * 1024

/** 下载写入与测速采样共用的缓冲区大小。 */
private const val TRANSFER_BUFFER_BYTES = 64 * 1024

/** 镜像候选的 URL 改写形态。 */
enum class MirrorUrlForm {
    /** 前缀式代理：官方 URL 的协议与域名整体保留，在头部拼接镜像前缀。 */
    Prefix,

    /** 全站镜像：仅把官方域名替换为镜像域名，路径与查询参数保持不变。 */
    DomainReplace,
}

/**
 * 单个 GitHub 加速镜像候选。
 *
 * @param base 前缀式代理填镜像前缀，全站镜像填镜像站点根
 */
data class MirrorCandidate(
    val base: String,
    val form: MirrorUrlForm,
    val enabled: Boolean = true,
) {
    /** 候选展示名，取镜像域名。 */
    val name: String = base.substringAfter("://").trimEnd('/').substringBefore('/')

    /** 按形态把官方下载地址改写为镜像地址。 */
    fun rewrite(officialUrl: String): String = when (form) {
        MirrorUrlForm.Prefix -> base.trimEnd('/') + "/" + officialUrl
        MirrorUrlForm.DomainReplace -> officialUrl.replaceFirst(OFFICIAL_DOWNLOAD_HOST, name)
    }
}

/**
 * 内置镜像候选池，硬编码于 commonMain 并随应用发版更新。
 *
 * 2026-09-21 实测记录：`kkgithub.com` 域名已过期（返回 DNSPod 域名过期页）、`ghproxy.link`
 * 返回的是加速地址发布落地页而非文件代理，两者都无法直接提供 Release 资产，已从池中移除。
 * 其余候选的可用性随运营方状态动态变化，由运行时探测与历史惩罚机制取舍；新增候选前必须先逐源实测，
 * 确认能按上述改写规则返回原始文件。
 */
val DEFAULT_MIRROR_CANDIDATES: List<MirrorCandidate> = listOf(
    MirrorCandidate("https://ghfast.top", MirrorUrlForm.Prefix),
    MirrorCandidate("https://gh-proxy.com", MirrorUrlForm.Prefix),
    MirrorCandidate("https://mirror.ghproxy.com", MirrorUrlForm.Prefix),
    MirrorCandidate("https://gh.qninq.cn", MirrorUrlForm.Prefix),
    MirrorCandidate("https://dgithub.xyz", MirrorUrlForm.DomainReplace),
    MirrorCandidate("https://bgithub.xyz", MirrorUrlForm.DomainReplace),
    MirrorCandidate("https://hub.gitmirror.com", MirrorUrlForm.DomainReplace),
)

/** 实际参与下载的一个来源（镜像或官方直连）。 */
data class DownloadSource(
    val name: String,
    val url: String,
)

/** 单个候选源的探测结果。 */
data class MirrorProbe(
    val source: DownloadSource,
    /** 响应大小是否与官方资产一致；不一致说明返回的不是同一个文件。 */
    val sizeMatches: Boolean,
    val supportsRange: Boolean,
    val ttfbMillis: Long,
    val bytesPerSecond: Long,
    val failureReason: String? = null,
) {
    /** 可用性一票否决：探测失败或大小不符的源不参与选优。 */
    val available: Boolean get() = failureReason == null && sizeMatches

    /** 评分：实测带宽为主指标，Range 缺失与历史连续失败按带宽量降权。 */
    internal fun score(consecutiveFailures: Int): Long =
        bytesPerSecond -
            (if (supportsRange) 0 else NO_RANGE_PENALTY_BYTES_PER_SECOND) -
            consecutiveFailures * FAILURE_PENALTY_BYTES_PER_SECOND
}

/** 一次多源下载请求。 */
data class MirrorDownloadRequest(
    val officialUrl: String,
    val destination: Path,
    /** 官方资产的字节数，用于探测筛选与下载后大小校验；元数据缺失时为 null。 */
    val expectedSize: Long? = null,
    /** 官方资产的 SHA256 摘要，用于下载后摘要校验；缺失时自动降级。 */
    val expectedDigest: String? = null,
    val mirrorEnabled: Boolean = true,
)

/** 下载进度与当前来源，用于向用户展示正在使用哪个源。 */
data class DownloadProgress(
    val source: DownloadSource,
    val downloadedBytes: Long,
    val totalBytes: Long,
)

/** 多源下载的终态。 */
sealed interface DownloadResult {
    /** 已通过校验，可以进入安装流程；[source] 是实际完成下载的来源。 */
    data class Verified(
        val source: DownloadSource,
        val layers: List<String>,
    ) : DownloadResult

    /** 全部候选源均失败；[reason] 用于向用户展示失败原因。 */
    data class Failed(
        val reason: String,
    ) : DownloadResult
}

/**
 * 镜像候选池与选优引擎。
 *
 * 负责候选池管理、URL 改写、并行探测、评分选优与下载切换调度。官方直连永远排在候选链尾部，
 * 因此镜像全部不可用时更新功能的最低可用性与现状一致。实例内保留各来源的连续失败次数供评分惩罚
 * 使用，避免反复选中不稳定源。
 */
class MirrorSelector(
    private val candidates: List<MirrorCandidate> = DEFAULT_MIRROR_CANDIDATES,
) {
    private val consecutiveFailures = mutableMapOf<String, Int>()

    /**
     * 构建候选链：启用的镜像候选 + 官方直连。
     *
     * 非 GitHub Releases 的地址不参与镜像改写，此时候选链只包含官方直连。
     */
    fun candidateChain(officialUrl: String, mirrorEnabled: Boolean): List<DownloadSource> {
        val mirrors = if (mirrorEnabled && officialUrl.contains("://$OFFICIAL_DOWNLOAD_HOST/")) {
            candidates.filter { it.enabled }.map { DownloadSource(it.name, it.rewrite(officialUrl)) }
        } else {
            emptyList()
        }
        return mirrors + DownloadSource(OFFICIAL_DOWNLOAD_SOURCE_NAME, officialUrl)
    }

    /**
     * 并行探测镜像候选。
     *
     * 官方直连不参与探测：它只作为兜底且始终位于候选链尾部，为它多付一次 512KB 采样不划算。
     */
    suspend fun probeAll(
        client: HttpClient,
        mirrors: List<DownloadSource>,
        expectedSize: Long?,
    ): List<MirrorProbe> = coroutineScope {
        mirrors.map { mirror -> async { probe(client, mirror, expectedSize) } }.awaitAll()
    }

    /** 按评分排序可用镜像：带宽优先，TTFB 次之。 */
    fun rankedMirrors(probes: List<MirrorProbe>): List<MirrorProbe> = probes
        .filter { it.available }
        .sortedWith(
            compareByDescending<MirrorProbe> { it.score(consecutiveFailures[it.source.name] ?: 0) }
                .thenBy { it.ttfbMillis },
        )

    /**
     * 下载官方资产：探测选优、低速或中断时切换候选源、完成后过校验门禁。
     *
     * 候选链最多被遍历一遍，因此「最大切换次数为候选数减一」由遍历本身保证；校验失败不占用切换
     * 额度，恶意镜像不能靠"探测存活但内容错误"把候选链耗尽，官方直连始终会被尝试。校验失败会
     * 丢弃已落盘内容并重新全量下载，中断或低速则保留断点供下一个候选源续传。
     */
    suspend fun download(
        client: HttpClient,
        request: MirrorDownloadRequest,
        verifier: DownloadedFileVerifier,
        onProgress: (DownloadProgress) -> Unit = {},
    ): DownloadResult {
        val chain = candidateChain(request.officialUrl, request.mirrorEnabled)
        val officialSource = chain.last()
        val mirrors = chain.dropLast(1)

        // 会话开始时丢弃上一次留下的残留文件，避免把陈旧内容当作断点续传基础。
        SystemFileSystem.delete(request.destination, mustExist = false)

        val ranked = rankedMirrors(probeAll(client, mirrors, request.expectedSize)).map { it.source }
        var resumeFrom = 0L
        var lastFailure = "全部候选源均下载失败"

        for (source in ranked + officialSource) {
            when (val attempt = transfer(client, request, source, resumeFrom, verifier, onProgress)) {
                is TransferOutcome.Verified -> {
                    consecutiveFailures.remove(source.name)
                    return DownloadResult.Verified(source, attempt.layers)
                }

                is TransferOutcome.Interrupted -> {
                    consecutiveFailures[source.name] = (consecutiveFailures[source.name] ?: 0) + 1
                    lastFailure = attempt.reason
                    resumeFrom = attempt.downloadedBytes
                }

                is TransferOutcome.Rejected -> {
                    SystemFileSystem.delete(request.destination, mustExist = false)
                    consecutiveFailures[source.name] = (consecutiveFailures[source.name] ?: 0) + 1
                    lastFailure = attempt.reason
                    resumeFrom = 0
                }
            }
        }
        return DownloadResult.Failed(lastFailure)
    }

    private sealed interface TransferOutcome {
        data class Verified(
            val layers: List<String>,
        ) : TransferOutcome

        /** 传输中断或速度跌破阈值；[downloadedBytes] 是已落盘字节数，可用于断点续传。 */
        data class Interrupted(
            val downloadedBytes: Long,
            val reason: String,
        ) : TransferOutcome

        /** 下载完成但未通过校验。 */
        data class Rejected(
            val reason: String,
        ) : TransferOutcome
    }

    private suspend fun probe(
        client: HttpClient,
        mirror: DownloadSource,
        expectedSize: Long?,
    ): MirrorProbe {
        var headLength: Long? = null
        var sampledSize: Long? = null
        var supportsRange = false
        var ttfbMillis = 0L
        var bytesPerSecond = 0L
        var failureReason: String? = null

        // HEAD 只用于补充 Accept-Ranges：部分镜像不支持 HEAD，它失败不代表源不可用，
        // 可用性与大小都以下面的首分片实测为准（HEAD 的 Content-Length 也可能被 CDN 置 0）。
        attemptQuietly(PROBE_TIMEOUT_MILLIS) { client.head(mirror.url) }?.let { response ->
            if (response.status.isSuccess()) {
                headLength = response.contentLengthOrNull()
                supportsRange = response.acceptsRanges()
            }
        }

        val sampled = attemptQuietly(PROBE_TIMEOUT_MILLIS) {
            client
                .prepareGet(mirror.url) {
                    header(HttpHeaders.Range, "bytes=0-${SPEED_SAMPLE_BYTES - 1}")
                }.execute { response ->
                    if (!response.status.isSuccess()) {
                        failureReason = "探测返回 HTTP ${response.status.value}"
                        return@execute false
                    }
                    if (response.status == HttpStatusCode.PartialContent) {
                        supportsRange = true
                        sampledSize = response.contentRangeTotal()
                    } else {
                        // 返回整体内容说明该源忽略 Range，只能全量下载，断点续传对它不可用。
                        supportsRange = false
                        sampledSize = response.contentLengthOrNull()
                    }

                    val start = TimeSource.Monotonic.markNow()
                    var firstByteAt: Duration? = null
                    var read = 0L
                    val channel = response.bodyAsChannel()
                    val buffer = ByteArray(TRANSFER_BUFFER_BYTES)
                    while (read < SPEED_SAMPLE_BYTES && start.elapsedNow() < PROBE_TIMEOUT_MILLIS.milliseconds) {
                        val chunk = minOf(buffer.size.toLong(), SPEED_SAMPLE_BYTES - read).toInt()
                        val count = channel.readAvailable(buffer, 0, chunk)
                        if (count <= 0) break
                        if (firstByteAt == null) firstByteAt = start.elapsedNow()
                        read += count
                    }
                    val elapsed = start.elapsedNow()
                    ttfbMillis = (firstByteAt ?: elapsed).inWholeMilliseconds
                    bytesPerSecond = read * 1000 / elapsed.inWholeMilliseconds.coerceAtLeast(1)
                    true
                }
        }
        if (sampled == null && failureReason == null) failureReason = "探测超时"

        return MirrorProbe(
            source = mirror,
            sizeMatches = expectedSize == null || (sampledSize ?: headLength) == expectedSize,
            supportsRange = supportsRange,
            ttfbMillis = ttfbMillis,
            bytesPerSecond = bytesPerSecond,
            failureReason = failureReason,
        )
    }

    private suspend fun transfer(
        client: HttpClient,
        request: MirrorDownloadRequest,
        source: DownloadSource,
        resumeFrom: Long,
        verifier: DownloadedFileVerifier,
        onProgress: (DownloadProgress) -> Unit,
    ): TransferOutcome = try {
        client
            .prepareGet(source.url) {
                if (resumeFrom > 0) header(HttpHeaders.Range, "bytes=$resumeFrom-")
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    return@execute TransferOutcome.Interrupted(
                        currentBytesOnDisk(request.destination),
                        "下载失败：HTTP ${response.status.value}",
                    )
                }
                val resumed = resumeFrom > 0 && response.status == HttpStatusCode.PartialContent
                if (resumeFrom > 0 && !resumed) {
                    // 该源忽略 Range 直接返回整体内容：断点无法续接，必须丢弃已落盘内容重新全量写入。
                    SystemFileSystem.delete(request.destination, mustExist = false)
                }

                when (val streamed = stream(response, request, source, if (resumed) resumeFrom else 0L, onProgress)) {
                    is StreamOutcome.Interrupted -> TransferOutcome.Interrupted(streamed.downloadedBytes, streamed.reason)
                    StreamOutcome.Completed -> when (
                        val outcome = verifier.verify(request.destination, request.expectedSize, request.expectedDigest)
                    ) {
                        is VerificationOutcome.Passed -> TransferOutcome.Verified(outcome.layers)
                        is VerificationOutcome.Failed -> TransferOutcome.Rejected(outcome.reason)
                    }
                }
            }
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        TransferOutcome.Interrupted(currentBytesOnDisk(request.destination), error.message ?: "下载失败")
    }

    private sealed interface StreamOutcome {
        data object Completed : StreamOutcome

        data class Interrupted(
            val downloadedBytes: Long,
            val reason: String,
        ) : StreamOutcome
    }

    private suspend fun stream(
        response: HttpResponse,
        request: MirrorDownloadRequest,
        source: DownloadSource,
        startBytes: Long,
        onProgress: (DownloadProgress) -> Unit,
    ): StreamOutcome {
        val remaining = response.contentLengthOrNull()
        val totalBytes = request.expectedSize ?: remaining?.plus(startBytes) ?: 0L
        var downloaded = startBytes
        var windowStart = TimeSource.Monotonic.markNow()
        var windowBytes = 0L
        val sink = SystemFileSystem.sink(request.destination, append = startBytes > 0).buffered()
        try {
            val channel = response.bodyAsChannel()
            val buffer = ByteArray(TRANSFER_BUFFER_BYTES)
            while (true) {
                val count = withTimeoutOrNull(SPEED_EVALUATION_INTERVAL_MILLIS) {
                    channel.readAvailable(buffer, 0, buffer.size)
                } ?: return StreamOutcome.Interrupted(
                    downloaded,
                    "下载停滞超过 ${SPEED_EVALUATION_INTERVAL_MILLIS / 1000} 秒，正在切换下载源",
                )
                if (count <= 0) break
                sink.write(buffer, 0, count)
                downloaded += count
                windowBytes += count
                onProgress(DownloadProgress(source, downloaded, totalBytes))

                val window = windowStart.elapsedNow()
                if (window.inWholeMilliseconds >= SPEED_EVALUATION_INTERVAL_MILLIS) {
                    if (windowBytes * 1000 / window.inWholeMilliseconds < MIN_DOWNLOAD_BYTES_PER_SECOND) {
                        return StreamOutcome.Interrupted(downloaded, "下载速度持续低于阈值，正在切换下载源")
                    }
                    windowStart = TimeSource.Monotonic.markNow()
                    windowBytes = 0
                }
            }
        } finally {
            sink.close()
        }
        return StreamOutcome.Completed
    }
}

/**
 * 在 [timeoutMillis] 内执行 [block]，超时或抛出异常时返回 null。
 *
 * 协程被外部取消时会继续抛出取消异常，不会伪装成"探测失败"。
 */
private suspend fun <T> attemptQuietly(timeoutMillis: Long, block: suspend () -> T): T? = withTimeoutOrNull(timeoutMillis) {
    try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        null
    }
}

/** 已落盘字节数，作为断点续传的起点。 */
private fun currentBytesOnDisk(path: Path): Long = SystemFileSystem.metadataOrNull(path)?.size ?: 0L

private fun HttpResponse.contentLengthOrNull(): Long? = headers[HttpHeaders.ContentLength]?.toLongOrNull()

private fun HttpResponse.acceptsRanges(): Boolean =
    headers[HttpHeaders.AcceptRanges].orEmpty().contains("bytes", ignoreCase = true)

/** 从 `Content-Range: bytes 0-524287/5575541` 中取出资产总大小。 */
private fun HttpResponse.contentRangeTotal(): Long? = headers[HttpHeaders.ContentRange]
    ?.substringAfterLast('/')
    ?.takeIf { it != "*" }
    ?.toLongOrNull()
