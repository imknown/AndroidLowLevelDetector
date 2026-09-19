# 08 第 5 步 · Settings 页面重建

> **⚠️ 2026-09-19 实现期更正（用户提供旧版截图比对）**：①8.2 的"summary = 当前选中项标签"改为**静态文案**（与旧 XML 的 `app:summary` 逐字一致，如"点击以显示主题选择器"）——当前选中项只在对话框里体现，随之 `getOrNull` 标签查找与 fast scroll "2" 兜底整段删除；②**补 `HorizontalDivider`**：两个 `PreferenceCategory` 之间的分隔线是旧观感的一部分，8.3 的 `SettingsCategoryHeader` 前各加一条；③滚动条接线按第 3 步决策推迟（正文 8.1 末段作废）。Preview：`SettingsScreen` 状态提升拆出纯数据 `SettingsContent` 并配双主题 `@Preview`。
>
> **二轮更正（反编译 androidx.preference 1.2.1 取基准）**：④分隔线色 = 库常量 **`#1f000000`（12% 黑）1dp**（`preference_list_divider_material`，不随主题）——Compose 默认 `outlineVariant` 深色下偏亮，改用常量色；⑤`SettingsContent` 背景统一为 **`surfaceContainer`**（用户拍板：旧 preference 窗口底色偏黑与旧列表页不一致，属无意差异，四页统一）；⑥列表上下内边距 = **0**（`PreferenceFragmentList.Material` 覆盖为 0dp），首分类间距全靠其自带 16dp margin + 8dp padding（=`SettingsCategoryHeader` top 24dp），去掉 contentPadding top 的 12dp；⑦分类标题色**保持 primary**——app 未覆盖 `preferenceCategoryTitleTextColor`，库在 AppCompat 主题下解析为 colorAccent（M3 = colorPrimary），两边同色，观感差异来自背景；字重 Body2 = titleSmall 等价。⑧外链行 title 资源应为 `*_title` 而非 `*_key`（原计划 8.3 表格把 key 当 title 用，真机截图抓出）。
>
> **三轮澄清（app 主题实为 `Theme.Material3Expressive.DynamicColors.DayNight.NoActionBar`）**：反编译 material 1.14.0 证实 Expressive 相对 Material3 基线的 23 项覆盖**全是焦点环**，不碰分隔线/偏好标题色/内边距——上述基准值不受影响。`DynamicColors` 说明 View 侧同样取壁纸动态色，与 Compose `AppTheme(dynamicColor=true)` 同源，**不存在双真源色差**。真正的 Expressive 差异（焦点环/形状语言/动效）在 Compose 侧需 material3 1.5.0-alpha，维持决策 8：BOM 升 1.5 后与滚动条、Style API 一并处理。

> 所属迁移计划：[README](README.md) · 上一章：[07 第 4 步 Home 与 Others 迁移](07-第4步-Home与Others迁移.md) · 下一章：[09 第 6 步 Navigation 3 与 MainActivity 切换](09-第6步-Navigation3与MainActivity切换.md)

**改动量：重写 1 个文件（约 30 行）、新增 1 个文件（约 200 行）、1 个单词级修改。**
这是唯一"重写"而非"翻译"的一步：**官方至今没有 Compose 版 Preference 库**（androidx.preference 停在 2023 年的 1.2.1，无 Compose 支持），官方参考应用 Now in Android 的设置页就是用普通 Material 3 组件手写的。本步照此办理，用 `LazyColumn` + `ListItem` + `Switch` + `AlertDialog` 重建设置页。

好消息是**存储完全不动**：仍读写同一份 `SharedPreferences`（键是 `translatable="false"` 的字符串资源值），老用户的设置原样继承，DataStore 迁移等现代化留作后续独立需求（见第 10 章遗留优化）。

