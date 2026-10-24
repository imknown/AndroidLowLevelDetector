<a id="i41"></a>

# 41 · 图标 `fillColor` 硬编码黑色, 正确性依赖 `Icon` 的默认 tint

> 返回 [README 索引](../../README.md) · [7 · UI 与无障碍](../../README.md#7--ui-与无障碍).

**严重程度: P3 | 修复难度: 低 (补注释) | 状态: 已修复 (依赖已在调用处留痕, 现状保持)**


**现象**: 四个导航图标把 `?attr/colorOnSurface` 改成了 `#FF000000`. 四个文件的 `android:fillColor` 都在第 8 行:  
`ui/home/res/drawable/ic_home_24dp.xml`, `ui/others/res/drawable/ic_others_24dp.xml`,  
`ui/prop/res/drawable/ic_prop_24dp.xml`, `ui/settings/res/drawable/ic_settings_24dp.xml`.

```diff
-        android:fillColor="?attr/colorOnSurface"
+        android:fillColor="#FF000000"
```

改动分两步完成: `9b3404a6` 把 `@color/md_theme_onSurface` 换成 `?attr/colorOnSurface`,  
`600629f7` 在把窗口主题换成平台外壳并删掉 `md_theme` 色板的那次提交里把它换成 `#FF000000`.

**评估**: **方向是对的**, 因为 XML 主题现在只管窗口外壳, 不可能跟着 App 内主题 (例如 "系统浅色 + 应用强制深色")  
走, 保留 `?attr` 反而会取到错误的颜色.

**风险**: 正确性**完全依赖** "必须用带 tint 的载体承载". 眼下这条依赖链是干净的,  
四个 drawable 的唯一消费点就是 `AppRoot()` 里的 `Icon(painterResource(tab.iconRes), contentDescription = null)`  
(在 `NavigationBarItem` 的 `icon` 槽位), 全仓 `Image(` 零使用; 深色下取到的也是内容色而不是纯黑,  
因为 M3 `Icon` 默认 `tint = LocalContentColor.current`,  
而 `NavigationBarItem` 为图标槽位提供 `LocalContentColor`,  
tint 是整体替换 RGB (这几条是 material3 的行为, 不在本仓库代码里).  
哪天把同一个 drawable 放到 `Image(painter = painterResource(...))` 或不带 `colorFilter` 的地方,  
立刻变成纯黑且深浅色都不对.

**怎么处理的**: 现状保持, 风险已在 `AppRoot.kt` 的 `icon` 槽位调用处以注释留痕:  
"图标 drawable 本体是纯黑 (fillColor 硬编码): 实际颜色完全依赖 Icon 默认 tint  
(LocalContentColor), 勿放到无 tint 的载体 (如裸 Image), 否则深浅色都不对".
