<a id="i69"></a>

# 69 Others 失败行渲染红色圆点 (需求: 圆点只属 Home)

> 返回 [README 索引](../README.md) · [7 · UI 与无障碍](../README.md#7--ui-与无障碍).

**严重程度: P3 | 修复难度: 低**

**需求**: 红黄绿圆点是 Home 每个 item 的专属; Others / Prop / Settings 不渲染圆点.

**现状**: `MyModelCard` 对 `color != StatusColor.NONE` 渲染圆点. Others 的 30+ 处 `guardedMyModel(R.string.xxx)` 失败行走 `toErrorMyModel()` (CRITICAL) — 该页一旦有检测项失败, 就会渲染出需求之外的红点. Prop 已随止血批转无色 (失败行 = 无色源级/逐项错误行, 见 [#20](../archived/5-稳定性与错误处理/20-列表key重复崩溃.md) 的落法); Settings 不使用 `MyModel`, 构造上无圆点.

**修改方向**: 同 Prop 的落法 — 失败行走无色 helper (标题沿用条目标题资源, 详情 = "检测失败" 文案), `StatusColor` 在 `OthersRepository` 维持零出现. 需求重申见 [#33](33-Unknown渲染红色.md), 失败呈现大方向见 [#63](../05-稳定性与错误处理/63-失败折成结论.md).
