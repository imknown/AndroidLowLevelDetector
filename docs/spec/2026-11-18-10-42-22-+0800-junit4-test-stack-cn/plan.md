# 全 JUnit 4 测试栈改造 (junit4-test-stack)

> **状态**: **living** (spec 任务, 进行中). 任务级计划, 流程见  
> [13-工作流](../../agents-cn/13-工作流.md).

## 负责人预写 (AI 开始之前)

问题: 测试依赖 已经与 Android 官方路线 偏离, 影响了 依赖升级  
打算: 按照 Android 官方路线, 全部使用 JUnit 4. 不使用 5 和 6 及其相关依赖和.  
风险: 测试 大部分是 slim ui spec 写的, 而 这个 spec 还没有做完, 且 已知 subtask-06 被外部 Agent 确认为有问题,  
需求已经理解错了, 正在被 外部 Agent 重写 (未提交, 处于 Review 阶段), 可能会本 spec 冲突  
(他可能还在用 JUnit 5 和 6). 之前写的 基于 5 和 6 的 各种测试, 可能也得重新跑测一遍

## 现状与证据

- 本地单测走 Jupiter 6.1.3, 由 `useJUnitPlatform()` 接线  
  (`build-logic/.../KotlinAndroid.kt:85`), 依赖在  
  `gradle/toml/android.toml:51` 的 `junit` 键与 `test` bundle.
- 25 个测试类, 111 个测试方法 (`app/src/test` 24 个类 107 个方法, `base/src/test` 1 个类 4 个方法).
- 其中 20 个类靠停更的桥接包 `tech.apter.junit5.jupiter:robolectric-extension:0.9.0`  
  把 Robolectric 抬进 Jupiter 平台; 该包最后发布 2024-11-18, 上游最后提交 2025-06.
- Robolectric 4.18 删掉了桥接包还继承着的 `TextLayoutModeConfigurer`, 所有 Robolectric  
  测试类在 4.18 下类加载失败, 依赖升级因此被卡住.
- 官方口径: Android 本地单测要求 JUnit 4 (`developer.android.com/training/testing/local-tests`),  
  仪器测试要求 JUnit 4 加 `AndroidJUnitRunner`, Compose UI 测试用 `ui-test-junit4`;  
  Robolectric 的 getting-started 也是 `junit:junit:4.13.2` 加  
  `@RunWith(RobolectricTestRunner::class)`; Kotlin 侧推荐 JUnit 6, 但只针对纯 JVM 测试.
- 现有测试只用到 `@Test`, `@ExtendWith`, `assertEquals`, `assertTrue`, `assertFalse`,  
  `assertThrows`, `assertSame`, `assertNull`; 没有 `assertAll`, `@ParameterizedTest`,  
  `@Nested`, `@DisplayName` 或生命周期回调; 断言调用没有 message 参数, 只有  
  `assertEquals` 与 `assertSame` 是两个实参, 其余一个实参或尾随 lambda.  
  所以改成 JUnit 4 是纯机械替换, 参数顺序不变.
- 本任务开始时的 HEAD 是 `9318f6b8`, 工作树除本 spec 目录外干净;  
  subtask-06 的外部重写不在这个工作树里.

## 范围

做:

- 依赖目录与约定插件的测试接线切到 JUnit 4.
- 25 个测试类换 runner 与断言来源.
- 文档与事实一致: `05-项目架构`, `06-技术栈缺口`, `43-比对语义逐行adhoc`, `README`,  
  `01-无领域模型`, `19-版本合并残留弱点` 里断言测试栈现状, 测试计数, 以及把已就位依赖  
  写成缺口的句子.

不做:

- Robolectric 4.17 升 4.18, 归下一个独立单元.
- Compose UI 测试基建, 按 #06 的排期另立项.
- `docs/spec/2026-10-25-22-16-01-+0800-slim-ui-layer-cn/` 下的任何文件.

## 子任务

### subtask-01 依赖与测试源码切到 JUnit 4

- **范围**: `gradle/toml/android.toml` (junit 键改成 4.13.2, 删 jupiter 与 platform  
  launcher 条目, `test` bundle 只留 junit, 重写注释), `gradle/toml/thirdParty.toml`  
  (删桥接包的键与库条目), `build-logic/.../KotlinAndroid.kt` (去掉  
  `useJUnitPlatform()`, 去掉 launcher interceptor 开关与桥接依赖, 保留 `--add-opens`  
  与 `--enable-native-access`), 25 个测试类的 import 与注解.
