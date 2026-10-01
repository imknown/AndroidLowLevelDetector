# ST-07 shell-binding — 修改计划报告

> 状态: living (预生成; 动工时与负责人预期对比, 必要时显式修订). 计划: [plan.md](plan.md) 子任务 07. 风险: 低 (调用点少; base 模块去壳第一步).

## 要改什么

- `ShellLibSu.kt` (`ui/common/`, object): 挂 `@ContributesBinding(AppScope::class, binding<IShell>())` — object 直接贡献绑定, Metro 自动提供实例; ShellLibSu 在 `:app` 内, 不给 `:base` 添 Metro 依赖
- `MountDataSource.kt`: 构造参数 `private val shell: IShell`, `getShellResult(CMD_MOUNT)` 改 `shell.execute(CMD_MOUNT)`
- 删 `ShellManager.kt` (`:base`) 与 `MyApplication.initShellAndProperty()` 里的 `ShellManager.instance = ...` (libsu 的 `Shell.Builder` 全局配置保留, 它本身就是进程级的)
- 删 `ShellExt.kt` 的顶层 `getShellResult`; 其余调用点改为注入 `IShell` 后 `if (condition) shell.execute(cmd) else ShellResult()` (`condition` 分支语义原样保留): `HomeRepository` 的 SELinux / toybox / GSI-compat (带 `condition` 实参), `PropertiesDataSource.getBuildProp` 的 getprop, `KernelDataSource` 的两条 kernel 命令 — 后两者的 `IShell` 经 `PropRepository` / `OthersRepository` 构造传入 (`OthersRepository` 自身零 shell 调用点, binder 查询走 JNI)

## 为什么

issues-cn #02 配套动作: 依赖从 "顶层函数里摸 `ShellManager.instance`" 变成构造函数上的显式参数; Manager 壳零附加行为, 随静态槽一起删.

## 覆盖核对

- 覆盖: `IShell` 绑定 + 全部 shell 消费点 (`ShellManager` / `getShellResult` / `HomeRepository` 3 处 / `MountDataSource` / `PropertiesDataSource.getBuildProp` / `KernelDataSource` 2 条)
- 不在本子任务: `ShellDefault` (按 AGENTS.md 保留), `PropertyManager` / `PropertyExt` (ST-08), 其余静态单例 (ST-08~10)

## 怎么验证

- `./gradlew assembleFossDebug`
- `rg "ShellManager|getShellResult" --glob "*.kt"` 全仓零残留 (`ShellDefault` 按 AGENTS.md 保留, 不注册绑定)

## 依赖与前提

- ST-05 完成 (图已可承载业务绑定)
- 注意 `ShellDefault` (`:base`) 是备选实现, **不动**
