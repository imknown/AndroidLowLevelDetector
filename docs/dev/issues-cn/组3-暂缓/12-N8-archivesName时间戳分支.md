<a id="N8"></a>

# N8 archivesName 嵌入分钟级时间戳与 git 分支名 (P2)

> 返回 [README 索引](../README.md) · [组3 · 暂缓](../README.md#组3--暂缓-原因见危害列标注).


**结论**: `AndroidApplicationConventionPlugin → configureName()` 在配置阶段取 `Instant.now()` (分钟级) 与 `git rev-parse --abbrev-ref` 拼进 archivesName, 并把分支名注入 `BuildConfig.GIT_BRANCH`.
**证据**: 产物名每分钟都变 (输出目录堆积历史 APK, 任务缓存永不命中); configuration cache 命中时时间戳是旧值; 无 git 的环境构建直接失败; detached HEAD 时 GIT_BRANCH 为 "HEAD", lld.json 在线 URL 失效 — 最后这条是 R8 已裁定风险的加重情节, 仅登记不改裁定.
**修复方向**: 产物时间戳交给 CI artifact 命名; 分支相关值改由 CI 显式注入, URL 指向稳定 ref (随 R8).


