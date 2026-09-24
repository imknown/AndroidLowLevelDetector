# 07 第 4 步 · Home 与 Others 页迁移

> **章首更正 (2026-09-22 复验)** — 本章两项核心做法后来都被推翻, 快照保留:
>
> - `ui/home/HomeScreen.kt` ("包一层"共用列表的 Home 专属屏幕)**已撤销**: Home 没有页面特有逻辑, `AppRoot() 的 entryProvider(Home entry)` 直接使用共用的 `MyModelListScreen`.
> - 本章教的 "`LaunchedEffect` 收集 `SettingsViewModel.outdatedOrderChangedSharedFlow`" 链路已整体删除: 排序开关改由 `HomeViewModel` 自己注册 `OnSharedPreferenceChangeListener` 观察偏好键 (`38492b82`; `HomeViewModel.outdatedOrderChangeListener`), 落地时再做一次校正 (`45f5f047`). 对应的架构体检条目 C5 已判定解决并删除.
> - Others 页与本章描述一致.

> 所属迁移计划: [README](README.md) · 上一章: [05 第 3 步 交互补齐](05-第3步-交互补齐.md) · 下一章: [07 第 5 步 Settings 页面重建](07-第5步-Settings页面重建.md)

**改动量: 重写 2 个文件 (各约 40 行), 新增 1 个文件 (约 30 行).**
这一步是第 2 步的"复制推广 + 一个新知识点": Home 页多一个跨页事件流要收集. 完成后三个列表页全部 Compose 化, `BaseListFragment` 体系再无使用者 (文件本身留到第 7 步统一删除).

## 6.1 OthersFragment: 纯复制

与 `PropFragment` (第 2 步) 完全同构, 仅 ViewModel 接线不同, 全量代码如下:

```kotlin
class OthersFragment : Fragment() {

    companion object {
        fun newInstance() = OthersFragment()
    }

    private val listViewModel by viewModels<OthersViewModel>(
        extrasProducer = {
            MutableCreationExtras(defaultViewModelCreationExtras).apply {
                val repository = OthersRepository(
                    BasicDataSource(),
                    ArchitectureDataSource(),
                    RomDataSource(),
                    FingerprintDataSource(),
                    KernelDataSource(),
                    OthersDataSource()
                )
                this[OthersViewModel.MY_REPOSITORY_KEY] = repository
            }
        },
        factoryProducer = { OthersViewModel.Factory }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            AppTheme { MyModelListScreen(listViewModel) }
        }
    }
}
```

对比可见**组件复用的收益**: 三个页面共享同一个 `MyModelListScreen`, 页面间差异只剩"喂哪个 ViewModel". 这正是"状态提升"纪律的回报 — 如果第 2 步把 ViewModel 逻辑写死在列表组件里, 这里就得复制三份.

## 6.2 HomeFragment: 多一个事件流

### before(现状)

```kotlin
class HomeFragment : BaseListFragment() {
    ...
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)   // ← BaseListFragment 的常规装配

        viewLifecycleOwner.lifecycleScope.launch {      // ← 本页特有: 设置页"过期应用排序"变化时刷新对应条目
            val flow = SettingsViewModel.outdatedOrderChangedSharedFlow
            flow.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect {
                listViewModel.payloadOutdatedTargetSdkVersionApk()
            }
        }
    }
}
```

### after

Fragment 壳与 Others 同构 (换 `HomeViewModel` 接线), 特有逻辑进一个新的屏幕组件 `HomeScreen`:

```kotlin
// ui/home/HomeScreen.kt(新增)
@Composable
fun HomeScreen(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    // 对应旧 onViewCreated 里的 SharedFlow 收集:
    // LaunchedEffect 进组合启动, 离组合取消; 注意它不随生命周期 STOPPED 暂停 (见下方讲解)
    LaunchedEffect(viewModel) {
        SettingsViewModel.outdatedOrderChangedSharedFlow.collect {      // 设置页的"广播"
            viewModel.payloadOutdatedTargetSdkVersionApk()              // 收到就刷新对应条目
        }
    }

    MyModelListScreen(viewModel, modifier)   // 其余全部复用公共列表屏 (HomeViewModel 是 BaseListViewModel 的子类)
}
```

