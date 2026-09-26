<a id="qw-8a"></a>

# QW-8a 用文案的资源 ID 判断业务分支 (AR-11 资源 ID 当"逻辑值"和"数据"用)

> 返回 [README 索引](../README.md) · [组5-快赢批](../README.md#目录).


**改动量: ~15 行, 独立提交 | 文件: `HomeRepository.kt`, `HomeViewModel.kt`**

## 问题代码

```kotlin
// HomeRepository.detectMode()
fun detectMode(lld: Lld?, errors: List<String?>, modeResId: Int): MyModel {
    ...
    color = if (modeResId == R.string.lld_json_online) {   // ← 拿"文案的编号"判断业务分支
        StatusColor.NO_PROBLEM
    } else {
        StatusColor.CRITICAL
    }
```

## 引入提交

| 日期 | 提交信息 | 当时目的 |
|---|---|---|
| 2021-03-21 | Feat: Architecture refactor: Repositories and DataSources | 仓库化重构时引入该比较 |
| 2025-12-03 | Feat: Improve exception handling via UDF | 异常处理改造把比较对象参数化成 modeResId, 沿调用链传进来 |

## 修改后

```kotlin
// HomeRepository.kt — 业务枚举承载语义, 资源只在拼文案时使用
enum class LldSource { ONLINE, OFFLINE }

fun detectMode(lld: Lld?, errors: List<String?>, source: LldSource): MyModel {
    ...
    color = if (source == LldSource.ONLINE) StatusColor.NO_PROBLEM else StatusColor.CRITICAL
    ...
    return toColoredMyModel(
        R.string.lld_json_mode_title,
        MyApplication.getMyString(
            when (source) {
                LldSource.ONLINE -> R.string.lld_json_online
                LldSource.OFFLINE -> R.string.lld_json_offline
            },
            datetimeFormatted
        ) + result,
        color
    )
}
```

`HomeViewModel.detect()` 的签名 `@StringRes modeResId: Int` 改为 `source: LldSource`; 两个调用点 `HomeViewModel.tryDetectOnline()` 与 `tryDetectOffline()` 分别传 `LldSource.ONLINE` / `LldSource.OFFLINE`.

## 直接原因

`R.string.lld_json_online` 是 "文案的地址" 不是业务语义: 资源重命名, 多模块资源 ID 重排, 或文案对应关系变化, 都会让这段 `if` 静默改变颜色逻辑, 编译器不报任何错.

## 根本原因

缺少显式的业务枚举, 资源 ID 因 "顺手可用" 被借作判别值 — "能跑" 和 "语义正确" 之间只隔一次无声的重构事故.

---


