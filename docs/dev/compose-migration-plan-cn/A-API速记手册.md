# A · API 速记手册

> 所属迁移计划：[README](README.md) · 上一章：[A·API 速查表](A-API速查表.md) · 下一章：[A·术语表](A-术语表.md)

[速查表](A-API速查表.md)回答"用的时候去哪查"（签名/版本/链接），本手册回答"**怎么记、怎么分辨**"（规律/家族/口诀/坑）。学完每步回来过一遍口诀，最后做自测。

## 1. 记忆总纲：API 名字就是说明书

Compose 的 API 命名极其规律，掌握五条，见到新 API 能猜个八九不离十：

| 规律 | 例子 | 一句话 |
| --- | --- | --- |
| **动词开头的 Modifier** | `padding` `background` `clickable` `clip` | 修饰符 = 动作，读链 = 穿衣服（从外到内） |
| **`remember` 开头 = 要长期持有的东西** | `rememberLazyListState` `rememberPullToRefreshState` `rememberNavBackStack` | 后面跟的是"什么仪表盘" |
| **`xxxAsState` = 转换成状态** | `collectAsStateWithLifecycle` | 把"外界的值"变成"界面认的状态" |
| **`Local` 开头 = 环境变量** | `LocalContext` `LocalDensity` `LocalExtendedColors` | 不传参、子树全可读 |
| **`-able` 结尾 = 能力插件** | `styleable`（可样式化的） | 挂上去就获得该能力 |

再加一条 M3 专属：**组件 + `组件名 Defaults` 成对出现**——想改默认值，先找 `CardDefaults` / `TopAppBarDefaults` / `NavigationBarItemDefaults`。

## 2. API 家族树（按"户口"分组）

### 2.1 状态家族（androidx.compose.runtime / androidx.lifecycle.compose）

```
状态的三个来源
├── 自己造：mutableStateOf（写触发重组，读登记依赖）
├── 从 Flow 收：collectAsStateWithLifecycle（收快递：家里有人[STARTED 以上]才收）
└── 从普通值"腌"：produceState(initial, key)（腌罐头：key 一变重腌，没腌成保留旧罐）
记住（remember 家族）
├── remember            → 草稿纸：抗重组，不抗重启
├── rememberSaveable    → 档案袋：系统帮你存 Bundle，抗旋转/进程重建
└── rememberXxxState    → 各类"仪表盘"（列表滚动、下拉进度、导航栈……）
```

### 2.2 副作用家族（androidx.compose.runtime）

```
副作用容器（都是"进组合登记、离组合销户"）
├── LaunchedEffect(key)   → 值日生：进门干活（协程），钥匙换了重干
├── DisposableEffect(key) → 值日生 + 离开前打扫（dispose 清理监听器等）
└── SideEffect            → 每次重组后同步执行（少用，向 View 世界发布状态时用）
```

**分辨**：要开协程/收流 → `LaunchedEffect`；要注册/反注册成对操作 → `DisposableEffect`。

### 2.3 布局与列表家族（androidx.compose.foundation）

```
容器三积木：Column（柱子·竖） Row（一行·横） Box（盒子·叠）
列表：LazyColumn
├── items(list, key, contentType) → key=身份证，contentType=viewType
├── contentPadding   → 随内容滚动（= clipToPadding=false 的 RV padding）
├── spacedBy         → 条目间距（= ItemDecoration）
└── rememberLazyListState → 仪表盘（滚动条/滚动定位都从这读）
```

### 2.4 M3 组件家族（androidx.compose.material3）

```
一个页面（Scaffold = 带凹槽的托盘）
├── topBar: TopAppBar（顶栏）
├── bottomBar: NavigationBar（底栏）→ 里面排 NavigationBarItem
└── 内容区拿 innerPadding 自己避让
行与卡片：ListItem（headline/supporting/trailing 三槽） Card（1dp 阴影）/ElevatedCard(3dp)/OutlinedCard(描边)
输入与反馈：Switch（哑组件） RadioButton AlertDialog（title/text/confirmButton 槽位）
下拉刷新：PullToRefreshBox（收 isRefreshing 状态、发 onRefresh 事件；指示器可换）
```

### 2.5 主题与导航家族

```
主题 = 一组环境变量
├── MaterialTheme.colorScheme.xxx（= ?attr/colorXxx）
├── MaterialTheme.typography.xxx（语义字号线）
└── 自建：@Immutable 数据类 + staticCompositionLocalOf + CompositionLocalProvider
Navigation 3 = 键 + 栈 + 映射 + 渲染器
├── @Serializable data object XxxKey : NavKey   （目的地 = 键）
├── rememberNavBackStack(key)                   （可存档返回栈 = 浏览器标签页历史）
├── entryProvider { entry<键> { 界面 } }         （键 → 界面映射）
├── NavDisplay(entries, onBack)                 （渲染 + 返回手势）
└── 装饰器：SaveableStateHolder(气泡膜·存状态) + ViewModelStore(小仓库·存 VM)
```

