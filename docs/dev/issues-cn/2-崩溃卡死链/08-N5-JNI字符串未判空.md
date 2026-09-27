<a id="N5"></a>

# N5 JNI GetStringUTFChars 未判空 (P2)

> 返回 [README 索引](../README.md) · [2 · 崩溃卡死链 — 严重度最高](../README.md#2--崩溃卡死链--严重度最高).


**结论**: `BinderDetector.cpp → getBinderVersion()` 里 `env->GetStringUTFChars(driver, nullptr)` 的返回值没判空, OOM 时返回 nullptr 并挂起异常, 直接传给 `open()` 会 SIGSEGV.
**修复方向**: 判空后直接 return (JVM 已挂 OOM 异常, 让 Java 层收到即可). JNI 标准必查项.


