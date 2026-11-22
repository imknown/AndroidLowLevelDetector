# subtask-01 依赖与测试源码切到 JUnit 4

> **状态**: living. 事实核对时间 2026-11-18, 代码在 `17b450b9`.  
> 任务级计划见 [plan.md](plan.md); 流程见 [13-工作流](../../agents-cn/13-工作流.md).

## 目标

把本地单测从 "Jupiter 6.1.3 加 Jupiter 桥接包" 切到 "JUnit 4.13.2 加 Robolectric 原生  
runner", 删掉 Jupiter 与平台接线, 25 个测试类跟着换 import 与注解. 行为与断言语义不变.

## concept primer

- JUnit 5 之后平台分成 Launcher 与 Engine 两层. Gradle 的 `useJUnitPlatform()` 只把测试交给  
  Launcher, 真正跑测试的是 Engine. 去掉这个开关, Gradle 回到 JUnit 4 的默认执行路径.
- 本项目现在没有 JUnit 4 Engine (vintage), 却把 Robolectric 的 JUnit 4 runner 塞进平台,  
  靠的是停更的桥接包 `tech.apter...:0.9.0`, 它把 runner 伪装成 Jupiter 的 Extension.
- Robolectric 自己的官方用法就是 `junit:junit:4.13.2` 加 `@RunWith(RobolectricTestRunner::class)`,  
  runner 继承 JUnit 4 的 `BlockJUnit4ClassRunner`, 不需要任何桥.
- 改造后本地单测与仪器测试同属 JUnit 4, 断言 API 同一套, 测试代码可以在两个源集之间搬.
- 25 个测试类里 20 个带 `@ExtendWith(RobolectricExtension::class)`, 5 个不带; 断言只用到  
  六个方法, 都没有 message 参数, 所以是机械替换.

## 现状与证据

| 项 | 位置 | 现在是什么样 |
|---|---|---|
| junit 版本键 | `gradle/toml/android.toml:51` | `junit = "6.1.3"`, 上方 46-48 行注释写 "JUnit 4 is deliberately absent" |
| Jupiter 库条目 | `gradle/toml/android.toml:97-100` | `junit-jupiter` 与 `junit-platform-launcher` 两条, 带平台说明注释 |
| test bundle | `gradle/toml/android.toml:114` | `test = ["junit-jupiter", "junit-platform-launcher"]` |
| 桥接包版本键 | `gradle/toml/thirdParty.toml:30-33` | `junit5RobolectricExtension = "0.9.0"` 与停更说明 |
| 桥接包库条目 | `gradle/toml/thirdParty.toml:51-53` | `tech.apter.junit5.jupiter:robolectric-extension` |
| 测试接线 | `KotlinAndroid.kt:79-95` | 注释写 "单测跑平台", `it.useJUnitPlatform()`, 三个 jvmArgs 里含 launcher interceptor 开关 |
| 测试依赖 | `KotlinAndroid.kt:98-111` | mockk, robolectric, 桥接包三行 |
| 桥接测试类 | `app/src/test` 20 个类 | 带 `@ExtendWith(RobolectricExtension::class)` |
| 纯 JVM 测试类 | 5 个 | `DetectResultTest`, `MyModelTest`, `DetectToyboxTest`, `DetectWebViewTest`, `base/src/test/.../DateTimeExtTest` |
| 非测试源码 | `app/src/test/.../repository/LldFixture.kt` | 测试替身, 无 `org.junit` 引用 |
| 仪器测试 | `app/src/androidTest` 8 个文件, `base/src/androidTest` 1 个 | 已经是 `org.junit.Test` 加 `AndroidJUnit4`, 不动 |

## 计划改动

1. `gradle/toml/android.toml`
   - 46-51 行注释与版本键改成 JUnit 4: 版本键 `junit = "4.13.2"`, 注释写明本地单测与仪器测试  
     统一 JUnit 4, Robolectric 走原生 runner.
   - 删 97-100 行的 `junit-jupiter` 与 `junit-platform-launcher` 两条.
   - 97 行换成 `junit = { group = "junit", name = "junit", version.ref = "junit" }`.
   - 114 行的 `test` bundle 改成 `["junit"]`.
2. `gradle/toml/thirdParty.toml`
   - 删 30-33 行的 `junit5RobolectricExtension` 版本键与注释.
   - 删 51-53 行的桥接包库条目与注释.
