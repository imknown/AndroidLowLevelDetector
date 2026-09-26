<a id="A4"></a>

# A4 不存在 root/Shizuku/dumpsys 层 (FR-9)

> 返回 [README 索引](../README.md) · [组4 · 已裁定, 无待办](../README.md#组4--已裁定-无待办).


**严重程度: P2(ℹ️ 非缺陷)**

现有技术: 公开 API; 反射 (`SystemProperties`, `WebViewUpdateService`, 内部资源); 非 root shell(libsu `FLAG_NON_ROOT_SHELL`) 跑 `getprop`/`getenforce`/`toybox`/`/proc/*`;NDK/JNI(`binderDetector` 模块, 接在 Others 页). FR-9 把提权层定为可选增强, **这不是缺陷**; 但检测条目目录的 techniques 列应记录各条目实际可达的层级, 不要承诺基于 root 的检测 (注册表落地时的目录核对一并做).


