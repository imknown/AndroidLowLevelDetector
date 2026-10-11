<a id="i59"></a>

# 59 · Expressive 主题跟进

> 返回 [README 索引](../README.md) · [7 · UI 与无障碍](../README.md#7--ui-与无障碍).

**严重程度: P3 | 修复难度: 中**

**结论**: 不是缺陷, 因为 `AppTheme()` 现为标准 M3 (light/dark scheme + Android 12+ 动态取色),  
是 Compose 迁移决策 8 的有意取舍 (Expressive 主题 API 当时已从 material3 1.4.0 stable 线移除, 仅在 1.5.0-alpha).  
本条登记一个**跟版本走的主题观感升级候选项**, 随 material3 1.5 落地实施.

**证据**:

- `ui/theme/Type.kt` 文件头注释原文: "Re-tune when Material 3 Expressive becomes available.",  
  迁移落地时就挂了升级标记.
- `ui/theme/Theme.kt` 的 `AppTheme()` 分支只在 `dynamicDarkColorScheme` / `dynamicLightColorScheme` / `darkScheme` / `lightScheme`  
  之间选; 同文件原四套零引用的对比度 scheme 已随 [#31](../archived/06-性能/31-主题死配色.md) 注释保留.
- 等的是什么: material3 1.5 线的 `MaterialExpressiveTheme` / `expressiveLightColorScheme()` /  
  `MaterialTheme.motionScheme` (迁移期 2026-09-16 对官方文档与 androidx 仓库逐条核对的结论);  
  当前 pin `gradle/toml/android.toml` →  
  `compose-bom` 条目 (其 material3 版本由 BOM 决定, 见 [#37](37-滚动条可拖动是stub.md) 与 121dd849 的替身记录).
- 出处: Compose 迁移遗留优化, 2026-09-25 整理旧 tracker 时的抢救记录把它记为 "活" 项 ([归档  
  spec](../../../spec/archived/2026-09-17-21-14-43-+0800-compose-migration-cn/plan.md) 决策  
  8). 它曾作为附录收入当时的 tracker; 现行 tracker 按问题性质重建后已无对应条目 (2026-10-04 全目录 grep 零命中),  
  本条补回并把出处锚定到归档 spec.

**处理**: 保持现状. material3 1.5.0 stable 转正后 (或按 AGENTS.md 版本分层政策研究后收 alpha) 整体重调主题:  
配色 / 动效 / 形状一套走,  
`Type.kt` 排版随之重新调制. 动手时 Expressive 重写 `Theme.kt` 会覆盖 #31 注释保留的那四套对比度 scheme (已归档,  
见 [#31](../archived/06-性能/31-主题死配色.md)), 删除或恢复均以重调后的方案为准, 不受其约束.
