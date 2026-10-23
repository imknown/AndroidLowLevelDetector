# 构建约定细则

> **状态**: **living**, 是 [AGENTS-cn.md](../../AGENTS-cn.md) 构建约定那节的细则部分.  
> 改 Gradle 文件, 版本目录, 工具链版本或 CI 之前, 先读这份.  
> 管住每一次构建改动的禁令写在 AGENTS-cn.md 里.

## Flavor 与变体

- Flavor 维度叫 `IssueTracker`, 不是常见的 `mode` 或 `store`.
- `Foss` 是默认 flavor: 不加任何跟踪, 版本名带 `-Foss` 后缀.
- `Firebase` 是 Play 变体, 需要 `google-services.json`. 这个文件已经被 gitignore.
- `AndroidApplicationFirebaseConventionPlugin` 用 `firebaseImplementation` 挂 Firebase 的依赖,  
  并且给 Foss 禁用 GoogleServices 和 Crashlytics 两个任务. 所以 Foss 构建从来不需要那个文件.

## 应用标识

- Debug 构建装出来就能用. debug 会加 `applicationIdSuffix = ".debug"`, 所以它能和 release 并存安装.  
  处理应用标识的时候记得这个后缀, 比如权限和 adb.
- Release 的签名配置在仓库之外, 放在已被 gitignore 的 `local.properties` 里 (根 README 有说明).

## JDK 的两条独立轨道

- 代码这条轨道从 build 目录读 `javaToolchain` 的版本. 这个值在 `gradle/toml/build.toml` 里,  
  经 `build-logic/convention/src/main/kotlin/.../ext/ProjectExtensions.kt` 的 `Project.buildVersion()` 取出来,  
  然后用到 `KotlinAndroid.kt` 的 `jvmToolchain` 块上. 那个块把厂商钉在了 Adoptium 上.
- Daemon 这条轨道读 `gradle/gradle-daemon-jvm.properties` 里的 `toolchainVersion` 和 `toolchainVendor`.  
  这个文件由 `updateDaemonJvm` 生成.
- 不要把两条轨道混成一件事说. 文档和注释里也永远不要复述任何一个版本号: 这两个文件各自就是自己那条轨道的真值源.
- 想看工具链解析器到底看到了什么, 跑 `./gradlew -q javaToolchains`.

## 原生与钉住的工具版本

- `:binderDetector` 的原生代码要求把 NDK 和 CMake 的版本钉在 `gradle/toml/build.toml` 里.
- CI 装的就是这些版本, 所以升级它们时要同步改 CI, 一步都不能落下.

## 版本目录布局

- 版本目录拆成 `gradle/toml/` 下五个文件: `build`, `android`, `kotlin`, `google`, `thirdParty`.  
  读取走 `libsAndroid`, `libsBuild`, `libsKotlin`, `libsGoogle`, `libsThirdParty` 这几个访问器.
- 引用一个依赖之前, 先定它属于哪一类.

## 仓库过滤

- 每个仓库都做了内容过滤, 比如 `google()` 用 `includeGroupByRegex` 收窄, 同时启用 `FAIL_ON_PROJECT_REPOS`.
- `jitpack.io` 只在主构建的依赖仓库里.

## 正文里不写版本号

- 存着某个值的那个构建文件, 就是这个值的真值源. 复述出来的数字, 在那个文件升级的那一刻就过时了.  
  文档里一个过时的数字, 下一个会话会当成事实继承下去, 那就是 bug.
- AGENTS-cn.md 的正文禁令同时管文档, 代码注释和 commit message, 也正因为这样, 它点名存值的文件,  
  不复述任何数字.

## Kotlin 标志

- Kotlin 版本只在 `gradle/toml/kotlin.toml` 里. 编译器带一组实验性标志运行, 这些标志在 `build-logic` 里声明,  
  按引入它们的 Kotlin 版本分组.
- 每升一次 Kotlin 都要重新审查这些分组, 并删掉已经稳定的标志.

## 构建缓存与 build scan

- 配置缓存 (带并行和完整性校验) 和并行构建都已启用.
- build scan 用 Develocity 插件, 但从不发布 (`publishing.onlyIf { false }`), 只生成本地 scan.
