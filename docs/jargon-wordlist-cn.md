# 行话与黑话词表(中英对照)

写开发者报告时的词汇总表:每条给出英文说法、中文说法、大白话解释。中英对照尽量同时保留字面义和语境义(如 verdict = 裁定/判决,flavor = 风味 → 构建语境指「变体」);通俗解释以中国普通高中生能看懂为准,能点出项目语境的地方会顺手说明,不硬凑。

词条分三类:

- **浮夸黑话(第 1 节)**:纯粹的汇报黑话。写文档时一律按「通俗解释」列改写,不保留原词。
- **行话与俗语(第 2–3 节)**:专业行话、俗语。写给大众看,优先用「通俗解释」列的说法;写技术内容可以保留术语 —— 幂等、竞态这类标准词改掉反而失真 —— 但首次出现建议括注通俗解释,如「幂等(做多少遍结果都一样)」。
- **词典类(第 4–11 节)**:当词典查。第 4 节和第 6–10 节的专业词可直接使用,中英互译、通俗解释都在词条里;第 5 节「英文难词」是普通英语生词(verdict、stale 这类),不是术语,查生词用;第 11 节是缩写和编号。术语尽管用,但要让目标读者看得懂,需要时按「通俗解释」列补一句大白话。

规则提醒:

- 本仓库文档里,标题行文字不动(保锚点)、代码块不动。
- JOTTINGS.md 是手写笔记,不受此表约束;词表自己也豁免(它必然包含这些词)。
- 英文生词(四六级往上的普通单词,如 verdict、stale)集中在第 5 节「英文难词」,按「英文」列找到词条,读「通俗解释」列。
- 领域和工具类的缩写(VNDK、APEX、ROM、AGP 等)在对应小节的词条里就地展开,不在末节重复。

## 目录

1. 汇报与管理黑话
2. 通用软件与架构
3. 正确性、并发与排错
4. 英文术语
5. 英文难词
6. Android 系统、设备与刷机调试
7. 应用架构与技术栈
8. 构建系统与依赖
9. 工具链与流程
10. 通用 Android 开发行话(扩展层)
11. 缩写与编号

## 1. 汇报与管理黑话

| 英文 | 中文 | 通俗解释 |
|---|---|---|
| align | 对齐 | 统一想法、商量一致:大家理解一样了再动手。 |
| sync up | 拉齐 | 同「对齐」,把不同人的理解掰成一致。 |
| closed loop | 闭环 | 有始有终、转一圈接上了:从头到尾都连起来,没有断头。 |
| land / put into practice | 落地 | 做完、做出来、变成现实。 |
| lever / handle | 抓手 | 下手的着力点:从哪儿入手把事办成。 |
| empower | 赋能 | 帮谁配上能力,让他做到原本做不到的事。 |
| accumulate / retain | 沉淀 | 攒下来、整理保存,以后能用。 |
| mindshare / mental model | 心智 | 人脑子里的理解和印象。「心智负担」= 看懂它要费多大劲。 |
| granularity | 颗粒度 | 拆得多细:「颗粒度粗」= 大而糙,「颗粒度细」= 碎而细。 |
| chain / pipeline | 链路 | 一串环节、一条路径:数据或请求走过的地方。 |
| portal | 门户 | 入口页、目录页:放一堆链接、帮你找到其他内容的那一页。 |
| ecosystem | 生态 | 围着某个东西转的整套周边。 |
| moat | 护城河 | 别人一时抢不走的优势。 |
| combo | 组合拳 | 几个办法搭配着一起上。 |
| top-level design | 顶层设计 | 最上层、全局的规划。 |
| underlying logic | 底层逻辑 | 最根本的道理:表面之下实际靠什么成立。 |
| methodology | 方法论 | 一套成体系的做法。 |
| pain point | 痛点 | 让人难受的问题。 |
| highlight | 亮点 | 特别好的地方。 |
| perceived experience | 体感 | 实际用起来的感受。「用户体感差」= 用户用着觉得难受。 |
| track / vertical | 赛道 | 领域、行业方向。 |
| paradigm | 范式 | 一套固定的做法、套路。 |
| endorsement | 背书 | 替某样东西作保、表示认可。 |
| stakeholder | 干系人 | 负责人、相关的人:这事归谁管、谁说了算。 |
| ruling | 裁定 | 拍板、下结论:问题怎么处理,定下来了。(法规语境保留原词没问题。)「裁定」对应的另一个常用英文 verdict 见第 5 节。 |
| closure / consolidate | 收口 | 归拢到一处收尾:散着的事集中到一处结束,别处不再重复。也指把散落的几份合成一份。 |
| narrow down / converge | 收敛 | 中文黑话指「收拢、变集中」;注意英文 converge 是「趋于一致」,两个意思别混。 |
| retrospective | 复盘 | 事后回头总结:哪步好、哪步砸了。(专指故障后那次的 postmortem 见第 10 节。) |
| force / drive | 倒逼 | 反过来逼着改进:下游的要求逼上游改。 |
| feed back into | 反哺 | 回头补给:先得益的一方反过来帮忙。 |
| deliver / deliverable | 交付 | 交出来的东西、完成的承诺。 |
| output | 输出 | (大厂话)拿出结果、给别人东西。 |
| positioning | 布局 | 提前安排、先占好位置。 |
| play / approach | 玩法 | 具体做法、套路。 |

## 2. 通用软件与架构

