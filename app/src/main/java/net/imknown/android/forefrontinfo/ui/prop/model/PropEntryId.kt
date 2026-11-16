package net.imknown.android.forefrontinfo.ui.prop.model

import net.imknown.android.forefrontinfo.ui.base.detect.Detector
import net.imknown.android.forefrontinfo.ui.base.detect.rawDetector
import net.imknown.android.forefrontinfo.ui.base.list.MyModel

/**
 * The Prop detection entries' stable ids. Declaration order is the display order the Prop
 * list assembles, and it matches the row order of the detection-entry catalog; the [id]
 * strings are the same strings the catalog's semantic-id column carries.
 * The rows are plain information: no three-state results, no colors.
 */
enum class PropEntryId(val id: String) {
    PROP_SYSTEM_PROP("prop-system-prop"),
    PROP_SETTINGS_SYSTEM("prop-settings-system"),
    PROP_SETTINGS_SECURE("prop-settings-secure"),
    PROP_SETTINGS_GLOBAL("prop-settings-global"),
    PROP_BUILD_PROP("prop-build-prop")
}

/**
 * [Detector] factory keyed by [PropEntryId]: the overload converts the enum to its id string
 * so the generic engine stays page-agnostic.
 * The entries render rows directly (the repository guards itself), so there is no
 * [Detector.failureRows] fallback: a chain exception keeps rethrowing, same as before the
 * engine.
 * No preconditions: every source exists on every device.
 */
fun rawDetector(
    id: PropEntryId,
    run: () -> List<MyModel>
): Detector = rawDetector(id.id, run = run)
