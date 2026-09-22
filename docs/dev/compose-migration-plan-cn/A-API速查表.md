# A · API 速查表

> 所属迁移计划：[README](README.md) · 上一章：[10 第 7 步 清理收尾](10-第7步-清理收尾.md) · 下一章：[A·API 速记手册](A-API速记手册.md)

本计划用到的全部 API，按"包"分组，标注版本与稳定性。 调研日期 2026-09-16，全部结论来自 developer.android.com 与 androidx 官方仓库，逐条附来源。如何**记忆与分辨**这些 API（规律/口诀/易混对照/自测），见姊妹篇 [A·API 速记手册](A-API速记手册.md)。

## 0. 版本与 BOM 映射（写计划时的官方快照）

| 组件 | 版本 | 状态 |
| --- | --- | --- |
| Kotlin / Compose 编译器插件 | 2.4.20 | stable |
| Compose BOM | 2026.09.00 | — |
| ↳ androidx.compose.ui / foundation | 1.12.1 | stable |
| ↳ androidx.compose.material3 | 1.4.0 | stable（1.5.0-alpha28 预发布中） |
| ↳ material3.adaptive 家族 | 1.3.0 | stable（navigation-suite 工件不在 BOM 内） |
| Navigation 3（runtime/ui） | 1.2.0-rc01 | RC（stable 线为 1.1.7） |
| lifecycle 全家（含 runtime-compose、viewmodel-navigation3） | 2.11.0 | stable（2.12.0-alpha03 预发布中） |
| AGP / Gradle / JDK | 9.4.0 / 9.7.1 / 25 | — |

