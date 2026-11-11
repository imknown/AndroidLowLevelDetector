# subtask-02 共用库与 daemon 骨架

> **状态**: living. 事实核对时间 2026-11-11, 代码在 `5656b51f`.  
> 任务级计划见 [plan.md](plan.md); 流程见 [13-任务工作流](../../agents-cn/13-任务工作流.md).

## 目标

补齐 `scripts/kotlin-lsp-shared.js`, 新增 `scripts/kotlin-lsp-daemon.js`. 前者收三个 CLI 重复的逻辑,  
后者持有一个 `intellij-server` 会话, 让后面的接入都连同一个本地端点. 本子任务不碰三个 CLI.  
`shared.js` 由子任务 1 先建, 里面已经放了一份版本检查模块.

## concept primer

- 现版三个 CLI 每跑一次都 `spawn(server, ['--stdio', '--system-path', indexDir], { cwd: repoRoot })`,  
  见 `kotlin-lsp-plan.js:293-295`, `kotlin-lsp-diagnostics.js:172`, `kotlin-lsp-edit.js:174`.
  stdio 管道只连父子两个进程, 别的进程连不上.
- `initialize` 里塞 `workspaceFolders: [本仓库]` 与 `buildTools`, 见 `kotlin-lsp-plan.js:645-655`,  
  `kotlin-lsp-diagnostics.js:330-340`, `kotlin-lsp-edit.js:430-437`. 一个会话绑一个项目.
- 三个脚本共用 `<repo>/.kotlin/lsp-cache/<平台>.lock`, 见 `kotlin-lsp-plan.js:247`,  
  `kotlin-lsp-diagnostics.js:112`, `kotlin-lsp-edit.js:133`. 拿不到锁直接退出, 不排队,
  理由在 `kotlin-lsp-diagnostics.js:106-111`.
- `10-分析细则:229-230` 把"三个脚本不能同时跑"写成契约. 本子任务改由 daemon 持锁,  
  那句契约的文字改动归子任务 8.
- 本机是 Windows, 具名管道与带盘符路径的平台前提已经真跑过, 记在 `plan.md:112-114`.  
  macOS 那一侧只能代码层核对.

## 现状与证据

| 项 | 位置 | 现在是什么样 |
|---|---|---|
| 每命令新起 server | 三个脚本的 `spawn` 行 | 单次固定开销 23.4 与 23.5 秒, node 自身 CPU 只 0.31 秒 |
| 锁 | `kotlin-lsp-plan.js:247-285`, `kotlin-lsp-diagnostics.js:112-147`, `kotlin-lsp-edit.js:133-169` | 三个脚本各自 `acquireLock()`, 拿不到就退 |
| ready 超时变量 | `kotlin-lsp-plan.js:127`, `kotlin-lsp-diagnostics.js:164`, `kotlin-lsp-edit.js:102` | 编辑通道读 `KOTLIN_LSP_PLAN_READY_TIMEOUT_MS` 默认 300000, 验证通道读 `KOTLIN_LSP_READY_TIMEOUT_MS` 默认 1800000 |
| 索引缓存 | `kotlin-lsp-plan.js:90`, `10-分析细则:107-110` | 按平台分目录, 双项目探针可能把别的项目模型写进 `workspace-model-v2.cache` |

## 计划改动
这一节列的是模块职责与改动项. daemon 的状态与生命周期图在 `plan.md:85-96`,  
本报告只给指向, 不重画.

1. **`shared.js` 收共用逻辑**  
   - server 二进制解析, 发行版根目录, 索引目录与锁路径, LSP 帧读写, 已打开文档集,  
     客户端连接与端点路径. 端点在 Windows 用具名管道, 其余平台用 `<indexDir>/d.sock`.
   - 收拢三条通道的 ready 超时变量, 把两个环境变量名字与预算定在同一处.
   - 写协议版本与代码指纹. 客户端连上先校验, 不匹配就报错退出并提示重启 daemon.
