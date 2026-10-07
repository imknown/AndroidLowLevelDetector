# ST-03: scrollbar-vertical-slice 修改计划

> 状态: living (动工时如有修订在此显式记录). 计划依据: 本目录 plan.md + issues-cn #08 / #10. 风险: 低 (机械同型, ST-02 模式已立).

## 动工记录 (2026-10-03)

- 按本报告实施: `LocalScrollBarMode` 定义在 `MyModelListScreen.kt` (形态与 `LocalThemeMode` 同构:  
  `ProvidableCompositionLocal<StateFlow<ScrollBarMode>>` + staticCompositionLocalOf + 常量 None preview 默认;  
  定义落点 = 首要消费方同文件, 对齐 LocalThemeMode-in-Theme.kt 先例), `MainActivity` 根部 provide 第二行 + 字段注释更新,  
  `MyModelListScreen` 收集换源 + 注释归真,  
  `MyApplication` 删滚动条三件 (伴生对象只剩 `instance` / `sharedPreferences` / `getMyString`, 留给 #02 收口; 未用 import 清理),  
  `SettingsScreen` 滚动条行换源 + 注释归真, `SettingsViewModel` 加 [Scroll bar] 区.  
  三态语义逐字保留 (存值 "2" 仍解析为 Draggable, `drawsScrollBar` 只认 Normal).
- 验证: `assembleFossDebug` 绿 (EXIT=0, 唯一警告为 `LldDataSource` 既有); 零残留 (三个已删符号全仓零现在时引用, SettingsStore 的过去时溯源注释除外); 标点零违规.

## 目标句 (占位, 动工对照时由负责人确认或改写)

滚动条设置项整链搬进 `SettingsStore`: `MyModelListScreen` (三个列表页共用件) 与设置页滚动条行改观察 store, `MyApplication` 伴生对象上的  
`scrollBarMode` / `setMyScrollBar` / `initScrollBar` 删除.

## 变更清单

1. `base/MyApplication.kt`: 删 companion `scrollBarMode` / `setMyScrollBar`;  
   删 `initScrollBar()` 及其 `onCreate` 调用 (store 播种已随 ST-02 的首解析落地); 相邻注释归真.
2. `ui/MainActivity.kt`:  
   `setContent` 根部的 `CompositionLocalProvider` 补一行 `LocalScrollBarMode provides settingsStore.scrollBarMode`  
   (store 字段 ST-02 已缓存, 只差 provide). 缺这行则下方两个收集点永远拿到定义处默认 `None`, 指示条全站消失, 三档切换失效, 且单独可编译不报错, 只能靠真机冒烟兜住.
3. `ui/base/list/MyModelListScreen.kt`: `MyApplication.scrollBarMode.collectAsStateWithLifecycle()` (`:54`) 换源  
   (`LocalScrollBarMode` CompositionLocal, 即 ST-02 钉死的按流拆形态在此复用:  
   类型 `CompositionLocal<StateFlow<ScrollBarMode>>`, 携带 `settingsStore.scrollBarMode`,  
   preview 默认 `MutableStateFlow(ScrollBarMode.None)`;  
   `MyModelListScreen.kt:153` 的 preview 传常量不受影响); 相邻注释 (`:52-53` "MyApplication owns it") 归真.
4. `ui/settings/SettingsScreen.kt`: `scrollBarMode` 收集 (`:115`) 换源 (`LocalScrollBarMode`, 同上);  
   `scrollBarValue` 的 `remember` 初读 (`:90-95`) 改 ViewModel 状态 (首帧即存值形态); `onScrollBarSelect` (`:129-133`) 改  
   `viewModel.setScrollBarMode(value)`; 删 `scrollBarKey` / `scrollBarDefaultValue` 直读; 相邻注释归真.
5. `ui/settings/SettingsViewModel.kt`: 滚动条原始存值状态 + `setScrollBarMode(value: String)`.
6. `ui/AppRoot.kt:186` 的 preview 常量传参不受影响.

## 行为等价性

- `setMyScrollBar` 的 when 逐字保持: `interface_normal_scroll_bar_value` → Normal,  
  退役的 `interface_fast_scroll_bar_value` → Draggable (存值仍解析为自身), 其余/null → None.
- `drawsScrollBar` 语义不变: 仅 Normal 画指示条.

## 验证

- `./gradlew assembleFossDebug` 绿.
- 真机: 三档切换即时生效 (设置页与 Home / Others / Prop 任一页指示条同步); `adb shell am kill` 重启存值恢复; 进入设置页首帧滚动条行选中项即为存值.
- 零残留 grep: `MyApplication.scrollBarMode` / `setMyScrollBar` / `initScrollBar` 全仓零现在时引用.
