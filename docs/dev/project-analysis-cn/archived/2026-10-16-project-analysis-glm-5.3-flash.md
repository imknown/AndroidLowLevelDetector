# 项目问题分析报告 (glm-5.3-flash)

**Status**: **scratch**, 不作为规则的来源, 只是一次性的问题快照分析.  
生成日期: 2026-10-16, 生成者: GLM-5.3-Flash Free (OpenCode 会话).

## 范围与方法

- 分析对象仅限当前代码: Kotlin 源文件, AndroidManifest, Gradle 构建脚本, 备份规则 XML.  
  未使用 `docs/` 下任何文档作为依据, 分析以代码为准; 唯一例外是 CP1, 判定 "该处不是问题"  
时引用了 `MyModelCard.kt:43-48` 一行标注已接受取舍的注释, 其余结论不依赖任何注释,  
全部落在可指认的 `文件:行` 上.
- 按约定排除的角度: CI, NDK, JNI. 涵盖的角度: SSOT, UDF, 架构, 设计模式, 模块化,  
  Security, Compose 性能, App 性能, Test. Test 一节为 2026-10-18 后补, 外部参考资料见这一节开头.
- 方式: 五路并行审查 (架构/SSOT/UDF, 模块化, 安全, Compose 性能, App 性能) 加撰写者抽查.  
  抽查核对过的条目在文末 "核对说明" 列出; 其余条目的 `文件:行` 来自审查的直接读取.
- 本报告不改任何代码. 条目编号全文件唯一, 供其他文档引用.

## 总览

| ID | 级别 | 一句话 |
|---|---|---|
| SS1 | 高 | 全局可变 `myAndroid` 有两个写入路径, 跨线程读写无同步 |
| SS2 | 高 | `MyApplication` companion 是与 DI 图并行的静态 service locator |
| AR1 | 高 | `ui.home` 与 `ui.settings` 数据源互相引用, feature 之间成环 |
| AR2 | 高 | `ui.common` 反向依赖 `ui.home.model` |
| AR9 | 高 | `BaseListViewModel` 无失败边界与超时, `collectModels()` 卡住则 spinner 常转, 抛异常则进程崩溃 |
| MD1 | 高 | `:app` 中的 `MyApplication` 使用 `:base` 的包名, 两模块共用一个包 |
| SE1 | 高 | lld.json 保存于外部目录, 内容零校验即全信, 部分场景可被其他 app 覆写 |
| AP1 | 高 | Prop 页对每个 Settings 键做一次 ContentResolver 查询, 数百次串行 binder 往返 |
| TE1 | 高 | 业务逻辑零测试: 唯一真实测试只覆盖 `:base` 的时间格式契约 |
| SS3 | 中 | SharedPreferences 有 DI 绑定与 companion 静态两条读取路径 |
| SS4 | 中 | theme 与 scrollBar 两组设置各由两个 StateFlow 手工配对维护 |
| SS5 | 中 | 本地化文案字符串被当作数据有效性哨兵, 检查逻辑重复实现 |
| UD1 | 中 | `updateModelDetail(targetIndex)` 是公开的按索引写入入口 |
| UD2 | 中 | `allowNetwork` 开关只在加载时读一次, 加载中切换不生效 |
| UD3 | 中 | 同一状态同时经 ViewModel 流与 CompositionLocal 两条通道下发 |
| AR3 | 中 | `HomeRepository` 约 1211 行, 混合数据源动作, 反射与表现层组装 |
| AR4 | 中 | `OthersRepository` 约 36 个方法一比一透传 DataSource |
| AR5 | 中 | `AndroidDataSource` 是未注入的常量类, 被跨 feature 静态引用 |
| AR6 | 中 | ViewModel 直接触达 DataSource 概念 |
| AR7 | 中 | Dispatcher 归属按 feature 各自不同, 且存在两层 `withContext(IO)` 嵌套 |
| AR8 | 中 | 下载主机选择的业务决策位于 DataSource, 时区判断工具位于 `:base` |
| DP1 | 中 | `SettingsRepository` 返回携带 `StringRes` 与位置占位符的表现层模型 |
| DP2 | 中 | `PropertyReader` 与 `LldFileStore` 注入具体 `MyApplication` 类型 |
| DP3 | 中 | `ShellDefault` 未接入 DI, 且保留了 read-after-wait 管道死锁的写法 |
| MD2 | 中 | 领域语义色 `StatusColor` 定义在 `ui.theme`, 被模型与仓库引用 |
| MD3 | 中 | `:binderDetector` 只暴露一个 `external fun` 门面, 加载与语义留在 `:app` |
| MD4 | 中 | 共享代码散在 `:base`, `ui.base.list`, `ui.common` 三处, 归属无规则 |
| MD5 | 中 | 两个同名 `FingerprintDataSource` 语义无关, 同名导入容易选错包 |
| MD6 | 中 | `MyModelExt.kt` 按 feature 分段承载各自专属逻辑 |
| SE2 | 中 | 在线 lld.json 无完整性校验, 且 payload 反写全局状态放大影响 |
| SE3 | 中 | 备份与迁移规则为空默认, lld.json 与偏好一并纳入云备份 |
| SE4 | 中 | debug.keystore 连同密码提交在仓库中, 公开 debug 构建可被同签名覆盖 |
| AP2 | 中 | 一次离线加载完整解析 lld.json 三次, 进程内无缓存 |
| AP3 | 中 | 每次刷新全量枚举 `getInstalledApplications`, 切换排序时重复 |
| AP4 | 中 | 系统属性读取每次调用重新做 `getDeclaredMethod` 反射定位 |
| AP5 | 中 | 每次刷新串行执行 4 次以上 shell 调用 |
| TE2 | 中 | 唯一真实测试不在 `testFossDebugUnitTest` 的执行面内 (该 flavor 任务只有 `:app` 有) |
| TE4 | 中 | 可测性被既有条目阻断 (DP2, SS2, SS3, SS5, SS1, 无超时的加载链) |
| CP1 | 低 | 每行一个空 `onClick` 的可点击 Card, 以 Prop 页的数百行叠加为甚 |
| CP2 | 低 | Settings 页六个流在同一作用域采集, 任一改动整页重算 |
| UD4 | 低 | 一次性事件以 ViewModel 返回值触发屏幕侧效应, 与其余形态不一致 |
| UD5 | 低 | Screen 驱动 ViewModel 初始化并把 PackageManager 下传三层 |
| SE5 | 低 | 离线文件的 version 无格式与时间校验, 仅做字典序比较 |
| SE6 | 低 | 获取 lld.json 未限制重定向, 可跨主机或降级 |
| SE7 | 低 | `QUERY_ALL_PACKAGES` 授权范围宽于实际需要 |
| SE8 | 低 | `PROP_VENDOR_SKU` 未清洗即拼入文件路径 |
| SS6 | 低 | 版本比较在四处用字符串字典序, 已引入的 `Version` 库未复用 |
| MD7 | 低 | `fileTree("libs")` 指向不存在的目录, 构成隐式依赖机制 |
| MD8 | 低 | settings 级插件版本号不在版本目录内 |
| MD9 | 低 | 组合根 `AppGraph` 引用了 `ui.common` 的辅助函数 |
| CP3 | 低 | 动态配色 scheme 每次 `AppTheme` 重算未 `remember` |
| CP4 | 低 | 空列表 `persistentListOf()` 每次重算新建 |
| CP5 | 低 | 滚动条修饰符在每次手势起止时触发重算并重建颜色 |
| CP6 | 低 | Prop 行 LazyColumn key 的唯一性仅靠命名约定保证 |
| AP6 | 低 | WebView 提供商反射链每次刷新重建 |
| AP7 | 低 | 启动路径: 反射完整 VERSION_CODES 表与 SharedPreferences 首读在主线程 |
| AP8 | 低 | 在线获取无 HTTP 缓存, 成功即整文件重写, 资产比对每次加载都重跑 |
| AP9 | 低 | 每次格式化时间新建 `DateTimeFormatter`, Prop 键枚举反射每次重跑 |
| TE3 | 低 | 模板 instrumented 断言与 `applicationIdSuffix` 冲突, 模板测试本身无价值 |

