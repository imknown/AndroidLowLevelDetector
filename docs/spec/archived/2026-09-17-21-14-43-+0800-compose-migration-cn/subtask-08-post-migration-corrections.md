# ST-08: post-migration-corrections 修改计划 (补录)

> 状态: record (由目录位置声明). 计划依据: **无对应计划章节**: 原 09 章 09 节把这批列为 "迁移后遗留优化", 实际紧贴迁移在 09-20..24 连续落地;  
> 决策 9 在此被推翻 (计划 README 落地差异认定). 风险: 高 (冷启动主题时序是主战场).

## 去重 / 归属说明

本步是计划外的迁移后尾巴, 覆盖三个批次: 主题链重构 (09-20/21), 列表状态重构 (09-21), UI 修正 (09-21..24). 拆成一份记录是因为它们共享同一个动因, 即迁移落地后暴露的 View 时代耦合该一并了断.

## 实际落地

### 去 AppCompat 主题链 (决策 9 推翻)

- 631b1ebd: darkTheme 改由 `StateFlow` 驱动, 不再 `AppCompatDelegate`.
- 900fa297: "跟随省电模式" 档退役 (四档 → 三档), 存值 1 保留为 tombstone 永不复用; 读到 1 归一为 follow system.
- d90659ad: 存量 "1" 从 UI 层归一改为启动时一次性写回 follow system (每次读都看到干净值).
- 095300a7: `MainActivity` → `ComponentActivity`, 系统栏同步.
- 600629f7: 窗口 XML 主题换平台父级, `md_theme` 色删除.
- 03f6c78e: 深色映射归 `AppThemeMode.isDark`.
- 499468ad: 冷启动修正, `setContent` 之前窗口就切到应用内主题 (这条时序此后成为反复守护的约束, settings-ssot 的 "首帧即存值" 是它的延续).
- 2aa6f52c: `appcompat` + MDC 依赖删除.
- 3d1066b8: 状态色句柄从 `R.attr` 换 `StatusColor` 枚举:  
  定义在 `ui/theme/ExtendedColors.kt` (现存代码; 计划文档章首更正写作独立文件 `StatusColor.kt`, 与代码不符, 此处按现状归真).
- 4ecc412f: `AppRootShell` 从 `AppRoot` 拆出 + preview (目标树外多出的件).

### 列表状态重构 (计划 README 落地差异所记 "迁移完成后另有一轮重构")

- befc120e: 过期排序广播收集移进 ViewModel.
- 55ecc34a: 直接观察过期排序偏好键.
- 17bc9227: 列表状态拆成 data + loading 两条 `StateFlow`, `State` 密封接口退役, 即现行 `BaseListViewModel` 双流形态 (AGENTS.md 表述的来源).
- 278f7d5a: load 落地时补齐中途变更 (现行 "Rule 2" 语义的起点).
- 5ef2ecc0: 版本信息去掉 State 包装.

### UI 修正与卫生

- a8ed04f5: 条目动画 + 涟漪反馈.
- f05b61ec: 砍底栏切换的 NavDisplay 转场.
- 980bb50f: debug 变体重命名整个应用名 (不只 label).
- 5cadc172: 滚动条 inert 注释归真 (R10 现状的记录).
- 81dbe020: 卡片 a11y 取舍落注.
- c6456b1e: 屏幕层注释把 ViewModel 拼写规范落字 (AGENTS.md 现行 "不缩写 ViewModel" 规则的代码侧起点).

## 证据

- 提交: 上列 21 笔 (2026-09-20..24).
- 现存注释: `MyApplication.kt:17` (省电档随去 AppCompat 退役), `SettingsStore.kt:119` (tombstone 归一的搬家史),  
  `MyModelListScreen.kt:57,70` (双流 + 落地补齐语义).
