# 检测条目细则

> **状态**: **living**, 是 [AGENTS-cn.md](../../AGENTS-cn.md) 的新增一个检测条目一节的一部分.  
> 在向任何功能添加检测条目之前先阅读本文.  
> 始终保留在 AGENTS-cn.md 里的规定就是这项义务本身: 新增检测条目时遵循此流程.

流程 (列表顺序 = 调用顺序):

1. 向该功能的 `DataSource` 添加属性键 / shell 命令.
2. 向该功能的 `Repository` 添加一个返回 `MyModel` 的 `detect...()` 方法  
   (新的 Repository / DataSource 类通过 `@Inject` 构造器注入加入依赖图;  
   缺少该注解会让构建以 `[Metro/MissingBinding]` 失败).
3. 在功能 ViewModel 构建列表的位置调用它: Others 与 Prop 是 `collectModels()`, Home 是它调用的 `detect()`.  
   调用顺序决定列表顺序.
4. 把字符串添加到该功能包的 `strings.xml` (默认英文) 以及三个翻译文件.
5. 把这个条目加进[检测条目目录](../requirements-cn/04-detection-item-catalog.md)和它的英文对照:  
   在该屏的表里加一行, Home 条目还要加定义块, 写 `question`, `techniques` 和 `tri-state semantics`.  
   目录里的顺序就是第 3 步的调用顺序.
6. 如果该条目需要带资源的新包, 在  
   `app/build.gradle.kts` sourceSets 中注册它的 res 目录.
7. 用 `./gradlew assembleFossDebug` 验证; 这一步覆盖变更本身, 而约束每个会话的验证流程  
   (见 [AGENTS-cn.md](../../AGENTS-cn.md) 的构建与验证一节) 在其之上依然适用.
