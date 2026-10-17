# 项目问题分析 (2026-10-16)

> **状态**: **living** (未归档, 可原地修订).  
> **依据**: 只以当前代码为证据, 每条给出文件与行号; 代码注释与 `docs/dev/issues-cn/` 未作为证据使用.  
> **范围**: SSOT, UDF, 架构, 设计模式, 模块化, Security, Compose 性能, App 性能.  
> 本次不考虑: CI, Test, NDK, JNI.  
> **严重度**是影响面的估计, 用于排优先级, 不是修复深度的上限: 定为"低"的问题, 要彻底解决也照彻底解决.  
> **陈述边界**: 条目里叙述平台或库行为 (非本仓库代码所能证实) 之处, 按对应平台或库文档判断;
> 只在该边界会改变条目结论时, 才在条目内逐句标注.  
> 条目编号 `PA-nn` 全仓库唯一, 后续讨论请直接引用编号.

## 结论

共 38 条. 最该先处理的是这 5 条:

- **PA-32** (高): 启动把 SharedPreferences 文件解析, 反射遍历, libsu 默认 builder 配置都压在主线程.  
- **PA-27** (高): 项目没有任何 Baseline Profile.  
- **PA-02** (高): `myAndroid` 是进程级可变全局, 由 Home 的加载写入, 其它页面的 API 判定因此依赖"Home 是否已加载";
  该双写入者设计是 AGENTS.md 已记录的约定, 是否动它由负责人决定.  
- **PA-10** (中): `BaseListViewModel` 没有失败分支, 而"不抛异常"的契约至少在两处 shell 调用上没有兜底.  
- **PA-35** (中): 最贵的那行每次加载都全量枚举已安装应用, 同一刷新里还可能算两遍.  

## 1. SSOT

### PA-01 (低) 占位值判定依赖两处代码解析到同一资源

`PropertyReader.getString` 在读取失败或条件不满足时返回本地化占位串 (`ui/common/PropertyReader.kt:24-37`),
`HomeRepository.isPropertyValueNotEmpty()` 再把这个值跟 `MyApplication.getMyString(R.string.result_not_supported)` /
`R.string.build_not_filled` 比较 (`ui/home/repository/HomeRepository.kt:1181-1184`);
同一写法还在分区指纹 (`ui/others/repository/OthersRepository.kt:219-221`) 与 `detectVndk` (`HomeRepository.kt:687,691`).  
两侧当前都经同一个 `MyApplication` 实例的 `getString` 解析, 所以各语言下比较都能命中, **行为不随系统语言变化**.  
脆点在于这是一条隐式约定: 两处独立代码必须解析到同一个资源串. 任一侧改成别的 Context 或 locale,
或把占位串写成字面量, 判定就会静默失效 (把"没有值"当成正常值, 例如缺失的 vendor 安全补丁被算作正常).  
**方向**: 让占位符不参与判定, 改成比较基准不到的哨兵值, 或让读取端返回 null 而不是本地化文案.

### PA-02 (高) `myAndroid` 全局可变, 跨特性顺序依赖

`myAndroid` 定义在 `ui/common/AndroidVersionExt.kt:62-64`, 有两个写入者: `initMyAndroid()` (`:66-110`)
与 `HomeRepository.detectAndroid()` (`HomeRepository.kt:124-131`).  
所有 API 判定通过 `isAtLeastAndroidN()` 读它 (`AndroidVersionExt.kt:114-125`), 而读取方遍布
`OthersViewModel` (`:44-50,78-80`), `HomeRepository`, `AppGraph` (`:125`) 等处.

先打开 Others 或 Prop 再回 Home, 先建好的列表用的是 Home 改写前的判定结果; 同一进程内两个页面可以对同一台设备给出不同结论.  
**定性**: "恰好两个写入者"是 AGENTS.md 已记录的约定, 所以这算不算缺陷由负责人裁定, 本文不替其决定.
若要消除顺序依赖而又保留该约定, 需要让判定不读被覆盖的字段 (例如判定统一用 `Build.VERSION.SDK_INT`, 覆盖只用于展示).

