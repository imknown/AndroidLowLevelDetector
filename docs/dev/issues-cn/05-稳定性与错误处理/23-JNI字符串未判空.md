<a id="i23"></a>

# 23 JNI GetStringUTFChars 未判空

> 返回 [README 索引](../README.md) · [5 · 稳定性与错误处理](../README.md#5--稳定性与错误处理).

**严重程度: P2 | 修复难度: 低**

**结论**: `binderDetector/src/main/cpp/BinderDetector.cpp` 的 `getBinderVersion()` 里, `env->GetStringUTFChars(driver, nullptr)` 的返回值 `driverChars` 没有判空就被当 C 字符串用起来 — JNI 规范是内存不足时返回 NULL 并在 JVM 侧挂起异常, 此时 `open(driverChars, O_RDONLY | O_CLOEXEC)` 拿到的是 NULL, 同一指针还随 `fd < 0` 分支进 `getErrorNo()`, 在那儿当 `%s` 参数打日志并交给 `ReleaseStringUTFChars()`. 把 NULL 当合法字符串用属于未定义行为, 崩在哪一步取决于运行期 (是否带 CheckJNI, 日志是否被过滤).

另一面同样坏: OOM 异常保持 pending, 而 native 函数照常返回一个错误码. Kotlin 侧 `OthersRepository.getBinderStatus()` 只看返回的 Int, `-1` 既不是 `-2` (ENOENT) 也不是 `-13` (EACCES), 于是落进 `else` 分支显示 "未知" — 真正该浮上来的 OOM 被吞在返回值之后.

**修复方向**: 取到 `driverChars` 后判空并直接 `return` (JVM 已挂 OOM 异常, 让 Java 层收到即可), 不再往下走 `open()` 与 `release()`. JNI 标准必查项.
