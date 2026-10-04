# ST-05 settings-viewmodel-chain — 修改计划报告

> 状态: living (预生成; 动工时与负责人预期对比, 必要时显式修订). 计划: [plan.md](plan.md) 子任务 05. 风险: 低 (同型机械改造 + 文档收尾; 含第一段的整体运行时验证).

## 要改什么

- `SettingsViewModel.kt`: 三注解 + 删伴生 `Factory`
- `SettingsRepository.kt` / `FingerprintDataSource.kt` (`ui/settings/datasource/`): 挂 `@Inject` (`AppInfoDataSource` 已在 ST-02 挂过)
- `AppRoot.kt`: `SettingsKey` entry 改 `metroViewModel<SettingsViewModel>()`; 移除 androidx `viewModel` 的残留 import
- 按 tracker 规则删除 issues-cn `05-组合根分散.md` 并同步 README 索引/总览表 (第一段完成, #05 关闭)
- 修剪指向 #05 的存活引用, 防止删除后悬空: `02-服务定位器上帝对象.md` 的两处与 README 修复路线的一句, 改为 "随 DI 接入落地" 的叙述, 不留死链

## 为什么

第一段最后一条链; 四条 entry 全部走 MetroX 后, "同一份组装知识写四遍" 的问题消失, #05 的关闭条件成立.

## 覆盖核对

- 覆盖: SettingsViewModel 链全部对象 + `SettingsKey` entry; `AppRoot` 的 androidx `viewModel` 收尾; issues-cn #05 条目关闭与引用修剪 — 第一段在此完整覆盖
- 不在本子任务: 静态单例与 HttpClient (ST-06~10, 第二段), 有意不做清单

## 怎么验证

- `./gradlew assembleFossDebug`
- `rg "viewModel<|\.Factory" app/src/main/java/net/imknown/android/forefrontinfo/ui/AppRoot.kt` 零残留
- `rg "#05|05-组合根分散" docs/dev/issues-cn` 零残留 (引用修剪干净)
- **运行时 smoke (需要设备/模拟器, 与负责人协调)**: 四个 tab 冷启动/切换/下拉刷新正常, 每个 tab 的 ViewModel 为 entry 作用域
