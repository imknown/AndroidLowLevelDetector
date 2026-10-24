# 项目分析报告 (qwen-3.8-flash)

> **Status**: **record**. 2026-10-18 对代码的一次时点阅读, 文本保留当时写下的内容, 后续发现可以追加进来;  
> 其中的建议句不构成今天的规则, 代码往后会变, 本报告不会随之更新, 引用它是为了追溯当时看到了什么.

分析范围与取材方式:

- 判定的事实依据是代码与构建文件, 逐文件读完 `app` `base` `binderDetector` `build-logic` 的 Kotlin 源码,  
  `app/build.gradle.kts` `base/build.gradle.kts` `settings.gradle.kts` `gradle/toml/*.toml`  
  `gradle.properties` `AndroidManifest.xml` `app/lint-baseline.xml` `resBackup/xml/*.xml`  
  `res/values/arrays.xml` `app/src/main/assets/lld.json`. Test 一节是例外: 它同时按官方测试文档逐页对照,  
  那几条都写了文档出处.
- 按要求未读 `docs/dev/` 与 `docs/spec/` 的任何内容; 未把代码注释当作事实, 注释里对行为的断言一律重新在代码里核对过,  
  因此下面的判定与注释写的意图无关. `AGENTS.md` 只作为 "负责人定过的约定" 来对照代码.
- 不含 CI, NDK, JNI 三个角度.
- 本报告自己的 ID 形如 `SSOT-01`, 只在本文内部互指, 不进入仓库的 `issues-cn` 编号序列.

一句话结论: 项目的真问题集中在 "一件事有多个来源" 和 "数据层与 UI 层互相穿插" 这两条上, 由此带出检测结论会随构建环境, 语言与调用时序交错变化的风险;  
测试侧只有一个真实测试文件, 且 androidTest 的 runner 依赖缺失, 所以现在任何回归都只能靠人眼发现.

| 一览 | 内容 |
|---|---|
| SSOT | 6 条: SSOT-01..06 |
| UDF | 7 条: UDF-01..07 |
| 架构 | 8 条: ARC-01..08 |
| 设计模式 | 6 条: PAT-01..06 |
| 模块化 | 7 条: MOD-01..07 |
| Security | 9 条: SEC-01..09 |
| Compose 性能 | 8 条: CP-01..08 |
| App 性能 | 9 条: AP-01..09 |
| Test | 21 条: TST-01..21 |

评级只用来排先后, 不用来决定某个问题可以修得多浅.

## SSOT, 同一个事实有多个来源

**SSOT-01 设备的 API 级别有两个来源, 而且其中一个可变**  
`app/src/main/java/net/imknown/android/forefrontinfo/ui/common/AndroidVersionExt.kt:53-64` 定义可变类 `MyAndroid`(`var api` 等)  
和顶层 `val myAndroid`; 写入方是 `AndroidVersionExt.kt:104-109`(`initMyAndroid()`)和  
`ui/home/repository/HomeRepository.kt:126-129`(用 lld.json 的 known 条目覆盖), 读取方是 `:114` 的  
`private val sdkInt get() = myAndroid.api` 与 `:116-125` 的 9 个 `isAtLeastAndroidX()`(`:123` 那条注释掉的不算), 这些助手在 app 里有 43 处调用点(另有 15 行  
import 与 10 行定义行).  
三处后果: 一是 `Build.VERSION.SDK_INT >= X || sdkInt >= X` 这种或条件只能把级别抬高, 不能压低, 而 `:87-90` 只在  
非 stable 且低于 15 的 preview 路径上做 `api++`, 于是 `ui/others/OthersViewModel.kt:44-50` 这类由 `isAtLeastAndroid12()` 保护,  
实际方法带 `@RequiresApi(S)` 的调用(`ui/others/repository/OthersRepository.kt:61-79`)可能在真实 SDK_INT 更低的 preview 设备上执行;  
二是覆盖值只写不清, 一次匹配成功后整个进程生命周期内都保持抬高后的值, 没有任何把它复位到运行时值的路径.  
第三个后果直接落在检测结论上: `HomeRepository.kt:1130` 用 `myAndroid.api` 当过滤阈值挑出 targetSdk 过期的系统应用,  
所以这一行的判定跟着一个数据文件驱动的覆盖值走, 而不是跟着设备真实级别走. 修法: 把级别收成一个注入的, 不可变的值对象,  
known 条目作为另一份可显示的数据参与判定, 与 ARC-03 的注入改造一起做.

**SSOT-02 版本比较有两套机制**  
`versioncompare` 已是依赖并被内核, toybox, WebView 三处使用(`HomeRepository.kt:345,355,909,1016`), 但另有五处直接用 `String`  
比较数值: `HomeRepository.kt:697`(`vndkVersionResult >= lld.android.stable.api`), `:658-659`  
(`versionName >= latestGooglePlaySystemUpdates`, 还拼了 `"$versionName-01"` 覆盖另一种形态),  
`AndroidVersionExt.kt:133`(`myAndroid.apiFull >= lld.android.preview.apiFull`), `:132,134`(`api.toInt()` 后比 Int),  
`ui/home/datasource/LldFileStore.kt:62`(`savedLldVersion < assetLldVersion` 决定要不要重新拷贝资产文件).  
字符串比较对当前的两位数 API 恰好成立, 但它把 "这一行结论是否正确" 寄托在数据形态上. 统一到一套比较类型即可.

**SSOT-03 判定阈值写在代码里而不是数据文件里**  
`HomeRepository.kt:245-252` 的 `offset` 用字面量 `250205 - 250101` 表示 "非最新稳定版时允许多旧的 build",  
`:232` 注释里的 `"250705"` 也是同类字面量; `:234-242` 直接把 build id 第二段当 `Int` 比. lld.json 已经承载了 build  
明细(`:206-209`)和 `scheme`, 这条容差应当与它同源. 现在它随时间自行失配, 失配后表现为绿色圆点变红, 没有代码会报错.

**SSOT-04 设置项的持久化标识放在 string 资源里, 同一映射抄了三份**  
`ui/settings/repository/SettingsStore.kt:33-45` 用 `MyApplication.getMyString(R.string.interface_themes_key)` 等 11 个  
资源值当 SharedPreferences 的键和存储值; 枚举映射在 `:79-91` 的 `when` 里第二次书写; 标签与值的配对在  
`ui/settings/res/values/arrays.xml:4-27` 用两个平行数组第三次表达, 消费处靠下标配对(`ui/settings/SettingsScreen.kt:337`  
`labels.zip(values)`). 后果: 有人改了一个 `*_value` 字符串或调换数组顺序, 老用户存的值就无人认识, 而编译器不会提示;  
`function_allow_network_data_key` 这条资源的名字与它的值 `about_allow_network_data_key` 已经不一致(`strings.xml:28`).  
修法是让枚举自己携带一个不进资源文件的稳定键, 资源只留标签.

**SSOT-05 文案解析有两条路径, 两条路径可以给出两种语言**  
标题在组合期用 `stringResource`(`ui/base/list/MyModelCard.kt:98-101`, `ui/AppRoot.kt:155,170`), 也就是 Activity 的  
resources; 明细在数据层用 `MyApplication.getMyString`(`app/src/main/java/net/imknown/android/forefrontinfo/base/MyApplication.kt:80-84`,  
Application 的 resources), `HomeRepository.kt` 单文件 69 处. `app/build.gradle.kts:46-49` 开了 `generateLocaleConfig`  
并过滤三种 locale. 同一行的两半文本取自两个不同 Context 的 resources(Activity 的那个与 Application 的那个)是代码事实;  
会不会因此给出两种语言, 取决于 Application 的 resources 有没有被设成该 locale, 这一层我没有可引的出处, 记 unknown.  
`generateLocaleConfig` 与 `localeFilters` 确实产出了按应用选语言的配置(构建产物 `app/build/intermediates/merged_manifest/`  
下的清单带 `android:localeConfig`, 那个目录不入库且随 variant 变), 但两个 Context 各自拿到什么 locale 是另一件事, 一致性  
没有代码保证. 修法: 明细也走 `MyModelTitle` 那种 "传 id, 渲染处解析" 的形态  
(`ui/base/list/MyModel.kt:12-15` 已有这个形状), 测试侧后果见 TST-19.

**SSOT-06 lld.json 自带 `scheme` 但没人校验, 解析器也不检查必备字段是否齐**  
`ui/home/model/Lld.kt:8` 声明 `SCHEME_VERSION = 1`, 全仓库零引用(grep 只在定义处), 而随包的  
`app/src/main/assets/lld.json` 里写着 `"scheme": 1`; `ui/common/JsonExt.kt:6` 只设了 `ignoreUnknownKeys = true`,  
`Lld.kt:12-19` 全字段非空, 上游多一个字段无所谓, 少一个字段整份解析失败, 失败后表现为  
`LldFileStore.kt:111-120` 吞掉异常返回 `null`, 用户只看到一行 unknown. 修法: 解析后校验 `scheme == SCHEME_VERSION`,  
并把不认识的 `scheme` 与缺字段区分成两种可显示的失败原因.

