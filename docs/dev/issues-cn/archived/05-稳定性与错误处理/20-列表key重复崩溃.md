<a id="i20"></a>

# 20 `MyModel.key` 用标题, 重复即崩溃

> 返回 [README 索引](../../README.md) · [5 · 稳定性与错误处理](../../README.md#5--稳定性与错误处理).

**严重程度: P1 | 修复难度: 低**

**现象**: `LazyColumn` 的 `key` 取自 `MyModel.key`, 而 `key` 就是标题本身: `MyModelTitle.Res` 取资源 id,  
`MyModelTitle.Raw` 取标题文本. 同一页一旦出现两个标题相同的条目,  
`LazyColumn` 直接抛 `IllegalArgumentException: Key was already used`, 整页崩.

**问题代码**:

```kotlin
// MyModel.kt
val key: String
    get() = when (title) {
        is MyModelTitle.Res -> title.id.toString()
        is MyModelTitle.Raw -> title.text        // ← 可能重复
    }
```

```kotlin
// MyModelListScreen.kt → MyModelListContent() 的 items()
key = { it.key },
```

UI 侧没有 `distinctBy`, 也没有任何东西再兜一层.  
全仓已无 `DiffUtil` (RecyclerView 时代的 `areItemsTheSame` 对同 key 只是动画与复用怪异, 不抛异常), 那层 "不会崩" 的缓冲随迁移一起没了.

**可达路径 (已核实)**: 崩溃不需要 "两行业务数据恰好同名", 逐条错误隔离自己就会造出同名行. `PropRepository` 的三个数据源用的是 **同一个标题资源**:

```kotlin
fun getSystemProp(): List<MyModel> = guardedMyModels(R.string.title_prop) { ... }
fun <T : Settings.NameValueTable> getSettings(subSettingsKClass: KClass<T>): List<MyModel> = guardedMyModels(R.string.title_prop) { ... }
fun getBuildProp(): List<MyModel> = guardedMyModels(R.string.title_prop) { ... }
```

`PropViewModel.collectModels()` 里 `getSettings()` 被调 3 次  
(`Settings.System` / `Settings.Secure` / `Settings.Global`), 加上 `getSystemProp()` 与 `getBuildProp()`,  
一页共 5 个守卫块. 块内抛出非 `CancellationException` 时,  
`guardedMyModels()` 拿 **同一个入参标题** 生成错误行  
(`listOf(toErrorMyModel(title, e))`, `toErrorMyModel()` 原样沿用 `title`), 于是任意两块同时失败,  
列表里就是两条 `title.id` 相同的行 → 同 key → `LazyColumn` 抛异常.

这条路径是随逐条错误隔离 (`f089854a`) 一起出现的: 该提交之前, 三块源内部各自吞异常返回空列表, 整页要么全成要么整体降级, 凑不出两条同名行. P1 的依据就在这里: 原来是潜伏缺陷, 现在是每天都走得到的活崩溃链.

**同形状的守卫**: `OthersRepository.getBinderStatus(driver)` 用 `guardedMyModel(R.string.binder_status)`, 而  
`OthersViewModel` 目前只用 `ArchitectureDataSource.DRIVER_BINDER` 调它一次, 所以还没撞上;  
一旦加第二个驱动 (`/dev/binderfs`, `/dev/hwbinder`, `/dev/vndbinder` 中的任何一个), Others 页就是同一个坑.  
Prop 页每行的 `Raw` 标题来自属性键与 `Settings` 的 `类名.键`, 数据行本身逐行唯一, 出问题的是块级错误行, 不是数据行.

**直接原因**: 守卫块的错误行复用块标题, 而块标题在 Prop 页是三处共用一个资源.

**根本原因**: key 承担了两个职责 (列表项身份 + 动画/滚动状态锚点), 却复用了 "业务标题" 这个天然可能重复的值, 且代码里没有任何兜底.

**修改方案** (任选, 第 1 条与第 2 条可并行):

1. 每个守卫块给不同的标题资源: `title_prop` 拆成系统属性 / `Settings` (再按 System, Secure, Global 区分) / `getprop` 各自的标题, 错误行就各带自身身份,  
   改动只在 `PropRepository` 与各 locale 的 `strings.xml`, 最贴现状;
2. key 侧兜底: 传 UI 前 `distinctBy { it.key }`, 或用 `itemsIndexed` 让序号参与 key. 代价要认下来:  
   `distinctBy` 会把第二条错误行整个藏掉 (故障消失而不是显示), 序号并入 key 则让 "行序变化" 被当成身份变化, 动画与滚动锚点跟着抖;
3. 终极方案给 `MyModel` 增加显式 `id` 字段, 让 key 不再依赖展示文本.

**落法 (止血批)**: Prop 页三个源取消块级守卫: 每行标题即各自的数据键 (JVM 系统属性的键, `Settings.<表>.<键>`, getprop 的属性键), 行与行之间不再可能同 key;  
取数失败渲染一条源级错误行 (技术标识标题), 失败行保持无色; 未新增任何字符串资源. 走的不是上面三条方案的任何一条: 块守卫整个消失, "身份" 回到数据键本身. Others 页的脚枪保留:  
每个 item 一个 `guardedMyModel` + 专属标题资源, 将来新增条目若复用现有标题资源即复现同 key 崩溃,  
该尾巴由 [#03](../../01-架构与分层/03-无三态模型与注册表.md) 的显式 id 收口.