3. `build-logic/.../KotlinAndroid.kt`
   - 79-80 行注释改成 "本地单测与仪器测试都用 JUnit 4, Robolectric 用原生 runner".
   - 删 85 行的 `it.useJUnitPlatform()`.
   - 删 91-93 行的 launcher interceptor 开关与注释, 保留 88-90 行的 `--add-opens` 与  
     `--enable-native-access`; 按验证那条补齐开关时, 同步重写 86-87 行的注释  
     (它现在只解释了 `jdk.internal.access` 这一条).
   - 删 107-110 行的桥接包依赖与注释.
4. 20 个桥接测试类
   - `@ExtendWith(RobolectricExtension::class)` 换成 `@RunWith(RobolectricTestRunner::class)`,  
     import 改成 `org.robolectric.RobolectricTestRunner`, 删 `tech.apter...RobolectricExtension`.
   - 断言 import 从 `org.junit.jupiter.api.Assertions.*` 换成 `org.junit.Assert.*`,  
     `org.junit.jupiter.api.Test` 换成 `org.junit.Test`.
   - 文件: DetectionEngineTest, RobolectricSpikeTest, HomeModelMapperTest, DetectAbTest,  
     DetectAdbAuthenticationTest, DetectAdbTest, DetectAndroidTest, DetectBuildIdTest,  
     DetectDeveloperOptionsTest, DetectDsuTest, DetectDynamicPartitionsTest, DetectEncryptionTest,  
     DetectKernelTest, DetectMainlineTest, DetectModeTest, DetectOutdatedTargetSdkTest,  
     DetectPerformanceClassTest, DetectSdkExtensionTest, DetectSecurityPatchesTest,  
     DetectVndkTest. 分散在 `ui/base/detect` (2 个), `ui/home/mapper` (1 个),  
     `ui/home/repository` (17 个) 三个子目录.
   - `DetectionEngineTest` 的 3 处 `assertThrows` 用 `org.junit.Assert.assertThrows`,  
     签名兼容.
   - 另改 `RobolectricSpikeTest.kt:12-14` 的文件头注释: 原文说它 "runs on the JUnit  
     platform", 改造后改成走 JUnit 4 原生 runner 的常驻冒烟.
5. 5 个纯 JVM 测试类: 只换 import 与 `@Test`, 断言调用一行不动 (这 5 个都没有  
   `assertThrows`).
6. 断言顺序核对: 没有 message 参数; 只有 `assertEquals` 与 `assertSame` 是两个实参,  
   `assertTrue` / `assertFalse` / `assertNull` 是一个实参, `assertThrows` 的第二个实参是  
   lambda. 换到 JUnit 4 参数顺序一致, 不需要重排.

## 有意不改

- `mockk` (`thirdParty.toml`) 与 `robolectric` 4.17 两条依赖不动.
- `isIncludeAndroidResources` 与 `--add-opens` / `--enable-native-access` 保留.
- `androidTest` 两侧的测试与依赖不动.
- `LldFixture.kt` 不是测试类, 不动.
- `gradle/toml/review/` 保留到负责人处理完 Robolectric 后续项再删, 本单元不碰  
  (它是上一轮 toml 任务的 review 目录, 目录自己记着这条收尾条件).
- `gradle/toml` 其余三份 catalog 与根 / 模块构建脚本不动.

## 覆盖核对表

| 计划里的范围 | 落点 | 是否覆盖 |
|---|---|---|
| 依赖与接线切 JUnit 4 | 改动 1, 2, 3 | 是 |
| 25 个测试类换 import 与注解 | 改动 4, 5 | 是 |
| 断言语义不变 | 改动 6 加验证里的全量单测 | 是 |
| Robolectric 4.18 升级 | 不属于本子任务, 归下一个独立单元 | 留给后续 |

## 验证

- `./gradlew :app:testFossDebugUnitTest :base:testDebugUnitTest`, 期望 25 个类 111 个方法  
  0 失败.
- `./gradlew assembleFossDebug` 与 `./gradlew lintFossDebug`.
- 换 runner 后对照 Robolectric 官方 "Running with Java 17 and higher" 的九条  
  `--add-opens` 核一遍, 只补实际需要的, 并同步 86-87 行的注释; 补完后的开关条数记进  
  本子任务的结果 (subtask-02 落笔时以它为准, 不在本子任务改文档).
- grep `tech.apter` 与 `org.junit.jupiter` 在 `app/src`, `base/src`, `build-logic/`,  
  `gradle/toml/*.toml` 零残留.

## 风险与回滚

- 风险低: 机械替换, 唯一接线变化是去掉平台与桥接, 由同一套测试验证.
- 回滚: 单笔提交整体回滚即可, 没有数据或行为迁移.
