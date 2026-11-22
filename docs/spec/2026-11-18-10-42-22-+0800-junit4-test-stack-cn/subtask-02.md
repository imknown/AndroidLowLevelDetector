# subtask-02 文档与事实一致

> **状态**: living. 事实核对时间 2026-11-18, 代码在 `17b450b9`.  
> 任务级计划见 [plan.md](plan.md); 流程见 [13-工作流](../../agents-cn/13-工作流.md).

## 目标

把 living 文档里断言测试栈现状, 测试计数, 以及把已就位依赖写成缺口的句子, 改成改造后的  
事实. 历史决定的记载与 dated 调查报告保持原样.

## concept primer

- 仓库有两类文档: living 的现状与规范 (发现了错就就地改), 与 record 性质的历史材料  
  (分析报告, spec 记录, 归档目录, 不动). 判断依据看 `07-文档规则` 与文件位置.
- 这次要改的句子分三类: ① 说本机单测用 Jupiter 或平台 launcher 的现状句; ② 测试计数  
  还停在 21 个类 82 个方法; ③ 把已经在仓库里的 Robolectric 说成缺口的句子.
- 不该改的是记 "1b / 0e 那次怎么定的" 的决策史, 以及带日期的一次性调查记录.
- 改造后本地单测与仪器测试都是 JUnit 4, Robolectric 走原生 runner, 测试类 25 个,  
  方法 111 个 (app 24 类 107 个, base 1 类 4 个).

## 现状与证据

| 文件 | 位置 | 现在的句子 |
|---|---|---|
| `docs/agents-cn/05-项目架构.md` | 64-73 | "测试栈分两套. 本机单测跑 JUnit Jupiter ... 单测 classpath 上故意不放 JUnit 4 ... 写了 JUnit 4 风格的测试也只能编译, 不会执行" |
| 同上 | 75-82 | "它经 Jupiter 平台的 `RobolectricExtension` 桥接加载 ... 三个测试 JVM 开关" |
| `docs/dev/issues-cn/01-架构与分层/06-技术栈缺口.md` | 9-14 | "已就位 ... JUnit Jupiter 与 JUnit Platform launcher (本机单测)" |
| 同上 | 19 | "缺 ... Espresso, Robolectric, Compose UI 测试" |
| 同上 | 20-23 | "给 `test` bundle 挂 Jupiter 与 platform launcher (`useJUnitPlatform()` 也在那里)" |
| 同上 | 22-23 | "本机单测的 classpath 上故意没有 JUnit 4, 仪器测试那一路的 JUnit 4 由 androidx.test 传递带入" |
| 同上 | 27 | "合计 21 个测试类, 82 个测试方法" |
| 同上 | 41 | "JUnit Jupiter 与 MockK 已随 1b 落地" |
| 同上 | 42 | "Espresso / Robolectric / Compose UI 测试随迁移完成后的测试立项补齐" |
| `docs/dev/issues-cn/08-反模式与卫生/43-比对语义逐行adhoc.md` | 54 | "合计 21 个测试类, 82 个方法" |
| 同上 | 55 | "有 16 个 `Detect*Test` 直接测 `HomeRepository` 的检测方法" |
| 同上 | 64 | "该设施已随 1b 就位, `test` bundle 现在是 JUnit Jupiter 与 platform launcher" |
| `docs/dev/issues-cn/README.md` | 276 | "真实测试已有 21 个类, 82 个方法" |
| `docs/dev/issues-cn/01-架构与分层/01-无领域模型.md` | 80 | "现有 21 个测试类, 82 个测试方法" |
| `docs/dev/issues-cn/05-稳定性与错误处理/19-版本合并残留弱点.md` | 24-25 | "仓里现有 21 个测试类, 82 个方法" |

## 计划改动

1. `docs/agents-cn/05-项目架构.md`
   - 64-73 行改成单一 JUnit 4 方言: 本地单测与仪器测试同一套断言. 65-67 是一个整句  
     (括号挂在句中), 整体改写, 不要按"删两句"去拆; 保留 MockK 的用法边界.
   - 75-82 行改成 Robolectric 原生 runner (`@RunWith(RobolectricTestRunner::class)`),  
     删桥接说明与 launcher interceptor 开关那句, 保留 `isIncludeAndroidResources` 与  
     `--add-opens` / `--enable-native-access` 的说明; 77-78 行的 "接线有四样" 改  
     "三样", "加三个测试 JVM 开关" 改 "两个" (这个 "两个" 以 subtask-01 补完后的实际  
     条数为准, 若补进来的 `--add-opens` 不止一条, 写成实际条数).
   - 改写 68-69 行时注意适用范围: 仪器侧那份 JUnit 4 仍由 `androidx.test.ext:junit` 与  
     `androidx.test:runner` 传递带入; 本机侧那份改由版本目录给.
   - 81-82 行的 "`RobolectricSpikeTest` 是桥接的常驻冒烟, 升级 Robolectric, 平台或 AGP  
     之后先看它" 里 "桥接" 与 "平台" 两处随整段改写.
