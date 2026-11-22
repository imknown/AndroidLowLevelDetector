# subtask-06 2c 编排收回

> **状态**: living. 事实核对时间 2026-11-17, 代码在 `e728acf1`.  
> 任务级计划见 [plan.md](plan.md); 流程见 [13-任务工作流](../../agents-cn/13-任务工作流.md).

## 目标

把"首页有哪些条目, 什么顺序"从 ViewModel 收回, 把 lld 的取数与回退一起收进去;  
排序开关按需求做成"只重排那一行, 实时, 不触发检测".  
ViewModel 侧只剩两件事: 取一次数据, 以及在开关变化时重排那一行.

不改的部分先说清: **检测仍是一次 `suspend collectModels()` 调用, 与 develop 同形**,  
不做成流, 也不改基类的契约. 检测的触发源只有两个动作, 见下面的表.

## concept primer

- `HomeViewModel` 现在做三件事: 读设置 (`HomeViewModel.kt:66`), 走 lld 取数链 (`:68-72` 分岔,  
  链在 `:109-212`), 在 `detect()` (`:214-309`) 里拼 23 条条目并逐条调用.
- 排序开关现在有两条补丁规则: Rule 1 在开关变化时就地补当前列表 (`:92-105`),  
  Rule 2 在列表刚回来时补一次 (`:318-329`), 两条都走 `payloadOutdatedTargetSdkVersionApk()` (`:331-349`).
- 补丁路径有两个毛病: 它每次重读并解析整份离线 `lld.json` (`:333`), 与这一轮实际用的在线数据可能不是同一份;  
  遇到 `lld == null` 时直接返回, 而加载路径会渲染成未知行.
- 但补丁路径的方向是对的: 需求 FR-17 (`02-功能需求.md:377`) 与 03 的 Q10 都写着, 这个开关"只重排那一行,  
  别的什么都不动". 要改的是补丁的取材方式 (复用本轮数据, 不重读盘), 不是取消"只改一行".
- `BaseListViewModel` 现在就是 develop 的形状: 一次 suspend 调用 (`:34`), `loadJob` 去重 (`:44`, `:60-71`),  
  `updateModelDetail()` 按类型改一行 (`:79-95`). 这与 FR-5 的 AC2 和 FR-13 的去重规则一致, 不动.
- 所以本步的收益不在"换一种控制流", 而在: 清单搬出去, lld 链搬进数据源层, 补丁路径换成"用缓存数据重渲染".

## 触发源 (硬约束)

跑一次检测的入口只有两个, 任何设置都不在其中:

| 触发 | 来源 | 效果 |
|---|---|---|
| 首次进入界面 | `BaseListViewModel.init()` | 跑一次检测; 已有缓存时直接显示; 已在跑时不并发启动第二次 |
| 下拉刷新 | 用户手势 | 跑一次检测, 已在跑时忽略这一次 (`loadJob` 去重) |

设置开关在切换那一刻的行为:

| 开关 | 切换时 | 对下一次检测 |
|---|---|---|
| 优先按包名排序 | 只把界面上那一行按新值重排, 不发起检测, 不拉数据, 不碰网络, 不写盘 | 渲染那一行时读它的最新值 |
| 允许联网 | 什么都不做 | 开始时读一次, 决定在线还是离线 |

三条注:

- 去重是两个入口共用的基类规则 (`loadJob`), 表里分两行写只是为了让每个入口自己读得懂.  
  重建后界面拿到的是同一轮的结果, 在跑的那一轮不重启也不重跑.
- "渲染那一行时读它的最新值" 在调用那一刻读 `SettingsStore` 的当前值, 有两个调用点:  
  检测结束时的渲染, 与开关变化时的重渲染. 两处都读当前值, 不用这一轮开始时的快照.
- 那一条检测失败时 (没有领域对象可重排), 保持旧行不动, 不改写成未知行.

## 现状与证据