另外两条归属于别处但同属 "一个事实多个来源": 服务定位器与 DI 并存见 ARC-03; 占位文案被当数据比较见 PAT-05.

## UDF, 单向数据流上的断点

**UDF-01 加载失败没有状态出口, 一处未包住的异常会让进度条永远转下去**  
`ui/base/list/BaseListViewModel.kt:26-32` 只有 `modelsStateFlow` 和 `isLoadingStateFlow` 两个流, 没有错误通道;  
`:65-71` 的 `startLoad()` 顺序是 `setLoading(true)`, `collectModels()`, `setModels(list)`, 中间没有 catch 也没有 finally,  
所以 `collectModels()` 抛出时 `isLoading` 停在 `true`, 异常继续冒到 `viewModelScope` 成为未捕获异常.  
`ui/home/HomeViewModel.kt:221-223` 的 `homeRepository.getMounts()` 是 `detect()` 里唯一一个不在  
`guardedMyModel` 保护内的仓库调用(`ui/base/list/MyModelExt.kt:35-39` 只保护单个行的构造).  
修法: 给加载契约一个 `Result` 或 sealed UI state, 并在 `startLoad` 里处理失败与 loading 的复位.

**UDF-02 补丁行走的是与列表不同的数据来源**  
`HomeViewModel.kt:267-286` 的 `payloadOutdatedTargetSdkVersionApk()` 为拿 `Lld` 调 `fetchOfflineLldOrNull()`, 而列表  
可能是 `:101-148` 用在线 payload 构造的, 于是这一行与相邻行来自不同版本的同一份数据; 更糟的是 `fetchOfflineLldOrNull()`  
在 `:159` 会调 `copyJsonIfNeededOrThrow()`, 于是 "切换排序方式" 这个纯展示动作可能重写缓存文件.  
修法: 把已解析的 `Lld` 作为一次加载的状态保留下来, 补丁复用它.

**UDF-03 按位置改数据**  
`BaseListViewModel.kt:79-87` 的 `updateModelDetail(targetIndex, newDetail)` 用下标定位行, 下标在  
`HomeViewModel.kt:278-280` 从当时的 `modelsStateFlow.value` 求出, 求完到写回之间一次 `refresh()` 完成就会把内容写到  
另一行上. 修法: 按 `key` 或按 `MyModelType` 定位, 或者让这一行整体由一个可观察输入重算.

**UDF-04 手工计数器代替数据流**  
`HomeViewModel.kt:78-82` 的 `outdatedOrderChanges`(变更次数)与 `loadStartGeneration`(普通 `var`)组合成一套  
"本次加载用的是哪一世代的偏好" 判定, 消费点在 `:54` 与 `:258-264`. 这恰好是 `combine`/`snapshotFlow` 提供的能力,  
自己写出来之后需要注释解释三段时序(`:70-77`). 修法: 把排序偏好作为流输入交给构造列表的那一步.

**UDF-05 去重会把用户主动发起的刷新丢掉**  
`BaseListViewModel.kt:61-63` 在 `loadJob?.isActive == true` 时直接 return; 用户在加载过程中下拉一次, 这次手势不会  
补做, 也不会给出任何提示, 结果是最坏情况下满屏旧数据加一个已消失的转圈. 修法: 记录 "请求过再加载一次", 在尾部补一次.

**UDF-06 首屏与刷新都由组合层触发, 且行序在三个地方手写**  
`ui/base/list/MyModelListScreen.kt:59` 的 `LaunchedEffect(viewModel) { viewModel.init() }` 把首次加载的时机放在 UI 里;  
`ui/settings/SettingsScreen.kt:90-92` 更是把 `LocalContext` 取来的 `packageManager` 和 `packageName` 传进 ViewModel,  
靠 `SettingsViewModel.kt:92-95` 的内部守卫保证只做一次. 行序则分别手写于 `HomeViewModel.kt:212-240`,  
`OthersViewModel.kt:36-97`, `PropViewModel.kt:32-38`, 与检测函数本身构成两两必须同步的清单.  
修法: 触发收进 ViewModel(`PackageManager` 由图提供), 行序与检测项同源于一张注册表(见 PAT-04).

**UDF-07 副作用不走状态**  
`SettingsViewModel.kt:104-121` 的彩蛋计数是私有 `var timesLeft`, 通过 `getVersionClickedMessage(): Int?` 的返回值  
表达事件, UI 在点击回调里直接弹 Toast(`SettingsScreen.kt:117-121`). 这类 "调用即事件" 的形态在配置变更后重放, 也无法  
在测试里观察. 修法: 一次性事件放进可被消费的 state, 或让点击本身只改计数状态.

## 架构

**ARC-01 数据层依赖 UI 层, 中间没有领域层**  
四个仓库都 import UI 侧类型: `HomeRepository.kt:23-27,50`(`MyModel`, `MyModelTitle`, `MyModelType`,  
`guardedMyModel`, `toColoredMyModel`, `StatusColor`), `OthersRepository.kt:14-16`, `PropRepository.kt:7-13`,  
`SettingsRepository.kt:104-113`(返回带 `@StringRes id` 的 `Version`). `AGENTS.md` "架构" 一节写的  
Screen → ViewModel → Repository → DataSource 是对现有代码的描述, 不是负责人定的约定, 所以这里的不一致属于描述与代码  
不符一类: 代码里 Repository 与 ViewModel 之间实际没有边界, 仓库既是数据源调度者, 又是行模型构造者和文案拼接者.  
后果之一是无法在没有 Android 资源的情况下测试任何一条检测(见 TST-04).  
代码侧的候选修法: 检测返回领域结果(值加判定), 行模型与文案在 UI 侧映射; 但改哪一边等 Q-07 判定, 本报告没有替你选.

**ARC-02 特性包互相依赖, home 与 settings 互为上下行**  
`ui/home/repository/HomeRepository.kt:65` 注入 `ui.settings.datasource.AppInfoDataSource`;  
`ui/settings/repository/SettingsRepository.kt:25` 注入 `ui.home.datasource.LldFileStore`;  
`ui/others/datasource/BasicDataSource.kt:7` 依赖 `ui.home.datasource.AndroidDataSource`;  
`ui/home/HomeViewModel.kt:30` 依赖 `ui.settings.repository.SettingsStore`(这是约定允许的);  
共享 UI 工具 `ui/base/list/MyModelExt.kt:28-139` 内部按 `[Home]`, `[Others]`, `[Prop]` 分区写各自的规则,  
即共享层知道每个页面的差别. 修法: 把被跨特性复用的原始能力(`AppInfoDataSource`, `LldFileStore`, 常量表, `PropertyReader`)  
移出特性包, 特性之间只允许向下依赖.

**ARC-03 两种取对象的方式并存, 并且 DI 的提供函数里读的是全局量**  
Metro 图之外还有 `MyApplication` 的三个静态成员, 实测 `MyApplication.instance` / `getMyString` / `sharedPreferences`  
共 115 处调用. 同一件事有两条路径的实例: `AppGraph.kt:207` 的 `fun sharedPreferences(app: MyApplication)` 声明了注入参数  
却返回 `MyApplication.sharedPreferences`(忽略参数); `ui/home/datasource/LldFileStore.kt:27` 注入了 `application` 并用它  
取目录(`:32`), 但同一文件 `:61,76` 又走 `MyApplication.instance.assets`; `ui/common/PropertyReader.kt:25,28` 用注入实例;  
`SettingsStore.kt:33-45` 用静态. 修法: 资源读取与 Context, AssetManager 都由图提供, 静态成员删掉, 见 TST-04.

**ARC-04 图的创建时序靠调用顺序保证, 没有任何强制**  
`MyApplication.kt:58` 在属性初始化里建图, `:90` 才 `instance = this`, 而 `SettingsStore` 的字段初始化(`:33-45`)读的正是  
`instance`; `:103` 用一次解析触发同步填初值, 注释说明 "这行之前谁都不能读这两个流". 也就是说  
`AppGraph.settingsStore` 一旦被提前解析就抛 `UninitializedPropertyAccessException`. 修法: 先绑定 Context 再建图,  
或让填初值所需的读取走注入的 `Context`.

**ARC-05 `:base` 不是抽象层, 而且和 `:app` 共用一个包名**  
`base/build.gradle.kts:6` 的 namespace 是 `net.imknown.android.forefrontinfo.base`, 而 `:app` 里也有同名包  
(`app/src/main/java/net/imknown/android/forefrontinfo/base/MyApplication.kt`), 两个模块各自持有这个包的一部分;  
`:base` 的内容也不只是接口: `base/src/main/.../extension/DateTimeExt.kt:9-26` 写死了 lld.json 的时间格式契约,  
`:35` 的 `isChinaMainlandTimezone()` 是 `:app` 的分发决策(`ui/home/datasource/LldDataSource.kt:35-39` 用它选 gitee 还是 github);  
`IShell` 的两个实现分居两模块(`base/.../impl/ShellDefault.kt:7` 与 `app/.../ui/common/ShellLibSu.kt:16`),  
`IProperty` 的实现在 `:base`(`PropertyDefault.kt:7`). 修法: 应用域内容回 `:app`, 接口模块只留接口与不依赖 app 的扩展,  
两个模块不再共用包名.

