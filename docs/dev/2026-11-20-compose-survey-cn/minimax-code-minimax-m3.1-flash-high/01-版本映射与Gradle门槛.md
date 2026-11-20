# 01 版本映射与 Gradle 门槛

> **状态**: **frozen**. 这份调查写在 2026-11-20, 结论不改, 后来的更正留在原处.

## BOM 前后差多少

`gradle/toml/android.toml:22` 现在写的是 `compose-bom = "2026.09.00"`.  
上游最新的是 `2026.11.01`.

两个 BOM 的 POM 逐条对比, 条目数从 247 变成 251, 版本变了的条目有 184 个. 当场核过.

| 模块 | 2026.09.00 | 2026.11.01 |
|---|---|---|
| compose ui / runtime / foundation / animation / material | 1.12.1 | 1.13.0 |
| material3, material3-window-size-class, material3-adaptive-navigation-suite | 1.4.0 | 1.5.0 |
| material3-ripple | 没有这个 artifact | 1.5.0 |

`material3-ripple` 是新增的 artifact, 用来画 inset 形式的焦点环, 替代 `material-ripple`. 当场核过.

这个 BOM 只管 Compose 自己的七个 group: `animation`, `foundation`, `material`, `material3`,  
`material3.adaptive`, `runtime`, `ui`. 当场核过.  
`activity`, `lifecycle`, `navigation3`, `core`, `annotation`, `savedstate`, `webkit`  
在 `gradle/toml/android.toml` 里各自有版本号, 不受这次升级影响.

## 升级要满足的条件

| 条件 | 上游要求 | 本仓库现状 | 位置 |
|---|---|---|---|
| Kotlin | Compose 1.13 的库编译目标改成 Kotlin 2.2, 要 kotlinc 2.2 | 2.5.0-RC | `gradle/toml/kotlin.toml:3` |
| compileSdk | Compose 库本身改用 37.1 编译 | 37.2 | `gradle/toml/build.toml:10` 与 `:11` |
| AGP | AAR 元数据里没有写 minAgpVersion | 9.5.1 | `gradle/toml/android.toml:7` |
| lifecycle-runtime | 2.10.0, AGP 9.5.0-alpha04 起的内置 lint 检查要这个版本 | 2.11.0 | `gradle/toml/android.toml:36` |

这四条里 Kotlin 和 compileSdk 是上游 blog 明写的要求. 当场核过.  
lifecycle-runtime 那条来自 compose-ui 1.13.0-beta01 和 compose-foundation 1.13.0-beta01 的 release notes. 当场核过.

compileSdk 这条要说清楚两层. 上游 release notes 写的是 "Compose 库的 compileSdk 更新到 37.1",  
意思是这些库是用 37.1 的 API 编译的. 当场核过.  
从 Google Maven 上取 `foundation-android-1.13.0.aar`, `material3-android-1.5.0.aar`,  
`ui-android-1.13.0.aar` 三个包, 它们 `META-INF/com/android/build/gradle/aar-metadata.properties`  
里写的都是 `minCompileSdk=37`. 当场核过.  
本仓库编译用的是 37.2, 两层要求都满足.

Material 3 自己还有一条: `material3` 1.5.0-alpha23 的 release notes 写着 "Remove requirement for `compileSdk 37`". 当场核过.  
这一条对本仓库没有影响, 37.2 本来就超过 37.

## 改动落在哪里

只改一行: `gradle/toml/android.toml:22`, 把 `compose-bom = "2026.09.00"` 改成  
`compose-bom = "2026.11.01"`.

约定插件 `build-logic/convention/src/main/kotlin/net/imknown/android/forefrontinfo/android/Compose.kt`  
会把这个 BOM 作为 `platform()` 加进每个应用该加的模块, 还会加 `compose` bundle 里的库.  
Gradle 侧不用动别的地方. 当场核过.

升级之后, `compose` bundle 里的库会落到这些版本:

| bundle 里的条目 | 升级后 |
|---|---|
| `compose-ui`, `compose-ui-graphics`, `compose-ui-tooling-preview` | 1.13.0 |
| `compose-material3` | 1.5.0 |
| `activity-compose`, `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose` | 不变, 由 `activity` 和 `lifecycle` 两个版本号管 |
| `navigation3-runtime`, `navigation3-ui`, `lifecycle-viewmodel-navigation3` | 不变, 同上 |

`compose-ui-tooling` 是 `debugImplementation`, 也跟着到 1.13.0. 当场核过.