| 项 | 位置 | 现在是什么样 |
|---|---|---|
| 编排 | HomeViewModel.kt:60-73, :214-309 | 读设置, 走 lld 链, 拼清单, 三者都在 ViewModel |
| lld 取数链 | HomeViewModel.kt:109-212 | 三级回退, 全在 ViewModel 里 try |
| 补丁路径 | HomeViewModel.kt:331-349 | 重读离线文件, 重跑那一条, 按类型写回一行 |
| 补丁的两个消费者 | HomeViewModel.kt:92-105, :318-329 | Rule 1 与 Rule 2 |
| 基类契约 | BaseListViewModel.kt:34 | 一次 suspend 调用, 保持不动 |
| 加载去重 | BaseListViewModel.kt:44, :60-71 | `loadJob` 在跑就忽略新请求, 保持不动 |
| 改一行的通道 | BaseListViewModel.kt:79-95 | 按 `MyModelType` 找下标替换, 可复用 |
| 列表写入 | BaseListViewModel.kt:98-101 | `setModels` 是 private, 子类写不进去 |
| 排序开关的需求 | 02-功能需求.md:377, 03:106 | 只重排那一行, 别的什么都不动 |

## 计划改动

1. lld 取数下沉 (行为不变).

   - 新增 `ui/home/datasource/LldLoader.kt`: 在线, 私有目录副本, 随包 asset 三级回退; 不抛异常,  
     失败作为 `LldFailure` 原因带回 (拉取, 解析, 写入三值加底层消息); 文案由 `HomeModelMapper` 拼.
   - `LldStatusState.errors` 改成 `failures: List<LldFailure>`; `HomeRepository.detectMode` 换签名.
   - `HomeRepository` 删 `fetchOnlineLldJsonStringOrThrow` 与 `saveLldJsonFileOrThrow` 两个直通,  
     以及 `lldDataSource` 构造参数; 补丁路径用的 `copyJsonIfNeededOrThrow` / `fetchOfflineLldFileOrThrow` 留着,  
     `getAssetLld` 留给三份仪器测试与旧离线链当夹具.
   - ViewModel 侧只删 `tryDetectOnline` 与 `tryDetectOffline`; `fetchOfflineLldOrNull` / `getAssetLldOrNull` /  
     `errorMessage()` 与 `LldAndError` 保留, 它们只服务补丁路径的取数; `LldAndError.message`  
     在第一批之后已经没有人读 (补丁只用 `.lld`), 第三批整包删, 不留半截.  
     这就是"第一批只换加载那一侧"的意思: 补丁路径原样留着, 两套取数并存到第三批.

1. 清单搬出 (行为不变).

   - 新增 `ui/home/detect/HomeDetectorCatalog.kt`: 23 条条目, 共享输入 (`LldLoader` 的 lld, `getMounts()`),  
     请求类型 `HomeDetectionRequest(allowNetwork)`.
   - `HomeViewModel.detect()` 变成一行调用, `collectModels()` 这个 suspend 形状保留.
   - 第二批的 catalog 还注入 `SettingsStore`, 检测开始时读一次排序值传给仓库: 与改前同一时机
     (改前也是检测开始时读), 所以这一批行为不变. 第三批把这处取值删掉, 改成渲染时读当前值.
   - Others 与 Prop 的清单留在各自 `collectModels()` 里, 不动.

1. 排序开关改成"实时重排一行" (本步唯一的行为改动).

   - `OutdatedTargetSdkVersionApkState` 去掉 `Order` 字段, apk 列表不再由仓库排序.
   - `HomeRepository.getOutdatedTargetSdkVersionApkState(lld)` 不再收 order, 只做过滤与 match 判定.
   - `HomeModelMapper.toOutdatedTargetSdkVersionApkModel(result, orderByPackageNameFirst)` 加参数,  
     排序与文案格式都在这里选.
   - `HomeDetectorCatalog.detect()` 返回"行 + 那一行的领域对象", 供两处共用: 检测结束时渲染,  
     以及开关变化时重渲染.
   - `HomeViewModel` 初始化时收 `settingsStore.outdatedOrderFirst`: 变化时界面没有数据就什么都不做;  
     有数据就用缓存的领域对象按**最新值**重渲染那一行, 经基类的写入入口写回整份列表.  
     取值只在这两处发生 (检测结束渲染时, 开关变化重渲染时), 都读调用那一刻的当前值;  
     mapper 保持纯函数, 只接收 `orderByPackageNameFirst` 参数, 自己不读设置.
   - 重渲染那一行复用基类已有的 `updateModelDetail()`: 它按 `MyModelType` 找行, 列表为空时不动,  
     那一行是失败行 (type 不是它) 时也不动, 正好是需要的语义. 所以基类一行不改, 只删 `onModelsLoaded()`  
     (它只服务 Rule 2).
   - 删除 Rule 1, Rule 2, `payloadOutdatedTargetSdkVersionApk()`.
   - `MyModelType.OutdatedTargetSdkApk` 保留: 它既是重渲染的寻址键, 也作 `contentType` 用.

