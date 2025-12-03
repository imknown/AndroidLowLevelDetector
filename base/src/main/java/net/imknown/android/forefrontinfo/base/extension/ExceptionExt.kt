package net.imknown.android.forefrontinfo.base.extension

val Throwable.fullMessage
    get() = "${javaClass.canonicalName}: $message\nCaused by: ${cause?.message}."