<a id="i33"></a>

# 33 判定圆点语义: Home 每行各自为政, Unknown 常渲染为红色

> 返回 [README 索引](../README.md) · [7 · UI 与无障碍](../README.md#7--ui-与无障碍).

**严重程度: P1(🔴) | 修复难度: 中 (先定映射)**

- 不存在中央判定函数. 每个检测器各自从原始值选颜色; 相当一部分条目走两态的布尔重载 `toColoredMyModel(title, detail, condition)` — `true -> StatusColor.NO_PROBLEM`, `false -> StatusColor.CRITICAL` (`ui/base/list/MyModelExt.kt`). `HomeRepository` 里用它的是 `detectAb()` (A/B), `detectDynamicPartitions()`, `detectDsu()`, `detectDeveloperOptions()`, `detectAdb()`, `detectAdbAuthentication()` 六处: 判定只剩 "是/否", 没有第三种出口.
- **未知/不可读值通常塌缩为红色**. Android 版本未知时 `lld == null -> StatusColor.CRITICAL` (`HomeRepository.detectAndroid()` 的 `val color = when { ... }` 第一支); DSU 更直白 — `detectDsu()` 在 `isDsuEnabled` 为 false 时详情取 `androidR.string.unknownName` ("未知"), 同一句里颜色按 false 判成 CRITICAL, 于是 "未知" 显示成失败.
- 颜色确实逐行决定的反例: 全仓 `R.string.result_unidentified` 只出现一次, 在 `detectGsi()` 里映射成 `StatusColor.WARNING` — 把 "未识别" 判成黄色的目前只有 GSI 这一行.
- 四值枚举 `StatusColor` (`NONE` / `NO_PROBLEM` / `WARNING` / `CRITICAL`, `ui/theme/ExtendedColors.kt`) 与色值映射 `ExtendedColors.of()` 是当前的表示方式, 本条不是要换掉枚举; 缺的是 "Unknown 该显示成哪一档" 这个决定 — 它不能悄悄落在 CRITICAL 上.
- 危害落在普通用户: 红色读作 "这机器有问题", 而实际只是 App 认不出这个值.
- 异常证据到行的那一半已经落地: 检测块抛异常时 `guardedMyModel()` / `guardedMyModels()` 经 `toErrorMyModel()` 把异常信息写进行详情, 颜色 `CRITICAL` (`ui/base/list/MyModelExt.kt`, `f089854a`). 剩下的只是判定语义.
- 范围: 状态色只由 Home 的检测器给出 — `StatusColor` 在 `OthersRepository` 与 `PropRepository` 零出现, 那两个页面上唯一带色的行是 `toErrorMyModel()` 的失败行. 所以本条只管 Home.

**前置 (仍未决)**: 本条依赖 [#03](../1-架构与分层/03-无三态模型与注册表.md) 的三态结果类型 — 没有 "未知" 这个结果类型, 颜色映射就没有可挂的东西. #03 与本条卡在同一个未决问题上: **三态 → 圆点颜色映射** (核心一句是 "未知绝不显示成红色"). 这个问题至今没有结论, 除本条目与 #03 之外在仓库里没有别的落点. 同一主题在无障碍侧的缺口见 [#40](40-圆点零语义.md) — 状态词资源两条同批加一次即可.