### PA-03 (中) 每个设置有两份表示, 靠回调手工同步

`SettingsStore` 同时暴露原始字符串 Flow (`themeValue`, `scrollBarValue`) 与枚举 Flow (`themeMode`, `scrollBarMode`)
(`ui/settings/repository/SettingsStore.kt:50-64`), 由同一个监听回调一起更新 (`:99-113`).  
存储值无法识别时 `themeMode` 回退 `FollowSystem` (`:79-83`), 而 `themeValue` 保留原值 (`:100`),
于是设置对话框显示"无选中项"而界面按 follow system 运行. 迁移路径 (`:116-143`) 是第三个必须同时维护两处的地方.  
**方向**: 只保留枚举 Flow, 由枚举反向生成对话框需要的字符串.

### PA-04 (中) `lld.json` 的 schema 版本从不校验

`SCHEME_VERSION` (`ui/home/model/Lld.kt:8`) 在全仓库零引用, `Lld.scheme` (`:13`) 只被反序列化, 没有任何比较.  
解析器配置为 `Json { ignoreUnknownKeys = true }` (`ui/common/JsonExt.kt:6`), 所以上游字段新增会被静默忽略, 字段改名会直接抛异常.  
**方向**: 解析后校验 `scheme == SCHEME_VERSION`, 不符走明确的回退与提示.

### PA-05 (中) API 判定有两套写法

`Theme.kt:322` 直接写 `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S`, 而项目其它地方用 `isAtLeastAndroid12()`
(`AndroidVersionExt.kt:121`, 例: `HomeRepository.kt:318`, `ui/others/OthersViewModel.kt:44`).  
两套写法的输入不同 (`isAtLeast*` 还叠加了 `myAndroid.api`, 见 PA-02), 因此动态取色开关和别的 API 12 判定可能不一致.  
**方向**: 动态取色也走 `isAtLeastAndroid12()`.

### PA-06 (低) 版本比较有两套机制

项目已依赖 `io.github.g00fy2.versioncompare.Version` 用于玩具箱/内核/WebView 比较
(`HomeRepository.kt:345,909,961,1016`), 但同一文件里还有裸字符串比较: `mySecurityPatch >= lldSecurityPatch` (`:292-293`),
`versionName >= latestGooglePlaySystemUpdates` (`:658-660`), `vndkVersionResult >= lld.android.stable.api` (`:697`),
`savedLldVersion < assetLldVersion` (`ui/home/datasource/LldFileStore.kt:62`), 以及 `isLatestPreviewAndroid` 比 `apiFull` 字符串
(`AndroidVersionExt.kt:133`). 当前数据形状 (两位数 API, `yyyy-MM-dd`, 定宽时间戳) 下结果正确, 形状一变就错.  

### PA-07 (低) `_key` 后缀承担两种含义

`interface_themes_key` / `interface_scroll_bar_key` 是真正的 SharedPreferences 键 (`ui/settings/res/values/strings.xml:6,16`),  
而 `interface_no_scroll_bar_key` / `interface_themes_follow_system_key` 等是显示标签 ("None", "Follow system") (`:9,11,19,22,24`).  
同一个后缀, 两种语义, 而两者都同时出现在 `SettingsStore.kt:33-45` 和 `arrays.xml:4-27` 里.  
另: 同一批常量有两条解析路径, store 直接读资源, 对话框经 `LocalResources` (`SettingsScreen.kt:315-319`).  

### PA-08 (低) "没有值"有 3 个词

`result_not_supported` (`PropertyReader.kt:25,32`), `build_not_filled` (`PropertyReader.kt:28`, `MyModelExt.kt:164`),
`android.R.string.unknownName` (`HomeRepository.kt:86` 等) 三者在页面上表达同一件事, 取哪个取决于取值路径而不是语义.  

## 2. UDF

### PA-09 (中) 回退策略与错误文案写在 ViewModel 里

