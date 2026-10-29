# subtask-02 基线快照与测试栈

> **状态**: living. 事实核对时间 2026-10-27, 代码在 `502a6eb7`.  
> 任务级计划见 [plan.md](plan.md); 流程见 [13-任务工作流](../../agents-cn/13-任务工作流.md).

## 目标

在动核心代码之前, 先把测试栈装好, 再把三页每行的现状导成可逐行对照的黄金数据,  
让 subtask-04 的一次性迁移有一个可核对的对照物.

## concept primer

- 三页各自的条目由 ViewModel 逐行调用仓库方法拼出来: Home 在 `HomeViewModel.detect()`  
  (HomeViewModel.kt:206-244), Others 在 `OthersViewModel.collectModels()` (37 行 `tempModels +=`),  
  Prop 在 `PropViewModel.collectModels()` (5 个来源).
- 每行的最终形态是 `MyModel(title, detail, color, type)` (MyModel.kt:18-28). 界面读四个字段:  
  `key` (标题的资源 ID 字符串或原始标题文案), `detail`, `color`, 以及 `type` (列表复用池按它分池,  
  MyModelListScreen.kt:132).
- 所以"行为等价"可以写成一句可核对的话: 三页每行的 `(key, detail, color, type)` 与改造前逐行相同.
- 测试依赖现在由约定插件统一挂: `build-logic/convention/src/main/kotlin/.../KotlinAndroid.kt:72-83`  
  里的 `configureTest()` 读 `libsAndroid.findBundle("test")`, 而这个 bundle 只有一个 JUnit 4  
  (`gradle/toml/android.toml:104`, 版本在同一份文件的 Test 段).
- 仓里现有测试: `base` 的 `DateTimeExtTest` 有四个真方法, 其余四个类是模板 (`ExampleUnitTest` 两个,  
  `ExampleInstrumentedTest` 两个). 前四个方法写的是 JUnit 4 断言 (`org.junit.Assert`, `org.junit.Test`).
- `Build.*`、shell 探针、`AssetManager` 都在仓库方法的执行路径上, 所以纯 JVM 测试跑不了仓库方法.  
  黄金数据只能从真机或模拟器上取.

## 现状与证据

| 项 | 位置 | 现在是什么样 |
|---|---|---|
| 测试依赖接线 | KotlinAndroid.kt:72-83 | 两个 bundle: `test` 与 `androidTest`, 都从 `libsAndroid` 取 |
| test bundle 内容 | gradle/toml/android.toml:104 | 只有 `junit` (JUnit 4) |
| 现有真测试 | base/src/test/.../DateTimeExtTest.kt | 四个方法, JUnit 4 写法 |
| 模板测试 | app 与 base 各两个类 | 无实际内容 |
| 检测条目数 | Home 23 个调用产 25 行; Others 37 次追加; Prop 5 个来源 | 每次加载动态行数还会变 (`getPartitionFingerprints`) |

## 计划改动

