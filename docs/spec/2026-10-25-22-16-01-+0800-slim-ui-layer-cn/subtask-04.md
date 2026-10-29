# subtask-04 2a 领域化 (一次性)

> **状态**: living. 事实核对时间 2026-10-27, 代码在 `502a6eb7`.  
> 任务级计划见 [plan.md](plan.md); 流程见 [13-任务工作流](../../agents-cn/13-任务工作流.md).

## 目标

三页所有检测方法改成返回领域对象, 判断与格式化分开; 把领域对象翻译成 `MyModel` 的 mapper  
只放在 UI 包. 改造后三页每行的 `(key, detail, color, type)` 与 subtask-02 的黄金数据逐行相同.

## concept primer

- 现在仓库方法把三件事揉在一个函数里: 取数 (读 `Build.*`, 跑 shell), 判断 (与参考数据比),  
  展示 (选文案 `R.string.*`, 选颜色 `StatusColor`). 返回值直接是界面模型 `MyModel`.
- `MyModel` 是界面模型: `title` 是资源 ID 或原始标题, `detail` 是拼好的本地化文案,  
  `color` 是展示用状态色 (MyModel.kt:18-28).
- 领域对象是另一回事: 它只说事实, 例如"媒体性能等级 33, 比系统版本低一代", 不带文案也不带颜色.
- [FR-3](../../requirements-cn/02-功能需求.md) 把 Home 的结论定成三态: 支持, 不支持, 未知,  
  并写明检测失败也算未知, 错误文字装在该条目自己的行里. 所以内部要能分出"没读到"和"读失败",  
  界面上两者都落在未知.
