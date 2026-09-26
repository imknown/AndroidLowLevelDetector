<a id="F5"></a>

# F5 · `Card(onClick = {})` 空点击 (P1, 改错)

> 返回 [README 索引](../README.md) · [组4 · 已裁定, 无待办](../README.md#组4--已裁定-无待办).


> 发现: Hy4-preview (F5) = Qwen3.8-Flash (#5, 同条). **已裁定保留** (2026-09-22), 2026-09-25 复核: 卡片注释与裁定一致.

**现象**: 最新提交 `cb4104aa` 为了复刻旧 `MaterialCardView` 的水波纹, 把卡片换成了 `onClick` 重载.

**直接原因**: 旧 XML 确实是 `android:clickable="true" android:focusable="true"` 且**没有**点击监听 — "可点击但没反应" 是旧代码自带的毛病.

**根本原因**: 把 View 时代的毛病当成了需求原样复刻. 在 Compose 里 `Card(onClick = ...)` 不只是多一个水波纹: 它会挂上 `clickable` 语义节点并引入 `minimumInteractiveComponentSize`, 于是 TalkBack 会朗读 "双击以激活", 用户照做却毫无反馈 — **从 "视觉小瑕疵" 升级成了 "无障碍缺陷"**.

**问题代码**:

```kotlin
// MyModelCard.kt → MyModelCard() 的 Card(onClick = {})  (cb4104aa 当时行号; 加了处置注释后是 42-60)
Card(
    // Legacy MaterialCardView was clickable + focusable with no click listener = ripple-only feedback
    onClick = {},                       // ← 空点击
    modifier = modifier.fillMaxWidth().animateContentSize(),
    ...
)
```

**修改方案** (推荐 A):

- **A**: 卡片本就不需要交互, 去掉 `onClick`, 回到无点击重载:

  ```kotlin
  Card(
      modifier = modifier.fillMaxWidth().animateContentSize(),
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
  ) { ... }
  ```

- **B**: 确实想要水波纹, 就只取水波纹, 交出语义控制权:

  ```kotlin
  modifier = modifier
      .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = ripple(),
          onClick = {},
      )
      .clearAndSetSemantics {}   // 明确告诉无障碍服务: 这不是一个可操作控件
  ```

  **另一份复查的补充 (Qwen)**: B 的等价写法也可以用 `Modifier.combinedClickable(interactionSource = ..., indication = LocalIndication.current, onClick = {})`(或 `Modifier.indication(...)` + 只读 `interactionSource`) — 既能出波纹又不会被识别为 button.

  > 另注: `animateItem()` + `animateContentSize()` 同时用是官方推荐组合 (前者管位移, 后者管自身高度), 这部分没问题; `Card(onClick)` 带来的 `minimumInteractiveComponentSize` (48dp) 当前也不影响布局 (卡片实际高度约 67dp > 48dp).

> **处置 (2026-09-22 负责人定)**: A, B 都不采纳 — **保留现在的水波纹**, `Card(onClick = {})` 不动; a11y 语义问题按 "已知接受项" 就地标记 (`MyModelCard() 里那段 onClick 注释` 的注释已写明本条, 影响面与出路); 点击行展开详情 (BL-1)**明确暂不做**, 所以也就没有 "给 `onClick` 一个真动作" 的需求, B 方案作为将来真要修 a11y 时的参考保留在上面.


