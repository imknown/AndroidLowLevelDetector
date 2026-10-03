<a id="i16"></a>

# 16 阻塞调用跑在 CPU 线程池

> 返回 [README 索引](../README.md) · [4 · 并发与调度](../README.md#4--并发与调度).

**严重程度: P2 | 修复难度: 低**
**影响文件: `ui/home/HomeViewModel.kt`, `ui/others/OthersViewModel.kt`, `ui/prop/PropViewModel.kt`, `ui/settings/repository/SettingsRepository.kt`**

> 原标题 "阻塞调用跑在 CPU 线程池, 每次请求新建 HttpClient" 的后半 (每次请求新建 `HttpClient` + `client.use{}` 关闭) 已随 DI 接入关闭: 客户端收成图内 `@SingleIn` 绑定, 经 `Provider` 惰性解析, `use{}` 关闭随之消失, 引擎配置原样上移 — 明细见 git log. 当时并排的 "HTTP 状态码闸门" 没有一起加, 仍归 [#24](../5-稳定性与错误处理/24-杂项隐患.md).

## 问题核心代码

### 阻塞调用包在 `Dispatchers.Default` 里 (全仓正好 8 处)

| 位置 | 块数 | 里面是什么 |
|---|---|---|
| `HomeViewModel.detect()` | 2 | shell / `getprop` / packageManager 探针, 23 个 `detectXxx()` 调用 |
| `HomeViewModel.payloadOutdatedTargetSdkVersionApk()` | 1 | 同上 (`getOutdatedTargetSdkVersionApkModel()`) |
| `OthersViewModel.collectModels()` | 1 | shell / `getprop` / binder 探针 |
| `PropViewModel.collectModels()` | 1 | `getprop`, `Settings` 逐 key 查询 (见 [#30](../6-性能/30-Prop页逐key查询.md)) |
| `SettingsRepository.getBuiltInDataVersion()` | 3 | packageManager 签名 / 安装者 / 安装时间查询, 跨进程 binder |

```kotlin
withContext(Dispatchers.Default) {              // Default 是给纯计算用的
    tempModels += homeRepository.detectAndroid(lld)   // 内部是 Shell.cmd().exec() 阻塞等待
    tempModels += homeRepository.detectSar(mounts)     // mounts 来自 cat /proc/mounts 阻塞读取
    // ...
}
```

`/proc/mounts` 那一处已经在 IO 上: `HomeViewModel.detect()` 里 `homeRepository.getMounts()` 单独包了 `withContext(Dispatchers.IO)`.

## 直接原因

`Shell.cmd().exec()`, `/proc/mounts` 读取, 反射 `SystemProperties` (`PropertyDefault`), packageManager 查询都是**阻塞 IO**; `Dispatchers.Default` 的线程按 CPU 核数分配, 阻塞调用在里面是空等而不是工作. 协程的约定是 Default 给计算, IO 给阻塞 — AGENTS.md 也把它写成了项目规则 ("阻塞工作 (shell, 系统属性, 文件, 网络) 走 `Dispatchers.IO`"), 所以**错的是代码, 不是约定**. 当前没出事是因为并发量小 (每个页面都是单次顺序加载, 见 [#15](15-检测串行不感知取消.md)), 但 8 个点都在错误的池上排队.

## 根本原因

调度器的选择是随手写的 (`Default` 听起来比 `Main` 安全), 调度知识放在调用方而不是拥有阻塞实现的 DataSource 一侧.

## 修复方案

### 缓解: 8 处 `Dispatchers.Default` 改 `Dispatchers.IO`

改动量 8 处, 每处一词, 其余一字不动. `HomeViewModel.detect()` 里读 mounts 的那处内层已经是 `withContext(Dispatchers.IO)`, 保留. 这一步就是同目录 [#14](14-检测链用CPU线程池.md) 的修法 — 那条记的是检测链的池选择与本条这 8 处调用点, 两条一起动.

- **性质与边界**: 这是**临时缓解**, 不是完整修法 — ViewModel 仍然知道 "里面有阻塞调用" 这个细节, 完整方案是把 IO 声明下沉到各 DataSource 方法内部, 本步为下沉铺路.
- **行为等价性**: 加载顺序, 结果, 异常路径不变. 线程池参数不同 (Default 按 CPU 核数给线程, IO 池是为阻塞设计的可扩容池), 高并发下的排队细节会变; 本项目这几个调用点都是单次顺序加载, 无感知.
- **来历**: 这 8 处不是一次写成的 — `5f8fc45f` (仓库化重构) 起了 `SettingsRepository` 的签名与安装者两处, `1266cb6f` 定了首页 `detect()` 的两处与 Others / Prop 各一处, 剩下的 `payloadOutdatedTargetSdkVersionApk()` 与安装时间两处由 `1e66c6f8` 与 `c0ef562e` 补上. 至于 "阻塞活儿丢给 `Dispatchers.Default`" 这个习惯, 始于 `e7a4be34` (MVVM + 协程改造) — 当时 `BaseListViewModel` 与 `SettingsViewModel` 的加载就跑在 Default 上.

### 完整修法: 阻塞声明下沉到 DataSource

```kotlin
// ui/home/datasource/MountDataSource.kt — 现状: shell 已是注入的 IShell (见 [#02])
class MountDataSource(private val shell: IShell) {
    suspend fun getMounts(): List<Mount> = withContext(Dispatchers.IO) {
        // 现有解析逻辑原样搬进来: shell.execute(CMD_MOUNT).output 逐行按 6 列切
    }
}
// HomeViewModel.detect() 里的 withContext(Dispatchers.Default) 包装随之删除
// (#01 / #11 改造后若仓库内还有纯计算, Default 才有保留价值)
```
