# 项目问题清单 (合并版)

> **状态**: **living** (未归档, 可原地修订).  
> **来源**: 本文件合并了同一目录下四份对同一版代码的独立通读与一份优先级报告, 五份都已移入 `archived/`:  
> `2026-10-16-project-analysis-deepseek-v4.1-flash.md` (条目号 `PA-nn`),  
> `2026-10-16-project-analysis-glm-5.3-flash.md` (`SS/UD/AR/DP/MD/SE/CP/AP/TE-nn`),  
> `2026-10-16-project-analysis-mimo-v2.6-flash.md` (`SSOT/UDF/ARCH/PATTERN/MOD/SEC/CP/AP/TEST-nn`),  
> `2026-10-16-project-analysis-qwen-3.8-flash.md` (`SSOT/UDF/ARC/PAT/MOD/SEC/CP/AP/TST-nn`),  
> 以及 `2026-10-17-priority-report.md`.  
> 引用它们时一律带来源名, 例如 `deepseek PA-02`, `qwen SEC-03`.  
> **复核**: 每一条写出的事实与文件行号都在 2026-10-18 对当前代码逐条复核过;  
> 生产代码自 2026-10-15 起没有再改 (此后的提交只有文档), 复核里更正了旧报告里过期或不准的行号与少数计数,  
> 也把被旧报告说过头的几条按当前代码写窄 (条目内以"复核更正"标出).  
> **编号**: 本文件的 `PA-nn` 是一条新序列, 与归档报告里的同前缀编号无关 (deepseek 那份也用 `PA-nn`).  
> **出处**: 每条末尾的 "出处" 列出提出它的报告与它在那份报告里的编号, 括号里是那一份自己给的级别.  
> **级别**: 只用来排先后, 不决定某个问题可以修得多浅; 一条被多份来源定过级时取其中最高的一级;  
> 来源没有定级的写 `未定级`, 这类条目的级别是本文件的估计.  
> **修法**: 每条里的 "参考修法" 与 [第 9 节](#9-优先级与建议顺序) 的彻底修法都只是改造时的参考,  
> 不是子任务列表; 子任务怎么切归开工时写的计划.  
> **范围**: 不含 CI, NDK, JNI.  
> **陈述边界**: 条目里叙述平台或库行为而仓库内不可证的地方, 按对应平台或库文档判断, 条目内已标出这类边界.

## 摘要

条目共 125 条: 高 12, 中 66, 低 47. 最该先处理的几组 (彻底修法见 [第 9 节](#9-优先级与建议顺序)):

1. **全局版本事实 `myAndroid`** (PA-01): 四份来源都指它, 同一台设备有两个版本来源, 其中之一可变且写后不复位.
2. **加载的失败边界与检测执行方式** (PA-14, PA-85, PA-86): 每个检测块各有守卫, 把它们合起来的那一层没有;  
   首屏要等整条检测串行跑完才能看到第一行.
3. **结构与分层的根本原因** (PA-22, PA-24, PA-25, PA-36): `ui.home` 与 `ui.settings` 互相引用成环,  
   `ui.common` 反向依赖 home 的模型, `:app` 顶着 `:base` 的包名, 数据层直接产出界面模型.
4. **`lld.json` 的信任链** (PA-59, PA-60, PA-61, PA-09): 文件写在其他应用够得到的外部目录,  
   读回与在线拉取都不校验完整性与 `scheme`, 而它决定安全相关的每一行结论.
5. **Prop 页对每个键各查一次** (PA-87): 三张设置表共 195 个公开常量, 每键一次跨进程查询.
6. **启动与首屏性能加 Baseline Profile** (PA-88, PA-89): 启动期本来就要反射, 执行 shell, 枚举应用包.
7. **测试** (PA-98, PA-103): 全仓库 5 个测试文件里 4 个是模板, release 变体零覆盖.

## 1. SSOT

### PA-01 (高) `myAndroid` 是进程级可变全局, 两个写入者, 读写没有同步

- 事实源: `MyAndroid` 的 `api` / `apiFull` / `version` / `dessert` 四个字段都是 `var`, 顶层 `val myAndroid`  
  持有实例 (`ui/common/AndroidVersionExt.kt:53-64`).
- 写入者一: `initMyAndroid()` (`:66-110`, 赋值在 `:104-109`), 由 `MyApplication.onCreate` 调用  
  (`base/MyApplication.kt:92`).
- 写入者二: `HomeRepository.detectAndroid()` (`ui/home/repository/HomeRepository.kt:124-131`)  
  把 lld 里 known 那一行赋进全局, 这一步跑在 `Dispatchers.IO` 里 (`ui/home/HomeViewModel.kt:211`).
- 读取方: `sdkInt` 与九个 `isAtLeastAndroid*()` (`AndroidVersionExt.kt:114-125`), `isLatestPreviewAndroid`  
  (`:132-134`), 以及过时 targetSdk 的过滤阈值 (`HomeRepository.kt:1130`). 全仓库没有  
  `volatile` / `synchronized` / `AtomicReference` / `Mutex`.
- 后果: 同一进程内 Home 加载前后对同一台设备可以给出不同的 API 判定; 四次独立赋值之间落进来的读会拿到  
  "新 `api` 配旧 `version`"; 覆盖值只写不清, 没有复位路径; 或条件 `Build.VERSION.SDK_INT >= X || sdkInt >= X`  
  只能把级别抬高不能压低, 于是 `isAtLeastAndroid12()` 护着的五个调用  
  (`ui/others/repository/OthersRepository.kt:61-79`) 可能在真实级别更低的预览设备上执行.
- 参考修法: 四个字段改 `val`; 加一个只读入口 (例如 `AndroidInfo.current`, 第一次用到时才构建);  
  lld 的修正改成纯函数返回新对象, 由 ViewModel 显式交给检测项, 不再写回全局.  
  注意 `initMyAndroid()` 第一句就读全局 (`isAtLeastAndroid16()` 经 `sdkInt`), 直接改成 `val` 会自我引用,  
  推导结果要先放局部变量, 最后一次性构造.
- 出处: deepseek PA-02 (高), glm SS1 (高), mimo SSOT-1 (高), qwen SSOT-01 (未定级).

### PA-02 (高) Application 与 SharedPreferences 都有第二条静态取得路径, 与 DI 图并行

- `base/MyApplication.kt:73-85` 的 companion 暴露 `instance`, `sharedPreferences` 与两个 `getMyString`  
  重载, 实测三者在 `:app` 里共 114 处调用 (`instance` 14, `getMyString` 99, `sharedPreferences` 1;  
  另有一处 `instance` 出现在注释里, 不计);  
  `getMyString` 里 `HomeRepository.kt` 69 处, `SettingsStore.kt:33-45` 11 处.
- 同一个类里两条路径混用: `LldFileStore.kt:27` 构造注入 `application` 并用它取目录 (`:32`),  
  同一个文件 `:61,76` 却读 `MyApplication.instance.assets`.
- 组合根自己也不一致: `AppGraph.kt:206-207` 的 `fun sharedPreferences(app: MyApplication)`  
  声明了参数却不用, 返回伴生对象的值; SharedPreferences 的文件命名约定  
  (`"${instance.packageName}_preferences"`, `MyApplication.kt:76-78`) 也编码在这条静态路径里.
- 后果: 仓库层与数据源无法脱离 Android 运行时做单测; 从 `AppGraph` 读不出真实的对象图;  
  没有任何标记说明哪条路径才权威. interface-first 的可测性只覆盖了 `IProperty` / `IShell` / `SettingsStore`.
- 参考修法: 在组合根绑定 `Context` / `AssetManager` / `PackageManager`, 注入到目前读静态的调用点,  
  然后撤掉 companion 访问器; 资源读取与字符串走注入.
- 出处: deepseek PA-15 (中), glm SS2 (高), glm SS3 (中), mimo SSOT-2 (中), mimo ARCH-4 (中),  
  qwen ARC-03 (未定级), qwen PAT-01 (未定级), glm DP2 (中).

### PA-03 (中) 枚举型设置被物化成两条并行流, 靠回调手工配对

- `SettingsStore.kt:50-64` 同时暴露 `themeValue` / `scrollBarValue` (原始字符串) 与 `themeMode` /  
  `scrollBarMode` (枚举) 四个流; 配对赋值出现在构造播种 (`:119-134`) 与偏好监听回调 (`:95-114`) 两处.
- 存值无法识别时两者分叉: `themeMode` 回退 `FollowSystem` (`:79-83`), `themeValue` 保留原字符串 (`:100`),  
  于是设置对话框显示 "无选中项" 而界面按跟随系统运行. 迁移路径 (`:116-143`) 是第三个必须同时维护两处的地方.
- 消费方按表示形式分开: 设置对话框读原始串 (`SettingsScreen.kt:83-84`, `:250`, `:265`),  
  主题与列表页读枚举 (`MainActivity.kt:77`, `ui/theme/Theme.kt:306`, `ui/base/list/MyModelListScreen.kt:64`).
- 参考修法: 只持久化一种表示, 另一种由它派生 (`map { parse(it) }`), 写点收到一处.
- 出处: deepseek PA-03 (中), glm SS4 (中), mimo SSOT-4 (低).

### PA-04 (中) 本地化文案被当作数据有效性哨兵, "没有值" 有三种词

- 读取失败或条件不满足时 `PropertyReader.getString` 返回本地化占位串  
  (`ui/common/PropertyReader.kt:24-37`: `result_not_supported` 在 `:25,32`, `build_not_filled` 在 `:28`),  
  判定层再拿这些文案去比较: `HomeRepository.isPropertyValueNotEmpty()` (`HomeRepository.kt:1181-1184`),  
  分区指纹 (`ui/others/repository/OthersRepository.kt:219-221`), `detectVndk` (`:687,691`).
- 同一件事在页面上有三个词: 上面两个资源, 加上 `android.R.string.unknownName`  
  (`HomeRepository.kt:86` 等), 取哪个取决于取值路径而不是语义.
- 后果: 翻译改一个词, 判定就跟着变; 真实属性值若恰好等于那句文案, 判定也错. 两侧目前都经同一个  
  `MyApplication` 实例解析, 所以行为不随系统语言变化, 脆点是这条隐式约定没有代码保证.
- 参考修法: 读取端返回可空结果或密封类型 (失败带原因), 文案只在渲染处解析; 删掉所有与文案比较的判定.
- 出处: deepseek PA-01 (低), deepseek PA-08 (低), glm SS5 (中), mimo MOD-4 (中), mimo PATTERN-6 (中),  
  qwen PAT-05 (未定级).

### PA-05 (中) 判定失败就地折成结论或空集合

- `MountDataSource.kt:24-36` 不看 `ShellResult.isSuccess` 就返回解析结果, shell 失败时得到空列表;  
  `HomeRepository.kt:433-436` 的 `isThe2siSar = isAtLeastAndroid10() && mounts.none { ... }` 在空列表上为真,  
  于是 "读不到 mounts" 被显示成 "支持 2SI system-as-root" 并配绿色点 (`:462-466`); `:727-729` 的 APEX 判断同理.
- 同一形态还有 `PropertyReader.getString` / `getBoolean` 在失败时返回占位串或 `false` (见 PA-04).
- 参考修法: 取值层保留成功与否 (结果类型或 wrapper), 失败渲染成 unknown, 不参与为真为假的判定.
- 出处: qwen AP-09 (未定级). 本文件的级别是估计.

### PA-06 (低) 版本比较有两套机制, 一半满足于字符串字典序

- 项目已依赖 `io.github.g00fy2.versioncompare.Version` 并用于玩具箱, 内核, WebView  
  (`HomeRepository.kt:345,355,909,1016`), 但另有五处直接比较字符串: `mySecurityPatch >= lldSecurityPatch`  
  (`:292`), `versionName >= latestGooglePlaySystemUpdates` 且拼了 `"$versionName-01"` (`:658-659`),  
  `vndkVersionResult >= lld.android.stable.api` (`:697`), `savedLldVersion < assetLldVersion`  
  (`ui/home/datasource/LldFileStore.kt:62`), `myAndroid.apiFull >= lld.android.preview.apiFull` 加  
  `api.toInt()` 后比 Int (`AndroidVersionExt.kt:132-134`).
- 当前数据形状 (两位数 API, `yyyy-MM-dd`, 定宽时间戳) 下结果正确, 形状一变就错 (`36.10 >= 36.2` 在字典序下为假).
- 参考修法: 统一走一套比较 (解析成类型再比, 或一个扩展函数).
- 出处: deepseek PA-06 (低), glm SS6 (低), qwen SSOT-02 (未定级).

### PA-07 (低) 判定阈值硬编码在代码里, 而不是随数据文件走

- `HomeRepository.kt:245-252` 的 `offset` 用字面量 `250205 - 250101` 表示 "非最新稳定版时允许多旧的 build",  
  `:232` 的注释里还有一个同类字面量 `"250705"`; `:234-243` 直接把 build id 第二段当 `Int` 比.
- lld.json 已经承载 build 明细 (`:206-209`) 与 `scheme`, 这条容差应当与它同源. 偏移随时间自行失配,  
  失配后只表现为绿色点变红, 没有代码会报错.
- 出处: qwen SSOT-03 (未定级). 本文件的级别是估计.

### PA-08 (中) 同一条 API 级别要求在两处编码, 且不一致

- `OthersViewModel.kt:44-50` 用 `isAtLeastAndroid12()` 守着一组调用, 而它到达的五个方法里有四个标了  
  `@RequiresApi(Build.VERSION_CODES.S)` (`OthersRepository.kt:61,65,69,76`), `getVendorSku` (`:73-75`)  
  在守卫块里却没有注解. 两份写法可以各自被改.
- 另一处不叠加 `myAndroid` 的写法: `ui/theme/Theme.kt:322` 直接写  
  `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S`, 而动态取色在别处经 `isAtLeastAndroid12()`  
  (`AndroidVersionExt.kt:121`, 例 `HomeRepository.kt:318`). 两种写法的输入不同 (后者叠加可变全局, 见 PA-01),  
  所以动态取色开关与别的 API 12 判定可能不一致.
- 出处: deepseek PA-05 (中), mimo MOD-5 (中).

### PA-09 (中) `lld.json` 的 `scheme` 与必备字段从不校验

- `Lld.kt:8` 声明 `SCHEME_VERSION = 1`, 全仓库只有这一处出现; `:13` 反序列化 `scheme`, 没有任何比较.  
  随包资产 `app/src/main/assets/lld.json` 里写着 `"scheme": 1`, 上游若发别的 scheme, 当前代码照样当本版消费.
- 解析器配置为 `Json { ignoreUnknownKeys = true }` (`ui/common/JsonExt.kt:6`), 所以上游多一个字段被静默丢掉  
  (已发生: 内置资产有第四个 `Toyboxes` 键 `mainline`, 模型里没有), 少一个字段则整份解析失败,  
  失败后 `LldFileStore.kt:111-120` 吞掉异常返回 `null`, 用户只看得到一行 unknown.
- 模型与数据的分叉本身是事实: 数据文件比模型新一版时, 新增字段无人知晓.
- 参考修法: 解析后校验 `scheme == SCHEME_VERSION`, 不符时给出明确提示并回退;  
  把 "不认识的 `scheme`" 与 "缺字段" 区分成两种可显示的失败; `api` 改用 `toIntOrNull` 加默认值.
- 出处: deepseek PA-04 (中), mimo SEC-4 (低), qwen SSOT-06 (未定级).

### PA-10 (低) 设置项的键与存储值放在 string 资源里, 同一映射抄了三份

- `SettingsStore.kt:33-45` 用 11 个 `MyApplication.getMyString(R.string.*)` 的返回值当 SharedPreferences  
  的键与存储值; 枚举映射在 `:79-91` 的两个 `when` 里第二次书写; 标签与值的配对在  
  `ui/settings/res/values/arrays.xml:4-27` 用四个平行数组 (主题与滚动条各一对) 第三次表达,  
  消费处靠下标配对 (`SettingsScreen.kt:337` 的 `labels.zip(values)`).
- 后果: 改一个资源值或调换数组顺序, 老用户存的值就无人认识, 编译器不会提示.  
  资源名与值已经不一致一处: `function_allow_network_data_key` 的字符串值是  
  `about_allow_network_data_key` (`ui/settings/res/values/strings.xml:28`).
- 同批常量还有两条解析路径: store 直接读资源, 对话框经 `LocalResources` (`SettingsScreen.kt:315-319`).  
  另外 `interface_themes_key` / `interface_scroll_bar_key` 是真正的键, 而 `*_no_scroll_bar_key` /  
  `*_follow_system_key` 是显示标签, 同一个后缀承担两种语义 (`strings.xml:6,16` 对 `:9,11,19,22,24`).
- 出处: deepseek PA-07 (低), qwen SSOT-04 (未定级).

### PA-11 (低) 同一行的标题与明细来自两个 Context 的 resources

- 标题在组合期用 `stringResource` (`ui/base/list/MyModelCard.kt:98-101`, `ui/AppRoot.kt:155,170`),  
  取自 Activity 的 resources; 明细在数据层用 `MyApplication.getMyString`  
  (`base/MyApplication.kt:80-84`), 取自 Application 的 resources, `HomeRepository.kt` 单文件 69 处.
- `app/build.gradle.kts:46-49` 开了 `generateLocaleConfig` 并过滤三种 locale; 两个 Context 各自拿到什么 locale  
  是另一件事, 一致性没有代码保证. 会不会因此给出两种语言, 仓库内不可证, 记 unknown.
- 参考修法: 明细也走 "传资源 id, 渲染处解析" 的形态 (`ui/base/list/MyModel.kt:12-15` 已有这个形状),  
  与 PA-04, PA-24 一起做.
- 出处: qwen SSOT-05 (未定级). 本文件的级别是估计.

### PA-12 (低) 当前标签页索引有两份, 保存的那份没有校验

- `ui/AppRoot.kt:83` 的 `rememberSaveable currentTabIndex` 没有校验, `:88` 的 `selectedTabIndex = currentTabIndex.coerceIn(0, topLevelTabs.lastIndex)` 是渲染用的那份.
- 标签页在后续版本里减少时, 保存的那份会在整个进程存活期内越界, 被重新保存的也是那个过时值;  
  两份在用户点标签页之前一直不一致.
- 出处: mimo SSOT-6 (低).

### PA-13 (低) Treble 支持状态从行的颜色推导

- `HomeRepository.kt:556-568` 里 `isTrebleSupported = trebleModel.color != StatusColor.CRITICAL` (`:563`),  
  这个标志又交给 `detectGsi` (`:565`, `:570-597`). 检测失败经 Home 的守卫也会得到 CRITICAL  
  (`ui/base/list/MyModelExt.kt:46-50`), 于是 "检测失败" 与 "Treble 确实关闭" 对 GSI 是同一个输入.
- 后果: 以后改颜色映射会无声地改变 GSI 的结论; 判定不该建立在展示用的颜色上.
- 出处: mimo SSOT-5 (低).

## 2. UDF

### PA-14 (高) 加载没有失败出口, 也没有超时

- `ui/base/list/BaseListViewModel.kt:60-71` 的 `startLoad()` 是一个没有 `try` / `finally` 的  
  `viewModelScope.launch`, 顺序是 `setLoading(true)`, `collectModels()`, `setModels(...)`,  
  `onModelsLoaded()` (`:66-69`), 而 `setLoading(false)` 只经 `setModels()` 可达 (`:89-93`).
- 后果: `collectModels()` 一抛出, `isLoading` 停在 `true` (页面继续转圈), 异常冲出 `viewModelScope`  
  成为未捕获异常; 一次不返回的 shell 或文件调用没有任何超时, 而 `:61-63` 的去重判断还挡住后续刷新.
- 这份契约靠逐个调用点套守卫实现, 至少两处 shell 调用在守卫之外: `HomeRepository.getMounts()`  
  (`ui/home/repository/HomeRepository.kt:415`, 调用于 `ui/home/HomeViewModel.kt:221-223`) 与  
  `PropRepository.getBuildProp()` (`ui/prop/repository/PropRepository.kt:70`).
- `IShell` (`base/src/main/java/net/imknown/android/forefrontinfo/base/shell/IShell.kt:3-5`)  
  没有声明 "不抛", 实现 `ShellLibSu` (`ui/common/ShellLibSu.kt:15-20`) 直接转发 libsu;  
  `PropertyDefault` 是 `object`, 类初始化失败抛 `ExceptionInInitializerError`, 那是 `Error`, 守卫只抓 `Exception`.
- 参考修法: 按 [05-代码规则](../../agents-cn/05-代码规则.md), UI 层不处理异常, 正解是让下层不抛:  
  `IShell.execute` 明确不抛的契约并把异常转成 `ShellResult(isSuccess = false)`; `waitFor` 加超时;  
  两处未守卫的调用点把失败当正常结果处理; `PropertyDefault` 的类初始化加保护 (见 PA-46 与 PA-68).  
  执行方式本身见 PA-85 与 PA-86.
- 出处: deepseek PA-10 (中), glm AR9 (高), mimo PATTERN-1 (中), qwen UDF-01 (未定级).

### PA-15 (中) 排序补丁那一行由另一份 lld 重算, 两条写入方向靠手写计数器协调

- 同一行 `OutdatedTargetSdkApk` 有两条写法: 加载流水线用本次内存里的载荷  
  (`HomeViewModel.kt:147`, `:238-240`), 排序开关触发的补丁则重读离线文件 (`:268` 调 `fetchOfflineLldOrNull`,  
  `:156-188`), 而那一路的 `:159` 还会调 `copyJsonIfNeededOrThrow()`, 于是一次纯展示操作可能重写缓存文件.
- 这一行的颜色取决于收到哪个 `lld` (`HomeRepository.kt:1141-1149`), 只有 `:136-145` 的保存成功时  
  两边才一致; ViewModel 里没有任何 `lld` 状态记录这一行当前用的是哪份数据.
- 两个写入方向由一套手写世代判定协调 (`outdatedOrderChanges` 与普通 `var loadStartGeneration`,  
  `HomeViewModel.kt:78-82`, 消费点 `:54` 与 `:258-264`), 时序还要靠 `:70-77` 的注释解释.
- 参考修法: 把已解析的 `Lld` 作为一次加载的状态保留, 补丁复用它; 排序偏好作为流输入交给构造列表那一步.
- 出处: mimo SSOT-3 (中), mimo UDF-1 (中), qwen UDF-02 (未定级), qwen UDF-04 (未定级).

### PA-16 (中) `updateModelDetail(targetIndex)` 是公开的按索引写入入口

- `BaseListViewModel.kt:79-87`; 唯一调用方 `HomeViewModel.kt:277-285` 先 `indexOfFirst` 求下标  
  (`:278-280`), 再按下标替换, 两步之间一次 `refresh()` 就会把内容写到另一行上.
- 这个类型当前只有一个实例 (`HomeRepository.kt:1176` 是唯一构造点), 出现第二个时更新会落到第一行,  
  且不会报错; 类型本已带 `MyModelType` 作身份标识.
- 参考修法: 按类型或 `key` 在基类内完成查找与替换, 或把这个入口收进 `HomeViewModel`.
- 出处: deepseek PA-13 (低), glm UD1 (中), qwen UDF-03 (未定级).

### PA-17 (中) `allowNetworkData` 只在加载时读一次

- `HomeViewModel.kt:58` 每次加载只用 `first()` 读一次, 全仓库唯一的消费点就是这里;  
  用户在加载进行中拨动开关, 下一次手动刷新前看不到效果, 而它的兄弟 `outdatedOrderFirst`  
  有完整的实时更新路径 (`:89-96` 加 `:258-264`). 两个开关嵌进同一张列表, 谁有实时同步无法从数据形状推出.
- 参考修法: 两个开关同等对待, 或在 `collectModels()` 内统一读 store 的当前值.
- 出处: glm UD2 (中), mimo UDF-2 (中).

### PA-18 (中) 同一状态有两条下行通道

- `MainActivity.kt:75-79` 把设置流作为 CompositionLocal 提供; `SettingsScreen.kt:96` 直接收集  
  `LocalScrollBarMode`, `:83-86` 又从 ViewModel 读同一批值; 滚动条设置在同一帧以两种类型进入  
  `SettingsContent` (`:100` 原始串, `:101` 枚举); 列表页同理 (`MyModelListScreen.kt:39-40,64`).
- 复核更正: CompositionLocal 里装的是声明类型 `StateFlow` (`SettingsStore.kt:63-64`,  
  `MyModelListScreen.kt:39`), 没有 setter, 所以归档报告里 "任何 composable 取到都能写" 不成立;  
  成立的是同一事实有两条送达通道, 状态的送达要在两处手工保持一致.
- 参考修法: 与其余偏好一致地走 ViewModel, CompositionLocal 只留给没有 ViewModel 的场合.
- 出处: deepseek PA-11 (低), glm UD3 (中), mimo UDF-3 (低).

### PA-19 (低) 一次性事件以返回值表达, 计数是普通 `var`

- `SettingsScreen.kt:117-121` 用 `viewModel.getVersionClickedMessage()?.let { context.toast(it) }`;  
  隐状态是 `private var timesLeft = 7` (`SettingsViewModel.kt:105-120`), 名字像取值却会自减;  
  普通 `var` 不进 `SavedStateHandle`, 进程死亡后静默重置, 也无法在测试里观察 (见 PA-116).
- 参考修法: 一次性事件放进可被消费的状态或一条流, 或让点击本身只改计数状态.
- 出处: deepseek PA-12 (低), glm UD4 (低), mimo UDF-4 (低), qwen UDF-07 (未定级).

### PA-20 (中) 首屏与刷新由组合层触发, 系统服务从页面下传

- `MyModelListScreen.kt:59` 的 `LaunchedEffect(viewModel) { viewModel.init() }` 把首次加载的时机  
  放在 UI 里; `SettingsScreen.kt:90-92` 在 `LaunchedEffect` 里把 `LocalContext` 取来的 `packageManager`  
  与 `packageName` 传进 ViewModel (`SettingsViewModel.kt:89-96` 的守卫保证只做一次),  
  `PackageManager` 再经 ViewModel 传到仓库 (`SettingsRepository.kt:27-29`);  
  `OthersViewModel.kt:90` 把 `MyApplication.instance` 传进仓库.
- 三个列表页走 `BaseListViewModel.init()`, settings 页另有一套启动约定.  
  展示层决定数据层收到哪些 Android 服务, 系统服务本应由数据层自己取.
- 参考修法: 触发收进 ViewModel (`PackageManager` 由图提供), 数据源经注入的 `Application` 自取所需管理器.
- 出处: glm UD5 (低), mimo PATTERN-4 (中), qwen UDF-06 (未定级).

### PA-21 (中) 去重把用户主动发起的刷新丢掉

- `BaseListViewModel.kt:61-63` 在 `loadJob?.isActive == true` 时直接 `return`;  
  用户在加载过程中下拉一次, 这次手势不会补做, 也不给提示, 最坏情况是满屏旧数据加一个已消失的转圈.
- 参考修法: 记下 "请求过就再加载一次", 在尾部补一次.
- 出处: qwen UDF-05 (未定级). 本文件的级别是估计.

## 3. 架构与分层

### PA-22 (高) 特性包互相引用成环

- home 到 settings: `HomeRepository.kt:49` 引 `ui.settings.datasource.AppInfoDataSource`  
  (`:1204-1209` 使用); settings 到 home: `SettingsRepository.kt:14` 引 `ui.home.datasource.LldFileStore`  
  (`:32` 使用).
- common 与 home 互引: `AndroidVersionExt.kt:11` 引 `ui.home.model.Lld` (`:132-134` 使用),  
  `ui/home/model/Lld.kt:5` 引 `ui.common.CODENAME_NONE`, `HomeRepository.kt:28-43` 引一排 `ui.common.*`.
- others 到 home: `ui/others/datasource/BasicDataSource.kt:7,25` 引 `ui.home.datasource.AndroidDataSource`.
- 后果: 任何 feature 都抽不出来也单独推理不了; 被两个 feature 消费的 `LldFileStore` 放在  
  `ui.home.datasource`, `AndroidDataSource` 事实上是共享常量层. `internal` 在 `:app` 里只用于 7 个符号,  
  所以 "每个特性一个包 + 同构分层" 只是命名约定 (另见 PA-23).
- 参考修法: 把跨特性复用的原始能力 (`AppInfoDataSource`, `LldFileStore`, 常量表, `PropertyReader`)  
  移出特性包, 特性之间只允许向下依赖; 目标结构见第 9 节的第 3 组.
- 出处: glm AR1 (高), glm AR2 (高), mimo MOD-1 (中), mimo MOD-2 (中), qwen ARC-02 (未定级),  
  deepseek PA-16 (中).

### PA-23 (中) 分层只是 `:app` 内的包名约定, 没有构建边界

- 仓库与数据源全在 `net.imknown.android.forefrontinfo.ui.*` 之下 (例 `HomeRepository.kt:1`,  
  `ui/prop/datasource/SettingsDataSource.kt:1`, `ui/settings/repository/SettingsStore.kt:1`),  
  构建里没有任何东西把 Screen / ViewModel / Repository / DataSource 分开: `settings.gradle.kts:54-56`  
  只有 `:binderDetector`, `:base`, `:app` 三个模块, 四个特性全在 `:app` 内.
- 后果: 数据层文件能引 Compose 辅助函数 (PA-24), ViewModel 能直接调数据源与常量 (PA-31),  
  包环 (PA-22) 也不会让构建失败, 没人会发现.
- 参考修法: 一特性一模块加 `:core:*`, 或保留包结构但加一条检查依赖方向的规则.
- 出处: mimo ARCH-3 (中), qwen MOD-02 (未定级).

### PA-24 (中) 数据层直接产出展示模型, 中间没有领域层

- 四个仓库都引 UI 侧类型: `HomeRepository.kt:23-27,50` (`MyModel`, `MyModelTitle`, `MyModelType`,  
  `guardedMyModel`, `toColoredMyModel`, `StatusColor`), `OthersRepository.kt:14-16`,  
  `PropRepository.kt:7-13`, `SettingsRepository.kt:104-113` (返回带 `@StringRes id` 的版本模型).
- 展示类型放在 `ui/base/list/MyModel.kt:7-29` 与 `ui/theme/ExtendedColors.kt:9-47`;  
  数据层于是必须有 `Context` 才能干活 (`getMyString` 99 处), 行模型由仓库拼好  
  (`HomeRepository.kt:1172-1177`). 屏幕再用位置占位符把仓库返回的 `@StringRes` 拼一次  
  (`SettingsScreen.kt:224-233`), 格式串拆在两层; 预览代码还硬编码同一组版本常量 (`:453-456`).
- 这是 PA-22 与 PA-36 的共同原因, 也是最深的一处: [03-概览](../../agents-cn/03-概览.md) 架构一节画的四级顺序是对现有代码的  
  陈述, 与代码不符的那一边要改哪边由负责人判定 (见 [第 11 节](#11-需要负责人判定的问题) 的 Q-07).
- 参考修法: 检测返回领域结果 (值加判定), 行模型与文案在 UI 侧唯一一处映射; 仓库返回普通字段.
- 出处: glm DP1 (中), mimo ARCH-2 (中), qwen ARC-01 (未定级), qwen PAT-03 (未定级).

### PA-25 (高) `:app` 与 `:base` 共用一个包名

- `app/src/main/java/net/imknown/android/forefrontinfo/base/MyApplication.kt:1` 声明  
  `package net.imknown.android.forefrontinfo.base`, 与 `base/build.gradle.kts:6` 的 namespace 同值,  
  与 `base/.../property/IProperty.kt:1` 等同一根包.
- 后果: 读代码时无法从包名判断 `base.*` 来自哪个模块; `SettingsStore.kt:11-13` 导入的 `base.*`  
  类型视觉上像 `:base` 却不是; 将来向 `:base` 迁移任何类型都会先撞名.
- 参考修法: `MyApplication` 移到应用根包, 两个设置枚举 (PA-27) 给真实归属, 两个模块不再共用包名.
- 出处: deepseek PA-14 (中), glm MD1 (高), mimo MOD-6 (中), qwen ARC-05 (未定级).

### PA-26 (中) `:base` 里放着应用特有的规则

- `base/.../extension/DateTimeExt.kt:9-26` 写死了 lld.json 的时间格式契约, `:35-37` 的  
  `isChinaMainlandTimezone()` 是 `:app` 的分发决策 (`ui/home/datasource/LldDataSource.kt:34-39`  
  用它选 Gitee 还是 GitHub).
- 这个模块自己声明的依赖只有 `base/build.gradle.kts:14` 的一条 `api(fileTree("libs"))`  
  (libs 目录为空, 见 PA-38), 没有 project 依赖, 于是这些规则位于它们的使用者之下, 而不是被使用者拉下去.
- 参考修法: 应用域内容回 `:app`, 接口模块只留接口与不依赖 app 的扩展 (与 PA-25 同一轮).
- 出处: mimo ARCH-6 (中), qwen ARC-05 (未定级).

### PA-27 (中) `MyApplication` 一个文件身兼五职, 两个设置枚举也放在其中

- 五职: 依赖图构造 (`MyApplication.kt:58`), 目录管理 (`:60-71`), 静态 companion (`:73-85`),  
  启动编排 (`:87-106`), libsu 配置 (`:108-116`).
- `AppThemeMode` 与 `ScrollBarMode` 定义在 `MyApplication.kt:18-31,37-47`, 被 `ui.theme`  
  (`Theme.kt:293,306-309`), `ui.settings` (`SettingsStore.kt:79-91`) 与 `ui/base/list`  
  (`MyModelListScreen.kt:86`) 消费, 所以一个 Application 文件成了设置领域的定义处.
- 参考修法: 枚举给真实归属, 文件按职责拆开 (与 PA-25 同一轮).
- 出处: deepseek PA-17 (低), mimo MOD-6 (中).

### PA-28 (中) 建图与播种靠调用顺序, 一行看起来像死代码的语句是承重的

- `MyApplication.kt:58` 在属性初始化里建图, `:90` 才 `instance = this`, 而 `SettingsStore` 的字段初始化  
  (`:33-45`) 读的正是 `instance`: 提前解析 `AppGraph.settingsStore` 会抛  
  `UninitializedPropertyAccessException`.
- `:103` 的 `val _ = appGraph.settingsStore` 用一次解析触发同步播种, 它必须先于 `MainActivity`  
  组合前的读取 (`MainActivity.kt:26-28,89-92` 读 `themeMode.value`), 而读者从那行字看不出它是承重的.
- 参考修法: 先绑定 `Context` 再建图, 或让播种所需的读取走注入的 `Context`.
- 出处: mimo ARCH-5 (中), qwen ARC-04 (未定级).

### PA-29 (中) `HomeRepository` 是 1211 行的巨类

- 全文 1211 行: 25 个 `detect*()` 方法 (`:74-1117`, 其中 3 个 private), WebView 私有 API 反射  
  (`:1045-1111`), 已安装应用枚举 (`:1121-1131`), 格式化辅助 (`:1180-1210`), 文件转发 (`:67-72`),  
  以及 PA-01 里那次全局写入 (`:124-131`).
- 检测, 比较, 错误文案, 展示拼装共处一个文件, 每加一个检测项都要动同一个类,  
  读系统与渲染行之间没有边界; 条目顺序还在 ViewModel 手写一遍 (23 条 `tempModels +=`, 见 PA-86).
- 参考修法: 按主题拆检测项; 文件, `Settings.Global` 与反射探测下沉数据源;  
  返回领域值, 由映射层组装 `MyModel`.
- 出处: glm AR3 (中), mimo ARCH-1 (中), qwen PAT-04 (未定级).

### PA-30 (中) `OthersRepository` 基本只做转发

- `OthersRepository.kt:38-278` 里多数方法是一行守卫包装数据源调用 (示例 `:39-44`),  
  少数几个有真实逻辑 (`getPartitionFingerprints` `:211-239`, `getCodename` `:189-199`,  
  `getTime` `:165-169`, `getKernelVersion` `:253-267`); 方法实测 37 个.
- 参考修法: 合并仓库与一比一的数据源为 feature 内一层, 或给仓库真正的汇总职责.
- 出处: glm AR4 (中).

### PA-31 (中) ViewModel 直接触达 DataSource 的概念与常量

- `OthersViewModel.kt:16,55` 导入 `ArchitectureDataSource` 只为把 `DRIVER_BINDER` 常量传给仓库;  
  `PropViewModel.kt:3,34-36` 导入 `android.provider.Settings` 并把三个 `KClass` 传给仓库.
- 参考修法: 仓库暴露无参或自含决策的 API, 常量与类的选择收回仓库内部.
- 出处: glm AR6 (中), mimo PATTERN-4 (中).

### PA-32 (中) 各层没有一致的调度器约定, 还多包了一层

- home / others / prop 的 ViewModel 把仓库调用包进 `withContext(Dispatchers.IO)`  
  (`HomeViewModel.kt:103,114,137,158,172,193,211`, `OthersViewModel.kt:34`, `PropViewModel.kt:31`);  
  settings 的仓库自己包 (`SettingsRepository.kt:31,39,52,74`); 仓库本身是没有声明策略的阻塞函数  
  (`HomeRepository.kt:1121-1131`, `PropRepository.kt:50-63`).
- 唯一的双层嵌套: `HomeViewModel.kt:211` 已在 IO 块内, `:221-223` 又包一层.
- 参考修法: 统一由仓库或数据源自持调度器, ViewModel 不包装; 删掉嵌套.
- 出处: glm AR7 (中), mimo PATTERN-5 (中).

### PA-33 (中) 业务决策位置不对档 (主机选择在数据源, 时区工具在 `:base`)

- `LldDataSource.kt:34-39` 在数据源里按是否中国大陆时区选 Gitee 或 GitHub,  
  而判断工具 `isChinaMainlandTimezone()` 放在共享模块 (`base/.../extension/DateTimeExt.kt:35-37`).
- 参考修法: 仓库层选定主机再交给数据源; 时区工具按其使用归属移动.
- 出处: glm AR8 (中).

### PA-34 (中) 导航: 每个标签页复制一份注册, 四个返回栈从不放第二个条目, 返回只有 `finish`

- `AppRoot.kt:103-116` 为四个标签页各建一份 `rememberDecoratedNavEntries` 与 `entryProvider`,  
  每份内含全部四个 `entry<...>` 注册 (`:110-113`, 四个 ViewModel 获取还在此内联),  
  于是运行时 16 个注册里 12 个永不被解析, 加一个目的地要改四份.
- 每个标签页一个 `rememberNavBackStack(tab.key)` (`:92-100`), 每个 key 一条注册  
  (`ui/navigation/NavKeys.kt:11-20`), 没有任何东西会压入第二个条目, 所以每条栈的 saveable 与  
  ViewModelStore 机制按标签页付费, 而它的遍历行为从未用到.
- `:124` 的 `onBack = { activity?.finish() }` 不弹栈; manifest 又声明了  
  `enableOnBackInvokedCallback="true"` (`app/src/main/AndroidManifest.xml:13`),  
  而代码里没有 `OnBackInvokedCallback` 或预测式返回的实现.
- 参考修法: 注册收成一份; 只有一个条目的栈不必建; `entries.size > 1` 时弹栈否则 `finish`,  
  并把预测式返回接进 Navigation 3.
- 出处: mimo PATTERN-7 (中), mimo PATTERN-11 (低), qwen ARC-07 (未定级).

### PA-35 (中) 资源目录靠构建脚本手工登记

- `app/build.gradle.kts:31-42` 登记 10 个 res 目录 (命名推导在 `:26-29`):  
  4 个 `base*` 加 `ui`, `ui/base/list` 与四个特性包; 全仓库没有 `app/src/main/res`.  
  新增一个包漏登记时, 被代码引用的资源以编译错误暴露, 没有任何代码引用的资源则静默缺失.
- 资源目录本身放在 `java/<package>/<feature>/res` 下, 与代码同层.
- 参考修法: 抽成模块让 AGP 自己合并资源; 或把这个约定写成一处可校验的函数,  
  并在检查阶段断言每个特性包都有已登记的 res 目录.
- 出处: deepseek PA-19 (中), qwen MOD-01 (未定级).

### PA-36 (中) 共享代码的归属没有规则

- `:base` 只有 7 个文件: `DateTimeExt`, `ExceptionExt`, `PropertyDefault`, `IProperty`,  
  `ShellDefault`, `IShell`, `ShellResult`; 而跨特性共享物散在三处: `ui/base/list/*` (模型,  
  列表 ViewModel, 屏), `ui/common/*` (5 个文件), `ui/base/ext/ToastExt.kt`.
- 复核更正: `:base` 不止接口与扩展, 另含两个实现文件 (`PropertyDefault`, `ShellDefault`).  
  共享 UI 套件与 "全 app 契约" 也混在 `ui.base.list`: `LocalScrollBarMode` 定义在列表特性里  
  (`MyModelListScreen.kt:39`), 却由根节点提供 (`MainActivity.kt:78`) 并被 `ui.settings` 消费  
  (`SettingsScreen.kt:62`); `ui/common` 既放数据基础设施与 Compose 绘制, 又反向依赖 home (PA-22).
- 这种没有规则的归属直接表现为 PA-22 的包环.
- 参考修法: 明文定规则 (例如 `:base` 收平台无关物, `ui.base.list` 收共享 UI 模型,  
  `ui.common` 收共享扩展), 按规则把文件放回各自的位置.
- 出处: deepseek PA-20 (低), glm MD4 (中), mimo MOD-1 (中).

### PA-37 (中) `:binderDetector` 的边界已经失效

- 模块本体 7 行 (`BinderDetector.kt:3-6`), 只有 `:app` 的 `ArchitectureDataSource.kt:27-30` 消费,  
  且在读取方法内每次 `System.loadLibrary`; 库缺失时抛出的 `UnsatisfiedLinkError` 由  
  `OthersRepository.kt:83-110` 打印与映射, errno 语义以 `:app` 常量存在 (`ArchitectureDataSource.kt:15-18`).
- 参考修法: 模块提供懒加载一次并返回结果类型的门面, `:app` 只消费门面.
- 出处: glm MD3 (中).

### PA-38 (低) 两条 `fileTree("libs")` 指向不存在的目录, `:base` 一侧还用 `api` 暴露

- `app/build.gradle.kts:126` 与 `base/build.gradle.kts:14`, 而 `app/libs` 与 `base/libs` 都不存在;  
  后者的 `api(...)` 是全仓库唯一的 `api()`, 放进一个 jar 就会不经版本目录直达每个使用者的编译类路径.
- 参考修法: 删掉两行, 或改为显式逐文件依赖.
- 出处: glm MD7 (低), mimo MOD-9 (低), qwen MOD-03 (未定级).

### PA-39 (中) `AndroidDataSource` 是没有行为的常量表, 却被跨特性当数据源引用

- `AndroidDataSource.kt:3-145` 全文只有 companion 里的 `const val` (系统属性键, 路径, shell 命令,  
  期望值), 没有 `@Inject`, 被 `HomeRepository.kt:44` 与 `ui/others/datasource/BasicDataSource.kt:7`  
  静态引用; 其他数据源都构造注入. 其他特性的属性与命令键也存在 home 包下.
- 参考修法: 改为 `object` 常量文件放进共享层 (或注入), 消除跨特性引用.
- 出处: glm AR5 (中), mimo MOD-7 (低).

### PA-40 (低) 空壳基类与恒为空的容器

- `ui/base/BaseViewModel.kt:5` 只是 `abstract class BaseViewModel : ViewModel()`, 没有任何成员,  
  却被 `BaseListViewModel` (`:18`) 与 `SettingsViewModel` (`:36`) 继承;  
  `di/AppViewModelFactory.kt:32-34` 的三个 map 里两个今天恒为空 (仓库里没有 `@AssistedInject` 声明).
- 参考修法: 去掉这两个空层次; 保留只会让 "谁负责什么" 更难看清.
- 出处: qwen PAT-02 (未定级). 本文件的级别是估计.

### PA-41 (中) 两个不相干的类共用一个类名

- `ui/others/datasource/FingerprintDataSource.kt:13-24` (读 build 与分区指纹) 与  
  `ui/settings/datasource/FingerprintDataSource.kt:12-57` (签名证书摘要与分发者映射), 同名而义异,  
  使用方分别是 `OthersRepository.kt:34` 与 `SettingsRepository.kt:24`; 两者都是按类型注入的具体类,  
  选错包会在调用点编译报错, 不会静默产出错数据.
- 另一对名义易混: `ui/prop/datasource/SettingsDataSource.kt:10` (Android Settings 表)  
  与 settings 特性的 `SettingsStore` / `SettingsRepository` (用户偏好).
- 参考修法: 各改名其一 (例如 `SignatureDataSource`, `AndroidSettingsDataSource`).
- 出处: glm MD5 (中), mimo MOD-3 (中), qwen MOD-04 (未定级).

### PA-42 (低) 格式串与位置参数数组分写在两处

- `HomeRepository.kt:158` (六参), `:263` (六参), `:919-927` (四参), `:1163-1167`  
  (`joinToString` 里按 `orderByPackageNameFirst` 换 format 资源但传同一对参数),  
  `SettingsScreen.kt:226-232` (七参); 四种语言各一份, 少传或错序在编译期无人发现.
- 复核更正: `SettingsScreen.kt:226-232` 是七个位置参数, 不是归档报告写的八个.
- 参考修法: 每行一个数据类, 在唯一一处拼接.
- 出处: qwen PAT-06 (未定级). 本文件的级别是估计.

### PA-43 (低) 分区指纹清单硬编码, 而公开 API 可用

- `ui/others/datasource/FingerprintDataSource.kt:18-20` 硬编码 10 个分区字符串,  
  `OthersRepository.kt:211-221` 只遍历它; `:17` 的注释正指向公开 API `Build.getFingerprintedPartitions()`.  
  既有代码是否回头改用公开 API 由负责人判定 (见 [第 11 节](#11-需要负责人判定的问题) 的 Q-05).
- 出处: qwen MOD-04 (未定级). 本文件的级别是估计.

### PA-44 (低) `OrThrow` 后缀不再表示 "会抛", 各数据源的后缀约定也混用

- 契约 ([05-代码规则](../../agents-cn/05-代码规则.md)) 是 "没有 `OrThrow` 后缀的函数承诺不抛". 两处相反:  
  `LldDataSource.kt:50` 的 `fetchOfflineLldFileOrThrow()` (经 `HomeRepository.kt:67` 透出)  
  只返回一个 `File`, 真正抛异常的是调用方 `HomeViewModel.kt:173` 的 `toObjectOrThrow()`;  
  `PropertyReader.kt:29` 调 `IProperty.getStringOrThrow` 又在 `:30-33` 捕获,  
  于是 `PropertyReader.getString` 成了依赖 `OrThrow` 却从不抛的中间层.
- 后缀风格也混用: `ui/settings/datasource/FingerprintDataSource.kt:27` 是  
  `getPublicKeySha256OrNullOrThrow`, 同级 `:53` 的 `getDistributor` 与  
  `ui/others/datasource/FingerprintDataSource.kt:13-25` 都没有后缀;  
  `LldFileStore.kt:32,42,101` 的 `*OrThrow` 挨着 `:111-120` 可空的 `getAssetLld`.
- 参考修法: 重命名, 让后缀只描述本函数的行为; 后缀约定写成一条规则.
- 出处: qwen MOD-05 (未定级), mimo MOD-8 (低).

### PA-45 (中) "降级成错误行" 有四种写法

- 共享守卫在 `ui/base/list/MyModelExt.kt` 构建失败行 (Home 的红色行 `:46-50`,  
  Others/Prop 的无色行 `:95-98`, 公共骨架 `:149-161`); `PropRepository.kt:41-48,57-62`  
  自己 catch 并直接构建失败行; `OthersRepository.kt:87-88,118-119,132-133,246-247`  
  手工 catch 加 `printStackTrace`; `SettingsRepository.kt:42-43,55-56,77-78` catch 加 `Log.w`  
  并用默认值顶替失败的读取, 没有守卫. 失败行的消息文本本身构建了两遍 (`MyModelExt.kt:48` 与 `:97`).
- 参考修法: 统一成一种失败表达 (与 PA-24 的领域结果同一处改动).
- 出处: mimo PATTERN-6 (中).

### PA-46 (中) 反射被当作 API 使用, 其中一部分有公开替代物

- 使用面: `PropertyDefault.kt:8-18` (反射 `android.os.SystemProperties`),  
  `HomeRepository.kt:1045-1111` (`WebViewUpdateService` 与 `WebViewProviderInfo` 五个字段),  
  `SettingsDataSource.kt:11-26` (`Settings.System/Secure/Global` 的 `declaredFields` 与  
  `getDeclaredMethod("getString")`), `ui/prop/datasource/PropertiesDataSource.kt:17-25`  
  (`java.util.Properties` 的私有 `defaults`), `HomeRepository.kt:641-645`  
  (`Resources.getSystem().getIdentifier`).
- 有公开路径的: `Settings.Secure.getString(ContentResolver, name)`, `PackageInfo` 的公开签名接口,  
  `Build.getFingerprintedPartitions()` (PA-43), 以及 `Properties` 的公开 `getProperty()` 与  
  `stringPropertyNames()`. [05-代码规则](../../agents-cn/05-代码规则.md) 那条 "新代码用当前版本允许的最新 API" 只管此后新增的代码,  
  既有反射是否回头改由负责人判定 (Q-05); 隐藏 API 那两处 (`SystemProperties`, `WebViewUpdateService`)  
  没有公开对应物, 要修的是 "每次读都重新查找方法" 与失败时的表现 (PA-68, PA-90).
- 出处: deepseek PA-23 (中), qwen ARC-06 (未定级).

### PA-47 (低) 仓库里留着大段注释掉的代码与无人使用的分支

- `ui/theme/Theme.kt:118-282` 整段注释 (165 行), 为它服务的 `ui/theme/Color.kt` 200 余条常量  
  继续公开存在; `AndroidDataSource.kt:21,25-29,52,90-91,110,117-119,133-134` 与  
  `HomeRepository.kt:390-394,498-499,684-685,796,837-846`, `AndroidVersionExt.kt:123,146-148`,  
  `build-logic/.../ext/ProjectExtensions.kt:25` 同类; `ScrollBarMode.Draggable` 仍可被解析  
  (`SettingsStore.kt:88-89`) 却没有任何绘制路径 (`MyApplication.kt:45-46` 只认 `Normal`,  
  选项在 `arrays.xml:19,26` 被注释); `ui/others/res/values/strings.xml` 的两条 binder 状态字符串  
  无人引用 (`app/lint-baseline.xml:15-35` 记为基线问题).
- 参考修法: 删除并让 git 保存历史; `ShellDefault` 见 PA-124.
- 出处: qwen ARC-08 (未定级). 本文件的级别是估计.

### PA-48 (中) 领域语义色 `StatusColor` 放在 Compose 主题包

- `ui/theme/ExtendedColors.kt:9-14` 定义, 数据层与共享模型引用: `HomeRepository.kt:50`,  
  `MyModel.kt:5,21`, `MyModelExt.kt:9`. 每个特性的数据路径都与只为 Compose 服务的包耦合.
- 参考修法: `StatusColor` 移到共享模型的旁边, `ui.theme` 只留映射.
- 出处: glm MD2 (中).

### PA-49 (中) 共享文件携带 feature 专属逻辑

- `ui/base/list/MyModelExt.kt` 按 feature 分段承载 Home 专属 (`:35-72`), Others 专属 (`:83-110`),  
  Prop 专属 (`:126-137`), 各 feature 都得引入整文件; 共享层因此知道每个页面的差别.
- 参考修法: 各 feature 自有守卫文件, 共享层只留真正通用的构造.
- 出处: glm MD6 (中), qwen ARC-02 (未定级).

### PA-50 (低) 组合根引用 feature 的辅助函数

- `AppGraph.kt:22` 导入 `ui.common.isAtLeastAndroid16` 用于 `:125`;  
  组合根按理不应依赖任何 feature 包.
- 参考修法: 版本判断工具移进共享层 (与 PA-01 的只读入口同一处).
- 出处: glm MD9 (低).

### PA-51 (低) `settings.gradle.kts` 里的两个插件版本不在版本目录内

- `settings.gradle.kts:63` (`com.gradle.develocity` `4.6.0`) 与 `:69` (foojay resolver `1.0.0`)  
  是构建脚本里唯一直接写版本的插件; 脚本改动是唯一的更新点.
- 出处: glm MD8 (低), qwen MOD-07 (未定级).

### PA-52 (低) flavor 维度名与实际用途不符, 版本名标记只给了一侧

- `build-logic/.../android/Flavor.kt:9-11,25` 用 `IssueTracker` 作维度名划分 `Foss` 与 `Firebase`,  
  而这条维度实际划分的是 "是否含 Google 服务"; `:13-18` 与 `:32-33` 只在 flavor 带  
  `hasVersionNameSuffix` 时加版本名后缀 (只有 `Foss` 为真), 于是 Firebase 构建的版本名与  
  `gradle/toml/build.toml` 里的值相同, 出问题时无法从版本名区分两种包.
- 出处: qwen MOD-06 (未定级). 本文件的级别是估计.

### PA-53 (中) 配置期读 git 状态与时钟

- `build-logic/.../app/AndroidApplicationConventionPlugin.kt:36-44` 在配置期执行  
  `git rev-parse --abbrev-ref HEAD` (`:37-39`) 与 `getCurrentDatetime()` (`Instant.now()` 在 `:52`),  
  前者进 `BuildConfig.GIT_BRANCH` (`:44`), 后者进 `archivesName` (`:41`);  
  项目开了 configuration cache (`gradle.properties:28-30`). 这条 git 值同时是在线数据的 URL 路径段  
  (`LldDataSource.kt:41`), 它的后果见 PA-71.
- 出处: qwen MOD-07 (未定级), mimo SEC-10 (低).

### PA-54 (低) 统一行模型装不下更复杂的行, key 取自展示数据

- `MyModel(title, detail, color, type)` (`ui/base/list/MyModel.kt:18-28`) 是唯一行模型,  
  所以 "targetSdk 过低的系统应用" 整份列表被拼进一个 `detail` 字符串 (`HomeRepository.kt:1163-1167`);  
  `key` 取自展示数据 (`MyModel.kt:24-28`: 资源 id 的字符串形式或原始标题), 行又用  
  `key = { it.key }` (`MyModelListScreen.kt:130`). 可证的是 key 的来源: 重复标题会产出重复 key;  
  "重复 key 会崩溃" 是 LazyColumn 的库行为, 仓库内不可证. 当前靠 "数据恰好唯一" 维持.
- 参考修法: 行模型携带检测结果与稳定标识, 长清单拆成子列表 (与 PA-24 同一轮).
- 出处: deepseek PA-18 (低), glm CP6 (低).

### PA-55 (中) 编排与失败处理写在 ViewModel 里, 仓库在那一段只做转发

- `HomeViewModel.kt:101-204` 做完三级回退 (在线, 离线文件, 内置资产) 与三个错误文案资源  
  (`lld_json_fetch_failed`, `lld_json_parse_failed`, `lld_json_save_failed`), `:246-251` 做格式化;  
  六个 `catch (e: Exception)` 在 `:108,119,143,163,179,198`; 同一文件 `:194` 直接读  
  `MyApplication.instance.assets`. 而 `HomeRepository` 在这段只做转发 (`HomeRepository.kt:67-72`).
- 检测部分正相反: 仓库才是功能的主体 (PA-29). Screen 到 ViewModel 到 Repository 到 DataSource  
  的名义链条两端各做对方的工作.
- [05-代码规则](../../agents-cn/05-代码规则.md) 说 UI 层不接业务异常, 所以这是规则与代码的分歧, 要定性:  
  要么把回退与错误映射下沉到仓库, 要么改规则 (与 PA-14 同一轮).
- 出处: deepseek PA-09 (中), mimo PATTERN-2 (中).

### PA-56 (中) Settings 功能绕开自己的仓库写数据

- `SettingsViewModel.kt:46,58,72,76` 四个 setter 直接写 `SettingsStore`,  
  而 `SettingsRepository` 只服务一个调用 `getBuiltInDataVersion` (`:99`), 连这个也是页面触发的  
  (`SettingsScreen.kt:91`). `SettingsStore` 这个仓库层类被 ViewModel, `MainActivity.kt:26-28`  
  与依赖图 (`AppGraph.kt:71`) 直接消费.
- 参考修法: 仓库层覆盖这个功能的全部读写, 或把 `SettingsStore` 的归属定清楚 (与 PA-36 同一轮).
- 出处: mimo PATTERN-3 (中).

### PA-57 (低) 两个 CompositionLocal 用静默的错误默认值

- `MyModelListScreen.kt:39-40` 的 `LocalScrollBarMode` 默认 `MutableStateFlow(ScrollBarMode.None)`,  
  `Theme.kt:293-294` 的 `LocalThemeMode` 默认跟随系统; 在 `MainActivity.kt:75-79` 的提供者之外  
  渲染的组合函数会安静地显示默认值, 而不是在缺失 provide 时失败.
- 出处: mimo PATTERN-8 (低).

### PA-58 (低) 一个会切调度器的挂起函数标了 `@MainThread`

- `HomeViewModel.kt:266` 给 `payloadOutdatedTargetSdkVersionApk` 标了 `@MainThread`,  
  而 `:271` 进入 `withContext(Dispatchers.IO)`: 注解无法跨越挂起点成立,  
  它声明的线程正是这个函数按设计会离开的.
- 出处: mimo PATTERN-9 (低).

## 4. Security

### PA-59 (高) `lld.json` 存在其他应用够得到的外部目录, 读回时只看能否解析与版本不旧

- 路径: `MyApplication.getDownloadDir()` (`base/MyApplication.kt:60-71`) 优先 `getExternalFilesDir(...)`,  
  只有外部目录为 null 或不存在时才退到内部 `filesDir`; `LldFileStore.kt:32` 据此确定保存路径,  
  `:101-109` 用 `FileWriter` 覆盖写入, `:45-65` 再把这个文件读回来当可信输入解析.
- 信任面: 已存在文件只看它能否解析, 以及 `savedLldVersion` 不早于内置资产, 目录是否脏, 文件是否存在  
  这几条 (`:46-48`, `:62-64`, `:67-69`). 复核更正: 归档报告里 "重拷只发生在解析失败时" 说过头了,  
  准确说法是 "投毒文件的 version 不旧于内置资产时就不会被重拷".
- 写入门槛按平台语义: API 30 起其他应用无法写本应用的外部专属目录 (持 `MANAGE_EXTERNAL_STORAGE` 也不行),  
  而 minSdk 覆盖的更低版本上, 持旧存储权限的应用可以写; adb / root 与被篡改的备份恢复路径始终可达.  
  这一层按平台文档判断, 仓库内不可证.
- 后果: 这个文件是全部检测结论的事实源, 一旦被覆写, 安全补丁, targetSdk 阈值, WebView 结论这些行  
  会一起翻转, 而且没有错误行. 载荷的第二个影响面见 PA-61.
- 参考修法: 保存路径改内部 `filesDir` (回退路径已经存在), 外部目录只留给用户导出的副本;  
  或对已存文件做指纹比对, 而不是信它自报的 version.
- 出处: glm SE1 (高), mimo SEC-2 (中), qwen SEC-03 (未定级), deepseek PA-25 (低, 备份面).

### PA-60 (中) 在线 payload 不看状态码, 无完整性校验, 重定向未限制, 无长度上限

- `ui/home/datasource/LldDataSource.kt:34-48`: GET 固定 HTTPS URL, `response.body()` 直取字符串,  
  不检查 `response.status.isSuccess()`, 无哈希, 无签名, 无固定 digest; 内容落盘到外部目录 (PA-59).
- 客户端绑定只配了超时, 代理选择器与 debug 日志 (`AppGraph.kt:116-183`), 没有 `followRedirects(false)`,  
  也没有 OkHttp 磁盘缓存; `response.body()` 的解码没有长度上限, 15s 请求超时是唯一边界 (`:177-181`).
- 通过校验即落盘 (`HomeViewModel.kt:136-145`), 而唯一的校验是 `version` 的时间戳格式 (`:128-134`);  
  之后这份数据决定安全相关的行 (安全补丁 `HomeRepository.kt:273`, targetSdk 阈值 `:1130`, WebView 结论).
- 参考修法: 校验状态码; 随 version 发布摘要或签名并在落盘前验证; 关掉重定向或校验最终主机;  
  加长度上限.
- 出处: glm SE2 (中), glm SE6 (低), mimo SEC-1 (中), deepseek PA-24 (中), qwen SEC-03 (未定级).

### PA-61 (中) 载荷会改写所有 API 检查背后的版本事实, 伪造条目可以弄崩应用

- `HomeRepository.detectAndroid()` 把载荷的 `api` 字段拷进全局 `myAndroid` (`HomeRepository.kt:107-109`  
  按 `apiFull` 匹配, `:124-131` 写入), 而 `isAtLeastAndroid*()` 读这个值 (`AndroidVersionExt.kt:114-125`).
- 检测顺序把这个写入排在 `detectSdkExtension` 与 `detectPerformanceClass` 之前  
  (`HomeViewModel.kt:214-218`): 一个 `apiFull` 匹配但 `api` 被抬高的条目, 会在缺少这些 API 的硬件上  
  通过 `isAtLeastAndroid11()` (`HomeRepository.kt:178-179` 到 `AndroidVersionExt.kt:144` 的  
  `SdkExtensions.getExtensionVersion`) 或 `isAtLeastAndroid12()` (`HomeRepository.kt:318-319` 到  
  `Build.VERSION.MEDIA_PERFORMANCE_CLASS`). 随之而来的 `NoSuchMethodError` 与 `NoSuchFieldError`  
  是 `Error`, 行守卫抓不住 (它只抓 `Exception`, `MyModelExt.kt:156`), 而加载没有失败分支 (PA-14).
- 利用它需要先控制载荷 (PA-60) 或那个文件 (PA-59).
- 出处: mimo SEC-3 (中).

### PA-62 (低) 离线路径的 version 无格式校验, 直接比较字符串

- `LldFileStore.kt:50-64`: 离线路径只查解析成功与否, 再直接比较 `savedLldVersion < assetLldVersion`  
  (字典序, 无格式检查), 而在线路径过了 `isLldDatetime()` (`HomeViewModel.kt:128`).
- 后果: 备份恢复回来的旧文件可以靠字典序长期压住内置资产 (与 PA-63 叠加).
- 参考修法: 离线值也过 `isLldDatetime()`, 比较改成解析后比时间 (与 PA-06 合并).
- 出处: glm SE5 (低).

### PA-63 (中) 备份与迁移规则是空元素

- `app/src/main/java/net/imknown/android/forefrontinfo/base/resBackup/xml/backup_rules.xml:5`  
  是空 `<full-backup-content />`, `data_extraction_rules.xml:4` 是空 `<data-extraction-rules />`  
  (同一目录 `resBackup/xml/` 在 `:app` 内, 经 `app/build.gradle.kts` 的 sourceSets 登记);  
  manifest 引用二者 (`AndroidManifest.xml:12,14`), 而 `android:allowBackup` 从未写 (全仓库没有这个属性).
- 按平台备份语义, 空规则等于默认全量备份: SharedPreferences 里的设置与 `files/` 下内容都进云端备份  
  与设备迁移; `MyApplication.getFileDir` 在外部目录不可用时回退 `filesDir` (`MyApplication.kt:62-71`),  
  所以拉取来的 lld.json 也可能落在备份范围内. 恢复出来的旧文件会靠字典序压住内置资产 (PA-62).
- 今天存的只是界面偏好, 数据敏感度不高; 问题是以后加入的任何类凭据键会被自动包含, 没有规则可审.
- 参考修法: 显式写 `android:allowBackup="false"`, 或按目录写排除规则, 把缓存数据排除.
- 出处: deepseek PA-25 (低), glm SE3 (中), mimo SEC-8 (低), qwen SEC-08 (未定级).

### PA-64 (中) debug 签名材料进了版本库, 缺失时静默产出未签名包

- `git ls-files keys/` 只有 `keys/debug.keystore` (被跟踪, `keys/release.jks` 存在但未跟踪);  
  debug 的签名库, 别名与口令硬编码在 `app/build.gradle.kts:74-77` (口令在 `:75,77`), 取值是公开的模板默认值.
- app 自己公开列出 debug 签名的分发者 (`ui/settings/datasource/FingerprintDataSource.kt:23-24,53-57`),  
  即公开渠道确有 debug 构建; debug 构建类型带 `applicationIdSuffix` (`:99,105`),  
  所以这类 APK 不会盖装到 release 安装上.
- release 侧从 `local.properties` 读口令 (`:53-56`), 文件不存在时静默 `return@register`,  
  于是 `signingConfig = signingConfigs.named("release")` (`:91`) 拿到未配置的签名,  
  `assembleFossRelease` 会产出未签名包而不报错; `v1..v4` 手工全开 (`:67-70,80-83`)  
  在 AGP 上是多余的.
- [12-git](../../agents-cn/12-git.md) 的 "绝不提交" 一节有张力, 由负责人判定 (见  
  [第 11 节](#11-需要负责人判定的问题) 的 Q-02).
- 参考修法: 签名材料移出仓库, 公开渠道只走 release 签名; release 签名缺失时让构建报错.
- 出处: glm SE4 (中), mimo SEC-9 (低), qwen SEC-09 (未定级).

### PA-65 (中) `QUERY_ALL_PACKAGES` 加 `tools:ignore`

- `AndroidManifest.xml:6-8` 同时声明权限与 `tools:ignore="PackageVisibilityPolicy, QueryAllPackagesPermission"`,  
  消费方是过时 targetSdk 那一行的 `getInstalledApplications` (`HomeRepository.kt:1121-1131`).
- 代码只能证实权限与 `tools:ignore` 的存在; Play 侧是否需要并维持豁免说明属于仓库外事实,  
  本文件不判定. 这条权限同时掩盖了 `SettingsScreen.kt:421` 已废弃的 `resolveActivity`  
  在 Android 11+ 上的可见性问题 (见第 10 节的分歧表).
- 参考修法: 读取范围可收窄 (只读 flags / `targetSdkVersion` / `packageName`, 或改用 `<queries>` 限定查询);  
  申报状态按 Play 后台的实际记录确认.
- 出处: deepseek PA-22 (中), glm SE7 (低), mimo SEC-12 (低), qwen SEC-01 (未定级).

### PA-66 (低) 系统属性未清洗就拼进文件路径

- `HomeRepository.kt:523-525`: `PATH_VENDOR_VINTF_SKU.format(<prop 值>)` 的取值来自  
  `ro.boot.product.vendor.sku` (`AndroidDataSource.kt:63-64`), 未过滤; `:529` 只用于 `File.exists()`.
- 影响限于检测准确性 (含 `/` 或 `..` 的值会指错文件), 无提权.
- 参考修法: 拒绝含 `/` 或 `..` 的值再拼接.
- 出处: glm SE8 (低).

### PA-67 (低) 对外部存储路径做递归删除

- `LldFileStore.kt:34-40` 的 `deleteDirtyDirectoryOrThrow()` 在目标路径是目录时调 `deleteRecursively()`,  
  调用点包括 `:47` (拷贝前) 与 `:102-104` (保存前); 路径来自 `getDownloadDir()` (`:32`), 位于外部存储.
- 正常情况下那只是一个文件, 但这个名字一旦被占成目录 (用户或别的持存储权限的应用造成),  
  应用会递归删掉那个目录的内容.
- 参考修法: 只删自己创建的文件, 目录形态改为报错.
- 出处: qwen SEC-04 (未定级). 本文件的级别是估计.

### PA-68 (中) 隐藏 API 反射失败即静默

- `PropertyDefault` 反射 `android.os.SystemProperties` (`base/.../property/impl/PropertyDefault.kt:6`  
  的 `@SuppressLint("PrivateApi")`, `:8-18`); `HomeRepository.getBuildInWebViewProvidersAndroid7()`  
  反射 `WebViewUpdateService` / `WebViewProviderInfo` (`HomeRepository.kt:1045-1111`),  
  失败分支是 `:1052-1055` 的 `Log.w` 加空列表, 界面显示 `build_not_filled` (`:953`).
- 非 SDK 接口限制下两处都可能失效, 失效时只留下一条日志或一个空结果, 没有可见的退化标记;  
  `@SuppressLint` 同时去掉了编译期提示. "限制会不会真的拦住它们" 属平台策略, 仓库内不可证.
- 参考修法: 为这两处各留一条可见的退化标记, 或在可行时改用 `Build` / `getprop` 取值.
- 出处: deepseek PA-23 (中), qwen ARC-06 (未定级).

### PA-69 (低) 日志: debug 构建写完整流量与 shell 输出, 8 处 `printStackTrace` 没有门控

- debug 构建里 Ktor 的 `Logging` 插件装上 `LogLevel.ALL` (`AppGraph.kt:167-172`),  
  请求头与整份 lld.json 正文进 logcat; libsu 的详细日志在同一条件下开启  
  (`base/MyApplication.kt:109`); 两者只限可调试构建.
- 全仓库 14 处 `printStackTrace()`, 其中 8 处没有 `BuildConfig.DEBUG` 门控:  
  `OthersRepository.kt:88,119,133,247`, `AppGraph.kt:153,158`, `LldFileStore.kt:118`,  
  `PropertiesDataSource.kt:23`; 另有 15 处 `Log.*` 无条件输出属性键, 包名, 绝对路径与异常全文;  
  `app/proguard-rules.pro` 是默认模板, 没有任何日志剥离规则.
- 一处 release 路径上的日志可能带出文件内容: `LldFileStore.kt:50-55` 无条件  
  `Log.e(..., e.fullMessage)`, 而 kotlinx.serialization 的报错带输入偏移与摘录;  
  邻近路径都有 DEBUG 守卫 (`HomeViewModel.kt:199-201,247-249`).
- 参考修法: 统一走一个带 debug 门控的日志入口, release 里剥掉.
- 出处: deepseek PA-37 (低), mimo SEC-6 (低), mimo SEC-7 (低), qwen SEC-06 (未定级).

### PA-70 (低) Firebase flavor 带统计与崩溃上报; `gwpAsanMode="always"` 对所有构建生效

- Firebase 约定插件应用 google-services 与 crashlytics, 加入统计与带原生符号上传的崩溃上报  
  (`build-logic/.../AndroidApplicationFirebaseConventionPlugin.kt:20-32,34-44`),  
  并为 Foss flavor 关掉两个任务 (`:52-70`), 所以默认构建不受影响;  
  代码里唯一的网络开关 `allowNetworkData` (`SettingsStore.kt:68,110,157-159`)  
  只被 `HomeViewModel.kt:58` 用来决定是否取 lld.json, 不影响这两个库  
  (全仓库没有 `setAnalyticsCollectionEnabled`).
- 在一个以暴露设备信息为主题的应用里, 这个变体值得一次明确的隐私决定 (见  
  [第 11 节](#11-需要负责人判定的问题) 的 Q-03).
- `AndroidManifest.xml:15` 的 `android:gwpAsanMode="always"` 对所有构建类型和两种 flavor 生效:  
  内存安全收益的代价是常驻 native 分配开销与更少的可复用槽位, 确认这是有意为之 (Q-03).
- 出处: deepseek PA-26 (低), mimo SEC-11 (低), qwen SEC-07 (未定级), qwen AP-07 (未定级).

### PA-71 (低) 在线数据的 URL 路径段来自构建机的 git 状态, 失败静默降级成离线

- `AndroidApplicationConventionPlugin.kt:37-39` 在配置期把 `git rev-parse --abbrev-ref HEAD`  
  的原始输出带进 `BuildConfig.GIT_BRANCH` (`:44`), `LldDataSource.kt:41` 把它拼进 URL 路径;  
  从 tag 或 detached HEAD 构建时得到 `HEAD`, 非 git 目录得到 `unknown` (`:37-39` 有 `ifEmpty` 处理),  
  于是 URL 指向不存在的路径.
- `HomeViewModel.kt:108-111` 把任何异常都转成离线分支, 界面只显示 "离线" 加一句错误文本:  
  发布包里的 "是否最新" 结论可能永远来自包内旧数据, 构建与运行两端都没有信号.
- 参考修法: 数据分支或通道作为显式构建输入, 并在界面区分 "网络失败" 与 "数据通道不可用".
- 出处: mimo SEC-10 (低), qwen SEC-02 (未定级).

### PA-72 (低) `IShell.execute` 收一整行命令

- 今天的注入面不存在: 7 个 `shell.execute` 调用点 (`MountDataSource.kt:27`,  
  `HomeRepository.kt:579,850,892`, `KernelDataSource.kt:13,15`, `PropertiesDataSource.kt:38`)  
  传的都是常量, `HomeRepository.kt:573-578` 那条模板也是常量填常量;  
  `ro.boot.product.vendor.sku` 的值只进 `File.exists()` (PA-66), 没有进命令行.
- 这条记的是形状: `IShell.execute(cmd: String)` (`IShell.kt:4`) 收一整行命令,  
  备用的 `ShellDefault.kt:14` 走 `Runtime.exec(arrayOf("sh", "-c", ...))`;  
  以后有人把外部值拼进命令字符串, 这两个接口都不阻止.
- 参考修法: 接口签名改成接收参数列表, 或在接口层约定 "命令只允许常量模板",  
  并在检查清单里落一条.
- 出处: qwen SEC-05 (未定级), mimo SEC-5 (note). 本文件的级别是估计.

### 核过没有问题的面 (Security)

- 无明文流量: 没有 `usesCleartextTraffic`, targetSdk 锁值导致平台默认禁止明文流量;  
  两个 URL 硬编码 `https://` (`LldDataSource.kt:41`); `HttpClient` 不持有 `Context`  
  (`AppGraph.kt:116-182`), 走系统信任, 没有自定义 `TrustManager`.
- 组件受攻击面最小化: 唯一导出组件是 launcher `MainActivity` (`AndroidManifest.xml:23-31`),  
  没有 receiver / provider / 服务, 没有 deep link, 应用里没有 WebView, 导出的 Activity 不读 Intent.
- shell 无注入面: 所有命令是编译期常量, libsu 以非 root 运行 (`MyApplication.kt:108-116`).
- 原生侧结果被映射成封闭枚举, 失败走 "unknown" 展示 (`OthersRepository.kt:87-107`).
- 在线获取有显式开关且默认关闭 (`HomeViewModel.kt:58`, 默认值 `SettingsStore.kt:69,136`).
- SharedPreferences 是 `MODE_PRIVATE` (`MyApplication.kt:76-78`), 只存设置, 树里没有令牌或账号数据.
- 反射是只读的 (没有对隐藏 API 的写入), `PropertyDefault` 调 `SystemProperties.get` 时没有 `setAccessible`.
- 无动态代码加载; 无硬编码 secret (PA-64 之外).

## 5. Compose 性能

### PA-73 (中) 滚动状态在组合期被读取, 且挂在页面级 LazyColumn 上

- `ScrollBarExt.nonInteractiveScrollbar` 是 `@Composable` 工厂 (`ui/common/ScrollBarExt.kt:35-40`),  
  组合期计算 `val scrolling = enabled && scrollState.isScrollInProgress` (`:41`),  
  由此驱动 `animateFloatAsState` 的目标 (`:42-51`); 调用点是页面级 LazyColumn 的 modifier 链  
  (`MyModelListScreen.kt:118`, `SettingsScreen.kt:160`).
- 于是每次滚动开始与结束都会重组这一页并重跑 `items` lambda; 默认安装上滚动条模式是 `None`,  
  `enabled &&` 在读取之前短路, 所以代价只在滚动条模式为 Normal 时出现.  
  alpha 已经正确下沉到 `drawWithContent` (`:56-88`).
- 参考修法: 把 "是否在滚动" 的读取下推到 draw / graphicsLayer 阶段.
- 出处: deepseek PA-29 (中), glm CP5 (低), mimo CP-1 (低), qwen CP-02 (未定级).

### PA-74 (低) 每一行都是空 `onClick` 的可点击 Card

- `MyModelCard.kt:42-49` 的 `Card(onClick = {})` 让每个已组合的行带上交互源, 指示, 输入与语义节点,  
  按下时什么也不做, 行还会宣告自己可被激活; 只有可见行会被组合, 代价在长列表里按行重复.
- 这里有意的取舍记录在 `:43-48` 的注释里 (涟漪反馈与 TalkBack 双击播报);  
  是否算缺陷由负责人定 (见第 10 节的分歧表), 测试侧的连带影响见 PA-114.
- 参考修法: 若重评取舍, 用 `clickable(enabled = false)` 或 `clearAndSetSemantics { }`.
- 出处: glm CP1 (低), mimo CP-3 (低), qwen CP-04 (未定级).

### PA-75 (中) 超长 detail 放在一个 Text 里, 还叠了两种动画

- "targetSdk 过低的应用" 整份列表是一个 `detail` 字符串 (`HomeRepository.kt:1163-1167`),  
  渲染为单个 `Text(model.detail)` (`MyModelCard.kt:87-91`, 无 `maxLines`, 无折叠),  
  外层 Card 带 `animateContentSize()` (`:55`), 列表项还带 `animateItem()`  
  (`MyModelListScreen.kt:141`).
- 刷新或切换排序时, 这个极高的行会被整体重排, 同时做位置动画与高度动画, 每帧重新测量这块 Text;  
  WebView 行 (`HomeRepository.kt:998-1032`) 同样可以很长.
- 参考修法: 两种动画二选一; 长文本默认截断加 "展开全部", 或把这一行改成可折叠的子列表,  
  让 LazyColumn 真正虚拟化.
- 出处: deepseek PA-30 (中), mimo CP-4 (低), qwen CP-03 (未定级), qwen AP-08 (未定级).

### PA-76 (低) 指示器淡出期间每帧重绘整个内容

- `ScrollBarExt.kt:57` 无条件 `drawContent()`, `:58` 起读 `alpha`, `:80-85` 用它画滑块;  
  滑块淡出期间 alpha 每帧都在变, 整个 lambda 每帧重跑, 没有图层缓存绘制命令.
- 只有滚动条模式为 Normal 时可达 (默认 `None`, `SettingsStore.kt:60-64,132-134`).
- 参考修法: 把内容绘制的命令缓存进一个图层, 让每帧变化的只剩滑块那一层.
- 出处: mimo CP-5 (低).

### PA-77 (低) 动态配色方案每次重组重建, 没有 `remember`

- `Theme.kt:321-329` 在 `AppTheme` 的函数体内直接调 `dynamicDarkColorScheme(context)` /  
  `dynamicLightColorScheme(context)` (`:324`), 没有 `remember`; `AppTheme` 订阅 `LocalThemeMode`  
  (`:306`) 因而会重组, 每次重组都重新遍历约 30 个槽位的颜色; 子节点按值相等跳过, 影响有界.
- 参考修法: 分支内 `remember(darkTheme, context) { ... }`.
- 出处: deepseek PA-31 (低), glm CP3 (低), qwen CP-01 (未定级).

### PA-78 (低) 空列表 `persistentListOf()` 每次重组新建

- `MyModelListScreen.kt:71` 的 `models ?: persistentListOf()`, 提为顶层常量即可.
- 出处: glm CP4 (低).

### PA-79 (低) 设置页六个流在同一作用域收集

- `SettingsScreen.kt:83-86,89,96` 共六个 `collectAsStateWithLifecycle` 全部喂给 `:98` 的  
  `SettingsContent`, 任一设置改动就整页重组; 子行多数能跳过 (strong skipping 默认开启),  
  实际代价是一次主体执行.
- 参考修法: 仅当性能分析显示问题时才拆行收集.
- 出处: glm CP2 (低).

### PA-80 (低) `contentType` 只有两类, 而 Prop 页行长度差别很大

- `MyModelListScreen.kt:132` 的 `contentType = { it.type }`, 而 `MyModel.kt:7-10`  
  只有 `General` 与 `OutdatedTargetSdkApk` 两类; Prop 页的行从一行到几十行都有  
  (`PropRepository.kt:25-98`), 每行仍是一个 Card (PA-74) 加高度动画 (PA-75).
- 参考修法: 按形态给出可区分的 `contentType`, 让短行用更便宜的表达.
- 出处: qwen CP-06 (未定级). 本文件的级别是估计.

### PA-81 (中) 列表首帧没有占位

- `MyModelListScreen.kt:71` 把 `null` 显示成空列表, 于是 Home 的 25 行, Others 的 37 行  
  与 Prop 的更多行在整条流水线跑完之前 (PA-85), 屏幕上只有转圈和一个空背景.
- 参考修法: 先给出条目骨架, 逐条填充 (与 PA-14 的失败分支和 PA-85 的分批显示一起做).
- 出处: qwen CP-05 (未定级). 本文件的级别是估计.

### PA-82 (中) `AppRoot` 每次重组重建导航参数

- `AppRoot.kt:97-100` 每次重组重建 `entryDecorators` 列表 (`listOf(...)`, 在 map 之外),  
  `:103-116` 的 `topLevelTabs.map` 为每个标签页重建一次 `decoratedEntries`, 其中的  
  `entryProvider` 在 `:109-114`; 切标签页会触发 `AppRoot` 自身重组 (读点 `:88,119,123`).  
  可证的到此为止: 这些参数每次重组都是新对象, 相应产生分配.  
  复核更正: 归档报告把 `entryDecorators` 也写成在 map 里构造, 实际它在 map 之外;  
  每次重组重建这一点不变.
- 它们是否因此让 `rememberDecoratedNavEntries` 的缓存失效, 取决于 Navigation 3 库内部的 remember  
  键选取, 仓库内不可证, 所以 "四个栈被重新装饰" 不作为结论.
- 参考修法: 把这两个参数提进 `remember` (它们只依赖常量与 ViewModel 工厂),  
  这样无论库内部怎么取键都不会被换掉.
- 出处: deepseek PA-28 (中).

### PA-83 (低) Compose 编译器报告与稳定性配置没有打开

- `build-logic/.../android/Compose.kt:12-28` 只设 `buildFeatures.compose` 与依赖,  
  `KotlinAndroid.kt:97-134` 只加 Kotlin 实验开关; 全仓库没有  
  `composeCompiler { enableReports = true }`, 也没有 `stabilityConfigurationFiles`,  
  所以 `Lld`, `ShellResult`, `SettingsRepository.Version` 与四个 ViewModel 是否真的可跳过只能靠猜.
- 参考修法: 打开 release 的编译器报告并把类名清单入档, 需要时加稳定性配置.
- 出处: qwen CP-07 (未定级). 本文件的级别是估计.

### PA-84 (低) 已知的 Compose lint 条目写进基线而不是处理

- `app/lint-baseline.xml:37-90` 记录 `ComposeCompositionLocalUsage` 三条  
  (`ExtendedColors.kt:40`, `MyModelListScreen.kt:36`, `Theme.kt:284`), `ComposeModifierMissing`  
  一条 (`AppRoot.kt:76`), `ComposeReadOnlyComposable` 一条 (`MyModelCard.kt:97`);  
  另有 `UnusedResources` 三条 (`:4-35`).
- 复核更正: 基线实测 8 条, 归档报告里有只举了 5 条的. `app/build.gradle.kts:113-118`  
  的意图是 "让检查管住新代码, 基线随修复缩小".
- 参考修法: 逐条清掉, 让基线变短; `AppRoot` 缺 `modifier` 参数与 `asText()` 缺  
  `@ReadOnlyComposable` 都是一行改动.
- 出处: deepseek PA-31 (低), qwen CP-08 (未定级).

### 核过没有问题的面 (Compose)

- 列表管线健全: `key` / `contentType` / `animateItem` 齐备 (`MyModelListScreen.kt:130-132,141`);  
  `MyModel` 是 `@Immutable` 纯稳定字段; `PersistentList` 只在 ViewModel 边界转换一次;  
  ViewModel 标注 `@Stable`; 列表刷新靠数据类相等跳过未变行.
- 重活都在 ViewModel 的 IO 块内, 组合体内没有 JSON / 排序 / 大字符串运算.
- 流一律走 `collectAsStateWithLifecycle` (`MyModelListScreen.kt:53-54`, `SettingsScreen.kt:83-86`),  
  没有 `collectAsState()`; 没有 `derivedStateOf` 与 `LaunchedEffect(Unit)` 的误用.
- 对话框的资源数组只在打开时读取 (`SettingsScreen.kt:244-245,259-260`).
- `MainActivity.kt:75-83` 下发 `StateFlow` 对象而不是常值, 主题改动只失效 `AppTheme` 作用域.
- 切换标签页不会重跑检测: 每个标签页的 ViewModel 与已缓存的列表都存活  
  (`AppRoot.kt:92-116`, `BaseListViewModel.kt:46-54`).

## 6. App 性能

### PA-85 (高) 首屏要等完整条检测流水线

- `modelsStateFlow` 初始是 `null`, 只在 `collectModels()` 返回后由 `setModels` 设置  
  (`BaseListViewModel.kt:26-27,65-70`), 页面渲染 `models ?: persistentListOf()`  
  并在加载标志下转圈 (`MyModelListScreen.kt:71-72`).
- Home 是第一个标签页 (`AppRoot.kt:62-67`), 它的加载先做只受 15s 超时约束的网络取回  
  (`AppGraph.kt:101,177-181`, `HomeViewModel.kt:101-148`) 或离线文件链,  
  然后跑完全部检测项才出现第一行.
- 参考修法: 已经算好的行先显示 (逐条发射到状态), 与 PA-14 的失败分支和 PA-81 的占位一起做.
- 出处: mimo AP-1 (高), qwen AP-01 (未定级), qwen CP-05 (未定级), deepseek PA-33 (中).

### PA-86 (中) 检测完全串行

- Home 的 24 个调用在同一个 `withContext(Dispatchers.IO)` 里顺序执行 (`HomeViewModel.kt:206-244`,  
  其中 `:221-223` 还嵌套一次 IO), Others 的 37 个同理 (`OthersViewModel.kt:31-101`);  
  每个调用都可能做反射, shell 执行或 PackageManager 查询, 所以一次冷启动 Home 的耗时是它们之和.
- 检测项之间并非彼此独立: `detectAndroid` (`:214`) 写入的全局值被其后 `:215` 与 `:218` 的检测  
  经 `isAtLeastAndroid*()` 读到 (PA-01 与 PA-61), 所以并发化得先解开这层耦合.
- 行序还在三个 ViewModel 手写 (`HomeViewModel.kt:211-241` 的 23 条 `tempModels +=` 产出 25 行,  
  `OthersViewModel.kt:36-96` 的 37 条, `PropViewModel.kt:32-38`), 与检测函数本身构成两两必须同步的清单;  
  没有 "检测项" 这一抽象类型, 所以无法注册, 无法只重跑其中一条 (PA-15 的补丁路径因此把整份数据重读一遍).
- 参考修法: 检测项改成一份清单 (每一项含身份与执行函数), 由统一的一段代码按清单执行,  
  有上限的并发与取消检查都在那段代码里, 同时保住固定的展示顺序与检测项之间的先后依赖.
- 出处: deepseek PA-33 (中), glm AP5 (中), mimo AP-2 (中), qwen AP-01 (未定级), qwen UDF-06 (未定级),  
  qwen PAT-04 (未定级).

### PA-87 (高) Prop 页对每个键各查一次 binder

- `SettingsDataSource.kt:11-26` 先用反射取出 `Settings.System/Secure/Global` 的常量名当键列表  
  (每次调用重新 `declaredFields` 并排序), `PropRepository.kt:50-63` 再逐个键调 `getStringOrNullOrThrow`,  
  每个键一次跨进程 binder 往返.
- 三张表的公开 String 常量实测 195 个 (System 92, Secure 62, Global 41),  
  也就是一次进入 Prop 页至少 195 次查询加 195 次方法查找;  
  这种取键方式还会漏掉设备动态写入, 不在 SDK 常量里的设置项.
- 参考修法: 每张表用一次 `contentResolver.query(NAME, VALUE)` 取回全部键值对,  
  用显式的表枚举取代 `KClass` 反射; 键集合改成 "设备上实际存了哪些行".  
  该调用走 `Dispatchers.IO`; 三个 `CONTENT_URI` 与 `query()` 的 API 等级按 `gradle/toml/build.toml`  
  的 minSdk 核对; 行集合变化会牵动 `LazyColumn` 的 key 与失败行的身份, 与 PA-16, PA-54 一起定.
- 出处: glm AP1 (高), mimo AP-3 (中), qwen AP-04 (未定级).

### PA-88 (高) 启动关键路径都在主线程

- `MyApplication.onCreate` 依次做: 建图 (`MyApplication.kt:58`), 取 `appGraph.settingsStore` (`:103`)  
  从而在构造里做 4 次 `prefs` 读取加 1 次条件写 (`SettingsStore.kt:119,126,132,136,137`;  
  首次读取会同步解析 XML 文件, `:116-143` 还含一次可能的迁移写盘),  
  `initMyAndroid()` 反射遍历并排序 `Build.VERSION_CODES` 字段 (`AndroidVersionExt.kt:22-44,66-110`),  
  以及 `initShell()` 配置 libsu 的默认 builder (`MyApplication.kt:105-116`, 只设置 builder, 未起 shell 进程).
- debug 包开了 `StrictMode.enableDefaults()` (`app/src/debug/java/.../MyDebugApplication.kt:13`),  
  这些磁盘读会直接报违规.
- 参考修法: 把 SP 读取与反射移出首帧路径 (延后到第一次真正需要时), 或让 theme / scrollbar  
  的首帧值来自更轻的来源; 反射表只在用到时算 (与 PA-01 的只读入口同一处).
- 出处: deepseek PA-32 (高), glm AP7 (低), mimo AP-4 (低), qwen AP-05 (未定级).

### PA-89 (高) 没有 Baseline Profile, 也没有 profileinstaller

- `settings.gradle.kts:54-56` 只有三个模块, 依赖里找不到 `androidx.profileinstaller`,  
  也没有 `BaselineProfileRule` 或生成模块; 全仓库没有 `baseline-prof.txt`.
- 对一个启动期就要做反射, 执行 shell, 枚举应用包的应用, 冷启动与首次滑动的 JIT 成本正好落在这里,  
  这是收益最大的单处改动.
- 参考修法: 加 generator 模块与 profileinstaller 依赖, 生成 Baseline Profile 并接进构建.
- 出处: deepseek PA-27 (高), qwen AP-06 (未定级), deepseek PA-47 (低, 测试侧同一处空白).

### PA-90 (中) 属性读取每次都重新查找反射方法

- `base/.../property/impl/PropertyDefault.kt:8` 缓存了类, 但 `:10-13` 与 `:15-18`  
  每次 `getString` / `getBoolean` 都调 `getDeclaredMethod`.
- 一次 Home 加载里未注释的 `propertyReader` 调用实测 20 处, Others 另有约 13 次属性读取  
  (10 个分区指纹加 3 个单值); 观察到的调用方都在主线程之外, 所以这是 CPU 浪费而不是卡顿.
- 参考修法: 两个 `Method` 提升为对象成员或 companion 常量.
- 出处: glm AP4 (中), mimo AP-5 (低), qwen AP-03 (未定级).

### PA-91 (中) 一次离线加载把 lld.json 解析三遍, 没有进程缓存

- 解析一: 已存文件全量解码只取 `version` (`LldFileStore.kt:50-51`);  
  解析二: 资产全量解码只取 `version` (`:61` 配 `:111-122`);  
  解析三: 同一已存文件再全量解码供实际使用 (`HomeViewModel.kt:171-174`);  
  每遍都是完整读取加解码 (`JsonExt.kt:8-12` 解析整段文本).
- 附加: `SettingsRepository.kt:31-35` 又全量解析一次只取 version; 每次拨动排序开关,  
  `payloadOutdatedTargetSdkVersionApk` (`HomeViewModel.kt:267-274`) 重跑整套;  
  资产 version 进程内恒定却每次离线加载都重算.
- 参考修法: 进程级缓存解析结果 (至少缓存 version, 资产 version 只需一次);  
  或改成只取顶层 version 的局部解析.
- 出处: glm AP2 (中), mimo AP-6 (低), qwen AP-02 (未定级).

### PA-92 (中) 每次刷新都全量枚举已安装应用, 切换排序再扫一遍

- `getOutdatedTargetSdkVersionApkModel` (`HomeRepository.kt:1117-1177`) 每次 Home 加载都  
  全量枚举已安装应用并排序 (`:1121-1131`, `:1151-1161`); 排序开关触发的补丁又跑一次同样的枚举  
  (`HomeViewModel.kt:93,258-274`). 数据只在安装 / 卸载时变.
- 若加载进行中开关被切, 同一次刷新里会枚举两遍 (`:238-240` 与 `:253-264`).
- 参考修法: 枚举结果按一次加载缓存 (或按包变更失效); 补丁路径只对既有行重排,  
  并复用本次已解析的 `Lld`.
- 出处: glm AP3 (中), mimo AP-7 (低), deepseek PA-35 (中), qwen AP-02 (未定级).

### PA-93 (中) 每次刷新串行执行多次 shell

- `MountDataSource.kt:27` (`cat /proc/mounts`), `HomeRepository.kt:579` (GSI linkerconfig),  
  `:850` (`getenforce`), `:892` (`toybox --version`) 全部在同一个 IO 块内依次执行, 每次独立 exec  
  (`ShellLibSu.kt:17-20` 的同步 `Shell.cmd(cmd).exec()`); Prop 另有一次 `getprop` 全量 dump  
  (`PropViewModel.kt:38` 逐层到 `PropertiesDataSource.kt:38`), Others 页另有 `cat /proc/version`  
  与 `uname -a` (`KernelDataSource.kt:13,15`).
- 参考修法: 四处并行 launch 后等待, 或合并成一次 exec (与 PA-86 的并发执行同一处).
- 出处: glm AP5 (中), mimo AP-2 (中), qwen AP-02 (未定级).

### PA-94 (低) WebView 提供商的反射链每次刷新重建

- `HomeRepository.kt:1046-1111`: `Class.forName` 再逐字段反射与 `getPackageInfoOrNull` 调用,  
  结果只依赖 WebView 包状态; `:643-645` 的资源 id 也每次重取.  
  复核更正: 循环里每提供商调的是 `getPackageInfoOrNull` (`AppInfoDataSource.kt:16,18`),  
  `loadLabel` 只在 `:996` 对当前 WebView 包调一次, 归档报告写的 "每个提供方一次 loadLabel" 说过头了.
- 参考修法: 按进程缓存, 或按 WebView 包状态失效.
- 出处: glm AP6 (低).

### PA-95 (中) 为一行信息初始化 WebView

- `OthersRepository.getDefaultUserAgent` (`OthersRepository.kt:242-251`) 调  
  `RomDataSource.getDefaultUserAgentOrThrow` (`ui/others/datasource/RomDataSource.kt:26`),  
  即 `WebSettings.getDefaultUserAgent`; 调用点在 `OthersViewModel.kt:90`.
- 调用链可证; "会加载 WebView 包并拉起其进程" 是平台行为, 仓库内不可证, 需按平台文档判断;  
  若它成立, 代价与一行文本不成比例.
- 参考修法: 该行改为按需展开, 或标为 "触发时会初始化 WebView".
- 出处: deepseek PA-34 (中).

### PA-96 (低) 没有 HTTP 缓存, 每次成功获取都整文件重写

- `AppGraph.kt:116-183` 没有 `If-None-Match` / `If-Modified-Since`, 也没有 OkHttp `Cache`;  
  每次成功在线获取都整文件重写 (`LldFileStore.kt:101-109`), 字节相同也不比对;  
  资产比对每次离线加载都跑 (解析两次, 见 PA-91), 而资产 version 进程内恒定.
- 参考修法: 内容相同则跳过写; `copyJsonIfNeededOrThrow` 的结果按进程记忆; 视需要装 HTTP 缓存.
- 出处: glm AP8 (低).

### PA-97 (低) 小规模的重复构造

- 每次调用 `Long` 版格式化都新建 `DateTimeFormatter` (`base/.../extension/DateTimeExt.kt:29`,  
  文件顶部 `:17` 已有常量形态可循); Prop 键列表的反射枚举每次加载重跑  
  (`SettingsDataSource.kt:11-17`), 而键集合编译期恒定.
- 参考修法: 提为常量或 `lazy`; 键列表记忆化 (与 PA-87 的取键方式一起改).
- 出处: glm AP9 (低), qwen AP-03 (未定级).

### 核过没有问题的面 (App 性能)

- 无 `runBlocking`, 无 `Thread.sleep`, 无 `commit()` (全仓库 grep 零命中); 解析产物不驻留进程.
- 单一 Ktor client 复用 (`AppGraph.kt:114-182`), 超时三层齐备; 列表加载有同 ViewModel 内的去重  
  (`BaseListViewModel.kt:60-63`).
- 阻塞的 shell, 属性与包工作都在 `Dispatchers.IO` 之下 (`HomeViewModel.kt:211`,  
  `OthersViewModel.kt:34`, `PropViewModel.kt:31`, `SettingsRepository.kt:31`), 协程都活在  
  `viewModelScope` 里; 没有 Activity / View 级 `Context` 泄漏进 `object` 或 companion  
  (Application 级的静态持有就是 PA-02 本身).
- `SettingsStore` 的偏好监听器是进程存活期的并被 store 强持有 (`SettingsStore.kt:95-142`),  
  不随组件重建重复注册.
- 启动期之后的检测与 shell 调用都不在首帧路径上, shell 进程延迟到首次真实命令才拉起  
  (`ShellLibSu.kt`); 启动期本身的同步开销见 PA-88, 不在本节.

## 7. 测试

本节按参考清单里的分层模型判定 (unit / component / feature / application / release candidate),  
并对照 what-to-test 列出的必测项与边界项; 基准材料是负责人指定的官方测试文档与 JUnit 5,  
清单见归档的 `2026-10-16-project-analysis-qwen-3.8-flash.md` 末尾.  
测试栈由 `build-logic/.../android/KotlinAndroid.kt:72-83` 的 `configureTest` 注入, 依赖三件  
(`gradle/toml/android.toml:47,53,54`), 测试文件共 5 个.

### PA-98 (高) 测试内容基本是模板, 业务逻辑零覆盖

- 5 个被跟踪的测试文件里 4 个是 IDE 模板: `app/src/test/.../ExampleUnitTest.kt:13-14`  
  与 `base/src/test/.../ExampleUnitTest.kt:13-14` 断言 `4 == 2 + 2`,  
  两个 `ExampleInstrumentedTest.kt:20` 断言包名; 唯一有内容的是  
  `base/src/test/.../DateTimeExtTest.kt` (4 个用例, 全部在守 lld 的 version 时间格式契约).
- 其他角度定位出的业务逻辑都没有测试: `HomeRepository` 约 1211 行 (PA-29),  
  `SettingsStore` 的双流配对 (PA-03), `BaseListViewModel` 的加载契约 (PA-14),  
  `LldFileStore` 的文件编排 (PA-59, PA-91), 全局 `myAndroid` 与版本比较 (PA-01, PA-06),  
  `PropertyReader` 与占位串 (PA-04), `MyModelExt` (PA-49).
- 按 what-to-test 的必测清单 (ViewModel, 数据层, 领域与工具类),  
  目前只有 "工具类" 这一档的 1 个类有测试.
- 参考修法: 先补 unit 层, 具体目标见 PA-104 与 PA-105.
- 出处: deepseek PA-39 (高), glm TE1 (高), mimo TEST-1 (中), qwen TST-01 (未定级).

### PA-99 (中) 文档写的测试命令跑不到唯一的真测试

- [11-构建与验证](../../agents-cn/11-构建与验证.md) 写的是 `./gradlew testFossDebugUnitTest`, 而这是 flavor 任务,  
  只有 `:app` 定义了 flavor (`app/build.gradle.kts:6`); `:base` 只有不带 flavor 的  
  `testDebugUnitTest` (`base/build.gradle.kts:1-3`), `:binderDetector` 同形且根本没有测试源目录.
- 后果: 全仓库唯一的真测试实际处于 "只有单独手动触发 `:base:testDebugUnitTest` 才会跑" 的状态.
- 参考修法: 统一测试命令 (库模块加任务别名, 或约定用全工程任务), 并把 [11-构建与验证](../../agents-cn/11-构建与验证.md) 的命令同步;  
  不改业务代码.
- 出处: glm TE2 (中), qwen TST-02 (未定级).

### PA-100 (中) 插桩测试今天跑不起来: 指定的 runner 不在 classpath

- `KotlinAndroid.kt:75` 设 `androidx.test.runner.AndroidJUnitRunner`, 而 androidTest 依赖  
  只有 `androidx.test:core` 与 `androidx.test.ext:junit` (`gradle/toml/android.toml:53-54,105`),  
  没有 `androidx.test:runner` 也没有 `rules`; 官方的插桩测试安装指引要求显式加 runner.
- 参考修法: 补 `test-runner` (与需要的 `test-rules`), 然后证明 `connectedFossDebugAndroidTest`  
  能跑起来.
- 出处: qwen TST-03 (未定级). 本文件的级别是估计.

### PA-101 (中) 缺 `kotlinx-coroutines-test` 与 Turbine

- `gradle/toml/kotlin.toml:9,19` 只有 `kotlinx-coroutines-android`; `coroutines-test` 与 Turbine  
  全仓库零命中. `BaseListViewModel.kt:65` 的 `viewModelScope.launch`, `:79-87` 与  
  `HomeViewModel.kt:84-97` 这些行为今天技术上无法断言.
- 参考修法: 两个依赖进 test bundle, 先断言两条已写明的契约 (刷新保留旧列表, `loadJob` 去重).
- 出处: qwen TST-05 (未定级). 本文件的级别是估计.

### PA-102 (中) 没有可替换的测试替身入口

- `di/AppGraph.kt:87-90` 的 `Factory.create(@Provides app: MyApplication)` 不接受额外的  
  binding container, 四个叶子绑定换不掉: `PropertyDefault` (`:192`),  
  `MyApplication.sharedPreferences` (`:207`), OkHttp `HttpClient` (`:116`),  
  `ShellLibSu` 贡献的 `IShell` (`ShellLibSu.kt:15-16`); 加上 114 处静态取用 (PA-02)  
  与可变的 `myAndroid` (PA-01), 仓库与 ViewModel 无法在宿主 JVM 上构造.
- [04-架构](../../agents-cn/04-架构.md) 的 interface-first 只覆盖了 `IProperty` / `IShell`, 它们正好是现成的替换点.
- 参考修法: 工厂签名加 binding container 变长参数 (或另建测试图), 资源与 Context 走注入.
- 出处: deepseek PA-42 (中), glm TE4 (中), mimo TEST-2 (中), qwen TST-04 (未定级).

### PA-103 (高) release 变体零测试覆盖

- `testBuildType` 未设置, 所以 androidTest 只跑 debug 变体 (AGP 的默认值),  
  而 release 打开 R8 与资源收缩 (`app/build.gradle.kts:88-96`).
- 这个应用的运行时恰好全是 R8 敏感项: 隐藏 API 反射 (`PropertyDefault.kt:8-18`,  
  `HomeRepository.kt:1045-1111`), kotlinx.serialization 模型  
  (`ui/home/model/Lld.kt` 里每个模型类都带 `@Keep`, 实测 9 处), libsu 与 JNI;  
  参考模型里负责这一层的第 5 层 (release candidate, 压缩后的二进制) 完全没有设施.
- 参考修法: 至少给 release 变体留一条能跑的路径; 否则 "debug 能跑" 证明不了发布版能跑.
- 出处: deepseek PA-40 (高).

### PA-104 (中) 纯逻辑大量未测

- 可测而没测的 (多数是纯判断, 少数要先开可见性或抽纯函数):  
  `SettingsStore.parseThemeMode` / `parseScrollBarMode` 与那次迁移 (`SettingsStore.kt:79-91,116-143`,  
  两个函数是 private); `LldFileStore.copyJsonIfNeededOrThrow` 的版本比较与目录分支 (`:46-65`,  
  函数体绑定 `MyApplication.instance.assets` 与下载路径 `:32,61,76`, 要先抽纯函数);  
  `PropRepository.getBuildProp` 的多行折叠 (`PropRepository.kt:79-93`) 与 `toPropMyModel` 的解析  
  (`MyModelExt.kt:127-134`); `MyModel.key` (`MyModel.kt:24-28`) 与 `AppRoot` 的下标 clamp  
  (`AppRoot.kt:88`); `HomeRepository.isDateHigherThanConfig` 里硬编码的 `250205 - 250101`  
  (`:212-253`, 偏移在 `:249`); `base/.../extension/ExceptionExt.kt:3-4` 的 `fullMessage`  
  (18 处调用点, 见 PA-125); `MyApplication.kt:26-30,45-46` 的深浅判定与滚动条判定;  
  `MountDataSource.kt:27-33` 的六列切分; `DateTimeExt.kt:28` 的 Long 重载与 `:35-37` 的 URL 选择依据.
- 这些都只需要一个手写 `FakeShell : IShell` 就能测, 不需要 DI 图.
- 参考修法: 测试基础设施就位后从纯逻辑补起 (顺序见第 9 节的第 9 组).
- 出处: deepseek PA-41 (中), qwen TST-07 (未定级).

### PA-105 (中) lld.json 的契约没有测试

- 随包资产 `app/src/main/assets/lld.json` 是现成 fixture, `Lld.kt:12-19,34-55` 要求每个字段非空,  
  `JsonExt.kt:6` 只关了未知键, 而 `DateTimeExtTest.kt:14` 只断言硬编码字面量, 不读资产.
- 后果: 上游数据一改, 只能靠运行时被 catch 掉再显示 unknown; what-to-test 把  
  "损坏数据, 畸形 JSON, 边界条件" 列在必测里, 而解析与回退链正好是这类.
- 参考修法: 一个 JVM 测试把资产解成 `Lld` 并断言 `version` 过 `isLldDatetime()`;  
  再补反序列化 (`JsonExt.kt:8-12`) 与回退链 (`HomeViewModel.kt:101-204`) 的坏数据用例.
- 出处: qwen TST-06 (未定级). 本文件的级别是估计.

### PA-106 (低) 模板测试与 flavor 冲突, 且模板本身没有价值

- `app/src/androidTest/.../ExampleInstrumentedTest.kt:20` 断言 targetContext 的包名等于  
  `net.imknown.android.forefrontinfo`, 而 debug 变体带 `applicationIdSuffix = ".$name"`  
  (`app/build.gradle.kts:105`), 所以这条断言在 debug 变体上必然失败; 它今天跑不起来 (PA-100)  
  所以没暴露. 复核更正: `:base` 那一份断言的是 base 包名, 与 `:base` 无后缀一致, 不受影响.
- 两个 `ExampleUnitTest` 断言 2+2, 没有覆盖价值.
- 参考修法: 直接删掉全部模板测试; 需要断言包名时用  
  `InstrumentationRegistry.getInstrumentation().targetContext.packageName` 与构建期值比较,  
  或不断言包名.
- 出处: deepseek PA-45 (中), glm TE3 (低), mimo TEST-1 (中), qwen TST-21 (未定级).

### PA-107 (中) UI 与截图测试的基础设施为零

- androidTest 侧没有 `ui-test-junit4`, 也没有 `ui-test-manifest` (`gradle/toml/android.toml:68-75`  
  与 `Compose.kt:12-28` 只给 BOM platform, 解析出 Compose UI 1.12.1), 没有 `screenshotTest`  
  源集与相应依赖, 所以 `createAndroidComposeRule` 这类入口今天连编译都过不了;  
  四个页面 (AppRoot, Home/Others/Prop 列表, Settings) 没有 UI 行为测试, 也没有截图基线.
- 而 screen / content 拆分与 `previewModels` 正是现成的天然边界: `MyModelListScreen.kt:82-166`,  
  `SettingsScreen.kt:126-468`, `AppRoot.kt:182-199`; 官方把截图测试点名为 Compose 视觉回归的  
  首选, 并强调不要做全组合 (建议最小面起步: `MyModelCard` 与设置页关键行).
- 参考修法: 先补两个测试依赖做一条行断言, 再按 locale 做卡片截图.
- 出处: deepseek PA-43 (中), mimo TEST-4 (中), qwen TST-09 (未定级).

### PA-108 (中) 官方已把 v1 测试入口标为废弃, 新套件应直接写 v2

- 测试主页的示例仍用 `createComposeRule()`, 而迁移页把 `test.junit4.createComposeRule`,  
  `createAndroidComposeRule`, `createEmptyComposeRule`, `AndroidComposeTestRule`,  
  `runComposeUiTest` 列为废弃并建议迁移, v2 住在 `androidx.compose.ui.test.junit4.v2`  
  与 `androidx.compose.ui.test.v2`; 同一页还写明 v1 走 `UnconfinedTestDispatcher`,  
  可能掩盖真实的时序与竞态.
- 本工程 BOM 锁值 (`gradle/toml/android.toml:22`, `2026.09.00`) 解析出的 Compose UI 是 1.12.1  
  (ui-test-junit4 同版本, ui-test-manifest 在列), v2 包在这一线已经存在.  
  后果: 现在写下的第一个测试就落在废弃面上, 而且比生产环境更少暴露竞态.
- 参考修法: 两个 v2 artifact 进 catalog 的命名别名 (与 PA-118 的拆分一起做),  
  从第一条测试起用 v2 加 `StandardTestDispatcher`, 每次触发后显式 `waitForIdle()` 或 `runOnIdle()`.
- 边界: v2 包是否存在, 废弃清单与 `UnconfinedTestDispatcher` 的说明都属官方文档论断,  
  仓库内只能核对 BOM 解析出的版本.
- 出处: qwen TST-11 (未定级). 本文件的级别是估计.

### PA-109 (中) 测试容器与 Activity: 缺 `ui-test-manifest`, `testApplicationId` 让每个插桩测试跑完整 `Application.onCreate`

- 测试主页与常见模式页都要求 `debugImplementation(ui-test-manifest)`, 作用是把  
  `ComponentActivity` 写进测试 apk 的清单; 仓库没有这条依赖, manifest 只声明 `.ui.MainActivity`  
  (`AndroidManifest.xml:23-31`).
- `KotlinAndroid.kt:72-83` 与 `app/build.gradle.kts` 的 `defaultConfig` 没有  
  `testInstrumentationRunnerArguments` (全仓库零命中), `testApplicationId` 取默认值意味着  
  每个插桩测试都要执行 `MyApplication.onCreate` (建图, 反射, 设置播种, libsu builder,  
  即 PA-88), 被测应用无法在测试里变轻.
- 另一条复核更正: `AppRoot.kt:79` 与 `Theme.kt:312` 读 `LocalActivity.current` 并不构成  
  "必须用 Activity 规则" 的理由, 因为 `createComposeRule()` 也在测试 Activity 里 setContent,  
  所以 `Theme.kt:314` 的 `activity != null` 判断在测试里走的是真写窗口 flags 的一侧  
  (只有 `@Preview` 才跳过).
- 参考修法: 补这条 debugImplementation; 需要 Activity 的用例按常见模式页用  
  `createAndroidComposeRule<ComponentActivity>()`; 先给 `testApplicationId` 与一个测试用 Application.
- 出处: qwen TST-12 (未定级), qwen TST-20 (未定级).

### PA-110 (中) `AppRoot` 离不开 DI 图, 导航与标签页切换今天测不了

- `AppRoot.kt:110-113` 四处调 `metroViewModel<...>()`, MetroX 的 `LocalMetroViewModelFactory`  
  要由 `CompositionLocalProvider` 提供, 唯一提供点是 `MainActivity.kt:76`,  
  而 `AppGraph.kt:87-90` 的工厂不收测试容器 (PA-102): 测试 API 页推荐的  
  `setContent { AppRoot() }` 形态会在第一帧失败.
- 依据: 上游 `metrox-viewmodel-compose` 把 `LocalMetroViewModelFactory` 的默认值写成  
  `error("No MetroViewModelFactory registered")`, 所以未提供时第一次 `metroViewModel()` 调用即失败  
  (`MainActivity.kt:67-70` 的注释也这么声称, 但这条判定站在上游源码上).
- 参考修法: 手写一个返回假 ViewModel 的 `MetroViewModelFactory` 替身,  
  连同 `LocalThemeMode` 与 `LocalScrollBarMode` 两条流组成一个测试用包装.
- 出处: qwen TST-13 (未定级). 本文件的级别是估计.

### PA-111 (低) 三个纯数据 composable 里两个是 private

- 常见模式页的 "Test in isolation" 与测试主页的 `setContent { MainScreen(uiState = fakeUiState) }`  
  形态要求页面级纯数据组件可见; `SettingsContent` (`SettingsScreen.kt:127`)  
  与 `AppRootShell` (`AppRoot.kt:144`) 都是 private, 只有 `MyModelListContent`  
  (`MyModelListScreen.kt:82`) 是 internal.
- 同一处也有一条好消息: `MyModelListScreen` 今天就可测, 因为它的参数是抽象的  
  `BaseListViewModel` (`:47`), `collectModels()` (`BaseListViewModel.kt:34`)  
  可以被手写的假子类覆盖; 而 `SettingsScreen` 收的是具体的 `SettingsViewModel` (`:77`),  
  它的构造链要求 `SettingsStore(SharedPreferences)`, 后者在构造期读静态资源  
  (`SettingsStore.kt:27,33`).
- 参考修法: 两个 private 改 internal; `SettingsScreen` 改收状态或接口.
- 出处: qwen TST-14 (未定级). 本文件的级别是估计.

### PA-112 (中) 真实后台工作不在测试时钟内, `waitForIdle()` 等于没等

- 同步页写明虚拟时钟只驱动 Compose 层次, `MainTestClock` 不管 Android 的 measure 与 draw,  
  影响 UI 状态的后台工作要 `registerIdlingResource`, 依赖外部工作的条件应改用 `waitUntil()`.
- 本项目的链路是 `MyModelListScreen.kt:59` 到 `BaseListViewModel.kt:65-71` 再到  
  `HomeViewModel.kt:211-241` (20 处属性读取, 若干 shell 调用, 一次全量 `getInstalledApplications`,  
  外加可选的网络请求): `waitForIdle()` 返回时列表还停在 `MyModelListScreen.kt:71` 的空值上,  
  断言只能靠超时或不确定的等待.
- 参考修法: 页面级测试用假 `BaseListViewModel` (PA-111); 只有 `AppRoot` 冒烟用例才用  
  `waitUntilAtLeastOneExists` 或 `waitUntilNodeCount`.
- 出处: qwen TST-15 (未定级). 本文件的级别是估计.

### PA-113 (中) 全仓库零 `testTag`

- `testTag` 与 `testTagsAsResourceId` 在 `:app` 与 `:base` 零命中;  
  测试 API 页的选择器示例是 `onNode(hasTestTag(...))`, 互操作页要求把  
  `Modifier.semantics { testTagsAsResourceId = true }` 加在靠近层级的根部,  
  否则 UI Automator 只能按显示文本与 contentDescription 定位,  
  而标签页图标是 `contentDescription = null` (`AppRoot.kt:169`),  
  卡片标题由 `stringResource` 解析 (`MyModelCard.kt:99`), 明细是数据层拼好的字符串 (PA-11).
- 后果: 任一 UI 断言都会随译文措辞改动失效, 四种 locale (`app/build.gradle.kts:46-49`)  
  等于四套断言目标.
- 参考修法: 一个 `TestTags` 对象给卡片, 开关行, 单选行, 对话框条目和标签页,  
  并在 `AppRoot.kt:149` 的 `Scaffold` 上开 `testTagsAsResourceId`.
- 出处: qwen TST-16 (未定级). 本文件的级别是估计.

### PA-114 (中) 现有 semantics 会让断言假通过, 或根本无从断言

- (a) `MyModelCard.kt:42-49` 的 `Card(onClick = {})` 让测试 API 页那种打印树里出现  
  `Actions = [OnClick]` 与 `MergeDescendants = 'true'`, 于是 `performClick()` 和  
  `hasClickAction()` 双双通过而实际无事发生 (与 PA-74 同一处代码).
- (b) `MyModelCard.kt:74-83` 的状态圆点只有 `clip` 与 `background`, 不带任何 semantics,  
  所以 "这一行是红是绿" 查不到; 常见模式页反对为纯视觉属性自造 semantics,  
  给的路子是像素比对, 而本仓库没有截图测试基础设施 (PA-107).
- (c) `SettingsScreen.kt:341,378` 的 `semantics(mergeDescendants = true)` 把标题与摘要并成一个 Text,  
  `assertTextEquals(标题)` 因此失败, 要用测试 API 页写明所有 finder 都支持的  
  `useUnmergedTree = true`; 机器可读的状态只剩 `Role.Switch` 与 `Role.RadioButton` (`:344,379`).
- 出处: qwen TST-17 (未定级). 本文件的级别是估计.

### PA-115 (低) 中间帧要显式驱动帧时钟

- `MyModelCard.kt:55` 的 `animateContentSize()`, `MyModelListScreen.kt:141` 的 `animateItem()`,  
  `ScrollBarExt.kt:42-49` 的 300ms 延迟加 250ms tween 都会吃进隐式等待,  
  想在动画中途查询就要同步页的 `mainClock.autoAdvance = false` 配 `advanceTimeByFrame()`,  
  逐帧查询再用 `runWithoutImplicitWait()` (该 API 从 ui-test 的 1.12.0-alpha03 起,  
  现锁的 BOM 线已包含).
- 另外 `ScrollBarExt.kt:56` 的滚动条只在 `drawWithContent` 里绘制, 没有可查询的节点,  
  所以它显示与否今天无法用 semantics 断言 (与 PA-73 同一处).
- 出处: qwen TST-18 (未定级). 本文件的级别是估计.

### PA-116 (低) 组合外的 Toast 断不到, locale 覆盖只覆盖一半文案

- `ui/base/ext/ToastExt.kt:8-9` 由 `SettingsScreen.kt:117-121,424` 调用,  
  弹的是 `android.widget.Toast`, 位于组合之外; 常见模式页对这类副作用给的做法是换  
  `SnackbarHost`, 于是 PA-19 的彩蛋计数今天没有任何可断言的出口.
- locale 侧, 常见模式页的 `DeviceConfigurationOverride.Locales()` 只改变 `stringResource`  
  的解析结果, 不改变数据层已经用 `MyApplication.getMyString` 拼好的明细串  
  (`HomeViewModel.kt:250`, `SettingsRepository.kt:34`), 所以一条 locale 用例里同一张卡片的两半  
  来自两次解析 (同 PA-11 的边界).
- 参考修法: 明细改传 id, 副作用改 `SnackbarHost`, 这两类断言才有可写的出口.
- 出处: qwen TST-19 (未定级). 本文件的级别是估计.

### PA-117 (中) Robolectric 与 `testOptions` 都没有

- `gradle/toml/*.toml` 无 robolectric 条目, 全仓库没有 `testOptions` 块  
  (`KotlinAndroid.kt:72-83` 只设 runner 与两个 bundle), 于是 15 处 `Log.*` (PA-69)  
  在宿主测试里会直接抛 "not mocked"; 官方本地测试页给的两种办法是 `returnDefaultValues`  
  或 Robolectric, 前者会把崩溃变成静默错值.
- Robolectric 页要求 `isIncludeAndroidResources = true` 加 `@RunWith(AndroidJUnit4::class)`.
- 参考修法: 建图之后引入 Robolectric, 用于 `MainActivity`, `SettingsScreen`  
  与一切读资源的代码.
- 出处: qwen TST-08 (未定级). 本文件的级别是估计.

### PA-118 (低) 测试依赖无差别发给所有模块

- `KotlinAndroid.kt:78-81` 把两个 bundle (`gradle/toml/android.toml:104-105`)  
  发给每个应用约定的模块, `:binderDetector` 没有测试源集也照样拿到 `junit`,  
  `base/build.gradle.kts:13-15` 自己不声明任何测试依赖; 结果是 "某个模块用什么测"  
  在读模块构建文件时看不出来, 缺失也看不出来.
- 参考修法: 按层拆命名别名 (unit, instrumentation, composeUi, robolectric, screenshot),  
  在需要处声明.
- 出处: qwen TST-10 (未定级). 本文件的级别是估计.

### PA-119 (低) JUnit 5 尚未接入 (机制与决策点)

- 现状是 JUnit 4 (`gradle/toml/android.toml:47` 的 `4.13.2`,  
  `base/.../DateTimeExtTest.kt:3,6` 用 `org.junit.Assert` 与 `@Test`), catalog 里没有 JUnit 5 坐标.
- 机制: 本地测试确实是 Gradle 的 `Test` 任务 (`app/build/test-results/testFossDebugUnitTest/`,  
  `base/build/test-results/testDebugUnitTest/`), 给任务加 `useJUnitPlatform()` 就能跑 Jupiter;  
  插桩侧仍是 JUnit 4 (`KotlinAndroid.kt:75` 的 runner 是 JUnit 4 runner,  
  社区插件 `de.mannodermaus.android-junit5` 把 Jupiter 插桩限制在 API 26 以上,  
  高于 `gradle/toml/build.toml` 的 minSdk 键, 且它与本仓库 AGP 线的兼容性未经确认).  
  官方本地测试页写的也是 JUnit 4.
- 现实形态可能是混合: 纯逻辑与 ViewModel (配 `kotlinx-coroutines-test` 的 `runTest`  
  与 Main dispatcher 替换) 走 JUnit 5, Compose 与插桩保留 JUnit 4; 另一选项是全 JUnit 4.  
  在覆盖还是一个文件的现在换框架得不到收益, 这是决策点 (见第 9 节的测试基础设施决策).
- 出处: deepseek PA-46 (低), mimo TEST-3 (低), qwen 的 JUnit 5 一段 (未定级).

### PA-120 (中) 没有测试策略文档与约定

- `docs/agents-cn/` 现在是 01-判据 到 13-任务工作流 的十三份编号文件, 没有 testing;
  规则文件里也没有测试一节.  
  于是分层, 跑在哪, 什么时候跑, 谁负责都没有书面定义, 而 Testing strategies  
  明确要求写一份测试策略并随团队演进.
- 另外现在没有任何入口在跑测试: androidTest 需要设备, `testFossDebugUnitTest` 不在自动化里  
  (CI 只跑 `assembleFossDebug`, `.github/workflows/android-ci.yml:36`);  
  测试在何时何地跑属于 CI 角度, 本条只指出这一层目前是空的.
- 出处: deepseek PA-44 (中).

### PA-121 (低) 没有性能与稳定性测试层

- 参考清单里的 Performance testing 与 Big test stability 对不到任何设施:  
  没有 benchmark 或 macrobenchmark 模块 (`settings.gradle.kts:54-56` 只含三个模块),  
  没有 Baseline Profile (与 PA-89 同源), 也没有 flake 策略.
- 对这个应用尤其相关: 启动与检测耗时是它的主要性能面 (PA-85, PA-86, PA-92),  
  而没有任何自动化的耗时或帧时间基线.
- 出处: deepseek PA-47 (低).

## 8. 卫生与小项

### PA-122 (中) catch 分支让类型退化成 `Any`

- `OthersRepository.kt:85-89`: `val binderVersion = try { ... } catch (e: UnsatisfiedLinkError) { e.printStackTrace() }`,  
  catch 分支的值是 `Unit`, 所以 `binderVersion` 被推断为 `Any`, 之后 `:91` 的  
  `when (binderVersion) { -ERRNO_... -> ... }` 虽然能编译, 但已经失去类型约束  
  (把返回值写成任何类型都不会报错).
- 参考修法: catch 分支返回明确的失败值 (例如 `null` 或哨兵整数).
- 出处: deepseek PA-36 (中).

### PA-123 (低) 小规模效率与副作用

- `PropRepository.getBuildProp` 在循环里用 `+=` 累加字符串 (`PropRepository.kt:81-93`),  
  `getprop` 输出上千行时是 O(n^2).
- `SettingsDataSource.getSettingsOrThrow` 在 `filter` 谓词里写 `it.isAccessible = true`  
  (`SettingsDataSource.kt:13-16`): 带副作用的判断条件.
- 出处: deepseek PA-38 (低).

### PA-124 (低) `ShellDefault` 有意保留, 但读流顺序会死锁

- `base/src/main/java/.../base/shell/impl/ShellDefault.kt:14-21`: 先 `process.waitFor()` (`:15`)  
  再读流 (`:20-21`), 命令输出超过管道缓冲区就永久阻塞; 全仓库没有调用方  
  (在用的是 `ShellLibSu`), [05-代码规则](../../agents-cn/05-代码规则.md) 把它记为有意保留的预备实现.
- 它不是缺陷, 但启用前必须先修死锁; 它还把参数拼进 `sh -c` (`:14`),  
  所以启用它而不逐个复查调用方会引入 PA-72 记的那条形状风险.
- 参考修法: 保留的前提下把读流移到 `waitFor()` 之前, 或加显著标记并记下启用前的必做修复  
  (见 [第 11 节](#11-需要负责人判定的问题) 的 Q-04).
- 出处: deepseek PA-21 (低), glm DP3 (中), mimo PATTERN-10 (低), qwen ARC-08 (未定级).

### PA-125 (低) `fullMessage` 丢掉整条 cause 链, 异常类名会进用户可见的行

- `base/.../extension/ExceptionExt.kt:3-4` 的 `fullMessage` 有 18 处调用点;  
  它只带一层 cause 的 `message` (`Caused by: ${cause?.message}.`), 更深的 cause 链与每个 cause 的类型  
  都丢掉, 没有 cause 时输出 `Caused by: null.`; 而异常类名会出现在用户可见的行里  
  (`MyModelExt.kt:158` 等).
- 出处: qwen TST-07 (未定级), issues-cn #24. 本文件的级别是估计.

## 9. 优先级与建议顺序

排序原则 (沿用优先级报告): 主体按 "会不会让应用报出假结论或用户可感的错误" 排,  
不按修改难度, 复杂度, 工时排; 测试那一组另按 "它是不是前面那些重构的防回归测试,  
以及它覆盖的是不是应用的判断核心" 排. 每组的修法只是改造时的参考, 不是子任务列表.  
每组的 "优先级报告在本组引到的条目" 是从归档报告的对应组里出现的来源编号换算来的  
(经第 12 节的索引), 所以它列的是那一组引用过的条目, 不是互斥的成员集合:  
同一个条目会被多个组引用 (当证据或关联), 条目自身的归属看它自己的正文.

### 第 1 组: 全局版本事实 `myAndroid`

四份来源都指它, 其中三份定 "高". 同一进程里 Home 加载前后对同一台设备可以给出不同的 API 判定;  
四次独立赋值之间落进来的读会拿到跨代组合; 覆盖值只写不清, 没有复位路径;  
载荷经这里写回之后, 所有 API 检查都被改写 (PA-61). 它也是第 3 组的前置.  
修法见 PA-01; 一处必须小心: `initMyAndroid()` 第一句就读全局, 直接把 `var` 改 `val` 会自我引用.  
关联: `Theme.kt:322` 绕过 `isAtLeastAndroid12()` 直接写 `Build.VERSION.SDK_INT >= S`,  
同一条 API 门槛在 ViewModel 的守卫与仓库的 `@RequiresApi` 里各写了一次 (PA-08).

优先级报告在本组引到的条目: PA-01, PA-08, PA-60, PA-61.

### 第 2 组: 加载的失败边界与检测执行方式

glm 定 "高", deepseek 与 mimo 定 "中", qwen 记同一条. 这条路径通向的是一直转圈或崩溃,  
不是一行错误提示; 而且这是用户每次都付的代价: 首屏要等整条检测跑完,  
慢设备上依次执行的 shell 与包枚举直接相加. 修法见 PA-14, PA-85, PA-86.

优先级报告在本组引到的条目: PA-05, PA-14, PA-16, PA-20, PA-21, PA-34, PA-46, PA-54, PA-68,  
PA-81, PA-85, PA-86, PA-93. 其中 PA-34 与 PA-46 的部分归第 8 组, PA-93 与 PA-81 归第 6 组,  
PA-05 也见第 7 组.

### 第 3 组: 结构与分层的根本原因

glm 定 "高", mimo 定 "中"; qwen 把它排在判定正确性之后. 从包名判断不出依赖方向,  
任何特性都抽不出来也单独推理不了; 它与第 2 组的检测项清单, 第 1 组的改成不可变是同一轮工作.  
修法: 采用 `docs/dev/module-structure-cn.md` 里的目标结构 (该文件标注目标结构尚未经负责人逐项定下,  
动工前先定): `:core` 放与平台无关的模型与工具 (含常量表, `Lld`, 日期与版本比较),  
`:feature/*` 各自独立 (Screen + ViewModel + Repository + 专属 DataSource),  
`MyApplication` 移到 `:app` 根包, `:base` 只留 `IShell` / `IProperty` 接口.  
先把 Metro 图接完, 再拆模块; 搬移只是移动文件与改引用, 但规则文件里记的路径要一起改:  
`ShellLibSu` 的路径就写在 [docs/agents-cn/04-架构.md](../../agents-cn/04-架构.md) 里.

优先级报告在本组引到的条目: PA-02, PA-22, PA-23, PA-24, PA-25, PA-26, PA-27, PA-28, PA-29,  
PA-30, PA-31, PA-35, PA-36, PA-37, PA-38, PA-39, PA-40, PA-41, PA-42, PA-43, PA-49, PA-55, PA-86.  
本组主题里还有一条 PA-48 (`StatusColor` 的位置) 归档报告没有在本组引到.

### 第 4 组: `lld.json` 的校验与它周围的信任链

glm SE1 定 "高". 这份文件决定安全相关的行 (安全补丁, targetSdk 阈值, WebView 结论),  
一旦被覆写或被上游账号失陷替换, 每一行结论立刻翻转; 载荷还会写回全局版本事实 (PA-61),  
影响扩大到所有 API 检查. 按优先级报告的构成, 这一组还含: 备份规则是空元素 (PA-63),  
离线路径的 version 只做字典序比较 (PA-62), 外部路径上的递归删除 (PA-67),  
配置期读 git 与时钟 (把 git 状态拼进数据 URL 并静默降级成离线, PA-53, PA-71),  
以及两个直接写在 `settings.gradle.kts` 里的插件版本 (PA-51).  
修法见 PA-09, PA-59, PA-60, PA-62, PA-63.

优先级报告在本组引到的条目: PA-06, PA-09, PA-51, PA-53, PA-59, PA-60, PA-61, PA-63, PA-67,  
PA-71. 另有一条本文件补进本组主题: PA-62 (离线路径的 version 只做字典序比较),  
归档报告在本组只说 "比较版本与日期统一解析后再比", 没有点编号.

### 第 5 组: Prop 页对每个键各查一次

glm 定 "高", mimo 定 "中", qwen 把它排进性能那一轮. 每次打开 Prop 页都会执行,  
键的数量是几百量级, 低端机上能感觉到. 修法见 PA-87.

优先级报告在本组引到的条目: PA-87.

### 第 6 组: 启动与首屏性能加 Baseline Profile

deepseek 两处定 "高"; qwen 把性能排成独立一轮. 这些都是每次启动或每次加载都会发生的成本,  
与第 2 组的首屏问题相加. 修法见 PA-88 与 PA-89; 另有 PA-90 到 PA-97 的重复计算,  
以及 Compose 侧的低分项 PA-73, PA-75, PA-77, PA-82.

优先级报告在本组引到的条目: PA-70, PA-73, PA-74, PA-75, PA-77, PA-80, PA-82, PA-83, PA-84,  
PA-88, PA-89, PA-90, PA-91, PA-92, PA-93, PA-95, PA-97. 其中 PA-70 归第 8 组,  
PA-74 与 PA-80, PA-83, PA-84 是 Compose 侧与测试侧的低分项.

### 第 7 组: 把判断与展示分开

glm 与 mimo 定 "中", deepseek 的相关条目定 "低", qwen 归入 `ARC-01` 并说改哪一边要负责人判定  
(Q-07); 但它在结构上是最深的一处, 也是 PA-22 与 PA-36 的共同原因: 界面模型被下推到数据层,  
于是仓库必须有 `Context` 才能干活, `ui.common` 反向依赖 home, 包环跟着出现.  
连带一起处理的: PA-05 (判定没有结果类型), PA-06 与 PA-07 (逐行各写各的比较),  
PA-10 (设置键的映射抄三份), PA-11 (标题与明细来自两个 Context), PA-15 (补丁路径重读, 手写计数器),  
PA-19 (一次性事件用返回值), PA-42 (格式串与参数分离), PA-44 (`OrThrow` 后缀), PA-45 (失败行四种写法),  
PA-11 与 PA-116 的边界.  
彻底修法: 取数层返回 "发生了什么" (有值 / 无值 / 失败), 判断与格式化分开,  
展示集中到 UI 层唯一的一处; 版本与日期统一解析后再比; 编排与回退从 ViewModel 下沉到数据层.  
与第 2 组的检测项清单是同一轮: 清单里的每一项持有自己的结果类型与比较方式.

优先级报告在本组引到的条目: PA-04, PA-05, PA-06, PA-07, PA-10, PA-11, PA-15, PA-19, PA-22,  
PA-24, PA-29, PA-36, PA-44, PA-45, PA-55.

### 第 8 组: 安全与卫生的低分项

按优先级报告的清单收在这里: PA-34 (返回只有 `finish`, 与多栈骨架不匹配),  
PA-46 (反射被当作 API 使用, 公开替代物与 Q-05 的回头改),  
PA-47 (注释掉的代码与无人使用的分支), PA-52 (flavor 维度名与版本名标记),  
PA-64 (`keys/debug.keystore` 与 release 签名缺失), PA-65 (`QUERY_ALL_PACKAGES`),  
PA-66 (属性未清洗入路径), PA-68 (隐藏 API 反射失败静默), PA-69 (日志门控),  
PA-70 (Firebase 遥测与 `gwpAsanMode`), PA-72 (`IShell` 收整行命令), PA-122 (catch 让类型退化),  
PA-123 (`+=` 与过滤器副作用), PA-124 (`ShellDefault` 死锁), PA-125 (`fullMessage` 的 cause 链).

优先级报告在本组引到的条目: PA-34, PA-46, PA-47, PA-52, PA-64, PA-65, PA-66, PA-68, PA-69,  
PA-70, PA-72, PA-104, PA-122, PA-123, PA-124, PA-125. 其中 PA-104 与 PA-125 的测试那一半见第 9 组;  
PA-52 (`Flavor.kt` 的维度名) 在归档报告里写作 `MOD-06` 而没有带来源名, 补零的写法是 qwen  
那一份的编号 (`mimo MOD-6` 是另一件事, 见 PA-25 与 PA-27).

### 第 9 组: 测试

qwen 把它放第一步 (先让回归可见), glm 说第一批纯 JVM 测试不受重构影响可先行;  
现在的防回归测试几乎是零: 全仓库 5 个测试文件里只有 `DateTimeExtTest.kt` 一个真测试.

不等重构就能做的: PA-99 (命令跑不到真测试), PA-100 (插桩 runner 缺失), PA-101 (`coroutines-test` 与 Turbine),  
PA-103 (release 变体), PA-104 (纯逻辑清单), PA-105 (lld.json 契约), PA-106 (删模板测试),  
PA-117 (Robolectric 与 `testOptions`), PA-118 (测试依赖分发).

要等重构的: PA-102 (替身入口) 与 PA-110 (`AppRoot` 离不开图); glm TE4 列的阻断项是 PA-01, PA-02,  
PA-04 与 PA-14, 所以这一半随第 1 到第 3 组.

UI 与截图: PA-107 到 PA-116.

四个基础设施决策: (1) JUnit 5 混合还是全 JUnit 4 (PA-119); (2) Compose 测试先按 BOM 内的 v1 写起,  
还是等 v2 齐后起 UI 层 (PA-108); (3) 截图方案选 AGP test-suite 还是 Roborazzi (PA-107);  
(4) 覆盖率工具 (例如 Jacoco) 是否引入.

优先级报告在本组引到的条目: PA-98, PA-99, PA-100, PA-101, PA-102, PA-103, PA-104, PA-105,  
PA-106, PA-107, PA-108, PA-109, PA-110, PA-111, PA-112, PA-113, PA-114, PA-115, PA-116,  
PA-117, PA-118, PA-119, PA-125.

### 建议顺序

从高到低: 第 9 组的 "不等重构" 半段; 第 1 组; 第 2 组与第 3 组 (第 9 组的 "要等重构" 半段随第 1 到第 3 组);  
第 4 组; 第 5 组与第 6 组; 第 7 组; 第 8 组.  
先做第 9 组的 "不等重构" 半段, 理由是先立起防回归的测试, 让后面那些重构被验证, 而不是靠人眼.

## 10. 与 issues-cn 的分歧

`docs/dev/issues-cn/` 是更早的调查; 下面是它与四份分析结论相反, 或者它没有覆盖的地方,  
一律以代码复核为准 (复核日期与 [文件头](#项目问题清单-合并版) 同):

| 主题 | issues-cn | 本清单 | 代码复核 |
|---|---|---|---|
| 加载失败边界 | #62 判 "有意接受" (UI 不 try, 契约是下层不抛) | PA-14 | `BaseListViewModel.kt:60-71` 仍没有失败分支; `IShell` 没有 "不抛" 的声明. 没有失败分支属实; 正解是下层不抛 (与 [05-代码规则](../../agents-cn/05-代码规则.md) 一致), 不是让 UI 吸收异常 |
| `myAndroid` | #09 P0, #17 P2 | PA-01 | 一致 |
| 模块环与共用包名 | #04 P1, #07 P2 | PA-22, PA-25 | 代码一致; 定级不同 |
| 静态取全局 | #02 P1 (DI 已接入, 剩全局取用点) | PA-02 | 一致, 本文更强调 "与图并行" |
| lld.json | 外部目录无条目; `scheme` 与 `toInt()` 是 #22, `LldFileStore` 的直接比较是 #19 | PA-09, PA-59, PA-60, PA-62 | 代码属实 (`MyApplication.kt:60-71`, `LldFileStore.kt:45-64`) |
| 无 Baseline Profile | 无条目 | PA-89 | 属实 (`settings.gradle.kts` 没有 profileinstaller) |
| 首屏要等整条检测 | 串行那半是 #15 (P1); 首屏那半无独立条目 | PA-85, PA-86 | 属实 (`BaseListViewModel.kt:26-27,65-70`) |
| 空 `onClick` 的 Card | #34 P1 (语义的代价还没还) | PA-74 | 代码属实 (`MyModelCard.kt:49`); 是否算缺陷需要负责人定 |
| 失败折成结论 | #63 P1 | PA-04, PA-05 | 现象属实, 但多发生在很少走到的分支 (例如 SELinux 的权限拒绝), 概率低 |
| 列表 key 重复导致崩溃 | #20 已修 (Prop 改用数据键) | PA-54 | 立即崩溃的那条路已经关闭, 结构上仍然容易出错 |
| `@Stable` 冗余 / 圆点颜色 / Others 失败行红点 | #47 暂缓; #33 有意接受; #69 已修 | 未涉及或一致 | 一致, 不动 |

issues-cn 独有, 四份分析没覆盖, 但代码能证实的, 仍然值得做, 只是不定高:  
**#65 外链跳转** (`SettingsScreen.kt:421` 用 `resolveActivity` 判浏览器, manifest 没有 `<queries>`,  
按平台的包可见性语义在 Android 11+ 可能判空; 动手前需要在一台真机上确认一次),  
**#66 标题与数据源不符** (`os_arch` / `build_cpu_abi` / `build_stock_fingerprint` 的标题与取值来源不同),  
**#40 圆点没有语义** (读屏拿不到判定色, 与 PA-74 一起做).

## 11. 需要负责人判定的问题

- **Q-01** `QUERY_ALL_PACKAGES` (PA-65) 是保留并申报, 还是把过时 targetSdk 那一行改成 `<queries>`  
  加限定查询? 这条权限同时掩盖了 `SettingsScreen.kt:421` 已废弃的 `resolveActivity` 的可见性问题.
- **Q-02** `keys/debug.keystore` 被提交 (PA-64) 与 [12-git](../../agents-cn/12-git.md) 的 "绝不提交" 一节如何调和?  
  是为了 Firebase 的调试证书指纹而有意保留, 还是移出仓库?
- **Q-03** `gwpAsanMode="always"` 与 `nativeSymbolUploadEnabled` (PA-70) 是只在 Firebase 包要,  
  还是两种包都要?
- **Q-04** `ShellDefault` (PA-124) 按 [05-代码规则](../../agents-cn/05-代码规则.md) 是保留的预备实现, 但它带一个已知的读流死锁:  
  继续保留 (并在文档里记录启用前的必做修复), 还是删除?
- **Q-05** 分区指纹清单 (PA-43) 硬编码 10 项, 其余反射 (PA-46) 同理: [05-代码规则](../../agents-cn/05-代码规则.md) 那条  
  "新代码用最新可用 API" 只管新写的代码, 既有代码是否回头改要你的决定.
- **Q-06** lld.json 缓存放外部存储 (PA-59) 是刻意选择吗 (例如便于用户查看)?  
  若只是历史选择, 内部存储是更安全的一侧.
- **Q-07** PA-24 属于 "描述与代码不符" 一类: [03-概览](../../agents-cn/03-概览.md) 架构一节的四级画法是对现有代码的陈述,  
  而代码里仓库直接产出界面行模型. 要你判定改哪一边 (把那句描述改成实际层数, 还是补出领域层).
- **Q-08** 归档的四份分析标的是 record 或 scratch, 可执行的发现要不要转进  
  `docs/dev/issues-cn/` 的编号条目, 本文件只做清单?
- 另外第 10 节的分歧表里还有一条要你定: 空 `onClick` 的 Card (PA-74) 算不算缺陷.

## 12. 出处索引

每个来源报告的全部编号都映射到本文件的编号; 同一编号出现在多处时就都列出.  
归档报告的原文在 `archived/`, 引用旧编号时用它归档时的文件名.

**deepseek** (`archived/2026-10-16-project-analysis-deepseek-v4.1-flash.md`):  
PA-01 → PA-04, PA-02 → PA-01, PA-03 → PA-03, PA-04 → PA-09, PA-05 → PA-08, PA-06 → PA-06,  
PA-07 → PA-10, PA-08 → PA-04, PA-09 → PA-55, PA-10 → PA-14, PA-11 → PA-18, PA-12 → PA-19,  
PA-13 → PA-16, PA-14 → PA-25, PA-15 → PA-02, PA-16 → PA-22, PA-17 → PA-27, PA-18 → PA-54,  
PA-19 → PA-35, PA-20 → PA-36, PA-21 → PA-124, PA-22 → PA-65, PA-23 → PA-46, PA-68,  
PA-24 → PA-60, PA-25 → PA-59, PA-63, PA-26 → PA-70, PA-27 → PA-89, PA-28 → PA-82, PA-29 → PA-73,  
PA-30 → PA-75, PA-31 → PA-77, PA-84, PA-32 → PA-88, PA-33 → PA-85, PA-86, PA-34 → PA-95,  
PA-35 → PA-92, PA-36 → PA-122, PA-37 → PA-69, PA-38 → PA-123, PA-39 → PA-98, PA-40 → PA-103,  
PA-41 → PA-104, PA-42 → PA-102, PA-43 → PA-107, PA-44 → PA-120, PA-45 → PA-106, PA-46 → PA-119,  
PA-47 → PA-89, PA-121.

**glm** (`archived/2026-10-16-project-analysis-glm-5.3-flash.md`):  
SS1 → PA-01, SS2 → PA-02, SS3 → PA-02, SS4 → PA-03, SS5 → PA-04, SS6 → PA-06,  
UD1 → PA-16, UD2 → PA-17, UD3 → PA-18, UD4 → PA-19, UD5 → PA-20,  
AR1 → PA-22, AR2 → PA-22, AR3 → PA-29, AR4 → PA-30, AR5 → PA-39, AR6 → PA-31, AR7 → PA-32,  
AR8 → PA-33, AR9 → PA-14, DP1 → PA-24, DP2 → PA-02, DP3 → PA-124,  
MD1 → PA-25, MD2 → PA-48, MD3 → PA-37, MD4 → PA-36, MD5 → PA-41, MD6 → PA-49, MD7 → PA-38,  
MD8 → PA-51, MD9 → PA-50,  
SE1 → PA-59, SE2 → PA-60, SE3 → PA-63, SE4 → PA-64, SE5 → PA-62, SE6 → PA-60, SE7 → PA-65,  
SE8 → PA-66,  
CP1 → PA-74, CP2 → PA-79, CP3 → PA-77, CP4 → PA-78, CP5 → PA-73, CP6 → PA-54,  
AP1 → PA-87, AP2 → PA-91, AP3 → PA-92, AP4 → PA-90, AP5 → PA-86, PA-93, AP6 → PA-94,  
AP7 → PA-88, AP8 → PA-96, AP9 → PA-97,  
TE1 → PA-98, TE2 → PA-99, TE3 → PA-106, TE4 → PA-102.

**mimo** (`archived/2026-10-16-project-analysis-mimo-v2.6-flash.md`):  
SSOT-1 → PA-01, SSOT-2 → PA-02, SSOT-3 → PA-15, SSOT-4 → PA-03, SSOT-5 → PA-13, SSOT-6 → PA-12,  
UDF-1 → PA-15, UDF-2 → PA-17, UDF-3 → PA-18, UDF-4 → PA-19,  
ARCH-1 → PA-29, ARCH-2 → PA-24, ARCH-3 → PA-23, ARCH-4 → PA-02, ARCH-5 → PA-28, ARCH-6 → PA-26,  
PATTERN-1 → PA-14, PATTERN-2 → PA-55, PATTERN-3 → PA-56, PATTERN-4 → PA-20, PA-31,  
PATTERN-5 → PA-32, PATTERN-6 → PA-04, PA-45, PATTERN-7 → PA-34, PATTERN-8 → PA-57,  
PATTERN-9 → PA-58, PATTERN-10 → PA-124, PATTERN-11 → PA-34,  
MOD-1 → PA-22, PA-36, MOD-2 → PA-22, MOD-3 → PA-41, MOD-4 → PA-04, MOD-5 → PA-08,  
MOD-6 → PA-25, PA-27, MOD-7 → PA-39, MOD-8 → PA-44, MOD-9 → PA-38,  
SEC-1 → PA-60, SEC-2 → PA-59, SEC-3 → PA-61, SEC-4 → PA-09, SEC-5 → PA-72, SEC-6 → PA-69,  
SEC-7 → PA-69, SEC-8 → PA-63, SEC-9 → PA-64, SEC-10 → PA-53, PA-71, SEC-11 → PA-70, SEC-12 → PA-65,  
CP-1 → PA-73, CP-3 → PA-74, CP-4 → PA-75, CP-5 → PA-76,  
AP-1 → PA-85, AP-2 → PA-86, PA-93, AP-3 → PA-87, AP-4 → PA-88, AP-5 → PA-90, AP-6 → PA-91,  
AP-7 → PA-92,  
TEST-1 → PA-98, PA-106, TEST-2 → PA-102, TEST-3 → PA-119, TEST-4 → PA-107.

**qwen** (`archived/2026-10-16-project-analysis-qwen-3.8-flash.md`):  
SSOT-01 → PA-01, SSOT-02 → PA-06, SSOT-03 → PA-07, SSOT-04 → PA-10, SSOT-05 → PA-11,  
SSOT-06 → PA-09,  
UDF-01 → PA-14, UDF-02 → PA-15, UDF-03 → PA-16, UDF-04 → PA-15, UDF-05 → PA-21,  
UDF-06 → PA-20, PA-86, UDF-07 → PA-19,  
ARC-01 → PA-24, ARC-02 → PA-22, PA-49, ARC-03 → PA-02, ARC-04 → PA-28,  
ARC-05 → PA-25, PA-26, ARC-06 → PA-46, PA-68, ARC-07 → PA-34,  
ARC-08 → PA-47, PA-124,  
PAT-01 → PA-02, PAT-02 → PA-40, PAT-03 → PA-24, PAT-04 → PA-29, PA-86, PAT-05 → PA-04,  
PAT-06 → PA-42,  
MOD-01 → PA-35, MOD-02 → PA-23, MOD-03 → PA-38, MOD-04 → PA-41, PA-43, MOD-05 → PA-44,  
MOD-06 → PA-52, MOD-07 → PA-51, PA-53,  
SEC-01 → PA-65, SEC-02 → PA-71, SEC-03 → PA-59, PA-60, SEC-04 → PA-67, SEC-05 → PA-72,  
SEC-06 → PA-69, SEC-07 → PA-70, SEC-08 → PA-63, SEC-09 → PA-64,  
CP-01 → PA-77, CP-02 → PA-73, CP-03 → PA-75, CP-04 → PA-74, CP-05 → PA-81, PA-85,  
CP-06 → PA-80, CP-07 → PA-83, CP-08 → PA-84,  
AP-01 → PA-85, PA-86, AP-02 → PA-91, PA-92, PA-93, AP-03 → PA-90, PA-97, AP-04 → PA-87,  
AP-05 → PA-88, AP-06 → PA-89, AP-07 → PA-70, AP-08 → PA-75, AP-09 → PA-05,  
TST-01 → PA-98, TST-02 → PA-99, TST-03 → PA-100, TST-04 → PA-102, TST-05 → PA-101,  
TST-06 → PA-105, TST-07 → PA-104, PA-125, TST-08 → PA-117, TST-09 → PA-107, TST-10 → PA-118,  
TST-11 → PA-108, TST-12 → PA-109, TST-13 → PA-110, TST-14 → PA-111, TST-15 → PA-112,  
TST-16 → PA-113, TST-17 → PA-114, TST-18 → PA-115, TST-19 → PA-116, TST-20 → PA-109,  
TST-21 → PA-106.

**优先级报告** (`archived/2026-10-17-priority-report.md`): 它的分组, 修法, 与 issues-cn 的分歧表  
与判定清单分别收进本文件的第 9, 10, 11 节; 它自己没有条目编号.

## 后续

本文件是问题清单, 不含修复方案与排期; 若要把其中某条转成改动, 按 [02-工作原则](../../agents-cn/02-工作原则.md) 的流程  
先出一份短报告再动手. 归档的四份分析与优先级报告只作历史记录, 引用条目时以本文件的编号为准;  
它们写下的行号是当时读到的, 本文件的行号已按 2026-10-18 的当前代码复核过.
