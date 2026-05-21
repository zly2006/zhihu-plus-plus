# BlockByKeywordsDialog Boundary Review

开始时间：2026-05-21 16:04:53 CST
结束时间：2026-05-21 16:07:54 CST
审查方式：`gpt-5.5 xhigh` subagent 只读审查；未修改文件。

## 输入

- 目标文件：`shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/components/BlockByKeywordsDialog.kt`
- 当前 source set：`shared/androidMain`
- 调用方：`shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/HomeScreen.kt`
- 关键依赖：`BlockedKeywordRepository(context)`、`KeywordAnalyzer`、`KeywordWithWeight`、`Toast`、`LocalContext`、Compose Material3 / FlowRow。

## 结论

`BlockByKeywordsDialog` 需拆分：UI 结构、关键词选择状态和详情展示属于 shared；Android `Context`、Room builder、Toast 消息和 full/lite NLP 实现属于平台或 variant adapter。

`KeywordDetailDialog` 与 `KeywordInfoItem` 是纯 Compose 展示，目标可迁 `shared/commonMain`。`KeywordWithWeight` 已在 `shared/commonMain`。`KeywordAnalyzer` 不能按 app 当前形态直接迁 shared，因为它调用 variant `NLPService`；正确形态是 shared analyzer core + variant keyword extractor 注入。`BlockedKeywordRepository` 是 Android facade，核心持久化语义已经在 common `BlockedKeywordService`。

## 证据

- `BlockByKeywordsDialog.kt` 直接使用 `Toast`、`LocalContext` 和 `BlockedKeywordRepository(context)`。
- `BlockByKeywordsDialog.kt` 调用 app source set 的 `KeywordAnalyzer`，shared 模块无法访问。
- `shared/src/commonMain/kotlin/com/github/zly2006/zhihu/shared/nlp/NLPModels.kt` 定义 `KeywordWithWeight`。
- `app/src/main/java/com/github/zly2006/zhihu/nlp/KeywordAnalyzer.kt` 的加权文本构建、去重、停用词过滤、排序是纯逻辑，但 NLP 提取调用依赖 `NLPService`。
- full variant 的 `NLPService` 依赖 HanLP / sentence embedding；lite variant 是 fallback。它们不能迁入 shared。
- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/nlp/BlockedKeywordRepository.kt` 依赖 Android `Context` 和 Android Room builder；`BlockedKeywordService.addNLPPhrase()` 已在 common。

## 风险

不能用空实现绕过关键词提取或屏蔽写入。full 必须注入真实 `NLPService.extractKeywordsWithWeight`；lite 保持当前 fallback。desktop 不接 `sentence_embeddings`，但 common UI 和关键词选择语义不能消失。

## 最小步骤

1. 改用 shared `KeywordWithWeight`。
2. 新增 common `KeywordWeightExtractor` 与 `KeywordAnalyzerCore`，迁入加权文本、去重、停用词过滤和排序。
3. Android app 向 shared runtime 注入 full/lite `NLPService.extractKeywordsWithWeight`。
4. 用 `UserMessageSink` 替换直接 `Toast`。
5. 当前切片保留 Android wrapper 写入 `BlockedKeywordRepository(context)`；后续再抽 common `BlockByKeywordsDialogContent`，让 Android wrapper 仅传 `addNlpPhrase`。

## 验证

```bash
rg -n "com\\.github\\.zly2006\\.zhihu\\.nlp\\.KeywordAnalyzer|com\\.github\\.zly2006\\.zhihu\\.nlp\\.KeywordWithWeight|android\\.widget\\.Toast|LocalContext|BlockedKeywordRepository\\(" shared/src/commonMain/kotlin shared/src/androidMain/kotlin -g '*.kt'
rg -n "KeywordWeightExtractor|extractFromFeedWithWeight|NLPService\\.extractKeywordsWithWeight|AndroidContentFilterRuntime|KeywordSemanticMatcher" shared/src app/src -g '*.kt'
./gradlew :shared:compileAndroidMain
./gradlew :shared:jvmTest :desktopApp:compileKotlin :app:compileLiteDebugKotlin
./gradlew assembleLiteDebug
./gradlew ktlintFormat
```
