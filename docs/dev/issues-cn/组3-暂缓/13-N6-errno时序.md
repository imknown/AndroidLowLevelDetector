<a id="N6"></a>

# N6 errno 在日志调用之后才读取 (P2)

> 返回 [README 索引](../README.md) · [组3 · 暂缓](../README.md#组3--暂缓-原因见危害列标注).


**结论**: `BinderDetector.cpp → getErrorNo()` 先 `__android_log_print` 再取 errno — 日志函数内部做 socket 写入可能覆盖 errno, 返回给 Kotlin 的错误码不再是真实失败原因.
**证据**: Kotlin 侧 (`OthersRepository.getBinderStatus()`) 靠 -2/-13 区分 "文件不存在/无权限", errno 失真后本该 "不支持" 的结果会变 "未知".
**修复方向**: 系统调用失败后立刻把 errno 存进局部变量, 再打日志.



