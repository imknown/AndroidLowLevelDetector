package net.imknown.android.forefrontinfo.base.extension

/** Fix weird R8 NPE on Android 6 only: Attempt to get length of null array */
fun <T> listOfSafety(vararg elements: T) = buildList {
    elements.forEach(::add)
}