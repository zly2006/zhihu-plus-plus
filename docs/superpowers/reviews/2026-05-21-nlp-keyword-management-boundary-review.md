# NLPKeywordManagementScreen KMP 边界审查

日期：2026-05-21

## 输入

- 编译错误：`shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/BlocklistSettingsScreen.kt` 引用 `NLPKeywordManagementScreen`，但该页面只存在于 `app/src/full` 和 `app/src/lite` 变体 source set。
- 目标：判断 NLP 关键词管理页面、blocklist NLP UI、NLP engine 和持久化边界。

## 结论

`NLPKeywordManagementScreen` 需要拆分，不能直接从 `shared/androidMain` 引用 app 变体页面。

- `shared/commonMain`：NLP blocklist 的数据模型、Room entity/DAO/service、短语 CRUD、屏蔽记录、匹配结果序列化、`KeywordSemanticMatcher` / `KeywordWeightExtractor` 接口、纯 Compose content 壳。
- `shared/androidMain`：Android database builder、`Context` 到数据库/设置的 adapter、Toast/log/content detail fetch 等 Android 副作用。
- `app` full/lite variant：真实 NLP engine、模型加载、`sentence_embeddings`、HanLP、ONNX Runtime Android、full/lite 的功能差异页面入口。
- JVM/desktop：当前不承载真实 NLP engine；可以接 shared UI 的禁用态或空能力，不能引入 WebView 或 Android NLP 实现。

## 证据

- `BlocklistSettingsScreen` 位于 `shared/androidMain`，不能依赖 `app/src/full` 或 `app/src/lite`。
- full `NLPKeywordManagementScreen` 使用 `LocalContext`、`Toast`、`NLPService`、`SentenceEmbeddingManager`。
- lite `NLPKeywordManagementScreen` 是变体 fallback 页面。
- `BlockedKeywordService`、`BlockedKeywordDao`、`BlockedKeyword`、`BlockedContentRecord`、`KeywordAnalyzerCore`、`KeywordWeightExtractor`、`KeywordSemanticMatcher` 已经具备 shared/common 边界。
- Android app 已通过 runtime 注入 NLP matcher/extractor，说明 engine 能力不应反向进入 shared。

## 依赖与替代

Compose、Navigation、Lifecycle、Room 已有 KMP 依赖，可以用于 shared UI 和数据层。HanLP 当前项目使用 Java/JAR 风格依赖，ONNX Runtime 当前是 Android AAR，`sentence_embeddings` 是 Android/JNI 模块，这些不能直接放进 `shared/commonMain`。

## 平台副作用

Toast 提示、`Context` 获取 DB/SharedPreferences、模型文件路径和下载、`SentenceEmbeddingManager.setDefaultContext/ensureModel/unload`、Android log、`ContentDetailCache.getOrFetch(context, ...)`、导入导出 `Intent/FileProvider` 都是平台副作用，不应决定整页所有权。

## 最小步骤

1. `BlocklistSettingsScreen` 通过一个明确的 `BlocklistSettingsNlpContent` slot 接收 NLP 页面内容。
2. Android app full/lite 在 `ZhihuMainPlatformAdapter` 注入真实 `NLPKeywordManagementScreen`。
3. shared/androidMain 只保留 fallback 禁用态和测试注入，不直接引用 app 变体页面。
4. 后续再抽 `NlpKeywordManagementContent` 到 `shared/commonMain`，full/lite wrapper 只负责注入 engine/model controller/Toast。

## 验证

```bash
rg -n "NLPKeywordManagementScreen|SentenceEmbeddingManager|NLPService|sentence_embeddings|HanLP|onnxruntime|LocalContext|Toast" shared/src/commonMain shared/src/androidMain app/src/full app/src/lite -g '*.kt'
./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :app:compileLiteDebugKotlin :app:compileFullDebugKotlin
```
