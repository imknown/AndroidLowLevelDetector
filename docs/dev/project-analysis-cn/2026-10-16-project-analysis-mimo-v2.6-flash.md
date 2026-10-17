# 项目分析报告 (mimo-v2.6-flash)

> **Status**: **record**. 2026-10-16 对代码的一次时点阅读; 文本保留当时写下的内容,  
> 含其中的更正, 后续发现可以追加进来.

## 范围

- 覆盖的角度: SSOT, UDF, 架构, 设计模式, 模块化, Security, Compose 性能, App 性能.
- 按要求排除的角度: CI, 测试, NDK/JNI.
- 输入: Kotlin 源码, `AndroidManifest.xml`, 资源, Gradle 脚本与版本目录. 未作为输入:  
  `docs/dev`, `docs/spec` 与代码注释, 所以下面每条发现只以可执行代码为依据.
- 证据路径中 `...` 代表 `net/imknown/android/forefrontinfo/`.
- 严重度: 高 = 今天就有可走通的错误行为, 暴露或用户可感的成本; 中 = 条件更窄的实际问题,  
  或后续成本明确的结构性问题; 低 = 卫生问题或很小的成本.
- 标签 `note` 不是严重度: 它标记一处查过, 没有问题的位置, 并指向相关发现.
- 发现编号 (`SSOT-1`, `SEC-3`, ...) 只在本报告内使用.

SSOT (单一事实来源): 一个事实只有一个持有者, 没有第二个位置可以更新它.  
UDF (单向数据流): 状态向下流向 UI, 事件向上流向状态的持有者, 每个事实只有一个写入方向.

## 汇总

| 编号 | 严重度 | 发现 |
|---|---|---|
| SSOT-1 | 高 | 版本事实的全局对象 `myAndroid` 有两个写入方且没有同步 |
| SSOT-2 | 中 | Application 既可以注入取得, 也可以经静态成员取得 |
| SSOT-3 | 中 | 同一行列表项由两个不同的 `lld` 数据源构建 |
| SSOT-4 | 低 | 枚举型设置被保存为两条并行的流 |
| SSOT-5 | 低 | Treble 支持状态又从行颜色推导了一遍 |
| SSOT-6 | 低 | 当前标签页索引存在两份, 保存的那份没有校验 |
| UDF-1 | 中 | 两条事件路径写同一行, 靠一个计数器协调 |
| UDF-2 | 中 | 网络开关每次加载只读一次, 之后不再观察 |
| UDF-3 | 低 | 一个设置经两条通道到达设置页 |
| UDF-4 | 低 | 版本行彩蛋放在一个有副作用的取值函数里 |
| ARCH-1 | 中 | 一个 1211 行的仓库包揽整个 Home 功能 |
| ARCH-2 | 中 | 仓库构建 Compose 行对象, 数据层反向依赖 UI 类型 |
| ARCH-3 | 中 | 分层只是 `:app` 内的包名约定 |
| ARCH-4 | 中 | 静态服务定位器与 Metro 图并存 |
| ARCH-5 | 中 | 设置存储的种子加载依赖一条看起来像死代码的语句 |
| ARCH-6 | 中 | `:base` 放着应用特有规则, 且没有任何声明依赖 |
| PATTERN-1 | 中 | 加载模板没有失败路径 |
| PATTERN-2 | 中 | Home 的 ViewModel 在做数据层工作, 仓库只是转发 |
| PATTERN-3 | 中 | Settings 功能绕开自己的仓库写数据 |
| PATTERN-4 | 中 | 页面把框架对象向下递给 ViewModel |
| PATTERN-5 | 中 | 各层的调度器策略不一致 |
| PATTERN-6 | 中 | 失败行处理有四种写法 |
| PATTERN-7 | 中 | 导航注册按标签页复制了四份 |
| PATTERN-8 | 低 | 两个 CompositionLocal 用静默的错误默认值 |
| PATTERN-9 | 低 | 给挂起函数标了 `@MainThread` |
| PATTERN-10 | 低 | 一个没被使用的 shell 实现在 `waitFor()` 之后读管道 |
| PATTERN-11 | 低 | 四个永远不会超过一个条目的返回栈 |
| MOD-1 | 中 | `ui/common` 是混杂的共享层, 却反向依赖功能包 |
| MOD-2 | 中 | 功能包之间循环 import |
| MOD-3 | 中 | 两个不相干的类共用 `FingerprintDataSource` 这个名字 |
| MOD-4 | 中 | 占位值规则没有唯一的持有者 |
| MOD-5 | 中 | 同一条 API 级别要求被编码了两次, 且不完全一致 |
| MOD-6 | 中 | `base` 包横跨两个模块; 一个文件身兼五职 |
| MOD-7 | 低 | 常量注册表放在了 DataSource 层 |
| MOD-8 | 低 | 各数据源的错误约定后缀混用 |
| MOD-9 | 低 | `:base` 会把放进 `libs` 的东西原样 `api` 出去 |
| SEC-1 | 中 | 拉回的 `lld.json` 没有状态码与完整性校验就被消费 |
| SEC-2 | 中 | 受信任的 `lld.json` 写在 Android 10 及以下其他应用够得到的地方 |
| SEC-3 | 中 | 载荷数据会改写所有 API 检查背后的版本事实 |
| SEC-4 | 低 | 载荷声明的 schema 从未被比对 |
| SEC-5 | note | 没有活的 shell 注入路径, 见 PATTERN-10 |
| SEC-6 | 低 | debug 构建把完整 HTTP 流量和 shell 输出写进 logcat |
| SEC-7 | 低 | 一处无条件的 release 日志可能带出文件内容 |
| SEC-8 | 低 | 备份规则是空元素, 什么都备份 |
| SEC-9 | 低 | debug 签名库进了版本库, 密码写死在构建脚本里 |
| SEC-10 | 低 | git 分支名未经过滤拼进生成源码和 URL |
| SEC-11 | 低 | 另一个 flavor 引入了统计与崩溃上报 |
| SEC-12 | 低 | 声明了 `QUERY_ALL_PACKAGES` 并压掉了两项 lint 检查 |
| CP-1 | 低 | 滚动指示器工厂在组合阶段读取滚动状态 |
| CP-3 | 低 | 每一行都是带空点击处理的可点击卡片 |
| CP-4 | 低 | 每张卡片都带尺寸动画节点 |
| CP-5 | 低 | 指示器淡出时每帧重绘整个内容 |
| AP-1 | 高 | 首屏列表要等完整条检测流水线 |
| AP-2 | 中 | Home 的检测项严格串行执行 |
| AP-3 | 中 | prop 页对整张 settings 表逐键反射 |
| AP-4 | 低 | `Application.onCreate` 里有同步的设置种子加载和反射 |
| AP-5 | 低 | 每次属性读取都重新解析反射方法 |
| AP-6 | 低 | 离线载荷每次加载被解析三遍 |
| AP-7 | 低 | 排序开关会重新扫描已安装应用列表 |

