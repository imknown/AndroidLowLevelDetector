# subtask-06 2c 编排收回

> **状态**: living. 事实核对时间 2026-10-27, 代码在 `502a6eb7`.  
> 任务级计划见 [plan.md](plan.md); 流程见 [13-任务工作流](../../agents-cn/13-任务工作流.md).

## 目标

把"首页有哪些条目, 什么顺序"从 ViewModel 收回仓库与注册表, 把 lld 的取数与回退也一起收进去,  
ViewModel 侧只剩状态推导. 本任务是"UI layer 减重"这条线的最后一步.

## concept primer

- 现在 `HomeViewModel.collectModels()` 做三件事: 读设置 (HomeViewModel.kt:58), 走 lld 的取数链  
  (:60-64), 再逐行调用 23 个仓库方法拼列表 (:206-244).
- lld 的取数链在 ViewModel 里用 try/catch 串起来: `tryDetectOnline` (:101-148), `tryDetectOffline` (:150-154),  
  `fetchOfflineLldOrNull` (:156-188), `getAssetLldOrNull` (:192-203). 这是业务回退, 不是界面逻辑.  
  [05-代码规则](../../agents-cn/05-代码规则.md) 写明 UI 层不处理业务异常.
- 同一条链里最后一级回退需要 `AssetManager`, 现在靠 `MyApplication.instance.assets` (:194) 拿到.
- 排序开关有两条补丁规则: Rule 1 在开关变化时就地补当前列表 (:84-97), Rule 2 在列表刚回来时补一次  
  (:253-264). 两条都需要 `payloadOutdatedTargetSdkVersionApk()` (:267-286) 这个事后补丁入口.
- 这个补丁入口有两个不对称: 加载路径接受 `lld == null` (渲染成未知行), 补丁路径遇到 `lld == null` 直接返回;  
  加载可能用的在线数据, 补丁固定重读离线文件 (:268). 补丁里还做磁盘写 (`copyJsonIfNeededOrThrow`, :159).
- 如果列表变成"由设置与检测结果推导出来的值", 上述两条规则与整个补丁入口都不需要存在,  
  因为设置一变, 推导就会重算. 这是本步真正的收益.

## 现状与证据

| 项 | 位置 | 现在是什么样 |
|---|---|---|
| 编排 | HomeViewModel.kt:206-244 | 23 个仓库方法调用加一次取挂载, 写在函数体里 |
| lld 取数链 | HomeViewModel.kt:101-203 | 三级回退, 全在 ViewModel 里 try |
| 补丁路径 | HomeViewModel.kt:267-286 | 事后按类型改一行, 重读离线文件 |
| 补丁入口的消费者 | HomeViewModel.kt:93, :261 | Rule 1 与 Rule 2 两处 |
| 基类契约 | BaseListViewModel.kt:34 | `abstract suspend fun collectModels(): List<MyModel>` |
| 加载去重 | BaseListViewModel.kt:60-71 | `loadJob` 在跑就忽略新的加载请求 |

## 计划改动

1. lld 取数下沉.

   - 候选 A: 全部下沉进 `HomeRepository`. 文件少, 但仓库已经 1210 行.
   - 候选 B: 新建 `ui/home/datasource/LldLoader` (名字待定, 但不要用 `LldSourceResolver`:  
     subtask-01 已经有一个 `LldSource` 枚举表示在线/离线, 两个名字同头不好读), 负责三级回退,  
     返回一个结果类型: 拿到的 lld 加一组错误信息. 仓库只管存取.
   - **已定 B** (负责人 2026-10-29): 回退链本身就是一段有分支的逻辑, 单独立起来能单独测;  
     仓库保持"取数加判断", 行数不再涨.
   - 一条时序上的约束: 2c 排在 2a 之后, 那时错误已经是"原因"而不是拼好的文案, 所以 `LldLoader`  
     返回失败原因 (Found / Missing / Failed 那一套), 文案交给 mapper. 不要在这里拼字符串.
   - `AssetManager` 的来源已经查清: `AppGraph.kt:89-92` 的 `@Provides app: MyApplication` 把应用绑进了图,  
     所以构造器注入 `MyApplication` 不用新增绑定 (它本身就是 `Context`, `app.assets` 直接能用);  
     要注入 `Context` 或 `AssetManager` 才需要补一个 `@Provides`.

1. 编排收回.

   - 条目顺序进 subtask-05 的清单. `HomeViewModel.detect()` 整个删除.

1. ViewModel 只剩推导.

   - 候选 A1: 保留基类的 `suspend collectModels()`, Home 内部改成一次 `flow.first()`.  
     改动最小, 但设置变化时不会自动重算, 两条补丁规则仍然要留.
   - 候选 A2: 基类换成"观察一个 Flow", 例如 `abstract fun modelsFlow(): Flow<List<MyModel>>`,  
     `loadJob` 与 `isLoadingStateFlow` 相应改. Home 用  
     `combine(设置流, 检测结果流, 刷新) { ... }` 推导; Others 与 Prop 用 `flow { emit(load()) }` 包一层.  
     这是 plan.md 写的目标形态, 也是唯一能让两条补丁规则消失的形状.
   - 三个源缺一不可: 少了 `refresh` (plan.md:89 的原话是 "combine(settings, 检测源, refresh)"),  
     下拉刷新就没有触发点, 而它现在是靠 `refresh()` 重新跑一次 `collectModels()` 实现的  
     (BaseListViewModel.kt:56-71). 走 A2 之后这个触发要由 flow 自己提供, 这是 A1 与 A2 的实质差别.
   - A2 会动到屏幕: `MyModelListScreen.kt:53` 与 `:54` 收集两个 StateFlow, `:59` 调 `init()`,  
     `:71` 兜一个空列表, `:73` 接 `refresh`, 还有 `MyModelListContent` 的 `models: PersistentList<MyModel>`  
     形参 (`:83`). 这五处加一个形参都要跟着改.
   - `PersistentList` 只在基类构建一次那条优化 (04-架构.md 里记着) 在新契约下保不保, 也要一起定:  
     保的话, flow 的输出类型仍是 `PersistentList`.
   - **已定 A2** (负责人 2026-10-29). 代价是基类与三页一起动, 违反"改动最小"的直觉, 但 A1 会留下两条补丁规则,  
     等于把同一段编排留到以后再改一次, 与"不建过渡形态"的决定冲突.

