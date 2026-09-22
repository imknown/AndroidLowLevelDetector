# 09 第 6 步 · Navigation 3 与 MainActivity 整体切换

> **章首更正（2026-09-22 复验）** —— Navigation 3 骨架已按本章落地，以下四点是本章之后发生的：
>
> - `MainActivity : AppCompatActivity` 已过期：现为 `ComponentActivity`（`733c6941`），`appcompat` 与 `material`(MDC) 依赖删除（`f66562d4`），窗口主题改为平台父级（`e51260e6`），主题由 `themeMode: StateFlow<AppThemeMode>` 驱动（`26b9094f`）。
> - `LocalContext.current as? Activity` 已换成 `LocalActivity.current`（`AppRoot.kt:78`）。
> - 计划外新增 `AppRootShell` 拆分，让外壳可预览（`26ee0f9d`）。
> - 验证清单的「四档主题模式」为三档；切标签的转场在 `a5d3a28a` 显式改成两端 `None`（决策点 3 就地更新）。

> **⚠️ 2026-09-19 实现期更正**：①**4 个 VM 的 `savedStateHandle` 参数整体删除**（4 个 VM 均未实际使用该参数，9.3 备注允许；连带消除 Nav3 条目作用域下 `createSavedStateHandle()` 缺 extras 的风险点），工厂改为"仓库在 initializer 内构造"；②**`main_activity.xml`/`bottom_nav_menu.xml` 推迟到第 7 步删除**（对计划删除清单的有意偏差：`BaseListFragment` 仍引用 `MainActivity.binding`，现在删 XML 会编译失败；MainActivity 重写后仍保留 `internal val binding` 惰性属性，未访问不会 inflate）；③`TopAppBar` 在 m3 1.4.0 仍需 `@OptIn(ExperimentalMaterial3Api)`；④`entryDecorators` 需显式声明为 `List<NavEntryDecorator<NavKey>>`（Kotlin 泛型推断失败）；⑤完整 M3E 逐条目圆角卡布局已实验并按用户决定回滚，等 material3 1.5.0 转正（同决策 6/8 批次）。

> 所属迁移计划：[README](README.md) · 上一章：[08 第 5 步 Settings 页面重建](08-第5步-Settings页面重建.md) · 下一章：[10 第 7 步 清理收尾](10-第7步-清理收尾.md)

**改动量：新增 2 个文件、重写 1 个文件（MainActivity）、修改 6 处（4 个 VM 的 Factory + 两个屏幕组件的内边距退役）、删除 8 个文件。**
这是最大的一步，也是最后一步"动骨架"：四个页面已经全部 Compose 化，现在把 Fragment 体系、CoordinatorLayout、BottomNavigationView 一起收掉，换成官方 Compose-only 架构（单 Activity + Navigation 3）。官方迁移指南的顺序要求正是"**所有目的地都变成 composable 之后**才整体切换导航"——此刻条件刚好凑齐。

| 文件 | 操作 | 内容 |
| --- | --- | --- |
| `ui/navigation/NavKeys.kt` | 新增 | 四个导航目的地（`@Serializable` + `NavKey`） |
| `ui/AppRoot.kt` | 新增 | Scaffold + NavigationBar + NavDisplay 多返回栈骨架 |
| `ui/MainActivity.kt` | 重写 | 只剩 `enableEdgeToEdge` + `setContent` |
| `HomeViewModel` 等 4 个 VM 的 `Factory` | 修改 | 仓库改在 initializer 内构造（详见 9.3） |
| `MyModelListScreen.kt` / `SettingsScreen.kt` | 修改 | 过渡期内边距代码退役（详见 9.5） |
| `PropFragment` / `OthersFragment` / `HomeFragment` / `SettingsFragment`、`MainViewModel.kt`、`main_activity.xml`、`bottom_nav_menu.xml`、`drop_scale.xml` | 删除 | 全部职责被上表取代 |

## 9.1 导航目的地：NavKey

```kotlin
// ui/navigation/NavKeys.kt
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable          // ← 必须可序列化：返回栈要靠它"存档/读档"（进程重建恢复）
data object HomeKey : NavKey      // 标记接口 NavKey：告诉 Nav3"我是个目的地"
@Serializable
data object OthersKey : NavKey    // data object：无参数目的地最省的写法（单例 + toString 友好）
@Serializable
data object PropKey : NavKey
@Serializable
data object SettingsKey : NavKey
```

