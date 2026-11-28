# Compose 2026.11.01 (1.13.0) 对本项目的影响调查 (调查于 2026-11-20)

> **状态**: **frozen**. 本目录记下写下时查到的现状与结论, 不随代码演进更新.  
> 后来的更正写在原处, 不回改这里.

- 调查者: WorkBuddy
- 模型: GLM-5.3-Flash
- effort: high
- 调查日期: 2026-11-20

## 分篇索引

1. [01-升级门槛与Gradle配置](01-升级门槛与Gradle配置.md): 升级的四个硬性门槛与本项目构建配置的逐条核对, 以及升级要动的唯一构建文件.
1. [02-源码影响与弃用核对](02-源码影响与弃用核对.md): 全仓库 Compose API 用法盘点, 唯一的可清理点, 这版弃用清单的零命中核对.
1. [03-行为变化与新特性](03-行为变化与新特性.md): 升级后可见的行为变化, 转正的 API, 新写法里对本项目有意义的与用不上的.
1. [04-待负责人决定的事](04-待负责人决定的事.md): 升级与否等三项决定, 逐条摆开, 附执行路径与验证顺序.

## 核心结论

1. 升级只改 `gradle/toml/android.toml` 的 `compose-bom` 条目一处构建配置.  
   四个硬性门槛 (Kotlin, compileSdk, AGP, lifecycle-runtime) 全部满足.  
   (当场核过: 01 节, 证据为各文件行号; 门槛数值本身转述未核, 出处见 01 节)
1. 源码层唯一可清理点是 `AppRoot.kt` 第 142 行的 `@OptIn(ExperimentalMaterial3Api::class)`:  
   Material3 1.5.0 把 `TopAppBar` 系转正, 升级后该注解与行尾注释都过时. 不删也能编译.  
   (当场核过: 文件行号; 转述未核: compose-material3 release notes 的 1.5.0 段)
1. 这版的大批弃用 (value 版 `BasicTextField`, `VisualTransformation`, Material2 库, Flow 布局,  
   Compose 版 `ConstraintLayout`) 本项目全部零命中, 无迁移成本.  
   (当场核过: 全仓库 grep, 见 02 节)
1. 升级后唯一要肉眼复核的可见变化是文本行高优化默认开启: 官方注记文本度量可能差几个像素,  
   本项目三个列表页和设置页全是文字密集的卡片.  
   (转述未核: compose-ui release notes [I5575a])
1. 官方新推荐的 `computedStateOf` (多数场景替代 `derivedStateOf`) 对本项目是新代码的默认选择,  
   没有存量迁移, 因为项目当前没有 `derivedStateOf` 使用点.  
   (当场核过: grep 零命中; 转述未核: compose-runtime release notes)
