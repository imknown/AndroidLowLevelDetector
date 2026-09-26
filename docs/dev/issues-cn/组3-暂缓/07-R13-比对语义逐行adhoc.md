<a id="R13"></a>

# R13 FR-7 比对语义事实上存在 — 逐行, ad hoc

> 返回 [README 索引](../README.md) · [组3 · 暂缓](../README.md#组3--暂缓-原因见危害列标注).


**严重程度: P1(🟡) | 修复: 特征测试锁定; 注册表落地时迁入注册表条目 (决策⑦)**

规格前提是 "比对语义待定 (Q2)", 实际约 12 个 Home 行已经对参考 json 做了比对, 但各写各的, 整体没法验证, 没有测试 (复验成立):

- 硬编码预览偏移 `250205 - 250101`(`HomeRepository.isDateHigherThanConfig()`);
- `"$versionName-01" >= latest` 之类的字符串 hack(`detectMainline() 里`);
- 安全补丁按年月字符串比较 (`detectSecurityPatch() 里`).

**处理 (决策⑦, 已批准)**: 写进文档并补特征测试 (把现在的行为原样用测试锁住); 重新设计自然发生在 5b 注册表做出来的时候.


