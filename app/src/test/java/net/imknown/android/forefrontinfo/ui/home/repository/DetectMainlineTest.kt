package net.imknown.android.forefrontinfo.ui.home.repository

import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.home.model.MainlineState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

// The domain contract of detectMainline: the provider and version nullability follows the
// framework config lookup (the Robolectric environment decides whether the internal resource
// resolves; either way no provider package is installed here), so the tests pin the match
// judgment and the carried lld date, not the environment's provider value.
// The comparison's dimension mismatch (a versionName against a date) is a known defect,
// locked as-is per the #43 disposition and owned by #70: the direct mainlineMatch cases
// pin the misjudgments so the future fix flips them visibly.
@ExtendWith(RobolectricExtension::class)
class DetectMainlineTest {

    private val repository by lazy { MyApplication.instance.appGraph.homeRepository }

    @Test
    fun `a missing lld is unknown`() {
        val state = (repository.detectMainline(null) as DetectResult.Found).value

        assertEquals(MainlineState.Match.Unknown, state.match)
        assertNull(state.latestGooglePlaySystemUpdates)
    }

    @Test
    fun `an lld date with no installed provider module is none`() {
        val state = (repository.detectMainline(lldForTests()) as DetectResult.Found).value

        assertEquals(MainlineState.Match.None, state.match)
        assertEquals("2026-11-01", state.latestGooglePlaySystemUpdates)
    }

    @Test
    fun `a month precision date is lifted to met by the suffix hack`() {
        // The "-01" suffix normalizes a month-precision date to the lld's day precision;
        // the comparison was designed for date-form versionNames.
        assertEquals(MainlineState.Match.Met, mainlineMatch("2026-11", "2026-11-01"))
    }

    @Test
    fun `an emulator style date with a suffix compares as none when older`() {
        // Google API system images report "2023-07-01 S+"; the date prefix dominates the
        // string compare, so the suffix changes nothing.
        assertEquals(MainlineState.Match.None, mainlineMatch("2023-07-01 S+", "2026-11-01"))
    }

    @Test
    fun `a system version format compares as none against the lld date (locked as is, 70)`() {
        // com.android.modulemetadata on real devices reports the system version ("14");
        // against a date it is a dimension mismatch, and "1"-prefixed versions lose the
        // lexicographic compare for every Android 10-19 release. Locked as-is, owned by #70.
        assertEquals(MainlineState.Match.None, mainlineMatch("14", "2026-11-01"))
    }

    @Test
    fun `a three dot one style version compares as met against the lld date (locked as is, 70)`() {
        // The synthetic counterpart of the mismatch: values sorting above a date (3-prefixed)
        // are met regardless of the actual update state. Locked as-is, owned by #70.
        assertEquals(MainlineState.Match.Met, mainlineMatch("3.1", "2026-11-01"))
    }

    @Test
    fun `a failed query carries an empty version and compares as none (locked as is, 70)`() {
        assertEquals(MainlineState.Match.None, mainlineMatch("", "2026-11-01"))
    }
}
