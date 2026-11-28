# Foundation 与 Runtime 新特性

> **状态**: **frozen**. 本篇梳理 Compose 1.13.0 在基础库与运行时层面的重要机制革新, 性能特性及新 API.

## 核心结论

1. Compose Runtime 1.13.0 引入 `ComputedState`, 提供不带缓存记账开销的按需计算状态, 补充了 `derivedStateOf` 的适用盲区.  
   核实程度: 当场核过 (核对官方 Runtime 1.13.0 更新说明与 API 描述).
2. Compose Foundation 1.13.0 正式稳定了 `FlexBox` 与 `Grid` 自适应布局, 官方同步弃用 `ConstraintLayout for Compose`.  
   核实程度: 当场核过 (核对官方发布博客重点与组件声明).
3. Compose UI 1.13.0 增加原生渐进模糊 (`Modifier.blur` 搭配 `BlurRadiusSpec`) 与多边形着色器边框 (`Modifier.border`), 图形渲染流水线进一步下推到平台运行时着色器.  
   核实程度: 转述未核 (转述自 Compose UI 1.13.0 官方更新日志).
4. 排版层面新增 `LineHeightStyle.Mode.PerLine`, 解决单行混排不同字体大小时行高计算割裂的问题.  
   核实程度: 转述未核 (转述自官方发布日志文字排版段落).

## 特性深度拆解

### 1. `ComputedState` 与按需计算

在 Compose 早期版本中, 派生计算主要依赖 `derivedStateOf`.  
然而 `derivedStateOf` 会维护一套缓存结构, 并追踪输入状态的快照版本. 当派生值频繁变更且计算代价非常廉价时 (例如单纯的算术运算, 简易布尔判断), 缓存本身的管理开销反而高过重新计算.

Runtime 1.13.0 提供的 `ComputedState` 的核心差异:
- **无缓存按需执行**: 每次读取时即时求值, 不占用对象缓存与哈希表记账.
- **适用场景**: 结果不依赖对象同一性, 计算不分配大内存对象, 内部不读取大量状态, 单帧内读取频次可控的计算逻辑.
- **与 `derivedStateOf` 的取舍准则**: 当计算逻辑昂贵且结果变更频率显著低于输入状态变更频率时, 依然选择 `derivedStateOf`; 其它轻量派生推荐使用 `ComputedState`.

### 2. 稳定的自适应布局: `FlexBox` 与 `Grid`

- `FlexBox`: 支持子元素自动换行, 弹性延伸与收缩, 适用于动态标签排版, 动态筛选器等非惰性自适应场景.
- `Grid`: 提供非惰性的二维结构排布 (区别于 `LazyVerticalGrid`), 适用于固定的卡片仪表板和多列控制面板.
- 上游同步将 `ConstraintLayout for Compose` 标记为弃用, 引导开发者使用 `Row`, `Column`, `Box`, `FlexBox` 与 `Grid` 的组合实现声明式约束.

### 3. 原生渐进模糊 (Progressive Blur)

Compose UI 1.13.0 的 `Modifier.blur` 支持 `BlurRadiusSpec`, 可配置线性, 径向渐变模糊或自定义 AGSL 着色器.  
弹窗组件 (`Dialog`, `Popup`) 原生支持该模糊配置, 无需通过 WindowManager 层自行侵入绘制.

### 4. 惰性列表预取与性能优化

Foundation 1.13.0 优化了基于 `LazyLayoutCacheWindow` 的预取管线, 修复了边缘像素拟合导致的滚动动画截断以及微卡顿问题.  
在 `LazyColumn` 配合 `Modifier.animateItem()` 场景下, 内部重排与滚动复用效率进一步提升.
