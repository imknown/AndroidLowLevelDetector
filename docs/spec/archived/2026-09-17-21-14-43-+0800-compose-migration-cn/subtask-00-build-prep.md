# ST-00: build-prep 修改计划 (补录)

> 状态: record (由目录位置声明). 计划依据:  
> 计划 03 章 "第 0 步 构建准备"  
> (已删; 58e469b7 收缩章号后改名 02-第0步-构建准备.md,  
> 取回用 `git show 9c91c8cc^:docs/dev/compose-migration-plan-cn/02-第0步-构建准备.md`). 风险: 低.

## 计划 (当时的拆分)

1 文件 3 行量级的小步: 盘点既有 Compose 依赖, 补齐缺失项, 为后续每步提供编译基础.

## 实际落地

Compose 构建接线实际早于计划文档数月, 分两段:

- **2026-04-20 (8cd79baa)**: Compose 依赖进版本目录 (`gradle/toml/android.toml` + `kotlin.toml`), `build-logic` 新增  
  `AndroidApplicationComposeConventionPlugin` 与 `configureCompose()` helper, 此后模块只 apply 插件,  
  与现行 build-logic 约定同构.
- **2026-07-28 (2430be11)**: `ui/theme/` 的 Color / Theme / Type 三件 (501 行) 先行入库:  
  计划盘点时主题已 "就绪" (计划 01 章 1.4 原话),  
  含高中对比度方案 (这套多 scheme 后来由 [issues-cn #31](../../../dev/issues-cn/06-性能/31-主题死配色.md) 记为 6 套只用 2 套的死配色).
- **2026-09-19 当日增量**:  
  62c36a5c (minSdk 23 → 24, Navigation 3 硬要求) + 16e8d99c  
  (`lifecycle-runtime-compose`, 提供 `collectAsStateWithLifecycle()`).

## 落地差异

计划设想的 "第 0 步" 在计划写作时已大半完成: 依赖与主题数月前已备, 当日实际增量只有 minSdk 与 lifecycle-runtime-compose 两笔.  
后续 (2026-09-30 起) issues-cn #53 记录 `configureCompose()` 存在死分支 (`when` 判 `Project` 永不成立), 属这批早期接线代码的后续发现.

## 证据

- 提交: 8cd79baa, 2430be11, 62c36a5c, 16e8d99c; 后续发现: [issues-cn #53](../../../dev/issues-cn/09-构建与CI/53-Compose开关死代码.md).
