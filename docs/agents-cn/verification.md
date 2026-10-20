# Android Studio 分析细则

> **状态**: **living**, 是 [AGENTS-cn.md](../../AGENTS-cn.md) 的构建与验证一节的一部分.  
> 在你依据 `android studio analyze-file` 的结果做判断之前先读这份文件.  
> 留在 AGENTS-cn.md 里的是这条义务本身: 每一批编辑之后, 为每个改动过的文件都跑一遍它.

`android studio analyze-file` 驱动一个*正在运行*的 Android Studio 实例, 一次只针对一个文件报告它实时的检查结果:  
编译错误, 各个 Severity 级别的 IDE 检查 (Warning / WeakWarning / Info; 这个范围涵盖 JetBrains IntelliJ Language Server  
所报的内容并超出它), Android Lint, 以及已安装的 IDE 插件所做的检查.

## 用法

```bash
android studio analyze-file --project=AndroidLowLevelDetector <path>
```

- 路径相对于当前目录, 也可以是绝对路径; 命令每次运行只接收一个文件.
- 有多个 Android Studio 实例正在运行时, 加上 `--pid=<pid>`.
- 凡是 Android Studio 能分析的东西都在范围内, 包括它的已安装插件检查的内容.

## 前提条件

- 这个命令自身不带任何分析器: 它查询的是一个正在运行的 Android Studio, 所以项目必须先在它里面打开.
- `android studio check` 把项目列为 `READY`, 这是一份结果值得一读的前提.  
  没有任何实例打开了这个项目时, 运行会失败, 所以要请负责人打开 Android Studio 和这个项目.

## 在验证流程中的位置

先为每个改动过的文件跑 analyze-file, 再为每个改动的 `.kt` 文件跑 `node scripts/ils-diagnostics.js <file>`,  
然后跑 AGENTS-cn.md 的构建与验证一节里的 Gradle 档位  
(`assembleFossDebug`, `testFossDebugUnitTest`, `lintFossDebug`): IDE 的单文件结果几秒就回,  
能尽早抓到编译错误和检查发现的问题, ILS 随后给出这些文件的编译类发现的完整清单,  
Gradle 构建再对整批改动跑它自己的检查. 新上下文的 diff review 排在三者之后.

这个顺序不是随便定的. analyze-file 排在最前, 因为它几秒就回, 而且读的是 IDE 的实时状态,  
明显的问题不用等更慢的东西. ILS 排在第二, 因为它把一个文件的每处编译类发现逐处列出,  
analyze-file 的结果做不到这一点; 它自己还要做一次 Gradle 导入, 比 IDE 那次检查贵.  
Gradle 那些档位排在最后, 因为它们的任务就是把编译类发现在整批改动上再讲一遍.

Gradle 档位替代不了 analyze-file, 两者只部分重叠: Gradle 档位检查这一批能否编译, 外加编译器警告和 Android Lint,  
它们有时也会带出自己的语法级警告; analyze-file 补上 IDE 检查范围的其余部分  
(各个 Severity 级别的用法发现, 已安装插件的检查), 而这些是一次成功的 Gradle 构建永远看不到的.  
这就是 analyze-file 这条义务对每个改动过的文件都成立, 资源文件也包括在内的原因:  
跳过它是一个实实在在的覆盖漏洞, 不是多余的第二遍.

Gradle 那些档位答的是与 JetBrains IntelliJ Language Server 同一类发现 (编译类), 但覆盖整批改动,  
每条发现一个 `Problem found:` 块, 每块带一行 `Location:`; 编译类发现的完整清单就来自那里.  
ILS 是它们的单文件通道:  
`node scripts/ils-diagnostics.js <file>` 列出这个文件里每一处编译与语义发现, 而 analyze-file 的结果做不到这一点 (见下面那条截断).

## 读结果

- 退出码不是结论: 一次打印出 `ERROR` 条目的运行仍然退出 0, 而退出 1 只说明调用失败了  
  (没有实例打开了这个项目, 或者文件不存在).
- 阅读打印出的 `ERROR` / `WARNING` / `INFO` 块, 每个块都带有行号和列号.
- 检查级别的发现 (例如 "Use destructuring declaration") 按语法警告对待: 直接修掉, 不写进文档.

## 结果为什么可能不完整

- **一个文件里, 同一条消息文本只出现一次.** 两条发现带着同一条消息文本时, 只有第一条回来,  
  所以 N 个发现不是 N 处位置: 修好被报告的那处会带出下一处.  
  规则落在消息文本上, 不是落在检查项上: 三条 `Property "alpha" / "beta" / "gamma" is never used` 全都回来,  
  因为每条带的名字不同; 四条 `Unnecessary non-null assertion (!!)` 只回来一条, 因为四条文本完全一样.  
  编译错误同样如此: 一个文件里两条 `Unresolved reference 'missingSymbolOne'.` 只回来一条.  
  在全仓实测过: 74 个 Kotlin 文件, 245 条诊断, 没有任何一个文件里出现过两条文本相同的条目.  
  截断是 IDE 做的, 不是 CLI 做的: CLI 只负责打印 IDE 交给它的列表.  
  文件开在编辑器里和关着拿到的结果一样, 所以开在编辑器里不是原因.  
  要在源码里数位置, 不要在一次运行的输出里数.
