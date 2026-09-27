# Android Full/Lite 的 KMP 构建

`app` 保留 `full` 和 `lite` product flavor。`shared` 使用的 Android KMP 插件只提供一份 Android 主编译，因此每次 Gradle 调用通过 `zhihu.androidVariant` 选择一套 `actual`：

```bash
./gradlew -Pzhihu.androidVariant=lite :app:assembleLiteDebug
./gradlew -Pzhihu.androidVariant=full :app:assembleFullDebug
```

产物分别位于 `app/build/outputs/apk/lite/debug/` 和 `app/build/outputs/apk/full/debug/`。Release 包使用同样的参数，任务名改为 `assembleLiteRelease` 或 `assembleFullRelease`。

构建两个 flavor 时必须执行两次 Gradle 命令。`shared/src/androidMain` 放两版共用的 Android 代码；`shared/src/androidFull` 和 `shared/src/androidLite` 分别提供版本专属的 `actual`。构建脚本会检查显式请求的 APK flavor 与选择参数是否一致。仅运行非 Android 任务时可以不传选择参数。

Android 单元测试同样要按 flavor 分开运行，例如 `-Pzhihu.androidVariant=full :app:testFullDebugUnitTest` 和 `-Pzhihu.androidVariant=lite :app:testLiteDebugUnitTest`。全局 `test` 任务会同时调度两套 Android 单元测试，不能在单次调用中使用。
