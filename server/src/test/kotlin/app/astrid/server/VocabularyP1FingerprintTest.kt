package app.astrid.server

import app.astrid.engine.GenerationRequest
import app.astrid.engine.LinguisticStatus
import app.astrid.engine.MeaningSatisfaction
import app.astrid.engine.PilotEngine
import app.astrid.engine.ResponseConstraintStatus
import app.astrid.engine.TargetStatus
import app.astrid.engine.VocabularyLexicalKind
import app.astrid.server.exercises.reproductionFingerprint
import app.astrid.server.exercises.toInspector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VocabularyP1FingerprintTest {
    private val engine = PilotEngine()

    @Test
    fun `vocabulary inspector maps structured private grading sources without legacy answer duplication`() {
        val exercise = generate("E039", null, 0)
        val source = assertNotNull(exercise.inspector.vocabularySource)
        val mapped = assertNotNull(exercise.toInspector(EXERCISE_ID).vocabularySource)
        val fact = source.supportFacts.first()
        val mappedFact = mapped.supportFacts.first()

        assertEquals(source.targetSupportFactId, mapped.targetSupportFactId)
        assertEquals(source.questionRole, mapped.questionRole)
        assertEquals(fact.referentId, mappedFact.referentId)
        assertEquals(fact.predicateId, mappedFact.predicateId)
        assertEquals(fact.valueId, mappedFact.valueId)
        assertEquals(fact.valueText, mappedFact.valueText)
        assertEquals(fact.valueRole, mappedFact.valueRole)
        assertEquals(fact.exposureKeys, mappedFact.exposureKeys)
    }

    @Suppress("CyclomaticComplexMethod") // Exhaustive mutation coverage is kept in one compatibility-contract test.
    @Test
    fun `pair seventeen fingerprint covers every structured vocabulary grading input`() {
        val reading = generate("E039", null, 0)
        val readingSource = assertNotNull(reading.inspector.vocabularySource)
        val fact = readingSource.supportFacts.first()
        val readingOriginal = reading.reproductionFingerprint()
        val readingLexical = readingSource.lexicalSources.first()
        val readingForms = readingLexical.forms
        listOf(
            readingSource.copy(taskId = "different"),
            readingSource.copy(operation = "different"),
            readingSource.copy(targetSupportFactId = "different"),
            readingSource.copy(questionRole = "different"),
            readingSource.copy(suppliedChoices = readingSource.suppliedChoices.reversed()),
            readingSource.copy(lexicalSources = readingSource.lexicalSources.toMutableList().also {
                it[0] = readingLexical.copy(entryId = "different")
            }),
            readingSource.copy(lexicalSources = readingSource.lexicalSources.toMutableList().also {
                it[0] = readingLexical.copy(senseId = "different")
            }),
            readingSource.copy(lexicalSources = readingSource.lexicalSources.toMutableList().also {
                it[0] = readingLexical.copy(kind = VocabularyLexicalKind.entries.first { kind ->
                    kind != readingLexical.kind
                })
            }),
            readingSource.copy(lexicalSources = readingSource.lexicalSources.toMutableList().also {
                it[0] = readingLexical.copy(forms = readingForms.copy(infinitive = "different"))
            }),
            readingSource.copy(lexicalSources = readingSource.lexicalSources.toMutableList().also {
                it[0] = readingLexical.copy(forms = readingForms.copy(present = "different"))
            }),
            readingSource.copy(lexicalSources = readingSource.lexicalSources.toMutableList().also {
                it[0] = readingLexical.copy(forms = readingForms.copy(bundle = "different"))
            }),
            readingSource.copy(lexicalSources = readingSource.lexicalSources.toMutableList().also {
                it[0] = readingLexical.copy(forms = readingForms.copy(form = "different"))
            }),
            readingSource.copy(lexicalSources = readingSource.lexicalSources.toMutableList().also {
                it[0] = readingLexical.copy(gloss = "different")
            }),
            readingSource.copy(lexicalSources = readingSource.lexicalSources.toMutableList().also {
                it[0] = readingLexical.copy(selectedUse = "different")
            }),
            readingSource.copy(supportFacts = readingSource.supportFacts.reversed()),
            readingSource.copy(supportFacts = readingSource.supportFacts.toMutableList().also {
                it[0] = fact.copy(factId = "different")
            }),
            readingSource.copy(supportFacts = readingSource.supportFacts.toMutableList().also {
                it[0] = fact.copy(referentId = "different")
            }),
            readingSource.copy(supportFacts = readingSource.supportFacts.toMutableList().also {
                it[0] = fact.copy(predicateId = "different")
            }),
            readingSource.copy(supportFacts = readingSource.supportFacts.toMutableList().also {
                it[0] = fact.copy(valueId = "different")
            }),
            readingSource.copy(supportFacts = readingSource.supportFacts.toMutableList().also {
                it[0] = fact.copy(valueText = "different")
            }),
            readingSource.copy(supportFacts = readingSource.supportFacts.toMutableList().also {
                it[0] = fact.copy(valueRole = "different")
            }),
            readingSource.copy(supportFacts = readingSource.supportFacts.toMutableList().also {
                it[0] = fact.copy(sentence = "different")
            }),
            readingSource.copy(supportFacts = readingSource.supportFacts.toMutableList().also {
                it[0] = fact.copy(exposureKeys = fact.exposureKeys + "different")
            }),
        ).forEach { changed ->
            assertNotEquals(readingOriginal, reading.reproductionFingerprint(vocabularySourceForFingerprint = changed))
        }
        assertNotEquals(
            readingOriginal,
            reading.reproductionFingerprint(
                vocabularySourceForFingerprint = readingSource.copy(
                    supportFacts = readingSource.supportFacts.toMutableList().also {
                        it[0] = fact.copy(exposureKeys = fact.exposureKeys.reversed())
                    },
                ),
            ),
        )

        val recall = generate("E034", "G004", 0)
        val recallSource = assertNotNull(recall.inspector.vocabularySource)
        val recallScope = assertNotNull(recall.targetScopeId)
        assertTrue(recallScope in setOf("p1-studere-present", "p1-drikke-present"))
        val alternative = recallSource.knownAlternatives.first()
        val recallOriginal = recall.reproductionFingerprint()
        assertNotEquals(
            recallOriginal,
            recall.reproductionFingerprint(
                targetScopeIdForFingerprint = if (recallScope == "p1-studere-present") {
                    "p1-drikke-present"
                } else {
                    "p1-studere-present"
                },
            ),
        )
        listOf(
            recallSource.copy(normalizationPolicy = "different"),
            recallSource.copy(expectedAnswer = "different"),
            recallSource.copy(knownAlternatives = recallSource.knownAlternatives.toMutableList().also {
                it[0] = alternative.copy(text = "different")
            }),
            recallSource.copy(knownAlternatives = recallSource.knownAlternatives.toMutableList().also {
                it[0] = alternative.copy(targetStatus = TargetStatus.entries.first { status ->
                    status != alternative.targetStatus
                })
            }),
            recallSource.copy(knownAlternatives = recallSource.knownAlternatives.toMutableList().also {
                it[0] = alternative.copy(linguisticStatus = LinguisticStatus.entries.first { status ->
                    status != alternative.linguisticStatus
                })
            }),
            recallSource.copy(knownAlternatives = recallSource.knownAlternatives.toMutableList().also {
                it[0] = alternative.copy(meaningSatisfaction = MeaningSatisfaction.entries.first { status ->
                    status != alternative.meaningSatisfaction
                })
            }),
            recallSource.copy(knownAlternatives = recallSource.knownAlternatives.toMutableList().also {
                it[0] = alternative.copy(responseConstraintStatus = ResponseConstraintStatus.entries.first { status ->
                    status != alternative.responseConstraintStatus
                })
            }),
            recallSource.copy(knownAlternatives = recallSource.knownAlternatives.toMutableList().also {
                it[0] = alternative.copy(issueCode = "different")
            }),
            recallSource.copy(knownAlternatives = recallSource.knownAlternatives.reversed()),
            recallSource.copy(lexicalSources = recallSource.lexicalSources.toMutableList().also {
                it[0] = it[0].copy(selectedUse = "different")
            }),
        ).forEach { changed ->
            assertNotEquals(recallOriginal, recall.reproductionFingerprint(vocabularySourceForFingerprint = changed))
        }

        val tiles = generate("E037", "G005", 0)
        assertEquals("p1-subject-first-supplied", tiles.targetScopeId)
        val tileSource = assertNotNull(tiles.inspector.vocabularySource)
        val reviewed = tileSource.reviewedOrders.first()
        val occurrence = tileSource.tileOccurrences.first()
        val tileOriginal = tiles.reproductionFingerprint()
        listOf(
            tileSource.copy(acceptedOrders = tileSource.acceptedOrders.map { it.reversed() }),
            tileSource.copy(acceptedOrders = tileSource.acceptedOrders + listOf(tileSource.acceptedOrders.first())),
            tileSource.copy(tileOccurrences = tileSource.tileOccurrences.reversed()),
            tileSource.copy(tileOccurrences = tileSource.tileOccurrences.toMutableList().also {
                it[0] = occurrence.copy(blockId = "different")
            }),
            tileSource.copy(tileOccurrences = tileSource.tileOccurrences.toMutableList().also {
                it[0] = occurrence.copy(lexicalId = "different")
            }),
            tileSource.copy(tileOccurrences = tileSource.tileOccurrences.toMutableList().also {
                it[0] = occurrence.copy(form = "different")
            }),
            tileSource.copy(reviewedOrders = tileSource.reviewedOrders.toMutableList().also {
                it[0] = reviewed.copy(words = reviewed.words.reversed())
            }),
            tileSource.copy(reviewedOrders = tileSource.reviewedOrders.toMutableList().also {
                it[0] = reviewed.copy(targetStatus = TargetStatus.entries.first { status ->
                    status != reviewed.targetStatus
                })
            }),
            tileSource.copy(reviewedOrders = tileSource.reviewedOrders.toMutableList().also {
                it[0] = reviewed.copy(linguisticStatus = LinguisticStatus.entries.first { status ->
                    status != reviewed.linguisticStatus
                })
            }),
            tileSource.copy(reviewedOrders = tileSource.reviewedOrders.toMutableList().also {
                it[0] = reviewed.copy(meaningSatisfaction = MeaningSatisfaction.entries.first { status ->
                    status != reviewed.meaningSatisfaction
                })
            }),
            tileSource.copy(reviewedOrders = tileSource.reviewedOrders.toMutableList().also {
                it[0] = reviewed.copy(responseConstraintStatus = ResponseConstraintStatus.entries.first { status ->
                    status != reviewed.responseConstraintStatus
                })
            }),
            tileSource.copy(reviewedOrders = tileSource.reviewedOrders.toMutableList().also {
                it[0] = reviewed.copy(issueCode = "different")
            }),
            tileSource.copy(reviewedOrders = tileSource.reviewedOrders.reversed()),
        ).forEach { changed ->
            assertNotEquals(tileOriginal, tiles.reproductionFingerprint(vocabularySourceForFingerprint = changed))
        }
        val twoAcceptedOrders = tileSource.acceptedOrders + listOf(tileSource.acceptedOrders.first().reversed())
        assertNotEquals(
            tiles.reproductionFingerprint(
                vocabularySourceForFingerprint = tileSource.copy(acceptedOrders = twoAcceptedOrders),
            ),
            tiles.reproductionFingerprint(
                vocabularySourceForFingerprint = tileSource.copy(acceptedOrders = twoAcceptedOrders.reversed()),
            ),
        )

        val matching = generate("E033", null, 0)
        val matchingSource = assertNotNull(matching.inspector.vocabularySource)
        val matchingPair = matchingSource.matchingPairs.first()
        val matchingLexical = matchingPair.lexicalSource
        val matchingOriginal = matching.reproductionFingerprint()
        listOf(
            matchingSource.copy(lexicalSources = matchingSource.lexicalSources.reversed()),
            matchingSource.copy(matchingPairs = matchingSource.matchingPairs.reversed()),
            matchingSource.copy(matchingPairs = matchingSource.matchingPairs.toMutableList().also {
                it[0] = matchingPair.copy(leftId = "different")
            }),
            matchingSource.copy(matchingPairs = matchingSource.matchingPairs.toMutableList().also {
                it[0] = matchingPair.copy(rightId = "different")
            }),
            matchingSource.copy(matchingPairs = matchingSource.matchingPairs.toMutableList().also {
                it[0] = matchingPair.copy(lexicalSource = matchingLexical.copy(entryId = "different"))
            }),
            matchingSource.copy(matchingPairs = matchingSource.matchingPairs.toMutableList().also {
                it[0] = matchingPair.copy(lexicalSource = matchingLexical.copy(senseId = "different"))
            }),
            matchingSource.copy(matchingPairs = matchingSource.matchingPairs.toMutableList().also {
                it[0] = matchingPair.copy(lexicalSource = matchingLexical.copy(
                    kind = VocabularyLexicalKind.entries.first { kind -> kind != matchingLexical.kind },
                ))
            }),
            matchingSource.copy(matchingPairs = matchingSource.matchingPairs.toMutableList().also {
                it[0] = matchingPair.copy(lexicalSource = matchingLexical.copy(
                    forms = matchingLexical.forms.copy(form = "different"),
                ))
            }),
            matchingSource.copy(matchingPairs = matchingSource.matchingPairs.toMutableList().also {
                it[0] = matchingPair.copy(lexicalSource = matchingLexical.copy(gloss = "different"))
            }),
            matchingSource.copy(matchingPairs = matchingSource.matchingPairs.toMutableList().also {
                it[0] = matchingPair.copy(lexicalSource = matchingLexical.copy(selectedUse = "different"))
            }),
        ).forEach { changed ->
            assertNotEquals(matchingOriginal, matching.reproductionFingerprint(vocabularySourceForFingerprint = changed))
        }
        assertEquals("p1-drikke-present", generate("E038", "G004", 0).targetScopeId)
    }

    private fun generate(formatId: String, targetSkillId: String?, index: Int) = engine.generate(
        GenerationRequest(formatId, targetSkillId, 11, index, "pilot-17", "generator-17"),
    )

    private companion object {
        const val EXERCISE_ID = "00000000-0000-0000-0000-000000000001"
    }
}
