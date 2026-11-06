package net.imknown.android.forefrontinfo.snapshot

import android.os.SystemClock
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import net.imknown.android.forefrontinfo.R
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.ui.base.list.MyModel
import net.imknown.android.forefrontinfo.ui.home.HomeViewModel
import net.imknown.android.forefrontinfo.ui.others.OthersViewModel
import net.imknown.android.forefrontinfo.ui.prop.PropViewModel
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

// How long the pinned settings get to reach their flows, and how often to re-check.
private const val SETTLE_TIMEOUT_MILLIS = 2_000L
private const val SETTLE_POLL_MILLIS = 20L

// Values that identify this particular device, the machine that built its ROM, or a local port.
// The snapshot is committed, so these must not travel with it: they are replaced by REDACTED.
// Matching is by substring on the row's key, which is the prop name for the prop page.
private const val REDACTED = "<redacted>"
private val SENSITIVE_KEY_PARTS = listOf(
    "serialno", "serialnumber", "android_id", "macaddr", "wifi.guid",
    "build.host", "build.user", "hostname", "device_name", "adb.tls.port"
)
// Four-character account names are in scope, so the bar cannot be higher. Values that are too
// generic to match on are named below instead: the model name shows up in dozens of rows, and the
// debug port is a bare number that only ever appears on its own row (which the key already hides).
private const val MIN_IDENTIFYING_LENGTH = 4
private val NOT_MATCHED_AS_VALUE = listOf("device_name", "adb.tls.port")
// The one shape that is unambiguous enough to check without knowing the key.
private val MAC_SHAPE = Regex("(?i)\\b[0-9a-f]{2}(:[0-9a-f]{2}){5}\\b")

private data class Row(
    val page: String,
    val index: Int,
    val key: String,
    val detail: String,
    val color: String,
    val type: String
)

// One-shot generator for the golden row data the UI-layer refactor compares against
// (spec 2026-10-25-22-16-01, subtask-02): every page's rows exactly as the ViewModels build
// them today.
//
// It calls each ViewModel's own collectModels() rather than replaying the repository call
// sequence, so the snapshot cannot drift from the production row order or content: whatever
// the pages build is what gets written.
//
// Not wired into CI: it writes a file on the device, which is pulled back with adb and stored
// in the spec directory. That file is committed, so device-identifying values are redacted on
// the way out (see SENSITIVE_KEY_PARTS); the row itself stays, so shape and order are intact.
@RunWith(AndroidJUnit4::class)
class ModelSnapshotTest {

    @Test
    fun dumpThreePages() = runBlocking {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
                as MyApplication
        val settingsStore = app.appGraph.settingsStore

        // Pin the two preferences the rows depend on: a device that happens to have network
        // data enabled would otherwise snapshot the online LLD path, and the outdated-order
        // switch changes the detail text of one row. The pins live inside the try so a failure
        // to settle still restores what the device had: these are the app's own settings.
        val allowNetworkBefore = settingsStore.allowNetworkData.value
        val outdatedOrderBefore = settingsStore.outdatedOrderFirst.value

        val factory = app.appGraph.metroViewModelFactory
        val collected = mutableListOf<Row>()

        try {
            settingsStore.setAllowNetworkData(false)
            settingsStore.setOutdatedOrderFirst(false)
            // The store refreshes its flows from the SharedPreferences change listener, and off
            // the main thread that callback is posted to the main looper, so the flows settle a
            // beat later. Wait for both: if they never settle, the snapshot would silently carry
            // the online LLD path or the other detail ordering instead of failing here.
            awaitSettled("allowNetworkData") { !settingsStore.allowNetworkData.value }
            awaitSettled("outdatedOrderFirst") { !settingsStore.outdatedOrderFirst.value }

            val homeModels = factory.create(HomeViewModel::class, MutableCreationExtras())
                .collectModels()
            collected += toRows("home", homeModels)

            val othersModels = factory.create(OthersViewModel::class, MutableCreationExtras())
                .collectModels()
            collected += toRows("others", othersModels)

            val propModels = factory.create(PropViewModel::class, MutableCreationExtras())
                .collectModels()
            collected += toRows("prop", propModels)
        } finally {
            settingsStore.setAllowNetworkData(allowNetworkBefore)
            settingsStore.setOutdatedOrderFirst(outdatedOrderBefore)
        }

        assertTrue("no rows were collected", collected.isNotEmpty())

        val rows = JSONArray()
        redactSensitive(collected, app.getString(R.string.build_not_filled)).forEach { row ->
            rows.put(
                JSONObject()
                    .put("page", row.page)
                    .put("index", row.index)
                    .put("key", row.key)
                    .put("detail", row.detail)
                    .put("color", row.color)
                    .put("type", row.type)
            )
        }

        val outFile = File(app.getExternalFilesDir(null), "snapshot/three-pages.json")
        outFile.parentFile?.mkdirs()
        outFile.writeText(JSONObject().put("rows", rows).toString(2))

        println("MODEL_SNAPSHOT_PATH=${outFile.absolutePath} rows=${rows.length()}")
    }

