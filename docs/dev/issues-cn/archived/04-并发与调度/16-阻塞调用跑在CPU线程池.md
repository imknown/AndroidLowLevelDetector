<a id="i16"></a>

# 16 阻塞调用跑在 CPU 线程池

> 返回 [README 索引](../../README.md) · [4 · 并发与调度](../../README.md#4--并发与调度).

**严重程度: P2 | 修复难度: 低 | 状态: 已修复 (8 处 Default 全部改 IO; DataSource 下沉的完整修法未做, 要做单独议)**

> 原标题 "阻塞调用跑在 CPU 线程池, 每次请求新建 HttpClient" 的后半 (每次请求新建 `HttpClient` + `client.use{}` 关闭) 已随 DI 接入关闭:  
> 客户端收成图内 `@SingleIn` 绑定, 经 `() -> HttpClient` 惰性解析, `use{}` 关闭随之消失,  
> 引擎配置原样上移, 明细见 git log. 当时并排的 "HTTP 状态码闸门" 没有一起加,  
> 仍归 [#24](../../05-稳定性与错误处理/24-杂项隐患.md).

## 问题核心代码

### 阻塞调用包在 `Dispatchers.Default` 里 (修复前全仓正好 8 处)

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

`/proc/mounts` 那一处已经在 IO 上:  
`HomeViewModel.detect()` 里 `homeRepository.getMounts()` 单独包了 `withContext(Dispatchers.IO)`.

## 直接原因

`Shell.cmd().exec()`, `/proc/mounts` 读取, 反射 `SystemProperties` (`PropertyDefault`),  
packageManager 查询都是**阻塞 IO**; `Dispatchers.Default` 的线程按 CPU 核数分配,  
阻塞调用在里面是空等而不是工作. 协程的约定是 Default 给计算,  
IO 给阻塞. AGENTS.md 也把它写成了项目规则 ("阻塞工作 (shell, 系统属性, 文件, 网络) 走 `Dispatchers.IO`"),  
所以**错的是代码, 不是约定**.  
当前没出事是因为并发量小 (每个页面都是单次顺序加载, 见 [#15](15-检测串行不感知取消.md)),  
但 8 个点都在错误的池上排队.

## 根本原因

调度器的选择是随手写的 (`Default` 听起来比 `Main` 安全), 调度知识放在调用方而不是拥有阻塞实现的 DataSource 一侧.

## 处置

缓解已随 [#14](14-检测链用CPU线程池.md) 一并落地 (一个提交关闭两条): 8 处 `Dispatchers.Default` 改  
`Dispatchers.IO`, 每处一词; `HomeViewModel.detect()` 里相邻的两个 IO 块合并成一个; 内层读 mounts 的  
`withContext(Dispatchers.IO)` 按条目原案保留 (外层改 IO 后它已冗余); `SettingsRepository` 三处各自产出  
`val` 的块未合并, 合并属于结构重排, 留给完整修法. 验证: 三档 Gradle 全部通过, lint 无新增问题.

完整修法 (把 IO 声明下沉到各 DataSource 方法内部, 调用方不再知道 "里面有阻塞调用") 未做,  
要做时单独议, 时机上与 [#15](15-检测串行不感知取消.md) 及 [#03](../../01-架构与分层/03-无三态模型与注册表.md)  
的注册表引擎合流最省. 届时 `#14` 处置里记录的纯计算分派问题一并解决.
