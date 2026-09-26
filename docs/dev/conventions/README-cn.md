# 工程约定与事实

本项目怎么构建, 以及改动它的规则的详细 memory. `AGENTS.md` 是索引; 这里是内容. 刻意偏离主流/官方模板的约定在行文中直接标出 — 它们不是要修的坏味道, 别顺手把它们 "规范化" 掉. 本文档与代码冲突时, 信代码并更新本文档.

## 构建

- Flavor (维度名 `IssueTracker` — 不是常见的 `mode` / `store`): `Foss` 是默认 (无跟踪, 版本名带 `-Foss` 后缀); `Firebase` 是 Play 变体, 需要 `google-services.json`, 该文件已被 gitignore. `AndroidApplicationFirebaseConventionPlugin` 以 `firebaseImplementation` 附加 Firebase 依赖, 并为 Foss 禁用 GoogleServices / Crashlytics 任务, 所以 Foss 构建永远不需要那个文件.
- Debug 构建开箱即用; debug 增加 `applicationIdSuffix = ".debug"`, 与 release 并排安装 — 处理应用身份 (权限, adb) 时记得这个后缀. Release 签名配置在仓库之外, 写在被 gitignore 的 `local.properties` (见 [README](../../../README-cn.md)).
- JDK 25 (Adoptium) 走两条独立的轨道: 代码编译经由 `jvmToolchain`, Gradle Daemon 经由 `gradle/gradle-daemon-jvm.properties` (由 `updateDaemonJvm` 生成). 不要混淆二者. 用 `./gradlew -q javaToolchains` 查看.
- `:binderDetector` 原生代码需要把 NDK 和 CMake 版本钉在 `gradle/toml/build.toml` (当前 NDK 30.0.16248370, CMake 4.1.2) — 与 CI 安装的版本完全一致, 改它们就要连同 CI 一起改.
- 版本目录拆成五个文件 (`gradle/toml/`: `build` / `android` / `kotlin` / `google` / `thirdParty`): 用 `libsAndroid`, `libsBuild`, `libsKotlin`, `libsGoogle`, `libsThirdParty`. 没有默认的 `libs` 访问器. 依赖和版本只存在于这些目录里; 绝不在模块的 `build.gradle.kts` 里写裸坐标, 引用之前先决定依赖属于哪一类.
- 版本号档位: RC / Stable 的依赖或工具链版本可直接用于生产. Beta / Alpha / Canary 也可以, 但前提是研究透彻, 已知问题能被修复或规避, 且经过评估.
- SDK, build-tools 和 NDK 版本只放在 `gradle/toml/build.toml` (带 `isPreview` 开关), 经由 `build-logic` 约定插件到达模块. 绝不在模块脚本里硬编码 SDK 级别.
- Kotlin 2.4 带一组在 `build-logic` 声明的实验性编译器 flag, 按引入它们的 Kotlin 版本分组 — 这些 flag 是有意为之, 不要删; 每次升级 Kotlin 都重新审一遍各组, 去掉已稳定的. 代码风格是 `official`.
- 仓库带内容过滤 (`google()` 被 `includeGroupByRegex` 收窄) 并启用 `FAIL_ON_PROJECT_REPOS`; `jitpack.io` 只存在于主构建的依赖仓库里.
- 已启用配置缓存 (带并行与完整性检查) 与并行构建; 自定义任务要保持与配置缓存兼容.
- 构建扫描用 Develocity 插件但从不发布 (`publishing.onlyIf { false }`) — 只有本地扫描.

## 模块细节

- `:app` — 应用; namespace 即 applicationId. 它的 `sourceSets` 登记包相邻的 res 目录: 资源位于 `java/<package>/.../res` 路径下 (例如 `app/src/main/java/net/imknown/android/forefrontinfo/ui/home/res`), 没有 `app/src/main/res`, 新的 res 目录必须在 `app/build.gradle.kts` 的 sourceSets 里登记.
- `build-logic` — 持有约定插件的 included build; 所有模块的 SDK 与构建取值的唯一来源. 共享配置 (SDK, 脱糖, Java 工具链, Kotlin 编译参数, 测试依赖) 在 `build-logic/convention/src/main/kotlin/.../android/`; 模块只 apply 插件. 新增约定插件: 在 `build-logic/convention` 下实现, 然后在 `gradle/toml/android.toml` 的 `[plugins]` 段登记别名.

## 架构

每个功能住在 `ui` 下自己的包里 (`ui.home`, `ui.others`, `ui.prop`, `ui.settings`), 分层相同:

```
Screen (Compose) → ViewModel (StateFlow) → Repository → DataSource
```