**讲解**：Navigation 3 的目的地是一个"键"而不是页面——`NavKey` 只是标记接口，必须配 `@Serializable`（返回栈要靠序列化在进程重建后恢复，kotlinx.serialization 插件项目已启用）。四个键都是 `data object`：无参数目的地用最省的写法。

## 9.2 AppRoot：多返回栈骨架

现状的"记住上个标签 + 手动 show/hide Fragment"在 Navigation 3 里有官方等价配方（[Nav3 迁移指南](https://developer.android.com/guide/navigation/navigation-3/migration-guide) 与官方 nav3-recipes 的 MultipleStacks 示例）：**每个顶层标签一条独立返回栈**，切换标签 = 换渲染的栈；未渲染的栈连同其界面状态、ViewModel 一起保留——与现在 Fragment show/hide 的行为一致。

```kotlin
// ui/AppRoot.kt（新增）
private data class TopLevelTab(
    val key: NavKey,                     // 导航键（唯一标识）
    @DrawableRes val iconRes: Int,       // 图标（复用旧菜单的矢量资源）
    @StringRes val labelRes: Int,        // 文案（复用旧菜单的字符串）
)

// 底部导航的数据源：旧 bottom_nav_menu.xml 变成一张普通列表（可用 @Preview 调试 UI 了）
private val topLevelTabs = persistentListOf(
    TopLevelTab(HomeKey, R.drawable.ic_home_24dp, R.string.title_home),
    TopLevelTab(OthersKey, R.drawable.ic_others_24dp, R.string.title_others),
    TopLevelTab(PropKey, R.drawable.ic_prop_24dp, R.string.title_prop),
    TopLevelTab(SettingsKey, R.drawable.ic_settings_24dp, R.string.title_settings),
)

@Composable
fun AppRoot() {
    // Context 必须在组合里取好：onBack 的 lambda 不是组合上下文，不能在里面读 LocalContext
    val activity = LocalContext.current as? Activity

    // 对应 MainViewModel.lastId 的"记住上次标签"
    // （rememberSaveable：旋转屏幕 + 进程重建都能恢复；存下标最简单，键是固定列表）
    var currentTabIndex by rememberSaveable { mutableIntStateOf(0) }

    // 每个标签一条返回栈（= 浏览器的每个标签页一份独立历史）；
    // map 是 inline 函数，所以里面能调 @Composable 的 rememberNavBackStack；
    // rememberNavBackStack 内部 rememberSaveable 化 → 栈内容活过进程重建
    val backStacks = topLevelTabs.map { tab -> tab.key to rememberNavBackStack(tab.key) }.toMap()

    // 条目装饰器：给每条导航条目附加能力的"插件"——
    // ① 冻结/解冻条目的 rememberSaveable 状态（滚动位置等）
    // ② 给条目一个独立 ViewModelStore（条目级 VM 作用域的来源）
    val entryDecorators = listOf(
        rememberSaveableStateHolderNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator(),
    )

    // 把"栈里的键"加工成"带装饰、可渲染的条目"——四个标签各加工一份
    val decoratedEntries = topLevelTabs.map { tab ->
        rememberDecoratedNavEntries(
            backStack = backStacks.getValue(tab.key),   // 本标签的那条栈
            entryDecorators = entryDecorators,           // 插件列表
            entryProvider = entryProvider {              // 键 → 界面的映射 DSL
                entry<HomeKey> { HomeScreen(viewModel(factory = HomeViewModel.Factory)) }
                entry<OthersKey> { MyModelListScreen(viewModel(factory = OthersViewModel.Factory)) }
                entry<PropKey> { MyModelListScreen(viewModel(factory = PropViewModel.Factory)) }
                entry<SettingsKey> { SettingsScreen(viewModel(factory = SettingsViewModel.Factory)) }
            },   // ↑ viewModel() 在 entry 内容里调用 → 自动作用域到该条目（VM 装饰器的功劳）
        )
    }

    Scaffold(   // 页面脚手架：顶栏/底栏/内容区三段式
        topBar = {
            // 对应 setSupportActionBar(toolbar) 后的默认标题栏（Activity label）
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
        bottomBar = {
            NavigationBar {     // = 旧 BottomNavigationView 的 M3 对应物
                topLevelTabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = index == currentTabIndex,   // 选中态：状态说了算
                        onClick = { currentTabIndex = index },  // 点击只改状态，渲染自动跟
                        icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->   // Scaffold 算好的避让量（顶栏 + 底栏 + 系统栏）——insets 时代结束
        NavDisplay(
            entries = decoratedEntries[currentTabIndex],   // 只渲染当前标签那条栈的条目
            onBack = { activity?.finish() },   // 见 9.6 行为说明；Context 已在组合里取好（见函数开头）
            modifier = Modifier.padding(innerPadding),     // 用脚手架给的避让量，一行搞定
        )
    }
}
```

**讲解**：

**白话（多返回栈）**：就像**浏览器的标签页**——每个标签一份独立历史（返回栈），切标签不清历史；没显示的标签页还"活在内存里"（界面状态、ViewModel 都保留），切回来原样接着用。旧代码"四个 Fragment 建一次然后 show/hide"追求的正是这个效果，Nav3 用官方配方达成。

**白话（装饰器）**：装饰器是给每条导航条目套的**快递包装服务**——`SaveableStateHolder` 是气泡膜（条目下架时把易碎品——滚动位置等——打包存好，上架时拆包复原）；`ViewModelStore` 是独立小仓库（每个条目一间，互不串味，条目真正"退单"才清仓）。

✅ 正例 / ❌ 反例（本步最容易做错的两件事）：

```kotlin
// ❌ 反例一：ViewModel 在 AppRoot 顶层创建再传进 entry——
//   作用域错位（VM 绑到了整棵骨架而不是条目），且破坏"entry 内容自给自足"的封装
val homeViewModel = viewModel(factory = HomeViewModel.Factory)   // ← 在 AppRoot 里
entry<HomeKey> { HomeScreen(homeViewModel) }                     // ← 传进去

// ✅ 正例：viewModel() 在 entry 内容里调用，VM 装饰器自动把作用域绑到条目
entry<HomeKey> { HomeScreen(viewModel(factory = HomeViewModel.Factory)) }
```

```kotlin
// ❌ 反例二：漏了 SaveableStateHolder 装饰器——切走再切回，滚动位置、输入内容全部归零
val entryDecorators = listOf(rememberViewModelStoreNavEntryDecorator())   // 只剩仓库没有气泡膜

// ✅ 正例：两个装饰器都上（官方指南的标准组合），状态与 VM 各有人管
```
- **`Scaffold`**：M3 的页面脚手架。给它 `topBar` / `bottomBar`，它通过 `innerPadding` 告诉内容区"避开这些栏和系统 insets"——第 2 步那些手算 insets、去 Activity 量底栏高度的过渡代码，到这一步**全部退役**（9.5）。这就是 01 章"痛点表"里第一行的兑现。
- **`NavigationBar` / `NavigationBarItem`**：`BottomNavigationView` 的 M3 对应物（stable），selected/onClick/icon/label 四个参数对号入座，菜单 XML 换成了普通数据列表。
- **`rememberNavBackStack(key)`**：可保存的返回栈。四个栈各自独立，换标签不销毁——对应"四个 Fragment 只建一次、此后 show/hide"。
- **`rememberDecoratedNavEntries(backStack, entryDecorators, entryProvider)`**：把"栈里的键"加工成"可渲染的条目"。装饰器是 Nav3 的扩展点：
  - `rememberSaveableStateHolderNavEntryDecorator()`（NavDisplay 的默认装饰器）——条目离开渲染时冻结其 `rememberSaveable` 状态（滚动位置等），回来时解冻；
  - `rememberViewModelStoreNavEntryDecorator()`——给每个条目独立的 ViewModelStore（来自 `lifecycle-viewmodel-navigation3`，官方指南的标准组合）。条目内容里 `viewModel()` 拿到的 VM 就**作用域到该条目**，标签切换不销毁、条目真正出栈才 clear。
- **`entryProvider { entry<HomeKey> { ... } }`**：键 → 界面的映射 DSL，类型参数就是 NavKey 类型，编译器保证穷尽。这是官方现行命名（2025 年早期预览的 `rememberNavEntryProvider` / `NavEntryProviderCreator` 等名字均已废弃，勿用旧资料）。
- **`NavDisplay(entries, onBack)`**：只渲染传入的条目列表并接好系统返回（含预测性返回手势的动画）。

## 9.3 ViewModel 接线：Fragment 时代 → 条目时代

Fragment 删除后，`viewModels(extrasProducer = ...)` 的接线随之消失。4 个 ViewModel 的 `Factory` 做一处小简化——仓库从 `CreationExtras` 注入改为在 initializer 内直接构造（它们都是无状态轻对象）：

```kotlin
// before：依赖 extrasProducer 塞进来的 MY_REPOSITORY_KEY
val Factory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val repository = this[MY_REPOSITORY_KEY] as PropRepository
        val savedStateHandle = createSavedStateHandle()
        PropViewModel(repository, savedStateHandle)
    }
}

// after：初始化器自给自足，任何宿主（Fragment 时代与 Nav3 条目时代）都能用
val Factory: ViewModelProvider.Factory = viewModelFactory {
    initializer {
        PropViewModel(
            PropRepository(PropertiesDataSource(), SettingsDataSource()),
            createSavedStateHandle()
        )
    }
}
```

条目内一行取用（9.2 的 `entryProvider` 里）：`viewModel(factory = PropViewModel.Factory)`——作用域是该 NavKey 的条目，等价于原来的 Fragment 作用域。`MY_REPOSITORY_KEY` 常量与四个 Fragment 里的 extrasProducer 样板一并删除。

> 实现验证点：`createSavedStateHandle()` 需要宿主提供 SavedStateHandle 相关 extras。当前 4 个 ViewModel 都**收了但没实际使用**这个参数，若条目作用域下初始化报缺 extras，可改传空的 `SavedStateHandle()` 或顺势删掉该参数，不影响任何功能。

## 9.4 MainActivity：从 120 行到 25 行

### before（节选）

```kotlin
internal val binding by viewBinding(MainActivityBinding::inflate)
private val mainViewModel by viewModels<MainViewModel>()

override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    if (isAtLeastAndroid10()) { window.isNavigationBarContrastEnforced = false }
    super.onCreate(savedInstanceState)
    setContentView(binding.root)
    initWindowInsets()      // 两个手动 insets 监听
    initViews()             // setSupportActionBar + BottomNavigationView 监听
    if (savedInstanceState == null) { supportFragmentManager.switch(R.id.navigation_home, true) }
}
// + initWindowInsets() / initViews() / switch() / createFragment() 约 70 行
```

### after

```kotlin
class MainActivity : AppCompatActivity() {   // 暂留 AppCompatActivity（原因见下方讲解与 9.6 决策点）

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()                   // 边到边：内容铺到系统栏后面（避让交给 Scaffold）
        if (isAtLeastAndroid10()) {
            window.isNavigationBarContrastEnforced = false   // 导航栏不做强制对比度遮罩
        }
        super.onCreate(savedInstanceState)

        setContent {          // Compose 版 setContentView：从此这棵树就是整个界面
            AppTheme {        // 根部包主题：深浅色/动态取色从这棵树的顶上流下去
                AppRoot()     // 骨架（Scaffold + 导航）+ 四个页面，全在这一个函数里长出来
            }
        }
    }
}
```

**讲解**：

- **`setContent { }`**：`ComponentActivity` 系的扩展（`activity-compose`），Activity 内容整棵树变成 Compose。窗口主题仍来自 manifest 的 `@style/AppTheme`（XML 主题继续负责窗口背景/启动外观，只是不再提供控件样式）。
- **保留 `AppCompatActivity`** 是刻意的最小改动：主题模式的"跟随省电模式/总是深色"目前靠 `AppCompatDelegate.setDefaultNightMode`（`MyApplication` 里），它只对 AppCompat Activity 生效；AppCompatDelegate 换夜间模式会重建 Activity，Compose 状态经 `rememberSaveable`/ViewModelStore 自动存活。迁到 `ComponentActivity` + Compose 侧自管 darkTheme 的方案见第 10 章"遗留优化"。

## 9.5 过渡期内边距退役

`Scaffold(innerPadding)` 接管 insets 后，两处屏幕组件做减法：

```diff
// MyModelListScreen.kt 的 MyModelListContent
-    val horizontal = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()
-    val bottomBarHeight = rememberBottomBarHeight()
     LazyColumn(
         state = listState,
         modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer),
-        contentPadding = PaddingValues(start = insets.left, top = 12.dp, end = insets.right, bottom = bottomBarHeight + 12.dp),
+        contentPadding = PaddingValues(vertical = 12.dp),   // 横向与底部由 innerPadding 负责
         ...
```

`SettingsScreen` 的 LazyColumn 同样处理；`rememberBottomBarHeight` 函数删除。列表项的 12dp 横向留白保留（那是内容设计的一部分，不是 insets 补偿）。

## 9.6 行为变化说明与决策点

诚实起见，本步有四处行为差异，除特别标注外建议接受；不接受按括号内方案改：

1. **返回键**：现状任何标签按返回直接退出；官方 Nav3 迁移指南假设"从非首页标签按返回先回首页"（exit through home）。本方案保持现状（`onBack = { activity?.finish() }`，`activity` 见 9.2 开头）；想跟官方对齐改成 `if (currentTabIndex != 0) currentTabIndex = 0 else activity?.finish()` 即可。
2. **底栏不再随滚动隐藏**：现状 `hide_bottom_view_on_scroll_behavior` 让底栏滚动时下潜；`Scaffold.bottomBar` 默认常显。要复刻需自写 NestedScrollConnection（约 30 行），收益有限，建议接受常显（这也是 M3 应用的主流形态）。
3. **标签切换动画**：现状 `drop_scale`（1.025 → 1.0 缩放 100ms）。**已定方案：不复刻缩放，切换瞬时完成、不做任何转场。** 标签切换是整栈替换，NavDisplay 视作前进导航，会走它自己的默认转场（fadeIn + fadeOut，各 700ms，该常量在库内为 internal），所以"不要动画"必须在 `AppRoot` 里显式设 `transitionSpec = { EnterTransition.None togetherWith ExitTransition.None }`。设备实测（1x 动画速率、约 0.6s 一帧连续抓帧）：切换是干净的一步，无中间帧、无空白帧。另附实测到的库行为：退出侧只给 `ExitTransition.None` 时旧页面不会淡出，而是保持不透明绘制到转场结束。若要改成按标签复刻缩放，可用每条目元数据定制：

```kotlin
entry<PropKey>(
    metadata = metadata {
        put(NavDisplay.TransitionKey) {
            scaleIn(initialScale = 1.025f, animationSpec = tween(100)) togetherWith
                scaleOut(targetScale = 1f, animationSpec = tween(100))
        }
    }
) { MyModelListScreen(viewModel(factory = PropViewModel.Factory)) }
```

4. **底栏选中标签颜色**：material3 1.4.0 起选中态标签色从 `onSurface` 改为 `secondary`（官方为对比度与系统一致性所做的全局调整）。与旧版 BottomNavigationView 存在轻微视觉差，如需复刻旧色可用 `NavigationBarItemDefaults.colors()` 覆盖，建议接受官方新默认。

## 9.7 验证清单

1. 四个标签切换：数据/滚动位置各自保留；切走再切回不重新加载（对应 Fragment show/hide）；
2. 杀进程重启：回到退出前所在标签（rememberSaveable + NavBackStack 序列化恢复）；
3. 系统返回手势：预测性返回动画正常，最终退出 app；
4. 主题模式四档逐一验证（重建后四页配色正确、返回栈状态不丢）；
5. 深色 + 动态取色 + 省电模式机型抽查；
6. 双 flavor（`assembleFossDebug` / `assembleFirebaseDebug`）均编译运行，设置页 shop 链接各归其主；
7. 旋转屏幕：动画过程不重入、对话框/滚动条设置项状态保留。

## 本步小结

- Navigation 3 的核心四件：`NavKey`（可序列化目的地）、`rememberNavBackStack`（可保存返回栈）、`entryProvider` DSL（键→界面）、`NavDisplay`（渲染 + 返回手势）；
- 多返回栈配方与装饰器体系（SaveableState + ViewModelStore 两个装饰器的分工）；
- `Scaffold` + `innerPadding` 如何一次性吃掉整个 insets 问题域。

最后一章把屋子扫干净：[10 第 7 步 清理收尾](10-第7步-清理收尾.md)。
