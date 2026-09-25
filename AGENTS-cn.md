# AGENTS.md

给在本仓库工作的编码 agent 的指引. 改动之前先读.

两边默认都不是权威: **代码和文档都可能错**. 代码决定应用*今天*做什么 — 这是可验证的事实; 应用*应该*做什么属于负责人, 而文档里对意图的陈述本身也可能是个错误 (写过头了, 或者凭空编的). 所以在编辑之前先给文档与代码的分歧定性 — 文档过时, 代码 bug, 未实现的需求, 还是文档夸大了需求 — 证据无法定性时, 或者问题在于所述意图是否真的出自负责人, 就问.

工作原则:

- **Memory**: 本文件只是索引, 保持简短. 把持久的发现 (已裁定的决定, 约定, 坑, 文档更正) 记进 `docs/` 下的文档 — 工程约定进 [docs/dev/conventions/](docs/dev/conventions/README-cn.md), 其余进相关文档 — 让下一个 session 站在它们之上, 而不是重新发现一遍. 不确定该记什么或该记在哪, 和用户讨论.
- **Never guess**: 先查代码库和文档; 每次改动都要建立在你能指出来的证据上 (文件, 行号, 文档小节). 无法确定, 或存在多种合理做法时, 摆出发现并问用户; 写 `unknown`, 不要编造取值. 冲突是停止信号, 不是择一的许可: 证据不能判定哪边错 (文档 vs 文档, 文档 vs 代码) 时, 把两边摆出来问. 指令的范围只到被点名的对象 — 第二句看起来像同一个问题的, 进入待问的清单, 不并进同一次修改.
- **English by default**: 你生成的一切 — 文档, 注释, 提交信息 — 除非用户另作说明, 一律用英语. 不管文档用什么语言写, 标点都是 ASCII, 间距按英语来 (不用 `，。、：（）「」`): 见 [docs/README-cn.md](docs/README-cn.md#约定).
- **No sensitive information**: 任何文档, memory 文档在内, 都不得含敏感信息: 隐私数据, 密码, 密钥, 证书或签名材料.

## 项目概览

Android 应用, 展示底层系统特征: Treble 与 GSI 兼容性, Mainline/APEX 模块, system-as-root, A/B 分区, Binder 位数, 安全补丁级别. 技术栈: Kotlin, Jetpack Compose (单 Activity, Navigation 3), MVVM + StateFlow 单向数据流, kotlinx.serialization, Ktor, libsu, JNI/NDK. 没有 DI 框架.

- 应用 id `net.imknown.android.forefrontinfo`; 版本信息在 `gradle/toml/build.toml`.
- 模块: `:app` (Compose UI, 功能在 `ui` 下) · `:base` (`IProperty`/`IShell` 抽象) · `:binderDetector` (经 JNI 的 C++) · `build-logic` (约定插件).

## 构建与验证

```bash
./gradlew assembleFossDebug      # CI 构建的目标 (GitHub Actions)
./gradlew testFossDebugUnitTest  # 单元测试 (目前只有模板)
./gradlew lintFossDebug          # Android lint
```

Flavor, 签名, 工具链, 版本目录以及所有构建约定: 见 [docs/dev/conventions/](docs/dev/conventions/README-cn.md).

## 该看哪里

[docs/README-cn.md](docs/README-cn.md) 是索引: 每份文档是什么, 阅读顺序, 以及文档写作规则 (错的陈述就地修正; 像迁移计划这样的 frozen 记录保持写作时的原样). [docs/dev/README-cn.md](docs/dev/README-cn.md) 对开发者文档做同样的事, 并标明哪些还在持续维护.

刻意偏离主流模板的约定在约定文档里就地标出 — 别顺手把它们 "规范化" 掉.

## Git 与 CI

- PR 目标是 `develop` (默认分支). 提交信息遵循 Conventional Commits, **type + scope 用小写**: `fix(home): ...`.
- 每条提交信息末尾加一个 trailer, 写明产生它的 agent, 模型, 以及推理 effort 等级 (`off` / `low` / `medium` / `high` / `xhigh` / `max`, ...), 例如 `Generated with ZCode (GLM-5.3, effort: xhigh)`. 等级写作 `effort:` — 它是那个推理 effort 旋钮本身, 不是对推理的评价; 2026-09-22 及更早的提交写的是 `reasoning:`, 保持原样. 绝不猜测取值; 当 agent, 模型或 effort 无法确定时, 问用户要记什么, 不要默默写 `unknown`.
- 从证据而不是习惯来命名 agent: 怎么找由你决定, 但要说明依据是什么, 并在写下来之前取得用户的同意. 版本级的名称是有区别的 (`Qoder CN` 与 `Qoder`, `Trae CN` 与 `Trae` 是不同的 ADE); 不要自造宿主形态的后缀, 例如 `IDE` / `CLI`, 除非用户要求.
- CI 只构建 `assembleFossDebug`. `master` 承载 `lld.json` 数据更新 — 不要向它开 PR.

## 绝不提交

- 绝不提交或强制加入 (force-add) 凭据, 签名材料或本地配置 (`local.properties`, `keys/release.jks`, `google-services.json`) — 根 `.gitignore` 已经排除了它们.
- 如果某个改动看起来需要提交这类文件, 先停下来问.
