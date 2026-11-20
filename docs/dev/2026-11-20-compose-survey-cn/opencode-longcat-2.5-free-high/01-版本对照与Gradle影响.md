# 01-版本对照与Gradle影响

> **状态**: **frozen**.

## BOM 与版本对照

| 模块 | 现在 | 升级后 |
|---|---|---|
| compose ui, ui-graphics, ui-text, ui-unit, ui-test, ui-tooling, ui-tooling-preview | 1.12.1 | 1.13.0 |
| foundation, foundation-layout | 1.12.1 | 1.13.0 |
| animation, animation-core, animation-graphics | 1.12.1 | 1.13.0 |
| runtime, runtime-saveable | 1.12.1 | 1.13.0 |
| material3 | 1.4.0 | 1.5.0 |
| material (Material 2) | 本仓库未用 | BOM 仍列 1.13.0, 该库已弃用 |

"升级后"这一列核过官方发布博客与 BOM mapping 页.  
"现在"这一列抄自仓库文档 (`docs/dev/issues-cn` 的 #37, #59, #60, 其中 #60 正文记 foundation 1.12.1), 没有逐列去核旧 BOM 的 mapping.  
核实程度: 升级后一列当场核过; 现在一列转述未核.

## Gradle 配置

- 真值源: `gradle/toml/android.toml` 的 `compose-bom` 键.  
  核实程度: 当场核过.
- `build-logic/convention/src/main/kotlin/net/imknown/android/forefrontinfo/android/Compose.kt` 只读 `compose-bom` 这一个 library, 具体 compose 库全不锁版本, 由 BOM 统管.  
  核实程度: 当场核过 (该文件 21-27 行).
- 升级动作只有把 `compose-bom` 从 2026.09.00 改成 2026.11.01 一行.  
  核实程度: 当场核过.
- 不用加插件, 不用加依赖. `foundation` 与 `androidx.graphics:graphics-shapes` 走传递依赖.  
  核实程度: 当场核过 (官方发布博客与 material3 1.5.0 release notes).

## 工具链要求

- 官方要求: Kotlin 2.2+, compileSdk 37.1.  
  出处: compose-ui 1.13.0-alpha03 的 I05b0f 条目; 官方发布博客也写了.  
  核实程度: 当场核过.
- 本仓库: Kotlin 2.5.0-RC (`gradle/toml/kotlin.toml`), compileSdk 37.2 (`gradle/toml/build.toml` 的 `compileSdk` 加 `compileSdkMinor`, 由 `KotlinAndroid.kt` 的 `compileSdk` 块下发), AGP 9.5.1 (`gradle/toml/android.toml`).  
  核实程度: 当场核过.
- 结论: 三条都满足, 工具链不动.  
  核实程度: 当场核过.

## Metro

- `gradle/toml/thirdParty.toml` 的注释写明 Kotlin 与 Metro 要配套升. 这次不升 Kotlin, Metro 不动.  
  核实程度: 当场核过.
