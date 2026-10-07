# ST-02 home-viewmodel-chain: 修改计划报告

> 状态: living (预生成; 动工时与负责人预期对比, 必要时显式修订). 计划: [plan.md](plan.md) 子任务 02. 风险: 高 (试点链, 生疏领域, 即 MetroX multibinding 首次落地).

## 要改什么

- `HomeViewModel.kt`: 挂 `@Inject @ViewModelKey @ContributesIntoMap(AppScope::class, binding<ViewModel>())`,  
  删除伴生 `Factory`; `binding<ViewModel>()` 必须显式给出 (Metro 默认绑直接父类型, 本类的直接父类是 `BaseListViewModel`, 绑错 key 运行期才暴露)
- `HomeRepository.kt` / `LldDataSource.kt` / `MountDataSource.kt` / `AppInfoDataSource.kt` (`ui/settings/datasource/`):  
  各挂 `@Inject` (全部具体类型, 零 `@Provides`)
- `AppRoot.kt`: `HomeKey` entry 改 `metroViewModel<HomeViewModel>()` (显式类型: `MyModelListScreen` 收抽象  
  `BaseListViewModel`); 其余三条 entry 不动

## 为什么

试点链: 一次打通 "注解 → multibinding → 工厂 map → entry 取用" 全路径, 验证作用域与工厂解析;  
`AppInfoDataSource` 被 Home/Settings 两条链共享, 在此首次挂 `@Inject`.

## 覆盖核对

- 覆盖: HomeViewModel 链全部对象 (ViewModel / Repository / 3 个 DataSource) + `HomeKey` entry
- 不在本子任务: 其余三条链 (ST-03~05), 静态单例与 HttpClient (ST-06~10),  
  有意不做 (json 单例 / `ShellDefault` / `myAndroid` / `getMyString` → #01)

## 怎么验证

- `./gradlew assembleFossDebug`
- `rg "HomeViewModel.Factory" --glob "*.kt"` 全仓零残留
- 运行时四 tab smoke 统一放 ST-05 (第一段收尾)

## 依赖与前提

- ST-01 已提供 `AppGraph` / `AppViewModelFactory` / `MainActivity` 工厂下发
- 本任务落地后工厂的 `viewModelProviders` map 出现第一个条目
