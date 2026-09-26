<a id="C6"></a>

# C6 判定圆点语义: Home 每行各自为政, Unknown 常渲染为红色

> 返回 [README 索引](../README.md) · [组2 · 主线重构 — 架构优先](../README.md#组2--主线重构--架构优先).


**严重程度: P1(🔴, 信任风险) | 修复: 三态结果类型落地时 (见 [R5](12-R5-无三态模型与注册表.md))**

- 不存在中央判定函数. 每个检测器各自从原始值选颜色; 大量条目是两态的 `toColoredMyModel(title, detail, condition ? green : red)`(如 A/B, DSU, 开发者选项, ADB, 动态分区, `toColoredMyModel(condition: Boolean) 重载`).
- **未知/不可读值通常塌缩为红色** — 例如 Android 版本未知时 `lld == null -> StatusColor.CRITICAL`(`HomeRepository.detectAndroid() 里的 lld == null -> StatusColor.CRITICAL`, 复验); 仅 GSI"未识别" 映射为黄色.
- 规格影响: NFR-7 要求 Unknown 绝不能读作失败; FR-15 把含义推迟到 Q9. 这会让 Explorer 画像被红色误导.

**裁定 2026-09-06(Q5)**: 四值颜色表示 (NoProblem / Warning / Critical / 无) 被接受为当前表示; 剩下的工作 = 给 Unknown 一个明确的映射 (**绝不悄悄显示成 Critical**)+ 异常证据显示出来 (与 C1 配套). 范围仅 Home(Others/Prop 纯展示). 落在三态结果类型落地时.


