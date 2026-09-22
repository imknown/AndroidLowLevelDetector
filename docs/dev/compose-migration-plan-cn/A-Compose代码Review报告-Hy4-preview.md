# B·迁移后代码复查报告

> 所属迁移计划：[README](README.md) · A 系列附录（Appendix = 附录，与 `01~10` 正文章节区分）
>
> 本报告由 **Hy4-preview** 产出，与 [A·Compose 代码 Review 报告（Qwen3.8-Flash）](A-Compose代码Review报告-Qwen3.8-Flash.md) 是同题的两份独立复查，互为补充：
> 重叠条目为 F1 ≈ 对方 2（滚动条事件总线死链路）、F4 ≈ 对方 3（主题死代码）、F5 ≈ 对方 5（`Card(onClick = {})`）；
> 本报告独有：F2（加载异常致 `isLoading` 卡死）、F3（Composable 直写 SharedPreferences）、F9（`MyModel.key` 重复即崩）、F10（`@Stable` 冗余与契约风险）、F11（返回栈索引越界）、F12（图标硬编码黑色依赖 tint）、F13（计划文档已落后于代码），以及第三节 9 条"已核对无问题"清单。
>
> 定位：**迁移完成后的整体复查**（第 0~7 步全部落地之后）。关注"改错的 / 改多的 / 漏改的 / 不合理的 / 有更好改法的"，每条给出直接原因 → 根本原因 → 问题代码 → 修改方案。

- **复查基线**：`HEAD = cb4104aa`（Compose 迁移系列共 35 个提交，起点 `58d22787`）
- **编译状态**：`./gradlew compileFossDebugKotlin` → **BUILD SUCCESSFUL**（2m35s，仅一条 `Detected multiple Kotlin daemon sessions` 警告）。
  顺带说明：迁移过程中留下的三处编译报错（`StatusColor` 未导入、`[a, b]` 位置解构歧义）在当前 HEAD 已全部消失，属中间态。
- **复查范围**：UI 层（Compose 组合 / 主题 / 导航 / 状态与副作用）。数据层只看它与 UI 的接缝。
- **对照物**：`git show 58d22787:<旧文件>` 逐项比对旧 View 实现 + 本目录的计划与"关键决策点汇总"。

---

## 一、结论速览

| # | 类别 | 严重度 | 位置 | 一句话 |
| --- | --- | --- | --- | --- |
| F1 | 改错 / 漏改 | P0 | `SettingsViewModel.kt:36-44`、`SettingsScreen.kt:117-121` | 滚动条事件总线**零订阅者**，`emit` 是静默空转；注释还写着"列表页立即生效" |
| F2 | 漏改（继承自旧代码） | P0 | `BaseListViewModel.kt:55-66` | 加载抛异常时 `isLoading` 永久停在 `true`，下拉刷新转圈再也停不下来 |
| F3 | 不合理 / 更好的改法 | P1 | `SettingsScreen.kt:69-130` | Composable 里直接读写 `SharedPreferences` 并调 `MyApplication.setMyTheme`，绕过 ViewModel |
| F4 | 改多 | P1 | `Theme.kt:108-270` | 4 套对比度配色 + `ColorFamily`/`unspecified_scheme` 全部无人使用，且在 `ThemeKt` 静态初始化里被一并构造 |
| F5 | 改错（a11y） | P1 | `MyModelCard.kt:42-56`（`cb4104aa` 引入） | `Card(onClick = {})` 空点击：TalkBack 报"可双击激活"却无反应 |
| F6 | 不合理 | P2 | `AppRoot.kt:111-116` | `onBack = { activity?.finish() }` 与"每标签一条返回栈"自相矛盾 |
| F7 | ~~改多~~ **已撤回** | — | `MyModelExt.kt:8-15` | ~~`toColoredMyModel(..., Boolean)` 重载零调用~~ 2026-09-22 复验：断言不成立，该重载有 6 处调用 |
| F8 | 改多 | P2 | `ExtendedColors.kt:43-49` | `of()` 不需要 `@Composable`，白白限制调用场景 |
| F9 | 隐患 | P2 | `MyModel.kt:24-28` + `MyModelListScreen.kt:106` | `key` 用标题文本，`Raw` 标题一旦重复 LazyColumn 直接抛异常（旧 DiffUtil 不崩） |
| F10 | 改多 / 契约风险 | P3 | `HomeViewModel.kt:36` 等 | `@Stable` 标在子类上是冗余的，且 `@Stable` 是一份"永不失效"的承诺 |
| F11 | 隐患 | P3 | `AppRoot.kt:94-115` | 每个标签的 provider 都注册了全部 4 个 entry；`decoratedEntries[currentTabIndex]` 无越界保护 |
| F12 | 隐性依赖 | P3 | `ic_*_24dp.xml`（4 个） | 图标 `fillColor` 硬编码 `#FF000000`，正确性完全依赖 `Icon` 的默认 tint |
| F13 | 文档欠账 | P3 | 本目录 README / 10 章 | 决策 9（暂留 AppCompatActivity）、10.4-2（去 AppCompat 化）**早已做完**，文档未同步 |

