<a id="N1"></a>

# N1 日期解析未指定 Locale, ar/fa 等地区启动即崩 (P1)

> 返回 [README 索引](../README.md) · [组3 · 暂缓](../README.md#组3--暂缓-原因见危害列标注).


**结论**: `base/extension/DateTimeExt.kt → formatToLocalZonedDatetimeString()` 两个重载的 `DateTimeFormatter.ofPattern(pattern)` 都不带 Locale, 跟随系统 "格式地区". ar/fa 等使用本地数字 (٠١٢) 的地区, 用它解析 ASCII 的 `"2026-09-18 13:00 +0800"` 直接抛 `DateTimeParseException`.
**证据**: 触发点 `HomeRepository.detectMode() 里对 lld.version 的解析` (每次 Home 加载都走, 含离线模式) 与 `SettingsRepository.getBuiltInDataVersion() 里对资产 lld 版本的解析`; 加载协程无兜底 (= C1/F2), 这些地区用户首次进首页即崩. 与 AR-15 是不同触发条件: 上游格式不变也会崩.
**修复方向**: 两处 pattern 补 `Locale.US` (或 ROOT), 约 4 个调用点随之受益. 修复时机随 C1 类 "业务逻辑暂缓" 裁定与否由负责人定.


