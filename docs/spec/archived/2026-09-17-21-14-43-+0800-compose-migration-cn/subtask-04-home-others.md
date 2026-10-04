# ST-04 — home-others 修改计划 (补录)

> 状态: record (由目录位置声明). 计划依据: 计划 07 章 "第 4 步 Home 与 Others 迁移" (已删). 风险: 低 (复用 ST-02 已定型的共用屏).

## 计划 (当时的拆分)

Home 与 Others 换壳; `home/HomeScreen.kt` 包一层共用列表并收集 Home 的两个 `SharedFlow` 事件 (滚动条模式, 过期应用排序) — "包一层" 扩展模式与 SharedFlow 事件收集是本章教学点.

## 实际落地

- 78566930: HomeFragment 与 OthersFragment → ComposeView, 复用 `MyModelListScreen`.

## 落地差异

- **`home/HomeScreen.kt` 做出后撤销** (计划 01 章章首更正): Home 没有页面特有逻辑, 直接用共用 `MyModelListScreen`, 由 `AppRoot()` 的 `entryProvider(Home entry)` 装配 — 目标树里的该文件不存在.
- **组合内收集 `SharedFlow` 的教学设想未进入代码**: 过期排序事件的收集最终落在 ViewModel (befc120e, 09-21, 属 ST-08 的状态重构), 不在组合内; 09-19 到 09-21 之间的过渡形态无存档, `unknown`.
- 6d4d76d8 (includeFontPadding 恢复) 落在本步提交区间, 更正记录挂在 step-1 章 (8461d8b2) — 见 ST-01.

## 证据

- 提交: 78566930; 撤销认定: 计划 01 章章首更正 (`git show 9c91c8cc^:docs/dev/compose-migration-plan-cn/01-现状盘点与目标架构.md`).
- 现状反证: `ui/home/` 下无 `HomeScreen.kt` (列表装配在 `ui/AppRoot.kt`).
