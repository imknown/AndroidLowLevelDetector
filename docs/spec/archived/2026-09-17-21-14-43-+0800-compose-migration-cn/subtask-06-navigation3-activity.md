# ST-06: navigation3-activity 修改计划 (补录)

> 状态: record (由目录位置声明). 计划依据: 计划 09 章 "第 6 步 Navigation3 与 MainActivity 切换" (已删). 风险:  
> 高 (骨架级切换, 计划认定的最险一步, 放在 Compose 经验最足时做).

## 计划 (当时的拆分)

新增 `AppRoot.kt` (Scaffold + NavigationBar + NavDisplay) 与 `navigation/NavKeys.kt` (四个 `@Serializable` 目的地);  
MainActivity 切 `setContent`; 删 8 件: 四个 Fragment, `MainViewModel` (SavedStateHandle 记标签页, Nav3 返回栈自带保存),  
`bottom_nav_menu.xml`, `drop_scale.xml`, 过渡期桥接件; Insets 交给 Scaffold 一次性解决.

## 实际落地

- 4093cd0c: Navigation 3 (NavKey + 返回栈 + entryProvider) + 单 Activity Compose 骨架; 底栏 item 在 Kotlin 里声明  
  (`AppRoot.kt:61` 现存注释, "the legacy bottom_nav_menu.xml becomes a plain list").
- 149f9f94: 过渡期 insets 处理退役, Scaffold `innerPadding` 覆盖顶栏 / 底栏 / 系统栏;  
  ST-02 立起的 `rememberBottomBarHeight` 底栏高度测量桥接随此删除, 剩余 `contentPadding` 是内容设计边距而非 insets 补偿.

## 落地差异与等价性

- MainViewModel (SavedStateHandle 记 "上次停在哪个标签页") 按计划删除, 返回栈自带同等级保存.
- 行为延续的现存对照: `AppRoot.kt:73` (NavDisplay 的标签切换行为镜像 legacy Fragment show/hide),  
  `:144` (顶栏标题 = legacy 默认标题栏的 app 名; debug 变体整名改 app, 980bb50f),  
  `:146` (顶栏背景槽位对齐 legacy `AppBarLayout` 的 `?attr/colorSurfaceContainer`).
- 为可预览拆出的 `AppRootShell` (4ecc412f) 是目标树之外多出来的件 (计划 01 章章首更正); 决策 11 的 ViewModel 工厂小简化随 Fragment 消亡一并成立, 后被 Metro 整体取代.
- f05b61ec (09-22): 底栏切换的 NavDisplay 转场被砍, 回到 legacy 的即时切换观感; 动机提交信息未展开, `unknown`.
- 官方 "先回首页再退出" 未采纳 (决策 10, 保持任何标签直接退出); 返回键语义后来的问题由 [issues-cn #35](../../../dev/issues-cn/07-UI与无障碍/35-onBack矛盾.md) 跟踪.

## 评审与更正

552a41e0 (step-6 implementation corrections, 2026-09-19).

## 证据

- 提交: 4093cd0c, 149f9f94, 4ecc412f, 552a41e0, f05b61ec, 980bb50f.
- 代码: `app/src/main/java/net/imknown/android/forefrontinfo/ui/AppRoot.kt`, `ui/navigation/NavKeys.kt`.
