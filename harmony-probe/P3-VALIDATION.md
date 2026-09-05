# P3 数据库选型验收记录

日期：2026-09-05。环境：Windows x64 主机，CPF 1.0.0 工具链（Kotlin 2.2.21-1.0.0 / CMP 1.9.2-1.0.0 / Gradle 8.9），DevEco CLI 26.0.0.621，bundle `com.github.zly2006.zhplus`。模拟器 `ZhihuPlus_API23`（HarmonyOS 6.1.0 API 23，x86_64）。

覆盖可行性报告 P3 的前三项：同一份最小 schema 分别验证 CPF Room3 与 SQLDelight；检查 KSP、迁移、事务、Flow、并发、数据库路径和异常恢复；评估 Android/Desktop 现有 Room 2.8.4 数据库格式兼容性。第 4 项（选型后再接历史、缓存、屏蔽）属选型后的落地工作，随 P4/MVP 排期，不在本阶段。

截图存于 `.validation/p3_*.jpeg`（不入库）。

## 选型结论（TL;DR）

**HarmonyOS 端采用 CPF SQLDelight（`app.cash.sqldelight` 2.2.1-1.0.0 OH 变体线）作为本地数据库路线。** CPF Room3 在宿主机 JVM 验证中表现优秀（含与生产 Room 2.8.4 的 identity hash 兼容），但其 OH 变体目前**没有可用的代码生成链**：`room3-runtime-ohosarm64 3.0.0-alpha01-0.3.0` 被 fork 改为非 suspend API，而配套的 room3-compiler 未发布，上游 3.0.0-alpha01～3.0.2 全部编译器生成的 suspend 代码均无法在其上编译。按可行性报告的停止条件，这属于"需要长期维护大型私有 fork（androidx room3-compiler）"的情形，不应在 PoC 阶段承担。

若 CPF 后续发布配套 room3-compiler，Room3 路线可凭其 Room 2.8.4 格式兼容性优势重新评估。

## 制品调研（Maven 实证，非文档转述）

CPF 仓库（maven.eazytec-cloud.com，`maven-releases`）实际可用制品：

| 栈 | 坐标 | 结论 |
| --- | --- | --- |
| Room3 OH | `androidx.room3:room3-runtime-ohosarm64:3.0.0-alpha01-0.3.0`（依赖 `androidx.sqlite:sqlite{,-framework}-ohosarm64:2.7.0-alpha01-0.3.0`） | 存在；运行时 API 被 fork 改为非 suspend |
| Room3 compiler | `androidx.room3:room3-compiler` 仅有上游 3.0.0-alpha01～3.0.2 | **fork 配套版本未发布** |
| Room3 JVM | `androidx.room3:room3-runtime-jvm:3.0.0-alpha01` + `androidx.sqlite:sqlite-bundled-jvm:2.7.0-alpha01`（上游线） | 可用，宿主机验证走此线 |
| SQLDelight OH | `app.cash.sqldelight:{runtime,native-driver,coroutines-extensions}-ohosarm64:2.2.1-1.0.0` + `sqlite-3-30-dialect:2.2.1-1.0.0` + gradle-plugin 2.2.1-1.0.0（依赖 `co.touchlab:sqliter-driver:1.3.3-0.3.0`） | 全部存在 |
| SQLDelight JVM | 同版本 fork 的 `runtime-jvm` / `sqlite-driver` / `coroutines-extensions-jvm` | 可用 |
| 两者 x64 | `*ohosx64*` 变体 | **均未发布**，x86_64 模拟器无法运行原生 DB |

两栈均只有 ohosArm64 变体，这决定了 P3 的验证形态：宿主机 JVM 全项功能验证 + ohosArm64 编译链接验证 + 模拟器 UI 能力边界展示；设备端运行时冒烟（arm64 真机）与 P2 的 arm64 网络路径同样待真机。

## 最小 schema（对齐生产 shared-local-db ContentFilterDatabase v6）

三张代表性表，实体注解与生产实体逐字段一致：

- `blocked_keywords`：自增整型主键、布尔列、枚举默认值列（对应生产迁移 2→3 的 `keywordType`）；
- `blocked_users`：文本主键；
- `content_open_events`：自增主键 + 两个索引（对应生产迁移 5→6）。

迁移测试面为 v1→v2（`ALTER TABLE blocked_keywords ADD COLUMN keywordType TEXT NOT NULL DEFAULT 'EXACT_MATCH'`），与生产 `migration2To3` 语义一致。

## 工程结构

| 模块 | 作用 |
| --- | --- |
| `db-room3` | Room3 JVM 验证模块：上游 room3 3.0.0-alpha01 + sqlite-bundled-jvm 2.7.0-alpha01，KSP codegen，schema 导出至 `db-room3/schemas` |
| `db-sqldelight` | SQLDelight KMP 模块（jvm + ohosArm64）：同一份 `.sq`/`.sqm` schema，OH 侧接入 probe |
| `db-legacy-room2` | 生产基线 fixture：Room 2.8.4 + sqlite-bundled 2.6.2，直接编译 shared-local-db 的 7 个实体源文件（Sync 拷贝，不复制代码），导出生产 v6 schema JSON |
| `db-checks` | 双栈同项测试套件 + 格式兼容实验（jvmTest） |

