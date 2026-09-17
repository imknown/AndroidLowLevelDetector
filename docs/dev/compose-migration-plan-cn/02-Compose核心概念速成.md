# 02 Compose 核心概念速成

> 所属迁移计划：[README](README.md) · 上一章：[01 现状盘点与目标架构](01-现状盘点与目标架构.md) · 下一章：[03 第 0 步 构建准备](03-第0步-构建准备.md)

本章只讲迁移本项目**会用到**的概念，每个概念都用项目里现成的代码对照。 读完这一章，后面各步的代码你应该能看懂七八成。

## 1. 一句话总纲：界面 = 函数(状态)

View 时代：先造好一棵控件树，数据来了再"命令"每个控件改自己（叫**命令式**）。

```kotlin
// MyAdapter.onBindViewHolder —— 命令式：拿到控件，逐个下指令
tvTitle.text = ...
tvDetail.text = model.detail
```

Compose 时代：写一个**纯函数**描述"状态长什么样时界面长什么样"（叫**声明式**）。状态变了，函数自动重新执行，界面自动更新。

```kotlin
// MyModelCard —— 声明式：只描述，不指挥
@Composable
fun MyModelCard(model: MyModel) {
    Text(text = model.detail)
}
```

- `@Composable` 注解：告诉编译器"这是描述界面的函数"。这类函数只能被其他 `@Composable` 函数调用，形成一棵调用树（组合树，对应原来的 View 树）。
- 你不再需要 `findViewById`、`Adapter`、`notifyItemChanged`——"状态变了要刷新"这件事由框架接管，这个重新执行的过程叫**重组（recomposition，重新组合）**。

