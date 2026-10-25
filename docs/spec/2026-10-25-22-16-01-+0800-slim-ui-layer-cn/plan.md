# plan: UI layer 减重

> **状态**: living. 任务级计划; 确认流程见 [13-任务工作流](../../agents-cn/13-任务工作流.md).

## 负责人预写

问题: UI layer 过重, ViewModel 里有业务逻辑. ViewModel 应该只有 UI 的状态和事件,  
业务逻辑应该属于 Repository 或 UseCase.  
做法: 按修复路线排队走完整条主线 (先行批, 1b, 1a, 2a, 2b, 2c), 整条线一个 spec 目录,  
不插队单独搬 Lld 回退链.  
最大风险: 改动量大, review 不过来.

(预写整理自负责人在会话里的原话, 任务结束前不改, 任务结束时拿它对照结果.)

## 任务目标

HomeViewModel 不再持有业务逻辑: 检测判断以领域对象返回 ([#01](../../dev/issues-cn/01-架构与分层/01-无领域模型.md)),  
检测清单成为注册表数据 ([#03](../../dev/issues-cn/01-架构与分层/03-无三态模型与注册表.md)),  
编排从 ViewModel 收回 ([#11](../../dev/issues-cn/03-UDF-单向数据流/11-HomeViewModel手工编排.md)),  
ViewModel 侧只剩状态推导. 防回归的检查先立起来再动核心 ([#50](../../dev/issues-cn/09-构建与CI/50-CI只编译不测试.md), 1b).

对应 [修复路线](../../dev/issues-cn/README.md#修复路线) 的主线: 先行批 → 1b → 1a → 2a → 2b → 2c.  
决定批 (步骤 0) 已全部定完, 结论在那份 README 里; 本计划不重开任何已定项.

## 子任务拆解

### subtask-01 先行批剩余三件 (#11 前半, #44 第 1 处, #63 剩余小件)

- 范围: 三件独立小改, 各自一次提交.
  1. #11 前半: `BaseListViewModel.updateModelDetail()` 的公开参数从下标改为 `MyModelType`,  
     条目定位收进 `modelsStateFlow.update` 这一个原子块;  
     `HomeViewModel.payloadOutdatedTargetSdkVersionApk()` 删掉自己翻状态找下标的代码.  
     不碰 "调用顺序就是列表顺序" 约定.
  2. #44 第 1 处: 引入 `LldSource` 枚举, `HomeRepository.detectMode()` 的颜色判别改用它,  
     `HomeViewModel.detect()` / `tryDetectOnline()` / `tryDetectOffline()` 的 `modeResId` 参数跟着换,  
     资源 ID 只在拼文案时用.
  3. #63 剩余小件: `MountDataSource.getMounts()` 查 `isSuccess`, 失败时仿 `PropRepository.getBuildProp()`  
     的已有做法渲染源级错误行. `getBuildProp` 与 `exitCode` 已完成; `ShellLibSu` 合并 stderr 不在本件,  
     随 2b 的 #03 主体一并定.
- 独立性: 三件都不改函数间的语义约定, 每件 1~3 个文件, 独立编译, 独立提交.
- 风险级别: 低 (机械改写与既有模式复制).
- 生疏领域: 否.

### subtask-02 1b 基线快照与测试栈

- 范围: 引入 JUnit 5 (含 useJUnitPlatform 接线) 与 MockK; 把 Home, Others, Prop 三页每行的  
  (key, detail, color) 现状导成黄金数据, 作为行为等价重构的回归网;  
  Turbine 随首个 Flow 测试进; 规则文件的测试段随本步校准 (0e).
- 独立性: 不动产品代码. 2a 的一次性迁移没有它就只能靠人工核对行为等价, 所以它是 2a 的硬前置.
- 风险级别: 低 (测试基础设施; 黄金数据怎么导出在子任务报告里定).
- 生疏领域: 部分 (测试栈接线对本仓库是新引入).

### subtask-03 1a CI 扩跑

- 范围: `android-ci.yml` 扩跑 `testFossDebug` 与 `lintFossDebug`; 首跑先在本地清红;  
  [09-构建约定](../../agents-cn/09-构建约定.md) 的 "CI 只构建 `assembleFossDebug`" 条款随本步改写 (0a).
- 独立性: 改一行配置加清红, 不依赖其他步. 排在 1b 之后, 让这条检查从第一天起就有黄金数据可守.
- 风险级别: 低 (清红的工作量首跑才知道).
- 生疏领域: 否.

### subtask-04 2a 领域化 (一次性)

- 范围: Home, Others, Prop 三页的全部检测条目方法迁移 (#01, 兼落 #03 的三态类型): 仓库只返回领域对象,  
  翻译成 MyModel 的 mapper 放 UI 包且只放在那里;  
  [#42](../../dev/issues-cn/08-反模式与卫生/42-字符串当哨兵值.md) 的 "拿翻译文案当判断值" 坏模式随新结构消除;  
  逐条配真单测 (`Build.*` 读数按 0f 逐项裁量);  
  `toColoredMyModel` / `toTranslatedDetailMyModel` 里 MyApplication 取不到值时的默认处理移入 mapper;  
  [06-检测条目](../../agents-cn/06-检测条目.md) 与 0d 条款随本步改写.  
  不走渐进, 全部迁完, 不留过渡形态.
- 独立性: 依赖 subtask-02 的黄金数据. 验收标准: 三页每行 (key, detail, color) 与快照逐行一致.
- 风险级别: 高 (核心逻辑; `getMyString` 全项目 99 处调用点, 其中 69 处在 HomeRepository).  
  Settings 页的 Version 行不在本步, 它是 #44 第 2 处, 在独立小件清单里.
- 生疏领域: 部分 (测试手段按 0f 可能新引入 Robolectric 或仪器测试).

### subtask-05 2b 注册表引擎

- 范围: 每个检测项变成清单里的一条数据 (含执行函数), 引擎统一执行 (#03);  
  并发有上限 ([#15](../../dev/issues-cn/04-并发与调度/15-检测串行不感知取消.md)), 可取消, 展示顺序固定;  
  Others / Prop 仿 Home 一并入表 (0g);  
  [#43](../../dev/issues-cn/08-反模式与卫生/43-比对语义逐行adhoc.md) 的比对语义随条目入表并用特征测试锁定,  
  限已定语义 (#67 与 #66 的 binder 行不锁); `ShellLibSu` 合并 stderr 随本步定.
- 独立性: 依赖 subtask-04 的领域对象. 引擎按 0h 已定的方向造, 以质量与可维护性为准.
- 风险级别: 高 (新抽象加并发).
- 生疏领域: 是 (注册表引擎是本仓库没有过的新设计).

### subtask-06 2c 编排收回

- 范围: #11 后半加它的迁移清单: 23 个仓库方法的逐行手抄随目录变成数据清单一并消失,  
  ViewModel 侧改为 combine(settings, 检测源, refresh) 纯推导;  
  lld 联网到离线的回退链 (`tryDetectOnline` / `tryDetectOffline` / `fetchOfflineLldOrNull` /  
  `getAssetLldOrNull`) 下沉, 取数与回退进仓库, 或返回结果类型由 mapper 呈现 (UI 不 try 约定);  
  补偿路径的固定重读离线文件与磁盘写一并收进; 不建 `collectHomeModels()` 过渡形态.  
  编排与 "用例" 逻辑默认放仓库与注册表引擎, UseCase 层按需增设.  
  根 AGENTS.md 索引与 docs/agents-cn 对应文件随本步改写.
- 独立性: 依赖 subtask-05. 本任务的目标 (UI layer 减重) 在这一步完成.
- 风险级别: 高 (UDF 主路径重排).
- 生疏领域: 否.

## 覆盖对照

修复路线各项到子任务的归属: 先行批剩余 → subtask-01; 1b → subtask-02; 1a → subtask-03;  
2a → subtask-04; 2b → subtask-05; 2c → subtask-06.

不在本任务范围的路线项: 独立小件清单 (#19 字典序, #30, #34, #38, #39, #44 第 2 处,  
#22 ~ #29, #65, #52, #51, #55), 目录核对 (步骤 3), #04 包结构搬移. 它们有自己的排期,  
不因本任务顺带改动.

## 待学习清单

- JUnit 5 平台接线, MockK, Turbine (subtask-02 起).
- `Build.*` 读数的测试手段取舍: 真机 / 模拟器, Robolectric, 探针假值 (subtask-04, 0f).
- 注册表引擎的并发模型: 有界并发与取消传播 (subtask-05).
- mapper 模式: 领域对象到 MyModel 的翻译只放 UI 包 (subtask-04 起).
