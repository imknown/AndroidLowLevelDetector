# View → Jetpack Compose 迁移计划

> 分支 `jetpack-compose-new` · 基线 Kotlin 2.4.20 / Compose BOM 2026.09.00 / AGP 9.4.0
> 调研日期 2026-09-16, 所有 API 结论均核对过 developer.android.com 当日最新文档与 androidx 官方仓库
>
> **状态 (2026-09-22)**: 第 0~7 步**已全部落地**, View 层清空 (无 Fragment / layout XML / AppCompat / androidx.preference). 本目录 (含本页决策表与各章步骤正文) 是**迁移当时的调查与计划**, 按原样保留, 不回写; 与终态的偏离写在每章章首的 "章首更正" 块, 本页则在决策表后附一份汇总.
> **2026-09-25**: 迁移后的 Compose 代码 Review 报告并入 [docs/dev/architecture-review-cn/](../architecture-review-cn/README.md) (项目问题汇总) — Hy4-preview / Qwen3.8-Flash 两份与新增的 GLM-5.3-Flash 轮合并为一份 [A-Compose代码Review报告.md](../architecture-review-cn/A-Compose代码Review报告.md) (编号 F1~F16, 问题发现纳入该目录的统一跟踪: F7 撤回, F13 已修, 其余开放条目见其总览表); 下表的链接指向新位置.

把本项目 (单 Activity + 4 Fragment + RecyclerView/Preference 的 View 界面层)**整体**迁到 Jetpack Compose 的渐进式计划.**迁移是载体, 学会 Compose 是目的** — 整套文档按教材体例编写. 设计目标有三:

1. **每步小且独立** — 每一步都可编译, 可运行, 可单独回退, review 粒度友好;
2. **每步都学到东西** — 主代码块**逐行中文注释**, 知识点配 `白话` (通俗类比), `✅ 正例 / ❌ 反例` (对照与后果), `记忆锚点` (一句话口诀) 三种教学块;
3. **结论可追溯** — 版本, 稳定性, 取舍全部来自 2026-09-16 对官方文档与 androidx 官方仓库的逐条核对.

数据层 (ViewModel / Repository / DataSource) 与 SharedPreferences **一行不动**, 老用户设置全部继承.

## 步骤总览

| 章 | 步骤 | 主题 | 改动规模 | 你会学到 |
| --- | --- | --- | --- | --- |
| [01](01-现状盘点与目标架构.md) | — | 现状盘点与目标架构 | 只读 | 迁什么, 不迁什么, 顺序为什么这样排 |
| [02](02-第0步-构建准备.md) | 第 0 步 | 构建准备 | 1 文件 3 行 | 现有 Compose 依赖盘点, 缺什么为什么 |
| [03](03-第1步-列表卡片组件.md) | 第 1 步 | 列表卡片组件 | 新增 2 + 修改 2 | `Text`/`Card`/`Box`, 自定义主题扩展 (CompositionLocal), `@Preview`,**Style API 试水** |
| [04](04-第2步-PropFragment接入ComposeView.md) | 第 2 步 | PropFragment 接入 ComposeView | 重写 1 + 新增 1 | `ComposeView` 互操作, `collectAsStateWithLifecycle`, `LazyColumn`, `produceState` |
| [05](05-第3步-交互补齐.md) | 第 3 步 | 交互补齐 | 修改 1 + 新增 2 | `PullToRefreshBox`, 自绘滚动条 (绘制阶段读状态的性能套路) |
| [06](06-第4步-Home与Others迁移.md) | 第 4 步 | Home 与 Others 迁移 | 重写 2 + 新增 1 | 组件复用与"包一层"扩展模式, SharedFlow 事件收集 |
| [07](07-第5步-Settings页面重建.md) | 第 5 步 | Settings 页面重建 | 重写 1 + 新增 1 + 修改 1 | 偏好读写三段式, `ListItem`/`Switch`/`AlertDialog` 槽位用法 |
| [08](08-第6步-Navigation3与MainActivity切换.md) | 第 6 步 | Navigation 3 + MainActivity 切换 | 新增 2 + 重写 1 + 改 6 + 删 8 | Nav3 全家 (NavKey/返回栈/entryProvider/装饰器), `Scaffold` 一次性解决 insets |
| [09](09-第7步-清理收尾.md) | 第 7 步 | 清理收尾 | 纯删除 | 删除清单, 依赖瘦身, 回归验证, 遗留优化立项 |
| [观察记录](A-迁移期观察记录.md) | 附录 | 迁移期观察记录 | 查阅 | 与迁移无关的旧代码发现, 有意接受的行为差异 |
| [Compose 代码 Review 报告 (三份合并) ](../architecture-review-cn/A-Compose代码Review报告.md) | 附录 (已迁出) | 迁移后代码复查 | 查阅 | 全部步骤落地后的整体复查: 改错 / 改多 / 漏改 / 不合理, 含直接原因与修改方案. Hy4-preview / Qwen3.8-Flash 两份 2026-09-25 并入 [architecture-review-cn/](../architecture-review-cn/README.md), 与 GLM-5.3-Flash 轮合并为一份 (编号 F1~F16) |

