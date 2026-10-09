# AGENTS.md

给在本仓库工作的编码 agent 的指引. 改动之前先读.

两边默认都不是权威: **代码和文档都可能错**. 代码决定应用*今天*做什么, 这是可验证的事实; 应用*应该*做什么属于负责人, 而文档里对意图的陈述本身也可能是个错误 (写过头了, 或者凭空编的).  
所以在编辑之前先给文档与代码的分歧定性 (文档过时, 代码 bug, 未实现的需求, 还是文档夸大了需求), 证据无法定性时, 或者问题在于所述意图是否真的出自负责人, 就问.

工作原则:

- **Memory**: 本文件就是持久的 memory, 已裁定的决定, 约定, 坑, 文档更正都记在这里, 让下一个 session 站在它们之上, 而不是重新发现一遍.  
  未整理的个人材料进 [docs/dev/JOTTINGS.md](docs/dev/JOTTINGS.md). 不确定该记什么或该记在哪, 和用户讨论.
- **Never guess**: 先查代码库和文档; 每次改动都要建立在你能指出来的证据上 (文件, 行号, 文档小节). 无法确定, 或存在多种合理做法时, 摆出发现并问用户; 写 `unknown`,  
  不要编造取值. 冲突是停止信号, 不是择一的许可: 证据不能判定哪边错 (文档 vs 文档, 文档 vs 代码) 时, 把两边摆出来问. 指令的范围只到被点名的对象: 第二句看起来像同一个问题的, 进入待问的清单,  
  不并进同一次修改.
