# 开发者文档

`docs/dev/` 的索引. 每个目录是什么, 以及是否还在持续回写 — 文档写作规则 (就地修正, ID 体系, 语言) 在 [../README-cn.md](../README-cn.md).

| 路径 | 是什么 | 状态 |
|---|---|---|
| [conventions/](conventions/README-cn.md) | 工程约定与事实 — `AGENTS.md` 背后的详细 memory: flavor, 签名, 工具链, 架构, 代码规则, 本地化, 工作流 | **living** — 描述现状; 漂移是要就地修的 bug |
| [architecture-review-cn/](architecture-review-cn/README.md) | View 时代架构的体检; 它的发现 (`AR-xx`, `C`/`R`/`A`) 和从中切出的快赢修复 (`QW-x`) 仍在喂 backlog | **record + backlog** — 依条目行动前先对当前代码复核; View 时代的描述是历史, 不要把今天的代码回填进去 |
| [compose-migration-plan-cn/](compose-migration-plan-cn/README.md) | 本分支背后的 View → Compose 迁移, 写成教学式走读 (分步计划, 前后代码, API 说明) — 动 UI 之前先读它的关键决策 | **frozen** — 第 0~7 步已全部落地; 它的文本和各章章首的更正块原样保留 |
| [JOTTINGS.md](JOTTINGS.md) | 未整理的个人随笔 | 草稿 |

**frozen** 文档在自己的 README 里保留自己的声明; 代码和 [conventions/](conventions/README-cn.md) 是关于现状的真相. 要在这里加目录, 加一行并说明它属于三种状态中的哪一种.

## 阅读顺序

1. [conventions/](conventions/README-cn.md) — 这个仓库怎么构建, 怎么改动
2. [architecture-review-cn/](architecture-review-cn/README.md) — 代码与目标之间的差距, 以及从中切出的快赢修复 (`QW-x`, 现暂缓)
3. [compose-migration-plan-cn/](compose-migration-plan-cn/README.md) — 只在本分支动 UI 之前读; 从它的关键决策开始
4. [JOTTINGS.md](JOTTINGS.md) — 未整理的材料
