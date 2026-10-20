# Documentation Formatting Reference

> **Status**: **living**, part of [AGENTS.md](../../AGENTS.md)'s Documentation rules section.  
> Read this before you write or rework anything governed by the rules collected here (a doc, a code comment,  
> a commit message, or a chat reply to the owner), and whenever you convert or reflow text that already exists.  
> AGENTS.md states the prohibitions; this file holds the wording lists, the rename table, the CJK-to-ASCII  
> mapping, the spacing cases, the exemptions, the list and sentence rules, and the language and file-naming  
> mechanics.

## Plain wording

What not to write, and what to write instead. It governs a doc, a code comment, a commit message, and a chat reply.  
It holds in every language you may write in: the lists below are the wording this repo wrote and the wording the  
owner called out, and the test carries over to any other language rather than the list being the boundary.

Three kinds of wording are out:

1. **Filler and hedging**: wording that carries no fact. Delete the phrase and check that the sentence still says  
   what it said: `It is worth noting that`, `In summary`, `As we can see`, `Obviously`, `Needless to say`,  
   `crucial`, `paramount`, `Let us begin`, `In today's fast-moving world`.  
   In Chinese: `值得注意的是`, `综上所述`, `不难看出`, `众所周知`, `毋庸置疑`, `至关重要`, `重中之重`,  
   `让我们开始吧`, `在当今快速发展的时代`.
1. **Marketing adjectives**: a claim nobody can check. Name the fact instead, or drop it: `seamless` says  
   "no manual step is needed", `powerful` says what the feature does, `robust` says which failures it handles.  
   Also out: `ultimate`, `disruptive`, `revolutionary`, `comprehensive`, `world-class`, `cutting-edge`,  
   `ecosystem`, `赋能`, `抓手`, `无缝`, `强大`, `极致`, `颠覆`, `革命性`, `全方位`, `高水平`, `高质量`, `生态`.
1. **A cross-domain metaphor used as the name of a work item, a check, or a decision.**  
   The list is not what makes this work; the test is whether a reader who has not opened this repo's docs can  
   follow the sentence. Three groups, because their evidence differs:
   - written into this repo's own docs at some point: `bucket`, `gate`, `ledger`, `close-out`, `land`/`landing`,  
     `tail`, `drift`, `debt`, `seam`, `in-flight`, `tempo`, and in Chinese `桶`, `闸门`, `台账`/`账本`, `收口`,  
     `收尾`, `落地`, `尾巴`, `漂移`, `债`, `接缝`, `护栏`, `兜底`, `兜住`, `裁定`, `定夺`, `拍板`, `点头`,  
     `落点`, `门禁`, `地基`, `伸手点`, `保护网`, `沉淀`, `复盘`, `闭环`, `对齐` (for agreement), `粒度`.
   - named in the owner's complaint about AI replies and never found in a file of this repo: `顶牛`, `揪出`,  
     `放宽`, `颗粒度`.
   - English siblings of the same failure, named by this rule before anyone here wrote one: `guardrail`,  
     `harness`, `circle back`, `touch base`.

A word that names something the code already names stays: `Alignment` in a layout, a Gradle build artifact,  
the Metro artifacts in `gradle/toml/thirdParty.toml`, the placeholder fallback in `PropertyReader`.  
The ban is on a second, invented name for a thing the code already has a name for.

## Terms this repo renamed

Use the right-hand column in a living doc. The old wording still stands in archived records  
(`docs/spec/archived/`, `docs/dev/issues-cn/archived/`), which are never rewritten in place, and in the entry  
titles and the section titles a living doc quotes from one of those records: cite a record by the name it was  
archived with, and a banned word inside such a name stays because it is that record's name, not today's wording.  
The middle column is the meaning, not a mandated synonym: when a plain sentence carries the meaning, write that.

