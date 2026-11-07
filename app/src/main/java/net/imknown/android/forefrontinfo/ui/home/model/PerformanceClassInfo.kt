package net.imknown.android.forefrontinfo.ui.home.model

/**
 * The facts of detectPerformanceClass: the declared media performance class and the
 * repository's judgment of it against the running SDK level.
 */
data class PerformanceClassInfo(
    val performanceClass: Int,
    val level: Level
) {
    enum class Level { Met, Lagging, Outdated, NotDeclared, NotSupported }
}
