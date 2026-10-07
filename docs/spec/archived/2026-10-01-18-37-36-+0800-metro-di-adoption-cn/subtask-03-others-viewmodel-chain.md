# ST-03 others-viewmodel-chain: 修改计划报告

> 状态: living (预生成; 动工时与负责人预期对比, 必要时显式修订). 计划: [plan.md](plan.md) 子任务 03. 风险: 低 (机械重复 ST-02 已验证的模式, 仅对象数多).

## 要改什么

- `OthersViewModel.kt`:  
  三注解 (`@Inject @ViewModelKey @ContributesIntoMap(AppScope::class, binding<ViewModel>())`) + 删伴生 `Factory`
- `OthersRepository.kt` + 6 个 DataSource  
  (`BasicDataSource`, `ArchitectureDataSource`, `RomDataSource`, `FingerprintDataSource`(others),  
  `KernelDataSource`, `OthersDataSource`): 各挂 `@Inject`
- `AppRoot.kt`: `OthersKey` entry 改 `metroViewModel<OthersViewModel>()`

## 为什么

与 ST-02 同型; 单链单提交保持 diff 可 review.  
`others.FingerprintDataSource` 与 `settings.FingerprintDataSource` 同名异义 (issues-cn #07), 注解时注意选对包.

## 覆盖核对

- 覆盖: OthersViewModel 链全部对象 (ViewModel / Repository / 6 个 DataSource) + `OthersKey` entry
- 不在本子任务: 其余三条链 (ST-02/04/05), 静态单例与 HttpClient (ST-06~10), 有意不做清单

## 怎么验证

- `./gradlew assembleFossDebug`
- `rg "OthersViewModel.Factory" --glob "*.kt"` 全仓零残留

## 依赖与前提

- ST-02 的试点模式已通过构建验证
