<a id="AR-12"></a>

# AR-12 阻塞调用跑在 CPU 线程池, 每次请求新建 HttpClient

> 返回 [README 索引](../README.md) · [组3 · 暂缓](../README.md#组3--暂缓-原因见危害列标注).


**严重程度: P2 | 修复难度: 低**
**影响文件: `HomeViewModel.kt`, `MountDataSource.kt`, `LldDataSource.kt`**

## 问题核心代码

阻塞式 Shell / 属性调用被包在 `Dispatchers.Default` (CPU 专用池) 里执行 (`HomeViewModel.detect()` 与 `HomeViewModel.payloadOutdatedTargetSdkVersionApk()` 里的三处 `withContext(Dispatchers.Default)`):

```kotlin
withContext(Dispatchers.Default) {              // Default 是给纯计算用的
    tempModels += homeRepository.detectAndroid(lld)   // 内部是 Shell.cmd().exec() 阻塞等待
    tempModels += homeRepository.detectSar()           // 内部是 cat /proc/mounts 阻塞读取
    ...
}
```

网络请求每次从零构建一个 `HttpClient` (`LldDataSource.fetchOnlineLldJsonStringOrThrow()`):

```kotlin
suspend fun fetchOnlineLldJsonStringOrThrow(): String {
    ...
    val client = HttpClient(OkHttp) {          // ← 每次调用新建: 连接池, 线程池全部重造
        engine { config { ... } }
        ...
    }
    val response: HttpResponse = client.get(url) { ... }
    return client.use { response.body() }      // 用完即弃
}
```

## 直接原因

- `Shell.cmd().exec()`, `/proc/mounts` 读取, 反射 `SystemProperties` 都是**阻塞 IO**, 放 `Default` 池会占用 CPU 工作线程; 协程官方约定: Default 给计算, IO 给阻塞. 当前没出事是因为并发量小, 但每个 `detect*` 都在错误池上排队.
- `HttpClient` 的构建成本 (OkHttp 引擎的连接池, 调度器) 按设计是进程级复用的; 每次新建等于每次丢弃所有连接复用与 keep-alive.

## 根本原因

调度器的选择是随手写的 (`Default` 听起来比 `Main` 安全), 客户端生命周期没有归属对象 (没有容器, 见 [AR-04](../组2-主线重构/06-AR-04-服务定位器上帝对象.md), 只能每次局部创建).

## 修复方案

```kotlin
// ① 阻塞 IO 一律 IO 池: 在各 DataSource 方法内部声明, 而不是靠调用方记得包
class MountDataSource(private val shell: IShell) {
    suspend fun getMounts(): List<Mount> = withContext(Dispatchers.IO) {
        shell.execute(CMD_MOUNT).output.mapNotNull { it.toMountOrNull() }
    }
}
// HomeViewModel.detect 里的 withContext(Dispatchers.Default) 包装随之删除
// (若 AR-01/AR-08 改造后仓库内还有纯计算, Default 才有保留价值)
```

```kotlin
// ② HttpClient 复用: 类级单例, 进程存活期内共享
class LldDataSource(private val store: LldFileStore) {
    companion object {
        private val client: HttpClient by lazy { buildHttpClient() }   // 构建逻辑原样搬入
    }

    suspend fun fetchOnlineLldJsonStringOrThrow(): String {
        val url = ...
        val response = client.get(url) { headers { append(HEADER_REFERER_KEY, HEADER_REFERER_VALUE) } }
        return response.body()        // 不再 use{} — client 不属于本次请求
    }
}
```

---

<a id="qw-3"></a>

## 快赢批配方 · QW-3 每次联网新建 HttpClient, 不检查 HTTP 状态码 (AR-12② 每次请求新建 HttpClient + AR-18.3 Ktor 不校验 HTTP 状态码) (整体暂缓)

> 配方原为快赢批独立文件, 2026-09-26 并入本宿主条目; 批次整体暂缓 (2026-09-16), 状态与总表见 [README 快赢批节](#qw).

**改动量: ~18 行 | 文件: `app/.../home/datasource/LldDataSource.kt`**

## 问题代码

```kotlin
// LldDataSource.fetchOnlineLldJsonStringOrThrow() — 每次调用新建, 用完即弃
val client = HttpClient(OkHttp) { engine { config { ... } } ... }

// LldDataSource.fetchOnlineLldJsonStringOrThrow() — 拿到响应直接读 body, 不查状态码
val response: HttpResponse = client.get(url) { headers { ... } }
return client.use {
    response.body()      // 404/500 的 HTML 错误页会被当 JSON 解析
}
```

## 引入提交

| 日期 | 提交信息 | 当时目的 |
|---|---|---|
| 2024-10-21 | TODO: Fuel → Ktor | 网络库从 Fuel 迁到 Ktor,"一次请求一个客户端"的写法照搬了 Fuel 习惯 (推断) |
| 2024-11-03 | Format | 纯格式化提交, 顺带重排了 body() 的位置 |

当时文件还叫 `ui/home/GatewayApi.kt`, 后随架构整理改名 `LldDataSource.kt`.

## 修改后

```kotlin
class LldDataSource {
    companion object {
        // ... 原有常量不动...
        private val client: HttpClient by lazy { buildClient() }

        private fun buildClient(): HttpClient = HttpClient(OkHttp) {
            engine {
                config {
                    // 原 eventListener(TrafficStats) 与 proxySelector 原样搬入
                }
            }
            if (BuildConfig.DEBUG) {
                install(Logging) { logger = Logger.ANDROID; level = LogLevel.ALL }
            }
        }
    }

    suspend fun fetchOnlineLldJsonStringOrThrow(): String {
        // ...urlPrefixLldJson 判断不动...
        val url = "https://$urlPrefixLldJson/${BuildConfig.GIT_BRANCH}/app/src/main/assets/$LLD_JSON_NAME"
        val response: HttpResponse = client.get(url) {
            headers { append(HEADER_REFERER_KEY, HEADER_REFERER_VALUE) }
        }
        if (!response.status.isSuccess()) {
            response.discard()      // 先释放未消费的响应体, 连接才能归还连接池供复用
            throw IOException("HTTP ${response.status.value}")
        }
        return response.body()      // 不再 client.use{} — client 不属于本次请求
    }
}
```

新增 import `io.ktor.http.isSuccess`, `io.ktor.client.statement.discard`(`java.io.IOException` 已有; Ktor 3.5.2 两者均有). TrafficStats 的 EventListener 随连接触发, 客户端复用后行为不变; 异常路径走既有的 "联网失败 → 降级离线" 分支, 降级文案从"解析失败"变成"获取失败", 归因更准.

## 直接原因

OkHttp 引擎的连接池, 调度器按设计是进程级复用的, 每次新建等于每次丢弃全部 keep-alive; 而 404/500 返回的错误页被当作响应体直接消费, 错误归因失真.

## 根本原因

客户端的生命周期没有归属对象 (没有容器, 见 AR-04 服务定位器满天飞, MyApplication 是上帝对象), 只能每次在函数里局部创建; 网络层也缺 "先看状态码, 再消费响应" 这道闸.

---

---

<a id="qw-6"></a>

## 快赢批配方 · QW-6 阻塞调用跑在 CPU 线程池 (AR-12① 阻塞调用跑在 CPU 线程池) (整体暂缓)

> 配方原为快赢批独立文件, 2026-09-26 并入本宿主条目; 批次整体暂缓 (2026-09-16), 状态与总表见 [README 快赢批节](#qw).

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