**ARC-06 反射被当作 API 使用, 且没有缓存查找结果**  
`PropertyDefault.kt:8-18` 反射 `android.os.SystemProperties`, 每次读取都 `getDeclaredMethod`;  
`HomeRepository.kt:1045-1111` 反射 `WebViewUpdateService.getAllWebViewPackages` 与 `WebViewProviderInfo` 的五个字段;  
`ui/prop/datasource/SettingsDataSource.kt:11-26` 反射 `Settings.*` 的 `declaredFields` 与 `getDeclaredMethod("getString")`;  
`ui/prop/datasource/PropertiesDataSource.kt:17-25` 反射 `java.util.Properties` 的私有 `defaults` 字段;  
`HomeRepository.kt:641-645` 用 `Resources.getSystem().getIdentifier("config_defaultModuleMetadataProvider", ...)`.  
`AGENTS.md` 的 "新代码用当前版本允许的最新 API" 这条约定只管此后新增的代码, 所以下面这些既有反射不属于违反这个约定,  
它们是已有的问题, 要不要按这个约定回头改由负责人判定 (见 Q-05). 其中确有公开路径的地方是 Settings 的公开  
`Settings.Secure.getString(ContentResolver, name)`(只需键名), `PackageInfo` 的公开签名接口,  
`Build.getFingerprintedPartitions()`(见 MOD-04), 以及 `Properties` 的公开 `getProperty()` 与 `stringPropertyNames()`(两者本来  
就会回落到 defaults). 对这一页要渲染的内容来说公开路径可以替代 `PropertiesDataSource.kt:17-25` 那次读私有 `defaults` 字段的  
反射(要不要回头改见 Q-05), 因为消费点只做字符串渲染: `PropRepository.kt:26-34` 取 `toString()`, `:28` 还有一次与  
`System.lineSeparator()` 的字符串相等比较. 类型上这个函数声明的是 `List<Pair<Any?, Any?>>`(`:15`), 而公开的字符串视图按 JDK 的  
`stringPropertyNames()` 会略去键或值不是 String 的条目; 本仓库唯一往系统属性写入的点是 `MyDebugApplication.kt:17` 的  
`setProperty(String, String)`, 框架侧的形态我没有自己核对, 所以会不会出现非 String 条目记 unknown. 其余两处(隐藏的  
`SystemProperties`, `WebViewUpdateService` 与 `WebViewProviderInfo`)没有公开对应物, 反射是唯一路径,  
那要记录的缺口是 "每次读都重新查找方法", 不是 "该用公开 API". 性能代价见 AP-03, AP-04.

**ARC-07 返回处理只有 finish, 与多栈骨架不匹配**  
`ui/AppRoot.kt:123-133` 的 `onBack = { activity?.finish() }` 不弹出选中栈; 现在每个栈只有一个条目所以看不出问题,  
但骨架是 "每页独立返回栈"(`:92` 为每个 tab 建 `rememberNavBackStack`), 一旦加入详情页或弹层, 系统返回会直接退出应用.  
`AndroidManifest.xml:13` 又声明了 `enableOnBackInvokedCallback="true"`, 而代码里没有 `OnBackInvokedCallback` 或  
预测式返回的实现. 修法: `entries.size > 1` 时 pop, 否则 finish, 并把预测式返回接进 Nav3.

**ARC-08 大段注释掉的代码与无人使用的分支留在仓库里**  
`ui/theme/Theme.kt:118-282` 整段注释(165 行), `ui/theme/Color.kt` 只为它服务的对比度颜色常量继续公开存在  
(全文件 200 余条 `val`); `ui/home/datasource/AndroidDataSource.kt:21,25-29,52,90-91,110,117-119,133-134`  
和 `HomeRepository.kt:390-394,498-499,684-685,796,837-846`, `AndroidVersionExt.kt:123,146-148`,  
`build-logic/.../ext/ProjectExtensions.kt:25` 同类; `base/.../impl/ShellDefault.kt` 是 `AGENTS.md` 明确保留的预备实现  
但自带一个会死锁的读流顺序(`:15-21` 先 `waitFor()` 再读流); `ScrollBarMode.Draggable` 仍可被解析  
(`SettingsStore.kt:88-89`)却没有任何绘制路径(`app/.../base/MyApplication.kt:45-46` 只认 `Normal`, 选项在 `arrays.xml:19,26` 被注释),  
`ui/others/res/values/strings.xml` 的 `hw_binder_status`, `vnd_binder_status` 两条字符串无人引用  
(`app/lint-baseline.xml:15-35` 把它记成基线问题). 修法: 删除并让 git 保存历史, 保留 `ShellDefault` 的话把它记成一处明确标注的  
可选实现并在启用清单里说明前置修复(保留还是删除见 Q-04).

## 设计模式

**PAT-01 服务定位器与构造注入并用** 见 ARC-03; `MyApplication.kt:73-85` 的 companion 是显式的服务定位器,  
`ShellLibSu`(`:16`)和 `PropertyDefault`(`:7`)又是 `object` 全局单例, 于是同一进程里有三种对象获取方式.

**PAT-02 空壳基类与空壳容器** `ui/base/BaseViewModel.kt:5` 只是 `abstract class BaseViewModel : ViewModel()`,  
没有任何成员, 但两个 ViewModel 继承它, `BaseListViewModel`(`:18`)再继承它; `ui/home/datasource/AndroidDataSource.kt:3-4`  
是一个永不实例化的类只为装 companion 常量; `di/AppViewModelFactory.kt:32-34` 的三个 map 里两个今天恒为空,  
因为仓库里没有 `@AssistedInject` 声明(grep 唯一命中是 `AppViewModelFactory.kt:20` 的注释).  
前两者是可以直接去掉的层次, 保留只会让 "谁负责什么" 更难看清.

**PAT-03 页面的渲染规则被编进数据生产的 API** `ui/base/list/MyModelExt.kt` 提供四个行构造函数与两个守卫:  
`toErrorMyModel`(`:46-50`, 红点), `toDetectFailedMyModel`(`:95-98`, 无点), `toColoredMyModel` 两个重载(`:56-72`),  
`toTranslatedDetailMyModel` 两个重载(`:105-124`), 选择依据是 "这一页画不画圆点". 同一个失败行在 `PropRepository.kt:102-103`  
还有第三个包装. 于是 "要不要画点" 这个渲染决定写在 62 个条目生产者上(`guardedMyModel(` 25 处,  
`guardedDetectFailedMyModel(` 37 处, 定义行已扣除), 一个页面日后想画点需要改数据层.  
修法: 行模型携带检测结果, 页面映射决定表达(与 ARC-01 一起做, 改哪一边见 Q-07).

**PAT-04 一个巨型类承担 25 个检测方法, 新条目要改两个文件且顺序手写**  
`HomeRepository.kt` 1211 行, 25 个 `detect*` 方法(3 个 private)加通用格式化(`:1181-1210`); 条目顺序在  
`HomeViewModel.kt:211-241` 用 23 条 `tempModels +=` 手写一遍, 其中两个方法各返回 2 行, 所以这一页实际 25 行;  
`OthersRepository.kt` 278 行 37 个 `get*` 方法, 顺序在 `OthersViewModel.kt:36-97` 又是 37 条 `+=`.  
没有 "检测项" 这一抽象类型, 因此无法注册, 无法单测,  
无法只重跑其中一条(UDF-02 的补丁路径因此把整份数据重读一遍). 修法: `interface Detector`, 一个条目一个类型, 列表顺序由注册表给出.

**PAT-05 用本地化文案当哨兵值** `ui/common/PropertyReader.kt:25,28` 把 `result_not_supported` 与 `build_not_filled`  
两条文案作为属性读取失败的返回值, 然后 `HomeRepository.kt:1181-1184` 与 `OthersRepository.kt:219-220` 再拿这些文案去  
比较属性值来决定颜色和是否显示分区指纹. 真实属性值若恰好等于那句文案, 判定就错; 翻译措辞一改, 判定跟着变.  
修法: 返回值用密封类型或 `null` 加原因, 文案只在渲染处取.

**PAT-06 格式串与位置参数数组分离** `HomeRepository.kt:158`(`android_info_detail` 六个参数), `:263`(六个),  
`:919-927`(四个), `:1133-1166`(`joinToString` 里按 `orderByPackageNameFirst` 换 format 资源但传同一对参数),  
`SettingsScreen.kt:226-232`(八个)都是把位置数组和 resource 里的 `%1$s..%6$s` 分别书写, 四种语言各一份.  
少传或错序在编译期无人发现. 修法: 每行一个数据类, 在唯一一处做拼接.

