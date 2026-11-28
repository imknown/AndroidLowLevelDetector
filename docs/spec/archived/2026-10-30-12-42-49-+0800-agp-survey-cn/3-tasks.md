# 任务清单: agp-survey

> **状态**: living. 流程见 [13-工作流](../../../agents-cn/13-工作流.md).  
> 追补目录: task 1 到 task 7 是实际发生的历史, 范围按负责人 2026-11-28 划定 (`e584ae28` 起到  
> `b4a011ca` 止); 勾选框按 2026-11-28 的核对打勾; 每 task 一笔提交, 提交号写在各 task 里.  
> 预研四轮 (2026-10-30 到 2026-11-25) 不占 task: 它们是需求与设计的依据材料, 记录见  
> [2-design.md](2-design.md) 的 预研设计 一节.

## 任务清单

- [x] **task 1 删实验开关** (2026-11-26 22:05:13 +0800, 提交 `e584ae28`).  
  凭什么是独立 task: 单文件两行删除, 开关默认 false, 删行不改行为, 独立可验.  
  目标: `gradle.properties` 删掉 `android.enableBuildConfigAsBytecode` 与它下面那行空注释.  
  动哪些文件: `gradle.properties` (-2).  
  怎么验证 (当时): 提交信息没附验证声明; 行为不变的依据写在提交信息里 (开关默认 false,  
  保持源码 BuildConfig 路径).  
  对应 design: 实施点 1; 落实 R-1.

- [x] **task 2 GIT_BRANCH 迁 variant API** (2026-11-26 22:05:22 +0800, 提交 `36cfdcd6`).  
  凭什么是独立 task: 单插件的取值路径迁移, 独立可编译可验; 与 task 1 合起来才是完整的 R-1.  
  目标: `defaultConfig` 里的急切 `buildConfigField()` 换成 `androidComponents.onVariants { }` 里的  
  `variant.buildConfigFields.put(...)`; 版本, 时间戳与分支三个取值挪进 `configureName()` 顶部,  
  defaultConfig, archivesName 与变体回调各读同一份来源.  
  动哪些文件: `build-logic/.../app/AndroidApplicationConventionPlugin.kt` (+25/-12).  
  怎么验证 (当时): 提交信息没附验证声明; 追补验收按 2026-11-28 的现状核对 (见验收节).  
  对应 design: 实施点 1; 落实 R-1.

- [x] **task 3 release 收缩走 optimization 块** (2026-11-26 22:05:31 +0800, 提交 `2d9afce9`).  
  凭什么是独立 task: 单文件单段改写, 独立可编译可验.  
  目标: app 的 release 段以 `optimization.enable` 一处开关覆盖代码收缩, 资源收缩与默认  
  `proguard-android-optimize.txt`, 删掉 `isMinifyEnabled`, `isShrinkResources` 与 `proguardFiles` 列表.  
  动哪些文件: `app/build.gradle.kts` (+4/-7).  
  怎么验证 (当时): 提交信息没附验证声明; 06-可选改进 第 4 项风险栏要求的行为比对落在本地, 提交信息未记.  
  对应 design: 实施点 2; 落实 R-2.

- [x] **task 4 Crashlytics 规则进 keepRules 源集** (2026-11-26 22:05:42 +0800, 提交 `fe538111`).  
  凭什么是独立 task: 规则文件迁移与插件删追加必须同一笔, 拆开会出现中间态缺规则.  
  目标: `app/src/Firebase/keepRules/firebase-crashlytics.keep` 就位, 删 `app/proguard-firebase-rules.pro`  
  与 `app/proguard-rules.pro`, Firebase 约定插件不再追加规则文件; Foss 变体不含 Crashlytics 规则.  
  动哪些文件: 两个 `.pro` 文件, `app/src/Firebase/keepRules/firebase-crashlytics.keep`,  
  `build-logic/.../app/AndroidApplicationFirebaseConventionPlugin.kt` (+9/-34).  
  怎么验证 (当时): 提交信息没附验证声明; 追补验收按 2026-11-28 的现状核对 (见验收节).  
  对应 design: 实施点 3; 落实 R-3.

- [x] **task 5 flavor 源集目录大小写** (2026-11-26 22:06:08 +0800, 提交 `4041ab5a`).  
  凭什么是独立 task: 纯目录大小写改名, 内容零变化, 独立可验.  
  目标: `app/src/foss/res/values/strings.xml` 所在目录改成 `app/src/Foss/`, 与 flavor 名逐字一致.  
  动哪些文件: `app/src/foss/` 目录改名 (0 行变化).  
  怎么验证 (当时): 提交信息说明小写目录在 Linux CI 被静默跳过, strings.xml 输给主源集,  
  这是对实际缺陷的修正.  
  对应 design: 实施点 4; 落实 R-4.

