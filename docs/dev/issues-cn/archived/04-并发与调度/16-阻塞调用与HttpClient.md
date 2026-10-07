<a id="i16"></a>

# 16 阻塞调用跑在 CPU 线程池, 每次请求新建 HttpClient

> 返回 [README 索引](../../README.md) · [4 · 并发与调度](../../README.md#4--并发与调度).

**严重程度: P2 | 修复难度: 低**
**影响文件: `ui/home/HomeViewModel.kt`, `ui/others/OthersViewModel.kt`, `ui/prop/PropViewModel.kt`, `ui/settings/repository/SettingsRepository.kt`, `ui/home/datasource/MountDataSource.kt`, `ui/home/datasource/LldDataSource.kt`**

## 问题核心代码

### 阻塞调用包在 `Dispatchers.Default` 里 (全仓正好 8 处)

| 位置 | 块数 | 里面是什么 |
|---|---|---|
| `HomeViewModel.detect()` | 2 | shell / `getprop` / packageManager 探针, 23 个 `detectXxx()` 调用 |
| `HomeViewModel.payloadOutdatedTargetSdkVersionApk()` | 1 | 同上 (`getOutdatedTargetSdkVersionApkModel()`) |
| `OthersViewModel.collectModels()` | 1 | shell / `getprop` / binder 探针 |
| `PropViewModel.collectModels()` | 1 | `getprop`, `Settings` 逐 key 查询 (见 [#30](../../06-性能/30-Prop页逐key查询.md)) |
| `SettingsRepository.getBuiltInDataVersion()` | 3 | packageManager 签名 / 安装者 / 安装时间查询, 跨进程 binder |

```kotlin
withContext(Dispatchers.Default) {              // Default 是给纯计算用的
    tempModels += homeRepository.detectAndroid(lld)   // 内部是 Shell.cmd().exec() 阻塞等待
    tempModels += homeRepository.detectSar(mounts)     // mounts 来自 cat /proc/mounts 阻塞读取
    // ...
}
```

`/proc/mounts` 那一处已经在 IO 上: `HomeViewModel.detect()` 里 `homeRepository.getMounts()` 单独包了 `withContext(Dispatchers.IO)`.

### 每次请求从零新建 `HttpClient`

`LldDataSource.fetchOnlineLldJsonStringOrThrow()` 是全仓唯一的 `HttpClient(` 构造点, 每次调用新建, 用完 `client.use{}` 关掉:

```kotlin
suspend fun fetchOnlineLldJsonStringOrThrow(): String {
    ...
    val client = HttpClient(OkHttp) {          // ← 每次调用新建: 连接池, 调度器全部重造
        engine { config { eventListener(...); proxySelector(...) } }
        if (BuildConfig.DEBUG) { install(Logging) { ... } }
    }
    val url = "https://$urlPrefixLldJson/${BuildConfig.GIT_BRANCH}/app/src/main/assets/$LLD_JSON_NAME"
    val response: HttpResponse = client.get(url) { headers { append(HEADER_REFERER_KEY, HEADER_REFERER_VALUE) } }
    return client.use {                        // 不查状态码, 404/500 的错误页照样当 body 消费
        response.body()
    }
}
```

## 直接原因

- `Shell.cmd().exec()`, `/proc/mounts` 读取, 反射 `SystemProperties` (`PropertyDefault`), packageManager 查询都是**阻塞 IO**; `Dispatchers.Default` 的线程按 CPU 核数分配, 阻塞调用在里面是空等而不是工作. 协程的约定是 Default 给计算, IO 给阻塞 — AGENTS.md 也把它写成了项目规则 ("阻塞工作 (shell, 系统属性, 文件, 网络) 走 `Dispatchers.IO`"), 所以**错的是代码, 不是约定**. 当前没出事是因为并发量小 (每个页面都是单次顺序加载, 见 [#15](../../04-并发与调度/15-检测串行不感知取消.md)), 但 8 个点都在错误的池上排队.
- `HttpClient` 的构建成本 (OkHttp 引擎的连接池, 调度器) 按设计是进程级复用的; 每次新建等于每次丢弃全部 keep-alive 与连接复用, 而错误状态码的响应体被当作 JSON 消费, 错误归因失真.

## 根本原因

调度器的选择是随手写的 (`Default` 听起来比 `Main` 安全), 调度知识放在调用方而不是拥有阻塞实现的 DataSource 一侧; 客户端生命周期没有归属对象 (没有容器, 见 [#02](../../01-架构与分层/02-服务定位器上帝对象.md)), 只能每次在函数里局部创建.

## 修复方案

### 缓解: 8 处 `Dispatchers.Default` 改 `Dispatchers.IO`

改动量 8 处, 每处一词, 其余一字不动. `HomeViewModel.detect()` 里读 mounts 的那处内层已经是 `withContext(Dispatchers.IO)`, 保留. 这一步就是同目录 [#14](../../04-并发与调度/14-检测链用CPU线程池.md) 的修法 — 那条记的是检测链的池选择与本条这 8 处调用点, 两条一起动.

- **性质与边界**: 这是**临时缓解**, 不是完整修法 — ViewModel 仍然知道 "里面有阻塞调用" 这个细节, 完整方案是把 IO 声明下沉到各 DataSource 方法内部, 本步为下沉铺路.
- **行为等价性**: 加载顺序, 结果, 异常路径不变. 线程池参数不同 (Default 按 CPU 核数给线程, IO 池是为阻塞设计的可扩容池), 高并发下的排队细节会变; 本项目这几个调用点都是单次顺序加载, 无感知.
- **来历**: 这 8 处不是一次写成的 — `5f8fc45f` (仓库化重构) 起了 `SettingsRepository` 的签名与安装者两处, `1266cb6f` 定了首页 `detect()` 的两处与 Others / Prop 各一处, 剩下的 `payloadOutdatedTargetSdkVersionApk()` 与安装时间两处由 `1e66c6f8` 与 `c0ef562e` 补上. 至于 "阻塞活儿丢给 `Dispatchers.Default`" 这个习惯, 始于 `e7a4be34` (MVVM + 协程改造) — 当时 `BaseListViewModel` 与 `SettingsViewModel` 的加载就跑在 Default 上.

### 完整修法: 阻塞声明下沉到 DataSource

```kotlin
// ui/home/datasource/MountDataSource.kt — 现状: 该类没有注入依赖, 走全局 getShellResult (#02)
class MountDataSource {
    suspend fun getMounts(): List<Mount> = withContext(Dispatchers.IO) {
        // 现有解析逻辑原样搬进来: getShellResult(CMD_MOUNT).output 逐行按 6 列切
    }
}
// HomeViewModel.detect() 里的 withContext(Dispatchers.Default) 包装随之删除
// (#01 / #11 改造后若仓库内还有纯计算, Default 才有保留价值)
```

### HttpClient 复用 + 状态码闸门

改动量约 18 行, 文件 `ui/home/datasource/LldDataSource.kt`.

```kotlin
class LldDataSource {
    companion object {
        // ... 原有常量不动 ...
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
        // ... urlPrefixLldJson 的时区判断不动 ...
        val url = "https://$urlPrefixLldJson/${BuildConfig.GIT_BRANCH}/app/src/main/assets/$LLD_JSON_NAME"
        val response: HttpResponse = client.get(url) {
            headers { append(HEADER_REFERER_KEY, HEADER_REFERER_VALUE) }
        }
        if (!response.status.isSuccess()) {
            response.discard()      // 先释放未消费的响应体, 连接才能归还连接池供复用
            throw IOException("HTTP ${response.status.value}")
        }
        return response.body()      // 不再 use{} — client 不属于本次请求
    }
}
```

需新增 `io.ktor.http.isSuccess` 的 import (`java.io.IOException` 文件里已有); Ktor 钉在 `gradle/toml/kotlin.toml` 的 **3.6.0**, 动手时按该版本核对 `discard()` 的 import. TrafficStats 的 `EventListener` 随连接触发, 客户端复用后行为不变. 异常路径走既有的 "联网失败 → 降级离线" 分支 (`HomeViewModel.tryDetectOnline()`), 降级文案从 "解析失败" 变成 "获取失败", 归因更准. 状态码这道闸门与 [#24](../../05-稳定性与错误处理/24-杂项隐患.md) 的 "Ktor 不校验 HTTP 状态码" 是同一次改动.

**来历**: "一次请求一个客户端" 的写法随 Ktor 迁移带入 — `db3e5ea2` 与 `062dfe86` 改的是当时的 `ui/home/GatewayApi.kt` (该文件后改名 `LldDataSource.kt`), `ede6fa52` 是纯格式化提交, 顺带重排了 `response.body()` 的缩进.
