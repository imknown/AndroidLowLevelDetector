# 架构

> 返回 [README](README.md)。分层、依赖方向、对象创建、领域模型与检测引擎这类「骨架」问题。

<a id="AR-01"></a>

## AR-01 数据层直接产出「展示内容」，全项目没有领域模型

**严重程度：P0 ｜ 修复难度：难（可渐进）**
**影响文件：`HomeRepository.kt`（约 1200 行）、`OthersRepository.kt`、`PropRepository.kt`、`SettingsRepository.kt`、`MyModel.kt`、`MyModelExt.kt`、`MyAdapter.kt`**

### 问题核心代码

`HomeRepository.detectPerformanceClass()`（`app/src/main/java/net/imknown/android/forefrontinfo/ui/home/repository/HomeRepository.kt:294`）——一个「数据层」方法，干的却是界面的活：

```kotlin
fun detectPerformanceClass(): MyModel {
    @AttrRes var performanceColorRes = R.attr.colorCritical        // ← 界面颜色（主题属性）
    val result = if (isAtLeastAndroid12()) {
        val performanceClass = Build.VERSION.MEDIA_PERFORMANCE_CLASS
        if (performanceClass == Build.VERSION.SDK_INT) {
            performanceColorRes = R.attr.colorNoProblem            // ← 界面规则
        } else if (performanceClass == Build.VERSION.SDK_INT - 1) {
            performanceColorRes = R.attr.colorWarning
        }
        if (performanceClass != 0) {
            MyApplication.getMyString(R.string.performance_class_detail_api, performanceClass)  // ← 界面文案
        } else {
            MyApplication.getMyString(R.string.result_not_supported)
        }
    } else {
        MyApplication.getMyString(R.string.result_not_supported)
    }
    return toColoredMyModel(R.string.performance_class_title, result, performanceColorRes)
}
```

同样的模式遍布 `HomeRepository` 的 20 多个 `detect*` 方法和另外三个仓库。`MyModel` 这个界面模型成了所有层之间传的东西：

```kotlin
// app/src/main/java/.../ui/base/list/MyModel.kt:18
data class MyModel(
    val title: MyModelTitle,          // 标题（资源 ID 或字符串）
    val detail: String,               // 已拼好、已本地化的展示字符串
    @param:AttrRes val color: Int = RES_ID_NONE,   // 主题颜色属性 ID
    val type: MyModelType = General
)
```

### 直接原因

仓库方法把三件事揉在一个函数里：**取数**（读 `Build.VERSION.MEDIA_PERFORMANCE_CLASS`）、**判断**（和 `SDK_INT` 比较）、**展示**（选文案、选颜色）。判断结果没有以数据形式返回，而是当场翻译成了给用户看的字符串。`MyApplication.getMyString` 在 app 模块被调用 **100 处**，绝大多数在仓库层。

### 根本原因

项目从第一行代码起就没有定义过「领域模型」（domain model，即只描述事实、不关心怎么显示的数据结构，例如「媒体性能等级 = 33，落后正式版 1 级」），`MyModel` 这个列表条目界面模型被直接下推到了最底层。一旦界面模型是通用货币：

