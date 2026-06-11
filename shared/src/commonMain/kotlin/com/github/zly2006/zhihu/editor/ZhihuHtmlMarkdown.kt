/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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

package com.github.zly2006.zhihu.editor

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.nodes.TextNode
import io.ktor.http.Url

/**
 * 把知乎回答的 HTML（DataHolder.Answer.content / editableContent）转换成 Markdown，
 * 用于“编辑已有回答”时回填到编辑框。
 *
 * 目前转换策略是“尽量保留可读性”，不追求 100% 还原知乎编辑器内部结构：
 * - 段落/标题/列表/引用/代码块/表格/链接/图片
 * - 知乎公式（eeimg=1/2）会转换为 $...$ / $$...$$
 */
fun zhihuHtmlToMarkdown(html: String): String {
    val document = Ksoup.parse(html)
    return elementToMarkdown(document.body()).trim()
}

private fun elementToMarkdown(element: Element): String {
    val sb = StringBuilder()

    for (node in element.childNodes()) {
        when (node) {
            is Element -> {
                when (node.tagName().lowercase()) {
                    "h1" -> sb.append("# ${node.text()}\n\n")
                    "h2" -> sb.append("## ${node.text()}\n\n")
                    "h3" -> sb.append("### ${node.text()}\n\n")
                    "h4" -> sb.append("#### ${node.text()}\n\n")
                    "h5" -> sb.append("##### ${node.text()}\n\n")
                    "h6" -> sb.append("###### ${node.text()}\n\n")
                    "p" -> sb.append("${elementToMarkdown(node).trim()}\n\n")
                    "br" -> sb.append("\n")
                    "strong", "b" -> sb.append("**${node.text()}**")
                    "em", "i" -> sb.append("*${node.text()}*")
                    "u" -> sb.append("_${node.text()}_")
                    "code" -> sb.append("`${node.text()}`")
                    "pre" -> sb.append("```\n${node.text()}\n```\n\n")
                    "blockquote" -> {
                        val lines = elementToMarkdown(node).trim().split("\n")
                        for (line in lines) {
                            if (line.isNotBlank()) {
                                sb.append("> ").append(line).append("\n")
                            }
                        }
                        sb.append("\n")
                    }

                    "ul", "ol" -> {
                        val items = node.select("li")
                        items.forEachIndexed { index, item ->
                            val prefix = if (node.tagName() == "ul") "- " else "${index + 1}. "
                            sb.append(prefix).append(elementToMarkdown(item).trim()).append("\n")
                        }
                        sb.append("\n")
                    }

                    "li" -> sb.append(elementToMarkdown(node))

                    "a" -> {
                        val href = node.attr("href")
                        val text = node.text()
                        if (href.isNotEmpty()) {
                            sb.append("[$text]($href)")
                        } else {
                            sb.append(text)
                        }
                    }

                    "img" -> {
                        val equationTex = extractEquationTex(node)
                        if (equationTex != null) {
                            val eeimg = node.attr("eeimg")
                            if (eeimg == "2") {
                                sb.append("\n\n$$").append(equationTex).append("$$\n\n")
                            } else {
                                sb.append("$").append(equationTex).append("$")
                            }
                        } else {
                            val src = node
                                .attr("data-original-src")
                                .ifEmpty { node.attr("data-actualsrc") }
                                .ifEmpty { node.attr("src") }
                            val alt = node.attr("alt").ifEmpty { "image" }
                            if (src.isNotEmpty()) {
                                sb.append("![$alt]($src)\n\n")
                            }
                        }
                    }

                    "figure" -> {
                        val img = node.selectFirst("img")
                        if (img != null) {
                            val src = img
                                .attr("data-original-src")
                                .ifEmpty { img.attr("data-actualsrc") }
                                .ifEmpty { img.attr("src") }
                            val alt = img.attr("alt").ifEmpty { "image" }
                            if (src.isNotEmpty()) {
                                sb.append("![$alt]($src)\n\n")
                            }
                        } else {
                            sb.append(elementToMarkdown(node))
                        }
                    }

                    "hr" -> sb.append("---\n\n")

                    "table" -> {
                        val rows = node.select("tr")
                        if (rows.isNotEmpty()) {
                            val headerCells = rows[0].select("th, td")
                            if (headerCells.isNotEmpty()) {
                                sb.append("| ")
                                headerCells.forEach { cell ->
                                    sb.append(cell.text()).append(" | ")
                                }
                                sb.append("\n| ")
                                headerCells.forEach { _ ->
                                    sb.append("--- | ")
                                }
                                sb.append("\n")
                            }
                            for (i in 1 until rows.size) {
                                val cells = rows[i].select("td")
                                if (cells.isNotEmpty()) {
                                    sb.append("| ")
                                    cells.forEach { cell ->
                                        sb.append(cell.text()).append(" | ")
                                    }
                                    sb.append("\n")
                                }
                            }
                            sb.append("\n")
                        }
                    }

                    "div", "span" -> sb.append(elementToMarkdown(node))
                    else -> sb.append(elementToMarkdown(node))
                }
            }

            is TextNode -> {
                val text = node.text()
                if (text.isNotBlank()) {
                    sb.append(text)
                }
            }
        }
    }

    return sb.toString()
}

private fun extractEquationTex(image: Element): String? {
    val eeimg = image.attr("eeimg")
    if (eeimg != "1" && eeimg != "2") return null
    val src = image.attr("src")
    if (src.isBlank()) return null
    val url = runCatching { Url(src) }.getOrNull() ?: return null
    if (url.host != "www.zhihu.com") return null
    if (url.segments.size != 1 || url.segments[0] != "equation") return null
    return url.parameters["tex"]
}
