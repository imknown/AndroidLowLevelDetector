package net.imknown.android.forefrontinfo.ui.prop.detect

import android.provider.Settings
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.ui.base.detect.detectAll
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.prop.model.PropEntryId
import net.imknown.android.forefrontinfo.ui.prop.model.rawDetector
import net.imknown.android.forefrontinfo.ui.prop.repository.PropRepository

/**
 * The prop page's detection list (the prop part of issues-cn #03): which entries exist, in
 * what order. Prop has no platform gates and no shared input, every entry fetches its own,
 * so there is no request type (compare HomeDetectorCatalog's HomeDetectionRequest).
 */
@Inject
class PropDetectorCatalog(
    private val propRepository: PropRepository
) {

    suspend fun detect(): List<MyModel> = withContext(Dispatchers.IO) {
        // The entry list is data (ids are the PropEntryId semantic ids;
        // list order is the display order); every source exists on every device, so there
        // are no preconditions.
        val detectors = listOf(
            rawDetector(PropEntryId.PROP_SYSTEM_PROP) { propRepository.getSystemProp() },
            rawDetector(PropEntryId.PROP_SETTINGS_SYSTEM) {
                propRepository.getSettings(Settings.System::class)
            },
            rawDetector(PropEntryId.PROP_SETTINGS_SECURE) {
                propRepository.getSettings(Settings.Secure::class)
            },
            rawDetector(PropEntryId.PROP_SETTINGS_GLOBAL) {
                propRepository.getSettings(Settings.Global::class)
            },
            rawDetector(PropEntryId.PROP_BUILD_PROP) { propRepository.getBuildProp() }
        )

        detectors.detectAll()
    }
}
