<a id="N16"></a>

# N16 lint 实测: 9 条真死资源 + 空 super 调用 (P3)

> 返回 [README 索引](../README.md) · [组3 · 暂缓](../README.md#组3--暂缓-原因见危害列标注).


**结论**: `lintFossDebug` 0 error / 11 warning, 编译零告警. 逐一核对引用后: 9 条真死资源 — `about_shop/source/privacy_policy/licenses/translator_more_info/version_key`, `interface_themes_power_saver_key` (随 AppCompat 退役的主题档), `hw_binder_status`, `vnd_binder_status` (设置页手写 M3 重建后 SP 键改硬编码, 旧键资源成遗骨); 1 条 `EmptySuperCall` (`HomeViewModel.onCleared() 的多余 super`); 1 条半误报 `leak_canary_display_activity_label` (被 LeakCanary 自己的 manifest 消费, lint 看不见).
**修复方向**: 纯删 + 顺手删 super; lint 零配置的现状因此不算欠账, 真正缺的是 CI 跑它 (QW-1).


