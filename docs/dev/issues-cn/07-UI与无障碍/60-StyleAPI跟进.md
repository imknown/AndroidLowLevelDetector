<a id="i60"></a>

# 60 · Style API 跟进

> 返回 [README 索引](../README.md) · [7 · UI 与无障碍](../README.md#7--ui-与无障碍).

**严重程度: P3 | 修复难度: — (未立项)**

**结论**: 不是缺陷 — 全仓零 Style API 使用; Compose 迁移决策 7 原计划的 "单文件试水" 从未落地 (stable foundation 1.12.1 反编译实证无文档所示签名, 无法编译). 本条登记一个**跟 foundation 版本走的样式写法升级候选项**, 随 foundation 1.13 落地实施.

**证据**:

- 迁移实现期实证 (2026-09-19, 记录于 step-1 更正 b16ed770): 文档形态的 Style API 只在 foundation alpha 线 (文档示例 1.12.0-alpha03), 1.12.1 stable 反编译无该签名; 且 foundation 1.13.0-alpha03 已宣布重构, 旧实现将废弃移除 — 试水代码即使硬写也会立刻报废.
- 当前依赖 pin: `gradle/toml/android.toml` → `compose-bom = "2026.09.00"`.
- 出处: Compose 迁移遗留优化, 2026-09-25 整理旧 tracker 时的抢救记录把它记为 "活" 项 ([归档 spec](../../../spec/archived/2026-09-17-21-14-43-+0800-compose-migration-cn/plan.md) 决策 7 与落地差异). 它曾作为附录收入当时的 tracker; 现行 tracker 按问题性质重建后已无对应条目 (2026-10-04 全目录 grep 零命中), 本条补回并把出处锚定到归档 spec.

**处理**: 保持现状. foundation 1.13 进 BOM 后, 按迁移期同款调研方法 (当日官方文档 + androidx 仓库逐条核对) 重新调研并从零立项 — 不背旧试水设计的包袱; material3 组件后续开放 `style` 参数时再进一步收纳. 立项前不预写任何代码.
