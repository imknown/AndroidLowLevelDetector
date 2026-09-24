# Documentation

Developer docs for AndroidLowLevelDetector. Product introduction and download links are in the [root README](../README.md).

## Contents

| Path | What it is |
|---|---|
| [spec-cn/](spec-cn/README.md) | Product spec (Chinese). Deliberately written without reading the code — when spec and code disagree, verify which one is right, then fix the wrong one |
| [dev/](dev/README.md) | Developer docs — engineering conventions, architecture review, quick wins, the Compose migration plan, notes. Its index says which of them are living and which are frozen records |

Each directory's README describes its own contents, background, and caveats — this file is only an index; when adding a new doc directory, add a row to this table.

## Reading order

Start at [spec-cn/](spec-cn/README.md) — what the app should be. From there the developer docs are ordered in their own index, [dev/](dev/README.md).

## Conventions

- Every requirement and finding has an ID (`FR-x`/`AC` for requirements, `ADR-x` for architecture decisions, `AR-xx` and `C`/`R`/`A` for review findings, `Q-x` for open questions). IDs are shared across all docs — reference them by ID.
- Fix docs **in place**: when a statement turns out to be wrong, rewrite it and everything downstream that leaned on it, so the doc reads as current truth on its own. Do not leave the superseded sentence standing with a parenthetical or blockquote correction appended to it, and do not label a corrected sentence with the review that produced it — the fact belongs in the sentence, not in a note about who checked it and when.
- Scope of the rule above: docs marked **living** in [dev/README.md](dev/README.md#developer-docs) — the spec, the conventions, anything that describes the present. Docs declared **frozen** or **record** there (the completed migration plan, the View-era review and its quick-win list) keep the text they were written with, corrections and later findings included: cite them for history, but do not back-fill today's code into them. Nothing is frozen by default; a new doc is living until its README says otherwise.
- A doc states only what the owner said or the code shows. No term, detection item, or qualifier is added to make a row or a sentence look complete; a glossary row is scoped to vocabulary the spec itself uses, and a row whose last reference is gone is asked about rather than kept because the term is generally known.
- What stays in prose: substance and navigation — the scope or exception the sentence is drawing, cross-references by ID (`FR-x`, `ADR-x`, `Q9`), code symbols. **Who ruled what, and when, is not prose**: dated rulings belong in the registry rows of [spec-cn chapter 03](spec-cn/03-nfr-release-and-open-questions.md) (roadmap list and open-questions table) or in `git log`, not as an "owner ruled, <date>" tag on every sentence. Likewise a version row names the version and points at `git log`; it does not accumulate a revision log.
- English is the default language for documents; non-English directories are marked with a language suffix (Chinese: `-cn`). File names stay English even inside a `-cn` directory. Any language may still be used inside a doc when there is a reason.
- No document (memory docs included) may contain sensitive information: privacy data, passwords, keys, certificates.
- Keep docs short; when one grows too long, split it along natural seams, keeping a README as the index page.
