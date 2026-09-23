# 05 第 2 步 · PropFragment 接入 ComposeView

> **章首更正（2026-09-22 复验）** —— 本章快照保留作「互操作两座桥」的教学记录，以下已不成立：
>
> - 宿主 `PropFragment` 已在第 6 步随 View 层删除（`aec789e5`），`ComposeView` 嵌 Fragment 这座桥在代码里已不存在。
> - 本章核心知识点 `produceState`「记住最近一次 Done 以防闪空」被换掉：状态拆成 `modelsStateFlow` + `isLoadingStateFlow` 两条流（`002f25b3`；`BaseListViewModel.modelsStateFlow / isLoadingStateFlow`），刷新期间保留旧列表由数据流本身保证（`MyModelListScreen() 的 LaunchedEffect { viewModel.init() }`），不再需要 `produceState`。
> - 连带删除了 `ui/common/StateExt.kt`（`State` 密封接口）；`ToastExt.kt` 仍在。

> 所属迁移计划：[README](README.md) · 上一章：[04 第 1 步 列表卡片组件](04-第1步-列表卡片组件.md) · 下一章：[06 第 3 步 交互补齐](06-第3步-交互补齐.md)

**改动量：重写 1 个文件（约 40 行）、新增 1 个文件（约 70 行）。**
这是第一个"真的跑起来"的 Compose 界面：Prop（属性）页整页换成 Compose，ViewModel 与数据层一行不动。

| 文件 | 操作 | 内容 |
| --- | --- | --- |
| `ui/base/list/MyModelListScreen.kt` | 新增 | 三个列表页共用的屏幕组件（状态订阅 + LazyColumn） |
| `ui/prop/PropFragment.kt` | 重写 | 从 `BaseListFragment` 子类改为"纯壳"：只剩 ViewModel 接线 + `ComposeView` |

**本步完成后的已知缺口**（下一步补回）：下拉刷新、滚动条设置项暂不生效。
选 Prop 页打头是因为它最简单（纯列表、无事件监听）；官方迁移策略页也建议从"数据显示相对静态的简单屏幕"起步。

## 5.1 before：PropFragment（现状）

```kotlin
class PropFragment : BaseListFragment() {   // ← 列表基建全靠继承
    companion object { fun newInstance() = PropFragment() }

    override val listViewModel by viewModels<PropViewModel>(   // ← 这段接线是唯一要保留的东西
        extrasProducer = {
            MutableCreationExtras(defaultViewModelCreationExtras).apply {
                val repository = PropRepository(PropertiesDataSource(), SettingsDataSource())
                this[PropViewModel.MY_REPOSITORY_KEY] = repository
            }
        },
        factoryProducer = { PropViewModel.Factory }
    )
}
```

继承链 `BaseListFragment` 替我们做了：insets 监听、滚动条模式、SwipeRefreshLayout 配色与刷新回调、RecyclerView 四件套装配、订阅 `modelsStateFlow` 并 `submitList`。换成 Compose 后这些全部由一个可组合函数承担。

## 5.2 after：新 PropFragment + 共用列表屏幕

**`ui/prop/PropFragment.kt`（重写）**——ViewModel 接线原封不动，界面部分只剩官方互操作样板：

```kotlin
class PropFragment : Fragment() {   // ← 不再继承 BaseListFragment：列表装配全部交给 Compose

    companion object {
        fun newInstance() = PropFragment()
    }

    // ↓ 这段 ViewModel 接线与迁移前一字不差——"数据层零改动"的最直接证据
    private val listViewModel by viewModels<PropViewModel>(
        extrasProducer = {          // "创建附加条件"：把仓库塞进 CreationExtras 备用
            MutableCreationExtras(defaultViewModelCreationExtras).apply {
                val repository = PropRepository(PropertiesDataSource(), SettingsDataSource())
                this[PropViewModel.MY_REPOSITORY_KEY] = repository
            }
        },
        factoryProducer = { PropViewModel.Factory }   // "怎么造"：定制工厂
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {   // 返回值从"XML inflate 的 View"换成 ComposeView
        // 官方推荐策略：视图树生命周期销毁时一并销毁组合（防止 Fragment 视图重建时泄漏旧组合）
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {                                 // Compose 版 "setContentView"
            AppTheme {                               // 必须显式包主题（原因见 5.3 讲解）
                MyModelListScreen(listViewModel)     // 交接给屏幕组件，Fragment 的活到此为止
            }
        }
    }
}
```

**`ui/base/list/MyModelListScreen.kt`（新增）**：

