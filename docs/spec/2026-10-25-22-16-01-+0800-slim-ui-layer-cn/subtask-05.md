# subtask-05 2b 注册表引擎

> **状态**: living. 事实核对时间 2026-10-27, 代码在 `502a6eb7`.  
> 任务级计划见 [plan.md](plan.md); 流程见 [13-任务工作流](../../agents-cn/13-任务工作流.md).

## 目标

每个检测项变成清单里的一条数据, 由一个引擎统一执行: 并发有上限, 能感知取消, 展示顺序固定,  
条目之间的数据依赖显式写出来. Others 与 Prop 一并入表.

## concept primer

- 现在"有哪些检测项, 什么顺序"写在三个函数体里: `HomeViewModel.detect()` 里 23 个检测调用加一次取挂载  
  (HomeViewModel.kt:211-241), `OthersViewModel.collectModels()` 的 37 次追加,  
  `PropViewModel.collectModels()` 的 5 个来源.
- 整段检测跑在**一个** `withContext(Dispatchers.IO)` 块里, 检测方法本身都不是 suspend 函数  
  (HomeViewModel.kt:211). 协程只在挂起点观察取消, 所以开跑之后取消要等整块返回.
- 有些数据被多个条目共用: `lld` 是 Home 十几个条目的输入, `mounts` 被 SAR 与 APEX 两行共用  
  (HomeViewModel.kt:221-230), 设置里的排序开关只影响一行.
- 有界并发不能建在共享的 shell 之上而不查它是否可并发调用: `IShell` 的实现 `ShellLibSu`  
  (ui/common/ShellLibSu.kt:16) 是单例对象, 底层 libsu 的实例能否并行执行命令, 本报告不下结论,  
  列为开工前必查项.