## 1. SSOT

**SSOT-1 (高)**: 版本事实的全局对象 `myAndroid` 用普通 `var` 字段持有设备的版本信息  
(`app/src/main/java/.../ui/common/AndroidVersionExt.kt:53-64`), 它有两个写入方: 启动时的  
`initMyAndroid()` (`app/src/main/java/.../base/MyApplication.kt:92` 调用, 写入在  
`AndroidVersionExt.kt:104-110`), 以及每次 Home 加载都会用载荷重写 `api`, `apiFull`,  
`version`, `dessert` 的 `HomeRepository.detectAndroid()`  
(`app/src/main/java/.../ui/home/repository/HomeRepository.kt:124-131`). 读取方遍布每个功能:  
`isAtLeastAndroid*()` 检查 (`AndroidVersionExt.kt:114-125`), `OthersViewModel.kt:44,78`,  
以及过时应用过滤 (`HomeRepository.kt:1130`). 第二个写入发生在  
`withContext(Dispatchers.IO)` 内 (`HomeViewModel.kt:211`), 而其他协程在没有同步的情况下读  
同一组字段, 所以一次 Home 加载和一次 Others 加载可能竞争同一个所有功能都当作常量的事实.

**SSOT-2 (中)**: Application 有两条可能分叉的取得路径. 它既被构造注入 (`LldFileStore.kt:27`),  
也经静态成员暴露 (`base/MyApplication.kt:73-85`: `instance`, `sharedPreferences`,  
`getMyString`), 而且有一个类把两种都用了: `LldFileStore` 在构造器里接收 `application`,  
方法体内却读 `MyApplication.instance.assets` (`LldFileStore.kt:61,76`). 图自己的  
`SharedPreferences` 提供者声明了 `app: MyApplication` 却忽略该参数, 返回静态伴生对象的值  
(`di/AppGraph.kt:206-207`), 于是这条被声明的依赖边只存在于签名里. 没有任何标记说明哪条路径  
才是权威.

**SSOT-3 (中)**: 同一行列表项按路径由两个数据源构建. 完整 Home 加载把内存里的载荷传给  
`getOutdatedTargetSdkVersionApkModel` (`HomeViewModel.kt:147,238-240`), 设置开关触发的补丁则  
重新读离线文件 (`HomeViewModel.kt:267-275` 调用 `:156-188` 的 `fetchOfflineLldOrNull`).  
这一行的颜色取决于收到哪个 `lld` (`HomeRepository.kt:1141-1149`), 只有  
`HomeViewModel.kt:136-145` 的保存成功时两边才一致. 保存一旦失败, 拨动排序开关就会用更旧的  
文件重画这一行, 而 ViewModel 没有任何 `lld` 状态记录这一行当前用的是哪份数据.

**SSOT-4 (低)**: 每个枚举型设置被物化为两条并行的流: 一条存原始字符串, 一条存解析后的枚举  
(`app/src/main/java/.../ui/settings/repository/SettingsStore.kt:50-64`). 配对逻辑写了两遍,  
构造时播种 (`SettingsStore.kt:119-134`) 一次, 偏好监听器 (`SettingsStore.kt:95-114`) 一次,  
而消费方按表示形式分开: 设置弹窗读原始字符串 (`SettingsScreen.kt:83-84`), 主题和列表页读枚举  
(`ui/MainActivity.kt:77`, `ui/theme/Theme.kt:306`, `MyModelListScreen.kt:64`). 一个无法识别的  
已存值会让这对流不一致 (原始流保留未知字符串, 枚举流说 `FollowSystem`), 每个新消费方都得知道  
该用哪一条.

**SSOT-5 (低)**: 一个展示用颜色被再次当作业务数据. `detectTrebleAndGsiCompatibility()` 从已  
构建行的颜色推导 "Treble 受支持": `trebleModel.color != StatusColor.CRITICAL`  
(`HomeRepository.kt:559-563`), 并把这个标志交给 `detectGsi` (`:565,570-597`). 检测失败经 Home  
的守卫也会得到 CRITICAL (`ui/base/list/MyModelExt.kt:46-50`), 于是 "检测失败" 和 "Treble 确实  
关闭" 对 GSI 来说是同一个输入. 以后改颜色映射会无声地改变 GSI 的结果.

**SSOT-6 (低)**: 当前标签页存在两份: 保存且未校验的 `currentTabIndex`, 以及钳制过的  
`selectedTabIndex` (`ui/AppRoot.kt:83-88`). 一旦保存的索引超过标签页数量 (标签页在后续版本里  
可能减少), 保存的那份会在整个进程存活期内越界, 而渲染用的是钳制后的那份; 两份在用户点标签页  
之前一直不一致, 且被重新保存的是那个过时值.

## 2. UDF

**UDF-1 (中)**: 一个事实由两条事件路径写入, 靠手写计数器协调. `OutdatedTargetSdkApk` 这一行  
由加载流水线写入 (`HomeViewModel.kt:52-65`), 又被 `settingsStore.outdatedOrderFirst` 的收集器  
就地补丁 (`HomeViewModel.kt:84-97` 调用 `:266-286` 的 `payloadOutdatedTargetSdkVersionApk`,  
经 `BaseListViewModel.updateModelDetail` 写回, `ui/base/list/BaseListViewModel.kt:79-87`).  
两个写入方由一个世代计数器协调 (`outdatedOrderChanges` 与 `loadStartGeneration`,  
`HomeViewModel.kt:78-82,258-264`), 这是在两条修改同一个 `StateFlow` 的方向之上手搭的同步;  
又因为补丁路径读的 `lld` 来源和加载路径不同 (SSOT-3), 两个方向能为同一行产出肉眼可见的不同  
内容.

