<a id="AR-14"></a>

# AR-14 Prop 页设置项逐 key 查询, 一次加载几百个串行 binder 调用

> 返回 [README 索引](../README.md) · [组3 · 暂缓](../README.md#组3--暂缓-原因见危害列标注).


**严重程度: P1(性能, 用户可感知) | 修复难度: 中**
**影响文件: `SettingsDataSource.kt`, `PropRepository.kt`, `PropViewModel.kt`**

## 问题核心代码

先用反射取出 `Settings.System/Secure/Global` 类里的**常量字段名**当作 key 列表 (`SettingsDataSource.getSettingsOrThrow()`):

```kotlin
fun <T : Settings.NameValueTable> getSettingsOrThrow(subSettingsKClass: KClass<T>): List<String> {
    return subSettingsKClass.java.declaredFields
        .filter { it.isAccessible = true; it.type == String::class.java }
        .map { it.get(null) as String }
        .sortedBy { it.uppercase(Locale.US) }
}
```

然后**每个 key 单独查一次值**(`PropRepository.getSystemProp()`):

```kotlin
list.forEach {
    val value = try {
        settingsDataSource.getStringOrNullOrThrow(          // 每次调用 = 一次 contentResolver binder 往返
            subSettingsKClass, MyApplication.instance.contentResolver, it)
    } catch (e: Exception) { ... }
    tempModels.add(toTranslatedDetailMyModel(key, value))
}
```

而 `getStringOrNullOrThrow` 本身又是一层反射 (`SettingsDataSource.getStringOrNullOrThrow()`): `getDeclaredMethod("getString", ...).invoke(null, ...)`.

## 直接原因

`Settings` 三张表的 String 常量加起来有几百个. Prop 页一次加载 = **几百次串行的跨进程 binder 查询**, 外加两轮反射, 全部跑在 `Dispatchers.Default` 上 (与 [AR-12](16-AR-12-阻塞调用与HttpClient.md) 的 "阻塞调用占用 CPU 池" 叠加). 低端机上 Prop 页加载明显偏慢.

## 根本原因

把 "一张可以整表枚举的 ContentProvider 表" 当成了 "键值对 API" 来用. 这同时还造成**漏数据**: 常量列表 ≠ 实际存储的行 — 设备上动态写入的, 不在 SDK 常量里的设置项全都显示不出来, SDK 里已废弃的常量又会查出空值.

## 修复方案

一次 query 拉全表 (name + value 成对返回, 单次 binder 往返), 用显式表定义取代 KClass 反射:

```kotlin
// ui/prop/datasource/SettingsDataSource.kt
enum class SettingsTable(val uri: Uri) {
    SYSTEM(Settings.System.CONTENT_URI),
    SECURE(Settings.Secure.CONTENT_URI),
    GLOBAL(Settings.Global.CONTENT_URI)
}

class SettingsDataSource(private val contentResolver: ContentResolver) {

    /** 单次往返拿到全部 "实际存储" 的键值对 */
    fun getSettingsOrThrow(table: SettingsTable): List<Pair<String, String?>> =
        contentResolver.query(
            table.uri,
            arrayOf(Settings.NameValueTable.NAME, Settings.NameValueTable.VALUE),
            null, null,
            "${Settings.NameValueTable.NAME} ASC"
        )?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(0) to cursor.getString(1))
                }
            }
        } ?: emptyList()
}
```

```kotlin
// PropRepository — 循环查询消失, 直接映射
fun getSettings(table: SettingsTable): List<MyModel> =
    settingsDataSource.getSettingsOrThrow(table).map { (key, value) ->
        toTranslatedDetailMyModel("${table.name.lowercase()}.$key", value)
    }

// PropViewModel.collectModels — KClass 参数换成枚举
tempModels += propRepository.getSettings(SettingsTable.SYSTEM)
```

注意事项: 个别 ROM 可能对 Secure 表的 query 结果做过滤, 若真机验证遇到, 仅对该表保留逐 key 查询 (key 列表仍一次取得), System/Global 照走全表.