| 文件 | 操作 | 内容 |
| --- | --- | --- |
| `ui/settings/SettingsScreen.kt` | 新增 | 设置页全部 UI（三个子步逐步长成） |
| `ui/settings/SettingsFragment.kt` | 重写 | 换成 ComposeView 壳（与列表页同构） |
| `ui/base/list/MyModelListScreen.kt` | 1 词修改 | 过渡期辅助函数 `rememberBottomBarHeight` 由 `private` 改 `internal`，设置页复用 |

## 8.1 子步 a：Fragment 壳 + 页面骨架 + 偏好读取模式

`SettingsFragment` 与列表页完全同构（换成 `SettingsViewModel` 接线），此处从略。 `SettingsScreen.kt` 先立骨架——重点是**偏好的读取模式**：

```kotlin
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // ---- 偏好当前值：进屏读一次 SharedPreferences，此后本地状态是真源，变更时"写穿"回 SharedPreferences ----
    val themeKey = stringResource(R.string.interface_themes_key)
    val themeDefaultValue = stringResource(R.string.interface_themes_follow_system_value)
    var themeValue by remember {
        mutableStateOf(
            MyApplication.sharedPreferences.getString(themeKey, null) ?: themeDefaultValue
        )
    }

    val scrollBarKey = stringResource(R.string.interface_scroll_bar_key)
    val scrollBarDefaultValue = stringResource(R.string.interface_no_scroll_bar_value)
    var scrollBarValue by remember {
        mutableStateOf(
            MyApplication.sharedPreferences.getString(scrollBarKey, null) ?: scrollBarDefaultValue
        )
    }

    val allowNetworkKey = stringResource(R.string.function_allow_network_data_key)
    var allowNetwork by remember {
        mutableStateOf(MyApplication.sharedPreferences.getBoolean(allowNetworkKey, false))
    }

    val outdatedOrderKey =
        stringResource(R.string.function_outdated_target_order_by_package_name_first_key)
    var outdatedOrderFirst by remember {
        mutableStateOf(MyApplication.sharedPreferences.getBoolean(outdatedOrderKey, false))
    }

    // ---- 版本信息：照抄旧 Fragment 的"订阅 + 只初始化一次" ----
    val versionState by viewModel.version.collectAsStateWithLifecycle()   // 同列表页的订阅套路
    LaunchedEffect(viewModel) {          // 进组合触发一次；VM 内部有防重入，重复调用无害
        viewModel.setBuiltInDataVersion(context.packageManager, context.packageName)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = /* 与 MyModelListContent 相同的过渡期内边距，横向 insets + 底部导航高度 */,
    ) {
        // 子步 b/c 的 item 在这里逐个加入
    }
}
```

设置页旧代码对**它自己的列表**也应用滚动条偏好（`listView.setScrollBarMode`），新页面照做：接上第 3 步的两个成果即可——`val listState = rememberLazyListState()`，`LazyColumn(state = listState, modifier = ...then(if (rememberScrollBarEnabled()) Modifier.verticalScrollbar(listState.scrollIndicatorState!!, ...) else Modifier))`。

**讲解：偏好为什么这样读**——Compose 没有官方 Preference 组件，惯用法（也是 Now in Android 的思路）是：

1. **进屏读一次**：`remember { mutableStateOf(读 SP) }`（注意 `stringResource` 要在 `remember` **外**先求值——`remember` 的 lambda 不是组合上下文，不能调用可组合函数）；
2. **本地状态为真源**：界面渲染只看 `by mutableStateOf` 的变量；
3. **变更时写穿（write-through）**：事件回调里 ① 更新本地状态 ② `edit().putXxx().apply()` 写回 ③ 触发副作用（换主题/发事件流）。

`MyApplication.sharedPreferences` 是全局单例，在组合里读它一次没有生命周期问题（对比旧代码在 `onViewCreated` 里读，时机等价）。

**白话（三段式）**：偏好读写像班级的**黑板**——进教室先**抄一遍黑板**（读一次进 `remember`）；上课期间以**自己的笔记**为准（本地状态是真源，界面只看它）；笔记改了要**同步写回黑板**（`edit().putXxx().apply()`），下节课（重启）才不丢。

