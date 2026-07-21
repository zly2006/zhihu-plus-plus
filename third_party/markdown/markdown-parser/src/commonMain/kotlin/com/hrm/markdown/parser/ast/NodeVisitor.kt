package com.hrm.markdown.parser.ast

/**
 * 用于遍历 AST 节点的访问者接口。
 */
interface NodeVisitor<R> {
    // 块级节点
    fun visitDocument(node: Document): R
    fun visitHeading(node: Heading): R
    fun visitSetextHeading(node: SetextHeading): R
    fun visitParagraph(node: Paragraph): R
    fun visitThematicBreak(node: ThematicBreak): R
    fun visitFencedCodeBlock(node: FencedCodeBlock): R
    fun visitIndentedCodeBlock(node: IndentedCodeBlock): R
    fun visitBlockQuote(node: BlockQuote): R
    fun visitListBlock(node: ListBlock): R
    fun visitListItem(node: ListItem): R
    fun visitHtmlBlock(node: HtmlBlock): R
    fun visitLinkReferenceDefinition(node: LinkReferenceDefinition): R
    fun visitTable(node: Table): R
    fun visitTableHead(node: TableHead): R
    fun visitTableBody(node: TableBody): R
    fun visitTableRow(node: TableRow): R
    fun visitTableCell(node: TableCell): R
    fun visitFootnoteDefinition(node: FootnoteDefinition): R
    fun visitMathBlock(node: MathBlock): R
    fun visitDefinitionList(node: DefinitionList): R
    fun visitDefinitionTerm(node: DefinitionTerm): R
    fun visitDefinitionDescription(node: DefinitionDescription): R
    fun visitAdmonition(node: Admonition): R
    fun visitFrontMatter(node: FrontMatter): R
    fun visitNativeBlock(node: NativeBlock): R
    fun visitBlankLine(node: BlankLine): R
    fun visitTocPlaceholder(node: TocPlaceholder): R
    fun visitAbbreviationDefinition(node: AbbreviationDefinition): R

    fun visitCustomContainer(node: CustomContainer): R
    fun visitDiagramBlock(node: DiagramBlock): R
    fun visitColumnsLayout(node: ColumnsLayout): R
    fun visitColumnItem(node: ColumnItem): R
    fun visitPageBreak(node: PageBreak): R

    // 行内节点
    fun visitText(node: Text): R
    fun visitSoftLineBreak(node: SoftLineBreak): R
    fun visitHardLineBreak(node: HardLineBreak): R
    fun visitEmphasis(node: Emphasis): R
    fun visitStrongEmphasis(node: StrongEmphasis): R
    fun visitStrikethrough(node: Strikethrough): R
    fun visitInlineCode(node: InlineCode): R
    fun visitLink(node: Link): R
    fun visitImage(node: Image): R
    fun visitAutolink(node: Autolink): R
    fun visitInlineHtml(node: InlineHtml): R
    fun visitHtmlEntity(node: HtmlEntity): R
    fun visitEscapedChar(node: EscapedChar): R
    fun visitFootnoteReference(node: FootnoteReference): R
    fun visitInlineMath(node: InlineMath): R
    fun visitHighlight(node: Highlight): R
    fun visitSuperscript(node: Superscript): R
    fun visitSubscript(node: Subscript): R
    fun visitInsertedText(node: InsertedText): R
    fun visitEmoji(node: Emoji): R
    fun visitStyledText(node: StyledText): R
    fun visitAbbreviation(node: Abbreviation): R
    fun visitKeyboardInput(node: KeyboardInput): R
    fun visitDirectiveBlock(node: DirectiveBlock): R
    fun visitDirectiveInline(node: DirectiveInline): R
    fun visitTabBlock(node: TabBlock): R
    fun visitTabItem(node: TabItem): R
    fun visitBibliographyDefinition(node: BibliographyDefinition): R
    fun visitCitationReference(node: CitationReference): R
    fun visitSpoiler(node: Spoiler): R
    fun visitWikiLink(node: WikiLink): R
    fun visitRubyText(node: RubyText): R
    fun visitFigure(node: Figure): R
}

/**
 * 默认访问者，为所有节点返回默认值。
 * 当只需要处理少量节点类型时非常有用。
 */
