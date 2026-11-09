package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PilotContentValidatorTest {
    private val valid = PilotContent.snapshot

    @Test
    fun `published snapshot is valid`() {
        assertEquals(emptyList(), PilotContentValidator.validate(valid))
        assertEquals(emptyList(), PilotContentValidator.validate(PilotContent.pilotTwoSnapshot))
        assertEquals(emptyList(), PilotContentValidator.validate(PilotContent.pilotThreeSnapshot))
        assertEquals(emptyList(), PilotContentValidator.validate(PilotContent.pilotFourSnapshot))
        assertEquals(emptyList(), PilotContentValidator.validate(PilotContent.pilotFiveSnapshot))
        assertEquals(emptyList(), PilotContentValidator.validate(PilotContent.pilotSixSnapshot))
        assertEquals(emptyList(), PilotContentValidator.validate(PilotContent.pilotSevenSnapshot))
        assertEquals(emptyList(), PilotContentValidator.validate(PilotContent.currentSnapshot))
    }

    @Test
    fun `vocabulary senses require unique exact identities and existing noun entries`() {
        val current = PilotContent.currentSnapshot
        val book = current.vocabularySenses.single { it.entryId == "L014" }
        assertIssue(current.copy(vocabularySenses = current.vocabularySenses + book), book.senseId, "duplicate vocabulary sense ID")
        assertIssue(
            current.copy(vocabularySenses = current.vocabularySenses.filterNot { it == book }),
            book.senseId,
            "missing vocabulary sense ID",
        )
        assertIssue(
            current.copy(vocabularySenses = current.vocabularySenses.replace(book, book.copy(entryId = "L999"))),
            book.senseId,
            "unknown entry L999",
        )
    }

    @Test
    fun `formats require exactly one matching grammar or lexical target`() {
        val current = PilotContent.currentSnapshot
        val recognition = current.formats.single { it.id == "E007" }
        assertIssue(
            current.copy(formats = current.formats.replace(recognition, recognition.copy(targetSkillId = "G004"))),
            recognition.id,
            "requires exactly one grammar or lexical target",
        )
        assertIssue(
            current.copy(formats = current.formats.replace(recognition, recognition.copy(lexicalTask = LexicalTask("meaning", "en-da")))),
            recognition.id,
            "expected lexical task meaning/da-en",
        )
    }

    @Test
    fun `duplicate and missing lexical IDs identify the records`() {
        val duplicate = valid.copy(humanPronouns = valid.humanPronouns + valid.humanPronouns.first())
        assertIssue(duplicate, "L001", "duplicate lexical ID")

        val missing = valid.copy(nouns = valid.nouns.filterNot { it.id == "L021" })
        assertIssue(missing, "L021", "missing lexical ID")
    }

    @Test
    fun `unknown references identify their sentence`() {
        val original = valid.sentences.first()
        val malformed = original.copy(subject = original.subject.copy(lexicalIds = listOf("L999")))
        val snapshot = valid.copy(sentences = valid.sentences.replace(original, malformed))

        assertIssue(snapshot, original.id, "unknown lexical reference L999")
    }

    @Test
    fun `prerequisite cycles identify a participating skill`() {
        val g001 = valid.skills.single { it.id == "G001" }
        val snapshot = valid.copy(skills = valid.skills.replace(g001, g001.copy(prerequisiteIds = listOf("G002"))))

        assertIssue(snapshot, "G001", "cyclic prerequisite")
    }

    @Test
    fun `empty and identical active verb forms identify the lexical record`() {
        val read = valid.verbs.single { it.lexicalId == "L011" }
        val empty = valid.copy(verbs = valid.verbs.replace(read, read.copy(present = "")))
        assertIssue(empty, "L011", "empty finite present form")

        val identical = valid.copy(verbs = valid.verbs.replace(read, read.copy(present = read.lemma)))
        assertIssue(identical, "L011", "infinitive and finite present forms are identical")
    }

    @Test
    fun `noun phrase article features are checked independently of surface plausibility`() {
        val house = valid.sentences.single { it.id == "C002-L001-L009-L019" }
        val malformed =
            house.copy(
                complement = requireNotNull(house.complement).copy(
                    lexicalIds = listOf("L022", "L019"),
                    text = "en hus",
                ),
            )
        val snapshot = valid.copy(sentences = valid.sentences.replace(house, malformed))

        assertIssue(snapshot, house.id, "expected [L023, L019]")
        assertIssue(snapshot, house.id, "expected 'et hus'")
    }

    @Test
    fun `invalid roles identify the sentence`() {
        val sleep = valid.sentences.single { it.id == "C001-L001-L013" }
        val malformed = sleep.copy(subject = sleep.subject.copy(role = "OBJECT"))
        val snapshot = valid.copy(sentences = valid.sentences.replace(sleep, malformed))

        assertIssue(snapshot, sleep.id, "invalid subject role OBJECT")
    }

    @Test
    fun `subject surface must exactly match its referenced pronoun including uppercase I and fixed det`() {
        val pluralYou = valid.sentences.single { it.id == "C001-L006-L013" }
        val blank = pluralYou.copy(subject = pluralYou.subject.copy(text = ""))
        assertIssue(
            valid.copy(sentences = valid.sentences.replace(pluralYou, blank)),
            pluralYou.id,
            "differs from L006 'I'",
        )

        val identifying = valid.sentences.single { it.id == "C003-L008-L010-L014" }
        val substituted = identifying.copy(subject = identifying.subject.copy(text = "den"))
        assertIssue(
            valid.copy(sentences = valid.sentences.replace(identifying, substituted)),
            identifying.id,
            "differs from L008 'det'",
        )
    }

    @Test
    fun `unlicensed verb noun combinations identify the sentence`() {
        val reading = valid.sentences.single { it.id == "C002-L001-L011-L014" }
        val table = valid.nouns.single { it.id == "L020" }
        val malformed =
            reading.copy(
                id = "C002-L001-L011-L020",
                complement =
                    Constituent(
                        lexicalIds = listOf(table.gender.articleId, table.id),
                        text = "${table.gender.article} ${table.text}",
                        role = "OBJECT",
                    ),
            )
        val snapshot = valid.copy(sentences = valid.sentences.replace(reading, malformed))

        assertIssue(snapshot, malformed.id, "unlicensed construction or lexical combination")
    }

    @Test
    fun `pair eight extension nouns cannot enter inherited C002 or C003 frames`() {
        val current = PilotContent.currentSnapshot
        val haveBook = current.sentences.single { it.id == "C002-L001-L009-L014" }
        val magazine = PilotContent.magazine
        val haveMagazine = haveBook.copy(
            id = "C002-L001-L009-L024",
            complement = Constituent(
                listOf(magazine.gender.articleId, magazine.id),
                "${magazine.gender.article} ${magazine.text}",
                "OBJECT",
            ),
        )
        assertIssue(
            current.copy(sentences = current.sentences.replace(haveBook, haveMagazine)),
            haveMagazine.id,
            "unlicensed construction or lexical combination",
        )

        val identifyingBook = current.sentences.single { it.id == "C003-L008-L010-L014" }
        val teacher = PilotContent.teacher
        val identifyingTeacher = identifyingBook.copy(
            id = "C003-L008-L010-L025",
            complement = Constituent(
                listOf(teacher.gender.articleId, teacher.id),
                "${teacher.gender.article} ${teacher.text}",
                "PREDICATIVE",
            ),
        )
        assertIssue(
            current.copy(sentences = current.sentences.replace(identifyingBook, identifyingTeacher)),
            identifyingTeacher.id,
            "unlicensed construction or lexical combination",
        )
    }

    @Test
    fun `pair eight review metadata extends only the bounded C004 records`() {
        val retainedVerb = PilotContent.pilotSevenSnapshot.lexicalReview.single { it.id == "L010" }
        val currentVerb = PilotContent.currentSnapshot.lexicalReview.single { it.id == "L010" }
        assertEquals("C003 fixed Det er … only; other copular and auxiliary uses excluded.", retainedVerb.constraints)
        assertTrue("ten enumerated C004 name and occupation clauses" in currentVerb.constraints)
        assertTrue(
            "bounded C004 name and occupation subjects" in
                PilotContent.currentSnapshot.lexicalReview.single { it.id == "L001" }.constraints,
        )
        assertTrue("C004" !in PilotContent.pilotSevenSnapshot.lexicalReview.single { it.id == "L001" }.constraints)
    }

    @Test
    fun `require valid reports all offending record IDs`() {
        val read = valid.verbs.single { it.lexicalId == "L011" }
        val malformed = valid.copy(verbs = valid.verbs.replace(read, read.copy(lemma = "", present = "")))

        val error = assertFailsWith<IllegalArgumentException> { PilotContentValidator.requireValid(malformed) }
        assertTrue("L011" in requireNotNull(error.message))
    }

    @Test
    fun `review metadata requires meaning active forms constraints sources and pilot review state`() {
        val record = valid.lexicalReview.single { it.id == "L011" }
        val malformedRecords =
            listOf(
                record.copy(selectedSense = "") to "empty selected sense",
                record.copy(activeForms = emptyMap()) to "missing active forms",
                record.copy(activeForms = mapOf("" to "læse")) to "blank active-form descriptor",
                record.copy(activeForms = mapOf("infinitive" to "")) to "blank active form",
                record.copy(constraints = "") to "empty review constraints",
                record.copy(sourceUrls = emptyList()) to "missing source reference",
                record.copy(sourceUrls = listOf("not a URL")) to "malformed HTTPS source reference",
                record.copy(sourceUrls = listOf("http://ordnet.dk/ddo/ordbog/l%C3%A6se")) to
                    "malformed HTTPS source reference",
                record.copy(sourceUrls = listOf("https://example.invalid/source")) to "unknown source reference",
                record.copy(reviewState = "teacher-reviewed") to "unsupported review state",
            )

        malformedRecords.forEach { (malformed, expectedMessage) ->
            val snapshot = valid.copy(lexicalReview = valid.lexicalReview.replace(record, malformed))
            assertIssue(snapshot, record.id, expectedMessage)
        }
    }

    private fun assertIssue(
        snapshot: PilotContentSnapshot,
        recordId: String,
        messagePart: String,
    ) {
        val issues = PilotContentValidator.validate(snapshot)
        assertTrue(
            issues.any { it.recordId == recordId && messagePart in it.message },
            "Expected $recordId / $messagePart in $issues",
        )
    }

    private fun <T> List<T>.replace(
        original: T,
        replacement: T,
    ): List<T> = map { if (it == original) replacement else it }
}