2. **`daemon.js` 收会话与生命周期**  
   - `initialize`, FIFO 请求队列, 一次只让一个批次进 server, `status`, `stop`, 空闲 20 分钟优雅退出,  
     server 崩溃处理.
   - 启动时原子拿锁防重复实例, 退出时释放. 客户端不碰锁. 陈旧的端点与陈旧的锁能自动接管.
   - daemon 起 server 时继承 `JAVA_HOME` 指的那个 JDK, 并在启动日志里打出来.  
     按 `plan.md:105`, 工具链对不上会整片导入失败.
   - 窄协议里给 applyEdit 加一层路由: 请求带发起客户端的标识, server 发来的  
     `workspace/applyEdit` 按这个标识只回给那一个客户端, 不广播.
   - daemon 只由负责人在不随命令结束的终端里前台启动一次. 客户端不自动拉起, 连不上就报错退出.
   - 日志按启动截断, 不无限增长. Unix socket 权限收紧到 0600.
3. **起会话前核单根**  
   - 按只有本仓库根的方式重做一次 warmup, 或起会话时核对 workspace 没有多余根.
4. **接进版本检查调用点**  
   - 就绪后异步调用子任务 1 的模块, 具体形状见 `subtask-01.md`.

## 有意不改

- 三个 CLI 的代码与命令行. 接入分别在子任务 4, 5, 6.
- 没有一次性模式, 没有客户端自动拉起. 连不上 daemon 就非 0 退出, 打印原因和启动命令,  
  不上报 `CLEAN`.
- 不做全机多项目共用. 一个 daemon 只服务本仓库.
- 锁不删. 锁不是提速手段, 留着防多个 daemon 抢同一份索引缓存.

## 覆盖核对表

| `plan.md` 的条目 | 本报告覆盖 | 留给后续 |
|---|---|---|
| 子任务 2 `shared.js` 与 `daemon.js` 的分工 | 是 | 无 |
| 子任务 2 锁归 daemon, 原子拿锁, 退出释放 | 是 | 无 |
| 子任务 2 收拢三条 ready 超时变量 | 是 | 10 的编辑通道那一节归子任务 8 |
| 子任务 2 单根 warmup | 是 | 无 |
| 子任务 2 全部判据 (status, stop, 空闲, 陈旧端点, 日志, 权限) | 是, 见验证 | 无 |
| 子任务 2 协议版本与代码指纹 | 是 | 无 |
| 子任务 2 客户端不自动拉起 | 是 | 三个 CLI 的失败路径归子任务 9 |
| 判据汇总里 `stop` 之后清干净 | 是 | 无 |

## 问题清单的回答

负责人没有给问题清单. 本报告按 `plan.md` 里本子任务的范围与目标自查.

## 验证

- 冷启动能起, `status` 能看到, `stop` 后端点与锁都干净, 没有残留的 JVM 进程.
- 空闲超时能自己退; 陈旧的端点与陈旧的锁能自动接管.
- workspace 里只有本仓库一个根. Windows 具名管道在本机真跑监听, 连接, 逐行 JSON 往返, 关闭.
- 用 `status` 探针验连不上 daemon 时报错退出 (非 0), 打印启动命令, 不回落, 不上报 `CLEAN`.
- 协议版本与代码指纹不匹配时客户端报错退出并提示重启. 客户端不会拉起 daemon.
- 0600 权限看端点的权限位; 日志截断连起两次, 看第二份日志没有第一次的内容.
- 陈旧端点与陈旧锁: 手动留一份上次的端点文件与锁文件, 再起一次, 看能不能接管.
- 单根核起会话前后的 workspace 根列表; 管道按监听, 连接, 逐行 JSON 往返, 关闭四步跑.
- 按 `plan.md:83` 先校验代码指纹, 确认不是旧代码的服务进程, 再跑上面每一项.

## 风险

- 高, 生疏. 守护进程的优雅退出与缓存刷盘要特别小心, 避免杀进程把索引锁写坏.
- 端点路径与锁路径在 Windows 与其余平台分叉. 本机只验得到 Windows 那一侧.
- 常驻会话让索引缓存长期被一个进程持有, IDE 托管的实例与 `warmup.py` 不写这把锁,  
  `kotlin-lsp-diagnostics.js:106-111` 记着这件事.

## 需要负责人定

- 无.
