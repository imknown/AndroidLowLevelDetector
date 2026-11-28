# Compose BOM 2026.11.01 升级 (2-design)

> **状态**: **living**. spec 三份产物之二; 目录归档进 `docs/spec/archived/` 时, 这份声明被位置取代.

## 设计 1: compose-bom 升级 (R-1)

- 改动: `gradle/toml/android.toml` 的 `compose-bom` 条目 (工作区里已改好, 未提交, 随本设计对应的 task 提交).
- 事实依据: 约定插件  
  `build-logic/convention/src/main/kotlin/net/imknown/android/forefrontinfo/android/Compose.kt:21-26`  
  读三样: `compose-bom` 加 `platform()`, `compose` bundle, `compose-ui-tooling` (debugImplementation).  
  `androidx.compose` 系里不带版本号的有五个 (`gradle/toml/android.toml:74-78`):  
  `compose-ui`, `compose-ui-graphics`, `compose-ui-tooling`, `compose-ui-tooling-preview`,  
  `compose-material3`, 都由 BOM 定版; 其中四个经 `compose` bundle 进来,  
  `compose-ui-tooling` 走约定插件的 debugImplementation. bundle 里其余构件  
  (`activity-compose`, `lifecycle-*-compose`, `navigation3-*`) 各挂自己的 `version.ref`  
  (`gradle/toml/android.toml:80-90`), 与 BOM 无关 (已验证: 读两份文件).
- 核对命令 (已验证: 命令跑通): `./gradlew :app:dependencies --configuration fossDebugRuntimeClasspath`,  
  核 `androidx.compose.ui`, `foundation`, `runtime`, `animation` 落 1.13.0, `material3` 落 1.5.0.
- 失败退路: 解析结果没落到目标版本, 或 CI 四条有红, 停下问负责人, 不改 R-1 定下的工具链与范围.
- 风险: 低 (单值单点的机械改动); 行为变化在上游默认行为里, R-1 的真机目检清单覆盖.

## 设计 2: ListItem 三处迁移 (R-2)

- 事实 (已验证: 拆本机 Gradle 缓存里的 `material3-android-1.5.0-sources.jar`,  
  读 `commonMain/androidx/compose/material3/ListItem.kt`): material3 1.5.0 有两个尾随 `content` 重载.  
  非交互重载在第 260 到 297 行, 内部把 `onClick` 与 `onLongClick` 传成 null (第 288 到 289 行),  
  旁边注释写着 "there's no interaction handling"; 交互重载在第 341 行起, 第一个形参就是 `onClick`.  
  弃用重载的 `ReplaceWith` (第 121 到 137 行) 指向的就是非交互重载. 两个新重载都不带实验性注解,  
  不触发 `docs/agents-cn/07-代码规则.md` 的实验 API 报批条款.
- 决定: 三处都迁到非交互重载, 手挂的 clickable 原样保留 (链接行与版本行的 `Modifier.clickable`,  
  选项行 `SettingsChoiceRow` 的 `modifier.clickable`), `headlineContent` 的内容改成尾随 `content`,  
  `colors = ListItemDefaults.colors(containerColor = Color.Transparent)` 实参保持.
- 为什么选它 (摆开过的候选): 另一个候选是给链接行与版本行换交互重载 (第一个参数 `onClick`),  
  选项行保留手挂 clickable. 不选它的理由: 交互重载的波纹与按压反馈换成组件自己的 indication,  
  是 R-2 验收没覆盖的观感变化; 而弃用重载官方给的迁移目标就是非交互重载, 今天的行为  
  (组件无交互加手挂 clickable, 单击一次一层波纹) 在迁移后原样保留, 改动最小.
- 失败退路: 若迁移后编译报错, 或真机目检发现叠了第二层点击 (不应发生, 非交互重载内部无交互),  
  回退到弃用重载照旧 (接受暂存的弃用警告), 停下问负责人.
