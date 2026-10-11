<a id="i52"></a>

# 52 archivesName 嵌入分钟级时间戳与 git 分支名

> 返回 [README 索引](../README.md) · [9 · 构建与 CI](../README.md#9--构建与-ci).

**严重程度: P2 | 修复难度: 中**

**影响文件**:  
`build-logic/convention/src/main/kotlin/net/imknown/android/forefrontinfo/android/app/AndroidApplicationConventionPlugin.kt`,  
`app/src/main/java/net/imknown/android/forefrontinfo/ui/home/datasource/LldDataSource.kt`

**结论**: `AndroidApplicationConventionPlugin.configureName()` 在配置阶段取 `Instant.now()` (格式 `yyyyMMdd-HHmm`,  
只到分钟) 和 `git rev-parse --abbrev-ref HEAD` 的输出,  
拼成 `archivesName.set("lld-$versionName-$versionCode-$currentDatetime-$currentGitBranchName")`,  
同一句里还把分支名写进  
`BuildConfig.GIT_BRANCH`.

**证据**:

- 每次配置都算出一个新名字 (粒度到分钟): 同一份内容换不出可复用的产物标识,  
  以产物名为输入的下游 (缓存, artifact 收集) 每次都对不上;
- 值是配置阶段一次性算出的字符串, 随配置缓存一起被序列化, 命中时连旧值一起复用, 所以它不是 "当下时间";
- `providers.execute("git", ...)` 里就是 `exec { commandLine(...) }.standardOutput.asText.get()`, 当场执行:  
  没有 git 或 `rev-parse` 非零退出, 配置阶段直接失败;
- 分离 HEAD 时 `--abbrev-ref HEAD` 输出字面量 `HEAD`, 于是 `BuildConfig.GIT_BRANCH == "HEAD"`,  
  而 `LldDataSource.fetchOnlineLldJsonStringOrThrow()` 的在线地址是 `"https://$urlPrefixLldJson/${BuildConfig.GIT_BRANCH}/app/src/main/assets/$LLD_JSON_NAME"`  
  所以分支段变成 `HEAD`, 这个 URL 指向不存在的路径, 联网刷新 lld.json 失败.

**修复方向**: 产物名的时间戳交给 CI 在 artifact 命名上做, 构建内部保持稳定名; 分支相关值改由 CI 显式注入,  
在线 URL 指向稳定 ref.  
两条都得保持 configuration-cache 兼容 ([AGENTS.md](../../../../AGENTS.md) 的约定),  
也就是不能改成运行期才读时钟或 git 的任务.

用分支名拼在线数据 URL 本身是另一件事, 有它自己的取舍, 本条不重开,  
这里只登记 `archivesName` 这一侧机械产生的后果 (缓存不命中, 无 git 环境失败, 分离 HEAD 时 URL 段变 `HEAD`).