1. 依赖与接线.

   - 已定 (负责人 2026-10-28): JUnit 5 放 `gradle/toml/android.toml` 的 Test 段, 理由是 AGP 的测试底座本来就依赖  
     JUnit 4, 两份放一起好对照; MockK 放 `gradle/toml/thirdParty.toml`.
   - 落点有三处, 缺一处就接不上:
     1. `gradle/toml/android.toml:104` 的 `test = ["junit"]` 加 JUnit 5 的条目 (版本在同一份文件的 Test 段).
     1. `gradle/toml/thirdParty.toml` 加 MockK 的版本与库, 并解开  
        `build-logic/convention/src/main/kotlin/.../ext/ProjectExtensions.kt:25` 那行被注释掉的  
        `libsThirdParty` 访问器; 不解开, 约定插件拿不到这份目录.
     1. `build-logic/convention/src/main/kotlin/.../android/KotlinAndroid.kt:79` 的  
        `testImplementation(libsAndroid.findBundle("test").get())` 是 JUnit 5 进 classpath 的唯一通路;  
        MockK 另挂一条 `testImplementation(libsThirdParty.findLibrary("mockk").get())`.  
     写成 `libsThirdParty.mockk` 编译不过: 约定插件里的 `libsThirdParty` 是 `ProjectExtensions.kt:21-25`  
     那个 `VersionCatalog` 委托, 它没有类型安全访问器, 同文件既有写法都是 `findBundle(...)` /  
     `findLibrary(...)` (KotlinAndroid.kt:79, :93); `libsThirdParty.mockk` 那种写法只在模块的  
     `build.gradle.kts` 里才生成.
   - 需要的条目: `junit-jupiter` (aggregate), `junit-platform-launcher` (Gradle 跑 Jupiter 需要它), `mockk`.  
     Turbine 随第一个 Flow 测试再进, 本子任务不引.
   - `useJUnitPlatform()` 是**新加**的接线, `configureTest()` 现在只设 `testInstrumentationRunner` 与挂两个 bundle  
     (KotlinAndroid.kt:72-83), 里面没有这块. 写法 `testOptions.unitTests.all { it.useJUnitPlatform() }`,  
     加在 `configureTest()` 或 `configureAndroid()` 里.
   - 这一层被三个模块吃到: `app`, `base`, 还有 `binderDetector` (它也 apply 了 library 约定插件).  
     binderDetector 没有测试源码, 只是多挂一遍.
   - `androidTest` 那一路继续用 JUnit 4: 设备上的 `AndroidJUnitRunner` 与 `androidx.test.ext:junit`  
     都是 JUnit 4 系, 换掉要连 runner 一起换, 不在本步.

1. 存量测试怎么处理.

   - A. 只跑 Jupiter, 把 `DateTimeExtTest` 的四个方法改成 Jupiter 写法 (`org.junit.jupiter.api.Test`  
     与 `Assertions`), 删掉 `app` 与 `base` 那两个 `ExampleUnitTest` 模板类. **推荐**.
   - B. 加 `junit-vintage-engine`, JUnit 4 测试原样继续跑.
   - 选 A: 存量只有四个方法, 改动是 import 与断言迁移; 留着 vintage engine 会让两套断言长期并存,  
     以后写测试的人不知道该跟哪一套.
   - 两个 `ExampleInstrumentedTest` 保留. `androidTest` 那一路仍是 JUnit 4; 要让仪器测试也上 JUnit 5,  
     得连 runner 一起换, 那是另一件事.

