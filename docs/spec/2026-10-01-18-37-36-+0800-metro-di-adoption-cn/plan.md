# Metro + MetroX DI 接入 — 修改计划

> 状态: living. 决策依据: `docs/dev/issues-cn` 的 #02 / #05 修订版 (选型表与两段式划分) 与此前的可行性分析 (版本兼容矩阵 / 构建性能 / 风险已逐项核实). 本页是任务级拆分与执行约定; 每个子任务动工前各有自己的修改计划报告 (`subtask-NN-*.md`) 落在本目录.

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
- 修改计划报告: 每个子任务动工前 (包括第一个), 负责人先写一两句自己的预期; AI 生成 `subtask-NN-<slug>.md` (要改什么, 为什么, 怎么验证) 供对比; 负责人说开始才动工. 不再另写完成后的总结报告.
- 每个子任务: 最小高内聚 diff, 单独可编译; 注释解释**为什么**这么改 (对齐代码库注释密度); 构建验证 `./gradlew assembleFossDebug`.
- 流程: 修改计划报告 → 实施 → 新上下文 subagent 后台 review (v1) → 有发现: 报告 → 负责人复核确认后只修本轮要求的内容 (改了才进入 v2 后台 review, 确认无需修改则直接走等点头提交那一步) → v2, v3 同理, 直到某一轮无新的实质发现 → 负责人放行留一句自己的话 → 提交 → 再等点头进入下一个子任务.
- 收敛护栏: 主观风格偏好与过早优化不构成新一轮; 连续几轮无新发现仍在打转时交由负责人收束, 不无限 review.
- 风险标注: 高 = ST-01, ST-02, ST-09; 生疏领域 = ST-01 (Metro 接入), ST-02 (MetroX multibinding 首用), ST-06 (首个 @Provides); 其余为低风险 (机械/样板). 低风险子任务的 review 与提交闸门可合并, 也可由负责人亲自读 diff 代替.
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
| 08 | property-binding | `AppGraph` 绑定 `IProperty` → `PropertyDefault`; `PropertyReader` (新, 原样封装 `getStringProperty` / `getBooleanProperty` 的回退逻辑); `PropertiesDataSource` 与约 90 个调用点改造 | 调用点量大但全是机械替换, 独立成子任务才好 review |
| 09 | lld-file-store | `LldManager` (object) → `LldFileStore` (`@Inject` 类, 路径构造时解析); `LldDataSource` 注入它 (环消除); `HomeViewModel` 的文件编排改走 `HomeRepository` | 静态 object → 注入类; 顺带落实 #02 "ViewModel 不直接碰文件" |
| 10 | shared-preferences-binding | `AppGraph` `@Provides` `SharedPreferences`; `HomeViewModel` 构造注入, 替换 `MyApplication.sharedPreferences` 直读 | SSOT 归属是 #08 的活, 这里只把依赖变成注入; 设置页的读取点不动 |
| 11 | docs-closeout | AGENTS.md 现状改写 ("No DI framework" 条目, Factory 模式 → 图模式, 升级纪律); issues-cn #02 改写 (剩余归 #01/#08/#09), #16 改写 (保留 dispatcher 半), README 索引 | 文档收尾集中一次, 避免中途反复改动文档 |

## 有意不做 (不在本任务)

- `myAndroid` 收敛 (#09), `SettingsStore` SSOT (#08/#10), `getMyString` 102 处消亡 (#01), 列表纯推导重构 (#11 原条目), `metrox-android` (需 API 28, 本项目 minSdk 24), `ShellDefault` (按 AGENTS.md 约定保留, 不注册绑定), `JsonExt` 顶层 `json` 单例 (无状态不可变, 迁移零收益).

## 风险与验证

- Gradle 9.7.1 + AGP 9.4.1 组合不在 Metro 官方兼容矩阵内 (矩阵只约束 Kotlin) → ST-01 冒烟即验证; 失败则停下与负责人定方案, 不硬闯.
- `binding<ViewModel>()` 漏写不报编译错 (运行期 `Unknown model class`) → 每轮 review 的固定检查项.
- 插件 / 运行时 / MetroX 必须同版本锁升 (同一 `version.ref`), 升 Kotlin 必须同步升 Metro — ST-01 的 toml 注释记录, ST-11 写进 AGENTS.md.

## 待学习清单

- Metro 插件 / 运行时 / MetroX 与 Kotlin 版本的锁升级关系 (兼容矩阵怎么读)
- multibinding map key 绑定直接父类型的陷阱 (`binding<ViewModel>()` 为什么必须显式给出)
- 编译器插件直接生成 IR 与 KSP 生成源码两种代码生成路线的差异 (对增量构建意味着什么)
