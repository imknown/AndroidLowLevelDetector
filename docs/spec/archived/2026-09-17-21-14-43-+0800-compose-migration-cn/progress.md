# 进度记录: View → Jetpack Compose 迁移 (补录)

> 状态: living. 每过一道确认点由 AI 更新; 任何新会话/新 agent/新模型 接续时先读 AGENTS.md 再读本文件, 按 "下一步" 继续. 进行中的步骤 (如后台 review) 不记入进度记录, 按进度记录重跑.  
> **补录说明**: 任务完成于 2026-09-28 (滚动条替身实现), 早于 spec 工作流; 本进度记录 2026-10-04 依据 git log 与代码注释补录, 是终态快照, 不是当时的确认点记录.

## 当前位置

**任务全部完成, 已结束.** View 层清零 (无 Fragment / layout XML / AppCompat / androidx.preference),  
单 Activity + Navigation 3 + Compose 骨架运行至今; 数据层迁移期内一行未动, 迁移后的状态重构 (双 `StateFlow` / ViewModel 收集事件) 成为现行架构;  
老用户设置零迁移继承 (键与存值原样, 经 settings-ssot 任务延续).  
11 个子任务全部完成 (ST-00..07 = 计划第 0~7 步, ST-08..10 = 迁移后修正 / 文档清理 / 延期项完成); 三份事后 review 合并为 F1~F16,  
开放条目入 issues-cn 跟踪; 迁移计划教学目录按负责人决定裁撤 (内容 git 可取回); 遗留优化 8 项处理完毕 (3 项仍开放 / 1 项已完成 / 4 项并入其他条目).  
本 spec 目录为 2026-10-04 补录;  
补录稿进仓前经同模型 subagent review 三轮, 轮次不再带来新发现后停  
(v1 新上下文; v2/v3 续用同一 reviewer 做 delta 复核; v1: 4 blocking + 1 should-fix + 3 nit 全修; v2:  
1 should-fix + 1 nit 全修; v3: pass), 同模型盲区与续用的自证倾向已知, 由逐条 file:line 证据要求对冲 (v2 照抓修复轮引入的枚举错为证).

## 子任务状态 (终表)

| 子任务 | 状态 | 证据 / 备注 |
| --- | --- | --- |
| ST-00 build-prep | 已提交 | 8cd79baa (04-20) / 2430be11 (07-28) / 62c36a5c / 16e8d99c; 大半早于计划文档 |
| ST-01 model-card | 已提交 | 52510c8b / 4908b69d; 更正 b16ed770 / 8461d8b2; Style API 试水从未实施 (决策 7) |
| ST-02 prop-compose-view | 已提交 | 327e6b29; 更正 dfe23c62; 三处等价性偏差评审核实接受 |
| ST-03 interaction | 已提交 | 39b9ce15; 滚动条自绘做完但定而不实施 (18eb67df, R10) |
| ST-04 home-others | 已提交 | 78566930; HomeScreen 做出后撤销, 共用屏直配 |
| ST-05 settings-rebuild | 已提交 | 621aa979; 更正 8735f941; 直读/直写后由 settings-ssot 收编 |
| ST-06 navigation3-activity | 已提交 | 4093cd0c / 149f9f94; 更正 552a41e0; MainViewModel 删除 |
| ST-07 legacy-cleanup | 已提交 | a92e163d / 64e36091; View 层清零; 旧 MyAdapter bug 未修即除 (观察记录) |
| ST-08 post-migration-corrections | 已提交 | 21 笔, 09-20..24; 决策 9 推翻 (去 AppCompat 链), 冷启动主题时序真机验证 |
| ST-09 review-docs-closeout | 已提交 | e98a62ec / 74a58b5a / 1bd04ff5 (F1~F16) / 7843a6fc / 9c91c8cc / d753a3e8; 遗留 8 项的处理 |
| ST-10 scrollbar-standin | 已提交 | 121dd849 / d2ac3ab0 (09-28); 替身实现, Draggable 仍 stub (#37) |

## 挂起物

- **滚动条 "可拖动" 档**: stub, 保持现状不排期 ([issues-cn #37](../../../dev/issues-cn/07-UI与无障碍/37-滚动条可拖动是stub.md));  
  官方 material3 滚动条到位后从零接入.
- **Expressive 主题 / Style API / WhileSubscribed 数据加载**: 2026-09-25 移入 issues-cn 的那版记 "活" (仍开放);  
  2026-10-04 已补立项为 issues-cn #59 / #60 / #61 (出处锚定本目录).
- **教学载体**: 概念速成 / API 速查表 / 速记手册 / 术语表只存于 git 历史 (7843a6fc / 9c91c8cc), 负责人已决定不回写.

## 偏差记录

- 全程 / 流程 / 任务先于 spec 工作流 (7ed16736, 2026-10-01), 因此预写 / 计划确认点 / 逐子任务确认点未走, 以计划文档 + 事后 review 代行; 本目录即补录载体.
- 决策 6 / 7 / 9 三处变形 (自绘推迟 → 替身实现; 试水从未实施; AppCompatActivity 被推翻), 详见 plan.md 决策与实施的差异一节, 均有负责人决定记录.
- 提交 trailer 规则 2026-09-21/22 才建立, 所以 09-19/20 的迁移主体提交无 agent / model 记录, 不可考 (不虚构);  
  5cadc172 起有据 (Qoder CN / Qwen3.8-Flash).
- 教学章节 09-24/25 裁撤是有意决定, 不属文档过期; 计划文档自身按 "章首更正" 制保留调查原文 (7ed492f7).
- 迁移发生在 `jetpack-compose-new` 分支, 这个谱系后被重写, 因此本目录引用一律换算为当前分支可达哈希 (对照表见 plan.md 哈希约定).

## 下一步

(无, 补录结束: 全部修复已过 v3 终检, 随本 commit 入库, 本目录自此为定稿记录.) 后续任务各有自己的进度记录:  
Metro DI (2026-10-01-18-37-36-+0800-metro-di-adoption-cn),  
设置 SSOT (2026-10-03-15-51-18-+0800-settings-ssot-cn); issues-cn 修复路线见其 README;  
本分支 (jetpack-compose-docs) 并回 develop 的 PR 尚未开 (settings-ssot 进度记录同记此项).