## 模块化

**MOD-01 资源目录要靠构建脚本手工登记** `app/build.gradle.kts:31-42` 登记 10 个条目(命名推导在 `:26-29`), 其中 4 个是  
`base/res`, `base/resLauncher`, `base/resBackup`, `base/resTheme`, 另 6 个是 `ui`, `ui/base/list` 与四个特性包  
`ui/home`, `ui/others`, `ui/prop`, `ui/settings`, 全仓库没有 `app/src/main/res`.  
新增一个页面必须回来加一行, 漏加时资源不参与合并. 修法: 把特性抽成模块(见 MOD-02), 让 AGP 自己合并资源; 或把这个约定写成一处可校验的函数并在  
`check` 阶段断言 "每个特性包都有已登记的 res 目录".

**MOD-02 四个特性只是包, 层级没有任何构建约束** `settings.gradle.kts:54-56` 只 include 了 `:binderDetector`, `:base`, `:app` 三个模块,  
`ui.home` / `ui.others` / `ui.prop` / `ui.settings` 全在 `:app` 内, 于是 ARC-02 的双向依赖不会让构建失败, 也不会有人发现.  
修法: 一特性一模块加 `:core:*`, 或者保留包结构但加一条依赖方向检查.

**MOD-03 两个模块声明了指向不存在目录的依赖** `app/build.gradle.kts:126` 与 `base/build.gradle.kts:14`  
各有一条 `fileTree("libs")`(后者是 `api(...)`, 会传染给所有消费者), 而 `app/libs` 与 `base/libs` 都不存在.  
属于无人使用的配置, 删掉即可; `:base` 用 `api` 这一点尤其不该保留.

**MOD-04 名字冲突与命名与实际不符** `ui/others/datasource/FingerprintDataSource.kt` 与  
`ui/settings/datasource/FingerprintDataSource.kt` 同名而义异(前者是 build fingerprint, 后者是签名证书摘要);  
分区清单硬编码在 `ui/others/datasource/FingerprintDataSource.kt:18-20`(10 个字符串), `OthersRepository.kt:211-221` 只是遍历它,  
而 `:17` 的注释正指向公开 API `Build.getFingerprintedPartitions()`; `AndroidDataSource` 与 `OthersDataSource` 名字相近,  
前者只有常量, 后者只有两个 `Build` 读取; `IProperty.getStringOrThrow` 与 `PropertyReader.getString` 的命名与抛出契约不一致,  
见 MOD-05. 候选修法是改名加用公开 API(既有代码是否回头改见 Q-05).

**MOD-05 `OrThrow` 后缀不再表示 "会抛"** 契约(`AGENTS.md` 的 "Code rules")是 "没有 `OrThrow` 后缀的函数承诺不抛".  
实测两处相反: `LldDataSource.kt:34` 的 `fetchOnlineLldJsonStringOrThrow` 名实相符, 但 `:50` 的  
`fetchOfflineLldFileOrThrow()`(经 `HomeRepository.kt:67` 透出)只是返回 `LldFileStore.kt:32` 的一个 `File`, 真正抛异常的是  
调用方 `HomeViewModel.kt:173` 的 `toObjectOrThrow()`; `AppGraph.kt:149-161` 的 `ProxySelector.select` 里 catch 之后  
返回 `Proxy.NO_PROXY`, 而 `PropertyReader.kt:29` 调用的 `IProperty.getStringOrThrow` 抛出的异常在 `PropertyReader` 内部被捕获,  
使 `PropertyReader.getString` 成为 "带 OrThrow 依赖却从不抛" 的中间层(抛与吞的分工被后缀名藏了起来;  
这与 UDF-01 记的 "加载契约没有错误出口" 是两件事). 修法: 重命名, 让后缀只描述本函数的行为.

**MOD-06 flavor 维度名与实际用途不符, 版本名标记只给了一边**  
`build-logic/.../android/Flavor.kt:9-16,25` 用 `IssueTracker` 作为维度名划分 `Foss` 与 `Firebase`, 这条维度实际划分的是  
"是否含 Google 服务"; `:16,33` 的 `hasVersionNameSuffix` 只对 `Foss` 为真, 于是 Firebase 构建的 versionName 与  
build.toml 里 `versionName` 键的值完全相同, 出问题时无法从版本名区分两种包.

**MOD-07 build-logic 里混入了与构建无关的环境读取**  
`build-logic/.../app/AndroidApplicationConventionPlugin.kt:36-44` 在配置期执行 `git rev-parse --abbrev-ref HEAD` 并取  
`Instant.now()`, 前者进入 `BuildConfig.GIT_BRANCH`, 后者进入 `archivesName`; 项目开了 configuration cache  
(`gradle.properties:28-30`). 这条 git 状态同时是在线数据的 URL 路径段(`LldDataSource.kt:41`), 后果见 SEC-02.  
另注: `gradle/toml/*.toml` 的 "版本只放在 catalog" 约定在这里被绕过了一次, `settings.gradle.kts:63` 与 `:69`  
把 `com.gradle.develocity` 和 `org.gradle.toolchains.foojay-resolver-convention` 两个插件的版本直接写在脚本里.

## Security

**SEC-01 全量应用清单权限, 且把 lint 抑制写进清单**  
`AndroidManifest.xml:6-8` 声明 `QUERY_ALL_PACKAGES` 并 `tools:ignore="PackageVisibilityPolicy, QueryAllPackagesPermission"`.  
它只为一行检测服务(`HomeRepository.kt:1121-1127` 扫全部已装应用的 `targetSdkVersion`), 但它同时把整个已安装应用列表  
暴露给本应用的每一处 `PackageManager` 调用, 也让包可见性限制对全应用失效. Google Play 对这条权限要求专项申报,  
抑制标记把检查关掉而不是记录理由. 可判定项见 Q-01.

**SEC-02 在线数据路径由构建机的 git 状态决定, 失败后静默降级**  
`LldDataSource.kt:41` 把 `BuildConfig.GIT_BRANCH` 拼进 URL; 这个值来自配置期的  
`git rev-parse`(`AndroidApplicationConventionPlugin.kt:37-44`),  
从 tag 或 detached HEAD 构建时得到 `HEAD`, 非 git 目录得到 `unknown`, 于是 URL 指向不存在的路径; 而  
`HomeViewModel.kt:108-111` 把任何异常都转成离线分支, 界面只显示 "离线" 加一句错误文本. 结果是发布包的 "是否最新" 结论  
可能在用户设备上永远来自包内旧数据, 而构建与运行两端都没有信号. 修法: 数据分支/通道作为显式构建输入, 并在 UI 上  
区分 "网络失败" 与 "数据通道不可用".

**SEC-03 缓存的 lld.json 可被外部写, 又被当作可信判定依据**  
`MyApplication.kt:60-71` 用 `getExternalFilesDir(DIRECTORY_DOWNLOADS)`, 这个函数在外部目录不可用时回落到 `filesDir.resolve(type)`,  
所以这条面不是恒在, 而是取决于设备状态; `LldFileStore.kt:101-109` 直接  
`FileWriter` 覆盖, `:50-65` 读取这个文件决定是否重新拷贝资产. 在 minSdk 覆盖的 API 24..29 区间, 应用专属外部目录对其他持有  
旧存储权限的应用是可读可写的; 这份文件又没有 `scheme` 校验(见 SSOT-06), 也没有完整性或签名校验, 传输侧也没有证书固定  
(`AppGraph.kt:116-182` 只设了超时与调试日志). 篡改者可以让 "安全补丁等级""最新 Android" 这些行的红点绿点按他的意愿出现.  
修法: 把缓存放内部存储(`filesDir`), 校验 `scheme` 与 `version` 单调性, 或对这个端点做证书固定 (外部存储是否刻意选择见 Q-06).

**SEC-04 对外部存储路径做递归删除** `LldFileStore.kt:34-40` 的 `deleteDirtyDirectoryOrThrow()` 在目标路径是目录时  
调用 `deleteRecursively()`, 调用点包括 `:47`(拷贝前)与 `:102-104`(保存前). 路径来自 `getDownloadDir()`, 正常情况下只是  
一个文件, 但一旦这个名字被占成目录 (用户或别的持有存储权限的应用造成), 应用会递归删掉那个目录的内容, 而它位于外部存储.  
修法: 只删自己创建的文件, 目录形态改为报错而不是删除.

