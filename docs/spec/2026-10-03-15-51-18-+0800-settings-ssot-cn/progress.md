# 进度账本 — 设置 SSOT (#08 + #10, 方案 A)

> 状态: living. 每过一道闸门由 AI 更新; 任何新会话/新 agent/新模型 接续时先读 AGENTS.md 再读本文件, 按 "下一步" 继续. 进行中的步骤 (如后台 review) 不落账, 按账本重跑.

## 当前位置

任务已立项, 尚未动工. 开工前两项裁定已定: 报告语言 = 中文 (-cn); #08 前置问题 (`themeMode` / `scrollBarMode` 伴生流是否终态) = 否, 方案 A 整段实施 (迁进 SettingsStore). 负责人 pre-write 已收 (问题 = 设置页 UDF/SSOT; 思路 = StateFlow; 最大风险 = 生命周期含 Activity 重建), 原样录于 plan.md 顶部. plan gate v1 review: 9 条发现 (1 高 / 2 中 / 6 低) — 负责人裁定全修, 9 处已全部落进 plan.md (播种机制改为 "首次解析 + onCreate 显式首解析点" / ST-01 风险升高, ST-03 降低 / 设置页首帧即存值约定 / 三个列表页勘误 / AGENTS.md 新增锚点 / 链接 / 标点 / 注释归真与枚举去留 / Rule 1-2 行为差论证靶), 自查标点链接零残留. plan.md + 账本已提交. 5 份子任务报告预生成中, 批次 review 随后.

## 子任务状态

(未进入循环 — plan gate 未过)

| 子任务 | 状态 | 闸门进度 |
|---|---|---|
| ST-01 settings-store-skeleton | 未开始 | — |
| ST-02 theme-vertical-slice | 未开始 | — |
| ST-03 scrollbar-vertical-slice | 未开始 | — |
| ST-04 home-and-remaining-switches | 未开始 | — |
| ST-05 docs-closeout | 未开始 | — |

## 挂起物

- module-structure-cn.md 仍保持未跟踪不入库, 5 个待决问题待负责人逐项裁定 (与本任务平行, 非本任务范围; 其待决问题 4 "companion 主题/滚动条状态流随 #08 迁走" 由本任务的方案 A 裁定落实).
- issues-cn 独立小件 (含 #20 唯一活崩溃链) 与主力线后续步骤 (#11 / #09 / 三态 / #03 / #42) 排队在本任务之后.

## 偏差记录

(无)

## 下一步

负责人裁定 9 条 v1 发现 → 修确认项 → 负责人放行 → 提交 plan.md + 一次性预生成 subtask-01..05 报告 + 批次 review → 进入子任务循环.