- **未索引的文件会打印一个假的 `No issues found!`** (仍然退出 0). 本次会话里新建的文件对 Android Studio  
  保持不可见, 直到它把这个文件索引完, 所以在把这份沉默当作通过之前, 要等索引追上之后再重跑一遍检查.  
  `--project` 点名路径之外的文件同样不可见.
- **Android Studio 已经认识的文件一经编辑就会被立刻分析**, 所以那里不需要等待.

## 另一条通道: 逐处给出的编译与语义发现

`android studio analyze-file` 报的是 IDE 检查项, Android Lint 和已安装插件的检查, 上面那条截断对它们都成立.  
JetBrains IntelliJ Language Server (ILS) 是第二条通道: 它报一个文件的编译与语义发现, 每一处都给出, 从不截断.  
两条通道在编译类发现上有重叠, 在检查项上不同: ILS 报的检查项范围更窄  
(`Function name 'ProbeOne' should start with a lowercase letter` 这类它会报, `Unused import directive` 和未使用声明那类不会),  
analyze-file 报的是完整的检查项范围.

仓库里有两个脚本驱动 ILS, 都在 `scripts/` 下:

```bash
node scripts/ils-diagnostics.js <path-to-kotlin-file>   # 一个文件的发现
node scripts/ils-selfcheck.js                            # 分析器是不是真的看得见工程
```

- **工程根目录由脚本自身的位置推出来**, 所以两个脚本都必须留在仓库根目录下一层, 仓库把它们放在 `scripts/` 下.  
  换到那个位置之外跑的那份副本, 分析的是工程模型之外的文件, 回答 `CLEAN (0 items)`.
- **服务端从环境变量里找**: `KOTLIN_LSP_SERVER` (到 `intellij-server` 的完整路径),  
  `KOTLIN_LSP_HOME` (发行版根目录, 用 `<root>/bin/intellij-server`), 或者 `PATH` 上的 `intellij-server`.
- **索引缓存在 `<repo>/.kotlin/lsp-cache/<process.platform>`** (已 gitignore). 缓存按平台分开,  
  因为每个平台跑各自的发行版. 用  
  `<发行版>/bin/warmup.py <repo> <repo>/.kotlin/lsp-cache/<process.platform> --build-tool gradle` 预热;  
  冷索引要几分钟, 可能超过 ready 超时, 热索引在一分钟内就有答案.
- **ILS 的 Gradle 同步跑在 `JAVA_HOME` 指的那个 JDK 上**, 它的版本不能低于工程的 Java 工具链  
  (`gradle/toml/build.toml` 里的 `javaToolchain`). `:build-logic:convention` 这个 included build 是按那个工具链的字节码版本编译的,  
  更老的构建 JVM 装不下这些 convention 插件: 导入会以 `Dependency requires at least JVM runtime version ...` 失败,  
  ILS 退化成不完整的工程模型, 照样回答诊断, 于是一次运行打印出假的 `CLEAN`.  
  预热或诊断之前, 把 `JAVA_HOME` 指到不低于那个版本的 JDK; ILS 发行版自带一个同版本的 JetBrains Runtime (JBR), 在 `<发行版>/jbr` 下.  
  命令行里发起的构建是另一回事: `./gradlew` 的 daemon JVM 取自 `gradle/gradle-daemon-jvm.properties`,  
  那里把它钉在工具链版本上并让 Gradle 自动 provision, 所以那条路上 `JAVA_HOME` 不决定 JVM.
- **自检是分辨"分析器没有工程模型"和"代码没问题"的办法**: 它往一个源码根写一个临时哨兵文件, 里面带着一条已知的警告,  
  要求那条警告回来, 之后把哨兵删掉. 哨兵上拿到 `CLEAN` 说明同步失败了, 不是说代码干净.
- **退出码** 只有在拉取成功且没有 ERROR 条目时才是 0, 运行失败或有 ERROR 条目是 1, 被中断是 130 或 143.  
  退出码不检查同步, 所以要配上自检才能相信一份 `CLEAN`.
- **这个检查只管 `.kt`**: ILS 不为 `.kts` Gradle 脚本建模型, 所以脚本在它上面会失败, 而不是打印一份空的 `CLEAN`.  
  构建脚本用 Gradle 构建本身来验.
- `ILS_READY_TIMEOUT_MS` (默认 1800000) 和 `ILS_CALL_TIMEOUT_MS` (默认 180000) 覆盖两处等待, 给慢的机器用.
