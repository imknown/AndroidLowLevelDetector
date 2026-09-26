<a id="R10"></a>

# R10 滚动条"可拖动"模式是 stub

> 返回 [README 索引](../README.md) · [组4 · 已裁定, 无待办](../README.md#组4--已裁定-无待办).


**严重程度: P2(🟢) | 处理: 决策③**

`arrays.xml` 的 `scrollBarKeys` 第三项被注释掉 (labels 2 个 / values 3 个不匹配), 默认值为 None; 实现只切换 `RecyclerView.isVerticalScrollBarEnabled` (原 `ViewExt.kt`, 已随 `fcc048d5` 删除), 不存在 fast-scroll(复验).**处理 (决策③;2026-09-12 改口: 先留着不删)**: 数组维持现状 — 第三项本来就被注释掉, 设置页里选不到它; fast-scroll 若还想要, 归 Compose 时代.

**2026-09-22 复验 (追加, 不改上文)**: 上面那句 "实现只切 `RecyclerView.isVerticalScrollBarEnabled`" 的载体 `ViewExt.kt` 已随 View 层删除 (`fcc048d5`), 列表侧**没有任何订阅者**: `SettingsViewModel.scrollBarModeChangedSharedFlow 与 emitScrollBarModeChangedSharedFlow()` 照旧 emit `scrollBarModeChangedSharedFlow`, 全仓零 collector(`SettingsScreen() 的 onScrollBarSelect` 把值写进 SharedPreferences, 并 emit 到这条无人订阅的流). 所以缺口比原记的更大: 不是 "只有第三档是 stub", 而是**三档全不生效**. `arrays.xml` 的结构与默认值仍与上文一致.

**处理 (2026-09-22 负责人定)**: 保持代码现状, 等官方方案 — material3 的滚动条组件目前只在 alpha 线 (不在 Compose BOM 内), 既不引入 alpha 覆盖 BOM, 也不自绘. 设置项, `arrays.xml` 三档值与那条 emit 一并留着, 官方组件转正后把值接上即可 (届时是从零接入, 不是 "一行替换");`SettingsScreen() 里 onScrollBarSelect 的那条注释` 的注释已按现状改写 (不再声称"列表页立即生效"). `fast` 档继续保持注释状态.


