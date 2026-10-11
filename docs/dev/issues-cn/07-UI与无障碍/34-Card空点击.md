<a id="i34"></a>

# 34 · `Card(onClick = {})` 空点击

> 返回 [README 索引](../README.md) · [7 · UI 与无障碍](../README.md#7--ui-与无障碍).

**严重程度: P1 | 修复难度: 中**

**待完成的需求** (复查基线 `cb4104aa`): 卡片保留 `Card(onClick = {})` 换回水波纹,  
换来的语义代价 (读屏把整卡报成按钮) 待还; 换 `onLongClick` 方案要重写展开.

**现象**: `MyModelCard()` 用 `onClick` 重载的 `Card`, 点击动作是一个空 lambda.

**直接原因**: 旧 XML 确实是 `android:clickable="true" android:focusable="true"` 且**没有**点击监听 (原  
`ui/base/list/res/layout/my_view_holder.xml` 的 `MaterialCardView` 根节点,  
已随 `a92e163d` 删除), 所以 "可点击但没反应" 是 View 时代自带的毛病.

**根本原因**: 把 View 时代的毛病当成了需求原样复刻. 在 Compose 里 `Card(onClick = ...)` 不只是多一个水波纹:  
这个重载在 material3 库里走 `Modifier.clickable` (库行为, 不在本仓库代码里), 于是卡片挂上可点击语义节点,  
并带上交互组件的最小尺寸约束 (`minimumInteractiveComponentSize`). 读屏因此把整张卡报成一个可操作控件,  
用户照做却没有对应的动作,  
**从 "视觉小瑕疵" 升级成了 "无障碍缺陷"**. 朗读出来的具体词句 (`MyModelCard()` 的注释记作 "double-tap to activate") 属读屏行为,  
需要真机 + TalkBack 核对; 那个最小尺寸下限是否真的改变这些卡片的高度也未测,  
本条不记数值. 代码侧能核实的是语义节点的挂载与 `onClick = {}` 的空实现.

**问题代码**:

```kotlin
// MyModelCard.kt → MyModelCard() 的 Card(onClick = {})
Card(
    // (上方是记录这个取舍的注释, 转述见下段)
    onClick = {},                       // ← 空点击
    modifier = modifier.fillMaxWidth().animateContentSize(),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
) { ... }
```

`onClick` 上方那段注释是这个取舍在代码里的留痕, 转述其内容: 旧 `MaterialCardView` 可点击可聚焦却没有监听,  
反馈只有水波纹,  
`onClick` 重载是把水波纹拿回来的 Compose 写法; 已知代价是空的 `onClick` 让 TalkBack 念 "double-tap to activate"  
而无事发生; 水波纹是有意保留的,  
行展开不在范围内; 真要撤销, 走 `clickable = false` / `clearAndSetSemantics {}`.

**为什么留着空点击而不是给它一个真动作**: 点击行展开详情明确不做, 所以 "给 `onClick` 一个真动作" 这条路不成立;  
而直接去掉 `onClick` 就同时失去水波纹, 那正是当初加它的唯一目的.

**出路**:

- **A**: 卡片本就不需要交互, 去掉 `onClick`, 回到无点击重载.  
  今天的代码在其余参数上已经等于 A 的写法 (`elevation = 0.dp` + `containerColor = surfaceBright`),  
  两者只差 `onClick` 这一行:

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

  等价写法也可以是 `Modifier.combinedClickable(interactionSource = ..., indication = LocalIndication.current, onClick = {})`,  
  或 `Modifier.indication(...)` 配一个只读 `interactionSource`, 都出波纹而不被识别成按钮.

**不算本条问题的一部分**: `animateContentSize()` (卡片自身高度变化, `MyModelCard()`)  
与 `animateItem()` (条目位移,  
`MyModelListScreen()` 的 `LazyColumn` item) 分工不同, 两者并存是要的, 不是重复动画.
