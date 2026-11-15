package net.imknown.android.forefrontinfo.ui.base.detect

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.list.MyModelTitle
import net.imknown.android.forefrontinfo.ui.home.mapper.HomeModelMapper
import net.imknown.android.forefrontinfo.ui.theme.StatusColor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import tech.apter.junit.jupiter.robolectric.RobolectricExtension

// The engine's per-entry guard contract (moved here from the retired mapper thin guard): an
// unexpected exception anywhere in an entry's chain degrades into that entry's own failure
// rows, coroutine cancellation rethrows, results land in list order, and the synthesized Failed
// rides the entry's own translation. Asserts row shape and colors only, never the exact
// display text.
@ExtendWith(RobolectricExtension::class)
class DetectionEngineTest {

    private val mapper by lazy { HomeModelMapper(MyApplication.instance) }

    private fun developerOptionsDetector(exec: () -> DetectResult<Boolean>) = detector(
        "HOME-16",
        exec
    ) { result ->
        listOf(mapper.toDeveloperOptionsModel(result))
    }

    @Test
    fun `unexpected exception in the chain degrades to the entry failure row`() {
        val rows = runBlocking {
            listOf(developerOptionsDetector { throw IllegalStateException("probe") }).detectAll()
        }

        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals(MyModelTitle.Res(R.string.developer_options_status_title), row.title)
        assertEquals(StatusColor.CRITICAL, row.color)
        assertTrue(row.detail.isNotEmpty())
    }

    @Test
    fun `cancellation from the chain rethrows`() {
        assertThrows(CancellationException::class.java) {
            runBlocking {
                listOf(developerOptionsDetector { throw CancellationException("cancelled") }).detectAll()
            }
        }
    }

    @Test
    fun `rows land in list order`() {
        val rows = runBlocking {
            listOf(
                developerOptionsDetector { DetectResult.Found(true) },
                developerOptionsDetector { DetectResult.Found(false) }
            ).detectAll()
        }

        assertEquals(
            listOf(StatusColor.NO_PROBLEM, StatusColor.CRITICAL),
            rows.map { it.color }
        )
    }

    @Test
    fun `failed result passes through to the failure row`() {
        val rows = runBlocking {
            listOf(developerOptionsDetector { DetectResult.Failed(IllegalStateException("boom")) }).detectAll()
        }

        assertEquals(StatusColor.CRITICAL, rows.single().color)
    }

    @Test
    fun `raw detector chain exception expands to the two authored failure rows`() {
        // Mechanism test: the engine expands whatever failureRows returns, in order. The
        // production fallbacks themselves are asserted in HomeModelMapperTest.
        val rows = runBlocking {
            listOf(
                rawDetector(
                    "HOME-05",
                    run = { throw IllegalStateException("probe") },
                    failureRows = { cause ->
                        listOf(
                            mapper.toFailureModel(R.string.security_patch_level_title, cause),
                            mapper.toFailureModel(R.string.vendor_security_patch_level_title, cause)
                        )
                    }
                )
            ).detectAll()
        }

        assertEquals(2, rows.size)
        assertEquals(
            listOf(
                MyModelTitle.Res(R.string.security_patch_level_title),
                MyModelTitle.Res(R.string.vendor_security_patch_level_title)
            ),
            rows.map { it.title }
        )
        assertEquals(listOf(StatusColor.CRITICAL, StatusColor.CRITICAL), rows.map { it.color })
        assertTrue(rows.all { it.detail.isNotEmpty() })
    }

    @Test
    fun `cancellation from the chain stops the remaining entries`() {
        val laterEntriesRan = mutableListOf<String>()

        assertThrows(CancellationException::class.java) {
            runBlocking {
                listOf(
                    detector<Boolean>("HOME-01", exec = { throw CancellationException("cancelled") }) {
                        error("first entry's translate must not run after cancellation")
                    },
                    rawDetector(
                        "HOME-02",
                        run = {
                            laterEntriesRan.add("HOME-02")
                            error("second entry must not run after cancellation")
                        },
                        failureRows = { error("unreachable") }
                    )
                ).detectAll()
            }
        }

        assertTrue(laterEntriesRan.isEmpty())
    }

    @Test
    fun `cancelling the job between entries stops the remaining ones via ensureActive`() {
        val laterEntriesRan = mutableListOf<String>()
        val job = Job()

        runBlocking {
            val worker = launch(job) {
                listOf(
                    developerOptionsDetector(exec = { job.cancel(); DetectResult.Found(true) }),
                    rawDetector(
                        "HOME-02",
                        run = {
                            laterEntriesRan.add("HOME-02")
                            error("second entry must not run after the job was cancelled")
                        },
                        failureRows = { error("unreachable") }
                    )
                ).detectAll()
            }
            worker.join()
            assertTrue(worker.isCancelled)
        }

        assertTrue(laterEntriesRan.isEmpty())
    }
}
