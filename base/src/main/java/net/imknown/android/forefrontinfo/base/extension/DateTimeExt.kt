package net.imknown.android.forefrontinfo.base.extension

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun String.formatToLocalZonedDatetimeString(): String {
    val pattern = "yyyy-MM-dd HH:mm Z"
    val formatter = DateTimeFormatter.ofPattern(pattern)
    val instant = ZonedDateTime.parse(this, formatter).toInstant()
    val datetime = instant.atZone(ZoneId.systemDefault())
    return formatter.format(datetime)
}

fun Long.formatToLocalZonedDatetimeString(): String {
    val pattern = "yyyy-MM-dd HH:mm:ss Z"
    val formatter = DateTimeFormatter.ofPattern(pattern)
    val instant = Instant.ofEpochMilli(this)
    val datetime = instant.atZone(ZoneId.systemDefault())
    return formatter.format(datetime)
}

fun isChinaMainlandTimezone() = with(ZoneId.systemDefault()) {
    id == "Asia/Shanghai" || id == "Asia/Urumqi"
}