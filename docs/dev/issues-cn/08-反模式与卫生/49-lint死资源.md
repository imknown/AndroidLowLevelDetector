<a id="i49"></a>

# 49 lint 检查项: 9 条真死资源 (P3)

> 返回 [README 索引](../README.md) · [8 · 反模式与卫生](../README.md#8--反模式与卫生).

**严重程度: P3 | 修复难度: 低 (纯删)**

**结论**: 下面这份清单是静态核对的结果 (声明处与引用处各 grep 一遍), 不是 lint 输出 — `:app` 今天没有任何 lint 配置 (无 `lint.xml`, 无 `lint {}` 块), CI 也不跑 `lintFossDebug`, 所以这些项没人检查过, 只能靠人或靠 [#50](../09-构建与CI/50-CI只编译不测试.md) 的门禁. 清单逐条对代码复核过, 今天仍成立.

**9 条真死资源** (声明了, 代码与资源都无引用):

- `ui/settings/res/values/strings.xml → about_shop_key` / `about_source_key` / `about_privacy_policy_key` / `about_licenses_key` / `about_translator_more_info_key` / `about_version_key` — 六条 `translatable="false"` 的存储键. 同一批的 `about_*_title` / `_summary` / `_uri` 都还在用 (`SettingsScreen.kt` 的 `settingsLinks` 列表), 只有键成了遗骨.
- `ui/settings/res/values/strings.xml → interface_themes_power_saver_key` — 可翻译的主题选项标签 ("Follow power saver"), 但 `ui/settings/res/values/arrays.xml → themeKeys` 只剩跟随系统 / 常亮 / 常暗三项, 其注释写明 "power saver" 随 de-AppCompat 退役.
- `ui/others/res/values/strings.xml → hw_binder_status`, `vnd_binder_status` — 按分区拆开的 Binder 位数标题, 四个语言档都有译文; 在用的那行是 `binder_status` (`OthersRepository.getBinderStatus()`), 这两条没有任何引用.

**1 条半误报**: `app/src/debug/res/values/strings.xml → leak_canary_display_activity_label` — lint 的引用分析看不见它, 实际被 LeakCanary 自己的 manifest 经资源合并消费 (`app/build.gradle.kts` 的 `debugImplementation(libsThirdParty.bundles.leakCanary)` 把它带进 debug 变体), 不是死资源.

**修复方向**: 纯删九条资源; `leak_canary_display_activity_label` 保留. (本条曾经的 "1 条 `EmptySuperCall`" — `HomeViewModel.onCleared()` 的空 `super` 调用 — 已随该监听器整体删除而消失, `onCleared` 现在只剩一句过去时注释, 记录见 git log.) `:app` 零 lint 配置这个现状因此不算欠账, 真正缺的是 CI 跑它 ([#50](../09-构建与CI/50-CI只编译不测试.md)).
