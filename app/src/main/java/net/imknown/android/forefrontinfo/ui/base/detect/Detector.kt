package net.imknown.android.forefrontinfo.ui.base.detect

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import net.imknown.android.forefrontinfo.ui.base.list.MyModel

/**
 * One detection entry as data: the entry's page-specific stable id,
 * the entry's whole fetch-and-translate chain, and the rows the engine falls back to when the
 * chain throws unexpectedly. List order is display order.
 *
 * [precondition] declares platform gates as data (e.g. a `Build.VERSION.SDK_INT` check): when it
 * returns false the engine skips the entry without running its chain, exactly as the old inline
 * `if` did. [failureRows] is the engine's catch fallback; pages whose repositories guard
 * themselves pass null, and if the chain still throws the exception rethrows as before.
 */
class Detector(
    val id: String,
    val precondition: (() -> Boolean)? = null,
    val run: () -> List<MyModel>,
    val failureRows: ((Exception) -> List<MyModel>)? = null
)

/**
 * Builds a [Detector] whose chain ends in a three-state result: [translate] dispatches on
 * [DetectResult] and handles Failed itself, so the engine can also feed it a synthesized Failed
 * when the chain throws unexpectedly (the entry's own failure rows, title included).
 */
fun <T> detector(
    id: String,
    precondition: (() -> Boolean)? = null,
    exec: () -> DetectResult<T>,
    translate: (DetectResult<T>) -> List<MyModel>
): Detector = Detector(
    id,
    precondition,
    { translate(exec()) },
    { cause -> translate(DetectResult.Failed(cause)) }
)

/**
 * Builds a [Detector] whose chain does not end in a single [DetectResult] (multi-row entries and
 * pages that render rows directly): the chain is authored as one lambda, and [failureRows], when
 * provided, renders the entry's own failure rows for the engine's catch.
 */
fun rawDetector(
    id: String,
    precondition: (() -> Boolean)? = null,
    run: () -> List<MyModel>,
    failureRows: ((Exception) -> List<MyModel>)? = null
): Detector = Detector(id, precondition, run, failureRows)

/**
 * Stage two of the detection engine: entries run one by one (the first version is fully serial;
 * bounded concurrency waits for the shared shell's re-entrancy question), cancellation is
 * checked before each entry starts, a false [Detector.precondition] skips the entry, and each
 * result lands in a fixed-size slot so display order is the list order no matter what. The
 * engine owns the per-entry guard for entries that provide [Detector.failureRows]: an
 * unexpected exception anywhere in an entry's chain degrades into that entry's own failure
 * rows; entries without it rethrow (pages whose repositories guard themselves);
 * [CancellationException] always rethrows.
 */
suspend fun List<Detector>.detectAll(): List<MyModel> {
    val slots = arrayOfNulls<List<MyModel>>(size)
    forEachIndexed { index, detector ->
        currentCoroutineContext().ensureActive()
        if (detector.precondition?.invoke() == false) {
            slots[index] = emptyList()
            return@forEachIndexed
        }
        slots[index] = try {
            detector.run()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            detector.failureRows?.invoke(e) ?: throw e
        }
    }
    // The explicit mapIndexed-plus-flatten is kept: the requireNotNull message needs the slot
    // index and the detector id, and the flatter alternatives mis-type under K2 here.
    return slots.mapIndexed { index, rows ->
        requireNotNull(rows) { "detector at index $index (${this@detectAll[index].id}) produced no rows" }
    }.flatten()
}
