# ST-01 — settings-store-skeleton 修改计划

> 状态: living (动工时如有修订在此显式记录). 计划依据: 本目录 plan.md + issues-cn #08 / #10. 风险: 高 (生疏领域: Flow 桥; 虽纯新增零行为变化可单独 revert, 但桥的语义是全部后续子任务的地基).

## 目标句 (占位, 动工对照时由负责人确认或改写)

新增可注入的设置唯一归属 `SettingsStore` 与 SP→Flow 桥, 挂上图 accessor; 不切任何消费方, 应用行为零变化.

## 变更清单 (纯新增 + 图一行)

1. 新 `ui/settings/repository/SettingsStore.kt`:
   - `@Inject` 构造 + `@SingleIn(AppScope::class)`, 构造参数 `SharedPreferences` (图内现成绑定, `AppGraph.kt:177-178`).
   - 键集中定义 (构造时解析一次, 取代各读取点就地 `getMyString`): `interface_themes_key`, `interface_themes_follow_system_value`, `interface_themes_always_light_value`, `interface_themes_always_dark_value`, `interface_themes_power_saver_value`, `interface_scroll_bar_key`, `interface_no_scroll_bar_value`, `interface_normal_scroll_bar_value`, `interface_fast_scroll_bar_value`, `function_allow_network_data_key`, `function_outdated_target_order_by_package_name_first_key`. 全部键资源 `translatable="false"`, 存值稳定.
   - 播种段 (构造时同步执行): 读 SP 播种两条非空枚举流; power-saver tombstone 归一 (`MyApplication.initTheme:148-154` 的逻辑原样迁入 — 存值 = power_saver_value 则写回 follow_system).
   - 暴露面: `themeMode: StateFlow<AppThemeMode>` / `scrollBarMode: StateFlow<ScrollBarMode>` (非空, 解析语义与 `setMyTheme` / `setMyScrollBar` 的 when 逐字一致, 未识别值回落 follow_system / none, Draggable 存值解析为自身); `themeValue` / `scrollBarValue` 原始存值流 (设置页对话框比较用); `allowNetworkData` / `outdatedOrderFirst` 布尔非空流 (播种默认 false, 与今天各读取点默认一致).
   - 写入口: `setTheme(value: String)` / `setScrollBarMode(value: String)` / `setAllowNetworkData(value: Boolean)` / `setOutdatedOrderFirst(value: Boolean)` — 只写 SP 一次.
2. SP→Flow 一致机制 (plan 约定 #1 授权本报告定稿): **工作默认 = (a) listener 回灌** — 构造时注册 `OnSharedPreferenceChangeListener` (store 字段持有, 强引用), 流只由 listener 更新; 写 = 写 SP → listener 同步回灌. 单一更新路径, 假想的外部直写也覆盖; 与 "SP 是持久真相" 的条目立场同构, 且 `registerOnSharedPreferenceChangeListener` 对同进程写是同步回调, 无可见延迟. 翻案条件一并落字: 若实施时证实回调时序或 listener 引用语义与预期不符, 停手报负责人, 备选 (b) 写穿透 (写方法内先更新流再写 SP, 不注册 listener; 机制更少, 但 store 之外出现写方时流会静默漂移). 两形态下 `PrefsFlow.kt` 独立文件都不需要 — 桥逻辑内聚在 store. 本钉死使 ST-02 的 "同帧生效" 与 ST-04 的差值 A / B 论证在本批次即可闭环.
3. `di/AppGraph.kt`: interface 加 `val settingsStore: SettingsStore` accessor (Metro 对 `@Inject` 类自动绑定具体类型; `@SingleIn(AppScope)` 保证 accessor 解析与后续 ViewModel 注入是同一实例 — 构造一次, 播种一次).
4. 不动: `MyApplication` / `SettingsScreen` / `SettingsViewModel` / `HomeViewModel` / `HomeRepository` / `Theme.kt` / `MainActivity` — 全部留到 ST-02..04.

## 行为等价性

零消费方切换: 现有读取/写入路径全部原样, store 无业务调用方. 编译绿 + 一次冒烟即可.

## 验证

- `./gradlew assembleFossDebug` 绿.
- 冒烟: 应用启动 / 四页渲染 / 设置切换 / 主题滚动条生效 — 与基线无差 (全部走旧路径).
- 零残留: 不适用 (纯新增).

## 待学清单落点

SP listener 注册表的引用语义与取消时序 → 本报告定稿段写清 (store 是 app 作用域单例, listener 生命周期 = 进程, 无需注销的论证也要落字); `stateIn` 语义 → ST-02 报告.
