# 检测条目目录

> **状态**: **living**, 属于[需求文档](README.md).

逐条列出应用检查什么, 按每屏产生行的顺序. 这份目录是 [FR-1](02-functional-requirements.md) 和  
[FR-2](02-functional-requirements.md) 的范围: FR-1 是 Home 条目, 它们的行带结论;  
FR-2 是其余全部.

**计数规则**: 按检测条目计数, 不按屏幕行数: Home 23, Others 37, Prop 5, **共 65**.  
Home 显示 25 行, 因为有两个条目各产生两行. Others 的分区指纹按分区各一行,  
Prop 按属性, 设置项或 build.prop 行各一行, 所以这两屏的行数取决于设备.

**结论范围**: 三态 (FR-3) 和结论圆点 (FR-15) 只适用于 **Home** 条目.  
Others 和 Prop 是纯展示: 信息行, 没有结论, 没有颜色.

**条目 id**: `HOME-01` ... `HOME-23`, `OTH-01` ... `OTH-37`, `PROP-01` ... `PROP-05`.  
编号是该条目在所属屏列表里的位置, 而一屏的列表顺序就是该功能 ViewModel 构建列表的顺序:  
Others 与 Prop 是 `collectModels()`, Home 是它调用的 `detect()`.  
新条目追加到列表末尾时取下一个编号; 插进中间时, 它后面条目的编号依次顺延,  
所有引用这个 id 的文档要在同一次改动里一起更新.

**失败行**: 每个 Home 条目的失败行都是红色, 错误文字写在这一行的内容里 (FR-6).  
所以下面的定义只列该条目自己区分的结果.

## Home (23 条)

| id | 条目 | 备注 |
|---|---|---|
| HOME-01 | 检测模式 (在线 / 离线) | 列表最上面的一行; 它反映网络开关和这次用了哪份数据 |
| HOME-02 | Android 版本 | |
| HOME-03 | SDK 扩展版本 | |
| HOME-04 | Build ID | |
| HOME-05 | 安全补丁 | 两行: 先是平台级别, 再是厂商级别 |
| HOME-06 | 性能等级 | |
| HOME-07 | 内核 | |
| HOME-08 | A/B 更新 | |
| HOME-09 | system-as-root (SAR) | |
| HOME-10 | 动态分区 | |
| HOME-11 | Treble 与 GSI 兼容性 | 两行: 先是 Treble, 再是 GSI |
| HOME-12 | DSU (动态系统更新) | |
| HOME-13 | Mainline (Google Play 系统更新) | |
| HOME-14 | VNDK (vendor NDK) | |
| HOME-15 | APEX (flattened) | |
| HOME-16 | 开发者选项 | |
| HOME-17 | ADB | |
| HOME-18 | ADB 认证 | |
| HOME-19 | 加密 | |
| HOME-20 | SELinux | |
| HOME-21 | Toybox | |
| HOME-22 | WebView 实现 | |
| HOME-23 | targetSdk 过旧的应用 | 设置里的排序开关只重排这一行 (FR-17) |

### Home 条目定义

#### HOME-01 检测模式 (在线 / 离线)

- **问题**: 这次检测用的是哪份参考数据, 它是哪一天的?
- **手段**: 应用自己的参考数据, 来自服务器, 私有缓存或内置 assets (FR-16).
- **三态语义**: 绿: 服务器 json 已取到, 解析成功并用于本次检测;  
  红: 本次用的是私有缓存或内置 json, 包括在线尝试失败后继续离线的情况;  
  红: 一份可用的 json 都没有, 版本位置显示 `Unknown`. 这个条目没有琥珀色结果.

#### HOME-02 Android 版本

- **问题**: 这个 Android 版本是最新的吗, 上游还支持它吗?
- **手段**: 公开 SDK API (`Build.VERSION`), 与参考数据比对.
- **三态语义**: 绿: 设备运行的是最新稳定版或最新预览版;  
  琥珀: 不是最新, 但上游仍支持; 红: 上游已不支持, 以及没有参考数据可比对时.

#### HOME-03 SDK 扩展版本

