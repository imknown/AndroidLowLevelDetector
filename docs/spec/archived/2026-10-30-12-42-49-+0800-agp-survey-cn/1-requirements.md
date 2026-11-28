# 需求文档: agp-survey

> **状态**: living. 流程见 [13-工作流](../../../agents-cn/13-工作流.md).  
> 追补目录: 任务实际发生于 2026-10-30 到 2026-11-26, 当时没走 spec 流程; 本目录 2026-11-28 追补建成,  
> 时间戳取自建目录那笔提交 `720c9421` (2026-10-30 12:42:49 +0800).  
> 原始需求三句为负责人 2026-11-28 凭当时的记忆补写.  
> 范围按负责人 2026-11-28 划定: 本 spec 的实施提交从 `e584ae28` 起, 到 `b4a011ca` 止;  
> 预研四轮的调查笔记收在本目录 (组织方式见 [2-design.md](2-design.md) 的 预研设计 一节);  
> 版本号升级本身的常规依赖更新 (例如 `1a38bc98`) 不在范围.

## 原始需求 (负责人补写于 2026-11-28, 追补场景)

问题: 因为要升级 AGP 9.5.x 最新版, 甚至为后续 9.6.x, 10.0.x 做技术预研. 并检查 当前项目 是否符合 AGP 9.0+, Gradle 9.0+ 的最佳实践. 此外我记得 AGP 还有些问题, 在等待官方修复 (https://issuetracker.google.com/issues/560282299, https://issuetracker.google.com/issues/572377862)
打算: 做了多个 Agent/Model 的 大量调查, 权衡利弊
风险: 版本跨度比较大, 且涉及到 Release, 需要大量耗时验证, 此外 AGP 和 Gradle 版本升级 本身 兼容性 风险就很高

## 需求条目

本节收实施与归档的交付条目; 预研阶段的产出 (四轮调查笔记) 是需求与设计的依据材料, 收在本目录.  
docs/requirements-cn 的全局需求正文不覆盖本任务; 验收标准来源一律标明.  
追补场景下, 验收按 2026-11-28 对历史提交与现存文件的核对执行, 结果记在 [3-tasks.md](3-tasks.md) 的验收节.

**R-1 buildConfigField 迁到 variant API, 实验开关删掉**  
`GIT_BRANCH` 等取值不再走 `defaultConfig` 的急切 `buildConfigField()` 写法, 改经  
`variant.buildConfigFields` 惰性写入; 实验开关 `android.enableBuildConfigAsBytecode` 从  
`gradle.properties` 删掉.  
来源: 原始需求的 "问题" (升级与最佳实践) + Doubao 轮 05-适配判定 的必改判定 (官方 roadmap 的 10.0 移除表  
把 `buildConfigField()` 列入, 替代写法 `variant.buildConfigFields.put(...)`, 仓库命中一处); 开关删除是迁移的  
连带要求 (`e584ae28` 提交信息: 字节码路径不写 DSL 外的字段, AGP 9.5.2, b/224758957, 开关开着会静默丢  
GIT_BRANCH).  
验收: `gradle.properties` 无该开关; 约定插件里 GIT_BRANCH 经 `variant.buildConfigFields` 写入,  
`defaultConfig` 里的急切写法消失; 源码 BuildConfig 的字段集合不变 (开关默认 false, 删行不改行为;  
`e584ae28` 提交信息).

**R-2 release 收缩改由 optimization 块驱动**  
app 的 release 段用 `optimization` 块一处开关覆盖代码收缩, 资源收缩与默认 keep 规则文件;  
`isMinifyEnabled`, `isShrinkResources` 与 `proguardFiles` 的旧组合消失.  
来源: 原始需求的 "问题" (最佳实践) + Doubao 轮 06-可选改进 第 4 项 (可做可不做档, AGP 9.3 起可用).  
验收: `app/build.gradle.kts` 的 release 段只有 `optimization` 块, 三个旧写法 grep 为零.

**R-3 Crashlytics 规则进 flavor keepRules 源集**  
Firebase 的 Crashlytics keep 规则放进 `src/Firebase/keepRules/`, 由 AGP 直接按变体读取;  
两个 ad-hoc `.pro` 文件删除, Firebase 约定插件不再追加规则文件; Foss 变体不含 Crashlytics 规则.  
来源: 原始需求的 "问题" (最佳实践) + Doubao 轮 06-可选改进 第 5 项 (可做可不做档, AGP 9.3 起可用).  
验收: `src/Firebase/keepRules/` 下规则文件存在, `app/proguard-firebase-rules.pro` 与  
`app/proguard-rules.pro` 不存在, 约定插件无追加写法.

**R-4 flavor 源集目录大小写与 flavor 名一致**  
`src/foss` 改成 `src/Foss` (仅目录大小写, 内容不变), 使大小写敏感文件系统 (Linux CI) 也能解析该源集.  
来源: 实施过程中的跟进修正 (`fe538111` 同一 arc 内发现: AGP 按 flavor 名逐字建源集根, 小写目录在 Linux CI  
被静默跳过, strings.xml 输给主源集; 依据 `4041ab5a` 提交信息).  
验收: `app/src/Foss/` 存在, `app/src/foss/` 不存在, 文件内容与改名前逐字一致.

**R-5 调查笔记失效留痕不回填**  
三项构建改造造成调查笔记里 18 处行号指针与陈述失效; 负责人 2026-11-26 定的口径是笔记按当时的快照保留,  
一处不回改, 失效点逐条记进 issue 条目并按已了结归档.  
来源: 负责人 2026-11-26 的口径 (原文记在 issue #71 的结论节).  
验收: issue #71 列全 18 处失效点并附现状; 四份调查子目录的结论与行号指针原文未动;  
issues-cn 的 README 索引加了对应行.

## 技术补充项 (不许替代主标准)

**补-1 调查目录归档保存**  
任务结束后整目录挪进 archived 按位置成 record, 之后的失效指针按死链就地修, 内容不改.  
来源: [13-工作流](../../../agents-cn/13-工作流.md) 的 归档 节与 01-聊天与文档内容规则 的状态模型  
(工作流要求, 原始需求没写).  
验收: 2026-11-26 整目录挪进 `docs/dev/archived/` (`8e2f3054`), 2026-11-28 随追补挪入本 spec 目录  
(位置变化, 内容不改, 相对链接补层).
