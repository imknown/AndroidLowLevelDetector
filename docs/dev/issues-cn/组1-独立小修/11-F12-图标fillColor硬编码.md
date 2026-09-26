<a id="F12"></a>

# F12 · 图标 `fillColor` 硬编码黑色, 正确性依赖 `Icon` 的默认 tint (P3, 隐性依赖)

> 返回 [README 索引](../README.md) · [组1 · 随时可做 — 独立小修](../README.md#组1--随时可做--独立小修).


> 发现: Hy4-preview (F12). 2026-09-25 复核: **仍开放** (建议补的 tint 依赖注释尚未加).

**现象**: 4 个导航图标把 `?attr/colorOnSurface` 改成了 `#FF000000`.

```diff
-        android:fillColor="?attr/colorOnSurface"
+        android:fillColor="#FF000000"
```

**评估**: **方向是对的** — XML 主题现在只管窗口外壳, 不可能跟着 App 内主题 (例如 "系统浅色 + 应用强制深色") 走, 保留 `?attr` 反而会取到错误的颜色. 深色模式下不会变成 "黑底黑图标", 因为 `Icon(painterResource(...))` 默认 `tint = LocalContentColor.current`, 而 `NavigationBarItem` 会为图标槽位提供正确的 `LocalContentColor`, `ColorFilter.tint` 会整体替换 RGB.

**风险**: 正确性**完全依赖** "必须用 `Icon` 承载". 哪天把同一个 drawable 放到 `Image(painter = painterResource(...))` 或不带 `colorFilter` 的地方, 立刻变成纯黑且深浅色都不对.

**修改方案**: 保持现状, 在 `AppRoot.kt` 的 `Icon(...)` 处补一行注释: "图标资源为纯黑, 颜色由 Icon 的默认 tint (LocalContentColor) 决定, 勿改用无 tint 的载体".