2. `06-技术栈缺口.md`
   - 14 行: `JUnit Jupiter 与 JUnit Platform launcher` 改成 `JUnit 4` (本地单测).
   - 20-23 行: `test` bundle 的说明改成只挂 `junit`, 删 `useJUnitPlatform()` 那句;  
     22-23 行的 "本机单测的 classpath 上故意没有 JUnit 4" 改成 "本机单测的 JUnit 4 来自  
     版本目录", 仪器侧那句 "由 androidx.test 传递带入" 保持原意.
   - 27 行: 计数改成 25 个测试类, 111 个方法.
   - 19 行: 从 "缺" 清单里删掉 Robolectric.
   - 41 行: 改成 "JUnit 4 与 MockK 已就位" (计划规定的写法; 1b 落地的是 Jupiter 与  
     MockK, JUnit 4 是本 spec 落的, 不能写成随 1b 落地), 与 14 行一致.
   - 42 行: 从 "待补齐" 里删掉 Robolectric.
3. `43-比对语义逐行adhoc.md`
   - 54 行: 计数改成 25 类 111 个方法.
   - 55 行: 按 "直接测 `HomeRepository` 的检测方法" 的口径实数为 16  
     (另 3 个只测 match 函数), 与原文一致, 保持不动.
   - 64 行: 改成 `test` bundle 现在是 `junit`, Robolectric 走原生 runner.
4. 计数三处: `README.md:268`, `01-无领域模型.md:80`, `19-版本合并残留弱点.md:24-25`  
   统一改成 25 类 111 个方法 (写明 app 24 类 107 个, base 1 类 4 个).
5. 核对 subtask-01 改过的 `gradle/toml/android.toml` 注释, 确保与文档口径一致.

## 有意不改

- `docs/dev/issues-cn/README.md:330` 与 `:353`: 记 0e 与 1b 那次决定的路线表行与步骤说明.
- `docs/dev/issues-cn/archived/09-构建与CI/50-CI只编译不测试.md:32`: 记 1b 那次改动的历史叙述.
- `docs/dev/2026-10-16-project-analysis-cn/` 与 `docs/dev/2026-10-30-agp-survey-cn/`:  
  dated 分析报告, 按 `07` 保持原状.
- `docs/spec/2026-10-25-...-slim-ui-layer-cn/`: 另一个 spec 的正文, 归它自己更新.
- 扫描过但确认不用改: `README.md:179` ("测试基础设施已随 1b 与 #50 补齐", 讲基础设施是否  
  补齐, 与框架更替无关), `24-杂项隐患.md:66` (未来时态的修法描述),  
  `docs/dev/issues-cn/archived/09-构建与CI/50-CI只编译不测试.md:22-28` (测试源集分布表, 逐行核实仍准确),  
  `43-比对语义逐行adhoc.md:66` ("只剩 Turbine 没引", 改造后仍成立).

## 覆盖核对表

| 计划里的范围 | 落点 | 是否覆盖 |
|---|---|---|
| JUnit 描述改 JUnit 4 | 改动 1 与 2 | 是 |
| 测试计数改 25 类 111 方法 | 改动 2, 3, 4 | 是 |
| Robolectric 缺口句 | 改动 2 (19, 42) | 是 |
| 06:41 与 14 一致 | 改动 2 | 是 |
| 43:55 的 Detect*Test 计数 | 改动 3 | 是 |
| 核对 subtask-01 改过的 catalog 注释措辞 | 改动 5 | 是 |
| 历史决定与 dated 报告 | 有意不改 | 是 |

## 验证

- 逐条对照官方文档与改完后的代码现状.
- grep `tech.apter` 与 `org.junit.jupiter` 在 `docs/agents-cn/05-项目架构.md` 与  
  `docs/dev/issues-cn/` 零残留.
- 另按 `Jupiter`, `useJUnitPlatform`, `platform launcher` 三个说法在被改的六份文档里逐句核,  
  `README.md:330` 与 `:353` 及 `docs/dev/issues-cn/archived/09-构建与CI/50-CI只编译不测试.md:32` 排除在外.
- 不跑构建.

## 风险与回滚

- 风险低: 只改文字, 不碰代码.
- 回滚: 文档提交单独一笔, 可独立回滚.
