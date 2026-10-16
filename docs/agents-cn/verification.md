# Android Studio 分析细则

> **状态**: **living**, 是 [AGENTS-cn.md](../../AGENTS-cn.md) 的构建与验证一节的一部分.  
> 在你依据 `android studio analyze-file` 的结果做判断之前先读这份文件.  
> 留在 AGENTS-cn.md 里的是这条义务本身: 每一批编辑之后, 为每个改动过的文件都跑一遍它.

`android studio analyze-file` 驱动一个*正在运行*的 Android Studio 实例, 一次只针对一个文件报告它实时的检查结果:  
编译错误, 各个 Severity 级别的 IDE 检查 (Warning / WeakWarning / Info; 这个范围涵盖语言服务器的能力并远超它们),  
Android Lint, 以及已安装的 IDE 插件所做的检查.

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

先为每个改动过的文件跑 analyze-file, 再跑 AGENTS-cn.md 的构建与验证一节里的 Gradle 档位  
(`assembleFossDebug`, `testFossDebugUnitTest`, `lintFossDebug`): 单文件的结果回来得快,  
能尽早抓到编译错误和检查发现的问题,  
Gradle 构建随后再对整批改动跑它自己的检查. 新上下文的 diff review 排在两者之后.

Gradle 档位替代不了 analyze-file, 两者只部分重叠: Gradle 档位检查这一批能否编译, 外加编译器警告和 Android Lint,  
它们有时也会带出自己的语法级警告; analyze-file 补上 IDE 检查范围的其余部分  
(各个 Severity 级别的用法发现, 已安装插件的检查), 而这些是一次成功的 Gradle 构建永远看不到的.  
这就是 analyze-file 这条义务对每个改动过的文件都成立, 资源文件也包括在内的原因:  
跳过它是一个实实在在的覆盖漏洞, 不是多余的第二遍.

## 读结果

- 退出码不是结论: 一次打印出 `ERROR` 条目的运行仍然退出 0, 而退出 1 只说明调用失败了  
  (没有实例打开了这个项目, 或者文件不存在).
- 阅读打印出的 `ERROR` / `WARNING` / `INFO` 块, 每个块都带有行号和列号.
- 检查级别的发现 (例如 "Use destructuring declaration") 按语法警告对待: 直接修掉, 不写进文档.

## 结果为什么可能不完整

- **编辑器会截断列表.** 只在文件没有开在编辑器里时才分析它.  
  Android Studio 会按它的 Severity 和 "Highlighting in editor" 设置过滤检查, 而 analyze-file 继承了这层过滤,  
  所以一个摆在编辑器标签页里的文件拿回来的结果是截断过的 (即使把所有可显示的 Severity 都勾上,  
  Problems 视图也一样不完整). 关闭文件是拿到完整列表的唯一办法.
- **未索引的文件会打印一个假的 `No issues found!`** (仍然退出 0). 本次会话里新建的文件对 Android Studio  
  保持不可见, 直到它把这个文件索引完, 所以在把这份沉默当作通过之前, 要等索引追上之后再重跑一遍检查.  
  `--project` 点名路径之外的文件同样不可见.
- **Android Studio 已经认识的文件一经编辑就会被立刻分析**, 所以那里不需要等待.
- **一项检查在每个文件里只报告一次.** 一次运行在一个给定文件里最多列出某项检查的一个条目,  
  所以 N 个发现不是 N 处位置: 修好被报告的那处会带出下一处. 这是在 assert 重写, 未使用声明那一组,  
  `Missing line break`, 以及法语撇号这几批改动上实测到的, 其中 3 个被报告的条目实为 3 个文件里的 19 处位置.  
  要在源码里数位置, 不要在一次运行的输出里数.