**UDF-2 (中)**: 同类的两个设置被区别对待. 加载用 `first()` 读一次 `allowNetworkData`  
(`HomeViewModel.kt:58`) 之后不再观察它, 于是在设置页拨动开关后, Home 页显示的模式行仍按旧值  
算, 直到手动刷新; 而它的兄弟 `outdatedOrderFirst` 得到了完整的实时更新路径  
(`HomeViewModel.kt:84-97` 的收集器加上 `:258-264` 的加载完成核对). 两个开关嵌进同一张列表,  
但谁有实时同步无法从数据形状推出来.

**UDF-3 (低)**: 全应用设置同时经两条通道到达页面. 设置的流在根部作为 CompositionLocal 提供  
(`ui/MainActivity.kt:75-79`), 页面里直接收集 (`MyModelListScreen.kt:64`,  
`SettingsScreen.kt:96`), 同一批值又经 ViewModel 再暴露一次 (`SettingsScreen.kt:83-86`).  
滚动条设置在同一帧以两种类型两次到达 `SettingsContent`: 经 ViewModel 的原始字符串  
(`SettingsScreen.kt:100`) 和经 CompositionLocal 的解析枚举 (`SettingsScreen.kt:101`). 事件始终  
是 ViewModel 到设置存储, 但状态的送达要在两处靠手工保持一致.

**UDF-4 (低)**: 版本行彩蛋建模成一个有副作用的取值函数. `getVersionClickedMessage()` 看似  
查询, 却递减隐藏的 `timesLeft` 状态 (`ui/settings/SettingsViewModel.kt:105-120`), 页面把返回值  
变成 toast (`SettingsScreen.kt:117-121`). 一次点击的效果依赖没有任何流建模的 ViewModel 历史,  
而且普通 `var` 不会在进程死亡后保存.

## 3. 架构

另有两条同样属于本角度的发现按主题列在别处: 全局版本事实在 SSOT-1, 功能包之间的循环 import  
在 MOD-2.

**ARCH-1 (中)**: 一个仓库包揽整个 Home 功能.  
`app/src/main/java/.../ui/home/repository/HomeRepository.kt` 全文 1211 行, 带有二十多个  
`detect*` 方法 (索引 `:74-1117`), WebView provider 的私有 API 反射 (`:1045-1111`),  
已安装应用枚举 (`:1121-1131`), 格式化辅助函数 (`:1180-1202`), 文件转发 (`:67-72`),  
以及 SSOT-1 里的全局写入 (`:124-131`). 检测, 比较, 错误格式化, 展示拼装共处一个文件,  
于是每个新检测项都要动同一个类, 读系统和渲染行之间没有任何边界.

**ARCH-2 (中)**: 仓库构建 Compose 面向的行对象, 在仓库边界上把自称的 Screen 到 ViewModel 到  
Repository 到 DataSource 顺序反转了. `HomeRepository` 引入 `ui.base.list` 的辅助函数和  
`ui.theme.StatusColor` (`HomeRepository.kt:23-27,50`), 并用 `@StringRes` 标题构造 `MyModel`  
(`:1172-1177`); `ui/prop/repository/PropRepository.kt:7-11` 一样. 数据层无法在没有展示类型  
(`ui/base/list/MyModel.kt`, `ui/theme/ExtendedColors.kt`) 的情况下编译, 输出的是最终展示字符串  
而不是数据.

**ARCH-3 (中)**: 分层只是同一个包树里的命名约定, 不是结构. 每个仓库和数据源都在  
`net.imknown.android.forefrontinfo.ui.*` 之下, 例如 `ui/home/repository/HomeRepository.kt:1`,  
`ui/prop/datasource/SettingsDataSource.kt:1`, `ui/settings/repository/SettingsStore.kt:1`,  
而没有任何模块或 Gradle 边界分开 Screen, ViewModel, 仓库与数据源. 这就是数据层文件能引入  
Compose 辅助函数, ViewModel 能直接调用数据源的原因 (PATTERN-2, PATTERN-4): 构建里没有任何东西  
阻止它们.

**ARCH-4 (中)**: 静态服务定位器 (经静态访问取得依赖的模式) 与 Metro 图并存, 且通常压过图.  
`MyApplication` 的伴生对象暴露 `instance`, `sharedPreferences` 和 `getMyString`  
(`base/MyApplication.kt:73-85`), ViewModel 和仓库里的使用例如 `HomeViewModel.kt:194,250`,  
`ui/others/OthersViewModel.kt:90`, `ui/prop/repository/PropRepository.kt:54`. 注入和环境静态访问  
甚至在同一个类里混用 (`LldFileStore.kt:27` 对 `:61,76`), 所以 `di/AppGraph.kt` 只解出应用实际  
使用的一部分对象图.

**ARCH-5 (中)**: 启动正确性依赖一条看起来像死代码的语句. 首帧之前构造设置存储依赖  
`MyApplication.onCreate` 里的 `val _ = appGraph.settingsStore` (`base/MyApplication.kt:103`)  
先于 `MainActivity` 组合前的读取执行 (`ui/MainActivity.kt:26-28,89-92`); 读者无法从那行字看出  
它是承重的. `SharedPreferences` 提供者那个没用上的 `app` 参数是 SSOT-2 记录的事实.

**ARCH-6 (中)**: `:base` 在最底层放着应用特有的规则.  
`base/src/main/java/.../base/extension/DateTimeExt.kt:9-20` 编码了 `lld.json` 的时间戳格式,  
`DateTimeExt.kt:35` 编码了应用的镜像选择规则 (中国大陆时区), 由  
`ui/home/datasource/LldDataSource.kt:35` 消费. 该模块自己声明的依赖为零  
(`base/build.gradle.kts:13-15`), 所以这些规则位于它们的使用者之下, 而不是被使用者拉下去.

