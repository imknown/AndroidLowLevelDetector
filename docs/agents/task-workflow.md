# Task Workflow Reference (docs/spec)

> **Status**: **living**, part of [AGENTS.md](../../AGENTS.md)'s Task workflow section.  
> Read this in full before you create a spec directory, before you start a subtask, and before you call a  
> task closed.  
> The invariants that stay in AGENTS.md are these: nothing is committed without the owner's explicit say-so,  
> no next step starts without one, and a new session resumes from the progress ledger.

The owner = the developer. AI handles research/analysis/coding/testing/review;  
the owner owns goals/boundaries/judgment/commit. Every gate ends in an explicit owner nod.

## Directory and file layout

- Create `docs/spec/<yyyy-MM-dd-HH-mm-ss-Z>-<english-title>[-cn]/` (host-clock timestamp, short English title).
- The trailing `-cn` marks non-English reports; files inside a suffixed directory keep their prescribed names  
  WITHOUT a language suffix.
- The files: `plan.md` (task level), `subtask-01..NN` (one modification plan report per subtask),  
  `progress.md` (the ledger).

## Owner pre-write (before AI starts)

The owner hand-writes 2-3 sentences at the top of `plan.md`: the problem, their own approach, the biggest risk  
they predict. That block stays unchanged until the task ends and is compared against at the close.

(Guards against anchoring (Tversky & Kahneman) and hindsight bias: pre-register your own judgment before  
seeing the AI's plan.)

## AI plan (`plan.md`, task level)

Write the subtask split: each subtask's scope, why it is its own unit (minimal, high-cohesion,  
independently compilable changes), its risk level (high = security/data/core logic/unfamiliar areas;  
low = mechanical/boilerplate), and unfamiliar-area tags; a to-learn list at the end.

The report language is the owner's call per task and applies to the plan and every subtask report.

(Guards against cognitive load (Sweller): small chunks cut extraneous load; risk tiering prevents gate  
fatigue.)

## Plan gate

Before starting subtasks:

- A fresh-context subagent reviews `plan.md` in the background; report the findings and wait for the owner to  
  confirm them.
- Once the owner confirms, commit `plan.md` automatically.
- Generate ALL per-subtask modification plan reports (`subtask-01..NN`) in one pass, and have a fresh-context  
  subagent review that batch too, same loop as the code reviews (v1, v2, ...; findings wait for the owner's  
  confirmation; only confirmed items get fixed).
- Only then enter the per-subtask loop.

## Progress ledger

The spec dir carries `progress.md`; the AI updates it at every gate: current position, per-subtask gate state,  
suspended items (stashes), deviations, next action.

- Any new session/agent/model resumes by reading AGENTS.md + the ledger first, then executing the ledger's next  
  action.
- In-flight steps (for example a running background review) are not captured, so re-run the step the ledger  
  points at.
