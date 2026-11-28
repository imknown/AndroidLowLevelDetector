# Compose BOM 2026.11.01 升级 (3-tasks)

> **状态**: **living**. 任务清单与验收; 没有记录节, 进度看勾选框, 轮次明细看 review 目录的 `state.md`, 历史看 git 提交.

## 任务清单

一次只做一个 task, 按清单顺序; 每个 task 一笔检查点提交, 落在本 spec 的 wip 分支  
(授权随本清单的确认给出, 边界见 `docs/agents-cn/12-Git.md`).  
每个 task 的验证顺序照 `docs/agents-cn/03-工作原则.md`: 先自审, 再 analyze-file, 再 Kotlin LSP,  
再下面的 Gradle 命令; LSP 用 `scripts/kotlin-lsp-plan.js` 的批量 diagnostics 一发查完本 task  
改过的全部 `.kt` 文件, 首次使用或怀疑同步状态时先跑 `scripts/kotlin-lsp-selfcheck.js`;  
task-1 与 task-5 按各自条目里的例外执行.

- [ ] **task-1: compose-bom 升到 2026.11.01** (R-1, 设计 1)
  - 目标: `gradle/toml/android.toml` 的 `compose-bom` 条目落 2026.11.01 (工作区里已改好, 本 task 提交它).
  - 凭什么是独立 task: 单值单点的构建改动, 单独编译与验证都成立; task-2 到 task-4 的代码迁移依赖  
    1.5.0 的构件, 必须排在它后面. 改动的是 Gradle 版本目录的 TOML, IDE 分析面里只回文件级检查,  
    与本改动结论无关, 也不在 Kotlin LSP 的 `.kt` 范围内, 所以两条分析通道豁免, 以 Gradle 解析核对为准.
  - 动哪些文件: `gradle/toml/android.toml` (一行).
  - 怎么验证: `./gradlew :app:dependencies --configuration fossDebugRuntimeClasspath` (已验证: 命令跑通)  
    核 `androidx.compose.ui`, `foundation`, `runtime`, `animation` 落 1.13.0, `material3` 落 1.5.0;  
    CI 四条 (`./gradlew assembleFossDebug`, `./gradlew testFossDebugUnitTest`,  
    `./gradlew :base:testDebugUnitTest`, `./gradlew lintFossDebug`) 全绿;  
    lint baseline 新命中按 `docs/agents-cn/09-构建约定.md` 清红, 不缩检查范围.
  - 失败退路: 解析结果不对, 编译或测试红, 或非 baseline 的 lint 红, 停下问负责人, 不改工具链与范围;  
    清红若牵动本次范围外的代码, 也停下问负责人. lint baseline 新命中按上面清红, 不属停机项 (设计 1).

- [ ] **task-2: 清理 AppRoot 的过期实验性注解** (R-3, 设计 3)
  - 目标: 删 `ui/AppRoot.kt:142` 的 `@OptIn(ExperimentalMaterial3Api::class)`, 它的行尾注释,  
    与 `:11` 的 import; 同步改 `docs/dev/issues-cn/07-UI与无障碍/38-工具栏不随滚动隐藏.md` 的  
    "动手前须知" 段整段改, 已知四处 (第 29 到 33 行), 其余三句照原样: 第 29 到 30 行整句改写  
    (`TopAppBarDefaults.*ScrollBehavior`  
    已转正, 不再需要 `@ExperimentalMaterial3Api`; `Modifier.nestedScroll` 本就是 compose-ui 的  
    稳定 API, 来源: compose-ui 1.13.0 sources 的 NestedScrollModifier.kt 第 359 行, 无实验注解),  
    第 31 行连接句改写 (以后遇到新的实验 API, 07-代码规则 仍要求先向负责人报告), 第 32 行  
    AppRootShell 的 OptIn 引用, 第 33 行 "material3 1.4.0 里仍实验" 的注释引用  
    (它引的正是本 task 删掉的那条行尾注释).
  - 凭什么是独立 task: 删注解加改一份 issue 文档的一段, 单独编译, 单独验证 (grep) 都成立;  
    与 task-3 不碰同一份文件, 与 task-4 同改 issue 38 但各改各的段 (本 task 改 "动手前须知",  
    task-4 改 "滚动容器侧同样没有接线"), 按清单顺序做不冲突.
  - 动哪些文件: `app/src/main/java/net/imknown/android/forefrontinfo/ui/AppRoot.kt`,  
    `docs/dev/issues-cn/07-UI与无障碍/38-工具栏不随滚动隐藏.md`.
  - 怎么验证: analyze-file 过 `AppRoot.kt`; Kotlin LSP 一发  
    (`node scripts/kotlin-lsp-plan.js "<AppRoot.kt 路径>:diagnostics"`);  
    `./gradlew :app:compileFossDebugKotlin --rerun` 绿;  
    `grep -rn "ExperimentalMaterial3Api" app/src base/src binderDetector/src` 零命中;  
    issue 38 的 "动手前须知" 段里不再有 "仍属 `@ExperimentalMaterial3Api`" 或 "仍实验" 这类断言  
    (转正交代里出现注解名不算命中);  
    `grep -n "仍属\|仍实验" docs/dev/issues-cn/07-UI与无障碍/38-工具栏不随滚动隐藏.md` 零命中.
  - 失败退路: 删 `@OptIn` 后编译报实验 API 错误, 恢复注解, 停下问负责人 (设计 3).

