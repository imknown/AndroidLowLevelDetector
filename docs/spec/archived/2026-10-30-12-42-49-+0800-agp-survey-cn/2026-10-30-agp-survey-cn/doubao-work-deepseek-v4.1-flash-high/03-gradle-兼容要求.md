# Java, Kotlin, Groovy, AGP 与 Develocity 的兼容边界

> **状态**: **frozen**. 写下的结论不改; 后来的更正留在原处.  
> 版本线在 [01](02-gradle-版本线.md), 仓库对照在 [03](04-仓库核对.md).

数据基准日: 2026-11-25. 全部抄自 Gradle 的 Compatibility Matrix 页, 各 AGP 发布说明页, Kotlin 官方的兼容表, 与 Develocity 插件手册.

## Java

Gradle 要求运行在 JVM 17 到 27 之间, 28 及以上还不支持.  
下表是 Gradle 官方测过、可以拿来跑 Gradle 或当工具链的 Java 版本:

| Java | 工具链支持起始 | 运行 Gradle 起始 |
|---|---|---|
| 27 | 9.8.0 | 9.8.0 起 |
| 26 | 9.4.0 | 9.4.0 起 |
| 25 | 9.1.0 | 9.1.0 起 |
| 24 | 8.14 | 8.14 起 |
| 23 | 8.10 | 8.10 起 |
| 22 | 8.7 | 8.8 起 |
| 21 | 8.4 | 8.5 起 |
| 17 | 7.3 | 7.3 起 |

核实程度: **已验证** (兼容矩阵页原文).

## 内嵌 Kotlin 与 Groovy

Gradle 自带一份 Kotlin 给构建脚本和 `kotlin-dsl` 用. 各版本对应的内嵌 Kotlin:

| 内嵌 Kotlin | 最低 Gradle 版本 | Kotlin 语言版本 |
|---|---|---|
| 2.4.21 | 9.9.0 | 2.2 |
| 2.4.10 | 9.8.0 | 2.2 |
| 2.4.0 | 9.7.0 | 2.2 |
| 2.3.21 | 9.6.0 | 2.2 |
| 2.3.20 | 9.5.0 | 2.2 |
| 2.3.0 | 9.4.0 | 2.2 |
| 2.2.21 | 9.3.0 | 2.2 |
| 2.2.20 | 9.2.0 | 2.2 |
| 2.2.0 | 9.0.0 | 2.2 |

Gradle 官方测过的 Kotlin Gradle 插件版本范围是 2.0.0 到 2.5.0-Beta1, 页面原文写 "Beta and RC versions may or may not work".  
内嵌 Groovy 从 9.8.0 起是 4.0.33, 从 9.6.0 起是 4.0.32; 测过的 Groovy 范围是 1.5.8 到 6.0.0.  
核实程度: **已验证** (兼容矩阵页原文).

## AGP

Gradle 兼容矩阵页的原话是 "Gradle is tested with Android Gradle Plugin 9.0 through 9.5.0-alpha10".  
这一行只说明 Gradle 自己测过哪些 AGP, 不代表更新的 AGP 不能用.  
反方向的约束在 AGP 的发布说明页: AGP 9.5 要求 Gradle 最低与默认都是 9.6.0; AGP 9.6 要求 Gradle 9.8.2.  
核实程度: **已验证** (两处原文) 加 **不确定** (超出这个范围的组合没有任何官方结论).

## KGP 对 Gradle 与 AGP 的官方上限

Kotlin 官方兼容表把 KGP 版本与 Gradle, AGP 的可用范围绑在一起. 表里最新的一行:

| KGP 版本 | Gradle 最低到最高 | AGP 最低到最高 |
|---|---|---|
| 2.4.20 到 2.4.21 | 7.6.3 到 9.7.0 | 8.5.2 到 9.3.1 |
| 2.4.0 到 2.4.10 | 7.6.3 到 9.5.0 | 8.5.2 到 9.1.0 |
| 2.3.20 到 2.3.21 | 7.6.3 到 9.3.0 | 8.2.2 到 9.0.0 |

表里没有 Kotlin 2.5.x 的行. 页面还写, 超出这张表也能用, 但可能会撞上弃用提示, 或者新功能不生效.  
核实程度: **已验证** (表原文) 加 **不确定** (2.5.x 的实际范围没有公布).

## Develocity 插件

插件门户页上 `com.gradle.develocity` 的最新版本是 4.6.1 (2026-10-20 发布).  
官方手册的 "Getting set up" 一节写插件 "is compatible with all Gradle 5.x and later versions", 而这一页列的版本号还停在 4.2.2, 页面本身没跟上.  
按手册那句政策, Gradle 9.9 在老版本兼容范围内.  
核实程度: 政策原文 **已验证**, 4.6.1 与 9.9.2 是否实测过 **不确定**.

## 出处

- Compatibility Matrix: https://docs.gradle.org/current/userguide/compatibility.html
- Configuration Cache: https://docs.gradle.org/current/userguide/configuration_cache.html
- AGP 9.5 发布说明: https://developer.android.com/build/releases/agp-9-5-0-release-notes
- AGP 9.6 发布说明: https://developer.android.com/build/releases/agp-9-6-0-release-notes
- Kotlin 的 Gradle 配置与兼容表: https://kotlinlang.org/docs/gradle-configure-project.html
- Develocity 插件手册: https://docs.gradle.com/develocity/gradle-plugin/current/
- Develocity 插件门户页: https://plugins.gradle.org/search?term=com