- **凭什么是独立的一步**: 删依赖与改测试必须同一笔, 拆开会出现编译不过的中间态;  
  改完后 `:app` 与 `:base` 的单测任务可以独立跑绿.
- **风险级别**: 低. 全是机械替换, 唯一的接线变化是去掉平台与桥接, 由同一套测试验证.
- **生疏领域**: 否.
- **验证**: `:app:testFossDebugUnitTest` 与 `:base:testDebugUnitTest` 期望 25 类 111 方法  
  0 失败; `assembleFossDebug`; `lintFossDebug`; 换 runner 后对照 Robolectric 官方  
  "Running with Java 17 and higher" 的九条 `--add-opens` 核一遍 JVM 开关,  
  只补实际需要的, 并同步 `KotlinAndroid.kt:86-87` 的注释;  
  grep `tech.apter` 与 `org.junit.jupiter` 零残留的范围限定为 `app/src`, `base/src`,  
  `build-logic/` 与 `gradle/toml/*.toml`;  
  `05-项目架构.md` 由 subtask-02 自己的验证管 (那里有一条同样的 grep); 不含 spec 目录  
  (记录性质的正文按 `07` 保持原样, 上一轮的 `gradle/toml/review/` 处理完即删).

### subtask-02 文档与事实一致

- **范围**: `docs/agents-cn/05-项目架构.md` 的测试段 (64-73) 改成"单一 JUnit 4 方言",  
  以及同文件 75-82 那段 Robolectric 桥接说明 (要改成原生 runner, 三个 JVM 开关里  
  launcher interceptor 那条随之删掉); `docs/dev/issues-cn/01-架构与分层/06-技术栈缺口.md`  
  的 :14 与 :20-23 的 JUnit 描述, :27 的测试计数 (25 类, 111 个方法), 以及它把  
  Robolectric 列进 "缺" (19) 与 "待补齐" (42) 的错句; `docs/dev/issues-cn/08-反模式与卫生/43-比对语义逐行adhoc.md`  
  的 :54 (:64) 现状断言句, 并核对 :55 的 "16 个 Detect*Test"  
  (按 "直接测 HomeRepository 的检测方法" 的口径实数为 16, 与原文一致, 不需要改); `docs/dev/issues-cn/README.md:268`,  
  `docs/dev/issues-cn/01-架构与分层/01-无领域模型.md:80`,  
  `docs/dev/issues-cn/05-稳定性与错误处理/19-版本合并残留弱点.md:24-25` 的过期测试计数;  
  同文件 :41 的 "JUnit Jupiter 与 MockK 已随 1b 落地" 改成 JUnit 4 与 MockK 已就位,  
  与 :14 的说法保持一致;  
  核对 subtask-01 改过的 catalog 注释措辞.  
  以上除 :41 外, 均按负责人 2026-11-18 的决定收进本子任务; :41 是主 agent 按事实层  
  裁决的 (见"review 的处置"一节).
- **有意不改**: `docs/dev/2026-10-16-project-analysis-cn/` 与  
  `docs/dev/2026-10-30-agp-survey-cn/` 下的分析报告,  
  `docs/spec/2026-10-25-...-slim-ui-layer-cn/` 的 plan 与 subtask 报告,  
  `docs/dev/issues-cn/README.md:353` 与 `:330`, `docs/dev/issues-cn/archived/09-构建与CI/50-CI只编译不测试.md:32`  
  这几处记 1b 与 0e 那次决定的句子 (记的是当时怎么定的, 不是当前现状).
- **凭什么是独立的一步**: 只改文字, 不碰代码, 不需要构建, 可以单独提交与核对.
- **风险级别**: 低, 机械.
- **生疏领域**: 否.
- **验证**: 逐条对照官方文档与改完后的代码现状; grep `tech.apter` 与  
  `org.junit.jupiter` 在 `docs/agents-cn/05-项目架构.md` 与 `docs/dev/issues-cn/` 零残留;  
  另按 `Jupiter`, `useJUnitPlatform`, `platform launcher` 三个说法在被改的六份文档里  
  逐句核, "有意不改" 的 `README.md:330` 与 `:353` 及 `docs/dev/issues-cn/archived/09-构建与CI/50-CI只编译不测试.md:32`  
  排除在外;  
  不跑构建.