级别统计: 高 9, 中 29, 低 21, 共 59 项 (同类项已合并).

## SSOT (单一事实源)

### SS1 [高] 全局可变 `myAndroid` 多写者

- 事实源本体: `AndroidVersionExt.kt:53-64`, 四个 `var` 字段的全局实例.  
  (文件路径 `app/src/main/java/net/imknown/android/forefrontinfo/ui/common/`, 后同约定)
- 写入方 1: 启动时 `initMyAndroid()` (`AndroidVersionExt.kt:104-110`, 从 `MyApplication.kt:92` 调用).
- 写入方 2: `HomeRepository.kt:124-131` 在侦测函数里把 lld 数据的版本字段直接赋给全局实例,  
  属于一次 detect 里的隐藏写副作用.
- 读取方: 全部 `isAtLeastAndroid*()` 经 `private val sdkInt get() = myAndroid.api`  
  (`AndroidVersionExt.kt:114-125`) 读同一组字段.
- 竞态面: 写入方 2 运行在 `Dispatchers.IO` (`HomeViewModel.kt:211`),  
  而 `outdatedOrderFirst` 的收集与使用在主线程, 四个 `var` 的写读无任何同步.
- 附加问题: `ui.home` 之外的所有调用方都会读到受影响后的值;  
  且 lld 值与运行时值可能不同, 每次刷新重复写入.
- 修复方向: `MyAndroid` 改为只读值对象, 启动时一次性确定后不再写; 仓库侦测逻辑只读.

### SS2 [高] companion 静态访问器与 DI 图并行

- `base/MyApplication.kt:73-85`: `instance`, `sharedPreferences`, 两个 `getMyString` 重载.  
- 约 100 处数据层调用 (如 `HomeRepository.kt:137,755`, `PropRepository.kt:54`,  
  `LldFileStore.kt:53,61,76`) 借静态取 Application 能力.
- 内部矛盾一处: `LldFileStore` 已构造注入 `application` (`LldFileStore.kt:27`),  
  第 61 与 76 行却读 `MyApplication.instance.assets`, 两条路径并存于同一个类.
- 组合根与自身绑定矛盾: `AppGraph.kt:206-207` 的 provider 声明 `app: MyApplication` 参数却不用,  
  返回 companion 静态值.
- 修复方向: 在组合根绑定 `Context`/`AssetManager`/`PackageManager`,  
  注入到目前读静态的调用点, 然后撤掉 companion 访问器.

### SS3 [中] SharedPreferences 两条读取路径

- DI 绑定: `AppGraph.kt:206-207` 返回静态访问器而非用 `app` 参数构建;  
  静态访问器自带 `"${instance.packageName}_preferences"` 的文件命名约定  
  (`MyApplication.kt:76-78`). 同一持久化事实的两种取法, 命名规则编码在静态里.
- 修复方向: provider 改为 `app.getSharedPreferences(...)` 构建,  
  companion 访问器改为私有或删除.

### SS4 [中] theme 与 scrollBar 两组设置各配两个 StateFlow, 手工同步

- `ui/settings/repository/SettingsStore.kt:50-64`:  
  `themeValue: StateFlow<String>` 与 `themeMode: StateFlow<AppThemeMode>` 同存一个事实.
- 两处手工成对赋值: 构造时填初值 `:129-134`, 监听回调 `:98-108`.  
  无任何机制强制两者同步, 新增写点忘记成对更新即静默分叉. 四个设置中只有这两组是成对流,  
  另两个 boolean 开关各为单流 (`SettingsStore.kt:68-72`), 不受此问题影响.
- 修复方向: 只持久化字符串流, enum 用 `.map { parse(it) }` 派生, 单点写入.

### SS5 [中] 本地化文案当作数据有效性哨兵

- 占位符值本身取自字符串资源: `ui/common/PropertyReader.kt:24-36`.
- 两处独立重实现同一组检查: `HomeRepository.kt:1181-1184` 与 `OthersRepository.kt:219-221`,  
  都与 `R.string.result_not_supported`, `R.string.build_not_filled` 比较.
- 这些字符串是真实翻译文案, 分属两个 feature 的 res 目录  
  (`ui/home/res/values/strings.xml:6`, `ui/others/res/values/strings.xml:53`).  
  翻译改一个词即静默改变侦测输出.
- 修复方向: `PropertyReader` 返回可空结果 (未取到即 null),  
  删除一切与文案比较的哨兵逻辑.

### SS6 [低] 版本比较直用字符串字典序

- 四处: `AndroidVersionExt.kt:133`, `HomeRepository.kt:658-659`, `:697`, `LldFileStore.kt:62`.  
  同一代码库已引入 `Version` 比较 (`HomeRepository.kt:345,909`),  
  但 `36.10 >= 36.2` 在字典序下为 false.
- 修复方向: 统一走 `Version` 比较 (或一个扩展函数).

## UDF (单向数据流)

### UD1 [中] 按索引的公开写入入口

- `ui/base/list/BaseListViewModel.kt:73-87`:  
  `fun updateModelDetail(targetIndex: Int, newDetail: String)` 是 public, 子类均可按位置改共享列表.