- The ledger rides the next commit.
- The resume protocol (scan `docs/spec/*/progress.md` on "continue", confirm the resume with the owner, list  
  and ask when several tasks are in flight) lives in  
  [AGENTS.md](../../AGENTS.md#task-workflow-docsspec): it binds every session, so it is stated there once.

(Guards against self-review blind spots: an independent context removes in-context anchoring.)

## Per-subtask loop, strictly in order

The numbers below are the workflow's own step names; AGENTS.md refers to "Task workflow, step 1", so the  
numbering stays 0..5.

0. **Start-of-subtask comparison** (default path: under AI-driven development the owner's unfamiliarity is  
   structural, not staged; no tiering by domain). The owner gives two things: a goal sentence for the subtask,  
   and a question list (may be empty). For a replacement-type task the owner adds a coverage check: audit the  
   report's changed / deliberately-not-changed list against the overall goal for gaps.  
   The decomposition and ordering are AI's responsibility: the owner does not pre-generate structure.  
   What the report owes the owner: a 5-10 line concept primer for the subtask, an answer to every question on  
   the list, and the coverage checklist.  
   Amend a stale report visibly before start; begin only when the owner says start; the gate tempo (how often  
   a nod is asked for) is the owner's call.  
   Hands-on implementation is not part of the flow: the owner arranges learning outside it.  
   (Guards against the ironies of automation (Bainbridge 1983): under AI-driven development the owner  
   structurally cannot keep up with implementation, so learning lives in the audit (question list / coverage  
   check / concept primers) with the learning scope narrowed to architecture concepts and review judgment, not  
   line-level implementation.)
1. **Implement** (code comments written in the owner's chat language, explaining *why*, at the codebase's  
   density; at the commit gate all new comments are translated to English, i.e. the committed codebase stays  
   English; comment punctuation follows the ASCII rule in  
   [Documentation Formatting Reference](doc-formatting.md)) and verify with  
   [the Android Studio checks](verification.md) plus the build.  
   For high-risk logic in unfamiliar areas, the owner reproduces it without AI first, then compares.  
   If the AI fails on the same problem twice, stop: the owner takes over or re-splits.  
   (Guards against deskilling, the ironies of automation (Bainbridge 1983): procedural memory needs practice;  
   and against endless retries.)
2. **Review**: a fresh-context subagent reviews the uncommitted diff in the background: v1, then v2, v3, ...  
   after each fix round. Later rounds may resume the same reviewer for a delta pass (it re-reads only what  
   changed since its own previous round, because its evidence base is already checked, so it is faster and  
   cheaper), but go back to a fresh reviewer when fixes rewrite large parts of the diff, when the owner  
   rejected most findings, when the reused context grows bloated, or for a final independent acceptance pass.  
   The reviewer duties stated in [AGENTS.md](../../AGENTS.md#build-and-verify) apply here: verifiable evidence  
   (file:line) per finding, and the preference for a reviewer model different from the one that wrote the  
   change.  
   For low-risk subtasks the owner may read the diff personally instead.  
   (Guards against automation bias (Parasuraman & Riley) and correlated same-model blind spots: the evidence  
   requirement turns recognition into verification; resuming a reviewer adds a self-confirming tendency,  
   countered by the evidence requirement and the fresh-reviewer triggers.)
3. **On findings**: report them; the owner re-reviews and confirms what really needs fixing; fix only what that  
   round requires. If anything was fixed, loop back to step 2 with the next version; if the owner confirms  
   nothing needs fixing, step 4 applies.
4. **No new substantive findings**: the owner leaves one sentence of their own words when approving (why this  
   can pass). Low-risk subtasks may merge steps 4 and 5 into a single nod.  
   (The testing effect (Roediger & Karpicke 2006) upgrades recognition into retrieval; counters the fluency  
   illusion and the illusion of explanatory depth (Rozenblit & Keil 2002); gate fatigue.)
5. **Commit** the subtask (Conventional Commits plus the trailer), then wait for another nod before starting the  
   next subtask.

## Convergence guard

Review rounds chase substance: subjective style preferences and premature-optimization suggestions do not force  
another round. If rounds keep churning without new findings, surface that to the owner instead of looping  
forever.

## Deviation ledger

Skipping a step is allowed, but log one line (task / which step / why) into the spec dir's progress ledger.

(Behavioral economics: allowed-but-logged beats forbidden; prevents both silent process decay and wholesale  
abandonment of the workflow.)

## Close-out

Clear the to-learn list; hold one cross-cutting retrospective (which classes of problems the AI gets wrong  
repeatedly, distilled into a review checklist).

(Spacing effect + metacognitive calibration, the long-term counter to the fluency illusion.)

## Archiving

When a task is fully closed out (the ledger's next action is none), its whole spec dir moves under  
`docs/spec/archived/` unchanged and is never edited again.

- The move itself declares the dir a **record** (see the status model in  
  [AGENTS.md](../../AGENTS.md#documentation-rules)); a ledger header still saying living is superseded by the  
  location, not a drift to fix in place.
- The resume scan (`docs/spec/*/progress.md`) does not reach one level deeper, so archived tasks never  
  resurface as unfinished.
