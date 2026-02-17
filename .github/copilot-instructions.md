# Zhihu++ Copilot Instructions

隐私增强的知乎 Android 客户端，支持本地推荐算法、广告屏蔽、内容过滤。

## 构建与测试

```bash
# 验证修改（必须按顺序执行）
./gradlew assembleLiteDebug  # 构建 lite 变体
./gradlew ktlintFormat        # 格式化代码
```

**重要**: 修改后必须先构建验证，再格式化，最后提交。

## 项目结构

- **app**: 主应用（Jetpack Compose UI）
    - `src/main`: 共享代码
    - `src/full`: Full variant（含 NLP）
    - `src/lite`: Lite variant（轻量级）
- **Module**: `sentence_embeddings`（Rust tokenizer，仅 full variant）

### Build Variants
- **lite**: 轻量版 (~3MB)，无 ML 功能，包名 `com.github.zly2006.zhplus.lite`
- **full**: 完整版，含 HanLP NLP，包名 `com.github.zly2006.zhplus`

## 关键约定

### 数据序列化
- **DataHolder** 和 data classes 使用 `camelCase`
- **知乎 API** 返回 `snake_case`
- **自动转换**: `AccountData.fetch*()` 和 `decodeJson()` 内部自动调用 `snake_case2camelCase()`
- 不要手动转换或在 data class 中使用 snake_case

### HTTP 客户端
- 使用 `AccountData.httpClient(context)` 获取配置好的客户端
- Web API 需要 `signFetchRequest(context)` 用于 zse96 v2 签名
- Android API 使用 `AccountData.ANDROID_HEADERS` 和 `ANDROID_USER_AGENT`

### Compose
- Material 3 组件
- 用 `LaunchedEffect` 处理副作用，设置正确的 key
- 用 `collectAsState()` 观察 Flow/StateFlow

## Android 调试标准流程

### 应用启动与验证
```bash
# 1. 检查包名（必须先做）
grep "applicationId" app/build.gradle.kts
# lite variant: com.github.zly2006.zhplus.lite

# 2. 构建并安装
./gradlew assembleLiteDebug
adb install -r app/build/outputs/apk/lite/debug/app-lite-debug.apk

# 3. 启动
adb shell am force-stop com.github.zly2006.zhplus.lite
adb shell monkey -p com.github.zly2006.zhplus.lite -c android.intent.category.LAUNCHER 1

# 4. 等待加载（关键！）
sleep 10

# 5. 截图验证
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png /tmp/screenshot.png
# 使用 view 工具查看

# 6. 测试交互
adb shell input swipe 350 800 350 400  # 上滑
sleep 1
adb shell screencap -p /sdcard/screenshot2.png
adb pull /sdcard/screenshot2.png /tmp/screenshot2.png
```

### UI 调试强制清单
修改 UI 代码后**必须**：
1. ✅ 构建 + 格式化
2. ✅ 安装到设备
3. ✅ 正确启动应用（检查包名！）
4. ✅ 等待加载完成（至少 8-10 秒）
5. ✅ 截图并 adb pull 到本地，注意不要在一个response中同时包括截图和第七步的Read命令，因为截图需要时间，此时要read的文件还不存在。
6. ✅ 使用 Read 工具查看，记得要用ls检查文件是否存在，路径是否正确
7. ✅ 测试交互（滚动、点击）
8. ✅ 再次截图验证
9. ❌ 不对则检查 logcat：`adb logcat | grep -i error`

## 代码风格
- Kotlin Serialization with `@Serializable`
- 只在必要时注释，不过度注释
- ktlint 格式化（14.0.1）

## License
自定义许可证（非自由软件）- 见 LICENSE.md
