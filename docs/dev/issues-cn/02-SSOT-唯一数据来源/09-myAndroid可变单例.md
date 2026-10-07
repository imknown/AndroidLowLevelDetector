<a id="i09"></a>

# 09 全局可变单例 myAndroid 有两个写入方

> 返回 [README 索引](../README.md) · [2 · SSOT · 唯一数据来源](../README.md#2--ssot--唯一数据来源).

**严重程度: P0(当前为偶发隐患, 结构性风险高) | 修复难度: 低~中**  
**影响文件: `AndroidVersionExt.kt`, `HomeRepository.kt`, `MyApplication.kt`**

## 问题核心代码

全局可变对象 (`MyAndroid`), 四个字段全是 `var`, `var` 与两个写入方都出自 `557b54ea` (Android 17 适配):

```kotlin
class MyAndroid(
    var api: Int,          // ← 全部是 var
    var apiFull: String,
    var version: String,
    var dessert: String? = null
)

val myAndroid = MyAndroid(   // 顶层 val, 全项目共享的单身实例
    Build.VERSION.SDK_INT, "${Build.VERSION.SDK_INT}.$minor", Build.VERSION.RELEASE
)
```

写入方一: App 启动 (`MyApplication.onCreate` → `initMyAndroid()`, 从运行时 `Build.VERSION` 推导).

写入方二: **首页数据仓库**(`HomeRepository.detectAndroid()`), 一个 "取数方法" 顺手改了全局状态:

```kotlin
if (android != null) {
    with(android) {
        myAndroid.api = api.toInt()        // ← 仓库在改全局单例
        myAndroid.apiFull = apiFull
        myAndroid.version = version
        myAndroid.dessert = name
    }
}
```

而全项目的系统版本判断都读它 (`sdkInt`):

```kotlin
private val sdkInt get() = myAndroid.api
fun isAtLeastAndroid12() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || sdkInt >= Build.VERSION_CODES.S
```

## 直接原因

"这台设备的 Android 版本信息" 有两个 "事实版本": `Build.VERSION.*` 推导出的初始值, 和 LLD 数据库 JSON 修正后的值. 两处写入, 多处读取, 没有任何机制保证先后一致:  
`isAtLeast*`, `isLatestPreviewAndroid` 等函数在首页加载前后的返回值**理论上可能不同**(大多数设备上两次写入恰好相同, 所以问题平时不发作). 具体触点: 过时应用过滤阈值  
`it.targetSdkVersion < myAndroid.api` (`HomeRepository.getOutdatedTargetSdkVersionApkModel()`) 读的正是这个全局.  
把 `detect()` 里 `detectAndroid` 与 `getOutdatedTargetSdkVersionApkModel` 的调用顺序对调, 过滤结果就会变, 一致性全靠手写顺序维持.

## 根本原因

可变全局单例 = 把 "数据" 和 "数据的位置" 混在一起: 值随时可被任何代码改写, 读到的结果取决于 "谁最后写过", 这就是**时间耦合**(temporal coupling, 正确性依赖调用顺序).  
LLD 的 `known` 数据本应作为参数参与计算, 却通过篡改全局变量来 "顺便" 传递.

## 修复方案

**前置约定变更 (已裁)**: 负责人已确认收敛为单一写入方: 冻结为不可变 + enrich 纯函数即本条目方案; "本地值先行, 联网取到 LLD 后用修正值展示" 的行为不变, 改的只是修正值不再回写全局.  
[AGENTS.md](../../../../AGENTS.md) 的 "exactly two writers" 条款随本条目同批改写, 不留两份互相矛盾的约定.

冻结为不可变 (immutable, 创建后不能改), 二次加工用 `copy` 返回新对象, 显式传递:

```kotlin
// ui/common/AndroidVersionExt.kt
data class MyAndroid(                       // data class + 全 val
    val api: Int,
    val apiFull: String,
    val version: String,
    val dessert: String?
)

object AndroidInfo {
    /** 启动时构建一次, 此后只读: isAtLeast* 只依赖它 */
    val current: MyAndroid by lazy { build() }

    private fun build(): MyAndroid {
        // ... 原 initMyAndroid() 的推导逻辑原样搬入, 最后 return MyAndroid(...)
    }

    /** 纯函数: 用 LLD 已知列表修正, 返回新对象, 不动原对象 */
    fun enrichWithLld(base: MyAndroid, lld: Lld): MyAndroid {
        val known = lld.android.known.find { it.apiFull == base.apiFull } ?: return base
        return base.copy(
            api = known.api.toInt(),
            apiFull = known.apiFull,
            version = known.version,
            dessert = known.name
        )
    }
}
```

```kotlin
// HomeRepository.detectAndroid: 不再写全局, 改用参数
fun detectAndroid(lld: Lld?, androidInfo: MyAndroid): MyModel {   // androidInfo 由 ViewModel 传入
    val enriched = lld?.let { AndroidInfo.enrichWithLld(androidInfo, it) } ?: androidInfo
    ...
}
```

`isAtLeast*` 一律改读 `AndroidInfo.current` (等价于原来的初始值, 不再被中途篡改).  
`initMyAndroid()` 从 `MyApplication.onCreate` 中删除 (`by lazy` 自带一次性初始化, 且懒加载避免启动期反射开销).

**这不是纯机械替换, 需要多想一步**: `initMyAndroid()` 的第一句就读全局:  
`val kClass = if (isAtLeastAndroid16()) { Build.VERSION_CODES_FULL::class } else { ... }`,  
而 `isAtLeastAndroid16()` 经 `sdkInt` 读 `myAndroid.api` (此刻还是顶层初始化留下的 `Build.VERSION.SDK_INT`).  
直接把 `var` 改成 `val` 会把 "先初始化, 后回写" 的两段式压成一次构造, 处理不好就自我引用. 必须像 `build()` 那样把推导结果先放进局部变量,  
最后一次性构造返回 (推导逻辑本身原样搬, 不改判断).

迁移注意: `myAndroid` 的直接引用点不多: `AndroidVersionExt.kt` 内 2 处读 (`sdkInt` 的 getter, `isLatestPreviewAndroid`) 加  
`initMyAndroid()` 的 4 行回写;  
`HomeRepository` 内 5 处读 (`detectAndroid()` 的 4 处取值加 `getOutdatedTargetSdkVersionApkModel()` 的阈值) 加 4 行回写.  
全局替换为 `AndroidInfo.current` 后编译器会兜底找出遗漏; 真正间接依赖它的是整套 `isAtLeast*` 系列, 它们只经 `sdkInt` 一个入口,  
所以改动面收敛在这一个 getter 上.

按这份落点清单, 改动是十几行的量级; 并发侧的无同步读写另记 [#17](../04-并发与调度/17-myAndroid并发读写.md), 随本条目一并关闭.
