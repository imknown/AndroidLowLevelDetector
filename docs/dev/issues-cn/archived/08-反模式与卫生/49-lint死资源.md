<a id="i49"></a>

# 49 lint 检查项: 死资源 (P3)

> 返回 [README 索引](../../README.md) · [8 · 反模式与卫生](../../README.md#8--反模式与卫生).

**严重程度: P3 | 修复难度: 低 (纯删) | 状态: 已了结 (7 条已删; 剩 2 条有意保留, 留待可能重启的按分区 Binder 检测)**

**结论**: 下面这份清单是静态核对的结果 (声明处与引用处各 grep 一遍), 不是 lint 输出, 因为 `:app` 今天没有任何 lint 配置 (无 `lint.xml`, 无 `lint {}` 块),  
CI 也不跑 `lintFossDebug`, 所以这些项没人检查过, 只能靠人或靠 [#50](../../09-构建与CI/50-CI只编译不测试.md) 的门禁. 清单逐条对代码复核过, 处置前仍成立.

**已删除 (7 条)**:

- `ui/settings/res/values/strings.xml → about_shop_key` / `about_source_key` / `about_privacy_policy_key` /  
  `about_licenses_key` / `about_translator_more_info_key` / `about_version_key`,  
  即六条 `translatable="false"` 的存储键. 它们是旧 View 时代设置页 (PreferenceFragment) 的存储键:  
  `SettingsFragment` 里的代码引用先随 Compose 重建 (`459e47e8`) 消失,  
  最后的引用随 `preferences.xml` 在 `633b71db` (de-AppCompat 清理) 删除, 六条键成了遗骨;  
  同一批的 `about_*_title` / `_summary` / `_uri` 都还在用 (`SettingsScreen.kt` 的 `settingsLinks` 列表), 只有键是死资源.
- `ui/settings/res/values/strings.xml → interface_themes_power_saver_key` 及三个翻译文件的译文,  
  可翻译的主题选项标签 ("Follow power saver"), 但 `arrays.xml → themeKeys` 只剩跟随系统 / 常亮 / 常暗三项  
  (power saver 主题模式已退役, `5962cf43`). 同名的 `_value` (`"1"`) **保留**:  
  `SettingsStore` 启动时的一次性迁移靠它识别旧存量值并写回跟随系统,  
  `parseThemeMode()` 的 `else` 分支本身也会把任何未知值折回跟随系统, 注释写明墓碑值不回收.

**保留 (2 条, 暂缓)**:

- `ui/others/res/values/strings.xml → hw_binder_status`, `vnd_binder_status` (四个语言档都有译文):  
  2020-03-09 `22aa5e6b` 随按分区拆行的 Binder 位数检测加入, 2020-09-10 `db6a3b53` (Remove useless binder)  
  把检测行删掉后一直无引用; 在用的是 `binder_status` (`OthersRepository.getBinderStatus()`).  
  两条可能是当时没做完的需求, 负责人决定搁置, 字符串留待将来重启该功能.

**1 条半误报**: `app/src/debug/res/values/strings.xml → leak_canary_display_activity_label`, lint 的引用分析看不见它,  
实际被 LeakCanary 自己的 manifest 经资源合并消费  
(`app/build.gradle.kts` 的 `debugImplementation(libsThirdParty.bundles.leakCanary)` 把它带进 debug 变体), 不是死资源.

(本条曾经的 "1 条 `EmptySuperCall`" (`HomeViewModel.onCleared()` 的空 `super` 调用) 已随该监听器整体删除而消失,  
`onCleared` 现在只剩一句过去时注释, 记录见 git log.)

`:app` 零 lint 配置这个现状因此不算欠账, 真正缺的是 CI 跑它 ([#50](../09-构建与CI/50-CI只编译不测试.md)).
