# Material 3 与组件演进

> **状态**: **frozen**. 本篇评估 Compose Material 3 1.5.0 对本仓库界面层组件的直接影响与代码清理机会.

## 核心结论

1. `TopAppBar` 与相关 API 在 Material 3 1.5.0 正式转正为稳定版 API, 允许直接移除 `AppRoot.kt:142` 的实验性注解.  
   核实程度: 当场核过 (核对官方 M3 1.5.0 发布文档与 `app/src/main/java/net/imknown/android/forefrontinfo/ui/AppRoot.kt:142-144`).
2. 官方标准 `Modifier.nonInteractiveScrollbar` 在 `androidx.compose.material3` 正式提供, 满足删除临时自定义文件 `ScrollBarExt.kt` 的前置条件.  
   核实程度: 当场核过 (核对官方 API 文档与 `app/src/main/java/net/imknown/android/forefrontinfo/ui/common/ScrollBarExt.kt:30-34`).
3. `PullToRefreshBox` 增加了原生的 `enabled` 与 `threshold` 参数支持, 下拉刷新手势有了原生开关控制能力.  
   核实程度: 当场核过 (核对官方 M3 1.5.0 更新日志与 `MyModelListScreen.kt:95-111`).
4. Material 3 Expressive 正式稳定, 包含分段列表项, 变形指示器等组件, 为后续界面改版提供了标准组件.  
   核实程度: 转述未核 (转述自官方发布博客与 Material 3 1.5.0 更新说明).

## 仓库具体影响分析

### 1. `TopAppBar` 移除实验性注解

在 `app/src/main/java/net/imknown/android/forefrontinfo/ui/AppRoot.kt:142-144`:

```kotlin
// 现状代码:
@OptIn(ExperimentalMaterial3Api::class) // TopAppBar is still an experimental API in m3 1.4.0
@Composable
private fun AppRootShell(
```

在 M3 1.5.0 中, `TopAppBar`, `MediumTopAppBar`, `LargeTopAppBar` 以及 `TopAppBarDefaults` 已经离开实验阶段, 成为正式稳定 API.  
升级后可直接移除这一行的 `@OptIn(ExperimentalMaterial3Api::class)` 及其注释.

### 2. 官方 `nonInteractiveScrollbar` 替代临时扩展

本仓库在 `app/src/main/java/net/imknown/android/forefrontinfo/ui/common/ScrollBarExt.kt:30-34` 中留下了维护说明:

```kotlin
/**
 * Stands in for the official `Modifier.nonInteractiveScrollbar` (androidx.compose.material3), which the
 * pinned Material3 does not ship yet. When it lands: delete this file, and at each call site pass
 * `scrollState.scrollIndicatorState` instead of the scroll state, turning the `enabled` argument into
 * the condition that guards the call.
 */
```

Material 3 1.5.0 提供了原生的 `Modifier.nonInteractiveScrollbar` (位于 `androidx.compose.material3` 包):

```kotlin
@Composable
public fun Modifier.nonInteractiveScrollbar(
    state: ScrollIndicatorState,
    orientation: Orientation,
    thumbColor: Color = NonInteractiveScrollbarDefaults.thumbColor,
    trackColor: Color = Color.Transparent,
    thickness: Dp = NonInteractiveScrollbarDefaults.Thickness,
    thumbMinLength: Dp = NonInteractiveScrollbarDefaults.ThumbMinLength,
    thumbMaxLengthFraction: Float = NonInteractiveScrollbarDefaults.ThumbMaxLengthFraction,
    isFadeEnabled: Boolean = true,
    fadeDurationMillis: Int = NonInteractiveScrollbarDefaults.ThumbFadeDurationMillis,
    fadeDelayMillis: Int = NonInteractiveScrollbarDefaults.ThumbFadeDelayMillis,
    mainAxisTrackInset: Dp = NonInteractiveScrollbarDefaults.MainAxisTrackInset,
    crossAxisTrackInset: Dp = NonInteractiveScrollbarDefaults.CrossAxisTrackInset,
): Modifier
```

在两个调用点 (`MyModelListScreen.kt:118` 与 `SettingsScreen.kt:160`):

```kotlin
// 现状:
.nonInteractiveScrollbar(listState, enabled = scrollBarMode.drawsScrollBar)

// 升级后官方写法:
.then(
    if (scrollBarMode.drawsScrollBar) {
        Modifier.nonInteractiveScrollbar(
            state = listState.scrollIndicatorState,
            orientation = Orientation.Vertical,
        )
    } else {
        Modifier
    }
)
```

替换后可完整删除 `app/src/main/java/net/imknown/android/forefrontinfo/ui/common/ScrollBarExt.kt` 整个文件 (89 行自定义绘制代码).

### 3. `PullToRefreshBox` 原生参数补充

在 `app/src/main/java/net/imknown/android/forefrontinfo/ui/base/list/MyModelListScreen.kt:95`:
目前是通过外部状态控制 `isRefreshing`, 新版增加了原生 `enabled` 与 `threshold` 属性, 当页面正在初始加载或无网络时可直接借由 `enabled = false` 禁用过度下拉手势.

### 4. `material3-ripple` 与焦点环

M3 1.5.0 抽离了专用的 `androidx.compose.material3:material3-ripple` 构件, 统一键盘与外设焦点状态下的绘制环.  
本仓库在 `MyModelCard.kt:42-49` 保留的空 `onClick = {}` 水波纹行为将直接受益于新版焦点感知.
