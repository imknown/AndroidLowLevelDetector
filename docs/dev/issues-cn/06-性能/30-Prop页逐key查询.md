<a id="i30"></a>

# 30 Prop 页设置项逐 key 查询, 一次加载几百个串行 binder 调用

> 返回 [README 索引](../README.md) · [6 · 性能](../README.md#6--性能).

**严重程度: P1(性能, 用户可感知) | 修复难度: 中**  
**影响文件: `ui/prop/datasource/SettingsDataSource.kt`, `ui/prop/repository/PropRepository.kt`,  
`ui/prop/PropViewModel.kt`**

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

然后**每个 key 单独查一次值**: 循环在 `PropRepository.getSettings()` (三张表各调一次,  
由 `PropViewModel.collectModels()` 排进列表):

```kotlin
val list = settingsDataSource.getSettingsOrThrow(subSettingsKClass)

list.forEach {
    val key = "${subSettingsKClass.qualifiedName}.$it"
    val value = try {
        settingsDataSource.getStringOrNullOrThrow(          // 每次调用 = 一次 contentResolver binder 往返
            subSettingsKClass, MyApplication.instance.contentResolver, it)
    } catch (e: Exception) { ... }
    tempModels.add(toTranslatedDetailMyModel(key, value))
}
```

而 `getStringOrNullOrThrow` 本身又是一层反射 (`SettingsDataSource.getStringOrNullOrThrow()`):  
`getDeclaredMethod("getString", ContentResolver::class.java, String::class.java).invoke(null, ...)`.

## 直接原因

`Settings` 三张表的 String 常量加起来是**几百个量级** (key 列表取自设备上框架类的 `declaredFields`,  
数量随 Android 版本与 ROM 增减).  
Prop 页一次加载 = 与常量数等量的**串行跨进程 binder 查询**, 外加两轮反射. 具体次数要在真机上量才知道 ("几百"  
是按 SDK 常量数给出的量级, 不是实测值); 串行次数直接乘在加载时间上,  
低端机上最吃紧. 整段跑在 `PropViewModel.collectModels()` 的 `withContext(Dispatchers.Default)` 里,  
于是还与 [#16](../04-并发与调度/16-阻塞调用跑在CPU线程池.md) / [#14](../04-并发与调度/14-检测链用CPU线程池.md)  
的 "阻塞调用占用 CPU 池" 叠加.

## 根本原因

把 "一张可以整表枚举的 ContentProvider 表" 当成了 "键值对 API" 来用. 这同时还造成**漏数据**:  
常量列表 ≠ 实际存储的行: 设备上动态写入的,  
不在 SDK 常量里的设置项全都显示不出来, SDK 里已废弃的常量又会查出空值. 这个漏法是取 key 方式的必然结果,  
换掉取 key 的方式才能一并解决. 顺带如实记录:  
反射枚举出的键集里含 `ANDROID_ID` 之类的标识键, 该页会把对应值原样显示在屏幕上, 仅本地展示,  
应用的网络只取 `lld.json`, 不外发; 换全表 query 后这一面自然收窄到实际存储的行.

## 修复方案

一次 query 拉全表 (name + value 成对返回, 单次 binder 往返), 用显式表定义取代 KClass 反射:

```kotlin
// ui/prop/datasource/SettingsDataSource.kt
enum class SettingsTable(val uri: Uri) {
    SYSTEM(Settings.System.CONTENT_URI),
    SECURE(Settings.Secure.CONTENT_URI),
    GLOBAL(Settings.Global.CONTENT_URI)
}

class SettingsDataSource {

    /** 单次往返拿到全部 "实际存储" 的键值对 */
    fun getSettingsOrThrow(table: SettingsTable, contentResolver: ContentResolver): List<Pair<String, String?>> =
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
// PropRepository: 循环查询消失, 直接映射
fun getSettings(table: SettingsTable): List<MyModel> =
    settingsDataSource.getSettingsOrThrow(table, MyApplication.instance.contentResolver).map { (key, value) ->
        toTranslatedDetailMyModel("${table.name.lowercase()}.$key", value)
    }

// PropViewModel.collectModels: KClass 参数换成枚举
tempModels += propRepository.getSettings(SettingsTable.SYSTEM)
```

落地时必须一起处理的三点:

1. `contentResolver.query()` 是跨进程阻塞调用, 按 AGENTS.md 走 `Dispatchers.IO`,  
   不能留在现在的 `withContext(Dispatchers.Default)` 里,  
   与 [#14](../04-并发与调度/14-检测链用CPU线程池.md) 是同一件事.
2. 三个 `CONTENT_URI` 常量与 `query()` 的 API 等级要按 **minSdk** (`gradle/toml/build.toml` 的 `minSdk`)  
   核对一遍再写 (`@RequiresApi` 或 `isAtLeast...()` 闸门按核对结果决定).
3. 条目集合的口径会变: 现在由 **SDK 常量名**决定 (`toTranslatedDetailMyModel(key, value)` 把 key 原样当标题,  
   查不到值的行渲染成 "未填写"),  
   改后由**实际存储的行**决定: 空的废弃行消失,  
   设备自写的项出现. Prop 页每行的 `MyModel.key` 就是这个原始标题 (`MyModelTitle.Raw`), 行集一变,  
   `LazyColumn` 的 key 集合跟着变, 与 [#20](../archived/05-稳定性与错误处理/20-列表key重复崩溃.md)  
   的重复 key 问题同一条链.

注意事项: 全表 query 能拿到哪些行, 由设备上的框架与权限决定,  
与逐 key `getString()` 的可见范围不必完全相同 (个别 ROM 会对 Secure 表的结果做过滤).  
真机验证时把两种取法的行集对比一次; 若某张表确实拿不全, 仅对该表保留逐 key 查询 (key 列表仍一次取得),  
System/Global 照走全表.

反射的逐 key 开销与逐 key 查询是同一循环里的两笔, [#32](32-启动性能杂项.md) 的 "每调用一次 `getDeclaredMethod`"  
一项在本页被放大成每 key 一次.
