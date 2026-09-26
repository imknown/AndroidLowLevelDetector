<a id="AR-07"></a>

# AR-07 包结构误导: base 一词三义 + 包级循环依赖

> 返回 [README 索引](../README.md) · [组2 · 主线重构 — 架构优先](../README.md#组2--主线重构--架构优先).


**严重程度: P1 | 修复难度: 中 (纯机械搬移)**
**影响文件: `MyApplication.kt`, `ShellLibSu.kt`, `LldManager.kt`, `JsonExt.kt`, `AndroidVersionExt.kt`, `PropertyExt.kt`, `ShellExt.kt` 及 `app/build.gradle.kts`**

## 问题核心代码

"base" 在项目里有三个互不相干的含义:

1. **Gradle 模块** `base/` (`net.imknown.android.forefrontinfo.base.property` / `base.shell`);
2. **app 模块里的同名包** `app/src/main/java/.../base/MyApplication.kt` — Application 类住在 app 模块却顶着 base 的包名;
3. **app 模块里的资源目录** `app/src/main/java/.../base/res/` (`build.gradle.kts` 的 sourceSets).

更糟的是依赖方向成环 (`MyApplication 对 base.shell.ShellManager 的 import`):

```kotlin
// app 模块的 base 包 (最底层的东西) 反向 import UI 层:
import net.imknown.android.forefrontinfo.ui.common.ShellLibSu   // Shell 实现居然住在 ui 包
import net.imknown.android.forefrontinfo.ui.common.initMyAndroid
```

而 `ui.common` 又依赖 base 模块 (`PropertyExt 对 base.property.PropertyManager 的 import` 的 `base.property.PropertyManager`) — 包级循环: `base ↔ ui.common`.

同时 `ui/common/` 成了大杂烩: `LldManager` (文件 IO), `ShellLibSu` (shell 实现), `JsonExt` (序列化), `AndroidVersionExt` (全局可变状态) 这些与 UI 毫无关系的底层设施都住在叫 `common` 的 UI 包里.(当年这里还举 `ViewExt.setScrollBarMode` 作 "真·UI 扩展挤在同一包" 的证据; 那个文件已随 View 层删除, 于是这个包如今 6 个文件全是非 UI 代码 — 名字比当年更误导.)

## 直接原因

"common / base = 什么都往里放" 的惯性: 新文件找不到明确归属时进 common; 底层实现图方便放进 common; common 再被最底层反向引用, 环就闭上了.

## 根本原因

包结构没有表达层次规则. 包名是开发者 (和 IDE 检查) 判断 "谁能依赖谁" 的第一线索; 当 base/ui/common 的实际依赖与名字承诺相反时, 每次新文件选址都在掷骰子, 圈只会越画越大.

## 修复方案

只搬包不改逻辑. 目标结构 (配合 [AR-03](04-AR-03-myAndroid可变单例.md),[AR-04](06-AR-04-服务定位器上帝对象.md) 一并落位):

```
app/src/main/java/net/imknown/android/forefrontinfo/
├── MyApplication.kt            # 移出 base 包; app 模块根包是它的正确位置
├── core/                       # 不依赖任何 UI 的共享能力
│   ├── androidinfo/            # AndroidVersionExt(AR-03 改造后: 只读 AndroidInfo)
│   ├── json/                   # JsonExt
│   ├── lld/                    # LldFileStore(原 LldManager)
│   ├── property/               # PropertyExt → IProperty 的扩展 (AR-04 改造后)
│   └── shell/                  # ShellLibSu(实现), ShellExt → IShell 扩展
├── data/...                      # 各功能的 repository / datasource(原样保留按功能分包)
└── ui/
    ├── common/                 # 只留真 UI 工具: setScrollBarMode, ToastExt, ViewBindingExt
    └── ...(各功能包不变)
```

搬移后 `MyApplication` 只 import `core.*`, 环消除; `ui.common` 缩小为纯 UI 工具. `app/build.gradle.kts` sourceSets 里那组 "资源跟包走" 的路径需同步加 `core` (若 core 下有 res 的话目前没有, 可不加).

迁移清单 (一次提交): 移动 7 个文件 + 全局改 import.(原先顺手列的 "删除 `ShellDefault`" 已按 [AR-13.6](../组1-独立小修/09-AR-13-零散小问题.md) 的裁定取消 — 它作为原生备选实现保留.) IDE 的 Refactor → Move 可全自动完成.