- **English by default**: 你生成的一切 (文档, 注释, 提交信息) 除非用户另作说明, 一律用英语. 例外: 代码注释在实施期用负责人的聊天语言书写,  
  提交闸门前整体译为英语 (见 任务工作流 第 1 步). 不管文档用什么语言写, 标点都是 ASCII, 间距按英语来 (不用 `，。、：（）「」`): 见 [文档规则](#文档规则).
- **No sensitive information**: 任何文档, memory 文档在内, 都不得含敏感信息: 隐私数据, 密码, 密钥, 证书或签名材料.

刻意偏离主流 / 官方模板的约定在下文就地标出, 它们不是要修的坏味道, 别顺手把它们 "规范化" 掉. 这里的陈述和代码不一致时, 以代码为准, 并改本文件.

## 项目概览

Android 应用, 展示底层系统特征: Treble 与 GSI 兼容性, Mainline/APEX 模块, system-as-root, A/B 分区, Binder 位数, 安全补丁级别. 技术栈:  
Kotlin, Jetpack Compose (单 Activity, Navigation 3), MVVM + StateFlow 单向数据流, kotlinx.serialization, Ktor,  
libsu, JNI/NDK. DI 用 Metro (编译期, 无反射).

- 应用 id `net.imknown.android.forefrontinfo`; 版本信息在 `gradle/toml/build.toml`.
- 模块: `:app` (Compose UI, 功能在 `ui` 下) · `:base` (`IProperty`/`IShell` 抽象) · `:binderDetector` (经 JNI 的 C++) ·  
  `build-logic` (约定插件).
- 产品介绍与下载链接在 [根 README](README-cn.md), 英文版是 [README.md](README.md).

## 构建与验证

```bash
./gradlew assembleFossDebug      # CI 构建的目标 (GitHub Actions)
./gradlew testFossDebugUnitTest  # 单元测试 (目前只有模板)
./gradlew lintFossDebug          # Android lint
```

- 只改文档或注释的编辑不需要过构建那几档, 没有改代码.
- **先检查 LSP**: 编辑某个语言的代码之前, 先确认该语言的 LSP 在本环境是否已配置;  
  没配置就先协助用户配置  
  (例: Kotlin LSP 用 JetBrains ILS (IntelliJ Language Server), 即这门语言官方的服务器,  
  不用 `fwcd` 的 kotlin-language-server 这类第三方实现; ILS 由下面那个脚本启动, 没有编辑器插件托管它,  
  以 `--stdio` 启动其服务,  
  等到 `intellij/ready-for-test` 通知后用 **pull 模式** 拉 `textDocument/diagnostic`; ILS 从不主动 push,  
  `textDocument/documentSymbol` 可兼作 "真的在分析" 自检). 每批编辑之后, 先跑 LSP diagnostics (再加上面几档 Gradle  
  检查), 然后派新上下文的 subagent 在后台 review 未提交 diff, 把发现汇报给负责人后停下: 无论是否走 spec 流程, 没有负责人的明确指令一律不提交.  
  检查级的 LSP 发现 (如 "Use destructuring declaration") 当语法警告同等对待, 直接修, 不记文档. 本仓库 Kotlin 侧已接好:  
  `scripts/kotlin-lsp-diagnostics.js` 一条命令跑完整套握手,  
  解析 ILS 安装位置的顺序是 `KOTLIN_LSP_SERVER` → `KOTLIN_LSP_HOME` (约定的用户级环境变量,  
  指向发行版根目录) → `PATH` 上的 `intellij-server`. 索引缓存就是 gitignored 的 `.kotlin/lsp-cache/<process.platform>`  
  (`win32`, `linux`, `darwin`), 可随时删, 并按平台分目录: 每个平台有自己的 ILS 发行版,  
  一个平台热好的缓存另一个平台用不了.  
  且启动始终带 `--system-path` (不带它 ILS 每次启动都随机临时目录, 从头重索引);  
  重建用 `<ILS 发行版>/bin/warmup.py <repo> <repo>/.kotlin/lsp-cache/<process.platform> --server <ILS 发行版>/bin/intellij-server --build-tool gradle`.  
  warmup 和一次诊断运行都会做一次 Gradle 项目导入 (sync),  
  这次 sync 跑在构建 JVM 上, 也就是 Gradle 运行时 `JAVA_HOME` 指向的那个 JDK,  
  它和编译 app 代码用的工具链 JVM 不是一回事.  
  构建 JVM 必须跟项目的 Java 工具链版本一致  
  (该版本在 `gradle/toml/build.toml` 里以 `javaToolchain` 声明, 由 `build-logic` 约定插件消费),  
  不写死版本号, 也不写死 JDK 路径:  
  `:build-logic:convention` 这个包含构建会被编译成该工具链对应的 bytecode 版本,  
  所以比它旧的构建 JVM 加载不了约定插件, sync 就会失败.  
  而且这个失败是静默的: sync 失败时 ILS 不会报错退出,  
  它会降级成一个不完整的项目模型并照常答复诊断,  
  于是太旧的构建 JVM 会让分析器变瞎, 运行打印出一个假的 CLEAN  
  (0 items), 看起来像成功了. 所以预热或诊断之前,  
  要确保 Gradle 将要运行所用的 `JAVA_HOME` 是一个不低于工具链版本的 JDK;  
  `./gradlew -q javaToolchains` 能列出 Gradle 探测到的 JDK 以及各自的版本.  
  warmup 只有同时满足两点才算成功:  
  一是 Gradle sync 为 SUCCESS  
  (intellij-server 日志里出现 `"tool":"gradle","status":"SUCCESS"`),  
  二是对一个已知会报问题的哨兵文件跑诊断, 确实把那些问题返回了.  
  哨兵不是业务文件  
  (不要借用 `Theme.kt` 之类, 它的告警会在真实代码被修干净后消失):  
  `scripts/kotlin-lsp-selfcheck.js` 会往 app 源码包里写一个用完即弃的探针,  
  带一处故意触发的警告 (一个非小写开头的函数名),  
  对它跑诊断, 再在 `finally` 里删掉, 所以不会留进 git 或构建产物.  
  对探针跑出 CLEAN 就说明 sync 静默失败了, 而不是 warmup 成功了.  
  `scripts/kotlin-lsp-diagnostics.js` 本身原样继承 shell 的 `JAVA_HOME`,  
  所以要靠跑这个自检 (而不是只跑一次光秃秃的诊断) 才能抓出太旧的构建 JVM.  
  这条检查只覆盖 `.kt`: ILS 不为 Gradle 构建脚本 (`.kts`) 建立模型, 所以对 `.kts` 会直接失败,  
  而不是打印一个空的 CLEAN; 构建脚本要用 Gradle 构建本身来验证.  
  只有拉取成功且没有 ERROR 级条目时才返回 0: 运行失败或存在 ERROR 条目都返回 1  
  (退出码并不检查 sync, 所以信一个 CLEAN 之前, 要配合上面的 sync SUCCESS 和哨兵文件两项检查).  
  `ILS_READY_TIMEOUT_MS` (默认 1800000) 与 `ILS_CALL_TIMEOUT_MS` (默认 180000) 可在更慢的机器上覆盖这两处等待上限.  
  一次运行全程持有 `.kotlin/lsp-cache/<process.platform>.lock`, 并发的第二个运行会报出第一个的 pid 并 exit 1,  
  收到 SIGINT 或 SIGTERM 时脚本会先停掉服务器再退出.

## 构建约定

- Flavor (维度名 `IssueTracker`, 不是常见的 `mode` / `store`): `Foss` 是默认 (无跟踪, 版本名带 `-Foss` 后缀);  
  `Firebase` 是 Play 变体, 需要 `google-services.json`, 该文件已被 gitignore.  
  `AndroidApplicationFirebaseConventionPlugin` 以 `firebaseImplementation` 附加 Firebase 依赖,  
  并为 Foss 禁用 GoogleServices / Crashlytics 任务, 所以 Foss 构建永远不需要那个文件.
- Debug 构建开箱即用; debug 加了 `applicationIdSuffix = ".debug"`, 于是能和 release 并存安装. 处理应用身份 (权限, adb) 时记得这个后缀.  
  发布签名配置放在仓库之外, 在 gitignored 的 `local.properties` 里 (见 README).
- JDK 25 (Adoptium) 走在两条互不相干的轨道上: 代码编译靠 `jvmToolchain`,  
  Gradle Daemon 靠 `gradle/gradle-daemon-jvm.properties` (由 `updateDaemonJvm` 生成). 别把两者混为一谈.  
  用 `./gradlew -q javaToolchains` 查看.
- `:binderDetector` 原生代码需要把 NDK 和 CMake 版本钉在 `gradle/toml/build.toml` (当前 NDK 30.0.16248370, CMake 4.1.2),  
  与 CI 安装的版本完全一致, 改它们就要连同 CI 一起改.
- 版本目录拆成五个文件 (`gradle/toml/`: `build` / `android` / `kotlin` / `google` / `thirdParty`): 用 `libsAndroid`,  
  `libsBuild`, `libsKotlin`, `libsGoogle`, `libsThirdParty`. 没有默认的 `libs` 访问器. 依赖和版本只存在于这些目录里; 绝不在模块的  
  `build.gradle.kts` 里写裸坐标, 引用之前先决定依赖属于哪一类.
- 版本号档位: RC / Stable 的依赖或工具链版本可直接用于生产. Beta / Alpha / Canary 也可以, 但前提是研究透彻, 已知问题能被修复或规避, 且经过评估.
- SDK, build-tools 和 NDK 版本只放在 `gradle/toml/build.toml` (带 `isPreview` 开关), 经由 `build-logic` 约定插件到达模块.  
  绝不在模块脚本里硬编码 SDK 级别.
- Kotlin 2.4 带一组在 `build-logic` 声明的实验性编译器 flag, 按引入它们的 Kotlin 版本分组, 这些 flag 是有意为之, 不要删; 每次升级 Kotlin 都重新审一遍各组,  
  去掉已稳定的. 代码风格是 `official`.
- 仓库都做了内容过滤 (`google()` 用 `includeGroupByRegex` 收窄), 且 `FAIL_ON_PROJECT_REPOS`; `jitpack.io` 只存在于主构建的依赖仓库里.
- 配置缓存 (带并行与完整性检查) 和并行构建已启用; 自定义任务要保持配置缓存兼容.
- Build scan 用 Develocity 插件但从不发布 (`publishing.onlyIf { false }`), 只用于本地 scan.

## 模块细节

- `:app`, 即 应用本身; namespace 也就是 applicationId. 它的 `sourceSets` 注册了贴着包的 res 目录:  
  资源住在 `java/<package>/.../res` 路径下 (例如 `app/src/main/java/net/imknown/android/forefrontinfo/ui/home/res`), 没有  
  `app/src/main/res`, 新增的 res 目录必须在 `app/build.gradle.kts` 的 sourceSets 里注册.
- `build-logic`, 即 装着约定插件的 included build; 是所有模块 SDK 与构建取值的唯一来源.  
  共享配置 (SDK, desugaring, Java toolchain, Kotlin 编译器参数,  
  测试依赖) 位于 `build-logic/convention/src/main/kotlin/.../android/`; 模块只负责 apply 插件. 新增约定插件:  
  在 `build-logic/convention` 里实现, 然后在 `gradle/toml/android.toml` 的 `[plugins]` 段注册一个 alias.

## 架构

每个功能住在 `ui` 下自己的包里 (`ui.home`, `ui.others`, `ui.prop`, `ui.settings`), 分层相同:

```
Screen (Compose) → ViewModel (StateFlow) → Repository → DataSource
```

- 导航是 **Navigation 3** (`androidx.navigation3`), 不是主流的 Navigation 2: API 形状是 `NavKey` + 返回栈 + entryProvider  
  (`ui/navigation/NavKeys.kt`). 别按 Nav2 的词去想 (`NavHost(route = ...)`).
- **DI 用 Metro**, 也就是 编译期 DI, 有意偏离 Hilt / Koin 主流 (手写伴生 `Factory` / `viewModel(factory = ...)` 的时代已退役).  
  ViewModel 用 `@Inject` + `@ViewModelKey` + `@ContributesIntoMap(AppScope::class, binding<ViewModel>())`,  
  在 Navigation 3 的 entry 经 `metroViewModel<...>()` 解析; Repository 与 DataSource 是普通 `@Inject` 构造注入;  
  承载不了 `@Inject` 构造函数的叶子绑定住在 `di/AppGraph.kt` 的 binding container 里, 图工厂绑定 `MyApplication`.  
  Gradle 插件 / 运行时 / MetroX 构件在 `gradle/toml/thirdParty.toml` 里同 `version.ref` 锁升; 升 Kotlin 必须同步升 Metro,  
  先查官方兼容矩阵.
- 设置项由 `SettingsStore` (`ui/settings/repository/SettingsStore.kt`) 持有: 全部设置的键/值与写入口的唯一可观察归属, 绑定在图里注入给消费方,  
  它之外任何地方都不直接碰 SharedPreferences (图里的 SharedPreferences 绑定就是为喂它而存在).
- 可测试性来自 **接口优先的设计**, 不是 mock 框架:  
  `:base` 只定义 `IProperty` / `IShell` 及其默认实现 (`PropertyDefault` / `ShellDefault`), 没有聚合类,  
  图绑定两者  
  (`ShellLibSu` 经 `@ContributesBinding` 贡献 `IShell`,  
  `PropertyDefault` 由 `PropertyContainer` 的 `@Provides`  
  绑定), `PropertyReader` (`ui/common`) 包住 `IProperty` 提供共享回退.
- `BaseListViewModel` 用两个 `StateFlow` 驱动每个列表页  
  (`modelsStateFlow: StateFlow<List<MyModel>?>` (null 表示冷启动; 刷新时刻意保留上一个列表, 让界面绝不闪空) 和 `isLoadingStateFlow`),  
  外加 `loadJob` 去重: 别在重建时重新引入多余的加载. `onModelsLoaded()` 在每次加载落地后运行, 用来对账构建过程中改变了的状态.
- Compose 的稳定性注解 (`@Immutable` / `@Stable`) 是有意加的;  
  状态类一改就要重新评估 (照 `HomeViewModel` / `BaseListViewModel` 顶部注释的写法走, 它解释了注解*为什么*安全).
- 内置的 `lld.json` 数据会被复制到外部 files 目录 (`LldFileStore`), 用户允许联网时经 Ktor 在线刷新; 用 GitHub 还是 Gitee 的 URL 按时区选.
- 命令执行用 libsu 的 **非 root** 模式 (`ui/common/ShellLibSu.kt`, 带 `Shell.FLAG_NON_ROOT_SHELL`): 没有 root 层.

## 新增一个检测条目

当前流程 (列表顺序 = 调用顺序):

1. 把参数 key / shell 命令加进该功能的 `DataSource`.
2. 给该功能的 `Repository` 加一个返回 `MyModel` 的 `detect...()` 方法  
   (新的 Repository / DataSource 类经 `@Inject` 构造注入进图; 漏注解会在构建时报 `[Metro/MissingBinding]`).
3. 在该功能 `ViewModel.collectModels()` 里调用它: 调用顺序就是列表顺序.
4. 字符串加进该功能包的 `strings.xml` (默认英语) 以及三个翻译文件.
5. 若该条目需要带资源的新包, 在 `app/build.gradle.kts` 的 sourceSets 里注册它的 res 目录.
6. 用 `./gradlew assembleFossDebug` 验证.

## 代码规则

给新代码的规则, 它们编码的是已裁定的决定; 别让已有的债更糟:

- `ViewModel` 一律拼全, 绝不缩写成 `VM`, 标识符, 注释, 提交信息和文档里都不. `VM` 已经是 *virtual machine* 的缩写,  
  而这应用把虚拟机当作自己的一个检测主题 (进程/VM 架构, `getArchitecture`), 所以即便上下文足以看懂, 这个短写也是有歧义的. `UseCase` 和 `DataSource` 同样拼全,  
  不写 `UC` / `DS`.
- 不用静态事件总线: 绝不把 `SharedFlow`/`StateFlow` 放进 ViewModel 的伴生对象. 跨功能的数据经 repository 传递.
- UI 层 (Compose / ViewModel) 不用 try 处理业务异常: Repository 与 DataSource 要么自己处理好失败, 要么返回 wrapper 类型;  
  不带 `OrThrow` 后缀的函数以 "不抛" 为契约. 异常若到达 UI 层, 即为下层的 bug, 修在下层, 不在 UI 吸收.
- 全局的 `myAndroid` (`AndroidVersionExt`) 只有两个写入方:  
  启动时的 `initMyAndroid()` (`MyApplication.onCreate`, 来自运行时的 `Build.VERSION`),  
  以及 `HomeRepository.detectAndroid()` 里的已知值覆写. 绝不在别处赋值, `isAtLeast...()` 辅助函数到处都在读它.
- minSdk 是 24: 更新的 API 要用 `isAtLeastAndroidX()` 辅助函数或 `@RequiresApi` 兜住.
- 新代码用钉住的版本所允许的最新语法和标准库 API: 当前 Kotlin 版本支持的最新语法与 std-lib API, 当前 compileSdk 提供的最新平台 API, 当前依赖版本提供的最新 API,  
  绝不必就比工具链允许的更老的写法.
- 当能用的最新语法或 API 本身处于 Beta / experimental, 不要单方面采用, 摆出来问负责人怎么处理. `build-logic` 里已启用的实验性编译器 flag 是定下来的一组; 这条管的是新的 opt-in.
- 阻塞性工作 (shell, 系统属性, 文件, 网络) 跑在 `Dispatchers.IO`, 不是 `Dispatchers.Default`.
- `ShellDefault` 故意没有调用方: 它是保留备用的原生 shell 实现, 不依赖 libsu (在用的是 `ShellLibSu`). 别把它当死代码删; 如果将来启用它,  
  先修掉 `waitFor()` 之后读管道的死锁.

## 本地化

- 字符串按功能包拆分. 支持的 locale: 默认 (英语), `zh-rCN`, `zh-rTW`, `fr-rFR` (`localeFilters` + `generateLocaleConfig`);  
  引入新语言时把 locale 加进 `localeFilters`.
- 新的面向用户字符串一律先有默认英语条目; 能做到的时候把三个翻译文件同步更新.
- 按语言的排版标点 (中文的全角冒号, 法语冒号前的空格, ...) 只适用于面向用户的本地化文案. [文档规则](#文档规则) 里的 ASCII 标点规则照常管文档, 代码注释,  
  以及非文案的字符串资源 (存储 key, URI, 技术取值).

## 任务工作流 (docs/spec)

负责人 = 开发者本人. AI 负责搜索/分析/编码/测试/review; 负责人负责目标/边界/判断/提交.  
每道闸门以负责人的明确点头收尾, 无论是否走 spec 流程, 没有负责人的明确指令一律不提交; 未经同意不开始下一步.

- 建 `docs/spec/<yyyy-MM-dd-HH-mm-ss-Z>-<english-title>[-cn]/` (宿主机时钟时间戳, 短英文标题). 目录名以 `-cn` 结尾表示报告为非英文;  
  目录内文件按既定模式命名, **不加**语言后缀.
- 负责人预写 (AI 动笔前): plan.md 开头由负责人手写 2~3 句: 要解决什么, 自己的思路, 预测的最大风险; 任务完成前不改, 收尾对照. 【解决:  
  锚定 (Tversky & Kahneman) 与后见之明偏差, 预注册自己的判断, 先有预期再看 AI 的方案】
- AI 写 plan.md (任务级): 子任务拆分: 每个子任务的范围, 为什么单列 (最小高内聚, 可独立编译), 风险级别 (高 = 安全/数据/核心逻辑/生疏领域; 低 = 机械/样板), 生疏领域标注;  
  末尾留待学习清单. 报告语言由负责人按任务指定, 对计划与全部子任务报告生效. 【解决: 认知负荷理论 (Sweller), 小颗粒降低工作记忆的外在负荷; 风险分级防闸门疲劳】
- 计划闸门: 动工前派一个新上下文的 subagent 在后台 review `plan.md`; 报告发现的问题, 等负责人复核确认后, 自动提交 plan.md, 然后一次性预生成全部子任务的修改计划报告  
  (`subtask-01..NN`), 并派新上下文的 subagent 后台 review 这批报告, 与子任务 review 同款循环 (v1, v2, ...; 有发现等负责人复核确认, 只修确认项),  
  之后才进入子任务循环. 【解决: 自我审查盲区, 独立上下文消除上下文内的锚定】
- 进度账本: spec 目录携带 `progress.md`; AI 每过一道闸门更新: 当前位置, 各子任务闸门状态, 挂起物 (stash), 偏差记录, 下一步.  
  任何新会话/新 agent/新模型 接续时先读 AGENTS.md + 账本, 然后执行账本的"下一步"; 进行中的步骤 (如正在跑的后台 review) 不落账, 按账本重跑该步. 账本随下一笔提交入库.  
  每个会话开始 (或负责人说 "继续") 时, 扫描 `docs/spec/*/progress.md`; 存在未完成任务就按账本接续并向负责人确认, 负责人只需要记得 "继续" 这一个词; 同时有多个未完成任务时,  
  列出来让负责人挑.
- 子任务循环, 严格按序:
  0. 动工对照 (默认路径: AI 主导开发时, 负责人的生疏是结构性常态, 不按领域分级): 负责人给 目标句 + 问题单 (可空); 替换类任务加 覆盖核对 (用报告的 改动/不改动 清单对照总目标找漏项);  
     子任务的拆分与顺序是 AI 的职责, 负责人不预生成结构. AI 义务: 每个子任务先给 5~10 行概念 primer, 报告逐条回答问题单, 附覆盖核对清单. 报告过时先显式修订, 说开始才动工;  
     闸门节拍由负责人决定. 亲手实现不强制进入流程, 负责人在流程之外自行安排学习. 【解决: AI 主导开发中负责人结构性生疏, 无法跟上实现节奏 (自动化的讽刺, Bainbridge 1983), 学习放进审计  
     (问题单/覆盖核对/primer), 学习目标收窄为 架构概念 + 审查判断, 而非实现练习】
  1. 实施 (代码注释用负责人的聊天语言书写, 解释**为什么**, 对齐代码库注释密度; 提交闸门前把本次新增注释整体译为英语, 入库形态保持英语;  
     注释标点遵循 ASCII  
     规则) 并以 LSP diagnostics 加构建验证; 生疏领域的高风险逻辑, 负责人先无 AI 复现再对照; AI 同一问题重做 2 次不过即停, 由负责人接手或重拆. 【解决: 技能退化,  
     自动化的讽刺 (Bainbridge 1983), 程序性记忆靠练习保持; 防无限重试】
  2. 派新上下文的 subagent 在后台 review 未提交的 diff: v1, 之后每修一轮 v2, v3, ... 顺延;  
     后续轮可续用同一 reviewer 做 delta 复核 (其证据底座已核验, 更快更省), 但修复大面积重写 diff / 负责人推翻该轮大部分发现 / 续用上下文已臃肿 / 要做最终独立验收时,  
     应回到全新 reviewer; reviewer 优先换模型; 每个发现必须带可核对的证据 (file:line); 低风险子任务负责人可亲自读 diff 代替. 【解决:  
     自动化偏见 (Parasuraman & Riley) 与同源盲区, 证据要求把再认变成核对; 续用带来自证倾向, 由证据要求与上述回退触发点对冲】
  3. review 有发现: 报告负责人; 负责人复核确认确实要改的内容后, 只修本轮要求的部分. 改了就回到 2 进入下一版本; 确认无需修改则直接走第 4 步.
  4. 无新实质发现: 负责人放行时留一句自己的话 (为什么可以过); 低风险子任务可将 4/5 合并为一次点头. 【解决: 测试效应 (Roediger & Karpicke 2006), 再认升级为检索;  
     对抗流畅错觉与解释深度错觉 (Rozenblit & Keil 2002); 闸门疲劳】
  5. 提交该子任务 (Conventional Commits + 尾注), 再等点头才进入下一个子任务.
- 收敛护栏: review 只追实质问题, 主观风格偏好与过早优化不构成新一轮; 连续几轮没有新发现仍在打转时, 交由负责人收束, 不无限 review.
- 偏差账本: 想跳步可以, 但记一行 (任务/跳了哪步/为什么) 进 spec 目录的进度账本. 【解决: 行为经济学, 允许 + 留痕 胜过 禁止; 防流程无声烂掉与整体废弃】
- 收尾: 清待学习清单; 一次横切复盘 (AI 常错在哪类问题 → 沉淀成 review 检查清单). 【解决: 间隔效应 (spacing effect) + 元认知校准, 即 对抗流畅错觉的长期机制】
- 归档: 任务全部收尾 (账本的下一步为无) 后, 整个 spec 目录原样移入 `docs/spec/archived/`, 此后不再改动,  
  移入本身就把该目录声明为 **record** (见 [文档规则](#文档规则)); 台账头部仍写着 living 的, 视为被位置取代, 不是要就地修复的漂移.  
  续接扫描 (`docs/spec/*/progress.md`) 不深入下一层, 归档任务不会作为未完成任务再次出现.

## Git 与 CI

- PR 目标是 `develop` (默认分支). 提交信息遵循 Conventional Commits, **type + scope 用小写**: `fix(home): ...`.
- 每条提交信息末尾加一个 trailer, 写明产生它的 agent, 模型,  
  以及推理 effort 等级 (`off` / `low` / `medium` / `high` / `xhigh` / `max`, ...),  
  例如 `Generated with ZCode (GLM-5.3, effort: xhigh)`. 等级写作 `effort:`, 它是那个推理 effort 旋钮本身, 不是对推理的评价;  
  2026-09-22 及更早的提交写的是 `reasoning:`, 保持原样. 绝不猜测取值; 当 agent, 模型或 effort 无法确定时, 问用户要记什么.  
  禁的是你擅自写 `unknown`, 不是这个词本身:  
  问过之后, 用户确认某个值确实无法确定 (比如 Auto 模式下界面不显示 effort 等级),  
  这时写 `unknown` 就是正确的, 经同意的记录, 不是猜测.
- 从证据而不是习惯来命名 agent: 怎么找由你决定, 但要说明依据是什么, 并在写下来之前取得用户的同意.  
  版本级的名称是有区别的 (`Qoder CN` 与 `Qoder`, `Trae CN` 与 `Trae` 是不同的 AI 开发环境 (ADE)); 不要自造宿主形态的后缀, 例如 `IDE` / `CLI`,  
  除非用户要求.
- CI 只构建 `assembleFossDebug`. `master` 承载 `lld.json` 数据更新, 不要向它开 PR.

## 绝不提交

- 绝不提交或强制加入 (force-add) 凭据, 签名材料或本地配置 (`local.properties`, `keys/release.jks`, `google-services.json`),  
  根 `.gitignore` 已经排除了它们.
- 如果某个改动看起来需要提交这类文件, 先停下来问.

## 文档规则

在这里以及 `docs/` 下写东西的规则. 每份文档自己声明自己的状态; 本文件和它承载的约定是 **living**, 描述现状, 漂移是要就地修的 bug.  
[docs/dev/JOTTINGS.md](docs/dev/JOTTINGS.md) 是 **scratch** (草稿). 一份文档可以声明自己 **frozen** 或 **record**:  
它保留写作时的文本, 包括其中的更正与后续发现, 所以引用它做历史, 但绝不把今天的代码回填进去. 没有任何文档默认被冻结; 新文档在它另行说明之前都是 living. 要在 `docs/` 下加文档,  
说明它是哪种状态: **living**, **frozen / record** 或 **scratch**. 唯一按位置而不是按文件声明的状态是:  
`docs/spec/archived/` 下的 spec 目录就是 **record**, 归档移入即声明, 不编辑文件去携带它, 里面仍写着 living 的状态头视为被取代.

- 文档追踪的东西都有 ID, ID 在所有文档间通用, 引用时用 ID.
- **就地修正**文档: 一句话被证明是错的, 就改写它以及所有依赖它的下游内容, 让文档单独读来就是当前事实. 不要让被推翻的句子原样留着, 后面再挂一个括号或引块更正; 也不要给改过的句子标注它是哪次核对改的,  
  事实属于句子本身, 不属于谁在何时核对过的说明.
- 有冲突先问再裁: 两份文档不一致, 或文档与代码不一致且证据不能判定谁错时, 把两边摆出来让负责人裁定, 然后只改被裁定的那句. 更正不外溢: 另一句看起来像同样问题的话, 记到待问清单里, 不并进同一次修改.
- 文档只陈述负责人说过的话或代码展示的事实. 不为了让一行或一句话看起来完整而添术语, 检测条目或限定词; 术语表的一行只覆盖本文档自己用到的词汇, 最后一处引用已消失的行要先问, 而不是因为该术语广为人知就留着.
- **大白话**: 每句话都要能一遍读懂, 不自造简称, 不给后文要依赖的东西起比喻性的代称 (比如把一批工作项叫成 "桶"), 不用文档没介绍过的缩写. 沿用 AGENTS.md 或本文档已定义的术语;  
  确实要引入新术语, 在它第一次出现的地方说清. 同一标准也适用于代码注释, 也适用于跟负责人聊天时的回答.
- 留在正文里的: 实质与导航, 句子所划的范围或例外, 按 ID 的交叉引用, 代码符号. **谁在何时裁定了什么, 不是正文**: 带日期的裁定属于 `git log`, 不是每句话上的 "负责人裁定,  
  <日期>" 标签. 带版本行的文档只写版本并指向 `git log`; 它不积累修订日志.
- 文档默认用英语; 非英语的文件和目录带语言后缀 (中文: `-cn`). 永远以英文版为生效版本: 英文的 `README.md` / `AGENTS.md` 保住规范文件名, 旁边配一份中文翻译  
  (`README-cn.md`, `AGENTS-cn.md`). 链接跟随链接方文档的语言 (中文文档指向中文版 (`README-cn.md`), 英文文档指向英文版), 没有对应版本时, 指向存在的那份.  
  纯翻译不提它的孪生版, 也不自称中文版或翻译版: 不加翻译注记, 不加回指; `-cn` 后缀本身已经说明了关系. 文件名默认用英语; 沿用各目录现有的命名. 有理由时, 文档内部仍可使用任何语言.
- **任何语言都用 ASCII 标点**: 中文 (或日文) 文档用英文标点, 不用 CJK 标点: `,` `.` `;` `:` `!` `?` `(...)` `"..."`,  
  而不是 `，。；：！（）「」《》`. 一一对应: `，、` → `,` · `。` → `.` · `；` → `;` · `：` → `:` · `（）` → `()` ·  
  `「」『』《》` → `"` (嵌套时用 `'`) · `……`/`…` → `...`. 非标点字形保持原样: `·` `→` `←`, 图中的表格线符号, 以及状态 emoji. 破折号不在映射之列,  
  而是被禁用: 见下一条.  
  间距跟英语走, 不论两侧是什么文字: 标点后若还接文字, 后面空一格 (`每个条目, 每次检测`), `,` `.;:!?)` 前不空格, `(` `"` 内不空格. 另外两种情况: `(` 紧跟标识符表示调用,  
  中间不空格 (`collectModels()`); 数字之间的 `:` 不加空格 (`16:9`, `12:30`).  
  这条对每份文档都成立, 包括 **frozen**, **record** 和 **scratch** 的那些, 标点属于排版, 不是那些状态所保留的实质. 写新文字或重写一段时, 顺手把改动触及的部分一并转换;  
  在语言之间翻译时也要核一遍标点.  
  标题的标点和间距决定目录所指向的 GitHub 锚点, 所以重排一个标题的间距时, 要在同一次修改里改写所有解析到它的 `](#...)`. 显式 `<a id="...">` 锚点是稳定文本, 永不移动.
- **不用破折号**: 任何文档, 代码注释, 与负责人的聊天回答里都不用 em dash `—` (也不写 CJK 的 `——`); 每处按语义改写成对等词语:  
  释义关系用 `, 即 ` (英语: `, i.e. `), 换种说法用 `, 也就是 ` (英语: `, that is, `), 真正的插入语放进括号, 单纯衔接用普通逗号. 像本条这样在行内代码里点名词符号,  
  是唯一的例外.
- **长行按语义折行** (规则先立; 存量文件的一次性重排已完成): 一行太长时, 在句子或分句边界折行 (逗号收尾的分句是合法断点, 但特别短的分句可以不换行, 和后面的内容并作一行; 简短的并列枚举不拆开),  
  目的是行不整体过长, 不定死宽度. 标题, 链接目标, URL, 代码块, 行内代码永远不折, 各占一个源码行. 表格行不拆成两个源码行,  
  但过长的单元格可以在单元格内用 `<br>` 折行 (源码里的裸换行会破坏表格). 段落, 列表项, 块引用里的每个折行行尾都带两个空格 (或 `<br>`),  
  让断点在渲染后同样生效; 块的最后一行不带. 这些空格是承载语义的: 绝不能删除或折叠. 折行属于格式, 对 **frozen**, **record** 和 **scratch** 文档同样适用. 代码注释遵循同样规则;  
  与代码同行的注释保持一行, 折行时绝不能碰注释以外的任何内容.  
  折行时绝不能碰注释以外的任何内容.
- 任何文档 (memory 文档在内) 都不得含敏感信息: 隐私数据, 密码, 密钥, 证书.
- `docs/` 下的文档保持简短; 一份长得太长时, 沿自然缝隙拆开, 保留一个 README 做索引页.
