# Markdown Maven 依赖

知乎使用独立开源库 [zly2006/Markdown](https://github.com/zly2006/Markdown)。依赖版本统一由根 `gradle.properties` 的 `markdown.version` 管理，Android app 与 shared 模块引用 `me.zly2006.markdown:markdown-renderer`。

renderer 的 API 依赖传递 parser、runtime、LaTeX 和 CodeHigh。应用不再维护这八个模块的 Gradle project，也不保留相应 `third_party` 源码。Markdown import 使用 `me.zly2006.markdown.*`；LaTeX 与高亮类型保留库本身的 `com.hrm.latex`、`com.hrm.codehigh` 包名。

## 更新流程

先在独立仓库完成修改与发布，确认 Central 状态为 `PUBLISHED`，并从公共 Maven 仓库读回根构件、平台构件和 metadata，再更新应用版本。不得用本机 Maven 仓库、composite build 或源码替代来证明线上构件已经可用。

更新后检查 Lite、Full、JVM 和 macOS 的实际依赖解析及构建。阅读与编辑功能共用 parser AST，提椠编译器、导出、段评和平台 Markdown 实现的 import 应同时迁移。需要业务运行验证时使用既有定向用例或离屏调试，不能只以依赖下载成功代替功能验收。

## 维护边界

库中的 LaTeX、CodeHigh 修改需要在独立 Markdown 工程同步验证并发布匹配版本。旧 `io.github.zly2006:latex-*` 发布线不能直接替代当前 `me.zly2006.markdown` 支持模块；同时引入两套相同 `com.hrm.*` 类型会产生重复类。

此前描述 vendored 目录或旧 Gradle project 任务的性能报告只作为历史证据保留。当前依赖关系及更新流程以本文件为准；库的源码来源和版权记录以独立仓库的 `docs/source-snapshot.json`、`NOTICE.md` 为准。
