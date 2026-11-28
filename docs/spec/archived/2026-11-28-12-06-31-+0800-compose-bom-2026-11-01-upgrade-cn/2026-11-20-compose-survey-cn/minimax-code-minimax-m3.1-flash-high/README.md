# Compose 2026.11.01 (1.13.0) 对本仓库的影响

> **状态**: **frozen**. 这份调查写在 2026-11-20, 结论不改, 后来的更正留在原处.

调查者: MiniMax Code  
模型: MiniMax-M3.1-Flash  
effort: high  
调查日期: 2026-11-20

## 调查范围

上游材料:

- Jetpack Compose November '26 release blog (2026-11-18)
- Compose BOM mapping (2026.09.00 与 2026.11.01)
- androidx release notes: compose-ui 1.13.0, compose-animation 1.13.0,  
  compose-foundation 1.13.0, compose-material3 1.5.0, compose-runtime 1.13.0, compose-material 1.13.0

仓库侧材料: `gradle/toml/*.toml`, `build-logic/convention`, `app/src/main/java/.../ui` 下的源码,  
以及 Google Maven 上的 `compose-bom` POM 与 AAR 元数据.

## 分篇索引

1. [01-版本映射与Gradle门槛.md](01-版本映射与Gradle门槛.md): BOM 前后差多少, 升级要满足什么条件, 改动落在哪个文件.
1. [02-行为变更与弃用.md](02-行为变更与弃用.md): 1.13.0 带进来的行为变化和弃用, 逐条对照本仓库的用法.
1. [03-新特性与可选用法.md](03-新特性与可选用法.md): 新 API, 新组件, 以及官方给出的更好的写法.
1. [04-本仓库影响与建议.md](04-本仓库影响与建议.md): 结论, 该做什么, 该跳过什么, 怎么验证.

## 核心结论

1. 升级门槛全部满足, 本仓库不用改 Gradle 结构, 改一行版本号即可.  
   当场核过. 见 [01-版本映射与Gradle门槛.md](01-版本映射与Gradle门槛.md).

1. 本仓库对 Compose 高级 API 的使用面很窄, 1.13.0 的破坏性变更没有一条落在现有代码上.  
   当场核过. 见 [02-行为变更与弃用.md](02-行为变更与弃用.md).

1. 真正会被感知到的是两处行为变化: `Modifier.clickable` 会开始播系统交互音效,  
   文字的单行行高优化默认开启. 当场核过. 见 [02-行为变更与弃用.md](02-行为变更与弃用.md).

1. 有四处写法可以换新的: 两处 `LazyColumn` 用 `LazyLayoutCacheWindow`,  
   多语言文本用 `LineHeightStyle.Mode.PerLine`,  
   以后写条件判断用 `computedStateOf` 而不是 `derivedStateOf`.  
   以后做退出动画用 `CapturedAnimatedVisibility`.  
   当场核过. 见 [03-新特性与可选用法.md](03-新特性与可选用法.md).

1. 有一条可以在同一次改动里顺手清掉: `AppRoot.kt:142` 的 `@OptIn(ExperimentalMaterial3Api::class)`  
   和它那句注释. `TopAppBar` 在 Material 3 1.5.0 已经稳定. 当场核过.  
   见 [04-本仓库影响与建议.md](04-本仓库影响与建议.md).

1. 本次调查没有跑构建, 也没有编译验证. 结论来自 release notes, POM, AAR 元数据和源码检索.  
   当场核过.