路径速查（相对仓库根）：`app/src/main/java/net/imknown/android/forefrontinfo/ui/…`。

---

## 二、问题详解

### F1 · 滚动条事件总线零订阅者，`emit` 是静默空转（P0，改错 + 漏改）

**现象**：设置页切换"滚动条模式"→ 值写进了 SharedPreferences，但列表页毫无反应；代码里那句 `viewModel.emitScrollBarModeChangedSharedFlow(value)` 看起来在"通知列表页"，实际上没有任何地方订阅。

**直接原因**：第 3 步把滚动条推迟到 material3 1.5 的官方组件（[A·迁移期观察记录](A-迁移期观察记录.md) / [10 章](10-第7步-清理收尾.md) 已记录为已知欠账），自绘滚动条与 `ViewExt.setScrollBarMode` 都已删除，但**发送端没有同步删掉**，也没有订阅端。

**根本原因**：把"偏好值"当成"一次性事件"来做同步——`SharedFlow` 事件总线必须"有人订阅才成立"，而订阅方（列表页）根本不存在。同一个仓库后面已经给出了正确范式：**过期排序开关**用的是 `OnSharedPreferenceChangeListener` 直接观察偏好键（`HomeViewModel.kt:66-108`），滚动条没有跟着改，成了两套并存的机制。

**问题代码**：

```kotlin
// SettingsViewModel.kt:36-44
val scrollBarModeChangedSharedFlow: SharedFlow<String?>
    field = MutableSharedFlow()          // replay = 0，无缓冲

fun emitScrollBarModeChangedSharedFlow(scrollBarMode: String?) {
    viewModelScope.launch {
        scrollBarModeChangedSharedFlow.emit(scrollBarMode)   // 没有订阅者
    }
}
```

```kotlin
// SettingsScreen.kt:117-121
onScrollBarSelect = { value ->
    scrollBarValue = value
    MyApplication.sharedPreferences.edit { putString(scrollBarKey, value) }
    viewModel.emitScrollBarModeChangedSharedFlow(value) // list pages pick it up immediately  ← 注释与事实不符
},
```

> 精确结论（已核对 `kotlinx-coroutines-core` 1.8.0 `SharedFlowImpl.tryEmitLocked`）：`nCollectors == 0` 且 `replay == 0` 时走 `tryEmitNoCollectorsLocked` 直接 `return true`——**事件被丢弃、不会挂起、也不泄漏协程**。所以它不是内存泄漏，但确实是一段"看起来在干活、实际什么也没做"的死链路，且每次切换都会白起一个协程。
>
> 2026-09-22 补：本项目实际解析到的是 `kotlinx-coroutines` **1.11.0**（`gradle/toml/kotlin.toml:9`），上面引用的 1.8.0 是复查当时翻的版本；"零订阅者即丢弃"的结论由本条现象自证（切换开关后界面毫无反应），但若要再引源码，请按 1.11.0 复核。

