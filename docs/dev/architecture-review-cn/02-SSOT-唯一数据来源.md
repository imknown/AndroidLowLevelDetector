# SSOT · 唯一数据来源 (Single Source of Truth)

> 返回 [README](README.md). 一份数据只能有一个来源, 一个写入方.

<a id="AR-02"></a>

## AR-02 设置项绕开唯一数据源, 靠静态事件总线广播变化

**严重程度: P0 | 修复难度: 中**
**影响文件: `SettingsScreen.kt`, `SettingsViewModel.kt`, `HomeViewModel.kt`, `HomeRepository.kt`, `MyApplication.kt`**

### 问题核心代码

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

收集方: **一个都没有**(全仓 `grep` 只命中定义与 emit). 列表页的旧收集者 `BaseListFragment` / `HomeFragment` 已随 View 层删除, 所以这条流现在是纯粹的空放炮 — 设置项改了值, 只有 SharedPreferences 变, 界面无反应 (现状与裁定见 [R10](05-已裁定事项.md#R10)).

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

### 直接原因

- 读取方绕过一切抽象直接摸 `MyApplication.sharedPreferences` (这是 [AR-04](01-架构.md#AR-04) 服务定位器的一个实例).
- View 时代那条 "框架层 import 功能层" 的跨包依赖 (`BaseListFragment` / `HomeFragment` import `ui.settings`) 已随迁移消失; 但**没有唯一数据源**这件事一点没变: 上面四处照样各自摸 `MyApplication.sharedPreferences`.
- 通知方没有 "可观察的数据源", 只能把 "值变了" 做成事件广播; 而 `MutableSharedFlow()` 没有重放 (replay),**收集方不在场时事件直接丢失**, 而且丢了连条日志都没有 — 现在连收集方都没有了.
- 主题的副作用已经收拢了一半: `AppCompatDelegate.setDefaultNightMode` 随 `26b9094f` 移除, `MyApplication.themeMode` 是一条 `StateFlow<AppThemeMode>` (`MyApplication.themeMode`), 由 `AppTheme` 收集 (`AppTheme() 收集 themeMode 那行`). 剩下一半没动: 写入仍由 `SettingsScreen() 的 onThemeSelect` 直接落 prefs 再调 `setMyTheme`, 仍属 "写路径没有归属".

### 根本原因

SSOT(Single Source of Truth, 唯一数据来源: 每份数据只保存在一个地方, 其他地方都观察它) 没有建立. 设置数据缺一个 "可观察的仓库", 导致每个用它的地方各自发明读取路径; 设置变化缺一个 "数据流", 导致只能用静态广播这种违背单向数据流的补丁 — 数据不再从数据层单向流向界面, 而是在静态总线上横着乱窜, 谁发谁收全靠约定, 编译器和 IDE 都查不出来.

### 修复方案

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

---

<a id="AR-03"></a>

## AR-03 全局可变单例 myAndroid 有两个写入方

**严重程度: P0(当前为偶发隐患, 结构性风险高) | 修复难度: 低~中**
**影响文件: `AndroidVersionExt.kt`, `HomeRepository.kt`, `MyApplication.kt`**

### 问题核心代码

全局可变对象 (`MyAndroid`):

```kotlin
class MyAndroid(
    var api: Int,          // ← 全部是 var
    var apiFull: String,
    var version: String,
    var dessert: String? = null
)

val myAndroid = MyAndroid(   // 顶层 val, 全项目共享的单身实例
    Build.VERSION.SDK_INT, "${Build.VERSION.SDK_INT}.$minor", Build.VERSION.RELEASE
)
```

写入方一: App 启动 (`MyApplication.onCreate` → `initMyAndroid()`, `initMyAndroid()`).

写入方二: **首页数据仓库**(`HomeRepository.detectAndroid()`, `HomeRepository.detectAndroid()`) — 一个 "取数方法" 顺手改了全局状态:

```kotlin
if (android != null) {
    with(android) {
        myAndroid.api = api.toInt()        // ← 仓库在改全局单例
        myAndroid.apiFull = apiFull
        myAndroid.version = version
        myAndroid.dessert = name
    }
}
```

而全项目的系统版本判断都读它 (`sdkInt`):

```kotlin
private val sdkInt get() = myAndroid.api
fun isAtLeastAndroid12() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || sdkInt >= Build.VERSION_CODES.S
```

### 直接原因

"这台设备的 Android 版本信息" 有两个 "事实版本": `Build.VERSION.*` 推导出的初始值, 和 LLD 数据库 JSON 修正后的值. 两处写入, 多处读取, 没有任何机制保证先后一致 — `isAtLeast*`, `isLatestPreviewAndroid` 等函数在首页加载前后的返回值**理论上可能不同**(大多数设备上两次写入恰好相同, 所以问题平时不发作). 具体触点: 过时应用过滤阈值 `it.targetSdkVersion < myAndroid.api` (`HomeRepository.getOutdatedTargetSdkVersionApkModel() 里的 it.targetSdkVersion < myAndroid.api`) 读的正是这个全局 — 把 `detect()` 里 `detectAndroid` 与 `getOutdatedTargetSdkVersionApkModel` 的调用顺序对调, 过滤结果就会变, 一致性全靠手写顺序维持.

### 根本原因

可变全局单例 = 把 "数据" 和 "数据的位置" 混在一起: 值随时可被任何代码改写, 读到的结果取决于 "谁最后写过", 这就是**时间耦合**(temporal coupling, 正确性依赖调用顺序). LLD 的 `known` 数据本应作为参数参与计算, 却通过篡改全局变量来 "顺便" 传递.

### 修复方案

冻结为不可变 (immutable, 创建后不能改), 二次加工用 `copy` 返回新对象, 显式传递:

```kotlin
// ui/common/AndroidVersionExt.kt
data class MyAndroid(                       // data class + 全 val
    val api: Int,
    val apiFull: String,
    val version: String,
    val dessert: String?
)

object AndroidInfo {
    /** 启动时构建一次, 此后只读 — isAtLeast* 只依赖它 */
    val current: MyAndroid by lazy { build() }

    private fun build(): MyAndroid {
        // ... 原 initMyAndroid() 的推导逻辑原样搬入, 最后 return MyAndroid(...)
    }

    /** 纯函数: 用 LLD 已知列表修正, 返回新对象, 不动原对象 */
    fun enrichWithLld(base: MyAndroid, lld: Lld): MyAndroid {
        val known = lld.android.known.find { it.apiFull == base.apiFull } ?: return base
        return base.copy(
            api = known.api.toInt(),
            apiFull = known.apiFull,
            version = known.version,
            dessert = known.name
        )
    }
}
```

```kotlin
// HomeRepository.detectAndroid — 不再写全局, 改用参数
fun detectAndroid(lld: Lld?, androidInfo: MyAndroid): MyModel {   // androidInfo 由 VM 传入
    val enriched = lld?.let { AndroidInfo.enrichWithLld(androidInfo, it) } ?: androidInfo
    ...
}
```

`isAtLeast*` 一律改读 `AndroidInfo.current` (等价于原来的初始值, 不再被中途篡改). `initMyAndroid()` 从 `MyApplication.onCreate` 中删除 (`by lazy` 自带一次性初始化, 且懒加载避免启动期反射开销).

迁移注意: `myAndroid` 的读取点约 10 处 (`HomeRepository`, `AndroidVersionExt`), 全局替换为 `AndroidInfo.current` 后编译器会兜底找出遗漏.

---

返回 [README](README.md)