- **问题**: 设备是否支持当前稳定版 Android 的 SDK 扩展级别?
- **手段**: 公开 SDK API (`Build.VERSION.SDK_INT` 和 SDK 扩展级别), 与参考数据比对.
- **三态语义**: 绿: 设备的扩展级别达到或超过稳定版的扩展级别;  
  红: 低于它, 或设备太旧没有 SDK 扩展特性 (Android 10 及以下), 或没有参考值.  
  这个条目没有琥珀色结果.

#### HOME-04 Build ID

- **问题**: 这个构建是否和参考数据里该 Android 版本的构建一样新?
- **手段**: 公开 SDK API (`Build.ID`), `ro.system.build.id` / `ro.vendor.build.id` /  
  `ro.odm.build.id` 属性, 与参考数据比对.
- **三态语义**: 绿: 构建日期不早于该版本参考数据里的第一条构建;  
  琥珀: 设备运行最新稳定版或预览版, 但构建日期更早; 红: 其余情况, 以及没有参考数据时.

#### HOME-05 安全补丁 (两行)

- **问题**: 平台安全补丁级别和厂商安全补丁级别是否达到参考级别?
- **手段**: 公开 SDK API (`Build.VERSION.SECURITY_PATCH`) 和 `ro.vendor.build.security_patch`  
  属性, 与参考数据比对.
- **三态语义** (每一行): 绿: 设备的级别达到或超过参考级别;  
  琥珀: 低于参考级别, 但落在同一年月; 红: 更低且不是同一年月, 值为空, 或没有参考值.

#### HOME-06 性能等级

- **问题**: 设备声明的是哪一档媒体性能等级?
- **手段**: 公开 SDK API (`Build.VERSION.MEDIA_PERFORMANCE_CLASS`).
- **三态语义**: 绿: 声明的等级等于设备的 SDK 级别; 琥珀: 比它低一级;  
  红: 更低, 没有声明等级, 或设备太旧没有这个字段 (Android 11 及以下).

#### HOME-07 内核

- **问题**: Linux 内核是否达到或超过 Android 上游为同一主次版本号提供的版本?
- **手段**: `os.version` 系统属性, 与参考数据比对.
- **三态语义**: 绿: 内核达到或超过同一主次版本号的支持版本;  
  琥珀: 主次版本号相同但版本更低; 红: 参考数据里没有相同主次版本号, 以及没有参考数据时.

#### HOME-08 A/B 更新

- **问题**: 设备是否使用无缝 A/B 更新?
- **手段**: `ro.build.ab_update`, `ro.boot.slot_suffix` 和相关的 virtual A/B 属性.
- **三态语义**: 绿: 所查属性里有任何一个表明 A/B 已开启, 这一行还会写出当前 slot  
  以及 virtual A/B 是否为 retrofit; 红: 它们都没有表明. 这个条目没有琥珀色结果.

#### HOME-09 system-as-root (SAR)

- **问题**: 系统是否挂载为根, 是哪种布局?
- **手段**: `ro.build.system_root_image` 属性和挂载表.
- **三态语义**: 绿: 布局是 2SI 或 `/` 挂载; 琥珀: legacy SAR 或 recovery SAR;  
  红: 设备不是 system-as-root, 以及 Android 8 及以下, 那里这项检测不适用.

#### HOME-10 动态分区

- **问题**: 设备是否使用动态分区?
- **手段**: `ro.boot.dynamic_partitions` 和 `ro.boot.dynamic_partitions_retrofit` 属性.
- **三态语义**: 绿: 动态分区已开启, 或者是 retrofit 来的; 红: 两者都不成立.  
  这个条目没有琥珀色结果.

#### HOME-11 Treble 与 GSI 兼容性

- **问题**: 设备是否支持 Project Treble, 是否兼容通用系统镜像 (GSI)?
- **手段**: Treble 行用 `ro.treble.enabled` 属性和磁盘上的 vendor VINTF 文件;  
  GSI 行用读取 vendor namespace 配置的 shell 命令.
- **三态语义** (两行):
  - **Treble 行**: 绿: Treble 已开启, 且存在 vendor manifest, manifest 片段或 SKU manifest;  
    琥珀: Treble 已开启, 但只有 legacy no-fragments 布局或其他布局; 红: Treble 未开启.
  - **GSI 行**: 绿: 命令报告 vendor namespace 兼容;  
    琥珀: 命令报告不兼容, 或 Treble 已开启但读不到结果;  
    红: Treble 未开启, 或设备太旧做不了这项检查 (Android 8 及以下).

