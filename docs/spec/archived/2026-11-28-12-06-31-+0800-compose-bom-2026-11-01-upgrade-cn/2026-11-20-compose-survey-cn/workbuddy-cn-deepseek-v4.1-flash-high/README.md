# Compose 2026.11.01 (1.13.0) 对本项目的影响调查

> **状态**: **frozen**. 这份是 2026-11-20 那次调查记下的现状与结论, 后来的更正留在原处.  
> 它不规定代码该怎么做.

- 调查者: WorkBuddy CN
- 模型: DeepSeek-V4.1-Flash
- effort: high
- 调查日期: 2026-11-20

调查范围是 Compose 1.13.0 与 Material 3 1.5.0 这一版: 新特性, 实验性转正, 行为变更, 弃用, 以及它们对这个仓库的影响.  
官方页面这次只能从 `developer.android.com` 的镜像域名和检索摘要读到 (直连被转到登录页), 所以涉及发行说明的结论统一标成  
转述未核, 并写清出处. BOM 到构件版本的映射改从本地 Gradle 缓存里的 pom 读, 那部分是真核过的.

## 分篇索引

| 篇 | 讲什么 |
|---|---|
| [01 版本边界与构建配置](01-版本边界与构建配置.md) | 这条 BOM 里装了什么, 上游对这个版本的要求, 项目要改哪个构建文件 |
| [02 本项目受影响的地方](02-本项目受影响的地方.md) | 逐条列出碰到的 API 与位置, 以及要不要动代码 |
| [03 可以直接采用的改进](03-可以直接采用的改进.md) | 七种走得通的写法, 各自的好处, 代价与失败时怎么办 |
| [04 需要真机验证的行为变更](04-需要真机验证的行为变更.md) | 点击音, 读屏顺序, 文字行高, 滚动条: 怎么验, 不达标怎么办 |
| [05 新特性与本项目的相关性](05-新特性与本项目的相关性.md) | 这一版给了什么, 在这个项目里用不用得上 |
| [06 升级步骤与验收](06-升级步骤与验收.md) | 顺序, 验收的四条硬指标, 怎么分开回退 |

## 核心结论

- 2026.11.01 这条 BOM 把 compose 的 ui, foundation, runtime, animation 一起给到 1.13.0, material3 给到 1.5.0,  
  adaptive 给到 1.3.0.  
  核实程度: 当场核过 (读本地 Gradle 缓存里的 compose-bom-2026.11.01.pom).
- `material3` 1.5.0 自己的 pom 钉的是 compose 1.12.0, 所以 BOM 是唯一能保证这一堆版本一起动的入口.  
  核实程度: 当场核过 (读缓存里的 material3-1.5.0.pom 依赖段).
- 上游要求 compileSdk 37.1, Kotlin 2.2, lifecycle-runtime 2.10.0, minSdk 24; 这个项目在  
  `gradle/toml/build.toml` 与 `gradle/toml/android.toml` 里的取值都够, 构建文件只需要改 `compose-bom` 那一行.  
  核实程度: 上游的四项转述未核 (出处: compose-ui, compose-foundation, compose-runtime 的 1.13.0 发行说明);  
  项目侧取值当场核过.
- 这个项目碰到的 Compose 面积很小: 93 条 import, 集中在 `foundation.layout`, `material3` 的基本组件,  
  `runtime` 的状态与副作用. 文本输入, 复杂布局, 图形, `derivedStateOf`, Material 2, Compose UI 测试, 一处都没有用.  
  所以这一版改动最大的几块 (文本重写, FlexBox 与 Grid, 渐进模糊与多边形, 状态计算) 与它无关.  
  核实程度: 当场核过 (按包统计 import, 再按符号逐个 `grep`).
- 要动代码的只有两处: `ListItem` 的 `headlineContent` 重载在 1.5.0 被弃用 (三处调用),  
  `TopAppBar` 转稳定之后 `ui/AppRoot.kt:142` 的 `@OptIn` 与注释过期.  
  核实程度: 弃用与转正转述未核 (出处: material3 的 `ListItem` 与 `TopAppBar` API 参考页); 三处行号当场核过.
- `ui/common/ScrollBarExt.kt` 等的那份官方实现 (`Modifier.nonInteractiveScrollbar`) 在 1.5.0 到位了,  
  按那个文件自己的注释, 现在可以删掉它, 但要显式传厚度, 颜色, 淡出时长才守得住旧观感.  
  核实程度: 转述未核 (出处: `nonInteractiveScrollbar` 的 API 参考页, 标 Added in 1.5.0, 无实验性注解);  
  文件里的注释与参数值当场核过.
- 用户唯一能听出来的变化是点击音: 1.13 起 `Modifier.clickable` 自动播系统点击音, 项目里四处会响.  
  核实程度: 转述未核 (出处: 2026 年 11 月发布博客的行为变更一节); 四处的行号当场核过.
- 测试这一块现在还用不上: `ComposeUiTest` 转稳定是给写 Compose UI 测试的人用的, 这个仓库的测试是  
  JUnit5 加 Robolectric 的普通单元测试, 没有 `ui-test` 依赖.  
  核实程度: 当场核过 (读 `app/build.gradle.kts` 的依赖段与 `gradle/toml/android.toml` 的测试构件).
