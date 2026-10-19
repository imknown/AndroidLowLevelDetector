# Detection Item Reference

> **Status**: **living**, part of [AGENTS.md](../../AGENTS.md)'s Adding a detection item section.  
> Read this before you add a detection item to any feature.  
> The invariant that stays in AGENTS.md is the duty itself: follow this workflow when adding a detection  
> item.

The workflow (list order = call order):

1. Add property keys / shell commands to the feature `DataSource`.
2. Add a `detect...()` method to the feature `Repository` returning `MyModel`  
   (new Repository / DataSource classes join the graph via `@Inject` constructor injection;  
   a missing annotation fails the build with `[Metro/MissingBinding]`).
3. Call it where the feature ViewModel builds the list: `collectModels()` for Others and Prop, and the  
   `detect()` call it makes for Home. The call order defines the list order.
4. Add the strings to the feature package's `strings.xml` (default English) plus the three translation files.
5. Add the item to [the detection item catalog](../requirements/04-detection-item-catalog.md) and its  
   Chinese copy: a row in the screen's table and, on Home, its definition block with `question`,  
   `techniques` and `tri-state semantics`. The catalog's order is the call order of step 3.
6. If the item needs a new package with resources, register its res directory in  
   `app/build.gradle.kts` sourceSets.
7. Verify with `./gradlew assembleFossDebug`; this step covers the change itself, and the verify flow that  
   binds every session (see [AGENTS.md](../../AGENTS.md)'s Build and verify section) applies on top of it.
