# AGENTS-cn.md

给在本仓库工作的编码 agent 的指引. 改动之前先读.

两边默认都不是权威: **代码和文档都可能错**.  
代码定下应用*今天*做什么, 这是可验证的事实. 应用*应该*做什么属于负责人,  
而文档里一句关于意图的陈述本身也可能就是个错误 (写过头了, 或者凭空编的).  
只有负责人定下的约定排在代码之上, 那时错的是代码, 而不是"过时"的文档.  
所以在动手编辑之前先给文档与代码的分歧定性 (通常是四种之一: 文档过时, 代码 bug,  
需求未实现, 或者文档把需求写过头了); 证据定不了性, 或者问题在于那句意图是否真的出自负责人时, 就问.

约束你的是什么, 由一条陈述**讲的是**什么决定, 绝不由它落在哪个字段, 哪一列, 哪个标签或哪个小节里决定,  
也不由它落在哪份文档里决定 (本文件和 `docs/` 下的一切在这点上没有区别):

- 一条**讲代码**的陈述是事实: 照着它行动之前, 先对照当前代码验证它.
- 一条**讲问题**的陈述是估算: 凡是主语是某个问题有多重, 而不是代码做什么的, 不管它落在文档或表格的哪个格子,  
  现在有的还是以后加的, 都一样. 它给问题定量, 永远不给修复设上限. 评级把问题说得小, 而负责人要把问题清掉时,  
  彻底的修复仍然是正确的目标; 因为那个评级就把负责人往浅的修法上带, 正是这条规则要防的那个错误.  
  划定改动范围的规则 (把一条指令的范围收到被点名的对象上, 而不是让已有的问题变得更糟) 说的是你接手哪个问题,  
  永远不管一个已经接手的问题可以修得多浅: 把它解决掉在定义上就是相关工作.
- 一条**负责人定下的约定**有约束力, 三者里也只有它有. 让一条陈述成为约定的, 是它规定代码*应该*做什么;  
  文档描述当前怎么做的不算, 所以改那种做法是负责人的决定, 也绝不是把负责人往弱修法上带的理由.  
  一份笔记的**状态**是主语之外唯一会改变这一点的因素: record 或 frozen 的笔记就是负责人把这份笔记  
  从今天的规定里撤回, 所以其中一句规定句不约束任何东西 (见文档规则).

所以"把格子列个清单"不是这里的运作方式, 本文件也不维护这样的清单: 检验标准是陈述的主语,  
而一句光看文档判断不出类别的陈述是拿来问的, 不是默认照办的.

工作原则:

- **Memory**: 本文件就是持久的 memory: 已经定下的决定, 约定, 坑, 文档更正都记录在这里,  
  让下一个会话站在它们之上, 而不是重新发现一遍.  
  未整理的个人材料进 [docs/dev/JOTTINGS.md](docs/dev/JOTTINGS.md).  
  不确定该记什么或该记在哪, 和用户讨论.
- **Never guess**: 先查代码库和文档; 每次改动都要建立在你能指出来的证据上 (文件, 行号, 文档小节).  
  "某条规则哪里都不存在" 是同一类断言, 也要同一类证据: 先全仓库 grep, 不要只查已经读过的那几个文件.  
  某件事无法确定, 或者存在多种合理做法时, 摆出发现并问用户;  
  写 `unknown`, 而不是编造一个取值. 冲突是停止信号, 不是自行择一的许可: 开口之前先查.  
  读代码, 然后在 `git log` 里把两边各自的来龙去脉追一遍, 报告你为每一边追到的证据和你实际持有的把握,  
  并问负责人真正想要什么: 由他们的答复决定, 而不是由一个沉默的猜测决定.  
  指令的范围只到被点名的对象: 第二句看起来像同一个问题的, 进入要问的清单, 不并进同一次修改.
- **Report before editing**: 一条要改项目的指令, 开的是一份要负责人答复的报告, 不是编辑. 当有文档跟着目标走时,  
  先重新核对它说的是否仍与当前代码一致 (文档可能过时或本来就是错的; 见开头),  
  然后在任何编辑之前给负责人一份简短报告: 当前行为是什么, 它缺了什么或哪里错了,  
  存在多种修法时列出候选, 以及你推荐哪一个和为什么.  
  修法由负责人挑; 负责人同意之后才动笔.  
  spec 子任务的第 0 步报告就是这份报告; 其余改动各自欠这份简短报告.  
  这是把 spec 流程的确认纪律套到每一次改动上, 有无文档都算, 且它约束每个会话.
