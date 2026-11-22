# 设计文档: junit4-test-stack

> **状态**: living. 由旧 plan.md 与 2 份 subtask 报告的方案内容重组 (2026-11-22);  
> 技术结论与负责人决定原样保留, 原文见 git 历史 `ff3ed754` 与 `40841c6a`.

## 风险分级

低. 两个 task 都是机械替换与文字改到与事实一致, 不生疏; 唯一的接线变化是去掉平台与桥接, 由同一套测试验证.

## 现状前提

- 本地单测走 Jupiter 6.1.3, 由 `useJUnitPlatform()` 接线  
  (`build-logic/.../KotlinAndroid.kt:85`), 依赖在 `gradle/toml/android.toml:51` 的 `junit` 键与 `test` bundle.
- 25 个测试类 111 个方法 (`app/src/test` 24 类 107 方法, `base/src/test` 1 类 4 方法);  
  其中 20 个类靠桥接包把 Robolectric 抬进 Jupiter 平台, 该包最后发布 2024-11-18, 上游最后提交 2025-06;  
  Robolectric 4.18 删掉了桥接包还继承着的 `TextLayoutModeConfigurer`, 升级因此被卡.
- 官方口径: Android 本地单测要求 JUnit 4 (developer.android.com/training/testing/local-tests),  
  仪器测试要求 JUnit 4 加 `AndroidJUnitRunner`, Compose UI 测试用 `ui-test-junit4`;  
  Robolectric 官方用法就是 `junit:junit:4.13.2` 加 `@RunWith(RobolectricTestRunner::class)`;  
  Kotlin 侧推荐 JUnit 6 但只针对纯 JVM 测试.
- 现有测试只用到 `@Test`, `@ExtendWith`, `assertEquals`, `assertTrue`, `assertFalse`, `assertThrows`,  
  `assertSame`, `assertNull`; 没有 message 参数, 只有 `assertEquals` 与 `assertSame` 是两个实参,  
  其余一个实参或尾随 lambda. 换 JUnit 4 参数顺序一致, 是纯机械替换.
- plan 写作时的 HEAD 是 `9318f6b8`; 之后外部 subtask-06 重写已实施完成并提交  
  (本地已 rebase, 2026-11-19 核过 `:app:testFossDebugUnitTest` 绿).  
  task 1 开工时先重新核对 HEAD 与工作树 (见 design 的外部冲突一节).
- `gradle/toml/review/` 是上一轮 toml 任务的 review 目录, 保留到负责人处理完 Robolectric 后续项再删,  
  两个 task 都不碰.

## 技术方案

**依赖与接线切换** (task 1):

- `gradle/toml/android.toml`: 版本键 `junit` 改 `"4.13.2"`, 上方注释写明本地单测与仪器测试统一 JUnit 4,  
  Robolectric 走原生 runner; 删 `junit-jupiter` 与 `junit-platform-launcher` 两条库条目,  
  换成 `junit = { group = "junit", name = "junit", version.ref = "junit" }`;  
  `test` bundle 改成 `["junit"]`.
- `gradle/toml/thirdParty.toml`: 删 `junit5RobolectricExtension` 版本键与桥接包库条目及各自注释.
- `build-logic/.../KotlinAndroid.kt`: 注释改成 "本地单测与仪器测试都用 JUnit 4, Robolectric 用原生 runner";  
  删 `it.useJUnitPlatform()`; 删 launcher interceptor 开关与注释, 保留 `--add-opens` 与  
  `--enable-native-access` (按验证那条对照 Robolectric 官方九条补齐, 同步重写 86-87 行注释);  
  删桥接包依赖与注释. mockk 与 robolectric 4.17 不动.
- 25 个测试类换 import 与注解: 20 个桥接类 `@ExtendWith(RobolectricExtension::class)` 换  
  `@RunWith(RobolectricTestRunner::class)` (import `org.robolectric.RobolectricTestRunner`),  
  断言 import `org.junit.jupiter.api.Assertions.*` 换 `org.junit.Assert.*`,  
  `org.junit.jupiter.api.Test` 换 `org.junit.Test`; `DetectionEngineTest` 的 3 处 `assertThrows` 用  
  `org.junit.Assert.assertThrows` (签名兼容); 5 个纯 JVM 类  
  (`DetectResultTest`, `MyModelTest`, `DetectToyboxTest`, `DetectWebViewTest`, `base` 的 `DateTimeExtTest`) 只换 import 与 `@Test`,  
  断言调用一行不动. 20 个桥接类清单: DetectionEngineTest, RobolectricSpikeTest, HomeModelMapperTest,  
  DetectAbTest, DetectAdbAuthenticationTest, DetectAdbTest, DetectAndroidTest, DetectBuildIdTest,  
  DetectDeveloperOptionsTest, DetectDsuTest, DetectDynamicPartitionsTest, DetectEncryptionTest,  
  DetectKernelTest, DetectMainlineTest, DetectModeTest, DetectOutdatedTargetSdkTest,  
  DetectPerformanceClassTest, DetectSdkExtensionTest, DetectSecurityPatchesTest, DetectVndkTest  
  (分散在 `ui/base/detect`, `ui/home/mapper`, `ui/home/repository` 三个子目录).  
  `RobolectricSpikeTest.kt` 的文件头注释随改造改: "runs on the JUnit platform" 改成 JUnit 4 原生 runner 的  
  常驻冒烟. `LldFixture.kt` 不是测试类, 不动.