✅ 正例 / ❌ 反例（本步最容易踩的两个坑）：

```kotlin
// ❌ 坑一：把 stringResource 写进 remember 的 lambda——编译直接报错！
//   remember { } 的块不是组合上下文，里面不能调用 @Composable 函数
var value by remember { mutableStateOf(stringResource(R.string.interface_themes_key)) }

// ✅ 正解：先在组合里（函数体）解析好，再交给 remember
val themeKey = stringResource(R.string.interface_themes_key)
var value by remember { mutableStateOf(sp.getString(themeKey, null)) }
```

```kotlin
// ❌ 坑二：界面每次重组都现读 SharedPreferences——组合函数要保持"纯"，
//   读盘这种事只该发生在 remember 初始化和事件回调里
Text(MyApplication.sharedPreferences.getString(key, null) ?: "")

// ✅ 正解：读一次进状态，渲染只看状态（写穿模式见上面三段式）
```

## 8.2 子步 b：两个"下拉选择" → 选择对话框

### before（XML + 回调）

```xml
<ListPreference
    app:defaultValue="@string/interface_themes_follow_system_value"
    app:entries="@array/themeKeys" app:entryValues="@array/themeValues"
    app:key="@string/interface_themes_key"
    app:summary="@string/interface_themes_summary"
    app:title="@string/interface_themes_title" />
```

```kotlin
themeModePref?.setOnPreferenceChangeListener { preference, newValue ->
    MyApplication.setMyTheme(newValue as? String); true
}
```

### after

入口行（显示当前选中项的标签，点击弹对话框）：

```kotlin
val themeLabels = stringArrayResource(R.array.themeKeys)     // 显示用（可翻译）
val themeValues = stringArrayResource(R.array.themeValues)   // 存储值（"−1"/"1"/"2"/"3"）
val scrollBarLabels = stringArrayResource(R.array.scrollBarKeys)
val scrollBarValues = stringArrayResource(R.array.scrollBarValues)

// LazyColumn 里：
item {
    SettingsChoiceRow(
        title = R.string.interface_themes_title,
        summary = themeLabels.getOrNull(themeValues.indexOf(themeValue)),
        onClick = { showThemeDialog = true },
    )
}
```

对话框本体（`labels.zip(values)` 把两个数组配成对）：

```kotlin
if (showThemeDialog) {
    SettingsChoiceDialog(
        title = stringResource(R.string.interface_themes_title),
        labels = themeLabels,
        values = themeValues,
        selected = themeValue,
        onSelect = { value ->
            themeValue = value
            MyApplication.sharedPreferences.edit().putString(themeKey, value).apply()
            MyApplication.setMyTheme(value)          // 副作用：立刻换深浅色（AppCompatDelegate）
            showThemeDialog = false
        },
        onDismiss = { showThemeDialog = false },
    )
}
```

```kotlin
@Composable
private fun SettingsChoiceDialog(
    title: String,               // 标题（调用方已解析成字符串再传）
    labels: Array<String>,       // 显示用文案（可翻译）
    values: Array<String>,       // 存储用值（translatable="false"，永不变）
    selected: String,            // 当前选中的值（判断哪个 RadioButton 点亮）
    onSelect: (String) -> Unit,  // 选中事件向上抛
    onDismiss: () -> Unit,       // 关闭事件向上抛
) {
    AlertDialog(
        onDismissRequest = onDismiss,   // 点对话框外/系统返回 = 关闭
        title = { Text(title) },        // 槽位（slot）参数：值是 @Composable，内容你说了算
        text = {
            Column {
                labels.zip(values).forEach { (label, value) ->   // 两个数组配成一对对（文案, 值）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value) }        // 整行可点（点单选按钮以外也行）
                            .padding(vertical = 12.dp),           // 行内垂直留白：好点
                        verticalAlignment = Alignment.CenterVertically,   // 圆点与文字垂直居中
                    ) {
                        RadioButton(selected = value == selected, onClick = null)  // 只展示、不接点击
                        Text(text = label, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = {   // 槽位要求非空：放一个"取消"按钮兜底（选中即生效，无需"确定"）
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}
```

