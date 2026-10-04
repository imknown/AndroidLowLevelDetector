<a id="i47"></a>

# 47 · `@Stable` 标在了不需要的地方, 且它是一份长期承诺 (P3)

> 返回 [README 索引](../README.md) · [8 · 反模式与卫生](../README.md#8--反模式与卫生).

**严重程度: P3 | 修复难度: 低**
**影响文件: `BaseListViewModel.kt`, `HomeViewModel.kt`, `SettingsViewModel.kt`**

**现象**: `BaseListViewModel` / `HomeViewModel` / `SettingsViewModel` 都标了 `@Stable`, `OthersViewModel` / `PropViewModel` 没标.

**直接原因**: 逐个提交做的 "稳定性优化" (`494b8f28` 标 `BaseListViewModel`, `05f7311d` 标 `HomeViewModel`, `42eb3e95` 标 `SettingsViewModel`), 逐个类补注解.

**根本原因**: 没弄清稳定性由**声明类型**决定. 三个列表页 (`AppRoot()` 的 `entry<HomeKey>`, `entry<OthersKey>`, `entry<PropKey>`) 都把具体 ViewModel 传给 `MyModelListScreen(viewModel: BaseListViewModel)` — 参数声明类型就是基类, `@Stable` 标在基类上已经足够, `HomeViewModel` 上那份是**冗余的** (`SettingsViewModel` 那份有效, 因为 `SettingsScreen(viewModel: SettingsViewModel)` 的参数声明类型就是它).

**风险提示**: `@Stable` 是 "所有公开属性永不静默变化" 的承诺. 目前各 ViewModel 的可变字段都是 `private` (`BaseListViewModel.loadJob`, `HomeViewModel.loadStartGeneration`, `SettingsViewModel.initBuiltInDataVersionJob` 与 `SettingsViewModel.timesLeft`), 承诺成立; 但一旦有人往 ViewModel 上加一个公开 `var`, Compose 会因为 "类型稳定" 跳过重组, 产生极难排查的状态不同步. 这是对未来改动的约束, 不是当下的缺陷.

**与 AGENTS.md 的张力**: AGENTS.md 把 `@Immutable` / `@Stable` 记为**有意的**约定, 并要求每当状态类变化时重评一次; 三个类上方的注释就是那条约定要求的 "为什么这个注解安全" 的说明. 所以删掉一处注解是负责人的取舍决定, 不是可以顺手做的清理.

**修改方案**: 若采纳 — 保留 `BaseListViewModel` / `SettingsViewModel` 上的注解, 删掉 `HomeViewModel` 上冗余的那份; 把注释里的理由从 "实例不变" 改成 "公开属性都是 StateFlow 或 private var, 满足 `@Stable` 契约", 避免后人误读为 "给 ViewModel 加 `@Stable` 总是安全的"; 注释改写后仍要说清 "为什么安全", 不能只剩结论.
