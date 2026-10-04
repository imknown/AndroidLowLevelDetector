<a id="i22"></a>

# 22 lld.json 的 scheme 闸门未启用; api 字段 toInt 无防护

> 返回 [README 索引](../README.md) · [5 · 稳定性与错误处理](../README.md#5--稳定性与错误处理).

**严重程度: P2 | 修复难度: 低~中**

**结论**: `ui/home/model/Lld.kt` 的 `SCHEME_VERSION = 1` 定义后全仓零引用 — 该文件之外再无任何一处出现这个标识符. 数据类里的 `scheme: Int` 字段被反序列化进模型, 但没有任何地方拿它跟 `SCHEME_VERSION` 比: `scheme` 本该是 "数据格式过新" 的闸门, 现在上游升了 scheme, 老版本只会把它当成一次普通的解析失败, 然后静默回退离线数据.

**证据**:

- `ui/common/JsonExt.kt` 的 `json = Json { ignoreUnknownKeys = true }`: 上游 **加字段** 不报错也不崩; 改字段类型或结构会在 `decodeFromString` 抛异常; 而 **只改值的写法** (`api` 从 `"37"` 写成 `"37.1"`) 反序列化照样成功, 坏值原样进模型, 引爆点被推到后面的 `toInt()`.
- `Lld.Androids.Android.api` 是 `String` (`lld.android.stable.api` 等取值点都经它), 消费处一律裸 `toInt()`: `AndroidVersionExt.kt` 的 `isLatestStableAndroid()` (`lld.android.stable.api.toInt()`) 与 `isSupportedByUpstreamAndroid()` (`lld.android.support.api.toInt()`), 以及 `HomeRepository.detectAndroid()` 的 `myAndroid.api = api.toInt()` — 三处都没有 `toIntOrNull` 兜底, 上游把 `"37"` 写成 `"37.1"` 即抛 `NumberFormatException`.
- 降级形态: 联网解析失败时 `HomeViewModel.tryDetectOnline()` 走 `tryDetectOffline()`, 把 `R.string.lld_json_parse_failed` 拼上 `e.fullMessage` 作为错误文案交给 `HomeRepository.detectMode()`, 附在那条 "lld json 来源" 行的详情末尾 — 用户看到的是一条含糊的解析失败, 而不是 "数据版本过新".

**失配已经发生的一例**: `app/src/main/assets/lld.json` 的 `toybox` 现有 4 个键 (`stable` / `support` / `mainline` / `master`), 而 `Lld.Toyboxes` 只声明 3 个 (无 `mainline`) — `ignoreUnknownKeys` 把那一档参考数据静默丢掉, `detectToybox()` 永远读不到它. 数据与模型已经悄悄漂移, 没有任何闸门或日志报告这件事. 这也正是 `scheme` 闸门该拦的形状: 模型与数据任何一侧演进, 现在都只能靠人眼发现.

**连带面**: 模型字段除 `extension` / `phase` 外全部必填, `Json { ignoreUnknownKeys = true }` 对 "加字段" 宽容, 对 "删键 / 改键名 / 改类型" 就是整份 `decodeFromString` 抛异常 → `lld = null` → 各行按 `lld == null` 分支落到 CRITICAL (见 [#33](../07-UI与无障碍/33-Unknown渲染红色.md)).

**严重程度说明**: `api` 串坏了不再拖垮整页 — 逐条错误隔离 (`f089854a`) 之后, `detectAndroid()` 的异常被它自己的 `guardedMyModel(R.string.android_info_title)` 接住, 降级成该探针的一行错误. 失效面从 "整页" 缩到 "一行", P2 定级不变.

**修复方向**: 解析后校验 `scheme == SCHEME_VERSION`, 不符走明确提示与回退; `api` 字段换 `toIntOrNull` + 兜底.
