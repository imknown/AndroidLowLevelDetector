<a id="N3"></a>

# N3 lld.json 的 scheme 闸门未启用; api 字段 toInt 无防护 (P2)

> 返回 [README 索引](../README.md) · [组3 · 暂缓](../README.md#组3--暂缓-原因见危害列标注).


**结论**: `Lld.kt → SCHEME_VERSION` 定义后全仓零引用 — `scheme` 字段本该是 "数据格式过新" 的闸门, 现在上游升 scheme 后老版本只会得到一条含糊的解析错误然后静默回退旧缓存.
**证据**: 同族: `Lld.Android.Android.api` 是 String, `AndroidVersionExt` 与 `HomeRepository` 多处 `toInt()` 无 `toIntOrNull` 防护, 上游一旦写成 "37.1" 即运行时异常. 序列化本身开了 `ignoreUnknownKeys`, 加字段不崩, 只有改字段类型/结构才触发.
**修复方向**: 解析后校验 `scheme == SCHEME_VERSION`, 不符走明确提示与回退; api 字段换 `toIntOrNull` + 兜底.