`harmony-probe/settings.gradle.kts` 新增四个模块；`gradle/libs.versions.toml` 新增 ksp/room3/sqldelight/room2 版本线。

## 宿主机 JVM 验证（`:db-checks:test`，全部通过）

### Room3（JVM，BundledSQLiteDriver + 文件路径）

| 用例 | 内容 | 结果 |
| --- | --- | --- |
| A1 | CRUD：自增主键递增、文本主键 upsert 覆盖、索引表写入 | 通过 |
| A2 | 事务回滚（`withWriteTransaction` 内抛异常，行数不变） | 通过 |
| A3 | Flow 观察（insert 后 `observeAll` 更新） | 通过 |
| A4 | 并发写入 8 协程 × 25 条，200 行全落库 | 通过 |
| A5 | 文件路径持久化（关闭重开数据保留） | 通过 |
| A6 | 迁移 v1→v2（MIGRATION_1_2 + Room 迁移后 schema 校验） | 通过 |
| A7 | 异常恢复：主键冲突抛 `SQLiteConstraintException` 后库可用；坏文件（4096 字节垃圾）打开报错且文件不被破坏 | 通过 |
| A8 | 迁移对象常量 | 通过 |

### SQLDelight（JVM，JdbcSqliteDriver + 文件路径）

| 用例 | 内容 | 结果 |
| --- | --- | --- |
| B1 | `Schema.create` + CRUD（自增验证改为读行 id：JDBC insert 返回受影响行数而非 rowid） | 通过 |
| B2 | 事务回滚（`transaction {}` 内抛异常） | 通过 |
| B3 | Flow 观察（`asFlow().mapToList()`） | 通过 |
| B4 | 并发写入 8×25（JVM 单连接以 Mutex 串行化；OHOS 生产用 NativeSqliteDriver 的连接管理） | 通过 |
| B5 | 文件路径持久化 | 通过 |
| B6 | 迁移 v1→v2（手工建 v1 表 + `Schema.migrate(1, 2)` 应用 `1.sqm`，旧数据保留、新列取 DEFAULT） | 通过 |
| B7 | 异常恢复：`SQLITE_CONSTRAINT_PRIMARYKEY` 冲突后库可用；坏文件报错且文件完好 | 通过 |

### 格式兼容实验（核心交付）

| 用例 | 内容 | 结果 |
| --- | --- | --- |
| C1 | 生产 Room 2.8.4 版本线写入完整 v6 数据库（7 张生产实体表） | 通过 |
| C2 | **SQLDelight 零迁移直接读取生产 Room 2.8.4 v6 文件**（不执行 Schema.create，直接查询三类行，值一致） | 通过 |
| C3 | **Room3 直接打开 Room 2.8.4 创建的同 DDL 文件** | 通过：`Room3 直接打开 Room 2.8.4 文件：成功（count=1），identity hash 兼容` |
| C4 | 三方（Room 2.8.4 / Room3 / SQLDelight）DDL + `PRAGMA table_info` 结构比对（5 对象 DDL 一致、3 表结构一致；主键写法差异已归一化：Room 表级 `PRIMARY KEY(col)` vs SQLDelight 列级） | 通过 |
| C5 | 迁移桥实验：C3 已直接兼容，无需清空 `room_master_table`（代码保留作为 CPF 后续版本 hash 行为变化时的回退方案） | 通过（直连分支） |

**关键发现：Room3 3.0.0-alpha01 与 Room 2.8.4 的 identity hash 兼容**——同 DDL 的生产格式文件可被 Room3 直接打开并写入新 hash，数据零丢失。这意味着一旦 Room3 的 OHOS 代码生成链补齐，历史/屏蔽/缓存数据库可以维持 Android/Desktop 现有格式，不需要平台独立数据库或数据迁移。

## Room3 OHOS 缺口定性（本轮最重要的负发现）

1. `room3-runtime-ohosarm64:3.0.0-alpha01-0.3.0` 的 `RoomOpenDelegate.createAllTables` 等为**非 suspend**，sqlite OH fork（2.7.0-alpha01-0.3.0）的 `execSQL/prepare/step` 为成员函数形态。
2. 上游 room3-compiler 3.0.0-alpha01（及 alpha02～3.0.2 全部）生成的代码是 `override suspend fun createAllTables` + 顶层扩展 `executeSQL/prepare/step` import，与 fork runtime 结构性不匹配（逐一编译验证，见工程内 `-Proom3CompilerVersion` 试探配置）。
3. CPF 的 androidx fork（gitcode `main-094969d9a0dfebbd4500d1c1eb54177a2cc6dd06-OH` 分支）**源码里有改配的 room3-compiler**（test-data 生成非 suspend 代码），但**未发布到 Maven**；fork 仓库不含构建体系，本地构建需要整套 androidx 基础设施。
4. 官方三方库清单标注 room3"已适配"，但未提供接入文档/示例；kmp-cmp-example 未使用 room3。

