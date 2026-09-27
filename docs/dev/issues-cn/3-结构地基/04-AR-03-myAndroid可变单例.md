<a id="AR-03"></a>

# AR-03 全局可变单例 myAndroid 有两个写入方

> 返回 [README 索引](../README.md) · [3 · 结构地基 — 阻塞后续开发](../README.md#3--结构地基--阻塞后续开发).


**严重程度: P0(当前为偶发隐患, 结构性风险高) | 修复难度: 低~中**
**影响文件: `AndroidVersionExt.kt`, `HomeRepository.kt`, `MyApplication.kt`**

## 问题核心代码

全局可变对象 (`MyAndroid`):

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

写入方一: App 启动 (`MyApplication.onCreate` → `initMyAndroid()`, `initMyAndroid()`).

写入方二: **首页数据仓库**(`HomeRepository.detectAndroid()`, `HomeRepository.detectAndroid()`) — 一个 "取数方法" 顺手改了全局状态:

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

"这台设备的 Android 版本信息" 有两个 "事实版本": `Build.VERSION.*` 推导出的初始值, 和 LLD 数据库 JSON 修正后的值. 两处写入, 多处读取, 没有任何机制保证先后一致 — `isAtLeast*`, `isLatestPreviewAndroid` 等函数在首页加载前后的返回值**理论上可能不同**(大多数设备上两次写入恰好相同, 所以问题平时不发作). 具体触点: 过时应用过滤阈值 `it.targetSdkVersion < myAndroid.api` (`HomeRepository.getOutdatedTargetSdkVersionApkModel() 里的 it.targetSdkVersion < myAndroid.api`) 读的正是这个全局 — 把 `detect()` 里 `detectAndroid` 与 `getOutdatedTargetSdkVersionApkModel` 的调用顺序对调, 过滤结果就会变, 一致性全靠手写顺序维持.

## 根本原因

可变全局单例 = 把 "数据" 和 "数据的位置" 混在一起: 值随时可被任何代码改写, 读到的结果取决于 "谁最后写过", 这就是**时间耦合**(temporal coupling, 正确性依赖调用顺序). LLD 的 `known` 数据本应作为参数参与计算, 却通过篡改全局变量来 "顺便" 传递.

## 修复方案

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
    /** 启动时构建一次, 此后只读 — isAtLeast* 只依赖它 */
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
// HomeRepository.detectAndroid — 不再写全局, 改用参数
fun detectAndroid(lld: Lld?, androidInfo: MyAndroid): MyModel {   // androidInfo 由 VM 传入
    val enriched = lld?.let { AndroidInfo.enrichWithLld(androidInfo, it) } ?: androidInfo
    ...
}
```

`isAtLeast*` 一律改读 `AndroidInfo.current` (等价于原来的初始值, 不再被中途篡改). `initMyAndroid()` 从 `MyApplication.onCreate` 中删除 (`by lazy` 自带一次性初始化, 且懒加载避免启动期反射开销).

迁移注意: `myAndroid` 的读取点约 10 处 (`HomeRepository`, `AndroidVersionExt`), 全局替换为 `AndroidInfo.current` 后编译器会兜底找出遗漏.

---

<a id="qw-9"></a>

## 快赢批配方 · QW-9(次选) 冻结全局可变单例 myAndroid(AR-03 全局可变单例 myAndroid 有两个写入方) (整体暂缓)

> 配方原为快赢批独立文件, 2026-09-26 并入本宿主条目; 批次整体暂缓 (2026-09-16), 状态与总表见 [README 快赢批节](../README.md#qw).

**严重程度: P0 | 改动量: ~15 行, 编译器兜底找出全部遗漏 | 文件: `AndroidVersionExt.kt`, `HomeRepository.kt`**

## 问题代码

全局可变对象, 且有两个写入方:

```kotlin
// AndroidVersionExt.kt → MyAndroid — 字段全是 var
class MyAndroid(
    var api: Int,
    var apiFull: String,
    var version: String,
    var dessert: String? = null
)

// HomeRepository.detectAndroid() 里的 myAndroid 回写 — 第二写入方: 一个"取数方法"顺手改全局状态
myAndroid.api = api.toInt()
myAndroid.apiFull = apiFull
myAndroid.version = version
myAndroid.dessert = name
```

全项目的 `isAtLeastAndroid*()` 都读它 (`sdkInt`), 首页加载前后的返回值理论上可能不同; 过时应用过滤阈值 (`HomeRepository.getOutdatedTargetSdkVersionApkModel() 里的 it.targetSdkVersion < myAndroid.api`) 也读它, 与 `detectAndroid` 的调用顺序存在时间耦合.

## 引入提交

| 日期 | 提交信息 | 当时目的 |
|---|---|---|
| 2025-11-25 | Fix: Android 17 | Android 17 适配重写版本推导: var 字段 + 仓库里的第二写入方在这次成体 (推断: LLD 数据修正设备信息的最省事做法就是回写全局) |

## 为什么列次选而不是快赢

不是纯机械替换: `initMyAndroid()` 内部经由 `isAtLeastAndroid16()` 读 `myAndroid` 自身, 存在初始化顺序耦合 — 直接 `var` 改 `val` 会把 "先初始化, 后回写" 的两段式变成一次构造, 处理不好就自我引用. 必须按评审方案把推导逻辑放进局部变量, 最后一次性构造返回, 这正是它需要多想一步的原因.

## 修改方案

完整代码见[评审 02-SSOT 的 AR-03(全局可变单例 myAndroid 有两个写入方)](04-AR-03-myAndroid可变单例.md). 落点清单: `class MyAndroid` 全字段改 `val` 并变 data class;`initMyAndroid()` 改为纯函数 `build()` 返回新对象 (`isAtLeast*` 一律改读只读的初始值);`HomeRepository.detectAndroid` 删掉 4 行回写, LLD 修正改用 `copy()` 生成新对象作参数; 其余读点 (`HomeRepository` 5 处, `AndroidVersionExt` 内部 2 处) 全局替换, 编译器兜底.

## 直接原因

"这台设备的 Android 版本" 有两个事实版本 (Build 推导值, LLD 修正值), 两处写入, 多处读取, 读到的结果取决于谁最后写过 — 正确性全靠手写调用顺序维持.

## 根本原因

可变全局单例把 "数据" 和 "数据的位置" 混在一起; LLD 的 known 数据本应作为参数参与计算, 却通过篡改全局变量来 "顺便" 传递.

---
