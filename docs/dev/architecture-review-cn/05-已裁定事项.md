# 已裁定事项(不动 / 推迟)

> 返回 [README](README.md)。这些条目负责人已有明确裁定,没有待办的修改工作;保留记录,防止将来重复讨论或误当成欠账。

<a id="A2"></a>

## A2 无 foss「不含 Firebase 类」防护(FR-10 AC1)

**严重程度:P1(🟡) ｜ 处置:关闭(2026-09-12,现状即正解,见下)**

Firebase 依赖(BOM + analytics + crashlytics-ndk)只经 `firebaseImplementation` 配置挂载,且 Google-services / Crashlytics **任务**对 Foss 禁用(`AndroidApplicationFirebaseConventionPlugin.kt:57-68`,复验)——这只防止构建破坏。**没有任何检查任务、测试或依赖约束**验证 foss APK 不含直接或传递的 Firebase 类。当前没有传递泄漏,但出现泄漏也不会被发觉。

**裁定 2026-09-06(Q8)**:规格要求维持;任务禁用是已记录的临时方案;类级检查 TODO。

**处置(2026-09-12 负责人定)**:现状即正解,关闭。在「Firebase 插件只能项目级应用 + `assembleDebug` 等伞任务需同时构建双变体」的 Gradle 约束下,「全量应用 Firebase 插件 + 按 Foss 变体禁用其任务」就是标准解法,不是权宜之计。曾试按调用参数条件应用插件(foss 构建完全不加载 Firebase 插件),构建验证通过(含配置缓存切换),但伞任务会静默产出被污染的 foss 产物,已回退。foss 产物本就不含 Firebase(依赖按变体挂载 + 任务禁用);若将来要防依赖传递泄漏,可在 CI 加依赖树 grep(约 5 行,不影响伞任务),另议。

---

<a id="C4"></a>

## C4 遗留缺陷:拖拽中重建吞掉下拉手势(已知遗留)

**严重程度:P1(🟡) ｜ 处理:已裁定永久接受(决策⑤)**

若 Activity/Fragment 重建恰好落在下拉拖拽进行中,`onRefresh` 永不触发、没有任何请求存在,重建后的视图展示旧列表。手势重放的修复曾被构建、真机验证,又因过于侵入而回退。影响 FR-5 AC1/AC2 的用户感知。

**处理(决策⑤,已批准)**:永久接受——「显示过期」的一半由 AR-08 纯推导重构后的可观察设置治好;不再排期「待处理刷新重放」特性。

---

<a id="R1"></a>

## R1 工具栏不随滚动移出屏幕(FR-12)

**严重程度:P2(🟡,折叠进 Compose) ｜ 处理:决策④**

规格要求「Toolbar 逐渐推出屏幕**且** BottomNavigationView 隐藏」;代码中 `main_activity.xml` 的 AppBarLayout **没有 `layout_scrollFlags`**(复验:仅 BNV 挂了 `hide_bottom_view_on_scroll_behavior`),滚动只隐藏底栏,工具栏固定。也因此不存在工具栏退出动画可反转,规格 AC1 的「可中断动画」只对上了一半。

**处理(决策④,已批准)**:修订为 Compose 时代交付物,不在 XML 里做(会被 Compose 重写丢弃)。文档已修订,Compose 迁移时兑现。

---

<a id="R2"></a>

## R2 无 NavigationRail / 自适应布局(FR-12 + NFR-8)

**严重程度:P2(🟡,折叠进 Compose) ｜ 处理:决策④**

无 `layout-sw600dp`/`layout-large` 限定符、无 NavigationRail、无 WindowSizeClass(复验:纯手机布局)。与 R1 同批处理:修订为 Compose 时代交付物(WindowSizeClass / NavigationRail 在 Compose 中是惯用做法)。

---

<a id="R10"></a>

## R10 滚动条「可拖动」模式是 stub

**严重程度:P2(🟢) ｜ 处理:决策③**

`arrays.xml` 的 `scrollBarKeys` 第三项被注释掉(labels 2 个 / values 3 个不匹配),默认值为 None;实现只切换 `RecyclerView.isVerticalScrollBarEnabled`(`ViewExt.kt:8-15`),不存在 fast-scroll(复验)。**处理(决策③;2026-09-12 改口:先留着不删)**:数组维持现状 —— 第三项本来就被注释掉,设置页里选不到它;fast-scroll 若还想要,归 Compose 时代。

---

<a id="R12"></a>

## R12 命名漂移

**严重程度:P2(🟢) ｜ 处置:跳过(2026-09-12 负责人定)**

设置分组标题为 "Interface"(`strings.xml:5`;规格叫「显示」),License 行标题与规格的「开源许可」不一致。**处置(2026-09-12 负责人定):跳过不碰**——strings 文件是人工写 + 人工翻译的。

---

<a id="A4"></a>

## A4 不存在 root/Shizuku/dumpsys 层(FR-9)

**严重程度:P2(ℹ️ 非缺陷)**

现有技术:公开 API;反射(`SystemProperties`、`WebViewUpdateService`、内部资源);非 root shell(libsu `FLAG_NON_ROOT_SHELL`)跑 `getprop`/`getenforce`/`toybox`/`/proc/*`;NDK/JNI(`binderDetector` 模块,接在 Others 页)。FR-9 把提权层定为可选增强,**这不是缺陷**;但检测条目目录的 techniques 列应记录各条目实际可达的层级,不要承诺基于 root 的检测(注册表落地时的目录核对一并做)。

---

<a id="A7"></a>

## A7 构建配置脚枪(休眠)

**严重程度:P2(🟢) ｜ 处置:零改动(2026-09-12 负责人定)**

- `build.toml` 的 `isPreview` 开关一行即可把整个构建翻到 preview SDK(`compileSdkPreview="37.2-beta3"`、`targetSdkPreview="DEV"`)——当前 false,但一次误改就会违背 ADR-010 发布 Beta 基线构建(复验);
- `versionCode`(73)/`versionName`(1.18.8_unpublished)硬编码在 `build.toml`;
- `settings.gradle.kts` 内联 develocity/foojay 插件版本(在 catalog 之外)。

**处置(2026-09-12 负责人定):全部不改。**曾拟:守护/注解 `isPreview`、清理未用的 AGP canary 条目、解除版本硬编码、把内联插件版本收进 catalog。逐条核对后的结论:isPreview 守护与删条目「不用改」;versionCode/versionName 现状已达标(只写在 build.toml 一处、由 convention plugin 读取,代码零硬编码——「硬编码」实际不存在);插件版本收不进版本目录(目录文件本身就是 settings.gradle.kts 加载的,先有鸡先有蛋)。

---

返回 [README](README.md)
