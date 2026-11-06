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
- 2026-10-29: v3 收口核对跑完 (结果在 v3 的结论 (已并入本记录)): 7 处修补 6 处成立, 第 7 处不成立,  
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
  - 顺带: subtask-02 的例外名单说法与 subtask-04 对齐 (原写两类, 后者写四类); `#50` 那句主语悬空改顺.
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
- 2026-11-05 (收口): 决定不再排 v9. 理由: v1 到 v8 共八轮, 代码侧的实质问题在 v6 ~ v8 已经出尽
  (短账号漏网, 生成时 vs 事后脱敏, 自检与替换口径不一致), v8 之后只剩文档口径与匹配细节, 逐一改完后
  v8 那五处我自查过: 自检与替换共用 `tokenRegex`, MAC 全表查, 07 指代改回限定说法, 记忆那句点明范围,
  记录补上; 离线全量全绿, 设备侧用新 APK 跑 `OK (1 test)`, 新自检没有误报.
  剩下三条归 subtask-04 (离线路径覆盖, 资源 id 键, 实时计数器例外), 已在本文件记着.
  以后要重开 review 的条件: 生成器的 key 片段表大改, 或 subtask-04 换设备重生成时出现新的机器定位值形态.
  八轮的走向 (报告都在 review 目录, 收口时删掉, 结论只留在这里):
  v1 全量: JUnit 4 残留, 04 架构版本线写错, issues 现状过期, 快照断言只覆盖一项 → 全修.
  v2 delta: 落定超时不还原设置, 四份 issue 现状过期, subtask-04 验收与时效边界冲突 → 全修.
  v3 delta: subtask-04 验收自相矛盾, #50 一句读着像模板被迁移 → 全修.
  v4 delta: 基线里有序列号等定位值 (我上一轮判错) → 生成器脱敏, 基线重做.
  v5 作废: 负责人往 origin 推了 07 判据, HEAD 变了.
  v6 delta: 4 字符构建账号仍漏 11 行 (事实层裁定) → 阈值与匹配重做, 基线重生成.
  v7 delta: 自检只查已收集值, 占位只认简中, 子串替换会切碎通用词 → 全修; 07 两处歧义请负责人裁定.
  v8 delta: 替换与自检口径不一致, 07 指代丢限定词 → 全修, 自查后收口.
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
- 2026-11-06: v4 (delta) 发起. 我建议收口, 负责人要看第四轮还能挖出什么, 定排 v4.  
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
- 2026-11-06: 收口 (负责人定). 偏差: v5 跳过, 未跑. 理由: v1 到 v4 共四轮, 每轮的新发现都是上一轮修复带出来的,  
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
  待负责人定 (需求正文): `#50` 在 `issues-cn/README.md:245` 的状态仍写 "开放 (主线步骤 1a)", 1a 已实现并实跑通过,  
  要不要改成已完成由负责人定.
- 各子任务状态: subtask-01 已关闭; subtask-02 已提交并关闭;  
  subtask-03 实现与本地验证完成, 等 review; subtask-04 ~ subtask-06 报告已出, 实现未开始.
- 未查清: 无.
- 挂起项: 无.
