<a id="F3"></a>

# F3 · Settings 在 Composable 里直接读写 SharedPreferences (P1, 不合理)

> 返回 [README 索引](../README.md) · [组2 · 主线重构 — 架构优先](../README.md#组2--主线重构--架构优先).


> 发现: Hy4-preview (F3). 2026-09-25 复核: **仍开放**; 与 [AR-02](01-AR-02-设置SSOT与死总线.md) 同根, 与其 `SettingsStore` 方案合流实施最省.

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


