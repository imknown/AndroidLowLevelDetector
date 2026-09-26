<a id="F9"></a>

# F9 · `MyModel.key` 用标题文本, 重复即崩溃 (P2, 隐患)

> 返回 [README 索引](../README.md) · [组1 · 随时可做 — 独立小修](../README.md#组1--随时可做--独立小修).


> 发现: Hy4-preview (F9). 2026-09-25 复核: **仍开放** (当前数据无碰撞的评估见第四节通过项 14).

**现象**: `LazyColumn` 的 `key` 取自 `MyModel.key`; `Raw` 标题的 key 就是标题文本本身. 一旦同页出现两个相同标题, `LazyColumn` 会直接抛 `IllegalArgumentException: Key was already used` — 而旧版 `DiffUtil.areItemsTheSame` 只会表现怪异, 不会崩.

**直接原因**: Compose 的 key 契约比 DiffUtil 严格 ([A·迁移期观察记录](../附录.md#appendix-c) 已记录了这点和 "当前三页无碰撞" 的结论).

**根本原因**: key 承担了两个职责 (列表项身份 + 动画/滚动状态锚点), 却复用了 "业务标题" 这个天然可能重复的值, 且代码里没有任何兜底.

**问题代码**:

```kotlin
// MyModel.key
val key: String
    get() = when (title) {
        is MyModelTitle.Res -> title.id.toString()
        is MyModelTitle.Raw -> title.text        // ← 可能重复
    }
```

```kotlin
// MyModelListScreen.kt → MyModelListContent() 的 items(key = { it.key })
key = { it.key },
```

**现状评估**: Prop 页三块数据源天然不冲突 (JVM 系统属性 / `Settings` 带类名前缀的键 / `getprop` 输出), Others, Home 用的是 `@StringRes`, 目前是安全的 — 所以定级 P2 而非 P0.

**修改方案** (任选):

1. 组合 key, 把 "类型" 纳入身份: `key = { "${it.type}:${it.key}" }` (最小改动, 能挡住跨类型碰撞);
2. 在数据源侧去重 (`distinctBy { it.key }`), 并在 `MyModelTitle.Raw` 的构造处加注释说明 "必须唯一";
3. 终极方案是给 `MyModel` 增加一个显式 `id` 字段, 让 key 不再依赖展示文本.


