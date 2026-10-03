# Metro + MetroX DI 接入 — 修改计划

> 状态: living. 决策依据: `docs/dev/issues-cn` 的 #02 / #05 修订版 (选型表与两段式划分) 与此前的可行性分析 (版本兼容矩阵 / 构建性能 / 风险已逐项核实). 本页是任务级拆分与执行约定; 全部子任务的修改计划报告 (`subtask-NN-*.md`) 已在动工前一次性生成于本目录.

## 负责人预写 (任务完成前不改, 收尾对照)

- 要解决什么: 解决 Service Locator 和混乱的对象生命周期管理.
- 你的思路: 对象不自己创建依赖, 而由外部容器在运行时把所需依赖传入, 从而实现控制反转与解耦.
- 预测的最大风险: Metro 不熟.

## 目标

把组合根 (composition root) 落地为 Metro 图, 对应 issues-cn #02 的两段式:

1. **第一段**: 图 + 四个 ViewModel 链 — 四个伴生 `Factory` 整体删除, `AppRoot()` 四条 entry 改 `metroViewModel<>()`.
2. **第二段**: 依赖型静态单例逐个汇入图 — `HttpClient`, `IShell` / `IProperty`, `LldManager`, `SharedPreferences`.
3. **收尾**: AGENTS.md 现状改写, issues-cn 条目按 tracker 规则关闭 / 改写.

## 执行约定 (每个子任务相同; 已按负责人修订的工作流更新)

- 门槛: 每一步都要负责人点头 — review 结果 (包括"干净") 先报告, 提交与进入下一个子任务都等同意.
- 进度账本: `progress.md` 每过一道闸门由 AI 更新; 新会话/新 agent/新模型 接续时先读 AGENTS.md + 账本, 按账本的"下一步"继续.
- 修改计划报告: 计划闸门通过后一次性预生成全部 `subtask-NN-<slug>.md` (ST-01 的完成式报告按过渡保留); 预生成批次同样过新上下文 subagent 的后台自动 review (v1 起, 有发现报负责人复核确认, 只修确认项, 改了才进下一版); 每个子任务动工前走默认对照路径: 负责人给 目标句 + 问题单 (可空), 替换类任务加 覆盖核对 (对照总目标找漏项), AI 先给概念 primer 并逐条回答问题单、附覆盖核对清单; 报告过时先显式修订; 说开始才动工; 闸门节拍由负责人定, 亲手实现不强制进入流程 (负责人在流程外自行安排学习). 不再另写完成后的总结报告.
- 每个子任务: 最小高内聚 diff, 单独可编译; 注释用负责人聊天语言书写、解释**为什么**这么改 (对齐代码库注释密度), 提交前整体译为英语; 构建验证 `./gradlew assembleFossDebug`.
- 流程: 动工对照 (默认路径: 目标句 + 问题单 + 覆盖核对, AI 附 primer) → 实施 → 新上下文 subagent 后台 review (v1) → 有发现: 报告 → 负责人复核确认后只修本轮要求的内容 (改了才进入 v2 后台 review, 确认无需修改则直接走等点头提交那一步) → v2, v3 同理, 直到某一轮无新的实质发现 → 负责人放行留一句自己的话 → 提交 → 再等点头进入下一个子任务.
- 收敛护栏: 主观风格偏好与过早优化不构成新一轮; 连续几轮无新发现仍在打转时交由负责人收束, 不无限 review.
- 风险标注: 高 = ST-01, ST-02, ST-06, ST-09; 生疏领域 = ST-01 (Metro 接入), ST-02 (MetroX multibinding 首用), ST-06 (首个 @Provides + 客户端生命周期语义变化); 其余为低风险 (机械/样板). 低风险子任务的 review 与提交闸门可合并, 也可由负责人亲自读 diff 代替.
- 本计划自身同样过 review 闸门: 负责人复核确认后自动提交 plan.md, 才进入子任务循环.
- 每个子任务独立提交, 可单独 revert.

## 子任务拆分

