<a id="N4"></a>

# N4 myAndroid 跨线程无同步读写 (P2, 与 AR-03 同根)

> 返回 [README 索引](../README.md) · [组2 · 主线重构 — 架构优先](../README.md#组2--主线重构--架构优先).


**结论**: `AndroidVersionExt → myAndroid` 的四个字段是 var, `detectAndroid()` 在 `Dispatchers.Default` 上改写, 而其他页签的加载协程同时经 `isAtLeastAndroidX()` 读它, 无任何同步.
**证据**: 多页签并发加载时其他线程可能读到写了一半的值; 预览版系统上 `detectAndroid` 运行中会把 api 从 36.1 语义改回 36, 同一次加载前后判断不一致.
**修复方向**: 与 AR-03 的 "初始化后冻结" 合流实施即同时解决; 短期可先在加载前做局部快照.