| 英文 | 中文 | 通俗解释 |
|---|---|---|
| privilege overreach | 越权 | 干了不该自己干的事:超出自己该管的范围。 |
| currency (metaphor) | 层间的「货币」 | 层和层之间传的东西(比喻)。直接说「层之间传的数据」。 |
| absorbed structurally | 结构性吸收 | 把结构改对之后,某个问题自然不存在了,不用单独修。 |
| dissolved | 溶解 | 顺着某次改动,这段代码直接删掉或消失了。 |
| seeded | 播种 | 预填、打底子:先把一版初始内容放进去。 |
| sandbox | 沙箱 | 应用私有目录、隔离空间:应用自己的地盘,别的应用进不来。 |
| on-device | 端内 | 在手机本地、应用内:不依赖服务器,设备自己完成。 |
| foundation / base | 底座 | 地基、基础:在底下撑着的那层。 |
| (single) owner | 权威属主 | 唯一说了算的地方:一份状态只有它有权改。 |
| survive / persist | 存续 | 还在、保留下来:经历重建之后没有丢。 |
| re-verified | 复验 | 再核对一遍:当时确认过,后来又到代码上再确认一次。 |
| weak compliance | 弱合规 | 合规不到位:勉强算、不太够要求。 |
| strict compliance | 强合规 | 严格满足规定,挑不出毛病。 |
| lost | 已佚 | (书面语)丢了、没留下来。 |
| mechanical | 机械(修改/搬移) | 不动脑子照样子改:每处都一样、不改逻辑。「照葫芦画瓢」。 |
| sentinel value | 哨兵值 | 拿来当记号用的特殊值:见到它就代表某种状态。 |
| implicit contract | 隐式契约 | 没写下来的约定:两边各自默认,没人明说,一变就出事。 |
| contract | 契约 | 约定:两边说好各自怎么做、数据长什么样的规矩。 |
| redundant | 冗余 | 多余的、重复的:删了也不影响功能。 |
| coupling | 耦合 | 两块代码缠在一起:改一个,另一个也得跟着改。 |
| decoupling | 解耦 | 把缠在一起的两块代码拆开,互不牵扯。 |
| high cohesion, low coupling | 高内聚低耦合 | 相关的放一起、无关的不纠缠:改一处只动一个文件。 |
| cohesion | 内聚 | 相关的东西抱成团:一个模块里的代码都为同一件事服务。 |
| god object | 上帝对象 | 什么都管的一个大类:职责多得没法看。 |
| composition root | 组合根 | 全项目唯一「创建对象、接线」的地方:谁依赖谁,只在这里说一次。 |
| service locator | 服务定位器 | 全局服务台:代码不在构造函数里说要什么,运行时伸手到全局去抓(不好的做法)。 |
| temporal coupling | 时间耦合 | 对不对取决于调用顺序:先调谁后调谁错了就出错。 |
| anti-pattern | 反模式 | 反面教材:看着合理、大家都写过,其实有害的写法。 |
| dead code | 死代码 | 没有任何地方使用的代码:留着只会误导人。 |
| legacy | 遗留 | 以前留下来的:老代码、旧机制,不一定坏但常常拖后腿。 |
| footgun | 脚枪 | 一碰就炸的设计:很容易误用、一不留神就出事。 |
| gotcha | 坑 | 容易踩的意外行为:文档没说、一用才发现。 |
| pitfall | 陷阱 | 明知道会有问题、但很容易掉进去的地方。 |
| caveat | 注意事项 | 「但有例外」:前面的结论在这个条件下不成立。 |
| silver bullet | 银弹 | 万能药:一招解决所有问题的东西,通常用来强调「不存在」。 |
| last-resort fallback | 兜底 | 最后接住的那道防线:前面全没接住时,保底不出大事。 |
| (top-level) fuse | 熔断 | 总闸:一出问题就整体停掉(和「单条隔离」相反)。 |
| guardrail | 护栏 | 防止误操作的挡板:错了就拦下来。 |
| gate / gating | 门控 | 前置条件:必须先满足它才能继续。 |
| degrade / fallback | 降级 | 退而求其次:好的用不了,就用差点的凑合,别直接崩。 |
| graceful degradation | 优雅降级 | 出问题时平稳退回:功能缩水,但不崩、不吓人。「平稳退回」。 |
| idempotent | 幂等 | 做多少遍结果都一样:重复执行不会越叠越多。 |
| race condition | 竞态 | 抢跑问题:两件事谁先谁后不确定,结果跟着运气走。 |
| side effect | 副作用 | 顺带干了别的:函数除了给返回值,还偷偷改了别的东西。(Compose 语境的用法另见第 10 节) |
| atomic operation | 原子操作 | 不可分割:要么全做完,要么全没做,不存在做一半。 |
| single responsibility | 单一职责 | 一件事只归一个管:一个类只干一种事。 |
| domain model | 领域模型 | 只描述事实、不管怎么显示的数据。如「性能等级=33,落后正式版 1 级」,不带颜色文案。 |
| mapper | 映射器 | 翻译员:把「事实数据」翻译成「界面要显示的样子」的那段代码。 |
| big-bang switchover | 大爆炸(切换) | 一口气全量换掉。 |
| technical debt | 技术债 | 图快欠下的账:现在省的事,以后要用更多时间还。 |
| code smell | 坏味道 | 代码里「感觉不对劲」的迹象:不一定有 bug,但照这么写下去会出问题。 |
| boilerplate | 样板代码 | 每个地方都要原样抄一遍的固定格式代码,没有信息量。 |
| blocking call | 阻塞调用 | 卡在那干等结果:等待期间这个线程什么别的都干不了。 |
| unguarded | 裸奔 | 没有任何保护就直接执行:一出异常就直接崩。 |
| dual-track | 双轨 | 两套并行的机制:名义上都能用,实际只有一套在干活。 |
| patch | 打补丁 | 事后修一块:不改整体设计,哪儿出问题在哪儿补一块。「补丁机制」= 靠补丁运转的机关。 |
| derivation | 推导 | 从源头算出来:界面显示什么,由设置和数据直接算出来,不靠事后修补。「纯推导」= 同样输入永远同样输出。 |
| relay / router | 中转 / 传话筒 | 传话的中间人:A 和 B 说话要经过 C 转发,C 就成了传话筒。 |
| mutual exclusion | 互斥 | 两个不能同时跑:新的开始,旧的必须先结束。 |
| short-circuit | 短路 | 检查直接跳过:一个条件不满足,后面全不执行。 |
| write-back | 写回 | 把结果写回原处:算完再把值存回去。 |
| snapshot | 快照 | 定格副本:某一瞬间的值,之后外面怎么变它都不跟着变。 |
| invalidate | 失效 | 让缓存作废:宣布旧数据不算数,重新取。 |
| fallback chain | 回退链 | 一条备用链:主用的不行,顺着次序换下一个,如 网络→缓存→内置。 |
| single entry point | 单一入口 | 只有一个门进:所有访问都走同一个地方,规则只写一遍。 |
| dedup / deduplication | 去重 | 去掉重复:正在跑的任务,再来一次触发就忽略。 |
| skip-based dedup | 跳过式去重 | 去重的一种:已经在跑就直接跳过,不打断它(区别于「取消式」:停掉旧的再来)。 |
| orphaned state | 孤儿状态 | 没人认领的状态:留下来了,但没有任何代码负责把它送走或接着用。 |
| residue / residual | 残留 | 上一轮留下的:之前那次运行没清干净的东西。 |
| sticky | 粘住 | 一直停留不动的旧值:新来的读取者拿到的还是它。 |
| terminal state | 终态 | 走到头的稳定状态:停在这里是正常的(如「加载完成」)。 |
| transient state | 瞬时状态 | 中途状态:只该短暂存在(如「加载中」),停在这儿就是出事了。 |
| threshold | 阈值 | 过线的界限:超过它就触发某个行为,如「低于这个版本算过期」。 |
| invasive | 侵入性 | 改动波及面大:为修一个小问题要动一大片。 |
| throwaway | 白做 | 抛弃式的活:明知以后会被重写,还是先做一遍救急。 |
| blast radius | 爆炸半径 | 改动会波及的范围:改这一下,会「炸到」多少别的地方。 |
| leverage | 杠杆/借力 | 名词:用最少的力气撬最大的效果。「高杠杆」= 做一件顶十件。动词:利用现成的东西,不重造。 |
| payoff | 收益 | 回报:做这件事能换来什么好处。 |
| deep dive | 深挖 | 掰开揉碎讲透一个问题:来龙去脉全交代。 |
| (code) walkthrough | 走读 | 顺着代码一行行读过去,边读边查。 |
| inventory | 盘点 | 清点:把所有相关的东西列个清单。 |
| quick reference | 速查表 | 一眼能查到结论的表。 |
| legend | 图例 | 符号说明:表里的 🔴🟡🟢 各代表什么。 |
| flatten | 拍平 | 去掉一层嵌套:目录或结构变浅。 |
| grab-bag | 大杂烩 | 什么都往里放的地方:没有主题、什么都有的包或模块。 |
| first-class | 一等公民 | 同等待遇:这个东西和其他正式成员一样,能直接传、直接存、直接返回。 |
| hygiene | 卫生 | 基础清洁活:「构建卫生」= 把构建配置里的坏习惯清掉。 |
| dormant | 休眠 | 现在没事、一触发就出事:留着没用的开关或配置。没人在意,误碰一下就炸。 |
| scope discipline | 范围纪律 | 只改该改的:不顺手把没说好的事情也改了。 |
| out of scope | 范围之外 | 这次不归它管:明说不处理,免得越改越多。 |
| pre-existing behavior | 原有表现 | 改动之前就是这样的:不是这次修改引入的。 |
| invariant | 不变量 | 任何时候都必须成立的规则:不管代码怎么跑,它不能被破坏。破坏了就是 bug。 |
| exit criterion | 完成标准 | 出口标准:满足什么条件,这一步才算做完。 |
| capstone | 收官 | 收尾之作:最后一块拼图,做完这一阶段就齐了。 |
| unlock | 解锁 | 打开后面的门:做完它,后面的事才能做。 |
| superseded | 被取代 | 有新的顶替它:旧方案作废。 |
| amended | 修订 | 文档改对:把写错或过时的说法改正。 |
| distill | 提炼 | 浓缩成一句话:从一大篇里抽出最核心的。 |
| reconcile | 核对 | 对账:把两边的数或说法对到一致。 |
| de-risk | 去风险 | 先做最怕的测试:把最大的不确定性提前消掉。 |

