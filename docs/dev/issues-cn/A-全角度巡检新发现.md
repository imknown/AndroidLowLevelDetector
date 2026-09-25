# A·全角度巡检新发现 (N1~N16)

> 返回 [项目问题汇总](README.md). 2026-09-25 两轮全角度巡检的净新增发现, 编号 N1~N16 (N = New).
> 基线: jetpack-compose-new 分支 `d7129dba` (代码与 `a87f80e1` 一致, 其后全是 docs 提交), 扫描者 GLM-5.3-Flash.
> 方法: 第一轮四路并行代码扫描 (原生 JNI 与安全 / 构建 CI 与依赖 / Compose UI 细节 / 数据层并发); 第二轮工具实测 (`lintFossDebug` + `testFossDebugUnitTest` 全绿, 编译零告警) + 多语言资源逐 key 脚本审计 + 已裁定条目落地核对 + 依赖新鲜度联网核对. 关键条目 (N1, N4~N8, N16) 已人工对源码复核.
> 严重度沿用 P0~P3. 与台账重叠的报告不占编号, 见文末[去重表](#去重表); 检查过没有问题的面见文末[已核对通过](#已核对通过). 裁定条目落地核对结论: R1/R2/R10/F1/F5 与 05 分册一致; 仅 README 总览表的 R1 行措辞按 05 分册 2026-09-22 复验对齐 (迁移未含, 工具栏与底栏都不隐藏).

## 稳定性与数据

<a id="N1"></a>

### N1 日期解析未指定 Locale, ar/fa 等地区启动即崩 (P1)

**结论**: `base/extension/DateTimeExt.kt → formatToLocalZonedDatetimeString()` 两个重载的 `DateTimeFormatter.ofPattern(pattern)` 都不带 Locale, 跟随系统 "格式地区". ar/fa 等使用本地数字 (٠١٢) 的地区, 用它解析 ASCII 的 `"2026-09-18 13:00 +0800"` 直接抛 `DateTimeParseException`.
**证据**: 触发点 `HomeRepository.detectMode() 里对 lld.version 的解析` (每次 Home 加载都走, 含离线模式) 与 `SettingsRepository.getBuiltInDataVersion() 里对资产 lld 版本的解析`; 加载协程无兜底 (= C1/F2), 这些地区用户首次进首页即崩. 与 AR-15 是不同触发条件: 上游格式不变也会崩.
**修复方向**: 两处 pattern 补 `Locale.US` (或 ROOT), 约 4 个调用点随之受益. 修复时机随 C1 类 "业务逻辑暂缓" 裁定与否由负责人定.

<a id="N2"></a>

### N2 网络请求零超时配置, 配合加载去重可永久卡死 (P2)

**结论**: `LldDataSource.fetchOnlineLldJsonStringOrThrow()` 的 Ktor/OkHttp 客户端没有 `HttpTimeout`, 也没设 OkHttp 的 callTimeout — 服务端只要慢速吐字节, 请求可以无限挂起.
**证据**: 配合 `BaseListViewModel.startLoad() 的 loadJob.isActive 去重`, 挂起的加载让之后所有下拉刷新与 init() 被静默吞掉, 页面永远转圈, 只能杀进程. 与 F2 (异常卡死) 症状相同, 根因不同.
**修复方向**: 客户端装 `HttpTimeout` + OkHttp `callTimeout`, 或在 fetch 处包 `withTimeout`.

<a id="N3"></a>

### N3 lld.json 的 scheme 闸门未启用; api 字段 toInt 无防护 (P2)

**结论**: `Lld.kt → SCHEME_VERSION` 定义后全仓零引用 — `scheme` 字段本该是 "数据格式过新" 的闸门, 现在上游升 scheme 后老版本只会得到一条含糊的解析错误然后静默回退旧缓存.
**证据**: 同族: `Lld.Android.Android.api` 是 String, `AndroidVersionExt` 与 `HomeRepository` 多处 `toInt()` 无 `toIntOrNull` 防护, 上游一旦写成 "37.1" 即运行时异常. 序列化本身开了 `ignoreUnknownKeys`, 加字段不崩, 只有改字段类型/结构才触发.
**修复方向**: 解析后校验 `scheme == SCHEME_VERSION`, 不符走明确提示与回退; api 字段换 `toIntOrNull` + 兜底.

<a id="N4"></a>

### N4 myAndroid 跨线程无同步读写 (P2, 与 AR-03 同根)

**结论**: `AndroidVersionExt → myAndroid` 的四个字段是 var, `detectAndroid()` 在 `Dispatchers.Default` 上改写, 而其他页签的加载协程同时经 `isAtLeastAndroidX()` 读它, 无任何同步.
**证据**: 多页签并发加载时其他线程可能读到写了一半的值; 预览版系统上 `detectAndroid` 运行中会把 api 从 36.1 语义改回 36, 同一次加载前后判断不一致.
**修复方向**: 与 AR-03 的 "初始化后冻结" 合流实施即同时解决; 短期可先在加载前做局部快照.

## 原生

<a id="N5"></a>

### N5 JNI GetStringUTFChars 未判空 (P2)

**结论**: `BinderDetector.cpp → getBinderVersion()` 里 `env->GetStringUTFChars(driver, nullptr)` 的返回值没判空, OOM 时返回 nullptr 并挂起异常, 直接传给 `open()` 会 SIGSEGV.
**修复方向**: 判空后直接 return (JVM 已挂 OOM 异常, 让 Java 层收到即可). JNI 标准必查项.

<a id="N6"></a>

### N6 errno 在日志调用之后才读取 (P2)

**结论**: `BinderDetector.cpp → getErrorNo()` 先 `__android_log_print` 再取 errno — 日志函数内部做 socket 写入可能覆盖 errno, 返回给 Kotlin 的错误码不再是真实失败原因.
**证据**: Kotlin 侧 (`OthersRepository.getBinderStatus()`) 靠 -2/-13 区分 "文件不存在/无权限", errno 失真后本该 "不支持" 的结果会变 "未知".
**修复方向**: 系统调用失败后立刻把 errno 存进局部变量, 再打日志.

## 构建 / CI

<a id="N7"></a>

### N7 configureCompose() 的 Compose 开关是死代码 (P2)

**结论**: `build-logic → Compose.kt → configureCompose()` 的 `when (this)` 判断的是 Project 本身, Project 永远不实现 ApplicationExtension/LibraryExtension, `buildFeatures.compose = true` 从未被设置.
**证据**: 现在能编译全靠 KGP 2.x 应用 `org.jetbrains.kotlin.plugin.compose` 时自动打开该开关 (官方文档允许省略); 这段 "看起来在开 Compose" 的代码实际从不运行, 照此模式给库模块配置会静默失效.
**修复方向**: 删掉 when, 需要时在 android 扩展上显式设置.

<a id="N8"></a>

### N8 archivesName 嵌入分钟级时间戳与 git 分支名 (P2)

**结论**: `AndroidApplicationConventionPlugin → configureName()` 在配置阶段取 `Instant.now()` (分钟级) 与 `git rev-parse --abbrev-ref` 拼进 archivesName, 并把分支名注入 `BuildConfig.GIT_BRANCH`.
**证据**: 产物名每分钟都变 (输出目录堆积历史 APK, 任务缓存永不命中); configuration cache 命中时时间戳是旧值; 无 git 的环境构建直接失败; detached HEAD 时 GIT_BRANCH 为 "HEAD", lld.json 在线 URL 失效 — 最后这条是 R8 已裁定风险的加重情节, 仅登记不改裁定.
**修复方向**: 产物时间戳交给 CI artifact 命名; 分支相关值改由 CI 显式注入, URL 指向稳定 ref (随 R8).

<a id="N9"></a>

### N9 CI 缺并发控制/超时/缓存/JDK 固定/最小权限; Dependabot 不含 github-actions (P2)

**结论**: 两个 workflow (`android-ci.yml`, `dependency-submission.yml`) 都没有 concurrency 组 (连续 push 新旧构建并行排队) 与 timeout-minutes (卡死挂满 6 小时); android-ci 没有 setup-java 与 Gradle 缓存 (JDK 25 每次经 foojay 现下载, foojay 一变 CI 就挂), 没有显式 `permissions:` 块; dependabot.yml 只配 gradle, actions/checkout 等自身永远不会被升级.
**修复方向**: 是 [QW-1](06-快赢清单.md#qw-1) (CI 加测试与 lint 门禁) 的前置补课, 建议合流实施.

## UI 与无障碍

<a id="N10"></a>

### N10 Settings 行缺单选/开关角色与状态语义 (P2, 与 F5/F16 同域)

**结论**: `SettingsScreen` 的单选行是 `Row.clickable + RadioButton(onClick = null)`, 行没有 Role.RadioButton 与 selected 状态, 读屏只念 "双击以激活" 不念 "已选中"; 开关行是整行 clickable 与尾部 Switch 两个独立焦点, 行本身不暴露开关状态.
**修复方向**: 单选行 `Modifier.selectable(selected, role = Role.RadioButton)`; 开关行 `Modifier.toggleable(value, role = Role.Switch)` 并把 `Switch(onCheckedChange = null)` 降为纯展示.

## P3 杂项 (编号只为跟踪)

<a id="N11"></a>

### N11 比较与解析健壮性杂项 (P3)

- `HomeRepository.detectVndk() 的 vndkVersionResult >= lld.android.stable.api` 是字符串比较, 位数不同就比错 ("9" > "34"); `AndroidVersionExt` 的 apiFull 比较、`LldManager` 的 savedLldVersion < assetLldVersion 同病;
- `HomeRepository` 的 `250205 - 250101` 魔法偏移随预览版周期失真;
- `MyModelExt` 的 `split(": ")` 只取第一段, 属性值里再出现 ": " 时后面的内容全丢;
- `PropRepository` 的多行 prop 拼接: 输出以非 "]" 行结束整块丢弃且无日志;
- `MountDataSource`: 非 6 列行静默跳过, columns[4].toInt() 无防护.

<a id="N12"></a>

### N12 启动路径与性能杂项 (P3)

- `MyApplication.initTheme()` 冷启动主线程同步读 SP; `AndroidVersionExt.initMyAndroid()` 主线程反射遍历 VERSION_CODES;
- `MyDebugApplication` 的 StrictMode 晚于 super.onCreate() 开启, 启动期违规全漏;
- `PropertyDefault` 与 `SettingsDataSource` 每次调用 getDeclaredMethod (后者每 key 一次, 放大 AR-14);
- `LldDataSource` 每次刷新新建整套 OkHttp 引擎 (AR-12/QW-3 的引擎重建面).

<a id="N13"></a>

### N13 行为细节杂项 (P3)

- `HomeViewModel`: allow_network 开关没注册监听 (与 outdated_order 不一致), 改完要手动刷新才生效;
- `LldDataSource`: 下载源只按 `isChinaMainlandTimezone()` 单选, 失败不回退另一个; TrafficStats 拿线程 id 当 socket tag, connectFailed 路径不清理;
- `MainActivity`: 只在 onCreate setTheme, 运行中切主题 window 底色不跟 (最近任务/退出动画闪旧底色);
- `MyModelCard`: 圆点悬浮 TopEnd, 长译文标题首行被压;
- `BaseListViewModel.startLoad()`: setLoading 前的 1 帧窗口内 refresh 被静默吞;
- `base/shell/impl/ShellDefault` (死代码): 先 waitFor 再读输出流, 管道死锁隐患, 要么修要么删;
- `ArchitectureDataSource.getBinderVersionOrThrow()`: 每调用一次 System.loadLibrary;
- `OthersRepository`: binder try/catch 表达式类型抹成 Any (catch 只捕 UnsatisfiedLinkError, 且 printStackTrace 返回 Unit).

<a id="N14"></a>

### N14 构建配置杂项 (P3)

- app/base 给不存在的 `libs/` 挂 fileTree (base 还是 api);
- 版本目录死条目: `androidGradlePlugin-beta` (比在用 stable 还旧), `androidGradlePlugin-canary`, `compileSdkExtension`, google.toml 的 firebase bundle, kotlin.toml 的 compose-gradlePlugin (仅注释引用);
- toml 注册逻辑在根 settings 与 build-logic/settings 整段复制;
- Develocity `publishing.onlyIf { false }` 常驻加载永不上传;
- gradle.properties 的 `configureondemand` 与 `android.debug.obsoleteApi` 可清理;
- app 的 res 目录用 `java.directories[0]` 脆弱取值;
- CI 的 cmake/NDK 版本与 build.toml 双份手工同步, `packages: ''` 空步骤, actions 只钉大版本 tag;
- `AndroidApplicationFirebaseConventionPlugin` 靠任务名 startsWith/contains 匹配禁用 foss 任务, 且每次配置 println;
- 两个 NdkVersion convention 插件重复 apply 宿主已应用的 android 插件;
- release 签名 storeFile 相对路径按 app 模块目录解析, 无环境变量回退, 无 local.properties 时 assembleRelease 报错难懂;
- 全模块注入 8 个实验性 `-X` 编译器开关 (知情项: 升 Kotlin 时最易集体翻车, 符合负责人偏好, 不立项).

<a id="N15"></a>

### N15 原生杂项 (P3)

- `BinderDetector.release()` 对负 fd 无条件 close(-1);
- CMakeLists: 无 -Wall -Wextra -Werror, 无 LTO, cmake_minimum_required 3.10 过旧 (AGP 默认已带 stack-protector/fortify, 加固够);
- :binderDetector 缺 consumer-rules.pro, JNI 符号目前靠 AGP 默认 keep 规则保住 (base 有, 参照补);
- namespace `binderDetector` (大写 D) 与包名 `binderdetector` (小写) 不一致;
- binder_version 结构体手写, __s32 靠传递引入, 不如直接 include &lt;linux/android/binder.h&gt;.

<a id="N16"></a>

### N16 lint 实测: 9 条真死资源 + 空 super 调用 (P3)

**结论**: `lintFossDebug` 0 error / 11 warning, 编译零告警. 逐一核对引用后: 9 条真死资源 — `about_shop/source/privacy_policy/licenses/translator_more_info/version_key`, `interface_themes_power_saver_key` (随 AppCompat 退役的主题档), `hw_binder_status`, `vnd_binder_status` (设置页手写 M3 重建后 SP 键改硬编码, 旧键资源成遗骨); 1 条 `EmptySuperCall` (`HomeViewModel.onCleared() 的多余 super`); 1 条半误报 `leak_canary_display_activity_label` (被 LeakCanary 自己的 manifest 消费, lint 看不见).
**修复方向**: 纯删 + 顺手删 super; lint 零配置的现状因此不算欠账, 真正缺的是 CI 跑它 (QW-1).

<a id="去重表"></a>

## 去重表 (报告过, 但已被台账覆盖, 不占编号)

| 巡检报告项 | 已有编号 |
|---|---|
| startLoad 协程无 try/catch, 异常即崩 + 转圈不止 | C1 + F2 (同根) |
| 在线数据无条件覆盖本地缓存 | R7 |
| lld.json URL 写死构建时分支 (detached HEAD 加重情节并入 N8) | R8 (已裁定) |
| Ktor client 每次刷新新建 | AR-12 / QW-3 |
| 每 key 反射查询放大 Prop 页开销 | AR-14 (伴生细节并入 N12) |
| 11 处 printStackTrace 丢栈 | AR-18 (错误丢栈主题) |
| RC 依赖 (serialization 1.12.0-RC, navigation3 1.2.0-rc01 等) | 负责人成熟度门槛 (RC 可上生产) 之内, 不算问题 |

<a id="已核对通过"></a>

## 已核对通过 (基准, 供下次复查对照)

- **工具面**: lintFossDebug + testFossDebugUnitTest 本地全绿 (4m5s), lint 0 error / 11 warning (见 N16), Kotlin 编译零告警; CI 同款命令可复现.
- **多语言**: 4 语言 x 8 个按包拆分 res 目录逐 key 对比 — 占位符错配 0, 真实漏翻 0 (90 条目录差异全为 translatable="false" 的 SP 键/URI/品牌词); Compose 侧零 Icons 引用, RTL 无新增缺口; textDirection 偏差维持 [观察记录](A-迁移期观察记录.md) 的接受裁定.
- **原生**: 无 strcpy/sprintf/memcpy 类危险调用, ioctl 入参为栈上结构体; 成功/失败路径 ReleaseStringUTFChars 与 close(fd) 均成对, open 带 O_CLOEXEC; JNI 符号与 Kotlin 侧匹配; /dev/binder 路径 minSdk 24 下恒存在, 无需 API 守卫.
- **安全**: 全部 URL https, 无 cleartext; shell 命令全部硬编码常量, 无注入面; release.jks / local.properties / google-services.json 均正确 ignore, git 中仅标准口令的 debug.keystore; manifest exported 仅 launcher Activity; gwpAsanMode=always; 备份内容现状无敏感数据 (规则文件为空仅提醒).
- **依赖与模块**: :base/:binderDetector 零反向依赖, implementation/api 无误用; Gradle 9.7.1 满足 AGP 9.4 最低要求, JDK 25 三处一致; kotlinx-serialization 1.12.0-RC (stable 1.11.0) 与 navigation3 1.2.0-rc01 (stable 1.1.5) 均在门槛内, 需要时可回落; navigation3 1.1.2/1.2.0-alpha02 曾有折叠屏双返回崩溃 (issue 516312097), 建议[回归清单第 6 项](A-Compose遗留优化与回归清单.md)在折叠屏过一遍.
- **Git**: 72 个 tag 连续到 1.18.7, 与 versionName 1.18.8_unpublished 衔接正常; 无大文件; .kilo worktree 未被 git 追踪.
- **其他**: 序列化 ignoreUnknownKeys 已开 (上游加字段不崩); Nav3 四栈用法与官方配方一致; SP 监听注册/注销对称, 流与文件句柄错误路径均关闭; 全仓零 TODO/FIXME; 无 GlobalScope/runBlocking/手动建线程.

---

返回 [README](README.md)
