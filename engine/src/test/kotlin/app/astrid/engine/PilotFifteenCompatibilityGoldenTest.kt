package app.astrid.engine

import java.nio.file.Files
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PilotFifteenCompatibilityGoldenTest {
    @Test
    fun `published pilot fifteen generation and evaluation remain frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(FIXTURE_ROW_COUNT, expected.size)
        assertEquals(expected, PilotFifteenCompatibilityGolden.rows())
    }

    @Test
    fun `archive covers all formats module one tasks answer kinds and boundaries`() {
        val rows = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
            lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
        }
        assertEquals((1..29).map { "E${it.toString().padStart(3, '0')}" }.toSet(), rows.map { it[1] }.toSet())
        val authoredCounts = mapOf(
            "E016" to 7, "E017" to 7, "E018" to 8, "E019" to 8, "E020" to 2, "E021" to 2,
            "E022" to 8, "E023" to 8, "E024" to 3, "E025" to 6, "E026" to 2, "E027" to 2,
            "E028" to 3,
            "E029" to 4,
        )
        authoredCounts.forEach { (formatId, taskCount) ->
            val formatRows = rows.filter { it[1] == formatId }
            assertEquals(taskCount + BOUNDARY_ROW_COUNT, formatRows.count { it[0] == "G" })
            assertTrue(setOf("T", "O", "M", "P").all { kind -> formatRows.any { it[0] == kind } })
            BOUNDARY_SEEDS.forEach { seed ->
                assertTrue(formatRows.any { it[0] == "G" && it[3] == seed.toString() && it[4] == MAX_INDEX.toString() })
            }
        }
        assertEquals(76, rows.count { it[0] == "O" && it[1] == "E029" })
        val moduleOneTextAnswers = rows.filter { it[0] == "T" && it[1] in authoredCounts }
            .map { Base64.getDecoder().decode(it[5]).toString(Charsets.UTF_8) }
        assertTrue("i" in moduleOneTextAnswers)
        assertTrue("De" in moduleOneTextAnswers)
        assertTrue(setOf("jeg", "du", "han", "hun", "vi", "I", "de", "være").all { it in moduleOneTextAnswers })
        assertTrue(setOf("den", "det").all { it in moduleOneTextAnswers })
        assertTrue(setOf("arbejder", "læser", "sover", "arbejde", "læse", "sove").all { it in moduleOneTextAnswers })
        assertTrue(setOf("har", "have").all { it in moduleOneTextAnswers })
        assertTrue(setOf("læse", "at læse", "læser", "LÆSE", "ukendt").all { it in moduleOneTextAnswers })
    }

    @Test
    fun `provenance pins release source revisions review and bytes`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        assertTrue("releaseSourceCommit=0f1b322903fc243871b3b7293a26b52af80dd0dc" in provenance)
        assertTrue("releaseVersion=0.17.0" in provenance)
        assertTrue("captureDate=2026-11-11" in provenance)
        assertTrue("contentRevision=pilot-15" in provenance)
        assertTrue("generatorRevision=generator-15" in provenance)
        assertTrue("evaluationRevision=evaluation-15" in provenance)
        assertTrue("reviewState=compatibility-only-not-linguistic-approval" in provenance)
        assertTrue("rowCount=$FIXTURE_ROW_COUNT" in provenance)
        assertTrue("fixtureSha256=$FIXTURE_SHA256" in provenance)
        assertTrue("reviewSha256=$REVIEW_SHA256" in provenance)
        assertEquals(REVIEW_SHA256, publishedPairFifteenReviewBytes().sha256())
    }

    @Test
    fun `teaching review contains all thirteen released guided introductions`() {
        val review = publishedPairFifteenReviewBytes().toString(Charsets.UTF_8)
        val revisions = setOf(
            "module-one-singular-1", "module-one-groups-1", "module-one-er-1", "module-one-things-1",
            "module-one-present-1", "module-one-order-1", "module-one-possession-1",
            "module-one-personal-1",
            "module-one-ability-1",
            "module-one-volition-1", "module-one-obligation-1", "module-one-permission-1",
            "module-one-circumstances-1",
        )
        revisions.forEach { assertTrue(it in review, it) }
    }

    @Test
    fun `pair fifteen literal circumstance task and metadata membership are included`() {
        val tasks = FinalCircumstanceContent.tasks
        assertEquals(4, tasks.size)
        assertEquals(
            setOf(
                "circumstance-work-today", "circumstance-read-letter-now",
                "circumstance-work-aarhus", "circumstance-read-newspaper-today",
            ),
            tasks.map { it.id }.toSet(),
        )
        assertTrue(tasks.all { it.targetScopeId == "one-final-circumstance" })
        assertTrue(tasks.all { it.feedbackExposureKeys.last() == "module-one:pattern:one-final-circumstance" })
        assertEquals(
            PAIR_FIFTEEN_IDS,
            PilotContent.currentSnapshot.lexicalReview.map { it.id }.filter { it in PAIR_FIFTEEN_IDS }.toSet(),
        )
        assertEquals("C007", FinalCircumstanceContent.CONSTRUCTION_ID)
        assertTrue(PilotContent.currentSnapshot.lexicalReview.single { it.id == "L032" }.constraints.contains("C007"))
        assertTrue(!PilotContent.pilotFourteenSnapshot.lexicalReview.single { it.id == "L032" }.constraints.contains("C007"))
        assertEquals(
            (1..29).map { "E${it.toString().padStart(3, '0')}" }.toSet(),
            PilotContent.currentSnapshot.formats.map { it.id }.toSet(),
        )
        assertEquals(
            (1..28).map { "E${it.toString().padStart(3, '0')}" }.toSet(),
            PilotContent.pilotFourteenSnapshot.formats.map { it.id }.toSet(),
        )
    }

    @Test
    fun `circumstance archive spans all sixty complete permutations and target distinctions`() {
        val engine = PilotEngine()
        val exercises = (0..3).map {
            engine.generate(GenerationRequest("E029", "G016", 47, it, "pilot-15", "generator-15"))
        }
        assertEquals(4, exercises.map { it.inspector.canonicalSentenceId }.toSet().size)
        assertEquals(60, exercises.sumOf { exercise ->
            val count = requireNotNull(exercise.inspector.tileSource).tokens.size
            (1..count).fold(1) { total, value -> total * value }
        })
        val four = exercises.first { requireNotNull(it.inspector.tileSource).tokens.size == 4 }
        val canonical = requireNotNull(four.inspector.tileSource).licensedOrders.single().blockIds
        val targetLastCoreWrong = Answer.Order(listOf(canonical[1], canonical[0], canonical[2], canonical[3]))
        val targetNotLast = Answer.Order(listOf(canonical[0], canonical[1], canonical[3], canonical[2]))
        assertEquals(TargetStatus.MET, engine.evaluate(four, targetLastCoreWrong).targetStatus)
        assertEquals(ResponseConstraintStatus.NOT_MET, engine.evaluate(four, targetLastCoreWrong).responseConstraintStatus)
        assertEquals(TargetStatus.NOT_MET, engine.evaluate(four, targetNotLast).targetStatus)
        val source = four.source as CircumstanceExerciseSource
        assertEquals("da", four.prompt.cueLanguage)
        assertEquals("da", four.prompt.answerLanguage)
        assertEquals(
            source.task.parts.dropLast(1).joinToString(" ") { it.text }.replaceFirstChar(Char::titlecase) + ".",
            four.prompt.cue,
        )
        assertTrue(source.task.englishCore in requireNotNull(four.context))
        assertTrue(source.task.circumstanceMeaning in requireNotNull(four.context))
        assertTrue(FinalCircumstanceContent.introduction().filter { it.bodyLanguage == "da" }.size >= 6)
    }

    @Test
    fun `pair fifteen projection distinguishes private circumstance mutations`() {
        val engine = PilotEngine()
        val exercise = engine.generate(GenerationRequest("E029", "G016", 47, 0, "pilot-15", "generator-15"))
        val source = exercise.source as CircumstanceExerciseSource
        val original = PilotFifteenCompatibilityGolden.generationHash(exercise)
        val mutations = listOf(
            mutate(exercise, source.copy(task = source.task.copy(englishCore = "changed"))),
            mutate(exercise, source.copy(task = source.task.copy(circumstanceMeaning = "changed"))),
            mutate(exercise, source.copy(task = source.task.copy(frameVariantId = "changed"))),
            mutate(exercise, source.copy(task = source.task.copy(parts = source.task.parts.reversed()))),
            mutate(exercise, source.copy(blockRoles = source.blockRoles.mapValues { "changed" })),
            mutate(exercise, source.copy(answerKey = AnswerKey.TileOrder(emptyList(), null, null))),
        )
        mutations.forEach { assertNotEquals(original, PilotFifteenCompatibilityGolden.generationHash(it)) }
    }

    private fun mutate(exercise: GeneratedExercise, source: ExerciseSource): GeneratedExercise = GeneratedExercise(
        id = exercise.id,
        formatId = exercise.formatId,
        targetSkillId = exercise.targetSkillId,
        lexicalTarget = exercise.lexicalTarget,
        supportSkillIds = exercise.supportSkillIds,
        presentation = exercise.presentation,
        contentRevision = exercise.contentRevision,
        generatorRevision = exercise.generatorRevision,
        reproduction = exercise.reproduction,
        inspector = exercise.inspector,
        source = source,
        readingTarget = exercise.readingTarget,
        lexicalTargets = exercise.lexicalTargets,
    )

    @Test
    fun `export refuses to overwrite either immutable file`() {
        val directory = Files.createTempDirectory("pilot-fifteen-compatibility")
        val fixture = Files.createFile(directory.resolve("fixture.tsv"))
        val provenance = directory.resolve("fixture.provenance")
        assertFailsWith<IllegalArgumentException> { PilotFifteenCompatibilityGolden.write(fixture, provenance) }
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Missing test resource $path" }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private companion object {
        const val FIXTURE_PATH = "golden/pilot-15-generator-15-evaluation-15.tsv"
        const val PROVENANCE_PATH = "golden/pilot-15-generator-15-evaluation-15.provenance"
        const val FIXTURE_ROW_COUNT = 3395
        const val FIXTURE_SHA256 = "753cd5742e90d30500af5ae55c7de7d0fb6aa9988d85180451a535acb17d4a19"
        const val REVIEW_SHA256 = "af3a0d37be95a1ffd9428bd2006c6b31341958ea78955fc0dc32b64326ac2518"
        const val BOUNDARY_ROW_COUNT = 12
        const val MAX_INDEX = 1_000_000
        val BOUNDARY_SEEDS = listOf(0, 1, Int.MAX_VALUE)
        val PAIR_FIFTEEN_IDS = setOf("L032", "L037", "L038")
    }
}
