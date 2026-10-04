<a id="i37"></a>

# 37 滚动条 "可拖动" 模式是 stub

> 返回 [README 索引](../README.md) · [7 · UI 与无障碍](../README.md#7--ui-与无障碍).

**严重程度: P2(🟢) | 修复难度: 低**

- **数组维持现状**: `ui/settings/res/values/arrays.xml` 的 `scrollBarKeys` 三个 item 里第三个 (`@string/interface_fast_scroll_bar_key`) 是注释状态, 于是 2 个 label 对 3 个 value; 默认值是 None — `SettingsStore` 播种时拿 `interface_no_scroll_bar_value` 兜底. 这条 2/3 错位本身的翻车风险归 [#18](../05-稳定性与错误处理/18-滚动条数组错位.md).
- **两档今天生效**: 这个偏好经 `SettingsStore.scrollBarMode: StateFlow` 送出 (根部经 `LocalScrollBarMode` provide; 曾经的 `MyApplication.scrollBarMode` 伴生流由 `121dd849` 引入, 已迁入 store, 原条目见 [archived #08](../archived/2-SSOT-唯一数据来源/08-设置无唯一数据来源.md)), 两个滚动列表 — `ui/base/list/MyModelListScreen.kt` 的 `MyModelListContent()` 与 `ui/settings/SettingsScreen.kt` 的 `SettingsContent()` — 各自调用 `.nonInteractiveScrollbar(listState, enabled = scrollBarMode.drawsScrollBar)`. 所以 None 与 Normal 都是接了线的: 设置改了立即反映在页面上, 不需要重进页面.
- **stub 只在第三档**: `ScrollBarMode.drawsScrollBar` 只对 `Normal` 为 true (`base/MyApplication.kt`), `Draggable` 没有任何实现 — 选它等于选 "什么都不画".
- **今天的实现是自绘**: `ui/common/ScrollBarExt.kt` 的 `Modifier.nonInteractiveScrollbar()` 用 `drawWithContent` 按平台 `ScrollBarUtils` 的 thumb 算法手画指示条 (4dp 宽, 靠尾边, 出现时不做动画而淡出为 250ms + 300ms 延迟, 色值取 `colorOnSurfaceVariant` 的 52% alpha). 文件注释写明它是官方同名扩展 `androidx.compose.material3` 的替身 — 当前 pin 的 material3 (Compose BOM `2026.09.00`, `gradle/toml/android.toml`) 还没有那个 API; 官方到位后的动作是删掉这个文件, 调用点改传 `scrollState.scrollIndicatorState`.
- **自绘这半边不解决 "可拖动"**: 官方那个扩展按其名字就是非交互的, `ScrollBarExt.kt` 的注释也写着 "Nothing is draggable"; 所以官方组件到位并不等于 `Draggable` 有了实现, fast-scroll 若还想要是另一件事.
- **历史载体**: 原 `ui/common/ViewExt.kt` (只切 `RecyclerView.isVerticalScrollBarEnabled`) 已随 `64e36091` 删除. `base/MyApplication.kt` 的 `ScrollBarMode` 注释记录了更早的一段: 那一档对应 View 时代的 fast scroll, 其 RecyclerView `FastScroller` 早在 2019 年就因 bug 被弃用.
- **界面选不到第三档**: `SettingsChoiceDialog()` 用 `labels.zip(values)` 配对, `zip` 截断到较短的一侧, 所以对话框里只渲染 2 个选项. 存着 `"2"` 的老偏好仍会被读成 `Draggable` — `SettingsStore.parseScrollBarMode()` 特意保留那条映射 (`interface_fast_scroll_bar_value -> ScrollBarMode.Draggable`), 正是为了认得这个值而不是把它塌缩成 None; 枚举也因此保持三档而不折成一个布尔位. 这类老用户打开对话框还有一个观感后果: 两个选项的 value 都不等于 `"2"`, 没有任何单选被选中.

**处理**: 保持现状, 不排期实现. 缺的是 "可拖动" 的实现而不是接线 (接线部分 `121dd849` 已做完), 所以动手时机跟着官方组件与 fast-scroll 的需求走; 官方组件到位时是从零接入, 不是 "一行替换".
