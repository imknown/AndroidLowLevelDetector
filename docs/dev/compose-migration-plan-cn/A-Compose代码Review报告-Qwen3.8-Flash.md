# Compose 代码 Review 调查报告

> 所属迁移计划：[README](README.md) · 与 [A·迁移期观察记录](A-迁移期观察记录.md) 并列的一份 Review 附录
>
> 调研日期 2026-09-21 · 范围：`app/src/main/java/net/imknown/android/forefrontinfo/ui/` 下全部 Composable、主题、导航，及配套资源与构建配置
>
> 本报告只调查、不改代码。按 `改错 / 改多 / 漏改 / 不合理 / 更好的改法` 归类，每条给出 **直接原因 / 根本原因 / 问题代码 / 修改方案**。

## 总体结论

整体质量很高：分层（Screen 持有 VM / Content 纯数据可预览）、状态提升、`collectAsStateWithLifecycle`、`@Stable`/`@Immutable`、导航装饰器、窗口主题壳都做得规范，迁移期偏差大多已在 [A·迁移期观察记录](A-迁移期观察记录.md) 里记录并"有意接受"。下面都是可落地的收敛点，不是架构性错误。

| # | 问题 | 类别 | 严重度 | 位置 |
| --- | --- | --- | --- | --- |
| 1 | 滚动条选项数组长度不一致（keys=2 / values=3） | 改错 / 数据不一致 | **高** | `ui/settings/res/values/arrays.xml` |
| 2 | 滚动条事件总线是死代码，注释与现实相反 | 漏改 / 残留 | **高** | `SettingsViewModel.kt`、`SettingsScreen.kt` |
| 3 | 主题文件保留了大量从未使用的对比度配色样板 | 改多 / 未清理 | 中 | `ui/theme/Theme.kt`、`ui/theme/Color.kt` |
| 4 | `toPersistentList()` 在每次重组都重新分配 | 更好的改法 | 中 | `MyModelListScreen.kt` |
| 5 | `Card(onClick = {})` 为"仅有水波纹"牺牲了语义 | 不合理 / 权衡 | 低 | `MyModelCard.kt` |
| 6 | 两套并存的偏好传播机制（SP 监听 vs 静态 SharedFlow） | 一致性 | 低 | `HomeViewModel` vs `SettingsViewModel` |

---

## 1. 滚动条数组长度不一致（真实数据 Bug）

**直接原因**：`scrollBarKeys` 只剩 2 项（`fast` 被注释掉），但并行的 `scrollBarValues` 仍有 3 项（`fast` 未同步注释）。

**根本原因**：下线 "fast scrollbar" 选项时只改了 keys 一侧，平行数组的两侧没有一起维护；用两个平行数组 + `zip` 表达 (label,value) 对的建模方式本身就缺少长度一致性保障。

**问题代码**：

- `ui/settings/res/values/arrays.xml`：keys 2 项 / values 3 项。
- `SettingsScreen.kt` `SettingsChoiceDialog` 的 `labels.zip(values)`：`zip` 会截断到较短的一侧，正好把第 3 个 value "藏"了，所以目前**没崩也没显示错**，但这是个陷阱——一旦有人调换顺序、或取消注释那行 key，就会立刻错位。而 theme 的数组是 3/3 对齐的，两者风格不一致。

**修改方案**：

- 短期：把 `interface_fast_scroll_bar_value` 那一行也注释/删除，让 values 与 keys 一样是 2 项。
- 更好（消灭整类问题）：`SettingsChoiceDialog` 不再接收两个平行 `PersistentList<String>`，改为接收一个 `PersistentList<Choice(label, value)>`，单一数据源天然不可能长度错位，也免去 `zip`。

---

## 2. 滚动条事件总线是死代码，注释与现实相反

**直接原因**：`SettingsViewModel.scrollBarModeChangedSharedFlow` 只有生产者（`emitScrollBarModeChangedSharedFlow`，被 `onScrollBarSelect` 调用），全仓**没有任何 `.collect`**。切换"滚动条"开关实际只写入 SharedPreferences，UI 无任何响应。

**根本原因**：迁移前由 View 版列表（RecyclerView / `BaseListFragment`）作为消费者观察这个静态 SharedFlow；Compose 迁移时消费端被删除，但生产端管道保留了下来；且滚动条渲染被有意"推迟到 material3 1.5.0 官方组件"（见 README 关键决策 #6，commit 85da9fe1），于是这条链路悬空。

