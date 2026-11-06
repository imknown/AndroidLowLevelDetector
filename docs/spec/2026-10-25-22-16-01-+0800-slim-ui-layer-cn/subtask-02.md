# subtask-02 基线快照与测试栈

> **状态**: living. 事实核对时间 2026-10-27, 代码在 `502a6eb7`;  
> 2026-11-03 开工前复核一次 (`1d33df3c`), 改痕见下面这条.
>
> 2026-11-03 复核改动: AGP 已从 RC 线升到 `9.5.0` 稳定版 (`android.toml:7`), 报告里两处 "RC 线"  
> 的措辞跟着改; 其余事实 (test bundle 内容, `libsThirdParty` 那行注释, 现有测试清单, 行号) 复核后不变.
>
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
- 测试依赖现在由约定插件统一挂: `build-logic/convention/src/main/kotlin/.../KotlinAndroid.kt:73-93`  
  里的 `configureTest()` 读 `libsAndroid.findBundle("test")`, 而这个 bundle 只有一个 JUnit 4  
  (`gradle/toml/android.toml:115`, 版本在同一份文件的 Test 段).
- 仓里现有测试: `base` 的 `DateTimeExtTest` 有四个真方法, 其余四个类是模板 (`ExampleUnitTest` 两个,  
  `ExampleInstrumentedTest` 两个). 前四个方法写的是 JUnit 4 断言 (`org.junit.Assert`, `org.junit.Test`).
- `Build.*`、shell 探针、`AssetManager` 都在仓库方法的执行路径上, 所以纯 JVM 测试跑不了仓库方法.  
  黄金数据只能从真机或模拟器上取.

## 现状与证据

| 项 | 位置 | 现在是什么样 |
|---|---|---|
| 测试依赖接线 | KotlinAndroid.kt:73-93 | 两个 bundle: `test` 与 `androidTest`, 都从 `libsAndroid` 取 |
| test bundle 内容 | gradle/toml/android.toml:115 | 核对当时只有 `junit` (JUnit 4); 现状是 Jupiter 与 platform launcher, 不含 JUnit 4 |
| 现有真测试 | base/src/test/.../DateTimeExtTest.kt | 四个方法, JUnit 4 写法 |
| 模板测试 | app 与 base 各两个类 | 无实际内容 |
| 检测条目数 | Home 23 个调用产 25 行; Others 37 次追加; Prop 5 个来源 | 每次加载动态行数还会变 (`getPartitionFingerprints`) |

## 计划改动

1. 依赖与接线.

   - 已定 (负责人 2026-10-28): JUnit Jupiter 放 `gradle/toml/android.toml` 的 Test 段, 理由是 AGP 的测试底座本来就依赖  
     JUnit 4, 两份放一起好对照; MockK 放 `gradle/toml/thirdParty.toml`.
   - 落点有三处, 缺一处就接不上:
     1. `gradle/toml/android.toml:115` 的 `test` bundle 加 JUnit Jupiter 的条目 (版本在同一份文件的 Test 段).  
        2026-11-04 按 review v1 的发现改了这件的收尾: bundle 里原来那一条 JUnit 4 去掉,  
        理由是它没有对应引擎, 谁写了 JUnit 4 单测都能编译而一个方法不跑; 仪器测试那一路的 JUnit 4  
        由 `androidx.test.ext:junit` 与 `androidx.test:runner` 传递带入, 不受影响.
     1. `gradle/toml/thirdParty.toml` 加 MockK 的版本与库, 并解开  
        `build-logic/convention/src/main/kotlin/.../ext/ProjectExtensions.kt:25` 那行被注释掉的  
        `libsThirdParty` 访问器; 不解开, 约定插件拿不到这份目录.
     1. `build-logic/convention/src/main/kotlin/.../android/KotlinAndroid.kt:86` 的  
        `testImplementation(libsAndroid.findBundle("test").get())` 是 JUnit Jupiter 进 classpath 的唯一通路;  
        MockK 另挂一条 `testImplementation(libsThirdParty.findLibrary("mockk").get())`.  
     写成 `libsThirdParty.mockk` 编译不过: 约定插件里的 `libsThirdParty` 是 `ProjectExtensions.kt:21-25`  
     那个 `VersionCatalog` 委托, 它没有类型安全访问器, 同文件既有写法都是 `findBundle(...)` /  
     `findLibrary(...)` (KotlinAndroid.kt:86, :103); `libsThirdParty.mockk` 那种写法只在模块的  
     `build.gradle.kts` 里才生成.
   - 需要的条目: `junit-jupiter` (aggregate), `junit-platform-launcher` (Gradle 跑 Jupiter 需要它), `mockk`.  
     Turbine 随第一个 Flow 测试再进, 本子任务不引.
   - 版本 (2026-11-03 定, 2026-11-04 跟进): `junit-jupiter` 与 `junit-platform-launcher` 都取 `6.1.3`,  
     JUnit 6 起这两条线同版本; 工具链 Java 25 与 Gradle 9.8.2 都满足 JUnit 6 的要求.  
     MockK 取 `1.14.11` (review v1 指出 `1.14.3` 落后, 按追最新稳定的习惯升上去).
   - `useJUnitPlatform()` 是**新加**的接线, `configureTest()` 现在只设 `testInstrumentationRunner` 与挂两个 bundle  
     (KotlinAndroid.kt:73-93), 里面没有这块. 写法 `testOptions.unitTests.all { it.useJUnitPlatform() }`,  
     加在 `configureTest()` 或 `configureAndroid()` 里.
   - 这一层被三个模块吃到: `app`, `base`, 还有 `binderDetector` (它也 apply 了 library 约定插件).  
     binderDetector 没有测试源码, 只是多挂一遍.
   - `androidTest` 那一路继续用 JUnit 4: 设备上的 `AndroidJUnitRunner` 与 `androidx.test.ext:junit`  
     都是 JUnit 4 系, 换掉要连 runner 一起换, 不在本步.
   - 2026-11-03 实跑发现一处漏项: `androidx.test:runner` 根本不在仪器测试 classpath 上,  
     `androidTest` bundle 只有 `core` 与 `ext:junit`, 后两者不带 `AndroidJUnitRunner`. 表现是  
     `am instrument` 立刻崩在 `ClassNotFoundException`. 这套仪器测试此前没在真机上跑过, 所以一直没暴露.  
     处置: bundle 补 `test-runner = 1.7.0`.

