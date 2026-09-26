<a id="AR-17"></a>

# AR-17 QUERY_ALL_PACKAGES 用途申报缺档

> 返回 [README 索引](../README.md) · [组1 · 随时可做 — 独立小修](../README.md#组1--随时可做--独立小修).


**严重程度: P2 | 修复难度: 低**
**影响文件: `app/src/main/AndroidManifest.xml`**

## 问题核心代码

```xml
<uses-permission
    android:name="android.permission.QUERY_ALL_PACKAGES"
    tools:ignore="PackageVisibilityPolicy, QueryAllPackagesPermission" />
```

## 直接原因

`QUERY_ALL_PACKAGES` 是 Google Play 的高敏感权限. 本应用 "过时 targetSdk 应用" 功能确实需要枚举全部应用, 属于正当用途, 但用途申报没有随代码留档, 上架/更新时容易在审核环节返工. (本条目原来的另一半 — intent-filter 里的 `<action VIEW>` 残留 — 已修复: 2026-09-12 删除, commit `5c24c742`, 该部分随修复从本报告删除.)

## 根本原因

Manifest 是 "只加不减" 的文件, 没有变更说明的归属; 权限的 "为什么需要" 只存在于开发者的脑子里.

## 修复方案

`QUERY_ALL_PACKAGES` 保留, 但在发布清单 (如 `SECURITY.md` 旁或开发者私有发布笔记) 留一段 Play Console 申报文案:"用于检测设备上 targetSdk 低于系统版本的预装/系统应用, 核心功能依赖". (`gwpAsanMode="always"` 与 `enableOnBackInvokedCallback` 均为有意配置, 无需处理.)


