package net.imknown.android.forefrontinfo.base.extension

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import java.util.Locale
import java.util.TimeZone
import org.junit.Test

class DateTimeExtTest {

    @Test
    fun `lld datetime contract accepts the exact format`() {
        assertTrue("2026-09-02 20:55 +0800".isLldDatetime())
    }

    @Test
    fun `lld datetime contract rejects deviating formats`() {
        assertFalse("2026-09-02 20:55 +08:00".isLldDatetime()) // colon in the offset
        assertFalse("2026-09-02 20:55".isLldDatetime()) // offset missing
        assertFalse("2026-09-02T20:55 +0800".isLldDatetime()) // ISO separator
        assertFalse("".isLldDatetime())
    }

    @Test
    fun `format round trips within the same offset`() {
        val originalLocale = Locale.getDefault(Locale.Category.FORMAT)
        val originalTimezone = TimeZone.getDefault()
        try {
            Locale.setDefault(Locale.Category.FORMAT, Locale.US)
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+08:00"))

            val input = "2026-09-02 20:55 +0800"
            assertEquals(input, input.formatToLocalZonedDatetimeString())
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, originalLocale)
            TimeZone.setDefault(originalTimezone)
        }
    }

    @Test
    fun `parsing is locale independent - native digit locales still parse ascii input`() {
        val originalLocale = Locale.getDefault(Locale.Category.FORMAT)
        val originalTimezone = TimeZone.getDefault()
        try {
            // ar renders native digits by default; the formatter must not adopt them (N1).
            Locale.setDefault(Locale.Category.FORMAT, Locale("ar"))
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+08:00"))

            val input = "2026-09-18 13:00 +0800"
            assertTrue(input.isLldDatetime())
            assertEquals(input, input.formatToLocalZonedDatetimeString())
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, originalLocale)
            TimeZone.setDefault(originalTimezone)
        }
    }
}
