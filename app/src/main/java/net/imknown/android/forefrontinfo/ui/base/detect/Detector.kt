package net.imknown.android.forefrontinfo.ui.base.detect

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import net.imknown.android.forefrontinfo.ui.base.list.MyModel

/**
 * One detection entry as data: the entry's page-specific stable id,
 * the entry's whole fetch-and-translate chain, and the rows the engine falls back to when the
 * chain throws unexpectedly. List order is display order.
 */
class Detector(
    val id: String,
    val run: () -> List<MyModel>,
    val failureRows: (Exception) -> List<MyModel>
)

/**
 * Builds a [Detector] whose chain ends in a three-state result: [translate] dispatches on
 * [DetectResult] and handles Failed itself, so the engine can also feed it a synthesized Failed
 * when the chain throws unexpectedly (the entry's own failure rows, title included).
 */
fun <T> detector(
    id: String,
    exec: () -> DetectResult<T>,
    translate: (DetectResult<T>) -> List<MyModel>
): Detector = Detector(id, { translate(exec()) }, { cause -> translate(DetectResult.Failed(cause)) })

/**
 * Builds a [Detector] whose chain does not end in a single [DetectResult] (the two multi-row
 * entries): the chain is authored as one lambda, and [failureRows] renders the entry's own
 * failure rows for the engine's catch.
 */
fun rawDetector(
    id: String,
    run: () -> List<MyModel>,
    failureRows: (Exception) -> List<MyModel>
): Detector = Detector(id, run, failureRows)

/**
 * Stage two of the detection engine: entries run one by one (the first version is fully serial;
 * bounded concurrency waits for the shared shell's re-entrancy question), cancellation is
 * checked before each entry starts, and each result lands in a fixed-size slot so display order
 * is the list order no matter what. The engine owns the per-entry guard (the old UI thin guard
 * retired into it): an unexpected exception anywhere in an entry's chain degrades into that
 * entry's own failure rows; [CancellationException] always rethrows.
 */
suspend fun List<Detector>.detectAll(): List<MyModel> {
    val slots = arrayOfNulls<List<MyModel>>(size)
    forEachIndexed { index, detector ->
        currentCoroutineContext().ensureActive()
        slots[index] = try {
            detector.run()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            detector.failureRows(e)
        }
    }
    return slots.mapIndexed { index, rows ->
        requireNotNull(rows) { "detector at index $index (${this@detectAll[index].id}) produced no rows" }
    }.flatten()
}