1. 存量测试怎么处理.

   - A. 只跑 Jupiter, 把 `DateTimeExtTest` 的四个方法改成 Jupiter 写法 (`org.junit.jupiter.api.Test`  
     与 `Assertions`), 删掉 `app` 与 `base` 那两个 `ExampleUnitTest` 模板类. **推荐**.
   - B. 加 `junit-vintage-engine`, JUnit 4 测试原样继续跑.
   - 选 A: 存量只有四个方法, 改动是 import 与断言迁移; 留着 vintage engine 会让两套断言长期并存,  
     以后写测试的人不知道该跟哪一套.
   - 两个 `ExampleInstrumentedTest` 保留. `androidTest` 那一路仍是 JUnit 4; 要让仪器测试也上 JUnit Jupiter,  
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
     用 `adb pull` 取回到本地临时目录, 不进 CI, 也不进仓库.
   - 取数方式 (2026-11-03 定): 从 `AppGraph` 的 `metroViewModelFactory` 造三个 ViewModel, 直接调  
     `collectModels()`. 这样快照就是产品代码当下的产物, 不走一份会走样的复刻序列.
   - 取数前把 `allowNetworkData` 与 `outdatedOrderFirst` 两项设置固定为 false, 取完还原:  
     前者决定 Home 走联网还是离线 LLD, 后者改一行详情的文案, 不固定就带进不确定性.
   - 数据形状: 每行 `{ page, index, key, detail, color, type }`. `page` 用来区分三页, `index` 保留展示顺序.
   - `type` 必须在里面: 界面读它 (`MyModelListScreen.kt:132` `contentType = { it.type }`),  
     它也是 subtask-01 第一件改成按类型寻址的依据. 快照漏了它, subtask-04 迁移时把某行的 `type` 改掉,  
     逐行对照根本发现不了.
   - 生成时机: 在 subtask-01 之后跑一次. subtask-01 改变了挂载失败的处理, 快照要拍在它之后,  
     否则 2a 的对照会带上一件与它无关的差异.

