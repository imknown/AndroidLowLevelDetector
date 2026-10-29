# subtask-01 先行批剩余三件

> **状态**: living. 事实核对时间 2026-10-27, 代码在 `502a6eb7`.  
> 任务级计划见 [plan.md](plan.md); 流程见 [13-任务工作流](../../agents-cn/13-任务工作流.md).

## 目标

把三处与主线无关的小改动先做完: 列表条目按类型寻址, lld 来源换成枚举,  
挂载读取失败不再静默走成功路径.

## concept primer

- 列表状态是 `modelsStateFlow: StateFlow<PersistentList<MyModel>?>` (BaseListViewModel.kt:26),  
  `null` 表示还没加载过. 加载标志另在 `isLoadingStateFlow` (同文件:31).
- 补丁接口现在收下标: `updateModelDetail(targetIndex: Int, newDetail: String)` (同文件:79).  
  下标由调用方先算好, 算完到写入之间列表可能已经被一次刷新换掉, 补丁就打到别的条目上.
- `MyModel` 带 `type: MyModelType` (MyModel.kt:22). 这个枚举现在只有 `General` 和  
  `OutdatedTargetSdkApk` 两个值 (MyModel.kt:7-10), 按类型寻址只在后者上成立.
- lld 来源现在用 `R.string.lld_json_online` 这个资源 ID 当判别值 (HomeRepository.kt:72),  
  同一个 ID 随后又被当文案用 (同文件:89).
- `MountDataSource.getMounts()` 直接遍历 `shell.execute(...).output` (MountDataSource.kt:27),  
  不查 `isSuccess`, 命令失败时返回空表, 上层走的就是成功路径.
- `PropRepository.getBuildProp()` 已经做过同一件事 (PropRepository.kt:66-75): 查 `isSuccess`,  
  失败产一行源级错误行, 错误行里带 `exitCode`. 这一件照它的做法写.

## 现状与证据

| 项 | 位置 | 现在是什么样 |
|---|---|---|
| 按下标打补丁 | BaseListViewModel.kt:79; HomeViewModel.kt:277-285 | 调用方 `indexOfFirst { it.type == OutdatedTargetSdkApk }`, 再传下标 |
| 资源 ID 当判别值 | HomeRepository.kt:66, 72, 89; HomeViewModel.kt:147, 153, 206-207 | 参数从 ViewModel 一路传到仓库, 比完又当文案; `@StringRes` 注解只在 `detect()` 的形参上 (HomeViewModel.kt:207), 仓库那侧没有 |
| 挂载失败静默 | MountDataSource.kt:24-36; HomeRepository.kt:403, 405, 712; HomeViewModel.kt:221-230 | 失败得到空表, SAR 与 APEX 两行按空表继续判 |

## 计划改动

1. 条目按类型寻址.

   - BaseListViewModel.kt:79 的签名改成 `fun updateModelDetail(type: MyModelType, newDetail: String)`.
   - 函数体在 `modelsStateFlow.update { }` 里定位: `indexOfFirst { it.type == type }`, 找不到就不动,  
     找到就 `replacingAt`. 定位与写入落进同一个原子块.
   - HomeViewModel.kt:277-285 里翻状态找下标的那几行删掉, 改成一行  
     `updateModelDetail(MyModelType.OutdatedTargetSdkApk, newDetail)`.
   - 注释写明一件事: `General` 是默认值, 同时被二十多个条目共用, 所以按类型寻址只对非默认值成立.  
     以后要给别的条目单独打补丁, 先给它一个自己的 type 值.

1. lld 来源换成枚举.

   - 新增 `enum class LldSource { ONLINE, OFFLINE }`, 放在 `ui/home/model/` 下, 与 `Lld` 同目录.
   - HomeRepository.kt:66 的形参换成 `source: LldSource`; :72 的判别换成 `source == LldSource.ONLINE`;  
     :89 改成 `when (source) { ONLINE -> R.string.lld_json_online; OFFLINE -> R.string.lld_json_offline }`.
   - HomeViewModel.kt:206-207 的签名同步换; :147 传 `LldSource.ONLINE`, :153 传 `LldSource.OFFLINE`.
   - `androidx.annotation.StringRes` 在 HomeViewModel 里**不能删**: `errorMessage()` 仍在用  
     (HomeViewModel.kt:246 `private fun errorMessage(@StringRes messageId: Int, ...)`).

