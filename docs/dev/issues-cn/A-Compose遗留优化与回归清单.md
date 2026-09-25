# Compose 迁移遗留优化与回归清单

> 2026-09-25 起收录于 [项目问题汇总](README.md) 作为附录; 原为 compose-migration-plan-cn 09 章的 9.3 回归清单与 9.4 遗留优化 (该教学目录已删, 原文见 git 历史, 章首更正已并入正文). 遗留优化不是问题 — 是迁移时刻意不做的增强项, 立项前先看现状.

## 一, 回归验证清单 (发布前 / 任何 UI 改动后全量过一遍)

1. **四个页面**: 数据正确, 卡片视觉逐项对齐 (圆角, `surfaceBright` 底色, 字号, 色点, 间距 12dp/10dp);
2. **下拉刷新**: 三列表页手势, 转圈配色, 刷新期间列表不闪空;
3. **滚动条**: 设置项 UI 存在且 SharedPreferences 读写正常, 但当前为 inert (滚动条实现推迟, 见 [R10](05-已裁定事项.md#R10) / [F1](A-Compose代码Review报告.md#F1)); 验证切选项不崩溃, 值持久化即可;
4. **设置项继承**: 从旧版本升级, 主题/滚动条/两开关的值全部保留 (SharedPreferences 键未动);
5. **主题**: 三档主题模式 × 深浅色 × 动态取色 (Android 12+) × 高对比度 (系统辅助功能) 抽查 (原文的 "四档" 已更正: "跟随省电模式" 随去 AppCompat 化退役, 旧存值启动时一次性迁回);
6. **导航**: 标签切换各自保留状态; 杀进程重启回到原标签; 返回手势/预测性返回动画正常;
7. **事件链路**: 过期应用排序开关 → Home 对应条目刷新; 网络数据开关 → Home 联网/离线策略切换;
8. **外链与彩蛋**: 五个外链, 无浏览器 Toast, 版本七连击;
9. **多语言**: zh-rCN / zh-rTW / fr-rFR 各过一遍 (`stringResource` 的格式化参数, RTL 镜像);
10. **双 flavor**: `assembleFossRelease` / `assembleFirebaseRelease` 出包, 混淆开启, shop 链接各归其主;
11. **性能主观对比**: 冷启动, 切标签, 快速滚动 (可用 Macrobenchmark 做量化, 属加分项).

## 二, 遗留优化 (迁移时刻意不做, 供后续立项)

原 8 条的当前状态 (2026-09-25 复核):

1. **滚动条换代** — 活, 跟踪见 [R10](05-已裁定事项.md#R10) / [F1](A-Compose代码Review报告.md#F1): material3 1.5 的官方滚动条转正后从零接入 (`Modifier.nonInteractiveScrollbar`, 自带淡出动画; 自绘版从未落地, 不存在 "一行替换").
2. ~~**主题模式去 AppCompat 化**~~ — **已完成** (`733c6941` 换 `ComponentActivity` → `26b9094f` 主题改 `StateFlow` → `e51260e6` XML 主题改平台父级 → `f66562d4` 删 `appcompat`/`material` 依赖).
3. **Expressive 主题** — 活: material3 1.5 线的 `expressiveLightColorScheme()` / `MaterialExpressiveTheme` / `MaterialTheme.motionScheme` 可让整体观感升级 (弹出手势, 组件动效全套), 评估稳定后切换; `Type.kt` 里 "将来 Expressive 可用时再重新调制" 的注释同此事.
4. **Style API 跟进** — 活: foundation 1.13 把 Style API 重构为 "DSL + 修饰符" 后**从零立项** (原计划的试水代码从未落地 — stable 1.12.1 无该签名, 见决策 7); material3 组件开放 `style` 参数后进一步收纳.
5. **设置存储现代化** — 被吸收进 [AR-02](02-SSOT-唯一数据来源.md#AR-02): 先 `SettingsStore` (SharedPreferences 加 Flow), DataStore 只换一个文件; 偏好状态改 Flow, `MyApplication.sharedPreferences` 单例退场.
6. **自适应导航** — 被吸收进 [R2](05-已裁定事项.md#R2): `material3-adaptive-navigation-suite` (1.5.0-alpha, 不在 BOM), 手机/折叠/平板自动切换底栏与侧栏.
7. **底栏随滚动隐藏** — 被吸收进 [R1](05-已裁定事项.md#R1) (FR-12): 用 `Modifier.nestedScroll` + 自定义 `NestedScrollConnection` 包住 `Scaffold`.
8. **测试基建** — 被吸收进 [AR-16](01-架构.md#AR-16) / [A6](01-架构.md#A6): Compose UI 测试 (`createComposeRule`), Robolectric, Macrobenchmark — 对应 "CI 门禁待测试基建补齐后再上" 的决定 (快赢 [QW-1](06-快赢清单.md#qw-1) 同此前提).

另一条独立于本清单的数据加载惰性化 (接网络/数据库时按 `WhileSubscribed(5_000)` 重造) 见 [A·迁移期观察记录](A-迁移期观察记录.md) 的遗留优化节.
