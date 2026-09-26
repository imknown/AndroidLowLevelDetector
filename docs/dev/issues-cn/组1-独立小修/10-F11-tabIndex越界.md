<a id="F11"></a>

# F11 · 每个标签都注册了全部 4 个 entry;`currentTabIndex` 无越界保护 (P3, 隐患)

> 返回 [README 索引](../README.md) · [组1 · 随时可做 — 独立小修](../README.md#组1--随时可做--独立小修).


> 发现: Hy4-preview (F11). 2026-09-25 复核: **仍开放** (`decoratedEntries[currentTabIndex]` 仍无 `coerceIn`).

**现象**: `topLevelTabs.map { rememberDecoratedNavEntries(...) }` 里, 4 个标签各自的 `entryProvider` 都包含全部 4 个 entry (实际只会用到 1 个); `decoratedEntries[currentTabIndex]` 直接下标取值.

**直接原因**: 多返回栈配方 (nav3-recipes) 的模板写法, 为了保持 `remember` 调用顺序稳定而统一构造.

**根本原因**: 模板照搬后没有按本项目的实际形态收敛. 风险点: `currentTabIndex` 是 `rememberSaveable` 持久化的, 将来若减少标签数量, 老用户升级后读到的旧索引会越界 → `IndexOutOfBoundsException` 冷启动崩溃.

**修改方案**:

```kotlin
val index = currentTabIndex.coerceIn(0, topLevelTabs.lastIndex)
...
entries = decoratedEntries[index]
```

entryProvider 的冗余可以不改 (改动反而会破坏 `remember` 的稳定性), 但建议加一行注释说明 "四个 provider 内容相同是有意为之".


