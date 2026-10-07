# ST-06 http-client-singleton: 修改计划报告

> 状态: living (预生成; 动工时与负责人预期对比, 必要时显式修订). 计划: [plan.md](plan.md) 子任务 06. 风险: 高 (生疏领域, 即首个 `@Provides`; 网络客户端生命周期语义变化).  
> 修订 (2026-10-02): 注入形式改为 `Provider<HttpClient>` 惰性单例, 实施后负责人提出生命周期问题并裁定方案 B (首次 fetch 才构造, 网络开关未开过则客户端零占用),  
> 与台账 progress.md 同记.

## 要改什么

- `AppGraph.kt`: 增 companion `@Provides @SingleIn(AppScope::class) fun httpClient(): HttpClient`,  
  OkHttp 引擎配置从 `LldDataSource.fetchOnlineLldJsonStringOrThrow()` **原样上移**  
  (TrafficStats `eventListener` + 自定义 `proxySelector` + debug `Logging`), 一行不改逻辑
- `LldDataSource.kt`: 构造注入 `Provider<HttpClient>` (惰性解析, 首次 fetch 调 `httpClient()` 才构造),  
  删除每次请求的 `HttpClient(OkHttp) {...}` 与 `client.use {}`

## 为什么

issues-cn #16 后半: 客户端没有归属对象, 每次请求 new 一整套 OkHttp 引擎再用完即弃; 收成图内 `@SingleIn` 后连接池进程级复用.

## 注意 (语义变化, 动工前向负责人确认)

- 现状 `client.use {}` 每请求关闭; 改为进程级单例后**不再关闭** (Metro 不负责 close, 文档明说), App 进程生命周期即客户端生命周期, 对本项目的单请求场景是净收益, 但属行为变化
- debug Logging 插件原样保留 (`BuildConfig.DEBUG` 条件)

## 覆盖核对

- 覆盖: 全仓唯一的 `HttpClient(` 构造点收归图绑定 (`LldDataSource` 不再自建)
- 不在本子任务: 请求超时 (归 #21), HTTP 状态码闸门 (归 #24), 阻塞调用调度 (归 #14 与 #16 剩余半), 其他静态单例 (ST-07~10)

## 怎么验证

- `./gradlew assembleFossDebug`
- `rg "HttpClient\(" app/src` 仅剩图绑定一处
- 运行时 smoke (设备): 联网拉取 lld.json 成功 (GitHub 与 Gitee 两路), 代理环境切 Wifi PAC 不崩 (原 proxySelector 的修复场景)

## 依赖与前提

- ST-05 完成 (第一段落地, 图已可承载业务绑定)