滚动条选择器同构，`onSelect` 里多发一个事件（对应旧代码 `emitScrollBarModeChangedSharedFlow`）：

```kotlin
onSelect = { value ->
    scrollBarValue = value
    MyApplication.sharedPreferences.edit().putString(scrollBarKey, value).apply()
    viewModel.emitScrollBarModeChangedSharedFlow(value)   // 三个列表页收到后即时切换滚动条
    showScrollBarDialog = false
}
```

**讲解**：

- **`AlertDialog` 是槽位式 API**：`title` / `text` / `confirmButton` 三个参数都是 `@Composable` 槽——塞什么内容你说了算，替代了 ListPreference + 自定义 dialog theme（`themes.xml` 里那段 `MaterialPreferenceDialog` 覆盖随之失去意义，第 7 步删除）。
- **对话框放 `LazyColumn` 外面**：它不是列表项，是覆盖层。`if (showThemeDialog)` 一关，整棵对话框子树离开组合、状态丢弃——不需要 show/hide 两个方向都写。
- **`showXxxDialog` 用 `rememberSaveable`**（子步 a 已声明）：旋转屏幕时对话框保持打开，对应系统对话框的默认行为。
- **`RadioButton(onClick = null)`**：把点击交给整行的 `clickable`，单选框只做展示——"行可点、控件随行"是 M3 的惯用搭配。

## 8.3 子步 c：开关、外链、版本信息

开关行（整行可点 + 尾部 `Switch`）：

```kotlin
item {
    SettingsSwitchRow(
        title = R.string.function_allow_network_data_title,
        summary = R.string.function_allow_network_data_summary,
        checked = allowNetwork,
        onCheckedChange = { value ->
            allowNetwork = value
            MyApplication.sharedPreferences.edit().putBoolean(allowNetworkKey, value).apply()
        },
    )
}

item {
    SettingsSwitchRow(
        title = R.string.function_outdated_target_order_by_package_name_first_title,
        summary = R.string.function_outdated_target_order_by_package_name_first_summary,
        checked = outdatedOrderFirst,
        onCheckedChange = { value ->
            outdatedOrderFirst = value
            MyApplication.sharedPreferences.edit().putBoolean(outdatedOrderKey, value).apply()
            viewModel.emitOutdatedOrderChangedSharedFlow()   // Home 页对应条目即时重排
        },
    )
}
```

```kotlin
@Composable
private fun SettingsSwitchRow(
    @StringRes title: Int,                 // 标题资源 id
    @StringRes summary: Int,               // 副标题资源 id
    checked: Boolean,                      // 开关状态：外部传入（哑组件，自己不记）
    onCheckedChange: (Boolean) -> Unit,    // 开关事件：向上抛
) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },        // 主行（= Preference 的 title）
        supportingContent = { Text(stringResource(summary)) },    // 副行（= summary）
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },  // 尾部
        modifier = Modifier.clickable { onCheckedChange(!checked) },   // 整行也能点（体验细节）
    )
}
```

外链行（五个，数据驱动）。`openInExternal` 从旧 Fragment 平移成普通函数，`Context` 由调用处传入：

```kotlin
private data class SettingsLink(
    @StringRes val title: Int,
    @StringRes val summary: Int,
    @StringRes val uri: Int,
)

private val settingsLinks = persistentListOf(
    SettingsLink(R.string.about_shop_key, R.string.about_shop_summary, R.string.about_shop_uri),
    SettingsLink(R.string.about_source_key, R.string.about_source_summary, R.string.about_source_uri),
    SettingsLink(R.string.about_privacy_policy_key, R.string.about_privacy_policy_summary, R.string.about_privacy_policy_uri),
    SettingsLink(R.string.about_licenses_key, R.string.about_licenses_summary, R.string.about_licenses_uri),
    SettingsLink(R.string.translation_language_and_translator, R.string.translator_more_info, R.string.translator_website),
)

// LazyColumn 里：
items(settingsLinks) { link ->
    val context = LocalContext.current
    val uri = stringResource(link.uri)   // 组合阶段解析好：点击回调不是组合上下文，不能再调 stringResource
    ListItem(
        headlineContent = { Text(stringResource(link.title)) },
        supportingContent = { Text(stringResource(link.summary)) },
        modifier = Modifier.clickable { openInExternal(context, uri) },
    )
}

private fun openInExternal(context: Context, uri: String) {
    val intent = Intent(Intent.ACTION_VIEW, uri.toUri()).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    } else {
        context.toast(R.string.no_browser_found)
    }
}
```

