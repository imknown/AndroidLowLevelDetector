<a id="AR-09"></a>

# AR-09 State 没有错误态, 加载异常没有出口

> 返回 [README 索引](../README.md) · [组3 · 暂缓](../README.md#组3--暂缓-原因见危害列标注).


**严重程度: P2 | 修复难度: 中**
**影响文件: `BaseListViewModel.kt`, `MyModelListScreen.kt`, 各 ViewModel**

## 问题核心代码

状态只有两条流, 没有失败通道 (`BaseListViewModel.modelsStateFlow / isLoadingStateFlow`):

```kotlin
val modelsStateFlow: StateFlow<List<MyModel>?>   // null = 还没加载过任何东西
val isLoadingStateFlow: StateFlow<Boolean>
```

View 时代那套 `sealed interface State` (`Done` / `Loading` / `NotInitialized`) 在 Compose 迁移里被拆成了这两条流. 拆分本身是对的 (加载不再擦掉旧数据), 但**当年缺的 Error 分支照样没人补** — 异常在类型里依然无处可去.

加载不设防 (`BaseListViewModel.startLoad()`) — `collectModels()` 一旦抛出未捕获异常, 后果比"协程死亡"更严重 (见下方直接原因):

```kotlin
loadJob = viewModelScope.launch {
    setLoading(true)
    val list = collectModels()   // ← 无 runCatching, 异常一路穿透
    setModels(list)
    onModelsLoaded()
}
```

而界面把 "正在转圈" 绑在那条布尔流上 (`MyModelListScreen()` 的两条 `collectAsStateWithLifecycle` 与 `isRefreshing = isLoading`):

```kotlin
val isLoading by viewModel.isLoadingStateFlow.collectAsStateWithLifecycle()
// ...
isRefreshing = isLoading,   // 只有 true / false, 没有 "失败了" 这一档
```

## 直接原因

异常没有出口: `Loading` 之后要么 `Done` 要么什么都不是. `viewModelScope.launch` 里未捕获的异常**不会**只让协程默默结束 — 它会传给线程的未捕获异常处理器, 在 Android 上就是**应用崩溃**,[AR-15](02-AR-15-lld日期隐式契约.md) 就是现成的触发链; 只有当异常恰好被中途某处吞掉时, 界面才表现为刷新圈永远转下去. 当前各 ViewModel 用 try/catch 把绝大多数异常吞成了 "错误文案拼进条目详情", 暂时没炸; 但这是把错误信息当**数据**渲染 (DiffUtil 会把它当内容变更去对比), 而且将来任何一条新增的没保护的路径都会触发上面两种结局之一.

## 根本原因

两条流的建模只考虑了 "成功" 一条路. 错误处理策略 (吞进字符串) 与状态表示 (无错误通道) 互相迁就, 谁也不完整: 既没有结构化的失败表示, 条目数据又被错误文案污染.

## 修复方案

```kotlin
// BaseListViewModel — 异常成为一等公民
private val errorChannel = Channel<String>(Channel.BUFFERED)
val errorEvents = errorChannel.receiveAsFlow()

loadJob = viewModelScope.launch {
    setLoading(true)
    runCatching { collectModels() }
        .onSuccess { list ->
            setModels(list)
            onModelsLoaded()
        }
        .onFailure { cause ->
            if (BuildConfig.DEBUG) cause.printStackTrace()
            errorChannel.trySend(cause.fullMessage)   // 或只发资源 ID, 见 AR-19
            setLoading(false)                         // ← 关键: 圈必须停下
        }
}
```

一次性事件走 `Channel` + `receiveAsFlow()`, 不要再开一条 `StateFlow`: 错误是要被消费掉的通知, 不是可重放的状态; 而 `SharedFlow` (replay=0) 在没人订阅时直接把事件丢掉 — 本报告的 [F1](../组4-已裁定/01-F1-滚动条死总线.md) 就是现成的教训. 流挂在 ViewModel 实例上, 绝不放伴生对象 (那是 [AR-02](../组2-主线重构/01-AR-02-设置SSOT与死总线.md) 的坑).

```kotlin
// MyModelListScreen — 消费端: 提示 + 复用既有 refresh() 重试
val snackbarHostState = remember { SnackbarHostState() }
LaunchedEffect(viewModel) {
    viewModel.errorEvents.collect { snackbarHostState.showSnackbar(it) }
}
```

重试入口已经有了 (`onRefresh = viewModel::refresh`), 失败后下拉即重跑, 不必新造按钮; `receiveAsFlow` 只允许一个收集者, 正好对应 "一页一 ViewModel".

迁移说明: 现有 "错误拼进条目" 的产品表现 (如 LLD 拉取失败仍显示离线数据 + 失败原因) 不必推翻 — 那属于 "降级成功"; `errorEvents` 只兜底真正的整体失败. 新增 `load_failed` 字符串资源即可 (默认英文 + 三份翻译, 见[约定文档](../../conventions/README-cn.md)的本地化一节).

另注: 项目已有一个定下来的决定 (2026-09-05 逐条目错误处理任务, 记录没留下来, 结论并入 [C1](01-C1-逐条错误隔离未实现.md)) — 错误防护按 "逐条目 try/catch, 异常直接显示在该条目上" 实施, 顶层暂不加 catch(当时为让卡死修复保持最小); Others / Prop 的 `collectModels` 完全无防护也在该任务范围内. 本条的 `runCatching` 兜底与该决策不冲突: 逐条目隔离是第一道防线 (单项失败变成一条红色条目), 顶层兜底是第二道 (意外逃逸的异常不再变成崩溃). 建议实施顺序: 先逐条目, 后兜底.