1. 文档.

   - `04-架构` 的检测执行模型与 Lld.json 数据流两节; `06-检测条目` 的清单位置 (Home 在 catalog);  
     `05-代码规则` 的 lld 括注; `issues-cn` 里 #11, #03, #15 的现状句.
   - `03-概览` 与 `08-文档排版` 不动: `loadJob` 的说法仍然对, 示例标识符仍是 `collectModels()`.
   - 需求正文的生效时机补句归负责人 (见最后).

## 分批

一批一笔提交, 每批单独拿出来都能编译并通过验证:

- 第一批: lld 下沉, 加原因类型, 文案挪进 mapper. 只换"加载整页"那一侧的取数, 补丁路径原样,  
  所以行为不变; 两套取数并存 (loader 一套, 旧的离线一套) 持续到第三批.
- 第二批: 清单搬出到 `HomeDetectorCatalog`, 行为不变.
- 第三批: 排序开关实时重排那一行, 删两条补丁规则与补丁入口, 删基类的 `onModelsLoaded` 钩子;  
  连带删第一批留着的 `fetchOfflineLldOrNull` / `getAssetLldOrNull` / `errorMessage()` 与仓库两个直通.
- 第四批: 文档与记录 (`04`, `05`, `06`, `issues-cn`, `plan`, 本报告, 进度记录),  
  外加工作树里已经改好的三份需求正文 (`02-功能需求.md` 的第 8 条验收标准, `03-非功能需求与路线图.md` 的  
  性能补充与第 9, 10 条, `requirements-cn/README.md` 的编号范围).
- 第二批依赖第一批的 `LldLoad` 形状; 第三批依赖第二批的 catalog 返回值. 三类依赖都是硬的, 不换序.

## 有意不改

- 基类契约与 `loadJob` 去重: 与 develop 和 FR-5 / FR-13 一致.
- 三态到两个 `StateFlow` 的拆分 (`1afeae87`): 与本次目标无关.
- 检测串行与引擎的 `Detector` 契约: 归 subtask-05 的结论, 不重开.
- `MyModelListScreen`: 一行不动.
- Others 与 Prop 的条目内容与形状.

## 覆盖核对表

