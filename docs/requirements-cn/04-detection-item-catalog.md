# 检测条目目录

> **状态**: **living**, [需求文档](README.md) 的一篇.

应用到底检查些什么, 逐条列在这里, 顺序就是每个界面产生行的顺序. 这份目录是  
[FR-1](02-functional-requirements.md) 与 [FR-2](02-functional-requirements.md) 的范围:  
FR-1 对应 Home 那些带结论的行, FR-2 对应其余全部.

**计数规则**: 按检测条目数, 不按界面行数: Home 23, Others 37, Prop 5, **共 65 条**.  
Home 界面显示 25 行, 因为有两个条目各产两行. Others 的分区指纹每个分区产一行,  
Prop 每条属性, 每个设置项, 每行 build.prop 各产一行, 所以这两屏的行数随设备而定.

**结论范围**: 三态 (FR-3) 与结论圆点 (FR-15) 只适用于 **Home** 的条目. Others 与 Prop 是纯展示:  
信息行, 没有结论也没有颜色.

## Home (23 条)

| # | 条目 | 说明 |
|---|---|---|
| 1 | 检测模式 (在线 / 离线) | 最上面那一行; 它反映网络开关以及用的是哪份数据 |
| 2 | Android 版本 | |
| 3 | SDK 扩展版本 | |
| 4 | Build ID | |
| 5 | 安全补丁 | 两行: 平台级别, 然后是厂商级别 |
| 6 | 性能等级 | |
| 7 | 内核 | |
| 8 | A/B 更新 | |
| 9 | system-as-root (SAR) | |
| 10 | 动态分区 | |
| 11 | Treble 与 GSI 兼容性 | 两行: Treble, 然后是 GSI |
| 12 | DSU (动态系统更新) | |
| 13 | Mainline (Google Play 系统更新) | |
| 14 | VNDK (厂商 NDK) | |
| 15 | APEX (扁平化) | |
| 16 | 开发者选项 | |
| 17 | ADB | |
| 18 | ADB 认证 | |
| 19 | 加密 | |
| 20 | SELinux | |
| 21 | Toybox | |
| 22 | WebView 实现 | |
| 23 | 过期 targetSdk 应用 | 它的行就是设置里排序开关重排的那一行 (FR-17) |

## Others (37 条)

| # | 条目 | 说明 |
|---|---|---|
| 1 | 品牌 | |
| 2 | 厂商 | |
| 3 | 型号 | |
| 4 | 设备 | |
| 5 | 产品 | |
| 6 | 硬件 | |
| 7 | 主板 | |
| 8 | SoC 型号 | Android 12+ |
| 9 | SoC 厂商 | Android 12+ |
| 10 | SKU | Android 12+ |
| 11 | 厂商 SKU | Android 12+ |
| 12 | ODM SKU | Android 12+ |
| 13 | Binder 状态 (binder 驱动) | |
| 14 | 进程位数 | |
| 15 | 进程与虚拟机架构 | |
| 16 | CPU ABI (构建字段) | |
| 17 | CPU ABI (系统属性) | |
| 18 | 支持的 32 位 ABI | |
| 19 | 支持的 64 位 ABI | |
| 20 | 构建用户 | |
| 21 | 构建主机 | |
| 22 | 构建时间 | |
| 23 | 基础系统 | |
| 24 | 指纹 | |
| 25 | 预览 SDK 指纹 | Android 10+ |
| 26 | 各分区指纹 | 每个分区一行 |
| 27 | Build Id | |
| 28 | Display | |
| 29 | 构建类型 | |
| 30 | 构建标签 | |
| 31 | Incremental | |
| 32 | 版本代号 | Android 13 及以上的开发者预览版会多一个后缀 |
| 33 | 预览 SDK 版号 | |
| 34 | 默认 user agent | |
| 35 | 内核版本 | |
| 36 | Bootloader | |
| 37 | 基带版本 | |

## Prop (5 条)

| # | 条目 | 说明 |
|---|---|---|
| 1 | 系统属性 (`getprop`) | 每条属性一行, 按键排序 |
| 2 | `Settings.System` 表 | 每个键一行 |
| 3 | `Settings.Secure` 表 | 每个键一行 |
| 4 | `Settings.Global` 表 | 每个键一行 |
| 5 | build.prop | `getprop` 输出里的每一行各一行 |

## 条目模板

新增或调整一个条目时, 它带这些字段:

| 字段 | 含义 |
|---|---|
| **id** | 条目的稳定标识 |
| **screen** | Home, Others 或 Prop |
| **title** | 用户看到的名字 |
| **question** | 这个条目回答什么问题, 用大白话说 |
| **techniques** | 这个条目允许用的检测手段 (FR-9) |
| **tri-state semantics** | 什么算支持, 不支持, 未知, 以及各自取什么颜色, 由该条目自己的语义决定 (FR-3, FR-15, NFR-7) |
