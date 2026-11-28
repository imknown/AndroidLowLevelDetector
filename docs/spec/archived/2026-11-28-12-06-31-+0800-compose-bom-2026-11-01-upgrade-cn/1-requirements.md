# Compose BOM 2026.11.01 升级 (1-requirements)

> **状态**: **living**. spec 三份产物之一, 确认先后为 1-requirements.md, 2-design.md, 3-tasks.md;  
> 目录归档进 `docs/spec/archived/` 时, 这份声明被位置取代.

## 原始需求

> 问题是什么: Jetpack Compose 新版本发布了, 改了不少东西, 需要适配
> 打算怎么做: 打算升级到 2026.11.01, 遇到 无法编译 和 警告 和 Lint 等问题 就直接修复
> 最大风险在哪: 上次发布还是 2026.09.00, 时间隔了很久, 说明 官方 攒了很多东西, 可能需要适配很多东西, 也需要大量测试.

(负责人手写, 原文照录, 本 spec 结束时拿它对照.)

## 调查输入与核证

证据基础是随本 spec 放置的 Compose 升级调查目录  
(`2026-11-20-compose-survey-cn/README.md`, 六份独立调查, 调查日期 2026-11-20, 主题为 Compose 2026.11.01 对本仓库的影响),  
加上发起本 spec 时的当面核证. 调查目录是一次性材料, 以后调查 Compose 新版本另开新目录或 spec;  
调查子目录各自声明 frozen, 结论不随代码演进更新, 所以本 spec 一律以当前代码为准;  
核证与调查的出入逐条列在这里:

- 工作树状态: `gradle/toml/android.toml` 的 `compose-bom` 条目已是 2026.11.01, 已随 task-1  
  (`95708775`) 提交; 本节的编译输出与字节码结论都在该条目内容为 2026.11.01 的状态下取的.
- 调查写的 AGP 与 lifecycle 值已过期, 最近一次依赖更新推到了新值; 本仓库侧真值源:  
  `gradle/toml/kotlin.toml` 的 `kotlin` 条目, `gradle/toml/build.toml` 的 `compileSdk` 与 `compileSdkMinor` 两个 key,  
  `gradle/toml/android.toml` 的 `androidGradlePlugin` 与 `lifecycle` 条目, 这几项都高于或等于上游要求.  
  上游要求逐条对着工件核, 全部核出 (已验证, 拆包核对): material3-android 1.5.0 的 POM 要求  
  `kotlin-stdlib` 2.2.20 与 `lifecycle-runtime-android` 2.10.0; ui-android 与 foundation-android 1.13.0  
  的 AAR 元数据要求 `minCompileSdk` 37 加 `minCompileMinorSdk` 1, 以及 `minAndroidGradlePluginVersion` 9.1.0.
- 弃用警告的完整清单以编译器输出为准: `:app:compileFossDebugKotlin` 重跑后只报一类弃用,  
  即 `ListItem` 的 `headlineContent` 重载, 三处 (`ui/settings/SettingsScreen.kt` 第 214, 222, 300 行), 其余零警告.  
  已验证 (编译日志).
- material3 1.5.0 的 AAR 里, `TopAppBar` 全系变体所在的 `AppBarKt`, 连同 `ScrollbarKt` 与 `ListItemKt`,  
  对 `ExperimentalMaterial3Api` 的引用为零; 同一个 AAR 里仍有 31 个 class 引用它  
  (BottomSheet 系, ModalBottomSheet, OutlinedTextFieldDefaults 等, 用旧注解的实验组件仍用旧注解);  
  另有 8 个 class 引用新的 `ExperimentalMaterial3ExpressiveApi`  
  (LoadingIndicator, MaterialShapes, Menu, PullToRefreshDefaults, SliderDefaults 等).  
  两类都不在本 spec 范围内. 所以 `ui/AppRoot.kt:142` 的 `@OptIn` 已经多余.  
  已验证 (拆包, 逐类统计).
- 官方滚动条修改器 `Modifier.nonInteractiveScrollbar` 已在 `androidx.compose.material3` 里;  
  `state` 参数类型为 `androidx.compose.foundation.ScrollIndicatorState` 且标 `@NotNull`,  
  而本仓库 `ui/common/ScrollBarExt.kt:40` 拿到的 `scrollState.scrollIndicatorState` 是可空的,  
  换用时调用点要做空守卫. 已验证 (拆包 javap).
- Compose 相邻依赖的版本盘点 (2026-11-28, 出处为 Google Maven 与 Maven Central 的 maven-metadata.xml):  
  activity, navigation3, lifecycle, core, savedstate, annotation, annotation-experimental,  
  kotlinx-collections-immutable, slack compose-lint-checks 全部已是各自最新稳定版, 本次不动版本.  
  已验证 (逐个查询 metadata).

## 需求条目

