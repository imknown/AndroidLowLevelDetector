# 设置 SSOT (#08 + #10, 方案 A): 修改计划

> 状态: living. 决策依据: `docs/dev/issues-cn` 的 #08 与 #10 (合流实施; 两条目已随本任务落地删除, 见 git log), 加开工前两项负责人裁定:  
> 报告语言 = 中文 (-cn); #08 条目预留的前置问题 ("`MyApplication` 伴生流路线是否就是可接受的终态") 的答案 = **否,  
> 方案 A 整段实施** (themeMode / scrollBarMode 迁进 SettingsStore). 本页是任务级拆分与执行约定;  
> 全部子任务的修改计划报告 (`subtask-NN-*.md`) 在本页过 plan gate 后一次性预生成.

## 负责人预写 (任务完成前不改, 收尾对照)

- 要解决什么: 解决设置页面 UDF/SSOT 问题
- 你的思路: 使用 StateFlow
- 预测的最大风险: 生命周期, 包括 Activity 重建的影响

## 目标

设置数据收敛到单一可观察归属 `SettingsStore` (issues-cn #08 全量 + #10 UI 侧投影):

1. 新建 `SettingsStore` (可注入, `@SingleIn(AppScope)`): 键集中定义, SP→Flow 桥, 写入口; 图挂 accessor.
2. 五路 SP 直读 + 设置页四处直写全部改走 store; `MyApplication` 伴生对象上的 `themeMode` / `scrollBarMode` / `setMyTheme` /  
   `setMyScrollBar` / `initTheme` / `initScrollBar` 全部删除.
3. 设置页改 UDF: ViewModel 暴露设置状态, Screen 纯 collect + 转发事件 (#10).

## 现状取证 (2026-10-03, 本分支 HEAD 84910208)

- 伴生流消费面: `themeMode` → `Theme.kt:279` (AppTheme) + `MainActivity.kt:77` (`isAppDark`); `scrollBarMode` →  
  `MyModelListScreen.kt:54` + `SettingsScreen.kt:115`. 写入面:  
  `SettingsScreen` 四个回调直写 SP (`SettingsScreen.kt:126-141`).
- 其余直读: `HomeViewModel.kt:57` (allowNetwork), `HomeViewModel.kt:75-83` (排序监听器),  
  `HomeRepository.kt:1121` (排序开关), `MyApplication.initTheme` / `initScrollBar` (启动播种).
- 关键时序 (负责人预测风险所在): `MainActivity.setTheme()` 与 `enableEdgeToEdge(::isAppDark)` 在 `setContent` **之前**同步读  
  `themeMode.value`, 今天靠 `initTheme` 在 `Application.onCreate` 播种伴生 `StateFlow`. 迁走后必须保住这条同步现值链. 硬约束:  
  `appGraph` 在 Application **构造器**里创建 (`MyApplication.kt:61`),  
  此时 `instance` (lateinit, `onCreate` 才赋值) 与 SP 均不可用, 且 Metro 绑定惰性解析, 所以播种不能发生在图创建期, 只能发生在 store 首次解析时, 首解析点由  
  `MyApplication.onCreate` 显式保证 (见关键设计约定 #1).
- SharedPreferences 绑定已在图里 (`AppGraph.kt:177-178`, 现唯一消费者 `HomeViewModel`), store 是它的第二个消费者;  
  `SharedPreferencesContainer` 头注释里 "deliberately out of scope  
  (SettingsScreen / HomeRepository, issues-cn #10/#08)" 指的就是本任务.
- `initTheme` 内有一次性的 power-saver 存值归一 (tombstone "1"), 随播种逻辑迁进 store, 语义不变.

## 关键设计约定 (子任务报告在此约束内展开)

1. store 的 `themeMode` / `scrollBarMode` 用**非空枚举 `StateFlow`, store 首次解析时同步播种** (对齐今天伴生流的现值语义);  
   `MyApplication.onCreate` 在今天 `initTheme` / `initScrollBar` 的同一位置主动解析一次 store,  
   保证首解析不晚于 `MainActivity.setTheme` 的现值读取; 流与存值的一致机制 (listener 或写穿透) 由报告定; 写入口 = store 方法 (只写 SP 一次).  
   其余布尔开关与设置页各行必须保住**首帧即存值**的现语义 (同步初值或 `stateIn(Eagerly)`, 不接受 `WhileSubscribed` + 默认值的首帧), 形态报告里定.
2. 消费面接入: `MainActivity` 经图 accessor 拿 store; 组合内的收集点 (AppTheme / MyModelListScreen / SettingsScreen) 倾向用  
   CompositionLocal 在 `setContent` 根部 provide (对齐 `LocalMetroViewModelFactory` 先例), preview 需有安全默认;  
   改参数下传的对比与取舍由子任务报告给出.
3. 存值格式零迁移: 键与值原样 (`interface_themes_*` 等); 设置页对话框比较用原始存值字符串, store 同时暴露原始值流与解析后的枚举流.
4. `HomeViewModel` 的 Rule 1 / Rule 2 (列表实时重排 / load 落地补齐) 语义必须等价保留, 也就是换源不改行为.

## 执行约定 (每个子任务相同; 沿用 Metro 任务的既立流程)

- 门槛: 每一步都要负责人点头, 也就是 review 结果 (包括 "干净") 先报告, 提交与进入下一个子任务都等同意.
- 进度账本: `progress.md` 每过一道闸门由 AI 更新; 新会话/新 agent/新模型 接续时先读 AGENTS.md + 账本, 按账本的 "下一步" 继续.
- 修改计划报告: plan gate 通过后一次性预生成全部 `subtask-NN-<slug>.md`; 预生成批次同样过新上下文 subagent 的后台 review; 每个子任务动工前走默认对照路径:  
  负责人给 目标句 + 问题单 (可空), 替换类任务加覆盖核对, AI 附概念 primer 并逐条回答; 说开始才动工.
- 每个子任务: 最小高内聚 diff, 单独可编译, 独立提交可单独 revert; 注释用负责人聊天语言书写, 解释**为什么**, 提交前整体译为英语; 构建验证 `./gradlew assembleFossDebug`.
- 流程: 动工对照 → 实施 → 新上下文 subagent 后台 review (v1) → 有发现:  
  报告 → 负责人复核确认后只修本轮要求的内容 (改了才进 v2) → 某轮无新实质发现 → 负责人放行留一句自己的话 → 提交 → 再等点头进入下一个子任务.
- 收敛护栏: 主观风格偏好与过早优化不构成新一轮; 连续几轮无新发现仍在打转时交由负责人收束.
- review 查历史只用 `git show`, 禁止 `checkout` 进工作区 (Metro 任务 ST-11 事故后的既立检查项).

## 子任务拆分

| # | slug | 范围 | 为什么单列 | 风险 |
|---|------|------|-----------|------|
| 01 | settings-store-skeleton | 新 `SettingsStore` + SP→Flow 桥 + `AppGraph` accessor; 不切任何消费方 | 纯新增, 零行为变化, 独立可编译; 是后续每一步的依赖面; SP→Flow 桥的取消/生命周期语义在此一次做对 | 高 (生疏领域: Flow 桥; 虽纯新增零行为变化可单独 revert, 但桥的语义是全部后续子任务的地基) |
| 02 | theme-vertical-slice | `AppTheme` / `MainActivity` (`isAppDark`) / `SettingsScreen` 主题行 (读写) / `SettingsViewModel` (主题状态 + setTheme) / `MyApplication` 删主题三件, `initTheme` (含 power-saver 归一) 迁 store, 相邻注释归真 | 一次搬完一个设置项的家: 删 `setMyTheme` 要求全部调用方同批走; 组合前读现值的时序 (冷启首帧 / Activity 重建) 在此验证, 也是负责人预注册风险的主战场 | 高 |
| 03 | scrollbar-vertical-slice | `MyModelListScreen` / `SettingsScreen` 滚动条行 + 指示条 / `SettingsViewModel` / `MyApplication` 删滚动条三件 (`initScrollBar` 的播种随 store 首解析一并落地), 相邻注释归真 | 同型第二刀, 模式已立降为机械; `MyModelListScreen` 是三个列表页 (Home / Others / Prop) 共用件, 消费面接入方式在此定形 | 低 (机械同型) |
| 04 | home-and-remaining-switches | `HomeViewModel` (allowNetwork + 排序监听换源, 删 SharedPreferences 注入) / `HomeRepository` 排序参数化 / `SettingsScreen` 余下两行读写, 相邻注释归真 | Home 检测链是核心逻辑, Rule 1 / Rule 2 等价性单独评审, 含两个换源行为差的显式论证 (同值写入被 `distinctUntilChanged` 折叠 / 桥接流 `onStart` 初始发射); 排序开关三个触点 (ViewModel / Repository / Settings) 是同一条数据, 必须一批走 | 高 (核心链 + 行为等价) |
| 05 | docs-closeout | issues-cn #08 / #10 删除 + README 索引同步; AGENTS.md / AGENTS-cn.md Architecture 节为 SettingsStore **新增**一句设置 SSOT 表述 (现无设置表述可改, 属新增); `MyApplication` / `SharedPreferencesContainer` 头注释更新; `AppThemeMode` / `ScrollBarMode` 枚举去留一句话裁定; module-structure-cn.md 待决问题 4 对应行 (该文件未入库, 入库与否需负责人先点头, 否则此行顺延) | 文档与代码真值同步, 前四个子任务全落地后才能一次写对 | 低 |

## 验证基线

- 每子任务: `./gradlew assembleFossDebug` 绿.
- ST-02 真机: 冷启首帧主题即正确 (不闪默认); 设置内切主题即时生效; 旋转 / 系统深色切换 recreate 后保持; `adb shell am kill` 进程杀掉重启后存值恢复.
- ST-03 真机: 滚动条三档切换即时生效 (设置页与任一列表页的指示条同步).
- ST-04 真机: 联网开关下次刷新生效, 关闭后零请求; 排序开关开着时列表实时重排 (Rule 1), 刷新中途切换, 落地后补齐 (Rule 2); 同值重复 toggle 不产生观感差 (行为差等价论证落在报告).
- ST-02..04 真机共用项: 进入设置页首帧各控件即为存值 (无默认值闪烁 / 选中项错位).
- ST-05: 零残留 grep (`themeMode` / `scrollBarMode` / `setMyTheme` / `setMyScrollBar` / `initTheme` /  
  `initScrollBar` 在 `MyApplication` 上零现在时引用;  
  `SettingsScreen` / `HomeRepository` 零 `MyApplication.sharedPreferences`;  
  注释层旧归属语句如 "MyApplication owns it" 零残留).

## 待学清单 (任务收尾已清空, 各项落地结果)

- ~~`callbackFlow` / `awaitClose` 与 SP listener 的生命周期~~ → 已学: 最终未用 callbackFlow,  
  换成 "构造期同步播种 + listener 回灌" (与现值语义同构); 关键事实 = SP 注册表持**弱引用**, store 必须字段强持有 listener;  
  同进程写是同步回调 (AOSP 7.0/8.0 `SharedPreferencesImpl` 实证, ST-03 review).
- ~~`stateIn` / `SharingStarted` 与 `collectAsStateWithLifecycle` 的组合语义~~ → 已学:  
  store 暴露的热 StateFlow 直通 ViewModel 再直通 collect 即满足 "首帧即存值"; `WhileSubscribed + 默认值` 形态被设计约定 #1 显式排除.
- ~~组合前取现值的时序面~~ → 已学: 播种钉在 `MyApplication.onCreate` (store 首解析 = 构造, `instance` / SP 就绪之后); 组合前读 `.value`,  
  界面内 collect; Activity 重建靠 app 作用域单例 + StateFlow 重放, `am kill` 靠重新播种, 均真机验证.
- ~~Metro 图 accessor 惯用法~~ → 已学: 图接口声明 `val settingsStore: SettingsStore` 即可,  
  `@Inject` 类 + `@SingleIn(AppScope)` 自动绑定; 相关的坑在 CompositionLocal 一侧,  
  `provides` infix 定义在 `ProvidableCompositionLocal` 子类型上, 显式标注父类型会抹掉它.
