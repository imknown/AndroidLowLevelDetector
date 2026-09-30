<a id="i17"></a>

# 17 myAndroid 跨线程无同步读写

> 返回 [README 索引](../README.md) · [4 · 并发与调度](../README.md#4--并发与调度).

**严重程度: P2 | 修复难度: 低~中**

**结论**: `ui/common/AndroidVersionExt.kt` 的全局 `myAndroid` 是顶层 `val` 指向的 `MyAndroid` 实例, 四个字段 (`api`, `apiFull`, `version`, `dessert`) 全是 `var`. 写发生在 `HomeRepository.detectAndroid()` (LLD 已知行命中时逐字段赋值), 它跑在 `HomeViewModel.detect()` 的 `withContext(Dispatchers.Default)` 块里; 读发生在其它协程 — `OthersViewModel.collectModels()` 在自己的 `withContext(Dispatchers.Default)` 块里调 `isAtLeastAndroid12()` / `isAtLeastAndroid10()` (`OthersRepository.getCodename()` 里还有 `isAtLeastAndroid13()`), 设置页的 `SettingsRepository.getBuiltInDataVersion()` 三处 `withContext(Dispatchers.Default)` 块经 `AppInfoDataSource` / `FingerprintDataSource` 调 `isAtLeastAndroid9/11/13()`. 这些 helper 都经 `sdkInt get() = myAndroid.api` 读同一个对象 — `isAtLeast...()` 是 `Build.VERSION.SDK_INT >= ... || sdkInt >= ...` 两支, 前一支没给出答案时才会读 `myAndroid`, 而这类调用是常态 (Android 16 以下的设备上 `isAtLeastAndroid16()` 必读它); `isLatestPreviewAndroid()` 则无条件读 `myAndroid.apiFull`. 每个页面各有自己的 `viewModelScope`, 谁也不等谁.

**这条链上没有任何同步**: `app`, `base`, `binderDetector` 的源码里 `volatile`, `synchronized`, `AtomicReference`, `Mutex` 全部为零, `myAndroid` 的读写两侧也没有别的保护.

**危害在组合, 不在撕裂**: 单次 int 写入本身是原子的, `api` 不会被读成 "半个值"; 危险的是 `detectAndroid()` 是**四次独立赋值** (`api` → `apiFull` → `version` → `dessert`), 这四步之间落进来的任何一次读, 拿到的都是跨两代的搭配 — 新 `api` 配旧 `version`, 或新 `apiFull` 配旧 `dessert`. 只读 `api` 的 `isAtLeastAndroidX()` 与读 `apiFull` 的 `isLatestPreviewAndroid()`, 在同一时刻给出的判断可以互相矛盾. `detectAndroid()` 自己也是这种读法: 它先用 `myAndroid.apiFull` 去 `known` 列表里找行, 找到后再把四个字段改掉.

**修复方向**: 与 [#09](../2-SSOT-唯一数据来源/09-myAndroid可变单例.md) 的冻结方案合流实施即同时解决 — 那是同一个根对象, 本条不单独开工. 全 `val` + `copy()` 返回新对象之后, 不存在 "改到一半" 的状态, 同步问题随之消失; 在那之前, 短期做法是在加载开始时把四个字段快照进局部变量, 整页只用这份快照.
