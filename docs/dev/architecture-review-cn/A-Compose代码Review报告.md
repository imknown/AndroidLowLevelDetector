# Compose 迁移后代码复查报告

> 2026-09-25 起收录于 [项目问题汇总](README.md) 作为附录. 本报告由**三份独立复查合并**而成 — 同一批迁移后代码, 三个模型各自找茬, 同条发现以最详版本为主体, 另一份的独有贡献以 "另一份复查的补充" 就地标注; 三份原文 (Hy4-preview / Qwen3.8-Flash / GLM-5.3-Flash 单行本) 已被本文件取代, 记录见 git 历史.
>
> 编号规则 (与[附录 A](README.md#appendix-a) 的映射一致): **F1~F13** = Hy4-preview 原编号; **F14 / F15** = Qwen3.8-Flash 独有条目并入时的补编; **F16** 起 = GLM-5.3-Flash 的新发现. 每节标题下标注发现者与当前状态.
>
> 定位: **迁移完成后的整体复查** (第 0~7 步全部落地之后), 关注 "改错的 / 改多的 / 漏改的 / 不合理的 / 有更好改法的", 每条给出直接原因 → 根本原因 → 问题代码 → 修改方案. 开放条目由 [README 总览表](README.md#问题总览表) 统一跟踪.

## 一, 三次复查与结论速览

| 轮次 | 模型 | 复查时点 | 基线 | 贡献 |
| --- | --- | --- | --- | --- |
| 1 | Hy4-preview | 2026-09-22 定稿 (含当日复验更正) | `cb4104aa` (迁移系列 35 个提交, 起点 `58d22787`) | F1~F13; 9 条 "已核对通过" 基准 |
| 2 | Qwen3.8-Flash | 2026-09-21 | 同上分支 | 独有发现 2 条 (补编 F14/F15); 6 条 "复核无需改动" 基准 |
| 3 | GLM-5.3-Flash | 2026-09-25 | `422b500e` (距轮次 1 基线又前进 68 个提交) | 新发现 F16; 对全部 15 个发现的逐条复核 (见[第三节](#三-2026-09-25-复核-开放发现的当前代码证据)); 9 条新增通过项 |

三次复查的编译状态均为 **BUILD SUCCESSFUL** (`./gradlew compileFossDebugKotlin`; 轮次 1 另注明: 迁移过程中留下的三处编译报错在其当时 HEAD 已全部消失, 属中间态). 范围: UI 层 (Compose 组合 / 主题 / 导航 / 状态与副作用) + 与数据层的接缝; 数据层内部问题归 [AR 系列](README.md) 跟踪. 对照物: `git show 58d22787:<旧文件>` 的 View 时代实现 + 计划文档与 "关键决策点汇总".

截至 2026-09-25 的合计结论: **16 个编号中, 12 条开放, 2 条已裁定保留 (F1 / F5), 1 条已撤回 (F7), 1 条已修复 (F13)**.

| # | 类别 | 严重度 | 位置 | 一句话 | 状态 |
| --- | --- | --- | --- | --- | --- |
| F1 | 改错 / 漏改 | P0 | `SettingsViewModel.scrollBarModeChangedSharedFlow 与 emitScrollBarModeChangedSharedFlow()`, `SettingsScreen() 的 onScrollBarSelect` | 滚动条事件总线**零订阅者**, `emit` 是静默空转 | 已裁定保留 (2026-09-22, 同 [R10](05-已裁定事项.md#R10)) |
| F2 | 漏改 (继承自旧代码) | P0 | `BaseListViewModel.startLoad()` | 加载抛异常时 `isLoading` 永久停在 `true`, 下拉刷新转圈停不下来 | 开放 |
| F3 | 不合理 / 更好的改法 | P1 | `SettingsScreen() 的状态与回调整块` | Composable 里直接读写 `SharedPreferences` 并调 `MyApplication.setMyTheme`, 绕过 ViewModel | 开放 |
| F4 | 改多 | P1 | `Theme.kt` 四套对比度 colorScheme + `Color.kt` 对应常量 | 4 套对比度配色 + `ColorFamily`/`unspecified_scheme` 全部无人使用, 且在 `ThemeKt` 静态初始化里被一并构造 | 开放 |
| F5 | 改错 (a11y) | P1 | `MyModelCard() 的 Card(onClick = {})` | 空点击: TalkBack 报 "可双击激活" 却无反应 | 已裁定保留 (2026-09-22, 见 F5 处置) |
| F6 | 不合理 | P2 | `AppRoot() 调 AppRootShell 的那段` | `onBack = { activity?.finish() }` 与 "每标签一条返回栈" 自相矛盾 | 开放 |
| F7 | ~~改多~~ | — | `toColoredMyModel(condition: Boolean) 重载` | ~~重载零调用~~ — 复验有 6 处调用 | 已撤回 (2026-09-22) |
| F8 | 改多 | P2 | `ExtendedColors.of()` | `of()` 不需要 `@Composable`, 白白限制调用场景 | 开放 |
| F9 | 隐患 | P2 | `MyModel.key` + `MyModelListContent() 的 items(key = { it.key })` | `key` 用标题文本, `Raw` 标题一旦重复 LazyColumn 直接抛异常 (旧 DiffUtil 不崩) | 开放 |
| F10 | 改多 / 契约风险 | P3 | `HomeViewModel` 等 | `@Stable` 标在子类上是冗余的, 且 `@Stable` 是一份 "永不失效" 的承诺 | 开放 |
| F11 | 隐患 | P3 | `AppRoot() 的 decoratedEntries` | 每个标签都注册了全部 4 个 entry; `currentTabIndex` 无越界保护 | 开放 |
| F12 | 隐性依赖 | P3 | `ic_*_24dp.xml` (4 个) | 图标 `fillColor` 硬编码 `#FF000000`, 正确性完全依赖 `Icon` 的默认 tint | 开放 (注释未补) |
| F13 | 文档欠账 | P3 | 本迁移计划 README / 09 章 | 决策 9 等三处文档与代码脱节 | 已修 (2026-09-22 汇总块) |
| F14 | 改错 / 数据不一致 | P1 · 高 | `ui/settings/res/values/arrays.xml` | 滚动条选项数组 keys=2 / values=3 错位, zip 截断暂时掩盖 | 开放 |
| F15 | 更好的改法 | P2 | `MyModelListScreen.kt` | `toPersistentList()` 在每次重组都重新分配 | 开放 |
| F16 | 漏改 (继承自旧代码) / 更好的改法 | P3 | `MyModelCard() 的状态圆点` | 状态只靠颜色编码且零语义 — TalkBack 用户听不到任何状态信息 | 开放 |

---

## 二, 问题详解

<a id="F1"></a>

### F1 · 滚动条事件总线零订阅者, `emit` 是静默空转 (P0, 改错 + 漏改)

> 发现: Hy4-preview (F1) = Qwen3.8-Flash (#2, 同条; 其 "两套偏好传播机制并存" 的 #6 也归入本条处理). **已裁定保留** (2026-09-22), 2026-09-25 复核与裁定一致.

**现象**: 设置页切换 "滚动条模式" → 值写进了 SharedPreferences, 但列表页毫无反应; 代码里那句 `viewModel.emitScrollBarModeChangedSharedFlow(value)` 看起来在 "通知列表页", 实际上没有任何地方订阅.

**直接原因**: 第 3 步把滚动条推迟到 material3 1.5 的官方组件 ([A·迁移期观察记录](../compose-migration-plan-cn/A-迁移期观察记录.md) / [09 章](../compose-migration-plan-cn/09-第7步-清理收尾.md) 已记录为已知欠账), 自绘滚动条与 `ViewExt.setScrollBarMode` 都已删除, 但**发送端没有同步删掉**, 也没有订阅端.

**根本原因**: 把 "偏好值" 当成 "一次性事件" 来做同步 — `SharedFlow` 事件总线必须 "有人订阅才成立", 而订阅方 (列表页) 根本不存在. 同一个仓库后面已经给出了正确范式: **过期排序开关**用的是 `OnSharedPreferenceChangeListener` 直接观察偏好键 (`HomeViewModel.outdatedOrderChangeListener`), 滚动条没有跟着改, 成了两套并存的机制 (Qwen #6).

**问题代码**:

```kotlin
// SettingsViewModel.scrollBarModeChangedSharedFlow 与 emitScrollBarModeChangedSharedFlow()
val scrollBarModeChangedSharedFlow: SharedFlow<String?>
    field = MutableSharedFlow()          // replay = 0, 无缓冲

fun emitScrollBarModeChangedSharedFlow(scrollBarMode: String?) {
    viewModelScope.launch {
        scrollBarModeChangedSharedFlow.emit(scrollBarMode)   // 没有订阅者
    }
}
```

```kotlin
// SettingsScreen.kt → SettingsScreen() 的 onScrollBarSelect(复查当时的原文; 那行注释已于 09d30573 改为实话)
onScrollBarSelect = { value ->
    scrollBarValue = value
    MyApplication.sharedPreferences.edit { putString(scrollBarKey, value) }
    viewModel.emitScrollBarModeChangedSharedFlow(value) // list pages pick it up immediately  ← 注释与事实不符
},
```

> 精确结论 (已核对 `kotlinx-coroutines-core` 1.8.0 `SharedFlowImpl.tryEmitLocked`): `nCollectors == 0` 且 `replay == 0` 时走 `tryEmitNoCollectorsLocked` 直接 `return true` — **事件被丢弃, 不会挂起, 也不泄漏协程**. 所以它不是内存泄漏, 但确实是一段 "看起来在干活, 实际什么也没做" 的死链路, 且每次切换都会白起一个协程.
>
> 2026-09-22 补: 本项目实际解析到的是 `kotlinx-coroutines` **1.11.0**(`gradle/toml/kotlin.toml → kotlinx-coroutines`), 上面引用的 1.8.0 是复查当时翻的版本; "零订阅者即丢弃" 的结论由本条现象自证 (切换开关后界面毫无反应), 但若要再引源码, 请按 1.11.0 复核.

**修改方案** (三选一, 推荐 A):

- **A (最小改动, 与 outdated-order 对齐)**: 滚动条落地前先删掉 `scrollBarModeChangedSharedFlow` 与 `emitScrollBarModeChangedSharedFlow`, `onScrollBarSelect` 只写 SP, 并把那条误导注释改成 "滚动条实现推迟, 此开关暂不生效". 将来落地滚动条时, 让列表页自己观察该偏好键 (照抄 `HomeViewModel` 的 `outdatedOrderChangeListener`).
- **B (保留事件总线)**: 至少要有人订阅, 且改成 `MutableSharedFlow(replay = 1)`, 否则切完再进列表页照样收不到.
- **C (顺手把坑填了)**: 当前 BOM 已是 `2026.09.00`, 建议先核一下 material3 是否已经转正 `Modifier.nonInteractiveScrollbar`; 若已转正, 直接用它把滚动条补上, F1 一并消失.

> **处置 (2026-09-22 负责人定)**: A/B/C 都不采纳 — 保持代码现状, 等 material3 官方滚动条转正后再接, 不引 alpha, 不自绘 (背景与裁定见 [R10](05-已裁定事项.md#R10)). A 里那条注释订正已单独落地 (`SettingsScreen() 的 onScrollBarSelect` 现在说实话), 死链路与设置项照原样保留.

---

<a id="F2"></a>

### F2 · 加载异常会让下拉刷新永久转圈 (P0, 漏改)

> 发现: Hy4-preview (F2). 2026-09-25 复核: **仍开放** (见[第三节](#三-2026-09-25-复核-开放发现的当前代码证据)).

**现象**: `collectModels()` 一旦抛异常 (shell 失败, IO 异常, 解析异常), `isLoadingStateFlow` 永远停在 `true` → PullToRefresh 指示器常驻, 列表空白, 用户只能杀进程.

**直接原因**: `setLoading(false)` 只在 `setModels()` 里调用; 异常路径绕过了它.

**根本原因**: 从旧 `BaseListViewModel.startLoad()` **原样搬过来**的 "无错误处理". 旧版用 `SwipeRefreshLayout`, 同样会卡 (旧代码遗留 bug), 但迁移时把它带进了 Compose: Compose 版把 `isLoading` 单独拆成一个 StateFlow 并直接喂给 `PullToRefreshBox(isRefreshing = ...)`, 卡住的可见性比旧版更强. 这是典型的 "迁移只搬行为, 没借机补洞".

**问题代码**:

```kotlin
// BaseListViewModel.startLoad()
private fun startLoad() {
    if (loadJob?.isActive == true) return

    loadJob = viewModelScope.launch {
        setLoading(true)
        val list = collectModels()   // ← 抛异常直接跳出, setLoading(false) 永远不执行
        setModels(list)              // ← setLoading(false) 藏在这里
        onModelsLoaded()
    }
}
```

**修改方案**:

```kotlin
loadJob = viewModelScope.launch {
    setLoading(true)
    try {
        val list = collectModels()
        setModels(list)          // 内部 setLoading(false), 可保留
        onModelsLoaded()
    } catch (e: CancellationException) {
        throw e                  // 取消必须重抛, 不能吞
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) e.printStackTrace()
        // 可选: 把错误暴露成 UI 状态 (此处至少保证不再卡死)
    } finally {
        setLoading(false)        // 兜底: 无论成功/失败/取消都收尾
    }
}
```

配套小改: 把 `setModels()` 里的 `setLoading(false)` 删掉, 让 `loading` 的开关只在一处出现, 避免以后再被 "藏起来的收尾" 坑一次.

---

<a id="F3"></a>

### F3 · Settings 在 Composable 里直接读写 SharedPreferences (P1, 不合理)

> 发现: Hy4-preview (F3). 2026-09-25 复核: **仍开放**; 与 [AR-02](02-SSOT-唯一数据来源.md#AR-02) 同根, 与其 `SettingsStore` 方案合流实施最省.

**现象**: `SettingsScreen` 一次性做了三件事 — 读 SP, 持有本地 `mutableStateOf`, 在点击回调里写 SP 并调 `MyApplication.setMyTheme()`; 而 `SettingsViewModel` 只负责版本信息.

**直接原因**: 迁移时把旧 `SettingsFragment` 的 "读 SP → 本地状态 → 写 SP" 三段式 1:1 搬进了 Composable, 没有顺手把写操作收进 ViewModel.

**根本原因**: **缺少 UI 状态契约**. 现在的状态所有权是割裂的: 值既存在 SP 里, 又存在 4 个互不相关的 `remember { mutableStateOf(...) }` 里. 后果有三: (1) 无法单测 (组合里直接摸全局单例); (2) 别处改了 SP (例如 `MyApplication.initTheme` 的省电模式迁移) UI 不会跟着变; (3) 以后加 "偏好联动/校验/异步" 没有落点.

**问题代码**:

```kotlin
// SettingsScreen.kt → SettingsScreen() 的各 onXxxSelect 回调
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

**修改方案**:

```kotlin
data class SettingsUiState(
    val themeValue: String = ...,
    val scrollBarValue: String = ...,
    val allowNetwork: Boolean = false,
    val outdatedOrderFirst: Boolean = false,
)

class SettingsViewModel(...) {
    val uiState: StateFlow<SettingsUiState> =
        preferenceFlow(...)                      // SP 变化 → 状态 (可用 OnSharedPreferenceChangeListener 或 callbackFlow)
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

好处: Screen 变成纯 "collect + 转发事件"; `MyApplication.sharedPreferences` 这个全局单例从 UI 层消失; "设置项继承" (回归清单 9.3-4) 也变成可测的纯数据映射.

---

<a id="F4"></a>

### F4 · 主题文件里的死代码与首次组合的额外开销 (P1, 改多)

> 发现: Hy4-preview (F4) = Qwen3.8-Flash (#3, 同条; Color.kt 的对比度常量为其独有贡献). 2026-09-25 复核: **仍开放**.

**现象**: `Theme.kt` 定义了 6 套 `ColorScheme`, 实际只用了 `lightScheme` / `darkScheme` 两套.

**直接原因**: Material Theme Builder 一键生成后只接了主配色, 其余 4 套 (medium/high contrast × light/dark) 留在文件里没人删.

**根本原因**: 生成物未经 "用不用得着" 的裁剪就入库; 再加上 Kotlin 顶层属性的初始化语义 — 这些 `private val` 与 `AppTheme` **在同一个文件**里, 编译产物是 `ThemeKt`, 访问 `AppTheme` 会触发 `ThemeKt.<clinit>`, **6 套配色会在首次组合时一次性全部构造** (每套约 50 个颜色条目), 而它们永远用不上. `unspecified_scheme` / `ColorFamily` 同理, 还是 `public` 且是 snake_case (与项目命名规范冲突), 等于对外暴露了一个没人用的 API.

**问题代码**:

```kotlin
// Theme.kt 里四套对比度 colorScheme  4 套, 全部零引用
private val mediumContrastLightColorScheme = lightColorScheme(...)
private val highContrastLightColorScheme   = lightColorScheme(...)
private val mediumContrastDarkColorScheme  = darkColorScheme(...)
private val highContrastDarkColorScheme    = darkColorScheme(...)

// Theme.kt 里的 ColorFamily / unspecified_scheme  零引用 + public + snake_case
@Immutable data class ColorFamily(...)
val unspecified_scheme = ColorFamily(Color.Unspecified, ...)
```

**另一份复查的补充 (Qwen)**: `ui/theme/Color.kt` 里 `...LightMediumContrast` / `...HighContrast` / `...DarkMediumContrast` / `...DarkHighContrast` 约 130+ 个颜色常量仅被上述死方案引用 — 这是 219 行文件里的绝大部分 (2026-09-25 复核 grep 计数: 140 行).

(验证方式: `grep -rn "mediumContrast\|highContrast\|unspecified_scheme\|ColorFamily" app/src` 只命中定义处.)

**修改方案**: 直接删除这 4 套配色 + `ColorFamily` + `unspecified_scheme`, 并删除 `Color.kt` 里对应的 contrast 颜色常量, 只保留 `lightScheme`/`darkScheme` 实际用到的两套 + `ExtendedColors`. 真要做 "系统高对比度" 适配, 一是另起文件, 二是放到需要时再构造 (`lazy` 或函数), 不要挂在 `ThemeKt` 的静态初始化上. 收益: 两文件合计从 ~750 行降到 ~260 行, 消除 IDE 未用告警.

---

<a id="F5"></a>

### F5 · `Card(onClick = {})` 空点击 (P1, 改错)

> 发现: Hy4-preview (F5) = Qwen3.8-Flash (#5, 同条). **已裁定保留** (2026-09-22), 2026-09-25 复核: 卡片注释与裁定一致.

**现象**: 最新提交 `cb4104aa` 为了复刻旧 `MaterialCardView` 的水波纹, 把卡片换成了 `onClick` 重载.

**直接原因**: 旧 XML 确实是 `android:clickable="true" android:focusable="true"` 且**没有**点击监听 — "可点击但没反应" 是旧代码自带的毛病.

**根本原因**: 把 View 时代的毛病当成了需求原样复刻. 在 Compose 里 `Card(onClick = ...)` 不只是多一个水波纹: 它会挂上 `clickable` 语义节点并引入 `minimumInteractiveComponentSize`, 于是 TalkBack 会朗读 "双击以激活", 用户照做却毫无反馈 — **从 "视觉小瑕疵" 升级成了 "无障碍缺陷"**.

**问题代码**:

```kotlin
// MyModelCard.kt → MyModelCard() 的 Card(onClick = {})  (cb4104aa 当时行号; 加了处置注释后是 42-60)
Card(
    // Legacy MaterialCardView was clickable + focusable with no click listener = ripple-only feedback
    onClick = {},                       // ← 空点击
    modifier = modifier.fillMaxWidth().animateContentSize(),
    ...
)
```

**修改方案** (推荐 A):

- **A**: 卡片本就不需要交互, 去掉 `onClick`, 回到无点击重载:

  ```kotlin
  Card(
      modifier = modifier.fillMaxWidth().animateContentSize(),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
  ) { ... }
  ```

- **B**: 确实想要水波纹, 就只取水波纹, 交出语义控制权:

  ```kotlin
  modifier = modifier
      .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = ripple(),
          onClick = {},
      )
      .clearAndSetSemantics {}   // 明确告诉无障碍服务: 这不是一个可操作控件
  ```

  **另一份复查的补充 (Qwen)**: B 的等价写法也可以用 `Modifier.combinedClickable(interactionSource = ..., indication = LocalIndication.current, onClick = {})`(或 `Modifier.indication(...)` + 只读 `interactionSource`) — 既能出波纹又不会被识别为 button.

  > 另注: `animateItem()` + `animateContentSize()` 同时用是官方推荐组合 (前者管位移, 后者管自身高度), 这部分没问题; `Card(onClick)` 带来的 `minimumInteractiveComponentSize` (48dp) 当前也不影响布局 (卡片实际高度约 67dp > 48dp).

> **处置 (2026-09-22 负责人定)**: A, B 都不采纳 — **保留现在的水波纹**, `Card(onClick = {})` 不动; a11y 语义问题按 "已知接受项" 就地标记 (`MyModelCard() 里那段 onClick 注释` 的注释已写明本条, 影响面与出路); 点击行展开详情 (BL-1)**明确暂不做**, 所以也就没有 "给 `onClick` 一个真动作" 的需求, B 方案作为将来真要修 a11y 时的参考保留在上面.

---

<a id="F6"></a>

### F6 · `onBack` 与多返回栈自相矛盾 (P2, 不合理)

> 发现: Hy4-preview (F6). 2026-09-25 复核: **仍开放**.

**现象**: 每个标签各自持有一条 `NavBackStack`, 但返回键一律 `activity?.finish()`.

**直接原因**: 决策 10 明确 "保持旧行为 (任何标签直接退出)", 旧版 Fragment show/hide 确实如此.

**根本原因**: 迁移引入了 "多返回栈" 这一新能力, 却没有同步更新返回语义. 当前每个栈恒为 1 个 entry, 行为等价; 但只要任一标签 push 第二个页面 (详情, WebView 等), 返回键就会变成 "直接退出 App" — 这是典型的 "改了一半".

**问题代码**:

```kotlin
// AppRoot.kt → AppRoot() 调 AppRootShell 的那段
NavDisplay(
    entries = decoratedEntries[currentTabIndex],
    onBack = { activity?.finish() },   // 永远退出, 从不 pop
    modifier = contentModifier,
)
```

**修改方案**:

```kotlin
onBack = {
    val stack = backStacks.getValue(topLevelTabs[currentTabIndex].key)
    if (stack.size > 1) stack.removeLastOrNull()   // 站内返回
    else activity?.finish()                        // 栈顶才退出, 保持旧观感
},
```

> 落地前请按当前 navigation3 版本确认 `onBack` 的精确签名 (本项目用的是 `entries:` 重载, `onBack` 为无参 lambda; 若版本已改为带 "返回次数" 参数, 按新签名取用).

---

<a id="F7"></a>

### F7 · ~~`toColoredMyModel(..., Boolean)` 重载零调用~~ **已撤回 (2026-09-22 复验)**

> 发现: Hy4-preview (F7). 状态: **已撤回**, 保留记录防止误当成欠账.

**撤回原因**: 本条的事实前提 "`grep` 只命中定义处" 在复查当天就不成立. `toColoredMyModel(condition: Boolean) 重载` 的 Boolean 重载有 **6 处调用点**, 全在 `HomeRepository`:

| 调用点 | 传入的布尔实参 |
|---|---|
| `HomeRepository.detectAb()` | `isAbEnable` |
| `HomeRepository.detectDynamicPartitions()` | `isDynamicPartitionsEnabled` |
| `HomeRepository.detectDsu()` | `isDsuEnabled` |
| `HomeRepository.detectDeveloperOptions()` | `isDeveloperOptionsDisabled` |
| `HomeRepository.detectAdb()` | `isAdbDebuggingDisabled` |
| `HomeRepository.detectAdbAuthentication()` | `isAdbAuthenticationEnabled` |

其余 16 处调用传的是 `StatusColor` (`color` / `*Color` 变量), 走另一个重载. **按本条原方案 "直接删除" 会让这 6 处编译失败**, 故整条撤回.

**仍然成立的判断**: 两个同名重载靠 `Boolean` / `StatusColor` 区分, 读调用点时看不出语义. 若要收敛, 正确顺序是先把上面 6 处改成显式 `StatusColor` (或改名 `toPassFailMyModel`), 再删重载 — 不是当死代码删.

---

<a id="F8"></a>

### F8 · `ExtendedColors.of()` 多标了 `@Composable` (P2, 改多)

> 发现: Hy4-preview (F8). 2026-09-25 复核: **仍开放**.

**现象**: 一个纯映射函数被声明成 `@Composable`.

**直接原因**: 大概率是 "它返回颜色, 在组合里用" 于是顺手加了注解.

**根本原因**: 对 `@Composable` 的语义理解有偏差 — 它表示 "这个函数会往组合里写东西". 后果: 只能在组合上下文调用 (想在 `@Composable` 之外的映射/预览数据构造里用就直接编译不过), 并且多引入一层重组作用域, 纯属自缚手脚.

**问题代码**:

```kotlin
// ExtendedColors.of()
@Composable
fun ExtendedColors.of(status: StatusColor): Color = when (status) { ... }
```

**修改方案**: 去掉 `@Composable` 即可 (`MyModelCard() 里的 LocalExtendedColors.current.of(model.color)` 调用处无需改动).

---

<a id="F9"></a>

### F9 · `MyModel.key` 用标题文本, 重复即崩溃 (P2, 隐患)

> 发现: Hy4-preview (F9). 2026-09-25 复核: **仍开放** (当前数据无碰撞的评估见第四节通过项 14).

**现象**: `LazyColumn` 的 `key` 取自 `MyModel.key`; `Raw` 标题的 key 就是标题文本本身. 一旦同页出现两个相同标题, `LazyColumn` 会直接抛 `IllegalArgumentException: Key was already used` — 而旧版 `DiffUtil.areItemsTheSame` 只会表现怪异, 不会崩.

**直接原因**: Compose 的 key 契约比 DiffUtil 严格 ([A·迁移期观察记录](../compose-migration-plan-cn/A-迁移期观察记录.md) 已记录了这点和 "当前三页无碰撞" 的结论).

**根本原因**: key 承担了两个职责 (列表项身份 + 动画/滚动状态锚点), 却复用了 "业务标题" 这个天然可能重复的值, 且代码里没有任何兜底.

**问题代码**:

```kotlin
// MyModel.key
val key: String
    get() = when (title) {
        is MyModelTitle.Res -> title.id.toString()
        is MyModelTitle.Raw -> title.text        // ← 可能重复
    }
```

```kotlin
// MyModelListScreen.kt → MyModelListContent() 的 items(key = { it.key })
key = { it.key },
```

**现状评估**: Prop 页三块数据源天然不冲突 (JVM 系统属性 / `Settings` 带类名前缀的键 / `getprop` 输出), Others, Home 用的是 `@StringRes`, 目前是安全的 — 所以定级 P2 而非 P0.

**修改方案** (任选):

1. 组合 key, 把 "类型" 纳入身份: `key = { "${it.type}:${it.key}" }` (最小改动, 能挡住跨类型碰撞);
2. 在数据源侧去重 (`distinctBy { it.key }`), 并在 `MyModelTitle.Raw` 的构造处加注释说明 "必须唯一";
3. 终极方案是给 `MyModel` 增加一个显式 `id` 字段, 让 key 不再依赖展示文本.

---

<a id="F10"></a>

### F10 · `@Stable` 标在了不需要的地方, 且它是一份长期承诺 (P3, 改多)

> 发现: Hy4-preview (F10). 2026-09-25 复核: **仍开放** (冗余结论在本轮得到再次确认: 组合里 `HomeViewModel` 的声明类型始终是基类).

**现象**: `BaseListViewModel` / `HomeViewModel` / `SettingsViewModel` 都标了 `@Stable`, `OthersViewModel` / `PropViewModel` 没标.

**直接原因**: 逐个提交做的 "稳定性优化" (提交 `494b8f28` / `05f7311d` / `42eb3e95`), 逐个类补注解.

**根本原因**: 没弄清稳定性由**声明类型**决定. 三个列表页调用的是 `MyModelListScreen(viewModel: BaseListViewModel)` — 参数声明类型就是基类, `@Stable` 标在基类上已经足够; `HomeViewModel` 上那份是**冗余的** (`SettingsViewModel` 那份有效, 因为 `SettingsScreen` 的参数声明类型就是它).

**风险提示**: `@Stable` 是 "所有公开属性永不静默变化" 的承诺. 目前各 VM 的可变字段都是 `private` (`loadJob`, `timesLeft`, `loadStartGeneration`), 承诺成立; 但一旦有人往 VM 上加一个 `var`, Compose 会因为 "类型稳定" 跳过重组, 产生极难排查的状态不同步.

**修改方案**: 保留 `BaseListViewModel` / `SettingsViewModel` 上的注解, 删掉 `HomeViewModel` 上冗余的那份; 把注释里的理由从 "实例不变" 改成 "公开属性都为 StateFlow 或 private var, 满足 @Stable 契约", 避免后人误读为 "给 ViewModel 加 @Stable 总是安全的".

---

<a id="F11"></a>

### F11 · 每个标签都注册了全部 4 个 entry;`currentTabIndex` 无越界保护 (P3, 隐患)

> 发现: Hy4-preview (F11). 2026-09-25 复核: **仍开放** (`decoratedEntries[currentTabIndex]` 仍无 `coerceIn`).

**现象**: `topLevelTabs.map { rememberDecoratedNavEntries(...) }` 里, 4 个标签各自的 `entryProvider` 都包含全部 4 个 entry (实际只会用到 1 个); `decoratedEntries[currentTabIndex]` 直接下标取值.

**直接原因**: 多返回栈配方 (nav3-recipes) 的模板写法, 为了保持 `remember` 调用顺序稳定而统一构造.

**根本原因**: 模板照搬后没有按本项目的实际形态收敛. 风险点: `currentTabIndex` 是 `rememberSaveable` 持久化的, 将来若减少标签数量, 老用户升级后读到的旧索引会越界 → `IndexOutOfBoundsException` 冷启动崩溃.

**修改方案**:

```kotlin
val index = currentTabIndex.coerceIn(0, topLevelTabs.lastIndex)
...
entries = decoratedEntries[index]
```

entryProvider 的冗余可以不改 (改动反而会破坏 `remember` 的稳定性), 但建议加一行注释说明 "四个 provider 内容相同是有意为之".

---

<a id="F12"></a>

### F12 · 图标 `fillColor` 硬编码黑色, 正确性依赖 `Icon` 的默认 tint (P3, 隐性依赖)

> 发现: Hy4-preview (F12). 2026-09-25 复核: **仍开放** (建议补的 tint 依赖注释尚未加).

**现象**: 4 个导航图标把 `?attr/colorOnSurface` 改成了 `#FF000000`.

```diff
-        android:fillColor="?attr/colorOnSurface"
+        android:fillColor="#FF000000"
```

**评估**: **方向是对的** — XML 主题现在只管窗口外壳, 不可能跟着 App 内主题 (例如 "系统浅色 + 应用强制深色") 走, 保留 `?attr` 反而会取到错误的颜色. 深色模式下不会变成 "黑底黑图标", 因为 `Icon(painterResource(...))` 默认 `tint = LocalContentColor.current`, 而 `NavigationBarItem` 会为图标槽位提供正确的 `LocalContentColor`, `ColorFilter.tint` 会整体替换 RGB.

**风险**: 正确性**完全依赖** "必须用 `Icon` 承载". 哪天把同一个 drawable 放到 `Image(painter = painterResource(...))` 或不带 `colorFilter` 的地方, 立刻变成纯黑且深浅色都不对.

**修改方案**: 保持现状, 在 `AppRoot.kt` 的 `Icon(...)` 处补一行注释: "图标资源为纯黑, 颜色由 Icon 的默认 tint (LocalContentColor) 决定, 勿改用无 tint 的载体".

---

<a id="F13"></a>

### F13 · 计划文档已落后于代码 (P3, 文档欠账) — **已修**

> 发现: Hy4-preview (F13). 状态: **已修复** — 迁移计划 README 的 "决策落地差异 (2026-09-22 汇总)" 块覆盖了下表全部偏差.

`AGENTS.md` 的规矩是 "文档与代码冲突时信代码并改文档". 以下三处当时已经对不上:

| 文档 | 当时现状 |
| --- | --- |
| README 决策 9: "MainActivity **暂留** AppCompatActivity" | 早已是 `ComponentActivity` (`733c6941`, `26b9094f`) |
| 9.2: "`appcompat` 与 `material` (MDC)**都暂留**" | 均已删除 (`f66562d4`), `AppCompatDelegate`/`DynamicColors` 已从代码里消失 |
| 9.4 遗留优化 2: "主题模式去 AppCompat 化" | 已完成, 应从 "遗留优化" 移到 "已完成" |

另外 README 决策 6 写的是 "实际: 砍掉设置项", 但设置项**仍在 UI 里** (只是失效) — 文档与代码各说各话, 正好是 F1 的成因, 已一并订正.

---

<a id="F14"></a>

### F14 · 滚动条数组长度不一致 (真实数据 Bug) (P1, 改错 / 数据不一致)

> 发现: Qwen3.8-Flash (#1, 独有条目, 并入时补编 F14). 2026-09-25 复核: **仍开放**.

**直接原因**: `scrollBarKeys` 只剩 2 项 (`fast` 被注释掉), 但并行的 `scrollBarValues` 仍有 3 项 (`fast` 未同步注释).

**根本原因**: 下线 "fast scrollbar" 选项时只改了 keys 一侧, 平行数组的两侧没有一起维护; 用两个平行数组 + `zip` 表达 (label, value) 对的建模方式本身就缺少长度一致性保障.

**问题代码**:

- `ui/settings/res/values/arrays.xml`: keys 2 项 / values 3 项.
- `SettingsScreen.kt` `SettingsChoiceDialog` 的 `labels.zip(values)`: `zip` 会截断到较短的一侧, 正好把第 3 个 value "藏" 了, 所以目前**没崩也没显示错**, 但这是个陷阱 — 一旦有人调换顺序, 或取消注释那行 key, 就会立刻错位. 而 theme 的数组是 3/3 对齐的, 两者风格不一致.

**修改方案**:

- 短期: 把 `interface_fast_scroll_bar_value` 那一行也注释/删除, 让 values 与 keys 一样是 2 项.
- 更好 (消灭整类问题): `SettingsChoiceDialog` 不再接收两个平行 `PersistentList<String>`, 改为接收一个 `PersistentList<Choice(label, value)>`, 单一数据源天然不可能长度错位, 也免去 `zip`.

---

<a id="F15"></a>

### F15 · `toPersistentList()` 每次重组都重新分配 (P2, 更好的改法)

> 发现: Qwen3.8-Flash (#4, 独有条目, 并入时补编 F15). 2026-09-25 复核: **仍开放**.

**直接原因**: `models` 来自 VM 的 `StateFlow<List<MyModel>?>`, 在 composable 体内每次重组都调用 `models?.toPersistentList()`. 即使 `models` 未变, 只是 `isLoading` 翻动 (下拉刷新起止各一次), 也会 `O(n)` 复制出一个**新的** `PersistentList` 实例传给 `MyModelListContent`.

**根本原因**: `MyModelListContent` 的参数声明为 `PersistentList` (为了 `@Stable` 可跳过), 但在边界处无条件转换反而破坏了这个跳过前提 — 每次都是新引用.

**问题代码**: `MyModelListScreen.kt` 的 `MyModelListScreen`: `models = models?.toPersistentList() ?: persistentListOf()`.

**修改方案** (二选一):

- 直接: `val modelsPersistent = remember(models) { models?.toPersistentList() ?: persistentListOf() }` — 只有 `models` 引用变化时才重建, `isLoading` 单独翻动时保持同一实例.
- 更干净: 让 `BaseListViewModel` 对外就暴露 `StateFlow<PersistentList<MyModel>?>` (在 `setModels` 处一次性 `toPersistentList()`), composable 侧不再转换. 这符合 "数据层产出不可变集合, UI 只消费" 的取向.

---

<a id="F16"></a>

### F16 · 状态圆点是纯颜色编码, 无障碍读不到任何状态 (P3, 漏改[继承] + 更好的改法)

> 发现: GLM-5.3-Flash (F16, 2026-09-25). 状态: **开放**.

**现象**: 每张卡片右上角的状态圆点 (正常=绿 / 警告=黄 / 异常=红) 是检测结果的唯一视觉信号, 但它只是一块带背景色的圆 — 没有 `contentDescription`, 没有 `stateDescription`. TalkBack 用户把焦点落到卡片上时只能听到标题与详情文案, **"这一项正常还是异常" 完全不可感知**.

**直接原因**: 圆点是纯绘制的 `Box` — `clip(CircleShape).background(...)`, 不产生任何语义节点.

**根本原因**: 从旧 `my_view_holder.xml` 的 `ShapeableImageView` 1:1 搬来 — 旧实现同样没有 `contentDescription`, View 时代 TalkBack 也会跳过它 (无描述的 ImageView 不获焦). 所以这是**继承至今的缺口, 不是迁移改出来的**; 但它与 [C6](01-架构.md#C6) / NFR-7 是同一个主题 — "状态不能只靠颜色表达": C6 管的是普通用户把 Unknown 误读成 Critical, 这条管的是盲用户干脆什么都读不到. 迁移把行模型收成了 `StatusColor` 枚举 (`MyModel.color`), 恰好让补语义变成一件零歧义的小事.

**问题代码**:

```kotlin
// MyModelCard.kt → MyModelCard() 的状态圆点
if (model.color != StatusColor.NONE) {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .size(with(LocalDensity.current) { MyModelColorDotSize.toDp() })
            .clip(CircleShape)
            .background(LocalExtendedColors.current.of(model.color)),   // ← 纯颜色, 零语义
    )
}
```

**修改方案**: 颜色不动, 给圆点补一条语义描述:

```kotlin
.semantics {
    contentDescription = when (model.color) {
        StatusColor.NO_PROBLEM -> dotNoProblem    // 新增状态词资源 (四语): "正常"
        StatusColor.WARNING -> dotWarning
        StatusColor.CRITICAL -> dotCritical
        StatusColor.NONE -> ""
    }
}
```

成本: 新增 3 条 string 资源 (默认英文 + 三份翻译, 见[约定文档](../conventions/README-cn.md)的本地化一节). 若与 C6 的三态 Unknown 映射 (Q9) 同批落地, "Unknown" 一词顺带就有了, 资源只加一次. 顺带注: 设置页的 `SettingsCategoryHeader` 可以加 `semantics { heading() }`, 让 TalkBack 支持按标题跳转 — 同类的零成本语义补齐.

---

## 三, 2026-09-25 复核: 开放发现的当前代码证据

GLM-5.3-Flash 轮对全部开放发现逐条重新取证, 结论: **证据全部仍在, 无一条被顺手修掉**. 每条给出本轮可 grep 的复核点:

| # | 当前证据 (2026-09-25, `422b500e`) |
|---|---|
| F2 | `BaseListViewModel.startLoad()` 内 `try` / `catch` / `finally` 命中数为 0 — 异常路径仍绕过 `setLoading(false)` |
| F3 | `SettingsScreen()` 仍是四组 `remember { ... MyApplication.sharedPreferences... }` 初读 + 回调里直写 SP 并直调 `setMyTheme` |
| F4 | `Theme.kt` 的 4 套 contrast 方案 + `ColorFamily` / `unspecified_scheme` 仍零引用; `Color.kt` 的 MediumContrast / HighContrast 常量仍有约 140 行 |
| F6 | `AppRoot()` 的 `onBack` 仍是 `activity?.finish()` |
| F8 | `ExtendedColors.of()` 仍标着 `@Composable` |
| F9 | `MyModelListContent` 的 items 仍是 `key = { it.key }`; `MyModel.key` 的 `Raw` 分支仍返回标题文本 |
| F10 | `HomeViewModel` 仍标 `@Stable` — 组合里它的声明类型是 `BaseListViewModel` (`AppRoot` 的两条 entry 都把实例传给 `viewModel: BaseListViewModel` 参数), 注解冗余如故; `SettingsViewModel` 那份仍有效 (`SettingsScreen` 的参数声明类型就是它) |
| F11 | `decoratedEntries[currentTabIndex]` 仍无 `coerceIn` |
| F12 | 4 个 `ic_*_24dp.xml` 的 `fillColor` 仍是 `#FF000000`, 且 `AppRoot.kt` 的 `Icon(...)` 处仍无 tint 依赖说明注释 |
| F14 | `scrollBarKeys` 2 项 / `scrollBarValues` 3 项 — `interface_fast_scroll_bar_value` 仍未注释 |
| F15 | `models?.toPersistentList() ?: persistentListOf()` 仍无 `remember(models)` 包裹 |

已裁定与已了结的 4 条: **F1 / F5** 已裁定保持现状 (处置块见各自小节, F1 与 [R10](05-已裁定事项.md#R10) 同源); **F7** 已撤回 (Boolean 重载有 6 处调用); **F13** 已修 ("决策落地差异" 汇总块).

---

## 四, 已核对通过的项 (三次复查合并, 去重)

复查不是只找茬. 下面这些**特意验证过, 结论是 "没改错"**, 供放心的同时也作为以后回归的基准. 括号内为核对轮次 (Hy4 = 轮次 1, Qwen = 轮次 2, GLM = 轮次 3):

1. **状态色映射 1:1 无错位**: `HomeRepository.kt` 旧 `R.attr.colorNoProblem/Warning/Critical` → 新 `StatusColor.NO_PROBLEM/WARNING/CRITICAL`, 逐项计数 (19 / 18 / 31) 与**出现顺序完全一致**, 不存在把 WARNING 换成 CRITICAL 之类的手滑. (Hy4)
2. **扩展色数值与旧资源逐字节一致**: `Color(0xD0_ACDDB7 / 0xD0_FDD18F / 0xD0_FFB1AC)`, 夜间 `0xD0_2B5128 / 0xD0_7E581F / 0xD0_812F2F` 与已删除的 `colors.xml` (含 `values-night`) 完全相同. (Hy4)
3. **排版对齐**: `Type.kt` 用 20sp Bold / 16sp + `includeFontPadding = true`, 与旧 `my_view_holder.xml` 两个 TextView 一致 (含 "Compose 默认已改为 false" 这个坑). (Hy4)
4. **卡片视觉对齐**: `surfaceBright` 容器色, `elevation 0`, M3 默认 medium 圆角, `item_card_padding = 10dp`, 间距 `12dp` 均与旧 XML 一致. (Hy4)
5. **偏好默认值一致**: 主题默认 `-1` (跟随系统), 滚动条默认 `0` (无), 与旧 `preferences.xml` 的 `app:defaultValue` 相同; 存储文件名仍是 `${packageName}_preferences`, 老用户设置继承无虞. (Hy4)
6. **清理清单已落地**: Fragment 壳 4 个, `main_activity.xml`, `bottom_nav_menu.xml`, `MainViewModel`, `drop_scale.xml`, `MyAdapter/MyViewHolder/MyItemDecoration`, `ViewExt.kt`, `StateExt.kt` (`State` 密封类), `preferences.xml` 等均已删除, 无 "删一半" 的残留引用. (Hy4)
7. **主题链路自洽 + 系统栏双写**: `AppThemeMode.isDark()` 是唯一的三态→明暗映射, `MainActivity` (窗口外壳 + edge-to-edge) 与 `AppTheme` (配色 + 系统栏 `SideEffect`) 共用, 不存在两处判断漂移; `enableEdgeToEdge` 传 `::isAppDark` 也是对的 (默认 `detectDarkMode` 只看系统 uiMode, 与 "应用强制深色" 不一致), 两处写入是有意的 belt-and-suspenders; 预览无 Activity 时 `SideEffect` 安全跳过. (Hy4 #7; Qwen 复核 3; GLM 复验)
8. **`MyApplication.initTheme` 的省电模式迁移**: 把遗留值 `1` 归一化回 "跟随系统" **并写回 SP**, Settings 页后续读到的就是干净值, 不靠每次启动兜底. (Hy4 #8; GLM 复验)
9. **Home 过期排序开关的代次护栏**: `OnSharedPreferenceChangeListener` + "加载开始/落地" 两代次比对 (`loadStartGeneration`), 覆盖 "刷新期间被切换" 的竞态, 比旧的 `SharedFlow` 广播可靠; Rule 1 的即时补丁在列表未落地前自动跳过. (Hy4 #9; GLM 复验)
10. **偏好存储键 / 值全部 locale 无关**: `interface_themes_key` / `function_allow_network_data_key` / 各 `*_value` 等真正的存储键值资源全部标了 `translatable="false"`, 三份翻译文件里也没有混入存储键 — 切语言不会漂移偏好键名, 也不会让 `setMyTheme` 的值比较失配. (易漏点: 资源名带 `_key` 的不都是存储键 — `interface_no_scroll_bar_key`("None") 其实是对话框标签, 可翻译; 两类同名后缀并存是个阅读陷阱, 也是 F14 数组错位的土壤.) (GLM)
11. **下拉刷新状态同源**: `rememberPullToRefreshState()` 一个实例同时喂给 `PullToRefreshBox` 与自定义 `Indicator`; `isRefreshing` 由参数单向驱动, 组件不自有状态 — 手势与指示器不会各说各话. (GLM)
12. **对话框资源读取已下沉**: 主题 / 滚动条的 4 个标签值数组都在 `if (showXxxDialog)` 块内声明, 对话框关闭时连资源读取都不发生. (GLM)
13. **圆点尺寸随字号缩放**: `16.sp` 经 `LocalDensity` 转 dp, 与旧 XML 的 `16sp` 行为一致; `StatusColor.NONE` 时圆点整块不组合 (条件组合, 不是组合后隐藏). (GLM)
14. **列表 key / contentType 与当前数据无碰撞**: items 同时给了 `key`(身份) 与 `contentType = { it.type }`(复用池), 与旧 RecyclerView viewType 对应; `MyModel.key` 对 Prop/Home 当前数据无碰撞, 且 `updateModelDetail` 只改 `detail` 不改 `key`, 动画与滚动状态得以保留. (Qwen 复核 1; GLM 复验)
15. **`LaunchedEffect(viewModel){ init() }` 触发加载**: 属观察记录 "遗留优化" 里明确记录, 当前幂等 + loadJob 去重可接受的取舍, 不算错. (Qwen 复核 2)
16. **Navigation3 多返回栈**: 对 "4 个平铺 tab, 无二级导航" 的 App 偏重, 但换来每 tab 独立状态与 VM 存活 (对应旧 Fragment show/hide), 是迁移计划第 6 步的有意决定; 恢复链闭环 — 4 个 `NavKey` 都是 `@Serializable data object`, `rememberNavBackStack` 可序列化恢复, `currentTabIndex` 走 `rememberSaveable`. (Qwen 复核 4; GLM 补验)
17. **`@Immutable MyModel`**: 其可达状态 (`MyModelTitle.Res(Int)` / `Raw(String)`, `detail: String`, 枚举) 均深度不可变, 注解成立, 安全. (Qwen 复核 5)
18. **预览配套**: `previewModels` / `@Preview` 明暗双预览, `debugImplementation ui-tooling` (在 `build-logic` `Compose.kt`): 预览可正常渲染, 无遗漏. (Qwen 复核 6)
19. **编译状态**: 三轮复查 `compileFossDebugKotlin` 均 BUILD SUCCESSFUL. (Hy4; GLM)

---

## 五, 建议处理顺序

开放条目共 12 条 (F2 / F3 / F4 / F6 / F8 / F9 / F10 / F11 / F12 / F14 / F15 / F16), 建议批次 (取代 Hy4 版处理顺序 — 该节对 F5 的建议已被 2026-09-22 裁定推翻, 其余判断与本轮一致):

1. **F2** — 6 行 try/finally, 直接消除 "卡死转圈", 仍是最高优先级 (与 Hy4 第四节的判断一致).
2. **F14 + F15** — 两处十行内小改: 数组对齐 (或换单一 Choice 列表) + `remember(models)`.
3. **F3** — Settings 状态收进 ViewModel, 量最大, 独立一批; 它就是 [AR-02](02-SSOT-唯一数据来源.md#AR-02) 修复方案在 UI 侧的投影, **与 AR-02 的 `SettingsStore` 合流实施最省** — 做完之后 F1 的死总线自然失去存在意义.
4. **F4** — 纯删除约 370 行死主题样板 (`Theme.kt` 4 套 contrast + `ColorFamily` / `unspecified_scheme` + `Color.kt` 对应常量).
5. **F16** — 圆点语义; 与 C6 的三态 Unknown 映射同批最划算, 状态词资源一次加齐.
6. **F6 / F8 / F9 / F10 / F11 / F12** — 各十行以内, 可攒一个收尾提交.

每个修复照旧跑 `./gradlew assembleFossDebug` + 过一遍迁移计划 [09 章](../compose-migration-plan-cn/09-第7步-清理收尾.md) 的回归清单 9.3.