```kotlin
package net.imknown.android.forefrontinfo.ui.base.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.core.view.doOnLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import net.imknown.android.forefrontinfo.MainActivity
import net.imknown.android.forefrontinfo.R

/** 三个列表页（Home/Others/Prop）共用的屏幕组件。只认 BaseListViewModel，不认具体页面。 */
@Composable
fun MyModelListScreen(
    viewModel: BaseListViewModel,   // 屏幕级组件拿 VM：官方允许且推荐的唯一层级
    modifier: Modifier = Modifier,  // 惯例参数：外部约束
) {
    // ① 订阅：Flow → Compose 状态。STOPPED 自动暂停收集（省电）；by 委托读起来像普通变量
    val state by viewModel.modelsStateFlow.collectAsStateWithLifecycle()

    // ② 副作用：对应 BaseListFragment 末尾的 listViewModel.init()。
    //    放进 LaunchedEffect（副作用不得直接写在组合体内）；init() 幂等，视图重建重复调用也无害
    LaunchedEffect(viewModel) { viewModel.init() }

    // ③ 派生状态：刷新时 state 会回到 Loading；为保持旧实现"列表不动、只加转圈"的手感，
    //    用 produceState 记住"最近一次 Done 的数据"（Loading 时保留旧值 → 不闪空）
    val models by produceState<PersistentList<MyModel>>(
        initialValue = persistentListOf(),  // 初值：空名单（首次加载中列表本来就是空的）
        key1 = state                        // 钥匙：state 每变一次，下面的块就重跑一次
    ) {
        (state as? State.Done)?.let { value = it.value.toPersistentList() }
        // ↑ 只有 Done 才写入；Loading/NotInitialized 什么都不做 → 旧值原样保留
    }

    MyModelListContent(models, modifier)    // 状态备齐，交给纯展示的内容函数
}

@Composable
private fun MyModelListContent(
    models: PersistentList<MyModel>,   // 纯数据进（不含 VM）→ 可预览、可复用
    modifier: Modifier = Modifier,
) {
    // 过渡期待遇（第 6 步由 Scaffold 自动处理，届时删除本函数全部内边距代码）：
    //   横向 = 系统栏 insets（对应旧 RV 的 updatePadding(left/right = insets)）
    //   底部 = 底部导航栏高度（旧代码从 Activity 的 View 上现量，这里同样）
    val horizontal = WindowInsets.systemBars
        .only(WindowInsetsSides.Horizontal)     // 只取左右：顶部已由 appBar 挡住，别重复避让
        .asPaddingValues()                      // insets → 可用于 contentPadding 的 PaddingValues
    val bottomBarHeight = rememberBottomBarHeight()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            // 对应 base_list_fragment.xml 的 android:background="?attr/colorSurfaceContainer"
            .background(MaterialTheme.colorScheme.surfaceContainer),
        // 对应 MyItemDecoration（四周 12dp、首条上方 12dp）+ clipToPadding="false"：
        // contentPadding 会随内容一起滚动（= 旧 RV 的 clipToPadding=false），不是把内容挤小
        contentPadding = PaddingValues(
            start = horizontal.calculateStartPadding(LocalLayoutDirection.current),
            top = dimensionResource(R.dimen.item_divider_space_vertical),
            end = horizontal.calculateEndPadding(LocalLayoutDirection.current),
            bottom = bottomBarHeight + dimensionResource(R.dimen.item_divider_space_vertical),
        ),
        // 条目之间的固定间距（= ItemDecoration 的 bottom = spaceV）
        verticalArrangement = Arrangement.spacedBy(
            dimensionResource(R.dimen.item_divider_space_vertical)
        ),
    ) {
        items(
            items = models,
            key = { it.key },          // 对应 DiffUtil.areItemsTheSame 的 key；不给会破坏动画与状态
            contentType = { it.type }, // 对应 RecyclerView 的 viewType
        ) { model ->
            MyModelCard(
                model,
                // 对应 MyItemDecoration 的 left/right = spaceH
                modifier = Modifier.padding(
                    horizontal = dimensionResource(R.dimen.item_divider_space_horizontal)
                ),
            )
        }
    }
}

/** 过渡期桥接：底部导航栏还是 View 世界的 BottomNavigationView，Compose 量不到它，借宿主 Activity 现量。 */
@Composable
private fun rememberBottomBarHeight(): Dp {
    val density = LocalDensity.current
    val bottomBar = (LocalView.current.context as? MainActivity)?.binding?.bottomNavigationView
    val heightPx = remember { mutableIntStateOf(0) }

    if (bottomBar != null) {
        // doOnLayout：第一次布局完成后量高度（此时高度才有效）——与旧代码同款做法
        LaunchedEffect(bottomBar) {
            bottomBar.doOnLayout { heightPx.intValue = it.height }
        }
    }

    return with(density) { heightPx.intValue.toDp() }
}
```

（`State`、`MyModel` 等本包内引用与少量导入从略。）

## 5.3 逐点讲解

### ComposeView 互操作（官方"Compose 进 View 世界"的标准姿势）

