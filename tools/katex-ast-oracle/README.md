# KaTeX AST oracle

该工具使用固定版本 KaTeX 的内部 `__parse` API，为知乎真实公式语料生成解析语义基准。输出只包含公式哈希、解析状态和归一化结构特征，不包含正文。

KaTeX 本身不支持 MathJax 的 `\bbox`，生成器仅注册一个不参与渲染的 parse-only 节点，使其内容仍能参与 AST 对比。知乎原始公式中奇数个末尾反斜杠属于已知脏数据；送入 KaTeX 前只移除最后一个孤立反斜杠，并由 Kotlin 测试单独锁定诊断行为。

```bash
npm ci
npm run generate
```

生成后必须运行 `ZhihuFormulaCorpusTest`，不能只更新 oracle 文件。