在线/离线文件/内置 asset 的三级回退, 以及 3 个错误文案资源 (`lld_json_fetch_failed`, `lld_json_parse_failed`,
`lld_json_save_failed`), 全部由 `HomeViewModel` 决定
(`ui/home/HomeViewModel.kt:101-204,246-251`), 其中 5 处 `catch (e: Exception)` 包着 `OrThrow` 结尾的接口
(`:106-111,117-122,141-145,161-169,177-185`).  
AGENTS 的规则是 UI 层 (Compose / ViewModel) 不接业务异常; 同一文件还直接读 `MyApplication.instance.assets` (`:194`).
这是规则与代码的分歧, 需要定性: 要么把回退与错误映射下沉到 repository, 要么改规则.  

### PA-10 (中) "不抛异常"的契约没有兜底, 且有漏洞

`BaseListViewModel.startLoad` (`ui/base/list/BaseListViewModel.kt:60-71`) 没有失败分支: `collectModels()` 一旦抛出,
`isLoadingStateFlow` 永远停在 true, 异常冲出 `viewModelScope`.  
该契约靠调用点逐个套 guard 实现, 而至少两处 shell 调用在 guard 之外:
`HomeRepository.getMounts()` (`HomeRepository.kt:415`, 调用于 `HomeViewModel.kt:221`) 与
`PropRepository.getBuildProp()` (`ui/prop/repository/PropRepository.kt:70`); `IShell` (`base/.../shell/IShell.kt:3-5`)
没有声明"不抛", 实现 `ShellLibSu` (`ui/common/ShellLibSu.kt:17-20`) 直接转发 libsu.  
**方向**: 先明确 `IShell.execute` 是否可能抛; 若可能, 这两个调用点需要 guard 或停转 spinner 的失败分支.  

### PA-11 (低) 同一个设置在同一个屏幕里有两条读取路径

`SettingsScreen` 既从 ViewModel 的 flow 读 (`SettingsScreen.kt:83-86`), 又从 `LocalScrollBarMode` 读 (`:96`),
而两者来自同一个 store. 根节点也提供同一份 flow (`MainActivity.kt:78`).  

### PA-12 (低) 点击回调里带隐藏状态

`SettingsViewModel.getVersionClickedMessage()` 名字是取消息, 实际会自减 `timesLeft` (`SettingsViewModel.kt:105-120`),
由点击 lambda 调用 (`SettingsScreen.kt:117-121`). 这个计数不是 Flow, 不进 `SavedStateHandle`, 进程死亡后静默重置为 7.  

### PA-13 (低) 行内更新按下标 + 类型定位

`HomeViewModel.payloadOutdatedTargetSdkVersionApk()` 用 `indexOfFirst { it.type == OutdatedTargetSdkApk }` 找行
(`HomeViewModel.kt:278-280`), 再按下标替换 (`BaseListViewModel.kt:79-87`). 该类型当前只有一个实例; 出现第二个时,
更新会落到第一行, 且不会报错.  

## 3. 架构 / 设计模式

### PA-14 (中) `...base` 包被两个模块同时占用

`:app` 有 `net.imknown.android.forefrontinfo.base` 包 (`app/.../base/MyApplication.kt:1`),
`:base` 模块的 namespace 同值 (`base/build.gradle.kts:6`, `base/.../base/property/IProperty.kt:1`).  
读代码时无法从包名判断 `base.*` 来自哪个模块; 类路径下合法, 但命名上不成立.  

### PA-15 (中) `MyApplication` 兼作 service locator

`instance` (`lateinit`, `app/.../base/MyApplication.kt:74`), companion 的 `sharedPreferences` 与 `getMyString` (`:76-85`)
被 repository, data source 和 `HomeViewModel` (`HomeViewModel.kt:194`) 直接调用.  
`MyApplication.getMyString` 在 `:app` 里 99 处调用 (其中 `HomeRepository.kt` 69 处, `SettingsStore.kt:33-45` 11 处),
`MyApplication.instance` 14 处, 用于取 `assets` / `packageManager` / `contentResolver` / `Context`
(`PropRepository.kt:54`, `HomeRepository.kt:755,770,946,994,1205`).  
这些都绕过 DI 图, 也是仓库层无法脱离 Android 运行时做单测的直接原因; interface-first 的可测性只覆盖了 `IProperty` / `IShell` / `SettingsStore`.  
**方向**: 字符串与 `Context` 走注入 (例如注入资源包装或已有的 `appInfoDataSource` 那种形式), 不再从全局取.  