1. 规则文件的测试段.

   - 要改的是 [04-架构](../../agents-cn/04-架构.md) 的 "接口优先的可测试性" 那一节 (04-架构.md:54-67).  
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
| JUnit Jupiter 与 MockK 引入 | [#06](../../dev/issues-cn/01-架构与分层/06-技术栈缺口.md), [#50](../../dev/issues-cn/09-构建与CI/50-CI只编译不测试.md) | 是 (测试栈部分) | CI 扩跑归 subtask-03 |
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
  - 2026-11-03 实跑 (Nexus 5X, LineageOS 21 / API 34): 1345 条记录, 其中 home 25, others 42, prop 1278.  
    `type` 一列确有区分, `index` 24 那行是 `OutdatedTargetSdkApk`, 其余是 `General`.
  - 连跑两次逐行对比: 行数一致, 只有 1 行不同, 是 prop 页的  
    `debug.tracing.battery_stats.wifi_signal_strength` (3 变 2), 系统自己刷新的实时计数器.  
    结论: 结构稳定, 但快照对照要留一个 "实时计数器类 prop 可能变" 的例外, subtask-04 复跑时别把这行当成回归.
  - 界面行数的人工核对 (2026-11-03 由负责人完成): 首页 25 行, Others 42 行, 与 json 一致.
  - 快照的时效边界 (2026-11-04 实测): 隔一天重跑, 行数变成 1342, 差异 829 处. 按键拆开看:  
    `debug.tracing.*` 三行消失 (系统调试属性会随状态出现与消失), 同 key 的 196 行内容变了  
    (`cache_key.*` 每次进程随机, `boot_count` 与亮度这类计数器在走).  
    短窗口内是稳的 (相隔几分钟两次只差 1 行), 跨时段不稳.  
    结论: 这份快照是某一时刻的设备快照, 不是可复用的恒定基线. subtask-04 对照前要在同一设备状态上  
    重新生成一次, 或者按类列全例外 (名单见 subtask-04.md 的验证节, 只列两类覆盖不住实测的 196 行).
  - 脱敏 (2026-11-04 起, review v4 / v6 / v7 三轮发现): prop 页里有能定位到具体机器的值  
    (设备序列号, 系统 android id, ROM 构建机名与构建账号, 蓝牙网卡地址, adb 调试端口, 设备名). 生成器分两遍:  
    先从按名字命中的行取出这些值, 再把含这些值的行处理掉. 按名字不能只认这一台设备的属性名,  
    所以 key 片段按类别写 (serialno, serialnumber, android_id, macaddr, wifi.guid, build.host,  
    build.user, hostname, device_name, adb.tls.port); 只按 key 也不够, 构建账号就藏在版本串里,  
    Others 页的 "构建主机" 那行用资源 ID 当 key.  
    key 命中的行整行换成 `<redacted>`; 只是正文里含值的, 按词边界做行内替换, 版本号这类产品事实留着,  
    4 字符的短值也不会切碎别的词. 占位文案取应用自己的本地化串 (`build_not_filled`), 不按字面硬编码.  
    处理完有自检: 收集到的值若还按同一套词边界规则留在任何一行, 或全表还有 MAC 形态的正文 (不豁免任何行),  
    生成器直接抛错. 自检的边界: 它只认收集到的值加 MAC 形态; 一个属性名不在片段表里、形态又不像 MAC 的  
    定位值, 它看不见, 所以片段表要跟着设备保持更新.
  - 基线不进仓库 (2026-11-06 定): 对照要的是 "改造前在同一次会话里取一份全量, 改造后再取一份", 那两份都在  
    设备侧, 仓库里存不存跟对照无关. 之前提过全量 (1343 条, 11k 行) 与抽样 (102 行) 两个方案, 都不划算:  
    全量太大且随设备漂移, 抽样又只剩 "给人看形状" 这点用处, 那点用处几行样例就够.  
    所以 `baseline/` 不提交, 生成器本身留着 (设备侧工具).
  - "改造前"那份从哪来: subtask-04 开工时先按当前代码在设备上跑一次生成器, 把 json 拉到本地临时目录,  
    再动代码; 改完再跑一次, 两份逐行比. 生成器在改动中不用动 (见下方风险节).
  - 行的形状 (供写 mapper 时对照, 取自实跑):
    ```
    home   idx0  key 2131558596  detail "离线（2026-10-02 10:05 +0800）"  color CRITICAL  type General
    prop   idx1157 key ro.serialno  detail "<redacted>"                    color NONE      type General
    ```
    `detail` 多行的情况 (例如首页的 Android 版本对比) 用 `\n\n` 分段; `key` 在 prop 页是属性名,  
    在另两页是资源 id 字符串.
  - 生成器仍然脱敏, 尽管文件不再进仓库: 拉回来的 json 会先落在本地临时目录, 可能被贴进文档或被误提交,  
    留着脱敏比事后判断便宜. 设备名 (设备型号) 那一行照常整行脱敏, 但它的值不当定位值用:  
    它在几十行无关的型号行里都会出现, 展开会误伤.

## 风险

- JUnit Jupiter (6.1.3) 与当前 AGP 版本 (9.5.0 稳定版, 见 `gradle/toml/android.toml` 的 `androidGradlePlugin` 键)  
  的兼容性没有先例可查. 2026-10-28 跑过一次四个任务, 现有 JUnit 4 的路是通的, 但那一次没有引入 Jupiter,  
  所以这一条仍然未知. 首跑出问题时的退路是加 vintage engine, 只换跑法, 不换断言库.
- 快照生成器原本在 subtask-04 之后要改一次. 2026-11-03 实现时改成直接调用三个 ViewModel 的  
  `collectModels()`, 不复刻仓库调用序列, 这项返工随之取消: 只要三页还由 `collectModels()` 产出,  
  生成器就不用动. 到 subtask-06 若这个入口本身被拆掉, 生成器才需要跟着改.
- 动态行数 (分区指纹, Prop 页) 让快照带不确定性. 报告里要写明: 同一台设备上连续跑两次,  
  行数与内容应当一致; 不一致就先查清楚再往下走.

## 需要老板定

- 已定三项 (2026-10-28): JUnit Jupiter 放 `android.toml`, MockK 放 `thirdParty.toml` 并解开 `libsThirdParty`  
  访问器; 存量四个方法迁 Jupiter, 仪器测试保持 JUnit 4; 黄金数据用仪器测试生成.
- 剩下一个没查清: JUnit Jupiter (6.1.3) 与当前 AGP 版本 (9.5.0 稳定版) 的兼容性. 要跑一次构建才知道,  
  这也是本任务里唯一可能推翻上面第三条的风险点.