## 4. 设计模式

**PATTERN-1 (中)**: 加载模板没有失败路径. `startLoad()` 在一个 `launch` 里依次  
`setLoading(true)`, `collectModels()`, `setModels(list)`, 没有 try/finally  
(`ui/base/list/BaseListViewModel.kt:60-71`), 而 `setLoading(false)` 只经 `setModels` 可达  
(`:89-93`). 子类加载一旦抛出, 就没有错误状态地死掉 (`MyModelListScreen.kt:71-72` 仍显示加载  
标志), 异常直达协程的未捕获处理. 今天每个检测项各自守卫自己的行 (PATTERN-6), 所以基类暗中  
依赖自身之外的约定; `detect()` 里一个没有守卫的调用, 例如 `getMounts()`  
(`HomeViewModel.kt:221-223`), 就足以触发它.

**PATTERN-2 (中)**: Home 的 ViewModel 在做数据层工作, 它下面的仓库只是转发. ViewModel 负责  
取回 (`HomeViewModel.kt:101-111`), 解码 JSON (`:113-122`), 写文件 (`:136-145`), 读打包资源  
(`:192-203`), 带着六个 `catch (e: Exception)` 块 (`:108,119,143,163,179,198`) 加字符串格式化  
(`:246-251`); 这部分的 `HomeRepository` 只做转发 (`HomeRepository.kt:67-72`). 检测部分正相反:  
仓库才是功能的主体 (ARCH-1). Screen 到 ViewModel 到 Repository 到 DataSource 的链条名义上  
成立, 实际两端各做对方的工作.

**PATTERN-3 (中)**: Settings 功能绕开自己的仓库写数据. ViewModel 经四个 setter 直接写  
`SettingsStore` (`ui/settings/SettingsViewModel.kt:46,58,72,76`), 而 `SettingsRepository` 只  
服务一个调用, `getBuiltInDataVersion` (`SettingsViewModel.kt:99`), 连这个也是页面触发的  
(`ui/settings/SettingsScreen.kt:91`). 这个功能的仓库层只覆盖了它操作中的一项, 而一个仓库层的类  
(`SettingsStore`) 被 ViewModel, `ui/MainActivity.kt:26-28` 和依赖图 (`di/AppGraph.kt:71`)  
直接消费.

**PATTERN-4 (中)**: 页面把框架对象向下递给各层, 而不是由下层自己解析. `SettingsScreen` 把  
`context.packageManager` 和 `context.packageName` 传进 ViewModel (`SettingsScreen.kt:91` 到  
`SettingsViewModel.kt:89` 再到 `SettingsRepository.kt:27`), `OthersViewModel` 把  
`MyApplication.instance` 传进它的仓库 (`ui/others/OthersViewModel.kt:90`). 展示层决定数据层  
收到哪些 Android 服务, `OthersViewModel` 还直接引入了一个数据源常量 (`OthersViewModel.kt:16,55`  
的 `ArchitectureDataSource.DRIVER_BINDER`).

**PATTERN-5 (中)**: 各层之间没有一致的线程约定. 有的调用方把仓库调用包在 `Dispatchers.IO` 里  
(`ui/others/OthersViewModel.kt:34`, `ui/prop/PropViewModel.kt:31`, `HomeViewModel.kt:211`,  
其中 `:221` 还嵌套了一个多余的 `withContext(Dispatchers.IO)`), `SettingsRepository` 在单个  
方法里做了四次调度器切换 (`SettingsRepository.kt:31,39,52,74`), 而仓库本身是没有声明策略的  
普通阻塞函数 (例如 `HomeRepository.kt:1121-1131`,  
`ui/prop/repository/PropRepository.kt:50-63`). 每个调用方必须逐个仓库知道直接调用它是否安全.

**PATTERN-6 (中)**: 同一个 "降级成错误行" 的行为有四种写法. 共享守卫在  
`ui/base/list/MyModelExt.kt` 里构建失败行 (Home 的红色行在 `:46-50`, Others/Prop 的无色行在  
`:95-98`, 公共骨架 `:149-161`); `PropRepository` 自己写 try/catch 直接构建失败行  
(`ui/prop/repository/PropRepository.kt:41-48,57-62`); `OthersRepository` 手工用 catch 加  
`printStackTrace` 包内部操作  
(`ui/others/repository/OthersRepository.kt:87-88,118-119,132-133,246-247`); `SettingsRepository`  
用 catch 加 `Log.w`, 用一个默认值顶替失败的读取, 没有守卫  
(`ui/settings/repository/SettingsRepository.kt:42-43,55-56,77-78`). 失败行的消息文本本身还构建  
了两遍 (`MyModelExt.kt:48` 和 `:97`), 所以改失败策略要在每种写法里各找一遍.

**PATTERN-7 (中)**: 导航注册按标签页复制. 四个标签页各自构建一个包含全部四个 `entry<...>`  
注册的 `entryProvider` (`ui/AppRoot.kt:103-116`), 于是运行时共 16 个注册 (4 个 provider 各含  
4 个), 其中 12 个永远不会被解析, 加一个目的地意味着改四份同样的代码块. ViewModel 的获取也在  
每份里原样内联 (`AppRoot.kt:110-113`).

**PATTERN-8 (低)**: 两个 CompositionLocal 用静默的错误默认值而不是直接失败.  
`LocalScrollBarMode` 默认为 `None` 流 (`ui/base/list/MyModelListScreen.kt:39-40`),  
`LocalThemeMode` 默认为跟随系统 (`ui/theme/Theme.kt:293-294`); 在 `MainActivity` 的提供者  
(`ui/MainActivity.kt:75-79`) 之外渲染的组合函数会安静地显示默认值, 而不是在缺失 provide 时失败.

**PATTERN-9 (低)**: 给一个会切换调度器的挂起函数标了 `@MainThread`:  
`payloadOutdatedTargetSdkVersionApk` (`HomeViewModel.kt:266-271`) 在 `:271` 进入  
`withContext(Dispatchers.IO)`. 该注解无法跨越挂起点成立, 它声明的线程恰恰是这个函数按设计  
会离开的.

