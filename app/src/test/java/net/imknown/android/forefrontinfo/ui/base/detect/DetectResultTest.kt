package net.imknown.android.forefrontinfo.ui.base.detect

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

// Shape contract: three exhaustive branches, and Missing/Failed fit a concretely typed container via Nothing.
class DetectResultTest {

    @Test
    fun `found carries the value it was given`() {
        val result = DetectResult.Found(33)

        assertEquals(33, result.value)
    }

    @Test
    fun `missing carries its reason`() {
        val result = DetectResult.Missing("performance class not declared")

        assertEquals("performance class not declared", result.reason)
    }

    @Test
    fun `failed carries its cause`() {
        val cause = IllegalStateException("reader blew up")
        val result = DetectResult.Failed(cause)

        assertSame(cause, result.cause)
    }

    // The when covers all three branches without else: exhaustiveness is compiler-guaranteed for a sealed family.
    @Test
    fun `all three branches dispatch exhaustively for one entry type`() {
        val results = listOf(
            DetectResult.Found(33),
            DetectResult.Missing("not declared"),
            DetectResult.Failed(IllegalStateException("boom"))
        )

        val labels = results.map { result ->
            when (result) {
                is DetectResult.Found -> "found"
                is DetectResult.Missing -> "missing"
                is DetectResult.Failed -> "failed"
            }
        }

        assertEquals(listOf("found", "missing", "failed"), labels)
    }

    // Explicit entry type on the container: drop "out" from DetectResult and the two assignments below stop compiling.
    @Test
    fun `missing and failed fit a concretely typed container`() {
        val missing: DetectResult<Int> = DetectResult.Missing("not declared")
        val failed: DetectResult<Int> = DetectResult.Failed(IllegalStateException("boom"))

        assertEquals("not declared", (missing as DetectResult.Missing).reason)
        assertEquals("boom", (failed as DetectResult.Failed).cause.message)
    }
}
