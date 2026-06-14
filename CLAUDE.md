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

修复截图导出空白、裁切或渲染时序问题时，不能只验证 bitmap 能创建、JPEG 能编码或文件大小大于 0；这些都不能证明页面内容真的画进去了。必须检查实际像素内容，至少验证导出区域存在非背景像素，最好保存并查看一张真实导出结果。例子：一个 WebView 长图如果在最终高度布局后立刻截图，可能得到一张尺寸正确但全白的图片；测试只断言压缩成功会漏掉根因，应验证正文文字区域已经产生可见像素。

### 返回栈上下文保存

处理“从弹层或列表进入详情，再返回原位置”的问题时，不能只保存开关状态，还要保存用户可见上下文。评论区这类弹层不仅要恢复打开状态，还要恢复评论列表滚动位置；从评论进入用户资料页再返回时，如果只是重新打开评论区但回到顶部，本质上仍然丢失了上下文。例子：列表弹层里的某一项进入详情页，返回后应该看到原先那一项附近，而不是只把弹层重新显示出来。

保存返回栈上下文时，必须区分“应该跨返回保留的 UI 状态”和“不能重复入栈的导航目标”。评论区这类场景可以保存弹层打开状态和列表位置，但从评论作者进入个人页这类外部导航要防止同一目标短时间连续入栈；否则返回时可能只是露出前一个重复的个人页，看起来像又自动进入了一次。不要声称 Compose recompose 会重放 `clickable` 的 `onClick`；排查这类问题应先看是否重复调用了导航入口、是否重复 push 了同一个 route。

用户指出某个提交引入具体回归时，不能只记录根因就结束；除非用户明确只要求写经验，否则必须同时修复回归并验证。例子：用户说返回评论页会再次进入个人主页时，记录“导航重复入栈”只是第一步，还要改掉导航去重逻辑并跑必要检查。

调试 UI 导航回归时，不能在未抓到导航调用次数、返回栈变化或输入事件日志前，把“重复入栈”“事件未消费”等猜测写成根因并上补丁。Compose 的 `clickable` 不会因为 recompose 自动重放 `onClick`；如果怀疑重复导航，必须先用最小日志或测试证明同一入口被调用了几次、每次来自哪里，再决定修导航层还是 UI 层。例子：返回评论页后再次出现个人页，可能是重复 push、返回键先关闭弹层、或其他返回栈状态问题；不能只看症状就给 `navigate()` 加时间窗口去重。

### Subagent 任务边界

当任务明确要求 subagent 负责实现或发 PR 时，主 agent 只能做调度、资源协调和最终验收，不能因为自己已经掌握上下文就越权直接提交 PR。例子：多个 issue worktree 并行处理时，主 agent 应负责分派互不冲突的工作、协调 AVD 使用和检查结果；具体分支提交、推送和 PR 创建应交给负责该 issue 的 subagent 完成，否则会破坏用户要求的并行工作边界。

### 新功能实现边界

实现新功能前必须先判断项目未来维护的主路径，不能为了“覆盖所有现存渲染方式”把即将废弃或非主线的路径也改一遍。例子：图片预览新交互如果产品方向只要求 Compose，就应该只接入 Compose 渲染链路；顺手把 WebView、平台能力接口和解析层都扩展，会让 diff 膨胀、审查成本上升，也会把功能承诺带到不打算继续支持的路径上。

### 段评与正文格式优先级

`segment_infos` 没有正文原始格式重要。段评高亮只能在不会破坏原 HTML 结构时注入；加粗和斜体可以正常处理，应纳入白名单；如果段落里已经有脚注、链接、图片、公式等非白名单内联或块级格式，应暂停解析这段 `segment_infos`，优先保留原格式。例子：一个带脚注引用的段落不能为了注入段评 span 把 `<sup>` 展平成普通 `[3]` 文本；临时跳过段评比破坏脚注显示更合理。

### 抽象边界

清理或新增 UI 辅助函数时，不能把只转发一次调用、没有分支、状态、契约隔离或复用收益的包装层保留下来。例子：一个正文渲染函数如果只是把参数原样传给底层渲染组件，调用点也只有少数几处，就应该在调用点直接使用底层组件；只有当它承载平台分支、设置读取、状态保存或跨页面统一语义时，才值得独立成函数。

