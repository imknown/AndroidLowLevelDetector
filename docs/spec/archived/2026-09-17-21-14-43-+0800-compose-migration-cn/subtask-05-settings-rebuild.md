# ST-05: settings-rebuild 修改计划 (补录)

> 状态: record (由目录位置声明). 计划依据: 计划 08 章 "第 5 步 Settings 页面重建" (已删). 风险: 中 (对话框 / 开关 / 外链 / 事件, 计划认定的复杂度最高页).

## 计划 (当时的拆分)

`SettingsFragment` + `preferences.xml` + `preference_widget_material_switch.xml` 用 Material 3 组件整体重建:  
`LazyColumn` + `ListItem` / `Switch` / `AlertDialog` 槽位;  
偏好读写三段式 (决策 3: 官方无 Compose 版 Preference 库, 对齐 Now in Android 的手写路线).

## 实际落地

- 621aa979: `SettingsScreen` 手写重建: 主题模式与滚动条两个下拉 (三段式: 读 SP 存值 → `SettingsChoiceDialog` 选项 → 写回并发射), 两个开关,  
  五个外链, 一个版本信息; 键与存值原样, 老设置零迁移直接继承.

## 落地差异与后续

- 偏好读写当时的形态是 "SP 直读 + 伴生流发射";  
  这套直读/直写在 2026-10-03 的 settings-ssot 任务中整体收编进 `SettingsStore` (issues-cn #08 全量 + #10):  
  迁移期内数据层与 SP 确实一行未动 (计划 README 落地差异认定), 收编是另立的后续任务 (兄弟 spec `2026-10-03-15-51-18-+0800-settings-ssot-cn`).
- 版本信息行镜像 legacy Fragment 的 "subscribe + init once" 语义 (`SettingsScreen.kt:83` 现存注释); State 包装随后被 5ef2ecc0 去掉 (ST-08).
- 省电档退役后的存值归一 (tombstone "1" → follow system) 起初在设置 UI 层, 后移到启动期 (d90659ad), 最终随 SSOT 任务迁进 `SettingsStore`  
  (`SettingsStore.kt:119` 现存注释记录了这段搬家).

## 评审与更正

8735f941 (step-5 implementation corrections, 2026-09-19).

## 证据

- 提交: 621aa979, 8735f941.
- 代码: `app/src/main/java/net/imknown/android/forefrontinfo/ui/settings/SettingsScreen.kt`.