**SEC-05 shell 命令今天全是常量, 但接口不收参数数组** 逐处核对: 7 个 `shell.execute` 调用点  
(`MountDataSource.kt:27`, `HomeRepository.kt:579,850,892`, `KernelDataSource.kt:13,15`, `PropertiesDataSource.kt:38`)  
传入的命令都出自 `AndroidDataSource` 等处的常量, 连 `HomeRepository.kt:573-578` 那条 `format` 也是常量模板填常量路径;  
`ro.boot.product.vendor.sku` 的值只进 `pathVendorSku` 供 `File.exists()` 用(`:523-529`), 没有进命令行. 所以注入面今天不存在,  
这条记的是形状: `IShell.execute(cmd: String)` 收一整行命令, 备用的 `ShellDefault.kt:14` 走 `Runtime.exec(arrayOf("sh","-c",...))`,  
一旦日后有人把外部值拼进命令字符串, 这两个接口都不阻止他. 修法: 把 `IShell` 的签名改成接收参数列表, 或在接口层约定  
"命令只允许常量模板", 并在 lint 或 review 清单里落一条检查.

**SEC-06 14 处 `printStackTrace()`, 其中 8 处没有 debug 门槛**  
未加门槛的是 `OthersRepository.kt:88,119,133,247`, `AppGraph.kt:153,158`, `LldFileStore.kt:118`,  
`PropertiesDataSource.kt:23`; 另有 15 处 `Log.w/i/e` 无条件输出属性键, 包名, 绝对路径与异常全文(例如 `PropertyReader.kt:31,42`,  
`HomeRepository.kt:667,1053,1207`, `SettingsRepository.kt` 四处). `R8` 侧没有任何日志剥离规则(`app/proguard-rules.pro` 是  
默认模板, 三条注释规则都未打开). 同一功能上还有不一致的示范: `MyModelExt.kt:157-159` 和 `PropRepository.kt:44-46`  
用 `BuildConfig.DEBUG` 包住. 修法: 统一走一个 debug 门槛的日志入口, release 里剥掉.

**SEC-07 Firebase 变体的遥测没有用户同意或开关**  
`build-logic/.../app/AndroidApplicationFirebaseConventionPlugin.kt:23-32` 为 `Firebase` flavor 加入 analytics 与  
crashlytics-ndk, `:37` 打开 `nativeSymbolUploadEnabled`; 代码里唯一的网络开关 `allowNetworkData`  
(`SettingsStore.kt:68,110,157-159`)只被 `HomeViewModel.kt:58` 用来决定是否取 lld.json, 不影响这两个库.  
数据申报与 `GOOGLE_PLAY_PRIVACY_POLICY.md` 的说法需要与代码一致. 修法: 由同一个开关驱动  
`setAnalyticsCollectionEnabled`, 并在隐私政策里写明两种包的差别, 或只发 FOSS 包(两种包的取舍见 Q-03).

**SEC-08 备份配置是两个空元素, 等于不排除任何东西**  
`AndroidManifest.xml:12,14` 指向 `resBackup/xml/backup_rules.xml` 与 `data_extraction_rules.xml`, 两个文件里分别是  
`<full-backup-content />`(`backup_rules.xml:5`)和 `<data-extraction-rules />`(`:4`), 空元素表示 "无 include/exclude",  
即全部备份并随设备迁移, 包含 SharedPreferences 与 `filesDir`, 外部目录副本也在其中; `allowBackup` 未写, 取默认 `true`.  
修法: 显式写 `android:allowBackup="false"`, 或者按目录写 exclude 规则, 把缓存数据排除.

**SEC-09 签名材料与签名缺失的处理**  
`git ls-files keys` 显示 `keys/debug.keystore` 被提交, `app/build.gradle.kts:74-79` 里 debug 库的口令写死为 `android`;  
release 侧 `:51-65` 从 `local.properties` 读口令, 文件不存在时 `:52-55` 静默 `return@register`, 于是  
`signingConfig = signingConfigs.named("release")`(`:91`)拿到的是未配置的签名, `assembleFossRelease` 会产出未签名包而不报错.  
`v1..v4` 手工全开(`:67-70,80-83`)在 AGP 上是多余的. `AGENTS.md` 的 "Never commit" 一节与此有张力, 见 Q-02.

## Compose 性能

**CP-01 动态色板每次重组都重新计算** `ui/theme/Theme.kt:321-329` 在 `AppTheme` 函数体内直接调  
`dynamicDarkColorScheme(context)` / `dynamicLightColorScheme(context)`, 外层没有 `remember`; `AppTheme` 订阅了  
`LocalThemeMode`(`:306`)因而会被重组, 每次重组都要重新遍历 overlayable 资源. 修法: `remember(context, darkTheme)`.

**CP-02 滚动状态被在组合期读取** `ui/common/ScrollBarExt.kt:41` 的 `val scrolling = enabled && scrollState.isScrollInProgress`  
是一次组合期 state 读取, 于是每次开始与停止滚动都会重组 `MyModelListContent`(`ui/base/list/MyModelListScreen.kt:82-150`)  
整个函数体, 项目本身可以跳过, 但 `rememberPullToRefreshState()`, 两处 `dimensionResource` 和整条 modifier 链会重建.  
修法: 把这个读取下推到 draw/layout 期 (或 `derivedStateOf` 加 lambda modifier), 与 `alpha` 现有的 draw 期读取(`:56-88`)一致.

**CP-03 同一行做了两次动画** `MyModelListScreen.kt:141` 给每个 item 加了 `animateItem()`, `MyModelCard.kt:55` 又给  
卡片加了 `animateContentSize()`; 刷新完成时每个可见行同时参与位置动画与自身高度动画, 而 `HomeRepository.kt:1163-1167`  
生成的过期 targetSdk 行文本可能有几百行, 高度动画期间每帧都要重新测量这块 `Text`(`MyModelCard.kt:87-91`).  
修法: 二选一, 并给长文本一个折叠或上限, 见 AP-08.

**CP-04 每行一个空 click 的 Card** `MyModelCard.kt:42-49` 用 `onClick = {}` 只为了找回旧的 ripple 反馈, 代价是每个行都  
带上 clickable 节点, focus 与 indication, 无障碍朗读会说 "点按即可激活" 却什么都不发生. 修法: 要么取消 ripple,  
要么把行展开做成真实动作; 这条也是可访问性缺陷, 不只是性能.

**CP-05 列表首帧没有占位** `MyModelListScreen.kt:71` 把 `null` 显示成空列表, 于是 Home 的 25 行, Others 的 37 行与 Prop 的  
更多行在全部串行跑完之前(`AP-01`)屏幕上只有转圈和一个空背景. 修法: 先给出条目骨架, 逐条填充(与 UDF-01 的流式发射一起做).

**CP-06 大列表的复用分组与条目大小没区分** `MyModelListScreen.kt:132` 的 `contentType` 只有 `General` 与  
`OutdatedTargetSdkApk` 两类(`ui/base/list/MyModel.kt:7-10`), 而 Prop 页的行长度从一行到几十行都有  
(`PropRepository.kt:25-98`, 行数见 AP-04); 每行仍是一个 `Card`(CP-04)加 `animateContentSize`(CP-03).  
修法: 按形态给出可区分的 contentType, 并让短行用更便宜的表达.

**CP-07 Compose 编译器的可见性没打开** `build-logic/.../android/Compose.kt:12-28` 只设 `buildFeatures.compose` 与依赖,  
`KotlinAndroid.kt:97-134` 只加 Kotlin 语言实验开关; 全仓库没有 `composeCompiler { enableReports = true }`,  
也没有 `stabilityConfigurationFiles`(`Lld`, `ShellResult`, `SettingsRepository.Version`, 四个 ViewModel 是否真的跳过  
只能靠猜). 修法: 打开 release 的编译器报告并把类名清单入档, 需要时加 stability 配置.

**CP-08 已知的 Compose lint 结论被写进基线而不是处理** `app/lint-baseline.xml:38-90` 记录  
`ComposeCompositionLocalUsage` 三条(`ExtendedColors.kt:40`, `MyModelListScreen.kt:36`, `Theme.kt:284`),  
`ComposeModifierMissing` 一条(`AppRoot.kt:76`), `ComposeReadOnlyComposable` 一条(`MyModelCard.kt:97`),  
`UnusedResources` 三条(`:3-35`). `app/build.gradle.kts:113-117` 的意图是 "让检查管住新代码, 基线随修复缩小".  
修法: 逐条清掉, 让基线变短; `AppRoot` 缺 `modifier` 参数与 `asText()` 缺 `@ReadOnlyComposable` 都是一行改动.

## App 性能

**AP-01 一次加载串行跑完所有检测才发射** `HomeViewModel.kt:211-241` 顺序 `+=` 23 次产出 25 行, 每项内部还有网络, 文件, shell,  
PackageManager 调用; `OthersViewModel.kt:34-98` 与 `PropViewModel.kt:31-39` 同形. 彼此独立的检测没有 `async` 并发,  
结果也不是流式给出(CP-05). 修法: 并发独立项, 逐条发射到 state.