R-2 与 R-4 触及需求正文的 FR-17 (设置页条目清单, `docs/requirements-cn/02-功能需求.md`:  
"点整行就触发这一行的动作", 界面条目含 "滚动条模式: 无, 普通",  
AC1 "上面列出的条目一个不少, 整行可点, 且行为与上面写的一致",  
AC2 "主题与滚动条选项在应用自己的界面里不用重启就生效");  
R-1 的真机目检复验 FR-17 的 AC2; R-3 与 R-5 不映射全局需求正文.  
每条主标准的来源都在条目内写明 (原始需求, 需求正文 FR-17, 负责人的决定, 编译器输出,  
或构建约定点名的命令); 技术上必需但负责人没写的另列 "技术补充项", 不替代主标准.

### R-1 compose-bom 升到 2026.11.01

- 要什么: `gradle/toml/android.toml` 的 `compose-bom` 条目升到 2026.11.01 (这一行已随 task-1  
  (`95708775`) 提交; 本 spec 对它的验证是解析核对, CI 与真机目检), Compose 各构件经 BOM 统一解析, 不改工具链, 不加新依赖.
- 验收标准 (来源: 原始需求第二句, 以及构建约定 `docs/agents-cn/09-构建约定.md` CI 一节的四条命令):
  - 依赖解析结果: `androidx.compose.ui`, `foundation`, `runtime`, `animation` 落到 1.13.0,  
    `material3` 落到 1.5.0 (命令级: `./gradlew :app:dependencies --configuration fossDebugRuntimeClasspath` 的输出里核对).
  - 四条 CI 任务全绿: `assembleFossDebug`, `testFossDebugUnitTest`, `:base:testDebugUnitTest`, `lintFossDebug`.
  - lint 若出 baseline 新命中, 按构建约定的 "任务红了先清红, 不许缩小检查范围" 处置.
  - 真机目检, 判据给到屏与行: 三个列表页 (Home / Others / Prop) 与设置页的首行与末行,  
    行高与深浅色各看一遍; 主题与滚动条选项在应用自己的界面里不用重启就生效  
    (复验 FR-17 的 AC2); 设置页的版本行连点 7 次弹 toast, 开关行能切换, 选项行能选中.
  - 负责人确认项: 点击音效的实际听感由负责人真机确认, 验收不预设预期值.
  - 测试面: CI 四条 (Foss debug) 加上述真机目检, 不含 release 构建验证与仪器测试.
  - 代码侧约束: 不做静音, 也不新增音效或静音相关代码 (负责人的决定).
- 映射全局需求: FR-17 的 AC2 与版本行条款 (真机目检复验; 本条其余部分不映射).
- 技术补充项: 编译期除 R-2 点名的那类弃用外, 不新增任何弃用警告.

### R-2 ListItem 三处迁到新重载

- 要什么: `ui/settings/SettingsScreen.kt` 第 214, 222, 300 行的 `ListItem(headlineContent = ...)`  
  调用迁到尾随 `content` lambda 的新重载, `ListItem` 的弃用警告清零.
- 验收标准 (来源: 编译器弃用输出, 需求正文 FR-17 的 AC1):
  - `:app:compileFossDebugKotlin` 的输出里无 `ListItem` 弃用条目.
  - 三处迁移的行 (链接行, 版本行, 选项行 (主题与滚动条模式)) 整行点一次只触发一次动作,  
    只有一层波纹.
  - 设置页真机目检无错位或明显异常.
- 观感口径 (负责人的决定): 跟官方新重载的默认值走, 不为还原旧默认值显式传参,  
  与 R-4 的滚动条同口径. 既有实参不在 "官方默认" 的范围内, 保持原样:  
  三处调用的 `colors = ListItemDefaults.colors(containerColor = Color.Transparent)`  
  是设置页分组卡片透明底的既有设计.
- 映射全局需求: FR-17 (设置页条目清单的链接行, 版本行与两个选项行; AC1 整行可点).
- 技术补充项: `SettingsSwitchRow` 一带按旧度量写的注释 (`ui/settings/SettingsScreen.kt:371`)  
  与新默认值比对时, 真值源是 material3 1.5.0 的 `ListItemDefaults` 与 `ListItem.kt` 的度量分支,  
  不靠人眼量; 不符就就地改注释, 不留过期数值.

### R-3 清理 AppRoot.kt 的过期实验性注解

- 要什么: `ui/AppRoot.kt` 的 `@OptIn(ExperimentalMaterial3Api::class)` (第 142 行), 它的行尾注释,  
  以及第 11 行的 import 一起删掉.
- 验收标准 (来源: 负责人的决定, 范围选定含本条清理;  
  事实依据是字节码核证): 编译绿; 应用源码里无 `ExperimentalMaterial3Api` 残留引用  
  (注解本身仍在 material3 里, 31 个 class 引用它, 不涉及本条).
- 映射全局需求: 无.
- 技术补充项: 同步改 `docs/dev/issues-cn/07-UI与无障碍/38-工具栏不随滚动隐藏.md` 的  
  "动手前须知" 那一段, 两处都过期: "TopAppBarDefaults.*ScrollBehavior 仍属实验 API"  
  (已核, `TopAppBarDefaults` 与 `TopAppBarScrollBehavior` 对该注解引用为零, 已转正),  
  以及 AppRootShell 的 OptIn 与 "1.4.0 里仍实验" 注释的引用.

