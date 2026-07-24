/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.onboarding

/**
 * 第三方依赖鸣谢条目（首装开源声明精选列表）。
 *
 * 全量许可证仍由 [com.github.zly2006.zhihu.ui.subscreens.OpenSourceLicensesScreen] aboutlibraries 提供。
 */
data class OssCredit(
    val name: String,
    val author: String,
    val description: String,
    val license: String,
    val url: String? = null,
)

object OssNoticeCopy {
    const val TITLE = "开源说明"
    const val INTRO =
        "使用 Zhihu++ 前请先阅读本说明。内容包括官方开源地址、永久免费与防骗提示、本项目开源协议，以及第三方依赖鸣谢。"
    const val REPO_TITLE = "官方开源仓库"
    const val REPO_BODY =
        "请仅从官方 GitHub 仓库获取 Zhihu++。这是我们维护的唯一来源。第三方「破解 / VIP / 付费分发」渠道均不受信任。"
    const val OPEN_REPO = "打开官方仓库"
    const val FREE_TITLE = "永久免费，谨防上当受骗"
    const val FREE_BODY =
        "Zhihu++ 永久免费且开源。请勿购买付费、破解或所谓「VIP」二次分发版本。任何以此收费的行为都可能是诈骗。"
    const val PROJECT_LICENSE_TITLE = "本项目开源协议"
    const val PROJECT_LICENSE_BODY =
        "本项目以 $ZHPLUS_PROJECT_LICENSE 发布。你可以在该协议允许范围内使用、学习、分享和改进本项目。源码与协议详情见官方仓库。"
    const val CREDITS_TITLE = "第三方依赖鸣谢"
    const val CREDITS_BODY = "Zhihu++ 基于以下开源项目与库构建。若有主页，点击条目可打开。"
    const val CONTINUE_LABEL = "继续"
    const val REOPEN_LABEL = "开源说明与鸣谢"
}

val curatedOssCredits: List<OssCredit> =
    listOf(
        OssCredit(
            name = "Jetpack Compose / Material 3",
            author = "Android Open Source Project / Google",
            description = "跨平台 UI 与 Material 3 组件体系。",
            license = "Apache License, Version 2.0",
            url = "https://developer.android.com/jetpack/compose",
        ),
        OssCredit(
            name = "Kotlin Multiplatform / Coroutines / Serialization",
            author = "JetBrains",
            description = "共享业务逻辑、异步与 JSON 序列化。",
            license = "Apache License, Version 2.0",
            url = "https://kotlinlang.org/",
        ),
        OssCredit(
            name = "Ktor Client",
            author = "JetBrains",
            description = "HTTP 客户端，用于知乎 API 与更新检查。",
            license = "Apache License, Version 2.0",
            url = "https://ktor.io/",
        ),
        OssCredit(
            name = "OkHttp",
            author = "Square",
            description = "Android 网络栈与连接池。",
            license = "Apache License, Version 2.0",
            url = "https://square.github.io/okhttp/",
        ),
        OssCredit(
            name = "AndroidX / Room / Lifecycle / Navigation",
            author = "Android Open Source Project / Google",
            description = "本地存储、生命周期与导航组件。",
            license = "Apache License, Version 2.0",
            url = "https://developer.android.com/jetpack",
        ),
        OssCredit(
            name = "Coil",
            author = "Coil Contributors",
            description = "Compose 图片加载。",
            license = "Apache License, Version 2.0",
            url = "https://coil-kt.github.io/coil/",
        ),
        OssCredit(
            name = "aboutlibraries",
            author = "Mike Penz",
            description = "自动收集并展示第三方许可证。",
            license = "Apache License, Version 2.0",
            url = "https://github.com/mikepenz/AboutLibraries",
        ),
        OssCredit(
            name = "KaTeX / Markdown 渲染栈",
            author = "KaTeX / 项目内置 markdown 组件",
            description = "正文公式与 Markdown 阅读体验。",
            license = "MIT / Apache License, Version 2.0",
            url = "https://katex.org/",
        ),
        OssCredit(
            name = "ZXing",
            author = "ZXing authors",
            description = "二维码登录扫描。",
            license = "Apache License, Version 2.0",
            url = "https://github.com/zxing/zxing",
        ),
        OssCredit(
            name = "Sentence-Embeddings-Android / HuggingFace Tokenizers",
            author = "shubham0204 / HuggingFace",
            description = "Full 变体本地句子嵌入与 tokenizer（智能过滤）。",
            license = "Apache License, Version 2.0",
            url = "https://github.com/shubham0204/Sentence-Embeddings-Android",
        ),
        OssCredit(
            name = "unDraw",
            author = "Katerina Limpitsouni",
            description = "开源声明与引导页使用的开源插画（动态取色 ImageVector）。",
            license = "unDraw License",
            url = "https://undraw.co/",
        ),
    )
