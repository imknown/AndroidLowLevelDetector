<a id="F8"></a>

# F8 · `ExtendedColors.of()` 多标了 `@Composable` (P2, 改多)

> 返回 [README 索引](../README.md) · [组1 · 随时可做 — 独立小修](../README.md#组1--随时可做--独立小修).


> 发现: Hy4-preview (F8). 2026-09-25 复核: **仍开放**.

**现象**: 一个纯映射函数被声明成 `@Composable`.

**直接原因**: 大概率是 "它返回颜色, 在组合里用" 于是顺手加了注解.

**根本原因**: 对 `@Composable` 的语义理解有偏差 — 它表示 "这个函数会往组合里写东西". 后果: 只能在组合上下文调用 (想在 `@Composable` 之外的映射/预览数据构造里用就直接编译不过), 并且多引入一层重组作用域, 纯属自缚手脚.

**问题代码**:

```kotlin
// ExtendedColors.of()
@Composable
fun ExtendedColors.of(status: StatusColor): Color = when (status) { ... }
```

**修改方案**: 去掉 `@Composable` 即可 (`MyModelCard() 里的 LocalExtendedColors.current.of(model.color)` 调用处无需改动).


