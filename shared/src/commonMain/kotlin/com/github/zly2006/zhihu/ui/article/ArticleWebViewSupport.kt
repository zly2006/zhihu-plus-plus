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

package com.github.zly2006.zhihu.ui.article

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element

/** 修复 WebView 文档中 `noscript` 图片的真实地址。 */
internal fun prepareContentDocument(
    content: String,
    onImageLoadFailure: () -> Unit = {},
): String =
    Ksoup
        .parse(content)
        .apply {
            select("noscript").forEach { noscript ->
                (noscript.nextSibling() as? Element)?.let { actualImg ->
                    if (actualImg.nodeName() == "img" && actualImg.attr("data-actualsrc").isNotEmpty()) {
                        actualImg.attr("src", actualImg.attr("data-actualsrc"))
                        actualImg.attr("class", actualImg.attr("class").replace("lazy", ""))
                        noscript.remove()
                        return@forEach
                    }
                }
                if (noscript.childrenSize() > 0) {
                    val node = noscript.child(0)
                    if (node.tagName() == "img") {
                        if (node.attr("class").contains("content_image")) {
                            node.attr("src", node.attr("data-thumbnail"))
                        }
                        if (node.attr("src").isEmpty()) {
                            if (node.attr("data-default-watermark-src").isNotEmpty()) {
                                node.attr("src", node.attr("data-default-watermark-src"))
                            } else {
                                onImageLoadFailure()
                            }
                        }
                    }
                    noscript.after(node)
                }
            }
        }.body()
        .html()
