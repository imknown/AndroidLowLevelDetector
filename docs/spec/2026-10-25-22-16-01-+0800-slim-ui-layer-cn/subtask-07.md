# subtask-07 Others 与 Prop 的注册表化

> **状态**: living. 事实核对时间 2026-11-19, 代码在 `bc6cfaa6`.  
> 任务级计划见 [plan.md](plan.md); 流程见 [13-工作流](../../agents-cn/13-工作流.md).

## 目标

Others 的 37 条与 Prop 的 5 条检测条目清单, 从各自 ViewModel 的 `collectModels()` 搬进各自的清单类,  
形状与 `HomeDetectorCatalog` 一致. 搬完以后 ViewModel 只剩一行调用, 增删条目只改清单一处.  
条目内容, 顺序, 行文案, 行数全不变; 这一步不动任何检测行为.

## concept primer

- 清单现在已经在 ViewModel 里变成数据了, 只是放在 ViewModel 的方法体里:  
  `OthersViewModel.collectModels()` (`OthersViewModel.kt:33-156`) 与 `PropViewModel.collectModels()`  
  (`PropViewModel.kt:31-50`) 各自造一个 `listOf(...)` 的 `Detector`, 交给 `detectAll()`.
- 这件事是 subtask-05 做的 (0g: Others 与 Prop 仿 Home 一并入表). 所以本步不改条目怎么写,  
  只把那份清单从 ViewModel 挪到单独一个类.
- Home 已经走完这条路: 清单在 `HomeDetectorCatalog` (`HomeDetectorCatalog.kt:46-52` 是类与注入,  
  `:54-157` 是 `detect()`), ViewModel 里只剩 `detectorCatalog.detect(...)` 一行 (`HomeViewModel.kt:60-78`).
- 清单类进依赖图靠 `@Inject` 构造器注入. ViewModel 的构造参数换成清单类, `metroViewModel` 解析照旧,  
  入口 `AppRoot.kt:111-112` 不动.
- 清单类的输入: Home 有一个网络开关 (所以有 `HomeDetectionRequest`), Others 与 Prop 没有任何输入.  
  05-项目架构:135 写的就是 "Others 与 Prop: 没有共享输入, 每条自己取".
- 清单的顺序就是展示顺序, 而 `OthersEntryId` / `PropEntryId` 的声明序已经锁在  
  `DetectionEntryOrderTest` 里. 搬迁时照 ViewModel 现有顺序抄, 不重排.

## 现状与证据

| 项 | 位置 | 现在是什么样 |
|---|---|---|
| Others 清单 | OthersViewModel.kt:36-153 | 37 条 `rawDetector(OthersEntryId...)`, 带 `// region` 分组注释 |
| Others 的平台门 | OthersViewModel.kt:45, :58, :71, :84, :87, :129 | 6 处 `precondition`; 其中 4 处在 `run` 里又写一遍同样的 `SDK_INT` 判断给 lint 看 (`:52`, `:65`, `:78`, `:94`) |
| Others 执行 | OthersViewModel.kt:33, :155 | 外层 `withContext(Dispatchers.IO)`, 末尾 `detectors.detectAll()` |
| Prop 清单 | PropViewModel.kt:35-47 | 5 条 `rawDetector(PropEntryId...)`, 没有 `precondition` |
| Prop 执行 | PropViewModel.kt:31, :49 | 同样在 `withContext(Dispatchers.IO)` 里调 `detectAll()` |
| 枚举与扩展 | OthersEntryId.kt:13-51, :59-63; PropEntryId.kt:13-19, :29-32 | 声明序即展示序; `rawDetector` 重载在 `model` 包 |
| Home 的现成形状 | HomeDetectorCatalog.kt:46-52, :54-157 | `@Inject` 构造, 一个 `suspend fun detect()` |
| Home 的 ViewModel | HomeViewModel.kt:34-37, :60-78 | 构造参数是 `detectorCatalog`, `collectModels()` 一行取数 |
| 基类契约 | BaseListViewModel.kt:34, :52-62 | `startLoad()` 在 `viewModelScope` 里调 `collectModels()`, 所以 IO 切换要留在清单类里 |
| 引擎 | Detector.kt:64-85 | `List<Detector>.detectAll()`, 按下标归位, 串行 |
| 界面入口 | AppRoot.kt:111-112 | `metroViewModel<OthersViewModel>()` / `<PropViewModel>()`, 不碰构造 |
| 快照生成器 | ModelSnapshotTest.kt:90-100 | 经工厂造三个 ViewModel 直接调 `collectModels()`, 只依赖构造签名 |
| 顺序锁 | DetectionEntryOrderTest.kt:18-107 | 锁三页枚举的声明序, 不锁清单 |
| 清单位置的现状描述 | 05-项目架构.md:125, 08-检测条目.md:27-29, :48 | 两份都写明 Others 与 Prop 的清单还在 `collectModels()` 里; 08 的图节点 S3 另有同样一句 |
| #03 的现状句 | issues-cn/01-架构与分层/03-无三态模型与注册表.md:5-7 | 写着 "另两页归 subtask-07 (搬进各自的清单类)" |
| 路线表状态 | issues-cn/README.md:178, :321 | :178 是状态列 "部分 (Home 已入表, 另两页归 subtask-07)"; :321 是修复路线引言里 "`collectModels()` 的调用顺序就是列表顺序" |
| Prop 页性能条目的位置描述 | issues-cn/06-性能/30-Prop页逐key查询.md:8-9, :25, :49, :100, :107 | 五处把 Prop 的检测位置写在 `PropViewModel.collectModels()`, 影响文件表在 `:8-9` |
| 检测串行条目 | issues-cn/04-并发与调度/15-检测串行不感知取消.md:15-16, :46-47 | 点名三个 ViewModel 的 `collectModels()`; 正文是修前叙事, 现状由顶部现状头 (`:5-7`) 承载 |