## 3. 正确性、并发与排错

| 英文 | 中文 | 通俗解释 |
|---|---|---|
| race window | 竞态窗口 | 出事的空档:两件事恰好撞在同一个时间段内才会出问题。 |
| timing-sensitive | 时机敏感 | 看时机:只在「恰好赶在某个时间点」才触发,平时复现不了。 |
| stuck / deadlock | 卡死 | 动弹不得:互相等、或者停在中途没人管,永远到不了终点。 |
| treat the symptom, not the disease | 治标不治本 | 只压住了表现,病根还在,换个场景照样犯。 |
| avoidance, not a fix | 绕开,不是修好 | 只是躲开触发条件:问题本身还在。 |
| counter-evidence / proof by contradiction | 反证 | 从「没坏的地方」倒推:这边正常,说明问题出在那边。 |
| reproduce | 复现 | 让 bug 再发生一次:找到能稳定触发它的步骤。 |
| swallow (an exception) | 吞掉(异常) | 捕获后不处理也不上抛:出错了但没人知道。 |
| cancellation-aware | 感知取消 | 能响应取消:收到「别干了」的信号会停下,而不是装没听见继续跑。 |
| bounded parallelism | 有上限的并发 | 同时干的活有个最大数:不会一口气开几百个任务把机器拖垮。 |
| structured cancellation | 结构化取消 | 协程取消的规矩:任务被取消时要干净利落地退出,把取消信号往上传。 |
| suspension point | 挂起点 | 协程可以被打断的位置:等网络、等文件的地方,取消就发生在这儿。 |
| mid-flight / in flight | 跑到一半 | 正在飞:请求发出去了但还没回来。「跑到一半被扔下」= 取消在半路。 |
| fire-and-forget | 发了就不管 | 事件发出去就完:不等结果,没被收到也不补发。 |
| evaporate | 蒸发 | 凭空消失:事件没被任何人收到,无影无踪。 |
| replay | 重放 | 补听:新来的听众能不能收到「以前发过的事件」。replay=0 = 不能,来晚了就没了。 |
| replayable | 可重放 | 补得了:随时加入的听众,也能拿到最近的那份状态。 |
| subscriber | 订阅者 | 收听的人:盯着数据流、有变化就收到通知的那一方。 |
| self-healing | 自愈 | 自己恢复:不用人工干预,再来一次正常操作就好了。 |
| insidious | 阴险 | 不容易发现的坏:表面看着正常,细看才发现数据不对。坏得不动声色。 |
| narrow trigger surface | 触发面窄 | 条件苛刻:要同时凑齐几个少见的条件才会发作。 |

## 4. 英文术语

| 英文 | 中文 | 通俗解释 |
|---|---|---|
| telemetry | 遥测 | 应用悄悄上报的使用数据、崩溃报告。 |
| observable | 可观察 | 值一变,关注它的人就能收到通知(反义:读一次就完的「哑」配置)。 |
| canonical | 权威的、标准的写法 | 大家都认的那个正版形式。 |
| canary | 金丝雀版本 | 最先放出去试水的试验版,出问题只影响一小撮人。 |
| wiring | 接线 | 对象之间怎么连、谁递给谁。 |
| idiomatic | 符合语言习惯的 | 这语言的老手都这么写。 |
| leverage (v.) | 借力 | 见第 2 节「杠杆」,名词、动词两种用法都在那边。 |
| outlive | 活得比…久 | 它的寿命超过了本该使用它的范围。 |
| as-is / to-be | 现状/目标 | 架构文档里的固定对照:As-Is = 现在实际什么样;To-Be = 想做成什么样。 |
| baseline | 基线 | 拿来对照的「固定参照点」:说体检基于某个提交基线,意思是「查的是那份代码」。 |
| audit | 审计 | 像查账一样逐条核对:把代码和规格一对一对过,偏差记编号。 |
| triage | 分诊 | 字面「伤员分拣」:把问题按轻重缓急分堆,定谁先处理。 |
| scope creep | 范围蔓延 | 要求悄悄越加越多、超出原定范围:每条看着都小,加起来做不完。 |

## 5. 英文难词

