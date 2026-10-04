<a id="i57"></a>

# 57 foss 纯度靠变体化配置, 没有机器闸门

> 返回 [README 索引](../README.md) · [10 · 权限与合规](../README.md#10--权限与合规).

**严重程度: P1(🟡) | 修复难度: 中 | 处置: 有意接受, 现状即方案**

**事实**:

- Firebase 三件套只挂在 `firebaseImplementation` 配置上: `AndroidApplicationFirebaseConventionPlugin` 里拼出 `Flavor.Firebase.name + "Implementation"`, 然后 `firebaseImplementation(platform(libsGoogle.findLibrary("firebase.bom")))`, `firebaseImplementation(findLibrary("firebase.analytics"))`, `firebaseImplementation(findLibrary("firebase.crashlytics.ndk"))`;
- 同一个插件把 Foss 的 Google-services 与 Crashlytics **任务**禁掉 (`tasks.configureEach` 按任务名匹配: 前缀 `process` 加 `Foss` 且后缀 `GoogleServices`, 或名字同时含 `Crashlytics` 与 `Foss`);
- 插件本身在 `app/build.gradle.kts` 的 plugins 块无条件应用;
- 仓库里没有任何验证任务或测试去检查 Foss 产物不含 Firebase 类, CI 也只跑 `./gradlew assembleFossDebug` (`.github/workflows/android-ci.yml`).

**为什么现状是对的**: Google-services 与 Crashlytics 两个 Gradle 插件由约定插件在项目级 `apply` (`apply(plugin = googlePlugin("googleServices"))`, `apply(plugin = googlePlugin("firebase-crashlytics"))`), 加载跟 "构建哪个变体" 无关, 而 `assembleDebug` 这类伞任务一次要处理双变体 — 所以 Foss 构建里插件照样会加载, 能做的只有禁用它的 Foss 任务. "全量应用插件 + 按 Foss 变体禁用任务" 是这种结构下的标准解法, 不是权宜之计. 依赖按变体挂载之后, Foss 产物里本来就没有 Firebase 类; [AGENTS.md](../../../../AGENTS.md) 也把它写成约定 (Foss 不需要 `google-services.json` 就能构建). 于是这条的真实状态是: **有实现, 有约定, 没有机器闸门** — 传递依赖万一泄漏, 没有东西会报告.

**若要闸门** (另议, 不是本条待办): 在 CI 加一步对 Foss 依赖树的 grep 是最小的可行方案, 不碰伞任务; 但 CI 的任务范围属于 AGENTS.md 的约定, 加步骤就是约定变更, 得负责人先点头 (同 [#50](../09-构建与CI/50-CI只编译不测试.md) 的处境). 实现细节层面的脆弱点登记在 [#55](../09-构建与CI/55-构建配置杂项.md) (按任务名匹配, 而不是按任务类型).
