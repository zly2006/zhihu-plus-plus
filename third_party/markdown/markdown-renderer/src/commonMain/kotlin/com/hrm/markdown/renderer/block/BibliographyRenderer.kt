package com.hrm.markdown.renderer.block

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hrm.markdown.parser.ast.BibliographyDefinition
import com.hrm.markdown.renderer.LocalMarkdownTheme
import com.hrm.markdown.renderer.internal.core.model.BibliographyDefinitionBlockModel
import com.hrm.markdown.renderer.internal.core.model.BibliographyEntryBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutBibliographyBlockModel

/**
 * 参考文献定义渲染器。
 */
@Composable
internal fun BibliographyDefinitionRenderer(
    node: BibliographyDefinition,
    modifier: Modifier = Modifier,
) {
    RenderBibliographyBlockModel(
        model = BibliographyDefinitionBlockModel(
            identity = com.hrm.markdown.renderer.internal.core.identity.RenderIdentity(
                stableId = node.stableKey.toLong(),
                contentRevision = node.contentHash,
                layoutRevision = node.contentHash,
                paintRevision = 0L,
            ),
            entries = node.entries.values.map { entry ->
                BibliographyEntryBlockModel(entry.key, entry.content)
            },
        ),
        modifier = modifier,
    )
}

@Composable
internal fun RenderBibliographyBlockModel(
    model: BibliographyDefinitionBlockModel,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    if (model.entries.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(theme.codeBlockBackground)
            .padding(12.dp),
    ) {
        Text(
            text = "References",
            style = theme.headingStyles.getOrElse(3) { theme.bodyStyle }.copy(
                fontWeight = FontWeight.Bold,
            ),
            modifier = Modifier.padding(bottom = 8.dp),
        )

        model.entries.forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
            ) {
                Text(
                    text = "[${entry.key}] ",
                    style = theme.bodyStyle.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = theme.linkColor,
                    ),
                )
                Text(
                    text = entry.content,
                    style = theme.bodyStyle,
                )
            }
        }
    }
}

@Composable
internal fun RenderBibliographyLayoutBlockModel(
    model: LayoutBibliographyBlockModel,
    modifier: Modifier = Modifier,
) {
    RenderBibliographyBlockModel(
        model = model.block,
        modifier = modifier,
    )
}