- 特征测试的意思是: 先照现状把行为写成断言, 再改结构. 现状里有已知的错, 那些不能照抄进断言.
- 比对语义散在各行 ([#43](../../dev/issues-cn/08-反模式与卫生/43-比对语义逐行adhoc.md)), 入表时要一处一处搬,  
  并用测试锁住.

## 现状与证据

| 项 | 位置 | 现在是什么样 |
|---|---|---|
| Home 条目清单 | HomeViewModel.kt:211-241 | 23 个检测调用加一次取挂载, 写在函数体里 |
| Others 条目清单 | OthersViewModel.kt | 37 次 `tempModels +=`, 6 次包在版本判断里 |
| Prop 条目清单 | PropViewModel.kt | 5 个来源, 行数随系统属性数量变 |
| 串行执行 | HomeViewModel.kt:211; 另两页同样 | 一个 `withContext(IO)` 块跑完所有条目 |
| 取消 | 全仓只有 `loadJob` 与设置页用 `job?.isActive` 去重 | 检测块内无取消检查 |
| shell 实现 | ui/common/ShellLibSu.kt:16; base/src/main/.../ShellResult.kt | 只映射 `out`; 注释说 `output` 是 stdout 加 stderr, 与实现不符 |
| 比对语义 | HomeRepository.kt:237 (魔法算术 `250205 - 250101`), :298 (`getSecurityPatchYearMonth`), :616-668 (`detectMainline`); `isDateHigherThanConfig` 从 :199 起 | 各写各的, 无测试 |

## 计划改动

1. 条目清单变成数据.

   - 一条 `Detector` 数据: 稳定 id, 标题 (资源 ID 或原始标题), 该条目的三态语义说明,  
     需要的共享输入 (无 / lld / mounts), 执行函数 (返回 subtask-04 的领域类型).
   - 三页各一份清单, 顺序就是展示顺序. 增删条目只改清单一处.

1. 执行模型.

   - 候选 A: 分阶段. 第一阶段取共享输入 (`lld`, `mounts`, 设置值), 第二阶段按清单并发执行条目,  
     结果按清单下标归位. 依赖只有三层, 阶段式写得清.
   - 候选 B: 每条目声明自己的输入, 引擎按依赖排拓扑序. 表达力更强, 但本仓的依赖关系简单,  
     多出来的机制没有用武之地.
   - **已定 A** (阶段式, 负责人 2026-10-29).
   - 并发上限: 已定 (负责人 2026-10-28) 第一版全部串行. 先把条目清单, 顺序, 取消检查做对;  
     有界并发等查清 `ShellLibSu` 能否并发之后单独做. 那时的候选是"读属性与纯计算的条目并发,  
     走 shell 的条目串行". 这一版不要提前造调度器.
   - 取消: 每个条目开始前检查一次 `ensureActive()`. shell 命令本身不可中断, 只能等它返回,  
     这一点要在报告里写明, 不能承诺"随时可取消".
   - 顺序: 结果按下标放进固定大小的数组, 谁先跑完都不影响展示顺序.

1. Others 与 Prop 入表.

   - Others 的 6 个版本判断变成条目的前置条件, 判断逻辑保留, 只是从函数体搬进条目声明.
   - Prop 的 5 个来源各成一条, 行数仍在条目内部决定.
   - Others 与 Prop 的行没有结论, 条目返回的类型不带状态色.

1. 比对语义入表并锁定.

   - 11 个对参考数据做比对的 Home 条目 (按行数是 12 行, securityPatch 一个条目两行),  
     比较方式随条目搬进清单.
   - 特征测试先照现状写, 但有三个已知错例不能原样锁. 处置已定 (负责人 2026-10-29):  
     第一个在本步修并锁正确行为, 第二个锁现状并另立条目, 第三个只补注释与测试说明:
     - `detectSecurityPatch`: 设备报 `YYYY-MM` 时, `substringBeforeLast('-')` 取到的是年份  
       (HomeRepository.kt:298 与调用处 :273-296), 完全同期的设备被判红.
     - `detectMainline`: 左边是包的 `versionName` (任意格式), 右边是库里的日期  
       (HomeRepository.kt:616-668), 量纲不同, `"3.1"` 这类版本恒判已更新.
     - `detectBuildId`: 预览版偏移做魔法算术 `250205 - 250101` (HomeRepository.kt:237).  
       这一处不一定错, 但要在测试里把偏移的来历写清楚, 否则以后没人敢改.
   - 不锁的范围: [#67](../../dev/issues-cn/05-稳定性与错误处理/67-检测规则方向存疑.md) 涉及的 VNDK 与 APEX 判定,  
     [#66](../../dev/issues-cn/05-稳定性与错误处理/66-标题与数据源不符.md) 的 binder 行. 这两处已定暂缓,  
     语义按现状保留, 测试不锁.

1. shell 的 stderr.

   - **已定加字段** (负责人 2026-10-29): `ShellResult` 增加 `errors` 字段, `output` 的语义保持"只有 stdout",  
     注释随之改成与实际一致. 解析层不受影响, 失败可归因.
   - 被否掉的: 把 stderr 合并进 `output`. 改动最小, 但所有遍历 `output` 的解析  
     (getprop 的 `[k]: [v]` 折叠, `/proc/mounts` 的六列切分) 都要处理混进来的行.
   - 落点共三处, 一个都不能漏:
     - `base/.../base/shell/ShellResult.kt`: 加 `errors` 字段, 改 `output` 的注释.
     - `app/.../ui/common/ShellLibSu.kt:19`: 构造点一, 位置参数要跟着补.
     - `base/.../base/shell/impl/ShellDefault.kt:26`: 构造点二.
   - 另有无参构造 `ShellResult()` 在 `HomeRepository.kt:568` (API 低于 P 的守卫分支). 新字段带默认值,  
     这处不用改.
   - `ShellDefault` 的处置明写: 它按 `isSuccess` 二选一读流 (`:20-21`), 失败时把 stderr 塞进 `output`,  
     与新契约"`output` 只有 stdout"相反. 它是零调用实现, [05-代码规则](../../agents-cn/05-代码规则.md):31 写明  
     "现在没有调用方, 这是故意的", 并且已经记着"将来真要启用它, 先修那个死锁". 所以本步**不改**,  
     也不给它加 `errors`. 这一句留在报告里, 免得实施的人顺手把它改坏.

1. 规则文件.

   - [04-架构](../../agents-cn/04-架构.md) 补执行模型的说明. 本子任务新增的抽象属于架构决定, 不记下来,  
     下一个会话会重新设计一遍.
   - [06-检测条目](../../agents-cn/06-检测条目.md) 整份流程要重写, 因为它是围绕"改函数体"写的:  
     第 2 步的"加一个 `detect...()` 方法, 返回 `MyModel`" (:9), 第 3 步的"调用顺序决定列表顺序" (:12-13),  
     第 5 步的"目录里的顺序就是调用顺序" (:17). 清单做出来之后, 加条目是往清单加一条数据, 顺序由清单决定.
   - 新增一条不变式要写进规则文件与 [检测条目目录](../../requirements-cn/04-检测条目目录.md):  
     目录按条目 id 排, 代码里的清单按执行顺序排, 两份有序表必须一致. 这条现在哪都没有,  
     不写下来的话, 下一个会话加条目时会把两个顺序搞岔.

## 有意不改

- 编排仍在 ViewModel. 归 subtask-06.
- `Dispatchers` 的选择不改. 检测链已经在 IO 上, [#14](../../dev/issues-cn/archived/04-并发与调度/14-检测链用CPU线程池.md) 已结.
- VNDK 与 APEX 的判定方向不改. #67 暂缓.
- 圆点颜色映射不引入. 各条目自己定 (NFR-7).

## 覆盖核对表

| 计划里的范围 | 问题条目 | 本子任务覆盖 | 留给后续 |
|---|---|---|---|
| 条目变数据, 引擎统一执行 | [#03](../../dev/issues-cn/01-架构与分层/03-无三态模型与注册表.md) | 是 | 无 |
| 有界并发与取消 | [#15](../../dev/issues-cn/04-并发与调度/15-检测串行不感知取消.md) | 是 | shell 可并发性待查, 查不清则全串行 |
| Others 与 Prop 入表 | 0g | 是 | 无 |
| 比对语义入表并锁定 | [#43](../../dev/issues-cn/08-反模式与卫生/43-比对语义逐行adhoc.md) | 是 | 已定: securityPatch 本步修, mainline 另立条目 |
| shell 的 stderr | [#63](../../dev/issues-cn/05-稳定性与错误处理/63-失败折成结论.md) 第 5 条 | 是 | 无 |
| 需求条目 | FR-1, FR-3, FR-6, FR-14, NFR-3 | 是 | 无 |

## 问题清单的回答

老板没有给问题清单.

## 验证

- `./gradlew assembleFossDebug`, `./gradlew testFossDebugUnitTest :base:testDebugUnitTest`,  
  `./gradlew lintFossDebug`.
- 黄金数据再对一次. 本步不改展示内容, 所以 `(key, detail, color, type)` 四列仍应逐行相同,  
  只有已知错例的修正会带来差异, 那些差异要逐条列出来.
- 手工: 三页各翻一遍; 中途下拉刷新或切 Tab 打断一次, 看是否按预期停止与重启, 不出现两次检测并行写同一份状态.

## 风险

- 高. 新抽象加并发, 本仓库没有先例.
- 并发与取消最容易出的是"看起来跑完了其实有任务被吞"和"两次加载互相覆盖". 前者靠结果条数核对,  
  后者靠 `loadJob` 现有的去重与一次加载一个结果集来避免.
- shell 能否并发未知. 查不清就串行, 先把结构和顺序做对, 并发留到有证据之后.
- 比对语义里两个已知错例的处置已定: securityPatch 本步修并锁正确行为, mainline 锁现状另立条目.  
  真正要小心的是锁的时候别把 mainline 的现状当成正确行为写进断言.

## 需要老板定

- 无. 已定 (2026-10-28 与 2026-10-29): 阶段式执行模型; 第一版全部串行; securityPatch 本步修而  
  mainline 锁现状并另立条目; `ShellResult` 加 `errors` 字段, `output` 的语义保持只有 stdout.