1. 黄金数据.

   三个候选, 差别在生成器靠什么读数据:

   - A. 仪器测试里复刻三页的调用序列, 直接从仓库取 `MyModel`, 序列化成 json.  
     不引新框架, 不动产品代码; 代价是 subtask-04 之后生成器要跟着改一次.
   - B. Compose UI 测试, 从屏幕上读每行的标题, 详情与圆点颜色. 最接近用户看到的东西;  
     但要引入 Compose 测试依赖与规则, 与 [#06](../../dev/issues-cn/01-架构与分层/06-技术栈缺口.md) 里  
     "Compose UI 测试随检测改造完成后另立项" 那句冲突, 需要老板点头.
   - C. 手工整理一张表. 不可重复, 也不可信.

   **已定 A** (负责人 2026-10-28): 不动产品代码, 不引新框架, 只多一个一次性的仪器测试类.

   - 落点: `app/src/androidTest/.../snapshot/ModelSnapshotTest.kt`, 输出到应用外部文件目录,  
     用 `adb pull` 取回, 存 `docs/spec/<本目录>/baseline/` 下. 不进 CI.
   - 数据形状: 每行 `{ page, index, key, detail, color, type }`. `page` 用来区分三页, `index` 保留展示顺序.
   - `type` 必须在里面: 界面读它 (`MyModelListScreen.kt:132` `contentType = { it.type }`),  
     它也是 subtask-01 第一件改成按类型寻址的依据. 快照漏了它, subtask-04 迁移时把某行的 `type` 改掉,  
     逐行对照根本发现不了.
   - 生成时机: 在 subtask-01 之后跑一次. subtask-01 改变了挂载失败的处理, 快照要拍在它之后,  
     否则 2a 的对照会带上一件与它无关的差异.

1. 规则文件的测试段.

   - 要改的是 [04-架构](../../agents-cn/04-架构.md) 的 "接口优先的可测试性" 那一节 (04-架构.md:37-42).  
     它写着 `能测, 靠的是接口优先的设计, 不是靠 mock 框架`; 引入 MockK 之后这句就成了错话, 要改写成  
     "接口优先让替身好写, MockK 只在需要验证交互时用" 之类的表述.
   - [09-构建约定](../../agents-cn/09-构建约定.md) 与 [11-构建与验证](../../agents-cn/11-构建与验证.md) 里  
     没有"测试只有 `DateTimeExtTest`"这类描述 (09 只有 CI 与版本目录两段, 11 只有三条命令),  
     不要照 plan.md 的措辞去这两份文件里加一句无中生有的话.
   - 决定批 0e 说的 "规则文件的测试段随 1b 校准", 指的就是上面这一处.

## 有意不改

- 产品代码一行不动.
- Compose UI 测试、Espresso 不引. 按 #06 的排期另立项.
- 检测条目目录不动.

## 覆盖核对表

| 计划里的范围 | 问题条目 | 本子任务覆盖 | 留给后续 |
|---|---|---|---|
| JUnit 5 与 MockK 引入 | [#06](../../dev/issues-cn/01-架构与分层/06-技术栈缺口.md), [#50](../../dev/issues-cn/09-构建与CI/50-CI只编译不测试.md) | 是 (测试栈部分) | CI 扩跑归 subtask-03 |
| 三页现状导成黄金数据 | [#01](../../dev/issues-cn/01-架构与分层/01-无领域模型.md) 的验收前提 | 是 | 逐条单测归 subtask-04 |
| Turbine | #06 | 不引 | 第一个 Flow 测试时再进 |
| 规则文件的测试段 | 0e | 是 | 检测条目流程的改写归 subtask-04 |
| 需求条目 | FR-14 共用的检测列表基础 | 只做快照 | 结构改造归 subtask-04 与 05 |

## 问题清单的回答

老板没有给问题清单.

## 验证

- `./gradlew testFossDebugUnitTest :base:testDebugUnitTest` 全绿. 这里有个比"看报告里的类数量"更硬的办法:  
  本仓 Gradle 在 9 以上 (见 `gradle/wrapper/gradle-wrapper.properties`), `Test` 任务的  
  `failOnNoDiscoveredTests` 默认为 true, 所以 `:base` 留着 `DateTimeExtTest` 却一个方法都没被 Jupiter 跑到时,  
  任务自己会红.
- `./gradlew assembleFossDebug` 与 `./gradlew lintFossDebug` 不回归.
- 黄金数据生成器在真机或模拟器上跑通一次, json 行数与界面行数对得上.  
  这一条要人工点一遍界面核对, 不能只看 json 条数.

## 风险

- JUnit 5 与当前 AGP 版本 (RC 线, 见 `gradle/toml/android.toml` 的 `androidGradlePlugin` 键) 的兼容性  
  没有先例可查. 2026-10-28 跑过一次四个任务, 现有 JUnit 4 的路是通的, 但那一次没有引入 Jupiter,  
  所以这一条仍然未知. 首跑出问题时的退路是加 vintage engine, 只换跑法, 不换断言库.
- 快照生成器在 subtask-04 之后要改一次. 这是 A 方案的已知成本, 不算返工.
- 动态行数 (分区指纹, Prop 页) 让快照带不确定性. 报告里要写明: 同一台设备上连续跑两次,  
  行数与内容应当一致; 不一致就先查清楚再往下走.

## 需要老板定

- 已定三项 (2026-10-28): JUnit 5 放 `android.toml`, MockK 放 `thirdParty.toml` 并解开 `libsThirdParty`  
  访问器; 存量四个方法迁 Jupiter, 仪器测试保持 JUnit 4; 黄金数据用仪器测试生成.
- 剩下一个没查清: JUnit 5 与当前 AGP 版本 (RC 线) 的兼容性. 要跑一次构建才知道,  
  这也是本任务里唯一可能推翻上面第三条的风险点.
