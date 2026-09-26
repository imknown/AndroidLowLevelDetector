<a id="AR-02"></a>

# AR-02 设置项绕开唯一数据源, 靠静态事件总线广播变化

> 返回 [README 索引](../README.md) · [组2 · 主线重构 — 架构优先](../README.md#组2--主线重构--架构优先).


**严重程度: P0 | 修复难度: 中**
**影响文件: `SettingsScreen.kt`, `SettingsViewModel.kt`, `HomeViewModel.kt`, `HomeRepository.kt`, `MyApplication.kt`**

## 问题核心代码

"设置" 这份本该只有一处权威的数据, 现状是**写一处, 抄四路, 广播一条 (而且没人收)**:

写入只有一处 (`SettingsScreen() 的各 onXxxSelect 回调`, 每个选项的回调里直接 `edit { put... }`); 然后四处各自偷读:

```kotlin
// ① 启动时读主题 — MyApplication.initTheme()
val themesValue = sharedPreferences.getString(themeKey, defaultTheme)

// ② 设置页读自己那四档 — SettingsScreen() 里四组 remember 直读 prefs
var allowNetwork by remember {
    mutableStateOf(MyApplication.sharedPreferences.getBoolean(allowNetworkKey, false))
}

// ③ ViewModel 读联网开关 — HomeViewModel.collectModels()
val allowNetwork = MyApplication.sharedPreferences.getBoolean(
    MyApplication.getMyString(R.string.function_allow_network_data_key), false)

// ④ 仓库读排序开关 — HomeRepository.getOutdatedTargetSdkVersionApkModel()
val shouldOrderByPackageNameFirst = MyApplication.sharedPreferences.getBoolean(...)
```

变化通知不走数据, 走 **ViewModel 伴生对象里的静态 SharedFlow**(`SettingsViewModel.scrollBarModeChangedSharedFlow`) — 本质是全局事件总线 (event bus, 一根谁都能喊话的大喇叭). 原本两条流里的一条 (`outdatedOrderChangedSharedFlow`) 已经在 2026-09-21 的排序重构中删除, 剩下这条滚动条的还在:

```kotlin
companion object {
    val scrollBarModeChangedSharedFlow: SharedFlow<String?>     // ← 静态可变状态
        field = MutableSharedFlow()
}

fun emitScrollBarModeChangedSharedFlow(scrollBarMode: String?) {   // 实例方法往静态流里塞事件
    viewModelScope.launch { scrollBarModeChangedSharedFlow.emit(scrollBarMode) }
}
```

收集方: **一个都没有**(全仓 `grep` 只命中定义与 emit). 列表页的旧收集者 `BaseListFragment` / `HomeFragment` 已随 View 层删除, 所以这条流现在是纯粹的空放炮 — 设置项改了值, 只有 SharedPreferences 变, 界面无反应 (现状与裁定见 [R10](../组4-已裁定/07-R10-滚动条stub.md)).

排序开关则已经换成了正确的形态 — 由消费者自己观察数据源, 不再有广播 (`HomeViewModel.outdatedOrderChangeListener`):

```kotlin
// HomeViewModel — 观察自己的 key, replay 问题不复存在
private val outdatedOrderChangeListener =
    SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == MyApplication.getMyString(R.string...._first_key)) {
            outdatedOrderChanges.update { it + 1 }
        }
    }
```

## 直接原因

- 读取方绕过一切抽象直接摸 `MyApplication.sharedPreferences` (这是 [AR-04](06-AR-04-服务定位器上帝对象.md) 服务定位器的一个实例).
- View 时代那条 "框架层 import 功能层" 的跨包依赖 (`BaseListFragment` / `HomeFragment` import `ui.settings`) 已随迁移消失; 但**没有唯一数据源**这件事一点没变: 上面四处照样各自摸 `MyApplication.sharedPreferences`.
- 通知方没有 "可观察的数据源", 只能把 "值变了" 做成事件广播; 而 `MutableSharedFlow()` 没有重放 (replay),**收集方不在场时事件直接丢失**, 而且丢了连条日志都没有 — 现在连收集方都没有了.
- 主题的副作用已经收拢了一半: `AppCompatDelegate.setDefaultNightMode` 随 `26b9094f` 移除, `MyApplication.themeMode` 是一条 `StateFlow<AppThemeMode>` (`MyApplication.themeMode`), 由 `AppTheme` 收集 (`AppTheme() 收集 themeMode 那行`). 剩下一半没动: 写入仍由 `SettingsScreen() 的 onThemeSelect` 直接落 prefs 再调 `setMyTheme`, 仍属 "写路径没有归属".

## 根本原因

SSOT(Single Source of Truth, 唯一数据来源: 每份数据只保存在一个地方, 其他地方都观察它) 没有建立. 设置数据缺一个 "可观察的仓库", 导致每个用它的地方各自发明读取路径; 设置变化缺一个 "数据流", 导致只能用静态广播这种违背单向数据流的补丁 — 数据不再从数据层单向流向界面, 而是在静态总线上横着乱窜, 谁发谁收全靠约定, 编译器和 IDE 都查不出来.

## 修复方案

第一步, 给 SharedPreferences 加 Flow(androidx 官方尚无现成扩展, 十几行即可; 日后想换 DataStore 时只改这一个文件):

```kotlin
// ui/settings/repository/PrefsFlow.kt
fun SharedPreferences.keyFlow(): Flow<String> = callbackFlow {
    val listener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key -> trySend(key) }
    registerOnSharedPreferenceChangeListener(listener)
    awaitClose { unregisterOnSharedPreferenceChangeListener(listener) }
}

fun SharedPreferences.stringFlow(key: String, default: String? = null): Flow<String?> =
    keyFlow()
        .onStart { emit(key) }              // 订阅时先推一次当前值, 取代各处手动初读
        .filter { it == key }
        .map { getString(key, default) }
        .distinctUntilChanged()

fun SharedPreferences.booleanFlow(key: String, default: Boolean = false): Flow<Boolean> =
    keyFlow()
        .onStart { emit(key) }
        .filter { it == key }
        .map { getBoolean(key, default) }
        .distinctUntilChanged()
```

第二步, 建唯一的设置数据仓 (与现有的 `SettingsRepository` — 关于页版本信息仓库 — 避免重名, 取名 `SettingsStore`), 所有键集中定义:

```kotlin
// ui/settings/repository/SettingsStore.kt
class SettingsStore(private val prefs: SharedPreferences) {   // 项目用的是具名 prefs 文件, 不是默认那个
    private fun key(@StringRes id: Int) = MyApplication.getMyString(id)

    val themeMode: Flow<String?> =
        prefs.stringFlow(key(R.string.interface_themes_key),
            key(R.string.interface_themes_follow_system_value))
    val scrollBarMode: Flow<String?> = prefs.stringFlow(key(R.string.interface_scroll_bar_key))
    val allowNetworkData: Flow<Boolean> =
        prefs.booleanFlow(key(R.string.function_allow_network_data_key))
    val outdatedOrderFirst: Flow<Boolean> =
        prefs.booleanFlow(key(R.string.function_outdated_target_order_by_package_name_first_key))
}
```

第三步, 删掉静态总线, 消费方改为观察自己的数据流 (UDF 恢复为: 数据向下流, 事件向上交). 排序开关已经是这个形状的现成实例 — `HomeViewModel.outdatedOrderChangeListener` 自己注册 `OnSharedPreferenceChangeListener`, 把 "键变了" 折进一条 `StateFlow`, 没有任何广播. 照同样的方向收:

```kotlin
// SettingsScreen — 只交意图给 ViewModel, 不再自己碰 prefs
onAllowNetworkChange = { value -> viewModel.setAllowNetworkData(value) },
onScrollBarSelect = { value -> viewModel.setScrollBarMode(value) },
```

```kotlin
// 列表页将来接官方滚动条组件时: 观察 store, 而不是收广播
val scrollBarMode by settingsStore.scrollBarMode.collectAsStateWithLifecycle(initialValue = null)
```

```kotlin
// HomeViewModel — 联网开关从构造注入的设置仓读
private val settingsStore: SettingsStore,   // 构造参数 (经 Factory 注入)
override suspend fun collectModels(): List<MyModel> {
    val allowNetwork = settingsStore.allowNetworkData.first()
    ...
}
```

主题一项**已经按这个方向落地了一半**: `AppCompatDelegate.setDefaultNightMode` 随 `26b9094f` 退役, `MyApplication.themeMode` 是唯一的 `StateFlow<AppThemeMode>`, 由 `AppTheme` 收集 (`AppTheme() 收集 themeMode 那行`), `MainActivity.isAppDark()` 只读它来设窗口明暗. 原方案里 "MainActivity 收集 themeMode" 这一步不必再做, 剩下的只是把 `SettingsScreen() 的 onThemeSelect` 那对 "直接写 prefs + 直接调 `setMyTheme`" 并进 `SettingsStore` 的写入口.

改造后: `SettingsViewModel` 伴生对象只剩 Factory, 那条无人订阅的 `scrollBarModeChangedSharedFlow` 一并删除; 四处偷读 prefs 改成观察 `SettingsStore`; 设置项加一个, 只动 `ui/settings/res/values/strings.xml` (键与文案, 四语) + `SettingsStore` (加一行 Flow) + 消费方 (收集它).