| # | slug | 范围 | 为什么单列 |
|---|------|------|-----------|
| 01 | build-wiring-and-graph-skeleton | `gradle/toml/thirdParty.toml`, `app/build.gradle.kts`, `di/AppGraph.kt` (新), `di/AppViewModelFactory.kt` (新), `MyApplication` (+appGraph), `MainActivity` (工厂下发) | 构建接线一次到位; 图与工厂下发是后续每一步的依赖面; 不碰任何业务类, 可独立冒烟 — Gradle 9.7.1 + AGP 9.4.1 组合的验证也落在这一步 |
| 02 | home-viewmodel-chain | `HomeViewModel`, `HomeRepository`, `LldDataSource`, `MountDataSource`, `AppInfoDataSource`, `AppRoot` (HomeKey entry) | 试点链: 一次打通 "注解 → multibinding → entry" 全路径, 验证 `binding<ViewModel>()` 与 entry 作用域; `AppInfoDataSource` 被两条链共享, 在此首次挂 `@Inject` |
| 03 | others-viewmodel-chain | `OthersViewModel`, `OthersRepository`, 6 个 DataSource, entry | 机械重复试点模式; 一链一提交, 保持 diff 可 review |
| 04 | prop-viewmodel-chain | `PropViewModel`, `PropRepository`, `PropertiesDataSource`, `SettingsDataSource` (prop), entry | 同上 |
| 05 | settings-viewmodel-chain | `SettingsViewModel`, `SettingsRepository`, `FingerprintDataSource` (settings), entry; 清掉 `AppRoot` 的 androidx `viewModel` 残留 import | 第一段在此完成; 按 tracker 规则删除 #05 条目并同步 README 索引 |
| 06 | http-client-singleton | `AppGraph` 伴生 `@Provides` + `@SingleIn` `HttpClient` (引擎配置自 `LldDataSource` 上移), `LldDataSource` 构造注入 | #16 后半 (客户端无归属): 网络客户端生命周期是独立关注点; 也是本项目第一个真正的 `@Provides` |
| 07 | shell-binding | `AppGraph` 绑定 `IShell` → `ShellLibSu`; `MountDataSource(shell)` 构造参数; 删 `ShellManager`; `getShellResult` 及其调用点改造 | base 模块去壳第一步; shell 调用点少, 单独一轮 |
| 08 | property-binding | 引入 graph factory (`AppGraph.Factory` + `@Provides app` 参数); `AppGraph` 绑定 `IProperty` → `PropertyDefault`; 新 `PropertyReader` 原样封装回退逻辑; 33 处调用点改造 | 调用点多但全是机械替换, 独立成子任务才好 review; factory 是 Context 绑定的地基 |
| 09 | lld-file-store | `LldManager` (object) → `LldFileStore` (`@Inject` 类, `LLD_JSON_NAME` 与路径构造时解析); `LldDataSource` 注入它 (单向依赖, 环消除); `SettingsRepository` 与 `HomeViewModel` 的调用改走仓库 | 静态 object → 注入类; 顺带落实 #02 "ViewModel 不直接碰文件" |
| 10 | shared-preferences-binding | `AppGraph` `@Provides` `SharedPreferences` (复用 ST-08 的 Application 绑定); `HomeViewModel` 构造注入, 替换 `MyApplication.sharedPreferences` 直读 | SSOT 归属是 #08 的活, 这里只把依赖变成注入; 设置页的读取点不动 |
| 11 | docs-closeout | AGENTS.md 现状改写 ("No DI framework" 条目, Factory 模式 → 图模式, 升级纪律); issues-cn #02 改写 (剩余归 #01/#08/#09), #16 改写 (保留 dispatcher 半), README 索引 | 文档收尾集中一次, 避免中途反复改动文档 |

## 有意不做 (不在本任务)

- `myAndroid` 收敛 (#09), `SettingsStore` SSOT (#08/#10), `getMyString` 102 处消亡 (#01), 列表纯推导重构 (#11 原条目), `metrox-android` (需 API 28, 本项目 minSdk 24), `ShellDefault` (按 AGENTS.md 约定保留, 不注册绑定), `JsonExt` 顶层 `json` 单例 (无状态不可变, 迁移零收益).

## 风险与验证

- Gradle 9.7.1 + AGP 9.4.1 组合不在 Metro 官方兼容矩阵内 (矩阵只约束 Kotlin) → ST-01 冒烟即验证; 失败则停下与负责人定方案, 不硬闯.
- `binding<ViewModel>()` 漏写不报编译错 (运行期 `Unknown model class`) → 每轮 review 的固定检查项.
- 插件 / 运行时 / MetroX 必须同版本锁升 (同一 `version.ref`), 升 Kotlin 必须同步升 Metro — ST-01 的 toml 注释记录, ST-11 写进 AGENTS.md.

## 待学习清单

(收尾时清空 — 三项均在任务中落地掌握: 版本锁升纪律已写入 AGENTS.md 的 DI 条目与 toml 注释; `binding<ViewModel>()` 陷阱已在 ST-02 实证并钉进 HomeViewModel 注释; 编译器插件直生 IR 的路径经 ST-01~10 的增量构建观察确认. 横切复盘已完成并经负责人审阅, 按其裁定未入库.)
