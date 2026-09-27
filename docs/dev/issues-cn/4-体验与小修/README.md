# 4 · 体验与小修 — 用户可感知

> 返回 [README 索引](../README.md) · [总览表本层](../README.md#4--体验与小修--用户可感知) — 用户可感知的体验 / 性能问题与独立小修; 不占主线, 可攒批或穿插. 多数开放, AR-14 / N10 随暂缓批 (2026-09-13 / 2026-09-26 裁定); 滚动条, 手势与布局类的已裁定条目见文末已裁定块. F 系列条目出自三份 Compose 复查的合并报告 (核对记录见 [附录 B](../README.md#appendix-b)). 文件名前缀 = 总览表行序.

1. [F4 · 主题文件留有 4 套对比度死配色](01-F4-主题死配色.md)
2. [AR-14 · Prop 页设置项逐 key 查询](02-AR-14-Prop页逐key查询.md) — 暂缓 2026-09-13
3. [F6 · onBack 与多返回栈自相矛盾](03-F6-onBack矛盾.md)
4. [F15 · toPersistentList() 每次重组重新分配](04-F15-toPersistentList重组分配.md)
5. [AR-17 · QUERY_ALL_PACKAGES 用途申报缺档](05-AR-17-QUERY_ALL_PACKAGES申报缺档.md)
6. [N10 · Settings 行缺 a11y 语义](06-N10-设置行缺a11y语义.md) — 暂缓 2026-09-26
7. [F8 · ExtendedColors.of() 多标 @Composable](07-F8-of多标Composable.md)
8. [AR-13 · 零散小问题 (可修 6 项)](08-AR-13-零散小问题.md)
9. [F11 · currentTabIndex 无越界保护](09-F11-tabIndex越界.md)
10. [F12 · 图标 fillColor 硬编码黑色](10-F12-图标fillColor硬编码.md)
11. [F10 · @Stable 标在不需要的地方 (HomeViewModel)](11-F10-冗余Stable.md)

## 已裁定, 无待办

滚动条, 手势与布局类: 负责人已有明确裁定, 无待办, 保留只为防止重复讨论 (裁定理由见条目).

12. [F1 · 滚动条事件总线零订阅者](12-F1-滚动条死总线.md) — 保持现状, 等官方滚动条
13. [R10 · 滚动条 stub](13-R10-滚动条stub.md) — 同 F1
14. [F5 · Card 空点击](14-F5-Card空点击.md) — 保留水波纹
15. [C4 · 拖拽中重建吞手势](15-C4-拖拽重建吞手势.md) — 永久接受
16. [R1 · 工具栏不随滚动隐藏](16-R1-工具栏不随滚动隐藏.md) — Compose 时代交付物
17. [R2 · 无自适应布局](17-R2-无自适应布局.md) — 遗留优化
