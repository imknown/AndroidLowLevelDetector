# 进度账本 — 设置 SSOT (#08 + #10, 方案 A)

> 状态: living. 每过一道闸门由 AI 更新; 任何新会话/新 agent/新模型 接续时先读 AGENTS.md 再读本文件, 按 "下一步" 继续. 进行中的步骤 (如后台 review) 不落账, 按账本重跑.

## 当前位置

任务已立项, 尚未动工. 开工前两项裁定已定: 报告语言 = 中文 (-cn); #08 前置问题 (`themeMode` / `scrollBarMode` 伴生流是否终态) = 否, 方案 A 整段实施 (迁进 SettingsStore). 负责人 pre-write 已收 (问题 = 设置页 UDF/SSOT; 思路 = StateFlow; 最大风险 = 生命周期含 Activity 重建), 原样录于 plan.md 顶部. plan gate v1 review: 9 条发现 (1 高 / 2 中 / 6 低) 全修, plan.md + 账本已提交 (faaa8673). 批次 review 循环已收口: v1 7 条 (pass) 全修 → v2 3 条新发现全修 → v3: 3 条全部 resolved, 新发现仅 1 条低 (02 文件伴生样例 :25-28 归真) — 负责人裁定该句现在补进 ST-05 清单 (已落) 并放行批次; 5 份报告已按负责人指示提前入库 (本次提交, 原计划随 ST-01). 当前进入 ST-01 动工对照: AI 已附概念 primer, 等负责人目标句 + 问题单, 说开始才动工.

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

ST-01 动工对照: 负责人给目标句 + 问题单 (可空) → AI 逐条回答问题单 + 附覆盖核对 → 负责人说开始 → 实施 → `./gradlew assembleFossDebug` → 新上下文 subagent 后台 review (v1) → 有发现报裁定, 无新发现等放行 → 提交 (报告五份随此提交入库, 注释译英) → 再等点头进 ST-02.
