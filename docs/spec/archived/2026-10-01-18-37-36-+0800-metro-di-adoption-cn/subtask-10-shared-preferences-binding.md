# ST-10 shared-preferences-binding — 修改计划报告

> 状态: living (预生成; 动工时与负责人预期对比, 必要时显式修订). 计划: [plan.md](plan.md) 子任务 10. 风险: 低 (单一绑定 + 单个消费类).
> 修订 (2026-10-02, 动工时): 原文 "AppGraph companion `@Provides`" 已过时 — ST-08 确立的 Metro 1.4.5 限制: 图声明 factory 后不能自带 companion, `@Provides` 入 binding container (同 HttpClient/Property 二容器先例), 新增 `SharedPreferencesContainer` 并注册进 `bindingContainers`. 又: `sharedPreferences` 是 companion val (Kotlin 禁止经实例引用调 companion 成员, 同 ST-09 的 `getDownloadDir` 教训), 但改实例成员会波及显式不动的外部静态调用点 (SettingsScreen / HomeRepository, 归 #10/#08) — 故 provider 形态为 `fun sharedPreferences(app: MyApplication): SharedPreferences = MyApplication.sharedPreferences`: 参数声明对 Application 的依赖边 (复用 ST-08 图工厂绑定), 函数体读 companion val 现状保留.

## 要改什么

- `AppGraph.kt`: companion `@Provides fun sharedPreferences(app: MyApplication): SharedPreferences = app.sharedPreferences` (复用 ST-08 的 Application 绑定)
- `HomeViewModel.kt`: 构造参数 `private val sharedPreferences: SharedPreferences`; 三处 `MyApplication.sharedPreferences` 改为注入实例 — `collectModels()` 的联网开关读取, `init` 的 `registerOnSharedPreferenceChangeListener`, `onCleared` 的 `unregisterOnSharedPreferenceChangeListener` (listener 内的 key 比较用的是 `getMyString`, 不动, 归 #01)

## 为什么

把 HomeViewModel 对全局静态的依赖变成构造签名上的显式依赖; SSOT 的归属重构 (设置可观察唯一来源) 是 #08 的活, 本任务只做 DI 化, 不改数据流设计.

## 覆盖核对

- 覆盖: `HomeViewModel` 的全部 3 处 `MyApplication.sharedPreferences` 直读 (collectModels 读取 / init 注册 / onCleared 注销)
- 不在本子任务: `SettingsScreen` 直读 (归 issues-cn #10 原条目), `HomeRepository:1121` (归 #08), `MyApplication` 自身, `getMyString` (归 #01)

## 怎么验证

- `./gradlew assembleFossDebug`
- `rg "MyApplication.sharedPreferences" app/src/main/java` — HomeViewModel 内零残留; 其余残留显式不动: `MyApplication` 自身, `SettingsScreen` (归 #10 原条目), `HomeRepository:1121` 的 outdated-order 读取 (归 #08 的偷读点之一)
- 运行时 smoke (设备): 切换 "允许联网" 开关后 Home 页下次加载走对应链路

## 依赖与前提

- ST-08 的 graph factory
