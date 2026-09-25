# UDF · 单向数据流 (Unidirectional Data Flow)

> 返回 [README](README.md). 数据从数据层单向流向界面, 用户的操作从界面交回数据层处理; 界面不自己改数据.

<a id="AR-08"></a>

## AR-08 HomeViewModel 手工编排 20+ 个仓库方法

**严重程度: P1 | 修复难度: 中**
**影响文件: `HomeViewModel.kt`, `HomeRepository.kt`, `BaseListViewModel.kt`**

### 问题核心代码

首页条目的**顺序**这份 "数据知识" 硬编码在 ViewModel 里 (`HomeViewModel.collectModels()`):

```kotlin
private suspend fun detect(lld: Lld?, errorMessage: List<String?>, modeResId: Int): List<MyModel> {
    val tempModels = mutableListOf<MyModel>()
    withContext(Dispatchers.Default) {
        tempModels += homeRepository.detectMode(lld, errorMessage, modeResId)
    }
    withContext(Dispatchers.Default) {
        tempModels += homeRepository.detectAndroid(lld)
        tempModels += homeRepository.detectSdkExtension(lld)
        tempModels += homeRepository.detectBuildId(lld)
        // ... 共 23 行 tempModels += ..., 逐条列出
    }
    return tempModels
}
```

同时数据更新走**下标寻址**: `BaseListViewModel.updateModelDetail(targetIndex: Int, ...)` (`BaseListViewModel.updateModelDetail()`) 是公开 API, 调用方要先自己算下标 (`HomeViewModel.payloadOutdatedTargetSdkVersionApk()`):

```kotlin
val targetIndex = list.indexOfFirst { it.type == OutdatedTargetSdkApk }
if (targetIndex == -1) { return }
updateModelDetail(targetIndex, newDetail)   // ← VM 翻自己的状态找位置, 再交给基类按下标改
```

### 直接原因

"首页有哪些条目, 什么顺序" 是仓库 (数据) 的知识, 却由 ViewModel 逐行手抄; `updateModelDetail` 暴露下标参数, 把 "如何定位一个条目" 的内部表示 (List 下标) 泄漏给了子类, 条目顺序一变, 并发一插队, 下标就对不上了. 更深一层: 条目身份同时存在三套标识 — DiffUtil 用 `MyModel.key` (标题资源 ID/文案), 补丁用 `type` 枚举, 定位用列表下标, 同一个东西有三套不同的标识.

### 根本原因

基类 API 按实现细节 (下标) 而非语义 (哪个条目) 设计; 编排逻辑没有归位到拥有列表知识的一侧. 两处合起来, 加一个首页条目要同时改 ViewModel(加一行编排) 和 Repository(加一个方法), 且顺序知识从此有两份可能的真相 (仓库方法注释顺序 vs VM 编排顺序).

### 修复方案

编排收回仓库, 更新按类型寻址:

```kotlin
// HomeRepository — 顺序知识只有一份
suspend fun collectHomeModels(lld: Lld?, errors: List<String?>, mode: LldSource): List<MyModel> =
    withContext(Dispatchers.Default) {
        buildList {
            add(detectMode(lld, errors, mode))
            add(detectAndroid(lld))
            add(detectSdkExtension(lld))
            addAll(detectSecurityPatches(lld))
            // ... 顺序在此维护
        }
    }
```

```kotlin
// HomeViewModel — 只剩一句话
override suspend fun collectModels(): List<MyModel> {
    // ... 联网/离线探测逻辑不变 (AR-02 改造后设置从注入的仓库读)
    return homeRepository.collectHomeModels(lld, errorMessages, mode)
}
```

```kotlin
// BaseListViewModel — 下标成为私有细节, 公开 API 按类型寻址
@MainThread
fun updateModelDetail(type: MyModelType, newDetail: String) {
    modelsStateFlow.update { state ->
        if (state !is State.Done) return@update state
        val index = state.value.indexOfFirst { it.type == type }
        if (index == -1) state
        else {
            val newList = state.value.toMutableList()
            newList[index] = newList[index].copy(detail = newDetail)
            State.Done(newList)
        }
    }
}
```

```kotlin
// HomeViewModel.payloadOutdatedTargetSdkVersionApk — 状态翻找代码全部消失
updateModelDetail(MyModelType.OutdatedTargetSdkApk, newDetail)
```

`LldSource` 枚举见 [AR-11](04-反模式与隐患.md#AR-11) (顺带消掉 `modeResId` 的资源 ID 判别).[AR-01](01-架构.md#AR-01) 全部做完之后还可以更进一步: 把每个检测项做成独立的 `Detector`, 顺序由一个列表持有 — 增删条目只改列表一处.

---

返回 [README](README.md)