- [ ] **task-3: ListItem 三处迁到非交互新重载** (R-2, 设计 2)
  - 目标: `ui/settings/SettingsScreen.kt:214, 222, 300` 三处迁到尾随 `content` lambda 的非交互新重载  
    (设计 2 定案: 非交互重载, 手挂 clickable 原样保留);  
    `colors = ListItemDefaults.colors(containerColor = Color.Transparent)` 实参保持;  
    `SettingsSwitchRow` 一带按旧度量写的注释 (`:371`) 按 material3 1.5.0 的度量核对后改写.  
    前置动作: 拆本机 Gradle 缓存里的 `material3-android-1.5.0-sources.jar`  
    (artifact 坐标 `androidx.compose.material3:material3-android:1.5.0`), 读 `ListItemDefaults`,  
    `ListItem.kt` 的度量分支与 `ListTokens.kt` (组件默认 88dp 与 72dp 的定义处); 改注释时分清两组数:  
    组件默认值 (三行容器高 88dp, 两行 72dp) 与本仓库自己在 `:382` 写死的  
    `.defaultMinSize(minHeight = 88.dp)` 与 `:383` 的 `.padding(horizontal = 16.dp, vertical = 12.dp)`,  
    注释只描述仓库写死的那组, 不冒充组件默认.
  - 凭什么是独立 task: 单文件三处同型迁移加同文件一处注释, 单独编译, 单独验证成立.
  - 动哪些文件: `app/src/main/java/net/imknown/android/forefrontinfo/ui/settings/SettingsScreen.kt`.
  - 怎么验证: analyze-file 过该文件; Kotlin LSP 一发  
    (`node scripts/kotlin-lsp-plan.js "<SettingsScreen.kt 路径>:diagnostics"`);  
    `./gradlew :app:compileFossDebugKotlin --rerun` 输出无 `ListItem` 弃用条目;  
    `sed -n '369,372p' <SettingsScreen.kt 路径>` 对照上面两组数, 确认注释只描述仓库写死的那组.
  - 失败退路: 编译报错或真机目检见叠两层点击 (不应发生, 非交互重载内部无交互), 回退弃用重载,  
    停下问负责人 (设计 2).

