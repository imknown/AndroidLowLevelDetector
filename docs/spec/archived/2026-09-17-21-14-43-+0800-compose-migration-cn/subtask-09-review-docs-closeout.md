# ST-09 — review-docs-closeout 修改计划 (补录)

> 状态: record (由目录位置声明). 计划依据: 计划 README 2026-09-25 头注 + 09-22 裁定批. 风险: 低 (纯文档).

## 实际落地

### 三份事后 review 合并为 F1~F16

- e98a62ec (09-21): GLM-5.3-Flash 的 Compose 代码 review 报告入库.
- 74a58b5a (09-22): Hy4-preview 轮作附录追加.
- 1bd04ff5 (09-25): 与 Qwen3.8-Flash 轮合并为一份 F1~F16, 并入 issues-cn (当时名 architecture-review-cn, 6eac2728 后改名), 开放条目纳入该目录统一跟踪; F7 撤回 (a3a7d249), F13 已修, 其余开放 (计划 README 09-25 头注).
- review 对象是重写前的 `jetpack-compose-new` 谱系 (复查基线 a87f80e1 / cb4104aa); 结论已在当前代码上逐条重新取证 (issues-cn README 基线块) — 引用 F 条目时以现行 tracker 为准, 不回溯旧附录.

### 计划文档的落地归真与删除

- 09-19/20: 各步实现期更正入库 — b16ed770 (step-1), dfe23c62 (step-2), 18eb67df (step-3 滚动条推迟), 8461d8b2 (typography), 8735f941 (step-5), 552a41e0 (step-6), 124f2b15 + ab8abc6c (step-7).
- 09-22: 负责人裁定批量落账 (dd34996f / 736a6028 / 7ed492f7 / bb8132a7 / ac54234c 等 — 恢复调查原文 + 章首更正制 + 决策表后附落地差异), 另有数字重测 (0468f411) 与 file:line 引用重指 (d32e579d, 后 09-23 起全面改按符号引用).
- 09-24: 教学章节裁撤 (7843a6fc, "02~09 步正文与决策表是历史, 可从 git 取回"), 章号收缩回 01~09 (58e469b7).
- 09-25: 整目录删除 (9c91c8cc); 可抢救件移入 issues-cn (d753a3e8): 迁移期观察记录 + 11 项回归清单 + 8 项遗留优化处置 (3 活 / 1 完 / 4 吸收); N 系列全仓扫查发现另行入册 (9022da55).

### 遗留优化 8 项的处置 (2026-09-25 抢救版)

1. 滚动条换代 — 活 → ST-10 落替身, 官方组件到位后从零接入 (现 [#37](../../../dev/issues-cn/7-UI与无障碍/37-滚动条可拖动是stub.md)).
2. 主题模式去 AppCompat 化 — 完 (ST-08).
3. Expressive 主题 — 活 (`Type.kt:18` "Re-tune when Material 3 Expressive becomes available" 仍在等).
4. Style API 跟进 — 活 (foundation 1.13 重构后从零立项).
5. 设置存储现代化 — 吸收进 AR-02 → 后成 issues-cn #08/#10 → settings-ssot 任务落地 (兄弟 spec).
6. 自适应导航 — 吸收进 R2 (现 [#39](../../../dev/issues-cn/7-UI与无障碍/39-无自适应布局.md)).
7. 底栏随滚动隐藏 — 吸收进 R1 / FR-12 (现 [#38](../../../dev/issues-cn/7-UI与无障碍/38-工具栏不随滚动隐藏.md)).
8. 测试基建 — 吸收进 AR-16 / A6 (现 [#06](../../../dev/issues-cn/1-架构与分层/06-技术栈缺口.md) + [#50](../../../dev/issues-cn/9-构建与CI/50-CI只编译不测试.md)).

另: 数据加载惰性化 (`WhileSubscribed(5_000)`, 接网络/DB 时重造) 独立一项, 见 ST-02 遗留节. **2026-10-04 现状注**: Expressive / Style API / WhileSubscribed 三项在现行 issues-cn (按性质重建版) grep 零命中 — tracker 多轮重建后无编号居所, 是否补立项由负责人定.

## 证据

- 提交: 上列各笔; 全文: `git show d753a3e8:docs/dev/issues-cn/A-Compose遗留优化与回归清单.md`, `git show 9c91c8cc^:docs/dev/compose-migration-plan-cn/A-迁移期观察记录.md`.
