# ST-11 docs-closeout — 修改计划报告

> 状态: living (预生成; 动工时与负责人预期对比, 必要时显式修订). 计划: [plan.md](plan.md) 子任务 11. 风险: 低 (纯文档; 集中一次避免中途反复).
> 修订 (2026-10-02): 增补 "代码注释 #05 悬空引用改写" 一项 — ST-05 v1 评审 [建议], 负责人裁定归本子任务统一处理.
> 修订 (2026-10-03, 动工时): 原方案的 AGENTS.md "No DI framework" 条目与手写 Factory 句已在 ST-08 的 findings 裁定中先行改写 (overview + Architecture 条目), 本任务只补: 升级纪律句 (版本锁升) 与 "新增检测项" 第 2 步的 `@Inject` 说明 (AGENTS.md + AGENTS-cn.md 同步). `16-阻塞调用与HttpClient.md` 随 HttpClient 半删除改名 `16-阻塞调用跑在CPU线程池.md` (5 处引用同步); issues-cn 另含 ST-08 挂起物记录的 5 处陈旧现场 (README 诊断 / 04 / 32 / 45 / 42) 一并改写; #02 条目整体改写为 "两段已落地 + 剩余收口" 并降档 P0→P1 (剩余面收窄, 评审复核此判断).
> 修订 (2026-10-03, v1 findings): v1 评审 5 条全修 — README #16 索引链接文字、24 §3 与已落地客户端复用的解耦、getMyString 计数刷新 (102/93 → 100/91, 波及 02 / README / 01)、已删代码历史引用改用 commit 哈希形态 (原稿 ST-07/ST-08 有两处顺序倒置, 换算时一并纠正)、范围扩展 3 处同类陈旧点 (19 / 27 / 24 文件清单). 收尾清扫又追补 2 处 (README "已经做对的地方" 的 ShellManager 现在时引用 / 07 的旧 getStringProperty 片段). 另: `retrospective.md` 负责人审阅后按其裁定删除未入库, plan.md 指针同步改写.

## 要改什么

- 代码注释的 `issues-cn #05` 引用改写 (6 处, 随 #05 条目删除而悬空): `HomeViewModel` / `OthersViewModel` / `PropViewModel` / `SettingsViewModel` 的 Metro 接线注释块, `MyApplication` 的 `appGraph` 注释, `AppGraph` 的组合根注释 — `issues-cn #02/#05` 改为不含编号的叙述; `#02` 单独出现的引用不受影响 (条目仍在).

- `AGENTS.md` / `AGENTS-cn.md` 现状改写:
  - "No DI framework" 条目 → "DI = Metro + MetroX (1.4.5), 编译器插件, 无 KSP; 插件/运行时/MetroX 同 `version.ref` 锁升, 升 Kotlin 必查兼容矩阵"
  - 架构节的手写 Factory 句 → 图模式 (`@Inject` 构造 + `metroViewModel<>()`), "新增检测项" 第 3 步同步 ("给 Repository/DataSource 挂 `@Inject` 并作为 ViewModel 构造参数")
- `issues-cn`:
  - `02-服务定位器上帝对象.md` 改写: Manager 壳 / LldManager / HttpClient / SharedPreferences 已随 ST-06~10 落地, 剩余静态显式分流 — `getMyString` 102 处归 #01, `myAndroid` 归 #09, 设置页直读归 #08/#10, `MyApplication.instance` 由 #02 收口撤除
  - `16-阻塞调用与HttpClient.md` 改写: 客户端归属与复用半已由 ST-06 关闭 (引擎配置原样上移, 未加状态码闸门 — 该遗留归 #24), 保留 dispatcher 半 (与 #14 的重叠关系说明)
  - README 索引/总览表同步; ST-05 已关 #05
- `plan.md`: 收尾对照"负责人预写", 清待学习清单, 横切复盘写入本目录

## 为什么

AGENTS.md 描述现状, 代码落地后必须就地改写 (文档规则), 否则两份约定互相矛盾; tracker 的条目按其自身规则关闭/改写, 不留 "已解决" 行.

## 怎么验证

- `rg "No DI framework|没有 DI 框架|HomeViewModel\.Factory" AGENTS.md AGENTS-cn.md` 零残留
- issues-cn 内部互链无悬挂 (`#05` 已删, 指向它的引用需已清理)
- 代码注释零残留: `rg "issues-cn #05" app/src --glob "*.kt"` 零命中 (6 处已改写)
- 全仓最终构建 + release 冒烟 (`assembleFossRelease`, R8 下 Metro 无反射理论无需 keep 规则, 实测确认)

## 依赖与前提

- ST-06 ~ ST-10 全部完成