版本行（订阅 `State<Version>` + 七连击彩蛋）：

```kotlin
item {
    val version = (versionState as? State.Done)?.value
    ListItem(
        headlineContent = { Text(stringResource(R.string.about_version_title)) },
        supportingContent = {
            // 槽位本体恒为非空 lambda；数据为空就什么都不画（条件渲染，而非隐藏）
            version?.let { v ->
                Text(
                    stringResource(          // 支持格式化参数，对应旧 getMyString(id, *args)
                        v.id,
                        v.versionName, v.versionCode, v.assetLldVersion,
                        v.distributor, v.installer, v.firstInstallTime, v.lastUpdateTime,
                    )
                )
            }
        },
        modifier = Modifier.clickable {
            viewModel.getVersionClickedMessage()?.let { context.toast(it) }   // 七连击彩蛋逻辑在 VM 里，原样复用
        },
    )
}
```

分类标题（对应 `PreferenceCategory`）：

```kotlin
@Composable
private fun SettingsCategoryHeader(@StringRes titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
    )
}
```

**讲解**：

- **`ListItem`**：M3 标准行组件，三个具名槽（`headlineContent`/`supportingContent`/`trailingContent`）正好对应 Preference 的 title/summary/widgetLayout——`preference_widget_material_switch.xml` 这个自定义开关布局文件从此无用（第 7 步删）。
- **`Switch` 的状态由外部传入**（`checked` + `onCheckedChange` 都在外）——它自己不存状态，这正是"状态提升"（02 章第 3 节）：开关是哑组件，真源在 `SettingsScreen` 的本地状态里。
- **`Intent`/`Toast` 这类 Android 交互照旧**：`LocalContext.current` 拿 Context，点击回调里随便用——Compose 替换的是"画界面"，不是平台 API。
- **shop 链接的双 flavor 逻辑零改动**：`stringResource(R.string.about_shop_uri)` 读的是当前 flavor 的资源，构建期决定 URI 的机制与 View 时代完全一致。

## 8.4 验证清单

1. **设置继承**：从旧版本升级上来的设备，主题/滚动条/两个开关的旧值全部正确显示（SharedPreferences 未动）；
2. 主题切换：选"总是深色" → 整个 app（包括 Compose 页和 View 骨架）立刻变深色（`AppCompatDelegate` 仍在工作，MainActivity 还是 AppCompatActivity）；
3. 滚动条切换：立即切到任一列表页 → 滚动条模式即时生效（第 3 步接好的事件流）；
4. 过期应用排序开关：切回 Home 页 → 对应条目详情刷新；
5. 外链五个全部能打开；无浏览器环境时出 Toast；
6. 版本摘要正确；连点 7 次出彩蛋 Toast；
7. 旋转屏幕：已打开的对话框保持打开（`rememberSaveable`）。

## 本步小结

- 偏好读写三段式：**读一次 → 本地真源 → 变更写穿 + 副作用**；
- 槽位式组件（`AlertDialog`）与哑组件 + 状态提升（`Switch`）两个 M3 惯用法；
- `LazyColumn` + `ListItem` 取代 `PreferenceScreen` 的完整对应关系。

最后一搏：把导航骨架也换成 Compose——[09 第 6 步 Navigation 3 与 MainActivity 切换](09-第6步-Navigation3与MainActivity切换.md)。