**修改方案**（三选一，推荐 A）：

- **A（最小改动，与 outdated-order 对齐）**：滚动条落地前先删掉 `scrollBarModeChangedSharedFlow` 与 `emitScrollBarModeChangedSharedFlow`，`onScrollBarSelect` 只写 SP，并把那条误导注释改成"滚动条实现推迟，此开关暂不生效"。将来落地滚动条时，让列表页自己观察该偏好键（照抄 `HomeViewModel` 的 `outdatedOrderChangeListener`）。
- **B（保留事件总线）**：至少要有人订阅，且改成 `MutableSharedFlow(replay = 1)`，否则切完再进列表页照样收不到。
- **C（顺手把坑填了）**：当前 BOM 已是 `2026.09.00`，建议先核一下 material3 是否已经转正 `Modifier.nonInteractiveScrollbar`；若已转正，直接用它把滚动条补上，F1 一并消失。

> **处置（2026-09-22 负责人定）**：A/B/C 都不采纳——保持代码现状，等 material3 官方滚动条转正后再接，不引 alpha、不自绘（背景与裁定见 [R10](../architecture-review-cn/05-已裁定事项.md#R10)）。A 里那条注释订正已单独落地（`SettingsScreen.kt:117-121` 现在说实话），死链路与设置项照原样保留。

---

### F2 · 加载异常会让下拉刷新永久转圈（P0，漏改）

**现象**：`collectModels()` 一旦抛异常（shell 失败、IO 异常、解析异常），`isLoadingStateFlow` 永远停在 `true` → PullToRefresh 指示器常驻、列表空白，用户只能杀进程。

**直接原因**：`setLoading(false)` 只在 `setModels()` 里调用；异常路径绕过了它。

**根本原因**：从旧 `BaseListViewModel.startLoad()` **原样搬过来**的"无错误处理"。旧版用 `SwipeRefreshLayout`，同样会卡（旧代码遗留 bug），但迁移时把它带进了 Compose：Compose 版把 `isLoading` 单独拆成一个 StateFlow 并直接喂给 `PullToRefreshBox(isRefreshing = ...)`，卡住的可见性比旧版更强。这是典型的"迁移只搬行为、没借机补洞"。

**问题代码**：

```kotlin
// BaseListViewModel.kt:55-66
private fun startLoad() {
    if (loadJob?.isActive == true) return

    loadJob = viewModelScope.launch {
        setLoading(true)
        val list = collectModels()   // ← 抛异常直接跳出，setLoading(false) 永远不执行
        setModels(list)              // ← setLoading(false) 藏在这里
        onModelsLoaded()
    }
}
```

**修改方案**：

```kotlin
loadJob = viewModelScope.launch {
    setLoading(true)
    try {
        val list = collectModels()
        setModels(list)          // 内部 setLoading(false)，可保留
        onModelsLoaded()
    } catch (e: CancellationException) {
        throw e                  // 取消必须重抛，不能吞
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) e.printStackTrace()
        // 可选：把错误暴露成 UI 状态（此处至少保证不再卡死）
    } finally {
        setLoading(false)        // 兜底：无论成功/失败/取消都收尾
    }
}
```

配套小改：把 `setModels()` 里的 `setLoading(false)` 删掉，让 `loading` 的开关只在一处出现，避免以后再被"藏起来的收尾"坑一次。

---

### F3 · Settings 在 Composable 里直接读写 SharedPreferences（P1，不合理）

**现象**：`SettingsScreen` 一次性做了三件事——读 SP、持有本地 `mutableStateOf`、在点击回调里写 SP 并调 `MyApplication.setMyTheme()`；而 `SettingsViewModel` 只负责版本信息。

**直接原因**：迁移时把旧 `SettingsFragment` 的"读 SP → 本地状态 → 写 SP"三段式 1:1 搬进了 Composable，没有顺手把写操作收进 ViewModel。

**根本原因**：**缺少 UI 状态契约**。现在的状态所有权是割裂的：值既存在 SP 里，又存在 4 个互不相关的 `remember { mutableStateOf(...) }` 里。后果有三：(1) 无法单测（组合里直接摸全局单例）；(2) 别处改了 SP（例如 `MyApplication.initTheme` 的省电模式迁移）UI 不会跟着变；(3) 以后加"偏好联动/校验/异步"没有落点。

**问题代码**：

```kotlin
// SettingsScreen.kt:112-130
onThemeSelect = { value ->
    themeValue = value
    MyApplication.sharedPreferences.edit { putString(themeKey, value) }   // UI 直接写存储
    MyApplication.setMyTheme(value)
},
onAllowNetworkChange = { value ->
    allowNetwork = value
    MyApplication.sharedPreferences.edit { putBoolean(allowNetworkKey, value) }
},
```

**修改方案**：

```kotlin
data class SettingsUiState(
    val themeValue: String = ...,
    val scrollBarValue: String = ...,
    val allowNetwork: Boolean = false,
    val outdatedOrderFirst: Boolean = false,
)

class SettingsViewModel(...) {
    val uiState: StateFlow<SettingsUiState> =
        preferenceFlow(...)                      // SP 变化 → 状态（可用 OnSharedPreferenceChangeListener 或 callbackFlow）
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setTheme(value: String) { write(KEY_THEME, value); MyApplication.setMyTheme(value) }
    fun setScrollBarMode(value: String) { write(KEY_SCROLL_BAR, value) }   // F1 一并解决
    fun setAllowNetwork(value: Boolean) { write(KEY_ALLOW_NETWORK, value) }
    fun setOutdatedOrderFirst(value: Boolean) { write(KEY_OUTDATED_ORDER, value) }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, ...) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(uiState = uiState, onThemeSelect = viewModel::setTheme, ...)
}
```

好处：Screen 变成纯"collect + 转发事件"；`MyApplication.sharedPreferences` 这个全局单例从 UI 层消失；"设置项继承"（回归清单 10.3-4）也变成可测的纯数据映射。

---

### F4 · 主题文件里的死代码与首次组合的额外开销（P1，改多）

**现象**：`Theme.kt` 定义了 6 套 `ColorScheme`，实际只用了 `lightScheme` / `darkScheme` 两套。

**直接原因**：Material Theme Builder 一键生成后只接了主配色，其余 4 套（medium/high contrast × light/dark）留在文件里没人删。

**根本原因**：生成物未经"用不用得着"的裁剪就入库；再加上 Kotlin 顶层属性的初始化语义——这些 `private val` 与 `AppTheme` **在同一个文件**里，编译产物是 `ThemeKt`，访问 `AppTheme` 会触发 `ThemeKt.<clinit>`，**6 套配色会在首次组合时一次性全部构造**（每套约 50 个颜色条目），而它们永远用不上。`unspecified_scheme` / `ColorFamily` 同理，还是 `public` 且是 snake_case（与项目命名规范冲突），等于对外暴露了一个没人用的 API。

**问题代码**：

```kotlin
// Theme.kt:108-258  4 套，全部零引用
private val mediumContrastLightColorScheme = lightColorScheme(...)
private val highContrastLightColorScheme   = lightColorScheme(...)
private val mediumContrastDarkColorScheme  = darkColorScheme(...)
private val highContrastDarkColorScheme    = darkColorScheme(...)

// Theme.kt:260-270  零引用 + public + snake_case
@Immutable data class ColorFamily(...)
val unspecified_scheme = ColorFamily(Color.Unspecified, ...)
```

（验证方式：`grep -rn "mediumContrast\|highContrast\|unspecified_scheme\|ColorFamily" app/src` 只命中定义处。）

**修改方案**：直接删除这 4 套配色 + `ColorFamily` + `unspecified_scheme`。真要做"系统高对比度"适配，一是另起文件，二是放到需要时再构造（`lazy` 或函数），不要挂在 `ThemeKt` 的静态初始化上。

---

### F5 · `Card(onClick = {})` 空点击（P1，改错）

**现象**：最新提交 `cb4104aa` 为了复刻旧 `MaterialCardView` 的水波纹，把卡片换成了 `onClick` 重载。

**直接原因**：旧 XML 确实是 `android:clickable="true" android:focusable="true"` 且**没有**点击监听——"可点击但没反应"是旧代码自带的毛病。

**根本原因**：把 View 时代的毛病当成了需求原样复刻。在 Compose 里 `Card(onClick = ...)` 不只是多一个水波纹：它会挂上 `clickable` 语义节点并引入 `minimumInteractiveComponentSize`，于是 TalkBack 会朗读"双击以激活"，用户照做却毫无反馈——**从"视觉小瑕疵"升级成了"无障碍缺陷"**。

**问题代码**：

```kotlin
// MyModelCard.kt:42-56  (cb4104aa)
Card(
    // Legacy MaterialCardView was clickable + focusable with no click listener = ripple-only feedback
    onClick = {},                       // ← 空点击
    modifier = modifier.fillMaxWidth().animateContentSize(),
    ...
)
```

**修改方案**（推荐 A）：

- **A**：卡片本就不需要交互，去掉 `onClick`，回到无点击重载：

  ```kotlin
  Card(
      modifier = modifier.fillMaxWidth().animateContentSize(),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
  ) { ... }
  ```

- **B**：确实想要水波纹，就只取水波纹、交出语义控制权：

  ```kotlin
  modifier = modifier
      .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = ripple(),
          onClick = {},
      )
      .clearAndSetSemantics {}   // 明确告诉无障碍服务：这不是一个可操作控件
  ```

  > 另注：`animateItem()` + `animateContentSize()` 同时用是官方推荐组合（前者管位移、后者管自身高度），这部分没问题；`Card(onClick)` 带来的 `minimumInteractiveComponentSize`（48dp）当前也不影响布局（卡片实际高度约 67dp > 48dp）。

---

### F6 · `onBack` 与多返回栈自相矛盾（P2，不合理）

**现象**：每个标签各自持有一条 `NavBackStack`，但返回键一律 `activity?.finish()`。

**直接原因**：决策 10 明确"保持旧行为（任何标签直接退出）"，旧版 Fragment show/hide 确实如此。

**根本原因**：迁移引入了"多返回栈"这一新能力，却没有同步更新返回语义。当前每个栈恒为 1 个 entry，行为等价；但只要任一标签 push 第二个页面（详情、WebView 等），返回键就会变成"直接退出 App"——这是典型的"改了一半"。

**问题代码**：

```kotlin
// AppRoot.kt:111-116
NavDisplay(
    entries = decoratedEntries[currentTabIndex],
    onBack = { activity?.finish() },   // 永远退出，从不 pop
    modifier = contentModifier,
)
```

**修改方案**：

```kotlin
onBack = {
    val stack = backStacks.getValue(topLevelTabs[currentTabIndex].key)
    if (stack.size > 1) stack.removeLastOrNull()   // 站内返回
    else activity?.finish()                        // 栈顶才退出，保持旧观感
},
```

> 落地前请按当前 navigation3 版本确认 `onBack` 的精确签名（本项目用的是 `entries:` 重载，`onBack` 为无参 lambda；若版本已改为带"返回次数"参数，按新签名取用）。

---

### F7 · ~~`toColoredMyModel(..., Boolean)` 重载零调用~~ **已撤回（2026-09-22 复验）**

**撤回原因**：本条的事实前提「`grep` 只命中定义处」在复查当天就不成立。`MyModelExt.kt:8-15` 的 Boolean 重载有 **6 处调用点**，全在 `HomeRepository`：

| 调用点 | 传入的布尔实参 |
|---|---|
| `HomeRepository.kt:386` | `isAbEnable` |
| `HomeRepository.kt:486` | `isDynamicPartitionsEnabled` |
| `HomeRepository.kt:589` | `isDsuEnabled` |
| `HomeRepository.kt:726` | `isDeveloperOptionsDisabled` |
| `HomeRepository.kt:740` | `isAdbDebuggingDisabled` |
| `HomeRepository.kt:751` | `isAdbAuthenticationEnabled` |

其余 16 处调用传的是 `StatusColor`（`color` / `*Color` 变量），走另一个重载。**按本条原方案「直接删除」会让这 6 处编译失败**，故整条撤回。

**仍然成立的判断**：两个同名重载靠 `Boolean` / `StatusColor` 区分，读调用点时看不出语义。若要收敛，正确顺序是先把上面 6 处改成显式 `StatusColor`（或改名 `toPassFailMyModel`），再删重载——不是当死代码删。

---

### F8 · `ExtendedColors.of()` 多标了 `@Composable`（P2，改多）

**现象**：一个纯映射函数被声明成 `@Composable`。

**直接原因**：大概率是"它返回颜色、在组合里用"于是顺手加了注解。

**根本原因**：对 `@Composable` 的语义理解有偏差——它表示"这个函数会往组合里写东西"。后果：只能在组合上下文调用（想在 `@Composable` 之外的映射/预览数据构造里用就直接编译不过），并且多引入一层重组作用域，纯属自缚手脚。

**问题代码**：

```kotlin
// ExtendedColors.kt:43-49
@Composable
fun ExtendedColors.of(status: StatusColor): Color = when (status) { ... }
```

**修改方案**：去掉 `@Composable` 即可（`MyModelCard.kt:78` 调用处无需改动）。

---

### F9 · `MyModel.key` 用标题文本，重复即崩溃（P2，隐患）

**现象**：`LazyColumn` 的 `key` 取自 `MyModel.key`；`Raw` 标题的 key 就是标题文本本身。一旦同页出现两个相同标题，`LazyColumn` 会直接抛 `IllegalArgumentException: Key was already used`——而旧版 `DiffUtil.areItemsTheSame` 只会表现怪异、不会崩。

**直接原因**：Compose 的 key 契约比 DiffUtil 严格（[A·迁移期观察记录](A-迁移期观察记录.md) 已记录了这点和"当前三页无碰撞"的结论）。

**根本原因**：key 承担了两个职责（列表项身份 + 动画/滚动状态锚点），却复用了"业务标题"这个天然可能重复的值，且代码里没有任何兜底。

**问题代码**：

```kotlin
// MyModel.kt:24-28
val key: String
    get() = when (title) {
        is MyModelTitle.Res -> title.id.toString()
        is MyModelTitle.Raw -> title.text        // ← 可能重复
    }
```

```kotlin
// MyModelListScreen.kt:106
key = { it.key },
```

**现状评估**：Prop 页三块数据源天然不冲突（JVM 系统属性 / `Settings` 带类名前缀的键 / `getprop` 输出），Others、Home 用的是 `@StringRes`，目前是安全的——所以定级 P2 而非 P0。

**修改方案**（任选）：

1. 组合 key，把"类型"纳入身份：`key = { "${it.type}:${it.key}" }`（最小改动，能挡住跨类型碰撞）；
2. 在数据源侧去重（`distinctBy { it.key }`），并在 `MyModelTitle.Raw` 的构造处加注释说明"必须唯一"；
3. 终极方案是给 `MyModel` 增加一个显式 `id` 字段，让 key 不再依赖展示文本。

---

### F10 · `@Stable` 标在了不需要的地方，且它是一份长期承诺（P3，改多）

**现象**：`BaseListViewModel` / `HomeViewModel` / `SettingsViewModel` 都标了 `@Stable`，`OthersViewModel` / `PropViewModel` 没标。

**直接原因**：逐个提交做的"稳定性优化"（提交 `494b8f28` / `05f7311d` / `42eb3e95`），逐个类补注解。

**根本原因**：没弄清稳定性由**声明类型**决定。三个列表页调用的是 `MyModelListScreen(viewModel: BaseListViewModel)`——参数声明类型就是基类，`@Stable` 标在基类上已经足够；`HomeViewModel` 上那份是**冗余**的（`SettingsViewModel` 那份有效，因为 `SettingsScreen` 的参数声明类型就是它）。

**风险提示**：`@Stable` 是"所有公开属性永不静默变化"的承诺。目前各 VM 的可变字段都是 `private`（`loadJob`、`timesLeft`、`loadStartGeneration`），承诺成立；但一旦有人往 VM 上加一个 `var`，Compose 会因为"类型稳定"跳过重组，产生极难排查的状态不同步。

**修改方案**：保留 `BaseListViewModel` / `SettingsViewModel` 上的注解，删掉 `HomeViewModel` 上冗余的那份；把注释里的理由从"实例不变"改成"公开属性都为 StateFlow 或 private var，满足 @Stable 契约"，避免后人误读为"给 ViewModel 加 @Stable 总是安全的"。

---

### F11 · 每个标签都注册了全部 4 个 entry；`currentTabIndex` 无越界保护（P3，隐患）

**现象**：`topLevelTabs.map { rememberDecoratedNavEntries(...) }` 里，4 个标签各自的 `entryProvider` 都包含全部 4 个 entry（实际只会用到 1 个）；`decoratedEntries[currentTabIndex]` 直接下标取值。

**直接原因**：多返回栈配方（nav3-recipes）的模板写法，为了保持 `remember` 调用顺序稳定而统一构造。

**根本原因**：模板照搬后没有按本项目的实际形态收敛。风险点：`currentTabIndex` 是 `rememberSaveable` 持久化的，将来若减少标签数量，老用户升级后读到的旧索引会越界 → `IndexOutOfBoundsException` 冷启动崩溃。

**修改方案**：

```kotlin
val index = currentTabIndex.coerceIn(0, topLevelTabs.lastIndex)
...
entries = decoratedEntries[index]
```

entryProvider 的冗余可以不改（改动反而会破坏 `remember` 的稳定性），但建议加一行注释说明"四个 provider 内容相同是有意为之"。

---

### F12 · 图标 `fillColor` 硬编码黑色，正确性依赖 `Icon` 的默认 tint（P3，隐性依赖）

**现象**：4 个导航图标把 `?attr/colorOnSurface` 改成了 `#FF000000`。

```diff
-        android:fillColor="?attr/colorOnSurface"
+        android:fillColor="#FF000000"
```

**评估**：**方向是对的**——XML 主题现在只管窗口外壳，不可能跟着 App 内主题（例如"系统浅色 + 应用强制深色"）走，保留 `?attr` 反而会取到错误的颜色。深色模式下不会变成"黑底黑图标"，因为 `Icon(painterResource(...))` 默认 `tint = LocalContentColor.current`，而 `NavigationBarItem` 会为图标槽位提供正确的 `LocalContentColor`，`ColorFilter.tint` 会整体替换 RGB。

**风险**：正确性**完全依赖**"必须用 `Icon` 承载"。哪天把同一个 drawable 放到 `Image(painter = painterResource(...))` 或不带 `colorFilter` 的地方，立刻变成纯黑且深浅色都不对。

**修改方案**：保持现状，在 `AppRoot.kt` 的 `Icon(...)` 处补一行注释："图标资源为纯黑，颜色由 Icon 的默认 tint（LocalContentColor）决定，勿改用无 tint 的载体"。

---

### F13 · 计划文档已落后于代码（P3，文档欠账）

`AGENTS.md` 的规矩是"文档与代码冲突时信代码并改文档"。以下三处已经对不上：

| 文档 | 现状 |
| --- | --- |
| README 决策 9："MainActivity **暂留** AppCompatActivity" | 早已是 `ComponentActivity`（`733c6941`、`26b9094f`） |
| 10.2："`appcompat` 与 `material`（MDC）**都暂留**" | 均已删除（`f66562d4`），`AppCompatDelegate`/`DynamicColors` 已从代码里消失 |
| 10.4 遗留优化 2："主题模式去 AppCompat 化" | 已完成，应从"遗留优化"移到"已完成" |

另外 README 决策 6 写的是"实际：砍掉设置项"，但设置项**仍在 UI 里**（只是失效）——文档与代码各说各话，正好是 F1 的成因，建议一并订正。

---

## 三、已核对通过的项（避免"看着像问题其实没问题"）

复查不是只找茬，下面这些**特意验证过、结论是"没改错"**，供放心的同时也作为以后回归的基准：

1. **状态色映射 1:1 无错位**：`HomeRepository.kt` 旧 `R.attr.colorNoProblem/Warning/Critical` → 新 `StatusColor.NO_PROBLEM/WARNING/CRITICAL`，逐项计数（19 / 18 / 31）与**出现顺序完全一致**，不存在把 WARNING 换成 CRITICAL 之类的手滑。
2. **扩展色数值与旧资源逐字节一致**：`Color(0xD0_ACDDB7 / 0xD0_FDD18F / 0xD0_FFB1AC)`、夜间 `0xD0_2B5128 / 0xD0_7E581F / 0xD0_812F2F` 与已删除的 `colors.xml`（含 `values-night`）完全相同。
3. **排版对齐**：`Type.kt` 用 20sp Bold / 16sp + `includeFontPadding = true`，与旧 `my_view_holder.xml` 两个 TextView 一致（含"Compose 默认已改为 false"这个坑）。
4. **卡片视觉对齐**：`surfaceBright` 容器色、`elevation 0`、M3 默认 medium 圆角、`item_card_padding = 10dp`、间距 `12dp` 均与旧 XML 一致。
5. **偏好默认值一致**：主题默认 `-1`（跟随系统）、滚动条默认 `0`（无），与旧 `preferences.xml` 的 `app:defaultValue` 相同；存储文件名仍是 `${packageName}_preferences`，老用户设置继承无虞。
6. **清理清单已落地**：Fragment 壳 4 个、`main_activity.xml`、`bottom_nav_menu.xml`、`MainViewModel`、`drop_scale.xml`、`MyAdapter/MyViewHolder/MyItemDecoration`、`ViewExt.kt`、`StateExt.kt`（`State` 密封类）、`preferences.xml` 等均已删除，无"删一半"的残留引用。
7. **主题链路自洽**：`AppThemeMode.isDark()` 一处定义，`MainActivity`（窗口外壳 + edge-to-edge）与 `AppTheme`（配色）共用，不存在两处判断漂移的风险；`enableEdgeToEdge` 传 `::isAppDark` 也是对的（默认 `detectDarkMode` 只看系统 uiMode，与"应用强制深色"不一致）。
8. **`MyApplication.initTheme` 的省电模式迁移**：把遗留值 `1` 归一化回"跟随系统"**并写回 SP**，Settings 页后续读到的就是干净值，不靠每次启动兜底——这处处理是对的。
9. **Home 过期排序开关**：`OnSharedPreferenceChangeListener` + "加载开始/落地" 两代次比对（`loadStartGeneration`），覆盖了"刷新期间被切换"的竞态，比旧的 `SharedFlow` 广播可靠。

---

## 四、建议处理顺序

1. **先修 P0**：F2（`try/finally`，改动 6 行，直接消除"卡死转圈"）、F1（删死链路 + 修正误导注释）。
2. **再修 P1**：F5（去掉空 `onClick`，提交 `cb4104aa` 刚引入，最好就地回改）、F4（删死代码）、F3（Settings 状态收进 VM，量最大，可单独一个 PR）。
3. **随后 P2/P3**：F6、F8、F9、F11 都是十行以内的小改，可以攒一个"收尾 PR"。（F7 已撤回，不在内。）
4. **文档**：F13 三处订正 + 在 [A·迁移期观察记录](A-迁移期观察记录.md) 里补记 F1/F2 两条"迁移期发现"。

> 建议每个修复都跑一次 `./gradlew assembleFossDebug` + 过一遍 [10 章](10-第7步-清理收尾.md) 的回归清单 10.3（尤其是第 3 条滚动条、第 5 条主题、第 6 条导航）。
