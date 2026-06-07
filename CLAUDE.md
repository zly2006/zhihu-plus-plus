# Zhihu++ Agent Instructions

本项目是隐私增强的知乎 Android 客户端，支持本地推荐算法、广告屏蔽、内容过滤。

## 经验总结

### CI 修复完成条件

修复 CI 时不能在本地验证不完整或远程检查仍在运行时宣布完成。即使本地测试因为模拟器卡住无法给出结论，也必须继续跟踪 GitHub Actions 的最新 run，拿到明确通过或新的失败日志后再决定下一步。例子：一个分页测试的本地单用例进入 instrumentation 后长时间无输出，不能因此把“已推送、等待 CI”当作任务完成；应该持续查看对应 job 日志，确认失败断言是否消失或定位新的失败点。

### Instrument test 验证边界

除非用户明确要求，本地不要跑完整的 instrument test；全量 Android instrument test 应交给 GitHub CI 验证。本地只做必要的构建、格式化，以及针对当前失败点的定向用例或诊断。例子：修复某个页面的单个失败用例时，可以本地跑该 class 或 method 辅助定位，但不能默认执行完整 `connectedLiteDebugAndroidTest` 来占用模拟器和拖慢反馈。

### Desktop release jar 运行验证

打包桌面单体 jar 时，不能只验证任务成功、文件大小和 manifest。ProGuard 会改变运行时可达性，尤其会删除 `ServiceLoader` 发现的 provider 类、Room/KSP 生成实现，或改名 JNI 需要按原签名查找的 native 方法，导致启动后才报错。例子：桌面 release jar 必须实际执行 `java -jar` 到数据库和网络初始化路径，并为服务 provider、反射生成类、native 方法添加 keep 规则；否则一个看起来更小的 jar 可能只是被错误裁剪了。

### 文章导出图片分辨率

处理截图导出体积时，先判断尺寸来源，不能只在最终图片上套总像素上限。网页或 Compose 这类逻辑布局导出，应先选定合理的输出 DPI/缩放倍率，再把 CSS/DP 尺寸转换成像素；否则高密度设备会直接生成 3x/4x 物理像素图，后面再猜一个像素上限只是补救。例子：长图导出应该按固定输出 DPI 渲染同一份逻辑页面宽度，而不是让页面宽度跟随设备物理像素后再硬压缩。

### 返回栈上下文保存

处理“从弹层或列表进入详情，再返回原位置”的问题时，不能只保存开关状态，还要保存用户可见上下文。评论区这类弹层不仅要恢复打开状态，还要恢复评论列表滚动位置；从评论进入用户资料页再返回时，如果只是重新打开评论区但回到顶部，本质上仍然丢失了上下文。例子：列表弹层里的某一项进入详情页，返回后应该看到原先那一项附近，而不是只把弹层重新显示出来。

### Subagent 任务边界

当任务明确要求 subagent 负责实现或发 PR 时，主 agent 只能做调度、资源协调和最终验收，不能因为自己已经掌握上下文就越权直接提交 PR。例子：多个 issue worktree 并行处理时，主 agent 应负责分派互不冲突的工作、协调 AVD 使用和检查结果；具体分支提交、推送和 PR 创建应交给负责该 issue 的 subagent 完成，否则会破坏用户要求的并行工作边界。

### 新功能实现边界

实现新功能前必须先判断项目未来维护的主路径，不能为了“覆盖所有现存渲染方式”把即将废弃或非主线的路径也改一遍。例子：图片预览新交互如果产品方向只要求 Compose，就应该只接入 Compose 渲染链路；顺手把 WebView、平台能力接口和解析层都扩展，会让 diff 膨胀、审查成本上升，也会把功能承诺带到不打算继续支持的路径上。

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
- **lite**: 轻量版 (~4MB)，无 ML 功能，包名 `com.github.zly2006.zhplus.lite`
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

### git worktree

新开worktree的时候，记得把 local.properties 复制过去，避免构建问题.

### 导航
- 使用 Jetpack Navigation Compose
- 定义 sealed interface `NavDestination` 表示不同页面，包含 route 和参数
- 在编写导航代码前必须检查 NavDestination.kt
- UI、导航、按钮或设置项设计改动前，先读 `docs/ai-ui-design-guide.md`，按其中的入口、preference key 和验证点检查影响范围。

## Android 调试标准流程

注意：
1. 必须使用avd验证，不要使用真机。
2. 时刻注意你是一个LLM，延迟很高。所以大多数情况下不需要你执行sleep指令，你本身的反应就很慢，足够程序响应了。这也是说，如果需要执行双击等复杂手势，必须用&&来串联多个adb指令，不然你的反应太慢就不是双击了。
3. UI 验证时如果启动后看到“下载官方App”“查看协议”“查看设置”这类官方 App/协议确认页，或进入知乎网页登录/安全验证页，不要当成普通业务 UI 问题；这表示当前 AVD 登录态缺失或失效。应先按 `.agents/skills/launch-on-device/SKILL.md` 的 Login JSON Backup and Restore 流程恢复/覆盖 `files/account.json`，确认已登录后再继续 UI 验证；不要反复卡在登录流程里。

