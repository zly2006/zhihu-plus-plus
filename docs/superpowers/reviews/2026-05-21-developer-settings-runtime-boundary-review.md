# Developer Settings Runtime Boundary Review

## 输入

- `shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/subscreens/DeveloperSettingsScreen.kt`
- `app/src/main/java/com/github/zly2006/zhihu/MainActivity.kt`
- `ArticleHost` / `TtsState`
- continuous usage reminder runtime

## 结论

`DeveloperSettingsScreen` 是需拆分的 shared 页面，不是 Android-only。页面结构、开发者模式开关语义、TTS 状态展示语义、连续使用时长展示模型、导航到句子相似度/配色页的路由语义都应 shared。Android `TextToSpeech` runtime、engine 实例/包名细节、Activity foreground lifecycle、Toast、SharedPreferences、ConnectivityManager、PowerSaveModeCompat、clipboard、Android `AccountData` 调用属于平台副作用。

TTS 的根本边界是：`TtsState`、朗读请求/状态语义应 shared；Android `TextToSpeech`、iOS `AVSpeechSynthesizer`、desktop no-op/disabled 是各自平台 adapter。不能因为 desktop 暂无 TTS 就把 TTS 状态判成 Android-only。

## 当前最小修复

当前 blocker 是 shared/androidMain 反向 import app `MainActivity` 并直接读取 `ttsEngine`、`textToSpeech`、`currentContinuousUsageDurationMs()`。

本切片新增窄接口：

- `DeveloperRuntimeInfo`
- `DeveloperRuntimeInfoProvider`

`DeveloperSettingsScreen` 只读取这个接口；`MainActivity` 实现接口并把 Android runtime 细节转换成 common `TtsState`、engine label、engine list 和 continuous usage duration。

不 common 化整页，不搬 Android TTS runtime，不新增 shared Pico/Google/Sherpa enum。

## 验证命令

```bash
rg -n "com\\.github\\.zly2006\\.zhihu\\.MainActivity|MainActivity\\.TtsEngine|textToSpeech|ttsEngine|currentContinuousUsageDurationMs" shared/src/androidMain/kotlin/com/github/zly2006/zhihu/ui/subscreens/DeveloperSettingsScreen.kt
./gradlew :shared:compileAndroidMain --continue
git diff --check
```
