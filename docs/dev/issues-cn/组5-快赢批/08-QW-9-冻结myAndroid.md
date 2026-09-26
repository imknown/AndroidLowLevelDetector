<a id="qw-9"></a>

# QW-9(次选) 冻结全局可变单例 myAndroid(AR-03 全局可变单例 myAndroid 有两个写入方)

> 返回 [README 索引](../README.md) · [组5-快赢批](../README.md#目录).


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

完整代码见[评审 02-SSOT 的 AR-03(全局可变单例 myAndroid 有两个写入方)](../组2-主线重构/04-AR-03-myAndroid可变单例.md). 落点清单: `class MyAndroid` 全字段改 `val` 并变 data class;`initMyAndroid()` 改为纯函数 `build()` 返回新对象 (`isAtLeast*` 一律改读只读的初始值);`HomeRepository.detectAndroid` 删掉 4 行回写, LLD 修正改用 `copy()` 生成新对象作参数; 其余读点 (`HomeRepository` 5 处, `AndroidVersionExt` 内部 2 处) 全局替换, 编译器兜底.

## 直接原因

"这台设备的 Android 版本" 有两个事实版本 (Build 推导值, LLD 修正值), 两处写入, 多处读取, 读到的结果取决于谁最后写过 — 正确性全靠手写调用顺序维持.

## 根本原因

可变全局单例把 "数据" 和 "数据的位置" 混在一起; LLD 的 known 数据本应作为参数参与计算, 却通过篡改全局变量来 "顺便" 传递.

---