- **不可测试**：检测逻辑离不开 `MyApplication.instance`（Context）、`R.string`、`R.attr`，只能跑在真机上，`app/src/test` 里至今只有 `ExampleUnitTest` 不是巧合。
- **不可复用**：同一份检测数据想换个展示方式（比如排序、导出、深色下换色），必须重写取数逻辑。
- **反向依赖**：数据层反过来依赖 UI 资源（`R`），违反了「依赖只能从外层指向内层」的分层规则，也是 [AR-07](architecture.md#AR-07) 包循环的一半成因。

### 修复方案

引入「领域模型 + 映射器」两步走：仓库只返回纯数据；把数据翻译成 `MyModel` 的映射函数放在 UI 包，且只放在那里。以 `detectPerformanceClass` 为例：

```kotlin
// ── domain 层：纯 Kotlin，无任何 Android 依赖，可直接单测 ──
// ui/home/model/PerformanceClass.kt
sealed interface PerformanceClassInfo {
    data class Met(val api: Int) : PerformanceClassInfo          // 达标
    data class Lagging(val api: Int) : PerformanceClassInfo      // 落后一代
    data object NotDeclared : PerformanceClassInfo               // 设备未声明
    data object NotSupported : PerformanceClassInfo              // 系统低于 12
}

// ui/home/repository/HomeRepository.kt —— 只剩取数 + 判断
fun detectPerformanceClass(): PerformanceClassInfo = when {
    !isAtLeastAndroid12() -> PerformanceClassInfo.NotSupported
    Build.VERSION.MEDIA_PERFORMANCE_CLASS == 0 -> PerformanceClassInfo.NotDeclared
    Build.VERSION.MEDIA_PERFORMANCE_CLASS == Build.VERSION.SDK_INT ->
        PerformanceClassInfo.Met(Build.VERSION.MEDIA_PERFORMANCE_CLASS)
    else -> PerformanceClassInfo.Lagging(Build.VERSION.MEDIA_PERFORMANCE_CLASS)
}
```

```kotlin
// ── ui 层映射器：全项目唯一知道「这个数据长什么样」的地方 ──
// ui/home/mapper/HomeModelMapper.kt
fun PerformanceClassInfo.toMyModel(context: Context): MyModel {
    val detail = when (this) {
        is Met, is Lagging ->
            context.getString(R.string.performance_class_detail_api, api)
        NotDeclared, NotSupported ->
            context.getString(R.string.result_not_supported)
    }
    val color = when (this) {
        is Met -> R.attr.colorNoProblem
        is Lagging -> R.attr.colorWarning
        else -> R.attr.colorCritical
    }
    return MyModel(
        title = MyModelTitle.Res(R.string.performance_class_title),
        detail = detail,
        color = color
    )
}
```

```kotlin
// ── HomeViewModel.detect() 内的调用点改为 ──
tempModels += homeRepository.detectPerformanceClass().toMyModel(context)
```

改造后可以写出这样的纯单元测试（现在写不出来）：

```kotlin
@Test
fun `落后一代时应返回 Lagging`() {
    // Shadows.shadowOf(Build.VERSION) 或抽象掉 Build 读取后
    assertEquals(PerformanceClassInfo.Lagging(35), repository.detectPerformanceClass())
}
```

**渐进路线**（不需要一次改完 20 个方法）：

1. 新增的检测条目一律走「领域模型 + mapper」。
2. 存量方法在每次因功能需求被触碰时顺手迁移，一次一个。
3. `MyModel`、`MyAdapter` 不用动——它们本来就是 UI 层的东西，动的只是「谁生产 MyModel」。
4. 迁移过半后，`toColoredMyModel` / `toTranslatedDetailMyModel`（`MyModelExt.kt`）里依赖 `MyApplication` 的兜底逻辑一并移入 mapper。

**边界说明**：本项目是只读检测工具，领域逻辑确实较薄，不必追求完整的 domain 模块。底线是把「判断」从「格式化」里剥出来——判断结果可测试、可断言，是防止以后越写越乱的最低要求。

---

<a id="AR-04"></a>

## AR-04 服务定位器满天飞，MyApplication 是上帝对象

**严重程度：P0 ｜ 修复难度：中（与 AR-05 一起做最省）**
**影响文件：`MyApplication.kt`、`PropertyManager.kt`、`ShellManager.kt`、`LldManager.kt`、`PropertyExt.kt`、`ShellExt.kt` 及所有调用方**

### 问题核心代码

`MyApplication` 伴生对象身兼七职（`MyApplication.kt:22`）：全局实例、SharedPreferences 持有者、字符串工具、下载目录工具、主题逻辑、Shell/Property 装配……全项目靠它伸手取物：

```kotlin
companion object {
    lateinit var instance: MyApplication                    // ① 全局实例
    val sharedPreferences: SharedPreferences by lazy { … } // ② 全局偏好
    fun getMyString(resId: Int) = instance.getString(resId) // ③ 字符串工具（100 处调用）
    fun getDownloadDir() = …                                 // ④ 目录工具
    fun setMyTheme(themesValue: String?) { … }               // ⑤ 主题逻辑
}
```

base 模块的「Manager」不管理任何东西，只是给全局单例披了层皮（`PropertyManager.kt:3`）：

```kotlin
class PropertyManager(property: IProperty) : IProperty by property {  // 零附加行为
    companion object {
        lateinit var instance: PropertyManager                        // 存在意义只有这个静态槽
    }
}
```

顶层函数把全局依赖藏进签名里（`ShellExt.kt:6`、`PropertyExt.kt:9`），调用方完全看不出有隐藏依赖：

```kotlin
fun getShellResult(cmd: String, condition: Boolean = true) = if (condition) {
    ShellManager.instance.execute(cmd)      // ← 看不见的依赖：谁执行？线程？可替换吗？
} else { ShellResult() }
```

此外：`LldManager`（object 单例，文件 IO，被 `HomeViewModel`、`LldDataSource`、`SettingsRepository` 三处直接引用）与 `LldDataSource` 互相引用成环——Manager 用 `LldDataSource.LLD_JSON_NAME` 定位文件，DataSource 的 `fetchOfflineLldFileOrThrow()` 又返回 Manager 的文件属性；`HomeViewModel` 还越过仓库直接调 `LldManager` 做文件编排（`HomeViewModel.kt:78/97/101`），ViewModel 干了数据源的活；`ShellDefault` 是死代码（定义后从未被使用）。

### 直接原因

需要 Context、需要跨层取物的代码都从 `MyApplication.instance` / `XxxManager.instance` 这个「全局服务台」现拿现用，构造函数注入只覆盖了 DataSource 一层（`HomeRepository` 注入了 3 个 DataSource，但同文件里仍直接用 `MyApplication.instance.packageManager`、`MyApplication.sharedPreferences`、`LldManager`——注入了假的，真的走全局）。

### 根本原因

服务定位器（service locator）反模式：依赖不体现在构造函数上，而是运行时全局抓取。后果：

- **依赖关系隐形**：看 `HomeRepository` 的构造函数以为它只依赖 3 个 DataSource，实际上它还依赖 `MyApplication`、`PropertyManager`、`ShellManager`、`LldManager`——换实现、写测试、理清所有权都无从下手。
- **上帝对象**（god object）：`MyApplication` 承担了本该属于「组装处」和「工具层」的职责，任何新需求都倾向往里加静态方法，只能长不能缩。

### 修复方案

建一个组合根（composition root：全项目唯一一处 new 对象、接线的工厂），把 6 个全局单例归拢成 1 个容器。不需要引入 DI 框架，纯手工即可：

```kotlin
// core/AppContainer.kt —— 全项目唯一知道「谁用谁」的地方
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val shell: IShell = ShellLibSu                       // 替换 base 模块的 Manager 单例
    val property: IProperty = PropertyDefault
    val settingsStore = SettingsStore(appContext)        // AR-02 的产物
    val lldFileStore = LldFileStore(appContext)          // 由 LldManager 改造，见下
    val appInfo = AppInfoDataSource()

    val homeRepository = HomeRepository(
        LldDataSource(lldFileStore), MountDataSource(shell), appInfo)
    val othersRepository = OthersRepository(BasicDataSource(), ArchitectureDataSource(property), …)
    val propRepository = PropRepository(PropertiesDataSource(shell), SettingsDataSource())
    val settingsRepository = SettingsRepository(appInfo, FingerprintDataSource())  // 现有的关于页仓库
}
```

```kotlin
// MyApplication —— 从「万能服务台」降级为「容器持有者」
class MyApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // 主题初始化改走 container.settingsStore（见 AR-02）；libsu 的 Shell 配置保留在此（它本身就是全局进程级的）
    }
}
```

配套动作：

- `PropertyExt` / `ShellExt` 顶层函数改为 DataSource 的构造依赖：`class MountDataSource(private val shell: IShell)`，`getShellResult(cmd)` 变成 `shell.execute(cmd)`——依赖显式出现在构造函数里。
- `LldManager` 从 `object` 改成普通类 `LldFileStore(context)`，文件路径从 `MyApplication.getDownloadDir()` 改为构造时解析好并持有；`LldDataSource` 的取数职责随之并入或注入它，环消除；`HomeViewModel` 不再直接碰文件——持久化编排归仓库。
- 删除 `ShellManager`、`PropertyManager`、`ShellDefault`（死代码）。
- `getMyString` 的 100 处调用随 [AR-01](#AR-01) 的 mapper 迁移自然消亡：mapper 拿 `Context` 参数，不再需要全局函数。

**依赖注入选型说明**：`MyApplication.instance` 在过渡期仍作为容器的唯一入口保留（1 个全局引用取代 6 个）。要不要引入 DI 框架、引入哪个，是独立决策，选型对比如下：

| 方案 | 特点 | 对本项目的判断 |
|------|------|----------------|
| 手工 AppContainer | 零依赖、零生成代码，对象图一目了然 | 对象图约 10 个对象、4 个入口，手工完全可控——推荐起步 |
| Hilt | Android 事实标准，KSP 编译期生成，Activity/Fragment 注入开箱即用 | 注解与构建复杂度较高；仅限 Android，未来走 KMP 需再换 |
| kotlin-inject | 纯 KSP、编译期校验、KMP 友好 | 若确定 KMP / Compose Multiplatform 方向，这是最顺的演进路径 |
| Koin | DSL 上手快，注解扩展可补编译期校验 | 核心仍是运行时解析，错配倾向运行期才暴露，对本项目收益低于 kotlin-inject |

判断依据：当前体量下，手工容器的维护成本低于任何框架的学习与构建成本。等 AR-01 领域模型化后对象图变复杂、或确定 KMP 方向（迁 kotlin-inject）、或协作者变多（迁 Hilt），再把 `AppContainer` 里的 new 换成注解即可——容器方案不会白做，它是以后迁到任何框架时的现成地基。


**裁定补记(2026-09-12)**:「PackageManager / packageName 下沉进仓库」不采纳——Context 是 View 层的东西,压进 ViewModel/Repository/DataSource 会让这些层绑死 Android 框架,没法做 JVM 单测,也是 Compose 迁移的阻碍;View 层解析 Context 后把值传下去是正路。真正的依赖抽象(让仓库不再需要 PackageManager)归本条目与 DI 重构。

---

<a id="AR-05"></a>

## AR-05 组合根分散：每个 Fragment 自己 new 一套对象图

**严重程度：P1 ｜ 修复难度：低**
**影响文件：`HomeFragment.kt`、`OthersFragment.kt`、`PropFragment.kt`、`SettingsFragment.kt`**

### 问题核心代码

四个 Fragment 各自在 `extrasProducer` 里手工 new 出完整的一套 Repository + DataSource（`HomeFragment.kt:23`，其余三个同样）：

```kotlin
override val listViewModel by viewModels<HomeViewModel>(
    extrasProducer = {
        MutableCreationExtras(defaultViewModelCreationExtras).apply {
            val repository = HomeRepository(          // ← 界面层在组装数据层
                LldDataSource(), MountDataSource(), AppInfoDataSource()
            )
            this[HomeViewModel.MY_REPOSITORY_KEY] = repository
        }
    },
    factoryProducer = { HomeViewModel.Factory }
)
```

`OthersFragment` 一口气 new 7 个对象，`SettingsFragment`、`PropFragment` 各自再来一套。四个 ViewModel 里还各有一份结构完全相同的 `MY_REPOSITORY_KEY` + `Factory` 样板。

### 直接原因

对象图的组装代码（「谁依赖谁」的知识）没有固定归属，被复制到了每个界面入口。改 `HomeRepository` 的构造签名，表面上要动 1 个类，实际上要动 `HomeFragment`——而 `HomeRepository` 的调用方还有 `HomeViewModel`，测试和未来第二个使用方（比如小部件、快捷方式入口）都得再抄一遍。

### 根本原因

缺少组合根（composition root，全项目唯一一处负责创建和装配对象的地方）。DI 的价值不在框架，而在「接线知识只写一次」：现在接线知识写了四遍，且写在最不稳定的 UI 层。

### 修复方案

在 [AR-04](architecture.md#AR-04) 的 `AppContainer` 之上，Fragment 退化为「取现成的」：

```kotlin
// ui/base/ext/FragmentExt.kt —— 唯一的取用入口
val Fragment.appContainer: AppContainer
    get() = (requireActivity().application as MyApplication).container
```

```kotlin
// HomeFragment —— 组装知识消失，只剩声明
override val listViewModel by viewModels<HomeViewModel>(
    extrasProducer = {
        MutableCreationExtras(defaultViewModelCreationExtras).apply {
            this[HomeViewModel.MY_REPOSITORY_KEY] = appContainer.homeRepository
        }
    },
    factoryProducer = { HomeViewModel.Factory }
)
```

进一步可把「extras + factory」的样板收拢成一个泛型辅助函数，四个 Fragment 各剩一行。注意：这一步必须在 AR-04 完成容器之后再做，否则只是把 `new` 换了个地方。

---

<a id="AR-06"></a>

## AR-06 基类反向依赖具体 Activity，insets 逻辑两处重复

**严重程度：P1 ｜ 修复难度：低**
**影响文件：`BaseListFragment.kt`、`SettingsFragment.kt`、`MainActivity.kt`**

### 问题核心代码

`BaseListFragment`（基类！）向下转型到具体 Activity，再伸手进它的 ViewBinding 拿底部导航的高度（`BaseListFragment.kt:75`）：

```kotlin
private fun initWindowInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView) { rv, windowInsetsCompat ->
        val insets = windowInsetsCompat.getInsets(windowInsetsCompatTypes)
        (activity as? MainActivity)?.binding?.bottomNavigationView?.doOnLayout { bnv ->
            rv.updatePadding(
                left = insets.left,
                right = insets.right,
                bottom = bnv.height          // ← 基类需要知道 MainActivity 布局里有 bottomNavigationView
            )
        }
        windowInsetsCompat
    }
}
```

`SettingsFragment.kt:67` 有一份逐字相同的复制。

### 直接原因

布局上底部导航悬浮在内容之上（`main_activity.xml` 中 `bottomNavigationView` 用 `layout_gravity="bottom"` + `hide_bottom_view_on_scroll_behavior`），内容需要让出它的高度。这个「让位」知识被塞进了每个内容 Fragment，而 Fragment 只能通过强转 Activity 才够得着那个视图。

### 根本原因

职责放错了层：**谁拥有视图，谁负责避让**。`bottomNavigationView` 和 `container` 都是 `MainActivity` 的直接子视图，避让逻辑理应由 Activity 一手包办；塞进 Fragment 后形成了「基类 → 具体子类内部结构」的反向依赖，基类从此被锁死在这个 Activity 上，换宿主（预览、别的入口）即断。

### 修复方案

避让上移到 Activity，一次写完，两个 Fragment 的重复代码全删：

```kotlin
// MainActivity.onCreate —— 自己的视图自己避让
binding.bottomNavigationView.doOnLayout { bnv ->
    binding.container.updatePadding(bottom = bnv.height)
}
```

```kotlin
// BaseListFragment.initWindowInsets —— 只剩系统栏的左右避让，不再认识任何具体 Activity
ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView) { rv, windowInsetsCompat ->
    val insets = windowInsetsCompat.getInsets(windowInsetsCompatTypes)
    rv.updatePadding(left = insets.left, right = insets.right)
    windowInsetsCompat
}
```

`SettingsFragment.initWindowInsets` 整个删除（它还有一行 `listView.clipToPadding = false`，移入布局 XML 的 `android:clipToPadding="false"`）。

行为差异说明：底部导航滚走时（hide on scroll），容器 padding 仍在——与现状一致（现在 Fragment 的 padding 也不会跟着收回），无行为变化。

---

<a id="AR-07"></a>

## AR-07 包结构误导：base 一词三义 + 包级循环依赖

**严重程度：P1 ｜ 修复难度：中（纯机械搬移）**
**影响文件：`MyApplication.kt`、`ShellLibSu.kt`、`LldManager.kt`、`JsonExt.kt`、`AndroidVersionExt.kt`、`PropertyExt.kt`、`ShellExt.kt` 及 `app/build.gradle.kts`**

### 问题核心代码

「base」在项目里有三个互不相干的含义：

1. **Gradle 模块** `base/`（`net.imknown.android.forefrontinfo.base.property` / `base.shell`）；
2. **app 模块里的同名包** `app/src/main/java/.../base/MyApplication.kt`——Application 类住在 app 模块却顶着 base 的包名；
3. **app 模块里的资源目录** `app/src/main/java/.../base/res/`（`build.gradle.kts` 的 sourceSets）。

更糟的是依赖方向成环（`MyApplication.kt:16`）：

```kotlin
// app 模块的 base 包（最底层的东西）反向 import UI 层：
import net.imknown.android.forefrontinfo.ui.common.ShellLibSu   // Shell 实现居然住在 ui 包
import net.imknown.android.forefrontinfo.ui.common.initMyAndroid
```

而 `ui.common` 又依赖 base 模块（`PropertyExt.kt:7` 的 `base.property.PropertyManager`）——包级循环：`base ↔ ui.common`。

同时 `ui/common/` 成了大杂烩：`LldManager`（文件 IO）、`ShellLibSu`（shell 实现）、`JsonExt`（序列化）、`AndroidVersionExt`（全局可变状态）这些与 UI 毫无关系的底层设施，和 `ViewExt.setScrollBarMode`（真·UI 扩展）挤在同一个包里。

### 直接原因

「common / base = 什么都往里放」的惯性：新文件找不到明确归属时进 common；底层实现图方便放进 common；common 再被最底层反向引用，环就闭上了。

### 根本原因

包结构没有表达层次规则。包名是开发者（和 IDE 检查）判断「谁能依赖谁」的第一线索；当 base/ui/common 的实际依赖与名字承诺相反时，每次新文件选址都在掷骰子，圈只会越画越大。

### 修复方案

只搬包不改逻辑。目标结构（配合 [AR-03](ssot.md#AR-03)、[AR-04](architecture.md#AR-04) 一并落位）：

```
app/src/main/java/net/imknown/android/forefrontinfo/
├── MyApplication.kt            # 移出 base 包；app 模块根包是它的正确位置
├── core/                       # 不依赖任何 UI 的共享能力
│   ├── androidinfo/            # AndroidVersionExt（AR-03 改造后：只读 AndroidInfo）
│   ├── json/                   # JsonExt
│   ├── lld/                    # LldFileStore（原 LldManager）
│   ├── property/               # PropertyExt → IProperty 的扩展（AR-04 改造后）
│   └── shell/                  # ShellLibSu（实现）、ShellExt → IShell 扩展
├── data/…                      # 各功能的 repository / datasource（原样保留按功能分包）
└── ui/
    ├── common/                 # 只留真 UI 工具：setScrollBarMode、ToastExt、ViewBindingExt
    └── …（各功能包不变）
```

搬移后 `MyApplication` 只 import `core.*`，环消除；`ui.common` 缩小为纯 UI 工具。`app/build.gradle.kts` sourceSets 里那组「资源跟包走」的路径需同步加 `core`（若 core 下有 res 的话目前没有，可不加）。

迁移清单（一次提交）：移动 7 个文件 + 全局改 import + 删除 `ShellDefault`（死代码）。IDE 的 Refactor → Move 可全自动完成。

---

<a id="AR-16"></a>

## AR-16 零真实测试，CI 只编译不测试

**严重程度：P1（工程配套风险） ｜ 修复难度：易（立门禁本身）；写测试依赖 AR-01 剥离纯逻辑**
**影响文件：`.github/workflows/android-ci.yml`、`app/src/test/`、`app/src/androidTest/`、`base/src/test/` 等**

### 问题核心代码

CI 只有一个构建步骤（`.github/workflows/android-ci.yml`）：

```yaml
    # region [Gradle]
    - name: Build with Gradle
      run: ./gradlew assembleFossDebug        # ← 只编译，不跑 test，不跑 lint
    # endregion [Gradle]
```

全部四个模块的测试目录里只有脚手架模板（`ExampleUnitTest`、`ExampleInstrumentedTest`），没有任何真实测试。

### 直接原因

门禁链条断在第一环：CI 不执行 `test` / `lint` 任务，即使有人写了测试也不会在合入前运行；仓库本身也没有测试可跑。

### 根本原因

「能编译 = 没问题」的默认假设。对本报告建议的所有修复而言，这很致命——AR-01（领域模型化）、AR-03（冻结全局状态）、AR-08（编排归位）、AR-14（全表 query）全部是**行为等价重构**，在没有回归保护网的情况下只能靠手工真机验证，改动意愿和信心都会被消磨掉。

### 修复方案

先让门禁转起来（一行改动），再补测试：

```yaml
    - name: Build with Gradle
      run: ./gradlew assembleFossDebug testFossDebugUnitTest lintFossDebug
```

（注意 flavor：任务是 `testFossDebugUnitTest` / `lintFossDebug`。`base`、`binderDetector` 模块随 `test` 生命周期任务一起跑。）

项目目前没有配置任何静态分析工具（detekt / ktlint / spotless 均未出现）——`lintFossDebug` 先把 AGP 自带的官方检查跑起来，需要更严再引入 detekt。

首批值得写的测试（不需要等 AR-01 完成，抽纯函数即可测）：

```kotlin
// 示例：lld 日期契约（AR-15 的防线，一个测试锁死契约）
@Test
fun `lld 日期格式契约`() {
    assertTrue("2026-09-02 20:55 +0800".isLldDatetime())
    assertFalse("2026-09-02 20:55 +08:00".isLldDatetime())
}

// 其余首批对象：toPropMyModel 的 getprop 输出解析、
// MountDataSource 行解析（抽成纯函数）、Lld 反序列化（ignoreUnknownKeys 行为）、
// MyModel 的 DiffUtil key 判定
```

AR-01 做完之后，`PerformanceClassInfo` 这类领域判断可全部纳入单测，测试才能真正铺开。

---

---

<a id="AR-20"></a>

## AR-20 「常量仓库」名不副实，常量跨功能引用，两个同名 DataSource 异义

**严重程度：P2 ｜ 修复难度：低（重命名 + 搬移，随 AR-07 顺手）**
**影响文件：`AndroidDataSource.kt`、`BasicDataSource.kt`、others 与 settings 的 `FingerprintDataSource.kt`**

### 问题核心代码

`AndroidDataSource` 没有任何数据源行为，是纯常量表，却顶着 DataSource 之名（`ui/home/datasource/AndroidDataSource.kt`，全文件 145 行只有 companion object 常量）：

```kotlin
class AndroidDataSource {
    companion object {
        const val PROP_AB_UPDATE = "ro.build.ab_update"
        const val PROP_VENDOR_SKU = "ro.boot.product.vendor.sku"
        // …约 40 个常量，无一个方法
    }
}
```

系统属性名是全设备共享的知识，却作为 home 功能的私有物，被 others 功能跨包引用（`BasicDataSource.kt`）：

```kotlin
import net.imknown.android.forefrontinfo.ui.home.datasource.AndroidDataSource

fun getVendorSku() = getStringProperty(AndroidDataSource.PROP_VENDOR_SKU)
```

另外 others 与 settings 各有一个职责不同的 `FingerprintDataSource`（前者读分区指纹，后者算签名证书 SHA-256），同名异义，IDE 自动导入易错。

### 直接原因

常量跟着「第一个使用者」所在的 feature 放置；命名沿用 DataSource 后缀，但内容早已不是数据源。

### 根本原因

与 [AR-07](architecture.md#AR-07) 同根：缺一个中立的共享层（core），跨功能知识只能寄生在某个功能包里，名字再错也只能将就。

### 修复方案

```kotlin
// core/android/AndroidProps.kt —— 名副其实、位置中立，跨功能引用消失
object AndroidProps {
    const val AB_UPDATE = "ro.build.ab_update"
    const val VENDOR_SKU = "ro.boot.product.vendor.sku"
    // …
}
```

两个 `FingerprintDataSource` 分别更名为 `PartitionFingerprintDataSource` 与 `SigningCertDataSource`（或随 [AR-07](architecture.md#AR-07) 的包重排迁入各自功能后自然消歧）。随 AR-07 的机械搬移一并完成，不单独开提交。

---

<a id="C6"></a>

## C6 判定圆点语义:Home 每行各自为政,Unknown 常渲染为红色

**严重程度:P1(🔴,信任风险) ｜ 修复:三态结果类型落地时(见 [R5](#R5))**

- 不存在中央判定函数。每个检测器各自从原始值选颜色;大量条目是两态的 `toColoredMyModel(title, detail, condition ? green : red)`(如 A/B、DSU、开发者选项、ADB、动态分区,`MyModelExt.kt:8-15`)。
- **未知/不可读值通常塌缩为红色**——例如 Android 版本未知时 `lld == null -> R.attr.colorCritical`(`HomeRepository.kt:143-148`,复验);仅 GSI「未识别」映射为黄色。
- 规格影响:NFR-7 要求 Unknown 绝不能读作失败;FR-15 把含义推迟到 Q9。这会让 Explorer 画像被红色误导。

**裁定 2026-09-06(Q5)**:四值颜色表示(NoProblem / Warning / Critical / 无)被接受为当前表示;剩下的工作 = 给 Unknown 一个明确的映射(**绝不悄悄显示成 Critical**)+ 异常证据显示出来(与 C1 配套)。范围仅 Home(Others/Prop 纯展示)。落在三态结果类型落地时。

---

<a id="R5"></a>

## R5 无三态模型、无检测器注册表 —— 目录硬编码(FR-1 AC2 / FR-3 / ADR-008)

**严重程度:P1(🔴,Compose 迁移前必须关闭的主要结构缺口) ｜ 修复:注册表引擎**

- 唯一的行模型是 `MyModel(title, detail, @AttrRes color, type)`——「级别」就是主题颜色属性;**不存在 支持/不支持/未知 类型**(「支持/不支持」只是本地化的详情字符串,`HomeRepository.kt:1169-1175`)。FR-3 的模型与 ADR-005 的「Unknown 是一等结果」在代码中没有表示。
- 目录是字面的 `tempModels += repository.detectXxx()` 序列(`HomeViewModel.kt:130-165`;`OthersViewModel.kt:41-102`;`PropViewModel.kt:37-43`)——增删条目 = 改函数体,正是 FR-1 AC2 / ADR-008 禁止的。
- 目录计数与渲染行数不符:Home = 23 个检测器调用但 **25 行**(`detectSecurityPatches` 产 2 行、`detectTrebleAndGsiCompatibility` 产 2 行,复验);Others = 36 固定 + API 条件项 + N 个分区指纹动态行;Prop = 5 个检测器各产动态行。目录(见 [docs/spec-cn/05](../../spec-cn/05-detection-item-catalog.md))预填的值是 23/37/5。
- 连锁后果:没有三态类型 → 没有统一的圆点映射(C6)→ 每行各自随便配色;没有注册表 → 目录附录没法按设计一一对应。Others/Prop 纯展示(2026-09-06),三态 + 注册表工作只需覆盖 Home 条目。

**裁定 2026-09-06(Q5/Q6/Q7)**:三态语义、注册表引擎、有界并发维持书面目标(四值颜色属性被接受为过渡表示)——作为代码 TODO 开放,不是文档抄写的问题。

---

<a id="A3"></a>

## A3 检测严格顺序执行、不感知取消(§3.3)

**严重程度:P1(🟡) ｜ 修复:注册表引擎落地时**

`BaseListViewModel.startLoad()` 只启动**一个**协程;三个 ViewModel 逐个 `+=` 顺序运行检测器(复验)。无 `async`/有界并发、无 `isActive` 检查;只有 json 拉取/解析离开主线程。慢设备上串行 shell/`getprop` 探针拉长加载时间,也放大 C4/C5 的竞态窗口。

**裁定 2026-09-06(Q6=A)**:有界并发维持为目标;§3.3 同时钉死**固定展示顺序**与检测器依赖。落在注册表引擎落地时(与 [AR-12](anti-patterns.md#AR-12) 的调度器问题同属检测执行模型,但相互独立)。

---

<a id="A6"></a>

## A6 规格技术栈缺口(Part 09)

**严重程度:P1(🟡,工程配套) ｜ 补齐节奏:大重构开工前先落测试基建,其余分批**

已就位:coroutines、Ktor(+OkHttp 引擎)、kotlinx.serialization、LeakCanary(debug)、StrictMode(debug)、libsu、androidx.preference。**缺**:DataStore(+protobuf)、Baseline Profiles、App Startup、Macrobenchmark、JUnit 5、MockK、Turbine、Espresso、Robolectric(Coil 缺席是正当的——规格本就按需采用)。其余合规:AGP/Gradle/Kotlin 均为当前稳定版、compileSdk/targetSdk 37、minSdk 23、全部 resolved 依赖版本稳定无 Beta(ADR-010 ✓)。

补齐节奏:JUnit 5 + MockK + Turbine 先行(大重构开工前);Espresso/Robolectric 随 Compose 迁移;Baseline Profiles/App Startup/Macrobenchmark 属 Phase 2+。注:五个拆分 version catalog(`gradle/toml/` 下 build/android/kotlin/google/thirdParty)是项目实际约定,非缺口。

---

返回 [README](README.md)
