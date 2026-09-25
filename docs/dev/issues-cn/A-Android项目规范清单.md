# 规范的 Android 项目清单

一个规范的 Android 项目应该具备的东西, 作为清单使用, 按"从地基到装修"的顺序排列.

## 1. 构建与依赖管理

- Gradle 用 Kotlin DSL(`build.gradle.kts`); 所有依赖版本集中在版本目录 (version catalog, 可以是单个 `libs.versions.toml`, 也可以物理拆成多份 `.toml`), 升级版本只改一处.
- 多模块项目把重复的构建配置抽成共享插件 (convention plugin), 放在 `build-logic` 下.
- AGP 和 Kotlin 版本保持较新且各模块一致; JDK 至少 17, 建议跟随较新版本 (不限于 LTS).
- `minSdk` / `targetSdk` / `compileSdk` 有明确策略: `targetSdk` 跟着 Google Play 每年的硬性要求走, 不要长期不升.
- release 构建配好正式签名和 R8 混淆规则; debug 构建用单独的 `applicationId` 后缀, 方便和线上版本共存安装.

## 2. 架构与分层

- 遵循官方架构指南: UI 层, (可选的) 领域层, 数据层. 数据来源统一包在 Repository 后面, UI 不直接碰网络和数据库.
- MVVM/MVI: 状态放在 ViewModel 里, 单向数据流 (事件往上, 状态往下), 异步用 Coroutines + Flow, 不混用多种异步写法.
- 项目变大后按功能拆模块 (`feature:xxx`, `core:xxx`), 依赖只能单向, 不允许循环引用.

## 3. UI

- 新项目默认用 Jetpack Compose + Material 3, 单 Activity + Navigation. 主题 (颜色, 字体, 形状) 集中定义, 支持深色模式.
- 不要只按"手机竖屏"写死: edge-to-edge(内容延伸到状态栏和导航栏), 屏幕旋转, 折叠屏/平板的自适应布局.
- 无障碍 (内容描述, 触控目标大小) 和多语言: 文案全部放进 `strings.xml`, 不写死在代码里.

## 4. 数据与基础设施

- 网络: Retrofit 或 Ktor + kotlinx.serialization. 本地: 结构化数据用 Room, 偏好设置用 DataStore(替代 SharedPreferences). 图片加载用 Coil.
- 依赖注入: 选一个**编译期** DI — Hilt(官方主推, Android 向) 或 Metro(Kotlin-first, KMP 原生) — 从头用到尾; Koin 本质是运行时服务定位器, 缺编译期校验, 不建议. 规模小的项目用手写的 `AppContainer` 也成立, 但组合根必须集中, 唯一.
- 运行时权限做统一封装. 要处理进程被系统杀掉后的状态恢复 (`SavedStateHandle` / `rememberSaveable`) — 这是最容易漏的一项.

## 5. 质量保障

- 测试: 核心逻辑有单元测试 (JUnit + Turbine/MockK), 关键界面有 Compose UI 测试.
- 静态检查: Android Lint + ktlint/detekt, 在 CI 里强制执行.
- 线上监控: 崩溃上报 (如 Crashlytics), ANR 监控.
- 性能: Baseline Profiles 加快启动, StrictMode 抓主线程上的磁盘和网络操作, 关键路径用 Macrobenchmark 做性能回归.

## 6. 协作与工程化

- README(说明项目是什么, 怎么构建运行), `.gitignore`, 统一的代码风格配置.
- CI(如 GitHub Actions) 在每次提交上跑编译 + lint + 测试.
- 语义化提交 (`feat:` / `fix:` 等类型前缀, 大小写不强制); 版本号和变更日志 (changelog) 有管理.
- 重要技术决策写成文档 (如 `docs/` 下的 ADR), 不留在聊天记录里.

## KMP 备注

有得选时, 优先用 KMP 兼容的库 (Ktor, kotlinx.serialization, 新版 Room, Coil 3, Metro), 以后想跨平台时迁移成本低很多. Compose 本身已经支持多平台.
