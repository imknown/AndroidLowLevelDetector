# subtask-03 1a CI 扩跑

> **状态**: living. 事实核对时间 2026-10-27, 代码在 `502a6eb7`.  
> 2026-11-06 开工前复核: 代码在 `bb839c00`; AGP 已从 RC 线升到稳定版 (`gradle/toml/android.toml` 里那条),  
> 下文原先写 "RC 线" 的地方已按现状改掉; yaml 行号, `gradle/toml/build.toml` 的 CMake 与 NDK 版本,  
> `configureFlavors` 调用点, `app/lint-baseline.xml` 复核后不变.  
> 任务级计划见 [plan.md](plan.md); 流程见 [13-任务工作流](../../agents-cn/13-任务工作流.md).

## 目标

让 CI 除了编译之外, 还跑 JVM 单元测试与 lint, 把已有的 `DateTimeExtTest` 和后续新增的单元测试  
从"只在本地跑"变成"合入前一定跑".  
黄金数据不在这条里: 它是仪器测试的产物, CI 只跑 JVM 测试, 那份数据由人工在真机或模拟器上生成.

## concept primer

- 1a 之前, CI 只有一个 Gradle 步骤: `.github/workflows/android-ci.yml` 的 `[Gradle]` 区,  
  跑 `./gradlew assembleFossDebug`.
- 单元测试任务的名字跟 flavor 走. Flavor 只在 `app` 生成, 因为  
  `AndroidApplicationFlavorsConventionPlugin` 是唯一的 `configureFlavors()` 调用点; `base` 没有 flavor,  
  它的任务是 `:base:testDebugUnitTest`. 只写 `testFossDebugUnitTest` 不会跑到 `base` 的测试.
- 不带模块前缀的任务名从根项目跑, 会选中所有模块里叫这个名字的任务. 现在只有 `app` 有 Foss 变体,  
  所以 `testFossDebugUnitTest` 等于 `:app:testFossDebugUnitTest`; 要限定到某一个模块, 写全路径.  
  (2026-11-06 用 `--dry-run` 核过: 选中的只有 `:app:testFossDebugUnitTest`, `base` 与 `binderDetector`  
  的任务名是 `testDebugUnitTest`.)
- lint 的存量问题已经用 baseline 记下: `app/lint-baseline.xml` 存在. `lintFossDebug` 在 2026-10-28  
  跑过一次, 1b 的实现与 review 里又跑过多次, 都没有冒出超越 baseline 的新问题; 但 CI 上是空缓存与独立环境,  
  不能把本地结果当成 CI 结果.
- 一条 Gradle 命令里, 前一个任务失败即停. 想让后面的任务继续跑完, 加 `--continue`.
- 按 0f 的决定, CI 只跑 JVM 测试; 真机与模拟器上的仪器测试不进 CI.

## 开工前核对的现状与证据

| 项 | 位置 | 核对时是什么样 |
|---|---|---|
| CI 步骤 | android-ci.yml 的 `[Gradle]` 区 | 只有 `assembleFossDebug` |
| CI 的 SDK 与 NDK | android-ci.yml:19-32 | 按 `gradle/toml/build.toml` 的版本装 CMake 与 NDK |
| lint baseline | app/lint-baseline.xml | 记录着存量发现 |
| 规则条款 | [09-构建约定](../../agents-cn/09-构建约定.md) 的 CI 一节 | 写着"CI 只构建 `assembleFossDebug`" |

## 计划改动

1. 加任务.

   三个候选, 差别在日志可读性与总时长:

   - A. 保持一步, 命令扩成  
     `./gradlew assembleFossDebug testFossDebugUnitTest :base:testDebugUnitTest lintFossDebug`.  
     改动最小; 但哪个任务红, 要翻日志才知道, 且前一个失败后面不跑.
   - B. 拆成四步, 各跑一个任务. 哪一步红一眼看得出, 失败原因也短.
   - C. 一步加 `--continue`, 四个任务全跑完再报错. 一次拿到全部结果, 日志仍混在一起.

   **已定 B** (负责人 2026-10-28). 理由: CI 上真正贵的是一次 runner 启动; 拆步后 Gradle 守护进程与  
   构建缓存都还在, 多出的开销很小, 换来的是红的定位快.

