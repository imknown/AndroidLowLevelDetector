package net.imknown.android.forefrontinfo.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

// Composition root: the only place in the whole project that knows "who needs
// whom" (issues-cn #02/#05). Metro resolves the entire graph at compile time
// from @Inject constructors, and a constructor-injected class is bound to its
// concrete type automatically, so the current all-concrete dependency chain
// needs no @Provides at all. The graph body stays empty for now; the real
// @Provides bindings (HttpClient, SharedPreferences, ...) land in phase two.
//
// Extending ViewModelGraph brings in MetroX's three ViewModel multibindings
// plus the metroViewModelFactory accessor that MainActivity provides down to
// Compose. That accessor can only resolve if a MetroViewModelFactory binding
// exists — supplied by AppViewModelFactory (via @ContributesBinding); without
// it the graph fails to compile with [Metro/MissingBinding].
//
// AppScope is Metro's application-lifetime scope: a @SingleIn(AppScope) binding
// caches one instance per graph, and this graph is the only one in the process.
// ViewModels deliberately get no @SingleIn — their lifetime belongs to the
// ViewModelStore (one per Navigation 3 entry), so the factory must emit a
// fresh instance on every create call.
@DependencyGraph(AppScope::class)
interface AppGraph : ViewModelGraph
