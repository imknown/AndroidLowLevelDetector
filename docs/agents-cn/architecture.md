# 架构细则

> **状态**: **living**, 是 [AGENTS-cn.md](../../AGENTS-cn.md) 的架构一节的一部分.  
> 改动导航, DI 图, 一个 ViewModel 或状态类, 列表页,  
> 设置持久化, 或 shell/属性访问之前, 先读本文.  
> 一句话的决策留在 AGENTS-cn.md 里; 本文承载细节和理由.

## 导航

导航用的是 **Navigation 3** (`androidx.navigation3`), 不是主流的 Navigation 2:  
API 的形状是 `NavKey` + 返回栈 + entryProvider (`ui/navigation/NavKeys.kt`).  
别用 Nav2 的思路去想 (`NavHost(route = ...)`).

## 依赖注入

DI 由 **Metro** 承担 (编译期, 无反射), 这是对 Hilt / Koin 主流的有意偏离  
(手写 companion `Factory` / `viewModel(factory = ...)` 的时代已退役). ViewModel 的写法是 `@Inject` +  
`@ViewModelKey` + `@ContributesIntoMap(AppScope::class, binding<ViewModel>())`,  
并在 Navigation 3 的入口经 `metroViewModel<...>()` 解析; Repository 和 DataSource 就是普通的  
`@Inject` 构造注入; 叶子绑定放在 `di/AppGraph.kt` 的绑定容器里,  
图工厂绑定 `MyApplication`. Gradle 插件, 运行时和 MetroX 构件都版本锁定到  
`gradle/toml/thirdParty.toml` 里的同一个 `version.ref`; Kotlin 升级要求配套的 Metro 升级,  
所以先查官方兼容性矩阵.

延迟注入的位置用函数类型 `() -> T`, 而不是 Metro 的 `Provider<T>`:  
Metro 默认就把 `() -> T` 当 provider, 所以声明出来的 `Provider<T>` 正是那种它会报告为  
`DESUGARED_PROVIDER_WARNING` 的不受鼓励的语法糖 (这条检查读的是声明类型, 不是值实际怎么被调用).

## 设置归属

设置归 `SettingsStore` (`ui/settings/repository/SettingsStore.kt`) 所有:  
它是每一项设置的键/值与全部写入口的唯一可观察持有者,  
在图里绑定并注入给消费者, 所以它之外没有任何东西直接碰 SharedPreferences  
(图里的那个 SharedPreferences 绑定就是为了喂给它而存在的).

## 接口优先的可测试性

可测试性来自**接口优先的设计**, 不是靠模拟框架:  
`:base` 定义 `IProperty` / `IShell` 并带默认实现 (`PropertyDefault` / `ShellDefault`),  
而且没有聚合类: 图把两者都绑定  
(`ShellLibSu` 通过 `@ContributesBinding` 贡献 `IShell`,  
`PropertyDefault` 由 `PropertyContainer` 以 `@Provides` 绑定),  
而 `PropertyReader` (`ui/common`) 给 `IProperty` 包上共用的占位值回退.

## BaseListViewModel

`BaseListViewModel` 用两个 `StateFlow` 驱动每一个列表页:  
`modelsStateFlow: StateFlow<PersistentList<MyModel>?>`  
(null = 冷启动; 刷新时刻意保留上一次的列表, 这样 UI 永远不会闪现空白) 和  
`isLoadingStateFlow`, 外加 `loadJob` 去重: 别在重建时重新引入多余的加载.  
那个 `PersistentList` 只在那个边界上由 `setModels()` 构建一次,  
并由 `updateModelDetail()` 用 `PersistentList.replacingAt()` 修补它 (`set()` 已废弃).  
composable 从不转换这个集合:  
在 composable 正文里的转换会在该作用域的每次重组时重跑 `O(n)`.  
`onModelsLoaded()` 在每次加载落地之后运行, 用来校正构建中途发生变化的状态.

## Compose 稳定性

Compose 的稳定性注解 (`@Immutable` / `@Stable`) 是有意为之;  
状态类一有改动就重新评估它们  
(照着 `HomeViewModel` / `BaseListViewModel` 顶部注释里的示范做,  
那条注释解释了*为什么*这个注解是安全的).

## Lld.json 数据流

随应用打包的 `lld.json` 数据会复制到外部文件目录 (`LldFileStore`),  
并在用户允许联网时经 Ktor 在线刷新; GitHub 还是 Gitee 的 URL 按时区选择.

## Shell 执行

命令执行用 libsu 的 **non-root** 模式  
(`ui/common/ShellLibSu.kt`, 带 `Shell.FLAG_NON_ROOT_SHELL`): 不存在 root 层.