1. 本地先清红.

   - 已经跑过一遍 (2026-10-28, 四个任务串一次, 16 分 25 秒, 本地无缓存, 大部分时间在下载依赖).  
     结果全绿: `assembleFossDebug`, `testFossDebugUnitTest`, `:base:testDebugUnitTest`, `lintFossDebug`  
     都通过, `BUILD SUCCESSFUL`.
   - lint 报告在 `app/build/reports/lint-results-FossDebug.txt`, 第 2 行的原句是  
     `No errors or warnings (and 1 error, 6 warnings, 1 hint filtered by baseline lint-baseline.xml)`.  
     意思是当前的 lint 没有冒出超越旧 baseline 的新问题, 今天不需要清红. 但 baseline 里压着  
     1 个 error 与 6 个 warning, 那是已经存在的问题, 不在本子任务范围.
   - 清红的边界照旧: 以后真红了, 能当场修的当场修; 修不动的列出来另立条目. 不为让 CI 变绿去缩短  
     检查范围 (例如把 lint 从命令里去掉).
   - CI 上的耗时与本地不同: 那边每次从空缓存开始, 且没有构建缓存目录. 首跑变慢是正常的,  
     后续要不要加缓存归 [#51](../../dev/issues-cn/09-构建与CI/51-CI缺硬化.md).

1. 规则文件.

   - [09-构建约定](../../agents-cn/09-构建约定.md) 的 CI 一节改成新的四步清单, 并写明 base 的单列任务.  
     同一条在 [#50](../../dev/issues-cn/09-构建与CI/50-CI只编译不测试.md) 的修复方案里已经写过.
   - [11-构建与验证](../../agents-cn/11-构建与验证.md) 构建命令块开头那句注释要改: 扩跑之后 CI 跑的不止是构建.  
     旧句是 `# CI 构建的目标 (GitHub Actions)`, 改的时候连那几条命令一起看.
   - 复述版本号的地方不写具体数字, 只点名 `gradle/toml/build.toml`.

## 有意不改

- CI 的并发控制, 超时, 缓存, JDK 固定, 最小权限. 那些在 [#51](../../dev/issues-cn/09-构建与CI/51-CI缺硬化.md),  
  不属本子任务.
- 不引 detekt / ktlint / spotless. 全仓零出现, 先用 AGP 自带检查.
- 不跑仪器测试.

## 覆盖核对表

| 计划里的范围 | 问题条目 | 本子任务覆盖 | 留给后续 |
|---|---|---|---|
| CI 扩跑单元测试与 lint | [#50](../../dev/issues-cn/09-构建与CI/50-CI只编译不测试.md) | 是 | 更严的静态检查另立项 |
| base 的单列任务 | #50 的实施细节 | 是 | 无 |
| 规则文件 CI 条款 | 0a | 是 | 无 |
| 需求条目 | NFR-4 可靠性, NFR-5 可维护性 | 间接 | 无 |

## 问题清单的回答

老板没有给问题清单.

## 验证

- 本地四个任务各跑一遍, 全绿再改 yaml.
- yaml 改完, 开一个 PR 看 CI 实跑结果. 这是唯一能证明配置写对的办法, 本地跑通不算.
- 若首跑就红, 把红的任务与原因报给老板, 不自行放宽检查范围 (例如去掉 lint).

## 风险

- 低. 本地四个任务已跑通, 剩下的不确定性在 CI 一侧: 空缓存下的耗时, 以及 runner 环境与本地 SDK  
  是否一致. 这些不改变本子任务的改法.
- CI 上装的是 `gradle/toml/build.toml` 里钉住的 CMake 与 NDK. 本子任务不改它们, 若首跑因工具链失败,  
  那是环境问题, 不是本子任务的改动.
- PR 的目标分支是 `develop` (`android-ci.yml:3-8`), 不是 `master`.

## 需要老板定

- 已定 (2026-10-28): 拆四步; lint 首跑带出的 error 能当场修就修, 修不动另立条目, 不放宽检查范围.
- lint 在当前的 AGP 上先跑过了, 没有新问题 (2026-10-28; 2026-11-06 复核仍是 `no new issues`).
