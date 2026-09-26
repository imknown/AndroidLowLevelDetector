<a id="AR-15"></a>

# AR-15 lld.json 的日期格式是隐式契约, 上游一改格式应用即崩溃

> 返回 [README 索引](../README.md) · [组3 · 暂缓](../README.md#组3--暂缓-原因见危害列标注).


**严重程度: P1(潜伏的崩溃链, 触发条件在应用外) | 修复难度: 低**
**影响文件: `DateTimeExt.kt`, `HomeViewModel.kt`, `HomeRepository.kt`, `SettingsRepository.kt`, `assets/lld.json`**

## 问题核心代码

格式契约存在于两处, 互不知晓:

```kotlin
// DateTimeExt.kt → String.formatToLocalZonedDatetimeString() — 契约 A: 解析 pattern
fun String.formatToLocalZonedDatetimeString(): String {
    val pattern = "yyyy-MM-dd HH:mm Z"
    val instant = ZonedDateTime.parse(this, formatter).toInstant()   // 格式不符 → DateTimeParseException
    ...
}
```

```json
// assets/lld.json → version — 契约 B: 数据长这样
"version": "2026-09-02 20:55 +0800"
```

而联网路径上, `tryDetectOnline` 对取数, 反序列化, 保存都有 try/catch,**唯独最后的 `detect()` 调用没有**(`HomeViewModel.tryDetectOnline()`); `detectMode` 第一行就调用这个格式化 (`HomeRepository.detectMode()`):

```kotlin
// HomeViewModel.tryDetectOnline — fetch / parse / save 都有防护, detect 裸奔
val errorMessage = try {
    withContext(Dispatchers.IO) { LldManager.saveLldJsonFileOrThrow(lldString) }
    null
} catch (e: Exception) { errorMessage(R.string.lld_json_save_failed, e) }

return detect(lld, listOf(errorMessage), R.string.lld_json_online)   // ← 无防护
```

## 直接原因

网络拉取的 lld.json 不受本应用控制. 只要上游把 `"version"` 改成 `"2026-09-02 20:55 +08:00"` (冒号) 或 `"+8"` 之类任何与本 pattern 不符的写法, `ZonedDateTime.parse` 抛出 `DateTimeParseException`, 沿 `detect()` → `collectModels()` 一路上抛; `BaseListViewModel.startLoad` 的 `viewModelScope.launch` 没有 catch([AR-09](05-AR-09-State无错误态.md)), 未捕获异常传给线程处理器 — **应用崩溃**. 所有开启联网检测的用户同时中招.

## 根本原因

外部数据进入系统时只做了 "反序列化类型检查", 没有 "语义校验" 这道闸. 日期格式这个契约以魔法字符串的形式埋在格式化函数里, 数据的提供方 (lld.json 维护者) 与消费方 (pattern) 之间没有任何机器可检查的约定 — 这正是数据边界 (boundary) 缺一道验证层的典型代价.

## 修复方案

契约集中定义 + 边界一次性校验, 两道防线:

```kotlin
// base/extension/DateTimeExt.kt — 契约唯一定义处
val LLD_DATETIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm Z")

fun String.isLldDatetime(): Boolean =
    runCatching { ZonedDateTime.parse(this, LLD_DATETIME_FORMATTER) }.isSuccess()

fun String.formatToLocalZonedDatetimeString(): String {
    val instant = ZonedDateTime.parse(this, LLD_DATETIME_FORMATTER).toInstant()
    ...  // 原逻辑
}
```

```kotlin
// HomeViewModel.tryDetectOnline — 网络数据进门先验货, 不合格按解析失败降级走离线
val lld = withContext(Dispatchers.IO) { lldString.toObjectOrThrow<Lld>() }
if (!lld.version.isLldDatetime()) {
    return tryDetectOffline(errorMessage(R.string.lld_json_parse_failed, Exception("bad version format")))
}
```

第二道防线是 [AR-09](05-AR-09-State无错误态.md) 的 `runCatching { collectModels() }` 整体兜底 — 即使未来再出现类似漏洞, 也只是错误态而非崩溃. `SettingsRepository` 读取资产内 lld 版本处 (同一契约, 随包分发风险低) 顺手加同样的校验.


