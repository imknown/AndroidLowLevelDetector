<a id="i70"></a>

# 70 mainline 判定量纲错位 (系统版本号对日期)

> 返回 [README 索引](../README.md) · [5 · 稳定性与错误处理](../README.md#5--稳定性与错误处理).

**严重程度: P2 | 修复难度: 中 (真实格式已由负责人给出, 比较规则仍待定) |  
状态: 开放 (随 #43 的处置另立; 现状已由特征测试锁住, 修复时由本条接管)**

`mainlineMatch()` (`ui/home/repository/LldComparisons.kt`) 拿 mainline 模块包的  
`versionName` 与库里 `googlePlaySystemUpdates` 的**日期** (如 `2026-11-01`) 做字典序比较,  
并带补后缀 `"$versionName-01"`.

**比较本是为日期格式设计的**: 后缀把月精度日期 (`2026-11`) 补到日的精度,  
使它能与 `2026-11-01` 相等判 Met. 真实世界的取值分布 (负责人给出):

| 来源 | versionName 实测格式 | 现行判定 |
|---|---|---|
| 真机 `com.google.android.modulemetadata` | `2023-07-01` (日期) | 日期对日期, 维度正确 |
| 模拟器 (Google API system image) | `2023-07-01 S+` (日期加后缀) | 日期前缀主导, 等效日期比较 |
| 真机 `com.android.modulemetadata` | 系统版本号, 如 `14` | **维度错位**: `"14" < "2026-..."` ('1' 对 '2'), Android 10~19 全部恒判 None (红) |
| (假设) `3.1` 这类 3 开头 | 同上机制反向 | 字典序恒判 Met (绿) |

两个方向的误判都源于 `com.android.modulemetadata` 报系统版本号这一路;  
日期与带后缀日期两路现行判定是合理的. 包查询失败 (`versionName` 空串) 也恒判红,  
诚实的结论应是 Unknown.

现状的特征测试 (`DetectMainlineTest` 的五例直测) 把上表行为原样锁住, 包括误判本身;  
修复时先改测试再改实现, 让差异可见.

修复前置条件: 由负责人按 mainline 模块版本的真实语义定比较规则  
(例如: 按包名分流: `com.android.modulemetadata` 走系统版本对照, 其余走日期比较;  
或统一解析后按类型判), 再动实现. 本条与 [#67](67-检测规则方向存疑.md) 同类:  
规则未定前不改行为.
