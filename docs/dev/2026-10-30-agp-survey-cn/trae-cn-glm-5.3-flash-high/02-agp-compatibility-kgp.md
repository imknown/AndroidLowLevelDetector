# AGP 9.x 全线兼容矩阵, KGP 耦合与 9.0 基线

> **状态**: **frozen**. 写下的结论不改; 后来的更正留在原处.
> 逐版本新特性在 [01](01-agp-9-1-to-9-6.md), AGP 10.0 在 [03](03-agp-10-0.md).

本篇的事实一律是转述未核, 出处列在文末. 数据基准日: 2026-11-04.

## 兼容矩阵

Gradle 最低要求与 Studio 支持范围来自官方 About AGP 页 (页尾 Last updated 2026-10-28).

| AGP | Gradle 最低/默认 | JDK 最低 | Build Tools 默认 | NDK 默认 | 支持它的最低 Studio |
|---|---|---|---|---|---|
| 9.0 | 9.1.0 | 17 | 36.0.0 | 28.2.13676358 | Otter 3 FD (2025.2.3) |
| 9.1 | 9.3.1 | 17 | 36.0.0 | 28.2.13676358 | Panda 2 (2025.3.2) |
| 9.2 | 9.4.1 | 17 | 36.0.0 | 28.2.13676358 | Panda 4 (2025.3.4) |
| 9.3 | 9.5.0 | 17 | 36.0.0 | 28.2.13676358 | Quail 2 (2026.1.2) |
| 9.4 | 9.6.0 | 17 | 36.0.0 | 28.2.13676358 | Quail 4 (2026.1.4) |
| 9.5 | 9.6.0 | 17 | 36.0.0 | 28.2.13676358 | Rabbit 2 (2026.2.2) |
| 9.6 | 9.7.1 (镜像站; 主站旧抓取写 9.6.0) | 17 | 36.0.0 | 28.2.13676358 | Rabbit 3 canary (2026.2.3) |

Studio 兼容表里每版支持的是一段 AGP 区间, 例如 Rabbit 2 (2026.2.2) 支持 AGP 7.1 到 9.5.

API level 对工具的最低要求 (About AGP 页): API 37.0 需要至少  
Studio Panda 3 (2025.3.3 Patch 1) 与 AGP 9.1.1. 该表只列到 API 37.0,  
没有 37.1 或 37.2 的行.

Studio 的两条政策: 每个 Studio 版本支持最近 3 年内发布的 AGP;  
预览版 Studio 必须搭配同版本号的预览 AGP, 否则 Sync 失败.

## KGP 耦合

AGP 9.x 全线最要紧的机制, 出处是 AGP 9.0 release notes:

- AGP 9.0 起对 KGP 有运行时依赖, 默认 2.2.10. 构建脚本声明了更低版本会被强制抬升;  
  KSP 低于 2.2.10-2.0.2 同样被抬升.
- 想用更高版本的 KGP: 在顶层 `buildscript` 的 classpath 里声明  
  `org.jetbrains.kotlin:kotlin-gradle-plugin` 指定版本即可.
- 9.2.0 把内置 KGP 依赖升到 2.3.10 (fixed issue 483083119).
- 想降级 KGP: 必须先退出内置 Kotlin, 再用 strict version 声明, 最低可降到 2.0.0;  
  官方警告降级后可能与后续 9.x 小版本不兼容, 且 test fixtures 可能失效.

内置 Kotlin:

- AGP 9.0 起 `android.builtInKotlin` 默认为 true, 不再需要也不建议再挂  
  `org.jetbrains.kotlin.android` 插件; 旧 Kotlin 插件与新 DSL 不兼容.
- 两条退路: 整项目退出 (`android.builtInKotlin=false`), 或按模块  
  `enableKotlin = false` 给无 Kotlin 源码的模块关闭内置 Kotlin.  
  两条退路在 AGP 10.0 全部移除, 见 [03](03-agp-10-0.md).

## 9.0 基线的行为变化

从 8.x 升到 9.x 会先撞上 9.0 的一批默认值翻转, 与本调查相关的有:

- `android.newDsl` 默认转 true: 旧 DSL 类 (`BaseExtension` 一族) 不再可用;  
  `applicationVariants`, `variantFilter`, `dexOptions` 等按官方对照表换成  
  `androidComponents` API. 退路 `android.newDsl=false` 在 AGP 10.0 移除.
- NDK 默认版本改为 r28c.
- 库消费者的 compileSdk 不得低于库要求的最低值 (可用 `AarMetadata.minCompileSdk` 放宽).
- 一批 `gradle.properties` 默认值翻转, 包括 `android.useAndroidX`,  
  `android.uniquePackageNames`, `android.default.androidx.test.runner`,  
  `android.dependency.useConstraints`, `android.enableAppCompileTimeRClass`,  
  `android.sdk.defaultTargetSdkToCompileSdkIfUnset`,  
  `android.onlyEnableUnitTestForTheTestedBuildType`.
- R8 的 9.0 变更: 新配置项 `-processkotlinnullchecks`, 停止向 companion 方法传播 keep info,  
  默认源文件名改为 `r8-map-id-<MAP_ID>` 等.

官方没有独立的 9.0 迁移指南页面; 迁移指引写在 9.0 release notes 页内,  
工具是 AGP Upgrade Assistant 加 android/skills 仓库的 `agp-9-upgrade` skill.

## 核实程度与出处

本篇全部结论: **转述未核**. 抄自下列官方材料:

- 兼容矩阵: https://developer.android.com/build/releases/about-agp
- AGP 9.0 release notes (KGP 耦合与 9.0 基线):  
  https://developer.android.com/build/releases/agp-9-0-0-release-notes
- AGP 9.2 release notes (内置 KGP 升到 2.3.10):  
  https://developer.android.com/build/releases/agp-9-2-0-release-notes
- Gradle Plugin API 更新日志:  
  https://developer.android.com/studio/releases/gradle-plugin-api-updates
