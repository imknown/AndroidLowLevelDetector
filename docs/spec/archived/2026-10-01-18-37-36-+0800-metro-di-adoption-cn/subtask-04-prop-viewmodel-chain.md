# ST-04 prop-viewmodel-chain: 修改计划报告

> 状态: living (预生成; 动工时与负责人预期对比, 必要时显式修订). 计划: [plan.md](plan.md) 子任务 04. 风险: 低 (机械重复, 链最短).

## 要改什么

- `PropViewModel.kt`: 三注解 + 删伴生 `Factory`
- `PropRepository.kt` / `PropertiesDataSource.kt` / `SettingsDataSource.kt` (`ui/prop/datasource/`): 各挂 `@Inject`
- `AppRoot.kt`: `PropKey` entry 改 `metroViewModel<PropViewModel>()`

## 为什么

与 ST-02 同型; Prop 链只有 2 个 DataSource, 是最短的一条.

## 覆盖核对

- 覆盖: PropViewModel 链全部对象 (ViewModel / Repository / 2 个 DataSource) + `PropKey` entry
- 不在本子任务: 其余三条链 (ST-02/03/05), 静态单例与 HttpClient (ST-06~10), 有意不做清单

## 怎么验证

- `./gradlew assembleFossDebug`
- `rg "PropViewModel.Factory" --glob "*.kt"` 全仓零残留
- `PropRepository` 在 `ui/prop/repository/`, 两个 DataSource 在 `ui/prop/datasource/`

## 依赖与前提

- ST-02 的试点模式已通过构建验证
