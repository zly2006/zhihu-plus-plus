---
name: release-latex-fork
description: Maintain the legacy standalone LaTeX fork when explicitly requested. Zhihu now consumes Markdown and its supporting modules through the published me.zly2006.markdown dependency chain.
---

# LaTeX 分叉与当前应用依赖

当前知乎的依赖关系和更新流程以仓库内 `docs/markdown-library-dependency.md` 为准。Markdown、LaTeX 和 CodeHigh 由独立 Markdown 工程统一构建并使用匹配版本发布，应用不再保留对应的 Gradle project 或源码副本。

## 执行边界

- 用户要求更新应用阅读库时，先核对 `me.zly2006.markdown` 实际发布版本和平台构件，再更新应用的 `markdown.version`。
- 不得把旧 `io.github.zly2006:latex-*` 版本直接混入当前依赖链，避免出现同包名重复类。
- 用户明确要求维护独立的旧 LaTeX 发布线时，先从其真实仓库核对版本、版权和消费方；不能假定它仍是当前知乎的依赖。
- 保留现有字体裁剪与系统字体回退的产品行为；任何源码基线或字体行为改变都需要真实验证。
- Maven 发布必须跟踪 deployment 至终态，并读回公共构件。上传成功、等待分发或本地构建成功均不代表发布完成。
- 不得委派未明确支持用户批准成本配置的被动发布等待任务；由发布执行者持续跟踪状态。
- 修改代码或依赖后按项目规则先构建、再格式化、再审查；不得覆盖工作树中的其他修改。
