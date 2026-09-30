<a id="i43"></a>

# 43 比对语义事实上存在 — 逐行, ad hoc

> 返回 [README 索引](../README.md) · [8 · 反模式与卫生](../README.md#8--反模式与卫生).

**严重程度: P1(🟡) | 修复难度: 中 (特征测试锁定, 注册表落地时迁入注册表条目)**
**影响文件: `HomeRepository.kt`**

约 12 个 Home 行已经对参考 json (`lld.json`) 做了比对, 但各写各的, 整体没法验证, 也没有测试: android, sdkExtension, buildId, 两条 securityPatch, kernel, mainline, vndk, toybox, webView, outdated-apk, mode — 每行的比较方式和它比的对象都写在该行函数体里.

三处最明显的 ad hoc 写法, 都在 `HomeRepository`:

- `HomeRepository.isDateHigherThanConfig()` (`detectBuildId()` 内的局部函数) 拿预览版偏移做魔法算术: `250205 - 250101`, 偏移加到 `myBuildIdDateIntOrNull` 上再和 `lldFirstBuildIdDateIntOrNull` 比.
- `HomeRepository.detectMainline()` 靠补后缀的字符串比较躲版本号位数问题: `"$versionName-01" >= latestGooglePlaySystemUpdates`.
- `HomeRepository.detectSecurityPatch()` 先整串字典序比 `mySecurityPatch >= lldSecurityPatch`, 再退一步比年月: `getSecurityPatchYearMonth(mySecurityPatch) >= getSecurityPatchYearMonth(lldSecurityPatch)`, 而 `getSecurityPatchYearMonth()` 的实现是 `substringBeforeLast('-')`.

测试侧: 没有任何测试碰 `HomeRepository`. 全仓唯一有实际内容的测试类是 `base/src/test/.../base/extension/DateTimeExtTest.kt`, 测的是 `isLldDatetime()` 的格式契约; `app/src/test` 与 `base/src/test` 里其余仍是模板 `ExampleUnitTest`.

**处理**: 比较语义散在各行这件事, 需要先用特征测试把现在的行为原样锁住, 再谈重新设计 — 重新设计自然发生在检测器注册表落地时 ([#03](../1-架构与分层/03-无三态模型与注册表.md)).

前置不在本条: 写特征测试要先把测试基建补上 ([#50](../9-构建与CI/50-CI只编译不测试.md)) — `gradle/toml/android.toml` 的 `test` bundle 目前只有 JUnit 4 (4.13.2), 仓库里没有 JUnit 5, MockK, Turbine ([#06](../1-架构与分层/06-技术栈缺口.md)). 是否引入 JUnit 5 这类新依赖, 按 AGENTS.md 的版本分层规则要由负责人评估后采纳, 不是本条能顺带决定的.
