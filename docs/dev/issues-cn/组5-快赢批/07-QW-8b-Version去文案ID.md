<a id="qw-8b"></a>

# QW-8b 数据类 Version 携带文案 ID(AR-11 资源 ID 当"逻辑值"和"数据"用)

> 返回 [README 索引](../README.md) · [组5-快赢批](../README.md#目录).


**改动量: ~10 行, 独立提交 | 文件: `SettingsRepository.kt`, `SettingsScreen.kt`**

## 问题代码

```kotlin
// SettingsRepository.Version(构造处 :88 传入了 R.string.about_version_summary)
data class Version(
    @param:StringRes val id: Int,   // ← 数据类里携带"用哪条文案"的界面指令
    val versionName: String,
    ...
)
```

## 引入提交

| 日期 | 提交信息 | 当时目的 |
|---|---|---|
| 2021-03-21 | Feat: Architecture refactor: Repositories and DataSources | 仓库化重构时把文案 ID 装进了数据类 (推断: 想让仓库"包办"文案) |

## 修改后

`Version` 删掉 `id` 字段, 构造处 (`SettingsRepository.getBuiltInDataVersion() 构造 Version 处`) 删掉 `R.string.about_version_summary` 实参; 界面自己知道自己该用哪条文案:

```kotlin
// SettingsScreen.kt → SettingsContent() 版本行里的 v.id — 版本行的 supporting 文案
Text(
    stringResource(
        R.string.about_version_summary,
        v.versionName, v.versionCode, v.assetLldVersion,
        ...   // 其余字段实参不变
    )
)
```

还有一处构造点要跟着改: 预览里的 `SettingsContentPreview()`(`id = R.string.about_version_summary`).

## 直接原因

`Version.id` 让界面层反向持有"用哪条文案"的格式化知识, 数据类混入了界面指令, `SettingsScreen` 不看仓库源码就不知道这行会套哪个模板.

## 根本原因

与 QW-8a 同根: 资源 ID 因 "顺手可用" 被当成数据跨层传递, 缺一个 "数据归数据, 文案归界面" 的边界.

---


