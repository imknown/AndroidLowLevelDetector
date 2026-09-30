<a id="i13"></a>

# 13 · `toPersistentList()` 在每次重组时重新分配

> 返回 [README 索引](../README.md) · [3 · UDF · 单向数据流](../README.md#3--udf--单向数据流).

**严重程度: P2 | 修复难度: 低**

**问题代码**: `ui/base/list/MyModelListScreen.kt` 的 `MyModelListScreen()` — `models = models?.toPersistentList() ?: persistentListOf()`.

**直接原因**: `models` 来自 ViewModel 的 `modelsStateFlow: StateFlow<List<MyModel>?>`, `isLoading` 来自 `isLoadingStateFlow`, 两者在同一个 composable 作用域里收集; 转换就写在函数体内, 外面没有 `remember`. 于是即使 `models` 引用没变, 只是 `isLoading` 翻动 (下拉刷新起止各一次), 也会 `O(n)` 复制出一个**新的** `PersistentList` 实例传给 `MyModelListContent`.

**根本原因**: `MyModelListContent` 的参数声明为 `PersistentList` (为了 `@Stable` 可跳过), 但在边界处无条件转换反而破坏了这个跳过前提 — 每次都是新引用.

**修改方案** (二选一):

- 方案 A (可直接动手): `val modelsPersistent = remember(models) { models?.toPersistentList() ?: persistentListOf() }` — 只有 `models` 引用变化时才重建, `isLoading` 单独翻动时保持同一实例.
- 方案 B (与 AGENTS.md 相冲, 需负责人定): 让 `BaseListViewModel` 对外就暴露 `StateFlow<PersistentList<MyModel>?>` (在 `setModels()` 处一次性 `toPersistentList()`), composable 侧不再转换. 取向本身不错 — 数据层产出不可变集合, UI 只消费 — 但 AGENTS.md 把基类的状态形状钉成 `modelsStateFlow: StateFlow<List<MyModel>?>`, 改它等于改约定; 在那之前只有方案 A 可动.
