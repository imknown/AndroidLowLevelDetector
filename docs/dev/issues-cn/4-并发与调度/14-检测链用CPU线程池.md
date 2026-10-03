<a id="i14"></a>

# 14 检测链在 CPU 线程池上跑阻塞调用

> 返回 [README 索引](../README.md) · [4 · 并发与调度](../README.md#4--并发与调度).

**严重程度: P1 | 修复难度: 低**

**影响文件**: `ui/home/HomeViewModel.kt`, `ui/others/OthersViewModel.kt`, `ui/prop/PropViewModel.kt`, `ui/settings/repository/SettingsRepository.kt`

## 问题核心代码

三个列表页的检测序列整体包在 `Dispatchers.Default` 里, 而序列里的每一步都是阻塞调用 — shell, `getprop`, `Settings.Secure` 查询:

```kotlin
// HomeViewModel.collectModels() — 23 个 detect 调用都在 Default 上
val tempModels = withContext(Dispatchers.Default) {
    val tempModels = mutableListOf<MyModel>()
    ...
    tempModels += homeRepository.detectAndroid()      // 内含 shell 与 getprop
    ...
}

// OthersViewModel.collectModels() / PropViewModel.collectModels() 同一写法
withContext(Dispatchers.Default) { ... }              // OthersViewModel.kt
withContext(Dispatchers.Default) { ... }              // PropViewModel.kt

// SettingsRepository.getSettingsModel() 里三处 SP 读也在 Default
```

只有个别点例外: `HomeViewModel` 读 `/proc/mounts` 与 lld.json 落盘时单独套了 `withContext(Dispatchers.IO)`.

## 直接原因

`Dispatchers.Default` 的线程数按 CPU 核数上限分配, 为计算密集工作设计. 阻塞调用 (等 shell 进程, 等 binder, 等磁盘) 在该池里是**空等而不是工作**, 既占着一个计算线程, 又不会随等待时间扩容.

## 根本原因

迁移时只解决了 "别在主线程干活", 没有区分**计算**与**阻塞**. AGENTS.md 已把 "阻塞工作 (shell, 系统属性, 文件, 网络) 走 `Dispatchers.IO`" 写成约定, 检测链是这个约定最大的未跟上的地方 — 与 [#16](16-阻塞调用跑在CPU线程池.md) 是同一件事的两半 (#16 数出的是另外几处漏掉的调用点).

## 修复方案

1. 三个 ViewModel 的 `collectModels()` 外层 `withContext(Dispatchers.Default)` 改为 `withContext(Dispatchers.IO)`; `SettingsRepository` 的三处 SP 读同样改 IO.
2. 纯计算的部分 (字符串比较, 解析, 排序) 不需要单独切回 Default — 它跟在 IO 块里不会有可测的代价, 真要分家等 [#03](../1-架构与分层/03-无三态模型与注册表.md) 的注册表引擎按探针特征分派.
3. 与 [#15](15-检测串行不感知取消.md) 同属检测执行模型, 但相互独立: #15 讲的是并发度与取消, 本条讲的是池的选择. 两条一起动最省 — 注册表落地时按探针类型分派即可一并解决.
