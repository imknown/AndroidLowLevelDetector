<a id="i26"></a>

# 26 每个标签都注册了全部 4 个 entry;`currentTabIndex` 无越界保护

> 返回 [README 索引](../README.md) · [5 · 稳定性与错误处理](../README.md#5--稳定性与错误处理).

**严重程度: P3 | 修复难度: 低**


**现象**: `AppRoot.kt` 里 `topLevelTabs.map { tab -> rememberDecoratedNavEntries(...) }` 为 4 个标签各建一套 decorated  
entries, 而每次迭代用的 `entryProvider { ... }` 都把 `HomeKey` / `OthersKey` / `PropKey` / `SettingsKey` 四个 key 全注册一遍  
(每条返回栈实际只会用到自己的那一个). 渲染处 `NavDisplay(entries = decoratedEntries[currentTabIndex], ...)` 直接下标取值.

**直接原因**: 多返回栈配方 (nav3-recipes) 的模板写法, 为了保持 `remember` 调用顺序稳定而统一构造.

**根本原因**: 模板照搬后没有按本项目的实际形态收敛. 风险点在 `currentTabIndex` 的来源:  
`var currentTabIndex by rememberSaveable { mutableIntStateOf(0) }`, 它被持久化, 读回来时没有任何校验,  
取 `decoratedEntries` 时也没有 `coerceIn`.

崩溃条件是 **将来的标签数变少**: 现在 `topLevelTabs` 是 4 条, `decoratedEntries` 也是 4 条, 老索引永远在界内. 一旦删掉某个顶层标签, 老用户升级后  
`rememberSaveable` 读到的旧索引可能是 3 → `IndexOutOfBoundsException`, 而且是冷启动第一帧就崩, 用户只能清数据或卸载重装. 因为依赖 "以后改列表" 这个前提,  
定级 P3.

**修改方案**:

```kotlin
val index = currentTabIndex.coerceIn(0, topLevelTabs.lastIndex)
...
entries = decoratedEntries[index],
```

entryProvider 的冗余可以不改 (改动反而会破坏 `remember` 的稳定性), 但建议加一行注释说明 "四个 provider 内容相同是有意为之".
