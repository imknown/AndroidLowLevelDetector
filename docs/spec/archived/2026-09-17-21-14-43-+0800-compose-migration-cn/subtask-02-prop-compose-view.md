# ST-02: prop-compose-view 修改计划 (补录)

> 状态: record (由目录位置声明). 计划依据:  
> 计划 05 章 "第 2 步 PropFragment 接入 ComposeView"  
> (已删; 幸存版见 `git show 9c91c8cc^:docs/dev/compose-migration-plan-cn/05-第3步-交互补齐.md` 的邻章与观察记录). 风险: 低.

## 计划 (当时的拆分)

PropFragment 换壳为 ComposeView 宿主, 即第一个能跑的 Compose 屏; 顺势立起三个列表页共用的屏幕组件 (`MyModelListScreen`) 与 `LazyColumn` /  
`collectAsStateWithLifecycle` / `produceState` 教学.

## 实际落地

- 327e6b29: PropFragment → ComposeView, 经共用的 `MyModelListScreen` 装配, 共用件设计即在此定型, 之后的 Home / Others 直接复用.

## 落地差异与等价性

评审核实后接受的偏差 (观察记录 2026-09-19, 三处):

1. **`rememberBottomBarHeight` 经 `LocalView.current.context` 取宿主**:  
   Fragment 的 `requireContext()` 就是宿主 Activity 本身, cast 成立; 属过渡期桥接, 第 6 步换 Scaffold 后整体删除 (149f9f94 兑现).
2. **底栏高度只在首次布局量一次**: 首帧 padding 为 0, 布局后跳真值, 旧代码同款行为.
3. **LazyColumn 遇重复 key 直接抛异常** (旧 DiffUtil 只是行为怪异不崩): Compose 侧更严格的一处; 当前三个列表页数据源无 key 碰撞,  
   后由 [issues-cn #20](../../../dev/issues-cn/archived/05-稳定性与错误处理/20-列表key重复崩溃.md) 独立跟踪.

`produceState` 防闪空的教学设想未进入代码, 防闪空由 `BaseListViewModel` 的 "刷新保留旧列表" 语义承担 (AGENTS.md 现行表述的来源).

现存等价性注释: `MyModelListScreen.kt:57` (`init()` 镜像 legacy `BaseListFragment` 尾调), `:70` (mid-refresh 补丁直达 UI),  
`:117` (背景对齐 `base_list_fragment.xml` 的 `colorSurfaceContainer`), `:123` / `:143` (ItemDecoration 四边距),  
`:130` (DiffUtil `areItemsTheSame` 同款 key), `:132` (viewType 同款 hint).

## 评审与更正

dfe23c62 (step-2 等价性说明 + 数据初始加载时机 follow-up).

## 遗留

数据初始加载时机: `BaseListViewModel` 是 UI 触发型 (`LaunchedEffect` → `init()`, 幂等 + loadJob 去重),  
属 Ian Lake 文章定义的两种反模式之一; 观察记录裁定 "现在不动" (本地 prop 毫秒级, 惰性化收益趋零), **接网络/数据库时按 `WhileSubscribed(5_000)` 模式重造**.  
现登记为 [issues-cn #61](../../../dev/issues-cn/archived/03-UDF-单向数据流/61-数据加载惰性化.md).

## 证据

- 提交: 327e6b29, dfe23c62; 桥接兑现: 149f9f94.
- 代码: `app/src/main/java/net/imknown/android/forefrontinfo/ui/base/list/MyModelListScreen.kt`.
