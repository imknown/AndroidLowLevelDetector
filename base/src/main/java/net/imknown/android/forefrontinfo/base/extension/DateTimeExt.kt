package net.imknown.android.forefrontinfo.base.extension

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val LLD_DATETIME_PATTERN = "yyyy-MM-dd HH:mm Z"

/**
 * The machine-checked contract for lld.json's `version` field. The data producer and this
 * formatter are two codebases; without a shared constant the format is only a magic string.
 * US locale keeps parsing independent of the device's format locale (native-digit locales
 * like ar/fa would otherwise fail to parse ASCII input).
 */
val LLD_DATETIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(LLD_DATETIME_PATTERN, Locale.US)

fun String.isLldDatetime(): Boolean =
    runCatching { ZonedDateTime.parse(this, LLD_DATETIME_FORMATTER) }.isSuccess

fun String.formatToLocalZonedDatetimeString(): String {
    val instant = ZonedDateTime.parse(this, LLD_DATETIME_FORMATTER).toInstant()
    val datetime = instant.atZone(ZoneId.systemDefault())
    return LLD_DATETIME_FORMATTER.format(datetime)
}

fun Long.formatToLocalZonedDatetimeString(): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z", Locale.US)
    val instant = Instant.ofEpochMilli(this)
    val datetime = instant.atZone(ZoneId.systemDefault())
    return formatter.format(datetime)
}

fun isChinaMainlandTimezone() = with(ZoneId.systemDefault()) {
    id == "Asia/Shanghai" || id == "Asia/Urumqi"
}
