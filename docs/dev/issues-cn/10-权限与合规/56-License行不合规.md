<a id="i56"></a>

# 56 "License" 行链到版本目录 toml

> 返回 [README 索引](../README.md) · [10 · 权限与合规](../README.md#10--权限与合规).

**严重程度: P2(🟢) | 修复难度: 低**

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

**待办**: "一个 toml 链接算不上合格的 License 入口" 是判断, 不是取证, 要不要改 (改指 `LICENSE`,  
或做应用内许可页, 或引入许可清单生成工具), 由负责人定.

**改的话动哪几个文件**: `about_licenses_uri` 标着 `translatable="false"`,  
改它的值只碰默认 `values/strings.xml` 一处,  
不涉及 `zh-rCN` / `zh-rTW` / `fr-rFR` 三份译文 ([AGENTS.md](../../../../AGENTS.md) 的本地化规则管的是用户可见文案, 技术值不在此列).  
若改的是标题或摘要文案, 那三个 locale 各有一份 (`协议` / `協議` / `Licences` 与各自 summary), 要同步.
