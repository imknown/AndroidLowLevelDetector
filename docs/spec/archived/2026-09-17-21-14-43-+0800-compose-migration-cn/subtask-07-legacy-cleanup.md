# ST-07 — legacy-cleanup 修改计划 (补录)

> 状态: record (由目录位置声明). 计划依据: 计划 10 章 "第 7 步 清理收尾" (已删; 幸存版部分内容见抢救件). 风险: 低 (纯删除; 回归面大, 靠回归清单兜底).

## 计划 (当时的拆分)

删除清单 (View 层类与 XML), 依赖瘦身, 回归验证 (9.3 清单), 遗留优化立项 (9.4 清单).

## 实际落地

- a92e163d (09-20): View 系列表资源与类删除 — `MyAdapter` / `MyViewHolder` / `MyItemDecoration` / `BaseListFragment` / `my_view_holder.xml` / `base_list_fragment.xml` 等.
- 64e36091 (09-20): legacy View 依赖与资源删除 — View 层清零达成 (计划 README 2026-09-22 状态块: 无 Fragment / layout XML / AppCompat / androidx.preference).
- 文档收尾: 124f2b15 (dimens.xml 的第 7 步决定), ab8abc6c (step-7 计划文档与实现对齐).

## 落地差异

- **依赖瘦身的后半段晚两天**: 计划把 "去 AppCompat 化" 列为第 9 章的遗留优化, 实际当场做掉 — `appcompat` / MDC 依赖的删除发生在 09-20/21 的主题链重构里 (2aa6f52c, ST-08), 与计划章节的设想顺序不同.
- **观察记录里的旧 bug 随删消亡, 未修即除**: `MyAdapter.onBindViewHolder` 复用 ViewHolder 时 GONE 不恢复 (曾隐藏色点的卡子复用后有色点条目色点消失) — View 世界一行修法始终没排上, 旧列表按计划整层删除; Compose 版 "条件即不组合" 天然无此 bug (观察记录 2026-09-19).
- `StateExt.kt` 计划标 "保留", 实际删除 — `State` 密封接口被双 `StateFlow` 取代 (17bc9227, ST-08); `ToastExt.kt` 确实保留 (计划 01 章章首更正).

## 遗留优化立项 (第 7 步的产出, 2026-09-25 抢救版处置)

8 项遗留优化的处置见 ST-09; 本步的职责是立项本身 — 原清单在计划 10 章 9.4, 随目录删除, 抢救版见 `git show d753a3e8:docs/dev/issues-cn/A-Compose遗留优化与回归清单.md`.

## 证据

- 提交: a92e163d, 64e36091, 124f2b15, ab8abc6c.
- 交叉: `MyModelCard.kt:43` (ripple-only 卡片 — 旧 `MyAdapter` 时代的对位注释仍留在新实现里).
