# subtask-05 diagnostics.js 接入

> **状态**: living. 事实核对时间 2026-11-11, 代码在 `5656b51f`.  
> 任务级计划见 [plan.md](plan.md); 流程见 [13-任务工作流](../../agents-cn/13-任务工作流.md).

## 目标

把诊断通道改成连常驻 daemon. 输出与退出码不变, 多次单文件调用都走同一个 daemon,  
不新增多文件入参, 命令行不变.

## concept primer

- 用法是 `node scripts/kotlin-lsp-diagnostics.js <path-to-kotlin-file>`, 见 `kotlin-lsp-diagnostics.js:5-8`  
  与 `:44-48`. 脚本必须待在仓库根的 `scripts/` 下, 项目根与索引目录都从脚本自身位置推出来.
- 流程是 initialize, initialized, didOpen, 等 `intellij/ready-for-test`, documentSymbol 当分析检查,  
  再 `textDocument/diagnostic`, shutdown, 见 `kotlin-lsp-diagnostics.js:3`.
- 退出码 0 只在拉取成功且没有 ERROR 时; 运行失败, 握手失败, 文档没被分析, 或任一条 ERROR 都退 1,  
  见 `kotlin-lsp-diagnostics.js:23-26` 与 `:410-419`.
- `CLEAN (0 items)` 打在 `:371`, 计数行打在 `:390`. 调用方拿这两行判通过.
- `kotlin-lsp-selfcheck.js` 用 `spawnSync` 调这个脚本, 见 `:24`, `:115` 与 `:121-141`.  
  它按 `CLEAN (0 items)` 与计数行的正则判模型是否活着.

## 现状与证据

| 项 | 位置 | 现在是什么样 |
|---|---|---|
| server 与锁 | `kotlin-lsp-diagnostics.js:112-172` | 自己 `acquireLock()`, 自己 `spawn` |
| initialize | `kotlin-lsp-diagnostics.js:330-343` | 自己发, 打印 `== initialized, server:` |
| 输出 | `kotlin-lsp-diagnostics.js:371-390` | `CLEAN (0 items)` 与四档计数行 |
| 调用方 | `kotlin-lsp-selfcheck.js:121-141` | 正则匹配输出, 判模型是否活着 |

## 计划改动
这一节列的是接入步骤与保持项, 不是流程或判定逻辑, 不画图.

1. **接入前存基线**  
   - 挑一批干净文件与带错文件, 在旧版上跑一遍, 把完整输出与退出码存成基线文件放进这个 spec 目录.
2. **换掉启动与锁那段**  
   - 删掉自己的 `acquireLock` 与 `spawn`, 改成连 daemon. 多次单文件调用复用同一个 daemon 会话.
   - 不新增多文件入参. 命令行仍是单文件一次调用.
3. **保持输出形状**  
   - `CLEAN (0 items)`, 四档计数行, 退出码全部原样. 除 server 版本行外与基线逐行一致.
   - 连不上 daemon 时非 0 退出, 打印原因和启动命令, 不上报 `CLEAN`.
4. **给 selfcheck 留口子**  
   - 本子任务不改 `kotlin-lsp-selfcheck.js`. 它按下一条的输出形状继续能跑;
   - 改成走 daemon 与扩展判据归子任务 8.

## 有意不改

- 输出行, 退出码, 单文件命令行.
- 不新增多文件入参, 不新增命令行开关.
- `kotlin-lsp-selfcheck.js`. 归子任务 8.
- `10-分析细则` 的诊断通道契约文字. 归子任务 8.

## 覆盖核对表

| `plan.md` 的条目 | 本报告覆盖 | 留给后续 |
|---|---|---|
| 子任务 5 输出与退出码不变 | 是 | 无 |
| 子任务 5 多次单文件调用走同一个 daemon | 是 | 无 |
| 子任务 5 不新增多文件入参, 命令行不变 | 是 | 无 |
| 子任务 5 接入前存旧版基线 | 是 | 无 |
| 判据干净文件 `CLEAN`, 带错退出 1, 逐行对比 | 是, 见验证 | 无 |
| 判据汇总: 现有调用处不用改 | 是 | selfcheck 改造归子任务 8 |
| 判据汇总: 不存在假 `CLEAN` | 依赖子任务 3 的刷新 | 端到端复核归子任务 9 |

## 问题清单的回答

负责人没有给问题清单. 本报告按 `plan.md` 里本子任务的范围与目标自查.

## 验证

- 干净文件回 `CLEAN`, 带错误文件退出 1, 与基线逐行对比一致.
- 连续多次单文件调用, 确认连的是同一个 daemon, 没有新起 server.  
  看 daemon 持有的 server 会话数保持 1, 日志里同一会话的请求计数在涨,
  再核进程列表没有多出的 JVM. 每次 CLI 调用会新建连接, 连接数不作观察点.
- 连不上 daemon 时非 0 退出, 打印启动命令, 不上报 `CLEAN`.
- 按 02 的验证顺序跑自审与分析通道, 再委派 review.
- 按 `plan.md:83` 先校验 daemon 的代码指纹, 再跑基线对比.

## 风险

- 中. 诊断结果被 `02`, `10`, `11` 当成每批必跑的检查, 输出回归影响面比规划通道更大.
- 常驻会话下文档的刷新由子任务 3 保证. 本子任务不重复实现刷新, 但验证要一起看.

## 需要负责人定

- 无.