| 英文 | 中文 | 通俗解释 |
|---|---|---|
| verdict | 裁定;判决 | 拍板下来的最终结论:法庭上是「判决」,评审、审计里是「最终的裁定」。中文文档里的「裁定」就是它。(另见第 1 节 ruling) |
| semantics | 语义 | 一个词或一段代码「是什么意思」:改措辞不改语义 = 意思没变。别和「语法」(怎么写)搞混。 |
| stale | 过期的;陈旧的 | 数据是旧的:世界已经变了,手里这份没跟着更新。 |
| invariant | 不变量 | 见第 2 节。 |
| reconcile | 对账;调和 | 见第 2 节。 |
| fragile | 脆弱的 | 一碰就碎:前提稍一变化就坏。 |
| brittle | 硬脆的 | 和 fragile 近义:平时正常,条件一苛刻当场断掉。 |
| dormant | 休眠的 | 见第 2 节。 |
| propagate | 传播;传导 | 一个变化顺着链条一路传下去,如异常沿调用链往上抛。 |
| explicit | 明说的 | 白纸黑字写出来的(反义:implicit)。 |
| implicit | 没明说的 | 靠默认约定存在:没写,但大家默认如此,一变就出事。 |
| teardown | 拆除;销毁 | 界面或窗口被系统拆掉重建的那一下。 |
| undermine | 暗中破坏 | 从底下掏空:让某个保证或前提悄悄失效。 |
| conform | 符合;遵从 | 和规格、约定保持一致。 |
| deviate | 偏离 | 和该有的样子不一样:「代码偏离规格」。 |
| collide | 相撞 | 两件事恰好撞在同一个时间点上。 |
| trivial | 不值一提的 | 琐碎、轻松;non-trivial = 不简单、有分量。 |
| arbitrary | 随意的 | 没有道理、随手定的:换个值照样成立,说明现在这个值没什么特殊理由。 |
| authoritative | 权威的 | 大家都认的那个正版来源。 |
| nominal / nominally | 名义上的 | 纸面上是、实际上不是:nominally dual-track = 名义两套,实际一套。 |
| precede | 在…之前 | 排在前面、先发生。 |
| concurrent / concurrently | 并发(同时) | 同一段时间内一起跑(不一定是同一瞬间)。 |
| abandon / abandoned | 半途扔下 | 跑到一半被放弃,不再管它。 |
| premature | 过早的 | 时机没到就做了:premature optimization = 还没量出瓶颈就开始优化。 |
| deviation | 偏差 | 和基准差出来的量:代码与规格的偏差。 |
| anomaly | 异常 | 不合常理、少见的情况。 |
| truncate / truncated | 截断 | 被砍掉一截:字符串太长被掐断。 |
| mutate / mutation | 变异;原地修改 | 直接改原对象本身(而不是另造一个新的)。 |
| respective / respectively | 各自的 | 按前面提到的顺序,一一对应。 |
| deliberately | 故意地 | 明知如此、特意为之(不是疏忽)。 |
| intentionally | 有意地 | 同「故意地」。 |
| presumably | 大概;据推测 | 没验证,但按常理应该是。 |
| terminology | 术语;叫法 | 一套用词:「术语说明」= 先把叫法统一。 |
| dense | 密集的 | issue-dense = 问题扎堆(一个文件里问题很多)。 |
| substantive | 实质性的 | 有实际内容、真伤筋动骨的(不是表面文章)。 |
| breakage | 损坏点 | 断掉的地方:17 处实质损坏。 |
| reproducible | 可复现的 | 能按步骤让 bug 再现。 |
| aggravate / aggravator | 加重;加重因素 | 让原本的问题变得更糟的那个因素。 |
| insidious | 阴险的 | 见第 3 节。 |
| evaporate | 蒸发 | 见第 3 节。 |
| isomorphic | 同构的 | 结构一模一样:问题 A 和问题 B 长得一样。 |
| upcast | 向上转型 | 把子类当成父类来用(技术词,保留)。 |
| enabler | 促成者;帮凶 | 让坏事得以发生的那个机制。 |
| lesion | 病灶 | (医学比喻)主要毛病所在的那个点。 |
| distill | 提炼 | 见第 2 节。 |
| latent | 潜伏的 | 现在没发作、条件到了才发作。 |
| diverge / divergence | 跑偏;分叉 | 两份本该一致的东西越走越不一样。 |
| consolidate | 归拢 | 见第 1 节「收口」。 |
| eradicate | 连根拔掉 | 彻底消灭,不留残余。 |
| intervene | 插进来 | 半路杀出:恰好在这时发生了另一件事。 |
| outright | 直接;干脆 | 毫不客气、一步到位:drops the value outright = 直接把值扔掉。 |
| sole | 唯一的 | 只此一个。 |
| hazard | 隐患 | 有风险的坑:timezone-offset hazard = 时区写法带来的坑。 |
| nit | 小瑕疵 | 鸡毛蒜皮的小问题(nitpick 的缩略)。 |
| pin / pinned | 钉死 | 固定不再变:URL pinned = 写死在那个地址。 |
| revert | 退回 | 把已经做的改动整体撤回来。 |
| glaring | 明晃晃的 | 一眼就能看见的(大得扎眼)。 |
| stacked | 叠加 | 两个问题摞在一起:单看谁都无害,凑一起才发作。 |
| hack | 取巧的歪招 | 能跑但不正经的做法:靠巧合成立,经不起变化。 |
| hardcoded | 写死的 | 直接写在代码或配置里,不做成可配置项。 |
| spec | 规格;规格说明书 | specification 的简称:应用该做什么的那份正式文档。 |
| draft | 草稿 | 还没定稿的版本。 |
| review | 评审;过目 | 别人看一遍你的改动,挑毛病、确认没问题。 |
| golden | 金样本 | 存一份「标准答案」,每次输出和它比,不一样就报错(golden test)。 |

## 6. Android 系统、设备与刷机调试

本应用检测的系统特性和底层机制,加上设备信息和刷机、调试会用到的工具与权限;「三态结果」是本应用自己的判定模型。

