# A·迁移期观察记录

> 2026-09-25 起收录于 [项目问题汇总](README.md) 作为附录 (原属 compose-migration-plan-cn 教学目录, 该目录已删, 原文见 git 历史); 下一章: [Compose 代码 Review 报告](A-Compose代码Review报告.md) · [遗留优化与回归清单](A-Compose遗留优化与回归清单.md)

迁移过程中顺手发现的, **与迁移本身无关**的旧代码问题和**有意接受的**行为差异, 集中记录在这里. 它们不阻塞迁移, 也不随某一步提交 — 旧代码按计划在第 7 步统一清理, 本文件的问题清单会随步骤陆续追加.

## 2026-09-19 · MyAdapter: 复用 ViewHolder 时 GONE 不恢复 (旧代码潜伏 bug)

`MyAdapter.onBindViewHolder` (`ui/base/list/MyAdapter.kt`) 在 `model.color == RES_ID_NONE` 时用 `sivColor.isGone = true` 藏掉色点, 但另一个分支**从不把可见性恢复回来**. RecyclerView 会复用 ViewHolder: 一个曾经显示过"无色点"条目的卡子, 滚动复用后遇到**有**色点的条目, 色点会悄悄消失.

View 世界的修法一行: `sivColor.isGone = (color == RES_ID_NONE)`.

发现于第 1 步评审: Compose 版 `MyModelCard` 用"条件即不组合" — 没有色点时那颗色点根本不进组合树 — 所以这个 bug 在新实现里**天然消失**. 旧列表按计划第 7 步删除, 大概率永远轮不到修.

## 2026-09-19 · `android:textDirection="locale"` 不在 `MyModelCard` 里复刻 (接受的偏差)

旧 `my_view_holder.xml` 的两个 TextView 都设了 `textDirection="locale"`. 而 Compose 的 `TextDirection` **没有 `Locale` 常量**(只有 Ltr/Rtl/Content/ContentOrLtr/ContentOrRtl/Unspecified), material3 的 `Text` 也没有 `textDirection` 参数 — 想原样复刻都没有 API.

**决定: 接受差异, 不复刻.** 理由:

- 对齐行为已经等价 — Compose 的 `Text` 默认 `TextAlign.Start` 跟随 `LocalLayoutDirection`, 而 `LocalLayoutDirection` 自动跟随系统语言 (locale), 和旧 `textDirection="locale"` 的效果一致;
- 剩下的差异只有"文本内部双向字符 (RTL/混排) 的判定方式": 旧=强制按 locale, 新=按文本第一个强方向字符 — 对本项目这类内容影响可以忽略;
- 反而强行写死 `TextDirection.Ltr` 会破坏 RTL 语言环境, 画蛇添足.

## 2026-09-19 · 第 2 步 (PropFragment 接入 ComposeView) 的三处等价性说明

评审核实后均**接受, 不改代码**, 理由如下:

1. **`rememberBottomBarHeight` 用 `LocalView.current.context as? MainActivity` 取宿主**: 旧代码是 `activity as? MainActivity`. Fragment 的 `requireContext()` 返回的就是宿主 Activity 本身 (不会包 ContextThemeWrapper), 所以 cast 成立; 这是过渡期桥接, 第 6 步换 Scaffold 后整体删除.
2. **底栏高度只在首次布局量一次**: 首帧 padding 为 0, 布局后跳到真值 (旧代码同款行为); BNV 高度在 Activity 不重建时变化的项目里没有此场景.
3. **LazyColumn 遇重复 key 直接抛异常**(旧 DiffUtil 只是行为怪异不崩): 这是 Compose 侧"更严格"的一处. 当前三个列表页的数据源拼装 (system prop / Settings / buildprop) 无 key 碰撞, 留意即可; 若未来某页 key 可能重复, 在数据源侧去重或在 key 后拼 index.

## 遗留优化: 数据初始加载的时机 (既存设计, 与迁移无关)

参考 skydoves"[Loading Initial Data in LaunchedEffect vs. ViewModel](https://proandroiddev.com/loading-initial-data-in-launchedeffect-vs-viewmodel-f1747c20ce62)" (Ian Lake 定调):

- **现状**: `BaseListViewModel` 由 UI 层触发 `init()` (迁移前 `onViewCreated` → 迁移后 `LaunchedEffect`, 语义等价) + 幂等 + `MutableStateFlow` 手动写入缓存. 属于文章说的两种"反模式"之一 (UI 触发型).
- **文章理想态**: 数据层暴露冷 Flow, `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)` 惰性观察 — 首订阅才加载, 失订阅 5 秒 (ANR 阈值) 后停, `collectAsStateWithLifecycle` 消费.
- **为什么现在不动**: 幂等 + loadJob 去重 + VM 级缓存已挡住该反模式的两个实际危害 (重复触发, 生命周期错位); 数据源是本地 prop, 毫秒级零成本, 惰性化的收益趋近于零.**接网络/数据库时应按 WhileSubscribed 模式重造**, 列入清理收尾后的优化候选.