**AP-02 每次刷新都重复的重活** `getInstalledApplications`(`HomeRepository.kt:1122-1127`)一次全量扫描,  
`cat /proc/mounts`(`MountDataSource.kt:27`), `getenforce`(`HomeRepository.kt:850`), `toybox --version`(`:892`),  
`cat|grep|grep`(`:579`)以及每个 WebView 提供方一次 `getApplicationInfo` 加 `loadLabel`(`:956-994`). 这些都随下拉刷新  
重跑, 且都是非 root shell 冷启动的进程间调用. 修法: 进程内缓存, 或把 "深扫" 与 "刷新" 分成两个动作.

**AP-03 属性读取的热路径上有反射查找** `PropertyDefault.kt:10-18` 每次读属性都重做 `getDeclaredMethod`  
(`Class.forName` 由 `:8` 的字段承担了一次); 一次 Home 加载有 29 处 `propertyReader` 调用, Others 加载另有约 13 次属性读取  
(10 个分区指纹加 3 个单值). 修法: 两个 `Method` 缓存为字段.

**AP-04 Prop 页每个键一次跨进程查询** `PropRepository.kt:36-63` 对 `Settings.System/Secure/Global` 每个声明的 String  
字段各调一次取值(`ui/prop/datasource/SettingsDataSource.kt:20-26`, 每次还反射 `getDeclaredMethod`);  
compileSdk 的 android.jar 里这三个类的 `public static final String` 常量共 195 个(`javap` 统计: System 92, Secure 62,  
Global 41), 也就是一次进入 Prop 页至少 195 次 Settings 查询加 195 次方法查找.  
修法: 用公开 getter 与一次性整表查询, 或者按可见区域分片加载.

**AP-05 启动主线程上有磁盘与反射** `MyApplication.kt:58,92,103` 依次建图, 反射遍历  
`Build.VERSION_CODES*` 公开字段(`AndroidVersionExt.kt:22-44`), 同步填初值设置(`SettingsStore.kt:116-143`,  
含一次可能的迁移写盘 `:126`), `:105` 初始化 libsu builder. debug 包开了 `StrictMode.enableDefaults()`  
(`app/src/debug/java/.../MyDebugApplication.kt:13`), 这些磁盘读会直接报违规. 修法: 初值的写入挪出 `onCreate` 或改为异步加  
首帧等待点; 反射表只在用到时算.

**AP-06 没有 baseline profile, 也没有 profileinstaller** `gradle/toml/*.toml` 里没有  
`androidx.baselineprofile` / `androidx.profileinstaller`, 仓库里没有 `baseline-prof.txt`, 也没有 macrobenchmark 模块  
(与 TST-09 同一处空白). 冷启动与首次滑动的 JIT 成本正好是 baseline profile 覆盖的范围. 修法: 加 generator 模块与  
profileinstaller 依赖.

**AP-07 `gwpAsanMode="always"` 对所有构建类型和两种 flavor 生效** `AndroidManifest.xml:15`. 它是内存安全收益,  
代价是常驻 native 分配开销与更少的可复用槽位. 当前没有任何测量能说明这笔交换划得来(`AP-06`). 修法: 由负责人决定  
是否只在 debug, 或配合 macrobenchmark 量一次(见 Q-03).

**AP-08 巨行文本** `HomeRepository.kt:998-1032`(WebView 行, 多段拼接)与 `:1163-1167`(每个系统应用一行)产生的  
`detail` 可以很长, 而 `MyModelCard.kt:87-91` 用一个 `Text` 全量渲染, 无 `maxLines`, 无折叠. 修法: 默认截断加 "展开全部",  
长清单改成可点开的详情条目.

**AP-09 空结果被当作 "否" 的证据, 且成功与否被丢掉** `MountDataSource.kt:24-36` 不看 `ShellResult.isSuccess`  
就返回解析结果, shell 失败时得到空列表; `HomeRepository.kt:434-436` 的 `isThe2siSar = isAtLeastAndroid10() && mounts.none { ... }`  
在空列表上为真, 于是把 "读不到 mounts" 显示成 "支持 2SI system-as-root" 并配绿色点(`:462-466`); `:727-729` 的 APEX 判断同样  
把空列表当证据. 修法: 保留成功状态, 失败时渲染 unknown.

## Test

依据 `AGENTS.md` 列出的官方测试文档(本地测试, Robolectric, 插桩测试, 稳定性, 性能, UI 测试, 行为, 截图, fundamentals,  
what-to-test, test-doubles, strategies, UI Automator, Gemini Journeys)与负责人补给的 Compose 测试九页(测试主页,  
速查表, semantics, 测试 API, v1 到 v2 迁移, 同步, 互操作, 常见模式, 调试)逐页核对, 现状如下. 两点取材说明:  
速查表那页没有正文, 只有一个 PDF 下载链接; 调试那页只点名 `composeTestRule.onRoot().printToLog()` 一件工具,  
所以 "Compose 编译器报告没打开" 属于性能页的空白(已记在 CP-07), 不是这九页的要求.

**TST-01 五个被跟踪的测试文件里四个是 IDE 模板** `app/src/test/.../ExampleUnitTest.kt:14` 与  
`base/src/test/.../ExampleUnitTest.kt:14` 断言 `2+2`; 两个 `ExampleInstrumentedTest.kt:20` 断言包名. 真实测试只有  
`base/src/test/.../extension/DateTimeExtTest.kt`(4 个用例). 48 个 app 源文件与 7 个 base 源文件的覆盖就是这一个文件.

**TST-02 文档里的测试命令跑不到唯一的真实测试** `AGENTS.md` 写的是 `./gradlew testFossDebugUnitTest`, 而  
`base/build.gradle.kts:2` 只应用 library 约定(无 flavor), 它的任务名是 `testDebugUnitTest`; `:binderDetector` 同形且根本没有  
测试源目录. 修法: 用聚合 `test` 任务, 并把命令文档同步.

**TST-03 插桩测试现在无法运行: 指定的 runner 不在 classpath 上**  
`build-logic/.../android/KotlinAndroid.kt:72-76` 设 `androidx.test.runner.AndroidJUnitRunner`, 但  
`gradle/toml/android.toml:52-55,92-93,104-105` 的 androidTest 只有 `androidx.test:core`, `androidx.test.ext:junit`  
(带进来的是 monitor 与 storage), 没有 `androidx.test:runner` 也没有 `rules`. 官方的插桩测试与 androidx.test 安装页  
要求显式 `androidTestImplementation` runner. 修法: 补 `test-runner`(与需要的 `test-rules`), 然后证明  
`connectedFossDebugAndroidTest` 能跑起来.

**TST-04 DI 没有测试替身入口, 静态成员堵死了 JVM 侧测试** `di/AppGraph.kt:88-90` 的  
`Factory.create(@Provides app: MyApplication)` 不接受额外的 binding container, 四个叶子绑定换不掉: `PropertyDefault`(`:192`),  
`MyApplication.sharedPreferences`(`:207`), OkHttp `HttpClient`(`:116`), `ShellLibSu` 贡献的 `IShell`  
(`ui/common/ShellLibSu.kt:15-16`). 加上 ARC-03 的 115 处静态取用, 仓库和行构造函数都无法在 JVM 上实例化.  
test-doubles 一页推荐的是手写替身, 而这里连替身的注入点都没有. 修法: 工厂签名加 binding container 变长参数(或另建测试图),  
资源与 Context 走注入.

**TST-05 协程与 Flow 测试依赖缺失** `gradle/toml/kotlin.toml:9,19` 只有 `kotlinx-coroutines-android`, 没有  
`kotlinx-coroutines-test`, 也没有 Turbine; `BaseListViewModel.kt:65`(`viewModelScope.launch`), `:79-87`,  
`HomeViewModel.kt:84-97` 这些行为今天在技术上无法断言. 修法: 两个依赖入 `test` bundle, 先断言两条已写明的契约  
(刷新保留旧列表, `loadJob` 去重).

**TST-06 最高性价比的缺失测试是 lld.json 契约** 随包的 `app/src/main/assets/lld.json` 是现成 fixture,  
`Lld.kt:12-19,34-55` 要求每字段非空, `JsonExt.kt:6` 只关了未知键, `DateTimeExtTest.kt:14` 只断言硬编码字面量而不读资产.  
上游数据一改, 现在只能靠运行时被 catch 掉再显示 unknown. 修法: 一个 JVM 测试把资产解成 `Lld` 并断言 `version` 过 `isLldDatetime()`.

**TST-07 纯 JVM 可测但零覆盖的清单** `base/src/main/.../extension/DateTimeExt.kt:28`(Long 重载)与 `:35`(URL 选择依据),  
`base/src/main/.../extension/ExceptionExt.kt:3-4`(`fullMessage` 有 18 处调用点, 注意它总会输出 `Caused by: null.` 一行),  
`MyApplication.kt:26-30,45-46`(深浅判定与滚动条判定), `MountDataSource.kt:27-33`(六列切分),  
`PropRepository.kt:26-34` 与 `:79-97`(getprop 折叠解析), `LldFileStore.kt:62`(用字符串比较当版本检查),  
`MyModelExt.kt:127-137`(`toPropMyModel`). 这些都不需要图, 只需要一个手写 `FakeShell : IShell`.