    // Waits for a settings flow to reach the pinned value. The callback that updates it is posted
    // to the main looper, so this test thread has to let the looper run instead of reading once.
    private fun awaitSettled(name: String, settled: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + SETTLE_TIMEOUT_MILLIS
        while (!settled()) {
            check(SystemClock.uptimeMillis() < deadline) {
                "$name did not settle within ${SETTLE_TIMEOUT_MILLIS}ms"
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            Thread.sleep(SETTLE_POLL_MILLIS)
        }
    }

    private fun toRows(page: String, models: List<MyModel>): List<Row> =
        models.mapIndexed { index, model ->
            Row(page, index, model.key, model.detail, model.color.name, model.type.name)
        }

    // Two passes: first take the identifying values off the rows that carry them by name, then
    // scrub those values wherever else they appear. Key-only matching was not enough: the build
    // account rides inside build-version strings, and the Others page shows the build host under
    // a resource-id key. Scrubbing is inline and token-bounded, so the product fact around a
    // value survives and a four-character account does not eat into longer words.
    // The model name is deliberately not collected - it appears in dozens of unrelated rows and
    // does not point at one machine; its own row is still hidden by key.
    private fun redactSensitive(rows: List<Row>, placeholder: String): List<Row> {
        val identifyingValues = rows
            .filter { row ->
                isSensitiveKey(row.key) && NOT_MATCHED_AS_VALUE.none { row.key.lowercase().contains(it) }
            }
            .map { it.detail }
            .filter {
                it.length >= MIN_IDENTIFYING_LENGTH && it != REDACTED &&
                    it.any(Char::isLetterOrDigit) && it != placeholder
            }
            .distinct()

        val scrubbed = rows.map { row ->
            if (isSensitiveKey(row.key)) {
                row.copy(detail = REDACTED)
            } else {
                var detail = row.detail
                identifyingValues.forEach { value ->
                    detail = detail.replace(tokenRegex(value), REDACTED)
                }
                if (detail == row.detail) row else row.copy(detail = detail)
            }
        }

        // Fail loudly instead of shipping a leak. Two checks over the scrubbed rows:
        // 1) a collected value that still matches by the same token rule the scrubbing used,
        // 2) a MAC-shaped value anywhere, whatever its key was.
        // What neither can see: a value whose key is not in the fragment list and whose shape is
        // not a MAC. Keeping the fragment list current is what covers that case.
        val leakedValues = scrubbed.filter { row ->
            identifyingValues.any { value -> tokenRegex(value).containsMatchIn(row.detail) }
        }
        check(leakedValues.isEmpty()) {
            "identifying values still present in ${leakedValues.size} row(s), " +
                "first key=${leakedValues.firstOrNull()?.key}"
        }
        val leakedShapes = scrubbed.filter { looksLikeMacAddress(it.detail) }
        check(leakedShapes.isEmpty()) {
            "MAC-shaped value still present in ${leakedShapes.size} row(s), " +
                "first key=${leakedShapes.firstOrNull()?.key}"
        }

        return scrubbed
    }

    private fun tokenRegex(value: String): Regex = Regex(
        "(?<![A-Za-z0-9])${Regex.escape(value)}(?![A-Za-z0-9])",
        RegexOption.IGNORE_CASE
    )

    private fun looksLikeMacAddress(detail: String): Boolean = MAC_SHAPE.containsMatchIn(detail)

    private fun isSensitiveKey(key: String): Boolean {
        val lower = key.lowercase()
        return SENSITIVE_KEY_PARTS.any { lower.contains(it) }
    }
}
