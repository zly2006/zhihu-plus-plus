package com.hrm.markdown.renderer.internal.core.compile

import com.hrm.markdown.parser.ast.Node
import com.hrm.markdown.renderer.internal.core.identity.RenderIdentity
import com.hrm.markdown.renderer.internal.core.model.InternalRenderBlockModel
import com.hrm.markdown.renderer.internal.core.model.InternalRenderDocumentMetadata

internal class RenderBlockCatalog(
    val documentIdentity: RenderIdentity,
    private val nodes: List<Node>,
    val metadata: InternalRenderDocumentMetadata,
    private val compileNode: (Node) -> InternalRenderBlockModel?,
) {
    private val blocks = arrayOfNulls<InternalRenderBlockModel>(nodes.size)
    private val compiled = BooleanArray(nodes.size)
    private val indexByStableId = nodes.indices.associateBy { blockStableId(nodes[it]) }

    val size: Int
        get() = nodes.size

    val compiledBlockCount: Int
        get() = compiled.count { it }

    fun nodeAt(index: Int): Node = nodes[index]

    fun identityAt(index: Int): RenderIdentity {
        val node = nodes[index]
        return RenderIdentity(
            stableId = blockStableId(node),
            contentRevision = node.contentHash,
            layoutRevision = node.contentHash,
            paintRevision = 0L,
        )
    }

    fun itemIndexOf(stableId: Long): Int? = indexByStableId[stableId]

    fun evict(index: Int) {
        blocks[index] = null
        compiled[index] = false
    }

    fun compile(index: Int): InternalRenderBlockModel? {
        if (!compiled[index]) {
            blocks[index] = compileNode(nodes[index])
            compiled[index] = true
        }
        return blocks[index]
    }
}
