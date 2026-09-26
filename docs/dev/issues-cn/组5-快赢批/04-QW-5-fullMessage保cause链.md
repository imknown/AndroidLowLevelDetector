<a id="qw-5"></a>

# QW-5 异常摘要丢整条 cause 链, 无因时输出 "Caused by: null." (AR-18.2 fullMessage 错误信息丢栈)

> 返回 [README 索引](../README.md) · [组5-快赢批](../README.md#目录).


**改动量: ~10 行 | 文件: `base/.../extension/ExceptionExt.kt`, 新增 1 个测试**

## 问题代码

```kotlin
// ExceptionExt.kt → Throwable.fullMessage
val Throwable.fullMessage
    get() = "${javaClass.canonicalName}: $message\nCaused by: ${cause?.message}."
```

## 引入提交

| 日期 | 提交信息 | 当时目的 |
|---|---|---|
| 2021-03-21 | Feat: Architecture refactor: Repositories and DataSources | 仓库化重构时引入该属性 |
| 2025-12-03 | Feat: Improve exception handling via UDF | 异常处理改造时写成现在的单层形式 |

## 修改后

```kotlin
val Throwable.fullMessage: String
    get() = generateSequence(this) { it.cause }
        .joinToString("\nCaused by: ") { "${it.javaClass.canonicalName}: ${it.message}" }
```

8 个调用方文件零改动. 两个修复点: cause 链整条保留 (原来只有一层, 深层根因全丢); 无 cause 时只输出一行, 不再出现 `Caused by: null.`(尾部句号也随之消失). 配套测试:

```kotlin
// base/src/test/java/net/imknown/android/forefrontinfo/base/extension/ExceptionExtTest.kt
class ExceptionExtTest {
    @Test
    fun fullMessageFollowsCauseChain() {
        val e = IllegalStateException("top", RuntimeException("root"))
        assertEquals(
            "java.lang.IllegalStateException: top\nCaused by: java.lang.RuntimeException: root",
            e.fullMessage
        )
        assertEquals("java.lang.IllegalStateException: top", IllegalStateException("top").fullMessage)
    }
}
```

范围说明: 异常类名出现在用户可见条目里是 C1(逐条错误隔离, 发布阻塞项) 已裁定的 "证据显示", 保留不动; 完整堆栈进 Log 属于后续增强, 不夹带在本条.

## 直接原因

模板只取一层 `cause?.message`, 链式异常的深层根因整条丢失; cause 为空时模板照样输出 `Caused by: null.`.

## 根本原因

字符串拼接模板没有按 "有没有 cause" 分支处理; 错误摘要的生成规则没有任何测试锁定, 坏了也不会被发现.

---