## 计划改动

1. Others 搬出.

   - 新增 `ui/others/detect/OthersDetectorCatalog.kt`: `@Inject` 构造注入 `OthersRepository`,  
     一个 `suspend fun detect(): List<MyModel>`, 体内是 `withContext(Dispatchers.IO)` 加  
     那 37 条 `Detector` 加 `detectAll()`. `// region` 分组注释与条目顺序逐字照抄.
   - 6 处 `precondition` 与 4 处 `run` 里的重复门原样搬, 一个字不改.  
     这两类门各自带的注释一起搬, `run` 里那 4 段在 `OthersViewModel.kt:48-51`, `:61-64`, `:74-77`, `:90-93`.
   - `OthersViewModel` 的构造参数换成 `private val detectorCatalog: OthersDetectorCatalog`,  
     `collectModels()` 变成一行 `detectorCatalog.detect()`, 删掉不再用的 import  
     (`Build`, `Dispatchers`, `withContext`, `MyApplication`, `ArchitectureDataSource`,  
     `detectAll`, `OthersEntryId`, `OthersRepository`, `rawDetector`).
   - `@Inject` / `@ViewModelKey` / `@ContributesIntoMap` 三项注解与顶部 Metro 注释不动.

1. Prop 搬出.

   - 新增 `ui/prop/detect/PropDetectorCatalog.kt`, 形状同上, 注入 `PropRepository`, 5 条条目.  
     `Settings.System` 这类实参留在清单里.
   - `PropViewModel` 同样只留一行, 删掉对应的 import (`Settings`, `Dispatchers`, `withContext`,  
     `detectAll`, `PropEntryId`, `PropRepository`, `rawDetector`).

1. 文档.

   - `05-项目架构` 的检测执行模型一节 (`:125`): 三页清单的位置改成三份清单类并列, 删掉 "(subtask-07 搬出)" 的括注.
   - `08-检测条目` 两处: 第 3 步正文 (`:27-30`) 与第 7 步之后那张图的 S3 节点 (`:49`).
   - `issues-cn` 的 #03 现状头 (`:5-7`) 与 README 两处: `:178` 的状态列, 加上 `:321` 修复路线引言里  
     "`collectModels()` 的调用顺序就是列表顺序" 那句.
   - `issues-cn/06-性能/30-Prop页逐key查询.md` 五处位置描述: `:8-9` 的影响文件表加新文件路径,  
     `:25`, `:49`, `:100`, `:107` 把 `PropViewModel.collectModels()` 换成清单类的 `detect()`.  
     #30 自身的修复不在本任务范围 (`plan.md` 的独立小件清单里有它), 这里只跟着搬位置.
   - `issues-cn/04-并发与调度/15-检测串行不感知取消.md` 不动. 它的正文是修前叙事, 现状由顶部现状头 (`:5-7`) 承载;  
     正文里混着两类过期, 点名 Home 的那半句与 "另两页直接 `tempModels +=`" 早于本单元就过期,  
     点名另两页 `collectModels()` 位置的那半句随本步过期. 两类都归 #15 自己的实施单元一起改.
   - 本报告补 "结果" 节, 进度记录跟着更新.

