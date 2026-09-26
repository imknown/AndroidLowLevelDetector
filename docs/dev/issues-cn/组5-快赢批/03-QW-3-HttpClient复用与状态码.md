<a id="qw-3"></a>

# QW-3 每次联网新建 HttpClient, 不检查 HTTP 状态码 (AR-12② 每次请求新建 HttpClient + AR-18.3 Ktor 不校验 HTTP 状态码)

> 返回 [README 索引](../README.md) · [组5-快赢批](../README.md#目录).


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


