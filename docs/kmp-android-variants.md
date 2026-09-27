# Android Full/Lite 的 KMP 构建

`app` 保留 `full` 和 `lite` product flavor。`shared` 使用的 Android KMP 插件只提供一份 Android 主编译。构建脚本会从 APK 任务名推断本次要编译的 `actual`：

```bash
./gradlew :app:assembleLiteDebug
./gradlew :app:assembleFullDebug
```

产物分别位于 `app/build/outputs/apk/lite/debug/` 和 `app/build/outputs/apk/full/debug/`。Release 包使用 `assembleLiteRelease` 或 `assembleFullRelease`。

构建两个 flavor 时必须执行两次 Gradle 命令。`shared/src/androidMain` 放两版共用的 Android 代码；`shared/src/androidFull` 和 `shared/src/androidLite` 分别提供版本专属的 `actual`。构建脚本也识别同一 flavor 的 bundle、install、compile 和单元测试任务。无法从任务名判断版本的 Android 任务不会自动选择 `actual`，应改用对应 flavor 的任务。

Android 单元测试同样要按 flavor 分开运行，例如 `:app:testFullDebugUnitTest` 和 `:app:testLiteDebugUnitTest`。全局 `test` 任务会同时调度两套 Android 单元测试，不能在单次调用中使用。
