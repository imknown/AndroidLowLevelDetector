# 06-与已有issue的关系

> **状态**: **frozen**.

三条 open issue 都跟这次升级有关, 核过的结论如下.

## #37 滚动条 "可拖动" 模式是 stub

- 条目等的条件: 官方 `androidx.compose.material3` 的 `nonInteractiveScrollbar` 到位.  
  核实程度: 当场核过 (`docs/dev/issues-cn/07-UI与无障碍/37-滚动条可拖动是stub.md`).
- 这次升级兑现了这一半: material3 1.5.0 有官方 `Modifier.nonInteractiveScrollbar`.  
  核实程度: 当场核过 (官方 API 参考页).
- 官方扩展是非交互的, 条目里 "可拖动" 那一档仍然没有实现.  
  所以这次升级不等于整条 issue 完成, 只解决 "自绘换官方".  
  核实程度: 当场核过.

## #59 Expressive 主题跟进

- 条目等的条件: material3 1.5 线的 `MaterialExpressiveTheme` / `expressiveLightColorScheme()` / `MaterialTheme.motionScheme`.  
  核实程度: 当场核过 (该条目正文).
- material3 1.5.0 把 `MaterialExpressiveTheme` 与 `expressiveLightColorScheme` 转稳定了, 这条可以开始做.  
  核实程度: 当场核过.
- `MaterialTheme.motionScheme` 在 1.5.0 的 package summary 里有 `MotionScheme` 接口, `LocalMotionScheme` 已弃用.  
  核实程度: 当场核过 (官方 package summary).

## #60 Style API 跟进

- 条目等的条件: foundation 1.13 的 Style API.  
  核实程度: 当场核过 (该条目正文).
- foundation 1.13 里 Styles API 仍是实验, 官方撤掉了文档和 skill.  
  这条继续等, 这次升级不给它解锁.  
  核实程度: 当场核过 (官方发布博客).
