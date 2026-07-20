package com.hrm.markdown.runtime

/**
 * Markdown 运行时预处理管线。
 */
class MarkdownDirectivePipeline(
    private val registry: MarkdownDirectiveRegistry,
) {
    val supportsStreamingFastPath: Boolean
        get() = registry.supportsStreamingFastPath

    fun transform(input: String): MarkdownTransformResult {
        var markdown = input
        var sourceMap: MarkdownSourceMap = MarkdownSourceMap.Identity

        for (transformer in registry.inputTransformers()) {
            val result = transformer.transform(markdown)
            markdown = result.markdown
            sourceMap = composeSourceMap(sourceMap, result.sourceMap)
        }

        return MarkdownTransformResult(
            markdown = markdown,
            sourceMap = sourceMap,
        )
    }
}
