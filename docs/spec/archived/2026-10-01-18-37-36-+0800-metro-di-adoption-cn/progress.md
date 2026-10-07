# 进度账本: Metro DI 接入

> 状态: living. 每过一道闸门由 AI 更新; 任何新会话/新 agent/新模型 接续时先读本文件, 再按"下一步"继续. 进行中的步骤 (如后台 review) 不落账, 按账本重跑.

## 当前位置

**Metro DI 接入任务全部完成 (11/11 子任务已提交).** 组合根 = `di/AppGraph.kt` 的 Metro 图;  
四条 ViewModel 链 + 五个绑定 (HttpClient / IShell / IProperty / LldFileStore / SharedPreferences) 全部注入; 静态单例清空;  
文档 (AGENTS 双语 / issues-cn / AGENTS.md 工作流) 已回到现状真值. ST-11 收尾内容: 6 处 #05 悬空注释改写, AGENTS 升级纪律 + @Inject 工作流说明,  
#02 改写 (两段落地 + 剩余收口, P0→P1), #16 删 HttpClient 半并改名,  
README/01/04/07/19/24/27/30/32/42/45 的陈旧现场按现状改写 (getMyString 计数 100/91),  
plan.md 待学清单清空 (retrospective 经负责人审阅后按裁定未入库). 验证: 双构建绿 (含 release/R8 冒烟), #05 与旧 AGENTS 表述零残留,  
issues-cn 链接零悬空, 已删符号零现在时引用. 后续工作归 issues-cn 修复路线与模块化任务 (module-structure-cn.md 待裁定), 均与 Metro 本体无欠账.

## 子任务状态

