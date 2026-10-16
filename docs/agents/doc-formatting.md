# Documentation Formatting Reference

> **Status**: **living**, part of [AGENTS.md](../../AGENTS.md)'s Documentation rules section.  
> Read this before you write or rework anything governed by the rules collected here (a doc, a code comment,  
> a commit message, or a chat reply to the owner), and whenever you convert or reflow text that already exists.  
> The prohibitions themselves stay in AGENTS.md; this file is the mapping, the spacing cases, the exemptions,  
> and the language and file-naming mechanics.

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

Naming the symbol inside inline code, as this rule does, is the only exception to that ban.

A `--` that is not punctuation carries structure and stays as it is:

- The doubled hyphen in a GitHub anchor (`../README.md#1--架构与分层`).
- A markdown table separator (`|---|`).
- An XML comment delimiter (`<!-- -->`).
- A command-line flag (`--project=...`).
- A postfix decrement (`i--`).
- The `git ... -- ` end-of-options marker.

## Hard-wrapping long lines by meaning

When a source line runs long, break it at a sentence or clause boundary  
(a comma-ended clause is a valid break point, but a very short clause may stay on one line with what follows;  
keep a short enumeration together). The point is that no line ends up too long overall, not a fixed width.

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
so a Chinese record still pointing at an English file is the correct state, not a drift to fix in place.

Any language may still be used inside a doc when there is a reason.
