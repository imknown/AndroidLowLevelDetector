package net.imknown.android.forefrontinfo.ui.home.mapper

import kotlinx.coroutines.CancellationException
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.detect.DetectResult
import net.imknown.android.forefrontinfo.ui.base.list.MyModelTitle
import net.imknown.android.forefrontinfo.ui.theme.StatusColor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

// The chained thin guard's contract: an unexpected exception degrades into the entry's own
// failure row, coroutine cancellation rethrows, and the three branches render their own colors.
// Asserts row shape and colors only, never the exact display text.
@ExtendWith(RobolectricExtension::class)
class HomeModelMapperTest {

    private val mapper by lazy { HomeModelMapper(MyApplication.instance) }

    @Test
    fun `unexpected exception in the chain degrades to the entry failure row`() {
        val row = mapper.toDeveloperOptionsModel { throw IllegalStateException("probe") }

        assertEquals(MyModelTitle.Res(R.string.developer_options_status_title), row.title)
        assertEquals(StatusColor.CRITICAL, row.color)
        assertTrue(row.detail.isNotEmpty())
    }

    @Test
    fun `cancellation from the detect chain rethrows`() {
        assertThrows(CancellationException::class.java) {
            val _ = mapper.toDeveloperOptionsModel { throw CancellationException("cancelled") }
        }
    }

    @Test
    fun `failed result renders the failure row`() {
        val row = mapper.toDeveloperOptionsModel {
            DetectResult.Failed(IllegalStateException("boom"))
        }

        assertEquals(StatusColor.CRITICAL, row.color)
    }

    @Test
    fun `found true and false map to the two state colors`() {
        val disabled = mapper.toDeveloperOptionsModel { DetectResult.Found(true) }
        val enabled = mapper.toDeveloperOptionsModel { DetectResult.Found(false) }

        assertEquals(StatusColor.NO_PROBLEM, disabled.color)
        assertEquals(StatusColor.CRITICAL, enabled.color)
    }
}
