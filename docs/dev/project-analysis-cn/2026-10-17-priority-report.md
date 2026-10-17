# 优先修复报告 (2026-10-17)

> **状态**: living (未归档, 可原地修订).  
> **首要依据**: `project-analysis-cn` 的三份报告 (2026-10-16 对当前代码的独立通读), 条目号写作  
> `PA-nn` (deepseek-v4.1-flash), `SS/AR/UD/DP/MD/SE/CP/AP-nn` (glm-5.3-flash),  
> 以及 `SSOT/UDF/ARCH/PATTERN/MOD/SEC/CP/AP-nn` (mimo-v2.6-flash); 报告之间冲突时以代码复核为准.  
> 两个来源各有一个 `CP` 系列, 引用时写明是哪一份 (例如 `glm CP1`, `mimo CP-1`).  
> **次要依据**: `issues-cn`. 它是更早的调查, 有的已过时或结论相反, 本报告只作交叉引用,  
> 与它的明确分歧单列在 [与 issues-cn 的分歧](#与-issues-cn-的分歧) 一节.  
> **范围**: 去掉 Test / CI / NDK / JNI.  
> **排序原则**: 按"会不会让 App 报出假结论 / 用户可感的错误"排, 不按修改难度, 复杂度, 工时排;  
> 一律给彻底修法, 不给只减轻症状的做法.  
> **修法一节的地位**: 每组的"彻底修法"只是修改实装时的参考, 不是子任务列表;  
> 子任务怎么切, 每个子任务怎么提交, 归开工时写的计划.  
> **条目号**: 沿用两个来源的既有编号, 本报告不自造编号.

## 结论

最该先修的是下面四组. 第一组是三份报告一致认定的"高"; 第二组与第三组只有 glm 一份认定"高",  
另两份定"中"; 第四组是 glm 一份认定"高", 但每次都会发生的一件:

1. **全局版本事实 `myAndroid`** (PA-02 · SS1 · SSOT-1). 三份一致"高", 是所有 API 判定的基础,  
   也含跨线程读写没有同步 (SS1 · SSOT-1) 与载荷写回全局 (SEC-3 · SE2).  
2. **加载的失败边界与检测执行方式** (PA-10 · AR9 · PATTERN-1, 加 PA-33 · AP-1 · AR7 · AP-2).  
   每个检测块都有守卫, 但把这些块合起来的那一层没有: 一次 shell 挂起或出错, 就是一直转圈或直接崩溃;  
   检测又全部一个接一个执行, 首屏要等整条检测跑完.  
3. **结构与分层的根本原因** (AR1 · AR2 · MD1 · SS2 · ARCH-4, 加 ARCH-1/2/3 · PATTERN-2 · DP1).  
   `ui.home` 与 `ui.settings` 互相引用成环, `ui.common` 反向依赖 home 的模型, `:app` 顶着 `:base` 的包名,  
   `MyApplication` 的伴生对象是与 DI 图并行的第二个入口, 且数据层直接产出界面模型.  
4. **`lld.json` 的校验** (SE1 · SE2 · SEC-1/2/3/4 · PA-04 · PA-24, 加 PA-25 · SEC-8).  
   文件写在应用专属的外部目录, 不校验任何完整性, 不校验 schema, 不查 HTTP 状态, 备份规则是空元素.

其后依次是: Prop 页对每个键各查一次 binder (AP1 · AP-3), 启动与首屏性能加 Baseline Profile  
(PA-27 · PA-32 · PA-34 · PA-35 · AP-4/AP-6/AP-8/AP-9), 把判断与展示分开  
(数据层直接产出展示内容, 中), 以及一批安全与卫生的低分项.

下面每组的"为什么优先"引用 `project-analysis-cn` 的结论, "彻底修法"是代码复核后给出, 供实装参考的修法.

## 第 1 组: 全局版本事实 `myAndroid` (PA-02 · SS1 · SSOT-1)

**证据**: `AndroidVersionExt.kt:53-64` 的 `MyAndroid` 四个字段全是 `var`, `:62` 是顶层 `val` 实例;  
写入方一是 `:104-109` 的 `initMyAndroid()` (由 `MyApplication.kt:92` 调用),  
写入方二是 `HomeRepository.kt:124-131` 的 `detectAndroid()`, 它把 lld 里 known 那一行赋进全局;  
读取方是 `:114-125` 的 `isAtLeast*` (经 `sdkInt get() = myAndroid.api`),  
`:132-134` 里读它的 `isLatestPreviewAndroid` (读 `myAndroid.apiFull`),  
以及 `HomeRepository.kt:1130` 里比 targetSdk 用的阈值.  
全仓没有 `volatile` / `synchronized` / `AtomicReference` / `Mutex`.

**为什么优先** (三份一致"高"): 同一个进程里, Home 加载前后对同一台设备可以给出不同的 API 判定;  
`detectAndroid()` 又是四次独立赋值 (`api` 到 `apiFull` 到 `version` 到 `dessert`),  
两步之间落进来的读会拿到"新 api 配旧 version"这种跨代的组合 (SS1 · SSOT-1);  
载荷经这里写回后, 之后所有 API 检查与 targetSdk 阈值都被改写 (SEC-3 · SE2),  
而载荷本身只有很弱的校验 (见第 4 组).  
它也是第 3 组的前置: 若先改结构再改它, 就等于把两处写入带进新结构.

**彻底修法**: `MyAndroid` 四个字段改成 `val`; 新增一个只读入口 (例如 `AndroidInfo.current`, 第一次用到时才构建);  
`isAtLeast*` / `isLatest*` 只读它;  
lld 的修正改成纯函数 (例如 `enrichWithLld(base, lld): MyAndroid`), 返回新对象, 由 `HomeViewModel`  
显式传给 `detectAndroid(lld, androidInfo)` 与过时 APK 的过滤, **不再写回任何全局**;  
`initMyAndroid()` 从 `MyApplication.onCreate` 删除.  
一处必须小心: `initMyAndroid()` 的第一句就读全局 (`isAtLeastAndroid16()` 经 `sdkInt`),  
直接把 `var` 改成 `val` 会自我引用, 推导结果要先放进局部变量, 最后一次性构造.

**关联**: 同组还要一并处理的判定写法: `Theme.kt:322` 绕过 `isAtLeastAndroid12()` 直接写  
`Build.VERSION.SDK_INT >= S` (PA-05);  
同一条 API 门槛在 ViewModel 的守卫与仓库的 `@RequiresApi` 里各写了一次 (MOD-5).

## 第 2 组: 加载的失败边界与检测执行方式 (PA-10 · AR9 · PATTERN-1)

**证据**: `BaseListViewModel.kt:60-71` 的 `startLoad()` 是一个没有 `try` / `finally` 的  
`viewModelScope.launch`, `setLoading(false)` 只经 `setModels()` 可达;  
`IShell` (`base/.../shell/IShell.kt`) 没有声明"不抛", 实现 `ShellLibSu` 直接转发 libsu;  
`HomeViewModel.kt:221-223` 的 `getMounts()` 与 `PropRepository.kt:70` 的 shell 调用  
(它属于 `:66` 开始的 `getBuildProp()`) 是检测链里没有守卫的两处;  
`PropertyDefault` 是 object, 类初始化失败会抛 `ExceptionInInitializerError` (它是 `Error`, 守卫只抓 `Exception`).  
检测侧: `HomeViewModel.kt:211-241` 里 23 个构造模型行的调用 (外加一次 `getMounts()`)  
全部挤在同一个 IO 块里依次执行, `OthersViewModel.kt:32-98` 与 `PropViewModel.kt:29-39` 同样;  
`BaseListViewModel.kt:26-27,65-70` 的 `modelsStateFlow` 初始是 null, 首屏要等 `collectModels()` 全部返回.

**为什么优先** (glm 定高, deepseek 与 mimo 定中): 这条路径通向的是一直转圈或崩溃, 不是一行错误提示;  
而且这是用户每次都付的代价: 首屏要等整条检测跑完 (mimo AP-1 高),  
慢设备上依次执行的 shell 与包枚举直接相加 (PA-33 · AP-2).

**彻底修法**: 按 `AGENTS.md` 的代码规则, UI 层不处理异常, 所以正解是让下层不抛, 不是让 UI 吸收异常:  
1. `IShell.execute` 明确"不抛"契约 (它没有 `OrThrow` 后缀), `ShellLibSu` 与 `ShellDefault` 各自  
   把 libsu 与进程异常转成 `ShellResult(isSuccess = false)`, 并给 `waitFor` 加超时.  
2. 没被守卫的调用点 (`getMounts`, `getBuildProp`) 把"失败"当成一种正常结果处理 (结果类型或 wrapper),  
   不再让异常从 `collectModels()` 里抛出.  
3. `PropertyDefault` 的类初始化加保护, 或把隐藏 API 反射失败变成可判定的结果 (见第 8 组的 PA-23).  
4. **执行方式**: 检测项改成一份清单 (每一项含身份与执行函数), 由一段统一的代码按清单执行,  
   有上限的并发与取消检查都在那段代码里, 同时保住固定的展示顺序与检测项之间的先后依赖;  
   首屏分批显示 (已经算好的行先显示). 这一步与第 3 组的检测项清单是同一件事.

**关联**: PA-10, AR9, PATTERN-1; PA-33, AP-1, AR7, AP-2, AP-5 (执行方式);  
`BaseListViewModel.kt:79-87` 的按 `targetIndex` 就地替换 (PA-13) 与列表里条目的身份 (PA-18) 随清单一起改;  
`AppRoot.kt:97-116` 的 16 份重复注册 (PATTERN-7) 与四条从不放第二个条目的返回栈 (PATTERN-11) 归第 3 组.

## 第 3 组: 结构与分层的根本原因 (AR1 · AR2 · MD1 · SS2 · ARCH-4)

**证据**:  
- **特性包互相引用成环**: `HomeRepository.kt:49` 引 `ui.settings.datasource.AppInfoDataSource`,  
  `SettingsRepository.kt:14` 反向引 `ui.home.datasource.LldFileStore` (AR1 · MOD-2);  
  `ui.common/AndroidVersionExt.kt:11` 引 `ui.home.model.Lld`, 反向 `HomeRepository` 引一排 `ui.common.*` (AR2 · MOD-1);  
  `BasicDataSource.kt:7` 引 `ui.home.datasource.AndroidDataSource` (AR2 · MOD-2 · AR5).  
- **两个模块共用一个包名**: `MyApplication.kt:1` 在 `:app` 里声明 `net.imknown.android.forefrontinfo.base`,  
  与 `:base` 模块的命名空间相同 (MD1 · MOD-6 · PA-14).  
- **静态取全局**: `MyApplication.kt:74-85` 的 `instance` / `sharedPreferences` / `getMyString`  
  是与 Metro 图并行的第二个入口, `AppGraph.kt:206-207` 的 `sharedPreferences` 提供者声明了  
  `app: MyApplication` 参数却不使用它, 直接读伴生对象 (SS2 · ARCH-4 · SS3 · DP2 · PA-15).  
- **数据层直接产出展示内容**: `HomeRepository` 的 24 个 `detect*` 直接构造 `MyModel`  
  (含 `StatusColor` 与本地化文案), `getMyString` 全仓 99 处;  
  `ui.common` 既放数据基础设施又放 Compose 绘制, 还反向依赖 feature  
  (ARCH-1 · ARCH-2 · ARCH-3 · PATTERN-2 · DP1 · MOD-1 · PA-16 · PA-20).  
- 仓库职责: `HomeRepository` 1211 行 (AR3), `OthersRepository` 基本只做转发 (AR4),  
  `AndroidDataSource` 是没有行为的常量表 (AR5 · MOD-7), ViewModel 直接用到 DataSource 的常量 (AR6),  
  `:binderDetector` 的边界已经失效 (MD3), `fileTree("libs")` 指向不存在的目录且 `:base` 侧用 `api` 暴露  
  (MD7 · MOD-9).

**为什么优先** (glm 定高, mimo 定中): 从包名判断不出依赖方向, 任何 feature 都抽不出来, 单独推理不了;  
这也是"改一处要动 4 到 6 个文件"的来源.  
它与第 2 组的检测项清单, 第 1 组的改成不可变是同一轮工作.

**彻底修法**: 采用 `module-structure-cn.md` 里的目标结构 (该文件标注未定, 动工前先定):  
`:core` 放与平台无关的模型与工具 (含 `AndroidDataSource` 的常量, `Lld`, 日期与版本比较),  
`:feature/*` 各自独立, 含 Screen + ViewModel + Repository + 专属 DataSource,  
`MyApplication` 移到 `:app` 根包, `:base` 只留 `IShell` / `IProperty` 接口.  
先把 Metro 图接完 (已基本完成), 再拆模块; 搬移只是移动文件与改引用,  
但必须与文档两种语言的路径一起改: `ShellLibSu` 的路径就写在 `docs/agents-cn/architecture.md` 里.

## 第 4 组: `lld.json` 的校验 (SE1 · SE2 · SEC-1/2/3/4 · PA-04 · PA-24)

**证据**: 文件写在应用专属的外部目录 (`MyApplication.kt:60-71` 的 `getDownloadDir`), 读回时没有来源校验,  
只查"能否解析"与"version 不旧于内置资产" (`LldFileStore.kt:45-64`, 且是直接比较字符串) (SE1 · SEC-2);  
在线拉取用固定 HTTPS 地址, 直接取 `response.body()`, 不看 HTTP 状态 (`LldDataSource.kt:41-47`) (SE2 · SEC-1);  
`SCHEME_VERSION` (`Lld.kt:8`) 全仓只有定义没有使用, `scheme` 只被反序列化;  
`Json { ignoreUnknownKeys = true }` (`JsonExt.kt:6`);  
`api` 字段直接 `toInt()` (`AndroidVersionExt.kt:132,134`, `HomeRepository.kt:126`) (SEC-4 · PA-04 · PA-24);  
模型与数据已经不一致: `Toyboxes` 只有 stable/support/master, 内置资产有第 4 个键 mainline, 被静默丢掉.  
备份规则 `backup_rules.xml` 与 `data_extraction_rules.xml` 都是空元素, 按平台语义等于默认全量备份 (PA-25 · SEC-8).

**为什么优先** (SE1 高): 这份文件决定安全相关的行 (安全补丁, targetSdk 阈值, WebView 结论),  
一旦被覆写或被上游账号失陷替换, 每一行结论立刻翻转;  
而且载荷还会写回全局 `myAndroid` (第 1 组的 SEC-3), 影响扩大到"所有 API 检查".  
与第 3 组的结构问题不同, 这是数据是否可信的问题.

**彻底修法**: 解析后校验 `scheme == SCHEME_VERSION`, 不符时给出明确提示并回退;  
`api` 改用 `toIntOrNull` 加默认值; 让模型与资产一致 (补 `mainline` 字段或删掉资产里的这个键,  
以数据实际存的为准); 在线路径校验 `response.status.isSuccess()`; 比较版本与日期统一解析后再比;  
文件改写进内部 `filesDir` (回退路径已经存在), 外部目录只留给用户导出的副本, 或对已存文件做指纹比对;  
两个备份规则显式排除 lld.json.

## 第 5 组: Prop 页对每个键各查一次 (AP1 · AP-3)

**证据**: `SettingsDataSource.kt:11-26` 先用反射取出 `Settings.System/Secure/Global` 的常量名当键列表,  
`PropRepository.kt:50-63` 再逐个键调 `getString`, 每个键一次跨进程 binder 往返; 键列表还每次重新反射一次.

**为什么优先** (glm 高): 每次打开 Prop 页都会执行, 键的数量是几百量级, 低端机上能直接感觉到;  
而且这种取键的方式会漏掉设备动态写入, 不在 SDK 常量里的设置项.

**彻底修法**: 每张表用一次 `contentResolver.query(NAME, VALUE)` 取回全部键值对,  
用显式的表枚举取代 KClass 反射; 键集合改成"设备上实际存了哪些行".  
三点注意: 该调用按 `AGENTS.md` 走 `Dispatchers.IO`;  
三个 `CONTENT_URI` 与 `query()` 的 API 等级按 `build.toml` 的 minSdk 核对;  
行集合变化会牵动 `LazyColumn` 的 key 与失败行的身份, 与第 3 组的条目身份一起定.

## 第 6 组: 启动与首屏性能加 Baseline Profile (PA-27 · PA-32)

**证据**: `settings.gradle.kts:54-56` 只有三个模块, 依赖里找不到 `androidx.profileinstaller`,  
也没有生成模块 (PA-27);  
`MyApplication.onCreate` 在主线程做了这些: 建图 (`:58`), 取 `settingsStore` 从而触发同步读  
`SharedPreferences` 并可能条件写 (`:103`, `SettingsStore.kt:116-143`),  
`initMyAndroid()` 用反射遍历 `Build.VERSION_CODES` (`:92`), 配置 libsu builder (`:105-116`) (PA-32);  
`PropertyDefault` 每次读取都调 `getDeclaredMethod` (`PropertyDefault.kt:10-18`) (AP-4 · AP-5);  
`RomDataSource.getDefaultUserAgentOrThrow` 为一行信息调 `WebSettings.getDefaultUserAgent`  
(`RomDataSource.kt:26`) (PA-34);  
过时 APK 那一行每次加载都全量枚举 `getInstalledApplications`, 同一次刷新还可能算两遍  
(`HomeRepository.kt:1117-1177`, `HomeViewModel.kt:238-240,266-275`) (PA-35).

**为什么优先** (deepseek 两处"高"): 这些都是每次启动或每次加载都会发生的成本, 与第 2 组的首屏问题相加.  
Baseline Profile 是本项目收益最大的一处改动 (启动期本来就要反射, 执行 shell, 枚举应用包).  
它的实现会牵到 Macrobenchmark 模块 (属测试工具), 要不要做进第 6 组需要你确认.

**彻底修法**: 把 SharedPreferences 读取与反射移出首帧路径 (第 1 组改成用时才构建已顺带解决反射);  
`PropertyDefault` 把 `Method` 缓存成对象成员; 过时 APK 的枚举结果每次加载只算一次,  
就地替换那一行的路径复用本次已经解析好的 `Lld`; WebView 用户代理那一行改成用时才算, 或标注它的代价;  
生成 Baseline Profile 并接进构建.  
另有 Compose 侧的低分项: `AppRoot.kt:97-100` 每次重组都重建导航参数 (PA-28),  
`ScrollBarExt.kt:41` 在组合期读滚动状态 (PA-29 · glm CP5 · mimo CP-1),  
超长内容放在一个 `Text` 里并做高度动画 (`HomeRepository.kt:1163-1167`, `MyModelCard.kt:55,88`)  
(PA-30 · mimo CP-4),  
`Theme.kt:321-329` 没有 `remember` 就重建 ColorScheme (PA-31 · glm CP3),  
`MyModelCard.kt:49` 每一行都是空 `onClick` 的 Card (glm CP1 · mimo CP-3, 见分歧一节).

## 第 7 组: 把判断与展示分开 (数据层直接产出展示内容, 中)

这一组 glm 与 mimo 定"中", deepseek 的相关条目 (PA-01 · PA-06) 定"低",  
但它在结构上是最深的一处, 也是 `project-analysis-cn` 里 `MOD-1` / `MOD-2`  
与 `AR1` / `AR2` 的共同原因: `MyModel` (界面模型, 带 `StatusColor` 与本地化详情) 被下推到数据层,  
于是仓库必须有 `Context` 才能干活 (`getMyString` 99 处, `HomeRepository` 69 处),  
`ui.common` 反向依赖 home, 循环也就跟着出现  
(ARCH-1 · ARCH-2 · ARCH-3 · PATTERN-2 · PATTERN-6 · DP1 · SS5).

**连带一起处理的**: 判断结果没有类型, 失败就地折成结论或空集合:  
`PropertyReader.getString` / `getBoolean` 在失败或条件不满足时返回本地化占位或 `false`  
(`PropertyReader.kt:24-44`), 判定层再拿文案去比 (PA-01 · SS5 · MOD-4);  
`MountDataSource.kt:27` 不看 `isSuccess` 就遍历 `.output`;  
逐行各写各的比较 (SS6 · PA-06): `detectSecurityPatch` 直接比较整串, 再用 `substringBeforeLast('-')` 取年月  
(`HomeRepository.kt:285-311`), `detectMainline` 把包的 versionName 与日期相比 (`:657-663`),  
`detectBuildId` 用了 250205-250101 这样的固定偏移 (`:249`),  
`detectVndk` 直接比 `vndkResult >= lld.android.stable.api` (`:697`);  
`HomeViewModel` 里用 try 编排 lld 的联网与离线回退 (PA-09).

**彻底修法**: 取数层返回"发生了什么" (有值 / 无值 / 失败), 判断与格式化分开,  
展示集中到 UI 层唯一的一处 (把结果翻成 `MyModel` 的那一个函数); 比较版本与日期统一解析后再比;  
编排与回退从 ViewModel 下沉到数据层.  
与第 2 组的检测项清单是同一轮: 清单里的每一项持有自己的结果类型与比较方式.

## 第 8 组: 安全与卫生的低分项

- PA-22 · SEC-12: `QUERY_ALL_PACKAGES` 加了 `tools:ignore`, 读取范围可以缩小到 flags / targetSdk / packageName.  
- PA-23: 隐藏 API 反射 (`PropertyDefault`, `HomeRepository.getBuildInWebViewProvidersAndroid7`)  
  失败时不留下任何可见痕迹.  
- SE8: 系统属性 `ro.boot.product.vendor.sku` 没清洗就拼进文件路径 (`HomeRepository.kt:523-531`).  
- SEC-9 · SE4: `keys/debug.keystore` 被 git 跟踪, 密码写在 `app/build.gradle.kts:74-77`  
  (与第 4 组同属签名材料问题, 这一条在仓库卫生一侧).  
- SEC-6 · SEC-7: debug 构建把完整的网络流量与 shell 输出写进 logcat; 有一处 release 日志可能带出文件内容.  
- SEC-11 · PA-26: Firebase flavor 带统计与崩溃上报, 值得做一次明确的隐私决定.  
- 卫生杂项: `OthersRepository.kt:85-89` 的 catch 分支返回 `Unit`, 使 `binderVersion` 退化成 `Any` (PA-36);  
  `ExceptionExt.kt:3-4` 的 `fullMessage` 丢掉整条 cause 链, 且异常类名会进用户可见的行 (PA-37);  
  `getBuildProp` 用 `+=` 累加是 O(n^2), `SettingsDataSource` 在过滤器里带副作用 (PA-38);  
  `ShellDefault` 有意保留, 但先 `waitFor()` 再读管道可能死锁 (PA-21 · PATTERN-10 · DP3).

## 与 issues-cn 的分歧

`issues-cn` 是更早的调查; 下面这些是它与 `project-analysis-cn` 结论相反, 或者它没有覆盖的地方,  
一律以代码复核为准:

| 主题 | issues-cn | project-analysis-cn | 代码复核 |
|------|-----------|---------------------|----------|
| 加载失败边界 | #62 判"有意接受" (UI 不 try, 契约是下层不抛) | PA-10 / AR9 / PATTERN-1 记为待修 | `BaseListViewModel.kt:60-71` 仍没有失败分支; `IShell` 没有"不抛"声明. 没有失败分支属实; 正解是下层不抛 (与 AGENTS.md 一致), 不是让 UI 吸收异常 |
| `myAndroid` | #09 P0, #17 P2 | PA-02 / SS1 / SSOT-1 高 | 一致 |
| 模块环与共用包名 | #04 P1, #07 P2 | AR1 / AR2 / MD1 高 | 代码一致; 定级不同 (#04 P1 / #07 P2 对应 AR1, AR2, MD1 高) |
| 静态取全局 | #02 P1 (DI 已接入, 剩全局取用点) | SS2 / ARCH-4 高 | 一致, project-analysis 更强调"与图并行" |
| lld.json | 外部目录无条目; `scheme` 与 `toInt()` 是 #22, `LldFileStore` 的直接比较是 #19 | SE1 高, SE2 / SEC-1/2 中, SEC-4 / PA-04 低到中 | 代码属实 (`MyApplication.kt:60-71`, `LldFileStore.kt:45-64`) |
| 无 Baseline Profile | 无条目 | PA-27 高 | 属实 (`settings.gradle.kts` 没有 profileinstaller) |
| 首屏要等整条检测 | 串行那半是 #15 (P1); 首屏那半无独立条目 | AP-1 高, PA-33 中 | 属实 (`BaseListViewModel.kt:26-27,65-70`) |
| 空 `onClick` 的 Card | #34 P1 (语义的代价还没还) | glm CP1 记为负责人接受的取舍, mimo CP-3 记为低分缺陷 | 代码属实 (`MyModelCard.kt:49`); 是否算缺陷需要你定 |
| 失败折成结论 | #63 P1 | SS5 / MOD-4 / PATTERN-6 中; deepseek PA-01 / PA-06 低 | 现象属实, 但多发生在很少走到的分支 (如 SELinux 的 Permission denied), 概率低 |
| 列表 key 重复导致崩溃 | #20 已修 (Prop 改用数据键) | PA-18 / glm CP6 记 key 仍取自展示数据 | 立即崩溃的那条路已经关闭, 结构上仍然容易出错 |
| `@Stable` 冗余 / 圆点颜色 / Others 红点 | #47 暂缓; #33 有意接受; #69 已修 | 未涉及或一致 | 一致, 不动 |

`issues-cn` 独有, `project-analysis-cn` 没覆盖, 但代码能证实的, 仍然值得做, 只是不定高:  
**#65 外链跳转** (`SettingsScreen.kt:421` 用 `resolveActivity` 判浏览器, manifest 没有 `<queries>`,  
按平台的包可见性语义在 Android 11+ 可能判空; 动手前需要在一台真机上确认一次),  
**#66 标题与数据源不符** (`os_arch` / `build_cpu_abi` / `build_stock_fingerprint` 的标题与取值来源不同),  
**#40 圆点没有语义** (读屏拿不到判定色, 与 #34 一起做).

## 建议顺序

从高到低: 第 1 组; 第 2 组与第 3 组; 第 4 组; 第 5 组与第 6 组; 第 7 组与第 8 组.

本报告只排先后. 子任务怎么切, 每个子任务怎么提交, 归开工时写的计划.