**PATTERN-10 (低)**: 留作备用的 `:base` shell 实现在源码里没有调用方 (在用的绑定是  
`ui/common/ShellLibSu.kt:15-20`, 在 `HomeRepository.kt:59-60` 注入), 它的实现在 `waitFor()`  
之后才读管道输出 (`base/src/main/java/.../base/shell/impl/ShellDefault.kt:14-21`), 一旦命令  
输出超过管道缓冲区就会永久阻塞. 它还把参数拼进 `sh -c` (`ShellDefault.kt:14`), 所以以后启用  
它而不逐个复查调用方就会制造 shell 注入路径; 现有调用点传的都是编译期常量 (见 SEC-5).

**PATTERN-11 (低)**: 装配了四个带装饰器的返回栈, 但每个栈只可能持有自己的 key: 每个标签页一个  
`rememberNavBackStack(tab.key)` (`ui/AppRoot.kt:92-100`), 每个 key 一条注册  
(`ui/navigation/NavKeys.kt:11-20`, `AppRoot.kt:110-113`), 且没有任何东西会压入第二个条目.  
返回也完全不遍历栈: `onBack = { activity?.finish() }` (`AppRoot.kt:124`). saveable 与  
ViewModelStore 机制按标签页付费, 而它的遍历行为从未被使用.

## 5. 模块化

**MOD-1 (中)**: `ui/common` 是一个混杂的共享层, 既服务各功能又依赖各功能. 它把数据基础设施  
(`ui/common/PropertyReader.kt`, `ui/common/ShellLibSu.kt`, `ui/common/JsonExt.kt:6-12`),  
进程级版本状态 (`ui/common/AndroidVersionExt.kt:53-110`) 和 Compose 绘制  
(`ui/common/ScrollBarExt.kt:36-89`) 混在一个包里, 同时引入一个功能包: `AndroidVersionExt.kt:11`  
引入 `ui.home.model.Lld`. 几乎所有东西都依赖这个包 (例如 `di/AppGraph.kt:22`,  
`HomeRepository.kt:28-43`), 所以这个共享层没有单一职责, 它自己的 import 又跑回它本应位于其下的  
功能里.

**MOD-2 (中)**: 功能包互相 import, 形成环. home 与 settings 互引: `HomeRepository.kt:49` 引入  
`ui.settings.datasource.AppInfoDataSource`, 而 `ui/settings/repository/SettingsRepository.kt:14`  
引入 `ui.home.datasource.LldFileStore`. common 与 home 互引: `AndroidVersionExt.kt:11` 引入  
`ui.home.model.Lld`, 而 `HomeRepository.kt:28-43` 引入 `ui.common.*`. others 到 home:  
`ui/others/datasource/BasicDataSource.kt:7` 引入 `ui.home.datasource.AndroidDataSource`.  
共享对象的所属功能由来源偶然决定, 于是没有任何功能包能被单独抽出或单独推理.

**MOD-3 (中)**: 两个不相干的类共用一个简单类名:  
`ui/others/datasource/FingerprintDataSource.kt` 从属性读取 build 与分区指纹 (`:13-24`),  
而 `ui/settings/datasource/FingerprintDataSource.kt` 计算签名证书 SHA-256 并映射到发行方  
(`:27,53`). 读者必须靠 import 解析才知道指的是哪个类, 搜索这个类名会同时命中两个.

**MOD-4 (中)**: 占位值规则没有唯一的持有者. 一个值算不算 "没填" 在 `HomeRepository.kt:1181-1184`  
和 `ui/others/repository/OthersRepository.kt:219-220` 各自独立比较 (两处都硬编码了  
`result_not_supported` 与 `build_not_filled`), 空输入在 `MyModelExt.kt:163-167` 被替换成占位值,  
`ui/common/PropertyReader.kt:25-28` 又把同样两个字符串解析成默认值. 改什么算占位值, 要在两个  
功能和共享辅助函数里把每处都找齐.

**MOD-5 (中)**: 同一条 API 级别要求在不同层被编码了两次, 而且不完全一致. `OthersViewModel` 用  
`isAtLeastAndroid12()` 守卫 SoC 与 SKU 的调用 (`ui/others/OthersViewModel.kt:44-50`), 而它  
到达的五个方法里有四个标了 `@RequiresApi(Build.VERSION_CODES.S)`  
(`ui/others/repository/OthersRepository.kt:61,65,69,76`); `getVendorSku` (`:73`) 在守卫块里  
却没有注解. 两份编码可以各自被修改, 且 ViewModel 侧的检查读的是 SSOT-1 里可变的  
`myAndroid.api`, 而不只是 `Build.VERSION.SDK_INT`.

**MOD-6 (中)**: `base` 包同时存在于两个模块: `app/src/main/java/.../base/MyApplication.kt:1` 在  
`:app` 里声明包名 `net.imknown.android.forefrontinfo.base`, 而 `:base` 模块拥有同一个  
命名空间根 (`base/build.gradle.kts:6`). 那一个应用文件还身兼五职: 依赖图构造 (`:58`),  
目录管理 (`:60-71`), 静态伴生对象 (`:73-85`), 启动编排 (`:87-106`) 和 libsu 配置 (`:108-116`).

**MOD-7 (低)**: `AndroidDataSource` 是常量注册表, 不是数据源: 类体是一个只有 `const val` 的  
伴生对象, 没有状态也没有数据访问 (`ui/home/datasource/AndroidDataSource.kt:3-145`), 却坐在  
DataSource 层并被当作数据源引入 (`ui/others/datasource/BasicDataSource.kt:7` 和  
`HomeRepository.kt:44`). 其他功能的属性与命令键被存在 home 包下.

**MOD-8 (低)**: 错误约定后缀在各数据源之间混用, 没有成文规则:  
`ui/settings/datasource/FingerprintDataSource.kt:27` 是 `getPublicKeySha256OrNullOrThrow`,  
它的同级 `:53` (`getDistributor`) 和整个 `ui/others/datasource/FingerprintDataSource.kt:13-24`  
则没有后缀, `LldFileStore.kt:111-120` 的 `getAssetLld` (返回 null) 又挨着它的 `*OrThrow` 同级  
(`:32,42,101`). 调用方能依赖的后缀无法从名字本身预判.