- 唯一调用方 `HomeViewModel.kt:277-285` 先 `indexOfFirst` 再按索引替换,  
  两步之间列表可能已被 `startLoad` 整体替换. 类型本已带 `MyModelType` 作身份标识.
- 修复方向: 按类型或 key 在基类内完成查找与替换, 或移入 `HomeViewModel` 私有.

### UD2 [中] 设置开关在加载中切换不生效

- `HomeViewModel.kt:58` 每次加载只用 `first()` 读 `allowNetworkData` 一次;  
  唯一有实时重同步机制的是 `outdatedOrderFirst` (`HomeViewModel.kt:89-96, 258-264` 的新代次比对).
- 用户在加载进行中切换网络开关, 下一次手动刷新前看不到效果; 同一 store 的两个消费者待遇不同.
- 修复方向: 两个开关同等处理, 或在 `collectModels` 内统一读取 store 当前值.

### UD3 [中] 同一状态的两条下行通道并行

- `ui/settings/SettingsScreen.kt:83-98` 四个偏好走 `viewModel.*.collectAsStateWithLifecycle()`,  
  同时 `:96` 的 `scrollBarMode` 又走 `LocalScrollBarMode.current`;  
  列表页同理 (`MyModelListScreen.kt:39-40,64`).
- 同一事实类 (滚动条模式) 在同一屏幕内两条通道, 且 CompositionLocal 里装的是原始  
  `MutableStateFlow` 引用, 任何 composable 取到都能写.
- 修复方向: 与其余偏好一致地走 ViewModel, CompositionLocal 仅留给无 ViewModel 的场合.

### UD4 [低] 一次性事件以返回值触发侧效应

- `SettingsScreen.kt:117-121` 用 `viewModel.getVersionClickedMessage()?.let { context.toast(it) }`;  
  隐状态为计数器 `private var timesLeft = 7` (`SettingsViewModel.kt:105-120`).
- 目前可用, 但与代码库其余的 StateFlow + 回调形态不一致.
- 修复方向: 一次性事件流, 或把 7 次计数器改为 UI 本地状态.

### UD5 [低] Screen 驱动 ViewModel 初始化并下传系统服务

- `SettingsScreen.kt:90-92` 在 `LaunchedEffect` 里调  
  `viewModel.setBuiltInDataVersion(context.packageManager, context.packageName)`,  
  `PackageManager` 经 ViewModel 传到仓库 (`SettingsViewModel.kt:99`, `SettingsRepository.kt:28-76`).
- 三个列表页走 `BaseListViewModel.init()`, 两套启动约定并存; 系统服务获取应归数据层自己完成.
- 修复方向: 数据源经注入的 `Application` 自取所需管理器, 仓库 API 不再携带 `PackageManager`.

## 架构与分层

### AR1 [高] feature 包之间互相引用成环

- home 到 settings: `HomeRepository.kt:49` 引 `ui.settings.datasource.AppInfoDataSource`  
  (用于 `:1204-1209`).
- settings 到 home: `SettingsRepository.kt:14` 引 `ui.home.datasource.LldFileStore` (用于 `:32`).
- others 到 home: `BasicDataSource.kt:7` 引 `ui.home.datasource.AndroidDataSource` (用于 `:25`).
- 后果: feature 不是独立单元; `LldFileStore` 被两个 feature (ui.home, ui.settings) 消费  
  却放在 `ui.home.datasource`.
- 修复方向: 把 `AppInfoDataSource`, `LldFileStore`, `AndroidDataSource` 常量移入共享数据层,  
  feature 间只留 feature 专属的引用.

### AR2 [高] `ui.common` 反向依赖 `ui.home.model`

- `ui/common/AndroidVersionExt.kt:11` 引 `ui.home.model.Lld`, `:132-134` 基于它做业务判断;  
  反向引用: `ui/home/model/Lld.kt:5` 引 `ui.common.CODENAME_NONE`.
- `ui.common` 是多个 feature 共用的最低层包, 却携带 home 的模型逻辑,  
  其他 feature 因此间接依赖 home 的模型类型.
- 修复方向: `Lld` 模型迁到中立包 (或 `:base`), codename 常量随之, 两方向引用改为同层同向.

### AR3 [中] `HomeRepository` 单类多职

- `HomeRepository.kt:58-1211`: 文件读写链路, 在线获取, 约 24 个 `detect*()` 方法,  
  每个方法自己决定 `StatusColor` 并拼装本地化详情, 期间穿插文件探测 (`:528-544`),  
  `Settings.Global` 读取 (`:754-773`), 反射 (`:1046-1111`).
- Screen 到 DataSource 各层职责在此类内合为一层.
- 修复方向: 按主题拆分侦测器; 把文件, Settings.Global 与反射探测下沉 DataSource;  
  返回领域值, 由映射层组装 `MyModel`.

### AR4 [中] `OthersRepository` 纯透传

- `OthersRepository.kt:38-278`: 大多数 method 为一行守卫包装 DataSource 调用 (示例 `:39-44`);  
  少数几个带真实逻辑 (`getPartitionFingerprints` `:211-239`, `getCodename` `:189-199`,  
  `getTime` `:165-169`, `getKernelVersion` `:253-267`). 仓库层整体仍偏样板.
- 修复方向: 合并仓库与一比一 DataSource 为一个 feature 层, 或赋予仓库真正的聚合职责.

### AR5 [中] `AndroidDataSource` 常量类跨界

- `ui/home/datasource/AndroidDataSource.kt:3-145`: 无 `@Inject`, 纯常量容器  
  (系统属性键, 路径, shell 命令, 期望值), 被 home 仓库与其他 feature 静态引用.
- 与其余 DataSource 全部构造注入的做法不一致.
- 修复方向: 改为 `object` 常量文件放入 `ui.common` (或注入), 消除跨 feature 引用.

### AR6 [中] ViewModel 触达 DataSource 概念

- `OthersViewModel.kt:16,55`: 导入 `ArchitectureDataSource` 只为把 `DRIVER_BINDER`  
  常量传给仓库.
- `PropViewModel.kt:3,34-36`: 导入 `android.provider.Settings` 并把 `KClass` 传给仓库.
- 修复方向: 仓库暴露无参或自含决策的 API, 常量与类的选择收回仓库内部.

### AR7 [中] Dispatcher 归属与双层包装

- home/others/prop: ViewModel 把仓库调用包进 `withContext(Dispatchers.IO)`  
  (`HomeViewModel.kt:103,114,137,158,172,193,211`; `OthersViewModel.kt:34`; `PropViewModel.kt:31`).
- settings: 仓库自包 (`SettingsRepository.kt:31,39,52,74`).
- home 内唯一的双层嵌套: `HomeViewModel.kt:211` 已在 IO 块内, `:221-223` 又包一层.
- 修复方向: 统一由仓库或数据源自持 dispatcher, ViewModel 不包装; 删除嵌套.

