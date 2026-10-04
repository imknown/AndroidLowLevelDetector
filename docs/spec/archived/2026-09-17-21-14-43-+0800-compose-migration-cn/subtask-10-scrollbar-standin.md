# ST-10 — scrollbar-standin 修改计划 (补录)

> 状态: record (由目录位置声明). 计划依据: R10 推迟裁定的后续走向 (121dd849 提交信息 + [#37](../../../dev/issues-cn/7-UI与无障碍/37-滚动条可拖动是stub.md)). 风险: 中 (自绘绘制 + 三档存值语义).

## 背景与定位

R10 (2026-09-22) 曾裁定 "保持代码现状, 等 material3 官方滚动条, 不引 alpha, 不自绘"; 2026-09-28 负责人改为落地一个替身实现 — 自绘指示条接活 None / Normal 两档, "可拖动" 档仍空. 本步是整个 View → Compose 迁移的最后一笔代码.

## 实际落地 (121dd849)

- **替身组件**: `ui/common/ScrollBarExt.kt` 的 `Modifier.nonInteractiveScrollbar()` — 官方 material3 同名扩展在 pin 的 1.4.0 尚未提供, 以 foundation 公开的 `ScrollableState.scrollIndicatorState` 自绘 (`drawWithContent`); KDoc 记录官方到位后的替换路径 (文件头注释 `ScrollBarExt.kt:16` 起; [#37](../../../dev/issues-cn/7-UI与无障碍/37-滚动条可拖动是stub.md) 补充: 官方到位是**从零接入, 不是一行替换**, 且官方件按名即非交互, 到位也不等于 Draggable 有了实现).
- **视觉对齐 View 时代 1.18.7 实测** (`ScrollBarExt.kt:16` 现存注释): 4dp 贴尾矩形, `onSurfaceVariant` 52% alpha, 无出现动画, 300ms 延迟后 250ms 淡出, thumb 数学按平台 `ScrollBarUtils` (viewport-to-content, 下限两倍厚度) — 即 View-era `isVerticalScrollBarEnabled` 的平台观感.
- **`ScrollBarMode` 三档 + 流**: none / normal / draggable, 流形态与 themeMode 同构; 存值 "2" 保持自身身份解析为 Draggable 而不塌缩成 none — 枚举因此保持三档, 不折成布尔位 (`MyApplication.kt:34-35,44` 现存注释记录 View-era 出处: 该档对应 RecyclerView FastScroller, 2019 年因 bug 弃用后此档即空).
- **接线**: 设置行写流 (伴生 `SharedFlow` 随之删除); 三个列表页与设置页各自调 `nonInteractiveScrollbar(listState, enabled = scrollBarMode.drawsScrollBar)` — `drawsScrollBar` 仅 Normal 为 true.
- d2ac3ab0: 设置行开关居中 + 涟漪统一 (同日修正).

## 验证

提交信息自记: `assembleFossDebug` + `lintFossDebug` + `testFossDebugUnitTest` 绿, 真机过存值 0/1/2, 即时切换, 轨道尾对齐.

## 后续

- 伴生流随 settings-ssot 任务 (2026-10-03) 迁入 `SettingsStore`, 经根部 `LocalScrollBarMode` provide — 接线本身不变 (兄弟 spec `2026-10-03-15-51-18-+0800-settings-ssot-cn` ST-03).
- Draggable 保持现状不排期 ([#37](../../../dev/issues-cn/7-UI与无障碍/37-滚动条可拖动是stub.md) "处理" 节).

## 证据

- 提交: 121dd849, d2ac3ab0; 前置: 5cadc172 (inert 归真), 18eb67df (推迟记录).
- 代码: `app/src/main/java/net/imknown/android/forefrontinfo/ui/common/ScrollBarExt.kt`, `base/MyApplication.kt`.
