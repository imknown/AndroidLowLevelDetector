# AGP 9.1 到 9.6 与 10.0 的调查和仓库核对

> **状态**: **frozen**. 写下的结论不改; 后来的更正留在原处.
> 里面记的是调查日 (2026-11-04) 查到的现状和结论, 附出处的证据.
> 这些笔记不规定代码该怎么做; 要变成规矩, 另外写进对应的规则文件.

- 调查者: TRAE CN
- 模型: GLM-5.3-Flash
- effort: high
- 调查日期: 2026-11-04

Android Gradle Plugin 下面简称 AGP, Kotlin Gradle Plugin 下面简称 KGP, 都是首次出现时的定义.

## 分篇索引

| 分篇 | 内容 |
|---|---|
| [01](01-agp-9-1-to-9-6.md) | AGP 9.1 到 9.6 逐版本的状态, 新特性与破坏性变化, 以及官方迁移工具 |
| [02](02-agp-compatibility-kgp.md) | AGP 9.x 全线兼容矩阵, KGP 耦合机制, 与 9.0 基线的行为变化 |
| [03](03-agp-10-0.md) | AGP 10.0 的存在性核查, 官方 roadmap 的移除清单与迁移步骤 |
| [04](04-repo-check.md) | 本仓库构建现状对照 9.1 到 9.6 与 10.0 要求的逐条判定 |

## 核心结论

1. 截至调查日, AGP 9.x 线的最新稳定版是 9.5.0 (2026 年 10 月与 Android Studio Rabbit 2 同发);  
   9.6 只有 9.6.0-alpha02, 没有 beta, RC 或稳定版.
   核实程度: 转述未核, 出处见 [01](01-agp-9-1-to-9-6.md).
1. AGP 10.0 连 alpha01 都没有上 Google Maven, Google Maven 上 com.android.tools.build:gradle  
   的最新值就是 9.6.0-alpha02; 10.0 只存在于官方 DSL/API 迁移时间线里, 定位 "late 2026".
   核实程度: 转述未核, 出处见 [03](03-agp-10-0.md).
1. 本仓库的构建栈满足 AGP 9.1 到 9.6 全线的最低要求 (Gradle, 两套 JDK, KGP, NDK, CMake 都达标),  
   9.x 线内没有任何待迁移项.
   核实程度: 当场核过, 证据见 [04](04-repo-check.md).
1. 本仓库已经走新 DSL 与内置 Kotlin, 没有设置 `android.newDsl` 或 `android.builtInKotlin`  
   退路旗标, 满足官方给的 10.0 前置条件.
   核实程度: 当场核过 (仓库侧) 加转述未核 (前置条件本身, 抄自官方 roadmap).
1. 对照 AGP 10.0 的移除清单, 全仓库只有一处命中: 约定插件里一处急切写入的 `buildConfigField()`,  
   roadmap 点名它要换成惰性 `MapProperty`. 这是本仓库唯一的 10.0 迁移点.
   核实程度: 当场核过 (仓库侧) 加转述未核 (roadmap 移除清单, 抄自官方 roadmap 页).
1. AGP 9.1 的 R8 默认重打包对本仓库无影响: 全部反射调用点的目标都是框架类,  
   没有对本仓库自身类的反射, 也没有 ServiceLoader.
   核实程度: 当场核过, 证据见 [04](04-repo-check.md).
