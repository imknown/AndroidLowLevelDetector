# ST-01: model-card 修改计划 (补录)

> 状态: record (由目录位置声明). 计划依据: 计划 04 章 "第 1 步 列表卡片组件" (已删, `git show 9c91c8cc^` 取回需看 75e7d2e9 版). 风险:  
> 低 (纯新增, 零运行时影响, 计划原文).

## 计划 (当时的拆分)

新增 `MyModelCard` (等价 `my_view_holder.xml`) + `ExtendedColors` (项目状态色), 另含 Style API 单文件试水 (决策 7) 与 `@Preview` 教学.

## 实际落地

- 52510c8b: `ExtendedColors` (`LocalExtendedColors` 镜像 `values/` + `values-night/colors.xml` 的  
  noProblem/warning/critical 状态色, 在 `AppTheme` 内 provide).
- 4908b69d: `MyModelCard`, 等价 `my_view_holder.xml` (MaterialCardView + 标题 + 详情 + 右上角状态色圆点).
- 稳定性注解随当日各步起步: 80e05e00 (`BaseListViewModel` `@Stable`) / 9a44c79a (`HomeViewModel` `@Stable`) / 3d9e06b4  
  (`SettingsViewModel` `@Stable`) / c9bb0c8b (`State` `@Immutable`),  
  这就是 AGENTS.md 现行 "Compose 稳定性注解 + 头注释解释为何安全" 模式的起点.
- 6d4d76d8: includeFontPadding 恢复, 近期 Compose 翻转了该默认值, 手动补回以对齐 legacy TextView 观感 (`Type.kt:10-14` 现存注释).

## 落地差异与等价性

- **Style API 试水未落地**: stable foundation 1.12.1 反编译实证无文档所示签名, 无法编译 (b16ed770 记录更正); 遗留为 foundation 1.13 重构后从零立项.
- **`textDirection="locale"` 不复刻** (有意接受的偏差, 观察记录 2026-09-19): Compose `TextDirection` 无 `Locale` 常量,  
  material3 `Text` 无该参数; `TextAlign.Start` + `LocalLayoutDirection` 跟随 locale 已达成等价, 剩余差异只在混排字符的判定方式,  
  强行写死反而破坏 RTL.
- **接受的 a11y 取舍**: `Card(onClick = {})` 保留只为涟漪, TalkBack 宣告 double-tap-activatable 而无动作;  
  负责人拍板 "记录而非修复" (81dbe020, 逃生口注释在案), 后由 [issues-cn #34](../../../dev/issues-cn/07-UI与无障碍/34-Card空点击.md) 独立跟踪.
- 现存等价性注释: `MyModelCard.kt:34` (16sp → dp 运行时换算), `:43` (ripple-only card),  
  `:53` (DefaultItemAnimator → `animateItem`), `:56` (legacy Card 无阴影, Compose 默认 1dp 清零).

## 评审与更正

b16ed770 (step-1 Style API 更正 + 迁移期观察记录起笔), 8461d8b2 (typography includeFontPadding 更正入 step-1 章).

## 证据

- 提交: 52510c8b, 4908b69d, 80e05e00, 9a44c79a, 3d9e06b4, c9bb0c8b, 6d4d76d8, 81dbe020.
- 代码: `app/src/main/java/net/imknown/android/forefrontinfo/ui/base/list/MyModelCard.kt`, `ui/theme/Type.kt`.
