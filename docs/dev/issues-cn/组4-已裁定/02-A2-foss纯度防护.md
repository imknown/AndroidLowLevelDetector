<a id="A2"></a>

# A2 无 foss"不含 Firebase 类"防护 (FR-10 AC1)

> 返回 [README 索引](../README.md) · [组4 · 已裁定, 无待办](../README.md#组4--已裁定-无待办).


**严重程度: P1(🟡) | 处置: 关闭 (2026-09-12, 现状即正解, 见下)**

Firebase 依赖 (BOM + analytics + crashlytics-ndk) 只经 `firebaseImplementation` 配置挂载, 且 Google-services / Crashlytics **任务**对 Foss 禁用 (`AndroidApplicationFirebaseConventionPlugin 里的 isGoogleServices 判断`, 复验) — 这只防止构建破坏.**没有任何检查任务, 测试或依赖约束**验证 foss APK 不含直接或传递的 Firebase 类. 当前没有传递泄漏, 但出现泄漏也不会被发觉.

**裁定 2026-09-06(Q8)**: 规格要求维持; 任务禁用是已记录的临时方案; 类级检查 TODO.

**处置 (2026-09-12 负责人定)**: 现状即正解, 关闭. 在 "Firebase 插件只能项目级应用 + `assembleDebug` 等伞任务需同时构建双变体" 的 Gradle 约束下,"全量应用 Firebase 插件 + 按 Foss 变体禁用其任务" 就是标准解法, 不是权宜之计. 曾试按调用参数条件应用插件 (foss 构建完全不加载 Firebase 插件), 构建验证通过 (含配置缓存切换), 但伞任务会静默产出被污染的 foss 产物, 已回退. foss 产物本就不含 Firebase(依赖按变体挂载 + 任务禁用); 若将来要防依赖传递泄漏, 可在 CI 加依赖树 grep(约 5 行, 不影响伞任务), 另议.