- [ ] **task-4: 滚动条换官方组件并删除自绘实现** (R-4, 设计 4)
  - 目标: 删 `ui/common/ScrollBarExt.kt` 整个文件; `ui/base/list/MyModelListScreen.kt:118` 与  
    `ui/settings/SettingsScreen.kt:160` 改为取 `listState.scrollIndicatorState` (可空),  
    `scrollBarMode.drawsScrollBar` 为真且 state 非空时挂  
    `Modifier.nonInteractiveScrollbar(state, Orientation.Vertical)`, 否则不挂; 只传 state 与  
    orientation, 不传厚度, 颜色, 淡出参数 (观感完全用官方默认值, 负责人的决定);  
    两个文件第 32 与 63 行的旧 import 换成 `androidx.compose.material3.nonInteractiveScrollbar`,  
    并新增 `androidx.compose.foundation.gestures.Orientation` 的 import;  
    同步改四个 issue: `37-滚动条可拖动是stub.md` 的五处 ("今天的实现是自绘" 一节, "两档今天生效"  
    里的旧调用签名, "自绘这半边不解决可拖动" 引用的 ScrollBarExt 注释, mermaid 图里 "画自绘指示条"  
    的标签, 末尾 "处理" 段改写为官方组件已到位并接入, 可拖动档仍未实现);  
    `38-工具栏不随滚动隐藏.md` 的 "滚动容器侧同样没有接线" 段;  
    `64-滚动条组合期订阅.md` 整条归档, 按索引页规矩挪到  
    `docs/dev/issues-cn/archived/06-性能/64-滚动条组合期订阅.md` (原件原样);  
    `04-base一词三义.md` 的五处 (第 36, 39, 40, 42 与 69 行);  
    `docs/dev/issues-cn/README.md` 五处, 全按归档规矩: 第 52 行类别清单删 #64 链接段,  
    第 213 行总览表删 #64 整行, 第 391 行修复路线删 #64 段, 第 69 到 103 行归档清单新增 #64 一条,  
    第 221 行 #37 的状态行改回 开放 (自绘换官方完成, 可拖动档仍无实现).
  - 凭什么是独立 task: 删文件加两个调用点加五份 issue 文档是同一笔改动的整体, 拆开会出现  
    引用断掉或索引过期的中间态; 单独编译与验证成立.
  - 动哪些文件:  
    `app/src/main/java/net/imknown/android/forefrontinfo/ui/common/ScrollBarExt.kt` (删),  
    `app/src/main/java/net/imknown/android/forefrontinfo/ui/base/list/MyModelListScreen.kt`,  
    `app/src/main/java/net/imknown/android/forefrontinfo/ui/settings/SettingsScreen.kt`,  
    `docs/dev/issues-cn/07-UI与无障碍/37-滚动条可拖动是stub.md`,  
    `docs/dev/issues-cn/07-UI与无障碍/38-工具栏不随滚动隐藏.md`,  
    `docs/dev/issues-cn/06-性能/64-滚动条组合期订阅.md` (挪进 archived),  
    `docs/dev/issues-cn/01-架构与分层/04-base一词三义.md`,  
    `docs/dev/issues-cn/README.md`.
  - 怎么验证: analyze-file 过两个调用点文件; Kotlin LSP 一发批量  
    (`node scripts/kotlin-lsp-plan.js "<MyModelListScreen.kt 路径>:diagnostics" "<SettingsScreen.kt 路径>:diagnostics"`);  
    `./gradlew :app:compileFossDebugKotlin --rerun` 绿; `ScrollBarExt.kt` 不存在;  
    `grep -rn "net.imknown.android.forefrontinfo.ui.common.nonInteractiveScrollbar" app/src` 零命中;  
    `grep -rn "enabled = scrollBarMode.drawsScrollBar" app/src` 零命中;  
    `grep -n "#64" docs/dev/issues-cn/README.md` 的命中只在第 69 到 103 行的归档清单里;  
    `grep -n "随行 官方滚动条组件" docs/dev/issues-cn/README.md` 零命中 (旧状态串);  
    `grep -rn "自绘\|ScrollBarExt\|nonInteractiveScrollbar" docs/dev/issues-cn --include="*.md" --exclude-dir=archived`  
    命中的每一句都已逐句改过 (`archived/` 下是 record, 不在判据内);  
    `grep -n "64-滚动条组合期订阅" docs/dev/issues-cn/README.md` 的命中只在第 69 到 103 行的  
    归档清单里 (无死链).
  - 失败退路: 负责人真机不接受官方默认观感, 恢复整个文件与两个调用点, 停下问负责人 (设计 4).

- [ ] **task-5: computedStateOf 写法约定进规则文件** (R-5, 设计 5)
  - 目标: `docs/agents-cn/07-代码规则.md` 的 代码规则 一节加一条 bullet:  
    "Compose 派生状态: 不需要缓存行为时默认 `computedStateOf`; 需要缓存时才用 `derivedStateOf`."  
    (官方判据, 判据原文出处见设计 5.)
  - 凭什么是独立 task: 纯文档一条, 与其他 task 无交集, 独立成笔.
  - 动哪些文件: `docs/agents-cn/07-代码规则.md`.
  - 怎么验证: 新条目断行时除末行外带两个尾随空格, 全 ASCII 标点, 与文件现有条目的排版一致  
    (对照 R-5 验收标准).
  - 失败退路: 排版核对不过就改到过为止, 无其他失败面 (设计 5).

