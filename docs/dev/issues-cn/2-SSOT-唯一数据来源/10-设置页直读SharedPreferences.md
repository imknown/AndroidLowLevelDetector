<a id="i10"></a>

# 10 · Settings 在 Composable 里直接读写 SharedPreferences

> 返回 [README 索引](../README.md) · [2 · SSOT · 唯一数据来源](../README.md#2--ssot--唯一数据来源).

**严重程度: P1 | 修复难度: 中**
**影响文件: `SettingsScreen.kt`, `SettingsViewModel.kt`**

与 [#08](08-设置无唯一数据来源.md) 同根, 是它的 UI 侧投影: 与 #08 的 `SettingsStore` 方案合流实施最省. 证据就是下面这段现状 — `SettingsScreen()` 仍是四组 `remember { ... MyApplication.sharedPreferences... }` 初读, 回调里直写 SP 并直调 `MyApplication.setMyTheme()` / `setMyScrollBar()`.

**现象**: `SettingsScreen` 一次性做了三件事 — 读 SP, 持有本地 `mutableStateOf`, 在点击回调里写 SP 并调 `MyApplication.setMyTheme()` / `setMyScrollBar()`; 而 `SettingsViewModel` 只负责版本信息与版本号点击彩蛋.

**直接原因**: 迁移时把旧 `SettingsFragment` 的 "读 SP → 本地状态 → 写 SP" 三段式 1:1 搬进了 Composable, 没有顺手把写操作收进 ViewModel.

**根本原因**: **缺少 UI 状态契约**. 现在的状态所有权是割裂的: 值既存在 SP 里, 又存在 4 个互不相关的 `remember { mutableStateOf(...) }` 里. 后果有三: (1) 无法单测 (组合里直接摸全局单例); (2) 主题与滚动条 **模式** 已经骑在 `MyApplication` 的 `StateFlow` 上 (`themeMode`, `scrollBarMode`), 所以外面改了值能部分反映到界面 (明暗, 有无指示条), 但设置页显示的那两个存值 (`themeValue`, `scrollBarValue`) 仍来自一次性 `remember` 初读, 永不刷新; (3) 以后加 "偏好联动/校验/异步" 没有落点.

**问题代码**:

```kotlin
// SettingsScreen.kt → SettingsScreen() 的各 onXxxSelect 回调
onThemeSelect = { value ->
    themeValue = value
    MyApplication.sharedPreferences.edit { putString(themeKey, value) }   // UI 直接写存储
    MyApplication.setMyTheme(value)
},
onScrollBarSelect = { value ->
    scrollBarValue = value
    MyApplication.sharedPreferences.edit { putString(scrollBarKey, value) }
    MyApplication.setMyScrollBar(value)
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
    fun setScrollBarMode(value: String) { write(KEY_SCROLL_BAR, value); MyApplication.setMyScrollBar(value) }
    fun setAllowNetwork(value: Boolean) { write(KEY_ALLOW_NETWORK, value) }
    fun setOutdatedOrderFirst(value: Boolean) { write(KEY_OUTDATED_ORDER, value) }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, ...) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(uiState = uiState, onThemeSelect = viewModel::setTheme, ...)
}
```

两个 `setMyXxx` 调用不能省: 明暗与指示条靠 `MyApplication` 那两条 `StateFlow` 生效, 写 SP 不等于写流 (这条 "写路径分两步" 的形状归 #08 一并收敛, `themeMode` / `scrollBarMode` 是否继续留在伴生对象上是那个问题). 可拖动档仍是 stub, 见 [#37](../7-UI与无障碍/37-滚动条可拖动是stub.md).

好处: Screen 变成纯 "collect + 转发事件"; `MyApplication.sharedPreferences` 这个全局单例从 UI 层消失; 存值迁移 (例如 `MyApplication.initTheme()` 对已退役的 "省电模式" 存值做的一次性归一) 也变成可测的纯数据映射.