#### HOME-12 DSU (动态系统更新)

- **问题**: 设备是否支持动态系统更新?
- **手段**: `persist.sys.fflag.override.settings_dynamic_system` 和  
  `sys.fflag.override.settings_dynamic_system` 属性.
- **三态语义**: 绿: 任一属性表明 DSU 已开启; 红: 其余情况, 关闭时这一行显示 `Unknown`.  
  这个条目没有琥珀色结果.

#### HOME-13 Mainline (Google Play 系统更新)

- **问题**: 已安装的 Mainline 模块是否达到或超过 Google 为这个版本发布的版本?
- **手段**: 框架的 `config_defaultModuleMetadataProvider` 字符串资源 (经 `Resources.getSystem()` 读取),  
  再取模块包的版本名, 与参考数据比对.
- **三态语义**: 绿: 已安装模块的版本达到发布的版本;  
  红: 更低, 提供者或其包读不到, 以及没有参考数据时. 这个条目没有琥珀色结果.

#### HOME-14 VNDK (vendor NDK)

- **问题**: 设备是否带有内置 VNDK, 它是不是当前的?
- **手段**: `ro.vndk.version` 和 `ro.vndk.lite` 属性, 与参考数据比对.
- **三态语义**: 绿: 有 VNDK 版本, 且达到或超过稳定版的 API 级别 (或者设备运行最新预览版),  
  并且不是 Lite 构建; 琥珀: 版本更低, 或者是 Lite 构建;  
  红: 没有 VNDK 版本, 以及没有参考数据时.

#### HOME-15 APEX (flattened)

- **问题**: 设备是否使用 APEX?
- **手段**: `ro.apex.updatable` 属性和挂载表.
- **三态语义**: 绿: APEX 可更新; 琥珀: 只有 flattened APEX 挂载, 且 APEX 不可更新;  
  红: 两者都不成立.

#### HOME-16 开发者选项

- **问题**: 开发者选项是否已打开?
- **手段**: 公开 SDK API (`Settings.Global.DEVELOPMENT_SETTINGS_ENABLED`).
- **三态语义**: 绿: 已关闭; 红: 已打开. 这个条目没有琥珀色结果.

#### HOME-17 ADB

- **问题**: ADB 调试是否已打开?
- **手段**: 公开 SDK API (`Settings.Global.ADB_ENABLED`).
- **三态语义**: 绿: 已关闭; 红: 已打开. 这个条目没有琥珀色结果.

#### HOME-18 ADB 认证

- **问题**: ADB 是否要求密钥认证?
- **手段**: `ro.adb.secure` 属性.
- **三态语义**: 绿: 认证已开启; 红: 未开启. 这个条目没有琥珀色结果.

#### HOME-19 加密

- **问题**: 设备存储是否已加密?
- **手段**: 公开 SDK API (`DevicePolicyManager.storageEncryptionStatus`).
- **三态语义**: 绿: 存储已加密 (用凭据密钥或每用户密钥);  
  琥珀: 已加密, 但用的是默认密钥且没有设置凭据;  
  红: 未加密, 不支持加密, 以及其他任何状态.

#### HOME-20 SELinux

- **问题**: SELinux 是否处于 enforcing?
- **手段**: shell 命令 (`getenforce`).
- **三态语义**: 绿: 模式是 enforcing, 以及命令被拒这种方式本身就证明策略在 enforcing;  
  琥珀: 模式是 permissive; 红: 已关闭, 以及读不到模式时.

#### HOME-21 Toybox

- **问题**: 设备是否带有 Toybox, 它的版本与上游相比怎样?
- **手段**: shell 命令 (`toybox --version`), 与参考数据比对.
- **三态语义**: 绿: 已安装版本达到参考的稳定版; 琥珀: 达到参考的支持版;  
  红: 更低, 没有找到 Toybox, 或没有参考数据时.

#### HOME-22 WebView 实现

- **问题**: 当前用的是哪个 WebView 实现, 它是不是当前的?
- **手段**: 对 `WebViewUpdateService.getAllWebViewPackages` 的反射读取,  
  公开 SDK API (`WebViewCompat.getCurrentWebViewPackage`), 与参考数据比对.
