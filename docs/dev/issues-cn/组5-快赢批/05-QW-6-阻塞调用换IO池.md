<a id="qw-6"></a>

# QW-6 阻塞调用跑在 CPU 线程池 (AR-12① 阻塞调用跑在 CPU 线程池)

> 返回 [README 索引](../README.md) · [组5-快赢批](../README.md#目录).


**改动量: 8 处, 每处一词 | 文件: `HomeViewModel.kt`, `OthersViewModel.kt`, `PropViewModel.kt`, `SettingsRepository.kt`**

## 问题代码

阻塞式 Shell / 属性 / packageManager 查询被包在 `Dispatchers.Default`(给纯计算用的池) 里:

```kotlin
// HomeViewModel.detect() 里的两处 withContext(Dispatchers.Default) — 20+ 个探针内部是 Shell.cmd().exec(), /proc 读取, 反射
withContext(Dispatchers.Default) {
    tempModels += homeRepository.detectAndroid(lld)
    ...
}

// HomeViewModel.payloadOutdatedTargetSdkVersionApk() 与 SettingsRepository.getBuiltInDataVersion() 里的三处 withContext — packageManager 查询是跨进程 binder 调用
// OthersViewModel.collectModels(), PropViewModel.collectModels() — collectModels 内含 shell 调用
```

## 引入提交

| 日期 | 提交信息 | 当时目的 |
|---|---|---|
| 2020-01-20 | Refactor with MVVM using coroutine (WIP, see TODOs) | MVVM 改造起 Others / Prop / Settings 就这么写, 一路沿用 (推断) |
| 2025-12-03 | Feat: Improve exception handling via UDF | 首页检测编排成块时随手选了 Default(推断: 觉得"比 Main 安全") |

## 修改后

8 处 `Dispatchers.Default` 全部改为 `Dispatchers.IO`, 其余一字不动. `HomeViewModel.detect()` 里读 mounts 的那处内层已经是 `withContext(Dispatchers.IO)`, 保留 (无害).

**性质与边界**: 这是**临时缓解**, 不是 AR-12(阻塞调用跑在 CPU 线程池, 每次请求新建 HttpClient) 的完整修法 — ViewModel 仍然知道"里面有阻塞调用"这个细节, 完整方案是把 IO 声明下沉到各 DataSource 方法内部, 本条为下沉铺路. 功能行为 (加载顺序, 结果, 异常路径) 等价; 线程池参数不同 (Default 约为 CPU 核数个线程, IO 池按需扩到 64 线程), 高并发下的排队细节会变 — 本项目这几个调用点都是单次顺序加载, 无感知.

## 直接原因

Shell 命令, `/proc` 读取, binder 查询都是阻塞 IO, 占住 CPU 池的工作线程; 协程的约定是 Default 给计算, IO 给阻塞. 当前并发量小没出事, 但每个探针都在错误的池上排队.

## 根本原因

调度器的选择是随手写的 (`Default` 听起来比 `Main` 安全); 调度知识放在调用方而不是拥有阻塞实现的 DataSource 一侧, 新调用点还会重复犯.

---


