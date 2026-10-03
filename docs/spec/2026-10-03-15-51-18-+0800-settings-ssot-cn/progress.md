# 进度账本 — 设置 SSOT (#08 + #10, 方案 A)

> 状态: living. 每过一道闸门由 AI 更新; 任何新会话/新 agent/新模型 接续时先读 AGENTS.md 再读本文件, 按 "下一步" 继续. 进行中的步骤 (如后台 review) 不落账, 按账本重跑.

## 当前位置

任务已立项, 尚未动工. 开工前两项裁定已定: 报告语言 = 中文 (-cn); #08 前置问题 (`themeMode` / `scrollBarMode` 伴生流是否终态) = 否, 方案 A 整段实施 (迁进 SettingsStore). 负责人 pre-write 已收 (问题 = 设置页 UDF/SSOT; 思路 = StateFlow; 最大风险 = 生命周期含 Activity 重建), 原样录于 plan.md 顶部. plan gate v1 review: 9 条发现 (1 高 / 2 中 / 6 低) 全修, plan.md + 账本已提交 (faaa8673). 批次 review 循环已收口: v1 7 条 (pass) 全修 → v2 3 条新发现全修 → v3: 3 条全部 resolved, 新发现仅 1 条低 (02 文件伴生样例 :25-28 归真) — 负责人裁定该句现在补进 ST-05 清单 (已落) 并放行批次; 5 份报告已按负责人指示提前入库 (本次提交, 原计划随 ST-01). 当前进入 ST-01 动工对照: AI 已附概念 primer, 等负责人目标句 + 问题单, 说开始才动工.

## 子任务状态

(未进入循环 — plan gate 未过)

| 子任务 | 状态 | 闸门进度 |
|---|---|---|
| ST-01 settings-store-skeleton | 已提交 (`feat(settings)` f54e54d0) | 全部 ✓ (目标句代记 "按报告范围" / 报告无修订 / 实施 / 构建绿 / v1 review "通过" + 2 条低发现按裁定修复 (xxxStored 命名归一) / 对照问答四问 (StateFlow 必要性, keys 不 lazy, 双流不合并, 对话框枚举化立案为 documented non-goal 记入 ST-02 报告) / 放行 / 注释译英 + 标点零违规 + 构建重验 EXIT=0 / 提交) |
| ST-02 theme-vertical-slice | 进行中 | 对照 ✓ 实施 ✓ 构建绿 ✓ (两处编译波折已修, 见上) ✓ v1 review ✓ (结论 "pass"; 承重论断全核实: 主题链逐行等价 / 组合前时序无泄漏窗口 / Activity 重建语义正确 / Metro 合法 / 范围零溢出 / @Suppress 实证 — RETURN_VALUE_NOT_USED 为 IDE 侧诊断, CLI 2.4.20 本不发, 压制对未来升级免疫; 3 条低: ① 滚动条伴生注释 "(no page reads SharedPreferences itself)" 与存续期直读并存的措辞矛盾 ② LocalThemeMode 静默 FollowSystem 默认 = 已接受权衡留档 ③ 提交勿 add -A 误扫 module-structure-cn.md) → 发现已报负责人裁定 ✓ (① 微调已落: "no page reads the mode itself from SharedPreferences" — 模式走流为真, 原始存值对话框直读是另一回事; ② 留档无动作; ③ 提交时只收代码文件 + 台账) → 负责人真机冒烟全过 ✓ → 放行 ✓ → 提交闸门 (注释译英回填 + 标点零违规 + 构建重验 + 提交) |
| ST-03 scrollbar-vertical-slice | 未开始 | — |
| ST-04 home-and-remaining-switches | 未开始 | — |
| ST-05 docs-closeout | 未开始 | — |

## 挂起物

- module-structure-cn.md 仍保持未跟踪不入库, 5 个待决问题待负责人逐项裁定 (与本任务平行, 非本任务范围; 其待决问题 4 "companion 主题/滚动条状态流随 #08 迁走" 由本任务的方案 A 裁定落实).
- issues-cn 独立小件 (含 #20 唯一活崩溃链) 与主力线后续步骤 (#11 / #09 / 三态 / #03 / #42) 排队在本任务之后.

## 偏差记录

- ST-02 / step0 / AI 把负责人一句 "ST-02 开始" 误解为动工放行, 未发 primer 即改了五个源文件 (构建一度红) — 负责人纠正 "开始 = 开始给我概念 primer"; 已 `git restore` 回退全部未提交改动到 ST-01 基线 (9cedd99f), 改为按流程发 primer 等对照. 检查清单新增: 闸门语义以负责人的实际意图为准, "开始" 在未发 primer 前一律理解为 "开始对照", 不理解成 "开始实施".
- ST-02 / step0 / primer 发出后负责人一句 "开始" 动工, 未另给目标句与问题单 — 按 subtask-02 报告范围执行; 台账代记目标句为 "按报告范围".
- ST-02 / 实施期 / AI 重做时把新增注释直接写成了英语, 跳过 "实现期中文" 中间态 — 负责人指出后已按工作流改回中文注释, 提交闸门整体译英. 检查清单新增: 实施期新注释一律先写中文, 译英只发生在提交闸门.
- ST-01 / step0 / 负责人未单独给出目标句与问题单, 以一句 "开始" 放行 — 按 subtask-01 报告范围执行; 台账代记目标句为 "按报告范围".
- ST-01 / 提交 / 5 份子任务报告按负责人指示提前入库 (3f5efa8f), 原计划随 ST-01 提交 — 后续子任务提交不再携带报告.

## 下一步

ST-01 动工对照: 负责人给目标句 + 问题单 (可空) → AI 逐条回答问题单 + 附覆盖核对 → 负责人说开始 → 实施 → `./gradlew assembleFossDebug` → 新上下文 subagent 后台 review (v1) → 有发现报裁定, 无新发现等放行 → 提交 (报告五份随此提交入库, 注释译英) → 再等点头进 ST-02.
