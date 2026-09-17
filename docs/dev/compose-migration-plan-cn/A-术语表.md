# A · 术语表

> 所属迁移计划：[README](README.md) · 上一章：[A·API 速记手册](A-API速记手册.md)

按"英文原词 | 字面义 | 本计划语境义"组织，末列为其**首次系统讲解**的章节（更早处可能顺带提过）。新词先查这里，正文不用来回翻。

## 核心机制

| 英文 | 字面义 | 本计划语境义 | 首讲 |
| --- | --- | --- | --- |
| Composable（`@Composable`） | 可组合的 | 带 `@Composable` 注解的函数，描述"状态长什么样时界面长什么样"；只能被其他 Composable 调用，共同组成"组合树"（对应原 View 树） | 02 |
| Composition | 组合 | 执行 Composable 函数、构建组合树的过程 | 02 |
| Recomposition | 重组 | 状态变化后重新执行受影响的 Composable 函数、更新界面 | 02 |
| Skippable / skipping | 可跳过 | 参数没变的 Composable 调用被框架跳过不重新执行（性能优化核心） | 02 |
| Stable / Unstable | 稳定/不稳定 | 编译器能否断定"类型不会偷偷变"。`data class` 全稳定字段 → 稳定；`List` 接口 → 不稳定 | 02 |
| State | 状态 | 会随时间变化、变化会触发重组的数据（`mutableStateOf` / `StateFlow` 两种来源） | 02 |
| State hoisting | 状态提升 | 子组件不持有状态，状态上移到调用方，子组件只收"值 + 事件回调" | 02 |
| Unidirectional Data Flow (UDF) | 单向数据流 | 状态只向下传、事件只向上抛的架构约束 | 02 |
| Side effect | 副作用 | 在组合函数里做的"不纯粹"的事（开协程、收流、弹 Toast），必须装进 `LaunchedEffect` 等容器 | 02 |
| CompositionLocal | 组合局部量 | 沿组合树隐式向下传的"环境值"，`MaterialTheme.colorScheme` 就是内置款 | 02 |
| `remember` / `rememberSaveable` | 记住 | 把局部值保过重组（`rememberSaveable` 还保过进程重建，自动进 Bundle） | 02 |
| Phases（Composition / Layout / Draw） | 组合/布局/绘制三阶段 | Compose 刷新的三层流水线；把读取推迟到后面的阶段 = 更少的计算（滚动条自绘用到） | 06 |

## 组件与样式

| 英文 | 字面义 | 本计划语境义 | 首讲 |
| --- | --- | --- | --- |
| Modifier | 修饰符 | 链式追加的界面属性（`padding`/`background`/`clickable`…），顺序敏感、**叠加**语义 | 02 |
| Style（API） | 样式 | foundation 1.11 起的新范式：只管视觉属性的对象，**后写覆盖**语义，可按状态条件化并自带动画 | 04 |
| Slot（槽位参数） | 槽 | 组件预留的"你自己填内容"参数（`AlertDialog.text`、`ListItem.trailingContent`），值是 `@Composable` lambda | 08 |
| Scaffold | 脚手架 | M3 页面骨架组件，自动处理顶栏/底栏/系统栏避让，产出 `innerPadding` | 09 |
| LazyColumn | 懒加载纵向列表 | 只组合可见区、离屏即丢的列表，RecyclerView 的声明式替身 | 02 |
| `key`（列表） | 键 | 条目的稳定身份证，增删挪移时对上号；对应旧 DiffUtil 的 `areItemsTheSame` | 02 |
| Content type（列表） | 内容类型 | 条目分类提示（对应旧 viewType），便于复用同类条目的组合结果 | 05 |
| `PersistentList` | 持久化列表 | kotlinx.collections.immutable 的不可变列表，Compose 判稳定，替代 `List` 传给 UI | 02 |
| Inset / WindowInsets | 内嵌区 | 系统栏（状态栏/导航栏/刘海）遮住的区域；Compose 里 `WindowInsets.xxx` 声明式处理 | 05 |
| Edge-to-edge | 边到边（全屏铺满） | 内容延伸到系统栏后面，再自己避让 | 09 |
| Predictive back | 预测性返回 | Android 14+ 的"返回前先预览目标"手势动画，NavDisplay 内建支持 | 09 |
| Dynamic color / Material You | 动态取色 | Android 12+ 按壁纸生成的主题色，`dynamicLightColorScheme()` | 01 |
| M3 Expressive | （Material 3 Expressive） | Material 3 的新一代视觉语言（弹性动效、新组件形态）；对应 API 在 material3 1.5 线 | 10 |

## 导航与架构

| 英文 | 字面义 | 本计划语境义 | 首讲 |
| --- | --- | --- | --- |
| Navigation 3（Nav3） | 导航 3 | 2025 年底 stable 的新导航库：目的地 = 可序列化的键，返回栈 = 状态 | 09 |
| NavKey | 导航键 | 标记接口 + `@Serializable`，四个标签页即四个键 | 09 |
| Back stack | 返回栈 | "按过哪些页面"的记录列表，可以回退 | 09 |
| Multiple back stacks | 多返回栈 | 每个顶层标签一条独立返回栈（官方配方），切换标签互不清空 | 09 |
| NavEntry | 导航条目 | 键 + 元数据 + 内容三件套，NavDisplay 渲染的单位 | 09 |
| Entry decorator | 条目装饰器 | 给条目附加能力的插件（保存状态、ViewModel 作用域） | 09 |
| Interop（interoperability） | 互操作性 | View 体系和 Compose 体系互相嵌入（`ComposeView` / `AndroidView` 两座桥） | 02 |
| `ComposeView` | — | View 树里嵌 Compose 内容的容器（迁移期第 2~5 步的主力桥） | 05 |
| ViewModel scoping | ViewModel 作用域 | VM 存活范围绑定到谁（Fragment → Nav3 条目）；作用域在，VM 在 | 09 |
| SavedStateHandle | 已保存状态句柄 | ViewModel 里存"进程被杀后要恢复"的小数据的官方通道 | 09 |
| BOM（Bill of Materials） | 物料清单 | Compose 的版本对齐机制：引 BOM，各库版本自动配套 | 03 |
| Flavor | 风味/口味 | 构建语境里指"构建变体"（本项目的 Foss / Firebase 两种发行包） | 08 |
| Factory / CreationExtras | 工厂 / 创建附加项 | ViewModel 的定制构造通道（本计划第 6 步将其简化为工厂内直接构造仓库） | 05 |
| SharedFlow | 共享流 | 一对多的事件广播（设置页改动 → 各列表页响应），与 UI 框架无关，迁移后原样保留 | 07 |
| `@Preview` | 预览 | 给组件配的"假数据渲染"函数，IDE 里实时看效果（替代 XML 的 `tools:`） | 02 |
| RC / alpha / stable | 候选发布/阿尔法/稳定 | 软件成熟度阶梯：RC≈准正式、alpha≈早期试验、stable≈正式 | 06 |
