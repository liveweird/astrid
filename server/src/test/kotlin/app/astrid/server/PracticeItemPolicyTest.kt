package app.astrid.server

import app.astrid.server.practice.PracticeItemPolicyPayload
import app.astrid.server.practice.effectiveCompletionRule
import app.astrid.server.practice.effectiveExposurePolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PracticeItemPolicyTest {
    @Test
    fun `absent item policy inherits globals while explicit nulls override them`() {
        val absent: PracticeItemPolicyPayload? = null
        assertEquals("global-completion", absent.effectiveCompletionRule("global-completion"))
        assertEquals("global-exposure", absent.effectiveExposurePolicy("global-exposure"))

        val explicit = PracticeItemPolicyPayload(null, null)
        assertNull(explicit.effectiveCompletionRule("global-completion"))
        assertNull(explicit.effectiveExposurePolicy("global-exposure"))
    }
}