| 英文 | 中文 | 通俗解释 |
|---|---|---|
| Project Treble | Treble 项目(无通用中文名) | Android 把「厂商负责的部分」和「Google 负责的部分」拆开的方案:厂商部分单独放一个分区,接口稳定可换。本应用最初就是为查它而做的。 |
| VINTF = Vendor Interface | 供应商接口兼容性框架 | Treble 配套的「对暗号」机制:设备清单报「我提供什么」,框架清单报「我要什么」,两边对上才算兼容。 |
| device manifest | 设备清单 | 设备这边「我有哪些硬件、提供哪些接口」的自述文件,放在 /vendor/etc/vintf/ 下;不同硬件配置可以各有各的。 |
| SKU / vendor SKU = Stock Keeping Unit | 配置型号/厂商配置号 | SKU 字面「库存单位」:同款机型不同配置的细分编号;厂商 SKU 特指记在系统属性里的那份,配置不同,配套的 VINTF 清单也可能不同。 |
| GSI = Generic System Image | 通用系统镜像 | Google 出的「标准版安卓」,刷上就能测兼容性。 |
| ROM | 刷机包 | 字面「只读存储器」;刷机圈借用它指「一整套可刷入的系统」,GSI 就是一种 ROM。 |
| GKI = Generic Kernel Image | 通用内核镜像 | Google 统一维护的内核:内核和厂商改动分家,内核可以单独升级。 |
| kernel / mainline kernel | 内核/主线内核 | 操作系统的「核心发动机」:直接管硬件、进程和内存。主线内核 = 跟着 Google 通用内核走的较新内核,区别于厂商多年不更新的老内核。 |
| A/B (seamless) update | A/B(无缝)更新 | 双槽位方案:系统存两份,更新在后台装到「备用那份」,重启一下就切过去;失败还能切回来。 |
| slot | 槽位 | A/B 方案里系统的两个「座位」:一个在用(名字带 _a 或 _b 后缀),另一个待命。 |
| Virtual A/B | 虚拟 A/B | 新版 A/B:备用槽位不再整块占地方,更新数据用文件形式存,省空间。 |
| retrofit | 翻新/事后加装 | 字面「翻新改造」:出厂时没有的机制,后来通过系统升级补装上(如 Virtual A/B retrofit)。 |
| userspace snapshot | 用户空间快照 | Virtual A/B 的底层手段:像拍照一样把更新前的状态存下来,出问题照着照片还原。 |
| dynamic partitions | 动态分区 | 新的分区方案:分区大小开机可调,不用重刷;所有分区装进一个 super 大分区里。 |
| super partition | super 分区 | 装下一堆逻辑分区的大容器:system、vendor 等都从它里面划分出来。 |
| SAR = system-as-root | system 即根目录 | 字面「系统当根」:把 /system 直接当作根目录 / 挂载的方案;判定时还分「老式(挂在 /system_root)」「新式(/ 本身就是 system)」等变体。 |
| 2SI = two-stage init | 两段式初始化 | 开机分两步走:先在内存盘里起个头,再切到真正的 system 分区继续;是判断新老启动方式的线索之一。 |
| rootfs | 根文件系统 | 「/」所在的文件系统:开机后整个目录树的起点。 |
| AVB = Android Verified Boot | 安卓启动校验 | 开机时逐级「验明正身」:bootloader 验 boot,boot 再验后面的分区,一级级往下,被篡改就拒绝启动。 |
| dm-verity | 磁盘完整性校验 | 挂在分区上的校验机制:运行时每读一块数据都和「签名树」对一遍,分区被改过就直接报错。 |
| APEX = Android Pony EXpress | APEX 模块(无通用译名) | 新格式的系统模块包:像「系统自带的应用」一样,系统组件能单独更新。名字是官方开的玩笑;AOSP system/apex 的 README 原文就写作「EXpress」(EX 大写、press 小写),不是 EXPRESS,别当笔误改。 |
| flattened APEX | 扁平化 APEX | 老式做法:模块不打包成独立文件,摊平成普通目录放系统里(现已淘汰,检测它是为了认老设备)。 |
| Mainline | Mainline 项目(主线化) | 把系统组件做成上面那种可单独更新的模块,让系统能像更新应用一样更新部件。 |
| GPSU = Google Play system update | Google Play 系统更新 | Mainline 面向用户的名字:系统组件更新走 Google Play 下发。 |
| module metadata | 模块元数据 | 系统里记录「装了哪些 Mainline 模块、各是什么版本」的那个特殊包。 |
| VNDK = Vendor NDK | 厂商可用框架库集 | 字面「厂商版 NDK」:系统给厂商部分单独准备的一套稳定系统库,系统升级也不弄坏它;ro.vndk.lite 表示简化模式。 |
| DSU = Dynamic System Update | 动态系统更新 | 不动当前系统,临时挂载一个新系统镜像试用;重启回去还是原来的系统。 |
| linkerconfig / linker namespace | 链接器配置/命名空间 | 系统里「哪部分代码能加载哪些库」的规矩;GSI 兼容性检测就是看厂商段有没有独立的命名空间。 |
| SELinux = Security-Enhanced Linux | 强制访问控制安全模块 | Linux 的安全看门人:每个进程能碰哪些文件全由策略定。Enforcing(强制)= 违规真拦;Permissive(宽容)= 只记日志不拦。 |
| toybox | toybox 工具箱 | Android 自带的命令行小工具集(取代更早的 toolbox),toybox --version 就能查版本。 |
| WebView | 内嵌浏览器引擎 | 应用里显示网页用的浏览器内核组件,由系统统一提供和更新。 |
| Trichrome / Monochrome | 三色/单色套件 | 字面是「三色/单色」,实为 WebView 的打包方案:Trichrome 把 WebView、浏览器、公共库拆成三个包配套升级;Monochrome 合成一个包。 |
| FDE = Full-Disk Encryption | 全盘加密 | 老式加密:整块存储一次性加解密,解锁前什么都干不了。 |
| FBE = File-Based Encryption | 文件级加密 | 新式加密:每个文件单独加密、分「解锁前/后」两把钥匙;重启没解锁也能接电话、收通知。 |
| Go edition | Go 版(轻量版) | 低内存设备的精简 Android:系统和预装应用都换成省内存的版本。 |
| SDK extension | SDK 扩展 | 版本之间的「补充包」:新功能不等年度大版本,先按扩展版本号发出来(Android 11 起有)。 |
| media performance class | 媒体性能等级 | 给「播放、录制有多流畅」定的等级数字:数字越大,媒体体验越好。 |
| security patch level | 安全补丁级别 | 系统内置的安全修复「补到了哪年哪月」:越新,堵上的漏洞越多。 |
| Build ID | 构建 ID | 一次系统构建的编号,通常是构建日期的编码;Android 8 起日期编码格式有变。 |
| build fingerprint | 构建指纹 | 系统构建的「全名」:品牌、设备、版本等信息拼成的一长串,像系统的身份证号;每个分区还可以各有各的指纹。 |
| SoC = System on a Chip | 片上系统 | 一颗芯片打包 CPU、GPU 等(手机的「大脑」);报告里指主控芯片型号。 |
| partition | (系统)分区 | 存储里各管一摊的区域:boot(内核)、system(系统)、vendor(厂商)、product(预装)、odm(机型定制)、system_ext(系统的延伸)、*_dlkm(内核模块)等。 |
| binder | binder 进程间通信 | Android 进程之间说话的「总机」;主通道是 /dev/binder,另有 hwbinder(硬件)、vndbinder(厂商)两条专线。协议版本还能看出系统位数:7 = 32 位,8 = 64 位。 |
| binderfs | binder 文件系统 | 按需新开 binder 通道的新管理方式:不再局限于写死的几个设备节点。 |
| ABI = Application Binary Interface | 应用二进制接口 | 编译产物的「机器方言」规格:arm64-v8a、armeabi-v7a 各是一套,设备支持哪套决定装哪个包;也用来描述系统进程的位数。 |
| ioctl | 设备控制调用 | 字面「输入输出控制」:程序给设备驱动下指令的通用接口;查 binder 版本就用它。 |
| JNI = Java Native Interface | Java 本地接口 | Java/Kotlin 代码和 C/C++ 代码互相调用的桥;本应用查 binder 位数就是 Kotlin 经它调 C++。 |
| NDK = Native Development Kit | 原生开发工具包 | 写 C/C++(原生)代码用的整套工具:编译器、头文件、系统库。 |
| getprop / build.prop | 属性读取命令/属性文件 | getprop = 读系统属性的命令;build.prop = 存属性的文件。 |
| system property | 系统属性 | 系统全局的「键: 值」对(如 ro.treble.enabled);ro 开头 = 只读,开机后不变。本应用的取数来源之一。 |
| HAL = Hardware Abstraction Layer | 硬件抽象层 | 把「硬件怎么控制」包成统一接口:系统上层对着接口写,不用管各家硬件的差异。 |
| CTS = Compatibility Test Suite | 兼容性测试套件 | Google 出的兼容性「考试题库」:设备全答对才有资格预装 Google 服务;Build ID 的日期格式也源自它的约定。 |
| Shizuku | Shizuku(无译名) | 不刷机、不完整 root,借 ADB 权限让普通应用能干高级操作的机制。 |
| ADB = Android Debug Bridge | 安卓调试桥 | 电脑和手机对话的通道:装应用、发命令都走它;「ADB 认证」= 每台电脑要先拿到手机授权才能连。 |
| root | root 权限 | 系统里的「超级管理员」身份:有了它能改系统任何角落;本应用不需要它,严格只读。 |
| three-state result | 三态结果 | 每个检测条目的结论只有三种:支持 / 不支持 / 未知 —— 查不出来就明说「未知」,绝不瞎猜。 |
| preview (Beta / Canary) | 预览版 | 正式版发布前的测试版:Beta = 公开测试,功能基本齐;Canary = 金丝雀版,最新也最不稳。 |
| OTA = Over-The-Air | 空中升级 | 手机联网自动下载系统更新。 |

## 7. 应用架构与技术栈