| 子任务 | 状态 | 闸门进度 |
|---|---|---|
| ST-01 | 已提交 (`feat(di)` 94ff1e6c) | 全部 ✓ |
| ST-02 | 已提交 (`refactor(home)` c0f0b7d9) | 全部 ✓ (字节码取证评审) |
| ST-03 | 已提交 (`refactor(others)` b359ac46) | 全部 ✓ (低风险合并点头) |
| ST-04 | 已提交 (`refactor(prop)` 09933507) | 全部 ✓ (低风险合并点头) |
| ST-05 | 已提交 (`refactor(settings)` 7db453d2) | 全部 ✓ (第一段完成; 真机 smoke: 双冷启动 / 四 tab 全渲染 / 刷新不闪空 / logcat 零异常) |
| ST-06 | 已提交 (`refactor(di)`) | 目标句 ✓ 实施 ✓ 构建 ✓ 联网 smoke (Gitee 302→200 两轮) ✓ process death 模拟 (am kill 后重启恢复) ✓ v1 review (1 建议已修: 头注释) → 方案 B 裁定 (Provider 惰性单例) + 实施 + 真机复验 ✓ v2 review (限定范围深查: 头注释与 Provider 字节码级核实无误; 1 建议: 报告随裁定修订, 已改) ✓ 放行 ("继续", 见偏差记录) ✓ 提交 ✓ 注释已译回英语 ✓ |
| ST-07 | 已提交 (`refactor(di)`) | 目标句 ✓ 实施 ✓ 构建 ✓ 零残留 ✓ 报告已修订 (DataSource 图构造后无需经 Repository 传参) ✓ 真机 smoke (Others 的 kernel 行吃到真实数据 = IShell 链路通; Home 整页加载无错误行; 零 FATAL; SELinux/GSI 行未滚到, 语义由评审覆盖) ✓ v1 review (1 建议: MyApplication 残留 ShellLibSu 冗余 import → 已删; 负责人确认不另开 v2) ✓ 放行 ✓ 提交 ✓ 注释已译回英语 + 构建重验 EXIT=0 + 零残留复核 ✓ |
| ST-08 | 已提交 (`refactor(di)`) | 报告预生成 + 批次 review ✓, 动工时修订 (factory 实施路径: binding container) ✓ step0 ("开始" 放行, 目标句代记 "按报告范围") ✓ 实施 ✓ 构建 ✓ 零残留 ✓ 行为等价抽查 ✓ v1 review ✓ (4 LOW, 无阻塞) findings 裁定 ✓ (① 改 ② 归 ST-11 ③ 备记: 显式暂存勿 add -A ④ 改) ①④ 已修 + AGENTS.md DI 条目改写 ✓ 放行 ✓ 提交 ✓ 注释译英 ✓ (CJK 复查净 + 构建重验 EXIT=0) |
| ST-09 | 已提交 (`refactor(di)`) | 报告预生成 + 批次 review ✓, 动工时修订 ×3 ✓ step0 ✓ 实施 ✓ 构建 ✓ 零残留 ✓ v1 review ✓ (4 findings 裁定全落) v2 review ✓ (2 LOW 台账措辞已修, 零代码发现) 运行时 smoke ✓ (清数据冷启复制 lld.json 落盘 + 零 FATAL/MissingBinding/StrictMode; 本地刷新链无多余写盘; 联网保存 mtime/内容变化 + Gitee 302→拉取) 放行 ✓ 提交 ✓ 注释译英 ✓ |
| ST-10 | 已提交 (`refactor(di)`) | 报告预生成 + 批次 review ✓, 动工时修订 ×1 ✓ step0 ✓ 实施 ✓ 构建 ✓ 残留符合预期 ✓ v1 review ✓ (重发后完成; 1 LOW informational) 行号裁定 ✓ (1106→1121 就地修正) 设备 smoke ✓ (双向: ON→Ktor 在线链; OFF→零请求离线链; 零 FATAL/MissingBinding) 放行 ✓ 提交 ✓ 注释译英 ✓ (CJK 复查净 + 构建重验 EXIT=0) |
| ST-11 | 进行中 | 报告预生成 + 批次 review ✓, 动工时修订 ×2 ✓ step0 ✓ 实施 ✓ 双构建 ✓ (#05 零残留 / AGENTS 零残留 / 链接零悬空) v1 review ✓ (评审事故已恢复+AI 独立验证; 5 findings) findings 裁定 ✓ (五条全修 + 收尾清扫追补 2 处: README 做对清单的 ShellManager 现在时引用 / 07 片段的旧 getStringProperty 形态; retrospective.md 按裁定删除未入库) → 放行 ⬜ → 提交 ⬜ |

### ST-06 smoke 残留项

- GitHub 路径未真机验证 (URL 由设备时区决定, 免 root 不可切; 与 Gitee 路径仅 URL 常量不同, 客户端配置同一份)
- Wi-Fi Manual→PAC 代理切换场景 (原 proxySelector 修复的小概率场景) 需人工在设备上切一次, 留给负责人随手验证

## 挂起物

- module-structure-cn.md (未跟踪草稿) 保持不入库, 其余内容仍待负责人逐项裁定;  
  2026-10-03 已记入 Metro 1.5.0 的 `checkMainMetroHiddenDependencies` CI 门禁建议 (模块拆分后采纳)
- HomeRepository.kt:346 全角 `｜` 分隔符 (既有代码, 与 Metro 接入无关), 负责人裁定暂不处理 (2026-10-02; v1 评审复核时行号漂移至 349, 非本次引入)
- 模块化结构改造 (2026-10-02 讨论存档): 负责人意向 = KMP 默认结构 / Kotlin Toolchain 方向; AI 建议草案 (目标结构 / 映射 / 现实检验) 与 5 个待决问题已存档至  
  [docs/dev/module-structure-cn.md](../../../dev/module-structure-cn.md) (living, **待负责人逐项裁定**, 不依赖本会话上下文);  
  ShellDefault 澄清同记其"关联裁定"节, ST-11 纳入 AGENTS.md; 顺序结论 = 先收完 ST-07~11 再动模块

## 偏差记录

- ST-04 / step0 / 负责人未单独给出目标句, 以一句"继续"放行, 按导读所述报告范围执行 (与 ST-02/03 同型), 台账代记目标句为"同前两条链".
- ST-04 / 放行+提交 / 负责人在评审结果汇报后以一句"继续"完成合并点头 (低风险子任务的简化形态), 未另写放行理由句.
- ST-05 / step0 / 负责人未单独给出目标句, 以一句"开始"放行, 按预告所述报告范围执行 (同型第四链 + #05 文档收尾), 台账代记目标句为"按报告范围".
- ST-05 / 放行+提交 / 负责人在 smoke 结果汇报后以一句"继续"完成合并点头, 未另写放行理由句.
- ST-06 / step0 / 负责人未单独给出目标句, 以一句"开始"放行, 按预告所述报告范围执行, 语义变化 (单例不再每请求关闭 / debug Logging 保留) 视为随"开始"确认; 台账代记目标句为"按报告范围".
- ST-06 / v2 后 / 报告修订未另开 v3 轮, 裁定波及的报告过时项非代码 (v2 对代码深查干净), 按 living 报告规则显式修订并记录.
- ST-06 / 放行 / 负责人在 v2 结果汇报后以一句"继续"放行 (高风险子任务未留理由句, 沿用本会话既立模式).
- ST-07 / step0 / 负责人未单独给出目标句, 以一句"开始"放行, 按预告所述报告范围执行; 台账代记目标句为"按报告范围".
- 台账维护 / ST-05 提交起文件尾部残留 ST-04 时代的重复段落 (AI 组稿失误: 一次整体 Write 未覆盖旧尾部), 后续闸门更新只动前半未被及时发现, 且已随 ST-05 提交入库;  
  ST-06 提交时整体重写清理, 以本版为唯一有效台账.
- ST-07 / 放行 / 负责人在 v1 结果汇报后以一句"继续"放行 (沿用 ST-06 起的既立模式, 未另写理由句).
- ST-08 / step0 / 负责人未单独给出目标句, 以一句"开始"放行, 按报告范围执行; 台账代记目标句为"按报告范围".
- ST-08 / 放行 / 负责人在 v1 修复汇报后以一句"继续"放行, 合并 v2 闸门 (低风险, 修复均为单句注释/文档级), 沿用 ST-06 起的既立模式.
- ST-09 / step0 / 负责人以一句"继续"放行 (先看效果后说"继续"), 按报告范围执行; 台账代记目标句为"按报告范围".
- ST-09 / smoke+放行+提交 / 联网保存路径的 adb 取证命令第一次被取消; 负责人现场观察后以一句"提交"合并放行,  
  复跑取证 (mtime/内容变化 + Ktor Gitee 302→拉取) 后三条路径全闭环; 注释译英照常在提交闸门完成.
- ST-10 / v1 review / 后台 review subagent 因配额耗尽未跑完, AI 曾按低风险替代路径附 diff 请负责人亲自读; 次日额度恢复,  
  负责人裁定仍走 subagent 复审 (重发同一 v1 prompt).
- ST-10 / 放行+提交 / 负责人在 smoke 汇报后以一句"提交"合并放行 (沿用 ST-06 起的既立模式).
- ST-11 / v1 review / reviewer 误以 `git checkout <旧提交> -- .` 把历史版本写进工作区 (将 "查历史" 误当只读操作), 随后自行恢复;  
  AI 独立验证 (status/diff 统计/复活文件/抽查) 确认完好. 检查清单新增: review 查历史只用 `git show`, 禁止 checkout 进工作区.
- ST-11 / 放行+提交 / 负责人在 v1 修复汇报后以一句"提交"合并放行 (沿用 ST-06 起的既立模式); 提交闸门确认六处 Kotlin 注释本就英语, 纯文档改动无需重验构建 (双构建已覆盖全部代码改动).

## 下一步

(无, 任务已收尾. 后续: issues-cn 修复路线按其文档的触发条件推进; 模块化见 module-structure-cn.md 待裁定项; 可选: release 包真机冒烟.)