| Renamed away from | Now in force | What it means |
|---|---|---|
| gate (plan gate, commit gate), 闸门 | approval point, 确认点 | a stop where the AI asks and the owner answers |
| ledger, deviation ledger, 台账, 账本, 偏差台账 | progress record, deviation record, 进度记录, 偏差记录 | `progress.md`, and one line logged per skipped step |
| convergence guard, 收敛护栏 | when the review loop stops, review 循环何时停 | stop looping review rounds once they bring no new finding |
| close-out, 收尾 (as the stage name) | finishing the task, 任务结束 | clear the to-learn list and hold the one cross-cutting retrospective |
| adjudicate, rule (verb: the owner ruled), 裁定, 定夺 | decide which side is wrong, 判定哪边错 | the owner decides, not the AI |
| nod, say-so, 点头 | the owner's approval, 负责人的明确同意 | the owner says yes in words |
| tail, 尾巴 | leftover part, 没做完的部分 | work of this unit that is not yet done |
| land, 落地 | record, implement, 记录, 实现, 完成, 实施 | write it down, or build it |
| fallback (as a doc term), 兜底 | what to do when it fails, 失败时怎么做 | keep the code's own name when the code has one |
| drift, 漂移 | out of date, inconsistent, 过期, 不一致 | a sentence the code no longer matches |
| debt, 债 | existing problem, 已有的问题 | work the code already owes |
| seam, 接缝 | natural boundary, 天然的边界 | where a long note gets split |
| 落点 | 归到哪一条, 记在哪一条 | which entry or step carries a decision or a fix |
| 门禁 | CI 上必须通过的检查 | the CI check that blocks a merge |
| 地基, 保护网 | 基础, 防回归的测试与检查 | what must already work before the main line starts |
| 伸手点 | 全局取用点 | a value any call site can reach without it being passed in |
| 沉淀 (as a verb) | 写成文档, 汇总成文档 | write knowledge down so it survives |
| in-flight, tempo | still running, unfinished, how often approval is asked for | a step still executing, and the rate of asking |
| 对齐 (for agreement) | 一致 | two descriptions saying the same thing |
| 粒度 | 精确到 | how fine a value or a split is |

## ASCII punctuation in every language

A Chinese (or Japanese) doc uses English punctuation marks, never CJK ones: `,` `.` `;` `:` `!` `?` `(...)`  
`"..."` instead of `，。；：！（）「」《》`.

The one-to-one map:

| CJK | ASCII |
|---|---|
| `，` `、` | `,` |
| `。` | `.` |
| `；` | `;` |
| `：` | `:` |
| `（）` | `()` |
| `「」` `『』` `《》` | `"` (or `'` nested) |
| `……` `…` | `...` |

Non-punctuation glyphs stay as they are: `·` `→` `←`, box-drawing in diagrams, and status emoji.  
Dashes are not mapped; they are banned, see the next section.

Spacing follows English, whatever the script on either side: one space after a mark that is followed by more  
text (`每个条目, 每次检测`), no space before `,` `.;:!?)` and none inside `(` `"`. Two extra cases:

- A `(` that follows an identifier is a call, so no space there (`collectModels()`).
- `:` between digits stays tight (`16:9`, `12:30`).

This holds for every doc, including the **frozen**, **record**, and **scratch** ones: punctuation there is  
formatting, not the substance those statuses preserve. When you write new text, or rework a paragraph, convert  
what the edit touches along with it, and check the punctuation when translating between languages.

A heading's punctuation and spacing decide the GitHub anchor its table of contents points at, so re-spacing a  
heading means rewriting every `](#...)` that resolves to it, in the same edit. Explicit `<a id="...">` anchors  
are stable text and never move.

## Dashes

No dash is ever used as punctuation in any doc, code comment, or chat reply: no em dash `—`, no CJK dash `——`,  
no ASCII stand-in `--`, and no `----` decorative rule opening or closing a comment.

Rewrite each occurrence as equivalent words chosen by meaning:

| Relation | English | Chinese |
|---|---|---|
| A gloss | `, i.e. ` | `, 即 ` |
| A rephrase | `, that is, ` | `, 也就是 ` |
| A reason, an explanation, an enumeration | `:` | `:` |
| Two independent clauses | `;` | `;` |
| A true aside | `(...)` | `(...)` |
| A plain continuation | `,` | `,` |

When the enumeration becomes a list, the colon stays and the list follows it.

Naming the symbol inside inline code, as this rule does, is the only exception to that ban.

A `--` that is not punctuation carries structure and stays as it is:

- The doubled hyphen in a GitHub anchor (`../README.md#1--架构与分层`).
- A markdown table separator (`|---|`).
- An XML comment delimiter (`<!-- -->`).
- A command-line flag (`--project=...`).
- A postfix decrement (`i--`).
- The `git ... -- ` end-of-options marker.

## When items go in a list