**MOD-9 (低)**: `:base` 把放进它 `libs` 目录的东西原样 `api` 出去: `base/build.gradle.kts:14` 是  
`api(fileTree("libs") { include("*.jar", "*.aar") })`, 构建里唯一的 `api()`. 目录今天是空的,  
但放进去一个 jar 就会不经目录条目直达每个使用者的编译 classpath, 绕过其他地方都在用的版本目录  
约束.

## 6. Security

**SEC-1 (中)**: 拉回的载荷在没有真实性和完整性校验的情况下被消费.  
`fetchOnlineLldJsonStringOrThrow` 不检查状态码直接返回 `response.body()`  
(`ui/home/datasource/LldDataSource.kt:41-47`), `HttpClient` 绑定只配置了超时, 代理选择器和  
debug 日志 (`di/AppGraph.kt:116-182`), 载荷被保存和使用之前唯一的校验是 `version` 的时间戳  
格式 (`HomeViewModel.kt:128-134`). 这份数据决定安全相关的行 (安全补丁比较在  
`HomeRepository.kt:273`, targetSdk 阈值在 `:1130`, 以及同一文件里的 WebView 结论), 所以控制  
响应或镜像主机的一方能让应用报告一个更健康的设备, 而伪造的文件还会留存下来供离线使用  
(`HomeViewModel.kt:136-145`).

**SEC-2 (中)**: 受信任的文件写在应用专属外部存储而不是内部存储: `getDownloadDir()` 解析到  
`getExternalFilesDir(...)`, 内部目录是备选 (`base/MyApplication.kt:60-71`), `LldFileStore` 在  
那里用 `FileWriter` 写入, 之后又把这个文件当受信任输入解析回来  
(`ui/home/datasource/LldFileStore.kt:32,101-109,45-51`). 在 Android 10 及以下的设备上, 持有  
存储权限的另一个应用能碰到这条路径, 所以一个格式完好, `version` 比打包资源更新的被篡改文件会  
被安静地接受 (`LldFileStore.kt:58-69`). 两个细节扩大了这个暴露面: 路径若以目录形式存在会被  
递归删除 (`LldFileStore.kt:34-40,102-104`), 且 `FileWriter` 会跟随已存在的符号链接.

**SEC-3 (中)**: 载荷数据改写所有 API 检查背后的事实, 伪造条目能让应用崩溃. `detectAndroid()`  
把载荷的 `api` 字段拷进全局 `myAndroid` (`HomeRepository.kt:124-131`, 在 `:107-109` 按  
`apiFull` 匹配), 而 `isAtLeastAndroid*()` 检查读这个值 (`AndroidVersionExt.kt:114-125`).  
检测顺序把这个写入排在 `detectSdkExtension` 和 `detectPerformanceClass` 之前  
(`HomeViewModel.kt:214-218`): 一个 `apiFull` 匹配但 `api` 被抬高的条目, 会在缺少这些 API 的  
硬件上通过 `isAtLeastAndroid11()` (`HomeRepository.kt:178-179` 到  
`AndroidVersionExt.kt:144` 的 `SdkExtensions.getExtensionVersion`) 或 `isAtLeastAndroid12()`  
(`HomeRepository.kt:318-319` 到 `Build.VERSION.MEDIA_PERFORMANCE_CLASS`). 随之而来的  
`NoSuchMethodError` 与 `NoSuchFieldError` 是 `Error`, 行守卫抓不住 (它只抓 `Exception`,  
`MyModelExt.kt:156`), 而加载没有处理器 (PATTERN-1), 于是直达未捕获处理. 同一个值也是 targetSdk  
阈值 (`HomeRepository.kt:1130`). 利用它需要控制载荷 (SEC-1) 或控制该文件 (SEC-2).

**SEC-4 (低)**: 载荷声明的 schema 从未被比对. `Lld.kt:8` 定义 `SCHEME_VERSION`, `Lld.kt:13`  
反序列化 `scheme`, 但没有任何代码比较两者 (全仓搜索只有声明处), 而  
`Json { ignoreUnknownKeys = true }` (`ui/common/JsonExt.kt:6`) 把未知字段丢弃而不是拒绝.  
声明了不同 schema 的载荷会被当作当前 schema 消费, 唯一能说明这份解释有错的信号被丢掉了.

**SEC-5 (note)**: 不存在活的注入路径. 观察到的每个 `IShell.execute` 调用点传的都是编译期常量  
(例如 `HomeRepository.kt:578`, `ui/prop/datasource/PropertiesDataSource.kt`), 树里唯一形如  
`sh -c` 的汇点是 PATTERN-10 里那个没人调用的实现.

**SEC-6 (低)**: debug 构建把完整 HTTP 流量和 shell 输出写进 logcat. 只要 `BuildConfig.DEBUG`  
为真, Ktor 的 `Logging` 插件就装上 `LogLevel.ALL` (`di/AppGraph.kt:167-172`), 请求头和整个  
`lld.json` 正文都会进 logcat, libsu 的详细日志在同一条件下开启 (`base/MyApplication.kt:109`).  
两者都只限于可调试构建; 这条记录的是: 这个应用的 debug 构建会在 logcat 里带上系统属性和指纹  
输出.

**SEC-7 (低)**: 一处 release 路径上的日志可能带出文件内容. 已存文件的解析失败被无条件记录, 带上  
异常的完整消息 (`LldFileStore.kt:50-55`), 而 kotlinx.serialization 的错误携带输入的偏移和摘录,  
于是落在那个存储文件里的内容片段会写进生产构建的 logcat; 邻近的路径是有 `BuildConfig.DEBUG`  
守卫的 (`HomeViewModel.kt:199-201,247-249`).

