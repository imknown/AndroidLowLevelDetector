<a id="i56"></a>

# 56 "License" 行链到版本目录 toml

> 返回 [README 索引](../../README.md) · [10 · 权限与合规](../../README.md#10--权限与合规).

**严重程度: P2(🟢) | 修复难度: 低 | 状态: 有意接受 (toml 目录页即 "引用的开源库" 清单, 与摘要一致; 许可页或生成工具归单独议)**


**事实** (都在文件里可核):

- `app/src/main/java/net/imknown/android/forefrontinfo/ui/settings/res/values/strings.xml` 里 `about_licenses_title` = `License`,  
  `about_licenses_summary` = `Referenced open-source repositories`,  
  `about_licenses_uri` = `https://github.com/imknown/AndroidLowLevelDetector/blob/master/gradle/toml`;
- `SettingsScreen.kt → settingsLinks` 把它和商店, 源码, 隐私政策, 译者信息四行并列渲染成 `SettingsLink`,  
  点击走 `openInExternal()` 的 `ACTION_VIEW`, 也就是在浏览器里打开那个 `gradle/toml` 目录页;
- 应用内没有许可页, 也没有别处承载它: `app/src/main/assets/` 只有 `lld.json`;  
  五个版本目录与 `build-logic` 里没有 AboutLibraries 一类的许可生成依赖;  
  除这几条 `about_licenses_*` 资源及其在 `settingsLinks` 的引用之外,  
  代码里再没有许可相关的入口;
- 许可文本本身只在仓库根 `LICENSE` (Apache-2.0 全文), `README.md` 不提许可.

**怎么处理**: 负责人已定接受现状: `gradle/toml` 目录页本身就是所用开源库的清单, 与摘要  
"Referenced open-source repositories" / "引用的开源库" 的语义一致, 不改 URI, 不加许可页.  
原评估里 "一个 toml 链接算不上合格的 License 入口" 的缺口 (许可文本不在手边) 若将来要补,  
走应用内许可页或 AboutLibraries 一类生成工具, 影响面超出小修队列定位, 归单独议;  
改链 `LICENSE` 的方案未采纳: 它承载的是本应用自身的 Apache-2.0, 与 "引用的开源库" 摘要对不上.