来源：[BOM 映射表](https://developer.android.com/develop/ui/compose/bom/bom-mapping)、[androidx 版本总览](https://developer.android.com/jetpack/androidx/versions)。注意：material3 的 Expressive 主题 API（`MaterialExpressiveTheme` / `expressiveLightColorScheme`）已从 1.4.0 stable 线**移除**，只在 1.5.0-alpha 线存在（见 §9）。

## 1. 状态与生命周期（androidx.compose.runtime / androidx.lifecycle）

| API | 用途（本计划出处） | 稳定性 |
| --- | --- | --- |
| `StateFlow<T>.collectAsStateWithLifecycle()` | Flow → Compose 状态，跟随生命周期（第 2 步起到处用） | stable，`androidx.lifecycle.compose`，lifecycle-runtime-compose 2.11.0 |
| `produceState(initialValue, key) { }` | 非状态 → 状态，key 变化重跑、否则保留旧值（刷新不闪空） | stable |
| `LaunchedEffect(key) { }` | 进组合启动协程、离组合取消（VM init、SharedFlow 事件） | stable |
| `remember { }` / `rememberSaveable { }` | 重组间/进程重建后保留值（对话框开合、标签页索引） | stable |
| `mutableStateOf` / `mutableIntStateOf` / `getValue`(by) | Compose 状态容器与委托 | stable |

来源：[State](https://developer.android.com/develop/ui/compose/state)、[Side-effects](https://developer.android.com/develop/ui/compose/side-effects)、[lifecycle releases](https://developer.android.com/jetpack/androidx/releases/lifecycle)

## 2. 布局与列表（androidx.compose.foundation）

| API | 用途 | 稳定性 |
| --- | --- | --- |
| `Column` / `Row` / `Box` / `Modifier.padding` / `fillMaxWidth` 等 | 基础布局（替代 LinearLayout/FrameLayout） | stable |
| `LazyColumn { items(items, key, contentType) { } }` | 列表（替代 RecyclerView 四件套） | stable |
| `rememberLazyListState()` | 列表状态（滚动条要读它） | stable |
| `LazyListState.scrollIndicatorState: ScrollIndicatorState?` | 滚动指示器状态：`scrollOffset` / `contentSize` / `viewportSize`（自绘滚动条的原料） | **stable**（foundation 1.10.0 起；`scrollOffset` 标注 `@get:FrequentlyChangingValue`，为绘制阶段读取设计） |
| `Modifier.drawWithContent { }` | 绘制阶段回调（滚动条自绘、性能优化） | stable |
| `WindowInsets.systemBars` / `.only(Horizontal)` / `.asPaddingValues()` | 过渡期的手动 insets（第 6 步后由 Scaffold 接管） | stable |

来源：[compose-foundation releases](https://developer.android.com/jetpack/androidx/releases/compose-foundation)（ScrollIndicatorState 于 1.10.0-alpha02 引入、1.10.0 转 stable）

## 3. Material 3 组件（androidx.compose.material3，版本 1.4.0）

| API | 用途 | 稳定性 |
| --- | --- | --- |
| `Card` / `CardDefaults.cardColors(containerColor=)` | 列表卡片（第 1 步） | stable |
| `Scaffold(topBar, bottomBar) { innerPadding -> }` | 页面骨架 + 自动 insets（第 6 步） | stable |
| `TopAppBar(title)` | 顶栏 | **仍需 `@OptIn(ExperimentalMaterial3Api::class)`**（2026-09-22 实测：摘掉后 `AppRoot.kt` 的 `TopAppBar` 调用点报 `OPT_IN_USAGE_ERROR`，编译失败 —— 本表原记的「stable」不成立）。`TopAppBarScrollBehavior` 同样在 1.4.0 仍需 opt-in（1.5.0-alpha20 起 stable）；本计划不用滚动行为。注：`TopAppBarDefaults.topAppBarColors` 是否单独也触发未分离验证 |
| `NavigationBar` / `NavigationBarBarItem(selected, onClick, icon, label)` | 底部导航 | stable。1.4.0 起选中标签色由 onSurface 改为 secondary |
| `ShortNavigationBar` / `ShortNavigationBarItem` | NavigationBar 的 Expressive 版 | stable（1.4.0-alpha18 毕业），本计划选 NavigationBar 保持视觉延续 |
| `PullToRefreshBox(isRefreshing, onRefresh) { content }` | 下拉刷新（第 3 步） | **stable（1.4.0 起）**；`enabled`/`threshold` 参数仅 1.5.0-alpha15+ |
| `PullToRefreshDefaults.Indicator(state, isRefreshing, containerColor, color)` | 刷新指示器定制配色 | stable（默认：surfaceContainerHigh 底 + onSurfaceVariant 圈） |
| `ListItem(headlineContent, supportingContent, trailingContent)` | 设置页行（第 5 步） | stable |
| `AlertDialog(onDismissRequest, title, text, confirmButton)` | 选择对话框 | stable |
| `Switch(checked, onCheckedChange)` / `RadioButton(selected, onClick)` / `TextButton` / `Text` / `Icon(painterResource(...), null)` | 表单与文案 | stable |
| `stringResource(id, vararg args)` / `stringArrayResource` / `dimensionResource` | 资源读取 | stable |

来源：[compose-material3 releases](https://developer.android.com/jetpack/androidx/releases/compose-material3)（PullToRefresh 稳定于 1.4.0-alpha18；Expressive API 于 1.4.0-beta01 移回 alpha 线）

## 4. 主题（androidx.compose.material3 + runtime）

| API | 用途 | 稳定性 |
| --- | --- | --- |
| `MaterialTheme(colorScheme, typography) { }` | 主题入口 | stable |
| `MaterialTheme.colorScheme.xxx` / `MaterialTheme.typography.xxx` | 语义色/文字样式（`?attr/` 的对应物） | stable |
| `Typography(titleLarge = TextStyle(...))` | 校准语义样式（第 1 步） | stable |
| `staticCompositionLocalOf { }` + `CompositionLocalProvider` | 自建主题扩展（ExtendedColors） | stable |
| `@Immutable` | 稳定性承诺注解 | stable |
| `isSystemInDarkTheme()` / `dynamicLightColorScheme(context)` | 深色判定 / 动态取色（现有 `AppTheme` 已接好） | stable |

来源：[Migrate XML themes to M3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/migrate-xml-theme-to-compose)。官方主题适配器（compose-theme-adapter / M3Theme）**已弃用下线**，本项目自建 `AppTheme` 即官方现行做法。

## 5. 互操作（迁移期桥接）

| API | 方向 | 本计划用处 | 说明 |
| --- | --- | --- | --- |
| `ComposeView` + `setContent { }` | View 树里放 Compose | 第 2~5 步各 Fragment | 需 `activity-compose`（ComponentActivity 系） |
| `ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed` | — | 同上 | 官方为 Fragment 场景推荐 |
| `AndroidView(factory, update, onReset)` | Compose 里放 View | 本计划不用 | Lazy 列表复用需 1.4.0-rc01+ 的 `onReset` 重载 |
| `AndroidFragment<T>()` | Compose 里放 Fragment | 不用（但 Nav3 官方 interop 配方用它做渐进迁移） | androidx.fragment 1.9+ |
| `AbstractComposeView` | 把 Compose 组件封装成自定义 View | 不用 | 双向共享组件时用 |

来源：[Interoperability APIs](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis)、[Migration strategy](https://developer.android.com/develop/ui/compose/migrate/strategy)

## 6. Navigation 3（androidx.navigation3，1.2.0-rc01）

| API | 说明 | 稳定性 |
| --- | --- | --- |
| `interface NavKey` + `@Serializable data object Xxx : NavKey` | 目的地键，可序列化才能存返回栈 | stable |
| `rememberNavBackStack(vararg keys)` → `NavBackStack<NavKey>` | 可保存返回栈（内部 rememberSaveable） | stable |
| `entryProvider { entry<T>(metadata) { content } }` | 键 → 界面的 DSL（`EntryProviderScope.entry`）；旧名 `rememberNavEntryProvider` 等已废弃勿用 | stable |
| `NavDisplay(backStack, onBack, entryDecorators, entryProvider, ...)` | 渲染栈顶 + 系统返回/预测性返回；另有 `entries:` 重载用于多返回栈切换 | stable |
| `rememberDecoratedNavEntries(backStack, entryDecorators, entryProvider)` | 把栈加工成带装饰的可渲染条目（多返回栈配方的核心） | stable |
| `rememberSaveableStateHolderNavEntryDecorator()` | 条目 rememberSaveable 状态的冻结/恢复 | stable |
| `rememberViewModelStoreNavEntryDecorator()` | 条目级 ViewModelStore（entry 内 `viewModel()` 自动按条目作用域）；2.11.0 起提供支持多返回栈的提升重载 | stable |
| `metadata { put(NavDisplay.TransitionKey) { ... } }` | 按条目定制转场动画 | stable（1.1.0 起 metadata DSL） |

来源：[Navigation 3 指南](https://developer.android.com/guide/navigation/navigation-3/get-started)、[save-state](https://developer.android.com/guide/navigation/navigation-3/save-state)、[releases](https://developer.android.com/jetpack/androidx/releases/navigation3)（要求 compileSdk 36+，本项目 37 ✓）、官方示例仓库 [nav3-recipes](https://github.com/android/nav3-recipes)（MultipleStacks 配方即第 6 步的骨架来源）

## 7. Style API（androidx.compose.foundation.style，foundation 1.12.1）

> 定位：**视觉属性专用**（内外边距/背景/边框/形状/阴影/排版/颜色），不管行为；与 Modifier 并存。来源：官方指南 [Styles in Compose](https://developer.android.com/develop/ui/compose/styles) 及其子页（[Fundamentals](https://developer.android.com/develop/ui/compose/styles/fundamentals)、[State and animations](https://developer.android.com/develop/ui/compose/styles/state-animations)、[Styles vs modifiers](https://developer.android.com/develop/ui/compose/styles/styles-vs-modifiers)）。

| API | 说明 |
| --- | --- |
| `Style { contentPadding(10.dp); background(...) }` | 构建样式对象；**同名属性后写覆盖**（非 Modifier 的叠加） |
| `styleA then styleB` | 样式合并，右边覆盖左边同名属性 |
| `Modifier.styleable(styleState, style)` / `styleable(styleState, vararg styles)` | 把样式挂到任意容器（`Added in 1.11.0`，`@ExperimentalFoundationStyleApi`） |
| `remember { MutableStyleState(interactionSource?) }` / `rememberUpdatedStyleState(interactionSource) { }` | 样式状态容器（enabled 等参数变化时用后者） |
| `StyleScope.pressed { } / hovered { } / focused { } / state(key, block, predicate)` | 条件样式（可嵌套：hovered 里再写 pressed） |
| `animate { ... }` / `animate(spring(...)) { ... }` | 状态切换的内置动画（只走布局/绘制阶段） |
| `StyleStateKey(default)` | 自定义状态键（如播放器状态驱动样式） |

**关键事实**：

- 引入于 foundation 1.11.0-alpha06，本 BOM（1.12.1）里为实验性 `@ExperimentalFoundationStyleApi`；
- **1.13.0-alpha03 已宣布重构**：拆为"样式 DSL + 修饰符"（`CustomStyle`/`CommonStyle`/`StyleResolver`/`Modifier.styleResolver`），旧 `Style` 实现将废弃移除——因此本计划只单文件试水（第 1 步 4.4 节）；
- 排版类属性（`textStyle`/`fontSize`/`contentColor`）可被子组件继承，实验开关 `ComposeFoundationFlags.isInheritedTextStyleEnabled`；
- 优先级：直接参数 > `style` 参数 > `Modifier.styleable` 链 > 父级继承；
- material3 组件的 `style` 参数官方预告在未来版本（现仅 foundation 的 `BaseButton`/`BaseText` 等基础组件具备）。

## 8. Kotlin 2.4 要点（本项目相关）

| 特性 | 状态 | 与本计划的关系 |
| --- | --- | --- |
| 上下文参数（context parameters） | **2.4.0 起 stable**（显式传参 `-Xexplicit-context-arguments` 与可调用引用仍实验） | 项目已开实验 flag；非 Compose 层可逐步采用，本计划未强制使用 |
| 显式后备字段（`val x: T field = ...`） | **stable** | 现有 ViewModel 的 `StateFlow` 声明已在用（对外暴露接口类型、字段存实现类型），原样保留 |
| 集合字面量 `["a", "b"]` | 实验（`-Xcollection-literals`，项目已开） | 了解即可 |
| Compose 编译器：模块内部声明的运行时稳定性推断增强；Strong Skipping 为默认 | 2.4.x | `MyModel` + `PersistentList` 组合稳定性的底气（02 章第 2 节） |

来源：[Kotlin 2.4.0](https://kotlinlang.org/docs/whatsnew24.html)、[2.4.20](https://kotlinlang.org/docs/whatsnew2420.html)

## 9. material3 1.5.0-alpha 展望（本计划刻意不采用，升级时按此表跟进）

| API | 说明 | 状态 |
| --- | --- | --- |
| `Modifier.nonInteractiveScrollbar(state, orientation, ...)` | 官方现成滚动条（自带淡出、`colorScheme.outline` 默认色、`NonInteractiveScrollbarDefaults`） | 1.5.0-alpha28 起，视觉专用；转正后一行替换本计划的自绘滚动条 |
| `expressiveLightColorScheme()` / `MaterialExpressiveTheme` | M3 Expressive 主题 | 1.5.0-alpha18 转非实验 |
| `MaterialTheme.motionScheme` | 组件动效方案（`MotionScheme.expressive()`） | 1.4.0 引入、1.5 线逐步毕业 |
| `TopAppBarScrollBehavior`（pinned/enterAlways，接受 `ScrollableState`） | 顶栏滚动行为 | 1.5.0-alpha20/22 起 stable |
| Expressive TopAppBar 家族（`MediumFlexibleTopAppBar` 等）/ Expressive ListItem / ToggleButton / FloatingToolbar | Expressive 组件 | 1.5.0-alpha19~28 陆续毕业 |
| `NavigationSuiteScaffold`（material3-adaptive-navigation-suite） | bar↔rail 自适应导航 | 1.5.0-alpha28，**不在 BOM 内**，需显式版本 |

来源：[compose-material3 releases](https://developer.android.com/jetpack/androidx/releases/compose-material3)、[build-adaptive-navigation](https://developer.android.com/develop/ui/compose/layouts/adaptive/build-adaptive-navigation)