## 验收

全部 task 勾完且总 review 循环停了之后, 在 wip 分支 HEAD 上执行:

- CI 四条全量重跑全绿 (`assembleFossDebug`, `testFossDebugUnitTest`, `:base:testDebugUnitTest`,  
  `lintFossDebug`).
- 逐条 R- 对照 1-requirements 的验收标准, 记 满足 / 不满足 / 部分满足, 附证据  
  (file:line, 命令输出, 真机截图): R-1 (解析版本, CI, 真机目检全项, 无新增弃用),  
  R-2 (无 `ListItem` 弃用, 整行单击一次一层波纹, 设置页目检无错位或明显异常),  
  R-3 (应用源码零命中, issue 38 同步),  
  R-4 (文件不存在, 应用源码与活 issue 文档无自绘残留, 四个 issue 加索引同步, 真机滚动条全项),  
  R-5 (规则文件条目落位与排版).
- 做歪检查: 本次每一处修改都要能归到某条 R- 编号或技术补充项, 归不进去的停下来商量.
- 真机目检 (负责人): R-1 的五个小项照 1-requirements 抄齐: 三个列表页 (Home / Others / Prop)  
  与设置页的首行与末行, 行高与深浅色各看一遍; 主题与滚动条选项在应用自己的界面里不用重启就生效;  
  设置页的版本行连点 7 次弹 toast; 开关行能切换; 选项行能选中; 点击音效的实际听感由负责人确认  
  (不预设预期值). R-2: 设置页无错位或明显异常. R-4 的三个小项照抄: Normal 档滚动时滚动条出现,  
  停下后淡出; None 档不画; 两个调用点都看, 滑到长列表底部看拇指.
- 验收结果写进本节, 补一笔检查点提交, 然后按 `docs/agents-cn/12-Git.md` 重排成最终提交.

### 验收结果 (2026-11-28)

- CI 四条全量重跑 (wip 分支 HEAD `f1b27136`): `assembleFossDebug`, `testFossDebugUnitTest`,  
  `:base:testDebugUnitTest`, `lintFossDebug` 全部通过; lint 无新命中 (baseline 里 1 error,  
  6 warnings, 1 hint 是旧账).
- R-1 满足: `gradle/toml/android.toml` 的 `compose-bom` 条目落 2026.11.01; 解析核对  
  (`fossDebugRuntimeClasspath`) ui, foundation, runtime, animation 全落 1.13.0, material3 落 1.5.0,  
  旧传递 pin 全被冲突重定向 (v1 两家 reviewer 独立复核, MiniMax 另拆 BOM POM 本体核对); 真机目检  
  (HEAD 代码构建的包, dex 内容核对过): 三个列表页与设置页的首末行, 行高与深浅色, 主题与滚动条选项  
  不重启生效, 版本行连点 7 次 toast, 开关行切换, 选项行选中, 全部通过; 点击音效实测无声音,  
  负责人判定可接受 (该条标准是不预设预期值, 由负责人确认, 以确认结果为准).
- R-2 满足: 三处 ListItem 迁到非交互新重载, 编译零弃用条目 (v1 前 3 条, 迁后 0); 真机目检设置页  
  无错位或明显异常.
- R-3 满足: 应用源码 `grep -rn "ExperimentalMaterial3Api" app/src base/src binderDetector/src`  
  零命中; issue 38 的动手前须知段已同步.
- R-4 满足: `ScrollBarExt.kt` 已删除; 应用源码与活 issue 文档无自绘残留 (残量 grep 逐句审过);  
  四个 issue 加 README 索引同步; 真机目检 Normal 档出现与淡出, None 档不画, 两个列表页的拇指, 全过.
- R-5 满足: `docs/agents-cn/07-代码规则.md` 的条目落位, 排版与现有条目一致.
- 做歪检查: 执行段与修复段动过的文件逐处归到 R-1 到 R-5, 技术补充项, 或两条负责人授权 (清单外的  
  archivesName 修复与预览样例), v1 到 v3 三轮总 review 都做过两方向覆盖核对, 无归不进去的改动.
- 验收通过, 进重排.