**问题代码**：

- `SettingsViewModel.kt`：定义 `scrollBarModeChangedSharedFlow` + `emitScrollBarModeChangedSharedFlow`。
- `SettingsScreen.kt` `onScrollBarSelect`：`viewModel.emitScrollBarModeChangedSharedFlow(value) // list pages pick it up immediately` —— 这句注释现在是**假的**，会误导后续维护者。

**修改方案**：

- 删除 `scrollBarModeChangedSharedFlow`、`emitScrollBarModeChangedSharedFlow` 及 `onScrollBarSelect` 里的调用，仅保留写 SP；把注释改为"滚动条渲染待 material3 官方组件，当前仅持久化偏好"，与 [A·迁移期观察记录](A-迁移期观察记录.md) 的"推迟"决定对齐。
- 若确实要立刻生效，则补上消费端——但那要等官方 scrollbar 组件，属功能开发而非本次 Review 范畴。
- 这条与第 6 条相关：修完后，滚动条与 "outdated-order" 就统一到"SP 为唯一事实源、由 Home 直接观察"的模式。

---

## 3. 主题文件保留了从未使用的对比度配色样板

**直接原因**：`AppTheme` 只在 `lightScheme` / `darkScheme` / 动态色之间选择，但文件里还留着 4 套 medium/high contrast 方案与 `ColorFamily`/`unspecified_scheme`，全部无引用。

**根本原因**：直接粘贴了 Material Theme Builder 的导出，未做裁剪；项目并未实现 WCAG 对比度切换，那 4 套方案永远进不了 `when` 分支。

**问题代码**：

- `ui/theme/Theme.kt`：`mediumContrastLightColorScheme`、`highContrastLightColorScheme`、`mediumContrastDarkColorScheme`、`highContrastDarkColorScheme`（4 个 `private val`，Kotlin/IDE 会报"never used"），以及 `ColorFamily` 数据类与 `unspecified_scheme`（全仓无引用）。
- `ui/theme/Color.kt`：`...LightMediumContrast` / `...HighContrast` / `...DarkMediumContrast` / `...DarkHighContrast` 约 130+ 个颜色常量，仅被上述死方案引用——这是 219 行文件里的绝大部分。

**修改方案**：删除 `Theme.kt` 中 4 套 contrast 方案、`ColorFamily`、`unspecified_scheme`，并删除 `Color.kt` 里对应的 contrast 颜色常量，只保留 `lightScheme`/`darkScheme` 实际用到的两套 + `ExtendedColors`。收益：文件从 ~530 行降到 ~130 行，消除 IDE 未用告警。（若担心未来要上对比度功能，可在 JOTTINGS 记一句"已裁剪，需要时重新生成"，但按 AGENTS.md "不猜测/不留悬空" 的取向，删更合适。）

---

## 4. `toPersistentList()` 每次重组都重新分配

**直接原因**：`models` 来自 VM 的 `StateFlow<List<MyModel>?>`，在 composable 体内每次重组都调用 `models?.toPersistentList()`。即使 `models` 未变、只是 `isLoading` 翻动（下拉刷新起止各一次），也会 `O(n)` 复制出一个**新的** `PersistentList` 实例传给 `MyModelListContent`。

**根本原因**：`MyModelListContent` 的参数声明为 `PersistentList`（为了 `@Stable` 可跳过），但在边界处无条件转换反而破坏了这个跳过前提——每次都是新引用。

**问题代码**：`MyModelListScreen.kt` 的 `MyModelListScreen`：`models = models?.toPersistentList() ?: persistentListOf()`。

**修改方案**（二选一）：

- 直接：`val modelsPersistent = remember(models) { models?.toPersistentList() ?: persistentListOf() }` —— 只有 `models` 引用变化时才重建，`isLoading` 单独翻动时保持同一实例。
- 更干净：让 `BaseListViewModel` 对外就暴露 `StateFlow<PersistentList<MyModel>?>`（在 `setModels` 处一次性 `toPersistentList()`），composable 侧不再转换。这符合"数据层产出不可变集合、UI 只消费"的取向。

---