### R-4 滚动条换官方组件并删除自绘实现

- 要什么: `ui/base/list/MyModelListScreen.kt:118` 与 `ui/settings/SettingsScreen.kt:160`  
  两个调用点改用官方 `Modifier.nonInteractiveScrollbar`, 删除 `ui/common/ScrollBarExt.kt` 整个文件;  
  观感完全用官方默认值 (负责人的决定), 不为还原旧观感 (4dp 厚, onSurfaceVariant 52% alpha,  
  250ms 淡出加 300ms 延迟) 显式传参.
- 验收标准 (来源: 负责人的决定):
  - `ScrollBarExt.kt` 不存在, 应用源码与活 issue 文档无自绘滚动残留.
  - 编译绿.
  - 真机目检: Normal 档滚动时滚动条出现, 停下后淡出; None 档不画;  
    两个调用点都看 (`MyModelListScreen` 覆盖三个列表页, `SettingsScreen` 覆盖设置页).
- 技术补充项: 官方修改器的签名没有 `enabled` 参数, `orientation` 是必填参数 (来源: material3 1.5.0 的 API 签名);  
  画与不画的接线方式归 2-design.  
  同步改四个 issue 的过期表述: `docs/dev/issues-cn/07-UI与无障碍/37-滚动条可拖动是stub.md`,  
  `docs/dev/issues-cn/07-UI与无障碍/38-工具栏不随滚动隐藏.md`, `docs/dev/issues-cn/06-性能/64-滚动条组合期订阅.md`,  
  `docs/dev/issues-cn/01-架构与分层/04-base一词三义.md` 里提到自绘实现或 `ScrollBarExt.kt` 的句子.
- 映射全局需求: FR-17 (设置页条目清单的 "滚动条模式: 无, 普通"; AC2 不重启生效).
- 关联 issue: `docs/dev/issues-cn/07-UI与无障碍/37-滚动条可拖动是stub.md` 的 "自绘换官方" 一半;  
  "可拖动" 档 (Draggable) 仍无实现, 不在本 spec 范围内.

### R-5 computedStateOf 写法约定进规则文件

- 要什么: `docs/agents-cn/07-代码规则.md` 加一条 Compose 派生状态的默认选择: 新代码默认 `computedStateOf`,  
  重计算或确实需要缓存才用 `derivedStateOf`. 全仓当前两者都无存量, 这是给以后新代码的约定.
- 验收标准 (来源: 负责人的决定, "保留 R-5"):
  - 规则文件新增该条, 语言与排版和文件现有条目一致.
- 映射全局需求: 无.
- 技术补充项: 无.

## 范围外 (明确不做)

- Expressive 主题重调 (`docs/dev/issues-cn/07-UI与无障碍/59-Expressive主题跟进.md`): 另立 spec, 紧跟本 spec;  
  前置条件已就绪 (1.5.0 里 `MaterialExpressiveTheme` 已稳定, 字节码零实验注解).  
  随它一起调研的还有一条未立项的想法: 列表条目 (Home/Others/Prop) 以后可能不走 `ListItem`,  
  按自定义方式参考 Expressive 风格重做, 想法已记在该 issue 里.
- 点击音效静音: 不做, 跟系统设置走 (负责人的决定).
- `LazyLayoutCacheWindow` 配置, `LineHeightStyle.Mode.PerLine`, `PullToRefreshBox` 的 `enabled`/`threshold`:  
  不做, 负责人圈定本 spec 范围时未包含它们. 逐项依据:
  - cacheWindow: 两份调查结论相反 (一份列为适用场景, 一份判收益趋零且自注没数过条数;  
    实际 Home 显示 25 行, Others 37 条, 见 `docs/requirements-cn/04-检测条目目录.md` 的 计数规则 一节),  
    留给以后的长列表场景再评估.
  - PerLine: 上游为高字形脚本设计 (调查, 未核), 仓库三种语言是否受益未核, 行高异常由 R-1 的真机目检把关.
  - PullToRefreshBox: 目前没有禁用下拉的产品需求.
- Compose UI 测试设施 (`ComposeUiTest`, `TestFailurePolicy`) 与 `material3-ripple` 依赖: 不做  
  (仓库没有 Compose UI 测试; 走 MaterialTheme 不需要直接依赖 ripple 构件;  
  负责人圈定本 spec 范围时未包含).
- 调查盘点过且明确用不上的其余新特性 (`CapturedAnimatedVisibility`, 渐进模糊, `PolygonShape`,  
  FlexBox 与 Grid, Compose Web, A2UI 渲染器): 不做, 依据见调查目录相应结论.
- Compose 相邻依赖升级: 盘点后全部已是最新稳定版, 不动 (见调查输入一节).
- "可拖动" 滚动条档 (Draggable): 仍无实现, 跟 fast-scroll 一起另立项, 不在本 spec.
