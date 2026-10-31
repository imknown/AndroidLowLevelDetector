# 优先修复报告 (2026-10-17)

> **状态**: living (未归档, 可原地修订).  
> **首要依据**: 四份对当前代码的独立通读: `archived/deepseek-v4.1-flash-high.md` (条目号 `PA-nn`),  
> `glm-5.3-flash-high.md` (`SS/AR/UD/DP/MD/SE/CP/AP/TE-nn`),  
> `mimo-v2.6-flash-high.md` (`SSOT/UDF/ARCH/PATTERN/MOD/SEC/CP/AP/TEST-nn`),  
> `qwen-3.8-flash-xhigh.md` (`SSOT/UDF/ARC/PAT/MOD/SEC/CP/AP/TST-nn`).  
> 四份的编号前缀有重叠: mimo 与 qwen 都用 `SSOT/UDF/MOD/SEC/CP/AP`, 只差编号是否补零; glm 与另两份都用 `CP/AP`.  
> 所以引用一律带来源名, 例如 `qwen SSOT-01`, `mimo SSOT-1`, `glm CP1`.  
> 报告之间冲突时以代码复核为准; 各条的文件与行号出自被引的那份报告.  
> **次要依据**: `issues-cn`, 更早的调查, 与它的明确分歧单列在 [与 issues-cn 的分歧](#与-issues-cn-的分歧) 一节.  
> **范围**: 去掉 CI / NDK / JNI; Test 已纳入.  
> **排序原则**: 主体按 "会不会让 App 报出假结论 / 用户可感的错误" 排, 不按修改难度, 复杂度, 工时排;  
> 测试那一组另按 "它是不是前面那些重构的防回归测试, 以及它覆盖的是不是 App 的判断核心" 排.  
> **修法一节的地位**: 每组的 "彻底修法" 只是修改实装时的参考, 不是子任务列表;  
> 子任务怎么切, 每个子任务怎么提交, 归开工时写的计划.  
> **条目号**: 沿用四个来源的既有编号, 本报告不自造编号.

## 结论

最该先修的是下面几组: 第一组 deepseek, glm, mimo 三份定 "高" (qwen 也把它排进判定正确性那一批);  
第二组与第三组只有 glm 定 "高", 另两份定 "中".

1. **全局版本事实 `myAndroid`** (deepseek PA-02, glm SS1, mimo SSOT-1, qwen SSOT-01).  
   四个来源都指它: 同一台设备有两个版本来源, 其中之一可变, 且写入后不复位.  
2. **加载的失败边界与检测执行方式** (deepseek PA-10, glm AR9, mimo PATTERN-1, qwen UDF-01).  
   每个检测块都有守卫, 但把这些块合起来的那一层没有; 检测全部一个接一个执行, 首屏要等整条检测跑完.  
3. **结构与分层的根本原因** (glm AR1, glm AR2, glm MD1, glm SS2, mimo ARCH-4, qwen ARC-01, qwen ARC-02, qwen ARC-03, qwen ARC-05, qwen MOD-02).  
   `ui.home` 与 `ui.settings` 互相引用成环, `ui.common` 反向依赖 home 的模型, `:app` 顶着 `:base` 的包名,  
   `MyApplication` 的伴生对象是与 DI 图并行的第二个入口, 且数据层直接产出界面模型.  
4. **`lld.json` 的校验** (glm SE1, glm SE2, mimo SEC-1/2/3/4, deepseek PA-04, deepseek PA-24, qwen SSOT-06, qwen SEC-02, qwen SEC-03).  
   文件写在应用专属的外部目录, 不校验任何完整性, 不校验 schema, 不查 HTTP 状态, 备份规则是空元素.

其后是 Prop 页对每个键各查一次 (glm AP1, mimo AP-3, qwen AP-04), 启动与首屏性能加 Baseline Profile  
(deepseek PA-27, deepseek PA-32, qwen AP-05, qwen AP-06), 把判断与展示分开 (数据层直接产出展示内容),  
测试 (第 9 组), 以及一批安全与卫生的低分项.

下面每组的 "为什么优先" 引用四份报告的结论, "彻底修法" 是代码复核后的方案.

## 第 1 组: 全局版本事实 `myAndroid` (deepseek PA-02, glm SS1, mimo SSOT-1, qwen SSOT-01)

**证据**: `AndroidVersionExt.kt:53-64` 的 `MyAndroid` 四个字段全是 `var`, `:62` 是顶层 `val` 实例;  
写入方一是 `:104-109` 的 `initMyAndroid()` (由 `MyApplication.kt:92` 调用),  
写入方二是 `HomeRepository.kt:124-131` 的 `detectAndroid()`, 它把 lld 里 known 那一行赋进全局;  
读取方是 `:114-125` 的 `isAtLeast*` (经 `sdkInt get() = myAndroid.api`),  
`:132-134` 里读它的 `isLatestPreviewAndroid` (读 `myAndroid.apiFull`),  
以及 `HomeRepository.kt:1130` 里比 targetSdk 用的阈值. 全仓没有 `volatile` / `synchronized` / `AtomicReference` / `Mutex`.

**为什么优先** (deepseek, glm, mimo 定 "高"; qwen 把它排进 "判定正确性" 一批):  
同一个进程里, Home 加载前后对同一台设备可以给出不同的 API 判定;  
`detectAndroid()` 又是四次独立赋值 (`api` 到 `apiFull` 到 `version` 到 `dessert`),  
两步之间落进来的读会拿到 "新 api 配旧 version" 这种跨代的组合 (glm SS1 与 mimo SSOT-1 记读写无同步;  
四次独立赋值与跨代组合这一层见 issues-cn #17);  
qwen SSOT-01 补充三处后果: `Build.VERSION.SDK_INT >= X || sdkInt >= X` 这种或条件只能把级别抬高不能压低,  
`isAtLeast*` 保护的调用可能在更低的预览设备上执行; 覆盖值只写不清, 没有任何复位路径;  
过时 APK 那一行的阈值跟着数据文件驱动的覆盖值走, 而不是跟着设备真实级别走.  
载荷经这里写回后, 之后所有 API 检查都被改写 (mimo SEC-3, glm SE2).  
它也是第 3 组的前置.

**彻底修法**: `MyAndroid` 四个字段改成 `val`; 新增一个只读入口 (例如 `AndroidInfo.current`, 第一次用到时才构建);  
`isAtLeast*` / `isLatest*` 只读它;  
lld 的修正改成纯函数 (例如 `enrichWithLld(base, lld): MyAndroid`), 返回新对象, 由 `HomeViewModel`  
显式传给 `detectAndroid(lld, androidInfo)` 与过时 APK 的过滤, **不再写回任何全局**;  
`initMyAndroid()` 从 `MyApplication.onCreate` 删除.  
一处必须小心: `initMyAndroid()` 的第一句就读全局 (`isAtLeastAndroid16()` 经 `sdkInt`),  
直接把 `var` 改成 `val` 会自我引用, 推导结果要先放进局部变量, 最后一次性构造.

**关联**: `Theme.kt:322` 绕过 `isAtLeastAndroid12()` 直接写 `Build.VERSION.SDK_INT >= S` (deepseek PA-05);  
同一条 API 门槛在 ViewModel 的守卫与仓库的 `@RequiresApi` 里各写了一次 (mimo MOD-5).

## 第 2 组: 加载的失败边界与检测执行方式 (deepseek PA-10, glm AR9, mimo PATTERN-1, qwen UDF-01)

**证据**: `BaseListViewModel.kt:60-71` 的 `startLoad()` 是一个没有 `try` / `finally` 的  
`viewModelScope.launch`, `setLoading(false)` 只经 `setModels()` 可达;  
`IShell` (`base/.../shell/IShell.kt`) 没有声明 "不抛", 实现 `ShellLibSu` 直接转发 libsu;  
`HomeViewModel.kt:221-223` 的 `getMounts()` 与 `PropRepository.kt:70` 的 shell 调用  
(它属于 `:66` 开始的 `getBuildProp()`) 是检测链里没有守卫的两处;  
`PropertyDefault` 是 object, 类初始化失败会抛 `ExceptionInInitializerError` (它是 `Error`, 守卫只抓 `Exception`).  
检测侧: `HomeViewModel.kt:211-241` 里 23 个构造模型行的调用 (外加一次 `getMounts()`)  
全部挤在同一个 IO 块里依次执行, `OthersViewModel.kt:32-98` 与 `PropViewModel.kt:29-39` 同样;  
`BaseListViewModel.kt:26-27,65-70` 的 `modelsStateFlow` 初始是 null, 首屏要等 `collectModels()` 全部返回.  
`BaseListViewModel.kt:61-63` 在 `loadJob?.isActive` 时直接 return, 加载中用户主动发起的一次下拉会被丢掉,  
既不补做也不提示 (qwen UDF-05); 首次加载的时机放在 UI (`MyModelListScreen.kt:59`), 行序又在三处 ViewModel 手写 (qwen UDF-06).  
`MountDataSource.kt:24-36` 不看 `isSuccess` 就返回解析结果 (qwen AP-09), shell 失败时得到空列表,  
而 `HomeRepository.kt:434-436` 的 `isThe2siSar` 与 `:727-729` 的 APEX 判断把空列表当证据.

**为什么优先** (glm 定高, deepseek 与 mimo 定中; qwen UDF-01 记同一条): 这条路径通向的是一直转圈或崩溃, 不是一行错误提示;  
而且这是用户每次都付的代价: 首屏要等整条检测跑完 (mimo AP-1, qwen AP-01, qwen CP-05),  
慢设备上依次执行的 shell 与包枚举直接相加 (deepseek PA-33, glm AP5, mimo AP-2).

**彻底修法**: 按 `AGENTS.md` 的代码规则, UI 层不处理异常, 所以正解是让下层不抛, 不是让 UI 吸收异常:  
1. `IShell.execute` 明确 "不抛" 契约 (它没有 `OrThrow` 后缀), `ShellLibSu` 与 `ShellDefault` 各自  
   把 libsu 与进程异常转成 `ShellResult(isSuccess = false)`, 并给 `waitFor` 加超时.  
2. 没被守卫的调用点 (`getMounts`, `getBuildProp`) 把 "失败" 当成一种正常结果处理 (结果类型或 wrapper),  
   不再让异常从 `collectModels()` 里抛出.  
3. `PropertyDefault` 的类初始化加保护, 或把隐藏 API 反射失败变成可判定的结果 (见第 8 组的 qwen ARC-06).  
4. 执行方式: 检测项改成一份清单 (每一项含身份与执行函数), 由一段统一的代码按清单执行,  
   有上限的并发与取消检查都在那段代码里, 同时保住固定的展示顺序与检测项之间的先后依赖;  
   首屏分批显示 (已经算好的行先显示). 这一步与第 3 组的检测项清单是同一件事.

**关联**: `BaseListViewModel.kt:79-87` 的按 `targetIndex` 就地替换 (deepseek PA-13, qwen UDF-03)  
与列表里条目的身份 (deepseek PA-18) 随清单一起改;  
`AppRoot.kt:97-116` 的 16 份重复注册 (mimo PATTERN-7) 与四条从不放第二个条目的返回栈 (mimo PATTERN-11, qwen ARC-07) 归第 3 组.

## 第 3 组: 结构与分层的根本原因 (glm AR1, glm AR2, glm MD1, glm SS2, mimo ARCH-4, qwen ARC-01, qwen ARC-02, qwen ARC-03, qwen ARC-05, qwen MOD-02)

**证据**:  
- **特性包互相引用成环**: `HomeRepository.kt:49` 引 `ui.settings.datasource.AppInfoDataSource`,  
  `SettingsRepository.kt:14` 反向引 `ui.home.datasource.LldFileStore` (glm AR1, mimo MOD-2, qwen ARC-02);  
  `ui.common/AndroidVersionExt.kt:11` 引 `ui.home.model.Lld`, 反向 `HomeRepository` 引一排 `ui.common.*` (glm AR2, mimo MOD-1);  
  `BasicDataSource.kt:7` 引 `ui.home.datasource.AndroidDataSource` (glm AR1, mimo MOD-2, glm AR5).  
- **两个模块共用一个包名**: `MyApplication.kt:1` 在 `:app` 里声明 `net.imknown.android.forefrontinfo.base`,  
  与 `:base` 模块的命名空间相同 (glm MD1, mimo MOD-6, deepseek PA-14, qwen ARC-05).  
- **静态取全局**: `MyApplication.kt:74-85` 的 `instance` / `sharedPreferences` / `getMyString`  
  是与 Metro 图并行的第二个入口, `AppGraph.kt:206-207` 的 `sharedPreferences` 提供者声明了  
  `app: MyApplication` 参数却不使用它, 直接读伴生对象 (glm SS2, mimo ARCH-4, glm SS3, deepseek PA-15, qwen ARC-03, qwen PAT-01).  
- **数据层直接产出展示内容**: `HomeRepository` 的 24 个 `detect*` 直接构造 `MyModel`  
  (含 `StatusColor` 与本地化文案), `getMyString` 全仓 99 处;  
  `ui.common` 既放数据基础设施又放 Compose 绘制, 还反向依赖 feature  
  (mimo ARCH-1/2/3, mimo PATTERN-2, glm DP1, deepseek PA-16, qwen ARC-01, qwen PAT-03, qwen PAT-04).  
- 仓库职责: `HomeRepository` 1211 行 (glm AR3), `OthersRepository` 基本只做转发 (glm AR4),  
  `AndroidDataSource` 是没有行为的常量表 (glm AR5, mimo MOD-7, qwen PAT-02), ViewModel 直接用到 DataSource 的常量 (glm AR6),  
  `:binderDetector` 的边界已经失效 (glm MD3), `fileTree("libs")` 指向不存在的目录且 `:base` 侧用 `api` 暴露  
  (glm MD7, mimo MOD-9, qwen MOD-03).  
- **建图时序**: `MyApplication.kt:58` 建图, `:90` 才赋值 `instance`, 而 `SettingsStore` 字段初始化读 `instance`,  
  提前解析 `AppGraph.settingsStore` 会抛 `UninitializedPropertyAccessException` (qwen ARC-04).  
- **资源目录与命名**: 资源目录靠构建脚本手工登记 (`app/build.gradle.kts:31-42` 十条), 漏加时资源不参与合并 (qwen MOD-01);  
  两个同名 `FingerprintDataSource` 义异, 分区清单硬编码 10 项  
  (`ui/others/datasource/FingerprintDataSource.kt:18-20`), 而公开 API `Build.getFingerprintedPartitions()` 可用 (qwen MOD-04);  
  `BaseViewModel` 是空壳, `AndroidDataSource` 与 `AppViewModelFactory` 里两个 map 恒为空 (qwen PAT-02).  
- **格式串**: 格式串与位置参数数组分离 (`HomeRepository.kt:158,263,919-927,1133-1166`, `SettingsScreen.kt:226-232`) (qwen PAT-06).

**为什么优先** (glm 定高, mimo 定中; qwen 把它排在 "判定正确性" 之后, 与 TST-04/TST-08 一起):  
从包名判断不出依赖方向, 任何 feature 都抽不出来, 单独推理不了;  
它与第 2 组的检测项清单, 第 1 组的改成不可变是同一轮工作.

**彻底修法**: 采用 `docs/dev/2026-10-02-module-structure-cn/README.md` 里的目标结构 (那个文件标注未定, 动工前先定):  
`:core` 放与平台无关的模型与工具 (含 `AndroidDataSource` 的常量, `Lld`, 日期与版本比较),  
`:feature/*` 各自独立, 含 Screen + ViewModel + Repository + 专属 DataSource,  
`MyApplication` 移到 `:app` 根包, `:base` 只留 `IShell` / `IProperty` 接口.  
先把 Metro 图接完 (已基本完成), 再拆模块; 搬移只是移动文件与改引用,  
但必须与文档两种语言的路径一起改: `ShellLibSu` 的路径就写在 `docs/agents-cn/architecture.md` 里.

## 第 4 组: `lld.json` 的校验 (glm SE1, glm SE2, mimo SEC-1/2/3/4, deepseek PA-04, deepseek PA-24, qwen SSOT-06, qwen SEC-02, qwen SEC-03)

**证据**: 文件写在应用专属的外部目录 (`MyApplication.kt:60-71` 的 `getDownloadDir`), 读回时没有来源校验,  
只查 "能否解析" 与 "version 不旧于内置资产" (`LldFileStore.kt:45-64`, 且是直接比较字符串) (glm SE1, mimo SEC-2);  
在线拉取用固定 HTTPS 地址, 直接取 `response.body()`, 不看 HTTP 状态 (`LldDataSource.kt:41-47`) (glm SE2, mimo SEC-1);  
`SCHEME_VERSION` (`Lld.kt:8`) 全仓只有定义没有使用, `scheme` 只被反序列化;  
`Json { ignoreUnknownKeys = true }` (`JsonExt.kt:6`);  
`api` 字段直接 `toInt()` (`AndroidVersionExt.kt:132,134`, `HomeRepository.kt:126`) (mimo SEC-4, deepseek PA-04, deepseek PA-24, qwen SSOT-02, qwen SSOT-06);  
模型与数据已经不一致: `Toyboxes` 只有 stable/support/master, 内置资产有第 4 个键 mainline, 被静默丢掉.  
备份规则 `backup_rules.xml` 与 `data_extraction_rules.xml` 都是空元素, 按平台语义等于默认全量备份 (deepseek PA-25, mimo SEC-8, qwen SEC-08).  
在线数据的 URL 路径段来自构建机的 git 状态 (`AndroidApplicationConventionPlugin.kt:37-44` 进 `BuildConfig.GIT_BRANCH`,  
`LldDataSource.kt:41` 拼进 URL), tag 或 detached HEAD 时得到 `HEAD`, 非 git 目录得到 `unknown`,  
而失败被静默降级成离线 (`HomeViewModel.kt:108-111`) (qwen SEC-02);  
目标路径是目录时会被 `deleteRecursively()` (`LldFileStore.kt:34-40`), 而路径在外部存储 (qwen SEC-04);  
配置期还读 git 与时钟 (`AndroidApplicationConventionPlugin.kt:36-44`),  
`settings.gradle.kts:63,69` 有两个版本直接写在脚本里 (qwen MOD-07).

**为什么优先** (glm SE1 高): 这份文件决定安全相关的行 (安全补丁, targetSdk 阈值, WebView 结论),  
一旦被覆写或被上游账号失陷替换, 每一行结论立刻翻转;  
而且载荷还会写回全局 `myAndroid` (第 1 组的 mimo SEC-3), 影响扩大到 "所有 API 检查".  
与第 3 组的结构问题不同, 这是数据是否可信的问题.

**彻底修法**: 解析后校验 `scheme == SCHEME_VERSION`, 不符时给出明确提示并回退;  
`api` 改用 `toIntOrNull` 加默认值; 让模型与资产一致 (补 `mainline` 字段或删掉资产里的这个键, 以数据实际存的为准);  
在线路径校验 `response.status.isSuccess()`; 比较版本与日期统一解析后再比;  
文件改写进内部 `filesDir` (回退路径已经存在), 外部目录只留给用户导出的副本, 或对已存文件做指纹比对;  
两个备份规则显式排除 lld.json.

## 第 5 组: Prop 页对每个键各查一次 (glm AP1, mimo AP-3, qwen AP-04)

**证据**: `SettingsDataSource.kt:11-26` 先用反射取出 `Settings.System/Secure/Global` 的常量名当键列表,  
`PropRepository.kt:50-63` 再逐个键调 `getString`, 每个键一次跨进程 binder 往返; 键列表还每次重新反射一次.  
qwen AP-04 数出三张表的公开常量共 195 个 (System 92, Secure 62, Global 41), 也就是至少 195 次查询加 195 次方法查找.

**为什么优先** (glm 定高, mimo 定中; qwen 把它排进性能那一轮): 每次打开 Prop 页都会执行, 键的数量是几百量级, 低端机上能直接感觉到;  
而且这种取键的方式会漏掉设备动态写入, 不在 SDK 常量里的设置项.

**彻底修法**: 每张表用一次 `contentResolver.query(NAME, VALUE)` 取回全部键值对,  
用显式的表枚举取代 KClass 反射; 键集合改成 "设备上实际存了哪些行".  
三点注意: 这个调用按 `AGENTS.md` 走 `Dispatchers.IO`;  
三个 `CONTENT_URI` 与 `query()` 的 API 等级按 `build.toml` 的 minSdk 核对;  
行集合变化会牵动 `LazyColumn` 的 key 与失败行的身份, 与第 3 组的条目身份一起定.

## 第 6 组: 启动与首屏性能加 Baseline Profile (deepseek PA-27, deepseek PA-32, qwen AP-05, qwen AP-06)

**证据**: `settings.gradle.kts:54-56` 只有三个模块, 依赖里找不到 `androidx.profileinstaller`,  
也没有生成模块 (deepseek PA-27, qwen AP-06);  
`MyApplication.onCreate` 在主线程做了这些: 建图 (`:58`), 取 `settingsStore` 从而触发同步读  
`SharedPreferences` 并可能条件写 (`:103`, `SettingsStore.kt:116-143`),  
`initMyAndroid()` 用反射遍历 `Build.VERSION_CODES` (`:92`), 配置 libsu builder (`:105-116`)  
(deepseek PA-32, qwen AP-05);  
`PropertyDefault` 每次读取都调 `getDeclaredMethod` (`PropertyDefault.kt:10-18`) (glm AP4, qwen AP-03);  
`RomDataSource.getDefaultUserAgentOrThrow` 为一行信息调 `WebSettings.getDefaultUserAgent` (`RomDataSource.kt:26`) (deepseek PA-34);  
过时 APK 那一行每次加载都全量枚举 `getInstalledApplications`, 同一次刷新还可能算两遍  
(`HomeRepository.kt:1117-1177`, `HomeViewModel.kt:238-240,266-275`) (deepseek PA-35, glm AP3, qwen AP-02).  
Compose 侧还有: `contentType` 只有两类, 而 Prop 页行长度差别很大 (qwen CP-06);  
没有打开 Compose 编译器的报告, 也没有 `stabilityConfigurationFiles` (qwen CP-07);  
已知的 Compose lint 结论被写进 `app/lint-baseline.xml` 而不是处理 (qwen CP-08);  
`AndroidManifest.xml:15` 的 `gwpAsanMode="always"` 对所有构建类型和两种 flavor 生效, 没有测量支撑 (qwen AP-07).

**为什么优先** (deepseek 两处 "高"; qwen 把性能排成独立一轮): 这些都是每次启动或每次加载都会发生的成本, 与第 2 组的首屏问题相加.  
Baseline Profile 是本项目收益最大的一处改动 (启动期本来就要反射, 执行 shell, 枚举应用包).

**彻底修法**: 把 SharedPreferences 读取与反射移出首帧路径 (第 1 组改成用时才构建已顺带解决反射);  
`PropertyDefault` 把 `Method` 缓存成对象成员; 过时 APK 的枚举结果每次加载只算一次,  
就地替换那一行的路径复用本次已经解析好的 `Lld`; WebView 用户代理那一行改成用时才算, 或标注它的代价;  
生成 Baseline Profile 并接进构建.  
另有 Compose 侧的低分项: `AppRoot.kt:97-100` 每次重组都重建导航参数 (deepseek PA-28),  
`ScrollBarExt.kt:41` 在组合期读滚动状态 (deepseek PA-29, glm CP5, mimo CP-1, qwen CP-02),  
超长内容放在一个 `Text` 里并做高度动画 (`HomeRepository.kt:1163-1167`, `MyModelCard.kt:55,88`)  
(deepseek PA-30, mimo CP-4, qwen CP-03, qwen AP-08),  
`Theme.kt:321-329` 没有 `remember` 就重建 ColorScheme (deepseek PA-31, glm CP3, qwen CP-01),  
`MyModelCard.kt:49` 每一行都是空 `onClick` 的 Card (glm CP1, mimo CP-3, qwen CP-04, 见分歧一节).

## 第 7 组: 把判断与展示分开 (数据层直接产出展示内容)

这一组是 glm 与 mimo 定 "中", deepseek 的相关条目定 "低", qwen 归入 `ARC-01` 并说 "改哪一边要负责人判定";  
但它在结构上是最深的一处, 也是 mimo `MOD-1` / `MOD-2` 与 glm `AR1` / `AR2` 的共同原因:  
`MyModel` (界面模型, 带 `StatusColor` 与本地化详情) 被下推到数据层,  
于是仓库必须有 `Context` 才能干活 (`getMyString` 99 处, `HomeRepository` 69 处),  
`ui.common` 反向依赖 home, 循环也就跟着出现  
(mimo ARCH-1/2/3, mimo PATTERN-2, mimo PATTERN-6, glm DP1, glm SS5, qwen ARC-01, qwen PAT-03, qwen PAT-05).

**连带一起处理的**: 判断结果没有类型, 失败就地折成结论或空集合:  
`PropertyReader.getString` / `getBoolean` 在失败或条件不满足时返回本地化占位或 `false`  
(`PropertyReader.kt:24-44`), 判定层再拿文案去比 (deepseek PA-01, glm SS5, mimo MOD-4, qwen PAT-05, qwen AP-09);  
`MountDataSource.kt:27` 不看 `isSuccess` 就遍历 `.output`;  
逐行各写各的比较 (glm SS6, deepseek PA-06, qwen SSOT-02, qwen SSOT-03):  
`detectSecurityPatch` 直接比较整串, 再用 `substringBeforeLast('-')` 取年月 (`HomeRepository.kt:285-311`),  
`detectMainline` 把包的 versionName 与日期相比 (`:657-663`),  
`detectBuildId` 用了 250205-250101 这样的固定偏移 (`:249`),  
`detectVndk` 直接比 `vndkResult >= lld.android.stable.api` (`:697`);  
`HomeViewModel` 里用 try 编排 lld 的联网与离线回退 (deepseek PA-09).  
设置项的键与存储值放在 string 资源里, 同一映射在 `SettingsStore.kt:33-45`, `:79-91`, `arrays.xml:4-27` 抄了三份 (qwen SSOT-04);  
标题在组合期用 `stringResource`, 明细在数据层用 `MyApplication.getMyString`, 同一行的两半来自两个 Context 的 resources (qwen SSOT-05);  
补丁路径重读离线文件, 而列表可能由在线 payload 构造 (`HomeViewModel.kt:267-286`) (qwen UDF-02);  
`outdatedOrderChanges` 与 `loadStartGeneration` 是手写的世代判定 (qwen UDF-04);  
彩蛋计数是私有 `var`, 通过返回值表达事件 (qwen UDF-07);  
`OrThrow` 后缀不再表示 "会抛" (`LldDataSource.kt:50`) (qwen MOD-05).

**彻底修法**: 取数层返回 "发生了什么" (有值 / 无值 / 失败), 判断与格式化分开,  
展示集中到 UI 层唯一的一处 (把结果翻成 `MyModel` 的那一个函数); 比较版本与日期统一解析后再比;  
编排与回退从 ViewModel 下沉到数据层.  
与第 2 组的检测项清单是同一轮: 清单里的每一项持有自己的结果类型与比较方式.

## 第 8 组: 安全与卫生的低分项

- deepseek PA-22, mimo SEC-12, qwen SEC-01: `QUERY_ALL_PACKAGES` 加了 `tools:ignore`, 读取范围可以缩小到 flags / targetSdk / packageName.  
- qwen ARC-06, deepseek PA-23: 隐藏 API 反射 (`PropertyDefault`, `HomeRepository.getBuildInWebViewProvidersAndroid7`,  
  `SettingsDataSource`, `PropertiesDataSource`) 失败时不留下任何可见痕迹, 且每次读取都重新查找方法.  
- glm SE8: 系统属性 `ro.boot.product.vendor.sku` 没清洗就拼进文件路径 (`HomeRepository.kt:523-531`).  
- glm SE4, qwen SEC-09: `keys/debug.keystore` 被 git 跟踪, 密码写在 `app/build.gradle.kts:74-77`;  
  release 侧 `local.properties` 缺失时静默产出未签名包而不报错 (qwen `:51-65,91`).  
- mimo SEC-6, mimo SEC-7, deepseek PA-37, qwen SEC-06: debug 构建把完整的网络流量与 shell 输出写进 logcat;  
  14 处 `printStackTrace()` 里 8 处没有 debug 门槛; 有一处 release 日志可能带出文件内容.  
- mimo SEC-11, deepseek PA-26, qwen SEC-07: Firebase flavor 带统计与崩溃上报, 值得做一次明确的隐私决定.  
- qwen SEC-05: `IShell.execute(cmd: String)` 收一整行命令, 备用的 `ShellDefault` 走 `sh -c`; 今天调用点全是常量, 记的是形状.  
- qwen ARC-07: `AppRoot.kt:123-133` 的 `onBack` 只有 `finish`, 与 "每个 tab 一条返回栈" 的骨架不匹配,  
  而 manifest 声明了 `enableOnBackInvokedCallback="true"`.  
- qwen ARC-08: 仓库里留着大段注释掉的代码与无人使用的分支 (`Theme.kt:118-282` 等), `ShellDefault` 带读后死锁.  
- 卫生杂项: `OthersRepository.kt:85-89` 的 catch 分支返回 `Unit`, 使 `binderVersion` 退化成 `Any` (deepseek PA-36);  
  `ExceptionExt.kt:3-4` 的 `fullMessage` 丢掉整条 cause 链, 且异常类名会进用户可见的行 (qwen TST-07, issues-cn #24);  
  `getBuildProp` 用 `+=` 累加是 O(n^2), `SettingsDataSource` 在过滤器里带副作用 (deepseek PA-38);  
  `ShellDefault` 有意保留, 但先 `waitFor()` 再读管道可能死锁 (deepseek PA-21, mimo PATTERN-10, glm DP3, qwen ARC-08);  
  `MOD-06`: flavor 维度名与实际用途不符 (`Flavor.kt:9-16,25`), 版本名标记只给了一侧.

## 第 9 组: 测试 (deepseek PA-39..PA-47, glm TE1..TE4, mimo TEST-1..TEST-4, qwen TST-01..TST-21)

**为什么优先**: qwen 的处理顺序把它放第一步 ("先让回归可见"), glm 说第一批纯 JVM 测试不受重构影响可先行;  
现在的防回归测试几乎是零: 全仓 5 个测试文件里只有 `base/src/test/.../DateTimeExtTest.kt` 一个真测试  
(deepseek PA-39, glm TE1, mimo TEST-1, qwen TST-01).

**不等重构就能做的**:
- 文档里的命令跑不到唯一的真测试: `testFossDebugUnitTest` 是 flavor 任务, 只有 `:app` 有 flavor,  
  `:base` 的任务是 `testDebugUnitTest` (glm TE2, qwen TST-02).  
- 插桩测试现在无法运行: 指定的 runner `androidx.test.runner.AndroidJUnitRunner` 不在 classpath  
  (`build-logic/.../KotlinAndroid.kt:72-76` 设了它, `android.toml` 的 androidTest 只有 `core` 与 `ext:junit`) (qwen TST-03).  
- 缺 `kotlinx-coroutines-test` 与 Turbine, 刷新保留旧列表与 `loadJob` 去重这两条契约今天无法断言 (qwen TST-05).  
- 模板测试与 flavor 冲突且本身无价值: `ExampleUnitTest` 断言 2+2, `ExampleInstrumentedTest` 断言无后缀包名,  
  而 debug 变体带 `applicationIdSuffix` (deepseek PA-45, glm TE3, mimo TEST-1, qwen TST-01, qwen TST-21).  
- lld.json 契约测试: 随包资产是现成 fixture, 一个 JVM 测试把资产解成 `Lld` 并断言 `version` 过 `isLldDatetime()` (qwen TST-06).  
- 纯逻辑零覆盖清单: `SettingsStore` 的解析, `LldFileStore` 的版本比较, `getBuildProp` 的多行折叠与 `toPropMyModel`,  
  `MyModel.key` 与 `AppRoot` 的下标 clamp, `isDateHigherThanConfig` 的固定偏移 (deepseek PA-41, qwen TST-07).  
- release 变体零覆盖: `testBuildType` 未设置, 而 release 打开 R8 与资源收缩, 运行时又全是 R8 敏感项 (deepseek PA-40).  
- Robolectric 与 `testOptions` 都没有, 15 处 `Log.*` 在宿主测试里会抛 "not mocked" (qwen TST-08).  
- 测试依赖无差别发给所有模块, 含没有测试源集的 `:binderDetector` (qwen TST-10).

**要等重构的**: 测试替身入口被堵死: DI 图不接受额外 binding container (`AppGraph.kt:88-90`),  
加上 115 处静态取用, 仓库与 ViewModel 无法在 JVM 上构造 (deepseek PA-42, glm TE4, mimo TEST-2, qwen TST-04, qwen TST-13).  
glm TE4 列了阻断它的既有条目: `DP2` / `SS2` / `SS3` / `SS5` / `SS1` / `AR9`. 所以这一半随第 1 到第 3 组.

**UI 与截图测试**: 基础设施为零 (`ui-test-junit4`, `ui-test-manifest`, 截图源集都没有)  
(deepseek PA-43, mimo TEST-4, qwen TST-09);  
文档已把 v1 入口标为废弃, 新套件应直接用 v2 (qwen `TST-11`);  
测试容器与 Activity (qwen `TST-12`); 三个纯数据 composable 里两个是 private (qwen `TST-14`);  
真实后台工作不在测试时钟内, `waitForIdle()` 等于没等 (qwen `TST-15`); 全仓零 `testTag`, 断言只能锚在本土化文本上 (qwen `TST-16`);  
现有 semantics 会让断言假通过或无从断言 (qwen `TST-17`); 中间帧要显式驱动帧时钟 (qwen `TST-18`);  
组合外的 Toast 断不到 (qwen `TST-19`); 失败捕获的配置项还够不到 (qwen `TST-20`).

**JUnit 5**: deepseek PA-46 说 "迁到 JUnit 5 的机制待查证"; glm 说现实形态可能是混合 (纯逻辑与 ViewModel 走 5,  
Compose 与插桩保留 4); mimo TEST-3 记仍是 JUnit 4; qwen 说本地测试确实是 Gradle `Test` 任务, 加 `useJUnitPlatform()` 即可,  
但插桩侧仍是 JUnit 4 (社区插件把 Jupiter 插桩限制在 API 26 以上, 高于本项目 minSdk).  
官方本地测试页的示例栈仍是 JUnit 4. 这是决策点.

**基础设施决策** (glm): 四个决策点: JUnit 5 混合还是全 JUnit 4; Compose 测试先锁 BOM 内 v1 写起还是等 v2 齐;  
截图方案 (AGP test-suite 还是 Roborazzi); 覆盖率工具是否引入.

## 需要负责人判定的问题

qwen 报告列了 8 条需要你判定的前置问题 (它的 `Q-01` 到 `Q-08`), 其中几条会改变上面某组的修法方向:

- `Q-01`: `QUERY_ALL_PACKAGES` 保留并申报, 还是把过时 targetSdk 那一行改成 `<queries>` 加限定查询 (第 8 组).  
- `Q-02`: `../../../keys/debug.keystore` 被提交与 "不提交签名材料" 如何调和 (第 8 组).  
- `Q-03`: `gwpAsanMode="always"` 与 `nativeSymbolUploadEnabled` 是只在 Firebase 包要, 还是两种包都要 (第 6, 第 8 组).  
- `Q-04`: `ShellDefault` 保留并记录前置修复, 还是删除 (第 8 组).  
- `Q-05`: 分区指纹清单与其余反射是既有问题, 是否回头改成公开 API (第 8 组).  
- `Q-06`: lld.json 缓存放外部存储是刻意的吗 (第 4 组).  
- `Q-07`: `MyModel` 下推是 "描述与代码不符", 改描述还是补出领域层 (第 7 组).  
- `Q-08`: qwen 报告标的是 record, 可执行的发现是否要转进 `issues-cn` 的编号条目.

## 与 issues-cn 的分歧

`issues-cn` 是更早的调查; 下面这些是它与 `2026-10-16-project-analysis-cn` 结论相反, 或者它没有覆盖的地方,  
一律以代码复核为准:

| 主题 | issues-cn | 2026-10-16-project-analysis-cn | 代码复核 |
|------|-----------|---------------------|----------|
| 加载失败边界 | #62 判 "有意接受" (UI 不 try, 契约是下层不抛) | deepseek PA-10 / glm AR9 / mimo PATTERN-1 / qwen UDF-01 记为待修 | `BaseListViewModel.kt:60-71` 仍没有失败分支; `IShell` 没有 "不抛" 声明. 没有失败分支属实; 正解是下层不抛 (与 AGENTS.md 一致), 不是让 UI 吸收异常 |
| `myAndroid` | #09 P0, #17 P2 | deepseek PA-02 / glm SS1 / mimo SSOT-1 高, qwen SSOT-01 | 一致 |
| 模块环与共用包名 | #04 P1, #07 P2 | glm AR1 / AR2 / MD1 高, mimo MOD-1/2, qwen ARC-02/ARC-05/MOD-02 | 代码一致; 定级不同 |
| 静态取全局 | #02 P1 (DI 已接入, 剩全局取用点) | glm SS2 高, mimo ARCH-4 中, qwen ARC-03 | 一致, project-analysis 更强调 "与图并行" |
| lld.json | 外部目录无条目; `scheme` 与 `toInt()` 是 #22, `LldFileStore` 的直接比较是 #19 | glm SE1 高, glm SE2 / mimo SEC-1/2 中, deepseek PA-04 中, qwen SSOT-06 / SEC-02 / SEC-03 | 代码属实 (`MyApplication.kt:60-71`, `LldFileStore.kt:45-64`) |
| 无 Baseline Profile | 无条目 | deepseek PA-27 高, qwen AP-06 | 属实 (`settings.gradle.kts` 没有 profileinstaller) |
| 首屏要等整条检测 | 串行那半是 #15 (P1); 首屏那半无独立条目 | mimo AP-1 高, deepseek PA-33 中, qwen AP-01 / CP-05 | 属实 (`BaseListViewModel.kt:26-27,65-70`) |
| 空 `onClick` 的 Card | #34 P1 (语义的代价还没还) | glm CP1 记为负责人接受的取舍, mimo CP-3 与 qwen CP-04 记为低分缺陷 | 代码属实 (`MyModelCard.kt:49`); 是否算缺陷需要你定 |
| 失败折成结论 | #63 P1 | glm SS5 / mimo MOD-4 / mimo PATTERN-6 中, qwen PAT-05 / AP-09, deepseek PA-01 / PA-06 低 | 现象属实, 但多发生在很少走到的分支 (如 SELinux 的 Permission denied), 概率低 |
| 列表 key 重复导致崩溃 | #20 已修 (Prop 改用数据键) | deepseek PA-18, glm CP6 记 key 仍取自展示数据 | 立即崩溃的那条路已经关闭, 结构上仍然容易出错 |
| `@Stable` 冗余 / 圆点颜色 / Others 红点 | #47 暂缓; #33 有意接受; #69 已修 | 未涉及或一致 | 一致, 不动 |

`issues-cn` 独有, `2026-10-16-project-analysis-cn` 没覆盖, 但代码能证实的, 仍然值得做, 只是不定高:  
**#65 外链跳转** (`SettingsScreen.kt:421` 用 `resolveActivity` 判浏览器, manifest 没有 `<queries>`,  
按平台的包可见性语义在 Android 11+ 可能判空; 动手前需要在一台真机上确认一次; qwen 把它记在 `Q-01` 里),  
**#66 标题与数据源不符** (`os_arch` / `build_cpu_abi` / `build_stock_fingerprint` 的标题与取值来源不同),  
**#40 圆点没有语义** (读屏拿不到判定色, 与 #34 一起做).

## 建议顺序

从高到低: 第 9 组的 "不等重构" 半段; 第 1 组; 第 2 组与第 3 组 (第 9 组的 "要等重构" 半段随第 1 到第 3 组);  
第 4 组; 第 5 组与第 6 组; 第 7 组; 第 8 组.

先做第 9 组的 "不等重构" 半段, 理由是先立起防回归的测试, 让后面那些重构被验证, 而不是靠人眼.

本报告只排先后. 子任务怎么切, 每个子任务怎么提交, 归开工时写的计划.
