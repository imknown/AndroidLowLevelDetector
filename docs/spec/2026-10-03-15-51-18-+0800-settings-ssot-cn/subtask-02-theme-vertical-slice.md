# ST-02 — theme-vertical-slice 修改计划

> 状态: living (动工时如有修订在此显式记录). 计划依据: 本目录 plan.md + issues-cn #08 / #10. 风险: 高 (启动时序 + Activity 重建 — 负责人预注册风险的主战场).

## 目标句 (占位, 动工对照时由负责人确认或改写)

主题设置项整链搬进 `SettingsStore`: `AppTheme` / `MainActivity` / 设置页主题行全部改观察或写 store, `MyApplication` 伴生对象上的 `themeMode` / `setMyTheme` / `initTheme` 删除, 组合前现值时序逐点保持.

## 变更清单

1. `base/MyApplication.kt`:
   - 删 companion `themeMode` / `setMyTheme` (AppThemeMode 顶层枚举原地保留, 去留 ST-05 终裁).
   - 删 `initTheme()` (power-saver 归一已随 ST-01 迁入 store 播种段); `onCreate` 里 `initTheme()` 的位置改为解析一次 store (`appGraph.settingsStore`) — 这同时完成滚动条流的播种 (store 一次构造播两条), `initScrollBar` 留到 ST-03 删, 两者并存期 companion 流与 store 流互不干扰.
   - `initScrollBar` 头注释 "(same as initTheme)" (`MyApplication.kt:160`) 本子任务顺带改写 (函数本体 ST-03 才删), 使本子任务的零残留 grep 可过.
   - 相邻注释归真 (companion 段引用 themeMode 的注释随成员删除一并处理).
2. `ui/theme/Theme.kt`: `MyApplication.themeMode.collectAsStateWithLifecycle()` (`:279`) 换源. 接入方式对比后**钉死 (a) 按流拆 CompositionLocal**:
   - (a) **已选**: `setContent` 根部与 `LocalMetroViewModelFactory` 并排 provide `LocalThemeMode` — 类型 `CompositionLocal<StateFlow<AppThemeMode>>`, 携带 `settingsStore.themeMode` 这一条流, **不是整个 store**; `AppTheme` 读它. 选它而非整 store local 的理由: preview 默认值 = `MutableStateFlow(AppThemeMode.FollowSystem)` 常量, 天然可构造; 整 store 形态 (`CompositionLocal<SettingsStore>`) 的默认值必须是 store 实例, 构造要 SP, preview 拿不到, 还得为 preview 引接口 + fake, 不值. Local 定义位置 (`ui/theme` 与 AppTheme 同文件, 或 `ui/common`) 报告落笔时定一处.
   - (b) 参数下传: `AppTheme(themeMode: StateFlow<AppThemeMode>)`, `MainActivity` 取 store 后传入. 弃用理由: preview 调用点 (SettingsScreen / MyModelListScreen / AppRoot 三处 AppTheme) 要逐个补参, 穿透整条 preview 链.
   - preview 默认的正确性: preview 靠 `uiMode` + `isSystemInDarkTheme()` 出深色, FollowSystem 默认即正确.
3. `ui/MainActivity.kt`: `isAppDark()` (`:77`) 改读 `settingsStore.themeMode.value`; store 经 `(application as MyApplication).appGraph.settingsStore` 取, `onCreate` 里缓存为字段; 相邻注释 (`:75-76`) 归真.
4. `ui/settings/SettingsViewModel.kt`: 注入 store; 新增主题原始存值状态 (首帧即存值形态, 见 plan 约定 #1 — 同步初值或 `stateIn(Eagerly)`) + `setTheme(value: String)` 委托 store.
5. `ui/settings/SettingsScreen.kt`: 主题行读写换源 — `themeValue` 的 `remember { ... getString(...) }` (`:81-86`) 改收集 ViewModel 状态; `onThemeSelect` 回调 (`:124-128`) 改 `viewModel.setTheme(value)`; 删 `themeKey` / `themeDefaultValue` 的 `stringResource` 直读; 相邻注释 (`:79-80` initTheme 迁移注) 归真.
6. preview (`SettingsContentPreview`): `themeValue` 照旧传常量, 不受影响.

## 有意不做 (documented non-goal)

- 对话框枚举化 (`SettingsChoiceDialog` 泛型化收枚举, enum 带 `labelRes` 与可选子集, store 的原始存值流随之退役) 是合理的后续演进, 但属设置 UI 的类型安全改造, 不混入本次存储层 SSOT 迁移: 文案搬家 ×4 locale, 且退役编码要重表达 (滚动条 Draggable 档今日以 "注释掉数组项" 的方式退役). 主线走完后再择机立项; 本任务两个对话框维持数组元数据形态 — 本条裁定与 ST-03 的滚动条对话框共用.

## 时序论证 (报告必须写清的三段)

1. `MyApplication.onCreate`: `instance` 赋值 → store 首解析 (SP 此时可用, 播种 + power-saver 归一) — 与今天 `initTheme()` 完全同位.
2. `MainActivity.onCreate` 前半 (`setTheme` / `enableEdgeToEdge`): 读 `store.themeMode.value` — 已播种, 值与今天伴生流一致.
3. 组合首帧: `AppTheme` collect — 初值即播种值, 无默认闪烁.

## 行为等价性 / 已知差值

- power-saver 归一: 从 `initTheme` 移到 store 首解析, 触发位置相同 (`onCreate` 同一点), 时序等价.
- 未识别存值回落 follow_system: 解析 when 逐字保持.
- 写路径: 今天 "Screen 写 SP + `setMyTheme` 刷流" 两步 → "Screen → ViewModel → store 写 SP 一次, 流随一致机制更新" 一步; 界面生效时机: ST-01 已钉 listener 回灌 — 写 SP → 同步回灌 → 同帧生效.
- Activity 重建: store 是 app 作用域单例, `StateFlow` 重放当前值 — 与今天伴生流语义一致 (负责人风险点, 真机验证单覆盖).

## 验证

- `./gradlew assembleFossDebug` 绿.
- 真机: 存值 = always_dark 时冷启首帧即深色不闪浅色; 设置内切换即时生效; 旋转 recreate 保持; follow_system 下系统深色切换即时; `adb shell am kill` 重启存值恢复; 进入设置页首帧主题行选中项即为存值.
- 零残留 grep: `MyApplication.themeMode` / `setMyTheme` / `initTheme` 全仓零现在时引用.