| 计划里的范围 | 问题条目 | 本子任务覆盖 | 留给后续 |
|---|---|---|---|
| 编排收回 | [#11](../../dev/issues-cn/03-UDF-单向数据流/11-HomeViewModel手工编排.md) 后半 | 是 | 无 |
| lld 回退链下沉, UI 不 try | #11 的迁移清单, [05-代码规则](../../agents-cn/05-代码规则.md) | 是 | 无 |
| 排序开关只重排一行 | FR-17 (`02-功能需求.md:377`), Q10 (`03:106`) | 是 | 需求正文的生效时机补句归负责人 |
| 刷新去重 | FR-5 AC2, FR-13 | 保持现状, 不改 | 无 |
| 检测清单成为数据 | [#03](../../dev/issues-cn/01-架构与分层/03-无三态模型与注册表.md) | 是, Home 的清单搬出 | Others 与 Prop 的注册表化, 归 subtask-07 |
| 取消感知 | [#15](../../dev/issues-cn/04-并发与调度/15-检测串行不感知取消.md) | 不做 | 界面没有入口, 归 #15 另议 |
| 缓存只放私有存储 | NFR-2 | 写盘路径未改 | 无 |
| 检测异步, 不阻塞主线程 | NFR-3 | 保持 | 无 |

## 问题清单的回答

负责人没有给问题清单.

## 验证

- `./gradlew assembleFossDebug`, `./gradlew testFossDebugUnitTest :base:testDebugUnitTest`,  
  `./gradlew lintFossDebug`, 加 Kotlin LSP 批发一发.
- 黄金数据: 本步不改展示内容与行序, `(key, detail, color, type)` 四列应与改动前逐行一致;  
  第三批之后在开关关闭状态下导一次对照.
- 手工:
  - 断网冷启动, 看 lld 行是否走离线, 失败文案是否仍在该条目自己的行里.
  - 联网启动, 看在线数据是否写盘.
  - 加载中途切 Tab 再回来, 确认不会启动第二次检测.
  - 切排序开关: 界面已有数据时立刻重排, 且不发网络请求、不写盘.
  - 切联网开关: 界面不动, 下次下拉刷新才换数据来源.

## 风险

- 高. 动 lld 取数链与补丁路径, 两者都在主链上.
- 第三批是唯一改行为的一批: 若重渲染时拿不到那一行的领域对象 (例如那一条检测失败),  
  退回"保持旧行不动", 不能因为开关变化把失败行洗成未知行.
- 删 `updateModelDetail` 之前要再 grep 一次, 不能凭记忆.
- 需求正文原来缺"切换联网开关不触发刷新"的明文, 已在 2026-11-18 补成第 8 条的第四条验收标准;  
  排序开关那三句需求第 17 条本来就有. 这张触发源表仍然留着, 两处互相校对.

## 结果

- 四批全部落地. 代码: 新增 `LldLoader` / `LldFailure` / `HomeDetectorCatalog`, `HomeViewModel` 从 349 行降到 80 行,
  删掉 Rule 1 / Rule 2 / 补丁入口 / ViewModel 侧的四个 lld 函数 / `errorMessage` / `LldAndError` / 两个计数器 /
  基类的 `onModelsLoaded` 钩子.
- 验证: 四档每批各跑一遍 (第一批 13 分 32 秒, 第二批 6 分 28 秒, 第三批 6 分 30 秒), 全绿;
  单测从 110 例到 112 例, 零失败; `lintFossDebug` 每批 `no new issues`; Kotlin LSP 每批改动文件全 `CLEAN`.
- review: v3 审第一批, v4 审第二与第三批, 各两个 reviewer. 一档共三条 (基类死钩子, catalog 两处 KDoc 写反,
  一处注释漏写用途) 全部修掉; 二档五条记归后续 (两套取数链与重复取数已随第三批消失, loader 无单测,
  重渲染路径无单测, 清单顺序无机器断言).
- 与计划的两处偏离, 都在实施时记录并解释了理由:
  - 第一批按负责人选的乙: 只换"加载整页"的取数, 补丁路径原样留到第三批, 所以第一批之后有两套取数并存.
  - 第三批不放开 `setModels`, 改用 `updateModelDetail` 做重渲染: 它按类型找行, 列表为空与失败行都自然不动.

## 需要老板定

- 已定 (2026-11-17): 基类保持 suspend 形状; 排序进 mapper; 改一行的逻辑放 Home; 联网开关下次生效;  
  开关切换不触发检测; NFR 三条都写; `12-git` 那条留; 重做后重发 v1; 调查只抄进度记录;  
  黄金快照重做后导.
- 已定 (2026-11-18): `plan.md` 六处过时与矛盾全改; 设计稿放行; `subtask-07` 报告等本步第三批之后再写;
  需求正文由 AI 补; `issues-cn` 路线段改掉; 四批照序; 提交层等代码完成; 规则修订等本步一起提交;
  第一批补丁路径选乙; 第二与第三批合并一轮 review.
- 待定: 分批是否照上面四批; 文档改动范围是否就是那五份加 `issues-cn`;  
  是否把"渲染分层"另开子任务 (本报告按不开处理, 只在 06 里写明"带展示参数的条目").
