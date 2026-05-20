package com.github.zly2006.zhihu.shared.util

fun extractImageUrl(attribute: (String) -> String): String? =
    attribute("data-original-token")
        .takeIf { it.startsWith("v2-") }
        ?.let { "https://pic1.zhimg.com/$it" }
        ?: attribute("data-original").takeIf { it.isNotBlank() }
        ?: attribute("data-default-watermark-src").takeIf { it.isNotBlank() }
        ?: attribute("data-actualsrc").takeIf { it.isNotBlank() }
        ?: attribute("data-thumbnail").takeIf { it.isNotBlank() }
        ?: attribute("src").takeIf { it.isNotBlank() }
