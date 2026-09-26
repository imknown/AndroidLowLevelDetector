<a id="qw-2"></a>

# QW-2 lld.json 日期格式是隐式契约, 上游一改格式就崩溃 (AR-15 lld.json 的日期格式是隐式契约)

> 返回 [README 索引](../README.md) · [组5-快赢批](../README.md#目录).


**改动量: ~35 行 | 文件: `base/.../extension/DateTimeExt.kt`, `app/.../home/HomeViewModel.kt`, 新增 2 个测试**

## 问题代码

格式契约只存在于解析函数的魔法字符串里 (`String.formatToLocalZonedDatetimeString()`):

```kotlin
fun String.formatToLocalZonedDatetimeString(): String {
    val pattern = "yyyy-MM-dd HH:mm Z"        // ← 契约埋在这里, 与数据方互相不知晓
    val formatter = DateTimeFormatter.ofPattern(pattern)
    val instant = ZonedDateTime.parse(this, formatter).toInstant()
    ...
}
```

联网路径上取数, 解析, 保存都有 try/catch, 唯独最后的 `detect()` 裸奔 (`HomeViewModel.tryDetectOnline()`):

```kotlin
return detect(lld, listOf(errorMessage), R.string.lld_json_online)   // ← 全链唯一无防护的一步
```

## 引入提交

| 日期 | 提交信息 | 当时目的 |
|---|---|---|
| 2022-06-24 | Improve Datetime APIs | 收拾日期工具函数, 把 pattern 写死进了函数体 (推断) |
| 2025-12-03 | Feat: Improve exception handling via UDF | 给联网检测补异常处理时, fetch / parse / save 三步都包了, 漏掉了最后的 detect() |

## 修改后

```kotlin
// DateTimeExt.kt — 契约唯一定义处
private val lldDatetimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm Z")

fun String.isLldDatetime(): Boolean =
    runCatching { ZonedDateTime.parse(this, lldDatetimeFormatter) }.isSuccess()

fun String.formatToLocalZonedDatetimeString(): String {
    val instant = ZonedDateTime.parse(this, lldDatetimeFormatter).toInstant()
    val datetime = instant.atZone(ZoneId.systemDefault())
    return lldDatetimeFormatter.format(datetime)
}
```

```kotlin
// HomeViewModel.tryDetectOnline — parse 之后, save 之前, 进门先验货
if (!lld.version.isLldDatetime()) {
    val errorMessage = errorMessage(
        R.string.lld_json_parse_failed,
        IOException("Unsupported lld.version format: ${lld.version}")
    )
    return tryDetectOffline(errorMessage)
}
```

新增契约测试 (兑现 R7(版本合并的残留弱点) 裁定 "测试需覆盖时区偏移后缀", 跑 `:base:testDebugUnitTest`;parse 侧锁定拒绝异形格式, format 侧用 round-trip 锁定"解析什么就格式化回什么"):

```kotlin
// base/src/test/java/net/imknown/android/forefrontinfo/base/extension/DateTimeExtTest.kt
class DateTimeExtTest {
    @Test
    fun lldDatetimeContract() {
        assertTrue("2026-09-02 20:55 +0800".isLldDatetime())
        assertFalse("2026-09-02 20:55 +08:00".isLldDatetime())
        assertFalse("2026-09-02 20:55".isLldDatetime())
        assertFalse("".isLldDatetime())
    }

    @Test
    fun formatRoundTripsWithinSameOffset() {
        val original = ZoneId.systemDefault()
        try {
            ZoneId.setDefault(ZoneId.of("+08:00"))
            val input = "2026-09-02 20:55 +0800"
            assertEquals(input, input.formatToLocalZonedDatetimeString())
        } finally {
            ZoneId.setDefault(original)
        }
    }
}
```

范围说明: `SettingsRepository` 读随包资产里的 lld 版本用的是同一格式, 但随包分发, 格式自控, 本轮不动, 后续随 AR-19(本地化字符串被当作 "哨兵值") 迁移统一.

## 直接原因

网络拉取的 lld.json 不受本应用控制, 上游把 `"version"` 写成 `"20:55 +08:00"`(带冒号) 等任何不合 pattern 的样子, `ZonedDateTime.parse` 抛 `DateTimeParseException`, 沿无防护的 `detect()` 一路上抛进 `viewModelScope.launch` — 未捕获异常即应用崩溃, 所有开启联网检测的用户同时中招.

## 根本原因

外部数据进门只做了类型反序列化, 没有语义校验这道闸; 格式契约以魔法字符串形式埋在格式化函数里, 数据的生产方 (上游 lld.json 维护者) 与消费方 (pattern) 之间没有任何机器可检查的约定.

---


