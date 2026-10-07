# View → Jetpack Compose 迁移: 修改计划 (补录)

> 状态: record (由目录位置声明, 即本目录位于 `docs/spec/archived/`, 归档即定稿, 不再编辑).  
> **补录说明**: 本 spec 于 2026-10-04 依据 git log, 现存代码注释与已删迁移计划文档的 git 存档追溯重建.  
> 目录名时间戳取任务开工时点 (计划文档入库 75e7d2e9, 2026-09-17 21:14:43 +0800), 非补录时刻.  
> 迁移本身 (前史 2026-09-12, 主体 2026-09-19/20, 收尾 2026-09-28) 早于本仓库的 spec 任务工作流 (7ed16736, 2026-10-01 引入) 约两周,  
> 因此负责人预写 / plan gate / 逐子任务闸门均未按现行流程走过:  
> 当时的计划载体是 `docs/dev/compose-migration-plan-cn/` 文档目录 (已删, `git show 9c91c8cc^:<路径>` 可取回), 收口载体是三份事后代码 review.  
> 本目录只记发生过的事, 不虚构未发生的闸门; 证据随引随注, 汇总见 [证据基线与哈希约定](#证据基线与哈希约定).

## 负责人预写

(无, 任务先于 spec 工作流, 预写制度当时不存在.) 迁移当时的成文意图以计划文档 README 开篇为准 (75e7d2e9, 2026-09-17): 把单 Activity + 4 Fragment +  
RecyclerView/Preference 的 View 界面层**整体**渐进迁到 Jetpack Compose; "迁移是载体, 学会 Compose 是目的";  
每步小且独立 (可编译, 可运行, 可单独回退); 数据层 (ViewModel / Repository / DataSource) 与 SharedPreferences **一行不动**, 老用户设置全部继承.

## 背景与动机

迁移前界面层的痛点 → Compose 解法 (计划 01 章盘点表, 迁移后全部兑现):

| 痛点 (View 时代) | Compose 解法 |
| --- | --- |
| 手动 `setOnApplyWindowInsetsListener` 算内边距, 还要跨 View 拿底栏高度 (`MainActivity.initWindowInsets` / `BaseListFragment.initWindowInsets`) | `Scaffold` 的 `contentWindowInsets` 自带, 零手动计算 (149f9f94) |
| 手动 `commitNow` + show/hide Fragment, 防重复选中 | Navigation 3: `NavDisplay` + 返回栈状态即全部 (4093cd0c) |
| Adapter / ViewHolder / DiffUtil / ItemDecoration 四件套样板 | `LazyColumn { items(list, key = ...) }` (MyModelListScreen.kt:130) |
| SwipeRefreshLayout 刷新圈手动取主题色 | `PullToRefreshBox` 自动用 MaterialTheme 配色 (39b9ce15) |
| 界面一半 XML 一半 Kotlin, 改个字号跳两个文件 | 单一 Kotlin + `@Preview` |
| 主题双轨制 (View 走 XML 主题, Compose 走 `AppTheme`, 动态色两头接) | 迁完后只有 `AppTheme` 一条路 (ST-08 终态) |

前史: Compose 构建接线 2026-04-20 就已入库 (8cd79baa), Compose 主题 2026-07-28 先行备好 (2430be11);  
2026-09-12 一轮架构体检 (R 系列 + 快赢清单) 把 FR-12 底栏随滚动 (R1), 自适应导航 (R2), 滚动条 (R10) 显式延期到 Compose 之后再做,  
并把执行计划改为 easy-first (a622d352, 对象在库但非当前分支祖先, 也无孪生可换算, 是下文哈希约定的唯一例外);  
2026-09-16 三份外部调研 (Navigation 3 与迁移指南 / 滚动条与 M3 组件 / Style API) 对官方文档逐条核对后,  
09-17 计划文档入库 (75e7d2e9, 14 份文件: README + 现状盘点 + 第 0~7 步各一章 + API 速查与术语附录).  
体检条目的代码修复同窗口落账  
(0318c866, f27168fe, 6db39365, 41c75beb, 97bbb455, 79495b0d, a3f81670, 7c0f8aac, 2026-09-12/13), 如 C2  
(CancellationException 先于通用 catch 重抛) 对 97bbb455, S7 (SavedStateHandle 单源裁定) 对 41c75beb; 对应关系按提交主题与体检条目比对得出,  
当时文档未直接互引哈希.

## 目标

1. View 层清零: 无 Fragment / layout XML / AppCompat / androidx.preference (计划 README 2026-09-22 状态块原文).
2. 渐进式: 每步最小高内聚, 独立可编译, 可单独 revert; 顺序 = 官方迁移指南的增量路线 (先内容后骨架), 同时是风险递增序 (列表内容低 → 偏好重建中 → 导航与 Activity 高).
3. 老用户设置全部继承: SharedPreferences 键与存值原样 (该约定后来被 settings-ssot 任务沿用为 "存值格式零迁移").
4. 数据层不动 (迁移期内; 迁移后的状态重构见 ST-08, 属计划外追加).

## 关键决策点 (计划 README 决策表, 11 条)

| # | 决策 | 理由 | 备选 (未选) |
| --- | --- | --- | --- |
| 1 | 顺序: 先屏幕内容 (ComposeView 嵌在 Fragment 里) → 最后换导航骨架 | 官方迁移指南增量路线; 每步可独立回退; 学习曲线平滑 | 自顶向下先换 Activity |
| 2 | 首个迁移页面选 Prop | 三列表页中最简单 (无事件监听) | Others; 设置页 (复杂度更高, 放第 5 步) |
| 3 | 设置页手写重建 (`LazyColumn` + M3 组件) | 官方无 Compose 版 Preference 库 (androidx.preference 停在 2023) | `AndroidFragment` 包旧页过渡 |
| 4 | 导航用 Navigation 3 (1.2.0-rc01) | 官方 Compose-only 架构推荐; RC 通道符合版本成熟度政策 | Navigation Compose (Nav2); 继续 Fragment |
| 5 | 底栏 `NavigationBar` (stable) | 与 BottomNavigationView 视觉延续; 不引 alpha 依赖 | `ShortNavigationBar`; `NavigationSuiteScaffold` (遗留优化) |
| 6 | 滚动条: 计划自绘 → 实现期 (09-19) 改为推迟 | 官方滚动条在 material3 1.5.0-alpha (不在 BOM); 自绘版做完并通过评审后负责人拍板不落地 | 砍设置项 (功能回退); 显式引 alpha |
| 7 | Style API 单文件试水 | 负责人点名要学的新范式 | 全面采用 (1.13 迁移成本高); 完全不用 |
| 8 | 主题沿用 `AppTheme` (标准 M3 + 动态取色) | Expressive 主题 API 已从 material3 1.4.0 stable 移除 | BOM 升 1.5 后切 Expressive (遗留优化) |
| 9 | MainActivity 暂留 AppCompatActivity | 主题四档靠 `AppCompatDelegate.setDefaultNightMode` | ComponentActivity + Compose 侧自管 darkTheme |
| 10 | 返回键: 任何标签直接退出 | 与现有行为一致 | 官方推荐 "先回首页再退出" |
| 11 | ViewModel 工厂第 6 步小简化 | Fragment 的 extrasProducer 接线随 Fragment 消亡 | 保留 CreationExtras 注入 |

### 决策落地差异 (原 README 2026-09-22 汇总 + 后续走向)

- **决策 6 (滚动条)** 两度变形: 自绘实现做完并通过评审但拍板不落地 (R10, 5cadc172 归真 "inert" 注释 (归真: 把过时的表述改写回与代码现状一致));  
  2026-09-28 落地自绘替身 `nonInteractiveScrollbar` (ST-10, 121dd849),  
  三档中 None / Normal 两档接线生效 (`drawsScrollBar` 仅认 Normal),  
  Draggable 仍是 stub (现由 [issues-cn #37](../../../dev/issues-cn/07-UI与无障碍/37-滚动条可拖动是stub.md) 跟踪, 保持现状).
- **决策 7 (Style API)**: 试水代码从未落地, stable foundation 1.12.1 反编译实证无该签名 (b16ed770 记录更正);  
  遗留为 foundation 1.13 重构后从零立项, 现登记为 [issues-cn #60](../../../dev/issues-cn/07-UI与无障碍/60-StyleAPI跟进.md).
- **决策 9 (暂留 AppCompatActivity)**: 被推翻, 09-20/21 的去 AppCompat 链把 `MainActivity` 换成 `ComponentActivity`,  
  主题改 `StateFlow` 单源, `appcompat` / MDC 依赖删除, "跟随省电模式" 档随之退役 (四档 → 三档终态, 存值 1 保留为 tombstone). 见 ST-08.
- **决策 11 (ViewModel 工厂简化)**: 落地后又被整体取代:  
  2026-10-02 起由 Metro 编译期 DI 接管 (兄弟 spec `2026-10-01-18-37-36-+0800-metro-di-adoption-cn`).
- 计划 04/05/06 章的四个教学设想 (Style API 试水, `produceState` 防闪空, 自绘滚动条落地, 组合内收集 `SharedFlow`) 均未进入代码  
  (原 README 落地差异认定); 过期排序事件的收集最终落在 ViewModel (befc120e), 不在组合内.

## 子任务拆分 ↔ 实际落地

| # | slug | 范围 | 实际落地 (提交) | 风险 |
| --- | --- | --- | --- | --- |
| 00 | build-prep | Compose 构建接线与依赖 | 8cd79baa (04-20, 早于计划), 2430be11 (07-28 主题备好), 62c36a5c + 16e8d99c (09-19 当日增量) | 低 |
| 01 | model-card | `ExtendedColors` + `MyModelCard` + 稳定性注解起步 | 52510c8b, 4908b69d; 80e05e00 / 9a44c79a / 3d9e06b4 / c9bb0c8b; 6d4d76d8 | 低 (纯新增, 零运行时影响) |
| 02 | prop-compose-view | 共用列表屏 `MyModelListScreen` + PropFragment 换壳 | 327e6b29 | 低 |
| 03 | interaction | `PullToRefreshBox` + 滚动条 (自绘 → R10 推迟) | 39b9ce15; 18eb67df (文档) | 低 |
| 04 | home-others | Home / Others 换壳, `HomeScreen` 做出后撤销 | 78566930 | 低 |
| 05 | settings-rebuild | 设置页手写 M3 重建 | 621aa979 | 中 |
| 06 | navigation3-activity | Navigation 3 + 单 Activity 骨架 + Scaffold insets | 4093cd0c, 149f9f94 | 高 |
| 07 | legacy-cleanup | View 层清零 (第 7 步) | a92e163d, 64e36091 | 低 (纯删除, 回归面大) |
| 08 | post-migration-corrections | 去 AppCompat 主题链 + 状态重构 + UI 修正 (09-20..24) | 631b1ebd, 900fa297, d90659ad, 095300a7, 600629f7, 03f6c78e, 499468ad, 2aa6f52c, 3d1066b8, 4ecc412f, befc120e, 55ecc34a, 17bc9227, 278f7d5a, 5ef2ecc0, a8ed04f5, f05b61ec, 980bb50f, 5cadc172, 81dbe020, c6456b1e | 高 (冷启动主题时序) |
| 09 | review-docs-closeout | 三份 review 合并 F1~F16 + 计划文档归真与删除 + 抢救 (09-21..25) | e98a62ec, 74a58b5a, a3a7d249, dd34996f, 7843a6fc, 1bd04ff5, 9c91c8cc, d753a3e8 | 低 |
| 10 | scrollbar-standin | 滚动条替身落地 (09-28, R10 推迟项的终态) | 121dd849, d2ac3ab0 | 中 |

ST-00..07 与计划第 0~7 步一一对应; ST-08..10 是计划外的迁移后尾巴 (修正 / 收口 / 延期项落地), 各自的报告里注明.

## 验证基线

- 每步收尾过该章 "验证清单" 再进下一步 (计划全局约定); 各章验证清单正文随教学目录删除, git 历史可取回. 构建门槛 `assembleFossDebug`; ST-10 的提交信息自记了完整三件套  
  (assembleFossDebug + lintFossDebug + testFossDebugUnitTest) 与真机验证.
- 回归验证清单 (2026-09-25 抢救版, 11 项; 括号内是 2026-10-04 的现状注):
  1. 四页面数据正确, 卡片视觉逐项对齐 (圆角, `surfaceBright` 底色, 字号, 色点, 间距 12dp/10dp);
  2. 下拉刷新: 三列表页手势, 转圈配色, 刷新期间列表不闪空;
  3. 滚动条: 设置项读写正常 (抢救版记 "inert", 已过时: ST-10 落地后 None/Normal 两档生效, Draggable 仍 stub,  
     [#37](../../../dev/issues-cn/07-UI与无障碍/37-滚动条可拖动是stub.md));
  4. 设置项继承: 旧版升级后主题/滚动条/两开关的值全部保留 (SP 键未动, 该性质经 settings-ssot 任务延续: 键与存值格式零迁移);
  5. 主题: 三档 × 深浅色 × 动态取色 (Android 12+) × 高对比度抽查 (抢救版已更正: "四档" 的省电档随去 AppCompat 退役, 存量值启动时一次性迁回);
  6. 导航: 标签切换各自保留状态; 杀进程重启回原标签; 返回手势正常;
  7. 事件链路: 过期应用排序开关 → Home 条目刷新; 网络开关 → 联网/离线策略切换;
  8. 外链与彩蛋: 五个外链, 无浏览器 Toast, 版本七连击;
  9. 多语言: zh-rCN / zh-rTW / fr-rFR 各过一遍;
  10. 双 flavor: `assembleFossRelease` / `assembleFirebaseRelease` 出包, 混淆开启, shop 链接各归其主;
  11. 性能主观对比: 冷启动, 切标签, 快速滚动.

## 证据基线与哈希约定

- **哈希约定**: 本目录引用的提交哈希一律以当前分支 (jetpack-compose-docs) 可达为准. 迁移开发发生在 `jetpack-compose-new` 分支, 该谱系后来被重写:  
  幸存文本 (计划文档的章首更正, 2026-09-25 附录) 引用的是重写前孪生提交, 同主题同日期但哈希不同, 对象仍在库中而非当前分支祖先 (issues-cn README 基线块同此认定).  
  已核对的孪生对照: 733c6941=095300a7, 26b9094f=631b1ebd, e51260e6=600629f7, f66562d4=2aa6f52c, 16963ba3=3d1066b8,  
  26ee0f9d=4ecc412f, 38492b82=55ecc34a, 002f25b3=17bc9227. 唯一例外: 前史的 a622d352 (2026-09-12 体检计划调整) 无孪生,  
  保留原哈希就地标注, 仅作检索用.
- **证据源**:
  1. git log 窗口 2026-09-12..2026-09-28 (本分支);
  2. 已删计划目录: 全 14 文件见 75e7d2e9,  
     删除前幸存 11 文件 (README + 01~09 各章 + A 观察记录; `git ls-tree 9c91c8cc^:docs/dev/compose-migration-plan-cn/` 可核);  
     本目录引用其中五份 (README, 01 盘点, 02-第0步, 05 交互, A 观察记录), `git show 9c91c8cc^:<路径>` 取回;
  3. 现存代码注释 (View 时代等价性对照): `MyModelListScreen.kt:57,70,102,117,123,130,132,138,143` ·  
     `MyModelCard.kt:34,43,53,56` · `AppRoot.kt:61,73,144,146` · `ScrollBarExt.kt:16,27` · `Type.kt:10-14` ·  
     `MyApplication.kt:17,34-35,44` · `SettingsScreen.kt:83` · `SettingsStore.kt:119` · `AppGraph.kt:184`;
  4. [docs/dev/issues-cn](../../../dev/issues-cn/README.md): README 基线块 (分支重写认定), #06/#37/#38/#39/#50;
  5. 兄弟 spec (迁移后的后续任务, 各自立账): `2026-10-01-18-37-36-+0800-metro-di-adoption-cn`,  
     `2026-10-03-15-51-18-+0800-settings-ssot-cn`.

## 待学清单的结局

计划的教学目标 ("迁移是载体, 学会 Compose 是目的") 的载体 (概念速成, API 速查表, 速记手册, 术语表等教学章节)  
于 09-24 以 "教学章节" 为由裁撤 (7843a6fc; "需要时从 git 历史取回" 一句随 58e469b7 的 README 更正入库), 09-25 整目录删除 (9c91c8cc).  
无 "收尾清空待学清单" 动作: 收尾制度当时不存在; 学习载体的存废是负责人的显式决定, 不是遗漏.
