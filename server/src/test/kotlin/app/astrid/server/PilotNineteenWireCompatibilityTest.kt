package app.astrid.server

import app.astrid.server.exercises.reproductionFingerprint
import app.astrid.server.exercises.toInspector
import app.astrid.server.exercises.toResponse
import app.astrid.server.infra.json.wireJson
import java.nio.file.Files
import java.security.MessageDigest
import java.util.Locale
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class PilotNineteenWireCompatibilityTest {
    @Test
    fun `pilot nineteen public inspector fingerprint and evaluation wire remains exact`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(ROW_COUNT, expected.size)
        assertEquals(expected, PilotNineteenWireCompatibilityFixture.rows())
    }

    @Test
    fun `archive covers all forty four formats and all twenty one P2 tasks`() {
        val rows = fixtureRows()
        assertEquals(
            (1..44).map { "E${it.toString().padStart(3, '0')}" }.toSet(),
            rows.filter { it[0] in setOf("C", "B") }.map { it[1] }.toSet(),
        )
        val exercises = PilotNineteenWireCompatibilityFixture.p2Exercises()
        assertEquals(21, exercises.size)
        assertEquals(
            mapOf("E040" to 4, "E041" to 3, "E042" to 4, "E043" to 5, "E044" to 5),
            exercises.groupingBy { it.formatId }.eachCount(),
        )
        assertEquals(21, exercises.map { requireNotNull(it.inspector.vocabularySource).taskId }.toSet().size)
        assertTrue(rows.any { it[0] == "GRADE" && it[1] == "E042" })
        assertTrue(rows.any { it[0] == "GRADE" && it[1] == "E043" })
        assertTrue(rows.any { it[0] == "GRADE" && it[1] == "E044" })
    }

    @Test
    fun `P2 public wire omits private keys and source correspondences`() {
        PilotNineteenWireCompatibilityFixture.p2Exercises().forEach { exercise ->
            val public = wireJson.parseToJsonElement(
                wireJson.encodeToString(exercise.toResponse(PilotNineteenWireCompatibilityFixture.EXERCISE_ID)),
            ).jsonObject
            val inspector = wireJson.parseToJsonElement(
                wireJson.encodeToString(exercise.toInspector(PilotNineteenWireCompatibilityFixture.EXERCISE_ID)),
            ).jsonObject
            val publicKeys = public.recursiveKeys()
            assertFalse("vocabularySource" in publicKeys)
            assertTrue(
                publicKeys.intersect(
                    setOf(
                        "expectedAnswer", "matchingPairs", "acceptedOrders", "knownAlternatives", "reviewedOrders",
                        "targetSupportFactId", "targetChronologyRelationId", "quantitySource", "predicateSource",
                        "objectSource",
                    ),
                ).isEmpty(),
                requireNotNull(exercise.inspector.vocabularySource).taskId,
            )
            assertTrue("vocabularySource" in inspector.recursiveKeys())
        }
    }

    @Test
    fun `every P2 private source mutation changes the pair nineteen fingerprint`() {
        val exercises = PilotNineteenWireCompatibilityFixture.p2Exercises()
        val factExercise = exercises.first { requireNotNull(it.inspector.vocabularySource).supportFacts.isNotEmpty() }
        val factSource = requireNotNull(factExercise.inspector.vocabularySource)
        val fact = factSource.supportFacts.first()
        val factMutations = listOf(
            factSource.copy(supportFacts = factSource.supportFacts.replaceFirst(fact.copy(referentDisplay = "changed"))),
            factSource.copy(supportFacts = factSource.supportFacts.replaceFirst(fact.copy(translation = "changed"))),
            fact.predicateSource?.let { lexical ->
                factSource.copy(supportFacts = factSource.supportFacts.replaceFirst(fact.copy(
                    predicateSource = lexical.copy(selectedUse = "changed"),
                )))
            },
            fact.objectSource?.let { lexical ->
                factSource.copy(supportFacts = factSource.supportFacts.replaceFirst(fact.copy(
                    objectSource = lexical.copy(gloss = "changed"),
                )))
            },
            fact.quantitySource?.let { quantity ->
                factSource.copy(supportFacts = factSource.supportFacts.replaceFirst(fact.copy(
                    quantitySource = quantity.copy(display = "changed"),
                )))
            },
        ).filterNotNull()
        assertTrue(factMutations.size >= 4)
        factMutations.forEach { changed -> assertFingerprintChanged(factExercise, changed) }
        val orderedFactExercise = exercises.first {
            requireNotNull(it.inspector.vocabularySource).supportFacts.size > 1
        }
        val orderedFactSource = requireNotNull(orderedFactExercise.inspector.vocabularySource)
        assertFingerprintChanged(
            orderedFactExercise,
            orderedFactSource.copy(supportFacts = orderedFactSource.supportFacts.reversed()),
        )

        val quantityFactExercise = exercises.first { exercise ->
            requireNotNull(exercise.inspector.vocabularySource).supportFacts.any { it.quantitySource != null }
        }
        val quantityFactSource = requireNotNull(quantityFactExercise.inspector.vocabularySource)
        val quantityFactIndex = quantityFactSource.supportFacts.indexOfFirst { it.quantitySource != null }
        val quantityFact = quantityFactSource.supportFacts[quantityFactIndex]
        val changedQuantityFact = quantityFact.copy(
            quantitySource = requireNotNull(quantityFact.quantitySource).copy(display = "changed"),
        )
        assertFingerprintChanged(
            quantityFactExercise,
            quantityFactSource.copy(supportFacts = quantityFactSource.supportFacts.toMutableList().also {
                it[quantityFactIndex] = changedQuantityFact
            }),
        )

        val tileExercise = exercises.first { exercise ->
            requireNotNull(exercise.inspector.vocabularySource).tileOccurrences.any { it.lexicalSource != null }
        }
        val tileSource = requireNotNull(tileExercise.inspector.vocabularySource)
        val tileIndex = tileSource.tileOccurrences.indexOfFirst { it.lexicalSource != null }
        val tile = tileSource.tileOccurrences[tileIndex]
        val tileMutations = listOfNotNull(
            tile.lexicalSource?.let { lexical ->
                tileSource.copy(tileOccurrences = tileSource.tileOccurrences.toMutableList().also {
                    it[tileIndex] = tile.copy(lexicalSource = lexical.copy(selectedUse = "changed"))
                })
            },
        )
        assertTrue(tileMutations.isNotEmpty())
        tileMutations.forEach { changed -> assertFingerprintChanged(tileExercise, changed) }

        val quantityTileExercise = exercises.first { exercise ->
            requireNotNull(exercise.inspector.vocabularySource).tileOccurrences.any { it.quantitySource != null }
        }
        val quantityTileSource = requireNotNull(quantityTileExercise.inspector.vocabularySource)
        val quantityTileIndex = quantityTileSource.tileOccurrences.indexOfFirst { it.quantitySource != null }
        val quantityTile = quantityTileSource.tileOccurrences[quantityTileIndex]
        assertFingerprintChanged(
            quantityTileExercise,
            quantityTileSource.copy(tileOccurrences = quantityTileSource.tileOccurrences.toMutableList().also {
                it[quantityTileIndex] = quantityTile.copy(
                    quantitySource = requireNotNull(quantityTile.quantitySource).copy(constructionId = "changed"),
                )
            }),
        )

        val chronologyExercise = exercises.first {
            requireNotNull(it.inspector.vocabularySource).chronologyRelations.isNotEmpty()
        }
        val chronologySource = requireNotNull(chronologyExercise.inspector.vocabularySource)
        val relation = chronologySource.chronologyRelations.first()
        assertFingerprintChanged(
            chronologyExercise,
            chronologySource.copy(chronologyRelations = chronologySource.chronologyRelations.replaceFirst(
                relation.copy(afterFactId = "changed"),
            )),
        )
        assertFingerprintChanged(
            chronologyExercise,
            chronologySource.copy(targetChronologyRelationId = "changed"),
        )
        val punctuationExercise = exercises.first { it.prompt.fixedPunctuation.isNotEmpty() }
        val changedPunctuation = punctuationExercise.prompt.copy(
            fixedPunctuation = punctuationExercise.prompt.fixedPunctuation.mapIndexed { index, punctuation ->
                if (index == 0) punctuation.copy(text = ";") else punctuation
            },
        )
        assertNotEquals(
            punctuationExercise.reproductionFingerprint(),
            punctuationExercise.reproductionFingerprint(promptForFingerprint = changedPunctuation),
        )
    }

    @Test
    fun `exporter refuses either output collision`() {
        val directory = createTempDirectory("pilot-nineteen-wire-collision")
        val fixture = directory.resolve("fixture.tsv")
        val provenance = directory.resolve("fixture.provenance")
        Files.writeString(fixture, "occupied")
        assertFailsWith<IllegalArgumentException> {
            PilotNineteenWireCompatibilityExportMain.main(arrayOf(fixture.toString(), provenance.toString()))
        }
        Files.delete(fixture)
        Files.writeString(provenance, "occupied")
        assertFailsWith<IllegalArgumentException> {
            PilotNineteenWireCompatibilityExportMain.main(arrayOf(fixture.toString(), provenance.toString()))
        }
    }

    @Test
    fun `provenance pins published source revisions rows and bytes`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        listOf(
            "releaseSourceCommit=${PilotNineteenWireCompatibilityFixture.RELEASE_SOURCE_COMMIT}",
            "releaseVersion=0.22.0", "captureDate=$CAPTURE_DATE", "contentRevision=pilot-19",
            "generatorRevision=generator-19", "evaluationRevision=evaluation-19",
            "projectionSchema=pair19-server-wire-v1", "reviewState=compatibility-only-not-linguistic-approval",
            "rowCount=$ROW_COUNT", "fixtureSha256=$FIXTURE_SHA256",
        ).forEach { assertTrue(it in provenance) }
    }

    private fun assertFingerprintChanged(
        exercise: app.astrid.engine.GeneratedExercise,
        changed: app.astrid.engine.VocabularyExerciseSourceDetails,
    ) = assertNotEquals(
        exercise.reproductionFingerprint(),
        exercise.reproductionFingerprint(vocabularySourceForFingerprint = changed),
    )

    private fun <T> List<T>.replaceFirst(value: T) = toMutableList().also { it[0] = value }

    private fun JsonObject.recursiveKeys(): Set<String> = buildSet {
        fun collect(element: kotlinx.serialization.json.JsonElement) {
            when (element) {
                is JsonObject -> element.forEach { (key, value) -> add(key); collect(value) }
                is kotlinx.serialization.json.JsonArray -> element.forEach(::collect)
                else -> Unit
            }
        }
        collect(this@recursiveKeys)
    }

    private fun fixtureRows() = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
        lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
    }
    private fun resource(path: String) = requireNotNull(javaClass.getResourceAsStream(path)) { "Missing resource $path" }
    private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private companion object {
        const val FIXTURE_PATH = "/golden/pilot-19-generator-19-wire.tsv"
        const val PROVENANCE_PATH = "/golden/pilot-19-generator-19-wire.provenance"
        const val ROW_COUNT = 7_099 // Captured from v0.22.0 in two fresh JVMs.
        const val FIXTURE_SHA256 = "a87beb67aae9a6a1f340f1b14a0ffbc839920e813db71d00f67e2dfde129eed6"
        const val CAPTURE_DATE = "2026-11-13"
    }
}
