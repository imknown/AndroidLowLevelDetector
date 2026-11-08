# progress: slim-ui-layer

> **状态**: living. 更新规则见 [13-任务工作流](../../agents-cn/13-任务工作流.md) 的 进度记录 一节.

- 2026-10-25: 任务开启. plan.md 写好并自审过 (自审修掉: agents-cn 链接多算一层目录,  
  subtask-04 范围从 "四个仓库" 改成三页检测条目).
- 2026-10-25: 新上下文 subagent review 一共派发六次 (general-purpose 五次, Explore 一次),  
  全部因宿主 "model concurrency limit exceeded" 失败; 等过 120 秒, 负责人两次要求继续, 第六次在负责人  
  腾出并发后重派仍失败. 又试过把 reviewer 钉到 GLM-5.3-Flash 的自定义 agent 定义, 宿主不注册项目级  
  定义, 已删. Explore (轻模型) 同错, 判定并发上限按账号计, 换模型救不了.  
  下一步动作见下一条.
- 2026-10-25: 负责人指示先提交 plan.md. 偏差: plan.md 的新上下文 subagent review 跳过, 未跑  
  (六次派发全部败在账号级并发上限, 负责人选择直接提交); 后续每一批的 review 仍照常走 subagent.  
  下一步动作: 一次性生成 subtask-01..06 的修改计划报告, 派 subagent review 这一批;  
  届时并发上限仍未解除的话, 再报负责人定.
- 2026-10-27: 一次性生成 subtask-01..06 六份修改计划报告, 自审过一遍 (自审修掉: subtask-01 把  
  `HomeViewModel.kt:277-285` 的行数说错, subtask-05 把 `detect()` 的调用数说成 24 行).  
  报告里的事实核对时间与 commit 是 `502a6eb7`, 核对过的新事实: 测试 bundle 只有 JUnit 4,  
  `app/lint-baseline.xml` 存在, `AppGraph.kt` 随 Metro 升级改过 16 行 (与本任务无关).  
  下一步动作: 派一个 reviewer 审这六份报告, 发现报给负责人; 派之前要负责人定 reviewer 模型.
- 2026-10-27: 报告 review 的自动派发失败两次. 用的模型是 `minimax-m3.1-flash` (跑一行探针正常),  
  两次都在写入一行开工心跳之后没有下文, 也没有返回, 消耗额度但没有产出. 第一次的前因是回合被打断,  
  第二次没有打断, 同样空跑, 判定这个模型承担不了这项任务.  
  偏差: 报告批的 review 改为负责人在独立会话里跑, prompt 写在 v1 的提示词 (临时文件, 已删),  
  产出仍落 v1 的结论 (已并入本记录). 这样不占本会话额度, 负责人也能全程看见.
- 2026-10-28: 报告批的 review v1 完成 (负责人在独立会话里跑), 结果在 v1 的结论 (已并入本记录).  
  计数: A 档 8, B 档 7, C 档 14. 覆盖核对查出 5 条需求没有子任务认领: FR-7, FR-9, FR-17, NFR-1, NFR-2.  
  最重的两条: subtask-01 的候选 A 机制写错 (照它选会让首页整份列表加载不出来), 候选 B 与 plan.md 里  
  "仿 getBuildProp 渲染源级错误行" 的措辞冲突. 另有 3 处没查清 (lint 首跑是否红,  
  AGP 上 `useJUnitPlatform()` 的写法, `assembleFossDebug` 现在是否绿).  
  下一步动作: 等负责人逐条确认要修哪些项, 只改确认过的, 然后排 v2.
- 2026-10-28: 负责人逐条答复后改了一轮报告. 已改: A 档 8 条全部补上 (漏项落点, 依赖三个挂载点,  
  黄金数据加 `type` 列, CI 注释那处, 第 23 个产行入口, mapper 的 Context 来源, 06-检测条目 重写与  
  两份有序表的不变式, 屏幕五处); C 档 11 条事实错全部改正 (行号, 计数, 机制描述); 5 条需求补进覆盖表.  
  已定的方案: subtask-01 第三件选 B (一行还是两行的问题, 见 subtask-01.md 的说明, plan.md 要同批改一句),  
  JUnit 5 放 `android.toml` 而 MockK 放 `thirdParty.toml` 并解开 `libsThirdParty`, 存量四个方法迁 Jupiter  
  而仪器测试保持 JUnit 4, 黄金数据用仪器测试生成, CI 拆四步, lint 首跑能修就修修不动另立.  
  仍待负责人定: 结果类型两层还是一层, 基类 A1 还是 A2, 执行模型阶段式还是依赖图, 两个已知错例修不修,  
  stderr 加字段还是合并, `updateModelDetail` 删不删与两套 id 合不合并.  
  下一步动作: 等这六条答复, 再定要不要跑 v2 的 review.
- 2026-10-28: 本地跑了一次 CI 的四个任务 (`assembleFossDebug`, `testFossDebugUnitTest`,  
  `:base:testDebugUnitTest`, `lintFossDebug`), `BUILD SUCCESSFUL`, 16 分 25 秒 (本地无缓存, 大部分时间在下载).  
  lint 报 `no new issues`, baseline 里还压着 1 个 error, 6 个 warning, 1 个 hint.  
  三个没查清的点结掉两个: `assembleFossDebug` 是绿的, lint 在 RC 线的 AGP 上不冒新问题.  
  剩下一个 (JUnit 5 与当前 AGP 的兼容性) 要等真正引入 Jupiter 才能验.
- 2026-10-29: 六条待定项全部定完, 结论已抄进对应报告: 结果类型两层 (subtask-04); 基类走 A2,  
  `updateModelDetail` 待 grep 确认零消费者后删除, `MyModelType` 与清单 id 不合并 (subtask-06);  
  执行模型阶段式, securityPatch 本步修而 mainline 另立条目, `ShellResult` 加 `errors` 字段 (subtask-05).  
  同批把 plan.md 第 37-38 行改成 B 的措辞, 这是负责人 2026-10-29 批的范围措辞修改, 记在这里留痕.  
  临时文件 open-questions.md 的结论已抄完, 已删除.  
  同日又定三件: v2 review 走增量 (只核对 v1 的 8 条 A 与 14 条 C 是否改对, 加上六条决策有没有写走样);  
  lld 回退链放进新类 `LldLoader`; subtask-06 破例拆成两次提交 (记在这里留痕).  
  偏差: 提交从"报告写完就提交"改成"v2 修完再提交", 理由是 v2 还会再改报告, 分两次提交会把同一批文字拆开.  
  下一步动作: 负责人在新会话按 v2 的提示词 (临时文件, 已删) 跑增量 review, 结果回来再修一轮,  
  然后一次提交. subtask 的实现在这之前不开.
- 2026-10-29: 增量 review v2 回来 (结果在 v2 的结论 (已并入本记录)): v1 的 8 条 A 与 14 条 C 全部改对,  
  六条决策也写对了. 新发现 1 条 A 档 + 6 条 C 档, 都是这一轮改出来的. 已全部修完:
  - A: stderr 那件的落点漏了 `ShellDefault.kt:26`. 已补三处落点, 并明写 `ShellDefault` 本步不改  
    (零调用是故意的, 见 05-代码规则:31), 也不加 `errors` 字段.
  - C: MockK 在约定插件里要写 `findLibrary("mockk").get()`; 概念 primer 里"界面只读三个字段"改口;  
    subtask-01 里"plan.md 还要改"改成"已改好"; subtask-03 的"没人跑过"与"首跑结果未知"跟着实跑结论改;  
    subtask-05 覆盖表与风险节的"待老板定"改成已定; 三份报告的逐行判据从三列改成四列 (加 `type`).
  - 顺带把 plan.md 的决策日期从 10-29 改成 10-28, 与 subtask-01 的日期一致.
  自审: 上面每条都回代码核过行号 (ShellDefault.kt:20-21 与 :26, ShellResult 的构造点,  
  HomeRepository.kt:568 的无参构造, KotlinAndroid.kt:79 与 :93 的写法).
  待定: 要不要再跑一次 v3. 我的意见是不跑, 这一轮改动是机械修补, 且已逐条回查; 若要跑则按 delta 方式.  
  下一步动作: 负责人定 v3 跑不跑, 然后提交.
- 2026-10-29: v3 结束时的核对跑完 (结果在 v3 的结论 (已并入本记录)): 7 处修补 6 处成立, 第 7 处不成立,  
  `subtask-04.md:9` 与 `plan.md:48`/`:71` 还写着三列. 另外报了 2 条文字不一致 (决策日期, lint 引文).  
  处置: 三处三列全改成四列 (加 `type`); `subtask-03/subtask-03.md` 的 lint 引文换成产物里的原句  
  (`No errors or warnings (...)`); 日期这条复核后不再成立, `plan.md:39` 与 `subtask-01.md:69` 现在都是  
  2026-10-28.  
  机械清底: 全目录 grep `key, detail, color)` 与 `三列` 零命中; 跨文件核过 1210 行, 23 个产行入口,  
  25 行, 101 个调用点, 37 个 `get*`, 37 次追加, 四处日期, 全部一致.  
  偏差 (负责人 2026-10-29 已批): 我先前定过 "v3 若又冒出会误导实现的错, 就再跑 v4". 这一轮确实触发了,  
  但那两条缺陷属于同一个字面模式, 可以 grep 穷举, 已经清底; 再读一遍散文不会覆盖到新模式,  
  所以没提 v4.  
  残余风险与看管方式: 非字面类的矛盾 (两处语义说法不同但没有共同字串) 仍可能残留. 挂在 subtask 第 0 步  
  的覆盖核对上: 那一步若发现这类矛盾, 就补跑一次 v4, 只审出问题的那一份; 第 0 步没发现, 这份报告就按  
  已经核过的状态进实现, 后面靠编译与 diff review 抓.
  下一步动作: 负责人批这次偏差, 然后提交这批文档.
- 2026-10-29: 目录试过按单元分目录 (`plan/` 加 `subtask-NN/`, review 轮回放各自的 `review/`), 负责人看过之后  
  决定改回平铺: `plan.md`, `subtask-01..06.md`, `progress.md` 都放在 spec 目录根上.  
  三轮回 review 的提示词与结论文件一并删掉, 结论早已并入本记录, 不再作为文件留在仓库里.  
  目录改动与删除都由 `git mv` / `git rm` 完成; 文件搬回后重跑了一遍相对链接解析, 坏链 0 条.
- 2026-10-29: 规则侧一度写过两条 review 形态规则 (能按模型派就直接派; 只能给默认模型时才写临时文件,  
  用完删掉). 负责人随后决定不加这两条, 11, 13 与 `AGENTS.md` 已全部回到原样, 与提交 `fb2c0f21` 一致.  
  这次改动最后只留下两样: 目录改回平铺, 六个 review 文件删除.
- 2026-11-02: subtask-01 实现 (三件全部完成, 未提交). 负责人说 "开始" (2026-11-02, 无问题清单, 按报告自查).
  - 第 1 件: `updateModelDetail` 参数从下标改 `MyModelType`, 定位与写入落进 `modelsStateFlow.update`
    同一个原子块; `HomeViewModel.payloadOutdatedTargetSdkVersionApk` 里翻状态找下标的代码删掉,
    改成一行 `updateModelDetail(MyModelType.OutdatedTargetSdkApk, newDetail)`.
  - 第 2 件: 新增 `ui/home/model/LldSource.kt` (`ONLINE` / `OFFLINE`); `detectMode` 与
    `HomeViewModel.detect` 的形参换 `source: LldSource`, 资源 ID 只留在拼文案的 `when` 里;
    `HomeViewModel` 的 `@StringRes` 保留 (`errorMessage` 在用).
  - 第 3 件: `MountDataSource` 新增 `MountQueryResult` (`Success(mounts)` / `Failed(exitCode)`),
    `getMounts` 查 `isSuccess`; `detectSar` / `detectApex` 各加一次失败判定, 失败时渲染本条目自己的
    CRITICAL 行, 行内带 `exitCode` (仿 `PropRepository.getBuildProp` 的 `RuntimeException("exitCode=...")`
    先例, 用 `fullMessage` 展开); ViewModel 调用点改传 `mountQuery`.
  - 自审: 全仓 grep 旧签名 (`modeResId`, 下标版 `updateModelDetail`, `List<Mount>` 形参) 零残留;
    资源 ID 只剩 `when` 两处引用.
  - 验证: Gradle 三档 (`assembleFossDebug`, `testFossDebugUnitTest` + `:base:testDebugUnitTest`,
    `lintFossDebug`) 全绿, lint `no new issues` (baseline 仍压 1 error / 6 warnings / 1 hint).
  - 偏差: analyze-file 与 ILS 两档跳过. `android studio check` 报没有正在运行的 Studio 实例;
    ILS 发行版过期 (`This build of kotlin-server has expired`, 退出码 7). 这两条要负责人解锁
    (开 Studio, 或重下 ILS 发行版) 后补跑; 解锁前 review 照常走, 编译类发现已由 Gradle 三档覆盖.
  - 观察一条: `HomeViewModel` 原第 279 行裸的 `OutdatedTargetSdkApk` (无 `MyModelType` import)
    一直编译绿 (本次实证 `compileFossDebugKotlin` 通过), 引入自 `1e66c6f8`; 机制未查明,
    本次改动把它换成了显式 import + 限定名, 不再依赖这个现状.
  - 下一步动作: review 委派 (去处与模型等负责人定); analyze-file / ILS 两档等负责人解锁后补跑.