**白话**：`ComposeView` 就是在 **View 大楼里租下的一间 Compose 教室**——大楼结构（Activity/Fragment/导航）不动，教室里随便布置；等第 6 步整栋楼翻新成 Compose，教室就变成整栋楼。

- **`ComposeView(requireContext())` + `setContent { }`**：View 树里的一扇 Compose 窗口。宿主 Fragment 的视图树原样保留（本例中 Fragment 连 XML 都不需要了）。
- **`ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed`**：官方为 Fragment 场景点名的策略——Fragment 视图销毁（如返回栈弹出、配置变更）时同步销毁 Compose 组合，避免泄漏。默认策略 `DisposeOnDetachedFromWindowOrReleasedFromPool` 在 RecyclerView 复用场景才更优。
- 注意 `setContent` 里包了 `AppTheme`：ComposeView 内容不自动继承任何 Material 主题，必须显式包主题（这也是官方主题迁移页的立场：过渡期"两个主题真源"并存，View 控件继续吃 XML 主题，Compose 内容吃 `AppTheme`，两边配色同源所以视觉一致）。

### 状态订阅三件套

- **`collectAsStateWithLifecycle()`**：`StateFlow` → Compose `State`，且跟随**生命周期**（STOPPED 时暂停收集，比 `collectAsState()` 省电、也避免后台页白白重组）。这是官方推荐的默认选择，来自第 0 步加的 `lifecycle-runtime-compose`。
- **`produceState`**：把"非状态的东西"（这里是'最近一次成功的数据'）变成状态。它的 lambda 是协程，`key` 变化时重跑、不满足条件时**保留旧值**——正好实现"刷新时列表不闪空"。02 章第 3 节的落地。
- **`LaunchedEffect(viewModel) { viewModel.init() }`**：任何"进组合时启动、离组合即取消"的副作用都走这里；直接在函数体里调 `init()` 会在每次重组重复触发。

**白话（三件套各一句）**：`collectAsStateWithLifecycle` 是**收快递**——家里有人（界面可见）才收；`produceState` 是**腌罐头**——把外面的食材加工成"冰箱里随取随用的存货（状态）"；`LaunchedEffect` 是**值日生**——进门干活、出门走人。

✅ 正例 / ❌ 反例（状态订阅的三个坑）：

```kotlin
// ❌ 坑一：collectAsState 不带生命周期——app 在后台仍收集，每次发射都白白重组
val state by viewModel.modelsStateFlow.collectAsState()

// ❌ 坑二：不用 produceState、直接取 Done 的值——Loading 一到列表瞬间清空（刷新闪空）
val models = (state as? State.Done)?.value.orEmpty()

// ❌ 坑三：produceState 忘了传 key——块只在首次组合跑一次，之后 state 怎么变都不更新
val models by produceState(persistentListOf<MyModel>()) { /* 永远只有初值 */ }

// ✅ 正例：见上面 5.2 的 ①③——带生命周期收集 + 带 key 的 produceState
```

### LazyColumn 的 key 与 contentType

- `key = { it.key }`：`MyModel.key` 正是旧 `DiffUtil.areItemsTheSame` 用的那个 key，语义 1:1 迁移。有了它，列表增删时 Compose 能对上"哪条是哪条"，动画与滚动位置不乱。
- `contentType = { it.type }`：`MyModelType`（普通条目/过期应用条目）继续当"类型提示"，帮 LazyColumn 复用同类条目的组合产物。

### ViewModel 的纪律

`MyModelListScreen` 收的是 `BaseListViewModel`（屏幕级组件拿 ViewModel 是官方允许且推荐的形态），但它**不向下传递** ViewModel——卡片只收 `MyModel` 数据。第 4 步加 Home 页事件时，会看到"屏幕组件包一层、事件用 LaunchedEffect 收"的扩展方式。

## 5.4 验证清单

1. `./gradlew :app:assembleFossDebug` 编译通过；
2. 运行 → Prop 页：卡片样式与迁移前一致（圆角、底色 surfaceBright、字号、色点位置）、上下左右留白一致、深色/动态取色正常；
3. 旋转屏幕：列表数据还在（ViewModel 作用域未变）；
4. Home/Others/Settings 三页完全不受影响（仍走旧 BaseListFragment）；
5. 已知缺口确认：Prop 页暂无下拉刷新、暂无滚动条（第 3 步补）。

## 本步小结

- 官方互操作样板：`ComposeView` + `ViewCompositionStrategy` + 主题显式包裹；
- `collectAsStateWithLifecycle` / `produceState` / `LaunchedEffect` 三个状态侧 API 的分工；
- LazyColumn 的 `contentPadding` / `spacedBy` / `key` / `contentType` 与旧 RV 体系的逐项对应。

下一章补齐交互：[06 第 3 步 交互补齐](06-第3步-交互补齐.md)。
