# ST-08 property-binding — 修改计划报告

> 状态: living (预生成; 动工时与负责人预期对比, 必要时显式修订). 计划: [plan.md](plan.md) 子任务 08. 风险: 低 (改动量大但全是机械替换; 引入 graph factory 是唯一结构性变化).
> 修订 (2026-10-02): "经 `OthersRepository` 构造传入" 已过时 — 同 ST-07 报告的同型修订, ST-03/04 落地后全部 DataSource 均为 @Inject 图构造, `PropertyReader` 由图直接解析进各自构造函数, 无需经 Repository 传参 (`OthersRepository` 自身零 property 调用点).
> 修订 (2026-10-02, 实施时): 原方案的 "AppGraph companion @Provides" 在 Metro 1.4.5 + Kotlin 2.4.20 上无法编译 — 图声明 `@DependencyGraph.Factory` 后, 插件的 supertype 生成器无条件给已存在的 companion 挂 `: Factory` 父类型, 但 SAM 实现生成环节不生效 (`ABSTRACT_MEMBER_NOT_IMPLEMENTED`); `fun create` / `operator fun invoke` / 源码显式 `companion object : AppGraph.Factory` 三种形状均复现, 与函数名无关. 改走 Metro 主干路径: 图不声明 companion (Metro 自行生成并实现工厂接口), 两个 `@Provides` 挪入 `@BindingContainer` object (`HttpClientContainer` / `PropertyContainer`), 经 `@DependencyGraph(bindingContainers = [...])` 挂入; 工厂用普通 interface (非 `fun interface`), 调用点 `createGraphFactory<AppGraph.Factory>().create(this)` 与原方案一致. 该限制升级 Metro 后值得复核 (AppGraph.kt 注释有记). 另: `initShellAndProperty` 随 property 半边摘除更名为 `initShell`.

## 要改什么

- **引入 graph factory** (Context 绑定的地基): `AppGraph` 增加
  ```kotlin
  @DependencyGraph.Factory
  fun interface Factory { fun create(@Provides app: MyApplication): AppGraph }
  ```
  `MyApplication.appGraph` 一行改 `createGraphFactory<AppGraph.Factory>().create(this)`; 后续 `SharedPreferences` (ST-10) 复用此绑定
- `AppGraph` companion `@Provides fun property(): IProperty = PropertyDefault` — `PropertyDefault` 在 `:base`, 不给它挂注解; 反射读 `SystemProperties` 不需要 Application, 不挂多余的依赖边
- 新建 `PropertyReader` (`@Inject`, 参数 `IProperty` + `MyApplication`): 原样封装现 `PropertyExt.kt` 的回退逻辑 (`getStringProperty` / `getBooleanProperty`, 含 `result_not_supported` / `build_not_filled` 两个字符串), 行为逐字节等价
- 三个 DataSource 的直读点改为构造注入 `propertyReader`: `BasicDataSource.kt` (1 处), `ArchitectureDataSource.kt` (1 处), others 的 `FingerprintDataSource.kt` (2 处) — 经 `OthersRepository` 构造传入
- 删 `PropertyManager.kt` (`:base`) / `PropertyExt.kt` / `MyApplication` 里的 instance 赋值
- 调用点共 33 处 (HomeRepository 29 + 上面的 4 处): `getStringProperty(` → `propertyReader.getString(`, `getBooleanProperty(` → `propertyReader.getBoolean(`; `HomeRepository` 构造加 `propertyReader` 参数

## 为什么

issues-cn #02 配套动作; 回退逻辑集中一处, 依赖显式化. 用 `PropertyReader` 而不是让每个 Repository 持 `IProperty` + 自己拼回退, 是为了避免同一段回退逻辑复制 33 处.

## 覆盖核对

- 覆盖: graph factory 引入; `IProperty` 绑定; `PropertyReader` 新建; 33 处 property 调用点 (HomeRepository 29 + 三个 DataSource 4); `PropertyManager` / `PropertyExt` 删除; 两个 binding container (`HttpClientContainer` / `PropertyContainer`, 容纳原方案 companion 的 `@Provides`; factory 实施路径见顶部修订)
- 不在本子任务: `getMyString` 其余调用点 (归 #01), `SettingsStore` (归 #08), SharedPreferences 绑定 (ST-10)

## 怎么验证

- `./gradlew assembleFossDebug`
- `rg "PropertyManager|getStringProperty\(|getBooleanProperty\(" --glob "*.kt"` 全仓零残留
- 行为等价抽查: 任选 3 处调用点对照改前改后的返回值分支 (condition=false / 异常 / 正常)

## 依赖与前提

- ST-07 完成 (同批去壳, 先 Shell 后 Property 减小单轮 diff)
- `getMyString` 的其余调用点不动 (归 #01)
