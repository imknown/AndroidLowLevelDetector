<a id="i33"></a>

# 33 判定圆点语义: Home 每行各自为政, Unknown 常渲染为红色

> 返回 [README 索引](../README.md) · [7 · UI 与无障碍](../README.md#7--ui-与无障碍).

**严重程度: P1(🔴) | 状态: 有意接受 (非缺陷)**

- 不存在中央判定函数. 每个检测器各自从原始值选颜色; 相当一部分条目走两态的布尔重载 `toColoredMyModel(title, detail, condition)` — `true -> StatusColor.NO_PROBLEM`, `false -> StatusColor.CRITICAL` (`ui/base/list/MyModelExt.kt`). `HomeRepository` 里用它的是 `detectAb()` (A/B), `detectDynamicPartitions()`, `detectDsu()`, `detectDeveloperOptions()`, `detectAdb()`, `detectAdbAuthentication()` 六处.
- 未知 / 不可读值通常塌缩为红色: Android 版本未知时 `lld == null -> StatusColor.CRITICAL` (`HomeRepository.detectAndroid()`), 同形的还有 `detectMode()` (无 lld → CRITICAL), `detectBuildId()`, `detectSecurityPatch()`, `detectVndk()`, `getOutdatedTargetSdkVersionApkModel()`; 内核行 `linuxColor` 初值即 CRITICAL, 库里没收的内核系列一律判红; `detectDsu()` 在 `isDsuEnabled` 为 false 时详情取 "未知", 颜色按 false 判成 CRITICAL.
- 全仓 `R.string.result_unidentified` 只出现一次, 在 `detectGsi()` 里映射成 `StatusColor.WARNING` — 把 "未识别" 判成黄色的目前只有 GSI 这一行.
- 范围: 需求上红黄绿圆点是 Home 每个 item 的专属, Others / Prop / Settings 不渲染圆点. `StatusColor` 在 `OthersRepository` 与 `PropRepository` 零出现; Settings 不使用 `MyModel`, 构造上无圆点; Prop 已随止血批转无色; **Others 的失败行仍走 `toErrorMyModel()` (CRITICAL), 违反需求, 待修** — 见 [#69](69-Others失败行渲染红色圆点.md).
- **为什么接受**: 圆点颜色不设跨条目的统一映射 — 未知 / 读不到落在哪个颜色, 由各检测项自身的语义决定 ("不搞符号主义"), 现状即各条目语义的现行取值, 不构成缺陷. 三态结果类型与注册表的结构缺口不在此条, 见 [#03](../01-架构与分层/03-无三态模型与注册表.md); 读屏侧的状态词缺口独立存在, 见 [#40](40-圆点零语义.md).