- 颜色不设跨条目统一映射 (NFR-7 与 [#33](../../dev/issues-cn/archived/07-UI与无障碍/33-Unknown渲染红色.md)),  
  每个条目按自己的语义决定. mapper 是唯一做这件事的地方.
- Others 与 Prop 的行没有结论, 也没有圆点 (FR-14, #69). 它们的领域对象不需要状态色.

## 现状与证据

| 项 | 数字或位置 | 说明 |
|---|---|---|
| `HomeRepository` 规模 | 1210 行 | 22 个 public `detect*` 加 `getOutdatedTargetSdkVersionApkModel` 共 23 个产行入口, <br>另有 3 个 private 辅助 (`:273` `detectSecurityPatch`, `:507` `detectTreble`, `:559` `detectGsi`); 产 25 行 |
| `getMyString` 调用 | 全项目 101 个调用点, 其中 69 处在 `HomeRepository` | 判断被就地翻成文案 |
| `Build.*` 直接读 | `HomeRepository` 里 45 行 | 迁移后的可测性由它决定 |
| `toColoredMyModel` | `HomeRepository` 里 24 处 | 颜色在仓库里定 |
| `toSupportOrNotString` | HomeRepository.kt:1185-1191 | 判断结果当场变成两条文案之一 |
| Others | `OthersRepository.kt` 37 个 `get*` 方法, 37 次追加 | 其中 36 个被 guard 包住, `getPartitionFingerprints` (:210) 自己按分区隔离 |
| Prop | `PropRepository.kt` 3 个入口 | 行数与系统属性数量相关 |
| 失败处理 | `MyModelExt.kt:35-50` (Home), `:83-98` (Others) | 现在由 `guarded*` 在仓库侧包住整个 block |

## 计划改动

1. 结果类型.

   - 引入一个统一的结果容器: `DetectResult<out T>`, 三个分支 `Found(value)`, `Missing(reason)`,  
     `Failed(cause)`. 每个检测项的语义再用自己的类型表达, 例如 `PerformanceClassInfo` 的  
     `Met` / `Lagging` / `NotDeclared` / `NotSupported`.
   - 为什么两层: `Failed` 与 `Missing` 的处理在每个条目都一样 (都落未知), 这一层用统一容器写得出来;  
     而"达标还是落后"只能由条目自己说清, 统一容器装不了.
   - **已定两层** (负责人 2026-10-29). 被否掉的两个:
     - 只做一层 (`Found` / `Missing` / `Failed` 各自带可选值): "落后一代"这种事实只能塞进字符串,  
       而且判断会被迫落进 mapper, 等于把 2a 要修的毛病搬回界面层.
     - 每项一个 sealed interface, 不要统一容器: 表达力最强, 但失败处理要抄 23 遍.

1. 仓库只返回领域对象.

   - `HomeRepository` 的 23 个产行入口, `OthersRepository` 的 37 个 `get*`, `PropRepository` 的 3 个入口,  
     签名全部去掉 `MyModel` 返回值, 改成领域类型.
   - 23 个入口里有一个不叫 `detect*`: `getOutdatedTargetSdkVersionApkModel` (HomeRepository.kt:1105).  
     它是全仓唯一产出 `MyModelType.OutdatedTargetSdkApk` 的地方 (:1162-1167), 也是 subtask-01 第一件  
     改成按类型寻址的那一行. 按"detect 方法"去列清单会把它漏掉. 三个 private 辅助随它们各自的  
     入口一起改, 不单独算一条.
   - 判断留在仓库; 文案与颜色全部移出. `toSupportOrNotString` 删除, 由 mapper 决定文案.
   - 取数仍留在仓库与 DataSource. 这一步不搬取数, 不引入 UseCase.

1. mapper 只放 UI 包.

   - 新增 `ui/home/mapper/HomeModelMapper.kt`, `ui/others/mapper/OthersModelMapper.kt`,  
     `ui/prop/mapper/PropModelMapper.kt`.
   - mapper 取文案的来源要先定. 应用级 Context 只有一处, `MyApplication.instance` (MyApplication.kt:73),  
     它本身也是 `Context`. 图里已经有现成入口: `AppGraph.kt:89-92` 的工厂用 `@Provides app: MyApplication`  
     把应用绑进图, 所以 mapper 注入 `MyApplication` 不用新增绑定; 要注入 `Context` 或 `Resources`  
     才需要补一个 `@Provides`. 推荐注入 `MyApplication`.
   - "不再用 `MyApplication.getMyString`" 指的是换成 mapper 内部的 `context.getString(...)`, 不是去掉  
     对 Application 的依赖. 这一点写清, 免得实施时以为要造一个新的 Context 通道.
   - 迁移时有两种默认行为要分开处理, 现在混在一起:
     - `toTranslatedDetail` (MyModelExt.kt:163-167): `detail` 为空时给 `R.string.build_not_filled`.
     - `toColoredMyModel` 的两个重载 (MyModelExt.kt:56-72) 用 `detail.toString()`, 会把 null 拼成字面量  
       `"null"` 显示出来. 后者是黄金数据 `detail` 列里会出现的内容, 迁移时漏掉就会让逐行对照出现差异.
   - 映射规则: `Found` 按条目语义给颜色, `Missing` 与 `Failed` 落未知, 颜色按该条目自己的语义定  
     (FR-3 AC2, NFR-7).

1. 失败隔离挪到调用侧.

   - 现在 `guardedMyModel` 包住整个检测 block (MyModelExt.kt:35-39). 迁移后仓库返回 `Failed(cause)`,  
     绝大多数失败走这条正常的返回路径.
   - 现在 `guardedMyModel` 包住的是**仓库方法里造 `MyModel` 那段 block** (MyModelExt.kt:35-39),  
     `HomeRepository` 里 24 处, `OthersRepository` 里 36 处. 仓库一旦不再返回 `MyModel`, 这层包装就  
     没有位置了, 它必须挪到调用侧. 这一步没有"先保留原形状"的选项.
   - 落点: subtask-04 期间在调用侧留一层薄 guard, 只处理"没预料到的异常" (例如探针抛出的  
     `RuntimeException`), 常规失败走 `DetectResult.Failed` 的正常返回路径, 不再依赖它.
   - subtask-05 造引擎时把这层薄 guard 收进引擎, 调用侧那层随之删掉.

1. 逐条单测.

   - 每个检测项一个测试类, 断言领域对象, 不断言文案.
   - `Build.*` 读数的手段按 0f 的优先级逐项裁量: 真机或模拟器上的仪器测试 > Robolectric > 探针假值.  
     裁量结果要一张表, 逐项写清用哪种手段和为什么, 交老板确认.
   - 判断标准: 只用 `Build.*` 静态字段和纯计算的条目, Robolectric 够用; 需要真实 shell,  
     真实 `/proc`, 真实包管理器的条目, 只能仪器测试; 两者都不行的条目 (例如依赖厂商改过的属性),  
     用探针假值, 并在报告里写明测不到真实行为.

1. 规则文件.

   - [06-检测条目](../../agents-cn/06-检测条目.md) 的流程改写: 仓库返回领域对象, 再经 mapper 出 `MyModel`.
   - 决定批 0d 说的 "只随 2a 改写 '新增一个检测条目' 一处", 指的就是 06-检测条目 这一份.  
     不要动 `dev/issues-cn/README.md` 里 0d 那一行, plan.md:23 已经写明本计划不重开任何已定项.
   - 根 `AGENTS.md` 的索引行只改"什么时候读"的描述, 不改结构.

## 有意不改

- `MyModel`, `MyModelCard`, `MyModelListScreen` 不动. 它们是 UI 层的东西, 改的只是谁生产 `MyModel`.
- Settings 页的 Version 行不动. 那是 [#44](../../dev/issues-cn/08-反模式与卫生/44-资源ID当逻辑值.md) 第 2 处,  
  在独立小件清单里.
- 检测的执行顺序与并发不动. 归 subtask-05.
- 编排留在原处. 归 subtask-06.
- 圆点颜色的语义不改. [#33](../../dev/issues-cn/archived/07-UI与无障碍/33-Unknown渲染红色.md) 已定为有意接受.

## 覆盖核对表

| 计划里的范围 | 问题条目 | 本子任务覆盖 | 留给后续 |
|---|---|---|---|
| 三页全部检测方法领域化 | [#01](../../dev/issues-cn/01-架构与分层/01-无领域模型.md) | 是 | 无 |
| 三态结果类型 | [#03](../../dev/issues-cn/01-架构与分层/03-无三态模型与注册表.md) 的类型部分 | 是 | 注册表引擎归 subtask-05 |
| 文案当判断值 | [#42](../../dev/issues-cn/08-反模式与卫生/42-字符串当哨兵值.md) | 是 | 无 |
| 失败被折成结论 | [#63](../../dev/issues-cn/05-稳定性与错误处理/63-失败折成结论.md) | 类型层面 | 取数层的 Found/Missing/Failed 细节随条目落, 挂载已在 subtask-01 |
| 逐条单测 | #50 的测试铺开 | 是 | 无 |
| 需求条目 | FR-1, FR-2, FR-3, FR-6, FR-14, FR-15, NFR-7 | 是 | 无 |
| 需求条目 | FR-9 合法的检测手段与逐级降级 | 是, 取数的 Found/Missing/Failed 决定这一条 | 无 |
| 需求条目 | NFR-1 兼容性 (老旧与厂商改过的系统上显示未知, 绝不崩溃) | 是, 迁移的验收底线 | 真机覆盖范围见风险节 |

## 问题清单的回答

老板没有给问题清单.

## 验证

- `./gradlew assembleFossDebug`, `./gradlew testFossDebugUnitTest :base:testDebugUnitTest`,  
  `./gradlew lintFossDebug`.
- 黄金数据对照: 改造后在真机或模拟器上重跑快照生成器 (生成器随本步改一次),  
  与 `baseline/` 逐行 diff, 要求 `(key, detail, color, type)` 四列全等. `type` 不能漏: 它是列表复用池  
  分池的依据 (MyModelListScreen.kt:132), 也是本步迁移时要重新分配的字段.
- 手工: 三页各翻一遍, 与改造前截图对照. 断网启动一次看 Home 的 lld 行.

## 风险

- 高. 45 处 `Build.*`, 24 处 `toColoredMyModel`, 69 处 `getMyString`, 25 行展示.
- 文案与颜色的细节容易在搬迁中走样. 黄金数据是唯一能证明等价的东西, 所以 subtask-02 必须先完成.
- 仪器测试需要设备, 与"CI 只跑 JVM 测试"的决定并存时, 这些条目在 CI 上不会被验证.  
  这一点要在报告里写清, 不能当成已验证.

## 需要老板定

- `Build.*` 读数逐项裁量表, 等实施时按上面的标准逐条给出, 由老板批准.
