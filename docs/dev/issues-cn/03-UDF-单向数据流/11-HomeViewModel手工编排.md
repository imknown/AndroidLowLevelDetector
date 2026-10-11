<a id="i11"></a>

# 11 HomeViewModel 手工编排 23 个仓库方法

> 返回 [README 索引](../README.md) · [3 · UDF · 单向数据流](../README.md#3--udf--单向数据流).

**严重程度: P1 | 修复难度: 中**  
**影响文件: `ui/home/HomeViewModel.kt`, `ui/home/repository/HomeRepository.kt`,  
`ui/base/list/BaseListViewModel.kt`**

## 问题核心代码

首页条目的**顺序**这份 "数据知识" 硬编码在 ViewModel 里:  
`HomeViewModel.collectModels()` 经 `tryDetectOnline()` / `tryDetectOffline()` 落到 `HomeViewModel.detect()`,  
后者逐行手工累加, 全函数共 **23 行** `tempModels += homeRepository.detectXxx(...)` (23 个调用,  
但 `detectSecurityPatches()` 与  
`detectTrebleAndGsiCompatibility()` 各返回 2 个模型):

```kotlin
private suspend fun detect(
    lld: Lld?, errorMessage: List<String?>, @StringRes modeResId: Int
): List<MyModel> {
    val tempModels = mutableListOf<MyModel>()

    withContext(Dispatchers.Default) {
        tempModels += homeRepository.detectMode(lld, errorMessage, modeResId)
    }

    withContext(Dispatchers.Default) {
        tempModels += homeRepository.detectAndroid(lld)
        tempModels += homeRepository.detectSdkExtension(lld)
        tempModels += homeRepository.detectBuildId(lld)
        tempModels += homeRepository.detectSecurityPatches(lld)
        // ... 此块 22 行, 连同上一块的 1 行共 23 行, 逐条列出
    }

    return tempModels
}
```

同时数据更新走**下标寻址**: `BaseListViewModel.updateModelDetail(targetIndex: Int, newDetail: String)` 是公开 API,  
调用方要先自己算下标 (`HomeViewModel.payloadOutdatedTargetSdkVersionApk()`):

```kotlin
val list = modelsStateFlow.value ?: return
val targetIndex = list.indexOfFirst {
    it.type == OutdatedTargetSdkApk
}
if (targetIndex == -1) {
    return
}

updateModelDetail(targetIndex, newDetail)   // ViewModel 翻自己的状态找位置, 再交给基类按下标改
```

同一条目还有第二个生产者, 与加载路径对 `lld == null` 的处置相反:  
加载路径里 `detect()` 接受 `lld == null` (渲染成含 "未知" 的行),  
补偿路径 `payloadOutdatedTargetSdkVersionApk()` 却是 `fetchOfflineLldOrNull().lld ?: return`,  
离线兜底链整体失败时,  
排序补丁静默放弃, 没有任何错误行说明为什么. 补偿路径还固定重读**离线**文件 (而刚才那次加载可能用的是在线数据),  
且这个名为 payload 的动作里做磁盘写 (`copyJsonIfNeededOrThrow()`). 编排收进仓库时这两条路径要一起归位.

## 直接原因

"首页有哪些条目, 什么顺序" 是仓库 (数据) 的知识, 却由 ViewModel 逐行手抄; `updateModelDetail` 暴露下标参数,  
把 "如何定位一个条目" 的内部表示 (List 下标) 泄漏给了子类: 下标取自一次 `modelsStateFlow.value` 快照,  
`updateModelDetail()` 改的却是它被调用那一刻的最新列表, 中间若另一次加载落地, 补丁就打到了别的条目上.

更深一层: 条目身份同时存在三套标识:  
`MyModelListScreen` 的 `LazyColumn` 按 `key = { it.key }` 认条目 (值是 `MyModel.key`,  
即标题资源 ID 的字符串形式或原始标题文案),  
补丁按 `MyModelType` 枚举匹配, 定位按列表下标. 同一个东西有三套不同的标识.

## 根本原因

基类 API 按实现细节 (下标) 而非语义 (哪个条目) 设计; 编排逻辑没有归位到拥有列表知识的一侧. 两处合起来,  
加一个首页条目要同时改 ViewModel (加一行编排) 和 Repository (加一个方法),  
且顺序知识从此有两份可能的真相 (仓库方法顺序 vs ViewModel 编排顺序).

## 修复方案

**动手前先与负责人确认约定**: 把编排整体移进 `HomeRepository.collectHomeModels()` 与 AGENTS.md 相反:  
该文件 "Adding a detection item" 一节写明检测项由 `ViewModel.collectModels()` 调用,  
**调用顺序就是列表顺序**. 下面的仓库化仍是目标形态,  
但采纳它等于同时改这条约定, 不是可以直接自行落地的事.

**与修复路线的关系**: [README 修复路线](../README.md#修复路线) 把本条目拆成两半排期:  
"更新按类型寻址" 一半 (`updateModelDetail` 改按类型 + 补偿路径归位) 不碰该约定, 列入独立小件先行;  
"编排收回仓库" 一半在走完整条主线时并入注册表引擎那一步 ([#03](../01-架构与分层/03-无三态模型与注册表.md)),  
不单独建 `collectHomeModels()` 过渡形态,  
下面的落点仅在注册表长期搁置时作为独立路径使用.

**迁移清单再添一项 (UI 不 try 约定, 见 AGENTS.md 代码规则)**:  
lld 联网→离线的回退链  
(`tryDetectOnline()` / `tryDetectOffline()` / `fetchOfflineLldOrNull()` / `getAssetLldOrNull()`) 目前在  
`HomeViewModel` 里用 try 编排失败回退, 违反 "UI 层不处理业务异常" 的约定, 随 "编排收回仓库" 一半一并归位:  
取数与回退下沉, 或返回 wrapper 由 mapper 呈现.

编排收回仓库, 更新按类型寻址:

```kotlin
// HomeRepository: 顺序知识只有一份
suspend fun collectHomeModels(
    lld: Lld?, errors: List<String?>, modeResId: Int
): List<MyModel> =
    withContext(Dispatchers.IO) {        // 阻塞探针走 IO 池, 见 #14
        buildList {
            add(detectMode(lld, errors, modeResId))
            add(detectAndroid(lld))
            add(detectSdkExtension(lld))
            addAll(detectSecurityPatches(lld))
            // ... 顺序在此维护
        }
    }
```

```kotlin
// HomeViewModel: 只剩一句话
override suspend fun collectModels(): List<MyModel> {
    // ... 联网/离线探测逻辑不变 (设置经注入的 SettingsStore 读取, 已落地, 原条目见 archived #08)
    return homeRepository.collectHomeModels(lld, errorMessages, modeResId)
}
```

```kotlin
// BaseListViewModel: 下标成为私有细节, 公开 API 按类型寻址.
// 状态按现状的形状写: modelsStateFlow: StateFlow<List<MyModel>?> (null = 还没加载过),
// 加载标志另在 isLoadingStateFlow, 不再包一层 State
@MainThread
fun updateModelDetail(type: MyModelType, newDetail: String) {
    modelsStateFlow.update { list ->
        if (list == null) return@update list
        val index = list.indexOfFirst { it.type == type }
        if (index == -1) return@update list
        list.toMutableList().also { it[index] = it[index].copy(detail = newDetail) }
    }
}
```

```kotlin
// HomeViewModel.payloadOutdatedTargetSdkVersionApk: 状态翻找代码全部消失
updateModelDetail(MyModelType.OutdatedTargetSdkApk, newDetail)
```

`modeResId` 换成 `LldSource` 枚举见 [#44](../08-反模式与卫生/44-资源ID当逻辑值.md) (顺带消掉资源 ID 判别).  
[#01](../01-架构与分层/01-无领域模型.md) 全部做完之后还可以更进一步: 把每个检测项做成独立的 `Detector`,  
顺序由一个列表持有: 增删条目只改列表一处,  
即 [#03](../01-架构与分层/03-无三态模型与注册表.md) 的注册表引擎.
