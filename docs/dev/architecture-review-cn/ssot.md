# SSOT · 唯一数据来源(Single Source of Truth)

> 返回 [README](README.md)。一份数据只能有一个来源、一个写入方。

<a id="AR-02"></a>

## AR-02 设置项绕开唯一数据源，靠静态事件总线广播变化

**严重程度：P0 ｜ 修复难度：中**
**影响文件：`SettingsViewModel.kt`、`BaseListFragment.kt`、`HomeFragment.kt`、`HomeViewModel.kt`、`HomeRepository.kt`、`SettingsFragment.kt`、`MyApplication.kt`**

### 问题核心代码

「设置」这份本该只有一处权威的数据，现状是**写一处、抄四路、广播两条**：

写入（唯一的正路，`PreferenceFragmentCompat` 自动写 SharedPreferences）；然后四层各自直接偷读：

```kotlin
// ① 启动时读主题 —— MyApplication.kt:86
val themesValue = sharedPreferences.getString(getMyString(R.string.interface_themes_key), null)

// ② 界面基类读滚动条 —— BaseListFragment.kt:43
val scrollBarMode = MyApplication.sharedPreferences.getString(scrollBarModeKey, null)

// ③ ViewModel 读联网开关 —— HomeViewModel.kt:45
val allowNetwork = MyApplication.sharedPreferences.getBoolean(
    MyApplication.getMyString(R.string.function_allow_network_data_key), false)

// ④ 仓库读排序开关 —— HomeRepository.kt:1129
val shouldOrderByPackageNameFirst = MyApplication.sharedPreferences.getBoolean(...)
```

变化通知不走数据，走 **ViewModel 伴生对象里的静态 SharedFlow**（`SettingsViewModel.kt:38`）——本质是全局事件总线（event bus，一根谁都能喊话的大喇叭）：

```kotlin
companion object {
    val scrollBarModeChangedSharedFlow: SharedFlow<String?>     // ← 静态可变状态
        field = MutableSharedFlow()
    val outdatedOrderChangedSharedFlow: SharedFlow<Unit>
        field = MutableSharedFlow()
}

fun emitScrollBarModeChangedSharedFlow(scrollBarMode: String?) {   // 实例方法往静态流里塞事件
    viewModelScope.launch { scrollBarModeChangedSharedFlow.emit(scrollBarMode) }
}
```

收集方横跨功能包（`BaseListFragment.kt:47`、`HomeFragment.kt:41`）：

```kotlin
// HomeFragment —— ui.home 功能直接 import ui.settings 的 SettingsViewModel 伴生对象
SettingsViewModel.outdatedOrderChangedSharedFlow.flowWithLifecycle(...).collect {
    listViewModel.payloadOutdatedTargetSdkVersionApk()
}
```

### 直接原因

