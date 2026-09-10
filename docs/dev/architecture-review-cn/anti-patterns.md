# 反模式与隐患

> 返回 [README](README.md)。代码层面的坏习惯和潜伏风险,单项影响小,合起来拖慢排障、放大故障。

<a id="AR-09"></a>

## AR-09 State 没有错误态，加载异常没有出口

**严重程度：P2 ｜ 修复难度：中**
**影响文件：`StateExt.kt`、`BaseListViewModel.kt`、`BaseListFragment.kt`、各 ViewModel**

### 问题核心代码

状态只有三种，没有失败分支（`StateExt.kt:3`）：

```kotlin
sealed interface State<out T> {
    data class Done<out T>(val value: T) : State<T>
    data object Loading : State<Nothing>
    data object NotInitialized : State<Nothing>
}
```

加载不设防（`BaseListViewModel.kt:41`）——`collectModels()` 一旦抛出未捕获异常，后果比"协程死亡"更严重（见下方直接原因）：

```kotlin
loadJob = viewModelScope.launch {
    setLoading()
    val list = collectModels()   // ← 无 runCatching，异常一路穿透
    setModels(list)
}
```

而界面把「正在转圈」绑在状态上（`BaseListFragment.kt:59`）：

```kotlin
State.Loading -> binding.swipeRefreshLayout.isRefreshing = true
is State.Done -> { …; binding.swipeRefreshLayout.isRefreshing = false }
// 没有 Error 分支可走
```

### 直接原因

