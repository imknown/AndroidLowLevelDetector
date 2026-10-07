# ST-01 build-wiring-and-graph-skeleton: 修改报告

> 状态: record. 对应计划: [plan.md](plan.md) 子任务 01. 范围: 只搭骨架, 不碰任何业务类.

## 改了什么 (6 个文件)

| 文件 | 改动 |
|---|---|
| `gradle/toml/thirdParty.toml` | + `metro = "1.4.5"`; + `metrox-viewmodel` / `metrox-viewmodel-compose` 两个库; + `metrox-viewmodel` bundle; + `metro` 插件别名 |
| `app/build.gradle.kts` | + `alias(libsThirdParty.plugins.metro)`; + `implementation(libsThirdParty.bundles.metrox.viewmodel)` |
| `di/AppGraph.kt` (新) | `@DependencyGraph(AppScope::class) interface AppGraph : ViewModelGraph` |
| `di/AppViewModelFactory.kt` (新) | MetroX 要求的工厂子类, `@Inject` + `@ContributesBinding` + `@SingleIn` |
| `base/MyApplication.kt` | + `val appGraph: AppGraph = createGraph<AppGraph>()` (+ 两个 import) |
| `ui/MainActivity.kt` | `setContent` 根部包一层 `CompositionLocalProvider(LocalMetroViewModelFactory provides ...)` (+ 2 个 import) |

## 为什么这么改

### 1. 为什么放在第一步且不碰业务类

构建接线 (插件 + 依赖 + 图骨架) 是后续每个子任务的依赖面; 一次接好后, ST-02 起每个子任务的 diff 就只剩业务类上的注解. 同时这一步承担计划里最大的风险验证:  
**Gradle 9.7.1 + AGP 9.4.1 组合不在 Metro 官方兼容矩阵内** (矩阵只约束 Kotlin), 冒烟结果: 通过. 首次全量构建 7m38s 失败于下述 import 问题,  
修复后增量 1m29s 成功.

### 2. 为什么 `metro` 版本注释要写 "同版本锁升"

Metro 的插件 / 运行时 / MetroX 工件必须同一版本  
(编译器插件与运行时生成代码强耦合,  
官方 stability 页明说 "does not guarantee generated code will be compatible with different versions of the  
runtime"), 且编译器插件跟随 Kotlin 版本 (Kotlin 2.4.20 需 Metro ≥  
1.2.0). 三者共用一个 `version.ref` 从目录层面锁死; 注释把 "升 Kotlin 必须查兼容矩阵" 留给下一次升级的人, 这是 AGENTS.md 版本阶梯规则在第三方依赖上的落实.

### 3. 为什么插件直接 `alias` 进 `app/build.gradle.kts` 而不是包一层 convention plugin

只有 `:app` 编译 Metro 注解代码 (`:base` / `:binderDetector` 零接触), 全模块 convention plugin 没有第二个消费者;  
项目先例是 `kotlinx.serialization` 同样直接 alias. 依赖走 `mavenCentral` (未做内容过滤), 所以 `settings.gradle.kts` 无需改动.

### 4. 为什么 `AppGraph` 一行绑定都没有

依赖全为具体类型, `@Inject` 构造函数即绑定 (ST-02 起逐类挂上), 现有对象图零 `@Provides` 即可建图, 这是选型表里 Metro 的核心优势.  
`AppGraph : ViewModelGraph` 继承进来的是三个 `@Multibinds(allowEmpty = true)` 的 ViewModel map 和 `metroViewModelFactory` 访问器;  
后者是**必须解析的 root**, 没有 `AppViewModelFactory` 绑定就会编译期 `[Metro/MissingBinding]`, 所以工厂子类必须在 ST-01 就位,  
这也是计划把两个新类放在同一子任务的原因.

### 5. 为什么 `AppViewModelFactory` 的三个 map 参数一个都不能省

`MetroViewModelFactory` 抽象类声明了三个 `protected open val`, 子类构造必须全给. assisted 两个 map 本项目今天为空,  
`allowEmpty = true` 正是 MetroX 为 "暂时没有 assisted ViewModel" 留的口子; 将来某个 ViewModel 上 `@AssistedInject` 后自动入 map,  
这个类零改动.

### 6. 为什么 `MyApplication` 持图用急切初始化而不是 `lazy`

`MainActivity.setContent` 要读 `appGraph.metroViewModelFactory`; Application 一定先于任何 Activity 构造,  
急切初始化消掉一切时序疑问. `createGraph<AppGraph>()` 是编译器 intrinsic,  
但它**在 runtime 里有真实声明** (`dev.zacsweers.metro.CreateGraphKt`, 编译器插件再替换其实现),  
所以必须 `import dev.zacsweers.metro.createGraph`. 本次构建失败一次的根因就是漏了这个 import:  
文档示例 `val graph = createGraph<AppGraph>()` 看起来像无 import 的魔法, 实际是普通顶层函数. 这个坑值得留给 review 与后来者.

### 7. 为什么 MainActivity 是手写的 "(application as MyApplication).appGraph"

`metrox-android` 的 AppComponentFactory 方案能让框架替图构造 Activity, 但要求 API 28; 本项目 minSdk 24,  
这是框架边界不是偷懒 (issues-cn #02 已记录). Activity 里这一行是全项目唯一保留的手写取图点;  
用 `application as MyApplication` 而不是 `MyApplication.instance` 静态, 避免在接入 DI 的同时再添一个服务定位器调用.

### 8. 为什么工厂经 CompositionLocal 下发而不是构造注入进 Activity

`LocalMetroViewModelFactory` 默认值是 `error("No MetroViewModelFactory registered")`,  
忘了 provide 会在第一帧 fail fast; 这是 metrox-viewmodel-compose 文档的标准模式. 四个页面的 `metroViewModel()` (ST-02 起) 都从  
CompositionLocal 取工厂, entry 作用域由 Nav3 的 `rememberViewModelStoreNavEntryDecorator()` 保证,  
与现状 `viewModel(factory = ...)` 完全一致.

## 验证

- `./gradlew assembleFossDebug` → BUILD SUCCESSFUL (失败→修复→通过的过程见第 6 节).
- 运行期验证 (四个 tab 正常加载) 需要真机/模拟器, 属 ST-05 第一段收尾时的整体验证项; ST-01 的工厂 map 为空, 页面行为与改造前一致.

## 遗留

无. 下一子任务 ST-02 (home 链) 将产出第一批 `@Inject` 业务类.
