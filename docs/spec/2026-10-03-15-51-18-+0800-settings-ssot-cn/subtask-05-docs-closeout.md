# ST-05 — docs-closeout 修改计划

> 状态: living (动工时如有修订在此显式记录). 计划依据: 本目录 plan.md + issues-cn #08 / #10. 风险: 低 (纯文档).

## 目标句 (占位, 动工对照时由负责人确认或改写)

文档与代码真值同步: issues-cn 两条目关闭, AGENTS 双语为 SettingsStore 补一句, 残留头注释归真, 枚举去留落一句裁定.

## 变更清单

1. issues-cn: 删 `2-SSOT-唯一数据来源/08-设置无唯一数据来源.md` 与 `2-SSOT-唯一数据来源/10-设置页直读SharedPreferences.md` (已修条目直接删除, 不留 "已解决" 行 — tracker 规则); `README.md` 同步 #08 / #10 的引用点: 行 19 (目录), 行 61 / 63 (总览表 2 节两行), 行 158 (总体诊断), 行 184 (修复路线第 1 步); README 之外还有三份文件引用两条目, 删除即悬空或陈述变假, 同批归真:
   - `1-架构与分层/02-服务定位器上帝对象.md`: `:23` (链#10) / `:34` (链#08) / `:49` (链#08+#10) 三处 markdown 链接随条目删除改写; `:36` / `:50` 两处 bare 引用与未来时陈述 ("归 [#10] 与 [#08]" / "是 [#08] 的过渡形态") 改写为已落地现状; 伴生对象代码样例 `:25-28` (画出 `themeMode` / `setMyTheme` / `scrollBarMode` / `setMyScrollBar` 四个已删成员) 随本批迁移一并改写, 免得以 "现状" 姿态展示不存在的代码并与 `:50` 的已迁走表述自相矛盾.
   - `6-性能/32-启动性能杂项.md:11`: 链#08 与 "`initTheme()` / `initScrollBar()` 直读" 的修法建议随本任务落地而过时, 相应句改写.
   - `3-UDF-单向数据流/11-HomeViewModel手工编排.md:85`: "#08 改造后设置从注入的仓库读" 的注释性提及改为已落地口径.
2. AGENTS.md + AGENTS-cn.md: Architecture 节 **新增** 一句 (现无设置表述, 属新增不是修改): 设置项的可观察唯一归属是 `SettingsStore` (键集中, 写入口, 流), 绑定在图上; 设置数据零 SP 直读. 双语同步.
3. `base/MyApplication.kt` / `di/AppGraph.kt` 头注释归真: `SharedPreferencesContainer` 的 "deliberately out of scope (SettingsScreen / HomeRepository, issues-cn #10/#08)" 段 (`AppGraph.kt:169-176`) 改写为已收编现状; `MyApplication` 类头注释相应段落.
4. `AppThemeMode` / `ScrollBarMode` 枚举去留一句话裁定: **倾向原地保留** (`MyApplication.kt` 顶层, 零改动, 消费方 import 不动); 若负责人要迁 (如 `ui/theme` / `ui/base`), 机械搬移 + import 同批, 单独小提交.
5. module-structure-cn.md 待决问题 4 对应行 ("companion 的主题/滚动条状态流随 #08 迁走") 改写为已落地. 前置: 该文件未入库 — 入库与否需负责人先点头; 不点头则此行顺延到模块化任务, 本子任务不动它.
6. ST-02..04 已各自归真相邻注释, 本子任务只做终检 (见验证).

## 验证

- 零残留 grep 终检: 六个已删符号 (`themeMode` / `scrollBarMode` / `setMyTheme` / `setMyScrollBar` / `initTheme` / `initScrollBar` 相对 `MyApplication`) + `MyApplication.sharedPreferences` (业务代码) + 注释层旧归属语句 ("MyApplication owns it" / "as themeMode" / "initTheme 迁移" / "Storage untouched" / "read SP once" / "only reads preferences" 形态) 零现在时命中.
- issues-cn 相对链接零悬空 (全目录, 不限 README): #08 / #10 的全部引用点 — README 四处 + `02-服务定位器上帝对象.md` 五处 + `32-启动性能杂项.md` / `11-HomeViewModel手工编排.md` 各一处 — 改写后全清.
- `./gradlew assembleFossDebug` 绿 (纯文档按惯例跑一次收尾冒烟).

## 前置

- module-structure-cn.md 入库裁定 (负责人点头与否决定第 5 项做不做).
