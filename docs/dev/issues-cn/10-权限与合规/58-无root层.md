<a id="i58"></a>

# 58 不存在 root / Shizuku 层 — 非缺陷

> 返回 [README 索引](../README.md) · [10 · 权限与合规](../README.md#10--权限与合规).

**严重程度: P2(ℹ️ 非缺陷) | 修复难度: — | 处置: 有意接受, 无待办**

留这一条只为防止有人把它当欠账重新提一遍: [AGENTS.md](../../../../AGENTS.md) 写的是 "libsu 非 root 模式 (`ui/common/ShellLibSu.kt`, `Shell.FLAG_NON_ROOT_SHELL`): there is no root layer", 代码与之一致.

**取证**: `app/src/main/java/net/imknown/android/forefrontinfo/base/MyApplication.kt → initShellAndProperty()` 给 libsu 的默认 Builder `setFlags(Shell.FLAG_NON_ROOT_SHELL)`, 活壳实现是 `ui/common/ShellLibSu.kt` (`Shell.cmd(cmd).exec()`); 全仓 grep `shizuku`, `Su.root`, `FLAG_ROOT`, `requestRoot` 在源码里零命中, 只命中 `.idea/workspace.xml` 里的历史任务记录.

需要提权的探针是被明确停掉的, 不是忘了写: `ui/home/datasource/AndroidDataSource.kt` 里 `CMD_BOOT_PARTITION` (`ls /dev/block/bootdevice/by-name | grep boot_`) 与 `CMD_LL_DEV_BLOCK_SUPER` (`ls -l /dev/block/by-name/super`) 两条注释掉的常量各自标着 `/* root needed */`; WebView 那区注释掉 `dumpsys webviewupdate` 并写明 "Need `android.permission.DUMP`", 实际改走反射.

**当前检测手段实际用的四层**:

- 公开 API;
- 反射: `base/.../property/impl/PropertyDefault.kt` 反射 `android.os.SystemProperties` 的 `get` / `getBoolean`; `HomeRepository.getBuildInWebViewProvidersAndroid7()` 反射 `android.webkit.WebViewUpdateService.getAllWebViewPackages()` 与 `WebViewProviderInfo` 字段; `HomeRepository` 经 `Resources.getSystem().getIdentifier("config_defaultModuleMetadataProvider", "string", "android")` 读内部资源;
- 非 root shell: `getprop` (`ui/prop/datasource/PropertiesDataSource`), `getenforce` 与 `toybox --version` (`AndroidDataSource`), `cat /proc/mounts` (`MountDataSource`), `cat /proc/version` (`ui/others/datasource/KernelDataSource`);
- NDK / JNI: `binderDetector` 模块的 `BinderDetector.getBinderVersion(driver)`, 由 `ui/others/datasource/ArchitectureDataSource` `System.loadLibrary(...)` 后接进 Others 页.

提权是待想的功能而不是缺失的实现: `docs/dev/JOTTINGS.md` 的 features todo 在 `settings` 下记着 `root mode`. 所以本条目没有待办. 各检测条目实际够得着哪一层, 等 [#03](../1-架构与分层/03-无三态模型与注册表.md) 的检测条目目录真落地时随目录一并记录, 目录里不要承诺基于 root 的检测.
