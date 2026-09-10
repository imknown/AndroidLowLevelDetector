# 架构体检报告(Architecture Review)

> 分析时看的代码:develop 分支,提交 `4d570cdb`(2026-09)。
> 2026-09-06 的规格对照审计(原 `docs/dev/spec-gap-analysis/`,已并入)按严重程度并进各分册(编号 C/R/A,并入时已于 2026-09-11 在当时的代码上重新核对过)。
> **2026-09-13 两次改版**:①原第四分册「实施路线图(批次 0–7+)」退役(负责人定:业务逻辑修复暂缓、集中攻架构、改动一步到位),仍有行动价值的内容已并入各条目与[修复路线](#修复路线2026-09-13-改版架构优先);②全部分册**按问题类型重排**(架构 / SSOT / UDF / 反模式 / 已裁定),不再按 P0/P1/P2 优先级分册;已经修好的条目(AR-10、AR-13 的 1/2/4/11/14 号子项、AR-17 的 VIEW 残留部分、R9、C2)直接删除,记录留在 git 历史里。

一句话总结:项目的分层骨架(UI → ViewModel → Repository → DataSource)方向是对的,但**每一层都干了不该自己干的事**——数据层直接生产界面内容、全局单例随处可改、设置项没有唯一的数据来源。单个问题都不致命,合在一起会让「加一个设置项 / 加一个检测条目」要动的文件越来越多。此外,规格对照审计确认了**逐条错误隔离(FR-6)没实现是发布阻塞**(C1:检测没做保护,一抛异常整个应用就崩;暂缓中,见条目)。

## 目录

- [阅读说明](#阅读说明)
- [问题总览表](#问题总览表)
- [总体诊断](#总体诊断)
- [已经做对的地方](#已经做对的地方)
- [修复路线(2026-09-13 改版:架构优先)](#修复路线2026-09-13-改版架构优先)
- [附录 A · 审计出处与编号映射](#appendix-a)
- **[架构](architecture.md)**:[AR-01 无领域模型](architecture.md#AR-01)、[AR-04 服务定位器 / MyApplication 上帝对象](architecture.md#AR-04)、[AR-05 组合根分散](architecture.md#AR-05)、[AR-06 基类反向依赖 Activity](architecture.md#AR-06)、[AR-07 包结构 base 一词三义](architecture.md#AR-07)、[AR-16 零真实测试](architecture.md#AR-16)、[AR-20 常量仓库名不副实](architecture.md#AR-20)、[C6 圆点 Unknown 常渲染红色](architecture.md#C6)、[R5 无三态模型与注册表](architecture.md#R5)、[A3 检测串行不感知取消](architecture.md#A3)
- **[SSOT · 唯一数据来源](ssot.md)**:[AR-02 设置绕开唯一数据源 + 静态事件总线](ssot.md#AR-02)、[AR-03 myAndroid 全局可变单例](ssot.md#AR-03)
- **[UDF · 单向数据流](udf.md)**:[AR-08 HomeViewModel 手工编排 20+ 仓库方法](udf.md#AR-08)、[C5 排序收集器静默陈旧](udf.md#C5)
- **[反模式与隐患](anti-patterns.md)**:[AR-09 State 无错误态](anti-patterns.md#AR-09)、[AR-11 资源 ID 当逻辑值](anti-patterns.md#AR-11)、[AR-12 阻塞调用跑 CPU 线程池](anti-patterns.md#AR-12)、[AR-13 零散小问题(剩 9 项)](anti-patterns.md#AR-13)、[AR-14 Prop 页逐 key 查询](anti-patterns.md#AR-14)、[AR-15 lld.json 日期隐式契约](anti-patterns.md#AR-15)、[AR-17 QUERY_ALL_PACKAGES 申报缺档](anti-patterns.md#AR-17)、[AR-18 杂项隐患](anti-patterns.md#AR-18)、[AR-19 字符串当哨兵值](anti-patterns.md#AR-19)、[C1 逐条错误隔离未实现(暂缓)](anti-patterns.md#C1)、[R7 版本合并残留弱点](anti-patterns.md#R7)、[R11 License 合规不到位](anti-patterns.md#R11)、[R13 比对语义逐行 ad hoc](anti-patterns.md#R13)
- **[已裁定事项(不动 / 推迟)](rulings.md)**:[A2 foss 纯度防护](rulings.md#A2)、[C4 拖拽中重建吞手势](rulings.md#C4)、[R1 工具栏滚动](rulings.md#R1)、[R2 自适应布局](rulings.md#R2)、[R10 滚动条 stub](rulings.md#R10)、[R12 命名漂移](rulings.md#R12)、[A4 无 root 层(非缺陷)](rulings.md#A4)、[A7 构建脚枪](rulings.md#A7)

## 阅读说明

- 问题编号有两套,都只是标识符(不代表先后,行动顺序见[修复路线](#修复路线2026-09-13-改版架构优先)),分册之间用编号互相引用:
  - `AR-01` ~ `AR-20`(AR = Architecture Review)—— 本报告原创发现。
  - `C1…C7` / `R1…R15` / `A1…A8`(C = 正确性 · R = 规格↔代码偏差 · A = 架构/栈/测试)—— 2026-09-06 规格对照审计的发现,编号保留。还有行动价值的已并入各分册;重复的和已经解决的不再重复记录,见[附录 A](#附录a-审计出处与编号映射)。
- 每个问题统一包含四块:**问题核心代码**(可直接对照源码行号)、**直接原因**(这行代码为什么错)、**根本原因**(为什么会写出这行代码)、**修复方案**(含目标代码)。从审计并进来的条目用简化格式(结论 + 证据 + 修复方向)。
- 严重程度:
  - **P0**:核心架构问题。影响全局,新代码会不断复制坏模式。
  - **P1**:结构性耦合,或已被证实的实际风险(性能、正确性、工程配套)。
  - **P2**:局部坏味道或低概率隐患。影响面小,顺手修即可。
  - 审计并入条目在 P1/P2 之下以 🔴🟡🟢ℹ️ 标注原审计严重程度。
- 修复难度:
  - **易**:原地小改,涉及 1~2 个文件,不动函数签名。
  - **中**:跨多个文件,签名会变,但属于照葫芦画瓢的替换,可以一次提交完成。
  - **难**:需要引入新的抽象,必须分步走、一点一点迁。

## 问题总览表

按类型分册;表内按严重程度优先、修复难度其次排。C/R/A 条目按并入后的分册排列。已修复的条目(AR-10 mounts 缓存、AR-13.1/13.2/13.4/13.11/13.14、AR-17 的 VIEW 残留、R9 商店页、C2)已从本报告删除,记录见 git 历史;AR-13.12 是裁定关闭(未修复),条目仍保留在 [AR-13](anti-patterns.md#AR-13) 内。

| 编号 | 问题 | 严重程度 | 修复难度 | 一句话危害 |
|------|------|----------|----------|------------|
| [AR-01](architecture.md#AR-01) | 数据层直接产出展示内容，无领域模型 | P0 | 难（可渐进） | 改文案、改颜色都要动数据层；检测逻辑永远无法单测 |
| [AR-02](ssot.md#AR-02) | 设置项绕开唯一数据源 + 静态事件总线 | P0 | 中 | 每加一个设置项就多一个静态 Flow，事件丢了都查不到 |
| [AR-03](ssot.md#AR-03) | myAndroid 全局可变单例被两处写入 | P0 | 低~中 | 判断结果依赖调用顺序，问题偶发且难复现 |
| [AR-04](architecture.md#AR-04) | 服务定位器 + MyApplication 上帝对象 | P0 | 中 | 任何新文件都能伸手拿全局对象，依赖关系看不见 |
| [C1](anti-patterns.md#C1) | 逐条错误隔离（FR-6）未实现(暂缓) | P1 · 🔴 发布阻塞 | 中（约 30+ 处逐个加 try/catch） | 一个探针抛异常 → 启动即崩溃循环 |
| [AR-05](architecture.md#AR-05) | 组合根分散，每个 Fragment 自己建对象图 | P1 | 低 | 改一个仓库构造函数要同时改多个不相干的文件 |
| [AR-06](architecture.md#AR-06) | 基类反向依赖 MainActivity 具体视图 | P1 | 低 | 基类被锁死在这个 Activity 上，insets 逻辑重复两处 |
| [AR-07](architecture.md#AR-07) | base 一词三义 + 包级循环依赖 | P1 | 中（机械搬移） | 层次方向看不出来，新人没法从包名判断依赖规则 |
| [AR-08](udf.md#AR-08) | HomeViewModel 手工编排 20+ 仓库方法 | P1 | 中 | 每加一个首页条目都要同时改 ViewModel 和仓库两处 |
| [C6](architecture.md#C6) | 圆点语义逐行各自为政，Unknown 常渲染红色 | P1 · 🔴 | 中 | 非专家用户被红色误导（NFR-7 风险） |
| [R5](architecture.md#R5) | 无三态模型、无检测器注册表（目录硬编码） | P1 · 🔴 | 难 | Compose 迁移前必须补上的主要结构缺口 |
| [AR-14](anti-patterns.md#AR-14) | Prop 页设置项逐 key binder 查询 | P1 | 中 | 一次加载几百次串行跨进程调用，低端机 Prop 页明显变慢 |
| [AR-15](anti-patterns.md#AR-15) | lld.json 日期格式是隐式契约，无边界校验 | P1 | 低 | 上游一改格式，联网用户直接崩溃 |
| [AR-16](architecture.md#AR-16) | 零真实测试，CI 只编译不测试 | P1 | 易 | 所有重构都没有回归保护网 |
| [AR-19](anti-patterns.md#AR-19) | 本地化字符串被当作哨兵值比较 | P1 | 中 | 状态藏在翻译文案里，没法单测；翻译撞车或切语言时静默误判 |
| [C5](udf.md#C5) | 排序收集器静默陈旧 | P1 · 🟡 | 低 | 设置开关可能被悄悄丢掉 |
| [R7](anti-patterns.md#R7) | 版本合并：时区偏移风险 + 在线无条件覆盖 | P1 · 🟡 | 低（测试锁定） | 格式假设需要测试锁住（含时区后缀） |
| [R13](anti-patterns.md#R13) | FR-7 比对语义逐行 ad hoc 存在 | P1 · 🟡 | 中 | 写进文档+用测试锁住；注册表落地时搬进注册表条目 |
| [A3](architecture.md#A3) | 检测严格串行、不感知取消 | P1 · 🟡 | 中 | 慢设备加载慢；放大竞态窗口 |
| [A6](architecture.md#A6) | 规格技术栈缺口（测试基建等） | P1 · 🟡 | — | 分批补齐；大重构开工前先落测试基建 |
| [AR-09](anti-patterns.md#AR-09) | State 无错误态，错误折叠进展示字符串 | P2 | 中 | 没保护的加载异常直接崩溃；没法做「重试」 |
| [AR-11](anti-patterns.md#AR-11) | 资源 ID 被当作逻辑值和数据传递 | P2 | 低~中 | 资源重命名/混淆会悄悄改变程序逻辑 |
| [AR-12](anti-patterns.md#AR-12) | 阻塞调用占 CPU 线程池 + 重复建 HttpClient | P2 | 低 | 线程不够用风险 + 没必要的对象开销 |
| [AR-13](anti-patterns.md#AR-13) | 零散小问题(原 14 项,已修复 5 项,剩 9 项) | P2 | 低 | 单项影响小,攒多了拖慢整体质量 |
| [AR-17](anti-patterns.md#AR-17) | QUERY_ALL_PACKAGES 用途申报缺档(VIEW 残留已删) | P2 | 低 | 上架/更新审核时容易返工 |
| [AR-18](anti-patterns.md#AR-18) | 杂项隐患（死代码、错误丢栈、HTTP 状态不查等） | P2 | 低 | 单个影响小，攒多了拖慢排障 |
| [AR-20](architecture.md#AR-20) | 常量仓库名不副实 + 跨功能引用 + 同名异义类 | P2 | 低 | 常量寄生在 home 包；两个 FingerprintDataSource 容易导错 |
| [R11](anti-patterns.md#R11) | 「License」行只链 toml，合规不到位 | P2 · 🟢 | 低 | 许可文本应该让人能方便看到 |
| [C4](rulings.md#C4) | 拖拽中重建吞掉下拉手势 | P1 · 🟡 | — | 已裁定永久接受（决策⑤）；显示过期的一半由 AR-08 纯推导重构修好 |
| [R1](rulings.md#R1) | 工具栏不随滚动隐藏（FR-12） | P2 · 🟡 | — | 并进 Compose 迁移（决策④） |
| [R2](rulings.md#R2) | 无 NavigationRail / 自适应布局 | P2 · 🟡 | — | 同上 |
| [R10](rulings.md#R10) | 滚动条「可拖动」是 stub | P2 · 🟢 | 低 | 先留着不删（2026-09-12 改口）；fast-scroll 归 Compose（决策③） |
| [R12](rulings.md#R12) | 命名漂移（Interface/Display 等） | P2 · 🟢 | — | 已裁定跳过（2026-09-12）：strings 是人工写+人工翻译的 |
| [A2](rulings.md#A2) | 无 foss「不含 Firebase 类」防护 | P1 · 🟡 | — | 已裁定关闭（2026-09-12）：现状即正解 |
| [A4](rulings.md#A4) | 无 root/Shizuku 层 | P2 · ℹ️ | — | 不是缺陷；目录 techniques 列如实记录 |
| [A7](rulings.md#A7) | 构建脚枪（isPreview 开关、硬编码版本等） | P2 · 🟢 | — | 已裁定零改动（2026-09-12），见条目处置 |

## 总体诊断

项目表面上分了四层(Fragment → ViewModel → Repository → DataSource),但**层和层之间传的东西类型不对**:从 DataSource 一路到 RecyclerView,传的都是 `MyModel`——一个已经拼好翻译文案、带好主题色资源 ID 的**界面模型**。于是:

1. 仓库(本该只管取数和判断)必须拿着 Context 才能干活,`MyApplication.getMyString` 全项目被调了 **100 次**(AR-01、AR-04)。
2. 因为没有可观察的领域数据,跨页面同步只能靠 ViewModel 伴生对象里的静态 SharedFlow 当全局广播站(AR-02)。
3. 因为没有注入的渠道,全局可变单例成了默认选项:`myAndroid`、`PropertyManager.instance`、`ShellManager.instance`、`LldManager`(AR-03、AR-04)。

「高内聚低耦合」的判断标准是:**改一处,只动一个文件**。现在这个项目里,加一个设置项要动 5 个文件(preferences.xml、SettingsFragment、SettingsViewModel 伴生对象、BaseListFragment 或目标 Fragment、目标 ViewModel/Repository),加一个首页检测条目要动 2~3 个文件(HomeViewModel、HomeRepository,还得小心列表下标)。上面这些架构问题大多不会让 App 直接崩,但会让每次改动的心理负担越来越重。除了架构和 SSOT/UDF 的视角,这次检测还确认了三处会直接伤到用户的风险:网络数据 lld.json 的日期格式没有边界校验,上游一改格式、联网用户就崩(AR-15);Prop 页一次加载要做几百次串行的跨进程查询(AR-14);没有任何真实测试、CI 只编译不测试(AR-16)。这三项跟分层无关,但跟架构问题同级。

规格对照审计(2026-09-06,并入时逐条重新核对过)补上了最重要的正确性缺口:**逐条错误隔离(FR-6)没实现是发布阻塞**(C1,暂缓中)——Others/Prop 的 `collectModels` 完全没有 try/catch,任何检测抛异常就崩,而且初始加载随启动自动运行,受影响的设备会陷入启动就崩的循环;Home 圆点的 Unknown 常显示成红色,误导非专家用户(C6);不存在三态结果类型和检测器注册表,目录硬编码在 ViewModel 的函数体里(R5)——这是 Compose 迁移前必须补上的主要结构缺口。其余发现见[总览表](#问题总览表)的 C/R/A 行和[附录 A](#附录a-审计出处与编号映射)。

## 已经做对的地方

修复时请保留这些,不要顺手重构掉:

- `BaseListViewModel` 的状态持有与 `loadJob` 去重(提交 `4d570cdb`),用 `StateFlow` + `flowWithLifecycle` 收集,配置变更后不重复加载——这是标准的 UDF 写法。
- `IShell` / `IProperty` 接口抽象 + 委托(`class ShellManager(shell: IShell) : IShell by shell`)——接口有了,坏的只是接线方式(见 AR-04)。
- 按功能分包(`ui.home` / `ui.others` / `ui.prop` / `ui.settings`),资源跟包走(`app/build.gradle.kts` 的 sourceSets 配置)。
- `MainActivity` 用 `MainViewModel` + `SavedStateHandle` 记住上次选中的 Tab。
- `MyModelTitle` 用密封接口区分「资源标题」和「原始标题」,`MyAdapter` 的 DiffUtil 按 `key` 判断是不是同一条。

## 修复路线(2026-09-13 改版:架构优先)

> 2026-09-13 负责人定:①业务逻辑类修复(行为修正、C1 逐条错误隔离这类)暂缓,集中攻克架构问题、反模式、SSOT、UDF;②改动尽可能**一步到位**——方案直奔最终形态,不做「先止血、以后重构替换」的两步走,真实存在的前置依赖照旧讲清;③交付周期不作为排序依据。原批次路线图(第四分册)就此退役,仍有行动价值的裁定已并入各条目正文与本节。

主力线(有先后依赖,串行;每步先出方案拆解再动手):

1. **AR-02 设置改 SSOT**:设置改用 DataStore + Flow(`SettingsStore`),删静态事件总线——后续「界面自动跟着设置变」的地基。
2. **AR-08 Home 改纯推导**:`combine(settings, sources, refresh)` 自动合成列表,删手工编排 20+ 仓库方法与按下标打补丁的机制(C5 补丁链一并删除;C4 显示过期好一半)。
3. **AR-03 `myAndroid` 改不可变**:全局可变单例只允许初始化一次(独立小件,穿插做)。
4. **三态结果类型**(原批次 5a):检测结果定为「正常/警告/异常/未知」,未知绝不显示成红色(C6)。开工前需负责人定 **Q9:三态 → 圆点颜色映射**。
5. **注册表引擎**(原批次 5b):每个检测变成清单里的一条数据(含执行函数),引擎统一执行——并发有上限(A3)、可取消、展示顺序固定。**这一步同时是 AR-01「没有领域模型」的落地**:检测结果从界面模型换成领域对象,显示层自己决定怎么渲染。
6. **目录核对 + AR-19**(原批次 5c):界面条目目录与注册表逐条对上(Q1 目录形态届时定);「拿翻译文案当判断值」的坏模式(AR-19)随新结构消除。

穿插件(不占主线):AR-05 + AR-04 对象创建集中到一处(完整的依赖注入框架仍是 Phase 2 的事);AR-06 的 Activity 反向依赖小修(容器 insets 留给 Compose,不做两遍)。测试基建(AR-16/A1:JUnit 5 + MockK + Turbine)在大重构开工前落地,穿插进行。AR-07 包结构搬移放最后——大面积路径改动会污染前面每一步的 diff。

暂缓(业务逻辑类,负责人 2026-09-13 定):C1 逐条错误隔离(已实现过一轮,撤销后存档于 git stash)、R11 许可合规、AR-14/AR-15 等行为与健壮性条目;R12/A7/C4/AR-13.13/AR-13.6 等已有「不动」裁定的维持原判(见 [rulings.md](rulings.md))。

各问题详情见五个分册。

<a id="appendix-a"></a>

## 附录 A · 审计出处与编号映射

**出处**:C/R/A 系列编号来自 2026-09-06 的规格对照审计(拿规格文档逐条核对 develop 分支代码,35 项发现;并入本目录时已于 2026-09-11 逐条重新核对,只保留仍然成立或仍有行动价值的条目)。审计引用的 SSOT/UDF 报告(17 项内部缺陷)和逐条目错误处理任务记录没有存进仓库,其内容已由 AR 系列和本表承载。

**与 AR 系列重叠的条目**(不重复记录):

| 原编号 | 对应条目 | 备注 |
|---|---|---|
| C2 | ~~AR-13.11~~ | 五处裸 `catch (e: Exception)` 吞掉 CancellationException;**已修复**(2026-09-12,`2bd6772d`),条目已删 |
| C3 | [AR-03](ssot.md#AR-03) | `myAndroid` 全局可变单例;随 AR-03 不可变化解决 |
| C7 | S/U/O 系列(见下表) | 除 U4(已修复)外全部还开着 |
| A1 | [AR-16](architecture.md#AR-16) | 零有效测试;先补最小安全网(测试基建) |
| A5 | [AR-07](architecture.md#AR-07) | 模块形态只满足了一半 P4——`LldManager` 混住在 `ui/common`、`:base` 是个什么都装的大杂烩;随 AR-07 搬移和 Phase 2 拆分解决 |
| A8 | [AR-02](ssot.md#AR-02) | 设置不可观察是 C4/C5 的根子;AR-02/AR-08 重构一并解决 |

**已裁定关闭、不用展开的条目**:

| ID | 结论 |
|---|---|
| R3 | 已裁定(2026-09-06):`singleTop` 语义 + 遗留标志组合的风险记为 BL-8(暂缓)。MAIN+VIEW 合并 intent-filter 里的 VIEW 残留已修复(2026-09-12,`5c24c742`);[AR-17](anti-patterns.md#AR-17) 现在只余 QUERY_ALL_PACKAGES 申报缺档。 |
| R4 | 已裁定:Others/Prop 是纯展示、没有判定圆点——代码里藏着的这个行为**就是**规格要的行为,不算 bug。 |
| R6 | 已裁定:内置 json 是普通 asset,「使用」=复制进应用私有目录;规格已修订,不用改代码。 |
| R8 | 已裁定:拉取主机按时区选、URL 写死构建时的分支——已写进 FR-16,过期的风险进了规格风险表,不用改代码。 |
| R9 | **已修复**(2026-09-12,`ed8c161f`+`30e00f08`):商店页 URI 改编译期变体资源覆盖(foss → GitHub,firebase → Play);时区分支按负责人新需求删除;条目已删。 |
| R15 | Q10 已用此证据关闭(2026-09-12):「优先包名排序」开关只重排 Home 的过期 targetSdk 应用列表(`HomeRepository.kt:1128-1144`)。 |
| D1~D5 | 都是原英文规格文档自己的毛病(编号错乱、死链、✅ 语义、目录计数不符)。英文 spec 已删除、中文重排版([docs/spec-cn](../../spec-cn/README.md))已修好编号和死链;目录计数不符(Home 23 个调用 → 实际 25 行:安全补丁和 Treble 兼容各产 2 行;Others 是 36 固定 + API 条件项 + N 个分区指纹动态行;Prop 5 个检测器各产动态行)由注册表落地时的目录核对一并解决。 |

**SSOT/UDF 报告(S/U/O 编号)到 AR 的映射**:

| 原编号 | 对应 AR | 内容 |
|---|---|---|
| S1 / U1 / U3 | [AR-02](ssot.md#AR-02) | 设置绕开唯一数据源、静态事件总线、四处直接读 prefs |
| S2 | ~~AR-13.1~~ | `MainViewModel.lastId` 状态有两份;**已修复**(`50e4a503`),条目已删 |
| S3 | [AR-03](ssot.md#AR-03) | `myAndroid` 可变单例 |
| S4 | [AR-04](architecture.md#AR-04) / [AR-07](architecture.md#AR-07) | `LldManager` 单一入口化、搬出 `ui/common` |
| S5 | ~~AR-10~~ | `mounts` 惰性缓存;**已修复**(`9c47f422`),条目已删 |
| S6 | [AR-13.12](anti-patterns.md#AR-13) | `detectWebView` 变量拼写(真机显示 bug);**已裁定关闭**——只影响 Android 6,minSdk 升 24 后成死代码 |
| S7 | [AR-13.13](anti-patterns.md#AR-13) | 四个没用上的 SavedStateHandle;已裁定保留 |
| U2 / U8 | [AR-08](udf.md#AR-08) | 按下标寻址的状态补丁 |
| U4 | — | 已修复(早期批次),条目已删 |
| U5 | [AR-13.10](anti-patterns.md#AR-13) | 版本行点击事件 |
| U6 | [AR-06](architecture.md#AR-06) | insets 避让(归 Compose 迁移) |
| U7 | ~~AR-13.14~~ | version 加载幂等护栏;**已修复**(`c9017cea`),条目已删 |
| O1 | [AR-13.9](anti-patterns.md#AR-13) | 绑定期解析 / 冻结文案(归 Compose 迁移) |
| O2 | [AR-04](architecture.md#AR-04) | `MyApplication.instance` 全局入口 |
| A.8 | [C5](udf.md#C5) | 排序收集器 |
| A.6.6 | [C5](udf.md#C5) | `loadJob` join 的 Loading 守卫竞态——随 C5 推迟到 Compose 迁移后(2026-09-12,View 版实现存档仅参考) |

---

返回 [docs 门户](../../README.md)