- **Finish in one go**: 一个内聚的改动单元 (一个 spec 子任务, 一次独立改动, 一次文档修订:  
  单元是什么都一样) 要在一整段连续的工作里做到可以验证为止.  
  同一个单元不许留下没做完的部分: 不许把剩下的部分推给以后的会话或以后的改动,  
  也不许因为中途冒出一个确认点就把半完成状态当成已完成. 等负责人同意不算没做完的部分:  
  确认点要的是一个决定, 它不结束这个单元.  
  工作中发现的问题, 在编辑扩到它之前先报告, 哪怕它看着就属于这个单元; 报告它本身也不算没做完的部分.  
  负责人的答复会把它点名, 从那时起它就是本单元的一部分.  
  候选只从与这个问题相关的东西里取 (边界怎么画: 见上文 "指令的范围只到被点名的对象"),  
  而候选之中被推荐的那个必须是把问题彻底解决掉的那个: 便宜的那个只列为候选, 绝不作为默认,  
  文档里的一个评级或某一列也不把浅的那个变成默认 (见开头).
- **English by default**: 你生成的一切 (文档, 注释, 提交信息) 除非用户另作说明, 一律用英语.  
例外: 一条**新增**的代码注释  
  在实施期间用负责人的聊天语言书写, 好让负责人看进行中的工作时能读自己的语言; 入库的代码库保持英语,  
  所以每条这样的注释在提交之前都要译为英语. 这笔翻译欠在当下这次改动的确认点上, 不只由任务工作流第 1 步欠着:  
  一次不开 spec 目录的改动永远走不到那一步.  
  一条已经存在且没有被重写的注释保持所在文件的语言, 而一个注释全为英语的文件并不让一条新增注释豁免于这个例外.  
  不管文档用什么语言写, 它的标点都是 ASCII, 间距按英语来 (不用 `，。、：（）「」`): 见 [文档规则](#文档规则).
- **No sensitive information**: 任何文档都不许含敏感信息, memory 文档也在内: 不许有隐私数据, 密码, 密钥,  
  证书或签名材料.

刻意偏离主流 / 官方模板的约定在下文就地标出:  
它们不是要修的坏味道, 别顺手把它们 "规范化" 掉.  
**哪一边错了, 被改的就是哪一边**; 上面那几类就是你分辨哪边错了的办法,  
而一条划定范围的规则说的是你接手哪个问题, 永远不管一个已经接手的问题可以修得多浅.  
退役一条约定是负责人的决定.

## 详细内容住在哪

本文件是索引: 它留下约束每个会话的东西, 再加上容易做错的流程部分. 一项活动的长文住在 `docs/agents-cn/`,  
在那项活动开始时去读.

| 读 | 什么时候 |
|---|---|
| [docs/agents-cn/verification.md](docs/agents-cn/verification.md) | 你要依赖一个 `android studio analyze-file` 的结果 |
| [docs/agents-cn/build-conventions.md](docs/agents-cn/build-conventions.md) | 你改 Gradle, 版本目录, 工具链版本, CMakeLists.txt 或 CI |
| [docs/agents-cn/architecture.md](docs/agents-cn/architecture.md) | 你碰导航, DI 图, 某个 ViewModel 或状态类, 列表页, 设置持久化, 或 shell/属性访问 |
| [docs/agents-cn/detection-item.md](docs/agents-cn/detection-item.md) | 你给某个功能新增一个检测条目 |
| [docs/agents-cn/task-workflow.md](docs/agents-cn/task-workflow.md) | 你开一个 spec 目录, 开始一个子任务, 或关闭一个任务 |
| [docs/agents-cn/doc-formatting.md](docs/agents-cn/doc-formatting.md) | 你写一份文档, 一条代码注释, 一笔提交信息, 或一条回复 |

## 项目概览

一个把底层系统特征摊开来看的 Android 应用: Treble 与 GSI 兼容性, Mainline/APEX 模块, system-as-root,  
A/B 分区, Binder 位数, 安全补丁级别. 技术栈: Kotlin,  
Jetpack Compose (单 Activity, Navigation 3), MVVM + StateFlow 单向数据流,  
kotlinx.serialization, Ktor, libsu, JNI/NDK. DI 用 Metro (编译期, 无反射).

- 应用 id `net.imknown.android.forefrontinfo`; 版本信息在 `gradle/toml/build.toml`.
- 模块: `:app` (Compose UI, 各功能在 `ui` 下) · `:base` (`IProperty`/`IShell` 抽象) ·  
  `:binderDetector` (经 JNI 的 C++) · `build-logic` (约定插件).
- 产品介绍与下载链接在 [根 README](README-cn.md).

## 构建与验证

```bash
./gradlew assembleFossDebug      # CI 构建的目标 (GitHub Actions)
./gradlew testFossDebugUnitTest  # 单元测试 (目前只有模板)
./gradlew lintFossDebug          # Android lint
```

- 只改文档或注释的编辑跳过构建那几档 (没改代码), 但下面那条 diff review 同样适用于它们.
- **用 Android Studio 做分析**: 每批编辑之后, 先对每个改动文件跑 `android studio analyze-file`,  
然后是上面那几档 Gradle 检查,  
  再派一个新上下文的 subagent 在后台 review 未提交的 diff,  
  把发现汇报给负责人后停下: 无论是否走 spec 流程, 没有负责人的明确同意一律不提交.  
  reviewer 欠每一条发现可核验的证据 (文件:行号), 且优先给 reviewer 选一个与写下这次改动时不同的模型:  
  这些义务约束每个会话, 不只是 spec 任务.  
  Review 按轮次进行 (v1, v2, ... 每修一轮顺延);  
  [docs/agents-cn/task-workflow.md](docs/agents-cn/task-workflow.md) 的 Review 步骤里那套增量复核与换新 reviewer 的策略同样约束每个会话.
  - **选 reviewer 的模型和推理强度是负责人的决定**: 启动 reviewer 之前先取模型清单.  
    到底有没有一份模型清单, 以及它带着哪些字段, 取决于跑这个会话的 agent; 有的暴露这样的工具, 有的没有.  
    清单返回时, 把整张表展示给负责人, 工具带一个字段就给一列, 工具对某个模型有定位说明时就附上.  
    示例字段是 id, name, 价格, 发布时间或 effort 取值; 绝不编造列.  
    清单里没有 effort 列时, 展示表格的同时说明这一点, 并让负责人点名 effort; 空结果算作取不到清单.  
    然后负责人同时选模型和 effort, 默认是多选: 点名几个模型就并行启动几个 reviewer, 他们的报告合并,  
    一致的发现合在一起, 分歧的摆给负责人. effort 按模型逐个选, 没有这个取值的模型跑它自己的默认值, 报告里写明.  
    这一次挑选管一整个 review 序列: 复用的 reviewer 保持它的模型和 effort, 换新 reviewer 时重新问.  
    取不到清单时, 问负责人该怎么办: 退回到写下这次改动的模型需要负责人同意,  
    而读不到本会话自己的 effort 级别时, 该取值仍然出自负责人. 绝不猜 effort 级别;  
    [Git 与 CI](#git-与-ci) 里的提交 trailer 规则照常适用.
  - [docs/agents-cn/verification.md](docs/agents-cn/verification.md) 是那条命令的完整契约:  
    依赖任何结果之前先读它, 并当心有些运行看着像通过其实不是 (坑清单和怎么读打印出来的分块都在那儿).

## 构建约定

[docs/agents-cn/build-conventions.md](docs/agents-cn/build-conventions.md) 是构建的布局.  
改 Gradle, 版本目录, 工具链版本或 CI 之前先读它.  
下面这些约束任何一次构建改动:

- 依赖与版本只住在 `gradle/toml/` 下的五份目录里:  
  没有默认的 `libs` 访问器, 所以模块的 `build.gradle.kts` 里绝不写裸坐标,  
  引用之前先定这个依赖归哪一份目录.
- SDK, build-tools 和 NDK 版本只住在 `gradle/toml/build.toml` (配一个 `isPreview` 开关),  
  经 `build-logic` 的约定插件下发给各模块. 模块脚本里绝不硬编码 SDK 级别.
- 正文里不写版本号: 哪个构建文件承载某个值, 那个值的真值源就是它, 所以文档,  
  代码注释和提交信息点名那个文件或它背后的 key, 不复述数字.  
  承载者就是 `gradle/toml/` 下的五份目录 (在 `build-logic` 里也可经 `buildVersion("<key>")` 取到),  
  `gradle/wrapper/gradle-wrapper.properties` 和 `gradle/gradle-daemon-jvm.properties`:  
  一个 `.kts` 脚本或一个 `.properties` 文件的分量与一份 `.toml` 目录完全相同. 禁令覆盖我们自己构建文件承载的值;  
  外部世界的版本 (某条上游发布线, 某个 API level) 是句子实质的一部分时就留在句子里.
- Kotlin 编译器带着在 `build-logic` 里声明的一组实验性 flag 运行: 那些 flag 是刻意的, 别删掉它们;  
  每次升 Kotlin 都重新评审那些 flag 分组, 把已经稳定的去掉. 代码风格是 `official`.
- 版本分级: 一个 RC / Stable 的依赖或工具链版本可以直接进生产.  
  Beta / Alpha / Canary 的也可以, 但只有在充分研究过,  
  它的已知问题能修好或绕开, 且这次采用经过评估之后.
- 配置缓存 (带 parallel + integrity 检查) 和并行构建已启用; 自定义 task 要保持配置缓存兼容.

## 模块细节

- `:app`: 应用本身; namespace 也是 applicationId.  
  它的 `sourceSets` 注册紧邻包路径的 res 目录:  
  资源住在 `java/<package>/.../res` 这样的路径下  
  (例如 `app/src/main/java/net/imknown/android/forefrontinfo/ui/home/res`),  
  没有 `app/src/main/res`,  
  新的 res 目录必须在 `app/build.gradle.kts` 的 sourceSets 里注册.
- `build-logic`: included build, 装着约定插件; 所有模块 SDK 与构建值的唯一来源.  
  共享配置 (SDK, desugaring, Java 工具链, Kotlin 编译器参数, 测试依赖) 住在  
  `build-logic/convention/src/main/kotlin/.../android/`; 模块只负责 apply 插件.  
  新增一个约定插件: 在 `build-logic/convention` 下实现,  
  然后在 `gradle/toml/android.toml` 的 `[plugins]` 一节里注册一个 alias.

## 架构

每个功能住在 `ui` 下自己的包里 (`ui.home`, `ui.others`, `ui.prop`,  
`ui.settings`), 分层相同:

```
Screen (Compose) → ViewModel (StateFlow) → Repository → DataSource
```

- 导航是 **Navigation 3**, 不是 Navigation 2; 别按 Nav2 的词去想.
- DI 用 **Metro** (编译期, 无反射); 延迟注入写 `() -> T`, 绝不写 `Provider<T>`.
- Metro 的构件在 `gradle/toml/thirdParty.toml` 里锁到同一个 `version.ref`;  
  升 Kotlin 就要配套升 Metro, 所以先查官方兼容矩阵.
- 设置项归 `SettingsStore` 所有; 它之外任何地方都不直接碰 SharedPreferences.
- 可测试性是接口优先: `:base` 定义 `IProperty` / `IShell`, 图绑定在用的那些实现.
- `BaseListViewModel` 驱动每一个列表页; 刷新时刻意保留上一个列表,  
  让界面绝不闪成空白, 而 `loadJob` 给加载去重.
- Compose 的稳定性注解是有意加的; 状态类一改动就要重新评估它们.
- 内置的 `lld.json` 会被复制到外部存储, 允许联网时经 Ktor 在线刷新;  
  用 GitHub 还是 Gitee 的 URL 按时区选.
- 命令执行用 libsu 的 **非 root** 模式; 没有 root 层.

[docs/agents-cn/architecture.md](docs/agents-cn/architecture.md) 带着细节和每个决定背后的为什么.

## 新增一个检测条目

[docs/agents-cn/detection-item.md](docs/agents-cn/detection-item.md) 就是那套流程 (列表顺序 = 调用顺序):  
给某个功能新增检测条目之前先读它.

## 代码规则

给新代码的规则. 它们编码的是已经定下的决定; 别让已有的问题变得更糟:

- `ViewModel` 一律拼全: 标识符, 注释, 提交信息和文档里绝不缩写成 `VM`. `VM` 已经是 *virtual machine* 的缩写,  
  而这应用把虚拟机当作自己另有一个主题来检测 (进程/VM 架构, `getArchitecture`),  
  所以即便上下文足以看懂, 这个短写也是有歧义的.  
  `UseCase` 和 `DataSource` 同样拼全: 不写 `UC` / `DS`.
- 不用静态事件总线: 绝不把 `SharedFlow`/`StateFlow` 放进 ViewModel 的伴生对象.  
  跨功能的数据经 repository 传递.
- UI 层 (Compose / ViewModel) 绝不用 try 接业务异常:  
  Repository 与 DataSource 要么自己处理好失败, 要么返回 wrapper 类型,  
  不带 `OrThrow` 后缀的函数以不抛为契约.  
  异常能到达 UI 层就是要从源头修掉的 bug, 不是给 UI 吸收掉的东西.
- 全局的 `myAndroid` (`AndroidVersionExt`) 只有两个写入方:  
  启动时的 `initMyAndroid()` (`MyApplication.onCreate`, 来自运行时的 `Build.VERSION`),  
  以及 `HomeRepository.detectAndroid()` 里的已知值覆写.  
  绝不在别处给它赋值: `isAtLeast...()` 这些辅助函数到处都在读它.
- minSdk 来自 `gradle/toml/build.toml`: 更新的 API 要用 `isAtLeastAndroidX()` 辅助函数或  
  `@RequiresApi` 限制.
- 新代码用钉住的版本所允许的最新语法和标准库 API:  
  当前 Kotlin 版本支持的最新 Kotlin 语法与 std-lib API,  
  当前 compileSdk 提供的最新平台 API,  
  以及当前依赖版本提供的最新 API,  
  所以绝不写比工具链允许的更老的写法.
- 能用上的最新语法或 API 本身处于 Beta / experimental 时, 不要单方面采用:  
  摆出来问负责人怎么处理.  
  `build-logic` 里已启用的那组实验性编译器 flag 是定下来的; 这条管的是新的 opt-in.
- 阻塞性工作 (shell, 系统属性, 文件, 网络) 跑在 `Dispatchers.IO`, 不是 `Dispatchers.Default`.
- `ShellDefault` 故意没有调用方:  
  它是保留备用的原生 shell 实现, 不依赖 libsu  
  (在用的是 `ShellLibSu`). 别把它当死代码删掉; 如果将来启用它,  
  先修掉 `waitFor()` 之后读管道的那个死锁.

## 本地化

- 字符串按功能包拆分. 支持的 locale: 默认 (英语), `zh-rCN`, `zh-rTW`, `fr-rFR`  
  (`localeFilters` + `generateLocaleConfig`);  
  引入新语言时把该 locale 加进 `localeFilters`.
- 新的面向用户字符串一律要有默认英语条目;  
  能做到的时候把三个翻译文件同步更新.
- 按语言的排版标点 (中文的全角冒号, 法语冒号前的空格,  
  ...) 只适用于面向用户的本地化文案.  
  [文档规则](#文档规则) 里的 ASCII 标点规则照常管辖文档, 代码注释,  
  以及非文案的字符串资源 (存储 key, URI, 技术取值).

## 任务工作流 (docs/spec)

负责人 = 开发者本人. AI 负责搜索/分析/编码/测试/review;  
负责人负责目标/边界/判断/提交. 每个确认点都以负责人的明确同意结束:  
没有负责人的明确同意一律不提交 (走不走 spec 流程都一样), 未经同意不开始下一步.

[docs/agents-cn/task-workflow.md](docs/agents-cn/task-workflow.md) 就是流程本身: spec 目录的布局, 负责人的预写块,  
`plan.md` 欠的东西 (子任务拆分, 每个单元为什么单列, 它的风险级别, 待学习清单), 计划确认,  
子任务的第 0..5 步与各自的新上下文 review 轮次, review 循环何时停, 偏差记录, 任务结束, 以及归档.  
创建 spec 目录之前读它, 采取某一步的行动之前重读你所在的那一步.

下面这些约束每个会话, 无论是否 spec 任务:

- 每个 spec 目录携带 `progress.md`, 也就是进度记录; AI 在每个确认点更新它,  
  它随下一笔提交入库.
- 会话开始时, 或负责人任何时候说 "continue" 时, 扫描 `docs/spec/*/progress.md` 找未完成的任务  
  并按进度记录接续: 负责人要记住的只有那一个词. 动手之前先和负责人确认这次接续, 哪怕只有一个未完成任务;  
  同时有多个还没做完时, 把它们列出来问选哪个.

## Git 与 CI

- PR 指向 `develop` (默认分支).  
  提交信息遵循 Conventional Commits, **类型 + 作用域用小写**: `fix(home): ...`.
- 每笔提交信息都以一行 trailer 结尾, 写明产出它的 agent, 模型和推理强度级别  
  (`off` / `low` / `medium` / `high` / `xhigh` / `max`, ...),  
  形如 `Generated with ZCode (GLM-5.3, effort: xhigh)`.  
  `effort:` 是推理强度这个旋钮本身, 不是对推理质量的评判; 很早期的提交写的是 `reasoning:`, 那些保持原样.  
  绝不猜取值, 也不要自己写 `unknown`: 问负责人该记什么, 他们确认过确实无法确定之后, `unknown` 才是正确的,  
  商定过的写法.
- agent 的名字要凭证据给, 不凭习惯, 并展示依据, 写下来之前先取得用户同意.  
  版本级的名字是不同的 (`Qoder CN` 和 `Qoder` 是两个不同的 AI 开发环境 (ADE));  
  用户不要求就别自造 `IDE` / `CLI` 这类宿主形态后缀.
- CI 只构建 `assembleFossDebug`. `master` 承载 `lld.json` 的数据更新, 所以不要对它开 PR.

## 绝不提交

- 绝不提交或强制添加凭据, 签名材料,  
  或本地配置 (`local.properties`, `keys/release.jks`, `google-services.json`):  
  根 `.gitignore` 已经排除了它们.
- 如果某个改动看起来非提交这类文件不可, 先停下问.

## 文档规则

在这里写东西的规则. 本文件是 **living** (活的): 它对代码所作的陈述描述当下, 所以一句过期的陈述就是要就地修掉的 bug.  
它的约定在另一层意义上是活的: 它们说的是代码该做什么, 所以代码没做到时它们不会失效,  
照着当前代码来更新它们反而是错的编辑 (见本文件开头的文档与代码分类).  
[docs/dev/JOTTINGS.md](docs/dev/JOTTINGS.md) 是 **scratch** (草稿): 未整理的材料, 不是规则的来源.  
一份笔记可以声明自己 **frozen** 或 **record**: 它保持写下时的文本,  
里面的更正和后来的发现也一并留着, 所以引用它来看历史, 但绝不把今天的代码回填进去.  
默认没有任何东西是 frozen; 新写的一切都是 living, 除非它另有声明,  
而一份新笔记要写明自己属于哪种状态: **living**, **frozen / record**, 或 **scratch**.  
有一个状态由位置声明, 而不是由文件声明:  
一份移进任何 `archived/` 目录的笔记就是 **record** (`docs/spec/archived/` 里的 spec 目录,  
`docs/dev/issues-cn/archived/` 里的 issue 条目, 以及仓库以后长出的任何归档都同样算):  
归档那次移动本身就是声明, 文件不会为了承载声明而被编辑,  
里面的状态头部仍写着 living 的视为被取代.

- 文档跟着什么就走一个 ID, ID 跨文档共享, 所以按 ID 引用它们.
- 文档**就地**修复: 一句陈述被证明是错的,  
  就重写它和下游依赖它的每一处, 让这份文档单独读起来就是当下的真值.  
  不要把被取代的那句留着, 在后面接一个括号或一段引用块作更正;  
  也不要在改好的句子上标注它是哪次检查改出来的: 事实属于句子本身,  
  不属于一句关于谁在什么时候检查了它的笔记.
- 判定冲突哪边错之前先问: 两份文档不一致,  
  或者一份文档和代码不一致且证据判定不了哪边错时,  
  把两边摆出来让负责人决定, 然后只重写被判定要改的那一句.  
  更正不扩散: 另一句看着像同一个问题的, 进入要问的清单, 不并进同一次编辑.
- 文档只写负责人说过的或代码显示的. 不为了让某一行或某一句显得完整而添加术语, 检测条目,  
  或限定语;  
  词汇表的每一行只覆盖这份文档自己用到的词,  
  而一行里最后的引用已经消失时, 那是拿来问的, 不是因为那个词人尽皆知就留着.
- **大白话**: 每个短语都要一遍读懂: 不自造简称,  
  不给后文要依赖的东西起比喻性的代称 (比如把一批工作项叫成 "桶"),  
  不用文档没有介绍过的缩写.  
  沿用 AGENTS-cn.md 或本文档已经定义的术语;  
  真正的新术语在首次出现处定义. 同样的标准适用于代码注释,  
  也适用于给负责人的聊天回复.
- **给负责人的聊天回复**: 写给一个没打开过本文件的人看.  
  这条标准对你写作的任何一种语言都一样:  
  [docs/agents-cn/doc-formatting.md](docs/agents-cn/doc-formatting.md) 里的词表是本仓库写出过的写法和负责人  
  点名的写法, 不是规则的边界.  
  先给结果, 再给原因; 只有在等负责人的答复时, 才加一句话说下一步做什么.  
  只说会改变负责人决定的内容: 不复述问题, 不把规则原文背回给负责人, 不在结尾加没有被要求的总结.  
  不叙述内部工作: 你在遵循哪条规则, 你打算检查什么, 你自己的判断是怎么推出来的.  
  本仓库要你交上去的报告 (编辑前的那份报告, reviewer 的发现) 是负责人在等的实质内容, 不是内部叙述:  
  给出它的内容, 去掉原本会围着它的那些一步步的过程交代.  
  指称一样东西时用代码符号, 文件路径或原文字符串, 而不用本文件自起的别名;  
  当本文件定义的术语确实是最清楚的说法时, 先用大白话说它, 再把术语放进括号, 且只放第一次.  
  禁用词表和改过名的术语清单, 见  
  [docs/agents-cn/doc-formatting.md](docs/agents-cn/doc-formatting.md) 的 `用词要平实` 和  
  `本仓库改过名的术语` 两节.
- 留在正文里的是实质与导航: 句子在划的范围或例外,  
  按 ID 的交叉引用, 代码符号. **谁在什么时候决定了什么都不是正文**:  
  带日期的决定属于 `git log`, 不该变成每句后面一个 "负责人决定, <日期>" 的标签.  
  一份带版本行的文档点名版本并指向 `git log`;  
  它不积累一份修订记录.
- 文档默认用英语, 文件名默认也是英语; 非英语的文档, 整个目录都用这门语言写时把语言标识加在目录上,  
  其余情形加在文件上. 处于镜像范围内的文档 (根的 `AGENTS.md` 和 `README.md`, 加上 `docs/agents/`  
  下的每一份英文正本) 在仓库把它们译成的每种语言里都有互为镜像的副本, 而改一份镜像文档的任何一个文件,  
  在这份文档的其余文件都带上同一改动之前都不算改完: 整组要在同一次修改里改到底.  
  孪生机制 (哪份生效, 哪些文档在镜像范围内, 语言标识加在哪, 链接与引用指向哪里,  
  采用一门新语言要添什么) 住在  
  [docs/agents-cn/doc-formatting.md](docs/agents-cn/doc-formatting.md).
- **任何语言都用 ASCII 标点**, **不用破折号类标点**, **长行按语义硬换行**:  
  这三条适用于每一份文档, 每条代码注释和每次聊天回复, **frozen**, **record**, **scratch** 的也一样  
  (标点和换行是排版, 不是那几种状态要保住的实质).  
  [docs/agents-cn/doc-formatting.md](docs/agents-cn/doc-formatting.md) 是机械部分: 禁用词表与改名对照表,  
  CJK 到 ASCII 的映射与间距例外, 按关系分类的破折号改写与结构性的 `--` 豁免, 以及表格和代码注释的换行  
  例外. 写文档, 代码注释, 提交信息或给负责人的回复之前读它, 转换或重排已有文本时也是.
- 笔记保持简短; 一份长到过头时, 沿它天然的边界拆分, 前面留一份索引页.