## 分批

一批一笔提交, 每批单独拿出来都能编译并通过验证:

- 第一批: Others 的 37 条搬进 `OthersDetectorCatalog`, ViewModel 只剩一行, 行为与行序不变.
- 第二批: Prop 的 5 条搬进 `PropDetectorCatalog`, ViewModel 只剩一行, 行为与行序不变.
- 第三批: 文档与记录 (`05-项目架构`, `08-检测条目` 两处, `issues-cn` 三份文件, 本报告, 进度记录).

两批之间没有依赖, 换序也能编. 分两批的理由是各自的 diff 与 review 材料小一号, 出问题只看一份清单.  
第三批依赖前两批的最终形状, 不换序.

## 有意不改

- `OthersEntryId`, `PropEntryId` 与它们的 `rawDetector` 重载: 位置和形状都不动.
- 每条条目的取数方法, 行文案, `precondition`, 失败隔离方式: 一个字不改,  
  `OthersRepository` 与 `PropRepository` 不动.
- `BaseListViewModel`: 不碰. `collectModels()` 的 suspend 形状保留.
- `HomeDetectorCatalog`, `Detector.kt`, `detectAll()`: 不碰.
- `ModelSnapshotTest`: 不改. 它靠工厂造对象, 只依赖构造签名, 搬迁后照跑.
- 清单顺序的机器断言: 现在没有, 本步也不加. Home 的 23 条同样没有, 这一条在 subtask-06 的  
  二档备查里记过, 归同一处另议.
- 有界并发, 以及三态类型在另两页的生产者: 归 #15 与 #03 的其余部分, 不在本步.

## 覆盖核对表

