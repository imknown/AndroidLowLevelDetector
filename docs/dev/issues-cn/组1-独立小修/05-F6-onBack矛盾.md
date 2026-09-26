<a id="F6"></a>

# F6 · `onBack` 与多返回栈自相矛盾 (P2, 不合理)

> 返回 [README 索引](../README.md) · [组1 · 随时可做 — 独立小修](../README.md#组1--随时可做--独立小修).


> 发现: Hy4-preview (F6). 2026-09-25 复核: **仍开放**.

**现象**: 每个标签各自持有一条 `NavBackStack`, 但返回键一律 `activity?.finish()`.

**直接原因**: 决策 10 明确 "保持旧行为 (任何标签直接退出)", 旧版 Fragment show/hide 确实如此.

**根本原因**: 迁移引入了 "多返回栈" 这一新能力, 却没有同步更新返回语义. 当前每个栈恒为 1 个 entry, 行为等价; 但只要任一标签 push 第二个页面 (详情, WebView 等), 返回键就会变成 "直接退出 App" — 这是典型的 "改了一半".

**问题代码**:

```kotlin
// AppRoot.kt → AppRoot() 调 AppRootShell 的那段
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

> 落地前请按当前 navigation3 版本确认 `onBack` 的精确签名 (本项目用的是 `entries:` 重载, `onBack` 为无参 lambda; 若版本已改为带 "返回次数" 参数, 按新签名取用).