### AR8 [中] 业务决策位置错档

- 主机选择 (按是否中国大陆时区选 Gitee 或 GitHub) 在数据源内: `LldDataSource.kt:34-39`;  
  判断工具 `isChinaMainlandTimezone()` 反而放在共享模块 `:base`  
  (`base/src/main/java/net/imknown/android/forefrontinfo/base/extension/DateTimeExt.kt:35-37`).
- 修复方向: 仓库层选定主机再交给数据源; 时区工具按它的使用归属移动.

## 设计模式

### AR9 [高] 基类加载无失败边界, 且加载无超时 (编号沿用架构序列, 条目按性质归入本节)

- `BaseListViewModel.kt:60-71` `startLoad`: 对 `collectModels()` 无 try/catch,  
  `setLoading(false)` 只在成功路径执行, 基类也没有加载超时的概念.
- 今日的实际风险是卡住: 各仓库的侦测条目目前都有守卫, 尚不会抛异常,  
  但一次不返回的 shell/文件调用 (`IShell`/libsu 无超时) 会让 `isLoading` 永远为 true,  
  且 `:61-63` 的 "活跃任务即去重" 判断挡住一切刷新.
- 潜在风险: 未来任一子类只要有一条路径未守卫而抛出, 会走协程未捕获处理器崩溃.
- 修复方向: `startLoad` 内包 try/catch, 失败渲染失败态并复位 loading; 加载加超时.

### DP1 [中] 仓库返回表现层模型

- `SettingsRepository.kt:27-113`: 数据层拼装本地化文案并返回  
  `data class Version(@param:StringRes val id: Int, ...)`, 屏幕 `SettingsScreen.kt:224-233`  
  用位置占位符再拼接, 格式串拆在两层.
- 预览代码硬编码同一组版本常量 (`SettingsScreen.kt:453-456`), 与 `BuildConfig` 值重复.
- 修复方向: 仓库返回普通字段, 屏幕或 ViewModel 做字符串映射; 预览引用同一常量源.

### DP2 [中] 注入具体 Application 类型

- `PropertyReader.kt:20-23`, `LldFileStore.kt:27` 注入 `MyApplication` 实类,  
  与 `AppInfoDataSource` 等以方法参数收 `PackageManager`/`Context` 的形态不一致.
- 修复方向: 注入 `Context` 或按需以参数收管理器, 与兄弟类保持一致.

### DP3 [中] 未使用的 `ShellDefault` 带有潜在死锁

- `base/src/main/java/net/imknown/android/forefrontinfo/base/shell/impl/ShellDefault.kt:7-27`:  
  全仓库无调用方 (仅自身声明一处), DI 只绑了 `PropertyDefault` (`AppGraph.kt:186-193`)  
  与 `ShellLibSu` (`ShellLibSu.kt:15-16`).
- 代码先 `process.waitFor()` (`:15`) 再读流 (`:20-21`), 管道缓冲写满即永久阻塞.
- 修复方向: 不删除的前提下把读流移到 wait 之前, 或加显著标记防止未来一行绑定直接接入.

## 模块化

### MD1 [高] split package: `:app` 使用 `:base` 的包名

- `app/src/main/java/net/imknown/android/forefrontinfo/base/MyApplication.kt:1`  
  声明 `package net.imknown.android.forefrontinfo.base`, 与 `:base` 模块命名空间  
  (`base/build.gradle.kts:6`) 完全一致; `AppThemeMode`/`ScrollBarMode` 同文件.
- `SettingsStore.kt:11-13` 导入的 `base.*` 视觉上像 `:base` 类型实际不是;  
  未来向 `:base` 迁移任何类型都会先撞名.
- 修复方向: `MyApplication` 移到应用根包, 两个 mode 类型给真实归属.

### MD2 [中] 领域语义色放在 Compose 主题包

- `ui/theme/ExtendedColors.kt:9-14` 定义 `StatusColor`;  
  数据层引用: `HomeRepository.kt:50`, 共享模型 `MyModel.kt:5,21`, `MyModelExt.kt:9`.
- 每个 feature 的数据路径与只为 Compose 服务的包耦合.
- 修复方向: `StatusColor` 移到 `ui.base.list` 模型旁边, `ui.theme` 只留映射.

### MD3 [中] `:binderDetector` 边界已失效

- 模块本体 7 行 `object` + `external fun` (`BinderDetector.kt:3-6`);  
  仅 `:app` 的 `ArchitectureDataSource.kt:27-30` 消费, 且在读取方法内每次 `System.loadLibrary`,  
  库缺失时抛出一次 `UnsatisfiedLinkError` (已加载成功后重复调用不再失败); errno 语义以  
  `:app` 常量存在 (`ArchitectureDataSource.kt:15-18`), 失败映射与打印在 `OthersRepository.kt:83-110`.
- 修复方向: 模块提供懒加载一次并返回结果类型的门面, `:app` 只消费门面.

### MD4 [中] 共享代码归属无规则

- `:base` 仅 7 个文件 (接口与实现); 跨 feature 共享物实际散落三处:  
  `ui/base/list/*` (模型, 列表 ViewModel, 屏), `ui/common/*`, `ui/base/ext/ToastExt.kt`.  
  归属不一致的后果已直接体现在 AR1 与 AR2 的包环上.
- 修复方向: 明文定规则 (如 `:base` 收平台无关物, `ui.base.list` 收共享 UI 模型,  
  `ui.common` 收共享扩展), 按规则把文件放回各自的位置.

### MD5 [中] 同名类的注入陷阱

- 两个同名 `FingerprintDataSource`: `ui/others/datasource/FingerprintDataSource.kt:7-8`  
  (构建指纹属性) 与 `ui/settings/datasource/FingerprintDataSource.kt:12-57`  
  (签名 hash 与分发者映射). 均为 `@Inject` 按类型注入; 两个类的成员方法不同, 选错包会在  
  调用点编译报错, 不会静默产出错数据 (`SettingsRepository.kt:24`, `OthersRepository.kt:34`  
  各自使用本包的类).
- 另一对: `ui/prop/datasource/SettingsDataSource.kt:10` (Android Settings 表)  
  与 settings feature 的 `SettingsStore`/`SettingsRepository` (用户偏好) 名义易混.
- 修复方向: 各改名其一 (如 `SignatureDataSource`, `AndroidSettingsDataSource`).

### MD6 [中] 共享文件携带 feature 专属逻辑

- `ui/base/list/MyModelExt.kt` 按 feature 分段承载 Home 专属 (`:35-73`),  
  Others 专属 (`:83-111`), Prop 专属 (`:126-137`), 各 feature 都得引入整文件.