代码 review 不能只确认 helper 行为正确，还必须检查新增调用层是否有存在价值。若一个成员函数只是把参数原样转发给同文件里的扩展函数，且没有隐藏状态转换、线程约束、平台差异或 API 稳定性收益，应直接在调用点使用真正承载语义的函数。例子：列表合并逻辑已经由扩展函数完整表达时，再包一层类成员只会制造假抽象，让 review 误以为那里有额外契约。

每写一个helper函数，扇自己是个耳光。扇了之后还是觉得他有价值，才能保留！

修复 review 反馈后，必须把“删除无价值包装层”作为提交前检查项，而不是只看测试和行为是否通过。例子：一个列表状态更新函数如果已经能直接在调用点修改状态列表，再保留同名成员方法转发，只会增加维护面；提交前应主动删掉这类转发层。

当用户明确要求删除某个抽象并在调用点使用更底层能力时，不能把原抽象换成一组私有 helper 或同形 adapter。即使 helper 只有文件内可见，只要它仍然是在替原调用点封装同一段直通逻辑，就违背了“删除抽象”的目标。例子：一个导航流程原来通过仓库接口取数据，用户要求直接使用环境对象和数据库；正确做法是在导航流程需要的位置直接读取环境和数据库，而不是新建 `fetchSomething()`、`toSomething()` 这类薄包装把同样的转发藏起来。

如果项目里已有明确承载语义的底层支持对象，不能忽略它再发明同义辅助函数。尤其是用户点名某个 support 对象时，应在调用点直接使用该对象已有 API，而不是另起一个 `getAlready...` 之类的二次包装。例子：已有内容打开记录支持对象可以查询已打开内容，就直接调用它并传入数据库和内容键；不要再包一层只改名不增加语义的函数。

删除抽象时不能把本来属于具体导航/状态上下文的数据强行塞进通用 environment。environment 只应保留真正跨功能的运行能力；如果某个数据库只服务回答切换导航，就应该由回答切换状态或具体调用点持有，再在导航逻辑里直接传给已有 support 对象。例子：为了调用内容打开记录支持对象查询已读回答，不应给所有文章环境新增一个数据库 getter。

### 通知偏好默认值

修复某类通知“数据缺失、不显示、不能进入”的问题时，只能修数据源、解析、分页和渲染链路，不能顺手改变该类通知的默认开关策略。默认是否展示属于产品偏好，不是 bug 修复的附属决定；如果用户原本需要主动选择接收某类通知，修复后仍应保持 opt-in。例子：某类通知以前因为只拉了聚合列表而缺失，正确修复是补齐对应分类接口和失败隔离，而不是把该类通知从默认隐藏改成默认显示。

### 设置项说明位置

修复设置页说明文字位置时，必须先检查设置项组件是否已有 description/supporting text 能力；说明只解释某个开关时，应绑定到该设置项自身，而不是为了视觉位置新建分组或放到组 footer。例子：一个“进入页面后自动执行”的说明只属于自动执行开关，就应该作为该行的说明文字；把另一个无关开关拆到新组只会改变信息架构，不能算修复说明位置。

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
1. 必须使用avd验证，不要使用真机。若 `/Users/zhaoliyan/.agents/skills/off-android-avd-ci-debug/SKILL.md` 存在，UI/AVD 验证必须优先使用 `$off-android-avd-ci-debug` 提供的远端 `off` AVD；只有该 skill 不存在或远端 runner 不可用时，才退回本地 AVD。
2. 时刻注意你是一个LLM，延迟很高。所以大多数情况下不需要你执行sleep指令，你本身的反应就很慢，足够程序响应了。这也是说，如果需要执行双击等复杂手势，必须用&&来串联多个adb指令，不然你的反应太慢就不是双击了。
3. UI 验证时如果启动后看到“下载官方App”“查看协议”“查看设置”这类官方 App/协议确认页，或进入知乎网页登录/安全验证页，不要当成普通业务 UI 问题；这表示当前 AVD 登录态缺失或失效。应先按 `.agents/skills/launch-on-device/SKILL.md` 的 Login JSON Backup and Restore 流程恢复/覆盖 `files/account.json`，确认已登录后再继续 UI 验证；不要反复卡在登录流程里。
4. 调用 `$ui-test` 或安排 UI 自动化 subagent 时，尽量使用 `gpt-5.4-mini`；复杂判断再使用 `gpt-5.4`，避免使用反应较慢的模型拖慢 AVD 交互。

