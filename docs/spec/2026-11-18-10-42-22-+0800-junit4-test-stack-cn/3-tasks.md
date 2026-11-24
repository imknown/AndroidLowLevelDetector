# 任务清单: junit4-test-stack

> **状态**: living. 流程见 [13-工作流](../../agents-cn/13-工作流.md);  
> 每 task 一笔提交, 顺序执行, 拆法照 [12-Git](../../agents-cn/12-Git.md) 的单元算法.

## 任务清单

- [x] **task 1 依赖与测试源码切到 JUnit 4** (低风险; 一笔提交).  
  凭什么是独立 task: 删依赖与改测试必须同一笔, 拆开会出现编译不过的中间态.  
  目标: 本地单测从 "Jupiter 6.1.3 加桥接包" 切到 "JUnit 4.13.2 加 Robolectric 原生 runner",  
  删掉 Jupiter 与平台接线, 26 个测试类跟着换 import 与注解, 行为与断言语义不变.  
  动哪些文件: `gradle/toml/android.toml`: 版本键改 4.13.2 加英文注释, 删 jupiter 与 platform launcher  
  两条连同 98-99 两行注释, 换 `junit` 库条目, `test` bundle 改 `["junit"]`;  
  `gradle/toml/thirdParty.toml`: 删桥接包版本键与库条目;  
  `build-logic/.../KotlinAndroid.kt`: 改英文注释, 删 `useJUnitPlatform()` 与 launcher interceptor 开关与  
  桥接依赖, 保留 `--add-opens` 与 `--enable-native-access`; 26 个测试类: 20 个桥接类换  
  `@RunWith(RobolectricTestRunner::class)` 与 `org.junit.Assert` 的显式 import, 6 个纯 JVM 类只换 import 与 `@Test`;  
  类清单见 design.  
  怎么验证: 开工先按 design 的恢复动作核对 HEAD 与工作树, 外部新增或改过的仍在用 Jupiter 的测试当场改;  
  `./gradlew :app:testFossDebugUnitTest :base:testDebugUnitTest` 期望 26 类 116 方法 0 失败  
  (外部新增的测试计入后按实际数核对); `./gradlew assembleFossDebug` 与 `./gradlew lintFossDebug`;  
  对照 Robolectric 官方九条 `--add-opens` 核 JVM 开关, 只补实际需要的并同步注释;  
  grep `tech.apter`, `org.junit.jupiter`, `org.junit.platform`, `Jupiter` 与 `useJUnitPlatform` 在 `app/src`, `base/src`, `build-logic/`, `gradle/toml/*.toml` 零残留.  
  对应 design: 依赖与接线切换; 落实 R-1, R-2.
- [x] **task 2 文档与事实一致** (低风险; 一笔提交, 只改文字不跑构建).  
  凭什么是独立 task: 只改文字不碰代码, 不需要构建, 可以单独提交与核对.  
  目标: living 文档的测试栈现状句, 测试计数, 与缺口句改成改造后的事实; 历史决定记载与 dated 报告不动.  
  动哪些文件: `docs/agents-cn/05-项目架构.md` (64-73 与 75-82 两段),  
  `docs/dev/issues-cn/01-架构与分层/06-技术栈缺口.md` (9-14, 19, 20-23, 27, 41, 42; 改动前的行号),  
  `docs/dev/issues-cn/08-反模式与卫生/43-比对语义逐行adhoc.md` (54, 64),  
  `docs/dev/issues-cn/README.md` (268), `docs/dev/issues-cn/01-架构与分层/01-无领域模型.md` (80),  
  `docs/dev/issues-cn/05-稳定性与错误处理/19-版本合并残留弱点.md` (24-25); 逐处改法见 design 的表格.  
  怎么验证: 逐条对照官方文档与改完后的代码现状; grep `tech.apter` 与 `org.junit.jupiter` 在  
  `docs/agents-cn/05-项目架构.md` 与 `docs/dev/issues-cn/` 零残留; `Jupiter`, `useJUnitPlatform`,  
  `platform launcher` 三个说法在被改的六份文档里逐句核  
  (`docs/dev/issues-cn/README.md:330`, `:353`, `docs/dev/issues-cn/archived/09-构建与CI/50-CI只编译不测试.md:32` 三处历史记载除外);  
  计数以 task 1 的实际数为准.  
  对应 design: 文档与事实一致; 落实 R-3.

## 验收

- 本地单测与仪器测试统一 JUnit 4, 26 类 116 方法 (按实际数) 全绿, 断言语义不变 (R-1).
- 桥接包与 Jupiter 平台接线删净, Robolectric 走原生 runner (R-2).
- 六份 living 文档与事实一致, 历史记载与 dated 报告原样 (R-3).
- 技术补充项: `assembleFossDebug` 与 `lintFossDebug` 绿; JVM 开关对照官方九条补齐;  
  Gradle 测试过滤与报告行为差异实跑核对.
- 判定格式与改动归属照 [13-工作流](../../agents-cn/13-工作流.md) 的 `测试与验收` 一节执行.

### 判定 (2026-11-23)

- **R-1 满足**: `:app:testFossDebugUnitTest` 25 个 suite 112 个测试, `:base:testDebugUnitTest`  
  1 个 suite 4 个测试, 合计 26 类 116 方法, 0 失败 0 错误 (结果 XML 时间 16:15 与 16:14);  
  断言只有 `HomeModelMapperTest` 的 2 处参数顺序调整, 断言语义不变; 仪器测试  
  `assembleFossDebugAndroidTest` 与 `assembleDebugAndroidTest` 编译通过, androidTest APK 已产出;  
  四个范围 grep 五个关键词零残留.  
- **R-2 满足**: `thirdParty.toml` 的桥接包版本键与库条目删净; 20 个测试类在  
  `@RunWith(RobolectricTestRunner::class)` 上跑通全量单测; 平台接线与 launcher 开关删净.  
- **R-3 满足**: 六份 living 文档改到与事实一致; 五个关键词只剩三处历史记载  
  (`docs/dev/issues-cn/README.md:330`, `:353`, `docs/dev/issues-cn/archived/09-构建与CI/50-CI只编译不测试.md:32`);  
  计数统一 26 类 116 方法, 三处写明构成; 历史记载与 dated 报告原样.  
- **技术补充项**: `assembleFossDebug` 与 `lintFossDebug` 绿, lint 报告是 "No errors or warnings",  
  被基线挡掉的 1 error 与 6 warnings 是旧账; JVM 开关对照官方九条后保留 1 条 `--add-opens` 加  
  `--enable-native-access`, 全量单测绿; 测试过滤与报告行为没有差异 (构建脚本无 Test filter 配置).  
- **改动归属**: 两笔 task 提交与后续修复都能归到 R-1, R-2, R-3, 技术补充项或负责人的新指令  
  (显式 import); 超出范围的改动无. analyze-file 已补跑, 0 个 ERROR, 检查级发现都是改动前就有的.
