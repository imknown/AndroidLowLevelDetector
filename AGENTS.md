# AGENTS.md

Guidance for coding agents working in this repository. Read this before making changes; if any doc disagrees with the code, trust the code and update the doc.

Working principles:

- **Memory**: this file is an index, kept short. Record durable findings (settled decisions, conventions, gotchas, doc corrections) in the docs under `docs/` — engineering conventions go in [docs/dev/conventions/](docs/dev/conventions/README.md), everything else in the relevant doc — so the next session starts from them instead of rediscovering. When unsure what or where to record, discuss with the user.
- **Never guess**: investigate the codebase and docs first; base every change on evidence you can point to (file, line, doc section). When something cannot be determined or multiple valid approaches exist, present findings and ask the user; write `unknown` rather than inventing a value.
- **English by default**: anything you generate — docs, comments, commit messages — is in English unless the user specifies otherwise.
- **No sensitive information** in any document, memory included: no privacy data, passwords, keys, certificates, or signing material.

## Project overview

Android app that surfaces low-level system characteristics: Treble and GSI compatibility, Mainline/APEX modules, system-as-root, A/B partitions, Binder bitness, security patch levels. Stack: Kotlin, Jetpack Compose (single Activity, Navigation 3), MVVM + StateFlow unidirectional data flow, kotlinx.serialization, Ktor, libsu, JNI/NDK. No DI framework.

- Application id `net.imknown.android.forefrontinfo`; version info in `gradle/toml/build.toml`.
- Modules: `:app` (Compose UI, features under `ui`) · `:base` (`IProperty`/`IShell` abstractions) · `:binderDetector` (C++ via JNI) · `build-logic` (convention plugins).

## Build and verify

```bash
./gradlew assembleFossDebug      # what CI builds (GitHub Actions)
./gradlew testFossDebugUnitTest  # unit tests (template-only today)
./gradlew lintFossDebug          # Android lint
```

Flavors, signing, toolchains, version catalogs, and all build conventions: see [docs/dev/conventions/](docs/dev/conventions/README.md).

## Where to look

| Topic | Doc |
|---|---|
| Build / architecture / code rules / localization / workflow details | [docs/dev/conventions/](docs/dev/conventions/README.md) |
| Docs index & reading order | [docs/README.md](docs/README.md) |
| Product spec — deliberately written without reading the code; when code and spec disagree, verify which is right, then fix the wrong one | [docs/spec-cn/](docs/spec-cn/README.md) |
| View→Compose migration plan behind this branch — read its "key decisions" before touching UI | [docs/dev/compose-migration-plan-cn/](docs/dev/compose-migration-plan-cn/README.md) |
| View-era architecture review and quick wins — re-verify findings against current code before acting | [docs/dev/architecture-review-cn/](docs/dev/architecture-review-cn/README.md) |

Conventions that deliberately diverge from the mainstream template are called out inline in the conventions doc — don't "normalize" them in passing.

## Git and CI

- PRs target `develop` (the default branch). Commit messages follow Conventional Commits with **lowercase type + scope**: `fix(home): ...`.
- End every commit message with a trailer naming the agent, the model, and the reasoning effort level (`off` / `low` / `medium` / `high` / `xhigh` / `max`, ...) that produced it, e.g. `Generated with ZCode (GLM-5.3, effort: xhigh)`. Name the level `effort:` — it is the reasoning-effort knob itself, not a verdict on the reasoning; commits from 2026-09-22 and earlier spell it `reasoning:`, leave those as they are. Never guess a value; when the agent, model, or effort cannot be determined, ask the user what to record rather than silently writing `unknown`.
- Name the agent from evidence, not from habit: how to find it is up to you, but show what it rests on and get the user's agreement before writing it. Edition-level names differ (`Qoder CN` and `Qoder`, `Trae CN` and `Trae` are different ADEs); do not invent host-form suffixes such as `IDE` / `CLI` unless the user asks for them.
- CI builds `assembleFossDebug` only. `master` carries `lld.json` data updates — don't open PRs against it.

## Never commit

- Never commit or force-add credentials, signing material, or local configuration (`local.properties`, `keys/release.jks`, `google-services.json`) — the root `.gitignore` already excludes them.
- If a change seems to require committing such files, stop and ask first.
