# 构建约定细则

> **状态**: **living**, 是 [AGENTS-cn.md](../../AGENTS-cn.md) 的构建约定一节的一部分.  
> 在修改 Gradle 文件, 版本目录, 工具链版本或 CI 之前先读本文.  
> 约束每一次构建修改的禁令保留在 AGENTS-cn.md 中.

## Flavor 与变体

- Flavor 维度是 `IssueTracker`, 不是常见的 `mode` / `store`.
- `Foss` 是默认 flavor: 不加跟踪, 且版本名带 `-Foss` 后缀.
- `Firebase` 是 Play 变体, 需要 `google-services.json`, 该文件已被 gitignore.
- `AndroidApplicationFirebaseConventionPlugin` 以 `firebaseImplementation` 挂载 Firebase 依赖, 并  
  为 Foss 禁用 GoogleServices / Crashlytics 任务, 因此 Foss 构建从不需要那个文件.

## 应用标识

- Debug 构建开箱即用; debug 会添加 `applicationIdSuffix = ".debug"`, 因此能与 release  
  并存安装. 处理应用标识 (权限, adb) 时记得这个后缀.
- Release 签名配置在仓库之外, 放在已被 gitignore 的 `local.properties` 中 (见根 README).

## JDK 的两条独立轨道

- 代码轨道从 build 目录读取 `javaToolchain` 版本 (`gradle/toml/build.toml`, 经由  
  `build-logic/convention/src/main/kotlin/.../ext/ProjectExtensions.kt` 中的 `Project.buildVersion()`), 并把它  
  应用到 `KotlinAndroid.kt` 的 `jvmToolchain` 块, 该块钉住了 Adoptium 厂商.
- Gradle Daemon 轨道读取 `gradle/gradle-daemon-jvm.properties` (`toolchainVersion` 与  
  `toolchainVendor`), 该文件由 `updateDaemonJvm` 生成.
- 不要把两条轨道混为一谈, 也永远不要在文档或注释中复述任何一个版本号: 上述文件就是  
  各自轨道的真值源.
- 用 `./gradlew -q javaToolchains` 查看工具链解析器看到了什么.

## 原生与钉住的工具版本

- `:binderDetector` 的原生代码需要把 NDK 与 CMake 版本钉在 `gradle/toml/build.toml` 中.
- 这些正是 CI 安装的版本, 因此升级它们时要与 CI 同步一步不落.

## 版本目录布局

- 版本目录拆分为 `gradle/toml/` 下的五个文件: `build`, `android`, `kotlin`, `google`,  
  `thirdParty`, 通过 `libsAndroid`, `libsBuild`, `libsKotlin`, `libsGoogle`, `libsThirdParty`  
  访问器读取.
- 引用一个依赖前, 先确定它属于哪个类别.

## 仓库过滤

- 仓库都做了内容过滤 (`google()` 由 `includeGroupByRegex` 收窄), 并启用  
  `FAIL_ON_PROJECT_REPOS`.
- `jitpack.io` 只存在于主构建的依赖仓库中.

## 文字叙述不写版本号

- 承载某个值的构建文件就是那个值的真值源; 复述出来的数字会在其背后的文件被升级的那一刻  
  过时, 而文档里一个过时的数字会变成下一个会话当作事实继承的 bug. 这就是 AGENTS-cn.md  
  的正文禁令同时覆盖文档, 代码注释和 commit message 的原因, 也是它点名承载文件而不复述  
  任何数字的原因.

## Kotlin 标志

- Kotlin 版本只存在于 `gradle/toml/kotlin.toml`; 编译器带着一组实验性标志运行, 这些标志在  
  `build-logic` 中声明, 并按引入它们的 Kotlin 版本分组.
- 每次 Kotlin 升级都要重新审查这些分组, 并删掉已经稳定的标志.

## 构建缓存与 build scan

- 配置缓存 (含并行与完整性校验) 与并行构建均已启用.
- build scan 使用 Develocity 插件但从不发布 (`publishing.onlyIf { false }`), 即仅生成本地 scan.