- 仪器测试 (`app/src/androidTest` 8 个文件, `base/src/androidTest` 1 个) 已经是  
  `org.junit.Test` 加 `AndroidJUnit4`, 不动; 仪器侧那份 JUnit 4 仍由  
  `androidx.test.ext:junit` 与 `androidx.test:runner` 传递带入, 本机侧那份改由版本目录给.

**文档与事实一致** (task 2), 逐处清单:

| 文件 | 位置 | 现在的句子 |
|---|---|---|
| `docs/agents-cn/05-项目架构.md` | 64-73 | 测试栈分两套, 本机单测跑 Jupiter, classpath 故意不放 JUnit 4 |
| 同上 | 75-82 | Robolectric 经 Jupiter 桥接加载, 三个测试 JVM 开关 |
| `docs/dev/issues-cn/01-架构与分层/06-技术栈缺口.md` | 9-14 | 已就位含 JUnit Jupiter 与 platform launcher |
| 同上 | 19, 20-23, 27 | Robolectric 列在 "缺"; test bundle 挂 Jupiter; 计数 21 类 82 方法 |
| 同上 | 41 | "JUnit Jupiter 与 MockK 已随 1b 落地" |
| 同上 | 42 | Robolectric 在 "待补齐" |
| `docs/dev/issues-cn/08-反模式与卫生/43-比对语义逐行adhoc.md` | 54, 64 | 计数 21 类 82 方法; test bundle 是 Jupiter |
| `docs/dev/issues-cn/README.md` | 268 | 真实测试 21 类 82 方法 |
| `docs/dev/issues-cn/01-架构与分层/01-无领域模型.md` | 80 | 21 类 82 方法 |
| `docs/dev/issues-cn/05-稳定性与错误处理/19-版本合并残留弱点.md` | 24-25 | 21 类 82 方法 |

改法: 05-项目架构 改成单一 JUnit 4 方言: 65-67 整句改写, 不拆; 75-82 改原生 runner, 删桥接说明与  
launcher interceptor 句, "三样" 改实际条数; #06 的 14 行改 `JUnit 4`, 20-23 改 bundle 只挂 `junit`  
并删 `useJUnitPlatform()` 句, 19 与 42 从缺与待补里删 Robolectric, 27 行计数改 25 类 111 方法,  
41 行改 "JUnit 4 与 MockK 已就位" (1b 落的是 Jupiter 与 MockK, JUnit 4 是本 spec 落的, 不能写成随 1b);  
43 的 54 行计数改, 55 行保持原样 (按 "直接测检测方法" 口径实数 16, 已核准), 64 行改 `junit`;  
README:268, #01:80, #19:24-25 三处计数统一 25 类 111 方法 (写明 app 24/107, base 1/4);  
核对 task 1 改过的 catalog 注释与文档口径一致.

## 方案取舍

无方案取舍. 理由: 低风险机械替换; 官方口径唯一  
(Android 官方路线与 Robolectric 官方用法都是 JUnit 4 原生 runner), 现有断言只用到六个方法且参数顺序与 JUnit 4 一致, 替换不存在需要负责人挑的两案.

## 覆盖映射

R-1 → task 1 的依赖切换加 25 类替换加全量单测; R-2 → task 1 的桥接与平台删除;  
R-3 → task 2 的文档清单加 grep 验证.

## 待学习清单

- 去掉 `useJUnitPlatform()` 之后, Gradle 的测试过滤与报告行为有无差异, 实际跑一遍核对.

## 外部冲突与顺序

- 负责人 2026-11-18 定: 等外部 subtask-06 重写先实施完成并提交, 再开工; 该条件已满足  
  (2026-11-19 实施, 本地 rebase 后单测绿).
- 恢复动作 (task 1 开工时): 先重新核对 HEAD 与工作树, 外部新增或改过的测试逐个看过,  
  仍在用 Jupiter 的当场改成 JUnit 4, 再按 task 1 范围动手; 外部改动动了本 spec 范围里的测试文件时,  
  应改清单按新状态重新枚举. 外部改动实施后, 预写里旧测试要重跑的那条风险  
  由 task 1 的全量验证一并覆盖.
- 不碰: `docs/spec/2026-10-25-22-16-01-+0800-slim-ui-layer-cn/` 下的任何文件 (归那个任务自己更新).
