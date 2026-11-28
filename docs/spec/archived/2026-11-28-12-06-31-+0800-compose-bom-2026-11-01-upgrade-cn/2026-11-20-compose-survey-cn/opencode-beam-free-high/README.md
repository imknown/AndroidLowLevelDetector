# Compose 2026.11.01 (1.13.0) 影响调查

> **状态**: **frozen**. 调查者: OpenCode. 模型: Beam Free. effort: high. 调查日期: 2026-11-20.

本调查聚焦 Jetpack Compose 2026.11.01 (BOM 2026.11.01, Compose 1.13.0, Material3 1.5.0) 对本仓库的影响. 本仓库当前锁定 BOM 2026.09.00 (Compose 1.12.1), Kotlin 2.5.0-RC, compileSdk 37.2.

## 分篇索引

- [01-构建与Gradle影响](01-构建与Gradle影响.md)
- [02-行为变更与弃用](02-行为变更与弃用.md)
- [03-新特性与可选采用](03-新特性与可选采用.md)
- [04-本仓库影响评估](04-本仓库影响评估.md)

## 核心结论

- 升级 BOM 到 2026.11.01 后, Compose 1.13.0 要求 Kotlin 2.2+ 与 compileSdk 37.1. 本仓库 Kotlin 2.5.0-RC 与 compileSdk 37.2 已满足, 无需改动工具链 (当场核过: gradle/toml/kotlin.toml:3, build.toml:10-12).
- 本仓库未使用 value-based BasicTextField / TextField / VisualTransformation / Slider / ExposedDropdownMenu / FlowRow / FlowColumn / ConstraintLayout / MotionLayout / onFocusedBoundsChanged, 因此弃用与移除项无直接破坏 (当场核过: grep 全仓).
- Material3 1.5.0 将 value-based TextField/OutlinedTextField 标记为 deprecated, 但本仓库无 TextField 用法, 无影响.
- clickable 元素默认播放系统交互音效 (行为变更), 可能影响无障碍/音效测试, 需关注.
- 新特性 (computedStateOf, CapturedAnimatedVisibility, LazyLayoutCacheWindow 稳定, PolygonShape, progressive blur, FlexBox/Grid 稳定) 均为可选采用, 无强制迁移.
- 建议: 升级 BOM 锁值为 2026.11.01; 其他保持现状, 新特性按需引入.