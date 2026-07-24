/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.onboarding

import com.github.zly2006.zhihu.shared.platform.SettingsStore
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

const val KEY_OSS_NOTICE_COMPLETED_AT = "oss_notice_completed_at"
const val KEY_ONBOARDING_COMPLETED_AT = "onboarding_completed_at"
const val KEY_WHATS_NEW_SEEN_COMMIT = "whats_new_seen_commit"
const val KEY_WHATS_NEW_SEEN_BUILD_TIME = "whats_new_seen_build_time"

/** 官方仓库地址，开源声明与防骗文案统一引用。 */
const val ZHPLUS_REPO_URL = "https://github.com/zly2006/zhihu-plus-plus"
const val ZHPLUS_LICENSE_URL = "https://github.com/zly2006/zhihu-plus-plus/blob/master/LICENSE"
const val ZHPLUS_PROJECT_LICENSE = "AGPL-3.0-only"

enum class AppGateKind {
    None,
    OpenSourceNotice,
    ProductOnboarding,
    WhatsNew,
}

data class AppGateState(
    val kind: AppGateKind = AppGateKind.None,
    val reopenOss: Boolean = false,
    val whatsNew: WhatsNewEntry? = null,
)

/**
 * 门控状态机：OSS 声明 > 产品用户须知 > 本次更新说明 > 主界面。
 *
 * 老用户升级（已有本地偏好、但从未写过新 flag）会静默 mark OSS/onboarding 完成，避免强制弹窗。
 * 无 catalog 条目时静默 mark whats-new 已见，避免空弹。
 *
 * 注意：此函数可能写入 SettingsStore（迁移 / 静默 mark），应在 [remember] / 用户回调中调用，
 * 不要在无 key 的每次 recompose 中直接调用。
 */
fun SettingsStore.resolveAppGate(
    identity: BuildIdentity,
    reopenOss: Boolean,
): AppGateState {
    if (reopenOss) {
        return AppGateState(kind = AppGateKind.OpenSourceNotice, reopenOss = true)
    }

    settleExistingUserIfNeeded()

    val ossDone = getLong(KEY_OSS_NOTICE_COMPLETED_AT, 0L) > 0L
    if (!ossDone) {
        return AppGateState(kind = AppGateKind.OpenSourceNotice, reopenOss = false)
    }

    val onboardingDone = getLong(KEY_ONBOARDING_COMPLETED_AT, 0L) > 0L
    if (!onboardingDone) {
        return AppGateState(kind = AppGateKind.ProductOnboarding)
    }

    val seenCommit = getString(KEY_WHATS_NEW_SEEN_COMMIT, "")
    val seenBuildTime = getLong(KEY_WHATS_NEW_SEEN_BUILD_TIME, 0L)
    if (!identity.matches(seenCommit, seenBuildTime)) {
        val entry = WhatsNewCatalog.resolve(identity)
        if (entry != null) {
            return AppGateState(kind = AppGateKind.WhatsNew, whatsNew = entry)
        }
        // 无对应条目时静默记为已见，避免每次冷启动空弹。
        markWhatsNewSeen(identity)
    }

    return AppGateState(kind = AppGateKind.None)
}

fun SettingsStore.markOssNoticeCompleted(nowMillis: Long = currentEpochMillis()) {
    putLong(KEY_OSS_NOTICE_COMPLETED_AT, nowMillis)
}

fun SettingsStore.markOnboardingCompleted(
    identity: BuildIdentity,
    nowMillis: Long = currentEpochMillis(),
) {
    putLong(KEY_ONBOARDING_COMPLETED_AT, nowMillis)
    // 首次安装路径：完成引导后直接标记当前构建已见，避免紧接着弹 whats-new。
    markWhatsNewSeen(identity)
}

fun SettingsStore.markWhatsNewSeen(identity: BuildIdentity) {
    val commit = identity.commitHash.ifBlank { identity.shortHash }
    putString(KEY_WHATS_NEW_SEEN_COMMIT, commit)
    putLong(KEY_WHATS_NEW_SEEN_BUILD_TIME, identity.buildTimeUtcMillis)
}

/**
 * 老用户升级：已有业务偏好写入，但从未完成新门控 flag。
 * 静默补齐，避免把存量用户强制拉回首装流程。
 */
private fun SettingsStore.settleExistingUserIfNeeded(nowMillis: Long = currentEpochMillis()) {
    val ossDone = getLong(KEY_OSS_NOTICE_COMPLETED_AT, 0L) > 0L
    val onboardingDone = getLong(KEY_ONBOARDING_COMPLETED_AT, 0L) > 0L
    if (ossDone && onboardingDone) return
    if (!looksLikeExistingInstall()) return
    if (!ossDone) putLong(KEY_OSS_NOTICE_COMPLETED_AT, nowMillis)
    if (!onboardingDone) putLong(KEY_ONBOARDING_COMPLETED_AT, nowMillis)
}

private fun SettingsStore.looksLikeExistingInstall(): Boolean {
    // 这些 key 只会被真实使用过的用户写入；默认全新安装不应命中。
    // lastUpdateCheck / skippedVersion：老用户启动后自动检查更新常见痕迹。
    if (getLong("lastUpdateCheck", 0L) > 0L) return true
    if (getStringOrNull("skippedVersion") != null) return true
    if (getStringOrNull("recommendationMode") != null) return true
    if (getStringOrNull("themeMode") != null) return true
    if (getStringOrNull("bottom_bar_items") != null) return true
    if (getStringOrNull("githubToken") != null) return true
    if (getBoolean("developer", false)) return true
    if (getBoolean("duo3_all", false)) return true
    if (getBoolean("autoCheckUpdates", true) != true) return true
    return false
}

@OptIn(ExperimentalTime::class)
internal fun currentEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