**TST-08 Robolectric 与 `testOptions` 都没有** `gradle/toml/*.toml` 无 robolectric 条目, 全仓库没有 `testOptions` 块  
(`KotlinAndroid.kt:72-83` 只设 runner 与两个 bundle), 于是 15 处 `Log.*`(清单见 SEC-06)在宿主测试里会直接抛  
"not mocked"; 官方本地测试页给的两种办法是 `returnDefaultValues` 或 Robolectric, 前者会把崩溃变成静默错值.  
Robolectric 页要求 `isIncludeAndroidResources = true` 加 `@RunWith(AndroidJUnit4::class)`. 修法: 建图之后引入 Robolectric,  
用于 `MainActivity`, `SettingsScreen` 与一切读资源的代码.

**TST-09 Compose UI 测试与截图测试的基础设施为零** androidTest 侧没有 `ui-test-junit4`, 也没有 `ui-test-manifest`  
(`android.toml:68-75` 与 `Compose.kt:12-28` 只给 BOM platform), 所以 `MyModelListScreen.kt`, `SettingsScreen.kt`, `AppRoot.kt`  
无法有行为测试; 没有 `screenshotTest` 源集与相应依赖, 而仓库有四种语言的文案(`app/build.gradle.kts:46-49`)和手写的  
主题/排版(`Theme.kt`, `Type.kt:20-30`, `Color.kt`). 行为测试需要 semantics matcher 与一个 Compose 测试规则(用哪个入口见 TST-11),  
截图测试需要 Layoutlib 形态的 `ComposableScreenshotRule`. 修法: 先补两个 test manifest 依赖做一条行断言, 再按 locale 做卡片截图.

**TST-10 bundle 把测试依赖无差别发给所有模块** `KotlinAndroid.kt:78-81` 把 `android.toml:104-105` 的两个 bundle  
发给每个应用约定的模块, `:binderDetector` 没有测试源集也照样拿到 `junit`, `base/build.gradle.kts:13-15` 自己不声明任何测试依赖.  
结果是 "某个模块用什么测" 在读模块构建文件时看不出来, 缺失也看不出来. 修法: 按层拆命名别名(unit, instrumentation,  
composeUi, robolectric, screenshot), 在需要处声明.

**TST-11 文档已把 v1 入口标为废弃, 新套件应直接写 v2** 测试主页的示例仍用 `createComposeRule()`, 但迁移页把  
`test.junit4.createComposeRule`, `createAndroidComposeRule`, `createEmptyComposeRule`, `AndroidComposeTestRule`,  
`runComposeUiTest` 列为废弃并建议迁移, v2 放在 `androidx.compose.ui.test.junit4.v2` 与 `androidx.compose.ui.test.v2`;  
同一页还写明 v1 走 `UnconfinedTestDispatcher`, 可能掩盖真实的时序与竞态. `gradle/toml/android.toml` 的 `compose-bom` 键  
解析出 Compose UI 1.12.1(BOM 对照页), v2 包在这一线已经存在. 后果: 现在写下的第一个测试就落在废弃面上, 而且比生产环境  
更少暴露竞态. 修法: 两个 v2 artifact 进 catalog 的命名别名(与 TST-10 的拆分一起做), 从第一条测试起用 v2 加  
`StandardTestDispatcher`, 每次触发后显式 `waitForIdle()` 或 `runOnIdle()`.

**TST-12 没有 `ui-test-manifest`, 测试容器里的 Activity 也不存在** 测试主页与常见模式页都要求  
`debugImplementation(ui-test-manifest)`, 作用是把 `ComponentActivity` 写进测试 apk 的清单; 仓库没有这条依赖(TST-09),  
`AndroidManifest.xml:23-31` 只声明了 `.ui.MainActivity`. 这里更正一处容易误判的推理: `ui/AppRoot.kt:79` 与  
`ui/theme/Theme.kt:312` 读 `LocalActivity.current` 并不构成 "必须用 Activity 规则" 的理由, 因为 `createComposeRule()` 也在  
测试 Activity 里 setContent, 所以 `Theme.kt:314` 的 `activity != null` 判断在测试里走的是真写窗口 flags 的一侧,  
只有 `@Preview` 才跳过(那里的注释说预览没有 Activity, 这句话对预览成立, 对测试规则不成立).  
修法: 补这条 debugImplementation, 需要 Activity 的用例按常见模式页用 `createAndroidComposeRule<ComponentActivity>()`.

**TST-13 `AppRoot` 离不开 DI 图, 因此导航与 tab 切换今天测不了** `AppRoot.kt:110-113` 四处调 `metroViewModel<...>()`,  
MetroX 的 `LocalMetroViewModelFactory` 要求由 `CompositionLocalProvider` 提供(上游 README 即如此写), 唯一提供点是  
`MainActivity.kt:76`, 而 `di/AppGraph.kt:87-90` 的工厂不收测试容器(TST-04). 测试 API 页推荐的  
`setContent { AppRoot() }` 形态会在第一帧失败. 修法: 手写一个返回假 ViewModel 的 `MetroViewModelFactory` 替身,  
连同 `LocalThemeMode` 与 `LocalScrollBarMode` 两条流组成一个 `TestApp` 包装. 依据: MetroX 的  
`metrox-viewmodel-compose`(版本键 `metro`, `gradle/toml/thirdParty.toml`)上游源码把 `LocalMetroViewModelFactory` 的默认值  
写成 `error("No MetroViewModelFactory registered")`, 所以未提供时第一次 `metroViewModel()` 调用即失败;  
`MainActivity.kt:67-70` 的注释也这么声称, 但这条判定站在上游源码上, 不站在注释上.

**TST-14 三个纯数据 composable 里两个是 private, 隔离测试做不到** 常见模式页的 "Test in isolation" 与测试主页的  
`setContent { MainScreen(uiState = fakeUiState) }` 形态要求页面级纯数据组件可见; `SettingsContent`  
(`ui/settings/SettingsScreen.kt:127`)与 `AppRootShell`(`ui/AppRoot.kt:144`)都是 private, 只有  
`MyModelListContent`(`ui/base/list/MyModelListScreen.kt:82`)是 internal. 同一处也有一条好消息值得记下:  
`MyModelListScreen` 今天就可测, 因为它的参数是抽象的 `BaseListViewModel`(`:47`), 而 `collectModels()`  
(`BaseListViewModel.kt:34`)可以被一个手写的假子类覆盖; `SettingsScreen` 收的是具体的 `SettingsViewModel`(`:77`),  
它的构造链要求 `SettingsStore(SharedPreferences)`, 而后者在构造期读静态资源(`SettingsStore.kt:27,33`, ARC-03).  
修法: 两个 private 改 internal, `SettingsScreen` 改收状态或接口.

**TST-15 真实后台工作不在测试时钟内, `waitForIdle()` 等于没等** 同步页写明虚拟时钟只驱动 Compose 层次,  
`MainTestClock` 不管 Android 的 measure 与 draw, 影响 UI 状态的后台工作要 `registerIdlingResource`,  
依赖外部工作的条件应改用 `waitUntil()`. 本项目的链路是 `MyModelListScreen.kt:59` 到 `BaseListViewModel.kt:65-71` 再到  
`HomeViewModel.kt:211-241`: 29 处属性读取, 若干 shell 调用, 一次全量 `getInstalledApplications`(AP-02),  
外加可选的 Ktor 请求(`AppGraph.kt:116-182`). 后果: `waitForIdle()` 返回时列表还停在 `MyModelListScreen.kt:71` 的空值上,  
断言只能靠超时或非确定等待. 修法: 页面级测试用假 `BaseListViewModel`(TST-14), 只有 `AppRoot` 冒烟用例才用  
`waitUntilAtLeastOneExists` 或 `waitUntilNodeCount`.

**TST-16 全仓库零 `testTag`, 断言只能锚在本地化文本上** `testTag` 与 `testTagsAsResourceId` 在 app 与 base 里 0 命中  
(全文 grep). 测试 API 页的选择器示例是 `onNode(hasTestTag(...))`; 互操作页要求把  
`Modifier.semantics { testTagsAsResourceId = true }` 加在靠近层级的根部, 否则 UI Automator 只能按显示文本和  
contentDescription 定位, 而 tab 图标是 `contentDescription = null`(`AppRoot.kt:169`), 卡片标题由 `stringResource` 解析  
(`MyModelCard.kt:99`), 明细是数据层拼好的字符串(SSOT-05). 后果: 任一 UI 断言都会随译文措辞改动失效,  
四种 locale(`app/build.gradle.kts:46-49`)等于四套断言目标. 修法: 一个 `TestTags` 对象给卡片, 开关行, 单选行,  
对话框条目和 tab, 并在 `AppRoot.kt:149` 的 `Scaffold` 上开 `testTagsAsResourceId`.