1. 两条补丁规则与补丁入口的去留.

   - 走 A2 时, Rule 1 与 Rule 2 连同 `payloadOutdatedTargetSdkVersionApk()` 一起删除.  
     设置变化由 `combine` 触发重算.
   - 副作用: `BaseListViewModel.updateModelDetail()` 在删除之后可能没有任何调用点.  
     subtask-01 刚把它改成按类型寻址, 本步若确认没有消费者, 就把它删掉.  
     这一点要在实施时用全仓 grep 确认, 不凭印象.

1. 提交拆分.

   - 已定 (负责人 2026-10-29) 本子任务占两次提交, 这是对"每个子任务一次提交"的破例, 记进进度记录.
   - 第一次: 父类契约换成流, 三页与屏幕跟着改. 补丁路径先留着, 代码能编译, 行为不变.
   - 第二次: 删 Rule 1, Rule 2 与 `payloadOutdatedTargetSdkVersionApk()`, 把 lld 回退链下沉到 `LldLoader`.
   - 两次提交的提交信息要能看出是同一条线, 例如都用同一个 scope, 正文互相点名.

1. 文档.

   - 根 `AGENTS.md` 的索引行与 [04-架构](../../agents-cn/04-架构.md), [06-检测条目](../../agents-cn/06-检测条目.md)  
     里描述"ViewModel 调 collectModels, 调用顺序决定列表顺序"的段落改写.
   - 本步改完, plan.md 里"任务目标"那一段才算达成.

## 有意不改

- 检测执行与并发. 归 subtask-05.
- 检测条目的判断与映射. 归 subtask-04.
- Others 与 Prop 的条目内容. 只改成 flow 形状, 内容不动.
- 设置存储本身. `SettingsStore` 的流已经够用.

## 覆盖核对表

| 计划里的范围 | 问题条目 | 本子任务覆盖 | 留给后续 |
|---|---|---|---|
| 编排收回 | [#11](../../dev/issues-cn/03-UDF-单向数据流/11-HomeViewModel手工编排.md) 后半 | 是 | 无 |
| lld 回退链下沉, UI 不 try | #11 的迁移清单, [05-代码规则](../../agents-cn/05-代码规则.md) | 是 | 无 |
| 补偿路径的两个不对称 | #11 的正文 | 是 | 无 |
| 23 行手抄消失 | #11, #03 | 是 | 无 |
| 规则文件改写 | plan.md 的 subtask-06 范围 | 是 | 无 |
| 需求条目 | FR-7 服务端参考数据同步与离线缓存 | 是, 取数与回退这一段 | 同步策略本身不改 |
| 需求条目 | NFR-2 隐私 (缓存只放私有存储) | 是, 磁盘写随取数一起搬 | 无 |
| 需求条目 | FR-4, FR-5, FR-8, FR-13, FR-16, NFR-5 | 是 | 无 |

## 问题清单的回答

老板没有给问题清单.

## 验证

- `./gradlew assembleFossDebug`, `./gradlew testFossDebugUnitTest :base:testDebugUnitTest`,  
  `./gradlew lintFossDebug`.
- 黄金数据再对一次: 本步不改展示内容, `(key, detail, color, type)` 四列应逐行相同.
- 手工:
  - 断网冷启动, 看 lld 行是否走离线, 错误文案是否仍出现在该条目自己的行里.
  - 联网启动, 看在线数据是否写盘 (下次断网启动能用).
  - 加载中途切 Tab 再回来, 确认不会启动第二次检测 (FR-13 的去重).
  - 开关排序设置, 看目标行是否即时重排, 且不出现两套顺序.

## 风险

- 高. UDF 主路径重排, 牵动基类与三页.
- 走 A2 时基类契约变化, Others 与 Prop 必须同批改到能编译, 否则中途没有可编译的点.  
  这一点与"每个子任务独立编译"的要求冲突, 需要老板知道: 本子任务内部可能要分两次提交, 但整体算一个单元.
- `AssetManager` 的注入方式若 Metro 图里没有现成绑定, 要先补绑定再下沉.
- 删 `updateModelDetail` 之前必须 grep 全仓, 不能因为看到零引用就删.

## 需要老板定

- 无. 已定 (2026-10-29):
  - 基类走 A2; `updateModelDetail` 在 grep 确认零消费者后删除.
  - `MyModelType` 与清单 id 不合并; 清单 id 用检测条目目录的 HOME 编号, `type` 只留 `contentType` 一处用途.
  - lld 回退链放进新类 `LldLoader`; 允许拆成两次提交 (见上面的提交拆分).
