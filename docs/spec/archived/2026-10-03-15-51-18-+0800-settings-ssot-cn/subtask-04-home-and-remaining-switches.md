# ST-04: home-and-remaining-switches 修改计划

> 状态: living (动工时如有修订在此显式记录). 计划依据: 本目录 plan.md + issues-cn #08 / #10. 风险: 高 (Home 检测链核心 + 行为等价).

## 动工记录 (2026-10-03)

- 按本报告实施: `HomeViewModel` 构造参数 `SharedPreferences` → `SettingsStore`; `collectModels()` 经 `first()` 一次性取联网开关;  
  排序监听的手写 `OnSharedPreferenceChangeListener` (注册/注销/`onCleared` 清理) 整体删除,  
  世代计数改为收集 `settingsStore.outdatedOrderFirst`, 每次发射 (含订阅重放) 计数 +1;  
  Rule 1 (列表已落地才补) 与 Rule 2 (`onModelsLoaded` 世代比较) 机制本体逐行保持; 两个调用方传 `settingsStore.outdatedOrderFirst.value`.  
  `HomeRepository.getOutdatedTargetSdkVersionApkModel` 加参数并删 SP 直读 (仓库退化为纯函数).  
  `SettingsViewModel` 加 [Function switches] 区; `SettingsScreen` 两开关换源,  
  KDoc "Storage untouched / only reads preferences" 陈述归真, 未用 import (MyApplication / remember / edit) 清理.
- 验证: `assembleFossDebug` 绿 (EXIT=0, 唯一警告为 `LldDataSource` 既有);  
  零残留 (`MyApplication.sharedPreferences` 仅剩图绑定这一合法居所); 标点零违规.

## 目标句 (占位, 动工对照时由负责人确认或改写)

联网与排序两个开关收编进 `SettingsStore`: `HomeViewModel` 换源并删 SharedPreferences 注入, `HomeRepository` 排序参数化,  
设置页余下两行走 ViewModel; Rule 1 / Rule 2 语义逐点等价.

## 变更清单

1. `ui/home/HomeViewModel.kt`:
   - 构造参数 `sharedPreferences: SharedPreferences` → `settingsStore: SettingsStore`  
     (图内 SharedPreferences 绑定的现唯一注入消费者就是本类, 删除注入不伤及第三方).
   - `collectModels()` (`:57-59`): `allowNetwork = settingsStore.allowNetworkData.first()`, 播种在 store 构造时已完成,  
     `first()` 立即返回 (报告论证非阻塞).
   - 排序监听换源: 删 `outdatedOrderChangeListener` 与 `registerOnSharedPreferenceChangeListener` /  
     `unregisterOnSharedPreferenceChangeListener` (含 `onCleared`);  
     `outdatedOrderChanges` 世代计数改为收集 `settingsStore.outdatedOrderFirst` 流, 每次发射计数 +1 (含订阅重放的那一次, 见差值 B).
   - Rule 1 (collect 里 `modelsStateFlow.value != null` 才补) 与 Rule 2 (`onModelsLoaded` 世代比较) 的机制本体逐行保持.
   - 相邻注释 (`:69-72` "SharedPreferences is the single source of truth" 段) 归真.
2. `ui/home/repository/HomeRepository.kt`:  
   `getOutdatedTargetSdkVersionApkModel(lld)` 加参数 `orderByPackageNameFirst: Boolean`, 删 `:1121-1124` 的 SP 直读;  
   两个调用方 (`HomeViewModel.kt:253` detect 与 `:284` payload) 同批更新, 排序开关的三个触点是同一条数据, 必须一批走.
3. `ui/settings/SettingsViewModel.kt`:  
   `allowNetwork` / `outdatedOrderFirst` 状态 (首帧即存值形态) + `setAllowNetworkData(value: Boolean)` / `setOutdatedOrderFirst(value: Boolean)`.
4. `ui/settings/SettingsScreen.kt`: 余下两行读写换源:  
   `allowNetwork` / `outdatedOrderFirst` 的 `remember` 初读 (`:98-106`) 改 ViewModel 状态;  
   两个回调 (`:134-142`) 改 ViewModel 委托; 删 `allowNetworkKey` / `outdatedOrderKey` 直读;  
   相邻注释 (`:141` "no broadcast needed" 注) 归真;  
   文件头注释  
   (`:64-70` "Storage untouched:  
   the same SharedPreferences is read/written" / "this function only reads preferences") 与状态段注释 (`:75-77` "read SP once on entry,  
   local state is the source of truth afterwards") 在本子任务落地后整体为假, 一并归真.

## Rule 1 / Rule 2 换源行为差论证 (plan 验证基点点名, 报告必须显式写)

- 差值 A (同值写入): 前提先弱化: 框架契约是同值写入 "may" 触发 `OnSharedPreferenceChangeListener` (不承诺必触发), 且现状 `toggleable` 行  
  (`SettingsScreen.kt:384`) 只在翻转时写, 同值写入今天不可达. ST-01 钉的形态下, listener 回灌把存值推入 `MutableStateFlow`,  
  同值被 StateFlow 的等值合并天然折叠, 世代计数不动. 结论: 无论框架回调与否, 同值写入都不产生新发射, 折叠等价成立 (两头折叠, 差异消失).
- 差值 B (初始发射): 订阅即得一次初始发射, 措辞机制无关 (ST-01 钉的形态下来自 StateFlow 重放, `onStart` 桥接形态下行为相同). Rule 1 角:  
  `init` 块的 collect 先跑一次, `modelsStateFlow.value == null` (首 load 未落地) 时直接短路; ViewModel 存活期内只订阅一次, 无重订阅场景.  
  Rule 2 角按三种时序分别论证: 若世代 +1 **晚于**首 load 戳 (`HomeViewModel.kt:55`, 戳到 0) 而**先于**落地,  
  `onModelsLoaded` (`:271`) 的比较 (0 != 1) 命中一次, 触发一次冗余 `payloadOutdatedTargetSdkVersionApk()`, 最坏恰好一次,  
  重算结果与当前列表一致, 观感不可见; 若 +1 先于戳, 戳到 1, 比较不命中; 若 +1 晚于落地, 走 Rule 1 支路, 同样至多一次. 报告按此论证 "初始发射不引入可观感差".
- 世代计数的意义不变: 只表示 "该开关变过", `StateFlow` conflation 恰好正确 (注释原样保留).

## 验证

- `./gradlew assembleFossDebug` 绿.
- 真机: 联网开 → 刷新走在线链 (Ktor 日志可证); 联网关 → 刷新零请求; 排序开 → 列表实时重排 (Rule 1); 排序切换卡在刷新中途 → 落地后 detail 补齐 (Rule 2);  
  同值重复 toggle 无观感差; 进入设置页首帧两开关即存值; `adb shell am kill` 重启存值恢复.
- 零残留 grep: `HomeViewModel` / `HomeRepository` 零 `MyApplication.sharedPreferences`; `HomeRepository` 零  
  `function_outdated_target_order_by_package_name_first_key` 现在时引用.