### 应用启动与验证
```bash
# 1. 检查包名（必须先做）
grep "applicationId" app/build.gradle.kts
# lite variant: com.github.zly2006.zhplus.lite

# 2. 启动模拟器（如果还没启动）
emulator -avd Medium_Phone_2

# 3. 构建并安装
./gradlew assembleLiteDebug
adb install -r app/build/outputs/apk/lite/debug/app-lite-debug.apk

# 4. 启动
adb shell am force-stop com.github.zly2006.zhplus.lite
adb shell monkey -p com.github.zly2006.zhplus.lite -c android.intent.category.LAUNCHER 1
```

### UI 调试强制清单
修改 UI 代码后**必须**：
1. ✅ 构建 + 格式化
2. ✅ 安装到设备
3. ✅ 正确启动应用（检查包名！）
4. ✅ 等待加载完成（至少 8-10 秒）
5. ✅ 使用 ui-test 技能查看当前页面状态：`python3 .agents/skills/ui-test/llm_test_helper.py dump`
6. ✅ 先 `dump` 再 `tap`，优先通过 `--tag/--text/--desc` 交互，不使用硬编码坐标 tap
7. ✅ 若目标是无标识可点击节点，使用 `--text "" --index N`（N 来自当前页面 dump）
8. ✅ 交互后再次 `dump` 或截图验证状态
9. ✅ 仅在无 tag/文字可用且必须手势操作时，才使用 `adb shell input swipe` 等手势
10. ❌ 异常时检查 logcat：`adb logcat | grep -i error`

### UI 双代理复检

`$ui-voyager` 和 `$picky-user` 运行成本较高，非必要不要调用。只有在改动范围较大、交互路径复杂、主 agent 已经完成基础截图/设备验证但仍需要额外视角，或我明确要求复检时，才启动它们。调用时必须使用 5.4 mini 级别模型，避免使用过慢模型。

需要调用时，主 agent 在完成上面的基础验证后，再执行以下流程：

1. 启动两个 subagent skill，不能由主 agent 自己扮演：
   - `$ui-voyager`（UI漫游者）：系统性探索目标页面，把能点的尽量都点一遍，把上下左右的滑动都试一遍，重点找空白页、越界、裁切、重叠、错位、状态切换异常。
   - `$picky-user`（挑剔的用户）：分别扮演新用户和老用户，对 self explain、明确性、直觉性、效率、布局和操作习惯提出高标准意见。
2. 两个 skill 都必须先读取自己的持久化记忆：
   - `.memory/YYYY-MM-DD/picky-user/`
   - `.memory/YYYY-MM-DD/ui-volayor/`
3. 两个 skill 都允许在 `ui-test` 之外结合截图做视觉判断，但交互仍优先走 `ui-test` 的 `dump` / `tap` / `screenshot` 工作流。
4. `ui-voyager` 遇到拿不准的地方，必须把复现步骤和犹豫原因交给 `$picky-user` 或主 agent，请其再判断，不要含糊带过。
5. 主 agent 只有在以下条件满足后，才能停止工作、宣布 UI 修改完成，或请求我做下一步决策：
   - `$ui-voyager` 没有新的有效问题；
   - `$picky-user` 没有新的有效意见；
   - 或者它们提出的意见都已经被修复，或被明确标记为无效/驳回并留下充分理由。
6. 主 agent 对每条意见都必须写回 memory，至少标记为 `fixed`、`rejected` 或 `invalid`，不能口头略过。

记忆回写命令示例：

```bash
TODAY=$(date +%F)
python3 .agents/skills/ui-review-memory/memory_store.py update-status \
  --agent picky-user \
  --date "$TODAY" \
  --id PU-20260417-001 \
  --status fixed \
  --note "已修复并复测通过。"
```

`update-status` 会按 `id` 自动定位历史记录，所以 issue 即使不是今天创建的，也必须继续回写，而不是新建另一个编号。

## 代码风格
- Kotlin Serialization with `@Serializable`
- 只在必要时注释，不过度注释
- ktlint 格式化（14.0.1）

## Code Review
- 每次修改后，必须进行代码 review，等待批准后才能 commit
- 不仅要进行上述所有检查，还要检查是否有代码重复片段，是否有未使用的变量或函数，是否有潜在的性能问题等
- 不仅要检查当前代码，还要把关键地方都grep一下，检查你写的代码是否和其他地方重复了，是否有类似的代码片段可以复用
- 在不降低注释质量的前提下，代码越短越好，避免过度设计和过度抽象

## ⏰重要提醒，在每次编写代码时必须遵守：
- 不得擅自简化代码实现，如果确实有的功能难以实现，停下来等待我的反馈，不要私自修改设计。
- 必须按照上述流程进行调试验证，尤其是 UI 相关的修改，不能跳过任何一步，确保你写的功能正常可用。
- 每次修改完代码后必须进行review，不能直接提交，必须等待我的反馈和批准后才能合并到主分支。

## Pull Requests

当我要求你发 PR 的时候，PR 的title必须以feat: /fix: /refactor: 开头，标题和内容必须用中文写。
提交PR前，先更新master与远程同步或领先，并确保当前分支基于master，而不包括其他feature branch的内容。
如果一开始给你的提示词包括了issue链接，并且此PR解决了这个issue，应该写上Resolves #issue_number在PR描述里，这样GitHub会自动关联并在PR合并时关闭这个issue。
涉及 UI、布局、样式、可见交互或截图可判断效果的 PR，PR 描述里必须放最终效果截图。截图必须来自实际运行的应用、AVD、或可复现的 UI 测试渲染结果，不能用设计参考图、想象图或旧截图代替；如果真实业务链路被登录态/安全验证挡住，要说明截图来源。
