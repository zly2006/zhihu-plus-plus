package com.hrm.markdown.parser.flavour

import com.hrm.markdown.parser.block.postprocessors.*
import com.hrm.markdown.parser.block.starters.*

/**
 * PHP Markdown Extra compatible flavour.
 *
 * based on the [PHP Markdown Extra](https://michelf.ca/projects/php-markdown/extra/)
 * specification. extends CommonMark with:
 * - definition lists (`Term\n: Definition`)
 * - abbreviations (`*[HTML]: HyperText Markup Language`)
 * - footnotes (`[^1]: footnote content`)
 * - fenced code blocks (already in CommonMark)
 * - tables
 *
 * intentionally does NOT include:
 * - custom containers (`::: type ... :::`)
 * - math blocks (`$$ ... $$`) or inline math (`$...$`)
 * - emoji shortcodes (`:smile:`)
 * - directives (`{% tag %}`)
 * - front matter
 * - page breaks
 * - admonitions
 * - highlight, superscript, subscript, inserted text
 *
 * ## usage
 *
 * ```kotlin
 * val parser = MarkdownParser(MarkdownExtraFlavour)
 * val doc = parser.parse("""
 *     Term
 *     : Definition
 *
 *     *[HTML]: HyperText Markup Language
 *
 *     The HTML specification is maintained by the W3C.
 *
 *     [^1]: This is a footnote.
 *
 *     | Name | Age |
 *     | ---- | --- |
 *     | Bob  | 30  |
 * """.trimIndent())
 * ```
 */
object MarkdownExtraFlavour : MarkdownFlavour {

    /**
     * block starters for PHP Markdown Extra.
     *
     * includes CommonMark core starters plus:
     * - tables (200)
     * - footnote definitions (510)
     * - definition descriptions (520)
     *
     * does not include: FrontMatter, CustomContainer, MathBlock,
     * PageBreak, DirectiveBlock.
     */
    override val blockStarters: List<BlockStarter> = listOf(
        SetextHeadingStarter(),        // 100
        HeadingStarter(),              // 110
        TableStarter(),                // 200
        ThematicBreakStarter(),        // 210
        FencedCodeBlockStarter(),      // 310
        HtmlBlockStarter(),            // 400
        BlockQuoteStarter(),           // 410
        ListItemStarter(),             // 500
        FootnoteDefinitionStarter(),   // 510
        DefinitionDescriptionStarter(), // 520
        IndentedCodeBlockStarter(),    // 600
    )

    /**
     * post processors for PHP Markdown Extra.
     *
     * includes:
     * - heading id generation (100)
     * - block attribute parsing (150)
     * - abbreviation replacement (200)
     *
     * does not include: DiagramProcessor, ColumnsLayoutProcessor.
     */
    override val postProcessors: List<PostProcessor> = listOf(
        HeadingIdProcessor(),          // 100
        BlockAttributeProcessor(),     // 150
        AbbreviationProcessor(),       // 200
    )

    /** gfm autolinks are not part of PHP Markdown Extra */
    override val enableGfmAutolinks: Boolean = false

    /** extended inline syntax (math, emoji, highlight, etc.) is not part of PHP Markdown Extra */
    override val enableExtendedInline: Boolean = false
}
