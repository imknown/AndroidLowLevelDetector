<a id="i13"></a>

# 13 · `toPersistentList()` 在每次重组时重新分配

> 返回 [README 索引](../../README.md) · [3 · UDF · 单向数据流](../../README.md#3--udf--单向数据流).

**严重程度: P2 | 修复难度: 低**

**问题代码**: `ui/base/list/MyModelListScreen.kt` 的 `MyModelListScreen()`:  
`models = models?.toPersistentList() ?: persistentListOf()`.

**直接原因**: `models` 与 `isLoading` 在同一个 composable 作用域里收集 (`scrollBarMode` 也在同一作用域),  
转换写在函数体内, 外面没有任何缓存.  
于是 `models` 未变而 `isLoading` 翻动 (下拉刷新起止各一次) 时, 函数体重跑照样做一次 `O(n)` 复制.

**根本原因**: 不可变集合的转换点放在 UI 层的函数体里, 而不是数据落地的边界,  
组合路径因此每次重组复制一遍, 副本再短命一次 (条目最多的 Prop 页最明显).

**代价的边界**: 跳过没有因此失效. `PersistentList` 在 compose 编译器的已知稳定类型名单里,  
这个形参按 `equals` 比内容, 复制出的等值实例挡不住跳过; 只有函数类型的形参按引用比,  
而 `onRefresh = viewModel::refresh` 已被编译器记忆化. 所以本条是主线程上的纯浪费, 不是下游重组放大.

**修复 (已实施)**: 转换点移到边界. `BaseListViewModel.modelsStateFlow` 改为  
`StateFlow<PersistentList<MyModel>?>`, `setModels()` 一次性 `toPersistentList()`,  
`updateModelDetail()` 改用 `PersistentList.replacingAt()` (`set()` 已废弃),  
顺带去掉了原来每个补丁一次的 `toMutableList()` 全表复制,  
`MyModelListScreen()` 只剩 `models ?: persistentListOf()`.  
编译产物里 `MyModelListScreenKt` 已无 `toPersistentList` 引用.  
状态形状的现状记录在 [docs/agents/architecture.md](../../../../agents-cn/04-架构.md) 的  
BaseListViewModel 一节, 已同步改写.  
只给那一行加 `remember(models)` 是把复制挪进缓存: 转换点仍在 UI 层, `models` 真更新时仍要复制一次,  
可变 `List` 仍然跨过 ViewModel 边界, 所以不走那条.

**同型不在本条内**: `ui/settings/SettingsScreen.kt` 另有四处同型写法 (两个对话框的选项数组在函数体内
无缓存地转换), 不属于本条的修复范围, 本条也不记录它们的处置.
