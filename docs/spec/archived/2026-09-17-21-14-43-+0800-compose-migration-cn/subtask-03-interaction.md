# ST-03: interaction 修改计划 (补录)

> 状态: record (由目录位置声明). 计划依据:  
> 计划 06 章 "第 3 步 交互补齐"  
> (幸存版: `git show 9c91c8cc^:docs/dev/compose-migration-plan-cn/05-第3步-交互补齐.md`, 教学章节裁撤后编号收缩,  
> 该章是删除前幸存 11 份文件之一). 风险: 低.

## 计划 (当时的拆分)

两件事: `PullToRefreshBox` 下拉刷新;  
自绘滚动条 (`VerticalScrollbar.kt` + `ScrollBarMode.kt`, 基于 stable 的 `ScrollIndicatorState`, 约 30 行保留设置项).

## 实际落地

- 39b9ce15: `PullToRefreshBox` 接入共用列表屏; 刷新圈配色自动取 MaterialTheme,  
  背景与 legacy 对齐 (`MyModelListScreen.kt:102`, legacy 是 primaryContainer 底 + onPrimaryContainer 转圈).
- **自绘滚动条**: 按计划做完并通过评审 (含估算漂移钳制), 但负责人拍板**不落地**:  
  等 material3 1.5.0 的 `nonInteractiveScrollbar` (自带淡出) 转正后一行替换;  
  自绘实现只留在计划文档 06 章 5.2 作参考 (18eb67df 记录推迟, 2026-09-19).

## 落地差异

- 目标树里的 `base/list/VerticalScrollbar.kt` 与 `common/ScrollBarMode.kt` **从未创建** (计划 01 章章首更正); 滚动条整体推迟由 R10 裁定.
- 设置项 UI 保留但 inert: 改设置只写 SharedPreferences, `scrollBarModeChangedSharedFlow` 零订阅者,  
  5cadc172 把注释归真 ("the emitter stays on purpose").
- 三档 (无 / 通常 / 可拖拽) 当时全不生效; F7 (review 认为该状态是缺陷) 被撤回, 这是有意维持的现状 (a3a7d249).
- 终局见 ST-10: 2026-09-28 落地自绘替身, None / Normal 两档接线生效.

## 证据

- 提交: 39b9ce15, 18eb67df, 5cadc172, a3a7d249.
- 代码: `app/src/main/java/net/imknown/android/forefrontinfo/ui/base/list/MyModelListScreen.kt` (刷新段).