- 修复方向: 各 feature 自有守卫文件, 共享层只留真正通用的构造 (归属问题亦见 MD4).

### MD7 [低] 指向不存在目录的 fileTree 依赖

- `app/build.gradle.kts:126` 与 `base/build.gradle.kts:14` 各有一条 `fileTree("libs")`,  
  全仓库无 `libs/` 目录. `:base` 一侧未来放入 jar 即成为 api 传递依赖, 绕过版本目录审查.
- 修复方向: 删除两行, 或改为显式逐文件依赖.

### MD8 [低] settings 级插件版本不在目录内

- `settings.gradle.kts:63` (`com.gradle.develocity`) 与 `:69` (foojay resolver)  
  是构建脚本里唯一不在版本目录内的版本号, 脚本改动是唯一的更新点.
- 修复方向: 保留但确认这两处的更新纳入与其他版本一致的审查路径 (现状可接受).

### MD9 [低] 组合根引用 feature 辅助包

- `AppGraph.kt:22` 导入 `ui.common.isAtLeastAndroid16`, 用于 `:125`.  
  组合根按理不应依赖任何 feature 包.
- 修复方向: 版本判断工具移入 `:base`.

## 安全 (Security)

### SE1 [高] lld.json 落在其他 app 可写的目录, 内容与来源零校验

- 路径: `MyApplication.kt:60-71` `getDownloadDir()` 优先 `getExternalFilesDir`,  
  仅当 external 目录为 null 或不存在时才退到内部 `filesDir` (`MyApplication.kt:62-71`);  
  `LldFileStore.kt:32` 据此确定保存路径.
- 外部 app 文件目录的可写面: API < 30 时, 持 `WRITE_EXTERNAL_STORAGE` 的旧式 app 可写;  
  API 30+ 起其他 app 不能写本 app 的专属目录 (持 `MANAGE_EXTERNAL_STORAGE` 也不行),  
  但 adb/root, 调试工具与被篡改的备份恢复路径仍可达.
- 信任面: `LldFileStore.kt:45-79` 对已存在文件只看它能否解析, 以及 `version` 不旧于内置资产  
  (即 `:62` 的比较) 即继续使用. 此后被攻击者投毒的文件成为全部侦测事实  
  (Treble/GSI/补丁/内核属性等) 的唯一来源, 没有任何错误行; 重拷只发生在解析失败时.
- 定级说明: 维持 高. 定级理由与写入面的广度无关, 关键在信任结构: 这个文件是零来源校验的外部输入,  
  却是全部侦测结论的事实源, 一旦沿上面那条路径被覆写, 每一行结论即刻被翻转.
- 修复方向: 保存路径改内部 `filesDir` (退路已存在), 外部目录只留给用户导出副本;  
  或对已存文件做指纹比对而非信它自报的 version.

### SE2 [中] 在线 payload 无完整性校验, 且影响放大作用于全局状态

- `LldDataSource.kt:34-48`: GET 固定 HTTPS URL, `response.body()` 直取 String,  
  无哈希, 无签名, 无固定 digest.
- 唯二校验: 可解析, 加 `version` 符合 `yyyy-MM-dd HH:mm Z` 格式 (`HomeViewModel.kt:128`,  
  我核对过这个位置确实是主线唯一的格式门槛).
- 影响放大: 通过校验即落盘 (`HomeViewModel.kt:136-145`), 并经 `HomeRepository.kt:124-131`  
  反写全局 `myAndroid` (同 SS1), 整个 app 后续读到的就是攻击者的版本.  
  任何能在 HTTPS 链路上替换 payload 的攻击者 (用户自选的代理, 被攻破的 CA,  
  CDN/上游账号失陷) 都能达到.
- 伴随的低分项: body 解码无长度上限, 15s 请求超时 (`AppGraph.kt:177-181`) 是唯一边界.
- 修复方向: 随 version 发布 SHA-256 摘要或签名, 落盘前必验; 加 content-length 上限.

### SE3 [中] 备份/迁移规则空默认全包括

- `app/src/main/java/net/imknown/android/forefrontinfo/base/resBackup/xml/backup_rules.xml:5`  
  为空 `<full-backup-content />` (默认允许全部), `data_extraction_rules.xml:4` 同;  
  Manifest `:12-14` 引用二者.
- 后果: lld.json 外部副本与偏好进入云备份与设备迁移, 其他环境恢复出来的旧版本文件  
  会以字典序较大的 version 长期压制内置资产 (与 SE1, SE5 叠加).
- 修复方向: 两规则文件显式排除 lld.json 缓存路径.

### SE4 [中] debug 签名材料入库

- `keys/debug.keystore` 确认被 git 跟踪; debug 签名配置的 storePassword 与 keyPassword  
  均硬编码在 `app/build.gradle.kts:73-83` (取值为 AOSP 默认值, 按仓库 "无敏感信息" 规矩,  
  原文不在本报告复述).
- app 自身公开列出 debug 签名的分发者 (`ui/settings/datasource/FingerprintDataSource.kt:23-24,53-57`),  
  即公开渠道确有 debug 构建.
- 后果: 任何拿到仓库的人可用同签名 APK 覆盖已装 debug 构建, 无安装确认.
- 修复方向: 库外保管 debug keystore, 公开分发渠道仅走 release 签名.

### SE5 [低] 离线文件 version 无格式与时间校验

- `LldFileStore.kt:50-64`: 离线路径只查解析成功与否, `savedLldVersion < assetLldVersion`  
  是无格式的字典序比较 (在线路径反而有格式检查, 见 `HomeViewModel.kt:128`).
- 修复方向: 离线值也过 `isLldDatetime()`, 比较改解析后时间或走 `Version` (与 SS6 合并).

### SE6 [低] 重定向未限制

- `AppGraph.kt:116-165` 未设 `followRedirects(false)`, OkHttp 引擎默认跟随,  
  可跨主机与 https 降级后通过 SE2 的弱校验.
- 修复方向: 关闭重定向, 或校验最终响应主机.

### SE7 [低] `QUERY_ALL_PACKAGES` 授权偏宽

- `app/src/main/AndroidManifest.xml:6-8` (带 `tools:ignore`),  
  支撑 "旧 targetSdk 系统 APK" 扫描与 WebView provider 枚举.
- 修复方向: 迁移到 `<queries>` 细化块, 或补商店申报理由.

### SE8 [低] 系统属性未清洗即入路径

- `HomeRepository.kt:523-531`: `"/vendor/etc/vintf/manifest_%s.xml".format(...)`  
  的取值来自 `ro.boot.product.vendor.sku` (`AndroidDataSource.kt:63-64`) 未过滤.
