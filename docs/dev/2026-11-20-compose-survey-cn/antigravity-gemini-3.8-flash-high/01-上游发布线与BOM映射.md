# 上游发布线与 BOM 映射

> **状态**: **frozen**. 本篇记录 2026 年 11 月 18 日发布的 Jetpack Compose 稳定版系列与 BOM 映射细节.

## 核心结论

1. Compose BOM `2026.11.01` 对应核心 Compose 库 `1.13.0` 与 Material 3 `1.5.0`.  
   核实程度: 当场核过 (通过官方发布页面与 `bom-mapping` 页面检索核对).
2. Material 2 (`androidx.compose.material:material`) 达到 `1.13.0` 后正式声明弃用, 停止功能维护.  
   核实程度: 当场核过 (核对官方博客说明与 `androidx.compose.material` 状态声明).
3. `ConstraintLayout for Compose` 与旧版基于值的输入框 API 同步进入弃用过渡期.  
   核实程度: 当场核过 (核对官方发布博客重点摘要).

## BOM 映射对照

上游在 2026 年 11 月 18 日推送 Compose November '26 稳定版发布线.  
在 Gradle 声明 `platform("androidx.compose:compose-bom:2026.11.01")` 时, 各子库解析版本如下:

| 库坐标 | BOM 2026.09.00 (仓库现状) | BOM 2026.11.01 (新版) | 状态变化 |
|---|---|---|---|
| `androidx.compose.ui:ui` | 1.12.x | 1.13.0 | 稳定版次版本升级 |
| `androidx.compose.ui:ui-graphics` | 1.12.x | 1.13.0 | 稳定版次版本升级 |
| `androidx.compose.ui:ui-tooling` | 1.12.x | 1.13.0 | 稳定版次版本升级 |
| `androidx.compose.ui:ui-tooling-preview` | 1.12.x | 1.13.0 | 稳定版次版本升级 |
| `androidx.compose.ui:ui-test-junit4` | 1.12.x | 1.13.0 | 稳定版次版本升级 |
| `androidx.compose.animation:animation` | 1.12.x | 1.13.0 | 稳定版次版本升级 |
| `androidx.compose.foundation:foundation` | 1.12.x | 1.13.0 | 稳定版次版本升级 |
| `androidx.compose.material3:material3` | 1.4.x | 1.5.0 | 稳定版次版本升级 |
| `androidx.compose.runtime:runtime` | 1.12.x | 1.13.0 | 稳定版次版本升级 |
| `androidx.compose.material:material` | 1.12.x | 1.13.0 | 标记弃用, 建议迁往 Material 3 |

## 外部官方参考链接

- 发布博客: `https://developer.android.com/blog/posts/jetpack-compose-november-2026-release`
- BOM 映射: `https://developer.android.com/develop/ui/compose/bom/bom-mapping`
- Compose UI: `https://developer.android.com/jetpack/androidx/releases/compose-ui#1.13.0`
- Compose Animation: `https://developer.android.com/jetpack/androidx/releases/compose-animation#1.13.0`
- Compose Foundation: `https://developer.android.com/jetpack/androidx/releases/compose-foundation#1.13.0`
- Compose Material 3: `https://developer.android.com/jetpack/androidx/releases/compose-material3#1.5.0`
- Compose Runtime: `https://developer.android.com/jetpack/androidx/releases/compose-runtime#1.13.0`
