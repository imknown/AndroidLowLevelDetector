<a id="i14"></a>

# 14 检测链在 CPU 线程池上跑阻塞调用

> 返回 [README 索引](../../README.md) · [4 · 并发与调度](../../README.md#4--并发与调度).

**严重程度: P1 | 修复难度: 低 | 状态: 已修复 (8 处 Default 全部改 IO, 检测链合并为单个 IO 块)**


**现象**: 三个列表页的检测序列整体包在 `Dispatchers.Default` 里, 而序列里的每一步都是阻塞调用,  
即 shell, `getprop`, `Settings.Secure` 查询. 修复前 `HomeViewModel.collectModels()` 的 23 个 detect 调用  
都在 Default 上, `OthersViewModel.collectModels()` 与 `PropViewModel.collectModels()` 同一写法.  
只有个别点例外: `HomeViewModel` 读 `/proc/mounts` 与 lld.json 落盘时单独套了 `withContext(Dispatchers.IO)`.

**直接原因**: `Dispatchers.Default` 的线程数按 CPU 核数上限分配, 为计算密集工作设计. 阻塞调用 (等 shell 进程, 等 binder,  
等磁盘) 在该池里是**空等而不是工作**, 既占着一个计算线程, 又不会随等待时间扩容.

**根本原因**: 迁移时只解决了 "别在主线程干活", 没有区分**计算**与**阻塞**. AGENTS.md 已把 "阻塞工作 (shell, 系统属性, 文件,  
网络) 走 `Dispatchers.IO`" 写成约定, 检测链是这个约定最大的未跟上的地方, 与 [#16](16-阻塞调用跑在CPU线程池.md)  
是同一件事的两半 (#16 数出的是另外几处漏掉的调用点).

**处置**: 全仓 8 处 `withContext(Dispatchers.Default)` 改为 `withContext(Dispatchers.IO)`  
(HomeViewModel 3 处, OthersViewModel / PropViewModel 各 1 处, SettingsRepository 3 处), 一并关闭本条与 #16;  
`HomeViewModel.detect()` 里原本相邻的两个 IO 块合并成一个. 纯计算的部分 (字符串比较, 解析, 排序) 未单独切回 Default,  
它跟在 IO 块里没有可测的代价, 真要分家等 [#03](../../01-架构与分层/03-无三态模型与注册表.md) 的注册表引擎按探针特征分派.

- 内层读 `/proc/mounts` 的 `withContext(Dispatchers.IO)` 保留: 外层改为 IO 后它变成冗余,  
  但消除它属于 IO 下沉到 DataSource 的完整修法, 见 [#16](16-阻塞调用跑在CPU线程池.md) 的处置.
- 行为等价: 加载顺序, 结果, 异常路径不变; 本项目各调用点都是单次顺序加载, 排队细节差异无感知.
- 验证: `assembleFossDebug` / `testFossDebugUnitTest` / `lintFossDebug` 全部通过.