1. 挂载读取查 `isSuccess`.

   三个候选, 差别在失败时渲染几行:

   - A. `getMounts()` 失败时抛异常. 这条路走不通: 取挂载发生在 `detect()` 里 (HomeViewModel.kt:221-223),  
     `detectSar` 与 `detectApex` 的 `guardedMyModel` 只包住各自的函数体 (HomeRepository.kt:406, :713),  
     看不到取数阶段的异常. 异常会穿出 `detect()`, 再穿出 `collectModels()` (HomeViewModel.kt:52-65),  
     落到 `BaseListViewModel.startLoad()` 的 `viewModelScope.launch` (BaseListViewModel.kt:65-70),  
     那里没有 try/catch, 协程终止, 整份列表写不进状态, 界面停在加载态. 一行错误行都没有.
   - B. `getMounts()` 返回 `MountQueryResult` (`Success(mounts)` / `Failed(exitCode)`), 两个检测各自判,  
     各渲染一行该条目的错误行. 行数不变, 失败可归因到挂载源.
   - C. 把挂载读取提到 `detect()` 的调用序列之前, 失败时只产一行源级错误行, 替换掉 SAR 与 APEX 两行.  
     行数从 25 变 24, 两个条目整行消失.

   **已定 B** (负责人 2026-10-28). 理由是 FR-3 的 AC2: 检测失败显示成**这个条目自己的**未知,  
   错误文字装在该条目自己的行里; SAR 与 APEX 是两个独立结论, 各报各的未知比合成一行更合这条.  
   C 的写法与 `PropRepository.getBuildProp()` 的先例更近, 但那个先例是一个来源几百行, 情形不同.

   选 B 之后 [plan.md](plan.md) 原第 37-38 行那句 "失败时仿 `PropRepository.getBuildProp()` 的已有做法  
   渲染源级错误行" 与实现不符. 这一段已在 2026-10-29 同批改好 (plan.md:37-39 现在写的是  
   "两个条目各渲染自己那一行的未知, 行内带 `exitCode`"), 实施时不要再改一遍.

   B 的落点: MountDataSource.kt:24-36 返回结果类型; HomeRepository.kt:403 转发;  
   :405 `detectSar` 与 :712 `detectApex` 各加一次判定; HomeViewModel.kt:221-230 的调用点跟着改类型.

## 有意不改

- "调用顺序就是列表顺序" 这条约定 (见 [06-检测条目](../../agents-cn/06-检测条目.md)).  
  本子任务不碰 `detect()` 的调用序列.
- `ShellLibSu` 合并 stderr. 归 subtask-05.
- `getBuildProp` 与 `exitCode` 的用法. 已经完成, 不动.
- `MyModelType` 不扩容. 这一件只服务 `OutdatedTargetSdkApk`.
- 补偿路径的其它问题 (固定重读离线文件, payload 动作里写盘). 归 subtask-06.

## 覆盖核对表

| 计划里的范围 | 问题条目 | 本子任务覆盖 | 留给后续 |
|---|---|---|---|
| #11 前半, 按类型寻址 | [#11](../../dev/issues-cn/03-UDF-单向数据流/11-HomeViewModel手工编排.md) | 是 | 编排收回归 subtask-06 |
| #44 第 1 处, 资源 ID 当判别值 | [#44](../../dev/issues-cn/08-反模式与卫生/44-资源ID当逻辑值.md) | 是 | 第 2 处 (`Version.id`) 不在本任务范围 |
| #63 剩余小件, 挂载读取 | [#63](../../dev/issues-cn/05-稳定性与错误处理/63-失败折成结论.md) | 是 | 三态结果类型与其它失败折叠归 subtask-04 / 05 |
| 需求条目 | FR-6 单项错误隔离 | 部分, 挂载这一处 | 全量归 subtask-04 |
| 需求条目 | FR-17 设置页条目清单 | 部分, 排序开关那一行的就地更新靠本件改的寻址 | 补丁路径的整体去留归 subtask-06 |

## 问题清单的回答

老板没有给问题清单. 本报告按 [plan.md](plan.md) 里本子任务的范围与目标自查.

## 验证

- `./gradlew assembleFossDebug`
- `./gradlew testFossDebugUnitTest :base:testDebugUnitTest` (此刻只有模板测试与实际存在的 `DateTimeExtTest`)
- `./gradlew lintFossDebug`
- 手工: 首页切设置里的排序开关, 看 `OutdatedTargetSdkApk` 那一行就地更新, 别的行不动.  
  断网启动一次, 看 mode 行的颜色与文案仍然是离线那一套.
- 分析三档按 [10-分析细则](../../agents-cn/10-分析细则.md) 走.

## 风险

- 低. 三件都是机械改写, 各自 1 到 3 个文件.
- 按类型寻址之后, 若出现第二个同类型条目, `indexOfFirst` 只改第一处. 现在不会发生,  
  注释里写明这个前提.
- 第三件的行数风险由已定的 B 避开: 25 行不变, 两个条目各报各的未知.

## 需要老板定

- 无. 第三件已定 B (2026-10-28).