| 英文 | 中文 | 通俗解释 |
|---|---|---|
| MVVM = Model-View-ViewModel | 模型-视图-视图模型 | 一种界面代码三层分法:视图(界面)只管显示,视图模型管界面要的数据和状态,模型管数据本身,各干各的。 |
| UDF (unidirectional data flow) | 单向数据流 | 数据只往一个方向流:操作从界面传向数据层,新数据再流回界面,不存在两头互相改。 |
| SSOT (single source of truth) | 唯一数据来源 | 一份状态只有一个地方说了算:别处要用都找它拿,自己不私存一份。 |
| ViewBinding | 视图绑定 | 编译器自动生成的「找界面控件」代码:不用手写 findViewById,控件引用还有类型检查。 |
| ViewModel | 视图模型 | 界面数据的「保险柜」:放里面的数据,转屏这类界面重建后还在。 |
| StateFlow | 状态流 | 一个「会主动通知的值盒子」:值一变,盯着它的界面都收到新值;新来的界面先拿到当前值。 |
| SharedFlow | 共享流 | 一条广播频道:事件发出去,正在收的人才能收到,来晚了听不到(除非设了重放缓存)。 |
| flowWithLifecycle | 随生命周期收集 | 收数据流时遵守界面状态:界面到前台才收、离开就停,回来接着收。 |
| lifecycleScope / viewLifecycleOwner | 生命周期作用域/所有者 | 「跟着界面走」的许可证:挂在它名下的任务,界面销毁时自动取消。 |
| Fragment | 片段 | 界面的一块拼图,拼在 Activity 里;有自己的生命周期,被系统拆掉重建是常事(数据因此要放 ViewModel)。 |
| Repository | 仓库(数据层) | 取数的门面:界面要数据找它,它决定问哪个来源(系统、文件、网络),界面不关心细节。 |
| DataSource | 数据源 | 真正取数的一层:一个来源一个类(系统属性、shell 命令、网络下载),仓库在它们之上做汇总和判断。 |
| ListAdapter | 列表适配器(自动差分版) | 列表的内容管理器:新旧两份列表给它,它自己算出哪几条变了,只刷新变的那几条。 |
| DiffUtil / diff | 差分工具/差分 | 把两份列表逐条对比、找出差异的算法;本应用按「key 相同就算同一条」来认。 |
| RecyclerView | 可复用列表控件 | 长列表控件:滑出屏幕的条目回收复用,列表再长也不卡。 |
| Adapter | 适配器 | 把数据「翻译」成列表条目视图的中间人。 |
| inflate | 填充(布局) | 把 XML 布局文件变成内存里真实的控件对象。 |
| viewModelFactory / CreationExtras | 视图模型工厂/创建附件 | 造 ViewModel 的「代工厂」和「随单备注」:工厂按附件里提供的依赖(比如仓库对象)把 ViewModel 造出来。 |
| SavedStateHandle | 已保存状态句柄 | 进程被杀后还找得回的小抽屉:ViewModel 往里放关键值,应用重启后接着用。 |
| coroutine | 协程 | 可暂停的轻量任务:等网络、等文件时把线程让出去,不干耗;取消信号也能一路传到。 |
| Dispatchers.IO / Default | IO/默认调度器 | 任务的「分派窗口」:IO 窗口专管磁盘、网络这类慢活,默认窗口专管纯计算;放错窗口会互相卡。 |
| withContext | 切换上下文 | 把一段活儿挪到指定调度器上干,干完回到原来的地方。 |
| Ktor / HttpClient | 网络客户端库 | Kotlin 的网络请求库;本应用全进程共用一个 HttpClient 实例,不每次请求新建。 |
| libsu / Shell | shell 执行库 | 在应用里执行命令行命令的库;非 root 模式下按 shell 用户权限跑,是本应用的取数手段之一。 |
| kotlinx.serialization | Kotlin 序列化库 | 数据类和 JSON 互相转换的官方库:类标上 @Serializable 即可;ignoreUnknownKeys = 遇到没见过的字段不报错。 |
| SharedPreferences | 键值对存储 | 老式轻量存档:一本「键: 值」小本本,读写按整本算。 |
| DataStore | 新式数据存储 | SharedPreferences 的继任者:基于协程,写不卡线程,变化有通知。 |
| PreferenceFragmentCompat | 设置页片段 | 系统现成的「设置项列表」框架:开关、单选这些行不用自己画。 |
| Material You / dynamic color | 动态取色 | 跟着用户壁纸自动生成的应用配色(Material 3 的招牌功能)。 |
| DayNight / dark mode | 昼夜主题/深色模式 | 浅色、深色两套主题,跟随系统切换。 |
| edge-to-edge | 边到边(全屏铺满) | 内容画到状态栏、导航栏底下,再按安全区留出空隙,画面铺满又不挡内容。 |
| WindowInsets | 窗口安全区信息 | 屏幕上「系统占了哪些边」的数据:状态栏、导航栏、挖孔摄像头的位置,内容要躲开。 |
| predictive back | 预测性返回 | 新版返回手势:往回滑先给「预览」要返回到哪,松手才真退。 |
| pull-to-refresh | 下拉刷新 | 列表顶部往下拉、触发重新加载的手势。 |
| BottomNavigationView | 底部导航栏 | 屏幕底部的标签栏;本应用四个页面靠它切换(页面常驻内存,用显示/隐藏切换)。 |
| hidden API / reflection | 隐藏 API/反射 | 系统不公开给应用的接口,靠「按名字找到方法直接调」的反射去用;系统版本一变就容易断。 |
| sealed State | 密封状态(接口) | 列表页的三阶段:未初始化 → 加载中 → 完成;「密封」= 状态列举齐全,编译器帮着查漏。 |
| loadJob dedup | 加载去重 | 界面重建带来的重复加载,用「已有任务在跑就直接跳过」挡掉。 |

## 8. 构建系统与依赖

