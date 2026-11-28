# Compose 2026.11.01 (1.13.0) 影响评估与特性盘点

> **状态**: **frozen**. 本文档为 2026-11-20 针对 Jetpack Compose 2026.11.01 (1.13.0) 版本的独立技术调查汇总.

- **调查者**: Antigravity
- **模型**: Gemini 3.8 Flash
- **effort**: high
- **调查日期**: 2026-11-20

## 核心结论

1. **环境与配置零阻碍**:  
   Compose 1.13.0 要求的 Kotlin 2.2 与 `compileSdk 37.1`, 本仓库当前分别为 `2.5.0-RC` 与 `37.2` (含 minor 2), 工具链完全满足, 仅需改动 `gradle/toml/android.toml:22` 中的 `compose-bom` 单行版本.  
   核实程度: 当场核过 (通过对比官方版本要求与 `gradle/toml/build.toml`, `gradle/toml/kotlin.toml`).

2. **官方组件就位, 可直接消减代码**:  
   Material 3 1.5.0 提供了官方的 `Modifier.nonInteractiveScrollbar`, 可完全替换并删除本仓库预留的 `ScrollBarExt.kt` (净减 89 行); 同时 `TopAppBar` 稳定转正, 可直接清除 `AppRoot.kt:142` 的实验性注解.  
   核实程度: 当场核过 (核对官方 API 签名与 `ScrollBarExt.kt:30-34`, `AppRoot.kt:142-144`).

3. **零弃用包袱**:  
   本次上游正式弃用的 Compose Material 2 (`androidx.compose.material:material`), `ConstraintLayout for Compose`, 以及基于值的文本输入框 API, 本仓库均未采用, 升级不会引发警告或兼容负担.  
   核实程度: 当场核过 (grep 全仓依赖与代码实现核实).

4. **新状态机制与自适应布局**:  
   Compose Runtime 1.13.0 引入了无缓存记账开销的 `ComputedState`, Foundation 1.13.0 稳定了 `FlexBox` 与 `Grid`, 为后续性能调优和非惰性多维布局提供了系统级原生支持.  
   核实程度: 当场核过 (核对官方 Runtime 与 Foundation 发布日志).

## 分篇索引

- [01-上游发布线与BOM映射.md](01-上游发布线与BOM映射.md): 梳理 BOM 2026.11.01 包含的各核心库版本, 发布重点及外部参考链接.
- [02-构建配置与环境要求.md](02-构建配置与环境要求.md): 对照本仓库 Gradle 版本目录, 编译器与 SDK 配置, 说明升级所需的具体构建修改.
- [03-Material3与组件演进.md](03-Material3与组件演进.md): 分析 M3 1.5.0 稳定版带来的 API 转正, 官方滚动条接入以及代码清理点.
- [04-Foundation与Runtime新特性.md](04-Foundation与Runtime新特性.md): 拆解 `ComputedState`, 渐进模糊, 自适应布局 (`FlexBox`/`Grid`) 及渲染流水线改进.
- [05-本仓库影响与建议.md](05-本仓库影响与建议.md): 汇总对现有代码库的收益, 影响清单与三步走实施建议.