### AVD 选择优先级

1. 若 `$off-android-avd-ci-debug` 存在，先读取该 skill，并用其远端 runner 脚本做健康检查：
   ```bash
   /Users/zhaoliyan/.agents/skills/off-android-avd-ci-debug/scripts/off-avd-ci-debug.sh status
   /Users/zhaoliyan/.agents/skills/off-android-avd-ci-debug/scripts/off-avd-ci-debug.sh boot-check
   ```
2. `boot-check` 只证明远端 runner 可启动并会在结束时清理模拟器。需要真实 UI 交互时，应按 `$off-android-avd-ci-debug` 的远端环境约定在 `off` 上启动短生命周期 AVD，并在远端 ADB 环境中安装、启动和执行 UI 验证，不要把本地 ADB 当成远端 emulator。
3. 远端 AVD 只作为短生命周期 runner 使用；验证完成后必须清理：
   ```bash
   /Users/zhaoliyan/.agents/skills/off-android-avd-ci-debug/scripts/off-avd-ci-debug.sh kill
   ```
4. 只有远端 skill 缺失或远端 runner 明确不可用时，才使用本地 `Medium_Phone_2`。

### 应用启动与验证
远端路径和本地回退路径必须分开执行，不能连续复制执行。只要选择了 `$off-android-avd-ci-debug`，后续 `adb` / `ui-test` 命令都必须在 `off` 的远端 ADB 环境中运行，不能继续使用本机裸 `adb`。如果当前远端 skill 只有 `status` / `boot-check` / `kill`，没有能保持 emulator 运行的交互入口，不能把 `boot-check` 后面接本机 `adb`；应先补远端交互脚本，或把远端 runner 明确标记为当前不可用后再走本地回退。

```bash
# 检查包名（必须先做）
grep "applicationId" app/build.gradle.kts
# lite variant: com.github.zly2006.zhplus.lite
```

远端优先路径：

```bash
/Users/zhaoliyan/.agents/skills/off-android-avd-ci-debug/scripts/off-avd-ci-debug.sh status
/Users/zhaoliyan/.agents/skills/off-android-avd-ci-debug/scripts/off-avd-ci-debug.sh boot-check
# boot-check 会清理模拟器。真实 UI 交互必须在 off 上启动短生命周期 AVD 后执行。
# 后续设备命令的作用域必须类似这样，不能换成本机裸 adb：
ssh off 'bash -lc '"'"'
BASE=/home/dom/android-ci
export JAVA_HOME="$BASE/java"
export ANDROID_HOME="$BASE/android-sdk"
export ANDROID_SDK_ROOT="$BASE/android-sdk"
export ANDROID_USER_HOME="$BASE/android-home"
export ANDROID_AVD_HOME="$BASE/avd"
export ANDROID_EMULATOR_HOME="$BASE/emulator-home"
export TMPDIR="$BASE/tmp"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
adb devices
'"'"''
```

本地回退路径，仅当 off skill 缺失或远端 runner 明确不可用时执行：

```bash
emulator -avd Medium_Phone_2

./gradlew assembleLiteDebug
adb install -r app/build/outputs/apk/lite/debug/app-lite-debug.apk

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

`$ui-voyager` 和 `$picky-user` 运行成本较高，非必要不要调用。只有在改动范围较大、交互路径复杂、主 agent 已经完成基础截图/设备验证但仍需要额外视角，或我明确要求复检时，才启动它们。调用时必须优先使用 `gpt-5.4-mini`，复杂场景可用 `gpt-5.4`，避免使用过慢模型。

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
