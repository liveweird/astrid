package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PilotReviewTest {
    @Test
    fun `review metadata covers all 23 lexical records with sources and draft state`() {
        val records = PilotContent.snapshot.lexicalReview

        assertEquals((1..23).map { "L${it.toString().padStart(3, '0')}" }, records.map { it.id })
        assertTrue(records.all { it.selectedSense.isNotBlank() })
        assertTrue(records.all { it.activeForms.isNotEmpty() && it.activeForms.values.none(String::isBlank) })
        assertTrue(records.all { it.constraints.isNotBlank() })
        assertTrue(records.all { it.sourceUrls.isNotEmpty() })
        assertTrue(records.all { record -> record.sourceUrls.all { it.startsWith("https://") } })
        assertTrue(records.all { it.reviewState == "draft-not-teacher-reviewed" })
    }

    @Test
    fun `review markdown is deterministic and includes full inventory and reproduction data`() {
        val first = PilotReview.renderMarkdown()
        val second = PilotReview.renderMarkdown()

        assertEquals(first, second)
        assertTrue("Content revision: `pilot-2`" in first)
        assertTrue("Generator revision: `generator-2`" in first)
        assertTrue("draft-not-teacher-reviewed" in first)
        assertTrue("baseline 3b462a9" in first)
        assertTrue("23 lexical records; 99 sentences" in first)
        assertTrue("| L001 |" in first)
        assertTrue("| L023 |" in first)
        assertTrue("| 99 |" in first)
        assertTrue("E001-pilot-2-generator-2-C" in first)
        assertTrue("E002-pilot-2-generator-2-C" in first)
        assertTrue("E003-pilot-2-generator-2-C" in first)
        assertTrue("E004-pilot-2-generator-2-C" in first)
        assertTrue("E005-pilot-2-generator-2-C" in first)
        assertTrue("E006-pilot-2-generator-2-C" in first)
        assertTrue("[replace: " in first)
        val legacy = PilotReview.renderLegacyMarkdown()
        assertTrue("Content revision: `pilot-1`" in legacy)
        assertTrue("Generator revision: `generator-1`" in legacy)
        assertTrue("[replace: " !in legacy)
    }
}