**TST-17 现有 semantics 会让断言假通过, 或根本无从断言** 三条都按页面对照过. (a) `MyModelCard.kt:42-49` 的  
`Card(onClick = {})` 让测试 API 页那种打印树里出现 `Actions = [OnClick]` 与 `MergeDescendants = 'true'`, 于是  
`performClick()` 和 `hasClickAction()` 双双通过而实际无事发生(与 CP-04 同一处代码); (b) `MyModelCard.kt:74-83` 的状态圆点  
是只有 `clip` 与 `background` 的 Box, 不带任何 semantics, 所以 "这一行是红是绿" 查不到, 而常见模式页明确反对为纯视觉属性  
自造 semantics, 文档给的路子是像素比对, 本仓库没有截图测试基础设施(TST-09); (c) `SettingsScreen.kt:341,378` 的  
`semantics(mergeDescendants = true)` 把标题与摘要并成一个 Text, `assertTextEquals(标题)` 因此失败, 要用测试 API 页写明  
所有 finder 都支持的 `useUnmergedTree = true`, 机器可读的状态只剩 `Role.Switch` 与 `Role.RadioButton`(`:344,:379`).

**TST-18 中间帧需要显式驱动帧时钟** `MyModelCard.kt:55` 的 `animateContentSize()`,  
`MyModelListScreen.kt:141` 的 `animateItem()`, `ui/common/ScrollBarExt.kt:42-49` 的 300ms 延迟加 250ms tween 都会吃进  
隐式等待, 想在动画中途查询就要同步页的 `mainClock.autoAdvance = false` 配 `advanceTimeByFrame()`, 逐帧查询再用  
`runWithoutImplicitWait()`(该 API 从 ui-test 的 1.12.0-alpha03 起, 现 pin 的 BOM 线已包含). 另外  
`ui/common/ScrollBarExt.kt:56` 的滚动条只在 `drawWithContent` 里绘制, 没有可查询的节点, 所以它显示与否今天无法用  
semantics 断言(CP-02 的同一处).

**TST-19 组合外的 Toast 断不到, locale 覆盖只能覆盖一半文案** `ui/base/ext/ToastExt.kt:8-9` 由  
`SettingsScreen.kt:117-121,424` 调用, 弹的是 `android.widget.Toast`, 位于组合之外, 常见模式页对这类副作用给的做法是换  
`SnackbarHost`; 于是 UDF-07 的彩蛋计数现在没有任何可断言的出口. locale 侧, 常见模式页的  
`DeviceConfigurationOverride.Locales()` 只改变 `stringResource` 的解析结果, 不改变数据层已经用  
`MyApplication.getMyString` 拼好的明细串(`HomeViewModel.kt:250`, `SettingsRepository.kt:34`), 所以一条 locale 用例里  
同一张卡片的两半来自两次解析, 是否读得出两种语言取决于 Application 的 resources 有没有跟着换(同 SSOT-05 的限定).  
修法: 明细改传 id, 副作用改 `SnackbarHost`, 然后这两类断言才有可写的出口.

**TST-21 那个模板插桩测试在 debug 变体上必失败** `app/src/androidTest/.../ExampleInstrumentedTest.kt:20` 断言  
targetContext 的包名等于 `net.imknown.android.forefrontinfo`, 而 `app/build.gradle.kts:105` 给 debug 构建类型加了  
`applicationIdSuffix = ".$name"`, 所以这个测试在 debug 变体上断言的是一个它拿不到的包名. 它今天跑不起来(TST-03 的 runner  
缺失)所以没暴露. 修法: 删掉模板(TST-01), 需要断言包名时用 `InstrumentationRegistry.getInstrumentation().targetContext.packageName`  
与构建期值比较, 或干脆不断言包名.

**TST-20 失败捕获的配置项还够不到, 而 `testApplicationId` 的后果已经在跑应用初始化** 常见模式页给的两个键  
(`androidx.compose.ui.test.failure.isScreenshotCaptureEnabled` 与 `...isUiHierarchyCaptureEnabled`)由迁移页的  
`ComposeUiTestConfig` 与 `TestFailurePolicy` 承载, 迁移页把两者门在比现有 BOM 线更高的 ui-test alpha 线上, 所以现在写了  
也无效; `KotlinAndroid.kt:72-83` 与 `app/build.gradle.kts` 的 `defaultConfig` 没有  
`testInstrumentationRunnerArguments`(全仓库 0 命中). 另一条即时后果是: `testApplicationId` 取默认值意味着每个插桩测试都要  
执行 `MyApplication.onCreate`(`MyApplication.kt:87-106`: 建图, 反射, 设置初值写入, libsu builder, 即 AP-05),  
被测应用无法在测试里变轻. 修法: 先给 `testApplicationId` 与一个测试用 Application, 等 BOM 升到含这个配置的线之后再打开  
两个捕获键.

关于 JUnit 5: 本地测试确实是 Gradle `Test` 任务(`app/build/test-results/testFossDebugUnitTest/`,  
`base/build/test-results/testDebugUnitTest/`), 所以只要给任务加 `useJUnitPlatform()` 就能跑 Jupiter, 现在没有任何地方设置它,  
catalog 里只有 JUnit 4 坐标(`android.toml:47,91`). 插桩测试仍是 JUnit 4: `KotlinAndroid.kt:75` 的 runner 是 JUnit 4 runner,  
社区插件 `de.mannodermaus.android-junit5` 把 Jupiter 插桩限制在 API 26 以上, 高于 `gradle/toml/build.toml` 的 `minSdk` 键,  
且它与本仓库 AGP 线的兼容性未经确认. 结论: 本地测试可用, 但在覆盖还是一个文件的现在换框架得不到收益; 建议先做  
TST-03 到 TST-06. 官方本地测试页写的也是 JUnit 4.

## 建议的处理顺序

1. 先让回归可见: TST-03(runner), TST-05(coroutines-test), TST-06(lld.json 契约), TST-01(删模板). 这四步不需要动架构.  
   补 Compose 测试依赖时把 TST-11(v2 入口), TST-12(manifest), TST-16(`TestTags`)一起做完,  
   否则第一条 UI 断言就只能锚在译文文本上.  
2. 再做判定正确性: AP-09(空列表当否), SEC-02(git 分支决定数据通道), SSOT-03(硬编码日期容差), SSOT-06(`scheme` 校验),  
   PAT-05(文案当哨兵), SSOT-01(级别两个来源). 这些直接决定屏幕上红黄绿是否可信.  
3. 然后拆层: ARC-01 与 ARC-03 一起(检测返回领域结果, 资源与 Context 走注入), 之后 TST-04/TST-08 才有可写的位置,  
   CP-07 的稳定性报告才有意义.  
4. 性能单独一轮: AP-01/AP-02/AP-04/AP-05/AP-06 与 CP-01/CP-02/CP-03.  
5. 清理随时可做: ARC-08, MOD-03, CP-08, SEC-06, PAT-02.

## 需要负责人判定的问题

- **Q-01** `QUERY_ALL_PACKAGES`(SEC-01)是保留并申报, 还是把过期 targetSdk 那行改成 `<queries>` 加限定查询?  
  这条权限同时掩盖了 `SettingsScreen.kt:421` 已废弃的 `resolveActivity` 在 Android 11+ 上的可见性问题.  
- **Q-02** `keys/debug.keystore` 被提交(SEC-09)与 `AGENTS.md` 的 "Never commit signing material" 如何调和?  
  是为了 Firebase 的调试证书指纹而有意保留, 还是应移出仓库?  
- **Q-03** `gwpAsanMode="always"`(AP-07)与 `nativeSymbolUploadEnabled`(SEC-07)是只在 Firebase 包想要的配置,  
  还是两种包都要?  
- **Q-04** `ShellDefault`(`base/.../impl/ShellDefault.kt`)按 `AGENTS.md` 是保留的预备实现, 但它带一个已知的读流死锁(ARC-08).  
  继续保留(并在文档里记录前置修复), 还是删除?  
- **Q-05** 分区指纹清单(MOD-04)硬编码 10 项, 公开 API `Build.getFingerprintedPartitions()` 要更高的 API 级别分支.  
  `AGENTS.md` 那条 "新代码用最新可用 API" 只管新写的代码, 所以这里要的是你的决定: 既有代码是否回头改.  
  ARC-06 里的其余反射同理, 它们是已有的问题, 不是违例.
- **Q-06** lld.json 缓存放外部存储是刻意的吗(SEC-03: 便于用户查看)? 若只是历史选择, 内部存储是更安全的一侧.
- **Q-07** ARC-01 属于描述与代码不符一类: `AGENTS.md` "架构" 一节的四级画法是对现有代码的陈述, 而代码里 Repository 直接产出  
  UI 行模型. 要你先判定改哪一边(把那句描述改成实际层数, 还是按那句补出领域层).  
- **Q-08** 本报告标的是 record, 但每条都带修法, 属于会被当作活文档引用的形态. 可执行的发现要不要转进  
  `docs/dev/issues-cn/` 的编号条目, 这里只留时点记录?
