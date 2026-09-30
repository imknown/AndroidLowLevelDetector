<a id="i04"></a>

# 04 包结构误导: base 一词三义 + 包级循环依赖

> 返回 [README 索引](../README.md) · [1 · 架构与分层](../README.md#1--架构与分层).

**严重程度: P1 | 修复难度: 中 (纯机械搬移)**
**影响文件: `MyApplication.kt`, `ShellLibSu.kt`, `LldManager.kt`, `JsonExt.kt`, `AndroidVersionExt.kt`, `PropertyExt.kt`, `ShellExt.kt` 及 `app/build.gradle.kts`**

## 问题核心代码

"base" 在项目里有三个互不相干的含义:

1. **Gradle 模块** `base/` (`net.imknown.android.forefrontinfo.base.property` / `base.shell`);
2. **app 模块里的同名包** `app/src/main/java/.../base/MyApplication.kt` — Application 类住在 app 模块却顶着 base 的包名;
3. **app 模块里的资源目录** `app/src/main/java/.../base/res`, `resLauncher`, `resBackup`, `resTheme` (`app/build.gradle.kts` 的 `sourceSets` 注册).

更糟的是依赖方向成环 (`MyApplication 对 base.shell.ShellManager 的 import`):

```kotlin
// app 模块的 base 包 (最底层的东西) 反向 import UI 层:
import net.imknown.android.forefrontinfo.ui.common.ShellLibSu   // Shell 实现居然住在 ui 包
import net.imknown.android.forefrontinfo.ui.common.initMyAndroid
```

而 `ui.common` 又依赖 base 模块 (`PropertyExt` 与 `ShellExt` 顶上的 `base.property.PropertyManager`, `base.shell.ShellManager` import) — 包级循环: `base ↔ ui.common`.

同时 `ui/common/` 成了大杂烩: 这个包现有 7 个文件, 其中 6 个是与 UI 毫无关系的底层设施 — `LldManager` (文件 IO), `ShellLibSu` (shell 实现), `JsonExt` (序列化), `AndroidVersionExt` (全局可变状态), `PropertyExt` 与 `ShellExt` (藏在顶层函数里的全局抓取). 唯一的例外是 `ScrollBarExt.kt`: 它确实是 Compose 的 `Modifier.nonInteractiveScrollbar()` 扩展, 该住在 UI 层. 原来挤在这个包里的真 UI 扩展 `ViewExt.setScrollBarMode` 是另一个形状的问题 — 那个文件已随 `fcc048d5` 删除, 它的替代品正是如今这个合规的 `ScrollBarExt.kt`. 包名承诺的 "common = UI 通用工具", 对 6 个文件里的内容都不成立.

## 直接原因

"common / base = 什么都往里放" 的惯性: 新文件找不到明确归属时进 common; 底层实现图方便放进 common; common 再被最底层反向引用, 环就闭上了.

## 根本原因

包结构没有表达层次规则. 包名是开发者 (和 IDE 检查) 判断 "谁能依赖谁" 的第一线索; 当 base/ui/common 的实际依赖与名字承诺相反时, 每次新文件选址都在掷骰子, 圈只会越画越大.

## 修复方案

只搬包不改逻辑. 目标结构 (配合 [#09](../2-SSOT-唯一数据来源/09-myAndroid可变单例.md), [#02](02-服务定位器上帝对象.md) 一并落位):

```
app/src/main/java/net/imknown/android/forefrontinfo/
├── MyApplication.kt            # 移出 base 包; app 模块根包是它的正确位置
├── core/                       # 不依赖任何 UI 的共享能力
│   ├── androidinfo/            # AndroidVersionExt(#09 改造后: 只读 AndroidInfo)
│   ├── json/                   # JsonExt
│   ├── lld/                    # LldFileStore(原 LldManager)
│   ├── property/               # PropertyExt → IProperty 的扩展 (#02 改造后)
│   └── shell/                  # ShellLibSu(实现), ShellExt → IShell 扩展
├── data/...                      # 各功能的 repository / datasource(原样保留按功能分包)
└── ui/
    ├── common/                 # 只留真 UI 工具: ScrollBarExt, ToastExt (后者现在 ui/base/ext/)
    └── ...(各功能包不变)
```

搬移后 `MyApplication` 只 import `core.*`, 环消除; `ui.common` 缩小为纯 UI 工具. `app/build.gradle.kts` sourceSets 里那组 "资源跟包走" 的路径需同步加 `core` (若 core 下有 res 的话目前没有, 可不加).

三处搬移不是纯机械的, 得连带处理:

- `LldManager` 进 `core/lld` 之前, 它 import 的 `ui.home.datasource.LldDataSource` (取 `LLD_JSON_NAME`) 和 `ui.home.model.Lld` 得先中立化, 否则只是把环从 `base ↔ ui.common` 换成 `core ↔ ui.home`; 消除这条边的方案归 [#02](02-服务定位器上帝对象.md) (`LldFileStore` 与 `LldDataSource` 合并或注入).
- `ShellLibSu` 的路径被 [AGENTS.md](../../../../AGENTS.md) 写死 (`ui/common/ShellLibSu.kt`), 搬它就得在同一次提交里改 AGENTS.md, 否则记忆里那条路径立刻失效.
- `ToastExt` 不在 `ui/common`, 它在 `ui/base/ext/`; 目标结构里 "只留真 UI 工具" 的那一份清单要按这个实际位置合并, 而不是再开一个包. `ViewBindingExt` 已随 View 层删除, 不再是要搬的东西.

迁移清单 (一次提交): 移动 7 个文件 (`MyApplication.kt` 出 `base` 包 + `AndroidVersionExt.kt`, `JsonExt.kt`, `LldManager.kt`, `PropertyExt.kt`, `ShellExt.kt`, `ShellLibSu.kt` 出 `ui/common`), 全局改 import, 同批改 AGENTS.md. 原先顺手列的 "删除 `ShellDefault`" 不成立 — 它按 [AGENTS.md](../../../../AGENTS.md) 与 [#45 第 6 项](../8-反模式与卫生/45-零散小问题.md) 作为不依赖 libsu 的原生备选实现保留 (零调用是有意状态), 搬移清单里没有它. IDE 的 Refactor → Move 可全自动完成 import 改写.
