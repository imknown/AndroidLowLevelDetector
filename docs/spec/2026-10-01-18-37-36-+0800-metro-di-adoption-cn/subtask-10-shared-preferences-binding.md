# ST-10 shared-preferences-binding — 修改计划报告

> 状态: living (预生成; 动工时与负责人预期对比, 必要时显式修订). 计划: [plan.md](plan.md) 子任务 10. 风险: 低 (单一绑定 + 单个消费类).

## 要改什么

- `AppGraph.kt`: companion `@Provides fun sharedPreferences(app: MyApplication): SharedPreferences = app.sharedPreferences` (复用 ST-08 的 Application 绑定)
- `HomeViewModel.kt`: 构造参数 `private val sharedPreferences: SharedPreferences`; 三处 `MyApplication.sharedPreferences` 改为注入实例 — `collectModels()` 的联网开关读取, `init` 的 `registerOnSharedPreferenceChangeListener`, `onCleared` 的 `unregisterOnSharedPreferenceChangeListener` (listener 内的 key 比较用的是 `getMyString`, 不动, 归 #01)

## 为什么

把 HomeViewModel 对全局静态的依赖变成构造签名上的显式依赖; SSOT 的归属重构 (设置可观察唯一来源) 是 #08 的活, 本任务只做 DI 化, 不改数据流设计.

## 覆盖核对

- 覆盖: `HomeViewModel` 的全部 3 处 `MyApplication.sharedPreferences` 直读 (collectModels 读取 / init 注册 / onCleared 注销)
- 不在本子任务: `SettingsScreen` 直读 (归 issues-cn #10 原条目), `HomeRepository:1106` (归 #08), `MyApplication` 自身, `getMyString` (归 #01)

## 怎么验证

- `./gradlew assembleFossDebug`
- `rg "MyApplication.sharedPreferences" app/src/main/java` — HomeViewModel 内零残留; 其余残留显式不动: `MyApplication` 自身, `SettingsScreen` (归 #10 原条目), `HomeRepository:1106` 的 outdated-order 读取 (归 #08 的偷读点之一)
- 运行时 smoke (设备): 切换 "允许联网" 开关后 Home 页下次加载走对应链路

## 依赖与前提

- ST-08 的 graph factory