### PA-16 (中) 特性包互相跨引用, 没有 `internal` 边界

- `HomeRepository` 引 `ui.settings.datasource.AppInfoDataSource` (`HomeRepository.kt:49`),
  `SettingsRepository` 引 `ui.home.datasource.LldFileStore` (`ui/settings/repository/SettingsRepository.kt:14`):
  home 与 settings 互相依赖.
- `BasicDataSource` 引 `ui.home.datasource.AndroidDataSource` (`ui/others/datasource/BasicDataSource.kt:7,25`):
  `ui.home.datasource` 事实上是共享常量层.
- `AndroidDataSource` 只有常量没有行为 (`ui/home/datasource/AndroidDataSource.kt`), 消费方却属于其它特性.

`internal` 在 `:app` 里只用于 7 个符号 (例如 `MyModelListContent`, `previewModels`, `guardedItemCore`,
`toErrorMyModel`, `toDetectFailedMyModel`, `ExtendedLightColors`, `ExtendedDarkColors`),
所以"每个特性一个包 + 同构分层"只是命名约定, 不是编译期边界.  

### PA-17 (低) 设置领域枚举放在 Application 文件里

`AppThemeMode` 与 `ScrollBarMode` 定义在 `MyApplication.kt:18-47`, 被 `ui.theme` (`Theme.kt:293-309`),
`ui.settings` (`SettingsStore.kt:79-91`) 和 `ui.base.list` (`MyModelListScreen.kt:86`) 消费.  

### PA-18 (低) 统一行模型装不下更复杂的行

`MyModel(title, detail, color, type)` (`ui/base/list/MyModel.kt:18-28`) 是唯一行模型, 所以整份
"targetSdk 过低的系统应用"列表被拼进一个 `detail` 字符串 (`HomeRepository.kt:1163-1167`).  
行同时被 `LazyColumn` 当作 key, 而 key 取自显示数据 (资源 id 的字符串形式或原始标题, `MyModel.kt:24-28`).  
可证的是 key 的来源: 重复标题会产出重复 key; "重复 key 会崩溃"是 LazyColumn 的库行为, 仓库内不可证, 按库文档判断.
当前靠"数据恰好唯一"维持.  

## 4. 模块化

### PA-19 (中) `res` 目录靠手工登记

每个特性的 `res` 目录要写进 `app/build.gradle.kts:24-44` 的 `sourceSets`. 新增包时漏登记:
被代码 `R.x` 引用的资源会以编译错误暴露, 没有任何代码引用的资源则静默缺失.  
资源目录本身也放在 `java/<package>/<feature>/res` 下, 与代码同层.  

### PA-20 (低) 共享 UI 套件的位置与"全 app 契约"的位置

`:base` 只放接口与扩展 (`IProperty`, `IShell`, `ShellResult`, 两个扩展文件); 共享 UI 套件
(`MyModel`, `BaseListViewModel`, `MyModelCard`, guard 助手) 留在 `:app` 的 `ui.base.list`.  
其中 `LocalScrollBarMode` 定义在列表特性里 (`MyModelListScreen.kt:39`), 却由根节点提供 (`MainActivity.kt:78`),
并被 `ui.settings` 消费 (`SettingsScreen.kt:62`): 一个特性符号被当成全局契约.  

### PA-21 (低) 保留实现带已知死锁

`ShellDefault` 没有被调用者, 是有意保留的原生实现, 但启用前必须先修 `waitFor()` 之后的管道读取死锁 (见 AGENTS.md 的 Code rules).
它不是缺陷, 是雷区: 只要有人"顺手"启用它就会挂.  

## 5. Security

### PA-22 (中) `QUERY_ALL_PACKAGES` 且清单里关掉了 lint