- 验证: `./gradlew :app:compileFossDebugKotlin --rerun` 的输出里无 `ListItem` 弃用条目; 真机按 R-2 目检.
- 观感: 新重载默认值跟官方走 (负责人的决定); `SettingsSwitchRow` 一带按旧度量写的注释  
  (`ui/settings/SettingsScreen.kt:371`) 对照 material3 1.5.0 的 `ListItemDefaults` 与 `ListItem.kt`  
  度量分支改写, 不靠人眼量.

## 设计 3: OptIn 清理 (R-3)

- 改动: 删 `ui/AppRoot.kt:142` 的 `@OptIn(ExperimentalMaterial3Api::class)`, 它的行尾注释, 与 `:11` 的 import.
- 同步改 `docs/dev/issues-cn/07-UI与无障碍/38-工具栏不随滚动隐藏.md` 的 "动手前须知" 段整段改,  
  已知四处 (第 29 到 33 行), 其余三句照原样 (第 29 到 30 行整句, 第 31 行连接句,  
  第 32 行 OptIn 引用, 第 33 行注释引用).
- 验证: 编译绿 (`./gradlew :app:compileFossDebugKotlin --rerun`); 应用源码  
  `grep -rn "ExperimentalMaterial3Api" app/src base/src binderDetector/src` 零命中.
- 失败退路: 删 `@OptIn` 后若编译报实验 API 错误 (说明还有别的实验 API 在用), 恢复注解, 停下问负责人.
- 风险: 低 (机械清理, 事实已被字节码核证: `AppBarKt` 对该注解引用为零).

## 设计 4: 滚动条换官方 (R-4)

- 改动: 删 `ui/common/ScrollBarExt.kt` 整个文件; `ui/base/list/MyModelListScreen.kt:118` 与  
  `ui/settings/SettingsScreen.kt:160` 两个调用点改为取 `listState.scrollIndicatorState` (可空),  
  `scrollBarMode.drawsScrollBar` 为真且 state 非空时挂  
  `Modifier.nonInteractiveScrollbar(state, Orientation.Vertical)`, 否则不挂;  
  两个文件里 `net.imknown.android.forefrontinfo.ui.common.nonInteractiveScrollbar` 的 import  
  (第 32 与 63 行) 换成 `androidx.compose.material3.nonInteractiveScrollbar`,  
  并新增 `androidx.compose.foundation.gestures.Orientation` 的 import (两个文件现在都没有它).
- 官方签名 (已验证: `Scrollbar.kt:84-99`): `state` 非空, `orientation` 必填, 没有 `enabled` 参数.
- 观感差异 (已验证: `Scrollbar.kt:120-148,522-532,548-556` 对 `ScrollBarExt.kt:19-22,54,66,70-72,80-85`):  
  厚度与淡出时长与旧自绘相同 (都是 4dp 与 250ms); 不同的是五处: 颜色 (官方 `outline` 的 70% alpha,  
  自绘是 `onSurfaceVariant` 的 52%), 淡出延迟 (官方 400ms, 自绘 300ms), 拇指最长占比  
  (官方 `ThumbMaxLengthFraction` 0.9, 即轨道的 90%, 自绘无上限), 轨道内缩 (官方  
  `MainAxisTrackInset` 2dp, 拇指从顶部内缩 2dp 起画, 自绘整条轨道按比例算), 端头形状  
  (官方 `drawRoundRect` 圆角, 半径为厚度一半的半圆, `Scrollbar.kt:522-532,548-556`;  
  自绘 `drawRect` 直角, `ScrollBarExt.kt:80-85`).  
  `thumbMinLength` 默认 24dp: 只在轨道短于 24dp 时不画 (`Scrollbar.kt:504-505`),  
  长列表里拇指最短 24dp, 不再随比例缩到没有 (`Scrollbar.kt:514-515`).
