# 第三方阅读器源码与许可

本工程从 Maven 发布的 sources.jar 直接构建 Android 使用的同版本源码，不下载浮动分支、不替换本地 klib。

| 模块 | 发布坐标 | 上游 |
| --- | --- | --- |
| markdown-parser / runtime / renderer | io.github.zly2006，0.0.1-alpha.11 | https://github.com/zly2006/Markdown |
| latex-base / parser / renderer | io.github.zly2006，1.4.6-zly | https://github.com/zly2006/latex |
| codehighlight-parser / render | io.github.huarangmeng，1.1.1 | https://github.com/huarangmeng/codehigh |

三组组件的发布元数据均声明 MIT License。Markdown 与 LaTeX 的原始 LICENSE 声明如下；CodeHighlight 的许可证声明可核对 Maven Central 同版本 POM（仓库 LICENSE 链接在核验时返回 404）。

OHOS 适配：
- 为八个模块增加 ohosArm64 / ohosX64。
- LaTeX 复用 upstream iosMain 中只依赖 Skia 的 glyph bounds / export 实现；平台标识新增 OHOS。
- CodeHighlight 使用 Compose Locale 提供 OHOS locale。
- Markdown 保留图片插槽契约；arm64 注入 CPF Coil，x64 注入 NetworkKit/Skia，因为 CPF Coil 未发布 OHOS x64 变体。
- 不包含上游生成的 Compose Res 声明；这些版本的渲染路径不需要随包字体资源。

MIT 原文及归属保留在 [随 HAP 打包的许可文件](harmonyApp/entry/src/main/resources/rawfile/third-party-readers.txt)。