## 5. `Card(onClick = {})`：为"仅有水波纹"付出了语义代价

**直接原因**：最近一次 commit（cb4104aa）为了让卡片恢复旧 `MaterialCardView` 的可点击水波纹，把 `Card` 换成 `onClick` 重载并传空 lambda。

**根本原因 / 权衡**：`Card(onClick=...)` 会把每张卡片标记成**可点击、可获焦的按钮语义**。本项目卡片其实不可点，于是 TalkBack 会把每张卡片播报成"按钮"却无任何动作，键盘/D-pad 也会在每张卡片上停留。这是"为还原 View 时代视觉反馈，引入了错误的无障碍语义"。

**问题代码**：`MyModelCard.kt` 的 `Card(onClick = {}, ...)`。

**修改方案**：如果只是想"按压有水波纹"，用不带按钮语义的方式：`Modifier.combinedClickable(interactionSource = remember { MutableInteractionSource() }, indication = LocalIndication.current, onClick = {})`（或 `Modifier.indication(...)` + 只读 `interactionSource`），既能出波纹又不会被识别为 button；若无障碍播报正确性优先，则直接去掉点击水波纹回到无点击 `Card`。建议按无障碍优先处理，并在 [A·迁移期观察记录](A-迁移期观察记录.md) 记一句决定。`animateContentSize()` + `animateItem()` 的分层动画本身是合理的，注释也准确，保留。

---

## 6. 两套并存的偏好传播机制（一致性建议）

**现象**：`outdated-order` 走的是新范式——`HomeViewModel` 直接 `registerOnSharedPreferenceChangeListener` 观察 SP key（SP 为唯一事实源，commit 38492b82/64a241cd）；而 `scroll-bar` 走的是旧范式——`SettingsViewModel` 里的静态 `MutableSharedFlow` 事件总线（且已悬空，见第 2 条）。

**根本原因**：迁移过程中把 `outdated-order` 从"事件总线"重构成"直接观察 SP"，但 `scroll-bar` 因功能推迟没被一并重构，留下了旧管道。

**修改方案**：随第 2 条一并处理——删掉悬空的 SharedFlow 事件总线。将来实现滚动条渲染时，采用与 `outdated-order` 相同的"观察 SP key"模式，全项目只保留一种偏好传播范式，避免"写方忘记发事件"这类坑。

---

## 复核确认"无需改动"的点（供放心）

- **LazyColumn 重复 key 抛异常**：`MyModel.key` 对 Prop/Home 当前数据无碰撞，且 `updateModelDetail` 只改 `detail` 不改 `key`，动画/滚动状态得以保留；已在 [A·迁移期观察记录](A-迁移期观察记录.md) 第 3 条"有意接受"。
- **`LaunchedEffect(viewModel){ init() }` 触发加载**：属观察记录"遗留优化"里明确记录、当前幂等 + loadJob 去重可接受的取舍，不算错。
- **主题双重设置系统栏**（`MainActivity` edge-to-edge 的 `::isAppDark` + `AppTheme` 的 `SideEffect`）：两处都以 `AppThemeMode.isDark` 为唯一映射源，注释已说明是"belt-and-suspenders"，设计正确。
- **Navigation3 多返回栈**：对"4 个平铺 tab、无二级导航"的 App 偏重，但换来每 tab 独立状态与 VM 存活（对应旧 Fragment show/hide），是迁移计划第 6 步的有意决定，合理。
- **`@Immutable MyModel`**：其可达状态（`MyModelTitle.Res(Int)` / `Raw(String)`、`detail:String`、枚举）均深度不可变，注解成立，安全。
- **`previewModels` / `@Preview` 明暗双预览、`debugImplementation ui-tooling`**（在 `build-logic` `Compose.kt`）：预览可正常渲染，无遗漏。

---

## 建议的落地顺序

1. 先修 **#1（数组错位）** 与 **#2（死事件总线 + 假注释）**——改动小、且 #2 直接消除误导。
2. 再做 **#3（裁剪主题样板）**——纯删除，独立提交。
3. 然后 **#4（`remember` 或 VM 暴露 PersistentList）**——性能与惯例收敛。
4. **#5 / #6** 属权衡/一致性，建议先确认取向（无障碍优先 vs 视觉还原优先；是否现在就统一偏好范式）再动。
