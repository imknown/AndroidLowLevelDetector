# 进度账本: View → Jetpack Compose 迁移 (补录)

> 状态: living. 每过一道闸门由 AI 更新; 任何新会话/新 agent/新模型 接续时先读 AGENTS.md 再读本文件, 按 "下一步" 继续. 进行中的步骤 (如后台 review) 不落账, 按账本重跑.  
> **补录说明**: 任务完成于 2026-09-28 (滚动条替身落地), 早于 spec 工作流; 本账本 2026-10-04 依据 git log 与代码注释补录, 是终态快照, 不是当时的闸门记录.

## 当前位置

**任务全部完成, 已收口.** View 层清零 (无 Fragment / layout XML / AppCompat / androidx.preference),  
单 Activity + Navigation 3 + Compose 骨架运行至今; 数据层迁移期内一行未动, 迁移后的状态重构 (双 `StateFlow` / ViewModel 收集事件) 成为现行架构;  
老用户设置零迁移继承 (键与存值原样, 经 settings-ssot 任务延续).  
11 个子任务全部落地 (ST-00..07 = 计划第 0~7 步, ST-08..10 = 迁移后修正 / 文档收口 / 延期项落地); 三份事后 review 合并为 F1~F16,  
开放条目入 issues-cn 跟踪; 迁移计划教学目录按负责人裁定裁撤 (内容 git 可取回); 遗留优化 8 项处置完毕 (3 活 / 1 完 / 4 吸收).  
本 spec 目录为 2026-10-04 补录;  
补录稿落库前经同模型 subagent review 三轮收敛  
(v1 新上下文; v2/v3 续用同一 reviewer 做 delta 复核; v1: 4 blocking + 1 should-fix + 3 nit 全修; v2:  
1 should-fix + 1 nit 全修; v3: pass), 同模型盲区与续用的自证倾向已知, 由逐条 file:line 证据要求对冲 (v2 照抓修复轮引入的枚举错为证).

## 子任务状态 (终表)

| 子任务 | 状态 | 证据 / 备注 |
| --- | --- | --- |
| ST-00 build-prep | 已提交 | 8cd79baa (04-20) / 2430be11 (07-28) / 62c36a5c / 16e8d99c; 大半早于计划文档 |
| ST-01 model-card | 已提交 | 52510c8b / 4908b69d; 更正 b16ed770 / 8461d8b2; Style API 试水未落地 (决策 7) |
| ST-02 prop-compose-view | 已提交 | 327e6b29; 更正 dfe23c62; 三处等价性偏差评审核实接受 |
| ST-03 interaction | 已提交 | 39b9ce15; 滚动条自绘做完但拍板不落地 (18eb67df, R10) |
| ST-04 home-others | 已提交 | 78566930; HomeScreen 做出后撤销, 共用屏直配 |
| ST-05 settings-rebuild | 已提交 | 621aa979; 更正 8735f941; 直读/直写后由 settings-ssot 收编 |
| ST-06 navigation3-activity | 已提交 | 4093cd0c / 149f9f94; 更正 552a41e0; MainViewModel 删除 |
| ST-07 legacy-cleanup | 已提交 | a92e163d / 64e36091; View 层清零; 旧 MyAdapter bug 未修即除 (观察记录) |
| ST-08 post-migration-corrections | 已提交 | 21 笔, 09-20..24; 决策 9 推翻 (去 AppCompat 链), 冷启动主题时序真机验证 |
| ST-09 review-docs-closeout | 已提交 | e98a62ec / 74a58b5a / 1bd04ff5 (F1~F16) / 7843a6fc / 9c91c8cc / d753a3e8; 遗留 8 项处置 |
| ST-10 scrollbar-standin | 已提交 | 121dd849 / d2ac3ab0 (09-28); 替身落地, Draggable 仍 stub (#37) |

## 挂起物

- **滚动条 "可拖动" 档**: stub, 保持现状不排期 ([issues-cn #37](../../../dev/issues-cn/07-UI与无障碍/37-滚动条可拖动是stub.md));  
  官方 material3 滚动条到位后从零接入.
- **Expressive 主题 / Style API / WhileSubscribed 数据加载**: 2026-09-25 抢救版记 "活";  
  2026-10-04 已补立项为 issues-cn #59 / #60 / #61 (出处锚定本目录).
- **教学载体**: 概念速成 / API 速查表 / 速记手册 / 术语表只存于 git 历史 (7843a6fc / 9c91c8cc), 负责人已裁定不回写.

## 偏差记录

- 全程 / 流程 / 任务先于 spec 工作流 (7ed16736, 2026-10-01), 因此预写 / plan gate / 逐子任务闸门未走, 以计划文档 + 事后 review 代行; 本目录即补录载体.
- 决策 6 / 7 / 9 三处变形 (自绘推迟 → 替身落地; 试水未落地; AppCompatActivity 被推翻), 详见 plan.md 决策落地差异节, 均有负责人裁定记录.
- 提交 trailer 规则 2026-09-21/22 才建立, 所以 09-19/20 的迁移主体提交无 agent / model 记录, 不可考 (不虚构);  
  5cadc172 起有据 (Qoder CN / Qwen3.8-Flash).
- 教学章节 09-24/25 裁撤是有意决定, 非文档漂移; 计划文档自身按 "章首更正" 制保留调查原文 (7ed492f7).
- 迁移发生在 `jetpack-compose-new` 分支, 该谱系后被重写, 因此本目录引用一律换算为当前分支可达哈希 (对照表见 plan.md 哈希约定).

## 下一步

(无, 补录收尾: 全部修复已过 v3 终检, 随本 commit 入库, 本目录自此为定稿记录.) 后续任务各自立账:  
Metro DI (2026-10-01-18-37-36-+0800-metro-di-adoption-cn),  
设置 SSOT (2026-10-03-15-51-18-+0800-settings-ssot-cn); issues-cn 修复路线见其 README;  
本分支 (jetpack-compose-docs) 并回 develop 的 PR 尚未开 (settings-ssot 账本同记此项).
