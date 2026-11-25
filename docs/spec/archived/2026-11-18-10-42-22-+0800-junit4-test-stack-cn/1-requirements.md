# 需求文档: junit4-test-stack

> **状态**: living. 流程见 [13-工作流](../../agents-cn/13-工作流.md).  
> 本目录由旧三层结构 (plan.md 加 2 份 subtask 报告) 于 2026-11-22 按新工作流改造;  
> 旧文件原文在 git 历史 `ff3ed754` (plan 加 progress) 与 `40841c6a` (2 份报告).

## 原始需求 (负责人预写, 任务结束前不改)

问题: 测试依赖 已经与 Android 官方路线 偏离, 影响了 依赖升级  
打算: 按照 Android 官方路线, 全部使用 JUnit 4. 不使用 5 和 6 及其相关依赖和.  
风险: 测试 大部分是 slim ui spec 写的, 而 这个 spec 还没有做完, 且 已知 subtask-06 被外部 Agent 确认为有问题,  
需求已经理解错了, 正在被 外部 Agent 重写 (未提交, 处于 Review 阶段), 可能会本 spec 冲突  
(他可能还在用 JUnit 5 和 6). 之前写的 基于 5 和 6 的 各种测试, 可能也得重新跑测一遍

## 需求条目

工具链任务, docs/requirements-cn 的全局需求正文不覆盖; 验收标准来源一律标明.

**R-1 统一 JUnit 4**  
本地单测与仪器测试全部 JUnit 4, 不用 JUnit 5 和 6 及其相关依赖; 26 个测试类 116 个测试方法  
(app 25 类 112 方法, base 1 类 4 方法) 行为与断言语义不变.  
来源: 负责人预写的 "打算" 加 plan.md 范围; 计数按 2026-11-23 的仓库现状核对, 外部新增的  
`DetectionEntryOrderTest` (3 个方法) 已计入.  
验收: `:app:testFossDebugUnitTest` 与 `:base:testDebugUnitTest` 26 类 116 方法 0 失败;  
grep `tech.apter`, `org.junit.jupiter` 与 `org.junit.platform` 在 `app/src`, `base/src`, `build-logic/`, `gradle/toml/*.toml` 零残留.

**R-2 解除升级卡点**  
删掉停更的桥接包 `tech.apter.junit5.jupiter:robolectric-extension:0.9.0` 与 Jupiter 平台接线  
(`useJUnitPlatform()`, launcher interceptor 开关), Robolectric 走原生 runner  
(`@RunWith(RobolectricTestRunner::class)`), Robolectric 升级不再被桥接卡住  
(4.18 删掉了桥接包还继承的 `TextLayoutModeConfigurer`, 这是卡点本体; 升级本身归下一个独立单元).  
来源: 负责人预写的 "问题" 加 plan.md 现状与证据.  
验收: 桥接包的版本键与库条目从 `gradle/toml/thirdParty.toml` 删净; 测试在 JUnit 4 原生执行路径上全绿.

**R-3 文档与事实一致**  
living 文档里断言测试栈现状, 测试计数, 以及把已就位依赖写成缺口的句子, 改成改造后的事实  
(`docs/agents-cn/05-项目架构.md`, `docs/dev/issues-cn/` 的 #06, #43, README, #01, #19);  
历史决定的记载 (`docs/dev/issues-cn/README.md:330`, `:353`, `docs/dev/issues-cn/archived/09-构建与CI/50-CI只编译不测试.md:32`)  
与 dated 分析报告 (`docs/dev/2026-10-16-project-analysis-cn/`, `docs/dev/2026-10-30-agp-survey-cn/`) 保持原样.  
来源: plan.md 范围加负责人 2026-11-18 的决定.  
验收: `tech.apter` 与 `org.junit.jupiter` 在 `docs/agents-cn/05-项目架构.md` 与 `docs/dev/issues-cn/` 零残留;  
`Jupiter`, `useJUnitPlatform`, `platform launcher` 三个说法在被改的六份文档里逐句核 (上面三个历史记载处除外).

## 技术补充项 (不许替代主标准)

- `assembleFossDebug` 与 `lintFossDebug` 绿.
- 换 runner 后对照 Robolectric 官方 "Running with Java 17 and higher" 的九条 `--add-opens` 核一遍,  
  只补实际需要的, 并同步 `KotlinAndroid.kt` 的注释; 补完后的开关条数记进验收.
- 去掉 `useJUnitPlatform()` 之后 Gradle 的测试过滤与报告行为有无差异, 实跑核对 (待学习清单项).