## 覆盖核对 (对负责人的预写)

| 预写里的点 | 由哪一条承担 |
|---|---|
| 测试依赖偏离 Android 官方路线, 影响依赖升级 | subtask-01 的依赖与接线 |
| 全部 JUnit 4, 不用 5 和 6 及其依赖 | subtask-01 的依赖与源码; subtask-02 的文档 |
| slim ui spec 还没做完 | 范围明确不碰该目录; 顺序见下一节 |
| subtask-06 被外部重写, 可能仍用 5 和 6 | 见下一节, 顺序由负责人定 |
| 基于 5 和 6 的测试要重新跑一遍 | subtask-01 的验证跑全量; 外部改动落地后复跑 |

## 外部冲突与顺序 (负责人 2026-11-18 定)

- 这个工作树里没有 subtask-06 的重写改动 (HEAD `9318f6b8`, 工作树只有本 spec 目录  
  未跟踪), 重写在别的上下文里, 尚未落地.
- 负责人定: **等外部重写先落地并提交, 再开 subtask-01**. 本 spec 在此之前挂起.
- 恢复时的动作: 先重新核对 HEAD 与工作树, 把外部新增或改过的测试逐个看过,  
  仍在用 Jupiter 的当场改成 JUnit 4, 再按 subtask-01 的范围动手;  
  若外部改动动了本 spec 范围里的测试文件, 应改清单按新状态重新枚举.
- 外部改动落地后, 预写里的最后一个风险 (基于 5 和 6 的测试要重跑一遍) 由  
  subtask-01 的全量验证一并覆盖.

## review 的处置 (v1 到 v4)

- 第一档四处已当场改计划: grep 验证口径限定范围; `43-比对语义逐行adhoc.md:64` 的现状  
  断言挂进 subtask-02; "有意不改" 补上 `docs/dev/issues-cn/archived/09-构建与CI/50-CI只编译不测试.md:32`, `README.md:330` 与 `:353`,  
  以及 `2026-10-30-agp-survey-cn/`; `--add-opens` 从待学习移进 subtask-01 的验证.
- 第二档一条: slim-ui spec 的正文仍写 Jupiter, 本单元不碰, 记进 `progress.md` 的归后续.
- 第三档两件 (06 的 Robolectric 缺口, 43:55 的 16 与 19) 与四处过期计数,  
  负责人 2026-11-18 定: 收进 subtask-02. 2026-11-19 按口径重数后, 43:55 的 16 是准的  
  (16 个直接调检测方法, 3 个只调 match 函数), 该句保持原文, 只把 54 行计数与这句的  
  复核结论写进报告.
- `README.md:330` 那句 "JUnit 5" 是 0e 那次决定的记载, 按"有意不改"保留, 不在  
  subtask-02 的范围里.
- v2 两条第一档: subtask-01 的 grep 范围移出 `05-项目架构.md` 并写明两个字符串;  
  `README.md:330` 的处置两节改成一致.
- v3 一条第一档 (workbuddy 判 met, mavis 判 not met, 分歧按事实层核过): `06-技术栈缺口.md:41`  
  说 "JUnit Jupiter 已随 1b 落地", 与同文件 :14 改完后的 JUnit 4 说法相反, 不宜留在  
  "有意不改". 采 mavis 的修法, 把它收进 subtask-02 改成 JUnit 4, 并给 subtask-02 的验证  
  补一条针对 `05-项目架构.md` 与 `docs/dev/issues-cn/` 的 grep.
- v4: 两份报告都判 met, workbuddy 撤回了 v3 的判定. 三条精度备注按"顺手改"处理:  
  06 的范围补 :14 与 :20-23 的行号; 验证加 `Jupiter` / `useJUnitPlatform` /  
  `platform launcher` 三个说法的逐句核对; 本节标题的轮次范围改到 v4,  
  并把 :41 的裁决与负责人决定的归属分开写.

## 待学习清单

- 去掉 `useJUnitPlatform()` 之后, Gradle 的测试过滤与报告行为有无差异, 实际跑一遍核对.

## 提交切分

- subtask-01 一笔, subtask-02 一笔. 都按 Conventional Commits 加 trailer.