`app/src/main/AndroidManifest.xml:6-8` 同时声明权限与 `tools:ignore="PackageVisibilityPolicy, QueryAllPackagesPermission"`,
消费方是 `getInstalledApplications` (`HomeRepository.kt:1121-1131`).  
代码只能证实权限与 `tools:ignore` 的存在; Play 侧是否需要并维持豁免说明属于仓库外事实, 本文不判定.  
**方向**: 读取范围可收窄 (只读 flags / `targetSdkVersion` / `packageName`); 申报状态请按 Play 后台的实际记录确认.  

### PA-23 (中) 隐藏 API 反射, 失败即静默

- `PropertyDefault` 反射 `android.os.SystemProperties` (`base/.../property/impl/PropertyDefault.kt:8-18`, `@SuppressLint("PrivateApi")`).
- `HomeRepository.getBuildInWebViewProvidersAndroid7()` 反射 `WebViewUpdateService` / `WebViewProviderInfo`
  (`HomeRepository.kt:1045-1111`, `@SuppressLint("PrivateApi")`).

非 SDK 接口限制下两者都可能失效, 失效时只走 `Log.w` / 空列表 (`getBuildInWebViewProvidersAndroid7` 的 catch 分支),
表现为 WebView 内置版本显示 `build_not_filled`. `@SuppressLint` 同时也去掉了编译期提示.  
可证的是"反射加失败降级路径"存在; "限制会不会真的拦住它们"属平台策略, 仓库内不可证, 按平台文档判断.  
**方向**: 为这两处各留一条可见的退化标记, 或在可行时改用 `Build` / `getprop` 取值.  

### PA-24 (中) 下载数据只靠 HTTPS, schema 不校验

`LldDataSource.fetchOnlineLldJsonStringOrThrow` (`ui/home/datasource/LldDataSource.kt:34-48`) 走 HTTPS 并带
`Referer: <applicationId>`, 内容落盘到外部 files 目录 (`LldFileStore.kt:101-109`), 解析用 `ignoreUnknownKeys = true`.  
唯一的契约检查是 `version` 的日期时间格式 (`HomeViewModel.kt:128-134`);
声明式 schema 版本不校验 (见 PA-04). 数据本身决定 UI 判定结果, 所以这是"数据可信度"链条的薄弱点.  

### PA-25 (低) 备份规则为空

`base/resBackup/xml/backup_rules.xml` 与 `data_extraction_rules.xml` 都是空元素 (可证); 按平台备份语义, 空规则等于默认全量备份,
SP 里的设置与 `files/` 下内容都会进云端备份 ("空规则到全量备份"属平台语义, 仓库内不可证).  
`MyApplication.getFileDir` 在外部目录不可用时回退到 `filesDir` (`MyApplication.kt:62-71`), 所以拉取来的 `lld.json` 也可能落到备份范围.
数据敏感度低, 但这是默认选择而不是显式选择.  

### PA-26 (低) 收集组件与 release 上的内存检测

Firebase 版带 Analytics + Crashlytics (`build-logic/.../AndroidApplicationFirebaseConventionPlugin.kt:23-32`, `gradle/toml/google.toml`),
foss 版通过任务禁用排除 (`:46-71`). 在一个以暴露设备信息为主题的 app 里, Analytics 值得在隐私评审里单独过一遍.  
另外 `android:gwpAsanMode="always"` (`AndroidManifest.xml:15`) 对 release 也生效: 确认这是有意的.  

**本次核对后未发现问题**: 无明文流量; 无动态代码加载; 无 WebView / JS 桥; 唯一导出的 Activity 不读 Intent;
全仓库 shell 命令都是常量 (唯一把设备值拼进路径的是 `PATH_VENDOR_VINTF_SKU.format(<prop 值>)`, `HomeRepository.kt:523-531`,
且只用于 `File.exists()`).  

## 6. Compose 性能

### PA-27 (高) 没有 Baseline Profile

`settings.gradle.kts:54-56` 只包含 `:binderDetector`, `:base`, `:app`; 依赖中找不到 `androidx.profileinstaller`,
也没有 `BaselineProfileRule` 或生成模块. 对一个启动期就要做反射 / shell / 包枚举的应用, 这是收益最大的单点.  

### PA-28 (中) `AppRoot` 每次重组都为 4 个 tab 重建导航参数

