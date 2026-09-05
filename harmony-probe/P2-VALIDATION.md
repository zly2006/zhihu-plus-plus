# P2 游客阅读切片验收记录

日期：2026-09-05。环境：Windows x64 主机，CPF 1.0.0 工具链（Kotlin 2.2.21-1.0.0 / CMP 1.9.2-1.0.0），DevEco CLI 26.0.0.621，bundle `com.github.zly2006.zhplus`（versionName 0.24.4 / versionCode 222）。

覆盖可行性报告 P2 的五项内容：CPF Coroutines/Serialization/Ktor CIO/Coil 接入、真实游客接口纵向切片、最小 Cookie 存储、Markdown/LaTeX/CodeHighlight 八模块 OHOS target、双 API 档长文压力样本。

截图存于 `.validation/`（不入库）。

## 构建证据

- `:reader-checks:jvmTest` 通过：`P2MarkdownTest` 3 个用例，0 失败（日报 HTML → Markdown 转换的宿主机解析回归）。
- `:probe:publishDebugBinariesToHarmonyApp` 成功产出双架构 Debug `libkn.so`（arm64 113,592,248 字节、x64 101,948,096 字节）与 `libkn_api.h`；本轮命中构建缓存，八模块（markdown-parser/runtime/renderer、latex-base/parser/renderer、codehighlight-parser/render）双架构 compile 任务 UP-TO-DATE。
- `devecocli build` 产出签名 HAP，`hdc install -r` 在两台模拟器上均安装成功。
- 网络栈：arm64 编入 CPF Ktor 3.3.3-1.0.0 CIO + Coil 3.3.0-1.0.0；x64 使用 ArkTS NetworkKit 桥 + 同一 Kotlin 模型。CPF Coroutines 1.10.2-1.0.0、Serialization 1.9.1-1.0.0 随双架构编译。

## 已验证项

### API 23 模拟器（`ZhihuPlus_API23`，HarmonyOS 6.1.0 API 23，x86_64）

| 验收项 | 结果 | 证据 |
| --- | --- | --- |
| P1 主壳 → 账号页 →「P2 知乎日报访客切片」入口 | 通过 | p2s2.jpeg |
| 真实游客接口加载日报首页（知乎日报公开 API · 20260905，列表 + hint 渲染） | 通过 | p2_home.jpeg |
| 最小 Cookie 存储：访客 Cookie 持久化（Preferences），**冷启动恢复**后复用，仅允许 _xsrf/BEC/d_c0 | 通过 | p2_home.jpeg 状态行「冷启动已恢复；访客 Cookie 已持久化」 |
| 未预载条目点击被禁用（不假装可读） | 通过 | p2_home.jpeg 灰字说明 |
| 打开预载文章：标题 + 真实封面图 | 通过 | p2_article.jpeg |
| 正文 Markdown：段落、链接（查看知乎原文）、正文网络图片 | 通过 | p2_article.jpeg / p2_article2.jpeg |
| 压力样本起始：粗斜体、引用、Kotlin 语法高亮（深色代码块）、LaTeX 公式 1/80 真实渲染 | 通过 | p2_stress.jpeg |
| 公式区连续滚动（13/80 → 15/80 无丢行、无黑块） | 通过 | p2_stress2.jpeg |
| 300 列表项区（198–212/300）连续滚动检查 | 通过 | p2_stress3.jpeg |
| 200 表格行区（187–200/200）到达「校验终点」与「P2 压力样本结束」 | 通过 | p2_stress4.jpeg |
| 全程无 cppcrash/appfreeze，进程存活 | 通过 | hilog 无崩溃记录，`ps` 进程在列 |

### API 20 模拟器（`ZhihuPlus_API20`，HarmonyOS 6.0.0 API 20，x86_64，对应报告要求的 API 19~21 档）

| 验收项 | 结果 | 证据 |
| --- | --- | --- |
| 同一签名 HAP 安装 + 冷启动，P1 主壳渲染 | 通过 | p2_api20_boot.jpeg |
| 日报游客切片：真实 API 首页加载（20260905） | 通过 | p2_api20_home2.jpeg |
| 最小 Cookie 存储：首次运行即持久化（Preferences） | 通过 | p2_api20_home2.jpeg 状态行「访客 Cookie 已持久化」 |
| 预载文章选取（最长阅读时长 5 分钟一篇） | 通过 | p2_api20_home2.jpeg「已预载 · 点击阅读」 |
| 压力样本起始：Kotlin 高亮 + LaTeX 公式渲染 | 通过 | p2_api20_stress.jpeg |
| 压力样本滚动至「校验终点」（80 公式 / 300 列表项 / 200 表格行全部通过） | 通过 | p2_api20_stress_end.jpeg |
| 全程无 cppcrash/appfreeze，进程存活 | 通过 | hilog 无崩溃记录，`ps` 进程在列 |

两台模拟器合计覆盖 API 20（19~21 档）与 API 23（22+ 档），满足报告 P2 第 5 项「一台 API 19~21 + 一台 API 22+」的环境要求（均为 x86_64 可靠设备环境，非真机）。

## 过程记录

- API 20 首次点击切片入口坐标略偏未命中，补点后进入（纯操作问题，非产品缺陷）。
- 本轮构建全部命中当日缓存（P2 代码当日早些时候已编译）；干净缓存重建属于 P0 范畴的重复验证，本轮未重复执行。

## 未验证项 / 已知限制

- **arm64 原生网络路径（Ktor CIO + Coil）仍只有编译链接证据**：两台模拟器均为 x86_64，实际走 NetworkKit 桥；arm64 真机端到端待真机验证。
- **arm64 真机未验证**：与 P1 相同，无鸿蒙真机可用。
- API 19 下限未覆盖：本地无 API 19 镜像，以 API 20 代表 19~21 档。
- 长文压力为**固定合成样本**（同一公式/列表/表格重复），不是真实知乎长回答语料；真实语料回归、帧率量化、自渲染 vs 统一渲染 A/B 仍属后续工作。
- HTML 适配范围仍是日报基本标签，未迁移 Android 端完整 Ksoup 转换器；公式/表格之外的复杂排版未验证。
- 游客 Cookie 只覆盖 _xsrf/BEC/d_c0 的持久化与回放，不承诺登录；Web 风控、账号登录不在本切片。
- 首页一次只预载一篇文章（按「分钟阅读」最长选取），其余条目明确关闭；未验证跨天分页与图片缓存策略。
- 未做前后台切换/低内存/断网恢复等系统级压力（P1 已覆盖部分生命周期项）；字体缩放、横竖屏仍未验证。
- Debug 原生库体积大（arm64 ≈ 113 MB）；Release/strip/包体优化属后续工作。

## 结论

P2 五项内容全部完成并通过双 API 档模拟器验收：真实游客接口端到端（首页 → 文章 → 图片 + Markdown）、最小 Cookie 存储与冷启动恢复、八模块阅读器 OHOS 双架构编译与渲染、长文压力样本在 API 20 与 API 23 全部到达校验终点且无崩溃。

PoC 前三阶段（P0 工具链、P1 共享主壳、P2 游客阅读切片）至此闭环。剩余大项与报告一致：arm64 真机验证、数据库选型（P3）、登录与系统能力（P4）。
