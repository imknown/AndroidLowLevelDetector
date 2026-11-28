# Gradle 9.0 到 9.9.2 的版本线, 以及 10.0 的状态

> **状态**: **frozen**. 写下的结论不改; 后来的更正留在原处.  
> 兼容边界在 [02](03-gradle-兼容要求.md), 仓库对照在 [03](04-仓库核对.md).

数据基准日: 2026-11-25. 发布日抄自 gradle.org 的 Releases 页; 各小版本的要点抄自各自 release notes 页.

## 全部版本与发布日 (已验证)

| 版本 | 发布日 | 性质 |
|---|---|---|
| 9.0.0 | 2025-07-31 | 大版本首版 |
| 9.1.0 | 2025-09-18 | 小版本 |
| 9.2.0 | 2025-10-29 | 小版本 |
| 9.2.1 | 2025-11-17 | patch |
| 9.3.0 | 2026-01-16 | 小版本 |
| 9.3.1 | 2026-01-29 | patch |
| 9.4.0 | 2026-03-04 | 小版本 |
| 9.4.1 | 2026-03-19 | patch |
| 9.5.0 | 2026-04-28 | 小版本 |
| 9.5.1 | 2026-05-12 | patch |
| 9.6.0 | 2026-06-18 | 小版本 |
| 9.6.1 | 2026-06-26 | patch |
| 9.7.0 | 2026-08-06 | 小版本 |
| 9.7.1 | 2026-08-19 | patch |
| 9.8.0 | 2026-09-24 | 小版本 |
| 9.8.1 | 2026-10-07 | patch |
| 9.8.2 | 2026-10-19 | patch |
| 9.9.0 | 2026-11-06 | 小版本 |
| 9.9.1 | 2026-11-10 | patch |
| 9.9.2 | 2026-11-19 | patch, 这条线的最新 |

9.9.2 的发布日取自 `./gradlew --version` 的 Build time 与 release notes 的措辞; gradle.org 的 Releases 页当时只列到 9.9.1, 还没跟上这一版.  
核实程度: **已验证** (本机构建实测) 加 **假设** (页面滞后, 日期按构建时间算).

## 各小版本的要点

- **9.0.0**: 大版本. 要求 Java 17 以上; 内嵌 Kotlin 语言版本升到 2, Groovy 升到 4; 改用 SemVer 版本号;  
  配置缓存成为首选执行方式; 删掉一批旧 API (核心 conventions, `ApplicationConvention`, `JavaPluginConvention` 等);  
  Gradle API 改用 JSpecify 空值注解.
- **9.1.0**: 支持 Java 25, 可以拿它跑 daemon, 也可以只做工具链; 配置缓存新增只读模式;  
  `-P` 属性变了也能复用缓存; 弃用 `archives` 配置.
- **9.2.0**: 支持 Windows on ARM; 首次任务执行更快, 大构建里最多快四成; 内存占用降一到一成多; daemon 工具链从孵化转稳定.
- **9.3.0**: 发布包开始带 `.asc` 签名; 仓库在更多失败情形下会被停用; 发布当天的 Known issues 一节列了 21 条, 含一条跨工程依赖操作的破坏性变化.
- **9.4.0**: 支持 Java 26; 配置缓存的报错能指到具体的 lambda; daemon 日志 14 天自动清理;  
  插件 ID 默认取注册名; 发布类插件的校验变严.
- **9.5.0**: 任务失败信息带出处 (哪个脚本或插件注册的); 新增 `GRADLE_DAEMON_BIND_ADDRESS`; wrapper 下载可重试;  
  Known issues 一节列了 8 条.
- **9.6.0**: 配置缓存能精确跟踪系统属性与环境变量带来的变化; 新增 `--non-interactive` 与 `NO_COLOR`;  
  开始弃用父工程里的隐式属性与方法查找, 官方写明这一行为将在 Gradle 10 移除, 并提供 `NO_IMPLICIT_LOOKUP_IN_PARENT_PROJECTS` 提前采用.
- **9.7.0**: Isolated Projects 从实验转孵化, 带诊断模式与把违规降成警告的开关; `ResolutionResult` 能直接当任务输入;  
  一批内置任务改出惰性的文件属性.