## 3. 易混对照表（背口诀）

| 易混对 | 口诀 |
| --- | --- |
| `remember` vs `rememberSaveable` | 草稿纸 vs 档案袋——**要过旋转/重启就存档** |
| `collectAsState` vs `collectAsStateWithLifecycle` | 快递 vs **家里有人**才收的快递——默认用后者 |
| `LaunchedEffect` vs `repeatOnLifecycle` | 值日生只认**组合**（STOPPED 不管）vs 严格认**生命周期** |
| `Modifier.padding` vs `contentPadding`（LazyColumn 参数） | 外距 vs **随内容滚动的内衬**（= RV 的 clipToPadding=false） |
| `Modifier.background` vs `Modifier.clip` | 上色 vs 裁形——**先裁形再上色**，反了色溢出 |
| `Card` vs `ElevatedCard` vs `OutlinedCard` | 1dp 阴影 / 3dp 阴影 / 描边无阴影（View 时代 Filled ≈ Card + 阴影归零） |
| `mutableStateOf` vs `MutableStateFlow` | 界面私有小状态 vs **协程世界的广播**（VM 里用） |
| `state`(组合读) vs 绘制阶段读 | **变得越勤、读得越晚**——动画数据在 `drawWithContent` 里读 |
| `Modifier` vs `Style` | **穿衣服（叠加）** vs **换衣服（后写覆盖）** |
| `viewModel()` 在 Fragment vs 在 Nav3 entry | 作用域 = 宿主——Fragment 一间、每条导航条目一间 |
| `stringResource` vs `MyApplication.getMyString` | 组合内标准读法（随语言刷新）vs 全局单例读法（新代码别用） |
| `WindowInsets`(手动避让) vs `Scaffold innerPadding` | 过渡期手算 vs **脚手架代劳**（终态） |

## 4. 核心 API 速记卡（一行口诀 + 一行坑）

| API | 口诀 | 坑 |
| --- | --- | --- |
| `@Composable` | 界面函数的"工牌" | 只能在组合里调用；别在后台线程调 |
| `mutableStateOf` | 写会广播、读会登记 | 忘了 `by` 委托就要 `.value` 到处跑 |
| `produceState` | 腌罐头：key 变重腌 | 忘传 key = 块只跑一次，之后不再更新 |
| `LaunchedEffect` | 值日生，钥匙变重干 | 写在函数体里 = 每次重组都执行 |
| `collectAsStateWithLifecycle` | 家里有人才收的快递 | 不带 Lifecycle 的版本后台白重组 |
| `Modifier.padding/fillMaxWidth` | 穿衣服，从外到内 | 顺序换 = 点击区域/背景范围变 |
| `Column/Row/Box` | 柱/行/盒 | `Box` 忘 `align` = 全堆左上角 |
| `LazyColumn items(key=)` | 身份证必带 | 无 key：动画乱、状态串门 |
| `contentPadding`+`spacedBy` | 内衬+隔断 | 想让边缘也留白用 contentPadding，不是 padding |
| `Card` | 默认带 1dp 阴影 | 要 0 阴影显式 `elevation=0`（View Filled 卡无阴影） |
| `MaterialTheme.colorScheme.x` | 语义色 = ?attr | 硬编码 Color.Red 是主题杀手 |
| `CompositionLocalProvider` | 发环境变量 | `static` 版适合不常变的值（主题） |
| `@Immutable` | 给编译器打包票 | 藏 var 的"假不可变"会出幽灵数据 |
| `PersistentList` | 拍照的名单 | 普通 `List` 传 UI = 整屏不跳过 |
| `stringResource(id, args...)` | 组合内读资源 | remember 的 lambda 里调它 = 编译错 |
| `@Preview` | 样品间 | 依赖 ViewModel/网络 = 预览崩 |
| `PullToRefreshBox` | 只翻译手势、不刷新数据 | 忘喂 `isRefreshing` = 转圈不停/不转 |
| `drawWithContent` | 只换贴纸不动家具 | 忘调 `drawContent()` = 原内容消失 |
| `ScrollIndicatorState` | 滚动仪表盘 | 组合里读 scrollOffset = 滚一下重组一屏 |
| `Scaffold(innerPadding)` | 托盘的凹槽 | 拿了 padding 不用 = 内容顶到栏底下 |
| `ComposeView` + strategy | View 楼里租教室 | Fragment 里忘设 DisposeOnViewTreeLifecycleDestroyed = 泄漏 |
| `NavKey @Serializable` | 目的地是键不是页 | 忘序列化 = 返回栈没法恢复 |
| `entryProvider{ entry<T>{} }` | 键→界面电话簿 | 别用 2025 旧名 rememberNavEntryProvider |
| `NavDisplay(entries, onBack)` | 渲染栈顶+接返回 | entries 重载必须给 onBack |
| 两个 Nav3 装饰器 | 气泡膜+小仓库 | 少任何一个：状态丢或 VM 串作用域 |
| `viewModel()`（entry 内） | 在哪拿、归哪管 | 顶层拿了传进去 = 作用域错位 |
| `AlertDialog` 槽位 | title/text/confirm 各塞各的 | confirmButton 必填（可放取消按钮） |
| `ListItem` 三槽 | 主行/副行/尾部 | 想整行可点要自己加 `clickable` |
| `Switch(checked, onChange)` | 哑开关 | 自己 remember 状态 = 外面不知情 |

