# 文档

AndroidLowLevelDetector 的开发者文档。产品介绍和下载入口在[根 README](../README.md)。

## 目录结构

| 路径 | 内容 |
|---|---|
| [spec-cn/](spec-cn/README.md) | 产品规格:应用该做什么、该怎么搭。需求(`FR-x`/`AC`)和架构(`§`/`ADR`)写在同一份文档里,另附一份检测条目清单 |
| [dev/architecture-review-cn/](dev/architecture-review-cn/README.md) | 架构体检报告:列出代码现在的问题(`AR-01`~`AR-20`,以及并进来的 `C`/`R`/`A` 审计条目),附修复路线(2026-09-13 起架构优先,原批次路线图已退役) |
| [jargon-wordlist-cn.md](jargon-wordlist-cn.md) | 黑话词表:容易冒出来的「浮夸词」→ 大白话对照,写文档前后的自查用 |
| [dev/JOTTINGS.md](dev/JOTTINGS.md) | 随手笔记:功能点子、adb 命令片段、参考链接(英文,个人笔记) |

## 阅读顺序

1. **spec-cn** —— 先看这个:应用应该做什么、应该怎么搭。写的是「期望的样子」,现在的代码不一定都做到了。
2. **dev/architecture-review-cn** —— 再看这个:现在的代码和目标差在哪,问题按什么顺序修。
3. **dev/JOTTINGS.md** —— 还没整理过的素材库,想到什么记什么。

## 惯例

- 每条需求、每个问题都有编号(`FR-x`/`AC` 是需求,`ADR-x` 是架构决策,`AR-xx` 和 `C`/`R`/`A` 是体检发现,`Q-x` 是开放问题)。编号在所有文档里通用,互相引用时直接写编号就行。
- 文档尽量写短;太长就按内容天然的分界拆成几个文件,留一个 README 当目录页。
