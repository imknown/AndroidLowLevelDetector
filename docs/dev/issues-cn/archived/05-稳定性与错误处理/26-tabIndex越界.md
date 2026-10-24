<a id="i26"></a>

# 26 每个标签都注册了全部 4 个 entry;`currentTabIndex` 无越界保护

> 返回 [README 索引](../../README.md) · [5 · 稳定性与错误处理](../../README.md#5--稳定性与错误处理).

**严重程度: P3 | 修复难度: 低 | 状态: 已修复 (clamp 一次, 渲染与选中态共用)**


**现象**: `AppRoot.kt` 里 `topLevelTabs.map { tab -> rememberDecoratedNavEntries(...) }` 为 4 个标签各建一套 decorated  
entries, 而每次迭代用的 `entryProvider { ... }` 都把 `HomeKey` / `OthersKey` / `PropKey` / `SettingsKey` 四个 key 全注册一遍  
(每条返回栈实际只会用到自己的那一个). 渲染处 `NavDisplay(entries = decoratedEntries[currentTabIndex], ...)` 直接下标取值.

**直接原因**: 多返回栈配方 (nav3-recipes) 的模板写法, 为每个标签统一构造一套 decorated entries.

**根本原因**: 模板照搬后没有按本项目的实际形态收紧. 风险点在 `currentTabIndex` 的来源:  
`var currentTabIndex by rememberSaveable { mutableIntStateOf(0) }`, 它被持久化, 读回来时没有任何校验,  
取 `decoratedEntries` 时也没有 `coerceIn`.

崩溃条件是 **将来的标签数变少**: 现在 `topLevelTabs` 是 4 条, `decoratedEntries` 也是 4 条,  
老索引永远在界内. 一旦删掉某个顶层标签, 老用户升级后  
`rememberSaveable` 读到的旧索引可能是 3 → `IndexOutOfBoundsException`, 而且是冷启动第一帧就崩,  
用户只能清数据或卸载重装. 因为依赖 "以后改列表" 这个前提,  
定级 P3.

**怎么修的**: `currentTabIndex` 在状态声明后 clamp 一次  
(`val selectedTabIndex = currentTabIndex.coerceIn(0, topLevelTabs.lastIndex)`),  
渲染 (`NavDisplay` 的 `entries`) 与选中态 (`AppRootShell` 的 `currentTabIndex` 参数) 两个消费者都改用  
clamp 后的值; 越界时界面自洽 (落回第 0 个标签且它显示选中), 下一次点击即写入界内值.  
四个 `entryProvider` 内容相同保持不动, 已留注释说明  
(每条返回栈只会查自己的 key, 其余三个注册不会被解析到).

- 条目原方案只在 `NavDisplay` 渲染处 coerce, 会留下一个没解决的问题: 越界时 "没有任何 tab 显示选中", 未采纳.
- 全仓确认 `currentTabIndex` 只有这两处消费者 (函数局部状态, 不逃逸出 `AppRoot`).
- 空列表会让 `coerceIn(0, -1)` 抛 `IllegalArgumentException`, 已议定不处理:  
  `topLevelTabs` 是硬编码 4 元素常量, 需求上不可能为空.
- 验证: `assembleFossDebug` / `testFossDebugUnitTest` / `lintFossDebug` 全部通过;  
  analyze-file 因无开着本项目的 Android Studio 实例, 负责人决定跳过.