- 影响限于侦测准确性, 无提权.
- 修复方向: 拒绝含 `/` 或 `..` 的值再拼接.

### 确认无问题的面

- 仅 HTTPS: 无 `usesCleartextTraffic`, targetSdk 锁值导致平台默认禁止明文流量  
  (锁值见 `gradle/toml/build.toml`);  
  两 URL 硬编码 `https://` (`LldDataSource.kt:41`).
- 组件受攻击面最小化: 出口仅 launcher `MainActivity` (`AndroidManifest.xml:23-31`),  
  无 receiver/provider/服务, 本应用不能被外部 app 唤起做侦测动作.
- shell 无注入面: 所有命令为编译期常量; libsu 以非 root 运行 (`MyApplication.kt:108-116`).
- 原生侧结果被映射成封闭枚举, 失败走 "unknown" 展示 (`OthersRepository.kt:87-107`).
- 在线获取显式开关, 默认关闭 (`HomeViewModel.kt:58`; 默认值见 `SettingsStore.kt:69`,`:136`).
- 无硬编码 secret (SE4 之外), 无 WebView JS 面, 无 deep link.

## Compose 性能 (整体较干净, 合计 6 项低分)

### CP1 [低] 每行一个空的 clickable Card

- `MyModelCard.kt:49` `onClick = {}` 带涟漪与交互语义; 每行只为涟漪付出  
  pointer-input 节点与 semantics 成本, 以 Prop 页 (可能数百行) 叠加最多.  
  此处的判断依据是 `MyModelCard.kt:43-48` 的行内注释 (分析唯一动用注释的例外,  
  见 "范围与方法"), 认定为负责人接受过的取舍 (涟漪反馈与 TalkBack 双击播报),  
  故只记录开销, 不计作问题.
- 修复方向: 若重评取舍, 用 `clickable(enabled=false)` 或 `clearAndSetSemantics { }`.

### CP2 [低] Settings 页单作用域收集六项

- `SettingsScreen.kt:83-98` 六个 `collectAsStateWithLifecycle` 喂一个 `SettingsContent`;  
  任一设置改动全页 recomposition. 子行大多数能跳过 (strong skipping 默认开启),  
  实际代价是一次主体执行.
- 修复方向: 仅当 profiling 显示问题时拆行采集.

### CP3 [低] 动态配色未 remember

- `Theme.kt:321-329`: 每次 `AppTheme` recomposition 新建 `dynamicDarkColorScheme(context)`  
  (约 30 个槽位的 Colors), children 按值相等跳过, 仅为分配开销.
- 修复方向: 分支内 `remember(darkTheme, context) { ... }`.

### CP4 [低] 空列表每次 recomposition 新建

- `MyModelListScreen.kt:71` `models ?: persistentListOf()`;  
  提升为 top-level 常量即可.

### CP5 [低] 滚动条修饰符作用域与颜色重建

- `ScrollBarExt.kt:41` 每次手势起止 `isScrollInProgress` 翻转带来两次额外失效;  
  `:54` 每次重算 `barColor`. alpha 动画的读取位置已正确 (只影响 draw 阶段).
- 修复方向: `remember` 颜色; 读取位置保持现状即可.

### CP6 [低] Prop 行 key 唯一性靠命名约定

- `MyModelListScreen.kt:130` `key = { it.key }`; 今天的来源互不重合  
  (JVM props `java.*`, `getprop` 行, `Settings.*` 表前缀), 但无结构性保证,  
  未来重复即 LazyColumn 崩溃.
- 修复方向: Raw key 加来源前缀, 构造时去重.

### 确认无问题的面

- 列表管线健全: `key`/`contentType`/`animateItem` 齐备 (`MyModelListScreen.kt:130,141`);  
  `MyModel` `@Immutable` 纯稳定字段; `PersistentList` 仅在 ViewModel 边界转换一次;  
  ViewModel 标注 `@Stable`; 列表刷新靠数据类相等跳过未变行.
- 重活都在 ViewModel 的 IO 块内, 组合体内无 JSON/排序/大字符串运算.
- `MainActivity.kt:75-83` 下发 StateFlow 对象而非常值, 主题改动只失效 `AppTheme` 作用域.

## App 性能

### AP1 [高] Prop 页逐键 binder 查询 (N+1)

- `PropRepository.kt:50-63` (我核对过) 对键列表逐个调 `getStringOrNullOrThrow(..., contentResolver, key)`,  
  每个 key 一次 binder 往返 (`SettingsDataSource.kt:20-26`).
- `Settings.System`/`Secure`/`Global` 每表通常百余键, Prop 页一次加载即数百次串行 binder 调用.
- 修复方向: 每表一次 `contentResolver.query(CONTENT_URI)` 批量取回, 再按行组装.

### AP2 [中] 一次离线加载解析 lld.json 三次, 无进程缓存

- 解析 1: `LldFileStore.kt:51` 已存文件全量解码只取 `version`.
- 解析 2: `LldFileStore.kt:111-122` (`getAssetLldVersion` 经 `getAssetLld`) 资产全量解码只取 `version`.
- 解析 3: `HomeViewModel.kt:172-174` 同一已存文件再全量解码.
- 附加: `SettingsRepository.kt:31-35` 又全量解析一次只取 version; 每次排序切换  
  `payloadOutdatedTargetSdkVersionApk` (`HomeViewModel.kt:267-274`) 重跑整套.
- 修复方向: 进程级缓存解析结果 (或只缓存 version, 资产 version 只需一次);  
  解析改为只取顶层 version 的局部方案.

### AP3 [中] 每次刷新全量枚举已安装应用

- `HomeRepository.kt:1121-1131`: 每次 `collectModels` 枚举全部包, 过滤排序拼串;  
  排序切换路径 (`HomeViewModel.kt:93, 261-274`) 再来一遍. 数据只在安装/卸载时变.
- 修复方向: 结果按包变更缓存, 或只在需要这一行时计算; 切换排序时仅对既有行重排.

### AP4 [中] 系统属性反射每次调用重定位

- `base/.../property/impl/PropertyDefault.kt:10-18`:  
  每读一个属性就 `systemPropertiesClass.getDeclaredMethod("get", ...)`.
- 每次刷新约 20 次 (Home 各 detect 的 `propertyReader` 调用点), Others 加上 10 次  
  (`ui/others/datasource/FingerprintDataSource.kt:24-25` 分区指纹列表).
- 修复方向: `Method` 对象提升为 companion 常量.

### AP5 [中] 每刷新串行 shell 往返

