# 模块化结构改造: 讨论纪要与建议 (未决)

> 状态: living (活文档, 随进展更新). 2026-10-02 会话讨论的存档: 会上负责人提出,  
> 将来要采用 KMP 式模块化 (KMP 即 Kotlin Multiplatform, 一套 Kotlin 代码共享给多个平台). 下面"目标结构建议"一节是 AI 的分析, **尚未经负责人逐项裁定**;  
> 裁定后会单开 spec 任务 (带自己的 plan / 逐模块报告 / 评审) 再动工. 触发背景:  
> Metro DI 接入完成后的方向之一  
> (Metro 是本项目的编译期依赖注入框架, DI 即依赖注入: 由框架统一给各部件接线, 不再各自手工创建;  
> 详见 docs/spec/2026-10-01-18-37-36-+0800-metro-di-adoption-cn/). 新会话从本文件续接, 不依赖原会话上下文.

## 参考材料 (负责人给定)

- JetBrains: A New Default Project Structure for Kotlin Multiplatform (2026-05):  
  https://blog.jetbrains.com/kotlin/2026/05/new-kmp-default-structure/
- Kotlin Toolchain: Tutorial, Step 6 Modularize:  
  https://kotlin-toolchain.org/latest/getting-started/tutorial/#step-6-modularize

## 两则材料的共同内核