据此，按报告停止条件（"Room、内容渲染和关键三方库需要同时维护多套私有 fork"），PoC 不承担 room3-compiler 私有 fork 的维护。`db-room3` 已收敛为纯 JVM 验证模块；compat shim（`androidx.sqlite` 扩展桥）保留在 git 历史中，验证过 import 层可桥接、卡点在 suspend 覆盖签名。

## SQLDelight OHOS 侧已验证项

- `db-sqldelight` 以 KMP（jvm + ohosArm64）编译通过：runtime / native-driver / coroutines-extensions OH 变体解析正常，`.sq`/`.sqm` codegen 正常。
- `:probe` ohosArm64 编译 + `linkDebugSharedOhosArm64` 链接通过（SQLDelight 全栈进入 libkn.so，含 sqliter fork 的原生 SQLite cinterop），`publishDebugBinariesToHarmonyApp` 产出双架构 Debug 库。
- fork 适配注意点（已落地到代码注释）：
  - `sqlite-3-30-dialect` 对 `INTEGER AS Boolean` 的代码生成有缺陷（生成非法 `import Boolean`），schema 直接用 INTEGER 存布尔（与 Room 的存储一致）；
  - `.sq` 的 CREATE TABLE 内部不能写 SQL 注释，会被原样存入 `sqlite_master` 的 DDL；
  - fork 的 `DatabaseConfiguration` 不含 `basePath` 字段，数据库路径随 `NativeSqliteDriver(schema, name)` 的 `name` 传完整沙箱路径（ArkTS 壳经 `P3SetDatabasePath` 注入 `filesDir + '/databases'`）。

## 模拟器验收（API 23，x86_64）

| 验收项 | 结果 | 证据 |
| --- | --- | --- |
| 含 P3 切片的签名 HAP 安装 + 冷启动，P1 主壳渲染 | 通过 | p3_boot.jpeg |
| 账号页出现「P3 数据库选型」入口 | 通过 | p3_scrolled.jpeg |
| P3 页面渲染：选型结论 + x64 能力边界如实展示（"CPF Room3/SQLDelight 未发布 ohosX64 变体，原生 DB 不支持"） | 通过 | p3_page.jpeg |
| 返回键从 P3 切片回到主壳（`P1ShellState.handleBack` 统一分发） | 通过 | p3_back.jpeg |
| 进程存活，无 cppcrash/appfreeze | 通过 | `ps` 在列；hilog 无崩溃记录 |

x64 上显示"不支持"是**真实能力边界**（两个 DB 栈都没有 ohosX64 变体），不是假实现；arm64 设备上的 SQLDelight 冒烟（建库/写读/关闭）已随 libkn.so 链接就绪，待真机执行。

## 过程记录

- 首次 C4 失败：`.sq` 内注释被 SQLDelight 原样存入 sqlite_master；首次 A6 失败：V1 库只建了一张表导致迁移后 schema 校验失败，改为 V1 含全部三张表（与生产 v2 之前形态一致）后通过。
- SQLDelight/SQLite 的 insert 返回值为受影响行数而非 rowid，自增断言改为读取行 id。
- Room3 的 `withTransaction` 在 3.0.0-alpha01 更名为 `withWriteTransaction`；`Migration.migrate` 为 suspend。

## 未验证项 / 已知限制

- **arm64 真机未验证**（无鸿蒙真机，与 P1/P2 相同）：SQLDelight OHOS 冒烟、Room3 C3 结论在真机 sqlite 上的复现、native-driver 的并发/锁行为待真机回归。
- x86_64 模拟器无法运行任何 CPF 原生 DB（无 ohosX64 变体），P3 设备端证据仅覆盖编译链接与 UI 能力边界。
- B4 的 JVM 并发以 Mutex 串行化（JDBC 单连接）；NativeSqliteDriver 真机并发行为未测。
- 选型后的落地（历史/缓存/屏蔽功能迁移到 SQLDelight、schema 全集 .sq 化、与 Android 端 Room 2.8.4 的长期双向兼容策略）属 P4/MVP 范围。
- 若 CPF 发布配套 room3-compiler，应重跑 db-checks（JVM 线可原样复用）并重评 Room3 路线。

## 结论

P3 前三项内容全部完成：同一最小 schema 的双栈验证（Room3 与 SQLDelight 各 8/7 项 + 5 项格式兼容实验在宿主机全部通过）、KSP/迁移/事务/Flow/并发/路径/异常恢复全项覆盖、Room 2.8.4 格式兼容性得出明确结论（SQLDelight 零迁移可读、Room3 identity hash 兼容）。选型为 **SQLDelight（OHOS 端）**，依据是 SQLDelight 全链路可用而 Room3 OH 变体缺配套编译器。模拟器验收通过，无崩溃。
