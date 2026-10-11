<a id="i46"></a>

# 46 · `ExtendedColors.of()` 多标了 `@Composable` (P2)

> 返回 [README 索引](../README.md) · [8 · 反模式与卫生](../README.md#8--反模式与卫生).

**严重程度: P2 | 修复难度: 低**  
**影响文件: `ExtendedColors.kt`, `MyModelCard.kt`**

**现象**: 一个纯映射函数被声明成 `@Composable`.

**问题代码** (`ui/theme/ExtendedColors.kt`):

```kotlin
@Composable
fun ExtendedColors.of(status: StatusColor): Color = when (status) {
    StatusColor.NO_PROBLEM -> noProblem
    StatusColor.WARNING -> warning
    StatusColor.CRITICAL -> critical
    StatusColor.NONE -> Color.Unspecified
}
```

函数体只读接收者 `ExtendedColors` 的三个字段, 不读任何 State, 不读 CompositionLocal, 也不 `remember`.

**直接原因**: 全项目唯一调用处就在组合里 (`MyModelCard()` 的 `.background(LocalExtendedColors.current.of(model.color))`),  
注解跟着使用场景加上了, 因为代码里没有别的理由要它做 Composable.

**根本原因**: 对 `@Composable` 的语义理解有偏差: 它表示 "这个函数要在组合上下文中运行". 后果是这个限制白加:  
函数只能在组合上下文调用,  
想在非 Composable 的地方用它 (比如映射函数, 或预览数据构造) 直接编译不过, 而它返回的值本身与组合无关.

**修改方案**: 去掉 `@Composable` 即可. 唯一调用处 `MyModelCard()` 里的 `LocalExtendedColors.current.of(model.color)` 本来就在组合中, 调用点无需改动.
