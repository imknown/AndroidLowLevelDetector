<a id="N7"></a>

# N7 configureCompose() 的 Compose 开关是死代码 (P2)

> 返回 [README 索引](../README.md) · [组3 · 暂缓](../README.md#组3--暂缓-原因见危害列标注).


**结论**: `build-logic → Compose.kt → configureCompose()` 的 `when (this)` 判断的是 Project 本身, Project 永远不实现 ApplicationExtension/LibraryExtension, `buildFeatures.compose = true` 从未被设置.
**证据**: 现在能编译全靠 KGP 2.x 应用 `org.jetbrains.kotlin.plugin.compose` 时自动打开该开关 (官方文档允许省略); 这段 "看起来在开 Compose" 的代码实际从不运行, 照此模式给库模块配置会静默失效.
**修复方向**: 删掉 when, 需要时在 android 扩展上显式设置.