两份材料说的是同一件事: 每个模块只干一类事 (单一职责), 模块之间边界清晰; 应用入口 (最终打包成 App 的那个模块) 独立于共享代码; 各处重复的构建配置, 收拢到模板/约定里统一管理.  
KMP 文章的动因之一 (从 AGP 9 起, 应用入口必须独立于 KMP 共享模块, AGP 即 Android Gradle Plugin, Android 官方的构建插件) 本项目已天然满足  
(:app 与 :base 本来就是分开的). 现状的真实问题: :base 名不副实 (issues-cn #04: 一词三义, 一个名字同时表达了三种含义; 还有包级循环依赖, 即包之间互相引用绕成圈),  
:app 巨石化 (什么都往里堆, 越长越大).

## 现实检验 (AI 判断, 待负责人确认)

检测功能深深绑在 Android 上  
(靠 shell 命令 / getprop 读系统属性 / /proc/mounts 读内核的挂载记录 / JNI 调原生代码查 Binder; JNI 即从 Kotlin 调用 C/C++ 的机制,  
Binder 是 Android 的进程间通信机制), 真正能跨平台共享的只有纯 Kotlin 部分  
(数据模型, 时间与版本比较工具, 纯逻辑). 建议: 先采纳"形" (把模块边界切出来), 暂缓采纳"实"  
(暂不真的添加 iosMain/desktopMain 这类平台专属代码目录), 因为只对齐形状是零成本的; 过早上 KMP 目标, 就得为每个平台重写一遍检测后端, 收益不存在, 成本巨大. 若将来真要 KMP,  
:core/model 与 :core/common 就是现成的 commonMain (KMP 里放跨平台共享代码的目录).

## 目标结构建议 (草案)

```
:app 组合根 + 入口: MainActivity / MyApplication / di / navigation / 主题
:core/model Lld / MyModel 纯 Kotlin 模型 (现散在各 feature 包)
:core/common DateTimeExt 等纯 Kotlin 工具 (现 :base 的一部分)
:core/detect-api IShell / IProperty 接口 (纯 Kotlin)
:core/detect-android ShellLibSu / PropertyDefault 等 Android 实现 (libsu 接线)
:core/data LldFileStore 文件存储 / HttpClient / lld.json 拉取
:feature/home ui.home 纵切 (Screen + ViewModel + Repository + 专属 DataSource)
:feature/others ui.others
:feature/prop ui.prop
:feature/settings ui.settings
:binderDetector JNI (不动)
```

与材料的映射 (对应关系): KMP 文章的 shared (共享代码模块) ≈ :core/*, androidApp (应用入口模块) ≈ :app + features;  
Toolchain 的 module.yaml 单一职责 ≈ 每模块一个 product, templates (模板) ≈ 现有 build-logic convention plugins (约定插件),  
library catalogs (依赖版本目录) ≈ TOML 五目录 (五个 .toml 依赖清单文件),  
exported (对外导出) ≈ api/implementation 收敛 (收紧"谁能看到这个依赖"的边界).  
现有构建投入 (convention plugins / 内容过滤仓库 / 配置缓存 / Firebase flavor 约定) 基本可平移, 因此现在不值得整体换构建系统, 把模块切干净,  
未来无论迁到哪种构建系统都只是机械活.

## 待决问题 (动工前逐项裁定)

1. 真的添加 KMP 编译目标 (像 iosMain/desktopMain 那样的平台代码目录), 还是只取结构 (用纯 Gradle 拆模块, 不碰多平台)?
2. 构建系统: 留在 Gradle (继续用 build-logic + TOML) 还是迁去 Kotlin Toolchain? AI 倾向: 先留, 等模块切干净, 迁移就只是机械活.
3. 跨 feature 共用的类归到哪: AppInfoDataSource (home 和 settings 两个功能共用) 与 LldFileStore (同样共用) 应放进 :core/data;  
   否则 feature 模块之间要互相依赖, "纵切" (一个功能从界面到数据整条线独立成一个模块) 就拆不动.
4. :app 的 .base 包怎么处置: MyApplication 留在入口模块 (组合根, 即统一组装各部件的地方, 就在这里);  
   其 companion (伴生对象) 里的主题/滚动条状态流 (界面可以监听的数据流) 已随 #08 (设置 SSOT, 方案 A; SSOT 即唯一数据源) 迁入 `SettingsStore`,  
   本行不再是待决项; getMyString / getDownloadDir 会在 #01 的 mapper (数据转换层) 迁移中一并消亡.
5. feature 拆分时, package-adjacent res (紧挨 Java 包目录放置的资源文件夹) 跟着各自 feature 走, app/build.gradle.kts 里的 sourceSets 注册要同步更新.

## 顺序结论 (与负责人方向一致)

先收完 Metro DI 的子任务 (ST-07~11, ST 即 subtask), 再动模块化: DI 是搬移的前提, 因为依赖注入的注解跟着类走, 依赖图 (谁依赖谁的接线关系) 自动重连; 没有 DI 时,  
每搬一次类都要手工重接一遍线. tracker #04 既定"放主线最后" (大面积的路径移动会污染每一步 diff, 让改动记录难以阅读). ST-11 closeout (收尾) 重写 AGENTS.md 时,  
把议定的目标结构写进去, 后续搬移向它收敛. Metro 原生支持 Anvil 式聚合 (Anvil 式做法: 各模块自行声明绑定, 最后由根图统一汇总; 根图即最上层的那张总依赖图), 绑定可以散在各模块,  
由根图统一聚合, 所以模块化后 @ContributesBinding 跟着类走即可; 且选型表里 Metro 的 KMP 列与该方向吻合.

## 关联裁定

- ShellDefault (2026-10-02 负责人澄清): 原话 "只是没人调用, 不代表不能修改",  
  AGENTS.md 里 kept-in-reserve (留作备用) 的表述由 ST-11 按此口径改写; 它的改造 (含已知问题: waitFor 之后才读管道会死锁) 留待确实需要时再做.
- Metro 1.5.0 (2026-10-03 核对 1.5.0-SNAPSHOT changelog): 新增 `checkMainMetroHiddenDependencies` 这个 Gradle 检查:  
  当一个模块向 Metro 贡献的绑定被使用方的编译类路径藏住时 (根源是 implementation 与 api 的可见性差异), 构建直接报错. 拆分后贡献方进 `:core/*`, 图留 `:app`,  
  此检查应接进 CI, 每次构建必跑; 拆分前的单模块用不上. 它未修复 ST-08 踩的 "图 factory + companion" 限制 (binding container 方案继续有效),  
  升级 Stable 后再复核.