- 同步改的过期表述 (四个 issue, 引用给全路径):  
  `docs/dev/issues-cn/07-UI与无障碍/37-滚动条可拖动是stub.md` 里每一句提到自绘实现,  
  `ScrollBarExt.kt` 或旧调用签名的表述, 加末尾 "处理" 段 (已知五处: "今天的实现是自绘" 一节,  
  "两档今天生效" 里的旧调用签名, "自绘这半边不解决可拖动" 引用的 ScrollBarExt 注释,  
  mermaid 图里 "画自绘指示条" 的标签, "处理" 段改写为官方组件已到位并接入, 可拖动档仍未实现);  
  `docs/dev/issues-cn/07-UI与无障碍/38-工具栏不随滚动隐藏.md` 里 "动手前须知" 归 R-3,  
  "滚动容器侧同样没有接线" 里的自绘消费者描述归本条;  
  `docs/dev/issues-cn/06-性能/64-滚动条组合期订阅.md` 整条 (它第 41 到 42 行自己写着  
  "官方 API 到位后该文件整体删除", "本条是替身退役前的性能修正", 按它自己的路径收掉);  
  `docs/dev/issues-cn/01-架构与分层/04-base一词三义.md` 里提到 ScrollBarExt 的句子  
  (已知第 36, 39, 40, 42 与 69 行). 写 task 的人按已知位置逐句改, 上面三个关键词只当兜底索引  
  (它们抓不到 "处理" 段), 不按行号枚举.
- 失败退路: 负责人真机看过官方默认观感后不接受, 恢复 `ScrollBarExt.kt` 整个文件与两个调用点,  
  停下问负责人.
- 验证: 编译绿 (`./gradlew :app:compileFossDebugKotlin --rerun`); `ScrollBarExt.kt` 不存在;  
  应用源码与活 issue 文档无自绘滚动残留 (两条符号级 grep 与逐句判据照 3-tasks);  
  真机按 R-4 目检 (滑到长列表底部看拇指).
- 风险: 低 (机械替换), 官方默认观感负责人已接受.

## 设计 5: computedStateOf 约定 (R-5)

- 改动: `docs/agents-cn/07-代码规则.md` 的 代码规则 一节加一条 bullet:  
  "Compose 派生状态: 不需要缓存行为时默认 `computedStateOf`; 需要缓存时才用 `derivedStateOf`."  
  判据原文 (compose-runtime 1.13.0 sources 的 `ComputedState.kt:92-93`):  
  "A ComputedState should be preferred over a DerivedState when you don't need the caching behavior  
  that DerivedState provides."
- 补充事实: `computedStateOf` 在 1.13.0 里不是实验 API (已验证: 同文件无实验性注解),  
  不触发规则 07 的实验 API 报批条款.
- 验证: 新条目断行时除末行外带两个尾随空格, 全 ASCII 标点, 与文件现有条目的排版一致  
  (对照 R-5 验收标准).
- 失败退路: 排版核对不过就改到过为止, 无其他失败面.
- 风险: 低 (单条文档约定).

## 覆盖映射 (R- 编号 → 设计节)

| R- | 设计节 | task |
|---|---|---|
| R-1 | 设计 1 | task-1 |
| R-2 | 设计 2 | task-3 |
| R-3 | 设计 3 | task-2 |
| R-4 | 设计 4 | task-4 |
| R-5 | 设计 5 | task-5 |

## 风险分级与生疏领域

五个设计点都按机械改动处理, 不摆两案: 设计 1 是单值单点; 设计 2 的重载选择已在该节定死并给了理由  
(候选摆开同节); 设计 3 是删注解; 设计 4 是删文件换官方组件, 观感口径负责人已定; 设计 5 是单条文档约定.  
生疏领域: 没有, 但有两件还没查清的事实列在待学习清单, 分别由 R-1 与 R-2 的真机目检给出预期,  
不影响本设计的任何决定.

## 待学习清单

- Compose 1.13 懒布局的窗口预取默认策略 (R-1 真机目检时手上要有预期).
- 新 `ListItem` 重载的默认 shapes 与 contentPadding 数值 (material3 1.5.0 的 `ListItemDefaults`,  
  sources jar 在本机 Gradle 缓存里, artifact 坐标 `androidx.compose.material3:material3-android:1.5.0`).
