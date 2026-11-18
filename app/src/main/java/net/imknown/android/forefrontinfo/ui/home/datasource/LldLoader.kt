package net.imknown.android.forefrontinfo.ui.home.datasource

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.imknown.android.forefrontinfo.BuildConfig
import net.imknown.android.forefrontinfo.base.MyApplication
import net.imknown.android.forefrontinfo.base.extension.fullMessage
import net.imknown.android.forefrontinfo.base.extension.isLldDatetime
import net.imknown.android.forefrontinfo.ui.common.toObjectOrThrow
import net.imknown.android.forefrontinfo.ui.home.model.Lld
import net.imknown.android.forefrontinfo.ui.home.model.LldFailure
import net.imknown.android.forefrontinfo.ui.home.model.LldFailureReason
import net.imknown.android.forefrontinfo.ui.home.model.LldSource
import java.io.IOException

/**
 * What one lld load produced: the payload the detection runs on (null when even the bundled asset
 * could not be read), where it came from, and what failed on the way, in fetch order.
 */
data class LldLoad(
    val lld: Lld?,
    val source: LldSource,
    val failures: List<LldFailure>
)

/**
 * The lld fallback chain, moved out of HomeViewModel (issues-cn #11's second half): online when the
 * user allows network, then the saved copy in the private files dir, then the bundled asset. The
 * chain is a data-source contract, so it lives here rather than in the UI layer, which is not
 * allowed to handle business exceptions (05-代码规则).
 *
 * It never throws: [load] reports every failure as an [LldFailure] reason, and the mapper turns
 * those into the row's text. A null payload is not an error either; it renders as the unknown row.
 */
@Inject
class LldLoader(
    private val lldDataSource: LldDataSource,
    private val lldFileStore: LldFileStore,
    private val application: MyApplication
) {

    suspend fun load(allowNetwork: Boolean): LldLoad =
        if (allowNetwork) loadOnline() else loadOffline(emptyList())

    private suspend fun loadOnline(): LldLoad {
        val lldString = try {
            withContext(Dispatchers.IO) {
                lldDataSource.fetchOnlineLldJsonStringOrThrow()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return loadOffline(listOf(e.toFailure(LldFailureReason.Fetch)))
        }

        val lld = try {
            withContext(Dispatchers.IO) {
                lldString.toObjectOrThrow<Lld>()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return loadOffline(listOf(e.toFailure(LldFailureReason.Parse)))
        }

        // The upstream `version` string is outside this app's control; the format contract is
        // machine-checked (LLD_DATETIME_FORMATTER). An unexpected format means the payload
        // cannot be trusted, so degrade to the saved copy instead of running the entries on it.
        if (!lld.version.isLldDatetime()) {
            val cause = IOException("Unsupported lld.version format: ${lld.version}")
            return loadOffline(listOf(cause.toFailure(LldFailureReason.Parse)))
        }

        // A failed write is reported but does not discard the online payload: the rows still show
        // the freshest data, only the next cold start loses it.
        val saveFailure = try {
            withContext(Dispatchers.IO) {
                lldFileStore.saveLldJsonFileOrThrow(lldString)
            }
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.toFailure(LldFailureReason.Save)
        }

        return LldLoad(lld, LldSource.ONLINE, listOfNotNull(saveFailure))
    }

    private suspend fun loadOffline(previous: List<LldFailure>): LldLoad {
        try {
            withContext(Dispatchers.IO) {
                lldFileStore.copyJsonIfNeededOrThrow()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val saveFailure = e.toFailure(LldFailureReason.Save)
            return LldLoad(getAssetLld(), LldSource.OFFLINE, previous + saveFailure)
        }

        val lld = try {
            withContext(Dispatchers.IO) {
                lldDataSource.fetchOfflineLldFileOrThrow().toObjectOrThrow<Lld>()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val parseFailure = e.toFailure(LldFailureReason.Parse)
            return LldLoad(getAssetLld(), LldSource.OFFLINE, previous + parseFailure)
        }

        return LldLoad(lld, LldSource.OFFLINE, previous)
    }

    // Last link of the chain: even the bundled asset read failing must not escape, because a null
    // payload renders as the unknown row rather than a crash.
    private suspend fun getAssetLld(): Lld? = try {
        withContext(Dispatchers.IO) {
            lldFileStore.getAssetLld(application.assets)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) {
            e.printStackTrace()
        }
        null
    }

    // The stack trace is debug-only noise control, not display material: the message travels with
    // the reason and the mapper formats it.
    private fun Exception.toFailure(reason: LldFailureReason): LldFailure {
        if (BuildConfig.DEBUG) {
            printStackTrace()
        }
        return LldFailure(reason, fullMessage)
    }
}
