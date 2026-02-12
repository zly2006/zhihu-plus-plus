package com.github.zly2006.zhihu.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.resolveContent
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher

sealed interface AstData

class MdAstLinks(
    val parent: MdAst? = null,
    val prev: MdAst? = null,
    val next: MdAst? = null,
    val firstChild: MdAst? = null,
    val lastChild: MdAst? = null,
)

class MdAst(
    val data: AstData,
    val links: MdAstLinks,
)

sealed interface InlineAstData

class AstSpan(
    val text: String,
    val textStyle: TextStyle,
) : InlineAstData

class AstHeader(
    val text: InlineAstData,
    val level: Int,
) : AstData

class AstParagraph(
    val inlines: List<InlineAstData>,
) : AstData

class AstBlockquote(
    val children: List<MdAst>,
) : AstData

class AstCodeBlock(
    val code: String,
    val language: String?,
) : AstData

class AstListItem(
    val children: List<MdAst>,
) : AstData

class AstList(
    val items: List<AstListItem>,
    val ordered: Boolean,
) : AstData

object AstHorizontalRule : AstData

class AstImage(
    val url: String,
    val altText: String,
) : AstData

class AstLink(
    val url: String,
    val title: InlineAstData,
) : InlineAstData

class AstCodeSpan(
    val code: String,
) : InlineAstData

class AstLineBreak : InlineAstData

class AstInlineMath(
    val math: String,
) : InlineAstData {
    override fun toString(): String {
        return "AstInlineMath(math='$math')"
    }
}

class AstDisplayMath(
    val math: String,
) : AstData

class AstTable(
    val headers: List<List<InlineAstData>>,
    val rows: List<List<List<InlineAstData>>>,
) : AstData

class AstTableAlignments(
    val alignments: List<Alignment>,
) : AstData {
    enum class Alignment {
        LEFT,
        CENTER,
        RIGHT,
        NONE,
    }
}

class MarkdownRenderContext(
    val requestedImages: MutableSet<AstImage>,
    val requestedFormulas: MutableSet<AstInlineMath>,
    val requestedFormulaBlocks: MutableSet<AstDisplayMath>,
)

@Composable
fun AnnotatedString.Builder.renderInline(
    ast: InlineAstData,
    renderContext: MarkdownRenderContext,
    onNavigate: (NavDestination) -> Unit,
    ) {
    val context = LocalContext.current
    when (val d = ast) {
        is AstSpan -> {
            pushStyle(d.textStyle.toSpanStyle())
            append(d.text)
            pop()
        }

        is AstLink -> {
            withLink(
                LinkAnnotation.Clickable(
                    d.url,
                    TextLinkStyles(style = SpanStyle(color = Color(0xff66CCFF))),
                ) {
                    resolveContent(d.url.toUri())?.let(onNavigate)
                        ?: luoTianYiUrlLauncher(context, d.url.toUri())
                },
            ) {
                renderInline(d.title, renderContext, onNavigate)
            }
        }

        is AstCodeSpan -> {
            pushStyle(TextStyle(
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                ).toSpanStyle())
            append(d.code)
            pop()
        }

        is AstLineBreak -> {
            append("\n")
        }

        is AstInlineMath -> {
            renderContext.requestedFormulas.add(d)
            appendInlineContent(d.toString(), d.math)
        }
    }
}

@Composable
fun MdAst.render(
    renderContext: MarkdownRenderContext,
    onNavigate: (NavDestination) -> Unit,) {
    when (val d = data) {
        is AstHeader -> {
            Text(
                text = buildAnnotatedString {
                    renderInline(d.text, renderContext, onNavigate)
                },
                style = when (d.level) {
                    1 -> MaterialTheme.typography.headlineLarge
                    2 -> MaterialTheme.typography.headlineMedium
                    3 -> MaterialTheme.typography.headlineSmall
                    4 -> MaterialTheme.typography.titleLarge
                    5 -> MaterialTheme.typography.titleMedium
                    else -> MaterialTheme.typography.titleSmall
                },
            )
        }

        is AstParagraph -> {
            Text(
                text = buildAnnotatedString {
                    d.inlines.forEach {
                        renderInline(it, renderContext, onNavigate)
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        is AstBlockquote -> {
            Row {
                Box(
                    Modifier
                        .padding(
                            start = 6.dp,
                            end = 6.dp,
                        )
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.onBackground, RoundedCornerShape(50))
                )
                Text(
                    text = buildAnnotatedString {
                        d.children.forEach {
                            it.render(renderContext, onNavigate)
                        }
                    },
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }

        is AstCodeBlock -> {
            Text(
                text = d.code,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    background = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                ),
            )
        }
        is AstDisplayMath -> {
            renderContext.requestedFormulaBlocks.add(d)
            Text(
                text = d.math,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        AstHorizontalRule -> {
            Box(
                Modifier
                    .padding(vertical = 8.dp)
                    .height(3.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            )
        }
        is AstImage -> {
            AsyncImage(
                d.url,
                contentDescription = d.altText,
            )
        }
        is AstList -> {}
        is AstListItem -> {}
        is AstTable -> {}
        is AstTableAlignments -> {}
    }
}