**SEC-8 (低)**: 备份规则是空元素, 于是什么都默认备份. `android:allowBackup` 从未设置  
(`app/src/main/AndroidManifest.xml:12-14`), `android:fullBackupContent` 指向  
`<full-backup-content />` (`base/resBackup/xml/backup_rules.xml:5`),  
`android:dataExtractionRules` 指向 `<data-extraction-rules />`  
(`base/resBackup/xml/data_extraction_rules.xml:4`), SharedPreferences 文件和已存的 `lld.json`  
会在没有任何排除的情况下离开设备. 今天存的只是界面偏好 (`SettingsStore.kt:33-45`); 问题在于  
以后加入的任何类凭据键会被自动包含, 没有规则可审.

**SEC-9 (低)**: debug 签名库进了版本库, 密码写死在构建脚本里: `keys/debug.keystore` 被 git  
跟踪, `app/build.gradle.kts:74-77` 硬编码了签名库文件, 别名和密码, 而 release 配置从  
`local.properties` 读同样的属性 (`app/build.gradle.kts:53-65`). 影响有限, 因为 debug 构建类型  
追加了 `applicationIdSuffix` (`app/build.gradle.kts:99,105`), 这类 APK 不会盖装到 release  
安装上; 发现点是版本管理里的签名材料, 以及紧挨着 release 配置的硬编码密钥写法.

**SEC-10 (低)**: git 分支名未经处理被拼进生成源码和 URL:  
`buildConfigField("String", "GIT_BRANCH", "\"$currentGitBranchName\"")` 在  
`build-logic/convention/src/main/kotlin/.../android/app/AndroidApplicationConventionPlugin.kt:37-44`  
把 `git rev-parse --abbrev-ref HEAD` 的原始输出拼进生成的字符串字面量 (git 的引用名允许 `"`  
和大多数其他字符), 同一个字符串还被拼进下载 URL 路径 (`ui/home/datasource/LldDataSource.kt:41`).  
暴露面仅限于从这种检出构建的人, 所以这是卫生问题而不是可利用的问题.

**SEC-11 (低)**: 另一个 flavor 引入了离设备的遥测. Firebase 约定插件应用 `google-services` 和  
`firebase-crashlytics`, 并加入统计与带原生符号上传的崩溃上报  
(`build-logic/convention/src/main/kotlin/.../android/app/AndroidApplicationFirebaseConventionPlugin.kt:20-44`),  
它为 Foss flavor 关闭了 Google services 和 Crashlytics 任务 (`:52-70`), 所以默认构建不受  
影响. 给负责人的提示: 同一模块里存在一个启用遥测的变体, 值得一个明确的决定.

**SEC-12 (低)**: `QUERY_ALL_PACKAGES` 是一项用于枚举所有已安装应用的常驻能力. 该权限在声明时  
压掉了两项 lint 检查 (`app/src/main/AndroidManifest.xml:6-8`), `getInstalledApplications` 为  
过时 targetSdk 行返回完整清单 (`HomeRepository.kt:1121-1131`). 应用自己的 HTTP 客户端只发出  
`lld.json` 这一个 GET (`LldDataSource.kt:42`), Firebase flavor 的遥测是另一条路径 (SEC-11),  
这份清单也没有分享或导出通道, 所以风险是这项安装期能力本身, 而不是观察到的外泄.

## 7. Compose 性能

**CP-1 (低)**: 滚动指示器的修饰符工厂在组合阶段读取滚动状态.  
`Modifier.nonInteractiveScrollbar` 是一个 `@Composable` 工厂, 其函数体读  
`scrollState.isScrollInProgress` (`ui/common/ScrollBarExt.kt:41`) 并由此导出  
`animateFloatAsState` 的目标 (`:42-51`); 它在页面级修饰符链上被实例化  
(`MyModelListScreen.kt:118`, `SettingsScreen.kt:160`), 于是滚动会话的起止 (拖动开始, 惯性结束)  
会在该工厂里安排组合工作, 并为 `LazyColumn` 产出一条新的修饰符链, 而不是只更新绘制阶段.  
默认安装上该设置是 `ScrollBarMode.None`, `enabled &&` 在读取之前短路 (`ScrollBarExt.kt:41`),  
所以代价只在滚动条模式为 Normal 时出现. 每帧部分 (`alpha`, 滑块几何) 正确地位于  
`drawWithContent` 内 (`:56-88`).

**CP-3 (低)**: 每一行都是带空处理函数的可点击卡片. `Card(onClick = {})`  
(`ui/base/list/MyModelCard.kt:42-49`) 让每个已组合的行分配交互源, 指示, 输入和语义节点,  
按下时什么也不做, 行还会宣告自己可被激活而没有任何效果. 只有可见行会被组合, 所以量级不大;  
代价在长列表里按行重复.

**CP-4 (低)**: 每张卡片都带一个尺寸动画节点: 每行的 `.animateContentSize()`  
(`MyModelCard.kt:55`). 一行的 detail 变化时 (就地补丁 `BaseListViewModel.kt:79-87`), 该行在  
动画持续期间每帧重新测量; 受影响的行包括最长的那些 (`HomeRepository.kt` 里构建的 WebView 与  
过时应用详情).

**CP-5 (低)**: 指示器淡出时每帧重绘整个内容. 滑块淡出期间 `alpha` 每帧都在变, 且在  
`drawWithContent` 内被读取 (`ScrollBarExt.kt:58,84`), 于是包含无条件 `drawContent()` 的整个  
lambda 每帧重跑, 没有图层缓存列表的绘制命令. 只有 `ScrollBarMode.drawsScrollBar` 为真时可达  
(`SettingsStore.kt:60-64` 把默认值播种为 None), 所以默认安装上不会触发.

## 8. App 性能

**AP-1 (高)**: 首屏列表要等完整条检测流水线. `modelsStateFlow` 初始为 `null`, 只在  
`collectModels()` 返回后被设置 (`BaseListViewModel.kt:26-27,65-70`), 而页面渲染  
`models ?: persistentListOf()` 并在加载标志下转圈 (`MyModelListScreen.kt:71-72`). Home 是第一个  
标签页 (`ui/AppRoot.kt:62-67`), 它的加载先做只受 `HTTP_REQUEST_TIMEOUT_MILLIS` 约束的网络取回  
(`di/AppGraph.kt:101,177-181`, `HomeViewModel.kt:101-148`) 或离线文件链, 然后跑完所有检测项,  
一行才会出现, 所以冷启动是空列表加加载指示器, 撑满整个往返加检测的时间.