官方心智模型文档：[Thinking in Compose](https://developer.android.com/develop/ui/compose/mental-model)

**白话**：把界面想成**状态的影子**——影子（界面）永远跟着人（状态）变。View 时代你要亲手去挪影子；Compose 里你只管挪人，影子自己动。

✅ 正例 / ❌ 反例（"改了数据界面不动"的经典翻车）：

```kotlin
// ❌ 反例：普通变量不是"状态"，改了它 Compose 根本不知道，界面纹丝不动
var title = "A"
Text(title)
Button(onClick = { title = "B" }) { }        // 点了也没反应

// ✅ 正例：mutableStateOf 包一层，写入会让读到它的 Text 重新执行（重组）
var title by remember { mutableStateOf("A") }
Text(title)
Button(onClick = { title = "B" }) { }        // 一点就变
```

（`by` 委托让我们把状态当普通变量读写，纯语法糖；`remember` 的意思见第 8 节。）

**记忆锚点**：Compose 口诀——**UI = f(state)**，界面是状态的函数。

## 2. 重组与稳定性：什么时候会"白刷"

重组不是全量重刷：Compose 会跳过参数没变的 `@Composable` 调用（叫**跳过/skipping**）。 框架判断"参数变没变"的方式：

- 基本类型、String、不可变类 → 一比就知，**稳定（stable）**；
- `data class` 且全是稳定字段 → 稳定（编译器推断，本项目的 `MyModel` 就是）；
- `List<T>` 这类接口 → 编译器**无法保证**背后是不是可变实现 → 判为**不稳定**，每次都可能重组整屏。

本项目正好有解药：依赖里已有 `kotlinx-collections-immutable`。它的 `PersistentList`（`persistentListOf(...)`、`.toPersistentList()`）是不可变集合，Compose 判定稳定。第 2 步的列表组件就用它接收数据（订阅处把 ViewModel 的 `List` 转成 `PersistentList`）。

> 补充：Kotlin 2.4 的 Compose 编译器插件对模块内部声明的稳定性推断更强了，配合"强跳过模式（Strong Skipping，现为默认）"，个别不稳定参数也只是多算一次比较、不会出错。集合换成 `PersistentList` 属于"低成本高收益"，不是硬性要求。
> 另一个工具是 `@Immutable` 注解——`ui/theme/Theme.kt` 里的 `ColorFamily` 已经在用，意思是"我向编译器承诺这个类不可变"。

**白话**："稳定"就是编译器敢不敢**打包票**："这东西不变，除非你重新给我一份"。`String`、`Int` 敢；`List` 不敢——接口背后可能是随时能改的 `ArrayList`。不敢打包票的，就只能每次都重新核对（重组），白干活。

✅ 正例 / ❌ 反例（`@Immutable` 不能撒谎）：

```kotlin
// ❌ 反例：嘴上说不可变，身体里藏着 var——运行时会被改，界面却以为没变，出现"幽灵数据"
@Immutable
class Bad(val title: String) { var count: Int = 0 }

// ✅ 正例：要么真不可变（全 val + 稳定类型），要么别加注解、让编译器自己判断
@Immutable
data class Good(val title: String, val count: Int)
```

**记忆锚点**：`PersistentList` = "拍过照的名单"——拍完就改不了，谁手里都是同一张。

## 3. 状态（state）与单向数据流

### 3.1 两种状态容器

| 容器 | 归属 | 本项目对应 |
| --- | --- | --- |
| `MutableStateFlow` / `StateFlow` | 协程世界（与 Compose 无关） | `BaseListViewModel.modelsStateFlow`（**原样保留**）、`MainViewModel.lastId`（第 6 步由 Navigation 3 返回栈接管，见 09 章） |
| `mutableStateOf` / `State<T>` | Compose 世界：**写入**会使依赖它的界面失效重组，**读取**则登记依赖关系 | 迁移中新增的少量本地 UI 状态（如设置页里"对话框是否打开"） |

Compose 侧订阅 `StateFlow` 用 `collectAsStateWithLifecycle()`（来自 `androidx.lifecycle:lifecycle-runtime-compose`）：

```kotlin
val models by viewModel.modelsStateFlow.collectAsStateWithLifecycle()
// 此后把 models 当普通变量用；Flow 每发一次新值，这里自动"变"，用到它的界面自动重组
```

### 3.2 单向数据流（UDF）

现状项目已经是这个形状，迁移后不变：

```
界面事件（下拉刷新）──调用──▶ ViewModel.refresh()
                                        │
                                        ▼
界面状态 ◀──collectAsStateWithLifecycle── StateFlow<State<List<MyModel>>>
```

- **状态向下流**：状态作为参数传给子组件；
- **事件向上冒**：子组件通过 lambda 参数把点击等事件交回上层。
- 这就是"状态提升（hoisting）"：子组件尽量不持有状态，只做"有状态的函数"。第 4 步会把三个列表页提炼成一个共用的 `MyModelListScreen(models, isRefreshing, onRefresh)`，它自己不认识任何 ViewModel。

官方文档：[State in Compose](https://developer.android.com/develop/ui/compose/state) · [State hoisting](https://developer.android.com/develop/ui/compose/state-hoisting)

**白话**：单向数据流就是"**水往低处流，请示往上递**"——状态像水，从父组件流向子组件；用户的操作像请示，一层层上报，由上层决定怎么改状态。

✅ 正例 / ❌ 反例（状态放哪）：

```kotlin
// ❌ 反例：开关自己记状态，外面永远不知道开没开，别处也没法同步它
@Composable
fun BadSwitch() {
    var checked by remember { mutableStateOf(false) }
    Switch(checked = checked, onCheckedChange = { checked = it })
}

// ✅ 正例：状态提升到调用方，本组件变"哑开关"——数据进、事件出，谁都能用、还能预览
@Composable
fun MySwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(checked = checked, onCheckedChange = onCheckedChange)
}
```

**记忆锚点**：状态放哪，问一句"**谁需要读它，就提升到它们共同的上一层**"。

## 4. 副作用：LaunchedEffect

"副作用（side effect）"= 在组合函数里做的不纯粹的事：开协程、收 Flow、弹 Toast。 不能直接在 `@Composable` 函数体里 `lifecycleScope.launch`——每次重组都会再启动一次。正确姿势是 `LaunchedEffect`：

```kotlin
// 旧（Fragment 里）：
viewLifecycleOwner.lifecycleScope.launch {
    listViewModel.modelsStateFlow.flowWithLifecycle(viewLifecycleOwner.lifecycle).collect { ... }
}

// 新（Compose 里）：进入组合时启动、离开组合时自动取消；key 变化时重启
LaunchedEffect(Unit) {
    settingsViewModel.someFlow.collect { ... }
}
```

> 语义差别要心里有数：`LaunchedEffect` 取消的时机是"离开组合"，而不是"生命周期降到 STOPPED"——app 退后台时组合还在，收集**不会**暂停。要精确复刻 `flowWithLifecycle`（STARTED 门槛）就用 `repeatOnLifecycle`；订阅 StateFlow 则直接用 3.1 节的 `collectAsStateWithLifecycle()`，它内部就是按生命周期来的。

订阅 StateFlow 更推荐第 3.1 节的 `collectAsStateWithLifecycle()`（内部就是干这个的），`LaunchedEffect` 留给"收一次性事件"的场景。

官方文档：[Side-effects in Compose](https://developer.android.com/develop/ui/compose/side-effects)

**白话**：`LaunchedEffect` 是"**值日生**"——进教室（组合）开始干活；全班重排座位（重组）不用重新任命；只有放学（离开组合）才走人；钥匙换了（key 参数变）= 换个值日生从头干。

✅ 正例 / ❌ 反例（副作用写错位置的两种死法）：

```kotlin
// ❌ 反例一：直接写在函数体里——重组几次就执行几次（Toast 弹三遍、协程起三个）
@Composable
fun Bad(vm: MyViewModel) {
    vm.init()
}

// ✅ 正例：副作用进门登记、出门销户
@Composable
fun Good(vm: MyViewModel) {
    LaunchedEffect(vm) { vm.init() }
}
```

**记忆锚点**：`LaunchedEffect(钥匙) { 活 }`——钥匙变了，活重干一遍。

## 5. Modifier：行内的"布局属性"

XML 属性在 Compose 里变成**链式修饰符**：

```xml
<!-- my_view_holder.xml 片段 -->
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:clickable="true" ...>
```

```kotlin
// Compose 等价物
Card(
    modifier = Modifier
        .fillMaxWidth()          // 对应 match_parent
        .clickable { ... }       // 对应 android:clickable
) { ... }                        // 对应 wrap_content：内容多大我多大
```

要点：

- 每个 `@Composable` 函数（约定俗成）第一个可选参数是 `modifier: Modifier = Modifier`，**由调用方传入**尺寸/位置约束——这替代了 XML 里的 layout_* 属性（`wrap_content` 是默认行为，不用写）。
- **顺序敏感**：`Modifier.padding(8.dp).clickable { }` 和 `Modifier.clickable { }.padding(8.dp)` 不一样——前者点击区域**不**含边距，后者含。读作"从外到内依次包装"。
- `dp`/`sp` 是数字字面量扩展属性（`10.dp`、`16.sp`），对应 XML 的 `10dp`/`16sp`。

官方文档：[Modifiers](https://developer.android.com/develop/ui/compose/modifiers)

> 展望：Modifier 之上正在出现一套新的**样式（Style）范式**（`Style { }` + `Modifier.styleable`，只管视觉属性、后写覆盖、状态动画内置）。第 1 步 4.4 节会在本项目里试水，[A·API 速查表](A-API速查表.md)有完整条目。

**白话**：`Modifier` 是**包装纸**——从上往下读就是从外到内一层层包；顺序换了，包出来的东西就不一样。

✅ 正例 / ❌ 反例（顺序敏感，两处最常踩）：

```kotlin
// 点击区域：两行只差顺序，能点的范围完全不同
Modifier.padding(16.dp).clickable { }   // 先包边距：点击区域不含边距（点空白处没反应）
Modifier.clickable { }.padding(16.dp)   // 先包点击：整块（含边距）都能点
```

```kotlin
// 背景：背景在边距"里面"还是"外面"，决定边距区域有没有颜色
Modifier.background(Color.Red).padding(16.dp)   // 背景在最外层：连边距一起红
Modifier.padding(16.dp).background(Color.Red)   // 背景在内层：只有内容区红
```

**记忆锚点**：读 Modifier 链像**穿衣服**——写在前的先穿、在最外层；想不敏感都难。

## 6. 布局基础：Column / Row / Box

对标现有三种 XML 容器：

| XML | Compose | 说明 |
| --- | --- | --- |
| `LinearLayout` 竖 | `Column { }` | 子项垂直排 |
| `LinearLayout` 横 | `Row { }` | 子项水平排 |
| `FrameLayout` / `ConstraintLayout`（简单场景） | `Box { }` | 子项叠放，用 `Modifier.align()` 定位 |

`my_view_holder.xml` 的 ConstraintLayout（标题在上、详情在下、色点叠在右上角）迁完后就是一个 `Column` + 一个 `Box` 定位的色点，见第 1 步。本项目没有复杂约束，全程用不到 Compose 版 ConstraintLayout。

官方文档：[Layouts in Compose](https://developer.android.com/develop/ui/compose/layouts/basics)

**白话**：`Column`/`Row`/`Box` 是三块基础积木——竖着摞、横着排、叠着放。M3 的成品组件（`Scaffold`、`ListItem`……）拆开看也是这三块搭的。

**记忆锚点**：名字即形状——`Column` = 柱子（竖）、`Row` = 一行（横）、`Box` = 盒子（叠）。

## 7. 主题：MaterialTheme 就是内置的 CompositionLocal

`CompositionLocal` 是"隐式上下文"机制——像 Android 里的 `Context`，但粒度是任意对象，沿组合树自动向下传。最常用的内置两个：

```kotlin
val context = LocalContext.current                       // 拿 android.content.Context
val onSurface = MaterialTheme.colorScheme.onSurface      // 拿当前主题色（对应 ?attr/colorOnSurface）
```

`MaterialTheme.colorScheme` / `MaterialTheme.typography` 本质就是两个特殊的 CompositionLocal。项目的 `AppTheme(...)`（`ui/theme/Theme.kt`）已经把浅色/深色/动态取色方案接好了，所以迁移各步里所有"主题色"都写成 `MaterialTheme.colorScheme.xxx`，深色/动态色自动正确——不再需要 `values-night/` 双份资源。

自定义扩展：`?attr/colorNoProblem`（"没问题"绿）这类自定义主题色，官方做法是给 `MaterialTheme` 加扩展属性，第 1 步会建 `LocalExtendedColors` 演示标准写法。

**白话**：`CompositionLocal` 是**环境变量**——不用一层层当参数传，子树里谁都能读到；`MaterialTheme.colorScheme` 就是官方内置的"颜色环境变量"。

✅ 正例 / ❌ 反例（取色姿势）：

```kotlin
// ❌ 反例：硬编码颜色——深色模式、动态取色、高对比度全部失灵
Text("出错了", color = Color(0xFFBA1A1A))

// ✅ 正例：语义色——是什么角色就用什么角色，主题怎么换都自动正确
Text("出错了", color = MaterialTheme.colorScheme.error)
```

**记忆锚点**：`Local` 开头 = "环境变量"；`MaterialTheme` 是官方最大的一组环境变量（颜色 + 文字）。

## 8. remember / rememberSaveable

组合函数会随时重新执行，函数里的**局部变量活不过重组**。要"记住"值：

```kotlin
var dialogShown by remember { mutableStateOf(false) }            // 重组间保留
var search by rememberSaveable { mutableStateOf("") }            // 重组间 + 进程重建后（配置变更）都保留
```

- `remember` ≈ 存到组合内存；
- `rememberSaveable` ≈ 自动塞进 Bundle（对比 View 时代的 `onSaveInstanceState`，或 `SavedStateHandle`——`MainViewModel` 现在用它存 `lastId`，Navigation 3 时代这活由返回栈接管）。

**白话**：`remember` 是**草稿纸**（重组之间有效），`rememberSaveable` 是**档案袋**（系统帮你存进 Bundle，旋转屏幕、进程重建都还在）。

✅ 正例 / ❌ 反例：

```kotlin
// ❌ 反例一：忘了 remember——每次重组都重新赋初值（输入框打着字被清空的感觉）
var query = ""

// ❌ 反例二：该用档案袋用了草稿纸——旋转屏幕状态丢失
var selected by remember { mutableStateOf(0) }

// ✅ 正例：要活过配置变更，就存档
var selected by rememberSaveable { mutableStateOf(0) }
```

**记忆锚点**：名字自带答案——remember（记住）、remember**Saveable**（记住**且存档**）。

## 9. 列表：LazyColumn

`LazyColumn` = RecyclerView 的声明式替身。对比：

```kotlin
// RecyclerView 四件套（Adapter + ViewHolder + DiffUtil +LayoutManager）在 Compose 里是：
LazyColumn(
    contentPadding = PaddingValues(12.dp),   // ≈ MyItemDecoration 的四周留白 + clipToPadding=false
    verticalArrangement = Arrangement.spacedBy(12.dp)
) {
    items(
        items = models,                      // List<MyModel>
        key = { it.key },                    // ≈ DiffUtil.areItemsTheSame 的 key；帮框架识别增删挪移
        contentType = { it.type }            // ≈ RecyclerView 的 viewType，便于复用组合结果
    ) { model ->
        MyModelCard(model)
    }
}
```

原理差异（教学重点）：RecyclerView **回收复用 View 对象**；LazyColumn **只在可见范围内执行组合、离开视野即丢弃**，没有"复用脏 View"问题，所以不需要 `onBindViewHolder` 反复擦写。`key` 没给时，框架只能按位置猜——列表有增删时动画和状态都会错位，所以**永远给 key**。

官方文档：[Lists and grids](https://developer.android.com/develop/ui/compose/lists)

**白话**：`LazyColumn` 像**短视频信息流**——只"播放"屏幕附近的几条，划走的直接丢，滑回来再重新组合。所以没有"复用脏 View"的问题，但前提是它得认得出"**哪条是哪条**"——这就是 key 的意义。

✅ 正例 / ❌ 反例（不给 key 的代价）：

```kotlin
// ❌ 反例：无 key，框架只能按"第 3 个位置"认条目。
// 头部插入一条后所有条目"身份"集体后移：增删动画错乱；
// 条目内部若有 TextField 之类局部状态，还会"串"到别的数据身上
items(models) { model -> ItemRow(model) }

// ✅ 正例：给稳定业务 key，框架认"人"不认"座位"
items(models, key = { it.key }) { model -> ItemRow(model) }
```

**记忆锚点**：key = 条目的**身份证**；来历就是 DiffUtil 的 `areItemsTheSame`，原班人马、原味语义。

## 10. 预览：@Preview

替代 XML 的 `tools:` 预览。给组件写一个带假数据的预览函数，IDE 里实时渲染，还支持互动模式（点一点、输文字）：

```kotlin
@Preview(showBackground = true)
@Preview(fontScale = 1.3f)          // 顺便测大字体
private fun MyModelCardPreview() {
    AppTheme { MyModelCard(previewModel) }
}
```

约定：预览函数 `private`、命名以 `Preview` 结尾，不进生产代码。它也是第 1 步"零运行时影响新增组件"的验证手段。

**白话**：`@Preview` 是"**样品间**"——用假数据把组件单独摆出来看，不依赖真机、不依赖 ViewModel、不依赖网络。

✅ 正例 / ❌ 反例：

```kotlin
// ❌ 反例：预览里拿"活物"（ViewModel/网络数据/Application 单例）——预览崩溃或干脆不显示
@Preview @Composable
fun Bad() { MyScreen(viewModel()) }

// ✅ 正例：组件只吃纯数据，预览喂假数据即可
@Preview @Composable
fun Good() { AppTheme { MyModelCard(previewModel) } }
```

**记忆锚点**：能预览的组件 = 不依赖"活物"的组件——**能不能预览，本身就是设计好坏的信号**。

## 11. 互操作两座桥

迁移期间 View 世界和 Compose 世界要互相串门，方向不同用不同的桥：

- **View 树里嵌 Compose**（本计划第 2~5 步用）：XML 或 Fragment 里放 `ComposeView`，`setContent { ... }` 里写 Compose。
- **Compose 里嵌 View**（本计划基本不需要）：`AndroidView { ... }` 工厂函数把任意 View 包进组合树。

官方文档：[Interoperability APIs](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis)

**白话**：两座桥记**方向**——**View 地盘里放 Compose** 用 `ComposeView`（View 是相框，Compose 是画，本计划第 2~5 步走这座）；**Compose 地盘里放 View** 用 `AndroidView`（把旧 View 包进新世界）。桥的使用方向反了，代码会立刻变得别扭——这是选择互操作 API 的唯一判断标准。

## 12. 新手常见误区（对应本项目）

1. **在组合函数里直接改状态**：`var shown = false` 这种局部变量改了也不刷新——必须 `mutableStateOf`。
2. **把 ViewModel 塞进子组件**：子组件应该只收数据 + lambda（状态提升），ViewModel 留在屏幕顶层组件里——这样组件可预览、可复用（三个列表页共用一个组件就靠这个纪律）。
3. **忘了给 `items` 提供 key**：列表条目动画和滚动位置错乱。
4. **在每次重组都执行的路径里做重活**：比如 `stringResource` 没问题（有缓存），但 `SharedPreferences` 读取、`Toast` 之类要放进 `remember`/`LaunchedEffect`。
5. **拿 `Context` 去 `startActivity` 之外还到处传**：能用 `MaterialTheme` / `stringResource` 表达的都别手动传 Context。

---

以上就是全部前置知识。下一步进入实操：[03 第 0 步 构建准备](03-第0步-构建准备.md)。

学完各步后，用 [A·API 速记手册](A-API速记手册.md) 过口诀、做自测题，把知识钉牢。