- `MountDataSource.kt:27` (`cat /proc/mounts`), `HomeRepository.kt:579` (GSI linkerconfig),  
  `:850` (`getenforce`), `:892` (`toybox --version`), 全部串行在一个 IO 块内, 每次独立 exec;  
  Prop 另有一次 `getprop` 全量 dump (`PropViewModel.kt:38` 逐层到 `PropertiesDataSource.kt:38`),  
  Others 页另有 `cat /proc/version` 与 `uname -a` (`KernelDataSource.kt:9-10`).
- 修复方向: 四处并行 launch 后 await, 或合并成一次 exec.

### AP6 [低] WebView 提供商反射链每次刷新重建

- `HomeRepository.kt:1046-1111`: `Class.forName` 再逐字段反射与 `getPackageInfoOrNull` 调用,  
  结果只依赖 WebView 包状态; `:643-645` 资源 id 也每刷重取.
- 修复方向: 按进程缓存, 或按 WebView 包状态失效.

### AP7 [低] 启动路径上的同步开销

- `MyApplication.kt:92` 起 `initMyAndroid()` 在主线程反射整张 VERSION_CODES 表  
  (`AndroidVersionExt.kt:22-43`, 每字段 `isAccessible`); `:103` 急切解析设置存储,  
  首次 SharedPreferences 磁盘读与 XML 解析发生在 `Application.onCreate`  
  (`SettingsStore.kt:116-142`).
- 实际量级预计 < 10ms (偏好文件小), 但两者都可移出首帧路径.
- 确认干净: 首屏重活确实都在首帧之后, shell 的拉起延迟到首次真实命令 (`ShellLibSu.kt`).

### AP8 [低] 网络与缓存的重复工作

- 无 `If-None-Match`/`If-Modified-Since`, 无 OkHttp `Cache` (`AppGraph.kt:116-183`);  
  每次成功在线获取都整文件重写 (`LldFileStore.kt:101-109`), 字节相同也不比对;  
  资产比对每次离线加载都跑 (解析两次, 见 AP2), 而资产 version 进程内恒定.
- 修复方向: 内容相同则跳过写; `copyJsonIfNeededOrThrow` 结果按进程记忆; 视需要装 HTTP 缓存.

### AP9 [低] 小型重复构造

- 每次调用 `Long` 版格式化都新建 `DateTimeFormatter`  
  (`base/.../extension/DateTimeExt.kt:29`, 顶部已有常量形态可循 `:17`);  
  Prop 键列表反射枚举每次 load 重跑 (`SettingsDataSource.kt:11-17`), 而键集合编译期恒定.
- 修复方向: 提为常量/`lazy`, 键列表记忆化.

### 确认无问题的面

- 无 `runBlocking`, 无 `Thread.sleep`, 无 `commit()` (全仓库 grep 零命中).
- 单一 Ktor client 复用 (`AppGraph.kt:114-182`), 超时三层齐备;  
  列表加载有同 ViewModel 去重 (`BaseListViewModel.kt:60-63`).
- 无 Activity/View 级 Context 泄漏进 `object`/companion (Application 级的静态持有就是 SS2 本身);  
  解析产物不驻留进程.

## Test (测试)

本节后补 (2026-10-18). 参考的外部资料: 负责人指定的 developer.android.google.cn 的 testing  
系列页面 (local-tests, robolectric, screenshot testing, what-to-test, compose synchronization  
与 compose migrate-v2 等), 以及官方测试基础设施指引; 官方页面内容以抓取时点为准, 引用行以补写时  
核对为准. 本节分现状盘点, 问题条目 (TE1-TE4), 建议的测试基础设施三部分.

### 现状盘点 (补写当日核对)

- 测试依赖只有一个注入点, 在 convention plugin: `build-logic/convention/.../KotlinAndroid.kt:72-83`  
  的 `configureTest` (runner 为默认的 `androidx.test.runner.AndroidJUnitRunner`, `:75`), 下挂  
  两个 bundle (`:79-80`): `test = ["junit"]` (bundle 行为 `gradle/toml/android.toml:104`,  
  它指向的 junit 条目在 `:47`,`:91`), `androidTest = ["test-core", "test-ext-junit"]`  
  (`:105`; 其中 test-ext-junit 条目在 `:54`,`:93`, test-core 条目在 `:53`,`:92`).
- 测试文件实况 (5 个文件, 1 个真):
  - 真测试: `base/src/test/.../DateTimeExtTest.kt`, 4 个用例, 守住 lld `version` 字段的格式契约  
    与本地化无关性;
  - 模板 4 个: `app` 与 `base` 的 `ExampleUnitTest.kt` (断言 2+2), `app` 与 `base` 的  
    `ExampleInstrumentedTest.kt`;
  - `binderDetector` 无任何测试目录.
- 没有 Robolectric, Espresso, `androidx.compose.ui:ui-test-*`, mock 框架,  
  `kotlinx-coroutines-test`, 覆盖率工具或截图测试 (全目录 grep 为零).

### TE1 [高] 业务逻辑零测试

- 本报告其他角度定位出的全部业务逻辑没有相应测试: `HomeRepository` 约 1211 行 (AR3),  
  `SettingsStore` 双流配对 (SS4), `BaseListViewModel` 加载契约 (AR9), `LldFileStore` 文件编排  
  (SE1, AP2), `MyAndroid` 全局与版本比较 (SS1, SS6), `PropertyReader` 与哨兵 (SS5),  
  `MyModelExt` (MD6), `JsonExt` 与 `DateTimeExt` 其余两个 function (AP9 涉及处).
- 按 "What to test" 官方页, ViewModel, 数据层, 工具类的单元测试是基本要求; 现况一条都没有,  
  这些条目既无测试在当初指出, 也无测试守住回归.
- 修复方向: 建立测试基础设施 (见下) 后补测试, 顺序从纯逻辑到 Robolectric 到 ViewModel  
  (与 TE4 对应的重构同批做).

### TE2 [中] 唯一的真实测试不在现况命令入口的执行面内

- `testFossDebugUnitTest` 是 flavor 任务, 只有 `:app` 定义了 flavor; `:base` 只有不带 flavor  
  的 `testDebugUnitTest`, 两者不同名, 所以跑 `testFossDebugUnitTest` 时 `DateTimeExtTest`  
  不会执行. 全仓库唯一的真测试实际处于 "只有单独手动触发 `:base:testDebugUnitTest` 才会跑" 的状态.
- 修复方向: 统一测试命令 (库模块加任务别名, 或约定用全工程任务), 属命令约定问题, 不改业务代码.

### TE3 [低] 模板测试与 flavor 设置冲突, 且模板本身无价值

- `ExampleInstrumentedTest` (`app/src/androidTest`) 断言 `targetContext.packageName` 等于  
  无后缀包名, 而构建对变体设置了 `applicationIdSuffix = ".$name"` (`app/build.gradle.kts:105`):  
  debug 变体上这条断言必失败; 现在没有设备执行入口, 从未暴露.
