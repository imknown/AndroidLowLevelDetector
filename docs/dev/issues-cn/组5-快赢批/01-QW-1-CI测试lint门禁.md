<a id="qw-1"></a>

# QW-1 CI 只编译, 不跑测试和 lint(AR-16 零真实测试, CI 只编译不测试)

> 返回 [README 索引](../README.md) · [组5-快赢批](../README.md#目录).


**改动量: 1 行 | 文件: `.github/workflows/android-ci.yml`**

## 问题代码

```yaml
# .github/workflows/android-ci.yml → "Build with Gradle" 步骤
- name: Build with Gradle
  run: ./gradlew assembleFossDebug
```

## 引入提交

| 日期 | 提交信息 | 当时目的 |
|---|---|---|
| 2025-05-29 | Update android.yml | 整理工作流后只剩"构建"一步; 之后的多次 "Chore: Update CI / dependencies" 只动依赖与 SDK 安装, 门禁范围再没变过 |

## 修改后

```yaml
- name: Build with Gradle
  run: ./gradlew assembleFossDebug testFossDebugUnitTest :base:testDebugUnitTest lintFossDebug
```

注意:

- `testFossDebugUnitTest` 只跑 app 模块, base 模块的测试要单列 `:base:testDebugUnitTest`(QW-2 / QW-5 附带的测试都在 base).
- 单条命令默认"前一个任务失败就停": 想即使测试挂了也把 lint 报告跑全, 可在 `./gradlew` 后加 `--continue`. 为 review 方便, 首版保持单行, 不加参数.
- 合入前先在本地各跑一遍: 若存量测试或 lint 有 error,CI 首跑会变红, 需要先清掉或单独裁定. 另注: AGP 自带 lint 默认**不会**把 Kotlin 弃用告警判成 error — 想让这类问题在 CI 拦截, 需另行配置 (如 `warningsAsErrors`), 不在本条范围.

## 直接原因

门禁链条只有"能编译"一环: 就算有人写了测试, lint 有告警, 合入前也不会被发现.

## 根本原因

"能编译 = 没问题" 的默认假设. 对本清单其余所有修复而言这很致命 — 它们大多是行为等价重构, 没有回归保护网就只能靠真机手点.

---


