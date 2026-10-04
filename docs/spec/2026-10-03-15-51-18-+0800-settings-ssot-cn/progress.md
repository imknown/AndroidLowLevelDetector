# 进度账本 — 设置 SSOT (#08 + #10, 方案 A)

> 状态: living. 每过一道闸门由 AI 更新; 任何新会话/新 agent/新模型 接续时先读 AGENTS.md 再读本文件, 按 "下一步" 继续. 进行中的步骤 (如后台 review) 不落账, 按账本重跑.

## 当前位置

**任务全部完成 (5/5; ST-05 以细颗粒四笔提交).** 设置数据收敛到单一可观察归属 `SettingsStore` (issues-cn #08 全量方案 A + #10): 代码四刀 f54e54d0 / c6a05302 / 07870cce / 0f8b64ba, 文档收口四笔 34c0e880 / aef435ba / c4f0c30b / 本 commit. `MyApplication` 伴生对象只剩 `instance` / `sharedPreferences` / `getMyString` (#02 剩余收口范围); 静态设置流与全部 SP 直读/直写清零. 每刀均过: 构建 + 新上下文 subagent review (ST-05 经 v1-v4 四轮收敛至 pass) + 负责人真机冒烟. 负责人 pre-write 三句全部兑现 (UDF/SSOT 达成 / StateFlow 思路即最终形态 / 生命周期风险经构造期同步播种 + onCreate 钉首解析点 + 真机验证正面命中). Retrospective 五类问题按裁定口头留档不入库. 后续工作归 issues-cn 修复路线 (下一步 = 新第 1 步 #11 Home 纯推导) 与模块化任务 (module-structure-cn.md 待决问题 4 已就地更新, 文件仍不入库).

## 子任务状态 (终表)

| 子任务 | 状态 | 闸门进度 |
|---|---|---|
| ST-01 settings-store-skeleton | 已提交 (`feat(settings)` f54e54d0) | 全部 ✓ (v1 review "通过", 2 条低按裁定修复: xxxStored 命名归一) |
| ST-02 theme-vertical-slice | 已提交 (`refactor(settings)` c6a05302) | 全部 ✓ (v1 review "pass", ① 措辞微调 ② 静默默认留档 ③ 提交防误扫; 负责人真机冒烟全过) |
| ST-03 scrollbar-vertical-slice | 已提交 (`refactor(settings)` 07870cce) | 全部 ✓ (v1 review "pass" — AOSP 实证同帧回调; 3 条低无需改码; 负责人真机冒烟通过) |
| ST-04 home-and-remaining-switches | 已提交 (`refactor(settings)` 0f8b64ba) | 全部 ✓ (v1 review "pass" — 三时序走查, 最坏分支不可达; 4 条低无需改码; 负责人真机冒烟通过) |
| ST-05 docs-closeout | v2 返回, 待负责人裁定 | v1 ("needs-fixes") → 负责人裁定 (修 ①-⑤; ⑥⑦⑧ 同修但等 review 完再定提交; 分两笔是届时形状) — AI 第二次误解抢跑两笔提交, 撤回 (见偏差记录) ✓ v2 复审 ✓ (结论 "needs-fixes": **①-⑧ 八项全部 resolved, 无修复引入的新问题**; 但清出 v1 枚举外的同类残留 — ST-04 的 HomeViewModel 接线改动未传导到 tracker 正文: **中** = 28-行为细节杂项:10 以现在时描述已删监听器接线, **低** ×2 = 12-拖拽重建:15 同类 + 02:13 DI 清单句与 :23 新改写有内部张力; 收敛项 = 台账新旧双表并存 / AGENTS-cn 少一 "直接" / #18 "键解析" 措辞 / 02:30 未列 contentResolver) → 负责人裁定: 全修 + v3 ✓ (同类残留已归真 28/12/02:13; 收敛项已收拢: 双表并一 / AGENTS-cn 补 "直接" / #18 改 "解析" / 02:30 补 contentResolver) → v3 终检 ✓ (唯一新发现 = 台账 "下一步" 节收尾残留, 已照处方改写) ✓ v4 终检 ✓ (**"Verdict: pass", 无新发现**; v3 修复 resolved; 全量清扫通过 — 悬空 / tracker 规则 / 计数 99/69 独立重算 / 过去时溯源 / AGENTS 双语 / 标点 / 两笔结构描述一致) → 待负责人本人 review 通过 ✓ 明确提交指令 ✓ → 细颗粒四笔: issues-cn 收口 (`docs(issues-cn)` 34c0e880) / AGENTS 双语 (`docs(agents)` aef435ba) / 代码注释归真 (`docs(settings)` c4f0c30b) / spec 收尾 (本 commit) |

## 挂起物

- module-structure-cn.md 仍保持未跟踪不入库, 5 个待决问题待负责人逐项裁定 (与本任务平行, 非本任务范围; 其待决问题 4 "companion 主题/滚动条状态流随 #08 迁走" 由本任务的方案 A 裁定落实).
- issues-cn 独立小件 (含 #20 唯一活崩溃链) 与主力线后续步骤 (#11 / #09 / 三态 / #03 / #42) 排队在本任务之后.

## 偏差记录

- ST-02 / step0 / AI 把负责人一句 "ST-02 开始" 误解为动工放行, 未发 primer 即改了五个源文件 (构建一度红) — 负责人纠正 "开始 = 开始给我概念 primer"; 已 `git restore` 回退全部未提交改动到 ST-01 基线 (9cedd99f), 改为按流程发 primer 等对照. 检查清单新增: 闸门语义以负责人的实际意图为准, "开始" 在未发 primer 前一律理解为 "开始对照", 不理解成 "开始实施".
- ST-02 / step0 / primer 发出后负责人一句 "开始" 动工, 未另给目标句与问题单 — 按 subtask-02 报告范围执行; 台账代记目标句为 "按报告范围".
- ST-03 / step0 / primer 发出后负责人一句 "继续" 动工, 未另给目标句与问题单 — 按 subtask-03 报告范围执行; 台账代记目标句为 "按报告范围".
- ST-05 / 提交闸门 (第二次) / 负责人的 "⑥⑦⑧ 也修但另起一个 commit, 然后 review v2" 被 AI 理解为 "修复后立即按两笔提交, 再跑 v2" — 实际含义是 "**等负责人本人与后台 v2 都 review 完**, 再决定后续修改还是提交; 分两笔只是届时提交的形状". 两笔提交 (2fe05deb / c3610eec) 均已 `git reset` 撤回, 改动保留在工作区, v2 复审不受影响 (审的是工作区). 检查清单升级: **任何提交都需要三重前置 — subagent review 已返回并报告 + 负责人本人已 review + 负责人在此后明确说 "提交"; 对修复方案或提交结构的裁定不构成提交授权; 有疑问时宁可停在未提交状态问一句.**
- ST-05 / 提交闸门 (第一次) / 负责人 "确认过没问题" 后 AI 未核实后台 review 是否返回即执行提交 (884213e3) — 负责人叫停, `git reset HEAD~1` 撤回 (改动保留在工作区), 等 review 结果.
- ST-05 / v1 裁定 / 负责人裁定: ①-⑤ 修复; ⑥⑦⑧ 也修 (等 review 完成后随独立 commit 处理). ⑥⑦⑧ 的修复 (SettingsStore 将来时 / AppGraph 从句中性化 / getMyString 计数 99/69 重算) 已在工作区; 与 02 / AppGraph / README 三文件 "ST-05 内容 + 残留修复同文件" 的拆分, 届时以 "暂还原残留 → 提交 A → 补回 → 提交 B" 方式实现.
- ST-05 / 收尾 / Retrospective 五类问题 (闸门语义误读 ×2 / 注释语言中间态 / 提交闸门 old_string 语言错配 ×3 / uiautomator 陈旧 dump / 台账组稿错标) 按负责人裁定口头留档不入库; 前三条已进检查清单.
- ST-04 / step0 / primer 发出后负责人一句 "继续" 动工, 未另给目标句与问题单 — 按 subtask-04 报告范围执行; 台账代记目标句为 "按报告范围".
- ST-02 / 实施期 / AI 重做时把新增注释直接写成了英语, 跳过 "实现期中文" 中间态 — 负责人指出后已按工作流改回中文注释, 提交闸门整体译英. 检查清单新增: 实施期新注释一律先写中文, 译英只发生在提交闸门.
- ST-01 / step0 / 负责人未单独给出目标句与问题单, 以一句 "开始" 放行 — 按 subtask-01 报告范围执行; 台账代记目标句为 "按报告范围".
- ST-01 / 提交 / 5 份子任务报告按负责人指示提前入库 (3f5efa8f), 原计划随 ST-01 提交 — 后续子任务提交不再携带报告.

## 下一步

(无 — 任务已收尾. 后续: issues-cn 修复路线从新第 1 步 #11 Home 纯推导推进; 主线剩余拍板项 = 三态 → 圆点颜色映射; 模块化任务见 module-structure-cn.md 待决问题 (问题 4 已就地更新); 可选: 本分支并回 develop 的 PR.)
