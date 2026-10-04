<a id="i43"></a>

# 43 比对语义事实上存在 — 逐行, ad hoc

> 返回 [README 索引](../README.md) · [8 · 反模式与卫生](../README.md#8--反模式与卫生).

**严重程度: P1(🟡) | 修复难度: 中 (特征测试锁定, 注册表落地时迁入注册表条目)**
**影响文件: `HomeRepository.kt`**

约 12 个 Home 行已经对参考 json (`lld.json`) 做了比对, 但各写各的, 整体没法验证, 也没有测试: android, sdkExtension, buildId, 两条 securityPatch, kernel, mainline, vndk, toybox, webView, outdated-apk, mode — 每行的比较方式和它比的对象都写在该行函数体里.

三处最明显的 ad hoc 写法, 都在 `HomeRepository`:

- `HomeRepository.isDateHigherThanConfig()` (`detectBuildId()` 内的局部函数) 拿预览版偏移做魔法算术: `250205 - 250101`, 偏移加到 `myBuildIdDateIntOrNull` 上再和 `lldFirstBuildIdDateIntOrNull` 比.
- `HomeRepository.detectMainline()` 靠补后缀的字符串比较躲版本号位数问题: `"$versionName-01" >= latestGooglePlaySystemUpdates`. 这一处还叠着**量纲错位**: 左边是包的 `versionName` (任意格式), 右边是库里的日期 (`2026-09-01`) — `"3.1"` 这类 3-9 开头的版本字典序恒判 "已更新" 绿; 包查询失败时 `versionName = ""`, 恒判 "落后" 红, 而正确说法是 "无法判定".
- `HomeRepository.detectSecurityPatch()` 先整串字典序比 `mySecurityPatch >= lldSecurityPatch`, 再退一步比年月: `getSecurityPatchYearMonth(mySecurityPatch) >= getSecurityPatchYearMonth(lldSecurityPatch)`, 而 `getSecurityPatchYearMonth()` 的实现是 `substringBeforeLast('-')`. 这一条有一个**已知错例**: CDD 要求补丁级别为 `YYYY-MM-DD`, 但现实中确有设备 (老/低端/不规范 ROM) 只报 `YYYY-MM` — 此时 `substringBeforeLast('-')` 取出的是年份 (`"2026-09"` → `"2026"`), 两次比较都错, 完全同期的设备被判红 (设备 `2026-09` 对库 `2026-09-05`: 整串比 `"2026-09" >= "2026-09-05"` 假, 年月比 `"2026" >= "2026-09"` 也假).

测试侧: 没有任何测试碰 `HomeRepository`. 全仓唯一有实际内容的测试类是 `base/src/test/.../base/extension/DateTimeExtTest.kt`, 测的是 `isLldDatetime()` 的格式契约; `app/src/test` 与 `base/src/test` 里其余仍是模板 `ExampleUnitTest`.

**处理**: 比较语义散在各行这件事, 需要先用特征测试把现在的行为原样锁住, 再谈重新设计 — 重新设计自然发生在检测器注册表落地时 ([#03](../01-架构与分层/03-无三态模型与注册表.md)). 顺序上注意: 上面标了 "已知错例" 的语义是**错的**, 不能原样锁 — 先修已知错例再锁定, 或者锁定时就把期望行为写成正确的, 别把 bug 钉进测试.

前置不在本条: 写特征测试要先把测试基建补上 ([#50](../09-构建与CI/50-CI只编译不测试.md)) — `gradle/toml/android.toml` 的 `test` bundle 目前只有 JUnit 4 (4.13.2), 仓库里没有 JUnit 5, MockK, Turbine ([#06](../01-架构与分层/06-技术栈缺口.md)). 是否引入 JUnit 5 这类新依赖, 按 AGENTS.md 的版本分层规则要由负责人评估后采纳, 不是本条能顺带决定的.
