<a id="F10"></a>

# F10 · `@Stable` 标在了不需要的地方, 且它是一份长期承诺 (P3, 改多)

> 返回 [README 索引](../README.md) · [组1 · 随时可做 — 独立小修](../README.md#组1--随时可做--独立小修).


> 发现: Hy4-preview (F10). 2026-09-25 复核: **仍开放** (冗余结论在本轮得到再次确认: 组合里 `HomeViewModel` 的声明类型始终是基类).

**现象**: `BaseListViewModel` / `HomeViewModel` / `SettingsViewModel` 都标了 `@Stable`, `OthersViewModel` / `PropViewModel` 没标.

**直接原因**: 逐个提交做的 "稳定性优化" (提交 `494b8f28` / `05f7311d` / `42eb3e95`), 逐个类补注解.

**根本原因**: 没弄清稳定性由**声明类型**决定. 三个列表页调用的是 `MyModelListScreen(viewModel: BaseListViewModel)` — 参数声明类型就是基类, `@Stable` 标在基类上已经足够; `HomeViewModel` 上那份是**冗余的** (`SettingsViewModel` 那份有效, 因为 `SettingsScreen` 的参数声明类型就是它).

**风险提示**: `@Stable` 是 "所有公开属性永不静默变化" 的承诺. 目前各 VM 的可变字段都是 `private` (`loadJob`, `timesLeft`, `loadStartGeneration`), 承诺成立; 但一旦有人往 VM 上加一个 `var`, Compose 会因为 "类型稳定" 跳过重组, 产生极难排查的状态不同步.

**修改方案**: 保留 `BaseListViewModel` / `SettingsViewModel` 上的注解, 删掉 `HomeViewModel` 上冗余的那份; 把注释里的理由从 "实例不变" 改成 "公开属性都为 StateFlow 或 private var, 满足 @Stable 契约", 避免后人误读为 "给 ViewModel 加 @Stable 总是安全的".
