/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.chloemlla.zhplus.onboarding

/**
 * 单次构建的「本次更新说明」。
 *
 * [commitHashPrefix] 匹配 [BuildIdentity.commitHash] / shortHash 前缀（忽略大小写）。
 * 为空时表示通用条目，仅在没有任何更具体匹配时作为回落。
 */
data class WhatsNewEntry(
    val commitHashPrefix: String = "",
    val title: String,
    val summary: String,
    val highlights: List<String>,
    val showGenericWhenNoMatch: Boolean = false,
)

/**
 * 每次用户可见功能/修复 commit 在此追加条目。
 *
 * 契约见 `docs/build-whats-new.md` 与 `.trellis/spec/frontend/flutter-build-whats-new.md`。
 */
object WhatsNewCatalog {
    /**
     * 最新在前。匹配规则：
     * 1. commit 前缀命中（最长优先）
     * 2. 否则若存在 [WhatsNewEntry.showGenericWhenNoMatch] 条目则使用之
     * 3. 否则 null（调用方静默 mark seen）
     */
    val pages: List<WhatsNewEntry> =
        listOf(
            WhatsNewEntry(
                commitHashPrefix = "",
                title = "本次更新说明",
                summary = "沉浸式用户须知与开源声明已就绪。",
                highlights =
                    listOf(
                        "首次安装将展示开源地址、永久免费防骗提示与第三方依赖鸣谢。",
                        "每次新构建可按 Git commit 与构建时间展示「本次更新说明」。",
                        "账号页可随时重开「开源说明与鸣谢」；完整许可证列表仍在「开源许可」。",
                        "请仅从官方 GitHub 仓库与 Release 获取安装包。",
                    ),
                showGenericWhenNoMatch = true,
            ),
        )

    fun resolve(identity: BuildIdentity): WhatsNewEntry? {
        val commit = identity.commitHash.ifBlank { identity.shortHash }
        if (commit.isNotBlank() && !commit.equals("unknown", ignoreCase = true)) {
            val matched =
                pages
                    .filter { it.commitHashPrefix.isNotBlank() }
                    .filter { commit.startsWith(it.commitHashPrefix, ignoreCase = true) }
                    .maxByOrNull { it.commitHashPrefix.length }
            if (matched != null) return matched
        }
        return pages.firstOrNull { it.showGenericWhenNoMatch }
    }
}