- [x] **task 6 归档调查笔记** (2026-11-26 22:10:02 +0800, 提交 `8e2f3054`).  
  凭什么是独立 task: 纯挪动加指针修正, 与实施无关, 独立可验.  
  目标: 整目录挪进 `docs/dev/archived/` 按位置成 record; 相对链接补一层;  
  [01-聊天与文档内容规则](../../../agents-cn/01-聊天与文档内容规则.md) 的结构参考指向归档副本,  
  追加做法补归档例外; junit4 spec 的 dated 报告指针跟着改.  
  动哪些文件: 整目录, `docs/agents-cn/01-聊天与文档内容规则.md`,  
  `docs/spec/archived/2026-11-18-10-42-22-+0800-junit4-test-stack-cn/1-requirements.md`.  
  怎么验证 (当时): 挪动后链接逐条可解析 (`8e2f3054` 提交信息).  
  对应 design: 设计点 3; 落实 补-1.

- [x] **task 7 失效留痕** (2026-11-26 22:10:15 +0800, 提交 `b4a011ca`).  
  凭什么是独立 task: 纯新增一个 issue 条目加索引行, 不改任何笔记, 独立可验.  
  目标: 新建 issue #71 列全 18 处失效点 (文件, 位置, 原文说的, 现状), 按已了结归档;  
  issues-cn 的 README 索引加行.  
  动哪些文件: `docs/dev/issues-cn/archived/09-构建与CI/71-调查笔记引用失效.md`,  
  `docs/dev/issues-cn/README.md`.  
  怎么验证 (当时): 18 处失效点按工作树逐处核过原文 (issue #71 证据节的落款).  
  对应 design: 实施点 5; 落实 R-5.

## 验收 (追补核于 2026-11-28)

对着历史提交与现存文件逐条核 R-:

- **R-1 buildConfigField 迁 variant API 加开关删除: 满足.** `gradle.properties` 里 grep 该开关为零;  
  约定插件的 `onVariants` 块里 `variant.buildConfigFields.put("GIT_BRANCH", ...)` 在  
  (`AndroidApplicationConventionPlugin.kt:54-56`), 急切 `buildConfigField(` 调用为零, 只剩第 52 行  
  说明迁移原因的注释; 源码 BuildConfig 字段集合不变的依据是开关默认 false (`e584ae28` 提交信息).  
- **R-2 release 收缩走 optimization 块: 满足.** `app/build.gradle.kts:89` 有 `optimization` 块;  
  `isMinifyEnabled`, `isShrinkResources`, `proguardFiles` grep 为零 (2026-11-28 核).  
- **R-3 Crashlytics 规则进 keepRules 源集: 满足.** `app/src/Firebase/keepRules/firebase-crashlytics.keep`  
  存在; `app/proguard-firebase-rules.pro` 与 `app/proguard-rules.pro` 都不存在;  
  Firebase 约定插件里 grep `proguard` 为零.  
- **R-4 flavor 源集目录大小写: 满足.** `app/src/` 下是 `Foss` 与 `Firebase`, 无小写目录.  
- **R-5 失效留痕不回填: 满足.** issue #71 列 18 处失效点并按已了结归档, 索引行在  
  (`docs/dev/issues-cn/README.md:92`); 调查笔记的结论与行号指针原文未动 (2026-11-28 追补只补过链接层级,  
  见补-1).  
- **补-1 调查目录归档保存: 满足.** 2026-11-26 挪进 `docs/dev/archived/` (`8e2f3054`); 2026-11-28 随追补  
  挪入本 spec 目录, 内容不改, 只有 24 处相对链接各补一层 (死链就地修, 01 状态模型允许).

预研段的核对记在 [2-design.md](2-design.md) 的 预研设计 一节: 四个子目录零交叉引用, 每轮含上游线与仓库  
核对分篇; issuetracker `560282299` 有记 (`01-agp-9-1-to-9-6.md:84`), `572377862` 四轮都缺;  
缺口按负责人 2026-11-28 的决定接受, 记录在案.  
追补核对结论: 五条 R 与补-1 全部满足. 任务已随归档结束, 本次追补只补记录.
