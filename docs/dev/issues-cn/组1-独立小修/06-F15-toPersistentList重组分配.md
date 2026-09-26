<a id="F15"></a>

# F15 · `toPersistentList()` 每次重组都重新分配 (P2, 更好的改法)

> 返回 [README 索引](../README.md) · [组1 · 随时可做 — 独立小修](../README.md#组1--随时可做--独立小修).


> 发现: Qwen3.8-Flash (#4, 独有条目, 并入时补编 F15). 2026-09-25 复核: **仍开放**.

**直接原因**: `models` 来自 VM 的 `StateFlow<List<MyModel>?>`, 在 composable 体内每次重组都调用 `models?.toPersistentList()`. 即使 `models` 未变, 只是 `isLoading` 翻动 (下拉刷新起止各一次), 也会 `O(n)` 复制出一个**新的** `PersistentList` 实例传给 `MyModelListContent`.

**根本原因**: `MyModelListContent` 的参数声明为 `PersistentList` (为了 `@Stable` 可跳过), 但在边界处无条件转换反而破坏了这个跳过前提 — 每次都是新引用.

**问题代码**: `MyModelListScreen.kt` 的 `MyModelListScreen`: `models = models?.toPersistentList() ?: persistentListOf()`.

**修改方案** (二选一):

- 直接: `val modelsPersistent = remember(models) { models?.toPersistentList() ?: persistentListOf() }` — 只有 `models` 引用变化时才重建, `isLoading` 单独翻动时保持同一实例.
- 更干净: 让 `BaseListViewModel` 对外就暴露 `StateFlow<PersistentList<MyModel>?>` (在 `setModels` 处一次性 `toPersistentList()`), composable 侧不再转换. 这符合 "数据层产出不可变集合, UI 只消费" 的取向.