`AppRoot` 在 `topLevelTabs.map { ... }` 里每次重组重新构造 `entryDecorators` 列表 (`AppRoot.kt:97-100`)
和 `entryProvider { ... }` (`:109-114`); 切 tab 会触发 `AppRoot` 自身重组 (读点 `:88,119,123`).  
可证实的部分到此为止: 两个参数每次重组都是新对象, 每次重组都产生这些分配.  
它们是否因此让 `rememberDecoratedNavEntries` 的缓存失效, 取决于 Navigation 3 库内部的 remember 键选取,
仓库内代码无法证实, 因此"四个栈被重新装饰"不作为结论.  
**方向**: 把这两个参数提到 `remember` 里 (它们只依赖常量与 ViewModel 工厂), 这样无论库内部怎么取键都不会被换掉.  

### PA-29 (中) 滚动状态在组合期被读取, 且挂在页面级 LazyColumn 上

`ScrollBarExt.nonInteractiveScrollbar` 在组合期计算 `val scrolling = enabled && scrollState.isScrollInProgress`
(`ui/common/ScrollBarExt.kt:41`), 由此驱动 `animateFloatAsState` 的目标值.  
调用点是页面级 LazyColumn 的 modifier 链 (`MyModelListScreen.kt:118`, `SettingsScreen.kt:160`),
所以每次滚动开始/结束都会重组该页面并重跑 `items` lambda.  
alpha 本身已经正确下沉到 `drawWithContent` (`ScrollBarExt.kt:56-58`), 这部分是好的.  
**方向**: 把"是否在滚动"的读取下沉到 draw / graphicsLayer, 或用不需要组合期读取的指示器封装.  

### PA-30 (中) 超长内容放在单个 Text 里并做高度动画

"targetSdk 过低的应用"整份列表是一个 `detail` 字符串 (`HomeRepository.kt:1163-1167`),
渲染为单个 `Text` (`MyModelCard.kt:88`), 外层 Card 还带 `animateContentSize()` (`:55`).  
刷新或切换排序时, 这个极高的行会被整体重排并做高度动画, 而不是虚拟化.  
**方向**: 该行改成可折叠的子列表 (或独立 item), 让 LazyColumn 真正虚拟化.  

### PA-31 (低) 主题构建与遗留 lint 条目

`AppTheme` 在 composable 体内直接构造 ColorScheme, 无 `remember` (`ui/theme/Theme.kt:321-329`);
`AppRoot()` 没有 `modifier` 参数; lint baseline 里留了 8 条, 其中 3 条 `ComposeCompositionLocalUsage`,
1 条 `ComposeModifierMissing`, 1 条 `ComposeReadOnlyComposable` (`app/lint-baseline.xml`).  
ColorScheme 重建只在主题或系统深浅色变化时发生, 影响有界.  

**本次核对后未发现问题**: 没有 `collectAsState()`, `derivedStateOf`, `LaunchedEffect(Unit)` 的误用;
列表状态一律走 `collectAsStateWithLifecycle`; 对话框的资源数组只在打开时读取 (`SettingsScreen.kt:244-245,259-260`).  

## 7. App 性能

### PA-32 (高) 启动关键路径全在主线程

`MyApplication.onCreate` 依次做: 建图 (`MyApplication.kt:58`), 触发 `appGraph.settingsStore` (`:103`) 从而
在 `SettingsStore` 构造里做 4 次 `prefs` 读取 + 1 次条件写 (`SettingsStore.kt:119,126,132,136,137`;
首次读取会同步解析 XML 文件), `initMyAndroid()` 反射遍历并排序 `Build.VERSION_CODES` 字段
(`AndroidVersionExt.kt:22-44,66-110`), 以及 `initShell()` 配置 libsu 的默认 builder (`MyApplication.kt:108-116`,
只设置 builder, 未起 shell 进程).  
**方向**: 把 SP 读取与反射挪出首帧路径 (延后到首次真正需要时), 或至少让 theme/scrollbar 的首帧值来自更轻的来源.  

### PA-33 (中) 检测完全串行

