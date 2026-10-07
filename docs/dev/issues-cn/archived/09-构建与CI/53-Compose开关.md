<a id="i53"></a>

# 53 configureCompose() 的 Compose 开关 (P2)

> 返回 [README 索引](../../README.md) · [9 · 构建与 CI](../../README.md#9--构建与-ci).

**严重程度: P2 | 修复难度: 低 | 状态: 已了结 (开关改写到 android 扩展上)**

**涉及文件**: `build-logic/convention/src/main/kotlin/net/imknown/android/forefrontinfo/android/Compose.kt`

**结论**: `Project.configureCompose()` 的 `when (this)` 判的是 **Project** 这个 receiver,  
两个分支写的是 `is ApplicationExtension` 与 `is LibraryExtension`, Gradle 的 `Project` 不实现这两个 android 扩展类型, 条件永不成立,  
分支里的 `buildFeatures { compose = true }` 从未被执行. 已用探针实证: 在两个分支与 `dependencies {}` 块各加一行打印,  
跑 `./gradlew help --no-configuration-cache`, 只有 `dependencies {}` 的打印出现, 两个分支的始终没有.

**证据**:

- 函数仍在被调用, 不是废弃代码: `AndroidApplicationComposeConventionPlugin.apply()` 先  
  `apply(plugin = libsKotlin.findPluginId("compose"))` (即 `org.jetbrains.kotlin.plugin.compose`), 再调  
  `configureCompose()` (修前签名), 而该约定插件由 `app/build.gradle.kts` 的 plugins 块应用;
- 同一个函数里 `dependencies {}` 那块是正常执行的  
  (compose BOM 平台 + `findBundle("compose")` + `debugImplementation` 的 `compose-ui-tooling`),  
  所以死掉的只有 `when` 那一半;
- Compose 之前能编译, 机制在 AGP 一侧: `BuildFeatureValuesImpl.kt` (AGP 9.4.1) 把有效值算成  
  `hasPlugin(COMPOSE_COMPILER_PLUGIN_ID) || (buildFeatures.compose ?: false)`, 插件在位时 DSL 值根本不参与,  
  显式写 `false` 还会触发一条 "Compose feature will be turned on" 警告.  
  所以这段死代码 "看起来在做的事", 实际是 AGP 这条规则在做.

**修法 (已实施)**: 需求本意是 "用到 Compose 的模块都显式写明 `compose = true`", 所以不是纯删, 而是把开关真的写下去:  
`configureCompose()` 改成泛型函数 `internal inline fun <reified T : CommonExtension> Project.configureCompose()`,  
函数内部用 `configure<T> {}` 自己取 android 扩展, 只写一句 `buildFeatures.compose = true`.  
属性赋值而不是 lambda 形状是上界决定的: `CommonExtension` 只暴露 `getBuildFeatures()`,  
`buildFeatures {}` lambda 只声明在 `ApplicationExtension` 与 `LibraryExtension` 上,  
按静态类型 `T` (上界 `CommonExtension`) 写不出 lambda 形状, 这一点用 `gradle-api-9.4.1.jar` 核对过.  
`inline` 与 `reified` 是必要的而不是风格选择: Gradle 的 `configure` 编译后只接 Project 与 lambda,  
参数表里没有 `Class` 也没有 `KClass`, 类型只能靠 reified 给出.

模块类型不再由函数内部判, 而是由两个插件应用处作为类型参数给出: `AndroidApplicationComposeConventionPlugin`  
调 `configureCompose<ApplicationExtension>()`, 新增的 `AndroidLibraryComposeConventionPlugin`  
调 `configureCompose<LibraryExtension>()`.  
两个类除包名, 类名, 类型参数与对应的扩展类型导入外逐行相同.  
传具体类型而不是 `configureAndroid<CommonExtension>()` 那种写法是刻意的: 把 app 侧 compose 插件误 apply 到  
library 模块会当场抛错, 传 `CommonExtension` 则会静默配好, 把一个真错误咽下去.

**顺序前提 (有意不兜)**: 两个 Compose 约定插件都只管 Compose, 不交叉 apply `com.android.application` 或  
`com.android.library`; 这与 NowInAndroid 不同, 那边的 compose 约定插件自己先把前置 android 插件 apply 掉.  
所以宿主模块必须在同一个 plugins 块里先应用 `lowleveldetector.android.application` 或  
`lowleveldetector.android.library`,  
再应用对应的 compose 插件; 漏写或顺序颠倒时 `configure<T>` 在配置期抛 `UnknownDomainObjectException`,  
不再像修前那样静默失效. 让错误暴露是有意选择, 不去兜底.  
library 侧插件已注册 (`lowleveldetector.android.library.compose`) 但暂无模块应用,  
因此这条抛错路径今天没被实测过, 第一次有 library 模块 apply 它时才是第一次执行.

**验证**: `./gradlew assembleFossDebug` 通过; `./gradlew -p build-logic :convention:validatePlugins`  
(该任务开着 `enableStricterValidation` 与 `failOnWarning`) 通过; 另用一个 init script 在 `afterEvaluate`  
里反射读 `getBuildFeatures().getCompose()`, 实测打印 `:app buildFeatures.compose = true`.  
显式写 `compose = true` 不改变有效行为 (上面那条 AGP 的 OR 规则), 本次修掉的是死分支对后来者的误导;  
探针读的是 DSL 值本身, 也就是验证那句赋值确实落到扩展对象上.