异常没有出口：`Loading` 之后要么 `Done` 要么什么都不是。`viewModelScope.launch` 里未捕获的异常**不会**只让协程默默结束——它会传给线程的未捕获异常处理器，在 Android 上就是**应用崩溃**，[AR-15](anti-patterns.md#AR-15) 就是现成的触发链；只有当异常恰好被中途某处吞掉时，界面才表现为刷新圈永远转下去。当前各 ViewModel 用 try/catch 把绝大多数异常吞成了「错误文案拼进条目详情」，暂时没炸；但这是把错误信息当**数据**渲染（DiffUtil 会把它当内容变更去对比），而且将来任何一条新增的没保护的路径都会触发上面两种结局之一。

### 根本原因

`State` 建模时只考虑了「成功」一条路。错误处理策略（吞进字符串）与状态机（无 Error 态）互相迁就，谁也不完整：既没有结构化的失败表示，条目数据又被错误文案污染。

### 修复方案

```kotlin
// StateExt.kt —— 补上失败分支
sealed interface State<out T> {
    data class Done<out T>(val value: T) : State<T>
    data object Loading : State<Nothing>
    data object NotInitialized : State<Nothing>
    data class Error(val cause: Throwable) : State<Nothing>
}
```

```kotlin
// BaseListViewModel.startLoad —— 异常成为一等公民
loadJob = viewModelScope.launch {
    setLoading()
    runCatching { collectModels() }
        .onSuccess { setModels(it) }
        .onFailure { setError(it) }
}

@MainThread
private fun setError(cause: Throwable) {
    if (BuildConfig.DEBUG) cause.printStackTrace()
    modelsStateFlow.value = State.Error(cause)
}
```

```kotlin
// BaseListFragment.collect —— 停圈 + 提示 + 复用 refresh() 重试
is State.Error -> {
    binding.swipeRefreshLayout.isRefreshing = false
    binding.root.context.toast(R.string.load_failed)   // 或展示空态视图 + 重试按钮
}
```

迁移说明：现有「错误拼进条目」的产品表现（如 LLD 拉取失败仍显示离线数据 + 失败原因）不必推翻——那属于「降级成功」；`State.Error` 只兜底真正的整体失败。新增 `load_failed` 字符串资源即可。

另注：项目已有一个定下来的决定（2026-09-05 逐条目错误处理任务，记录没留下来，结论并入 [C1](anti-patterns.md#C1)）——错误防护按「逐条目 try/catch、异常直接显示在该条目上」实施，顶层暂不加 catch（当时为让卡死修复保持最小）；Others / Prop 的 `collectModels` 完全无防护也在该任务范围内。本条的 `runCatching` 兜底与该决策不冲突：逐条目隔离是第一道防线（单项失败变成一条红色条目），顶层兜底是第二道（意外逃逸的异常不再变成崩溃）。建议实施顺序：先逐条目，后兜底。

---

<a id="AR-11"></a>

## AR-11 把资源 ID 当「逻辑值」和「数据」用

**严重程度：P2 ｜ 修复难度：低~中**
**影响文件：`HomeRepository.kt`、`HomeViewModel.kt`、`SettingsRepository.kt`、`MyModel.kt`**

### 问题核心代码

字符串资源 ID 被当成枚举做逻辑判别（`HomeRepository.detectMode`，`HomeRepository.kt:64`）：

```kotlin
fun detectMode(lld: Lld?, errors: List<String?>, modeResId: Int): MyModel {
    …
    color = if (modeResId == R.string.lld_json_online) {   // ← 用「文案的编号」判断业务分支
        R.attr.colorNoProblem
    } else {
        R.attr.colorCritical
    }
```

资源 ID 还作为数据跨层传递（`SettingsRepository.Version`，`SettingsRepository.kt:99`）：

```kotlin
data class Version(
    @param:StringRes val id: Int,   // ← 数据类里携带「用哪条文案」的界面指令
    val versionName: String,
    …
)
```

### 直接原因

- `R.string.lld_json_online` 是**文案地址**不是业务语义。资源重命名、多模块合并资源 ID 重排、或有人改了文案对应关系，这段 `if` 会静默改变颜色逻辑——编译器不会报任何错。
- `Version.id` 让 Fragment 不得不反向持有格式化知识（`SettingsFragment.kt:139` 按 `version.id` 取字符串），数据类里混入了界面指令。

### 根本原因

缺少显式的业务枚举，资源 ID 因「顺手可用」被借用作判别值——「能跑」和「语义正确」之间隔着一次无声的重构事故。

### 修复方案

```kotlin
// 业务枚举承载语义，资源只在展示层被解析
enum class LldSource { ONLINE, OFFLINE }

fun detectMode(lld: Lld?, errors: List<String?>, source: LldSource): MyModel {
    …
    color = if (source == LldSource.ONLINE) R.attr.colorNoProblem else R.attr.colorCritical
    …
}

// HomeViewModel 调用处：R.string.lld_json_online / lld_json_offline 只在拼文案时使用
```

```kotlin
// SettingsRepository.Version —— 去掉 id 字段，Fragment 自己知道自己该用哪条文案
data class Version(
    val versionName: String,
    val versionCode: Int,
    val assetLldVersion: String,
    val distributor: String,
    val installer: String,
    val firstInstallTime: String,
    val lastUpdateTime: String
)

// SettingsFragment 收集处
versionPref?.summary = MyApplication.getMyString(       // 文案选择留在界面层
    R.string.about_version_summary, version.versionName, version.versionCode, …)
```

`MyModel` 里的 `MyModelTitle.Res` / `color: @AttrRes` 属于「界面模型携带资源 ID」，与 AR-01 的 mapper 方案一致（mapper 就在 UI 层，拿资源 ID 是合法的），不需要改。

---

<a id="AR-12"></a>

## AR-12 阻塞调用跑在 CPU 线程池，每次请求新建 HttpClient

**严重程度：P2 ｜ 修复难度：低**
**影响文件：`HomeViewModel.kt`、`MountDataSource.kt`、`LldDataSource.kt`**

### 问题核心代码

阻塞式 Shell / 属性调用被包在 `Dispatchers.Default`（CPU 专用池）里执行（`HomeViewModel.kt:134`）：

```kotlin
withContext(Dispatchers.Default) {              // Default 是给纯计算用的
    tempModels += homeRepository.detectAndroid(lld)   // 内部是 Shell.cmd().exec() 阻塞等待
    tempModels += homeRepository.detectSar()           // 内部是 cat /proc/mounts 阻塞读取
    …
}
```

网络请求每次从零构建一个 `HttpClient`（`LldDataSource.kt:50`）：

```kotlin
suspend fun fetchOnlineLldJsonStringOrThrow(): String {
    …
    val client = HttpClient(OkHttp) {          // ← 每次调用新建：连接池、线程池全部重造
        engine { config { … } }
        …
    }
    val response: HttpResponse = client.get(url) { … }
    return client.use { response.body() }      // 用完即弃
}
```

### 直接原因

- `Shell.cmd().exec()`、`/proc/mounts` 读取、反射 `SystemProperties` 都是**阻塞 IO**，放 `Default` 池会占用 CPU 工作线程；协程官方约定：Default 给计算，IO 给阻塞。当前没出事是因为并发量小，但每个 `detect*` 都在错误池上排队。
- `HttpClient` 的构建成本（OkHttp 引擎的连接池、调度器）按设计是进程级复用的；每次新建等于每次丢弃所有连接复用与 keep-alive。

### 根本原因

调度器的选择是随手写的（`Default` 听起来比 `Main` 安全），客户端生命周期没有归属对象（没有容器，见 [AR-04](architecture.md#AR-04)，只能每次局部创建）。

### 修复方案

```kotlin
// ① 阻塞 IO 一律 IO 池：在各 DataSource 方法内部声明，而不是靠调用方记得包
class MountDataSource(private val shell: IShell) {
    suspend fun getMounts(): List<Mount> = withContext(Dispatchers.IO) {
        shell.execute(CMD_MOUNT).output.mapNotNull { it.toMountOrNull() }
    }
}
// HomeViewModel.detect 里的 withContext(Dispatchers.Default) 包装随之删除
// （若 AR-01/AR-08 改造后仓库内还有纯计算，Default 才有保留价值）
```

```kotlin
// ② HttpClient 复用：类级单例，进程存活期内共享
class LldDataSource(private val store: LldFileStore) {
    companion object {
        private val client: HttpClient by lazy { buildHttpClient() }   // 构建逻辑原样搬入
    }

    suspend fun fetchOnlineLldJsonStringOrThrow(): String {
        val url = …
        val response = client.get(url) { headers { append(HEADER_REFERER_KEY, HEADER_REFERER_VALUE) } }
        return response.body()        // 不再 use{} —— client 不属于本次请求
    }
}
```

---

<a id="AR-13"></a>

## AR-13 零散小问题清单

**严重程度：P2 ｜ 修复难度：低（各项独立，顺手修）**

> 编号有跳号:已修复的子项(1、2、4、11、14)已删除,编号保留原样,方便和 git 历史/审计记录对照。

### 3. getStringProperty 的版本门控散落在调用方

`PropertyExt.kt:9` 的 `condition: Boolean = true` 参数，把「该属性最低哪个 Android 版本才有」的知识压给每个调用方（`HomeRepository` 里出现了 10 余处 `isAtLeastAndroid9()` 之类实参）。版本门槛应与 key 定义放在一起：

```kotlin
// core/property/PropKey.kt —— 定义处自带版本信息
data class PropKey(val key: String, val since: Int? = null)

// 调用方不再记得门槛
val PROP_RO_VENDOR_SECURITY_PATCH = PropKey("ro.vendor.build.security_patch", Build.VERSION_CODES.P)
```

### 5. PropRepository.getBuildProp 的字符串套娃解析

`PropRepository.kt:68` 对 `getprop` 输出做 `[key]: [value]` 字符串往返解析。属于「把终端展示格式当数据格式」的 stringly-typed（用字符串代替结构化数据）味道。低优先级：等 AR-01 做完，可在 DataSource 层直接产出 `Pair<String, String>`。

### 6. ShellDefault 是死代码

`base/src/main/java/.../shell/impl/ShellDefault.kt` 定义后全项目无引用（实际使用的是 `ShellLibSu`）。直接删除。

**处置（2026-09-12 负责人定）**：保留不删——它未来可能作为 `ShellLibSu` 的替代实现留着备用。

### 7. LldManager 的属性名带动词后缀

`LldManager.kt:15`：`val savedLldJsonFileOrThrow by lazy { … }`——属性命名带 `OrThrow`（方法后缀习惯），读起来像在调用函数。改造为 `LldFileStore`（AR-04）时顺手改为 `val savedJsonFile: File`。

### 8. 版本 / 日期字符串按字典序比较

四处用 `>=` / `<` 直接比较字符串，正确性完全依赖格式恒定（`HomeRepository.kt:272` 安全补丁、`:626` Mainline、`:664` VNDK、`LldManager.kt:47` lld 版本）。零填充的 ISO 日期目前碰巧正确，但位数一变（如 `9` 对 `10`）即静默出错：

```kotlin
mySecurityPatch >= lldSecurityPatch      // "2026-08-05" 字典序比较，目前靠零填充保平安
savedLldVersion < assetLldVersion        // "2026-09-02 20:55 +0800" 同上
```

版本号统一走 `Version(...)` 比较，安全补丁解析为 `LocalDate` / `YearMonth` 后比较。

### 9. MyAdapter 在绑定期解析全局字符串

`MyAdapter.onBindViewHolder` 对每个条目调 `MyApplication.getMyString(title.id)`（`MyAdapter.kt:27`）——标题解析发生在滚动绑定时，且 Adapter 依赖全局入口。更糟的是**同一行的文本解析时机分裂**：标题每次绑定按当前语言现解析，详情却在检测时冻结成旧语言的字符串——系统切换语言后，同一行会显示「新语言标题 + 旧语言详情」，直到手动刷新。[AR-01](architecture.md#AR-01) 的 mapper 做出来之后，标题与详情统一在数据到位时解析一次，Adapter 只读现成字符串。

### 10. Easter egg 计数器放在 ViewModel

`SettingsViewModel.timesLeft`（`SettingsViewModel.kt:71`）是可变字段 + 返回资源 ID 的查询函数——「连点 7 次版本号」的交互细节属于视图层。挪进 Fragment，或改为不可变状态流转。

### 12. detectWebView 拼写近邻变量，Android 6 分支读错变量

外层 `var builtInVersionName`（`HomeRepository.kt:910`，供最终颜色判定用）与 `< Android 7` 分支的局部 `val buildInVersionName`（`:958`，build ≠ built）一词双拼；只有 Android 7+ 分支会更新外层变量，于是 Android 6（minSdk 23 下该分支的实际范围）设备上 `Version(builtInVersionName)` 恒为 `Version("")`（`:989`）——「内置 WebView 是否最新」的绿色判定失效，最新的内置 WebView 也只能显示警告色。修复：合并为单一 `maxBuiltInVersionName`，两个分支共同更新。

**处置（2026-09-12 负责人定）**：关闭，不改——只影响 Android 6；AndroidX 即将把 minSdk 提升到 API 24（Android 7.0），届时该分支整体成为死代码，修复没有意义。修复方案已验证过（曾实现并编译通过），存档于本条目描述中。

### 13. 四个 ViewModel 注入的 SavedStateHandle 从未使用

`HomeViewModel` / `OthersViewModel` / `PropViewModel` / `SettingsViewModel` 的 Factory 都 `createSavedStateHandle()` 注入构造参数，但类体内**零引用**——真正的恢复靠 ViewModel 存活 + StateFlow（配置变更够用；进程死亡后列表并不会恢复）。名义双轨、实际单轨，误导读者以为状态能跨进程死亡。修复二选一：删掉四个参数与对应 Factory 调用；或改用 `savedStateHandle.getStateFlow(...)` 让它成为真正的状态源。

**处置（2026-09-12 负责人定）**：保留不删——最小改动原则；未来真需要进程死亡恢复时，再按需设计并接入。

---

<a id="AR-14"></a>

## AR-14 Prop 页设置项逐 key 查询，一次加载几百个串行 binder 调用

**严重程度：P1（性能，用户可感知） ｜ 修复难度：中**
**影响文件：`SettingsDataSource.kt`、`PropRepository.kt`、`PropViewModel.kt`**

### 问题核心代码

先用反射取出 `Settings.System/Secure/Global` 类里的**常量字段名**当作 key 列表（`SettingsDataSource.kt:7`）：

```kotlin
fun <T : Settings.NameValueTable> getSettingsOrThrow(subSettingsKClass: KClass<T>): List<String> {
    return subSettingsKClass.java.declaredFields
        .filter { it.isAccessible = true; it.type == String::class.java }
        .map { it.get(null) as String }
        .sortedBy { it.uppercase(Locale.US) }
}
```

然后**每个 key 单独查一次值**（`PropRepository.kt:52`）：

```kotlin
list.forEach {
    val value = try {
        settingsDataSource.getStringOrNullOrThrow(          // 每次调用 = 一次 contentResolver binder 往返
            subSettingsKClass, MyApplication.instance.contentResolver, it)
    } catch (e: Exception) { … }
    tempModels.add(toTranslatedDetailMyModel(key, value))
}
```

而 `getStringOrNullOrThrow` 本身又是一层反射（`SettingsDataSource.kt:17`）：`getDeclaredMethod("getString", …).invoke(null, …)`。

### 直接原因

`Settings` 三张表的 String 常量加起来有几百个。Prop 页一次加载 = **几百次串行的跨进程 binder 查询**，外加两轮反射，全部跑在 `Dispatchers.Default` 上（与 [AR-12](anti-patterns.md#AR-12) 的「阻塞调用占用 CPU 池」叠加）。低端机上 Prop 页加载明显偏慢。

### 根本原因

把「一张可以整表枚举的 ContentProvider 表」当成了「键值对 API」来用。这同时还造成**漏数据**：常量列表 ≠ 实际存储的行——设备上动态写入的、不在 SDK 常量里的设置项全都显示不出来，SDK 里已废弃的常量又会查出空值。

### 修复方案

一次 query 拉全表（name + value 成对返回，单次 binder 往返），用显式表定义取代 KClass 反射：

```kotlin
// ui/prop/datasource/SettingsDataSource.kt
enum class SettingsTable(val uri: Uri) {
    SYSTEM(Settings.System.CONTENT_URI),
    SECURE(Settings.Secure.CONTENT_URI),
    GLOBAL(Settings.Global.CONTENT_URI)
}

class SettingsDataSource(private val contentResolver: ContentResolver) {

    /** 单次往返拿到全部「实际存储」的键值对 */
    fun getSettingsOrThrow(table: SettingsTable): List<Pair<String, String?>> =
        contentResolver.query(
            table.uri,
            arrayOf(Settings.NameValueTable.NAME, Settings.NameValueTable.VALUE),
            null, null,
            "${Settings.NameValueTable.NAME} ASC"
        )?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(0) to cursor.getString(1))
                }
            }
        } ?: emptyList()
}
```

```kotlin
// PropRepository —— 循环查询消失，直接映射
fun getSettings(table: SettingsTable): List<MyModel> =
    settingsDataSource.getSettingsOrThrow(table).map { (key, value) ->
        toTranslatedDetailMyModel("${table.name.lowercase()}.$key", value)
    }

// PropViewModel.collectModels —— KClass 参数换成枚举
tempModels += propRepository.getSettings(SettingsTable.SYSTEM)
```

注意事项：个别 ROM 可能对 Secure 表的 query 结果做过滤，若真机验证遇到，仅对该表保留逐 key 查询（key 列表仍一次取得），System/Global 照走全表。

---

<a id="AR-15"></a>

## AR-15 lld.json 的日期格式是隐式契约，上游一改格式应用即崩溃

**严重程度：P1（潜伏的崩溃链，触发条件在应用外） ｜ 修复难度：低**
**影响文件：`DateTimeExt.kt`、`HomeViewModel.kt`、`HomeRepository.kt`、`SettingsRepository.kt`、`assets/lld.json`**

### 问题核心代码

格式契约存在于两处，互不知晓：

```kotlin
// base/extension/DateTimeExt.kt:10 —— 契约 A：解析 pattern
fun String.formatToLocalZonedDatetimeString(): String {
    val pattern = "yyyy-MM-dd HH:mm Z"
    val instant = ZonedDateTime.parse(this, formatter).toInstant()   // 格式不符 → DateTimeParseException
    …
}
```

```json
// app/src/main/assets/lld.json:4 —— 契约 B：数据长这样
"version": "2026-09-02 20:55 +0800"
```

而联网路径上，`tryDetectOnline` 对取数、反序列化、保存都有 try/catch，**唯独最后的 `detect()` 调用没有**（`HomeViewModel.kt:85`）；`detectMode` 第一行就调用这个格式化（`HomeRepository.kt:63`）：

```kotlin
// HomeViewModel.tryDetectOnline —— fetch / parse / save 都有防护，detect 裸奔
val errorMessage = try {
    withContext(Dispatchers.IO) { LldManager.saveLldJsonFileOrThrow(lldString) }
    null
} catch (e: Exception) { errorMessage(R.string.lld_json_save_failed, e) }

return detect(lld, listOf(errorMessage), R.string.lld_json_online)   // ← 无防护
```

### 直接原因

网络拉取的 lld.json 不受本应用控制。只要上游把 `"version"` 改成 `"2026-09-02 20:55 +08:00"`（冒号）或 `"+8"` 之类任何与本 pattern 不符的写法，`ZonedDateTime.parse` 抛出 `DateTimeParseException`，沿 `detect()` → `collectModels()` 一路上抛；`BaseListViewModel.startLoad` 的 `viewModelScope.launch` 没有 catch（[AR-09](anti-patterns.md#AR-09)），未捕获异常传给线程处理器——**应用崩溃**。所有开启联网检测的用户同时中招。

### 根本原因

外部数据进入系统时只做了「反序列化类型检查」，没有「语义校验」这道闸。日期格式这个契约以魔法字符串的形式埋在格式化函数里，数据的提供方（lld.json 维护者）与消费方（pattern）之间没有任何机器可检查的约定——这正是数据边界（boundary）缺一道验证层的典型代价。

### 修复方案

契约集中定义 + 边界一次性校验，两道防线：

```kotlin
// base/extension/DateTimeExt.kt —— 契约唯一定义处
val LLD_DATETIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm Z")

fun String.isLldDatetime(): Boolean =
    runCatching { ZonedDateTime.parse(this, LLD_DATETIME_FORMATTER) }.isSuccess()

fun String.formatToLocalZonedDatetimeString(): String {
    val instant = ZonedDateTime.parse(this, LLD_DATETIME_FORMATTER).toInstant()
    …  // 原逻辑
}
```

```kotlin
// HomeViewModel.tryDetectOnline —— 网络数据进门先验货，不合格按解析失败降级走离线
val lld = withContext(Dispatchers.IO) { lldString.toObjectOrThrow<Lld>() }
if (!lld.version.isLldDatetime()) {
    return tryDetectOffline(errorMessage(R.string.lld_json_parse_failed, Exception("bad version format")))
}
```

第二道防线是 [AR-09](anti-patterns.md#AR-09) 的 `runCatching { collectModels() }` 整体兜底——即使未来再出现类似漏洞，也只是错误态而非崩溃。`SettingsRepository` 读取资产内 lld 版本处（同一契约、随包分发风险低）顺手加同样的校验。

---

<a id="AR-17"></a>

## AR-17 QUERY_ALL_PACKAGES 用途申报缺档

**严重程度:P2 ｜ 修复难度:低**
**影响文件:`app/src/main/AndroidManifest.xml`**

### 问题核心代码

```xml
<uses-permission
    android:name="android.permission.QUERY_ALL_PACKAGES"
    tools:ignore="PackageVisibilityPolicy, QueryAllPackagesPermission" />
```

### 直接原因

`QUERY_ALL_PACKAGES` 是 Google Play 的高敏感权限。本应用「过时 targetSdk 应用」功能确实需要枚举全部应用,属于正当用途,但用途申报没有随代码留档,上架/更新时容易在审核环节返工。(本条目原来的另一半——intent-filter 里的 `<action VIEW>` 残留——已修复:2026-09-12 删除,commit `5c24c742`,该部分随修复从本报告删除。)

### 根本原因

Manifest 是「只加不减」的文件,没有变更说明的归属;权限的「为什么需要」只存在于开发者的脑子里。

### 修复方案

`QUERY_ALL_PACKAGES` 保留,但在发布清单(如 `SECURITY.md` 旁或开发者私有发布笔记)留一段 Play Console 申报文案:「用于检测设备上 targetSdk 低于系统版本的预装/系统应用,核心功能依赖」。(`gwpAsanMode="always"` 与 `enableOnBackInvokedCallback` 均为有意配置,无需处理。)

---

<a id="AR-18"></a>

## AR-18 杂项：死代码隐患、错误信息丢栈、HTTP 状态不查等

**严重程度：P2 ｜ 修复难度：低（各项独立，顺手修）**

### 1. ShellDefault 若被启用，自带管道死锁隐患

`base/src/main/java/.../shell/impl/ShellDefault.kt`（[AR-13](#AR-13) 已判定为死代码）：先 `waitFor()` 再读输出流——当输出超过管道缓冲区时，子进程阻塞在写、父进程阻塞在等，**互相死锁**；且 `waitFor` 无超时。死代码不删，总有一天会被谁「顺手启用」。删除（AR-13.6）是唯一正确处理。

### 2. fullMessage 丢失堆栈，并把异常类名拼进用户可见文案

`base/extension/ExceptionExt.kt`：

```kotlin
val Throwable.fullMessage
    get() = "${javaClass.canonicalName}: $message\nCaused by: ${cause?.message}."
```

- 整个调用堆栈被丢弃（`cause?.message` 只有一层），排障时基本没有信息量；
- 无 cause 时输出 `Caused by: null.`；
- 该字符串被拼进 `R.string.lld_json_parse_failed` 等**用户可见**的条目文案，用户会看到 `kotlinx.serialization.json.JsonDecodingException: …`。

修复：Log 里打完整堆栈（`Log.w(tag, "…", e)`），用户文案只保留简短摘要（或干脆只说「解析失败」），`fullMessage` 仅供调试日志使用。

### 3. Ktor 不校验 HTTP 状态码

`LldDataSource.fetchOnlineLldJsonStringOrThrow`：`client.get(url)` 后直接 `response.body()`。404/500 返回的 HTML 错误页会被当作「JSON 解析失败」降级——行为碰巧可接受（有离线兜底），但错误归因失真（用户看到的是「解析失败」而不是「网络/服务异常」）。修复（与 [AR-12](#AR-12) 的客户端复用一起做）：

```kotlin
val response = client.get(url) { headers { append(HEADER_REFERER_KEY, HEADER_REFERER_VALUE) } }
if (!response.status.isSuccess()) {
    throw IOException("HTTP ${response.status.value}")
}
return response.body()
```

### 4. MODE_NIGHT_AUTO_BATTERY 已弃用

`MyApplication.setMyTheme` 使用的 `AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY` 在新版 AppCompat 中已标记弃用（Android 10 起省电暗色由系统接管，`MODE_NIGHT_FOLLOW_SYSTEM` 已含该行为）。随 [AR-02](ssot.md#AR-02) 的设置收拢一并处理：删掉该分支或映射到 `FOLLOW_SYSTEM`。

---

<a id="AR-19"></a>

## AR-19 本地化字符串被当作「哨兵值」，数据状态藏在翻译文案里

**严重程度：P1（设计缺陷，潜伏正确性风险） ｜ 修复难度：中（随 AR-01 渐进）**
**影响文件：`PropertyExt.kt`、`HomeRepository.kt`、`OthersRepository.kt`**

### 问题核心代码

数据访问函数把「不支持 / 失败」就地翻译成本地化**文案**返回（`PropertyExt.kt:9`）：

```kotlin
fun getStringProperty(key: String, condition: Boolean = true): String {
    val notSupport = MyApplication.getMyString(R.string.result_not_supported)
    return if (condition) {
        try {
            val default = MyApplication.getMyString(R.string.build_not_filled)
            PropertyManager.instance.getStringOrThrow(key, default)
        } catch (e: Exception) {
            Log.w("getStringProperty", "$key: ${e.fullMessage}")
            notSupport                            // ← 失败被翻译成 UI 文案
        }
    } else {
        notSupport                               // ← 「不支持」也是 UI 文案
    }
}
```

而判定层靠**比较这些文案的内容**来识别状态，全项目两处（`HomeRepository.kt:1164`、`OthersRepository.kt:150`）：

```kotlin
private fun isPropertyValueNotEmpty(result: String) =
    result.isNotEmpty()
        && result != MyApplication.getMyString(R.string.result_not_supported)
        && result != MyApplication.getMyString(R.string.build_not_filled)

// OthersRepository.getPartitionFingerprints —— 同一模式第二处：
if (fingerprint != MyApplication.getMyString(R.string.build_not_filled)
    && fingerprint != MyApplication.getMyString(R.string.result_not_supported)
) { … }
```

### 直接原因

`getStringProperty` 的返回类型是 `String`，无法表达「有值 / 缺失 / 失败」三种状态，三种状态被编码进字符串**内容**；下游只能用 `!=` 对着翻译资源反向猜状态。

### 根本原因

与 [AR-01](architecture.md#AR-01) 同根：没有结果类型。字符串内容一旦充当状态载体：断言必须携带资源上下文（不可单测）；语义对翻译资源形成依赖——正常运行中生产方与比较方同进程同语言取同一资源，比较暂时成立，但若某语言把两条文案译成同一句、或运行中切了语言，`!=` 会悄悄对不上，把「不支持」误判成「有值」。

### 修复方案

数据层返回「发生了什么」，翻译集中到唯一的展示映射：

```kotlin
// core/property/PropValue.kt —— 状态用类型表达，与 locale 彻底无关
sealed interface PropValue {
    data class Found(val value: String) : PropValue
    data object Missing : PropValue            // 属性不存在 / 系统版本不支持
    data class Failed(val error: Throwable) : PropValue
}

// 判定逻辑：模式匹配，翻译资源不参与
val hasVndk = when (val v = property.string(PROP_VNDK_VERSION)) {
    is PropValue.Found -> v.value.isNotEmpty()
    else -> false
}

// 展示翻译：全项目唯一知道这些文案的地方
fun PropValue.toDisplayText(): String = when (this) {
    is PropValue.Found -> value
    is PropValue.Missing -> MyApplication.getMyString(R.string.result_not_supported)
    is PropValue.Failed -> MyApplication.getMyString(androidR.string.unknownName)
}
```

迁移路径：`PropValue` 先在属性网关内部做出来，`getStringProperty` 保留为薄包装（内部调 `toDisplayText()`），各调用点随 [AR-01](architecture.md#AR-01) 的逐条迁移切换，避免一口气全量切换。

---

<a id="C1"></a>

## C1 逐条错误隔离(FR-6)未实现 —— 崩溃风险【发布阻塞】

**严重程度:P1(🔴 发布阻塞)**

三个 ViewModel 的检测探针均无逐条防护(复验成立):

- `HomeViewModel.detect()`(`app/src/main/java/.../ui/home/HomeViewModel.kt:130-165`)顺序调用 23 个 `detect*` 探针,**无任何逐探针 try/catch**;只有网络/解析/保存编排层有 catch。
- `OthersViewModel.collectModels()` 与 `PropViewModel.collectModels()` **完全没有 try/catch**(grep 计数为 0)——`Shell.cmd().exec()`、WebView UA 查询等在部分设备上可抛异常。
- `BaseListViewModel.startLoad()` 刻意无顶层 catch(2026-09-05 决策,见 [AR-09](anti-patterns.md#AR-09)):逃逸异常即 `viewModelScope.launch` 的未捕获异常 → **应用崩溃**。

加重因素:初始加载随启动自动运行(FR-13),受影响设备会陷入**启动即崩溃循环**。异常传播链的具体分析见 [AR-09](anti-patterns.md#AR-09),现成的触发例子见 [AR-15](#AR-15)(lld.json 日期格式)。

**裁定 2026-09-06(Q4)**:干系人接受**仓库级 try/catch 作为隔离边界**;缺口是 Others/Prop 完全没有 catch、以及若干 catch 只吞异常记日志——这些必须把异常文字显示到行内容里(FR-14)。

**修复(暂缓——2026-09-13 起负责人定:优先架构类条目)**:把每个探针调用点包上 try/catch(约 30+ 处照葫芦画瓢的修改),异常文字显示进该行内容并记日志(同时为 BL-1 单条详情攒证据)。已实现过一轮(编译 + lint + 单测全绿、4 轮 AI 复查),按负责人要求整体撤销,存档于 git stash(docs-cn 分支,消息「C1 full fix …」)。

---

<a id="R7"></a>

## R7 版本合并的残留弱点(FR-16 AC4)

**严重程度:P1(🟡) ｜ 修复:测试锁定**

字典序比较的主体与既定裁定见 [AR-13.8](anti-patterns.md#AR-13);本条记录两个残留弱点(复验成立):

- **时区偏移风险**:`LldManager.copyJsonIfNeededOrThrow()` 对版本做原始字符串比较,目前正确完全依赖两侧同用固定位宽的 `yyyy-MM-dd HH:mm Z` 格式;若时区偏移写法不同(`+0800` vs `+0000`)会错序。**裁定 2026-09-06(Q2=C)**:字典序比较成立,但格式假设必须由测试锁定——**测试需覆盖时区偏移后缀**。
- **在线路径不做版本比较**:成功的网络拉取无条件覆盖缓存(`HomeViewModel.kt:76-83`)。这是当前规格允许的行为(服务端副本通常较新),但意味着过期的服务端文件会降级本地较新的缓存——该风险的缓解(发布时更新分支 json 或钉到 tag)已记录在规格的风险表中,不需要代码改动。

---

<a id="R11"></a>

## R11 「License」行链接到 version catalog

**严重程度:P2(🟢)**

该行打开 GitHub 上的 `gradle/toml`(`strings.xml:60`,复验)而非应用内开源许可页。多数开源许可要求许可文本让人能方便看到,只有一个 toml 链接算不上合格,与「开源许可」的条目名不符。

---

<a id="R13"></a>

## R13 FR-7 比对语义事实上存在 —— 逐行、ad hoc

**严重程度:P1(🟡) ｜ 修复:特征测试锁定;注册表落地时迁入注册表条目(决策⑦)**

规格前提是「比对语义待定(Q2)」,实际约 12 个 Home 行已经对参考 json 做了比对,但各写各的、整体没法验证、没有测试(复验成立):

- 硬编码预览偏移 `250205 - 250101`(`HomeRepository.kt:226-232`);
- `"$versionName-01" >= latest` 之类的字符串 hack(`:626-628`);
- 安全补丁按年月字符串比较(`:272-273`)。

**处理(决策⑦,已批准)**:写进文档并补特征测试(把现在的行为原样用测试锁住);重新设计自然发生在 5b 注册表做出来的时候。

---

返回 [README](README.md)
