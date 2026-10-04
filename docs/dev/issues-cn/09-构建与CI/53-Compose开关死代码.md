<a id="i53"></a>

# 53 configureCompose() 的 Compose 开关是死代码

> 返回 [README 索引](../README.md) · [9 · 构建与 CI](../README.md#9--构建与-ci).

**严重程度: P2 | 修复难度: 低 (纯删)**

**影响文件**: `build-logic/convention/src/main/kotlin/net/imknown/android/forefrontinfo/android/Compose.kt`

**结论**: `Project.configureCompose()` 的 `when (this)` 判的是 **Project** 这个 receiver, 两个分支写的是 `is ApplicationExtension` 与 `is LibraryExtension` — Gradle 的 `Project` 不实现这两个 android 扩展类型, 条件永不成立, 分支里的 `buildFeatures { compose = true }` 从未被执行.

**证据**:

- 函数仍在被调用, 不是废弃代码: `AndroidApplicationComposeConventionPlugin.apply()` 先 `apply(plugin = libsKotlin.findPluginId("compose"))` (即 `org.jetbrains.kotlin.plugin.compose`), 再调 `configureCompose()`, 而该约定插件由 `app/build.gradle.kts` 的 plugins 块应用;
- 同一个函数里 `dependencies {}` 那块是正常执行的 (compose BOM 平台 + `findBundle("compose")` + `debugImplementation` 的 `compose-ui-tooling`), 所以死掉的只有 `when` 那一半;
- 全仓 `buildFeatures` 只出现在两处: `app/build.gradle.kts → android { buildFeatures { ... } }` 里唯一的 `buildConfig = true`, 以及这段死代码自己的两个分支 — 也就是说 `compose = true` 在任何地方都没有真正被设过. Compose 依然能编译, 机制是 KGP 在 `org.jetbrains.kotlin.plugin.compose` 被应用时自行打开该开关 — 这段代码 "看起来在做的事", 实际由 Kotlin 插件完成.

**修复方向**: 删掉整个 `when`. 真要显式打开时, 写在 android 扩展上 (`configure<ApplicationExtension> { buildFeatures.compose = true }`), 而不是 `Project` receiver 上. 值得删的理由不只是 "多几行": 后来者照这个形状给库模块配 Compose, 得到的是一份永不生效的配置, 而且失败是静默的.
