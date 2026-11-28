# 01-构建与Gradle影响

> **状态**: **frozen**.

## BOM 与版本

- BOM 2026.11.01 对应 Compose 1.13.0, Material3 1.5.0 (转述未核: 官方 BOM mapping 页).
- 当前锁定: `gradle/toml/android.toml:22` = `compose-bom = "2026.09.00"` (当场核过).

## Kotlin 与 compileSdk 要求

- Compose 1.13 libraries target Kotlin 2.2, require kotlinc 2.2 (转述未核: compose-ui release notes 1.13.0-beta01).
- 要求 compileSdk 37.1 (转述未核: compose-ui release notes 1.13.0-alpha03, blog).
- 本仓库 Kotlin = "2.5.0-RC" (gradle/toml/kotlin.toml:3), compileSdk = "37", minor = "2" (build.toml:10-11). 满足 (当场核过).

## Gradle 配置影响

- 无需修改 Kotlin 版本或 compileSdk.
- 升级 BOM 锁值即可; 无新增插件或依赖 (当场核过: build-logic convention Compose.kt 仅读取 compose-bom library).

## Metro 兼容性

- Metro 与 Kotlin 版本强耦合; 本仓库 Metro 版本未变更. 由于 Kotlin 未升级, 无额外动作 (当场核过: 03-概览.md).