| 计划里的范围 | 问题条目 | 本子任务覆盖 | 留给后续 |
|---|---|---|---|
| Others 的 37 条搬进清单类 | [#03](../../dev/issues-cn/01-架构与分层/03-无三态模型与注册表.md) 的 Others 部分 | 是 | 无 |
| Prop 的 5 条搬进清单类 | #03 的 Prop 部分 | 是 | 无 |
| 条目内容, 顺序, 行文案不变 | 0g (Others / Prop 仿 Home 入表) | 是 | 无 |
| 清单顺序与目录行序一致 | `04-检测条目目录` 的 OTH-01..37 与 PROP-01..05 | 保持, 不重排 | 无 |
| 三态类型与有界并发 | #03 的书面目标, [#15](../../dev/issues-cn/04-并发与调度/15-检测串行不感知取消.md) | 不做 | 归 #15 与 #03 其余部分 |
| Prop 页性能条目的位置描述 | [#30](../../dev/issues-cn/06-性能/30-Prop页逐key查询.md) | 是, 只改位置描述 | #30 自身的修复另排期 |
| 检测串行条目正文 | #15 | 不做 | 归 #15 的有界并发单元 |

## 问题清单的回答

负责人没有给问题清单.

## 验证

- `./gradlew assembleFossDebug`, `./gradlew testFossDebugUnitTest :base:testDebugUnitTest`,  
  `./gradlew lintFossDebug`.
- analyze-file 与 Kotlin LSP 两档按 [10](../../agents-cn/10-代码分析规则.md) 跑, 每批点名自己那两个 `.kt` 文件,  
  两批一共四个: 批 1 是 `OthersDetectorCatalog.kt` 与 `OthersViewModel.kt`,  
  批 2 是 `PropDetectorCatalog.kt` 与 `PropViewModel.kt`.  
  analyze-file 要 Android Studio 实例在跑, 本机这一档历来报 "没有运行中的实例, CLI 挂起无输出",  
  这处按 13 的偏差记录留一行.
- 逐条对照, 每批各自做一遍: 新清单与搬之前的 ViewModel 清单逐行比, 核四样. 条目顺序, 语义 id,  
  调的仓库方法与实参, `precondition` 与 `run` 内重复门. Others 应是 37 条加 6 处 `precondition`,  
  Prop 应是 5 条零 `precondition`.
- 黄金数据: 本步不改展示内容, 三页的 `(key, detail, color, type)` 应与改动前逐行相同.  
  生成器在 `ModelSnapshotTest`. 先在同一台设备上跑一次改动前的快照, 每批改完再跑一次, 两份逐行比.
- 手工: Others 与 Prop 两页各翻一遍, 行数与同一台设备改动前一致. 行数随设备变,  
  所以只比同一台设备的前后两次, 不拿别的机型的数字当期望.

## 风险

- 中. 机械搬迁, 42 条, 没有行为改动. 风险集中在漏条, 顺序错, 实参抄错三处.
- 漏条与顺序错靠逐条对照与编译抓住, 实参抄错编译也能抓到大半 (类型不符).
- `withContext(Dispatchers.IO)` 忘在 ViewModel 或清单类都不留, 会让 shell 与属性读取跑在主线程.  
  照 Home 的形状写, 别自己重新安排.
- 清单类不加 `@SingleIn`. Home 的也没加, 每次注入新建实例, 与现状一致.

## 结果

- 三批全部完成 (2026-11-22). 代码: 新增 `ui/others/detect/OthersDetectorCatalog.kt` (37 条)  
  与 `ui/prop/detect/PropDetectorCatalog.kt` (5 条), 两个 ViewModel 各剩一行调用,  
  三份清单类的形状一致 (都是 `@Inject` 构造加一个 `suspend detect()`;  
  Home 的多收一个请求类型并返回结果类型); 三份清单的顺序与目录的 HOME-01..23, OTH-01..37 和 PROP-01..05  
  逐一对应 (四家 review 各自独立核对).
- 提交: 批 1 `15715974`, 批 2 `17d0ca91`, 批 3 (本笔, 文档与记录) 随本节入库.
- 验证: 两批代码各跑 Kotlin LSP (两文件 `CLEAN (0 items)`) 与 Gradle 四条 (全绿);  
  黄金快照改前改后逐行比, 两批的 home 与 others 段都零差异; prop 段的差异全是  
  设备状态行, 批 1 十二处 (闹钟时间一条, 亮度一条, `cache_key` 九条,  
  `sys.oem_unlock_allowed` 一条), 批 2 五处 (亮度两条, `cache_key` 三条);  
  真机 Others 页 42 行逐行全对, Prop 页滚动到底抽样 170 行全对.  
  analyze-file 两批都因本机没有运行中的实例记偏差.
- review: 两批代码各一轮 v1 full, 各四个 reviewer, 四家全 met, 第一档零,  
  review 循环均停, 未排 v2. 归后续动代码的单元: `OthersEntryId.kt:9` 与  
  `HomeEntryId.kt:11` 的过期括注 (排期时由负责人点名); `#15` 正文归 #15 的实施单元.  
  需求目录 `:23` 前半句与 `#30` 的影响文件表已在本批处理.
- 与计划的偏离: 偏离一处, 已获负责人同意: `#30` 的影响文件表把 `PropViewModel`  
  替换成清单类 (计划原文是 "加新文件路径"). 指针口径 (负责人定): 报告现状表里与本单元  
  相关的五行 (`:45-49`) 钉在 `bc6cfaa6`, 行内指针也是改前的; 其余指针随现行.  
  Prop 行的旧位置由本节的新位置陈述承载.

## 需要老板定

- 无.

## 已定

- 2026-11-19: 分批照三批走 (批 1 Others, 批 2 Prop, 批 3 文档与记录).
- 2026-11-19: `plan.md` 的 "66 条" 改成 "65 条", 授权我改, 已改.
- 2026-11-19: 设备已连, 黄金对照与两页手工核对照验证那节做.
- 2026-11-19: 清单类只一个 `detect()`, 不加空的请求类型与结果类型.
- 2026-11-19: `plan.md` 的独立性那句改成 "清单类形状 (请求类型那一半只服务 Home)".
- 2026-11-19: `plan.md` 的 subtask-07 一节补一句文档随本步改写.