- **9.8.0**: 支持 Java 27; 配置阶段起外部进程 (不走 `ProviderFactory.exec` 或 `ValueSource`) 开始被弃用;  
  隐式开启并行 Tooling 模型也被弃用.
- **9.9.0**: Kotlin DSL 脚本增量编译, 大项目里重编脚本最多快六成半; 测试过滤对 JUnit Platform 的非方法级测试也生效;  
  wrapper 任务保留自定义属性; Problems API 按领域分组; Isolated Projects 支持 Eclipse. Known issues 两条:  
  #39756 (仓库对插件 marker 返回 401 时插件解析失败) 与 #39729 (增量构建里缺依赖检查变慢).
- **9.9.1**: 修三个安全问题 (版本目录值会被当代码执行, 级别 Critical; 客户端信任 daemon 注册表内容; 客户端信任过期注册表项);  
  修 KT-89265 带来的弃用提示误报.
- **9.9.2**: 修四个高危安全问题 (SFTP 仓库不校验主机钥; 传递性 snapshot 依赖绕过依赖校验;  
  TestKit 用可预测的临时目录; daemon 注册表创建瞬间可被本机其他用户读到), 并把 9.9.0 的两条已知问题一起修掉;  
  官方在 release notes 里直接写 "We recommend upgrading to Gradle 9.9.2".

patch 版里, 只有 9.9.1 与 9.9.2 的修复清单核过; 其余八个 patch 官方页没有逐个抓, 只知道它们存在.  
核实程度: 9.9.1 与 9.9.2 **已验证**, 其余 patch **不确定**.

## 10.0 的状态: 没有发布

| 核查点 | 结果 |
|---|---|
| gradle.org 的 Releases 页 | 最新条目是 9.9.1, 没有任何 10.x |
| `/versions/current` 接口 | 返回 9.9.1, `"current": true` |
| `/versions/release-candidate` 接口 | 返回 `{ }`, 没有进行中的 RC |
| `/distributions/gradle-9.9.2-bin.zip.sha256` | 能取到, 与仓库 wrapper 记的值一致 |

所以 10.0 目前只存在于计划里. 核实程度: **已验证**.

官方已经写明的 10.0 计划, 全部来自 Gradle 自己的文档与 9.6 的 release notes:

- 配置缓存将在 Gradle 10 默认开启, 见 Configuration Cache 页首句 "This feature will be enabled by default in Gradle 10".
- 父工程里的隐式属性与方法查找将在 Gradle 10 移除, 见中文对应的 9.6 弃用条目.
- 软件模型 (`model { }`, `RuleSource`, 以及建在它上面的 native 组件) 将在 Gradle 10 移除.
- 在构建逻辑里改 start parameter 开启构建缓存, 将在 Gradle 10 移除.
- 任务执行没声明输入的 Artifact Transform, 在 10 里升级成错误.

这五条没有一条给出版本号或日期, 官方只说"将在 Gradle 10".  
核实程度: **已验证** (各页原文) 加 **不确定** (10.0 的发布时间没有任何官方日期).

## 出处

- Releases 页: https://gradle.org/releases/
- 当前版本与 RC 接口: https://services.gradle.org/versions/current , https://services.gradle.org/versions/release-candidate
- 9.0.0: https://docs.gradle.org/9.0.0/release-notes.html
- 9.1.0: https://docs.gradle.org/9.1.0/release-notes.html
- 9.2.0: https://docs.gradle.org/9.2.0/release-notes.html
- 9.3.0: https://docs.gradle.org/9.3.0/release-notes.html
- 9.4.0: https://docs.gradle.org/9.4.0/release-notes.html
- 9.5.0: https://docs.gradle.org/9.5.0/release-notes.html
- 9.6.0: https://docs.gradle.org/9.6.0/release-notes.html
- 9.7.0: https://docs.gradle.org/9.7.0/release-notes.html
- 9.8.0: https://docs.gradle.org/9.8.0/release-notes.html
- 9.9.0: https://docs.gradle.org/9.9.0/release-notes.html
- 9.9.2: https://docs.gradle.org/9.9.2/release-notes.html
- Configuration Cache (10.0 默认开启那句): https://docs.gradle.org/current/userguide/configuration_cache.html
- 9.x 升级指南 (10.0 移除项): https://docs.gradle.org/current/userguide/upgrading_version_9.html