阅读路线 (以**学会**为目的):**01 先读**(迁什么, 不迁什么, 顺序为什么这样排) → **02~09 按步骤跟着做**, 每步做完对照该章"验证清单", 回顾该章 ✅/❌. 章号与文件同名, `01~09` 连续 (`01` 盘点, `02`~`09` 即第 0~7 步); 原有第 02 章 "Compose 核心概念速成" 讲通用概念, 不属本项目改动, 已删除, 需要时从 git 历史取回.

## 关键决策点汇总 (review 从这里开始)

| # | 决策 | 理由 | 备选 |
| --- | --- | --- | --- |
| 1 | 迁移顺序: 先屏幕内容 (ComposeView 嵌在 Fragment 里) → 最后才换导航骨架 | 官方迁移指南的增量路线; 每步可独立回退; 学习曲线从函数/状态起步, 导航留到最后 | 自顶向下先换 Activity(对大改动应用更合适, 本项目不选) |
| 2 | 首个迁移页面选 **Prop** | 三列表页中最简单 (无事件监听); 官方建议从"数据显示相对静态的简单屏幕"起步 | Others(同样简单); 设置页 (官方点名适合起步, 但本项目设置页有对话框/开关/外链/事件, 复杂度更高, 放第 5 步) |
| 3 | 设置页**手写重建**(`LazyColumn` + M3 组件) | 官方至今无 Compose 版 Preference 库 (androidx.preference 停在 2023); 官方参考应用 Now in Android 即手写 | 过渡期 `AndroidFragment` 包着旧页 (多养一层壳, 收益低) |
| 4 | 导航用 **Navigation 3**(1.2.0-rc01) | 官方已把 Nav3 定为 Compose-only 架构推荐; 依赖已在版本目录备好; RC 通道符合你的成熟度政策 | Navigation Compose(Nav2, 功能全但非最新); 继续 Fragment 手动管理 (违背迁移目标) |
| 5 | 底栏用 `NavigationBar` (stable) | 与现状 BottomNavigationView 视觉延续; 不引 alpha 依赖 | `ShortNavigationBar` (1.4.0 已 stable 的 Expressive 版); `NavigationSuiteScaffold` (自适应, 需 1.5.0-alpha, 列为遗留优化) |
| 6 | 滚动条**自绘**(基于 stable 的 `ScrollIndicatorState`)**→ 2026-09-19 实现期改为推迟**: 自绘版已按计划做完并通过评审 (含估算漂移钳制), 但用户拍板不落地 — 等 material3 1.5.0 的 `nonInteractiveScrollbar` (自带淡出) 转正后一行替换, 自绘实现保留在计划文档 05 章 5.2 作参考 | 官方滚动条 UI 在 material3 1.5.0-alpha(不在 BOM); stable 状态 API + 约 30 行自绘即可保留设置项 | 暂时砍掉设置项 (用户可见的功能回退, 不选); 显式引入 1.5.0-alpha 覆盖 BOM(拖整库进 alpha, 不选) → 实际: **设置项保留**, 三档 (无 / 通常 / 可拖拽) 当前全不生效 — 自绘实现未落地, `scrollBarModeChangedSharedFlow` 零订阅者, 等 material3 官方滚动条转正后再接 (负责人 2026-09-22 定, 详见 [R10](../architecture-review-cn/05-已裁定事项.md#R10)) |
| 7 | **Style API 单文件试水**(第 1 步 3.4 节)**→ 2026-09-19 实现期改为推迟**: 文档形态 DSL 只在 foundation alpha 线 (文档示例 1.12.0-alpha03), 1.12.1 stable 反编译实证无此签名, 无法编译 | 你点名要学的新范式; 但 foundation 1.13.0-alpha03 已宣布重构 (旧实现将废弃移除), 且 stable 线连试水形态都不可用 | 全面采用 (1.13 迁移成本高, 不选); 完全不用 (错过学习目标, 不选) → 实际: BOM 升 1.13 后在第 7 步收尾立项 |
| 8 | 主题沿用现有 `AppTheme` (标准 M3 + 动态取色) | Expressive 主题 API(`MaterialExpressiveTheme`/`expressiveLightColorScheme`) 已从 material3 1.4.0 stable 线移除, 仅在 1.5.0-alpha | BOM 升 1.5 后切 Expressive(列为遗留优化) |
| 9 | MainActivity **暂留 AppCompatActivity** | 主题模式四档靠 `AppCompatDelegate.setDefaultNightMode` (只对 AppCompat 生效); 保留 = 该机制零改动 | 换 ComponentActivity + Compose 侧自管 darkTheme(更纯粹但需重构主题链路, 列为遗留优化) |
| 10 | 返回键保持现状 (任何标签直接退出) | 与现有行为一致 | 官方 Nav3 推荐"先回首页再退出" (exit through home), 一行可切换 |
| 11 | ViewModel 工厂在第 6 步小简化 (仓库改在 initializer 内构造) | Fragment 的 extrasProducer 接线随 Fragment 消亡; 简化后任意宿主可用, diff 极小 | 保留 CreationExtras 注入 (在 Nav3 条目下需 extras 合并, 实现时验证成本高) |

> **决策落地差异 (2026-09-22 汇总)** — 上面的决策表是当时的取舍记录, 原样保留; 以下几条后来变了形:
>
> - **决策 6(滚动条自绘)**: 最终**设置项保留, 三档 (无 / 通常 / 可拖拽) 全不生效** — 自绘实现未落地, `scrollBarModeChangedSharedFlow` 零订阅者. 负责人 2026-09-22 定: 保持代码现状, 等 material3 官方滚动条, 不引 alpha, 不自绘 (详见 [R10](../architecture-review-cn/05-已裁定事项.md#R10)).
> - **决策 7(Style API 试水)**: 试水代码从未落地 (1.12.1 stable 无该签名), 改到 1.13 之后从零立项.
> - **决策 9(暂留 AppCompatActivity)**: 已被推翻 — `MainActivity` 现为 `ComponentActivity`, 主题由 `StateFlow` 驱动, `appcompat` / MDC 依赖删除,"跟随省电模式"一档随之退役 (终态三档).
> - **步骤总览 03 / 04 / 05 / 06 的 "你会学到"**: Style API 试水, `produceState` 防闪空, 自绘滚动条, `SharedFlow` 事件收集, 四项均属**当时的教学设想**, 最终没有进入代码.
> - **"数据层与 SharedPreferences 一行不动"**: 迁移期内确实没动; 迁移完成后另有一轮重构改了 `HomeViewModel` (改为观察偏好键, `38492b82`) 与 `BaseListViewModel` (状态拆两条流, `002f25b3`), 见 04 / 06 章首更正与架构体检 AR-02.

## 全局约定

- 代码示例中的 `import` 大多从略 (除特意讲解的 API); `...` 表示与上文相同的省略段.
- "before"一律是仓库当前代码 (`jetpack-compose-new` 分支) 的真实摘录;"after"是可直接落盘的完整代码或 diff.
- 每章末尾有"验证清单" — **每步收尾必须过一遍**再进下一步.
- 各步的已知行为差异都在当章"行为变化/决策点"小节明示, 没有静默变更.

## 调研可信度说明

- 三份外部调研 (Navigation 3 / 迁移指南, 滚动条与 M3 组件, Style API) 于 2026-09-16 通过官方文档与 androidx 官方仓库逐条核对;
- 官方文档中"旧名 API" (如 Nav3 2025 预览期的 `rememberNavEntryProvider`, 已下线的 MDC 主题适配器) 均已确认弃用状态并避开;
- 两处"实现时验证点" (Nav3 条目下 `createSavedStateHandle()` 的 extras 可用性; `PullToRefreshBox` 自定义指示器与 state 的搭配) 已在对应章节就地标注, 属实现期 10 分钟可验证项, 不影响计划结构.