| 英文 | 中文 | 通俗解释 |
|---|---|---|
| Gradle | 构建工具 | Android 官方构建系统:编译、打包、跑测试都靠它;./gradlew 就是叫它干活。 |
| Kotlin DSL (.kts) | Kotlin 构建脚本 | 用 Kotlin 写的构建配置(build.gradle.kts):比老式 Groovy 写法多代码提示和类型检查。 |
| included build / build-logic | 内嵌构建/构建逻辑区 | 仓库里再套的一个小构建:约定插件集中在 build-logic,各模块来引用,配置只写一遍。 |
| convention plugin | 约定插件 | 「把配置打包成插件」:SDK 版本、编译选项写一次,各模块一行应用,不用各抄一份。 |
| version catalog | 版本目录 | 集中登记依赖坐标和版本的清单(TOML 文件);本仓库拆成五份目录,没有默认的 libs。 |
| flavor | 变体(风味) | 字面「风味」:同一套代码产出的不同发行版本;本应用按统计服务分 Foss 和 Firebase 两个变体。 |
| dimension | 变体维度 | 给变体分组的「轴」:同一根轴上多选一(本仓库只有 IssueTracker 一根轴)。 |
| buildType | 构建类型 | debug/release 两档:debug 方便调试(包名加 .debug 后缀),release 做压缩和签名。 |
| sourceSets | 源集 | 告诉构建系统「代码和资源在哪儿」的登记表;本仓库的 res 目录放在 java 包路径里,靠它注册。 |
| configuration cache | 配置缓存 | 把「配置阶段的计算结果」存下来:下次构建直接复用,配置阶段整个跳过,明显变快。 |
| build cache | 构建缓存 | 把编译产物存下来:输入没变的任务直接拿缓存,不再重编。 |
| parallel build | 并行构建 | 互不依赖的模块同时编译,多核吃满。 |
| Gradle daemon | 构建常驻进程 | 一直待命的构建进程,省掉每次启动的开销;本仓库还单独锁定了它的 JVM 版本。 |
| toolchain / foojay | 工具链/自动配 JDK | 按声明自动下载相配的 JDK(经 foojay 解析);本仓库声明 JDK 25,谁来构建都一致。 |
| AGP = Android Gradle Plugin | Android 构建插件 | Gradle 里的 Android 专家:打包 APK、处理资源那一套都归它管。 |
| compileSdk / minSdk / targetSdk | 编译/最低/目标 SDK | 三个 API 级别:编译时用的最新头文件、能装的最低系统版本、按哪个版本的行为标准运行。 |
| buildTools | 构建工具版本 | 编译打包用的底层工具集版本号,全仓库只在一处声明。 |
| CMake | C/C++ 构建系统 | C/C++ 世界的构建指挥:NDK 编译原生代码由它安排。 |
| externalNativeBuild | 原生构建声明 | Gradle 里「原生部分交给 CMake」的配置块。 |
| R8 / minify | 代码压缩混淆器 | release 打包时的「打包机」:删掉没用到的代码、把名字改短,包更小也更难被反编译。 |
| resource shrinking | 资源收缩 | 把没被引用的图片、字符串等资源从包里剔掉。 |
| ProGuard rules | 保留规则 | 告诉 R8「哪些不许删、不许改名」的例外清单;靠反射用的类必须写进来。 |
| signing / keystore | 签名/密钥库 | 给安装包盖「作者章」:keystore 是私章材料,v1–v4 是历代盖章方案;签名材料永不进仓库。 |
| local.properties | 本地配置文件 | 每台机器自己一份的配置(SDK 路径、签名密码),已被 gitignore。 |
| core library desugaring | 核心库脱糖 | 把新版 Java 写法「降级翻译」给老系统:minSdk 23 的设备也能用上新 API。字面「去糖」= 把新语法糖还原成老写法。 |
| BuildConfig | 构建配置类 | 构建时自动生成的常量类:把 git 分支这类信息打进包里。 |
| localeFilters / generateLocaleConfig | 语言过滤/语言清单 | 只保留声明的语言资源,并生成「应用支持哪些语言」的清单,配合系统的按应用设置语言。 |
| typesafe project accessors | 类型安全项目访问器 | 用 projects.app 这样的属性引用兄弟模块,代替手写字符串 ":app"。 |
| dependency / transitive dependency | 依赖/传递依赖 | 项目借用的别人写的代码库;传递依赖 = 依赖自己又带进来的依赖(一串套娃)。 |
| semantic versioning | 语义化版本 | 版本号「主.次.修」三段:不兼容改动升主号,加功能升次号,修 bug 升修订号。 |

## 9. 工具链与流程

| 英文 | 中文 | 通俗解释 |
|---|---|---|
| GitHub Actions | 自动化流水线 | 仓库自带的机器人:推代码、发 PR 时自动跑编译检查(剧本在 .github/workflows 里)。 |
| workflow / runner | 工作流/运行器 | workflow = 一次自动化的「剧本」;runner = 实际照剧本跑的机器。 |
| Dependabot | 依赖升级机器人 | 盯着依赖新版本的机器人:有更新就自动开 PR 提醒。 |
| Android Lint | 静态检查 | 不运行代码、光读代码就挑毛病:拼错、废弃 API、资源问题等。 |
| unit test | 单元测试 | 针对一个函数、一个类的自动化小考:跑得快,不用启动整个应用。 |
| Gradle wrapper | Gradle 包装器 | 仓库里的 gradlew 脚本加版本声明:谁检出仓库都用同一个版本的 Gradle,不用自己装。 |
| build scan / Develocity | 构建扫描报告 | 一次构建的「体检单」:每个任务跑了多久、哪里慢,发布到网上可查(Develocity 是平台名)。 |
| conventional commits | 约定式提交(语义化提交) | 提交信息带类型前缀的行规:fix: 修 bug、feat: 加功能、docs: 文档、chore: 杂务,一眼看出每笔改动的性质。行业标准名叫 Conventional Commits(约定式提交),本仓库文档里也口语化称「语义化提交」。 |
| branch (develop / master) | 分支 | 平行的开发线:develop 是日常主线(PR 都对它提),master 只收 lld.json 数据更新,定期并回 develop。 |
| google-services.json | Firebase 配置文件 | Firebase 变体的身份配置:含项目标识,属本地文件,gitignore 不提交。 |
| Crashlytics | 崩溃上报服务 | Firebase 的崩溃收集器:应用一崩,现场自动发回后台(Firebase 变体专属)。 |
| LeakCanary | 内存泄漏检测器 | debug 构建自带的探测器:该回收的对象没回收时,当场弹通知(图标是只金丝雀)。 |
| StrictMode | 严格模式 | 开发期的「较真检查」:主线程干慢活、忘了关资源这类毛病,直接在日志里报警。 |
| Baseline Profile | 基线配置文件 | 预先录好「启动必经的代码路径」,装机时提前编译,换更快的启动。 |
| Macrobenchmark | 宏基准测试 | 从外部整体测应用性能(启动耗时、滚动流畅度)的测试框架;和逐个函数的「微基准」相对。 |

## 10. 通用 Android 开发行话(扩展层)

本仓库暂时没用到、但写报告聊到行业和未来规划时常见的词。