- 2026-11-02: 验证补齐与 v1 委派. 负责人手动更新 ILS 后通道恢复: 5 个改动 `.kt` 逐一跑过,
  4 个 `CLEAN`; `HomeRepository.kt` 3 条 `DestructuringDeclaration` 警告 (104, 446, 450 附近)
  经 stash 对照核实为既有, 不在本次 diff 内, 不动. analyze-file 仍欠 (Studio 未开).
  负责人定: review 走跨端 (14-跨端review), 对面用增强推理档模型, effort 默认.
  已建 spec 目录下 `review/` (带 `.gitignore` 的 `*`), 提示词 `review-v1-prompt-2158.md`,
  状态在 `review/state.md`, 发起时 HEAD `2104df7d`. v1 为 `等待报告`, 工作树随之冻结
  (记录文件除外). 实现期间的新注释暂按中文写, 提交前翻译成英语 (05-代码规则).
  下一步动作: 收 v1 报告, 按第 3 步处理发现.
- 2026-11-02: v1 收下 (reviewer: TraeCode / DeepSeek-V4.1-Flash, effort default, 跨端独立会话;
  head `2104df7d` 核对一致, 尾行齐全). 结论: 三件应改位置全部命中, 无漏改, 无超出范围改动,
  实质发现 0 条. 处置:
  - 措辞 2 条当场修: 新增注释全部翻译成英语 (BaseListViewModel, HomeRepository 三处,
    MountDataSource, LldSource); `LldSource.kt` 注释的禁用词 `兜底` (08 禁用词表在册) 改成
    直接点名 fallback 链函数名. 翻译属 05 定下的提交前机械步骤, 不触发 v2 (reviewer 已明说
    不需要下一轮).
  - 归后续单元 1 条: 挂载失败行只带 exitCode, 命令自身错误文字不可见, 归 subtask-05
    (ShellLibSu 合并 stderr 随 2b #03 一并定). 已记 review/state.md.
  - v1 报告另核实: 本地 `compileFossDebugKotlin` 在 reviewer 侧也跑过, 通过.
  下一步动作: 第 4 步负责人放行 (低风险与第 5 步并一次同意); trailer 值等负责人给
  (agent / model / effort, 12-git: 取值绝不猜); 三笔提交逐笔等同意.
- 2026-11-02: 第 4+5 步完成, subtask-01 关闭. 负责人放行; trailer 经负责人定:
  agent `WorkBuddy CN`, model `GLM-5.3-Flash`, effort `high`.
  偏差: 13 第 4 步的放行理由一句, 两次询问负责人都没留 (两次都点了"我自己写"但没带文字),
  不编造理由写入提交信息, 记在这里.
  提交三笔 (hunk 拆分: B/D 是三件共改文件, 中间态逐一编译验证, 验证时工作树要临时退到
  对应中间态, 编译吃的是工作树不是 index):
  - `4c2b795d` refactor(list): 按类型寻址 (件 1).
  - `4f2444ea` refactor(home): LldSource 枚举 (件 2).
  - 第 3 笔: 挂载读取查 isSuccess (件 3), 见下一行提交.
  review 目录按 14 报告归宿整体删除 (归后续单元的发现已搬进本记录).
  下一步动作: 下一个子任务 subtask-02 (1b 基线快照与测试栈), 等负责人说开始.
- 2026-11-03: subtask-02 实现 (测试栈接线加快照生成器, 未提交). 负责人说开始, 问题清单为空.
  - 第 0 步: 开工前复核报告事实. AGP 已从 RC 线升到 `9.5.0` 稳定版 (`android.toml:7`),  
    报告里两处 "RC 线" 改掉, 抬头留了改痕; 其余事实复核后不变.
  - 接线: `android.toml` 加 `junitJupiter` 与 `junitPlatform`,  
    `test` bundle 改成 `["junit", "junit-jupiter", "junit-platform-launcher"]`;  
    `thirdParty.toml` 加 `mockk = 1.14.3`; 解开 `ProjectExtensions.kt:25` 的 `libsThirdParty`;  
    `KotlinAndroid.kt` 的 `configureTest()` 加 `testOptions.unitTests.all { it.useJUnitPlatform() }`  
    与 `testImplementation(libsThirdParty.findLibrary("mockk").get())`.
  - 版本更正: 第一版选了 `5.12.2` / `1.12.2`, 依据是 Maven 搜索索引 (它只返回到 5.13.0-M3).  
    负责人指出 JUnit 已经到 6, 查 `maven-metadata.xml` 确认最新是 `6.1.3` (2026-08), 5.x 线也到 5.14.4.  
    改成 `6.1.3` / `6.1.3` 后单测照旧全绿: `:base` 4 个, `:app` 3 个, 依赖树里也是 6.1.3.  
    教训: 选版本要读 maven-metadata.xml, 不看搜索接口的排序结果.
  - 存量测试: `DateTimeExtTest` 四个方法迁 Jupiter 断言; 删 `app` 与 `base` 两个 `ExampleUnitTest`.
  - 偏差 (加了一件): 删掉 `app` 的模板单测后 `app` 一个单测都没有, Gradle 9 的  
    `failOnNoDiscoveredTests` 默认 true 会让 `testFossDebugUnitTest` 直接红. 新增  
    `app/src/test/.../ui/base/list/MyModelTest.kt` 三个 Jupiter 方法顶上, 内容是 `MyModel.key`  
    的两种映射与默认 `color` / `type`, 正好是快照逐行对照用的那四个字段.
  - 快照生成器: `app/src/androidTest/.../snapshot/ModelSnapshotTest.kt`. 走 `metroViewModelFactory`  
    造三个 ViewModel 直接调 `collectModels()`, 不复刻仓库调用序列; 取数前把 `allowNetworkData`  
    与 `outdatedOrderFirst` 固定为 false, 取完还原; 输出 `{page, index, key, detail, color, type}`  
    到外部文件目录的 `snapshot/three-pages.json`.
  - 规则文件: `04-架构.md` 的 "接口优先的可测试性" 改写, 去掉 "不是靠 mock 框架" 那句错话,  
    补上两套测试栈的分工 (本机单测 JUnit 5, 仪器测试 JUnit 4).
  - 验证: `testFossDebugUnitTest` 与 `:base:testDebugUnitTest` 全绿, `:base` 4 个方法全被 Jupiter  
    发现, `:app` 3 个全绿, JUnit 5 与 AGP 9.5.0 的兼容性这一条结掉; `assembleFossDebug` 与  
    `lintFossDebug` 全绿, lint `no new issues`, baseline 仍 1 error / 6 warnings / 1 hint;  
    `:app:dependencies` 里 `io.mockk:mockk:1.14.3` 与 `org.junit.jupiter:junit-jupiter:6.1.3`  
    都在 `fossDebugUnitTestRuntimeClasspath` 上.
  - 装包: 前两台设备都装不上. 第一台 HyperOS 报 `INSTALL_FAILED_USER_RESTRICTED: Install canceled by user`  
    (没有弹框, 是开关挡的, 手动 `adb install` 也一样); 第二台无线调试连接反复掉线, 装包时正好不在.  
    换到第三台 Nexus 5X (LineageOS 21 / API 34) 才成. 这台 `adb install` 报 `splice failed: EINVAL`,  
    改成 `adb push` 到 `/data/local/tmp` 再 `pm install` 才装上; Git Bash 还会把设备路径改写成 Windows  
    路径, adb 命令要带 `MSYS_NO_PATHCONV=1`.
  - 漏项 (实跑才发现): `androidx.test:runner` 不在仪器测试 classpath 上, `androidTest` bundle 只有  
    `core` 与 `ext:junit`. 表现是 `am instrument` 立刻崩在  
    `ClassNotFoundException: androidx.test.runner.AndroidJUnitRunner`. 这套仪器测试此前从没在真机上跑过,  
    所以一直没暴露. 处置: bundle 补 `test-runner = 1.7.0`, 重装测试 APK 后跑通.
  - 快照: 1345 行 (home 25 / others 42 / prop 1278), 落在 `baseline/three-pages.json`.  
    `type` 列确有区分, 第 24 行是 `OutdatedTargetSdkApk`, 其余 `General`.
  - 一致性: 连跑两次逐行对比, 行数一致, 只有 prop 页  
    `debug.tracing.battery_stats.wifi_signal_strength` 一行变了 (3 变 2, 系统实时计数器).  
    已记进 subtask-02 的验证节, subtask-04 对照时把这类实时计数器列为例外.
  - 界面核对 (报告要求的人工一步): 负责人在设备上点过, 首页 25 行, Others 42 行, 与 json 一致. 已结.
- 2026-11-04: 第 2 步 review 发起, 走跨端 (14-跨端review).  
  负责人定: 跨端独立会话, 未提交状态进 review. 已建 spec 目录下 `review/` (带 `.gitignore` 的 `*`),  
  状态在 `review/state.md`, 提示词 `review-v1-prompt-2100.md`, 发起时 HEAD `30fd613d`.  
  v1 为 `等待报告`, 工作树随之冻结 (记录文件除外).
  下一步动作: 收 v1 报告, 按第 3 步处理发现.
- 2026-11-04: review v1 收下, 两份 (TraeCode / GLM-5.3-Flash `2116`, MiniMax Code / MiniMax-M3.1-Flash `2120`).  
  HEAD 都是 `30fd613d`, 头尾齐. 结论: 实质发现 4 条, 无超出范围改动. 处置:
  - `test` bundle 还挂着 JUnit 4 (两份都报, Trae 记第三档, MiniMax 记第一档): 单测里写 JUnit 4 会编译过  
    而一个方法不跑, 与本步选方案 A 的理由冲突. 判定第一档当场修: bundle 去掉 `junit`,  
    版本目录里那条 `junit` 也一起删 (没有别的消费者). 仪器测试那一路由 `test-ext-junit` 与  
    `test-runner` 传递带入 JUnit 4, 两份报告都核过依赖树.
  - `04-架构.md` 写 "JUnit 5" 与代码里的 6.1.3 不符: 改成 "JUnit Jupiter", 不带版本线,  
    补一句单测 classpath 故意不放 JUnit 4 及其理由.
  - `06-技术栈缺口` 与 `README:330` 的测试栈现状已不成立 (JUnit Jupiter 与 MockK 现在都在,  
    app 也有真单测): 两处就地改, #06 不归档, 剩下缺的照旧列着.
  - 快照生成器只给 `allowNetworkData` 留了落定断言: 照同样写法给 `outdatedOrderFirst` 补一条.
  - 顺带: mockk `1.14.3` → `1.14.11`; `plan.md` 与 `subtask-02.md` 的 "JUnit 5" 统一成 "JUnit Jupiter";  
    行号漂移 4 处改掉; "1345 行" 改 "1345 条记录"; "第 24 行" 改 "`index` 24 那行".
  - 验证: 单测 4 + 3 全绿; 单测 classpath 上 `junit:junit:4` 命中数 0, `io.mockk:mockk:1.14.11` 在;  
    仪器测试 classpath 仍有 `junit:junit:4.13.2` 与 `androidx.test:runner:1.7.0`;  
    `assembleFossDebug` 与 `lintFossDebug` 全绿, lint `no new issues`.
  - 归后续单元 (subtask-04): 黄金数据只覆盖离线那条路 (联网路径产出的行不在快照里);  
    资源行的 key 是资源 id 数字, 资源表一动逐行 diff 会报假回归; prop 页实时计数器已在验证节记为例外.
  - 补断言这条被现实纠正了一次: 原来的写法是写完直接 `assertFalse` 读 flow, 理由写在注释里  
    ("监听器同步刷新 flow"). 实跑失败, 因为 SharedPreferences 的变更回调在非主线程发起时会被 post 到  
    主线程, 测试线程读到的是旧值. `allowNetworkData` 那条当时白过 (设备上本来就是 false).  
    改成轮询等落定 (`awaitSettled`, 上限 2 秒, 中间 `waitForIdleSync`), 重跑通过.
  - 顺带实测出快照的时效边界: 隔一天重跑 1342 行 (基线 1345), 差异 829 处. 按键拆开看是  
    `debug.tracing.*` 三行消失加 196 行内容变化 (`cache_key.*` 随机, `boot_count` 与亮度在走),  
    其余是删行导致的下标错位. 结论: 快照是时点快照, subtask-04 要接着在设备上重生成再比.
  - 设备侧重跑: 新写法在 Nexus 5X 上通过 (4.6 秒, `OK (1 test)`), 说明两项设置确实都落定;  
    拉回来的快照就是上面 "时效边界" 那一组数据 (1342 行).
  - 验证环境: 本机跑构建时遇到过一次依赖下载被截断 (构件在缓存里是完整的, 换个下载客户端同一个 URL 能拿全),  
    与仓库状态无关. 绕开之后普通构建通过, 不用 `--offline`.  
    全量验证: `assembleFossDebug` / 两个单测任务 / `lintFossDebug` 全绿, lint `no new issues`.  
    本机网络环境的具体细节按要求不写进仓库; 以后再撞到时先怀疑本机链路, 别先怀疑依赖配置.
  - 下一步动作: rev2 (delta) 已发起, 提示词 `review-v2-prompt-2218.md`, 等报告. 工作树冻结.
- 2026-11-04: review v2 收下 (两份: TraeCode / GLM-5.3-Flash `2228`, MiniMax Code / MiniMax-M3.1-Flash `2227`).  
  HEAD 都是 `30fd613d`, 头尾齐. 结论: v1 四条都修对; 新发现四条, 全修:
  - 生成器把落定等待写在 `try` 外面 (MiniMax D1): 超时抛错时 `finally` 还没进来, 设备上两项设置会留在 false,  
    下次打开 app 看到 "允许联网取数" 被关掉. 已把写设置与两次 `awaitSettled` 一起挪进 `try`.
  - 同一类过期句在另外四份 issue 文件里 (MiniMax D2 / Trae 第一档): `#43` 两处, `#01`, `#19`,  
    `#50` 的测试树分布表. 全部按现状就地改, 条目不归档.
  - subtask-04 的验收还写着 "生成器随本步改一次" 与 "逐行全等" (MiniMax D3): 按本轮实测的时效边界改写,  
    并写明生成器不用改.
  - issues `README` 修复路线那句 "现状真实测试仅 1 类 4 方法" 与 "JUnit 5": 已改.  
    0e 那一行是决定记录, 保留当时的叫法.
  - 验证: 离线全量 (`assembleFossDebug` + 两个单测任务 + `lintFossDebug` + `assembleFossDebugAndroidTest`) 全绿,  
    lint `no new issues`; 设备侧重跑 4.1 秒通过, 跑完读偏好文件  
    `function_outdated_target_order_by_package_name_key` 仍是 true, 还原路径正常.
  - 下一步动作: 发 v3 (delta), 只核这四条.
- 2026-11-04: review v3 收下 (两份: TraeCode / GLM-5.3-Flash `2327`, MiniMax Code / MiniMax-M3.1-Flash `2328`).  
  v2 四条都确认修对, 无超出范围改动. 新发现两条, 已处置:
  - subtask-04 的验收自相矛盾 (MiniMax N1): 同一节一处写 "与 `baseline/` 对照", 另一处写 "拿基线比会把  
    设备状态变化当成回归"; 例外只列两类共 33 行, 覆盖不住实测的 196 行. 已把对照物统一成  
    "同一次会话里改前取的那一份", 仓库基线降为形状参考, 例外名单补到四类, 目标句跟着改口径.
  - `#50` 那句 "随 1b 迁移到 JUnit Jupiter" 读着像模板被迁移了 (Trae 措辞): 已改写.
  - 另有一处 (subtask-02 的 concept primer 与现状表还留着核对当时的模板描述) 判为当时的记录, 不改.
  - 负责人要求的规则补充已落地: `07-文档规则` 的 "不带敏感信息" 补了本机环境一段  
    (进程名与进程号, 代理地址, 端口, 内网与回环地址, 主机名, 用户名, 设备序列号, 与本机绑定的路径),  
    `AGENTS.md` 索引那一格补 "(含本机环境)". 待负责人定: 快照里的设备 prop 值算不算本机环境信息.
  - 下一步动作: 发 v4 (delta, 极短), 只核 N1 改写, `#50` 那句, 和 07/AGENTS 的规则补充.
- 2026-11-04: review v4 收下 (两份: TraeCode / GLM-5.3-Flash `2342`, MiniMax Code / MiniMax-M3.1-Flash `2341`).  
  前两处 (subtask-04 验收改写, `#50` 那句) 确认修对. 第三处翻出真问题, 已处置:
  - **基线快照里带能定位到具体机器的值** (两份都报): prop 页九行是真实值, 不是占位,  
    包括设备序列号两处, android id 两处, ROM 构建机名, 构建账号, 蓝牙网卡地址, adb 调试端口, 设备名.  
    这份文件要进仓库, 与负责人 2026-11-04 的隐私要求正面冲突. 上一轮我把它判成 "产品输出, 不算", 判错了.
  - 处置 (不按 "给规则加例外" 处理, 那样等于把序列号写进仓库): 生成器分两遍脱敏, 先从按名字命中的行  
    取出这些值, 再把任何正文里含这些值的行一起换成 `<redacted>`; 只按 key 命中漏了两处, 序列号还藏在  
    `persist.adb.wifi.guid` 里, Others 页的 "构建主机" 那行用资源 ID 当 key. 行位置与 key 都留着.  
    提交的基线按同一规则就地改, 11 行被替换 (prop 10 + Others 1), 行数仍 1345.  
    subtask-04 对照时这些行只比 key / color / type, 两边同一个生成器, 占位一致.
  - 07 补两类边界句: 设备型号与系统版本不算本机环境 (定位设备类别, 不定位这台机器);  
    应用自己产出的数据里若带定位值, 必须在生成时换占位, 不能靠事后脱敏或一句例外绕过去.
  - 顺带: subtask-02 的例外名单说法改成与 subtask-04 一致 (原写两类, 后者写四类); `#50` 那句主语悬空改顺.
  - 设备侧已验: 重装后 1 次 `OK (1 test)`, 拉回的 json 里脱敏 11 行, 旧值 (序列号, android id,  
    ROM 构建机名, adb 端口) 残留条数 0.
  - 下一步动作: 发 v5 (只核脱敏这一处).
- 2026-11-05: 负责人往 origin 推了 `40a8ea82` (07 加一条 "怎么判一句话是不是敏感信息" 的判据:  
  能不能从这句话反推出一台具体的机器或一个人的网络环境; 代理工具, 代理进程, 代理地址,  
  带用户名或本机信息的本地保存位置一律不许写). 本地快进到该提交, 07 的两份改动合成一处:  
  该判据在前, 这轮补的派生例子 (进程名与进程号, 端口, 内网与回环地址, 主机名, 账号名, 设备序列号,  
  网卡地址, spec 记录与 review 目录受管, 落盘只写抽象一层) 与两类边界 (型号与系统版本不算;  
  产出数据带定位值的必须在生成时换占位) 接在后面, 没有另起一条.  
  这一步顺带把 v5 作废了 (HEAD 变了, 按 14 的规则报告过期).  
  下一步动作: 发 v6 (delta), 只核脱敏与 07 合并后的文本, HEAD 用 `40a8ea82`.
- 2026-11-05: review v6 收下三份 (TraeCode / GLM-5.3-Flash `1849`, MiniMax Code / MiniMax-M3.1-Flash `1855`,  
  WorkBuddy CN / Tencent Hunyuan Hy `1939`). 结论打架, 按 11 的分歧阶梯先走事实层:
  - Trae 判 "脱敏干净", Hy 判 "4 字符构建账号仍留在 11 行". 实测复现 Hy: 基线里确有 11 行正文含该账号  
    (Others 页 3 行, 加 `*.build.version.incremental` 与 `ro.build.display.id` 8 行). 事实层裁定 Hy 成立.
  - 处置 (Hy N1 / MiniMax N1 / Hy N3): 阈值由 6 降到 4, 短账号也参与匹配; 占位文案 "**未填写**" 排除,  
    免得被当成定位值连坐 233 行; 匹配改忽略大小写; 只含值的行改行内替换, 版本号这类产品事实留住;  
    key 片段补成类别级 (serialnumber, wifi.guid, hostname, adb.tls.port); 加一条自检,  
    脱敏后若还有行带这些值, 测试直接抛错.
  - Hy N2 (规则要求生成时脱敏, 但提交的基线是事后改的): 用改好的生成器在设备上重跑一份,  
    直接替换仓库里那份. 现在基线 1343 条 (home 25, others 42, prop 1276), 整行占位 12 行, 行内占位 10 行,  
    旧值与账号残留 0, 文件本身就是生成时脱敏的产物.
  - 07 收紧三处: 派生例子写成禁列并说明 "单看某一项未必定位得到机器, 凑在一起就够" (Hy N5);  
    补 "记忆里同样只留一句抽象的事实" (Hy N6 / MiniMax N3); "两类不算" 的第一类补半句  
    "混着定位值时只留产品事实, 定位值换占位" (Hy N4). 索引格改成 "不带敏感信息 (含本机环境与产出数据)" (Hy N7).
  - 下一步动作: 全量验证后发 v7 (delta), 只核这几条.
- 2026-11-05 (晚): review v7 收下三份 (TraeCode `2012`, MiniMax `2008`, WorkBuddy CN / Hy `2040`).  
  Trae 判 "六处全部到位", Hy 与 MiniMax 各自挖出生成器的结构缺口. 按 11 的事实层, 以代码读得出的为准:
  - 自检只查 "已收集的值有没有被替换", 集合外的东西它看不见 (Hy N1 / MiniMax N1). 改成两层:  
    已收集值复查 + MAC 形态扫描; 并在注释与文档里写明它看不见什么.
  - 占位排除只认简中 "未填写" (Hy N2): 改成取应用自己的本地化串 `build_not_filled`, 换语言仍成立.
  - 阈值 4 加子串替换会切碎通用词 (Hy N3): 行内替换改成按词边界匹配.
  - 文档三处口径 (Hy N6, MiniMax N3): 去掉过时的 `persist.adb.wifi.guid` 例子; Others 那 2 行写明是  
    "整条正文被行内替换覆盖"; "两端" 改成 "两份快照里占位一致".
  - 设备侧 (手机连回后补跑): 装新生成器跑一次 `OK (1 test)` 7.6 秒; 拉回 1343 条, 整行占位 12 + 行内占位 10,  
    旧值与构建账号残留 0; 与上一份基线只差 8 行, 全是设备状态 (亮度, `cache_key.*` 随机值), 结构没变.  
    已用这份替换仓库基线, 保持 "文件与生成器同版".
  - 07 的两处条文歧义由负责人 2026-11-05 裁定: 产品数据里的系统服务名与每次开机都变的进程号不算;  
    记忆两类分开记. 文本已按这两条改.
- 2026-11-05 (深夜): review v8 收下三份 (MiniMax `2105`, TraeCode `2107`, WorkBuddy CN / Hy `2142`), HEAD 仍 `40a8ea82`.  
  三份都判六处到位且基线干净, 另外报了几处结构问题, 已处置:
  - 替换按词边界, 自检却按子串 (Hy N1): 同一个值两套匹配口径, 值若成了某个更长词的子段, 替换不动而自检判残留,  
    生成器会误报抛错. 已让自检共用 `tokenRegex`; MAC 扫描也不再豁免含占位的行, 改成全表查.
  - 07 里 "后者里若带这类值" 丢了限定词 (Hy N2): 指代成了裸的 "这类值", 与上面 "进程号不算" 读法相撞.  
    已改回 "若带能定位到某台具体机器的值", 并把产品数据那句并进第二类, 去掉 "两类不算" 与新增句的数量张力 (Hy W2).
  - 记忆那句补 "凭据类材料按上一条" (Hy W3).
  - 文档与注释的边界写清 (Hy W1): 自检用同一套词边界, MAC 全表扫; subtask-02 的自检说明跟着改.
  - 进度记录停在旧状态 (Trae 第一档, Hy W4): 本条目即是补齐, 07 裁定与两次设备侧重跑都记进来了.
- 2026-11-05 (review 循环结束): 决定不再排 v9. 理由: v1 到 v8 共八轮, 代码侧的实质问题在 v6 ~ v8 已经出尽
  (短账号漏网, 生成时 vs 事后脱敏, 自检与替换口径不一致), v8 之后只剩文档口径与匹配细节, 逐一改完后
  v8 那五处我自查过: 自检与替换共用 `tokenRegex`, MAC 全表查, 07 指代改回限定说法, 记忆那句点明范围,
  记录补上; 离线全量全绿, 设备侧用新 APK 跑 `OK (1 test)`, 新自检没有误报.
  剩下三条归 subtask-04 (离线路径覆盖, 资源 id 键, 实时计数器例外), 已在本文件记着.
  以后要重开 review 的条件: 生成器的 key 片段表大改, 或 subtask-04 换设备重生成时出现新的机器定位值形态.
  八轮的走向 (报告都在 review 目录, 循环结束后删掉, 结论只留在这里):
  v1 全量: JUnit 4 残留, 04 架构版本线写错, issues 现状过期, 快照断言只覆盖一项 → 全修.
  v2 delta: 落定超时不还原设置, 四份 issue 现状过期, subtask-04 验收与时效边界冲突 → 全修.
  v3 delta: subtask-04 验收自相矛盾, #50 一句读着像模板被迁移 → 全修.
  v4 delta: 基线里有序列号等定位值 (我上一轮判错) → 生成器脱敏, 基线重做.
  v5 作废: 负责人往 origin 推了 07 判据, HEAD 变了.
  v6 delta: 4 字符构建账号仍漏 11 行 (事实层裁定) → 阈值与匹配重做, 基线重生成.
  v7 delta: 自检只查已收集值, 占位只认简中, 子串替换会切碎通用词 → 全修; 07 两处歧义请负责人裁定.
  v8 delta: 替换与自检口径不一致, 07 指代丢限定词 → 全修, 自查后结束本轮.
- 2026-11-05 (提交): subtask-02 按三笔提交 (测试栈切换 / 快照生成器与基线 / 文档与记录),  
  拆的依据是每笔都能独立构建: 构建接线与单测迁移必须同一笔 (去 JUnit 4 的同时不迁 `DateTimeExtTest`, 编译就断),
  生成器与基线一笔, 文档一笔.
- 2026-11-06: 基线不再进仓库 (负责人提出: prop 每行都是 key→value, 解析路径相同, 没必要全取;  
  看过抽样效果后又问 "不删吗"). 中间试过抽样: 按类别取 102 行 (home 25, others 42, prop 35),  
  脚本幂等, 保留整行占位 2 行与行内占位 2 行. 试完定为不存: 对照要的是 "改造前在同一次会话里  
  取一份全量", 那两份都在设备侧; 仓库里存全量 (1343 条, 11k 行) 随设备漂移, 存抽样只剩  
  "给人看形状" 一点用处, 那点用处两行样例就够. `baseline/` 目录与抽样脚本都删掉, 生成器保留.  
  生成器仍脱敏: 拉回的 json 会先落在本地临时目录, 可能被贴进文档或被误提交, 留着比事后判断便宜.  
  subtask-02 与 subtask-04 的口径已改 (形状参考改为文档里的两行样例, 对照物是同会话改前那份).
- 2026-11-06: subtask-03 实现 (CI 扩跑四步, 未提交). 负责人说开工, 问题清单为空.
  - 第 0 步复核 (代码 `bb839c00`): yaml 的 Gradle 区仍在 34-37 行; `gradle/toml/build.toml` 里 CMake 与  
    NDK 与 CI 装的两条一致; `configureFlavors()` 全仓只有 `AndroidApplicationFlavorsConventionPlugin` 一处调用;  
    `app/lint-baseline.xml` 在. 报告里三处过期措辞改掉 (抬头补复核行, 两处 "RC 线" 按现状改写), 其余事实不变.
  - yaml: `[Gradle]` 区从一步变四步, 一个任务一步 (`assembleFossDebug`, `testFossDebugUnitTest`,  
    `:base:testDebugUnitTest`, `lintFossDebug`), 每步带名字. 其余 step 与 region 注释没动.
  - 规则文件: 09 的 CI 一节改写 (四步, `base` 单列的理由, 不跑仪器测试, 不许缩检查范围, `master` 不当目标分支);  
    11 的构建命令块改成四行并换掉 `# CI 构建的目标` 那句注释.
  - 超出报告一处 (记在这里): 11 的命令块补了 `:base:testDebugUnitTest` 一行. 报告只要求改注释,  
    但本地验证清单少了它就会漏跑 `base`, 与 09 的四步对不上. 判定在范围内.
  - 验证: 四任务一次命令跑完, `BUILD SUCCESSFUL` 6 分 22 秒; lint 报 `Lint found no new issues`  
    (baseline 仍 1 error / 6 warnings / 1 hint); 两个单测任务 `--rerun` 真跑一遍  
    (app 3 个方法, base 4 个方法, 0 失败 0 跳过). 两个任务一起带 `--rerun` 时 `app` 那条第一次仍报  
    UP-TO-DATE, 单独再跑一次才真执行, 所以上面这组数字来自单独跑的那次.
  - 报给负责人的待定项 (需求正文一档): `docs/dev/issues-cn/09-构建与CI/50-CI只编译不测试.md:62` 与  
    `docs/dev/issues-cn/README.md:330` 引着旧条款原文 "CI 只构建 `assembleFossDebug`", `#50` 的修复方案  
    还写着单条命令. 09 改写之后这几处与现状冲突, 改不改请负责人定 (后来授权了, 见下面那一条).
  - 措辞更正 (负责人 2026-11-06 指出): `testFossDebugUnitTest` 不是 app 专属, 未带模块前缀的任务名从根项目跑  
    会选中所有模块里同名的任务; `:app:testFossDebugUnitTest` 才是限定到 app. 我原先在两处把它写成 "app 的单元测试".  
    事实用两条命令核过: `--dry-run` 下只选中 `:app:testFossDebugUnitTest`; `:base:tasks` 与 `:binderDetector:tasks`  
    里是 `testDebugUnitTest`, 名字不带 flavor. 已改 09 (补一段讲前缀与匹配集), 11 (两条注释),  
    subtask-03 (concept primer 补一句). CI 的命令本身没改, 仍是不带前缀的写法.
  - 需求正文就地改 (负责人 2026-11-06 说 "错了的话可以改"). 三处:
    - `issues-cn/README.md` 的 0a 行: 去掉旧条款引文, 改成 "09 的 CI 一节随 1a 改写为四步".
    - `#50` 的修复方案: 原写单条命令 (四步是候选 B, 负责人 2026-10-28 定) 与 `--continue` 那条 (属候选 C),  
      与已定方案冲突, 改成四步的 yaml; 实施细节第二条改成 GitHub Actions 前一步失败就停的默认行为;  
      "问题核心代码" 段首句标成 "1a 之前", 免得与修复方案混.
    - `#51`: "不动 'CI 只构建 `assembleFossDebug`' 的任务范围" 改成不引旧原文的说法.
    判定: 引文已被 09 改写丢下, 且 #50 的方案段落与 0a 的结论打架, 属需求正文写错.
  - 第 2 步 review 发起, 走跨端 (负责人 2026-11-06 定). 模型先不点名, 由对面 session 自己开.  
    已建 spec 目录下 `review/` (带 `.gitignore` 的 `*`), 提示词 `review-v1-prompt-2130.md`,  
    状态在 `review/state.md`, 发起时 HEAD `bb839c00`. v1 为 `等待报告`, 工作树随之冻结 (记录文件除外).  
    CI 实跑不在这轮做: 负责人 review 过之后自己去 GitHub 建 CI.
  - 下一步动作: 收 v1 报告, 按第 3 步处理发现.
- 2026-11-06: review v1 收下两份 (WorkBuddy / Tencent Hy4 `2143`, effort high; catpaw / Meituan LongCat 2.5  
  `2150`, effort default). HEAD 都是 `bb839c00`, 元数据块与尾行齐. 两份无分歧, 都判要下一轮.  
  并集六处, 全成立, 全修:
  - yaml 与 `#50` 的第二步名字 `Run app unit tests` 把不带前缀的命令写成 app 限定: 改成 `Run Foss debug unit tests`.
  - `issues-cn/README.md` 三处旧约定的下游引用: `:274` "CI 只编译", `:318` "CI 只跑 `assembleFossDebug`",  
    `:351` "一行 yaml 扩跑" 三处按四步现状重写. `:274` 的 "测试只有 base 一个真实测试类" 也在同一句里,  
    1b 之后 app 也有真单测, 一起改成 "app 与 base 各一个类"; 拆半句会留下同一句话里的第二个过期点.
  - `plan.md` 的 subtask-03 范围: `testFossDebug` 这个名字哪里都不存在, 还漏了 base, `:57` 引的旧条款已不在 09 里.  
    改成四步的实际任务名与 09 现在的说法; 独立性里的 "改一行配置" 跟着改成四步.
  - `subtask-03.md`: concept primer 的 "CI 现在只有一个步骤" 标成 "1a 之前"; 现状表里 CI 那一行补 "本步已改成四步";  
    "改的时候和三行命令一起看" 改成四行.
  - `#50`:64 把 "一个任务一步" 归给 0a 不准, 0a 只批了扩跑, 四步是候选 B 由负责人 2026-10-28 定: 删掉归属, 只留理由.  
    本文件里同一处归属 (`0a 已定四步`) 一并改.
  - `11`:10 的注释 "现在只落在 app" 容易被读成 app 专属: 改成 "现在只有 app 有这个任务".
  超出范围判定: 两份都报 0 条, 我核过一致.
  下一步动作: 发 v2 (delta), 只核这六处; 或者按低风险跳过 v2 (要负责人同意, 记偏差).
- 2026-11-06: v2 (delta) 发起. 负责人定排 v2, 不跳过.  
  提示词 `review-v2-prompt-2211.md`, 里面带 v1 两份报告的路径, 六处修复的对照表, 和 "先核 v1 六处再扫修复本身" 的顺序.  
  发起时 HEAD 仍 `bb839c00`. v2 为 `等待报告`, 工作树再次冻结 (记录文件除外).
  下一步动作: 收 v2 报告, 先看六处是否修对, 再处理新发现.
- 2026-11-06: review v2 收下两份 (WorkBuddy / Tencent Hy4 `2221`, catpaw / Meituan LongCat 2.5 `2246`),  
  HEAD 都是 `bb839c00`, 头尾齐. 六处都确认修对. 两份结论打架: Hy4 报 3 条第一档加 2 条措辞并要 v3,  
  catpaw 报 "没有新引入的错, 不需要下一轮". 按 11 的事实层逐条回原文核:
  - `README.md:274` 改成完成态与段落冲突 (Hy4 新发现 1): 那句排在 "两类会直接伤到进度" 的枚举里,  
    `#50:245` 与 `#06:179` 在索引表里仍是 "开放", yaml 也还没提交. 成立. 改回问题陈述:  
    "真实测试 `app` 与 `base` 各一个类, CI 此前只编译 (#50, #06)". 测试数那半保留 (1b 之后 app 也有真单测).
  - `plan.md:58` 的 "只改 CI 配置与清红" (新发现 2): 同一段范围行自己写着 09 随本步改写, 实际还改了 11,  
    三份需求条目与 spec 记录. 成立. 改成 "改 CI 配置与随之要改的文字, 不依赖其他步".
  - `subtask-03.md:74` 只改了下半句 (新发现 3): 上半句还引着 11 第 8 行已不存在的注释. 成立.  
    改成完成态 ("已换掉: ...") 并标出原句, 末句改成 "它和命令的行数是一起变的".
  - 措辞两条也改: 章节标题改成 "开工前核对的现状与证据", 表内两处 "本步已..." 去掉 (标题已限定时点),  
    `progress.md` 的 "面里带" 改成 "里面带".
  判定依据: catpaw 说 "没有新引入的错" 与原文不符, 属漏报; 三条按事实层核过都站得住, 不需要再走条文层.
  下一步动作: v3 (delta) 只核这三条与两处措辞; 排不排 v3 等负责人定, 跳过要记偏差.
- 2026-11-06: v3 (delta) 发起 (负责人定排, 并问能不能换全新 agent 与模型).  
  提示词 `review-v3-prompt-2251.md`, 带 v2 两份报告路径与五处对照表.  
  跨端本来就是新 session, 没有本会话记忆; 要"全新"还需对面开新会话而不是续用 v2 那个, 且避开 v1 / v2 用过的  
  Tencent Hy4 与 Meituan LongCat 2.5. 发起时 HEAD 仍 `bb839c00`. v3 为 `等待报告`, 工作树冻结.
  下一步动作: 收 v3 报告, 核五处是否修对.
- 2026-11-06: review v3 收下两份 (WorkBuddy / Tencent Hy4 `2256`, catpaw / Meituan LongCat 2.5 `2253`),  
  HEAD 都是 `bb839c00`. 两份打架: Hy4 判五处全对, 0 条新发现, 不要下一轮; catpaw 判第 1 与第 3 处没修 / 修错,  
  要 v4. 按 11 的事实层逐条核, 两家各对一半:
  - `README.md:274`: 句子与 `#50:245` / `#06:179` 的开放状态不冲突 (Hy4 对), 但读起来偏现状描述,  
    不像问题 (catpaw 对). 再走一步, 改成 "这些测试此前拦不住回归 (#50, #06)", 直接引 #50 危害栏的原意.
  - `subtask-03.md:74`: 引文清楚 (Hy4 对), 但所在小节标题是 "计划改动", 却写成 "已换掉", 时态不一致  
    (catpaw 对). 改回待改说法: "那句注释要改: 扩跑之后 CI 跑的不止是构建. 旧句是 `# CI 构建的目标 (GitHub Actions)`".
  - 措辞三条采纳两条: 现状表的时点统一交给表头 ("核对时是什么样"), 四格不再各写一遍 "核对时";  
    `:75` 的 "它" 指代含混随第 2 处重写消失. (表头已限定时点, 格内重复四遍是冗余, 四格只留事实.)
  两份都确认第 2, 4, 5 处修对, 这部分无分歧.
  下一步动作: 三条修完, 剩下的全是文本表述. v4 排不排等负责人定; 跳过要记偏差.
- 2026-11-06: v4 (delta) 发起. 我建议结束循环, 负责人要看第四轮还能挖出什么, 定排 v4.  
  提示词 `review-v4-prompt-2309.md`, 只核 v3 的两处与现状表的时点统一; 里面写明 v3 两份的打架点与发起方的裁法,  
  并要求 reviewer 只在认为裁法本身错时按第三档报. 发起时 HEAD 仍 `bb839c00`. v4 为 `等待报告`, 工作树冻结.
  下一步动作: 收 v4 报告.
- 2026-11-06: review v4 收下两份 (WorkBuddy / Tencent Hy4 `2315`, catpaw / Meituan LongCat 2.5 `2310`),  
  HEAD 都是 `bb839c00`. 两份打架: Hy4 报 2 条 (1 条第一档, 1 条第三档) 要 v5; catpaw 判三处全对, 不要下一轮.  
  事实层核, Hy4 两条都成立:
  - `subtask-03.md:75` "和命令的行数一起看": 宾语从 "命令" 换成 "行数", 数字没法看, 提醒也丢了.  
    是 v3 那次重写自己带出来的. 改成 "连那几条命令一起看".
  - `README.md:274` 的 "此前": yaml 未提交, CI 没跑过四步, 问题现在仍成立, 过去时把它写成了已解决.  
    catpaw 自己也承认 "没写参照时点", 判它不构成实质错偏松. 改成现在时: "CI 还不跑它们, 回归拦不住".
  下一步动作: 两处修完. 四轮的新发现都是上一轮修复的副产品, 已到字面级. v5 排不排等负责人定,  
  我的意见是不排, 跳过要记偏差.
- 2026-11-06: 结束循环 (负责人定). 偏差: v5 跳过, 未跑. 理由: v1 到 v4 共四轮, 每轮的新发现都是上一轮修复带出来的,  
  严重度从漏改错改降到一句中文读不通; v4 那两条已在事实层核过并逐条回查, 再排一轮只会产生新的措辞意见.  
  另一条事实: v3 / v4 的 reviewer 仍是 v1 / v2 用过的两个模型 (新开的 session), 换模型这层独立性没有增加.
  机械自查 (替代 v5 的核对):
  - 四步任务名逐字一致: `.github/workflows/android-ci.yml:35-42`, `09-构建约定.md` 的 CI 一节,  
    `11-构建与验证.md` 的命令块, `#50` 的 yaml 示例与实施细节, `issues-cn/README.md:351`.
  - 旧说法 grep (`CI 只构建`, `一行 yaml`, `Run app unit tests`, `面里`, `CI 已经跑`, 不带 UnitTest 的  
    `testFossDebug`): living 文档零残留. 命中的只有 `docs/dev/2026-10-30-agp-survey-cn/` (record),  
    本文件的历次记录, `subtask-03.md:39` 的核对时引文 (表头已限定), 和功能需求里 "界面里" 的误命中.
  - 相对链接: 改动涉及的 8 份文件 0 条坏链 (README 里两处 `[#NN](路径)` 是写法示例, 不是链接).
  下一步动作: 等负责人给提交的同意 (一笔, 9 个文件), trailer 按 12-git 由负责人给.
- 2026-11-06: 第 4 + 5 步, subtask-03 关闭. 负责人批: 删 review 目录并提交本批, 一笔 (9 个文件).  
  第 4 步那句放行理由负责人没留, 不编造理由写进提交信息, 记在这里.  
  偏差: 13 第 4 步与第 5 步并成一次同意 (低风险子任务允许).  
  trailer 经负责人定: agent `WorkBuddy CN`, model `DeepSeek-V4.1-Flash`, effort `high`.  
  review 目录 (四轮提示词与 8 份报告) 按 14 的归宿整体删除, 结论已在本文件与 `state.md` 里.
  下一步动作: 下一个子任务 subtask-04 (2a 领域化), 等负责人说开始.
- 2026-11-06: CI 实跑结果回来了 (负责人手动触发). run `Android CI #407` (id 42243375884),  
  `workflow_dispatch`, 分支 `jetpack-compose-docs`, head `081db825`, 结论 `success`, 用时 5 分 3 秒.  
  九步全绿, 四步 Gradle 各自的耗时: assemble 3 分 45 秒, app 单测 10 秒, base 单测 2 秒, lint 45 秒.  
  这一步结掉 subtask-03 验证里 "本地跑通不算, 要看 CI 实跑" 的缺口: 拆四步的配置在空缓存的 CI 上写对了,  
  lint 在 CI 上也没有冒出 baseline 之外的新问题.
  一条 notice (归后续, 不在 1a): `ubuntu-latest` 标签从 2026-10-19 起迁到 Ubuntu 26  
  (actions/runner-images#14748). 归 #51 的 CI 硬化, 本轮不动.
  需求正文 (负责人 2026-11-06 批改): `#50` 在 `issues-cn/README.md:245` 的状态改成 "已完成 (1a)",  
  正文补完成段. 连带改了三处, 因为 #50 一完成它们就成了错的或只指向已完成条目的:  
  - `README:274` 的 "CI 还不跑它们, 回归拦不住" 改成 "覆盖太薄 (#06)": CI 现在跑了.  
  - `#06` 的状态括号 "部分随行 #50" 改成 "测试基础设施已随 1b 与 #50 补齐"; 条目正文里  
    "且没有 CI 上必须通过的检查 (检查归 #50)" 与 "Turbine ... 与 #50 的 CI 检查同批" 两处跟着改.  
  - `#51` 的状态括号 "随行 #50" 去掉, 只留 "开放".
- 2026-11-07: 负责人确认接续 subtask-04 (2a 领域化), 并定下节奏: 尽可能一点一点改, 每批改动最小,  
  方便一点一点 review, 最后按最小单位一笔一笔提交 (每笔提交仍单独要负责人同意, trailer 另给).  
  第 0 步复核完成 (HEAD `372a5de7`): subtask-04 报告全部事实重核, 漂移 7 处带批注改进报告  
  (HomeRepository 1210→1245 行; 三个 private 辅助 :273/:507/:559→:281/:529/:581;  
  getOutdatedTargetSdkVersionApkModel :1105→:1141, 产出行 :1162-1167→:1202; toSupportOrNotString :1185-1191→:1221;  
  getMyString 69→71 处在 HomeRepository; toColoredMyModel 24→26 处; AppGraph 工厂 :89-92→:91),  
  其余事实不变 (23 个产行入口, 45 处 `Build.*`, `guardedMyModel` 24 处, Others 37 get*/36 guard/:210,  
  Prop 3 入口, MyModelExt 四段行号, MyApplication.kt:73, MyModelListScreen.kt:132).  
  顺带把下面 "各子任务状态" 一行修正: subtask-03 已于 2026-11-06 关闭, 原文还写着等 review.  
  下一步动作: 批次划分与 review 节奏等负责人定 (当轮回复已给三个候选), 定完开工批 1 (DetectResult 容器).
- 2026-11-07 (批 1 实现): DetectResult 容器与单测落地, 未提交. 节奏负责人定: 每批一轮 review, 批批提交.
  - 新增 `ui/base/detect/DetectResult.kt` (三支 `Found(value)` / `Missing(reason)` / `Failed(cause)`,  
    报告计划改动 1 的形状) 与 `DetectResultTest.kt` (修后 5 个方法: 三个取值断言, when 穷尽,  
    协变由第 5 个方法的显式 `DetectResult<Int>` 赋值锁定, v2 修). 纯新增, 不碰现有代码.
  - 验证: ILS 对 `DetectResult.kt` CLEAN; 对测试文件报 11 条 UNRESOLVED, 对照存量 `MyModelTest.kt`
    (已提交, Gradle 全绿) 同样报, 判定 ILS 解析不了 test 源集的依赖 (机制未查明), 已记进 10-分析细则;
    测试文件的编译类发现以 Gradle 为准. analyze-file 跳过: 没有正在运行的 Studio 实例 (同 1a 的偏差),
    要负责人开 Studio 解锁.
  - Gradle 四任务一次跑完 170 秒 `BUILD SUCCESSFUL`: `DetectResultTest` 4 个方法全被发现 0 失败
    (读 test-results xml 核过), lint `No errors or warnings`, baseline 仍 1 error / 6 warnings / 1 hint.
  - Windows 通道笔记记进 11: 不带 shell 的进程调 Gradle 入口要用 `gradlew.bat`
    (构建包装器直接调 `./gradlew` 报 WinError 193).
  - 注释按 05 暂用中文, 本批提交确认时翻英语.
  - 下一步动作: 收 v1 报告 (review 目录已架, 提示词 `review-v1-prompt-1038.md`, HEAD `372a5de7`),
    处理发现后走放行与提交; 工作树冻结 (记录文件除外).
  - reviewer 去处负责人定: 批 1 的 v1 走跨端独立会话 (对面开新 session, 模型与 effort 对面定).
- 2026-11-07 (批 1 v1 收下, 处理发现): 四份报告 (CatPaw/LongCat 2.5 high, TraeCode/DeepSeek-V4.1-Flash default,
  WorkBuddy/Hy4 default, MiniMax Code/M3.1-Flash on), HEAD 都是 `372a5de7`, 头尾齐. 合并判定:
  - 修 5 处: 
    - 协变没被测试锁住 (MiniMax 实测去 `out` 测试照样过; Hy4 RV1-01, CatPaw F-4 同源): 测试补第 5 个方法
      `missing and failed fit a concretely typed container` (显式 `DetectResult<Int>` 赋值接 Missing/Failed);
      第 4 个方法注释改成只说穷尽. 负向验证: 去 `out` 后 `:app:compileFossDebugUnitTestKotlin` 报
      `INITIALIZER_TYPE_MISMATCH` 失败, 恢复后四任务全绿, 5 方法 0 失败.
    - `subtask-04.md` 的 `toSupportOrNotString` 行号漏改 (TraeCode 与 Hy4 RV1-11 都报): 实际 `:1221-1227`,
      已补批注. 上面 2026-11-07 复核条目里 "7 处已带批注就地改" 对这一处不成立 (当时漏了), 以本条为准.
    - `toColoredMyModel` 口径注明 (MiniMax): 25 个调用点命中 26 行, 含 1 处 import.
    - `DetectResult.kt` KDoc 一句 (CatPaw F-2 措辞): "文案与颜色不在这里定" 对 Missing 不成立, 改成
      "`Missing` 的 `reason` 由仓库给, 展示文案与颜色由 mapper 定".
    - 10-分析细则的 ILS test 源集条补 base 证据 (TraeCode 指出证据只有 app 两个文件):
      base 的 `DateTimeExtTest.kt` 同样报 UNRESOLVED (16 条), 措辞维持全测试源集.
  - 驳回 1 处 (MiniMax 发现 1, 判断层): 容器落 `ui/base/detect/` 被指违背分层方向. 事实层核: 它引的报告与
    #01 原文都没有把共用容器划到 ui 外 (#01 自己的领域对象落点示例 `ui/home/model/` 就在 ui 下);
    "63 个入口返回值按错约定走" 不成立, 各条目自己的类型落各自功能包, 共用容器只有这一个文件.
    维持 `ui/base/detect/`, 与 `ui/base/list` (MyModel) 的共用代码惯例一致. 已在回复里给老板一行, 可否决.
  - 归后续单元 4 处 (已记 `state.md`): 批 2 薄 guard 定 `CancellationException` 走法 (仓库 guard 在
    `MyModelExt.kt:154` rethrow, CatPaw F-1); 批 3 开工前定 `Missing.reason` 形状 (MiniMax 发现 3);
    mapper 批照报告口径执行 Missing 落未知 (CatPaw F-3, 与 FR-3 不冲突); 收尾批回答 `MountQueryResult`
    要不要并进 `DetectResult` 家族 (Hy4 RV1-06).
  - 超出范围判定 (四份都单列了同四份文档): 提交拆法采纳 "第 0 步复核批注一笔, 批 1 两个代码文件一笔,
    10/11 通道笔记一笔"; 驳回 MiniMax 的 "通道笔记收进批 1" (判断层): 通道笔记是验证学习, 不是批 1 交付物,
    与老板每批最小单位的规矩一致.
  - 下一步动作: 发 v2 (delta), 提示词 `review-v2-prompt-1148.md`, 只核五处修复与修复本身的新错;
    HEAD 仍 `372a5de7`, 工作树冻结 (记录文件除外).
- 2026-11-07 (批 1 v2 收下, 处理发现): 三份报告 (TraeCode/DeepSeek-V4.1-Flash default, WorkBuddy/Hy4 default,
  CatPaw/LongCat 2.5 high), HEAD 都是 `372a5de7`, 头尾齐. 三家一致: 五处修复全判修对;
  MiniMax 发现 1 的驳回被三家独立核过, 都同意维持; 实质新发现 0 条. 新增 3 条措辞, 当场修完:
  - `DetectResultTest.kt:7` 类注释 "装进任何条目类型的容器" 比测试证明的强 (CatPaw N-1, Hy4 RV2-01):
    改成 "装进有具体条目类型的容器".
  - `progress.md` 批 1 实现条还写 "4 个方法...与协变" (CatPaw N-2/N-3, Hy4 RV2-02): 就地改成 5 个方法,
    协变记在第 5 个方法上, 标 "v2 修".
  - `subtask-04.md` 风险节与现状表的 `toColoredMyModel` 口径不齐 (Hy4 RV2-03): 风险节改成
    "25 处调用点 (命中 26 行, 含 1 处 import)".
  - 归后续补 1 条 (Hy4 v2): 文档批把共用容器落哪个包写清 (`#01:86` 只管 mapper 落点).
  - 下一步动作: v3 排不排等负责人定 (三家都明说不再要求多一轮, 三处措辞已修完; 我的意见是不排, 跳过记偏差);
    之后批 1 放行, 注释翻英语, 三笔提交逐笔等同意.
- 2026-11-07 (批 1 放行, 提交中): v3 跳过 (负责人定, 偏差): 三家明说不再要求多一轮, 三处措辞已修完且字面级可验证.
  第 4 步与第 5 步并一次同意 (低风险子任务先例); 放行理由一句负责人未留, 不编造, 记此.
  新增注释已翻英语 (05), `DetectResult.kt` ILS 复验 CLEAN. 提交拆三笔逐笔等同意:
  第 0 步复核批注与进度记录 / 批 1 两个代码文件 / 10 与 11 通道笔记. trailer 经负责人定:
  agent ZCode, model GLM-5.3-Flash, effort max (三笔共用).
- 2026-11-07 (批 1 提交完成, 关闭): 三笔落地, 工作树净:
  `f1cc8757` docs(spec) 复核批注; `9e2ea131` feat(detect) 容器与单测; `18c75daf` docs(agents) 通道笔记.
  review 目录按 14 的归宿待负责人点头后整体删除; 归后续 5 条已在本文件 (v1/v2 两条) 与 state.md.
  下一步动作: 批 2 (mapper 基建 + 首条 Home 条目端到端), 等负责人说开始; 改前快照要在同一会话取, 开工先连设备 (Nexus 5X).
- 2026-11-07 (批 2 前置: Robolectric spike 打通): 单测手段经负责人定 Robolectric 为主 (场景规则已收进 04-架构);
  因三重未知 (AGP 9.5 组合, 桥接件 0.9.0 停在 2024-11, JUnit 平台 6 对 1.x), 负责人定 "验证 spike, 红了就回退".
  - 接线: `thirdParty.toml` 加 robolectric 与 apter 桥接两条, `configureTest()` 给 `isIncludeAndroidResources`
    与三个测试 JVM 开关. 首跑: 桥接和原生运行时都起来了, 但两个 spike 测试死于
    `FileDescriptorInterceptor cannot access SharedSecrets` (新 JDK 模块封装), `--add-opens` + `--enable-native-access` 修掉.
  - 第二层: `RuntimeEnvironment.getApplication()` 返回 null. 诊断跑 (类加载器指纹) 定位: 测试类没被路由进沙箱,
    全局 LauncherInterceptor 没点火. 读上游 gradle-plugin 源码找到缺失开关
    `-Djunit.platform.launcher.interceptors.enabled=true`, 补上后指纹全对 (application 真实创建, 测试类进沙箱).
  - 定论: 正式断言两测试 + 存量 8 测试全绿 (10/10). 桥接件在本仓库可用 (平台 6 + AGP 9.5 + 本机 JDK),
    `RobolectricSpikeTest` 留作常驻冒烟. experimental 采纳 (负责人批的 spike 路线) 记偏差于此.
  - 偏差补充: Robolectric 的 JUnit 4 传递依赖因此上了单测 classpath, 04-架构 的 "故意不放" 条按现状改写
    (没有引擎跑它, 意图不变).
  - 下一步动作: 批 2 主体 (改前快照 → mapper 基建 + detectDeveloperOptions 迁移 + 逐条单测), 等设备连上;
    review 轮把 spike 接线与迁移合为一批 (两笔提交: 接线 / 迁移).
- 2026-11-07 (批 2 实现: 接线 + detectDeveloperOptions 领域化, 未提交). 负责人插上设备后按顺序走:
  - 改前快照: 双 APK 出包, push + pm install (本机 splice 前科), 仪器测试 `OK (1 test)`, 拉回 1344 行
    (本地临时目录, 不进仓库). 生成器的真包是 `net.imknown.android.forefrontinfo.snapshot` (androidTest 根, 不带 ui).
  - 迁移: `AppGraph` 加 `homeRepository` 访问器 (`settingsStore` 先例, 逐条单测要生产同款实例);
    `DetectResult` 加 `detectResultCatching` 薄 guard (CancellationException 一律 rethrow);
    新增 `ui/home/mapper/HomeModelMapper` (`@Inject`, 注入 `MyApplication`): `toDeveloperOptionsModel`
    三支 (两态行 / 未知行 / 失败行), 与 `toColoredMyModel` 两态重载和 `toErrorMyModel` 逐行为等价复刻;
    `detectDeveloperOptions` 返回 `DetectResult<Boolean>` (getInt 失败转 Failed, 不抛, 05 约定);
    `HomeViewModel` 注入 mapper, 调用点 guard + map.
  - 单测: `DetectDeveloperOptionsTest` 三个领域契约 (关=true, 开=false, 缺省回退=true), Robolectric + 真图
    (lazy 等扩展先建好应用). 单测 13/13 (5 + 2 + 3 + 3).
  - 验证: ILS 五文件, 四 CLEAN + `HomeRepository` 3 条既有 DestructuringDeclaration (行号漂 4 行, 与本次无关);
    lint `no new issues` (baseline 仍 1 error / 6 warnings / 1 hint); base 4 方法绿.
  - 黄金数据: 改后重装重生成, 1344 = 1344 行, 差异 7 行全是已知活值类 (6 行 `cache_key.*` 每进程随机,
    1 行 `debug.tracing.battery_stats.wifi_signal_strength` 计数器 3→2, 与 subtask-02 记过的同一行);
    开发者选项行四列逐字节等价. 对照物是本会话改前那份, 未进仓库.
  - 手工步已过 (负责人 2026-11-07 实机看过, "没什么问题"): 三页浏览与断网 lld 行均正常. 报告要求的这一步已结.
  - 下一步动作: 收 v1 报告 (review 目录已架, 提示词 `review-v1-prompt-1415.md`, HEAD `18c75daf`),
    处理发现后放行与两笔提交 (接线 / 迁移), 提交前注释翻英语; 工作树冻结 (记录文件除外).
- 2026-11-07 (批 2 v1 收下, 处理发现): 三份报告 (WorkBuddy/Hy4 default, WorkBuddy/DeepSeek-V4.1-Flash default,
  CatPaw/LongCat 2.5 default), HEAD 都是 `18c75daf`, 头尾齐. 三家打架: Hy4 六条, DeepSeek 六条, CatPaw 判零条实质.
  按 11 的阶梯事实层逐条核, 合并判定:
  - 修 9 处 (实质三 + 文档措辞一批):
    - guard 覆盖面 (Hy4 D2 / DeepSeek F1, 事实层成立: 旧 guard 包取数加翻译, 新的只包取数, `startLoad` 协程无 catch,
      mapper 逃逸即整页失败): 重构为链式: mapper 收 `detect: () -> DetectResult<Boolean>`, try 包整条链,
      调用点无 try 一行; `detectResultCatching` 失去全部消费者, 从 `DetectResult.kt` 删除 (从未提交, 工作树内增删).
    - 失败路径丢 DEBUG 打印 (Hy4 D6 / DeepSeek F2, FR-3 AC2 原文核过: "细节进日志供排查"): mapper catch 补 printStackTrace.
    - guard 契约无测试 (Hy4 D3): 新增 `HomeModelMapperTest` 四条 (意外异常降级失败行, 取消 rethrow, Failed 分支,
      两态颜色); 单测 13→17.
    - 05 与薄 guard 冲突无记录 (Hy4 D1 / DeepSeek F3): 05 的 UI 禁令条补例外句 (过渡结构, 落 UI 包翻译层, 引擎落地收回);
      代码按已批准的报告没动, 规则补记录, 已在回复里给老板一行可否决.
    - 04-架构 三处: 场景表改成与 subtask-04 裁量标准一致 (Hy4 D5: 普通系统属性归 Robolectric, 厂商改过的具体值归探针),
      接线段补 isIncludeAndroidResources (DeepSeek F4), "兜底" 禁用词与 "假环境足够真" 措辞 (DeepSeek F6).
    - 计数漂移 (DeepSeek F5): MyModelExt 注释去数字, subtask-04 三处计数带批注更新.
    - 措辞一批: toml 与 KotlinAndroid 的过期 "spike 定去留" 句, AppGraph 访问器注释自相矛盾句, import 归位, spike 去掉多余强转.
  - 驳回 (事实层): CatPaw 对 guard 覆盖面与 DEBUG 打印的 "不算发现" 判定; 其余六项 "核过不算" 的核对成立
    (依赖树, putString(null) 行为, @SingleIn 等都核过对).
  - 发起方自查: assertThrows lambda 触发 RETURN_VALUE_NOT_USED_COERCION 编译警告, `val _ =` 修掉.
  - 验证: ILS 五文件 CLEAN; 单测 17/17; lint 与 base 绿; 黄金数据重做 1344=1344, 差异 6 行全是 cache_key.* 随机值,
    开发者选项行四列逐字节等价 (链式 guard 下).
  - 归后续补一句 (Hy4 D4): `06-检测条目` 混合状态已记, 2a 期间的新条目照新模式写, 不照旧流程.
  - 下一步动作: 发 v2 (delta), 提示词 `review-v2-prompt-1500.md`, 核九处修复; 工作树冻结 (记录文件除外).
- 2026-11-07 (批 2 v2 收下, 处理发现): 两份报告 (WorkBuddy/Hy4 default, Trae/DeepSeek-V4.1-Flash default),
  HEAD 都是 `18c75daf`, 头尾齐. 判决高度一致: 九处修复八处修对, 唯一实质发现是修复 2 只修了一半.
  - N1 (实质, 两家同判第一档): 读失败走仓库转 `Failed` 的正常路径, 不经过 mapper 的 catch,
    DEBUG 打印在主路径上仍是空的, FR-3 AC2 只对没预料到的异常成立. 修法取单点: 打印从 catch 撤出,
    移进 `failureRow` 本体 (两条失败路径必经, 每条失败打一次, 无重复), KDoc 同步.
  - N2 (措辞): guard 位置三处说法不一致, 05 例外句的理由改成 "负责人批准的过渡结构, 只限这一层",
    并清掉 "落在 / 落地" 两个禁用词; `HomeRepository` 注释改成 "mapper 的链式薄 guard";
    `subtask-04.md` 计划改动 4 落点下补实施注记.
  - N3 (风格): `HomeModelMapper.kt` 的 import 归位字母序.
  - 归后续评估一致 (06 混合状态建议连新条目写法一起进 06, 已记 state.md 的归后续清单).
  - 验证: ILS 两文件 CLEAN (HomeRepository 3 条既有警告), 单测 17/17, lint `no new issues`, base 绿.
    设备黄金对照本轮跳过 (只动 DEBUG 打印位置, 行输出路径未变, 等价证明沿用上一轮), 记偏差于此.
  - 下一步动作: 发 v3 (delta), 提示词 `review-v3-prompt-1553.md`, 只核三处修复; 工作树冻结 (记录文件除外).
- 2026-11-07 (批 2 v3 收下, 处理发现): 两份报告 (Trae/DeepSeek-V4.1-Flash default, WorkBuddy/Hy4 default),
  HEAD 都是 `18c75daf`, 头尾齐. 判决: N1 修对 (两家一致: 打印单点盖两条路径, 无重复, FR-3 AC2 主路径成立,
  连 xml 时间戳晚于修复时间都核了), N3 修对, N2 前两处修对.
  - F1 (实质, Hy4 抓的): 本小姐给 `subtask-04.md` 接的括号注记违反 07 的 "原句不动接括号当更正" 禁令与 08 的
    括号补丁句式, 且 "按 v1 review 定" 指向注定删除的 review 目录, 不是稳定引用. Trae 未报, Hy4 报, 事实层核:
    07/08 原文在, 成立. 修法照 Hy4: 注记整段删, 原句直接重写成与代码一致的说法 (guard 现在写在 HomeModelMapper
    的链式接口里, 包住取数加翻译整条链), "调用侧那层" 改 "mapper 里这层", 阶段图 S2/S3 两个节点同步.
  - F2 (措辞): 05 例外句删去多余的 "不写在 ViewModel 里" 半句 (禁令划的是整个 UI 层, 豁免理由已由前半句承担).
  - F3 (次要观察, 记录不修): 打印行为没有自动断言 (断言打印要改接 System.err), 归后续引擎批一并看.
  - 设备黄金对照跳过: 两家独立核过跳过成立 (failureRow 三字段逐字未变, 打印只进 stderr), 不报发现.
  - 验证: 本轮修复纯文档 (subtask-04.md, 05-代码规则.md), 构建档按 11 跳过; 代码侧沿用 v3 (17/17, lint, ILS).
  - 下一步动作: 发 v4 (delta), 提示词 `review-v4-prompt-1607.md`, 只核 F1 重写与 F2 修剪; 两家已预告下一轮
    若只剩措辞按停止条件停; 工作树冻结 (记录文件除外).
- 2026-11-07 (批 2 v4 收下, 处理发现): 两份报告 (Trae/DeepSeek-V4.1-Flash default, WorkBuddy/Hy4 default),
  HEAD 都是 `18c75daf`, 头尾齐. 两处修复全判修对 (F1 的注记三条理由逐条消失被核, 连尾随空格都在), F3 归档与
  构建跳过两项声明都成立; Trae 判批 2 可进放行, Hy4 报两条新发现:
  - N1 (实质): 阶段图 S1 节点还写 `guardedMyModel<br>HomeRepository 24 处`, 与正文 :90 的 23 处对不上
    (批 2 迁走 1 个时正文改了, 图没跟着). 08:250 明文图文对不上就地改. 修: S1 数字 24 → 23.
  - N2 (措辞): guard 所在层三种说法并存. 修: 归并成两套各有分工的词: guard 所在说 "UI 侧" 与
    "HomeModelMapper 链式接口" (:91 改 "必须挪出仓库, 由 UI 侧持有", :97 图前引句与 :124 虚线箭头改
    "挪到 UI 侧", 05 例外句改 "UI 侧的薄 guard"); 图里 c1/c2/c3 的 "调用侧" 指调用者 (ViewModel), 语义准确, 保留.
  - 验证: 纯文档改动, 构建档按 11 跳过; 代码侧沿用 (17/17, lint, ILS, 设备等价).
  - 下一步动作: 发 v5 (delta), 提示词 `review-v5-prompt-1616.md`, 只核这两处; Hy4 已预告那一轮若只剩措辞即停;
    工作树冻结 (记录文件除外).
- 2026-11-07 (批 2 v5 收下, review 循环结束): 两份报告 (Trae/DeepSeek-V4.1-Flash default, WorkBuddy/Hy4 default),
  HEAD 都是 `18c75daf`, 头尾齐. 两处修复全判修对: 图, 正文, 代码三处都是 23 (口径注记留在正文); 说法归并被认可,
  c 节点保留正确. 剩余只有措辞, 两家都判停止条件到, 不再排轮:
  - W1 (Trae): 节标题 "失败隔离挪到调用侧" → "失败隔离挪到 UI 侧" (plan.md 无引用, 不带动别处). 已改.
  - W2 (Trae): 列表项末行 :95 的两个尾随空格删除. 已改.
  - Hy4 N2: mapper KDoc 里的 "调用侧" 说法, 按其判定随提交前的注释翻译处理 (05 例外), 翻译时用归并后的说法. 已落实.
  - 注释翻译完成 (05 的提交前机械步骤): mapper, 三个测试文件, KotlinAndroid, thirdParty.toml, AppGraph,
    HomeRepository 的实施期中文注释全翻英语; 触及文件 grep 中文字符 0; ILS mapper CLEAN.
  - 下一步动作: 第 4 步放行与第 5 步提交 (低风险并一次同意的先例), 两笔 (接线 / 迁移), trailer 等负责人;
    review 目录按 14 删, 待负责人点头.
- 2026-11-07 (批 2 提交完成, 关闭): 负责人放行 (放行理由一句未留, 不编造, 记此), trailer 两笔同
  (agent ZCode, model GLM-5.3-Flash, effort max):
  `033dbd7b` build(test) 接线四件; `cf990ba7` refactor(home) 领域化八件. review 目录按 14 整体删除,
  归后续清单已在本文件与 state.md. 工作树剩 progress 与 subtask-04 的批 2 收尾记录, 随下一笔提交入库.
  批 2 全程: 前置 spike 一轮, 实现, v1-v5 五轮跨端 review 共 12 份报告, 实质发现全修
  (guard 覆盖面重构, DEBUG 打印单点化, 契约测试, 05 例外, 04 裁量表, 图文同步), 手工步与黄金等价过.
  下一步动作: 批 3 (Home 其余 22 条分组迁移), 开工前按组给 `Build.*` 逐项裁量表等负责人批; 等负责人说开始.
- 2026-11-07 (批 3 裁量表批 + 实现): 负责人读完全部 22 条的取数方式后批了全表裁量与批次划分
  (批 3 属性/Settings 组 5 条, 批 4 lld/Build 组 5 条, 批 5 混合组 4 条, 均 Robolectric;
  批 6 shell/挂载组 5 条, 仪器; 批 7 混合 3 条: encryption Robolectric, webview 与全量包列表仪器).
  本批五条实现, 未提交:
  - 领域类型: `ui/home/model/AbState` (四字段) 与 `DynamicPartitionsState` (两字段), 照 LldSource 先例.
  - mapper: 新增共用骨架 `guardedEntry` (onFound 末位参数, 尾随 lambda 绑它; onMissing 默认 unknownRow),
    toDeveloperOptionsModel 收编 (行为不变), 五个新翻译方法逐行等价复刻 (含 dsu 的 else 落平台
    unknownName 一处原样).
  - 仓库: 五方法返回 DetectResult (Failed 不抛), 删 translateDisabled/translateEnabled (零调用者),
    注释掉的属性探针随之移除 (detectAb 的 5 行与 detectDynamicPartitions 的 superPartition 两组, git 可考古);
    toSupportOrNotString 仍被五个调用点用 (sar, treble, vndk, apex, toybox), 保留.
  - ViewModel: 六个调用点全部经 mapper, 行顺序未动.
  - 实施中自查修掉三件: 尾随 lambda 误绑 onMissing (onFound 挪到参数表末位), 数据类 lambda 参数的
    按名解构建议 (两处改解构参数, 恰好全字段使用), `android.os.SystemProperties` 是 hide 类单测编译
    解析不了 (测试只用 ShadowSystemProperties 静态方法, 不 import).
  - 验证: ILS 五文件 CLEAN; 单测 33/33 (17 + 本批 16); lint 无新问题; base 绿.
  - 黄金数据: 1344 = 1344, 差异 6 行全是 cache_key.* 随机值, 本批五条行四列逐字节等价.
  - 下一步动作: 收 v1 报告 (review 目录已架, 提示词 `review-v1-prompt-1717.md`, HEAD `cf990ba7`),
    处理发现后放行与提交 (一笔, 本批十件); 工作树冻结 (记录文件除外).
- 2026-11-07 (批 3 v1 收下, 处理发现): 两份报告 (WorkBuddy/DeepSeek-V4.1-Flash default, WorkBuddy/claude-opus-6
  default, 新厂商上桌), HEAD 都是 `cf990ba7`, 头尾齐. 合并判定:
  - 修 (实质/机械): 
    - 计数过期 (两家同报第一档): subtask-04.md 现状表, 正文, 图 S1, 风险节全部按 HEAD 重算带批注
      (1199 行; 产行入口 23 中 5 条返回领域对象; 辅助 :285/:504/:556; getMyString 96/66; Build 39;
      toColoredMyModel 19/20 行; toSupportOrNotString :1185-1191; guardedMyModel 18).
    - em dash (两家同报机械违规): mapper guardedEntry 注释的 `—` 改逗号. 本小姐自己立的规矩自己踩, 记此.
    - KDoc 说错 guard 位置 (DeepSeek F-1): toDeveloperOptionsModel 的 KDoc 删 guard 段, 解释单点回
      guardedEntry 并补引擎句.
    - AbState.slotSuffix 装本地化文案 (DeepSeek F-4, 取修法一不改判): 字段改 String?; 新增
      PropertyReader.getStringOrNull (缺席 null, 空串为 IProperty 层缺席标记, 加法不动存量 33 调用点);
      detectAb 改用之, enabled 第三或项改 isNullOrEmpty; mapper null → build_not_filled 输出逐字不变.
    - 测试名声称的分支没测 (DeepSeek F-2): DetectAbTest 补第六例 (只摆 retrofit 期望 false).
  - 顺手 (两家提示): DetectAdbTest putInt 收 helper; DetectAdbAuthenticationTest 补缺席用例;
    progress 两处数字更正 (toSupportOrNotString 五个调用点含 vndk; 注释探针 = detectAb 5 行 +
    dynamicPartitions 的 superPartition 组, Claude U2).
  - 归后续 (DeepSeek F-3): isPropertyValueNotEmpty 的判据仍是本地化文案 (#42 遗留): 修法要动
    PropertyReader 读契约 (33 调用点), 无批次认领; 覆盖核对表 #42 的 "是" 加此限定. 记 state.md.
  - 观察不判漏 (Claude F4): 五个新翻译方法与 Failed 路径无 CI 断言, 沿用批 2 分工 (领域对象断言 +
    黄金数据 + 仪器), 记录在案.
  - 超出范围判定 (Claude U1): subtask-04.md 未提交 diff 是批 2 收尾记录, 按既定随本批入库, 收进.
  - 验证: ILS 四文件 CLEAN; 单测 35/35 (补两例); lint 无新问题; 黄金复验 1344=1344, 五条行逐字节等价
    (nullable 输出与占位路径逐字不变).
  - 下一步动作: 发 v2 (delta), 提示词 `review-v2-prompt-1757.md`, 核七组修复; 工作树冻结 (记录文件除外).
- 2026-11-07 (批 3 v2 收下, 处理发现, review 循环结束): 两份报告 (WorkBuddy/DeepSeek-V4.1-Flash default,
  WorkBuddy/claude-opus-6 default), HEAD 都是 `cf990ba7`, 头尾齐. 七组修复五组全对, 两组有尾:
  - 组 1 (计数): 数字全对, 但头部复核行还指 `372a5de7`, 覆盖核对表 #42 的格没改 (两家各抓一头). 已修:
    头部补 "批 2 与批 3 各自按当时的树重算过计数" 一句, "事实漂移/落地改动" 换 "过期/实施改动";
    #42 格照 #63 写法改 "部分是", 留给后续列写明 isPropertyValueNotEmpty 两处与 PropertyReader 契约归后续.
  - N1 / V2-3 (实质, Claude 抓的): getStringOrNull 把缺席, 空值, 读失败三种取值并成 null, 缺席路径输出
    逐字不变, 但空值与读失败的 detail 从旧文案 (空串 / result_not_supported) 变成 build_not_filled 占位.
    按 02 逐条摆开三案 (接受+记录 / catch 返回旧文案 / 读失败升级失败行), 负责人定 **接受新行为, 记录例外**:
    逐行等价的例外记此: slot 属性的空值与读失败两种取值, detail 由旧文案变为 build_not_filled 占位;
    两条边只在 A/B 设备且空值/读失败时可达, 黄金对照够不到; 领域对象不装文案的代价, 与实时计数器例外同类.
    配套: PropertyReader 的 getStringOrNull 注释改说三种退化并成 null (原句 "same semantics" 不实),
    类注释补例外句; AbState KDoc 补 "或读失败"; mapper 的 "When the engine lands" 改 "absorbs this
    scaffold" (翻译腔规避).
  - W2: progress 里 "33 调用点" 补口径: 类注释自称 33, 实测 `propertyReader.` 28 行 (HR 24 + 三个
    others datasource 4); 本文按类注释口径引, 差异记此.
  - 判定记档: state.md 已载 isPropertyValueNotEmpty 归后续; Claude 要求的 "判定理由留档" 即本条与 state.md.
  - 两家都判: 剩余全是不重要措辞与记录问题, 按停止条件停, 不再排轮. 循环结束.
  - 下一步动作: 第 4 步放行与第 5 步提交 (一笔, 代码十一件 + 两份 spec 记录), trailer 等负责人;
    review 目录按 14 删, 待负责人点头; 工作树冻结 (记录文件除外).
- 2026-11-07 (批 3 提交, 重排, 关闭): 先按提案一笔提交 (`18dd8ba7`, 十三件); 负责人随后问能否拆细,
  并定 **重排并定成规矩**. 因 `18dd8ba7` 未推 origin, 用备份分支 + `reset --hard` 重建十个中间态重拼,
  每态单独编译验证, 末态与备份对账只剩预期内的 12-git 规矩一笔; 重排走样的两处 (PropertyReader 类注释的
  v2 例外句, mapper 五方法顺序) 由 `3e95da05` 恢复; 全量测试 35/35 在重排树上复跑绿, 备份分支已删.
  提交串 (老 → 新): `b9c64d52` 骨架收编, `315d8354` getStringOrNull, `396cc771` ab, `b4a724dc`
  dynamicPartitions, `48de2244` dsu, `f07e4d10` adb, `1327d047` adbAuthentication, `439ef915`
  translate 退休, `e1d2317e` spec 记录, `2f8d91dc` 12-git 规矩 (+ 末笔恢复走样两处的 `3e95da05`).
  规矩落 12-git: 提交按最小可独立编译与验证的单元拆, 逐条迁移每条目一笔, 共用骨架与纯新增辅助各成一笔,
  spec 记录随批内最后一笔入库. 本条目与重排记录随批 4 首笔入库.
  下一步动作: 批 4 (lld/Build 组五条: mode, android, sdkExtension, performanceClass, kernel), 等负责人说开始.
- 2026-11-07 (批 4 节奏修正 + 实现): 负责人修正节奏: **同类型条目一批改完, 一起走 v1/v2/v3 多轮 review,
  提交时按条目拆细**; 12-git 的规矩句照此改写 (原句的 "实现, 验证, 提交, 再下一个" 顺序作废). 批 4 按此开工:
  - 领域类型五个: `LldStatusState` (version/source/errors, errors 是 ViewModel 已渲染的失败消息, 原样携带),
    `AndroidInfoState` (含 AndroidReferenceRow 与 AndroidCurrency 枚举; version 为 known 修正后的裸值,
    preview 后缀与 Go 标记属文案归 mapper), `SdkExtensionState`, `PerformanceClassInfo` (报告点名的四态
    之上补 Outdated 一态: class 非零又不等于 SDK/SDK-1 的分支真实存在), `KernelState` (含 KernelMatch;
    match 循环保持旧的 "最后匹配者胜" 语义).
  - 仓库五方法返回领域对象; detectKernel 的 `Version(checkNotNull(...))` 显式化: os.version 为 null 时
    旧路径其实不失败 (versioncompare 1.5.0 单参构造不抛异常, 静默按 0.0 比较且 detail 渲染字面 null,
    字节码与公开文档两头核实), 新路径显式走 Failed. 这条逐行等价例外由负责人 2026-11-07 定为接受新行为
    并记录 (批 3 slot 例外的同类), null 路径有领域断言钉住; toSupportOrNotString 剩
    sar/treble/apex/toybox/vndk 五个调用点保留.
  - mapper: 补 `coloredRow` 的颜色重载 (老 toColoredMyModel 本有两个重载, 本小姐批 2 只搬了 Boolean 那个,
    ILS 的 ARGUMENT_TYPE_MISMATCH 抓的); 五个新翻译方法; android_info_detail 的六参从 spread 改显式
    (直连 getString 后 lint 的 StringFormatMatches 开始检查这层, spread 被当 1 个参数);
    performance_class_detail_api 的 Int 参数 toString() (同因, 格式串是 %1$s).
  - 测试: `LldFixture` 共享夹具 + 五个新测试类共 16 例, v1 发起时的分布 (mode 3, android 3,
    sdkExtension 2, kernel 4, performanceClass 4); v2 前补例至 21 (kernel 6, android 6,
    见批 4 v1 收下条). 实施排查记四案: AndroidInfo.api 是 Int 不是 String; BaseInfo 是顶层类不嵌套
    Lld; ShadowBuild 的 API 叫 setVersionMediaPerformanceClass; **os.version 的 stage 值必须是裸版本号**
    (本小姐自己发明了 "Linux version " 前缀, g00fy2 不扫前缀解析成 (0,0), kernel 匹配全 None, 探针三号
    实锤, 真机返回裸版本号由 golden 数据可证); 另: 桥接件不传播 `@Config(sdk)` (PreS/PreR 两类删除;
    低版本分支黄金数据够不到, 设备 API 34 高于 R 与 S, 覆盖缺口按批 2 分工记此).
  - 验证: ILS 三主文件 0 ERROR (Destructuring 建议族既有); 单测 51/51 (15 类); lint 无新问题; base 绿.
  - 欠账: 设备无线连接中途掉线, 黄金对照未跑 (设备回来补跑, 对照物仍是本会话批 2 前那份).
  - 下一步动作: 负责人定发 review 的时序 (先补黄金对照 / 先发 review 并行); review 材料不依赖设备.
- 2026-11-07 (批 4 黄金对照补齐 + v1 发起): 设备重连, 装批 4 包重取快照. 基线已隔约十小时, 行数 1344=1346
  且按下标 zip 错位 (828 行), 改**按键对账**: home 页全部 25 行 (含本批五条与批 2/3 的行) detail/color/type
  逐字节相同; prop 页差异 827 行 + 新增 3 行 / 消失 1 行, 全部为 `ro.boottime` / `init.svc` / `persist.*` /
  `debug.tracing` / `cache_key` 设备状态家族 (PropRepository 本批未动, 类别性排除); 基线陈化的口径记此:
  后续批次的对照物仍是 "同一次设备会话内改前改后各一份", 隔日基线只作按键级参考.
  批 4 验证至此齐: 单测 51/51, ILS 0 ERROR, lint 无新问题, 黄金等价. v1 已发起 (review 目录已架,
  提示词 `review-v1-prompt-2123.md`, HEAD `3e95da05`), 工作树冻结 (记录文件除外).
- 2026-11-07 (批 4 v1 收下, 处理发现): 两份报告 (WorkBuddy/claude-opus-6 default, WorkBuddy/DeepSeek-V4.1-Flash
  effort 会话未暴露), HEAD 都是 `3e95da05`, 头尾齐, v1 标 `已收`, 工作树解冻. 合并判定与处置:
  - 老板定的 (实质, 两家同报): detectKernel 的 os.version 为 null 时新旧路径不等价 (主 agent 用 1.5.0 字节码复核:
    单参构造 throwExceptions=false, null 不抛, 静默按 0.0 比较且 detail 渲染字面 null; 新路径 checkNotNull 走
    Failed). 撞上逐行等价的验收标准且两案都站得住, 按 02 摆开, 负责人定 **方案 B: 接受新行为** (批 3 slot
    例外的同类): 注释两处改事实, 补 null 走 Failed 的领域断言, 例外记上一条.
  - 修 (第一档/机械): DetectKernelTest 补 `the last matching lld entry wins` (DeepSeek 发现 3a) 与
    null 路径例; DetectAndroidTest 补 currency Latest/Supported/Outdated 三断言 (DeepSeek 发现 4, api 相对
    `Build.VERSION.SDK_INT` 构造, 类注释记探针实测的 Robolectric 默认环境 SDK 37 / CODENAME REL);
    删 HomeRepository 未使用 import (两家同报); review 目录补 `.gitignore` (内容 `*`, 14 的要求,
    两家同报, 已验证 check-ignore 生效); subtask-04.md 计数按本批修完的树重算带批注 (两家同报: 1154 行,
    入口 23 中 11/12, 辅助 :247/:459/:511, getMyString 78/48, Build 39, toColoredMyModel 14/15 行,
    toSupportOrNotString :1140-1146, guardedMyModel 13, getOutdated :1060).
  - 顺手 (两家提示): LldStatusState 删同包冗余 import; AndroidInfoState 的 AndroidReferenceRow KDoc 改事实
    (只有 lld 整体缺失时才 null); DetectKernelTest 类注释破折号改冒号并补例外句; progress 破折号清零
    (报告点名批 4 段 6 处, 同类扫出批 3 段 12 处, 一并改; 669 提到字符本身那处不算用例);
    progress:724 调用点数改五 (vndk 再次漏记, 同批 3), :730 测试类数改五类 16 例 (PreR 残留删),
    :734 覆盖句改事实 (低版本分支黄金数据够不到, 设备 API 34).
  - 超出范围 O-01: 12-git 提交节奏改写, 收进不回退 (本批开工前负责人另下), 已报给负责人可否决.
  - 观察记此: HomeModelMapper 五行 reference 用下标取值, 依赖 referenceRows 恰五项, 不动代码,
    动该类型时把不变式写进注释 (DeepSeek). 测试名反引号内不能带点号 (`os.version` 编译器抓的, 改 os version).
  - 验证: ILS 四文件 (HomeRepository 0 ERROR + 3 条既有 Destructuring 建议, 三模型 CLEAN); 单测 56/56
    (15 类, 本批新增 21 例); base 绿; lint 无新问题. 偏差记录: analyze-file 本轮未跑, 项目没有开着的
    Android Studio 实例, CLI 挂起无输出, 与批 4 本轮同口径 (ILS + 单测 + lint + base).
  - 下一步动作: 发 v2 (delta), 提示词 `review-v2-prompt-2230.md`, 先核对五组修复再扫新增; 工作树冻结
    (记录文件除外).
- 2026-11-07 (批 4 v2 收下, 处理发现, review 循环结束): 两份报告 (WorkBuddy/DeepSeek-V4.1-Flash
  effort 会话未暴露, WorkBuddy/Hy4 default), HEAD 都是 `3e95da05`, 头尾齐, v2 标 `已收`, 工作树解冻.
  合并判定:
  - 修复核对: 十组修复两家逐条核实全对 (含计数逐格重算, 单测 56 例数对上), 无第一档漏项, 清单之外
    零未列出变化; v1 的应改清单合上. 两家都判停止条件到, 不再排轮.
  - 顺手修 (v2 措辞与卫生三组):
    - 12-git.md:21 的禁用词 `收口` (DeepSeek 1, 08 比喻组在列) 改 "review 结束后". 同类扫出
      progress.md 里九处旧 `收口`, 一并改平实说法; 259/579 两处 `对齐` 亦同规矩 (08 改名表),
      改 "一致"; 709/712 两处按 DeepSeek 2 指名改, 恢复提交直接点名 `3e95da05`.
    - progress.md:732 的 "16 例" 批注为 v1 发起时的分布, 补 "v2 前补例至 21" (Hy4 W-01, 07 就地为真).
    - DetectKernelTest null 例的 `System.clearProperty` 补 try/finally 还原 (DeepSeek 3; Hy4 W-02 判
      观察不动, 判断层采 DeepSeek 的便宜加固, 驳 Hy4 的 "只记一笔", 记此).
  - 观察 (不动作, 已有记录): pre-R / pre-S 分支覆盖缺口等批 6 仪器 (两家同记); LldSource.kt 修改时间
    22:43 但内容与 HEAD 一致且不进 git status, 不算改动 (DeepSeek 核过).
  - 验证: 单测 56/56 (15 类) 复跑绿; 本轮只动测试一处与记录文件, ILS / lint 不重跑 (测试文件以
    Gradle 单测为准, 10 有言).
  - 下一步动作: 第 4 步负责人放行 (留一句放行理由), 第 5 步按 12-git 拆笔提交.
- 2026-11-07 (批 4 第 4 步放行): 负责人看了修改的代码, 说感觉没什么问题; 点名要在 Nexus 5X 真机运行
  看效果 (批 4 包装机: 无线 adb 流式安装两度断流, 改走 push 加设备端 pm install, 装机 Success, 主活动
  启动后无线连接掉线, 老板本机目视), 答复 "没问题". 放行理由留了, 照 14 拟写进 spec 记录笔的提交信息
  (英语): 代码粗看过没有问题, Nexus 5X 真机运行效果也没问题. 拆笔方案已摆给负责人, 等对提交的单独同意.
- 2026-11-07 (批 4 第 5 步提交, 九笔): 老板同意拆笔方案后按批 3 先例重拼: 整树先存进备份分支,
  `reset --hard` 回批 3 末梢, 逐态重拼, 每态 `compileFossDebugKotlin` + `compileFossDebugUnitTestKotlin`
  验证后提交, 末态与备份全树对账零差异, 全量单测 56/56 与 lint 复跑绿, 备份分支已删. 途中一处编译错
  (mapper 的条目 import 忘了跟条目同批加) 当场修掉, 不影响终态. 提交串 (老 → 新): `e8b5243b`
  coloredRow 状态色重载, `6940b90e` LldFixture, `151bc84f` mode, `c53de15d` android, `e995e021`
  sdkExtension, `cca25f27` performanceClass, `7f0115a2` kernel, `f8291ee6` spec 记录 (含老板放行理由),
  `f522aed3` 12-git 规矩. 本条与各子任务状态行的更新随批 5 首笔入库.
  下一步动作: 批 5 (buildId, securityPatches, vndk, mainline), 等负责人说开始; review 目录按 14
  待负责人点头后删 (2026-11-08 负责人已点头, 目录已整个删掉; 归后续单元与全部判定理由在 progress
  在案, 删前逐条核对过).
- 2026-11-08 (批 5 实现 + v1 发起): 负责人说开始, 混合组四条按批 4 同款节奏开工 (一批改完, 多轮 review,
  提交拆细):
  - 领域类型四个: `BuildIdState` (含 BuildDetail 与 Match 四态; 三个分区 build id 原样携带, getString 的
    占位串会原文进 detail, 等 #42 整治, 不新开例外), `SecurityPatchState` (行级, 双行由仓库各出一份
    DetectResult, mapper 按序定标题: 系统在前厂商在后, 仿 referenceRows 的顺序先例), `VndkState`
    (getStringOrNull, 缺席 null; hasVndkLite 只在有版本时读; isPropertyValueNotEmpty 的三退化与 null
    恰好同域, 逐行等价不需要例外), `MainlineState` (provider 与 version 可空, 内层 try/catch 语义
    保留: Log.w 加部分值照旧, 异常路径渲染等价).
  - 判断留仓库: buildId 的日期比较提成 `isBuildIdDateHigherThanConfig` (逐字搬运, 含 preview 偏移),
    securityPatch 的年月比较, vndk 的字符串比较 (version >= stable.api 照旧), mainline 的版本日期比较;
    颜色判断全部落各类型的 Match 枚举.
  - mapper 四个翻译方法; toSecurityPatchModels 返回 List<MyModel> (第一个 List 行条目), 双行各自带 guard;
    ViewModel 四调用点经 mapper, 行顺序未动; toSupportOrNotString 剩四个调用点 (vndk 迁走).
  - 测试: 四个新类 14 例 (buildId 4 含 preview 偏移, securityPatches 3, vndk 5, mainline 2); ShadowBuild
    的 setId 与 setVersionCodename 钉日期比较与 preview 通道; SECURITY_PATCH 无 setter, 系统行按值
    不可知设计, 厂商行走属性可钉; 一次跑绿, 未用探针.
  - 验证: ILS 0 ERROR (Destructuring 建议族随 guardedEntry 方法数增长: mapper 6 → 10, repository
    3 → 4, 同族既有噪音); 单测 70/70 (19 类, 本批 +14); lint 无新问题; analyze-file 未跑 (无 AS 实例,
    同批 4 偏差).
  - 欠账: 设备无线连接不稳, 批 5 黄金对照未跑 (review 不依赖设备; 对照物仍是 "同一次设备会话内改前
    改后各一份", 设备回来补跑).
  - v1 已发起 (review 目录已架, 提示词 `review-v1-prompt-1006.md`, HEAD `f522aed3`), 工作树冻结
    (记录文件除外).
- 2026-11-08 (批 5 v1 收下, 处理发现): 两份报告 (WorkBuddy CN/DeepSeek-V4.1-Flash effort 会话未暴露,
  WorkBuddy/Tencent Hunyuan Hy default), HEAD 都是 `f522aed3`, 头尾齐, v1 标 `已收`, 工作树解冻.
  两家独立核四条逐行等价全过, 无新例外; 合并判定:
  - 修 (第一档, 三组):
    - toSecurityPatchModels 的取数链在 guard 之外 (Hunyuan 发现 1): 事实成立 (detect 先求值,
      guardedEntry 收的是已算好的结果), 但两处取数实际抛不出 (PropertyReader 内部自接), 旧实现同位置
      也整体抛, 不是等价回归, 是 guard 契约覆盖偏差. 修法采仓库侧: detectSecurityPatches 加外层 try,
      取数链异常转两行各自的 Failed (各行 guard 用各自标题渲染失败行); mapper 侧修法要新造 List 失败
      行的展示决策, 弃. 修完的路径不可达 (无抛出源), 补不了用例, 记此.
    - securityPatch 的 Lagging 与 buildId 的 Outdated 两分支无用例 (DeepSeek F1): 补两例
      (年月相等日期落后; stable.api 99 加早日期 build id), 单测 70 → 72.
    - assembleFossDebug 未在本批树跑过 (DeepSeek F2): 补跑通过, 不记偏差.
  - 顺手 (记录, 两家同报): progress 各子任务状态行更新 (批 5 进行中); subtask-04.md:6 现值树说明
    停在批 4, 改批 5.
  - 观察记此 (不动作): 双行标题按行序定, 沿用 referenceRows 顺序先例, 引擎轮统一行的身份 (DeepSeek F5);
    mainline 的 Met 分支 Robolectric 造不出 (需 provider 已安装且内部 config 串可解析), 测试注释已有,
    按裁量表要求补记在此 (DeepSeek F6).
  - 途中一编译错: 新测试把 Lld.Androids 直传给要 Lld? 的方法, 包回夹具即修.
  - 验证: 单测 72/72 (19 类), assembleFossDebug 通过, ILS 0 ERROR (同族既有噪音); lint 不重跑
    (改动仅仓库一处 try 包裹与测试).
  - 下一步动作: 发 v2 (delta), 提示词 `review-v2-prompt-1048.md`, 先核对五组修复再扫新增; 工作树冻结
    (记录文件除外).
- 2026-11-08 (批 5 v2 收下, 处理发现, review 循环结束): 两份报告 (WorkBuddy CN/DeepSeek-V4.1-Flash,
  WorkBuddy/Tencent Hunyuan Hy, 同 v1 的两个 reviewer 续用 delta), HEAD 都是 `f522aed3`, 头尾齐, v2 标
  `已收`, 工作树解冻. 六组修复两家逐条核实全对 (外层 try 的取消处置与仓库其余方法同形, 正常路径逐字
  未动), 等价没有被修复打破, 无新例外; 清单外零代码变化. 合并判定:
  - 修 (记录同步, 三条都是第一档, 顺手改完):
    - subtask-04.md 计数与行号停在修复前的树 (两家同报; v2 修复给仓库加 8 行): 1130 行, 辅助
      `:257`/`:463`/`:515`, toSupportOrNotString `:1116-1122`, getOutdated `:1036`, OutdatedTargetSdkApk
      `:1097`, 各带 "v2 修复加 8 行" 批注. 档位判定: 采 Hunyuan 的第一档 (记录同步属本单元惯例),
      DeepSeek 的 "需求正文自己写错" 标注不成立, 处置相同.
    - state.md 残留 v1 的旧下一步动作行, 与 v2 行并存 (Hunyuan 发现 2): 删旧行.
    - progress 各子任务状态行停在 "v1 已收修复中" (Hunyuan 发现 3): 更新为 v2 已收.
  - 观察记此 (不动作): 外层 catch 用同一个异常实例造两个 Failed, 一行取数失败两行一起失败, 行级隔离在
    该不可达分支不成立 (DeepSeek F2), 与双行标题按下标 (v1 F5) 一起等引擎轮统一; lint 证据早于本轮修复
    (DeepSeek F3), 提交前的整批检查会复跑 lint, 不记偏差.
  - 下一步动作: 第 4 步负责人放行 (留一句放行理由; 欠的批 5 黄金对照一并问时序), 第 5 步按条目拆笔提交.
- 2026-11-08 (批 5 黄金对照补齐): 设备重连 (无线 TLS), 欠账按 "同一次设备会话内改前改后各一份" 补齐:
  改前包由 `git stash push -u` 露出 HEAD `f522aed3` 构建 (stash 后复原), 改后包为批 5 工作树; 两包
  各自 pm install 后跑 `ModelSnapshotTest`, 快照落在应用外部目录, 仅留本地不进仓库 (2026-11-06 定).
  按键对账: 1344 → 1342 行两份同数; **home 25 行与 others 42 行 (key, detail, color, type) 逐字节
  相同, 零差异** — 批 5 四条行的真机等价就此钉死; prop 1275 行仅 4 处 detail 差异, 全部落在 cache_key
  设备状态家族 (类别性排除), color 与 type 零差异. 途中两坑记此: androidTest APK 无需重打 (测试类
  未动, 主类来自随装的主包); `pm install` 两个包不能合一条命令 (测试包是独立包名, 合装报
  INSTALL_FAILED_INVALID_APK). 批 5 验证至此齐: 单测 72/72, ILS 0 ERROR, lint 无新问题 (提交前复跑),
  打包通过, 黄金等价.
  下一步动作: 第 4 步负责人放行 (留一句放行理由), 第 5 步按条目拆笔提交.
- 各子任务状态: subtask-01 已关闭; subtask-02 已提交并关闭;  
  subtask-03 已提交并关闭; subtask-04 进行中 (批 1 ~ 批 4 已提交关闭, 批 5 进行中: v2 已收, 循环结束); subtask-05 ~ subtask-06 报告已出, 实现未开始.
- 未查清: 无.
- 挂起项: 无.