- **三态语义**: 绿: 内置提供者的版本达到参考的稳定版;  
  琥珀: 只有当前实现的版本达到它; 红: 其余情况, 以及没有参考值时.

#### HOME-23 targetSdk 过旧的应用

- **问题**: 哪些预装系统应用的目标 SDK 低于设备的 SDK, 有几个?
- **手段**: 公开 SDK API (`PackageManager.getInstalledApplications`), 只筛预装系统应用,  
  以及 `ro.product.first_api_level` 属性.
- **三态语义**: 绿: 没有目标 SDK 更旧的预装系统应用, 且设备运行最新稳定版或预览版;  
  琥珀: 没有这类应用, 但设备不在最新版本上; 红: 至少有一个这类应用, 以及没有参考数据时.

## Others (37 条)

Others 的行没有结论: 没有圆点, 没有颜色.

| id | 条目 | 问题 | 手段 | 备注 |
|---|---|---|---|---|
| OTH-01 | 品牌 | 构建声明的是哪个品牌? | 公开 SDK API (`Build.BRAND`) | |
| OTH-02 | 制造商 | 构建声明的是哪个制造商? | 公开 SDK API (`Build.MANUFACTURER`) | |
| OTH-03 | 型号 | 构建声明的型号名是什么? | 公开 SDK API (`Build.MODEL`) | |
| OTH-04 | 设备代号 | 构建声明的设备代号是什么? | 公开 SDK API (`Build.DEVICE`) | |
| OTH-05 | 产品名 | 构建声明的产品名是什么? | 公开 SDK API (`Build.PRODUCT`) | |
| OTH-06 | 硬件名 | 构建声明的硬件名是什么? | 公开 SDK API (`Build.HARDWARE`) | |
| OTH-07 | 主板名 | 构建声明的主板名是什么? | 公开 SDK API (`Build.BOARD`) | |
| OTH-08 | SoC 型号 | 构建声明的是哪颗 SoC? | 公开 SDK API (`Build.SOC_MODEL`) | Android 12+ |
| OTH-09 | SoC 厂商 | 构建声明的是哪家 SoC 厂商? | 公开 SDK API (`Build.SOC_MANUFACTURER`) | Android 12+ |
| OTH-10 | SKU | 构建声明的是哪个 SKU? | 公开 SDK API (`Build.SKU`) | Android 12+ |
| OTH-11 | Vendor SKU | 系统属性报告的是哪个 vendor SKU? | 系统属性 | Android 12+ |
| OTH-12 | ODM SKU | 构建声明的是哪个 ODM SKU? | 公开 SDK API (`Build.ODM_SKU`) | Android 12+ |
| OTH-13 | Binder 状态 (binder 驱动) | binder 驱动报告的是哪个协议版本? | 原生代码 (JNI) 读驱动; errno 报告为不支持或未知 | |
| OTH-14 | 进程位数 | 这个应用的进程是 32 位还是 64 位? | 公开 SDK API (`Process.is64Bit()`) | |
| OTH-15 | 进程与 VM 架构 | JVM 报告的架构是什么? | `os.arch` 系统属性 | |
| OTH-16 | CPU ABI (构建字段) | 构建声明的是哪个 ABI? | 公开 SDK API (`Build.CPU_ABI`) | |
| OTH-17 | CPU ABI (系统属性) | 系统属性报告的是哪个 ABI? | 系统属性 | |
| OTH-18 | 支持的 32 位 ABI | 设备支持哪些 32 位 ABI? | 公开 SDK API (`Build.SUPPORTED_32_BIT_ABIS`) | |
| OTH-19 | 支持的 64 位 ABI | 设备支持哪些 64 位 ABI? | 公开 SDK API (`Build.SUPPORTED_64_BIT_ABIS`) | 列表为空时显示为不支持 |
| OTH-20 | 构建用户 | 这个构建是哪个用户构建的? | 公开 SDK API (`Build.USER`) | |
| OTH-21 | 构建主机 | 这个构建是在哪台主机上构建的? | 公开 SDK API (`Build.HOST`) | |
| OTH-22 | 构建时间 | 这个构建是什么时候做的? | 公开 SDK API (`Build.TIME`), 按本地时区显示 | |
| OTH-23 | Base OS | 构建声明的是哪个 Base OS? | 公开 SDK API (`Build.VERSION.BASE_OS`) | |
| OTH-24 | 指纹 | 构建声明的是哪个构建指纹? | 公开 SDK API (`Build.FINGERPRINT`) | |
| OTH-25 | 预览版 SDK 指纹 | 系统属性报告的是哪个预览版 SDK 指纹? | 系统属性 | Android 10+ |
| OTH-26 | 分区指纹 | 每个分区报告的是哪个指纹? | 每个分区一个系统属性 | 只给能读到指纹的分区各一行; 读不到的分区不出行 |
| OTH-27 | Build Id | 构建声明的是哪个 build id? | 公开 SDK API (`Build.ID`) | |
| OTH-28 | Display | 构建声明的是哪个 display id? | 公开 SDK API (`Build.DISPLAY`) | |
| OTH-29 | 构建类型 | 构建声明的是哪种构建类型? | 公开 SDK API (`Build.TYPE`) | |
| OTH-30 | 构建标签 | 构建声明的是哪些标签? | 公开 SDK API (`Build.TAGS`) | |
| OTH-31 | 增量版本 | 构建声明的是哪个增量版本? | 公开 SDK API (`Build.VERSION.INCREMENTAL`) | |
| OTH-32 | 版本代号 | 构建声明的是哪个代号? | 公开 SDK API (`Build.VERSION.CODENAME`) | Android 13 及以上的预览版构建, 当 `RELEASE_OR_PREVIEW_DISPLAY` 与代号不同时追加开发者预览后缀 |
| OTH-33 | 预览版 SDK 级别 | 构建声明的是哪个预览版 SDK 级别? | 公开 SDK API (`Build.VERSION.PREVIEW_SDK_INT`) | |
| OTH-34 | 默认 User-Agent | 系统 WebView 报告的 User-Agent 是什么? | 公开 SDK API (`WebSettings.getDefaultUserAgent`) | |
| OTH-35 | 内核版本 | 设备运行的是哪个内核版本? | shell 命令 | |
| OTH-36 | Bootloader | 构建声明的是哪个 Bootloader 版本? | 公开 SDK API (`Build.BOOTLOADER`) | |
| OTH-37 | 基带版本 | 设备报告的是哪个基带版本? | 公开 SDK API (`Build.getRadioVersion()`) | |

