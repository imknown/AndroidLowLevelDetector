# Compose 2026.11.01 (1.13.0) 影响调查

> **状态**: **frozen**. 调查者: OpenCode. 模型: LongCat 2.5 Free. effort: high. 调查日期: 2026-11-20.

本调查聚焦 Jetpack Compose 2026.11.01 (BOM 2026.11.01, Compose 1.13.0, Material3 1.5.0) 对本仓库的影响.  
本仓库当前锁定 BOM 2026.09.00, Kotlin 2.5.0-RC, compileSdk 37.2, AGP 9.5.1.

## 分篇索引

- [01-版本对照与Gradle影响](01-版本对照与Gradle影响.md)
- [02-破坏性变更核对](02-破坏性变更核对.md)
- [03-行为变更核对](03-行为变更核对.md)
- [04-新特性与实验性功能](04-新特性与实验性功能.md)
- [05-更好的写法与改动建议](05-更好的写法与改动建议.md)
- [06-与已有issue的关系](06-与已有issue的关系.md)
- [07-风险验证与结论](07-风险验证与结论.md)

## 核心结论

- 升级只需改 `gradle/toml/android.toml` 的 `compose-bom` 一行, 从 2026.09.00 改成 2026.11.01.  
  核实程度: 当场核过.
- Kotlin 2.5.0-RC 与 compileSdk 37.2 满足 Compose 1.13 的要求 (Kotlin 2.2+, compileSdk 37.1), 工具链不动.  
  核实程度: 当场核过.
- 官方 `Modifier.nonInteractiveScrollbar` 在 material3 1.5.0 到位, 可换掉自绘的 `ScrollBarExt.kt`.  
  核实程度: 当场核过 (官方 API 参考页 `NonInteractiveScrollbarDefaults` 标着 Added in 1.5.0).
- `TopAppBar` 在 material3 1.5.0 转正, `AppRoot.kt:142` 的 `@OptIn(ExperimentalMaterial3Api::class)` 可删.  
  核实程度: 当场核过.
- `Modifier.clickable` 默认播系统交互音效, 是用户可感知的行为变更.  
  核实程度: 当场核过 (官方发布博客).
- 1.13.0 与 material3 1.5.0 的移除与弃用清单逐条核过, 本仓库零命中.  
  核实程度: 当场核过 (全仓 grep).
- issue #59 (Expressive 主题) 可以开始; issue #37 只解决自绘换官方这一半; issue #60 仍等.  
  核实程度: 当场核过.