- 读取方绕过一切抽象直接摸 `MyApplication.sharedPreferences`（这是 [AR-04](architecture.md#AR-04) 服务定位器的一个实例）。
- 依赖方向也颠倒了：`ui.base.list`（框架层）import `ui.settings`（功能层）——框架不应该认识任何具体功能。
- 通知方没有「可观察的数据源」，只能把「值变了」做成事件广播；而 `MutableSharedFlow()` 没有重放（replay），**收集方不在场时事件直接丢失**，而且丢了连条日志都没有。
- 主题的副作用（`AppCompatDelegate.setDefaultNightMode`）散在 `MyApplication.setMyTheme`（启动）和 `SettingsFragment`（改设置时）两处。

### 根本原因

SSOT（Single Source of Truth，唯一数据来源：每份数据只保存在一个地方，其他地方都观察它）没有建立。设置数据缺一个「可观察的仓库」，导致每个用它的地方各自发明读取路径；设置变化缺一个「数据流」，导致只能用静态广播这种违背单向数据流的补丁——数据不再从数据层单向流向界面，而是在静态总线上横着乱窜，谁发谁收全靠约定，编译器和 IDE 都查不出来。

### 修复方案

第一步，给 SharedPreferences 加 Flow（androidx 官方尚无现成扩展，十几行即可；日后想换 DataStore 时只改这一个文件）：

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
        .onStart { emit(key) }              // 订阅时先推一次当前值，取代各处手动初读
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

第二步，建唯一的设置数据仓（与现有的 `SettingsRepository`——关于页版本信息仓库——避免重名，取名 `SettingsStore`），所有键集中定义：

```kotlin
// ui/settings/repository/SettingsStore.kt
class SettingsStore(context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private fun key(@StringRes id: Int) = context.getString(id)

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

第三步，删掉静态总线，消费方改为观察自己的数据流（UDF 恢复为：数据向下流、事件向上交）：

```kotlin
// BaseListFragment —— 不再读 prefs、不再收集别的功能的静态流
private val interfaceSettings: InterfaceSettingsViewModel by activityViewModels(...)

// onViewCreated:
viewLifecycleOwner.lifecycleScope.launch {
    interfaceSettings.scrollBarMode
        .flowWithLifecycle(viewLifecycleOwner.lifecycle)
        .collect { binding.recyclerView.setScrollBarMode(it) }   // 初值 + 变化同一条流
}

// InterfaceSettingsViewModel —— activity 级共享的一份状态
class InterfaceSettingsViewModel(store: SettingsStore) : ViewModel() {
    val scrollBarMode: StateFlow<String?> =
        store.scrollBarMode.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val themeMode: StateFlow<String?> =
        store.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
```

```kotlin
// HomeViewModel —— 联网开关从构造注入的设置仓读
private val settingsStore: SettingsStore,   // 构造参数（经 Factory 注入）
override suspend fun collectModels(): List<MyModel> {
    val allowNetwork = settingsStore.allowNetworkData.first()
    ...
}
```

主题副作用收拢到唯一收集点（`MainActivity`）：

```kotlin
// MainActivity.onCreate —— 唯一调用 setDefaultNightMode 的地方
lifecycleScope.launch {
    interfaceSettings.themeMode                       // activity 级共享的 InterfaceSettingsViewModel
        .flowWithLifecycle(lifecycle)
        .collect { MyApplication.setMyTheme(it) }
}
```

改造后：`SettingsViewModel` 伴生对象只剩 Factory；`emitXxxSharedFlow`、`outdatedOrderChangedSharedFlow`、`HomeFragment` 对 `ui.settings` 的 import 全部删除；设置项加一个，只动 `preferences.xml` + `SettingsRepository`（加一行 Flow）+ 消费方（收集它）。

---

<a id="AR-03"></a>

## AR-03 全局可变单例 myAndroid 有两个写入方

**严重程度：P0（当前为偶发隐患，结构性风险高） ｜ 修复难度：低~中**
**影响文件：`AndroidVersionExt.kt`、`HomeRepository.kt`、`MyApplication.kt`**

### 问题核心代码

全局可变对象（`AndroidVersionExt.kt:53`）：

```kotlin
class MyAndroid(
    var api: Int,          // ← 全部是 var
    var apiFull: String,
    var version: String,
    var dessert: String? = null
)

val myAndroid = MyAndroid(   // 顶层 val，全项目共享的单身实例
    Build.VERSION.SDK_INT, "${Build.VERSION.SDK_INT}.$minor", Build.VERSION.RELEASE
)
```

写入方一：App 启动（`MyApplication.onCreate` → `initMyAndroid()`，`AndroidVersionExt.kt:104`）。

写入方二：**首页数据仓库**（`HomeRepository.detectAndroid()`，`HomeRepository.kt:107`）——一个「取数方法」顺手改了全局状态：

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

而全项目的系统版本判断都读它（`AndroidVersionExt.kt:114`）：

```kotlin
private val sdkInt get() = myAndroid.api
fun isAtLeastAndroid12() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || sdkInt >= Build.VERSION_CODES.S
```

### 直接原因

「这台设备的 Android 版本信息」有两个「事实版本」：`Build.VERSION.*` 推导出的初始值，和 LLD 数据库 JSON 修正后的值。两处写入、多处读取，没有任何机制保证先后一致——`isAtLeast*`、`isLatestPreviewAndroid` 等函数在首页加载前后的返回值**理论上可能不同**（大多数设备上两次写入恰好相同，所以问题平时不发作）。具体触点：过时应用过滤阈值 `it.targetSdkVersion < myAndroid.api`（`HomeRepository.kt:1108`）读的正是这个全局——把 `detect()` 里 `detectAndroid` 与 `getOutdatedTargetSdkVersionApkModel` 的调用顺序对调，过滤结果就会变，一致性全靠手写顺序维持。

### 根本原因

可变全局单例 = 把「数据」和「数据的位置」混在一起：值随时可被任何代码改写，读到的结果取决于「谁最后写过」，这就是**时间耦合**（temporal coupling，正确性依赖调用顺序）。LLD 的 `known` 数据本应作为参数参与计算，却通过篡改全局变量来「顺便」传递。

### 修复方案

冻结为不可变（immutable，创建后不能改），二次加工用 `copy` 返回新对象、显式传递：

```kotlin
// ui/common/AndroidVersionExt.kt
data class MyAndroid(                       // data class + 全 val
    val api: Int,
    val apiFull: String,
    val version: String,
    val dessert: String?
)

object AndroidInfo {
    /** 启动时构建一次，此后只读 —— isAtLeast* 只依赖它 */
    val current: MyAndroid by lazy { build() }

    private fun build(): MyAndroid {
        // ……原 initMyAndroid() 的推导逻辑原样搬入，最后 return MyAndroid(…)
    }

    /** 纯函数：用 LLD 已知列表修正，返回新对象，不动原对象 */
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
// HomeRepository.detectAndroid —— 不再写全局，改用参数
fun detectAndroid(lld: Lld?, androidInfo: MyAndroid): MyModel {   // androidInfo 由 VM 传入
    val enriched = lld?.let { AndroidInfo.enrichWithLld(androidInfo, it) } ?: androidInfo
    ...
}
```

`isAtLeast*` 一律改读 `AndroidInfo.current`（等价于原来的初始值，不再被中途篡改）。`initMyAndroid()` 从 `MyApplication.onCreate` 中删除（`by lazy` 自带一次性初始化，且懒加载避免启动期反射开销）。

迁移注意：`myAndroid` 的读取点约 10 处（`HomeRepository`、`AndroidVersionExt`），全局替换为 `AndroidInfo.current` 后编译器会兜底找出遗漏。

---

返回 [README](README.md)
