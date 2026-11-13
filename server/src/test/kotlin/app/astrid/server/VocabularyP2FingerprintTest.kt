package app.astrid.server

import app.astrid.engine.FixedPunctuation
import app.astrid.engine.GenerationRequest
import app.astrid.engine.PilotEngine
import app.astrid.engine.VocabularyExerciseSourceDetails
import app.astrid.server.exercises.reproductionFingerprint
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class VocabularyP2FingerprintTest {
    private val engine = PilotEngine()

    @Test
    fun `pair nineteen fingerprint covers every new nested fact quantity chronology tile and punctuation field`() {
        val bread = exercise("p2-1.3", "E044")
        val breadSource = source(bread)
        val breadFact = breadSource.supportFacts.single { it.factId == "p2-s1-f2" }
        val predicate = assertNotNull(breadFact.predicateSource)
        val objectSource = assertNotNull(breadFact.objectSource)
        mutations(breadSource, breadFact, predicate, objectSource).forEach { changed ->
            assertChanged(bread, changed)
        }

        val quantityReading = exercise("p2-6.2", "E044")
        val quantityReadingSource = source(quantityReading)
        val quantityFact = quantityReadingSource.supportFacts.single { it.factId == "p2-s6-f2" }
        val quantity = assertNotNull(quantityFact.quantitySource)
        val head = quantity.headSource
        val component = quantity.componentSources.last()
        listOf(
            quantityReadingSource.copy(supportFacts = quantityReadingSource.supportFacts.replace(quantityFact,
                quantityFact.copy(quantitySource = quantity.copy(constructionId = "different")))),
            quantityReadingSource.copy(supportFacts = quantityReadingSource.supportFacts.replace(quantityFact,
                quantityFact.copy(quantitySource = quantity.copy(display = "different")))),
            quantityReadingSource.copy(supportFacts = quantityReadingSource.supportFacts.replace(quantityFact,
                quantityFact.copy(quantitySource = quantity.copy(headSource = head.copy(entryId = "different"))))),
            quantityReadingSource.copy(supportFacts = quantityReadingSource.supportFacts.replace(quantityFact,
                quantityFact.copy(quantitySource = quantity.copy(componentSources = quantity.componentSources.replace(
                    component, component.copy(forms = component.forms.copy(form = "different")),
                ))))),
        ).forEach { assertChanged(quantityReading, it) }

        val chronologyReading = exercise("p2-4.2", "E044")
        val chronologySource = source(chronologyReading)
        val relation = chronologySource.chronologyRelations.single()
        listOf(
            chronologySource.copy(chronologyRelations = chronologySource.chronologyRelations.reversed() + relation),
            chronologySource.copy(chronologyRelations = listOf(relation.copy(relationId = "different"))),
            chronologySource.copy(chronologyRelations = listOf(relation.copy(beforeFactId = "different"))),
            chronologySource.copy(chronologyRelations = listOf(relation.copy(afterFactId = "different"))),
            chronologySource.copy(targetChronologyRelationId = "different"),
        ).forEach { assertChanged(chronologyReading, it) }

        val request = exercise("p2-6.3", "E043", "G005")
        val requestSource = source(request)
        val occurrence = requestSource.tileOccurrences.single { it.quantitySource != null }
        val occurrenceQuantity = assertNotNull(occurrence.quantitySource)
        assertNotEquals(
            request.reproductionFingerprint(),
            request.reproductionFingerprint(promptForFingerprint = request.prompt.copy(
                fixedPunctuation = listOf(FixedPunctuation("different", ";")),
            )),
        )
        listOf(
            requestSource.copy(tileOccurrences = requestSource.tileOccurrences.replace(occurrence,
                occurrence.copy(lexicalSource = occurrenceQuantity.headSource))),
            requestSource.copy(tileOccurrences = requestSource.tileOccurrences.replace(occurrence,
                occurrence.copy(quantitySource = occurrenceQuantity.copy(display = "different")))),
        ).forEach { assertChanged(request, it) }

    }

    @Test
    fun `pair nineteen inherited and P2 matching fingerprints are deterministic in fresh locale JVMs`() {
        val classpath = System.getProperty("java.class.path")
        val java = File(System.getProperty("java.home"), "bin/java").absolutePath
        val fingerprints = listOf("en-US", "tr-TR", "ar-EG").map { locale ->
            val process = ProcessBuilder(
                java,
                "-Duser.language=${locale.substringBefore('-')}",
                "-Duser.country=${locale.substringAfter('-')}",
                "-cp",
                classpath,
                VocabularyP2FingerprintProbeMain::class.qualifiedName,
            ).redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            assertEquals(0, process.waitFor(), output)
            output.lineSequence().filter(String::isNotBlank).toList()
        }
        assertEquals(1, fingerprints.distinct().size)
    }

    private fun mutations(
        source: VocabularyExerciseSourceDetails,
        fact: app.astrid.engine.VocabularySupportFactDetails,
        predicate: app.astrid.engine.VocabularyLexicalSourceDetails,
        objectSource: app.astrid.engine.VocabularyLexicalSourceDetails,
    ) = listOf(
        source.copy(supportFacts = source.supportFacts.replace(fact, fact.copy(referentDisplay = "different"))),
        source.copy(supportFacts = source.supportFacts.replace(fact, fact.copy(translation = "different"))),
        source.copy(supportFacts = source.supportFacts.replace(fact,
            fact.copy(predicateSource = predicate.copy(entryId = "different")))),
        source.copy(supportFacts = source.supportFacts.replace(fact,
            fact.copy(predicateSource = predicate.copy(senseId = "different")))),
        source.copy(supportFacts = source.supportFacts.replace(fact,
            fact.copy(predicateSource = predicate.copy(forms = predicate.forms.copy(infinitive = "different"))))),
        source.copy(supportFacts = source.supportFacts.replace(fact,
            fact.copy(predicateSource = predicate.copy(forms = predicate.forms.copy(present = "different"))))),
        source.copy(supportFacts = source.supportFacts.replace(fact,
            fact.copy(predicateSource = predicate.copy(gloss = "different")))),
        source.copy(supportFacts = source.supportFacts.replace(fact,
            fact.copy(predicateSource = predicate.copy(selectedUse = "different")))),
        source.copy(supportFacts = source.supportFacts.replace(fact,
            fact.copy(objectSource = objectSource.copy(entryId = "different")))),
    )

    private fun exercise(taskId: String, formatId: String, targetSkillId: String? = null) =
        (0 until 8).asSequence().map { index ->
            engine.generate(GenerationRequest(formatId, targetSkillId, 47, index, "pilot-19", "generator-19"))
        }.first { source(it).taskId == taskId }

    private fun source(exercise: app.astrid.engine.GeneratedExercise) = assertNotNull(exercise.inspector.vocabularySource)

    private fun assertChanged(exercise: app.astrid.engine.GeneratedExercise, changed: VocabularyExerciseSourceDetails) {
        assertNotEquals(
            exercise.reproductionFingerprint(),
            exercise.reproductionFingerprint(vocabularySourceForFingerprint = changed),
        )
    }

    private fun <T> List<T>.replace(old: T, new: T): List<T> = map { if (it == old) new else it }
}