## 5. 自测十二题（先答再展开答案）

<details><summary>1. 为什么 <code>items</code> 必须给 <code>key</code>？不给会坏什么？</summary>

key 是条目的"身份证"（= 旧 DiffUtil.areItemsTheSame）。不给时框架按位置认条目：增删导致动画错乱，条目内局部状态（输入框等）会"串"到别的数据上。
</details>

<details><summary>2. <code>remember</code> 与 <code>rememberSaveable</code> 分别抗什么、不抗什么？</summary>

remember 抗重组（草稿纸）；rememberSaveable 额外抗配置变更与进程重建（档案袋，存进 Bundle）。都随"离开组合"而失效。
</details>

<details><summary>3. <code>Modifier.padding(16.dp).clickable { }</code> 与反序有什么区别？</summary>

穿衣服从外到内：先 padding 后 clickable → 边距区不可点；先 clickable 后 padding → 边距区也能点。背景同理（颜色是否延伸到边距）。
</details>

<details><summary>4. 刷新时如何做到"列表不闪空"？本计划用的哪个 API？</summary>

produceState：key（state）变化重跑块，Loading 时不赋值 → 保留最近一次 Done 的数据（腌罐头保留旧罐）。
</details>

<details><summary>5. 为什么滚动条读数要放在 <code>drawWithContent</code> 里而不是组合里？</summary>

变得越勤、读得越晚：滚动值每帧变，组合阶段读会让整列每帧重组；绘制阶段读只重画一层（零重组零重排）。
</details>

<details><summary>6. <code>LaunchedEffect</code> 收 SharedFlow 与旧 <code>flowWithLifecycle</code> 的差别在哪？需要精确等价怎么办？</summary>

LaunchedEffect 只认"离开组合"才取消，STOPPED 不暂停；flowWithLifecycle 默认 STARTED 门槛。精确等价用 repeatOnLifecycle(STARTED){ flow.collect{} }。
</details>

<details><summary>7. 子组件想要开关状态，参数该怎么设计？</summary>

状态提升：`MySwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit)`——数据进、事件出；状态本身住在调用方。
</details>

<details><summary>8. <code>Card</code> 与 XML 的 Filled 卡在阴影上的差别？怎么归零？</summary>

Compose Card 默认 1dp 阴影；XML Widget.Material3.CardView.Filled 是 0。显式 `elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)`。
</details>

<details><summary>9. Nav3 里"每个标签一条返回栈"对应旧行为的什么？靠哪两个装饰器保住状态与 VM？</summary>

对应四个 Fragment 建一次后 show/hide（切标签不清状态）。SaveableStateHolder 装饰器保 rememberSaveable 状态（气泡膜）；ViewModelStore 装饰器给条目独立 VM 仓库。
</details>

<details><summary>10. 自定义 <code>PullToRefreshBox</code> 的指示器时，为什么必须自备 <code>rememberPullToRefreshState()</code>？</summary>

指示器要读手势进度（distanceFraction），进度住在 state 里；自备后要同时传给 PullToRefreshBox(state=) 和 Indicator(state=)，两边共用同一个仪表盘。
</details>

<details><summary>11. 偏好读写"三段式"是哪三段？为什么不能每次重组都读 SharedPreferences？</summary>

进屏读一次（remember）→ 本地状态为真源 → 变更写穿（edit().apply() + 副作用）。组合函数要保持纯：读盘只该发生在初始化与事件回调里。
</details>

<details><summary>12. Style API 与 Modifier 最本质的语义差别？本计划为什么只在一处试水？</summary>

Modifier 叠加（穿衣服）、Style 后写覆盖（换衣服）+ 状态动画内置 + 只走布局/绘制阶段。foundation 1.13 将重构旧 Style 实现（废弃移除）且 M3 组件尚未开放 style 参数，故单文件试水、锁死改动面。
</details>

---

全答对了？回去把 [02 概念速成](02-Compose核心概念速成.md)的白话和锚点再扫一遍，然后开工 [03 第 0 步](03-第0步-构建准备.md)。答不上来的，回到对应章节看 ✅/❌ 对照。
