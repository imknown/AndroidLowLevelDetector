# 05 — 检测条目目录 (附录 A)

属于 "[LowLevelDetector 规格文档](README.md)".

| | |
|---|---|
| **文档** | 条目级的正式检测清单 |
| **状态** | **已定稿 (2026-09-22;Q1 解决).** 带顺序的正式条目清单, 照着负责人评审过的 `HomeViewModel.detect()` / `OthersViewModel.collectModels()` / `PropViewModel.collectModels()` 调用顺序抄录 (2026-09-05). 检测项本身没有问题, 逻辑稳定, **Compose 改版不会动检测项相关逻辑**. |
| **日期** | 2026-09-22 |
| **计数口径** | 按**检测器方法**计数: Home 23 + Others 37 + Prop 5 = **65 项**, 这是评审与排期用的单位, **不等于屏幕行数**. `HomeRepository.detectSecurityPatches()` 与 `HomeRepository.detectTrebleAndGsiCompatibility()` 各产 2 行 (所以 Home 屏上是 25 行); Others 的分区指纹与 Prop 的逐属性表按设备状态产动态多行. |

## 范围说明

三态判定 (FR-3) 和判定圆点 (FR-15) 只适用于 **Home** 条目; **Others 和 Prop 是纯展示** — 没有分级判定的信息行.

## 有序条目 (正式清单)

### Home(23 项)

顺序照着评审过的 `HomeViewModel.detect()` 抄; 没经负责人评审不得重排. 第 1 项是排在最前面的在线 / 离线模式行.

| # | 条目 | 来源调用 / 备注 |
|---|---|---|
| 1 | 检测模式 (在线 / 离线) | `detectMode`; 反映设置开关和数据源 (FR-16) |
| 2 | Android 版本 | `detectAndroid` |
| 3 | SDK 扩展版本 | `detectSdkExtension` |
| 4 | Build ID | `detectBuildId` |
| 5 | 安全补丁 | `detectSecurityPatches` |
| 6 | 性能等级 | `detectPerformanceClass` |
| 7 | 内核 | `detectKernel` |
| 8 | A/B(无缝更新) | `detectAb` |
| 9 | system-as-root(SAR) | `detectSar` |
| 10 | 动态分区 | `detectDynamicPartitions` |
| 11 | Treble 与 GSI 兼容性 | `detectTrebleAndGsiCompatibility` |
| 12 | DSU(动态系统更新) | `detectDsu` |
| 13 | Mainline(Google Play 系统更新) | `detectMainline` |
| 14 | VNDK(供应商 NDK) | `detectVndk` |
| 15 | APEX(扁平化) | `detectApex` |
| 16 | 开发者选项 | `detectDeveloperOptions` |
| 17 | ADB | `detectAdb` |
| 18 | ADB 授权 | `detectAdbAuthentication` |
| 19 | 加密 | `detectEncryption` |
| 20 | SELinux | `detectSELinux` |
| 21 | Toybox | `detectToybox` |
| 22 | WebView 实现 | `detectWebView` |
| 23 | 过期 targetSdk 应用 | `getOutdatedTargetSdkVersionApkModel`; 详情可以单独刷新 (属于 FR-13 说的单行刷新; 范围说明见 FR-14) |

### Others(37 项)

顺序照着评审过的 `OthersViewModel.collectModels()` 抄; 受 API 级别限制的条目标在备注里.

| # | 条目 | 来源调用 / 备注 |
|---|---|---|
| 1 | 品牌 | `getBrand` |
| 2 | 制造商 | `getManufacturer` |
| 3 | 型号 | `getModel` |
| 4 | 设备 | `getDevice` |
| 5 | 产品 | `getProduct` |
| 6 | 硬件 | `getHardware` |
| 7 | 主板 | `getBoard` |
| 8 | SoC 型号 | `getSocModel`;Android 12+ |
| 9 | SoC 制造商 | `getSocManufacturer`;Android 12+ |
| 10 | SKU | `getSku`;Android 12+ |
| 11 | 供应商 SKU | `getVendorSku`;Android 12+ |
| 12 | ODM SKU | `getOdmSku`;Android 12+ |
| 13 | Binder 状态 (binder 驱动) | `getBinderStatus` |
| 14 | 进程位数 | `getProcessBit` |
| 15 | 进程 / VM 架构 | `getArchitecture` |
| 16 | CPU ABI(Build 字段) | `getCpuAbi` |
| 17 | CPU ABI(系统属性) | `getPropertyCpuAbi` |
| 18 | 支持的 32 位 ABI | `getSupported32BitAbis` |
| 19 | 支持的 64 位 ABI | `getSupported64BitAbis` |
| 20 | 构建用户 | `getUser` |
| 21 | 构建主机 | `getHost` |
| 22 | 构建时间 | `getTime` |
| 23 | 基础 OS | `getBaseOs` |
| 24 | 指纹 | `getFingerprint` |
| 25 | 预览 SDK 指纹 | `getPreviewSdkFingerprint`;Android 10+ |
| 26 | 分区指纹 | `getPartitionFingerprints` |
| 27 | Build Id | `getId` |
| 28 | Display | `getDisplay` |
| 29 | 构建类型 | `getType` |
| 30 | 构建标签 | `getTags` |
| 31 | Incremental | `getIncremental` |
| 32 | 版本代号 | `getCodename` |
| 33 | 预览 SDK int | `getPreviewSdkInt` |
| 34 | 默认用户代理 | `getDefaultUserAgent` |
| 35 | 内核版本 | `getKernelVersion` |
| 36 | Bootloader | `getBootloader` |
| 37 | 基带版本 | `getRadioVersionOrNull` |

### Prop(5 项)

顺序照着评审过的 `PropViewModel.collectModels()` 抄.

| # | 条目 | 来源调用 / 备注 |
|---|---|---|
| 1 | 系统属性 (`getprop`) | `getSystemProp` |
| 2 | Settings.System 表 | `getSettings(Settings.System)` |
| 3 | Settings.Secure 表 | `getSettings(Settings.Secure)` |
| 4 | Settings.Global 表 | `getSettings(Settings.Global)` |
| 5 | build.prop | `getBuildProp` |

## 条目模板 (新增 / 调整条目时使用)

| 字段 | 含义 |
|---|---|
| **id** | 检测引擎注册表用的稳定标识符 |
| **screen** | Home / Others / Prop |
| **title** | 用户可见的名字 |
| **question** | 这个条目回答什么问题, 用大白话写 |
| **techniques** | 允许的检测层级 (FR-9: 公开 API / 反射 / shell / root-Shizuku) |
| **三态语义** | 什么算支持 / 不支持 / 未知, 以及对应的圆点颜色 — 按本条目自身语义规定 (见 FR-3, FR-15, NFR-7;Q9 已解决) |

条目和检测引擎注册表**一一对应** — 见 "[04 — 架构与决策](04-architecture-and-decisions.md)" 的检测引擎一节.