## Prop (5 条)

Prop 的行没有结论: 没有圆点, 没有颜色. 每行的标题就是键本身.

| id | 条目 | 问题 | 手段 | 备注 |
|---|---|---|---|---|
| PROP-01 | 系统属性 (`getprop`) | 应用的进程能看到哪些系统属性? | 公开 SDK API (`System.getProperties()`) 加对 `Properties.defaults` 字段的反射读取 | 每个属性一行, 按键排序 |
| PROP-02 | `Settings.System` 表 | 系统设置表里有哪些条目? | 该类的已声明 `String` 字段, 再逐键反射调用 `getString(ContentResolver, String)` | 每个键一行 |
| PROP-03 | `Settings.Secure` 表 | 安全设置表里有哪些条目? | 该类的已声明 `String` 字段, 再逐键反射调用 `getString(ContentResolver, String)` | 每个键一行 |
| PROP-04 | `Settings.Global` 表 | 全局设置表里有哪些条目? | 该类的已声明 `String` 字段, 再逐键反射调用 `getString(ContentResolver, String)` | 每个键一行 |
| PROP-05 | build.prop | `getprop` 的输出里有哪些条目? | shell 命令 (`getprop`) | 每个解析出的 `[key]: [value]` 条目一行; 多行条目算一行 |

## 条目模板

新增或调整条目时, 它带这些字段:

| 字段 | 含义 |
|---|---|
| **id** | 该条目的稳定标识符 |
| **screen** | Home, Others 或 Prop |
| **title** | 该条目在本目录里的名字; 用户看到的文字在功能自己的 `strings.xml` 里 |
| **question** | 这个条目回答什么问题, 用大白话说 |
| **techniques** | 这个条目实际使用的检测手段, 适用时用 FR-9 的说法, 与参考数据比对时再加上 FR-16 的参考数据 |
| **tri-state semantics** | 什么算支持, 不支持, 未知, 以及各自取什么颜色, 由该条目自己的语义决定 (FR-3, FR-15, NFR-7); 只区分两种结果的条目要写明这一点 |