Home 的 24 个调用在同一个 `withContext(Dispatchers.IO)` 里顺序执行 (`HomeViewModel.kt:206-244`),
Others 的约 36 个同理 (`OthersViewModel.kt:31-101`). 每个调用都可能做反射, shell 执行或 PackageManager 查询,
所以一次冷启动 Home 的耗时是它们之和; 其中 `getMounts()` 还在 IO 里再嵌套一次 `withContext(IO)` (`HomeViewModel.kt:221-223`).  
**方向**: 去掉嵌套调度; 对彼此独立的检测用并发 (共享一个限流) 而不是串行.  

### PA-34 (中) 为一行信息初始化 WebView provider

`OthersRepository.getDefaultUserAgent` (`OthersRepository.kt:242-251`) 调用
`RomDataSource.getDefaultUserAgentOrThrow` (`ui/others/datasource/RomDataSource.kt:26`), 即 `WebSettings.getDefaultUserAgent`.  
调用链可证; "会加载 WebView 包并拉起其进程"是平台行为, 仓库内不可证, 需按平台文档判断.  
若它成立, 代价与一行文本不成比例.  
**方向**: 该行改为按需展开, 或标为"触发时会初始化 WebView".  

### PA-35 (中) 最贵的行每次加载都重算, 且同一刷新可能算两次

`getOutdatedTargetSdkVersionApkModel` (`HomeRepository.kt:1117-1177`) 每次 Home 加载都全量枚举已安装应用并排序 (`:1121-1131,1151-1161`);
切换排序开关时, `payloadOutdatedTargetSdkVersionApk()` 还要重读并解析整份 `lld.json` 只为取一行 detail (`HomeViewModel.kt:268-275`).  
若加载进行中开关被切, 同一刷新里会枚举两遍: `detect()` (`:238-240`) 与 `onModelsLoaded` 的补丁各一次 (`:253-264`).  
**方向**: 枚举结果按 load 缓存一次; 补丁路径复用本次加载已解析的 `Lld` 而不是重读文件.  

## 8. 其它 (代码卫生, 不在本次角度内但影响可维护性)

### PA-36 (中) catch 让类型退化, 校验失效

`OthersRepository.getBinderStatus` (`OthersRepository.kt:85-89`):

```kotlin
val binderVersion = try {
    architectureDataSource.getBinderVersionOrThrow(driver)
} catch (e: UnsatisfiedLinkError) {
    e.printStackTrace()
}
```

catch 分支的值是 `Unit`, 所以 `binderVersion` 被推断为 `Any`, 之后 `when (binderVersion) { -ERRNO_... -> ... }`
虽然能编译, 但已经失去类型约束 (把返回值写成任何类型都不会报错).  
本文件的 `printStackTrace()` 无 `BuildConfig.DEBUG` 门控的有 4 处 (`:88,:119,:133,:247`).  
**方向**: catch 分支返回明确的兜底值 (例如 `null` 或哨兵整数).  

### PA-37 (低) 日志风格与门控不一致

全仓库 14 处 `printStackTrace()`, 门控不一致: `OthersRepository.kt:88,119,133,247` 与 `PropertiesDataSource.kt:23`,
`LldFileStore.kt:118`, `AppGraph.kt:153,158` 无门控; `MyModelExt.kt:158`, `HomeViewModel.kt:200,248`,
`PropRepository.kt:45,59`, `OthersRepository.kt:231` 有门控. 同时 `Log.w` 与 `printStackTrace` 两套风格混用.  

### PA-38 (低) 小规模效率与副作用问题

- `PropRepository.getBuildProp` 在循环里用 `+=` 累加字符串 (`PropRepository.kt:79-93`), `getprop` 输出上千行时是 O(n^2).
- `SettingsDataSource.getSettingsOrThrow` 在 `filter` 谓词里写 `it.isAccessible = true` (`ui/prop/datasource/SettingsDataSource.kt:13-16`):
  带副作用的判断条件.  

## 后续

本文件是问题清单, 不含修复方案与排期. 若要把其中某条转成改动, 按 AGENTS 的流程先出短报告再动手.  
