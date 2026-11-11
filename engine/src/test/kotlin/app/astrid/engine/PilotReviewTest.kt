package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PilotReviewTest {
    @Test
    fun `pair sixteen review adds neutral questions while retained pair fifteen stays bounded`() {
        val current = PilotReview.renderMarkdown()
        val retained = PilotReview.renderPilotFifteenMarkdown()

        assertTrue("Content revision: `pilot-16`" in current)
        assertTrue("Generator revision: `generator-16`" in current)
        assertTrue("## Neutral yes/no question inventory" in current)
        assertTrue("### Asking yes or no questions" in current)
        assertTrue("Content revision: `pilot-15`" in retained)
        assertTrue("Neutral yes/no question" !in retained)
        assertTrue("Asking yes or no questions" !in retained)
    }

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
        val first = PilotReview.renderPilotThreeMarkdown()
        val second = PilotReview.renderPilotThreeMarkdown()

        assertEquals(first, second)
        assertTrue("Content revision: `pilot-3`" in first)
        assertTrue("Generator revision: `generator-3`" in first)
        assertTrue("draft-not-teacher-reviewed" in first)
        assertTrue("baseline 3b462a9" in first)
        assertTrue("23 lexical records; 99 sentences" in first)
        assertTrue("| L001 |" in first)
        assertTrue("| L023 |" in first)
        assertTrue("| 99 |" in first)
        assertTrue("E001-pilot-3-generator-3-C" in first)
        assertTrue("E006-pilot-3-generator-3-C" in first)
        assertTrue("E007-pilot-3-generator-3-L" in first)
        assertTrue("E008-pilot-3-generator-3-L" in first)
        val lexicalExampleRows = first.lineSequence().filter { it.startsWith("| E007 |") || it.startsWith("| E008 |") }.toList()
        assertEquals(4, lexicalExampleRows.size)
        assertTrue(lexicalExampleRows.all { " |  | Cue (" in it })
        assertTrue(lexicalExampleRows.filter { it.startsWith("| E007 |") }.all { "Cue (da):" in it })
        assertTrue(
            lexicalExampleRows.filter { it.startsWith("| E008 |") }
                .all { "Cue (en):" in it && "→ Danish singular headword" in it },
        )
        assertTrue("letter (written message)" in first)
        assertTrue("[replace: " in first)
        val legacy = PilotReview.renderLegacyMarkdown()
        assertTrue("Content revision: `pilot-1`" in legacy)
        assertTrue("Generator revision: `generator-1`" in legacy)
        assertTrue("[replace: " !in legacy)
        val pilotTwo = PilotReview.renderPilotTwoMarkdown()
        assertTrue("Content revision: `pilot-2`" in pilotTwo)
        assertTrue("Generator revision: `generator-2`" in pilotTwo)
        assertTrue("E006-pilot-2-generator-2-C" in pilotTwo)
    }
}