**AP-2 (中)**: Home 的检测项严格串行. 所有检测调用按语句顺序在同一个  
`withContext(Dispatchers.IO)` 协程里执行 (`HomeViewModel.kt:211-241`), 所以延迟是每个检测项  
之和: 阻塞的 shell 命令 (例如 `HomeRepository.kt:579` 经同步的 `Shell.cmd(cmd).exec()`,  
`ui/common/ShellLibSu.kt:17-20`), 应用枚举 (`HomeRepository.kt:1121-1131`), 每次加载都重做的  
WebView provider 反射 (`:1045-1111`) 和文件检查, 整套在每次下拉刷新时全部重跑. 检测项之间并非  
彼此独立: `detectAndroid` 在 `HomeViewModel.kt:214` 写入的全局值, 其后 `:215` 与 `:218` 的检测  
经 `isAtLeastAndroid*()` 读到它 (见 SSOT-1 与 SEC-3), 所以要并发化得先解开这层耦合.

**AP-3 (中)**: prop 页对整张 settings 表逐键反射. `PropViewModel` 依次查询 `Settings.System`,  
`Settings.Secure`, `Settings.Global` 和 `getprop` (`ui/prop/PropViewModel.kt:31-39`); 每张表的  
每个键, `SettingsDataSource` 都解析一次 `getDeclaredMethod("getString", ...)` 并调用它  
(`ui/prop/datasource/SettingsDataSource.kt:20-26`) 而不是直接静态调用, 键的清点则每次调用都  
重新反射 `declaredFields`, 标记可访问并排序 (`:11-18`). 每个键的主要成本仍是底层的框架查询;  
反射在上面又加了一次方法查找, 而整套行在每次刷新时从零重建.

**AP-4 (低)**: 主线程上有一批同步工作排在首帧之前的 `Application.onCreate` 里: DI 图作为字段  
初始化器被构建 (`base/MyApplication.kt:58`), `val _ = appGraph.settingsStore` (`:103`) 构造  
存储, 它同步解析字符串资源并读取 SharedPreferences, 还可能有一次迁移写入  
(`SettingsStore.kt:116-143`; SharedPreferences 首次访问时加载它的 XML 文件), `initMyAndroid()`  
(`:92`) 对版本码字段做一次反射清点 (`AndroidVersionExt.kt:66-110`). 每一块都不大, 但它们都在  
通往首帧的关键路径上.

**AP-5 (低)**: 属性读取每次都重新解析反射方法. `PropertyDefault` 缓存了类, 但每次 `getString`  
和 `getBoolean` 都调用 `getDeclaredMethod`  
(`base/src/main/java/.../base/property/impl/PropertyDefault.kt:10-18`), 一次 Home 加载大约要做  
二十次这样的读取 (`ui/common/PropertyReader.kt`). 观察到的调用方都在主线程之外运行, 所以这是 CPU  
浪费而不是卡顿; 一个缓存的 `Method` 字段就能去掉它.

**AP-6 (低)**: 离线载荷每次加载被解析三遍: 一遍读已存文件的 `version` (`LldFileStore.kt:50-51`),  
一遍读打包资源的版本 (`:61` 配 `:111-122`), 一遍供实际使用 (`HomeViewModel.kt:171-174`),  
每遍都是完整读取加解码, 因为 `toObjectOrThrow` 解析整段文本 (`ui/common/JsonExt.kt:8-12`).  
文件今天都很小, 但排序开关的补丁跑同一条链时次数还会翻倍.

**AP-7 (低)**: 拨动排序开关会重新扫描已安装应用列表. `outdatedOrderFirst` 的收集器在有列表显示时  
启动 `payloadOutdatedTargetSdkVersionApk()` (`HomeViewModel.kt:84-97,266-286`), 它重跑离线取回  
与解析链, 再跑 `getOutdatedTargetSdkVersionApkModel`, 又是一次完整的  
`getInstalledApplications` 加排序和字符串拼装 (`HomeRepository.kt:1121-1176`). 它在 IO 上为一次  
少有的用户操作运行, 所以这是值得知道的成本而不是瓶颈.

## 检查过, 没有发现的问题

- 阻塞的 shell, 属性和包工作在观察到的每个仓库调用点都在 `Dispatchers.IO` 之下  
  (`HomeViewModel.kt:211`, `OthersViewModel.kt:34`, `PropViewModel.kt:31`,  
  `SettingsRepository.kt:31`); 协程都活在 `viewModelScope` 里.
- 流的读取使用 `collectAsStateWithLifecycle` (`MyModelListScreen.kt:53-54`,  
  `SettingsScreen.kt:83-86`), `SettingsStore` 的监听器是进程存活期的且被存储强持有  
  (`SettingsStore.kt:95-142`).
- `HttpClient` 不持有 `Context` (`di/AppGraph.kt:116-182`); URL 是固定的 HTTPS  
  (`LldDataSource.kt:41`), 用系统信任, 没有自定义 `TrustManager`.
- 懒布局是单层的, 带 `key` 和 `contentType` (`MyModelListScreen.kt:130-132`), 位移动画经  
  `animateItem` (`:141`).
- 切换标签页不会重跑检测: 每个标签页的 ViewModel 和已缓存的列表都存活  
  (`AppRoot.kt:92-116`, `BaseListViewModel.kt:46-54`).
- 只有一个导出组件, 没有深度链接或 Intent 处理 (`app/src/main/AndroidManifest.xml:23-31`),  
  应用里没有 WebView.
- SharedPreferences 是 `MODE_PRIVATE` (`base/MyApplication.kt:76-78`), 只存设置  
  (`SettingsStore.kt:33-45`); 树里没有令牌或账号数据, 唯一的签名材料是 SEC-9 提到的 debug 签名库.
- 反射是只读的 (没有对隐藏 API 的写入), `PropertyDefault` 调用 `SystemProperties.get` 时没有  
  `setAccessible` (`base/src/main/java/.../base/property/impl/PropertyDefault.kt:10-18`).
