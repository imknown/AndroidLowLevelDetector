# Android Studio Analysis Reference

> **Status**: **living**, part of [AGENTS.md](../../AGENTS.md)'s Build and verify section.  
> Read this before you rely on an `android studio analyze-file` result.  
> The invariant that stays in AGENTS.md is the duty itself: run it for every changed file after each edit batch.

`android studio analyze-file` drives a *running* Android Studio instance and reports its live inspection  
results for one file at a time: compiler errors, warnings, and Android Lint inspections.

## Usage

```bash
android studio analyze-file --project=AndroidLowLevelDetector <path>
```

- The path is relative to the current directory or absolute; the command takes one file per run.
- Add `--pid=<pid>` when more than one Android Studio instance is running.
- Anything Android Studio can analyze is in scope, including what its installed plugins inspect.

## Prerequisites

- The command carries no analyzer of its own: it queries a running Android Studio, so the project has to be  
  open there first.
- `android studio check` listing the project as `READY` is what makes a result worth reading.  
  When no instance has the project open, the run fails, so ask the owner to open Android Studio and  
  this project.

## Reading the result

- The exit code is not a verdict: a run that prints `ERROR` items still exits 0, and exit 1 only says the  
  call failed (no instance has the project open, or the file does not exist).
- Read the printed `ERROR` / `WARNING` / `INFO` blocks, each of which carries a line and a column.
- Inspection-level findings (for example "Use destructuring declaration") are treated like syntax warnings:  
  fixed directly, not documented.

## Why a result can be incomplete

- **The editor truncates the list.** Analyze a file only while it is closed in the editor.  
  Android Studio filters inspections by its Severity and "Highlighting in editor" settings, and analyze-file  
  inherits that filter, so a file sitting in an editor tab comes back truncated (the Problems view is just as  
  partial even with every displayable severity ticked). A closed file is the only way to get the complete list.
- **An unindexed file prints a false `No issues found!`** (still exit 0). A file created in this session stays  
  invisible to Android Studio until it indexes the file, so re-run the check after indexing has caught up before  
  treating the silence as a pass. A path outside the `--project` named is invisible the same way.
- **A file Android Studio already knows is analyzed right away** once you edit it, so no waiting is needed  
  there.