abstract class DefaultNodeVisitor<R>(private val defaultValue: R) : NodeVisitor<R> {
    override fun visitDocument(node: Document): R = defaultValue
    override fun visitHeading(node: Heading): R = defaultValue
    override fun visitSetextHeading(node: SetextHeading): R = defaultValue
    override fun visitParagraph(node: Paragraph): R = defaultValue
    override fun visitThematicBreak(node: ThematicBreak): R = defaultValue
    override fun visitFencedCodeBlock(node: FencedCodeBlock): R = defaultValue
    override fun visitIndentedCodeBlock(node: IndentedCodeBlock): R = defaultValue
    override fun visitBlockQuote(node: BlockQuote): R = defaultValue
    override fun visitListBlock(node: ListBlock): R = defaultValue
    override fun visitListItem(node: ListItem): R = defaultValue
    override fun visitHtmlBlock(node: HtmlBlock): R = defaultValue
    override fun visitLinkReferenceDefinition(node: LinkReferenceDefinition): R = defaultValue
    override fun visitTable(node: Table): R = defaultValue
    override fun visitTableHead(node: TableHead): R = defaultValue
    override fun visitTableBody(node: TableBody): R = defaultValue
    override fun visitTableRow(node: TableRow): R = defaultValue
    override fun visitTableCell(node: TableCell): R = defaultValue
    override fun visitFootnoteDefinition(node: FootnoteDefinition): R = defaultValue
    override fun visitMathBlock(node: MathBlock): R = defaultValue
    override fun visitDefinitionList(node: DefinitionList): R = defaultValue
    override fun visitDefinitionTerm(node: DefinitionTerm): R = defaultValue
    override fun visitDefinitionDescription(node: DefinitionDescription): R = defaultValue
    override fun visitAdmonition(node: Admonition): R = defaultValue
    override fun visitFrontMatter(node: FrontMatter): R = defaultValue
    override fun visitNativeBlock(node: NativeBlock): R = defaultValue
    override fun visitBlankLine(node: BlankLine): R = defaultValue
    override fun visitTocPlaceholder(node: TocPlaceholder): R = defaultValue
    override fun visitAbbreviationDefinition(node: AbbreviationDefinition): R = defaultValue
    override fun visitCustomContainer(node: CustomContainer): R = defaultValue
    override fun visitDiagramBlock(node: DiagramBlock): R = defaultValue
    override fun visitColumnsLayout(node: ColumnsLayout): R = defaultValue
    override fun visitColumnItem(node: ColumnItem): R = defaultValue
    override fun visitPageBreak(node: PageBreak): R = defaultValue
    override fun visitText(node: Text): R = defaultValue
    override fun visitSoftLineBreak(node: SoftLineBreak): R = defaultValue
    override fun visitHardLineBreak(node: HardLineBreak): R = defaultValue
    override fun visitEmphasis(node: Emphasis): R = defaultValue
    override fun visitStrongEmphasis(node: StrongEmphasis): R = defaultValue
    override fun visitStrikethrough(node: Strikethrough): R = defaultValue
    override fun visitInlineCode(node: InlineCode): R = defaultValue
    override fun visitLink(node: Link): R = defaultValue
    override fun visitImage(node: Image): R = defaultValue
    override fun visitAutolink(node: Autolink): R = defaultValue
    override fun visitInlineHtml(node: InlineHtml): R = defaultValue
    override fun visitHtmlEntity(node: HtmlEntity): R = defaultValue
    override fun visitEscapedChar(node: EscapedChar): R = defaultValue
    override fun visitFootnoteReference(node: FootnoteReference): R = defaultValue
    override fun visitInlineMath(node: InlineMath): R = defaultValue
    override fun visitHighlight(node: Highlight): R = defaultValue
    override fun visitSuperscript(node: Superscript): R = defaultValue
    override fun visitSubscript(node: Subscript): R = defaultValue
    override fun visitInsertedText(node: InsertedText): R = defaultValue
    override fun visitEmoji(node: Emoji): R = defaultValue
    override fun visitStyledText(node: StyledText): R = defaultValue
    override fun visitAbbreviation(node: Abbreviation): R = defaultValue
    override fun visitKeyboardInput(node: KeyboardInput): R = defaultValue
    override fun visitDirectiveBlock(node: DirectiveBlock): R = defaultValue
    override fun visitDirectiveInline(node: DirectiveInline): R = defaultValue
    override fun visitTabBlock(node: TabBlock): R = defaultValue
    override fun visitTabItem(node: TabItem): R = defaultValue
    override fun visitBibliographyDefinition(node: BibliographyDefinition): R = defaultValue
    override fun visitCitationReference(node: CitationReference): R = defaultValue
    override fun visitSpoiler(node: Spoiler): R = defaultValue
    override fun visitWikiLink(node: WikiLink): R = defaultValue
    override fun visitRubyText(node: RubyText): R = defaultValue
    override fun visitFigure(node: Figure): R = defaultValue
}