Two or more parallel items go in a list, one item per line, when the text labels them (`(a)`, `1)`)  
or when each item is a clause.  
A short series of names, values, or short phrases inside one sentence is not an enumeration  
and stays inline (`off` / `low` / `high`, `read and write`). A table cell keeps its series inline.  
An example quoted in inline code does not trigger this rule.  
A long item is wrapped by the hard-wrapping rules below like any other line,  
but two items never share one line.

When a paragraph is reworked, its inline enumerations are converted too. An untouched paragraph stays as it is.

### Numbered lists

A numbered list writes the real start number on its first item and `1.` on every item after it:  
a 1-based list is `1.`, `1.`, `1.`. A 0-based list starts with `0.`.  
The renderer numbers them, so an inserted item never shifts the rest.  
Number the items when their order matters. Use bullets otherwise.

## One idea per sentence

One sentence carries one idea, so split a sentence whose last fact needs a second conjunction to reach it.  
`and` fills that role in English, "和" or "而且" in Chinese.  
A line's width follows its sentence; see the hard-wrapping rules below.  
It applies to the same text as the list rule: text being written or reworked.

## Hard-wrapping long lines by meaning

When a source line runs long, break it at a sentence or clause boundary  
(a comma-ended clause is a valid break point, but a very short clause may stay on one line with what follows;  
keep a short series of names together). The point is that no line ends up too long overall, not a fixed width.

Never break these; each stays on one source line:

- A heading, a link target, a URL, a code block, inline code.
- A table row never splits into two source rows, but a long cell may wrap with `<br>` inside the cell  
  (a raw newline would break the table).

Every wrapped line inside a paragraph, list item, or blockquote ends with two trailing spaces (or `<br>`) so  
the break also survives rendering; the block's last line carries none. Those spaces are load-bearing: never  
strip or collapse them.

Code comments follow the same rule; a comment sharing its line with code stays one line, and a wrapping pass  
must never touch anything that is not a comment.

Wrapping is formatting, so it holds for **frozen**, **record**, and **scratch** docs too.

## Language and file naming

English is the default language for documents, and file names are English by default. A non-English document  
marks its language on the directory when a whole directory is written in it, and the files inside then keep  
their plain names, as `docs/dev/issues-cn/` and `docs/agents-cn/` do. The marker goes on the file itself only  
where the document sits in a mixed directory or at the repository root, as `docs/dev/module-structure-cn.md`,  
`AGENTS-cn.md` and `README-cn.md` do. A translated root `README` stays a root file: GitHub renders only the  
repository root's `README.md`, so it cannot move into a language directory. Where a directory already has a  
naming pattern, follow it.

The mirrored set is the root `AGENTS.md` and `README.md` plus every English original in `docs/agents/`.  
The copies standing in the repo today are the Chinese ones: `AGENTS-cn.md`, `README-cn.md`, and  
`docs/agents-cn/`, which holds the plain file names. A doc in that set is mirrored into every language the  
repo holds a translation in, and the twins of a doc hold the same statements in the same order. Nothing else  
is mirrored, and a doc that stands in one language is not half of a pair: `docs/dev/JOTTINGS.md` is English  
only, and `docs/dev/module-structure-cn.md` and `docs/dev/issues-cn/` are Chinese originals with no  
counterpart. A doc outside the set owes no twin in any language.

Where a doc is mirrored, its English file is the one in force and keeps the unsuffixed, canonical name.  
A translation carries its language's marker, does not label itself a translation, and holds no link pointing  
at another language's copy of itself: the marker states the relationship.

An edit to any one file of a mirrored doc is incomplete until every other file of that doc carries the same  
change, so revise the whole set in one pass: a rule in force reads the same in every language. When two  
copies disagree, the English file decides and the translations are the side that gets fixed. Adopting a new  
language adds a copy of every file in the set: a copy of a file that lives in `docs/agents/` goes into a  
directory marked with that language, while a copy of a root file stays a root file carrying the file marker;  
from then on all of them join that same sync duty.

Links follow the language of the document carrying them: any document, mirrored or not, points at the copy  
in its own language where one exists and at the English file where none does; an English document points at  
the English file. The duty covers a citation written as prose, not only a link: a Chinese document cites  
`AGENTS-cn.md`, and it names the default English file only where its own language has no copy of it. One  
sentence keeps the English name on purpose: a statement about which side governs points at the file in force.

Archived material is exempt from this duty: a record keeps the links and the citations it was archived with,  
so a Chinese record still pointing at an English file is the correct state, not an inconsistency to fix in place.

Any language may still be used inside a doc when there is a reason.