- 两处 `ExampleUnitTest` 断言 2+2, 无覆盖价值.
- 修复方向: 直接删除全部模板测试; `androidTest` 源集在决定做插桩测试时再建.

### TE4 [中] 可测性被既有条目阻断

- 不先处理下面的条目, 测试会写成大面积 mock 或以静态初始化为前提的形态:
  - 具体类型注入 `MyApplication` (DP2) 与静态 service locator (SS2, SS3): JVM 单测里  
    `MyApplication.instance` 未初始化即系统性 NPE, 静态路径也无法用 fake 替换.
  - 本地化哨兵 (SS5): 测试断言会绑死翻译文案.
  - 全局可变 `myAndroid` (SS1): 测试之间相互污染.
  - 无超时的加载链 (AR9): fake 卡住的路径需要能被测试观察到才可断言.
  - 物理 shell: libsu 无法在 JVM 造真 shell; `:base` 的 `IProperty`/`IShell` 接口是现成替换点  
    (接口先行的设计在这里直接受益), 测试用 fake 实现即可.
- 修复方向: TE1 的第一批纯 JVM 测试 (版本比较, 日期, `JsonExt`, `SettingsStore` 流)  
  不受以上影响可先行; 其余排到对应重构之后.

### 建议的测试基础设施 (分层建议; 引入与否由负责人决定)

1. **纯 JVM 单元测试** (`src/test` 中无 Android 依赖的纯逻辑): 负责人指定的 JUnit 5 可行,  
   引 `junit-jupiter` 加 junit-bom; 实现时以当时的 AGP 文档核对 (Local test task 本质是  
   Gradle 的 Test 任务, 配 `useJUnitPlatform()` 即跑 JUnit 5). 两个现况需要一致: 官方  
   local-tests 页与 instrumented 测试文档当前仍以 JUnit 4 为基准; Compose 测试的  
   `createComposeRule` (v1 与 v2 相同) 是 JUnit 4 的 Rule, JUnit 5 不能直接承载.  
   所以现实形态是混合: 纯逻辑与 ViewModel (配 `kotlinx-coroutines-test` 的 `runTest` 与  
   Main dispatcher 替换) 走 JUnit 5, Compose/instrumented 保留 JUnit 4. 这是决策点一  
   (另一选项: 全 JUnit 4, 与官方文档完全一致).
2. **Robolectric 承载 Compose 行为测试** (官方建议: 不必每个 UI 都上设备; 把 instrumented  
   搬进 `test` 源集, `testOptions.unitTests.isIncludeAndroidResources = true`): 需补  
   `androidx.compose.ui:ui-test-junit4` 与 `ui-test-manifest`, 外加 robolectric. Compose 测试  
   v2 API (`androidx.compose.ui.test.junit4.v2.createComposeRule` 等) 自 `ui-test`  
   1.11.0-alpha03 起可用; `ComposeUiTestConfig` 自 1.13.0-alpha01 起, 主线程同步同样自  
   1.13.0-alpha01 起; 本工程 BOM 锁值见 `gradle/toml/android.toml:22`, 其解析出的  
  `ui-test` 具体版本实现时按 BOM 映射核对后定;  
   v1 API 已标弃用但仍可用. 决策点二: 锁 BOM 内可用版本先写, 还是等 v2 API 全量可用再起 UI 层.
3. **截图测试** (官方把截图测试点名为 Compose 视觉回归的首选): 建议最小面起步  
   (`MyModelCard`, Settings 关键行), 控制截图数量, 官方页强调不要做全组合. 官方  
   Compose Preview Screenshot Testing 方案的 test-suite 入口自 AGP 9.5.0-alpha03 起,  
   本工程 AGP 走不到入口 (版本目录另备 canary 档 `androidGradlePlugin-canary`  
   (`gradle/toml/android.toml:9`), 这一档高于 9.5.0-alpha03 门槛, 是否启用按版本分层规则  
   由负责人决定); 旧独立插件路径仍可用但官方已标注 deprecated. 选项 (a) 单独决策 AGP 升级后走 test-suite; 选项 (b) Roborazzi  
   (基于 Robolectric 渲染). 决策点三, 与 AGP 升级解耦.
4. **插桩与端到端**: UIAutomator 与 Gemini Journeys 建议暂不引入: 本 app 无 deep link,  
   无系统 UI 交涉, 首批旅程 (启动, 三个 tab, 设置项切换) 用 Robolectric 旅程即可; Journeys  
   属实验性能力, 按仓库的版本分层规则需负责人另行决定. 性能测试 (Macrobenchmark 等) 本轮不引入.

### 测试基础设施汇总

- 决策点四个: (1) JUnit 5 混合 vs 全 JUnit 4; (2) Compose 测试先锁 BOM 内 v1 写起, 还是等  
  v2 API 齐后起 UI 层; (3) 截图方案选 (a) 或 (b); (4) 覆盖率工具 (Jacoco) 是否引入.
- 引入次序建议: 决策完成后, 先 TE1 第一批纯 JVM 测试与 TE3 模板清理, 再 Robolectric 层,  
  与 TE4 对应的重构同步推进.

## 核对说明

撰写者直接读过并复核了以下条目的引用行 (其余条目的引用行来自五路审查的直接读取):

- SS1 (`AndroidVersionExt.kt:53-110,114-125`, `HomeRepository.kt:124-131`),  
  SS2 (`MyApplication.kt:55-116`, `AppGraph.kt:206-207`), AR2 (`AndroidVersionExt.kt:11,132-134`),  
  AR9 与 UD1 (`BaseListViewModel.kt:1-94`), SE1 (`LldFileStore.kt:1-123`, `MyApplication.kt:60-71`),  
  SE2 (`HomeViewModel.kt:95-204`, `LldDataSource.kt:1-51`),  
  SE3 (`backup_rules.xml` 全文) 与 SE4 (`git ls-files` 的跟踪状态), AP1 (`PropRepository.kt:1-104`),  
  CP1 (`MyModelCard.kt:40-79`), MD1 的 split package 事实 (`MyApplication.kt:1` 的 package 声明).

报告其余条目的 `文件:行` 以当时代码为准; 若后续代码变更, 先逐行复验再引用.

Test 一节 (现状盘点, TE1-TE4, 测试基础设施建议) 的引用行由撰写者亲自读取核对  
(5 个测试文件, `KotlinAndroid.kt` 的 `configureTest`, `android.toml` 的两个 bundle 与  
`compose-bom` 与 AGP 的锁值, `app/build.gradle.kts` 的 flavor 后缀).
