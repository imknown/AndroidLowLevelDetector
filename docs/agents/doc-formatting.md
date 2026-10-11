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

English is the default language for documents, and file names are English by default; a non-English  
document carries a language suffix (Chinese: `-cn`), as `docs/dev/module-structure-cn.md` does.

Where a document has a translated twin, the English file is the one in force and keeps the canonical name;  
the translation carries the suffix, does not mention its twin, does not call itself a translation, and adds  
no back-reference: the suffix states the relationship. No translated twin stands in the repo now, so  
`AGENTS.md` and the root `README.md` are single-language files. Links point at the file that exists, and  
where a document has no counterpart in the reader's language, at the one that does. Where a directory  
already has a naming pattern, follow it. Any language may still be used inside a doc when there is a reason.
