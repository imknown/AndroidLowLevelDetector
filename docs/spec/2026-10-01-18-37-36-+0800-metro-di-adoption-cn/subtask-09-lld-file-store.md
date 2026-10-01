# ST-09 lld-file-store — 修改计划报告

> 状态: living (预生成; 动工时与负责人预期对比, 必要时显式修订). 计划: [plan.md](plan.md) 子任务 09. 风险: 高 (核心文件编排搬移 + 消除 LldManager ↔ LldDataSource 引用环).

## 要改什么

- `LldManager.kt` (object) → 新 `LldFileStore.kt` (`@Inject` 类): `LLD_JSON_NAME` 常量与 `savedLldJsonFileOrThrow` 的文件路径一并移入, 构造时解析 (经 ST-08 的 Application 绑定取 `getDownloadDir()`); 其余方法 (`copyJsonIfNeededOrThrow` / `saveLldJsonFileOrThrow` / `getAssetLld` / `getAssetLldVersion`) 原样搬入, 内部 `MyApplication.instance.assets` / `MyApplication.getMyString` 现状保留 (后者归 #01)
- `LldDataSource.kt`: 构造注入 `LldFileStore`, `fetchOfflineLldFileOrThrow()` 委托它; 远程 URL 里的文件名改引用 `LldFileStore.LLD_JSON_NAME` — 依赖只剩 LldDataSource → LldFileStore 一个方向, 环真正消除
- `HomeRepository.kt`: 注入 `LldFileStore`, 新增三个委托方法 (保存 / 按需复制 / 读内置资产)
- `SettingsRepository.kt`: 注入 `LldFileStore`, `getAssetLldVersion(MyApplication.instance.assets)` 委托它 (内置数据版本号读取)
- `HomeViewModel.kt`: 三处直接调 `LldManager` 的点 (`tryDetectOnline` 的保存, `fetchOfflineLldOrNull` 的按需复制, `getAssetLldOrNull`) 改走 `homeRepository` — ViewModel 不再直接碰文件 (issues-cn #02)

## 为什么

静态 object 单例换注入类是第二段的本体; 顺带落实 #02 的 "持久化编排归仓库, ViewModel 只做编排结果的使用方" — 深层编排重构仍归 #11, 本任务只搬不改语义.

## 覆盖核对

- 覆盖: `LldManager` 全部 4 个消费方 (`LldDataSource` / `HomeRepository` / `SettingsRepository` / `HomeViewModel`) 与环消除; 文件编排入口归仓库
- 不在本子任务: 编排的深层重构 (归 #11), `LldFileStore` 内部 `getMyString` 残留 (归 #01)

## 怎么验证

- `./gradlew assembleFossDebug`
- `rg "LldManager" --glob "*.kt"` 全仓零残留
- 运行时 smoke (设备): 首次冷启动资产 lld.json 复制到外部目录; 断网启动走离线链; 允许联网时在线保存成功 — 三条路径覆盖 `copyJsonIfNeededOrThrow` / `saveLldJsonFileOrThrow` / `getAssetLld` 全部分支

## 依赖与前提

- ST-08 的 Application 绑定 (graph factory)