| 英文 | 中文 | 通俗解释 |
|---|---|---|
| Compose | 声明式 UI 框架 | Android 新界面写法:只描述「界面长什么样」,怎么变交给框架;本应用现为 XML + ViewBinding,迁移在路线图上。 |
| composable | 可组合函数 | Compose 的界面积木:标了 @Composable 的函数就是一块。 |
| recomposition | 重组 | 数据一变,受影响的积木重新画一遍;乱重组会白白浪费性能。 |
| state hoisting | 状态上提 | 把状态挪给父层、自己只管显示:组件变成「无记忆」的纯展示件,好复用、好测试。 |
| remember / rememberSaveable | 记住/可保存记忆 | 在函数里临时记住一个值(重组间不丢);Saveable 版连界面重建后都找得回。 |
| side effect / LaunchedEffect | 副作用/进入效应 | 在界面描述里「顺便干影响外部的事」(比如发请求);LaunchedEffect = 进界面才启动、离开就取消的挂载方式。(通用含义见第 2 节) |
| expect / actual | 占位与兑现声明 | 跨平台(KMP)的约定写法:公共代码写 expect 立下规矩,各平台写 actual 兑现。 |
| commonMain / source set | 公共源集/平台源集 | 跨平台项目按平台分代码目录:commonMain 放共享逻辑,androidMain、iosMain 放各平台专属。 |
| ANR = Application Not Responding | 应用无响应 | 主线程被卡住太久,系统弹出「等待/关闭」对话框 —— 用户眼里就是「卡死了」。 |
| jank | 卡顿 | 一帧没赶上屏幕刷新,画面顿了一下。 |
| dropped frame | 掉帧 | 该画的帧没画出来,动画看着不连贯。 |
| cold start | 冷启动 | 应用进程从零开始启动:没有任何现成缓存,是最慢的那种启动。 |
| memory leak | 内存泄漏 | 用完的对象该释放没释放,一直占着内存;攒多了应用越来越卡,最后被系统杀掉。 |
| GC = garbage collection | 垃圾回收 | 自动清扫不再使用的内存;清扫集体开工时,界面可能顺带卡一下。 |
| overdraw | 过度绘制 | 同一块像素上叠了好几层画面,底下几层白画了。 |
| regression | 回归 | 本来好的功能又坏了:新改动带来的「退步」。 |
| flaky test | 不稳定测试 | 时过时不过的测试:结果看运气,说明不了代码好坏。 |
| code coverage | 覆盖率 | 测试跑过的代码占全部代码的比例:比例高不等于没问题,比例低多半没测透。 |
| snapshot test | 快照测试 | 给界面存一张「标准照」,以后每次渲染都和它比对。 |
| instrumentation test | 设备端测试 | 跑在真机或模拟器上的测试(和纯电脑上的单元测试相对)。 |
| AAB = Android App Bundle | 应用包(上传格式) | 上传 Google Play 的新格式:商店按设备配置裁剪后再分发,代替直接发 APK。 |
| staged rollout | 分阶段发布 | 新版本先放给一小撮用户,没问题再逐步放大比例。 |
| dogfood | 内部自用 | 自己团队先用自家产品,亲身体验毛病(典故是「吃自家狗粮」)。 |
| feature flag | 功能开关 | 上线后还能远程开关的功能闸门:出问题一键关闭,不用发新版本。 |
| hotfix | 紧急修复 | 线上着火时的快速补丁版本,不走常规排期。 |
| deprecate | 弃用 | 官方宣布「别再用了」:还能跑,但已被替代,将来会删。 |
| sunset / EOL | 下线/停止维护 | 官方宣布寿终正寝:不再修、不再更,该准备搬家了(EOL = end of life)。 |
| RFC = Request for Comments | 征求意见稿 | 大改动前的公开草案:先晒方案收意见,再动手。 |
| upstream / downstream | 上游/下游 | 依赖关系里的两方:库是应用的上游,应用是它的下游;上游一改,下游跟着动。 |
| fork | 分叉 | 把仓库复制一份自成一支、独立演进。 |
| cherry-pick | 摘樱桃 | 从别的分支单摘一笔提交过来,像只挑熟的那颗。 |
| rebase | 变基 | 把自己的提交「搬到」新底座上重放一遍,历史变成一条直线。 |
| LTS = Long-Term Support | 长期支持版 | 承诺维护很多年的稳定版本,求稳的项目选它。 |
| breaking change | 破坏性改动 | 升级后老代码编不过、或行为变了的改动,必须显眼标出。 |
| migration | 迁移 | 从旧方案搬到新方案:代码、数据或依赖的搬家工程。 |
| technical spike | 技术验证 | 动手前先做的小实验:把最大的不确定点趟一遍,结论给正式决策用。 |
| YAGNI = You Aren't Gonna Need It | 你不会需要它 | 没有真实需求就别提前做:提前做的大多半白做。 |
| bikeshedding | 自行车棚争论 | 在鸡毛蒜皮的选择上吵个没完,真正的大事反而没人管(典故:核电站方案没人提意见,车棚颜色人人有话说)。 |
| rubber duck debugging | 橡皮鸭调试 | 对着小玩具把问题讲一遍,讲着讲着自己想通了。 |
| postmortem | 故障复盘 | 出事之后写的「验伤报告」:怎么发生的、怎么修的、怎么防再犯(和第 1 节「复盘」相比,专指出故障后的那次)。 |

## 11. 缩写与编号

编号在所有文档里通用,互相引用时直接写编号。领域和工具的缩写(VNDK、APEX、ROM、AGP 等)在前面各节的词条里就地展开,这里只收编号和通用缩写。

| 英文 | 中文 | 通俗解释 |
|---|---|---|
| FR-x = Functional Requirement | 功能需求 | 第 x 条:应用「必须会做什么」。 |
| NFR-x = Non-Functional Requirement | 非功能需求 | 做得好不好(快不快、稳不稳、安不安全),不是做什么。 |
| AC-x = Acceptance Criteria | 验收标准 | 这条需求怎么才算做完,一条条可检查。 |
| ADR-x = Architecture Decision Record | 架构决策记录 | 把「技术上为什么这么定」写下来,免得以后忘了当初为什么。 |
| AR-xx = Architecture Review | 架构体检 | 架构体检报告的原创发现编号。 |
| C / R / A = Correctness / spec-code gap / Architecture | 正确性/规格偏差/架构 | 2026-09-06 规格对照审计的发现编号:C=正确性 bug,R=代码和规格对不上,A=架构/技术栈/测试缺口。 |
| BL-x = Backlog | 待办 | 确定要做、先放一放的。 |
| RM-x = Roadmap | 路线图 | 以后再说、还没排期的。 |
| Q-x = open Question | 开放问题 | 还没拍板的问题。 |
| NG-x = Non-Goal | 非目标 | 本版本明说不做的事:不是忘了,是决定不干。 |
| D1~D5 = Defect | 缺陷编号 | 已删除的旧英文规格的缺陷编号,如今只在编号映射表里出现。 |
| S/U/O = SSOT / UDF / Other | SSOT/UDF/其他 | SSOT/UDF 报告的发现编号:S=唯一数据来源类,U=单向数据流类,O=其他。 |
| P0/P1/P2 = Priority | 优先级 | P0=必须有;P1=应该有;P2=有更好。 |
| TBD = To Be Determined | 待定 | 还没定。 |
| PR = Pull Request | 合并请求 | 请别人 review 我的改动,没问题就合进主分支。 |
| CI = Continuous Integration | 持续集成 | 每次提交自动编译、测试的机器人。 |
| DI = Dependency Injection | 依赖注入 | 对象要什么,由外面递给它,而不是自己伸手拿。 |
| KMP / CMP = Kotlin / Compose Multiplatform | Kotlin/Compose 跨平台 | Kotlin 和 Compose 的跨平台方案:一套代码跑多个系统。 |
| FOSS = Free and Open-Source Software | 自由开源软件 | 通用含义:自由和开源软件,源码公开、可自由使用和修改。本项目语境:指 foss 发布变体 —— 不含专有组件、不收集数据的版本。 |
| § | 章节号 | 规格文档里指向小节的记号(如 §1.3 = 第 1 章第 3 节)。 |
| API = Application Programming Interface | 应用编程接口 | 系统开放给应用的「服务窗口」;API level = 窗口的代数,数字越大越新。 |
| SDK = Software Development Kit | 软件开发工具包 | 面向某平台或服务做开发用的工具和文档整套。 |

---

写完任何文档后:用第 1 节的「英文」「中文」两列做关键词全文搜一遍,搜到就按「通俗解释」列改写(标题行和代码块除外);第 2–3 节过一遍,看该括注通俗解释的地方注了没;第 4 节起当词典查,中英拿不准的词按「英文」列查。新抓到的词随时补进表里。
