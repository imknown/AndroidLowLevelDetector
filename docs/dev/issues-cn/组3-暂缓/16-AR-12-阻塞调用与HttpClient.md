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


