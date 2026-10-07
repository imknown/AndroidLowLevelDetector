<a id="i35"></a>

# 35 · `onBack` 与多返回栈自相矛盾

> 返回 [README 索引](../README.md) · [7 · UI 与无障碍](../README.md#7--ui-与无障碍).

**严重程度: P2 | 修复难度: 低**

**现象**: 每个标签各自持有一条 `NavBackStack` (`AppRoot()`:  
`topLevelTabs.associate { tab -> tab.key to rememberNavBackStack(tab.key) }`),  
但 `NavDisplay` 的 `onBack` 一律 `activity?.finish()`, 从不 pop. 本仓库没有任何 `BackHandler` / `onBackPressed` 接线,  
所以这条 lambda 就是返回事件的出口.

**直接原因**: 迁移时选择保持旧行为 (任何标签按返回直接退出), `AppRoot()` 那行后面的注释写着 "keep legacy behavior:  
back from any tab exits directly". 这个选择是否与旧 Fragment show/hide 时代等价, 是判断而不是代码事实: 旧载体已删, 现在无从比对.

**根本原因**: 迁移引入了 "多返回栈" 这一新能力, 却没有同步更新返回语义.

**当前危害是潜在的**: `ui/navigation/NavKeys.kt` 只声明四个顶层 `data object` 键 (`HomeKey` / `OthersKey` / `PropKey` /  
`SettingsKey`), 全仓没有向任何栈 push 第二个键的调用点 (`grep` 无 `backStack` 的  
`add`), 所以每条栈今天恒为 1 个 entry, `onBack` "从不 pop" 目前没有可观察后果. 但只要任一标签 push 第二个页面 (详情,  
WebView 等), 返回键就立刻变成 "直接退出 App", 这是典型的 "改了一半".

**问题代码**:

```kotlin
// AppRoot.kt → AppRoot() 传给 AppRootShell 的那段
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

> 落地前请按当前 navigation3 版本确认 `onBack` 的精确签名: 本项目用的是 `entries:` 重载, `onBack` 是无参 lambda (见上面的调用处);  
> 若版本已改为带 "返回次数" 参数, 按新签名取用.
