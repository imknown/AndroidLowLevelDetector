# A·迁移期观察记录

> 所属迁移计划：[README](README.md) · 与 [A·API 速查表](A-API速查表.md)、[A·API 速记手册](A-API速记手册.md)、[A·术语表](A-术语表.md) 并列的第四份附录

迁移过程中顺手发现的、**与迁移本身无关**的旧代码问题和**有意接受的**行为差异，集中记录在这里。它们不阻塞迁移，也不随某一步提交——旧代码按计划在第 7 步统一清理，本文件的问题清单会随步骤陆续追加。

## 2026-09-19 · MyAdapter：复用 ViewHolder 时 GONE 不恢复（旧代码潜伏 bug）

`MyAdapter.onBindViewHolder`（`ui/base/list/MyAdapter.kt`）在 `model.color == RES_ID_NONE` 时用 `sivColor.isGone = true` 藏掉色点，但另一个分支**从不把可见性恢复回来**。RecyclerView 会复用 ViewHolder：一个曾经显示过"无色点"条目的卡子，滚动复用后遇到**有**色点的条目，色点会悄悄消失。

View 世界的修法一行：`sivColor.isGone = (color == RES_ID_NONE)`。

发现于第 1 步评审：Compose 版 `MyModelCard` 用"条件即不组合"——没有色点时那颗色点根本不进组合树——所以这个 bug 在新实现里**天然消失**。旧列表按计划第 7 步删除，大概率永远轮不到修。

## 2026-09-19 · `android:textDirection="locale"` 不在 `MyModelCard` 里复刻（接受的偏差）

旧 `my_view_holder.xml` 的两个 TextView 都设了 `textDirection="locale"`。而 Compose 的 `TextDirection` **没有 `Locale` 常量**（只有 Ltr/Rtl/Content/ContentOrLtr/ContentOrRtl/Unspecified），material3 的 `Text` 也没有 `textDirection` 参数——想原样复刻都没有 API。

**决定：接受差异，不复刻。** 理由：

- 对齐行为已经等价——Compose 的 `Text` 默认 `TextAlign.Start` 跟随 `LocalLayoutDirection`，而 `LocalLayoutDirection` 自动跟随系统语言（locale），和旧 `textDirection="locale"` 的效果一致；
- 剩下的差异只有"文本内部双向字符（RTL/混排）的判定方式"：旧=强制按 locale，新=按文本第一个强方向字符——对本项目这类内容影响可以忽略；
- 反而强行写死 `TextDirection.Ltr` 会破坏 RTL 语言环境，画蛇添足。