```kotlin
// ui/home/HomeFragment.kt(重写, 壳与 Others 相同)
setContent {
    AppTheme { HomeScreen(listViewModel) }
}
```

### 讲解

**白话**:"包一层"就是给公共积木配**专属说明书** — `MyModelListScreen` 是通用积木 (三个页面共用), Home 的特殊需求 (收事件) 写在说明书 (`HomeScreen`) 里, 积木本身一个字不用改. Compose 里"组合的单位是函数", 包一层没有运行时代价, 放心包.

✅ 正例 / ❌ 反例 (页面特有逻辑放哪):

```kotlin
// ❌ 反例一: 往公共组件塞开关 — 每来一个页面加一个参数, 组件很快烂掉
MyModelListScreen(viewModel, collectOutdatedOrderEvents = true, collectXxxEvents = false)

// ❌ 反例二: 把 ViewModel 一路传进子组件 — 破坏"子组件只吃数据"的边界, 预览也废了
MyModelCard(model, viewModel = viewModel)

// ✅ 正例: 页面专属组件包一层, 事件在层里收, 公共组件保持无知
@Composable
fun HomeScreen(viewModel: HomeViewModel, modifier: Modifier) {
    LaunchedEffect(viewModel) { /* 收事件 */ }
    MyModelListScreen(viewModel, modifier)
}
```

- **为什么 HomeScreen 不并进 MyModelListScreen**: 这个事件是 Home 独有的业务; 塞进共用组件就得加开关参数, 破坏"共用组件只依赖 BaseListViewModel"的边界. 正确姿势是**页面专属组件包一层, 公共组件保持通用** — Compose 里"组合"的天然单位就是函数, 包一层没有运行时代价.
- **collect 为什么没有 flowWithLifecycle**: `LaunchedEffect` 跟随的是**组合**生命周期 — 离开组合 (Fragment 视图销毁) 即取消; 但它**不会**在生命周期降到 STOPPED 时暂停, 而旧代码的 `flowWithLifecycle` 默认 STARTED 门槛会暂停. 两者的差别只在"app 退到后台期间事件是否仍被消费" — 本项目的事件只会由设置页的用户交互触发, 后台不会发射, 因此实际无感. 若将来出现"后台可能发事件, 且希望后台不消费"的流, 用 `repeatOnLifecycle(Lifecycle.State.STARTED) { flow.collect { ... } }` 包裹即可获得与旧代码完全一致的行为; 若这个流改从 ViewModel 实例上收 (而非现在的 companion 静态流), 则应改用第 2 步的 `collectAsStateWithLifecycle` 思路.
- 静态 `SharedFlow` (`SettingsViewModel.outdatedOrderChangedSharedFlow`) 是现状架构, 本步**原样保留**; 第 5 步重建设置页时会看到它的新发射方, 第 6 步后有"是否改为状态提升"的可选重构, 见第 6 步那章末尾的"遗留优化".

## 6.3 验证清单

1. Others 页: 列表正常, 下拉刷新正常 (第 3 步已补齐), 深色正常;
2. Home 页: 列表正常; **关键场景** — 设置页切换"过期应用按包名排序"开关 → 回到 Home 页, 对应条目详情刷新 (验证事件流链路);
3. 四个标签页来回切换, Home/Others 的滚动位置与数据保持 (Fragment show/hide 保留状态的原行为不变);
4. 杀进程重启: 停留在 Home 时仍回到 Home(`MainViewModel.lastId` 逻辑未动).

## 本步小结

- 屏幕组件的"包一层"扩展模式 (页面特有逻辑在外层, 公共能力在内层);
- `LaunchedEffect` 收 SharedFlow 事件 vs `collectAsStateWithLifecycle` 订状态 — 两种"收集"的适用边界.

下一章是本次迁移中**唯一的"重写"而非"翻译"**:[07 第 5 步 Settings 页面重建](07-第5步-Settings页面重建.md).
