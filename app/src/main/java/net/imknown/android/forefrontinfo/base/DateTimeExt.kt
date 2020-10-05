package net.imknown.android.forefrontinfo.base

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun String.formatToLocalZonedDatetimeString(): String {
    val patternZonedDatetime = "yyyy-MM-dd HH:mm Z"
    val formatter = DateTimeFormatter.ofPattern(patternZonedDatetime)
    val zonedDateTime =
        // ZonedDateTime.ofInstant(ZonedDateTime.parse(this, formatter).toInstant(), ZoneId.systemDefault())
        ZonedDateTime.parse(this, formatter).toInstant().atZone(ZoneId.systemDefault())
    return formatter.format(zonedDateTime)
}

fun Long.formatToDatetimeString(): String {
    val patternDatetime = "yyyy-MM-dd HH:mm:ss"
    val formatter = DateTimeFormatter.ofPattern(patternDatetime)
    val localDateTime =
        // Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()
        LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

    return formatter.format(localDateTime)
}

fun isChinaMainlandTimezone() =
    ZoneId.systemDefault().id == "Asia/Shanghai" || ZoneId.systemDefault().id == "Asia/Urumqi"