- 导航用 **Navigation 3** (`androidx.navigation3`), 不是主流的 Navigation 2: API 形态是 `NavKey` + 返回栈 + entryProvider (`ui/navigation/NavKeys.kt`). 不要用 Nav2 的思路 (`NavHost(route = ...)`).
- **没有 DI 框架** — 刻意偏离 Hilt / Koin 主流. 每个 ViewModel 用伴生 `Factory` 手工接线依赖 (`viewModelFactory { initializer { ... } }`), Screen 用 `viewModel(factory = ...)` 取. 新增 ViewModel 时照抄 `HomeViewModel.Factory` 的模式.
- 可测性来自 **接口优先的设计**, 不是 mock 框架: `:base` 定义 `IProperty` / `IShell` 并给出默认实现 (`PropertyDefault` / `ShellDefault`), 由 `PropertyManager` 用 `by` 委托聚合.
- `BaseListViewModel` 用两条 `StateFlow` 驱动所有列表页 — `modelsStateFlow: StateFlow<List<MyModel>?>` (null = 冷启动; 刷新刻意保留上一份列表, 让 UI 永不闪空) 和 `isLoadingStateFlow` — 外加 `loadJob` 去重: 不要在重建时重新引入冗余加载. 每次加载落地之后运行 `onModelsLoaded()`, 用于调和构建期间发生变化的状态.
- Compose 稳定性注解 (`@Immutable` / `@Stable`) 是刻意的; 状态类一变就重新评估 (照 `HomeViewModel` / `BaseListViewModel` 顶部注释里的模式, 它解释了 *为什么* 该注解是安全的).
- 内置的 `lld.json` 数据被复制到外部 files 目录 (`LldManager`), 用户允许联网时经由 Ktor 在线刷新; 用 GitHub 还是 Gitee 的 URL 按时区选择.
- 命令执行用 libsu 的 **非 root** 模式 (`ui/common/ShellLibSu.kt`, 带 `Shell.FLAG_NON_ROOT_SHELL`): 还没有 root 层, 见问题汇总 [A4](../issues-cn/05-已裁定事项.md#A4).

## 新增检测条目

当前工作流 (列表顺序 = 调用顺序):

1. 往功能的 `DataSource` 加属性 key / shell 命令.
2. 给功能的 `Repository` 加一个 `detect...()` 方法, 返回 `MyModel`.
3. 在功能的 `ViewModel.collectModels()` 里调用它 — 调用顺序决定列表顺序.
4. 往功能包的 `strings.xml` (默认英语) 加字符串, 外加三份翻译文件.
5. 条目需要带资源的新包时, 在 `app/build.gradle.kts` 的 sourceSets 登记它的 res 目录.
6. 用 `./gradlew assembleFossDebug` 验证.

## 代码规则

针对新代码的规则 — 它们编码了已裁定的决定; 别让既有债务变得更糟:

- `ViewModel` 写全称 — 在标识符, 注释, 提交信息或文档里都不许缩写成 `VM`. `VM` 已经是 *虚拟机* 的缩写, 而这个应用把虚拟机当检测对象本身 (进程/VM 架构, `getArchitecture`), 所以即使上下文显然, 短形式也有歧义. `UseCase` 和 `DataSource` 同样写全称 — 不用 `UC` / `DS`.
- 没有静态事件总线: 绝不把 `SharedFlow`/`StateFlow` 放进 ViewModel 的伴生对象. 跨功能数据走仓库.
- 全局的 `myAndroid` (`AndroidVersionExt`) 只有两个写入方: 启动时的 `initMyAndroid()` (`MyApplication.onCreate`, 来自运行时的 `Build.VERSION`), 以及 `HomeRepository.detectAndroid()` 里的已知值覆写. 绝不在别处赋值 — `isAtLeast...()` 辅助函数到处都在读它.
- minSdk 是 24: 更新的 API 用 `isAtLeastAndroidX()` 辅助函数或 `@RequiresApi` 把关.
- 新代码使用钉住的版本所允许的最新语法和标准库 API: 当前 Kotlin 版本支持的最新 Kotlin 语法与标准库 API, 当前 compileSdk 提供的最新平台 API, 以及当前依赖版本提供的最新 API — 绝不写比工具链允许的更旧的写法.
- 当最新可用的语法或 API 本身是 Beta / 实验性时, 不要擅自采用 — 先摆出来, 询问 owner 如何处理. `build-logic` 里已启用的实验性编译器 flag 是已裁定的集合; 这条规则针对的是新的 opt-in.
- 阻塞工作 (shell, 系统属性, 文件, 网络) 跑在 `Dispatchers.IO`, 不是 `Dispatchers.Default`.
- `ShellDefault` 故意没有调用方: 它是保留备用的原生 shell 实现, 不依赖 libsu (在用的是 `ShellLibSu`). 别把它当死代码删; 如果将来启用它, 先修掉 `waitFor()` 之后读管道的死锁 (问题汇总 [AR-18.1](../issues-cn/04-反模式与隐患.md#AR-18)).

## 本地化

- 字符串按功能包拆分. 支持的 locale: 默认 (英语), `zh-rCN`, `zh-rTW`, `fr-rFR` (`localeFilters` + `generateLocaleConfig`); 新增语言时把它加进 `localeFilters`.
- 新的面向用户字符串必须有默认英语条目; 能同步时保持三份翻译文件同步.
